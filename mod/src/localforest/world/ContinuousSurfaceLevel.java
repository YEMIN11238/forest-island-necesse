package localforest.world;

import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.SurfaceLevel;
import necesse.level.maps.regionSystem.Region;

/** Original SurfaceLevel generation/validation, native streaming and region saves. */
public final class ContinuousSurfaceLevel extends SurfaceLevel {
    public ContinuousSurfaceLevel(LevelIdentifier id,int width,int height,WorldEntity world){
        super(id,0,0,world);WorldDefinition d=WorldDefinition.forWorld(world);if(d!=null)d.stamp(this);
        baseBiome=MosslumeContinuousBiome.get("forest");
    }
    @Override public void generateRegion(Region region){
        WorldDefinition d=WorldDefinition.forLevel(this);
        if(d==null||!d.isContinuous())throw new IllegalStateException("Continuous level has no v4 definition");
        if(generatorStack==null)generatorStack=getWorldEntity().getGeneratorStack();
        if(!(generatorStack instanceof MosslumeContinuousStack))throw new IllegalStateException("World-owned continuous generator patch was not applied");
        RegionDiagnostics.record(this,region.regionX,region.regionY,"generateBegin",regionManager.getLoadedRegionsSize(),"surface");
        try{
            super.generateRegion(region);
            ContinuousRegionContent.surface(this,region,(MosslumeContinuousStack)generatorStack);
            RegionDiagnostics.record(this,region.regionX,region.regionY,"generated",regionManager.getLoadedRegionsSize(),"surface");
        }catch(RuntimeException error){RegionDiagnostics.record(this,region.regionX,region.regionY,"failed",regionManager.getLoadedRegionsSize(),error.toString());throw error;}
    }
}
