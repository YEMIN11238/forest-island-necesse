package localforest.companions;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import localforest.ai.MemoryRanking;
import localforest.ai.NpcMind;
import localforest.ai.NpcBehaviorPolicy;
import necesse.engine.GameEvents;
import necesse.engine.GameEventListener;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.events.ServerStopEvent;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.registries.WorldDataRegistry;

/** Single-player bridge: clients enqueue intentions; only WorldData.tick changes the world. */
public final class CompanionSystem {
    public static final String WORLD_KEY="MosslumeCompanions";
    public record MemoryEvent(String id,long worldTime,String type,String text,String source) {}
    public record CompanionView(String id,String name,int mobId,String level,int tileX,int tileY,boolean downed,
        long revision,String action,String state,String message,int completedSteps,List<MemoryEvent> memories,
        boolean recruited,String role,List<DialogueTurn> recentConversation,boolean loaded,
        NpcMind.Snapshot mind,double healthFraction,boolean threatened,String profile) {}
    public record WorldSpeech(String session,String level,int mobId,String text) {}
    public record DialogueTurn(long worldTime,String playerText,String replyText) {}
    /** Only dialogue is spoken by the character. Validation/errors stay in the interface. */
    public record ConversationReply(String dialogue,String status,long ticket) {
        public ConversationReply(String dialogue,String status){this(dialogue,status,-1);}
        public ConversationReply { dialogue=dialogue==null?"":dialogue;status=status==null?"":status; }
        public static ConversationReply status(String text){return new ConversationReply("",text);}
        public static ConversationReply dialogue(String text){return new ConversationReply(text,"");}
        public String combined(){return dialogue.isBlank()?status:status.isBlank()?dialogue:dialogue+"\n"+status;}
    }
    public record View(String sessionId,List<CompanionView> companions) {}
    public record TextRequest(Client client,String sessionId,String companionId,long revision,String text,String context,long requestEpoch,int playerTileX,int playerTileY,boolean commandsAllowed,List<DialogueTurn> recentConversation,String level) {}
    public interface TextHandler { void handle(TextRequest request,Consumer<ConversationReply> reply); }
    record Command(int slot,String session,String companion,long revision,String action,int x,int y,int amount,long requestEpoch) {}
    private static volatile TextHandler textHandler;
    private static volatile boolean initialized;
    private static final ConcurrentHashMap<Server,CompanionWorldData> active=new ConcurrentHashMap<>();
    public static synchronized void init(){
        if(initialized)return;initialized=true;
        WorldDataRegistry.registerWorldData("mosslumecompanions",CompanionWorldData.class);
        GameEvents.addListener(ServerClientConnectedEvent.class,new GameEventListener<ServerClientConnectedEvent>(){
            @Override public void onEvent(ServerClientConnectedEvent e){
                if(!localforest.world.IslandWorld.enabled())return;
                var world=e.client.getServer().world.worldEntity;
                if(world.getWorldData(WORLD_KEY)==null)world.addWorldData(WORLD_KEY,new CompanionWorldData());
            }
        });
        GameEvents.addListener(ServerStopEvent.class,new GameEventListener<ServerStopEvent>(){
            @Override public void onEvent(ServerStopEvent event){CompanionWorldData old=active.remove(event.server);if(old!=null)old.clearPendingConversations();}
        });
    }
    static void register(Server server,CompanionWorldData data){
        // A new local session invalidates every outstanding request from the old one.
        active.forEach((oldServer,old)->{if(oldServer!=server)old.clearPendingConversations();});
        active.entrySet().removeIf(e->e.getKey()!=server);active.put(server,data);
    }
    public static View snapshot(Client client){
        CompanionWorldData d=data(client);return d==null?new View("",List.of()):d.view;
    }
    public static void updateScenePresence(Client client,boolean interactive,String talkingTo){
        CompanionWorldData d=data(client);if(d==null)return;
        String next=talkingTo==null?"":talkingTo;
        if(!next.isEmpty()&&!next.equals(d.talkingTo))localforest.ai.AiIntentService.cancelRequest(d.sessionId,next);
        d.sceneInteractive=interactive;d.scenePresenceAt=System.nanoTime();d.talkingTo=next;
    }
    public static List<WorldSpeech> pollWorldSpeech(Client client){
        CompanionWorldData d=data(client);if(d==null)return List.of();
        java.util.ArrayList<WorldSpeech> result=new java.util.ArrayList<>();
        for(int i=0;i<8;i++){WorldSpeech line=d.speech.poll();if(line==null)break;
            if(d.sessionId.equals(line.session())&&client.getLevel()!=null&&line.level().equals(client.getLevel().getIdentifier().toString()))result.add(line);
        }return List.copyOf(result);
    }
    public static NpcBehaviorPolicy.Traits behaviorTraits(NpcMind.Snapshot mind){
        var t=mind.traits();return new NpcBehaviorPolicy.Traits(t.sociability(),t.caution(),.45+t.trust()*.3,t.trust());
    }
    public static String assessIntent(TextRequest request,String action){
        var v=snapshot(request.client()).companions().stream().filter(p->p.id().equals(request.companionId())).findFirst().orElse(null);
        if(v==null)return "对方暂时不在这里。";
        boolean night=request.client().getLevel()!=null&&request.client().getLevel().getWorldEntity().isNight();
        var decision=NpcBehaviorPolicy.evaluate(action,behaviorTraits(v.mind()),new NpcBehaviorPolicy.Situation(v.healthFraction(),v.downed(),v.threatened(),night,v.threatened(),false,v.recruited()));
        return decision.allowed()?"":decision.fallbackText();
    }
    private static CompanionWorldData data(Client c){return c==null||c.isDisconnected()||c.getLocalServer()==null?null:active.get(c.getLocalServer());}
    public static boolean isSessionCurrent(Client c,String session){CompanionWorldData d=data(c);return d!=null&&d.sessionId.equals(session);}
    public static boolean isRequestCurrent(TextRequest r){
        CompanionWorldData d=data(r.client());
        if(d==null||!d.sessionId.equals(r.sessionId())||!d.requests.isCurrent(r.companionId(),r.requestEpoch()))return false;
        if(r.client().getPlayer()==null||r.client().getPlayer().getLevel()==null||!r.level().equals(r.client().getPlayer().getLevel().getIdentifier().toString()))return false;
        return d.view.companions().stream().anyMatch(p->p.id().equals(r.companionId())&&p.loaded()&&p.level().equals(r.level())&&p.revision()==r.revision());
    }
    public static String submit(Client c,String companion,String action,int x,int y,int amount){
        CompanionWorldData d=data(c);if(d==null)return "伙伴系统正在载入";
        CompanionView v=d.view.companions().stream().filter(p->p.id().equals(companion)).findFirst().orElse(null);
        if(v==null)return "找不到这个角色";
        if(!v.recruited()&&!action.equals("OPEN_NATIVE")&&!action.equals("OPEN_RESCUE"))return "这位角色还不是营地伙伴，可以聊天或打开原生事务";
        // Invalidate on the calling thread: a paused server may not drain its queue yet.
        d.requests.invalidate(companion);d.pendingConversations.remove(companion);
        localforest.ai.AiIntentService.cancelRequest(d.sessionId,companion);
        d.commands.add(new Command(c.getSlot(),d.sessionId,companion,-1,action,x,y,amount,-1));return "指令已提交，等待伙伴确认";
    }
    public static String submitFromAi(TextRequest r,String action,int x,int y,int amount){
        return submitFromAi(r,action,x,y,amount,null);
    }
    public static String submitFromAi(TextRequest r,String action,int x,int y,int amount,Consumer<String> receipt){
        CompanionWorldData d=data(r.client());if(d==null||!isRequestCurrent(r))return "请求已经失效，请重新发起请求";
        if(!r.commandsAllowed())return "这位角色还不是营地伙伴，无法安排营地任务";
        // Deliver the pending receipt before the server can increment the revision
        // for this same action. Later validation/blocked/completed status stays native.
        if(receipt!=null)receipt.accept("已转达，伙伴会根据现场情况行动。");
        // The proposal has been accepted. Closing a finished chat must not revoke this task;
        // the server still checks the bound revision, current map and native permissions.
        d.commands.add(new Command(r.client().getSlot(),r.sessionId(),r.companionId(),r.revision(),action,x,y,amount,-1));return "已转达，伙伴会根据现场情况行动。";
    }
    public static void setTextHandler(TextHandler handler){textHandler=handler;}
    public static boolean hasTextHandler(){return textHandler!=null;}
    /** Filtered to this companion's observed event log before applying the AI Town ranking port. */
    public static List<MemoryEvent> recall(Client client,String companion,String query,int limit){
        CompanionWorldData d=data(client);if(d==null)return List.of();
        CompanionView v=d.view.companions().stream().filter(p->p.id().equals(companion)).findFirst().orElse(null);
        if(v==null)return List.of();String q=query==null?"":query.trim();
        List<MemoryRanking.Candidate<MemoryEvent>> candidates=v.memories().stream().map(e->{
            double importance=switch(e.type()){case "rescued","downed"->5;case "task_completed","joined"->3;default->1;};
            double relevance=q.isEmpty()?0:e.text().contains(q)||e.type().contains(q)?1:0;
            return new MemoryRanking.Candidate<>(e.id(),e,relevance,importance,e.worldTime());
        }).toList();
        return MemoryRanking.rank(candidates,d.publishedWorldTime,Math.max(0,Math.min(16,limit)));
    }
    /** Compatibility surface for existing task checks and the management panel. */
    public static void submitText(Client client,String companion,String text,Consumer<String> reply){
        submitConversation(client,companion,text,result->{reply.accept(result.combined());confirmConversationReply(client,companion,result);});
    }
    public static void submitConversation(Client client,String companion,String text,Consumer<ConversationReply> reply){
        TextHandler handler=textHandler;if(handler==null){reply.accept(ConversationReply.status("模型尚未连接，离线任务按钮可正常使用"));return;}
        View view=snapshot(client);CompanionView v=view.companions().stream().filter(p->p.id().equals(companion)).findFirst().orElse(null);
        if(v==null||!v.loaded()){reply.accept(ConversationReply.status("角色尚未载入"));return;}
        if(text==null||text.isBlank()||text.length()>2000){reply.accept(ConversationReply.status("请输入 1～2000 字的请求"));return;}
        if(client.getPlayer()==null||client.getPlayer().getLevel()==null){reply.accept(ConversationReply.status("玩家尚未载入"));return;}
        String level=client.getPlayer().getLevel().getIdentifier().toString();
        if(!v.level().equals(level)){reply.accept(ConversationReply.status("靠近对方，再和他交谈。"));return;}
        StringBuilder context=new StringBuilder("角色：").append(v.name()).append("；原生身份：").append(v.role())
            .append("；是否已加入玩家营地：").append(v.recruited()).append("；状态：").append(v.state()).append("；当前任务：").append(v.message());
        if(!v.profile().isBlank())context.append("\n角色档案：").append(v.profile());
        int x=client.getPlayer().getTileX(),y=client.getPlayer().getTileY();
        context.append("\n玩家当前格 tileX=").append(x).append(" tileY=").append(y)
            .append("；角色当前格 tileX=").append(v.tileX()).append(" tileY=").append(v.tileY())
            .append("；角色是否重伤=").append(v.downed());
        if(client.getPlayer().getLevel().tileWidth>0&&client.getPlayer().getLevel().tileHeight>0)
            context.append("；地图尺寸=").append(client.getPlayer().getLevel().tileWidth).append("x").append(client.getPlayer().getLevel().tileHeight);
        else context.append("；世界按原生区域连续生成，坐标可为负；只能使用本次上下文提供的玩家当前格，不自行猜测远处目标。");
        if(!v.recruited())context.append("\n这是独立的村民、商人或旅人；可以对话，但尚未加入玩家营地，无权接受跟随或营地工作指令。交易、招募、救援须通过原生事务。");
        recall(client,companion,text,8).forEach(e->context.append("\n共同经历数据：").append(e.text()));
        CompanionWorldData d=data(client);if(d==null||!d.sessionId.equals(view.sessionId()))return;
        context.append("\n").append(NpcMind.context(v.mind(),text,d.publishedWorldTime));
        if(d.pendingConversations.size()>=32&&!d.pendingConversations.containsKey(companion)){reply.accept(ConversationReply.status("正在进行的交谈较多，请稍后再说。"));return;}
        cancelConversation(client,companion);
        localforest.ai.AiIntentService.cancelRequest(d.sessionId,companion);
        TextRequest request=new TextRequest(client,view.sessionId(),v.id(),v.revision(),text,context.toString(),d.requests.issue(v.id()),x,y,v.recruited(),v.recentConversation(),level);
        var pending=new CompanionWorldData.PendingConversation(request,System.nanoTime()+30_000_000_000L,null);
        d.pendingConversations.put(companion,pending);
        handler.handle(request,result->{
            if(!isRequestCurrent(request)){d.pendingConversations.remove(companion,pending);return;}
            if(!result.dialogue().isBlank()){
                var completed=new CompanionWorldData.CompletedDialogue(view.sessionId(),v.id(),client.getSlot(),level,request.requestEpoch(),v.mobId(),text,result.dialogue());
                if(!d.pendingConversations.replace(companion,pending,new CompanionWorldData.PendingConversation(request,pending.deadline(),completed)))return;
                reply.accept(new ConversationReply(result.dialogue(),result.status(),request.requestEpoch()));
            }else if(d.pendingConversations.remove(companion,pending))reply.accept(result);
        });
    }
    /** Call after the scene UI accepts the reply; hidden/cancelled responses are never memories. */
    public static void confirmConversationReply(Client client,String companion,ConversationReply reply){
        if(reply==null||reply.ticket()<0||reply.dialogue().isBlank())return;
        CompanionWorldData d=data(client);if(d==null)return;
        var pending=d.pendingConversations.get(companion);
        if(pending==null||pending.delivery()==null||pending.request().requestEpoch()!=reply.ticket())return;
        var delivery=pending.delivery();
        if(!d.sessionId.equals(delivery.session())||client.getPlayer()==null||client.getPlayer().getLevel()==null
            ||!delivery.level().equals(client.getPlayer().getLevel().getIdentifier().toString()))return;
        if(!d.pendingConversations.remove(companion,pending))return;
        if(d.requests.acceptDialogue(companion,delivery.ticket()))d.dialogues.add(delivery);
    }
    public static void cancelConversation(Client client,String companion){
        CompanionWorldData d=data(client);if(d==null)return;
        if(d.pendingConversations.remove(companion)!=null){
            d.requests.invalidate(companion);
            localforest.ai.AiIntentService.cancelRequest(d.sessionId,companion);
        }
    }
    public static void invalidateModelRequests(Client client){
        CompanionWorldData d=data(client);if(d!=null){
            d.clearPendingConversations();
            d.commands.add(new Command(client.getSlot(),d.sessionId,"",-1,"INVALIDATE",0,0,0,-1));
        }
    }
    private CompanionSystem(){}
}
