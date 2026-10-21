package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventorySlot;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public abstract class BombProjectile extends Projectile {
   public float startHeight;
   public float throwHeight;
   protected float finalAngle = -1.0F;
   protected boolean stopsRotatingOnStationary = false;
   protected boolean isStationary;
   protected float lastSpeed;
   protected float particleTicker;
   protected SoundPlayer fuseSoundPlayer;

   public BombProjectile() {
   }

   public BombProjectile(float x, float y, float targetX, float targetY, int speed, int distance, GameDamage damage, Mob owner) {
      this();
      this.setLevel(owner.getLevel());
      this.applyData(x, y, targetX, targetY, speed, distance, damage, 0, owner);
   }

   @Override
   public void init() {
      super.init();
      this.givesLight = true;
      this.setWidth(15.0F);
      this.startHeight = 18.0F;
      this.throwHeight = this.distance * 0.1F;
      this.height = 18.0F;
      this.spawnTime = this.getWorldEntity().getTime();
      this.doesImpactDamage = false;
      this.trailOffset = 0.0F;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.isStationary);
      if (this.isStationary) {
         writer.putNextFloat(this.lastSpeed);
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.isStationary = reader.getNextBoolean();
      if (this.isStationary) {
         this.lastSpeed = reader.getNextFloat();
      }
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), new Color(160, 160, 160), 14.0F, 150, 18.0F);
   }

   @Override
   public void refreshParticleLight() {
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 0.0F, 0.5F);
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.texture.getWidth() / 2;
         int drawY = camera.getDrawY(this.y) - this.texture.getHeight() / 2;
         final TextureDrawOptions options = this.texture
            .initDraw()
            .light(light)
            .rotate(this.getAngle(), this.texture.getWidth() / 2, this.texture.getHeight() / 2)
            .pos(drawX, drawY - (int)this.getHeight());
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         this.addShadowDrawables(tileList, drawX, drawY, light, this.getAngle(), this.texture.getHeight() / 2);
      }
   }

   @Override
   public float getAngle() {
      return this.finalAngle != -1.0F ? this.finalAngle : (float)(this.getWorldEntity().getTime() - this.spawnTime);
   }

   @Override
   public float tickMovement(float delta) {
      float out = super.tickMovement(delta);
      this.updateHeight(delta);
      return out;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.spawnTime + this.getFuseTime() < this.getTime()) {
         this.onFuseEnded();
         this.remove();
      }
   }

   @Override
   protected SoundSettings getMoveSound() {
      return new SoundSettings(GameResources.burningFuse).volume(0.1F).basePitch(0.8F);
   }

   @Override
   public void clientTick() {
      super.clientTick();

      for (this.particleTicker += 0.5F; this.particleTicker >= 1.0F; this.particleTicker--) {
         Float dir = GameMath.getAngleDir(GameMath.fixAngle(this.getParticleAngle() + this.getAngle()));
         float startHeight = this.height + 2.0F - (int)(dir.y * this.getParticleDistance());
         this.spawnFuseParticle(this.x + dir.x * this.getParticleDistance(), this.y + 2.0F, startHeight);
      }

      if (this.spawnTime + this.getFuseTime() < this.getTime()) {
         this.onFuseEnded();
         this.remove();
      }
   }

   public void spawnFuseParticle(float x, float y, float startHeight) {
      spawnFuseParticle(this.getLevel(), x, y, startHeight);
   }

   public void onFuseEnded() {
      if (!this.isClient()) {
         ExplosionEvent event = this.getExplosionEvent(this.x, this.y);
         this.getLevel().entityManager.events.add(event);
      }
   }

   private void updateHeight(float delta) {
      if (this.isStationary) {
         this.traveledDistance = this.traveledDistance + this.lastSpeed * delta / 250.0F;
         float distPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
         float heightDistanceDecline = Math.max(0, (int)(this.startHeight * Math.abs(distPerc - 1.0F)));
         float throwHeightDistance = GameMath.sin(distPerc * 180.0F) * this.throwHeight;
         this.height = heightDistanceDecline + throwHeightDistance;
         if (distPerc >= 1.0F) {
            this.finalAngle = this.getAngle();
         }
      } else {
         float distPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
         float heightDistanceDecline = Math.max(0, (int)(this.startHeight * Math.abs(distPerc - 1.0F)));
         float throwHeightDistance = GameMath.sin(distPerc * 180.0F) * this.throwHeight;
         this.height = heightDistanceDecline + throwHeightDistance;
      }
   }

   @Override
   public void onHit(Mob mob, LevelObjectHit object, float fromX, float fromY, boolean fromPacket, ServerClient packetSubmitter) {
      if (mob == null) {
         if (!this.isStationary) {
            this.lastSpeed = this.speed;
            this.speed = 0.0F;
            if (this.stopsRotatingOnStationary) {
               this.finalAngle = this.getAngle();
            }

            this.isStationary = true;
         }
      }
   }

   @Override
   public void checkRemoved() {
      if (!this.isStationary && this.traveledDistance >= this.distance) {
         this.lastSpeed = this.speed;
         this.speed = 0.0F;
         if (this.stopsRotatingOnStationary) {
            this.finalAngle = this.getAngle();
         }

         this.isStationary = true;
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("explosion", 3);
   }

   public abstract int getFuseTime();

   public abstract float getParticleAngle();

   public abstract float getParticleDistance();

   public abstract ExplosionEvent getExplosionEvent(float var1, float var2);

   public float getOwnerToolTier() {
      Mob owner = this.getOwner();
      if (owner != null && owner.isPlayer) {
         PlayerInventoryManager inv = ((PlayerMob)owner).getInv();
         return (float)inv.streamInventorySlots(false, false, false, false)
            .map(InventorySlot::getItem)
            .filter(i -> i != null && i.item instanceof ToolDamageItem)
            .mapToDouble(i -> ((ToolDamageItem)i.item).getToolTier(i, owner))
            .max()
            .orElse(-1.0);
      } else {
         return -1.0F;
      }
   }

   public static void spawnFuseParticle(Level level, float x, float y, float startHeight) {
      spawnFuseParticle(level, x, y, startHeight, ParticleOption.defaultFlameHue, ParticleOption.defaultSmokeHue);
   }

   public static void spawnFuseParticle(Level level, float x, float y, float startHeight, float flameHue, float smokeHue) {
      level.entityManager
         .addParticle(x + (int)(GameRandom.globalRandom.nextGaussian() * 2.0), y, Particle.GType.COSMETIC)
         .heightMoves(startHeight, startHeight + 8.0F)
         .flameColor(flameHue)
         .sizeFades(8, 12)
         .lifeTime(1000)
         .onProgress(
            0.8F,
            p -> {
               for (int i = 0; i < GameRandom.globalRandom.getIntBetween(1, 2); i++) {
                  level.entityManager
                     .addParticle(p.x + (int)(GameRandom.globalRandom.nextGaussian() * 2.0), p.y, Particle.GType.COSMETIC)
                     .smokeColor(smokeHue)
                     .sizeFades(8, 12)
                     .heightMoves(startHeight + 6.0F, startHeight + 26.0F);
               }
            }
         );
   }
}
