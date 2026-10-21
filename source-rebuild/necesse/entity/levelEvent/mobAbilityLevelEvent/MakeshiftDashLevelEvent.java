package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;

public class MakeshiftDashLevelEvent extends MobDashLevelEvent {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   private float particleBuffer;

   public MakeshiftDashLevelEvent() {
   }

   public MakeshiftDashLevelEvent(Mob owner, int seed, float dirX, float dirY, float distance, int animTime, GameDamage damage) {
      super(owner, seed, dirX, dirY, distance, animTime, damage, 0.0F);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
   }

   @Override
   public void init() {
      super.init();
      if (this.owner != null) {
         this.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.INVULNERABLE_ACTIVE, this.owner, this.animTime, null), false);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();

      for (int i = 0; i < 10; i++) {
         this.owner
            .getLevel()
            .entityManager
            .addParticle(
               this.owner.x + GameRandom.globalRandom.getIntBetween(-16, 16),
               this.owner.y + GameRandom.globalRandom.getIntBetween(-16, 16),
               this.particleTypeSwitcher.next()
            )
            .sizeFades(20, 40)
            .heightMoves(16.0F, 32.0F)
            .color(
               (options, lifeTime, timeAlive, lifePercent) -> {
                  float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                  Color fireColor = new Color(255, 150, 50);
                  Color smokeColor = new Color(45, 35, 35);
                  options.color(
                     new Color(
                        GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                        GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                        GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                     )
                  );
               }
            )
            .lifeTime(200 + GameRandom.globalRandom.getIntBetween(0, 500));
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.particleBuffer += delta;
      if (this.particleBuffer > 50.0F) {
         this.particleBuffer -= 50.0F;
         this.getLevel()
            .entityManager
            .addParticle(
               this.owner.x + (float)GameRandom.globalRandom.nextGaussian() * 16.0F,
               this.owner.y + (float)GameRandom.globalRandom.nextGaussian() * 16.0F,
               Particle.GType.CRITICAL
            )
            .color(new Color(255, 150, 50))
            .ignoreLight(true)
            .heightMoves(0.0F, 30.0F, 0.0F, 0.5F, 0.0F, 0.0F)
            .givesLight(35.0F, 0.8F)
            .lifeTime(1000)
            .sizeFades(8, 16);
      }
   }

   @Override
   public void clientHit(Mob target, Packet content) {
      super.clientHit(target, content);

      for (int i = 0; i < 10; i++) {
         target.getLevel()
            .entityManager
            .addParticle(
               target.x + GameRandom.globalRandom.getIntBetween(-16, 16),
               target.y + GameRandom.globalRandom.getIntBetween(-16, 16),
               this.particleTypeSwitcher.next()
            )
            .sizeFades(20, 40)
            .heightMoves(16.0F, 32.0F)
            .color(
               (options, lifeTime, timeAlive, lifePercent) -> {
                  float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                  Color fireColor = new Color(255, 150, 50);
                  Color smokeColor = new Color(45, 35, 35);
                  options.color(
                     new Color(
                        GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                        GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                        GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                     )
                  );
               }
            )
            .lifeTime(200 + GameRandom.globalRandom.getIntBetween(0, 500));
      }
   }

   @Override
   public void dealServerDamage(Mob target, boolean isClientSubmitted) {
      super.dealServerDamage(target, isClientSubmitted);
   }

   @Override
   public Shape getHitBox() {
      Float dir;
      if (this.owner.getDir() == 3) {
         dir = GameMath.getPerpendicularDir(-this.dirX, -this.dirY);
      } else {
         dir = GameMath.getPerpendicularDir(this.dirX, this.dirY);
      }

      float width = 100.0F;
      float frontOffset = 0.0F;
      float range = 100.0F;
      float rangeOffset = -50.0F;
      return new LineHitbox(
         this.owner.x + dir.x * rangeOffset + this.dirX * frontOffset, this.owner.y + dir.y * rangeOffset + this.dirY * frontOffset, dir.x, dir.y, range, width
      );
   }
}
