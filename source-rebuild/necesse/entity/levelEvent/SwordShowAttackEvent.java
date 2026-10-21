package necesse.entity.levelEvent;

import necesse.engine.util.GameMath;
import necesse.entity.mobs.AttackAnimMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.swordToolItem.SwordToolItem;

public abstract class SwordShowAttackEvent extends LevelEvent {
   protected float lastAngleTick;
   protected float anglePerTick;
   protected boolean firstSetLastAngleTick;
   protected int aimX;
   protected int aimY;
   protected int attackSeed;
   protected AttackAnimMob attackMob;
   protected InventoryItem inventoryItem;
   protected SwordToolItem sword;

   public SwordShowAttackEvent() {
   }

   public SwordShowAttackEvent(AttackAnimMob attackMob, int x, int y, int attackSeed, float anglePerTick, InventoryItem inventoryItem, SwordToolItem sword) {
      super(false);
      this.attackMob = attackMob;
      this.aimX = x - attackMob.getX();
      this.aimY = y - attackMob.getY();
      this.attackSeed = attackSeed;
      this.anglePerTick = anglePerTick;
      this.inventoryItem = inventoryItem;
      this.sword = sword;
   }

   @Override
   public void init() {
      super.init();
      if (this.attackMob == null) {
         this.over();
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isOver()) {
         if (this.attackMob.isAttacking && this.attackMob.attackSeed == this.attackSeed) {
            float nextProgress = this.attackMob.getAttackAnimProgress();
            if (!this.firstSetLastAngleTick) {
               this.lastAngleTick = GameMath.fixAngle(this.sword.getSwingDirection(this.inventoryItem, this.attackMob).apply(0.0F));
               this.firstSetLastAngleTick = true;
            }

            float nextAngle = GameMath.fixAngle(this.sword.getSwingDirection(this.inventoryItem, this.attackMob).apply(nextProgress));
            float angleDifference = GameMath.getAngleDifference(this.lastAngleTick, nextAngle);
            float sign = Math.signum(angleDifference);
            float absValue = Math.abs(angleDifference);
            if (absValue >= this.anglePerTick) {
               for (float i = 0.0F; i <= absValue; i += this.anglePerTick) {
                  this.tick(this.lastAngleTick + i * sign);
               }

               this.lastAngleTick = nextAngle;
            }
         } else {
            this.over();
         }
      }
   }

   public abstract void tick(float var1);
}
