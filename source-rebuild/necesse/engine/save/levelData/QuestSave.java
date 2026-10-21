package necesse.engine.save.levelData;

import java.util.HashSet;
import necesse.engine.quest.Quest;
import necesse.engine.registries.QuestRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class QuestSave {
   public static HashSet<String> REMOVED_STRING_IDS = new HashSet<>();

   public static Quest loadSave(LoadData save) {
      try {
         String type = save.getUnsafeString("stringID");
         Quest quest = QuestRegistry.getNewQuest(type);
         quest.applyLoadData(save);
         return quest;
      } catch (Exception e) {
         String type = save.getUnsafeString("stringID", "N/A", false);
         if (!REMOVED_STRING_IDS.contains(type)) {
            System.err.println("Could not load quest with stringID " + type);
            e.printStackTrace();
         }

         return null;
      }
   }

   public static SaveData getSave(Quest quest) {
      SaveData save = new SaveData("QUEST");
      save.addUnsafeString("stringID", quest.getStringID());
      quest.addSaveData(save);
      return save;
   }

   static {
      REMOVED_STRING_IDS.add("killmobssettlement");
      REMOVED_STRING_IDS.add("deliveritemssettlement");
      REMOVED_STRING_IDS.add("havekilledmobssettlement");
      REMOVED_STRING_IDS.add("completetierxincursion");
   }
}
