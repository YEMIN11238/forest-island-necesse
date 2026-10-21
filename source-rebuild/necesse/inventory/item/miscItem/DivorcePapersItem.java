package necesse.inventory.item.miscItem;

import java.awt.Point;
import necesse.engine.input.Control;
import necesse.engine.localization.Localization;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketClientRelationshipData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameBlackboard;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerInventoryItemAttackSlot;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.container.EndPlayerRelationshipContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemInteractAction;
import necesse.level.maps.Level;

public class DivorcePapersItem extends Item implements ItemInteractAction {
   public DivorcePapersItem() {
      super(1);
      this.rarity = Item.Rarity.RARE;
      this.setItemCategory("misc");
      this.setItemCategory(ItemCategory.craftingManager, "misc");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "divorcepaperstip"), 350);
      return tooltips;
   }

   public boolean isInInteractRange(Mob me, Mob target) {
      Point interactPos = target.getInteractPos();
      return me.getDistance(interactPos.x, interactPos.y) <= 75.0F;
   }

   @Override
   public boolean canMobInteract(Level level, Mob mob, ItemAttackerMob attackerMob, InventoryItem item) {
      if (!mob.isPlayer || !attackerMob.isPlayer) {
         return false;
      }

      if (!this.isInInteractRange(attackerMob, mob)) {
         return false;
      }

      NetworkClient me = ((PlayerMob)attackerMob).getNetworkClient();
      NetworkClient them = ((PlayerMob)mob).getNetworkClient();
      return this.isInRelationship(me, them);
   }

   public boolean isInRelationship(NetworkClient me, NetworkClient them) {
      return me != null && them != null ? me.relationshipData.isInRelationshipWithPlayer(them) : false;
   }

   @Override
   public InventoryItem onMobInteract(
      Level level, Mob targetMob, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int seed, GNDItemMap mapContent
   ) {
      if (targetMob.isPlayer && attackerMob.isPlayer && attackerMob.isServer()) {
         PlayerMob targetPlayer = (PlayerMob)targetMob;
         PlayerMob myPlayer = (PlayerMob)attackerMob;
         ServerClient targetClient = targetPlayer.getServerClient();
         ServerClient myClient = myPlayer.getServerClient();
         if (targetClient != null && myClient != null) {
            if (this.isInRelationship(targetClient, myClient)) {
               if (slot instanceof PlayerInventoryItemAttackSlot) {
                  PlayerInventorySlot playerSlot = ((PlayerInventoryItemAttackSlot)slot).slot;
                  EndPlayerRelationshipContainer.openContainer(myClient, targetClient, item, playerSlot);
               }
            } else {
               myClient.sendPacket(new PacketClientRelationshipData(myClient));
            }
         }
      }

      return item;
   }

   @Override
   public boolean onMouseHoverMob(InventoryItem item, GameCamera camera, PlayerMob perspective, Mob mob, GameBlackboard blackboard, boolean isDebug) {
      boolean sup = super.onMouseHoverMob(item, camera, perspective, mob, blackboard, isDebug);
      if (mob.isPlayer && perspective != null) {
         NetworkClient me = perspective.getNetworkClient();
         NetworkClient them = ((PlayerMob)mob).getNetworkClient();
         if (this.isInRelationship(me, them)) {
            String tip = Localization.translate("romance", "servepapers", "item", TypeParsers.getItemAndNameParseString(this.getStringID()));
            Renderer.setCursor(GameWindow.CURSOR.INTERACT);
            if (this.isInInteractRange(perspective, mob)) {
               GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, tip), TooltipLocation.INTERACT_FOCUS);
            } else {
               GameTooltipManager.addTooltip(new InputTooltip(Control.MOUSE2, tip, 0.5F), TooltipLocation.INTERACT_FOCUS);
            }

            blackboard.set("preventInteractTip", true);
            return true;
         } else {
            return sup;
         }
      } else {
         return sup;
      }
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.swingRotation(attackProgress);
   }
}
