package localforest.visual;

import java.util.*;
import localforest.scene.RegionSceneQuery;
import localforest.scene.SceneStructures;
import localforest.scene.StructureInstance;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;

/** Live native floor/object checks plus a once-per-preparation occupied-house snapshot. */
public final class GroundDecorationPolicy {
    private static final Map<Level,Snapshot> snapshots=new WeakHashMap<>();
    private static final class Snapshot {
        final long signature;final List<StructureInstance> occupied;
        Snapshot(long signature,List<StructureInstance> occupied){this.signature=signature;this.occupied=List.copyOf(occupied);}
    }
    public static synchronized void refresh(Level level){
        ArrayList<StructureInstance> occupied=new ArrayList<>();long signature=17;
        for(StructureInstance house:SceneStructures.houses(level)){
            boolean exists=SceneStructures.exists(level,house);
            signature=signature*31+house.footTileX;signature=signature*31+house.footTileY;signature=signature*31+(exists?1:0);
            signature=signature*31+house.left;signature=signature*31+house.top;signature=signature*31+house.right;signature=signature*31+house.bottom;
            if(exists)occupied.add(house);
        }
        Snapshot old=snapshots.get(level);
        if(old==null||old.signature!=signature){snapshots.put(level,new Snapshot(signature,occupied));SceneLighting.invalidate(level);}
    }
    private static synchronized boolean occupied(Level level,int x,int y){
        Snapshot snapshot=snapshots.get(level);if(snapshot==null){refresh(level);snapshot=snapshots.get(level);}
        for(StructureInstance house:snapshot.occupied)if(house.contains(x,y))return true;return false;
    }
    public static boolean canShowWildPlant(Level level,int x,int y){
        return RegionSceneQuery.loaded(level,x,y)&&!level.getTile(x,y).isFloor&&!occupied(level,x,y);
    }
    public static boolean canDecorate(Level level,int x,int y){
        if(!canShowWildPlant(level,x,y)||level.getObjectID(x,y)!=0||level.tileLayer.isPlayerPlaced(x,y))return false;
        GameTile tile=level.getTile(x,y);
        return tile.isOrganic&&"grasstile".equals(tile.getStringID());
    }
    private GroundDecorationPolicy(){}
}
