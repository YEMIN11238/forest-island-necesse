package necesse.entity.levelEvent;

import necesse.entity.mobs.AttackAnimMob;

public abstract class SwordCleanSliceAttackMobEvent extends ShowAttackMobTickEvent {
   public SwordCleanSliceAttackMobEvent() {
   }

   public SwordCleanSliceAttackMobEvent(AttackAnimMob attackMob, int attackSeed, int totalTicksPerAttack) {
      super(attackMob, attackSeed, totalTicksPerAttack);
   }

   public void tick(AttackAnimMob mob, float currentAttackProgress) {
      switch (mob.getDir()) {
         case 0:
            this.tick(-90.0F, currentAttackProgress);
            break;
         case 1:
            this.tick(0.0F, currentAttackProgress);
            break;
         case 2:
            this.tick(90.0F, currentAttackProgress);
            break;
         case 3:
            this.tick(180.0F, currentAttackProgress);
      }
   }

   public abstract void tick(float var1, float var2);
}
