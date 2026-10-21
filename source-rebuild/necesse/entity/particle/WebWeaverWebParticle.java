package necesse.entity.particle;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class WebWeaverWebParticle extends Particle {
   private final long startupDelay;
   private final long startTime;
   public boolean startupDone = false;
   private int currentIndex = 0;
   protected ParticleTypeSwitcher extraParticlesTypes = new ParticleTypeSwitcher(Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

   public WebWeaverWebParticle(Level level, float x, float y, long lifeTime, long startupDelay) {
      super(level, x, y, lifeTime);
      this.startTime = this.getLevel().getWorldEntity().getLocalTime();
      this.startupDelay = startupDelay;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.startupDone) {
         if (this.startupDelay + this.startTime < this.getLevel().getWorldEntity().getLocalTime()) {
            this.startupDone = true;
            return;
         }

         this.currentIndex = (int)((this.getLevel().getWorldEntity().getLocalTime() - this.startTime) / (this.startupDelay / 8L));
      }

      this.spawnParticles();
   }

   private void spawnParticles() {
      long remainingLifeTime = this.getRemainingLifeTime();
      int lifetime = remainingLifeTime < 500L ? (int)remainingLifeTime : 1000;
      int particles;
      float rotationSpeed;
      float distance;
      Color color;
      if (this.startupDone) {
         particles = 1;
         rotationSpeed = 0.0F;
         color = new Color(255, 246, 79);
         distance = GameRandom.globalRandom.nextInt(115);
      } else {
         particles = 10;
         rotationSpeed = 15.0F;
         color = new Color(166, 204, 52);
         distance = 16 * this.currentIndex;
      }

      for (int i = 0; i < particles; i++) {
         AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameMath.sin(currentAngle.get()) * distance, this.y + GameMath.cos(currentAngle.get()) * distance, this.extraParticlesTypes.next()
            )
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(3), 0, 22))
            .color(color)
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               float angle = currentAngle.accumulateAndGet(delta * rotationSpeed / 250.0F, Float::sum);
               float distY = distance * 0.75F;
               pos.x = this.x + GameMath.sin(angle) * distance;
               pos.y = this.y + GameMath.cos(angle) * distY;
            })
            .givesLight(75.0F, 0.5F)
            .lifeTime(lifetime)
            .sizeFades(16, 24);
      }
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
      int drawX = camera.getDrawX(this.getX()) - 116;
      int drawY = camera.getDrawY(this.getY()) - 88;
      long remainingLifeTime = this.getRemainingLifeTime();
      float alpha = 1.0F;
      if (remainingLifeTime < 500L) {
         alpha = Math.max(0.0F, (float)remainingLifeTime / 500.0F);
      }

      DrawOptions options;
      if (this.startupDone) {
         options = GameResources.spideriteStaffWeb.initDraw().sprite(0, 0, 232, 176).alpha(alpha).pos(drawX, drawY);
      } else {
         options = GameResources.spideriteStaffWeb.initDraw().sprite(1 + this.currentIndex, 0, 232, 176).alpha(alpha).pos(drawX, drawY);
      }

      if (options != null) {
         tileList.add(tm -> options.draw());
      }
   }

   public void despawnNow() {
      if (this.getRemainingLifeTime() > 500L) {
         this.lifeTime = 500L;
         this.spawnTime = this.getWorldEntity().getLocalTime();
      }
   }
}
