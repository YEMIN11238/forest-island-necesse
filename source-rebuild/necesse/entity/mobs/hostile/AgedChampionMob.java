package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobBeforeHitEvent;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SequenceAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.InverterAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.AgedChampionWaveProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AgedChampionMob extends HostileMob {
   public static LootTable lootTable = new LootTable(
      new LootItem("agedchampionhelmet"), new LootItem("agedchampionchestplate"), new LootItem("agedchampiongreaves"), new LootItem("agedchampionsword")
   );
   private final ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC
   );
   public static GameDamage baseDamage = new GameDamage(65.0F);
   public static GameDamage baseProjectileDamage = new GameDamage(65.0F);
   public static GameDamage incursionDamage = new GameDamage(100.0F);
   public static GameDamage incursionProjectileDamage = new GameDamage(100.0F);
   private final float baseSpeed = 45.0F;
   protected boolean isBlocking;
   protected boolean isChargingMelee;
   protected boolean isChargingRanged;
   protected long blockStartTime;
   protected long meleeStartTime;
   protected long rangedStartTime;
   protected final BooleanMobAbility startBlockingAbility;
   protected final BooleanMobAbility chargeMeleeAbility;
   protected final BooleanMobAbility chargeRangedAbility;
   protected int lookSeed;
   protected HumanLook look = new HumanLook();

   public AgedChampionMob() {
      super(3000);
      this.attackCooldown = 1000;
      this.attackAnimTime = 200;
      this.setSpeed(45.0F);
      this.setFriction(3.0F);
      this.setArmor(20);
      this.setMaxResilience(100);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
      this.updateLook();
      this.startBlockingAbility = this.registerAbility(
         new BooleanMobAbility() {
            @Override
            protected void run(boolean value) {
               AgedChampionMob.this.isBlocking = value;
               if (value) {
                  AgedChampionMob.this.blockStartTime = AgedChampionMob.this.getTime();
                  if (AgedChampionMob.this.isClient()) {
                     GameRandom random = GameRandom.globalRandom;

                     for (int i = 0; i < 20; i++) {
                        AgedChampionMob.this.getLevel()
                           .entityManager
                           .addParticle(AgedChampionMob.this.x, AgedChampionMob.this.y, AgedChampionMob.this.typeSwitcher.next())
                           .sprite(GameResources.magicSparkParticles.sprite(0, 0, 22))
                           .sizeFades(22, 44)
                           .movesFriction(random.getIntBetween(-100, 100), random.getIntBetween(-100, 100), 0.8F)
                           .lifeTime(250);
                     }

                     SoundManager.playSound(GameResources.electricExplosion, SoundEffect.effect(this.getMob()).pitch(2.5F).volume(1.5F));
                  }
               }
            }
         }
      );
      this.chargeMeleeAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            if (value != AgedChampionMob.this.isChargingMelee) {
               AgedChampionMob.this.isChargingMelee = value;
               if (AgedChampionMob.this.isChargingMelee) {
                  AgedChampionMob.this.meleeStartTime = AgedChampionMob.this.getTime();
               }
            }
         }
      });
      this.chargeRangedAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            if (value != AgedChampionMob.this.isChargingRanged) {
               AgedChampionMob.this.isChargingRanged = value;
               if (AgedChampionMob.this.isChargingRanged) {
                  AgedChampionMob.this.rangedStartTime = AgedChampionMob.this.getTime();
               }
            }
         }
      });
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.lookSeed);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.lookSeed = reader.getNextInt();
      this.updateLook();
   }

   @Override
   public void init() {
      super.init();
      this.updateLook();
      if (this.getLevel() instanceof IncursionLevel) {
         this.setMaxHealth(4500);
         this.setHealthHidden(this.getMaxHealth());
         this.setArmor(20);
      }

      this.ai = new BehaviourTreeAI<>(this, new AgedChampionMob.AgedChampionAI<>(256, 64, 640));
   }

   @Override
   protected void doBeforeHitLogic(MobBeforeHitEvent event) {
      Mob attackerMob = event.attacker.getAttackOwner();
      if (attackerMob != null && !this.isAttacking && GameMath.diamondDistance(this.x, this.y, attackerMob.x, attackerMob.y) > 192.0F) {
         this.startBlockingAbility.runAndSend(true);
         event.prevent();
      }

      super.doBeforeHitLogic(event);
   }

   @Override
   public void spawnDamageText(int damage, int size, boolean isCrit) {
      if (!this.isBlocking) {
         super.spawnDamageText(damage, size, isCrit);
      }
   }

   protected GameDamage getMeleeDamage() {
      return this.getLevel() instanceof IncursionLevel ? incursionDamage : baseDamage;
   }

   protected GameDamage getRangedDamage() {
      return this.getLevel() instanceof IncursionLevel ? incursionProjectileDamage : baseProjectileDamage;
   }

   public void updateLook() {
      if (this.lookSeed == 0) {
         this.lookSeed = GameRandom.globalRandom.nextInt();
      }

      GameRandom random = new GameRandom(this.lookSeed);
      this.look.setFacialFeature(0);
      this.look.setSkin(random.getIntBetween(0, 5));
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      if (this.getLevel() != null) {
         return this.buffManager.getModifier(BuffModifiers.CAN_BREAK_OBJECTS)
            ? this.getLevel().regionManager.CAN_BREAK_OBJECTS_OPTIONS
            : this.getLevel().regionManager.CAN_OPEN_DOORS_OPTIONS;
      } else {
         return null;
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("agedchampion", 5);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.setSpeed(this.isBlocking ? 22.5F : 45.0F);
      if (this.isChargingRanged) {
         int particleCount = 5;
         GameRandom random = GameRandom.globalRandom;
         ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
         float anglePerParticle = 360.0F / particleCount;

         for (int j = 0; j < particleCount; j++) {
            int angle = (int)(j * anglePerParticle + random.nextFloat() * anglePerParticle);
            this.getLevel()
               .entityManager
               .addParticle(this, (float)Math.sin(Math.toRadians(angle)) * 15.0F, (float)Math.cos(Math.toRadians(angle)) * 15.0F, typeSwitcher.next())
               .sizeFades(11, 22)
               .color(new Color(208, 204, 50))
               .heightMoves(0.0F, 45.0F)
               .lifeTime(200);
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.isBlocking && this.getTime() - this.blockStartTime > 1000L) {
         this.startBlockingAbility.runAndSend(false);
      }

      this.setSpeed(this.isBlocking ? 22.5F : 45.0F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 50; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         int lifeTime = GameRandom.globalRandom.getIntBetween(2000, 5000);
         float lifePerc = lifeTime / 5000.0F;
         float startHeight = GameRandom.globalRandom.getIntBetween(0, 10);
         float height = startHeight + GameRandom.globalRandom.getIntBetween(70, 150) * lifePerc;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
               this.y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFades(15, 20)
            .movesFriction(GameRandom.globalRandom.getIntBetween(2, 36) * dir.x, GameRandom.globalRandom.getIntBetween(2, 36) * dir.y, 1.0F)
            .heightMoves(startHeight, height)
            .color(this.getDeathParticleColor(GameRandom.globalRandom))
            .lifeTime(lifeTime);
      }
   }

   protected Color getDeathParticleColor(GameRandom random) {
      return new Color(random.getIntBetween(22, 88), random.getIntBetween(22, 33), random.getIntBetween(22, 88));
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
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(x / 32, y / 32);
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      float animProgress = this.getAttackAnimProgress();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += this.getLevel().getTile(x / 32, y / 32).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, this.look, false)
         .sprite(sprite)
         .dir(dir)
         .mask(swimMask)
         .light(light)
         .applyEnemyTracker(this, perspective)
         .helmet(new InventoryItem("agedchampionhelmet"))
         .chestplate(new InventoryItem("agedchampionchestplate"))
         .boots(new InventoryItem("agedchampiongreaves"));
      if (this.isBlocking) {
         humanDrawOptions.holdItem(new InventoryItem("agedchampionshield"));
      } else if (this.isChargingMelee) {
         humanDrawOptions.itemAttack(new InventoryItem("agedchampionsword"), null, 0.0F, dir, dir);
      } else if (this.isAttacking) {
         humanDrawOptions.itemAttack(new InventoryItem("agedchampionsword"), null, animProgress, this.attackDir.x, this.attackDir.y);
      }

      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   public static class AgedChampionAI<T extends AgedChampionMob> extends SequenceAINode<T> {
      public final EscapeAINode<T> escapeAINode;
      public final CooldownAttackTargetAINode<T> atttckTargetNode;
      public final TargetFinderAINode<T> targetFinderNode;
      public final ChaserAINode<T> chaserNode;

      public AgedChampionAI(final int shootDistance, final int meleeDistance, int searchDistance) {
         this.addChild(new InverterAINode<>(this.escapeAINode = new EscapeAINode<T>() {
            public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
               return mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat();
            }
         }));
         if (shootDistance > 0) {
            this.addChild(
               this.atttckTargetNode = new CooldownAttackTargetAINode<T>(CooldownAttackTargetAINode.CooldownTimer.TICK, 2000, shootDistance) {
                  public boolean attackTarget(T mob, Mob target) {
                     if (mob.getDistance(target) > meleeDistance) {
                        mob.chargeMeleeAbility.runAndSend(false);
                     }

                     if (!mob.isBlocking && mob.canAttack()) {
                        mob.chargeRangedAbility.runAndSend(true);
                        if (mob.getTime() - mob.rangedStartTime > 1000L && GameMath.diamondDistance(mob.x, mob.y, target.x, target.y) > shootDistance / 2.0F) {
                           Projectile projectile = new AgedChampionWaveProjectile(
                              mob.getLevel(), mob.x, mob.y, target.x, target.y, 125.0F, 500, mob.getRangedDamage(), mob
                           );
                           projectile.setModifier(new ResilienceOnHitProjectileModifier(10.0F));
                           mob.getLevel().entityManager.projectiles.add(projectile);
                           mob.chargeRangedAbility.runAndSend(false);
                           return true;
                        } else {
                           return false;
                        }
                     } else {
                        mob.chargeRangedAbility.runAndSend(false);
                        return false;
                     }
                  }
               }
            );
            this.atttckTargetNode.attackTimer = this.atttckTargetNode.attackCooldown;
         } else {
            this.atttckTargetNode = null;
         }

         TargetFinderDistance<T> targetFinder = new TargetFinderDistance<>(searchDistance);
         targetFinder.targetLostAddedDistance = searchDistance * 2;
         this.addChild(this.targetFinderNode = new TargetFinderAINode<T>(targetFinder) {
            public GameAreaStream<? extends Mob> streamPossibleTargets(T mob, Point base, TargetFinderDistance<T> distance) {
               return TargetFinderAINode.streamPlayersAndHumans(mob, base, distance);
            }
         });
         this.addChild(this.chaserNode = new ChaserAINode<T>(meleeDistance, false, true) {
            public boolean attackTarget(T mob, Mob target) {
               mob.chargeRangedAbility.runAndSend(false);
               if (!mob.isBlocking && mob.canAttack()) {
                  mob.chargeMeleeAbility.runAndSend(true);
                  if (mob.getTime() - mob.meleeStartTime > 300L) {
                     if (mob.getDistance(target) <= meleeDistance) {
                        mob.attack(target.getX(), target.getY(), false);
                        target.isServerHit(mob.getMeleeDamage(), target.x - mob.x, target.y - mob.y, 125.0F, mob);
                        mob.addResilience(20.0F);
                     }

                     mob.chargeMeleeAbility.runAndSend(false);
                     return true;
                  } else {
                     return false;
                  }
               } else {
                  mob.chargeMeleeAbility.runAndSend(false);
                  return false;
               }
            }
         });
      }
   }
}
