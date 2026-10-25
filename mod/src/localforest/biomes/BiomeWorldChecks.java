package localforest.biomes;

import java.awt.Point;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import localforest.world.BiomeLayout;
import localforest.world.GeneratedWorldLevel;
import localforest.world.GeneratedLayout;
import localforest.world.WorldDefinition;
import necesse.engine.GlobalData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.TileRegistry;
import necesse.entity.ObjectDamageResult;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

/** Opt-in, destructive only in a disposable native QA profile. Never installed in ordinary play. */
public final class BiomeWorldChecks {
    private static final String PREFIX="mosslumeBiomeQaV1";
    private static final int MARKER_COINS=53;
    public static Map<String,Object> verify(ServerClient client,boolean loaded){
        require(client!=null&&client.playerMob!=null,"Biome check requires native player/server");
        Path profile=Path.of(GlobalData.appDataPath()).toAbsolutePath().normalize();
        require(System.getProperty("mosslume.refactorProbe")!=null&&profile.getFileName().toString().startsWith("Mosslume-QA-Refactor"),"Refusing biome mutations outside explicit disposable QA profile");
        Level level=client.getLevel();WorldDefinition d=WorldDefinition.forLevel(level);
        require(level instanceof GeneratedWorldLevel&&!level.isCave&&d!=null&&d.isMultibiome(),"Six-region disposable surface required");
        GeneratedLayout layout=((GeneratedWorldLevel)level).layout();
        level.regionManager.ensureTilesAreLoaded(0,0,d.width-1,d.height-1);
        Map<String,Object> out=new LinkedHashMap<>(),terrain=new LinkedHashMap<>();
        if(d.generatorVersion>=3){
            int forestView=0,localRoads=0;
            for(int x=0;x<d.width;x++)for(int y=0;y<d.height;y++){
                if(Math.hypot(x-d.spawnX,y-d.spawnY)<=32){
                    require(!level.getTile(x,y).isLiquid&&!level.getTile(x,y).getStringID().startsWith("mlbiome_"),"Birthplace contains a foreign biome or water");
                    forestView++;
                }
                if(level.getTileID(x,y)==localforest.world.NaturalPathWorld.tileID){
                    require(Math.hypot(x-d.campX,y-d.campY)<26,"Global spoke road leaked into the wilderness");localRoads++;
                }
            }
            require(forestView>3000&&localRoads>50&&localRoads<160,"Invalid native home-forest footprint");
            out.put("worldDesign",Map.of("generatorVersion",d.generatorVersion,"forestSpawnTiles",forestView,"localRoadTiles",localRoads,
                "globalSpokeRoads",false,"landAndPlantPlanner","Adapted native IslandGeneration cell construction and native GenerationTools cellular automaton; finite habitat configuration is Mosslume."));
        }
        for(String biome:BiomeLayout.IDS){
            String id="mlbiome_"+biome+"_ground";int tile=TileRegistry.getTileID(id);require(tile>=0,"Biome tile registry missing: "+id);
            Point center=layout.biomeLayout().center(biome);require(layout.biome(center.x,center.y).equals(biome),"Biome center classification: "+biome);
            int found=0;for(int x=0;x<d.width;x++)for(int y=0;y<d.height;y++)if(level.getTileID(x,y)==tile)found++;
            require(found>100,"Missing live biome ground: "+biome);
            terrain.put(biome,Map.of("nativeTileID",tile,"liveTiles",found,"center",List.of(center.x,center.y)));
        }
        int collisionParts=0;
        for(var structure:layout.biomeLayout().structures()){
            int x=structure.x(),y=structure.y();GameObject object=level.getObject(x,y);
            if(d.generatorVersion>=3&&structure.variant()==0){
                require(!level.getTile(x-3,y+3).isLiquid&&!level.getTile(x+3,y+3).isLiquid,"Outpost utility placed in water");
                require(level.getObject(x-3,y+3).getStringID().equals("workstation"),"Outpost workstation missing");
                require(level.getObject(x+3,y+3).getStringID().equals("torch"),"Outpost torch missing");
            }
            require(object.getStringID().equals(structure.id()),"Missing live structure: "+structure.id());
            var multi=object.getMultiTile(0);require(multi.isMaster&&multi.width==3&&multi.height==2&&multi.checkValid()==null,"Invalid native multipart structure");
            require(storage(level,x,y).getInventory().getSize()==20,"Wrong native outpost inventory size");
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=0;dy++){
                GameObject part=level.getObject(x+dx,y+dy);
                require(part.getMultiTile(0).getMasterObject().getID()==object.getID(),"Wrong multipart ownership");
                require(part.isSolid&&!part.getCollisions(level,x+dx,y+dy,0).isEmpty(),"Missing multipart collision");collisionParts++;
                require(!layout.path(x+dx,y+dy),"Native structure blocks generated road");
            }
        }
        out.put("terrain",terrain);out.put("nativeOutpostInventories",layout.biomeLayout().structures().size());out.put("nativeCollisionParts",collisionParts);
        if(loaded){
            require(level.gndData.getBoolean(PREFIX+"Checkpoint"),"Biome disk checkpoint missing");
            int x=level.gndData.getInt(PREFIX+"ChestX"),y=level.gndData.getInt(PREFIX+"ChestY");
            InventoryItem item=storage(level,x,y).getInventory().getItem(19);
            require(item!=null&&item.item.getStringID().equals("coin")&&item.getAmount()==MARKER_COINS,"Outpost contents not restored from disk");
            int hx=level.gndData.getInt(PREFIX+"HarvestX"),hy=level.gndData.getInt(PREFIX+"HarvestY");
            require(level.getObjectID(hx,hy)==0,"Harvested biome flora regenerated on load");
            out.put("diskOutpostStorageReadBack",true);out.put("diskHarvestRemovalReadBack",true);
            out.put("harvestedObject",level.gndData.getString(PREFIX+"HarvestId",""));out.put("harvestedTile",List.of(hx,hy));
        }else{
            require(!level.gndData.getBoolean(PREFIX+"Checkpoint"),"Biome probe must run once in fresh disposable world");
            var structure=layout.biomeLayout().structures().get(0);InventoryObjectEntity chest=storage(level,structure.x(),structure.y());
            require(chest.getInventory().getItem(19)==null,"Refusing to overwrite occupied probe slot");
            chest.getInventory().setItem(19,new InventoryItem("coin",MARKER_COINS));
            level.gndData.setInt(PREFIX+"ChestX",structure.x()).setInt(PREFIX+"ChestY",structure.y());
            verifyNativeInteraction(client,level,structure.x(),structure.y());out.put("nativeOutpostInteractionAvailable",true);
            out.put("outpostUiScope","Server interaction eligibility only. Remote client UI is not opened before its destination regions have streamed.");
            Point harvested=null;String harvestedId=null;ObjectDamageResult result=null;
            search:for(int y=3;y<d.height-3;y++)for(int x=3;x<d.width-3;x++){
                GameObject object=level.getObject(x,y);String id=object.getStringID();
                if(!id.startsWith("mlbiome_")||!(id.contains("_flora")||id.endsWith("_tree")))continue;
                result=level.entityManager.doObjectDamage(0,x,y,object.objectHealth+1,object.toolTier,client.playerMob,client,false,x*32+16,y*32+16);
                require(result!=null&&result.destroyed&&result.addedDamage>0,"Native biome object damage failed");
                require(level.getObjectID(x,y)==0,"Native damage did not remove flora");
                require(result.itemsDropped!=null&&!result.itemsDropped.isEmpty(),"Biome harvest dropped no native items");
                harvested=new Point(x,y);harvestedId=id;break search;
            }
            require(harvested!=null,"No collectable biome flora in native map");
            level.gndData.setInt(PREFIX+"HarvestX",harvested.x).setInt(PREFIX+"HarvestY",harvested.y).setString(PREFIX+"HarvestId",harvestedId);
            out.put("harvestedObject",harvestedId);out.put("harvestedTile",List.of(harvested.x,harvested.y));
            out.put("nativeDamage",result.addedDamage);out.put("nativeHarvestDrops",result.itemsDropped.stream().map(i->i.item.item.getStringID()+":"+i.item.getAmount()).toList());
            out.put("harvestMethod","Native EntityManager.doObjectDamage, with damage and loot result; does not simulate physical mouse/tool animation.");
        }
        Map<String,Object> population=new LinkedHashMap<>();
        for(var species:BiomeCreatures.SPECIES){
            require(level.gndData.getBoolean("mosslumeInitialFaunaV1"+species.id()),"Initial species was never seeded: "+species.id());
            var tracked=level.entityManager.mobs.stream().filter(m->!m.removed()&&!m.canDespawn&&m.getStringID().equals(species.id())).toList();
            require(tracked.size()==1,"Initial persistent species duplicated/missing: "+species.id()+" count="+tracked.size());
            int identity=tracked.get(0).getUniqueID();String key=PREFIX+"Mob_"+species.id();
            if(loaded)require(level.gndData.getInt(key)==identity,"Initial species identity changed after reload: "+species.id());
            else level.gndData.setInt(key,identity);
            population.put(species.id(),Map.of("persistentCount",tracked.size(),"uniqueID",identity));
        }
        out.put("persistentPopulation",population);out.put("populationDuplicateCheck",loaded?"same21 persistent identities read from disk; one per species":"21 persistent identities checkpointed; one per species");
        out.put("creatureDefinitions",BiomeCreatureChecks.verifyDefinitions(level));out.put("liveCreatureDiagnostics",BiomeCreatures.diagnostics(level));
        if(!loaded)level.gndData.setBoolean(PREFIX+"Checkpoint",true);
        out.put("loaded",loaded);out.put("scope","Disposable native world API and disk reload checks. Physical input, perception of art and frame time are separate visual QA.");return out;
    }
    private static void verifyNativeInteraction(ServerClient client,Level level,int x,int y){
        float oldX=client.playerMob.x,oldY=client.playerMob.y;
        try{
            client.playerMob.setPos(x*32+16,(y+1)*32+16,true);
            GameObject object=level.getObject(x,y);require(object.canInteract(level,x,y,client.playerMob),"Outpost interaction unavailable");
        }finally{client.playerMob.setPos(oldX,oldY,true);}
    }
    private static InventoryObjectEntity storage(Level level,int x,int y){var entity=level.entityManager.getObjectEntity(x,y);require(entity instanceof InventoryObjectEntity,"Missing native outpost inventory at "+x+","+y);return (InventoryObjectEntity)entity;}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private BiomeWorldChecks(){}
}
