package necesse.entity.levelEvent.explosionEvent;

import java.awt.Color;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class CursedCroneSpiritBeamsExplosionLevelEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public boolean hasGeneratedASoul;
   public boolean generateSoulsOnHit;

   public CursedCroneSpiritBeamsExplosionLevelEvent() {
      this(0.0F, 0.0F, 100, new GameDamage(100.0F), false, 0.0F, null, false);
   }

   public CursedCroneSpiritBeamsExplosionLevelEvent(
      float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner, boolean generateSoulsOnHit
   ) {
      super(x, y, range, damage, destructive, toolTier, owner);
      this.knockback = 100;
      this.hasGeneratedASoul = false;
      this.generateSoulsOnHit = generateSoulsOnHit;
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(10.0F);
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.magicExplosion, SoundEffect.effect(this.x, this.y).volume(0.4F).pitch(1.0F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 2.0F, 2.0F, true);
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange);
   }

   @Override
   protected float getDistanceMod(float targetDistance) {
      return 1.0F;
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return super.canHitMob(target) && target != this.ownerMob;
   }

   @Override
   protected void onMobWasHit(Mob mob, float distance) {
      super.onMobWasHit(mob, distance);
      if (!this.hasGeneratedASoul && this.generateSoulsOnHit && this.ownerMob != null) {
         this.hasGeneratedASoul = true;
         this.ownerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SOULSTORM_SOULS, this.ownerMob, 6.0F, null), true);
      }
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      if (GameRandom.globalRandom.getChance(0.1F)) {
         this.level
            .entityManager
            .addParticle(x + 4.0F, y - 10.0F, this.explosionTypeSwitcher.next())
            .sprite(GameResources.particles.sprite(0, 0, 8))
            .sizeFades(8, 14)
            .movesConstant(dirX * 0.2F, dirY * 0.2F)
            .height(10.0F)
            .color(new Color(69, 229, 193, 185))
            .givesLight(166.0F, 0.7F)
            .lifeTime(2000);
      }
   }
}
