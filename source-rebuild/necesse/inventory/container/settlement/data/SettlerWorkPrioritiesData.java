package necesse.inventory.container.settlement.data;

import java.util.HashMap;
import java.util.Map.Entry;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.JobTypeRegistry;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.JobType;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.level.maps.levelData.settlementData.LevelSettler;

public class SettlerWorkPrioritiesData {
   public final HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities;

   public SettlerWorkPrioritiesData(LevelSettler settler, EntityJobWorker worker) {
      this.priorities = new HashMap<>();

      for (JobTypeHandler.TypePriority priority : worker.getJobTypeHandler().getTypePriorities()) {
         if (priority.type.canChangePriority) {
            this.priorities
               .put(priority.type, new SettlerWorkPrioritiesData.TypePriority(priority.disabledBySettler, priority.priority, priority.disabledByPlayer));
         }
      }
   }

   public SettlerWorkPrioritiesData(PacketReader reader) {
      int size = reader.getNextShortUnsigned();
      this.priorities = new HashMap<>(size);

      for (int i = 0; i < size; i++) {
         int typeID = reader.getNextShortUnsigned();
         JobType jobType = JobTypeRegistry.getJobType(typeID);
         boolean disabledBySettler = reader.getNextBoolean();
         int priority = reader.getNextInt();
         boolean disabledByPlayer = reader.getNextBoolean();
         this.priorities.put(jobType, new SettlerWorkPrioritiesData.TypePriority(disabledBySettler, priority, disabledByPlayer));
      }
   }

   public void writeContentPacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.priorities.size());

      for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> e : this.priorities.entrySet()) {
         writer.putNextShortUnsigned(e.getKey().getID());
         SettlerWorkPrioritiesData.TypePriority priority = e.getValue();
         writer.putNextBoolean(priority.disabledBySettler);
         writer.putNextInt(priority.priority);
         writer.putNextBoolean(priority.disabledByPlayer);
      }
   }

   public static class TypePriority {
      public final boolean disabledBySettler;
      public int priority;
      public boolean disabledByPlayer;

      public TypePriority(boolean disabledBySettler, int priority, boolean disabledByPlayer) {
         this.disabledBySettler = disabledBySettler;
         this.priority = priority;
         this.disabledByPlayer = disabledByPlayer;
      }
   }
}
