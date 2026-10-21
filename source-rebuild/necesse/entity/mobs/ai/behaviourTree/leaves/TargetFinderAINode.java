package necesse.entity.mobs.ai.behaviourTree.leaves;

import java.awt.Point;
import java.util.Comparator;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.decorators.MoveTaskAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.event.TargetAIEvent;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.TargetValidity;

public abstract class TargetFinderAINode<T extends Mob> extends MoveTaskAINode<T> {
   public TargetValidity<T> validity;
   public TargetFinderDistance<T> distance;
   public String baseKey = "mobBase";
   public boolean baseKeyIsJustPreference;
   public int targetNonBaseWithinRange;
   public String focusTargetKey;
   public String currentTargetKey;
   public String newTargetFoundEventType = "newTargetFound";
   public String lastTargetInvalidEventType = "lastTargetInvalid";
   public boolean moveToAttacker = true;
   public boolean runOnGlobalTick = false;
   public boolean canRegainSameTargetIfLostToTimer = true;
   public int loseTargetTimer;
   public int noTargetFoundTimer;
   public int loseTargetMinCooldown = 3000;
   public int loseTargetMaxCooldown = 6000;
   public int noTargetFoundMinCooldown = 2000;
   public int noTargetFoundMaxCooldown = 4000;
   public final String resetTargetEvent = "resetTarget";

   public TargetFinderAINode(TargetFinderDistance<T> distance, TargetValidity<T> validity, String focusTargetKey, String currentTargetKey) {
      this.distance = distance;
      this.validity = validity;
      this.focusTargetKey = focusTargetKey;
      this.currentTargetKey = currentTargetKey;
      this.startLoseTargetTimer();
   }

   public TargetFinderAINode(TargetFinderDistance<T> distance, TargetValidity<T> validity) {
      this(distance, validity, "focusTarget", "currentTarget");
   }

   public TargetFinderAINode(TargetFinderDistance<T> distance) {
      this(distance, new TargetValidity<>());
   }

   public TargetFinderAINode(int searchDistance) {
      this(new TargetFinderDistance<>(searchDistance));
   }

   @Override
   protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      if (mob.isBoss() && this.noTargetFoundMinCooldown == 2000 && this.noTargetFoundMaxCooldown == 4000) {
         this.noTargetFoundMinCooldown = 1000;
         this.noTargetFoundMaxCooldown = 2000;
      }

      blackboard.onGlobalTick(e -> {
         if (this.runOnGlobalTick) {
            this.tickTargetFinder(mob, blackboard);
         }
      });
      blackboard.onWasHit(e -> this.noTargetFoundTimer = 0);
      blackboard.onEvent("resetTarget", e -> {
         this.noTargetFoundTimer = 0;
         this.loseTargetTimer = 0;
         blackboard.put(this.currentTargetKey, null);
         blackboard.submitEvent(this.newTargetFoundEventType, new TargetAIEvent(null));
      });
   }

   @Override
   public void init(T mob, Blackboard<T> blackboard) {
   }

   public AINodeResult tickTargetFinder(T mob, Blackboard<T> blackboard) {
      Mob lastTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
      Mob newTarget = lastTarget;
      boolean addTargetLostDistance = false;
      int lastSearchDistance = this.distance.searchDistance;

      try {
         if (newTarget == null && this.noTargetFoundTimer >= 0) {
            this.noTargetFoundTimer -= 50;
            return AINodeResult.FAILURE;
         }

         Mob lostTargetToTimer = null;
         if (newTarget != null && this.loseTargetMinCooldown >= 0 && this.loseTargetMaxCooldown >= 0) {
            this.loseTargetTimer -= 50;
            if (this.loseTargetTimer <= 0) {
               lostTargetToTimer = newTarget;
               newTarget = null;
               addTargetLostDistance = true;
               this.startLoseTargetTimer();
            }
         }

         Point baseKeyObject = this.getBase(mob, blackboard);
         Point base = baseKeyObject != null ? baseKeyObject : new Point(mob.getX(), mob.getY());
         if (newTarget != null
            && (
               newTarget.removed()
                  || !this.validity.isValidTarget(this, mob, newTarget, false)
                  || this.distance.getDistance(base, newTarget) > this.distance.getTargetLostDistance(mob, newTarget)
            )) {
            blackboard.submitEvent(this.lastTargetInvalidEventType, new TargetAIEvent(newTarget));
            newTarget = null;
            addTargetLostDistance = true;
         }

         if (addTargetLostDistance) {
            this.distance.searchDistance = this.distance.searchDistance + this.distance.targetLostAddedDistance;
         }

         Mob focusTarget = blackboard.getObject(Mob.class, this.focusTargetKey);
         if (focusTarget != null
            && newTarget != focusTarget
            && this.validity.isValidTarget(this, mob, focusTarget, true)
            && this.distance.getDistance(base, focusTarget) < this.distance.getSearchDistance(mob, focusTarget)
            && mob.estimateCanMoveTo(focusTarget.getTileX(), focusTarget.getTileY(), focusTarget.canBeTargetedFromAdjacentTiles())) {
            newTarget = focusTarget;
         }

         if (newTarget == null) {
            if (baseKeyObject != null && this.baseKeyIsJustPreference && this.targetNonBaseWithinRange > 0) {
               GameAreaStream<? extends Mob> meTargetStream = this.streamPossibleTargets(mob, new Point(mob.getX(), mob.getY()), this.distance);
               if (meTargetStream != null) {
                  newTarget = meTargetStream.filter(
                        m -> this.validity.isValidTarget(this, mob, m, true)
                           && mob.getDistance(m) <= this.targetNonBaseWithinRange * this.distance.getSearchDistanceMod(mob, m)
                     )
                     .filter(m -> mob.estimateCanMoveTo(m.getTileX(), m.getTileY(), m.canBeTargetedFromAdjacentTiles()))
                     .findBestDistance(0, Comparator.comparingDouble(mob::getDistance))
                     .orElse(null);
               }

               if (newTarget == null) {
                  GameAreaStream<? extends Mob> baseTargetStream = this.streamPossibleTargets(mob, base, this.distance);
                  if (baseTargetStream != null) {
                     newTarget = baseTargetStream.filter(
                           m -> this.validity.isValidTarget(this, mob, m, true) && this.distance.getDistance(base, m) < this.distance.getSearchDistance(mob, m)
                        )
                        .filter(m -> mob.estimateCanMoveTo(m.getTileX(), m.getTileY(), m.canBeTargetedFromAdjacentTiles()))
                        .findBestDistance(0, Comparator.comparingDouble(m -> this.distance.getDistance(base, m)))
                        .orElse(null);
                  }
               }
            } else {
               GameAreaStream<? extends Mob> targetStream = this.streamPossibleTargets(mob, base, this.distance);
               if (targetStream != null) {
                  newTarget = targetStream.filter(
                        m -> this.validity.isValidTarget(this, mob, m, true) && this.distance.getDistance(base, m) < this.distance.getSearchDistance(mob, m)
                     )
                     .filter(m -> mob.estimateCanMoveTo(m.getTileX(), m.getTileY(), m.canBeTargetedFromAdjacentTiles()))
                     .findBestDistance(0, Comparator.comparingDouble(mob::getDistance))
                     .orElse(null);
               }
            }
         }

         if (this.canRegainSameTargetIfLostToTimer
            && newTarget == null
            && lostTargetToTimer != null
            && !lostTargetToTimer.removed()
            && this.validity.isValidTarget(this, mob, lostTargetToTimer, false)
            && this.distance.getDistance(base, lostTargetToTimer) <= this.distance.getTargetLostDistance(mob, lostTargetToTimer)) {
            newTarget = lostTargetToTimer;
         }

         if (newTarget == null && this.noTargetFoundMinCooldown >= 0 && this.noTargetFoundMaxCooldown >= 0) {
            this.startNoTargetFoundCooldown();
         }

         if (lastTarget != newTarget) {
            blackboard.put(this.currentTargetKey, newTarget);
            blackboard.submitEvent(this.newTargetFoundEventType, new TargetAIEvent(newTarget));
         }

         return newTarget != null ? AINodeResult.SUCCESS : AINodeResult.FAILURE;
      } finally {
         this.distance.searchDistance = lastSearchDistance;
      }
   }

   @Override
   public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
      Mob lastTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
      if (this.moveToAttacker && lastTarget == null) {
         for (AIWasHitEvent e : blackboard.getLastHits()) {
            Mob owner = e.event.attacker != null ? e.event.attacker.getAttackOwner() : null;
            Point baseKeyObject = this.getBase(mob, blackboard);
            if (owner != null
               && this.validity.isValidTarget(this, mob, owner, true)
               && mob.estimateCanMoveTo(owner.getTileX(), owner.getTileY(), owner.canBeTargetedFromAdjacentTiles())) {
               Point base = baseKeyObject != null ? baseKeyObject : new Point(mob.getX(), mob.getY());
               if (baseKeyObject == null || !(this.distance.getDistance(base, owner) > this.distance.getTargetLostDistance(mob, owner))) {
                  return this.moveToTileTask(owner.getTileX(), owner.getTileY(), null, path -> {
                     path.moveIfWithin(-1, 1, null);
                     Mob currentTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
                     if (currentTarget != owner) {
                        blackboard.put(this.currentTargetKey, owner);
                        blackboard.submitEvent(this.newTargetFoundEventType, new TargetAIEvent(owner));
                     }

                     return AINodeResult.SUCCESS;
                  });
               }
            }
         }
      }

      return !this.runOnGlobalTick ? this.tickTargetFinder(mob, blackboard) : AINodeResult.SUCCESS;
   }

   public Point getBase(T mob, Blackboard<T> blackboard) {
      return blackboard.getObject(Point.class, this.baseKey);
   }

   public void startLoseTargetTimer() {
      this.loseTargetTimer = GameRandom.globalRandom.getIntBetween(this.loseTargetMinCooldown, this.loseTargetMaxCooldown);
   }

   public void startNoTargetFoundCooldown() {
      this.noTargetFoundTimer = GameRandom.globalRandom.getIntBetween(this.noTargetFoundMinCooldown, this.noTargetFoundMaxCooldown);
   }

   public abstract GameAreaStream<? extends Mob> streamPossibleTargets(T var1, Point var2, TargetFinderDistance<T> var3);

   public static <T extends Mob> GameAreaStream<PlayerMob> streamPlayers(T mob, Point base, TargetFinderDistance<T> distance) {
      return distance.streamPlayersInRange(base, mob).filter(m -> m != null && m != mob && !m.removed() && m.isVisible());
   }

   public static <T extends Mob> GameAreaStream<Mob> streamPlayersAndHumans(T mob, Point base, TargetFinderDistance<T> distance) {
      return distance.streamMobsAndPlayersInRange(base, mob).filter(m -> {
         if (m != null && m != mob && !m.removed() && m.isVisible()) {
            int team = m.getTeam();
            if (team == -100) {
               return true;
            } else {
               return m.isHuman && team != -1 ? true : m.isPlayer;
            }
         } else {
            return false;
         }
      });
   }
}
