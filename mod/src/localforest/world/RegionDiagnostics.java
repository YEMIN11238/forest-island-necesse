package localforest.world;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import necesse.engine.gameLoop.tickManager.PerformanceTimerManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.client.ClientLevelLoading;
import necesse.engine.network.packet.PacketRequestRegionData;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionManager;
import net.bytebuddy.asm.Advice;

/** Opt-in, bounded lifecycle evidence. No disk I/O, model calls or level loading
 * in hooks. Use -Dmosslume.regionDiagnostics=true in an isolated QA process. */
public final class RegionDiagnostics {
    private static final ArrayDeque<Event> events=new ArrayDeque<>();
    private static final Map<String,Pending> pending=new LinkedHashMap<>();
    private static final LongAdder invalidatedCaches=new LongAdder();
    private static long sequence,lastServerTick,lastReport;
    public record Event(long sequence,long elapsedMillis,String level,int x,int y,String stage,int loaded,String detail){}
    private record Pending(long sequence,long time){}
    private static final long START=System.nanoTime();
    public static boolean enabled(){return Boolean.getBoolean("mosslume.regionDiagnostics");}
    public static void invalidatedRegionCache(){invalidatedCaches.increment();}
    public static long invalidatedCaches(){return invalidatedCaches.sum();}
    public static synchronized void record(Level level,int x,int y,String stage,int loaded,String detail){
        if(!enabled()||!IslandWorld.isIsland(level))return;
        String id=level.getIdentifier().toString(),key=id+":"+x+":"+y;long now=System.nanoTime();
        Pending request=pending.get(key);
        if(stage.equals("request")){
            request=new Pending(++sequence,now);pending.put(key,request);
            while(pending.size()>512)pending.remove(pending.keySet().iterator().next());
        }
        long serial=request==null?++sequence:request.sequence;
        if(stage.equals("applied")||stage.equals("failed"))pending.remove(key);
        events.addLast(new Event(serial,(now-START)/1_000_000,id,x,y,stage,loaded,detail==null?"":detail));
        while(events.size()>128)events.removeFirst();
    }
    public static synchronized Map<String,Object> snapshot(){
        long now=System.nanoTime();Map<String,Object> result=new LinkedHashMap<>();
        result.put("enabled",enabled());result.put("invalidatedUnloadedCaches",invalidatedCaches.sum());
        result.put("serverTickAgeMs",lastServerTick==0?-1:(now-lastServerTick)/1_000_000);
        result.put("pendingRegions",pending.size());
        result.put("oldestRequestMs",pending.values().stream().mapToLong(p->(now-p.time)/1_000_000).max().orElse(0));
        result.put("events",events.stream().map(e->Map.of("sequence",e.sequence(),"elapsedMillis",e.elapsedMillis(),"level",e.level(),"x",e.x(),"y",e.y(),"stage",e.stage(),"loaded",e.loaded(),"detail",e.detail())).toList());return result;
    }
    public static synchronized void serverTick(long start){
        if(!enabled())return;long now=System.nanoTime();lastServerTick=now;
        long elapsed=(now-start)/1_000_000;
        if(elapsed>250||now-lastReport>5_000_000_000L){
            lastReport=now;
            System.out.println("MOSSLUME_REGION_HEARTBEAT tickMs="+elapsed+" pending="+pending.size()+" invalidatedCaches="+invalidatedCaches.sum());
            if(elapsed>1000)System.err.println("MOSSLUME_SLOW_SERVER_TICK "+snapshot());
        }
    }
    public static long begin(){return enabled()?System.nanoTime():0;}
    public static void loaded(RegionManager manager,Region region,long start,Throwable error){
        if(start==0)return;long elapsed=(System.nanoTime()-start)/1_000_000;
        record(manager.level,region.regionX,region.regionY,error==null?"loaded":"failed",manager.getLoadedRegionsSize(),elapsed+"ms"+(error==null?"":" "+error.getClass().getSimpleName()));
        if(elapsed>120||error!=null)System.err.println("MOSSLUME_REGION_LOAD level="+manager.level.getIdentifier()+" region="+region.regionX+","+region.regionY+" elapsedMs="+elapsed+" error="+(error==null?"none":error));
    }
    @ModMethodPatch(target=ClientLevelLoading.class,name="sendRequest",arguments={int.class,int.class})
    public static class ClientRequest {
        @Advice.OnMethodEnter public static void before(@Advice.This ClientLevelLoading loading,@Advice.Argument(0) int x,@Advice.Argument(1) int y){record(loading.level,x,y,"request",loading.getRegionsLoadedCount(),"queued="+loading.getRegionsLoadQueueCount());}
    }
    @ModMethodPatch(target=PacketRequestRegionData.class,name="processServer",arguments={NetworkPacket.class,Server.class,ServerClient.class})
    public static class ServerRequest {
        @Advice.OnMethodEnter public static void before(@Advice.This PacketRequestRegionData packet,@Advice.Argument(2) ServerClient client){record(client.getLevel(),packet.regionX,packet.regionY,"serverReceived",-1,"");}
        @Advice.OnMethodExit(onThrowable=Throwable.class) public static void after(@Advice.This PacketRequestRegionData packet,@Advice.Argument(2) ServerClient client,@Advice.Thrown Throwable error){record(client.getLevel(),packet.regionX,packet.regionY,error==null?"serverHandled":"failed",-1,error==null?"client applied is the success signal":error.toString());}
    }
    @ModMethodPatch(target=RegionManager.class,name="loadNewRegion",arguments={Region.class,boolean.class})
    public static class RegionLoad {
        @Advice.OnMethodEnter public static long before(@Advice.This RegionManager manager,@Advice.Argument(0) Region region){long started=begin();record(manager.level,region.regionX,region.regionY,"loadBegin",manager.getLoadedRegionsSize(),"");return started;}
        @Advice.OnMethodExit(onThrowable=Throwable.class) public static void after(@Advice.This RegionManager manager,@Advice.Argument(0) Region region,@Advice.Enter long start,@Advice.Thrown Throwable error){loaded(manager,region,start,error);}
    }
    @ModMethodPatch(target=ClientLevelLoading.class,name="applyRegionData",arguments={int.class,int.class,Packet.class})
    public static class ClientApply {
        @Advice.OnMethodExit(onThrowable=Throwable.class) public static void after(@Advice.This ClientLevelLoading loading,@Advice.Argument(0) int x,@Advice.Argument(1) int y,@Advice.Thrown Throwable error){
            boolean valid=loading.isRegionLoaded(x,y);
            record(loading.level,x,y,error!=null?"failed":valid?"applied":"requeued",loading.getRegionsLoadedCount(),error==null?"":error.toString());
        }
    }
    @ModMethodPatch(target=Server.class,name="tick",arguments={})
    public static class Heartbeat {
        @Advice.OnMethodEnter public static long before(){return begin();}
        @Advice.OnMethodExit public static void after(@Advice.Enter long start){if(start!=0)serverTick(start);}
    }
    @ModMethodPatch(target=RegionManager.class,name="getSaveHandler",arguments={PerformanceTimerManager.class,Server.class})
    public static class Saving {
        @Advice.OnMethodEnter public static long before(@Advice.This RegionManager manager){record(manager.level,Integer.MIN_VALUE,Integer.MIN_VALUE,"saveBegin",manager.getLoadedRegionsSize(),"");return begin();}
        @Advice.OnMethodExit(onThrowable=Throwable.class) public static void after(@Advice.This RegionManager manager,@Advice.Enter long start,@Advice.Thrown Throwable error){if(start!=0)record(manager.level,Integer.MIN_VALUE,Integer.MIN_VALUE,error==null?"saveEnd":"saveFailed",manager.getLoadedRegionsSize(),(System.nanoTime()-start)/1_000_000+"ms");}
    }
    private RegionDiagnostics(){}
}
