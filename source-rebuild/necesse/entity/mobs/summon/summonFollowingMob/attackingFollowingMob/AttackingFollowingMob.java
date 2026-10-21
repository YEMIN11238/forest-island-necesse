package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.SummonedFollowingMob;
import necesse.gfx.camera.GameCamera;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;

public abstract class AttackingFollowingMob extends SummonedFollowingMob implements ToolItemSummonedMob {
   protected GameDamage summonDamage = new GameDamage(0.0F);
   protected ToolItemEnchantment summonEnchantment = ToolItemEnchantment.noEnchant;
   protected ModifierValue<?>[] summonModifiers = new ModifierValue[0];

   public AttackingFollowingMob(int health) {
      super(health);
   }

   @Override
   public void updateDamage(GameDamage damage) {
      this.summonDamage = damage;
   }

   @Override
   public void setEnchantment(ToolItemEnchantment enchantment) {
      this.summonEnchantment = enchantment;
      this.summonModifiers = new ModifierValue[]{
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, enchantment.getModifier(ToolItemModifiers.DAMAGE)),
         new ModifierValue<>(BuffModifiers.SPEED, enchantment.getModifier(ToolItemModifiers.SUMMONS_SPEED)),
         new ModifierValue<>(BuffModifiers.CHASER_RANGE, enchantment.getModifier(ToolItemModifiers.SUMMONS_TARGET_RANGE)),
         new ModifierValue<>(BuffModifiers.KNOCKBACK_OUT, enchantment.getModifier(ToolItemModifiers.KNOCKBACK))
      };
   }

   @Override
   public void setRemoveWhenNotInInventory(Item summonItem, CheckSlotType slotType) {
      this.removeWhenNotInInventoryItem = summonItem;
      this.removeWhenNotInInventorySlotType = slotType;
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.setEnchantment(EnchantmentRegistry.getEnchantment(reader.getNextShortUnsigned(), ToolItemEnchantment.class, ToolItemEnchantment.noEnchant));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextShortUnsigned(this.summonEnchantment.getID());
   }

   @Override
   public float getSpeedModifier() {
      if (this.isFollowing()) {
         Mob attackOwner = this.getAttackOwner();
         if (attackOwner != null) {
            return attackOwner.buffManager.getModifier(BuffModifiers.SUMMONS_SPEED) * super.getSpeedModifier();
         }
      }

      return super.getSpeedModifier();
   }

   @Override
   public boolean canTarget(Mob target) {
      ItemAttackerMob followingAttacker = this.getFollowingItemAttacker();
      return followingAttacker != null && !followingAttacker.isHostile
         ? target.isHostile || target.getUniqueID() == followingAttacker.getSummonFocusUniqueID()
         : super.canTarget(target);
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return super.canCollisionHit(target);
   }

   @Override
   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, GameBlackboard blackboard, boolean debug) {
      return !debug ? false : super.onMouseHover(camera, perspective, blackboard, debug);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.summonModifiers == null ? super.getDefaultModifiers() : Stream.of(this.summonModifiers);
   }
}
