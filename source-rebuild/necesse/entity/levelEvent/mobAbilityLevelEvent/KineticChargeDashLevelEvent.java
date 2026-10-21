package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.swordToolItem.KatanaToolItem;
import necesse.inventory.item.toolItem.swordToolItem.VoidClawSwordToolItem;

public class KineticChargeDashLevelEvent extends MobDashLevelEvent {
   public static int PARTICLES_PER_SECOND = 500;
   public static ParticleTypeSwitcher PARTICLE_TYPE_SWITCHER = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.COSMETIC
   );
   protected float particleBuffer;
   protected float timeAlive = 0.0F;
   public InventoryItem item;

   public KineticChargeDashLevelEvent() {
   }

   public KineticChargeDashLevelEvent(Mob owner, int seed, float dirX, float dirY, float distance, int animTime, KatanaToolItem katanaItem, InventoryItem item) {
      super(owner, seed, dirX, dirY, distance, animTime, katanaItem.getAttackDamage(item).modDamage(2.0F), katanaItem.getResilienceGain(item) * 2.0F);
      this.item = item;
   }

   @Override
   public void init() {
      super.init();
      if (this.owner != null) {
         this.owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.INVULNERABLE_ACTIVE, this.owner, this.animTime, null), false);
         if (!this.owner.isPlayer) {
            this.spawnStepParticles(5);
         }

         if (this.isClient()) {
            SoundManager.playSound(new SoundSettings(GameResources.laserBlast2).volume(0.8F).basePitch(0.6F).pitchVariance(0.15F), this.owner);
            SoundManager.playSound(new SoundSettings(GameResources.swoosh).volume(0.4F).basePitch(0.8F).pitchVariance(0.15F), this.owner);
         }
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.particleBuffer = this.particleBuffer + delta / 1000.0F * PARTICLES_PER_SECOND;
      if (this.particleBuffer >= 1.0F) {
         int particles = (int)this.particleBuffer;
         this.particleBuffer -= particles;
         this.spawnStepParticles(particles);
      }

      this.timeAlive += delta;
   }

   @Override
   public void dealServerDamage(Mob target, boolean isClientSubmitted) {
      super.dealServerDamage(target, isClientSubmitted);
      VoidClawSwordToolItem.restoreHealthOnHit(this.item, this.getLevel(), target, this.owner);
   }

   protected void spawnStepParticles(int particles) {
      if (this.level != null && this.level.isClient() && this.owner != null) {
         float forceMod = Math.min(this.animTime / 700.0F, 1.0F);
         float forceX = this.dirX * this.distance * forceMod;
         float forceY = this.dirY * this.distance * forceMod;
         float colorFactor = GameMath.limit(this.timeAlive / this.animTime, 0.0F, 1.0F);
         int colorValue = GameMath.limit(120 + (int)(135.0F * colorFactor), 0, 255);
         Color particleColor = new Color(colorValue, 255, colorValue);

         for (int i = 0; i < particles; i++) {
            this.level
               .entityManager
               .addParticle(
                  this.owner.x + (float)GameRandom.globalRandom.nextGaussian() * 15.0F + forceX / 5.0F,
                  this.owner.y + (float)GameRandom.globalRandom.nextGaussian() * 20.0F + forceY / 5.0F,
                  PARTICLE_TYPE_SWITCHER.next()
               )
               .sprite(GameResources.kineticChargeParticle.sprite(0, 0, 20))
               .movesConstant(
                  forceX * GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F) / 5.0F, forceY * GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F) / 5.0F
               )
               .color(particleColor)
               .minDrawLight((int)(255.0F * colorFactor))
               .height(18.0F)
               .lifeTime(700);
         }
      }
   }

   @Override
   public Shape getHitBox() {
      Float dir;
      if (this.owner.getDir() == 3) {
         dir = GameMath.getPerpendicularDir(-this.dirX, -this.dirY);
      } else {
         dir = GameMath.getPerpendicularDir(this.dirX, this.dirY);
      }

      float width = 40.0F;
      float frontOffset = 0.0F;
      float range = 80.0F;
      float rangeOffset = -40.0F;
      return new LineHitbox(
         this.owner.x + dir.x * rangeOffset + this.dirX * frontOffset, this.owner.y + dir.y * rangeOffset + this.dirY * frontOffset, dir.x, dir.y, range, width
      );
   }
}
