package necesse.level.maps.levelData.settlementData.zones;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.registries.ClassIDDataContainer;
import necesse.engine.registries.ClassedGameRegistry;

public class SettlementWorkZoneRegistry extends ClassedGameRegistry<SettlementWorkZone, SettlementWorkZoneRegistry.ZoneRegistryElement> {
   public static int FORESTRY_ID;
   public static int HUSBANDRY_ID;
   public static int FERTILIZE_ID;
   public static final SettlementWorkZoneRegistry instance = new SettlementWorkZoneRegistry();

   public SettlementWorkZoneRegistry() {
      super("SettlementZone", 32767);
   }

   @Override
   public void registerCore() {
      FORESTRY_ID = registerZone("forestry", SettlementForestryZone.class);
      HUSBANDRY_ID = registerZone("husbandry", SettlementHusbandryZone.class);
      FERTILIZE_ID = registerZone("fertilize", SettlementFertilizeZone.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerZone(String stringID, Class<? extends SettlementWorkZone> zoneClass) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register work zones");
      }

      try {
         return instance.register(stringID, new SettlementWorkZoneRegistry.ZoneRegistryElement(zoneClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(
            zoneClass.getSimpleName()
               + " must have 3 different constructors: One with no parameters, one with LoadData parameter and one with PacketReader parameter."
         );
      }
   }

   public static int replaceZone(String stringID, Class<? extends SettlementWorkZone> zoneClass) {
      try {
         return instance.replace(stringID, new SettlementWorkZoneRegistry.ZoneRegistryElement(zoneClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(
            zoneClass.getSimpleName()
               + " must have 3 different constructors: One with no parameters, one with LoadData parameter and one with PacketReader parameter."
         );
      }
   }

   public static int getZoneID(Class<? extends SettlementWorkZone> clazz) {
      return instance.getElementID(clazz);
   }

   public static int getZoneID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static SettlementWorkZone getNewZone(int zoneID) {
      SettlementWorkZoneRegistry.ZoneRegistryElement element = instance.getElement(zoneID);

      try {
         SettlementWorkZone zone = element.newInstance();
         zone.idData.setData(element.getID(), element.getStringID());
         return zone;
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }

   public static <T extends SettlementWorkZone> T getNewZone(Class<T> zoneClass) {
      return zoneClass.cast(getNewZone(getZoneID(zoneClass)));
   }

   protected static class ZoneRegistryElement extends ClassIDDataContainer<SettlementWorkZone> {
      public ZoneRegistryElement(Class<? extends SettlementWorkZone> zoneClass) throws NoSuchMethodException {
         super(zoneClass);
      }
   }
}
