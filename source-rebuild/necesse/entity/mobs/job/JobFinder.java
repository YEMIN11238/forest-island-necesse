package necesse.entity.mobs.job;

import java.awt.Rectangle;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.registries.JobTypeRegistry;
import necesse.entity.mobs.Mob;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.ZoneTester;

public class JobFinder {
   public final EntityJobWorker worker;
   public final JobTypeHandler handler;
   public final Mob mob;
   protected final PreSequenceComputes preSequenceComputes = new PreSequenceComputes();
   public Predicate<AbstractLevelJob> levelJobPredicate;
   public ZoneTester zoneTester;
   public Rectangle searchBounds;

   public JobFinder(EntityJobWorker worker) {
      this.worker = worker;
      this.mob = worker.getMobWorker();
      this.handler = worker.getJobTypeHandler();
   }

   public Stream<FoundJob> streamFoundJobs(boolean ignoreRecreationJobs) {
      if (this.handler == null) {
         return Stream.empty();
      }

      ZoneTester restrictZone = this.zoneTester != null ? this.zoneTester : this.worker.getJobRestrictZone();
      Stream<FoundJobGenerator<? extends AbstractLevelJob>> stream = this.handler.streamJobs(this.worker, this.searchBounds, this.preSequenceComputes);
      Stream<FoundJobGenerator<?>> extraJobs = this.worker.streamExtraJobs();
      if (extraJobs != null) {
         stream = Stream.concat(stream, extraJobs);
      }

      stream = stream.filter(e -> e.isAvailable(this.mob)).filter(e -> e.isWithinRestrictZone(restrictZone));
      if (ignoreRecreationJobs) {
         stream = stream.filter(e -> e.levelJob.jobType.getID() != JobTypeRegistry.recreationID);
      }

      if (this.levelJobPredicate != null) {
         stream = stream.filter(e -> e.testPredicate(this.levelJobPredicate));
      }

      return stream.<FoundJob>map(e -> e.generateJob(this.worker)).filter(Objects::nonNull).filter(e -> !e.isDisabled());
   }

   public FoundJob<?> findJob(boolean ignoreRecreationJobs) {
      Comparator<FoundJob> comparator = Comparator.comparingInt(e -> -e.job.getFirstPriority((FoundJob<?>)e));
      comparator = comparator.thenComparingInt(e -> this.handler.prioritizeNextJobID == e.job.getID() ? 0 : 1)
         .thenComparingInt(e -> this.handler.lastPerformedJobID == e.job.getID() ? 0 : 1)
         .thenComparingInt(e -> -e.job.getAfterPrioritizedPriority((FoundJob<?>)e))
         .thenComparingInt(e -> e.priority == null ? 100000 : -e.priority.priority)
         .thenComparingInt(e -> e.priority == null ? 100000 : -e.priority.type.getID())
         .thenComparingInt(e -> -e.job.getSameTypePriority((FoundJob<?>)e))
         .thenComparing((o1, o2) -> -Integer.compare(o1.job.getSameJobPriority(), o2.job.getSameJobPriority()))
         .thenComparingDouble(FoundJob::getDistanceFromWorker);
      Stream<FoundJob> stream = this.streamFoundJobs(ignoreRecreationJobs).sorted(comparator).filter(FoundJob::canMoveTo).filter(e -> e.getSequence() != null);
      if (ignoreRecreationJobs) {
         stream = stream.filter(e -> !e.getSequence().isRecreationJob());
      }

      return stream.findFirst().orElse(null);
   }
}
