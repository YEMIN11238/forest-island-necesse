package necesse.level.maps.regionSystem.layers;

import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.regionSystem.Region;

public class BiomeBlendingRegionLayer extends RegionLayer {
   protected BiomeBlendingOptions[][] blendOptions;

   public BiomeBlendingRegionLayer(Region region) {
      super(region);
   }

   @Override
   public void init() {
      if (!this.level.isServer()) {
         this.blendOptions = new BiomeBlendingOptions[this.region.tileWidth][this.region.tileHeight];

         for (int x = 0; x < this.region.tileWidth; x++) {
            for (int y = 0; y < this.region.tileHeight; y++) {
               this.blendOptions[x][y] = new BiomeBlendingOptions();
            }
         }
      }
   }

   @Override
   public void onLayerLoaded() {
      if (this.level.isLoadingComplete()) {
         if (!this.level.isServer()) {
            this.region.updateBiomeBlending();
         }
      }
   }

   @Override
   public void onLoadingComplete() {
      if (!this.level.isServer()) {
         this.region.updateBiomeBlending();
      }
   }

   public BiomeBlendingOptions getBlendingOptionsByRegion(int regionTileX, int regionTileY) {
      return this.blendOptions == null ? null : this.blendOptions[regionTileX][regionTileY];
   }

   public void addDebugTooltips(int regionTileX, int regionTileY, StringTooltips tooltips) {
      BiomeBlendingOptions options = this.getBlendingOptionsByRegion(regionTileX, regionTileY);
      if (options != null) {
         synchronized (this.level.entityManager.lock) {
            options.addDebugTooltips(tooltips);
         }
      }
   }

   @Override
   public void onLayerUnloaded() {
   }
}
