package necesse.entity.particle;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AscendedCageBombParticle extends Particle {
   ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
   private final GameTexture animTexture = GameTexture.fromFile("projectiles/ascendedbomb");
   private final GameTexture shadowTexture = GameTexture.fromFile("projectiles/ascendedbomb_shadow");

   public AscendedCageBombParticle(Level level, float x, float y, long lifeTime) {
      super(level, x, y, lifeTime);
   }

   @Override
   public void init() {
   }

   @Override
   public void clientTick() {
      super.clientTick();
      GameRandom random = GameRandom.globalRandom;

      for (int i = 0; i < 10; i++) {
         int angle = random.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         float range = random.getFloatBetween(25.0F, 75.0F);
         float startX = this.x + dir.x * range;
         float startY = this.y;
         float endHeight = 0.0F;
         float startHeight = endHeight + dir.y * range;
         int lifeTime = random.getIntBetween(100, 300);
         float speed = dir.x * range * 250.0F / lifeTime;
         this.getLevel()
            .entityManager
            .addTopParticle(startX, startY - this.getHeightProgress(), this.typeSwitcher.next())
            .sprite(GameResources.ascendedParticle.sprite(random.nextInt(5), 0, 20))
            .sizeFades(20, 40)
            .rotates()
            .movesConstant(-speed, 0.0F)
            .heightMoves(startHeight, endHeight)
            .fadesAlphaTime(100, 50)
            .ignoreLight(true)
            .lifeTime(lifeTime);
      }

      this.getLevel()
         .entityManager
         .addParticle(
            new AscendedSparkleParticle(
               this.getLevel(), this.x + random.getIntBetween(-32, 32), this.y - this.getHeightProgress() + random.getIntBetween(-32, 32), 500L
            ),
            this.typeSwitcher.next()
         );
   }

   private float getLifePercentage() {
      return (float)this.getRemainingLifeTime() / (float)this.lifeTime;
   }

   private float getHeightProgress() {
      return this.getLifePercentage() * this.getLifePercentage() * 600.0F;
   }

   private float getAlpha() {
      return Math.min(1.0F, 1.0F - this.getLifePercentage() * this.getLifePercentage());
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int centerDistance = this.animTexture.getHeight() / 2;
         int drawX = (int)(camera.getDrawX(this.x) - centerDistance + GameRandom.globalRandom.getIntBetween(-5, 5) * (1.0F - this.getLifePercentage()));
         int drawY = (int)(camera.getDrawY(this.y) - centerDistance + GameRandom.globalRandom.getIntBetween(-5, 5) * (1.0F - this.getLifePercentage()));
         int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 4, 400);
         final TextureDrawOptions options = this.animTexture
            .initDraw()
            .sprite(anim, 0, 36)
            .alpha(this.getAlpha())
            .light(light.minLevelCopy(150.0F))
            .pos(drawX, (int)(drawY - centerDistance - this.getHeightProgress()));
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
         TextureDrawOptions shadowOptions = this.shadowTexture.initDraw().alpha(this.getAlpha()).light(light.minLevelCopy(150.0F)).pos(drawX - 2, drawY - 2);
         tileList.add(tm -> shadowOptions.draw());
      }
   }

   public void despawnNow() {
      if (this.getRemainingLifeTime() > 250L) {
         this.lifeTime = 250L;
         this.spawnTime = this.getWorldEntity().getLocalTime();
      }
   }
}
