package necesse.entity.mobs.buffs.staticBuffs;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;

public class OnFireBuff extends Buff {
   public OnFireBuff() {
      this.canCancel = false;
      this.isImportant = true;
   }

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      buff.setModifier(BuffModifiers.FIRE_DAMAGE_FLAT, 2.0F);
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      if (buff.owner.isVisible()) {
         Mob owner = buff.owner;
         owner.getLevel()
            .entityManager
            .addParticle(
               owner.x + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
               owner.y + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .movesConstant(owner.dx / 10.0F, owner.dy / 10.0F)
            .flameColor()
            .givesLight(0.0F, 0.5F)
            .height(16.0F);
      }
   }

   @Override
   public Attacker getSource(Attacker source) {
      return new OnFireBuff.FireAttacker(source);
   }

   public static class FireAttacker implements Attacker {
      Attacker owner;

      public FireAttacker(Attacker owner) {
         this.owner = owner;
      }

      @Override
      public GameMessage getAttackerName() {
         return this.owner != null ? this.owner.getAttackerName() : new LocalMessage("deaths", "firename");
      }

      @Override
      public DeathMessageTable getDeathMessages() {
         return this.getDeathMessages("fire", 3);
      }

      @Override
      public Mob getFirstAttackOwner() {
         return this.owner != null ? this.owner.getAttackOwner() : null;
      }

      @Override
      public boolean isTrapAttacker() {
         return this.owner != null ? this.owner.isTrapAttacker() : Attacker.super.isTrapAttacker();
      }
   }
}
