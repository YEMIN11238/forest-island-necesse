package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.Entity;
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
import necesse.entity.mobs.ai.behaviourTree.decorators.IsolateRunningAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RemoveOnNoTargetNode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RunningAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.CryoQuakeProjectile;
import necesse.entity.projectile.CryoShardProjectile;
import necesse.entity.projectile.CryoVolleyProjectile;
import necesse.entity.projectile.CryoWarningProjectile;
import necesse.entity.projectile.CryoWaveProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.pathProjectile.CryoQuakeCirclingProjectile;
import necesse.entity.projectile.pathProjectile.CryoWarningCirclingProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ConditionLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.MobConditionLootItemList;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CryoQueenMob extends FlyingBossMob {
   public static LootTable lootTable = new LootTable(new LootItemMultiplierIgnored(new ChanceLootItem(0.2F, "battleforthefrozenreignvinyl")));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      new LootItem("cryoquake"), new LootItem("cryospear"), new LootItem("cryoblaster"), new LootItem("cryoglaive")
   );
   public static LootTable privateLootTable = new LootTable(
      new MobConditionLootItemList(
         mob -> mob.getLevel() == null || !mob.getLevel().isIncursionLevel,
         new ConditionLootItem(
            "cryoheart",
            (r, o) -> {
               ServerClient client = LootTable.expectExtra(ServerClient.class, o, 1);
               return client != null
                  && client.playerMob.healthUpgradeManager.canUpgrade("cryoheart")
                  && client.playerMob.getInv().getAmount(ItemRegistry.getItem("cryoheart"), false, false, true, true, "have") == 0;
            }
         ),
         uniqueDrops
      )
   );
   public static MaxHealthGetter BASE_MAX_HEALTH = new MaxHealthGetter(8000, 14000, 18000, 21000, 26000);
   public static MaxHealthGetter INCURSION_MAX_HEALTH = new MaxHealthGetter(24000, 30000, 36000, 42000, 48000);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   public LinkedList<Mob> spawnedMobs = new LinkedList<>();
   public boolean attackingAnimation;
   public final EmptyMobAbility magicSoundAbility;
   public final EmptyMobAbility spiralSpawnSoundAbility;
   public final EmptyMobAbility jingleSoundAbility;
   public final EmptyMobAbility roarSoundAbility;
   public final EmptyMobAbility moveSoundAbility;
   public final BooleanMobAbility attackingAnimationAbility;
   public GameDamage collisionDamage = new GameDamage(60.0F);
   public GameDamage cryoQuakeDamage = new GameDamage(80.0F);
   public GameDamage cryoShardDamage = new GameDamage(70.0F);
   public GameDamage cryoWaveDamage = new GameDamage(80.0F);
   public GameDamage cryoVolleyDamage = new GameDamage(70.0F);
   public static GameDamage baseCollisionDamage = new GameDamage(60.0F);
   public static GameDamage baseCryoQuakeDamage = new GameDamage(80.0F);
   public static GameDamage baseCryoShardDamage = new GameDamage(70.0F);
   public static GameDamage baseCryoWaveDamage = new GameDamage(80.0F);
   public static GameDamage baseCryoVolleyDamage = new GameDamage(70.0F);
   public static GameDamage incursionCollisionDamage = new GameDamage(90.0F);
   public static GameDamage incursionCryoQuakeDamage = new GameDamage(120.0F);
   public static GameDamage incursionCryoShardDamage = new GameDamage(120.0F);
   public static GameDamage incursionCryoWaveDamage = new GameDamage(120.0F);
   public static GameDamage incursionCryoVolleyDamage = new GameDamage(110.0F);

   public CryoQueenMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(BASE_MAX_HEALTH);
      this.moveAccuracy = 60;
      this.setSpeed(110.0F);
      this.setArmor(35);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-40, -80, 80, 105);
      this.hitBox = new Rectangle(-40, -80, 80, 105);
      this.selectBox = new Rectangle(-40, -90, 80, 120);
      this.magicSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (CryoQueenMob.this.isClient()) {
               SoundManager.playSound(GameResources.magicbolt1, SoundEffect.effect(CryoQueenMob.this).falloffDistance(2000));
            }
         }
      });
      this.spiralSpawnSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (CryoQueenMob.this.isClient()) {
               SoundManager.playSound(GameResources.shake, SoundEffect.effect(CryoQueenMob.this).falloffDistance(2000));
               SoundManager.playSound(GameResources.stomp, SoundEffect.effect(CryoQueenMob.this).falloffDistance(2000));
            }
         }
      });
      this.jingleSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (CryoQueenMob.this.isClient()) {
               SoundSettings jingle = new SoundSettings(GameResources.jingle).pitchVariance(0.04F).fallOffDistance(1200);
               SoundManager.playSound(jingle, CryoQueenMob.this);
               SoundSettings magicBolt = new SoundSettings(GameResources.magicbolt2).volume(0.2F).basePitch(1.3F).pitchVariance(0.04F).fallOffDistance(1200);
               SoundManager.playSound(magicBolt, CryoQueenMob.this);
            }
         }
      });
      this.roarSoundAbility = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (CryoQueenMob.this.isClient()) {
                  SoundManager.playSound(
                     new SoundSettings(GameResources.roar).volume(0.7F).basePitch(1.3F).pitchVariance(0.04F).fallOffDistance(1200), CryoQueenMob.this
                  );
               }
            }
         }
      );
      this.moveSoundAbility = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (CryoQueenMob.this.isClient()) {
                  SoundManager.playSound(
                     new SoundSettings(GameResources.swoosh).volume(0.2F).basePitch(0.5F).pitchVariance(0.0F).fallOffDistance(1400), CryoQueenMob.this
                  );
               }
            }
         }
      );
      this.attackingAnimationAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            CryoQueenMob.this.attackingAnimation = value;
         }
      });
      this.ambientSoundCooldownMin = 5000;
      this.ambientSoundCooldownMax = 10000;
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.attackingAnimation);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.attackingAnimation = reader.getNextBoolean();
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
   public void init() {
      if (this.getLevel() instanceof IncursionLevel) {
         this.difficultyChanges.setMaxHealth(INCURSION_MAX_HEALTH);
         this.setHealth(this.getMaxHealth());
         this.collisionDamage = incursionCollisionDamage;
         this.cryoQuakeDamage = incursionCryoQuakeDamage;
         this.cryoShardDamage = incursionCryoShardDamage;
         this.cryoWaveDamage = incursionCryoWaveDamage;
         this.cryoVolleyDamage = incursionCryoVolleyDamage;
      } else {
         this.collisionDamage = baseCollisionDamage;
         this.cryoQuakeDamage = baseCryoQuakeDamage;
         this.cryoShardDamage = baseCryoShardDamage;
         this.cryoWaveDamage = baseCryoWaveDamage;
         this.cryoVolleyDamage = baseCryoVolleyDamage;
      }

      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CryoQueenMob.CryoQueenAI<>(), new FlyingAIMover());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.cryoqueenbegin, SoundEffect.effect(this).volume(1.2F).falloffDistance(4000));
      }
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
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.collisionDamage;
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
         this.setDir(1);
      } else if (deltaX > 0.0F) {
         this.setDir(0);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.BattleForTheFrozenReign, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(110.0F + healthPercInv * 70.0F);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(110.0F + healthPercInv * 70.0F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 6; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.cryoQueen, i, 17, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.cryoqueenhurt).volume(0.5F).fallOffDistance(1500);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.cryoqueendeath).pitchVariance(0.0F).fallOffDistance(3000);
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
      int drawX = camera.getDrawX(x) - 64;
      int drawY = camera.getDrawY(y) - 100;
      int dir = this.getDir();
      int frame = GameUtils.getAnim(this.getWorldEntity().getTime(), 5, 750);
      DrawOptions body = MobRegistry.Textures.cryoQueen
         .initDraw()
         .sprite(frame, this.attackingAnimation ? 0 : 1, 128)
         .size(128, 128)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .mirror(dir != 0, false)
         .pos(drawX, drawY);
      TextureDrawOptions shadowOptions = this.getShadowDrawOptions(level, x, y, light, camera);
      topList.add(tm -> {
         shadowOptions.draw();
         body.draw();
      });
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.reaper_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2 + 20;
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 16;
      int drawY = y - 16;
      MobRegistry.Textures.cryoQueen.initDraw().sprite(0, 7, 64).size(32, 32).mirror(this.getDir() != 0, false).draw(drawX, drawY);
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-16, -16, 32, 32);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.2F));
   }

   public boolean isSecondStage() {
      float healthPerc = (float)this.getHealth() / this.getMaxHealth();
      return healthPerc < 0.5F;
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

   public static class CryoPillar extends GroundPillar {
      public GameTextureSection texture;
      public boolean mirror = GameRandom.globalRandom.nextBoolean();

      public CryoPillar(int x, int y, double spawnDistance, long spawnTime) {
         super(x, y, spawnDistance, spawnTime);
         this.texture = MobRegistry.Textures.cryoQueen == null
            ? null
            : GameRandom.globalRandom
               .getOneOf(
                  new GameTextureSection(MobRegistry.Textures.cryoQueen).sprite(0, 6, 64),
                  new GameTextureSection(MobRegistry.Textures.cryoQueen).sprite(1, 6, 64),
                  new GameTextureSection(MobRegistry.Textures.cryoQueen).sprite(2, 6, 64)
               );
         this.behaviour = new GroundPillar.TimedBehaviour(300, 200, 800);
      }

      @Override
      public DrawOptions getDrawOptions(Level level, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
         GameLight light = level.getLightLevel(Entity.getTileCoordinate(this.x), Entity.getTileCoordinate(this.y));
         int drawX = camera.getDrawX(this.x);
         int drawY = camera.getDrawY(this.y);
         double height = this.getHeight(currentTime, distanceMoved);
         int endY = (int)(height * this.texture.getHeight());
         return this.texture
            .section(0, this.texture.getWidth(), 0, endY)
            .initDraw()
            .mirror(this.mirror, false)
            .light(light)
            .pos(drawX - this.texture.getWidth() / 2, drawY - endY);
      }
   }

   public static class CryoQuakeRotation<T extends CryoQueenMob> extends RunningAINode<T> {
      private int timer;
      private int index;
      private float startAngle;
      private boolean circling;
      private boolean clockwise;
      private java.awt.geom.Point2D.Float startPos;
      private boolean playedWarningSound;
      private boolean playedQuakeSound;
      public final int msPerProjectile = 30;
      public final int totalProjectiles = 18;
      public final int anglePerProjectile = 20;

      public void start(T mob, Blackboard<T> blackboard) {
         mob.attackingAnimationAbility.runAndSend(true);
         this.circling = GameRandom.globalRandom.nextBoolean();
         this.clockwise = GameRandom.globalRandom.nextBoolean();
         this.timer = 0;
         this.index = 0;
         this.playedWarningSound = false;
         this.playedQuakeSound = false;
         this.startPos = new java.awt.geom.Point2D.Float(mob.x, mob.y + 15.0F);
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            this.startAngle = (float)Math.toDegrees(Math.atan2(currentTarget.y - this.startPos.y, currentTarget.x - this.startPos.x)) + 90.0F - 40.0F;
         }

         this.startAngle = this.startAngle + GameRandom.globalRandom.getIntBetween(-5, 5);
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         for (this.timer += 50; this.timer >= 30; this.index++) {
            this.timer -= 30;
            float angle = this.startAngle + this.index * 20;
            if (this.index < 18) {
               if (!this.playedWarningSound) {
                  mob.magicSoundAbility.runAndSend();
                  this.playedWarningSound = true;
               }

               float speed = this.getProjectileSpeed(mob);
               if (this.circling) {
                  mob.getLevel()
                     .entityManager
                     .projectiles
                     .add(new CryoWarningCirclingProjectile(this.startPos.x, this.startPos.y, 20.0F, angle, this.clockwise, speed, 960));
               } else {
                  mob.getLevel().entityManager.projectiles.add(new CryoWarningProjectile(this.startPos.x, this.startPos.y, angle, speed, 1120));
               }
            } else {
               if (this.index >= 36) {
                  return AINodeResult.SUCCESS;
               }

               if (!this.playedQuakeSound) {
                  mob.spiralSpawnSoundAbility.runAndSend();
                  this.playedQuakeSound = true;
               }

               float speed = this.getProjectileSpeed(mob);
               if (this.circling) {
                  mob.getLevel()
                     .entityManager
                     .projectiles
                     .add(
                        new CryoQuakeCirclingProjectile(
                           this.startPos.x, this.startPos.y, 20.0F, angle, this.clockwise, speed, 960, mob.cryoQuakeDamage, 100, mob
                        )
                     );
               } else {
                  mob.getLevel()
                     .entityManager
                     .projectiles
                     .add(new CryoQuakeProjectile(this.startPos.x, this.startPos.y, angle, speed, 1120, mob.cryoQuakeDamage, 100, mob));
               }
            }
         }

         return AINodeResult.RUNNING;
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.attackingAnimationAbility.runAndSend(false);
      }

      protected float getProjectileSpeed(T mob) {
         float percInv = Math.abs((float)mob.getHealth() / mob.getMaxHealth() - 1.0F);
         return this.circling ? 80.0F + percInv * 140.0F : 100.0F + percInv * 180.0F;
      }
   }

   public static class CryoQueenAI<T extends CryoQueenMob> extends SequenceAINode<T> {
      public CryoQueenAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         this.addChild(new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         SequenceAINode<T> rotations = new SequenceAINode<>();
         rotations.addChild(new CryoQueenMob.MoveToRandomPosition<>(true, 300));
         rotations.addChild(new CryoQueenMob.IdleTime<>(100));
         rotations.addChild(new CryoQueenMob.CryoQuakeRotation<>());
         rotations.addChild(new CryoQueenMob.MoveToRandomPosition<>(true, 300));
         rotations.addChild(new CryoQueenMob.IdleTime<>(100));
         rotations.addChild(new CryoQueenMob.CryoShardRotation<>());
         rotations.addChild(new CryoQueenMob.MoveToRandomPosition<>(true, 300));
         rotations.addChild(new CryoQueenMob.IdleTime<>(100));
         rotations.addChild(new CryoQueenMob.CryoWaveRotation<>());

         for (int i = 0; i < 3; i++) {
            rotations.addChild(new CryoQueenMob.MoveToRandomPosition<>(false, 300));
            rotations.addChild(new CryoQueenMob.IdleTime<>(1000));
            rotations.addChild(new CryoQueenMob.CryoVolleyRotation<>());
         }

         this.addChild(new IsolateRunningAINode<>(rotations));
      }

      public void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         super.onRootSet(root, mob, blackboard);
         blackboard.onRemoved(e -> mob.spawnedMobs.forEach(Mob::remove));
      }
   }

   public static class CryoShardRotation<T extends CryoQueenMob> extends RunningAINode<T> {
      private int ticker;
      private int timerBuffer;
      private boolean reversed;

      public void start(T mob, Blackboard<T> blackboard) {
         blackboard.mover.stopMoving(mob);
         mob.attackingAnimationAbility.runAndSend(true);
         this.ticker = 0;
         this.timerBuffer = 0;
         this.reversed = !this.reversed;
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            int distance = GameMath.limit((int)mob.getDistance(currentTarget), 100, 500);
            mob.setMovement(new MobMovementCircleLevelPos(mob, currentTarget.x, currentTarget.y, distance, 2.0F, this.reversed));
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            this.timerBuffer += 50;
            float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
            int msPerShard = 150 + (int)(healthPerc * 200.0F);
            if (this.timerBuffer > msPerShard) {
               mob.jingleSoundAbility.runAndSend();
            }

            while (this.timerBuffer > msPerShard) {
               int speed = 100 + (int)(Math.abs(healthPerc - 1.0F) * 160.0F);
               this.timerBuffer -= msPerShard;
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(
                     new CryoShardProjectile(
                        mob.x,
                        mob.y,
                        currentTarget.x + GameRandom.globalRandom.getIntBetween(-20, 20),
                        currentTarget.y + GameRandom.globalRandom.getIntBetween(-20, 20),
                        speed,
                        1440,
                        mob.cryoShardDamage,
                        100,
                        mob
                     )
                  );
            }
         }

         this.ticker++;
         return this.ticker <= 100 ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.stopMoving();
         mob.attackingAnimationAbility.runAndSend(false);
      }
   }

   public static class CryoVolleyRotation<T extends CryoQueenMob> extends RunningAINode<T> {
      private int ticker;

      public void start(T mob, Blackboard<T> blackboard) {
         blackboard.mover.stopMoving(mob);
         mob.attackingAnimationAbility.runAndSend(true);
         this.ticker = 0;
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         this.ticker++;
         float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
         int maxTicks = 10 + (int)(20.0F * healthPerc * 1.5F);
         return this.ticker < maxTicks ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.attackingAnimationAbility.runAndSend(false);
         LinkedList<Float> firedAngles = new LinkedList<>();
         float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
         int speed = 150 + (int)(Math.abs(healthPerc - 1.0F) * 170.0F);
         GameUtils.streamServerClients(mob.getLevel())
            .map(c -> c.playerMob)
            .filter(m -> mob.getDistance(m) < 1120.0F)
            .forEach(
               target -> {
                  float angleToTarget = Projectile.getAngleToTarget(mob.x, mob.y, target.getX(), target.getY());
                  if (firedAngles.stream().noneMatch(f -> Math.abs(GameMath.getAngleDifference(angleToTarget, f)) < 35.0F)) {
                     java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angleToTarget + 90.0F);
                     int missiles = 6;
                     int perpRange = 150;
                     int perpPerMissile = perpRange / missiles;

                     for (int i = 0; i < missiles; i++) {
                        int offsetDistance = GameRandom.globalRandom.getIntBetween(50, 100);
                        java.awt.geom.Point2D.Float offset = new java.awt.geom.Point2D.Float(dir.x * offsetDistance, dir.y * offsetDistance);
                        int perp = i * perpPerMissile - perpRange / 2;
                        offset = GameMath.getPerpendicularPoint(offset, perp, dir);
                        java.awt.geom.Point2D.Float targetPos = new java.awt.geom.Point2D.Float(
                           target.x + GameRandom.globalRandom.getIntBetween(-50, 50), target.y + GameRandom.globalRandom.getIntBetween(-50, 50)
                        );
                        float var10003 = mob.x + offset.x;
                        mob.getLevel()
                           .entityManager
                           .projectiles
                           .add(
                              new CryoVolleyProjectile(
                                 var10003, mob.y + offset.y - 30.0F, targetPos.x, targetPos.y, speed, 1440, mob.cryoVolleyDamage, 100, mob
                              )
                           );
                     }

                     firedAngles.add(angleToTarget);
                  }
               }
            );
         mob.jingleSoundAbility.runAndSend();
      }
   }

   public static class CryoWaveRotation<T extends CryoQueenMob> extends RunningAINode<T> {
      private java.awt.geom.Point2D.Float attackDir = new java.awt.geom.Point2D.Float(1.0F, 0.0F);
      private boolean reverseDir;
      private int timerBuffer;

      public void start(T mob, Blackboard<T> blackboard) {
         mob.attackingAnimationAbility.runAndSend(true);
         this.reverseDir = !this.reverseDir;
         this.timerBuffer = 0;
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            float targetDistance = mob.getDistance(currentTarget);
            float perc = GameMath.limit((targetDistance - 500.0F) / 500.0F, 0.0F, 1.0F);
            float angle = 45.0F + perc * 35.0F;
            float travelDistance = GameMath.cos(angle) * targetDistance * 1.6F;
            float attackAngle = (float)Math.toDegrees(Math.atan2(currentTarget.y - mob.y, currentTarget.x - mob.x)) + (this.reverseDir ? angle : -angle);
            java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(attackAngle);
            this.attackDir = GameMath.getAngleDir(attackAngle + (this.reverseDir ? -90 : 90));
            blackboard.mover.directMoveTo(this, (int)(mob.x + dir.x * travelDistance), (int)(mob.y + dir.y * travelDistance));
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            this.timerBuffer += 50;
            float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
            int msPerWave = 250 + (int)(healthPerc * 230.0F);
            if (this.timerBuffer > msPerWave) {
               mob.magicSoundAbility.runAndSend();
            }

            while (this.timerBuffer > msPerWave) {
               this.timerBuffer -= msPerWave;
               int speed = 80 + (int)(Math.abs(healthPerc - 1.0F) * 100.0F);
               float var10005 = mob.x + this.attackDir.x * 100.0F;
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(new CryoWaveProjectile(mob.x, mob.y, var10005, mob.y + this.attackDir.y * 100.0F, speed, 1280, mob.cryoWaveDamage, 100, mob));
               if (mob.getHealth() < mob.getMaxHealth() / 2) {
                  var10005 = mob.x - this.attackDir.x * 100.0F;
                  mob.getLevel()
                     .entityManager
                     .projectiles
                     .add(new CryoWaveProjectile(mob.x, mob.y, var10005, mob.y - this.attackDir.y * 100.0F, speed, 1280, mob.cryoWaveDamage, 100, mob));
               }

               if (!blackboard.mover.isMoving()) {
                  return AINodeResult.SUCCESS;
               }
            }

            return AINodeResult.RUNNING;
         } else {
            return AINodeResult.SUCCESS;
         }
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.attackingAnimationAbility.runAndSend(false);
      }
   }

   public static class IdleTime<T extends Mob> extends RunningAINode<T> {
      public int msToIdle;
      private int timer;

      public IdleTime(int msToIdle) {
         this.msToIdle = msToIdle;
      }

      @Override
      public void start(T mob, Blackboard<T> blackboard) {
         this.timer = 0;
      }

      @Override
      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         this.timer += 50;
         return this.timer <= this.msToIdle ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      @Override
      public void end(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class MoveToRandomPosition<T extends CryoQueenMob> extends RunningAINode<T> {
      public int baseDistance;
      public boolean isRunningWhileMoving;

      public MoveToRandomPosition(boolean isRunningWhileMoving, int baseDistance) {
         this.isRunningWhileMoving = isRunningWhileMoving;
         this.baseDistance = baseDistance;
      }

      public void start(T mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         java.awt.geom.Point2D.Float base = new java.awt.geom.Point2D.Float(mob.x, mob.y);
         if (currentTarget != null) {
            base = new java.awt.geom.Point2D.Float(currentTarget.x, currentTarget.y);
         }

         java.awt.geom.Point2D.Float pos = new java.awt.geom.Point2D.Float(mob.x, mob.y);

         for (int i = 0; i < 10; i++) {
            int randomAngle = GameRandom.globalRandom.nextInt(360);
            java.awt.geom.Point2D.Float angleDir = GameMath.getAngleDir(randomAngle);
            pos = new java.awt.geom.Point2D.Float(base.x + angleDir.x * this.baseDistance, base.y + angleDir.y * this.baseDistance);
            if (mob.getDistance(pos.x, pos.y) >= this.baseDistance / 4.0F) {
               break;
            }
         }

         blackboard.mover.directMoveTo(this, (int)pos.x, (int)pos.y);
         mob.moveSoundAbility.runAndSend();
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         return this.isRunningWhileMoving && blackboard.mover.isMoving() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      public void end(T mob, Blackboard<T> blackboard) {
      }
   }
}
