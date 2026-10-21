package necesse.entity.particle;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.Projectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public abstract class ProjectileHitStuckParticle extends Particle {
   protected Mob target;
   protected float xOffset;
   protected float yOffset;
   protected float angle;

   public ProjectileHitStuckParticle(Mob target, Projectile projectile, float x, float y, float insertDistance, long lifeTime) {
      super(projectile.getLevel(), x, y, lifeTime);
      this.target = target;
      this.xOffset = x - (target == null ? 0.0F : target.x);
      this.yOffset = y - (target == null ? 0.0F : target.y);
      Float normalize = GameMath.normalize(projectile.dx, projectile.dy);
      this.xOffset = this.xOffset + normalize.x * insertDistance;
      this.yOffset = this.yOffset + normalize.y * insertDistance;
      this.angle = projectile.getAngle();
   }

   private float levelDrawX() {
      return this.target == null ? this.xOffset : this.target.getDrawX() + this.xOffset;
   }

   private float levelDrawY() {
      return this.target == null ? this.yOffset : this.target.getDrawY() + this.yOffset;
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
      if ((this.target == null || !this.target.removed()) && !this.removed()) {
         this.x = this.levelDrawX();
         this.y = this.levelDrawY();
         this.addDrawables(this.target, this.x, this.y, this.angle, list, tileList, topList, level, tickManager, camera, perspective);
      } else {
         this.remove();
      }
   }

   public abstract void addDrawables(
      Mob var1,
      float var2,
      float var3,
      float var4,
      List<LevelSortedDrawable> var5,
      OrderableDrawables var6,
      OrderableDrawables var7,
      Level var8,
      TickManager var9,
      GameCamera var10,
      PlayerMob var11
   );
}
