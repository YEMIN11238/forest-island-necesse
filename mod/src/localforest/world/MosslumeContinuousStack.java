package localforest.world;

import necesse.engine.world.biomeGenerator.BiomeGeneratorStack;
import necesse.engine.world.biomeGenerator.GeneratorCache;

/** Extend native river/ocean/cave stack; replace only biome selection and the
 * small authored coastal POI. Coordinates include negative regions. */
public final class MosslumeContinuousStack extends BiomeGeneratorStack {
    public final ContinuousWorldLayout layout;
    public final CoastalCampLayout coast;
    private final GeneratorCache biomeCache=new GeneratorCache(8192);
    public MosslumeContinuousStack(WorldDefinition world){
        super((int)(world.seed^(world.seed>>>32)));layout=ContinuousWorldLayout.forWorld(world);MosslumeContinuousBiome.initialize(this);
        coast=layout.coast.connectToNative((x,y)->super.isSurfaceExpensiveWater(x,y));
    }
    private int biomeID(int x,int y){return biomeCache.get(x,y,(tx,ty)->MosslumeContinuousBiome.get(layout.biome(tx,ty)).getID());}
    @Override public int getExpensiveBiomeID(int x,int y){return biomeID(x,y);}
    @Override public int getSpreadBiomeID(int x,int y){return biomeID(x,y);}
    @Override public int getLazyBiomeID(int x,int y){return layout==null?super.getLazyBiomeID(x,y):biomeID(x,y);}
    private CoastalCampLayout.Terrain local(int x,int y){return coast==null?CoastalCampLayout.Terrain.NATIVE:coast.terrain(x,y);}
    // Preserve all native query contracts. Grass reaches the cliff edge at this
    // authored POI; beaches/rivers elsewhere continue through the original stack.
    @Override public boolean isSurfaceExpensiveWater(int x,int y){var t=local(x,y);return t==CoastalCampLayout.Terrain.NATIVE?super.isSurfaceExpensiveWater(x,y):t==CoastalCampLayout.Terrain.WATER;}
    @Override public boolean isSurfaceExpensiveWaterOrBeach(int x,int y){var t=local(x,y);return t==CoastalCampLayout.Terrain.NATIVE?super.isSurfaceExpensiveWaterOrBeach(x,y):t==CoastalCampLayout.Terrain.WATER;}
    @Override public boolean isSurfaceOcean(int x,int y){var t=local(x,y);return t==CoastalCampLayout.Terrain.NATIVE?super.isSurfaceOcean(x,y):t==CoastalCampLayout.Terrain.WATER;}
    @Override public boolean isSurfaceOceanBeach(int x,int y){return local(x,y)==CoastalCampLayout.Terrain.NATIVE&&super.isSurfaceOceanBeach(x,y);}
    @Override public boolean isSurfaceOceanOrBeach(int x,int y){var t=local(x,y);return t==CoastalCampLayout.Terrain.NATIVE?super.isSurfaceOceanOrBeach(x,y):t==CoastalCampLayout.Terrain.WATER;}
    @Override public boolean isSurfaceOceanOrRiver(int x,int y){var t=local(x,y);return t==CoastalCampLayout.Terrain.NATIVE?super.isSurfaceOceanOrRiver(x,y):t==CoastalCampLayout.Terrain.WATER;}
    @Override public boolean isSurfaceOceanOrRiverBeach(int x,int y){return local(x,y)==CoastalCampLayout.Terrain.NATIVE&&super.isSurfaceOceanOrRiverBeach(x,y);}
    @Override public boolean isSurfaceOceanOrRiverOrBeach(int x,int y){var t=local(x,y);return t==CoastalCampLayout.Terrain.NATIVE?super.isSurfaceOceanOrRiverOrBeach(x,y):t==CoastalCampLayout.Terrain.WATER;}
}
