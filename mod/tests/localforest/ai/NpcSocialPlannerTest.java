package localforest.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import localforest.ai.NpcBehaviorPolicy.Traits;
import localforest.ai.NpcSocialPlanner.Action;
import localforest.ai.NpcSocialPlanner.Event;
import localforest.ai.NpcSocialPlanner.EventKind;
import localforest.ai.NpcSocialPlanner.Kind;
import localforest.ai.NpcSocialPlanner.Npc;
import localforest.ai.NpcSocialPlanner.Snapshot;

/** Pure server-policy fixture: no game runtime, configuration, HTTP or paid model calls. */
public final class NpcSocialPlannerTest {
    private static final Traits FRIENDLY=new Traits(1,.2,1,.8),CAUTIOUS=new Traits(.7,.9,.7,.5);
    public static void main(String[] ignored) {
        lifecycleAndDeterminism();doesNotInterrupt();movementPermissionsAndFailure();observedEventPriority();injuredDistressDoesNotMove();reportedAndUndeliveredRetry();nightAndWorldIsolation();frequencyAndMultipleCharacters();personalityAffectsBehavior();
        System.out.println("NpcSocialPlannerTest PASS: AI Town invite/accept/approach/talk/leave, deterministic personality, real event priority, busy/combat/chat protection, injured busy/combat distress without movement, reported-event replay/retry, native movement eligibility, failure/timeout cleanup, night/day, world isolation, rate limits and multiple characters; no paid calls");
    }
    private static void lifecycleAndDeterminism() {
        var a=npc("a",0,0,true,FRIENDLY);var b=npc("b",8,0,true,FRIENDLY);
        var first=new NpcSocialPlanner();var second=new NpcSocialPlanner();
        List<Action> invite=first.tick(snap(0,false,List.of(a,b),List.of(),false,false));
        check(invite.equals(second.tick(snap(0,false,List.of(b,a),List.of(),false,false))),"Input ordering changed deterministic plan");
        check(has(invite,Kind.INVITE),"No invitation");String id=invite.get(0).conversationId();
        var approach=first.tick(snap(1_000,false,List.of(a,b),List.of(),false,false));
        check(has(approach,Kind.ACCEPT)&&count(approach,Kind.APPROACH)==2,"Invitation did not cause both native movement requests");
        var near=List.of(npc("a",3.5,0,true,FRIENDLY),npc("b",4.5,0,true,FRIENDLY));
        var start=first.tick(snap(2_000,false,near,List.of(),false,false));
        check(count(start,Kind.TALK_PAIR)==1,"Nearby arrival did not begin speech");
        String firstSpeaker=start.stream().filter(x->x.kind()==Kind.TALK_PAIR).findFirst().orElseThrow().speakerId();
        check(!has(first.tick(snap(3_000,false,near,List.of(),false,false)),Kind.TALK_PAIR),"Message cooldown skipped");
        var response=first.tick(snap(7_000,false,near,List.of(),false,false));
        check(response.stream().anyMatch(x->x.kind()==Kind.TALK_PAIR&&!x.speakerId().equals(firstSpeaker)),"Other NPC did not answer");
        check(has(first.tick(snap(12_000,false,near,List.of(),false,false)),Kind.TALK_PAIR),"Bounded closing line missing");
        check(has(first.tick(snap(13_000,false,near,List.of(),false,false)),Kind.END)&&!first.owns(id,"a"),"Conversation reservation not released");
        check(!has(first.tick(snap(100_000,false,near,List.of(),false,false)),Kind.INVITE),"Pair cooldown skipped");
    }
    private static void doesNotInterrupt() {
        var healthy=npc("b",1,0,true,FRIENDLY);
        for(Npc protectedNpc:List.of(
            new Npc("a",1,"甲",0,0,true,true,false,false,false,true,FRIENDLY),
            new Npc("a",1,"甲",0,0,true,false,true,false,false,true,FRIENDLY),
            new Npc("a",1,"甲",0,0,true,false,false,true,false,true,FRIENDLY),
            new Npc("a",1,"甲",0,0,true,false,false,false,true,true,FRIENDLY),
            new Npc("a",1,"甲",0,0,false,false,false,false,false,true,FRIENDLY))) {
            var planner=new NpcSocialPlanner();check(planner.tick(snap(0,false,List.of(protectedNpc,healthy),List.of(),false,false)).isEmpty(),"Protected character was interrupted");
        }
        var planner=new NpcSocialPlanner();var free=List.of(npc("a",0,0,true,FRIENDLY),healthy);
        planner.tick(snap(0,false,free,List.of(),false,false));
        var busy=new Npc("a",1,"甲",0,0,true,true,false,false,false,true,FRIENDLY);
        check(has(planner.tick(snap(1_000,false,List.of(busy,healthy),List.of(),false,false)),Kind.END),"New player task did not interrupt social reservation");
    }
    private static void movementPermissionsAndFailure() {
        var planner=new NpcSocialPlanner();var far=List.of(npc("a",0,0,false,FRIENDLY),npc("b",8,0,false,FRIENDLY));
        check(planner.tick(snap(0,false,far,List.of(),false,false)).isEmpty(),"Two uncommandable visitors received distant invitation");
        planner=new NpcSocialPlanner();var oneCanMove=List.of(npc("a",0,0,false,FRIENDLY),npc("b",8,0,true,FRIENDLY));
        var invitation=planner.tick(snap(0,false,oneCanMove,List.of(),false,false));String id=invitation.get(0).conversationId();
        var actions=planner.tick(snap(1_000,false,oneCanMove,List.of(),false,false));
        check(actions.stream().filter(x->x.kind()==Kind.APPROACH).allMatch(x->x.speakerId().equals("b"))&&count(actions,Kind.APPROACH)==1,"Visitor movement permission ignored");
        check(has(planner.fail(id,2_000),Kind.END)&&planner.activeConversations()==0,"Native movement failure leaked reservations");
        check(planner.fail(id,3_000).isEmpty(),"Failure applied twice");
        planner=new NpcSocialPlanner();planner.tick(snap(0,false,oneCanMove,List.of(),false,false));planner.tick(snap(1_000,false,oneCanMove,List.of(),false,false));
        check(has(planner.tick(snap(19_000,false,oneCanMove,List.of(),false,false)),Kind.END),"Blocked path did not time out");
        planner=new NpcSocialPlanner();var close=List.of(npc("a",0,0,false,FRIENDLY),npc("b",1,0,false,FRIENDLY));
        planner.tick(snap(0,false,close,List.of(),false,false));check(has(planner.tick(snap(1_000,false,close,List.of(),false,false)),Kind.TALK_PAIR),"Nearby visitors could not socialize without native commands");
    }
    private static void observedEventPriority() {
        var npc=npc("a",0,0,true,FRIENDLY);var planner=new NpcSocialPlanner();
        var events=List.of(new Event("task-1",EventKind.TASK_COMPLETED,"a","服务器任务记录 #1 已完成。",0),
            new Event("rescue-1",EventKind.RESCUED,"a","玩家刚刚实际救起了甲。",0));
        var invitation=planner.tick(snap(0,false,List.of(npc),events,true,false));
        check(invitation.get(0).topicId().equals("rescued"),"Task report outranked rescue gratitude");
        var utterance=planner.tick(snap(1_000,false,List.of(npc),events,true,false));
        check(utterance.stream().anyMatch(x->x.kind()==Kind.TALK_PLAYER&&x.evidence().equals("玩家刚刚实际救起了甲。")&&x.fallbackText().contains("能活动")&&!x.fallbackText().contains("你来救")),"Recovery statement invented an actor or lost its evidence");
        planner.tick(snap(2_000,false,List.of(npc),events,true,false));
        var replay=planner.tick(snap(200_000,false,List.of(npc),events,true,false));
        check(replay.stream().noneMatch(x->x.topicId().equals("rescued")),"Consumed rescue event repeated");
        var hurt=new Npc("hurt",3,"受伤者",1,0,true,false,true,false,false,false,CAUTIOUS);
        var distress=List.of(new Event("hurt-1",EventKind.RESCUE_NEEDED,"hurt","原生角色当前处于可救援重伤状态。",0));
        planner=new NpcSocialPlanner();planner.tick(snap(0,false,List.of(hurt),distress,true,false));
        check(has(planner.tick(snap(1_000,false,List.of(hurt),distress,true,false)),Kind.TALK_PLAYER),"Observed injured NPC could not ask for help");
        check(new NpcSocialPlanner().tick(snap(0,false,List.of(npc),events,true,true)).isEmpty(),"Player typing/combat was interrupted by automatic report");
        var future=List.of(new Event("future",EventKind.RESCUED,"a","未发生事件。",10_000));
        check(new NpcSocialPlanner().tick(snap(0,false,List.of(npc),future,true,false)).isEmpty(),"Future event spoken as fact");
    }
    private static void nightAndWorldIsolation() {
        var planner=new NpcSocialPlanner();var npcs=List.of(npc("a",1,0,true,CAUTIOUS));
        check(planner.tick(snap(0,false,npcs,List.of(),true,false)).isEmpty(),"Daytime issued night warning");
        check(has(planner.tick(snap(1_000,true,npcs,List.of(),true,false)),Kind.INVITE),"Cautious NPC never notices nighttime");
        check(planner.tick(snap(2_000,true,npcs,List.of(),true,false)).stream().anyMatch(x->x.topicId().equals("night_reminder")&&x.kind()==Kind.TALK_PLAYER),"Night reminder missing");
        planner.tick(snap(3_000,true,npcs,List.of(),true,false));
        check(planner.tick(snap(250_000,true,npcs,List.of(),true,false)).isEmpty(),"Night warning repeated within same day");
        check(has(planner.tick(new Snapshot("other-level",250_000,1,true,0,0,true,false,npcs,List.of())),Kind.INVITE),"Another world's social state leaked into this world");
        check(has(planner.tick(new Snapshot("other-level",100,1,true,0,0,true,false,npcs,List.of())),Kind.INVITE),"Clock rollback kept stale in-flight session");
    }
    private static void injuredDistressDoesNotMove(){
        var hurt=new Npc("hurt",3,"受伤者",1,0,true,true,true,true,false,false,CAUTIOUS);
        var distress=List.of(new Event("hurt-busy-combat",EventKind.RESCUE_NEEDED,"hurt","角色已被原生系统确认为重伤可救援。",0));
        var planner=new NpcSocialPlanner();
        var invite=planner.tick(snap(0,false,List.of(hurt),distress,true,false));
        var speech=planner.tick(snap(1_000,false,List.of(hurt),distress,true,false));
        check(has(invite,Kind.INVITE)&&has(speech,Kind.TALK_PLAYER),"Real nearby distress was suppressed by the stale task/combat flags");
        check(!has(invite,Kind.APPROACH)&&!has(speech,Kind.APPROACH),"Downed NPC was given an approach command");
        check(speech.stream().anyMatch(a->a.kind()==Kind.TALK_PLAYER&&a.topicId().equals("rescue_needed")&&a.fallbackText().contains("帮帮我")),"Distress lost its rescue request");
        var far=new Npc("hurt",3,"受伤者",8,0,true,true,true,true,false,false,CAUTIOUS);
        check(new NpcSocialPlanner().tick(snap(0,false,List.of(far),distress,true,false)).isEmpty(),"Downed distress tried to move to a distant player");
        check(new NpcSocialPlanner().tick(snap(0,false,List.of(hurt),List.of(),true,false)).isEmpty(),"No observed event still triggered a fabricated distress call");
    }
    private static void reportedAndUndeliveredRetry(){
        var npc=npc("a",0,0,true,FRIENDLY);var npcs=List.of(npc);
        var event=new Event("persistent-report",EventKind.TASK_COMPLETED,"a","一项已被服务器确认的工作已经完成。",0);
        var planner=new NpcSocialPlanner();
        // Bind this level first, matching the adapter's restored-report initialization contract.
        planner.tick(snap(0,false,npcs,List.of(),true,false));
        planner.markReported(event.id());
        check(planner.tick(snap(1_000,false,npcs,List.of(event),true,false)).isEmpty(),"A persisted reported event was invited again");
        planner.allowRetry(event.id());
        var invite=planner.tick(snap(2_000,false,npcs,List.of(event),true,false));
        check(has(invite,Kind.INVITE),"An explicitly undelivered event could not be retried");
        var spoken=planner.tick(snap(3_000,false,npcs,List.of(event),true,false));
        check(has(spoken,Kind.TALK_PLAYER),"Retried event did not become one utterance");
        planner.allowRetry(event.id());
        planner.fail(invite.get(0).conversationId(),4_000);
        var fresh=new Event(event.id(),event.kind(),event.npcId(),event.evidence(),0);
        var again=planner.tick(snap(200_000,false,npcs,List.of(fresh),true,false));
        check(has(again,Kind.INVITE),"Failed delivery kept the event consumed after normal cooldown");
        planner.fail(again.get(0).conversationId(),201_000);
        // Restore the persisted report into a fresh bound planner, so a cooldown cannot make
        // this assertion pass accidentally while the report itself has actually been forgotten.
        planner=new NpcSocialPlanner();planner.tick(snap(250_000,false,npcs,List.of(),true,false));
        planner.markReported(event.id());
        check(planner.tick(snap(251_000,false,npcs,List.of(fresh),true,false)).isEmpty(),"Final delivered report was not retained on planner recreation");
        planner.allowRetry("missing");planner.markReported("");planner.markReported(null);
    }
    private static void frequencyAndMultipleCharacters() {
        var planner=new NpcSocialPlanner();List<Npc> npcs=new ArrayList<>();
        for(int i=0;i<20;i++)npcs.add(npc(String.format("n%02d",i),(i/2)*30+(i%2),0,false,FRIENDLY));
        List<Long> utterances=new ArrayList<>();Set<String> speakers=new HashSet<>();long lastInvite=-8_000;
        for(long now=0;now<300_000;now+=1_000) {
            List<Action> actions=planner.tick(snap(now,false,npcs,List.of(),false,false));
            for(Action action:actions) {
                if(action.kind()==Kind.INVITE){check(now-lastInvite>=NpcSocialPlanner.GLOBAL_START_COOLDOWN,"Global invitation throttle bypassed");lastInvite=now;}
                if(action.kind()==Kind.TALK_PAIR){utterances.add(now);speakers.add(action.speakerId());}
            }
            final long moment=now;
            check(utterances.stream().filter(t->moment-t<60_000).count()<=NpcSocialPlanner.MAX_SPEECH_PER_MINUTE,"Automatic utterance budget bypassed");
            check(planner.activeConversations()<=NpcSocialPlanner.MAX_CONCURRENT,"Unlimited simultaneous conversations");
        }
        check(speakers.size()>=6,"Planner only ever socialized the original two characters");
        check(utterances.size()>=8,"Rate limits suppressed all social activity");
    }
    private static void personalityAffectsBehavior() {
        var risky=new NpcBehaviorPolicy.Situation(1,false,false,true,true,true,true);
        check(NpcBehaviorPolicy.evaluate("HARVEST",new Traits(.5,1,.5,0),risky).choice()==NpcBehaviorPolicy.Choice.DECLINE,"Cautious distrustful NPC accepted known night danger");
        check(NpcBehaviorPolicy.evaluate("HARVEST",new Traits(.5,0,.5,1),risky).allowed(),"Brave trusting NPC cannot take an acceptable risk");
        check(NpcBehaviorPolicy.evaluate("FOLLOW",FRIENDLY,new NpcBehaviorPolicy.Situation(.1,true,false,false,false,false,true)).choice()==NpcBehaviorPolicy.Choice.REQUEST_RESCUE,"Serious injury did not affect behavior");
        check(!NpcBehaviorPolicy.evaluate("FOLLOW",FRIENDLY,new NpcBehaviorPolicy.Situation(1,false,false,false,false,false,false)).allowed(),"Personality granted recruitment permission");
        check(NpcBehaviorPolicy.socialInterest(FRIENDLY,false,1)>NpcBehaviorPolicy.socialInterest(new Traits(0,.9,0,.5),false,1),"Sociability does not affect initiative");
        check(NpcBehaviorPolicy.topic(CAUTIOUS,true).equals("stay_together"),"Caution does not affect social topic");
    }
    private static Npc npc(String id,double x,double y,boolean canMove,Traits traits) { return new Npc(id,id.hashCode(),id,x,y,true,false,false,false,false,canMove,traits); }
    private static Snapshot snap(long now,boolean night,List<Npc> npcs,List<Event> events,boolean playerPresent,boolean playerBusy) { return new Snapshot("level-1",now,1,night,0,0,playerPresent,playerBusy,npcs,events); }
    private static boolean has(List<Action> actions,Kind kind) { return actions.stream().anyMatch(a->a.kind()==kind); }
    private static long count(List<Action> actions,Kind kind) { return actions.stream().filter(a->a.kind()==kind).count(); }
    private static void check(boolean good,String message) { if(!good)throw new AssertionError(message); }
}
