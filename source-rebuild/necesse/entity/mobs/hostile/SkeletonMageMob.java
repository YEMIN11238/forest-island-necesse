package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.CoordinateMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.decorators.FailerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TeleportOnProjectileHitAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.particle.SmokePuffParticle;
import necesse.entity.projectile.SkeletonMageProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SkeletonMageMob extends HostileMob {
   public static LootTable lootTable = new LootTable(LootItem.between("bone", 1, 3));
   public static GameDamage baseDamage = new GameDamage(50.0F);
   public static GameDamage incursionDamage = new GameDamage(70.0F);
   public final CoordinateMobAbility teleportAbility;

   public SkeletonMageMob() {
      super(150);
      this.attackAnimTime = 200;
      this.setSpeed(40.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.4F);
      this.setArmor(20);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
      this.teleportAbility = this.registerAbility(
         new CoordinateMobAbility() {
            @Override
            protected void run(int x, int y) {
               if (SkeletonMageMob.this.isClient()) {
                  SkeletonMageMob.this.getLevel()
                     .entityManager
                     .addParticle(
                        new SmokePuffParticle(SkeletonMageMob.this.getLevel(), SkeletonMageMob.this.x, SkeletonMageMob.this.y, new Color(217, 202, 46)),
                        Particle.GType.CRITICAL
                     );
                  SkeletonMageMob.this.getLevel()
                     .entityManager
                     .addParticle(new SmokePuffParticle(SkeletonMageMob.this.getLevel(), x, y, new Color(217, 202, 46)), Particle.GType.CRITICAL);
               }

               SkeletonMageMob.this.setPos(x, y, true);
            }
         }
      );
   }

   @Override
   public void init() {
      super.init();
      final GameDamage damage;
      if (this.getLevel() instanceof IncursionLevel) {
         this.setMaxHealth(400);
         this.setHealthHidden(this.getMaxHealth());
         this.setArmor(30);
         damage = incursionDamage;
      } else {
         damage = baseDamage;
      }

      ConfusedPlayerChaserWandererAI<SkeletonMageMob> chaserAI = new ConfusedPlayerChaserWandererAI<SkeletonMageMob>(null, 512, 320, 40000, false, false) {
         public boolean attackTarget(SkeletonMageMob mob, Mob target) {
            if (mob.canAttack()) {
               mob.attack(target.getX(), target.getY(), false);
               mob.getLevel()
                  .entityManager
                  .projectiles
                  .add(new SkeletonMageProjectile(mob.getLevel(), mob, mob.x, mob.y, target.x, target.y, 90.0F, 640, damage, 50));
               this.wanderAfterAttack = GameRandom.globalRandom.getChance(0.75F);
               return true;
            } else {
               return false;
            }
         }
      };
      chaserAI.addChildFirst(new FailerAINode<>(new TeleportOnProjectileHitAINode<SkeletonMageMob>(3000, 7) {
         public boolean teleport(SkeletonMageMob mob, int x, int y) {
            if (mob.isServer()) {
               mob.teleportAbility.runAndSend(x, y);
               this.getBlackboard().mover.stopMoving(mob);
            }

            return true;
         }
      }));
      this.ai = new BehaviourTreeAI<>(this, chaserAI);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("skeleton", 3);
   }

   @Override
   public void playHitSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.crack, SoundEffect.effect(this).volume(1.6F).pitch(pitch));
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.crackdeath).volume(0.8F);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.skeletonMage.body,
                  GameRandom.globalRandom.nextInt(5),
                  8,
                  32,
                  this.x,
                  this.y,
                  20.0F,
                  knockbackX,
                  knockbackY
               ),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.skeletonMage)
         .sprite(sprite)
         .dir(dir)
         .mask(swimMask)
         .light(light)
         .applyEnemyTracker(this, perspective);
      float animProgress = this.getAttackAnimProgress();
      if (this.isAttacking) {
         ItemAttackDrawOptions attackOptions = ItemAttackDrawOptions.start(dir)
            .itemSprite(MobRegistry.Textures.skeletonMage.body, 0, 9, 32)
            .itemRotatePoint(4, 4)
            .itemEnd()
            .armSprite(MobRegistry.Textures.skeletonMage.body, 0, 8, 32)
            .swingRotation(animProgress);
         humanDrawOptions.attackAnim(attackOptions, animProgress);
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

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public void showAttack(int x, int y, int seed, boolean showAllDirections) {
      super.showAttack(x, y, seed, showAllDirections);
      if (this.isClient()) {
         SoundManager.playSound(GameResources.swing2, SoundEffect.effect(this).volume(0.7F).pitch(1.2F));
      }
   }
}
