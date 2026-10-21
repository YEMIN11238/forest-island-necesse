package necesse.entity.levelEvent;

import java.awt.Point;
import necesse.engine.CameraShake;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.TempleEntranceObjectEntity;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class TempleEntranceEvent extends LevelEvent {
   public static int ANIMATION_TIME = 10000;
   public long startTime;
   public int tileX;
   public int tileY;
   protected SoundPlayer secondStageRumble;

   public TempleEntranceEvent() {
   }

   public TempleEntranceEvent(int tileX, int tileY) {
      this.tileX = tileX;
      this.tileY = tileY;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      if (this.isServer()) {
         for (int x = this.tileX - 1; x <= this.tileX + 1; x++) {
            for (int y = this.tileY; y <= this.tileY + 1; y++) {
               for (int layer = 0; layer < ObjectLayerRegistry.getTotalLayers(); layer++) {
                  this.level.entityManager.doObjectDamage(layer, x, y, 1000000, 1000000.0F, null, null);
               }
            }
         }
      }

      ObjectRegistry.getObject("templeentrance").placeObject(this.level, this.tileX - 1, this.tileY, 0, false);
      ObjectEntity entity = this.level.entityManager.getObjectEntity(this.tileX - 1, this.tileY);
      if (entity instanceof TempleEntranceObjectEntity) {
         ((TempleEntranceObjectEntity)entity).startRevealAnimation(ANIMATION_TIME);
      }

      this.startTime = this.level.getWorldEntity().getTime();
      if (this.isClient()) {
         CameraShake cameraShake = this.level.getClient().startCameraShake(this.tileX * 32 + 16, this.tileY * 32 + 16, ANIMATION_TIME, 40, 5.0F, 5.0F, true);
         cameraShake.minDistance = 200;
         cameraShake.listenDistance = 2000;
      } else {
         this.over();
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      long timeProgress = this.level.getWorldEntity().getTime() - this.startTime;
      if (timeProgress > ANIMATION_TIME) {
         this.over();
      } else {
         if (this.secondStageRumble == null || this.secondStageRumble.isDone()) {
            this.secondStageRumble = SoundManager.playSound(
               GameResources.rumble, SoundEffect.effect(this.tileX * 32 + 16, this.tileY * 32 + 16).volume(4.0F).falloffDistance(2000)
            );
         }

         if (this.secondStageRumble != null) {
            this.secondStageRumble.refreshLooping(1.0F);
         }

         float floatProgress = Math.abs(GameMath.limit((float)timeProgress / ANIMATION_TIME, 0.0F, 1.0F) - 1.0F);
         int pixels = (int)(floatProgress * 32.0F * 3.0F);

         for (int i = 0; i < 4; i++) {
            this.level
               .entityManager
               .addParticle(
                  this.tileX * 32 - 32 + pixels + GameRandom.globalRandom.floatGaussian() * 5.0F,
                  this.tileY * 32 + GameRandom.globalRandom.nextFloat() * 32.0F * 2.0F,
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .movesConstant(GameRandom.globalRandom.floatGaussian() * 3.0F, GameRandom.globalRandom.floatGaussian() * 3.0F)
               .smokeColor()
               .heightMoves(0.0F, GameRandom.globalRandom.getFloatBetween(20.0F, 30.0F))
               .lifeTime(1000);
         }
      }
   }

   @Override
   public Point getSaveToRegionPos() {
      return new Point(this.level.regionManager.getRegionCoordByTile(this.tileX), this.level.regionManager.getRegionCoordByTile(this.tileY));
   }
}
