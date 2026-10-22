package localforest.world;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Native tile/object/region storage; only the finite content generator is replaced. */
public class GeneratedWorldLevel extends Level {
    private GeneratedLayout layout;
    public GeneratedWorldLevel(LevelIdentifier id,int width,int height,WorldEntity world){
        super(id,width,height,world);
        isCave=LevelIdentifier.CAVE_IDENTIFIER.equals(id);
        baseBiome=BiomeRegistry.getBiome("forest");
        WorldDefinition definition=WorldDefinition.forWorld(world);
        if(definition!=null)definition.stamp(this);
        gndData.setBoolean("localforestisland",true);
        gndData.setBoolean("mosslumeGenerated",true);
    }
    public GeneratedLayout layout(){
        if(layout==null){WorldDefinition d=WorldDefinition.forLevel(this);if(d==null)throw new IllegalStateException("Generated world definition missing");layout=new GeneratedLayout(d);}
        return layout;
    }
    @Override public boolean isSurfaceLevel(){return !isCave;}
    @Override public GameMessage getLocationMessage(int x,int y){WorldDefinition d=WorldDefinition.forLevel(this);return new StaticMessage(d==null?"苔光世界":isCave?"苔光洞穴":d.displayName());}
    @Override public void generateRegion(Region r){
        GeneratedLayout g=layout();WorldDefinition d=g.world;
        for(int x=0;x<r.tileWidth;x++)for(int y=0;y<r.tileHeight;y++){
            int tx=r.tileXOffset+x,ty=r.tileYOffset+y;
            int tile;
            if(isCave)tile=TileRegistry.rockID;
            else if(!g.land(tx,ty))tile=TileRegistry.waterID;
            else if(g.house(tx,ty))tile=TileRegistry.woodFloorID;
            else if(g.farmland(tx,ty))tile=TileRegistry.farmlandID;
            else if(g.path(tx,ty))tile=NaturalPathWorld.tileID;
            else tile=TileRegistry.getTileID(d.content.text(g.biome(tx,ty)+".ground"));
            r.tileLayer.setTileByRegion(x,y,tile);
            r.biomeLayer.setBiomeByRegion(x,y,BiomeRegistry.getBiome(d.content.text(g.biome(tx,ty)+".nativeBiome")).getID());
        }
        for(int x=0;x<r.tileWidth;x++)for(int y=0;y<r.tileHeight;y++){
            int tx=r.tileXOffset+x,ty=r.tileYOffset+y;
            if(isCave){
                if(!g.inside(tx,ty))place("lfcoastbarrier",tx,ty,0);
                else if(!g.caveFloor(tx,ty)){
                    double ore=g.random(tx,ty,76);
                    place(ore<.08?"ironorerock":ore<.18?"copperorerock":"rock",tx,ty,0);
                }
            }else{
                if(g.coast(tx,ty))place("lfcoastbarrier",tx,ty,0);
                else {String object=g.object(tx,ty);if(object!=null)place(object,tx,ty,0);}
            }
        }
        if(!isCave)camp(r,d);
        // The native ladder creates and persists its reciprocal endpoint on first use.
    }
    private boolean contains(Region r,int x,int y){return x>=r.tileXOffset&&y>=r.tileYOffset&&x<r.tileXOffset+r.tileWidth&&y<r.tileYOffset+r.tileHeight;}
    private void camp(Region r,WorldDefinition d){
        for(WorldContent.TemplateObject object:d.content.campObjects){
            int x=object.x(d),y=object.y(d);
            if(!contains(r,x,y))continue;
            place(object.id(),x,y,object.rotation());
            ObjectEntity oe=entityManager.getObjectEntity(x,y);
            if(object.role().equals("starter_storage")&&oe instanceof InventoryObjectEntity){
                int slot=0;
                for(String item:d.content.csv("starter.items")){
                    String[] pair=item.split(":");
                    ((InventoryObjectEntity)oe).getInventory().setItem(slot++,new InventoryItem(pair[0],Integer.parseInt(pair[1])));
                }
            }
            if(object.role().equals("fuel")&&oe instanceof FueledInventoryObjectEntity){
                FueledInventoryObjectEntity fire=(FueledInventoryObjectEntity)oe;
                fire.inventory.setItem(0,new InventoryItem("oaklog",d.content.integer("starter.fuel")));
                fire.keepRunning=true;fire.useFuel();
            }
        }
    }
    private void place(String id,int x,int y,int rotation){
        int objectID=ObjectRegistry.getObjectID(id);
        if(objectID<0)throw new IllegalStateException("Unknown content object "+id);
        GameObject object=ObjectRegistry.getObject(objectID);
        object.placeObject(this,x,y,rotation,false);
    }
}
