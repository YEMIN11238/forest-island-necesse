package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.Color;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.KineticChargeDashAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.itemAttack.HumanAttackDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class KineticChargerToolItem extends VoidClawSwordToolItem {
   public KineticChargerToolItem() {
      this.canBeUsedForRaids = false;
      this.maxDashStacks.setBaseValue(5);
   }

   @Override
   public ListGameTooltips getKatanaTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add("Hold [input=mouse1] to perform a powerful kinetic charge", 400);
      return tooltips;
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("NOT_OBTAINABLE: Kinetic Charger");
   }

   @Override
   public void showKatanaAttack(Level level, AttackAnimMob mob, int seed, InventoryItem item) {
   }

   @Override
   public String canAttack(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      String superCanAttack = super.canAttack(level, x, y, attackerMob, item);
      if (superCanAttack != null) {
         return superCanAttack;
      } else {
         return !this.canDash(attackerMob) ? "" : superCanAttack;
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
      int animTime = this.getAttackAnimTime(item, attackerMob);
      mapContent.setBoolean("chargeUp", true);
      attackerMob.startAttackHandler(new KineticChargeDashAttackHandler(attackerMob, slot, item, this, animTime, new Color(99, 208, 255), seed, x, y));
      return item;
   }

   @Override
   public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
      return false;
   }

   @Override
   public InventoryItem onLevelInteract(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      return item;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return (int)(this.dashRange.getValue(this.getUpgradeTier(item)).intValue() * 0.8F);
   }

   @Override
   public boolean canDash(ItemAttackerMob attackerMob) {
      return !attackerMob.isRiding() && !attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.KATANA_DASH_COOLDOWN);
   }

   @Override
   public HumanAttackDrawOptions getAttackDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
      InventoryItem headItem,
      InventoryItem chestItem,
      InventoryItem feetItem,
      int mobDir,
      float attackDirX,
      float attackDirY,
      float attackProgress,
      GameSprite armSprite
   ) {
      return new HumanAttackDrawOptions() {
         @Override
         public HumanAttackDrawOptions setOffsets(
            int centerX,
            int centerY,
            int armPosX,
            int armPosY,
            float armRotationOffset,
            int armRotateX,
            int armRotateY,
            int armLength,
            int armCenterHeight,
            int itemYOffset
         ) {
            return this;
         }

         @Override
         public HumanAttackDrawOptions light(Level level, boolean hasGlowEffect, int glowHash, GameLight light) {
            return this;
         }

         @Override
         public HumanAttackDrawOptions skinColor(int skinIndex) {
            return this;
         }

         @Override
         public DrawOptions pos(int drawX, int drawY) {
            return () -> {};
         }
      };
   }
}
