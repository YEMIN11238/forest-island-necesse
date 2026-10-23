package localforest.companions;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import localforest.ai.MemoryRanking;
import necesse.engine.GameEvents;
import necesse.engine.GameEventListener;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.registries.WorldDataRegistry;

/** Single-player bridge: clients enqueue intentions; only WorldData.tick changes the world. */
public final class CompanionSystem {
    public static final String WORLD_KEY="MosslumeCompanions";
    public record MemoryEvent(String id,long worldTime,String type,String text,String source) {}
    public record CompanionView(String id,String name,int mobId,String level,int tileX,int tileY,boolean downed,
        long revision,String action,String state,String message,int completedSteps,List<MemoryEvent> memories) {}
    public record View(String sessionId,List<CompanionView> companions) {}
    public record TextRequest(Client client,String sessionId,String companionId,long revision,String text,String context,long requestEpoch,int playerTileX,int playerTileY) {}
    public interface TextHandler { void handle(TextRequest request,Consumer<String> reply); }
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
    }
    static void register(Server server,CompanionWorldData data){
        // A new local session invalidates every outstanding request from the old one.
        active.forEach((oldServer,old)->{if(oldServer!=server)localforest.ai.AiIntentService.cancelSession(old.sessionId);});
        active.entrySet().removeIf(e->e.getKey()!=server);active.put(server,data);
    }
    public static View snapshot(Client client){
        CompanionWorldData d=data(client);return d==null?new View("",List.of()):d.view;
    }
    private static CompanionWorldData data(Client c){return c==null||c.isDisconnected()||c.getLocalServer()==null?null:active.get(c.getLocalServer());}
    public static boolean isSessionCurrent(Client c,String session){CompanionWorldData d=data(c);return d!=null&&d.sessionId.equals(session);}
    public static boolean isRequestCurrent(TextRequest r){
        CompanionWorldData d=data(r.client());
        if(d==null||!d.sessionId.equals(r.sessionId())||!d.requests.isCurrent(r.companionId(),r.requestEpoch()))return false;
        return d.view.companions().stream().anyMatch(p->p.id().equals(r.companionId())&&p.revision()==r.revision());
    }
    public static String submit(Client c,String companion,String action,int x,int y,int amount){
        CompanionWorldData d=data(c);if(d==null)return "伙伴系统正在载入";
        CompanionView v=d.view.companions().stream().filter(p->p.id().equals(companion)).findFirst().orElse(null);
        if(v==null)return "找不到这个伙伴";
        // Invalidate on the calling thread: a paused server may not drain its queue yet.
        d.requests.invalidate(companion);
        localforest.ai.AiIntentService.cancelRequest(d.sessionId,companion);
        d.commands.add(new Command(c.getSlot(),d.sessionId,companion,-1,action,x,y,amount,-1));return "指令已提交，等待伙伴确认";
    }
    public static String submitFromAi(TextRequest r,String action,int x,int y,int amount){
        return submitFromAi(r,action,x,y,amount,null);
    }
    public static String submitFromAi(TextRequest r,String action,int x,int y,int amount,Consumer<String> receipt){
        CompanionWorldData d=data(r.client());if(d==null||!isRequestCurrent(r))return "请求已经失效，请重新发起请求";
        // Deliver the pending receipt before the server can increment the revision
        // for this same action. Later validation/blocked/completed status stays native.
        if(receipt!=null)receipt.accept("意图已提交校验");
        d.commands.add(new Command(r.client().getSlot(),r.sessionId(),r.companionId(),r.revision(),action,x,y,amount,r.requestEpoch()));return "意图已提交校验";
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
    public static void submitText(Client client,String companion,String text,Consumer<String> reply){
        TextHandler handler=textHandler;if(handler==null){reply.accept("模型尚未连接，离线任务按钮可正常使用");return;}
        View view=snapshot(client);CompanionView v=view.companions().stream().filter(p->p.id().equals(companion)).findFirst().orElse(null);
        if(v==null){reply.accept("伙伴尚未载入");return;}
        if(text==null||text.isBlank()||text.length()>2000){reply.accept("请输入 1～2000 字的请求");return;}
        if(client.getPlayer()==null||client.getPlayer().getLevel()==null){reply.accept("玩家尚未载入");return;}
        StringBuilder context=new StringBuilder("伙伴：").append(v.name()).append("；状态：").append(v.state()).append("；当前任务：").append(v.message());
        int x=client.getPlayer().getTileX(),y=client.getPlayer().getTileY();
        context.append("\n玩家当前格 tileX=").append(x).append(" tileY=").append(y)
            .append("；伙伴当前格 tileX=").append(v.tileX()).append(" tileY=").append(v.tileY())
            .append("；伙伴是否重伤=").append(v.downed()).append("；地图尺寸=")
            .append(client.getPlayer().getLevel().tileWidth).append("x").append(client.getPlayer().getLevel().tileHeight);
        recall(client,companion,text,8).forEach(e->context.append("\n").append(e.id()).append(": ").append(e.text()));
        CompanionWorldData d=data(client);if(d==null||!d.sessionId.equals(view.sessionId()))return;
        localforest.ai.AiIntentService.cancelRequest(view.sessionId(),companion);
        TextRequest request=new TextRequest(client,view.sessionId(),v.id(),v.revision(),text,context.toString(),d.requests.issue(v.id()),x,y);
        // This wraps action acknowledgements, ordinary conversation and error replies alike.
        handler.handle(request,message->{if(isRequestCurrent(request))reply.accept(message);});
    }
    public static void invalidateModelRequests(Client client){
        CompanionWorldData d=data(client);if(d!=null){
            d.requests.invalidateAll();
            localforest.ai.AiIntentService.cancelSession(d.sessionId);
            d.commands.add(new Command(client.getSlot(),d.sessionId,"",-1,"INVALIDATE",0,0,0,-1));
        }
    }
    private CompanionSystem(){}
}
