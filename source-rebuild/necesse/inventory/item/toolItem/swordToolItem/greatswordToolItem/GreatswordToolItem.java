package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.GreatswordAttackHandler;
import necesse.entity.mobs.attackHandler.GreatswordChargeLevel;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.toolItem.swordToolItem.SwordToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

public class GreatswordToolItem extends SwordToolItem {
   public GreatswordChargeLevel[] chargeLevels;

   public GreatswordToolItem(int enchantCost, OneOfLootItems lootTableCategory, GreatswordChargeLevel... chargeLevels) {
      super(enchantCost, lootTableCategory);
      this.chargeLevels = chargeLevels;
      if (chargeLevels.length == 0) {
         throw new IllegalArgumentException("Must have at least one charge level for greatswords");
      }

      this.attackAnimTime.setBaseValue(200);
      this.attackXOffset = 16;
      this.attackYOffset = 16;
      this.resilienceGain.setBaseValue(6.0F).setUpgradedValue(1.0F, 6.0F).setUpgradedValue(10.0F, 8.0F);
      this.raiderDamageModifier = 0.5F;
      this.tierOneEssencesUpgradeRequirement = "bioessence";
      this.tierTwoEssencesUpgradeRequirement = "slimeessence";
      this.tierThreeEssenceUpgradeRequirement = "crystalessence";
   }

   public static GreatswordChargeLevel[] getThreeChargeLevels(int level1Time, int level2Time, int level3Time) {
      return new GreatswordChargeLevel[]{
         new GreatswordChargeLevel(level1Time, 1.0F, new Color(200, 200, 200)),
         new GreatswordChargeLevel(level2Time, 1.5F, new Color(200, 200, 100)),
         new GreatswordChargeLevel(level3Time, 2.0F, new Color(200, 100, 100))
      };
   }

   @Override
   public boolean getConstantUse(InventoryItem item) {
      return false;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "greatswordtip"));
      return tooltips;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (item.getGndData().getBoolean("charged")) {
         if (level.isClient()) {
            int chargeLevel = item.getGndData().getInt("currentChargeLevel");
            SoundSettings sound = this.getGreatswordSwingSound(chargeLevel);
            sound.setPitchVarianceIfNotSet(0.02F);
            SoundManager.playSound(sound, attackerMob);
         }
      }
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
      attackerMob.startAttackHandler(new GreatswordAttackHandler(attackerMob, slot, item, this, seed, x, y, this.chargeLevels));
      return item;
   }

   public InventoryItem superOnAttack(
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
      return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
   }

   private SoundSettings getGreatswordSwingSound(int chargeLevel) {
      switch (chargeLevel) {
         case 0:
            return this.getGreatswordSwingSound1();
         case 1:
            return this.getGreatswordSwingSound2();
         case 2:
            return this.getGreatswordSwingSound3();
         default:
            return this.getGreatswordSwingSound1();
      }
   }

   protected SoundSettings getGreatswordSwingSound1() {
      return new SoundSettings(GameResources.regularGreatSwords1).volume(0.9F);
   }

   protected SoundSettings getGreatswordSwingSound2() {
      return new SoundSettings(GameResources.regularGreatSwords2).volume(0.9F);
   }

   protected SoundSettings getGreatswordSwingSound3() {
      return new SoundSettings(GameResources.regularGreatSwords3).volume(0.7F);
   }

   @Override
   public boolean shouldRunOnAttackedBuffEvent(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent
   ) {
      return false;
   }

   @Override
   public GameDamage getAttackDamage(InventoryItem item) {
      float damageMultiplier = item.getGndData().getFloat("chargeDamageMultiplier", 1.0F);
      return super.getAttackDamage(item).modDamage(damageMultiplier);
   }

   @Override
   public float getResilienceGain(InventoryItem item) {
      float chargeMod = 1.0F + 0.5F * item.getGndData().getInt("currentChargeLevel");
      return this.resilienceGain.getValue(this.getUpgradeTier(item))
         * chargeMod
         * this.getEnchantment(item).applyModifierLimited(ToolItemModifiers.RESILIENCE_GAIN, ToolItemModifiers.RESILIENCE_GAIN.defaultBuffManagerValue);
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "greatsword");
   }
}
