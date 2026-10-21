package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.MouseBeamLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.attackHandler.MouseBeamAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemStatTipList;
import necesse.level.maps.Level;

public class MouseBeamProjectileToolItem extends MagicProjectileToolItem {
   public MouseBeamProjectileToolItem() {
      super(600, null);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(2000);
      this.attackDamage.setBaseValue(25.0F);
      this.knockback.setBaseValue(10);
      this.velocity.setBaseValue(120);
      this.attackCooldownTime.setBaseValue(500);
      this.attackRange.setBaseValue(400);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("Mouse Beam Test");
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
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      return this.applyInaccuracy(attackerMob, item, new Point(target.getX(), target.getY()));
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient()) {
         SoundManager.playSound(
            GameResources.magicbolt1, SoundEffect.effect(attackerMob).volume(0.2F).pitch(GameRandom.globalRandom.getFloatBetween(1.5F, 1.6F))
         );
      }
   }

   @Override
   public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, ItemAttackerMob perspective, boolean forceAdd) {
      this.addAttackDamageTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addResilienceGainTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addCritChanceTip(list, currentItem, lastItem, perspective, forceAdd);
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
         200,
         enchantmentSpeedModifier,
         1,
         this.getResilienceGain(item),
         new Color(50, 180, 255)
      );
      attackerMob.addAndSendAttackerLevelEvent(event);
      attackerMob.startAttackHandler(new MouseBeamAttackHandler(attackerMob, slot, 75, seed, event));
      return item;
   }
}
