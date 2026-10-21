package necesse.entity.particle;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SmokeDirectionParticle extends Particle {
   float angle;
   public Color col;

   public SmokeDirectionParticle(Level level, float x, float y, float targetX, float targetY, float speed) {
      super(level, x, y, 1000L);
      this.setTarget(targetX, targetY);
      this.dx *= speed;
      this.dy *= speed;
      this.friction = 1.0F;
      this.col = new Color(1.0F, 1.0F, 1.0F, 1.0F);
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
      float life = this.getLifeCyclePercent();
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = this.getX() - camera.getX() - 16;
         int drawY = this.getY() - camera.getY() - 16 - 16;
         int spriteY = life < 0.5F ? 0 : 1;
         int spriteX = 4;
         if (life < 0.1F) {
            spriteX = 0;
         } else if (life < 0.2F) {
            spriteX = 1;
         } else if (life < 0.3F) {
            spriteX = 2;
         } else if (life < 0.4F) {
            spriteX = 3;
         } else if (life < 0.5F) {
            spriteX = 4;
         } else if (life < 0.6F) {
            spriteX = 0;
         } else if (life < 0.7F) {
            spriteX = 1;
         } else if (life < 0.8F) {
            spriteX = 2;
         } else if (life < 0.9F) {
            spriteX = 3;
         }

         final TextureDrawOptions options = GameResources.smokeParticles
            .initDraw()
            .sprite(spriteX, spriteY + 4, 64)
            .colorLight(this.col, light)
            .rotate(this.getAngle() - 90.0F, 16, 16)
            .size(32, 32)
            .pos(drawX, drawY);
         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }

   public void moveDist(float dist) {
      Float dir = this.getDir(this.dx, this.dy);
      this.x = this.x + dir.x * dist;
      this.y = this.y + dir.y * dist;
   }

   public void setTarget(float x, float y) {
      Float point = this.getDir(x - this.x, y - this.y);
      this.dx = point.x;
      this.dy = point.y;
      this.angle = (float)Math.toDegrees(Math.atan(this.dy / this.dx));
      if (this.dx < 0.0F) {
         this.angle += 270.0F;
      } else {
         this.angle += 90.0F;
      }
   }

   public Float getDir(float dx, float dy) {
      Float tempPoint = new Float(dx, dy);
      float dist = (float)tempPoint.distance(0.0, 0.0);
      float normX = tempPoint.x / dist;
      float normY = tempPoint.y / dist;
      return new Float(normX, normY);
   }

   public float getAngle() {
      return this.angle % 360.0F;
   }
}
