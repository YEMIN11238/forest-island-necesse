package necesse.gfx.forms.presets.debug.tools;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.MouseWheelBuffer;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.pathProjectile.SurvivorWhipProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.HudDrawElement;

public class SurvivorWhipTestGameTool extends MouseDebugGameTool {
   public HudDrawElement hudElement;
   private long speed = 1000L;
   private final MouseWheelBuffer wheelBuffer = new MouseWheelBuffer(false);
   private double time = 0.0;

   public SurvivorWhipTestGameTool(DebugForm parent, String name) {
      super(parent, name);
   }

   @Override
   public void init() {
      this.onScroll(e -> {
         this.wheelBuffer.add(e);
         this.wheelBuffer.useScrollY(isPositive -> {
            int change = isPositive ? 1 : -1;
            double percent = this.time % this.speed / this.speed;
            this.speed = Math.max(100L, this.speed + change * 100);
            this.time = percent * this.speed;
         });
         return true;
      }, "Change simulation speed");
      this.setupHudElement();
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

   public void setupHudElement() {
      if (this.hudElement != null) {
         this.hudElement.remove();
      }

      this.getLevel()
         .hudManager
         .addElement(
            this.hudElement = new HudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
                  list.add(
                     new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return Integer.MAX_VALUE;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           SurvivorWhipTestGameTool.this.time += tickManager.getDelta();
                           SurvivorWhipProjectile.drawWhipPositionFloat(
                              (float)(SurvivorWhipTestGameTool.this.time % SurvivorWhipTestGameTool.this.speed) / (float)SurvivorWhipTestGameTool.this.speed,
                              400,
                              WindowManager.getWindow().mousePos().sceneX - 410,
                              WindowManager.getWindow().mousePos().sceneY - 400,
                              400.0F,
                              Color.red
                           );
                           FontManager.bit
                              .drawString(
                                 WindowManager.getWindow().mousePos().sceneX,
                                 WindowManager.getWindow().mousePos().sceneY + 30,
                                 SurvivorWhipTestGameTool.this.speed + " ms",
                                 new FontOptions(10)
                              );
                        }
                     }
                  );
               }
            }
         );
   }
}
