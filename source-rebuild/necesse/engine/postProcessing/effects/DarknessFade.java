package necesse.engine.postProcessing.effects;

import java.awt.Point;
import java.util.function.Supplier;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.util.GameMath;

public class DarknessFade {
   public float startDarkness;
   public float endDarkness;
   public int minViewDist;
   public int fadeDist;
   public long startTime;
   public long timeSpan;
   public Supplier<Point> midGetter;
   public Supplier<Boolean> isValid;

   public DarknessFade(
      float startDarkness,
      float endDarkness,
      int minViewDist,
      int fadeDist,
      long startTime,
      long timeSpan,
      Supplier<Point> midGetter,
      Supplier<Boolean> isValid
   ) {
      this.startDarkness = startDarkness;
      this.endDarkness = endDarkness;
      this.minViewDist = minViewDist;
      this.fadeDist = fadeDist;
      this.startTime = startTime;
      this.timeSpan = timeSpan;
      this.midGetter = midGetter;
      this.isValid = isValid;
   }

   public boolean apply() {
      if (this.isValid != null && !this.isValid.get()) {
         return false;
      }

      long timeSinceStart = System.currentTimeMillis() - this.startTime;
      if (timeSinceStart > this.timeSpan) {
         return false;
      }

      float percent = (float)timeSinceStart / (float)this.timeSpan;
      float actualDarkness = this.startDarkness + (this.endDarkness - this.startDarkness) * percent;
      Point mid = this.midGetter.get();
      PostProcessingEffects.setSceneDarkness(GameMath.limit(actualDarkness, 0.0F, 1.0F), this.minViewDist, this.fadeDist, mid.x, mid.y);
      return true;
   }
}
