package necesse.entity.mobs.attackHandler;

import java.awt.geom.Point2D.Float;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.BabyDryadMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.summonToolItem.DryadBranchSummonToolItem;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;

public class DryadBranchAttackHandler extends MouseAngleAttackHandler {
   public DryadBranchSummonToolItem toolItem;
   public InventoryItem item;
   public int seed;
   public long nextSummonTime;
   protected int maxSummons = 5;

   public DryadBranchAttackHandler(
      ItemAttackerMob attackerMob, ItemAttackSlot slot, InventoryItem item, DryadBranchSummonToolItem toolItem, int seed, int startX, int startY
   ) {
      super(attackerMob, slot, 50, 1000.0F, startX, startY);
      this.toolItem = toolItem;
      this.item = item;
      this.seed = seed;
      this.nextSummonTime = attackerMob.getTime();
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (!this.attackerMob.isClient()) {
         float babyDryadCount = this.attackerMob.serverFollowersManager.getFollowerCount("babydryad");
         if (this.attackerMob.getTime() > this.nextSummonTime && babyDryadCount < this.maxSummons) {
            this.spawnBabyDryad(this.attackerMob);
            this.nextSummonTime = this.attackerMob.getTime() + 1000L;
         }
      }

      Float dir = GameMath.getAngleDir(this.currentAngle);
      int attackX = this.attackerMob.getX() + (int)(dir.x * 100.0F);
      int attackY = this.attackerMob.getY() + (int)(dir.y * 100.0F);
      InventoryItem showItem = this.item.copy();
      this.attackerMob.showAttackAndSendAttacker(showItem, attackX, attackY, 0, this.seed);
   }

   public void spawnBabyDryad(Mob owner) {
      if (owner != null && owner.isServer()) {
         BabyDryadMob summonedMob = (BabyDryadMob)MobRegistry.getMob("babydryad", owner.getLevel());
         summonedMob.updateDamage(this.toolItem.getAttackDamage(this.item));
         ((ItemAttackerMob)owner)
            .serverFollowersManager
            .addFollower("babydryad", summonedMob, FollowPosition.WALK_CLOSE, "summonedmob", 1.0F, p -> this.maxSummons, null, false);
         Float spawnPoint = SummonToolItem.findSpawnLocation(summonedMob, owner.getLevel(), owner.x, owner.y);
         owner.getLevel().entityManager.addMob(summonedMob, spawnPoint.x, spawnPoint.y);
      }
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      this.maxSummons = 0;
   }
}
