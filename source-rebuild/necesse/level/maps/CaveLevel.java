package necesse.level.maps;

import java.util.Comparator;
import java.util.TreeSet;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.regionSystem.Region;

public class CaveLevel extends BiomeGeneratorStackLevel {
   public CaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public CaveLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity, int seed) {
      super(identifier, width, height, worldEntity, seed);
      this.isCave = true;
   }

   @Override
   public void generateRegion(Region region) {
      super.generateRegion(region);
      this.startDirtyRegionTracking();
      int presetGenerationUniqueID = this.getWorldEntity().startPresetGenerationInRegion(region, this.seed);

      try {
         TreeSet<Biome> foundBiomes = new TreeSet<>(Comparator.comparingInt(Biome::getID));

         for (int regionTileX = 0; regionTileX < region.tileLayer.region.tileWidth; regionTileX++) {
            for (int regionTileY = 0; regionTileY < region.tileLayer.region.tileHeight; regionTileY++) {
               int tileX = regionTileX + region.tileXOffset;
               int tileY = regionTileY + region.tileYOffset;
               Biome biome = this.generatorStack.getExpensiveBiome(tileX, tileY);
               Biome spreadBiome = this.generatorStack.getSpreadBiome(tileX, tileY);
               region.biomeLayer.setBiomeByRegion(regionTileX, regionTileY, biome.getID());
               foundBiomes.add(spreadBiome);
               boolean isLava = this.generatorStack.isCaveLava(tileX, tileY);
               int lavaTileID = biome.getGenerationCaveLavaTileID();
               if (isLava && lavaTileID != 0) {
                  region.tileLayer.setTileByRegion(regionTileX, regionTileY, lavaTileID);
               } else {
                  boolean isRiver = this.generatorStack.isCaveRiver(tileX, tileY);
                  int riverTileID = biome.getGenerationCaveRiverWaterTileID();
                  if (isRiver && riverTileID != 0) {
                     region.tileLayer.setTileByRegion(regionTileX, regionTileY, riverTileID);
                  } else {
                     region.tileLayer.setTileByRegion(regionTileX, regionTileY, spreadBiome.getGenerationCaveTileID());
                     if (this.generatorStack.isCaveRock(tileX, tileY)) {
                        region.objectLayer
                           .setObjectByRegion(ObjectLayerRegistry.BASE_LAYER, regionTileX, regionTileY, spreadBiome.getGenerationCaveRockObjectID());
                     }
                  }
               }
            }
         }

         for (Biome biome : foundBiomes) {
            biome.generateRegionCaveTerrain(region, this.generatorStack, this.generatorStack.getNewRegionRandom(region));
         }
      } finally {
         this.getWorldEntity().runPresetGenerationInRegion(presetGenerationUniqueID, region, this.seed);
         this.removeDirtyRegion(region.regionX, region.regionY);
      }
   }

   @Override
   public void onRegionGenerated(Region region, boolean skipGenerateForced) {
      super.onRegionGenerated(region, skipGenerateForced);
      region.checkGenerationValid();
   }
}
