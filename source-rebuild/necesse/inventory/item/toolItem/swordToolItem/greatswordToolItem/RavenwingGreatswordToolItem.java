package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.IncursionGreatswordWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class RavenwingGreatswordToolItem extends GreatswordToolItem {
   public RavenwingGreatswordToolItem() {
      super(1900, IncursionGreatswordWeaponsLootTable.incursionGreatswordWeapons, getThreeChargeLevels(150, 300, 450));
      this.rarity = Item.Rarity.EPIC;
      this.attackDamage.setBaseValue(150.0F).setUpgradedValue(1.0F, 186.66672F);
      this.attackRange.setBaseValue(100);
      this.knockback.setBaseValue(150);
      this.resilienceGain.setBaseValue(3.0F).setUpgradedValue(1.0F, 3.0F).setUpgradedValue(10.0F, 4.0F);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.25F;
      this.defaultLootTier = 1.0F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ravenwinggreatswordtip"), 400);
      return tooltips;
   }

   @Override
   public float getFinalAttackMovementMod(InventoryItem item, ItemAttackerMob attackerMob) {
      return 1.0F;
   }

   @Override
   protected SoundSettings getGreatswordSwingSound1() {
      return new SoundSettings(GameResources.ravenwingGreatsword1).volume(0.4F);
   }

   @Override
   protected SoundSettings getGreatswordSwingSound2() {
      return new SoundSettings(GameResources.ravenwingGreatsword2).volume(0.4F);
   }

   @Override
   protected SoundSettings getGreatswordSwingSound3() {
      return new SoundSettings(GameResources.ravenwingGreatsword3).volume(0.7F);
   }
}
