package necesse.level.maps.levelData;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import necesse.engine.GameLog;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.VersionMigration;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PointHashMap;
import necesse.engine.util.PointHashSet;
import necesse.entity.ObjectDamageResult;
import necesse.entity.TileDamageResult;
import necesse.entity.manager.ObjectDamagedListenerEntityComponent;
import necesse.entity.manager.RegionLoadedListenerEntityComponent;
import necesse.entity.manager.TileDamagedListenerEntityComponent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanAngerTargetAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;
import necesse.level.maps.regionSystem.Region;

public class OneWorldNPCVillageData
   extends SingleRegionBasedLevelData<OneWorldNPCVillageData.NPCVillageRegionData>
   implements RegionLevelDataComponent,
   RegionLoadedListenerEntityComponent,
   TileDamagedListenerEntityComponent,
   ObjectDamagedListenerEntityComponent {
   public static int MIN_VILLAGER_RESPAWN_MINUTES = 10;
   public static int MAX_VILLAGER_RESPAWN_MINUTES = 40;
   public static int MIN_PLAYER_PREVENTED_DISTANCE = 1440;
   protected long nextWorldTimeVillagerSpawn;

   protected static long getNextSpawnWaitTime() {
      return GameRandom.globalRandom.getIntBetween(MIN_VILLAGER_RESPAWN_MINUTES, MAX_VILLAGER_RESPAWN_MINUTES) * 60L * 1000L;
   }

   public static OneWorldNPCVillageData getVillageData(Level level, boolean createNewIfNull) {
      LevelData data = level.getLevelData("oneworldnpcvillagedata");
      if (data instanceof OneWorldNPCVillageData) {
         return (OneWorldNPCVillageData)data;
      } else if (createNewIfNull) {
         OneWorldNPCVillageData newData = new OneWorldNPCVillageData();
         level.addLevelData("oneworldnpcvillagedata", newData);
         return newData;
      } else {
         return null;
      }
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("nextWorldTimeVillagerSpawn", this.nextWorldTimeVillagerSpawn);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.nextWorldTimeVillagerSpawn = save.getLong("nextWorldTimeVillagerSpawn", this.nextWorldTimeVillagerSpawn, false);
   }

   public void addRegionSaveData(SaveData save, OneWorldNPCVillageData.NPCVillageRegionData data) {
      if (!data.villageTiles.isEmpty()) {
         save.addPointHashSet("villageTiles", data.villageTiles);
      }

      SaveData villagersSave = new SaveData("VILLAGERS");

      for (OneWorldNPCVillageData.NPCVillagerData villager : data.villagers.values()) {
         villagersSave.addSaveData(villager.getSaveData(""));
      }

      if (!villagersSave.isEmpty()) {
         save.addSaveData(villagersSave);
      }

      if (this.nextWorldTimeVillagerSpawn != 0L) {
         save.addLong("nextWorldTimeVillagerSpawn", this.nextWorldTimeVillagerSpawn);
      }
   }

   public OneWorldNPCVillageData.NPCVillageRegionData loadRegionData(Region region, LoadData save) {
      OneWorldNPCVillageData.NPCVillageRegionData data = new OneWorldNPCVillageData.NPCVillageRegionData(region.regionX, region.regionY);

      try {
         data.villageTiles = save.getPointHashSet("villageTiles", data.villageTiles, false);
         LoadData villagersSave = save.getFirstLoadDataByName("VILLAGERS");
         if (villagersSave != null) {
            for (LoadData villagerSave : villagersSave.getLoadData()) {
               try {
                  OneWorldNPCVillageData.NPCVillagerData villager = new OneWorldNPCVillageData.NPCVillagerData(villagerSave);
                  data.villagers.put(villager.tileX, villager.tileY, villager);
               } catch (LoadDataException e) {
                  GameLog.warn.println("Failed to load villager data: " + e.getMessage() + " at " + this.level);
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      data.loadedNextWorldTimeVillagerSpawn = save.getLong("nextWorldTimeVillagerSpawn", 0L, false);
      return data.villagers.isEmpty() && data.villageTiles.isEmpty() ? null : data;
   }

   @Override
   public void onRegionLoaded(Region region) {
      OneWorldNPCVillageData.NPCVillageRegionData data = this.getDataInRegion(region.regionX, region.regionY);
      if (data != null && data.loadedNextWorldTimeVillagerSpawn != 0L && this.level.getWorldTime() >= data.loadedNextWorldTimeVillagerSpawn) {
         this.tickRespawn(data, true);
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.tickRespawn(false);
   }

   public void tickRespawn(boolean forceSpawn) {
      if (this.level.isServer()) {
         long currentTime = this.level.getWorldTime();
         if (this.nextWorldTimeVillagerSpawn == 0L) {
            this.nextWorldTimeVillagerSpawn = currentTime + getNextSpawnWaitTime();
         }

         while (currentTime >= this.nextWorldTimeVillagerSpawn) {
            this.nextWorldTimeVillagerSpawn = this.nextWorldTimeVillagerSpawn + getNextSpawnWaitTime();
            boolean spawnedVillager = false;

            for (OneWorldNPCVillageData.NPCVillageRegionData value : this.getAllData()) {
               spawnedVillager = this.tickRespawn(value, forceSpawn) || spawnedVillager;
            }

            if (!spawnedVillager) {
               this.nextWorldTimeVillagerSpawn = currentTime + getNextSpawnWaitTime();
            }
         }
      }
   }

   public boolean tickRespawn(OneWorldNPCVillageData.NPCVillageRegionData data, boolean forceSpawn) {
      PointHashSet toRemove = new PointHashSet();
      boolean spawnedVillager = false;

      for (OneWorldNPCVillageData.NPCVillagerData villager : data.villagers.values()) {
         if (villager.tickRespawn(this.level, forceSpawn)) {
            spawnedVillager = true;
            toRemove.add(villager.tileX, villager.tileY);
            break;
         }

         if (villager.shouldRemove()) {
            toRemove.add(villager.tileX, villager.tileY);
         }
      }

      for (Point point : toRemove) {
         data.villagers.remove(point.x, point.y);
      }

      return spawnedVillager;
   }

   @Override
   public void onTileDamaged(GameTile tile, int tileX, int tileY, ServerClient client, TileDamageResult result) {
      if (client != null && !result.levelTile.isPlayerPlaced && this.isVillageTile(tileX, tileY)) {
         float damagePercent = (float)result.addedDamage / result.levelTile.tile.tileHealth;
         if (result.destroyed) {
            damagePercent = 1.0F;
         }

         float anger = damagePercent * 2.0F;
         HumanAngerTargetAINode.addNearbyHumansAnger(this.level, tileX, tileY, anger, client.playerMob, m -> !m.isSettler(), false);
      }
   }

   @Override
   public void onObjectDamaged(GameObject object, int objectLayerID, int tileX, int tileY, ServerClient client, ObjectDamageResult result) {
      if (client != null && !result.levelObject.isPlayerPlaced && !result.levelObject.object.attackThrough && this.isVillageTile(tileX, tileY)) {
         float damagePercent = (float)result.addedDamage / result.levelObject.object.objectHealth;
         if (result.destroyed) {
            damagePercent = 1.0F;
         }

         float anger = damagePercent * 2.0F;
         HumanAngerTargetAINode.addNearbyHumansAnger(this.level, tileX, tileY, anger, client.playerMob, m -> !m.isSettler(), false);
      }
   }

   public void addVillageTile(int tileX, int tileY) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      OneWorldNPCVillageData.NPCVillageRegionData data = this.computeDataInRegion(regionX, regionY, (p, value) -> {
         if (value == null) {
            value = new OneWorldNPCVillageData.NPCVillageRegionData(regionX, regionY);
         }

         return value;
      });
      data.villageTiles.add(tileX, tileY);
   }

   public boolean isVillageTile(int tileX, int tileY) {
      int regionX = GameMath.getRegionCoordByTile(tileX);
      int regionY = GameMath.getRegionCoordByTile(tileY);
      OneWorldNPCVillageData.NPCVillageRegionData data = this.getDataInRegion(regionX, regionY);
      return data != null && data.villageTiles.contains(tileX, tileY);
   }

   public void addNPCVillager(int tileX, int tileY, String... mobStringIDs) {
      this.addNPCVillager(new OneWorldNPCVillageData.NPCVillagerData(tileX, tileY, new ArrayList<>(Arrays.asList(mobStringIDs))));
   }

   public void addNPCVillager(OneWorldNPCVillageData.NPCVillagerData villagerData) {
      int regionX = GameMath.getRegionCoordByTile(villagerData.tileX);
      int regionY = GameMath.getRegionCoordByTile(villagerData.tileY);
      OneWorldNPCVillageData.NPCVillageRegionData data = this.computeDataInRegion(regionX, regionY, (p, value) -> {
         if (value == null) {
            value = new OneWorldNPCVillageData.NPCVillageRegionData(regionX, regionY);
         }

         return value;
      });
      data.villagers.put(villagerData.tileX, villagerData.tileY, villagerData);
   }

   public boolean canPlaceSettlementFlagAt(int tileX, int tileY, int flagTier) {
      Rectangle regionRectangle = SettlementBoundsManager.getRegionRectangleFromTier(tileX, tileY, flagTier);

      for (int regionX = regionRectangle.x; regionX < regionRectangle.x + regionRectangle.width; regionX++) {
         for (int regionY = regionRectangle.y; regionY < regionRectangle.y + regionRectangle.height; regionY++) {
            this.level.regionManager.ensureRegionIsLoaded(regionX, regionY);
            OneWorldNPCVillageData.NPCVillageRegionData data = this.getDataInRegion(regionX, regionY);
            if (data != null && !data.villageTiles.isEmpty()) {
               return false;
            }
         }
      }

      return true;
   }

   public static class NPCVillageRegionData extends SingleRegionBasedLevelData.RegionData {
      protected PointHashSet villageTiles = new PointHashSet();
      protected PointHashMap<OneWorldNPCVillageData.NPCVillagerData> villagers = new PointHashMap<>();
      public long loadedNextWorldTimeVillagerSpawn;

      public NPCVillageRegionData(int regionX, int regionY) {
         super(regionX, regionY);
      }
   }

   public static class NPCVillagerData {
      public final int tileX;
      public final int tileY;
      public ArrayList<String> mobStringIDs;

      public NPCVillagerData(int tileX, int tileY, ArrayList<String> mobStringIDs) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.mobStringIDs = mobStringIDs;
      }

      public NPCVillagerData(LoadData save) {
         Point tile = save.getPoint("tile", null);
         if (tile == null) {
            throw new LoadDataException("NPCVillagerData tile is missing");
         }

         this.tileX = tile.x;
         this.tileY = tile.y;
         this.mobStringIDs = new ArrayList<>(save.getSafeStringCollection("mobStringIDs", new ArrayList<>()));
      }

      public SaveData getSaveData(String name) {
         SaveData save = new SaveData(name);
         save.addPoint("tile", new Point(this.tileX, this.tileY));
         save.addSafeStringCollection("mobStringIDs", this.mobStringIDs);
         ListIterator<String> li = this.mobStringIDs.listIterator();

         while (li.hasNext()) {
            String oldStringID = li.next();
            String newStringID = VersionMigration.tryFixStringID(oldStringID, VersionMigration.oldMobStringIDs);
            if (!oldStringID.equals(newStringID)) {
               li.set(newStringID);
            }
         }

         return save;
      }

      public boolean tickRespawn(Level level, boolean forceSpawn) {
         if (!forceSpawn) {
            int levelPosX = this.tileX * 32 + 16;
            int levelPosY = this.tileY * 32 + 16;
            boolean playersFound = level.entityManager
               .players
               .streamArea(levelPosX, levelPosY, OneWorldNPCVillageData.MIN_PLAYER_PREVENTED_DISTANCE)
               .anyMatch(p -> p.getDistance(levelPosX, levelPosY) < OneWorldNPCVillageData.MIN_PLAYER_PREVENTED_DISTANCE);
            if (playersFound) {
               return false;
            }
         }

         while (!this.mobStringIDs.isEmpty()) {
            int nextIndex = GameRandom.globalRandom.nextInt(this.mobStringIDs.size());
            String mobStringID = this.mobStringIDs.get(nextIndex);
            Mob newMob = MobRegistry.getMob(mobStringID, level);
            if (newMob instanceof HumanMob) {
               HumanMob humanMob = (HumanMob)newMob;
               humanMob.setSettlerSeed(GameRandom.globalRandom.nextInt(), true);
               humanMob.setHome(this.tileX, this.tileY);
               humanMob.villagerData = this;
               level.entityManager.addMob(humanMob, this.tileX * 32 + 16, this.tileY * 32 + 16);
               return true;
            }

            this.mobStringIDs.remove(nextIndex);
         }

         return false;
      }

      public boolean shouldRemove() {
         return this.mobStringIDs.isEmpty();
      }
   }
}
