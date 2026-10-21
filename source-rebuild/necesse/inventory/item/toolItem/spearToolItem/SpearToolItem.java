package necesse.inventory.item.toolItem.spearToolItem;

import java.awt.Shape;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Set;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.Enchantable;
import necesse.inventory.enchants.ItemEnchantment;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

public class SpearToolItem extends ToolItem {
   public SpearToolItem(int enchantCost, OneOfLootItems lootTableCategory) {
      super(enchantCost, lootTableCategory);
      this.setItemCategory("equipment", "weapons", "meleeweapons");
      this.setItemCategory(ItemCategory.equipmentManager, "weapons", "meleeweapons");
      this.setItemCategory(ItemCategory.craftingManager, "equipment", "weapons", "meleeweapons");
      this.keyWords.add("spear");
      this.damageType = DamageTypeRegistry.MELEE;
      this.attackXOffset = 26;
      this.attackYOffset = 16;
      this.width = 16.0F;
      this.resilienceGain.setBaseValue(3.0F).setUpgradedValue(1.0F, 3.0F).setUpgradedValue(10.0F, 4.0F);
      this.tierOneEssencesUpgradeRequirement = "primordialessence";
      this.tierTwoEssencesUpgradeRequirement = "bloodessence";
      this.tierThreeEssenceUpgradeRequirement = "crystalessence";
   }

   @Override
   public GameSprite getWorldItemSprite(InventoryItem item, PlayerMob perspective) {
      return this.getItemSprite(item, perspective);
   }

   @Override
   protected ListGameTooltips getBaseTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getBaseTooltips(item, perspective, blackboard);
      this.addSpearTooltip(tooltips, item, perspective, blackboard);
      return tooltips;
   }

   public void addSpearTooltip(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      tooltips.add(Localization.translate("itemtooltip", "speartip"));
   }

   @Override
   public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, ItemAttackerMob perspective, boolean forceAdd) {
      this.addAttackDamageTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addAttackSpeedTip(list, currentItem, lastItem, perspective);
      this.addResilienceGainTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addKnockbackTip(list, currentItem, lastItem, perspective);
      this.addCritChanceTip(list, currentItem, lastItem, perspective, forceAdd);
   }

   @Override
   public int getItemAttackerMinimumAttackRange(ItemAttackerMob attackerMob, InventoryItem item) {
      return this.getAttackRange(item) / 2;
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      if (animAttack == 0) {
         int animTime = this.getAttackAnimTime(item, attackerMob);
         ToolItemMobAbilityEvent event = new ToolItemMobAbilityEvent(
            attackerMob, seed, item, x - attackerMob.getX(), y - attackerMob.getY(), animTime, animTime / 2
         );
         attackerMob.addAndSendAttackerLevelEvent(event);
      }

      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.defaultSpear).volume(0.7F);
   }

   @Override
   public ArrayList<Shape> getHitboxes(InventoryItem item, AttackAnimMob mob, int aimX, int aimY, ToolItemMobAbilityEvent event, boolean forDebug) {
      ArrayList<Shape> out = new ArrayList<>();
      float attackRange = this.getAttackRange(item);
      Float dir = GameMath.normalize(aimX, aimY);
      float yOffset = Math.min(mob.getCurrentAttackDrawYOffset() + mob.getStartAttackHeight(), 0);
      java.awt.geom.Line2D.Float attackLine = new java.awt.geom.Line2D.Float(
         mob.x + dir.x * attackRange / 2.0F, mob.y + dir.y * attackRange / 2.0F + yOffset, mob.x + dir.x * attackRange, mob.y + dir.y * attackRange + yOffset
      );
      if (this.width > 0.0F) {
         out.add(new LineHitbox(attackLine, this.width));
      } else {
         out.add(attackLine);
      }

      return out;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY);
      drawOptions.thrustOffsets(attackDirX, attackDirY, attackProgress);
   }

   @Override
   public boolean animDrawBehindHand(InventoryItem item) {
      return true;
   }

   @Override
   public ToolItemEnchantment getRandomEnchantment(GameRandom random, InventoryItem item) {
      return Enchantable.getRandomEnchantment(random, EnchantmentRegistry.meleeItemEnchantments, this.getEnchantmentID(item), ToolItemEnchantment.class);
   }

   @Override
   public boolean isValidEnchantment(InventoryItem item, ItemEnchantment enchantment) {
      return EnchantmentRegistry.meleeItemEnchantments.contains(enchantment.getID());
   }

   @Override
   public Set<Integer> getValidEnchantmentIDs(InventoryItem item) {
      return EnchantmentRegistry.meleeItemEnchantments;
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "spear");
   }
}
