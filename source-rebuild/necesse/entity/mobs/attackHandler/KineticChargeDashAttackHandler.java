package necesse.entity.mobs.attackHandler;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.mobAbilityLevelEvent.KineticChargeDashLevelEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.swordToolItem.KatanaToolItem;

public class KineticChargeDashAttackHandler extends KatanaDashAttackHandler {
   public KineticChargeDashAttackHandler(
      ItemAttackerMob attackerMob,
      ItemAttackSlot slot,
      InventoryItem item,
      KatanaToolItem katanaItem,
      int chargeTime,
      Color particleColors,
      int seed,
      int startTargetX,
      int startTargetY
   ) {
      super(attackerMob, slot, item, katanaItem, chargeTime, particleColors, seed, startTargetX, startTargetY);
   }

   @Override
   protected Color startColor() {
      return new Color(0, 0, 0, 0);
   }

   @Override
   protected Color endColor() {
      return new Color(127, 199, 225, 100);
   }

   @Override
   protected Color edgeColor() {
      return new Color(18, 137, 255, 100);
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      float chargePercent = this.getChargePercent();
      if (this.attackerMob.isClient() && this.katanaChargeSoundPlayer != null) {
         this.katanaChargeSoundPlayer.fadeOutAndStop(0.4F);
      }

      if (!this.endedByInteract && chargePercent >= 0.5F) {
         if (this.attackerMob.isPlayer) {
            ((PlayerMob)this.attackerMob).constantAttack = true;
         }

         InventoryItem attackItem = this.item.copy();
         attackItem.getGndData().setBoolean("sliceDash", true);
         attackItem.getGndData().setFloat("chargePercent", chargePercent);
         this.attackerMob.showAttackAndSendAttacker(attackItem, this.lastX, this.lastY, 0, this.seed);
         Float dir = GameMath.normalize(this.lastX - this.attackerMob.x, this.lastY - this.attackerMob.y);
         chargePercent = Math.min(chargePercent, 1.0F);
         KineticChargeDashLevelEvent event = new KineticChargeDashLevelEvent(
            this.attackerMob, this.seed, dir.x, dir.y, this.getChargeDistance(chargePercent), (int)(200.0F * chargePercent), this.katanaItem, attackItem
         );
         this.attackerMob.addAndSendAttackerLevelEvent(event);
         int maxStacks = this.katanaItem.maxDashStacks.getValue(this.katanaItem.getUpgradeTier(this.item));
         boolean sendUpdate = this.attackerMob.isServer();
         if (this.attackerMob.buffManager.getStacks(BuffRegistry.KATANA_DASH_STACKS) < maxStacks - 1) {
            this.attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.KATANA_DASH_STACKS, this.attackerMob, 10.0F, null), sendUpdate);
            this.attackerMob.buffManager.removeBuff(BuffRegistry.Debuffs.KATANA_DASH_COOLDOWN, sendUpdate);
         } else {
            this.attackerMob.buffManager.removeBuff(BuffRegistry.KATANA_DASH_STACKS, sendUpdate);
            this.attackerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.KATANA_DASH_COOLDOWN, this.attackerMob, 5.0F, null), sendUpdate);
         }
      }

      if (this.hudDrawElement != null) {
         this.hudDrawElement.remove();
      }
   }
}
