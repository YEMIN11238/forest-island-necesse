package localforest.tasks;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.CompanionView;
import localforest.world.WorldDefinition;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.job.activeJob.MineObjectActiveJob;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.Inventory;
import necesse.level.gameObject.SeedObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.HarvestCropLevelJob;

/** Isolated-QA-only observer. Every task goes through the production client queue.
 * It never places crops, creates loot, edits inventory, or teleports a worker. */
public final class NativeHarvestCheck {
    @FunctionalInterface public interface CommandSink {void submit(CompanionView companion,String action,int x,int y);}
    private final WorldDefinition world;
    private final CommandSink commands;
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Map<String,Object>> completed=new ArrayList<>();
    private String workerId="",otherId="",taskId="",cancelledTaskId="";
    private int workerMob,phase,targetX,targetY,matureBefore,wheatBefore;
    private long waitingRevision,phaseStart;
    private boolean sawNativeWork;
    private Point staging;

    public NativeHarvestCheck(WorldDefinition world,CommandSink commands){this.world=world;this.commands=commands;}
    public Map<String,Object> evidence(){return evidence;}
    public String companionId(){return workerId;}
    public int mobId(){return workerMob;}
    public String latestTaskId(){return taskId;}

    public boolean tick(ServerClient server,Client client,long now){
        if(client==null)return false;
        Level level=server.getLevel();List<CompanionView> all=CompanionSystem.snapshot(client).companions();
        if(phase==0){
            if(all.size()<2)return false;
            require(!level.getWorldEntity().isNight(),"HARVEST probe requires daytime; do not combine it with visualProbe.hour=21");
            CompanionView worker=all.stream().filter(c->c.name().contains("禾")).findFirst().orElseThrow();
            workerId=worker.id();workerMob=worker.mobId();otherId=all.stream().filter(c->!c.id().equals(workerId)).findFirst().orElseThrow().id();
            HumanMob other=mob(level,view(all,otherId).mobId());
            staging=findStaging(level,mob(level,workerMob));
            evidence.put("initialMatureCrops",mature(level));evidence.put("noInventoryInjection",true);
            issue(view(all,otherId),"GUARD",other.getTileX(),other.getTileY(),1,now);return false;
        }
        require(now-phaseStart<45_000_000_000L,"HARVEST probe timed out in phase "+phase+": "+evidence.get("latest"));
        CompanionView worker=view(all,workerId);HumanMob human=mob(level,workerMob);
        evidence.put("latest",Map.of("phase",phase,"state",worker.state(),"message",worker.message(),"revision",worker.revision(),
            "tile",List.of(human.getTileX(),human.getTileY()),"tracking",NativeWorkAdapter.isTracking(human),"matureCrops",mature(level),"wheat",wheat(level,server)));
        if(worker.state().equals("待执行")||worker.state().equals("遇到阻碍")){
            var handler=human.getJobTypeHandler().getJobHandler(HarvestCropLevelJob.class);
            evidence.put("jobSearch",Map.of("globalCooldownRemaining",Math.max(0,human.getJobTypeHandler().globalCooldown-human.getTime()),
                "registeredCropJobs",human.streamValidJobsWithinRange(null).filter(j->j instanceof HarvestCropLevelJob).count(),
                "handlerCanPerform",handler!=null&&handler.canPerform(human),"inventoryFull",human.getWorkInventory().isFull()));
        }
        require(!worker.downed(),"Worker became downed during harvest probe");
        if(!cancelledTaskId.isEmpty())require(worker.memories().stream().noneMatch(e->e.type().equals("task_completed")&&e.source().equals(cancelledTaskId)),"Cancelled task later emitted completion");
        switch(phase){
            case 1 -> {if(accepted(view(all,otherId),"GUARD"))issue(worker,"GUARD",staging.x,staging.y,2,now);}
            case 2,5,8 -> {
                if(accepted(worker,"GUARD")&&!human.commandMoveToGuardPoint&&human.commandGuardPoint!=null){
                    require(Math.hypot(human.x-(staging.x*32+16),human.y-(staging.y*32+16))<64,"Worker did not walk to staging point");
                    matureBefore=mature(level);wheatBefore=wheat(level,server);require(matureBefore>0,"No original mature wheat remains");
                    sawNativeWork=false;taskId="";
                    issue(worker,"HARVEST",staging.x,staging.y,phase==2?3:phase==5?6:9,now);
                }
            }
            case 3,6,9 -> {
                if(!accepted(worker,"HARVEST"))break;
                taskId=worker.memories().stream().filter(e->e.type().equals("task_accepted")).map(CompanionSystem.MemoryEvent::source).reduce((a,b)->b).orElseThrow();
                ActiveJob job=human.ai.blackboard.getObject(ActiveJob.class,"currentJob");
                if(job instanceof MineObjectActiveJob mine&&NativeWorkAdapter.isTracking(human)){
                    require(isOriginalCrop(mine.tileX,mine.tileY)&&level.getObject(mine.tileX,mine.tileY) instanceof SeedObject,"HARVEST selected something other than original camp wheat");
                    targetX=mine.tileX;targetY=mine.tileY;sawNativeWork=true;
                    evidence.put("nativeJobClass",job.getClass().getName());
                    if(phase==6){
                        require(Math.hypot(human.x-(targetX*32+16),human.y-(targetY*32+16))>64,"Cancel case started too close to crop to verify in-flight cancellation");
                        cancelledTaskId=taskId;issue(worker,"CANCEL",staging.x,staging.y,7,now);
                    }else{phase=phase==3?4:10;phaseStart=now;}
                }
                require(!worker.state().equals("已完成")||sawNativeWork,"Work finished without observing its native movement/job stage");
            }
            case 4,10 -> {
                failIfBlocked(worker);
                if(worker.state().equals("已完成")){
                    require(worker.completedSteps()==1&&sawNativeWork,"Task success lacked a native job execution");
                    require(worker.memories().stream().anyMatch(e->e.type().equals("task_completed")&&e.source().equals(taskId)),"Native completion event missing");
                    require(!isMature(level,targetX,targetY)&&mature(level)<matureBefore,"Harvest did not change the original mature crop");
                    require(wheat(level,server)>wheatBefore,"Harvest produced no real wheat in native inventories/pickups");
                    completed.add(Map.of("task",taskId,"target",List.of(targetX,targetY),"matureBefore",matureBefore,"matureAfter",mature(level),"wheatBefore",wheatBefore,"wheatAfter",wheat(level,server),"steps",worker.completedSteps()));
                    if(phase==4)issue(worker,"GUARD",staging.x,staging.y,5,now);
                    else{
                        evidence.put("completedBatches",completed);evidence.put("cancelledTask",cancelledTaskId);evidence.put("cancelThenReexecutePassed",true);
                        evidence.put("claim","Real crop harvest and native inventory/pickup output; harvest does not promise warehouse delivery.");
                        evidence.put("passed",true);return true;
                    }
                }
            }
            case 7 -> {
                if(accepted(worker,"HARVEST")&&worker.state().equals("已取消")){
                    require(!NativeWorkAdapter.isTracking(human),"Cancellation left tracked native work running");
                    require(isMature(level,targetX,targetY),"Cancelled in-flight harvest still destroyed its target");
                    evidence.put("cancelledBeforeHarvest",true);issue(worker,"GUARD",staging.x,staging.y,8,now);
                }
            }
            default -> throw new IllegalStateException("Invalid harvest probe phase "+phase);
        }
        return false;
    }
    private void issue(CompanionView view,String action,int x,int y,int next,long now){waitingRevision=view.revision();phase=next;phaseStart=now;commands.submit(view,action,x,y);}
    private boolean accepted(CompanionView v,String action){if(v.revision()<=waitingRevision)return false;failIfBlocked(v);return v.action().equals(action);}
    private static void failIfBlocked(CompanionView v){require(!v.state().equals("遇到阻碍")&&!v.state().equals("未完成"),"Native task blocked: "+v.message());}
    private static CompanionView view(List<CompanionView> all,String id){return all.stream().filter(v->v.id().equals(id)).findFirst().orElseThrow();}
    private static HumanMob mob(Level level,int id){return level.entityManager.mobs.stream().filter(m->m instanceof HumanMob&&m.getUniqueID()==id).map(m->(HumanMob)m).findFirst().orElseThrow();}
    private Point findStaging(Level level,HumanMob worker){
        for(int x=world.campX+1;x<=world.campX+3;x++)for(int y=world.campY+2;y<=world.campY+4;y++)
            if(!level.isSolidTile(x,y)&&!level.isLiquidTile(x,y)&&worker.estimateCanMoveTo(x,y,false))return new Point(x,y);
        throw new IllegalStateException("No walkable staging point in original camp");
    }
    private boolean isOriginalCrop(int x,int y){return x>=world.campX-9&&x<=world.campX-6&&y>=world.campY+5&&y<=world.campY+7;}
    private static boolean isMature(Level level,int x,int y){return level.getObject(x,y) instanceof SeedObject seed&&seed.isLastStage();}
    private int mature(Level level){int count=0;for(int x=world.campX-9;x<=world.campX-6;x++)for(int y=world.campY+5;y<=world.campY+7;y++)if(isMature(level,x,y))count++;return count;}
    private int wheat(Level level,ServerClient client){
        int total=amount(client.playerMob.getInv().main);
        total+=level.entityManager.mobs.stream().filter(m->m instanceof HumanMob).map(m->(HumanMob)m)
            .mapToInt(m->m.getWorkInventory().stream().filter(i->i.item.getStringID().equals("wheat")).mapToInt(i->i.getAmount()).sum()).sum();
        total+=level.entityManager.pickups.stream().filter(p->!p.removed()&&p instanceof ItemPickupEntity).map(p->(ItemPickupEntity)p)
            .filter(p->p.item.item.getStringID().equals("wheat")).mapToInt(p->p.item.getAmount()).sum();
        for(var o:world.content.campObjects)if(o.role().endsWith("storage")&&level.entityManager.getObjectEntity(o.x(world),o.y(world)) instanceof InventoryObjectEntity chest)total+=amount(chest.getInventory());
        return total;
    }
    private static int amount(Inventory inv){int total=0;for(int i=0;i<inv.getSize();i++)if(inv.getItem(i)!=null&&inv.getItem(i).item.getStringID().equals("wheat"))total+=inv.getItem(i).getAmount();return total;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
