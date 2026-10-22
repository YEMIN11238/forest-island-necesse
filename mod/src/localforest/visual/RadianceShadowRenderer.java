package localforest.visual;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;
import localforest.scene.RegionSceneQuery;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import necesse.engine.window.WindowManager;
import necesse.engine.window.GameWindow;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.Level;

/**
 * Native receiver-plane adapter for SDV-Radiance's grounded silhouette projection.
 * This is a port of the grounded projection, not of Radiance's entire pipeline.
 * Each light has its own max-union silhouette mask. The final blocked-light
 * fraction accounts for unoccluded lights filling a shadow. Alpha is sampled
 * from the complete sprite, rather than stretched two-row strips.
 */
public final class RadianceShadowRenderer {
    public static final class Light {
        public final long key;
        public final float x,y,z,strength,radius;
        public final boolean directional;
        /** Directional x/y is the shadow direction; radius is length per height. */
        public Light(long key,float x,float y,float z,float strength,float radius,boolean directional) {
            this.key=key;this.x=x;this.y=y;this.z=z;this.strength=strength;this.radius=radius;this.directional=directional;
        }
        float energy(float wx,float wy) {
            if(directional)return strength;
            float dx=wx-x,dy=wy-y;
            float d=(float)Math.sqrt(dx*dx+dy*dy)/radius;
            return d>=1?0:strength*(1-d*d*(3-2*d));
        }
    }
    private static final class Mask {
        final int w,h;final int[] a;
        GameTexture gpuTexture;
        Mask(int w,int h,int[] a){this.w=w;this.h=h;this.a=a.clone();}
        float sample(float x,float y) {
            if(x<-.5f||y<-.5f||x>w-.5f||y>h-.5f)return 0;
            int ix=(int)Math.floor(x),iy=(int)Math.floor(y);float fx=x-ix,fy=y-iy;
            return (at(ix,iy)*(1-fx)+at(ix+1,iy)*fx)*(1-fy)+(at(ix,iy+1)*(1-fx)+at(ix+1,iy+1)*fx)*fy;
        }
        int at(int x,int y){return x>=0&&y>=0&&x<w&&y<h?a[y*w+x]:0;}
    }
    private static final class Cast {
        final Mask mask;final float x,y,worldX,worldY,scale,height;final int tx,ty;final boolean crown,plant;
        final float[] windOffsets=new float[9];
        final float[] projectedVertices=new float[36];
        float seaX,seaY;boolean gpuVisible;
        Cast(Mask mask,String id,float x,float y,float wx,float wy,float scale,float height,int tx,int ty) {
            this.mask=mask;this.x=x;this.y=y;worldX=wx;worldY=wy;this.scale=scale;this.height=height;this.tx=tx;this.ty=ty;
            crown=id.contains("-top")||id.startsWith("crown-");
            plant=crown||id.contains("bush")||id.equals("fern")||id.equals("grass")||id.contains("flower")||id.equals("reeds");
        }
        void prepareWind(double time) {
            if(!plant||CrownWindRenderer.isLegacy())return;
            float phase=(float)Math.sin(tx*.12+ty*.08)*.06f;
            double t=time*(crown?2.05:1.9)+phase;
            double diff=(.70-.38)*(.70-.38);
            double strength=Math.max(.38,Math.min(.70,.38+diff+Math.sin(t/3.5)*diff))*mask.w*scale*(crown?.052:.034);
            double amplitude=(Math.sin(t)+Math.cos(t))*strength;
            // These are the same nine vertices as the GPU's eight-segment
            // mesh, so interpolation reproduces its actual raster geometry.
            for(int i=0;i<9;i++)windOffsets[i]=(float)(amplitude*Math.pow(Math.max(0,1-i/8.0-(crown?.04:.08)),crown?1.35:1.8));
        }
        float wind(float v) {
            float at=Math.max(0,Math.min(8,v*8));int i=Math.min(7,(int)at);float f=at-i;
            return windOffsets[i]*(1-f)+windOffsets[i+1]*f;
        }
    }
    private static final class Layer {
        byte[] pixels;int minX,minY,maxX,maxY,width,height;
        Layer(int w,int h){resize(w,h);}
        void resize(int w,int h){width=w;height=h;pixels=new byte[w*h];minX=w;minY=h;maxX=maxY=0;}
        void clear(int w,int h){
            if(width!=w||height!=h){resize(w,h);return;}
            for(int y=minY;y<maxY;y++)Arrays.fill(pixels,y*width+minX,y*width+maxX,(byte)0);
            minX=w;minY=h;maxX=maxY=0;
        }
        void put(int x,int y,int a){int at=y*width+x;if(a>(pixels[at]&255)){pixels[at]=(byte)a;touch(at);}}
        void bounds(int x0,int y0,int x1,int y1){minX=Math.min(minX,x0);minY=Math.min(minY,y0);maxX=Math.max(maxX,x1);maxY=Math.max(maxY,y1);}
    }
    private static final class Group {
        final Level level;final List<Light> lights;final List<Cast> casts=Collections.synchronizedList(new ArrayList<>());
        float cameraX,cameraY;boolean shore;
        Group(Level level,List<Light> lights){this.level=level;this.lights=new ArrayList<>(lights);}
        void draw(){render(this);}
    }
    private static final Map<String,Mask> masks=new HashMap<>();
    private static final Map<OrderableDrawables,Group> groups=new WeakHashMap<>();
    private static final Map<Long,Layer> layers=new HashMap<>();
    private static GameTexture texture;
    private static ByteBuffer upload;
    // Only actually covered pixels need radiance composition. The upload is
    // persistent; previous dirty pixels are cleared before new ones are drawn.
    private static int[] touched,stamps;
    private static int touchedCount,stamp=1;
    private static boolean[] waterTiles;
    private static int mapWidth,mapHeight,mapOriginX,mapOriginY;
    public static int receiverTileCount,receiverOriginX,receiverOriginY,receiverWidth,receiverHeight;
    public static long frames,castsDrawn,shoreQuads,drawBatches;
    public static int lastLightCount;
    public static double lastCpuMillis,meanCpuMillis,maxCpuMillis;
    public static boolean gpuActive;
    private static double totalCpuMillis;
    private static boolean gpuUnavailable;
    private static GameShader maskShader,composeShader;
    private static final GameFrameBuffer[] gpuMasks=new GameFrameBuffer[8];
    private static GameTexture waterMap,white;
    private static ByteBuffer waterUpload;
    private static FloatBuffer vertexUpload;
    private static final class CachedShader extends GameShader {
        private final Map<String,Integer> locations=new HashMap<>();
        CachedShader(String vertex,String fragment){super(vertex,fragment);}
        @Override public int getUniform(String name){return locations.computeIfAbsent(name,super::getUniform);}
    }

    public static void registerMask(String id,int width,int height,int[] alpha){masks.put(id,new Mask(width,height,alpha));}
    private static synchronized Group group(OrderableDrawables tiles,Level level,List<Light> lights) {
        Group g=groups.get(tiles);
        if(g==null){g=new Group(level,lights);groups.put(tiles,g);final Group draw=g;tiles.add(-100,tm->draw.draw());}
        return g;
    }
    public static void add(OrderableDrawables tiles,String id,int fx,int fy,float scale,Level level,int tx,int ty,float height,List<Light> lights) {
        Mask mask=masks.get(id);if(mask==null||lights.isEmpty())return;
        Group g=group(tiles,level,lights);
        // tx/ty identify the anchored native object, independent of screen/camera.
        float wx=tx*32+16,wy=ty*32+29;
        if(!g.shore){g.cameraX=wx-fx;g.cameraY=wy-fy;}
        g.casts.add(new Cast(mask,id,fx,fy,wx,wy,scale,height,tx,ty));
    }
    /** Called once from terrain collection; exact native liquid boundaries are the occluder. */
    public static void addShore(OrderableDrawables tiles,Level level,GameCamera camera,List<Light> lights) {
        if(lights.isEmpty())return;
        Group g=group(tiles,level,lights);g.shore=true;
        g.cameraX=-camera.getTileDrawX(0);g.cameraY=-camera.getTileDrawY(0);
    }
    private static boolean water(Level l,float wx,float wy){return nativeWater(l,(int)Math.floor(wx/32),(int)Math.floor(wy/32));}
    private static void touch(int at){if(stamps[at]!=stamp){stamps[at]=stamp;touched[touchedCount++]=at;}}
    private static void raster(Cast c,Light light,Layer layer,Group g,double time) {
        float rootWX=c.x+g.cameraX,rootWY=c.y+g.cameraY;
        if(!light.directional&&light.energy(rootWX,rootWY)<.035f)return;
        float dx=light.directional?light.x:rootWX-light.x,dy=light.directional?light.y:rootWY-light.y;
        float distance=(float)Math.hypot(dx,dy);if(distance<.001f)return;
        dx/=distance;dy/=distance;
        float length=light.directional?c.height*light.radius:Math.min(c.height*1.65f,c.height*distance/Math.max(32,light.z)*.38f);
        length=Math.max(c.height*.20f,length);
        float raised=c.crown?.20f:0;
        float scaleHeight=c.mask.h*c.scale;
        RadianceProjection p=RadianceProjection.solid(dx,dy,length*(1-raised)/scaleHeight,.62f);
        float px=c.x+dx*length*raised,py=c.y+dy*length*raised;
        float half=c.mask.w*c.scale*.5f;
        float seaDrop=light.directional?10*light.radius:Math.min(18,10*distance/Math.max(32,light.z)*.38f);
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=-minX,maxY=-minX;
        for(int yy=0;yy<=1;yy++)for(int xx=0;xx<=1;xx++) {
            float xxp=px+p.x(xx==0?-half:half,yy*scaleHeight),yyp=py+p.y(xx==0?-half:half,yy*scaleHeight);
            minX=Math.min(minX,xxp);maxX=Math.max(maxX,xxp);minY=Math.min(minY,yyp);maxY=Math.max(maxY,yyp);
        }
        int pad=4+(int)Math.ceil(seaDrop);
        int x0=Math.max(0,(int)Math.floor(minX)-pad),y0=Math.max(0,(int)Math.floor(minY)-pad);
        int x1=Math.min(layer.width,(int)Math.ceil(maxX)+pad),y1=Math.min(layer.height,(int)Math.ceil(maxY)+pad);
        if(x0>=x1||y0>=y1)return;
        float det=p.determinant();if(Math.abs(det)<.0001f)return;
        layer.bounds(x0,y0,x1,y1);
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++) {
            float ox=x+.5f-px,oy=y+.5f-py;
            if(water(g.level,x+g.cameraX,y+g.cameraY)){ox-=dx*seaDrop;oy-=dy*seaDrop;}
            float across=(ox*p.alongY-oy*p.alongX)/det;
            float above=(p.acrossX*oy-p.acrossY*ox)/det;
            float v=1-above/scaleHeight;
            if(v<0||v>1)continue;
            across-=c.wind(v);
            float sx=across/c.scale+c.mask.w*.5f-.5f,sy=v*c.mask.h-.5f;
            int a=(int)(c.mask.sample(sx,sy)*(1-.10f*(1-v)));
            if(a>0)layer.put(x,y,a);
        }
        castsDrawn++;
    }
    private static float cross(float ax,float ay,float bx,float by){return ax*by-ay*bx;}
    private static void shoreEdge(Group g,Light light,Layer layer,float ax,float ay,float bx,float by) {
        float mx=(ax+bx)*.5f,my=(ay+by)*.5f;
        if(!light.directional&&light.energy(mx,my)<.035f)return;
        float dx=light.directional?light.x:mx-light.x,dy=light.directional?light.y:my-light.y;
        float distance=(float)Math.hypot(dx,dy);if(distance<.001f)return;
        dx/=distance;dy/=distance;
        float reach=light.directional?16*light.radius:Math.min(24,16*distance/Math.max(32,light.z)*.38f);
        float ex=dx*reach,ey=dy*reach;
        ax-=g.cameraX;ay-=g.cameraY;bx-=g.cameraX;by-=g.cameraY;
        float ux=bx-ax,uy=by-ay,det=cross(ux,uy,ex,ey);
        if(Math.abs(det)<.01f)return;
        int x0=Math.max(0,(int)Math.floor(Math.min(ax,bx)+Math.min(0,ex))-1);
        int x1=Math.min(layer.width,(int)Math.ceil(Math.max(ax,bx)+Math.max(0,ex))+1);
        int y0=Math.max(0,(int)Math.floor(Math.min(ay,by)+Math.min(0,ey))-1);
        int y1=Math.min(layer.height,(int)Math.ceil(Math.max(ay,by)+Math.max(0,ey))+1);
        if(x0>=x1||y0>=y1)return;
        layer.bounds(x0,y0,x1,y1);
        for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++) {
            if(!water(g.level,x+g.cameraX,y+g.cameraY))continue;
            float ox=x+.5f-ax,oy=y+.5f-ay;
            float a=cross(ox,oy,ex,ey)/det,b=cross(ux,uy,ox,oy)/det;
            if(a>=0&&a<=1&&b>=0&&b<=1)layer.put(x,y,(int)(255*(1-.28f*b)));
        }
        shoreQuads++;
    }
    private static void shoreline(Group g,Light light,Layer layer) {
        int left=Math.max(0,(int)(g.cameraX-48)/32),right=Math.min(g.level.tileWidth,(int)(g.cameraX+layer.width+48)/32+1);
        int top=Math.max(0,(int)(g.cameraY-48)/32),bottom=Math.min(g.level.tileHeight,(int)(g.cameraY+layer.height+48)/32+1);
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
            if(water(g.level,x*32+16,y*32+16))continue;
            float x0=x*32,y0=y*32;
            if(water(g.level,x0+16,y0-16))shoreEdge(g,light,layer,x0,y0,x0+32,y0);
            if(water(g.level,x0+16,y0+48))shoreEdge(g,light,layer,x0,y0+32,x0+32,y0+32);
            if(water(g.level,x0-16,y0+16))shoreEdge(g,light,layer,x0,y0,x0,y0+32);
            if(water(g.level,x0+48,y0+16))shoreEdge(g,light,layer,x0+32,y0,x0+32,y0+32);
        }
    }
    private static void render(Group g) {
        long started=System.nanoTime();
        try {
            if(!Boolean.getBoolean("mosslume.shadow.cpu")&&!gpuUnavailable) {
                try {renderGpu(g);gpuActive=true;return;}
                catch(RuntimeException failure){gpuUnavailable=true;gpuActive=false;System.err.println("MOSSLUME_SHADOW_GPU_FAILED "+failure);}
            }
            renderCpu(g);
        } finally {
            lastCpuMillis=(System.nanoTime()-started)/1_000_000.0;
            totalCpuMillis+=lastCpuMillis;maxCpuMillis=Math.max(maxCpuMillis,lastCpuMillis);
            meanCpuMillis=totalCpuMillis/Math.max(1,frames);
            if(frames%120==0)System.out.println("MOSSLUME_SHADOW_COST mode="+(gpuActive?"gpu":"cpu")+" meanMs="+meanCpuMillis+" lastMs="+lastCpuMillis+" maxMs="+maxCpuMillis+" lights="+lastLightCount+" batchesPerFrame="+(drawBatches/(double)Math.max(1,frames)));
        }
    }

    private static void checkShader(GameShader shader) {
        if(GL20.glGetProgrami(shader.getProgram(),GL20.GL_LINK_STATUS)==GL11.GL_FALSE)
            throw new IllegalStateException(GL20.glGetProgramInfoLog(shader.getProgram()));
    }
    private static GameTexture gpuTexture(Mask mask) {
        if(mask.gpuTexture==null) {
            byte[] rgba=new byte[mask.w*mask.h*4];
            for(int i=0;i<mask.a.length;i++){rgba[i*4]=rgba[i*4+1]=rgba[i*4+2]=(byte)255;rgba[i*4+3]=(byte)mask.a[i];}
            mask.gpuTexture=new GameTexture("mosslume-alpha-caster",mask.w,mask.h,rgba);
            mask.gpuTexture.setBlendQuality(GameTexture.BlendQuality.LINEAR);
        }
        return mask.gpuTexture;
    }
    private static void prepareGpu(GameWindow window,int w,int h,Group group) {
        if(maskShader==null) {
            maskShader=new CachedShader("mosslumeShadowMask","mosslumeShadowMask");checkShader(maskShader);
            composeShader=new CachedShader("vertScene","mosslumeShadowCompose");checkShader(composeShader);
            white=new GameTexture("mosslume-shadow-solid",1,1,new byte[]{-1,-1,-1,-1});
            System.out.println("MOSSLUME_SHADOW_GPU_READY fullResolution=true lights=8 projection=SDV-Radiance");
        }
        for(int i=0;i<gpuMasks.length;i++)if(gpuMasks[i]==null||gpuMasks[i].getWidth()!=w||gpuMasks[i].getHeight()!=h){
            if(gpuMasks[i]!=null)gpuMasks[i].dispose();
            gpuMasks[i]=window.getNewFrameBuffer(w,h);
            if(!gpuMasks[i].isComplete())throw new IllegalStateException("Incomplete shadow buffer "+i);
        }
        prepareWaterTiles(group,w,h);
        if(waterMap==null||waterMap.getWidth()!=mapWidth||waterMap.getHeight()!=mapHeight) {
            if(waterMap!=null)waterMap.delete();
            waterMap=new GameTexture("mosslume-shadow-water-receivers",mapWidth,mapHeight);
            waterMap.setBlendQuality(GameTexture.BlendQuality.NEAREST);
            waterUpload=BufferUtils.createByteBuffer(mapWidth*mapHeight*4);
        }
        waterUpload.clear();
        for(boolean liquid:waterTiles) {
            byte value=(byte)(liquid?255:0);
            waterUpload.put(value).put(value).put(value).put((byte)255);
        }
        waterUpload.flip();waterMap.bindTexture(GL13.GL_TEXTURE1);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,mapWidth,mapHeight,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,waterUpload);
    }
    /** Receiver data is camera-sized, independent of total map dimensions or coordinates. */
    private static void prepareWaterTiles(Group group,int width,int height) {
        final int margin=3;
        mapOriginX=(int)Math.floor(group.cameraX/32.0)-margin;
        mapOriginY=(int)Math.floor(group.cameraY/32.0)-margin;
        // Fixed dimensions avoid GPU texture reallocation on each tile crossing.
        mapWidth=(width+31)/32+margin*2+1;
        mapHeight=(height+31)/32+margin*2+1;
        if(waterTiles==null||waterTiles.length!=mapWidth*mapHeight)waterTiles=new boolean[mapWidth*mapHeight];
        for(int y=0;y<mapHeight;y++)for(int x=0;x<mapWidth;x++)
            waterTiles[y*mapWidth+x]=RegionSceneQuery.liquid(group.level,mapOriginX+x,mapOriginY+y);
        receiverTileCount=mapWidth*mapHeight;
        receiverOriginX=mapOriginX;receiverOriginY=mapOriginY;receiverWidth=mapWidth;receiverHeight=mapHeight;
    }
    private static void prepareGpuCast(Group group,Cast c,Light light) {
        c.gpuVisible=false;
        float wx=c.x+group.cameraX,wy=c.y+group.cameraY;
        if(!light.directional&&light.energy(wx,wy)<.035f)return;
        float dx=light.directional?light.x:wx-light.x,dy=light.directional?light.y:wy-light.y;
        float distance=(float)Math.hypot(dx,dy);if(distance<.001f)return;dx/=distance;dy/=distance;
        float length=light.directional?c.height*light.radius:Math.min(c.height*1.65f,c.height*distance/Math.max(32,light.z)*.38f);
        length=Math.max(c.height*.20f,length);
        float raised=c.crown?.20f:0,height=c.mask.h*c.scale,width=c.mask.w*c.scale;
        RadianceProjection p=RadianceProjection.solid(dx,dy,length*(1-raised)/height,.62f);
        float px=c.x+dx*length*raised,py=c.y+dy*length*raised;
        float seaDrop=light.directional?10*light.radius:Math.min(18,10*distance/Math.max(32,light.z)*.38f);
        c.seaX=dx*seaDrop;c.seaY=dy*seaDrop;
        for(int row=0;row<=8;row++) {
            float v=row/8f,above=(1-v)*height,bend=c.windOffsets[row];
            int at=row*4;
            c.projectedVertices[at]=px+p.x(-width*.5f+bend,above);
            c.projectedVertices[at+1]=py+p.y(-width*.5f+bend,above);
            c.projectedVertices[at+2]=px+p.x(width*.5f+bend,above);
            c.projectedVertices[at+3]=py+p.y(width*.5f+bend,above);
        }
        c.gpuVisible=true;
        castsDrawn++;
    }
    private static void putVertex(float u,float v,float x,float y) {vertexUpload.put(u).put(v).put(x).put(y).put(0);}
    private static void drawMaskBatch(Mask mask,List<Cast> batch,int receiver) {
        int visibleCount=0;
        for(Cast c:batch)if(c.gpuVisible)visibleCount++;
        if(visibleCount==0)return;
        int required=visibleCount*20*5;
        if(vertexUpload==null||vertexUpload.capacity()<required)vertexUpload=BufferUtils.createFloatBuffer(Math.max(4096,Integer.highestOneBit(required-1)<<1));
        vertexUpload.clear();
        boolean first=true;float lastX=0,lastY=0;
        for(Cast c:batch) {
            if(!c.gpuVisible)continue;
            float ox=receiver==2?c.seaX:0,oy=receiver==2?c.seaY:0;
            // Two repeated endpoints join strips without generating a visible
            // bridge between separate trees, including opposite winding.
            if(!first){putVertex(1,1,lastX,lastY);putVertex(0,0,c.projectedVertices[0]+ox,c.projectedVertices[1]+oy);}
            for(int row=0;row<=8;row++) {
                int at=row*4;float v=row/8f;
                putVertex(0,v,c.projectedVertices[at]+ox,c.projectedVertices[at+1]+oy);
                putVertex(1,v,c.projectedVertices[at+2]+ox,c.projectedVertices[at+3]+oy);
            }
            lastX=c.projectedVertices[34]+ox;lastY=c.projectedVertices[35]+oy;first=false;
        }
        vertexUpload.flip();gpuTexture(mask).bindTexture(GL13.GL_TEXTURE0);
        GL11.glInterleavedArrays(GL11.GL_T2F_V3F,0,vertexUpload);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP,0,vertexUpload.remaining()/5);
        drawBatches++;
    }
    private static boolean nativeWater(Level level,int x,int y){
        int localX=x-mapOriginX,localY=y-mapOriginY;
        return localX<0||localY<0||localX>=mapWidth||localY>=mapHeight||waterTiles[localY*mapWidth+localX];
    }
    private static void gpuShoreEdge(Group group,Light light,float ax,float ay,float bx,float by) {
        float mx=(ax+bx)*.5f,my=(ay+by)*.5f;
        if(!light.directional&&light.energy(mx,my)<.035f)return;
        float dx=light.directional?light.x:mx-light.x,dy=light.directional?light.y:my-light.y;
        float distance=(float)Math.hypot(dx,dy);if(distance<.001f)return;
        float reach=light.directional?16*light.radius:Math.min(24,16*distance/Math.max(32,light.z)*.38f);
        dx=dx/distance*reach;dy=dy/distance*reach;
        ax-=group.cameraX;ay-=group.cameraY;bx-=group.cameraX;by-=group.cameraY;
        GL11.glColor4f(1,1,1,1);GL11.glTexCoord2f(.5f,.5f);GL11.glVertex2f(ax,ay);
        GL11.glColor4f(1,1,1,1);GL11.glTexCoord2f(.5f,.5f);GL11.glVertex2f(bx,by);
        GL11.glColor4f(1,1,1,.72f);GL11.glTexCoord2f(.5f,.5f);GL11.glVertex2f(bx+dx,by+dy);
        GL11.glColor4f(1,1,1,.72f);GL11.glTexCoord2f(.5f,.5f);GL11.glVertex2f(ax+dx,ay+dy);
        shoreQuads++;
    }
    private static void gpuShore(Group group,Light light,int width,int height) {
        int left=Math.max(0,(int)(group.cameraX-48)/32),right=Math.min(group.level.tileWidth,(int)(group.cameraX+width+48)/32+1);
        int top=Math.max(0,(int)(group.cameraY-48)/32),bottom=Math.min(group.level.tileHeight,(int)(group.cameraY+height+48)/32+1);
        maskShader.pass1i("receiverMode",2);maskShader.pass1f("heightFade",0);white.bindTexture(GL13.GL_TEXTURE0);
        GL11.glBegin(GL11.GL_QUADS);
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
            if(nativeWater(group.level,x,y))continue;
            float wx=x*32,wy=y*32;
            if(nativeWater(group.level,x,y-1))gpuShoreEdge(group,light,wx,wy,wx+32,wy);
            if(nativeWater(group.level,x,y+1))gpuShoreEdge(group,light,wx,wy+32,wx+32,wy+32);
            if(nativeWater(group.level,x-1,y))gpuShoreEdge(group,light,wx,wy,wx,wy+32);
            if(nativeWater(group.level,x+1,y))gpuShoreEdge(group,light,wx+32,wy,wx+32,wy+32);
        }
        GL11.glEnd();
    }
    private static void renderGpu(Group group) {
        GameWindow window=WindowManager.getWindow();GameFrameBuffer previous=window.getCurrentBuffer();
        int w=window.getSceneWidth(),h=window.getSceneHeight();
        int previousMatrix=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int previousArrayBuffer=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousClientTexture=GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();
        try {
            prepareGpu(window,w,h,group);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glColorMask(true,true,true,true);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,0);GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
            List<Light> visible=new ArrayList<>();
            for(Light l:group.lights)if(l.directional||l.x+l.radius>=group.cameraX&&l.x-l.radius<=group.cameraX+w&&l.y+l.radius>=group.cameraY&&l.y-l.radius<=group.cameraY+h)visible.add(l);
            visible.sort((a,b)->Float.compare(b.directional?100:b.energy(group.cameraX+w*.5f,group.cameraY+h*.5f),a.directional?100:a.energy(group.cameraX+w*.5f,group.cameraY+h*.5f)));
            if(visible.size()>8)visible.subList(8,visible.size()).clear();
            double frameTime=CrownWindRenderer.timeSeconds();
            Map<Mask,List<Cast>> batches=new LinkedHashMap<>();
            synchronized(group.casts){for(Cast c:group.casts){c.prepareWind(frameTime);batches.computeIfAbsent(c.mask,m->new ArrayList<>()).add(c);}}
            for(int i=0;i<8;i++) {
                gpuMasks[i].bindFrameBuffer();GL11.glClearColor(0,0,0,0);gpuMasks[i].clearColor();
                if(i>=visible.size())continue;
                GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_ONE,GL11.GL_ONE);GL14.glBlendEquation(GL14.GL_MAX);
                GL11.glLoadIdentity();
                maskShader.use();
                try {
                    maskShader.pass2f("cameraWorld",group.cameraX,group.cameraY);
                    maskShader.pass2f("mapSize",mapWidth,mapHeight);
                    maskShader.pass2f("mapOrigin",mapOriginX,mapOriginY);
                    maskShader.pass1i("waterMap",1);waterMap.bindTexture(GL13.GL_TEXTURE1);
                    Light light=visible.get(i);
                    synchronized(group.casts){for(Cast c:group.casts)prepareGpuCast(group,c,light);}
                    maskShader.pass1f("heightFade",.10f);GL11.glColor4f(1,1,1,1);
                    for(int receiver=1;receiver<=2;receiver++) {
                        maskShader.pass1i("receiverMode",receiver);
                        for(Map.Entry<Mask,List<Cast>> batch:batches.entrySet())drawMaskBatch(batch.getKey(),batch.getValue(),receiver);
                    }
                    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
                    if(group.shore)gpuShore(group,light,w,h);
                } finally {maskShader.stop();}
            }
            previous.bindFrameBuffer();GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            composeShader.use();
            try {
                composeShader.pass2f("cameraWorld",group.cameraX,group.cameraY);composeShader.pass2f("sceneSize",w,h);
                for(int i=0;i<8;i++) {
                    composeShader.pass1i("mask"+i,i);gpuMasks[i].bindTexture(GL13.GL_TEXTURE0+i);
                    Light l=i<visible.size()?visible.get(i):null;
                    composeShader.pass4f("source"+i,l==null?0:l.x,l==null?0:l.y,l==null?0:l.strength,l==null?1:l.directional?-1:l.radius);
                }
                GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glLoadIdentity();GL11.glColor4f(1,1,1,1);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(0,1);GL11.glVertex2f(0,0);
                GL11.glTexCoord2f(1,1);GL11.glVertex2f(w,0);
                GL11.glTexCoord2f(1,0);GL11.glVertex2f(w,h);
                GL11.glTexCoord2f(0,0);GL11.glVertex2f(0,h);
                GL11.glEnd();
            } finally {composeShader.stop();}
            lastLightCount=visible.size();frames++;
        } finally {
            previous.bindFrameBuffer();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
            GL11.glMatrixMode(previousMatrix);GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,previousArrayBuffer);GL13.glClientActiveTexture(previousClientTexture);
            GL11.glPopAttrib();
        }
    }

    private static void renderCpu(Group g) {
        int w=WindowManager.getWindow().getSceneWidth(),h=WindowManager.getWindow().getSceneHeight();
        if(w<1||h<1)return;
        if(texture==null||texture.getWidth()!=w||texture.getHeight()!=h) {
            if(texture!=null)texture.delete();
            texture=new GameTexture("mosslume-radiance-receiver",w,h);
            texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
            upload=BufferUtils.createByteBuffer(w*h*4);
            touched=new int[w*h];stamps=new int[w*h];touchedCount=0;
        }
        for(int i=0;i<touchedCount;i++)upload.putInt(touched[i]*4,0);
        touchedCount=0;
        if(++stamp==0){Arrays.fill(stamps,0);stamp=1;}
        prepareWaterTiles(g,w,h);
        List<Light> visible=new ArrayList<>();
        for(Light l:g.lights)if(l.directional||l.x+l.radius>=g.cameraX&&l.x-l.radius<=g.cameraX+w&&l.y+l.radius>=g.cameraY&&l.y-l.radius<=g.cameraY+h)visible.add(l);
        visible.sort((a,b)->Float.compare(b.energy(g.cameraX+w*.5f,g.cameraY+h*.5f),a.energy(g.cameraX+w*.5f,g.cameraY+h*.5f)));
        // The island presently has fewer than twelve emitting objects. Keep a
        // bounded working set if a player later fills the entire map with lamps.
        if(visible.size()>12)visible.subList(12,visible.size()).clear();
        Layer[] active=new Layer[visible.size()];
        double time=CrownWindRenderer.timeSeconds();
        synchronized(g.casts){for(Cast c:g.casts)c.prepareWind(time);}
        for(int i=0;i<visible.size();i++) {
            Light l=visible.get(i);Layer layer=layers.get(l.key);
            if(layer==null){layer=new Layer(w,h);layers.put(l.key,layer);}else layer.clear(w,h);
            active[i]=layer;
            synchronized(g.casts){for(Cast c:g.casts)raster(c,l,layer,g,time);}
            if(g.shore)shoreline(g,l,layer);
        }
        layers.keySet().removeIf(key->{for(Light light:visible)if(light.key==key)return false;return true;});
        for(int pixel=0;pixel<touchedCount;pixel++) {
            int at=touched[pixel],x=at%w,y=at/w;
            float blocked=0,total=.52f;
            for(int i=0;i<active.length;i++) {
                Light l=visible.get(i);float energy=l.energy(x+g.cameraX,y+g.cameraY);
                total+=energy;blocked+=energy*(active[i].pixels[at]&255)/255f;
            }
            int alpha=(int)(Math.min(.58f,blocked/total*.97f)*255);
            int dst=at*4;
            upload.put(dst,(byte)3).put(dst+1,(byte)8).put(dst+2,(byte)13).put(dst+3,(byte)alpha);
        }
        upload.position(0);upload.limit(w*h*4);texture.bindTexture();
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,upload);
        texture.initDraw().size(w,h).draw(0,0);
        lastLightCount=visible.size();frames++;
    }
    private RadianceShadowRenderer(){}
}
