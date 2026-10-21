package necesse.entity.mobs.hostile.theRunebound;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CroneMob extends HostileMob {
   public CroneMob() {
      super(1000);
      this.attackCooldown = 600;
      this.attackAnimTime = 800;
      this.setSpeed(50.0F);
      this.setFriction(3.0F);
      this.setArmor(10);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
   }

   @Override
   public boolean canDespawn() {
      return false;
   }

   @Override
   public boolean canTakeDamage() {
      return false;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      super.spawnDeathParticles(knockbackX, knockbackY);

      for (int i = 0; i < 20; i++) {
         int lifeTime = GameRandom.globalRandom.getIntBetween(500, 5000);
         float lifePerc = lifeTime / 5000.0F;
         float startHeight = 10.0F;
         float height = startHeight + GameRandom.globalRandom.getIntBetween(70, 150) * lifePerc;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
               this.y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sizeFades(20, 30)
            .movesFriction(GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F), 0.5F)
            .heightMoves(startHeight, height)
            .colorRandom(21.0F, 0.8F, 0.4F, 5.0F, 0.2F, 0.2F)
            .lifeTime(lifeTime);
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
      int dir = 2;
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      HumanDrawOptions humanDrawOptions = new HumanDrawOptions(level, MobRegistry.Textures.crone)
         .sprite(sprite)
         .dir(dir)
         .mask(swimMask)
         .light(light)
         .applyEnemyTracker(this, perspective);
      final DrawOptions drawOptions = humanDrawOptions.pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }
}
