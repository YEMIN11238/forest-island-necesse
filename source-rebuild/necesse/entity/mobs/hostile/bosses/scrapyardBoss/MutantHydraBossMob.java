package necesse.entity.mobs.hostile.bosses.scrapyardBoss;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
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
import necesse.entity.levelEvent.FlameTrapEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.SimpleChainChildMob;
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
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.BossMob;
import necesse.entity.mobs.hostile.bosses.FlyingBossMob;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.AttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.IdleTimeAttackStage;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.RotationAttackStageNode;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.SimultaneousAttackStageManagerNode;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.FullDoubleHeadsClapStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.FullDoubleHeadsSweepRotateStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.FullHeadsClapStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.FullHeadsSweepRotateStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.HeadShakeAndGooSprayStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.HeadSpitStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.HeadsWhipStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.SetHeadsIdleStage;
import necesse.entity.mobs.hostile.bosses.scrapyardBoss.attackStages.SeveralHeadScreamsStage;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.TrapObjectEntity;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
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
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.ScrapyardIncursionLevel;
import necesse.level.maps.light.GameLight;

public class MutantHydraBossMob extends FlyingBossMob {
   public static RotationLootItem vinylRotation = RotationLootItem.globalLootRotation(
      new LootItemList(),
      new LootItemList(),
      new LootItemList(),
      RotationLootItem.globalLootRotation(4, new LootItem("ascrewloosevinyl"), new LootItem("themutantfactoryvinyl"))
   );
   public static LootTable lootTable = new LootTable(
      new LootItemMultiplierIgnored(vinylRotation), new ChanceLootItem(0.1F, "bumpercar").preventLootMultiplier()
   );
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(50000, 68000, 80000, 92000, 115000);
   public static GameDamage neckContactDamage = new GameDamage(115.0F);
   public static GameDamage headContactDamage = new GameDamage(125.0F);
   public static GameDamage gooSprayDamage = new GameDamage(110.0F);
   public static GameDamage gooSpitDamage = new GameDamage(110.0F);
   public static float spitLingerSeconds = 30.0F;
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   public static MutantHydraBossMob.NeckCountAndDistance[] neckCounts = new MutantHydraBossMob.NeckCountAndDistance[]{
      new MutantHydraBossMob.NeckCountAndDistance(12, 13.0F, new Rectangle(-10, -10, 20, 20), new Rectangle(-10, -10, 20, 20), new Rectangle(-15, -15, 30, 30)),
      new MutantHydraBossMob.NeckCountAndDistance(10, 15.0F, new Rectangle(-13, -13, 26, 26), new Rectangle(-13, -13, 26, 26), new Rectangle(-20, -20, 40, 40)),
      new MutantHydraBossMob.NeckCountAndDistance(8, 17.0F, new Rectangle(-17, -17, 34, 34), new Rectangle(-17, -17, 34, 34), new Rectangle(-25, -27, 50, 50)),
      new MutantHydraBossMob.NeckCountAndDistance(8, 19.0F, new Rectangle(-20, -20, 40, 40), new Rectangle(-20, -20, 40, 40), new Rectangle(-30, -36, 60, 60))
   };
   protected final int totalNeckParts;
   protected final float totalNeckDistance;
   protected final ArrayList<MutantHydraBossMob.NeckIndexAndDistance> neckParts;
   public final MutantHydraBossMob.HeadData topLeftHead;
   public final MutantHydraBossMob.HeadData topRightHead;
   public final MutantHydraBossMob.HeadData bottomLeftHead;
   public final MutantHydraBossMob.HeadData bottomRightHead;
   public final ArrayList<MutantHydraBossMob.HeadData> heads;

   public MutantHydraBossMob() {
      super(100);
      int totalNeckParts = 0;
      float totalNeckDistance = 0.0F;
      this.neckParts = new ArrayList<>();

      for (int i = 0; i < neckCounts.length; i++) {
         MutantHydraBossMob.NeckCountAndDistance current = neckCounts[i];

         for (int j = 0; j < current.count; j++) {
            this.neckParts
               .add(
                  new MutantHydraBossMob.NeckIndexAndDistance(
                     neckCounts.length - 1 - i, current.distancePerNeckPart - (j == 0 ? current.distancePerNeckPart / 2.0F : 0.0F), current
                  )
               );
            totalNeckParts++;
            totalNeckDistance += current.distancePerNeckPart;
         }
      }

      this.totalNeckParts = totalNeckParts;
      this.totalNeckDistance = totalNeckDistance;
      this.topLeftHead = new MutantHydraBossMob.HeadData(-48, -86, -50, -80, -50, 0, 15, true);
      this.topRightHead = new MutantHydraBossMob.HeadData(48, -86, 50, -80, -50, 1, 0, true);
      this.bottomLeftHead = new MutantHydraBossMob.HeadData(-48, -24, -60, -20, 0, 2, 0, false);
      this.bottomRightHead = new MutantHydraBossMob.HeadData(48, -24, 60, -20, 0, 3, 5, false);
      this.heads = new ArrayList<>(Arrays.asList(this.topLeftHead, this.topRightHead, this.bottomLeftHead, this.bottomRightHead));
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.setSpeed(0.0F);
      this.setArmor(40);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-45, -30, 90, 40);
      this.hitBox = new Rectangle(-45, -90, 90, 100);
      this.selectBox = new Rectangle(-50, -120, 100, 140);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.neckPartsManager.setupSpawnPacket(writer);
         MutantHydraHeadMob headMob = head.getHeadMob();
         if (headMob != null) {
            writer.putNextBoolean(true);
            Packet spawnPacket = new Packet();
            headMob.setupSpawnPacket(new PacketWriter(spawnPacket));
            writer.putNextContentPacket(spawnPacket);
         } else {
            writer.putNextBoolean(false);
         }
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.neckPartsManager.applySpawnPacket(reader);
         if (reader.getNextBoolean()) {
            head.spawnPacket = reader.getNextContentPacket();
         }
      }
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.updateNeckPartPositions();
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
      this.ai = new BehaviourTreeAI<>(this, new MutantHydraBossMob.MutantHydraBossAI<>(), new FlyingAIMover());

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.init();
      }

      if (this.isClient()) {
         SoundManager.playSound(GameResources.mutanthydrabegin, SoundEffect.effect(this).volume(1.2F).falloffDistance(4000));
      }
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.updateNeckPartPositions();
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.TheMutantFactory, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.neckPartsManager.tickKeepAlive();
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.neckPartsManager.tickKeepAlive();
      }
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
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.9F, 1.0F, 1.1F);
      SoundManager.playSound(GameResources.mutanthydrahurt, SoundEffect.effect(this).volume(0.8F).pitch(pitch).falloffDistance(1500));
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.mutanthydradeath, SoundEffect.effect(this).volume(1.5F).falloffDistance(3000));
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return headContactDamage;
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
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.mutantHydraBody,
                  i,
                  3,
                  64,
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
      int drawX = camera.getDrawX(x) - 80;
      int drawY = camera.getDrawY(y) - 160;
      int spriteX = GameUtils.getAnim(this.getTime(), 4, 1000);
      final DrawOptions drawOptions = MobRegistry.Textures.mutantHydraBody
         .initDraw()
         .sprite(spriteX, 0, 160, 192)
         .startGlowOptions(this, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 32;
      int drawY = y - 32;
      MobRegistry.Textures.mutantHydraMap.initDraw().draw(drawX, drawY);
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-32, -32, 64, 64);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.0F));
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return super.canBeHit(attacker) && !(attacker instanceof FlameTrapEvent);
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      super.remove(knockbackX, knockbackY, attacker, isDeath);

      for (MutantHydraBossMob.HeadData head : this.heads) {
         head.neckPartsManager.removeChildren(knockbackX, knockbackY, attacker, isDeath);
      }
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

   public class HeadData {
      public final SimpleChainChildMob.ChildrenManager<MutantHydraBossMob, MutantHydraNeckMob> neckPartsManager = new SimpleChainChildMob.ChildrenManager<>(
         MutantHydraBossMob.this
      );
      public final int drawOrder;
      public final int headSpriteY;
      public final int headSpriteXOffset;
      public final boolean reverseSway;
      public int startXOffset;
      public int startYOffset;
      public int idleXOffset;
      public int idleYOffset;
      public Packet spawnPacket;

      public HeadData(
         int startXOffset, int startYOffset, int idleXOffset, int idleYOffset, int drawOrder, int headSpriteY, int headSpriteXOffset, boolean reverseSway
      ) {
         this.startXOffset = startXOffset;
         this.startYOffset = startYOffset;
         this.idleXOffset = startXOffset + idleXOffset;
         this.idleYOffset = startYOffset + idleYOffset;
         this.drawOrder = drawOrder;
         this.headSpriteY = headSpriteY;
         this.headSpriteXOffset = headSpriteXOffset;
         this.reverseSway = reverseSway;
      }

      public void init() {
         AtomicReference<LevelMob<MutantHydraHeadMob>> headMobRef = new AtomicReference<>();
         this.neckPartsManager.initChildren(MutantHydraBossMob.this.totalNeckParts, index -> {
            MutantHydraBossMob.NeckIndexAndDistance neckIndexAndDistance = MutantHydraBossMob.this.neckParts.get(index);
            if (index == 0) {
               MutantHydraHeadMob headMob = new MutantHydraHeadMob();
               headMob.index = index;
               headMob.apply(neckIndexAndDistance);
               headMob.drawOrder = MutantHydraBossMob.this.totalNeckParts + this.drawOrder - index;
               headMob.sharesHitCooldownWithNext = true;
               headMob.headSpriteY = this.headSpriteY;
               headMob.headSpriteXOffset = this.headSpriteXOffset;
               headMob.idleXOffset = this.idleXOffset;
               headMob.idleYOffset = this.idleYOffset;
               return headMob;
            } else {
               MutantHydraNeckMob neckMob = new MutantHydraNeckMob();
               neckMob.index = index;
               neckMob.apply(neckIndexAndDistance);
               neckMob.drawOrder = MutantHydraBossMob.this.totalNeckParts + this.drawOrder - index;
               neckMob.sharesHitCooldownWithNext = index != MutantHydraBossMob.this.totalNeckParts - 1;
               neckMob.relaysBuffsToNext = true;
               return neckMob;
            }
         }, (index, part) -> {
            if (index == 0) {
               if (this.spawnPacket != null) {
                  part.applySpawnPacket(new PacketReader(this.spawnPacket));
               } else {
                  part.setPos(MutantHydraBossMob.this.x + this.idleXOffset, MutantHydraBossMob.this.y + this.idleYOffset, true);
               }

               headMobRef.set(new LevelMob<>((MutantHydraHeadMob)part));
               this.spawnPacket = null;
            }

            part.headMob = headMobRef.get();
         });
         this.updateNeckPartPositions();
      }

      public MutantHydraHeadMob getHeadMob() {
         return this.neckPartsManager.isEmpty() ? null : (MutantHydraHeadMob)this.neckPartsManager.getChild(0);
      }

      public void runHeadMobIfPresent(Consumer<MutantHydraHeadMob> consumer) {
         MutantHydraHeadMob headMob = this.getHeadMob();
         if (headMob != null) {
            consumer.accept(headMob);
         }
      }

      public <R> R getHeadVar(Function<MutantHydraHeadMob, R> getter, R defaultValue) {
         MutantHydraHeadMob headMob = this.getHeadMob();
         return headMob == null ? defaultValue : getter.apply(headMob);
      }

      public boolean hasHeadArrivedAtTarget() {
         return this.<Boolean>getHeadVar(Mob::hasArrivedAtTarget, false);
      }

      public void updateNeckPartPositions() {
         if (!this.neckPartsManager.isEmpty()) {
            MutantHydraHeadMob head = this.getHeadMob();
            if (head != null) {
               Float shakeOffset = head.getShakeOffset();
               float headDeltaX = head.x + shakeOffset.x - (MutantHydraBossMob.this.x + this.startXOffset);
               float headDeltaY = head.y - head.currentHeight + shakeOffset.y - (MutantHydraBossMob.this.y + this.startYOffset);
               float headDistance = (float)Math.sqrt(headDeltaX * headDeltaX + headDeltaY * headDeltaY);
               float headDirX = headDeltaX / headDistance;
               float headDirY = headDeltaY / headDistance;
               float maxDistance = MutantHydraBossMob.this.totalNeckDistance;
               if (headDistance > maxDistance) {
                  headDistance = maxDistance;
                  head.setPos(
                     MutantHydraBossMob.this.x + this.startXOffset + headDirX * maxDistance,
                     MutantHydraBossMob.this.y + this.startYOffset + headDirY * maxDistance,
                     false
                  );
                  if (!MutantHydraBossMob.this.isClient()) {
                     head.returnToIdle();
                  }

                  headDeltaX = head.x - (MutantHydraBossMob.this.x + this.startXOffset);
                  headDeltaY = head.y - (MutantHydraBossMob.this.y + this.startYOffset);
               }

               float xSwayPercent = Math.min(Math.abs(headDeltaY) / 140.0F, 1.0F);
               if (this.idleXOffset < 0) {
                  xSwayPercent = -xSwayPercent;
               }

               float ySwayPercent = Math.min(Math.abs(headDeltaX) / 200.0F, 1.0F);
               float currentDistance = 0.0F;

               for (int i = 1; i < this.neckPartsManager.getChildrenSize(); i++) {
                  MutantHydraNeckMob child = this.neckPartsManager.getChild(i);
                  if (child != null) {
                     boolean wasLastDistanceAtBody = currentDistance == headDistance;
                     currentDistance += MutantHydraBossMob.this.neckParts.get(i).distance;
                     if (currentDistance > headDistance) {
                        currentDistance = headDistance;
                     }

                     child.canHit = !wasLastDistanceAtBody;
                     float progressTowardsHead = 1.0F - currentDistance / headDistance;
                     float xSway = (float)Math.sin(progressTowardsHead * Math.PI * 2.0) * 15.0F * xSwayPercent;
                     float ySway = (float)Math.sin(progressTowardsHead * Math.PI * 2.0) * 15.0F * ySwayPercent;
                     if (this.reverseSway) {
                        xSway = -xSway;
                        ySway = -ySway;
                     }

                     child.setPos(
                        MutantHydraBossMob.this.x + this.startXOffset + headDeltaX - headDirX * currentDistance + xSway,
                        MutantHydraBossMob.this.y + this.startYOffset + headDeltaY - headDirY * currentDistance + ySway,
                        true
                     );
                  }
               }
            }
         }
      }
   }

   public static class MutantHydraBossAI<T extends MutantHydraBossMob> extends SequenceAINode<T> {
      public MutantHydraBossAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         TargetFinderAINode<T> targetFinder;
         this.addChild(targetFinder = new TargetFinderAINode<T>(0) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         targetFinder.distance = new TargetFinderDistance<T>(704) {
            public float getSearchDistanceMod(T mob, Mob target) {
               return 1.0F;
            }
         };
         this.addChild(new MutantHydraBossMob.TriggerScrapyardTrapsAINode<>());
         int minIdleWaitTime = 500;
         int maxIdleWaitTime = 1500;
         int minAttackTimeTaken = 4000;
         int maxAttackTimeTaken = 6000;
         int minShakeTime = 500;
         int maxShakeTime = 2000;
         int minSweepWaitTime = 200;
         int maxSweepWaitTime = 750;
         Function<T, Integer> cooldownGetter = m -> GameMath.lerp(m.getHealthPercent(), 200, 500);
         Function<T, Integer> shakeTimeGetter = m -> GameMath.lerp(m.getHealthPercent(), minShakeTime, maxShakeTime);
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         this.addChild(new IsolateRunningAINode<>(attackStages));
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         SimultaneousAttackStageManagerNode<T> subStage = new SimultaneousAttackStageManagerNode<>();
         subStage.addChild(
            new FullHeadsClapStage<>(minSweepWaitTime, maxSweepWaitTime, false, true, MutantHydraHeadType.BOTTOM_LEFT, MutantHydraHeadType.BOTTOM_RIGHT)
         );
         subStage.addChild(
            new RotationAttackStageNode<>(
               new HeadSpitStage(MutantHydraHeadType.TOP_RIGHT, 12, 10, minAttackTimeTaken, maxAttackTimeTaken),
               new HeadShakeAndGooSprayStage<>(
                  MutantHydraHeadType.TOP_RIGHT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
                  minShakeTime,
                  maxShakeTime
               )
            )
         );
         attackStages.addChild(subStage);
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         attackStages.addChild(new FullDoubleHeadsSweepRotateStage<>(minSweepWaitTime, maxSweepWaitTime, false, true, MutantHydraHeadType.values()));
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         subStage = new SimultaneousAttackStageManagerNode<>();
         subStage.addChild(
            new FullHeadsSweepRotateStage<>(minSweepWaitTime, maxSweepWaitTime, false, true, MutantHydraHeadType.TOP_LEFT, MutantHydraHeadType.BOTTOM_RIGHT)
         );
         subStage.addChild(
            new RotationAttackStageNode<>(
               new SeveralHeadScreamsStage(MutantHydraHeadType.TOP_RIGHT, 3, 1000, 1000),
               new HeadSpitStage(MutantHydraHeadType.TOP_RIGHT, 12, 10, minAttackTimeTaken, maxAttackTimeTaken),
               new HeadShakeAndGooSprayStage<>(
                  MutantHydraHeadType.TOP_RIGHT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
                  minShakeTime,
                  maxShakeTime
               )
            )
         );
         subStage.addChild(
            new RotationAttackStageNode<>(
               new HeadShakeAndGooSprayStage<>(
                  MutantHydraHeadType.BOTTOM_LEFT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
                  minShakeTime,
                  maxShakeTime
               ),
               new HeadSpitStage(MutantHydraHeadType.BOTTOM_LEFT, 12, 10, minAttackTimeTaken, maxAttackTimeTaken)
            )
         );
         attackStages.addChild(subStage);
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         subStage = new SimultaneousAttackStageManagerNode<>();

         for (MutantHydraHeadType head : MutantHydraHeadType.values()) {
            subStage.addChild(
               new HeadsWhipStage<>(
                  targetFinder, head, m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken, maxAttackTimeTaken), shakeTimeGetter, cooldownGetter, 600
               )
            );
         }

         attackStages.addChild(subStage);
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         subStage = new SimultaneousAttackStageManagerNode<>();
         subStage.addChild(
            new FullHeadsClapStage<>(minSweepWaitTime, maxSweepWaitTime, false, true, MutantHydraHeadType.TOP_LEFT, MutantHydraHeadType.BOTTOM_LEFT)
         );
         subStage.addChild(
            new RotationAttackStageNode<>(
               new HeadShakeAndGooSprayStage<>(
                  MutantHydraHeadType.TOP_RIGHT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
                  minShakeTime,
                  maxShakeTime
               ),
               new HeadSpitStage(MutantHydraHeadType.TOP_RIGHT, 12, 10, minAttackTimeTaken, maxAttackTimeTaken),
               new HeadShakeAndGooSprayStage<>(
                  MutantHydraHeadType.TOP_RIGHT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
                  minShakeTime,
                  maxShakeTime
               )
            )
         );
         subStage.addChild(
            new HeadsWhipStage<>(
               targetFinder,
               MutantHydraHeadType.BOTTOM_RIGHT,
               m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken, maxAttackTimeTaken),
               shakeTimeGetter,
               cooldownGetter,
               600
            )
         );
         attackStages.addChild(subStage);
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         subStage = new SimultaneousAttackStageManagerNode<>();
         subStage.addChild(
            new FullHeadsSweepRotateStage<>(minSweepWaitTime, maxSweepWaitTime, true, true, MutantHydraHeadType.BOTTOM_LEFT, MutantHydraHeadType.BOTTOM_RIGHT)
         );
         subStage.addChild(
            new RotationAttackStageNode<>(
               new HeadShakeAndGooSprayStage<>(
                  MutantHydraHeadType.TOP_RIGHT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
                  minShakeTime,
                  maxShakeTime
               ),
               new HeadSpitStage(MutantHydraHeadType.TOP_RIGHT, 12, 10, minAttackTimeTaken, maxAttackTimeTaken)
            )
         );
         subStage.addChild(
            new HeadsWhipStage<>(
               targetFinder,
               MutantHydraHeadType.TOP_LEFT,
               m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken, maxAttackTimeTaken),
               shakeTimeGetter,
               cooldownGetter,
               600
            )
         );
         attackStages.addChild(subStage);
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         attackStages.addChild(new FullDoubleHeadsClapStage<>(minSweepWaitTime, maxSweepWaitTime, true, true, MutantHydraHeadType.values()));
         attackStages.addChild(new SetHeadsIdleStage<>());
         attackStages.addChild(new MutantHydraBossMob.WaitHeadsIdleTime<>(minIdleWaitTime, maxIdleWaitTime));
         subStage = new SimultaneousAttackStageManagerNode<>();
         subStage.addChild(
            new HeadShakeAndGooSprayStage<>(
               MutantHydraHeadType.TOP_RIGHT,
               m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
               minShakeTime,
               maxShakeTime
            )
         );
         subStage.addChild(
            new HeadShakeAndGooSprayStage<>(
               MutantHydraHeadType.BOTTOM_LEFT,
               m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken - minShakeTime, maxAttackTimeTaken - maxShakeTime),
               minShakeTime,
               maxShakeTime
            )
         );
         subStage.addChild(
            new RotationAttackStageNode<>(
               new FullHeadsSweepRotateStage(minSweepWaitTime, maxSweepWaitTime, true, true, MutantHydraHeadType.BOTTOM_RIGHT),
               new HeadsWhipStage<>(
                  targetFinder,
                  MutantHydraHeadType.BOTTOM_RIGHT,
                  m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken, maxAttackTimeTaken),
                  shakeTimeGetter,
                  cooldownGetter,
                  600
               )
            )
         );
         subStage.addChild(
            new HeadsWhipStage<>(
               targetFinder,
               MutantHydraHeadType.TOP_LEFT,
               m -> GameMath.lerp(m.getHealthPercent(), minAttackTimeTaken, maxAttackTimeTaken),
               shakeTimeGetter,
               cooldownGetter,
               600
            )
         );
         attackStages.addChild(subStage);
         attackStages.addChild(new SetHeadsIdleStage<>());
      }
   }

   public static class NeckCountAndDistance {
      public final int count;
      public final float distancePerNeckPart;
      public final Rectangle collision;
      public final Rectangle hitbox;
      public final Rectangle selectBox;

      public NeckCountAndDistance(int count, float distancePerNeckPart, Rectangle collision, Rectangle hitbox, Rectangle selectBox) {
         this.count = count;
         this.distancePerNeckPart = distancePerNeckPart;
         this.collision = collision;
         this.hitbox = hitbox;
         this.selectBox = selectBox;
      }
   }

   public static class NeckIndexAndDistance {
      public final int spriteIndex;
      public final float distance;
      public final MutantHydraBossMob.NeckCountAndDistance neckCountAndDistance;

      public NeckIndexAndDistance(int spriteIndex, float distance, MutantHydraBossMob.NeckCountAndDistance neckCountAndDistance) {
         this.spriteIndex = spriteIndex;
         this.distance = distance;
         this.neckCountAndDistance = neckCountAndDistance;
      }
   }

   public static class TriggerScrapyardTrapsAINode<T extends MutantHydraBossMob> extends AINode<T> {
      public float triggerFlameTrapBuffer;
      public int lastFlameTrapIndex = -1;
      public float triggerBarrelTrapBuffer;
      public int lastBarrelTrapIndex = -1;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Level level = mob.getLevel();
         if (level instanceof ScrapyardIncursionLevel) {
            ScrapyardIncursionLevel scrapyardLevel = (ScrapyardIncursionLevel)level;
            if (!scrapyardLevel.explosiveBarrelTrapTiles.isEmpty()) {
               float barrelsPerSecond = 0.2F;
               this.triggerBarrelTrapBuffer += barrelsPerSecond / 20.0F;

               while (this.triggerBarrelTrapBuffer >= 1.0F) {
                  this.triggerBarrelTrapBuffer--;
                  int nextIndex = (this.lastBarrelTrapIndex + 1) % scrapyardLevel.explosiveBarrelTrapTiles.size();
                  this.triggerTrap(scrapyardLevel, scrapyardLevel.explosiveBarrelTrapTiles.get(nextIndex));
                  this.lastBarrelTrapIndex = nextIndex;
               }
            }

            if (!scrapyardLevel.flameTrapTiles.isEmpty()) {
               float barrelsPerSecond = 0.4F;
               this.triggerFlameTrapBuffer += barrelsPerSecond / 20.0F;

               while (this.triggerFlameTrapBuffer >= 1.0F) {
                  this.triggerFlameTrapBuffer--;

                  for (int i = 0; i < 4; i++) {
                     int nextIndex = (this.lastFlameTrapIndex + 1) % scrapyardLevel.flameTrapTiles.size();
                     this.triggerTrap(scrapyardLevel, scrapyardLevel.flameTrapTiles.get(nextIndex));
                     this.lastFlameTrapIndex = nextIndex;
                  }
               }
            }
         }

         return AINodeResult.SUCCESS;
      }

      public void triggerTrap(Level level, Point tile) {
         ObjectEntity objectEntity = level.entityManager.getObjectEntity(tile.x, tile.y);
         if (objectEntity instanceof TrapObjectEntity) {
            ((TrapObjectEntity)objectEntity).triggerTrap(0, level.getObjectRotation(tile.x, tile.y));
         }
      }
   }

   public static class WaitHeadsIdleTime<T extends MutantHydraBossMob> extends IdleTimeAttackStage<T> {
      public WaitHeadsIdleTime(int minTime, int maxTime) {
         super(m -> GameMath.lerp(m.getHealthPercent(), minTime, maxTime));
      }
   }
}
