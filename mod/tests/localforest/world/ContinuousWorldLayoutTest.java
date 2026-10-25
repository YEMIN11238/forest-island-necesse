package localforest.world;

import java.util.HashSet;
import java.util.Set;

/** Version isolation, negative coordinates and exploration-order independence. */
public final class ContinuousWorldLayoutTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String seed=WorldDefinition.encodeSeed("苔光-continuous","continuous","standard");
        WorldDefinition d=WorldDefinition.fromSeed(seed);
        require(d.isContinuous()&&d.generatorVersion==4&&d.width==0&&d.height==0,"Not a native unbounded world");
        require(WorldDefinition.fromSeed(seed)==d,"Immutable definition cache not reused");
        ContinuousWorldLayout a=new ContinuousWorldLayout(d),b=new ContinuousWorldLayout(WorldDefinition.fromSeed(seed));
        Set<String> habitats=new HashSet<>();long forward=0,reverse=0;
        for(int x=-4096;x<=4096;x+=37)for(int y=-4096;y<=4096;y+=41){
            String biome=a.biome(x,y);habitats.add(biome);forward^=a.hash(x,y,6)^biome.hashCode();
        }
        int nx=8192/37,ny=8192/41;
        for(int ix=nx;ix>=0;ix--)for(int iy=ny;iy>=0;iy--){int x=-4096+ix*37,y=-4096+iy*41;reverse^=b.hash(x,y,6)^b.biome(x,y).hashCode();}
        require(forward==reverse,"Biome depends on exploration order");
        require(habitats.size()==7,"Missing repeated biomes: "+habitats);
        require(a.safeCamp(d.spawnX,d.spawnY)&&a.reserved(d.spawnX,d.spawnY),"Unsafe spawn");
        for(int x=d.campX-29;x<d.campX+30;x++)for(int y=d.campY-29;y<d.campY+30;y++)
            if(a.safeCamp(x,y))require(a.biome(x,y).equals("forest"),"Home biome torn by remote cell");
        for(int cx=-12;cx<=12;cx++)for(int cy=-12;cy<=12;cy++){
            int candidates=0;
            for(int dx=0;dx<4;dx++)for(int dy=0;dy<4;dy++)if(a.outpostRegion(cx*4+dx,cy*4+dy))candidates++;
            require(candidates<=1,"Duplicate outpost within a macrocell");
        }
        require(!WorldDefinition.fromSeed(WorldDefinition.encodeSeed("old","biomes","standard")).isContinuous(),"Existing v3 encoding silently changed");
        require(WorldDefinition.fromSeed(WorldDefinition.encodeSeed("old","woodland","standard")).generatorVersion==1,"Existing v1 encoding changed");
        try{new GeneratedLayout(d);throw new AssertionError("Continuous world allocated finite plan");}catch(IllegalArgumentException expected){}
        System.out.println("ContinuousWorldLayoutTest PASS nativeBounds=0x0 versionsIsolated=true biomes=7 negativeCoordinates=true traversalOrder=stable finiteArrays=forbidden sites=bounded");
    }
}
