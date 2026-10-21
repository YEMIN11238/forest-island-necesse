package necesse.entity.mobs.job;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.registries.JobTypeRegistry;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class JobTypeHandler {
   public long globalCooldown;
   public int lastPerformedJobID = -1;
   public int prioritizeNextJobID = -1;
   public boolean resetPrioritizeNextJobIfFound = true;
   private final HashMap<Integer, JobTypeHandler.SubHandler<?>> handlers = new HashMap<>();
   private final HashMap<String, JobTypeHandler.TypePriority> priorities = new HashMap<>();

   public JobTypeHandler() {
      for (JobType type : JobTypeRegistry.getTypes()) {
         this.priorities.put(type.getStringID(), new JobTypeHandler.TypePriority(type));
      }
   }

   public JobTypeHandler.TypePriority getPriority(String jobType) {
      return this.priorities.get(jobType);
   }

   public JobTypeHandler.TypePriority getPriority(int jobTypeID) {
      JobType type = JobTypeRegistry.getJobType(jobTypeID);
      return type == null ? null : this.priorities.get(type.getStringID());
   }

   public <T extends AbstractLevelJob> JobTypeHandler.SubHandler<T> setJobHandler(Class<T> jobClass, Function<FoundJob<T>, JobSequence> sequenceFunction) {
      int jobID = LevelJobRegistry.getJobID(jobClass);
      if (jobID == -1) {
         throw new IllegalArgumentException("Job class must be registered");
      }

      JobTypeHandler.TypePriority priority = this.getPriority(LevelJobRegistry.getJobTypeID(jobClass));
      JobTypeHandler.SubHandler<T> handler = new JobTypeHandler.SubHandler<>(jobID, priority, sequenceFunction);
      this.handlers.put(jobID, handler);
      return handler;
   }

   public <T extends AbstractLevelJob> JobTypeHandler.SubHandler<T> getJobHandler(Class<T> jobClass) {
      int jobID = LevelJobRegistry.getJobID(jobClass);
      if (jobID == -1) {
         throw new IllegalArgumentException("Job class must be registered");
      } else {
         return (JobTypeHandler.SubHandler<T>)this.handlers.get(jobID);
      }
   }

   public JobTypeHandler.SubHandler<?> getJobHandler(int jobID) {
      return this.handlers.get(jobID);
   }

   public void startGlobalCooldown(long currentTime, int milliseconds) {
      this.globalCooldown = currentTime + milliseconds;
   }

   public boolean isOnGlobalCooldown(long currentTime) {
      return currentTime < this.globalCooldown;
   }

   public Collection<JobTypeHandler.SubHandler<?>> getJobHandlers() {
      return this.handlers.values();
   }

   public Collection<JobTypeHandler.TypePriority> getTypePriorities() {
      return this.priorities.values();
   }

   public Stream<FoundJobGenerator<? extends AbstractLevelJob>> streamJobs(
      EntityJobWorker worker, Rectangle searchBounds, PreSequenceComputes preSequenceComputes
   ) {
      Stream<FoundJobGenerator<? extends AbstractLevelJob>> out = null;
      boolean searchInLevelJobData = false;
      HashSet<Integer> invalidJobTypeIDs = new HashSet<>();

      for (JobTypeHandler.TypePriority priority : this.priorities.values()) {
         if (priority.disabledBySettler || priority.disabledByPlayer || !worker.allowsFindingJobType(priority.type)) {
            invalidJobTypeIDs.add(priority.type.getID());
         }
      }

      for (JobTypeHandler.SubHandler<?> value : this.handlers.values()) {
         if (!invalidJobTypeIDs.contains(value.priority.type.getID()) && value.canPerform(worker)) {
            if (value.searchInLevelJobData) {
               searchInLevelJobData = true;
            }

            if (value.extraJobStreamer != null) {
               Stream<? extends AbstractLevelJob> stream = (Stream<? extends AbstractLevelJob>)value.extraJobStreamer.stream(worker, value);
               if (stream != null) {
                  Stream<FoundJobGenerator<? extends AbstractLevelJob>> mappedStream = stream.map(
                     e -> new SubHandlerFoundJobGenerator(
                        (AbstractLevelJob)e, (JobTypeHandler.SubHandler<? extends AbstractLevelJob>)value, preSequenceComputes
                     )
                  );
                  if (out == null) {
                     out = mappedStream;
                  } else {
                     out = Stream.concat(out, mappedStream);
                  }
               }
            }
         }
      }

      if (searchInLevelJobData) {
         Stream<FoundJobGenerator<? extends AbstractLevelJob>> stream = worker.streamValidJobsWithinRange(searchBounds)
            .filter(job -> !invalidJobTypeIDs.contains(job.jobType.getID()))
            .map(e -> new HandlerFoundJobGenerator<>((AbstractLevelJob)e, this, preSequenceComputes));
         if (out == null) {
            out = stream;
         } else {
            out = Stream.concat(out, stream);
         }
      }

      return out == null ? Stream.empty() : out;
   }

   public interface JobStreamSupplier<T extends AbstractLevelJob> {
      Stream<T> stream(EntityJobWorker var1, JobTypeHandler.SubHandler<?> var2);
   }

   public static class SubHandler<T extends AbstractLevelJob> {
      public final int jobID;
      public final JobTypeHandler.TypePriority priority;
      public int minCooldownMS;
      public int maxCooldownMS;
      public boolean startCooldownAfterPerformed = true;
      public boolean disabledBySettler;
      public Supplier<Boolean> canPerform;
      public Function<FoundJob<T>, JobSequence> sequenceFunction;
      public BiFunction<EntityJobWorker, JobTypeHandler.SubHandler<T>, Object> preSequenceCompute;
      public long nextCooldownComplete;
      protected JobTypeHandler.JobStreamSupplier<? extends T> extraJobStreamer;
      protected boolean searchInLevelJobData = true;

      public SubHandler(int jobID, JobTypeHandler.TypePriority priority, Function<FoundJob<T>, JobSequence> sequenceFunction) {
         this.jobID = jobID;
         this.priority = priority;
         this.sequenceFunction = sequenceFunction;
      }

      public JobTypeHandler.SubHandler<T> cooldown(int minCooldownMS, int maxCooldownMS) {
         this.minCooldownMS = minCooldownMS;
         this.maxCooldownMS = maxCooldownMS;
         return this;
      }

      public JobTypeHandler.SubHandler<T> addPredicate(Supplier<Boolean> canPerform) {
         if (this.canPerform == null) {
            this.canPerform = canPerform;
         } else {
            Supplier<Boolean> last = this.canPerform;
            this.canPerform = () -> last.get() && canPerform.get();
         }

         return this;
      }

      public JobTypeHandler.SubHandler<T> setPredicate(Supplier<Boolean> canPerform) {
         this.canPerform = canPerform;
         return this;
      }

      public JobTypeHandler.SubHandler<T> setPreSequenceCompute(BiFunction<EntityJobWorker, JobTypeHandler.SubHandler<T>, Object> preSequenceCompute) {
         this.preSequenceCompute = preSequenceCompute;
         return this;
      }

      public JobTypeHandler.SubHandler<T> setExtraJobStreamer(boolean searchInLevelJobData, JobTypeHandler.JobStreamSupplier<? extends T> extraJobStreamer) {
         this.searchInLevelJobData = searchInLevelJobData;
         this.extraJobStreamer = extraJobStreamer;
         return this;
      }

      public void startCooldown(long currentTime) {
         this.nextCooldownComplete = currentTime + GameRandom.globalRandom.getIntBetween(this.minCooldownMS, this.maxCooldownMS);
      }

      public boolean isOnCooldown(long currentTime) {
         return currentTime <= this.nextCooldownComplete;
      }

      public boolean canPerform(EntityJobWorker worker) {
         if (this.disabledBySettler) {
            return false;
         } else {
            return this.isOnCooldown(worker.getMobWorker().getTime()) ? false : this.canPerform == null || this.canPerform.get();
         }
      }

      public Stream<? extends T> streamJobs(EntityJobWorker worker, Rectangle searchBounds) {
         ZoneTester restrictZone = worker.getJobRestrictZone();
         Stream<? extends T> out = null;
         if (this.searchInLevelJobData) {
            out = worker.streamValidJobsWithinRange(searchBounds)
               .filter(e -> e.getID() == this.jobID)
               .filter(e -> e.isWithinRestrictZone(restrictZone))
               .map(e -> (T)e);
         }

         if (this.extraJobStreamer != null) {
            Stream<? extends T> stream = this.extraJobStreamer.stream(worker, this);
            if (stream != null) {
               if (out == null) {
                  out = stream;
               } else {
                  out = Stream.concat(out, stream);
               }
            }
         }

         return out == null ? Stream.empty() : out;
      }

      public ComputedValue<Object> getPreSequenceCompute(EntityJobWorker worker) {
         return new ComputedValue<>(() -> this.preSequenceCompute == null ? null : this.preSequenceCompute.apply(worker, this));
      }

      public Stream<FoundJob<T>> streamFoundJobs(EntityJobWorker worker, Rectangle searchBounds) {
         Mob mob = worker.getMobWorker();
         ComputedValue<Object> preSequenceCompute = this.getPreSequenceCompute(worker);
         return this.streamJobs(worker, searchBounds)
            .filter(e -> e.reservable.isAvailable(mob))
            .map(e -> new HandlerFoundJob<>(worker, (T)e, this, this.priority, preSequenceCompute));
      }

      public Stream<FoundJob<T>> streamFoundJobsFiltered(EntityJobWorker worker, Rectangle searchBounds) {
         return this.streamFoundJobs(worker, searchBounds).filter(e -> e.priority == null || !e.priority.disabledBySettler && !e.priority.disabledByPlayer);
      }
   }

   public static class TypePriority {
      public final JobType type;
      public int priority;
      public boolean disabledBySettler;
      public boolean disabledByPlayer = false;

      public TypePriority(JobType type) {
         this.type = type;
         this.disabledBySettler = type.defaultDisabledBySettler;
      }

      public void loadSaveData(LoadData save) {
         this.priority = save.getInt("priority", this.priority, false);
         this.disabledByPlayer = save.getBoolean("disabledByPlayer", this.disabledByPlayer, false);
      }

      public void addSaveData(SaveData save) {
         save.addInt("priority", this.priority);
         save.addBoolean("disabledByPlayer", this.disabledByPlayer);
      }
   }
}
