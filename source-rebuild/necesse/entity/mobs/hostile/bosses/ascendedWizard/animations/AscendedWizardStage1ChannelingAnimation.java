package necesse.entity.mobs.hostile.bosses.ascendedWizard.animations;

public class AscendedWizardStage1ChannelingAnimation extends AscendedWizardAnimationList {
   public AscendedWizardStage1ChannelingAnimation(int totalTime) {
      super(new SimpleSingleAnimation(0, 5, 2, 200), new SimpleLoopingAnimation(0, 6, 4, 400).setTotalTime(totalTime - 200));
   }
}
