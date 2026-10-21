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

public class SharkFrenzyBuff extends Buff {
   public SharkFrenzyBuff() {
      this.isImportant = true;
      this.isVisible = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      float modifier = 0.05F;
      buff.setModifier(BuffModifiers.ALL_DAMAGE, modifier);
      buff.setModifier(BuffModifiers.SPEED, modifier);
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 5;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      if (buff.owner.isVisible() && GameRandom.globalRandom.getChance(0.4)) {
         Mob owner = buff.owner;
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y + 23.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sprite(GameResources.particles.sprite(0, 0, 8))
            .color(new Color(182, 12, 12))
            .dontRotate()
            .height(32.0F)
            .movesConstant(0.0F, -3.0F)
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
