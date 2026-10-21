package necesse.gfx.forms.presets.debug.tools;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.level.maps.hudManager.HudDrawElement;

public class ChaikinSmoothTestGameTool extends MouseDebugGameTool {
   public Point lastPoint;
   public ArrayList<TrailVector> points = new ArrayList<>();
   public HudDrawElement hudElement;

   public ChaikinSmoothTestGameTool(DebugForm parent, String name) {
      super(parent, name);
   }

   @Override
   public void init() {
      this.onLeftEvent(e -> {
         Point nextPoint = new Point(this.getMouseX(), this.getMouseY());
         if (e.state) {
            this.lastPoint = nextPoint;
         } else {
            if (this.lastPoint != null) {
               this.points.add(new TrailVector(this.lastPoint.x, this.lastPoint.y, nextPoint.x - this.lastPoint.x, nextPoint.y - this.lastPoint.y, 0.0F, 0.0F));
            }

            this.lastPoint = null;
         }

         return true;
      }, "Add point");
      this.onRightClick(e -> {
         if (!this.points.isEmpty()) {
            this.points.remove(this.points.size() - 1);
         }

         return true;
      }, "Remove point");
      this.onKeyClick(71, e -> {
         this.points = Trail.smooth(this.points);
         return true;
      }, "Smooth");
      this.onKeyClick(67, e -> {
         this.points.clear();
         return true;
      }, "Clear points");
      this.setupHudElement();
   }

   public void setupHudElement() {
      if (this.hudElement != null) {
         this.hudElement.remove();
      }

      this.getLevel()
         .hudManager
         .addElement(
            this.hudElement = new HudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, final GameCamera camera, PlayerMob perspective) {
                  list.add(
                     new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return Integer.MAX_VALUE;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           if (ChaikinSmoothTestGameTool.this.lastPoint != null) {
                              Renderer.drawLineRGBA(
                                 camera.getDrawX(ChaikinSmoothTestGameTool.this.lastPoint.x),
                                 camera.getDrawY(ChaikinSmoothTestGameTool.this.lastPoint.y),
                                 camera.getDrawX(ChaikinSmoothTestGameTool.this.getMouseX()),
                                 camera.getDrawY(ChaikinSmoothTestGameTool.this.getMouseY()),
                                 0.0F,
                                 0.0F,
                                 1.0F,
                                 1.0F
                              );
                           }

                           for (int i = 0; i < ChaikinSmoothTestGameTool.this.points.size(); i++) {
                              TrailVector next = ChaikinSmoothTestGameTool.this.points.get(i);
                              if (i > 0) {
                                 TrailVector last = ChaikinSmoothTestGameTool.this.points.get(i - 1);
                                 Renderer.drawLineRGBA(
                                    camera.getDrawX(last.pos.x),
                                    camera.getDrawY(last.pos.y),
                                    camera.getDrawX(next.pos.x),
                                    camera.getDrawY(next.pos.y),
                                    1.0F,
                                    0.0F,
                                    0.0F,
                                    1.0F
                                 );
                              }
                           }

                           for (TrailVector next : ChaikinSmoothTestGameTool.this.points) {
                              Renderer.drawCircle(camera.getDrawX(next.pos.x), camera.getDrawY(next.pos.y), 2, 15, 0.0F, 1.0F, 0.0F, 1.0F, true);
                              Renderer.drawLineRGBA(
                                 camera.getDrawX(next.pos.x),
                                 camera.getDrawY(next.pos.y),
                                 camera.getDrawX(next.pos.x + next.dx * 20.0F),
                                 camera.getDrawY(next.pos.y + next.dy * 20.0F),
                                 0.0F,
                                 1.0F,
                                 0.0F,
                                 1.0F
                              );
                           }
                        }
                     }
                  );
               }
            }
         );
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
