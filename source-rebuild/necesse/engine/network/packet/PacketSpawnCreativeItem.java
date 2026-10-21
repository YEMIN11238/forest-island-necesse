package necesse.engine.network.packet;

import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.GameLog;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.container.Container;
import necesse.inventory.container.ContainerAction;
import necesse.inventory.container.ContainerActionResult;

public class PacketSpawnCreativeItem extends Packet {
   public final Packet itemContent;
   public final PacketSpawnCreativeItem.Destination destination;
   public final boolean clearIfSameItem;
   public final boolean clearIfCannotCombine;
   public final int clientResult;

   public PacketSpawnCreativeItem(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.destination = reader.getNextEnum(PacketSpawnCreativeItem.Destination.class);
      this.itemContent = reader.getNextContentPacket();
      this.clearIfSameItem = reader.getNextBoolean();
      this.clearIfCannotCombine = reader.getNextBoolean();
      this.clientResult = reader.getNextInt();
   }

   public PacketSpawnCreativeItem(
      InventoryItem item, PacketSpawnCreativeItem.Destination destination, boolean clearIfSameItem, boolean clearIfCannotCombine, int clientResult
   ) {
      this.itemContent = InventoryItem.getContentPacket(item);
      this.destination = destination;
      this.clearIfSameItem = clearIfSameItem;
      this.clearIfCannotCombine = clearIfCannotCombine;
      this.clientResult = clientResult;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextEnum(destination);
      writer.putNextContentPacket(this.itemContent);
      writer.putNextBoolean(clearIfSameItem);
      writer.putNextBoolean(clearIfCannotCombine);
      writer.putNextInt(clientResult);
   }

   public static void runAndSendAction(
      Client client, InventoryItem item, PacketSpawnCreativeItem.Destination destination, boolean clearIfSameItem, boolean clearIfCannotCombine
   ) {
      int result = runAction(client.getContainer(), client.getPlayer(), item.copy(), destination, clearIfSameItem, clearIfCannotCombine);
      client.network.sendPacket(new PacketSpawnCreativeItem(item, destination, clearIfSameItem, clearIfCannotCombine, result));
   }

   public static int runAction(
      Container container,
      PlayerMob playerMob,
      InventoryItem item,
      PacketSpawnCreativeItem.Destination destination,
      boolean clearIfSameItem,
      boolean clearIfCannotCombine
   ) {
      PlayerInventoryManager playerInventory = playerMob.getInv();
      switch (destination) {
         case Inventory:
            playerInventory.addItem(item, false, "give", null);
            return 0;
         case DragSlot:
            InventoryItem draggingItem = playerMob.getDraggingItem();
            if (draggingItem == null) {
               playerMob.setDraggingItem(item);
               return 1;
            } else if (draggingItem.combine(playerMob.getLevel(), playerMob, playerInventory.drag, 0, item.copy(), "spawnitem", null).success) {
               return 3;
            } else if (!draggingItem.equals(playerMob.getLevel(), item, true, false, "equals")) {
               if (clearIfCannotCombine) {
                  playerMob.setDraggingItem(null);
                  return 2;
               }

               int draggingSlotIndex = container.getClientDraggingSlot().getContainerIndex();
               ContainerActionResult resultx = container.applyContainerAction(draggingSlotIndex, ContainerAction.QUICK_TRASH);
               playerMob.setDraggingItem(item);
               return GameRandom.prime(8346) * resultx.value;
            } else if (clearIfSameItem) {
               playerMob.setDraggingItem(null);
               return 2;
            }
         default:
            return -1;
         case Hotbar:
            AtomicInteger result = new AtomicInteger(4);
            playerInventory.addItem(item.copy(), false, "give", (inventory, inventorySlot, amountAdded) -> {
               if (amountAdded > 0) {
                  if (inventorySlot >= container.CLIENT_HOTBAR_START - 1 && inventorySlot <= container.CLIENT_HOTBAR_END - 1) {
                     playerMob.setSelectedSlot(inventorySlot);
                     result.set(5 + inventorySlot);
                  } else if (!playerInventory.player.hotbarLocked) {
                     int slotToUse = playerMob.getSelectedSlot();
                     if (inventory.isItemLocked(slotToUse)) {
                        slotToUse = -1;

                        for (int i = container.CLIENT_HOTBAR_START - 1; i <= container.CLIENT_HOTBAR_END - 1; i++) {
                           if (!inventory.isItemLocked(i)) {
                              slotToUse = i;
                              break;
                           }
                        }
                     }

                     if (slotToUse != -1) {
                        inventory.swapItems(slotToUse, inventorySlot);
                        playerMob.setSelectedSlot(slotToUse);
                     }

                     result.set(GameRandom.prime(5720) * slotToUse * inventorySlot);
                  }
               }
            });
            return result.get();
      }
   }

   @Override
   public void processServer(NetworkPacket packet, Server server, ServerClient client) {
      if (server.world.settings.creativeMode) {
         InventoryItem item = InventoryItem.fromContentPacket(this.itemContent);
         if (ItemRegistry.isValidCreativeItem(item.item)) {
            int result = runAction(client.getContainer(), client.playerMob, item, this.destination, this.clearIfSameItem, this.clearIfCannotCombine);
            if (result != this.clientResult) {
               client.playerMob.getInv().markFullDirty();
            }
         } else {
            GameLog.err.println(client.getName() + " tried to spawn an invalid creative item: " + item.getItemDisplayName() + " (" + item.item.idData + ")");
            client.playerMob.getInv().markFullDirty();
         }
      } else {
         GameLog.err.println(client.getName() + " tried to spawn a creative item, but creative mode isn't enabled");
         client.playerMob.getInv().markFullDirty();
      }
   }

   public enum Destination {
      Inventory,
      DragSlot,
      Hotbar;
   }
}
