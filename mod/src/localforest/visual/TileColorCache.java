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
    private final long[] frames;private long frame=Long.MIN_VALUE;
    private Map<Long,Long> outsideFrames;

    TileColorCache(TileBounds bounds) {
        this.bounds=bounds;
        values=new float[Math.multiplyExact(bounds.width(),bounds.height())][];frames=new long[values.length];java.util.Arrays.fill(frames,Long.MIN_VALUE);
    }

    float[] get(int x,int y) {
        if(bounds.contains(x,y)){int at=(y-bounds.top)*bounds.width()+x-bounds.left;return frames[at]==frame?values[at]:null;}
        return outside==null||!java.util.Objects.equals(outsideFrames.get(mixedKey(x,y)),frame)?null:outside.get(mixedKey(x,y));
    }

    void put(int x,int y,float[] value) {
        if(bounds.contains(x,y)){int at=(y-bounds.top)*bounds.width()+x-bounds.left;values[at]=value;frames[at]=frame;}
        else {
            if(outside==null){outside=new HashMap<>();outsideFrames=new HashMap<>();}
            outside.put(mixedKey(x,y),value);outsideFrames.put(mixedKey(x,y),frame);
        }
    }

    void begin(long value){frame=value;}
    float[] storage(int x,int y,int size){
        float[] old=bounds.contains(x,y)?values[(y-bounds.top)*bounds.width()+x-bounds.left]:outside==null?null:outside.get(mixedKey(x,y));
        return old!=null&&old.length==size?old:new float[size];
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
