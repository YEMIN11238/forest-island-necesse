package necesse.gfx.forms.presets.debug.tools;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.ExpandingPolygon;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.hudManager.HudDrawElement;

public class ExpandingPolygonGameTool extends MouseDebugGameTool {
   private ArrayList<Point> points;
   private ExpandingPolygon polygon;
   public HudDrawElement hudElement;

   public ExpandingPolygonGameTool(DebugForm parent, String name) {
      super(parent, name);
      this.onLeftClick(e -> {
         Point point = new Point(this.getMouseX(), this.getMouseY());
         this.points.add(point);
         this.polygon.addPoint(point);
         return true;
      }, "Add point");
      this.onRightClick(e -> {
         this.points.stream().min(Comparator.comparingDouble(p -> p.distance(this.getMouseX(), this.getMouseY()))).ifPresent(p -> this.points.remove(p));
         this.polygon = new ExpandingPolygon(this.points.toArray(new Point[0]));
         return true;
      }, "Remove point");
   }

   @Override
   public void init() {
      this.points = new ArrayList<>();
      this.polygon = new ExpandingPolygon();
      final CollisionFilter filter = new CollisionFilter().mobCollision();
      this.getLevel().hudManager.addElement(this.hudElement = new HudDrawElement() {
         @Override
         public void addDrawables(List<SortedDrawable> list, final GameCamera camera, PlayerMob perspective) {
            list.add(new SortedDrawable() {
               @Override
               public int getPriority() {
                  return -10000;
               }

               @Override
               public void draw(TickManager tickManager) {
                  for (int i = 0; i < ExpandingPolygonGameTool.this.points.size(); i++) {
                     Point point = ExpandingPolygonGameTool.this.points.get(i);
                     Renderer.drawCircle(camera.getDrawX(point.x), camera.getDrawY(point.y), 5, 10, 0.0F, 1.0F, 0.0F, 1.0F, true);
                     FontManager.bit.drawString(camera.getDrawX(point.x), camera.getDrawY(point.y), "" + i, new FontOptions(12).outline());
                  }

                  Renderer.drawShape(ExpandingPolygonGameTool.this.polygon, camera, false, 1.0F, 1.0F, 0.0F, 1.0F);

                  for (LevelObjectHit collision : getLevel().getCollisions(ExpandingPolygonGameTool.this.polygon, filter)) {
                     Renderer.drawRectangleLines(collision, camera, 1.0F, 0.0F, 0.0F, 1.0F);
                  }
               }
            });
         }
      });
   }

   @Override
   public void isCancelled() {
      super.isCancelled();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }
   }

   @Override
   public void isCleared() {
      super.isCleared();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }
   }
}
