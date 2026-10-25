package localforest.visual;

import necesse.level.maps.Level;

/** Render time, independent of server ticks. Pause is explicit, not inferred from a stalled server. */
public final class VisualClock {
    private static long previous=System.nanoTime();
    private static double seconds;
    private static boolean paused;
    private static long frames;
    public static synchronized void frame(Level level) {
        frames++;
        long now=System.nanoTime();
        boolean next=level!=null&&level.getClient()!=null&&level.getClient().isPaused();
        if(!paused&&!next)seconds+=Math.max(0,Math.min(.25,(now-previous)/1_000_000_000.0));
        previous=now;paused=next;
    }
    public static synchronized double seconds(){return seconds;}
    public static synchronized long frameIndex(){return frames;}
    private VisualClock(){}
}
