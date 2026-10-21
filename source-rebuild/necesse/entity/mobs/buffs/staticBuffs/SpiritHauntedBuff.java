package necesse.entity.mobs.buffs.staticBuffs;

import java.awt.Color;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class SpiritHauntedBuff extends Buff {
   public SpiritHauntedBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.ATTACK_SPEED, -0.02F);
      buff.setModifier(BuffModifiers.SPEED, -0.02F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      super.clientTick(buff);
      Mob owner = buff.owner;
      this.updateSpiritPossessedBuff(buff);
      if (owner.isVisible() && GameRandom.globalRandom.nextInt(2) == 0) {
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(owner.dx / 10.0F, owner.dy / 10.0F)
            .color(new Color(105, 236, 172))
            .height(16.0F);
      }
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      super.serverTick(buff);
      this.updateSpiritPossessedBuff(buff);
   }

   @Override
   public int getStackSize(ActiveBuff buff) {
      return 10;
   }

   public void updateSpiritPossessedBuff(ActiveBuff buff) {
      Mob owner = buff.owner;
      if (buff.getStacks() == 10) {
         owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SPIRIT_POSSESSED, owner, 1000, buff.getAttacker()), false);
      } else {
         owner.buffManager.removeBuff(BuffRegistry.Debuffs.SPIRIT_POSSESSED, false);
      }
   }
}
