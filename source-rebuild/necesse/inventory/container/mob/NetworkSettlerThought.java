package necesse.inventory.container.mob;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.levelData.settlementData.settler.ActiveSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtStacks;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;

public class NetworkSettlerThought {
   public SettlerThought thought;
   public int stacks = 0;
   public long firstExpirationTime = -1L;

   public NetworkSettlerThought(ActiveSettlerThought thought) {
      this.thought = thought.thought;
      this.addStacks(thought);
   }

   public NetworkSettlerThought(SettlerThoughtStacks thought) {
      this.thought = thought.thought;
      this.addStacks(thought);
   }

   public void addStacks(ActiveSettlerThought activeThought) {
      if (this.thought != activeThought.thought) {
         throw new IllegalArgumentException("Cannot stack different thoughts");
      }

      this.stacks = this.stacks + activeThought.getStacks();
      long expiration = activeThought.getFirstStackExpirationTime();
      if (expiration >= 0L && expiration < this.firstExpirationTime) {
         this.firstExpirationTime = expiration;
      }
   }

   public void addStacks(SettlerThoughtStacks staticThought) {
      if (this.thought != staticThought.thought) {
         throw new IllegalArgumentException("Cannot stack different thoughts");
      }

      this.stacks = this.stacks + staticThought.stacks;
   }

   public NetworkSettlerThought(PacketReader reader) {
      int thoughtID = reader.getNextShortUnsigned();
      this.thought = SettlerThoughtRegistry.getSettlerThought(thoughtID);
      this.stacks = reader.getNextShortUnsigned();
      if (reader.getNextBoolean()) {
         this.firstExpirationTime = reader.getNextLong();
      } else {
         this.firstExpirationTime = -1L;
      }
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.thought.getID());
      writer.putNextShortUnsigned(this.stacks);
      if (this.firstExpirationTime != -1L) {
         writer.putNextBoolean(true);
         writer.putNextLong(this.firstExpirationTime);
      } else {
         writer.putNextBoolean(false);
      }
   }

   public GameMessage getDescription(HumanMob mob) {
      return this.thought.getStackedDescription(mob, this.stacks);
   }

   public int getHappinessModifier(HumanMob mob) {
      return this.thought.getStackedHappinessModifier(mob, this.stacks);
   }
}
