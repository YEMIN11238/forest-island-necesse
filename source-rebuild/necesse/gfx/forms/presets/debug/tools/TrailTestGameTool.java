package necesse.gfx.forms.presets.debug.tools;

import java.awt.Color;
import java.awt.Point;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.Renderer;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameTexture.GameSprite;

public class TrailTestGameTool extends MouseDebugGameTool {
   public boolean isDown;
   public Trail trail;
   public float thickness = 100.0F;
   public float height = 0.0F;
   public Point lastPoint;

   public TrailTestGameTool(DebugForm parent, String name) {
      super(parent, name);
   }

   @Override
   public void init() {
      this.onLeftEvent(e -> {
         if (e.state) {
            this.isDown = true;
            this.refreshTrail(this.getMouseX(), this.getMouseY());
         } else {
            this.isDown = false;
         }

         return true;
      }, "Create trail");
      this.onMouseMove(e -> {
         if (this.isDown) {
            this.refreshTrail(this.getMouseX(), this.getMouseY());
            return true;
         } else {
            return false;
         }
      });
      this.onRightClick(e -> {
         if (this.trail != null) {
            this.trail.remove();
            this.trail = null;
         }

         return true;
      }, "Clear trail");
   }

   protected void refreshTrail(int x, int y) {
      if (this.lastPoint == null || this.lastPoint.x != x || this.lastPoint.y != y) {
         if (this.trail != null && !this.trail.isRemoved()) {
            this.trail.addPoint(new TrailVector(x, y, x - this.lastPoint.x, y - this.lastPoint.y, this.thickness, this.height), 0);
         } else {
            this.trail = new Trail(new TrailVector(x, y, 0.0F, -1.0F, this.thickness, this.height), this.getLevel(), new Color(255, 0, 0, 100), 60000);
            this.trail.smoothCorners = true;
            this.trail.sprite = new GameSprite(Renderer.getQuadTexture());
            this.getLevel().entityManager.addTrail(this.trail);
         }

         this.lastPoint = new Point(x, y);
      }
   }
}
