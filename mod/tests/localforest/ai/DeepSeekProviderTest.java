package localforest.ai;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Offline HTTP fixture only. Never reads the real configuration or calls a paid endpoint. */
public final class DeepSeekProviderTest {
    private static final String FOLLOW="{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"content\":null,\"tool_calls\":[{\"function\":{\"name\":\"request_action\",\"arguments\":\"{\\\"action\\\":\\\"FOLLOW\\\",\\\"tileX\\\":94,\\\"tileY\\\":82,\\\"say\\\":\\\"我跟着你。\\\"}\"}}]}}],\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50}}";
    public static void main(String[] ignored) throws Exception {
        var request=new ModelProvider.Request("s","p",1,"跟着我","伙伴：小禾；玩家当前格 tileX=94 tileY=82");
        JsonObject body=DeepSeekProvider.requestBody(request,600);
        check(AgentScopeDeepSeekRuntime.VERSION.equals("2.0.4"),"Unpinned AgentScope runtime");
        check(AgentScopeDeepSeekRuntime.messages(request).get(0) instanceof io.agentscope.core.message.Msg,"SDK messages not used");
        check(AgentScopeDeepSeekRuntime.tools(true).size()==1&&AgentScopeDeepSeekRuntime.tools(false).isEmpty(),"SDK tool boundary invalid");
        var execution=AgentScopeDeepSeekRuntime.options(600,true).getExecutionConfig();
        check(execution.getMaxAttempts()==1&&execution.getTimeout().toSeconds()<20,"SDK retry or timeout exceeded game policy");
        check(body.get("thinking").asObject().getString("type","").equals("disabled"),"Thinking must be off");
        check(body.getInt("max_tokens",0)==600&&!body.toString().contains("Bearer"),"Body leaked auth or lost output limit");
        var visitor=new ModelProvider.Request("s","merchant",1,"还记得我怎么称呼吗？","角色：阿穆；职业：旅行商人；未招募",false,
            List.of(new ModelProvider.ConversationTurn("请叫我阿森。","好，阿森。")));
        JsonObject visitorBody=DeepSeekProvider.requestBody(visitor,600);
        check(visitorBody.get("tools")==null&&visitorBody.get("tool_choice")==null,"Visitor received camp action tools");
        var messages=visitorBody.get("messages").asArray();
        check(messages.size()==5&&messages.get(2).asObject().getString("role","").equals("user")&&messages.get(2).asObject().getString("content","").equals("请叫我阿森。")
            &&messages.get(3).asObject().getString("role","").equals("assistant")&&messages.get(3).asObject().getString("content","").equals("好，阿森。"),"Paired conversation was not sent as distinct speaker turns");
        check(body.get("tools")!=null,"Recruited companion lost native action tools");
        verifyTypedConversationGate();
        var follow=DeepSeekProvider.parseProposal(Json.parse(FOLLOW).asObject());
        check(follow.actions().equals(List.of(new ModelProvider.Action("FOLLOW",94,82,1))),"Invalid action mapping");
        var chat=DeepSeekProvider.parseProposal(Json.parse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"我是小禾。\"}}]}").asObject());
        check(chat.actions().isEmpty(),"Conversation became an action");
        rejects(FOLLOW.replace("FOLLOW","DELETE_SAVE"));
        check(DeepSeekProvider.parseProposal(Json.parse(FOLLOW.replace("tileX\\\":94","tileX\\\":-1")).asObject()).actions().get(0).tileX()==-1,"Signed native world coordinate was rejected");
        rejects(FOLLOW.replace("tileX\\\":94","tileX\\\":67108864"));
        rejects(FOLLOW.replace("tool_calls\",\"message", "length\",\"message"));
        rejects(FOLLOW.replace("request_action","execute_code"));
        var ledgerDir=Files.createTempDirectory("mosslume-ledger-test-");
        var settings=new DeepSeekConfig(ledgerDir,"sk-fixture",100_000,2,600);
        var ledger=new AiUsageLedger(settings);var reservation=ledger.reserve(1000);ledger.settle(reservation,100,50);
        check(ledger.latest().micros()==600&&ledger.latest().requests()==1,"Usage settlement incorrect");
        new AiUsageLedger(settings).reserve(1000);
        try{new AiUsageLedger(settings).reserve(1000);throw new AssertionError("Restart bypassed daily request cap");}catch(java.io.IOException expected){}
        var tiny=new AiUsageLedger(new DeepSeekConfig(Files.createTempDirectory("mosslume-budget-test-"),"sk-fixture",10,2,600));
        try{tiny.reserve(1000);throw new AssertionError("Budget reservation was not enforced");}catch(java.io.IOException expected){}
        var classifiedConfig=new DeepSeekConfig(Files.createTempDirectory("mosslume-categories-test-"),"sk-fixture",5_000_000,12,600);
        var classified=new AiUsageLedger(classifiedConfig);
        for(int i=0;i<4;i++)classified.reserve(1000,true);
        try{new AiUsageLedger(classifiedConfig).reserve(1000,true);throw new AssertionError("Restart bypassed aggregate background budget");}catch(java.io.IOException expected){}
        check(new AiUsageLedger(classifiedConfig).reserve(1000,false)!=null,"Background allowance consumed reserved player dialogue capacity");

        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var workers=Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});server.setExecutor(workers);
        AtomicInteger mode=new AtomicInteger(),requestsSeen=new AtomicInteger();CountDownLatch slowStarted=new CountDownLatch(1),release=new CountDownLatch(1);
        server.createContext("/chat/completions",exchange->{
            String sent=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            JsonObject sdkSent=Json.parse(sent).asObject();
            if(!sdkSent.getString("model","").equals("deepseek-flash")||sdkSent.getBoolean("stream",true)
                ||!sdkSent.get("thinking").asObject().getString("type","").equals("disabled"))
                throw new AssertionError("Actual SDK request lost model generation policy");
            if(sdkSent.get("tools")!=null&&(sdkSent.get("tools").asArray().size()!=1
                ||!sdkSent.get("tools").asArray().get(0).asObject().get("function").asObject().getString("name","").equals("request_action")))
                throw new AssertionError("SDK exposed a non-game tool");
            if(!"Bearer sk-fixture".equals(exchange.getRequestHeaders().getFirst("Authorization"))||sent.contains("sk-fixture"))throw new AssertionError("Auth handling invalid");
            requestsSeen.incrementAndGet();int state=mode.get();
            if((state==4||state==5)&&sdkSent.get("tools")!=null)throw new AssertionError("Visitor received SDK game tools");
            if(state==1){slowStarted.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException stop){Thread.currentThread().interrupt();}}
            String dialogue="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"我记得你叫阿森。\"}}],\"usage\":{\"prompt_tokens\":100,\"completion_tokens\":50}}";
            byte[] answer=(state==4?dialogue:state==3?"x".repeat(70000):state==2?"untrusted raw error text":FOLLOW).getBytes(StandardCharsets.UTF_8);
            try{exchange.sendResponseHeaders(state==2?401:200,answer.length);exchange.getResponseBody().write(answer);}catch(java.io.IOException cancelled){}finally{exchange.close();}
        });server.start();
        try {
            var provider=new DeepSeekProvider(new DeepSeekConfig(Files.createTempDirectory("mosslume-http-test-"),"sk-fixture",5_000_000,20,600),
                URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions"));
            check(provider.propose(request).get(5,TimeUnit.SECONDS).actions().get(0).name().equals("FOLLOW"),"HTTP result not mapped");
            check(provider.completedCalls()==1&&provider.inputTokens()==100&&provider.outputTokens()==50,"Usage not counted");
            mode.set(1);var pending=provider.propose(new ModelProvider.Request("s","cancel",1,"跟着我","伙伴：小禾"));
            check(slowStarted.await(5,TimeUnit.SECONDS),"Slow request did not reach fixture");provider.cancel("s","cancel");
            check(pending.isCancelled(),"Cancellation did not cancel actual result");release.countDown();
            mode.set(2);int beforeError=requestsSeen.get();
            try{provider.propose(request).get(5,TimeUnit.SECONDS);throw new AssertionError("401 accepted");}
            catch(java.util.concurrent.ExecutionException error){check(error.getCause().getMessage().contains("Key")&&!error.getCause().getMessage().contains("untrusted"),"Raw server error leaked");}
            check(requestsSeen.get()==beforeError+1,"SDK retried a failed paid request");
            mode.set(4);
            var remembered=provider.propose(visitor).get(5,TimeUnit.SECONDS);
            check(remembered.reply().equals("我记得你叫阿森。")&&remembered.actions().isEmpty(),"SDK chat-only response mapping failed");
            mode.set(5);
            try{provider.propose(visitor).get(5,TimeUnit.SECONDS);throw new AssertionError("Visitor model output bypassed tools permission");}
            catch(java.util.concurrent.ExecutionException expected){}
            mode.set(3);
            try{provider.propose(request).get(5,TimeUnit.SECONDS);throw new AssertionError("Unbounded response accepted");}catch(java.util.concurrent.ExecutionException expected){}
        }finally{release.countDown();server.stop(0);workers.shutdownNow();}
        System.out.println("DeepSeekProviderTest PASS: actual AgentScope Java 2.0.4 model/message/tool + guarded HTTP path, JSON/action validation, chat-only, visitor tool and intent permission, paired multi-turn context, typed dialogue/status, budget/restart, HTTP mapping, usage, cancellation, sanitized errors, response-size bound; no paid calls");
    }
    private static void verifyTypedConversationGate() throws Exception {
        var method=AiIntentService.class.getDeclaredMethod("handle",localforest.companions.CompanionSystem.TextRequest.class,java.util.function.Consumer.class);method.setAccessible(true);
        var request=new localforest.companions.CompanionSystem.TextRequest(null,"s","merchant",1,"跟着我","未招募旅行商人",1,12,14,false,List.of(),"test-level");
        var result=new java.util.concurrent.atomic.AtomicReference<localforest.companions.CompanionSystem.ConversationReply>();
        java.util.function.Consumer<localforest.companions.CompanionSystem.ConversationReply> receiver=result::set;
        AiIntentService.setProvider(ModelProvider.offline());method.invoke(null,request,receiver);
        check(result.get()!=null&&result.get().dialogue().isEmpty()&&!result.get().status().isEmpty(),"Offline status leaked as character speech");
        AiIntentService.setProvider(new ModelProvider(){
            public String name(){return "fixture";}public boolean configured(){return true;}
            public java.util.concurrent.CompletableFuture<Proposal> propose(Request r){return java.util.concurrent.CompletableFuture.completedFuture(new Proposal("我跟着你。",List.of(new Action("FOLLOW",12,14,1))));}
        });
        result.set(null);method.invoke(null,request,receiver);
        check(result.get()!=null&&result.get().dialogue().isEmpty()&&result.get().status().contains("尚未加入"),"Visitor action escaped server-side intent gate");
        AiIntentService.setProvider(new ModelProvider(){
            public String name(){return "fixture";}public boolean configured(){return true;}
            public java.util.concurrent.CompletableFuture<Proposal> propose(Request r){return java.util.concurrent.CompletableFuture.completedFuture(new Proposal("你好，阿森。",List.of()));}
        });
        result.set(null);method.invoke(null,request,receiver);
        check(result.get()!=null&&result.get().dialogue().equals("你好，阿森。")&&result.get().status().isEmpty(),"Successful conversation was mistaken for system status");
        AiIntentService.setProvider(ModelProvider.offline());
    }
    private static void rejects(String response){try{DeepSeekProvider.parseProposal(Json.parse(response).asObject());throw new AssertionError("Invalid proposal accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
