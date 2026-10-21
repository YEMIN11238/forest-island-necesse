package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.pickup.AscendedStarPickupEntity;
import necesse.entity.pickup.IncursionEmpowermentPickupEntity;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.pickup.PickupEntity;
import necesse.entity.pickup.QuestItemPickupEntity;
import necesse.entity.pickup.StarBarrierPickupEntity;
import necesse.entity.pickup.VampiricHeartPickupEntity;

public class PickupRegistry extends ClassedGameRegistry<PickupEntity, PickupRegistry.PickupRegistryElement> {
   public static final PickupRegistry instance = new PickupRegistry();

   private PickupRegistry() {
      super("PickupEntity", 250);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "pickups"));
      registerPickup("item", ItemPickupEntity.class);
      registerPickup("questitem", QuestItemPickupEntity.class);
      registerPickup("starbarrier", StarBarrierPickupEntity.class);
      registerPickup("ascendedstar", AscendedStarPickupEntity.class);
      registerPickup("vampiricheart", VampiricHeartPickupEntity.class);
      registerPickup("incursionempowerment", IncursionEmpowermentPickupEntity.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerPickup(String stringID, Class<? extends PickupEntity> pickupClass) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register pickups");
      }

      try {
         return instance.register(stringID, new PickupRegistry.PickupRegistryElement(pickupClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register PickupEntity " + pickupClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static PickupEntity getPickup(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static PickupEntity getPickup(String stringID) {
      return getPickup(getPickupID(stringID));
   }

   public static int getPickupID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getPickupID(Class<? extends PickupEntity> clazz) {
      return instance.getElementID(clazz);
   }

   public static String getPickupStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class PickupRegistryElement extends ClassIDDataContainer<PickupEntity> {
      public PickupRegistryElement(Class<? extends PickupEntity> pickupClass) throws NoSuchMethodException {
         super(pickupClass);
      }
   }
}
