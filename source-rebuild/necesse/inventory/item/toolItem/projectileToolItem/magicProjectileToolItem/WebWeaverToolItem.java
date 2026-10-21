package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Point;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.WebWeaverWebEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

public class WebWeaverToolItem extends MagicProjectileToolItem {
   public WebWeaverToolItem(OneOfLootItems lootTableCategory) {
      super(1900, lootTableCategory);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(1000);
      this.attackDamage.setBaseValue(37.0F).setUpgradedValue(1.0F, 56.00002F);
      this.attackXOffset = 30;
      this.attackYOffset = 30;
      this.attackRange.setBaseValue(5000);
      this.manaCost.setBaseValue(15.0F);
      this.resilienceGain.setBaseValue(2.0F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.25F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "webweavertip"));
      return tooltips;
   }

   @Override
   public Point getControllerAttackLevelPos(Level level, float aimDirX, float aimDirY, PlayerMob player, InventoryItem item) {
      int range = 500;
      return new Point((int)(player.x + aimDirX * range), (int)(player.y + aimDirY * range));
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
      GameRandom random = new GameRandom(seed);
      Point targetPos = this.controlledRangePosition(random, attackerMob, x, y, item, 0, 40);
      float speedModifier = this.getAttackSpeedModifier(item, attackerMob);
      int delayTime = (int)(1000.0F * (1.0F / speedModifier));
      WebWeaverWebEvent event = new WebWeaverWebEvent(
         attackerMob, targetPos.x, targetPos.y, random, this.getAttackDamage(item), this.getResilienceGain(item), delayTime
      );
      attackerMob.addAndSendAttackerLevelEvent(event);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      float speedModifier = this.getAttackSpeedModifier(item, attackerMob);
      int delayTime = (int)(1000.0F * (1.0F / speedModifier));
      return this.applyInaccuracy(attackerMob, item, this.getPredictedItemAttackerAttackPositionMillis(target, delayTime));
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return this.getAttackRange(item) / 4;
   }
}
