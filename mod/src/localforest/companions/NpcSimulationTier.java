package localforest.companions;

/** Bounds our extension work; native unloading, sleep, hunger and combat stay authoritative. */
public enum NpcSimulationTier {
    NEAR(250), LOADED_DISTANT(2_000), UNLOADED(30_000);
    public final long observationInterval;
    NpcSimulationTier(long interval){observationInterval=interval;}
    public static NpcSimulationTier of(boolean loaded,boolean samePlace,double distance){
        return !loaded?UNLOADED:samePlace&&distance<=768?NEAR:LOADED_DISTANT;
    }
}
