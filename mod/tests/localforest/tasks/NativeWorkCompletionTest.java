package localforest.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobResult;
import necesse.entity.mobs.job.activeJob.SimplePerformActiveJob;

/** Regression for native fruit work's direct completion callback and cancel-before-finish. */
public final class NativeWorkCompletionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static JobSequence wrap(JobSequence sequence,List<Boolean> results)throws Exception{
        var c=Class.forName("localforest.tasks.NativeWorkAdapter$TrackingSequence").getDeclaredConstructor(JobSequence.class,BiConsumer.class);
        c.setAccessible(true);return (JobSequence)c.newInstance(sequence,(BiConsumer<Boolean,String>)(ok,message)->results.add(ok));
    }
    private static ActiveJob finishedJob(){return new SimplePerformActiveJob(null,null){public ActiveJobResult perform(){return ActiveJobResult.FINISHED;}};}
    public static void main(String[] args)throws Exception{
        List<Boolean> results=new ArrayList<>();int[] nativeCallbacks={0};
        JobSequence seq=wrap(new SingleJobSequence(finishedJob(),null){public void onJobFinished(ActiveJob j){nativeCallbacks[0]++;}},results);
        ActiveJob active=seq.next();check(results.isEmpty(),"Dequeuing work must not count as completion");
        seq.onJobFinished(active);seq.cancel(false);
        check(nativeCallbacks[0]==1&&results.equals(List.of(true)),"Single native completion callback was lost or overwritten by cancellation");
        results.clear();seq=wrap(new SingleJobSequence(finishedJob(),null),results);active=seq.next();seq.cancel(false);seq.onJobFinished(active);
        check(results.equals(List.of(false)),"Cancelled work was later promoted to success");
        System.out.println("PASS: native single-job completion; exactly-once result; cancel/finish race");
    }
}
