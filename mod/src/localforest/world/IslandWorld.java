package localforest.world;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.GameEvents;
import necesse.engine.GameEventListener;
import necesse.engine.GameLaunch;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.LevelRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldGenerator;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;

public final class IslandWorld {
 private static int coastID;
 private static final List<Placement> placements=new ArrayList<>();
 public static boolean enabled(){return Boolean.getBoolean("localforest.islandprofile")||(GameLaunch.launchOptions!=null&&GameLaunch.launchOptions.containsKey("islandprofile"));}
 public static boolean isIsland(Level l){return l instanceof IslandLevel||(l!=null&&l.gndData.getBoolean("localforestisland"));}
 public static void init(){
  LevelRegistry.registerLevel("localforestisland",IslandLevel.class);
  coastID=ObjectRegistry.registerObject("lfcoastbarrier",new CoastBarrier(),-1.0f,false);
  WorldGenerator.registerGenerator(new WorldGenerator(){
   @Override public Level getNewLevel(LevelIdentifier id,Server server,GameBlackboard board){
    if(enabled()&&LevelIdentifier.SURFACE_IDENTIFIER.equals(id))return new IslandLevel(id,IslandLayout.WIDTH,IslandLayout.HEIGHT,server.world.worldEntity);
    return null;
   }
  });
  GameEvents.addListener(ServerClientConnectedEvent.class,new GameEventListener<ServerClientConnectedEvent>(){
   @Override public void onEvent(ServerClientConnectedEvent e){onConnected(e.client);}
  });
 }
 public static void postInit(){buildPlacements();}
 public static void initializeNewWorld(WorldEntity world){
  world.spawnLevelIdentifier=LevelIdentifier.SURFACE_IDENTIFIER;
  world.spawnTile=new Point(IslandLayout.SPAWN_X,IslandLayout.SPAWN_Y);
  world.defaultSpawnTile=new Point(world.spawnTile);
  world.setWorldTime((long)(world.hourToDayTime(21f)*1000f));
  Level l=world.serverWorld.getLevel(world.spawnLevelIdentifier);
  if(!isIsland(l))throw new IllegalStateException("Expected registered forest island level, got "+l.getClass());
  l.regionManager.ensureTilesAreLoaded(0,0,IslandLayout.WIDTH-1,IslandLayout.HEIGHT-1);
  addResidents(l);
  System.out.println("LOCALFOREST_WORLD_READY level="+l.getStringID()+" spawn="+world.spawnTile+" tileWidth="+l.tileWidth+" tileHeight="+l.tileHeight);
 }
 private static void onConnected(ServerClient c){
  if(!enabled())return;
  Level l=c.getLevel();if(!isIsland(l))return;
  c.spawnLevelIdentifier=LevelIdentifier.SURFACE_IDENTIFIER;
  c.spawnTile=new Point(IslandLayout.SPAWN_X,IslandLayout.SPAWN_Y);
  ServerSettlementData data=SettlementsWorldData.getSettlementsData(l).getOrCreateServerData(l,51,36);
  data.networkData.setOwner(c);data.networkData.setName(new StaticMessage("林间小岛"));
  if(c.playerMob!=null&&!IslandLayout.isLand(c.playerMob.getTileX(),c.playerMob.getTileY())){
   c.playerMob.setPos(IslandLayout.SPAWN_X*32+16,IslandLayout.SPAWN_Y*32+16,true);
  }
 }
 private static void addResidents(Level l){
  if(l.gndData.getBoolean("lfResidentsSeeded"))return;
  ServerSettlementData settlement=SettlementsWorldData.getSettlementsData(l).getOrCreateServerData(l,51,36);
  settlement.networkData.markPreventDisbanding();settlement.networkData.setName(new StaticMessage("林间小岛"));
  String[] types={"elderhuman","farmerhuman"};String[] names={"林叔","小禾"};
  for(int i=0;i<types.length;i++){
   HumanMob mob=(HumanMob)MobRegistry.getMob(types[i],l);
   mob.canDespawn=false;mob.setSettlerName(names[i]);
   int x=IslandLayout.OFFSET_X+23+i*3,y=IslandLayout.OFFSET_Y+24;
   mob.onSpawned(x*32+16,y*32+16);l.entityManager.addMob(mob,x*32+16,y*32+16);
   LevelSettler settler=new LevelSettler(settlement,mob);
   SettlementBed bed=settlement.addOrValidateBed(IslandLayout.OFFSET_X+29+i*2,IslandLayout.OFFSET_Y+29);
   if(bed!=null)settlement.moveSettler(settler,bed,null);else settlement.moveIn(settler);
  }
  l.gndData.setBoolean("lfResidentsSeeded",true);
 }
 static void populateRegion(Level l,Region r){
  for(int x=r.tileXOffset;x<r.tileXOffset+r.tileWidth;x++)for(int y=r.tileYOffset;y<r.tileYOffset+r.tileHeight;y++){
   if(IslandLayout.isCoastBarrier(x,y))l.setObject(x,y,coastID);
  }
  for(Placement p:placements){
   int x=p.x+IslandLayout.OFFSET_X,y=p.y+IslandLayout.OFFSET_Y;
   if(x<r.tileXOffset||y<r.tileYOffset||x>=r.tileXOffset+r.tileWidth||y>=r.tileYOffset+r.tileHeight)continue;
   GameObject o=ObjectRegistry.getObject(p.id);
   o.placeObject(l,x,y,p.rotation,false);
   ObjectEntity oe=l.entityManager.getObjectEntity(x,y);
   if(oe instanceof FueledInventoryObjectEntity){
    FueledInventoryObjectEntity fuel=(FueledInventoryObjectEntity)oe;
    fuel.inventory.setItem(0,new InventoryItem("oaklog",250));fuel.keepRunning=true;fuel.useFuel();
   }else if(oe instanceof InventoryObjectEntity&&p.id.equals("oakchest")){
    Inventory inv=((InventoryObjectEntity)oe).getInventory();
    String[] ids={"oaklog","stone","wheatseed","woodpickaxe","woodaxe","woodshovel","torch","oakchest","woodwall","workstationduo","apple"};
    int[] amounts={120,90,20,1,1,1,15,2,20,1,12};
    int slot=0;for(int i=0;i<ids.length&&slot<inv.getSize();i++){
     if(necesse.engine.registries.ItemRegistry.getItemID(ids[i])>=0)inv.setItem(slot++,new InventoryItem(ids[i],amounts[i]));
    }
   }
  }
 }
 private static void put(int x,int y,String id){put(x,y,id,0);}
 private static void put(int x,int y,String id,int rotation){
  placements.removeIf(p->p.x==x&&p.y==y);placements.add(new Placement(x,y,id,rotation));
 }
 private static void buildPlacements(){
  placements.clear();
  String[] trees={"oaktree","sprucetree","birchtree"};int n=0;
  for(int[] p:IslandLayout.TREES)if(IslandLayout.isSourceLand(p[0],p[1])&&!IslandLayout.isHouse(p[0],p[1]))put(p[0],p[1],trees[n++%trees.length]);
  for(int[] p:IslandLayout.BUSHES)if(IslandLayout.isSourceLand(p[0],p[1])&&!IslandLayout.isHouse(p[0],p[1]))put(p[0],p[1],(p[0]+p[1])%3==0?"raspberrybush":"blueberrybush");
  for(int[] p:IslandLayout.GRASS)if(IslandLayout.isSourceLand(p[0],p[1])&&!IslandLayout.isHouse(p[0],p[1]))put(p[0],p[1],"grass");
  for(int[] p:IslandLayout.FLOWERS)if(IslandLayout.isSourceLand(p[0],p[1])&&!IslandLayout.isHouse(p[0],p[1]))put(p[0],p[1],(p[0]%2==0)?"yellowflowerpatch":"redflowerpatch");
  for(int[] p:IslandLayout.ROCKS)if(IslandLayout.isSourceLand(p[0],p[1])&&!IslandLayout.isHouse(p[0],p[1]))put(p[0],p[1],"surfacerocksmall");
  int[][] chests={{18,23},{13,22},{18,22},{26,21},{33,22},{29,12}};
  for(int[] p:chests)put(p[0],p[1],"oakchest");
  put(17,23,"workstation");put(16,24,"ironanvil");put(25,29,"forge");
  put(15,23,"cookingpot");put(25,21,"cookingpot");
  put(25,22,"campfire");put(24,27,"campfire");put(17,22,"torch");put(12,23,"torch");put(36,23,"torch");
  put(29,24,"settlementflag");
  for(int y=29;y<=31;y++)for(int x=17;x<=19;x++)put(x,y,"wheatseed"+(y<31?"4":""));
  for(int y=27;y<=30;y++)for(int x=28;x<=32;x++){
   final int px=x,py=y;placements.removeIf(p->p.x==px&&p.y==py);
  }
  for(int x=28;x<=32;x++){put(x,27,"woodwall");put(x,30,"woodwall");}
  for(int y=28;y<30;y++){put(28,y,"woodwall");put(32,y,"woodwall");}
  put(30,30,"wooddoor",2);put(29,29,"oakbed");put(31,29,"oakbed");
 }
 private static final class Placement {final int x,y,rotation;final String id;Placement(int x,int y,String id,int r){this.x=x;this.y=y;this.id=id;this.rotation=r;}}
 private IslandWorld(){}
}
