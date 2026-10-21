package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.dialogues.CollectorSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceBreakUpSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerEnterRelationshipDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerGiveItemDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerKissDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerReceiveGiftDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerStartDatingDialogue;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;

public class SettlerDialogueRegistry extends ClassedGameRegistry<SettlerDialogue, SettlerDialogueRegistry.SettlerDialogueRegistryElement> {
   public static final SettlerDialogueRegistry instance = new SettlerDialogueRegistry();

   private SettlerDialogueRegistry() {
      super("SettlerDialogue", 32767);
   }

   @Override
   public void registerCore() {
      registerSettlerDialogue("collector", CollectorSettlerDialogue.class);
      registerSettlerDialogue("startdating", RomanceSettlerStartDatingDialogue.class);
      registerSettlerDialogue("enterrelationship", RomanceSettlerEnterRelationshipDialogue.class);
      registerSettlerDialogue("divorce", RomanceBreakUpSettlerDialogue.class);
      registerSettlerDialogue("romanceactivity", RomanceSettlerDialogue.class);
      registerSettlerDialogue("giveitem", RomanceSettlerGiveItemDialogue.class);
      registerSettlerDialogue("kiss", RomanceSettlerKissDialogue.class);
      registerSettlerDialogue("receivegift", RomanceSettlerReceiveGiftDialogue.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerSettlerDialogue(String stringID, Class<? extends SettlerDialogue> dialogueClass) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register settler dialogues");
      }

      try {
         return instance.register(stringID, new SettlerDialogueRegistry.SettlerDialogueRegistryElement(dialogueClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register SettlerDialogue " + dialogueClass.getSimpleName() + ": Missing constructor with HumanMob parameter");
         return -1;
      }
   }

   public static int replaceSettlerDialogue(String stringID, Class<? extends SettlerDialogue> dialogueClass) {
      try {
         return instance.replace(stringID, new SettlerDialogueRegistry.SettlerDialogueRegistryElement(dialogueClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register SettlerDialogue " + dialogueClass.getSimpleName() + ": Missing constructor with HumanMob parameter");
         return -1;
      }
   }

   public static SettlerDialogue getNewSettlerDialogue(int id, HumanMob mob) {
      if (id >= 0 && id < instance.size()) {
         try {
            return instance.getElement(id).newInstance(mob);
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
         }
      } else {
         return null;
      }
   }

   public static SettlerDialogue getNewSettlerDialogue(String stringID, HumanMob mob) {
      return getNewSettlerDialogue(getSettlerDialogueID(stringID), mob);
   }

   public static int getSettlerDialogueID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getSettlerDialogueStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class SettlerDialogueRegistryElement extends ClassIDDataContainer<SettlerDialogue> {
      public SettlerDialogueRegistryElement(Class<? extends SettlerDialogue> dialogueClass) throws NoSuchMethodException {
         super(dialogueClass, HumanMob.class);
      }
   }
}
