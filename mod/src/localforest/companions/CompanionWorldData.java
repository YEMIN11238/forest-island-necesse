package localforest.companions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.awt.Point;
import java.util.concurrent.ConcurrentLinkedQueue;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.worldData.WorldData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.ai.behaviourTree.leaves.CommandMoveToAINode;
import necesse.level.maps.Level;
import localforest.tasks.CompanionTask;
import localforest.tasks.NativeWorkAdapter;
import static localforest.companions.CompanionSystem.*;

/** Versioned companion identity/task/event extension in the native world save. */
public final class CompanionWorldData extends WorldData {
    private static final int SCHEMA=1,MAX_MEMORIES=64;
    private static final Set<String> ACTIONS=Set.of("FOLLOW","GUARD","HOME","WORK","HARVEST","FORESTRY","HAUL","CANCEL","PAUSE","RESUME","OPEN_RESCUE");
    final String sessionId=UUID.randomUUID().toString();
    final ConcurrentLinkedQueue<Command> commands=new ConcurrentLinkedQueue<>();
    final RequestEpochs requests=new RequestEpochs();
    volatile View view=new View(sessionId,List.of());
    volatile long publishedWorldTime;
    private final Map<String,Companion> companions=new LinkedHashMap<>();
    private long nextScan;
    private boolean attached;

    private static final class Companion {
        String id,name,level="";int mobId,x,y,homeX=-1,homeY=-1;long revision;
        boolean downed,restorePending;CompanionTask task;HumanMob mob;long nextWorkAttempt;
        final List<MemoryEvent> memories=new ArrayList<>();
    }
    @Override public void init(){if(isServer()){CompanionSystem.register(getServer(),this);attached=true;}}
    @Override public void tick(){
        if(!isServer())return;if(!attached){CompanionSystem.register(getServer(),this);attached=true;}
        long now=worldEntity.getWorldTime();
        if(now>=nextScan){nextScan=now+500;discover(now);}
        for(int i=0;i<64;i++){
            Command c=commands.poll();if(c==null)break;
            try{apply(c,now);}catch(RuntimeException error){
                Companion p=companions.get(c.companion());
                if(p!=null&&p.task!=null&&!p.task.terminal()&&p.task.state!=CompanionTask.State.PAUSED)
                    p.task.transition(CompanionTask.State.BLOCKED,"任务暂时无法执行，请检查目标后继续",now);
                System.err.println("MOSSLUME_COMPANION_COMMAND_FAILED action="+c.action()+" error="+error.getClass().getSimpleName());
            }
        }
        for(Companion p:companions.values())observe(p,now);
        publish();
    }
    private void discover(long now){
        for(Companion p:companions.values())p.mob=null;
        for(Level level:getServer().world.levelManager.getLoadedLevels()){
            if(!localforest.world.IslandWorld.isIsland(level))continue;
            level.entityManager.mobs.stream().filter(m->m instanceof HumanMob).forEach(m->{
                HumanMob human=(HumanMob)m;
                Companion p=companions.values().stream().filter(c->c.mobId==human.getUniqueID()).findFirst().orElse(null);
                if(p==null){
                    if(!human.isSettler())return;
                    p=new Companion();p.id=UUID.randomUUID().toString();p.mobId=human.getUniqueID();p.name=human.getSettlerName();
                    companions.put(p.id,p);remember(p,"joined",p.name+"成为营地伙伴","native.settler",now);
                }
                p.mob=human;p.name=human.getSettlerName();p.level=level.getIdentifier().toString();p.x=human.getTileX();p.y=human.getTileY();
                p.homeX=human.home==null?-1:human.home.x;p.homeY=human.home==null?-1:human.home.y;
                if(p.restorePending){
                    p.restorePending=false;
                    if(p.task!=null&&!p.task.terminal()){NativeWorkAdapter.clearCommands(human,null);NativeWorkAdapter.cancel(human);}
                }
            });
        }
    }
    private void observe(Companion p,long now){
        HumanMob mob=p.mob;
        if(mob==null){
            if(p.task!=null&&(p.task.state==CompanionTask.State.RUNNING||p.task.state==CompanionTask.State.QUEUED))p.task.transition(CompanionTask.State.BLOCKED,"伙伴所在区域尚未载入；返回后可继续",now);
            return;
        }
        p.x=mob.getTileX();p.y=mob.getTileY();
        if(mob.isDowned()!=p.downed){
            p.downed=mob.isDowned();p.revision++;
            if(p.downed){
                if(p.task!=null&&!p.task.terminal())p.task.transition(CompanionTask.State.BLOCKED,"伙伴重伤，需要靠近后进行救治",now);
                remember(p,"downed",p.name+"在 ("+p.x+", "+p.y+") 重伤，原生工作已中断","native.health",now);
            }else remember(p,"rescued",p.name+"已恢复活动；物品和住所按原生救援结果恢复","native.health",now);
        }
        CompanionTask t=p.task;
        if(t!=null&&t.state==CompanionTask.State.QUEUED&&!p.downed&&now>=p.nextWorkAttempt){
            try{startWork(p,t,mob,now);}
            catch(RuntimeException error){
                t.transition(CompanionTask.State.BLOCKED,"原生工作暂时无法启动，请检查现场后继续",now);
                NativeWorkAdapter.clearCommands(mob,null);NativeWorkAdapter.cancel(mob);
                System.err.println("MOSSLUME_COMPANION_RETRY_FAILED action="+t.action+" error="+error.getClass().getSimpleName());
            }
        }
        if(t==null||t.state!=CompanionTask.State.RUNNING||p.downed)return;
        if(t.action.equals("HOME")&&!mob.commandMoveToGuardPoint&&new Point(t.tileX*32+16,t.tileY*32+16).equals(mob.commandGuardPoint)
            &&Math.hypot(mob.x-(t.tileX*32+16),mob.y-(t.tileY*32+16))<64){
            t.transition(CompanionTask.State.SUCCEEDED,"已返回住所附近",now);NativeWorkAdapter.clearCommands(mob,null);
            remember(p,"task_completed",p.name+"已返回住所",t.id,now);
        }
        if(t.action.equals("HOME")&&t.state==CompanionTask.State.RUNNING&&now-t.changedAt>60000){
            t.transition(CompanionTask.State.BLOCKED,"尚未到达住所，请检查入口和路径后继续",now);NativeWorkAdapter.clearCommands(mob,null);
        }
        if((t.action.equals("HARVEST")||t.action.equals("HAUL")||t.action.equals("FORESTRY"))&&now-t.changedAt>180000){
            t.transition(CompanionTask.State.BLOCKED,"工作耗时过长，已停止重试；请检查路径或存储条件",now);NativeWorkAdapter.cancel(mob);
        }
        if((t.action.equals("HARVEST")||t.action.equals("HAUL")||t.action.equals("FORESTRY"))&&t.state==CompanionTask.State.RUNNING
            &&now-t.changedAt>2000&&!NativeWorkAdapter.isTracking(mob)){
            t.transition(CompanionTask.State.BLOCKED,"原生工作已中断；请检查现场后继续",now);
        }
    }
    private void apply(Command c,long now){
        if(!sessionId.equals(c.session()))return;
        if(c.action().equals("INVALIDATE")){companions.values().forEach(p->p.revision++);return;}
        Companion p=companions.get(c.companion());if(p==null)return;
        if(c.requestEpoch()>=0&&!requests.isCurrent(c.companion(),c.requestEpoch()))return;
        if(c.revision()>=0&&c.revision()!=p.revision)return;
        if(!ACTIONS.contains(c.action()))return;
        ServerClient client=getServer().getClient(c.slot());if(client==null||!client.hasSpawned())return;
        HumanMob mob=p.mob;
        if(c.action().equals("PAUSE")||c.action().equals("CANCEL")){
            // Cancellation must still work while a resident is unloaded or on strike.
            if(mob!=null&&!mob.isDowned()&&!mob.canBeCommanded(client,true)){blocked(p,c,commandBlockReason(mob,true),now);return;}
            p.revision++;
            if(p.task!=null&&!p.task.terminal())p.task.transition(c.action().equals("PAUSE")?CompanionTask.State.PAUSED:CompanionTask.State.CANCELLED,
                c.action().equals("PAUSE")?"任务已暂停；伙伴恢复日常活动":"指令已取消，恢复日常活动",now);
            if(mob!=null){NativeWorkAdapter.clearCommands(mob,client);NativeWorkAdapter.cancel(mob);}
            return;
        }
        if(mob==null){blocked(p,c,"伙伴不在已载入区域",now);return;}
        if(!mob.isSamePlace(client.playerMob)){blocked(p,c,"伙伴与玩家不在同一区域",now);return;}
        if(!mob.isDowned()&&!mob.canBeCommanded(client,false)){blocked(p,c,commandBlockReason(mob,false),now);return;}
        if(c.action().equals("OPEN_RESCUE")){
            if(!mob.isDowned()){blocked(p,c,"伙伴目前不需要救治",now);return;}
            if(mob.getDistance(client.playerMob)>128){blocked(p,c,"请先靠近重伤伙伴，再打开救治对话",now);return;}
            mob.interact(client.playerMob);return;
        }
        if(mob.isDowned()){blocked(p,c,"伙伴重伤，先进行救治",now);return;}
        String action=c.action();int x=c.x(),y=c.y();
        if(action.equals("RESUME")){
            if(p.task==null||p.task.terminal())return;
            action=p.task.action;x=p.task.tileX;y=p.task.tileY;
        }
        if(c.amount()!=1){blocked(p,c,"当前离线工作按一批执行，请使用数量 1",now);return;}
        if(action.equals("HOME")){
            if(p.homeX<0||!mob.isSettlerOnCurrentLevel()){blocked(p,c,"当前区域没有可返回的伙伴住所",now);return;}
            // Beds themselves can be solid. Use the engine's own reachable-tile fallback,
            // then issue a local guard command; never use cross-level teleport helpers.
            Point home=CommandMoveToAINode.findClosestMoveToTile(mob,p.homeX*32+16,p.homeY*32+16);
            if(home==null||home.distance(p.homeX,p.homeY)>4||mob.getLevel().isLiquidTile(home.x,home.y)){
                blocked(p,c,"住所附近没有可到达的位置，请检查入口",now);return;
            }
            x=home.x;y=home.y;
        }
        if(action.equals("GUARD")){
            if(x<0||y<0||x>=mob.getLevel().tileWidth||y>=mob.getLevel().tileHeight||mob.getLevel().isSolidTile(x,y)||mob.getLevel().isLiquidTile(x,y)||!mob.estimateCanMoveTo(x,y,false)){
                blocked(p,c,"守卫位置不可通行",now);return;
            }
        }
        p.revision++;
        if(p.task!=null&&!p.task.terminal())p.task.transition(CompanionTask.State.CANCELLED,"被新指令替代",now);
        NativeWorkAdapter.cancel(mob);NativeWorkAdapter.clearCommands(mob,client);
        CompanionTask task=new CompanionTask(action,x,y,1,now);p.task=task;
        switch(action){
            case "FOLLOW" -> {mob.commandFollow(client,client.playerMob);task.transition(CompanionTask.State.RUNNING,"正在跟随你；可随时取消",now);}
            case "GUARD" -> {mob.commandGuard(client,x*32+16,y*32+16);task.transition(CompanionTask.State.RUNNING,"前往并守卫指定位置",now);}
            case "HOME" -> {mob.commandGuard(null,x*32+16,y*32+16);task.transition(CompanionTask.State.RUNNING,"正在返回住所",now);}
            case "WORK" -> {task.transition(CompanionTask.State.RUNNING,"按原生日程工作、进食和休息",now);}
            default -> startWork(p,task,mob,now);
        }
        remember(p,"task_accepted",p.name+"收到指令："+actionLabel(action),task.id,now);
        System.out.println("MOSSLUME_COMPANION_TASK action="+action+" state="+task.state+" mob="+p.mobId);
    }
    private void startWork(Companion p,CompanionTask task,HumanMob mob,long now){
        NativeWorkAdapter.BeginResult attempt=NativeWorkAdapter.begin(mob,task.action,(success,message)->{
            if(p.task!=task||task.state!=CompanionTask.State.RUNNING)return;
            task.completedSteps=success?1:0;
            task.transition(success?CompanionTask.State.SUCCEEDED:CompanionTask.State.BLOCKED,message,worldEntity.getWorldTime());
            remember(p,success?"task_completed":"task_blocked",p.name+"："+message,task.id,worldEntity.getWorldTime());
        });
        if(attempt.started()){
            task.transition(CompanionTask.State.RUNNING,attempt.message(),now);
        }else if(attempt.retryable()&&now-task.createdAt<20000){
            task.transition(CompanionTask.State.QUEUED,attempt.message(),now);p.nextWorkAttempt=now+1000;
            // Hold at the current location using a native local order. Otherwise the
            // autonomous daily AI can take the crop before the queued intent retries.
            if(mob.commandGuardPoint==null)mob.commandGuard(null,mob.getX(),mob.getY());
        }else{
            String reason=attempt.retryable()?(task.action.equals("FORESTRY")?
                "没有可执行林业任务：请设置原生林业区并检查可采树木":
                "仍没有可执行工作：检查成熟作物、工作权限、可达路径与聚落仓库"):attempt.message();
            task.transition(CompanionTask.State.BLOCKED,reason,now);
            NativeWorkAdapter.clearCommands(mob,null);NativeWorkAdapter.cancel(mob);
            remember(p,"task_blocked",p.name+"："+reason,task.id,now);
        }
    }
    private void blocked(Companion p,Command c,String why,long now){
        // A rejected request is still a processed request, and supersedes older model work.
        p.revision++;
        if(p.task==null||p.task.terminal())p.task=new CompanionTask(c.action(),c.x(),c.y(),c.amount(),now);
        if(p.task.state!=CompanionTask.State.RUNNING)p.task.transition(CompanionTask.State.BLOCKED,why,now);
        remember(p,"request_blocked",why,"validation",now);
    }
    private static String commandBlockReason(HumanMob mob,boolean settingsOnly){
        if(!settingsOnly&&mob.getCurrentMission()!=null)return "伙伴正在执行原生远征任务，结束后才能接受指令";
        var settlement=mob.getSettlerSettlementNetworkData();
        if(settlement==null)return "伙伴的营地信息尚未载入，请稍后重试";
        if(!settingsOnly&&mob.isOnStrike()&&!settlement.isRaidActiveOrApproaching()&&settlement.isTileWithinBounds(mob.getTileX(),mob.getTileY()))
            return "伙伴正在罢工，请先改善饮食、住所与生活条件";
        return "你目前没有指挥这个伙伴的权限";
    }
    private void remember(Companion p,String type,String text,String source,long now){
        p.memories.add(new MemoryEvent(UUID.randomUUID().toString(),now,type,text,source));
        trimMemories(p.memories);
    }
    private static void trimMemories(List<MemoryEvent> events){
        while(events.size()>MAX_MEMORIES){
            int remove=0;int lowest=Integer.MAX_VALUE;
            for(int i=0;i<events.size();i++){
                int importance=switch(events.get(i).type()){case "joined","rescued","downed"->3;case "task_completed"->2;default->1;};
                if(importance<lowest){lowest=importance;remove=i;}
            }
            events.remove(remove);
        }
    }
    private void publish(){
        publishedWorldTime=worldEntity==null?0:worldEntity.getWorldTime();
        List<CompanionView> list=new ArrayList<>();
        for(Companion p:companions.values()){
            CompanionTask t=p.task;
            list.add(new CompanionView(p.id,p.name,p.mobId,p.level,p.x,p.y,p.downed,p.revision,t==null?"WORK":t.action,
                p.downed?"重伤待救":t==null?"日常活动":stateLabel(t.state),t==null?"可分配离线任务":t.message,t==null?0:t.completedSteps,List.copyOf(p.memories)));
        }
        view=new View(sessionId,List.copyOf(list));
    }
    public static String actionLabel(String a){return switch(a){case "FOLLOW"->"跟随";case "GUARD"->"守卫";case "HOME"->"回家";case "WORK"->"日常工作";case "HARVEST"->"采收一处";case "FORESTRY"->"林业工作";case "HAUL"->"搬运一批";default->a;};}
    private static String stateLabel(CompanionTask.State s){return switch(s){case QUEUED->"待执行";case RUNNING->"执行中";case BLOCKED->"遇到阻碍";case PAUSED->"已暂停";case SUCCEEDED->"已完成";case CANCELLED->"已取消";case FAILED->"未完成";};}
    @Override public void addSaveData(SaveData save){
        super.addSaveData(save);save.addInt("schema",SCHEMA);
        for(Companion p:companions.values()){
            SaveData s=new SaveData("companion");s.addSafeString("id",p.id);s.addSafeString("name",p.name);s.addInt("mobId",p.mobId);
            s.addSafeString("level",p.level);s.addInt("x",p.x);s.addInt("y",p.y);s.addInt("homeX",p.homeX);s.addInt("homeY",p.homeY);
            s.addBoolean("downed",p.downed);s.addLong("revision",p.revision);if(p.task!=null)s.addSaveData(p.task.save());
            for(MemoryEvent e:p.memories){SaveData m=new SaveData("memory");m.addSafeString("id",e.id());m.addLong("time",e.worldTime());m.addSafeString("type",e.type());m.addSafeString("text",e.text());m.addSafeString("source",e.source());s.addSaveData(m);}
            save.addSaveData(s);
        }
    }
    @Override public void applyLoadData(LoadData save){
        int schema=save.getInt("schema",1);if(schema!=SCHEMA)throw new IllegalStateException("Unsupported companion save schema "+schema);
        companions.clear();commands.clear();requests.invalidateAll();
        for(LoadData s:save.getLoadDataByName("companion")){
            Companion p=new Companion();p.id=s.getSafeString("id");p.name=s.getSafeString("name");p.mobId=s.getInt("mobId");p.level=s.getSafeString("level","");
            p.x=s.getInt("x",0);p.y=s.getInt("y",0);p.homeX=s.getInt("homeX",-1);p.homeY=s.getInt("homeY",-1);p.downed=s.getBoolean("downed",false);p.revision=s.getLong("revision",0)+1;p.restorePending=true;
            LoadData task=s.getFirstLoadDataByName("task");if(task!=null)p.task=CompanionTask.load(task);
            for(LoadData m:s.getLoadDataByName("memory"))p.memories.add(new MemoryEvent(m.getSafeString("id"),m.getLong("time",0),m.getSafeString("type"),m.getSafeString("text"),m.getSafeString("source","native")));
            trimMemories(p.memories);companions.put(p.id,p);
        }
        publish();
    }
}
