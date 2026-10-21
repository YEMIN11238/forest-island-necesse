package necesse.engine.registries;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobType;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobTypeHandlerGenerator;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.jobs.ChatWithOtherLevelJob;
import necesse.level.maps.levelData.jobs.ConsumeFoodLevelJob;
import necesse.level.maps.levelData.jobs.EnjoyArtTileLevelJob;
import necesse.level.maps.levelData.jobs.FertilizeLevelJob;
import necesse.level.maps.levelData.jobs.FillApiaryFrameLevelJob;
import necesse.level.maps.levelData.jobs.FindCollectorItemsLevelJob;
import necesse.level.maps.levelData.jobs.FishingTileLevelJob;
import necesse.level.maps.levelData.jobs.ForestryLevelJob;
import necesse.level.maps.levelData.jobs.GoJoggingTileLevelJob;
import necesse.level.maps.levelData.jobs.HarvestApiaryLevelJob;
import necesse.level.maps.levelData.jobs.HarvestCropLevelJob;
import necesse.level.maps.levelData.jobs.HarvestFruitLevelJob;
import necesse.level.maps.levelData.jobs.HasStorageLevelJob;
import necesse.level.maps.levelData.jobs.HaulFromLevelJob;
import necesse.level.maps.levelData.jobs.HuntMobLevelJob;
import necesse.level.maps.levelData.jobs.ListenToMusicTileLevelJob;
import necesse.level.maps.levelData.jobs.ManageEquipmentLevelJob;
import necesse.level.maps.levelData.jobs.MilkHusbandryMobLevelJob;
import necesse.level.maps.levelData.jobs.PlantCropLevelJob;
import necesse.level.maps.levelData.jobs.PlantSaplingLevelJob;
import necesse.level.maps.levelData.jobs.ShearHusbandryMobLevelJob;
import necesse.level.maps.levelData.jobs.ShippingChestLevelJob;
import necesse.level.maps.levelData.jobs.SitDownTileLevelJob;
import necesse.level.maps.levelData.jobs.SlaughterHusbandryMobLevelJob;
import necesse.level.maps.levelData.jobs.StartExpeditionLevelJob;
import necesse.level.maps.levelData.jobs.StorePickupItemLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.levelData.jobs.UseWorkstationLevelJob;
import necesse.level.maps.levelData.jobs.WaterGazingTileLevelJob;

public class LevelJobRegistry extends GameRegistry<LevelJobRegistry.RegistryItem<?>> {
   public static final LevelJobRegistry instance = new LevelJobRegistry();
   public static int waterGazingID;
   public static int enjoyArtID;
   public static int goJoggingID;
   public static int sitDownID;
   public static int listenToMusicID;
   public static int chatWithOtherID;
   public static int consumeFoodID;
   public static int equipArmorID;
   public static int findCollectorItemID;
   public static int hasStorageID;
   public static int startExpeditionID;
   public static int startTradingMissionID;
   public static int haulFromID;
   public static int storePickupItemID;
   public static int useWorkstationID;
   public static int forestryID;
   public static int plantSaplingID;
   public static int fertilizeID;
   public static int slaughterHusbandryMobID;
   public static int milkHusbandryMobID;
   public static int shearHusbandryMobID;
   public static int harvestApiaryID;
   public static int fillApiaryFrameID;
   public static int harvestCropID;
   public static int plantCropID;
   public static int harvestFruitID;
   public static int fishingPositionID;
   public static int huntMobID;
   private final HashMap<Class<? extends AbstractLevelJob>, Integer> classToIDMap = new HashMap<>();

   private LevelJobRegistry() {
      super("LevelJob", 32762);
   }

   @Override
   public void registerCore() {
      waterGazingID = registerJob("watergazing", WaterGazingTileLevelJob.class, WaterGazingTileLevelJob::handler, "recreation");
      enjoyArtID = registerJob("enjoyart", EnjoyArtTileLevelJob.class, EnjoyArtTileLevelJob::handler, "recreation");
      goJoggingID = registerJob("gojogging", GoJoggingTileLevelJob.class, GoJoggingTileLevelJob::handler, "recreation");
      sitDownID = registerJob("sitdown", SitDownTileLevelJob.class, SitDownTileLevelJob::handler, "recreation");
      listenToMusicID = registerJob("listentomusic", ListenToMusicTileLevelJob.class, ListenToMusicTileLevelJob::handler, "recreation");
      chatWithOtherID = registerJob("chatwithother", ChatWithOtherLevelJob.class, ChatWithOtherLevelJob::handler, "recreation");
      consumeFoodID = registerJob("consumefood", ConsumeFoodLevelJob.class, ConsumeFoodLevelJob::handler, "needs");
      equipArmorID = registerJob("manageequipment", ManageEquipmentLevelJob.class, ManageEquipmentLevelJob::handler, "needs");
      findCollectorItemID = registerJob("findcollectoritem", FindCollectorItemsLevelJob.class, FindCollectorItemsLevelJob::handler, "needs");
      hasStorageID = registerJob("hasstorage", HasStorageLevelJob.class, HasStorageLevelJob::handler, "needs", 100);
      startExpeditionID = registerJob("startexpedition", StartExpeditionLevelJob.class, StartExpeditionLevelJob::handler, "expeditions");
      startTradingMissionID = registerJob("starttrading", ShippingChestLevelJob.class, ShippingChestLevelJob::handler, "tradingmission");
      haulFromID = registerJob("haulfrom", HaulFromLevelJob.class, HaulFromLevelJob::handler, "hauling");
      storePickupItemID = registerJob("storepickupitem", StorePickupItemLevelJob.class, StorePickupItemLevelJob::handler, "hauling", 100);
      useWorkstationID = registerJob("useworkstation", UseWorkstationLevelJob.class, UseWorkstationLevelJob::handler, "crafting", 0);
      forestryID = registerJob("forestry", ForestryLevelJob.class, ForestryLevelJob::handler, "forestry");
      plantSaplingID = registerJob("plantsapling", PlantSaplingLevelJob.class, PlantSaplingLevelJob::handler, "forestry", 100);
      fertilizeID = registerJob("fertilize", FertilizeLevelJob.class, FertilizeLevelJob::handler, "fertilize");
      slaughterHusbandryMobID = registerJob("slaughterhusbandry", SlaughterHusbandryMobLevelJob.class, SlaughterHusbandryMobLevelJob::handler, "husbandry");
      milkHusbandryMobID = registerJob("milkhusbandry", MilkHusbandryMobLevelJob.class, MilkHusbandryMobLevelJob::handler, "husbandry");
      shearHusbandryMobID = registerJob("shearhusbandry", ShearHusbandryMobLevelJob.class, ShearHusbandryMobLevelJob::handler, "husbandry");
      harvestApiaryID = registerJob("harvestapiary", HarvestApiaryLevelJob.class, HarvestApiaryLevelJob::handler, "husbandry");
      fillApiaryFrameID = registerJob("fillapiaryframe", FillApiaryFrameLevelJob.class, FillApiaryFrameLevelJob::handler, "husbandry");
      fishingPositionID = registerJob("fishingposition", FishingTileLevelJob.class, FishingTileLevelJob::handler, "fishing");
      harvestFruitID = registerJob("harvestfruit", HarvestFruitLevelJob.class, HarvestFruitLevelJob::handler, "farming");
      harvestCropID = registerJob("harvestcrop", HarvestCropLevelJob.class, HarvestCropLevelJob::handler, "farming");
      plantCropID = registerJob("plantcrop", PlantCropLevelJob.class, PlantCropLevelJob::handler, "farming", 100);
      huntMobID = registerJob("huntmob", HuntMobLevelJob.class, HuntMobLevelJob::handler, "hunting");
   }

   protected void onRegister(LevelJobRegistry.RegistryItem<?> object, int id, String stringID, boolean isReplace) {
      this.classToIDMap.put((Class<? extends AbstractLevelJob>)object.aClass, id);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static <T extends AbstractLevelJob> int registerJob(
      String stringID, Class<T> jobClass, JobTypeHandlerGenerator<T> handlerGenerator, String jobTypeStringID
   ) {
      return registerJob(stringID, jobClass, handlerGenerator, jobTypeStringID, 0);
   }

   public static <T extends AbstractLevelJob> int registerJob(
      String stringID, Class<T> jobClass, JobTypeHandlerGenerator<T> handlerGenerator, String jobTypeStringID, int sameTypePriority
   ) {
      try {
         return instance.register(stringID, new LevelJobRegistry.RegistryItem<>(jobClass, handlerGenerator, jobTypeStringID, sameTypePriority));
      } catch (NoSuchMethodException e) {
         throw new RuntimeException(instance.objectCallName + " must have a constructor with LoadData parameter when extending TileLevelJob", e);
      }
   }

   public static TileLevelJob loadJob(String stringID, LoadData save) {
      LevelJobRegistry.RegistryItem<?> element = instance.getElement(stringID);
      if (element == null) {
         throw new LoadDataException(instance.objectCallName + " not found with stringID \"" + stringID + "\"");
      }

      try {
         return element.newInstance(save);
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         throw new LoadDataException("Could not load " + instance.objectCallName, e);
      }
   }

   public static String getJobStringID(int jobID) {
      return instance.getElementStringID(jobID);
   }

   public static int getJobID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getJobID(Class<? extends AbstractLevelJob> clazz) {
      Integer id = instance.classToIDMap.get(clazz);
      return id == null ? -1 : id;
   }

   public static int getJobTypeID(Class<? extends AbstractLevelJob> clazz) {
      int id = getJobID(clazz);
      return id != -1 ? instance.getElement(id).jobTypeID : -1;
   }

   public static void addHandlers(EntityJobWorker worker, JobTypeHandler handler) {
      for (LevelJobRegistry.RegistryItem<?> element : instance.getElements()) {
         element.handlerGenerator.registerHandler(worker, handler);
      }
   }

   public static JobType applyIDData(Class<? extends AbstractLevelJob> clazz, IDData objectIDData) {
      int id = getJobID(clazz);
      if (id == -1) {
         throw new IllegalStateException("Cannot construct unregistered " + instance.objectCallName + " class " + clazz.getSimpleName());
      }

      LevelJobRegistry.RegistryItem<?> element = instance.getElement(id);
      IDData registryIDData = element.data;
      objectIDData.setData(registryIDData.getID(), registryIDData.getStringID());
      return JobTypeRegistry.getJobType(element.jobTypeID);
   }

   protected static class RegistryItem<T extends AbstractLevelJob> implements IDDataContainer {
      protected final IDData data;
      public final Class<T> aClass;
      public final Constructor<T> tileJobConstructor;
      public JobTypeHandlerGenerator<T> handlerGenerator;
      public final int jobTypeID;
      public final int sameTypePriority;

      public RegistryItem(Class<T> jobClass, JobTypeHandlerGenerator<T> handlerGenerator, String jobTypeStringID, int sameTypePriority) throws NoSuchMethodException {
         this.aClass = jobClass;
         if (TileLevelJob.class.isAssignableFrom(jobClass)) {
            this.tileJobConstructor = this.aClass.getConstructor(LoadData.class);
         } else {
            this.tileJobConstructor = null;
         }

         this.handlerGenerator = handlerGenerator;
         this.data = new IDData();
         this.jobTypeID = JobTypeRegistry.getJobTypeID(jobTypeStringID);
         if (this.jobTypeID == -1) {
            throw new IllegalArgumentException("Must first register job type with stringID \"" + jobTypeStringID + "\"");
         }

         this.sameTypePriority = sameTypePriority;
      }

      @Override
      public IDData getIDData() {
         return this.data;
      }

      public TileLevelJob newInstance(LoadData save) throws IllegalAccessException, InvocationTargetException, InstantiationException {
         if (this.tileJobConstructor == null) {
            throw new IllegalStateException(
               "Registered job class "
                  + this.aClass.getName()
                  + ", stringID \""
                  + this.data.getStringID()
                  + "\" does not extend TileLevelJob, so it cannot be loaded with LoadData"
            );
         } else {
            return (TileLevelJob)this.tileJobConstructor.newInstance(save);
         }
      }
   }
}
