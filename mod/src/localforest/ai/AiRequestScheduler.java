package localforest.ai;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** One global cap, not a new quota for every NPC. No model loop or game-thread waits. */
public final class AiRequestScheduler {
    public enum Priority { PLAYER, AMBIENT }
    private final Map<String,Permit> active=new HashMap<>();
    private final LinkedHashMap<String,Long> lastAmbient=new LinkedHashMap<>();
    record Limits(int concurrent,int backgroundConcurrent,int backgroundPerMinute,int backgroundPerDay,long npcCooldownMillis) {}
    private final Limits limits;
    private long minute=Long.MIN_VALUE,day=Long.MIN_VALUE;
    private int minuteBackground,dailyBackground;
    public AiRequestScheduler(){this(new Limits(2,1,2,40,90_000));}
    AiRequestScheduler(Limits limits){this.limits=limits;}
    public final class Permit implements AutoCloseable {
        private final String key;private final Priority priority;private final AtomicBoolean closed=new AtomicBoolean();
        private Permit(String key,Priority priority){this.key=key;this.priority=priority;}
        @Override public void close(){if(closed.compareAndSet(false,true))synchronized(AiRequestScheduler.this){active.remove(key,this);}}
    }
    public synchronized Permit tryAcquire(String key,Priority priority,long monotonicMillis,long epochDay){
        if(active.containsKey(key)||active.size()>=limits.concurrent())return null;
        if(priority==Priority.AMBIENT){
            // Foreground dialogue reserves the channel; background chatter cannot queue behind it.
            if(active.values().stream().anyMatch(p->p.priority==Priority.PLAYER)
                ||active.values().stream().filter(p->p.priority==Priority.AMBIENT).count()>=limits.backgroundConcurrent())return null;
            long nextMinute=monotonicMillis/60_000;
            if(minute!=nextMinute){minute=nextMinute;minuteBackground=0;}
            if(day!=epochDay){day=epochDay;dailyBackground=0;}
            if(minuteBackground>=limits.backgroundPerMinute()||dailyBackground>=limits.backgroundPerDay()
                ||monotonicMillis-lastAmbient.getOrDefault(key,Long.MIN_VALUE/2)<limits.npcCooldownMillis())return null;
            minuteBackground++;dailyBackground++;lastAmbient.put(key,monotonicMillis);
            while(lastAmbient.size()>128)lastAmbient.remove(lastAmbient.keySet().iterator().next());
        }
        Permit permit=new Permit(key,priority);active.put(key,permit);return permit;
    }
    public synchronized int activeCount(){return active.size();}
    public synchronized int backgroundReservations(){return dailyBackground;}
}
