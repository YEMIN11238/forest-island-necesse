package necesse.entity.trails;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import necesse.engine.util.GameMath;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import org.lwjgl.opengl.GL11;

public class TrailDrawSection {
   public final Trail trail;
   public int pointsStartIndex;
   public int pointsEndIndex;
   public int yTile;

   public TrailDrawSection(Trail trail, int yTile, int pointsStartIndex, int pointsEndIndex) {
      this.trail = trail;
      this.yTile = yTile;
      this.pointsStartIndex = pointsStartIndex;
      this.pointsEndIndex = pointsEndIndex;
   }

   public static TrailDrawSection.TrailPointConsumer fadeLightColorSetter(int fadeInTime, int fadeOutTime, Level level, Color col) {
      return fadeLightColorSetter(level == null ? 0L : level.getWorldEntity().getLocalTime(), fadeInTime, fadeOutTime, level, col);
   }

   public static TrailDrawSection.TrailPointConsumer fadeLightColorSetter(long globalTime, int fadeInTime, int fadeOutTime, Level level, Color col) {
      return (p1, p2, time) -> {
         GameLight light = level == null
            ? new GameLight(150.0F)
            : level.getLightLevel(GameMath.getTileCoordinate(p1.getX()), GameMath.getTileCoordinate(p1.getY()));
         float lifeTime = (float)(globalTime - time);
         float fadeInAlpha = fadeInTime > 0 ? Math.min(lifeTime / fadeInTime, 1.0F) : 1.0F;
         float fadeOutAlpha = fadeOutTime > 0 ? 1.0F - Math.min(lifeTime / fadeOutTime, 1.0F) : 1.0F;
         light.getGLColorSetter(col.getRed() / 255.0F, col.getGreen() / 255.0F, col.getBlue() / 255.0F, col.getAlpha() / 255.0F * fadeInAlpha * fadeOutAlpha)
            .run();
      };
   }

   public static TrailDrawSection.TrailPointConsumer lightColorSetter(Level level, Color col) {
      return (p1, p2, time) -> {
         GameLight light = level == null
            ? new GameLight(150.0F)
            : level.getLightLevel(GameMath.getTileCoordinate(p1.getX()), GameMath.getTileCoordinate(p1.getY()));
         light.getGLColorSetter(col.getRed() / 255.0F, col.getGreen() / 255.0F, col.getBlue() / 255.0F, col.getAlpha() / 255.0F).run();
      };
   }

   public DrawOptions getSpriteTrailsDraw(GameSprite sprite, GameCamera camera, TrailDrawSection.TrailPointConsumer colorSetter) {
      synchronized (this.trail) {
         TrailPointList points = this.trail.points;
         int pointsStartIndex = this.pointsStartIndex;
         int pointsEndIndex = this.pointsEndIndex;
         return getSpriteTrailsDraw(sprite, points, pointsStartIndex, pointsEndIndex, camera, colorSetter);
      }
   }

   public static DrawOptions getSpriteTrailsDraw(
      GameSprite sprite, TrailPointList points, int pointsStartIndex, int pointsEndIndex, GameCamera camera, TrailDrawSection.TrailPointConsumer colorSetter
   ) {
      int width = sprite.texture.getWidth();
      int height = sprite.texture.getHeight();
      float leftXCoord = TextureDrawOptions.pixel(sprite.spriteX, 1, sprite.spriteWidth, width);
      float rightXCoord = TextureDrawOptions.pixel(sprite.spriteX + 1, -1, sprite.spriteWidth, width);
      float topYCoord = TextureDrawOptions.pixel(sprite.spriteY, 1, sprite.spriteHeight, height);
      float botYCoord = TextureDrawOptions.pixel(sprite.spriteY + 1, -1, sprite.spriteHeight, height);
      LinkedList<TrailDrawSection.TrailDrawOption> drawPoints = new LinkedList<>();
      pointsEndIndex = Math.min(pointsEndIndex, points.size() - 1);

      for (int i = pointsStartIndex; i <= pointsEndIndex; i++) {
         drawPoints.add(new TrailDrawSection.TrailDrawOption(points.get(i), i % 2));
      }

      return () -> {
         sprite.texture.bindTexture();
         GL11.glLoadIdentity();
         GL11.glBegin(5);

         for (TrailDrawSection.TrailDrawOption drawPoint : drawPoints) {
            Point2D p1 = drawPoint.draw1;
            int x1 = camera.getDrawX((int)p1.getX());
            int y1 = camera.getDrawY((int)(p1.getY() - drawPoint.height));
            Point2D p2 = drawPoint.draw2;
            int x2 = camera.getDrawX((int)p2.getX());
            int y2 = camera.getDrawY((int)(p2.getY() - drawPoint.height));
            if ((p1.getX() != 0.0 || p1.getY() != 0.0) && (p2.getX() != 0.0 || p2.getY() != 0.0)) {
               colorSetter.apply(p1, p2, drawPoint.spawnTime);
               if (drawPoint.tex == 0) {
                  GL11.glTexCoord2f(leftXCoord, topYCoord);
               } else {
                  GL11.glTexCoord2f(leftXCoord, botYCoord);
               }

               GL11.glVertex2f(x1, y1);
               if (drawPoint.tex == 0) {
                  GL11.glTexCoord2f(rightXCoord, topYCoord);
               } else {
                  GL11.glTexCoord2f(rightXCoord, botYCoord);
               }

               GL11.glVertex2f(x2, y2);
            }
         }

         GL11.glEnd();
      };
   }

   private static class TrailDrawOption {
      public final Point2D draw1;
      public final Point2D draw2;
      public final long spawnTime;
      public final float height;
      public final int tex;

      public TrailDrawOption(TrailPointList.TrailPoint point, int tex) {
         this.draw1 = point.getDrawPos1();
         this.draw2 = point.getDrawPos2();
         this.spawnTime = point.spawnTime;
         this.height = point.vector.height;
         this.tex = tex;
      }
   }

   @FunctionalInterface
   public interface TrailPointConsumer {
      void apply(Point2D var1, Point2D var2, long var3);
   }
}
