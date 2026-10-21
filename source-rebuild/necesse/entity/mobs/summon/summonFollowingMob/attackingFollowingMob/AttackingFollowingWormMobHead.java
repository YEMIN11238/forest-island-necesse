package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.FollowingWormMobBody;
import necesse.entity.mobs.summon.FollowingWormMobHead;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;

public abstract class AttackingFollowingWormMobHead<T extends FollowingWormMobBody<B, T>, B extends FollowingWormMobHead<T, B>>
   extends FollowingWormMobHead<T, B>
   implements ToolItemSummonedMob {
   protected GameDamage summonDamage = new GameDamage(0.0F);
   protected ToolItemEnchantment summonEnchantment = ToolItemEnchantment.noEnchant;
   protected ModifierValue<?>[] summonModifiers = new ModifierValue[0];

   public AttackingFollowingWormMobHead(int health, float waveLength, float distPerMoveSound, int totalBodyParts, float heightMultiplier, float heightOffset) {
      super(health, waveLength, distPerMoveSound, totalBodyParts, heightMultiplier, heightOffset);
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
   public boolean canTarget(Mob target) {
      ItemAttackerMob followingAttacker = this.getFollowingItemAttacker();
      return followingAttacker != null && !followingAttacker.isHostile
         ? target.isHostile || target.getUniqueID() == followingAttacker.getSummonFocusUniqueID()
         : super.canTarget(target);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return this.summonModifiers == null ? super.getDefaultModifiers() : Stream.of(this.summonModifiers);
   }
}
