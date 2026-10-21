package necesse.entity.mobs.hostile.bosses;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import necesse.engine.GlobalData;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GamePoint3D;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.InverseKinematics;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.Entity;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.ConditionAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.IsolateRunningAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RemoveOnNoTargetNode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.FlyToOppositeDirectionAttackStage;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.FlyToRandomPositionAttackStage;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.IdleTimeAttackStage;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.EmpressAcidProjectile;
import necesse.entity.projectile.EmpressSlashProjectile;
import necesse.entity.projectile.EmpressSlashWarningProjectile;
import necesse.entity.projectile.EmpressWebBallProjectile;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SpiderEmpressMob extends FlyingBossMob {
   public static RotationLootItem vinylRotation = RotationLootItem.globalLootRotation(
      new LootItemList(),
      new LootItemList(),
      new LootItemList(),
      RotationLootItem.globalLootRotation(4, new LootItem("venomousreckoningvinyl"), new LootItem("wrathoftheempressvinyl"))
   );
   public static LootTable lootTable = new LootTable(new LootItemMultiplierIgnored(vinylRotation));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(new LootItem("empressmask", 1));
   public static LootTable privateLootTable = new LootTable(new LootItemMultiplierIgnored(uniqueDrops));
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(35000, 46000, 52000, 58000, 69000);
   public static float ACID_LINGER_SECONDS = 15.0F;
   public static float ACID_LINGER_SECONDS_RAGE = 10.0F;
   public static GameDamage collisionDamage = new GameDamage(115.0F);
   public static GameDamage rageCollisionDamage = new GameDamage(140.0F);
   public static GameDamage slashDamage = new GameDamage(115.0F);
   public static GameDamage webBallDamage = new GameDamage(100.0F);
   public static GameDamage acidProjectileDamage = new GameDamage(115.0F);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   private ArrayList<SpiderEmpressMob.SpiderEmpressLeg> frontLegs;
   private ArrayList<SpiderEmpressMob.SpiderEmpressLeg> backLegs;
   public SpiderEmpressMob.SpiderEmpressArm leftArm = new SpiderEmpressMob.SpiderEmpressArm(this, true);
   public SpiderEmpressMob.SpiderEmpressArm rightArm = new SpiderEmpressMob.SpiderEmpressArm(this, false);
   public float currentHeight;
   public boolean isRaging = false;
   public float rageProgress = 0.0F;
   public float passiveRageGenerationTime = 60.0F;
   public float passiveRageDecayTime = 30.0F;
   public ArrayList<Projectile> spawnedProjectiles = new ArrayList<>();
   protected final BooleanMobAbility setRagingAbility;
   protected final EmptyMobAbility setAcidBreathAnimationAbility;
   public boolean isAcidBreathing;
   public int acidBreathingParticleTimer;
   protected final EmptyMobAbility setWebVolleyAnimationAbility;
   protected final EmptyMobAbility setScreenSlashWarningAnimationAbility;
   protected final EmptyMobAbility setScreenSlashAttackAnimationAbility;
   protected final EmptyMobAbility setIdleAttackAnimationAbility;
   protected final EmptyMobAbility roarAbility;
   protected final EmptyMobAbility slashSoundAbility;
   protected final EmptyMobAbility acidSoundAbility;
   protected final EmptyMobAbility chargeSoundAbility;
   protected final EmptyMobAbility webBallSoundAbility;

   public SpiderEmpressMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.setSpeed(120.0F);
      this.setArmor(20);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-40, -30, 80, 80);
      this.hitBox = new Rectangle(-30, -90, 60, 120);
      this.selectBox = new Rectangle(-75, -120, 150, 200);
      this.setRaging(false);
      this.setRagingAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            SpiderEmpressMob.this.setRaging(value);
         }
      });
      this.setAcidBreathAnimationAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SpiderEmpressMob.this.isAcidBreathing = true;
               SpiderEmpressMob.this.addAcidBreathParticles();
               SpiderEmpressMob.this.leftArm.doAcidBreathAnimation();
               SpiderEmpressMob.this.rightArm.doAcidBreathAnimation();
            }
         }
      });
      this.setWebVolleyAnimationAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SpiderEmpressMob.this.leftArm.doWebVolleyAnimation();
               SpiderEmpressMob.this.rightArm.doWebVolleyAnimation();
            }
         }
      });
      this.setScreenSlashWarningAnimationAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SpiderEmpressMob.this.leftArm.doScreenSlashWarningAnimation();
               SpiderEmpressMob.this.rightArm.doScreenSlashWarningAnimation();
            }
         }
      });
      this.setScreenSlashAttackAnimationAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SpiderEmpressMob.this.leftArm.doScreenSlashAttackAnimation();
               SpiderEmpressMob.this.rightArm.doScreenSlashAttackAnimation();
            }
         }
      });
      this.setIdleAttackAnimationAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SpiderEmpressMob.this.isAcidBreathing = false;
               SpiderEmpressMob.this.leftArm.returnToIdleAnimation();
               SpiderEmpressMob.this.rightArm.returnToIdleAnimation();
            }
         }
      });
      this.roarAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SoundManager.playSound(GameResources.shears, SoundEffect.globalEffect().pitch(0.2F));
            }
         }
      });
      this.slashSoundAbility = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (SpiderEmpressMob.this.isClient()) {
                  SoundManager.playSound(
                     GameResources.magicbolt4, SoundEffect.globalEffect().volume(0.75F).pitch(GameRandom.globalRandom.getFloatBetween(0.75F, 0.8F))
                  );
               }
            }
         }
      );
      this.acidSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SoundManager.playSound(GameResources.spit, SoundEffect.globalEffect().volume(6.0F).pitch(0.45F));
            }
         }
      });
      this.chargeSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (SpiderEmpressMob.this.isClient()) {
               SoundManager.playSound(GameResources.roar, SoundEffect.globalEffect().volume(2.0F).pitch(3.0F));
            }
         }
      });
      this.webBallSoundAbility = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (SpiderEmpressMob.this.isClient()) {
                  SoundManager.playSound(
                     GameResources.swing1, SoundEffect.globalEffect().volume(0.2F).pitch(GameRandom.globalRandom.getFloatBetween(0.75F, 0.8F))
                  );
               }
            }
         }
      );
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      this.setRaging(this.isRaging);
      writer.putNextBoolean(this.isAcidBreathing);
      this.leftArm.setupSpawnPacket(writer);
      this.rightArm.setupSpawnPacket(writer);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.isRaging = reader.getNextBoolean();
      this.isAcidBreathing = reader.getNextBoolean();
      this.leftArm.applySpawnPacket(reader);
      this.rightArm.applySpawnPacket(reader);
   }

   @Override
   public void setupHealthPacket(PacketWriter writer, boolean isFull) {
      this.scaling.setupHealthPacket(writer, isFull);
      super.setupHealthPacket(writer, isFull);
   }

   @Override
   public void applyHealthPacket(PacketReader reader, boolean isFull) {
      this.scaling.applyHealthPacket(reader, isFull);
      super.applyHealthPacket(reader, isFull);
   }

   @Override
   public void setMaxHealth(int maxHealth) {
      super.setMaxHealth(maxHealth);
      if (this.scaling != null) {
         this.scaling.updatedMaxHealth();
      }
   }

   @Override
   public void setHealthHidden(int health, float knockbackX, float knockbackY, Attacker attacker, boolean fromNetworkUpdate) {
      int beforeHealth = this.getHealth();
      super.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
      int deltaHealth = beforeHealth - this.getHealth();
      if (deltaHealth > 0) {
         this.rageProgress = this.rageProgress + (float)deltaHealth / this.getMaxHealth();
      }
   }

   @Override
   public void setPos(float x, float y, boolean isDirect) {
      super.setPos(x, y, isDirect);
      if (isDirect && this.backLegs != null && this.frontLegs != null) {
         for (SpiderEmpressMob.SpiderEmpressLeg leg : this.backLegs) {
            leg.snapToPosition();
         }

         for (SpiderEmpressMob.SpiderEmpressLeg leg : this.frontLegs) {
            leg.snapToPosition();
         }
      }
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new SpiderEmpressMob.SpiderCastleBossAI<>(), new FlyingAIMover());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.spiderempressbegin, SoundEffect.effect(this).volume(1.2F).falloffDistance(4000));
      }

      this.currentHeight = this.getDesiredHeight();
      this.frontLegs = new ArrayList<>();
      this.backLegs = new ArrayList<>();
      int legCount = 8;
      float[] angles = new float[]{65.0F, 95.0F, 125.0F, 155.0F, -65.0F, -95.0F, -125.0F, -155.0F};

      for (int i = 0; i < legCount; i++) {
         final float angle = angles[i] - 90.0F;
         boolean mirror = angles[i] > 0.0F;
         float offsetPercent = (float)(i + (i % 2 == 0 ? 4 : 0)) / legCount % 1.0F;
         final Float dir = GameMath.getAngleDir(angle);
         float maxLeftAngle = 170.0F;
         float maxRightAngle = 170.0F;
         if (dir.x < 0.0F) {
            maxRightAngle = 0.0F;
         } else if (dir.x > 0.0F) {
            maxLeftAngle = 0.0F;
         }

         SpiderEmpressMob.SpiderEmpressLeg leg = new SpiderEmpressMob.SpiderEmpressLeg(this, 80.0F, offsetPercent, maxLeftAngle, maxRightAngle, mirror) {
            @Override
            public GamePoint3D getCenterPosition() {
               int dist = 10;
               return new GamePoint3D(
                  SpiderEmpressMob.this.getDrawX() + dir.x * dist,
                  SpiderEmpressMob.this.getDrawY() + dir.y * dist * 0.2F,
                  SpiderEmpressMob.this.getFlyingHeight() + 30
               );
            }

            @Override
            public GamePoint3D getDesiredPosition() {
               Float dirx = GameMath.getAngleDir(angle);
               int dist = SpiderEmpressMob.this.isRaging ? 90 : 60;
               float moveMod = Math.min(SpiderEmpressMob.this.getCurrentSpeed() / (SpiderEmpressMob.this.isRaging ? 200.0F : 150.0F), 1.0F);
               Float moveDir = GameMath.normalize(SpiderEmpressMob.this.dx, SpiderEmpressMob.this.dy);
               if (moveDir.y < 0.0F) {
                  moveMod *= 1.0F + -moveDir.y * 80.0F / SpiderEmpressMob.this.getSpeed();
               }

               return new GamePoint3D(
                  SpiderEmpressMob.this.getDrawX() + dirx.x * dist + moveDir.x * dist * moveMod,
                  SpiderEmpressMob.this.getDrawY() + dirx.y * dist + moveDir.y * dist * moveMod,
                  0.0F
               );
            }

            @Override
            public float getJumpHeight() {
               return 40.0F;
            }
         };
         if (dir.y < 0.0F) {
            this.backLegs.add(leg);
         } else {
            this.frontLegs.add(leg);
         }
      }

      this.frontLegs.sort(Comparator.comparingDouble(l -> l.y));
      this.backLegs.sort(Comparator.comparingDouble(l -> l.y));
   }

   @Override
   public void tickMovement(float delta) {
      float desiredHeight = this.getDesiredHeight();
      float heightDelta = desiredHeight - this.currentHeight;
      float heightSpeed = Math.abs(heightDelta) * 2.0F + 10.0F;
      float heightToMove = heightSpeed * delta / 250.0F;
      if (Math.abs(heightDelta) < heightToMove) {
         this.currentHeight = desiredHeight;
      } else {
         this.currentHeight = this.currentHeight + Math.signum(heightDelta) * heightToMove;
      }

      super.tickMovement(delta);

      for (SpiderEmpressMob.SpiderEmpressLeg leg : this.backLegs) {
         leg.tickMovement(delta);
      }

      for (SpiderEmpressMob.SpiderEmpressLeg leg : this.frontLegs) {
         leg.tickMovement(delta);
      }

      this.leftArm.tickMovement(delta);
      this.rightArm.tickMovement(delta);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.WrathOfTheEmpress, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      if (this.isRaging) {
         this.addRageParticles();
      }

      if (this.isAcidBreathing) {
         for (this.acidBreathingParticleTimer += 50; this.acidBreathingParticleTimer >= 25; this.acidBreathingParticleTimer -= 25) {
            this.addAcidBreathParticles();
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
      if (!this.isRaging) {
         this.rageProgress = this.rageProgress + 1.0F / (this.passiveRageGenerationTime * 20.0F);
      } else {
         this.rageProgress = this.rageProgress - 1.0F / (this.passiveRageDecayTime * 20.0F);
      }
   }

   protected void setRaging(boolean isRaging) {
      this.isRaging = isRaging;
      if (isRaging) {
         this.setArmor(60);
      } else {
         this.setArmor(20);
      }

      if (isRaging && this.isClient()) {
         SoundManager.playSound(GameResources.shears, SoundEffect.globalEffect().pitch(0.2F));
         this.addInitialRageParticles();
      }
   }

   @Override
   public int getFlyingHeight() {
      return (int)this.currentHeight;
   }

   public float getDesiredHeight() {
      float perc = GameUtils.getAnimFloat(this.getWorldEntity().getTime(), 1000);
      float height = GameMath.sin(perc * 360.0F) * 5.0F;
      return this.isAcidBreathing ? height - 20.0F : 20 + (int)height - (this.isRaging ? 20 : 0);
   }

   @Override
   public int getRespawnTime() {
      return this.isSummoned ? BossMob.getBossRespawnTime(this) : super.getRespawnTime();
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public LootTable getPrivateLootTable() {
      return privateLootTable;
   }

   @Override
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.spiderempresshurt, SoundEffect.effect(this).volume(0.3F).pitch(pitch).falloffDistance(1500));
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.spiderempressdeath, SoundEffect.effect(this).falloffDistance(3000));
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.isRaging ? rageCollisionDamage : collisionDamage;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 150;
   }

   @Override
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
   }

   @Override
   public void setFacingDir(float deltaX, float deltaY) {
      if (deltaX < 0.0F) {
         this.setDir(0);
      } else if (deltaX > 0.0F) {
         this.setDir(1);
      }
   }

   private void addInitialRageParticles() {
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      float anglePerParticle = 9.0F;

      for (int i = 0; i < 40; i++) {
         int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(140, 150);
         float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(130, 140) * 0.8F;
         this.getLevel()
            .entityManager
            .addParticle(this, typeSwitcher.next())
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 22))
            .sizeFades(30, 100)
            .movesFriction(dx, dy, 0.8F)
            .color(new Color(228, 92, 95))
            .heightMoves(0.0F, 10.0F)
            .lifeTime(1500);
      }
   }

   private void addRageParticles() {
      GameRandom random = GameRandom.globalRandom;

      for (int i = 0; i < 5; i++) {
         this.getLevel()
            .entityManager
            .addParticle(this, random.getIntBetween(-25, 25), random.getIntBetween(-20, 20) * 0.75F - 25.0F, Particle.GType.CRITICAL)
            .color(new Color(228, 92, 95))
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .movesConstant(0.0F, -5.0F)
            .height(75.0F)
            .lifeTime(1000)
            .givesLight(0.0F, 75.0F)
            .sizeFades(32, 48);
      }

      for (int i = 0; i < 1; i++) {
         this.getLevel()
            .entityManager
            .addParticle(this.x + random.getIntBetween(-25, 25), this.y + random.getIntBetween(-20, 20) * 0.75F, Particle.GType.CRITICAL)
            .color(new Color(228, 92, 95))
            .movesConstant(0.0F, -5.0F)
            .height(75.0F)
            .lifeTime(1000)
            .givesLight(0.0F, 75.0F)
            .sizeFades(16, 24);
      }
   }

   public void addAcidBreathParticles() {
      for (int i = 0; i < 5; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         float range = GameRandom.globalRandom.getFloatBetween(25.0F, 40.0F);
         float startX = this.x + dir.x * range;
         float startY = this.y + 20.0F;
         float endHeight = 29.0F;
         float startHeight = endHeight + dir.y * range;
         int lifeTime = GameRandom.globalRandom.getIntBetween(200, 500);
         float speed = dir.x * range * 250.0F / lifeTime;
         this.getLevel()
            .entityManager
            .addTopParticle(startX, startY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFades(10, 26)
            .rotates()
            .height(this.currentHeight)
            .heightMoves(startHeight, endHeight)
            .movesConstant(-speed, 0.0F)
            .color(new Color(166, 204, 52))
            .givesLight(74.0F, 1.0F)
            .fadesAlphaTime(100, 50)
            .lifeTime(lifeTime);
         this.getLevel()
            .entityManager
            .addTopParticle(startX, startY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.bubbleParticle.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFades(10, 16)
            .rotates()
            .height(this.currentHeight)
            .heightMoves(startHeight, endHeight)
            .movesConstant(speed * 2.0F, 0.0F)
            .color(new Color(166, 204, 52))
            .givesLight(74.0F, 1.0F)
            .fadesAlphaTime(100, 50)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 7; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.spiderEmpressDebris,
                  i,
                  0,
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
      int drawX = camera.getDrawX(x);
      int drawY = camera.getDrawY(y);
      drawY -= this.getFlyingHeight();
      float rotate = GameMath.limit(this.dx / 10.0F, -10.0F, 10.0F);
      GameTexture textureHead;
      GameTexture textureTorso;
      GameTexture textureDress;
      if (this.isRaging) {
         textureHead = MobRegistry.Textures.spiderEmpressRageHead;
         textureTorso = MobRegistry.Textures.spiderEmpressRageTorso;
         textureDress = MobRegistry.Textures.spiderEmpressRageDress;
      } else {
         textureHead = MobRegistry.Textures.spiderEmpressHead;
         textureTorso = MobRegistry.Textures.spiderEmpressTorso;
         textureDress = MobRegistry.Textures.spiderEmpressDress;
      }

      DrawOptions dress = textureDress.initDraw()
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(rotate * 4.0F, MobRegistry.Textures.spiderEmpressDress.getWidth() / 2, MobRegistry.Textures.spiderEmpressDress.getHeight())
         .posMiddle(drawX - (int)(rotate * 2.5F), drawY + 20 - 57 + (int)(rotate <= 0.0F ? rotate : -rotate));
      DrawOptions body = textureTorso.initDraw()
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(rotate * 2.0F, MobRegistry.Textures.spiderEmpressTorso.getWidth() / 2, MobRegistry.Textures.spiderEmpressTorso.getHeight())
         .posMiddle(drawX, drawY - 18 - 57);
      DrawOptions head = textureHead.initDraw()
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(rotate * 0.6F, MobRegistry.Textures.spiderEmpressHead.getWidth() / 2, MobRegistry.Textures.spiderEmpressHead.getHeight() + 150)
         .posMiddle(drawX, drawY - 54 - 57);
      DrawOptionsList legsShadows = new DrawOptionsList();
      DrawOptionsList backLegsDrawBottom = new DrawOptionsList();
      DrawOptionsList backLegsDrawsTop = new DrawOptionsList();
      DrawOptionsList frontLegsDrawBottom = new DrawOptionsList();
      DrawOptionsList frontLegsDrawsTop = new DrawOptionsList();

      for (SpiderEmpressMob.SpiderEmpressLeg leg : this.backLegs) {
         leg.addDrawOptions(legsShadows, backLegsDrawBottom, backLegsDrawsTop, level, camera, this.isRaging, perspective);
      }

      for (SpiderEmpressMob.SpiderEmpressLeg leg : this.frontLegs) {
         leg.addDrawOptions(legsShadows, frontLegsDrawBottom, frontLegsDrawsTop, level, camera, this.isRaging, perspective);
      }

      DrawOptionsList armsDrawHand = new DrawOptionsList();
      DrawOptionsList armsDrawBottom = new DrawOptionsList();
      DrawOptionsList armsDrawTop = new DrawOptionsList();
      this.leftArm.addDrawOptions(armsDrawHand, armsDrawBottom, armsDrawTop, level, x + (int)rotate, y + (int)rotate, camera, this.isRaging, perspective);
      this.rightArm.addDrawOptions(armsDrawHand, armsDrawBottom, armsDrawTop, level, x + (int)rotate, y - (int)rotate, camera, this.isRaging, perspective);
      topList.add(tm -> {
         legsShadows.draw();
         dress.draw();
         backLegsDrawBottom.draw();
         backLegsDrawsTop.draw();
         frontLegsDrawBottom.draw();
         frontLegsDrawsTop.draw();
         armsDrawTop.draw();
         body.draw();
         head.draw();
         armsDrawBottom.draw();
         armsDrawHand.draw();
      });
   }

   @Override
   public float getSoundPositionY() {
      return super.getSoundPositionY() - 40.0F;
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 24;
      int drawY = y - 42;
      MobRegistry.Textures.spiderEmpressHead.initDraw().sprite(0, 0, 62, 54).size(48, 48).draw(drawX, drawY);
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-24, -34, 48, 34);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      super.remove(knockbackX, knockbackY, attacker, isDeath);
      this.spawnedProjectiles.forEach(Projectile::remove);
      this.spawnedProjectiles.clear();
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.0F));
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      attackers.stream()
         .map(Attacker::getFirstPlayerOwner)
         .filter(Objects::nonNull)
         .filter(PlayerMob::isServerClient)
         .map(PlayerMob::getServerClient)
         .distinct()
         .forEach(c -> c.sendChatMessage(new LocalMessage("misc", "bossdefeat", "name", this.getLocalization())));
      if (!this.isDamagedByPlayers) {
         AchievementManager.checkMeAndThisArmyKill(this.getLevel(), attackers);
      }
   }

   @Override
   protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
      super.addHoverTooltips(tooltips, debug);
      if (debug) {
         tooltips.add("Raging: " + this.isRaging);
      }
   }

   public static class AcidWaveStage<T extends SpiderEmpressMob> extends AINode<T> implements AttackStageInterface<T> {
      private boolean useRageVariant;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.useRageVariant = mob.isRaging;
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target == null) {
            return AINodeResult.FAILURE;
         }

         float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
         float healthPercInv = Math.abs(healthPerc - 1.0F);
         if (this.useRageVariant) {
            float speed = 120.0F + healthPercInv * 60.0F;

            for (int i = -1; i <= 1; i++) {
               EmpressAcidProjectile projectile = new EmpressAcidProjectile(
                  mob.getLevel(), mob.x, mob.y, target.x, target.y, speed, 1000, SpiderEmpressMob.acidProjectileDamage, mob
               );
               projectile.setAngle(projectile.getAngle() + 10 * i);
               mob.getLevel().entityManager.projectiles.add(projectile);
               mob.spawnedProjectiles.add(projectile);
            }

            mob.acidSoundAbility.runAndSend();
         } else {
            float speed = 120.0F + healthPercInv * 40.0F;
            EmpressAcidProjectile projectile = new EmpressAcidProjectile(
               mob.getLevel(), mob.x, mob.y, target.x, target.y, speed, 1000, SpiderEmpressMob.acidProjectileDamage, mob
            );
            mob.getLevel().entityManager.projectiles.add(projectile);
            mob.spawnedProjectiles.add(projectile);
            mob.acidSoundAbility.runAndSend();
         }

         return AINodeResult.SUCCESS;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class AcidWaveWindupStage<T extends SpiderEmpressMob> extends AINode<T> implements AttackStageInterface<T> {
      private float timer;
      private final Function<T, Integer> duration;

      public AcidWaveWindupStage(Function<T, Integer> duration) {
         this.duration = duration;
      }

      public AcidWaveWindupStage(int noHealthIdleTime, int fullHealthIdleTime) {
         this(m -> {
            int delta = fullHealthIdleTime - noHealthIdleTime;
            float healthPerc = (float)m.getHealth() / m.getMaxHealth();
            return noHealthIdleTime + (int)(delta * healthPerc);
         });
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.timer = 0.0F;
         mob.setAcidBreathAnimationAbility.runAndSend();
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         this.timer += 50.0F;
         if (this.timer > this.duration.apply(mob).intValue()) {
            mob.setIdleAttackAnimationAbility.runAndSend();
            return AINodeResult.SUCCESS;
         } else {
            return AINodeResult.RUNNING;
         }
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class ChargeTargetStage<T extends SpiderEmpressMob> extends FlyToOppositeDirectionAttackStage<T> {
      public ChargeTargetStage() {
         super(true, 250.0F, 0.0F);
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         super.onStarted(mob, blackboard);
         if (blackboard.mover.isMoving()) {
            mob.chargeSoundAbility.runAndSend();
            mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SPIDER_CHARGE, mob, 5.0F, null), true);
         }
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
         super.onEnded(mob, blackboard);
         mob.buffManager.removeBuff(BuffRegistry.SPIDER_CHARGE, true);
      }
   }

   public static class CheckRageTransitionStage<T extends SpiderEmpressMob> extends AINode<T> implements AttackStageInterface<T> {
      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (0.0F < mob.rageProgress && mob.rageProgress < 1.0F) {
            return AINodeResult.SUCCESS;
         }

         if (mob.rageProgress < 0.0F && mob.isRaging) {
            mob.setRagingAbility.runAndSend(false);
         } else if (mob.rageProgress > 1.0F && !mob.isRaging) {
            mob.setRagingAbility.runAndSend(true);
         }

         return AINodeResult.SUCCESS;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class ScreenSlashStage<T extends SpiderEmpressMob> extends AINode<T> implements AttackStageInterface<T> {
      private int slashesRemaining;
      private final ArrayList<java.lang.Float> attackAngles = new ArrayList<>();
      private final ArrayList<Point> attackPos = new ArrayList<>();
      private float timeSinceStart = 0.0F;
      private float timeBeforeSlash;
      private float slashBuffer;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.slashesRemaining = mob.isRaging ? 20 : 10;
         this.timeSinceStart = 0.0F;
         this.slashBuffer = 0.0F;
         mob.setScreenSlashWarningAnimationAbility.runAndSend();
         this.calculateTimeBeforeSlash(mob);
      }

      private void calculateTimeBeforeSlash(T mob) {
         float defaultTime = mob.isRaging ? 750.0F : 1000.0F;
         float healthPercInv = Math.abs(mob.getHealthPercent() - 1.0F);
         this.timeBeforeSlash = defaultTime - healthPercInv * 250.0F;
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target == null) {
            return AINodeResult.RUNNING;
         }

         if (this.timeSinceStart >= this.timeBeforeSlash) {
            mob.setScreenSlashAttackAnimationAbility.runAndSend();
            if (this.attackAngles.isEmpty()) {
               mob.setIdleAttackAnimationAbility.runAndSend();
               return AINodeResult.SUCCESS;
            }

            float angle = this.attackAngles.get(0);
            int attackX = this.attackPos.get(0).x;
            int attackY = this.attackPos.get(0).y;
            EmpressSlashProjectile slashProjectile = new EmpressSlashProjectile(attackX, attackY, angle, SpiderEmpressMob.slashDamage, mob);
            mob.getLevel().entityManager.projectiles.add(slashProjectile);
            mob.spawnedProjectiles.add(slashProjectile);
            EmpressSlashProjectile slashProjectileReverse = new EmpressSlashProjectile(attackX, attackY, angle - 180.0F, SpiderEmpressMob.slashDamage, mob);
            mob.getLevel().entityManager.projectiles.add(slashProjectileReverse);
            mob.spawnedProjectiles.add(slashProjectileReverse);
            mob.slashSoundAbility.runAndSend();
            this.attackAngles.remove(0);
            this.attackPos.remove(0);
            if (this.slashesRemaining < 1) {
               return AINodeResult.RUNNING;
            }
         }

         GameRandom random = GameRandom.globalRandom;
         this.timeSinceStart += 50.0F;

         for (this.slashBuffer += 50.0F; this.slashesRemaining > 0 && this.slashBuffer > 50.0F; this.slashesRemaining--) {
            int randomX = random.getIntBetween(-200, 200);
            int randomY = random.getIntBetween(-200, 200);
            float angle = random.getIntBetween(-180, 180);
            EmpressSlashWarningProjectile slashProjectile = new EmpressSlashWarningProjectile(
               (int)(target.x + randomX), (int)(target.y + randomY), angle, SpiderEmpressMob.slashDamage, mob
            );
            mob.getLevel().entityManager.projectiles.add(slashProjectile);
            mob.spawnedProjectiles.add(slashProjectile);
            EmpressSlashWarningProjectile slashProjectileReverse = new EmpressSlashWarningProjectile(
               (int)(target.x + randomX), (int)(target.y + randomY), angle - 180.0F, SpiderEmpressMob.slashDamage, mob
            );
            mob.getLevel().entityManager.projectiles.add(slashProjectileReverse);
            mob.spawnedProjectiles.add(slashProjectileReverse);
            this.attackAngles.add(angle);
            this.attackPos.add(new Point((int)(target.x + randomX), (int)(target.y + randomY)));
            this.slashBuffer -= 50.0F;
         }

         return AINodeResult.RUNNING;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class SpiderCastleBossAI<T extends SpiderEmpressMob> extends SequenceAINode<T> {
      public SpiderCastleBossAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         this.addChild(new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         int maxIdleTime = 500;
         int minIdleTime = 100;
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         this.addChild(new ConditionAINode<>(new IsolateRunningAINode<>(attackStages), m -> !m.isRaging, AINodeResult.SUCCESS));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new SpiderEmpressMob.ScreenSlashStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new SpiderEmpressMob.WebVolleyWindupStage<>(minIdleTime, maxIdleTime));
         attackStages.addChild(new SpiderEmpressMob.WebVolleyStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new SpiderEmpressMob.ChargeTargetStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new SpiderEmpressMob.ScreenSlashStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new SpiderEmpressMob.AcidWaveWindupStage<>(minIdleTime, maxIdleTime));
         attackStages.addChild(new SpiderEmpressMob.AcidWaveStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new SpiderEmpressMob.ChargeTargetStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         maxIdleTime = 400;
         minIdleTime = 50;
         attackStages = new AttackStageManagerNode<>();
         this.addChild(new ConditionAINode<>(new IsolateRunningAINode<>(attackStages), m -> m.isRaging, AINodeResult.SUCCESS));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 400));
         final int ragingMaxIdleTime = maxIdleTime;
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, ragingMaxIdleTime)));

         for (int i = 0; i < 3; i++) {
            attackStages.addChild(new SpiderEmpressMob.ScreenSlashStage<>());
         }

         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 400));
         attackStages.addChild(new SpiderEmpressMob.WebVolleyWindupStage<>(minIdleTime, maxIdleTime));
         attackStages.addChild(new SpiderEmpressMob.WebVolleyStage<>());
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 400));
         attackStages.addChild(new SpiderEmpressMob.WebVolleyWindupStage<>(minIdleTime, maxIdleTime));
         attackStages.addChild(new SpiderEmpressMob.WebVolleyStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));

         for (int i = 0; i < 2; i++) {
            attackStages.addChild(new SpiderEmpressMob.ChargeTargetStage<>());
         }

         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 400));
         attackStages.addChild(new SpiderEmpressMob.AcidWaveWindupStage<>(minIdleTime, maxIdleTime));
         attackStages.addChild(new SpiderEmpressMob.AcidWaveStage<>());
         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 100));
         attackStages.addChild(new SpiderEmpressMob.AcidWaveWindupStage<>(minIdleTime, maxIdleTime));
         attackStages.addChild(new SpiderEmpressMob.AcidWaveStage<>());

         for (int i = 0; i < 2; i++) {
            attackStages.addChild(new SpiderEmpressMob.ChargeTargetStage<>());
         }

         attackStages.addChild(new SpiderEmpressMob.CheckRageTransitionStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
      }

      private int getIdleTime(T mob, int maxTime) {
         float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
         return (int)(maxTime * healthPerc);
      }
   }

   public static class SpiderEmpressArm {
      private final SpiderEmpressMob mob;
      private final boolean isLeftHand;
      private boolean targetReached;
      private boolean isAttacking;
      private float handRotation;
      private float targetHandRotation;
      private float moveSpeed = 50.0F;
      private float targetX;
      private float targetY;
      public float currentX;
      public float currentY;
      public InverseKinematics limbs;

      public SpiderEmpressArm(SpiderEmpressMob mob, boolean isLeftHand) {
         this.isLeftHand = isLeftHand;
         this.mob = mob;
         this.targetX = isLeftHand ? 50.0F : -50.0F;
         this.targetY = -100.0F;
         this.currentX = this.targetX;
         this.currentY = this.targetY;
         this.handRotation = 0.0F;
         this.targetHandRotation = 0.0F;
         this.isAttacking = false;
         if (isLeftHand) {
            this.limbs = InverseKinematics.startFromPoints(15.0F, -90.0F, 35.0F, -90.0F, 140.0F, -10.0F);
            this.limbs.addJointPoint(65.0F, -90.0F);
         } else {
            this.limbs = InverseKinematics.startFromPoints(-15.0F, -90.0F, -35.0F, -90.0F, -10.0F, 140.0F);
            this.limbs.addJointPoint(-65.0F, -90.0F);
         }
      }

      public void setupSpawnPacket(PacketWriter writer) {
         writer.putNextFloat(this.currentX);
         writer.putNextFloat(this.currentY);
         writer.putNextFloat(this.targetX);
         writer.putNextFloat(this.targetY);
         writer.putNextFloat(this.handRotation);
         writer.putNextFloat(this.targetHandRotation);
      }

      public void applySpawnPacket(PacketReader reader) {
         this.currentX = reader.getNextFloat();
         this.currentY = reader.getNextFloat();
         this.targetX = reader.getNextFloat();
         this.targetY = reader.getNextFloat();
         this.handRotation = reader.getNextFloat();
         this.targetHandRotation = reader.getNextFloat();
      }

      public void setTarget(float targetX, float targetY, float targetHandRotation) {
         this.targetX = targetX;
         this.targetY = targetY;
         this.targetHandRotation = targetHandRotation;
         this.targetReached = false;
      }

      public void tickMovement(float delta) {
         double dist = GameMath.preciseDistance(this.targetX, this.targetY, this.currentX, this.currentY);
         if (dist == 0.0) {
            this.targetReached = true;
            if (!this.isAttacking) {
               this.doIdleAnimation();
            }
         } else {
            float distToMove = this.moveSpeed * delta / 250.0F;
            if (dist <= distToMove) {
               this.currentX = this.targetX;
               this.currentY = this.targetY;
               this.targetReached = true;
            } else {
               Float dir = GameMath.normalize(this.targetX - this.currentX, this.targetY - this.currentY);
               this.currentX = this.currentX + dir.x * distToMove;
               this.currentY = this.currentY + dir.y * distToMove;
            }

            this.limbs.apply(this.currentX, this.currentY, 0.1F, 0.5F, 100);
         }

         if (this.targetHandRotation != this.handRotation) {
            float handMoveSpeed = this.moveSpeed * delta / 250.0F;
            float deltaHandRotation = this.targetHandRotation - this.handRotation;
            if (Math.abs(deltaHandRotation) < handMoveSpeed) {
               this.handRotation = this.targetHandRotation;
            } else {
               this.handRotation = this.handRotation + handMoveSpeed * Math.signum(deltaHandRotation);
            }
         }
      }

      public void doAcidBreathAnimation() {
         this.moveSpeed = 100.0F;
         this.isAttacking = true;
         this.setTarget(this.isLeftHand ? 20.0F : -20.0F, 0.0F, this.isLeftHand ? 20.0F : -20.0F);
      }

      public void doWebVolleyAnimation() {
         this.moveSpeed = 100.0F;
         this.isAttacking = true;
         this.setTarget(this.isLeftHand ? 30.0F : -30.0F, -40.0F, this.isLeftHand ? 90.0F : -90.0F);
      }

      public void doScreenSlashWarningAnimation() {
         this.moveSpeed = 100.0F;
         this.isAttacking = true;
         this.setTarget(this.isLeftHand ? 60.0F : -60.0F, -150.0F, this.isLeftHand ? -130.0F : 130.0F);
      }

      public void doScreenSlashAttackAnimation() {
         this.moveSpeed = 200.0F;
         this.setTarget(this.isLeftHand ? 40.0F : -40.0F, -10.0F, this.isLeftHand ? 20.0F : -20.0F);
      }

      public void returnToIdleAnimation() {
         this.moveSpeed = 30.0F;
         this.isAttacking = false;
         this.setTarget(this.isLeftHand ? 50.0F : -50.0F, -100.0F, 0.0F);
      }

      public void doIdleAnimation() {
         if (this.targetReached) {
            if ((this.currentX == 60.0F || this.currentX == -60.0F) && this.currentY == -75.0F) {
               this.setTarget(this.isLeftHand ? 50.0F : -50.0F, -100.0F, 0.0F);
            } else {
               this.setTarget(this.isLeftHand ? 60.0F : -60.0F, -75.0F, 0.0F);
            }

            this.moveSpeed = 10.0F;
         }
      }

      public void addDrawOptions(
         DrawOptionsList armsDrawHand,
         DrawOptionsList armsDrawBottom,
         DrawOptionsList armsDrawTop,
         Level level,
         int x,
         int y,
         GameCamera camera,
         boolean isRaging,
         PlayerMob perspective
      ) {
         if (!this.mob.isServer()) {
            int currentHeight = (int)this.mob.currentHeight;

            for (InverseKinematics.Limb limb : this.limbs.limbs) {
               GameLight light = level.getLightLevel(Entity.getTileCoordinate(x), Entity.getTileCoordinate(y));
               if (this.limbs.limbs.getLast() == limb) {
                  GameTexture texture = isRaging ? MobRegistry.Textures.spiderEmpressRageArmBottom : MobRegistry.Textures.spiderEmpressArmBottom;
                  int textureWidth = texture.getWidth();
                  int textureHeight = texture.getHeight();
                  GameTexture handTexture = isRaging ? MobRegistry.Textures.spiderEmpressRageHand : MobRegistry.Textures.spiderEmpressHand;
                  int handTextureWidth = handTexture.getWidth();
                  int handTextureHeight = handTexture.getHeight();
                  TextureDrawOptions drawOptions;
                  TextureDrawOptions handDrawOptions;
                  if (this.isLeftHand) {
                     drawOptions = texture.initDraw()
                        .mirrorX()
                        .rotate(limb.angle + 20.0F, 4, textureHeight - 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(textureWidth, textureHeight)
                        .pos(camera.getDrawX(limb.inboundX - 4.0F) + x, camera.getDrawY(limb.inboundY - textureHeight / 2.0F - 4.0F) + y - currentHeight);
                     handDrawOptions = handTexture.initDraw()
                        .mirrorX()
                        .rotate(this.handRotation, 10, 8)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(handTextureWidth, handTextureHeight)
                        .pos(camera.getDrawX(limb.outboundX - 10.0F) + x, camera.getDrawY(limb.outboundY - 8.0F) + y - currentHeight);
                     armsDrawHand.add(handDrawOptions);
                  } else {
                     drawOptions = texture.initDraw()
                        .rotate(limb.angle - 200.0F, textureWidth - 4, textureHeight - 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(textureWidth, textureHeight)
                        .pos(
                           camera.getDrawX(limb.inboundX - (textureWidth - 4)) + x,
                           camera.getDrawY(limb.inboundY - textureHeight / 2.0F - 4.0F) + y - currentHeight
                        );
                     handDrawOptions = handTexture.initDraw()
                        .rotate(this.handRotation, handTextureWidth - 10, 8)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(handTextureWidth, handTextureHeight)
                        .pos(camera.getDrawX(limb.outboundX - (handTextureWidth - 10)) + x, camera.getDrawY(limb.outboundY - 8.0F) + y - currentHeight);
                  }

                  armsDrawBottom.add(drawOptions);
                  armsDrawHand.add(handDrawOptions);
               } else {
                  GameTexture texture = isRaging ? MobRegistry.Textures.spiderEmpressRageArmTop : MobRegistry.Textures.spiderEmpressArmTop;
                  int textureWidth = texture.getWidth();
                  int textureHeight = texture.getHeight();
                  TextureDrawOptions drawOptions;
                  if (this.isLeftHand) {
                     drawOptions = texture.initDraw()
                        .mirrorX()
                        .rotate(limb.angle - 45.0F, 4, 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(textureWidth, textureHeight)
                        .pos(camera.getDrawX(limb.inboundX - 4.0F) + x, camera.getDrawY(limb.inboundY - 4.0F) + y - currentHeight);
                  } else {
                     drawOptions = texture.initDraw()
                        .rotate(limb.angle - 135.0F, textureWidth - 4, 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(textureWidth, textureHeight)
                        .pos(camera.getDrawX(limb.inboundX - (textureWidth - 4)) + x, camera.getDrawY(limb.inboundY - 4.0F) + y - currentHeight);
                  }

                  armsDrawTop.add(drawOptions);
               }
            }

            if (GlobalData.debugActive()) {
               Color color = this.isLeftHand ? Color.BLUE : Color.RED;
               armsDrawTop.add(() -> this.limbs.drawDebug(camera, x, y - currentHeight, color, Color.GREEN));
            }
         }
      }
   }

   public abstract static class SpiderEmpressLeg {
      public final Mob mob;
      public float startX;
      public float startY;
      public float x;
      public float y;
      public float nextX;
      public float nextY;
      public boolean isMoving;
      private InverseKinematics ik;
      private List<java.awt.geom.Line2D.Float> shadowLines;
      public float maxLeftAngle;
      public float maxRightAngle;
      private final float moveAtDist;
      private float checkX;
      private float checkY;
      private float distBuffer;
      private final boolean mirror;

      public SpiderEmpressLeg(Mob mob, float moveAtDist, float offsetPercent, float maxLeftAngle, float maxRightAngle, boolean mirror) {
         this.mob = mob;
         this.moveAtDist = moveAtDist;
         this.distBuffer = moveAtDist * offsetPercent;
         this.maxLeftAngle = maxLeftAngle;
         this.maxRightAngle = maxRightAngle;
         this.mirror = mirror;
         this.snapToPosition();
         this.checkX = this.x;
         this.checkY = this.y;
      }

      public void snapToPosition() {
         GamePoint3D desiredPosition = this.getDesiredPosition();
         this.x = desiredPosition.x;
         this.y = desiredPosition.y;
         this.nextX = desiredPosition.x;
         this.nextY = desiredPosition.y;
         this.updateIK();
      }

      public void tickMovement(float delta) {
         GamePoint3D centerPos = this.getCenterPosition();
         double checkDist = new Float(centerPos.x, centerPos.y).distance(this.checkX, this.checkY);
         this.distBuffer = (float)(this.distBuffer + checkDist);
         this.checkX = centerPos.x;
         this.checkY = centerPos.y;
         if (checkDist == 0.0) {
            this.distBuffer = this.distBuffer + delta / (this.moveAtDist / 5.0F);
         }

         if (!this.isMoving) {
            GamePoint3D desiredPos = this.getDesiredPosition();
            double desiredDist = new Float(desiredPos.x, desiredPos.y).distance(this.x, this.y);
            if (desiredDist > 175.0) {
               this.distBuffer = this.distBuffer + this.moveAtDist;
            }

            if (this.distBuffer >= this.moveAtDist) {
               this.distBuffer = this.distBuffer - this.moveAtDist;
               if (this.x != desiredPos.x || this.y != desiredPos.y) {
                  this.startX = this.x;
                  this.startY = this.y;
                  this.nextX = desiredPos.x;
                  this.nextY = desiredPos.y;
                  this.isMoving = true;
               }
            }
         }

         if (this.isMoving) {
            double nextDist = new Float(this.x, this.y).distance(this.nextX, this.nextY);
            float speed = (float)nextDist * 2.0F + this.mob.getSpeed() * 1.2F;
            float distToMove = speed * delta / 250.0F;
            if (nextDist < distToMove) {
               if (this.mob.isClient()) {
                  SoundManager.playSound(
                     GameResources.tap2, SoundEffect.effect(this.x, this.y).volume(0.1F).pitch(GameRandom.globalRandom.getFloatBetween(0.5F, 0.75F))
                  );
               }

               this.x = this.nextX;
               this.y = this.nextY;
               this.isMoving = false;
            } else {
               Float dir = GameMath.normalize(this.nextX - this.x, this.nextY - this.y);
               this.x = this.x + dir.x * distToMove;
               this.y = this.y + dir.y * distToMove;
            }
         }

         this.updateIK();
      }

      public void updateIK() {
         if (!this.mob.isServer()) {
            GamePoint3D centerPos = this.getCenterPosition();
            Float dir = GameMath.normalize(this.x - centerPos.x, this.y - centerPos.y);
            float jointDistMod = 0.5F;
            float dist = (float)new Float(centerPos.x, centerPos.y).distance(this.x, this.y) * jointDistMod;
            float jointHeight = 20.0F;
            GamePoint3D jointPos = centerPos.dirFromLength(centerPos.x + dir.x * dist, centerPos.y + dir.y * dist, jointHeight, 40.0F);
            GamePoint3D footPos = jointPos.dirFromLength(this.x, this.y, 0.0F, 70.0F);
            float jumpHeight = this.getJumpHeight();
            float perspectiveMod = 0.6F;
            synchronized (this) {
               this.shadowLines = Collections.synchronizedList(new LinkedList<>());
               this.shadowLines
                  .add(new java.awt.geom.Line2D.Float(centerPos.x, centerPos.y + 18.0F, jointPos.x, jointPos.y - jointPos.height * perspectiveMod));
               this.shadowLines.add(new java.awt.geom.Line2D.Float(jointPos.x, jointPos.y - jointPos.height * perspectiveMod, this.x, this.y));
            }

            this.ik = InverseKinematics.startFromPoints(
               centerPos.x,
               centerPos.y - centerPos.height * perspectiveMod - jumpHeight,
               jointPos.x,
               jointPos.y - jointPos.height * perspectiveMod - jumpHeight,
               this.maxLeftAngle,
               this.maxRightAngle
            );
            this.ik.addJointPoint(footPos.x, footPos.y - footPos.height * perspectiveMod - jumpHeight);
            this.ik.apply(this.x, this.y - this.getCurrentLegLift() - Math.max(jumpHeight - 150.0F, jumpHeight / 4.0F), 0.0F, 2.0F, 500);
         }
      }

      public float getCurrentLegLift() {
         double startDist = new Float(this.startX, this.startY).distance(this.nextX, this.nextY);
         float lift = Math.min((float)startDist / 40.0F, 1.0F) * 20.0F + 5.0F;
         double currentDist = new Float(this.x, this.y).distance(this.nextX, this.nextY);
         double progress = GameMath.limit(currentDist / startDist, 0.0, 1.0);
         return GameMath.sin((float)progress * 180.0F) * lift;
      }

      public abstract float getJumpHeight();

      public abstract GamePoint3D getDesiredPosition();

      public abstract GamePoint3D getCenterPosition();

      public void addDrawOptions(
         DrawOptionsList legsShadows,
         DrawOptionsList legsDrawBottom,
         DrawOptionsList legsDrawsTop,
         Level level,
         GameCamera camera,
         boolean isRaging,
         PlayerMob perspective
      ) {
         if (!this.mob.isServer()) {
            int legShadowWidth = MobRegistry.Textures.queenSpiderLeg_shadow.getWidth() / 2;
            synchronized (this) {
               for (java.awt.geom.Line2D.Float shadowLine : this.shadowLines) {
                  GameLight light = level.getLightLevel(
                     Entity.getTileCoordinate((shadowLine.x1 + shadowLine.x2) / 2.0F), Entity.getTileCoordinate((shadowLine.y1 + shadowLine.y2) / 2.0F)
                  );
                  float angle = GameMath.getAngle(new Float(shadowLine.x1 - shadowLine.x2, shadowLine.y1 - shadowLine.y2));
                  float length = (float)shadowLine.getP1().distance(shadowLine.getP2());
                  TextureDrawOptions drawOptions = MobRegistry.Textures.queenSpiderLeg_shadow
                     .initDraw()
                     .rotate(angle + 90.0F, legShadowWidth / 2, 6)
                     .light(light)
                     .size(legShadowWidth, (int)length + 16)
                     .pos(camera.getDrawX(shadowLine.x1 - legShadowWidth / 2.0F), camera.getDrawY(shadowLine.y1));
                  legsShadows.add(drawOptions);
               }
            }

            float jumpHeight = this.getJumpHeight();

            for (InverseKinematics.Limb limb : this.ik.limbs) {
               int legTextureWidth = MobRegistry.Textures.spiderEmpressLegBottom.getWidth() + 2;
               GameLight light = level.getLightLevel(
                  Entity.getTileCoordinate((limb.inboundX + limb.outboundX) / 2.0F),
                  Entity.getTileCoordinate((limb.inboundY + limb.outboundY) / 2.0F + jumpHeight)
               );
               if (this.ik.limbs.getLast() == limb) {
                  GameTexture texture = isRaging ? MobRegistry.Textures.spiderEmpressRageLegBottom : MobRegistry.Textures.spiderEmpressLegBottom;
                  TextureDrawOptions drawOptions;
                  if (this.mirror) {
                     drawOptions = texture.initDraw()
                        .mirrorX()
                        .rotate(limb.angle - 90.0F, legTextureWidth / 2, 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(legTextureWidth, (int)limb.length + 16)
                        .pos(camera.getDrawX(limb.inboundX - legTextureWidth / 2.0F), camera.getDrawY(limb.inboundY) - 8);
                  } else {
                     drawOptions = texture.initDraw()
                        .rotate(limb.angle - 90.0F, legTextureWidth / 2, 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(legTextureWidth, (int)limb.length + 16)
                        .pos(camera.getDrawX(limb.inboundX - legTextureWidth / 2.0F), camera.getDrawY(limb.inboundY) - 8);
                  }

                  legsDrawBottom.add(drawOptions);
               } else {
                  GameTexture texture = isRaging ? MobRegistry.Textures.spiderEmpressRageLegTop : MobRegistry.Textures.spiderEmpressLegTop;
                  legTextureWidth = texture.getWidth();
                  TextureDrawOptions drawOptions;
                  if (this.mirror) {
                     drawOptions = texture.initDraw()
                        .mirrorX()
                        .rotate(limb.angle - 90.0F, legTextureWidth / 2, 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(legTextureWidth, (int)limb.length + 16)
                        .pos(camera.getDrawX(limb.inboundX - legTextureWidth / 2.0F), camera.getDrawY(limb.inboundY) - 8);
                  } else {
                     drawOptions = texture.initDraw()
                        .rotate(limb.angle - 90.0F, legTextureWidth / 2, 4)
                        .startGlowOptions(level, this.mob.getID())
                        .light(light)
                        .applyEnemyTracker(this.mob, perspective)
                        .size(legTextureWidth, (int)limb.length + 16)
                        .pos(camera.getDrawX(limb.inboundX - legTextureWidth / 2.0F), camera.getDrawY(limb.inboundY) - 8);
                  }

                  legsDrawsTop.add(drawOptions);
               }
            }

            if (GlobalData.debugActive()) {
               legsDrawsTop.add(
                  () -> Renderer.drawCircle(camera.getDrawX(this.x), camera.getDrawY(this.y - this.getCurrentLegLift()), 4, 12, 1.0F, 0.0F, 0.0F, 1.0F, false)
               );
               legsDrawsTop.add(() -> this.ik.drawDebug(camera, Color.RED, Color.GREEN));
            }
         }
      }
   }

   public static class WebVolleyStage<T extends SpiderEmpressMob> extends AINode<T> implements AttackStageInterface<T> {
      private boolean useRageVariant;
      private boolean initial;
      public int direction;
      private float angleBuffer;
      private float remainingAngle;
      private float totalAngle;
      private float currentAngleLeft;
      private float currentAngleRight;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.useRageVariant = mob.isRaging;
         this.initial = true;
         this.angleBuffer = 0.0F;
         this.totalAngle = 180.0F;
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (this.initial) {
            Mob target = blackboard.getObject(Mob.class, "currentTarget");
            if (target == null) {
               return AINodeResult.SUCCESS;
            }

            this.currentAngleLeft = -6.0F;
            this.currentAngleRight = 6.0F;
            this.remainingAngle = this.totalAngle;
            this.initial = false;
         }

         this.angleBuffer = this.angleBuffer + this.totalAngle * 50.0F / 500.0F;
         float anglePerProjectile = this.useRageVariant ? 10.0F : 15.0F;

         while (this.angleBuffer >= anglePerProjectile) {
            this.currentAngleLeft += anglePerProjectile;
            EmpressWebBallProjectile projectileLeft = new EmpressWebBallProjectile(
               mob.x, mob.y, this.currentAngleLeft, SpiderEmpressMob.webBallDamage, 120.0F, mob
            );
            mob.getLevel().entityManager.projectiles.add(projectileLeft);
            mob.spawnedProjectiles.add(projectileLeft);
            mob.webBallSoundAbility.runAndSend();
            this.currentAngleRight -= anglePerProjectile;
            EmpressWebBallProjectile projectileRight = new EmpressWebBallProjectile(
               mob.x, mob.y, this.currentAngleRight, SpiderEmpressMob.webBallDamage, 120.0F, mob
            );
            mob.getLevel().entityManager.projectiles.add(projectileRight);
            mob.spawnedProjectiles.add(projectileRight);
            this.angleBuffer -= anglePerProjectile;
            this.remainingAngle -= anglePerProjectile;
            mob.webBallSoundAbility.runAndSend();
            if (this.remainingAngle < 1.0F) {
               break;
            }
         }

         return this.angleBuffer >= this.remainingAngle ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class WebVolleyWindupStage<T extends SpiderEmpressMob> extends AINode<T> implements AttackStageInterface<T> {
      private float timer;
      private final Function<T, Integer> duration;

      public WebVolleyWindupStage(Function<T, Integer> duration) {
         this.duration = duration;
      }

      public WebVolleyWindupStage(int noHealthIdleTime, int fullHealthIdleTime) {
         this(m -> {
            int delta = fullHealthIdleTime - noHealthIdleTime;
            float healthPerc = (float)m.getHealth() / m.getMaxHealth();
            return noHealthIdleTime + (int)(delta * healthPerc);
         });
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.timer = 0.0F;
         mob.setWebVolleyAnimationAbility.runAndSend();
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         this.timer += 50.0F;
         if (this.timer > this.duration.apply(mob).intValue()) {
            mob.setIdleAttackAnimationAbility.runAndSend();
            return AINodeResult.SUCCESS;
         } else {
            return AINodeResult.RUNNING;
         }
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }
}
