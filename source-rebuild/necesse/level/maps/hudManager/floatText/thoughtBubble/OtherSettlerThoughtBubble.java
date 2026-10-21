package necesse.level.maps.hudManager.floatText.thoughtBubble;

import java.util.Objects;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.job.JobWorkerChatter;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.level.maps.levelData.settlementData.settler.Settler;

public class OtherSettlerThoughtBubble extends ThoughtBubble {
   public JobWorkerChatter target;

   public OtherSettlerThoughtBubble(Mob mob, int stayTime, JobWorkerChatter target) {
      super(mob, stayTime);
      Objects.requireNonNull(target);
      this.target = target;
   }

   @Override
   public DrawOptions getThoughtContent(int drawX, int drawY, int size, float fadeInProgress, PlayerMob perspective) {
      HumanDrawOptions drawOptions = this.target.getChatterFaceDrawOptions();
      return Settler.getHumanFaceDrawOptions(drawOptions, size, drawX, drawY + (int)(fadeInProgress * 2.0F));
   }
}
