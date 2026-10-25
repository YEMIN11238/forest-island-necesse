package localforest.world;

import java.awt.Point;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientLevelLoading;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Server-driven same-level teleports, followed by ordinary client region
 * requests/application. Client state is sampled ONLY on its frame thread. */
public final class NativeContinuousTraversalCheck {
    private record Destination(int sequence,int tileX,int tileY,int regionX,int regionY,int pixelX,int pixelY,int tileId,byte teleportBuffer){}
    private record Observation(long frame,int sequence,boolean sameLevel,boolean position,boolean teleport,
        boolean neighborsLoaded,boolean ground,boolean disposed,int tileId,int loaded,int queued,int requested){}
    private volatile Destination destination;
    private volatile Observation observation;
    private long frameSequence;
    private long began,legBegan,lastObservedFrame;
    private int leg,stableSamples;
    private Point home;
    private ServerClient serverClient;
    private boolean done;
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Map<String,Object>> checkpoints=new ArrayList<>();
    private static final int[][] ROUTE={{24,0},{-24,0},{0,24},{0,-24}};

    /** Called by RefactorNativeChecks.frame after the native client frame. */
    public void observe(Client client){
        Destination wanted=destination;if(wanted==null||client==null)return;
        Level level=client.getLevel();PlayerMob player=client.getPlayer();ClientLevelLoading loading=client.levelManager.loading();
        if(level==null||player==null||loading==null)return;
        boolean same=level instanceof ContinuousSurfaceLevel&&loading.level==level;
        boolean position=Math.hypot(player.x-wanted.pixelX,player.y-wanted.pixelY)<=4;
        boolean teleport=player.getMovementTeleportBuffer()==wanted.teleportBuffer;
        boolean neighbors=same;
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)neighbors&=loading.isRegionLoaded(wanted.regionX+dx,wanted.regionY+dy);
        Region region=level.regionManager.getRegion(wanted.regionX,wanted.regionY,false);
        int tile=-1;boolean disposed=region!=null&&region.isDisposed(),ground=false;
        if(region!=null&&!disposed&&loading.isRegionLoaded(wanted.regionX,wanted.regionY)){
            int rx=wanted.tileX-region.tileXOffset,ry=wanted.tileY-region.tileYOffset;
            tile=region.tileLayer.getTileIDByRegion(rx,ry);
            ground=!region.tileLayer.getTileByRegion(rx,ry).isLiquid;
        }
        observation=new Observation(++frameSequence,wanted.sequence,same,position,teleport,neighbors,ground,disposed,tile,
            loading.getRegionsLoadedCount(),loading.getRegionsLoadQueueCount(),loading.getRegionsRequestedCount());
    }

    /** Called once per real server tick; never blocks or injects region packets. */
    public boolean tick(ServerClient server,Client local,long nowNanos){
        if(done)return true;
        try{
            require(safeProfile(),"Explicit region QA flag and disposable profile required");
            require(server!=null&&local!=null&&local.getLocalServer()==server.getServer(),"Local server/client pair missing");
            if(began==0){
                began=nowNanos;serverClient=server;home=new Point(Math.round(server.playerMob.x),Math.round(server.playerMob.y));
                require(server.getLevel() instanceof ContinuousSurfaceLevel,"Continuous surface required");
                evidence.put("timeoutSeconds",timeoutSeconds());
                evidence.put("scope","Server drives native same-level teleport/position packets; ordinary ClientLevelLoading requests/applies each destination and a 3x3 region neighborhood. No physical walking/keyboard claim, no client terrain/region injection.");
                beginLeg(server,nowNanos);return false;
            }
            require(nowNanos-began<timeoutSeconds()*1_000_000_000L,"Client traversal timed out at leg "+leg+" observation="+observation);
            Observation seen=observation;Destination wanted=destination;
            if(seen==null||wanted==null||seen.sequence!=wanted.sequence||seen.frame==lastObservedFrame)return false;
            lastObservedFrame=seen.frame;
            boolean ready=seen.sameLevel&&seen.position&&seen.teleport&&seen.neighborsLoaded&&seen.ground&&!seen.disposed&&seen.tileId==wanted.tileId;
            stableSamples=ready?stableSamples+1:0;
            if(stableSamples<4)return false;
            Map<String,Object> checkpoint=new LinkedHashMap<>();
            checkpoint.put("step",leg);checkpoint.put("returnHome",leg==ROUTE.length);checkpoint.put("tile",List.of(wanted.tileX,wanted.tileY));
            checkpoint.put("region",List.of(wanted.regionX,wanted.regionY));checkpoint.put("tileIdMatchedServer",true);
            checkpoint.put("teleportBufferMatched",true);checkpoint.put("nativeLoadedNeighborhood",9);checkpoint.put("stableServerSamples",stableSamples);
            checkpoint.put("clientLoadedRegions",seen.loaded);checkpoint.put("clientQueue",seen.queued);checkpoint.put("clientRequested",seen.requested);
            checkpoint.put("elapsedSeconds",(nowNanos-legBegan)/1e9);checkpoints.add(checkpoint);
            if(++leg>ROUTE.length){
                done=true;destination=null;evidence.put("passed",true);evidence.put("checkpoints",List.copyOf(checkpoints));
                evidence.put("elapsedSeconds",(nowNanos-began)/1e9);evidence.put("returnedToCamp",true);return true;
            }
            beginLeg(server,nowNanos);return false;
        }catch(RuntimeException error){
            evidence.put("passed",false);evidence.put("error",error.toString());evidence.put("leg",leg);
            evidence.put("lastObservation",String.valueOf(observation));evidence.put("checkpoints",List.copyOf(checkpoints));
            restoreHome();done=true;destination=null;return true;
        }
    }
    private void beginLeg(ServerClient server,long now){
        Level level=server.getLevel();Point tile;
        if(leg==ROUTE.length)tile=new Point(Math.floorDiv(home.x,32),Math.floorDiv(home.y,32));
        else{
            int regionX=ROUTE[leg][0],regionY=ROUTE[leg][1];
            Region region=level.regionManager.getRegion(regionX,regionY,true);
            require(region!=null&&!region.isDisposed(),"Server route region unavailable");
            tile=null;
            // Inspect only this newly generated region. The region QA's persisted
            // floor checkpoint is a valid option, but nothing is painted here.
            for(int radius=0;radius<=7&&tile==null;radius++)for(int dy=-radius;dy<=radius&&tile==null;dy++)for(int dx=-radius;dx<=radius;dx++){
                int x=region.tileXOffset+7+dx,y=region.tileYOffset+7+dy;
                if(!level.regionManager.isTileLoaded(x,y)||level.getTile(x,y).isLiquid||level.getObject(x,y).isSolid)continue;
                if(server.playerMob.collidesWith(level,x*32+16,y*32+16))continue;
                tile=new Point(x,y);break;
            }
            require(tile!=null,"No actual walkable ground at route region "+regionX+","+regionY);
        }
        require(!level.getTile(tile.x,tile.y).isLiquid&&!level.getObject(tile.x,tile.y).isSolid,"Route destination is not walkable");
        server.playerMob.dx=0;server.playerMob.dy=0;server.playerMob.moveX=0;server.playerMob.moveY=0;
        final Point pixels=leg==ROUTE.length?new Point(home):new Point(tile.x*32+16,tile.y*32+16);
        // This native same-level API increments movementTeleportBuffer, informs
        // the owning client and broadcasts position. It does not send region data.
        server.changeLevel(level.getIdentifier(),ignored->pixels,true);
        byte buffer=server.playerMob.getMovementTeleportBuffer();
        destination=new Destination(leg,tile.x,tile.y,Math.floorDiv(tile.x,16),Math.floorDiv(tile.y,16),pixels.x,pixels.y,level.getTileID(tile.x,tile.y),buffer);
        legBegan=now;stableSamples=0;lastObservedFrame=0;observation=null;
    }
    private void restoreHome(){
        if(home==null||serverClient==null||serverClient.playerMob==null)return;
        try{serverClient.changeLevel(serverClient.getLevelIdentifier(),ignored->new Point(home),true);}catch(RuntimeException ignored){/* Keep the original QA failure. */}
    }
    public Map<String,Object> evidence(){return Map.copyOf(evidence);}
    private static int timeoutSeconds(){return Math.max(10,Math.min(120,Integer.getInteger("mosslume.regionProbe.traversalTimeoutSeconds",20)));}
    private static boolean safeProfile(){return Boolean.getBoolean("mosslume.regionProbe")&&Path.of(GlobalData.appDataPath()).getFileName().toString().startsWith("Mosslume-QA-Refactor-");}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
