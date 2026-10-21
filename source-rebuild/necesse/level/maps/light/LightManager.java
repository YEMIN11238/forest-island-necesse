package necesse.level.maps.light;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.Settings;
import necesse.engine.util.GameMath;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionBoundsExecutor;

public class LightManager {
   public static final int fullLight = 255;
   public static final int maxLight = 150;
   public static final int oneThirdsLight = 50;
   public static final int twoThirdsLight = 100;
   public static final int solidObjectMod = 40;
   public static final int airMod = 10;
   public static final int lightDistance = 25;
   public static final float shadowLightMod = 0.7F;
   public static final float insideLightMod = 0.5F;
   public static final int particleLightFadeTime = 750;
   public static final int particleLightStartLevel = 255;
   public static final int particleLightDistance = 15;
   public Settings.LightSetting setting = Settings.lights;
   protected ExecutorService updateExecutor;
   private static final int COMPUTE_THREADS = 4;
   protected ThreadPoolExecutor computeExecutor;
   public final Object sourcesLock = new Object();
   public final Object lightsLock = new Object();
   public final Object particlesLock = new Object();
   public final Level level;
   private RegionBoundsExecutor drawRegionBounds;
   private LightArea drawLights;
   private LightArea drawWallLights;
   private ParticleLightMap particleLightLevels;
   public GameLight ambientLightOverride = null;
   public GameLight ambientLight = this.newLight(0.0F);

   public LightManager(Level level) {
      synchronized (this) {
         this.level = level;
         this.updateExecutor = Executors.newSingleThreadExecutor(this.defaultThreadFactory("update", false));
         this.computeExecutor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), this.defaultThreadFactory("compute", true));
      }
   }

   public Settings.LightSetting getCurrentSetting() {
      return this.setting;
   }

   public void ensureSetting(Settings.LightSetting setting) {
      if (this.setting != setting) {
         this.setting = setting;
         this.level.regionManager.forEachLoadedRegions(Region::updateLight);
      }
   }

   private ThreadFactory defaultThreadFactory(String type, boolean appendCount) {
      AtomicInteger threadNum = new AtomicInteger(0);
      return r -> {
         Thread thread = new Thread(
            null,
            r,
            "level-"
               + this.level.getHostString()
               + "-"
               + this.level.getIdentifier()
               + "-light-"
               + type
               + (appendCount ? "-" + threadNum.incrementAndGet() : "")
         );
         thread.setDaemon(true);
         return thread;
      };
   }

   public GameLight newLight(float level) {
      return this.setting == Settings.LightSetting.Color ? new GameLightColor(level) : new GameLight(level);
   }

   public GameLight newLight(float hue, float saturation, float level) {
      return this.setting == Settings.LightSetting.Color ? new GameLightColor(hue, saturation, level) : new GameLight(level);
   }

   public GameLight newLight(Color color, float saturation, float level) {
      return this.setting == Settings.LightSetting.Color ? GameLightColor.fromColor(color, saturation, level) : new GameLight(level);
   }

   public float getAmbientLight() {
      return this.ambientLight.getLevel();
   }

   public void updateAmbientLight() {
      if (this.ambientLightOverride != null) {
         this.ambientLight = this.ambientLightOverride;
      } else if (Settings.alwaysLight) {
         this.ambientLight = this.newLight(150.0F);
      } else {
         if (this.level.isCave) {
            this.ambientLight = this.newLight(0.0F, 0.0F, 0.0F);
         } else {
            float ambientLight = this.level.getWorldEntity().getAmbientLight();
            float minLightMod = 1.0F;
            if (Settings.brightness > 0.7F) {
               minLightMod *= GameMath.lerp((float)Math.pow(Settings.brightness - 0.7F, 0.3F), 1.0F, 0.4F);
            }

            float minLight = 150.0F / (10.0F * minLightMod);
            if (ambientLight < minLight) {
               ambientLight = minLight;
            }

            float ambientFloat = Math.abs(ambientLight / 150.0F - 1.0F);
            ambientFloat = (float)Math.pow(ambientFloat, 2.0);
            this.ambientLight = this.newLight(240.0F, ambientFloat * 0.85F, ambientLight);
         }
      }
   }

   public float getInsideModifier(int tileX, int tileY) {
      RegionBoundsExecutor regions = new RegionBoundsExecutor(this.level.regionManager, tileX - 1, tileY - 1, tileX + 1, tileY + 1, false);
      Region region = regions.getRegionByTile(tileX, tileY);
      if (region == null) {
         return 0.0F;
      }

      synchronized (this) {
         return region.lightLayer.getInsideModifierByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, regions);
      }
   }

   public List<SourcedLightModifier> getInsideLightSources(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? Collections.emptyList() : region.lightLayer.getInsideSourcesByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public List<SourcedGameLight> getStaticLightSources(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region == null) {
         return Collections.emptyList();
      }

      synchronized (this.sourcesLock) {
         LinkedList<SourcedGameLight> sources = region.lightLayer.getStaticSourcesByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
         return sources == null ? Collections.emptyList() : new LinkedList<>(sources);
      }
   }

   public GameLight getStaticLightByRegion(Region region, int regionTileX, int regionTileY) {
      GameLight light = region.lightLayer.getStaticLight(regionTileX, regionTileY);
      return light == null ? this.newLight(0.0F) : light;
   }

   public GameLight getStaticLight(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? this.newLight(0.0F) : this.getStaticLightByRegion(region, tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public GameLight getParticleLight(int tileX, int tileY) {
      return this.particleLightLevels == null ? this.newLight(0.0F) : this.particleLightLevels.getLight(tileX, tileY);
   }

   public void refreshParticleLight(int tileX, int tileY) {
      this.refreshParticleLight(tileX, tileY, 100);
   }

   public void refreshParticleLight(int tileX, int tileY, int lightLevel) {
      this.refreshParticleLight(tileX, tileY, this.newLight(0.0F), lightLevel);
   }

   public void refreshParticleLight(int tileX, int tileY, float hue, float saturation) {
      this.refreshParticleLight(tileX, tileY, hue, saturation, 100);
   }

   public void refreshParticleLight(int tileX, int tileY, float hue, float saturation, int lightLevel) {
      this.refreshParticleLight(tileX, tileY, this.newLight(hue, saturation, 0.0F), lightLevel);
   }

   public void refreshParticleLight(int tileX, int tileY, Color color, float saturation) {
      this.refreshParticleLight(tileX, tileY, color, saturation, 100);
   }

   public void refreshParticleLight(int tileX, int tileY, Color color, float saturation, int lightLevel) {
      this.refreshParticleLight(tileX, tileY, this.newLight(color, saturation, 0.0F), lightLevel);
   }

   private void refreshParticleLight(int tileX, int tileY, GameLight color, int lightLevel) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         synchronized (this.particlesLock) {
            region.lightLayer.refreshParticleLightByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, this, color, lightLevel);
         }
      }
   }

   public void refreshParticleLightFloat(float x, float y) {
      this.refreshParticleLightFloat(x, y, 100);
   }

   public void refreshParticleLightFloat(float x, float y, int lightLevel) {
      this.refreshParticleLightFloat(x, y, this.newLight(0.0F), lightLevel);
   }

   public void refreshParticleLightFloat(float x, float y, float hue, float saturation) {
      this.refreshParticleLightFloat(x, y, hue, saturation, 100);
   }

   public void refreshParticleLightFloat(float x, float y, float hue, float saturation, int lightLevel) {
      this.refreshParticleLightFloat(x, y, this.newLight(hue, saturation, 0.0F), lightLevel);
   }

   public void refreshParticleLightFloat(float x, float y, Color color, float saturation) {
      this.refreshParticleLightFloat(x, y, color, saturation, 100);
   }

   public void refreshParticleLightFloat(float x, float y, Color color, float saturation, int lightLevel) {
      this.refreshParticleLightFloat(x, y, this.newLight(color, saturation, 0.0F), lightLevel);
   }

   private void refreshParticleLightFloat(float x, float y, GameLight color, int lightLevel) {
      this.refreshParticleLight(GameMath.getTileCoordinate((int)x), GameMath.getTileCoordinate((int)y), color, lightLevel);
   }

   private GameLight getAmbientAndStaticLightLevelByRegion(Region region, int regionTileX, int regionTileY, RegionBoundsExecutor regions) {
      GameLight light = this.ambientLight.copy();
      if (!this.level.isCave && !region.subRegionData.isOutsideByRegion(regionTileX, regionTileY)) {
         float newLightLevel = light.getLevel() * 0.5F;
         float deltaLightLevel = newLightLevel - light.getLevel();
         synchronized (this) {
            deltaLightLevel *= 1.0F - region.lightLayer.getInsideModifierByRegion(regionTileX, regionTileY, regions);
         }

         light.setLevel(light.getLevel() + deltaLightLevel);
      }

      light.combine(this.getStaticLightByRegion(region, regionTileX, regionTileY));
      return light;
   }

   public GameLight getAmbientAndStaticLightLevel(int tileX, int tileY) {
      RegionBoundsExecutor regions = new RegionBoundsExecutor(this.level.regionManager, tileX - 1, tileY - 1, tileX + 1, tileY + 1, false);
      Region region = regions.getRegionByTile(tileX, tileY);
      return region == null
         ? this.newLight(0.0F)
         : this.getAmbientAndStaticLightLevelByRegion(region, tileX - region.tileXOffset, tileY - region.tileYOffset, regions);
   }

   public float getAmbientAndStaticLightLevelFloat(int tileX, int tileY) {
      float lightLevel = this.ambientLight.level;
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         int regionTileX = tileX - region.tileXOffset;
         int regionTileY = tileY - region.tileYOffset;
         GameLight staticLight = this.getStaticLightByRegion(region, regionTileX, regionTileY);
         lightLevel = GameMath.max(lightLevel, staticLight.level);
      }

      return lightLevel;
   }

   public GameLight getLightLevel(int tileX, int tileY) {
      if (this.drawLights == null) {
         return this.newLight(0.0F);
      }

      tileX = GameMath.limit(tileX, this.drawLights.startX, this.drawLights.endX);
      tileY = GameMath.limit(tileY, this.drawLights.startY, this.drawLights.endY);
      synchronized (this) {
         GameLight light = this.drawLights.lights[this.drawLights.getIndex(tileX, tileY)];
         if (light == null) {
            Region region = this.drawRegionBounds.getRegionByTile(tileX, tileY);
            if (region != null) {
               int regionTileX = tileX - region.tileXOffset;
               int regionTileY = tileY - region.tileYOffset;
               light = this.getAmbientAndStaticLightLevelByRegion(region, regionTileX, regionTileY, this.drawRegionBounds);
               light.combine(this.particleLightLevels.getLight(tileX, tileY));
            } else {
               light = this.newLight(0.0F);
            }

            this.drawLights.lights[this.drawLights.getIndex(tileX, tileY)] = light;
         }

         return light;
      }
   }

   public GameLight getLightLevelWall(int tileX, int tileY) {
      if (this.drawWallLights == null) {
         return this.newLight(0.0F);
      }

      tileX = GameMath.limit(tileX, this.drawWallLights.startX, this.drawWallLights.endX);
      tileY = GameMath.limit(tileY, this.drawWallLights.startY, this.drawWallLights.endY);
      synchronized (this) {
         GameLight light = this.drawWallLights.lights[this.drawWallLights.getIndex(tileX, tileY)];
         if (light == null) {
            Region region = this.drawRegionBounds.getRegionByTile(tileX, tileY);
            if (region != null) {
               int regionTileX = tileX - region.tileXOffset;
               int regionTileY = tileY - region.tileYOffset;
               light = this.ambientLight.copy();
               if (!this.level.isCave
                  && !region.subRegionData.isOutsideByRegion(regionTileX, regionTileY)
                  && !this.isOutside(this.drawRegionBounds, tileX - 1, tileY)
                  && !this.isOutside(this.drawRegionBounds, tileX + 1, tileY)
                  && !this.isOutside(this.drawRegionBounds, tileX, tileY - 1)
                  && !this.isOutside(this.drawRegionBounds, tileX, tileY + 1)) {
                  light.setLevel(light.getLevel() * 0.5F);
               }

               light.combine(this.getStaticLightByRegion(region, regionTileX, regionTileY));
               light.combine(this.particleLightLevels.getLight(tileX, tileY));
            } else {
               light = this.newLight(0.0F);
            }

            this.drawWallLights.lights[this.drawWallLights.getIndex(tileX, tileY)] = light;
         }

         return light;
      }
   }

   public void setDrawArea(int startTileX, int startTileY, int endTileX, int endTileY) {
      ParticleLightMap particleLightMap = new ParticleLightMap(this.level.lightManager, startTileX - 15, startTileY - 15, endTileX + 15, endTileY + 15);
      if (!this.level.isServer()) {
         particleLightMap.update(false);
      }

      this.particleLightLevels = particleLightMap;
      int drawRegionBoundsStartTileX = this.level.limitTileXToBounds(startTileX - 1);
      int drawRegionBoundsStartTileY = this.level.limitTileYToBounds(startTileY - 1);
      int drawRegionBoundsEndTileX = this.level.limitTileXToBounds(endTileX + 1);
      int drawRegionBoundsEndTileY = this.level.limitTileYToBounds(endTileY + 1);
      RegionBoundsExecutor drawRegionBounds = new RegionBoundsExecutor(
         this.level.regionManager, drawRegionBoundsStartTileX, drawRegionBoundsStartTileY, drawRegionBoundsEndTileX, drawRegionBoundsEndTileY, false
      );
      drawRegionBounds.calculateAllRegions();
      LightArea drawLights = new LightArea(startTileX, startTileY, endTileX, endTileY);
      drawLights.initLights();
      LightArea drawWallLights = new LightArea(startTileX, startTileY, endTileX, endTileY);
      drawWallLights.initLights();
      this.drawRegionBounds = drawRegionBounds;
      this.drawLights = drawLights;
      this.drawWallLights = drawWallLights;
   }

   private boolean isOutside(RegionBoundsExecutor executor, int tileX, int tileY) {
      return !executor.isInsideBounds(tileX, tileY)
         ? true
         : executor.getOnTile(tileX, tileY, (region, regionTileX, regionTileY) -> region.subRegionData.isOutsideByRegion(regionTileX, regionTileY), true);
   }

   public void updateStaticLight(int tileX, int tileY) {
      StaticLightUpdater staticUpdater = new StaticLightUpdater(this, tileX, tileY, tileX, tileY);
      synchronized (this) {
         staticUpdater.runUpdate(true);
      }

      if (!this.level.isCave) {
         InsideLightUpdater insideUpdater = new InsideLightUpdater(this, tileX, tileY, tileX, tileY);
         synchronized (this) {
            insideUpdater.runUpdate();
         }
      }
   }

   public void updateStaticLight(int startTileX, int startTileY, int endTileX, int endTileY, boolean concurrently) {
      StaticLightUpdater updater = new StaticLightUpdater(this, startTileX, startTileY, endTileX, endTileY);
      synchronized (this) {
         updater.runUpdate(concurrently);
      }

      if (!this.level.isCave) {
         InsideLightUpdater insideUpdater = new InsideLightUpdater(this, startTileX, startTileY, endTileX, endTileY);
         synchronized (this) {
            insideUpdater.runUpdate();
         }
      }
   }

   public void dispose() {
      if (this.updateExecutor != null) {
         this.updateExecutor.shutdownNow();
      }

      this.updateExecutor = null;
      if (this.computeExecutor != null) {
         this.computeExecutor.shutdownNow();
      }

      this.computeExecutor = null;
   }

   public boolean isDisposed() {
      return this.updateExecutor == null || this.computeExecutor == null;
   }
}
