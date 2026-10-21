package necesse.engine.screenHudManager;

import java.util.ArrayList;
import java.util.Comparator;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;

public class ScreenHudManager {
   private ArrayList<ScreenHudElement> list;
   private boolean fadedHUD = false;
   private long fadedHUDStartTime;
   private float finalHudAlpha;

   public ScreenHudManager() {
      this.list = new ArrayList<>();
   }

   public void cleanUp() {
      for (int i = 0; i < this.list.size(); i++) {
         ScreenHudElement element = this.list.get(i);
         if (element.isRemoved()) {
            this.list.remove(i);
            element.onRemove();
            i--;
         }
      }
   }

   public void draw(TickManager tickManager) {
      this.list.sort(Comparator.comparingInt(ScreenHudElement::getDrawPriority).reversed());

      for (ScreenHudElement element : this.list.toArray(new ScreenHudElement[0])) {
         if (element != null && !element.isRemoved()) {
            element.draw(tickManager);
         }
      }
   }

   public void preGameTick(TickManager tickManager) {
      boolean fadeHUD = this.fadedHUD;
      if (tickManager.isGameTick()) {
         if (!this.fadedHUD) {
            this.fadedHUDStartTime = 0L;
         }

         this.fadedHUD = false;
      }

      float hudAlpha = 1.0F;
      if (fadeHUD) {
         long timeSinceStart = System.currentTimeMillis() - this.fadedHUDStartTime;
         if (timeSinceStart > 500L) {
            long fadeTime = 1000L;
            long timeSinceFadeStart = Math.min(timeSinceStart - 500L, fadeTime);
            float fadePercent = (float)timeSinceFadeStart / (float)fadeTime;
            hudAlpha = GameMath.lerp(fadePercent, 1.0F, 0.1F);
         }
      }

      this.finalHudAlpha = hudAlpha;
   }

   public <T extends ScreenHudElement> T addElement(T element) {
      element.addThis(this.list);
      return element;
   }

   public void fadeHUD() {
      this.fadedHUD = true;
      if (this.fadedHUDStartTime == 0L) {
         this.fadedHUDStartTime = System.currentTimeMillis();
      }
   }

   public float getHudAlpha() {
      return this.finalHudAlpha;
   }
}
