package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.journal.JournalChallenge;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.Entity;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.SpiritCorruptedLevelEvent;
import necesse.entity.levelEvent.TicTacToeLevelEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CursedCroneSpiritSkullsEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.LinedUpSpiritBeamsLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.IsolateRunningAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RunningAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.mobs.mobMovement.MobMovementDiagonalLineFixed;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;
import necesse.entity.mobs.mobMovement.MobMovementSpiral;
import necesse.entity.mobs.mobMovement.MobMovementSpiralLevelPos;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.followingProjectile.TicTacToePunishProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.MobConditionLootItemList;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.levelData.CursedCroneArenasLevelData;
import necesse.level.maps.light.GameLight;

public class TheCursedCroneMob extends FlyingBossMob {
   public static LootTable lootTable = new LootTable(
      new LootItemMultiplierIgnored(new LootItemList(new ChanceLootItem(0.2F, "thecursedtriumphvinyl"), new ChanceLootItem(0.05F, "tictactoeboard")))
   );
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      new LootItem("thesoulstorm"), new LootItem("spiritgreaves"), new LootItem("summonersbestiary"), new LootItem("barkblade")
   );
   public static LootTable privateLootTable = new LootTable(
      new MobConditionLootItemList(mob -> mob.getLevel() == null || !mob.getLevel().isIncursionLevel, uniqueDrops)
   );
   public static MaxHealthGetter BASE_MAX_HEALTH = new MaxHealthGetter(14000, 18000, 25000, 29000, 35000);
   public static GameDamage collisionDamage = new GameDamage(80.0F);
   public static GameDamage ghoulsMeleeDamage = new GameDamage(70.0F);
   public static GameDamage ghoulsGooDamage = new GameDamage(60.0F);
   public static GameDamage spiritBeamsDamage = new GameDamage(85.0F);
   public static GameDamage spiritSkullsDamage = new GameDamage(75.0F);
   public static GameDamage tornadoCollisionDamage = new GameDamage(70.0F);
   public static float punishProjectileDamagePercent = 0.66F;
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   public LinkedList<Mob> spawnedMobs = new LinkedList<>();
   public LinkedList<LevelEvent> spawnedEvents = new LinkedList<>();
   protected TheCursedCroneMob.AnimationState animationState;
   public long animationStateStartTime;
   public int animationDuration;
   public boolean hasAnimationEnded;
   public final TheCursedCroneMob.StartAnimationStateAbility startAnimationStateAbility;
   public final BooleanMobAbility changeHostileAbility;
   public final IntMobAbility setMoveAccuracyAbility;
   protected boolean hasSpeedBurst = false;
   public final EmptyMobAbility laughSoundAbility;
   protected Point loadedArenaTile;
   protected CursedCroneArenasLevelData.ArenaData arena;
   protected SpiritCorruptedLevelEvent spiritCorruptedEvent;
   public ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC);
   public float particleBuffer;

   public TheCursedCroneMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(BASE_MAX_HEALTH);
      this.moveAccuracy = 40;
      this.setSpeed(110.0F);
      this.setArmor(24);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-15, -15, 30, 30);
      this.hitBox = new Rectangle(-20, -20, 40, 40);
      this.selectBox = new Rectangle(-20, -20, 40, 40);
      this.animationState = TheCursedCroneMob.AnimationState.ON_GROUND_IDLE;
      this.isHostile = false;
      this.animationDuration = 500;
      this.startAnimationStateAbility = this.registerAbility(new TheCursedCroneMob.StartAnimationStateAbility());
      this.laughSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (TheCursedCroneMob.this.isClient()) {
               SoundManager.playSound(GameResources.croneLaugh, SoundEffect.globalEffect().volume(1.1F).pitch(1.3F));
            }
         }
      });
      this.changeHostileAbility = this.registerAbility(
         new BooleanMobAbility() {
            @Override
            protected void run(boolean value) {
               if (!TheCursedCroneMob.this.isHostile && value) {
                  if (TheCursedCroneMob.this.isServer()) {
                     TheCursedCroneMob.this.getServer()
                        .network
                        .sendToClientsWithEntity(
                           new PacketChatMessage(new LocalMessage("misc", "bossawoke", "name", TheCursedCroneMob.this.getLocalization())),
                           TheCursedCroneMob.this
                        );
                  } else {
                     SoundManager.playSound(GameResources.cursedcronebegin, SoundEffect.effect(TheCursedCroneMob.this).volume(1.4F).falloffDistance(3000));
                  }

                  TheCursedCroneMob.this.startAnimation(TheCursedCroneMob.AnimationState.START_FLYING, 500, true);
               } else if (TheCursedCroneMob.this.isHostile && !value) {
                  TheCursedCroneMob.this.stopMoving();
                  TheCursedCroneMob.this.resetAI();
                  TheCursedCroneMob.this.resetPositionToArena();
                  TheCursedCroneMob.this.hasSpeedBurst = false;
                  TheCursedCroneMob.this.startAnimation(TheCursedCroneMob.AnimationState.ON_GROUND_IDLE, 500, true);
               }

               TheCursedCroneMob.this.isHostile = value;
            }
         }
      );
      this.setMoveAccuracyAbility = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            TheCursedCroneMob.this.moveAccuracy = value;
         }
      });
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextEnum(this.animationState);
      writer.putNextLong(this.animationStateStartTime);
      writer.putNextInt(this.animationDuration);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.animationState = reader.getNextEnum(TheCursedCroneMob.AnimationState.class);
      this.animationStateStartTime = reader.getNextLong();
      this.animationDuration = reader.getNextInt();
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isHostile);
      writer.putNextBoolean(this.hasSpeedBurst);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isHostile = reader.getNextBoolean();
      this.hasSpeedBurst = reader.getNextBoolean();
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
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.arena != null) {
         save.addPoint("arenaTile", new Point(this.arena.tileX, this.arena.tileY));
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.loadedArenaTile = save.getPoint("arenaTile", this.loadedArenaTile, false);
   }

   @Override
   public void onLoadingComplete() {
      super.onLoadingComplete();
      if (this.loadedArenaTile != null) {
         this.getLevel().regionManager.ensureTileIsLoaded(this.loadedArenaTile.x, this.loadedArenaTile.y);
         CursedCroneArenasLevelData arenas = CursedCroneArenasLevelData.getCursedCroneArenasData(this.getLevel(), false);
         if (arenas != null) {
            this.arena = arenas.getArena(this.loadedArenaTile.x, this.loadedArenaTile.y);
            this.resetPositionToArena();
         }

         this.loadedArenaTile = null;
      }
   }

   @Override
   public void init() {
      this.setHealth(this.getMaxHealth());
      super.init();
      this.resetAI();
      if (this.animationState != null) {
         if (this.animationState.updateCollisions != null) {
            this.animationState.updateCollisions.accept(this);
         }

         if (this.animationState.onAnimationStart != null) {
            this.animationState.onAnimationStart.accept(this);
         }
      }

      if (this.isClient() && this.isHostile) {
         SoundManager.playSound(GameResources.cursedcronebegin, SoundEffect.effect(this).volume(1.4F).falloffDistance(3000));
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickAnimationState();
      if (this.isHostile) {
         if (this.isClientPlayerNearby()) {
            SoundManager.setMusic(MusicRegistry.TheCursedTriumph, SoundManager.MusicPriority.EVENT, 1.5F);
            EventStatusBarManager.registerMobHealthStatusBar(this);
         }

         BossNearbyBuff.applyAround(this);
      }

      if (this.animationState != TheCursedCroneMob.AnimationState.ON_GROUND_IDLE && this.animationState != TheCursedCroneMob.AnimationState.ON_GROUND_STUNNED) {
         int particlesPerSecond = 10;
         this.particleBuffer += 50 * particlesPerSecond / 1000.0F;

         while (this.particleBuffer >= 1.0F) {
            this.particleBuffer--;
            int minHeight = -20;
            int maxHeight = 20;
            float height = GameMath.lerp(GameRandom.globalRandom.nextFloat(), minHeight, maxHeight);
            AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
            float distance = 20.0F;
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameMath.sin(currentAngle.get()) * distance,
                  this.y + GameMath.cos(currentAngle.get()) * distance * 0.75F,
                  this.particleTypeSwitcher.next()
               )
               .colorRandom(158.0F, 0.7F, 0.5F, 10.0F, 0.1F, 0.1F)
               .heightMoves(height, height + 20.0F)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
                  float distanceProgress = GameMath.lerp((float)Math.pow(lifePercent, 2.0), distance, distance + 60.0F);
                  float distY = distanceProgress * 0.75F;
                  pos.x = this.x + GameMath.sin(angle) * distanceProgress;
                  pos.y = this.y + GameMath.cos(angle) * distY * 0.75F;
               })
               .lifeTime(3000)
               .sizeFades(12, 16);
         }
      }

      this.setSpeed(GameMath.lerp(this.getHealthPercent(), 210, 140) * (this.hasSpeedBurst ? 2.0F : 1.0F));
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      if (this.isHostile) {
         BossNearbyBuff.applyAround(this);
         if (this.spiritCorruptedEvent != null && !this.spiritCorruptedEvent.isOver()) {
            if (this.spiritCorruptedEvent.getRemainingTime() <= 3000L) {
               this.spiritCorruptedEvent.setEndTimeAction.runAndSend(this.getTime() + 5000L);
            }
         } else {
            this.spiritCorruptedEvent = new SpiritCorruptedLevelEvent(this.getTime() + 5000L);
            this.getLevel().entityManager.events.add(this.spiritCorruptedEvent);
         }
      } else if (this.spiritCorruptedEvent != null) {
         this.spiritCorruptedEvent.over();
         this.spiritCorruptedEvent = null;
      }

      this.tickAnimationState();
      this.setSpeed(GameMath.lerp(this.getHealthPercent(), 210, 140) * (this.hasSpeedBurst ? 2.0F : 1.0F));
   }

   @Override
   public boolean shouldSave() {
      return this.arena != null;
   }

   public void startAnimation(TheCursedCroneMob.AnimationState animationState, int animationDuration, boolean runStartLogic) {
      TheCursedCroneMob.AnimationState lastAnimationState = this.animationState;
      if (lastAnimationState != null && lastAnimationState.looping && lastAnimationState.onAnimationEnd != null) {
         lastAnimationState.onAnimationEnd.accept(this);
      }

      this.animationState = animationState;
      this.animationStateStartTime = this.getTime();
      this.animationDuration = animationDuration;
      if (animationState.updateCollisions != null) {
         animationState.updateCollisions.accept(this);
      } else {
         this.collision = new Rectangle(-20, -20, 40, 40);
         this.hitBox = new Rectangle(-30, -40, 60, 70);
         this.selectBox = new Rectangle(-30, -75, 60, 90);
      }

      if (animationState.onAnimationStart != null && runStartLogic) {
         animationState.onAnimationStart.accept(this);
      }

      this.hasAnimationEnded = false;
   }

   public void tickAnimationState() {
      if (!this.animationState.looping && !this.hasAnimationEnded && this.getRemainingAnimationTime() <= 0) {
         this.hasAnimationEnded = true;
         if (this.animationState.onAnimationEnd != null) {
            this.animationState.onAnimationEnd.accept(this);
         }
      }
   }

   public int getRemainingAnimationTime() {
      return this.animationState.looping ? this.animationDuration : (int)(this.animationDuration - this.getTime() + this.animationStateStartTime);
   }

   public void setArenaData(CursedCroneArenasLevelData.ArenaData arena) {
      this.arena = arena;
   }

   public void resetPositionToArena() {
      if (this.arena != null) {
         this.getLevel().regionManager.ensureTileIsLoaded(this.arena.tileX, this.arena.tileY);
         this.setPos(this.arena.tileX * 32 + 16, (this.arena.tileY - 4) * 32, true);
         this.dx = 0.0F;
         this.dy = 0.0F;
      }
   }

   protected void resetAI() {
      this.ai = new BehaviourTreeAI<>(this, new TheCursedCroneMob.TheCursedCroneAI<>(), new FlyingAIMover());
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
      return collisionDamage;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 150;
   }

   @Override
   public void setMaxHealth(int maxHealth) {
      super.setMaxHealth(maxHealth);
      if (this.scaling != null) {
         this.scaling.updatedMaxHealth();
      }
   }

   @Override
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 70; i++) {
         int lifeTime = GameRandom.globalRandom.getIntBetween(500, 5000);
         float lifePerc = lifeTime / 5000.0F;
         float startHeight = 26.0F;
         float height = startHeight + GameRandom.globalRandom.getIntBetween(70, 150) * lifePerc;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
               this.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 4), 0, 12))
            .sizeFades(20, 30)
            .movesFriction(GameRandom.globalRandom.getFloatBetween(-40.0F, 40.0F), GameRandom.globalRandom.getFloatBetween(-40.0F, 40.0F), 0.5F)
            .heightMoves(startHeight, height)
            .colorRandom(158.0F, 0.8F, 0.6F, 10.0F, 0.1F, 0.1F)
            .givesLight(158.0F, 0.8F)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void playHurtSound() {
      if (this.isHostile) {
         float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
         SoundManager.playSound(GameResources.cursedcronehurt, SoundEffect.effect(this).pitch(pitch).volume(0.4F));
      }
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.cursedcronedeath, SoundEffect.effect(this).falloffDistance(3000));
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
      boolean hasGlowEffect = perspective != null && perspective.buffManager.getModifier(BuffModifiers.ENEMY_TRACKER);
      float rotate = Math.min(30.0F, this.dx / 8.0F);
      long timeSinceAnimationStart = this.getTime() - this.animationStateStartTime;
      int xFrame;
      if (this.animationState.looping) {
         xFrame = GameUtils.getAnim(this.getTime(), this.animationState.frames, this.animationDuration);
      } else {
         float animationProgress = GameMath.limit((float)timeSinceAnimationStart / this.animationDuration, 0.0F, 1.0F);
         xFrame = GameMath.min((int)(animationProgress * this.animationState.frames), this.animationState.frames - 1);
      }

      int drawX = camera.getDrawX(x) - 64;
      int drawY = camera.getDrawY(y) - 96 - 32 - 16;
      if (this.animationState == TheCursedCroneMob.AnimationState.ON_GROUND_IDLE) {
         xFrame = 0;
         if (!this.isHostile && level.buffManager.getModifier(LevelModifiers.SPIRIT_CORRUPTED)) {
            xFrame = 1;
         }
      }

      final DrawOptions shadow = MobRegistry.Textures.theCursedCrone_shadow
         .initDraw()
         .sprite(xFrame, this.animationState.spriteY, 128, 192)
         .light(light)
         .pos(drawX, drawY);
      final DrawOptions backEffects = MobRegistry.Textures.theCursedCroneBackEffects
         .initDraw()
         .sprite(xFrame, this.animationState.spriteY, 128, 192)
         .startGlowOptions(level, this.getID())
         .light(light)
         .apply(hasGlowEffect)
         .rotate(rotate, 64, 80)
         .pos(drawX, drawY);
      final DrawOptions body = MobRegistry.Textures.theCursedCrone
         .initDraw()
         .sprite(xFrame, this.animationState.spriteY, 128, 192)
         .startGlowOptions(level, this.getID())
         .light(light)
         .apply(hasGlowEffect)
         .rotate(rotate, 64, 80)
         .pos(drawX, drawY);
      final DrawOptions frontEffects = MobRegistry.Textures.theCursedCroneFrontEffects
         .initDraw()
         .sprite(xFrame, this.animationState.spriteY, 128, 192)
         .startGlowOptions(level, this.getID())
         .light(light)
         .apply(hasGlowEffect)
         .rotate(rotate, 64, 80)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            shadow.draw();
            backEffects.draw();
            body.draw();
            frontEffects.draw();
         }
      });
   }

   @Override
   protected int getDrawSortY(Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective, boolean fromMount) {
      return this.animationState != TheCursedCroneMob.AnimationState.ON_GROUND_IDLE
            && this.animationState != TheCursedCroneMob.AnimationState.ON_GROUND_STUNNED
         ? super.getDrawSortY(level, x, y, tickManager, camera, perspective, fromMount)
         : super.getDrawSortY(level, x, y, tickManager, camera, perspective, fromMount) + 14;
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 32;
      int drawY = y - 48;
      if (this.animationState == TheCursedCroneMob.AnimationState.ON_GROUND_IDLE) {
         MobRegistry.Textures.theCursedCrone.initDraw().sprite(0, 7, 128, 192).size(64, 96).draw(drawX, drawY - 16);
      } else {
         MobRegistry.Textures.theCursedCrone.initDraw().sprite(0, 2, 128, 192).size(64, 96).draw(drawX, drawY);
      }
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

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.spiritCorruptedEvent != null) {
         this.spiritCorruptedEvent.over();
         this.spiritCorruptedEvent = null;
      }

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

   public enum AnimationState {
      FACE_UP(0, 4, true, null),
      FACE_RIGHT(1, 4, true, null),
      DEFAULT_FLYING(2, 4, true, null),
      FACE_LEFT(3, 4, true, null),
      SPIRIT_BEAMS(4, 4, false, null, null, theCursedCroneMob -> {
         if (theCursedCroneMob.isServer()) {
            theCursedCroneMob.startAnimationStateAbility.runAndSend(DEFAULT_FLYING);
         } else {
            theCursedCroneMob.startAnimation(DEFAULT_FLYING, 500, false);
         }
      }),
      CHANNEL_SPIRIT_GHOULS(5, 4, true, null, theCursedCroneMob -> theCursedCroneMob.moveAccuracy = 5, theCursedCroneMob -> theCursedCroneMob.moveAccuracy = 40),
      START_FLYING(6, 4, false, null, null, theCursedCroneMob -> {
         if (theCursedCroneMob.isServer()) {
            theCursedCroneMob.startAnimationStateAbility.runAndSend(DEFAULT_FLYING);
         } else {
            theCursedCroneMob.startAnimation(DEFAULT_FLYING, 500, false);
         }
      }),
      ON_GROUND_IDLE(7, 2, true, theCursedCroneMob -> {
         theCursedCroneMob.collision = new Rectangle(-15, -15, 30, 30);
         theCursedCroneMob.hitBox = new Rectangle(-20, -20, 40, 40);
         theCursedCroneMob.selectBox = new Rectangle(-20, -20, 40, 40);
      }, null, null),
      ON_GROUND_STUNNED(
         7,
         1,
         false,
         theCursedCroneMob -> {
            theCursedCroneMob.collision = new Rectangle(-15, -15, 30, 30);
            theCursedCroneMob.hitBox = new Rectangle(-20, -20, 40, 40);
            theCursedCroneMob.selectBox = new Rectangle(-20, -20, 40, 40);
         },
         theCursedCroneMob -> {
            if (theCursedCroneMob.isServer()) {
               theCursedCroneMob.buffManager.removeBuff(BuffRegistry.INVULNERABLE_ACTIVE, true);
               int remainingAnimationTime = theCursedCroneMob.getRemainingAnimationTime();
               if (remainingAnimationTime > 0) {
                  theCursedCroneMob.buffManager
                     .addBuff(new ActiveBuff(BuffRegistry.Debuffs.STUNNED_DAMAGE_TAKEN_INCREASED, theCursedCroneMob, remainingAnimationTime, null), true);
               }
            }
         },
         theCursedCroneMob -> {
            if (theCursedCroneMob.isServer()) {
               theCursedCroneMob.startAnimationStateAbility.runAndSend(START_FLYING);
               theCursedCroneMob.buffManager.removeBuff(BuffRegistry.Debuffs.STUNNED_DAMAGE_TAKEN_INCREASED, true);
            } else {
               theCursedCroneMob.startAnimation(START_FLYING, 500, false);
            }
         }
      ),
      SHIELD_EXPLODE(9, 4, false, null, null, theCursedCroneMob -> {
         if (theCursedCroneMob.isServer()) {
            theCursedCroneMob.startAnimationStateAbility.runAndSend(ON_GROUND_STUNNED, 6000);
         } else {
            theCursedCroneMob.startAnimation(ON_GROUND_STUNNED, 6000, false);
         }
      }),
      CHANNEL_SHIELD(8, 4, true, null, theCursedCroneMob -> theCursedCroneMob.moveAccuracy = 5, theCursedCroneMob -> theCursedCroneMob.moveAccuracy = 40),
      FADE_OUT_SHIELD(10, 4, false, null, null, theCursedCroneMob -> {
         if (theCursedCroneMob.isServer()) {
            theCursedCroneMob.startAnimationStateAbility.runAndSend(DEFAULT_FLYING);
         } else {
            theCursedCroneMob.startAnimation(DEFAULT_FLYING, 500, false);
         }
      });

      public final int spriteY;
      public final int frames;
      public final boolean looping;
      public final Consumer<TheCursedCroneMob> updateCollisions;
      public final Consumer<TheCursedCroneMob> onAnimationStart;
      public final Consumer<TheCursedCroneMob> onAnimationEnd;

      AnimationState(
         int spriteY,
         int frames,
         boolean looping,
         Consumer<TheCursedCroneMob> updateCollisions,
         Consumer<TheCursedCroneMob> onAnimationStart,
         Consumer<TheCursedCroneMob> onAnimationEnd
      ) {
         this.spriteY = spriteY;
         this.frames = frames;
         this.looping = looping;
         this.onAnimationStart = onAnimationStart;
         this.onAnimationEnd = onAnimationEnd;
         this.updateCollisions = updateCollisions;
      }

      AnimationState(int spriteY, int frames, boolean looping, Consumer<TheCursedCroneMob> updateCollisions) {
         this(spriteY, frames, looping, updateCollisions, null, null);
      }
   }

   public static class ChargeOppositeTargetDirection<T extends TheCursedCroneMob> extends RunningAINode<T> {
      protected int range = 200;

      public void start(T mob, Blackboard<T> blackboard) {
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target != null) {
            float distance = GameMath.getExactDistance(target.x, target.y, mob.x, mob.y);
            java.awt.geom.Point2D.Float chargeDir = GameMath.normalize(target.x - mob.x, target.y - mob.y);
            float chargeToX = mob.x + chargeDir.x * (distance + this.range);
            float chargeToY = mob.y + chargeDir.y * (distance + this.range);
            float travelTime = Projectile.getTravelTimeMillis(mob.getSpeed(), distance);
            float nextX = GameMath.limit(Entity.getPositionAfterMillis(target.dx, travelTime), -this.range * 2, this.range * 2);
            float nextY = GameMath.limit(Entity.getPositionAfterMillis(target.dy, travelTime), -this.range * 2, this.range * 2);
            chargeToX += nextX;
            chargeToY += nextY;
            mob.hasSpeedBurst = true;
            blackboard.mover.setCustomMovement(this, new MobMovementLevelPos(chargeToX, chargeToY));
            mob.sendMovementPacket(false);
            mob.laughSoundAbility.runAndSend();
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         return blackboard.mover.isCurrentlyMovingFor(this) && !mob.hasArrivedAtTarget() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.hasSpeedBurst = false;
         mob.sendMovementPacket(false);
      }
   }

   public static class DiagonalChargeRotation<T extends TheCursedCroneMob> extends RunningAINode<T> {
      protected Mob target;
      protected java.awt.geom.Point2D.Float chargeDir;
      protected long chargeAtTime;
      protected int range = (int)(192.0 * GameMath.diagonalDistance);
      protected boolean isCharging;

      public void start(T mob, Blackboard<T> blackboard) {
         this.target = blackboard.getObject(Mob.class, "currentTarget");
         if (this.target != null) {
            Point centerPos;
            if (mob.arena != null) {
               centerPos = new Point(mob.arena.tileX * 32 + 16, mob.arena.tileY * 32 + 16);
            } else {
               centerPos = new Point(this.target.getX(), this.target.getY());
            }

            this.chargeDir = null;
            float bestDistance = -1.0F;

            for (int i = 0; i < 4; i++) {
               int nextAngle = i * 90 + 45;
               java.awt.geom.Point2D.Float angleDir = GameMath.getAngleDir(nextAngle);
               float distance = mob.getDistance(centerPos.x + angleDir.x * -this.range, centerPos.y + angleDir.y * -this.range);
               if (this.chargeDir == null || distance < bestDistance) {
                  this.chargeDir = angleDir;
                  bestDistance = distance;
               }
            }

            float offsetAngle = GameMath.fixAngle(GameMath.getAngle(this.chargeDir) + 180.0F);
            blackboard.mover.setCustomMovement(this, new MobMovementDiagonalLineFixed(this.target, centerPos.x, centerPos.y, this.range, offsetAngle));
            this.chargeAtTime = mob.getTime() + GameMath.lerp(mob.getHealthPercent(), 1000, 3000);
            this.isCharging = false;
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         if (this.target == null) {
            return AINodeResult.SUCCESS;
         }

         if (this.isCharging) {
            return blackboard.mover.isCurrentlyMovingFor(this) && !mob.hasArrivedAtTarget() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
         }

         if (this.chargeAtTime <= mob.getTime()) {
            float distance = GameMath.getExactDistance(this.target.x, this.target.y, mob.x, mob.y);
            java.awt.geom.Point2D.Float chargeDir = GameMath.normalize(this.target.x - mob.x, this.target.y - mob.y);
            float chargeToX = mob.x + chargeDir.x * (distance + this.range);
            float chargeToY = mob.y + chargeDir.y * (distance + this.range);
            float travelTime = Projectile.getTravelTimeMillis(mob.getSpeed(), distance);
            float nextX = GameMath.limit(Entity.getPositionAfterMillis(this.target.dx, travelTime), -this.range * 2, this.range * 2);
            float nextY = GameMath.limit(Entity.getPositionAfterMillis(this.target.dy, travelTime), -this.range * 2, this.range * 2);
            chargeToX += nextX;
            chargeToY += nextY;
            mob.hasSpeedBurst = true;
            blackboard.mover.setCustomMovement(this, new MobMovementLevelPos(chargeToX, chargeToY));
            mob.sendMovementPacket(false);
            mob.laughSoundAbility.runAndSend();
            this.isCharging = true;
         }

         return AINodeResult.RUNNING;
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.hasSpeedBurst = false;
         mob.sendMovementPacket(false);
      }
   }

   public static class IdleTime<T extends Mob> extends RunningAINode<T> {
      public int msToIdleAtNoHealth;
      public int msToIdleAtMaxHealth;
      private int timer;

      public IdleTime(int msToIdleAtNoHealth, int msToIdleAtMaxHealth) {
         this.msToIdleAtNoHealth = msToIdleAtNoHealth;
         this.msToIdleAtMaxHealth = msToIdleAtMaxHealth;
      }

      public IdleTime(int msToIdle) {
         this(msToIdle, msToIdle);
      }

      @Override
      public void start(T mob, Blackboard<T> blackboard) {
         this.timer = 0;
      }

      @Override
      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         this.timer += 50;
         int msToIdle = GameMath.lerp(mob.getHealthPercent(), this.msToIdleAtNoHealth, this.msToIdleAtMaxHealth);
         return this.timer <= msToIdle ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      @Override
      public void end(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class MoveToArenaCenterOrRandomPosition<T extends TheCursedCroneMob> extends RunningAINode<T> {
      public boolean isRunningWhileMoving;
      public int randomDistance;
      public int finalAdjustmentTimer;

      public MoveToArenaCenterOrRandomPosition(boolean isRunningWhileMoving, int randomDistance) {
         this.isRunningWhileMoving = isRunningWhileMoving;
         this.randomDistance = randomDistance;
      }

      public void start(T mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         mob.setMoveAccuracyAbility.runAndSend(5);
         this.finalAdjustmentTimer = 0;
         Point targetPos;
         if (mob.arena != null) {
            targetPos = new Point(mob.arena.tileX * 32 + 16, mob.arena.tileY * 32 + 16);
         } else if (currentTarget != null) {
            targetPos = this.getRandomPositionFrom(currentTarget.getX(), currentTarget.getY());
         } else {
            targetPos = this.getRandomPositionFrom(mob.getX(), mob.getY());
         }

         if (targetPos != null) {
            blackboard.mover.setCustomMovement(this, new MobMovementLevelPos(targetPos.x, targetPos.y));
         }
      }

      protected Point getRandomPositionFrom(int x, int y) {
         int randomAngle = GameRandom.globalRandom.nextInt(360);
         java.awt.geom.Point2D.Float angleDir = GameMath.getAngleDir(randomAngle);
         return new Point(x + (int)(angleDir.x * this.randomDistance), y + (int)(angleDir.y * this.randomDistance));
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         if (this.isRunningWhileMoving && blackboard.mover.isCurrentlyMovingFor(this)) {
            if (mob.hasArrivedAtTarget()) {
               if (this.finalAdjustmentTimer >= 500) {
                  return AINodeResult.SUCCESS;
               }

               this.finalAdjustmentTimer += 50;
            }

            return AINodeResult.RUNNING;
         } else {
            return AINodeResult.SUCCESS;
         }
      }

      public void end(T mob, Blackboard<T> blackboard) {
         mob.setMoveAccuracyAbility.runAndSend(40);
      }
   }

   public static class MoveToRandomPosition<T extends TheCursedCroneMob> extends RunningAINode<T> {
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
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         return this.isRunningWhileMoving && blackboard.mover.isMoving() ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      public void end(T mob, Blackboard<T> blackboard) {
      }
   }

   public static class SpiritBeamsRotation<T extends TheCursedCroneMob> extends RunningAINode<T> {
      private int ticker;
      private int timerBuffer;
      private int rotationTimeInSeconds;
      private boolean reversed;

      public void start(T mob, Blackboard<T> blackboard) {
         blackboard.mover.stopMoving(mob);
         this.ticker = 0;
         this.timerBuffer = 0;
         this.rotationTimeInSeconds = 3;
         this.reversed = !this.reversed;
         Point targetPoint = null;
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (mob.arena != null) {
            targetPoint = new Point(mob.arena.tileX * 32 + 16, mob.arena.tileY * 32 + 16);
         } else if (currentTarget != null) {
            targetPoint = new Point(currentTarget.getX(), currentTarget.getY());
         }

         if (targetPoint != null) {
            int distance = GameMath.limit((int)mob.getDistance(targetPoint.x, targetPoint.y), 175, 250);
            float speed = MobMovementCircle.convertToRotSpeed(distance, mob.getSpeed());
            blackboard.mover.setCustomMovement(this, new MobMovementCircleLevelPos(mob, targetPoint.x, targetPoint.y, distance, speed, this.reversed));
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            this.timerBuffer += 50;
            int spiritBeamsEvents = GameMath.lerp(mob.getHealthPercent(), 8, 2);

            while (this.timerBuffer > this.rotationTimeInSeconds * 1000 / spiritBeamsEvents) {
               this.timerBuffer = this.timerBuffer - this.rotationTimeInSeconds * 1000 / spiritBeamsEvents;
               int timeBetweenBeams = GameMath.lerp(mob.getHealthPercent(), 80, 160);
               mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.SPIRIT_BEAMS);
               GameRandom uniqueIDRandom = new GameRandom(mob.getUniqueID() + this.ticker);
               Point startPosition = new Point(currentTarget.getX(), currentTarget.getY());
               startPosition.x = startPosition.x + (int)GameMath.limit(Entity.getPositionAfterMillis(currentTarget.dx, 1500.0F), -100.0F, 100.0F);
               startPosition.y = startPosition.y + (int)GameMath.limit(Entity.getPositionAfterMillis(currentTarget.dy, 1500.0F), -100.0F, 100.0F);
               startPosition.x = startPosition.x + GameRandom.globalRandom.getIntBetween(-50, 50);
               startPosition.y = startPosition.y + GameRandom.globalRandom.getIntBetween(-50, 50);
               int angle = uniqueIDRandom.nextInt(360);
               LinedUpSpiritBeamsLevelEvent event = new LinedUpSpiritBeamsLevelEvent(mob, uniqueIDRandom, startPosition, angle, timeBetweenBeams, 8);
               mob.getLevel().entityManager.events.add(event);
            }
         }

         this.ticker++;
         return this.ticker <= 20 * this.rotationTimeInSeconds ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }

      public void end(T mob, Blackboard<T> blackboard) {
         if (blackboard.mover.isCurrentlyMovingFor(this)) {
            blackboard.mover.stopMoving(mob);
         }
      }
   }

   public class StartAnimationStateAbility extends MobAbility {
      public void runAndSend(TheCursedCroneMob.AnimationState animationState, int animationDuration) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextEnum(animationState);
         writer.putNextInt(animationDuration);
         this.runAndSendAbility(packet);
      }

      public void runAndSend(TheCursedCroneMob.AnimationState animationState) {
         this.runAndSend(animationState, 500);
      }

      @Override
      public void executePacket(PacketReader reader) {
         TheCursedCroneMob.AnimationState animationState = reader.getNextEnum(TheCursedCroneMob.AnimationState.class);
         int animationDuration = reader.getNextInt();
         TheCursedCroneMob.this.startAnimation(animationState, animationDuration, true);
      }
   }

   public static class SummonSpiritGhoulsRotation<T extends TheCursedCroneMob> extends TheCursedCroneMob.MoveToArenaCenterOrRandomPosition<T> {
      public long nextGhoulSpawnTime;
      public long endTime;

      public SummonSpiritGhoulsRotation() {
         super(true, 200);
      }

      @Override
      public void start(T mob, Blackboard<T> blackboard) {
         super.start(mob, blackboard);
         this.endTime = mob.getTime() + 6000L;
         this.nextGhoulSpawnTime = 0L;
         mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.CHANNEL_SPIRIT_GHOULS);
      }

      @Override
      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         AINodeResult superResult = super.tickRunning(mob, blackboard);
         if (superResult == AINodeResult.RUNNING) {
            return superResult;
         }

         if (this.endTime <= mob.getTime()) {
            return AINodeResult.SUCCESS;
         }

         if (this.nextGhoulSpawnTime <= mob.getTime()) {
            Mob cursedCroneSpiritGhoul = MobRegistry.getMob("cursedcronespiritghoul", mob.getLevel());
            java.awt.geom.Point2D.Float spawnPos = this.getNewSpiritSpawnPosition(mob.getX(), mob.getY());
            mob.getLevel().entityManager.addMob(cursedCroneSpiritGhoul, spawnPos.x, spawnPos.y);
            mob.spawnedMobs.add(cursedCroneSpiritGhoul);
            this.nextGhoulSpawnTime = mob.getTime() + GameMath.lerp(mob.getHealthPercent(), 100, 1000);
         }

         return AINodeResult.RUNNING;
      }

      @Override
      public void end(T mob, Blackboard<T> blackboard) {
         super.end(mob, blackboard);
         mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.DEFAULT_FLYING);
      }

      public java.awt.geom.Point2D.Float getNewSpiritSpawnPosition(float centerX, float centerY) {
         int distanceFromBoss = 325;
         java.awt.geom.Point2D.Float randomDir = GameRandom.globalRandom
            .getOneOf(
               GameMath.getAngleDir(GameRandom.globalRandom.getFloatBetween(25.0F, 65.0F)),
               GameMath.getAngleDir(GameRandom.globalRandom.getFloatBetween(115.0F, 155.0F)),
               GameMath.getAngleDir(GameRandom.globalRandom.getFloatBetween(205.0F, 245.0F)),
               GameMath.getAngleDir(GameRandom.globalRandom.getFloatBetween(295.0F, 335.0F))
            );
         float x = centerX + randomDir.x * distanceFromBoss;
         float y = centerY + randomDir.y * distanceFromBoss;
         return new java.awt.geom.Point2D.Float(x, y);
      }
   }

   public static class TheCursedCroneAI<T extends TheCursedCroneMob> extends SequenceAINode<T> {
      private int dropCombatCounter;
      private int cleanSpawnedTimer;

      public TheCursedCroneAI() {
         this.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
               blackboard.onWasHit(e -> {
                  if (!mob.isHostile) {
                     mob.changeHostileAbility.runAndSend(true);
                  }
               });
               blackboard.onRemoved(e -> {
                  mob.spawnedMobs.forEach(Mob::remove);
                  mob.spawnedEvents.forEach(LevelEvent::over);
                  mob.spawnedMobs.clear();
                  mob.spawnedEvents.clear();
               });
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               TheCursedCroneAI.this.cleanSpawnedTimer++;
               if (TheCursedCroneAI.this.cleanSpawnedTimer >= 20) {
                  mob.spawnedMobs.removeIf(Entity::removed);
                  mob.spawnedEvents.removeIf(LevelEvent::isOver);
                  TheCursedCroneAI.this.cleanSpawnedTimer = 0;
               }

               if (!mob.isHostile) {
                  blackboard.mover.stopMoving(mob);
                  return AINodeResult.FAILURE;
               }

               Mob target = blackboard.getObject(Mob.class, "currentTarget");
               if (target != null) {
                  TheCursedCroneAI.this.dropCombatCounter = 0;
               } else {
                  TheCursedCroneAI.this.dropCombatCounter++;
                  if (TheCursedCroneAI.this.dropCombatCounter > 100) {
                     TheCursedCroneAI.this.dropCombatCounter = 0;
                     mob.setHealth(mob.getMaxHealth());
                     mob.changeHostileAbility.runAndSend(false);
                     mob.spawnedMobs.forEach(Mob::remove);
                     mob.spawnedEvents.forEach(LevelEvent::over);
                     mob.spawnedMobs.clear();
                     mob.spawnedEvents.clear();
                  }
               }

               return AINodeResult.SUCCESS;
            }
         });
         TargetFinderAINode<T> targetFinderAINode = new TargetFinderAINode<T>(416) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }

            public Point getBase(T mob, Blackboard<T> blackboard) {
               return mob.arena != null ? new Point(mob.arena.tileX * 32 + 16, mob.arena.tileY * 32 + 16) : super.getBase(mob, blackboard);
            }
         };
         targetFinderAINode.noTargetFoundMinCooldown = 500;
         targetFinderAINode.noTargetFoundMaxCooldown = 1000;
         this.addChild(targetFinderAINode);
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         TheCursedCroneMob.TicTacToeRotation<T> ticTacToeRotation = new TheCursedCroneMob.TicTacToeRotation<>();
         attackStages.addChild(new TheCursedCroneMob.DiagonalChargeRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(100, 500));
         attackStages.addChild(new TheCursedCroneMob.ChargeOppositeTargetDirection<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(100, 500));
         attackStages.addChild(new TheCursedCroneMob.ChargeOppositeTargetDirection<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.SpiritBeamsRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(500, 1000));
         attackStages.addChild(new TheCursedCroneMob.ChargeOppositeTargetDirection<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.ChargeOppositeTargetDirection<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.SummonSpiritGhoulsRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.SpiritBeamsRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.DiagonalChargeRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(100, 500));
         attackStages.addChild(new TheCursedCroneMob.ChargeOppositeTargetDirection<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.MoveToArenaCenterOrRandomPosition<>(true, 200));
         attackStages.addChild(new TheCursedCroneMob.TornadoDanceRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(1000, 4000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.SpiritBeamsRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         attackStages.addChild(new TheCursedCroneMob.SummonSpiritGhoulsRotation<>());
         attackStages.addChild(new TheCursedCroneMob.IdleTime<>(200, 1000));
         attackStages.addChild(ticTacToeRotation);
         this.addChild(new IsolateRunningAINode<>(attackStages));
      }
   }

   public static class TicTacToeRotation<T extends TheCursedCroneMob> extends RunningAINode<T> {
      public float[] triggerEventAtHealthPercent = new float[]{0.8F, 0.4F};
      public TicTacToeLevelEvent ticTacToeLevelEvent;
      public TicTacToeLevelEvent.TileState gameResult;
      public CursedCroneSpiritSkullsEvent spiritSkullsEvent;
      public long nextRefreshInvulnerableBuffTime;
      public long croneWonEndTime;
      public ArrayList<Projectile> croneWonProjectiles;
      public int nextTriggerIndex = 0;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         super.onRootSet(root, mob, blackboard);
         blackboard.onBeforeHit(e -> {
            if (e.event.attacker instanceof TicTacToePunishProjectile) {
               e.event.prevent();
               e.event.showDamageTip = false;
               e.event.playHitSound = false;
               if (mob.animationState != TheCursedCroneMob.AnimationState.SHIELD_EXPLODE) {
                  mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.SHIELD_EXPLODE);
               }
            }
         });
      }

      public void start(T mob, Blackboard<T> blackboard) {
         this.croneWonProjectiles = new ArrayList<>();
         this.gameResult = null;
         this.croneWonEndTime = 0L;
         if (this.spiritSkullsEvent != null) {
            mob.spawnedEvents.remove(this.spiritSkullsEvent);
            this.spiritSkullsEvent.over();
            this.spiritSkullsEvent = null;
         }

         if (this.ticTacToeLevelEvent != null) {
            mob.spawnedEvents.remove(this.ticTacToeLevelEvent);
            this.ticTacToeLevelEvent.over();
            this.ticTacToeLevelEvent = null;
         }

         if (this.nextTriggerIndex < this.triggerEventAtHealthPercent.length
            && !(mob.getHealthPercent() > this.triggerEventAtHealthPercent[this.nextTriggerIndex])) {
            this.nextTriggerIndex++;
            Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
            java.awt.geom.Point2D.Float targetPos = null;
            if (mob.arena != null) {
               mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.CHANNEL_SHIELD);
               TicTacToeLevelEvent.GameEndedEvent gameEndedEvent = (winner, winnerTiles, xPlayer, oPlayer) -> {
                  this.gameResult = winner;
                  if (this.spiritSkullsEvent != null) {
                     mob.spawnedEvents.remove(this.spiritSkullsEvent);
                     this.spiritSkullsEvent.over();
                     this.spiritSkullsEvent = null;
                  }

                  if (winner == TicTacToeLevelEvent.TileState.X) {
                     for (PlayerMob player : mob.streamAttackers()
                        .map(Attacker::getFirstPlayerOwner)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList())) {
                        if (!player.removed() && player.isSamePlace(mob)) {
                           float damage = (player.getMaxHealth() + player.getResilience())
                              * TheCursedCroneMob.punishProjectileDamagePercent
                              / winnerTiles.length;
                           this.croneWonProjectiles
                              .addAll(TicTacToeLevelEvent.spawnPunishProjectiles(winnerTiles, player, new GameDamage(DamageTypeRegistry.TRUE, damage), true));
                        }
                     }

                     this.croneWonEndTime = mob.getTime() + 5000L;
                  } else if (winner == TicTacToeLevelEvent.TileState.O) {
                     TicTacToeLevelEvent.spawnPunishProjectiles(winnerTiles, mob, new GameDamage(0.0F), false);

                     for (PlayerMob player : mob.streamAttackers()
                        .map(Attacker::getFirstPlayerOwner)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList())) {
                        ServerClient serverClient = player.getServerClient();
                        JournalChallenge challenge = JournalChallengeRegistry.getChallenge(JournalChallengeRegistry.WIN_TIC_TAC_TOE_VS_CRONE_ID);
                        if (!challenge.isCompleted(serverClient) && challenge.isJournalEntryDiscovered(serverClient)) {
                           challenge.markCompleted(serverClient);
                           serverClient.forceCombineNewStats();
                        }
                     }
                  }
               };
               this.ticTacToeLevelEvent = new TicTacToeLevelEvent(mob.arena.tileX, mob.arena.tileY, mob, null, gameEndedEvent);
               Level level = mob.getLevel();
               mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.INVULNERABLE_ACTIVE, mob, 5.0F, null), true);
               this.nextRefreshInvulnerableBuffTime = mob.getTime() + 3000L;
               level.entityManager.events.add(this.ticTacToeLevelEvent);
               mob.spawnedEvents.add(this.ticTacToeLevelEvent);
               if (!mob.arena.spiritSkullTiles.isEmpty()) {
                  int shootInterval = GameMath.lerp(mob.getHealthPercent(), 250, 1000);
                  ArrayList<Point> skullPoints = mob.arena
                     .spiritSkullTiles
                     .stream()
                     .map(tile -> new Point(tile.x * 32 + 16, tile.y * 32 + 22))
                     .collect(Collectors.toCollection(ArrayList::new));
                  this.spiritSkullsEvent = new CursedCroneSpiritSkullsEvent(mob, skullPoints, shootInterval);
                  level.entityManager.events.add(this.spiritSkullsEvent);
                  mob.spawnedEvents.add(this.spiritSkullsEvent);
               }
            }

            if (mob.arena != null) {
               targetPos = new java.awt.geom.Point2D.Float(mob.arena.tileX * 32 + 16, (mob.arena.tileY - 6) * 32 + 16);
            } else if (currentTarget != null) {
               targetPos = new java.awt.geom.Point2D.Float(currentTarget.x, currentTarget.y - 150.0F);
            }

            if (targetPos != null) {
               blackboard.mover.setCustomMovement(this, new MobMovementLevelPos(targetPos.x, targetPos.y));
            }
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         if (this.ticTacToeLevelEvent == null) {
            return AINodeResult.SUCCESS;
         }

         if (mob.animationState == TheCursedCroneMob.AnimationState.CHANNEL_SHIELD && this.nextRefreshInvulnerableBuffTime <= mob.getTime()) {
            mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.INVULNERABLE_ACTIVE, mob, 5.0F, null), true);
            this.nextRefreshInvulnerableBuffTime = mob.getTime() + 3000L;
         }

         if (this.croneWonEndTime != 0L) {
            this.croneWonProjectiles.removeIf(Entity::removed);
            if (this.croneWonEndTime > mob.getTime() && !this.croneWonProjectiles.isEmpty()) {
               return AINodeResult.RUNNING;
            }

            mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.FADE_OUT_SHIELD);
            return AINodeResult.SUCCESS;
         } else {
            if (this.spiritSkullsEvent != null) {
               this.spiritSkullsEvent.refreshAliveTimer();
            }

            if (this.gameResult != null) {
               if (this.gameResult == TicTacToeLevelEvent.TileState.O) {
                  return mob.animationState == TheCursedCroneMob.AnimationState.DEFAULT_FLYING ? AINodeResult.SUCCESS : AINodeResult.RUNNING;
               }

               mob.startAnimationStateAbility.runAndSend(TheCursedCroneMob.AnimationState.FADE_OUT_SHIELD);
               return AINodeResult.SUCCESS;
            } else {
               return AINodeResult.RUNNING;
            }
         }
      }

      public void end(T mob, Blackboard<T> blackboard) {
         while (
            this.nextTriggerIndex < this.triggerEventAtHealthPercent.length
               && mob.getHealthPercent() <= this.triggerEventAtHealthPercent[this.nextTriggerIndex]
         ) {
            this.nextTriggerIndex++;
         }

         if (this.spiritSkullsEvent != null) {
            this.spiritSkullsEvent.over();
            mob.spawnedEvents.remove(this.spiritSkullsEvent);
         }

         if (this.ticTacToeLevelEvent != null) {
            mob.spawnedEvents.remove(this.ticTacToeLevelEvent);
            this.ticTacToeLevelEvent = null;
         }
      }
   }

   public static class TornadoDanceRotation<T extends TheCursedCroneMob> extends RunningAINode<T> {
      public long nextTornadoSpawnTime = 0L;
      public int spawnedTornadoes = 0;

      public void start(T mob, Blackboard<T> blackboard) {
         this.spawnedTornadoes = 0;
         int initialTornadoSpawns = 3;

         for (int i = 0; i < initialTornadoSpawns; i++) {
            this.spawnTornado(mob);
            this.spawnedTornadoes++;
         }
      }

      public AINodeResult tickRunning(T mob, Blackboard<T> blackboard) {
         if (this.nextTornadoSpawnTime <= mob.getTime()) {
            this.spawnTornado(mob);
            this.spawnedTornadoes++;
            int maxTornadoes = GameMath.lerp(mob.getHealthPercent(), 30, 15);
            if (this.spawnedTornadoes >= maxTornadoes) {
               return AINodeResult.SUCCESS;
            }

            int nextTornadoDelay = GameMath.lerp(mob.getHealthPercent(), 300, 1200);
            this.nextTornadoSpawnTime = mob.getTime() + nextTornadoDelay;
         }

         return AINodeResult.RUNNING;
      }

      public void end(T mob, Blackboard<T> blackboard) {
      }

      protected void spawnTornado(T mob) {
         Mob tornado = MobRegistry.getMob("spirittornado", mob.getLevel());
         tornado.setPos(mob.x, mob.y, true);
         int distanceAtTheEnd = 1200;
         int radiusDecrease = 100;
         int semiCircles = MobMovementSpiral.getSemiCircles(distanceAtTheEnd, radiusDecrease, 10.0F);
         int startAngle = GameRandom.globalRandom.nextInt(360);
         boolean clockwise = GameRandom.globalRandom.nextBoolean();
         tornado.setMovement(
            new MobMovementSpiralLevelPos(tornado, mob.x, mob.y, 10.0F, semiCircles, -radiusDecrease, tornado.getSpeed(), startAngle, clockwise)
         );
         mob.getLevel().entityManager.mobs.add(tornado);
         mob.spawnedMobs.add(tornado);
      }
   }
}
