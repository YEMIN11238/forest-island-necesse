package localforest.visual;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.GameShader;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/**
 * Native-order adaptation of libGDX SpriteBatch's bounded vertex buffer,
 * texture/state flush and indexed submission (Apache-2.0, see third-party).
 * Per-sprite wind/light/atlas data travels in vertices, not changing uniforms.
 * No depth sorting is changed and native draw boundaries explicitly flush.
 */
public final class CrownSpriteBatch {
    private static final int CAPACITY=256, FLOATS=40, VERTICES=18, INDICES=48;
    private static final FloatBuffer upload=BufferUtils.createFloatBuffer(CAPACITY*VERTICES*FLOATS);
    private static final float[] sprite=new float[FLOATS];
    private static final IdentityHashMap<GameTexture,Region> regions=new IdentityHashMap<>();
    private static GameTexture atlas,normalAtlas;
    private static GameShader shader;
    private static int vertexBuffer,indexBuffer,count,scope;
    private static final int[] attributes=new int[10];
    private static final String[] ATTRIBUTE_NAMES={"aPositionUV","aFootSize","aShape","aWind","aAtlasRect","aLamp0","aLamp1","aLamp2","aLight0","aLight1"};
    // Three exact RGB slots, no quantization. A fourth distinct color flushes
    // in native order. Positions/radii/strengths travel with each sprite so
    // GLSL 1.20 never dynamically indexes a large uniform array per fragment.
    private static final float[][] palette=new float[3][3];
    private static int paletteCount;
    private static Level level;
    private static boolean unavailable,flushing;
    public static long draws,sprites,nativeBoundaries,atlasMisses;
    public static int maxBatch;
    /** Uniform values belong to this program; unchanged frame/state data must
     * not cause a driver lookup and upload at every native-order flush. */
    private static final class BatchShader extends GameShader {
        private final Map<String,Integer> locations=new HashMap<>(),integers=new HashMap<>();
        private final Map<String,float[]> values=new HashMap<>();
        BatchShader(){super("mosslumeBatchWind","mosslumeBatchWind");}
        @Override public int getUniform(String name){
            Integer old=locations.get(name);
            if(old==null){old=super.getUniform(name);locations.put(name,old);}
            return old;
        }
        private boolean changed(String name,float a,float b,float c,float d){
            float[] old=values.get(name);
            if(old!=null&&old[0]==a&&old[1]==b&&old[2]==c&&old[3]==d)return false;
            if(old==null){old=new float[4];values.put(name,old);}
            old[0]=a;old[1]=b;old[2]=c;old[3]=d;return true;
        }
        @Override public void pass1f(String n,float a){if(changed(n,a,0,0,0))super.pass1f(n,a);}
        @Override public void pass2f(String n,float a,float b){if(changed(n,a,b,0,0))super.pass2f(n,a,b);}
        @Override public void pass3f(String n,float a,float b,float c){if(changed(n,a,b,c,0))super.pass3f(n,a,b,c);}
        @Override public void pass4f(String n,float a,float b,float c,float d){if(changed(n,a,b,c,d))super.pass4f(n,a,b,c,d);}
        @Override public void pass1i(String n,int a){Integer old=integers.get(n);if(old==null||old!=a){integers.put(n,a);super.pass1i(n,a);}}
    }

    private static final class Region {
        final int x,y,w,h;
        Region(int x,int y,int w,int h){this.x=x;this.y=y;this.w=w;this.h=h;}
    }
    public static boolean enabled(){
        return !"false".equalsIgnoreCase(System.getProperty("mosslume.wind.batch","true"))
            &&!Boolean.getBoolean("mosslume.wind.immediate")&&!CrownWindRenderer.isLegacy()&&!unavailable;
    }

    /** Copies original untrimmed pixels once; no rotation, scaling or pivot change. */
    public static void prepareAtlas(Map<String,GameTexture> art) {
        if(!enabled())return;
        flush();regions.clear();if(atlas!=null)atlas.delete();if(normalAtlas!=null)normalAtlas.delete();
        Set<String> skip=Set.of("glow","smoke","mote","ground","dirt","deep","shallow","ocean-art","shore","earth");
        ArrayList<Map.Entry<String,GameTexture>> entries=new ArrayList<>(art.entrySet());
        entries.removeIf(e->e.getKey().endsWith("-mask")||e.getKey().startsWith("flame-")||skip.contains(e.getKey()));
        entries.sort(Comparator.<Map.Entry<String,GameTexture>>comparingInt(e->-e.getValue().getHeight()).thenComparing(Map.Entry::getKey));
        int x=2,y=2,row=0,width=1024;
        for(Map.Entry<String,GameTexture> entry:entries) {
            GameTexture t=entry.getValue();if(regions.containsKey(t))continue;
            if(t.getWidth()+4>width)continue;
            if(x+t.getWidth()+2>width){x=2;y+=row+4;row=0;}
            regions.put(t,new Region(x,y,t.getWidth(),t.getHeight()));
            x+=t.getWidth()+4;row=Math.max(row,t.getHeight());
        }
        int height=Integer.highestOneBit(Math.max(1,y+row+3))<<1;
        atlas=new GameTexture("mosslume-wind-batch-atlas",width,height);
        atlas.setBlendQuality(GameTexture.BlendQuality.NEAREST);
        normalAtlas=new GameTexture("mosslume-normal-batch-atlas",width,height);normalAtlas.setBlendQuality(GameTexture.BlendQuality.NEAREST);
        for(Map.Entry<GameTexture,Region> e:regions.entrySet()){
            atlas.copy(e.getKey(),e.getValue().x,e.getValue().y);
            GameTexture normal=VisualMaterials.normal(e.getKey());if(normal!=null)normalAtlas.copy(normal,e.getValue().x,e.getValue().y);
        }
        atlas.makeFinal();normalAtlas.makeFinal();
        System.out.println("MOSSLUME_WIND_ATLAS entries="+regions.size()+" size="+width+"x"+height+" trim=false rotate=false");
    }

    private static void prepareShader() {
        if(shader!=null)return;
        shader=new BatchShader();
        if(GL20.glGetProgrami(shader.getProgram(),GL20.GL_LINK_STATUS)==GL11.GL_FALSE)
            throw new IllegalStateException(GL20.glGetProgramInfoLog(shader.getProgram()));
        for(int i=0;i<attributes.length;i++){
            attributes[i]=GL20.glGetAttribLocation(shader.getProgram(),ATTRIBUTE_NAMES[i]);
            if(attributes[i]<0)throw new IllegalStateException("Inactive batch attribute "+ATTRIBUTE_NAMES[i]);
        }
        vertexBuffer=GL15.glGenBuffers();indexBuffer=GL15.glGenBuffers();
        int previous=GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        ShortBuffer indices=BufferUtils.createShortBuffer(CAPACITY*INDICES);
        for(int i=0;i<CAPACITY;i++)for(int row=0;row<8;row++) {
            int base=i*VERTICES+row*2;
            indices.put((short)base).put((short)(base+1)).put((short)(base+2));
            indices.put((short)(base+2)).put((short)(base+1)).put((short)(base+3));
        }
        indices.flip();GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,indexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER,indices,GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,previous);
        System.out.println("MOSSLUME_WIND_BATCH_READY upstream=libgdx-03799a27 segments=8 maxSprites="+CAPACITY);
    }

    static void begin(){scope++;}
    static void end(){try{flush();}finally{scope=Math.max(0,scope-1);level=null;}}

    private static int colorIndex(SceneLighting.Source source){
        if(source==null)return -1;
        for(int i=0;i<paletteCount;i++)if(palette[i][0]==source.r&&palette[i][1]==source.g&&palette[i][2]==source.b)return i;
        return -1;
    }
    private static int paletteSlot(SceneLighting.Source source){
        if(source==null)return 0;
        int i=colorIndex(source);
        if(i<0){i=paletteCount++;palette[i][0]=source.r;palette[i][1]=source.g;palette[i][2]=source.b;}
        return i+1;
    }
    private static void lamp(int offset,SceneLighting.Source source){
        sprite[offset]=source==null?0:source.x;sprite[offset+1]=source==null?0:source.y;
        sprite[offset+2]=source==null?0:source.z;sprite[offset+3]=source==null?1:source.radius;
    }

    static boolean add(GameTexture texture,int footX,int footY,float scale,GameLight light,float alpha,
            int tileX,int tileY,boolean crown,Level world,boolean wind,float elevation) {
        if(scope==0||!enabled()||atlas==null||world==null||!SceneLighting.enabled())return false;
        Region region=regions.get(texture);
        if(region==null){atlasMisses++;return false;}
        List<SceneLighting.Source> sources=SceneLighting.sources(world);
        if(count==CAPACITY||world!=level){flush();level=world;}
        if(shader==null) {
            try{prepareShader();}
            catch(RuntimeException failure){unavailable=true;System.err.println("MOSSLUME_WIND_BATCH_FAILED "+failure);return false;}
        }
        VisualMaterialDefinition material=VisualMaterials.get(texture);
        float width=Math.round(texture.getWidth()*scale),height=Math.round(texture.getHeight()*scale);
        sprite[4]=tileX*32+16;sprite[5]=tileY*32+29;sprite[6]=width;sprite[7]=height;
        sprite[8]=material==null?elevation:material.effectiveShadowHeight(scale);sprite[9]=crown?1:0;sprite[10]=wind&&CrownWindRenderer.usesPixelCoverage()?1:0;sprite[11]=alpha;
        sprite[12]=material==null?(crown?WindField.CROWN_SPEED:WindField.PLANT_SPEED):material.windSpeed;
        sprite[13]=wind?width*(material==null?(crown?WindField.CROWN_AMPLITUDE:WindField.PLANT_AMPLITUDE):material.windAmplitude):0;
        sprite[14]=material==null?(crown?.10f:.07f):material.windPivot;sprite[15]=crown?1.5f:1.9f;
        SceneLighting.Source first=null,second=null,third=null;float a=0,b=0,c=0;
        for(int i=0;i<sources.size();i++){
            float energy=SceneLighting.energy(sources.get(i),sprite[4],sprite[5]);
            if(energy>a){third=second;c=b;second=first;b=a;first=sources.get(i);a=energy;}
            else if(energy>b){third=second;c=b;second=sources.get(i);b=energy;}
            else if(energy>c){third=sources.get(i);c=energy;}
        }
        int missing=(first!=null&&colorIndex(first)<0?1:0)+(second!=null&&colorIndex(second)<0?1:0)+(third!=null&&colorIndex(third)<0?1:0);
        // Counting duplicate missing colors conservatively can flush early,
        // never discard a light or change its RGB/strength.
        if(paletteCount+missing>3)flush();
        sprite[16]=region.x;sprite[17]=region.y;sprite[18]=region.w;sprite[19]=region.h;
        lamp(20,first);lamp(24,second);lamp(28,third);
        sprite[32]=paletteSlot(first);sprite[33]=first==null?0:first.strength*SceneLighting.flicker(first);
        sprite[34]=paletteSlot(second);sprite[35]=second==null?0:second.strength*SceneLighting.flicker(second);
        sprite[36]=paletteSlot(third);sprite[37]=third==null?0:third.strength*SceneLighting.flicker(third);
        sprite[38]=WindField.phase(tileX,tileY);sprite[39]=0;
        float left=material==null?footX-width*.5f:material.left(footX,scale);
        float top=material==null?footY-height:material.top(footY,scale);
        for(int row=0;row<=8;row++){
            float v=row/8f,y=top+height*v;
            upload.put(left).put(y).put(0).put(v).put(sprite,4,FLOATS-4);
            upload.put(left+width).put(y).put(1).put(v).put(sprite,4,FLOATS-4);
        }
        count++;sprites++;return true;
    }

    /** libGDX's flush boundary pattern, adapted to the native shader stack. */
    public static void flush() {
        if(count==0||flushing)return;
        flushing=true;
        int previousArray=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int previousElement=GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        TextureStateScope.begin();
        try {
            shader.use();
            shader.pass1f("windTime",(float)CrownWindRenderer.timeSeconds());
            shader.pass2f("inverseAtlasSize",1f/atlas.getWidth(),1f/atlas.getHeight());
            shader.pass1f("smoothing_factor",1f);
            shader.pass1f("nightAmount",SceneLighting.night(level));
            shader.pass3f("dayMaterial",DaylightMaterials.amount(level),DaylightMaterials.CROWN_SATURATION,DaylightMaterials.CROWN_BRIGHTNESS);
            float[] ambient=SceneLighting.ambient(level,false);
            shader.pass3f("ambientRGB",ambient[0],ambient[1],ambient[2]);
            double angle=(level.getWorldEntity().getDayTimeHourFloat()-12)*.055;
            shader.pass3f("skyDirection",(float)(-.65+Math.sin(angle)*.25),-.6f,.9f);
            for(int i=0;i<3;i++)shader.pass3f(i==0?"palette0":i==1?"palette1":"palette2",palette[i][0],palette[i][1],palette[i][2]);
            shader.pass1i("normalTexture",1);normalAtlas.bindTexture(GL13.GL_TEXTURE1);
            SurfaceOcclusion.bind(shader,level);
            atlas.bindTexture(GL13.GL_TEXTURE0);GL11.glLoadIdentity();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,vertexBuffer);
            upload.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER,(long)upload.capacity()*Float.BYTES,GL15.GL_STREAM_DRAW);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER,0,upload);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,indexBuffer);
            for(int i=0;i<attributes.length;i++){
                GL20.glEnableVertexAttribArray(attributes[i]);
                GL20.glVertexAttribPointer(attributes[i],4,GL11.GL_FLOAT,false,FLOATS*Float.BYTES,(long)i*4*Float.BYTES);
            }
            GL11.glDrawElements(GL11.GL_TRIANGLES,count*INDICES,GL11.GL_UNSIGNED_SHORT,0L);
            draws++;maxBatch=Math.max(maxBatch,count);
        } finally {
            for(int attribute:attributes)GL20.glDisableVertexAttribArray(attribute);
            try{shader.stop();}finally{TextureStateScope.end();}
            GL11.glPopClientAttrib();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,previousArray);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,previousElement);
            upload.clear();count=0;paletteCount=0;flushing=false;
        }
    }

    public static LevelSortedDrawable sceneDrawable(GameObject object,int x,int y,int sort,Runnable run){
        return new SceneDrawable(object,x,y,sort,run);
    }
    private static final class SceneDrawable extends LevelSortedDrawable {
        private final Runnable draw;
        SceneDrawable(GameObject o,int x,int y,int sort,Runnable draw){
            super(o,x,false);this.sortY=sort;this.draw=draw;this.tileY=y;init();
        }
        private int sortY,tileY;
        @Override public int getSortY(){return tileY*32+sortY;}
        @Override public void draw(TickManager tick){draw.run();}
    }
    private static final class Boundary extends LevelSortedDrawable {
        final LevelSortedDrawable original;boolean last;
        Boundary(LevelSortedDrawable original){super(original,false);this.original=original;}
        @Override public int getSortY(){return original.getSortY();}
        @Override public int compareTo(LevelSortedDrawable other){
            return original.compareTo(other instanceof Boundary?((Boundary)other).original:other);
        }
        @Override public void draw(TickManager tick){
            boolean nativeDraw=!(original instanceof SceneDrawable);
            if(nativeDraw){nativeBoundaries++;flush();}
            try{original.draw(tick);}
            finally{if(nativeDraw||last)flush();}
        }
    }
    private static final class OrderedList extends ArrayList<LevelSortedDrawable> {
        OrderedList(List<LevelSortedDrawable> list){super(list.size());for(LevelSortedDrawable d:list)add(new Boundary(d));}
        @Override public void sort(Comparator<? super LevelSortedDrawable> comparator){
            super.sort(comparator);if(!isEmpty())((Boundary)get(size()-1)).last=true;
        }
    }
    public static List<LevelSortedDrawable> boundaries(List<LevelSortedDrawable> sorted){
        return enabled()?new OrderedList(sorted):sorted;
    }
    private CrownSpriteBatch(){}
}
