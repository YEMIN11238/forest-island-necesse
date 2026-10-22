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
 public static boolean isIsland(Level l){return l instanceof IslandLevel||l instanceof GeneratedWorldLevel||(l!=null&&l.gndData.getBoolean("localforestisland"));}
 public static void init(){
  LevelRegistry.registerLevel("localforestisland",IslandLevel.class);
  LevelRegistry.registerLevel("mosslumegenerated",GeneratedWorldLevel.class);
  NaturalPathWorld.init();
  coastID=ObjectRegistry.registerObject("lfcoastbarrier",new CoastBarrier(),-1.0f,false);
  WorldGenerator.registerGenerator(new WorldGenerator(){
   @Override public Level getNewLevel(LevelIdentifier id,Server server,GameBlackboard board){
    if(!enabled())return null;
    WorldDefinition definition=WorldDefinition.forWorld(server.world.worldEntity);
    if(definition!=null&&(LevelIdentifier.SURFACE_IDENTIFIER.equals(id)||LevelIdentifier.CAVE_IDENTIFIER.equals(id)))
      return new GeneratedWorldLevel(id,definition.width,definition.height,server.world.worldEntity);
    if(definition==null&&LevelIdentifier.SURFACE_IDENTIFIER.equals(id))return new IslandLevel(id,IslandLayout.WIDTH,IslandLayout.HEIGHT,server.world.worldEntity);
    return null;
   }
  });
  GameEvents.addListener(ServerClientConnectedEvent.class,new GameEventListener<ServerClientConnectedEvent>(){
   @Override public void onEvent(ServerClientConnectedEvent e){onConnected(e.client);}
  });
 }
 public static void postInit(){buildPlacements();}
 public static void initializeNewWorld(WorldEntity world){
  WorldDefinition definition=WorldDefinition.forWorld(world);
  if(definition!=null){initializeGeneratedWorld(world,definition);return;}
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
  WorldDefinition definition=WorldDefinition.forLevel(l);
  if(definition!=null){connectGenerated(c,l,definition);return;}
  NaturalPathWorld.migrate(l);
  preserveOrRepairSpawn(c,null);
  ServerSettlementData data=SettlementsWorldData.getSettlementsData(l).getOrCreateServerData(l,51,36);
  data.networkData.setOwner(c);
  if(!data.networkData.isSettlementNameSet())data.networkData.setName(new StaticMessage("林间小岛"));
  if(c.playerMob!=null&&!IslandLayout.isLand(c.playerMob.getTileX(),c.playerMob.getTileY())){
   c.playerMob.setPos(IslandLayout.SPAWN_X*32+16,IslandLayout.SPAWN_Y*32+16,true);
  }
 }
 private static void initializeGeneratedWorld(WorldEntity world,WorldDefinition d){
  world.spawnLevelIdentifier=LevelIdentifier.SURFACE_IDENTIFIER;
  world.spawnTile=new Point(d.spawnX,d.spawnY);world.defaultSpawnTile=new Point(world.spawnTile);
  world.setWorldTime((long)(world.hourToDayTime(8f)*1000f));
  world.serverWorld.settings.canSettlersDie=false;
  world.serverWorld.settings.playerHunger=true;
  world.serverWorld.settings.difficulty=d.rules.equals("relaxed")?necesse.engine.GameDifficulty.CASUAL:necesse.engine.GameDifficulty.CLASSIC;
  world.serverWorld.settings.saveSettings();
  Level level=world.serverWorld.getLevel(world.spawnLevelIdentifier);
  level.regionManager.ensureTilesAreLoaded(d.campX-48,d.campY-48,d.campX+56,d.campY+48);
  addGeneratedResidents(level,d);
  System.out.println("MOSSLUME_GENERATED_WORLD_READY preset="+d.preset+" userSeed="+d.userSeed+" generator="+d.generatorVersion+" content="+d.contentVersion+" dimensions="+d.width+"x"+d.height);
 }
 private static void connectGenerated(ServerClient client,Level level,WorldDefinition d){
  preserveOrRepairSpawn(client,d);
  if(level.isSurfaceLevel()){
   ServerSettlementData settlement=SettlementsWorldData.getSettlementsData(level).getOrCreateServerData(level,d.campX+2,d.campY-4);
   settlement.networkData.setOwner(client);
   if(!settlement.networkData.isSettlementNameSet())settlement.networkData.setName(new StaticMessage("苔光营地"));
   completeInitialRecruitment(level,settlement);
   if(client.playerMob!=null&&(!level.isTileWithinBounds(client.playerMob.getTileX(),client.playerMob.getTileY())||level.getTile(client.playerMob.getTileX(),client.playerMob.getTileY()).isLiquid))
    client.playerMob.setPos(d.spawnX*32+16,d.spawnY*32+16,true);
  }
 }
 /** Connection happens after native player save loading. Preserve its bed/respawn target;
  * only repair missing, out-of-bounds or unsupported-level data. Native respawn validation
  * still checks whether the selected bed exists and permits respawning when it is used.
  */
 private static void preserveOrRepairSpawn(ServerClient client,WorldDefinition d){
  boolean allowed=LevelIdentifier.SURFACE_IDENTIFIER.equals(client.spawnLevelIdentifier)
   ||d!=null&&LevelIdentifier.CAVE_IDENTIFIER.equals(client.spawnLevelIdentifier);
  int width=d==null?IslandLayout.WIDTH:d.width,height=d==null?IslandLayout.HEIGHT:d.height;
  Point tile=client.spawnTile;
  if(allowed&&tile!=null&&tile.x>=0&&tile.y>=0&&tile.x<width&&tile.y<height)return;
  client.spawnLevelIdentifier=LevelIdentifier.SURFACE_IDENTIFIER;
  client.spawnTile=d==null?new Point(IslandLayout.SPAWN_X,IslandLayout.SPAWN_Y):new Point(d.spawnX,d.spawnY);
 }
 private static void addGeneratedResidents(Level level,WorldDefinition d){
  if(level.gndData.getBoolean("mosslumeResidentsSeededV1"))return;
  ServerSettlementData settlement=SettlementsWorldData.getSettlementsData(level).getOrCreateServerData(level,d.campX+2,d.campY-4);
  settlement.networkData.markPreventDisbanding();settlement.networkData.setName(new StaticMessage("苔光营地"));
  for(int i=0;i<2;i++){
   HumanMob mob=(HumanMob)MobRegistry.getMob(i==0?"elderhuman":"farmerhuman",level);
   mob.canDespawn=false;mob.setSettlerName(i==0?"林叔":"小禾");
   int x=d.campX-2+i*3,y=d.campY-2;
   mob.onSpawned(x*32+16,y*32+16);level.entityManager.addMob(mob,x*32+16,y*32+16);
   LevelSettler settler=new LevelSettler(settlement,mob);
   SettlementBed bed=settlement.addOrValidateBed(d.houseX-1+i*2,d.houseY-2);
   if(bed!=null)settlement.moveSettler(settler,bed,null);else settlement.moveIn(settler);
   level.gndData.setInt(i==0?"mosslumeInitialElderId":"mosslumeInitialFarmerId",mob.getUniqueID());
  }
  completeInitialRecruitment(level,settlement);
  for(WorldContent.TemplateObject object:d.content.campObjects){
   if(object.role().endsWith("storage"))settlement.storageManager.assignStorage(object.x(d),object.y(d),false);
   if(object.role().equals("workstation"))settlement.storageManager.assignWorkstation(object.x(d),object.y(d),false);
  }
  for(int i=0;i<6;i++){
   String type=i<2?"sheep":i<4?"chicken":"rabbit";
   necesse.entity.mobs.Mob animal=MobRegistry.getMob(type,level);
   int x=d.campX-9+i*2,y=d.campY-11;
   if(level.getObjectID(x,y)!=0)level.setObject(x,y,0);
   animal.onSpawned(x*32+16,y*32+16);animal.canDespawn=false;
   level.entityManager.addMob(animal,x*32+16,y*32+16);
  }
  level.gndData.setBoolean("mosslumeResidentsSeededV1",true);
 }
 /** Finish the same recruitment lifecycle as the native shop action, once per starter resident.
  * Early 0.4 previews only moved residents into beds and omitted their joining thought/cooldowns.
  * Match those two residents narrowly; never refresh the bonus on every connection or affect recruits.
  */
 private static void completeInitialRecruitment(Level level,ServerSettlementData settlement){
  for(LevelSettler settler:settlement.getSettlers()){
   if(!(settler.getMob() instanceof HumanMob))continue;
   HumanMob mob=(HumanMob)settler.getMob();
   if(!mob.isSettler()||mob.getSettlementUniqueID()!=settlement.uniqueID||mob.getLevel()!=level)continue;
   boolean elder=mob.getStringID().equals("elderhuman"),farmer=mob.getStringID().equals("farmerhuman");
   if(!elder&&!farmer)continue;
   String identity=elder?"mosslumeInitialElderId":"mosslumeInitialFarmerId";
   int known=level.gndData.getInt(identity);
   if(known!=0?known!=mob.getUniqueID():!mob.getSettlerName().equals(elder?"林叔":"小禾"))continue;
   String marker="mosslumeRecruitedV1_"+mob.getUniqueID();
   if(level.gndData.getBoolean(marker))continue;
   mob.onRecruited(null,settlement,settler);
   mob.updateHappiness();
   level.gndData.setInt(identity,mob.getUniqueID());
   level.gndData.setBoolean(marker,true);
   System.out.println("MOSSLUME_STARTER_RECRUITMENT_READY type="+mob.getStringID()+" id="+mob.getUniqueID());
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
