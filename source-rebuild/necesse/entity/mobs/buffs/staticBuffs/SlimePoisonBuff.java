package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class SlimePoisonBuff extends Buff {
   public SlimePoisonBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      this.tickDamage(buff, true);
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
            .color(new Color(56, 173, 45))
            .height(16.0F);
      }
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      this.tickDamage(buff, true);
   }

   public void tickDamage(ActiveBuff buff, boolean forceUpdate) {
      float lastDamage = buff.getModifier(BuffModifiers.POISON_DAMAGE_FLAT);
      int maxHealth = buff.owner.getMaxHealth();
      float newDamage = Math.max((float)Math.pow(maxHealth, 0.5) / 10.0F + maxHealth / 50.0F, 5.0F);
      if (lastDamage != newDamage) {
         buff.setModifier(BuffModifiers.POISON_DAMAGE_FLAT, newDamage);
         if (forceUpdate) {
            buff.forceManagerUpdate();
         }
      }
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      this.tickDamage(buff, false);
   }
}
