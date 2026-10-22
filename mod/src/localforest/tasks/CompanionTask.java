package localforest.tasks;

import java.util.UUID;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

/** A persisted intent, never a second inventory or a serialized native job. */
public final class CompanionTask {
    public enum State { QUEUED, RUNNING, BLOCKED, PAUSED, SUCCEEDED, CANCELLED, FAILED }
    public final String id;
    public final String action;
    public final int tileX, tileY, amount;
    public final long createdAt;
    public State state;
    public String message;
    public int completedSteps;
    public long changedAt;

    public CompanionTask(String action, int x, int y, int amount, long now) {
        this(UUID.randomUUID().toString(), action, x, y, amount, now, State.QUEUED, "等待执行", 0, now);
    }
    private CompanionTask(String id,String action,int x,int y,int amount,long created,State state,String message,int steps,long changed) {
        this.id=id;this.action=action;tileX=x;tileY=y;this.amount=amount;createdAt=created;
        this.state=state;this.message=message;completedSteps=steps;changedAt=changed;
    }
    public boolean terminal(){return state==State.SUCCEEDED||state==State.CANCELLED||state==State.FAILED;}
    public void transition(State next,String why,long now){state=next;message=why;changedAt=now;}
    public SaveData save(){
        SaveData s=new SaveData("task");s.addSafeString("id",id);s.addSafeString("action",action);
        s.addInt("x",tileX);s.addInt("y",tileY);s.addInt("amount",amount);s.addLong("created",createdAt);
        s.addSafeString("state",state.name());s.addSafeString("message",message);s.addInt("steps",completedSteps);s.addLong("changed",changedAt);return s;
    }
    public static CompanionTask load(LoadData s){
        State state=State.valueOf(s.getSafeString("state","PAUSED"));
        CompanionTask t=new CompanionTask(s.getSafeString("id"),s.getSafeString("action"),s.getInt("x",0),s.getInt("y",0),s.getInt("amount",1),s.getLong("created",0),state,s.getSafeString("message",""),s.getInt("steps",0),s.getLong("changed",0));
        // Native sequences and reservations are deliberately not replayed after load.
        if(!t.terminal())t.transition(State.PAUSED,"存档已恢复；点击继续以重新校验任务",t.changedAt);
        return t;
    }
}
