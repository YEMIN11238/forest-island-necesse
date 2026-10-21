package necesse.level.gameObject;

import java.awt.Color;
import java.awt.geom.Point2D.Double;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;

public class BurnedTreeObject extends TreeObject {
   public BurnedTreeObject(
      String textureName, String logStringID, Color mapColor, int leavesCenterWidth, int leavesMinHeight, int leavesMaxHeight, String leavesTextureName
   ) {
      super(textureName, logStringID, null, mapColor, leavesCenterWidth, leavesMinHeight, leavesMaxHeight, leavesTextureName);
      this.lightLevel = 50;
      this.lightHue = 35.0F;
      this.lightSat = 0.5F;
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      LootTable lootTable = new LootTable();
      if (this.logStringID != null) {
         lootTable.items.add(LootItem.between(this.logStringID, 2, 4).splitItems(5).preventLootMultiplier());
      }

      return lootTable;
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      float leavesPerSecond = 0.25F;
      float leavesChance = leavesPerSecond / 20.0F;
      if (GameRandom.globalRandom.getChance(leavesChance)) {
         float windSpeed = level.weatherLayer.getWindSpeed();
         float windAmount = level.weatherLayer.getWindAmount(tileX, tileY) * 3.0F;
         Double windDir = level.weatherLayer.getWindDirNormalized();
         this.spawnLeafParticles(level, tileX, tileY, this.leavesMinHeight, 1, windDir, windAmount * windSpeed);
      }

      GameRandom random = GameRandom.globalRandom;
      float particlesPerSecond = 0.4F;
      float particleChance = particlesPerSecond / 20.0F;
      if (random.getChance(particleChance)) {
         int posX = tileX * 32 + random.nextInt(32);
         int posY = tileY * 32 + random.nextInt(32);
         level.entityManager
            .addParticle(posX, posY, Particle.GType.IMPORTANT_COSMETIC)
            .color(new Color(225, 118, 11))
            .givesLight(35.0F, 0.8F)
            .sizeFades(11, 22)
            .minDrawLight(100)
            .heightMoves(16.0F, 64.0F)
            .lifeTime(1000);
      }
   }

   @Override
   public void spawnLeafParticles(Level level, int x, int y, int minStartHeight, int amount, Double windDir, float windSpeed) {
      boolean alternate = GameRandom.globalRandom.nextBoolean();

      for (int i = 0; i < amount; i++) {
         float posX = x * 32 + 16
            + (alternate ? GameRandom.globalRandom.getFloatBetween(-1.0F, 0.0F) : GameRandom.globalRandom.getFloatBetween(0.0F, 1.0F)) * this.leavesCenterWidth;
         alternate = !alternate;
         float posY = y * 32 + 32;
         float startHeight = GameRandom.globalRandom.getFloatBetween(minStartHeight, this.leavesMaxHeight);
         float startHeightSpeed = GameRandom.globalRandom.getFloatBetween(0.0F, 60.0F);
         float endHeight = GameRandom.globalRandom.getFloatBetween(-10.0F, -5.0F);
         float gravity = GameRandom.globalRandom.getFloatBetween(8.0F, 20.0F);
         boolean mirror = GameRandom.globalRandom.nextBoolean();
         float moveX = GameRandom.globalRandom.floatGaussian() * 5.0F + (float)windDir.x * windSpeed * 10.0F;
         float moveY = GameRandom.globalRandom.floatGaussian() * 2.0F + (float)windDir.y * windSpeed * 10.0F;
         ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
         ParticleOption.CollisionMover mover = new ParticleOption.CollisionMover(
            level, frictionMover, new CollisionFilter().mobCollision().addFilter(tp -> tp.object().object.isWall)
         );
         int timeToLive = GameRandom.globalRandom.getIntBetween(1000, 2000);
         int timeToFadeOut = GameRandom.globalRandom.getIntBetween(500, 1000);
         int totalTime = timeToLive + timeToFadeOut;
         ParticleOption.HeightMover heightMover = new ParticleOption.HeightMover(startHeight, startHeightSpeed, gravity, 2.0F, endHeight, 0.0F);
         AtomicReference<Float> floatingTime = new AtomicReference<>(0.0F);
         ParticleOption particle = level.entityManager
            .addParticle(posX, posY, Particle.GType.COSMETIC)
            .color(new Color(225, 118, 11))
            .givesLight(35.0F, 0.8F)
            .sizeFades(11, 22)
            .fadesAlphaTime(0, timeToFadeOut)
            .height(heightMover)
            .onMoveTick((delta, lifeTime, timeAlive, lifePercent) -> {
               if (heightMover.currentHeight > endHeight) {
                  floatingTime.set(floatingTime.get() + delta);
               }
            })
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               if (heightMover.currentHeight > endHeight) {
                  mover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
               }
            })
            .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
            .lifeTime(totalTime);
         if (!particle.isRemoved()) {
            SoundManager.playSound(SoundSettingsRegistry.fizzAmbient, SoundEffect.effect(posX, posY));
         }
      }
   }
}
