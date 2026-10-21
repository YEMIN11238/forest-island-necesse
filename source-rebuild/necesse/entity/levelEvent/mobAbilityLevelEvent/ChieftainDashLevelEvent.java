package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Rectangle;
import java.awt.Shape;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.hostile.bosses.ChieftainMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;

public class ChieftainDashLevelEvent extends MobDashLevelEvent {
   public ChieftainDashLevelEvent() {
   }

   public ChieftainDashLevelEvent(Mob owner, int seed, float dirX, float dirY, float distance, int animTime, GameDamage damage) {
      super(owner, seed, dirX, dirY, distance, animTime, damage, 0.0F);
   }

   @Override
   public void init() {
      super.init();
      if (this.level != null && this.owner != null) {
         if (this.level.isClient()) {
            float forceMod = Math.min(this.animTime / 2000.0F, 1.0F);
            float forceX = this.dirX * this.distance * forceMod;
            float forceY = this.dirY * this.distance * forceMod;

            for (int i = 0; i < 30; i++) {
               float singleForceMod = GameRandom.globalRandom.getFloatBetween(0.5F, 1.5F);
               this.level
                  .entityManager
                  .addParticle(
                     this.owner.x + (float)GameRandom.globalRandom.nextGaussian() * 15.0F + forceX / 5.0F,
                     this.owner.y + (float)GameRandom.globalRandom.nextGaussian() * 20.0F + forceY / 5.0F,
                     Particle.GType.IMPORTANT_COSMETIC
                  )
                  .movesFriction(
                     forceX * singleForceMod * GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F) / 5.0F,
                     forceY * singleForceMod * GameRandom.globalRandom.getFloatBetween(0.8F, 1.2F) / 5.0F,
                     0.8F
                  )
                  .smokeColor(20.0F)
                  .height(18.0F)
                  .lifeTime(2000);
            }

            SoundManager.playSound(GameResources.magicbolt3, SoundEffect.effect(this.owner).volume(0.5F).pitch(1.5F));
            SoundManager.playSound(GameResources.warcryshort, SoundEffect.effect(this.owner).volume(3.0F).falloffDistance(2000));
         }

         if (this.owner instanceof ChieftainMob) {
            ((ChieftainMob)this.owner).showChargeAttack((int)(this.owner.x + this.dirX * 100.0F), (int)(this.owner.y + this.dirY * 100.0F), this.animTime);
         }
      }
   }

   @Override
   protected double getMoveCurve(double x) {
      return Math.cos(x * Math.PI + Math.PI) * 0.5 + 0.5;
   }

   @Override
   public Shape getHitBox() {
      return new Rectangle(this.owner.getX() - 25, this.owner.getY() - 25, 50, 50);
   }
}
