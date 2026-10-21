package necesse.entity.mobs.hostile.bosses;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
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
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.MusicRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.Entity;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.mobMovement.MobMovementLevelPos;
import necesse.entity.mobs.mobMovement.MobMovementRelative;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.AncientVultureProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AncientVultureMob extends FlyingBossMob {
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation(
      new LootItem("vulturestaff"), new LootItem("vulturesburst"), new LootItem("vulturestalon")
   );
   public static LootTable lootTable = new LootTable(new ChanceLootItem(0.1F, "vulturemask"), new ChanceLootItem(0.2F, "beatdownvinyl"));
   public static LootTable privateLootTable = new LootTable(uniqueDrops);
   protected MobHealthScaling scaling = new MobHealthScaling(this);
   public LinkedList<Mob> spawnedMobs = new LinkedList<>();
   public static GameDamage collisionDamage = new GameDamage(48.0F);
   public static GameDamage projectileDamage = new GameDamage(44.0F);
   public static GameDamage hatchlingCollision = new GameDamage(40.0F);
   public static GameDamage hatchlingProjectile = new GameDamage(36.0F);
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(5000, 6500, 7500, 8500, 10000);
   public final EmptyMobAbility flickSoundAbility;
   public final EmptyMobAbility popSoundAbility;

   public AncientVultureMob() {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.moveAccuracy = 60;
      this.setSpeed(150.0F);
      this.setArmor(20);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-55, -90, 110, 110);
      this.hitBox = new Rectangle(-55, -90, 110, 110);
      this.selectBox = new Rectangle(-75, -110, 150, 130);
      this.flickSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (AncientVultureMob.this.isClient()) {
               SoundManager.playSound(GameResources.flick, SoundEffect.effect(AncientVultureMob.this).pitch(0.8F).falloffDistance(1400));
            }
         }
      });
      this.popSoundAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            if (AncientVultureMob.this.isClient()) {
               SoundManager.playSound(GameResources.pop, SoundEffect.effect(AncientVultureMob.this).volume(0.8F).pitch(0.5F).falloffDistance(2000));
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
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new AncientVultureMob.AncientVultureAI<>());
      if (this.isClient()) {
         SoundManager.playSound(GameResources.ancientvulturebegin, SoundEffect.effect(this).falloffDistance(4000));
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
   protected SoundSettings getHurtSound() {
      return new SoundSettings(GameResources.ancientvulturehurt).volume(0.4F).fallOffDistance(1500);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.ancientvulturedeath).fallOffDistance(3000);
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
   public void setFacingDir(float deltaX, float deltaY) {
      if (deltaX < 0.0F) {
         this.setDir(0);
      } else if (deltaX > 0.0F) {
         this.setDir(1);
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClientPlayerNearby()) {
         SoundManager.setMusic(MusicRegistry.AncientVulturesFeast, SoundManager.MusicPriority.EVENT, 1.5F);
         EventStatusBarManager.registerMobHealthStatusBar(this);
      }

      BossNearbyBuff.applyAround(this);
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(130.0F + healthPercInv * 90.0F);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.scaling.serverTick();
      BossNearbyBuff.applyAround(this);
      float healthPercInv = Math.abs((float)this.getHealth() / this.getMaxHealth() - 1.0F);
      this.setSpeed(130.0F + healthPercInv * 90.0F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 5; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.ancientVulture, 4 + i, 10, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 160;
      int drawY = camera.getDrawY(y) - 200;
      int dir = this.getDir();
      long time = level.getWorldEntity().getTime() % 350L;
      int sprite;
      if (time < 100L) {
         sprite = 0;
      } else if (time < 200L) {
         sprite = 1;
      } else if (time < 300L) {
         sprite = 2;
      } else {
         sprite = 3;
      }

      float rotate = Math.min(30.0F, this.dx / 5.0F);
      DrawOptions options = MobRegistry.Textures.ancientVulture
         .initDraw()
         .sprite(sprite, 0, 320)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .mirror(dir == 0, false)
         .rotate(rotate, 160, 100)
         .pos(drawX, drawY);
      TextureDrawOptions shadowOptions = this.getShadowDrawOptions(level, x, y, light, camera);
      topList.add(tm -> {
         shadowOptions.draw();
         options.draw();
      });
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.ancientVulture_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2 + 10;
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      super.drawOnMap(tickManager, client, x, y, tileScale, drawBounds, isMinimap);
      int drawX = x - 32;
      int drawY = y - 16;
      int dir = this.getDir();
      MobRegistry.Textures.ancientVulture.initDraw().sprite(0, 4, 96, 96).size(48, 48).mirror(dir == 0, false).draw(drawX, drawY);
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-21, -16, 40, 32);
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

   public class AncientVultureAI<T extends AncientVultureMob> extends AINode<T> {
      private int removeCounter;
      private Mob currentTarget;
      private long findNewTargetTime;
      private final ArrayList<AncientVultureMob.AncientVultureAI<T>.AttackRotation> attackRotations = new ArrayList<>();
      private int currentRotation;

      public AncientVultureAI() {
         this.attackRotations.add(new AncientVultureMob.AncientVultureAI.FlyAroundAttackRotation());
         this.attackRotations.add(new AncientVultureMob.AncientVultureAI.SimpleProjectileAttackRotation());
         this.attackRotations.add(new AncientVultureMob.AncientVultureAI.PlopEggsAttackRotation());
         this.currentRotation = 0;
      }

      protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
         blackboard.onEvent("refreshBossDespawn", event -> this.removeCounter = 0);
         if (mob.isServer()) {
            this.attackRotations.get(this.currentRotation).start();
         }

         blackboard.onRemoved(e -> AncientVultureMob.this.spawnedMobs.forEach(Mob::remove));
      }

      public void init(T mob, Blackboard<T> blackboard) {
      }

      public AINodeResult tick(T mob, Blackboard<T> blackboard) {
         this.tickTargets();
         if (this.currentTarget != null) {
            this.removeCounter = 0;
            if (this.attackRotations.get(this.currentRotation).isOver()) {
               this.currentRotation = (this.currentRotation + 1) % this.attackRotations.size();
               this.attackRotations.get(this.currentRotation).start();
            }

            this.attackRotations.get(this.currentRotation).tick();
         } else {
            AncientVultureMob.this.stopMoving();
            this.removeCounter++;
            if (this.removeCounter > 100) {
               AncientVultureMob.this.remove();
            }
         }

         return AINodeResult.SUCCESS;
      }

      public void tickTargets() {
         if (this.currentTarget != null) {
            if (this.currentTarget.isSamePlace(this.mob()) && !this.currentTarget.removed()) {
               if (AncientVultureMob.this.getWorldEntity().getTime() <= this.findNewTargetTime) {
                  this.refreshTarget();
               }
            } else {
               this.refreshTarget();
            }
         } else {
            this.refreshTarget();
         }
      }

      public void refreshTarget() {
         this.currentTarget = GameUtils.streamServerClients(AncientVultureMob.this.getLevel())
            .map(c -> c.playerMob)
            .min(Comparator.comparingInt(player -> (int)AncientVultureMob.this.getDistance(player)))
            .orElse(null);
         this.findNewTargetTime = AncientVultureMob.this.getWorldEntity().getTime() + 5000L;
      }

      private abstract class AttackRotation {
         private AttackRotation() {
         }

         public abstract void start();

         public abstract void tick();

         public abstract boolean isOver();
      }

      private class FlyAroundAttackRotation extends AncientVultureMob.AncientVultureAI<T>.AttackRotation {
         private final Point[] positionRotation = new Point[]{new Point(260, 260), new Point(-260, -180), new Point(260, -260), new Point(-260, 180)};
         private int currentPoint;
         private int timer;

         private FlyAroundAttackRotation() {
         }

         @Override
         public void start() {
            this.timer = 0;
         }

         @Override
         public void tick() {
            this.timer++;
            if (AncientVultureMob.this.hasArrivedAtTarget()) {
               this.currentPoint = (this.currentPoint + 1) % this.positionRotation.length;
            }

            Point point = this.positionRotation[this.currentPoint];
            AncientVultureMob.this.setMovement(new MobMovementRelative(AncientVultureAI.this.currentTarget, point.x, point.y));
         }

         @Override
         public boolean isOver() {
            float healthPerc = (float)AncientVultureMob.this.getHealth() / AncientVultureMob.this.getMaxHealth();
            return this.timer > 20.0F * (5.0F + healthPerc * 10.0F);
         }
      }

      private class PlopEggsAttackRotation extends AncientVultureMob.AncientVultureAI<T>.AttackRotation {
         private int timer;
         private float buffer;

         private PlopEggsAttackRotation() {
         }

         @Override
         public void start() {
            this.timer = 0;
            this.buffer = 0.0F;
            this.findNewPosition();
         }

         @Override
         public void tick() {
            this.timer++;
            float healthPerc = (float)AncientVultureMob.this.getHealth() / AncientVultureMob.this.getMaxHealth();
            int secs;
            if (healthPerc < 0.08F) {
               secs = 1;
            } else if (healthPerc < 0.3F) {
               secs = 2;
            } else if (healthPerc < 0.5F) {
               secs = 3;
            } else if (healthPerc < 0.7F) {
               secs = 4;
            } else {
               secs = 5;
            }

            long clients = GameUtils.streamServerClients(AncientVultureMob.this.getLevel())
               .filter(c -> !c.isDead() && AncientVultureMob.this.getDistance(c.playerMob) < 1280.0F)
               .count();
            float spawnsMod = Math.min(1.0F + (float)(clients - 1L) / 2.0F, 4.0F);
            this.buffer += 1.0F / secs / 20.0F * spawnsMod;
            if (this.buffer > 1.0F) {
               this.buffer--;
               AncientVultureMob.this.popSoundAbility.runAndSend();
               AncientVultureEggMob mob = new AncientVultureEggMob(AncientVultureMob.this);
               AncientVultureMob.this.getLevel().entityManager.addMob(mob, AncientVultureMob.this.getX(), AncientVultureMob.this.getY());
               AncientVultureMob.this.spawnedMobs.removeIf(Entity::removed);
               AncientVultureMob.this.spawnedMobs.add(mob);
            }

            if (AncientVultureMob.this.hasArrivedAtTarget()) {
               this.findNewPosition();
            }
         }

         @Override
         public boolean isOver() {
            float healthPerc = (float)AncientVultureMob.this.getHealth() / AncientVultureMob.this.getMaxHealth();
            return this.timer > 20.0F * (7.0F + healthPerc * 13.0F);
         }

         private void findNewPosition() {
            if (AncientVultureAI.this.currentTarget == null) {
               AncientVultureMob.this.setMovement(new MobMovementLevelPos(AncientVultureMob.this.x, AncientVultureMob.this.y));
            } else {
               float angle = GameRandom.globalRandom.nextInt(360);
               float nx = (float)Math.cos(Math.toRadians(angle));
               float ny = (float)Math.sin(Math.toRadians(angle));
               float distance = 300.0F;
               AncientVultureMob.this.setMovement(new MobMovementRelative(AncientVultureAI.this.currentTarget, nx * distance, ny * distance));
            }
         }
      }

      private class SimpleProjectileAttackRotation extends AncientVultureMob.AncientVultureAI<T>.AttackRotation {
         private int timer;

         private SimpleProjectileAttackRotation() {
         }

         @Override
         public void start() {
            this.timer = 0;
         }

         @Override
         public void tick() {
            this.timer++;
            AncientVultureMob.this.setMovement(new MobMovementRelative(AncientVultureAI.this.currentTarget, 0.0F, -200.0F));
            float healthPerc = (float)AncientVultureMob.this.getHealth() / AncientVultureMob.this.getMaxHealth();
            int divider;
            if (healthPerc < 0.15F) {
               divider = 4;
            } else if (healthPerc < 0.4F) {
               divider = 3;
            } else if (healthPerc < 0.7F) {
               divider = 2;
            } else {
               divider = 1;
            }

            if (this.timer % (20 / divider) == 0) {
               AncientVultureMob.this.flickSoundAbility.runAndSend();
               AncientVultureMob.this.getLevel()
                  .entityManager
                  .projectiles
                  .add(
                     new AncientVultureProjectile(
                        AncientVultureAI.this.mob(),
                        AncientVultureMob.this.getX(),
                        AncientVultureMob.this.getY(),
                        AncientVultureAI.this.currentTarget.getX(),
                        AncientVultureAI.this.currentTarget.getY(),
                        AncientVultureMob.projectileDamage
                     )
                  );
            }
         }

         @Override
         public boolean isOver() {
            float healthPerc = (float)AncientVultureMob.this.getHealth() / AncientVultureMob.this.getMaxHealth();
            return this.timer > 20.0F * (3.0F + healthPerc * 7.0F);
         }
      }
   }
}
