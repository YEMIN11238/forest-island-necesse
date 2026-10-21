package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.MovedRectangle;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.engine.util.tween.Easings;
import necesse.engine.util.tween.FloatTween;
import necesse.engine.util.tween.PlayableSequence;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHitCooldowns;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.SucceederAINode;
import necesse.entity.mobs.ai.behaviourTree.event.ConfuseWanderAIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ConfusedWandererAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.LooseTargetTimerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MosquitoMob extends HostileMob {
   public static LootTable lootTable = new LootTable();
   public static GameDamage collisionDamage = new GameDamage(65.0F);
   public final int searchDistance = 320;
   protected final PlayableSequence landSequence = new PlayableSequence();
   protected final PlayableSequence flySequence = new PlayableSequence();
   protected final EmptyMobAbility landAbility;
   protected final EmptyMobAbility flyAbility;
   protected final FloatTween greenTween = new FloatTween(1000.0, 1.0F, 0.0F).setEase(Easings.ExpoOut);
   protected final int animationFPS = 11;
   protected final int randomAnimationOffset;
   protected final float flySpeed = 50.0F;
   protected final FloatTween flySpeedTween = new FloatTween(50.0F).onValueChanged(this::setSpeed);
   public float healOnAttackPercent = 0.15F;
   public float attackDistance = 10.0F;
   protected boolean isFlying = true;
   protected final FloatTween flyOffsetTween = new FloatTween(-16.0F);
   protected float attackAngle;
   protected long spawnTime;
   protected boolean shouldPlaySound = false;
   protected boolean allowedToPlaySound = false;
   protected SoundPlayer buzzSoundPlayer;

   public MosquitoMob() {
      super(300);
      this.setSpeed(50.0F);
      this.setArmor(25);
      this.setFriction(2.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-9, -7, 18, 14);
      this.selectBox = new Rectangle(-20, -34, 40, 32);
      this.hitBox = new Rectangle(-13, -16, 26, 30);
      this.randomAnimationOffset = GameRandom.globalRandom.nextInt(900);
      double landTime = 500.0;
      this.landSequence
         .addAt(0.0, this.flyOffsetTween.newTween(landTime, 0.0F))
         .addAtTheSameTime(this.flySpeedTween.newTween(landTime, 0.0F))
         .onComplete(() -> {
            this.isFlying = false;
            this.shouldPlaySound = false;
         })
         .onPlay(this.flySequence::kill)
         .setEase(Easings.SineOut);
      this.flySequence
         .addAt(0.0, this.flyOffsetTween.newTween(landTime, -16.0F).setEase(Easings.SineOut))
         .addAtTheSameTime(this.flySpeedTween.newTween(landTime, 50.0F).setEase(Easings.QuadIn))
         .onPlay(() -> {
            this.landSequence.kill();
            this.isFlying = true;
            this.shouldPlaySound = true;
         });
      this.landAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            MosquitoMob.this.landSequence.play(MosquitoMob.this.getTime(), MosquitoMob.this.getTime());
         }
      });
      this.flyAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            MosquitoMob.this.flySequence.play(MosquitoMob.this.getTime(), MosquitoMob.this.getTime());
         }
      });
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addBoolean("isFlying", this.isFlying);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.isFlying = save.getBoolean("isFlying", true);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.spawnTime);
      writer.putNextBoolean(this.isFlying);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.spawnTime = reader.getNextLong();
      this.isFlying = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.spawnTime == 0L ? this.getTime() : this.spawnTime;
      this.attackAngle = this.attackAngle == 0.0F ? GameRandom.globalRandom.nextFloat() * 360.0F : this.attackAngle;
      this.ai = new BehaviourTreeAI<>(this, new MosquitoMob.MosquitoMobAI<>(this), new AIMover());
      this.greenTween.play(this.spawnTime, this.getTime());
      if (this.isFlying) {
         this.shouldPlaySound = true;
      } else {
         this.greenTween.complete(true);
         this.landSequence.play(this.getTime(), this.getTime());
         this.landSequence.complete(true);
      }
   }

   protected void playBuzzSound() {
      if (this.buzzSoundPlayer != null && !this.buzzSoundPlayer.isDone()) {
         this.buzzSoundPlayer.refreshLooping(1.0F);
      } else {
         this.buzzSoundPlayer = SoundManager.playSound(
            GameResources.mosquitoBuzz,
            SoundEffect.effect(this).volume(GameRandom.globalRandom.getFloatBetween(0.8F, 1.0F)).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F))
         );
         if (this.buzzSoundPlayer != null) {
            this.buzzSoundPlayer.setPosition(GameRandom.globalRandom.getFloatBetween(0.0F, GameResources.mosquitoBuzz.getLengthInSeconds()));
            this.buzzSoundPlayer.refreshLooping(1.0F);
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.landSequence.update(this.getTime());
      this.flySequence.update(this.getTime());
      if (this.isClient()) {
         PlayerMob player = this.getClient().getPlayer();
         if (player != null) {
            this.allowedToPlaySound = GameMath.squareDistance(player.x, player.y, this.x, this.y) < 800.0F;
         }
      }

      if (this.shouldPlaySound && this.allowedToPlaySound) {
         this.playBuzzSound();
      } else {
         this.buzzSoundPlayer = null;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.landSequence.update(this.getTime());
      this.flySequence.update(this.getTime());
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
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 32;
      int drawY = (int)(camera.getDrawY(y) - 32 + this.flyOffsetTween.getValue());
      int spriteCount = 4;
      int sprite = (int)((this.getTime() + this.randomAnimationOffset) / 1000.0 * 11.0) % spriteCount;
      if (!this.isFlying) {
         sprite = -2;
         drawY -= 15;
      }

      float rotate = this.dx / 10.0F;
      this.greenTween.update(this.getTime());
      Color color = new Color((int)(255.0F * (1.0F - this.greenTween.getValue())), 255, (int)(255.0F * (1.0F - this.greenTween.getValue())));
      final DrawOptions options = MobRegistry.Textures.mosquito
         .initDraw()
         .sprite(sprite + 2, this.getDir(), 64)
         .startGlowOptions(level, this.getID())
         .color(color)
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, 32, 32)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      int spriteCount = 4;
      int sprite = (int)((this.getTime() + this.randomAnimationOffset) / 1000.0 * 11.0) % spriteCount;
      if (!this.isFlying) {
         sprite = -2;
      }

      GameTexture shadowTexture = MobRegistry.Textures.mosquito_shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32 - 22;
      return shadowTexture.initDraw().sprite(sprite + 2, this.getDir(), 64).light(light).pos(drawX, drawY);
   }

   @Override
   public boolean canPushMob(Mob other) {
      return other instanceof MosquitoMob;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.mosquito, i, 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return DeathMessageTable.fromRange("mosquito", 3);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   static class MosquitoMobAI<T extends MosquitoMob> extends SelectorAINode<T> {
      public MosquitoMobAI(T mob) {
         final MosquitoMob.MosquitoMobAI.LandAINode<T> landAINode = new MosquitoMob.MosquitoMobAI.LandAINode<>(6000);
         ConfusedWandererAINode<T> confusedWandererAINode;
         this.addChild(confusedWandererAINode = new ConfusedWandererAINode<>());
         confusedWandererAINode.hitsCausesConfusionChance = 0.25F;
         SequenceAINode<T> chaserSequence = new SequenceAINode<>();
         this.addChild(chaserSequence);
         chaserSequence.addChild(new SucceederAINode<>(new LooseTargetTimerAINode<>()));
         TargetFinderAINode<T> targetFinderAINode = new TargetFinderAINode<T>(320) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
            }
         };
         targetFinderAINode.noTargetFoundMinCooldown = 500;
         targetFinderAINode.noTargetFoundMaxCooldown = 1000;
         chaserSequence.addChild(targetFinderAINode);
         chaserSequence.addChild(new MosquitoMob.MosquitoMobAI.FlyToTargetThenToDirectionAINode<>(mob.attackCooldown));
         chaserSequence.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mobx, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               landAINode.resetTimeToLand();
               return AINodeResult.SUCCESS;
            }
         });
         this.addChild(landAINode);
         this.addChild(new WandererAINode<>(5000));
      }

      static class FlyToTargetThenToDirectionAINode<T extends MosquitoMob> extends MoveTaskAINode<T> {
         public final MobHitCooldowns hitCooldowns;
         public long nextMoveUpdateTime = 0L;
         public Mob lastTarget;

         public FlyToTargetThenToDirectionAINode(int attackCooldown) {
            this.hitCooldowns = new MobHitCooldowns(attackCooldown);
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            if (target != null) {
               float distance = mob.getDistance(target);
               if (distance < 32.0F && this.hitCooldowns.canHit(target) && target.canBeHit(mob) && mob.getCollision().intersects(target.getCollision())) {
                  target.isServerHit(MosquitoMob.collisionDamage, target.x - mob.x, target.y - mob.y, 0.0F, mob);
                  mob.setHealth((int)(mob.getHealth() * (1.0F + mob.healOnAttackPercent)));
                  this.hitCooldowns.startCooldown(target);
                  if (GameRandom.globalRandom.getChance((0.4F - (float)mob.getHealth() / mob.getMaxHealth()) / 2.0F)) {
                     blackboard.submitEvent(
                        "confuseWander",
                        new ConfuseWanderAIEvent(GameRandom.globalRandom.getIntBetween(3000, 6000), GameMath.getAngleDir(mob.attackAngle - 180.0F))
                     );
                  }
               }
            }

            return super.tick(mob, blackboard);
         }

         public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            if (target != this.lastTarget) {
               this.nextMoveUpdateTime = 0L;
               this.lastTarget = target;
            }

            if (mob.getTime() >= this.nextMoveUpdateTime && target != null) {
               boolean shouldUsePathFinding = true;
               float distance = mob.getDistance(target);
               Float angleDir = GameMath.getAngleDir(mob.attackAngle);
               Float desiredPosition = new Float(target.x + angleDir.x * mob.attackDistance, target.y + angleDir.y * mob.attackDistance);
               if (distance < 100.0F) {
                  MovedRectangle movedRectangle = new MovedRectangle(mob, (int)desiredPosition.x, (int)desiredPosition.y);
                  if (!mob.getLevel().collides(movedRectangle, mob.modifyChasingCollisionFilter(mob.getLevelCollisionFilter(), target))) {
                     blackboard.mover
                        .setCustomMovement(this, new MobMovementRelative(target, angleDir.x * mob.attackDistance, angleDir.y * mob.attackDistance));
                     this.nextMoveUpdateTime = mob.getTime() + 1000L;
                     shouldUsePathFinding = false;
                  }
               }

               if (shouldUsePathFinding) {
                  return this.moveToTileTask(target.getTileX(), target.getTileY(), null, aiPathResult -> {
                     if (aiPathResult.moveIfWithin(-1, -1, () -> this.nextMoveUpdateTime = 0L)) {
                        int pathTime = aiPathResult.getNextPathTimeBasedOnPathTime(mob.getSpeed(), 1.5F, 500, 0.1F);
                        this.nextMoveUpdateTime = mob.getTime() + pathTime;
                        return AINodeResult.SUCCESS;
                     } else {
                        this.nextMoveUpdateTime = mob.getTime() + GameRandom.globalRandom.getIntBetween(5000, 8000);
                        return AINodeResult.FAILURE;
                     }
                  });
               }
            }

            return blackboard.mover.isCurrentlyMovingFor(this) ? AINodeResult.SUCCESS : AINodeResult.FAILURE;
         }
      }

      static class LandAINode<T extends MosquitoMob> extends AINode<T> {
         protected final int timeToLand;
         protected float timeToNextLand;
         protected boolean hasLanded;

         public LandAINode(int timeToLand) {
            this.timeToLand = timeToLand;
         }

         public void resetTimeToLand() {
            this.timeToNextLand = (float)this.mob().getTime() + GameRandom.globalRandom.getFloatBetween(this.timeToLand * 0.5F, this.timeToLand * 1.5F);
            if (this.hasLanded) {
               this.mob().flyAbility.runAndSend();
            }

            this.hasLanded = false;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            if (((MosquitoMob)this.mob()).isServer()) {
               if (!mob.isFlying) {
                  this.hasLanded = true;
               } else {
                  this.resetTimeToLand();
               }
            }
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (this.hasLanded) {
               return AINodeResult.SUCCESS;
            } else if ((float)mob.getTime() >= this.timeToNextLand && !this.hasLanded) {
               this.hasLanded = true;
               mob.landAbility.runAndSend();
               return AINodeResult.SUCCESS;
            } else {
               return AINodeResult.FAILURE;
            }
         }
      }
   }
}
