package necesse.entity.mobs.job;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.friendly.HusbandryMob;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.inventory.item.Item;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public abstract class JobWorkerChatterHandler {
   public final JobWorkerChatter chatter;
   public long nextLookForOtherTime = 0L;
   protected boolean invalidated;
   public boolean shouldSetFacingDirection;
   protected long lastValidTick;
   protected JobWorkerChatterHandler currentlyInteractingWith;
   protected boolean isControllingInteraction;
   protected int currentInteractionStageTicker;
   protected boolean interactionPositive;

   public JobWorkerChatterHandler(JobWorkerChatter chatter, boolean shouldSetFacingDirection) {
      this.chatter = chatter;
      this.shouldSetFacingDirection = shouldSetFacingDirection;
   }

   public JobWorkerChatterHandler getCurrentlyInteractingWith() {
      return this.currentlyInteractingWith;
   }

   public boolean isFreeToHaveConversation() {
      return this.currentlyInteractingWith == null && this.isStillValid();
   }

   public boolean canProgressConversation(JobWorkerChatterHandler other) {
      return true;
   }

   public void serverTick() {
      Mob chatterMob = this.getChatterMob();
      this.lastValidTick = chatterMob.getWorldEntity().getGameTicks();
      if (this.currentlyInteractingWith != null && !this.isStillValidConversation()) {
         if (this.currentlyInteractingWith.currentlyInteractingWith == this) {
            this.currentlyInteractingWith.endConversation(false, false);
         }

         this.endConversation(false, false);
      }

      if (this.currentlyInteractingWith != null) {
         if (this.isControllingInteraction) {
            this.serverTickConversation();
         } else if (chatterMob.ai.blackboard.mover.isMoving()) {
            chatterMob.ai.blackboard.mover.stopMoving(chatterMob);
         }
      }
   }

   public void invalidate() {
      this.invalidated = true;
   }

   public void startSearchCooldown() {
      this.nextLookForOtherTime = this.getChatterMob().getTime() + GameRandom.globalRandom.getIntBetween(15, 30) * 1000L;
   }

   public boolean isOnSearchCooldown() {
      return this.getChatterMob().getTime() < this.nextLookForOtherTime;
   }

   public JobWorkerChatterHandler lookForOtherToChatWith(int tileRange, boolean estimateCanMoveTo, boolean losRequired, boolean useZoneTester) {
      if (this.isOnSearchCooldown()) {
         return null;
      } else {
         this.startSearchCooldown();
         List<JobWorkerChatterHandler> list = findChattersToInteractWith(
            this.chatter, tileRange, estimateCanMoveTo, losRequired, useZoneTester ? this.getChatterRestrictZone() : null
         );
         if (!list.isEmpty()) {
            JobWorkerChatterHandler found = list.remove(GameRandom.globalRandom.nextInt(list.size()));
            this.startConversation(found);
            return found;
         } else {
            return null;
         }
      }
   }

   public void startConversation(JobWorkerChatterHandler other) {
      other.currentlyInteractingWith = this;
      this.currentlyInteractingWith = other;
      this.isControllingInteraction = true;
      this.currentInteractionStageTicker = 0;
   }

   protected boolean isStillValid() {
      return !this.invalidated && this.lastValidTick >= this.getChatterMob().getWorldEntity().getGameTicks() - 2L;
   }

   protected boolean isStillValidConversation() {
      return this.currentlyInteractingWith.currentlyInteractingWith != this ? false : this.isStillValid() && this.currentlyInteractingWith.isStillValid();
   }

   public boolean isInConversation() {
      return this.currentlyInteractingWith != null && this.currentlyInteractingWith.currentlyInteractingWith == this;
   }

   protected void serverTickConversation() {
      if (this.canProgressConversation(this.currentlyInteractingWith)) {
         Float interactionDir = GameMath.normalize(
            this.currentlyInteractingWith.getChatterMob().x - this.getChatterMob().x, this.currentlyInteractingWith.getChatterMob().y - this.getChatterMob().y
         );
         if (this.shouldSetFacingDirection) {
            this.chatter.getChatterMob().setFacingDir(interactionDir.x, interactionDir.y);
         }

         if (this.currentInteractionStageTicker == 60) {
            this.currentlyInteractingWith.getChatterMob().sendMovementPacket(false);
         }

         if (this.currentlyInteractingWith.shouldSetFacingDirection && this.currentInteractionStageTicker >= 60) {
            this.currentlyInteractingWith.getChatterMob().setFacingDir(-interactionDir.x, -interactionDir.y);
         }

         this.currentInteractionStageTicker++;
         this.onConversationGoing();
         if (this.currentInteractionStageTicker == 20) {
            this.getChatterMob().sendMovementPacket(false);
            List<JobWorkerChatter> chatters = this.findRandomChatter(30, 8);
            List<String> animals = this.findRandomAnimalStringIDs(30, 5);
            List<Item> foods = this.findRandomFoodItem(8);
            int totalTickets = chatters.size() + animals.size() + foods.size();
            if (totalTickets == 0) {
               animals.add(GameRandom.globalRandom.getOneOf("honeybee", "rabbit", "turtle", "duck"));
               totalTickets = 1;
            }

            int decidedTicket = GameRandom.globalRandom.nextInt(totalTickets);
            float positiveModifier = GameMath.limit(
               (this.getChatterPositiveInteractionModifier() + this.currentlyInteractingWith.getChatterPositiveInteractionModifier()) / 2.0F, 0.0F, 1.0F
            );
            if (decidedTicket < chatters.size()) {
               JobWorkerChatter target = chatters.get(decidedTicket);
               this.runAndSendOtherChatterThought(target, 6000);
               this.interactionPositive = GameRandom.globalRandom.getChance(GameMath.lerp(positiveModifier, 0.3F, 0.8F));
            } else if (decidedTicket < chatters.size() + animals.size()) {
               String animalStringID = animals.get(decidedTicket - chatters.size());
               this.runAndSendChatterMobThought(animalStringID, 6000);
               this.interactionPositive = GameRandom.globalRandom.getChance(GameMath.lerp(positiveModifier, 0.3F, 0.8F));
            } else {
               Item foodItem = foods.get(decidedTicket - chatters.size() - animals.size());
               this.runAndSendChatterItemThought(foodItem, 6000);
               this.interactionPositive = GameRandom.globalRandom.getChance(GameMath.lerp(positiveModifier, 0.3F, 0.7F))
                  || this.currentlyInteractingWith.getRecentFoodItemIDsEaten().stream().anyMatch(itemID -> itemID == foodItem.getID());
            }
         } else if (this.currentInteractionStageTicker == 80) {
            this.currentlyInteractingWith.getChatterMob().sendMovementPacket(false);
            if (this.interactionPositive) {
               this.currentlyInteractingWith.runAndSendLoveThought(6000);
               this.currentlyInteractingWith.runAndSendShowExpression(FormExpressionWheel.Expression.SURPRISED, 6000);
            } else {
               this.currentlyInteractingWith.runAndSendDisagreeThought(6000);
               this.currentlyInteractingWith.runAndSendShowExpression(FormExpressionWheel.Expression.BORED, 6000);
            }
         } else if (this.currentInteractionStageTicker == 160) {
            if (this.interactionPositive) {
               this.runAndSendExcitedThought(6000);
               this.runAndSendShowExpression(FormExpressionWheel.Expression.SURPRISED, 6000);
            } else {
               this.runAndSendShowExpression(FormExpressionWheel.Expression.ANGRY, 6000);
            }
         } else if (this.currentInteractionStageTicker >= 240) {
            this.currentlyInteractingWith.endConversation(true, this.interactionPositive);
            this.endConversation(true, this.interactionPositive);
         }
      }
   }

   protected void onConversationGoing() {
   }

   public void endConversation(boolean becauseConversationIsOver, boolean wasPositive) {
      this.currentlyInteractingWith = null;
      this.isControllingInteraction = false;
      if (becauseConversationIsOver) {
         this.getChatterMob().ai.blackboard.submitEvent("wanderNow", new AIEvent());
      }

      this.startSearchCooldown();
   }

   public static List<JobWorkerChatterHandler> findChattersToInteractWith(
      JobWorkerChatter chatter, int tileRange, boolean estimateCanMoveTo, boolean losRequired, ZoneTester zoneTester
   ) {
      Mob mob = chatter.getChatterMob();
      GameAreaStream<JobWorkerChatterHandler> stream = chatter.getLevel()
         .entityManager
         .mobs
         .streamAreaTileRange(mob.getX(), mob.getY(), tileRange)
         .filter(m -> m != mob)
         .filter(m -> m instanceof JobWorkerChatter)
         .map(m -> ((JobWorkerChatter)m).getCurrentChatterHandler())
         .filter(Objects::nonNull)
         .filter(JobWorkerChatterHandler::isFreeToHaveConversation);
      if (estimateCanMoveTo) {
         stream = stream.filter(h -> mob.estimateCanMoveTo(h.getChatterMob().getTileX(), h.getChatterMob().getTileY(), true));
      }

      if (losRequired) {
         stream = stream.filter(h -> hasLOS(mob, h.getChatterMob()));
      }

      if (zoneTester != null) {
         stream = stream.filter(h -> zoneTester.containsTile(h.getChatterMob().getTileX(), h.getChatterMob().getTileY()));
      }

      stream.filter(h -> h.getChatterMob().getDistance(mob) <= tileRange * 32);
      return stream.findExtraDistance(1, Collectors.toList());
   }

   public static boolean hasLOS(Mob from, Mob target) {
      Line2D line = new java.awt.geom.Line2D.Float(from.x, from.y, target.x, target.y);
      return !from.getLevel().collides(line, from.modifyChasingCollisionFilter(new CollisionFilter().wallCollision(), target));
   }

   protected List<JobWorkerChatter> findRandomChatter(int maxTileRange, int limit) {
      Mob mob = this.getChatterMob();
      return this.getLevel()
         .entityManager
         .mobs
         .streamAreaTileRange(mob.getX(), mob.getY(), maxTileRange)
         .filter(m -> m != mob && m != this.currentlyInteractingWith.chatter)
         .filter(m -> m instanceof JobWorkerChatter)
         .map(m -> (JobWorkerChatter)m)
         .findExtraItems(limit, Collectors.toList());
   }

   protected List<String> findRandomAnimalStringIDs(int maxTileRange, int limit) {
      Mob mob = this.getChatterMob();
      return this.getLevel()
         .entityManager
         .mobs
         .streamAreaTileRange(mob.getX(), mob.getY(), maxTileRange)
         .filter(m -> m != mob && m != this.currentlyInteractingWith.chatter)
         .filter(m -> m instanceof HusbandryMob)
         .map(m -> (HusbandryMob)m)
         .filter(HusbandryMob::canBeUsedAsSettlerThought)
         .map(Mob::getStringID)
         .findExtraItems(limit, Collectors.toList());
   }

   protected List<Item> findRandomFoodItem(int limit) {
      ArrayList<Item> list = new ArrayList<>(limit);

      for (int itemID : this.getRecentFoodItemIDsEaten()) {
         Item item = ItemRegistry.getItem(itemID);
         if (item != null) {
            list.add(item);
            if (list.size() >= limit) {
               break;
            }
         }
      }

      return list;
   }

   public Mob getChatterMob() {
      return this.chatter.getChatterMob();
   }

   public Level getLevel() {
      return this.chatter.getLevel();
   }

   public abstract ZoneTester getChatterRestrictZone();

   public abstract Collection<Integer> getRecentFoodItemIDsEaten();

   public abstract float getChatterPositiveInteractionModifier();

   public abstract void runAndSendOtherChatterThought(JobWorkerChatter var1, int var2);

   public abstract void runAndSendChatterMobThought(String var1, int var2);

   public abstract void runAndSendChatterItemThought(Item var1, int var2);

   public abstract void runAndSendLoveThought(int var1);

   public abstract void runAndSendExcitedThought(int var1);

   public abstract void runAndSendDisagreeThought(int var1);

   public abstract void runAndSendShowExpression(FormExpressionWheel.Expression var1, int var2);
}
