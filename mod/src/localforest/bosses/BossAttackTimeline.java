package localforest.bosses;

/** Pure timing shared by the native server hit window and the client sprite pose. */
public final class BossAttackTimeline {
    public enum Phase { READY, WINDUP, STRIKE, RECOVERY }
    private long started;
    private int windup,active,recovery;
    private boolean running,impacted;
    public void begin(long now,int windup,int active,int recovery){
        if(windup<250||active<50||recovery<100)throw new IllegalArgumentException("Boss attacks require visible, positive windows");
        started=now;this.windup=windup;this.active=active;this.recovery=recovery;running=true;impacted=false;
    }
    public void clear(){running=false;impacted=false;}
    public boolean running(){return running;}
    public long elapsed(long now){return Math.max(0,now-started);}
    public int total(){return windup+active+recovery;}
    public int windup(){return windup;}
    public int active(){return active;}
    public Phase phase(long now){if(!running)return Phase.READY;long t=elapsed(now);return t<windup?Phase.WINDUP:t<windup+active?Phase.STRIKE:t<total()?Phase.RECOVERY:Phase.READY;}
    public boolean impact(long now){return impact(now,0);}
    public boolean impact(long now,int sourceDelayMillis){
        if(!running||impacted)return false;
        if(sourceDelayMillis<0||sourceDelayMillis>=active)throw new IllegalArgumentException("Source damage must be inside native strike window");
        long age=elapsed(now);if(age<windup+sourceDelayMillis)return false;
        impacted=true;return age<windup+active;
    }
    public boolean complete(long now){return running&&elapsed(now)>=total();}
    public float telegraphProgress(long now){return Math.min(1,elapsed(now)/(float)Math.max(1,windup));}
    public int spriteColumn(long now){return switch(phase(now)){case WINDUP->3;case STRIKE->4;case RECOVERY->5;case READY->0;};}
}
