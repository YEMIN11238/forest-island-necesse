package localforest.ai;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.ConversationReply;

/** Offline by default. Real provider installation is explicit, with no network or credential discovery. */
public final class AiIntentService {
    private static volatile ModelProvider provider=ModelProvider.offline();
    private static final Set<String> ALLOWED=DeepSeekProvider.ACTIONS;
    private static final ConcurrentHashMap<String,Pending> active=new ConcurrentHashMap<>();
    private static volatile AiRequestScheduler scheduler=new AiRequestScheduler();
    static synchronized void installSchedulerForChecks(AiRequestScheduler next){
        if(!active.isEmpty())throw new IllegalStateException("Cannot replace an active scheduler");
        scheduler=java.util.Objects.requireNonNull(next);
    }
    private static final class Pending {
        final ModelProvider owner;
        final ModelProvider.Request binding;
        final AiRequestScheduler.Priority priority;
        final AiRequestScheduler.Permit permit;
        private CompletableFuture<ModelProvider.Proposal> future;
        private boolean cancelled;
        Pending(ModelProvider owner,ModelProvider.Request binding,AiRequestScheduler.Priority priority,AiRequestScheduler.Permit permit) {
            this.owner=owner;this.binding=binding;this.priority=priority;this.permit=permit;
        }
        synchronized void attach(CompletableFuture<ModelProvider.Proposal> next) {
            future=next;
            if(cancelled) next.cancel(true);
        }
        synchronized void cancel() {
            cancelled=true;
            permit.close();
            if(future!=null) future.cancel(true);
        }
    }
    public static void install() {
        CompanionSystem.setTextHandler(AiIntentService::handle);
        boolean disposable=localforest.RefactorNativeChecks.enabled()||necesse.engine.GameLaunch.launchOptions!=null&&necesse.engine.GameLaunch.launchOptions.containsKey("mosslumeqa");
        if(!disposable||Boolean.getBoolean("mosslume.refactorProbe.ai"))DeepSeekProvider.installLocal();
    }
    public static String status() { return provider.name(); }
    public static boolean isConfigured() { return provider.configured(); }
    public static synchronized void setProvider(ModelProvider next) {
        provider=java.util.Objects.requireNonNull(next);
        var old=java.util.List.copyOf(active.values()); active.clear();
        old.forEach(Pending::cancel);
    }
    public static void cancelRequest(String session,String companion) {
        Pending pending=active.remove(session+":"+companion);if(pending!=null)pending.cancel();
    }
    public static void cancelSession(String session) {
        active.forEach((key,pending)->{if(key.startsWith(session+":" )&&active.remove(key,pending))pending.cancel();});
    }
    public static ModelProvider providerForChecks() { return provider; }
    /** Low-frequency, tool-free autonomous utterance. Scheduling and facts remain game-owned. */
    public static CompletableFuture<String> ambient(String session,String companion,String context,String prompt){
        ModelProvider current=provider;
        if(!current.configured())return CompletableFuture.failedFuture(new IllegalStateException("offline"));
        String key=session+":"+companion;
        AiRequestScheduler.Permit permit=scheduler.tryAcquire(key,AiRequestScheduler.Priority.AMBIENT,System.nanoTime()/1_000_000L,
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toEpochDay());
        if(permit==null)return CompletableFuture.failedFuture(new IllegalStateException("background budget busy"));
        Pending pending=new Pending(current,new ModelProvider.Request(session,companion,0,prompt,context,false,java.util.List.of(),ModelProvider.RequestKind.AMBIENT),AiRequestScheduler.Priority.AMBIENT,permit);
        synchronized(AiIntentService.class){
            if(provider!=current){permit.close();return CompletableFuture.failedFuture(new IllegalStateException("provider changed"));}
            if(active.putIfAbsent(key,pending)!=null){permit.close();return CompletableFuture.failedFuture(new IllegalStateException("busy"));}
        }
        try{
            CompletableFuture<ModelProvider.Proposal> future=current.propose(pending.binding);pending.attach(future);
            CompletableFuture<String> result=future.orTimeout(3,TimeUnit.SECONDS).thenApply(plan->{
                if(provider!=current||plan==null||!plan.actions().isEmpty()||plan.reply()==null||plan.reply().isBlank()||plan.reply().length()>200)
                    throw new IllegalStateException("invalid autonomous speech");
                return plan.reply();
            });
            result.whenComplete((line,error)->{permit.close();active.remove(key,pending);if(error!=null)pending.cancel();});return result;
        }catch(RuntimeException failed){active.remove(key,pending);pending.cancel();return CompletableFuture.failedFuture(failed);}
    }
    private static void handle(CompanionSystem.TextRequest request, Consumer<ConversationReply> reply) {
        ModelProvider current=provider;
        if(!current.configured()) { reply.accept(ConversationReply.status("模型尚未连接；离线任务和共同经历记录可正常使用。")); return; }
        if(request.text()==null||request.text().isBlank()||request.text().length()>2000) { reply.accept(ConversationReply.status("请把指令控制在 2000 字以内。")); return; }
        String key=request.sessionId()+":"+request.companionId();
        // Player dialogue preempts optional background speech, including the same NPC's pending line.
        active.forEach((otherKey,other)->{if(other.priority==AiRequestScheduler.Priority.AMBIENT&&active.remove(otherKey,other))other.cancel();});
        AiRequestScheduler.Permit permit=scheduler.tryAcquire(key,AiRequestScheduler.Priority.PLAYER,System.nanoTime()/1_000_000L,
            java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).toEpochDay());
        if(permit==null){reply.accept(ConversationReply.status("正在回应其他交谈，请稍候再说。"));return;}
        // Reservation prevents concurrent requests touching the same companion memory session.
        Pending pending=new Pending(current,new ModelProvider.Request(request.sessionId(),request.companionId(),request.revision(),request.text(),request.context(),request.commandsAllowed(),request.recentConversation().stream().map(t->new ModelProvider.ConversationTurn(t.playerText(),t.replyText())).toList()),AiRequestScheduler.Priority.PLAYER,permit);
        synchronized(AiIntentService.class) {
            if(provider!=current){permit.close();return;}
            if(active.putIfAbsent(key,pending)!=null) {permit.close();reply.accept(ConversationReply.status("角色正在回应上一句话，请稍候。"));return;}
        }
        try {
            CompletableFuture<ModelProvider.Proposal> future=java.util.Objects.requireNonNull(current.propose(pending.binding));
            pending.attach(future);
            future.orTimeout(20,TimeUnit.SECONDS).whenComplete((plan,error)-> {
                    permit.close();
                    if(!active.remove(key,pending)||provider!=current) return;
                    if(error!=null||plan==null) {
                        Throwable why=error;while(why instanceof java.util.concurrent.CompletionException&&why.getCause()!=null)why=why.getCause();
                        reply.accept(ConversationReply.status(current instanceof DeepSeekProvider&&why instanceof java.io.IOException?why.getMessage()+"；离线任务仍可用。":"模型暂时不可用，仍可使用任务按钮。"));return;
                    }
                    if(plan.actions()==null||plan.actions().stream().anyMatch(java.util.Objects::isNull)) { reply.accept(ConversationReply.status("模型返回的任务无效，未改变任何任务。")); return; }
                    if(plan.actions().size()>1) { reply.accept(ConversationReply.status("这项计划包含多个动作，请拆成一项任务。")); return; }
                    if(plan.reply()==null||plan.reply().isBlank()||plan.reply().length()>500) {reply.accept(ConversationReply.status("角色暂时没有给出有效回应，请再试一次。"));return;}
                    if(plan.actions().isEmpty()) { reply.accept(ConversationReply.dialogue(plan.reply())); return; }
                    if(!request.commandsAllowed()){reply.accept(ConversationReply.status("这位角色尚未加入营地，可以聊天，但不能安排营地任务。"));return;}
                    ModelProvider.Action action=plan.actions().get(0);
                    if(action.name()==null||!ALLOWED.contains(action.name())||action.amount()!=1) { reply.accept(ConversationReply.status("这项动作尚未开放，未改变任何任务。")); return; }
                    if(action.name().equals("GUARD")&&(action.tileX()!=request.playerTileX()||action.tileY()!=request.playerTileY())) {
                        reply.accept(ConversationReply.status("只能守卫本次指令提供的位置，请重新指定。"));return;
                    }
                    String objection=CompanionSystem.assessIntent(request,action.name());
                    if(!objection.isEmpty()){reply.accept(ConversationReply.dialogue(objection));return;}
                    // The game-side adapter checks bound session + revision again on the server thread.
                    CompanionSystem.submitFromAi(request,action.name(),action.tileX(),action.tileY(),action.amount(),receipt->
                        reply.accept(new ConversationReply(plan.reply(),receipt)));
                });
        } catch(Exception error) { if(active.remove(key,pending)&&provider==current) reply.accept(ConversationReply.status("模型请求未能发出，离线任务仍然可用。")); pending.cancel(); }
    }
    private AiIntentService() {}
}
