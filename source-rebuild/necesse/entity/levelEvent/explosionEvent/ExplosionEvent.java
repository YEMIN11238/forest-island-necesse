package necesse.entity.levelEvent.explosionEvent;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class ExplosionEvent extends LevelEvent implements Attacker {
   public float x;
   public float y;
   public int tileX;
   public int tileY;
   public int range;
   public GameDamage damage;
   public boolean destroysObjects;
   public boolean destroysTiles = false;
   public boolean destroysGrass = true;
   public float toolTier;
   public int owner;
   public Mob ownerMob;
   public boolean hitsOwner;
   public int knockback;
   public float targetRangeMod = 0.66F;
   public boolean sendCustomData;
   public boolean sendOwnerData;
   public int tickCounter;
   public int maxTicks;
   private final HashMap<Long, Float> tileDistances = new HashMap<>();
   private final HashSet<Integer> hitsUniqueIDs = new HashSet<>();

   public ExplosionEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier) {
      this.x = x;
      this.y = y;
      this.range = Math.max(0, Math.min(range, 32767));
      this.damage = damage;
      this.destroysObjects = destructive;
      this.toolTier = toolTier;
      this.owner = -1;
      this.knockback = 500;
      this.hitsOwner = true;
      this.sendOwnerData = false;
      this.sendCustomData = true;
   }

   public ExplosionEvent(float x, float y, int range, GameDamage damage, boolean destructive, float toolTier, Mob owner) {
      this(x, y, range, damage, destructive, toolTier);
      if (owner != null) {
         this.owner = owner.getUniqueID();
      }

      this.ownerMob = owner;
      this.sendOwnerData = true;
   }

   public ExplosionEvent(float x, float y, int range, GameDamage damage, boolean destroysObjects, boolean destroysTiles, float toolTier, Mob owner) {
      this(x, y, range, damage, destroysObjects, toolTier);
      this.destroysTiles = destroysTiles;
      if (owner != null) {
         this.owner = owner.getUniqueID();
      }

      this.ownerMob = owner;
      this.sendOwnerData = true;
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.x = reader.getNextFloat();
      this.y = reader.getNextFloat();
      if (this.sendOwnerData) {
         this.owner = reader.getNextInt();
      }

      if (this.sendCustomData) {
         this.range = reader.getNextShortUnsigned();
         this.damage = GameDamage.fromReader(reader);
         this.toolTier = reader.getNextFloat();
         this.destroysObjects = reader.getNextBoolean();
         this.destroysTiles = reader.getNextBoolean();
      }

      int hitsSize = reader.getNextShortUnsigned();
      this.hitsUniqueIDs.clear();

      for (int i = 0; i < hitsSize; i++) {
         this.hitsUniqueIDs.add(reader.getNextInt());
      }
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.x);
      writer.putNextFloat(this.y);
      if (this.sendOwnerData) {
         writer.putNextInt(this.owner);
      }

      if (this.sendCustomData) {
         writer.putNextShortUnsigned(this.range);
         this.damage.writePacket(writer);
         writer.putNextFloat(this.toolTier);
         writer.putNextBoolean(this.destroysObjects);
         writer.putNextBoolean(this.destroysTiles);
      }

      writer.putNextShortUnsigned(this.hitsUniqueIDs.size());

      for (int hitUniqueID : this.hitsUniqueIDs) {
         writer.putNextInt(hitUniqueID);
      }
   }

   @Override
   public void init() {
      super.init();
      this.tileX = getTileCoordinate(this.x);
      this.tileY = getTileCoordinate(this.y);
      if (this.owner != -1) {
         this.ownerMob = GameUtils.getLevelMob(this.owner, this.level, true, true);
         if (this.ownerMob == null) {
            GameLog.warn.println("Could not find owner with unique id " + this.owner + " for explosion event.");
            this.over();
         }
      }

      this.maxTicks = Math.max(5, this.range / 200 * 20 / 4);
   }

   @Override
   public void clientTick() {
      if (this.tickCounter == 0) {
         this.playExplosionEffects();
      }

      int adjustedRange = this.range - this.range / 10;
      float lastRange = Math.max(0.0F, adjustedRange * ((float)(this.tickCounter - 2) / this.maxTicks));
      float range = adjustedRange * ((float)this.tickCounter / this.maxTicks);
      int particles = (int)this.getParticleCount(range, lastRange);
      spawnExplosionParticles(
         this.level,
         this.x,
         this.y,
         particles,
         lastRange,
         range * this.getParticleRangeModifier(),
         (level, x, y, dirX, dirY, lifeTime, currentRange) -> this.spawnExplosionParticle(x, y, dirX, dirY, lifeTime, currentRange)
      );
      this.tickCounter++;
      if (this.tickCounter > this.maxTicks) {
         this.over();
      }
   }

   protected float getParticleRangeModifier() {
      return 1.0F;
   }

   public float getParticleCount(float currentRange, float lastRange) {
      return (float)(Math.PI * (currentRange + (currentRange - lastRange) / 2.0F) / 4.0);
   }

   public void spawnExplosionParticle(float x, float y, float dirX, float dirY, int lifeTime, float range) {
      spawnExplosionParticle(this.level, x, y, dirX, dirY, lifeTime);
   }

   public static void spawnExplosionParticles(Level level, float x, float y, int particles, float minRange, float maxRange) {
      spawnExplosionParticles(
         level,
         x,
         y,
         particles,
         minRange,
         maxRange,
         (level1, x1, y1, dirX, dirY, lifeTime, range) -> spawnExplosionParticle(level1, x1, y1, dirX, dirY, lifeTime)
      );
   }

   public static void spawnExplosionParticles(
      Level level, float x, float y, int particles, float minRange, float maxRange, ExplosionEvent.ExplosionSpawnFunction spawnFunction
   ) {
      for (int i = 0; i <= particles; i++) {
         float anglePerParticle = 360.0F / particles;
         int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
         float range = GameRandom.globalRandom.getFloatBetween(minRange, maxRange);
         float dx = (float)Math.sin(Math.toRadians(angle));
         float dy = (float)Math.cos(Math.toRadians(angle));
         spawnFunction.spawn(level, x + dx * range, y + dy * range, dx * 20.0F, dy * 20.0F, 400, range);
      }
   }

   public static void spawnExplosionParticle(Level level, float x, float y, float dirX, float dirY, int lifeTime) {
      level.entityManager
         .addParticle(x, y, Particle.GType.CRITICAL)
         .movesConstant(dirX, dirY)
         .flameColor()
         .height(10.0F)
         .sizeFades(15, 25)
         .givesLight(0.0F, 0.5F)
         .onProgress(
            0.4F,
            p -> {
               java.awt.geom.Point2D.Float norm = GameMath.normalize(dirX, dirY);
               level.entityManager
                  .addParticle(p.x + norm.x * 20.0F, p.y + norm.y * 20.0F, Particle.GType.IMPORTANT_COSMETIC)
                  .movesConstant(dirX, dirY)
                  .smokeColor()
                  .heightMoves(10.0F, 40.0F)
                  .lifeTime(lifeTime);
            }
         )
         .lifeTime(lifeTime);
   }

   @Override
   public void serverTick() {
      int adjustedRange = this.range;
      float lastRange = Math.max(0.0F, adjustedRange * ((float)(this.tickCounter - 2) / this.maxTicks));
      float range = adjustedRange * ((float)this.tickCounter / this.maxTicks);
      if (this.ownerMob != null && this.ownerMob.isPlayer || this.destroysObjects || this.destroysTiles) {
         int tileRange = (int)(range / 32.0F) + 1;

         for (int x = this.tileX - tileRange; x <= this.tileX + tileRange; x++) {
            for (int y = this.tileY - tileRange; y <= this.tileY + tileRange; y++) {
               GameObject target = this.level.getObject(x, y);
               if (target.toolType != ToolType.UNBREAKABLE && target.toolTier <= this.toolTier) {
                  int finalX = x;
                  int finalY = y;
                  float distance = this.tileDistances
                     .compute(
                        GameMath.getUniqueLongKey(x, y),
                        (k, d) -> (Float)(d != null && d != 0.0F
                           ? d
                           : (float)new java.awt.geom.Point2D.Float(this.x, this.y).distance(finalX * 32 + 16, finalY * 32 + 16))
                     );
                  if (!(distance < lastRange) && !(distance > range)) {
                     GameDamage damage = this.getTotalObjectDamage(this.getDistanceMod(distance));
                     int totalDamage = damage.getTotalDamage(null, this, 1.0F);
                     ServerClient client = null;
                     if (this.ownerMob != null && this.ownerMob.isPlayer) {
                        PlayerMob player = (PlayerMob)this.ownerMob;
                        if (player.isServerClient()) {
                           client = player.getServerClient();
                        }
                     }

                     if (this.destroysObjects) {
                        for (int layer = 0; layer < ObjectLayerRegistry.getTotalLayers(); layer++) {
                           this.level.getObject(layer, x, y).doExplosionDamage(this.level, layer, x, y, totalDamage, this.toolTier, this, client);
                        }
                     } else if (client != null) {
                        GameObject object = this.level.getObject(x, y);
                        if (object.attackThrough) {
                           if (!this.destroysGrass) {
                              damage = new GameDamage(0.0F);
                           }

                           object.attackThrough(this.level, x, y, damage, client.playerMob);
                        }
                     }

                     if (this.destroysTiles) {
                        this.level.getTile(x, y).doExplosionDamage(this.level, x, y, totalDamage, this.toolTier, this, client);
                     }
                  }
               }
            }
         }
      }

      this.streamTargets().filter(this::canHitMob).filter(m -> !this.hitsUniqueIDs.contains(m.getHitCooldownUniqueID())).forEach(m -> {
         float distancex = m.getDistance(this.x, this.y);
         if (distancex >= lastRange && distancex <= range) {
            this.onMobWasHit(m, distancex);
            this.hitsUniqueIDs.add(m.getHitCooldownUniqueID());
         }
      });
      this.tickCounter++;
      if (this.tickCounter > this.maxTicks) {
         this.over();
      }
   }

   protected void onMobWasHit(Mob mob, float distance) {
      float mod = this.getDistanceMod(distance);
      GameDamage damage = this.getTotalMobDamage(mod);
      float knockback = this.knockback * mod;
      mob.isServerHit(damage, mob.getX() - this.x, mob.getY() - this.y, knockback, this);
   }

   protected float getDistanceMod(float targetDistance) {
      float percentRange = GameMath.limit(targetDistance * this.targetRangeMod / this.range, 0.0F, 1.0F);
      float percentRangeInv = Math.abs(percentRange - 1.0F);
      return (float)Math.pow(percentRangeInv, 2.5);
   }

   protected boolean canHitMob(Mob target) {
      return target.canBeHit(this);
   }

   protected GameDamage getTotalMobDamage(float mod) {
      return this.damage.modDamage(mod);
   }

   protected GameDamage getTotalObjectDamage(float mod) {
      return this.getTotalMobDamage(mod);
   }

   protected Stream<Mob> streamTargets() {
      if (this.ownerMob == null) {
         return Stream.concat(
            this.level.entityManager.mobs.getInRegionByTileRange(getTileCoordinate(this.x), getTileCoordinate(this.y), this.range / 32 + 2).stream(),
            GameUtils.streamServerClients(this.level).map(c -> c.playerMob)
         );
      }

      Stream<Mob> out = GameUtils.streamTargets(this.ownerMob, this.level, GameUtils.rangeBounds((int)this.x, (int)this.y, this.range + 64));
      if (this.hitsOwner && this.ownerMob.getLevel().isSamePlace(this.getLevel())) {
         out = Stream.concat(out, Stream.of(this.ownerMob));
      }

      return out;
   }

   @Override
   public GameMessage getAttackerName() {
      return this.ownerMob != null ? this.ownerMob.getAttackerName() : new LocalMessage("deaths", "explosionname");
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("explosion", 3);
   }

   @Override
   public Mob getFirstAttackOwner() {
      return this.ownerMob;
   }

   protected void playExplosionEffects() {
      SoundSettings explosionSound = this.getExplosionSound();
      if (explosionSound != null) {
         SoundManager.playSound(explosionSound, SoundEffect.effect(this.x, this.y));
      }
   }

   protected SoundSettings getExplosionSound() {
      return new SoundSettings(GameResources.explosionHeavy).volume(2.5F);
   }

   @Override
   public Point getSaveToRegionPos() {
      return new Point(
         this.level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(this.x)),
         this.level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(this.y))
      );
   }

   public interface ExplosionSpawnFunction {
      void spawn(Level var1, float var2, float var3, float var4, float var5, int var6, float var7);
   }
}
