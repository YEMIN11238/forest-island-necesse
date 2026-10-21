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
import necesse.inventory.container.StartPlayerRelationshipContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemInteractAction;
import necesse.level.maps.Level;

public class PrettyBouquetItem extends Item implements ItemInteractAction {
   public PrettyBouquetItem() {
      super(1);
      this.rarity = Item.Rarity.RARE;
      this.setItemCategory("misc");
      this.setItemCategory(ItemCategory.craftingManager, "misc");
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "prettybouquettip1"), 350);
      tooltips.add(Localization.translate("itemtooltip", "prettybouquettip2"), 350);
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

      NetworkClient them = ((PlayerMob)mob).getNetworkClient();
      NetworkClient me = ((PlayerMob)attackerMob).getNetworkClient();
      return !this.isInRelationship(them, me);
   }

   public boolean isInRelationship(NetworkClient client1, NetworkClient client2) {
      return client1 != null && client2 != null
         ? client2.relationshipData.isInRelationshipWithPlayer(client1) || client1.relationshipData.isInRelationshipWithPlayer(client2)
         : false;
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
            if (!this.isInRelationship(targetClient, myClient)) {
               StartPlayerRelationshipContainer.openContainer(myClient, targetClient);
               if (slot != null) {
                  slot.markDirty();
               }

               return null;
            }

            myClient.sendPacket(new PacketClientRelationshipData(myClient));
            if (slot != null) {
               slot.markDirty();
            }
         }
      }

      return item;
   }

   @Override
   public boolean onMouseHoverMob(InventoryItem item, GameCamera camera, PlayerMob perspective, Mob mob, GameBlackboard blackboard, boolean isDebug) {
      boolean sup = super.onMouseHoverMob(item, camera, perspective, mob, blackboard, isDebug);
      if (mob.isPlayer && perspective != null) {
         NetworkClient them = ((PlayerMob)mob).getNetworkClient();
         NetworkClient me = perspective.getNetworkClient();
         if (!this.isInRelationship(them, me)) {
            String tip = Localization.translate("romance", "givebouquet", "item", TypeParsers.getItemAndNameParseString(this.getStringID()));
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
