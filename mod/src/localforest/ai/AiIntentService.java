package localforest.ai;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import localforest.companions.CompanionSystem;

/** Offline by default. Real provider installation is explicit, with no network or credential discovery. */
public final class AiIntentService {
    private static volatile ModelProvider provider=ModelProvider.offline();
    private static final Set<String> ALLOWED=Set.of("FOLLOW","GUARD","HOME","WORK","CANCEL");
    private static final ConcurrentHashMap<String,Pending> active=new ConcurrentHashMap<>();
    private static final class Pending {
        final ModelProvider owner;
        final ModelProvider.Request binding;
        private CompletableFuture<ModelProvider.Proposal> future;
        private boolean cancelled;
        Pending(ModelProvider owner, ModelProvider.Request binding) { this.owner=owner; this.binding=binding; }
        synchronized void attach(CompletableFuture<ModelProvider.Proposal> next) {
            future=next;
            if(cancelled) next.cancel(true);
        }
        synchronized void cancel() {
            cancelled=true;
            if(future!=null) future.cancel(true);
            try { owner.cancel(binding.sessionId(),binding.companionId()); } catch(RuntimeException ignored) { }
        }
    }
    public static void install() { CompanionSystem.setTextHandler(AiIntentService::handle); }
    public static String status() { return provider.name(); }
    public static boolean isConfigured() { return provider.configured(); }
    public static synchronized void setProvider(ModelProvider next) {
        provider=java.util.Objects.requireNonNull(next);
        var old=java.util.List.copyOf(active.values()); active.clear();
        old.forEach(Pending::cancel);
    }
    private static void handle(CompanionSystem.TextRequest request, Consumer<String> reply) {
        ModelProvider current=provider;
        if(!current.configured()) { reply.accept("模型尚未连接；离线任务和共同经历记录可正常使用。"); return; }
        if(request.text()==null||request.text().isBlank()||request.text().length()>2000) { reply.accept("请把指令控制在 2000 字以内。"); return; }
        String key=request.sessionId()+":"+request.companionId();
        // Reservation prevents concurrent requests touching the same companion memory session.
        Pending pending=new Pending(current,new ModelProvider.Request(request.sessionId(),request.companionId(),request.revision(),request.text(),request.context()));
        synchronized(AiIntentService.class) {
            if(provider!=current) return;
            if(active.putIfAbsent(key,pending)!=null) { reply.accept("伙伴正在理解上一条请求，请稍候。"); return; }
        }
        try {
            CompletableFuture<ModelProvider.Proposal> future=java.util.Objects.requireNonNull(current.propose(pending.binding));
            pending.attach(future);
            future.orTimeout(20,TimeUnit.SECONDS).whenComplete((plan,error)-> {
                    if(!active.remove(key,pending)||provider!=current) return;
                    if(error!=null||plan==null) { reply.accept("模型暂时不可用，仍可使用任务按钮。"); return; }
                    if(plan.actions()==null||plan.actions().stream().anyMatch(java.util.Objects::isNull)) { reply.accept("模型返回的任务无效，未改变任何任务。"); return; }
                    if(plan.actions().size()>1) { reply.accept("这项计划包含多个动作，请拆成一项任务。"); return; }
                    if(plan.actions().isEmpty()) { reply.accept(plan.reply()==null?"伙伴没有提出新任务。":plan.reply()); return; }
                    ModelProvider.Action action=plan.actions().get(0);
                    if(action.name()==null||!ALLOWED.contains(action.name())||action.amount()<0||action.amount()>1000) { reply.accept("这项动作尚未开放，未改变任何任务。"); return; }
                    // The game-side adapter checks bound session + revision again on the server thread.
                    reply.accept(CompanionSystem.submitFromAi(request,action.name(),action.tileX(),action.tileY(),action.amount()));
                });
        } catch(Exception error) { if(active.remove(key,pending)&&provider==current) reply.accept("模型请求未能发出，离线任务仍然可用。"); pending.cancel(); }
    }
    private AiIntentService() {}
}
