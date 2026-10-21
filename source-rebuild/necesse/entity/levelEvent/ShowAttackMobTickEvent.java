package necesse.entity.levelEvent;

import necesse.entity.mobs.AttackAnimMob;

public abstract class ShowAttackMobTickEvent extends ShowAttackAbstractTickEvent<AttackAnimMob> {
   public int attackSeed;

   public ShowAttackMobTickEvent() {
   }

   public ShowAttackMobTickEvent(AttackAnimMob attackMob, int attackSeed, int totalTicksPerAttack) {
      super(attackMob, totalTicksPerAttack);
      this.attackSeed = attackSeed;
      this.totalTicksPerAttack = totalTicksPerAttack;
   }

   public float getAttackProgress(AttackAnimMob entity) {
      return entity.getAttackAnimProgress();
   }

   public boolean isAttackValid(AttackAnimMob entity) {
      return entity.isAttacking && entity.attackSeed == this.attackSeed;
   }
}
