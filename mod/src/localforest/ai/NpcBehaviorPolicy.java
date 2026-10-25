package localforest.ai;

import java.util.Locale;

/** Deterministic personality-to-behavior policy. The server must still validate all native actions. */
public final class NpcBehaviorPolicy {
    /** Stable traits plus learned trust, supplied by the persisted character record; all values are 0..1. */
    public record Traits(double sociability,double caution,double warmth,double trust) {
        public Traits { sociability=unit(sociability);caution=unit(caution);warmth=unit(warmth);trust=unit(trust); }
        public static Traits neutral() { return new Traits(.55,.5,.55,.5); }
    }
    public record Situation(double healthFraction,boolean injured,boolean inCombat,boolean night,
        boolean hostilesNearby,boolean dangerousDestination,boolean commandsAllowed) {
        public Situation { healthFraction=unit(healthFraction); }
    }
    public enum Choice { ALLOW, DECLINE, REQUEST_RESCUE }
    public record Decision(Choice choice,String reason,String fallbackText) {
        public boolean allowed() { return choice==Choice.ALLOW; }
    }

    /** Advice only: this never mutates a mob or silently grants command permission. */
    public static Decision evaluate(String proposedAction,Traits traits,Situation situation) {
        String action=proposedAction==null?"":proposedAction.toUpperCase(Locale.ROOT);
        Traits t=traits==null?Traits.neutral():traits;
        if(!situation.commandsAllowed())return new Decision(Choice.DECLINE,"not_recruited","我还没有加入营地，暂时不能接受这项安排。");
        // Home/cancel must remain available even when the character refuses a dangerous request.
        if(action.equals("CANCEL")||action.equals("HOME"))return new Decision(Choice.ALLOW,"safe_control","");
        if(situation.injured()||situation.healthFraction()<.2)
            return new Decision(Choice.REQUEST_RESCUE,"seriously_hurt","我伤得很重，先来帮帮我好吗？");
        boolean hazardous=situation.dangerousDestination()||situation.hostilesNearby();
        boolean movingOrExposed=action.equals("FOLLOW")||action.equals("EXPLORE")||action.equals("HARVEST")
            ||action.equals("GUARD")||action.equals("RESCUE")||action.equals("WORK")||action.equals("HAUL");
        if(!movingOrExposed)return new Decision(Choice.ALLOW,"ordinary_task","");
        double risk=(hazardous?.55:0)+(situation.night()&&hazardous?.15:0)
            +(1-situation.healthFraction())*.55+(situation.inCombat()?.15:0);
        // A trusting/warm companion is more willing to help, but courage cannot erase severe injury.
        double tolerance=.85-t.caution()*.48+t.trust()*.17+(action.equals("RESCUE")?t.warmth()*.12:0);
        if(hazardous&&risk>tolerance)
            return new Decision(Choice.DECLINE,"risk_exceeds_tolerance",situation.healthFraction()<.5
                ?"我现在的伤势撑不住，先让我缓一缓，再一起去。":"那边风险有点大。先做好准备，我们再一起过去，好吗？");
        return new Decision(Choice.ALLOW,"acceptable_risk","");
    }

    /** Higher score means a stronger desire for a nearby, nonurgent social visit. */
    public static double socialInterest(Traits t,boolean night,double distanceTiles) {
        if(t==null)t=Traits.neutral();
        return t.sociability()*.62+t.warmth()*.22+t.trust()*.08
            -(night?t.caution()*.16:0)-Math.min(12,Math.max(0,distanceTiles))*.012;
    }
    public static String topic(Traits t,boolean night) {
        if(t==null)t=Traits.neutral();
        if(night&&t.caution()>=.55)return "stay_together";
        if(t.warmth()>=.65)return "check_on_friend";
        if(t.sociability()>=.65)return "camp_company";
        return "quiet_rest";
    }
    private static double unit(double value) { return Double.isFinite(value)?Math.max(0,Math.min(1,value)):.5; }
    private NpcBehaviorPolicy() {}
}
