package necesse.entity.mobs.hostile.bosses;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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
import necesse.engine.registries.ItemRegistry;
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
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
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
import necesse.entity.mobs.hostile.bosses.bossAIUtils.FlyToOppositeDirectionAttackStage;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.FlyToRandomPositionAttackStage;
import necesse.entity.mobs.hostile.bosses.bossAIUtils.IdleTimeAttackStage;
import necesse.entity.mobs.mobMovement.MobMovementCircleLevelPos;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.QueenSpiderEggProjectile;
import necesse.entity.projectile.QueenSpiderSpitProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.item.matItem.MultiTextureMatItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ConditionLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class QueenSpiderMob extends FlyingBossMob {
   public static LootTable lootTable = new LootTable(
      LootItem.between("cavespidergland", 10, 20, MultiTextureMatItem.getGNDData(1)), new ChanceLootItem(0.2F, "queenspidersdancevinyl")
   );
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      new LootItem("spidercharm"),
      new LootItem("spiderclaw"),
      new LootItemList(new LootItem("webbedgun"), LootItem.between("simplebullet", 50, 100)),
      new LootItem("frostpiercer")
   );
   public static LootTable privateLootTable = new LootTable(
      new ConditionLootItem(
         "spiderheart",
         (r, o) -> {
            ServerClient client = LootTable.expectExtra(ServerClient.class, o, 1);
            return client != null
               && client.playerMob.healthUpgradeManager.canUpgrade("spiderheart")
               && client.playerMob.getInv().getAmount(ItemRegistry.getItem("spiderheart"), false, false, true, true, "have") == 0;
         }
      ),
      uniqueDrops
   );
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   private ArrayList<QueenSpiderMob.SpiderLeg> frontLegs;
   private ArrayList<QueenSpiderMob.SpiderLeg> backLegs;
   public float currentHeight;
   public EmptyMobAbility roarAbility;
   public IntMobAbility startLaunchAnimation;
   public long launchStartTime;
   public int launchAnimationTime;
   public EmptyMobAbility playSpitSoundAbility;
   public int jumpStartX;
   public int jumpStartY;
   public int jumpEndX;
   public int jumpEndY;
   public boolean isJumping;
   public CoordinateMobAbility startJumpAbility;
   public static GameDamage collisionDamage = new GameDamage(26.0F);
   public static GameDamage spitDamage = new GameDamage(22.0F);
   public static GameDamage landDamage = new GameDamage(55.0F);
   public static GameDamage hatchlingDamage = new GameDamage(22.0F);
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(4000, 4500, 5000, 5500, 6000);
   public static float SPIT_LINGER_SECONDS = 60.0F;

   public QueenSpiderMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.moveAccuracy = 20;
      this.setSpeed(75.0F);
      this.setArmor(5);
      this.setFriction(2.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-60, -60, 120, 90);
      this.hitBox = new Rectangle(-60, -60, 120, 90);
      this.selectBox = new Rectangle(-70, -45, 140, 100);
      this.roarAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (QueenSpiderMob.this.isClient()) {
               SoundManager.playSound(GameResources.roar, SoundEffect.globalEffect().volume(0.7F).pitch(1.3F));
            }
         }
      });
      this.startLaunchAnimation = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            QueenSpiderMob.this.launchStartTime = QueenSpiderMob.this.getWorldEntity().getLocalTime();
            QueenSpiderMob.this.launchAnimationTime = value;
            if (QueenSpiderMob.this.isClient()) {
               SoundManager.playSound(GameResources.spit, SoundEffect.effect(QueenSpiderMob.this));
            }
         }
      });
      this.playSpitSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (QueenSpiderMob.this.isClient()) {
               SoundManager.playSound(GameResources.spit, SoundEffect.effect(QueenSpiderMob.this));
            }
         }
      });
      this.startJumpAbility = this.registerAbility(new CoordinateMobAbility() {
         @Override
         protected void run(int x, int y) {
            QueenSpiderMob.this.jumpStartX = QueenSpiderMob.this.getX();
            QueenSpiderMob.this.jumpStartY = QueenSpiderMob.this.getY();
            QueenSpiderMob.this.jumpEndX = x;
            QueenSpiderMob.this.jumpEndY = y;
            QueenSpiderMob.this.isJumping = true;
         }
      });
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextBoolean(this.isJumping);
      if (this.isJumping) {
         writer.putNextInt(this.jumpStartX);
         writer.putNextInt(this.jumpStartY);
         writer.putNextInt(this.jumpEndX);
         writer.putNextInt(this.jumpEndY);
      }
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.isJumping = reader.getNextBoolean();
      if (this.isJumping) {
         this.jumpStartX = reader.getNextInt();
         this.jumpStartY = reader.getNextInt();
         this.jumpEndX = reader.getNextInt();
         this.jumpEndY = reader.getNextInt();
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
   public void setPos(float x, float y, boolean isDirect) {
      super.setPos(x, y, isDirect);
      if (isDirect && this.backLegs != null && this.frontLegs != null) {
         for (QueenSpiderMob.SpiderLeg leg : this.backLegs) {
            leg.snapToPosition();
         }

         for (QueenSpiderMob.SpiderLeg leg : this.frontLegs) {
            leg.snapToPosition();
         }
      }
   }

   @Override
   public void init() {
      super.init();
      this.frontLegs = new ArrayList<>();
      this.backLegs = new ArrayList<>();
      int legCount = 8;
      this.currentHeight = this.getDesiredHeight();
      float[] angles = new float[]{65.0F, 95.0F, 125.0F, 155.0F, -65.0F, -95.0F, -125.0F, -155.0F};

      for (int i = 0; i < legCount; i++) {
         final float angle = angles[i] - 90.0F;
         float offsetPercent = (float)(i + (i % 2 == 0 ? 4 : 0)) / legCount % 1.0F;
         final java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
         float maxLeftAngle = 170.0F;
         float maxRightAngle = 170.0F;
         if (dir.x < 0.0F) {
            maxRightAngle = 0.0F;
         } else if (dir.x > 0.0F) {
            maxLeftAngle = 0.0F;
         }

         QueenSpiderMob.SpiderLeg leg = new QueenSpiderMob.SpiderLeg(this, 125.0F, offsetPercent, maxLeftAngle, maxRightAngle) {
            @Override
            public GamePoint3D getCenterPosition() {
               int dist = 50;
               return new GamePoint3D(
                  QueenSpiderMob.this.getDrawX() + dir.x * dist, QueenSpiderMob.this.getDrawY() + dir.y * dist * 0.5F, QueenSpiderMob.this.getFlyingHeight()
               );
            }

            @Override
            public GamePoint3D getDesiredPosition() {
               java.awt.geom.Point2D.Float dirx = GameMath.getAngleDir(angle);
               int dist = 130;
               float moveMod = Math.min(QueenSpiderMob.this.getCurrentSpeed() / 250.0F, 1.0F);
               java.awt.geom.Point2D.Float moveDir = GameMath.normalize(QueenSpiderMob.this.dx, QueenSpiderMob.this.dy);
               if (moveDir.y < 0.0F) {
                  moveMod *= 1.0F + -moveDir.y * 30.0F / QueenSpiderMob.this.getSpeed();
               }

               return new GamePoint3D(
                  QueenSpiderMob.this.getDrawX() + dirx.x * dist + moveDir.x * dist * moveMod,
                  QueenSpiderMob.this.getDrawY() + dirx.y * dist + moveDir.y * dist * moveMod,
                  0.0F
               );
            }

            @Override
            public float getJumpHeight() {
               return QueenSpiderMob.this.getCurrentJumpHeight();
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
      this.ai = new BehaviourTreeAI<>(this, new QueenSpiderMob.SpiderMotherAI<>(), new FlyingAIMover());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.queenspiderbegin, SoundEffect.effect(this).volume(1.3F).falloffDistance(4000));
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
      float pitch = GameRandom.globalRandom.getOneOf(0.9F, 0.95F, 1.0F);
      SoundManager.playSound(GameResources.queenspiderhurt, SoundEffect.effect(this).pitch(pitch).volume(0.2F).falloffDistance(1500));
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.queenspiderdeath, SoundEffect.effect(this).falloffDistance(3000));
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
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
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

      if (this.isJumping) {
         float distToEnd = this.getDistance(this.jumpEndX, this.jumpEndY);
         float distToMove = this.getSpeed() * 1.3F * delta / 250.0F;
         if (!(distToEnd < distToMove)) {
            java.awt.geom.Point2D.Float dir = GameMath.normalize(this.jumpEndX - this.x, this.jumpEndY - this.y);
            this.x = this.x + dir.x * distToMove;
            this.y = this.y + dir.y * distToMove;
         } else {
            int particles = 200;
            float anglePerParticle = 360.0F / particles;

            for (int i = 0; i < particles; i++) {
               int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
               float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(100, 200);
               float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(100, 200) * 0.8F;
               this.getLevel()
                  .entityManager
                  .addParticle(this.x, this.y, i % 4 == 0 ? Particle.GType.IMPORTANT_COSMETIC : Particle.GType.COSMETIC)
                  .movesFriction(dx, dy, 0.8F)
                  .color(new Color(50, 50, 50))
                  .heightMoves(0.0F, 30.0F)
                  .lifeTime(1000);
            }

            this.x = this.jumpEndX;
            this.y = this.jumpEndY;
            this.isJumping = false;
            this.stopMoving();
            if (this.isServer()) {
               int size = 300;
               int halfSize = size / 2;
               Ellipse2D hitBox = new java.awt.geom.Ellipse2D.Float(this.x - halfSize, this.y - halfSize * 0.8F, size, size * 0.8F);
               GameUtils.streamTargets(this, GameUtils.rangeTileBounds(this.getX(), this.getY(), 8))
                  .filter(m -> m.canBeHit(this) && hitBox.intersects(m.getHitBox()))
                  .forEach(m -> m.isServerHit(landDamage, m.getX() - this.x, m.getY() - this.y, 150.0F, this));
            }
         }

         this.calcNetworkSmooth(delta);

         for (QueenSpiderMob.SpiderLeg leg : this.backLegs) {
            leg.snapToPosition();
         }

         for (QueenSpiderMob.SpiderLeg leg : this.frontLegs) {
            leg.snapToPosition();
         }
      } else {
         super.tickMovement(delta);

         for (QueenSpiderMob.SpiderLeg leg : this.backLegs) {
            leg.tickMovement(delta);
         }

         for (QueenSpiderMob.SpiderLeg leg : this.frontLegs) {
            leg.tickMovement(delta);
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.QueenSpidersDance, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(70.0F + healthPercInv * 40.0F);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(70.0F + healthPercInv * 40.0F);
   }

   @Override
   public int getFlyingHeight() {
      return (int)this.currentHeight;
   }

   public float getDesiredHeight() {
      float perc = GameUtils.getAnimFloat(this.getWorldEntity().getTime(), 1000);
      float height = GameMath.sin(perc * 360.0F) * 10.0F;
      long localTime = this.getWorldEntity().getLocalTime();
      if (this.isJumping) {
         height = 0.0F;
      } else if (localTime < this.launchStartTime + this.launchAnimationTime) {
         float progress = (float)(localTime - this.launchStartTime) / this.launchAnimationTime;
         float endPref = 0.4F;
         float slope = 1.5F;
         height = 10.0F - (float)Math.pow(Math.sin(Math.pow(progress * Math.PI, endPref) / Math.pow(Math.PI, endPref - 1.0F)), slope) * 25.0F;
      }

      return 20 + (int)height;
   }

   public float getCurrentJumpHeight() {
      if (!this.isJumping) {
         return 0.0F;
      }

      float totalDist = (float)new java.awt.geom.Point2D.Float(this.jumpStartX, this.jumpStartY).distance(this.jumpEndX, this.jumpEndY);
      float distToEnd = (float)new java.awt.geom.Point2D.Float(this.x, this.y).distance(this.jumpEndX, this.jumpEndY);
      float distPerc = distToEnd / totalDist;
      return GameMath.sin(distPerc * 180.0F) * totalDist / 1.2F;
   }

   @Override
   public Rectangle getSelectBox(int x, int y) {
      Rectangle selectBox = super.getSelectBox(x, y);
      selectBox.y = selectBox.y - this.getFlyingHeight();
      return selectBox;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 7; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.queenSpiderDebris,
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
      drawY = (int)(drawY - this.getCurrentJumpHeight());
      float rotate = GameMath.limit(this.dx / 10.0F, -10.0F, 10.0F);
      DrawOptions body = MobRegistry.Textures.queenSpiderBody
         .initDraw()
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, MobRegistry.Textures.queenSpiderBody.getWidth() / 2, (int)(MobRegistry.Textures.queenSpiderBody.getHeight() * 0.6F))
         .posMiddle(drawX, drawY);
      DrawOptions head = MobRegistry.Textures.queenSpiderHead
         .initDraw()
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, MobRegistry.Textures.queenSpiderHead.getWidth() / 2, (int)(MobRegistry.Textures.queenSpiderHead.getHeight() * 0.6F))
         .posMiddle(drawX, drawY + 24);
      DrawOptionsList legsShadows = new DrawOptionsList();
      DrawOptionsList backLegsDrawBottom = new DrawOptionsList();
      DrawOptionsList backLegsDrawsTop = new DrawOptionsList();
      DrawOptionsList frontLegsDrawBottom = new DrawOptionsList();
      DrawOptionsList frontLegsDrawsTop = new DrawOptionsList();

      for (QueenSpiderMob.SpiderLeg leg : this.backLegs) {
         leg.addDrawOptions(legsShadows, backLegsDrawBottom, backLegsDrawsTop, level, camera, perspective);
      }

      for (QueenSpiderMob.SpiderLeg leg : this.frontLegs) {
         leg.addDrawOptions(legsShadows, frontLegsDrawBottom, frontLegsDrawsTop, level, camera, perspective);
      }

      TextureDrawOptions shadowOptions = this.getShadowDrawOptions(level, x, y, light, camera);
      topList.add(tm -> {
         shadowOptions.draw();
         legsShadows.draw();
         backLegsDrawBottom.draw();
         backLegsDrawsTop.draw();
         body.draw();
         frontLegsDrawBottom.draw();
         frontLegsDrawsTop.draw();
         head.draw();
      });
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.queenSpider_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2 + 24;
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      MobRegistry.Textures.queenSpiderHead
         .initDraw()
         .size(MobRegistry.Textures.queenSpiderHead.getWidth() / 2, MobRegistry.Textures.queenSpiderHead.getHeight() / 2)
         .posMiddle(x, y)
         .draw();
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

   public static class ChargeTargetStage<T extends QueenSpiderMob> extends FlyToOppositeDirectionAttackStage<T> {
      public ChargeTargetStage() {
         super(true, 250.0F, 0.0F);
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         super.onStarted(mob, blackboard);
         if (blackboard.mover.isMoving()) {
            mob.roarAbility.runAndSend();
            mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.SPIDER_CHARGE, mob, 5.0F, null), true);
         }
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
         super.onEnded(mob, blackboard);
         mob.buffManager.removeBuff(BuffRegistry.SPIDER_CHARGE, true);
      }
   }

   public static class JumpTargetStage<T extends QueenSpiderMob> extends AINode<T> implements AttackStageInterface<T> {
      public boolean jumped = false;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.jumped = false;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Mob target = blackboard.getObject(Mob.class, "currentTarget");
         if (target == null) {
            return AINodeResult.SUCCESS;
         }

         if (!this.jumped) {
            mob.startJumpAbility
               .runAndSend((int)(target.x + GameRandom.globalRandom.floatGaussian() * 30.0F), (int)(target.y + GameRandom.globalRandom.floatGaussian() * 30.0F));
            this.jumped = true;
         }

         return mob.isJumping ? AINodeResult.RUNNING : AINodeResult.SUCCESS;
      }
   }

   public static class LaunchEggsStage<T extends QueenSpiderMob> extends AINode<T> implements AttackStageInterface<T> {
      public float buffer;
      public float eggsPerLaunch;
      public int launchCounter;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.buffer = 0.0F;
         this.eggsPerLaunch = 1.0F;
         this.launchCounter = 0;
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         float launchesPerSecond = 0.7F;
         this.buffer += 1.0F / launchesPerSecond / 20.0F;
         if (this.buffer >= 1.0F) {
            mob.startLaunchAnimation.runAndSend((int)(launchesPerSecond * 1000.0F));

            for (int i = 0; i < (int)this.eggsPerLaunch; i++) {
               float mod = GameRandom.globalRandom.getFloatBetween(0.7F, 1.0F);
               int dist = (int)(300.0F * mod);
               float angle = GameRandom.globalRandom.nextInt(360);
               java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
               java.awt.geom.Point2D.Float targetPos = new java.awt.geom.Point2D.Float(mob.x + dir.x * dist, mob.y + dir.y * dist);
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(new QueenSpiderEggProjectile(mob.getLevel(), mob, mob.x, mob.y, targetPos.x, targetPos.y, 30.0F, dist, new GameDamage(0.0F), 50));
            }

            this.buffer--;
            float healthPercInv = Math.abs(GameMath.limit((float)mob.getHealth() / mob.getMaxHealth(), 0.0F, 1.0F) - 1.0F);
            float eggsIncPerLaunch = healthPercInv * 1.3F;
            long clients = GameUtils.streamServerClients(mob.getLevel()).filter(c -> !c.isDead() && mob.getDistance(c.playerMob) < 1280.0F).count();
            float clientsMod = Math.min(1.0F + (float)(clients - 1L) / 2.0F, 4.0F);
            this.eggsPerLaunch += eggsIncPerLaunch * clientsMod;
            this.launchCounter++;
            if (this.launchCounter >= 4) {
               return AINodeResult.SUCCESS;
            }
         }

         return AINodeResult.RUNNING;
      }
   }

   public abstract static class SpiderLeg {
      public final Mob mob;
      public float startX;
      public float startY;
      public float x;
      public float y;
      public float nextX;
      public float nextY;
      public boolean isMoving;
      private InverseKinematics ik;
      private List<Float> shadowLines;
      public float maxLeftAngle;
      public float maxRightAngle;
      private final float moveAtDist;
      private float checkX;
      private float checkY;
      private float distBuffer;

      public SpiderLeg(Mob mob, float moveAtDist, float offsetPercent, float maxLeftAngle, float maxRightAngle) {
         this.mob = mob;
         this.moveAtDist = moveAtDist;
         this.distBuffer = moveAtDist * offsetPercent;
         this.maxLeftAngle = maxLeftAngle;
         this.maxRightAngle = maxRightAngle;
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
         double checkDist = new java.awt.geom.Point2D.Float(centerPos.x, centerPos.y).distance(this.checkX, this.checkY);
         this.distBuffer = (float)(this.distBuffer + checkDist);
         this.checkX = centerPos.x;
         this.checkY = centerPos.y;
         if (checkDist == 0.0) {
            this.distBuffer = this.distBuffer + delta / (this.moveAtDist / 20.0F);
         }

         if (!this.isMoving) {
            GamePoint3D desiredPos = this.getDesiredPosition();
            double desiredDist = new java.awt.geom.Point2D.Float(desiredPos.x, desiredPos.y).distance(this.x, this.y);
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
            double nextDist = new java.awt.geom.Point2D.Float(this.x, this.y).distance(this.nextX, this.nextY);
            float speed = (float)nextDist * 2.0F + this.mob.getSpeed() * 1.2F;
            float distToMove = speed * delta / 250.0F;
            if (nextDist < distToMove) {
               if (this.mob.isClient()) {
                  int particles = 20;
                  float anglePerParticle = 360.0F / particles;

                  for (int i = 0; i < particles; i++) {
                     int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
                     float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(10, 40);
                     float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(10, 40) * 0.8F;
                     this.mob
                        .getLevel()
                        .entityManager
                        .addParticle(this.x, this.y, i % 4 == 0 ? Particle.GType.IMPORTANT_COSMETIC : Particle.GType.COSMETIC)
                        .movesFriction(dx, dy, 0.8F)
                        .color(new Color(50, 50, 50))
                        .heightMoves(0.0F, 10.0F)
                        .lifeTime(500);
                  }

                  SoundManager.playSound(GameResources.punch, SoundEffect.effect(this.x, this.y).volume(0.4F).pitch(0.8F));
               }

               this.x = this.nextX;
               this.y = this.nextY;
               this.isMoving = false;
            } else {
               java.awt.geom.Point2D.Float dir = GameMath.normalize(this.nextX - this.x, this.nextY - this.y);
               this.x = this.x + dir.x * distToMove;
               this.y = this.y + dir.y * distToMove;
            }
         }

         this.updateIK();
      }

      public void updateIK() {
         if (!this.mob.isServer()) {
            GamePoint3D centerPos = this.getCenterPosition();
            java.awt.geom.Point2D.Float dir = GameMath.normalize(this.x - centerPos.x, this.y - centerPos.y);
            float jointDistMod = 0.5F;
            float dist = (float)new java.awt.geom.Point2D.Float(centerPos.x, centerPos.y).distance(this.x, this.y) * jointDistMod;
            float jointHeight = 40.0F;
            GamePoint3D jointPos = centerPos.dirFromLength(centerPos.x + dir.x * dist, centerPos.y + dir.y * dist, jointHeight, 80.0F);
            GamePoint3D footPos = jointPos.dirFromLength(this.x, this.y, 0.0F, 100.0F);
            float jumpHeight = this.getJumpHeight();
            float perspectiveMod = 0.6F;
            synchronized (this) {
               this.shadowLines = Collections.synchronizedList(new LinkedList<>());
               this.shadowLines.add(new Float(centerPos.x, centerPos.y + 18.0F, jointPos.x, jointPos.y - jointPos.height * perspectiveMod));
               this.shadowLines.add(new Float(jointPos.x, jointPos.y - jointPos.height * perspectiveMod, this.x, this.y));
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
         double startDist = new java.awt.geom.Point2D.Float(this.startX, this.startY).distance(this.nextX, this.nextY);
         float lift = Math.min((float)startDist / 40.0F, 1.0F) * 20.0F + 5.0F;
         double currentDist = new java.awt.geom.Point2D.Float(this.x, this.y).distance(this.nextX, this.nextY);
         double progress = GameMath.limit(currentDist / startDist, 0.0, 1.0);
         return GameMath.sin((float)progress * 180.0F) * lift;
      }

      public abstract float getJumpHeight();

      public abstract GamePoint3D getDesiredPosition();

      public abstract GamePoint3D getCenterPosition();

      private static Shape generateShadowShape(Iterable<Float> lines, float radius) {
         LinkedList<Float> reverse = new LinkedList<>();
         java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
         Float first = null;
         Float last = null;

         for (Float line : lines) {
            if (first == null) {
               first = line;
            }

            java.awt.geom.Point2D.Float dir = GameMath.normalize(line.x1 - line.x2, line.y1 - line.y2);
            java.awt.geom.Point2D.Float p1 = GameMath.getPerpendicularPoint(line.x1, line.y1, radius, dir);
            java.awt.geom.Point2D.Float p2 = GameMath.getPerpendicularPoint(line.x2, line.y2, radius, dir);
            if (last != null) {
               java.awt.geom.Point2D.Float lastDir = GameMath.normalize(last.x1 - last.x2, last.y1 - last.y2);
               java.awt.geom.Point2D.Float lastP1 = GameMath.getPerpendicularPoint(last.x1, last.y1, radius, lastDir);
               java.awt.geom.Point2D.Float lastP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, radius, lastDir);
               Point2D intersectionPoint = GameMath.getIntersectionPoint(new Float(p1, p2), new Float(lastP1, lastP2), false);
               if (intersectionPoint != null) {
                  path.lineTo(intersectionPoint.getX(), intersectionPoint.getY());
               } else {
                  path.lineTo(lastP2.x, lastP2.y);
                  path.lineTo(p1.x, p1.y);
               }
            } else {
               path.moveTo(p1.x, p1.y);
            }

            last = line;
            reverse.addFirst(line);
         }

         if (last != null) {
            java.awt.geom.Point2D.Float lastDir = GameMath.normalize(last.x1 - last.x2, last.y1 - last.y2);
            java.awt.geom.Point2D.Float lastP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, radius, lastDir);
            path.lineTo(lastP2.x, lastP2.y);
         }

         last = null;

         for (Float line : reverse) {
            java.awt.geom.Point2D.Float dir = GameMath.normalize(line.x2 - line.x1, line.y2 - line.y1);
            java.awt.geom.Point2D.Float p1 = GameMath.getPerpendicularPoint(line.x1, line.y1, radius, dir);
            java.awt.geom.Point2D.Float p2 = GameMath.getPerpendicularPoint(line.x2, line.y2, radius, dir);
            if (last != null) {
               java.awt.geom.Point2D.Float lastDir = GameMath.normalize(last.x2 - last.x1, last.y2 - last.y1);
               java.awt.geom.Point2D.Float lastP1 = GameMath.getPerpendicularPoint(last.x1, last.y1, radius, lastDir);
               java.awt.geom.Point2D.Float lastP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, radius, lastDir);
               Point2D intersectionPoint = GameMath.getIntersectionPoint(new Float(p2, p1), new Float(lastP2, lastP1), false);
               if (intersectionPoint != null) {
                  path.lineTo(intersectionPoint.getX(), intersectionPoint.getY());
               } else {
                  path.lineTo(lastP1.x, lastP1.y);
                  path.lineTo(p2.x, p2.y);
               }
            } else {
               path.lineTo(p2.x, p2.y);
            }

            last = line;
         }

         if (last != null) {
            java.awt.geom.Point2D.Float lastDir = GameMath.normalize(last.x2 - last.x1, last.y2 - last.y1);
            java.awt.geom.Point2D.Float lastP1 = GameMath.getPerpendicularPoint(last.x1, last.y1, radius, lastDir);
            path.lineTo(lastP1.x, lastP1.y);
         }

         path.closePath();
         return path;
      }

      private static LinkedList<java.awt.geom.Point2D.Float> generateShadowTriangles(Iterable<Float> lines, float radius) {
         LinkedList<java.awt.geom.Point2D.Float> out = new LinkedList<>();
         Float last = null;

         for (Float line : lines) {
            java.awt.geom.Point2D.Float dir = GameMath.normalize(line.x1 - line.x2, line.y1 - line.y2);
            java.awt.geom.Point2D.Float leftP1 = GameMath.getPerpendicularPoint(line.x1, line.y1, radius, dir);
            java.awt.geom.Point2D.Float leftP2 = GameMath.getPerpendicularPoint(line.x2, line.y2, radius, dir);
            java.awt.geom.Point2D.Float rightP1 = GameMath.getPerpendicularPoint(line.x1, line.y1, -radius, dir);
            java.awt.geom.Point2D.Float rightP2 = GameMath.getPerpendicularPoint(line.x2, line.y2, -radius, dir);
            if (last != null) {
               java.awt.geom.Point2D.Float lastDir = GameMath.normalize(last.x1 - last.x2, last.y1 - last.y2);
               java.awt.geom.Point2D.Float lastLeftP1 = GameMath.getPerpendicularPoint(last.x1, last.y1, radius, lastDir);
               java.awt.geom.Point2D.Float lastLeftP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, radius, lastDir);
               Point2D leftIntersection = GameMath.getIntersectionPoint(new Float(leftP1, leftP2), new Float(lastLeftP1, lastLeftP2), false);
               java.awt.geom.Point2D.Float lastRightP1 = GameMath.getPerpendicularPoint(last.x1, last.y1, -radius, lastDir);
               java.awt.geom.Point2D.Float lastRightP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, -radius, lastDir);
               Point2D rightIntersection = GameMath.getIntersectionPoint(new Float(rightP1, rightP2), new Float(lastRightP1, lastRightP2), false);
               if (leftIntersection != null) {
                  out.add(new java.awt.geom.Point2D.Float((float)leftIntersection.getX(), (float)leftIntersection.getY()));
                  out.add(lastRightP2);
                  out.add(new java.awt.geom.Point2D.Float((float)leftIntersection.getX(), (float)leftIntersection.getY()));
                  out.add(rightP1);
               } else if (rightIntersection != null) {
                  out.add(lastLeftP2);
                  out.add(new java.awt.geom.Point2D.Float((float)rightIntersection.getX(), (float)rightIntersection.getY()));
                  out.add(leftP1);
                  out.add(new java.awt.geom.Point2D.Float((float)rightIntersection.getX(), (float)rightIntersection.getY()));
               } else {
                  out.add(leftP1);
                  out.add(rightP1);
               }
            } else {
               out.add(leftP1);
               out.add(rightP1);
            }

            last = line;
         }

         if (last != null) {
            java.awt.geom.Point2D.Float dir = GameMath.normalize(last.x1 - last.x2, last.y1 - last.y2);
            java.awt.geom.Point2D.Float leftP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, radius, dir);
            java.awt.geom.Point2D.Float rightP2 = GameMath.getPerpendicularPoint(last.x2, last.y2, -radius, dir);
            out.add(leftP2);
            out.add(rightP2);
         }

         return out;
      }

      public void addDrawOptions(
         DrawOptionsList legsShadows, DrawOptionsList legsDrawBottom, DrawOptionsList legsDrawsTop, Level level, GameCamera camera, PlayerMob perspective
      ) {
         if (!this.mob.isServer()) {
            int legShadowWidth = MobRegistry.Textures.queenSpiderLeg_shadow.getWidth();
            synchronized (this) {
               for (Float shadowLine : this.shadowLines) {
                  GameLight light = level.getLightLevel(
                     Entity.getTileCoordinate((shadowLine.x1 + shadowLine.x2) / 2.0F), Entity.getTileCoordinate((shadowLine.y1 + shadowLine.y2) / 2.0F)
                  );
                  float angle = GameMath.getAngle(new java.awt.geom.Point2D.Float(shadowLine.x1 - shadowLine.x2, shadowLine.y1 - shadowLine.y2));
                  float length = (float)shadowLine.getP1().distance(shadowLine.getP2());
                  TextureDrawOptions drawOptions = MobRegistry.Textures.queenSpiderLeg_shadow
                     .initDraw()
                     .rotate(angle + 90.0F, legShadowWidth / 2, 6)
                     .startGlowOptions(level, this.mob.getID())
                     .light(light)
                     .applyEnemyTracker(this.mob, perspective)
                     .size(legShadowWidth, (int)length + 16)
                     .pos(camera.getDrawX(shadowLine.x1 - legShadowWidth / 2.0F), camera.getDrawY(shadowLine.y1));
                  legsShadows.add(drawOptions);
               }
            }

            float jumpHeight = this.getJumpHeight();
            int legTextureWidth = MobRegistry.Textures.queenSpiderLeg.getWidth();

            for (InverseKinematics.Limb limb : this.ik.limbs) {
               GameLight light = level.getLightLevel(
                  Entity.getTileCoordinate((limb.inboundX + limb.outboundX) / 2.0F),
                  Entity.getTileCoordinate((limb.inboundY + limb.outboundY) / 2.0F + jumpHeight)
               );
               TextureDrawOptions drawOptions = MobRegistry.Textures.queenSpiderLeg
                  .initDraw()
                  .rotate(limb.angle - 90.0F, legTextureWidth / 2, 4)
                  .startGlowOptions(level, this.mob.getID())
                  .light(light)
                  .applyEnemyTracker(this.mob, perspective)
                  .size(legTextureWidth, (int)limb.length + 16)
                  .pos(camera.getDrawX(limb.inboundX - legTextureWidth / 2.0F), camera.getDrawY(limb.inboundY) - 8);
               if (this.ik.limbs.getLast() == limb) {
                  legsDrawBottom.add(drawOptions);
               } else {
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

   public static class SpiderMotherAI<T extends QueenSpiderMob> extends SequenceAINode<T> {
      public SpiderMotherAI() {
         this.addChild(new RemoveOnNoTargetNode<>(100));
         this.addChild(new TargetFinderAINode<T>(3200) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         AttackStageManagerNode<T> attackStages = new AttackStageManagerNode<>();
         this.addChild(new IsolateRunningAINode<>(attackStages));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new QueenSpiderMob.LaunchEggsStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1000)));
         attackStages.addChild(new FlyToOppositeDirectionAttackStage<>(true, 300.0F, 20.0F));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new QueenSpiderMob.SpitStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 500)));
         attackStages.addChild(new FlyToRandomPositionAttackStage<>(true, 300));
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1000)));
         attackStages.addChild(new QueenSpiderMob.ChargeTargetStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1000)));
         attackStages.addChild(new QueenSpiderMob.ChargeTargetStage<>());
         attackStages.addChild(new IdleTimeAttackStage<>(m -> this.getIdleTime(m, 1000)));
         attackStages.addChild(new QueenSpiderMob.ChargeTargetStage<>());
      }

      private int getIdleTime(T mob, int maxTime) {
         float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
         return (int)(maxTime * healthPerc);
      }
   }

   public static class SpitStage<T extends QueenSpiderMob> extends AINode<T> implements AttackStageInterface<T> {
      private int timer;
      private float shootBuffer;
      private boolean reversed;
      private int radius;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         this.timer += 50;
         if (currentTarget != null) {
            float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
            float secondsPerSpit = 0.25F + healthPerc * 0.4F;
            this.shootBuffer += 1.0F / secondsPerSpit / 20.0F;
            if (this.shootBuffer >= 1.0F) {
               this.shootBuffer--;
               float healthPercInv = Math.abs(healthPerc - 1.0F);
               float mod = GameRandom.globalRandom.nextFloat();
               int targetDist = (int)(140.0F * mod);
               float angle = GameRandom.globalRandom.nextInt(360);
               java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
               java.awt.geom.Point2D.Float targetPos = new java.awt.geom.Point2D.Float(
                  currentTarget.x + dir.x * targetDist, currentTarget.y + dir.y * targetDist
               );
               int dist = Math.min((int)targetPos.distance(mob.x, mob.y), 960);
               float speed = 60.0F + healthPercInv * 40.0F;
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(new QueenSpiderSpitProjectile(mob.getLevel(), mob, mob.x, mob.y, targetPos.x, targetPos.y, speed, dist, QueenSpiderMob.spitDamage, 50));
               mob.playSpitSoundAbility.runAndSend();
            }
         }

         if (this.timer >= 4000) {
            blackboard.mover.stopMoving(mob);
            return AINodeResult.SUCCESS;
         } else {
            return AINodeResult.RUNNING;
         }
      }

      public void onStarted(T mob, Blackboard<T> blackboard) {
         this.timer = 0;
         this.shootBuffer = 0.0F;
         this.reversed = !this.reversed;
         Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
         if (currentTarget != null) {
            this.radius = GameMath.limit((int)mob.getDistance(currentTarget), 200, 400);
            float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
            float speedMod = 0.5F + Math.abs(healthPerc - 1.0F) / 2.0F;
            float speed = MobMovementCircleLevelPos.convertToRotSpeed(this.radius, mob.getSpeed() * speedMod);
            blackboard.mover.setCustomMovement(this, new MobMovementCircleLevelPos(mob, currentTarget.x, currentTarget.y, this.radius, speed, this.reversed));
         }
      }

      public void onEnded(T mob, Blackboard<T> blackboard) {
      }
   }
}
