package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Point;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.EquipmentBuffManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.settlement.data.SettlerGearData;
import necesse.inventory.container.settlement.data.SettlerLooksData;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.SettlementWealthCounter;

public interface SettlerMob {
   void setHome(Point var1);

   void assignBed(LevelSettler var1, SettlementBed var2, boolean var3);

   boolean hasBedAssigned();

   boolean isMovingIn();

   void moveOut();

   boolean isMovingOut();

   void settlementTick(ServerSettlementData var1, LevelSettler var2);

   void makeSettler(ServerSettlementData var1, LevelSettler var2);

   void onRoomStatsCalculated(SettlementBed var1, SettlementRoom var2);

   boolean isSettler();

   int getSettlementUniqueID();

   default LevelIdentifier getSettlementLevelIdentifier() {
      int settlementUniqueID = this.getSettlementUniqueID();
      if (settlementUniqueID == 0) {
         return null;
      }

      Mob mob = this.getMob();
      if (mob.getLevel() == null) {
         return null;
      }

      CachedSettlementData settlement = SettlementsWorldData.getSettlementsData(mob).getCachedData(settlementUniqueID);
      return settlement != null ? settlement.levelIdentifier : null;
   }

   default boolean isSettlerWithinSettlement(NetworkSettlementData settlement) {
      Mob mob = this.getMob();
      return mob.getLevel() != null && !mob.getLevel().getIdentifier().equals(settlement.level.getIdentifier())
         ? false
         : settlement.isTileWithinBounds(mob.getTileX(), mob.getTileY());
   }

   default boolean isSettlerWithinSettlement() {
      NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
      return settlement == null ? false : this.isSettlerWithinSettlement(settlement);
   }

   default boolean isSettlerWithinSettlementLoadedRegions(NetworkSettlementData settlement) {
      Mob mob = this.getMob();
      Level level = mob.getLevel();
      if (level == null) {
         return false;
      } else {
         return !level.getIdentifier().equals(settlement.level.getIdentifier())
            ? false
            : settlement.isTileWithinLoadedRegionBounds(mob.getTileX(), mob.getTileY());
      }
   }

   default boolean isSettlerWithinSettlementLoadedRegions() {
      NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
      return settlement == null ? false : this.isSettlerWithinSettlementLoadedRegions(settlement);
   }

   default boolean isSettlerOnCurrentLevel() {
      if (!this.isSettler()) {
         return false;
      }

      LevelIdentifier identifier = this.getSettlementLevelIdentifier();
      if (identifier == null) {
         return false;
      }

      Level level = this.getMob().getLevel();
      return level != null && level.getIdentifier().equals(identifier);
   }

   default Level getSettlementServerLevel() {
      if (!this.isSettler()) {
         return null;
      } else {
         Level level = this.getMob().getLevel();
         if (level != null && level.isServer()) {
            LevelIdentifier levelIdentifier = this.getSettlementLevelIdentifier();
            return levelIdentifier == null ? null : level.getServer().world.getLevel(levelIdentifier);
         } else {
            return null;
         }
      }
   }

   default ServerSettlementData getSettlerSettlementServerData() {
      int settlementUniqueID = this.getSettlementUniqueID();
      if (settlementUniqueID == 0) {
         return null;
      } else {
         Mob mob = this.getMob();
         if (mob.isClient()) {
            return null;
         } else {
            return mob.getWorldEntity() == null ? null : SettlementsWorldData.getSettlementsData(mob).getOrLoadServerData(settlementUniqueID);
         }
      }
   }

   default NetworkSettlementData getSettlerSettlementNetworkData() {
      int settlementUniqueID = this.getSettlementUniqueID();
      if (settlementUniqueID == 0) {
         return null;
      }

      Mob mob = this.getMob();
      return mob.getWorldEntity() == null ? null : SettlementsWorldData.getSettlementsData(mob.getWorldEntity()).getNetworkData(settlementUniqueID);
   }

   void setSettlerSeed(int var1, boolean var2);

   int getSettlerSeed();

   void setSettlerName(String var1);

   String getSettlerName();

   SettlerLooksData createLooksData();

   default SettlerGearData createGearData() {
      return new SettlerGearData(
         this.getSettlerWeapon(),
         this.getSettlerHeadArmor(),
         this.getSettlerChestArmor(),
         this.getSettlerBootsArmor(),
         this.getSettlerHeadCosmetic(),
         this.getSettlerChestCosmetic(),
         this.getSettlerBootsCosmetic()
      );
   }

   InventoryItem getSettlerWeapon();

   InventoryItem getSettlerHeadArmor();

   InventoryItem getSettlerChestArmor();

   InventoryItem getSettlerBootsArmor();

   InventoryItem getSettlerHeadCosmetic();

   InventoryItem getSettlerChestCosmetic();

   InventoryItem getSettlerBootsCosmetic();

   EquipmentBuffManager getSettlerGearBuffManager();

   String getSettlerStringID();

   default Settler getSettler() {
      String settlerStringID = this.getSettlerStringID();
      return settlerStringID == null ? null : SettlerRegistry.getSettler(settlerStringID);
   }

   boolean managesEquipmentThemself();

   void setManageEquipmentThemself(boolean var1);

   void addToWealthCounter(SettlementWealthCounter var1);

   boolean doesEatFood();

   int getSettlerQualityOfLife();

   int getSettlerHappiness();

   default int getSettlerTotalHappinessIncludingQualityOfLife() {
      return this.getSettlerHappiness()
         + (HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD - HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD) * this.getSettlerQualityOfLife();
   }

   default boolean shouldIgnoreQualityOfLifeAndHappiness() {
      return false;
   }

   boolean hasCommandOrders();

   GameMessage getCurrentActivity();

   Mob getMob();

   default void runSettlerCheck() {
      if (this.isSettler() && !this.isMovingOut()) {
         ServerSettlementData settlementData = this.getSettlerSettlementServerData();
         if (settlementData != null && !settlementData.isMobPartOf(this)) {
            this.moveOut();
         }
      }
   }

   default boolean canSubmitNoBedNotification() {
      return true;
   }
}
