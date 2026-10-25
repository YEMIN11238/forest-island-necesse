package localforest.world;

import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Small templates applied only during native NEW-region generation. Their
 * object state is thereafter owned by native region saves, never repaired on draw. */
final class ContinuousRegionContent {
    static void surface(Level level,Region region,MosslumeContinuousStack stack){
        WorldDefinition d=stack.layout.world;ContinuousWorldLayout layout=stack.layout;
        for(int rx=0;rx<region.tileWidth;rx++)for(int ry=0;ry<region.tileHeight;ry++){
            int x=region.tileXOffset+rx,y=region.tileYOffset+ry;
            if(stack.coast.owned(x,y)){
                // Native SurfaceLevel already generated matching water/land and
                // liquid collision; only newly generated local object content is ours.
                level.setObject(x,y,0);
                String vegetation=stack.coast.object(x,y);
                if(vegetation!=null)place(level,vegetation,x,y,0);
            }
            if(layout.house(x,y))region.tileLayer.setTileByRegion(rx,ry,TileRegistry.woodFloorID);
            else if(layout.farmland(x,y))region.tileLayer.setTileByRegion(rx,ry,TileRegistry.farmlandID);
            else if(stack.coast.path(x,y))region.tileLayer.setTileByRegion(rx,ry,NaturalPathWorld.tileID);
        }
        for(WorldContent.TemplateObject object:d.content.campObjects){
            int x=object.x(d),y=object.y(d);if(!contains(region,x,y))continue;
            // A cave entrance must be usable even if the native river passes here.
            if(object.role().equals("entrance")){
                for(int dx=-2;dx<=2;dx++)for(int dy=-2;dy<=2;dy++){
                    int tx=x+dx,ty=y+dy;if(!contains(region,tx,ty))continue;
                    level.setTile(tx,ty,TileRegistry.grassID);level.setObject(tx,ty,0);
                }
            }
            place(level,object.id(),x,y,object.rotation());
            var entity=level.entityManager.getObjectEntity(x,y);
            if(object.role().equals("starter_storage")&&entity instanceof InventoryObjectEntity storage){
                int slot=0;for(String item:d.content.csv("starter.items")){
                    String[] pair=item.split(":");storage.getInventory().setItem(slot++,new InventoryItem(pair[0],Integer.parseInt(pair[1])));
                }
            }else if(object.role().equals("fuel")&&entity instanceof FueledInventoryObjectEntity fire){
                fire.inventory.setItem(0,new InventoryItem("oaklog",d.content.integer("starter.fuel")));fire.keepRunning=true;fire.useFuel();
            }
        }
        outpost(level,region,stack);
    }
    private static void outpost(Level level,Region region,MosslumeContinuousStack stack){
        ContinuousWorldLayout layout=stack.layout;
        if(!layout.outpostRegion(region.regionX,region.regionY))return;
        int x=region.tileXOffset+8,y=region.tileYOffset+8;
        if(Math.hypot((double)x-layout.world.campX,(double)y-layout.world.campY)<64)return;
        String biome=layout.biome(x,y);if(biome.equals("forest"))return;
        // Keep every owned tile/approach inside this 16x16 region. This avoids
        // recursively generating neighbours merely to place a decorative building.
        for(int dx=-4;dx<=4;dx++)for(int dy=-2;dy<=4;dy++){
            if(stack.isSurfaceExpensiveWater(x+dx,y+dy)||!layout.biome(x+dx,y+dy).equals(biome))return;
        }
        for(int dx=-4;dx<=4;dx++)for(int dy=-2;dy<=4;dy++)level.setObject(x+dx,y+dy,0);
        int variant=(int)(layout.random(region.regionX,region.regionY,64)*3);
        place(level,"mlbiome_"+biome+"_structure"+variant,x,y,0);
        if(variant==0){place(level,"workstation",x-3,y+3,0);place(level,"torch",x+3,y+3,0);}
        var entity=level.entityManager.getObjectEntity(x,y);
        if(entity instanceof InventoryObjectEntity storage){
            storage.getInventory().setItem(0,new InventoryItem("apple",4));storage.getInventory().setItem(1,new InventoryItem("torch",3));
        }
    }
    static void cave(Level level,Region region,WorldDefinition definition){
        for(int rx=0;rx<region.tileWidth;rx++)for(int ry=0;ry<region.tileHeight;ry++){
            int x=region.tileXOffset+rx,y=region.tileYOffset+ry;
            if(Math.hypot((double)x-definition.caveX,(double)y-definition.caveY)>5)continue;
            region.tileLayer.setTileByRegion(rx,ry,TileRegistry.rockID);level.setObject(x,y,0);
        }
    }
    private static void place(Level level,String id,int x,int y,int rotation){GameObject object=ObjectRegistry.getObject(id);object.placeObject(level,x,y,rotation,false);}
    private static boolean contains(Region region,int x,int y){return x>=region.tileXOffset&&y>=region.tileYOffset&&x<region.tileXOffset+region.tileWidth&&y<region.tileYOffset+region.tileHeight;}
    private ContinuousRegionContent(){}
}
