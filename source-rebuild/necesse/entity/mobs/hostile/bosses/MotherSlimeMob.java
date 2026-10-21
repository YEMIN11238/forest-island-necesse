package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.CameraShake;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SlimeQuakeEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SlimeQuakeWarningEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.IsolateRunningAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RemoveOnNoTargetNode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageInterface;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.IdleTimeAttackStage;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.SlimeEggProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MotherSlimeMob extends FlyingBossMob {
   public static RotationLootItem vinylRotation = RotationLootItem.globalLootRotation(
      new LootItemList(),
      new LootItemList(),
      new LootItemList(),
      RotationLootItem.globalLootRotation(4, new LootItem("slimesurgevinyl"), new LootItem("motherslimestremblevinyl"))
   );
   public static LootTable lootTable = new LootTable(new LootItemMultiplierIgnored(vinylRotation));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation();
   public static LootTable privateLootTable = new LootTable(new LootItemMultiplierIgnored(uniqueDrops));
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(35000, 46000, 52000, 58000, 69000);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.COSMETIC, Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC
   );
   public TicksPerSecond particleTicks = TicksPerSecond.ticksPerSecond(20);
   protected float height;
   protected long wobbleTimerOffset;
   protected long jumpStartTime;
   protected int jumpAnimationTime;
   protected float jumpStartX;
   protected float jumpStartY;
   protected float jumpTargetX;
   protected float jumpTargetY;
   public final MotherSlimeMob.SlimeBossJumpMobAbility jumpAbility;
   protected long squishStartTime;
   protected int squishAnimationTime;
   public CameraShake squishShake;
   public final IntMobAbility squishLaunchAbility;
   public static GameDamage collisionDamage = new GameDamage(150.0F);
   public static GameDamage quakeDamage = new GameDamage(130.0F);
   public final EmptyMobAbility flickSoundAbility;
   public final EmptyMobAbility popSoundAbility;

   public MotherSlimeMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.moveAccuracy = 60;
      this.setSpeed(150.0F);
      this.setArmor(40);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-100, -100, 200, 100);
      this.hitBox = new Rectangle(-100, -100, 200, 100);
      this.selectBox = new Rectangle(-110, -140, 220, 150);
      this.jumpAbility = this.registerAbility(new MotherSlimeMob.SlimeBossJumpMobAbility());
      this.squishLaunchAbility = this.registerAbility(
         new IntMobAbility() {
            @Override
            protected void run(int value) {
               MotherSlimeMob.this.squishStartTime = MotherSlimeMob.this.getLocalTime();
               MotherSlimeMob.this.squishAnimationTime = value;
               MotherSlimeMob.this.squishShake = new CameraShake(
                  MotherSlimeMob.this.getLocalTime(), MotherSlimeMob.this.squishAnimationTime, 50, 2.0F, 2.0F, true
               );
            }
         }
      );
      this.flickSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (MotherSlimeMob.this.isClient()) {
               SoundManager.playSound(GameResources.slimeSplash4, SoundEffect.effect(MotherSlimeMob.this).pitch(1.0F));
               SoundManager.playSound(GameResources.flick, SoundEffect.effect(MotherSlimeMob.this).pitch(0.8F));
            }
         }
      });
      this.popSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (MotherSlimeMob.this.isClient()) {
               SoundManager.playSound(GameResources.pop, SoundEffect.effect(MotherSlimeMob.this).volume(0.3F).pitch(0.5F).falloffDistance(1400));
            }
         }
      });
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      if (this.jumpStartTime != 0L) {
         writer.putNextBoolean(true);
         int timeSinceStart = (int)(this.getLocalTime() - this.jumpStartTime);
         writer.putNextInt(timeSinceStart);
         writer.putNextFloat(this.jumpStartX);
         writer.putNextFloat(this.jumpStartY);
         writer.putNextFloat(this.jumpTargetX);
         writer.putNextFloat(this.jumpTargetY);
         writer.putNextInt(this.jumpAnimationTime);
      } else {
         writer.putNextBoolean(false);
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      if (reader.getNextBoolean()) {
         int timeSinceStart = reader.getNextInt();
         this.jumpStartTime = this.getLocalTime() - timeSinceStart;
         this.jumpStartX = reader.getNextFloat();
         this.jumpStartY = reader.getNextFloat();
         this.jumpTargetX = reader.getNextFloat();
         this.jumpTargetY = reader.getNextFloat();
         this.jumpAnimationTime = reader.getNextInt();
      }
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
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new MotherSlimeMob.SlimeBossAI<>());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.motherslimebegin, SoundEffect.effect(this).volume(1.5F).falloffDistance(5000));
      }
   }

   @Override
   public void tickMovement(float delta) {
      if (this.jumpStartTime != 0L) {
         long timeSinceJumpStart = this.getLocalTime() - this.jumpStartTime;
         float jumpPercentProgress = GameMath.limit((float)timeSinceJumpStart / this.jumpAnimationTime, 0.0F, 1.0F);
         if (timeSinceJumpStart <= this.jumpAnimationTime) {
            float heightProgress = (float)Math.pow(jumpPercentProgress, 1.5);
            float moveProgress = (float)Math.pow(Math.min(jumpPercentProgress * 1.2F, 1.0F), 0.8F);
            this.x = GameMath.lerp(moveProgress, this.jumpStartX, this.jumpTargetX);
            this.y = GameMath.lerp(moveProgress, this.jumpStartY, this.jumpTargetY);
            this.height = (float)Math.sin(Math.PI * heightProgress) * 140.0F;
         } else {
            this.spawnLandParticles();
            this.height = 0.0F;
            this.jumpStartTime = 0L;
            this.wobbleTimerOffset = this.getLocalTime() - 300L;
         }
      } else if (this.squishStartTime != 0L) {
         long timeSinceSquishStart = this.getLocalTime() - this.squishStartTime;
         if (timeSinceSquishStart > this.squishAnimationTime) {
            this.squishStartTime = 0L;
            this.wobbleTimerOffset = this.getLocalTime();
         }

         this.height = 0.0F;
      } else {
         this.height = 0.0F;
      }

      super.tickMovement(delta);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.MotherSlimesTremble, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      this.particleTicks.gameTick();

      while (this.particleTicks.shouldTick()) {
         int x = this.getX() + GameRandom.globalRandom.getIntBetween(-90, 90);
         int y = this.getY() + 10;
         float dx = GameRandom.globalRandom.getFloatBetween(-50.0F, 50.0F);
         float startHeight = this.height + GameRandom.globalRandom.getIntBetween(30, 90);
         float endHeight = startHeight + 300.0F;
         int lifeTime = GameRandom.globalRandom.getIntBetween(4000, 6000);
         this.getLevel()
            .entityManager
            .addParticle(x, y, this.particleTypeSwitcher.next())
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .movesFriction(dx, 0.0F, 0.5F)
            .sizeFades(14, 20)
            .rotates()
            .heightMoves(startHeight, endHeight)
            .colorRandom(36.0F, 0.7F, 0.6F, 10.0F, 0.1F, 0.1F)
            .fadesAlphaTime(500, 1000)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
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
      SoundManager.playSound(GameResources.motherslimehurt, SoundEffect.effect(this).pitch(pitch).volume(0.3F).falloffDistance(1500));
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.motherslimedeath, SoundEffect.effect(this).falloffDistance(3000));
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return this.height > 50.0F ? false : super.canBeHit(attacker);
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return collisionDamage;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 150;
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return this.height < 20.0F && super.canCollisionHit(target);
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

   public void spawnLandParticles() {
      if (!this.isServer()) {
         int particles = 200;
         float anglePerParticle = 360.0F / particles;

         for (int i = 0; i < particles; i++) {
            int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
            int startRange = GameRandom.globalRandom.getIntBetween(50, 80);
            float startX = this.x + (float)Math.sin(Math.toRadians(angle)) * startRange;
            float startY = this.y + (float)Math.cos(Math.toRadians(angle)) * startRange * 0.6F;
            float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(50, 100);
            float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(50, 100) * 0.6F;
            this.getLevel()
               .entityManager
               .addParticle(startX, startY - 40.0F, i % 4 == 0 ? Particle.GType.IMPORTANT_COSMETIC : Particle.GType.COSMETIC)
               .movesFriction(dx, dy, 0.8F)
               .colorRandom(36.0F, 0.7F, 0.6F, 10.0F, 0.1F, 0.1F)
               .heightMoves(0.0F, 50.0F)
               .lifeTime(1500);
         }

         SoundManager.playSound(GameResources.magicbolt2, SoundEffect.effect(this).volume(0.7F).pitch(0.8F));
         SoundManager.playSound(GameResources.slimeSplash4, SoundEffect.effect(this).pitch(0.8F));
         this.getLevel().getClient().startCameraShake(this, 400, 40, 8.0F, 8.0F, true);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 10; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.motherSlime.body, i % 5, 4, 64, this.x, this.y - 40.0F, 10.0F, knockbackX, knockbackY),
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
      float widthPercent = GameUtils.getAnimFloatContinuous(this.getWorldEntity().getLocalTime() - this.wobbleTimerOffset, 600);
      float heightPercent = GameUtils.getAnimFloatContinuous(this.getWorldEntity().getLocalTime() - this.wobbleTimerOffset + 200L, 600);
      if (this.jumpStartTime != 0L) {
         long timeSinceJumpStart = this.getLocalTime() - this.jumpStartTime;
         float jumpPercentProgress = GameMath.limit((float)timeSinceJumpStart / this.jumpAnimationTime, 0.0F, 1.0F);
         if (jumpPercentProgress < 0.4F) {
            float next = jumpPercentProgress / 0.4F;
            widthPercent = 1.0F - next;
            heightPercent = 1.0F;
         } else if (jumpPercentProgress < 0.8F) {
            float next = GameMath.limit((jumpPercentProgress - 0.4F) / 0.4F, 0.0F, 1.0F);
            widthPercent = next;
            heightPercent = 1.0F - next;
         } else {
            float next = GameMath.limit((jumpPercentProgress - 0.8F) / 0.2F, 0.0F, 1.0F);
            widthPercent = 1.0F;
            heightPercent = next;
         }
      } else if (this.squishStartTime != 0L) {
         long timeSinceSquishStart = this.getLocalTime() - this.squishStartTime;
         int expandTime = Math.min(150, this.squishAnimationTime / 2);
         if (timeSinceSquishStart < this.squishAnimationTime - expandTime) {
            if (!this.squishShake.isOver(this.getLocalTime())) {
               Float shake = this.squishShake.getCurrentShake(this.getLocalTime());
               x = (int)(x + shake.x);
               y = (int)(y + shake.y);
            }

            float compressProgress = GameMath.limit((float)timeSinceSquishStart / (this.squishAnimationTime - expandTime), 0.0F, 1.0F);
            heightPercent = GameMath.lerp(compressProgress, 1.0F, -1.0F);
            widthPercent = 1.0F;
         } else {
            float expandProgress = GameMath.limit((float)(timeSinceSquishStart - (this.squishAnimationTime - expandTime)) / expandTime, 0.0F, 1.0F);
            heightPercent = GameMath.lerp(expandProgress, -1.0F, 1.0F);
            widthPercent = GameMath.lerp(expandProgress, 1.0F, 0.0F);
         }
      }

      float widthFloat = GameMath.lerp(widthPercent, 0.9F, 1.0F);
      int width = (int)(256.0F * widthFloat);
      float heightFloat = GameMath.lerp(heightPercent, 0.9F, 1.0F);
      int height = (int)(256.0F * heightFloat);
      int drawX = camera.getDrawX(x) - width / 2;
      int drawY = camera.getDrawY(y) - 215 + (256 - height);
      final DrawOptions body = MobRegistry.Textures.motherSlime
         .body
         .initDraw()
         .sprite(0, 0, 256)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .size(width, height)
         .pos(drawX, drawY - (int)this.height);
      float shadowSize = 1.0F - GameMath.limit(this.height / 700.0F, 0.0F, 0.8F);
      int shadowWidth = (int)(width * shadowSize);
      int shadowHeight = (int)(height * shadowSize);
      int shadowDrawX = camera.getDrawX(x) - shadowWidth / 2;
      int shadowDrawY = camera.getDrawY(y) - 215 + (256 - shadowHeight);
      TextureDrawOptions shadowOptions = MobRegistry.Textures.motherSlime
         .shadow
         .initDraw()
         .sprite(0, 0, 256)
         .light(light)
         .size(shadowWidth, shadowHeight)
         .pos(shadowDrawX, shadowDrawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            body.draw();
         }
      });
      tileList.add(tm -> shadowOptions.draw());
   }

   @Override
   public int getFlyingHeight() {
      return (int)this.height;
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
      MobRegistry.Textures.motherSlime.body.initDraw().sprite(0, 0, 256).size(48, 48).draw(drawX, drawY - (int)(this.height / 10.0F));
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
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.2F));
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

   public static class SlimeBossAI<T extends MotherSlimeMob> extends SequenceAINode<T> {
      public SlimeBossAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         this.addChild(new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         this.addChild(new IsolateRunningAINode<>(attackStages));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.WaitForJumpDoneStage());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1000, 2000)));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.JumpSlamStage());
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.WaitForJumpDoneStage());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500, 1000)));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.JumpQuakeStage(300, 0));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.WaitForJumpDoneStage());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500, 1000)));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.JumpQuakeStage(300, 200));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.WaitForJumpDoneStage());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500, 1000)));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.JumpQuakeStage(300, 300));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.WaitForJumpDoneStage());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500, 1000)));
         attackStages.addChild(new MotherSlimeMob.SlimeBossAI.SquishLaunchStage());
      }

      public int getIdleTime(T mob, int minTime, int maxTime) {
         return GameMath.lerp(mob.getHealthPercent(), minTime, maxTime);
      }

      public Mob getCurrentTarget() {
         return this.getBlackboard().getObject(Mob.class, "currentTarget");
      }

      public class JumpQuakeStage extends AINode<T> implements AttackStageInterface<T> {
         public int desiredDistanceFromTarget;
         public int jumpDistance;
         public int targetX;
         public int targetY;
         public int jumpAnimationTime;
         public long warningTime;
         public long quakeTime;
         public int offset;
         public float velocity;

         public JumpQuakeStage(int desiredDistanceFromTarget, int jumpDistance) {
            this.desiredDistanceFromTarget = desiredDistanceFromTarget;
            this.jumpDistance = jumpDistance;
         }

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            if (this.jumpDistance != 0) {
               Mob currentTarget = SlimeBossAI.this.getCurrentTarget();
               if (currentTarget != null) {
                  float distToTarget = mob.getDistance(currentTarget);
                  Float dirToTarget = GameMath.normalize(currentTarget.x - mob.x, currentTarget.y - mob.y);
                  float distancePercentNeeded = GameMath.limit((this.desiredDistanceFromTarget - distToTarget) / this.jumpDistance, -1.0F, 1.0F);
                  float angleOffset;
                  if (distancePercentNeeded < 0.0F) {
                     angleOffset = GameMath.lerp(Math.abs(distancePercentNeeded), 90, 0);
                  } else {
                     angleOffset = GameMath.lerp(Math.abs(distancePercentNeeded), 90, 180);
                  }

                  if (GameRandom.globalRandom.nextBoolean()) {
                     angleOffset = -angleOffset;
                  }

                  float angle = GameMath.fixAngle(GameMath.getAngle(dirToTarget) + angleOffset);
                  Float dir = GameMath.getAngleDir(angle);
                  this.targetX = (int)(mob.x + dir.x * this.jumpDistance);
                  this.targetY = (int)(mob.y + dir.y * this.jumpDistance);
               } else {
                  int angle = GameRandom.globalRandom.nextInt(360);
                  Float dir = GameMath.getAngleDir(angle);
                  this.targetX = (int)(mob.x + dir.x * this.jumpDistance);
                  this.targetY = (int)(mob.y + dir.y * this.jumpDistance);
               }
            } else {
               this.targetX = mob.getX();
               this.targetY = mob.getY();
            }

            float exp = GameMath.expSmooth(mob.getHealthPercent(), 1.0F, 0.3F);
            this.jumpAnimationTime = GameMath.lerp(exp, 1000, 1200);
            this.warningTime = mob.getTime() + this.jumpAnimationTime - 400L;
            this.quakeTime = this.warningTime + GameMath.lerp(exp, 1000, 1300);
            this.offset = GameRandom.globalRandom.nextInt(15000);
            this.velocity = GameMath.lerp(exp, 700, 400);
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (this.jumpAnimationTime != 0) {
               mob.jumpAbility.runAndSend(this.targetX, this.targetY, this.jumpAnimationTime);
               this.jumpAnimationTime = 0;
            }

            if (this.warningTime != 0L && this.warningTime <= mob.getTime()) {
               int timeToQuake = (int)(this.quakeTime - mob.getTime());
               mob.getLevel()
                  .entityManager
                  .events
                  .add(
                     new SlimeQuakeWarningEvent(mob, this.targetX, this.targetY - 40, new GameRandom(), 0.0F, this.velocity, 1200.0F, timeToQuake, this.offset)
                  );
               this.warningTime = 0L;
               return AINodeResult.RUNNING;
            } else if (this.quakeTime != 0L && this.quakeTime <= mob.getTime()) {
               mob.getLevel()
                  .entityManager
                  .events
                  .add(
                     new SlimeQuakeEvent(
                        mob, this.targetX, this.targetY - 40, new GameRandom(), 0.0F, MotherSlimeMob.quakeDamage, this.velocity, 50.0F, 1200.0F, this.offset
                     )
                  );
               this.quakeTime = 0L;
               return AINodeResult.SUCCESS;
            } else {
               return AINodeResult.RUNNING;
            }
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      public class JumpSlamStage extends AINode<T> implements AttackStageInterface<T> {
         public long nextJumpTime;
         public long endTime;

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            this.endTime = mob.getTime() + GameMath.lerp(mob.getHealthPercent(), 5000, 8000);
            this.nextJumpTime = 0L;
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (this.endTime <= mob.getTime()) {
               return AINodeResult.SUCCESS;
            }

            if (this.nextJumpTime <= mob.getTime() && mob.jumpStartTime == 0L) {
               Mob target = SlimeBossAI.this.getCurrentTarget();
               if (target != null) {
                  float distance = GameMath.limit(target.getDistance(mob), 200.0F, 500.0F);
                  Float dir = GameMath.normalize(target.x - mob.x, target.y - mob.y + 40.0F);
                  int randomXOffset = GameRandom.globalRandom.getIntBetween(-40, 40);
                  int randomYOffset = GameRandom.globalRandom.getIntBetween(-40, 40);
                  float exp = GameMath.expSmooth(mob.getHealthPercent(), 1.0F, 0.3F);
                  int animationTime = GameMath.lerp(exp, 400, 900);
                  mob.jumpAbility.runAndSend(mob.x + dir.x * distance + randomXOffset, mob.y + dir.y * distance + randomYOffset, animationTime);
                  int cooldownTime = GameMath.lerp(exp, 150, 800);
                  this.nextJumpTime = mob.getTime() + animationTime + cooldownTime;
               } else {
                  this.nextJumpTime = mob.getTime() + 500L;
               }
            }

            return AINodeResult.RUNNING;
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
         }
      }

      public class SquishLaunchStage extends AINode<T> implements AttackStageInterface<T> {
         public boolean hasSentSoundEffect;

         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public void onStarted(T mob, Blackboard<T> blackboard) {
            float exp = GameMath.expSmooth(mob.getHealthPercent(), 1.0F, 0.3F);
            int animationTime = GameMath.lerp(exp, 1000, 3000);
            mob.squishLaunchAbility.runAndSend(animationTime);
            this.hasSentSoundEffect = false;
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            if (!this.hasSentSoundEffect) {
               long timeSinceSquishStart = mob.getLocalTime() - mob.squishStartTime;
               int expandTime = Math.min(150, mob.squishAnimationTime / 2);
               if (timeSinceSquishStart >= mob.squishAnimationTime - expandTime) {
                  mob.flickSoundAbility.runAndSend();
                  this.hasSentSoundEffect = true;
               }
            }

            return mob.squishStartTime == 0L ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
         }

         public void onEnded(T mob, Blackboard<T> blackboard) {
            int spawns = GameMath.lerp(mob.getHealthPercent(), 5, 15);

            for (int i = 0; i < spawns; i++) {
               float mod = GameRandom.globalRandom.getFloatBetween(0.7F, 1.0F);
               int dist = (int)(300.0F * mod);
               float angle = GameRandom.globalRandom.nextInt(360);
               Float dir = GameMath.getAngleDir(angle);
               Float targetPos = new Float(mob.x + dir.x * dist, mob.y + dir.y * dist);
               float x = mob.x + dir.x * 70.0F;
               float y = mob.y - 40.0F + dir.y * 30.0F;
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(new SlimeEggProjectile(mob.getLevel(), mob, x, y, targetPos.x, targetPos.y, 30.0F, dist, new GameDamage(0.0F), 50));
            }
         }
      }

      public class WaitForJumpDoneStage extends AINode<T> {
         protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         }

         public void init(T mob, Blackboard<T> blackboard) {
         }

         public AINodeResult tick(T mob, Blackboard<T> blackboard) {
            return mob.jumpStartTime == 0L ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
         }
      }
   }

   public class SlimeBossJumpMobAbility extends MobAbility {
      public void runAndSend(float targetX, float targetY, int animationTime) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextFloat(targetX);
         writer.putNextFloat(targetY);
         writer.putNextInt(animationTime);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         MotherSlimeMob.this.jumpStartTime = MotherSlimeMob.this.getLocalTime();
         MotherSlimeMob.this.jumpStartX = MotherSlimeMob.this.x;
         MotherSlimeMob.this.jumpStartY = MotherSlimeMob.this.y;
         MotherSlimeMob.this.jumpTargetX = reader.getNextFloat();
         MotherSlimeMob.this.jumpTargetY = reader.getNextFloat();
         MotherSlimeMob.this.jumpAnimationTime = reader.getNextInt();
         if (!MotherSlimeMob.this.isServer()) {
            SoundManager.playSound(GameResources.slimeSplash2, SoundEffect.effect(MotherSlimeMob.this).pitch(0.6F));
         }
      }
   }
}
