package localforest.visual;

import java.util.Map;
import java.util.WeakHashMap;
import localforest.scene.RegionSceneQuery;
import localforest.scene.TileBounds;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;

/** Cache shore geometry, never sea time/color. Native terrain and region-load
 * state are sampled before draw workers, so edits invalidate next frame. */
public final class OceanGeometryCache {
    @FunctionalInterface interface LandSource { boolean land(int x,int y); }
    private static final Map<Level,Snapshot> snapshots=new WeakHashMap<>();
    public static long rebuilds;
    static final class Snapshot {
        final TileBounds vertices,geometry;
        final byte[] land;
        final float[] depths;
        // Native advColor retains this immutable array. Only depth belongs in
        // it: flowing water color/time continue to be evaluated by the shader.
        final float[][] tileColors;
        Snapshot(TileBounds vertices,LandSource source){
            this.vertices=vertices;
            geometry=new TileBounds(vertices.left-3,vertices.top-3,vertices.right+2,vertices.bottom+2);
            land=new byte[geometry.width()*geometry.height()];
            for(int y=geometry.top;y<geometry.bottom;y++)for(int x=geometry.left;x<geometry.right;x++)
                land[(y-geometry.top)*geometry.width()+x-geometry.left]=(byte)(source.land(x,y)?1:0);
            depths=new float[vertices.width()*vertices.height()];
            for(int y=vertices.top;y<vertices.bottom;y++)for(int x=vertices.left;x<vertices.right;x++)
                depths[(y-vertices.top)*vertices.width()+x-vertices.left]=direct(this::land,x,y);
            tileColors=new float[Math.max(0,vertices.width()-1)*Math.max(0,vertices.height()-1)][];
        }
        boolean land(int x,int y){return land[(y-geometry.top)*geometry.width()+x-geometry.left]!=0;}
        boolean matches(TileBounds bounds,LandSource source){
            if(!vertices.equals(bounds))return false;
            for(int y=geometry.top;y<geometry.bottom;y++)for(int x=geometry.left;x<geometry.right;x++)
                if(land(x,y)!=source.land(x,y))return false;
            return true;
        }
        float at(int x,int y){return depths[(y-vertices.top)*vertices.width()+x-vertices.left];}
        boolean containsTile(int x,int y){return vertices.contains(x,y)&&vertices.contains(x+1,y+1);}
        float[] tileColors(int x,int y){
            int index=(y-vertices.top)*(vertices.width()-1)+x-vertices.left;
            float[] colors=tileColors[index];
            if(colors==null){
                colors=new float[]{1,1,1,at(x,y),1,1,1,at(x+1,y),
                    1,1,1,at(x+1,y+1),1,1,1,at(x,y+1)};
                tileColors[index]=colors;
            }
            return colors;
        }
    }
    static Snapshot refresh(Snapshot old,TileBounds bounds,LandSource source){
        return old!=null&&old.matches(bounds,source)?old:new Snapshot(bounds,source);
    }
    public static synchronized void prepare(Level level,GameCamera camera){
        if(level.isCave)return;
        TileBounds bounds=TileBounds.fromPixels(camera.getX(),camera.getY(),camera.getWidth(),camera.getHeight(),12);
        Snapshot old=snapshots.get(level);
        Snapshot next=refresh(old,bounds,(x,y)->!RegionSceneQuery.liquid(level,x,y));
        if(next!=old){snapshots.put(level,next);rebuilds++;}
    }
    public static synchronized float shallowAt(Level level,int x,int y){
        Snapshot snapshot=snapshots.get(level);
        if(snapshot!=null&&snapshot.vertices.contains(x,y))return snapshot.at(x,y);
        return direct((xx,yy)->!RegionSceneQuery.liquid(level,xx,yy),x,y);
    }
    /** Read-only per-tile corner data, bounded by the same camera snapshot as depth. */
    public static synchronized float[] tileColors(Level level,int x,int y){
        Snapshot snapshot=snapshots.get(level);
        if(snapshot!=null&&snapshot.containsTile(x,y))return snapshot.tileColors(x,y);
        return new float[]{1,1,1,shallowAt(level,x,y),1,1,1,shallowAt(level,x+1,y),
            1,1,1,shallowAt(level,x+1,y+1),1,1,1,shallowAt(level,x,y+1)};
    }
    static float direct(LandSource source,int vx,int vy){
        double nearest=2.5;
        for(int y=vy-3;y<=vy+2;y++)for(int x=vx-3;x<=vx+2;x++)if(source.land(x,y)){
            double dx=Math.max(x-vx,Math.max(0,vx-x-1)),dy=Math.max(y-vy,Math.max(0,vy-y-1));
            nearest=Math.min(nearest,Math.hypot(dx,dy));
            if(nearest==0)return 1;
        }
        float value=(float)Math.max(0,1-nearest/2.5);
        return value*value*(3-2*value);
    }
    private OceanGeometryCache(){}
}
