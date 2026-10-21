package necesse.level.maps.levelData.settlementData.settler;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;

public class SettlerThoughtsList implements Iterable<SettlerThoughtStacks> {
   private final LinkedHashMap<Integer, SettlerThoughtStacks> thoughts;

   public SettlerThoughtsList(int initialCapacity) {
      this.thoughts = new LinkedHashMap<>(initialCapacity);
   }

   public SettlerThoughtsList() {
      this.thoughts = new LinkedHashMap<>();
   }

   public SettlerThoughtsList(SettlerThoughtStacks... initialThoughts) {
      this(initialThoughts.length);

      for (SettlerThoughtStacks thoughtStacks : initialThoughts) {
         this.addThought(thoughtStacks.thought, thoughtStacks.stacks);
      }
   }

   public SettlerThoughtsList(SettlerThought... initialThoughts) {
      this(initialThoughts.length);

      for (SettlerThought thoughtStacks : initialThoughts) {
         this.addThought(thoughtStacks);
      }
   }

   public SettlerThoughtsList(SettlerThoughtsList initialThoughts) {
      this(initialThoughts.size());
      this.addAll(initialThoughts);
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.thoughts.size());

      for (SettlerThoughtStacks thought : this.thoughts.values()) {
         writer.putNextShortUnsigned(thought.thought.getID());
         writer.putNextShortUnsigned(thought.stacks);
      }
   }

   public void addThought(SettlerThought thought, int stacks) {
      this.thoughts.compute(thought.getID(), (key, existing) -> {
         if (existing == null) {
            return new SettlerThoughtStacks(thought, stacks);
         }

         existing.stacks += stacks;
         return (SettlerThoughtStacks)existing;
      });
   }

   public void addThought(SettlerThought thought) {
      this.addThought(thought, 1);
   }

   public void addThought(String thoughtStringID, int stacks) {
      this.addThought(SettlerThoughtRegistry.getSettlerThought(thoughtStringID), stacks);
   }

   public void addThought(String thoughtStringID) {
      this.addThought(thoughtStringID, 1);
   }

   public SettlerThoughtStacks getThought(SettlerThought thought) {
      return this.thoughts.get(thought.getID());
   }

   public SettlerThoughtStacks getThought(String thoughtStingID) {
      return this.thoughts.get(SettlerThoughtRegistry.getSettlerThoughtID(thoughtStingID));
   }

   public void removeThought(SettlerThought thought) {
      Objects.requireNonNull(thought);
      this.thoughts.remove(thought.getID());
   }

   public void removeThought(String thoughtStringID) {
      this.removeThought(SettlerThoughtRegistry.getSettlerThought(thoughtStringID));
   }

   public void clear() {
      this.thoughts.clear();
   }

   public boolean isEmpty() {
      return this.thoughts.isEmpty();
   }

   public int size() {
      return this.thoughts.size();
   }

   @Override
   public Iterator<SettlerThoughtStacks> iterator() {
      return this.thoughts.values().iterator();
   }

   public Stream<SettlerThoughtStacks> stream() {
      return this.thoughts.values().stream();
   }

   public void addAll(SettlerThoughtsList other) {
      for (SettlerThoughtStacks otherThought : other.thoughts.values()) {
         this.addThought(otherThought.thought, otherThought.stacks);
      }
   }
}
