package localforest.visual;

import java.awt.Color;
import java.util.*;
import localforest.scene.RegionSceneQuery;
import localforest.scene.SceneStructures;
import localforest.scene.StructureInstance;
import localforest.scene.TileBounds;
import necesse.level.maps.Level;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.light.GameLight;
import necesse.gfx.shader.GameShader;

/** Render-only shared light field. Native visibility, AI and light propagation stay authoritative. */
public final class SceneLighting {
    public static final class Source {
        public final long key;
        public final float x,y,z,strength,radius,r,g,b;
        private long flickerFrame=Long.MIN_VALUE;private float flickerValue=1;
        Source(long key,float x,float y,float z,float strength,float radius,float r,float g,float b){
            this.key=key;this.x=x;this.y=y;this.z=z;this.strength=strength;this.radius=radius;this.r=r;this.g=g;this.b=b;
        }
    }
    private static final class Snapshot {
        final long time;final TileBounds bounds;final List<Source> sources;
        List<RadianceShadowRenderer.Light> shadows;long shadowFrame=Long.MIN_VALUE;
        final TileColorCache terrainSmooth,terrainFlat,points;
        Snapshot(long time,TileBounds bounds,List<Source> sources){
            this.time=time;this.bounds=bounds;this.sources=Collections.unmodifiableList(sources);
            terrainSmooth=new TileColorCache(bounds);terrainFlat=new TileColorCache(bounds);
            // Smooth-light samples include the right/bottom corner of the
            // final tile, one point beyond the half-open tile rectangle.
            points=new TileColorCache(new TileBounds(bounds.left,bounds.top,bounds.right+1,bounds.bottom+1));
        }
    }
    private static final Map<Level,Snapshot> cache=new WeakHashMap<>();
    public static long terrainSamples,spritePasses,lightTilesScanned;
    public static int lastQueryTiles;
    public static boolean enabled(){return !Boolean.getBoolean("mosslume.lighting.legacy");}
    public static synchronized List<Source> sources(Level level){
        long time=level.getWorldEntity().getWorldTime();
        Snapshot old=cache.get(level);
        TileBounds bounds=RegionSceneQuery.visibleLights(level);
        if(old!=null&&bounds.equals(old.bounds)&&time>=old.time&&time-old.time<120)return old.sources;
        ArrayList<Source> lights=new ArrayList<>();
        lastQueryTiles=0;
        for(int y=bounds.top;y<bounds.bottom;y++)for(int x=bounds.left;x<bounds.right;x++){
            if(!RegionSceneQuery.loaded(level,x,y))continue;
            lightTilesScanned++;lastQueryTiles++;
            GameObject o=level.getObject(x,y);int nativeLevel=o.getLightLevel(level,0,x,y);
            if(nativeLevel<=0)continue;
            boolean tall=o.getStringID().contains("torch");
            lights.add(new Source(((long)y<<32)|(x&0xffffffffL),x*32+16,y*32+20,
                tall?56:36,Math.min(1.2f,nativeLevel/100f)*(.10f+.90f*night(level)),tall?128:152,1f,.73f,.22f));
        }
        float night=night(level);
        if(night>.01f){
            int index=0;
            for(StructureInstance house:SceneStructures.houses(level)){
                if(bounds.contains(house.footTileX,house.footTileY)&&SceneStructures.exists(level,house)){
                    lights.add(new Source(-10-index*2,house.windowX(0),house.windowY(),34,.42f*night,104,1f,.73f,.28f));
                    lights.add(new Source(-11-index*2,house.windowX(1),house.windowY(),34,.42f*night,104,1f,.73f,.28f));
                }
                index++;
            }
        }
        Snapshot result=new Snapshot(time,bounds,lights);cache.put(level,result);return result.sources;
    }
    public static float night(Level level){
        if(level.isCave)return 1f;
        float h=level.getWorldEntity().getDayTimeHourFloat();
        return h>=18?Math.min(1,(h-18)/1.5f):h<7?Math.min(1,(7-h)/1.5f):0;
    }
    public static synchronized List<RadianceShadowRenderer.Light> shadowSources(Level level){
        List<Source> lights=sources(level);Snapshot snapshot=cache.get(level);
        long frame=Double.doubleToLongBits(CrownWindRenderer.timeSeconds());
        if(snapshot.shadows!=null&&snapshot.shadowFrame==frame)return snapshot.shadows;
        snapshot.shadowFrame=frame;
        ArrayList<RadianceShadowRenderer.Light> out=new ArrayList<>();
        double angle=(level.getWorldEntity().getDayTimeHourFloat()-12)*.055;
        float dx=(float)(.65-Math.sin(angle)*.25),dy=.60f;
        float length=(float)Math.hypot(dx,dy);
        if(!level.isCave)out.add(new RadianceShadowRenderer.Light(-1,dx/length,dy/length,200,.48f-.34f*night(level),.78f,true));
        for(Source s:lights)out.add(new RadianceShadowRenderer.Light(s.key,s.x,s.y,s.z,s.strength*flicker(s),s.radius,false));
        snapshot.shadows=Collections.unmodifiableList(out);return snapshot.shadows;
    }
    public static float[] ambient(Level level,boolean water){
        float n=night(level);
        // Cool, readable moonlight; warm light comes from bounded local sources.
        // Keep emission sprites out of this field so flames and windows stay bright.
        return water?new float[]{1-.83f*n,1-.73f*n,1-.56f*n}
            :new float[]{1-.80f*n,1-.68f*n,1-.64f*n};
    }
    public static float flicker(Source s){
        if(s.key<0)return 1;
        long frame=Double.doubleToLongBits(CrownWindRenderer.timeSeconds());
        if(s.flickerFrame==frame)return s.flickerValue;
        double t=CrownWindRenderer.timeSeconds(),p=(s.key&0xffff)*.83;
        s.flickerValue=(float)(.97+.018*Math.sin(t*3.1+p)+.012*Math.sin(t*6.7+p));s.flickerFrame=frame;return s.flickerValue;
    }
    public static float energy(Source s,float wx,float wy){
        float d=(float)Math.hypot(wx-s.x,wy-s.y)/s.radius;
        if(d>=1)return 0;
        float fall=1-d*d*(3-2*d);
        return fall*fall*s.strength*flicker(s);
    }
    public static float[] color(Level level,float wx,float wy,boolean water){
        return color(level,wx,wy,water,new float[3]);
    }
    private static float[] color(Level level,float wx,float wy,boolean water,float[] rgb){
        float n=night(level);
        rgb[0]=1-(water?.83f:.80f)*n;rgb[1]=1-(water?.73f:.68f)*n;rgb[2]=1-(water?.56f:.64f)*n;
        for(Source s:sources(level)){
            float e=energy(s,wx,wy)*.50f;
            rgb[0]+=s.r*e;rgb[1]+=s.g*e;rgb[2]+=s.b*e;
        }
        for(int i=0;i<3;i++)rgb[i]=Math.min(1.18f,rgb[i]);
        return rgb;
    }
    public static GameLight light(Level level,float wx,float wy){
        float[] c=color(level,wx,wy,false);
        return level.lightManager.newLight(new Color(Math.min(1,c[0]),Math.min(1,c[1]),Math.min(1,c[2])),1f,150);
    }
    public static synchronized float[] terrain(Level level,int tx,int ty,boolean smooth){
        sources(level);Snapshot snapshot=cache.get(level);
        long frame=Double.doubleToLongBits(CrownWindRenderer.timeSeconds());
        snapshot.terrainSmooth.begin(frame);snapshot.terrainFlat.begin(frame);snapshot.points.begin(frame);
        TileColorCache fields=smooth?snapshot.terrainSmooth:snapshot.terrainFlat;
        float[] cached=fields.get(tx,ty);terrainSamples++;if(cached!=null)return cached;
        float[] out=fields.storage(tx,ty,16);
        for(int i=0;i<4;i++){
            int ox=(i==1||i==2)?1:0,oy=i>=2?1:0;
            float wx=(tx+.5f+(smooth?ox:0))*32,wy=(ty+.5f+(smooth?oy:0))*32;
            int px=tx+(smooth?ox:0),py=ty+(smooth?oy:0);
            float[] c=snapshot.points.get(px,py);
            if(c==null){boolean water=!IslandVisuals.land(level,px,py);c=color(level,wx,wy,water,snapshot.points.storage(px,py,3));snapshot.points.put(px,py,c);}
            out[i*4]=Math.min(1,c[0]);out[i*4+1]=Math.min(1,c[1]);out[i*4+2]=Math.min(1,c[2]);out[i*4+3]=1;
        }
        fields.put(tx,ty,out);return out;
    }
    /** Uniforms are consumed by both the wind mesh and rigid props, in world coordinates. */
    public static void bind(GameShader shader,Level level,float wx,float wy,float width,float height,float elevation,boolean rounded){
        shader.pass1i("surfaceLighting",1);
        shader.pass1f("nightAmount",night(level));
        float[] a=ambient(level,false);shader.pass3f("ambientRGB",a[0],a[1],a[2]);
        shader.pass2f("worldFoot",wx,wy);shader.pass2f("spriteSize",width,height);
        shader.pass1f("elevation",elevation);shader.pass1i("roundedSurface",rounded?1:0);
        double angle=(level.getWorldEntity().getDayTimeHourFloat()-12)*.055;
        shader.pass3f("skyDirection",(float)(-.65+Math.sin(angle)*.25),-.6f,.9f);
        List<Source> source=sources(level);
        Source first=null,second=null,third=null;float a1=0,a2=0,a3=0;
        for(Source s:source){float e=energy(s,wx,wy);if(e>a1){third=second;a3=a2;second=first;a2=a1;first=s;a1=e;}else if(e>a2){third=second;a3=a2;second=s;a2=e;}else if(e>a3){third=s;a3=e;}}
        bindLamp(shader,"lamp0","lampColor0",first);
        bindLamp(shader,"lamp1","lampColor1",second);
        bindLamp(shader,"lamp2","lampColor2",third);
        spritePasses++;
    }
    private static void bindLamp(GameShader shader,String lampName,String colorName,Source s){
        shader.pass4f(lampName,s==null?0:s.x,s==null?0:s.y,s==null?0:s.z,s==null?1:s.radius);
        shader.pass4f(colorName,s==null?0:s.r,s==null?0:s.g,s==null?0:s.b,s==null?0:s.strength*flicker(s));
    }
    public static synchronized void invalidate(Level level){cache.remove(level);SurfaceOcclusion.invalidate(level);}
    private SceneLighting(){}
}
