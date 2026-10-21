package necesse.engine.world.worldData;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import necesse.engine.network.server.Server;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.save.levelData.MobSave;
import necesse.engine.util.TeleportResult;
import necesse.entity.levelEvent.SmokePuffLevelEvent;
import necesse.entity.levelEvent.TeleportEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlersWorldData extends WorldData {
   private final HashMap<Integer, SettlersWorldData.SettlerData> settlers = new HashMap<>();
   private HashSet<Integer> deadRelationshipSettlersUniqueIDs = new HashSet<>();

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (!this.settlers.isEmpty()) {
         SaveData settlersSave = new SaveData("settlers");

         for (Entry<Integer, SettlersWorldData.SettlerData> entry : this.settlers.entrySet()) {
            int uniqueID = entry.getKey();
            SettlersWorldData.SettlerData settlerData = entry.getValue();
            SaveData settlerSave = new SaveData("settler");
            settlerSave.addInt("uniqueID", uniqueID);
            settlerSave.addLong("returnTime", settlerData.returnTime);
            settlerSave.addSaveData(MobSave.getSave("mob", settlerData.mob));
            settlersSave.addSaveData(settlerSave);
         }

         save.addSaveData(settlersSave);
      }

      if (!this.deadRelationshipSettlersUniqueIDs.isEmpty()) {
         save.addIntCollection("deadRelationshipSettlersUniqueIDs", this.deadRelationshipSettlersUniqueIDs);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.settlers.clear();
      LoadData settlersSave = save.getFirstLoadDataByName("settlers");
      if (settlersSave != null) {
         for (LoadData settlerSave : settlersSave.getLoadDataByName("settler")) {
            try {
               int uniqueID = settlerSave.getInt("uniqueID");
               long returnTime = settlerSave.getLong("returnTime");
               Mob mob = MobSave.loadSave(settlerSave.getFirstLoadDataByName("mob"), null);
               if (mob instanceof HumanMob) {
                  this.settlers.put(uniqueID, new SettlersWorldData.SettlerData(returnTime, (HumanMob)mob));
               }
            } catch (Exception e) {
               System.err.println("Error loading world settler:");
               e.printStackTrace();
            }
         }
      }

      this.deadRelationshipSettlersUniqueIDs.clear();
      this.deadRelationshipSettlersUniqueIDs = new HashSet<>(save.getIntCollection("deadRelationshipSettlersUniqueIDs", new ArrayList<>(), false));
   }

   @Override
   public void tick() {
      super.tick();
   }

   public HumanMob getSettler(int mobUniqueID) {
      SettlersWorldData.SettlerData data = this.settlers.get(mobUniqueID);
      return data != null ? data.mob : null;
   }

   public boolean isRelationshipSettlerDead(int mobUniqueID) {
      return this.deadRelationshipSettlersUniqueIDs.contains(mobUniqueID);
   }

   public boolean clearDeadRelationshipSettler(int mobUniqueID) {
      return this.deadRelationshipSettlersUniqueIDs.remove(mobUniqueID);
   }

   public void onSettlerRelationshipDeath(int mobUniqueID) {
      this.deadRelationshipSettlersUniqueIDs.add(mobUniqueID);
   }

   public boolean exists(int mobUniqueID) {
      return this.settlers.containsKey(mobUniqueID);
   }

   public void remove(int mobUniqueID) {
      this.settlers.remove(mobUniqueID);
   }

   public boolean returnIfShould(int mobUniqueID, ServerSettlementData settlement) {
      SettlersWorldData.SettlerData data = this.settlers.get(mobUniqueID);
      if (data != null) {
         if (data.returnTime < this.worldEntity.getTime()) {
            this.returnToSettlement(mobUniqueID, data, settlement, false);
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean returnToSettlement(HumanMob mob, boolean useTeleportEvent) {
      SettlersWorldData.SettlerData data = this.settlers.get(mob.getUniqueID());
      if (data != null) {
         int settlementUniqueID = data.mob.getSettlementUniqueID();
         if (settlementUniqueID != 0) {
            ServerSettlementData settlement = SettlementsWorldData.getSettlementsData(this.worldEntity).getOrLoadServerData(settlementUniqueID);
            if (settlement != null) {
               this.returnToSettlement(mob.getUniqueID(), data, settlement, useTeleportEvent);
               return true;
            }
         }
      }

      return false;
   }

   public static Point getReturnPos(Mob mob, ServerSettlementData settlement) {
      return PortalObjectEntity.getTeleportDestinationAroundObject(
         settlement.getLevel(), mob, settlement.networkData.getTileX(), settlement.networkData.getTileY(), true
      );
   }

   private void returnToSettlement(int mobUniqueID, SettlersWorldData.SettlerData data, ServerSettlementData settlement, boolean useTeleportEvent) {
      Point returnPos = getReturnPos(data.mob, settlement);
      Level mobLevel = data.mob.getLevel();
      if (mobLevel != null && mobLevel.isDisposed()) {
         mobLevel = null;
      }

      if (mobLevel != null) {
         mobLevel.regionManager.ensureTileIsLoaded(data.mob.getTileX(), data.mob.getTileY());
         Mob newMob = mobLevel.entityManager.mobs.get(mobUniqueID, false);
         if (newMob instanceof HumanMob) {
            data.mob = (HumanMob)newMob;
            mobLevel = data.mob.getLevel();
         }
      }

      if (mobLevel != null && mobLevel.entityManager.mobs.get(mobUniqueID, false) == data.mob) {
         data.mob.cancelJob();
         data.mob.ai.blackboard.submitEvent("resetPathTime", new AIEvent());
         if (useTeleportEvent) {
            TeleportEvent teleportEvent = new TeleportEvent(
               data.mob,
               0,
               settlement.getLevel().getIdentifier(),
               0.0F,
               null,
               newLevel -> new TeleportResult(true, settlement.getLevel().getIdentifier(), returnPos.x, returnPos.y)
            );
            mobLevel.entityManager.events.add(teleportEvent);
         } else {
            mobLevel.entityManager.events.add(new SmokePuffLevelEvent(data.mob.x, data.mob.y, 64, new Color(50, 50, 50)));
            if (mobLevel.isSamePlace(settlement.getLevel())) {
               data.mob.setPos(returnPos.x, returnPos.y, true);
            } else {
               mobLevel.entityManager.events.add(new SmokePuffLevelEvent(data.mob.x, data.mob.y, 64, new Color(50, 50, 50)));
               mobLevel.entityManager.changeMobLevel(data.mob, settlement.getLevel(), returnPos.x, returnPos.y, true);
            }
         }
      } else {
         if (data.mob.removed()) {
            data.mob.restore();
         }

         float var10002 = returnPos.x;
         settlement.getLevel().entityManager.addMob(data.mob, var10002, returnPos.y);
      }

      settlement.onReturned(mobUniqueID);
      data.mob.clearCommandsOrders(null);
      this.settlers.remove(mobUniqueID);
   }

   public void refreshWorldSettler(HumanMob mob, boolean refreshTime) {
      SettlersWorldData.SettlerData data = this.settlers.computeIfAbsent(mob.getUniqueID(), key -> new SettlersWorldData.SettlerData(0L, mob));
      data.mob = mob;
      if (refreshTime) {
         data.returnTime = this.worldEntity.getTime() + 5000L;
      }
   }

   public static SettlersWorldData getSettlersData(Server server) {
      WorldData settlers = server.world.worldEntity.getWorldData("settlers");
      if (settlers instanceof SettlersWorldData) {
         return (SettlersWorldData)settlers;
      }

      SettlersWorldData data = new SettlersWorldData();
      server.world.worldEntity.addWorldData("settlers", data);
      return data;
   }

   private static class SettlerData {
      public long returnTime;
      public HumanMob mob;

      public SettlerData(long returnTime, HumanMob mob) {
         this.returnTime = returnTime;
         this.mob = mob;
      }
   }
}
