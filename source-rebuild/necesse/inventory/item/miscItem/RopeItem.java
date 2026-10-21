package necesse.inventory.item.miscItem;

import java.awt.Color;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.FriendlyRopableMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemInteractAction;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class RopeItem extends Item implements ItemInteractAction {
   public RopeItem() {
      super(100);
      this.rarity = Item.Rarity.COMMON;
      this.setItemCategory("equipment", "tools", "misc");
      this.worldDrawSize = 32;
   }

   public boolean consumesRope() {
      return true;
   }

   public Color getRopeColor(InventoryItem item) {
      return new Color(71, 39, 25);
   }

   public int getRopeRange(InventoryItem item) {
      return GameRandom.globalRandom.getIntBetween(75, 150);
   }

   @Override
   public boolean holdItemInFrontOfArms(
      InventoryItem item,
      Level level,
      PlayerMob player,
      int spriteX,
      int spriteY,
      int drawX,
      int drawY,
      int width,
      int height,
      boolean mirrorX,
      boolean mirrorY,
      GameLight light,
      boolean hasGlowEffect,
      int glowHash,
      float alpha,
      MaskShaderOptions mask
   ) {
      return true;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "ropetip"));
      return tooltips;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.swingRotation(attackProgress);
   }

   @Override
   public boolean canMobInteract(Level level, Mob mob, ItemAttackerMob attackerMob, InventoryItem item) {
      return mob instanceof FriendlyRopableMob && ((FriendlyRopableMob)mob).canRope(attackerMob.getUniqueID(), item) && mob.inInteractRange(attackerMob);
   }

   @Override
   public InventoryItem onMobInteract(
      Level level, Mob targetMob, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      if (targetMob instanceof FriendlyRopableMob) {
         FriendlyRopableMob rMob = (FriendlyRopableMob)targetMob;
         if (rMob.canRope(attackerMob.getUniqueID(), item)) {
            return rMob.onRope(attackerMob.getUniqueID(), item);
         }
      }

      return item;
   }

   @Override
   public boolean onMouseHoverMob(InventoryItem item, GameCamera camera, PlayerMob perspective, Mob mob, GameBlackboard blackboard, boolean isDebug) {
      boolean sup = super.onMouseHoverMob(item, camera, perspective, mob, blackboard, isDebug);
      if (mob instanceof FriendlyRopableMob && ((FriendlyRopableMob)mob).canRope(perspective.getUniqueID(), item)) {
         Renderer.setCursor(GameWindow.CURSOR.INTERACT);
         if (mob.inInteractRange(perspective)) {
            GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, Localization.translate("controls", "addroptip")), TooltipLocation.INTERACT_FOCUS);
         } else {
            GameTooltipManager.addTooltip(
               new InputTooltip(Control.MOUSE2, Localization.translate("controls", "addroptip"), 0.5F), TooltipLocation.INTERACT_FOCUS
            );
         }

         blackboard.set("preventInteractTip", true);
         return true;
      } else {
         return sup;
      }
   }
}
