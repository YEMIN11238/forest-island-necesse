package necesse.engine.commands.serverCommands.setupCommand;

import necesse.engine.network.packet.PacketUpdateTrinketSlots;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerEquipmentInventory;
import necesse.inventory.PlayerInventoryManager;

public class SimpleTrinketSetBuild extends CharacterBuild {
   public String[] trinkets;

   public SimpleTrinketSetBuild(String... trinkets) {
      this.trinkets = trinkets;
   }

   @Override
   public void apply(ServerClient client) {
      client.playerMob.getInv().equipment.changeTrinketSlotsSize(Math.max(this.trinkets.length, 4));
      client.playerMob.equipmentBuffManager.updateTrinketBuffs();
      client.closeContainer(false);
      client.updateInventoryContainer();
      client.getServer().network.sendToAllClients(new PacketUpdateTrinketSlots(client));
      PlayerInventoryManager inventory = client.playerMob.getInv();

      for (int i = 0; i < this.trinkets.length; i++) {
         PlayerEquipmentInventory trinketsInventory = inventory.equipment.getSelectedTrinketsInventory(i);
         if (i >= trinketsInventory.getSize()) {
            break;
         }

         trinketsInventory.setItem(i, new InventoryItem(this.trinkets[i]));
      }
   }
}
