package necesse.level.maps.levelData.settlementData.settler.personalities;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.packet.PacketObjectDamage;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PointHashSet;
import necesse.entity.DamagedObjectEntity;
import necesse.entity.ObjectDamageResult;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.gameObject.furniture.SettlerBedObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SimpleSettlerThought;

public class ClumsySettlerPersonality extends SettlerPersonality {
   public static int SECONDS_IT_TAKES_TO_DESTROY_FURNITURE = 3600;
   public static int BASE_HAPPINESS_BONUS = 40;
   public static int HAPPINESS_PER_BROKEN = -10;
   public static int MAX_FURNITURE_TO_KEEP_BROKEN = 6;
   public float nextTickBuffer;
   public float damageBuffer;
   public LinkedHashSet<Point> damagedFurnitureTiles = new LinkedHashSet<>();
   protected int lastCompletelyDamagedTiles;
   protected boolean shouldDoInitialTick;

   public ClumsySettlerPersonality(HumanMob mob) {
      super(mob);
   }

   @Override
   public void init() {
      super.init();
      this.shouldDoInitialTick = true;
      this.nextTickBuffer = GameRandom.globalRandom.nextFloat();
   }

   @Override
   public void onRegistered() {
      super.onRegistered();
      LocalMessage happyMessage = new LocalMessage("personalities", "clumsyhappy");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_happy", new SimpleSettlerThought(happyMessage, BASE_HAPPINESS_BONUS).setPersonalitySubmitter(this)
      );
      LocalMessage unHappyMessage = new LocalMessage("personalities", "clumsyunhappy");
      SettlerThoughtRegistry.registerSettlerThought(
         this.getStringID() + "_unhappy", new SimpleSettlerThought(unHappyMessage, HAPPINESS_PER_BROKEN).setPersonalitySubmitter(this)
      );
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addFloat("damageBuffer", this.damageBuffer);
      if (!this.damagedFurnitureTiles.isEmpty()) {
         save.addPointCollection("damagedFurnitureTiles", this.damagedFurnitureTiles);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.damageBuffer = save.getFloat("damageBuffer", this.damageBuffer, false);
      this.damagedFurnitureTiles = new LinkedHashSet<>(save.getPointCollection("damagedFurnitureTiles", new ArrayList<>(), false));
   }

   @Override
   public void settlementTick(ServerSettlementData data) {
      super.settlementTick(data);
      if (this.mob.levelSettler != null) {
         int secondsBetweenDamageTicks = GameMath.limit(DamagedObjectEntity.RECOVER_START_TIME / 1000 - 2, 1, 10);
         this.nextTickBuffer += 1.0F / secondsBetweenDamageTicks;
         if (this.nextTickBuffer >= 1.0F || this.shouldDoInitialTick) {
            this.nextTickBuffer--;
            SettlementBed bed = this.mob.levelSettler.getBed();
            if (bed == null) {
               this.damageBuffer = 0.0F;
               this.lastCompletelyDamagedTiles = 0;
            } else {
               SettlementRoom room = bed.getRoom();
               if (room == null) {
                  this.damageBuffer = 0.0F;
                  this.lastCompletelyDamagedTiles = 0;
               } else {
                  PointHashSet removedTiles = new PointHashSet();

                  for (Point currentTile : this.damagedFurnitureTiles) {
                     if (!room.containsTile(currentTile.x, currentTile.y)) {
                        removedTiles.add(currentTile.x, currentTile.y);
                     } else {
                        LevelObject levelObject = bed.data.getLevel().getLevelObject(currentTile.x, currentTile.y);
                        if (!this.isValidObject(bed.data, levelObject)) {
                           removedTiles.add(currentTile.x, currentTile.y);
                        }
                     }
                  }

                  for (Point tile : removedTiles) {
                     this.damagedFurnitureTiles.remove(tile);
                  }

                  if (!this.shouldDoInitialTick) {
                     this.damageBuffer = this.damageBuffer + 1.0F / SECONDS_IT_TAKES_TO_DESTROY_FURNITURE * secondsBetweenDamageTicks;
                  }

                  int nextCompletelyDamagedTiles = 0;

                  for (Point tile : this.damagedFurnitureTiles) {
                     LevelObject levelObject = bed.data.getLevel().getLevelObject(tile.x, tile.y);
                     if (!this.damageObject(levelObject)) {
                        break;
                     }

                     nextCompletelyDamagedTiles++;
                  }

                  while (nextCompletelyDamagedTiles == this.damagedFurnitureTiles.size() && this.damagedFurnitureTiles.size() < MAX_FURNITURE_TO_KEEP_BROKEN) {
                     LevelObject levelObject = this.findAndAddNewObject(room);
                     if (levelObject == null || !this.damageObject(levelObject)) {
                        break;
                     }

                     nextCompletelyDamagedTiles++;
                  }

                  this.lastCompletelyDamagedTiles = nextCompletelyDamagedTiles;
                  if (this.damagedFurnitureTiles.isEmpty()) {
                     this.damageBuffer = 0.0F;
                  }
               }
            }
         }

         this.shouldDoInitialTick = false;
      }
   }

   public boolean damageObject(LevelObject levelObject) {
      float toDamage = this.damageBuffer * levelObject.object.objectHealth;
      int dealDamage = 0;
      if (toDamage > 0.0F) {
         dealDamage = (int)toDamage;
      }

      int currentDamage = 0;
      Level level = levelObject.level;
      DamagedObjectEntity damagedObjectEntity = level.entityManager.getDamagedObjectEntity(levelObject.tileX, levelObject.tileY);
      if (damagedObjectEntity != null) {
         currentDamage = damagedObjectEntity.getObjectDamage(ObjectLayerRegistry.BASE_LAYER);
      }

      if (dealDamage > 0) {
         int remainingDamage = levelObject.object.objectHealth - currentDamage;
         if (dealDamage >= remainingDamage) {
            dealDamage = Math.max(0, remainingDamage - 1);
         }
      }

      if (dealDamage > 0) {
         this.damageBuffer = this.damageBuffer - (float)dealDamage / levelObject.object.objectHealth;
      }

      if (currentDamage <= 0 && dealDamage <= 0) {
         return false;
      }

      ObjectDamageResult result = level.entityManager.doObjectDamageOverride(0, levelObject.tileX, levelObject.tileY, dealDamage);
      result.damagedObjectEntity.shouldSave = true;
      result.damagedObjectEntity.hasExtendedRecoveryStartTime = true;
      level.getServer().network.sendToClientsWithTile(new PacketObjectDamage(result), level, levelObject.tileX, levelObject.tileY);
      currentDamage += dealDamage;
      return currentDamage == levelObject.object.objectHealth - 1;
   }

   public LevelObject findAndAddNewObject(SettlementRoom room) {
      ArrayList<LevelObject> validObjects = new ArrayList<>();

      for (LevelObject newLevelObject : room.getLevelObjectsInRoom(true, true)) {
         if (!this.damagedFurnitureTiles.contains(new Point(newLevelObject.tileX, newLevelObject.tileY)) && this.isValidObject(room.data, newLevelObject)) {
            validObjects.add(newLevelObject);
         }
      }

      if (!validObjects.isEmpty()) {
         LevelObject levelObject = GameRandom.globalRandom.getOneOf(validObjects);
         Point tile = new Point(levelObject.tileX, levelObject.tileY);
         this.damagedFurnitureTiles.add(tile);
         return levelObject;
      } else {
         return null;
      }
   }

   public boolean isValidObject(ServerSettlementData data, LevelObject levelObject) {
      if (levelObject.object.getID() == 0) {
         return false;
      } else if (levelObject.object.objectHealth <= 10) {
         return false;
      } else if (!levelObject.object.isMultiTileMaster()) {
         return false;
      } else if (levelObject.object.roomProperties.contains("lights")) {
         return false;
      } else {
         return levelObject.object instanceof SettlerBedObject ? false : !data.storageManager.hasInventoryAtTile(levelObject.tileX, levelObject.tileY);
      }
   }

   @Override
   public SettlerThoughtsList getThoughts(SettlementBed bed) {
      SettlerThoughtsList list = new SettlerThoughtsList();
      list.addThought(this.getStringID() + "_happy");
      if (this.lastCompletelyDamagedTiles > 0) {
         list.addThought(this.getStringID() + "_unhappy", this.lastCompletelyDamagedTiles);
      }

      return list;
   }
}
