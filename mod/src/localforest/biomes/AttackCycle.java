package localforest.biomes;

/** A server-time attack with one impact; render frame count cannot multiply damage. */
public final class AttackCycle {
    public enum Phase { IDLE,WINDUP,ACTIVE,RECOVERY }
    private long began;
    private int duration;
    private boolean running,consumed;
    public boolean begin(long now,int durationMillis){
        if(running)return false;
        began=now;duration=Math.max(120,durationMillis);running=true;consumed=false;return true;
    }
    public float progress(long now){return !running?1:Math.max(0,Math.min(1,(now-began)/(float)duration));}
    public Phase phase(long now){
        if(!running)return Phase.IDLE;
        float p=progress(now);return p<.43f?Phase.WINDUP:p<.62f?Phase.ACTIVE:Phase.RECOVERY;
    }
    public boolean impact(long now){
        if(!running||consumed||progress(now)<.43f)return false;
        consumed=true;
        // A stalled server must not apply a surprise hit after the visible strike ended.
        return progress(now)<.62f;
    }
    public boolean complete(long now){return running&&progress(now)>=1;}
    public boolean running(){return running;}
    public void cancel(){running=false;consumed=true;}
}
