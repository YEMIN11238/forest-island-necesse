package necesse.entity.mobs.hostile;

import java.awt.Point;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ability.TargetedMobAbility;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;

public class HostileArcherMob extends HostileMob {
   protected long bowAttackStartTime;
   protected Mob bowAttackTargetMob;
   protected Point bowAttackTargetPos;
   protected final TargetedMobAbility startBowAttackAbility;
   protected final CoordinateMobAbility fireBowAttackAbility;
   protected int bowAttackChargeUpTime = 500;
   protected int bowAttackDecidedAimTime = 100;
   protected int bowAttackFireTime = 300;
   protected boolean showBowAttackAllDirections = false;

   public HostileArcherMob(int health) {
      super(health);
      this.startBowAttackAbility = this.registerAbility(new TargetedMobAbility() {
         @Override
         protected void run(Mob target) {
            HostileArcherMob.this.runStartBowAttack(target);
         }
      });
      this.fireBowAttackAbility = this.registerAbility(new CoordinateMobAbility() {
         @Override
         protected void run(int x, int y) {
            HostileArcherMob.this.runFireBowAttack(x, y);
         }
      });
   }

   public void runStartBowAttack(Mob target) {
      if (target != null) {
         this.bowAttackTargetMob = target;
         this.bowAttackStartTime = this.getTime();
         this.attackAnimTime = this.bowAttackChargeUpTime + 500;
         this.attackCooldown = this.bowAttackChargeUpTime + 1000;
         this.bowAttackTargetPos = new Point(target.getX(), target.getY());
         this.attack(target.getX(), target.getY(), this.showBowAttackAllDirections);
      }
   }

   public void runFireBowAttack(int x, int y) {
      this.bowAttackStartTime = 0L;
      this.attackAnimTime = this.bowAttackFireTime;
      this.attackCooldown = this.bowAttackFireTime + 500;
      this.attack(x, y, this.showBowAttackAllDirections);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.bowAttackTargetMob != null && this.bowAttackStartTime != 0L) {
         long timeSinceStartAttack = this.getTime() - this.bowAttackStartTime;
         if (timeSinceStartAttack < this.bowAttackChargeUpTime - this.bowAttackDecidedAimTime) {
            this.bowAttackTargetPos = new Point(this.bowAttackTargetMob.getX(), this.bowAttackTargetMob.getY());
            this.updateAttackDir(this.bowAttackTargetPos.x, this.bowAttackTargetPos.y, this.showBowAttackAllDirections);
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.bowAttackTargetPos != null && this.bowAttackStartTime != 0L) {
         long timeSinceStartAttack = this.getTime() - this.bowAttackStartTime;
         if (timeSinceStartAttack >= this.bowAttackChargeUpTime) {
            this.fireBowAttackAbility.runAndSend(this.bowAttackTargetPos.x, this.bowAttackTargetPos.y);
         } else if (this.bowAttackTargetMob != null && timeSinceStartAttack < this.bowAttackChargeUpTime - this.bowAttackDecidedAimTime) {
            this.bowAttackTargetPos = new Point(this.bowAttackTargetMob.getX(), this.bowAttackTargetMob.getY());
            this.updateAttackDir(this.bowAttackTargetPos.x, this.bowAttackTargetPos.y, this.showBowAttackAllDirections);
         }
      }
   }

   public void setupHumanAttackOptions(HumanDrawOptions humanDrawOptions, InventoryItem bowItem, float attackProgress) {
      if (this.bowAttackStartTime != 0L) {
         long timeSinceStartAttack = this.getTime() - this.bowAttackStartTime;
         float progress = GameMath.limit((float)timeSinceStartAttack / this.bowAttackChargeUpTime, 0.0F, 1.0F);
         bowItem.getGndData().setFloat("chargePercent", progress);
         bowItem.getGndData().setBoolean("charging", true);
      } else {
         float bowMaxProgress = 30.0F / this.bowAttackFireTime;
         float bowProgress = GameMath.getPercentageBetweenTwoNumbers(attackProgress, 0.0F, bowMaxProgress);
         bowProgress = GameMath.limit(bowProgress, 0.0F, 1.0F);
         bowProgress = GameMath.lerp(bowProgress, 1.0F, 0.0F);
         bowItem.getGndData().setFloat("chargePercent", bowProgress);
         bowItem.getGndData().setBoolean("charging", true);
         float armProgress = GameMath.getPercentageBetweenTwoNumbers(attackProgress, 0.0F, 0.5F);
         armProgress = GameMath.limit(armProgress, 0.0F, 1.0F);
         armProgress = GameMath.lerp(armProgress, 1.0F, 0.0F);
         bowItem.getGndData().setFloat("chargedArm", armProgress);
      }

      humanDrawOptions.itemAttack(bowItem, null, attackProgress, this.attackDir.x, this.attackDir.y);
   }
}
