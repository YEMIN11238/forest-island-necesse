package necesse.entity.mobs.job;

import java.util.HashMap;
import java.util.function.Supplier;
import necesse.engine.util.ComputedValue;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;

public class PreSequenceComputes {
   private final HashMap<Integer, ComputedValue<Object>> preSequenceComputes = new HashMap<>();

   public ComputedValue<Object> getCompute(AbstractLevelJob levelJob, Supplier<ComputedValue<Object>> getter) {
      return this.preSequenceComputes.compute(levelJob.getID(), (id, last) -> last != null ? last : getter.get());
   }
}
