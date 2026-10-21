package necesse.inventory.container.settlement.events;

import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.JobTypeRegistry;
import necesse.entity.mobs.job.JobType;
import necesse.inventory.container.settlement.data.SettlerWorkPrioritiesData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class SettlementSettlerPrioritiesChangedEvent extends SettlementContainerEvent {
   public final int mobUniqueID;
   public final boolean includeDisabledBySettler;
   public final HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities;

   public SettlementSettlerPrioritiesChangedEvent(ServerSettlementData data, int mobUniqueID, JobType jobType, int priority, boolean disabledByPlayer) {
      super(data);
      this.mobUniqueID = mobUniqueID;
      this.includeDisabledBySettler = false;
      this.priorities = new HashMap<>();
      this.priorities.put(jobType, new SettlerWorkPrioritiesData.TypePriority(false, priority, disabledByPlayer));
   }

   public SettlementSettlerPrioritiesChangedEvent(
      ServerSettlementData data, int mobUniqueID, boolean includeDisabledBySettler, HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities
   ) {
      super(data);
      this.mobUniqueID = mobUniqueID;
      this.includeDisabledBySettler = includeDisabledBySettler;
      this.priorities = priorities;
   }

   public SettlementSettlerPrioritiesChangedEvent(
      ServerSettlementData data, int mobUniqueID, HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities
   ) {
      this(data, mobUniqueID, false, priorities);
   }

   public SettlementSettlerPrioritiesChangedEvent(PacketReader reader) {
      super(reader);
      this.mobUniqueID = reader.getNextInt();
      this.includeDisabledBySettler = reader.getNextBoolean();
      int prioritiesSize = reader.getNextShortUnsigned();
      this.priorities = new HashMap<>(prioritiesSize);

      for (int i = 0; i < prioritiesSize; i++) {
         int jobTypeID = reader.getNextShortUnsigned();
         boolean disabledBySettler = this.includeDisabledBySettler && reader.getNextBoolean();
         JobType jobType = JobTypeRegistry.getJobType(jobTypeID);
         int priority = reader.getNextInt();
         boolean disabledByPlayer = reader.getNextBoolean();
         if (jobType != null) {
            this.priorities.put(jobType, new SettlerWorkPrioritiesData.TypePriority(disabledBySettler, priority, disabledByPlayer));
         }
      }
   }

   @Override
   public void settlementWrite(PacketWriter writer) {
      writer.putNextInt(this.mobUniqueID);
      writer.putNextBoolean(this.includeDisabledBySettler);
      writer.putNextShortUnsigned(this.priorities.size());

      for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> e : this.priorities.entrySet()) {
         writer.putNextShortUnsigned(e.getKey().getID());
         if (this.includeDisabledBySettler) {
            writer.putNextBoolean(e.getValue().disabledBySettler);
         }

         writer.putNextInt(e.getValue().priority);
         writer.putNextBoolean(e.getValue().disabledByPlayer);
      }
   }
}
