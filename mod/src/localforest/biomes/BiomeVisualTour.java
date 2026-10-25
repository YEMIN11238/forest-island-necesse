package localforest.biomes;

import java.awt.Point;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import localforest.VisualSceneProbe;
import localforest.world.BiomeLayout;
import localforest.world.WorldDefinition;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.gfx.camera.MainGameCamera;
import necesse.level.maps.Level;

/** Opt-in native camera tour; never teleports the player or fabricates a scene. */
public final class BiomeVisualTour {
    private final MainGameCamera original;
    private final BiomeLayout layout;
    private final List<String> results=new ArrayList<>();
    private int index,frames,loadedFrames;
    private long started,loadedSince;
    private Point center;
    private boolean done;
    private boolean timeoutStackWritten;
    private String lastStreaming="null";
    public static boolean enabled(){return Boolean.getBoolean("mosslume.visualProbe.biomeTour");}
    public BiomeVisualTour(MainGame game,Level level,long now){
        WorldDefinition d=WorldDefinition.forLevel(level);
        if(d==null||!d.isMultibiome()||level.isCave)throw new IllegalStateException("Biome tour requires the v2 six-biome surface");
        original=game.getCamera();layout=new BiomeLayout(d);startView(game,now);
    }
    private void startView(MainGame game,long now){
        center=layout.center(BiomeLayout.IDS[index]);
        game.setCamera(new TourCamera(center.x*32+16,center.y*32+16));
        started=now;frames=0;loadedFrames=0;loadedSince=0;
        System.out.println("MOSSLUME_BIOME_TOUR_VIEW biome="+BiomeLayout.IDS[index]+" center="+center.x+","+center.y);
    }
    /** A non-null name means the previous GL_FRONT frame is settled and its visible regions are loaded. */
    public String ready(MainGame game,Client client,Level level,long now){
        if(done)return null;
        frames++;
        var loading=client.levelManager.loading();
        var camera=game.getCamera();
        int halfW=Math.max(32,camera.getWidth()/2)+64,halfH=Math.max(32,camera.getHeight()/2)+64;
        int left=Math.max(0,(center.x*32+16-halfW)/32),right=Math.min(level.tileWidth-1,(center.x*32+16+halfW)/32);
        int top=Math.max(0,(center.y*32+16-halfH)/32),bottom=Math.min(level.tileHeight-1,(center.y*32+16+halfH)/32);
        boolean allLoaded=loading!=null;
        List<String> missing=new ArrayList<>();
        if(loading!=null){
            for(int rx=left/16;rx<=right/16;rx++)for(int ry=top/16;ry<=bottom/16;ry++){
                if(!loading.isRegionLoaded(rx,ry)){
                    allLoaded=false;
                    missing.add("{\"x\":"+rx+",\"y\":"+ry+",\"inQueue\":"+loading.isRegionInQueue(rx,ry)
                        +",\"regionPresent\":"+(level.regionManager.getRegion(rx,ry,false)!=null)+"}");
                    // Use the game's network request machinery. This can stream/generate the
                    // normal destination region; no direct server or player mutation occurs.
                    if(!loading.isRegionInQueue(rx,ry))loading.sendRequest(rx,ry);
                }else{
                    var region=level.regionManager.getRegion(rx,ry,false);
                    if(region!=null)region.unloadRegionBuffer.keepLoaded();
                }
            }
        }
        lastStreaming="{\"loadingPresent\":"+(loading!=null)+",\"loadedCount\":"+(loading==null?0:loading.getRegionsLoadedCount())
            +",\"queuedCount\":"+(loading==null?0:loading.getRegionsLoadQueueCount())+",\"requestedCount\":"+(loading==null?0:loading.getRegionsRequestedCount())
            +",\"missing\":["+String.join(",",missing)+"]}";
        if(allLoaded){if(loadedSince==0)loadedSince=now;loadedFrames++;}else{loadedSince=0;loadedFrames=0;}
        if(frames>=3&&loadedFrames>=3&&loadedSince>0&&now-loadedSince>=1_000_000_000L)return "biome-"+BiomeLayout.IDS[index]+".png";
        if(now-started>5_000_000_000L){
            System.err.println("MOSSLUME_BIOME_TOUR_TIMEOUT biome="+BiomeLayout.IDS[index]+" streaming="+lastStreaming);
            if(!timeoutStackWritten){timeoutStackWritten=true;VisualSceneProbe.writeThreadDiagnostics("biome-tour-timeout-threads.txt","biome="+BiomeLayout.IDS[index]+" streaming="+lastStreaming);}
            record(false,now,level,"viewport regions did not finish loading and settling");advance(game,now);
        }
        return null;
    }
    public void captured(MainGame game,Level level,long now){record(true,now,level,null);advance(game,now);}
    private void record(boolean success,long now,Level level,String error){
        results.add("{\"biome\":\""+BiomeLayout.IDS[index]+"\",\"file\":"+(success?"\"biome-"+BiomeLayout.IDS[index]+".png\"":"null")+",\"captured\":"+success+
            ",\"centerTileX\":"+center.x+",\"centerTileY\":"+center.y+",\"settledLoadedFrames\":"+loadedFrames+",\"elapsedSeconds\":"+String.format(Locale.ROOT,"%.3f",(now-started)/1e9)+
            ",\"environmentHour\":"+String.format(Locale.ROOT,"%.3f",level.getWorldEntity().getDayTimeHourFloat())+",\"streaming\":"+lastStreaming+",\"error\":"+(error==null?"null":"\""+error+"\"")+"}");
    }
    private void advance(MainGame game,long now){index++;if(index>=BiomeLayout.IDS.length){done=true;game.setCamera(original);}else startView(game,now);}
    public boolean done(){return done;}
    public void writeManifest(Path output)throws java.io.IOException{
        Files.writeString(output.resolve("biome-tour.json"),"{\"optIn\":true,\"readBuffer\":\"GL_FRONT previous presented frame\",\"playerTeleported\":false,\"nativeRegionStreaming\":true,\"cameraRestored\":"+done+",\"scope\":\"Additional daytime camera captures after normal visual/performance probe; captures require actual loaded viewport regions. Creature visibility and content quality require image inspection.\",\"views\":["+String.join(",",results)+"]}\n");
    }
    private static final class TourCamera extends MainGameCamera {
        private final int x,y;TourCamera(int x,int y){this.x=x;this.y=y;}
        @Override public void tickCamera(TickManager tick,MainGame game,Client client){centerCamera(x,y);}
    }
}
