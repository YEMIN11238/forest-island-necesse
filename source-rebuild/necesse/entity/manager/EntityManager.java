package necesse.entity.manager;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.packet.PacketDamagedTileEntity;
import necesse.engine.network.packet.PacketObjectEntity;
import necesse.engine.network.packet.PacketRemoveMob;
import necesse.engine.network.packet.PacketRemovePickupEntity;
import necesse.engine.network.packet.PacketRemoveProjectile;
import necesse.engine.network.packet.PacketSpawnMob;
import necesse.engine.network.packet.PacketSpawnPickupEntity;
import necesse.engine.network.packet.PacketSpawnProjectile;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.gameAreaSearch.EntityListsRegionSearch;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.world.ReturnedObjects;
import necesse.entity.AbstractDamageResult;
import necesse.entity.DamagedObjectEntity;
import necesse.entity.DrawOnMapEntity;
import necesse.entity.Entity;
import necesse.entity.ObjectDamageResult;
import necesse.entity.TileDamageResult;
import necesse.entity.chains.Chain;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.particle.ParticleOptions;
import necesse.entity.pickup.PickupEntity;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.MobChance;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.regionSystem.Region;

public class EntityManager {
   public static int MAX_PICKUP_ENTITIES = 1000;
   public final Level level;
   public final Object lock = new Object();
   public final EntityComponentManager<Integer> componentManager = new EntityComponentManager<>();
   public final EntityList<Mob> mobs;
   public final EntityRegionList<PlayerMob> players;
   public final EntityList<PickupEntity> pickups;
   public final EntityList<Projectile> projectiles;
   public final EntityComponentManager<Long> objectEntityComponentManager = new EntityComponentManager<>();
   public final TileEntityList<ObjectEntity> objectEntities;
   public final EntityList<Particle> particles;
   public final ParticleOptions particleOptions;
   public final TileEntityList<DamagedObjectEntity> damagedObjects;
   public final LevelEventsManager events;
   public final ArrayList<Chain> chains = new ArrayList<>();
   public final ArrayList<Trail> trails = new ArrayList<>();
   public final ArrayList<GroundPillarHandler<?>> pillarHandlers = new ArrayList<>();
   private LinkedList<DrawOnMapEntity> drawOnMap = new LinkedList<>();
   private float spawnRateMod;
   private float spawnCapMod;
   private float chaserDistanceMod;
   private Rectangle particlesAllowed = new Rectangle();
   public HashMap<Point, Mob> serverOpenedDoors = new HashMap<>();
   public final ClientSubmittedHits submittedHits;

   public EntityManager(Level level) {
      this.level = level;
      this.submittedHits = new ClientSubmittedHits(this);
      this.mobs = new EntityList<>(this, true, "mob", m -> {
         if (level.isServer() && m.shouldSendSpawnPacket()) {
            level.getServer().network.sendToClientsWithEntity(new PacketSpawnMob(m), m);
         }
      }, m -> {
         if (level.isServer()) {
            level.getServer().network.sendToClientsWithEntity(new PacketRemoveMob(m.getUniqueID()), m);
         }
      }, true);
      this.mobs.onHiddenAdded = mob -> level.<OnMobAddedListenerEntityComponent, Class<OnMobAddedListenerEntityComponent>>streamAll(
            OnMobAddedListenerEntityComponent.class
         )
         .forEach(listener -> listener.onMobSpawned(mob));
      this.players = new EntityRegionList<>(level);
      this.pickups = new EntityList<>(this, true, "pickup", p -> {
         if (level.isServer() && p.shouldSendSpawnPacket()) {
            level.getServer().network.sendToClientsWithEntity(new PacketSpawnPickupEntity(p), p);
         }
      }, p -> {
         if (level.isServer()) {
            level.getServer().network.sendToClientsWithEntity(new PacketRemovePickupEntity(p.getUniqueID()), p);
         }
      }, true);
      this.projectiles = new EntityList<>(this, true, "projectile", p -> {
         if (level.isServer() && p.shouldSendSpawnPacket()) {
            level.getServer().network.sendToClientsWithEntity(new PacketSpawnProjectile(p), p);
         }
      }, p -> {
         if (level.isServer() && p.sendRemovePacket) {
            level.getServer().network.sendToClientsWithEntity(new PacketRemoveProjectile(p.getUniqueID()), p);
         }
      }, true);
      this.projectiles.onHiddenAdded = p -> {
         if (level.isServer()) {
            this.submittedHits.submitNewProjectile(p);
         }
      };
      this.particles = new EntityList<>(this, false, "particle", null, null, true);
      this.particles.cacheTTL = 0;
      this.particleOptions = new ParticleOptions(level);
      this.objectEntities = new TileEntityList<>(this, this.objectEntityComponentManager, "objectEntity", null, null);
      this.objectEntities.cacheTTL = 0;
      this.damagedObjects = new TileEntityList<>(this, null, "damagedObject", null, null);
      this.damagedObjects.cacheTTL = 0;
      this.events = new LevelEventsManager(this);
      this.updateMods();
   }

   public void addChain(Chain chain) {
      synchronized (this.chains) {
         this.chains.add(chain);
      }
   }

   public void addTrail(Trail trail) {
      synchronized (this.trails) {
         this.trails.add(trail);
      }
   }

   public void changeMobLevel(Mob mob, Level newLevel, int newX, int newY, boolean mountFollow) {
      this.changeMobLevel(mob.getUniqueID(), newLevel, newX, newY, mountFollow);
   }

   public void changeMobLevel(int uniqueID, Level newLevel, int newX, int newY, boolean mountFollow) {
      Mob mob = this.mobs.get(uniqueID, false);
      if (mob != null) {
         Mob mount = mob.getMount();
         if ((mount == null || !mount.forceFollowRiderLevelChange(mob)) && (!mountFollow || mount == null)) {
            mob.dismount();
         } else {
            this.changeMobLevel(mount, newLevel, newX, newY, true);
         }

         this.mobs.onRemoved.accept(mob);
         this.mobs.map.remove(uniqueID);
         newLevel.entityManager.addMob(mob, newX, newY);
      } else {
         System.err
            .println("Tried to change level of invalid mob unique id " + uniqueID + " from " + this.level.getIdentifier() + " tp " + newLevel.getIdentifier());
      }
   }

   public void addMob(Mob mob, float x, float y) {
      mob.setPos(x, y, true);
      this.mobs.add(mob);
   }

   public void addParticle(Particle particle, Particle.GType gType) {
      this.addParticle(particle, false, gType);
   }

   public void addParticle(Particle particle, boolean overrideAllowed, Particle.GType gType) {
      if (!this.level.isServer()) {
         if (gType.canAdd(this.level)) {
            if (overrideAllowed || this.particlesAllowed.contains(particle.getX(), particle.getY())) {
               this.particles.add(particle);
            }
         }
      }
   }

   public ParticleOption addParticle(ParticleOption o, Particle.GType gType) {
      if (gType != null) {
         if (!gType.canAdd(this.level)) {
            o.remove();
            return o;
         }

         Float levelPos = o.getLevelPos();
         if (!this.particlesAllowed.contains(levelPos.x, levelPos.y)) {
            o.remove();
            return o;
         }
      }

      this.particleOptions.add(o);
      return o;
   }

   public ParticleOption addParticle(Supplier<Float> snapPos, float startXOffset, float startYOffset, Particle.GType gType) {
      return this.addParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType);
   }

   public ParticleOption addParticle(Entity snapPos, float startXOffset, float startYOffset, Particle.GType gType) {
      return this.addParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType);
   }

   public ParticleOption addParticle(Supplier<Float> snapPos, Particle.GType gType) {
      return this.addParticle(snapPos, 0.0F, 0.0F, gType);
   }

   public ParticleOption addParticle(Entity snapPos, Particle.GType gType) {
      return this.addParticle(snapPos, 0.0F, 0.0F, gType);
   }

   public ParticleOption addParticle(float x, float y, Particle.GType gType) {
      return this.addParticle(ParticleOption.standard(x, y), gType);
   }

   public ParticleOption addTopParticle(ParticleOption o, Particle.GType gType, int drawOrder) {
      if (gType != null) {
         if (!gType.canAdd(this.level)) {
            o.remove();
            return o;
         }

         Float levelPos = o.getLevelPos();
         if (!this.particlesAllowed.contains(levelPos.x, levelPos.y)) {
            o.remove();
            return o;
         }
      }

      this.particleOptions.addTop(o, drawOrder);
      return o;
   }

   public ParticleOption addTopParticle(Supplier<Float> snapPos, float startXOffset, float startYOffset, Particle.GType gType, int drawOrder) {
      return this.addTopParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType, drawOrder);
   }

   public ParticleOption addTopParticle(Entity snapPos, float startXOffset, float startYOffset, Particle.GType gType, int drawOrder) {
      return this.addTopParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType, drawOrder);
   }

   public ParticleOption addTopParticle(Supplier<Float> snapPos, Particle.GType gType, int drawOrder) {
      return this.addTopParticle(snapPos, 0.0F, 0.0F, gType, drawOrder);
   }

   public ParticleOption addTopParticle(Entity snapPos, Particle.GType gType, int drawOrder) {
      return this.addTopParticle(snapPos, 0.0F, 0.0F, gType, drawOrder);
   }

   public ParticleOption addTopParticle(float x, float y, Particle.GType gType, int drawOrder) {
      return this.addTopParticle(ParticleOption.standard(x, y), gType, drawOrder);
   }

   public ParticleOption addTopParticle(ParticleOption o, Particle.GType gType) {
      return this.addTopParticle(o, gType, 0);
   }

   public ParticleOption addTopParticle(Supplier<Float> snapPos, float startXOffset, float startYOffset, Particle.GType gType) {
      return this.addTopParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType);
   }

   public ParticleOption addTopParticle(Entity snapPos, float startXOffset, float startYOffset, Particle.GType gType) {
      return this.addTopParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType);
   }

   public ParticleOption addTopParticle(Supplier<Float> snapPos, Particle.GType gType) {
      return this.addTopParticle(snapPos, 0.0F, 0.0F, gType);
   }

   public ParticleOption addTopParticle(Entity snapPos, Particle.GType gType) {
      return this.addTopParticle(snapPos, 0.0F, 0.0F, gType);
   }

   public ParticleOption addTopParticle(float x, float y, Particle.GType gType) {
      return this.addTopParticle(ParticleOption.standard(x, y), gType);
   }

   public ParticleOption addTileParticle(ParticleOption o, Particle.GType gType, int drawOrder) {
      if (gType != null) {
         if (!gType.canAdd(this.level)) {
            o.remove();
            return o;
         }

         Float levelPos = o.getLevelPos();
         if (!this.particlesAllowed.contains(levelPos.x, levelPos.y)) {
            o.remove();
            return o;
         }
      }

      this.particleOptions.addTile(o, drawOrder);
      return o;
   }

   public ParticleOption addTileParticle(Supplier<Float> snapPos, float startXOffset, float startYOffset, Particle.GType gType, int drawOrder) {
      return this.addTileParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType, drawOrder);
   }

   public ParticleOption addTileParticle(Entity snapPos, float startXOffset, float startYOffset, Particle.GType gType, int drawOrder) {
      return this.addTileParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType, drawOrder);
   }

   public ParticleOption addTileParticle(Supplier<Float> snapPos, Particle.GType gType, int drawOrder) {
      return this.addTileParticle(snapPos, 0.0F, 0.0F, gType, drawOrder);
   }

   public ParticleOption addTileParticle(Entity snapPos, Particle.GType gType, int drawOrder) {
      return this.addTileParticle(snapPos, 0.0F, 0.0F, gType, drawOrder);
   }

   public ParticleOption addTileParticle(float x, float y, Particle.GType gType, int drawOrder) {
      return this.addTileParticle(ParticleOption.standard(x, y), gType, drawOrder);
   }

   public ParticleOption addTileParticle(ParticleOption o, Particle.GType gType) {
      return this.addTileParticle(o, gType, 0);
   }

   public ParticleOption addTileParticle(Supplier<Float> snapPos, float startXOffset, float startYOffset, Particle.GType gType) {
      return this.addTileParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType);
   }

   public ParticleOption addTileParticle(Entity snapPos, float startXOffset, float startYOffset, Particle.GType gType) {
      return this.addTileParticle(ParticleOption.standard(startXOffset, startYOffset).snapPosition(snapPos), gType);
   }

   public ParticleOption addTileParticle(Supplier<Float> snapPos, Particle.GType gType) {
      return this.addTileParticle(snapPos, 0.0F, 0.0F, gType);
   }

   public ParticleOption addTileParticle(Entity snapPos, Particle.GType gType) {
      return this.addTileParticle(snapPos, 0.0F, 0.0F, gType);
   }

   public ParticleOption addTileParticle(float x, float y, Particle.GType gType) {
      return this.addTileParticle(ParticleOption.standard(x, y), gType);
   }

   public void updateParticlesAllowed(GameCamera camera) {
      this.particlesAllowed = new Rectangle(camera.getX() - 50, camera.getY() - 50, camera.getWidth() + 100, camera.getHeight() + 100);
   }

   public boolean isParticlesAllowed(float x, float y) {
      return this.particlesAllowed.contains(x, y);
   }

   public ObjectEntity getObjectEntity(int tileX, int tileY) {
      return this.objectEntities.get(tileX, tileY, false);
   }

   public <T extends ObjectEntity> T getObjectEntity(int tileX, int tileY, Class<T> expectedClass) {
      ObjectEntity objectEntity = this.getObjectEntity(tileX, tileY);
      return objectEntity != null && expectedClass.isAssignableFrom(objectEntity.getClass()) ? expectedClass.cast(objectEntity) : null;
   }

   public void removeObjectEntity(int tileX, int tileY) {
      ObjectEntity entity = this.getObjectEntity(tileX, tileY);
      if (entity != null) {
         entity.remove();
      }
   }

   public DamagedObjectEntity getDamagedObjectEntity(int tileX, int tileY) {
      return this.damagedObjects.get(tileX, tileY, false);
   }

   @Deprecated
   public void addLevelEvent(LevelEvent event) {
      this.events.add(event);
   }

   @Deprecated
   public void addLevelEventHidden(LevelEvent event) {
      this.events.addHidden(event);
   }

   @Deprecated
   public LevelEvent getLevelEvent(int uniqueID, boolean searchCache) {
      return this.events.get(uniqueID, searchCache);
   }

   public void addPillarHandler(GroundPillarHandler<?> handler) {
      synchronized (this.pillarHandlers) {
         handler.level = this.level;
         this.pillarHandlers.add(handler);
      }
   }

   public <T extends Entity> GameAreaStream<T> streamArea(float x, float y, int range, EntityRegionList<? extends T>... entityLists) {
      return this.streamAreaTileRange((int)x, (int)y, range / 32 + 1, entityLists);
   }

   public <T extends Entity> GameAreaStream<T> streamAreaTileRange(int x, int y, int tileRange, EntityRegionList<? extends T>... entityLists) {
      return new EntityListsRegionSearch<>(this.level, x, y, tileRange, entityLists).streamEach();
   }

   public GameAreaStream<Mob> streamAreaMobsAndPlayers(float x, float y, int range) {
      return this.streamAreaMobsAndPlayersTileRange((int)x, (int)y, range / 32 + 1);
   }

   public GameAreaStream<Mob> streamAreaMobsAndPlayersTileRange(int x, int y, int tileRange) {
      return new EntityListsRegionSearch<Mob>(this.level, x, y, tileRange, this.mobs.regionList, this.players).streamEach();
   }

   public Stream<Mob> streamMobsAndPlayers(float x, float y, int range) {
      return Stream.concat(this.mobs.streamInRegionsInRange(x, y, range), this.players.streamInRegionsInRange(x, y, range));
   }

   public Stream<Mob> streamMobsAndPlayersTileRange(int x, int y, int tileRange) {
      return Stream.concat(this.mobs.streamInRegionsInTileRange(x, y, tileRange), this.players.streamInRegionsInTileRange(x, y, tileRange));
   }

   public GameLinkedList<Entity> getRegionDrawEntities(int regionX, int regionY) {
      GameLinkedList<Entity> out = new GameLinkedList<>();
      synchronized (this.lock) {
         out.addAll(this.mobs.getInRegion(regionX, regionY));
         out.addAll(this.players.getInRegion(regionX, regionY));
         out.addAll(this.pickups.getInRegion(regionX, regionY));
         out.addAll(this.projectiles.getInRegion(regionX, regionY));
         out.addAll(this.particles.getInRegion(regionX, regionY));
         return out;
      }
   }

   public void refreshSetLevel() {
      synchronized (this.lock) {
         this.mobs.stream().forEach(e -> e.setLevel(this.level));
         this.pickups.stream().forEach(e -> e.setLevel(this.level));
         this.projectiles.stream().forEach(e -> e.setLevel(this.level));
         this.objectEntities.stream().forEach(e -> e.setLevel(this.level));
         this.particles.stream().forEach(e -> e.setLevel(this.level));
         this.damagedObjects.stream().forEach(e -> e.setLevel(this.level));
      }
   }

   public LinkedList<DrawOnMapEntity> getEntityDrawOnMap() {
      return this.drawOnMap;
   }

   public void frameTick(TickManager tickManager) {
      this.mobs.frameTick(tickManager, Mob::tickMovement);
      this.pickups.frameTick(tickManager, PickupEntity::tickMovement);
      this.projectiles.frameTick(tickManager, Projectile::tickMovement);
      this.particles.frameTick(tickManager, Particle::tickMovement);
      float delta = tickManager.getDelta();
      this.particleOptions.tickMovement(delta);
      Performance.record(this.level.tickManager(), "movement", () -> Performance.record(this.level.tickManager(), "events", () -> {
         synchronized (this.lock) {
            for (LevelEvent event : this.events) {
               if (event != null && !event.isOver()) {
                  event.tickMovement(delta);
               }
            }
         }
      }));
      this.objectEntities.frameTick(tickManager, ObjectEntity::frameTick);
   }

   public void clientTick() {
      LinkedList<DrawOnMapEntity> drawOnMap = new LinkedList<>();
      Performance.record(this.level.tickManager(), "mobs", () -> this.mobs.clientTick(Mob::clientTick, drawOnMap));
      Performance.record(this.level.tickManager(), "pickups", () -> this.pickups.clientTick(PickupEntity::clientTick, drawOnMap));
      Performance.record(this.level.tickManager(), "projectiles", () -> this.projectiles.clientTick(Projectile::clientTick, drawOnMap));
      Performance.record(this.level.tickManager(), "particles", () -> this.particles.clientTick(Particle::clientTick, drawOnMap));
      Performance.record(this.level.tickManager(), "particleOpts", this.particleOptions::tick);
      Performance.record(this.level.tickManager(), "chains", () -> {
         synchronized (this.chains) {
            for (int i = 0; i < this.chains.size(); i++) {
               if (this.chains.get(i).isRemoved()) {
                  this.chains.remove(i);
                  i--;
               }
            }
         }
      });
      Performance.record(this.level.tickManager(), "trails", () -> {
         synchronized (this.trails) {
            for (int i = 0; i < this.trails.size(); i++) {
               Trail trail = this.trails.get(i);
               trail.tick();
               if (trail.isRemoved()) {
                  this.trails.remove(i);
                  i--;
               }
            }
         }
      });
      Performance.record(this.level.tickManager(), "pillars", () -> {
         synchronized (this.pillarHandlers) {
            for (int i = 0; i < this.pillarHandlers.size(); i++) {
               if (this.pillarHandlers.get(i).tickAndShouldRemove()) {
                  this.pillarHandlers.remove(i);
                  i--;
               }
            }
         }
      });
      Performance.record(this.level.tickManager(), "levelEvents", this.events::clientTick);
      Performance.record(this.level.tickManager(), "damageEntities", () -> this.damagedObjects.clientTick(DamagedObjectEntity::clientTick, drawOnMap));
      Performance.record(this.level.tickManager(), "objectEntities", () -> this.objectEntities.clientTick(ObjectEntity::clientTick, drawOnMap));
      this.drawOnMap = drawOnMap;
   }

   public void serverTick() {
      LinkedList<DrawOnMapEntity> drawOnMap = new LinkedList<>();
      this.submittedHits.tick();
      Performance.record(this.level.tickManager(), "mobs", () -> this.mobs.serverTick(mob -> {
         if (this.level.tickManager().getTick() == 1 && GameRandom.globalRandom.nextFloat() < mob.getDespawnOdds() && mob.canDespawn()) {
            mob.remove();
         } else {
            mob.serverTick();
            if (mob.isDirty()) {
               if (mob.shouldSendSpawnPacket()) {
                  this.level.getServer().network.sendToClientsWithEntity(new PacketSpawnMob(mob), mob);
               }

               mob.markClean();
            }
         }
      }, drawOnMap));
      Performance.record(this.level.tickManager(), "pickups", () -> {
         if (this.pickups.count() > MAX_PICKUP_ENTITIES) {
            int toRemove = this.pickups.count() - MAX_PICKUP_ENTITIES;
            this.pickups.stream().sorted(Comparator.comparingLong(p -> p.spawnTime)).limit(toRemove).forEach(Entity::remove);
         }

         this.pickups.serverTick(pickup -> {
            pickup.serverTick();
            if (pickup.isDirty()) {
               if (this.level.isServer()) {
                  this.level.getServer().network.sendToClientsWithEntity(new PacketSpawnPickupEntity(pickup), pickup);
               }

               pickup.markClean();
            }
         }, drawOnMap);
      });
      Performance.record(this.level.tickManager(), "projectiles", () -> this.projectiles.serverTick(Projectile::serverTick, drawOnMap));
      Performance.record(this.level.tickManager(), "particles", () -> this.particles.serverTick(Particle::serverTick, drawOnMap));
      Performance.record(this.level.tickManager(), "chains", () -> {
         synchronized (this.chains) {
            for (int i = 0; i < this.chains.size(); i++) {
               if (this.chains.get(i).isRemoved()) {
                  this.chains.remove(i);
                  i--;
               }
            }
         }
      });
      Performance.record(this.level.tickManager(), "trails", () -> {
         synchronized (this.trails) {
            for (int i = 0; i < this.trails.size(); i++) {
               if (this.trails.get(i).isRemoved()) {
                  this.trails.remove(i);
                  i--;
               }
            }
         }
      });
      Performance.record(this.level.tickManager(), "pillars", () -> {
         synchronized (this.pillarHandlers) {
            for (int i = 0; i < this.pillarHandlers.size(); i++) {
               if (this.pillarHandlers.get(i).tickAndShouldRemove()) {
                  this.pillarHandlers.remove(i);
                  i--;
               }
            }
         }
      });
      Performance.record(this.level.tickManager(), "levelEvents", this.events::serverTick);
      Performance.record(this.level.tickManager(), "damageEntities", () -> this.damagedObjects.serverTick(DamagedObjectEntity::serverTick, drawOnMap));
      Performance.record(this.level.tickManager(), "objectEntities", () -> this.objectEntities.serverTick(oe -> {
         oe.serverTick();
         if (oe.isDirty()) {
            if (this.level.isServer()) {
               this.level.getServer().network.sendToClientsWithTile(new PacketObjectEntity(oe), this.level, oe.tileX, oe.tileY);
            }

            oe.markClean();
         }
      }, drawOnMap));
      this.drawOnMap = drawOnMap;
   }

   public int countMobs(int levelX, int levelY, MobSpawnArea area, Predicate<Mob> filter) {
      return (int)this.mobs.streamInRegionsShape(GameUtils.rangeBounds(levelX, levelY, area.maxSpawnDistance + 320), 0).filter(filter).count();
   }

   public int countPlayers(int levelX, int levelY, MobSpawnArea area) {
      return (int)this.players.streamInRegionsInRange(levelX, levelY, (int)(area.maxSpawnDistance * 1.75F)).filter(p -> !p.removed()).count();
   }

   public boolean tickMobSpawning(Server server, ServerClient client) {
      return server.world.settings.disableMobSpawns
         ? true
         : Performance.record(
            this.level.tickManager(),
            "mobSpawning",
            () -> {
               int hostileMobs = this.countMobs(client.playerMob.getX(), client.playerMob.getY(), Mob.MOB_SPAWN_AREA, m -> m.isHostile && m.canDespawn);
               if (hostileMobs < client.getMobSpawnCap(this.level)) {
                  Point spawnTile = getMobSpawnTile(
                     this.level,
                     client.playerMob.getX(),
                     client.playerMob.getY(),
                     Mob.MOB_SPAWN_AREA,
                     tile -> this.level.isTileWithinBounds(tile.x, tile.y) && !this.level.isSolidTile(tile.x, tile.y)
                        ? this.level.getTile(tile.x, tile.y).getMobSpawnPositionTickets(this.level, tile.x, tile.y)
                        : 0
                  );
                  if (spawnTile != null) {
                     MobSpawnTable mobSpawnTable = client.getMobSpawnTable(this.level, spawnTile.x, spawnTile.y);
                     return this.spawnRandomMob(server, client, pos -> pos.tile().tile.getMobSpawnTable(pos, mobSpawnTable), spawnTile);
                  }
               }

               return true;
            }
         );
   }

   public boolean tickCritterSpawning(Server server, ServerClient client) {
      return Performance.record(
         this.level.tickManager(),
         "critterSpawning",
         () -> {
            Point spawnTile = getMobSpawnTile(
               this.level,
               client.playerMob.getX(),
               client.playerMob.getY(),
               Mob.CRITTER_SPAWN_AREA,
               tile -> this.level.isTileWithinBounds(tile.x, tile.y) && !this.level.isSolidTile(tile.x, tile.y) ? 100 : 0
            );
            if (spawnTile != null) {
               int critterMobs = this.countMobs(client.playerMob.getX(), client.playerMob.getY(), Mob.CRITTER_SPAWN_AREA, m -> m.isCritter && m.canDespawn);
               if (critterMobs < client.getCritterSpawnCap(this.level, spawnTile.x, spawnTile.y)) {
                  MobSpawnTable critterSpawnTable = client.getCritterSpawnTable(this.level, spawnTile.x, spawnTile.y);
                  return this.spawnRandomMob(server, client, critterSpawnTable, spawnTile);
               }
            }

            return true;
         }
      );
   }

   private boolean spawnRandomMob(Server server, ServerClient client, Function<TilePosition, MobSpawnTable> spawnTableGetter, Point spawnTile) {
      if (spawnTile != null) {
         MobSpawnTable spawnTable = spawnTableGetter.apply(new TilePosition(this.level, spawnTile.x, spawnTile.y));
         return this.spawnRandomMob(server, client, spawnTable, spawnTile);
      } else {
         return false;
      }
   }

   private boolean spawnRandomMob(Server server, ServerClient client, MobSpawnTable spawnTable, Point spawnTile) {
      if (spawnTile != null && spawnTable != null) {
         while (true) {
            MobChance randomMob = spawnTable.getRandomMob(this.level, client, spawnTile, GameRandom.globalRandom, "mobspawning");
            if (randomMob == null) {
               return false;
            }

            Collection<Mob> spawned = randomMob.spawnMob(this.level, client, spawnTile, null, this.level::onMobSpawned, "mobspawning");
            if (spawned != null) {
               return true;
            }

            spawnTable = spawnTable.withoutRandomMob(randomMob);
         }
      } else {
         return false;
      }
   }

   public static Point getMobSpawnTile(Level level, int centerLevelX, int centerLevelY, MobSpawnArea spawnArea, Function<Point, Integer> ticketsGetter) {
      Point spawnPos = Performance.record(
         level.tickManager(),
         "getSpawnPos",
         () -> ticketsGetter != null
            ? spawnArea.getRandomTicketTile(
               GameRandom.globalRandom, GameMath.getTileCoordinate(centerLevelX), GameMath.getTileCoordinate(centerLevelY), ticketsGetter
            )
            : spawnArea.getRandomTile(GameRandom.globalRandom, GameMath.getTileCoordinate(centerLevelX), GameMath.getTileCoordinate(centerLevelY))
      );
      if (spawnPos != null) {
         if (!level.isTileWithinBounds(spawnPos.x, spawnPos.y)) {
            return null;
         }

         int spawnLevelPosX = spawnPos.x * 32 + 16;
         int spawnLevelPosY = spawnPos.y * 32 + 16;
         if (level.entityManager
            .players
            .streamArea(spawnLevelPosX, spawnLevelPosY, spawnArea.minSpawnDistance)
            .anyMatch(p -> p.getDistance(spawnLevelPosX, spawnLevelPosY) < spawnArea.minSpawnDistance)) {
            return null;
         }
      }

      return spawnPos;
   }

   public static Point getMobSpawnTile(Level level, int centerX, int centerY, int minDistance, int maxDistance) {
      double t = GameRandom.globalRandom.nextDouble() * Math.PI * 2.0;
      double a = 2.0 / (Math.pow(maxDistance, 2.0) - Math.pow(minDistance, 2.0));
      double r = Math.sqrt(2.0 * GameRandom.globalRandom.nextDouble() / a + Math.pow(minDistance, 2.0));
      double vX = r * Math.cos(t) + centerX;
      double vY = r * Math.sin(t) + centerY;
      int tileX = GameMath.getTileCoordinate(vX);
      int tileY = GameMath.getTileCoordinate(vY);
      return GameUtils.streamServerClients(level).anyMatch(c -> c.playerMob.getDistance(tileX * 32 + 16, tileY * 32 + 16) <= minDistance)
         ? null
         : new Point(tileX, tileY);
   }

   public int getSize() {
      return this.mobs.count()
         + this.pickups.count()
         + this.damagedObjects.count()
         + this.projectiles.count()
         + this.particles.count()
         + this.objectEntities.count();
   }

   public TileDamageResult doTileDamageOverride(int tileX, int tileY, int damage) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY).doTileDamageOverride(damage);
   }

   public ObjectDamageResult doObjectDamageOverride(int objectLayerID, int tileX, int tileY, int damage, boolean checkDamageNow) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY).doObjectDamageOverride(objectLayerID, damage, checkDamageNow);
   }

   public ObjectDamageResult doObjectDamageOverride(int objectLayerID, int tileX, int tileY, int damage) {
      return this.doObjectDamageOverride(objectLayerID, tileX, tileY, damage, true);
   }

   public ObjectDamageResult destroyObjectOverride(int objectLayerID, int tileX, int tileY, boolean checkDamageNow) {
      GameObject object = this.level.getObject(objectLayerID, tileX, tileY);
      return object.getID() != 0 ? this.doObjectDamageOverride(objectLayerID, tileX, tileY, object.objectHealth, checkDamageNow) : null;
   }

   public ObjectDamageResult destroyObjectOverride(int objectLayerID, int tileX, int tileY) {
      return this.destroyObjectOverride(objectLayerID, tileX, tileY, true);
   }

   public TileDamageResult doTileDamage(int tileX, int tileY, int damage, float toolTier, Attacker attacker, ServerClient client) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY).doTileDamage(damage, toolTier, attacker, client);
   }

   public TileDamageResult doTileDamage(
      int tileX, int tileY, int damage, float toolTier, Attacker attacker, ServerClient client, boolean showEffects, int mouseX, int mouseY
   ) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY).doTileDamage(damage, toolTier, attacker, client, showEffects, mouseX, mouseY);
   }

   public ObjectDamageResult doObjectDamage(int objectLayerID, int tileX, int tileY, int damage, float toolTier, Attacker attacker, ServerClient client) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY).doObjectDamage(objectLayerID, damage, toolTier, attacker, client);
   }

   public ObjectDamageResult doObjectDamage(
      int objectLayerID, int tileX, int tileY, int damage, float toolTier, Attacker attacker, ServerClient client, boolean showEffects, int mouseX, int mouseY
   ) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY).doObjectDamage(objectLayerID, damage, toolTier, attacker, client, showEffects, mouseX, mouseY);
   }

   public AbstractDamageResult doToolDamage(
      int priorityObjectLayerID,
      int tileX,
      int tileY,
      int damage,
      ToolType toolType,
      float toolTier,
      Attacker attacker,
      ServerClient client,
      boolean showEffects,
      int mouseX,
      int mouseY
   ) {
      return this.getOrCreateDamagedObjectEntity(tileX, tileY)
         .doToolDamage(priorityObjectLayerID, damage, toolType, toolTier, attacker, client, showEffects, mouseX, mouseY);
   }

   public DamagedObjectEntity getOrCreateDamagedObjectEntity(int tileX, int tileY) {
      DamagedObjectEntity damagedObject = this.getDamagedObjectEntity(tileX, tileY);
      if (damagedObject == null) {
         damagedObject = new DamagedObjectEntity(this.level, tileX, tileY);
      }

      this.damagedObjects.add(damagedObject);
      return damagedObject;
   }

   public boolean uniqueIDOccupied(int id) {
      if (this.mobs.get(id, false) != null) {
         return true;
      } else if (this.pickups.get(id, false) != null) {
         return true;
      } else {
         return this.projectiles.get(id, false) != null ? true : this.events.hasUniqueID(id);
      }
   }

   public void updateMods() {
      this.spawnRateMod = 1.0F;
      this.spawnCapMod = 1.0F;
      this.chaserDistanceMod = 1.0F;
   }

   public float getChaserDistanceMod() {
      return this.chaserDistanceMod;
   }

   public float getSpawnRate(int tileX, int tileY) {
      return this.level.getBiome(tileX, tileY).getSpawnRateMod(this.level) * this.spawnRateMod;
   }

   public float getSpawnCapMod(int tileX, int tileY) {
      return this.level.getBiome(tileX, tileY).getSpawnCapMod(this.level) * this.spawnCapMod;
   }

   public static float getSpawnCap(int playerCount, float multiplier, float addition) {
      return (float)(Math.pow(playerCount, 0.33333334F) * multiplier + addition);
   }

   public void onServerClientLoadedRegion(Region region, ServerClient client) {
      for (Mob mob : this.level.entityManager.mobs.getInRegion(region.regionX, region.regionY)) {
         if (mob.shouldSendSpawnPacket()) {
            client.sendPacket(new PacketSpawnMob(mob));
         }
      }

      for (PickupEntity pickup : this.level.entityManager.pickups.getInRegion(region.regionX, region.regionY)) {
         if (pickup.shouldSendSpawnPacket()) {
            client.sendPacket(new PacketSpawnPickupEntity(pickup));
         }
      }

      for (Projectile projectile : this.level.entityManager.projectiles.getInRegion(region.regionX, region.regionY)) {
         if (projectile.shouldSendSpawnPacket()) {
            client.sendPacket(new PacketSpawnProjectile(projectile));
         }
      }

      for (DamagedObjectEntity damagedTile : this.level.entityManager.damagedObjects.getInRegion(region.regionX, region.regionY)) {
         client.sendPacket(new PacketDamagedTileEntity(damagedTile));
      }
   }

   public void addReturnedEntities(ReturnedObjects returnedObjects) {
      for (Mob mob : this.mobs) {
         if (mob.shouldAddToDeletedLevelReturnedMobs()) {
            returnedObjects.mobs.add(mob);
         }
      }

      for (PickupEntity pickup : this.pickups) {
         if (pickup.shouldAddToDeletedLevelReturnedPickups()) {
            returnedObjects.pickups.add(pickup);
         }
      }
   }

   public void onLoadingComplete() {
      this.mobs.onLoadingComplete();
      this.pickups.onLoadingComplete();
      this.projectiles.onLoadingComplete();
      this.objectEntities.onLoadingComplete();
      this.particles.onLoadingComplete();
      this.damagedObjects.onLoadingComplete();
      this.events.onLoadingComplete();
   }

   public void onUnloading() {
      this.mobs.onUnloading();
      this.pickups.onUnloading();
      this.projectiles.onUnloading();
      this.objectEntities.onUnloading();
      this.particles.onUnloading();
      this.damagedObjects.onUnloading();
      this.events.onUnloading();
   }

   public void dispose() {
      this.mobs.dispose();
      this.pickups.dispose();
      this.projectiles.dispose();
      this.objectEntities.dispose();
      this.particles.dispose();
      this.damagedObjects.dispose();
      this.events.dispose();
   }
}
