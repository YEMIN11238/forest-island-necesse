import localforest.ai.MemoryRanking;
import localforest.ai.ModelProvider;
import java.util.List;

public final class MemoryRankingCheck {
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        check(MemoryRanking.rank(List.of(),0,3).isEmpty(),"empty memories");
        var tied=List.of(new MemoryRanking.Candidate<>("b","B",0,0,0),new MemoryRanking.Candidate<>("a","A",0,0,0));
        check(MemoryRanking.rank(tied,0,2).equals(List.of("A","B")),"uniform score ranges must be finite and deterministic");
        var facts=List.of(new MemoryRanking.Candidate<>("rescue","救援",1,9,10_000),new MemoryRanking.Candidate<>("idle","休息",0,0,0));
        check(MemoryRanking.rank(facts,10_000,1).equals(List.of("救援")),"verified important relevant fact wins");
        check(MemoryRanking.rank(facts,10_000,0).isEmpty(),"zero budget");
        boolean invalid=false; try { new MemoryRanking.Candidate<>("x","x",Double.NaN,1,0); } catch(IllegalArgumentException e){invalid=true;}
        check(invalid,"invalid score rejected");
        check(!ModelProvider.offline().configured(),"no hidden model activation");
        check(ModelProvider.offline().propose(new ModelProvider.Request("w","npc",1,"采集","真实事件")).get().actions().isEmpty(),"offline provider never fabricates actions");
        System.out.println("MemoryRankingCheck: 7 checks passed");
    }
}
