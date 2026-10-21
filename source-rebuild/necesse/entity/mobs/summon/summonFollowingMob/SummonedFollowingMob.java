package necesse.entity.mobs.summon.summonFollowingMob;

import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.SummonedMob;
import necesse.inventory.item.Item;
import necesse.level.maps.regionSystem.Region;

public class SummonedFollowingMob extends SummonedMob {
   public Item removeWhenNotInInventoryItem;
   public CheckSlotType removeWhenNotInInventorySlotType;
   private int checkRemoveByMissingItemBuffer;

   public SummonedFollowingMob(int health) {
      super(health);
      this.shouldSave = false;
      this.canDespawn = false;
      this.isStatic = true;
   }

   @Override
   public void onUnloading(Region region) {
      super.onUnloading(region);
      if (this.isServer()) {
         Mob followingMob = this.getFollowingMob();
         if (followingMob != null) {
            if (!this.isSamePlace(followingMob)) {
               this.onFollowingAnotherLevel(followingMob);
            } else if (followingMob.isItemAttacker) {
               this.remove(0.0F, 0.0F, null, true);
            }
         }
      }
   }

   @Override
   public boolean canPushMob(Mob other) {
      return other.isFollowing() && !other.isMounted();
   }

   @Override
   public void setFollowing(Mob mob, boolean sendUpdatePacket) {
      if (this.isFollowing() && mob == null) {
         super.setFollowing(mob, false);
         this.remove(0.0F, 0.0F, null, true);
      } else {
         super.setFollowing(mob, sendUpdatePacket);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.isFollowing()) {
         this.remove(0.0F, 0.0F, null, true);
      } else if (this.removeWhenNotInInventoryItem != null) {
         this.checkRemoveByMissingItemBuffer++;
         if (this.checkRemoveByMissingItemBuffer > 20) {
            this.checkRemoveByMissingItemBuffer = 0;
            ItemAttackerMob followingAttacker = this.getFollowingItemAttacker();
            if (followingAttacker != null && !followingAttacker.hasValidSummonItem(this.removeWhenNotInInventoryItem, this.removeWhenNotInInventorySlotType)) {
               this.remove(0.0F, 0.0F, null, true);
            }
         }
      }
   }

   @Override
   public Mob getFirstAttackOwner() {
      Mob followingMob = this.getFollowingMob();
      return followingMob != null ? followingMob : super.getFirstAttackOwner();
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return null;
   }
}
