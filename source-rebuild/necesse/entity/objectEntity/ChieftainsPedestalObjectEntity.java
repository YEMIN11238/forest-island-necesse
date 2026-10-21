package necesse.entity.objectEntity;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.OneWorldMigration;
import necesse.entity.events.EntityEvent;
import necesse.entity.levelEvent.ChieftainGauntletEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.ChieftainMob;
import necesse.entity.mobs.hostile.theRunebound.CroneMob;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.level.maps.Level;

public class ChieftainsPedestalObjectEntity extends ObjectEntity {
   public ArrayList<Point> wallTiles = new ArrayList<>();
   public ArrayList<Point> enemyPortalTiles = new ArrayList<>();
   public ArrayList<Point> teleportPortalTiles = new ArrayList<>();
   public Point chieftainSpawnPos;
   public int chieftainMobID;
   public Point croneSpawnPos;
   public int croneMobID;
   protected boolean isRising = true;
   protected long animationStartTime;
   protected int checkValidEventTicker;
   protected final ChieftainsPedestalObjectEntity.ResetPedestalEvent resetEvent = this.registerEvent(new ChieftainsPedestalObjectEntity.ResetPedestalEvent());

   public ChieftainsPedestalObjectEntity(Level level, int x, int y) {
      super(level, "chieftainpedestal", x, y);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addPointCollection("wallTiles", this.wallTiles);
      save.addPointCollection("enemyPortalTiles", this.enemyPortalTiles);
      save.addPointCollection("teleportPortalTiles", this.teleportPortalTiles);
      if (this.chieftainSpawnPos != null) {
         save.addPoint("chieftainSpawnPos", this.chieftainSpawnPos);
      }

      save.addInt("chieftainMobID", this.chieftainMobID);
      if (this.croneSpawnPos != null) {
         save.addPoint("croneSpawnPos", this.croneSpawnPos);
      }

      save.addInt("croneMobID", this.croneMobID);
      save.addLong("animationStartTime", this.animationStartTime);
      save.addBoolean("isRising", this.isRising);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.wallTiles = save.getPointCollection("wallTiles", new ArrayList<>());
      this.enemyPortalTiles = save.getPointCollection("enemyPortalTiles", new ArrayList<>());
      this.teleportPortalTiles = save.getPointCollection("teleportPortalTiles", new ArrayList<>());
      this.chieftainSpawnPos = save.getPoint("chieftainSpawnPos", this.chieftainSpawnPos);
      this.chieftainMobID = save.getInt("chieftainMobID", this.chieftainMobID);
      this.croneSpawnPos = save.getPoint("croneSpawnPos", this.croneSpawnPos);
      this.croneMobID = save.getInt("croneMobID", this.croneMobID);
      this.animationStartTime = save.getLong("animationStartTime", 0L);
      this.isRising = save.getBoolean("isRising", this.isRising);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      writer.putNextLong(this.animationStartTime);
      writer.putNextBoolean(this.isRising);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      this.animationStartTime = reader.getNextLong();
      this.isRising = reader.getNextBoolean();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.shouldHaveCollision()) {
         this.checkValidEventTicker++;
         if (this.checkValidEventTicker >= 100) {
            ChieftainGauntletEvent currentActiveEvent = this.getCurrentActiveEvent();
            if (currentActiveEvent == null) {
               this.resetEvent.runAndSend();
            }

            this.checkValidEventTicker = 0;
         }
      }
   }

   public boolean shouldHaveCollision() {
      return this.isRising || this.animationStartTime + ChieftainBoneSpikeWallObjectEntity.ANIMATION_TIME > this.getTime();
   }

   public void startAnimation(boolean isRising) {
      this.isRising = isRising;
      this.animationStartTime = this.getTime();
   }

   public float getAnimationHeightProgress() {
      long progress = this.getTime() - this.animationStartTime;
      if (this.isRising) {
         return progress < ChieftainBoneSpikeWallObjectEntity.ANIMATION_TIME ? (float)progress / ChieftainBoneSpikeWallObjectEntity.ANIMATION_TIME : 1.0F;
      } else {
         return progress < ChieftainBoneSpikeWallObjectEntity.ANIMATION_TIME
            ? 1.0F - (float)progress / ChieftainBoneSpikeWallObjectEntity.ANIMATION_TIME
            : 0.0F;
      }
   }

   public ChieftainGauntletEvent getCurrentActiveEvent() {
      return this.getLevel()
         .entityManager
         .events
         .regionList
         .getInRegionTileByTile(this.tileX, this.tileY)
         .stream()
         .filter(event -> event instanceof ChieftainGauntletEvent)
         .map(event -> (ChieftainGauntletEvent)event)
         .filter(event -> event.tileX == this.tileX && event.tileY == this.tileY)
         .findFirst()
         .orElse(null);
   }

   public boolean canStartChieftainEvent() {
      return this.chieftainSpawnPos == null ? false : this.getCurrentActiveEvent() == null;
   }

   @Override
   public void onMouseHover(PlayerMob perspective, boolean debug) {
      super.onMouseHover(perspective, debug);
      if (debug && this.isServer()) {
         StringTooltips tooltips = new StringTooltips();
         tooltips.add("Walls: " + this.wallTiles.size());
         tooltips.add("Enemy portals: " + Arrays.toString(this.enemyPortalTiles.stream().map(p -> p.x + "x" + p.y).toArray()));
         tooltips.add("Teleport portals: " + Arrays.toString(this.teleportPortalTiles.stream().map(p -> p.x + "x" + p.y).toArray()));
         tooltips.add(
            "Chieftain tile: "
               + (
                  this.chieftainSpawnPos == null
                     ? "N/A"
                     : GameMath.getTileCoordinate(this.chieftainSpawnPos.x) + "x" + GameMath.getTileCoordinate(this.chieftainSpawnPos.y)
               )
         );
         tooltips.add("Chieftain uniqueID: " + this.chieftainMobID);
         tooltips.add(
            "Crone tile: "
               + (
                  this.croneSpawnPos == null
                     ? "N/A"
                     : GameMath.getTileCoordinate(this.croneSpawnPos.x) + "x" + GameMath.getTileCoordinate(this.croneSpawnPos.y)
               )
         );
         tooltips.add("Crone uniqueID: " + this.croneMobID);
         GameTooltipManager.addTooltip(tooltips, TooltipLocation.INTERACT_FOCUS);
      }
   }

   public void startChieftainEvent() {
      ChieftainMob chieftainMob = null;
      if (this.chieftainMobID != -1) {
         Mob foundMob = this.getLevel().entityManager.mobs.get(this.chieftainMobID, false);
         if (foundMob instanceof ChieftainMob) {
            chieftainMob = (ChieftainMob)foundMob;
         }
      }

      if (chieftainMob == null) {
         chieftainMob = new ChieftainMob();
         chieftainMob.spawnPos = new Point(this.chieftainSpawnPos);
         float var10002 = this.chieftainSpawnPos.x;
         this.getLevel().entityManager.addMob(chieftainMob, var10002, this.chieftainSpawnPos.y);
         this.chieftainMobID = chieftainMob.getUniqueID();
      }

      CroneMob croneMob = null;
      if (this.croneMobID != -1) {
         Mob foundMob = this.getLevel().entityManager.mobs.get(this.croneMobID, false);
         if (foundMob instanceof CroneMob) {
            croneMob = (CroneMob)foundMob;
         }
      }

      if (croneMob == null && this.croneSpawnPos != null) {
         croneMob = new CroneMob();
         float var5 = this.croneSpawnPos.x;
         this.getLevel().entityManager.addMob(croneMob, var5, this.croneSpawnPos.y);
         this.croneMobID = croneMob.getUniqueID();
      }

      this.getLevel()
         .entityManager
         .events
         .add(
            new ChieftainGauntletEvent(
               this.tileX,
               this.tileY,
               this.wallTiles,
               this.enemyPortalTiles,
               this.teleportPortalTiles,
               this.chieftainSpawnPos,
               chieftainMob,
               this.croneSpawnPos,
               croneMob
            )
         );
   }

   @Override
   public void migrateToOneWorld(OneWorldMigration migrationData, LevelIdentifier oldLevelIdentifier, Point tileOffset, Point positionOffset) {
      super.migrateToOneWorld(migrationData, oldLevelIdentifier, tileOffset, positionOffset);
      ArrayList<Point> oldWallTiles = this.wallTiles;
      this.wallTiles = new ArrayList<>();

      for (Point tile : oldWallTiles) {
         this.wallTiles.add(new Point(tile.x + tileOffset.x, tile.y + tileOffset.y));
      }

      ArrayList<Point> oldEnemyPortalTiles = this.enemyPortalTiles;
      this.enemyPortalTiles = new ArrayList<>();

      for (Point tile : oldEnemyPortalTiles) {
         this.enemyPortalTiles.add(new Point(tile.x + tileOffset.x, tile.y + tileOffset.y));
      }

      ArrayList<Point> oldTeleportPortalTiles = this.teleportPortalTiles;
      this.teleportPortalTiles = new ArrayList<>();

      for (Point tile : oldTeleportPortalTiles) {
         this.teleportPortalTiles.add(new Point(tile.x + tileOffset.x, tile.y + tileOffset.y));
      }

      if (this.chieftainSpawnPos != null) {
         this.chieftainSpawnPos.x = this.chieftainSpawnPos.x + positionOffset.x;
         this.chieftainSpawnPos.y = this.chieftainSpawnPos.y + positionOffset.y;
      }
   }

   public class ResetPedestalEvent extends EntityEvent {
      public void runAndSend() {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextShortUnsigned(ChieftainsPedestalObjectEntity.this.wallTiles.size());

         for (Point wallTile : ChieftainsPedestalObjectEntity.this.wallTiles) {
            writer.putNextInt(wallTile.x);
            writer.putNextInt(wallTile.y);
         }

         this.runAndSendAbility(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int walls = reader.getNextShortUnsigned();
         ArrayList<Point> wallTiles = new ArrayList<>(walls);

         for (int i = 0; i < walls; i++) {
            wallTiles.add(new Point(reader.getNextInt(), reader.getNextInt()));
         }

         ChieftainGauntletEvent.resetWalls(ChieftainsPedestalObjectEntity.this.getLevel(), wallTiles);
         ChieftainsPedestalObjectEntity.this.startAnimation(true);
      }
   }
}
