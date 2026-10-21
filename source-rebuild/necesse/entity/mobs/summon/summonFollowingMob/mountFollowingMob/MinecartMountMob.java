package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketMobMount;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.summon.MinecartLinePos;
import necesse.entity.mobs.summon.MinecartLines;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.MinecartTrackObject;
import necesse.level.gameObject.TrapTrackObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MinecartMountMob extends MountFollowingMob {
   public float minecartSpeed;
   public int minecartDir;
   public float collisionMovementBuffer;
   public Point collisionMovementLastPos;
   protected SoundPlayer movingSound;
   protected SoundPlayer breakingSound;
   protected float breakParticleBuffer;
   protected boolean breakParticleAlternate;

   public MinecartMountMob() {
      super(1);
      this.setSpeed(200.0F);
      this.setFriction(3.0F);
      this.accelerationMod = 0.1F;
      this.setKnockbackModifier(0.1F);
      this.collision = new Rectangle(-10, -10, 20, 14);
      this.hitBox = new Rectangle(-14, -15, 28, 24);
      this.selectBox = new Rectangle(-14, -20, 28, 30);
      this.swimMaskMove = 8;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = 0;
      this.overrideMountedWaterWalking = true;
      this.staySmoothSnapped = true;
   }

   @Override
   protected GameMessage getSummonLocalization() {
      return MobRegistry.getLocalization("minecart");
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("minecartDir", this.minecartDir);
      save.addFloat("minecartSpeed", this.minecartSpeed);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.minecartDir = save.getInt("minecartDir", this.minecartDir);
      this.minecartSpeed = save.getFloat("minecartSpeed", this.minecartSpeed);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextFloat(this.minecartSpeed);
      writer.putNextMaxValue(this.minecartDir, 3);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.minecartSpeed = reader.getNextFloat();
      this.minecartDir = reader.getNextMaxValue(3);
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
      if (object instanceof MinecartTrackObject && !(object instanceof TrapTrackObject)) {
         MinecartTrackObject trackObject = (MinecartTrackObject)object;
         float colDx = this.colDx / 20.0F;
         float colDy = this.colDy / 20.0F;
         float moveX = this.moveX;
         float moveY = this.moveY;
         MinecartLines lines = trackObject.getMinecartLines(this.getLevel(), tileX, tileY, moveX, moveY, false);
         MinecartLinePos pos = lines.getMinecartPos(this.x, this.y, this.minecartDir);
         if (pos != null) {
            boolean breaking = false;
            float moving = 0.0F;
            if (this.minecartDir == 0) {
               if (moveY < 0.0F) {
                  moving = 1.0F;
               } else if (moveY > 0.0F) {
                  breaking = true;
                  moving = -1.0F;
               }

               moving -= colDy;
               colDx = 0.0F;
            } else if (this.minecartDir == 1) {
               if (moveX > 0.0F) {
                  moving = 1.0F;
               } else if (moveX < 0.0F) {
                  breaking = true;
                  moving = -1.0F;
               }

               moving += colDx;
               colDy = 0.0F;
            } else if (this.minecartDir == 2) {
               if (moveY > 0.0F) {
                  moving = 1.0F;
               } else if (moveY < 0.0F) {
                  breaking = true;
                  moving = -1.0F;
               }

               moving += colDy;
               colDx = 0.0F;
            } else {
               if (moveX < 0.0F) {
                  moving = 1.0F;
               } else if (moveX > 0.0F) {
                  breaking = true;
                  moving = -1.0F;
               }

               moving -= colDx;
               colDy = 0.0F;
            }

            if (colDx != 0.0F || colDy != 0.0F) {
               if (this.getLevel().entityManager.players.streamArea(this.x, this.y, 100).filter(this::collidesWith).anyMatch(p -> true)) {
                  this.collisionMovementBuffer = (float)(this.collisionMovementBuffer + GameMath.diagonalMoveDistance(colDx, colDy) * delta / 250.0 * 20.0);
               }

               this.collisionMovementLastPos = new Point(this.getX(), this.getY());
               this.movementUpdateTime = Math.min(this.movementUpdateTime, this.getWorldEntity().getTime() - (this.movementUpdateCooldown - 1000));
            }

            float friction = rider == null ? 2.0F : 0.0F;
            float accMod = this.getAccelerationModifier();
            float speed = this.getSpeed();
            if (friction != 0.0F) {
               this.minecartSpeed = this.minecartSpeed + (speed * friction * moving - friction * this.minecartSpeed) * delta / 250.0F * accMod;
            } else if (moving != 0.0F) {
               this.minecartSpeed = this.minecartSpeed + (speed * moving - this.minecartSpeed) * delta / 250.0F * accMod;
            }

            if (this.minecartSpeed < 0.0F) {
               this.minecartDir = (this.minecartDir + 2) % 4;
               this.minecartSpeed = 0.0F;
            }

            if (moving == 0.0F && Math.abs(this.minecartSpeed) < speed / 40.0F) {
               this.minecartSpeed = 0.0F;
            }

            if (this.minecartSpeed > 0.0F) {
               MinecartLinePos resultPos = pos.progressLines(this.minecartDir, this.minecartSpeed * delta / 250.0F, null);
               this.x = resultPos.x;
               this.y = resultPos.y;
               this.minecartDir = resultPos.dir;
               this.setDir(this.minecartDir);
               if (resultPos.distanceRemainingToTravel > 0.0F) {
                  if (!this.isServer() && this.minecartSpeed > 25.0F) {
                     SoundManager.playSound(GameResources.cling, SoundEffect.effect(this).volume(0.6F).pitch(0.8F));
                  }

                  this.minecartSpeed = 0.0F;
               } else if (!this.isServer()) {
                  if (breaking && moving < 0.0F) {
                     if (this.minecartSpeed > 10.0F) {
                        this.breakParticleBuffer += delta;
                        if (this.breakParticleBuffer > 10.0F) {
                           this.breakParticleBuffer -= 10.0F;
                           float xOffset = GameRandom.globalRandom.floatGaussian();
                           float yOffset = GameRandom.globalRandom.floatGaussian();
                           boolean alternate = this.breakParticleAlternate;
                           if (this.minecartDir == 0) {
                              xOffset += alternate ? 8.0F : -8.0F;
                              yOffset += 4.0F;
                           } else if (this.minecartDir == 1) {
                              yOffset += alternate ? 6.0F : -6.0F;
                              xOffset -= 4.0F;
                           } else if (this.minecartDir == 2) {
                              xOffset += alternate ? 8.0F : -8.0F;
                              yOffset -= 4.0F;
                           } else {
                              yOffset += alternate ? 6.0F : -6.0F;
                              xOffset += 4.0F;
                           }

                           float var10001 = this.x + xOffset;
                           float var10002 = this.y + yOffset;
                           this.getLevel()
                              .entityManager
                              .addParticle(var10001, var10002, Particle.GType.IMPORTANT_COSMETIC)
                              .color(new Color(210, 160, 8))
                              .sizeFadesInAndOut(4, 8, 50, 200)
                              .movesConstant(this.dx / 10.0F, this.dy / 10.0F)
                              .lifeTime(300)
                              .height(2.0F);
                           this.breakParticleAlternate = !this.breakParticleAlternate;
                        }
                     }

                     if (this.breakingSound == null || this.breakingSound.isDone()) {
                        this.breakingSound = SoundManager.playSound(GameResources.trainBrake, SoundEffect.effect(this).falloffDistance(1400).volume(0.0F));
                     }

                     if (this.breakingSound != null) {
                        this.breakingSound.effect.volume(GameMath.limit((this.minecartSpeed - 10.0F) / 100.0F, 0.0F, 1.0F) * 1.5F);
                        this.breakingSound.refreshLooping(0.5F);
                     }
                  }

                  if (this.movingSound == null || this.movingSound.isDone()) {
                     this.movingSound = SoundManager.playSound(GameResources.train, SoundEffect.effect(this).falloffDistance(1400).volume(0.0F));
                  }

                  if (this.movingSound != null) {
                     this.movingSound.effect.volume(Math.min(this.minecartSpeed / 200.0F, 1.0F) / 1.5F);
                     this.movingSound.refreshLooping(0.2F);
                  }
               }
            } else {
               this.x = pos.x;
               this.y = pos.y;
               if (pos.dir != 1 && pos.dir != 3) {
                  if (this.minecartDir == 1 || this.minecartDir == 3) {
                     this.minecartDir = pos.dir;
                  }
               } else if (this.minecartDir == 0 || this.minecartDir == 2) {
                  this.minecartDir = pos.dir;
               }

               this.setDir(this.minecartDir);
            }
         } else {
            this.minecartSpeed = 0.0F;
            this.minecartDir = rider == null ? this.getDir() : rider.getDir();
         }
      } else {
         this.minecartDir = rider == null ? this.getDir() : rider.getDir();
         this.dx = 0.0F;
         this.dy = 0.0F;
         if (this.colDx != 0.0F || this.colDy != 0.0F) {
            if (this.getLevel().entityManager.players.streamArea(this.x, this.y, 100).filter(this::collidesWith).anyMatch(p -> true)) {
               this.collisionMovementBuffer = (float)(this.collisionMovementBuffer + GameMath.diagonalMoveDistance(this.colDx, this.colDy) * delta / 250.0);
            }

            this.collisionMovementLastPos = new Point(this.getX(), this.getY());
            this.movementUpdateTime = Math.min(this.movementUpdateTime, this.getWorldEntity().getTime() - (this.movementUpdateCooldown - 1000));
         }

         super.tickCollisionMovement(delta, rider);
      }

      if (this.collisionMovementBuffer >= 5.0F
         || this.collisionMovementLastPos != null && GameMath.diagonalMoveDistance(this.collisionMovementLastPos, new Point(this.getX(), this.getY())) > 32.0) {
         this.collisionMovementBuffer = 0.0F;
         this.collisionMovementLastPos = null;
         this.sendMovementPacket(false);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.isMounted()) {
         this.moveX = 0.0F;
         this.moveY = 0.0F;
      }

      if (this.inLiquid()) {
         this.setHealth(0);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.isMounted()) {
         this.moveX = 0.0F;
         this.moveY = 0.0F;
      }
   }

   @Override
   public void spawnDamageText(int damage, int size, boolean isCrit) {
   }

   @Override
   public void playHurtSound() {
   }

   @Override
   public void interact(PlayerMob player) {
      if (this.isServer()) {
         if (player.getUniqueID() == this.rider) {
            player.dismount();
            this.getLevel().getServer().network.sendToClientsWithEntity(new PacketMobMount(player.getUniqueID(), -1, false, player.x, player.y), player);
         } else if (player.mount(this, false)) {
            this.getLevel()
               .getServer()
               .network
               .sendToClientsWithEntity(new PacketMobMount(player.getUniqueID(), this.getUniqueID(), false, player.x, player.y), player);
         }
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return !this.isMounted() || mob.getUniqueID() == this.rider;
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return this.isMounted() ? null : Localization.translate("controls", "usetip");
   }

   @Override
   public void playDeathSound() {
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 47;
      Point sprite = this.getAnimSprite(x, y, this.minecartDir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions behind = MobRegistry.Textures.minecart
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            swimMask.use();
            behind.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   public static void drawPlacePreview(Level level, int levelX, int levelY, int dir, GameCamera camera) {
      Mob mob = MobRegistry.getMob("minecart", level);
      if (mob != null) {
         mob.setPos(levelX, levelY, true);
         int tileX = mob.getTileX();
         int tileY = mob.getTileY();
         GameObject object = level.getObject(tileX, tileY);
         if (object instanceof MinecartTrackObject) {
            MinecartTrackObject trackObject = (MinecartTrackObject)object;
            float moveX = 0.0F;
            float moveY = 0.0F;
            if (dir == 0) {
               moveY = -1.0F;
            } else if (dir == 1) {
               moveX = 1.0F;
            } else if (dir == 2) {
               moveY = 1.0F;
            } else {
               moveX = -1.0F;
            }

            MinecartLines lines = trackObject.getMinecartLines(level, tileX, tileY, moveX, moveY, false);
            MinecartLinePos pos = lines.getMinecartPos(levelX, levelY, dir);
            if (pos != null) {
               int drawX = camera.getDrawX(pos.x) - 32;
               int drawY = camera.getDrawY(pos.y) - 47;
               Point sprite = mob.getAnimSprite((int)pos.x, (int)pos.y, pos.dir);
               drawY += mob.getBobbing((int)pos.x, (int)pos.y);
               drawY += level.getTile(getTileCoordinate(pos.x), getTileCoordinate(pos.y)).getMobSinkingAmount(mob);
               MobRegistry.Textures.minecart.initDraw().sprite(sprite.x, sprite.y, 64).alpha(0.5F).draw(drawX, drawY);
               return;
            }
         }
      }

      int drawX = camera.getDrawX(levelX) - 32;
      int drawY = camera.getDrawY(levelY) - 47;
      drawY += level.getLevelTile(getTileCoordinate(levelX), getTileCoordinate(levelY)).getLiquidBobbing();
      MobRegistry.Textures.minecart.initDraw().sprite(0, dir, 64).alpha(0.5F).draw(drawX, drawY);
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      Point p = new Point(0, dir);
      if (!this.inLiquid(x, y)) {
         p.x = (int)(this.getDistanceRan() / this.getRockSpeed()) % 2;
      }

      return p;
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      return getShadowDrawOptions(this, level, x, y, 0, this.minecartDir, light, camera);
   }

   public static TextureDrawOptions getShadowDrawOptions(Mob mob, Level level, int x, int y, int yOffset, int dir, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.minecart_shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 47 + yOffset;
      drawY += mob.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(mob);
      return shadowTexture.initDraw().sprite(0, dir, 64).light(light).pos(drawX, drawY);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   public int getWaterRockSpeed() {
      return 10000;
   }

   @Override
   public Point getSpriteOffset(int spriteX, int spriteY) {
      Point p = new Point(0, 0);
      p.x = p.x + this.getRiderDrawXOffset();
      p.y = p.y + this.getRiderDrawYOffset();
      return p;
   }

   @Override
   public int getRiderDrawYOffset() {
      return this.getSwimMaskShaderOptions(this.inLiquidFloat(this.getDrawX(), this.getDrawY())).drawYOffset - 6;
   }

   @Override
   public int getRiderArmSpriteX() {
      return 0;
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.minecart_mask[GameMath.limit(this.minecartDir, 0, MobRegistry.Textures.minecart_mask.length - 1)];
   }

   @Override
   public int getRiderMaskYOffset() {
      return -10;
   }

   @Override
   public boolean isWaterWalking() {
      GameObject object = this.getLevel().getObject(this.getTileX(), this.getTileY());
      return object instanceof MinecartTrackObject ? true : super.isWaterWalking();
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultRiderModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.WATER_WALKING, true));
   }
}
