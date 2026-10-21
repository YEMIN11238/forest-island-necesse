package necesse.level.maps.levelData.settlementData;

import java.awt.Point;
import java.util.Objects;
import necesse.engine.GameLog;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.engine.world.worldData.SettlersWorldData;
import necesse.entity.mobs.Mob;
import necesse.inventory.container.settlement.events.SettlementSettlersChangedEvent;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationSeverity;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;

public class LevelSettler {
   public final ServerSettlementData data;
   public final Settler settler;
   public final int mobUniqueID;
   public final int settlerSeed;
   public final ItemCategoriesFilter dietFilter = new ItemCategoriesFilter(ItemCategory.foodQualityMasterCategory, true);
   public boolean preferArmorSets;
   public final ItemCategoriesFilter equipmentFilter = new ItemCategoriesFilter(ItemCategory.equipmentMasterCategory, true);
   public int restrictZoneUniqueID;
   public int notFoundBuffer;
   protected SettlementBed bed;
   protected boolean roomDirty = true;

   public LevelSettler(ServerSettlementData data, Settler settler, int mobUniqueID, int settlerSeed) {
      Objects.requireNonNull(settler);
      this.data = data;
      this.settler = settler;
      this.mobUniqueID = mobUniqueID;
      this.settlerSeed = settlerSeed;
      this.dietFilter.loadFromCopy(data.getNewSettlerDiet());
      this.equipmentFilter.loadFromCopy(data.getNewSettlerEquipmentFilter());
      this.preferArmorSets = data.newSettlerEquipmentPreferArmorSets;
      this.restrictZoneUniqueID = data.getNewSettlerRestrictZoneUniqueID();
   }

   public LevelSettler(ServerSettlementData data, SettlerMob mob) {
      this(data, mob.getSettler(), mob.getMob().getUniqueID(), mob.getSettlerSeed());
   }

   public LevelSettler(ServerSettlementData data, LoadData save, int tileXOffset, int tileYOffset) {
      this.data = data;
      this.settler = SettlerRegistry.getSettler(save.getUnsafeString("stringID"));
      Objects.requireNonNull(this.settler);
      this.mobUniqueID = save.getInt("mobUniqueID", -1);
      this.settlerSeed = save.getInt("seed", GameRandom.globalRandom.nextInt());
      Point bedPos = save.getPoint("bed", null, false);
      if (bedPos != null) {
         bedPos.translate(tileXOffset, tileYOffset);
         SettlementBed loadedBed = data.addOrValidateBed(bedPos.x, bedPos.y);
         if (loadedBed != null && !loadedBed.isLocked && loadedBed.getSettler() == null) {
            this.bed = loadedBed;
            this.bed.settler = this;
            this.bed.isLocked = false;
         }
      }

      LoadData dietFilterSave = save.getFirstLoadDataByName("dietFilter");
      if (dietFilterSave != null) {
         this.dietFilter.applyLoadData(dietFilterSave);
      } else {
         this.dietFilter.loadFromCopy(data.getNewSettlerDiet());
      }

      LoadData equipmentFilterSave = save.getFirstLoadDataByName("equipmentFilter");
      if (equipmentFilterSave != null) {
         this.equipmentFilter.applyLoadData(equipmentFilterSave);
      } else {
         this.equipmentFilter.loadFromCopy(data.getNewSettlerEquipmentFilter());
      }

      this.preferArmorSets = save.getBoolean("preferArmorSets", data.newSettlerEquipmentPreferArmorSets, false);
      this.restrictZoneUniqueID = save.getInt("restrictZoneUniqueID", 0, false);
   }

   public void addSaveData(SaveData save) {
      save.addUnsafeString("stringID", this.settler.getStringID());
      save.addInt("mobUniqueID", this.mobUniqueID);
      save.addInt("seed", this.settlerSeed);
      if (this.bed != null) {
         save.addPoint("bed", new Point(this.bed.tileX, this.bed.tileY));
      }

      if (!this.dietFilter.isEqualsFilter(this.data.getNewSettlerDiet())) {
         SaveData dietFilterSave = new SaveData("dietFilter");
         this.dietFilter.addSaveData(dietFilterSave);
         save.addSaveData(dietFilterSave);
      }

      if (!this.equipmentFilter.isEqualsFilter(this.data.getNewSettlerEquipmentFilter())) {
         SaveData equipmentFilterSave = new SaveData("equipmentFilter");
         this.equipmentFilter.addSaveData(equipmentFilterSave);
         save.addSaveData(equipmentFilterSave);
      }

      save.addBoolean("preferArmorSets", this.preferArmorSets);
      if (this.restrictZoneUniqueID != 0) {
         save.addInt("restrictZoneUniqueID", this.restrictZoneUniqueID);
      }
   }

   public SettlerMob getMob() {
      Mob mob = this.data.getLevel().entityManager.mobs.get(this.mobUniqueID, false);
      if (mob instanceof SettlerMob) {
         return (SettlerMob)mob;
      }

      SettlersWorldData settlersData = SettlersWorldData.getSettlersData(this.data.getLevel().getServer());
      return settlersData.getSettler(this.mobUniqueID);
   }

   public void updateHome() {
      SettlerMob mob = this.getMob();
      if (mob != null) {
         if (mob.isSettlerOnCurrentLevel()) {
            if (this.bed != null) {
               mob.setHome(new Point(this.bed.tileX, this.bed.tileY));
            } else {
               mob.setHome(this.data.getFlagTile());
            }
         } else {
            mob.setHome(null);
         }
      }
   }

   public void serverTick() {
      if (this.roomDirty) {
         try {
            if (this.bed != null) {
               SettlerMob mob = this.getMob();
               if (mob != null) {
                  mob.onRoomStatsCalculated(this.bed, this.bed.getRoom());
               }
            } else {
               SettlerMob mob = this.getMob();
               if (mob != null) {
                  mob.onRoomStatsCalculated(null, null);
               }
            }
         } finally {
            this.roomDirty = false;
         }
      }
   }

   public void settlementTick() {
      SettlerMob mob = this.getMob();
      if (mob != null) {
         mob.settlementTick(this.data, this);
      }
   }

   public void setRestrictZoneUniqueID(int zoneUniqueID) {
      this.restrictZoneUniqueID = zoneUniqueID;
   }

   public int getRestrictZoneUniqueID() {
      if (this.restrictZoneUniqueID != 0 && this.restrictZoneUniqueID != 1 && this.data.getRestrictZone(this.restrictZoneUniqueID) == null) {
         this.restrictZoneUniqueID = 0;
      }

      return this.restrictZoneUniqueID;
   }

   public ZoneTester isTileInSettlementBoundsTester() {
      return this.data.boundsManager.getZoneTester();
   }

   public ZoneTester isTileInRestrictZoneTester() {
      if (this.restrictZoneUniqueID == 0) {
         return null;
      }

      RestrictZone zone = this.data.getRestrictZone(this.restrictZoneUniqueID);
      return zone == null ? null : zone::containsTile;
   }

   public ZoneTester isTileInSettlementBoundsAndRestrictZoneTester() {
      return this.isTileInSettlementBoundsTester().and(this.isTileInRestrictZoneTester());
   }

   public void assignBed(SettlementBed bed) {
      if (this.bed != null) {
         this.data.rooms.recalculateStats(this.bed.tileX, this.bed.tileY);
         this.bed.settler = null;
      }

      if (bed != null) {
         LevelSettler lastSettler = bed.settler;
         if (bed.settler != null) {
            bed.settler.bed = null;
         }

         bed.settler = this;
         bed.isLocked = false;
         if (lastSettler != this) {
            this.data.rooms.recalculateStats(bed.tileX, bed.tileY);
         }
      }

      this.bed = bed;
      SettlerMob mob = this.getMob();
      if (mob != null) {
         if (bed == null) {
            this.markRoomDirty();
            if (mob.canSubmitNoBedNotification()) {
               this.data.networkData.notifications.submitNotification("nobed", mob, SettlementNotificationSeverity.NOTE);
            }
         } else {
            this.markRoomDirty();
            this.data.networkData.notifications.removeNotification("nobed", mob);
         }

         mob.assignBed(this, bed, true);
         mob.makeSettler(this.data, this);
         this.settler.onMoveIn(this);
      }
   }

   public void onSettlerDeath() {
      if (this.bed != null && this.bed.getSettler() == this) {
         this.bed.settler = null;
      }

      this.bed = null;
      GameLog.debug.println("Removed settler from bed because of death");
      this.data.groups.onSettlerRemoved(this.mobUniqueID);
      this.data.sendEvent(SettlementSettlersChangedEvent.class);
   }

   public SettlementBed getBed() {
      return this.bed;
   }

   public void moveOut() {
      SettlerMob m = this.getMob();
      if (m != null) {
         m.moveOut();
      }
   }

   public boolean canMoveOut() {
      return this.settler.canMoveOut(this, this.data);
   }

   public boolean canBanish() {
      return this.settler.canBanish(this, this.data);
   }

   public void markRoomDirty() {
      this.roomDirty = true;
   }
}
