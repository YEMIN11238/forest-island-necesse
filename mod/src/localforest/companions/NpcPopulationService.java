package localforest.companions;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import localforest.scene.SceneStructures;
import localforest.world.WorldDefinition;

/** Server-only, local and idempotent population seeding. Does not load any other region. */
public final class NpcPopulationService {
    private static final String PREFIX="mosslumePopulationV2_";
    private static final java.util.Map<Level,java.util.Map<String,String>> WAITING=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private NpcPopulationService(){}
    public static void ensureInitialPopulation(Level level,int campX,int campY){
        if(level==null||!level.isServer())return;
        level.gndData.setInt(PREFIX+"campX",campX);level.gndData.setInt(PREFIX+"campY",campY);
        level.gndData.setBoolean(PREFIX+"camp",true);
        var settlements=SettlementsWorldData.getSettlementsData(level);
        ServerSettlementData camp=settlements.getOrCreateServerData(level,campX+2,campY-4);
        // Each district contains at most three residents. A native settlement can cover several
        // districts; being housed there does not automatically grant our model companion tools.
        for(int district=0;district<=3;district++){
            final int group=district;
            List<NpcDefinition> definitions=NpcDefinition.INITIAL.stream().filter(d->d.district()==group).toList();
            String hutKey=PREFIX+"hut"+district;
            if(district>0&&level.gndData.getBoolean(hutKey)){
                int hx=level.gndData.getInt(hutKey+"x"),hy=level.gndData.getInt(hutKey+"y");
                SceneStructures.registerResidence(level,"npc-district-"+district,hx,hy,7,6);
                ServerSettlementData housing=district==1?camp:settlements.getOrCreateServerData(level,hx+3,hy+4);
                NpcLifeSupplies.residence(level,housing,district,hx,hy);
            }
            List<NpcDefinition> missing=new ArrayList<>();
            for(var def:definitions){
                HumanShop adopted=findPresent(level,def);
                if(adopted!=null){bind(level,adopted,def,campX,campY);continue;}
                if(!level.gndData.getBoolean(PREFIX+def.id()))missing.add(def);
            }
            if(missing.isEmpty())continue;
            if(district==0){
                // The world adapter normally made these two and their beds before calling us.
                for(var def:missing)spawn(level,def,camp,campX,campY,null);
                continue;
            }
            var world=WorldDefinition.forLevel(level);boolean continuous=world!=null&&world.isContinuous();
            int wantedX=campX+(continuous?(district==1?-11:district==2?4:-17):(district==1?-12:district==2?17:-22));
            int wantedY=campY+(continuous?(district==1?-13:district==2?-22:-26):(district==1?5:-12));
            Point hut=rememberedOrCreateHut(level,district,wantedX,wantedY);
            if(hut==null){
                waitFor(level,"district-"+district,"No legal loaded-land residence near "+wantedX+","+wantedY+"; desired footprint: "+hutBlocker(level,wantedX,wantedY));
                continue; // Keep the spawn flags unset: retry, never destroy an occupied house or place on water.
            }
            clearWait(level,"district-"+district);
            SceneStructures.registerResidence(level,"npc-district-"+district,hut.x,hut.y,7,6);
            ServerSettlementData districtSettlement=district==1?camp:settlements.getOrCreateServerData(level,hut.x+3,hut.y+4);
            if(district>1&&districtSettlement.uniqueID!=camp.uniqueID){districtSettlement.networkData.markPreventDisbanding();districtSettlement.networkData.setName(new StaticMessage(district==2?"海边工坊":"林边住处"));}
            NpcLifeSupplies.residence(level,districtSettlement,district,hut.x,hut.y);
            for(var def:missing){
                int slot=definitions.indexOf(def);
                SettlementBed bed=districtSettlement.addOrValidateBed(hut.x+1+slot*2,hut.y+2);
                spawn(level,def,districtSettlement,campX,campY,bed);
            }
        }
        level.gndData.setBoolean(PREFIX+"complete",NpcDefinition.INITIAL.stream().allMatch(d->level.gndData.getBoolean(PREFIX+d.id())));
    }
    private static HumanShop findPresent(Level level,NpcDefinition def){
        return level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed()).map(m->(HumanShop)m)
            .filter(m->NativeNpcIdentity.template(m).equals(def.id())||NativeNpcIdentity.template(m).isEmpty()
                &&m.getStringID().equals(def.nativeType())&&def.name().equals(m.getSettlerName())).findFirst().orElse(null);
    }
    private static void bind(Level level,HumanShop mob,NpcDefinition def,int x,int y){
        if(NativeNpcIdentity.get(mob).isEmpty())NativeNpcIdentity.bind(mob,def.tier()==NpcDefinition.Tier.VISITOR?
            UUID.nameUUIDFromBytes(("mosslume:visitor:"+level.getIdentifier()+":"+def.id()).getBytes(StandardCharsets.UTF_8)).toString():UUID.randomUUID().toString());
        NativeNpcIdentity.bindTemplate(mob,def.id(),x,y);level.gndData.setBoolean(PREFIX+def.id(),true);
        NpcLifeSupplies.ration(level,mob,def);
    }
    private static void spawn(Level level,NpcDefinition def,ServerSettlementData settlement,int campX,int campY,SettlementBed bed){
        HumanShop mob=(HumanShop)MobRegistry.getMob(def.nativeType(),level);
        var world=WorldDefinition.forLevel(level);
        Point origin=world==null?new Point(campX,campY+3):new Point(world.spawnX,world.spawnY);
        Point point=PopulationSpawnSearch.connectedNear(origin,new Point(campX+def.workX(),campY+def.workY()),40,
            (x,y)->level.isTileWithinBounds(x,y)&&level.regionManager.isTileLoaded(x,y)&&!level.isLiquidTile(x,y)&&!level.isSolidTile(x,y),
            (x,y)->!mob.collidesWith(level,x*32+16,y*32+16)&&level.entityManager.mobs.stream().noneMatch(other->!other.removed()
                &&Math.abs(other.x-(x*32+16))<24&&Math.abs(other.y-(y*32+16))<24));
        if(point==null){waitFor(level,def.id(),"No dry collision-free spawn connected to camp in loaded regions");return;}
        mob.canDespawn=false;mob.setSettlerName(def.name());
        mob.onSpawned(point.x*32+16,point.y*32+16);level.entityManager.addMob(mob,point.x*32+16,point.y*32+16);
        // Some native professions randomize their name while initializing their appearance.
        mob.setSettlerName(def.name());
        // Stable identity is saved on the native NPC, not inferred from a reused numeric mob id.
        bind(level,mob,def,campX,campY);
        if(def.tier()!=NpcDefinition.Tier.VISITOR){
            LevelSettler settler=new LevelSettler(settlement,mob);
            if(bed!=null)settlement.moveSettler(settler,bed,null);else settlement.moveIn(settler);
            mob.onRecruited(null,settlement,settler);mob.updateHappiness();
        }
        NpcLifeSupplies.ration(level,mob,def);
        mob.setSettlerName(def.name());mob.markDirty();
        clearWait(level,def.id());
        System.out.println("MOSSLUME_POPULATION_ADDED template="+def.id()+" tier="+def.tier()+" mob="+mob.getUniqueID());
    }
    private static Point rememberedOrCreateHut(Level level,int district,int desiredX,int desiredY){
        String key=PREFIX+"hut"+district;
        if(level.gndData.getBoolean(key))return new Point(level.gndData.getInt(key+"x"),level.gndData.getInt(key+"y"));
        for(int radius=0;radius<=14;radius+=2)for(int dy=-radius;dy<=radius;dy+=2)for(int dx=-radius;dx<=radius;dx+=2){
            if(radius>0&&Math.abs(dx)!=radius&&Math.abs(dy)!=radius)continue;
            int x=desiredX+dx,y=desiredY+dy;if(!canPlaceHut(level,x,y))continue;
            int floor=TileRegistry.getTileID("woodfloor"),wall=ObjectRegistry.getObjectID("woodwall");
            for(int iy=0;iy<6;iy++)for(int ix=0;ix<7;ix++){
                level.setObject(x+ix,y+iy,0);level.setTile(x+ix,y+iy,floor);
                if(ix==0||ix==6||iy==0||iy==5)level.setObject(x+ix,y+iy,wall);
            }
            ObjectRegistry.getObject("wooddoor").placeObject(level,x+3,y+5,0,false);
            for(int i=0;i<3;i++)ObjectRegistry.getObject("oakbed").placeObject(level,x+1+i*2,y+2,0,false);
            level.gndData.setInt(key+"x",x);level.gndData.setInt(key+"y",y);level.gndData.setBoolean(key,true);return new Point(x,y);
        }
        return null;
    }
    private static boolean canPlaceHut(Level level,int x,int y){return hutBlocker(level,x,y).isEmpty();}
    private static String hutBlocker(Level level,int x,int y){
        for(int dy=-1;dy<=6;dy++)for(int dx=-1;dx<=7;dx++){
            int tx=x+dx,ty=y+dy;
            if(!level.isTileWithinBounds(tx,ty))return "bounds@"+tx+","+ty;
            if(!level.regionManager.isTileLoaded(tx,ty))return "unloaded@"+tx+","+ty;
            if(level.isLiquidTile(tx,ty))return "water@"+tx+","+ty;
            var object=level.getObject(tx,ty);
            String id=object.getStringID();
            boolean generatedPlant=id!=null&&id.startsWith("mlbiome_")&&(id.endsWith("_tree")||id.matches("mlbiome_[a-z]+_flora[0-8]"));
            if(level.getObjectID(tx,ty)!=0&&!object.isGrass&&!object.isTree&&!generatedPlant)return "object:"+id+"@"+tx+","+ty;
            if(level.entityManager.mobs.stream().anyMatch(m->!m.removed()&&m.getTileX()==tx&&m.getTileY()==ty))return "mob@"+tx+","+ty;
        }
        return "";
    }
    private static void waitFor(Level level,String key,String reason){
        var waiting=WAITING.computeIfAbsent(level,ignored->new java.util.LinkedHashMap<>());
        if(!reason.equals(waiting.put(key,reason)))System.out.println("MOSSLUME_POPULATION_WAIT target="+key+" reason="+reason);
    }
    private static void clearWait(Level level,String key){var waiting=WAITING.get(level);if(waiting!=null)waiting.remove(key);}
    static Point walkableNear(Level level,int x,int y,int radius){
        for(int r=0;r<=radius;r++)for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++){
            if(r>0&&Math.abs(dx)!=r&&Math.abs(dy)!=r)continue;
            int tx=x+dx,ty=y+dy;if(level.isTileWithinBounds(tx,ty)&&level.regionManager.isTileLoaded(tx,ty)&&!level.isLiquidTile(tx,ty)&&!level.isSolidTile(tx,ty))return new Point(tx,ty);
        }return null;
    }
    /** World-time events, not background model polling. One native visitor per two world days. */
    public static void tickVisitors(Level level){
        if(level==null||!level.isServer()||!level.gndData.getBoolean(PREFIX+"camp"))return;
        if(!level.gndData.getBoolean(PREFIX+"complete")||!lifeReady(level))
            ensureInitialPopulation(level,level.gndData.getInt(PREFIX+"campX"),level.gndData.getInt(PREFIX+"campY"));
        int day=level.getWorldEntity().getDay();if(day<1||level.getWorldEntity().isNight())return;
        int cycle=day/2+1;String key=PREFIX+"visitCycle";if(level.gndData.getInt(key,0)>=cycle)return;
        long present=level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed())
            .map(m->(HumanShop)m).filter(m->{var d=NpcDefinition.find(NativeNpcIdentity.template(m));return d!=null&&d.tier()==NpcDefinition.Tier.VISITOR;}).count();
        if(present>0)return;
        int cx=level.gndData.getInt(PREFIX+"campX"),cy=level.gndData.getInt(PREFIX+"campY");
        var def=NpcDefinition.VISITORS.get(Math.floorMod(cycle,NpcDefinition.VISITORS.size()));
        var settlement=SettlementsWorldData.getSettlementsData(level).getOrCreateServerData(level,cx+2,cy-4);
        Point point=walkableNear(level,cx+def.workX(),cy+def.workY(),10);if(point==null)return;
        HumanShop mob=(HumanShop)MobRegistry.getMob(def.nativeType(),level);
        mob.setSettlerName(def.name());mob.canDespawn=false;mob.onSpawned(point.x*32+16,point.y*32+16);
        level.entityManager.addMob(mob,point.x*32+16,point.y*32+16);bind(level,mob,def,cx,cy);
        mob.setSettlerName(def.name());
        // startVisitor can relocate using native entry selection. Its native travel-out timer remains.
        mob.startVisitor(settlement);mob.setSettlerName(def.name());mob.markDirty();level.gndData.setInt(key,cycle);
    }
    private static boolean lifeReady(Level level){return lifeReadyData(level.gndData);}
    static boolean lifeReadyData(necesse.engine.network.gameNetworkData.GNDItemMap data){
        for(var def:NpcDefinition.INITIAL)if(!data.getBoolean(NpcLifeSupplies.PREFIX+"ration_"+def.id()))return false;
        for(int district=1;district<=3;district++)for(var furniture:NpcLifeSupplies.INTERIOR)
            if(!data.getBoolean(NpcLifeSupplies.PREFIX+district+"_"+furniture.key()))return false;
        for(int district=1;district<=3;district++)if(!data.getBoolean(NpcLifeSupplies.PREFIX+district+"_pantryStock")
            ||!data.getBoolean(NpcLifeSupplies.PREFIX+district+"_storageAssigned"))return false;
        return true;
    }
    public static java.util.Map<String,Object> counts(Level level){
        long core=0,residents=0,visitors=0;
        for(var mob:level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed()).map(m->(HumanShop)m).toList()){
            var definition=NpcDefinition.find(NativeNpcIdentity.template(mob));if(definition==null)continue;
            switch(definition.tier()){case CORE->core++;case RESIDENT->residents++;case VISITOR->visitors++;}
        }
        return java.util.Map.of("core",core,"residents",residents,"visitors",visitors,"seedingComplete",level.gndData.getBoolean(PREFIX+"complete"));
    }
    public static java.util.Map<String,Object> populationSummary(Level level){
        int core=0,residents=0;
        for(var d:NpcDefinition.INITIAL)if(level.gndData.getBoolean(PREFIX+d.id())){if(d.tier()==NpcDefinition.Tier.CORE)core++;else residents++;}
        var loaded=counts(level);
        return java.util.Map.of("seededCore",core,"seededResidents",residents,"loadedCore",loaded.get("core"),
            "loadedResidents",loaded.get("residents"),"loadedVisitors",loaded.get("visitors"),"seedingComplete",loaded.get("seedingComplete"),
            "pendingTemplates",NpcDefinition.INITIAL.stream().filter(d->!level.gndData.getBoolean(PREFIX+d.id())).map(NpcDefinition::id).toList(),
            "waiting",java.util.Map.copyOf(WAITING.getOrDefault(level,java.util.Map.of())));
    }
    /** Read-only evidence for a natural-running QA session; no needs, jobs or inventories are changed. */
    public static java.util.Map<String,Object> lifeSummary(Level level){
        List<java.util.Map<String,Object>> dining=new ArrayList<>();int chairs=0,rations=0;
        var settlements=SettlementsWorldData.getSettlementsData(level);
        for(var def:NpcDefinition.INITIAL)if(level.gndData.getBoolean(NpcLifeSupplies.PREFIX+"ration_"+def.id()))rations++;
        for(int district=1;district<=3;district++){
            String hut=PREFIX+"hut"+district;if(!level.gndData.getBoolean(hut))continue;
            int left=level.gndData.getInt(hut+"x"),top=level.gndData.getInt(hut+"y");
            for(var furniture:NpcLifeSupplies.INTERIOR){
                int x=left+furniture.x(),y=top+furniture.y();if(!level.regionManager.isTileLoaded(x,y))continue;
                if(furniture.object().equals("oakchair")&&level.getObject(x,y).getStringID().equals("oakchair"))chairs++;
                if(!furniture.key().equals("pantry"))continue;
                int food=0,nutrition=0;
                if(level.entityManager.getObjectEntity(x,y) instanceof necesse.entity.objectEntity.InventoryObjectEntity chest){
                    var inventory=chest.getInventory();
                    for(int slot=0;slot<inventory.getSize();slot++){
                        var item=inventory.getItem(slot);
                        if(item!=null&&item.item instanceof necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem edible){
                            food+=item.getAmount();nutrition+=edible.nutrition*item.getAmount();
                        }
                    }
                }
                var settlement=settlements.getServerDataAtTile(level.getIdentifier(),x,y);
                dining.add(java.util.Map.of("district",district,"tile",List.of(x,y),"foodItems",food,"nutrition",nutrition,
                    "nativeStorageRegistered",settlement!=null&&settlement.storageManager.hasInventoryAtTile(x,y),
                    "suppliedOnce",level.gndData.getBoolean(NpcLifeSupplies.PREFIX+district+"_pantryStock")));
            }
        }
        var people=level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed()).map(m->(HumanShop)m)
            .filter(m->NpcDefinition.find(NativeNpcIdentity.template(m))!=null).map(m->{
                java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();row.put("template",NativeNpcIdentity.template(m));
                row.put("tile",List.of(m.getTileX(),m.getTileY()));row.put("hunger",m.getHungerLevel());row.put("recreation",m.getRecreationLevel());
                row.put("nativeJob",m.hasActiveJob());row.put("guarded",m.commandGuardPoint!=null);row.put("objectUse",m.objectUser!=null);
                row.put("lastFoodEaten",m.lastFoodEaten==null?"":m.lastFoodEaten.getStringID());
                row.put("carriedFood",m.getWorkInventory().stream().filter(i->i.item.isFoodItem()).mapToInt(i->i.getAmount()).sum());return row;
            }).toList();
        return java.util.Map.of("rationsIssuedOnce",rations,"nativeChairsLoaded",chairs,"pantries",dining,"people",people,
            "scope","Actual loaded native inventory/need/job/object state; finite initial supply, no synthetic hunger or job success.");
    }
}
