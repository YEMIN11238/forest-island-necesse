package localforest.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-thread, low-frequency social state machine. No network, model, game objects or render work.
 * Adapted from AI Town's invite -> walkingOver -> participating -> leave lifecycle, operation lock,
 * invite/action/message timeouts and per-agent/per-pair cooldowns. MIT, copyright (c) 2023 a16z-infra.
 * Source pin: 8e05997f2409275669c8344b84a51692e83f3f33; see third-party/ai-town/SOCIAL-PORT.md.
 * Positions are TILE coordinates. busy excludes this planner's own temporary approach movement.
 */
public final class NpcSocialPlanner {
    public static final String PLAYER="@player";
    public static final long GLOBAL_START_COOLDOWN=8_000,PERSON_COOLDOWN=75_000,PAIR_COOLDOWN=180_000;
    public static final long INVITE_TIMEOUT=8_000,APPROACH_TIMEOUT=18_000,MAX_CONVERSATION_DURATION=36_000;
    public static final long MESSAGE_COOLDOWN=4_500,GLOBAL_SPEECH_COOLDOWN=2_500;
    public static final int MAX_CONCURRENT=2,MAX_MESSAGES=3,MAX_SPEECH_PER_MINUTE=8;
    private static final double TALK_DISTANCE=2.5,MAX_VISIT_DISTANCE=12;

    public enum Kind { INVITE, ACCEPT, APPROACH, TALK_PAIR, TALK_PLAYER, END }
    public enum EventKind { RESCUE_NEEDED, RESCUED, TASK_COMPLETED }
    private enum Phase { INVITED, WALKING, TALKING }
    public record Npc(String id,int mobId,String name,double x,double y,boolean friendly,boolean busy,
        boolean injured,boolean inCombat,boolean playerConversing,boolean canApproach,NpcBehaviorPolicy.Traits traits) {
        public Npc { id=required(id);name=name==null||name.isBlank()?id:name;traits=traits==null?NpcBehaviorPolicy.Traits.neutral():traits;
            if(!Double.isFinite(x)||!Double.isFinite(y))throw new IllegalArgumentException("Invalid NPC position"); }
    }
    /** Evidence must describe an observed event, never an unverified model claim. IDs must be stable across ticks. */
    public record Event(String id,EventKind kind,String npcId,String evidence,long occurredAt) {
        public Event { id=required(id);Objects.requireNonNull(kind);npcId=required(npcId);evidence=required(evidence); }
    }
    public record Snapshot(String worldId,long nowMillis,long dayIndex,boolean night,double playerX,double playerY,
        boolean playerPresent,boolean playerBusy,List<Npc> npcs,List<Event> events) {
        public Snapshot { worldId=required(worldId);npcs=List.copyOf(npcs);events=List.copyOf(events);
            if(nowMillis<0||!Double.isFinite(playerX)||!Double.isFinite(playerY))throw new IllegalArgumentException("Invalid snapshot"); }
    }
    /**
     * APPROACH asks the adapter to navigate speakerId toward a safe walkable tile near targetX/Y.
     * END releases only this conversation's movement reservation; never cancel a newer player task.
     * TALK_* is one bounded utterance; fallbackText is playable without a model. Evidence is prompt data.
     */
    public record Action(Kind kind,String conversationId,String speakerId,String peerId,double targetX,double targetY,
        String topicId,String evidence,String fallbackText) {}
    private static final class Session {
        final String id,a,b,topic,evidence,eventId;final long created;final boolean urgent;
        Phase phase=Phase.INVITED;long phaseAt,lastMessage=Long.MIN_VALUE;int messages;
        Session(String id,String a,String b,String topic,String evidence,String eventId,long now,boolean urgent) {
            this.id=id;this.a=a;this.b=b;this.topic=topic;this.evidence=evidence;this.eventId=eventId;
            this.created=now;this.phaseAt=now;this.urgent=urgent;
        }
    }
    private final Map<String,Session> active=new LinkedHashMap<>();
    private final Map<String,Long> personLast=new HashMap<>(),pairLast=new HashMap<>();
    private final Set<String> consumedEvents=new HashSet<>();
    private final ArrayDeque<String> eventOrder=new ArrayDeque<>();
    private final ArrayDeque<Long> speechTimes=new ArrayDeque<>();
    private String worldId="";private long lastTick=-1,lastStart=Long.MIN_VALUE,lastSpeech=Long.MIN_VALUE,sequence;

    public List<Action> tick(Snapshot snapshot) {
        Objects.requireNonNull(snapshot);long now=snapshot.nowMillis();
        prepareWorld(snapshot.worldId(),now);
        lastTick=now;
        while(!speechTimes.isEmpty()&&now-speechTimes.peekFirst()>=60_000)speechTimes.removeFirst();
        Map<String,Npc> npcs=new LinkedHashMap<>();snapshot.npcs().stream().sorted(Comparator.comparing(Npc::id)).forEach(n->npcs.put(n.id(),n));
        List<Action> result=new ArrayList<>();
        for(Session s:new ArrayList<>(active.values()))advance(s,snapshot,npcs,result);
        if(active.size()>=MAX_CONCURRENT||!elapsed(now,lastStart,GLOBAL_START_COOLDOWN))return List.copyOf(result);
        Candidate next=eventCandidate(snapshot,npcs);
        if(next==null)next=nightCandidate(snapshot,npcs);
        if(next==null)next=pairCandidate(snapshot,npcs);
        if(next!=null) {
            Session s=new Session(worldId+":social:"+(++sequence),next.a.id(),next.b,next.topic,next.evidence,next.eventId,now,next.urgent);
            active.put(s.id,s);lastStart=now;
            result.add(action(Kind.INVITE,s,s.a,s.b,next.a.x(),next.a.y(),""));
        }
        // Keep old loaded-character cooldown entries bounded in long sessions.
        personLast.entrySet().removeIf(e->now-e.getValue()>PAIR_COOLDOWN*2);
        pairLast.entrySet().removeIf(e->now-e.getValue()>PAIR_COOLDOWN*2);
        return List.copyOf(result);
    }

    private void advance(Session s,Snapshot world,Map<String,Npc> npcs,List<Action> out) {
        long now=world.nowMillis();Npc a=npcs.get(s.a),b=npcs.get(s.b);boolean player=s.b.equals(PLAYER);
        if(!available(a,s.urgent)||(!player&&!available(b,false))||(player&&(!world.playerPresent()||world.playerBusy()))) {
            end(s,now,"interrupted",out);return;
        }
        double bx=player?world.playerX():b.x(),by=player?world.playerY():b.y();
        double distance=distance(a.x(),a.y(),bx,by);
        if(now-s.created>MAX_CONVERSATION_DURATION||distance>MAX_VISIT_DISTANCE*1.5) {end(s,now,"expired",out);return;}
        if(s.phase==Phase.INVITED) {
            if(now-s.phaseAt>INVITE_TIMEOUT){end(s,now,"invite_timeout",out);return;}
            // AI Town accepts human-initiated invites unconditionally; automatic NPC invitations use
            // deterministic personality preference here, so saves/tests do not depend on Math.random.
            if(!player&&!accepts(s,b,world.night())){end(s,now,"declined",out);return;}
            s.phase=Phase.WALKING;s.phaseAt=now;
            if(!player)out.add(action(Kind.ACCEPT,s,s.b,s.a,b.x(),b.y(),""));
            if(distance>TALK_DISTANCE)approach(s,a,b,bx,by,out);
        }
        if(s.phase==Phase.WALKING) {
            if(distance>TALK_DISTANCE) {
                if(now-s.phaseAt>=APPROACH_TIMEOUT||a.injured()){end(s,now,"approach_timeout",out);return;}
                return;
            }
            s.phase=Phase.TALKING;s.phaseAt=now;
        }
        if(distance>TALK_DISTANCE+1.5){end(s,now,"walked_away",out);return;}
        if(s.messages>= (player?1:MAX_MESSAGES)){end(s,now,"finished",out);return;}
        if(!elapsed(now,s.lastMessage,MESSAGE_COOLDOWN)||!elapsed(now,lastSpeech,GLOBAL_SPEECH_COOLDOWN)
            ||speechTimes.size()>=MAX_SPEECH_PER_MINUTE)return;
        String speaker=s.messages%2==0?s.a:s.b,peer=speaker.equals(s.a)?s.b:s.a;
        Npc who=npcs.get(speaker),other=npcs.get(peer);
        out.add(action(player?Kind.TALK_PLAYER:Kind.TALK_PAIR,s,speaker,peer,who.x(),who.y(),fallback(s,who,other,s.messages,world.night())));
        s.messages++;s.lastMessage=now;lastSpeech=now;speechTimes.addLast(now);
        if(!s.eventId.isEmpty())consume(s.eventId);
    }

    private void approach(Session s,Npc a,Npc b,double bx,double by,List<Action> out) {
        // Match AI Town's midpoint approach for distant pairs. The adapter chooses passable neighbors.
        double dx=bx-a.x(),dy=by-a.y(),length=Math.max(1,Math.hypot(dx,dy));
        if(b==null||!b.canApproach()) {
            if(a.canApproach())out.add(action(Kind.APPROACH,s,a.id(),s.b,bx-dx/length,by-dy/length,""));
        }
        else if(!a.canApproach())out.add(action(Kind.APPROACH,s,b.id(),a.id(),a.x()+dx/length,a.y()+dy/length,""));
        else {
            double mx=(a.x()+bx)/2,my=(a.y()+by)/2;
            out.add(action(Kind.APPROACH,s,a.id(),b.id(),mx-dx/length*.65,my-dy/length*.65,""));
            out.add(action(Kind.APPROACH,s,b.id(),a.id(),mx+dx/length*.65,my+dy/length*.65,""));
        }
    }
    private record Candidate(Npc a,String b,String topic,String evidence,String eventId,boolean urgent,double score) {}
    private Candidate eventCandidate(Snapshot world,Map<String,Npc> npcs) {
        if(!world.playerPresent()||world.playerBusy()||reserved(PLAYER))return null;
        return world.events().stream().filter(e->!consumedEvents.contains(e.id()))
            .filter(e->e.occurredAt()<=world.nowMillis()&&world.nowMillis()-e.occurredAt()<=300_000)
            .sorted(Comparator.comparingInt((Event e)->priority(e.kind())).thenComparingLong(Event::occurredAt).thenComparing(Event::id))
            .map(e->{Npc n=npcs.get(e.npcId());boolean urgent=e.kind()==EventKind.RESCUE_NEEDED;
                if(!available(n,urgent)||reserved(n.id())||!cool(n.id(),PLAYER,world.nowMillis(),urgent))return null;
                double d=distance(n.x(),n.y(),world.playerX(),world.playerY());
                if(d>MAX_VISIT_DISTANCE||((n.injured()||!n.canApproach())&&d>TALK_DISTANCE))return null;
                return new Candidate(n,PLAYER,e.kind().name().toLowerCase(java.util.Locale.ROOT),e.evidence(),e.id(),urgent,0);
            }).filter(Objects::nonNull).findFirst().orElse(null);
    }
    private Candidate nightCandidate(Snapshot world,Map<String,Npc> npcs) {
        String eventId="night:"+world.dayIndex();
        if(!world.night()||!world.playerPresent()||world.playerBusy()||reserved(PLAYER)||consumedEvents.contains(eventId))return null;
        return npcs.values().stream().filter(n->available(n,false)&&!reserved(n.id())&&cool(n.id(),PLAYER,world.nowMillis(),false))
            .filter(n->n.traits().caution()>=.55&&distance(n.x(),n.y(),world.playerX(),world.playerY())<=(n.canApproach()?MAX_VISIT_DISTANCE:TALK_DISTANCE))
            .sorted(Comparator.comparingDouble((Npc n)->n.traits().caution()).reversed().thenComparing(Npc::id))
            .map(n->new Candidate(n,PLAYER,"night_reminder","观察：世界当前为夜晚。未观察到额外怪物、库存或任务事实。",eventId,false,0))
            .findFirst().orElse(null);
    }
    private Candidate pairCandidate(Snapshot world,Map<String,Npc> npcs) {
        List<Npc> free=npcs.values().stream().filter(n->available(n,false)&&!reserved(n.id())).toList();
        Candidate best=null;
        for(int i=0;i<free.size();i++)for(int j=i+1;j<free.size();j++) {
            Npc a=free.get(i),b=free.get(j);double d=distance(a.x(),a.y(),b.x(),b.y());
            if(d>MAX_VISIT_DISTANCE||(!a.canApproach()&&!b.canApproach()&&d>TALK_DISTANCE)||!cool(a.id(),b.id(),world.nowMillis(),false))continue;
            if(b.traits().sociability()>a.traits().sociability()){Npc swap=a;a=b;b=swap;}
            double score=NpcBehaviorPolicy.socialInterest(a.traits(),world.night(),d)+b.traits().warmth()*.08;
            if(score<.19)continue;
            String topic=NpcBehaviorPolicy.topic(a.traits(),world.night());
            Candidate candidate=new Candidate(a,b.id(),topic,"观察：两人目前空闲、未重伤、未战斗且距离为"+Math.round(d)+"格。", "",false,score);
            if(best==null||candidate.score>best.score)best=candidate;
        }
        return best;
    }
    private boolean accepts(Session s,Npc b,boolean night) {
        double preference=.5+b.traits().sociability()*.35+b.traits().warmth()*.1-(night?b.traits().caution()*.08:0);
        double sample=(Integer.toUnsignedLong(Objects.hash(s.a,s.b,s.created/30_000))%1000)/1000.;
        return sample<preference;
    }
    private static boolean available(Npc n,boolean rescue) {
        return n!=null&&n.friendly()&&!n.playerConversing()&&(rescue&&n.injured()||!n.busy()&&!n.inCombat()&&!n.injured());
    }
    private boolean reserved(String id) { return active.values().stream().anyMatch(s->s.a.equals(id)||s.b.equals(id)); }
    private boolean cool(String a,String b,long now,boolean urgent) {
        // Urgent observed distress may bypass ordinary personal cooldown, never the pair cooldown.
        return (urgent||elapsed(now,personLast.getOrDefault(a,Long.MIN_VALUE),PERSON_COOLDOWN))
            &&(b.equals(PLAYER)||elapsed(now,personLast.getOrDefault(b,Long.MIN_VALUE),PERSON_COOLDOWN))
            &&elapsed(now,pairLast.getOrDefault(pair(a,b),Long.MIN_VALUE),urgent?30_000:PAIR_COOLDOWN);
    }
    private void end(Session s,long now,String reason,List<Action> out) {
        active.remove(s.id);personLast.put(s.a,now);if(!s.b.equals(PLAYER))personLast.put(s.b,now);pairLast.put(pair(s.a,s.b),now);
        out.add(new Action(Kind.END,s.id,s.a,s.b,0,0,s.topic,reason,""));
    }
    /** Native path failure/model rejection releases both participants and starts normal cooldown. */
    public List<Action> fail(String conversationId,long nowMillis) {
        Session s=active.get(conversationId);if(s==null)return List.of();List<Action> out=new ArrayList<>();end(s,nowMillis,"execution_failed",out);return List.copyOf(out);
    }
    public boolean owns(String conversationId,String npcId) { Session s=active.get(conversationId);return s!=null&&(s.a.equals(npcId)||s.b.equals(npcId)); }
    public boolean isConversing(String npcId){return reserved(npcId);}
    public void prepareWorld(String id,long now){if(!worldId.equals(id)||now<lastTick){clear();worldId=id;}}
    public void markReported(String eventId){if(eventId!=null&&!eventId.isBlank())consume(eventId);}
    public void allowRetry(String eventId){consumedEvents.remove(eventId);eventOrder.remove(eventId);}
    public int activeConversations() { return active.size(); }
    /** Call on world disposal; active approaches are ephemeral and must never be restored from a save. */
    public void clear() { active.clear();personLast.clear();pairLast.clear();consumedEvents.clear();eventOrder.clear();speechTimes.clear();
        worldId="";lastTick=-1;lastStart=Long.MIN_VALUE;lastSpeech=Long.MIN_VALUE;sequence=0; }
    private void consume(String id) { if(consumedEvents.add(id))eventOrder.addLast(id);while(eventOrder.size()>1024)consumedEvents.remove(eventOrder.removeFirst()); }
    private static Action action(Kind kind,Session s,String speaker,String peer,double x,double y,String text) {
        return new Action(kind,s.id,speaker,peer,x,y,s.topic,s.evidence,text);
    }
    private static String fallback(Session s,Npc speaker,Npc peer,int message,boolean night) {
        if(s.topic.equals("rescue_needed"))return "我伤得很重，你能来帮帮我吗？";
        // Current native evidence proves recovery, not who performed the rescue.
        if(s.topic.equals("rescued"))return "我已经能活动了，刚才那段经历还记得。";
        if(s.topic.equals("task_completed"))return "交给我的那项工作已经做完了，我来跟你说一声。";
        if(s.topic.equals("night_reminder"))return "天已经黑了，我们尽量别走散，互相照应。";
        String name=peer==null?"":peer.name()+"，";
        if(message>=2)return speaker.traits().warmth()>=.6?"聊一会儿心里踏实多了。我们待会儿再聊。":"我先去忙了，待会儿见。";
        if(message==1)return switch(s.topic) {
            case "stay_together"->"好，互相照应。我就在附近。";
            case "check_on_friend"->"谢谢你惦记我。能一起待一会儿挺好的。";
            case "camp_company"->"好啊，忙完能有个人聊聊也不错。";
            default->"嗯，一起歇一会儿吧。";
        };
        return switch(s.topic) {
            case "stay_together"->name+"夜里我们别走得太散，互相照应吧。";
            case "check_on_friend"->name+"你现在还好吗？要不要一起歇一会儿？";
            case "camp_company"->name+"现在有空吗？想跟你聊一会儿。";
            default->name+"我想在这儿歇一会儿，你要一起吗？";
        };
    }
    private static int priority(EventKind kind) { return switch(kind){case RESCUE_NEEDED->0;case RESCUED->1;case TASK_COMPLETED->2;}; }
    private static boolean elapsed(long now,long previous,long duration) { return previous==Long.MIN_VALUE||now>=previous&&now-previous>=duration; }
    private static double distance(double ax,double ay,double bx,double by) { return Math.hypot(ax-bx,ay-by); }
    private static String pair(String a,String b) { return a.compareTo(b)<0?a+"\u0000"+b:b+"\u0000"+a; }
    private static String required(String text) { if(text==null||text.isBlank())throw new IllegalArgumentException("Missing value");return text; }
}
