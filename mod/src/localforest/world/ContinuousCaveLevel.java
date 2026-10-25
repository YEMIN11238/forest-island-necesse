package localforest.world;

import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.CaveLevel;
import necesse.level.maps.regionSystem.Region;

/** Native CaveLevel with the same coordinate biome stack as the surface. */
public final class ContinuousCaveLevel extends CaveLevel {
    public ContinuousCaveLevel(LevelIdentifier id,int width,int height,WorldEntity world){
        super(id,0,0,world,0);WorldDefinition d=WorldDefinition.forWorld(world);if(d!=null)d.stamp(this);
        baseBiome=MosslumeContinuousBiome.get("forest");
    }
    @Override public void generateRegion(Region region){
        WorldDefinition d=WorldDefinition.forLevel(this);
        if(d==null||!d.isContinuous())throw new IllegalStateException("Continuous cave has no v4 definition");
        if(generatorStack==null)generatorStack=getWorldEntity().getGeneratorStack();
        if(!(generatorStack instanceof MosslumeContinuousStack))throw new IllegalStateException("World-owned continuous generator patch was not applied");
        RegionDiagnostics.record(this,region.regionX,region.regionY,"generateBegin",regionManager.getLoadedRegionsSize(),"cave");
        try{
            super.generateRegion(region);ContinuousRegionContent.cave(this,region,d);
            RegionDiagnostics.record(this,region.regionX,region.regionY,"generated",regionManager.getLoadedRegionsSize(),"cave");
        }catch(RuntimeException error){RegionDiagnostics.record(this,region.regionX,region.regionY,"failed",regionManager.getLoadedRegionsSize(),error.toString());throw error;}
    }
}
