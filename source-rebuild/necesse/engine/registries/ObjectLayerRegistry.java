package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.regionSystem.layers.ObjectLayerAbstract;
import necesse.level.maps.regionSystem.layers.ObjectRegionLayer;
import necesse.level.maps.regionSystem.layers.objectLayer.ArrayObjectLayer;

public class ObjectLayerRegistry extends ClassedGameRegistry<ObjectLayerAbstract, ObjectLayerRegistry.LayerRegistryElement> {
   public static int BASE_LAYER;
   public static int TILE_LAYER;
   public static int WALL_DECOR;
   public static int FENCE_AND_TABLE_DECOR;
   public static final ObjectLayerRegistry instance = new ObjectLayerRegistry();

   public ObjectLayerRegistry() {
      super("ObjectLayer", 250);
   }

   @Override
   public void registerCore() {
      BASE_LAYER = registerLayer("base", ArrayObjectLayer.class);
      TILE_LAYER = registerLayer("tile", ArrayObjectLayer.class);
      WALL_DECOR = registerLayer("wallDecor", ArrayObjectLayer.class);
      FENCE_AND_TABLE_DECOR = registerLayer("tableDecor", ArrayObjectLayer.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerLayer(String stringID, Class<? extends ObjectLayerAbstract> layerClass) {
      try {
         if (LoadedMod.isRunningModClientSide()) {
            throw new IllegalStateException("Client/server only mods cannot register object layers");
         } else {
            return instance.register(stringID, new ObjectLayerRegistry.LayerRegistryElement(layerClass));
         }
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with ObjectLevelLayer parameter");
      }
   }

   public static int replaceLayer(String stringID, Class<? extends ObjectLayerAbstract> layerClass) {
      try {
         return instance.replace(stringID, new ObjectLayerRegistry.LayerRegistryElement(layerClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with ObjectLevelLayer parameter");
      }
   }

   public static int getLayerID(Class<? extends ObjectLayerAbstract> layerClass) {
      try {
         return instance.getElementIDRaw(layerClass);
      } catch (NoSuchElementException e) {
         System.err.println("Could not find ObjectLayer id for " + layerClass.getSimpleName());
         return -1;
      }
   }

   public static int getLayerID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getLayerStringID(int layerID) {
      return instance.getElementStringID(layerID);
   }

   public static int getTotalLayers() {
      return instance.size();
   }

   public static Iterable<Integer> getLayerIDs() {
      return () -> IntStream.range(0, getTotalLayers()).iterator();
   }

   public static ObjectLayerAbstract[] getNewLayersArray(ObjectRegionLayer layer) {
      ObjectLayerAbstract[] out = new ObjectLayerAbstract[instance.size()];

      for (int i = 0; i < out.length; i++) {
         ObjectLayerRegistry.LayerRegistryElement e = instance.getElement(i);

         try {
            ObjectLayerAbstract levelLayer = e.newInstance(layer);
            levelLayer.idData.setData(e.getIDData().getID(), e.getIDData().getStringID());
            out[i] = levelLayer;
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            throw new RuntimeException("Could not create new ObjectLayer object for " + e.layerClass.getSimpleName(), ex);
         }
      }

      return out;
   }

   protected static class LayerRegistryElement extends ClassIDDataContainer<ObjectLayerAbstract> {
      private final Class<? extends ObjectLayerAbstract> layerClass;

      public LayerRegistryElement(Class<? extends ObjectLayerAbstract> layerClass) throws NoSuchMethodException {
         super(layerClass, ObjectRegionLayer.class);
         this.layerClass = layerClass;
      }
   }
}
