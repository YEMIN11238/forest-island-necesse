package necesse.inventory.item.questItem;

import java.util.ArrayList;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;

public class ZombieArmQuestItem extends QuestItem {
   public static final ArrayList<String> droppedByMobs = new ArrayList<>();

   public ZombieArmQuestItem() {
      super(new LocalMessage("itemtooltip", "zombiearmobtain"));
   }

   @Override
   public LootTable getExtraMobDrops(ServerClient client, Mob mob) {
      return droppedByMobs.contains(mob.getStringID()) && client.playerMob.getInv().getAmount(this, false, false, true, true, "questdrop") <= 0
         ? new LootTable(new ChanceLootItem(0.5F, this.getStringID()))
         : super.getExtraMobDrops(client, mob);
   }

   static {
      droppedByMobs.add("zombie");
      droppedByMobs.add("zombiearcher");
      droppedByMobs.add("crawlingzombie");
   }
}
