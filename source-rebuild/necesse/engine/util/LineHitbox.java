package necesse.engine.util;

import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D.Float;
import necesse.gfx.gameTexture.GameTexture;
import org.lwjgl.opengl.GL11;

public class LineHitbox extends Polygon {
   private final float width;
   private boolean circular = false;
   private float circleX;
   private float circleY;

   public LineHitbox(Line2D line, float width) {
      this.width = width;
      this.calculatePolygon(line);
   }

   public LineHitbox(Line2D line, float circleX, float circleY, float width) {
      this.width = width;
      this.calculatePolygon(line);
      this.circular = true;
      this.circleX = circleX;
      this.circleY = circleY;
   }

   public LineHitbox(float x, float y, float dx, float dy, float length, float width) {
      this.width = width;
      Float p = GameMath.normalize(dx, dy);
      this.calculatePolygon(x, y, p.x, p.y, length);
   }

   public LineHitbox(float fromX, float fromY, float toX, float toY, float width) {
      this.width = width;
      Float tempPoint = new Float(toX - fromX, toY - fromY);
      float dist = (float)tempPoint.distance(0.0, 0.0);
      float normX = dist == 0.0F ? 0.0F : tempPoint.x / dist;
      float normY = dist == 0.0F ? 0.0F : tempPoint.y / dist;
      this.calculatePolygon(fromX, fromY, normX, normY, dist);
   }

   public static LineHitbox fromAngled(float fromX, float fromY, float angle, float length, float width) {
      angle = GameMath.fixAngle(angle);
      float dx = (float)Math.cos(Math.toRadians(angle - 90.0F));
      float dy = (float)Math.sin(Math.toRadians(angle - 90.0F));
      return new LineHitbox(fromX, fromY, dx, dy, length, width);
   }

   @Override
   public boolean intersects(double x, double y, double w, double h) {
      boolean intersects = super.intersects(x, y, w, h);
      if (intersects && this.circular) {
         double midX = x + w / 2.0;
         double midY = y + h / 2.0;
         Float normalize = GameMath.normalize((float)midX - this.circleX, (float)midY - this.circleY);
         java.awt.geom.Line2D.Float line = new java.awt.geom.Line2D.Float(
            this.circleX, this.circleY, this.circleX + normalize.x * this.width / 2.0F, this.circleY + normalize.y * this.width / 2.0F
         );
         return line.intersects(x, y, w, h);
      } else {
         return intersects;
      }
   }

   protected void calculatePolygon(float x, float y, float dx, float dy, float length) {
      this.calculatePolygon(new java.awt.geom.Line2D.Float(x, y, x + dx * length, y + dy * length));
   }

   protected void calculatePolygon(Line2D line) {
      Float start = new Float((float)line.getX1(), (float)line.getY1());
      Float end = new Float((float)line.getX2(), (float)line.getY2());
      Float dir = GameMath.normalize(start.x - end.x, start.y - end.y);
      Float start1 = GameMath.getPerpendicularPoint(start, -this.width / 2.0F, dir);
      Float start2 = GameMath.getPerpendicularPoint(start, this.width / 2.0F, dir);
      Float end1 = GameMath.getPerpendicularPoint(end, -this.width / 2.0F, dir);
      Float end2 = GameMath.getPerpendicularPoint(end, this.width / 2.0F, dir);
      this.xpoints = new int[]{(int)start1.x, (int)start2.x, (int)end2.x, (int)end1.x};
      this.ypoints = new int[]{(int)start1.y, (int)start2.y, (int)end2.y, (int)end1.y};
      this.npoints = 4;
   }

   public void draw(float red, float green, float blue, float alpha) {
      GameTexture.unbindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(5);
      GL11.glColor4f(red, green, blue, alpha);
      GL11.glVertex2f(this.xpoints[1], this.ypoints[1]);
      GL11.glVertex2f(this.xpoints[0], this.ypoints[0]);
      GL11.glVertex2f(this.xpoints[2], this.ypoints[2]);
      GL11.glVertex2f(this.xpoints[3], this.ypoints[3]);
      GL11.glEnd();
   }
}
