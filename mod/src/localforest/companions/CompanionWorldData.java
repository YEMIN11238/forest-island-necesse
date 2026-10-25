package localforest.companions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.awt.Point;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.world.worldData.WorldData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.ai.behaviourTree.leaves.CommandMoveToAINode;
import necesse.level.maps.Level;
import localforest.tasks.CompanionTask;
import localforest.tasks.NativeWorkAdapter;
import localforest.ai.NpcMind;
import localforest.ai.NpcBehaviorPolicy;
import static localforest.companions.CompanionSystem.*;

/** Versioned companion identity/task/event extension in the native world save. */
public final class CompanionWorldData extends WorldData {
    private static final int SCHEMA=1,MAX_MEMORIES=64,MAX_DIALOGUE_TURNS=8;
    private static final Set<String> ACTIONS=Set.of("FOLLOW","GUARD","HOME","WORK","HARVEST","FORESTRY","HAUL","CANCEL","PAUSE","RESUME","OPEN_RESCUE","OPEN_NATIVE");
    final String sessionId=UUID.randomUUID().toString();
    final ConcurrentLinkedQueue<Command> commands=new ConcurrentLinkedQueue<>();
    record CompletedDialogue(String session,String companion,int slot,String level,long ticket,int mobId,String playerText,String replyText) {}
    record PendingConversation(TextRequest request,long deadline,CompletedDialogue delivery) {}
    final ConcurrentHashMap<String,PendingConversation> pendingConversations=new ConcurrentHashMap<>();
    final ConcurrentLinkedQueue<CompletedDialogue> dialogues=new ConcurrentLinkedQueue<>();
    final RequestEpochs requests=new RequestEpochs();
    volatile View view=new View(sessionId,List.of());
    volatile long publishedWorldTime;
    volatile boolean sceneInteractive;
    volatile long scenePresenceAt;
    volatile String talkingTo="";
    // Package-only explicit disposable-server fixture input; never persisted or model-accessible.
    String nativeCheckTalkingTo="";
    long nativeCheckPresenceUntil;
    final ConcurrentLinkedQueue<WorldSpeech> speech=new ConcurrentLinkedQueue<>();
    private final NpcSocialRuntime social=new NpcSocialRuntime(sessionId,speech);
    private final Map<String,Companion> companions=new LinkedHashMap<>();
    private final Map<String,Companion> identityIndex=new java.util.HashMap<>();
    private final List<Companion> loadedCompanions=new ArrayList<>();
    private final Set<Companion> persistentViews=new java.util.LinkedHashSet<>();
    private final ConversationLease conversationLease=new ConversationLease();
    private final NpcActivitySchedule activities=new NpcActivitySchedule();
    private long nextScan,nextPublish,nextSocial;
    private long nextVisitors;
    private boolean attached;

    /** Immutable published identity lookup used by the native interaction bridge. */
    public boolean isRegisteredMob(int mobId,String level){
        return view.companions().stream().anyMatch(p->p.mobId()==mobId&&p.level().equals(level));
    }

    private static final class Companion {
        String id,name,level="",nativeUuid="",nativeType="",template="";Integer identitySeed;int mobId,x,y,homeX=-1,homeY=-1;long revision,nextObservation,lastSeen;
        boolean downed,restorePending,recruited=true;String role="营地伙伴";CompanionTask task;HumanMob mob;long nextWorkAttempt;
        final List<MemoryEvent> memories=new ArrayList<>();
        final List<DialogueTurn> recentConversation=new ArrayList<>();
        NpcMind mind;
        LoadData archivedMind;
        boolean threatened;
    }
    @Override public void init(){if(isServer()){CompanionSystem.register(getServer(),this);attached=true;}}
    @Override public void tick(){
        if(!isServer())return;if(!attached){CompanionSystem.register(getServer(),this);attached=true;}
        long now=worldEntity.getWorldTime();
        if(now>=nextScan){nextScan=now+500;discover(now);}
        if(now>=nextVisitors){nextVisitors=now+10_000;
            for(Level level:getServer().world.levelManager.getLoadedLevels())if(localforest.world.IslandWorld.isIsland(level))NpcPopulationService.tickVisitors(level);
        }
        tickConversationLease(now);
        long elapsed=System.nanoTime();
        pendingConversations.forEach((id,pending)->{
            if(elapsed>=pending.deadline()&&pendingConversations.remove(id,pending)){
                requests.invalidate(id);localforest.ai.AiIntentService.cancelRequest(sessionId,id);
            }
        });
        // Only a reply accepted by the scene UI may enter persistent dialogue memory.
        for(int i=0;i<64;i++){
            CompletedDialogue dialogue=dialogues.poll();if(dialogue==null)break;
            Companion p=companions.get(dialogue.companion());
            boolean accepted=requests.consumeDialogue(dialogue.ticket());
            ServerClient player=getServer().getClient(dialogue.slot());
            HumanMob mob=p==null?null:p.mob;
            boolean present=mob!=null&&!mob.removed()&&mob.getUniqueID()==dialogue.mobId()&&player!=null&&player.hasSpawned()&&player.playerMob!=null&&player.playerMob.getLevel()!=null&&mob.getLevel()!=null;
            String playerLevel=present?player.playerMob.getLevel().getIdentifier().toString():null;
            String npcLevel=present?mob.getLevel().getIdentifier().toString():null;
            if(accepted&&sessionId.equals(dialogue.session())&&validDialoguePlace(dialogue.level(),playerLevel,npcLevel,present)){
                p.recentConversation.add(new DialogueTurn(now,dialogue.playerText(),dialogue.replyText()));
                trimConversation(p.recentConversation);
                mind(p).observeDialogue(now,dialogue.playerText(),dialogue.replyText());
            }
        }
        for(int i=0;i<64;i++){
            Command c=commands.poll();if(c==null)break;
            try{apply(c,now);}catch(RuntimeException error){
                Companion p=companions.get(c.companion());
                if(p!=null&&p.task!=null&&!p.task.terminal()&&p.task.state!=CompanionTask.State.PAUSED)
                    p.task.transition(CompanionTask.State.BLOCKED,"任务暂时无法执行，请检查目标后继续",now);
                System.err.println("MOSSLUME_COMPANION_COMMAND_FAILED action="+c.action()+" error="+error.getClass().getSimpleName());
            }
        }
        ServerClient localPlayer=getServer().getLocalServerClient();
        for(Companion p:loadedCompanions){
            if(p.mob==null||p.mob.removed())continue;
            if(now<p.nextObservation)continue;
            boolean same=localPlayer!=null&&localPlayer.playerMob!=null&&p.mob.isSamePlace(localPlayer.playerMob);
            var tier=NpcSimulationTier.of(true,same,same?p.mob.getDistance(localPlayer.playerMob):Double.POSITIVE_INFINITY);
            p.nextObservation=now+tier.observationInterval;observe(p,now);
            if(tier==NpcSimulationTier.NEAR&&p.mob instanceof HumanShop shop){
                boolean busy=p.task!=null&&!p.task.terminal()&&!p.task.action.equals("WORK")&&p.task.state!=CompanionTask.State.PAUSED;
                activities.tick(p.id,shop,busy,p.threatened,talkingTo,now);
            }
        }
        tickSocial(now);
        if(now>=nextPublish){publish();nextPublish=now+200;}
    }
    private void discover(long now){
        List<Companion> previous=List.copyOf(loadedCompanions);
        for(Companion p:previous)p.mob=null;
        loadedCompanions.clear();
        for(Level level:getServer().world.levelManager.getLoadedLevels()){
            if(!localforest.world.IslandWorld.isIsland(level))continue;
            level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.isHostile&&!m.removed()).forEach(m->{
                HumanShop human=(HumanShop)m;
                String marker=NativeNpcIdentity.get(human);
                Companion p=marker.isEmpty()?null:identityIndex.get(marker);
                // Duplicated native NPC save data must not merge two live characters' memories.
                boolean duplicate=p!=null&&p.mob!=null&&p.mob!=human;
                if(duplicate)p=null;
                if(p==null&&!duplicate){
                    List<Companion> legacy=companions.values().stream().filter(c->c.mob==null&&NativeNpcIdentity.canMigrate(
                        new NativeNpcIdentity.Legacy(c.nativeUuid,c.mobId,c.level,c.name,c.nativeType,c.identitySeed),
                        human.getUniqueID(),level.getIdentifier().toString(),characterName(human),human.getStringID(),human.settlerSeed)).toList();
                    if(legacy.size()==1)p=legacy.get(0);
                }
                if(p==null){
                    p=new Companion();p.id=UUID.randomUUID().toString();p.mobId=human.getUniqueID();p.name=characterName(human);
                    p.recruited=isRecruited(human,false);
                    companions.put(p.id,p);remember(p,p.recruited?"joined":"met",p.recruited?p.name+"成为营地伙伴":"你遇见了"+p.name,"native.identity",now);
                }
                String uuid=duplicate||marker.isEmpty()?(p.nativeUuid.isEmpty()?UUID.randomUUID().toString():p.nativeUuid):marker;
                NativeNpcIdentity.bind(human,uuid);
                identityIndex.put(uuid,p);
                p.nativeUuid=uuid;p.nativeType=human.getStringID();p.identitySeed=human.settlerSeed;p.mobId=human.getUniqueID();
                boolean recruited=isRecruited(human,p.recruited);
                if(recruited!=p.recruited){
                    p.recruited=recruited;p.revision++;
                    if(recruited)remember(p,"joined",p.name+"成为营地伙伴","native.settler",now);
                    else if(p.task!=null&&!p.task.terminal())p.task.transition(CompanionTask.State.CANCELLED,"角色已不属于玩家营地",now);
                }
                p.template=NativeNpcIdentity.template(human);var definition=NpcDefinition.find(p.template);
                if(p.recruited||definition!=null&&definition.tier()==NpcDefinition.Tier.CORE||p.task!=null&&!p.task.terminal())persistentViews.add(p);
                else persistentViews.remove(p);
                p.role=definition==null?MobRegistry.getLocalization(human.getStringID()).translate():definition.occupation();
                p.mob=human;p.name=characterName(human);p.level=level.getIdentifier().toString();p.x=human.getTileX();p.y=human.getTileY();
                p.lastSeen=now;loadedCompanions.add(p);
                mind(p).updateIdentity(p.name,p.role);
                p.threatened=human.isAttacking||human.commandAttackMob!=null||level.entityManager.mobs.stream().anyMatch(other->other.isHostile&&!other.removed()&&other.getDistance(human)<224);
                p.homeX=human.home==null?-1:human.home.x;p.homeY=human.home==null?-1:human.home.y;
                if(p.restorePending){
                    p.restorePending=false;
                    if(p.recruited&&p.task!=null&&!p.task.terminal()){NativeWorkAdapter.clearCommands(human,null);NativeWorkAdapter.cancel(human);}
                }
            });
        }
        for(Companion p:previous)if(p.mob==null){
            activities.interrupt(p.id);observe(p,now);
            if(!persistentViews.contains(p)&&p.mind!=null){
                SaveData archive=new SaveData("archive");p.mind.save(archive);
                p.archivedMind=archive.toLoadData().getFirstLoadDataByName("npcMind");p.mind=null;
            }
        }
        ServerClient player=getServer().getLocalServerClient();
        if(player!=null&&player.playerMob!=null)loadedCompanions.sort(java.util.Comparator.comparingDouble(p->
            p.mob.isSamePlace(player.playerMob)?p.mob.getDistance(player.playerMob):Double.POSITIVE_INFINITY));
    }
    private void tickConversationLease(long now){
        ServerClient player=getServer().getLocalServerClient();
        boolean checking=System.nanoTime()<nativeCheckPresenceUntil&&!nativeCheckTalkingTo.isEmpty();
        if(checking)talkingTo=nativeCheckTalkingTo;
        Companion target=companions.get(talkingTo);
        boolean fresh=checking||System.nanoTime()-scenePresenceAt<3_000_000_000L;
        if(target==null||target.mob==null||!fresh){conversationLease.release();return;}
        boolean danger=target.threatened||target.mob.isAttacking||target.mob.commandAttackMob!=null;
        if(!target.id.equals(conversationLease.person())){social.interrupt(target.id,now);activities.interrupt(target.id);}
        if(!conversationLease.renew(target.id,target.mob,player,danger,System.nanoTime())){
            requests.invalidate(target.id);pendingConversations.remove(target.id);
            localforest.ai.AiIntentService.cancelRequest(sessionId,target.id);
        }
    }
    private boolean isRecruited(HumanMob human,boolean previous){
        ServerClient player=getServer().getLocalServerClient();
        // A temporarily unloaded settlement is not evidence that a saved companion left.
        if(player==null||(human.isSettler()&&human.getSettlerSettlementNetworkData()==null))return previous;
        if(human instanceof HumanShop shop){
            var definition=NpcDefinition.find(NativeNpcIdentity.template(shop));
            if(definition!=null&&definition.tier()!=NpcDefinition.Tier.CORE){
                // Nearby villagers may live within the same large native settlement boundary.
                // Only an actual native adventure-party invitation promotes them to AI task control.
                if(player.adventureParty.contains(human))NativeNpcIdentity.promote(shop);
                if(!NativeNpcIdentity.promoted(shop))return false;
            }
        }
        return human.canBeCommanded(player,true);
    }
    private static String characterName(HumanMob human){
        String name=human.getSettlerName();return name==null||name.isBlank()?human.getDisplayName():name;
    }
    private void observe(Companion p,long now){
        HumanMob mob=p.mob;
        if(mob==null){
            if(p.task!=null&&(p.task.state==CompanionTask.State.RUNNING||p.task.state==CompanionTask.State.QUEUED)){
                p.task.transition(CompanionTask.State.BLOCKED,"伙伴所在区域尚未载入；返回后可继续",now);
                remember(p,"task_suspended",p.name+"所在区域尚未载入，工作暂时中断",p.task.id,now);
            }
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
        if(!p.recruited)return;
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
            remember(p,"task_blocked",p.name+"尚未到达住所，返程暂时受阻",t.id,now);
        }
        if((t.action.equals("HARVEST")||t.action.equals("HAUL")||t.action.equals("FORESTRY"))&&now-t.changedAt>180000){
            t.transition(CompanionTask.State.BLOCKED,"工作耗时过长，已停止重试；请检查路径或存储条件",now);NativeWorkAdapter.cancel(mob);
            remember(p,"task_blocked",p.name+"的工作超时，已经停止重试",t.id,now);
        }
        if((t.action.equals("HARVEST")||t.action.equals("HAUL")||t.action.equals("FORESTRY"))&&t.state==CompanionTask.State.RUNNING
            &&now-t.changedAt>2000&&!NativeWorkAdapter.isTracking(mob)){
            t.transition(CompanionTask.State.BLOCKED,"原生工作已中断；请检查现场后继续",now);
            remember(p,"task_blocked",p.name+"的原生工作被中断",t.id,now);
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
        social.interrupt(p.id,now);
        activities.interrupt(p.id);
        if(!p.recruited&&!c.action().equals("OPEN_NATIVE")&&!c.action().equals("OPEN_RESCUE"))return;
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
        if(c.action().equals("OPEN_NATIVE")){
            if(!localforest.world.IslandWorld.enabled()||!getServer().isSingleplayer()
                ||getServer().getLocalServerClient()!=client)return;
            if(client.isDead()||!mob.inInteractRange(client.playerMob)||!mob.canInteract(client.playerMob)){
                blocked(p,c,"请先靠近伙伴，再打开原版事务",now);return;
            }
            // Opening a native form must not cancel work or require command rights:
            // the original shop/rescue interaction applies its own permissions.
            localforest.ui.CompanionInteraction.openNative(mob,client.playerMob);return;
        }
        if(c.action().equals("OPEN_RESCUE")){
            if(!mob.isDowned()){blocked(p,c,"伙伴目前不需要救治",now);return;}
            if(mob.getDistance(client.playerMob)>128){blocked(p,c,"请先靠近重伤伙伴，再打开救治对话",now);return;}
            if(!mob.canInteract(client.playerMob)){blocked(p,c,"目前无法打开救治对话",now);return;}
            localforest.ui.CompanionInteraction.openNative(mob,client.playerMob);return;
        }
        if(!mob.isDowned()&&!mob.canBeCommanded(client,false)){blocked(p,c,commandBlockReason(mob,false),now);return;}
        if(mob.isDowned()){blocked(p,c,"伙伴重伤，先进行救治",now);return;}
        String action=c.action();int x=c.x(),y=c.y();
        if(action.equals("RESUME")){
            if(p.task==null||p.task.terminal())return;
            action=p.task.action;x=p.task.tileX;y=p.task.tileY;
        }
        if(c.amount()!=1){blocked(p,c,"当前离线工作按一批执行，请使用数量 1",now);return;}
        var decision=NpcBehaviorPolicy.evaluate(action,CompanionSystem.behaviorTraits(mind(p).snapshot()),
            new NpcBehaviorPolicy.Situation(health(mob),p.downed,p.threatened,worldEntity.isNight(),p.threatened,false,p.recruited));
        if(!decision.allowed()){
            blocked(p,c,decision.fallbackText(),now);
            if(speech.size()<32)speech.add(new WorldSpeech(sessionId,p.level,p.mobId,decision.fallbackText()));
            return;
        }
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
            if(!mob.getLevel().isTileWithinBounds(x,y)||!mob.getLevel().regionManager.isTileLoaded(x,y)||mob.getLevel().isSolidTile(x,y)||mob.getLevel().isLiquidTile(x,y)||!mob.estimateCanMoveTo(x,y,false)){
                blocked(p,c,"守卫位置不可通行",now);return;
            }
        }
        p.revision++;
        if(p.task!=null&&!p.task.terminal())p.task.transition(CompanionTask.State.CANCELLED,"被新指令替代",now);
        NativeWorkAdapter.cancel(mob);NativeWorkAdapter.clearCommands(mob,client);
        CompanionTask task=new CompanionTask(action,x,y,1,now);p.task=task;
        persistentViews.add(p);
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
        MemoryEvent event=new MemoryEvent(UUID.randomUUID().toString(),now,type,text,source);p.memories.add(event);
        mind(p).observeEvent(event.id(),now,type,text,source);
        trimMemories(p.memories);
    }
    private static NpcMind mind(Companion p){
        if(p.mind==null){p.mind=NpcMind.load(p.archivedMind,p.id,p.name,p.role);p.archivedMind=null;}
        return p.mind;
    }
    private static double health(HumanMob mob){return mob==null?1:Math.max(0,Math.min(1,(double)mob.getHealth()/Math.max(1,mob.getMaxHealth())));}
    private void tickSocial(long now){
        if(now<nextSocial)return;nextSocial=now+500;
        ServerClient player=getServer().getLocalServerClient();
        boolean ready=sceneInteractive&&System.nanoTime()-scenePresenceAt<3_000_000_000L&&player!=null&&player.playerMob!=null&&!player.isDead();
        List<NpcSocialRuntime.Person> people=loadedCompanions.stream().filter(p->p.mob!=null&&!p.mob.removed()).map(p->{
            boolean busy=p.task!=null&&!p.task.terminal()&&!p.task.action.equals("WORK")&&p.task.state!=CompanionTask.State.PAUSED;
            return new NpcSocialRuntime.Person(p.id,p.name,p.mob,p.recruited,busy,p.threatened,mind(p),List.copyOf(p.memories));
        }).toList();
        social.tick(player,now,ready,talkingTo,people);
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
    static boolean validDialoguePlace(String bound,String playerLevel,String npcLevel,boolean present){
        return present&&bound!=null&&bound.equals(playerLevel)&&bound.equals(npcLevel);
    }
    void clearPendingConversations(){
        pendingConversations.clear();dialogues.clear();requests.invalidateAll();
        social.clear();activities.clear();conversationLease.release();speech.clear();sceneInteractive=false;talkingTo="";
        nativeCheckTalkingTo="";nativeCheckPresenceUntil=0;
        localforest.ai.AiIntentService.cancelSession(sessionId);
    }
    private static void trimConversation(List<DialogueTurn> turns){
        while(turns.size()>MAX_DIALOGUE_TURNS)turns.remove(0);
    }
    private void publish(){
        publishedWorldTime=worldEntity==null?0:worldEntity.getWorldTime();
        List<CompanionView> list=new ArrayList<>();
        java.util.LinkedHashSet<Companion> candidates=new java.util.LinkedHashSet<>();
        candidates.addAll(loadedCompanions);candidates.addAll(persistentViews);
        if(companions.size()<=64)candidates.addAll(companions.values());
        List<Companion> visible=candidates.stream().limit(64).toList();
        for(Companion p:visible){
            CompanionTask t=p.task;
            var definition=NpcDefinition.find(p.template);String profile=definition==null?"":definition.occupation()+"；"+definition.introduction();
            list.add(new CompanionView(p.id,p.name,p.mobId,p.level,p.x,p.y,p.downed,p.revision,t==null?"WORK":t.action,
                p.downed?"重伤待救":t==null?"日常活动":stateLabel(t.state),t==null?(p.recruited?"可分配离线任务":"可以交谈；交易、招募请打开原生事务"):t.message,t==null?0:t.completedSteps,List.copyOf(p.memories),
                p.recruited,p.role,List.copyOf(p.recentConversation),p.mob!=null&&!p.mob.removed(),mind(p).snapshot(),health(p.mob),p.threatened,profile));
        }
        view=new View(sessionId,List.copyOf(list));
    }
    public static String actionLabel(String a){return switch(a){case "FOLLOW"->"跟随";case "GUARD"->"守卫";case "HOME"->"回家";case "WORK"->"日常工作";case "HARVEST"->"采收一处";case "FORESTRY"->"林业工作";case "HAUL"->"搬运一批";default->a;};}
    private static String stateLabel(CompanionTask.State s){return switch(s){case QUEUED->"待执行";case RUNNING->"执行中";case BLOCKED->"遇到阻碍";case PAUSED->"已暂停";case SUCCEEDED->"已完成";case CANCELLED->"已取消";case FAILED->"未完成";};}
    @Override public void addSaveData(SaveData save){
        super.addSaveData(save);save.addInt("schema",SCHEMA);
        for(Companion p:companions.values()){
            SaveData s=new SaveData("companion");s.addSafeString("id",p.id);s.addSafeString("name",p.name);s.addInt("mobId",p.mobId);
            s.addSafeString("nativeUuid",p.nativeUuid);s.addSafeString("nativeType",p.nativeType);if(p.identitySeed!=null)s.addInt("identitySeed",p.identitySeed);
            s.addSafeString("template",p.template);s.addLong("lastSeen",p.lastSeen);
            s.addSafeString("level",p.level);s.addInt("x",p.x);s.addInt("y",p.y);s.addInt("homeX",p.homeX);s.addInt("homeY",p.homeY);
            s.addBoolean("downed",p.downed);s.addBoolean("recruited",p.recruited);s.addSafeString("role",p.role);s.addLong("revision",p.revision);if(p.task!=null)s.addSaveData(p.task.save());
            for(DialogueTurn turn:p.recentConversation){SaveData d=new SaveData("dialogue");d.addLong("time",turn.worldTime());d.addSafeString("player",turn.playerText());d.addSafeString("reply",turn.replyText());s.addSaveData(d);}
            for(MemoryEvent e:p.memories){SaveData m=new SaveData("memory");m.addSafeString("id",e.id());m.addLong("time",e.worldTime());m.addSafeString("type",e.type());m.addSafeString("text",e.text());m.addSafeString("source",e.source());s.addSaveData(m);}
            if(p.mind==null&&p.archivedMind!=null)s.addSaveData(p.archivedMind.toSaveData());else mind(p).save(s);
            save.addSaveData(s);
        }
        social.save(save);
    }
    @Override public void applyLoadData(LoadData save){
        int schema=save.getInt("schema",1);if(schema!=SCHEMA)throw new IllegalStateException("Unsupported companion save schema "+schema);
        companions.clear();identityIndex.clear();loadedCompanions.clear();persistentViews.clear();commands.clear();clearPendingConversations();
        for(LoadData s:save.getLoadDataByName("companion")){
            Companion p=new Companion();p.id=s.getSafeString("id");p.name=s.getSafeString("name");p.mobId=s.getInt("mobId");p.level=s.getSafeString("level","");
            p.nativeUuid=NativeNpcIdentity.validUuid(s.getSafeString("nativeUuid","",false));p.nativeType=s.getSafeString("nativeType","",false);
            p.template=s.getSafeString("template","",false);p.lastSeen=s.getLong("lastSeen",0,false);
            p.identitySeed=s.hasLoadDataByName("identitySeed")?s.getInt("identitySeed"):null;
            p.x=s.getInt("x",0);p.y=s.getInt("y",0);p.homeX=s.getInt("homeX",-1);p.homeY=s.getInt("homeY",-1);p.downed=s.getBoolean("downed",false);p.revision=s.getLong("revision",0)+1;p.restorePending=true;
            p.recruited=s.getBoolean("recruited",true,false);p.role=s.getSafeString("role","营地伙伴",false);
            for(LoadData d:s.getLoadDataByName("dialogue")){
                String player=d.getSafeString("player",""),reply=d.getSafeString("reply","");
                if(!player.isBlank()&&!reply.isBlank()&&player.length()<=2000&&reply.length()<=500)p.recentConversation.add(new DialogueTurn(d.getLong("time",0),player,reply));
            }
            trimConversation(p.recentConversation);
            LoadData task=s.getFirstLoadDataByName("task");if(task!=null)p.task=CompanionTask.load(task);
            for(LoadData m:s.getLoadDataByName("memory"))p.memories.add(new MemoryEvent(m.getSafeString("id"),m.getLong("time",0),m.getSafeString("type"),m.getSafeString("text"),m.getSafeString("source","native")));
            trimMemories(p.memories);companions.put(p.id,p);
            var definition=NpcDefinition.find(p.template);
            if(p.recruited||definition!=null&&definition.tier()==NpcDefinition.Tier.CORE||p.task!=null&&!p.task.terminal())persistentViews.add(p);
            if(!p.nativeUuid.isEmpty())identityIndex.putIfAbsent(p.nativeUuid,p);
            LoadData savedMind=s.getFirstLoadDataByName("npcMind");
            if(savedMind!=null&&!persistentViews.contains(p)){p.archivedMind=savedMind;}
            else p.mind=NpcMind.load(savedMind,p.id,p.name,p.role);
            if(savedMind==null){
                p.memories.stream().sorted(java.util.Comparator.comparingLong(MemoryEvent::worldTime)).forEach(e->p.mind.observeEvent(e.id(),e.worldTime(),e.type(),e.text(),e.source()));
                p.recentConversation.forEach(t->p.mind.observeDialogue(t.worldTime(),t.playerText(),t.replyText()));
            }
        }
        social.load(save.getFirstLoadDataByName("npcSocial"));
        publish();
    }
    /** Server diagnostics contain counts only, never player dialogue or model credentials. */
    public Map<String,Object> diagnostics(){
        return Map.of("known",companions.size(),"loaded",loadedCompanions.size(),"visible",view.companions().size(),
            "activeMinds",companions.values().stream().filter(p->p.mind!=null).count(),
            "conversationHeld",!conversationLease.person().isEmpty());
    }
    public Map<String,String> permanentPopulationIdentities(){
        Map<String,String> result=new LinkedHashMap<>();
        for(var p:companions.values()){
            var d=NpcDefinition.find(p.template);
            if(d!=null&&d.tier()!=NpcDefinition.Tier.VISITOR&&!p.nativeUuid.isEmpty())result.put(p.template,p.nativeUuid);
        }
        return Map.copyOf(result);
    }
}
