package necesse.entity.mobs.hostile.pirates;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.AreaFinder;
import necesse.engine.CameraShake;
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
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.PositionSoundEffect;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.manager.EntityManager;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.HumanTexture;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.TimedMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.ConditionAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.PirateAITree;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.PirateHumanMob;
import necesse.entity.mobs.hostile.bosses.BossMob;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.ConditionLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobChance;
import necesse.level.maps.biomes.MobSpawnTable;
import necesse.level.maps.light.GameLight;

public class PirateCaptainMob extends PirateMob {
   public static LootTable lootTable = new LootTable(LootItem.between("coin", 300, 500), new ChanceLootItem(0.2F, "piratecaptainslairvinyl"));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      new LootItemList(new LootItem("handcannon"), LootItem.between("cannonball", 10, 50)), new LootItem("piratetelescope"), new LootItem("spareboatparts")
   );
   public static LootTable privateLootTable = new LootTable(
      new ConditionLootItem(
         "piratesheath",
         (r, o) -> {
            ServerClient client = LootTable.expectExtra(ServerClient.class, o, 1);
            return client != null
               && client.playerMob.getInv().equipment.getTrinketSlotsSize() < 6
               && client.playerMob.getInv().getAmount(ItemRegistry.getItem("piratesheath"), false, false, true, true, "have") == 0;
         }
      ),
      uniqueDrops
   );
   public static MobSpawnTable pirateMobs = new MobSpawnTable().add(100, "piraterecruit").add(50, "pirateparrot");
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(6250, 7750, 8500, 9250, 10750);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   protected boolean isItemSummoned;
   protected boolean inCombat;
   protected boolean hasPlayedIntroSound;
   protected boolean inSecondStage;
   protected boolean inSecondStageTransition;
   protected long secondStageTransitionStartTime;
   protected CameraShake secondStageCameraShake;
   protected SoundPlayer secondStageRumble;
   protected SoundPlayer secondStageWind;
   private PositionSoundEffect secondStageWindEffect;
   protected int settlerSeed = GameRandom.globalRandom.nextInt();
   protected HumanLook look;
   protected float leftCannonRotation = 135.0F;
   protected float midCannonRotation = 90.0F;
   protected float rightCannonRotation = 45.0F;
   public boolean dropLadder;
   protected int shootDistance = 960;
   public BooleanMobAbility setInCombatAbility;
   public TimedMobAbility startSecondStageAbility;
   public EmptyMobAbility endSecondStageAbility;
   public CoordinateMobAbility fireCannonAbility;
   public static int secondStageTransitionTime = 5000;
   public static int captainMeleeDamage = 50;
   public static int captainCannonDamage = 120;
   public static GameDamage collisionDamage = new GameDamage(55.0F);

   public PirateCaptainMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.setSpeed(15.0F);
      this.setKnockbackModifier(0.0F);
      this.setRegen(100.0F);
      this.meleeDamage = captainMeleeDamage;
      this.shootDamage = captainCannonDamage;
      this.setInCombatAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            PirateCaptainMob.this.inCombat = value;
         }
      });
      this.startSecondStageAbility = this.registerAbility(new TimedMobAbility() {
         @Override
         protected void run(long time) {
            PirateCaptainMob.this.inSecondStageTransition = true;
            PirateCaptainMob.this.secondStageTransitionStartTime = time;
            PirateCaptainMob.this.inSecondStage = true;
            PirateCaptainMob.this.moveX = 0.0F;
            PirateCaptainMob.this.moveY = 0.0F;
            PirateCaptainMob.this.setMovement(null);
            PirateCaptainMob.this.updateStageVariables();
         }
      });
      this.endSecondStageAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            PirateCaptainMob.this.inSecondStageTransition = false;
            PirateCaptainMob.this.inSecondStage = false;
            PirateCaptainMob.this.updateStageVariables();
            if (!PirateCaptainMob.this.isClient() && PirateCaptainMob.this.collidesWith(PirateCaptainMob.this.getLevel())) {
               Point pos = PirateCaptainMob.this.findEmptySpot(4);
               if (pos != null) {
                  PirateCaptainMob.this.setPos(pos.x, pos.y, true);
                  PirateCaptainMob.this.sendMovementPacket(true);
               }
            }
         }
      });
      this.fireCannonAbility = this.registerAbility(
         new CoordinateMobAbility() {
            @Override
            protected void run(int x, int y) {
               java.awt.geom.Point2D.Float dir = GameMath.normalize(x - PirateCaptainMob.this.getX(), y - PirateCaptainMob.this.getY() - 20);
               float rotation = GameMath.fixAngle(GameMath.getAngle(dir) + 90.0F);
               if (rotation >= 45.0F && rotation < 135.0F) {
                  PirateCaptainMob.this.rightCannonRotation = rotation - 90.0F;
               } else if (rotation >= 135.0F && rotation < 225.0F) {
                  PirateCaptainMob.this.midCannonRotation = rotation - 90.0F;
               } else if (rotation >= 225.0F && rotation < 315.0F) {
                  PirateCaptainMob.this.leftCannonRotation = rotation - 90.0F;
               }

               int range = (int)PirateCaptainMob.this.getDistance(x, y + 20);
               if (PirateCaptainMob.this.isServer()) {
                  float healthPercInv = Math.abs((float)PirateCaptainMob.this.getHealth() / PirateCaptainMob.this.getMaxHealth() - 1.0F);
                  int velocity = 125 + (int)(healthPercInv * 100.0F);
                  Projectile p = ProjectileRegistry.getProjectile(
                     "captaincannonball",
                     PirateCaptainMob.this.getLevel(),
                     PirateCaptainMob.this.getX(),
                     PirateCaptainMob.this.getY() - 20,
                     x,
                     y,
                     velocity,
                     range,
                     new GameDamage(PirateCaptainMob.this.shootDamage),
                     50,
                     PirateCaptainMob.this
                  );
                  p.isSolid = false;
                  PirateCaptainMob.this.getLevel().entityManager.projectiles.add(p);
               } else {
                  SoundManager.playSound(GameResources.explosionLight, SoundEffect.effect(PirateCaptainMob.this).volume(2.0F).pitch(0.9F));

                  for (int i = 0; i < 40; i++) {
                     PirateCaptainMob.this.getLevel()
                        .entityManager
                        .addTopParticle(PirateCaptainMob.this.getX(), PirateCaptainMob.this.getY() - 20, Particle.GType.IMPORTANT_COSMETIC)
                        .movesConstant(
                           PirateCaptainMob.this.dx / 4.0F + dir.x * 20.0F + GameRandom.globalRandom.floatGaussian() * 5.0F,
                           PirateCaptainMob.this.dy / 4.0F + dir.y * 20.0F + GameRandom.globalRandom.floatGaussian() * 5.0F
                        )
                        .smokeColor()
                        .lifeTime(1000);
                  }
               }
            }
         }
      );
   }

   public Point findEmptySpot(int maxTileRange) {
      AreaFinder areaFinder = new AreaFinder(this.getTileX(), this.getTileY(), maxTileRange, true) {
         @Override
         public boolean checkPoint(int x, int y) {
            return PirateCaptainMob.this.getLevel()
               .collides(new Rectangle(PirateCaptainMob.this.getX() - 10, PirateCaptainMob.this.getY() - 7, 20, 14), new CollisionFilter().mobCollision());
         }
      };
      areaFinder.tickFinder(areaFinder.getRemainingTicks());
      if (areaFinder.hasFound()) {
         Point firstFind = areaFinder.getFirstFind();
         return new Point(firstFind.x * 32 + 16, firstFind.y * 32 + 16);
      } else {
         return null;
      }
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addBoolean("dropLadder", this.dropLadder);
      save.addInt("settlerSeed", this.settlerSeed);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.dropLadder = save.getBoolean("dropLadder", true);
      this.setSettlerSeed(save.getInt("settlerSeed", this.settlerSeed, false));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextBoolean(this.inCombat);
      writer.putNextBoolean(this.inSecondStage);
      writer.putNextBoolean(this.inSecondStageTransition);
      if (this.inSecondStageTransition) {
         writer.putNextLong(this.secondStageTransitionStartTime);
      }

      writer.putNextInt(this.settlerSeed);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.inCombat = reader.getNextBoolean();
      this.inSecondStage = reader.getNextBoolean();
      this.inSecondStageTransition = reader.getNextBoolean();
      if (this.inSecondStageTransition) {
         this.secondStageTransitionStartTime = reader.getNextLong();
      }

      this.setSettlerSeed(reader.getNextInt());
      this.updateStageVariables();
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
      this.hasPlayedIntroSound = false;
      this.updateLook();
      this.updateStageVariables();
   }

   public void setSettlerSeed(int seed) {
      if (this.settlerSeed != seed) {
         this.settlerSeed = seed;
         this.updateLook();
      }
   }

   public void updateLook() {
      Mob mob = MobRegistry.getMob("piratehuman", this.getLevel());
      if (mob instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)mob;
         humanMob.setSettlerSeed(this.settlerSeed, false);
         this.look = humanMob.look;
      } else {
         this.look = new HumanLook();
         GameRandom random = new GameRandom(this.settlerSeed);
         this.look.randomizeLook(random, true, HumanGender.MALE, true, true, true, true);
         this.look.setFacialFeature(random.getOneOf(1, 3, 4));
      }

      this.look.setEyeType(7);
   }

   public void markItemSummoned() {
      this.dropLadder = true;
      this.isItemSummoned = true;
      this.inCombat = true;
      this.inSecondStage = true;
      this.inSecondStageTransition = false;
   }

   @Override
   public boolean shouldSave() {
      return super.shouldSave() && !this.isItemSummoned;
   }

   public int getSettlerSeed() {
      return this.settlerSeed;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return collisionDamage;
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return this.inSecondStage && super.canCollisionHit(target);
   }

   @Override
   public int getMaxHealth() {
      return super.getMaxHealth() + (int)((this.scaling == null ? 0 : this.scaling.getHealthIncrease()) * this.getMaxHealthModifier());
   }

   @Override
   public void setupAI() {
      this.ai = new BehaviourTreeAI<>(this, new PirateCaptainMob.PirateCaptainAITree<>(this.shootDistance, 5000, 40, 640, 60000));
   }

   @Override
   public boolean isInCombat() {
      return this.inCombat || super.isInCombat();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.inCombat) {
         if (!this.hasPlayedIntroSound) {
            SoundManager.playSound(GameResources.piratecaptainbegin, SoundEffect.effect(this).volume(1.5F).falloffDistance(4000));
            this.hasPlayedIntroSound = true;
         }

         if (this.isClientPlayerNearby()) {
            SoundManager.setMusic(MusicRegistry.PirateCaptainsLair, SoundManager.MusicPriority.EVENT, 1.5F);
            EventStatusBarManager.registerMobHealthStatusBar(this);
         }

         BossNearbyBuff.applyAround(this);
      } else {
         this.hasPlayedIntroSound = false;
      }

      if (this.inSecondStage && !this.inSecondStageTransition) {
         if (this.secondStageWind == null || this.secondStageWind.isDone()) {
            this.secondStageWindEffect = SoundEffect.effect(this).volume(0.7F).falloffDistance(1400);
            this.secondStageWind = SoundManager.playSound(GameResources.wind1, this.secondStageWindEffect);
         }

         if (this.secondStageWind != null) {
            this.secondStageWind.refreshLooping(1.0F);
            this.secondStageWindEffect.volume(Math.min(1.0F, this.getCurrentSpeed() / 100.0F) * 0.7F);
         }
      }

      if (this.inSecondStageTransition && this.isClient()) {
         Client client = this.getClient();
         if (this.secondStageCameraShake == null) {
            client.startCameraShake(this, this.secondStageTransitionStartTime, secondStageTransitionTime + 500, 40, 3.0F, 3.0F, true);
            this.secondStageCameraShake = new CameraShake(secondStageTransitionTime, secondStageTransitionTime, 40, 1.5F, 1.5F, true);
         }

         if (this.secondStageRumble == null || this.secondStageRumble.isDone()) {
            this.secondStageRumble = SoundManager.playSound(GameResources.rumble, SoundEffect.effect(this).falloffDistance(1400));
         }

         if (this.secondStageRumble != null) {
            this.secondStageRumble.refreshLooping(1.0F);
         }

         this.getSecondStageTransitionProgress();

         for (int i = 0; i < 2; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameRandom.globalRandom.floatGaussian() * 15.0F,
                  this.y + GameRandom.globalRandom.floatGaussian() * 5.0F + 20.0F,
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .movesConstant(GameRandom.globalRandom.floatGaussian() * 6.0F, GameRandom.globalRandom.floatGaussian() * 3.0F)
               .smokeColor()
               .heightMoves(10.0F, GameRandom.globalRandom.getFloatBetween(30.0F, 40.0F))
               .lifeTime(1000);
         }
      } else {
         this.secondStageCameraShake = null;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      if (this.inCombat) {
         BossNearbyBuff.applyAround(this);
      }

      this.getSecondStageTransitionProgress();
      if (this.inCombat && !this.inSecondStage && !this.isAttacking) {
         float healthPerc = (float)this.getHealth() / this.getMaxHealth();
         if (healthPerc <= 0.9F) {
            this.startSecondStageAbility.runAndSend(this.getTime());
         }
      }

      if (!this.inCombat && this.inSecondStage && this.getHealth() == this.getMaxHealth()) {
         this.endSecondStageAbility.runAndSend();
      }
   }

   public void updateStageVariables() {
      if (this.inSecondStage) {
         this.setSpeed(140.0F);
         this.setFriction(0.5F);
         this.collision = new Rectangle(-20, -37, 40, 44);
         this.hitBox = new Rectangle(-25, -42, 50, 54);
         this.selectBox = new Rectangle(-30, -121, 60, 128);
         this.moveAccuracy = 50;
      } else {
         this.setSpeed(15.0F);
         this.setFriction(3.0F);
         this.collision = new Rectangle(-10, -7, 20, 14);
         this.hitBox = new Rectangle(-14, -12, 28, 24);
         this.selectBox = new Rectangle(-14, -41, 28, 48);
         this.moveAccuracy = 5;
      }
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return this.isFlying() ? null : super.getLevelCollisionFilter();
   }

   @Override
   public int getFlyingHeight() {
      if (this.isRiding()) {
         return super.getFlyingHeight();
      } else if (this.inSecondStageTransition) {
         float progress = this.getSecondStageTransitionProgress();
         return (int)(progress * 50.0F);
      } else {
         return this.inSecondStage ? 40 : 0;
      }
   }

   @Override
   public boolean canHitThroughCollision() {
      return this.isFlying();
   }

   public float getSecondStageTransitionProgress() {
      if (this.inSecondStageTransition) {
         long transitionTime = this.getTime() - this.secondStageTransitionStartTime;
         if (transitionTime >= secondStageTransitionTime) {
            this.inSecondStageTransition = false;
            return 1.0F;
         } else {
            return (float)transitionTime / secondStageTransitionTime;
         }
      } else {
         return -1.0F;
      }
   }

   @Override
   public boolean inLiquid(int x, int y) {
      return this.inSecondStage ? false : super.inLiquid(x, y);
   }

   public HumanDrawOptions getHumanDrawOptions(Level level) {
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, this.look, true);
      humanDrawOptions.helmet(new InventoryItem("captainshat"));
      humanDrawOptions.chestplate(new InventoryItem("captainsshirt"));
      humanDrawOptions.boots(new InventoryItem("captainsboots"));
      return humanDrawOptions;
   }

   @Override
   public void addDrawables(
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      java.awt.geom.Point2D.Float shake = this.secondStageCameraShake == null
         ? new java.awt.geom.Point2D.Float()
         : this.secondStageCameraShake.getCurrentShake(this.getTime());
      int drawX = camera.getDrawX(x + shake.x) - 32;
      int drawY = camera.getDrawY(y + shake.y) - 51;
      int dir = this.getDir();
      Point sprite;
      if (this.inSecondStage) {
         this.setDir(2);
         sprite = new Point(0, 2);
      } else {
         sprite = this.getAnimSprite(x, y, dir);
      }

      drawY += this.getBobbing(x, y);
      HumanDrawOptions humanDrawOptions = this.getHumanDrawOptions(level).sprite(sprite).dir(dir).light(light).applyEnemyTracker(this, perspective);
      if (this.inSecondStageTransition) {
         float progress = this.getSecondStageTransitionProgress();
         int height = (int)(progress * 220.0F) - 180;
         final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY - Math.max(height, 0));
         GameTexture maskTexture = MobRegistry.Textures.pirateCaptainShip_mask;
         GameTexture shipTexture = MobRegistry.Textures.pirateCaptainShip;
         drawY -= height;
         TextureDrawOptionsEnd shipBack = shipTexture.initDraw()
            .sprite(0, 0, 192)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective);
         final DrawOptions shipBackOptions = shipBack.addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, 0, -height - 70)).pos(drawX - 64, drawY - 64);
         TextureDrawOptionsEnd shipFront = shipTexture.initDraw()
            .sprite(0, 1, 192)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective);
         final DrawOptions shipFrontOptions = shipFront.addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, 0, -height - 70)).pos(drawX - 64, drawY - 64);
         TextureDrawOptionsEnd leftCannon = shipTexture.initDraw()
            .sprite(0, 12, 32)
            .mirrorX()
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, -18, -height - 70 + 64 + 56));
         final DrawOptions leftCannonOptions = leftCannon.pos(drawX - 2, drawY + 56);
         TextureDrawOptionsEnd midCannon = shipTexture.initDraw()
            .sprite(1, 12, 32)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, 0, -height - 70 + 64 + 62));
         final DrawOptions midCannonOptions = midCannon.pos(drawX + 16, drawY + 62);
         TextureDrawOptionsEnd rightCannon = shipTexture.initDraw()
            .sprite(0, 12, 32)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, 18, -height - 70 + 64 + 56));
         final DrawOptions rightCannonOptions = rightCannon.pos(drawX + 34, drawY + 56);
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               shipBackOptions.draw();
               drawOptions.draw();
               shipFrontOptions.draw();
               leftCannonOptions.draw();
               midCannonOptions.draw();
               rightCannonOptions.draw();
            }
         });
      } else if (this.inSecondStage) {
         float rotate = GameMath.limit(this.dx / 10.0F, -15.0F, 15.0F);
         int anim = GameUtils.getAnim(this.getTime(), 4, 600);
         drawY -= 40;
         GameTexture shipTexture = MobRegistry.Textures.pirateCaptainShip;
         DrawOptions humanOptions = humanDrawOptions.pos(drawX, drawY);
         DrawOptions shipBack = shipTexture.initDraw()
            .sprite(anim, 0, 192)
            .mirror(this.dx < 0.0F, false)
            .rotate(rotate, 96, 120)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX - 64, drawY - 64);
         DrawOptions shipFront = shipTexture.initDraw()
            .sprite(anim, 1, 192)
            .mirror(this.dx < 0.0F, false)
            .rotate(rotate, 96, 120)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX - 64, drawY - 64);
         DrawOptions leftCannon = shipTexture.initDraw()
            .sprite(0, 12, 32)
            .rotate(rotate, 34, 0)
            .addRotation(this.leftCannonRotation - rotate + 45.0F + 180.0F, 16, 16)
            .mirrorX()
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX - 2, drawY + 56);
         DrawOptions midCannon = shipTexture.initDraw()
            .sprite(1, 12, 32)
            .rotate(rotate, 16, -6)
            .addRotation(this.midCannonRotation - rotate - 90.0F, 16, 14)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX + 16, drawY + 62);
         DrawOptions rightCannon = shipTexture.initDraw()
            .sprite(0, 12, 32)
            .rotate(rotate, -2, 0)
            .addRotation(this.rightCannonRotation - rotate - 45.0F, 14, 14)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX + 34, drawY + 56);
         topList.add(tm -> {
            shipBack.draw();
            humanOptions.draw();
            shipFront.draw();
            leftCannon.draw();
            midCannon.draw();
            rightCannon.draw();
         });
      } else {
         MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
         humanDrawOptions.mask(swimMask);
         drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
         float attackProgress = this.getAttackAnimProgress();
         if (this.isAttacking) {
            this.addAttackDraw(humanDrawOptions, attackProgress);
         }

         boolean inLiquid = this.inLiquid(x, y);
         if (inLiquid) {
            humanDrawOptions.armSprite(2);
            humanDrawOptions.mask(MobRegistry.Textures.boat_mask[sprite.y % 4], 0, -7);
         }

         final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
         final DrawOptions boat = inLiquid ? MobRegistry.Textures.steelBoat.initDraw().sprite(0, dir % 4, 64).light(light).pos(drawX, drawY + 7) : null;
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               if (boat != null) {
                  boat.draw();
               }

               drawOptions.draw();
            }
         });
      }

      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 16;
      int drawY = y - 26;
      int dir = this.getDir();
      Point sprite;
      if (this.inSecondStage) {
         this.setDir(2);
         sprite = new Point(0, 2);
      } else {
         sprite = this.getAnimSprite(this.getDrawX(), this.getDrawY(), dir);
      }

      HumanDrawOptions humanOptions = this.getHumanDrawOptions(this.getLevel()).size(32, 32).sprite(sprite).dir(dir);
      if (this.inSecondStage) {
         drawY -= 20;
         int anim = GameUtils.getAnim(this.getTime(), 4, 600);
         GameTexture shipTexture = MobRegistry.Textures.pirateCaptainShip;
         shipTexture.initDraw().sprite(anim, 0, 192).size(96, 96).draw(drawX - 32, drawY - 32);
         humanOptions.draw(drawX, drawY);
         shipTexture.initDraw().sprite(anim, 1, 192).size(96, 96).draw(drawX - 32, drawY - 32);
         shipTexture.initDraw().sprite(0, 12, 32).size(16, 16).mirrorX().draw(drawX - 1, drawY + 28);
         shipTexture.initDraw().sprite(1, 12, 32).size(16, 16).draw(drawX + 8, drawY + 31);
         shipTexture.initDraw().sprite(0, 12, 32).size(16, 16).draw(drawX + 17, drawY + 28);
      } else {
         humanOptions.draw(drawX, drawY);
      }
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      if (this.inSecondStage) {
         GameTexture shadowTexture = MobRegistry.Textures.pirateCaptainShip_shadow;
         int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
         int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2;
         drawY += this.getBobbing(x, y);
         return shadowTexture.initDraw().light(light).pos(drawX, drawY);
      } else {
         return super.getShadowDrawOptions(level, x, y, light, camera);
      }
   }

   @Override
   protected HumanTexture getPirateTexture() {
      return MobRegistry.Textures.pirateCaptain;
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return this.inSecondStage ? new Rectangle(-16, -60, 32, 60) : new Rectangle(-8, -22, 16, 25);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName() + " " + this.getHealth() + "/" + this.getMaxHealth());
   }

   @Override
   public LootTable getLootTable() {
      return this.dropLadder ? new LootTable(lootTable, new LootItem("deepladderdown")) : lootTable;
   }

   @Override
   public LootTable getPrivateLootTable() {
      return privateLootTable;
   }

   @Override
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.piratecaptainhurt, SoundEffect.effect(this).volume(0.3F).pitch(pitch).falloffDistance(1500));
   }

   @Override
   protected void addRangedAttackDraw(HumanDrawOptions drawOptions, float attackProgress) {
      drawOptions.itemAttack(new InventoryItem("handcannon"), null, attackProgress, this.attackDir.x, this.attackDir.y);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.2F));
   }

   @Override
   public void shootAbilityProjectile(int x, int y) {
      if (this.isServer()) {
         GameRandom random = new GameRandom(x + y);
         float prcRange = this.getDistance(x, y) / this.shootDistance;
         int finalX = (int)(x + (random.nextFloat() * 2.0F - 1.0F) * 30.0F * prcRange);
         int finalY = (int)(y + (random.nextFloat() * 2.0F - 1.0F) * 30.0F * prcRange);
         int range = (int)this.getDistance(finalX, finalY);
         Projectile p = ProjectileRegistry.getProjectile(
            "captaincannonball", this.getLevel(), this.x, this.y, finalX, finalY, 150.0F, range, new GameDamage(this.shootDamage), 50, this
         );
         p.isSolid = true;
         p.resetUniqueID(random);
         this.getLevel().entityManager.projectiles.add(p);
      }

      this.showAttack(x, y, false);
      if (this.isClient()) {
         SoundManager.playSound(GameResources.explosionLight, SoundEffect.effect(this).volume(2.0F).pitch(0.9F));
      }
   }

   @Override
   public int getRespawnTime() {
      return BossMob.getBossRespawnTime(this);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      if (this.inSecondStage) {
         for (int i = 0; i < 7; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(this.getLevel(), MobRegistry.Textures.pirateCaptainShip, i, 12, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
                  Particle.GType.IMPORTANT_COSMETIC
               );
         }
      }
   }

   @Override
   public void playDeathSound() {
      SoundManager.playSound(GameResources.piratecaptaindeath, SoundEffect.effect(this).falloffDistance(3000));
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
            c.sendChatMessage(new LocalMessage("misc", "bossdefeat", "name", this.getLocalization()));
            if (c.achievementsLoaded()) {
               c.achievements().DEFEAT_PIRATE.markCompleted(c);
            }
         });
      if (!this.isDamagedByPlayers) {
         AchievementManager.checkMeAndThisArmyKill(this.getLevel(), attackers);
      }

      PirateHumanMob pirateHuman = (PirateHumanMob)MobRegistry.getMob("piratehuman", this.getLevel());
      pirateHuman.buffManager.addBuff(new ActiveBuff(BuffRegistry.INVULNERABLE_ACTIVE, pirateHuman, 2.0F, null), false);
      pirateHuman.setSettlerSeed(this.settlerSeed, true);
      pirateHuman.setTrapped();
      pirateHuman.setDir(this.getDir());
      Point pos = this.findEmptySpot(4);
      if (pos != null) {
         float var10002 = pos.x;
         this.getLevel().entityManager.addMob(pirateHuman, var10002, pos.y);
      } else {
         this.getLevel().entityManager.addMob(pirateHuman, this.x, this.y);
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("piratecap", 4);
   }

   public static class PirateCaptainAITree<T extends PirateCaptainMob> extends PirateAITree<T> {
      public String chaserTargetKey = "chaserTarget";
      public String currentTargetKey = "currentTarget";
      private final ArrayList<Mob> spawnedMobs = new ArrayList<>();
      private long musicCooldown;
      private float minionSpawnBuffer;
      private int dropCombatCounter;

      public PirateCaptainAITree(int shootDistance, int shootCooldown, int meleeDistance, int searchDistance, int wanderFrequency) {
         super(shootDistance, shootCooldown, meleeDistance, searchDistance, wanderFrequency);
         this.addChildFirst(
            new AINode<T>() {
               protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
                  blackboard.onEvent("refreshBossDespawn", event -> PirateCaptainAITree.this.dropCombatCounter = 0);
               }

               public void init(T mob, Blackboard<T> blackboard) {
               }

               public AINodeResult tick(T mob, Blackboard<T> blackboard) {
                  Mob target = blackboard.getObject(Mob.class, PirateCaptainAITree.this.chaserTargetKey);
                  if (target != null) {
                     PirateCaptainAITree.this.dropCombatCounter = 0;

                     for (int i = 0; i < PirateCaptainAITree.this.spawnedMobs.size(); i++) {
                        Mob m = PirateCaptainAITree.this.spawnedMobs.get(i);
                        if (m.removed()) {
                           PirateCaptainAITree.this.spawnedMobs.remove(i);
                           i--;
                        }
                     }

                     long clients = GameUtils.streamServerClients(mob.getLevel()).filter(c -> !c.isDead() && mob.getDistance(c.playerMob) < 1280.0F).count();
                     float spawnsMod = Math.min(1.0F + (float)(clients - 1L) / 2.0F, 5.0F);
                     PirateCaptainAITree.this.minionSpawnBuffer += 0.0125F * spawnsMod;
                     if (PirateCaptainAITree.this.minionSpawnBuffer >= 1.0F) {
                        PirateCaptainAITree.this.minionSpawnBuffer--;
                        if (PirateCaptainAITree.this.spawnedMobs.size() < 5.0F * spawnsMod) {
                           Point spawnTile = EntityManager.getMobSpawnTile(mob.getLevel(), target.getX(), target.getY(), Mob.MOB_SPAWN_AREA, null);
                           if (spawnTile != null) {
                              MobChance randomMob = PirateCaptainMob.pirateMobs
                                 .getRandomMob(mob.getLevel(), null, spawnTile, GameRandom.globalRandom, "piratecaptain");
                              if (randomMob != null) {
                                 randomMob.spawnMob(mob.getLevel(), null, spawnTile, mx -> {
                                    if (mx instanceof PirateMob) {
                                       ((PirateMob)mx).setSummoned();
                                       return true;
                                    } else {
                                       return false;
                                    }
                                 }, mx -> {
                                    if (mob.baseTile == null) {
                                       ((PirateMob)mx).baseTile = null;
                                    } else {
                                       ((PirateMob)mx).baseTile = new Point(mob.baseTile);
                                    }

                                    PirateCaptainAITree.this.spawnedMobs.add(mx);
                                    mx.ai.blackboard.put(PirateCaptainAITree.this.currentTargetKey, target);
                                 }, "piratecaptain");
                              }
                           }
                        }
                     }

                     if (PirateCaptainAITree.this.musicCooldown < mob.getTime()) {
                        PirateCaptainAITree.this.musicCooldown = mob.getTime() + 5000L;
                        mob.setInCombatAbility.runAndSend(true);
                     }
                  } else if (mob.inCombat) {
                     PirateCaptainAITree.this.dropCombatCounter++;
                     if (PirateCaptainAITree.this.dropCombatCounter > 100) {
                        PirateCaptainAITree.this.dropCombatCounter = 0;

                        for (int i = 0; i < PirateCaptainAITree.this.spawnedMobs.size(); i++) {
                           Mob m = PirateCaptainAITree.this.spawnedMobs.get(i);
                           m.remove();
                           PirateCaptainAITree.this.spawnedMobs.remove(i);
                           i--;
                        }

                        if (mob.isItemSummoned) {
                           mob.remove();
                        } else if (PirateCaptainAITree.this.musicCooldown != 0L) {
                           PirateCaptainAITree.this.musicCooldown = 0L;
                           mob.setInCombatAbility.runAndSend(false);
                        }
                     }
                  }

                  return AINodeResult.FAILURE;
               }
            }
         );
         this.addChildFirst(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               if (mob.inSecondStageTransition) {
                  blackboard.mover.stopMoving(mob);
                  return AINodeResult.SUCCESS;
               } else {
                  return AINodeResult.FAILURE;
               }
            }
         });
      }

      @Override
      protected AINode<T> getChaserNode(int shootDistance, int shootCooldown, int meleeDistance, int searchDistance) {
         SequenceAINode<T> sequence = new SequenceAINode<>();
         TargetFinderDistance<T> targetFinder = new TargetFinderDistance<>(searchDistance);
         targetFinder.targetLostAddedDistance = searchDistance * 5;
         sequence.addChild(new TargetFinderAINode<T>(targetFinder) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayers(mob, base, distance);
            }
         });
         sequence.addChild(new PirateCaptainMob.PirateCaptainSecondStageChaserAINode<>());
         SelectorAINode<T> selector = new SelectorAINode<>();
         selector.addChild(new ConditionAINode<>(sequence, m -> m.inSecondStage, AINodeResult.FAILURE));
         selector.addChild(super.getChaserNode(shootDistance, shootCooldown, meleeDistance, searchDistance));
         return selector;
      }
   }

   public static class PirateCaptainSecondStageChaserAINode<T extends PirateCaptainMob> extends AINode<T> {
      public String currentTargetKey = "currentTarget";
      public String chaserTargetKey = "chaserTarget";
      public long nextCannonShot = 0L;

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      }

      public void init(T mob, Blackboard<T> blackboard) {
         blackboard.put(this.chaserTargetKey, null);
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         Mob target = blackboard.getObject(Mob.class, this.currentTargetKey);
         blackboard.put(this.chaserTargetKey, target);
         if (target == null) {
            if (blackboard.mover.isCurrentlyMovingFor(this)) {
               blackboard.mover.stopMoving(mob);
            }

            return AINodeResult.FAILURE;
         } else {
            if (!blackboard.mover.isCurrentlyMovingFor(this) || mob.hasArrivedAtTarget()) {
               int nextAngle = GameRandom.globalRandom.nextInt(360);
               java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(nextAngle);
               blackboard.mover.setCustomMovement(this, new MobMovementLevelPos(target.x + dir.x * 200.0F, target.y + dir.y * 200.0F));
            }

            if (this.nextCannonShot <= mob.getTime()) {
               float healthPerc = (float)mob.getHealth() / mob.getMaxHealth();
               int cooldown = 1500 + (int)(healthPerc * 4000.0F);
               this.nextCannonShot = mob.getTime() + GameRandom.globalRandom.getIntBetween(cooldown, cooldown + 1000);
               LinkedList<Float> firedAngles = new LinkedList<>();
               List<PlayerMob> targets = GameUtils.streamServerClients(mob.getLevel()).map(c -> c.playerMob).filter(t -> {
                  float distance = t.getDistance(mob);
                  return distance > 80.0F && distance <= mob.shootDistance;
               }).collect(Collectors.toList());
               Collections.shuffle(targets);

               for (PlayerMob player : targets) {
                  float angleToTarget = Projectile.getAngleToTarget(mob.x, mob.y, player.x, player.y);
                  if (firedAngles.stream().noneMatch(f -> Math.abs(GameMath.getAngleDifference(angleToTarget, f)) < 60.0F)) {
                     int offset = 35;
                     mob.fireCannonAbility
                        .runAndSend(
                           player.getX() + GameRandom.globalRandom.getIntBetween(-offset, offset),
                           player.getY() + GameRandom.globalRandom.getIntBetween(-offset, offset)
                        );
                     firedAngles.add(angleToTarget);
                  }
               }
            }

            return AINodeResult.SUCCESS;
         }
      }
   }
}
