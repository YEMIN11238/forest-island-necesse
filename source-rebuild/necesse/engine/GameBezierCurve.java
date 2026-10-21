package necesse.engine;

import java.util.ArrayList;
import necesse.engine.util.GameMath;
import necesse.gfx.gameTexture.GameTexture;
import org.lwjgl.opengl.GL11;

public class GameBezierCurve {
   public ArrayList<GameBezierPoint> points = new ArrayList<>();

   public void draw(float offsetX, float offsetY, float curvePrecision) {
      GameTexture.unbindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(1);
      GL11.glColor4f(0.0F, 0.0F, 1.0F, 1.0F);

      for (GameBezierPoint point : this.points) {
         GL11.glVertex2f(point.startX + offsetX, point.startY + offsetY);
         GL11.glVertex2f(point.targetX + offsetX, point.targetY + offsetY);
      }

      GL11.glEnd();

      for (GameBezierPoint point : this.points) {
         GL11.glBegin(2);
         GL11.glColor4f(1.0F, 1.0F, 0.0F, 1.0F);
         int segments = 20;
         int radius = 5;

         for (int i = 0; i < segments; i++) {
            double theta = (Math.PI * 2) * i / segments;
            double x = radius * Math.cos(theta);
            double y = radius * Math.sin(theta);
            GL11.glVertex2d(x + point.startX + offsetX, y + point.startY + offsetY);
         }

         GL11.glEnd();
      }

      GL11.glBegin(3);
      GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);

      for (int i = 0; i < this.points.size() - 1; i++) {
         GameBezierPoint first = this.points.get(i);
         GameBezierPoint second = this.points.get(i + 1);
         float deltaX = second.targetX - second.startX;
         float deltaY = second.targetY - second.startY;
         second = new GameBezierPoint(second.startX, second.startY, second.startX - deltaX, second.startY - deltaY);
         float distance = GameMath.getExactDistance(first.startX, first.startY, first.targetX, first.targetY)
            + GameMath.getExactDistance(first.targetX, first.targetY, second.startX, second.startY)
            + GameMath.getExactDistance(first.startX, first.startY, first.targetX, first.targetY);
         int drawIterations = (int)(distance / curvePrecision) + 1;

         for (int drawIteration = 0; drawIteration <= drawIterations; drawIteration++) {
            float progress = (float)drawIteration / drawIterations;
            float drawX = first.getPointXOnCurve(second, progress) + offsetX;
            float drawY = first.getPointYOnCurve(second, progress) + offsetY;
            GL11.glVertex2f(drawX, drawY);
         }
      }

      GL11.glEnd();
   }
}
