package necesse.inventory.container.events;

import java.util.ArrayList;
import java.util.HashMap;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.container.mob.NetworkSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.ActiveSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtStacks;

public class ShopThoughtsUpdateEvent extends ContainerEvent {
   public int qualityOfLifeLevel;
   public ArrayList<NetworkSettlerThought> thoughts;

   public ShopThoughtsUpdateEvent(HumanMob mob) {
      this.qualityOfLifeLevel = mob.getSettlerQualityOfLife();
      HashMap<Integer, NetworkSettlerThought> thoughtsByID = new HashMap<>();

      for (SettlerThoughtStacks staticThought : mob.getStaticThoughts()) {
         thoughtsByID.compute(staticThought.thought.getID(), (id, existing) -> {
            if (existing == null) {
               return new NetworkSettlerThought(staticThought);
            }

            existing.addStacks(staticThought);
            return (NetworkSettlerThought)existing;
         });
      }

      for (ActiveSettlerThought activeThought : mob.getActiveThoughts()) {
         thoughtsByID.compute(activeThought.thought.getID(), (id, existing) -> {
            if (existing == null) {
               return new NetworkSettlerThought(activeThought);
            }

            existing.addStacks(activeThought);
            return (NetworkSettlerThought)existing;
         });
      }

      this.thoughts = new ArrayList<>(thoughtsByID.values());
   }

   public ShopThoughtsUpdateEvent(PacketReader reader) {
      super(reader);
      this.qualityOfLifeLevel = reader.getNextInt();
      int size = reader.getNextShortUnsigned();
      this.thoughts = new ArrayList<>(size);

      for (int i = 0; i < size; i++) {
         this.thoughts.add(new NetworkSettlerThought(reader));
      }
   }

   @Override
   public void write(PacketWriter writer) {
      writer.putNextInt(this.qualityOfLifeLevel);
      writer.putNextShortUnsigned(this.thoughts.size());

      for (NetworkSettlerThought thought : this.thoughts) {
         thought.writePacket(writer);
      }
   }

   public int getTotalHappiness(HumanMob mob) {
      int total = 0;

      for (NetworkSettlerThought thought : this.thoughts) {
         total += thought.getHappinessModifier(mob);
      }

      return total;
   }
}
