package localforest.world;

import java.util.*;

/** Determinism, configuration and playability invariants, independent of native rendering. */
public final class GeneratedLayoutTest {
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        int checked=0;
        for(String preset:WorldContent.load(1).presets)for(String seed:List.of("mosslume","苔光","12345","night-forest","north","south","a","b")){
            String encoded=WorldDefinition.encodeSeed(seed,preset,"standard");
            WorldDefinition d=WorldDefinition.fromSeed(encoded);
            require(d.userSeed.equals(seed),"Seed roundtrip");
            GeneratedLayout g=new GeneratedLayout(d),second=new GeneratedLayout(WorldDefinition.fromSeed(encoded));
            long forward=0,reverse=0;int meadow=0,forest=0,trees=0;
            List<Integer> order=new ArrayList<>();for(int i=0;i<d.width*d.height;i++)order.add(i);
            for(int i:order){int x=i%d.width,y=i/d.width;forward^=fingerprint(g,x,y);String b=g.biome(x,y);if(g.land(x,y)){if(b.equals("meadow"))meadow++;else forest++;}String o=g.object(x,y);if(o!=null&&o.endsWith("tree"))trees++;}
            Collections.reverse(order);for(int i:order)reverse^=fingerprint(second,i%d.width,i/d.width);
            require(forward==reverse,"Generation depends on traversal order");
            require(meadow>100&&forest>100,"Missing second biome for "+preset+" "+seed);
            require(trees>50,"Missing forest resources");
            require(g.land(d.spawnX,d.spawnY)&&g.object(d.spawnX,d.spawnY)==null,"Blocked spawn");
            require(reachable(g,d.spawnX,d.spawnY,d.caveX,d.caveY),"Cave entrance unreachable");
            require(reachable(g,d.spawnX,d.spawnY,d.houseX,d.houseY),"House entrance unreachable");
            for(int x=0;x<d.width;x++)require(!g.land(x,0)&&!g.land(x,d.height-1),"Unbounded map");
            for(int y=0;y<d.height;y++)require(!g.land(0,y)&&!g.land(d.width-1,y),"Unbounded map");
            require(d.content.campObjects.stream().filter(o->o.role().endsWith("storage")).count()==2,"Storage template missing");
            checked++;
        }
        require(WorldDefinition.fromSeed("old-vanilla-seed")==null,"Old world misclassified");
        try{WorldDefinition.fromSeed("MOSS:2:1:forest_island:standard:YQ");throw new AssertionError("Unknown generator accepted");}catch(IllegalArgumentException expected){}
        try{WorldDefinition.encodeSeed("a","unknown","standard");throw new AssertionError("Unknown preset accepted");}catch(IllegalArgumentException expected){}
        try{String encoded=WorldDefinition.encodeSeed("a","forest_island","standard");WorldDefinition.fromSeed(encoded.substring(0,encoded.lastIndexOf(':')+1)+"bad-hash");throw new AssertionError("Changed content accepted");}catch(IllegalArgumentException expected){}
        try{WorldDefinition.fromSeed("MOSS:1:1:forest_island:standard:YQ");throw new AssertionError("Missing content hash accepted");}catch(IllegalArgumentException expected){}
        System.out.println("GeneratedLayoutTest PASS worlds="+checked+" traversalOrder=stable biomeCoverage=true bounds=true resourceAccess=true legacySeedPreserved=true unknownVersionRejected=true");
    }
    private static long fingerprint(GeneratedLayout g,int x,int y){String o=g.object(x,y);return g.hash(x,y,21)^((long)(g.land(x,y)?1:0)<<62)^((long)g.biome(x,y).hashCode()<<11)^(o==null?0:o.hashCode());}
    private static boolean reachable(GeneratedLayout g,int sx,int sy,int tx,int ty){
        boolean[][] seen=new boolean[g.world.width][g.world.height];ArrayDeque<int[]> queue=new ArrayDeque<>();queue.add(new int[]{sx,sy});seen[sx][sy]=true;
        while(!queue.isEmpty()){int[] p=queue.remove();if(p[0]==tx&&p[1]==ty)return true;for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=p[0]+delta[0],y=p[1]+delta[1];if(x<0||y<0||x>=g.world.width||y>=g.world.height||seen[x][y]||!g.land(x,y))continue;String o=g.object(x,y);if(o!=null&&(o.endsWith("tree")||o.endsWith("bush")||o.contains("rock")))continue;seen[x][y]=true;queue.add(new int[]{x,y});}}
        return false;
    }
}
