package necesse.entity.mobs.job;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobTargetFoundResult;

public class LinkedListJobSequence extends LinkedList<ActiveJob> implements JobSequence {
   public GameMessage activityDescription;
   public boolean isRecreationJob;

   public LinkedListJobSequence(GameMessage activityDescription, boolean isRecreationJob) {
      this.activityDescription = activityDescription;
      this.isRecreationJob = isRecreationJob;
   }

   @Override
   public GameMessage getActivityDescription() {
      return this.activityDescription;
   }

   @Override
   public void setActivityDescription(GameMessage description) {
      this.activityDescription = description;
   }

   @Override
   public void tick() {
      for (ActiveJob activeJob : this) {
         activeJob.tick(false, false);
      }
   }

   @Override
   public boolean hasNext() {
      return !this.isEmpty();
   }

   @Override
   public ActiveJob next() {
      return this.removeFirst();
   }

   @Override
   public boolean isValid() {
      ListIterator<ActiveJob> li = this.listIterator();

      while (li.hasNext()) {
         ActiveJob next = li.next();
         if (!next.isValid(false)) {
            if (next.shouldClearSequence()) {
               return false;
            }

            li.remove();
         }
      }

      return true;
   }

   @Override
   public ActiveJobTargetFoundResult onTargetFound(Mob target) {
      for (ActiveJob activeJob : this) {
         ActiveJobTargetFoundResult next = activeJob.onTargetFound(target, false, false);
         if (next == ActiveJobTargetFoundResult.FAIL) {
            return next;
         }
      }

      return ActiveJobTargetFoundResult.CONTINUE;
   }

   @Override
   public ActiveJobTargetFoundResult onFollowOutOfRange(Mob followingMob) {
      for (ActiveJob activeJob : this) {
         ActiveJobTargetFoundResult next = activeJob.onFollowOutOfRange(followingMob, false, false);
         if (next == ActiveJobTargetFoundResult.CONTINUE) {
            return next;
         }
      }

      return ActiveJobTargetFoundResult.FAIL;
   }

   @Override
   public void cancel(boolean becauseOfInvalid) {
      for (ActiveJob activeJob : this) {
         activeJob.onCancelled(becauseOfInvalid, false, false);
      }
   }

   @Override
   public boolean isRecreationJob() {
      return this.isRecreationJob;
   }

   @Override
   public void onJobFinished(ActiveJob job) {
   }

   @Override
   public String toString() {
      return super.toString() + "{" + Arrays.toString(this.toArray()) + "}";
   }
}
