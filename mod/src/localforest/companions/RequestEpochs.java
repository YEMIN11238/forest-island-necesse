package localforest.companions;

import java.util.HashMap;
import java.util.Map;

/** Session-local request tickets, invalidated immediately even while simulation is paused. */
final class RequestEpochs {
    private long clock;
    private final Map<String,Long> current=new HashMap<>();
    synchronized long issue(String companion){long next=++clock;current.put(companion,next);return next;}
    synchronized void invalidate(String companion){current.remove(companion);}
    synchronized void invalidateAll(){current.clear();}
    synchronized boolean isCurrent(String companion,long ticket){return ticket>=0&&Long.valueOf(ticket).equals(current.get(companion));}
}
