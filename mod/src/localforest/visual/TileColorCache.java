package localforest.visual;

import java.util.HashMap;
import java.util.Map;
import localforest.scene.TileBounds;

/** Dense camera-local color cache. The snapshot owns this instance, so its
 * replacement still invalidates every entry at the original 120ms boundary.
 */
final class TileColorCache {
    private final TileBounds bounds;
    private final float[][] values;
    private Map<Long,float[]> outside;

    TileColorCache(TileBounds bounds) {
        this.bounds=bounds;
        values=new float[Math.multiplyExact(bounds.width(),bounds.height())][];
    }

    float[] get(int x,int y) {
        if(bounds.contains(x,y))return values[(y-bounds.top)*bounds.width()+x-bounds.left];
        return outside==null?null:outside.get(mixedKey(x,y));
    }

    void put(int x,int y,float[] value) {
        if(bounds.contains(x,y))values[(y-bounds.top)*bounds.width()+x-bounds.left]=value;
        else {
            if(outside==null)outside=new HashMap<>();
            outside.put(mixedKey(x,y),value);
        }
    }

    private static long mixedKey(int x,int y) {
        // Long.hashCode of a plain packed coordinate is x^y, which sends
        // neighboring tiles into the same few buckets. This bijective 64-bit
        // permutation preserves coordinate identity and spreads that hash.
        long key=((long)x<<32)|(y&0xffffffffL);
        key=(key^(key>>>30))*0xbf58476d1ce4e5b9L;
        key=(key^(key>>>27))*0x94d049bb133111ebL;
        return key^(key>>>31);
    }
}
