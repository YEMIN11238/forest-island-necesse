package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureAnimalRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureButterflyWithNetRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureCritterWithNetRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CaptureFireflyWithNetRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CraftAndGiveItemRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.CraftAndPlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.FindAndGiveItemRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.FindAndPlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.GiveItemRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.KillMobAndVictimNameRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.KillMobRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.PlaceObjectRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeAndLocationRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitBiomeRomanceActivity;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.VisitLocationRomanceActivity;

public class RomanceActivitiesRegistry extends ClassedGameRegistry<RomanceActivity, RomanceActivitiesRegistry.RomanceActivityRegistryElement> {
   public static final RomanceActivitiesRegistry instance = new RomanceActivitiesRegistry();

   private RomanceActivitiesRegistry() {
      super("RomanceActivities", 32767);
   }

   @Override
   public void registerCore() {
      registerRomanceActivity("killmobnew", KillMobRomanceActivity.class);
      registerRomanceActivity("killmobandvictimname", KillMobAndVictimNameRomanceActivity.class);
      registerRomanceActivity("visitbiomenew", VisitBiomeRomanceActivity.class);
      registerRomanceActivity("visitlocation", VisitLocationRomanceActivity.class);
      registerRomanceActivity("visitbiomeandlocation", VisitBiomeAndLocationRomanceActivity.class);
      registerRomanceActivity("captureanimal", CaptureAnimalRomanceActivity.class);
      registerRomanceActivity("capturecritternew", CaptureCritterWithNetRomanceActivity.class);
      registerRomanceActivity("capturebutterfly", CaptureButterflyWithNetRomanceActivity.class);
      registerRomanceActivity("capturefirefly", CaptureFireflyWithNetRomanceActivity.class);
      registerRomanceActivity("giveitemnew", GiveItemRomanceActivity.class);
      registerRomanceActivity("findandgiveitem", FindAndGiveItemRomanceActivity.class);
      registerRomanceActivity("craftandgiveitem", CraftAndGiveItemRomanceActivity.class);
      registerRomanceActivity("placeobject", PlaceObjectRomanceActivity.class);
      registerRomanceActivity("craftandplaceobjectnew", CraftAndPlaceObjectRomanceActivity.class);
      registerRomanceActivity("findandplaceobject", FindAndPlaceObjectRomanceActivity.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerRomanceActivity(String stringID, Class<? extends RomanceActivity> romanceActivity) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register settler personalities");
      }

      try {
         return instance.register(stringID, new RomanceActivitiesRegistry.RomanceActivityRegistryElement(romanceActivity));
      } catch (NoSuchMethodException e) {
         System.err
            .println("Could not register SettlerPersonality " + romanceActivity.getSimpleName() + ": Missing constructor with PlayerRomanceManager parameter");
         return -1;
      }
   }

   public static RomanceActivity getNewRomanceActivity(int id, PlayerRomanceManager romanceManager) {
      if (id >= 0 && id < instance.size()) {
         try {
            return instance.getElement(id).newInstance(romanceManager);
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
         }
      } else {
         return null;
      }
   }

   public static RomanceActivity getNewRomanceActivity(String stringID, PlayerRomanceManager romanceManager) {
      return getNewRomanceActivity(getRomanceActivityID(stringID), romanceManager);
   }

   public static int getRomanceActivityID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getRomanceActivityStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class RomanceActivityRegistryElement extends ClassIDDataContainer<RomanceActivity> {
      public RomanceActivityRegistryElement(Class<? extends RomanceActivity> aClass) throws NoSuchMethodException {
         super(aClass, PlayerRomanceManager.class);
      }
   }
}
