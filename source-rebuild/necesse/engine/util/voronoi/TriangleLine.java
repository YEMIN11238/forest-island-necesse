package necesse.engine.util.voronoi;

import java.awt.geom.Point2D.Float;
import java.util.Objects;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;

public class TriangleLine {
   public final Float p1;
   public final Float p2;

   public TriangleLine(Float p1, Float p2) {
      if (TriangleData.compare(p1, p2) == -1) {
         this.p1 = p1;
         this.p2 = p2;
      } else {
         this.p2 = p1;
         this.p1 = p2;
      }
   }

   public DrawOptions getDrawOptions(GameCamera camera) {
      DrawOptionsList drawOptions = new DrawOptionsList(3);
      int x1 = camera.getDrawX(this.p1.x);
      int y1 = camera.getDrawY(this.p1.y);
      int x2 = camera.getDrawX(this.p2.x);
      int y2 = camera.getDrawY(this.p2.y);
      drawOptions.add(() -> Renderer.drawLineRGBA(x1, y1, x2, y2, 0.0F, 1.0F, 1.0F, 1.0F));
      return drawOptions;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof TriangleLine)) {
         return false;
      }

      TriangleLine that = (TriangleLine)o;
      return this.p1.equals(that.p1) && this.p2.equals(that.p2);
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.p1, this.p2);
   }

   @Override
   public String toString() {
      return "L[" + this.p1.x + "x" + this.p1.y + ", " + this.p2.x + "x" + this.p2.y + "]";
   }
}
