package necesse.level.gameTile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.BombProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.LevelTileTerrainDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.biomes.desert.DesertBiome;
import necesse.level.maps.biomes.forest.ForestBiome;
import necesse.level.maps.biomes.incursions.SettlementRuinsBiome;
import necesse.level.maps.biomes.snow.SnowBiome;
import necesse.level.maps.biomes.swamp.SwampBiome;

public class WaterTile extends LiquidTile {
   public GameTextureSection deepTexture;
   public GameTextureSection shallowTexture;
   protected final GameRandom drawRandom = new GameRandom();

   public WaterTile() {
      super(
         new Color(31, 133, 170),
         "freshwater_shallow",
         "freshwater_deep",
         "saltwater_shallow",
         "saltwater_deep",
         "swampfreshwater_shallow",
         "swampfreshwater_deep",
         "swampsaltwater_shallow",
         "swampsaltwater_deep"
      );
   }

   @Override
   protected void loadTextures() {
      super.loadTextures();
      this.deepTexture = tileTextures.addTexture(GameTexture.fromFile("tiles/waterdeep"));
      this.shallowTexture = tileTextures.addTexture(GameTexture.fromFile("tiles/watershallow"));
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      if (!mob.isFlying() && !mob.isWaterWalking() && level.inLiquid(mob.getX(), mob.getY())) {
         mob.buffManager.removeBuff("onfire", false);
      }
   }

   @Override
   public void tickValid(Level level, int x, int y, boolean underGeneration) {
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) {
               GameTile t = level.getTile(x + i, y + j);
               if (t.isLiquid && t instanceof LavaTile) {
                  if (!underGeneration && level.isClient()) {
                     for (int k = 0; k < 10; k++) {
                        BombProjectile.spawnFuseParticle(
                           level, x * 32 + GameRandom.globalRandom.nextInt(33), y * 32 + GameRandom.globalRandom.nextInt(33), 1.0F
                        );
                     }

                     level.lightManager.refreshParticleLight(x, y, 0.0F, 0.3F);
                     SoundManager.playSound(GameResources.fizz, SoundEffect.effect(x * 32 + 16, y * 32 + 16).volume(0.5F));
                  }

                  level.setTile(x, y, TileRegistry.getTileID("rocktile"));
               }
            }
         }
      }
   }

   @Override
   public LiquidTile.TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
      return biome instanceof SwampBiome ? new LiquidTile.TextureIndexes(4, 5, 6, 7) : new LiquidTile.TextureIndexes(0, 1, 2, 3);
   }

   @Override
   public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
      if (biome instanceof SwampBiome) {
         return this.getLiquidColor(2);
      } else {
         return biome instanceof DesertBiome && level.isCave ? this.getLiquidColor(3) : this.getLiquidColor(0);
      }
   }

   @Override
   public Color getNewSplattingLiquidColor(Level level, int tileX, int tileY, Biome biome) {
      if (level.isCave && biome instanceof SwampBiome) {
         return new Color(200, 200, 100);
      } else if (level.isCave && biome instanceof SnowBiome) {
         return new Color(200, 240, 255);
      } else if (level.baseBiome instanceof SettlementRuinsBiome) {
         return new Color(255, 25, 50);
      } else if (level.isCave) {
         return level.isDeepCaveLevel() && biome instanceof ForestBiome ? new Color(175, 100, 75) : new Color(180, 200, 150);
      } else {
         return super.getNewSplattingLiquidColor(level, tileX, tileY, biome);
      }
   }

   @Override
   public Color getLiquidMapColor(Level level, int tileX, int tileY, Biome biome) {
      if (level.isCave && biome instanceof SwampBiome) {
         return new Color(33, 104, 61);
      } else if (level.isCave && biome instanceof DesertBiome) {
         return new Color(16, 194, 188);
      } else {
         return level.baseBiome instanceof SettlementRuinsBiome ? new Color(150, 50, 75) : this.getLiquidColor(level, tileX, tileY, biome);
      }
   }

   @Override
   protected void addLiquidTopDrawables(
      LevelTileTerrainDrawOptions list, List<LevelSortedDrawable> sortedList, Level level, int tileX, int tileY, GameCamera camera, TickManager tickManager
   ) {
      boolean addBobbing;
      synchronized (this.drawRandom) {
         addBobbing = this.drawRandom.seeded(getTileSeed(tileX, tileY)).getChance(0.15F);
      }

      if (addBobbing) {
         int drawX = camera.getTileDrawX(tileX);
         int drawY = camera.getTileDrawY(tileY);
         int offset = this.getLiquidBobbing(level, tileX, tileY);
         int xOffset;
         int yOffset;
         GameTextureSection bobbingTexture;
         if (level.liquidManager.getHeight(tileX, tileY) <= -10) {
            xOffset = 0;
            yOffset = offset;
            bobbingTexture = this.deepTexture;
         } else {
            xOffset = offset;
            yOffset = 0;
            bobbingTexture = this.shallowTexture;
         }

         int tile;
         synchronized (this.drawRandom) {
            tile = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(bobbingTexture.getHeight() / 32);
         }

         list.add(bobbingTexture.sprite(0, tile, 32)).color(this.getLiquidColor(level, tileX, tileY).brighter()).pos(drawX + xOffset, drawY + yOffset - 2);
      }
   }
}
