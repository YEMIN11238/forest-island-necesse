package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;

public class BleedDebuff extends Buff {
   public BleedDebuff() {
      this.isImportant = true;
      this.isVisible = true;
      this.canCancel = false;
   }

   protected float getDamagePerSecond(ActiveBuff buff) {
      float bleedDamagePerSec = buff.getGndData().getFloat("damagepersec");
      return bleedDamagePerSec != 0.0F ? bleedDamagePerSec : 5.0F;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.BLEED_DAMAGE_FLAT, this.getDamagePerSecond(buff));
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 1;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isVisible() && GameRandom.globalRandom.getEveryXthChance(4)) {
         Mob owner = buff.owner;
         float variance = 0.4F;
         float xDir = GameRandom.globalRandom.getFloatBetween(-variance, variance);
         float yVariance = GameRandom.globalRandom.getFloatBetween(10.0F, 20.0F);
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 5.0),
               owner.y + 23.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sprite(GameResources.particles.sprite(0, 0, 8))
            .color(new Color(182, 12, 12))
            .dontRotate()
            .height(32.0F)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               pos.x += xDir;
               float heightFactor = (float)(-Math.pow(0.6 - 1.6 * lifePercent, 2.0)) + 1.0F;
               pos.y = owner.y + 32.0F + yVariance - heightFactor * 25.0F;
            })
            .fadesAlphaTimeToCustomAlpha(200, 200, 0.9F)
            .size(new ParticleOption.DrawModifier() {
               @Override
               public void modify(SharedTextureDrawOptions.Wrapper options, int lifeTime, int timeAlive, float lifePercent) {
                  options.size(10, 10);
               }
            })
            .lifeTime(500);
      }
   }
}
