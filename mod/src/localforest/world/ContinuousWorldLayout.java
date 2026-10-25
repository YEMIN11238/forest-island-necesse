package localforest.world;

import java.util.LinkedHashMap;
import java.util.Map;
import localforest.world.noise.FastNoiseLite;

/** Bounded coordinate sampler for v4. No world-sized arrays or visit-order RNG.
 * Native BiomeGeneratorStack still owns ocean, river and cave generation. */
public final class ContinuousWorldLayout {
    private static final Map<String,ContinuousWorldLayout> cache=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,ContinuousWorldLayout> e){return size()>16;}
    };
    private static final String[] HABITATS={"forest","forest","forest","forest","wetland","mushroom","ruins","snowfield","desert","volcano"};
    public final WorldDefinition world;
    public final CoastalCampLayout coast;
    private final FastNoiseLite warpX,warpY;
    public ContinuousWorldLayout(WorldDefinition world){
        if(!world.isContinuous())throw new IllegalArgumentException("Continuous world required");
        this.world=world;warpX=noise(world.seed^0x7163e1L);warpY=noise(world.seed^0x449bacL);coast=new CoastalCampLayout(this);
    }
    public static synchronized ContinuousWorldLayout forWorld(WorldDefinition world){return cache.computeIfAbsent(world.encodedSeed,key->new ContinuousWorldLayout(world));}
    private static FastNoiseLite noise(long seed){
        FastNoiseLite n=new FastNoiseLite((int)(seed^(seed>>>32)));n.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);n.SetFrequency(.009f);return n;
    }
    public long hash(int x,int y,int channel){
        long h=world.seed^(long)x*0x9e3779b97f4a7c15L^(long)y*0xc2b2ae3d27d4eb4fL^(long)channel*0x165667b19e3779f9L;
        h=(h^(h>>>30))*0xbf58476d1ce4e5b9L;h=(h^(h>>>27))*0x94d049bb133111ebL;return h^(h>>>31);
    }
    public double random(int x,int y,int channel){return (hash(x,y,channel)>>>11)*0x1.0p-53;}
    public String biome(int x,int y){
        if(Math.hypot((double)x-world.campX,(double)y-world.campY)<48)return "forest";
        // Jittered ecological cells repeat across an unbounded plane, with curved
        // boundaries; no six-sector hub and no mandatory map-wide roads.
        double wx=x+warpX.GetNoise(x,y)*30,wy=y+warpY.GetNoise(x,y)*30;
        int cx=(int)Math.floor(wx/112),cy=(int)Math.floor(wy/112),bestX=cx,bestY=cy;double best=Double.MAX_VALUE;
        for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
            int bx=cx+dx,by=cy+dy;double sx=(bx+.18+random(bx,by,1)*.64)*112,sy=(by+.18+random(bx,by,2)*.64)*112;
            double distance=(wx-sx)*(wx-sx)+(wy-sy)*(wy-sy);
            if(distance<best){best=distance;bestX=bx;bestY=by;}
        }
        return HABITATS[(int)Math.floor(random(bestX,bestY,3)*HABITATS.length)];
    }
    public boolean safeCamp(int x,int y){return coast.terrain(x,y)==CoastalCampLayout.Terrain.LAND;}
    public boolean house(int x,int y){return x>=world.houseX-2&&x<=world.houseX+2&&y>=world.houseY-4&&y<=world.houseY-1;}
    public boolean farmland(int x,int y){return x>=world.campX-9&&x<=world.campX-6&&y>=world.campY+5&&y<=world.campY+7;}
    public boolean reserved(int x,int y){
        return coast.reserved(x,y);
    }
    public boolean path(int x,int y){
        return coast.path(x,y);
    }
    /** At most one candidate in each 64x64 macrocell, always entirely inside a
     * native region. Native regional persistence provides generation idempotence. */
    public boolean outpostRegion(int regionX,int regionY){
        int cx=Math.floorDiv(regionX,4),cy=Math.floorDiv(regionY,4);
        if(random(cx,cy,61)>.68)return false;
        return Math.floorMod(regionX,4)==(int)(random(cx,cy,62)*4)
            &&Math.floorMod(regionY,4)==(int)(random(cx,cy,63)*4);
    }
}
