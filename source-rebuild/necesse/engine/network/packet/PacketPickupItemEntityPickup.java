package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.pickup.PickupEntity;
import necesse.inventory.InventoryItem;

public class PacketPickupItemEntityPickup extends Packet {
   public final int pickupUniqueID;
   public final int targetSlot;
   public final InventoryItem item;
   public final boolean addedToNonPlayerInventory;
   public final long playerDeathAuth;
   public final int deathInventoryID;
   public final int deathInventorySlot;
   public final boolean deathIsLocked;

   public PacketPickupItemEntityPickup(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.pickupUniqueID = reader.getNextInt();
      this.targetSlot = reader.getNextByteUnsigned();
      this.item = InventoryItem.fromContentPacket(reader);
      this.addedToNonPlayerInventory = reader.getNextBoolean();
      if (reader.getNextBoolean()) {
         this.playerDeathAuth = reader.getNextLong();
         this.deathInventoryID = reader.getNextShortUnsigned();
         this.deathInventorySlot = reader.getNextShortUnsigned();
         this.deathIsLocked = reader.getNextBoolean();
      } else {
         this.playerDeathAuth = 0L;
         this.deathInventoryID = 0;
         this.deathInventorySlot = 0;
         this.deathIsLocked = false;
      }
   }

   public PacketPickupItemEntityPickup(ItemPickupEntity entity, InventoryItem item, boolean addedToNonPlayerInventory) {
      this.pickupUniqueID = entity.getUniqueID();
      this.targetSlot = entity.getTarget().slot;
      this.item = item;
      this.addedToNonPlayerInventory = addedToNonPlayerInventory;
      this.playerDeathAuth = entity.playerDeathAuth;
      this.deathInventoryID = entity.deathInventoryID;
      this.deathInventorySlot = entity.deathInventorySlot;
      this.deathIsLocked = entity.deathIsLocked;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.pickupUniqueID);
      writer.putNextByteUnsigned(this.targetSlot);
      InventoryItem.addPacketContent(item, writer);
      writer.putNextBoolean(addedToNonPlayerInventory);
      if (this.playerDeathAuth != 0L) {
         writer.putNextBoolean(true);
         writer.putNextLong(this.playerDeathAuth);
         writer.putNextShortUnsigned(this.deathInventoryID);
         writer.putNextShortUnsigned(this.deathInventorySlot);
         writer.putNextBoolean(this.deathIsLocked);
      } else {
         writer.putNextBoolean(false);
      }
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.getLevel() != null) {
         ClientClient target = client.getClient(this.targetSlot);
         if (target != null) {
            PickupEntity entity = client.getLevel().entityManager.pickups.get(this.pickupUniqueID, true);
            ItemPickupEntity itemEntity = entity instanceof ItemPickupEntity ? (ItemPickupEntity)entity : null;
            ItemPickupEntity.onPickup(itemEntity, target, this);
         } else {
            client.network.sendPacket(new PacketRequestPlayerData(this.targetSlot));
         }
      }
   }
}
