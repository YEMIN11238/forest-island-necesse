package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.util.function.Consumer;
import necesse.engine.GameTileRange;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.JobFinder;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;

public class HumanJobSearchingAINode<T extends HumanMob> extends AINode<T> {
   public long nextSearchTime;
   public GameTileRange maxFollowingTileRange;
   public int minSearchCooldownSeconds = 5;
   public int maxSearchCooldownSeconds = 10;

   public HumanJobSearchingAINode(GameTileRange maxFollowingTileRange) {
      this.maxFollowingTileRange = maxFollowingTileRange;
   }

   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
   }

   public void init(T mob, Blackboard<T> blackboard) {
   }

   public boolean isOnSearchCooldown(T mob) {
      return mob.getTime() < this.nextSearchTime;
   }

   public void startSearchCooldown(T mob) {
      this.nextSearchTime = mob.getTime() + GameRandom.globalRandom.getIntBetween(this.minSearchCooldownSeconds, this.maxSearchCooldownSeconds) * 1000L;
   }

   public AINodeResult tick(T mob, Blackboard<T> blackboard) {
      return this.tickSearching(false).nodeResult;
   }

   public HumanJobSearchingAINode.SearchResult tickSearching(boolean allowReplaceRecreationJob) {
      T mob = this.mob();
      Blackboard<T> blackboard = this.getBlackboard();
      if (!mob.isBeingInteractedWith()) {
         HumanJobSearchingAINode.SearchResult result = Performance.record(
            mob.getLevel().tickManager(),
            "humanJobSearch",
            () -> {
               Mob followingMob = mob.getJobFollowingMob();
               Consumer<JobFinder> finderMod;
               if (this.maxFollowingTileRange != null) {
                  if (followingMob == null) {
                     return HumanJobSearchingAINode.SearchResult.FAILED;
                  }

                  finderMod = finder -> {
                     finder.levelJobPredicate = job -> job.canDoWhileFollowing(followingMob);
                     finder.zoneTester = (tileX, tileY) -> this.maxFollowingTileRange
                        .isWithinRange(followingMob.getTileX(), followingMob.getTileY(), tileX, tileY);
                     finder.searchBounds = this.maxFollowingTileRange.getRangeBounds(followingMob.getTileX(), followingMob.getTileY());
                  };
               } else {
                  finderMod = null;
               }

               ActiveJob lastJob = blackboard.getObject(ActiveJob.class, "currentJob");
               if (lastJob != null && !allowReplaceRecreationJob) {
                  return HumanJobSearchingAINode.SearchResult.KEPT_EXISTING;
               }

               JobSequence lastSequence = blackboard.getObject(JobSequence.class, "currentJobSequence");
               boolean foundNew = false;
               if (lastSequence == null) {
                  mob.resetJobCancelled();
                  if (this.isOnSearchCooldown(mob)) {
                     return HumanJobSearchingAINode.SearchResult.ON_COOLDOWN;
                  }

                  lastSequence = mob.findJob(false, finderMod);
                  if (lastSequence == null) {
                     this.startSearchCooldown(mob);
                  }

                  foundNew = true;
               } else if (mob.isJobCancelled()) {
                  lastSequence.cancel(false);
                  lastSequence = null;
                  mob.resetJobCancelled();
               } else if (allowReplaceRecreationJob && lastSequence.isRecreationJob()) {
                  if (this.isOnSearchCooldown(mob)) {
                     return HumanJobSearchingAINode.SearchResult.ON_COOLDOWN;
                  }

                  JobSequence nextSequence = mob.findJob(true, finderMod);
                  if (nextSequence == null) {
                     this.startSearchCooldown(mob);
                  }

                  if (nextSequence != null && !nextSequence.isRecreationJob()) {
                     lastSequence.cancel(false);
                     foundNew = true;
                     lastSequence = nextSequence;
                  }
               }

               if (lastSequence != null && lastSequence.hasNext()) {
                  if (mob.objectUser != null) {
                     mob.objectUser.stopUsing();
                  }

                  ActiveJob next = lastSequence.next();
                  if (next != null) {
                     next.onMadeCurrent();
                  }

                  blackboard.put("currentJobSequence", lastSequence);
                  blackboard.put("currentJob", next);
                  GameMessage activityDescription = lastSequence.getActivityDescription();
                  if (activityDescription != null) {
                     mob.setActivity("job", 10000, activityDescription);
                  }

                  return foundNew ? HumanJobSearchingAINode.SearchResult.FOUND_NEW : HumanJobSearchingAINode.SearchResult.KEPT_EXISTING;
               } else {
                  return null;
               }
            }
         );
         if (result != null) {
            return result;
         }

         blackboard.put("currentJobSequence", null);
         blackboard.put("currentJob", null);
         mob.resetJobCancelled();
      }

      mob.clearActivity(null);
      return HumanJobSearchingAINode.SearchResult.FAILED;
   }

   public enum SearchResult {
      KEPT_EXISTING(AINodeResult.SUCCESS),
      FOUND_NEW(AINodeResult.SUCCESS),
      FAILED(AINodeResult.FAILURE),
      ON_COOLDOWN(AINodeResult.FAILURE);

      public final AINodeResult nodeResult;

      SearchResult(AINodeResult nodeResult) {
         this.nodeResult = nodeResult;
      }
   }
}
