package necesse.inventory.container.object;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.zip.DataFormatException;
import necesse.engine.localization.Localization;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketMapData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.MultiTickRunnable;
import necesse.entity.objectEntity.CartographerTableObjectEntity;
import necesse.inventory.container.Container;
import necesse.inventory.container.customAction.ContainerCustomAction;
import necesse.level.maps.mapData.BasicDiscoveredMapManager;

public class CartographersTableContainer extends Container {
   public CartographerTableObjectEntity tableObjectEntity;
   public final CartographersTableContainer.ShareDiscoveriesWithPlayerAction shareWithPlayer;
   public final CartographersTableContainer.InformPlayerAction informPlayer;
   public final HashMap<Integer, MultiTickRunnable> sendTasks = new HashMap<>();

   public CartographersTableContainer(NetworkClient client, int uniqueSeed, CartographerTableObjectEntity tableObjectEntity) {
      super(client, uniqueSeed);
      this.tableObjectEntity = tableObjectEntity;
      this.shareWithPlayer = this.registerAction(new CartographersTableContainer.ShareDiscoveriesWithPlayerAction());
      this.informPlayer = this.registerAction(new CartographersTableContainer.InformPlayerAction());
   }

   @Override
   public void init() {
      super.init();
      this.tableObjectEntity.addUsingClient(this.client);
   }

   @Override
   public void onClose() {
      super.onClose();
      this.tableObjectEntity.removeUsingClient(this.client);
      this.sendTasks.clear();
   }

   @Override
   public void tick() {
      super.tick();
      this.sendTasks.entrySet().removeIf(entry -> entry.getValue().tick());
   }

   @Override
   public boolean isValid(ServerClient client) {
      return !super.isValid(client) ? false : !this.tableObjectEntity.removed() && this.tableObjectEntity.getLevelObject().isInInteractRange(client.playerMob);
   }

   public class InformPlayerAction extends ContainerCustomAction {
      public void runAndSend(int playerSlot) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextShortUnsigned(playerSlot);
         this.runAndSendAction(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         if (CartographersTableContainer.this.client.isServer()) {
            int playerSlot = reader.getNextShortUnsigned();
            Server server = CartographersTableContainer.this.client.getServerClient().getServer();
            ServerClient otherClient = server.getClient(playerSlot);
            if (otherClient != null) {
               otherClient.sendChatMessage(Localization.translate("ui", "cartographergotfrom", "name", CartographersTableContainer.this.client.getName()));
               CartographersTableContainer.this.client
                  .getServerClient()
                  .sendChatMessage(Localization.translate("ui", "cartographergotto", "name", otherClient.getName()));
            }
         }
      }
   }

   public class ShareDiscoveriesWithPlayerAction extends ContainerCustomAction {
      public void sendAllMapDataOverMultiplePackets(int playerSlot, BasicDiscoveredMapManager mapData) {
         ArrayDeque<Runnable> tasks = mapData.setupPacketSendTasks(writer -> {}, mapContentPacket -> {
            Packet packet = new Packet();
            PacketWriter writer = new PacketWriter(packet);
            writer.putNextShortUnsigned(playerSlot);
            writer.putNextContentPacket(mapContentPacket);
            this.runAndSendAction(packet);
         });
         tasks.add(() -> CartographersTableContainer.this.informPlayer.runAndSend(playerSlot));
         CartographersTableContainer.this.sendTasks.put(playerSlot, new MultiTickRunnable(tasks, 25, 16.0F));
      }

      public void sendAllMapDataInOnePacket(int playerSlot, BasicDiscoveredMapManager mapData) throws DataFormatException {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextShortUnsigned(playerSlot);
         Packet mapContent = new Packet();
         mapData.writePacketData(new PacketWriter(mapContent));
         writer.putNextContentPacket(mapContent);
         this.runAndSendAction(content);
         CartographersTableContainer.this.informPlayer.runAndSend(playerSlot);
      }

      @Override
      public void executePacket(PacketReader reader) {
         if (CartographersTableContainer.this.client.isServer()) {
            int playerSlot = reader.getNextShortUnsigned();
            if (CartographersTableContainer.this.tableObjectEntity.hasClientUsing(playerSlot)) {
               Packet mapContentPacket = reader.getNextContentPacket();
               Server server = CartographersTableContainer.this.client.getServerClient().getServer();
               ServerClient otherClient = server.getClient(playerSlot);
               otherClient.sendPacket(new PacketMapData(CartographersTableContainer.this.client.slot, mapContentPacket));
            }
         }
      }
   }
}
