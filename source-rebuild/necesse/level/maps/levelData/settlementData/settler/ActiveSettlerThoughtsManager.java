package necesse.level.maps.levelData.settlementData.settler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;

public class ActiveSettlerThoughtsManager implements Iterable<ActiveSettlerThought> {
   public final HumanMob owner;
   private final LinkedHashMap<Integer, ActiveSettlerThought> thoughts;
   protected int cachedHappiness;
   protected boolean isHappinessDirty = true;

   public ActiveSettlerThoughtsManager(HumanMob owner) {
      this.owner = owner;
      this.thoughts = new LinkedHashMap<>();
   }

   public void addSaveData(SaveData save) {
      for (ActiveSettlerThought activeThought : this.thoughts.values()) {
         SaveData thoughtSave = new SaveData("");
         activeThought.addSaveData(thoughtSave);
         save.addSaveData(thoughtSave);
      }
   }

   public void applyLoadData(LoadData save) {
      this.thoughts.clear();

      for (LoadData thoughtSave : save.getLoadData()) {
         try {
            ActiveSettlerThought thought = ActiveSettlerThought.fromLoadData(this, thoughtSave);
            if (thought != null) {
               this.thoughts.put(thought.thought.getID(), thought);
            }
         } catch (Exception e) {
            System.err.println("Could not load thought");
         }
      }

      this.isHappinessDirty = true;
   }

   public void init() {
      LinkedList<Integer> invalidThoughtIDs = new LinkedList<>();

      for (ActiveSettlerThought activeThought : this.thoughts.values()) {
         SettlerPersonality personalitySubmitter = activeThought.thought.getPersonalitySubmitter();
         if (personalitySubmitter != null && activeThought.thought.shouldClearIfDoesntHavePersonalitySubmitter()) {
            boolean found = false;

            for (SettlerPersonality personality : this.owner.getPersonalities()) {
               if (personality.getID() == personalitySubmitter.getID()) {
                  found = true;
                  break;
               }
            }

            if (!found) {
               invalidThoughtIDs.add(activeThought.thought.getID());
            }
         }
      }

      for (int invalidThoughtID : invalidThoughtIDs) {
         this.thoughts.remove(invalidThoughtID);
         this.isHappinessDirty = true;
      }
   }

   public void tick() {
      HashSet<Integer> expiredThoughtIDs = new HashSet<>();

      for (ActiveSettlerThought thought : this.thoughts.values()) {
         thought.tick();
         if (thought.hasExpired()) {
            expiredThoughtIDs.add(thought.thought.getID());
         }
      }

      for (int expiredThoughtID : expiredThoughtIDs) {
         this.thoughts.remove(expiredThoughtID);
         this.isHappinessDirty = true;
      }
   }

   public void addThoughtMilliseconds(SettlerThought thought, int msDuration) {
      this.thoughts.compute(thought.getID(), (key, existing) -> {
         if (existing == null) {
            return new ActiveSettlerThought(this, thought, msDuration);
         }

         existing.addStack(msDuration);
         return (ActiveSettlerThought)existing;
      });
      this.isHappinessDirty = true;
   }

   public void addThoughtSeconds(SettlerThought thought, float secondsDuration) {
      this.addThoughtMilliseconds(thought, (int)(secondsDuration * 1000.0F));
   }

   public void addThoughtMilliseconds(String thoughtStringID, int msDuration) {
      SettlerThought thought = SettlerThoughtRegistry.getSettlerThought(thoughtStringID);
      Objects.requireNonNull(thought);
      this.addThoughtMilliseconds(thought, msDuration);
   }

   public void addThoughtSeconds(String thoughtStringID, float secondsDuration) {
      SettlerThought thought = SettlerThoughtRegistry.getSettlerThought(thoughtStringID);
      Objects.requireNonNull(thought);
      this.addThoughtSeconds(thought, secondsDuration);
   }

   public ActiveSettlerThought getThought(SettlerThought thought) {
      return this.thoughts.get(thought.getID());
   }

   public ActiveSettlerThought getThought(String thoughtStingID) {
      return this.thoughts.get(SettlerThoughtRegistry.getSettlerThoughtID(thoughtStingID));
   }

   public boolean hasThought(SettlerThought thought) {
      return this.thoughts.containsKey(thought.getID());
   }

   public boolean hasThought(String thoughtStringID) {
      return this.thoughts.containsKey(SettlerThoughtRegistry.getSettlerThoughtID(thoughtStringID));
   }

   public void removeThought(SettlerThought thought) {
      if (this.thoughts.remove(thought.getID()) != null) {
         this.isHappinessDirty = true;
      }
   }

   public void removeThought(String thoughtStringID) {
      if (this.thoughts.remove(SettlerThoughtRegistry.getSettlerThoughtID(thoughtStringID)) != null) {
         this.isHappinessDirty = true;
      }
   }

   public void clearStacksAbove(SettlerThought thought, int maxStacks) {
      ActiveSettlerThought activeThought = this.thoughts.get(thought.getID());
      if (activeThought != null && activeThought.clearStacksAbove(maxStacks)) {
         this.isHappinessDirty = true;
      }
   }

   public void clearStacksAbove(String thoughtStringID, int maxStacks) {
      SettlerThought thought = SettlerThoughtRegistry.getSettlerThought(thoughtStringID);
      Objects.requireNonNull(thought);
      this.clearStacksAbove(thought, maxStacks);
   }

   public void clear() {
      this.thoughts.clear();
      this.isHappinessDirty = true;
   }

   public boolean isEmpty() {
      return this.thoughts.isEmpty();
   }

   public int size() {
      return this.thoughts.size();
   }

   @Override
   public Iterator<ActiveSettlerThought> iterator() {
      return this.thoughts.values().iterator();
   }

   public Stream<ActiveSettlerThought> stream() {
      return this.thoughts.values().stream();
   }

   public int getTotalHappinessModifier() {
      if (this.isHappinessDirty) {
         this.cachedHappiness = 0;

         for (ActiveSettlerThought thought : this.thoughts.values()) {
            this.cachedHappiness = this.cachedHappiness + thought.getHappinessModifier(this.owner);
         }

         this.isHappinessDirty = false;
      }

      return this.cachedHappiness;
   }
}
