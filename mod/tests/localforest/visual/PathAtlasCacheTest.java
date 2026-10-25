package localforest.visual;

/** Slots are shared GPU atlas coordinates; an eviction must not rewrite a still-referenced frame. */
public final class PathAtlasCacheTest {
    public static void main(String[] args){
        NaturalPathRenderer.TileSlots cache=new NaturalPathRenderer.TileSlots(8);
        NaturalPathRenderer.Entry[] first=new NaturalPathRenderer.Entry[8];
        for(int i=0;i<8;i++)first[i]=cache.acquire(i);
        check(cache.acquire(100)==null,"A current-frame path slot was evicted");
        cache.begin();check(cache.acquire(100)==null,"Previous double-buffered draw lost its atlas pixels");
        cache.begin();check(cache.acquire(0)==first[0],"Pinned survivor moved to another source rectangle");
        first[1].neighbours=255;first[1].material="old-biome";
        NaturalPathRenderer.Entry reused=cache.acquire(100);
        check(reused==first[1]&&!cache.contains(1)&&cache.contains(0),"LRU evicted a fresh slot or allocated beyond the cap");
        check(reused.neighbours==-1&&reused.material.isEmpty(),"Reused atlas rectangle retained old terrain signature");
        NaturalPathRenderer.TileSlots travelling=new NaturalPathRenderer.TileSlots(4096);
        for(int frame=0;frame<500;frame++){
            travelling.begin();
            for(int i=0;i<48;i++){
                int x=frame*48+i-12000,y=(frame%2==0?-1:1)*123456;
                NaturalPathRenderer.Entry entry=travelling.acquire(NaturalPathRenderer.coordinateKey(x,y));
                check(entry!=null,"Ordinary camera travel exhausted a 4096-tile atlas");
                check(entry.page<16,"Infinite exploration allocated a seventeenth 512px page");
            }
            check(travelling.size()<=4096,"Path cache grew with explored world area");
        }
        System.out.println("PathAtlasCacheTest PASS: current/previous pinning, LRU slot reuse, signature reset, 24000 signed-coordinate road cells bounded to 4096 tiles/16 pages");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
