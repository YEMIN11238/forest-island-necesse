package necesse.entity.mobs.friendly;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import necesse.engine.CameraShake;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.PositionSoundEffect;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.CustomMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.event.SetMoveToTileAIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.MoveToAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerChaserAI;
import necesse.entity.mobs.friendly.human.MoveToTile;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.light.GameLight;

public class GrizzboltMob extends HostileMob {
   public static LootTable DEATH_ANIMATION_LOOT = new LootTable(new LootItem("grizzboltsphere"));
   public static int DEATH_PARTICLES_RISE_TIME = 1500;
   public static int DEATH_PARTICLES_SPINNING_TIME = 500;
   public static GameDamage DAMAGE = new GameDamage(80.0F).modFinalMultiplier(0.2F);
   public static int MINIGUN_CHARGE_TIME = 2000;
   public static int MINIGUN_DISCHARGE_TIME = 4000;
   public Point baseTile;
   public float minigunProgress;
   public GrizzboltMob.ShowGrizzboltAttackAbility showMinigunAbility;
   public SoundPlayer chargeSoundPlayer;
   public float lastMinigunSoundProgress;
   public static int textureDrawX = 64;
   public static int textureDrawY = 88;
   public static Point[] pivotOffsets = new Point[]{
      new Point(74 - textureDrawX, 60 - textureDrawY),
      new Point(58 - textureDrawX, 70 - textureDrawY),
      new Point(60 - textureDrawX, 74 - textureDrawY),
      new Point(80 - textureDrawX, 70 - textureDrawY)
   };

   public GrizzboltMob() {
      super(6000);
      this.setSpeed(30.0F);
      this.setArmor(30);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.2F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-20, -16, 40, 32);
      this.selectBox = new Rectangle(-30, -66, 60, 70);
      this.swimMaskMove = 26;
      this.swimMaskOffset = -48;
      this.swimSinkOffset = -8;
      this.attackAnimTime = 500;
      this.attackCooldown = 100;
      this.showMinigunAbility = this.registerAbility(new GrizzboltMob.ShowGrizzboltAttackAbility(this));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addPoint("baseTile", this.baseTile);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.baseTile = save.getPoint("baseTile", new Point(this.getTileX(), this.getTileY()), false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.minigunProgress);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.minigunProgress = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      if (this.baseTile == null) {
         this.baseTile = new Point(this.getTileX(), this.getTileY());
      }

      this.ai = new BehaviourTreeAI<>(this, new GrizzboltMob.GrizzboltAI<>());
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isAttacking && this.minigunProgress > 0.0F) {
         float decrease = delta / MINIGUN_DISCHARGE_TIME;
         this.minigunProgress = Math.max(this.minigunProgress - decrease, 0.0F);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.minigunProgress > 0.0F) {
         if (this.chargeSoundPlayer == null
            || this.chargeSoundPlayer.isDone()
            || Math.abs(this.minigunProgress - this.lastMinigunSoundProgress) > 0.02F
            || this.minigunProgress >= 1.0F && this.lastMinigunSoundProgress < 1.0F) {
            SoundPlayer lastPlayer = this.chargeSoundPlayer;
            float pitch = GameMath.lerp(this.minigunProgress, 1.0F, 1.5F);
            float volume = GameMath.lerp(this.minigunProgress, 0.3F, 0.2F);
            PositionSoundEffect effect = SoundEffect.effect(this).volume(volume).pitch(pitch).falloffDistance(1500);
            this.chargeSoundPlayer = SoundManager.playSound(GameResources.minigunCharge, effect, player -> {
               if (lastPlayer == null) {
                  player.fadeIn(1.0F);
               } else {
                  player.copyFadeInProgress(lastPlayer);
                  player.setPosition(lastPlayer.getPositionSeconds());
               }
            });
            this.lastMinigunSoundProgress = this.minigunProgress;
         }

         if (this.chargeSoundPlayer != null && !this.chargeSoundPlayer.isDone()) {
            this.chargeSoundPlayer.refreshLooping(0.5F);
         }
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("grizzbolt", 3);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      float colorHue = 195.0F;
      final float middleX = this.x;
      final float middleY = this.y;
      final float startHeight = 10.0F;
      final float endHeight = 50.0F;
      final float middleHeight = 20.0F;
      final Function<Float, Float> distanceFromCenterFunction = lifePercent -> {
         lifePercent = (float)Math.sin(lifePercent.floatValue() * Math.PI);
         return (float)Math.pow(lifePercent.floatValue(), 0.5);
      };
      final Function<Float, Float> heightFunction = lifePercent -> (float)Math.pow(lifePercent.floatValue(), 2.0);

      for (int i = 0; i < 50; i++) {
         int lifeTime = GameRandom.globalRandom.getIntBetween(DEATH_PARTICLES_RISE_TIME - 500, DEATH_PARTICLES_RISE_TIME);
         final float startX = GameRandom.globalRandom.getFloatOffset(middleX, 10.0F);
         final float startY = GameRandom.globalRandom.getFloatOffset(middleY, 10.0F);
         final float distanceFromCenter = GameRandom.globalRandom.getIntBetween(20, 90);
         final java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(GameRandom.globalRandom.nextInt(360));
         this.getLevel()
            .entityManager
            .addParticle(startX, startY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFadesInAndOut(16, 20, 300, 400)
            .height(new ParticleOption.HeightGetter() {
               @Override
               public float tick(float delta, int lifeTimex, int timeAlive, float lifePercent) {
                  float lerpHeight = GameMath.lerp((float)Math.pow(lifePercent, 2.0), startHeight, endHeight);
                  float distanceHeight = heightFunction.apply(lifePercent) * (middleHeight - startHeight);
                  return lerpHeight + distanceHeight;
               }
            })
            .rotates()
            .moves(new ParticleOption.Mover() {
               @Override
               public void tick(java.awt.geom.Point2D.Float pos, float delta, int lifeTimex, int timeAlive, float lifePercent) {
                  pos.x = startX + dir.x * distanceFromCenterFunction.apply(lifePercent) * distanceFromCenter;
                  pos.y = startY + dir.y * distanceFromCenterFunction.apply(lifePercent) * distanceFromCenter * 0.7F;
               }
            })
            .colorRandom(colorHue, 0.9F, 1.0F, 10.0F, 0.1F, 0.1F)
            .givesLight(colorHue, 0.5F)
            .lifeTime(lifeTime);
      }

      for (int i = 0; i < 10; i++) {
         final CameraShake shake = new CameraShake(0L, DEATH_PARTICLES_RISE_TIME + DEATH_PARTICLES_SPINNING_TIME, 50, 3.0F, 3.0F, false);
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .size(new ParticleOption.DrawModifier() {
               @Override
               public void modify(SharedTextureDrawOptions.Wrapper options, int lifeTime, int timeAlive, float lifePercent) {
                  timeAlive -= GrizzboltMob.DEATH_PARTICLES_RISE_TIME - 500;
                  int size;
                  if (timeAlive > 0) {
                     float sizePercent = Math.min((float)timeAlive / GrizzboltMob.DEATH_PARTICLES_SPINNING_TIME, 1.0F);
                     if (lifePercent > 0.95F) {
                        sizePercent *= 1.0F - (lifePercent - 0.95F) / 0.05F;
                     }

                     size = GameMath.lerp(sizePercent, 0, 30);
                  } else {
                     size = 0;
                  }

                  options.size(size, size);
               }
            })
            .height(new ParticleOption.HeightGetter() {
               @Override
               public float tick(float delta, int lifeTime, int timeAlive, float lifePercent) {
                  float otherLifePercent = Math.min((float)timeAlive / GrizzboltMob.DEATH_PARTICLES_RISE_TIME, 1.0F);
                  float lerpHeight = GameMath.lerp((float)Math.pow(otherLifePercent, 2.0), endHeight - 10.0F, endHeight + 10.0F);
                  float distanceHeight = heightFunction.apply(otherLifePercent) * (middleHeight - startHeight);
                  return lerpHeight + distanceHeight;
               }
            })
            .moves(new ParticleOption.Mover() {
               @Override
               public void tick(java.awt.geom.Point2D.Float pos, float delta, int lifeTime, int timeAlive, float lifePercent) {
                  java.awt.geom.Point2D.Float offset = shake.getCurrentShake(timeAlive);
                  pos.x = middleX + offset.x;
                  pos.y = middleY + offset.y;
               }
            })
            .rotates(4000.0F, 5000.0F)
            .colorRandom(colorHue, 0.9F, 1.0F, 10.0F, 0.1F, 0.1F)
            .givesLight(colorHue, 0.5F)
            .lifeTime(DEATH_PARTICLES_RISE_TIME + DEATH_PARTICLES_SPINNING_TIME);
      }
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (!this.isClient()) {
         final Mob mob = this;
         final float dropX = this.x;
         final float dropY = this.y;
         float seconds = (DEATH_PARTICLES_RISE_TIME + DEATH_PARTICLES_SPINNING_TIME) / 1000.0F;
         WaitForSecondsEvent event = new WaitForSecondsEvent(seconds) {
            @Override
            public void onWaitOver() {
               for (InventoryItem item : GrizzboltMob.DEATH_ANIMATION_LOOT
                  .getNewList(GameRandom.globalRandom, this.getLevel().buffManager.getModifier(LevelModifiers.LOOT), mob)) {
                  ItemPickupEntity entity = item.getPickupEntity(this.getLevel(), dropX, dropY);
                  entity.height = 50.0F;
                  if (GrizzboltMob.this.isBoss()) {
                     entity.showsLightBeam = true;
                  }

                  this.getLevel().entityManager.pickups.add(entity);
                  GrizzboltMob.this.itemsDropped.add(entity);
               }
            }
         };
         this.getLevel().entityManager.events.addHidden(event);
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
      addDrawables(this, this.minigunProgress, list, tileList, topList, level, x, y, tickManager, camera, perspective);
   }

   public static void addDrawables(
      AttackAnimMob mob,
      float minigunProgress,
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
      int drawX = camera.getDrawX(x) - textureDrawX;
      int drawY = camera.getDrawY(y) - textureDrawY;
      final int dir = mob.getDir();
      Point sprite = mob.getAnimSprite(x, y, dir);
      drawY += mob.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(mob);
      final MaskShaderOptions swimMask = mob.getSwimMaskShaderOptions(mob.inLiquidFloat(x, y));
      final DrawOptions bodyOptions = MobRegistry.Textures.grizzbolt_body
         .initDraw()
         .sprite(sprite.x, sprite.y, 128)
         .addMaskShader(swimMask)
         .startGlowOptions(level, mob.getID())
         .light(light)
         .applyEnemyTracker(mob, perspective)
         .pos(drawX, drawY);
      final DrawOptions armsOptions = MobRegistry.Textures.grizzbolt_arms
         .initDraw()
         .sprite(sprite.x, sprite.y, 128)
         .addMaskShader(swimMask)
         .startGlowOptions(level, mob.getID())
         .light(light)
         .applyEnemyTracker(mob, perspective)
         .pos(drawX, drawY);
      final DrawOptions gunOptions;
      if (minigunProgress > 0.0F) {
         java.awt.geom.Point2D.Float attackDir = mob.isAttacking ? mob.attackDir : null;
         if (attackDir == null) {
            if (dir == 0) {
               attackDir = new java.awt.geom.Point2D.Float(0.0F, -1.0F);
            } else if (dir == 1) {
               attackDir = new java.awt.geom.Point2D.Float(1.0F, 0.0F);
            } else if (dir == 2) {
               attackDir = new java.awt.geom.Point2D.Float(0.0F, 1.0F);
            } else {
               attackDir = new java.awt.geom.Point2D.Float(-1.0F, 0.0F);
            }
         }

         float angle = GameMath.getAngle(attackDir);
         if (dir == 0) {
            angle += 90.0F;
         } else if (dir == 2) {
            angle -= 90.0F;
         } else if (dir == 3) {
            angle += 180.0F;
         }

         Point offset = pivotOffsets[dir];
         gunOptions = MobRegistry.Textures.grizzbolt_minigun
            .initDraw()
            .sprite(0, sprite.y, 128)
            .rotate(angle, offset.x + textureDrawX, offset.y + textureDrawY)
            .startGlowOptions(level, mob.getID())
            .light(light)
            .applyEnemyTracker(mob, perspective)
            .pos(drawX + swimMask.drawXOffset, drawY + swimMask.drawYOffset);
      } else {
         gunOptions = MobRegistry.Textures.grizzbolt_minigun
            .initDraw()
            .sprite(0, sprite.y, 128)
            .startGlowOptions(level, mob.getID())
            .light(light)
            .applyEnemyTracker(mob, perspective)
            .pos(drawX + swimMask.drawXOffset, drawY + swimMask.drawYOffset);
      }

      DrawOptions shadowOptions = MobRegistry.Textures.grizzbolt_shadow.initDraw().light(light).pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            if (dir == 0) {
               armsOptions.draw();
               gunOptions.draw();
            }

            bodyOptions.draw();
            if (dir != 0) {
               armsOptions.draw();
               gunOptions.draw();
            }

            swimMask.stop();
         }
      });
      tileList.add(tm -> shadowOptions.draw());
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   public Point getWanderBaseTile() {
      return this.baseTile != null ? this.baseTile : super.getWanderBaseTile();
   }

   public static class GrizzboltAI<T extends GrizzboltMob> extends SelectorAINode<T> {
      public final PlayerChaserAI<T> chaserAINode;

      public GrizzboltAI() {
         this.addChild(new MoveToAINode<>());
         this.addChild(
            this.chaserAINode = new PlayerChaserAI<T>(768, 384, false, false) {
               public boolean canHitTarget(T mob, float fromX, float fromY, Mob target) {
                  return ChaserAINode.hasLineOfSightToTarget(mob, fromX, fromY, target);
               }

               public boolean attackTarget(T mob, Mob target) {
                  if (mob.canAttack()) {
                     int attackCooldownMs = Math.min(mob.attackCooldown, mob.attackAnimTime);
                     float increase = 1.0F / ((float)GrizzboltMob.MINIGUN_CHARGE_TIME / attackCooldownMs);
                     mob.minigunProgress = Math.min(mob.minigunProgress + increase, 1.0F);
                     mob.showMinigunAbility.run(target.getX(), target.getY());
                     Point offset = GrizzboltMob.pivotOffsets[mob.getDir()];
                     if (mob.minigunProgress >= 1.0F) {
                        Projectile projectile = ProjectileRegistry.getProjectile(
                           "grizzboltbullet",
                           mob.getLevel(),
                           mob.x + offset.x,
                           mob.y + offset.y + 22.0F,
                           target.x,
                           target.y,
                           200.0F,
                           1024,
                           GrizzboltMob.DAMAGE,
                           mob
                        );
                        projectile.moveDist(70.0);
                        mob.getLevel().entityManager.projectiles.add(projectile);
                     }

                     return true;
                  } else {
                     return false;
                  }
               }
            }
         );
         this.chaserAINode.chaserAINode.stoppingDistance = 256;
         WandererAINode<T> wanderer = new WandererAINode<>(15000);
         wanderer.searchRadius = 0;
         wanderer.runAwayFromAttackerToBase = true;
         this.addChild(wanderer);
      }

      public void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         super.onRootSet(root, mob, blackboard);
         blackboard.onEvent("lastTargetInvalid", e -> {
            Point baseTile = mob.getWanderBaseTile();
            if (baseTile != null) {
               float distanceToBase = mob.getDistance(baseTile.x * 32 + 16, baseTile.y * 32 + 16);
               if (distanceToBase > 384.0F) {
                  blackboard.submitEvent("setMoveToTile", new SetMoveToTileAIEvent(new MoveToTile(baseTile.x, baseTile.y, false) {
                     @Override
                     public boolean moveIfPathFailed(float tileDistance) {
                        return false;
                     }

                     @Override
                     public boolean isAtLocation(float tileDistance, boolean foundPath) {
                        return tileDistance < 2.0F;
                     }

                     @Override
                     public void onArrivedAtLocation() {
                        blackboard.submitEvent("resetTarget", new AIEvent());
                        blackboard.submitEvent("resetPathTime", new AIEvent());
                     }
                  }, true));
               }
            }
         });
      }

      public void init(T mob, Blackboard<T> blackboard) {
         Point baseTile = mob.getWanderBaseTile();
         if (baseTile != null) {
            blackboard.put(this.chaserAINode.targetFinderAINode.baseKey, new Point(baseTile.x * 32 + 16, baseTile.y * 32 + 16));
         } else {
            blackboard.put(this.chaserAINode.targetFinderAINode.baseKey, null);
         }

         super.init(mob, blackboard);
      }
   }

   public static class ShowGrizzboltAttackAbility extends CustomMobAbility {
      public final GrizzboltMob mob;

      public ShowGrizzboltAttackAbility(GrizzboltMob mob) {
         this.mob = mob;
      }

      public void run(int targetX, int targetY) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(targetX);
         writer.putNextInt(targetY);
         writer.putNextFloat(this.mob.minigunProgress);
         this.runAndSend(content);
      }

      @Override
      protected void run(Packet content) {
         PacketReader reader = new PacketReader(content);
         int targetX = reader.getNextInt();
         int targetY = reader.getNextInt();
         this.mob.minigunProgress = reader.getNextFloat();
         this.mob.showAttack(targetX, targetY, 0, true);
         if (this.mob.minigunProgress >= 1.0F && !this.mob.isServer()) {
            SoundManager.playSound(GameResources.minigunShoot, SoundEffect.effect(this.mob).volume(0.5F).falloffDistance(1500));
            int yOffset = this.mob.getDir() == 0 ? -10 : 20;
            this.mob
               .getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(this.mob.getLevel(), MobRegistry.Textures.grizzbolt_body, 5, 16, 32, this.mob.x, this.mob.y + yOffset, 20.0F, 0.0F, 0.0F),
                  Particle.GType.IMPORTANT_COSMETIC
               );
         }
      }
   }
}
