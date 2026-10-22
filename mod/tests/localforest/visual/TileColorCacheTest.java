package localforest.visual;

import java.util.HashMap;
import java.util.Map;
import localforest.scene.TileBounds;

/** Regression against the previous coordinate-key map, including corners
 * outside the viewport and fresh-snapshot invalidation.
 */
public final class TileColorCacheTest {
    public static void main(String[] args) {
        for(TileBounds bounds:new TileBounds[]{new TileBounds(-20,-15,43,39),new TileBounds(61,43,124,97),
                new TileBounds(0,0,0,0),new TileBounds(1000000,-1000000,1000063,-999946)}) {
            TileColorCache cache=new TileColorCache(bounds);
            Map<Long,float[]> previous=new HashMap<>();
            for(int y=bounds.top-2;y<=bounds.bottom+2;y++)for(int x=bounds.left-2;x<=bounds.right+2;x++){
                check(cache.get(x,y)==null,"Fresh cache contained an existing color");
                float[] value={x,y,.5f,1f};cache.put(x,y,value);previous.put(key(x,y),value);
            }
            for(int y=bounds.top-2;y<=bounds.bottom+2;y++)for(int x=bounds.left-2;x<=bounds.right+2;x++)
                check(cache.get(x,y)==previous.get(key(x,y)),"Dense/fallback cache changed coordinate identity");
            int[][] remote={{Integer.MIN_VALUE,Integer.MAX_VALUE},{Integer.MAX_VALUE,Integer.MIN_VALUE},
                {-1,-1},{0,0},{1,1},{2,2},{-32,32},{32,-32}};
            for(int[] xy:remote){float[] value={xy[0],xy[1]};cache.put(xy[0],xy[1],value);previous.put(key(xy[0],xy[1]),value);}
            for(int[] xy:remote)check(cache.get(xy[0],xy[1])==previous.get(key(xy[0],xy[1])),"Fallback aliased distant coordinates");
            float[] replacement={.1f,.2f,.3f,1};cache.put(bounds.left,bounds.top,replacement);
            check(cache.get(bounds.left,bounds.top)==replacement,"Existing cell did not update");
            TileColorCache fresh=new TileColorCache(bounds);
            check(fresh.get(bounds.left,bounds.top)==null&&fresh.get(Integer.MIN_VALUE,Integer.MAX_VALUE)==null,
                "New snapshot inherited stale viewport/fallback colors");
        }
        System.out.println("TileColorCacheTest PASS: dense/fallback equivalence, negative and edge coordinates, fresh-snapshot isolation");
    }
    private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
