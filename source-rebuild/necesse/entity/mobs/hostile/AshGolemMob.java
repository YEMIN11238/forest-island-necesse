package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.util.tween.Easings;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.AshenWaveProjectile;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;

public class AshGolemMob extends HostileMob {
   public static LootTable lootTable = new LootTable(new LootItem("livingash"));
   public GameDamage collisionDamage = new GameDamage(160.0F);
   public GameDamage ashenWaveDamage = new GameDamage(80.0F);
   public static int projectileCooldown = 10000;

   public AshGolemMob() {
      super(3000);
      this.spawnLightThreshold = new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD, 0).min(150, Integer.MAX_VALUE);
      this.setArmor(100);
      this.setFriction(3.0F);
      this.setSpeed(20.0F);
      this.setKnockbackModifier(0.1F);
      this.prioritizeVerticalDir = false;
      this.collision = new Rectangle(-25, -15, 50, 30);
      this.hitBox = new Rectangle(-30, -20, 60, 40);
      this.selectBox = new Rectangle(-40, -50, 80, 80);
      this.swimMaskMove = 20;
      this.swimMaskOffset = -55;
      this.swimSinkOffset = -5;
   }

   @Override
   public Point getPathMoveOffset() {
      return new Point(32, 32);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new AshGolemMob.AshGolemAI<>(640, 64, 512));
   }

   @Override
   public double getPathBreakDownCost(Level level, int tileX, int tileY, GameObject object) {
      return 2.0;
   }

   @Override
   public int getPathBreakDownDamage(LevelObject lo) {
      return super.getPathBreakDownDamage(lo) * 12;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      float difficulty = Easings.CubicOut.ease(1.0F - this.getHealthPercent());
      this.setSpeed(GameMath.lerp(difficulty, 15, 50));
   }

   @Override
   public void serverTick() {
      super.serverTick();
      float difficulty = Easings.CubicOut.ease(1.0F - this.getHealthPercent());
      this.setSpeed(GameMath.lerp(difficulty, 15, 50));
      if (this.getLevel().getTileID(this.getTileX(), this.getTileY()) == TileRegistry.ashID) {
         this.spawnTilePosition = new Point(this.getTileX(), this.getTileY());
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.ashGolem, i, 16, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int spriteSize = 128;
      int drawX = camera.getDrawX(x) - spriteSize / 2;
      int drawY = camera.getDrawY(y) - spriteSize + 28;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.ashGolem
         .initDraw()
         .sprite(sprite.x, sprite.y, spriteSize)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY + 16);
      DrawOptions shadowOptions = MobRegistry.Textures.thrumbo_shadow.initDraw().sprite(0, sprite.y, spriteSize).light(light).pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      tileList.add(tm -> shadowOptions.draw());
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      return Math.abs(this.dx) <= 0.01F && Math.abs(this.dy) <= 0.01F
         ? new Point(0, dir)
         : new Point((int)(this.getDistanceRan() / this.getRockSpeed()) % 4 + 1, dir);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.stomp).fallOffDistance(2000).volume(0.6F).basePitch(1.2F).pitchVariance(0.1F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.cratebreak1).fallOffDistance(2000).volume(0.5F).pitchVariance(0.1F);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.CAN_BREAK_OBJECTS, true), new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
   }

   public static class AshGolemAI<T extends AshGolemMob> extends SelectorAINode<T> {
      public final CooldownAttackTargetAINode<T> shootAtTargetNode;
      public final TargetFinderAINode<T> targetFinderNode;
      public final ChaserAINode<T> chaserNode;

      public AshGolemAI(int shootDistance, int meleeDistance, int searchDistance) {
         SequenceAINode<T> chaserSequence = new SequenceAINode<>();
         this.addChild(chaserSequence);
         if (shootDistance > 0) {
            chaserSequence.addChild(
               this.shootAtTargetNode = new CooldownAttackTargetAINode<T>(
                  CooldownAttackTargetAINode.CooldownTimer.HAS_TARGET, AshGolemMob.projectileCooldown, shootDistance
               ) {
                  public boolean attackTarget(T mob, Mob target) {
                     if (mob.canAttack()) {
                        Projectile p = new AshenWaveProjectile(mob.getLevel(), mob.x, mob.y, target, 50.0F, 1000, mob.ashenWaveDamage, mob);
                        mob.getLevel().entityManager.projectiles.add(p);
                        return true;
                     } else {
                        return false;
                     }
                  }
               }
            );
            this.shootAtTargetNode.attackTimer = AshGolemMob.projectileCooldown / 2;
         } else {
            this.shootAtTargetNode = null;
         }

         TargetFinderDistance<T> targetFinder = new TargetFinderDistance<>(searchDistance);
         targetFinder.targetLostAddedDistance = 160;
         chaserSequence.addChild(this.targetFinderNode = new TargetFinderAINode<T>(targetFinder) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
            }

            public Point getBase(T mob, Blackboard<T> blackboard) {
               return mob.spawnTilePosition == null ? null : new Point(mob.spawnTilePosition.x * 32 + 16, mob.spawnTilePosition.y * 32 + 16);
            }
         });
         chaserSequence.addChild(this.chaserNode = new ChaserAINode<T>(meleeDistance, false, false) {
            public boolean attackTarget(T mob, Mob target) {
               if (mob.canAttack()) {
                  mob.attack(target.getX(), target.getY(), true);
                  target.isServerHit(mob.collisionDamage, target.x - mob.x, target.y - mob.y, 100.0F, mob);
                  return true;
               } else {
                  return false;
               }
            }
         });
         WandererAINode<T> wanderer = new WandererAINode<T>(20000) {
            @Override
            public WandererBaseOptions<T> getBaseOptions() {
               return new WandererBaseOptions<T>() {
                  public Point getBaseTile(T mob) {
                     return mob.spawnTilePosition;
                  }

                  public boolean forceFindAroundBase(T mob) {
                     return true;
                  }
               };
            }
         };
         this.addChild(wanderer);
      }
   }
}
