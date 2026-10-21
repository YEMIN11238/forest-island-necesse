package necesse.entity.mobs.friendly.human;

import java.awt.Point;
import java.util.Objects;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.EquipmentBuffManager;
import necesse.entity.mobs.Mob;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.settlement.data.SettlerLooksData;
import necesse.level.maps.levelData.jobs.ConsumeFoodLevelJob;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.SettlementWealthCounter;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

public interface HumanSettlerMob extends SettlerMob {
   default HumanMob getHumanMob() {
      return (HumanMob)this;
   }

   @Override
   default void setHome(Point home) {
      HumanMob humanMob = this.getHumanMob();
      if (!Objects.equals(humanMob.home, home)) {
         humanMob.home = home;
         humanMob.sendWorkUpdatePacket();
      }
   }

   @Override
   default void assignBed(LevelSettler settler, SettlementBed bed, boolean walkNow) {
      HumanMob humanMob = this.getHumanMob();
      humanMob.moveOutPoint = null;
      humanMob.levelSettler = settler;
      if (humanMob.getLevel() != null) {
         if (bed != null) {
            humanMob.moveIn(bed.tileX, bed.tileY, walkNow);
         } else {
            humanMob.moveIn(settler.data.networkData.getTileX(), settler.data.networkData.getTileY(), walkNow);
         }
      }
   }

   @Override
   default boolean hasBedAssigned() {
      HumanMob humanMob = this.getHumanMob();
      return humanMob.levelSettler != null && humanMob.levelSettler.getBed() != null;
   }

   @Override
   default boolean isMovingIn() {
      return this.getHumanMob().moveInPoint != null;
   }

   @Override
   default void moveOut() {
      HumanMob humanMob = this.getHumanMob();
      if (humanMob.moveOutPoint == null) {
         if (!humanMob.isDowned()) {
            humanMob.moveOutPoint = humanMob.getNewEdgeOfSettlementTile();
         }

         humanMob.settlementUniqueID = 0;
         humanMob.levelSettler = null;
         humanMob.cancelJob();
      }
   }

   @Override
   default boolean isMovingOut() {
      return this.getHumanMob().moveOutPoint != null;
   }

   @Override
   default void settlementTick(ServerSettlementData data, LevelSettler settler) {
      HumanMob humanMob = this.getHumanMob();
      humanMob.levelSettler = settler;
      humanMob.updateTeam();
      humanMob.updateHappiness();

      for (SettlerPersonality personality : humanMob.personalities) {
         personality.settlementTick(data);
      }
   }

   @Override
   default void makeSettler(ServerSettlementData data, LevelSettler settler) {
      HumanMob humanMob = this.getHumanMob();
      if (humanMob.downedState != null) {
         humanMob.clearDownedState();
      }

      if (!this.isSettler()) {
         humanMob.hungerLevel = 0.2F;
         humanMob.markDirty();
      }

      if (humanMob.loadedDietFilter != null) {
         settler.dietFilter.loadFromCopy(humanMob.loadedDietFilter);
         humanMob.loadedDietFilter = null;
      }

      humanMob.canDespawn = false;
      humanMob.visitorSettlementUniqueID = 0;
      humanMob.settlementUniqueID = data.uniqueID;
      humanMob.cachedServerSettlementData = data;
      humanMob.cachedNetworkSettlementData = data.networkData;
      humanMob.cachedVisitorServerSettlementData = null;
      humanMob.cachedVisitorNetworkSettlementData = null;
      humanMob.levelSettler = settler;
      this.setSettlerSeed(settler.settlerSeed, false);
      this.assignBed(settler, settler.getBed(), false);
      humanMob.updateTeam();
   }

   @Override
   default void onRoomStatsCalculated(SettlementBed bed, SettlementRoom settlementRoom) {
      for (SettlerPersonality personality : this.getHumanMob().personalities) {
         personality.onRoomStatsCalculated(bed, settlementRoom);
      }
   }

   @Override
   default boolean isSettler() {
      return this.getHumanMob().settlementUniqueID != 0;
   }

   @Override
   default int getSettlementUniqueID() {
      return this.getHumanMob().settlementUniqueID;
   }

   @Override
   default void setSettlerSeed(int seed, boolean overrideName) {
      HumanMob humanMob = this.getHumanMob();
      humanMob.settlerSeed = seed;
      GameRandom random = new GameRandom(seed + 6);
      humanMob.gender = random.getOneOfWeighted(HumanGender.class, 40, HumanGender.MALE, 40, HumanGender.FEMALE, 20, HumanGender.NEUTRAL);
      if (!humanMob.customLook) {
         humanMob.look = new HumanLook();
         humanMob.randomizeLook(humanMob.look, humanMob.gender, random);
      }

      if (humanMob.settlerName == null || overrideName) {
         humanMob.settlerName = humanMob.getRandomName(new GameRandom(seed));
      }

      if (humanMob.personalities.isEmpty() || overrideName) {
         humanMob.setupPersonalities();
      }
   }

   @Override
   default int getSettlerSeed() {
      return this.getHumanMob().settlerSeed;
   }

   @Override
   default void setSettlerName(String name) {
      this.getHumanMob().settlerName = name;
   }

   @Override
   default String getSettlerName() {
      return this.getHumanMob().settlerName;
   }

   @Override
   default SettlerLooksData createLooksData() {
      HumanMob humanMob = this.getHumanMob();
      return new SettlerLooksData(humanMob.settlerSeed, humanMob.look, humanMob.customLook, this.getSettlerName());
   }

   @Override
   default InventoryItem getSettlerWeapon() {
      return this.getHumanMob().equipmentInventory.getItem(6);
   }

   @Override
   default InventoryItem getSettlerHeadArmor() {
      return this.getHumanMob().equipmentInventory.getItem(0);
   }

   @Override
   default InventoryItem getSettlerChestArmor() {
      return this.getHumanMob().equipmentInventory.getItem(1);
   }

   @Override
   default InventoryItem getSettlerBootsArmor() {
      return this.getHumanMob().equipmentInventory.getItem(2);
   }

   @Override
   default InventoryItem getSettlerHeadCosmetic() {
      return this.getHumanMob().equipmentInventory.getItem(3);
   }

   @Override
   default InventoryItem getSettlerChestCosmetic() {
      return this.getHumanMob().equipmentInventory.getItem(4);
   }

   @Override
   default InventoryItem getSettlerBootsCosmetic() {
      return this.getHumanMob().equipmentInventory.getItem(5);
   }

   @Override
   default EquipmentBuffManager getSettlerGearBuffManager() {
      return this.getHumanMob().equipmentBuffManager;
   }

   @Override
   default String getSettlerStringID() {
      return this.getHumanMob().settlerStringID;
   }

   @Override
   default boolean managesEquipmentThemself() {
      return this.getHumanMob().selfManageEquipment.get();
   }

   @Override
   default void setManageEquipmentThemself(boolean active) {
      this.getHumanMob().selfManageEquipment.set(active);
   }

   @Override
   default void addToWealthCounter(SettlementWealthCounter counter) {
      HumanMob humanMob = this.getHumanMob();

      for (int i = 0; i < humanMob.equipmentInventory.getSize(); i++) {
         counter.addSettlerItem(humanMob.equipmentInventory.getItem(i));
      }
   }

   @Override
   default boolean doesEatFood() {
      return !this.getHumanMob().jobTypeHandler.getJobHandler(ConsumeFoodLevelJob.class).disabledBySettler;
   }

   @Override
   default int getSettlerQualityOfLife() {
      return this.getHumanMob().settlerQualityOfLife;
   }

   @Override
   default int getSettlerHappiness() {
      return this.getHumanMob().settlerHappiness;
   }

   @Override
   default GameMessage getCurrentActivity() {
      GameMessage currentActivity = this.getHumanMob().currentActivity.getCurrentActivity();
      return currentActivity != null ? currentActivity : new LocalMessage("activities", "idle");
   }

   @Override
   default Mob getMob() {
      return (Mob)this;
   }
}
