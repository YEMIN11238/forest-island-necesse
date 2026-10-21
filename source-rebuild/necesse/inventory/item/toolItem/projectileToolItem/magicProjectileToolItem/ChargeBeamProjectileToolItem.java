package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ChargeBeamWarningLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MouseBeamLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.ChargeBeamAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTipList;
import necesse.level.maps.Level;

public class ChargeBeamProjectileToolItem extends MagicProjectileToolItem {
   public ChargeBeamProjectileToolItem() {
      super(1900, null);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(1500);
      this.attackDamage.setBaseValue(300.0F).setUpgradedValue(1.0F, 310.0F);
      this.knockback.setBaseValue(10);
      this.velocity.setBaseValue(1000);
      this.attackCooldownTime.setBaseValue(500);
      this.attackRange.setBaseValue(1000);
      this.attackXOffset = 20;
      this.attackYOffset = 20;
      this.manaCost.setBaseValue(0.0F);
      this.resilienceGain.setBaseValue(0.75F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = false;
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("NOT_OBTAINABLE: Charge Beam");
   }

   @Override
   public int getAttackAnimTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return item.getGndData().getBoolean("charging") ? 2000 : super.getAttackAnimTime(item, attackerMob);
   }

   @Override
   public boolean animDrawBehindHand(InventoryItem item) {
      return false;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      return super.getPreEnchantmentTooltips(item, perspective, blackboard);
   }

   @Override
   public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, ItemAttackerMob perspective, boolean forceAdd) {
      this.addAttackDamageTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addAttackSpeedTip(list, currentItem, lastItem, perspective);
      this.addResilienceGainTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addCritChanceTip(list, currentItem, lastItem, perspective, forceAdd);
   }

   @Override
   public String canAttack(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      String superError = super.canAttack(level, x, y, attackerMob, item);
      if (superError != null) {
         return superError;
      } else {
         return attackerMob.isOnGenericCooldown("chargeBeam") ? "" : null;
      }
   }

   @Override
   public ItemAttackDrawOptions setupItemSpriteAttackDrawOptions(
      ItemAttackDrawOptions options,
      InventoryItem item,
      PlayerMob player,
      int mobDir,
      float attackDirX,
      float attackDirY,
      float attackProgress,
      Color itemColor
   ) {
      return options;
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      return this.applyInaccuracy(attackerMob, item, new Point(target.getX(), target.getY()));
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
      int fullChargeTime = this.getAttackAnimTime(item, attackerMob);
      MouseBeamLevelEvent event = new ChargeBeamWarningLevelEvent(
         attackerMob, x, y, seed, 2.0F, this.getAttackRange(item), 1.0F, 10, new Color(222, 42, 75), fullChargeTime
      );
      attackerMob.addAndSendAttackerLevelEvent(event);
      attackerMob.startAttackHandler(new ChargeBeamAttackHandler(attackerMob, slot, 75, seed, event, fullChargeTime));
      return item;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return (int)(this.getAttackRange(item) * 0.8F);
   }
}
