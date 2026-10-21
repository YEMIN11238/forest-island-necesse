package necesse.entity.particle;

import java.awt.Rectangle;
import java.util.function.Function;
import necesse.engine.Settings;
import necesse.entity.Entity;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;

public abstract class Particle extends Entity {
   public float dx;
   public float dy;
   public long lifeTime;
   public long spawnTime;
   public float friction;
   public boolean hasCollision;
   public Rectangle collision;

   public Particle(Level level, float x, float y, long lifeTime) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.dx = 0.0F;
      this.dy = 0.0F;
      this.lifeTime = lifeTime;
      this.refreshSpawnTime();
      this.friction = 0.0F;
      this.hasCollision = false;
      this.collision = new Rectangle(0, 0);
   }

   public Particle(Level level, float x, float y, float dx, float dy, long lifeTime) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.dx = dx;
      this.dy = dy;
      this.lifeTime = lifeTime;
      this.refreshSpawnTime();
      this.friction = 0.0F;
      this.hasCollision = false;
      this.collision = new Rectangle(0, 0);
   }

   @Override
   public void init() {
      super.init();
   }

   @Override
   public void clientTick() {
      if (this.lifeTime + this.spawnTime < this.getWorldEntity().getLocalTime()) {
         this.remove();
      }
   }

   @Override
   public void serverTick() {
   }

   public void refreshSpawnTime() {
      this.spawnTime = this.getWorldEntity().getLocalTime();
   }

   public void tickMovement(float delta) {
      if (!this.removed() && (this.dx != 0.0F || this.dy != 0.0F)) {
         this.calcMovement(delta);
         if (this.hasCollision) {
            this.moveX(delta);
            if (this.getLevel().collides(this.getCollision(), new CollisionFilter().projectileCollision())) {
               this.moveX(-delta);
               this.dx = 0.0F;
            }

            this.moveY(delta);
            if (this.getLevel().collides(this.getCollision(), new CollisionFilter().projectileCollision())) {
               this.moveY(-delta);
               this.dy = 0.0F;
            }
         } else {
            this.moveX(delta);
            this.moveY(delta);
         }
      }
   }

   public Rectangle getCollision() {
      return new Rectangle(
         (int)(this.x + this.collision.getX()), (int)(this.y + this.collision.getY()), (int)this.collision.getWidth(), (int)this.collision.getHeight()
      );
   }

   public void calcMovement(float delta) {
      if (this.dx != 0.0F || this.dy != 0.0F) {
         if (this.friction != 0.0F) {
            this.dx = this.dx + (0.0F - this.friction * this.dx) * delta / 250.0F;
            this.dy = this.dy + (0.0F - this.friction * this.dy) * delta / 250.0F;
         }
      }
   }

   public void moveX(float mod) {
      this.x = this.x + this.dx * mod / 250.0F;
   }

   public void moveY(float mod) {
      this.y = this.y + this.dy * mod / 250.0F;
   }

   public float getLifeCyclePercent() {
      float life = (float)this.getLifeCycleTime() / (float)this.lifeTime;
      if (life >= 1.0F) {
         this.remove();
      }

      return Math.min(life, 1.0F);
   }

   public long getLifeCycleTime() {
      return this.getWorldEntity().getLocalTime() - this.spawnTime;
   }

   public long getRemainingLifeTime() {
      return this.lifeTime - this.getLifeCycleTime();
   }

   public enum GType {
      COSMETIC(l -> Settings.particles.ordinal() >= Settings.ParticleSetting.Maximum.ordinal()),
      IMPORTANT_COSMETIC(l -> Settings.particles.ordinal() >= Settings.ParticleSetting.Decreased.ordinal()),
      CRITICAL(l -> true);

      private final Function<Level, Boolean> canAdd;

      GType(Function<Level, Boolean> canAdd) {
         this.canAdd = canAdd;
      }

      public boolean canAdd(Level level) {
         return this.canAdd.apply(level);
      }
   }
}
