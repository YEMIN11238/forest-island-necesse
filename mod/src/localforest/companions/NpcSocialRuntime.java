package localforest.companions;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import localforest.ai.AiIntentService;
import localforest.ai.NpcMind;
import localforest.ai.NpcSocialPlanner;
import localforest.tasks.NativeWorkAdapter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.ai.behaviourTree.leaves.CommandMoveToAINode;
import static localforest.companions.CompanionSystem.*;

/** Server-thread adapter: AI Town social states drive native paths; model results cross a queue. */
final class NpcSocialRuntime {
    record Person(String id,String name,HumanMob mob,boolean recruited,boolean busy,boolean threatened,NpcMind mind,List<MemoryEvent> events) {}
    private record Move(String conversation,String person,HumanMob mob,Point target) {}
    private record Delivery(long generation,String level,NpcSocialPlanner.Action action,String text,long expires) {}
    private final String session;
    private final ConcurrentLinkedQueue<WorldSpeech> speech;
    private final NpcSocialPlanner planner=new NpcSocialPlanner();
    private final Map<String,Move> moves=new LinkedHashMap<>();
    private final ConcurrentLinkedQueue<Delivery> deliveries=new ConcurrentLinkedQueue<>();
    private final LinkedHashSet<String> reported=new LinkedHashSet<>();
    private final Map<String,Long> lastSocial=new LinkedHashMap<>();
    private final Map<String,Point> restoredMoves=new LinkedHashMap<>();
    private String level="";
    private long nextTick,nextModelAt,dayIndex;
    private volatile long generation;
    private volatile boolean modelPending;
    private String modelActor="";
    private long approaches,utterances,modelUtterances;

    NpcSocialRuntime(String session,ConcurrentLinkedQueue<WorldSpeech> speech){this.session=session;this.speech=speech;}
    void tick(ServerClient player,long now,boolean ready,String talkingTo,List<Person> people){
        if(now<nextTick)return;nextTick=now+500;
        for(Person p:people){Point target=restoredMoves.remove(p.id());
            if(target!=null&&!p.busy()&&target.equals(p.mob().commandGuardPoint))NativeWorkAdapter.clearCommands(p.mob(),null);
        }
        if(!ready||player==null||player.playerMob==null||player.playerMob.getLevel()==null){suspend();return;}
        String place=player.playerMob.getLevel().getIdentifier().toString();
        dayIndex=player.getLevel().getWorldEntity().getDay();
        if(!place.equals(level)){suspend();level=place;}
        boolean playerBusy=talkingTo!=null&&!talkingTo.isEmpty();
        Map<String,Person> nearby=new LinkedHashMap<>();
        for(Person p:people)if(!p.mob().removed()&&p.mob().isSamePlace(player.playerMob)&&p.mob().getDistance(player.playerMob)<448)nearby.put(p.id(),p);
        planner.prepareWorld(level,now);reported.forEach(planner::markReported);
        Delivery delivery;
        while((delivery=deliveries.poll())!=null){
            if(delivery.generation()!=generation||!level.equals(delivery.level())||now>delivery.expires())continue;
            speak(delivery.action(),delivery.text(),now,player,nearby,playerBusy);
        }
        List<NpcSocialPlanner.Npc> candidates=new ArrayList<>();
        List<NpcSocialPlanner.Event> events=new ArrayList<>();
        for(Person p:nearby.values()){
            HumanMob mob=p.mob();Move moving=moves.get(p.id());boolean ownMove=moving!=null&&moving.target().equals(mob.commandGuardPoint);
            boolean nativeBusy=mob.hasActiveJob()||mob.getCurrentMission()!=null||mob.commandFollowMob!=null||mob.commandAttackMob!=null||mob.commandGuardPoint!=null&&!ownMove;
            boolean cooling=now-lastSocial.getOrDefault(p.id(),Long.MIN_VALUE/2)<NpcSocialPlanner.PERSON_COOLDOWN&&!plannerOwns(p.id());
            boolean busy=!mob.isDowned()&&(p.busy()||nativeBusy||cooling);
            boolean canWalk=canSocialWalk(p,player)&&!busy&&!mob.isDowned();
            candidates.add(new NpcSocialPlanner.Npc(p.id(),mob.getUniqueID(),p.name(),mob.x/32.0,mob.y/32.0,true,busy,mob.isDowned(),p.threatened(),playerBusy,canWalk,CompanionSystem.behaviorTraits(p.mind().snapshot())));
            for(MemoryEvent event:p.events()){
                if(reported.contains(event.id()))continue;
                var kind=switch(event.type()){
                    case "downed"->mob.isDowned()?NpcSocialPlanner.EventKind.RESCUE_NEEDED:null;
                    case "rescued"->!mob.isDowned()?NpcSocialPlanner.EventKind.RESCUED:null;
                    case "task_completed"->NpcSocialPlanner.EventKind.TASK_COMPLETED;
                    default->null;
                };
                if(kind!=null)events.add(new NpcSocialPlanner.Event(event.id(),kind,p.id(),event.text(),event.worldTime()));
            }
        }
        var world=new NpcSocialPlanner.Snapshot(level,now,player.getLevel().getWorldEntity().getDay(),player.getLevel().getWorldEntity().isNight(),player.playerMob.x/32.0,player.playerMob.y/32.0,true,playerBusy,candidates,events);
        for(var action:planner.tick(world))execute(action,now,player,nearby,events);
    }
    private boolean plannerOwns(String id){return planner.isConversing(id);}
    private static boolean canSocialWalk(Person p,ServerClient player){
        if(p.recruited()&&p.mob().canBeCommanded(player,true))return true;
        if(p.mob() instanceof HumanShop shop){
            var d=NpcDefinition.find(NativeNpcIdentity.template(shop));
            return d!=null&&d.tier()==NpcDefinition.Tier.RESIDENT&&!shop.isVisitor();
        }
        return false;
    }
    private void execute(NpcSocialPlanner.Action action,long now,ServerClient player,Map<String,Person> people,List<NpcSocialPlanner.Event> events){
        switch(action.kind()){
            case INVITE,ACCEPT->{ }
            case APPROACH->{
                Person p=people.get(action.speakerId());
                if(p==null||p.busy()||!canSocialWalk(p,player)||p.mob().hasActiveJob()){
                    fail(action.conversationId(),now);return;
                }
                HumanMob mob=p.mob();
                Point tile=CommandMoveToAINode.findClosestMoveToTile(mob,(int)(action.targetX()*32),(int)(action.targetY()*32));
                if(tile==null||tile.distance(action.targetX(),action.targetY())>2||mob.getLevel().isLiquidTile(tile.x,tile.y)||mob.getLevel().isSolidTile(tile.x,tile.y)||!mob.estimateCanMoveTo(tile.x,tile.y,false)){
                    fail(action.conversationId(),now);return;
                }
                Point target=new Point(tile.x*32+16,tile.y*32+16);
                mob.commandGuard(null,target.x,target.y);moves.put(p.id(),new Move(action.conversationId(),p.id(),mob,target));approaches++;
            }
            case END->release(action.conversationId());
            case TALK_PAIR,TALK_PLAYER->{
                Person speaker=people.get(action.speakerId());if(speaker==null)return;
                // Hold the native guard reservation until END, keeping daily work from interrupting this short visit.
                if(!modelPending&&System.nanoTime()>=nextModelAt&&AiIntentService.isConfigured()){
                    modelPending=true;modelActor=speaker.id();long token=generation;String boundLevel=level;
                    // The shared scheduler enforces the aggregate allowance, regardless of population.
                    nextModelAt=System.nanoTime()+30_000_000_000L;
                    String context=speaker.mind().context(action.evidence(),now)+"\n这是自主交谈，不是玩家命令。当前可验证观察："+action.evidence()+"\n可表达的内容示例："+action.fallbackText();
                    if(speaker.mob() instanceof HumanShop shop){var definition=NpcDefinition.find(NativeNpcIdentity.template(shop));
                        if(definition!=null)context+="\n个人档案："+definition.occupation()+"；"+definition.introduction();}
                    Person peer=people.get(action.peerId());
                    String name=peer==null?"玩家":peer.name();
                    AiIntentService.ambient(session,speaker.id(),context,"请对"+name+"自然地说一句话，最多50字。依据真实观察和你的记忆表达，不新增事件，不宣称完成未完成任务，不调用任何行动工具。")
                        .whenComplete((text,error)->{if(token==generation){modelPending=false;if(deliveries.size()<16)deliveries.add(new Delivery(token,boundLevel,action,error==null?text:action.fallbackText(),now+12_000));}});
                }else speak(action,action.fallbackText(),now,player,people,false);
            }
        }
    }
    private void speak(NpcSocialPlanner.Action action,String text,long now,ServerClient player,Map<String,Person> people,boolean playerBusy){
        if(playerBusy||text==null||text.isBlank()||text.length()>200){retry(action,now,people);return;}
        Person a=people.get(action.speakerId()),b=people.get(action.peerId());
        boolean rescue=action.topicId().equals("rescue_needed");
        if(a==null||!rescue&&(a.busy()||a.threatened())||a.mob().removed()){retry(action,now,people);return;}
        double distance=action.peerId().equals(NpcSocialPlanner.PLAYER)?a.mob().getDistance(player.playerMob):b==null?Double.POSITIVE_INFINITY:a.mob().getDistance(b.mob());
        if(distance>112||b!=null&&(b.busy()||b.threatened())){retry(action,now,people);return;}
        if(speech.size()>=32)speech.poll();speech.add(new WorldSpeech(session,level,a.mob().getUniqueID(),text));utterances++;
        String peer=b==null?"player":b.id(),peerName=b==null?"玩家":b.name();
        a.mind().observeSocial(now,peer,peerName,a.name()+"对"+peerName+"说：“"+text+"”");
        lastSocial.put(a.id(),now);
        if(b!=null){b.mind().observeSocial(now,a.id(),a.name(),a.name()+"对"+b.name()+"说：“"+text+"”");lastSocial.put(b.id(),now);}
        while(lastSocial.size()>128)lastSocial.remove(lastSocial.keySet().iterator().next());
        if(action.kind()==NpcSocialPlanner.Kind.TALK_PLAYER)for(var e:a.events())if(e.text().equals(action.evidence()))report(e.id());
        if(action.topicId().equals("night_reminder"))report("night:"+player.getLevel().getWorldEntity().getDay());
    }
    private void retry(NpcSocialPlanner.Action action,long now,Map<String,Person> people){
        Person p=people.get(action.speakerId());
        if(p!=null)for(var e:p.events())if(e.text().equals(action.evidence()))planner.allowRetry(e.id());
        if(action.topicId().equals("night_reminder"))planner.allowRetry("night:"+dayIndex);
        fail(action.conversationId(),now);
    }
    private void report(String id){reported.add(id);while(reported.size()>128)reported.remove(reported.iterator().next());}
    private void fail(String id,long now){for(var end:planner.fail(id,now))release(end.conversationId());}
    void interrupt(String person,long now){
        for(var move:List.copyOf(moves.values()))if(planner.owns(move.conversation(),person))fail(move.conversation(),now);
        if(modelActor.equals(person)&&modelPending){generation++;AiIntentService.cancelRequest(session,person);modelPending=false;deliveries.clear();}
    }
    private void release(String conversation){
        var iterator=moves.entrySet().iterator();while(iterator.hasNext()){
            Move move=iterator.next().getValue();if(!move.conversation().equals(conversation))continue;
            if(!move.mob().removed()&&move.target().equals(move.mob().commandGuardPoint))NativeWorkAdapter.clearCommands(move.mob(),null);
            iterator.remove();
        }
    }
    private void suspend(){
        for(var move:List.copyOf(moves.values()))release(move.conversation());planner.clear();
        if(modelPending&&!modelActor.isEmpty())AiIntentService.cancelRequest(session,modelActor);
        modelPending=false;modelActor="";deliveries.clear();generation++;
    }
    void clear(){suspend();level="";}
    void save(SaveData parent){
        SaveData own=new SaveData("npcSocial");for(String id:reported){SaveData item=new SaveData("reported");item.addSafeString("id",id);own.addSaveData(item);}
        for(var e:lastSocial.entrySet()){SaveData item=new SaveData("last");item.addSafeString("id",e.getKey());item.addLong("time",e.getValue());own.addSaveData(item);}
        for(var move:moves.values()){SaveData item=new SaveData("pendingMove");item.addSafeString("id",move.person());item.addPoint("target",move.target());own.addSaveData(item);}
        for(var e:restoredMoves.entrySet()){SaveData item=new SaveData("pendingMove");item.addSafeString("id",e.getKey());item.addPoint("target",e.getValue());own.addSaveData(item);}parent.addSaveData(own);
    }
    void load(LoadData own){
        clear();reported.clear();lastSocial.clear();restoredMoves.clear();if(own==null)return;
        for(var item:own.getLoadDataByName("reported"))report(item.getSafeString("id",""));
        for(var item:own.getLoadDataByName("last")){if(lastSocial.size()>=128)break;lastSocial.put(item.getSafeString("id",""),item.getLong("time",0));}
        for(var item:own.getLoadDataByName("pendingMove")){if(restoredMoves.size()>=128)break;Point target=item.getPoint("target",null);if(target!=null)restoredMoves.put(item.getSafeString("id",""),target);}
    }
    Map<String,Object> counters(){return Map.of("nativeApproaches",approaches,"worldUtterances",utterances,"activeConversations",planner.activeConversations(),"reportedEvents",reported.size());}
}
