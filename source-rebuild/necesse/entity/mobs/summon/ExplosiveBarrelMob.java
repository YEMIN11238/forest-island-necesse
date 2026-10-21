package necesse.entity.mobs.summon;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.explosionEvent.DynamiteExplosionEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffManager;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.TrapTrackObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.light.GameLight;

public class ExplosiveBarrelMob extends SummonedMob {
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );
   public static GameDamage damage = new GameDamage(250.0F);
   public float mobSpeed;
   public float topSpeed = 32.0F;
   private float particleBuffer;
   public int mobDir;
   protected long startTime = 0L;
   protected long startupTime = 250L;
   protected long moveStartTime = 0L;
   protected long lifeTime = 10000L;
   protected boolean isAtEndOfTrack = false;
   protected long explosionStartTime;
   protected float timeBeforeExploding = 3000.0F;
   protected boolean hasExploded = false;

   public ExplosiveBarrelMob() {
      super(1000000);
      this.isSummoned = true;
      this.setSpeed(32.0F);
      this.setFriction(0.0F);
      this.accelerationMod = 0.2F;
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-10, -10, 20, 14);
      this.hitBox = new Rectangle(-14, -15, 28, 24);
      this.selectBox = new Rectangle(-14, -20, 28, 30);
      this.overrideMountedWaterWalking = true;
      this.staySmoothSnapped = true;
      this.buffManager = new BuffManager(this) {
         @Override
         public ActiveBuff addBuff(ActiveBuff ab, boolean sendUpdatePacket, boolean forceOverride, boolean forceUpdateBuffs) {
            return ab;
         }
      };
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("sawDir", this.mobDir);
      save.addFloat("sawSpeed", this.mobSpeed);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.mobDir = save.getInt("sawDir", this.mobDir);
      this.mobSpeed = save.getFloat("sawSpeed", this.mobSpeed);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.explosionStartTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.explosionStartTime = reader.getNextLong();
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextFloat(this.mobSpeed);
      writer.putNextMaxValue(this.mobDir, 3);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.mobSpeed = reader.getNextFloat();
      this.mobDir = reader.getNextMaxValue(3);
   }

   @Override
   public void tickCurrentMovement(float delta) {
      super.tickCurrentMovement(delta);
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
   }

   @Override
   protected void tickCollisionMovement(float delta, Mob rider) {
      int tileX = this.getTileX();
      int tileY = this.getTileY();
      GameObject object = this.getLevel().getObject(tileX, tileY);
      if (object instanceof TrapTrackObject) {
         TrapTrackObject trackObject = (TrapTrackObject)object;
         float colDx = this.colDx / 20.0F;
         float colDy = this.colDy / 20.0F;
         float moveX = this.moveX;
         float moveY = this.moveY;
         MinecartLines lines = trackObject.getMinecartLines(this.getLevel(), tileX, tileY, moveX, moveY, false);
         MinecartLinePos pos = lines.getMinecartPos(this.x, this.y, this.mobDir);
         if (pos != null) {
            float moving = 0.0F;
            switch (this.mobDir) {
               case 0:
                  if (moveY < 0.0F) {
                     moving = 1.0F;
                  }

                  moving -= colDy;
                  colDx = 0.0F;
                  break;
               case 1:
                  if (moveX > 0.0F) {
                     moving = 1.0F;
                  }

                  moving += colDx;
                  colDy = 0.0F;
                  break;
               case 2:
                  if (moveY > 0.0F) {
                     moving = 1.0F;
                  }

                  moving += colDy;
                  colDx = 0.0F;
                  break;
               default:
                  if (moveX < 0.0F) {
                     moving = 1.0F;
                  }

                  moving -= colDx;
                  colDy = 0.0F;
            }

            if (colDx != 0.0F || colDy != 0.0F) {
               this.movementUpdateTime = Math.min(this.movementUpdateTime, this.getTime() - (this.movementUpdateCooldown - 1000));
            }

            float accMod = this.getAccelerationModifier();
            float speed = this.getSpeed();
            if (this.mobSpeed < this.topSpeed && this.moveStartTime > 0L) {
               this.mobSpeed = this.mobSpeed + (-(speed * moving) + this.mobSpeed) * delta / 250.0F * accMod;
            }

            if (this.mobSpeed < 0.0F) {
               this.mobDir = (this.mobDir + 2) % 4;
               this.mobSpeed = 0.0F;
            }

            if (moving == 0.0F && Math.abs(this.mobSpeed) < speed / 40.0F) {
               this.mobSpeed = 0.0F;
            }

            if (this.startTime == 0L) {
               this.startTime = this.getTime();
            }

            if (this.moveStartTime == 0L && this.getTime() - this.startTime > this.startupTime) {
               this.moveStartTime = this.getTime();
               this.mobSpeed = 30.0F;
            }

            if (this.mobSpeed > 0.0F) {
               if (this.moveStartTime > 0L) {
                  this.particleBuffer += delta;
                  this.drawTrailParticles();
               }

               MinecartLinePos resultPos = pos.progressLines(this.mobDir, this.mobSpeed * delta / 250.0F, null);
               this.x = resultPos.x;
               this.y = resultPos.y;
               this.mobDir = resultPos.dir;
               this.setDir(this.mobDir);
               int dir = this.getDir();
               this.dx = dir != 1 && dir != 3 ? 0.0F : this.mobSpeed * (dir == 1 ? 1 : -1);
               this.dy = dir != 0 && dir != 2 ? 0.0F : this.mobSpeed * (dir == 2 ? 1 : -1);
               if (resultPos.distanceRemainingToTravel > 0.0F) {
                  this.isAtEndOfTrack = true;
                  this.mobSpeed = 0.0F;
               }
            } else {
               this.moveStartTime = 0L;
               this.x = pos.x;
               this.y = pos.y;
               if (pos.dir != 1 && pos.dir != 3) {
                  if (this.mobDir == 1 || this.mobDir == 3) {
                     this.mobDir = pos.dir;
                  }
               } else if (this.mobDir == 0 || this.mobDir == 2) {
                  this.mobDir = pos.dir;
               }
            }
         }
      } else {
         this.mobDir = this.getDir();
         this.dx = 0.0F;
         this.dy = 0.0F;
         if (this.colDx != 0.0F || this.colDy != 0.0F) {
            this.movementUpdateTime = Math.min(this.movementUpdateTime, this.getTime() - (this.movementUpdateCooldown - 1000));
         }

         super.tickCollisionMovement(delta, rider);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isExploding()) {
         if (!this.hasExploded && (float)(this.getTime() - this.explosionStartTime) > this.timeBeforeExploding) {
            this.hasExploded = true;
         }

         if (!this.hasExploded) {
            int startHeight = GameRandom.globalRandom.getIntBetween(20, 30);
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
                  this.y + GameRandom.globalRandom.getFloatBetween(0.0F, 10.0F),
                  this.particleTypeSwitcher.next()
               )
               .sizeFades(20, 40)
               .heightMoves(startHeight, startHeight + 20)
               .color(
                  (options, lifeTime, timeAlive, lifePercent) -> {
                     float smokeProgress = GameMath.min(lifePercent * 2.0F, 1.0F);
                     Color fireColor = new Color(255, 150, 50);
                     Color smokeColor = new Color(45, 35, 35);
                     options.color(
                        new Color(
                           GameMath.lerp(smokeProgress, fireColor.getRed(), smokeColor.getRed()),
                           GameMath.lerp(smokeProgress, fireColor.getGreen(), smokeColor.getGreen()),
                           GameMath.lerp(smokeProgress, fireColor.getBlue(), smokeColor.getBlue())
                        )
                     );
                  }
               )
               .ignoreLight(true)
               .lifeTime(400 + GameRandom.globalRandom.getIntBetween(0, 500));
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.isExploding() && !this.hasExploded && (float)(this.getTime() - this.explosionStartTime) > this.timeBeforeExploding) {
         this.hasExploded = true;
         DynamiteExplosionEvent e = new DynamiteExplosionEvent(this.x, this.y, 230, damage, false, false, 0.0F, this);
         this.getLevel().entityManager.events.add(e);
      }

      if (this.startTime == 0L) {
         this.startTime = this.getTime();
      } else if (this.startTime + this.lifeTime < this.getTime()) {
         this.isAtEndOfTrack = true;
      }

      if (this.isAtEndOfTrack) {
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public GameMessage getAttackerName() {
      return new LocalMessage("deaths", "explosivebarrelname");
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("explosivebarrel", 2);
   }

   @Override
   public void spawnDamageText(int damage, int size, boolean isCrit) {
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   @Override
   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, GameBlackboard blackboard, boolean debug) {
      return false;
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return !this.isExploding() && !this.hasExploded && attacker != null && attacker.getFirstPlayerOwner() != null;
   }

   public boolean isExploding() {
      return this.explosionStartTime != 0L;
   }

   @Override
   public float getIncomingDamageModifier() {
      return 0.0F;
   }

   @Override
   public float getOutgoingDamageModifier() {
      float modifier = super.getOutgoingDamageModifier();
      if (this.getLevel() != null) {
         modifier *= this.getLevel().buffManager.getModifier(LevelModifiers.ENEMY_DAMAGE);
      }

      return modifier;
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      return super.canBeTargeted(attacker, attackerClient) && !attacker.isHostile;
   }

   @Override
   protected void doWasHitLogic(MobWasHitEvent event) {
      super.doWasHitLogic(event);
      this.setHealthHidden(this.getMaxHealth());
      if (!this.isExploding()) {
         this.explosionStartTime = this.getTime();
      }
   }

   @Override
   public void playHurtSound() {
      float variation = GameRandom.globalRandom.getFloatBetween(-0.1F, 0.1F);
      SoundManager.playSound(GameResources.cling, SoundEffect.effect(this.x, this.y).pitch(0.5F + variation));
      SoundManager.playSound(GameResources.blunthit, SoundEffect.effect(this.x, this.y).pitch(0.6F + variation).volume(1.4F));
   }

   @Override
   public void playHitDeathSound() {
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      this.playDeathSound();
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 3; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.explosiveBarrel, i, 2, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }

      if (!this.hasExploded) {
         for (int i = 0; i < 4; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(this.getLevel(), MobRegistry.Textures.explosiveBarrel, i, 3, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
                  Particle.GType.IMPORTANT_COSMETIC
               );
         }
      }
   }

   private long getTimeSinceStartedMoving() {
      return this.getTime() - this.moveStartTime;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   private void drawTrailParticles() {
      if (this.particleBuffer > 50.0F) {
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.floatGaussian() * 3.0F,
               this.y - 4.0F + GameRandom.globalRandom.floatGaussian() * 3.0F,
               Particle.GType.IMPORTANT_COSMETIC
            )
            .color(new Color(235, 193, 49))
            .height(0.0F)
            .lifeTime(500);
         this.particleBuffer -= 50.0F;
      }
   }

   @Override
   public void addDrawables(
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
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 62;
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final TextureDrawOptions options = MobRegistry.Textures.explosiveBarrel
         .initDraw()
         .sprite(this.hasExploded ? 0 : 1, 0, 32, 64)
         .light(light)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }
}
