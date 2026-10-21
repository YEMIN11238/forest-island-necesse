package necesse.entity.mobs.hostile.bosses;

import java.awt.Rectangle;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionShooterPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.VultureHatchlingProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class VultureHatchling extends FlyingBossMob {
   public static LootTable lootTable = new LootTable();
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(50, 80, 100, 120, 160);
   private final AncientVultureMob owner;

   public VultureHatchling() {
      this(null);
   }

   public VultureHatchling(AncientVultureMob owner) {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.isSummoned = true;
      this.owner = owner;
      this.moveAccuracy = 10;
      this.setSpeed(60.0F);
      this.setArmor(20);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.3F);
      this.collision = new Rectangle(-18, -15, 36, 30);
      this.hitBox = new Rectangle(-18, -15, 36, 36);
      this.selectBox = new Rectangle(-20, -34, 40, 36);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new CollisionShooterPlayerChaserWandererAI<VultureHatchling>(
            null, 1600, AncientVultureMob.hatchlingCollision, 100, CooldownAttackTargetAINode.CooldownTimer.TICK, 2000, 800, 40000
         ) {
            public boolean shootAtTarget(VultureHatchling mob, Mob target) {
               if (VultureHatchling.this.canAttack() && mob.getDistance(target) > 96.0F) {
                  VultureHatchling.this.getLevel()
                     .entityManager
                     .projectiles
                     .add(new VultureHatchlingProjectile(mob, mob.getX(), mob.getY(), target.getX(), target.getY(), AncientVultureMob.hatchlingProjectile));
                  return true;
               } else {
                  return false;
               }
            }
         },
         new FlyingAIMover()
      );
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
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 5; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.vultureHatchling,
                  GameRandom.globalRandom.nextInt(4),
                  2,
                  32,
                  this.x,
                  this.y,
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32 - 16;
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

      float rotate = this.dx / 10.0F;
      DrawOptions options = MobRegistry.Textures.vultureHatchling
         .initDraw()
         .sprite(sprite, 0, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .mirror(dir == 0, false)
         .rotate(rotate, 32, 32)
         .pos(drawX, drawY);
      topList.add(tm -> options.draw());
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.vultureHatchling_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2 + 13;
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }
}
