package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class WebPotionSlowBuff extends Buff {
   public WebPotionSlowBuff() {
      this.shouldSave = false;
      this.isImportant = true;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Mob owner = buff.owner;
      if (owner.isVisible() && GameRandom.globalRandom.nextInt(2) == 0) {
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(owner.dx / 10.0F, owner.dy / 10.0F)
            .color(new Color(220, 215, 210))
            .height(16.0F);
      }
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.SLOW, 0.5F);
   }
}
