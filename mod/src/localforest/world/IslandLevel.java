package localforest.world;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Native Level: tiles, objects, entities and region persistence all remain engine-owned. */
public class IslandLevel extends Level {
 public IslandLevel(LevelIdentifier id,int width,int height,WorldEntity world){
  super(id,width,height,world);
  baseBiome=BiomeRegistry.getBiome("forest");
  gndData.setBoolean("localforestisland",true);
 }
 @Override public boolean isSurfaceLevel(){return true;}
 @Override public void generateRegion(Region r){
  for(int x=0;x<r.tileWidth;x++)for(int y=0;y<r.tileHeight;y++){
   int tx=x+r.tileXOffset,ty=y+r.tileYOffset;
   int tile=IslandLayout.isLand(tx,ty)?(IslandLayout.isPath(tx,ty)?TileRegistry.dirtID:TileRegistry.grassID):TileRegistry.waterID;
   int sx=tx-IslandLayout.OFFSET_X,sy=ty-IslandLayout.OFFSET_Y;
   if(IslandLayout.isHouse(sx,sy))tile=TileRegistry.woodFloorID;
   if(sx>=17&&sx<=19&&sy>=29&&sy<=31)tile=TileRegistry.farmlandID;
   r.tileLayer.setTileByRegion(x,y,tile);
   r.biomeLayer.setBiomeByRegion(x,y,baseBiome.getID());
  }
  IslandWorld.populateRegion(this,r);
 }
 @Override public GameMessage getLocationMessage(int x,int y){return new StaticMessage("林间小岛");}
}
