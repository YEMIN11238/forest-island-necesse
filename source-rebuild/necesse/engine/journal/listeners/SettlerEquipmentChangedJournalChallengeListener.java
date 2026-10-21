package necesse.engine.journal.listeners;

import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryRange;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public interface SettlerEquipmentChangedJournalChallengeListener {
   void onSettlerEquipmentChanged(ServerClient var1, ServerSettlementData var2, LevelSettler var3, InventoryRange var4, int var5, boolean var6);
}
