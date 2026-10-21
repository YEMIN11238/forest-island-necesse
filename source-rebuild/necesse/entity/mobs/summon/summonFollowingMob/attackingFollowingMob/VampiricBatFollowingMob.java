package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFollowerChaserAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.projectile.Projectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class VampiricBatFollowingMob extends AttackingFollowingMob {
   public VampiricBatFollowingMob() {
      super(10);
      this.setSpeed(50.0F);
      this.setFriction(2.0F);
      this.moveAccuracy = 30;
      this.attackCooldown = 700;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 40);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new PlayerFollowerChaserAI<VampiricBatFollowingMob>(576, 448, false, false, 640, 64) {
            public boolean attackTarget(VampiricBatFollowingMob mob, Mob target) {
               if (mob.canAttack()) {
                  mob.attack(target.getX(), target.getY(), false);
                  Projectile projectile = ProjectileRegistry.getProjectile(
                     "vampiricbatbolt", mob.getLevel(), mob.x, mob.y, target.x, target.y, 80.0F, 768, VampiricBatFollowingMob.this.summonDamage, mob
                  );
                  mob.getLevel().entityManager.projectiles.add(projectile);
                  return true;
               } else {
                  return false;
               }
            }
         }
      );
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.vampiricBat, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public int getFlyingHeight() {
      return 20;
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 55;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      float bobbing = GameUtils.getBobbing(this.getWorldEntity().getTime(), 1000) * 5.0F;
      drawY += (int)bobbing;
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final TextureDrawOptionsEnd drawOptions = MobRegistry.Textures.vampiricBat
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      return new Point(GameUtils.getAnim(this.getWorldEntity().getTime(), 4, 300), dir);
   }
}
