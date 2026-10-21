package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.RayLinkedList;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.ParticleBeamHandler;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ability.TargetedMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.InverterAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.laserProjectile.CrystalGolemBeamProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class CrystalGolemMob extends HostileMob {
   public static GameDamage damage = new GameDamage(130.0F);
   public static int chargeTime = 2000;
   public static int stickTime = 200;
   protected long shootTime;
   protected Mob shootTarget;
   protected int shootX;
   protected int shootY;
   public ParticleBeamHandler warningBeam;
   public final TargetedMobAbility startShootingAbility;
   public final CoordinateMobAbility stickShootAbility;
   public final CoordinateMobAbility shootAbility;
   protected SoundPlayer beamChargeSoundPlayer;

   public CrystalGolemMob() {
      super(500);
      this.setArmor(40);
      this.setSpeed(20.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.4F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -6;
      this.swimSinkOffset = -4;
      this.startShootingAbility = this.registerAbility(new TargetedMobAbility() {
         @Override
         protected void run(Mob target) {
            CrystalGolemMob.this.attackAnimTime = CrystalGolemMob.chargeTime + CrystalGolemMob.stickTime + 500;
            CrystalGolemMob.this.attackCooldown = CrystalGolemMob.chargeTime + CrystalGolemMob.stickTime;
            CrystalGolemMob.this.shootTime = CrystalGolemMob.this.getTime() + CrystalGolemMob.chargeTime + CrystalGolemMob.stickTime;
            CrystalGolemMob.this.shootTarget = target;
            if (CrystalGolemMob.this.warningBeam != null) {
               CrystalGolemMob.this.warningBeam.dispose();
            }

            CrystalGolemMob.this.warningBeam = null;
            CrystalGolemMob.this.startAttackCooldown();
            if (target != null) {
               CrystalGolemMob.this.showAttack(target.getX(), target.getY(), true);
            } else {
               CrystalGolemMob.this.showAttack(CrystalGolemMob.this.getX() + 100, CrystalGolemMob.this.getY(), true);
            }
         }
      });
      this.stickShootAbility = this.registerAbility(new CoordinateMobAbility() {
         @Override
         protected void run(int x, int y) {
            CrystalGolemMob.this.shootX = x;
            CrystalGolemMob.this.shootY = y;
            CrystalGolemMob.this.shootTarget = null;
         }
      });
      this.shootAbility = this.registerAbility(new CoordinateMobAbility() {
         @Override
         protected void run(int x, int y) {
            CrystalGolemMob.this.shootAbilityProjectile(x, y);
         }
      });
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      if (this.getLevel() != null) {
         return this.buffManager.getModifier(BuffModifiers.CAN_BREAK_OBJECTS)
            ? this.getLevel().regionManager.CAN_BREAK_OBJECTS_OPTIONS
            : this.getLevel().regionManager.CAN_OPEN_DOORS_OPTIONS;
      } else {
         return null;
      }
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CrystalGolemMob.CrystalGolemAI<>(544, 320, this.isSummoned ? 960 : 384));
   }

   @Override
   public float getAttackingMovementModifier() {
      return 0.0F;
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.isClient()) {
         if (this.shootTime != 0L) {
            long timer = this.shootTime - this.getWorldEntity().getTime();
            Float target;
            if (this.shootTarget != null) {
               target = new Float(this.shootTarget.x, this.shootTarget.y);
            } else {
               target = new Float(this.shootX, this.shootY);
            }

            this.setFacingDir(target.x - this.x, target.y - this.y);
            RayLinkedList<LevelObjectHit> rays = GameUtils.castRay(
               this.getLevel(), this.x, this.y, target.x - this.x, target.y - this.y, 1000.0, 0, new CollisionFilter().projectileCollision()
            );
            if (this.warningBeam == null) {
               this.warningBeam = new ParticleBeamHandler(this.getLevel())
                  .particleSize(10, 12)
                  .particleThicknessMod(0.2F)
                  .endParticleSize(8, 12)
                  .distPerParticle(20.0F)
                  .thickness(10, 2)
                  .speed(50.0F)
                  .height(24.0F)
                  .sprite(new GameSprite(GameResources.chains, 7, 0, 32));
            }

            int alpha = GameMath.limit(GameMath.lerp((float)timer / (chargeTime + stickTime), 255, 0), 0, 255);
            this.warningBeam.color(this.getWarningBeamColor(alpha));
            this.warningBeam.update(rays, delta);
         } else if (this.warningBeam != null) {
            this.warningBeam.dispose();
            this.warningBeam = null;
         }
      }
   }

   public Color getWarningBeamColor(int alpha) {
      return new Color(249, 198, 255, alpha);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickShooting();
      if (this.beamChargeSoundPlayer != null) {
         this.beamChargeSoundPlayer.refreshLooping();
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickShooting();
   }

   private void tickShooting() {
      if (this.shootTime != 0L) {
         long timer = this.shootTime - this.getWorldEntity().getTime();
         if (this.isClient()) {
            if (timer <= 0L) {
               this.shootTime = 0L;
               return;
            }

            this.spawnChargeParticles();
         } else if (timer <= stickTime && this.shootTarget != null && this.isSamePlace(this.shootTarget)) {
            this.stickShootAbility.runAndSend(this.shootTarget.getX(), this.shootTarget.getY());
         } else if (timer <= 0L) {
            this.shootAbility.runAndSend(this.shootX, this.shootY);
            this.shootTime = 0L;
         }
      }
   }

   public Projectile getProjectile(int targetX, int targetY, int distance) {
      return new CrystalGolemBeamProjectile(this.getLevel(), this, this.x, this.y, targetX, targetY, distance, damage, 20);
   }

   public void shootAbilityProjectile(int x, int y) {
      if (this.isServer()) {
         this.getLevel().entityManager.projectiles.add(this.getProjectile(x, y, 1000));
      }

      if (this.isClient()) {
         if (this.warningBeam != null) {
            this.warningBeam.dispose();
         }

         this.warningBeam = null;
         if (this.beamChargeSoundPlayer != null) {
            this.beamChargeSoundPlayer.stop();
         }

         SoundManager.playSound(GameResources.firespell1, SoundEffect.effect(this).pitch(1.8F).volume(0.5F).falloffDistance(1500));
      }

      this.shootTime = 0L;
   }

   public void spawnChargeParticles() {
      for (int i = 0; i < 2; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         float range = GameRandom.globalRandom.getFloatBetween(25.0F, 40.0F);
         float startX = this.x + dir.x * range;
         float startY = this.y + 4.0F;
         float endHeight = 29.0F;
         float startHeight = endHeight + dir.y * range;
         int lifeTime = GameRandom.globalRandom.getIntBetween(200, 500);
         float speed = dir.x * range * 250.0F / lifeTime;
         Color color1 = new Color(184, 174, 255);
         Color color2 = new Color(82, 210, 255);
         Color color3 = new Color(0, 191, 163);
         Color color4 = new Color(255, 125, 175);
         Color color = GameRandom.globalRandom.getOneOf(color1, color2, color3, color4);
         float hueMod = (float)this.getLevel().getWorldEntity().getLocalTime() / 10.0F % 240.0F;
         float glowHue = hueMod < 120.0F ? hueMod + 200.0F : 440.0F - hueMod;
         this.getLevel()
            .entityManager
            .addParticle(startX, startY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFades(10, 16)
            .rotates()
            .heightMoves(startHeight, endHeight)
            .movesConstant(-speed, 0.0F)
            .color(color)
            .givesLight(glowHue, 1.0F)
            .ignoreLight(true)
            .fadesAlphaTime(100, 50)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(), MobRegistry.Textures.crystalGolem, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY
               ),
               Particle.GType.IMPORTANT_COSMETIC
            );
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
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7 - 6;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      if (this.isAttacking) {
         sprite.x = 0;
      }

      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions drawOptions = MobRegistry.Textures.crystalGolem
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light.minLevelCopy(100.0F))
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            drawOptions.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.warningBeam != null) {
         this.warningBeam.dispose();
      }

      this.warningBeam = null;
      if (this.beamChargeSoundPlayer != null) {
         this.beamChargeSoundPlayer.stop();
      }
   }

   public static class CrystalGolemAI<T extends CrystalGolemMob> extends SequenceAINode<T> {
      public final EscapeAINode<T> escapeAINode;
      public final CooldownAttackTargetAINode<T> shootAtTargetNode;
      public final TargetFinderAINode<T> targetFinderNode;
      public final ChaserAINode<T> chaserNode;

      public CrystalGolemAI(int shootDistance, int meleeDistance, int searchDistance) {
         this.addChild(new InverterAINode<>(this.escapeAINode = new EscapeAINode<T>() {
            public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
               return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat();
            }
         }));
         if (shootDistance > 0) {
            this.addChild(
               this.shootAtTargetNode = new CooldownAttackTargetAINode<T>(
                  CooldownAttackTargetAINode.CooldownTimer.TICK, CrystalGolemMob.chargeTime + CrystalGolemMob.stickTime + 500, shootDistance
               ) {
                  public boolean attackTarget(T mob, Mob target) {
                     if (mob.canAttack() && mob.shootTime == 0L) {
                        mob.startShootingAbility.runAndSend(target);
                        return true;
                     } else {
                        return false;
                     }
                  }
               }
            );
            this.shootAtTargetNode.attackTimer = this.shootAtTargetNode.attackCooldown;
         } else {
            this.shootAtTargetNode = null;
         }

         TargetFinderDistance<T> targetFinder = new TargetFinderDistance<>(searchDistance);
         targetFinder.targetLostAddedDistance = searchDistance * 2;
         this.addChild(this.targetFinderNode = new TargetFinderAINode<T>(targetFinder) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
            }
         });
         this.addChild(this.chaserNode = new ChaserAINode<T>(meleeDistance, false, true) {
            public boolean attackTarget(T mob, Mob target) {
               return false;
            }
         });
      }
   }
}
