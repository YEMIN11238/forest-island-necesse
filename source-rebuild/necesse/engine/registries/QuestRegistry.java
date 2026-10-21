package necesse.engine.registries;

import necesse.engine.quest.KillMobsTitleQuest;
import necesse.engine.quest.Quest;

public class QuestRegistry extends EmptyConstructorGameRegistry<Quest> {
   public static final QuestRegistry instance = new QuestRegistry();

   private QuestRegistry() {
      super("Quest", 32762);
   }

   @Override
   public void registerCore() {
      registerQuest("killmobstitle", KillMobsTitleQuest.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static void registerQuest(String stringID, Class<? extends Quest> questClass) {
      instance.registerClass(stringID, questClass);
   }

   public static Quest getNewQuest(int id) {
      return instance.getNewInstance(id);
   }

   public static int getQuestID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getQuestID(Class<? extends Quest> clazz) {
      return instance.getElementID(clazz);
   }

   public static Quest getNewQuest(String stringID) {
      return instance.getNewInstance(stringID);
   }
}
