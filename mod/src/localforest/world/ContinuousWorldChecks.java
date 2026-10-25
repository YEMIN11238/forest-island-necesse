package localforest.world;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import necesse.engine.GlobalData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Explicit isolated native QA entry. Runs on the server thread; never on a
 * player save. Loading/reloading here proves region storage, not walk streaming. */
public final class ContinuousWorldChecks {
    public static Map<String,Object> run(ServerClient client){
        return run(client,false);
    }
    public static Map<String,Object> run(ServerClient client,boolean readBack){
        Path profile=Path.of(GlobalData.appDataPath()).toAbsolutePath().normalize();
        require(Boolean.getBoolean("mosslume.regionProbe")&&profile.getFileName().toString().startsWith("Mosslume-QA-"),"Disposable QA profile and regionProbe are required");
        Level level=client.getLevel();WorldDefinition d=WorldDefinition.forLevel(level);
        require(level instanceof ContinuousSurfaceLevel&&d!=null&&d.isContinuous(),"Continuous v4 surface required");
        require(level.isOneWorldLevel()&&level.tileWidth==0&&level.tileHeight==0,"Native unbounded bounds missing");
        var samples=new ArrayList<Map<String,Object>>();
        for(int[] position:new int[][]{{-24,0},{24,0},{0,-24},{0,24}}){
            int rx=position[0],ry=position[1];
            Region first=level.regionManager.getRegion(rx,ry,true);require(first!=null&&!first.isDisposed(),"Region unavailable");
            long fingerprint=fingerprint(first);
            int tx=first.tileXOffset+7,ty=first.tileYOffset+7;
            if(readBack)require(level.getTileID(tx,ty)==TileRegistry.woodFloorID,"Disk reopen lost remote region checkpoint");
            level.setTile(tx,ty,TileRegistry.woodFloorID);
            // The production unload lifecycle updates the native region save before
            // disposing its buffers. Do not regenerate it or hand-copy its layers.
            require(level.regionManager.unloadRegion(rx,ry),"Unload failed");
            require(level.regionManager.getRegion(rx,ry,false)==null,"Unloaded region is still cached");
            Region second=level.regionManager.getRegion(rx,ry,true);
            require(second!=first&&!second.isDisposed(),"Reload returned disposed/stale region");
            require(level.getTileID(tx,ty)==TileRegistry.woodFloorID,"Native reload lost player edit");
            samples.add(Map.of("regionX",rx,"regionY",ry,"initialFingerprint",Long.toUnsignedString(fingerprint),"editPreserved",true,"freshInstance",true,"diskCheckpointReadBack",readBack));
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("generatorVersion",d.generatorVersion);result.put("bounds","native-unbounded");result.put("samples",samples);
        result.put("diagnostics",RegionDiagnostics.snapshot());
        result.put("scope",readBack?"Server remote-region checkpoint verified after process/disk reopen; native unload/reload repeated. Client traversal and cave round trip are separate checks.":"Server generation and native unload/reload only; does not prove client traversal, cave round trip or disk reopen until a --reload probe.");
        return result;
    }
    private static long fingerprint(Region region){
        long value=0xcbf29ce484222325L;
        for(int y=0;y<region.tileHeight;y++)for(int x=0;x<region.tileWidth;x++){
            value=(value^region.tileLayer.getTileIDByRegion(x,y))*0x100000001b3L;
            value=(value^region.objectLayer.getObjectIDByRegion(0,x,y))*0x100000001b3L;
            value=(value^region.biomeLayer.getBiomeIDByRegion(x,y))*0x100000001b3L;
        }return value;
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private ContinuousWorldChecks(){}
}
