package localforest.tasks;

/** Standalone save-format checks; no running game, model or credentials required. */
public final class CompanionTaskSaveTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        CompanionTask live=new CompanionTask("HARVEST",12,34,1,12345);
        live.transition(CompanionTask.State.RUNNING,"采收中：玩家的“营地”",12350);live.completedSteps=2;
        CompanionTask restored=CompanionTask.load(live.save().toLoadData());
        check(restored.id.equals(live.id),"Task identity changed");
        check(restored.action.equals("HARVEST")&&restored.tileX==12&&restored.tileY==34,"Target changed");
        check(restored.state==CompanionTask.State.PAUSED,"Unfinished native job must not auto-replay");
        check(restored.completedSteps==2,"Actual progress lost");
        for(CompanionTask.State state:new CompanionTask.State[]{CompanionTask.State.SUCCEEDED,CompanionTask.State.CANCELLED,CompanionTask.State.FAILED}){
            live.transition(state,"已处理：中文/quote\"",12360);
            CompanionTask loaded=CompanionTask.load(live.save().toLoadData());
            check(loaded.state==state&&loaded.message.equals(live.message),"Terminal state or text changed");
        }
        System.out.println("PASS: stable task ID/target, safe resume gate, terminal states, progress, Unicode save round trip");
    }
}
