package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.objectives.CompleteTierFiveIncursionStoryObjective;
import necesse.engine.storyObjectives.objectives.CompleteTierOneIncursionStoryObjective;
import necesse.engine.storyObjectives.objectives.CraftPickaxeStoryObjective;
import necesse.engine.storyObjectives.objectives.EquippingTrinketStoryObjective;
import necesse.engine.storyObjectives.objectives.ExploringNecesseStoryObjective;
import necesse.engine.storyObjectives.objectives.GettingStartedStoryObjective;
import necesse.engine.storyObjectives.objectives.GoMiningStoryObjective;
import necesse.engine.storyObjectives.objectives.RecruitSettlerStoryObjective;
import necesse.engine.storyObjectives.objectives.StartSettlementStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatAncientVultureStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatAscendedWizardStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatChieftainStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatCryoQueenStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatEvilsProtectorStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatFallenWizardStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatPestWardenStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatPirateCaptainStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatQueenSpiderStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatReaperStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatSageAndGritStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatSwampGuardianStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatTheCursedCroneStoryObjective;
import necesse.engine.storyObjectives.objectives.bossObjectives.DefeatVoidWizardStoryObjective;
import necesse.engine.util.GameUtils;

public class StoryObjectiveRegistry extends ClassedGameRegistry<StoryObjective, StoryObjectiveRegistry.StoryObjectiveRegistryElement> {
   public static final StoryObjectiveRegistry instance = new StoryObjectiveRegistry();
   private static ArrayList<StoryObjectiveRegistry.StoryObjectiveRegistryElement> sortedElements;
   private static ArrayList<StoryObjectiveRegistry.StoryObjectiveRegistryElement> sortedMajorObjectives;

   private StoryObjectiveRegistry() {
      super("StoryObjective", 32762);
   }

   @Override
   public void registerCore() {
      registerObjective("gettingstarted", GettingStartedStoryObjective.class, false);
      registerObjective("equippingtrinket", EquippingTrinketStoryObjective.class, false);
      registerObjective("craftpickaxe", CraftPickaxeStoryObjective.class, false);
      registerObjective("gomining", GoMiningStoryObjective.class, false);
      registerObjective("startsettlement", StartSettlementStoryObjective.class, false);
      registerObjective("exploringnecesse", ExploringNecesseStoryObjective.class, false);
      registerObjective("recruitsettler", RecruitSettlerStoryObjective.class, false);
      registerObjective("defeatevilsprotector", DefeatEvilsProtectorStoryObjective.class, true);
      registerObjective("defeatqueenspider", DefeatQueenSpiderStoryObjective.class, true);
      registerObjective("defeatvoidwizard", DefeatVoidWizardStoryObjective.class, true);
      registerObjective("defeatchieftain", DefeatChieftainStoryObjective.class, true);
      registerObjective("defeatswampguardian", DefeatSwampGuardianStoryObjective.class, true);
      registerObjective("defeatancientvulture", DefeatAncientVultureStoryObjective.class, true);
      registerObjective("defeatpiratecaptain", DefeatPirateCaptainStoryObjective.class, true);
      registerObjective("defeatreaper", DefeatReaperStoryObjective.class, true);
      registerObjective("defeatcryoqueen", DefeatCryoQueenStoryObjective.class, true);
      registerObjective("defeatthecursedcrone", DefeatTheCursedCroneStoryObjective.class, true);
      registerObjective("defeatpestwarden", DefeatPestWardenStoryObjective.class, true);
      registerObjective("defeatsageandgrit", DefeatSageAndGritStoryObjective.class, true);
      registerObjective("defeatfallenwizard", DefeatFallenWizardStoryObjective.class, true);
      registerObjective("completetieroneincursion", CompleteTierOneIncursionStoryObjective.class, true);
      registerObjective("completetierfiveincursion", CompleteTierFiveIncursionStoryObjective.class, true);
      registerObjective("defeatascendedwizard", DefeatAscendedWizardStoryObjective.class, true);
   }

   @Override
   protected void onRegistryClose() {
      sortedElements = new ArrayList<>(this.size());
      sortedMajorObjectives = new ArrayList<>();

      for (StoryObjectiveRegistry.StoryObjectiveRegistryElement element : this.getElements()) {
         sortedElements.add(element);
      }

      sortedElements.sort(StoryObjectiveRegistry.StoryObjectiveRegistryElement::compareTo);

      for (int i = 0; i < sortedElements.size(); i++) {
         StoryObjectiveRegistry.StoryObjectiveRegistryElement element = sortedElements.get(i);
         element.sortedIndex = i;
         if (element.isMajorObjective) {
            element.majorObjectiveIndex = sortedMajorObjectives.size();
            sortedMajorObjectives.add(element);
         }
      }
   }

   public static StoryObjectiveRegistry.StoryObjectiveRegistryElement registerObjective(
      String stringID, Class<? extends StoryObjective> objectiveClass, boolean isMajorObjective
   ) {
      try {
         StoryObjectiveRegistry.StoryObjectiveRegistryElement element = new StoryObjectiveRegistry.StoryObjectiveRegistryElement(
            objectiveClass, isMajorObjective
         );
         instance.register(stringID, element);
         return element;
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(objectiveClass.getSimpleName() + " does not have a constructor with StoryObjectiveManager parameter");
      }
   }

   public static int getObjectiveID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getObjectiveStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static ArrayList<StoryObjective> createObjectiveLine(StoryObjectiveManager manager) {
      try {
         ArrayList<StoryObjective> objectiveLine = new ArrayList<>(sortedElements.size());

         for (StoryObjectiveRegistry.StoryObjectiveRegistryElement element : sortedElements) {
            StoryObjective storyObjective = element.newInstance(manager);
            storyObjective.getIDData().setData(element.getID(), element.getStringID());
            objectiveLine.add(storyObjective);
         }

         return objectiveLine;
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         throw new RuntimeException(e);
      }
   }

   public static boolean objectiveExists(String stringID) {
      return instance.getElementID(stringID) != -1;
   }

   public static boolean isMajorObjective(int id) {
      return instance.getElement(id).isMajorObjective;
   }

   public static boolean isMajorObjective(String stringID) {
      return instance.getElement(stringID).isMajorObjective;
   }

   public static int getSortedIndex(int id) {
      return instance.getElement(id).sortedIndex;
   }

   public static int getSortedIndex(String stringID) {
      return instance.getElement(stringID).sortedIndex;
   }

   public static int getMajorObjectiveIndex(int id) {
      return instance.getElement(id).majorObjectiveIndex;
   }

   public static int getMajorObjectiveIndex(String stringID) {
      return instance.getElement(stringID).majorObjectiveIndex;
   }

   public static Iterable<String> getSortedStringIDs() {
      return GameUtils.mapIterable(sortedElements.iterator(), IDDataContainer::getStringID);
   }

   public static String getStringIDBySortedIndex(int sortedIndex) {
      return sortedElements.get(sortedIndex).getStringID();
   }

   public static String getStringIDByMajorObjectiveIndex(int majorObjectiveIndex) {
      return sortedMajorObjectives.get(majorObjectiveIndex).getStringID();
   }

   public static class StoryObjectiveRegistryElement
      extends ClassIDDataContainer<StoryObjective>
      implements Comparable<StoryObjectiveRegistry.StoryObjectiveRegistryElement> {
      public final boolean isMajorObjective;
      private int sortedIndex = -1;
      private int majorObjectiveIndex = -1;
      private String sortedResultStringID;
      private boolean sortedShowBefore;

      public StoryObjectiveRegistryElement(Class<? extends StoryObjective> objectiveClass, boolean isMajorObjective) throws NoSuchMethodException {
         super(objectiveClass, StoryObjectiveManager.class);
         this.isMajorObjective = isMajorObjective;
      }

      public void showBeforeObjective(String objectiveStringID) {
         this.sortedResultStringID = objectiveStringID;
         this.sortedShowBefore = true;
      }

      public void showAfterObjective(String objectiveStringID) {
         this.sortedResultStringID = objectiveStringID;
         this.sortedShowBefore = false;
      }

      public int compareTo(StoryObjectiveRegistry.StoryObjectiveRegistryElement o) {
         if (this.sortedResultStringID != null && o.getStringID().equals(this.sortedResultStringID)) {
            return this.sortedShowBefore ? -1 : 1;
         } else {
            return Integer.compare(this.getID(), o.getID());
         }
      }
   }
}
