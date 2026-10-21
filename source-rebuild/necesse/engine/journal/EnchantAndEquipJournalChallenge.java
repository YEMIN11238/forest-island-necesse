package necesse.engine.journal;

import java.util.ArrayList;
import necesse.engine.journal.listeners.EquipmentChangedJournalChallengeListener;
import necesse.engine.journal.listeners.LevelChangedJournalChallengeListener;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerEquipmentInventory;
import necesse.inventory.PlayerEquipmentSetInventoryManager;
import necesse.inventory.enchants.Enchantable;
import necesse.level.maps.Level;

public class EnchantAndEquipJournalChallenge
   extends SimpleJournalChallenge
   implements EquipmentChangedJournalChallengeListener,
   LevelChangedJournalChallengeListener {
   @Override
   public void onEquipmentChanged(
      ServerClient serverClient,
      PlayerEquipmentSetInventoryManager manager,
      ArrayList<PlayerEquipmentInventory> list,
      PlayerEquipmentInventory changedInventory,
      int changedSlot
   ) {
      if (list == manager.armor) {
         if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
            Level level = serverClient.playerMob.getLevel();
            if (level.isCave && JournalChallengeUtils.isSwampBiome(level.getBiome(serverClient.playerMob.getTileX(), serverClient.playerMob.getTileY()))) {
               InventoryItem changedItem = changedInventory.getItem(changedSlot);
               if (changedItem != null) {
                  this.checkEquipment(serverClient, changedInventory);
               }
            }
         }
      }
   }

   @Override
   public void onLevelChanged(ServerClient serverClient, Level oldLevel, Level newLevel) {
      if (!this.isCompleted(serverClient) && this.isJournalEntryDiscovered(serverClient)) {
         Level level = serverClient.playerMob.getLevel();
         if (level.isCave && JournalChallengeUtils.isSwampBiome(level.getBiome(serverClient.playerMob.getTileX(), serverClient.playerMob.getTileY()))) {
            this.checkEquipment(serverClient, serverClient.playerMob.getInv().equipment.getSelectedArmorInventory());
         }
      }
   }

   public void checkEquipment(ServerClient serverClient, PlayerEquipmentInventory armorInventory) {
      for (int slot = 0; slot < armorInventory.getSize(); slot++) {
         InventoryItem item = armorInventory.getCurrentUsedItem(slot);
         if (!this.isValidItem(item)) {
            return;
         }
      }

      this.markCompleted(serverClient);
      serverClient.forceCombineNewStats();
   }

   public boolean isValidItem(InventoryItem item) {
      return item != null && item.item instanceof Enchantable && ((Enchantable)item.item).getEnchantmentID(item) > 0;
   }
}
