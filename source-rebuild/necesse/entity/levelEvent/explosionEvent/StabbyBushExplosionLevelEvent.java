package necesse.entity.levelEvent.explosionEvent;

import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTextureSection;

public class StabbyBushExplosionLevelEvent extends ExplosionEvent implements Attacker {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public StabbyBushExplosionLevelEvent() {
      this(0.0F, 0.0F, 100, new GameDamage(100.0F), false, 0.0F, null);
   }

   public StabbyBushExplosionLevelEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      super(x, y, range, damage, destructive, toolTier, owner);
      this.knockback = 100;
   }

   @Override
   protected boolean canHitMob(Mob target) {
      return super.canHitMob(target) && target != this.ownerMob;
   }

   @Override
   protected GameDamage getTotalObjectDamage(float targetDistance) {
      return super.getTotalObjectDamage(targetDistance).modDamage(10.0F);
   }

   @Override
   protected void playExplosionEffects() {
      SoundManager.playSound(GameResources.pop, SoundEffect.effect(this.x, this.y).volume(1.5F).pitch(0.5F));
      this.level.getClient().startCameraShake(this.x, this.y, 300, 40, 1.5F, 1.5F, true);
   }

   @Override
   public float getParticleCount(float currentRange, float lastRange) {
      return super.getParticleCount(currentRange, lastRange) * 1.5F;
   }

   @Override
   protected float getDistanceMod(float targetDistance) {
      return 1.0F;
   }

   @Override
   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      GameTextureSection stabbyBushSprites = GameResources.stabbyBushParticles;
      int res = stabbyBushSprites.getHeight();
      int sprite = GameRandom.globalRandom.nextInt(stabbyBushSprites.getWidth() / res);
      if (GameRandom.globalRandom.getChance(0.5F)) {
         this.level
            .entityManager
            .addParticle(x + 4.0F, y - 10.0F, this.explosionTypeSwitcher.next())
            .sprite(stabbyBushSprites.sprite(sprite, 0, 32))
            .sizeFades(25, 40)
            .movesConstant(dirX * 0.8F, dirY * 0.8F)
            .height(10.0F)
            .givesLight(75.0F, 0.5F)
            .lifeTime(lifeTime);
      }
   }
}
