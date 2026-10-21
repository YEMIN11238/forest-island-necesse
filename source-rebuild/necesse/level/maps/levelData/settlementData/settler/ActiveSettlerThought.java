package necesse.level.maps.levelData.settlementData.settler;

import java.util.Comparator;
import java.util.LinkedList;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;

public class ActiveSettlerThought {
   protected ActiveSettlerThoughtsManager manager;
   public final SettlerThought thought;
   private final LinkedList<ActiveSettlerThought.StackTime> stackTimes;

   private ActiveSettlerThought(ActiveSettlerThoughtsManager manager, SettlerThought thought) {
      this.manager = manager;
      this.thought = thought;
      this.stackTimes = new LinkedList<>();
   }

   public ActiveSettlerThought(ActiveSettlerThoughtsManager manager, SettlerThought thought, int msDuration) {
      this(manager, thought);
      this.stackTimes.add(new ActiveSettlerThought.StackTime(msDuration));
   }

   public void addSaveData(SaveData save) {
      save.addUnsafeString("stringID", this.thought.getStringID());
      if (!this.stackTimes.isEmpty()) {
         if (this.stackTimes.size() == 1) {
            ActiveSettlerThought.StackTime first = this.stackTimes.getFirst();
            save.addInt("duration", first.getDurationLeft());
         } else {
            save.addInt("stacks", this.stackTimes.size());
            int counter = 0;

            for (ActiveSettlerThought.StackTime stack : this.stackTimes) {
               save.addInt("stack" + counter, stack.getDurationLeft());
               counter++;
            }
         }
      }
   }

   public static ActiveSettlerThought fromLoadData(ActiveSettlerThoughtsManager manager, LoadData save) {
      String stringID = save.getUnsafeString("stringID");
      SettlerThought settlerThought = SettlerThoughtRegistry.getSettlerThought(stringID);
      if (settlerThought == null) {
         return null;
      }

      ActiveSettlerThought thought = new ActiveSettlerThought(manager, settlerThought);
      int stacks = save.getInt("stacks", 1, false);
      if (stacks <= 1) {
         int duration = save.getInt("duration", 0, false);
         thought.addStack(duration);
      } else {
         for (int i = 0; i < stacks; i++) {
            int duration = save.getInt("stack" + i, -1, false);
            if (duration > 0) {
               thought.addStack(duration);
            }
         }
      }

      return thought;
   }

   public GameMessage getDescription(HumanMob mob) {
      return this.thought.getStackedDescription(mob, this.stackTimes.size());
   }

   public int getHappinessModifier(HumanMob mob) {
      return this.thought.getStackedHappinessModifier(mob, this.stackTimes.size());
   }

   public void addStack(int msDuration) {
      GameUtils.insertSortedList(
         this.stackTimes, new ActiveSettlerThought.StackTime(msDuration), Comparator.comparingInt(ActiveSettlerThought.StackTime::getDurationLeft)
      );
      this.clearStacksAbove(this.thought.getMaxStacks(this.manager.owner));
   }

   public boolean clearStacksAbove(int maxStacks) {
      boolean removedAny;
      for (removedAny = false; this.stackTimes.size() > 1 && this.stackTimes.size() > maxStacks; removedAny = true) {
         this.stackTimes.removeFirst();
      }

      return removedAny;
   }

   public void tick() {
      if (this.stackTimes.size() > 1) {
         ActiveSettlerThought.StackTime first = this.stackTimes.getFirst();
         if (first.getDurationLeft() <= 0) {
            this.stackTimes.removeFirst();
         }
      }
   }

   public boolean hasExpired() {
      return this.stackTimes.getFirst().getDurationLeft() <= 0;
   }

   protected long getTime() {
      return this.manager == null ? -1L : this.manager.owner.getTime();
   }

   public int getStacks() {
      return this.stackTimes.size();
   }

   public long getFirstStackExpirationTime() {
      return this.stackTimes.isEmpty() ? -1L : this.stackTimes.getFirst().getExpirationTime();
   }

   public class StackTime {
      public int duration;
      public long startTime;

      private StackTime(int duration) {
         this.duration = duration;
         this.startTime = ActiveSettlerThought.this.getTime();
      }

      public int getDurationLeft() {
         if (this.startTime <= 0L) {
            this.startTime = ActiveSettlerThought.this.getTime();
            return this.duration;
         } else {
            return (int)(this.duration + this.startTime - ActiveSettlerThought.this.getTime());
         }
      }

      public long getExpirationTime() {
         if (this.startTime <= 0L) {
            this.startTime = ActiveSettlerThought.this.getTime();
            if (this.startTime <= 0L) {
               return -1L;
            }
         }

         return this.startTime + this.duration;
      }
   }
}
