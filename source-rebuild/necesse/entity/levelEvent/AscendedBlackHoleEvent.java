package necesse.entity.levelEvent;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.function.Function;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.PausableSound;
import necesse.engine.sound.SoundEffect;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.TeleportResult;
import necesse.engine.util.TicketSystemList;
import necesse.entity.Entity;
import necesse.entity.levelEvent.actions.LevelEventAction;
import necesse.entity.mobs.hostile.bosses.ArenaEntrancePortalMob;
import necesse.entity.mobs.hostile.bosses.ascendedWizard.AscendedWizardMob;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.presets.AscendedArenaPreset;
import necesse.level.maps.presets.Preset;
import necesse.level.maps.regionSystem.Region;

public class AscendedBlackHoleEvent extends LevelEvent {
   public Preset arenaPreset = new AscendedArenaPreset();
   public float inwardSpeed = 20.0F;
   public float circleSpeed = 50.0F;
   public float radiusToStopCounteractMoveSpeed = 500.0F;
   public float radiusToIncreaseInwardSpeed = 800.0F;
   public float inwardSpeedIncreasePerDistance = 0.05F;
   public float maxPlayerDistanceAtEnd = 500.0F;
   protected int tileX;
   protected int tileY;
   protected long spawnTime;
   protected long endTime;
   protected int totalArenaTiles;
   protected LinkedList<AscendedBlackHoleEvent.ArenaTilePlace> missingArenaTiles;
   protected float nextTileBuffer;
   protected final AscendedBlackHoleEvent.PlaceArenaLevelEventAction placeArenaAction;
   protected PausableSound blackHoleSound;
   public AscendedWizardMob wizardMob;
   protected TicketSystemList<Integer> tileIDs = new TicketSystemList<>();

   public AscendedBlackHoleEvent() {
      this.tileIDs.addObject(100, TileRegistry.ascendedGrowthID);
      this.tileIDs.addObject(50, TileRegistry.ascendedCorruptionID);
      this.placeArenaAction = this.registerAction(new AscendedBlackHoleEvent.PlaceArenaLevelEventAction());
   }

   public AscendedBlackHoleEvent(int tileX, int tileY, long endTime) {
      this();
      this.tileX = tileX;
      this.tileY = tileY;
      this.endTime = endTime;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void onUnloading(Region region) {
      super.onUnloading(region);
      if (!this.isClient()) {
         this.doFinalPresetPlace();
      }
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextLong(this.spawnTime);
      writer.putNextLong(this.endTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.spawnTime = reader.getNextLong();
      this.endTime = reader.getNextLong();
   }

   @Override
   public void init() {
      super.init();
      if (this.spawnTime == 0L) {
         this.spawnTime = this.getTime();
      }

      if (!this.isClient()) {
         if (this.wizardMob == null) {
            this.wizardMob = new AscendedWizardMob();
            this.wizardMob.setLevel(this.level);
            this.wizardMob.onSpawned(this.tileX * 32 + 16, (this.tileY - 4) * 32 + 16);
            this.wizardMob.setSpawnTilePosition(this.tileX, this.tileY);
            this.wizardMob.startBlackHolePhase();
            this.level.entityManager.mobs.add(this.wizardMob);
         }

         this.missingArenaTiles = new LinkedList<>();

         for (int presetX = 0; presetX < this.arenaPreset.width; presetX++) {
            int tileX = this.tileX + presetX - this.arenaPreset.width / 2;

            for (int presetY = 0; presetY < this.arenaPreset.height; presetY++) {
               int tileY = this.tileY + presetY - this.arenaPreset.height / 2;
               int tileID = this.arenaPreset.getTile(presetX, presetY);
               int objectID = this.arenaPreset.getObject(presetX, presetY);
               if (tileID != -1 || objectID != -1) {
                  this.missingArenaTiles.add(new AscendedBlackHoleEvent.ArenaTilePlace(tileX, tileY, tileID, objectID));
               }
            }
         }

         this.totalArenaTiles = this.missingArenaTiles.size();
         Collections.shuffle(this.missingArenaTiles, GameRandom.globalRandom);
      }

      if (!this.isServer()) {
         this.blackHoleSound = new PausableSound(GameResources.blackholeBegin, SoundEffect.globalEffect().volume(2.0F));
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      long timeSinceSpawned = this.getTime() - this.spawnTime;
      float force = Math.max(timeSinceSpawned <= 1000L ? (float)timeSinceSpawned / 1000.0F : 1.0F, 0.0F);
      int centerX = this.tileX * 32 + 16;
      int centerY = this.tileY * 32 + 16;
      GameUtils.streamNetworkClients(this.getLevel())
         .filter(c -> !c.isDead())
         .map(c -> c.playerMob)
         .filter(p -> p != null && !p.removed())
         .forEach(
            p -> {
               float centerDistance = this.getDistanceToCenter(p.x, p.y);
               Float centerDir = GameMath.normalize(centerX - p.x, centerY - p.y);
               float centerAngle = GameMath.getAngle(centerDir);
               float currentSpeed;
               if (p.dx == 0.0F && p.dy == 0.0F) {
                  currentSpeed = 0.0F;
               } else {
                  currentSpeed = (float)Math.sqrt(p.dx * p.dx + p.dy * p.dy);
                  Float currentSpeedDir = GameMath.normalize(p.dx, p.dy);
                  float speedAngle = GameMath.getAngle(currentSpeedDir);
                  float angleDifference = Math.abs(GameMath.getAngleDifference(speedAngle, centerAngle));
                  if (angleDifference > 90.0F) {
                     if (centerDistance < this.radiusToStopCounteractMoveSpeed) {
                        float differenceFactor = (angleDifference - 90.0F) / 90.0F;
                        currentSpeed *= differenceFactor;
                     }
                  } else {
                     currentSpeed = 0.0F;
                  }
               }

               float inwardSpeed = this.getInwardSpeed(centerDistance, this.maxPlayerDistanceAtEnd) + currentSpeed;
               Float perpDir = GameMath.getPerpendicularDir(centerDir.x, centerDir.y);
               float inwardMove = inwardSpeed * force * delta / 250.0F;
               float circleMove = this.circleSpeed * force * delta / 250.0F;
               p.x = p.x + perpDir.x * circleMove;
               p.y = p.y + perpDir.y * circleMove;
               p.x = p.x + centerDir.x * inwardMove;
               p.y = p.y + centerDir.y * inwardMove;
               if (this.isServer()) {
                  ArrayList<LevelObjectHit> collisions = this.getLevel().getCollisions(p.getCollision(), p.getLevelCollisionFilter());
                  PointHashSet damagedTiles = new PointHashSet();

                  for (LevelObjectHit collision : collisions) {
                     if (!damagedTiles.contains(collision.tileX, collision.tileY) && !this.level.isProtected(collision.tileX, collision.tileY)) {
                        GameObject object = collision.getObject();
                        if (object.toolType != ToolType.UNBREAKABLE) {
                           int var10009 = collision.tileX * 32 + 16;
                           int var10010 = collision.tileY * 32;
                           this.getLevel()
                              .entityManager
                              .doObjectDamage(
                                 0, collision.tileX, collision.tileY, object.objectHealth, java.lang.Float.MAX_VALUE, null, null, true, var10009, var10010 + 16
                              );
                        }

                        damagedTiles.add(collision.tileX, collision.tileY);
                     }
                  }
               }
            }
         );
   }

   @Override
   public void clientTick() {
      super.clientTick();
      long timeToEnd = this.endTime - this.getTime();
      if (timeToEnd <= 0L) {
         this.over();
      } else {
         if (this.blackHoleSound != null) {
            this.blackHoleSound = this.blackHoleSound.gameTick();
         }

         GameCamera camera = GlobalData.getCurrentState().getCamera();
         float particlesDivider = 1.0F;
         switch (Settings.particles) {
            case Minimal:
               particlesDivider = 10.0F;
               break;
            case Decreased:
               particlesDivider = 4.0F;
         }

         double particlesPerTick = camera.getWidth() * camera.getHeight() / 32.0 / 10000.0 / particlesDivider;

         while (particlesPerTick > 0.0 && (!(particlesPerTick < 1.0) || GameRandom.globalRandom.getChance(particlesPerTick))) {
            particlesPerTick--;
            int particleHeight = 16;
            final int centerX = this.tileX * 32 + 16;
            final int centerY = this.tileY * 32 + 16 + particleHeight;
            int tileX = GameRandom.globalRandom.getIntBetween(camera.getStartTileX() - 5, camera.getEndTileX() + 5);
            int tileY = GameRandom.globalRandom.getIntBetween(camera.getStartTileY() - 5, camera.getEndTileY() + 5);
            int posX = tileX * 32 + GameRandom.globalRandom.nextInt(32);
            int posY = tileY * 32 + GameRandom.globalRandom.nextInt(32);
            int lifeTime = GameRandom.globalRandom.getIntBetween(4000, 6000);
            float distanceToCenter = GameMath.getExactDistance(posX, posY, centerX, centerY);
            float timeToCenter = Entity.getTravelTimeMillis(this.inwardSpeed, distanceToCenter);
            lifeTime = Math.min(lifeTime, (int)(timeToCenter + 500.0F));
            if (lifeTime > timeToEnd) {
               lifeTime = (int)timeToEnd;
            }

            this.getLevel()
               .entityManager
               .addParticle(posX, posY, null)
               .moves(
                  new ParticleOption.Mover() {
                     @Override
                     public void tick(Float pos, float delta, int lifeTimex, int timeAlive, float lifePercent) {
                        Float centerDir = GameMath.normalize(centerX - pos.x, centerY - pos.y);
                        Float perpDir = GameMath.getPerpendicularDir(centerDir.x, centerDir.y);
                        float inwardMove = AscendedBlackHoleEvent.this.getInwardSpeed(AscendedBlackHoleEvent.this.getDistanceToCenter(pos.x, pos.y), 0.0F)
                           * delta
                           / 250.0F;
                        float circleMove = AscendedBlackHoleEvent.this.circleSpeed * delta / 250.0F;
                        pos.x = pos.x + perpDir.x * circleMove;
                        pos.y = pos.y + perpDir.y * circleMove;
                        pos.x = pos.x + centerDir.x * inwardMove;
                        pos.y = pos.y + centerDir.y * inwardMove;
                     }
                  }
               )
               .sizeFadesInAndOut(10, 20, 500, 500)
               .fadesAlphaTime(500, 500)
               .colorRandom(300.0F, 0.9F, 0.9F, 5.0F, 0.1F, 0.1F)
               .minDrawLight(100)
               .height(particleHeight)
               .lifeTime(lifeTime);
         }
      }
   }

   public float getDistanceToCenter(float posX, float posY) {
      float centerX = this.tileX * 32 + 16;
      float centerY = this.tileY * 32 + 16;
      return GameMath.getExactDistance(posX, posY, centerX, centerY);
   }

   public float getInwardSpeed(float distanceToCenter, float minDistance) {
      if (distanceToCenter >= this.radiusToIncreaseInwardSpeed) {
         return this.inwardSpeed + (distanceToCenter - this.radiusToIncreaseInwardSpeed) * this.inwardSpeedIncreasePerDistance;
      } else if (distanceToCenter <= minDistance) {
         float factor = distanceToCenter / minDistance;
         return this.inwardSpeed * GameMath.lerp(factor, -1.0F, 1.0F);
      } else {
         return this.inwardSpeed;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickNextTiles(false);
      if (this.getTime() >= this.endTime) {
         int centerX = this.tileX * 32 + 16;
         int centerY = this.tileY * 32 + 16;
         GameUtils.streamNetworkClients(this.getLevel()).filter(c -> !c.isDead() && c.hasSpawned()).map(NetworkClient::getServerClient).forEach(c -> {
            float distanceToCenter = GameMath.getExactDistance(c.playerMob.x, c.playerMob.y, centerX, centerY);
            if (!(distanceToCenter <= this.maxPlayerDistanceAtEnd)) {
               final Float centerDir = GameMath.normalize(c.playerMob.x - centerX, c.playerMob.y - centerY);
               TeleportEvent event = new TeleportEvent(c, 0, this.getLevel().getIdentifier(), 0.0F, null, new Function<Level, TeleportResult>() {
                  public TeleportResult apply(Level newLevel) {
                     float distance = AscendedBlackHoleEvent.this.maxPlayerDistanceAtEnd * GameRandom.globalRandom.getFloatBetween(0.5F, 1.0F);
                     float targetX = centerX + centerDir.x * distance;
                     float targetY = centerY + centerDir.y * distance;
                     return new TeleportResult(true, new Point((int)targetX, (int)targetY));
                  }
               });
               this.getLevel().entityManager.events.add(event);
            }
         });
         this.over();
         this.tickNextTiles(true);
      }
   }

   protected void tickNextTiles(boolean allRemaining) {
      long totalTime = this.endTime - this.spawnTime;
      float tilesPerSecond = this.totalArenaTiles / ((float)totalTime / 1000.0F);
      this.nextTileBuffer += tilesPerSecond / 20.0F;

      while (this.nextTileBuffer >= 1.0F || allRemaining) {
         this.nextTileBuffer--;
         if (this.missingArenaTiles.isEmpty()) {
            break;
         }

         AscendedBlackHoleEvent.ArenaTilePlace tile = this.missingArenaTiles.remove(0);
         this.placeArenaAction.runAndSend(tile.tileX, tile.tileY, tile.tileID, tile.objectID);
      }
   }

   protected void spawnArenaPortal(int portalTileX, int portalTileY) {
      ArenaEntrancePortalMob mob = new ArenaEntrancePortalMob();
      mob.onSpawned(portalTileX * 32 + 16, portalTileY * 32 + 16);
      Float portalDir = GameMath.normalize(portalTileX - this.tileX, portalTileY - this.tileY);
      mob.targetPos = new Point(this.tileX * 32 + 16 + (int)(portalDir.x * 4.0F * 32.0F), this.tileY * 32 + 16 + (int)(portalDir.y * 4.0F * 32.0F));
      mob.keepAliveAlways = true;
      this.level.entityManager.mobs.add(mob);
   }

   @Override
   public void over() {
      if (!this.isOver()) {
         if (this.isClient()) {
            this.getClient().addShockwaveEffect(this.tileX * 32 + 16, this.tileY * 32 + 16, 2000.0F, 200.0F, 1000.0F, 100.0F, 150);
         }

         this.doFinalPresetPlace();
         if (this.wizardMob != null) {
            this.wizardMob.endBlackHolePhase();
         }
      }

      super.over();
   }

   protected void doFinalPresetPlace() {
      int arenaPresetTileX = this.tileX - this.arenaPreset.width / 2;
      int arenaPresetTileY = this.tileY - this.arenaPreset.height / 2;
      this.arenaPreset.applyToLevel(this.level, arenaPresetTileX, arenaPresetTileY);
      this.spawnArenaPortal(arenaPresetTileX + this.arenaPreset.width / 2, arenaPresetTileY - 2);
      this.spawnArenaPortal(arenaPresetTileX + this.arenaPreset.width / 2, arenaPresetTileY + this.arenaPreset.height + 1);
      this.spawnArenaPortal(arenaPresetTileX - 2, arenaPresetTileY + this.arenaPreset.height / 2);
      this.spawnArenaPortal(arenaPresetTileX + this.arenaPreset.width + 1, arenaPresetTileY + this.arenaPreset.height / 2);
   }

   protected static class ArenaTilePlace {
      public final int tileX;
      public final int tileY;
      public final int tileID;
      public final int objectID;

      public ArenaTilePlace(int tileX, int tileY, int tileID, int objectID) {
         this.tileX = tileX;
         this.tileY = tileY;
         this.tileID = tileID;
         this.objectID = objectID;
      }
   }

   protected class PlaceArenaLevelEventAction extends LevelEventAction {
      public void runAndSend(int tileX, int tileY, int tileID, int objectID) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(tileX);
         writer.putNextInt(tileY);
         writer.putNextInt(tileID);
         writer.putNextInt(objectID);
         this.runAndSendAction(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int tileX = reader.getNextInt();
         int tileY = reader.getNextInt();
         int tileID = reader.getNextInt();
         int objectID = reader.getNextInt();
         if (!AscendedBlackHoleEvent.this.getLevel().isClient()) {
            for (int layerID = 0; layerID < ObjectLayerRegistry.getTotalLayers(); layerID++) {
               AscendedBlackHoleEvent.this.getLevel().entityManager.destroyObjectOverride(layerID, tileX, tileY);
            }
         }

         if (objectID > 0) {
            AscendedBlackHoleEvent.this.getLevel().regionManager.setTileProtected(tileX, tileY, true);
         }

         if (tileID != -1) {
            AscendedBlackHoleEvent.this.getLevel().setTile(tileX, tileY, tileID);
         }

         if (objectID != -1) {
            AscendedBlackHoleEvent.this.getLevel().setObject(tileX, tileY, objectID);
         }

         for (int layerID = 1; layerID < ObjectLayerRegistry.getTotalLayers(); layerID++) {
            AscendedBlackHoleEvent.this.getLevel().objectLayer.setObject(layerID, tileX, tileY, 0);
         }
      }
   }
}
