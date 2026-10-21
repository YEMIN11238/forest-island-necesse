package necesse.engine.registries;

import java.util.Comparator;
import java.util.TreeSet;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.levelData.settlementData.settler.FoodQuality;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.thoughts.DietSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.PopulationSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.QualityOfLifeSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.RoomQualitySettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.RoomSizeSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SharingRoomSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class SettlerThoughtRegistry extends GameRegistry<SettlerThoughtRegistry.SettlerThoughtRegistryElement> {
   public static final SettlerThoughtRegistry instance = new SettlerThoughtRegistry();
   public static TreeSet<PopulationSettlerThought> populationThoughts = new TreeSet<>(Comparator.comparingInt(s -> s.population));
   public static TreeSet<RoomSizeSettlerThought> roomSizes = new TreeSet<>(Comparator.comparingInt(s -> s.minSize));
   public static TreeSet<RoomQualitySettlerThought> roomQualities = new TreeSet<>(Comparator.comparingInt(s -> s.minScore));
   public static TreeSet<DietSettlerThought> dietVarieties = new TreeSet<>(Comparator.comparingInt(s -> s.variety));
   public static SettlerThought qualityOfLifeThought;
   public static SettlerThought noFoodThought;
   public static SettlerThought noBedThought;
   public static SettlerThought bedOutsideThought;
   public static SettlerThought missingFloorThought;
   public static SettlerThought missingLightsThought;
   public static SettlerThought sharingRoomThought;
   public static SettlerThought recentlyRecruitedThought;

   private SettlerThoughtRegistry() {
      super("SettlerThought", 32767);
   }

   @Override
   public void registerCore() {
      registerSettlerThought("qualityoflife", qualityOfLifeThought = new QualityOfLifeSettlerThought());
      registerSettlerThought("tinysettlement", new PopulationSettlerThought("tinysettlement", 0, 40));
      registerSettlerThought("smallsettlement", new PopulationSettlerThought("smallsettlement", 6, 30));
      registerSettlerThought("averagesettlement", new PopulationSettlerThought("averagesettlement", 12, 20));
      registerSettlerThought("largesettlement", new PopulationSettlerThought("largesettlement", 18, 10));
      registerSettlerThought("hugesettlement", new PopulationSettlerThought("hugesettlement", 24, 0));
      registerSettlerThought("babyroomsize", new RoomSizeSettlerThought("sizebaby", 0, 0));
      registerSettlerThought("tinyroomsize", new RoomSizeSettlerThought("sizetiny", 10, 4));
      registerSettlerThought("smallroomsize", new RoomSizeSettlerThought("sizesmall", 20, 8));
      registerSettlerThought("mediocreroomsize", new RoomSizeSettlerThought("sizemediocre", 25, 10));
      registerSettlerThought("decentroomsize", new RoomSizeSettlerThought("sizedecent", 30, 12));
      registerSettlerThought("largeroomsize", new RoomSizeSettlerThought("sizelarge", 40, 15));
      registerSettlerThought("hugeroomsize", new RoomSizeSettlerThought("sizehuge", 50, 18));
      registerSettlerThought("enormousroomsize", new RoomSizeSettlerThought("sizeenormous", 60, 20));
      registerSettlerThought("plainroomquality", new RoomQualitySettlerThought("roomplain", 0, 0));
      registerSettlerThought("dullroomquality", new RoomQualitySettlerThought("roomdull", 1, 4));
      registerSettlerThought("simpleroomquality", new RoomQualitySettlerThought("roomsimple", 2, 7));
      registerSettlerThought("normalroomquality", new RoomQualitySettlerThought("roomnormal", 3, 10));
      registerSettlerThought("goodroomquality", new RoomQualitySettlerThought("roomgood", 4, 13));
      registerSettlerThought("impressiveroomquality", new RoomQualitySettlerThought("roomimpressive", 5, 15));
      registerSettlerThought("wonderfulroomquality", new RoomQualitySettlerThought("roomwonderful", 6, 17));
      registerSettlerThought("unrivaledroomquality", new RoomQualitySettlerThought("roomunrivaled", 7, 20));
      registerSettlerThought("nofood", noFoodThought = new SimpleSettlerThought("settlement", "nofoodmood", 0));

      for (FoodQuality foodQuality : Settler.foodQualities) {
         foodQuality.registerThought();
      }

      registerSettlerThought("samedietvariety", new DietSettlerThought("dietsame", 0, 0));
      registerSettlerThought("slightlydietvariety", new DietSettlerThought("dietslightly", 2, 10));
      registerSettlerThought("somewhatdietvariety", new DietSettlerThought("dietsomewhat", 5, 20));
      registerSettlerThought("nicelydietvariety", new DietSettlerThought("dietnicely", 8, 30));
      registerSettlerThought("extremelydietvariety", new DietSettlerThought("dietextremely", 12, 40));
      registerSettlerThought("recreationdeprived", new SimpleSettlerThought("settlement", "recreationdeprived", 0));
      registerSettlerThought("recreationsatisfied", new SimpleSettlerThought("settlement", "recreationsatisfied", 10));
      registerSettlerThought("recreationfulfilled", new SimpleSettlerThought("settlement", "recreationfulfilled", 20));
      registerSettlerThought("nobed", noBedThought = new SimpleSettlerThought("settlement", "nobed", -40));
      registerSettlerThought("bedoutside", bedOutsideThought = new SimpleSettlerThought("settlement", "bedoutside", -30));
      registerSettlerThought("missingfloor", missingFloorThought = new SimpleSettlerThought("settlement", "roommissingfloor", -10));
      registerSettlerThought("missinglights", missingLightsThought = new SimpleSettlerThought("settlement", "roommissinglights", -10));
      registerSettlerThought("sharingroom", sharingRoomThought = new SharingRoomSettlerThought());
      registerSettlerThought("recentlyrecruited", recentlyRecruitedThought = new SimpleSettlerThought("settlement", "recentlyrecruited", 40));
      registerSettlerThought("lifepartner", new SimpleSettlerThought("settlement", "lifepartner", 30));
   }

   protected void onRegister(SettlerThoughtRegistry.SettlerThoughtRegistryElement object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      for (SettlerThoughtRegistry.SettlerThoughtRegistryElement element : this.getElements()) {
         if (element.thought instanceof PopulationSettlerThought) {
            populationThoughts.add((PopulationSettlerThought)element.thought);
         }

         if (element.thought instanceof RoomSizeSettlerThought) {
            roomSizes.add((RoomSizeSettlerThought)element.thought);
         }

         if (element.thought instanceof RoomQualitySettlerThought) {
            roomQualities.add((RoomQualitySettlerThought)element.thought);
         }

         if (element.thought instanceof DietSettlerThought) {
            dietVarieties.add((DietSettlerThought)element.thought);
         }
      }
   }

   public static int registerSettlerThought(String stringID, SettlerThought thought) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register happiness modifiers");
      } else {
         return instance.register(stringID, new SettlerThoughtRegistry.SettlerThoughtRegistryElement(thought));
      }
   }

   public static int replaceSettlerThought(String stringID, SettlerThought thought) {
      return instance.replace(stringID, new SettlerThoughtRegistry.SettlerThoughtRegistryElement(thought));
   }

   public static SettlerThought getSettlerThought(int id) {
      if (id >= 0 && id < instance.size()) {
         SettlerThoughtRegistry.SettlerThoughtRegistryElement e = instance.getElement(id);
         return e == null ? null : e.thought;
      } else {
         return null;
      }
   }

   public static SettlerThought getSettlerThought(String stringID) {
      SettlerThoughtRegistry.SettlerThoughtRegistryElement e = instance.getElement(stringID);
      return e == null ? null : e.thought;
   }

   public static int getSettlerThoughtID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getSettlerThoughtStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static PopulationSettlerThought getPopulationThought(int totalSettlers) {
      for (PopulationSettlerThought thought : populationThoughts.descendingSet()) {
         if (totalSettlers >= thought.population) {
            return thought;
         }
      }

      return null;
   }

   public static RoomSizeSettlerThought getRoomSizeThought(int roomSize) {
      for (RoomSizeSettlerThought size : roomSizes.descendingSet()) {
         if (roomSize >= size.minSize) {
            return size;
         }
      }

      return null;
   }

   public static RoomQualitySettlerThought getRoomQualityThought(int furnitureScore) {
      for (RoomQualitySettlerThought quality : roomQualities.descendingSet()) {
         if (furnitureScore >= quality.minScore) {
            return quality;
         }
      }

      return null;
   }

   public static DietSettlerThought getDietVarietyThought(int variety) {
      for (DietSettlerThought thought : dietVarieties.descendingSet()) {
         if (variety >= thought.variety) {
            return thought;
         }
      }

      return null;
   }

   protected static class SettlerThoughtRegistryElement implements IDDataContainer {
      public final SettlerThought thought;

      public SettlerThoughtRegistryElement(SettlerThought thought) {
         this.thought = thought;
      }

      @Override
      public IDData getIDData() {
         return this.thought.idData;
      }
   }
}
