package localforest.ai;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Model doubles only: no credentials, HTTP, game startup, or paid generation. */
public final class AiIntentAmbientTest {
    private static final class TrackedFuture extends CompletableFuture<ModelProvider.Proposal>{
        final AtomicInteger cancelCalls=new AtomicInteger();final AtomicBoolean hookFired=new AtomicBoolean();volatile Runnable onCancel;
        @Override public boolean cancel(boolean interrupt){
            cancelCalls.incrementAndGet();boolean result=super.cancel(interrupt);Runnable hook=onCancel;
            if(hook!=null&&hookFired.compareAndSet(false,true))hook.run();return result;
        }
    }
    private static final class RecordingProvider implements ModelProvider {
        final CopyOnWriteArrayList<Request> requests=new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<TrackedFuture> futures=new CopyOnWriteArrayList<>();
        final AtomicInteger keyCancelCalls=new AtomicInteger();
        public String name(){return "offline-recording-fixture";}
        public boolean configured(){return true;}
        public CompletableFuture<Proposal> propose(Request request){TrackedFuture result=new TrackedFuture();requests.add(request);futures.add(result);return result;}
        // This intentionally models the dangerous provider API: cancel by key affects whichever
        // request is current now, including a replacement that started after the old one finished.
        public void cancel(String session,String companion){
            keyCancelCalls.incrementAndGet();
            for(int i=requests.size()-1;i>=0;i--){Request request=requests.get(i);
                if(request.sessionId().equals(session)&&request.companionId().equals(companion)){futures.get(i).cancel(true);break;}}
        }
    }
    public static void main(String[] ignored)throws Exception{
        // Cancellation races need immediate replacement and two distinct pending NPCs. Production
        // pacing is verified separately by AiRequestSchedulerTest, not bypassed in the shipped game.
        AiIntentService.installSchedulerForChecks(new AiRequestScheduler(new AiRequestScheduler.Limits(2,2,1000,1000,0)));
        try{toolFreeBindingAndValidation();errorCannotCancelReplacement(false);errorCannotCancelReplacement(true);providerSwapCancelsBoundPending();explicitCancelOnlyTouchesBoundFuture();playerPreemptsAmbient();}
        finally{AiIntentService.setProvider(ModelProvider.offline());AiIntentService.installSchedulerForChecks(new AiRequestScheduler());}
        System.out.println("AiIntentAmbientTest PASS: tool-free autonomous binding, action rejection, busy reservation, error/3s-timeout replacement isolation, provider-swap cancellation and bound-future cancellation; fixture models only, no credentials or paid calls");
    }
    private static void toolFreeBindingAndValidation()throws Exception{
        var provider=new RecordingProvider();AiIntentService.setProvider(provider);
        var speech=AiIntentService.ambient("fixture","a","观察：角色在附近休息。","对身旁的人说一句话。");
        var request=provider.requests.get(0);
        check(!request.commandsAllowed()&&request.recentConversation().isEmpty()&&request.revision()==0,"Ambient request received action permission or player chat history");
        check(request.kind()==ModelProvider.RequestKind.AMBIENT,"Background model work was billed as player dialogue");
        check(request.observedContext().equals("观察：角色在附近休息。")&&request.text().equals("对身旁的人说一句话。"),"Observed evidence/prompt binding was changed");
        rejects(AiIntentService.ambient("fixture","a","另一份观察","同时发言"));
        check(provider.requests.size()==1,"Same NPC received concurrent autonomous model calls");
        provider.futures.get(0).complete(new ModelProvider.Proposal("一起歇一会儿吧。",List.of()));
        check(speech.get(1,TimeUnit.SECONDS).equals("一起歇一会儿吧。"),"Valid autonomous line was not returned");
        var tool=AiIntentService.ambient("fixture","a","观察","乱用工具");
        provider.futures.get(1).complete(new ModelProvider.Proposal("我要跟随你。",List.of(new ModelProvider.Action("FOLLOW",0,0,1))));
        rejects(tool);
        check(provider.keyCancelCalls.get()==0,"Rejected ambient tool called key-based provider cancellation");
        var longLine=AiIntentService.ambient("fixture","a","观察","超长回应");
        provider.futures.get(2).complete(new ModelProvider.Proposal("字".repeat(201),List.of()));rejects(longLine);
    }
    private static void errorCannotCancelReplacement(boolean timeout)throws Exception{
        var provider=new RecordingProvider();AiIntentService.setProvider(provider);
        var oldResult=AiIntentService.ambient("replacement","npc","原观察","原发言");
        var old=provider.futures.get(0);var replacement=new AtomicReference<CompletableFuture<String>>();var created=new CountDownLatch(1);
        old.onCancel=()->{replacement.set(AiIntentService.ambient("replacement","npc","新观察","新发言"));created.countDown();};
        if(!timeout)old.completeExceptionally(new IOException("fixture failure"));
        rejects(oldResult);check(created.await(1,TimeUnit.SECONDS),"Bound future cancellation did not run replacement fixture");
        check(provider.requests.size()==2,"Replacement was blocked by stale active reservation");
        check(!provider.futures.get(1).isDone(),"Old error/timeout cancelled the newer same-key request");
        check(provider.keyCancelCalls.get()==0,"Old error/timeout called owner.cancel(session, npc)");
        provider.futures.get(1).complete(new ModelProvider.Proposal("这是新的一句话。",List.of()));
        check(replacement.get().get(1,TimeUnit.SECONDS).equals("这是新的一句话。"),"Replacement cannot finish after old request failure");
    }
    private static void providerSwapCancelsBoundPending()throws Exception{
        var oldProvider=new RecordingProvider();AiIntentService.setProvider(oldProvider);
        var oldResult=AiIntentService.ambient("swap","npc","旧观察","旧发言");
        var newProvider=new RecordingProvider();var replacement=new AtomicReference<CompletableFuture<String>>();
        oldProvider.futures.get(0).onCancel=()->replacement.set(AiIntentService.ambient("swap","npc","新观察","新发言"));
        AiIntentService.setProvider(newProvider);
        rejects(oldResult);
        check(oldProvider.futures.get(0).isCancelled(),"Provider swap left old model pending");
        check(newProvider.futures.size()==1&&!newProvider.futures.get(0).isDone(),"Provider swap cancelled or blocked its new request");
        check(oldProvider.keyCancelCalls.get()==0&&newProvider.keyCancelCalls.get()==0,"Provider swap used key-based cancellation");
        newProvider.futures.get(0).complete(new ModelProvider.Proposal("新模型的回应。",List.of()));
        check(replacement.get().get(1,TimeUnit.SECONDS).equals("新模型的回应。"),"New provider response was lost");
    }
    private static void explicitCancelOnlyTouchesBoundFuture()throws Exception{
        var provider=new RecordingProvider();AiIntentService.setProvider(provider);
        var first=AiIntentService.ambient("cancel","npc-a","观察","第一句");
        var second=AiIntentService.ambient("cancel","npc-b","观察","第二句");
        AiIntentService.cancelRequest("cancel","npc-a");rejects(first);
        check(provider.futures.get(0).isCancelled()&&!provider.futures.get(1).isDone(),"Per-NPC cancel touched another role");
        check(provider.keyCancelCalls.get()==0,"Explicit cancellation used dangerous key-based owner.cancel");
        provider.futures.get(1).complete(new ModelProvider.Proposal("另一位仍能说话。",List.of()));
        check(second.get(1,TimeUnit.SECONDS).equals("另一位仍能说话。"),"Other NPC response did not survive cancellation");
    }
    private static void playerPreemptsAmbient()throws Exception{
        var provider=new RecordingProvider();AiIntentService.setProvider(provider);
        var background=AiIntentService.ambient("priority","npc","观察","背景发言");
        var reply=new AtomicReference<localforest.companions.CompanionSystem.ConversationReply>();
        var request=new localforest.companions.CompanionSystem.TextRequest(null,"priority","npc",0,"你好","场景",1,0,0,false,List.of(),"surface");
        var handler=AiIntentService.class.getDeclaredMethod("handle",localforest.companions.CompanionSystem.TextRequest.class,java.util.function.Consumer.class);
        handler.setAccessible(true);handler.invoke(null,request,(java.util.function.Consumer<localforest.companions.CompanionSystem.ConversationReply>)reply::set);
        rejects(background);
        check(provider.futures.get(0).isCancelled()&&provider.requests.size()==2,"Player dialogue did not preempt a pending background line");
        check(provider.requests.get(1).kind()==ModelProvider.RequestKind.PLAYER,"Player dialogue inherited the background quota");
        provider.futures.get(1).complete(new ModelProvider.Proposal("你好，稍坐一会儿吧。",List.of()));
        check(reply.get()!=null&&reply.get().dialogue().contains("你好"),"Player reply was lost after background preemption");
    }
    private static void rejects(CompletableFuture<?> result)throws Exception{
        try{result.get(6,TimeUnit.SECONDS);throw new AssertionError("Expected failed/cancelled autonomous response");}
        catch(ExecutionException|CancellationException expected){}
    }
    private static void check(boolean good,String message){if(!good)throw new AssertionError(message);}
}
