package localforest.companions;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/** Session-local request tickets, invalidated immediately even while simulation is paused. */
final class RequestEpochs {
    private long clock;
    private final Map<String,Long> current=new HashMap<>();
    private final Set<Long> acceptedDialogue=new HashSet<>();
    synchronized long issue(String companion){long next=++clock;current.put(companion,next);return next;}
    synchronized void invalidate(String companion){current.remove(companion);}
    synchronized void invalidateAll(){current.clear();acceptedDialogue.clear();}
    synchronized boolean acceptDialogue(String companion,long ticket){
        return isCurrent(companion,ticket)&&acceptedDialogue.add(ticket);
    }
    synchronized boolean consumeDialogue(long ticket){return acceptedDialogue.remove(ticket);}
    synchronized boolean isCurrent(String companion,long ticket){return ticket>=0&&Long.valueOf(ticket).equals(current.get(companion));}
}
