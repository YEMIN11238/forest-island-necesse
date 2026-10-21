package necesse.gfx.forms.presets.debug.tools;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.MouseWheelBuffer;
import necesse.engine.util.ObjectValue;
import necesse.engine.util.tween.EaseFunction;
import necesse.engine.util.tween.Easings;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.hudManager.HudDrawElement;

public class EasingsTestGameTool extends MouseDebugGameTool {
   private static final ArrayList<ObjectValue<EaseFunction, String>> easings = new ArrayList<>(
      Arrays.asList(
         new ObjectValue<>(Easings.Linear, "Linear"),
         new ObjectValue<>(Easings.SineIn, "SineIn"),
         new ObjectValue<>(Easings.SineOut, "SineOut"),
         new ObjectValue<>(Easings.SineInOut, "SineInOut"),
         new ObjectValue<>(Easings.QuadIn, "QuadIn"),
         new ObjectValue<>(Easings.QuadOut, "QuadOut"),
         new ObjectValue<>(Easings.QuadInOut, "QuadInOut"),
         new ObjectValue<>(Easings.CubicIn, "CubicIn"),
         new ObjectValue<>(Easings.CubicOut, "CubicOut"),
         new ObjectValue<>(Easings.CubicInOut, "CubicInOut"),
         new ObjectValue<>(Easings.QuartIn, "QuartIn"),
         new ObjectValue<>(Easings.QuartOut, "QuartOut"),
         new ObjectValue<>(Easings.QuartInOut, "QuartInOut"),
         new ObjectValue<>(Easings.QuintIn, "QuintIn"),
         new ObjectValue<>(Easings.QuintOut, "QuintOut"),
         new ObjectValue<>(Easings.QuintInOut, "QuintInOut"),
         new ObjectValue<>(Easings.ExpoIn, "ExpoIn"),
         new ObjectValue<>(Easings.ExpoOut, "ExpoOut"),
         new ObjectValue<>(Easings.ExpoInOut, "ExpoInOut"),
         new ObjectValue<>(Easings.CircIn, "CircIn"),
         new ObjectValue<>(Easings.CircOut, "CircOut"),
         new ObjectValue<>(Easings.CircInOut, "CircInOut"),
         new ObjectValue<>(Easings.BackIn, "BackIn"),
         new ObjectValue<>(Easings.BackOut, "BackOut"),
         new ObjectValue<>(Easings.BackInOut, "BackInOut"),
         new ObjectValue<>(Easings.ElasticIn, "ElasticIn"),
         new ObjectValue<>(Easings.ElasticOut, "ElasticOut"),
         new ObjectValue<>(Easings.ElasticInOut, "ElasticInOut"),
         new ObjectValue<>(Easings.BounceIn, "BounceIn"),
         new ObjectValue<>(Easings.BounceOut, "BounceOut"),
         new ObjectValue<>(Easings.BounceInOut, "BounceInOut")
      )
   );
   public HudDrawElement hudElement;
   private ObjectValue<EaseFunction, String> easeFunction = easings.get(0);
   private long speed = 1000L;
   private MouseWheelBuffer wheelBuffer = new MouseWheelBuffer(false);
   private double time = 0.0;
   private int easingIndex = 0;

   public EasingsTestGameTool(DebugForm parent, String name) {
      super(parent, name);
   }

   @Override
   public void init() {
      this.onLeftClick(e -> {
         this.easingIndex--;
         if (this.easingIndex < 0) {
            this.easingIndex = easings.size() - 1;
         }

         this.easeFunction = easings.get(this.easingIndex);
         return true;
      }, "Previous easing");
      this.onRightClick(e -> {
         this.easingIndex++;
         if (this.easingIndex >= easings.size()) {
            this.easingIndex = 0;
         }

         this.easeFunction = easings.get(this.easingIndex);
         return true;
      }, "Next easing");
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
                           EasingsTestGameTool.this.time += tickManager.getDelta();
                           Easings.drawEasing(
                              EasingsTestGameTool.this.easeFunction.object,
                              EasingsTestGameTool.this.time % EasingsTestGameTool.this.speed / EasingsTestGameTool.this.speed,
                              100,
                              WindowManager.getWindow().mousePos().sceneX - 110,
                              WindowManager.getWindow().mousePos().sceneY - 100,
                              100.0,
                              Color.red
                           );
                           FontManager.bit
                              .drawString(
                                 WindowManager.getWindow().mousePos().sceneX,
                                 WindowManager.getWindow().mousePos().sceneY + 10,
                                 EasingsTestGameTool.this.easeFunction.value,
                                 new FontOptions(10)
                              );
                           FontManager.bit
                              .drawString(
                                 WindowManager.getWindow().mousePos().sceneX,
                                 WindowManager.getWindow().mousePos().sceneY + 20,
                                 EasingsTestGameTool.this.speed + " ms",
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
