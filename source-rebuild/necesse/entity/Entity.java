package necesse.entity;

import java.awt.Point;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.GameState;
import necesse.engine.GlobalData;
import necesse.engine.WorldSettingsGetter;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.sound.SoundEmitter;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.PointSetAbstract;
import necesse.engine.util.SingletonPointSet;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldEntityGameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.chains.ChainLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.mapData.ClientDiscoveredMap;
import necesse.level.maps.regionSystem.EntityRegionPositionTracker;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionPositionGetter;
import necesse.level.maps.regionSystem.RegionTracker;
import necesse.level.maps.regionSystem.RegionTrackerGetter;

public abstract class Entity
   implements SoundEmitter,
   ChainLocation,
   RegionPositionGetter,
   RegionTrackerGetter<Entity>,
   GameState,
   WorldSettingsGetter,
   WorldEntityGameClock,
   DrawOnMapEntity,
   LoadingCompleteInterface {
   public static final float SPEED_DELTA_DIV = 250.0F;
   public static final float SPEED_DELTA_DIV_SQRT = (float)Math.sqrt(250.0);
   private boolean isInitialized;
   private boolean removed;
   private boolean disposed;
   public float x;
   public float y;
   private Point lastRegionPos;
   private Level level;
   private WorldEntity worldEntity;
   private WorldSettings worldSettings;
   private long clientUpdateTime;
   private int uniqueID;
   private boolean isDirty;
   protected final EntityRegionPositionTracker<Entity> regionTracker = new EntityRegionPositionTracker<>(this);

   @Override
   public RegionTracker<Entity> getRegionTracker() {
      return this.regionTracker;
   }

   public abstract void addDrawables(
      List<LevelSortedDrawable> var1,
      OrderableDrawables var2,
      OrderableDrawables var3,
      OrderableDrawables var4,
      Level var5,
      TickManager var6,
      GameCamera var7,
      PlayerMob var8
   );

   public void addDebugDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
   }

   @Override
   public boolean isVisibleOnMap(Client client, ClientDiscoveredMap map) {
      return GlobalData.debugCheatActive() || map.isTileKnown(this.getTileX(), this.getTileY());
   }

   @Override
   public Point getMapPos() {
      return new Point(this.getX(), this.getY());
   }

   public void init() {
      this.isInitialized = true;
   }

   public void postInit() {
   }

   public void onLevelChanged() {
   }

   public abstract void clientTick();

   public abstract void serverTick();

   public void updateRegionPos() {
      int newRegionX = this.level.regionManager.getRegionCoordByTile(this.getTileX());
      int newRegionY = this.level.regionManager.getRegionCoordByTile(this.getTileY());
      if (this.lastRegionPos == null) {
         this.lastRegionPos = new Point(newRegionX, newRegionY);
      } else if ((newRegionX != this.lastRegionPos.x || newRegionY != this.lastRegionPos.y)
         && this.level.regionManager.isRegionWithinBounds(newRegionX, newRegionY)) {
         this.onRegionChanged(this.lastRegionPos.x, this.lastRegionPos.y, newRegionX, newRegionY);
         this.lastRegionPos = new Point(newRegionX, newRegionY);
      }
   }

   public void onRegionChanged(int lastRegionX, int lastRegionY, int newRegionX, int newRegionY) {
      if (this.shouldRemoveWhenInUnloadedRegion() && !this.checkIfOccupyingRegions(p -> this.getLevel().regionManager.isRegionLoaded(p.x, p.y))) {
         this.remove();
      }
   }

   protected void sendPacketToNewClientsWithRegion(int lastRegionX, int lastRegionY, int newRegionX, int newRegionY, Supplier<Packet> packetSupplier) {
      ComputedValue<Packet> packet = new ComputedValue<>(packetSupplier);
      this.getServer()
         .streamClients()
         .filter(c -> !c.hasRegionLoaded(this.level, lastRegionX, lastRegionY) && c.hasRegionLoaded(this.level, newRegionX, newRegionY))
         .forEach(c -> c.sendPacket(packet.get()));
   }

   @Override
   public Level getLevel() {
      return this.level;
   }

   @Override
   public WorldEntity getWorldEntity() {
      if (this.worldEntity != null) {
         return this.worldEntity;
      } else {
         return this.level != null ? this.level.getWorldEntity() : null;
      }
   }

   @Override
   public WorldSettings getWorldSettings() {
      if (this.worldSettings != null) {
         return this.worldSettings;
      } else {
         return this.level != null ? this.level.getWorldSettings() : null;
      }
   }

   public void setWorldData(WorldEntity worldEntity, WorldSettings worldSettings) {
      if (worldEntity != null) {
         this.worldEntity = worldEntity;
      }

      if (worldSettings != null) {
         this.worldSettings = worldSettings;
      }
   }

   public void setLevel(Level level) {
      this.level = level;
      if (level != null) {
         this.setWorldData(level.getWorldEntity(), level.getWorldSettings());
      }
   }

   public void refreshClientUpdateTime() {
      WorldEntity worldEntity = this.getWorldEntity();
      if (worldEntity != null) {
         this.clientUpdateTime = worldEntity.getLocalTime();
      }
   }

   public long getTimeSinceClientUpdate() {
      WorldEntity worldEntity = this.getWorldEntity();
      return worldEntity != null ? worldEntity.getLocalTime() - this.clientUpdateTime : 0L;
   }

   public int getUniqueID(GameRandom random) {
      if (this.uniqueID == 0) {
         this.uniqueID = getNewUniqueID(this.level, random);
      }

      return this.uniqueID;
   }

   public int getUniqueID() {
      return this.getUniqueID(null);
   }

   public int getRealUniqueID() {
      return this.uniqueID;
   }

   public int resetUniqueID(GameRandom random) {
      this.uniqueID = 0;
      return this.getUniqueID(random);
   }

   public int resetUniqueID() {
      return this.resetUniqueID(null);
   }

   public void setUniqueID(int uniqueID) {
      this.uniqueID = uniqueID;
   }

   @Override
   public int getX() {
      return (int)this.x;
   }

   @Override
   public int getY() {
      return (int)this.y;
   }

   public int getTileX() {
      return getTileCoordinate(this.getX());
   }

   public int getTileY() {
      return getTileCoordinate(this.getY());
   }

   public static int getTileCoordinate(int levelCoordinate) {
      return GameMath.getTileCoordinate(levelCoordinate);
   }

   public static int getTileCoordinate(float levelCoordinate) {
      return GameMath.getTileCoordinate(levelCoordinate);
   }

   public static int getTileCoordinate(double levelCoordinate) {
      return GameMath.getTileCoordinate(levelCoordinate);
   }

   public Point getPositionPoint() {
      return new Point(this.getX(), this.getY());
   }

   public Point getTilePoint() {
      return new Point(this.getTileX(), this.getTileY());
   }

   @Override
   public PointSetAbstract<?> getRegionPositions() {
      int regionX = GameMath.getRegionCoordByTile(this.getTileX());
      int regionY = GameMath.getRegionCoordByTile(this.getTileY());
      return this.lastRegionPos != null && this.level != null && !this.level.regionManager.isRegionWithinBounds(regionX, regionY)
         ? new SingletonPointSet(this.lastRegionPos.x, this.lastRegionPos.y)
         : new SingletonPointSet(regionX, regionY);
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   @Override
   public void onLoadingComplete() {
   }

   @Override
   public void onUnloading(Region region) {
   }

   public void dispose() {
      this.disposed = true;
   }

   public boolean isDisposed() {
      return this.disposed;
   }

   public void remove() {
      if (!this.removed) {
         this.regionTracker.clearElements();
      }

      this.removed = true;
   }

   @Override
   public boolean removed() {
      return this.removed;
   }

   public void onRemovedFromManager() {
   }

   public void restore() {
      this.removed = false;
   }

   @Override
   public float getSoundPositionX() {
      return this.x;
   }

   @Override
   public float getSoundPositionY() {
      return this.y;
   }

   public Point getDrawPos() {
      return new Point(this.getDrawX(), this.getDrawY());
   }

   public int getDrawX() {
      return (int)this.x;
   }

   public int getDrawY() {
      return (int)this.y;
   }

   public boolean shouldSave() {
      return true;
   }

   public boolean shouldRemoveOnRegionUnload() {
      return this.isClient() ? true : this.shouldSave();
   }

   public boolean shouldRemoveWhenInUnloadedRegion() {
      return this.isClient() ? true : this.shouldRemoveOnRegionUnload();
   }

   public void limitWithinRegionBounds(int minLevelX, int minLevelY, int maxLevelX, int maxLevelY) {
      this.x = GameMath.limit(this.x, minLevelX, maxLevelX);
      this.y = GameMath.limit(this.y, minLevelY, maxLevelY);
   }

   public final void limitWithinRegionBounds(Region region) {
      if (region != null) {
         this.limitWithinRegionBounds(
            GameMath.getLevelCoordinate(region.tileXOffset) + 1,
            GameMath.getLevelCoordinate(region.tileYOffset) + 1,
            GameMath.getLevelCoordinate(region.tileXOffset + region.tileWidth) - 1,
            GameMath.getLevelCoordinate(region.tileYOffset + region.tileHeight) - 1
         );
      }
   }

   public static int getNewUniqueID(Level level, GameRandom random) {
      int uniqueID = GameRandom.getNewUniqueID(random);
      if (level != null) {
         while (level.entityManager.uniqueIDOccupied(uniqueID)) {
            uniqueID = GameRandom.getNewUniqueID(random);
         }
      }

      return uniqueID;
   }

   public void markDirty() {
      this.isDirty = true;
   }

   public void markClean() {
      this.isDirty = false;
   }

   public boolean isDirty() {
      return this.isDirty;
   }

   public boolean isSamePlace(Entity other) {
      return this.getLevel() != null && other.getLevel() != null ? this.getLevel().isSamePlace(other.getLevel()) : false;
   }

   public boolean isSamePlace(Level level) {
      return this.getLevel() != null && level != null ? this.getLevel().isSamePlace(level) : false;
   }

   @Deprecated
   public boolean isClientLevel() {
      return this.isClient();
   }

   @Override
   public boolean isClient() {
      return this.level != null && this.level.isClient();
   }

   @Override
   public Client getClient() {
      return this.level == null ? null : this.level.getClient();
   }

   @Deprecated
   public boolean isServerLevel() {
      return this.isServer();
   }

   @Override
   public boolean isServer() {
      return this.level != null && this.level.isServer();
   }

   @Override
   public Server getServer() {
      return this.level == null ? null : this.level.getServer();
   }

   public boolean isInitialized() {
      return this.isInitialized;
   }

   public static float getTravelTimeMillis(float speed, float distance) {
      if (speed <= 0.0F) {
         return 0.0F;
      }

      float distancePerSec = speed / 250.0F;
      return distance / distancePerSec;
   }

   public static float getTravelSpeedForMillis(int millis, float distance) {
      if (millis <= 0) {
         return 100.0F;
      }

      if (distance <= 0.0F) {
         return 100.0F;
      }

      float speedPerSec = 250.0F * distance;
      return speedPerSec / millis;
   }

   public static float getPositionAfterMillis(float speed, float time) {
      return speed * time / 250.0F;
   }
}
