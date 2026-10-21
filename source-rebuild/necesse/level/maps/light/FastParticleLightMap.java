package necesse.level.maps.light;

import necesse.level.maps.regionSystem.Region;

public class FastParticleLightMap extends FastLightMap {
   public FastParticleLightMap(LightManager manager, int startX, int startY, int endX, int endY) {
      super(manager, startX, startY, endX, endY, 15);
   }

   @Override
   protected GameLight getNewLight(int x, int y) {
      Region region = this.level.regionManager.getRegionByTile(x, y, false);
      if (region == null) {
         return this.manager.newLight(0.0F);
      }

      synchronized (this.manager.particlesLock) {
         GameLight light = region.lightLayer.getParticleLightByRegion(x - region.tileXOffset, y - region.tileYOffset);
         return light == null ? this.manager.newLight(0.0F) : light;
      }
   }
}
