package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.Level;
import necesse.level.maps.layers.LevelLayer;
import necesse.level.maps.layers.WeatherLevelLayer;

public class LevelLayerRegistry extends ClassedGameRegistry<LevelLayer, LevelLayerRegistry.LayerRegistryElement> {
   public static int RAINING_LAYER;
   public static final LevelLayerRegistry instance = new LevelLayerRegistry();

   public LevelLayerRegistry() {
      super("LevelLayer", 32767);
   }

   @Override
   public void registerCore() {
      RAINING_LAYER = registerLayer("weather", WeatherLevelLayer.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerLayer(String stringID, Class<? extends LevelLayer> layerClass) {
      try {
         if (LoadedMod.isRunningModClientSide()) {
            throw new IllegalStateException("Client/server only mods cannot register level layers");
         } else {
            return instance.register(stringID, new LevelLayerRegistry.LayerRegistryElement(layerClass));
         }
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with level parameter");
      }
   }

   public static int replaceLayer(String stringID, Class<? extends LevelLayer> layerClass) {
      try {
         return instance.replace(stringID, new LevelLayerRegistry.LayerRegistryElement(layerClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with level parameter");
      }
   }

   public static int getLayerID(Class<? extends LevelLayer> layerClass) {
      try {
         return instance.getElementIDRaw(layerClass);
      } catch (NoSuchElementException e) {
         System.err.println("Could not find LevelLayer id for " + layerClass.getSimpleName());
         return -1;
      }
   }

   public static int getLayerID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static LevelLayer[] getNewLayersArray(Level level) {
      LevelLayer[] out = new LevelLayer[instance.size()];

      for (int i = 0; i < out.length; i++) {
         LevelLayerRegistry.LayerRegistryElement e = instance.getElement(i);

         try {
            LevelLayer levelLayer = e.newInstance(level);
            levelLayer.idData.setData(e.getIDData().getID(), e.getIDData().getStringID());
            out[i] = levelLayer;
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            throw new RuntimeException("Could not create new LevelLayer object for " + e.layerClass.getSimpleName(), ex);
         }
      }

      return out;
   }

   protected static class LayerRegistryElement extends ClassIDDataContainer<LevelLayer> {
      private final Class<? extends LevelLayer> layerClass;

      public LayerRegistryElement(Class<? extends LevelLayer> layerClass) throws NoSuchMethodException {
         super(layerClass, Level.class);
         this.layerClass = layerClass;
      }
   }
}
