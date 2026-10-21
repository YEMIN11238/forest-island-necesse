package necesse.engine.util.tween;

import necesse.engine.GameLog;

public class TestTween extends Tween<TestTween> {
   public TestTween(double duration) {
      super(duration);
   }

   @Override
   protected void preparePlay() {
      GameLog.debug.println("Tween preparing for forwards play");
   }

   @Override
   protected void prepareBackwardsPlay() {
      GameLog.debug.println("Tween preparing for backwards play");
   }

   @Override
   protected void tween(double percent) {
      GameLog.debug.println("Tweening: " + percent);
   }

   @Override
   protected void progressToCompletion() {
      GameLog.debug.println("Tween finished");
   }

   @Override
   protected void progressToBeginning() {
      GameLog.debug.println("Backwards tween finished");
   }

   @Override
   protected void progressBackwards(double percent) {
      GameLog.debug.println("Backwards tweening: " + percent);
   }
}
