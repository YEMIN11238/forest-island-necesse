package localforest.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.CompanionView;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;

/** Explicit opt-in paid smoke check, limited to three requests in a disposable world. */
public final class NativeModelCheck {
    private record Ask(int index,String companion,String text) {}
    private record Reply(int index,String text) {}
    private volatile Ask ask;
    private volatile Reply reply;
    private volatile int issued;
    private int phase;
    private String id;
    private long revision,started,initialCalls,initialInputs,initialOutputs;
    private DeepSeekProvider provider;
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Map<String,Object>> interactions=new ArrayList<>();
    public Map<String,Object> evidence(){return evidence;}
    public void frame(Client client){
        Ask next=ask;if(next==null||issued==next.index())return;
        issued=next.index();localforest.ui.CompanionPanel.openForChecks();
        CompanionSystem.submitText(client,next.companion(),next.text(),message->reply=new Reply(next.index(),message));
    }
    public boolean tick(ServerClient server,Client client,long now){
        if(client==null)return false;
        var all=CompanionSystem.snapshot(client).companions();if(all.isEmpty())return false;
        if(phase==0){
            if(!(AiIntentService.providerForChecks() instanceof DeepSeekProvider live))throw new IllegalStateException("Local DeepSeek is not configured");
            provider=live;initialCalls=live.completedCalls();initialInputs=live.inputTokens();initialOutputs=live.outputTokens();
            CompanionView v=all.stream().filter(p->p.name().contains("禾")).findFirst().orElse(all.get(0));id=v.id();
            evidence.put("provider","deepseek-flash");evidence.put("thinking",false);evidence.put("maximumRequests",3);evidence.put("noInventoryInjection",true);
            issue(v,1,"跟着我走，我们一起行动。",now);return false;
        }
        if(now-started>35_000_000_000L)throw new IllegalStateException("AI native check timed out in phase "+phase+"; reply="+(reply==null?"pending":reply.text()));
        CompanionView v=all.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow();
        HumanMob mob=server.getLevel().entityManager.mobs.stream().filter(m->m.getUniqueID()==v.mobId()).filter(m->m instanceof HumanMob).map(m->(HumanMob)m).findFirst().orElseThrow();
        Reply response=reply;if(response==null||response.index()!=phase)return false;
        if(phase==1&&v.revision()>revision&&v.action().equals("FOLLOW")&&mob.commandFollowMob==server.playerMob){
            interactions.add(Map.of("request","follow","reply",response.text(),"nativeFollowMatched",true));
            issue(v,2,"取消跟随，请取消当前指令。",now);
        }else if(phase==2&&v.revision()>revision&&v.state().equals("已取消")&&mob.commandFollowMob==null&&mob.commandGuardPoint==null){
            interactions.add(Map.of("request","cancel","reply",response.text(),"nativeCancelMatched",true));
            issue(v,3,"你叫什么名字？记得我们刚才做过什么吗？只聊天，不改变当前任务。",now);
        }else if(phase==3){
            if(v.revision()!=revision)throw new IllegalStateException("Conversation unexpectedly changed the task");
            if(!response.text().contains("小禾"))throw new IllegalStateException("Companion did not retain the supplied identity");
            if(provider.completedCalls()-initialCalls!=3)throw new IllegalStateException("Conversation did not complete three real model calls");
            interactions.add(Map.of("request","conversation","reply",response.text(),"taskUnchanged",true));ask=null;
            evidence.put("interactions",interactions);evidence.put("successfulModelCalls",provider.completedCalls()-initialCalls);
            long in=provider.inputTokens()-initialInputs,out=provider.outputTokens()-initialOutputs;
            evidence.put("inputTokens",in);evidence.put("outputTokens",out);evidence.put("peakRateUpperEstimateRmb",AiUsageLedger.conservativeMicros(in,out)/1e6);
            evidence.put("passed",true);return true;
        }
        return false;
    }
    private void issue(CompanionView v,int phase,String text,long now){this.phase=phase;revision=v.revision();started=now;ask=new Ask(phase,v.id(),text);}
}
