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
        check(body.get("thinking").asObject().getString("type","").equals("disabled"),"Thinking must be off");
        check(body.getInt("max_tokens",0)==600&&!body.toString().contains("Bearer"),"Body leaked auth or lost output limit");
        var follow=DeepSeekProvider.parseProposal(Json.parse(FOLLOW).asObject());
        check(follow.actions().equals(List.of(new ModelProvider.Action("FOLLOW",94,82,1))),"Invalid action mapping");
        var chat=DeepSeekProvider.parseProposal(Json.parse("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"我是小禾。\"}}]}").asObject());
        check(chat.actions().isEmpty(),"Conversation became an action");
        rejects(FOLLOW.replace("FOLLOW","DELETE_SAVE"));rejects(FOLLOW.replace("tileX\\\":94","tileX\\\":-1"));
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

        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var workers=Executors.newCachedThreadPool(r->{Thread t=new Thread(r);t.setDaemon(true);return t;});server.setExecutor(workers);
        AtomicInteger mode=new AtomicInteger();CountDownLatch slowStarted=new CountDownLatch(1),release=new CountDownLatch(1);
        server.createContext("/chat/completions",exchange->{
            String sent=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            if(!"Bearer sk-fixture".equals(exchange.getRequestHeaders().getFirst("Authorization"))||sent.contains("sk-fixture"))throw new AssertionError("Auth handling invalid");
            int state=mode.get();if(state==1){slowStarted.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException stop){Thread.currentThread().interrupt();}}
            byte[] answer=(state==3?"x".repeat(70000):state==2?"untrusted raw error text":FOLLOW).getBytes(StandardCharsets.UTF_8);
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
            mode.set(2);
            try{provider.propose(request).get(5,TimeUnit.SECONDS);throw new AssertionError("401 accepted");}
            catch(java.util.concurrent.ExecutionException error){check(error.getCause().getMessage().contains("Key")&&!error.getCause().getMessage().contains("untrusted"),"Raw server error leaked");}
            mode.set(3);
            try{provider.propose(request).get(5,TimeUnit.SECONDS);throw new AssertionError("Unbounded response accepted");}catch(java.util.concurrent.ExecutionException expected){}
        }finally{release.countDown();server.stop(0);workers.shutdownNow();}
        System.out.println("DeepSeekProviderTest PASS: JSON/action validation, chat-only, budget/restart, HTTP mapping, usage, cancellation, sanitized errors, response-size bound; no paid calls");
    }
    private static void rejects(String response){try{DeepSeekProvider.parseProposal(Json.parse(response).asObject());throw new AssertionError("Invalid proposal accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
