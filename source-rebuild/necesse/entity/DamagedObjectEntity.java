package necesse.entity;

import java.util.ArrayList;
import necesse.engine.GameEvents;
import necesse.engine.GameLog;
import necesse.engine.events.players.ObjectDamageEvent;
import necesse.engine.events.players.TileDamageEvent;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketObjectDamage;
import necesse.engine.network.packet.PacketTileDamage;
import necesse.engine.network.packet.PacketTileDestroyed;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobObjectDamagedEvent;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.LevelTile;

public class DamagedObjectEntity extends TileEntity {
   public static int RECOVER_START_TIME = 15000;
   public static int DAMAGE_RECOVERY_PER_SECOND = 10;
   public int tileDamage = 0;
   public int[] objectDamage = new int[ObjectLayerRegistry.getTotalLayers()];
   public long lastDamageTime;
   public boolean hasExtendedRecoveryStartTime;
   public boolean shouldSave;
   public float damageRecoverBuffer;

   public DamagedObjectEntity(Level level, int tileX, int tileY) {
      super(level, tileX, tileY);
   }

   public boolean shouldSave() {
      return this.shouldSave && (this.tileDamage > 0 || this.hasAnyObjectDamage());
   }

   public void addSaveData(SaveData save) {
      if (this.tileDamage > 0) {
         save.addInt("tileDamage", this.tileDamage);
      }

      SaveData objectDamageSave = new SaveData("objectDamage");

      for (int layerID = 0; layerID < this.objectDamage.length; layerID++) {
         if (this.objectDamage[layerID] > 0) {
            String layerStringID = ObjectLayerRegistry.getLayerStringID(layerID);
            objectDamageSave.addInt(layerStringID + "Damage", this.objectDamage[layerID]);
         }
      }

      if (!objectDamageSave.isEmpty()) {
         save.addSaveData(objectDamageSave);
      }

      save.addLong("lastDamageTime", this.lastDamageTime);
      if (this.hasExtendedRecoveryStartTime) {
         save.addBoolean("hasExtendedRecoveryStartTime", this.hasExtendedRecoveryStartTime);
      }
   }

   public void applyLoadData(LoadData save) {
      this.tileDamage = save.getInt("tileDamage", 0, false);
      LoadData objectDamageSave = save.getFirstLoadDataByName("objectDamage");
      if (objectDamageSave != null) {
         for (LoadData damageSave : objectDamageSave.getLoadData()) {
            if (damageSave.isData() && damageSave.getName().endsWith("Damage")) {
               String layerStringID = damageSave.getName().substring(0, damageSave.getName().length() - "Damage".length());
               int layerID = ObjectLayerRegistry.getLayerID(layerStringID);
               if (layerID != -1) {
                  try {
                     this.objectDamage[layerID] = Integer.parseInt(damageSave.getData());
                  } catch (NumberFormatException e) {
                     GameLog.warn
                        .println(
                           "Failed to load object damage at tile "
                              + this.tileX
                              + "x"
                              + this.tileY
                              + " for layer string ID "
                              + layerStringID
                              + " because the damage value is not a valid integer: "
                              + damageSave.getData()
                        );
                  }
               } else {
                  GameLog.warn
                     .println(
                        "Failed to load object damage at tile "
                           + this.tileX
                           + "x"
                           + this.tileY
                           + " for layer string ID "
                           + layerStringID
                           + " because it does not exist"
                     );
               }
            }
         }
      }

      this.lastDamageTime = save.getLong("lastDamageTime", 0L, false);
      this.hasExtendedRecoveryStartTime = save.getBoolean("hasExtendedRecoveryStartTime", this.hasExtendedRecoveryStartTime, false);
   }

   public void setupSpawnPacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.tileDamage);
      writer.putNextShortUnsigned(this.objectDamage[0]);
      boolean hasAnyObjectLayerDamage = false;

      for (int i = 1; i < this.objectDamage.length; i++) {
         if (this.objectDamage[i] > 0) {
            hasAnyObjectLayerDamage = true;
            break;
         }
      }

      writer.putNextBoolean(hasAnyObjectLayerDamage);
      if (hasAnyObjectLayerDamage) {
         for (int i = 1; i < this.objectDamage.length; i++) {
            writer.putNextShortUnsigned(this.objectDamage[i]);
         }
      }

      writer.putNextLong(this.lastDamageTime);
      writer.putNextBoolean(this.hasExtendedRecoveryStartTime);
   }

   public void applySpawnPacket(PacketReader reader) {
      this.tileDamage = reader.getNextShortUnsigned();
      this.objectDamage[0] = reader.getNextShortUnsigned();
      boolean hasAnyObjectLayerDamage = reader.getNextBoolean();
      if (hasAnyObjectLayerDamage) {
         for (int i = 1; i < this.objectDamage.length; i++) {
            this.objectDamage[i] = reader.getNextShortUnsigned();
         }
      }

      this.lastDamageTime = reader.getNextLong();
      this.hasExtendedRecoveryStartTime = reader.getNextBoolean();
   }

   @Override
   public void clientTick() {
      if (!this.removed()) {
         this.tickDamageRecovery();
      }
   }

   @Override
   public void serverTick() {
      if (!this.removed()) {
         this.tickDamageRecovery();
         this.checkTileDamage(null, null, null, true);

         for (int layer = 0; layer < this.objectDamage.length; layer++) {
            this.checkObjectDamage(layer, null, null, null, true);
         }
      }
   }

   public void tickDamageRecovery() {
      int startTime = RECOVER_START_TIME;
      if (this.hasExtendedRecoveryStartTime) {
         startTime *= 4;
      }

      if (this.getTimeSinceLastDamage() > startTime) {
         this.damageRecoverBuffer = this.damageRecoverBuffer + DAMAGE_RECOVERY_PER_SECOND / 20.0F;
         if (this.damageRecoverBuffer >= 1.0F) {
            int damageRecover = (int)this.damageRecoverBuffer;
            this.damageRecoverBuffer -= damageRecover;
            this.tileDamage = Math.max(this.tileDamage - damageRecover, 0);

            for (int i = 0; i < this.objectDamage.length; i++) {
               this.objectDamage[i] = Math.max(this.objectDamage[i] - damageRecover, 0);
            }
         }

         if (this.shouldRemove()) {
            this.remove();
         }
      } else {
         this.damageRecoverBuffer = 0.0F;
      }
   }

   public long getTimeSinceLastDamage() {
      return this.getTime() - this.lastDamageTime;
   }

   public int getObjectDamage(int layerID) {
      return this.objectDamage[layerID];
   }

   public boolean hasAnyObjectDamage() {
      for (int damage : this.objectDamage) {
         if (damage != 0) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldRemove() {
      return this.tileDamage == 0 && !this.hasAnyObjectDamage();
   }

   public static void destroyObject(
      Level level, int layerID, int tileX, int tileY, Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped, boolean sendPacket
   ) {
      GameObject currentObject = level.getObject(layerID, tileX, tileY);
      if (itemsDropped == null) {
         itemsDropped = new ArrayList<>();
      }

      currentObject.onDestroyed(level, layerID, tileX, tileY, attacker, client, itemsDropped);
      if (level.isServer()) {
         if (sendPacket) {
            level.getServer()
               .network
               .sendToClientsWithTile(new PacketTileDestroyed(level, tileX, tileY, currentObject.getID(), false, layerID), level, tileX, tileY);
         }

         level.onObjectDestroyed(currentObject, layerID, tileX, tileY, attacker, client, itemsDropped);
      }

      level.getLevelTile(tileX, tileY).checkAround();
      level.getTile(tileX, tileY).checkIsValid(level, tileX, tileY);
      level.getLevelObject(tileX, tileY).checkAround();
      level.objectLayer.setIsPlayerPlaced(layerID, tileX, tileY, false);
   }

   public static void destroyTile(
      Level level, int tileX, int tileY, Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped, boolean sendPacket
   ) {
      GameTile currentTile = level.getTile(tileX, tileY);
      currentTile.onDestroyed(level, tileX, tileY, attacker, client, itemsDropped == null ? new ArrayList<>() : itemsDropped);
      if (level.isServer()) {
         if (sendPacket) {
            level.getServer().network.sendToClientsWithTile(new PacketTileDestroyed(level, tileX, tileY, currentTile.getID(), true, -1), level, tileX, tileY);
         }

         level.onTileDestroyed(currentTile, tileX, tileY, attacker, client, itemsDropped);
      }

      level.getLevelTile(tileX, tileY).checkAround();
      level.getLevelObject(tileX, tileY).checkAround();

      for (int layerID : ObjectLayerRegistry.getLayerIDs()) {
         GameObject object = level.getObject(layerID, tileX, tileY);
         if (object.getID() != 0) {
            object.checkIsValid(level, layerID, tileX, tileY);
         }
      }

      level.tileLayer.setIsPlayerPlaced(tileX, tileY, false);
   }

   public boolean checkObjectDamage(int objectLayerID, Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped, boolean sendPacket) {
      Level level = this.getLevel();
      GameObject currentObject = level.getObject(objectLayerID, this.tileX, this.tileY);
      if (this.objectDamage[objectLayerID] >= currentObject.objectHealth) {
         destroyObject(level, objectLayerID, this.tileX, this.tileY, attacker, client, itemsDropped, sendPacket);
         this.objectDamage[objectLayerID] = 0;
         if (this.shouldRemove()) {
            this.remove();
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean checkTileDamage(Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped, boolean sendPacket) {
      Level level = this.getLevel();
      GameTile currentTile = level.getTile(this.tileX, this.tileY);
      if (this.tileDamage >= currentTile.tileHealth) {
         destroyTile(level, this.tileX, this.tileY, attacker, client, itemsDropped, sendPacket);
         this.tileDamage = 0;
         if (this.shouldRemove()) {
            this.remove();
         }

         return true;
      } else {
         return false;
      }
   }

   public void updateTileDamage(int totalDamage, boolean destroyed) {
      this.lastDamageTime = this.getTime();
      this.tileDamage = destroyed ? 0 : totalDamage;
      if (this.shouldRemove()) {
         this.remove();
      }
   }

   public void updateObjectDamage(int objectLayerID, int totalDamage, boolean destroyed) {
      this.lastDamageTime = this.getTime();
      this.objectDamage[objectLayerID] = destroyed ? 0 : totalDamage;
      if (this.shouldRemove()) {
         this.remove();
      }
   }

   public TileDamageResult doTileDamageOverride(int damage) {
      this.tileDamage = Math.max(0, this.tileDamage + damage);
      ArrayList<ItemPickupEntity> itemsDropped = new ArrayList<>();
      boolean destroyed = false;
      LevelTile levelTile = this.getLevel().getLevelTile(this.tileX, this.tileY);
      LevelObject levelObject = this.getLevel().getLevelObject(this.tileX, this.tileY);
      int totalDamage = this.tileDamage;
      if (!this.isClient()) {
         destroyed = this.checkTileDamage(null, null, itemsDropped, true);
      }

      return new TileDamageResult(this, levelTile, levelObject, totalDamage, damage, destroyed, itemsDropped, false, 0, 0);
   }

   public ObjectDamageResult doObjectDamageOverride(int objectLayerID, int damage, boolean checkDamageNow) {
      this.lastDamageTime = this.getTime();
      this.objectDamage[objectLayerID] = Math.max(0, this.objectDamage[objectLayerID] + damage);
      ArrayList<ItemPickupEntity> itemsDropped = new ArrayList<>();
      boolean destroyed = false;
      LevelTile levelTile = this.getLevel().getLevelTile(this.tileX, this.tileY);
      LevelObject levelObject = this.getLevel().getLevelObject(objectLayerID, this.tileX, this.tileY);
      int totalDamage = this.objectDamage[objectLayerID];
      if (!this.isClient() && checkDamageNow) {
         destroyed = this.checkObjectDamage(objectLayerID, null, null, itemsDropped, true);
      }

      return new ObjectDamageResult(this, levelTile, levelObject, objectLayerID, totalDamage, damage, destroyed, itemsDropped, false, 0, 0);
   }

   public TileDamageResult doTileDamage(int damage, float toolTier, Attacker attacker, ServerClient client) {
      return this.doTileDamage(damage, toolTier, attacker, client, false, 0, 0);
   }

   public TileDamageResult doTileDamage(int damage, float toolTier, Attacker attacker, ServerClient client, boolean showEffects, int mouseX, int mouseY) {
      TileDamageEvent event = new TileDamageEvent(this.getLevel(), this.tileX, this.tileY, damage, toolTier, attacker, client);
      GameEvents.triggerEvent(event);
      if (!event.isPrevented()) {
         this.lastDamageTime = this.getTime();
         GameTile tile = this.getLevel().getTile(this.tileX, this.tileY);
         if (tile.getID() != 0 && tile.canBeMined) {
            if (this.getLevel().isProtected(this.tileX, this.tileY) || toolTier != -1.0F && tile.toolTier > toolTier) {
               damage = 0;
            }

            boolean damaged = tile.onDamaged(this.getLevel(), this.tileX, this.tileY, damage, attacker, client, showEffects, mouseX, mouseY);
            if (!damaged) {
               damage = 0;
            }

            this.tileDamage += damage;
            ArrayList<ItemPickupEntity> itemsDropped = new ArrayList<>();
            boolean destroyed = false;
            LevelTile levelTile = this.getLevel().getLevelTile(this.tileX, this.tileY);
            LevelObject levelObject = this.getLevel().getLevelObject(this.tileX, this.tileY);
            int totalDamage = this.tileDamage;
            if (!this.isClient()) {
               destroyed = this.checkTileDamage(attacker, client, itemsDropped, false);
            }

            TileDamageResult result = new TileDamageResult(
               this, levelTile, levelObject, totalDamage, damage, destroyed, itemsDropped, showEffects, mouseX, mouseY
            );
            if (this.isServer()) {
               if (client != null) {
                  this.getServer().network.sendToClientsWithTileExcept(new PacketTileDamage(result), this.getLevel(), this.tileX, this.tileY, client);
                  client.sendPacket(new PacketTileDamage(result, false));
               } else {
                  this.getServer().network.sendToClientsWithTile(new PacketTileDamage(result), this.getLevel(), this.tileX, this.tileY);
               }

               this.getLevel().onTileDamaged(tile, this.tileX, this.tileY, attacker, client, result);
               if (destroyed) {
                  this.getServer()
                     .network
                     .sendToClientsWithTile(
                        new PacketTileDestroyed(this.getLevel(), this.tileX, this.tileY, tile.getID(), true, -1), this.getLevel(), this.tileX, this.tileY
                     );
               }
            }

            return result;
         }
      }

      return null;
   }

   public ObjectDamageResult doObjectDamage(int objectLayerID, int damage, float toolTier, Attacker attacker, ServerClient client) {
      return this.doObjectDamage(objectLayerID, damage, toolTier, attacker, client, false, 0, 0);
   }

   public ObjectDamageResult doObjectDamage(
      int objectLayerID, int damage, float toolTier, Attacker attacker, ServerClient client, boolean showEffects, int mouseX, int mouseY
   ) {
      int startDamage = damage;
      ObjectDamageEvent event = new ObjectDamageEvent(this.getLevel(), objectLayerID, this.tileX, this.tileY, damage, toolTier, attacker, client);
      GameEvents.triggerEvent(event);
      if (!event.isPrevented()) {
         this.lastDamageTime = this.getTime();
         GameObject obj = this.getLevel().getObject(objectLayerID, this.tileX, this.tileY);
         if (obj.getID() != 0 && obj.toolType != ToolType.UNBREAKABLE) {
            if (!obj.canPlaceOnProtectedLevels && this.getLevel().isProtected(this.tileX, this.tileY) || toolTier != -1.0F && obj.toolTier > toolTier) {
               damage = 0;
            }

            if (obj.getMultiTile(this.getLevel(), objectLayerID, this.tileX, this.tileY)
               .streamOtherObjects(this.tileX, this.tileY)
               .anyMatch(e -> !e.value.canPlaceOnProtectedLevels && this.getLevel().isProtected(e.tileX, e.tileY))) {
               damage = 0;
            }

            boolean damaged = obj.onDamaged(this.getLevel(), objectLayerID, this.tileX, this.tileY, damage, attacker, client, showEffects, mouseX, mouseY);
            if (!damaged) {
               damage = 0;
            }

            this.objectDamage[objectLayerID] = this.objectDamage[objectLayerID] + damage;
            LevelObject master = obj.isMultiTileMaster()
               ? null
               : obj.getMultiTile(this.getLevel(), objectLayerID, this.tileX, this.tileY)
                  .getMasterLevelObject(this.getLevel(), objectLayerID, this.tileX, this.tileY)
                  .orElse(null);
            ArrayList<ItemPickupEntity> itemsDropped = new ArrayList<>();
            boolean destroyed = false;
            LevelTile levelTile = this.getLevel().getLevelTile(this.tileX, this.tileY);
            LevelObject levelObject = this.getLevel().getLevelObject(objectLayerID, this.tileX, this.tileY);
            if (master != null) {
               ObjectDamageResult result = this.getLevel()
                  .entityManager
                  .doObjectDamage(objectLayerID, master.tileX, master.tileY, damage, toolTier, attacker, client, false, mouseX, mouseY);
               if (result != null) {
                  destroyed = result.destroyed;
                  itemsDropped.addAll(result.itemsDropped);
               }
            }

            int totalDamage = this.objectDamage[objectLayerID];
            if (!this.isClient()) {
               destroyed = this.checkObjectDamage(objectLayerID, attacker, client, itemsDropped, false) || destroyed;
            }

            ObjectDamageResult result = new ObjectDamageResult(
               this, levelTile, levelObject, objectLayerID, totalDamage, damage, destroyed, itemsDropped, showEffects, mouseX, mouseY
            );
            if (this.isServer()) {
               if (client != null) {
                  this.getServer().network.sendToClientsWithTileExcept(new PacketObjectDamage(result), this.getLevel(), this.tileX, this.tileY, client);
                  client.sendPacket(new PacketObjectDamage(result, false));
               } else {
                  this.getServer().network.sendToClientsWithTile(new PacketObjectDamage(result), this.getLevel(), this.tileX, this.tileY);
               }

               this.getLevel().onObjectDamaged(obj, objectLayerID, this.tileX, this.tileY, attacker, client, result);
               if (destroyed) {
                  this.getServer()
                     .network
                     .sendToClientsWithTile(
                        new PacketTileDestroyed(this.getLevel(), this.tileX, this.tileY, obj.getID(), false, objectLayerID),
                        this.getLevel(),
                        this.tileX,
                        this.tileY
                     );
               }
            }

            if (attacker != null) {
               MobObjectDamagedEvent mobEvent = new MobObjectDamagedEvent(this.getLevel(), startDamage, toolTier, result, attacker);

               for (Mob mob : attacker.getAttackOwnerChain()) {
                  mob.buffManager.submitMobEvent(mobEvent);
               }
            }

            return result;
         }
      }

      return null;
   }

   public AbstractDamageResult doToolDamage(
      int priorityObjectLayerID,
      int damage,
      ToolType toolType,
      float toolTier,
      Attacker attacker,
      ServerClient client,
      boolean showEffects,
      int mouseX,
      int mouseY
   ) {
      Level level = this.getLevel();
      if (toolType == ToolType.AXE || toolType == ToolType.PICKAXE || toolType == ToolType.ALL) {
         if (priorityObjectLayerID != -1) {
            int objectID = level.getObjectID(priorityObjectLayerID, this.tileX, this.tileY);
            if (objectID != 0) {
               GameObject obj = ObjectRegistry.getObject(objectID);
               if (obj.toolType != ToolType.UNBREAKABLE) {
                  if (!toolType.canDealDamageTo(obj.toolType)) {
                     damage = 0;
                  }

                  return this.doObjectDamage(priorityObjectLayerID, damage, toolTier, attacker, client, showEffects, mouseX, mouseY);
               }
            }
         }

         for (int layer = ObjectLayerRegistry.getTotalLayers() - 1; layer >= 0; layer--) {
            int objectID = level.getObjectID(layer, this.tileX, this.tileY);
            if (objectID != 0) {
               GameObject obj = ObjectRegistry.getObject(objectID);
               if (obj.toolType != ToolType.UNBREAKABLE) {
                  if (!toolType.canDealDamageTo(obj.toolType)) {
                     damage = 0;
                  }

                  return this.doObjectDamage(layer, damage, toolTier, attacker, client, showEffects, mouseX, mouseY);
               }
            }
         }
      }

      if (toolType == ToolType.SHOVEL || toolType == ToolType.ALL) {
         GameTile tile = level.getTile(this.tileX, this.tileY);
         if (tile.getID() != 0 && tile.canBeMined) {
            return this.doTileDamage(damage, toolTier, attacker, client, showEffects, mouseX, mouseY);
         }
      }

      return null;
   }
}
