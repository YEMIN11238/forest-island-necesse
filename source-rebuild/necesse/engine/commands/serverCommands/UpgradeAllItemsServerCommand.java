package necesse.engine.commands.serverCommands;

import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.IntParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerEquipmentInventory;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.item.upgradeUtils.UpgradableItem;

public class UpgradeAllItemsServerCommand extends ModularChatCommand {
   public UpgradeAllItemsServerCommand() {
      super(
         "upgradeall",
         "Clears or sets the tier on all items equipped and on your hotbar",
         PermissionLevel.ADMIN,
         true,
         new CmdParameter("tier", new IntParameterHandler())
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      if (serverClient == null) {
         logs.add("Cannot run upgrade command from server");
      } else {
         int tier = (Integer)args[0];
         int totalUpgrades = 0;
         PlayerInventoryManager inventory = serverClient.playerMob.getInv();
         PlayerEquipmentInventory armorInventory = inventory.equipment.getSelectedArmorInventory();

         for (int slot = 0; slot < armorInventory.getSize(); slot++) {
            InventoryItem item = armorInventory.getItem(slot);
            if (item != null && item.item instanceof UpgradableItem) {
               item.item.setUpgradeTier(item, tier);
               armorInventory.updateSlot(slot);
               armorInventory.markDirty(slot);
               totalUpgrades++;
            }
         }

         for (int slot = 0; slot < 10; slot++) {
            InventoryItem item = inventory.main.getItem(slot);
            if (item != null && item.item instanceof UpgradableItem) {
               item.item.setUpgradeTier(item, tier);
               inventory.main.updateSlot(slot);
               inventory.main.markDirty(slot);
               totalUpgrades++;
            }
         }

         if (totalUpgrades <= 0) {
            logs.add("No upgradable items found in armor or hotbar");
         } else {
            logs.add("Upgraded " + totalUpgrades + " items to tier " + tier);
         }
      }
   }
}
