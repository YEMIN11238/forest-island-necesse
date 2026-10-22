package localforest;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.CompanionView;
import localforest.scene.RegionSceneQuery;
import localforest.scene.SceneStructures;
import localforest.scene.TileBounds;
import localforest.tasks.NativeHarvestCheck;
import localforest.visual.CrownWindRenderer;
import localforest.visual.RadianceShadowRenderer;
import localforest.visual.SceneLighting;
import localforest.world.GeneratedLayout;
import localforest.world.GeneratedWorldLevel;
import localforest.world.IslandWorld;
import localforest.world.WorldDefinition;
import necesse.engine.GameEventListener;
import necesse.engine.GameEvents;
import necesse.engine.GlobalData;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.WorldDataRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.window.GameWindow;
import necesse.engine.world.worldData.WorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.entity.objectEntity.LadderDownObjectEntity;
import necesse.entity.objectEntity.PortalObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.Container;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;

/** Opt-in native integration checks. No production save, model call or physical mouse input. */
public final class RefactorNativeChecks extends WorldData {
    private static final String WORLD_KEY="MosslumeRefactorChecks";
    private static final int MARKER=4261023;
    private static final String[] ACTIONS={"FOLLOW","GUARD","HOME","FOLLOW","CANCEL"};
    private static volatile RefactorNativeChecks active;
    private static volatile Client probeClient;
    private static volatile VisualSample visual;
    private static boolean registered;
    private record Request(int index,String companion,String action,int x,int y,long revision) {}
    private record Submission(int index,String result) {}
    private record VisualSample(int levelWidth,int levelHeight,int queryTiles,int lightBoundsTiles,
        int receiverTiles,int receiverX,int receiverY,int receiverWidth,int receiverHeight,
        boolean shadowGpu,long shadowFrames,long windDraws,long windFallbacks,long lightsScanned,Map<String,Object> companionSprites) {}
    private volatile Request request;
    private volatile Submission submission;
    private volatile boolean done;
    private boolean loaded,started,checkpointComplete;
    private int marker,chestX,chestY,chestSlot,expectedLogs,expectedTorches,selectedMob;
    private String savedSeed="",selectedId="",savedMemoryId="";
    private int phase,waitTicks,commandIndex;
    private long startNanos,phaseNanos;
    private WorldDefinition definition;
    private NativeHarvestCheck harvestCheck;
    private int harvestRequestIndex=100;
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Map<String,Object>> commandEvidence=new ArrayList<>();

    public static boolean enabled(){String value=System.getProperty("mosslume.refactorProbe");return value!=null&&!value.isBlank();}
    private static boolean safeProfile(){
        Path path=Path.of(GlobalData.appDataPath()).toAbsolutePath().normalize();
        return path.getFileName()!=null&&path.getFileName().toString().startsWith("Mosslume-QA-Refactor");
    }
    public static synchronized void register(){
        if(registered)return;registered=true;
        WorldDataRegistry.registerWorldData("mosslumerefactorchecks",RefactorNativeChecks.class);
        GameEvents.addListener(ServerClientConnectedEvent.class,new GameEventListener<ServerClientConnectedEvent>(){
            @Override public void onEvent(ServerClientConnectedEvent event){
                if(!enabled()||!safeProfile())return;
                var world=event.client.getServer().world.worldEntity;
                if(world.getWorldData(WORLD_KEY)==null)world.addWorldData(WORLD_KEY,new RefactorNativeChecks());
            }
        });
    }
    @Override public void init(){if(enabled()&&safeProfile()&&isServer())active=this;}
    @Override public void addSaveData(SaveData save){
        super.addSaveData(save);
        save.addInt("marker",marker);save.addBoolean("checkpointComplete",checkpointComplete);
        save.addSafeString("seed",savedSeed);save.addInt("chestX",chestX);save.addInt("chestY",chestY);save.addInt("chestSlot",chestSlot);
        save.addInt("expectedLogs",expectedLogs);save.addInt("expectedTorches",expectedTorches);
        save.addSafeString("selectedId",selectedId);save.addInt("selectedMob",selectedMob);save.addSafeString("savedMemoryId",savedMemoryId);
    }
    @Override public void applyLoadData(LoadData save){
        super.applyLoadData(save);loaded=true;
        marker=save.getInt("marker",0);checkpointComplete=save.getBoolean("checkpointComplete",false);
        savedSeed=save.getSafeString("seed","");chestX=save.getInt("chestX",0);chestY=save.getInt("chestY",0);chestSlot=save.getInt("chestSlot",0);
        expectedLogs=save.getInt("expectedLogs",0);expectedTorches=save.getInt("expectedTorches",0);
        selectedId=save.getSafeString("selectedId","");selectedMob=save.getInt("selectedMob",0);savedMemoryId=save.getSafeString("savedMemoryId","");
    }
    /** All scene reads and user-facing submit calls run on the client/render thread. */
    public static void frame(MainGame game){
        if(!enabled()||!safeProfile())return;
        // A disposable automated run must not freeze when the user focuses another app.
        // This is process-local; never persist the setting or affect ordinary profiles.
        necesse.engine.Settings.pauseOnFocusLoss=false;
        RefactorNativeChecks check=active;Client client=game.getClient();
        if(check==null||check.done||client==null||client.isDisconnected()||client.getLocalServer()!=check.getServer())return;
        if(!game.isRunning())game.setRunning(true);
        PlayerMob player=client.getPlayer();if(player==null||player.getLevel()==null)return;
        probeClient=client;Level level=player.getLevel();
        if(IslandWorld.isIsland(level)&&!level.isCave){
            TileBounds b=RegionSceneQuery.visibleLights(level);
            visual=new VisualSample(level.tileWidth,level.tileHeight,SceneLighting.lastQueryTiles,b.width()*b.height(),
                RadianceShadowRenderer.receiverTileCount,RadianceShadowRenderer.receiverOriginX,RadianceShadowRenderer.receiverOriginY,
                RadianceShadowRenderer.receiverWidth,RadianceShadowRenderer.receiverHeight,RadianceShadowRenderer.gpuActive,
                RadianceShadowRenderer.frames,CrownWindRenderer.meshDraws,CrownWindRenderer.fallbackDraws,SceneLighting.lightTilesScanned,
                localforest.visual.CompanionSprites.counters());
        }
        Request next=check.request;Submission old=check.submission;
        if(next!=null&&(old==null||old.index()!=next.index())){
            String result=CompanionSystem.submit(client,next.companion(),next.action(),next.x(),next.y(),1);
            check.submission=new Submission(next.index(),result);
        }
    }
    @ModMethodPatch(target=MainGame.class,name="frameTick",arguments={TickManager.class,GameWindow.class})
    public static class FrameProbe {
        @Advice.OnMethodExit public static void after(@Advice.This MainGame game){RefactorNativeChecks.frame(game);}
    }
    @Override public void tick(){
        if(done||!enabled()||!safeProfile()||!isServer())return;
        try{
            long now=System.nanoTime();if(startNanos==0)startNanos=now;
            require(now-startNanos<(Boolean.getBoolean("mosslume.refactorProbe.harvest")?180_000_000_000L:60_000_000_000L),"Native probe timed out in phase "+phase);
            ServerClient client=getServer().streamClients().filter(c->c.hasSpawned()&&c.playerMob!=null).findFirst().orElse(null);
            if(client==null)return;
            if(!started){
                if(!(client.getLevel() instanceof GeneratedWorldLevel)||client.getLevel().isCave)return;
                started=true;definition=WorldDefinition.forLevel(client.getLevel());require(definition!=null,"Generated definition missing");
                checkWorld(client);
                phaseNanos=now;
                if(Boolean.getBoolean("mosslume.refactorProbe.harvest")){
                    require(!loaded,"Harvest probe requires a fresh disposable world");
                    harvestCheck=new NativeHarvestCheck(definition,(companion,action,x,y)->{
                        request=new Request(++harvestRequestIndex,companion.id(),action,x,y,companion.revision());
                    });
                    phase=7;evidence.put("harvest",harvestCheck.evidence());evidence.put("inventoryInjectionSkipped",true);
                }else{
                    checkInventory(client);phase=!loaded&&Boolean.getBoolean("mosslume.refactorProbe.ladders")?1:4;
                }
                evidence.put("ladderCheckRequested",phase==1);
            }
            if(phase==1){enterCave(client);phase=2;waitTicks=0;return;}
            if(phase==2){if(++waitTicks<20)return;leaveCave(client);phase=3;waitTicks=0;return;}
            if(phase==3){
                if(++waitTicks<20)return;
                require(LevelIdentifier.SURFACE_IDENTIFIER.equals(client.getLevelIdentifier()),"Ladder did not return to surface");
                require(Math.hypot(client.playerMob.getTileX()-definition.caveX,client.playerMob.getTileY()-definition.caveY)<5,"Return ladder landed far from its endpoint");
                client.playerMob.setPos(definition.spawnX*32+16,definition.spawnY*32+16,true);
                evidence.put("nativeLadderRoundTrip",true);phase=4;phaseNanos=now;return;
            }
            if(phase==4){
                Client local=probeClient;if(local==null||local.getLocalServer()!=getServer())return;
                List<CompanionView> companions=CompanionSystem.snapshot(local).companions();if(companions.size()<2)return;
                if(loaded){checkReloadCompanion(companions);phase=6;}
                else{
                    CompanionView selected=companions.stream().filter(c->c.name().contains("禾")).findFirst().orElse(companions.get(0));
                    selectedId=selected.id();selectedMob=selected.mobId();commandIndex=0;phase=5;queue(selected,now);
                }
            }
            if(phase==5)advanceCommand(client,now);
            if(phase==7&&harvestCheck.tick(client,probeClient,now)){
                request=null;selectedId=harvestCheck.companionId();selectedMob=harvestCheck.mobId();
                savedMemoryId=harvestCheck.latestTaskId();phase=6;
            }
            if(phase==6)finishWhenRendered(now);
        }catch(Throwable error){fail(error);}
    }
    private void checkWorld(ServerClient client){
        Level level=client.getLevel();WorldDefinition d=definition;GeneratedLayout layout=((GeneratedWorldLevel)level).layout();
        require(level.tileWidth==d.width&&level.tileHeight==d.height,"Native dimensions differ from versioned definition");
        require(d.encodedSeed.equals(worldEntity.worldSeed),"Native world seed lost its versioned definition");
        require(d.encodedSeed.equals(level.gndData.getString("mosslumeWorldDefinition","")),"Level metadata differs from native world seed");
        require(worldEntity.spawnTile.equals(new Point(d.spawnX,d.spawnY)),"Native world spawn differs from definition");
        require(!level.isLiquidTile(d.spawnX,d.spawnY)&&!level.isSolidTile(d.spawnX,d.spawnY),"Spawn is not walkable");
        int samples=0;
        for(int x=0;x<d.width;x++){require(!layout.land(x,0)&&!layout.land(x,d.height-1),"Land escaped north/south limit");samples+=2;}
        for(int y=0;y<d.height;y++){require(!layout.land(0,y)&&!layout.land(d.width-1,y),"Land escaped east/west limit");samples+=2;}
        int coastX=d.campX;while(coastX<d.width-1&&layout.land(coastX,d.campY))coastX++;
        level.regionManager.ensureTileIsLoaded(coastX,d.campY);
        require(layout.coast(coastX,d.campY),"Coast boundary sample missing");
        require(level.getObject(coastX,d.campY).getStringID().equals("lfcoastbarrier")&&level.getObject(coastX,d.campY).isSolid,"Native coast collision missing");
        LevelIdentifier before=client.getLevelIdentifier();client.changeLevel(new LevelIdentifier("mosslume-probe-forbidden"));
        require(before.equals(client.getLevelIdentifier()),"Invalid map travel escaped generated world");
        require(SceneStructures.houses(level).size()==1,"Generated house anchor missing");
        var house=SceneStructures.houses(level).get(0);require(house.footTileX==d.houseX&&house.footTileY==d.houseY,"House visual anchor differs from world structure");
        Map<String,Object> world=new LinkedHashMap<>();
        world.put("seed",d.userSeed);world.put("encodedSeed",d.encodedSeed);world.put("preset",d.preset);world.put("rules",d.rules);
        world.put("generatorVersion",d.generatorVersion);world.put("contentVersion",d.contentVersion);world.put("width",d.width);world.put("height",d.height);
        world.put("spawn",List.of(d.spawnX,d.spawnY));world.put("house",List.of(d.houseX,d.houseY));world.put("finiteEdgeSamples",samples);
        world.put("solidCoastSample",List.of(coastX,d.campY));world.put("invalidTravelBlocked",true);evidence.put("world",world);
    }
    private void checkInventory(ServerClient client){
        Level level=client.getLevel();PlayerMob player=client.playerMob;
        if(loaded){
            require(marker==MARKER&&checkpointComplete,"WorldData checkpoint absent/incomplete on reload");
            require(savedSeed.equals(definition.encodedSeed),"Persisted test checkpoint belongs to another world");
            level.regionManager.ensureTileIsLoaded(chestX,chestY);
            InventoryObjectEntity chest=chest(level,chestX,chestY);InventoryItem stored=chest.getInventory().getItem(chestSlot);
            require(stored!=null&&stored.item.getStringID().equals("coin")&&stored.getAmount()==37,"Native chest marker was not saved/read back");
            require(amount(player,"oaklog")==expectedLogs&&amount(player,"torch")==expectedTorches,"Crafted item/ingredient inventory was not saved/read back");
            evidence.put("worldDataReadBack",true);evidence.put("nativeChestReadBack",true);evidence.put("craftedInventoryReadBack",true);
            checkCampPersistence(client,true);
            return;
        }
        require(marker==0,"Unexpected preexisting checkpoint on a fresh probe");
        var storage=definition.content.campObjects.stream().filter(o->o.role().equals("starter_storage")).findFirst().orElseThrow();
        chestX=storage.x(definition);chestY=storage.y(definition);InventoryObjectEntity chest=chest(level,chestX,chestY);
        require(chest.getInventory().getItem(0)!=null,"Native starter chest is empty");
        chestSlot=chest.getInventory().getSize()-1;require(chest.getInventory().getItem(chestSlot)==null,"Probe marker slot is already occupied");
        chest.getInventory().setItem(chestSlot,new InventoryItem("coin",37));
        player.getInv().main.setItem(5,new InventoryItem("oaklog",25));player.getInv().main.setItem(6,new InventoryItem("oaksapling",25));
        Recipe recipe=Recipes.streamRecipes().filter(r->r.resultStringID.equals("torch")).findFirst().orElseThrow();
        Container container=new Container(client,4261023);int recipeId=container.addRecipe(recipe);
        int logs=amount(player,"oaklog"),torches=amount(player,"torch");
        require(container.applyCraftingAction(recipeId,recipe.getRecipeHash()+1,1,true)==0,"Invalid recipe hash was accepted");
        require(amount(player,"oaklog")==logs&&amount(player,"torch")==torches,"Invalid recipe changed native inventory");
        require(container.applyCraftingAction(recipeId,recipe.getRecipeHash(),1,true)==1,"Native crafting transaction failed");
        expectedLogs=amount(player,"oaklog");expectedTorches=amount(player,"torch");
        require(expectedLogs<logs&&expectedTorches>torches,"Craft did not consume ingredients and produce native items");
        marker=MARKER;savedSeed=definition.encodedSeed;
        evidence.put("nativeChestMarkerWritten",true);evidence.put("invalidRecipeRejected",true);evidence.put("nativeCraftConsumesAndProduces",true);
        evidence.put("expectedSavedInventory",Map.of("oaklog",expectedLogs,"torch",expectedTorches));
        checkCampPersistence(client,false);
    }
    private void checkCampPersistence(ServerClient client,boolean readBack){
        var settlement=necesse.engine.world.worldData.SettlementsWorldData.getSettlementsData(client.getLevel())
            .getOrCreateServerData(client.getLevel(),definition.campX+2,definition.campY-4);
        Point bed=new Point(definition.houseX-1,definition.houseY-2);
        String name="QA 搬迁营地";
        if(readBack){
            require(LevelIdentifier.SURFACE_IDENTIFIER.equals(client.spawnLevelIdentifier)&&bed.equals(client.spawnTile),
                "Saved personal bed spawn was overwritten on reconnect");
            require(name.equals(settlement.networkData.getSettlementName().translate()),"Saved camp name was overwritten on reconnect");
            evidence.put("personalSpawnReadBack",true);evidence.put("renamedCampReadBack",true);
        }else{
            // Same persisted fields used by native BedContainer.setSpawn. This tests
            // reconnect persistence, not physical interaction with the bed UI.
            client.spawnLevelIdentifier=LevelIdentifier.SURFACE_IDENTIFIER;client.spawnTile=bed;
            settlement.networkData.setName(new necesse.engine.localization.message.StaticMessage(name));
            evidence.put("personalSpawnWritten",List.of(bed.x,bed.y));evidence.put("renamedCampWritten",name);
        }
    }
    private InventoryObjectEntity chest(Level level,int x,int y){
        var object=level.entityManager.getObjectEntity(x,y);require(object instanceof InventoryObjectEntity,"Native chest object entity missing");return (InventoryObjectEntity)object;
    }
    private int amount(PlayerMob player,String id){
        int total=0;for(int i=0;i<player.getInv().main.getSize();i++){InventoryItem item=player.getInv().main.getItem(i);if(item!=null&&item.item.getStringID().equals(id))total+=item.getAmount();}return total;
    }
    private void enterCave(ServerClient client){
        Level surface=client.getLevel();surface.regionManager.ensureTileIsLoaded(definition.caveX,definition.caveY);
        var object=surface.entityManager.getObjectEntity(definition.caveX,definition.caveY);
        require(object instanceof LadderDownObjectEntity,"Native ladder-down missing");((LadderDownObjectEntity)object).use(getServer(),client);
    }
    private void leaveCave(ServerClient client){
        Level cave=client.getLevel();require(cave instanceof GeneratedWorldLevel&&cave.isCave&&LevelIdentifier.CAVE_IDENTIFIER.equals(cave.getIdentifier()),"Native ladder did not enter generated cave");
        require(SceneStructures.houses(cave).isEmpty(),"Surface house fallback appeared in cave");
        var up=cave.entityManager.getObjectEntity(definition.caveX,definition.caveY);require(up instanceof PortalObjectEntity,"Native reciprocal ladder missing");
        ((PortalObjectEntity)up).use(getServer(),client);
    }
    private void queue(CompanionView companion,long now){
        String action=ACTIONS[commandIndex];request=new Request(commandIndex,selectedId,action,definition.spawnX,definition.spawnY,companion.revision());phaseNanos=now;
    }
    private void advanceCommand(ServerClient client,long now){
        Submission sent=submission;Request req=request;
        CompanionView view=CompanionSystem.snapshot(probeClient).companions().stream().filter(c->c.id().equals(selectedId)).findFirst().orElse(null);
        HumanMob nativeMob=client.getLevel().entityManager.mobs.stream().filter(m->m instanceof HumanMob&&m.getUniqueID()==selectedMob).map(m->(HumanMob)m).findFirst().orElse(null);
        Map<String,Object> diagnostic=new LinkedHashMap<>();
        diagnostic.put("requested",req==null?"":req.action());diagnostic.put("queueFeedback",sent==null?"not submitted":sent.result());
        diagnostic.put("requestRevision",req==null?-1:req.revision());diagnostic.put("serverClientSlot",client.slot);
        if(view!=null){diagnostic.put("viewRevision",view.revision());diagnostic.put("viewAction",view.action());diagnostic.put("viewState",view.state());diagnostic.put("viewMessage",view.message());}
        if(nativeMob!=null){
            var settlement=nativeMob.getSettlerSettlementNetworkData();
            diagnostic.put("nativeClass",nativeMob.getClass().getName());diagnostic.put("settlementId",nativeMob.getSettlementUniqueID());
            diagnostic.put("canCommand",nativeMob.canBeCommanded(client,false));diagnostic.put("canCommandSettings",nativeMob.canBeCommanded(client,true));
            diagnostic.put("isOnStrike",nativeMob.isOnStrike());diagnostic.put("hasMission",nativeMob.getCurrentMission()!=null);
            diagnostic.put("settlementPresent",settlement!=null);diagnostic.put("settlementOwned",settlement!=null&&settlement.hasOwner());
            diagnostic.put("ownerMatchesClient",settlement!=null&&settlement.getOwnerAuth()==client.authentication);
            diagnostic.put("settlementUnloaded",settlement!=null&&settlement.isUnloadedOrDisbanded());diagnostic.put("settlementPrivate",settlement!=null&&settlement.isPrivate());
            diagnostic.put("settlementAllowsAccess",settlement!=null&&settlement.doesClientHaveAccess(client));
            diagnostic.put("nativeFollowingPlayer",nativeMob.commandFollowMob==client.playerMob);
            diagnostic.put("nativeGuard",nativeMob.commandGuardPoint==null?null:List.of(nativeMob.commandGuardPoint.x,nativeMob.commandGuardPoint.y));
        }
        evidence.put("commandDiagnostic",diagnostic);
        require(now-phaseNanos<10_000_000_000L,"Companion command timeout: "+ACTIONS[commandIndex]);
        if(sent==null||req==null||sent.index()!=commandIndex)return;
        require(sent.result().contains("已提交"),"Companion command was not queued: "+sent.result());
        if(view==null||view.revision()<=req.revision())return;
        require(!view.state().equals("遇到阻碍")&&!view.state().equals("未完成"),"Companion native command blocked: "+view.message());
        require(nativeMob!=null,"Selected native companion disappeared");
        String action=req.action();boolean matched;
        if(action.equals("FOLLOW"))matched=view.action().equals(action)&&view.state().equals("执行中")&&nativeMob.commandFollowMob==client.playerMob;
        else if(action.equals("GUARD"))matched=view.action().equals(action)&&view.state().equals("执行中")&&new Point(req.x()*32+16,req.y()*32+16).equals(nativeMob.commandGuardPoint);
        else if(action.equals("HOME"))matched=view.action().equals(action)&&nativeMob.home!=null&&(
            nativeMob.commandGuardPoint!=null
                &&nativeMob.commandGuardPoint.distance(nativeMob.home.x*32+16,nativeMob.home.y*32+16)<=4*32
                &&!nativeMob.getLevel().isLiquidTile(nativeMob.commandGuardPoint.x/32,nativeMob.commandGuardPoint.y/32)
            ||view.state().equals("已完成")&&Math.hypot(nativeMob.x-(nativeMob.home.x*32+16),nativeMob.y-(nativeMob.home.y*32+16))<6*32);
        else matched=view.state().equals("已取消")&&nativeMob.commandFollowMob==null&&nativeMob.commandGuardPoint==null;
        if(!matched){
            require(!view.state().equals("遇到阻碍")&&!view.state().equals("未完成"),"Companion native command blocked: "+view.message());return;
        }
        commandEvidence.add(Map.of("submitted",action,"queueFeedback",sent.result(),"state",view.state(),"feedback",view.message(),"revision",view.revision(),"nativeOrderMatched",true));
        commandIndex++;
        if(commandIndex<ACTIONS.length)queue(view,now);
        else{
            request=null;savedMemoryId=view.memories().stream().filter(e->e.type().equals("task_accepted")).map(CompanionSystem.MemoryEvent::id).reduce((a,b)->b).orElseThrow();
            evidence.put("commands",commandEvidence);recordCompanion(view);phase=6;
        }
    }
    private void recordCompanion(CompanionView view){
        evidence.put("companion",Map.of("id",view.id(),"mobId",view.mobId(),"name",view.name(),"revision",view.revision(),"memoryCount",view.memories().size(),"savedMemoryId",savedMemoryId));
    }
    private void checkReloadCompanion(List<CompanionView> companions){
        CompanionView view=companions.stream().filter(c->c.id().equals(selectedId)).findFirst().orElseThrow(()->new IllegalStateException("Stable companion identity not restored"));
        require(view.mobId()==selectedMob,"Native companion ID changed after load");
        require(!savedMemoryId.isEmpty()&&view.memories().stream().anyMatch(e->e.id().equals(savedMemoryId)),"Companion event memory not restored");
        require(view.state().equals("已取消"),"Saved terminal companion task not restored");
        recordCompanion(view);evidence.put("companionIdentityReadBack",true);evidence.put("companionMemoryReadBack",true);evidence.put("companionTaskReadBack",true);
    }
    private void finishWhenRendered(long now) throws Exception {
        VisualSample v=visual;if(v==null||v.shadowFrames()<12||v.windDraws()<1||v.queryTiles()<1||v.receiverTiles()<1)return;
        require(v.shadowGpu(),"Shadow renderer fell back from GPU");require(v.windFallbacks()==0,"Wind renderer used fallback path");
        require(v.queryTiles()<=v.lightBoundsTiles(),"Light query escaped camera neighborhood");
        require(v.queryTiles()<v.levelWidth()*v.levelHeight(),"Light query scanned whole generated world");
        require(v.receiverTiles()==v.receiverWidth()*v.receiverHeight()&&v.receiverTiles()<v.levelWidth()*v.levelHeight(),"Shadow receiver cache is not bounded");
        Map<String,Object> render=new LinkedHashMap<>();render.put("lightTilesQueried",v.queryTiles());render.put("cameraLightBoundsTiles",v.lightBoundsTiles());
        render.put("worldTiles",v.levelWidth()*v.levelHeight());render.put("receiverTiles",v.receiverTiles());render.put("receiverOrigin",List.of(v.receiverX(),v.receiverY()));
        render.put("receiverDimensions",List.of(v.receiverWidth(),v.receiverHeight()));render.put("shadowGpu",v.shadowGpu());render.put("shadowFrames",v.shadowFrames());
        render.put("windGpuDraws",v.windDraws());render.put("windFallbacks",v.windFallbacks());render.put("lightTilesScannedCumulative",v.lightsScanned());
        render.put("companionSprites",v.companionSprites());evidence.put("visual",render);
        checkpointComplete=true;done=true;evidence.put("passed",true);evidence.put("reload",loaded);evidence.put("profile",GlobalData.appDataPath());
        evidence.put("elapsedSeconds",(now-startNanos)/1e9);evidence.put("checkpointMarker",marker);
        evidence.put("scope",harvestCheck==null?
            "Native server inventory/world/command checks in an opt-in disposable QA profile; commands use the same client queue as UI buttons. No physical mouse, AI-provider, aesthetic similarity or long-session performance claim.":
            "Native crop harvest, in-flight cancellation and re-execution in an opt-in fresh disposable QA profile. Uses original generated crops and native inventories; no crop/item injection. Harvest output is not a warehouse-delivery claim.");
        write(loaded?"native-checks-reload.json":"native-checks.json");
        System.out.println("MOSSLUME_REFACTOR_NATIVE_CHECKS "+json(evidence));
        if(Boolean.getBoolean("mosslume.refactorProbe.stop"))getServer().stop(server->{System.out.println("MOSSLUME_REFACTOR_PROBE_SAVED");System.exit(0);});
    }
    private void fail(Throwable error){
        done=true;request=null;evidence.put("passed",false);evidence.put("reload",loaded);evidence.put("phase",phase);evidence.put("error",error.toString());
        evidence.put("scope","Opt-in disposable native API probe; no physical mouse input");
        try{write("native-checks-failed.json");}catch(Exception writeError){System.err.println("MOSSLUME_REFACTOR_PROBE_WRITE_FAILED "+writeError);}
        System.err.println("MOSSLUME_REFACTOR_NATIVE_CHECKS_FAILED "+json(evidence));error.printStackTrace();
        // fail() is called by WorldData.tick on the server thread. Install the
        // native save/stop handler here, without a UI call or a shutdown thread.
        if (enabled() && safeProfile() && isServer() && Boolean.getBoolean("mosslume.refactorProbe.stop"))
            getServer().stop(server->{System.out.println("MOSSLUME_REFACTOR_PROBE_FAILED_SAVED");System.exit(1);});
    }
    private void write(String file) throws Exception {Path dir=Path.of(System.getProperty("mosslume.refactorProbe"));Files.createDirectories(dir);Files.writeString(dir.resolve(file),json(evidence)+"\n",StandardCharsets.UTF_8);}
    private static void require(boolean condition,String error){if(!condition)throw new IllegalStateException(error);}
    private static String json(Object value){
        if(value==null)return "null";if(value instanceof Number||value instanceof Boolean)return value.toString();
        if(value instanceof Map<?,?>){StringBuilder out=new StringBuilder("{");for(var e:((Map<?,?>)value).entrySet()){if(out.length()>1)out.append(',');out.append(json(e.getKey().toString())).append(':').append(json(e.getValue()));}return out.append('}').toString();}
        if(value instanceof Iterable<?>){StringBuilder out=new StringBuilder("[");for(Object item:(Iterable<?>)value){if(out.length()>1)out.append(',');out.append(json(item));}return out.append(']').toString();}
        return '"'+value.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t")+'"';
    }
}
