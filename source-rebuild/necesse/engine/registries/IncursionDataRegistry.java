package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.incursion.BiomeAscendedIncursionData;
import necesse.level.maps.incursion.BiomeExtractionIncursionData;
import necesse.level.maps.incursion.BiomeHuntIncursionData;
import necesse.level.maps.incursion.BiomeTrialIncursionData;
import necesse.level.maps.incursion.IncursionData;

public class IncursionDataRegistry extends ClassedGameRegistry<IncursionData, IncursionDataRegistry.IncursionDataRegistryElement> {
   public static final IncursionDataRegistry instance = new IncursionDataRegistry();

   private IncursionDataRegistry() {
      super("IncursionData", 250);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "incursions"));
      registerIncursionData("hunt", BiomeHuntIncursionData.class);
      registerIncursionData("extraction", BiomeExtractionIncursionData.class);
      registerIncursionData("trial", BiomeTrialIncursionData.class);
      registerIncursionData("ascended", BiomeAscendedIncursionData.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerIncursionData(String stringID, Class<? extends IncursionData> pickupClass) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register pickups");
      }

      try {
         return instance.register(stringID, new IncursionDataRegistry.IncursionDataRegistryElement(pickupClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register PickupEntity " + pickupClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static IncursionData getNewIncursionData(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static IncursionData getNewIncursionData(String stringID) {
      return getNewIncursionData(getIncursionDataID(stringID));
   }

   public static <C extends IncursionData> void applyIncursionDataIDData(C incursionData) {
      instance.applyIDData((Class<? extends IncursionData>)incursionData.getClass(), incursionData.idData);
   }

   public static int getIncursionDataID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getIncursionDataID(Class<? extends IncursionData> clazz) {
      return instance.getElementID(clazz);
   }

   public static String getIncursionDataStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class IncursionDataRegistryElement extends ClassIDDataContainer<IncursionData> {
      public IncursionDataRegistryElement(Class<? extends IncursionData> incursionDataClass) throws NoSuchMethodException {
         super(incursionDataClass);
      }
   }
}
