package necesse.inventory.container;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketClientRelationshipData;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.gfx.GameColor;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.container.customAction.EmptyCustomAction;

public class EndPlayerRelationshipContainer extends Container {
   public final int itemID;
   public final PlayerInventorySlot inventoryItemSlot;
   private InventoryItem item;
   public final long targetAuthentication;
   public final String targetName;
   public final EmptyCustomAction acceptButton;
   public final EmptyCustomAction declineButton;

   public EndPlayerRelationshipContainer(final NetworkClient client, int uniqueSeed, PacketReader reader) {
      super(client, uniqueSeed);
      this.itemID = reader.getNextShortUnsigned();
      int itemInventoryID = reader.getNextInt();
      int itemInventorySlot = reader.getNextInt();
      this.inventoryItemSlot = new PlayerInventorySlot(itemInventoryID, itemInventorySlot);
      this.item = this.inventoryItemSlot.getItem(client.playerMob.getInv());
      this.targetAuthentication = reader.getNextLong();
      this.targetName = reader.getNextString();
      if (this.item != null && this.item.item.getID() == this.itemID) {
         this.lockSlot(this.inventoryItemSlot);
      } else {
         this.close();
      }

      this.acceptButton = this.registerAction(
         new EmptyCustomAction() {
            @Override
            protected void run() {
               if (client.isServer()) {
                  ServerClient me = client.getServerClient();
                  Server server = me.getServer();
                  GameMessage myMessage = new GameMessageBuilder()
                     .append(GameColor.ITEM_QUEST.getColorCode())
                     .append(new LocalMessage("romance", "youhavebroken", "name", EndPlayerRelationshipContainer.this.targetName));
                  me.relationshipData.clearRelationship();
                  me.sendChatMessage(myMessage);
                  server.network.sendToAllClients(new PacketClientRelationshipData(me));
                  ServerClient them = server.getClientByAuth(EndPlayerRelationshipContainer.this.targetAuthentication);
                  if (them != null && them.relationshipData.isInRelationshipWithPlayer(me)) {
                     them.relationshipData.clearRelationship();
                     GameMessage theirMessage = new GameMessageBuilder().append(GameColor.ITEM_QUEST.getColorCode()).append("romance", "brokenupplayer");
                     them.sendChatMessage(theirMessage);
                     server.network.sendToAllClients(new PacketClientRelationshipData(them));
                  }
               }

               EndPlayerRelationshipContainer.this.item.setAmount(EndPlayerRelationshipContainer.this.item.getAmount() - 1);
               EndPlayerRelationshipContainer.this.inventoryItemSlot.markDirty(client.playerMob.getInv());
               EndPlayerRelationshipContainer.this.close();
            }
         }
      );
      this.declineButton = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            EndPlayerRelationshipContainer.this.close();
         }
      });
   }

   @Override
   public boolean isValid(ServerClient client) {
      if (!super.isValid(client)) {
         return false;
      }

      if (this.inventoryItemSlot == null) {
         return false;
      }

      InventoryItem invItem = this.inventoryItemSlot.getItem(client.playerMob.getInv());
      return invItem != null && invItem.item.getID() == this.itemID;
   }

   public static void openContainer(ServerClient from, ServerClient target, InventoryItem item, PlayerInventorySlot inventorySlot) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextShortUnsigned(item.item.getID());
      writer.putNextInt(inventorySlot.inventoryID);
      writer.putNextInt(inventorySlot.slot);
      writer.putNextLong(target.authentication);
      writer.putNextString(target.getName());
      ContainerRegistry.openAndSendContainer(from, new PacketOpenContainer(ContainerRegistry.END_PLAYER_RELATIONSHIP_CONTAINER, packet));
   }
}
