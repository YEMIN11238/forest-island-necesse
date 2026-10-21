package necesse.entity.mobs.summon;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.inventory.item.Item;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.regionSystem.Region;

public abstract class FollowingWormMobHead<T extends FollowingWormMobBody<B, T>, B extends FollowingWormMobHead<T, B>> extends WormMobHead<T, B> {
   public Item removeWhenNotInInventoryItem;
   public CheckSlotType removeWhenNotInInventorySlotType;
   private int checkRemoveByMissingItemBuffer;

   public FollowingWormMobHead(int health, float waveLength, float distPerMoveSound, int totalBodyParts, float heightMultiplier, float heightOffset) {
      super(health, waveLength, distPerMoveSound, totalBodyParts, heightMultiplier, heightOffset);
      this.isHostile = false;
      this.shouldSave = false;
      this.canDespawn = false;
      this.isStatic = true;
   }

   protected void modifyBodyPart(int index, T bodyPart) {
      super.modifyBodyPart(index, bodyPart);
      bodyPart.setMasterFollowingConfig(this.followingUniqueID);
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      return this.getLevel() != null ? this.getLevel().regionManager.SUMMONED_MOB_OPTIONS : null;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter();
   }

   protected GameMessage getSummonLocalization() {
      return super.getLocalization();
   }

   @Override
   public GameMessage getLocalization() {
      if (this.getLevel() == null) {
         return super.getLocalization();
      }

      Mob followingMob = this.getFollowingMob();
      return followingMob != null
         ? new LocalMessage("mob", "spawnedname", "player", followingMob.getDisplayName(), "mob", this.getSummonLocalization())
         : super.getLocalization();
   }

   @Override
   public void onUnloading(Region region) {
      super.onUnloading(region);
      if (this.isServer()) {
         Mob followingMob = this.getFollowingMob();
         if (followingMob != null && !this.isSamePlace(followingMob)) {
            this.onFollowingAnotherLevel(followingMob);
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
   public float getSpeedModifier() {
      if (this.isFollowing()) {
         Mob attackOwner = this.getAttackOwner();
         if (attackOwner != null) {
            return (1.0F + (attackOwner.buffManager.getModifier(BuffModifiers.SUMMONS_SPEED) - 1.0F) * 0.2F) * super.getSpeedModifier();
         }
      }

      return super.getSpeedModifier();
   }

   @Override
   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, GameBlackboard blackboard, boolean debug) {
      return !debug ? false : super.onMouseHover(camera, perspective, blackboard, debug);
   }
}
