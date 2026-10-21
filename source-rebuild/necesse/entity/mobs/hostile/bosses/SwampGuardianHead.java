package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.HashSet;
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
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChargingCirclingChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RemoveOnNoTargetNode;
import necesse.entity.mobs.ai.behaviourTree.leaves.SpawnProjectilesOnHealthLossAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.SwampBoulderProjectile;
import necesse.entity.projectile.SwampRazorProjectile;
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
import necesse.inventory.lootTable.lootItem.ConditionLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SwampGuardianHead extends BossWormMobHead<SwampGuardianBody, SwampGuardianHead> {
   public static LootTable lootTable = new LootTable(new ChanceLootItem(0.2F, "rumbleoftheswampguardianvinyl"));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      new LootItem("razorbladeboomerang"), new LootItem("guardianshell"), new LootItem("dredgingstaff")
   );
   public static LootTable privateLootTable = new LootTable(
      new ConditionLootItem(
         "guardianheart",
         (r, o) -> {
            ServerClient client = LootTable.expectExtra(ServerClient.class, o, 1);
            return client != null
               && client.playerMob.healthUpgradeManager.canUpgrade("guardianheart")
               && client.playerMob.getInv().getAmount(ItemRegistry.getItem("guardianheart"), false, false, true, true, "have") == 0;
         }
      ),
      uniqueDrops
   );
   public static float lengthPerBodyPart = 25.0F;
   public static float waveLength = 500.0F;
   public static final int totalBodyParts = 70;
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   public static GameDamage headCollisionDamage = new GameDamage(58.0F);
   public static GameDamage bodyCollisionDamage = new GameDamage(42.0F);
   public static GameDamage razorDamage = new GameDamage(42.0F);
   public static GameDamage boulderExplosionDamage = new GameDamage(70.0F);
   public static int boulderExplosionRange = 80;
   public static int totalRazorProjectiles = 250;
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(6000, 12000, 15000, 18000, 24000);
   public final CoordinateMobAbility flickSound;
   public final CoordinateMobAbility swingSound;

   public SwampGuardianHead() {
      super(100, waveLength, 80.0F, 70, 36.0F, -8.0F);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.moveAccuracy = 160;
      this.setSpeed(150.0F);
      this.setArmor(15);
      this.accelerationMod = 1.0F;
      this.decelerationMod = 1.0F;
      this.collision = new Rectangle(-20, -15, 40, 30);
      this.hitBox = new Rectangle(-25, -20, 50, 40);
      this.selectBox = new Rectangle(-32, -60, 64, 64);
      this.flickSound = this.registerAbility(new CoordinateMobAbility() {
         @Override
         protected void run(int x, int y) {
            if (SwampGuardianHead.this.isClient()) {
               SoundManager.playSound(GameResources.magicbolt2, SoundEffect.effect(x, y).pitch(1.5F).volume(0.5F));
            }
         }
      });
      this.swingSound = this.registerAbility(new CoordinateMobAbility() {
         @Override
         protected void run(int x, int y) {
            if (SwampGuardianHead.this.isClient()) {
               SoundManager.playSound(GameResources.swing1, SoundEffect.effect(x, y).pitch(0.8F));
            }
         }
      });
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

   protected float getDistToBodyPart(SwampGuardianBody bodyPart, int index, float lastDistance) {
      return lengthPerBodyPart;
   }

   protected SwampGuardianBody createNewBodyPart(int index) {
      SwampGuardianBody bodyPart;
      if (index == 69) {
         bodyPart = new SwampGuardianTail();
      } else {
         bodyPart = new SwampGuardianBody();
      }

      bodyPart.sharesHitCooldownWithNext = index % 3 < 2;
      bodyPart.relaysBuffsToNext = index % 3 < 2;
      if (index != 0 && index != 68) {
         bodyPart.sprite = new Point(index % 4, 0);
         bodyPart.shadowSprite = 0;
      } else {
         bodyPart.sprite = new Point(4, 0);
         bodyPart.shadowSprite = 1;
      }

      return bodyPart;
   }

   @Override
   protected void playMoveSound() {
      SoundManager.playSound(GameResources.shake, SoundEffect.effect(this).falloffDistance(1000));
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new SwampGuardianHead.SwampGuardianAI<>(), new FlyingAIMover());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.swampguardianbegin, SoundEffect.effect(this).volume(1.3F).falloffDistance(4000));
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
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.swampguardianhurt, SoundEffect.effect(this).volume(0.3F).pitch(pitch).falloffDistance(1500));
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.swampguardiandeath, SoundEffect.effect(this).volume(1.3F).falloffDistance(3000));
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return headCollisionDamage;
   }

   @Override
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.RumbleOfTheSwampGuardian, SoundManager.MusicPriority.EVENT, 1.5F);
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
               this.getLevel(), MobRegistry.Textures.swampGuardian, GameRandom.globalRandom.nextInt(6), 6, 32, this.x, this.y, 20.0F, knockbackX, knockbackY
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
         int drawX = camera.getDrawX(x) - 48;
         int drawY = camera.getDrawY(y);
         float headAngle = GameMath.fixAngle(GameMath.getAngle(new Float(this.dx, this.dy)));
         WormMobHead.addAngledDrawable(
            list,
            this,
            new GameSprite(MobRegistry.Textures.swampGuardian, 0, 1, 96),
            MobRegistry.Textures.swampGuardian_mask,
            light,
            (int)this.height,
            headAngle,
            drawX,
            drawY,
            64,
            perspective
         );
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
      MobRegistry.Textures.swampGuardian.initDraw().sprite(2, 2, 96).rotate(headAngle + 90.0F, 24, 24).size(48, 48).draw(drawX, drawY);
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

   public static class DiveChargeRotationAI<T extends SwampGuardianHead> extends AINode<T> {
      private int ticker;
      private final ChargingCirclingChaserAINode<T> chaserAI;

      public DiveChargeRotationAI(ChargingCirclingChaserAINode<T> chaserAI) {
         this.chaserAI = chaserAI;
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         this.ticker = 100;
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target != null) {
            this.ticker--;
            if (this.ticker <= 0) {
               if (!mob.dive && !mob.isUnderground) {
                  mob.diveAbility.runAndSend();
                  this.chaserAI.startCircling(mob, blackboard, target, 100);
                  this.ticker = (int)(20.0F * GameRandom.globalRandom.getFloatBetween(2.0F, 3.0F));
               } else {
                  this.chaserAI.startCharge(mob, blackboard, target);
                  float currentAngle = GameMath.getAngle(new Float(mob.x - target.x, mob.y - target.y));
                  Float dir = GameMath.getAngleDir(currentAngle);
                  mob.appearAbility.runAndSend(mob.x, mob.y, -dir.x, -dir.y);
                  this.ticker = (int)(20.0F * GameRandom.globalRandom.getFloatBetween(8.0F, 9.0F));
               }
            }
         }

         return AINodeResult.SUCCESS;
      }
   }

   public static class SpawnBouldersAI<T extends SwampGuardianHead> extends AINode<T> {
      public int ticker;
      public TargetFinderAINode<T> targetFinder;

      public SpawnBouldersAI(TargetFinderAINode<T> targetFinder) {
         this.targetFinder = targetFinder;
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         this.ticker = (int)(20.0F * GameRandom.globalRandom.getFloatBetween(0.8F, 2.0F));
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         this.ticker--;
         if (this.ticker <= 0) {
            WormMobHead.BodyPartTarget t = mob.getRandomTargetFromBodyPart(this, this.targetFinder, (m, bp) -> {
               float distx = m.getDistance(bp);
               return distx > 350.0F && distx < 400.0F;
            });
            if (t != null) {
               Float targetPos = new Float(
                  t.target.x + GameRandom.globalRandom.floatGaussian() * 30.0F, t.target.y + GameRandom.globalRandom.floatGaussian() * 30.0F
               );
               int dist = (int)t.bodyPart.getDistance(targetPos.x, targetPos.y);
               t.bodyPart
                  .getLevel()
                  .entityManager
                  .projectiles
                  .add(
                     new SwampBoulderProjectile(
                        t.bodyPart.getLevel(), mob, t.bodyPart.x, t.bodyPart.y, targetPos.x, targetPos.y, 40.0F, dist, new GameDamage(0.0F), 50
                     )
                  );
               mob.swingSound.runAndSend(t.bodyPart.getX(), t.bodyPart.getY());
            }

            this.ticker = (int)(20.0F * GameRandom.globalRandom.getFloatBetween(0.8F, 2.0F));
         }

         return AINodeResult.SUCCESS;
      }
   }

   public static class SwampGuardianAI<T extends SwampGuardianHead> extends SelectorAINode<T> {
      public SwampGuardianAI() {
         SequenceAINode<T> chaserSequence = new SequenceAINode<>();
         this.addChild(chaserSequence);
         chaserSequence.addChild(new RemoveOnNoTargetNode<>(100));
         final TargetFinderAINode<T> targetFinder;
         chaserSequence.addChild(targetFinder = new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         targetFinder.moveToAttacker = false;
         ChargingCirclingChaserAINode<T> chaserAI;
         chaserSequence.addChild(chaserAI = new ChargingCirclingChaserAINode<>(500, 40));
         chaserSequence.addChild(
            new SpawnProjectilesOnHealthLossAINode<T>(SwampGuardianHead.totalRazorProjectiles) {
               public void shootProjectile(T mob) {
                  WormMobHead.BodyPartTarget t = mob.getRandomTargetFromBodyPart(this, targetFinder, (m, bp) -> {
                     if (bp.getDistance(m) > 500.0F) {
                        return false;
                     }

                     CollisionFilter collisionFilter = bp.modifyChasingCollisionFilter(new CollisionFilter().mobCollision(), m);
                     return !mob.getLevel().collides(new java.awt.geom.Line2D.Float(m.x, m.y, bp.x, bp.y), collisionFilter);
                  });
                  if (t != null) {
                     t.bodyPart
                        .getLevel()
                        .entityManager
                        .projectiles
                        .add(
                           new SwampRazorProjectile(
                              t.bodyPart.getLevel(), mob, t.bodyPart.x, t.bodyPart.y, t.target.x, t.target.y, 70.0F, 1750, SwampGuardianHead.razorDamage, 50
                           )
                        );
                     mob.flickSound.runAndSend(t.bodyPart.getX(), t.bodyPart.getY());
                  }
               }
            }
         );
         chaserSequence.addChild(new SwampGuardianHead.SpawnBouldersAI<>(targetFinder));
         chaserSequence.addChild(new SwampGuardianHead.DiveChargeRotationAI<>(chaserAI));
         this.addChild(new WandererAINode<>(0));
      }
   }
}
