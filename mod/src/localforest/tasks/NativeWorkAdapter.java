package localforest.tasks;

import java.util.function.BiConsumer;
import necesse.engine.localization.message.GameMessage;
import necesse.entity.mobs.Mob;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.SingleJobSequence;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.ActiveJobTargetFoundResult;
import necesse.entity.mobs.job.activeJob.PerformLevelJobEventActiveJob;
import necesse.level.maps.levelData.jobs.HarvestCropLevelJob;
import necesse.level.maps.levelData.jobs.HarvestFruitLevelJob;
import necesse.level.maps.levelData.jobs.ForestryLevelJob;
import necesse.level.maps.levelData.jobs.HaulFromLevelJob;
import necesse.level.maps.levelData.jobs.StorePickupItemLevelJob;

/** Reuses native job search, pathing, inventory reservations and cancellation. */
public final class NativeWorkAdapter {
    public record BeginResult(boolean started,boolean retryable,String message) {}
    public static BeginResult begin(HumanMob mob,String action,BiConsumer<Boolean,String> result){
        // Native humans are intentionally unable to search for their first 5–10 seconds.
        // A new region also registers its initial tile jobs over several seconds. Neither
        // condition means that the player's crop/hauling request has no valid target.
        if(mob.getWorldEntity().isNight())return new BeginResult(false,false,"现在是休息时间，白天再安排这项工作");
        if(mob.isOnStrike())return new BeginResult(false,false,"伙伴正在罢工，请先改善饮食、住所与生活条件");
        if(mob.getCurrentMission()!=null)return new BeginResult(false,false,"伙伴正在执行原生远征，结束后再安排工作");
        if(mob.getJobTypeHandler().isOnGlobalCooldown(mob.getTime())){
            long seconds=(mob.getJobTypeHandler().globalCooldown-mob.getTime()+999)/1000;
            return new BeginResult(false,true,"等待原生工作冷却（约 "+seconds+" 秒），可随时取消");
        }
        clearCommands(mob,null);cancel(mob);mob.resetJobCancelled();
        JobSequence sequence=mob.findJob(true,finder->finder.levelJobPredicate=job->switch(action){
            case "HARVEST" -> job instanceof HarvestCropLevelJob||job instanceof HarvestFruitLevelJob;
            case "FORESTRY" -> job instanceof ForestryLevelJob;
            case "HAUL" -> job instanceof HaulFromLevelJob||job instanceof StorePickupItemLevelJob;
            default -> false;
        });
        if(sequence==null)return new BeginResult(false,true,action.equals("FORESTRY")?
            "等待原生林业作业：需要允许砍伐的林业区与可采树木":"等待原生作业就绪：正在检查成熟作物、可搬物品与聚落仓库");
        if(!sequence.hasNext()){sequence.cancel(false);return new BeginResult(false,true,"等待可执行的原生作业");}
        TrackingSequence tracked=new TrackingSequence(sequence,result);
        ActiveJob first=tracked.next();first.onMadeCurrent();
        mob.ai.blackboard.put("currentJobSequence",tracked);mob.ai.blackboard.put("currentJob",first);
        return new BeginResult(true,false,"原生任务执行中：寻路、工作与物品均使用原版系统");
    }
    public static void cancel(HumanMob mob){
        ActiveJob current=mob.ai.blackboard.getObject(ActiveJob.class,"currentJob");
        JobSequence sequence=mob.ai.blackboard.getObject(JobSequence.class,"currentJobSequence");
        try{if(current!=null)current.onCancelled(false,true,false);}
        finally{
            try{if(sequence!=null)sequence.cancel(false);}
            finally{mob.ai.blackboard.put("currentJob",null);mob.ai.blackboard.put("currentJobSequence",null);mob.cancelJob();mob.ai.blackboard.mover.stopMoving(mob);}
        }
    }
    /** Avoid the native low-health adventure-party return teleport when replacing a local task. */
    public static void clearCommands(HumanMob mob,ServerClient commander){mob.adventureParty.clear(true);mob.clearCommandsOrders(commander);}
    public static boolean isTracking(HumanMob mob){return mob.ai.blackboard.getObject(JobSequence.class,"currentJobSequence") instanceof TrackingSequence;}
    private static final class TrackingSequence implements JobSequence {
        private final JobSequence delegate;private final BiConsumer<Boolean,String> result;private boolean completed,observedNativeEvent;
        TrackingSequence(JobSequence delegate,BiConsumer<Boolean,String> result){this.delegate=delegate;this.result=result;}
        private void finish(boolean ok,String why){if(!completed){completed=true;result.accept(ok,why);}}
        public GameMessage getActivityDescription(){return delegate.getActivityDescription();}
        public void setActivityDescription(GameMessage m){delegate.setActivityDescription(m);}
        public void tick(){delegate.tick();}
        public boolean hasNext(){return delegate.hasNext();}
        public ActiveJob next(){return delegate.next();}
        public boolean isValid(){boolean valid=delegate.isValid();if(!valid)finish(false,"目标或存储条件已变化，请检查后继续");return valid;}
        public ActiveJobTargetFoundResult onTargetFound(Mob m){return delegate.onTargetFound(m);}
        public ActiveJobTargetFoundResult onFollowOutOfRange(Mob m){return delegate.onFollowOutOfRange(m);}
        public void cancel(boolean invalid){delegate.cancel(invalid);finish(false,invalid?"原生任务目标失效":"工作已被取消或危险打断");}
        public boolean isRecreationJob(){return delegate.isRecreationJob();}
        public void onJobFinished(ActiveJob j){
            delegate.onJobFinished(j);
            // Fruit harvesting uses SingleJobSequence.onJobFinished to emit its native
            // performed event directly, rather than adding a separate event ActiveJob.
            if(j instanceof PerformLevelJobEventActiveJob||delegate instanceof SingleJobSequence)observedNativeEvent=true;
            if(!delegate.hasNext())finish(observedNativeEvent,observedNativeEvent?"已完成一批原生工作，物品按真实作业处理":"作业结束但缺少完成事件，请检查现场");
        }
    }
    private NativeWorkAdapter(){}
}
