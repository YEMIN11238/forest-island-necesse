package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageEquipmentTypeIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageFoodQualityIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageGlobalIngredientIDIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageIndex;
import necesse.level.maps.levelData.settlementData.storage.SettlementStorageItemIDIndex;

public class SettlementStorageIndexRegistry extends ClassedGameRegistry<SettlementStorageIndex, SettlementStorageIndexRegistry.IndexRegistryElement> {
   public static int ITEM_IDS;
   public static int GLOBAL_INGREDIENT_IDS;
   public static int FOOD_QUALITY;
   public static int ARMOR_TYPE;
   public static final SettlementStorageIndexRegistry instance = new SettlementStorageIndexRegistry();

   public SettlementStorageIndexRegistry() {
      super("SettlementStorageIndex", 32767);
   }

   @Override
   public void registerCore() {
      ITEM_IDS = registerIndex("itemids", SettlementStorageItemIDIndex.class);
      GLOBAL_INGREDIENT_IDS = registerIndex("globalingredientids", SettlementStorageGlobalIngredientIDIndex.class);
      FOOD_QUALITY = registerIndex("foodquality", SettlementStorageFoodQualityIndex.class);
      ARMOR_TYPE = registerIndex("armortype", SettlementStorageEquipmentTypeIndex.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerIndex(String stringID, Class<? extends SettlementStorageIndex> layerClass) {
      try {
         return instance.register(stringID, new SettlementStorageIndexRegistry.IndexRegistryElement(layerClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with level parameter");
      }
   }

   public static int replaceIndex(String stringID, Class<? extends SettlementStorageIndex> layerClass) {
      try {
         return instance.replace(stringID, new SettlementStorageIndexRegistry.IndexRegistryElement(layerClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with level parameter");
      }
   }

   public static int getIndexID(Class<? extends SettlementStorageIndex> clazz) {
      return instance.getElementID(clazz);
   }

   public static int getIndexID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static SettlementStorageIndex[] getNewIndexesArray(Level level) {
      SettlementStorageIndex[] out = new SettlementStorageIndex[instance.size()];

      for (int i = 0; i < out.length; i++) {
         SettlementStorageIndexRegistry.IndexRegistryElement e = instance.getElement(i);

         try {
            SettlementStorageIndex index = e.newInstance(level);
            index.idData.setData(e.getIDData().getID(), e.getIDData().getStringID());
            out[i] = index;
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            throw new RuntimeException("Could not create new " + instance.objectCallName + " object for " + e.indexClass.getSimpleName(), ex);
         }
      }

      return out;
   }

   protected static class IndexRegistryElement extends ClassIDDataContainer<SettlementStorageIndex> {
      private Class<? extends SettlementStorageIndex> indexClass;

      public IndexRegistryElement(Class<? extends SettlementStorageIndex> indexClass) throws NoSuchMethodException {
         super(indexClass, Level.class);
         this.indexClass = indexClass;
      }
   }
}
