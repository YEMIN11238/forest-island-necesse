package localforest.ai;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Replaceable provider boundary; a provider proposes actions, never receives mutable game objects. */
public interface ModelProvider {
    enum RequestKind { PLAYER, AMBIENT }
    record ConversationTurn(String playerText,String replyText) {}
    record Request(String sessionId, String companionId, long revision, String text, String observedContext,
        boolean commandsAllowed,List<ConversationTurn> recentConversation,RequestKind kind) {
        public Request { recentConversation=List.copyOf(recentConversation);kind=kind==null?RequestKind.PLAYER:kind; }
        public Request(String sessionId,String companionId,long revision,String text,String context,boolean allowed,List<ConversationTurn> turns){
            this(sessionId,companionId,revision,text,context,allowed,turns,RequestKind.PLAYER);
        }
        /** Existing task-only callers retain their recruited-companion behavior. */
        public Request(String sessionId,String companionId,long revision,String text,String observedContext) {
            this(sessionId,companionId,revision,text,observedContext,true,List.of());
        }
    }
    record Action(String name, int tileX, int tileY, int amount) {}
    record Proposal(String reply, List<Action> actions) {
        public Proposal { actions=List.copyOf(actions); }
    }
    String name();
    boolean configured();
    CompletableFuture<Proposal> propose(Request request);
    default void cancel(String sessionId, String companionId) {}

    static ModelProvider offline() {
        return new ModelProvider() {
            public String name() { return "尚未连接模型"; }
            public boolean configured() { return false; }
            public CompletableFuture<Proposal> propose(Request request) {
                return CompletableFuture.completedFuture(new Proposal("模型稍后配置。现在可以通过任务按钮安排伙伴工作。",List.of()));
            }
        };
    }
}
