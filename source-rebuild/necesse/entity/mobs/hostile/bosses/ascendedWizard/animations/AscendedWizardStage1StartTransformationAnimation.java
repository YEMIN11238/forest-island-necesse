package necesse.entity.mobs.hostile.bosses.ascendedWizard.animations;

public class AscendedWizardStage1StartTransformationAnimation extends AscendedWizardAnimationList {
   public AscendedWizardStage1StartTransformationAnimation(int totalTime) {
      super(new SimpleSingleAnimation(0, 11, 2, 200), new SimpleLoopingAnimation(0, 12, 4, 400).setTotalTime(totalTime - 200));
   }
}
