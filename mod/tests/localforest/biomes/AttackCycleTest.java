package localforest.biomes;

public final class AttackCycleTest {
    public static void main(String[] args){
        AttackCycle attack=new AttackCycle();
        check(attack.begin(1000,420),"initial attack starts");
        check(!attack.begin(1010,420),"cannot restart an in-flight attack");
        check(attack.phase(1100)==AttackCycle.Phase.WINDUP&&!attack.impact(1100),"windup cannot hit");
        check(attack.impact(1200)&&!attack.impact(1200)&&!attack.impact(1300),"only one impact per attack");
        check(attack.phase(1300)==AttackCycle.Phase.RECOVERY,"recovery is readable");
        check(attack.complete(1420),"finishes at native duration");attack.cancel();
        check(attack.begin(1600,420),"new attack after release");attack.cancel();
        check(!attack.impact(2000),"cancelled windup can never hit later");
        check(attack.begin(2200,420)&&!attack.impact(2700),"a stalled server cannot deliver a late recovery hit");
        System.out.println("AttackCycleTest PASS: no windup damage; one impact; cancel; no late recovery damage");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
