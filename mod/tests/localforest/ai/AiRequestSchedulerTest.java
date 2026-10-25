package localforest.ai;

/** Offline only: aggregate pacing, foreground reservation, cancellation and day transitions. */
public final class AiRequestSchedulerTest {
    private static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    public static void main(String[] args){
        var scheduler=new AiRequestScheduler();
        var one=scheduler.tryAcquire("s:one",AiRequestScheduler.Priority.AMBIENT,1_000,5);
        check(one!=null,"First visible social line was blocked");
        check(scheduler.tryAcquire("s:two",AiRequestScheduler.Priority.AMBIENT,1_100,5)==null,"Population multiplied background concurrency");
        var player=scheduler.tryAcquire("s:player",AiRequestScheduler.Priority.PLAYER,1_200,5);
        check(player!=null,"Background speech consumed the player's reserved channel");
        check(scheduler.tryAcquire("s:third",AiRequestScheduler.Priority.PLAYER,1_300,5)==null,"Global concurrent requests exceeded two");
        one.close();one.close();
        check(scheduler.activeCount()==1,"Cancellation released another caller's reservation");
        check(scheduler.tryAcquire("s:two",AiRequestScheduler.Priority.AMBIENT,1_400,5)==null,"Background started during player dialogue");
        player.close();
        check(scheduler.tryAcquire("s:one",AiRequestScheduler.Priority.AMBIENT,2_000,5)==null,"Same NPC bypassed its social cooldown");
        var two=scheduler.tryAcquire("s:two",AiRequestScheduler.Priority.AMBIENT,2_100,5);check(two!=null,"Second actor could not use remaining minute allowance");two.close();
        check(scheduler.tryAcquire("s:three",AiRequestScheduler.Priority.AMBIENT,2_200,5)==null,"A third NPC bypassed the shared minute cap");
        var next=scheduler.tryAcquire("s:one",AiRequestScheduler.Priority.AMBIENT,91_001,5);check(next!=null,"Elapsed NPC cooldown did not recover");next.close();
        var budget=new AiRequestScheduler();
        for(int i=0;i<40;i++){
            var permit=budget.tryAcquire("s:npc"+i,AiRequestScheduler.Priority.AMBIENT,i*61_000L,7);
            check(permit!=null,"Background budget prematurely exhausted at "+i);permit.close();
        }
        check(budget.tryAcquire("s:extra",AiRequestScheduler.Priority.AMBIENT,3_000_000,7)==null,"40/day background cap was per person, not global");
        var foreground=budget.tryAcquire("s:player",AiRequestScheduler.Priority.PLAYER,3_000_100,7);
        check(foreground!=null,"Background budget blocked a player request before the provider's overall ledger");foreground.close();
        var tomorrow=budget.tryAcquire("s:extra",AiRequestScheduler.Priority.AMBIENT,3_060_001,8);check(tomorrow!=null,"Next day did not reset background allowance");tomorrow.close();
        System.out.println("AiRequestSchedulerTest PASS: aggregate 2-request cap, reserved player channel, one background, actor cooldown, shared minute/day allowance, idempotent release and day rollover; no network");
    }
}
