package necesse.engine.registries;

import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.save.LoadData;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.CursedCroneArenasLevelData;
import necesse.level.maps.levelData.DeprecatedCursedCroneArenasLevelData;
import necesse.level.maps.levelData.DeprecatedNPCVillageLevelData;
import necesse.level.maps.levelData.LevelData;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.OneWorldPirateVillageData;
import necesse.level.maps.levelData.PathBreakDownLevelData;
import necesse.level.maps.levelData.settlementData.DeprecatedJobsLevelData;
import necesse.level.maps.levelData.settlementData.DeprecatedSettlementLevelData;
import necesse.level.maps.levelData.villageShops.DeprecatedVillageShopsData;

public class LevelDataRegistry extends ClassedGameRegistry<LevelData, LevelDataRegistry.LevelDataRegistryElement> {
   public static final LevelDataRegistry instance = new LevelDataRegistry();

   private LevelDataRegistry() {
      super("LevelData", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "leveldata"));
      registerLevelData("settlement", DeprecatedSettlementLevelData.class);
      registerLevelData("jobs", DeprecatedJobsLevelData.class);
      registerLevelData("villageshops", DeprecatedVillageShopsData.class);
      registerLevelData("pathbreak", PathBreakDownLevelData.class);
      registerLevelData("npcvillagedata", DeprecatedNPCVillageLevelData.class);
      registerLevelData("oneworldnpcvillagedata", OneWorldNPCVillageData.class);
      registerLevelData("oneworldpiratevillagedata", OneWorldPirateVillageData.class);
      registerLevelData("cursedcronearenas", DeprecatedCursedCroneArenasLevelData.class);
      registerLevelData("oneworldcursedcronearenas", CursedCroneArenasLevelData.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerLevelData(String stringID, Class<? extends LevelData> levelDataClass) {
      try {
         return instance.register(stringID, new LevelDataRegistry.LevelDataRegistryElement(levelDataClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register LevelData " + levelDataClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static LevelData loadLevelData(Level level, LoadData save) {
      String stringID = save.hasLoadDataByName("stringID") ? save.getUnsafeString("stringID") : null;

      try {
         int elementID = instance.getElementID(stringID);
         if (elementID == -1) {
            System.err.println("Could not find LevelData with stringID: " + stringID);
            return null;
         } else {
            LevelData data = instance.getElement(elementID).newInstance();
            data.setLevel(level);
            data.applyLoadData(save);
            return data;
         }
      } catch (Exception e) {
         System.err.println("Could not instantiate level data with id " + stringID);
         e.printStackTrace();
         return null;
      }
   }

   protected static class LevelDataRegistryElement extends ClassIDDataContainer<LevelData> {
      public LevelDataRegistryElement(Class<? extends LevelData> levelDataClass) throws NoSuchMethodException {
         super(levelDataClass);
      }
   }
}
