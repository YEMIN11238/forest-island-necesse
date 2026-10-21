package necesse.entity.mobs.hostile.bosses;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketLevelEventOver;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.storyObjectives.listeners.MobKilledStatAddedStoryObjectiveListener;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.levelEvent.TempleEntranceEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.FireDanceLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.entity.mobs.WormMoveLine;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RemoveOnNoTargetNode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.mobs.mobMovement.MobMovementCircleRelative;
import necesse.entity.mobs.mobMovement.MobMovementConstant;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.MobConditionLootItemList;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FlyingSpiritsHead extends BossWormMobHead<FlyingSpiritsBody, FlyingSpiritsHead> {
   private SoundPlayer sound;
   public static LootTable lootTable = new LootTable(new LootItemMultiplierIgnored(new ChanceLootItem(0.2F, "symphonyoftwinsvinyl")));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      "sageandgrit", new LootItem("dragonsrebound"), new LootItem("dragonlance"), new LootItem("bowofdualism"), new LootItem("skeletonstaff")
   );
   public static LootTable privateLootTable = new LootTable(
      new MobConditionLootItemList(mob -> mob.getLevel() == null || !mob.getLevel().isIncursionLevel, uniqueDrops)
   );
   public static float lengthPerBodyPart = 50.0F;
   public static float waveLength = 800.0F;
   public static final int totalBodyParts = 13;
   public static MaxHealthGetter BASE_MAX_HEALTH = new MaxHealthGetter(21000, 27000, 30000, 33000, 39000);
   public static MaxHealthGetter INCURSION_MAX_HEALTH = new MaxHealthGetter(32000, 40000, 44000, 48000, 56000);
   public GameDamage collisionDamage;
   public GameDamage fireDamage;
   public static GameDamage baseHeadCollisionDamage = new GameDamage(115.0F);
   public static GameDamage baseBodyCollisionDamage = new GameDamage(100.0F);
   public static GameDamage baseFireDamage = new GameDamage(100.0F);
   public static GameDamage incursionHeadCollisionDamage = new GameDamage(150.0F);
   public static GameDamage incursionBodyCollisionDamage = new GameDamage(130.0F);
   public static GameDamage incursionFireDamage = new GameDamage(115.0F);
   public final LevelMob<FlyingSpiritsHead> friend = new LevelMob<>();
   public Point pedestalPosition;
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   protected FlyingSpiritsHead.Variant variant;
   protected boolean isEnraged;
   protected boolean isEscaping;

   public FlyingSpiritsHead(FlyingSpiritsHead.Variant variant) {
      super(100, waveLength, 100.0F, 13, 25.0F, -5.0F);
      this.variant = variant;
      this.difficultyChanges.setMaxHealth(BASE_MAX_HEALTH);
      this.moveAccuracy = 100;
      this.movementUpdateCooldown = 2000;
      this.movePosTolerance = 700.0F;
      this.setSpeed(180.0F);
      this.setArmor(35);
      this.accelerationMod = 1.0F;
      this.decelerationMod = 1.0F;
      this.collision = new Rectangle(-25, -20, 50, 40);
      this.hitBox = new Rectangle(-30, -25, 60, 50);
      this.selectBox = new Rectangle(-40, -60, 80, 64);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.friend.uniqueID = reader.getNextInt();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.friend.uniqueID);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isEnraged);
      writer.putNextBoolean(this.isEscaping);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isEnraged = reader.getNextBoolean();
      this.isEscaping = reader.getNextBoolean();
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
   protected void onAppearAbility() {
      super.onAppearAbility();
      if (this.isClient()) {
         SoundManager.playSound(GameResources.roar, SoundEffect.globalEffect().pitch(1.2F));
      }
   }

   protected float getDistToBodyPart(FlyingSpiritsBody bodyPart, int index, float lastDistance) {
      return index == 1 ? lengthPerBodyPart + 10.0F : lengthPerBodyPart;
   }

   protected FlyingSpiritsBody createNewBodyPart(int index) {
      FlyingSpiritsBody bodyPart = new FlyingSpiritsBody();
      int tailParts = 4;
      if (index == 0) {
         bodyPart.spriteY = 1;
      } else if (index == 2) {
         bodyPart.spriteY = 3;
      } else if (index == 13 - tailParts - 2) {
         bodyPart.spriteY = 3;
      } else if (index >= 13 - tailParts) {
         bodyPart.spawnsParticles = true;
         int tailPart = Math.abs(13 - index - tailParts);
         bodyPart.spriteY = 4 + tailPart;
      } else {
         bodyPart.spriteY = 1;
      }

      return bodyPart;
   }

   @Override
   protected void playMoveSound() {
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
   public boolean dropsLoot() {
      FlyingSpiritsHead friend = this.friend.get(this.getLevel());
      return friend != null && !friend.removed() ? false : super.dropsLoot();
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return baseHeadCollisionDamage;
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return this.height < 45.0F && super.canCollisionHit(target);
   }

   @Override
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
   }

   @Override
   public void init() {
      if (this.getLevel() instanceof IncursionLevel) {
         this.difficultyChanges.setMaxHealth(INCURSION_MAX_HEALTH);
         this.setHealth(this.getMaxHealth());
         this.collisionDamage = incursionHeadCollisionDamage;
         this.fireDamage = incursionFireDamage;
      } else {
         this.collisionDamage = baseHeadCollisionDamage;
         this.fireDamage = baseFireDamage;
      }

      super.init();
      this.ai = new BehaviourTreeAI<>(this, new FlyingSpiritsHead.FlyingSpiritsHeadAI<>(), new FlyingAIMover());
      if (this.isClient()) {
      }

      this.streamBodyParts().forEach(bp -> bp.variant = this.variant);
   }

   @Override
   public float getTurnSpeed(float delta) {
      return super.getTurnSpeed(delta) * (this.isEnraged ? 2.0F : 1.0F);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.sound == null || this.sound.isDone()) {
         this.sound = SoundManager.playSound(GameResources.wind1, SoundEffect.effect(this).falloffDistance(1400).volume(0.4F));
      }

      if (this.sound != null) {
         this.sound.refreshLooping(1.0F);
      }

      if (!this.isEscaping && this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.SymphonyOfTwins, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      this.setSpeed(!this.isEscaping && !this.isEnraged ? 180.0F : 250.0F);
      this.accelerationMod = this.isEnraged ? 1.3F : 1.0F;
      this.decelerationMod = this.isEnraged ? 1.3F : 1.0F;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
      if (!this.isEnraged) {
         FlyingSpiritsHead other = this.friend.get(this.getLevel());
         if (other == null || this.getDistance(other) > 3840.0F) {
            this.isEnraged = true;
            this.sendMovementPacket(false);
         }
      }

      this.setSpeed(!this.isEscaping && !this.isEnraged ? 180.0F : 280.0F);
      this.accelerationMod = this.isEnraged ? 1.3F : 1.0F;
      this.decelerationMod = this.isEnraged ? 1.3F : 1.0F;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      this.getLevel()
         .entityManager
         .addParticle(
            new FleshParticle(
               this.getLevel(),
               MobRegistry.Textures.flyingSpirits,
               4 + this.variant.spriteX,
               GameRandom.globalRandom.nextInt(6),
               64,
               this.x,
               this.y,
               20.0F,
               knockbackX,
               knockbackY
            ),
            Particle.GType.IMPORTANT_COSMETIC
         );
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
      if (this.isVisible()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - 64;
         int drawY = camera.getDrawY(this.y);
         float headAngle = GameMath.fixAngle(GameMath.getAngle(new Float(this.dx, this.dy)));
         final MobDrawable headDrawable = WormMobHead.getAngledDrawable(
            this,
            new GameSprite(MobRegistry.Textures.flyingSpirits, this.variant.spriteX, 0, 128),
            null,
            light,
            (int)this.height,
            headAngle,
            drawX,
            drawY,
            96,
            perspective
         );
         new ComputedObjectValue<>(null, () -> 0.0);
         ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> shoulderLine = WormMobHead.moveDistance(this.moveLines.getFirstElement(), 70.0);
         final MobDrawable shoulderDrawable;
         if (shoulderLine.object != null) {
            java.awt.geom.Point2D.Double shoulderPos = WormMobHead.linePos(shoulderLine);
            GameLight shoulderLight = level.getLightLevel(getTileCoordinate(shoulderPos.x), getTileCoordinate(shoulderPos.y));
            int shoulderDrawX = camera.getDrawX((float)shoulderPos.x) - 64;
            int shoulderDrawY = camera.getDrawY((float)shoulderPos.y);
            float shoulderHeight = this.getWaveHeight(
               ((WormMoveLine)((GameLinkedList.Element)shoulderLine.object).object).movedDist + ((Double)shoulderLine.get()).floatValue()
            );
            float shoulderAngle = GameMath.fixAngle(
               (float)GameMath.getAngle(new java.awt.geom.Point2D.Double(this.x - shoulderPos.x, this.y - this.height - (shoulderPos.y - shoulderHeight)))
            );
            shoulderDrawable = WormMobHead.getAngledDrawable(
               this,
               new GameSprite(MobRegistry.Textures.flyingSpirits, this.variant.spriteX, 1, 128),
               null,
               shoulderLight,
               (int)shoulderHeight,
               shoulderAngle,
               shoulderDrawX,
               shoulderDrawY,
               96,
               perspective
            );
         } else {
            shoulderDrawable = null;
         }

         topList.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               if (shoulderDrawable != null) {
                  shoulderDrawable.draw(tickManager);
               }

               headDrawable.draw(tickManager);
            }
         });
         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.swampGuardian_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(2, 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return this.isVisible();
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 24;
      int drawY = y - 24;
      float headAngle = GameMath.fixAngle(GameMath.getAngle(new Float(this.dx, this.dy)));
      MobRegistry.Textures.flyingSpirits.initDraw().sprite(this.variant.spriteX, 0, 128).rotate(headAngle + 90.0F, 24, 24).size(48, 48).draw(drawX, drawY);
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-15, -15, 30, 30);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return !this.isVisible() ? null : new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(
         new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.0F),
         new ModifierValue<>(BuffModifiers.POISON_DAMAGE_FLAT, 0.0F).max(0.0F),
         new ModifierValue<>(BuffModifiers.FIRE_DAMAGE_FLAT, 0.0F).max(0.0F),
         new ModifierValue<>(BuffModifiers.FROST_DAMAGE_FLAT, 0.0F).max(0.0F)
      );
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      boolean defeatedOther = this.friend.get(this.getLevel()) == null;
      boolean openingStaircase = false;
      if (defeatedOther && this.isServer()) {
         if (this.pedestalPosition != null && !(this.getLevel() instanceof IncursionLevel)) {
            Point entrancePosition = new Point(this.pedestalPosition.x, this.pedestalPosition.y + 2);
            if (!this.getLevel().getLevelObject(entrancePosition.x, entrancePosition.y).getMultiTile().getMasterObject().getStringID().equals("templeentrance")
               )
             {
               this.getLevel().entityManager.events.add(new TempleEntranceEvent(this.pedestalPosition.x, this.pedestalPosition.y + 2));
               openingStaircase = true;
            }
         }

         if (!this.isDamagedByPlayers) {
            AchievementManager.checkMeAndThisArmyKill(this.getLevel(), attackers);
         }
      }

      boolean finalOpeningStaircase = openingStaircase;
      attackers.stream()
         .map(Attacker::getFirstPlayerOwner)
         .filter(Objects::nonNull)
         .filter(PlayerMob::isServerClient)
         .map(PlayerMob::getServerClient)
         .distinct()
         .forEach(c -> {
            if (defeatedOther) {
               c.newStats.mob_kills.addKill("sageandgrit");
               c.storyManager.streamObjectives(MobKilledStatAddedStoryObjectiveListener.class).forEach(challenge -> challenge.onMobStatAdded("sageandgrit"));
            }

            c.sendChatMessage(new LocalMessage("misc", "bossdefeat", "name", this.getLocalization()));
            if (finalOpeningStaircase) {
               c.sendChatMessage(new LocalMessage("misc", "staircaseopening"));
            }
         });
   }

   public boolean controlsAI() {
      return this.variant == FlyingSpiritsHead.Variant.GRIT || this.isEnraged();
   }

   public boolean isEnraged() {
      return this.isEnraged;
   }

   public boolean isEscaping() {
      return this.isEscaping;
   }

   public static class FlyingSpiritsHeadAI<T extends FlyingSpiritsHead> extends SequenceAINode<T> {
      public FlyingSpiritsHead.FlyingSpiritsHeadAINode<T> node;

      public FlyingSpiritsHeadAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         TargetFinderAINode<T> targetFinder;
         this.addChild(targetFinder = new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         targetFinder.moveToAttacker = false;
         this.addChild(this.node = new FlyingSpiritsHead.FlyingSpiritsHeadAINode<>());
      }
   }

   public static class FlyingSpiritsHeadAINode<T extends FlyingSpiritsHead> extends AINode<T> {
      public String targetKey = "currentTarget";
      private int startMoveAccuracy;
      protected FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState state;
      protected Mob chargingTarget;
      protected float circlingAngleOffset;
      protected float circlingSpeed;
      protected boolean circlingReversed;
      protected boolean lastWasCharge;
      protected float fireDanceTargetX;
      protected float fireDanceTargetY;
      protected long fireDanceStateTime;
      protected boolean fireDanceActive;
      protected FireDanceLevelEvent fireDanceLevelEvent;
      public long nextStateTime;
      public int circlingRange = 500;
      public int fireDanceRange = 300;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         this.startMoveAccuracy = mob.moveAccuracy;
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public float getHealthPercent() {
         float myHealthPercent = GameMath.limit((float)this.mob().getHealth() / this.mob().getMaxHealth(), 0.0F, 1.0F);
         FlyingSpiritsHead friend = this.mob().friend.get(this.mob().getLevel());
         if (friend != null) {
            float friendHealthPercent = GameMath.limit((float)friend.getHealth() / friend.getMaxHealth(), 0.0F, 1.0F);
            return (myHealthPercent + friendHealthPercent) / 2.0F;
         } else {
            return myHealthPercent;
         }
      }

      public int getRandomCooldownBasedOnMissingHealth(int minimum, int maximum, int range) {
         float healthPercent = this.getHealthPercent();
         int totalRange = maximum - minimum;
         range = GameMath.limit(range, 1, Math.abs(totalRange));
         if (totalRange < 0) {
            range = -range;
         }

         int added = (int)(healthPercent * (totalRange - range));
         int randomInt = range < 0 ? -GameRandom.globalRandom.nextInt(-range + 1) : GameRandom.globalRandom.nextInt(range + 1);
         return minimum + added + randomInt;
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (mob.isEscaping()) {
            blackboard.mover.setCustomMovement(this, new MobMovementConstant(mob.dx, mob.dy));
            return AINodeResult.SUCCESS;
         }

         if (this.state == null) {
            this.startCircling();
            this.nextStateTime = mob.getWorldEntity().getTime() + 15000L;
         } else if (this.state == FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.Charging) {
            if (this.chargingTarget != null && !this.chargingTarget.removed()) {
               float distance = mob.getDistance(this.chargingTarget);
               if (distance < this.circlingRange / 4.0F) {
                  float currentAngle = GameMath.getAngle(new Float(mob.dx, mob.dy));
                  float targetAngle = GameMath.getAngle(new Float(this.chargingTarget.x - mob.x, this.chargingTarget.y - mob.y));
                  float diff = GameMath.getAngleDifference(currentAngle, targetAngle);
                  float maxAngle = mob.isEnraged() ? 20.0F : 40.0F;
                  if (Math.abs(diff) >= maxAngle && distance > 75.0F || mob.dx == 0.0F && mob.dy == 0.0F) {
                     mob.moveAccuracy = this.startMoveAccuracy;
                     this.startCircling();
                  }
               }
            } else {
               this.startCircling();
            }
         } else if (this.state == FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.FireDance) {
            if (this.fireDanceStateTime < mob.getWorldEntity().getTime()) {
               if (this.fireDanceActive) {
                  this.startCircling();
               } else {
                  this.startFireDanceLaser();
               }
            } else if (this.mob().controlsAI()) {
               boolean anyTargetsInsideArea = mob.getLevel()
                  .entityManager
                  .players
                  .streamArea(this.fireDanceTargetX, this.fireDanceTargetY, this.fireDanceRange)
                  .anyMatch(
                     m -> m != null && !m.removed() && m.isVisible()
                        ? m.getDistance(this.fireDanceTargetX, this.fireDanceTargetY) <= this.fireDanceRange
                        : false
                  );
               if (!anyTargetsInsideArea) {
                  this.startCircling();
                  this.friendAI(FlyingSpiritsHead.FlyingSpiritsHeadAINode::startCircling);
               }
            }
         }

         if (mob.controlsAI()) {
            FlyingSpiritsHead.FlyingSpiritsHeadAINode<?> friendAI = this.getFriendAI();
            if (this.state == FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.Circling
               && (friendAI == null || friendAI.state == FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.Circling)
               && this.nextStateTime < mob.getWorldEntity().getTime()) {
               if (!mob.isEnraged() && this.lastWasCharge && !GameRandom.globalRandom.nextBoolean()) {
                  this.lastWasCharge = false;
                  this.startFireDance();
               } else {
                  this.lastWasCharge = true;
                  this.startCharging();
               }

               this.nextStateTime = mob.getWorldEntity().getTime() + 4000L;
            }
         }

         return AINodeResult.SUCCESS;
      }

      public Mob getTarget() {
         return this.getBlackboard().getObject(Mob.class, this.targetKey);
      }

      public void friendAI(Consumer<FlyingSpiritsHead.FlyingSpiritsHeadAINode<?>> consumer) {
         FlyingSpiritsHead.FlyingSpiritsHeadAINode<?> friendAI = this.getFriendAI();
         if (friendAI != null) {
            consumer.accept(friendAI);
         }
      }

      public FlyingSpiritsHead.FlyingSpiritsHeadAINode<?> getFriendAI() {
         FlyingSpiritsHead friend = this.mob().friend.get(this.mob().getLevel());
         return friend != null && !friend.isEnraged() ? ((FlyingSpiritsHead.FlyingSpiritsHeadAI)friend.ai.tree).node : null;
      }

      public void startCircling() {
         this.stopExistingFireDance();
         Mob target = this.getBlackboard().getObject(Mob.class, this.targetKey);
         this.mob().moveAccuracy = this.startMoveAccuracy;
         this.state = FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.Circling;
         if (this.mob().isEnraged()) {
            this.nextStateTime = this.mob().getWorldEntity().getTime() + this.getRandomCooldownBasedOnMissingHealth(1000, 4000, 1000);
         } else {
            this.nextStateTime = this.mob().getWorldEntity().getTime() + this.getRandomCooldownBasedOnMissingHealth(2000, 6000, 1000);
         }

         FlyingSpiritsHead.FlyingSpiritsHeadAINode<?> friendAI = this.getFriendAI();
         if (friendAI != null && friendAI.state == FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.Circling) {
            this.circlingReversed = friendAI.circlingReversed;
            this.circlingAngleOffset = friendAI.circlingAngleOffset + 180.0F;
            this.circlingSpeed = friendAI.circlingSpeed;
            if (target != null) {
               this.getBlackboard()
                  .mover
                  .setCustomMovement(
                     this,
                     new MobMovementCircleRelative(this.mob(), target, this.circlingRange, this.circlingSpeed, this.circlingAngleOffset, this.circlingReversed)
                  );
            } else {
               this.getBlackboard()
                  .mover
                  .setCustomMovement(
                     this,
                     new MobMovementCircleLevelPos(
                        this.mob(), this.mob().x, this.mob().y, this.circlingRange, this.circlingSpeed, this.circlingAngleOffset, this.circlingReversed
                     )
                  );
            }
         } else {
            this.circlingSpeed = MobMovementCircle.convertToRotSpeed(this.circlingRange, this.mob().getSpeed()) * 1.1F;
            MobMovementCircle movement;
            if (target != null) {
               movement = new MobMovementCircleRelative(this.mob(), target, this.circlingRange, this.circlingSpeed, GameRandom.globalRandom.nextBoolean());
            } else {
               movement = new MobMovementCircleLevelPos(
                  this.mob(), this.mob().x, this.mob().y, this.circlingRange, this.circlingSpeed, GameRandom.globalRandom.nextBoolean()
               );
            }

            this.circlingReversed = movement.reversed;
            this.circlingAngleOffset = movement.angleOffset;
            this.getBlackboard().mover.setCustomMovement(this, movement);
         }
      }

      public void startCharging() {
         Mob target = this.getTarget();
         if (target != null) {
            this.mob().moveAccuracy = 5;
            this.state = FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.Charging;
            this.chargingTarget = target;
            this.getBlackboard().mover.setCustomMovement(this, new MobMovementRelative(target, 0.0F, 0.0F));
         }

         if (this.mob().controlsAI()) {
            this.friendAI(FlyingSpiritsHead.FlyingSpiritsHeadAINode::startCharging);
         }
      }

      public void startFireDance() {
         Mob target = this.getTarget();
         if (target != null) {
            float speed = MobMovementCircle.convertToRotSpeed(this.fireDanceRange, this.mob().getSpeed()) * 1.1F;
            float angleOffset = (new MobMovementCircleLevelPos(this.mob(), target.x, target.y, this.fireDanceRange, speed, this.circlingReversed)).angleOffset;
            this.startFireDance(target.x, target.y, this.fireDanceRange, speed, angleOffset, this.circlingReversed);
            if (this.mob().controlsAI()) {
               this.friendAI(ai -> ai.startFireDance(target.x, target.y, this.fireDanceRange, speed, angleOffset + 180.0F, this.circlingReversed));
            }
         }
      }

      public void startFireDanceLaser() {
         if (this.mob().controlsAI()) {
            int fireTime = this.getRandomCooldownBasedOnMissingHealth(3000, 6000, 1000);
            this.fireDanceStateTime = this.mob().getWorldEntity().getTime() + fireTime;
            this.mob().buffManager.addBuff(new ActiveBuff(BuffRegistry.HARDENED, this.mob(), fireTime, null), true, true);
            this.stopExistingFireDance();
            this.mob()
               .getLevel()
               .entityManager
               .events
               .add(
                  this.fireDanceLevelEvent = new FireDanceLevelEvent(
                     this.mob(),
                     new GameRandom(),
                     this.fireDanceTargetX,
                     this.fireDanceTargetY,
                     this.mob().fireDamage,
                     100,
                     this.mob().variant.fireColor,
                     fireTime
                  )
               );
            this.fireDanceActive = true;
            this.friendAI(
               ai -> {
                  ai.fireDanceStateTime = this.fireDanceStateTime;
                  ai.fireDanceActive = true;
                  ai.mob().buffManager.addBuff(new ActiveBuff(BuffRegistry.HARDENED, ai.mob(), fireTime, null), true, true);
                  ai.stopExistingFireDance();
                  ai.mob()
                     .getLevel()
                     .entityManager
                     .events
                     .add(
                        ai.fireDanceLevelEvent = new FireDanceLevelEvent(
                           ai.mob(),
                           new GameRandom(),
                           this.fireDanceTargetX,
                           this.fireDanceTargetY,
                           this.mob().fireDamage,
                           100,
                           ai.mob().variant.fireColor,
                           fireTime
                        )
                     );
               }
            );
         } else {
            this.fireDanceStateTime = this.mob().getWorldEntity().getTime() + 2000L;
            this.fireDanceActive = true;
         }
      }

      public void stopExistingFireDance() {
         if (this.fireDanceLevelEvent != null) {
            this.fireDanceLevelEvent.over();
            if (this.mob().isServer()) {
               this.mob().getLevel().getServer().network.sendToClientsWithEntity(new PacketLevelEventOver(this.fireDanceLevelEvent.getUniqueID()), this.mob());
            }

            this.fireDanceLevelEvent = null;
         }
      }

      public void startFireDance(float x, float y, int range, float speed, float angleOffset, boolean circlingReversed) {
         this.fireDanceTargetX = x;
         this.fireDanceTargetY = y;
         this.state = FlyingSpiritsHead.FlyingSpiritsHeadAINode.MState.FireDance;
         this.fireDanceActive = false;
         this.fireDanceStateTime = this.mob().getWorldEntity().getTime() + 3500L;
         this.getBlackboard().mover.setCustomMovement(this, new MobMovementCircleLevelPos(this.mob(), x, y, range, speed, angleOffset, circlingReversed));
      }

      private enum MState {
         Circling,
         Charging,
         FireDance;
      }
   }

   public enum Variant {
      GRIT(0, 20.0F, new Color(184, 102, 40)),
      SAGE(1, 200.0F, new Color(65, 105, 151));

      public final int spriteX;
      public final float particleHue;
      public final Color fireColor;

      Variant(int spriteX, float particleHue, Color fireColor) {
         this.spriteX = spriteX;
         this.particleHue = particleHue;
         this.fireColor = fireColor;
      }
   }
}
