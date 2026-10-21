package necesse.level.maps.levelData.settlementData;

import java.util.HashMap;
import java.util.HashSet;
import necesse.engine.save.LoadData;

public class SettlementQuestTierMigration {
   public static HashMap<String, String> QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE = new HashMap<>();

   public static long loadClientAuth(LoadData save) {
      return save.getLong("clientAuth");
   }

   public static HashSet<String> loadClientQuestsToObjectivesCompleted(LoadData save) {
      HashSet<String> questTiersCompleted = save.getStringHashSet("questTiersCompleted", new HashSet<>(), false);
      questTiersCompleted.removeIf(stringIDx -> !QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.containsKey(stringIDx));
      HashSet<String> objectivesCompleted = new HashSet<>();

      for (String stringID : questTiersCompleted) {
         String objectiveStringID = QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.get(stringID);
         if (objectiveStringID != null) {
            objectivesCompleted.add(objectiveStringID);
         }
      }

      return objectivesCompleted;
   }

   static {
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("evilsprotector", "defeatevilsprotector");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("queenspider", "defeatqueenspider");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("voidwizard", "defeatvoidwizard");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("chieftain", "defeatchieftain");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("swampguardian", "defeatswampguardian");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("ancientvulture", "defeatancientvulture");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("piratecaptain", "defeatpiratecaptain");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("reaper", "defeatreaper");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("cryoqueen", "defeatcryoqueen");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("thecursedcrone", "defeatthecursedcrone");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("pestwarden", "defeatpestwarden");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("sageandgrit", "defeatsageandgrit");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("fallenwizard", "defeatfallenwizard");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("tieroneincursion", "completetieroneincursion");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("tierfiveincursion", "completetierfiveincursion");
      QUEST_TIER_STRING_ID_TO_STORY_OBJECTIVE.put("ascendedwizard", "defeatascendedwizard");
   }
}
