package necesse.entity.mobs.hostile.bosses.scrapyardBoss;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;
import java.awt.geom.Point2D.Float;
import java.util.Collections;
import java.util.List;
import necesse.engine.CameraShake;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.LinesSoundEmitter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.mobMovement.MobMovement;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.mobs.networkField.BooleanNetworkField;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.projectile.LevelProjectile;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MutantHydraHeadMob extends MutantHydraNeckMob {
   public int headSpriteY = -1;
   public int headSpriteXOffset = 0;
   public float idleXOffset;
   public float idleYOffset;
   public float currentHeight;
   public float heightBuffer;
   public BooleanNetworkField setNewIdlePosition;
   public EmptyMobAbility startRumbleAbility;
   public boolean isRumbling;
   protected SoundPlayer rumbleSound;
   public MutantHydraHeadMob.StartGooSprayAbility startGooSprayAbility;
   public EmptyMobAbility stopGooSprayAbility;
   protected LevelProjectile<Projectile> gooSprayTargetProjectile;
   protected int gooSprayFireSFXCooldown = 200;
   protected long nextGooSprayFireSFXTime = 0L;
   protected int gooSprayLandSFXCooldown = 200;
   protected long nextGooSprayLandSFXTime = 0L;
   protected float gooSprayParticleBuffer;
   protected ParticleTypeSwitcher gooSprayParticleSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public static float gooSprayParticlesPerSecond = 50.0F;
   public IntMobAbility startLaunchAnimation;
   protected long launchStartTime;
   protected int launchAnimationTime;
   public IntMobAbility startShakeAnimation;
   protected CameraShake shake;
   protected int attackAnimationTicks;
   protected boolean returningToIdle;

   public MutantHydraHeadMob() {
      this.setSpeed(10.0F);
      this.setFriction(3.0F);
      this.moveAccuracy = 20;
      this.collision = new Rectangle(-25, -35, 50, 50);
      this.hitBox = new Rectangle(-25, -35, 50, 50);
      this.selectBox = new Rectangle(-25, -35, 50, 50);
      this.doesChainChildMobSendMovementPackets = true;
      this.setNewIdlePosition = this.registerNetworkField(new BooleanNetworkField(true) {
         public void onChanged(Boolean value) {
            super.onChanged(value);
            MutantHydraHeadMob.this.updateSpeed();
         }
      });
      this.startRumbleAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            MutantHydraHeadMob.this.isRumbling = true;
         }
      });
      this.startGooSprayAbility = this.registerAbility(new MutantHydraHeadMob.StartGooSprayAbility());
      this.stopGooSprayAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            MutantHydraHeadMob.this.gooSprayTargetProjectile = null;
            if (!MutantHydraHeadMob.this.isClient()) {
               MutantHydraHeadMob.this.returnToIdle();
            }
         }
      });
      this.startLaunchAnimation = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            MutantHydraHeadMob.this.launchStartTime = MutantHydraHeadMob.this.getLocalTime();
            MutantHydraHeadMob.this.launchAnimationTime = value;
            if (MutantHydraHeadMob.this.isClient()) {
               float pitch = GameRandom.globalRandom.getFloatBetween(0.5F, 0.9F);
               SoundManager.playSound(GameResources.slimeSplash2, SoundEffect.effect(MutantHydraHeadMob.this).volume(0.3F).pitch(pitch));
            }
         }
      });
      this.startShakeAnimation = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            long time = MutantHydraHeadMob.this.getLocalTime();
            Float lastShake = new Float();
            if (MutantHydraHeadMob.this.shake != null && !MutantHydraHeadMob.this.shake.isOver(time)) {
               lastShake = MutantHydraHeadMob.this.shake.getCurrentShake(time);
            }

            MutantHydraHeadMob.this.shake = new CameraShake(time, value, 50, 2.0F, 2.0F, false, lastShake);
         }
      });
   }

   @Override
   public void apply(MutantHydraBossMob.NeckIndexAndDistance indexAndDistance) {
      super.apply(indexAndDistance);
      this.collision = new Rectangle(-25, -35, 50, 50);
      this.hitBox = new Rectangle(-25, -35, 50, 50);
      this.selectBox = new Rectangle(-25, -35, 50, 50);
   }

   @Override
   public void onArrivedAtTarget() {
      super.onArrivedAtTarget();
      this.isRumbling = false;
   }

   @Override
   public void init() {
      super.init();
      this.heightBuffer = (float)(new GameRandom(this.getUniqueID()).nextDouble() * Math.PI * 2.0);
      this.currentHeight = this.getDesiredHeight();
   }

   @Override
   public void tickCurrentMovement(float delta) {
      if (!this.isClient() && (this.setNewIdlePosition.get() || this.returningToIdle) && this.hasArrivedAtTarget()) {
         MutantHydraBossMob master = this.master.get(this.getLevel());
         if (master != null) {
            if (this.returningToIdle) {
               this.setNewIdlePosition.set(true);
            }

            float targetXOffset = GameRandom.globalRandom.getFloatOffset(this.idleXOffset, 30.0F);
            float targetYOffset = GameRandom.globalRandom.getFloatOffset(this.idleYOffset, 30.0F);
            this.setMovement(new MobMovementRelative(master, targetXOffset, targetYOffset));
         }
      }

      super.tickCurrentMovement(delta);
   }

   @Override
   public void setMovement(MobMovement movement) {
      super.setMovement(movement);
      this.returningToIdle = false;
   }

   public void returnToIdle() {
      if (this.isClient()) {
         throw new IllegalStateException("Cannot set MutantHydraHeadMob movement on client");
      }

      MutantHydraBossMob master = this.master.get(this.getLevel());
      if (master != null) {
         float targetXOffset = GameRandom.globalRandom.getFloatOffset(this.idleXOffset, 30.0F);
         float targetYOffset = GameRandom.globalRandom.getFloatOffset(this.idleYOffset, 30.0F);
         this.setMovement(new MobMovementRelative(master, targetXOffset, targetYOffset));
         this.returningToIdle = true;
      } else {
         this.stopMoving();
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.moveTickDesiredHeight(delta);
      float desiredHeight = this.getDesiredHeight();
      float heightDelta = desiredHeight - this.currentHeight;
      float heightSpeed = Math.abs(heightDelta) * 2.0F + 10.0F;
      float heightToMove = heightSpeed * delta / 250.0F;
      if (Math.abs(heightDelta) < heightToMove) {
         this.currentHeight = desiredHeight;
      } else {
         this.currentHeight = this.currentHeight + Math.signum(heightDelta) * heightToMove;
      }

      if (!this.isServer() && this.gooSprayTargetProjectile != null) {
         final Projectile projectile = this.gooSprayTargetProjectile.get(this.getLevel());
         if (projectile != null) {
            if (this.nextGooSprayFireSFXTime <= this.getTime()) {
               this.nextGooSprayFireSFXTime = this.getTime() + this.gooSprayFireSFXCooldown;
               float pitch = GameRandom.globalRandom.getFloatBetween(0.5F, 0.9F);
               SoundManager.playSound(GameResources.slimeSplash2, SoundEffect.effect(this).volume(0.3F).pitch(pitch));
            }

            this.gooSprayParticleBuffer = this.gooSprayParticleBuffer + delta / 1000.0F * gooSprayParticlesPerSecond;

            while (this.gooSprayParticleBuffer >= 1.0F) {
               this.gooSprayParticleBuffer--;
               final float startX = GameRandom.globalRandom.getFloatOffset(this.x, 10.0F);
               final float startY = GameRandom.globalRandom.getFloatOffset(this.y - this.currentHeight, 10.0F) - 10.0F;
               final float endXOffset = GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F);
               final float endYOffset = GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F);
               float distance = GameMath.getExactDistance(startX, startY, projectile.x, projectile.y);
               final float maxHeight = GameMath.limit(distance * 3.0F, 30.0F, 100.0F);
               final int flyLifeTime = GameMath.limit((int)(distance * 4.0F), 400, 2000);
               final int groundIdleTime = 1800;
               final int startSize = GameRandom.globalRandom.getIntBetween(25, 35);
               final float rotateOffset = GameRandom.globalRandom.nextInt(360);
               final float rotateSpeed = GameRandom.globalRandom.getFloatBetween(50.0F, 150.0F) * (GameRandom.globalRandom.nextBoolean() ? 1 : -1);
               this.getLevel()
                  .entityManager
                  .addTopParticle(startX, startY, this.gooSprayParticleSwitcher.next(), this.drawOrder + 100)
                  .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
                  .moves(new ParticleOption.Mover() {
                     @Override
                     public void tick(Float pos, float deltax, int lifeTime, int timeAlive, float lifePercent) {
                        float flyTimePercent = (float)timeAlive / flyLifeTime;
                        if (flyTimePercent <= 1.0F) {
                           pos.x = GameMath.lerp(flyTimePercent, startX, projectile.x + projectile.dx * 10.0F + endXOffset);
                           pos.y = GameMath.lerp(flyTimePercent, startY, projectile.y + projectile.dy * 10.0F + endYOffset);
                        }
                     }
                  })
                  .height(new ParticleOption.HeightGetter() {
                     @Override
                     public float tick(float delta, int lifeTime, int timeAlive, float lifePercent) {
                        float flyTimePercent = Math.min((float)timeAlive / flyLifeTime, 1.0F);
                        return (float)Math.sin(flyTimePercent * Math.PI) * maxHeight;
                     }
                  })
                  .size(new ParticleOption.DrawModifier() {
                     @Override
                     public void modify(SharedTextureDrawOptions.Wrapper options, int lifeTime, int timeAlive, float lifePercent) {
                        float groundTimePercent = Math.max(0.0F, (float)(timeAlive - flyLifeTime) / groundIdleTime);
                        int currentSize = (int)(startSize * (1.0F - groundTimePercent));
                        options.size(currentSize, currentSize);
                     }
                  })
                  .rotation(new ParticleOption.FloatGetter() {
                     @Override
                     public float get(int lifeTime, int timeAlive, float lifePercent) {
                        float flyTimePercent = Math.min((float)timeAlive / flyLifeTime, 1.0F);
                        return rotateOffset + flyTimePercent * rotateSpeed;
                     }
                  })
                  .onTimeAlive(flyLifeTime, p -> {
                     if (this.nextGooSprayLandSFXTime <= this.getTime()) {
                        this.nextGooSprayLandSFXTime = this.getTime() + this.gooSprayLandSFXCooldown;
                        float pitch = GameRandom.globalRandom.getFloatBetween(0.5F, 0.9F);
                        SoundManager.playSound(GameResources.slimeSplash3, SoundEffect.effect(p.x, p.y).volume(0.2F).pitch(pitch));
                     }
                  })
                  .colorRandom(130.0F, 0.9F, 0.8F, 20.0F, 0.1F, 0.1F)
                  .givesLight(130.0F, 0.5F)
                  .lifeTime(flyLifeTime + groundIdleTime);
            }
         }
      }
   }

   public void moveTickDesiredHeight(float delta) {
      this.heightBuffer += delta / 250.0F;
      if (this.heightBuffer >= Math.PI * 2) {
         this.heightBuffer -= (float) (Math.PI * 2);
      }
   }

   public float getDesiredHeight() {
      float height = (float)(Math.sin(this.heightBuffer) * 5.0);
      long localTime = this.getLocalTime();
      if (localTime < this.launchStartTime + this.launchAnimationTime) {
         float progress = (float)(localTime - this.launchStartTime) / this.launchAnimationTime;
         float endPref = 0.4F;
         float slope = 1.5F;
         height -= (float)Math.pow(Math.sin(Math.pow(progress * Math.PI, endPref) / Math.pow(Math.PI, endPref - 1.0F)), slope) * 20.0F;
      }

      return height;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isRumbling) {
         if (this.rumbleSound == null || this.rumbleSound.isDone()) {
            this.rumbleSound = SoundManager.playSound(GameResources.rumble, SoundEffect.effect(this.getNeckSoundEmitter()).falloffDistance(1400));
         }

         if (this.rumbleSound != null) {
            this.rumbleSound.refreshLooping(1.0F);
         }
      }

      this.updateSpeed();
      if (!this.setNewIdlePosition.get()) {
         if (this.shake != null && !this.shake.isOver(this.getLocalTime())) {
            this.attackAnimationTicks = 0;
         } else {
            this.attackAnimationTicks++;
         }
      } else {
         this.attackAnimationTicks = 0;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.updateSpeed();
   }

   public void updateSpeed() {
      this.setSpeed(this.setNewIdlePosition.get() ? 10.0F : GameMath.lerp(this.getHealthPercent(), 220.0F, 160.0F));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.mutantHydraHeads,
                  28,
                  this.headSpriteY * 4 + i,
                  32,
                  this.x + GameRandom.globalRandom.floatGaussian() * 15.0F,
                  this.y + GameRandom.globalRandom.floatGaussian() * 15.0F,
                  10.0F,
                  knockbackX,
                  knockbackY
               ),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   public Float getShakeOffset() {
      return this.shake != null && !this.shake.isOver(this.getLocalTime()) ? this.shake.getCurrentShake(this.getLocalTime()) : new Float();
   }

   @Override
   protected void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      boolean mirroredHead = this.dx < 0.0F;
      Float shakeOffset = this.getShakeOffset();
      int drawX = camera.getDrawX(x + shakeOffset.x) - 64 + (mirroredHead ? -this.headSpriteXOffset : this.headSpriteXOffset);
      int drawY = camera.getDrawY(y - this.currentHeight + shakeOffset.y) - 90;
      int spriteX = GameUtils.getAnim(this.getTime(), 4, 1000);
      if (this.shake != null && !this.shake.isOver(this.getLocalTime())) {
         spriteX = 4;
      } else if (!this.setNewIdlePosition.get()) {
         int totalTimeToPlayAnimation = 500;
         int totalTicksToPlayAnimation = totalTimeToPlayAnimation / 50;
         int frame = Math.min((int)((float)this.attackAnimationTicks / totalTicksToPlayAnimation * 3.0F), 2);
         spriteX = 4 + frame;
      }

      DrawOptions drawOptions = MobRegistry.Textures.mutantHydraHeads
         .initDraw()
         .sprite(spriteX, this.headSpriteY, 128)
         .startGlowOptions(this, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .mirror(mirroredHead, false)
         .pos(drawX, drawY);
      topList.add(this.drawOrder, tm -> drawOptions.draw());
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.rumbleSound != null) {
         this.rumbleSound.dispose();
      }
   }

   public LinesSoundEmitter getNeckSoundEmitter() {
      return new LinesSoundEmitter() {
         @Override
         public Iterable<Line2D> getSoundLines() {
            MutantHydraBossMob master = MutantHydraHeadMob.this.master.get(MutantHydraHeadMob.this.getLevel());
            return master != null
               ? Collections.singleton(new Double(master.x, master.y, MutantHydraHeadMob.this.x, MutantHydraHeadMob.this.y))
               : Collections.singleton(
                  new Double(MutantHydraHeadMob.this.x, MutantHydraHeadMob.this.y, MutantHydraHeadMob.this.x, MutantHydraHeadMob.this.y + 1.0F)
               );
         }
      };
   }

   public class StartGooSprayAbility extends MobAbility {
      public void runAndSend(Projectile targetProjectile) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(targetProjectile.getUniqueID());
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int projectileUniqueID = reader.getNextInt();
         MutantHydraHeadMob.this.gooSprayTargetProjectile = new LevelProjectile<Projectile>(projectileUniqueID) {
            @Override
            public void onProjectileRemoved(Projectile projectile) {
               super.onProjectileRemoved(projectile);
               MutantHydraHeadMob.this.gooSprayTargetProjectile = null;
            }
         };
         MutantHydraHeadMob.this.setNewIdlePosition.set(false);
      }
   }
}
