package necesse.inventory.item.placeableItem.objectItem;

import java.util.function.Supplier;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemInteractAction;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class FlowerObjectItem extends CustomObjectItem implements ItemInteractAction {
   public FlowerObjectItem(GameObject object, Supplier<GameTexture> textureSupplier) {
      super(object, textureSupplier, 0, 0);
   }

   @Override
   public boolean canMobInteract(Level level, Mob mob, ItemAttackerMob attackerMob, InventoryItem item) {
      return mob instanceof HusbandryMob && ((HusbandryMob)mob).canFeed(item) && mob.inInteractRange(attackerMob);
   }

   @Override
   public InventoryItem onMobInteract(
      Level level, Mob targetMob, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      if (targetMob instanceof HusbandryMob) {
         HusbandryMob hMob = (HusbandryMob)targetMob;
         if (hMob.canFeed(item)) {
            return hMob.onFed(item);
         }
      }

      return item;
   }

   @Override
   public boolean getConstantInteract(InventoryItem item) {
      return true;
   }

   @Override
   public boolean onMouseHoverMob(InventoryItem item, GameCamera camera, PlayerMob perspective, Mob mob, GameBlackboard blackboard, boolean isDebug) {
      boolean sup = super.onMouseHoverMob(item, camera, perspective, mob, blackboard, isDebug);
      if (mob instanceof HusbandryMob && ((HusbandryMob)mob).canFeed(item)) {
         Renderer.setCursor(GameWindow.CURSOR.INTERACT);
         if (mob.inInteractRange(perspective)) {
            GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, Localization.translate("controls", "feedtip")), TooltipLocation.INTERACT_FOCUS);
         } else {
            GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, Localization.translate("controls", "feedtip"), 0.5F), TooltipLocation.INTERACT_FOCUS);
         }

         blackboard.set("preventInteractTip", true);
         return true;
      } else {
         return sup;
      }
   }
}
