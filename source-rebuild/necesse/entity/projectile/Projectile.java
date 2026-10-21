package necesse.entity.projectile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketProjectileHit;
import necesse.engine.network.packet.PacketProjectilePositionUpdate;
import necesse.engine.network.packet.PacketSpawnProjectile;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IDData;
import necesse.engine.registries.ProjectileModifierRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.IntersectionPoint;
import necesse.engine.util.LineHitbox;
import necesse.engine.util.PointHashSet;
import necesse.entity.Entity;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ProjectileHitboxMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.projectile.modifiers.ProjectileModifier;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public abstract class Projectile extends Entity implements Attacker {
   public final IDData idData = new IDData();
   protected ParticleTypeSwitcher spinningTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.COSMETIC);
   protected ProjectileModifier modifier;
   public float dx;
   public float dy;
   public float targetX;
   public float targetY;
   public float speed;
   public float traveledDistance;
   public float height;
   public int knockback;
   public int distance;
   public boolean heightBasedOnDistance;
   private GameDamage damage;
   public boolean doesImpactDamage;
   public boolean canBreakObjects;
   public boolean canHitMobs = true;
   public boolean clientHandlesHit;
   public NetworkClient handlingClient;
   public boolean isSolid;
   protected boolean canBounce = true;
   public int bouncing;
   public int piercing;
   protected int bounced;
   protected float angle;
   public boolean dropItem;
   public boolean sendPositionUpdate;
   protected long spawnTime;
   public int maxMovePerTick = 32;
   public float particleRandomOffset = 4.0F;
   public float particleRandomPerpOffset = 0.0F;
   public float particleDirOffset = -8.0F;
   public float particleSpeedMod = 0.1F;
   public float trailOffset = -10.0F;
   public final boolean hasHitbox;
   protected int hitboxMobID = -1;
   public float hitboxOffset = -5.0F;
   protected boolean isBoomerang = false;
   protected boolean returningToOwner = false;
   public boolean removeIfOutOfBounds = true;
   public boolean sendRemovePacket = true;
   public boolean isCircularHitbox = false;
   int team;
   public Trail trail;
   protected float width;
   protected float hitLength;
   protected boolean useWidthForCollision;
   public boolean givesLight;
   public float lightSaturation = 0.75F;
   protected float lightDistMoved;
   protected float tickDistMoved;
   protected double trailParticles;
   private float distToMove;
   private float angleToTurn;
   protected int amountHit;
   protected final MobHitCooldowns hitCooldowns = new MobHitCooldowns();
   protected final PointHashSet objectHits;
   private int owner;
   private Mob ownerMob;
   protected GameTexture texture;
   protected GameTexture shadowTexture;
   protected SoundPlayer spawnSound;
   protected SoundPlayer moveSoundPlayer;

   public final String getStringID() {
      return this.idData.getStringID();
   }

   public final int getID() {
      return this.idData.getID();
   }

   public Projectile(boolean isNetworkCapable, boolean hasHitbox) {
      if (isNetworkCapable) {
         ProjectileRegistry.instance.applyIDData((Class<? extends Projectile>)this.getClass(), this.idData);
         this.texture = ProjectileRegistry.Textures.getTexture(this.getID());
         this.shadowTexture = ProjectileRegistry.Textures.getShadowTexture(this.getID());
      } else {
         this.idData.setData(-1, "unknown");
         this.texture = GameResources.error;
         this.shadowTexture = null;
      }

      this.hasHitbox = hasHitbox;
      this.setDamage(new GameDamage(0.0F));
      this.objectHits = new PointHashSet();
      this.traveledDistance = 0.0F;
      this.lightDistMoved = 0.0F;
      this.tickDistMoved = 0.0F;
      this.isSolid = true;
      this.height = 18.0F;
      this.doesImpactDamage = true;
      this.knockback = 25;
      this.owner = -1;
   }

   public Projectile(boolean hasHitbox) {
      this(true, hasHitbox);
   }

   public Projectile() {
      this(false);
   }

   public void applyData(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, Mob owner) {
      this.applyData(x, y, targetX, targetY, speed, distance, damage, 25, owner);
   }

   public void applyData(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.setDamage(damage);
      this.knockback = knockback;
      this.setOwner(owner);
      this.setDistance(distance);
   }

   public boolean shouldSendSpawnPacket() {
      return this.isNetworkCapable();
   }

   public boolean isNetworkCapable() {
      return this.getID() != -1;
   }

   public void setupSpawnPacket(PacketWriter writer) {
      writer.putNextInt(this.getUniqueID());
      writer.putNextBoolean(this.modifier != null);
      if (this.modifier != null) {
         writer.putNextShortUnsigned(this.modifier.getID());
         this.modifier.setupSpawnPacket(writer);
      }

      this.setupPositionPacket(writer);
      writer.putNextFloat(this.speed);
      writer.putNextInt(this.distance);
      writer.putNextShort((short)this.knockback);
      writer.putNextInt(this.getOwnerID());
      this.damage.writePacket(writer);
      if (this.hasHitbox) {
         writer.putNextBoolean(true);
         if (this.hitboxMobID == -1) {
            this.generateHitboxMob(true);
         }

         writer.putNextInt(this.hitboxMobID);
      } else {
         writer.putNextBoolean(false);
      }

      writer.putNextInt(this.amountHit);
      this.hitCooldowns.setupPacket(writer, false, this.getWorldEntity().getTime());
      writer.putNextShortUnsigned(this.objectHits.size());

      for (Point objectHit : this.objectHits) {
         writer.putNextInt(objectHit.x);
         writer.putNextInt(objectHit.y);
      }
   }

   public void applySpawnPacket(PacketReader reader) {
      this.setUniqueID(reader.getNextInt());
      boolean hasModifier = reader.getNextBoolean();
      if (hasModifier) {
         int modifierID = reader.getNextShortUnsigned();
         this.modifier = ProjectileModifierRegistry.getModifier(modifierID);
         if (this.modifier == null) {
            throw new IllegalStateException("Could not find projectile modifier with ID " + modifierID);
         }

         this.modifier.projectile = this;
         this.modifier.applySpawnPacket(reader);
      }

      this.applyPositionPacket(reader);
      this.updateAngle();
      this.speed = reader.getNextFloat();
      this.setDistance(reader.getNextInt());
      this.knockback = reader.getNextShort();
      this.setOwner(reader.getNextInt());
      this.damage = GameDamage.fromReader(reader);
      if (reader.getNextBoolean()) {
         this.hitboxMobID = reader.getNextInt();
      }

      this.amountHit = reader.getNextInt();
      this.hitCooldowns.applyPacket(reader, this.getWorldEntity().getTime());
      int totalObjectHits = reader.getNextShortUnsigned();

      for (int i = 0; i < totalObjectHits; i++) {
         int tileX = reader.getNextInt();
         int tileY = reader.getNextInt();
         this.objectHits.add(tileX, tileY);
      }
   }

   public void setupPositionPacket(PacketWriter writer) {
      writer.putNextFloat(this.x);
      writer.putNextFloat(this.y);
      writer.putNextFloat(this.dx);
      writer.putNextFloat(this.dy);
      writer.putNextFloat(this.traveledDistance);
      if (this.modifier != null) {
         this.modifier.setupPositionPacket(writer);
      }
   }

   public void applyPositionPacket(PacketReader reader) {
      float x = reader.getNextFloat();
      float y = reader.getNextFloat();
      this.changePosition(x, y);
      this.dx = reader.getNextFloat();
      this.dy = reader.getNextFloat();
      this.traveledDistance = reader.getNextFloat();
      this.updateAngle();
      if (this.modifier != null) {
         this.modifier.applyPositionPacket(reader);
      }
   }

   public void changePosition(float x, float y) {
      double distance = new Float(this.x, this.y).distance(x, y);
      this.x = x;
      this.y = y;
      if (distance > 10.0) {
         this.replaceTrail();
      }
   }

   public Projectile setModifier(ProjectileModifier modifier) {
      if (this.isInitialized()) {
         throw new IllegalStateException("Cannot set projectile modifier after initialization");
      }

      this.modifier = modifier;
      modifier.projectile = this;
      return this;
   }

   @Override
   public void onRegionChanged(int lastRegionX, int lastRegionY, int newRegionX, int newRegionY) {
      super.onRegionChanged(lastRegionX, lastRegionY, newRegionX, newRegionY);
      if (this.isServer() && this.shouldSendSpawnPacket()) {
         this.sendPacketToNewClientsWithRegion(lastRegionX, lastRegionY, newRegionX, newRegionY, () -> new PacketSpawnProjectile(this));
      }
   }

   @Override
   public void init() {
      super.init();
      Mob owner = this.getOwner();
      if (owner != null) {
         this.setTeam(owner.getTeam());
      }

      this.canBreakObjects = owner != null && owner.isPlayer;
      if (owner != null) {
         if (this.isServer()) {
            if (!Settings.strictServerAuthority) {
               if (owner.isPlayer) {
                  this.handlingClient = ((PlayerMob)owner).getServerClient();
               }

               this.clientHandlesHit = true;
            }
         } else if (this.isClient()) {
            ClientClient client = this.getClient().getClient();
            if (!this.getClient().hasStrictServerAuthority()) {
               if (client != null && owner == client.playerMob) {
                  this.handlingClient = client;
               }

               this.clientHandlesHit = true;
            }
         }
      }

      if (this.getLevel() != null && this.isClient()) {
         if (this.trail != null) {
            this.trail.removeOnFadeOut = true;
         }

         this.trail = this.getTrail();
         if (this.trail != null) {
            this.trail.removeOnFadeOut = false;
            this.getLevel().entityManager.addTrail(this.trail);
         }
      }

      if (this.hasHitbox) {
         this.generateHitboxMob(this.hitboxMobID == -1);
      }

      if (this.modifier != null) {
         this.modifier.init();
      }

      this.spawnTime = this.getWorldEntity().getTime();
      if (this.isClient()) {
         this.playSpawnSound();
         if (this.removed()) {
            this.stopSounds();
         }
      }
   }

   @Override
   public void postInit() {
      super.postInit();
      if (this.modifier != null) {
         this.modifier.postInit();
      }
   }

   protected void replaceTrail() {
      if (this.trail != null) {
         this.trail.removeOnFadeOut = true;
         if (this.getLevel() != null && this.isClient()) {
            this.trail = this.getTrail();
            if (this.trail != null) {
               this.trail.removeOnFadeOut = false;
               this.getLevel().entityManager.addTrail(this.trail);
            }
         }
      }
   }

   public float tickMovement(float delta) {
      if (this.removed()) {
         return 0.0F;
      }

      if (this.isBoomerang) {
         Mob owner = this.getOwner();
         if (owner == null) {
            this.remove();
            return 0.0F;
         }

         if (this.returningToOwner) {
            this.setTarget(owner.x, owner.y);
         }
      }

      float moveX = this.getMoveDist(this.dx * this.speed, delta);
      float moveY = this.getMoveDist(this.dy * this.speed, delta);
      double totalDist = Math.sqrt(moveX * moveX + moveY * moveY);
      if (Double.isNaN(totalDist) || Double.isInfinite(totalDist)) {
         totalDist = 0.0;
      }

      this.moveDist(totalDist);
      if (this.removeIfOutOfBounds && !this.getLevel().regionManager.isTileLoaded(this.getTileX(), this.getTileY())) {
         this.remove();
      }

      return (float)totalDist;
   }

   @Override
   public void serverTick() {
      if (this.sendPositionUpdate && this.handlingClient == null) {
         this.sendServerUpdatePacket();
         this.sendPositionUpdate = false;
      }
   }

   @Override
   public void clientTick() {
      if (this.sendPositionUpdate) {
         if (this.isClient() && this.handlingClient == this.getLevel().getClient().getClient()) {
            this.sendClientUpdatePacket();
         }

         this.sendPositionUpdate = false;
      }

      if (this.givesLight && this.isClient()) {
         this.refreshParticleLight();
      }

      float particleChance = this.getParticleChance();
      if (particleChance > 0.0F && (particleChance >= 1.0F || GameRandom.globalRandom.nextFloat() <= particleChance)) {
         this.spawnSpinningParticle();
      }

      if (this.moveSoundPlayer == null || this.moveSoundPlayer.isDone()) {
         this.moveSoundPlayer = this.playMoveSound();
         if (this.moveSoundPlayer != null) {
            this.moveSoundPlayer.refreshLooping(0.5F);
         }
      }
   }

   protected SoundPlayer playMoveSound() {
      SoundSettings sound = this.getMoveSound();
      if (sound == null) {
         return null;
      }

      sound.setFallOffDistanceIfNotSet(2000);
      return SoundManager.playSound(sound, SoundEffect.effect(this));
   }

   protected SoundSettings getMoveSound() {
      return null;
   }

   public void refreshParticleLight() {
      Color color = this.getParticleColor();
      if (color == null) {
         this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y);
      } else {
         this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, color, this.lightSaturation);
      }
   }

   protected int getExtraSpinningParticles() {
      return 1;
   }

   protected int getSpinningParticleTopDrawOrder() {
      return Integer.MIN_VALUE;
   }

   protected void spawnSpinningParticle() {
      Color particleColor = this.getParticleColor();
      if (particleColor != null) {
         int drawOrder = this.getSpinningParticleTopDrawOrder();
         Float perp = GameMath.getPerpendicularDir(this.dx, this.dy);
         float height = this.getHeight();
         float randomPerp = GameRandom.globalRandom.floatGaussian();
         ParticleOption particle;
         if (drawOrder != Integer.MIN_VALUE) {
            particle = this.getLevel()
               .entityManager
               .addTopParticle(
                  this.x
                     + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                     + this.dx * this.particleDirOffset
                     + perp.x * randomPerp * this.particleRandomPerpOffset,
                  this.y
                     + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                     + this.dy * this.particleDirOffset
                     + perp.y * randomPerp * this.particleRandomPerpOffset,
                  this.spinningTypeSwitcher.next(),
                  drawOrder
               );
         } else {
            particle = this.getLevel()
               .entityManager
               .addParticle(
                  this.x
                     + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                     + this.dx * this.particleDirOffset
                     + perp.x * randomPerp * this.particleRandomPerpOffset,
                  this.y
                     + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                     + this.dy * this.particleDirOffset
                     + perp.y * randomPerp * this.particleRandomPerpOffset,
                  this.spinningTypeSwitcher.next()
               );
         }

         this.modifySpinningParticle(
            particle.movesConstant(this.dx * this.speed * this.particleSpeedMod, this.dy * this.speed * this.particleSpeedMod)
               .color(particleColor)
               .height(height)
         );
         int extra = this.getExtraSpinningParticles();

         for (int i = 0; i < extra; i++) {
            randomPerp = GameRandom.globalRandom.floatGaussian();
            ParticleOption extraParticle;
            if (drawOrder != Integer.MIN_VALUE) {
               extraParticle = this.getLevel()
                  .entityManager
                  .addTopParticle(
                     this.x
                        + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                        + this.dx * GameRandom.globalRandom.nextFloat() * this.maxMovePerTick
                        + this.dx * this.particleDirOffset
                        + perp.x * randomPerp * this.particleRandomPerpOffset,
                     this.y
                        + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                        + this.dy * GameRandom.globalRandom.nextFloat() * this.maxMovePerTick
                        + this.dy * this.particleDirOffset
                        + perp.y * randomPerp * this.particleRandomPerpOffset,
                     this.spinningTypeSwitcher.next(),
                     drawOrder
                  );
            } else {
               extraParticle = this.getLevel()
                  .entityManager
                  .addParticle(
                     this.x
                        + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                        + this.dx * GameRandom.globalRandom.nextFloat() * this.maxMovePerTick
                        + this.dx * this.particleDirOffset
                        + perp.x * randomPerp * this.particleRandomPerpOffset,
                     this.y
                        + GameRandom.globalRandom.floatGaussian() * this.particleRandomOffset
                        + this.dy * GameRandom.globalRandom.nextFloat() * this.maxMovePerTick
                        + this.dy * this.particleDirOffset
                        + perp.y * randomPerp * this.particleRandomPerpOffset,
                     this.spinningTypeSwitcher.next()
                  );
            }

            this.modifySpinningParticle(
               extraParticle.movesConstant(this.dx * this.speed * this.particleSpeedMod, this.dy * this.speed * this.particleSpeedMod)
                  .color(this.getParticleColor())
                  .height(height)
            );
         }
      }
   }

   protected void modifySpinningParticle(ParticleOption particle) {
   }

   protected void spawnDeathParticles() {
      Color particleColor = this.getParticleColor();
      if (particleColor != null) {
         float height = this.getHeight();

         for (int i = 0; i < 8; i++) {
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, this.spinningTypeSwitcher.next())
               .movesConstant(
                  GameRandom.globalRandom.getIntBetween(5, 20) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1),
                  GameRandom.globalRandom.getIntBetween(5, 20) * (GameRandom.globalRandom.nextBoolean() ? -1 : 1)
               )
               .color(this.getParticleColor())
               .height(height);
         }
      }
   }

   public float getParticleChance() {
      return 1.0F;
   }

   public Color getParticleColor() {
      return null;
   }

   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(150, 150, 150), 6.0F, 250, 18.0F);
   }

   public float getTrailThickness() {
      return this.trail.thickness;
   }

   public long getTimeSinceSpawned() {
      return this.spawnTime == 0L ? 0L : this.getTime() - this.spawnTime;
   }

   protected Mob generateHitboxMob(boolean generateMobID) {
      Mob lastMob = this.getHitboxMob();
      if (lastMob != null) {
         lastMob.remove();
      }

      Mob m = this.constructHitboxMob();
      m.setLevel(this.getLevel());
      if (generateMobID) {
         m.getUniqueID(new GameRandom(this.getUniqueID()));
      } else {
         m.setUniqueID(this.hitboxMobID);
      }

      this.getLevel().entityManager.addMob(m, 10.0F, 10.0F);
      this.hitboxMobID = m.getUniqueID();
      return m;
   }

   public Mob getHitboxMob() {
      return this.hitboxMobID == -1 ? null : this.getLevel().entityManager.mobs.get(this.hitboxMobID, false);
   }

   protected Mob constructHitboxMob() {
      ProjectileHitboxMob m = new ProjectileHitboxMob();
      m.projectile = this;
      return m;
   }

   public Rectangle getHitbox() {
      float size = Math.max(16.0F, this.getWidth());
      return new Rectangle(
         (int)(this.x + this.dx * this.hitboxOffset - size / 2.0F), (int)(this.y + this.dy * this.hitboxOffset - size / 2.0F), (int)size, (int)size
      );
   }

   public void setDistance(int distance) {
      this.distance = distance;
   }

   public void checkHitCollision(Line2D hitLine) {
      this.checkCollision(this.toHitbox(hitLine));
   }

   public final void setWidth(float width) {
      this.setWidth(width, false);
   }

   public final void setWidth(float width, float hitLength) {
      this.setWidth(width, false, hitLength);
   }

   public void setWidth(float width, boolean useWidthForCollision) {
      this.setWidth(width, useWidthForCollision, useWidthForCollision ? width / 2.0F : 0.0F);
   }

   public void setWidth(float width, boolean useWidthForCollision, float hitLength) {
      this.width = width;
      this.useWidthForCollision = useWidthForCollision;
      this.hitLength = hitLength;
   }

   public float getWidth() {
      return this.width;
   }

   public float getHitLength() {
      return this.hitLength;
   }

   public int movePerIteration() {
      return GameMath.limit((int)(Math.pow(this.speed, 1.7F) / 250.0), 1, 8);
   }

   public void moveDist(double dist) {
      dist = Math.min(dist, this.distance + this.distToMove - this.traveledDistance);
      this.distToMove = (float)(this.distToMove + dist);
      if (this.distToMove < 0.0F || java.lang.Float.isNaN(this.distToMove) || java.lang.Float.isInfinite(this.distToMove)) {
         this.distToMove = 0.0F;
      }

      if (this.distToMove == 0.0F) {
         this.checkRemoved();
      }

      int distPerIteration = this.movePerIteration();

      while (this.distToMove > distPerIteration && !this.removed()) {
         Float startPos = new Float(this.x, this.y);
         double movedDist = this.getDistanceMovedBeforeCollision(Math.min(distPerIteration, this.maxMovePerTick));
         this.distToMove = (float)(this.distToMove - movedDist);
         this.traveledDistance = (float)(this.traveledDistance + movedDist);
         this.tickDistMoved = (float)(this.tickDistMoved + movedDist);
         this.onMoveTick(startPos, movedDist);
         if (this.tickDistMoved >= this.maxMovePerTick) {
            this.onMaxMoveTick();
            this.tickDistMoved = this.tickDistMoved - this.maxMovePerTick;
         }

         if (this.isClient() && this.givesLight) {
            this.lightDistMoved = (float)(this.lightDistMoved + movedDist);
            if (this.lightDistMoved > 32.0F) {
               this.lightDistMoved -= 32.0F;
               if (this.isClient()) {
                  this.refreshParticleLight();
               }
            }
         }

         this.checkRemoved();
         this.addTrailPoint(this.x, this.y);
      }
   }

   public void addTrailPoint(float x, float y) {
      if (this.trail != null) {
         this.trail
            .addPoint(
               new TrailVector(x + this.dx * this.trailOffset, y + this.dy * this.trailOffset, this.dx, this.dy, this.getTrailThickness(), this.getHeight())
            );
      }
   }

   public void onMoveTick(Float startPos, double movedDist) {
      if (this.modifier != null) {
         this.modifier.onMoveTick(startPos, movedDist);
      }
   }

   public void spawnTrailParticle(Float startPos, double movedDist, Color color, float countDiv, int lifeTime) {
      if (this.isClient()) {
         float height = this.getHeight();
         this.trailParticles += movedDist / countDiv;
         if (this.trailParticles > 1.0) {
            Float endPos = new Float(this.x, this.y);
            Float norm = GameMath.normalize(startPos.x - endPos.x, startPos.y - endPos.y);
            Float perp = GameMath.getPerpendicularDir(norm.x, norm.y);

            while (this.trailParticles > 1.0) {
               this.trailParticles--;
               float dist = (float)(GameRandom.globalRandom.nextFloat() * movedDist);
               float perpDist = (GameRandom.globalRandom.nextFloat() - 0.5F) * 8.0F;
               float xOffset = norm.x * dist + perp.x * perpDist;
               float yOffset = norm.y * dist + perp.y * perpDist;
               float var10001 = startPos.x + xOffset;
               this.getLevel()
                  .entityManager
                  .addParticle(var10001, startPos.y + yOffset, this.spinningTypeSwitcher.next())
                  .color(color)
                  .height(height)
                  .lifeTime(lifeTime);
            }
         }
      }
   }

   public void onMaxMoveTick() {
   }

   public void checkRemoved() {
      if (this.traveledDistance >= this.distance) {
         if (this.isBoomerang) {
            if (!this.returningToOwner) {
               this.returnToOwner();
            } else {
               this.remove();
            }
         } else {
            this.doHitLogic(null, null, this.x, this.y);
            if (this.isServer() && this.dropItem) {
               this.dropItem();
            }

            this.remove();
            this.sendRemovePacket = false;
         }
      }
   }

   protected void returnToOwner() {
      if (!this.returningToOwner) {
         Mob owner = this.getOwner();
         if (owner != null) {
            this.distance = (int)(this.distance + owner.getDistance(this.x, this.y) * 4.0F);
            this.setTarget(owner.x, owner.y);
            if (this.trail != null) {
               this.trail
                  .addBreakPoint(
                     new TrailVector(
                        this.x + this.dx * this.trailOffset, this.y + this.dy * this.trailOffset, this.dx, this.dy, this.getTrailThickness(), this.getHeight()
                     )
                  );
            }
         } else {
            this.distance *= 5;
         }

         if (this.getTotalPiercing() > 0) {
            this.clearHits();
         }

         this.returningToOwner = true;
         this.isSolid = false;
      }
   }

   public boolean returningToOwner() {
      return this.returningToOwner;
   }

   public float getBoomerangUsage() {
      return 1.0F;
   }

   @Override
   public void remove() {
      if (!this.removed()) {
         if (this.isClient()) {
            this.spawnDeathParticles();
            this.stopSounds();
         }

         if (this.isBoomerang) {
            Mob owner = this.getOwner();
            if (owner instanceof ItemAttackerMob) {
               ((ItemAttackerMob)owner).boomerangs.remove(this);
            }
         }

         if (this.hasHitbox) {
            Mob hitboxMob = this.getHitboxMob();
            if (hitboxMob != null) {
               hitboxMob.remove();
            }
         }
      }

      this.sendRemovePacket = true;
      super.remove();
   }

   public void stopSounds() {
      if (this.spawnSound != null) {
         this.spawnSound.fadeOutAndStop(0.5F);
      }
   }

   public final Shape toHitbox(Line2D line) {
      if (line == null) {
         return null;
      } else {
         float width = this.getWidth();
         if (width > 0.0F) {
            return this.isCircularHitbox ? new LineHitbox(line, this.x, this.y, width) : new LineHitbox(line, width);
         } else {
            return line;
         }
      }
   }

   protected CollisionFilter getLevelCollisionFilter() {
      CollisionFilter filter = new CollisionFilter().projectileCollision();
      if (this.canBreakObjects) {
         filter = filter.addFilter(tp -> !tp.object().object.attackThrough);
      }

      Mob owner = this.getOwner();
      if (owner != null) {
         owner.modifyChasingCollisionFilter(filter, null);
      }

      return filter;
   }

   protected double getDistanceMovedBeforeCollision(double dist) {
      float width = this.getWidth();
      float hitLength = this.getHitLength();
      double hitDist = dist + hitLength;
      Float startPoint = new Float(this.x, this.y);
      Float endPoint = new Float(this.x + (float)(this.dx * hitDist), this.y + (float)(this.dy * hitDist));
      java.awt.geom.Line2D.Float hitLine = new java.awt.geom.Line2D.Float(startPoint, endPoint);
      if (!this.isSolid) {
         this.x = (float)(this.x + this.dx * dist);
         this.y = (float)(this.y + this.dy * dist);
         this.checkHitCollision(hitLine);
         return dist;
      }

      Float hitStartPoint = startPoint;
      java.awt.geom.Line2D.Float objectHitLine = hitLine;
      CollisionFilter collisionFilter = this.getLevelCollisionFilter();
      ArrayList<LevelObjectHit> hitCollisions = this.getLevel().getCollisions(hitLine, collisionFilter);
      if (hitCollisions.isEmpty() && this.useWidthForCollision) {
         for (int i = 8; i < width / 2.0F; i += 8) {
            hitStartPoint = GameMath.getPerpendicularPoint(startPoint, i, this.dx, this.dy);
            Float perpEndPoint = new Float(hitStartPoint.x + (float)(this.dx * dist), hitStartPoint.y + (float)(this.dy * dist));
            java.awt.geom.Line2D.Float perpLine = new java.awt.geom.Line2D.Float(hitStartPoint, perpEndPoint);
            hitCollisions = this.getLevel().getCollisions(perpLine, collisionFilter);
            if (!hitCollisions.isEmpty()) {
               objectHitLine = perpLine;
               break;
            }

            hitStartPoint = GameMath.getPerpendicularPoint(startPoint, -i, this.dx, this.dy);
            perpEndPoint = new Float(hitStartPoint.x + (float)(this.dx * dist), hitStartPoint.y + (float)(this.dy * dist));
            perpLine = new java.awt.geom.Line2D.Float(hitStartPoint, perpEndPoint);
            hitCollisions = this.getLevel().getCollisions(perpLine, collisionFilter);
            if (!hitCollisions.isEmpty()) {
               objectHitLine = perpLine;
               break;
            }
         }
      }

      IntersectionPoint<LevelObjectHit> p = this.getLevel().getCollisionPoint(hitCollisions, objectHitLine, false);
      if (p == null) {
         this.x = (float)(this.x + this.dx * dist);
         this.y = (float)(this.y + this.dy * dist);
         this.checkHitCollision(hitLine);
         return dist;
      }

      Float objectHitOffset = new Float(this.x - hitStartPoint.x, this.y - hitStartPoint.y);
      this.x = (float)p.getX() + objectHitOffset.x;
      this.y = (float)p.getY() + objectHitOffset.y;
      if (this.bounced < this.getTotalBouncing() && this.canBounce) {
         if (this.trail != null) {
            this.trail.addBreakPoint(new TrailVector((float)p.getX(), (float)p.getY(), this.dx, this.dy, this.getTrailThickness(), this.getHeight()));
         }

         if (p.dir == IntersectionPoint.Dir.RIGHT || p.dir == IntersectionPoint.Dir.LEFT) {
            this.dx = -this.dx;
            this.x = this.x + Math.signum(this.dx) * 4.0F;
         } else if (p.dir == IntersectionPoint.Dir.UP || p.dir == IntersectionPoint.Dir.DOWN) {
            this.dy = -this.dy;
            this.y = this.y + Math.signum(this.dy) * 4.0F;
         }

         this.updateAngle();
         this.sendPositionUpdate = true;
      } else if (p.dir == IntersectionPoint.Dir.UP) {
         this.y += 8.0F;
      } else if (p.dir == IntersectionPoint.Dir.RIGHT) {
         this.x -= 8.0F;
      } else if (p.dir == IntersectionPoint.Dir.DOWN) {
         this.y -= 8.0F;
      } else if (p.dir == IntersectionPoint.Dir.LEFT) {
         this.x += 8.0F;
      }

      this.onHit(null, p.target, (float)p.getX(), (float)p.getY(), false, null);
      this.bounced++;
      float distance = (float)hitStartPoint.distance(p);
      this.checkHitCollision(new java.awt.geom.Line2D.Float(this.x, this.y, this.x + this.dx * distance, this.y + this.dy * distance));
      return Math.max(distance, 1.0F);
   }

   public int getTotalBouncing() {
      Mob owner = this.getOwner();
      return owner == null ? this.bouncing : this.bouncing + owner.buffManager.getModifier(BuffModifiers.PROJECTILE_BOUNCES);
   }

   public int getTotalPiercing() {
      Mob owner = this.getOwner();
      return owner == null ? this.piercing : this.piercing + owner.buffManager.getModifier(BuffModifiers.PROJECTILE_PIERCES);
   }

   public float getMoveDist(float speed, float delta) {
      return speed * delta / 250.0F;
   }

   public boolean shouldAlwaysKnockAwayFromOwner() {
      return true;
   }

   public void onHit(Mob mob, LevelObjectHit object, float x, float y, boolean fromPacket, ServerClient packetSubmitter) {
      if (this.modifier == null || !this.modifier.onHit(mob, object, x, y, fromPacket, packetSubmitter)) {
         this.hit(mob, x, y, fromPacket, packetSubmitter);
         if (mob == null) {
            if (this.isClient()) {
               this.spawnWallHitParticles(x, y);
            }

            this.doHitLogic(mob, object, x, y);
            if (this.bounced >= this.getTotalBouncing() || !this.canBounce) {
               if (this.isBoomerang) {
                  this.returnToOwner();
               } else {
                  if (this.dropItem && this.isServer()) {
                     this.dropItem();
                  }

                  this.remove();
                  this.sendRemovePacket = false;
               }
            }
         } else {
            boolean isFromServerPacket = this.isClient() && fromPacket;
            boolean canHit = this.checkHitCooldown(
               mob, !this.isServer() || packetSubmitter == null && this.handlingClient == null && (!this.clientHandlesHit || !mob.isPlayer) ? 0 : 100
            );
            if (canHit && (this.amountHit() <= this.getTotalPiercing() || this.returningToOwner) || isFromServerPacket) {
               boolean addHit = true;
               if (this.isServer()) {
                  boolean isClientProjectile = this.handlingClient != null || this.clientHandlesHit && mob.isPlayer;
                  if (packetSubmitter == null && isClientProjectile) {
                     addHit = false;
                  } else {
                     if (this.doesImpactDamage) {
                        this.applyDamage(mob, x, y);
                     }

                     this.doHitLogic(mob, object, x, y);
                     if (packetSubmitter != null) {
                        this.getServer().network.sendToClientsWithEntityExcept(new PacketProjectileHit(this, x, y, mob), this, packetSubmitter);
                     } else {
                        this.getServer().network.sendToClientsWithEntity(new PacketProjectileHit(this, x, y, mob), this);
                     }
                  }
               } else if (this.isClient()) {
                  if (!this.getClient().hasStrictServerAuthority()) {
                     ClientClient client = this.getClient().getClient();
                     if ((!this.clientHandlesHit || mob != client.playerMob) && this.handlingClient != client) {
                        this.doHitLogic(mob, object, x, y);
                     } else if (!fromPacket) {
                        this.getClient().network.sendPacket(new PacketProjectileHit(this, x, y, mob));
                        this.startTargetHitCooldown(mob);
                        this.doHitLogic(mob, object, x, y);
                     } else {
                        addHit = false;
                     }
                  } else {
                     this.doHitLogic(mob, object, x, y);
                  }
               } else if (this.doesImpactDamage) {
                  this.applyDamage(mob, x, y);
               }

               if (addHit) {
                  this.addHit(mob);
               }
            }

            if (this.amountHit() > this.getTotalPiercing()) {
               if (this.isBoomerang) {
                  if (!this.returningToOwner) {
                     this.returnToOwner();
                  }
               } else {
                  this.remove();
               }
            }
         }
      }
   }

   @Deprecated
   public void hit(Mob mob, float x, float y, boolean fromPacket, ServerClient packetSubmitter) {
   }

   public void applyDamage(Mob mob, float x, float y) {
      float knockbackDirX = mob.x - x * this.dx * 50.0F;
      float knockbackDirY = mob.y - y * this.dy * 50.0F;
      if (this.shouldAlwaysKnockAwayFromOwner()) {
         Mob owner = this.getOwner();
         if (owner != null) {
            knockbackDirX = mob.x - owner.x;
            knockbackDirY = mob.y - owner.y;
         }
      }

      this.applyDamage(mob, x, y, knockbackDirX, knockbackDirY);
   }

   public void applyDamage(Mob mob, float x, float y, float knockbackDirX, float knockbackDirY) {
      mob.isServerHit(this.getDamage(), knockbackDirX, knockbackDirY, this.knockback, this);
   }

   public void attackThrough(LevelObjectHit hit) {
      if (!this.hasHit(hit.tileX, hit.tileY)) {
         hit.getLevelObject().attackThrough(this.getDamage(), this);
         this.addHit(hit.tileX, hit.tileY);
      }
   }

   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      this.addTrailPoint(x, y);
      if (this.modifier != null) {
         this.modifier.doHitLogic(mob, object, x, y);
      }

      this.doHitLogic(mob, x, y);
      if (this.isClient()) {
         if (mob == null && object == null) {
            this.playDisappearSound(x, y);
         } else {
            this.playHitSound(x, y);
         }
      }
   }

   @Deprecated
   public void doHitLogic(Mob mob, float x, float y) {
   }

   protected Color getWallHitColor() {
      Color c = this.getParticleColor();
      return c == null ? new Color(0.5F, 0.5F, 0.5F) : c;
   }

   protected void spawnWallHitParticles(float x, float y) {
      Color c = this.getWallHitColor();
      if (c != null) {
         float height = this.getHeight();

         for (int i = 0; i < 5; i++) {
            this.getLevel()
               .entityManager
               .addParticle(x, y, this.spinningTypeSwitcher.next())
               .movesConstant((float)GameRandom.globalRandom.nextGaussian() * 10.0F, (float)GameRandom.globalRandom.nextGaussian() * 10.0F)
               .height(height)
               .color(c)
               .size((options, lifeTime, timeAlive, lifePercent) -> options.size(8, 8))
               .lifeTime(200);
         }
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      Mob owner = this.getOwner();
      if (owner != null) {
         DeathMessageTable ownerDeathMessages = owner.getDeathMessages();
         if (ownerDeathMessages != null) {
            return ownerDeathMessages;
         }
      }

      return this.getDeathMessages("projectile", 3);
   }

   @Override
   public GameMessage getAttackerName() {
      Mob owner = this.getOwner();
      return owner != null ? owner.getAttackerName() : new LocalMessage("deaths", "unknownatt");
   }

   @Override
   public Mob getFirstAttackOwner() {
      return this.getOwner();
   }

   @Override
   public int getAttackerUniqueID() {
      return this.getUniqueID();
   }

   protected void playHitSound(float x, float y) {
      SoundSettings svp = this.getHitSound();
      if (svp != null && svp.sounds != null) {
         svp.setPitchVarianceIfNotSet(0.03F);
         SoundManager.playSound(svp, x, y);
      }
   }

   protected SoundSettings getHitSound() {
      return null;
   }

   public void playDisappearSound(float x, float y) {
      SoundManager.playSound(this.getDisappearSound(), x, y);
   }

   protected SoundSettings getDisappearSound() {
      return null;
   }

   public void playSpawnSound() {
      this.spawnSound = SoundManager.playSound(this.getSpawnSound(), this);
   }

   protected SoundSettings getSpawnSound() {
      return null;
   }

   protected CollisionFilter getAttackThroughCollisionFilter() {
      return new CollisionFilter().projectileCollision().attackThroughCollision();
   }

   protected final void checkCollision(Shape hitbox) {
      Mob ownerMob = this.getOwner();
      if (ownerMob != null && this.isBoomerang && this.returningToOwner && hitbox.intersects(ownerMob.getHitBox())) {
         this.remove();
      }

      if (this.isServer() && this.canBreakObjects) {
         for (LevelObjectHit hit : this.getLevel().getCollisions(hitbox, this.getAttackThroughCollisionFilter())) {
            if (!hit.invalidPos() && hit.getObject().attackThrough) {
               this.attackThrough(hit);
            }
         }
      }

      if (this.canHitMobs) {
         for (Mob target : this.streamTargets(ownerMob, hitbox)
            .filter(m -> this.canHit(m) && hitbox.intersects(m.getHitBox()))
            .filter(m -> !this.isSolid || m.canHitThroughCollision() || !this.perpLineCollidesWithLevel(m.x, m.y))
            .collect(Collectors.toCollection(LinkedList::new))) {
            this.onHit(target, null, this.x, this.y, false, null);
         }
      }
   }

   public boolean canHit(Mob mob) {
      return mob.canBeHit(this);
   }

   protected boolean perpLineCollidesWithLevel(float x, float y) {
      java.awt.geom.Line2D.Float perpLine = new java.awt.geom.Line2D.Float(x, y, x + -this.dy, y + this.dx);
      Point2D p = GameMath.getIntersectionPoint(new java.awt.geom.Line2D.Float(this.x, this.y, this.x + this.dx, this.y + this.dy), perpLine, true);
      if (p != null) {
         java.awt.geom.Line2D.Float colLine = new java.awt.geom.Line2D.Float(x, y, (float)p.getX(), (float)p.getY());
         return this.getLevel().collides(colLine, this.getLevelCollisionFilter());
      } else {
         return false;
      }
   }

   protected Stream<Mob> streamTargets(Mob owner, Shape hitBounds) {
      return owner != null
         ? GameUtils.streamTargets(owner, hitBounds)
         : Stream.concat(
            this.getLevel().entityManager.mobs.streamInRegionsShape(hitBounds, 1),
            GameUtils.streamNetworkClients(this.getLevel()).filter(c -> !c.isDead() && c.hasSpawned()).map(sc -> sc.playerMob)
         );
   }

   protected void dropItem() {
   }

   public void setDamage(GameDamage damage) {
      if (damage != null) {
         this.damage = damage;
      }
   }

   public GameDamage getDamage() {
      return this.damage;
   }

   public void setDir(float dx, float dy) {
      this.setTarget(this.x + dx * 100.0F, this.y + dy * 100.0F);
   }

   public void setTarget(float x, float y) {
      this.targetX = x;
      this.targetY = y;
      Float dir = GameMath.normalize(x - this.x, y - this.y);
      if (dir.x == 0.0F && dir.y == 0.0F) {
         this.dx = 1.0F;
         this.dy = 0.0F;
      } else {
         this.dx = dir.x;
         this.dy = dir.y;
      }

      this.angle = (float)Math.toDegrees(Math.atan2(this.dy, this.dx));
      this.angle += 90.0F;
      this.fixAngle();
   }

   public static Float getPredictedTargetPos(
      float targetStartX, float targetStartY, float targetDx, float targetDy, float myStartX, float myStartY, float mySpeed, float distanceOffset
   ) {
      float distance = (float)new Float(myStartX, myStartY).distance(targetStartX, targetStartY) + distanceOffset;
      float travelTime = getTravelTimeMillis(mySpeed, distance);
      float nextX = getPositionAfterMillis(targetDx, travelTime);
      float nextY = getPositionAfterMillis(targetDy, travelTime);
      return new Float(targetStartX + nextX, targetStartY + nextY);
   }

   public static Float getPredictedTargetPos(Mob target, float myStartX, float myStartY, float mySpeed, float distanceOffset) {
      return getPredictedTargetPos(target.x, target.y, target.dx, target.dy, myStartX, myStartY, mySpeed, distanceOffset);
   }

   public void setTargetPrediction(float x, float y, float dx, float dy, float distanceOffset) {
      Float targetPos = getPredictedTargetPos(x, y, dx, dy, this.x, this.y, this.speed, distanceOffset);
      this.setTarget(targetPos.x, targetPos.y);
   }

   public void setTargetPrediction(Mob mob, float distanceOffset) {
      for (int i = 0; i < 10; i++) {
         Mob mount = mob.getMount();
         if (mount == null) {
            break;
         }

         mob = mount;
      }

      this.setTargetPrediction(mob.x, mob.y, mob.dx, mob.dy, distanceOffset);
   }

   public void setTargetPrediction(Mob mob) {
      this.setTargetPrediction(mob, 0.0F);
   }

   public void fixAngle() {
      this.angle = GameMath.fixAngle(this.angle);
   }

   public void setAngle(float angle) {
      this.angle = angle;
      this.fixAngle();
      Float dir = GameMath.getAngleDir(this.angle - 90.0F);
      this.dx = dir.x;
      this.dy = dir.y;
      this.targetX = this.x + this.dx * 100.0F;
      this.targetY = this.y + this.dy * 100.0F;
   }

   public void updateAngle() {
      this.angle = (float)Math.toDegrees(Math.atan2(this.dy, this.dx));
      this.angle += 90.0F;
      this.fixAngle();
   }

   public float getAngle() {
      return this.angle % 360.0F;
   }

   public boolean turnToward(float x, float y, float angle) {
      this.angleToTurn += angle;
      float anglePerIteration = 2.0F;
      boolean out = false;

      while (this.angleToTurn >= anglePerIteration) {
         float angleToTarget = this.getAngleToTarget(x, y);
         float dif = this.getAngleDifference(angleToTarget);
         if (Math.abs(dif) - anglePerIteration < 1.0F) {
            anglePerIteration = Math.abs(dif) - 1.0F;
            this.angleToTurn = 0.0F;
            out = true;
         }

         this.setAngle(this.angle + anglePerIteration * Math.signum(dif));
         if (out) {
            break;
         }

         this.angleToTurn -= anglePerIteration;
      }

      return out;
   }

   public static float getAngleToTarget(float thisX, float thisY, float targetX, float targetY) {
      float dx = targetX - thisX;
      float dy = targetY - thisY;
      float angleToTarget = (float)Math.toDegrees(Math.atan(dy / dx));
      if (dx < 0.0F) {
         angleToTarget += 270.0F;
      } else {
         angleToTarget += 90.0F;
      }

      return angleToTarget;
   }

   public float getAngleToTarget(float x, float y) {
      return getAngleToTarget(this.x, this.y, x, y);
   }

   public float getAngleDifference(float angle) {
      return GameMath.getAngleDifference(angle, this.getAngle());
   }

   public float getHeight() {
      if (!this.heightBasedOnDistance) {
         return this.height;
      }

      float distPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      return this.height - (int)((this.height - 4.0F) * distPerc);
   }

   public void setOwner(int uniqueID, Level level) {
      this.owner = uniqueID;
      if (level != null) {
         this.ownerMob = GameUtils.getLevelMob(uniqueID, level);
         this.setOwner(this.ownerMob);
      }
   }

   public void setOwner(int uniqueID) {
      this.setOwner(uniqueID, this.getLevel());
   }

   public void setOwner(Mob owner) {
      if (owner != null) {
         this.owner = owner.getUniqueID();
         this.team = owner.getTeam();
         this.ownerMob = owner;
      }
   }

   public Mob getOwner() {
      if (this.ownerMob == null && this.owner != 0) {
         this.setOwner(this.owner);
      }

      return this.ownerMob;
   }

   public int getOwnerID() {
      return this.owner;
   }

   public boolean checkHitCooldown(Mob target, int tolerance) {
      return this.hitCooldowns.canHit(target, this.getTime(), tolerance);
   }

   public void addHit(Mob target) {
      this.startHitCooldown(target);
      this.amountHit++;
   }

   public void startHitCooldown(Mob target) {
      this.hitCooldowns.startCooldown(target);
   }

   public int amountHit() {
      return this.amountHit;
   }

   public boolean hasHit(int tileX, int tileY) {
      return this.objectHits.contains(tileX, tileY);
   }

   public void addHit(int tileX, int tileY) {
      this.objectHits.add(tileX, tileY);
   }

   public void clearHits() {
      this.hitCooldowns.resetCooldowns();
      this.objectHits.clear();
   }

   public int getTeam() {
      return this.team;
   }

   public boolean isSameTeam(Mob other) {
      return this.getTeam() != -1 && other.getTeam() != -1 ? this.getTeam() == other.getTeam() : false;
   }

   public void setTeam(int team) {
      this.team = team;
   }

   public float getLifeTime() {
      return this.traveledDistance / this.distance;
   }

   public void sendServerUpdatePacket() {
      if (this.isNetworkCapable()) {
         if (this.handlingClient != null) {
            this.getLevel()
               .getServer()
               .network
               .sendToClientsWithEntityExcept(new PacketProjectilePositionUpdate(this), this, (ServerClient)this.handlingClient);
         } else {
            this.getLevel().getServer().network.sendToClientsWithEntity(new PacketProjectilePositionUpdate(this), this);
         }
      }
   }

   public void sendClientUpdatePacket() {
      if (this.isNetworkCapable()) {
         this.getLevel().getClient().network.sendPacket(new PacketProjectilePositionUpdate(this));
      }
   }

   public Shape getSelectBox() {
      float height = this.getHeight();
      float size = Math.max(this.getWidth(), 16.0F);
      float size2 = size / 2.0F;
      return new LineHitbox(
         new java.awt.geom.Line2D.Float(
            this.x - this.dx * size2, this.y - this.dy * size2 - height, this.x + this.dx * size2, this.y + this.dy * size2 - height
         ),
         size
      );
   }

   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, boolean debug) {
      if (debug) {
         StringTooltips tips = new StringTooltips();
         tips.add("Projectile: " + this.getStringID() + " (" + this.getID() + ")");
         tips.add("UniqueID: " + this.getUniqueID());
         Mob owner = this.getOwner();
         if (owner != null) {
            tips.add("Owner: " + owner.getDisplayName() + " (" + this.getOwnerID() + ")");
         } else {
            tips.add("Owner: " + this.getOwnerID());
         }

         tips.add("Pos: " + this.x + ", " + this.y);
         tips.add("Delta: " + this.dx + ", " + this.dy);
         tips.add("Speed: " + this.speed + ", Angle: " + this.getAngle());
         tips.add("Width: " + this.getWidth());
         if (this.hasHitbox) {
            tips.add("HitboxMobID: " + this.hitboxMobID);
         }

         GameTooltipManager.addTooltip(tips, TooltipLocation.INTERACT_FOCUS);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.trail != null) {
         this.trail.removeOnFadeOut = true;
      }
   }

   protected float getFadeAlphaDistance(int fadeInDistance, int fadeOutDistance) {
      if (fadeInDistance > 0 && this.traveledDistance < fadeInDistance) {
         return this.traveledDistance / fadeInDistance;
      } else {
         return fadeOutDistance > 0 && this.traveledDistance > this.distance - fadeOutDistance
            ? Math.abs((Math.min(this.traveledDistance, this.distance) - (this.distance - fadeOutDistance)) / fadeOutDistance - 1.0F)
            : 1.0F;
      }
   }

   protected float getFadeAlphaTime(int fadeInMilliseconds, int fadeOutMilliseconds) {
      if (fadeInMilliseconds > 0) {
         float fadeInDist = this.getMoveDist(this.speed, fadeInMilliseconds);
         if (this.traveledDistance < fadeInDist) {
            return this.traveledDistance / fadeInDist;
         }
      }

      if (fadeOutMilliseconds > 0) {
         float fadeOutDist = this.getMoveDist(this.speed, fadeOutMilliseconds);
         if (this.traveledDistance > this.distance - fadeOutDist) {
            return Math.abs((Math.min(this.traveledDistance, this.distance) - (this.distance - fadeOutDist)) / fadeOutDist - 1.0F);
         }
      }

      return 1.0F;
   }

   protected void addShadowDrawables(OrderableDrawables list, int drawX, int drawY, GameLight light, float angle, int centerY) {
      this.addShadowDrawables(list, drawX, drawY, light, o -> o.rotate(angle, this.shadowTexture.getWidth() / 2, centerY));
   }

   protected void addShadowDrawables(OrderableDrawables list, int drawX, int drawY, GameLight light, float angle, int centerX, int centerY) {
      this.addShadowDrawables(list, drawX, drawY, light, o -> o.rotate(angle, centerX, centerY));
   }

   protected void addShadowDrawables(
      OrderableDrawables list, int drawX, int drawY, GameLight light, Function<TextureDrawOptionsEnd, TextureDrawOptionsEnd> modifier
   ) {
      this.addShadowDrawables(list, this.shadowTexture.initDraw().next(), drawX, drawY, light, modifier);
   }

   protected void addShadowDrawables(
      OrderableDrawables list,
      TextureDrawOptionsEnd options,
      int drawX,
      int drawY,
      GameLight light,
      Function<TextureDrawOptionsEnd, TextureDrawOptionsEnd> modifier
   ) {
      if (this.shadowTexture != null) {
         options = modifier.apply(options.light(light));
         TextureDrawOptions shadowOptions = options.pos(drawX, drawY);
         if (shadowOptions != null) {
            list.add(tm -> shadowOptions.draw());
         }
      }
   }

   protected void addServerProjectileDrawable(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (GlobalData.debugActive() && this.isClient()) {
         Server localServer = this.getLevel().getClient().getLocalServer();
         if (localServer != null) {
            Projectile other = localServer.world.getLevel(this.getLevel().getIdentifier()).entityManager.projectiles.get(this.getUniqueID(), false);
            if (other != null) {
               other.addDrawables(list, tileList, topList, overlayList, level, tickManager, camera, perspective);
               int drawX = camera.getDrawX(other.x);
               int drawY = camera.getDrawY(other.y - other.getHeight());
               final DrawOptions asterisk = () -> FontManager.bit.drawChar(drawX, drawY, '*', new FontOptions(12));
               list.add(new EntityDrawable(this) {
                  @Override
                  public int getSortY() {
                     return Integer.MAX_VALUE;
                  }

                  @Override
                  public void draw(TickManager tickManager) {
                     asterisk.draw();
                  }
               });
            }
         }
      }
   }

   @Override
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
      this.drawHitBox(overlayList, camera);
   }

   protected void drawHitBox(OrderableDrawables overlayList, GameCamera camera) {
      float hitLength = Math.max(this.getHitLength(), 16.0F);
      java.awt.geom.Line2D.Float hitLine = new java.awt.geom.Line2D.Float(this.x, this.y, this.x + this.dx * hitLength, this.y + this.dy * hitLength);
      Shape shape = this.toHitbox(hitLine);
      overlayList.add(tickManager -> Renderer.drawShape(shape, camera, true, 1.0F, 0.0F, 0.0F, 0.3F));
   }
}
