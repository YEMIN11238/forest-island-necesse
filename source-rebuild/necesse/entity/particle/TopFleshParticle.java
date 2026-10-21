package necesse.entity.particle;

import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TopFleshParticle extends Particle {
   GameTexture texture;
   int spriteX;
   int spriteY;
   int spriteRes;
   int rotation;
   boolean mirrored;
   public float height;
   public float dh;

   public TopFleshParticle(Level level, GameTexture texture, int spriteX, int spriteY, int spriteRes, float x, float y, float knockbackX, float knockbackY) {
      super(level, x, y, 5000L);
      this.texture = texture;
      this.spriteX = spriteX;
      this.spriteY = spriteY;
      this.spriteRes = spriteRes;
      this.x = x;
      this.y = y;
      this.friction = 0.5F;
      this.rotation = GameRandom.globalRandom.nextInt(360);
      this.mirrored = GameRandom.globalRandom.nextBoolean();
      this.dx = (float)GameRandom.globalRandom.nextGaussian() * 20.0F;
      this.dy = (float)GameRandom.globalRandom.nextGaussian() * 20.0F;
      Float normVec = this.getNorm(knockbackX, knockbackY);
      this.dx = this.dx + normVec.x * 50.0F;
      this.dy = this.dy + normVec.y * 50.0F;
      this.hasCollision = false;
      this.collision = new Rectangle(-5, -5, 10, 10);
      this.height = GameRandom.globalRandom.getFloatBetween(10.0F, 20.0F);
      this.dh = GameRandom.globalRandom.getFloatBetween(20.0F, 30.0F);
   }

   public TopFleshParticle(
      Level level, GameTexture texture, int spriteX, int spriteY, int spriteRes, float x, float y, float randomUp, float knockbackX, float knockbackY
   ) {
      this(level, texture, spriteX, spriteY, spriteRes, x, y - GameRandom.globalRandom.nextFloat() * randomUp, knockbackX, knockbackY);
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.height != -1.0F) {
         float heightChange = 30.0F * delta / 250.0F;
         this.dh -= heightChange;
         this.height = this.height + this.dh * delta / 250.0F;
         if (this.height < 0.0F) {
            this.dh = -this.dh * 0.5F;
            this.height = -this.height;
            if (Math.abs(this.dh) < heightChange * 2.0F) {
               this.height = -1.0F;
               this.dh = 0.0F;
            }
         }
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
      float cycle = this.getLifeCyclePercent();
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(this.x) - this.spriteRes / 2;
         int drawY = camera.getDrawY(this.y) - this.spriteRes / 2 + 4 - (int)Math.max(0.0F, this.height);
         float alpha = 1.0F;
         if (cycle > 0.5F) {
            alpha = Math.abs(cycle - 1.0F) * 2.0F;
         }

         TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(this.spriteX, this.spriteY, this.spriteRes)
            .light(light)
            .alpha(alpha)
            .rotate(this.rotation, this.spriteRes / 2, this.spriteRes / 2)
            .mirror(this.mirrored, false)
            .pos(drawX, drawY);
         topList.add(tm -> options.draw());
      }
   }

   public Float getNorm(float x, float y) {
      float tempDist = (float)new Float(0.0F, 0.0F).distance(x, y);
      return tempDist == 0.0F ? new Float(0.0F, 0.0F) : new Float(x / tempDist, y / tempDist);
   }
}
