package necesse.engine.commands.serverCommands.setupCommand;

import necesse.engine.network.packet.PacketUpdateTrinketSlots;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerEquipmentInventory;
import necesse.inventory.PlayerInventoryManager;

public class AsbjCommandBuild extends CharacterBuild {
   protected boolean godBuffs = false;
   public String[] trinkets;

   public AsbjCommandBuild(String... trinkets) {
      this.trinkets = trinkets;
   }

   public AsbjCommandBuild() {
      this.godBuffs = true;
   }

   @Override
   public void apply(ServerClient client) {
      if (this.godBuffs) {
         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.getBuff("debugbuff"), client.playerMob, 5000000, null), true);
         client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.getBuff("debuginvisibility"), client.playerMob, 5000000, null), true);
      } else {
         PlayerInventoryManager inventory = client.playerMob.getInv();
         client.playerMob.getInv().equipment.changeTrinketSlotsSize(Math.max(this.trinkets.length, 7));
         client.playerMob.equipmentBuffManager.updateTrinketBuffs();
         client.closeContainer(false);
         client.updateInventoryContainer();
         client.getServer().network.sendToAllClients(new PacketUpdateTrinketSlots(client));

         for (int i = 0; i < this.trinkets.length; i++) {
            PlayerEquipmentInventory trinketsInventory = inventory.equipment.getSelectedTrinketsInventory(i);
            if (i >= trinketsInventory.getSize()) {
               break;
            }

            trinketsInventory.setItem(i, new InventoryItem(this.trinkets[i]));
         }

         inventory.equipment.getSelectedCosmeticSlot(0).setItem(new InventoryItem("arcanichelmet"));
         inventory.equipment.getSelectedCosmeticSlot(1).setItem(new InventoryItem("leathershirt"));
         inventory.equipment.getSelectedCosmeticSlot(2).setItem(new InventoryItem("crystalboots"));
      }
   }
}
