package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import necesse.engine.modLoader.LoadedMod;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.SubRegionDataRegionLayer;
import necesse.level.maps.regionSystem.layers.BiomeBlendingRegionLayer;
import necesse.level.maps.regionSystem.layers.BiomeRegionLayer;
import necesse.level.maps.regionSystem.layers.JobsRegionLayer;
import necesse.level.maps.regionSystem.layers.LiquidDataRegionLayer;
import necesse.level.maps.regionSystem.layers.LogicRegionLayer;
import necesse.level.maps.regionSystem.layers.ObjectRegionLayer;
import necesse.level.maps.regionSystem.layers.RegionLayer;
import necesse.level.maps.regionSystem.layers.SplattingRegionLayer;
import necesse.level.maps.regionSystem.layers.TileRegionLayer;
import necesse.level.maps.regionSystem.layers.TilesProtectedRegionLayer;
import necesse.level.maps.regionSystem.layers.WireDataRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.ClientTickRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.FrameTickRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.LevelJobsSubmitterRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.RegionPacketHandlerRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.SaveDataRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.ServerTickRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.TileTickEffectRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.TileTickRegionLayer;
import necesse.level.maps.regionSystem.layers.interfaces.WireUpdateRegionLayer;
import necesse.level.maps.regionSystem.layers.lighting.LightingRegionLayer;

public class RegionLayerRegistry extends ClassedGameRegistry<RegionLayer, RegionLayerRegistry.LayerRegistryElement> {
   public static int BIOME_LAYER;
   public static int BIOME_BLENDING_LAYER;
   public static int TILE_LAYER;
   public static int SPLATTING_LAYER;
   public static int OBJECT_LAYER;
   public static int LOGIC_LAYER;
   public static int WIRE_LAYER;
   public static int TILE_PROTECTED_LAYER;
   public static int JOBS_LAYER;
   public static int LIQUID_DATA_LAYER;
   public static int SUB_REGION_DATA_LAYER;
   public static int LIGHT_LAYER;
   public static final RegionLayerRegistry instance = new RegionLayerRegistry();
   private static final Class<?>[] interfaces = new Class[]{
      FrameTickRegionLayer.class,
      ClientTickRegionLayer.class,
      ServerTickRegionLayer.class,
      TileTickRegionLayer.class,
      TileTickEffectRegionLayer.class,
      WireUpdateRegionLayer.class,
      LevelJobsSubmitterRegionLayer.class,
      RegionPacketHandlerRegionLayer.class,
      SaveDataRegionLayer.class
   };
   private static final int[] totalInterfaces = new int[interfaces.length];

   public RegionLayerRegistry() {
      super("RegionLayer", 32767);
   }

   @Override
   public void registerCore() {
      BIOME_LAYER = registerLayer("biomes", BiomeRegionLayer.class);
      BIOME_BLENDING_LAYER = registerLayer("biomeBlending", BiomeBlendingRegionLayer.class);
      TILE_LAYER = registerLayer("tiles", TileRegionLayer.class);
      SPLATTING_LAYER = registerLayer("splatting", SplattingRegionLayer.class);
      OBJECT_LAYER = registerLayer("objects", ObjectRegionLayer.class);
      LOGIC_LAYER = registerLayer("logic", LogicRegionLayer.class);
      WIRE_LAYER = registerLayer("wire", WireDataRegionLayer.class);
      TILE_PROTECTED_LAYER = registerLayer("tilesProtected", TilesProtectedRegionLayer.class);
      JOBS_LAYER = registerLayer("jobs", JobsRegionLayer.class);
      LIQUID_DATA_LAYER = registerLayer("liquidData", LiquidDataRegionLayer.class);
      SUB_REGION_DATA_LAYER = registerLayer("subRegionData", SubRegionDataRegionLayer.class);
      LIGHT_LAYER = registerLayer("lightData", LightingRegionLayer.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   protected void onRegister(RegionLayerRegistry.LayerRegistryElement object, int id, String stringID, boolean isReplace) {
      super.onRegister(object, id, stringID, isReplace);

      for (int i = 0; i < interfaces.length; i++) {
         if (object.implementsInterfaces[i]) {
            totalInterfaces[i]++;
         }
      }
   }

   public static int registerLayer(String stringID, Class<? extends RegionLayer> layerClass) {
      try {
         if (LoadedMod.isRunningModClientSide()) {
            throw new IllegalStateException("Client/server only mods cannot register region layers");
         } else {
            return instance.register(stringID, new RegionLayerRegistry.LayerRegistryElement(layerClass));
         }
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with single parameter: Region");
      }
   }

   public static int replaceLayer(String stringID, Class<? extends RegionLayer> layerClass) {
      try {
         return instance.replace(stringID, new RegionLayerRegistry.LayerRegistryElement(layerClass));
      } catch (NoSuchMethodException e) {
         throw new IllegalArgumentException(layerClass.getSimpleName() + " does not have a constructor with single parameter: Region");
      }
   }

   public static int getLayerID(Class<? extends RegionLayer> layerClass) {
      try {
         return instance.getElementIDRaw(layerClass);
      } catch (NoSuchElementException e) {
         System.err.println("Could not find RegionLayer id for " + layerClass.getSimpleName());
         return -1;
      }
   }

   public static int getLayerID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static RegionLayersList getNewLayersArray(Region region) {
      RegionLayer[] allLayers = new RegionLayer[instance.size()];
      int interferface = 0;
      ArrayList<FrameTickRegionLayer> frameTickLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<ClientTickRegionLayer> clientTickLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<ServerTickRegionLayer> serverTickLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<TileTickRegionLayer> tileTickLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<TileTickEffectRegionLayer> tileTickEffectLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<WireUpdateRegionLayer> wireUpdateLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<LevelJobsSubmitterRegionLayer> levelJobsSubmitterLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<RegionPacketHandlerRegionLayer> regionPacketHandlerLayers = new ArrayList<>(totalInterfaces[interferface++]);
      ArrayList<SaveDataRegionLayer> saveDataLayers = new ArrayList<>(totalInterfaces[interferface++]);

      for (int i = 0; i < allLayers.length; i++) {
         RegionLayerRegistry.LayerRegistryElement e = instance.getElement(i);

         try {
            RegionLayer regionLayer = e.newRegionLayer(
               region,
               frameTickLayers,
               clientTickLayers,
               serverTickLayers,
               tileTickLayers,
               tileTickEffectLayers,
               wireUpdateLayers,
               levelJobsSubmitterLayers,
               regionPacketHandlerLayers,
               saveDataLayers
            );
            allLayers[i] = regionLayer;
         } catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
            throw new RuntimeException("Could not create new RegionLayer object for " + e.layerClass.getSimpleName(), ex);
         }
      }

      return new RegionLayersList(
         allLayers,
         frameTickLayers,
         clientTickLayers,
         serverTickLayers,
         tileTickLayers,
         tileTickEffectLayers,
         wireUpdateLayers,
         levelJobsSubmitterLayers,
         regionPacketHandlerLayers,
         saveDataLayers
      );
   }

   protected static class LayerRegistryElement extends ClassIDDataContainer<RegionLayer> {
      private final Class<? extends RegionLayer> layerClass;
      private final boolean[] implementsInterfaces;

      public LayerRegistryElement(Class<? extends RegionLayer> layerClass) throws NoSuchMethodException {
         super(layerClass, Region.class);
         this.layerClass = layerClass;
         this.implementsInterfaces = new boolean[RegionLayerRegistry.interfaces.length];

         for (int i = 0; i < RegionLayerRegistry.interfaces.length; i++) {
            this.implementsInterfaces[i] = RegionLayerRegistry.interfaces[i].isAssignableFrom(layerClass);
         }
      }

      public RegionLayer newRegionLayer(Region region, ArrayList... lists) throws InvocationTargetException, IllegalAccessException, InstantiationException {
         RegionLayer layer = this.newInstance(region);
         layer.idData.setData(this.getID(), this.getStringID());

         for (int i = 0; i < RegionLayerRegistry.interfaces.length; i++) {
            if (this.implementsInterfaces[i]) {
               lists[i].add(RegionLayerRegistry.interfaces[i].cast(layer));
            }
         }

         return layer;
      }
   }
}
