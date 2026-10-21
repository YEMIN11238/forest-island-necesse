package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.BrutesBattleaxeAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GreatswordWeaponsLootTable;
import necesse.level.maps.Level;

public class BrutesBattleaxeToolItem extends GreatswordToolItem {
   public BrutesBattleaxeToolItem() {
      super(800, GreatswordWeaponsLootTable.greatswordWeapons, getThreeChargeLevels(500, 600, 700));
      this.rarity = Item.Rarity.RARE;
      this.attackDamage.setBaseValue(84.0F).setUpgradedValue(1.0F, 165.66672F);
      this.attackRange.setBaseValue(70);
      this.knockback.setBaseValue(150);
      this.attackXOffset = 12;
      this.attackYOffset = 12;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return 200;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = new ListGameTooltips();
      this.addStatTooltips(
         tooltips,
         item,
         blackboard.get(InventoryItem.class, "compareItem"),
         blackboard.getBoolean("showDifference"),
         blackboard.getBoolean("forceAdd"),
         blackboard.get(ItemAttackerMob.class, "perspective", perspective)
      );
      tooltips.add(Localization.translate("itemtooltip", "brutesbattleaxetip"), 400);
      return tooltips;
   }

   @Override
   public int getAttackCooldownTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return attackerMob != null && !attackerMob.isPlayer ? 1000 : super.getAttackCooldownTime(item, attackerMob);
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
      if (!attackerMob.isPlayer) {
         attackerMob.startAttackHandler(new BrutesBattleaxeAttackHandler(attackerMob, level, item, this, seed, x, y, true, slot, this.chargeLevels));
      } else {
         attackerMob.startAttackHandler(new BrutesBattleaxeAttackHandler(attackerMob, level, item, this, seed, x, y, false, slot, this.chargeLevels));
      }

      return item;
   }

   @Override
   protected SoundSettings getGreatswordSwingSound1() {
      return new SoundSettings(GameResources.brutesBattleAxe1).volume(0.3F);
   }

   @Override
   protected SoundSettings getGreatswordSwingSound2() {
      return new SoundSettings(GameResources.brutesBattleAxe2).volume(0.3F);
   }

   @Override
   protected SoundSettings getGreatswordSwingSound3() {
      return new SoundSettings(GameResources.brutesBattleAxe3).volume(0.5F);
   }
}
