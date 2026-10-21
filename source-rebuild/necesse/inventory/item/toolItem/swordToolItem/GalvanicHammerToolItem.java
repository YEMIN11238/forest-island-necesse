package necesse.inventory.item.toolItem.swordToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.GalvanicTrailEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.CloseRangeWeaponsLootTable;
import necesse.level.maps.Level;

public class GalvanicHammerToolItem extends SwordToolItem {
   protected FloatUpgradeValue projectileResilienceGain = new FloatUpgradeValue(0.0F, 0.0F);

   public GalvanicHammerToolItem() {
      super(1350, CloseRangeWeaponsLootTable.closeRangeWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(450);
      this.attackDamage.setBaseValue(18.0F).setUpgradedValue(1.0F, 26.83334F);
      this.attackRange.setBaseValue(75);
      this.knockback.setBaseValue(75);
      this.resilienceGain.setBaseValue(2.5F).setUpgradedValue(1.0F, 2.5F).setUpgradedValue(10.0F, 3.25F);
      this.projectileResilienceGain.setBaseValue(0.25F);
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "galvanichammertip"));
      return tooltips;
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
      item = super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);

      for (int i = 1; i < 6; i++) {
         LevelEvent event = new GalvanicTrailEvent(
            attackerMob,
            this.getAttackDamage(item),
            this.getResilienceGain(item, this.projectileResilienceGain),
            attackerMob.getX(),
            attackerMob.getY(),
            x,
            y + attackHeight,
            seed * i
         );
         attackerMob.addAndSendAttackerLevelEvent(event);
      }

      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.lightningHammer).volume(0.35F);
   }
}
