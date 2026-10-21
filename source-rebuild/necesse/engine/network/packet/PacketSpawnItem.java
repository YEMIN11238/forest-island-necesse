package necesse.engine.network.packet;

import necesse.engine.commands.PermissionLevel;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;

public class PacketSpawnItem extends Packet {
   public final boolean inHand;
   public final Packet itemContent;

   public PacketSpawnItem(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.inHand = reader.getNextBoolean();
      this.itemContent = reader.getNextContentPacket();
   }

   public PacketSpawnItem(InventoryItem item, boolean inHand) {
      this.inHand = inHand;
      this.itemContent = InventoryItem.getContentPacket(item);
      PacketWriter writer = new PacketWriter(this);
      writer.putNextBoolean(inHand);
      writer.putNextContentPacket(this.itemContent);
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (client.getPermissionLevel().getLevel() >= PermissionLevel.ADMIN.getLevel()) {
         if (server.world.settings.cheatsAllowedOrHidden()) {
            InventoryItem item = InventoryItem.fromContentPacket(this.itemContent);
            int startAmount = item.getAmount();
            if (this.inHand) {
               if (client.playerMob.getDraggingItem() == null) {
                  client.playerMob.setDraggingItem(item);
                  System.out.println(client.getName() + " spawned an item: " + item.getItemDisplayName() + " (" + item.getAmount() + ")");
               } else if (!client.playerMob
                  .getDraggingItem()
                  .combine(client.playerMob.getLevel(), client.playerMob, client.playerMob.getInv().drag, 0, item, "spawnitem", null)
                  .success) {
                  client.playerMob.setDraggingItem(null);
               }
            } else {
               client.playerMob.getInv().addItem(item, true, "give", null);
               if (item.getAmount() != startAmount) {
                  System.out
                     .println(client.getName() + " spawned an item: " + item.getItemDisplayName() + " (" + Math.abs(item.getAmount() - startAmount) + ")");
               }
            }
         } else {
            System.out.println(client.getName() + " tried to spawn an item, but cheats aren't allowed");
         }
      } else {
         System.out.println(client.getName() + " tried to spawn an item, but isn't admin");
      }
   }
}
