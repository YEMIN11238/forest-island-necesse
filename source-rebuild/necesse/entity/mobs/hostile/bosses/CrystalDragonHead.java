package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.GameTileRange;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystalBombEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.CrystalDragonLaserLevelEvent;
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
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.FloatMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
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
import necesse.entity.mobs.mobMovement.MobMovementCircle;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.mobs.mobMovement.MobMovementCircleRelative;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.CrystalDragonShardProjectile;
import necesse.entity.projectile.CrystalGolemSpawnProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CrystalDragonHead extends BossWormMobHead<CrystalDragonBody, CrystalDragonHead> {
   public static RotationLootItem vinylRotation = RotationLootItem.globalLootRotation(
      new LootItemList(),
      new LootItemList(),
      new LootItemList(),
      RotationLootItem.globalLootRotation(4, new LootItem("pastbehindglassvinyl"), new LootItem("dragonshoardvinyl"))
   );
   public static LootTable lootTable = new LootTable(new LootItemMultiplierIgnored(vinylRotation));
   protected SoundPlayer windSound;
   public static float lengthPerBodyPart = 60.0F;
   public static float waveLength = 800.0F;
   public static final int totalBodyParts = 7;
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(35000, 46000, 52000, 58000, 69000);
   public static GameDamage headCollisionDamage = new GameDamage(150.0F);
   public static GameDamage bodyCollisionDamage = new GameDamage(115.0F);
   public static GameDamage shardDamage = new GameDamage(115.0F);
   public static GameDamage broadsideBarrageDamage = new GameDamage(115.0F);
   public static GameDamage laserDamage = new GameDamage(150.0F);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   protected float lastBombPlaceMoveUnits = 0.0F;
   protected float placeBombsEveryMoveUnits = 0.0F;
   protected final FloatMobAbility startPlaceBombs;
   protected SoundPlayer dashSoundPlayer;
   protected final EmptyMobAbility dashSoundAbility;

   public CrystalDragonHead() {
      super(100, waveLength, 100.0F, 7, 0.0F, -5.0F);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.moveAccuracy = 100;
      this.movementUpdateCooldown = 2000;
      this.movePosTolerance = 100.0F;
      this.setSpeed(250.0F);
      this.setArmor(35);
      this.accelerationMod = 1.0F;
      this.decelerationMod = 1.0F;
      this.collision = new Rectangle(-30, -25, 60, 50);
      this.hitBox = new Rectangle(-40, -35, 80, 70);
      this.selectBox = new Rectangle(-40, -60, 80, 80);
      this.startPlaceBombs = this.registerAbility(new FloatMobAbility() {
         @Override
         protected void run(float value) {
            CrystalDragonHead.this.lastBombPlaceMoveUnits = CrystalDragonHead.this.distanceMoved;
            CrystalDragonHead.this.placeBombsEveryMoveUnits = value;
         }
      });
      this.dashSoundAbility = this.registerAbility(
         new EmptyMobAbility() {
            @Override
            protected void run() {
               if (CrystalDragonHead.this.isClient() && (CrystalDragonHead.this.dashSoundPlayer == null || CrystalDragonHead.this.dashSoundPlayer.isDone())) {
                  CrystalDragonHead.this.dashSoundPlayer = SoundManager.playSound(
                     new SoundSettings(GameResources.swoosh).volume(0.3F).basePitch(1.4F).pitchVariance(0.04F).fallOffDistance(1000), CrystalDragonHead.this
                  );
               }
            }
         }
      );
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
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
         SoundManager.playSound(GameResources.roar, SoundEffect.effect(this).pitch(1.1F).volume(0.8F).falloffDistance(4000));
      }
   }

   protected float getDistToBodyPart(CrystalDragonBody bodyPart, int index, float lastDistance) {
      return index >= 1 ? lengthPerBodyPart + 10.0F : lengthPerBodyPart;
   }

   protected CrystalDragonBody createNewBodyPart(int index) {
      CrystalDragonBody bodyPart = new CrystalDragonBody();
      bodyPart.spriteY = index + 1;
      bodyPart.sharesHitCooldownWithNext = true;
      bodyPart.relaysBuffsToNext = true;
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
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return headCollisionDamage;
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
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CrystalDragonHead.CrystalDragonHeadAI<>(), new FlyingAIMover());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.crystaldragonbegin, SoundEffect.effect(this).volume(0.8F).falloffDistance(4000));
      }
   }

   @Override
   public float getWaveHeight(float length) {
      return super.getWaveHeight(length);
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (!this.isClient() && this.placeBombsEveryMoveUnits > 0.0F) {
         while (true) {
            float nextBombPlace = this.lastBombPlaceMoveUnits + this.placeBombsEveryMoveUnits;
            if (!(this.distanceMoved >= nextBombPlace)) {
               break;
            }

            this.lastBombPlaceMoveUnits = this.lastBombPlaceMoveUnits + this.placeBombsEveryMoveUnits;
            CrystalBombEvent event = new CrystalBombEvent(this, this.getX(), this.getY(), shardDamage, GameRandom.globalRandom);
            this.getLevel().entityManager.events.add(event);
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.windSound == null || this.windSound.isDone()) {
         this.windSound = SoundManager.playSound(GameResources.wind1, SoundEffect.effect(this).falloffDistance(1200).volume(0.7F));
      }

      if (this.windSound != null) {
         this.windSound.refreshLooping(1.0F);
      }

      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.DragonsHoard, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      float healthPerc = (float)this.getHealth() / this.getMaxHealth();
      float mod = Math.abs((float)Math.pow(healthPerc, 0.5) - 1.0F);
      this.setSpeed(120.0F + mod * 90.0F);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
      float healthPerc = (float)this.getHealth() / this.getMaxHealth();
      float mod = Math.abs((float)Math.pow(healthPerc, 0.5) - 1.0F);
      this.setSpeed(120.0F + mod * 90.0F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      this.getLevel()
         .entityManager
         .addParticle(
            new FleshParticle(
               this.getLevel(), MobRegistry.Textures.crystalDragon, 4, GameRandom.globalRandom.nextInt(6), 64, this.x, this.y, 20.0F, knockbackX, knockbackY
            ),
            Particle.GType.IMPORTANT_COSMETIC
         );
   }

   @Override
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.crystaldragonhurt, SoundEffect.effect(this).pitch(pitch).volume(0.3F).falloffDistance(1500));
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.crystaldragondeath, SoundEffect.effect(this).falloffDistance(3000));
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
         int drawX = camera.getDrawX(this.x) - 112;
         int drawY = camera.getDrawY(this.y);
         float headAngle = GameMath.fixAngle(GameMath.getAngle(new Float(this.dx, this.dy)));
         final MobDrawable headDrawable = WormMobHead.getAngledDrawable(
            this,
            new GameSprite(MobRegistry.Textures.crystalDragon, 0, 0, 224),
            null,
            light.minLevelCopy(100.0F),
            (int)this.height,
            headAngle,
            drawX,
            drawY,
            130,
            perspective
         );
         MobDrawable headDrawableShadow = WormMobHead.getAngledDrawable(
            this,
            new GameSprite(MobRegistry.Textures.crystalDragon_shadow, 0, 0, 224),
            null,
            light,
            (int)this.height,
            headAngle,
            drawX,
            drawY + 40,
            130,
            perspective
         );
         new ComputedObjectValue<>(null, () -> 0.0);
         ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> shoulderLine = WormMobHead.moveDistance(this.moveLines.getFirstElement(), 70.0);
         final MobDrawable shoulderDrawable;
         MobDrawable shoulderDrawableShadow;
         if (shoulderLine.object != null) {
            java.awt.geom.Point2D.Double shoulderPos = WormMobHead.linePos(shoulderLine);
            GameLight shoulderLight = level.getLightLevel(getTileCoordinate(shoulderPos.x), getTileCoordinate(shoulderPos.y));
            int shoulderDrawX = camera.getDrawX((float)shoulderPos.x) - 112;
            int shoulderDrawY = camera.getDrawY((float)shoulderPos.y);
            float shoulderHeight = this.getWaveHeight(
               ((WormMoveLine)((GameLinkedList.Element)shoulderLine.object).object).movedDist + ((Double)shoulderLine.get()).floatValue()
            );
            float shoulderAngle = GameMath.fixAngle(
               (float)GameMath.getAngle(new java.awt.geom.Point2D.Double(this.x - shoulderPos.x, this.y - this.height - (shoulderPos.y - shoulderHeight)))
            );
            shoulderDrawable = WormMobHead.getAngledDrawable(
               this,
               new GameSprite(MobRegistry.Textures.crystalDragon, 0, 1, 224),
               null,
               shoulderLight.minLevelCopy(100.0F),
               (int)shoulderHeight,
               shoulderAngle,
               shoulderDrawX,
               shoulderDrawY,
               130,
               perspective
            );
            shoulderDrawableShadow = WormMobHead.getAngledDrawable(
               this,
               new GameSprite(MobRegistry.Textures.crystalDragon_shadow, 0, 1, 224),
               null,
               shoulderLight,
               (int)shoulderHeight,
               shoulderAngle,
               shoulderDrawX,
               shoulderDrawY + 40,
               130,
               perspective
            );
         } else {
            shoulderDrawable = null;
            shoulderDrawableShadow = null;
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
         tileList.add(tickManager1 -> {
            if (shoulderDrawableShadow != null) {
               shoulderDrawableShadow.draw(tickManager);
            }

            headDrawableShadow.draw(tickManager);
         });
      }
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
      MobRegistry.Textures.crystalDragonHead.initDraw().sprite(0, 0, 136).rotate(headAngle - 90.0F, 24, 24).size(48, 48).draw(drawX, drawY);
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
         .forEach(c -> {
            LocalMessage message = new LocalMessage("misc", "bossdefeat", "name", this.getLocalization());
            c.sendPacket(new PacketChatMessage(message));
         });
      if (!this.isDamagedByPlayers) {
         AchievementManager.checkMeAndThisArmyKill(this.getLevel(), attackers);
      }
   }

   @Override
   public void dispose() {
      if (this.dashSoundPlayer != null) {
         this.dashSoundPlayer.stop();
      }

      super.dispose();
   }

   public static class BroadsideBarrageStage<T extends CrystalDragonHead> extends AINode<T> implements AttackStageInterface<T> {
      public float angleOffset;
      public float oppositeAngleDistance;
      public float shootWhenPastTargetDistance;
      public Mob target;
      public float flightAngle;
      public float distanceMovedWhenPerpendicular;
      public boolean hasShot;

      public BroadsideBarrageStage(float angleOffset, float oppositeAngleDistance, float shootWhenPastTargetDistance) {
         this.angleOffset = angleOffset;
         this.oppositeAngleDistance = oppositeAngleDistance;
         this.shootWhenPastTargetDistance = shootWhenPastTargetDistance;
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.hasShot = false;
         this.target = blackboard.getObject(Mob.class, "currentTarget");
         if (this.target != null) {
            float angle = GameMath.getAngle(GameMath.normalize(this.target.x - mob.x, this.target.y - mob.y));
            float targetAngle = angle + GameRandom.globalRandom.getOneOf(-this.angleOffset, this.angleOffset);
            Float finalDir = GameMath.getAngleDir(targetAngle);
            float targetX = this.target.x + finalDir.x * this.oppositeAngleDistance;
            float targetY = this.target.y + finalDir.y * this.oppositeAngleDistance;
            blackboard.mover.directMoveTo(this, (int)targetX, (int)targetY);
            this.flightAngle = GameMath.getAngle(GameMath.normalize(targetX - mob.x, targetY - mob.y));
            this.distanceMovedWhenPerpendicular = 0.0F;
         }
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (this.target != null && blackboard.mover.isMoving()) {
            if (!this.hasShot) {
               if (this.distanceMovedWhenPerpendicular == 0.0F) {
                  float angle = GameMath.getAngle(GameMath.normalize(this.target.x - mob.x, this.target.y - mob.y));
                  float delta = GameMath.getAngleDifference(this.flightAngle, angle);
                  if (delta > 90.0F || delta < -90.0F) {
                     this.distanceMovedWhenPerpendicular = mob.distanceMoved;
                  }
               } else {
                  float distanceMoved = mob.distanceMoved - this.distanceMovedWhenPerpendicular;
                  if (distanceMoved >= this.shootWhenPastTargetDistance) {
                     for (LevelMob<CrystalDragonBody> lBodyPart : mob.bodyParts) {
                        CrystalDragonBody bodyPart = lBodyPart.get(mob.getLevel());
                        if (bodyPart != null && bodyPart.isVisible()) {
                           Mob next = bodyPart.next;
                           if (next == null) {
                              next = mob;
                           }

                           Float dir = new Float(next.x - bodyPart.x, next.y - bodyPart.y);
                           Float perpDir = GameMath.getPerpendicularDir(dir.x, dir.y);
                           mob.getLevel()
                              .entityManager
                              .projectiles
                              .add(
                                 new CrystalDragonShardProjectile(
                                    bodyPart.x,
                                    bodyPart.y,
                                    bodyPart.x + perpDir.x * 100.0F,
                                    bodyPart.y + perpDir.y * 100.0F,
                                    100.0F,
                                    500,
                                    CrystalDragonHead.broadsideBarrageDamage,
                                    50,
                                    mob
                                 )
                              );
                           mob.getLevel()
                              .entityManager
                              .projectiles
                              .add(
                                 new CrystalDragonShardProjectile(
                                    bodyPart.x,
                                    bodyPart.y,
                                    bodyPart.x - perpDir.x * 100.0F,
                                    bodyPart.y - perpDir.y * 100.0F,
                                    100.0F,
                                    500,
                                    CrystalDragonHead.broadsideBarrageDamage,
                                    50,
                                    mob
                                 )
                              );
                        }
                     }

                     this.hasShot = true;
                  }
               }
            }

            return AINodeResult.RUNNING;
         } else {
            return AINodeResult.SUCCESS;
         }
      }
   }

   public static class ChargeTargetStage<T extends CrystalDragonHead> extends AINode<T> implements AttackStageInterface<T> {
      public int startMoveAccuracy;
      public Mob chargingTarget;
      public float bombPlacePerUnits;

      public ChargeTargetStage(float bombPlacePerUnits) {
         this.bombPlacePerUnits = bombPlacePerUnits;
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.startMoveAccuracy = mob.moveAccuracy;
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target != null) {
            mob.moveAccuracy = 5;
            this.chargingTarget = target;
            this.getBlackboard().mover.setCustomMovement(this, new MobMovementRelative(target, 0.0F, 0.0F));
         }

         mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SPIDER_CHARGE, mob, 30.0F, null), true);
         mob.startPlaceBombs.runAndSend(this.bombPlacePerUnits);
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
         mob.moveAccuracy = this.startMoveAccuracy;
         mob.buffManager.removeBuff(BuffRegistry.SPIDER_CHARGE, true);
         this.chargingTarget = null;
         mob.startPlaceBombs.runAndSend(0.0F);
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         if (!mob.buffManager.hasBuff(BuffRegistry.SPIDER_CHARGE)) {
            return AINodeResult.SUCCESS;
         }

         if (this.chargingTarget != null && !this.chargingTarget.removed()) {
            float distance = mob.getDistance(this.chargingTarget);
            if (distance > 300.0F && distance < 400.0F) {
               mob.dashSoundAbility.runAndSend();
            }

            if (distance < 150.0F) {
               float currentAngle = GameMath.getAngle(new Float(mob.dx, mob.dy));
               float targetAngle = GameMath.getAngle(new Float(this.chargingTarget.x - mob.x, this.chargingTarget.y - mob.y));
               float diff = GameMath.getAngleDifference(currentAngle, targetAngle);
               float maxAngle = 30.0F;
               if (Math.abs(diff) >= maxAngle || mob.dx == 0.0F && mob.dy == 0.0F) {
                  mob.moveAccuracy = this.startMoveAccuracy;
                  return AINodeResult.SUCCESS;
               }
            }

            return AINodeResult.RUNNING;
         } else {
            return AINodeResult.SUCCESS;
         }
      }
   }

   public static class CirclingStage<T extends CrystalDragonHead> extends AINode<T> implements AttackStageInterface<T> {
      public long statTime;
      public int circlingRange;
      public int circlingTime;

      public CirclingStage(int circlingRange, int circlingTime) {
         this.circlingRange = circlingRange;
         this.circlingTime = circlingTime;
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.statTime = mob.getTime();
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         float circlingSpeed = MobMovementCircle.convertToRotSpeed(this.circlingRange, this.mob().getSpeed()) * 1.1F;
         MobMovementCircle movement;
         if (target != null) {
            movement = new MobMovementCircleRelative(this.mob(), target, this.circlingRange, circlingSpeed, GameRandom.globalRandom.nextBoolean());
         } else {
            movement = new MobMovementCircleLevelPos(
               this.mob(), this.mob().x, this.mob().y, this.circlingRange, circlingSpeed, GameRandom.globalRandom.nextBoolean()
            );
         }

         this.getBlackboard().mover.setCustomMovement(this, movement);
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         long endTime = this.statTime + this.circlingTime;
         return mob.getTime() < endTime ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }
   }

   public static class CrystalDragonHeadAI<T extends CrystalDragonHead> extends SequenceAINode<T> {
      public CrystalDragonHeadAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         this.addChild(new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         this.addChild(new IsolateRunningAINode<>(attackStages));
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(900, 4000));
         attackStages.addChild(new CrystalDragonHead.BroadsideBarrageStage<>(35.0F, 400.0F, 150.0F));
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(600, 4000));
         attackStages.addChild(new CrystalDragonHead.SpawnCrystalGolemsStage<>());
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(600, 5000));
         attackStages.addChild(new CrystalDragonHead.ChargeTargetStage<>(100.0F));
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(900, 4000));
         attackStages.addChild(new CrystalDragonHead.BroadsideBarrageStage<>(35.0F, 400.0F, 150.0F));
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(600, 4000));
         attackStages.addChild(new CrystalDragonHead.SpawnCrystalGolemsStage<>());
         attackStages.addChild(new CrystalDragonHead.LaserSpiralStage<>(200, 100, 300, 5000, 1000, 5000));
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(900, 4000));
         attackStages.addChild(new CrystalDragonHead.BroadsideBarrageStage<>(35.0F, 400.0F, 150.0F));
         attackStages.addChild(new CrystalDragonHead.CirclingStage<>(600, 5000));
         attackStages.addChild(new CrystalDragonHead.ChargeTargetStage<>(100.0F));
      }
   }

   public static class LaserSpiralStage<T extends CrystalDragonHead> extends AINode<T> implements AttackStageInterface<T> {
      public int circlingRange;
      public int minRandomTargetDistanceOffset;
      public int maxRandomTargetDistanceOffset;
      public int indicatorWaitTime;
      public int laserRange;
      public int laserAliveTime;
      public long startTime;
      public CrystalDragonLaserLevelEvent event;
      public int startMoveAccuracy;

      public LaserSpiralStage(
         int circlingRange, int minRandomTargetDistanceOffset, int maxRandomTargetDistanceOffset, int indicatorWaitTime, int laserRange, int laserAliveTime
      ) {
         this.circlingRange = circlingRange;
         this.minRandomTargetDistanceOffset = minRandomTargetDistanceOffset;
         this.maxRandomTargetDistanceOffset = maxRandomTargetDistanceOffset;
         this.indicatorWaitTime = indicatorWaitTime;
         this.laserRange = laserRange;
         this.laserAliveTime = laserAliveTime;
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         if (this.event != null) {
            this.event.over();
         }

         this.event = null;
         this.startMoveAccuracy = mob.moveAccuracy;
         mob.moveAccuracy = 5;
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         Point basePos;
         if (target != null) {
            basePos = new Point(target.getX(), target.getY());
         } else {
            basePos = new Point(mob.getX(), mob.getY());
         }

         int randomAngle = GameRandom.globalRandom.nextInt(360);
         int randomDistance = GameRandom.globalRandom.getIntBetween(this.minRandomTargetDistanceOffset, this.maxRandomTargetDistanceOffset);
         Float dir = GameMath.getAngleDir(randomAngle);
         Float circleCenter = new Float(basePos.x + dir.x * randomDistance, basePos.y + dir.y * randomDistance);
         float circlingSpeed = MobMovementCircle.convertToRotSpeed(this.circlingRange, this.mob().getSpeed()) * 1.1F;
         this.getBlackboard()
            .mover
            .setCustomMovement(
               this,
               new MobMovementCircleLevelPos(
                  this.mob(), circleCenter.x, circleCenter.y, this.circlingRange, circlingSpeed, GameRandom.globalRandom.nextBoolean()
               )
            );
         this.startTime = mob.getTime();
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
         mob.moveAccuracy = this.startMoveAccuracy;
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         long laserStartTime = this.startTime + this.indicatorWaitTime;
         if (mob.getTime() >= laserStartTime) {
            if (this.event == null) {
               mob.getLevel()
                  .entityManager
                  .events
                  .add(
                     this.event = new CrystalDragonLaserLevelEvent(
                        mob, new GameRandom(), this.laserRange, CrystalDragonHead.laserDamage, 100, this.laserAliveTime
                     )
                  );
            } else if (this.event.isOver()) {
               this.event = null;
               return AINodeResult.SUCCESS;
            }
         }

         return AINodeResult.RUNNING;
      }
   }

   public static class SpawnCrystalGolemsStage<T extends CrystalDragonHead> extends AINode<T> implements AttackStageInterface<T> {
      static GameTileRange range = new GameTileRange(10);

      public void onStarted(T mob, Blackboard<T> blackboard) {
         ArrayList<Point> validSpawnTiles = new ArrayList<>();

         for (Point tile : range.getValidTiles(mob.getTileX(), mob.getTileY())) {
            if (!mob.getLevel().isSolidTile(tile.x, tile.y)) {
               validSpawnTiles.add(tile);
            }
         }

         if (!validSpawnTiles.isEmpty()) {
            for (int i = 0; i < 3; i++) {
               Point spawnTile = GameRandom.globalRandom.getOneOf(validSpawnTiles);
               int targetX = spawnTile.x * 32 + 16;
               int targetY = spawnTile.y * 32 + 16;
               int distance = (int)mob.getDistance(targetX, targetY);
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(new CrystalGolemSpawnProjectile(mob.getLevel(), mob, targetX, targetY, 30.0F, distance, new GameDamage(0.0F), 50));
            }
         }
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         return AINodeResult.SUCCESS;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }
}
