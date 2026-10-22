package localforest.world;

import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;

/** One-time upgrade of the generated trail. User terrain edits are never continually repaired. */
public final class NaturalPathWorld {
    private static final String MIGRATION="mosslumeStablePathsV1";
    public static int tileID;
    public static long migratedTiles,restoredGrassTiles,removedGrassObjects,preservedPlayerTiles,preservedOtherTiles;
    public static void init(){tileID=TileRegistry.registerTile("mosslumenaturalpath",new NaturalPathTile(),0f,false,false,false);}
    public static void migrate(Level level) {
        if(!IslandWorld.isIsland(level)||WorldDefinition.forLevel(level)!=null||!level.isServer()||level.gndData.getBoolean(MIGRATION))return;
        level.regionManager.ensureTilesAreLoaded(0,0,IslandLayout.WIDTH-1,IslandLayout.HEIGHT-1);
        int changed=0,restored=0,removed=0,player=0,other=0;
        for(int[] p:IslandLayout.PATHS) {
            int x=p[0]+IslandLayout.OFFSET_X,y=p[1]+IslandLayout.OFFSET_Y;
            if(level.tileLayer.isPlayerPlaced(x,y)){player++;continue;}
            int old=level.getTileID(x,y);
            // Only natural grass/dirt from the generated trail can be upgraded. This excludes
            // farmland, wood floors, other crafted floors, liquids and any later replacement.
            if(old!=TileRegistry.dirtID&&old!=TileRegistry.grassID){if(old!=tileID)other++;continue;}
            if(old==TileRegistry.grassID)restored++;
            level.setTile(x,y,tileID);level.sendTileUpdatePacket(x,y);changed++;
            if("grass".equals(level.getObject(x,y).getStringID())&&!level.objectLayer.isPlayerPlaced(x,y)) {
                level.setObject(x,y,0);level.sendObjectUpdatePacket(x,y);removed++;
            }
        }
        level.gndData.setBoolean(MIGRATION,true);
        migratedTiles+=changed;restoredGrassTiles+=restored;removedGrassObjects+=removed;
        preservedPlayerTiles+=player;preservedOtherTiles+=other;
        System.out.println("MOSSLUME_PATH_MIGRATION changed="+changed+" restoredGrass="+restored
            +" removedWildGrass="+removed+" preservedPlayer="+player+" preservedOther="+other);
    }
    private NaturalPathWorld(){}
}
