package necesse.entity.particle;

import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameSkin;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FleshParticle extends Particle {
   public GameTextureSection sprite;
   public float rotation;
   public boolean mirrored;
   public float height;
   public float dh;
   public GameTexture recolorTexture;
   public int recolorYIndex;

   public FleshParticle(Level level, GameTextureSection sprite, float x, float y, float knockbackX, float knockbackY) {
      super(level, x, y, 5000L);
      this.sprite = sprite;
      this.x = x;
      this.y = y;
      this.friction = 0.5F;
      this.rotation = GameRandom.globalRandom.nextInt(360);
      this.mirrored = GameRandom.globalRandom.nextBoolean();
      this.dx = (float)GameRandom.globalRandom.nextGaussian() * 20.0F;
      this.dy = (float)GameRandom.globalRandom.nextGaussian() * 20.0F;
      Float normVec = GameMath.normalize(knockbackX, knockbackY);
      this.dx = this.dx + normVec.x * 50.0F;
      this.dy = this.dy + normVec.y * 50.0F;
      this.hasCollision = true;
      this.collision = new Rectangle(-5, -5, 10, 10);
      this.height = GameRandom.globalRandom.getFloatBetween(10.0F, 20.0F);
      this.dh = GameRandom.globalRandom.getFloatBetween(20.0F, 30.0F);
   }

   public FleshParticle(Level level, GameTextureSection sprite, float x, float y, float randomUp, float knockbackX, float knockbackY) {
      this(level, sprite, x, y - GameRandom.globalRandom.nextFloat() * randomUp, knockbackX, knockbackY);
   }

   public FleshParticle(Level level, GameTexture texture, int spriteX, int spriteY, int spriteRes, float x, float y, float knockbackX, float knockbackY) {
      this(level, new GameTextureSection(texture).sprite(spriteX, spriteY, spriteRes), x, y, knockbackX, knockbackY);
   }

   public FleshParticle(
      Level level, GameTexture texture, int spriteX, int spriteY, int spriteWidth, int spriteHeight, float x, float y, float knockbackX, float knockbackY
   ) {
      this(level, new GameTextureSection(texture).sprite(spriteX, spriteY, spriteWidth, spriteHeight), x, y, knockbackX, knockbackY);
   }

   public FleshParticle(
      Level level, GameTexture texture, int spriteX, int spriteY, int spriteRes, float x, float y, float randomUp, float knockbackX, float knockbackY
   ) {
      this(level, texture, spriteX, spriteY, spriteRes, x, y - GameRandom.globalRandom.nextFloat() * randomUp, knockbackX, knockbackY);
   }

   public FleshParticle(
      Level level, GameSkin skin, int spriteX, int spriteY, int spriteRes, float x, float y, float randomUp, float knockbackX, float knockbackY
   ) {
      this(level, skin.getBodyTexture(), spriteX, spriteY, spriteRes, x, y - GameRandom.globalRandom.nextFloat() * randomUp, knockbackX, knockbackY);
      this.recolorTexture = GameSkin.recolorLookupTexture;
      this.recolorYIndex = skin.colorIndex + 1;
   }

   public FleshParticle(
      Level level,
      GameTexture texture,
      int spriteX,
      int spriteY,
      int spriteWidth,
      int spriteHeight,
      float x,
      float y,
      float randomUp,
      float knockbackX,
      float knockbackY
   ) {
      this(level, texture, spriteX, spriteY, spriteWidth, spriteHeight, x, y - GameRandom.globalRandom.nextFloat() * randomUp, knockbackX, knockbackY);
   }

   @Override
   public void tickMovement(float delta) {
      float speed = GameMath.preciseDistance(0.0F, 0.0F, this.dx, this.dy);
      super.tickMovement(delta);
      if (speed > 0.0F) {
         float rotate = speed * 3.0F * delta / 250.0F;
         if (this.dx < 0.0F) {
            this.rotation -= rotate;
         } else {
            this.rotation += rotate;
         }
      }

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
         int halfWidth = this.sprite.getWidth() / 2;
         int halfHeight = this.sprite.getHeight() / 2;
         int drawX = camera.getDrawX(this.x) - halfWidth;
         int drawY = camera.getDrawY(this.y) - halfHeight + 4 - (int)Math.max(0.0F, this.height);
         float alpha = 1.0F;
         if (cycle > 0.5F) {
            alpha = Math.abs(cycle - 1.0F) * 2.0F;
         }

         final TextureDrawOptionsEnd options = this.sprite
            .initDraw()
            .light(light)
            .alpha(alpha)
            .rotate(this.rotation, halfWidth, halfHeight)
            .mirror(this.mirrored, false)
            .pos(drawX, drawY);
         if (this.recolorTexture != null) {
            options.recolor(this.recolorTexture, this.recolorYIndex);
         }

         list.add(new EntityDrawable(this) {
            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }
   }
}
