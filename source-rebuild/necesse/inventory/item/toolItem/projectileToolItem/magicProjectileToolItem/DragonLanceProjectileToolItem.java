package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MouseBeamLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.MouseBeamAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class DragonLanceProjectileToolItem extends MagicProjectileToolItem {
   public DragonLanceProjectileToolItem() {
      super(1800, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(2000);
      this.attackDamage.setBaseValue(80.0F).setUpgradedValue(1.0F, 126.00004F);
      this.knockback.setBaseValue(10);
      this.velocity.setBaseValue(120);
      this.attackCooldownTime.setBaseValue(500);
      this.attackRange.setBaseValue(400);
      this.attackXOffset = 20;
      this.attackYOffset = 20;
      this.manaCost.setBaseValue(10.0F).setUpgradedValue(1.0F, 10.0F);
      this.resilienceGain.setBaseValue(1.0F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = false;
   }

   @Override
   public float getFinalAttackMovementMod(InventoryItem item, ItemAttackerMob attackerMob) {
      return 0.0F;
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
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      return this.applyInaccuracy(attackerMob, item, new Point(target.getX(), target.getY()));
   }

   @Override
   protected SoundSettings getSwingSound() {
      return new SoundSettings(GameResources.magicbolt1).basePitch(1.4F).volume(0.3F);
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.dragonLance).basePitch(0.9F).volume(0.05F);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "dragonlancetip"));
      return tooltips;
   }

   @Override
   public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, ItemAttackerMob perspective, boolean forceAdd) {
      this.addAttackDamageTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addResilienceGainTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addCritChanceTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addManaCostTip(list, currentItem, lastItem, perspective);
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return (int)(this.getAttackRange(item) * 0.8F);
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
      float enchantmentSpeedModifier = this.getEnchantment(item)
         .applyModifierLimited(ToolItemModifiers.ATTACK_SPEED, ToolItemModifiers.ATTACK_SPEED.defaultBuffValue);
      MouseBeamLevelEvent event = new MouseBeamLevelEvent(
         attackerMob,
         x,
         y,
         seed,
         50.0F,
         this.getAttackRange(item),
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob),
         null,
         250,
         enchantmentSpeedModifier,
         0,
         this.getResilienceGain(item),
         new Color(212, 75, 41)
      );
      attackerMob.addAndSendAttackerLevelEvent(event);
      float startManaCost = this.getManaCost(item);
      if (startManaCost > 0.0F) {
         this.consumeMana(startManaCost, attackerMob);
      }

      attackerMob.startAttackHandler(new MouseBeamAttackHandler(attackerMob, slot, 75, seed, event).setManaCostPerSecond(this.getManaCost(item)));
      return item;
   }
}
