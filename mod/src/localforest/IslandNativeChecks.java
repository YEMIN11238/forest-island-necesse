package localforest;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import necesse.engine.*;
import necesse.engine.events.ServerClientConnectedEvent;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.WorldDataRegistry;
import necesse.engine.save.*;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.worldData.WorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.inventory.*;
import necesse.inventory.container.Container;
import necesse.inventory.recipe.*;
import necesse.level.maps.Level;
import localforest.world.IslandWorld;

/** Only executes in an explicitly requested disposable -islandsmoke profile. */
public class IslandNativeChecks extends WorldData {
    private boolean done;private boolean readBack;private int marker;
    public static void register() {
        WorldDataRegistry.registerWorldData("localforestchecks",IslandNativeChecks.class);
        GameEvents.addListener(ServerClientConnectedEvent.class,new GameEventListener<ServerClientConnectedEvent>(){
            @Override public void onEvent(ServerClientConnectedEvent event){
                if(!GameLaunch.launchOptions.containsKey("islandsmoke"))return;
                necesse.engine.world.WorldEntity world=event.client.getServer().world.worldEntity;
                if(world.getWorldData("ForestChecks")==null)world.addWorldData("ForestChecks",new IslandNativeChecks());
            }
        });
    }
    @Override public void addSaveData(SaveData save){super.addSaveData(save);save.addInt("marker",marker);}
    @Override public void applyLoadData(LoadData save){marker=save.getInt("marker",0);readBack=true;}
    private void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private int amount(PlayerMob player,String id){int total=0;for(int i=0;i<player.getInv().main.getSize();i++){InventoryItem item=player.getInv().main.getItem(i);if(item!=null&&item.item.getStringID().equals(id))total+=item.getAmount();}return total;}
    @Override public void tick(){
        if(done||!isServer()||!GameLaunch.launchOptions.containsKey("islandsmoke"))return;
        ServerClient client=getServer().streamClients().filter(c->c.hasSpawned()&&c.playerMob!=null).findFirst().orElse(null);
        if(client==null||!IslandWorld.isIsland(client.getLevel()))return;
        done=true;
        try{
            Level level=client.getLevel();PlayerMob player=client.playerMob;
            require(level.tileWidth==96&&level.tileHeight==80,"Unexpected island dimensions");
            InventoryObjectEntity chest=(InventoryObjectEntity)level.entityManager.getObjectEntity(40,35);
            require(chest!=null&&chest.getInventory().getItem(0)!=null,"Native chest/inventory is missing");
            int barrierCount=0;for(int y=0;y<level.tileHeight;y++)for(int x=0;x<level.tileWidth;x++)if(level.getObject(x,y).getStringID().equals("lfcoastbarrier")){require(level.getObject(x,y).isSolid,"Non-solid shore barrier");barrierCount++;}
            require(barrierCount>50,"Incomplete shore collision");
            LevelIdentifier before=client.getLevelIdentifier();client.changeLevel(new LevelIdentifier("forest-forbidden-destination"));
            require(before.equals(client.getLevelIdentifier()),"Map travel escaped fixed island");
            player.getInv().main.setItem(5,new InventoryItem("oaklog",25));player.getInv().main.setItem(6,new InventoryItem("oaksapling",25));
            Recipe recipe=Recipes.streamRecipes().filter(r->r.resultStringID.equals("torch")).findFirst().orElseThrow(()->new IllegalStateException("Native torch recipe missing"));
            Container container=new Container(client,420421);int id=container.addRecipe(recipe);
            int logsBefore=amount(player,"oaklog"),torchesBefore=amount(player,"torch");
            require(container.applyCraftingAction(id,recipe.getRecipeHash()+1,1,true)==0,"Invalid recipe hash was accepted");
            require(amount(player,"oaklog")==logsBefore,"Invalid craft consumed ingredients");
            require(container.applyCraftingAction(id,recipe.getRecipeHash(),1,true)==1,"Native crafting transaction failed");
            require(amount(player,"oaklog")<logsBefore&&amount(player,"torch")>torchesBefore,"Crafting did not consume and produce real items");
            boolean persisted=readBack&&marker==4212026;marker=4212026;
            Path dir=Paths.get(GlobalData.appDataPath(),"evidence");Files.createDirectories(dir);
            String result="{\"passed\":true,\"nativeChest\":true,\"shoreBarriers\":"+barrierCount+",\"travelBlocked\":true,\"nativeCraftConsumesAndProduces\":true,\"invalidRecipeRejected\":true,\"customWorldDataReadBack\":"+persisted+",\"scope\":\"Native server API transaction checks in disposable smoke profile; no claim of physical mouse input\"}";
            Files.write(dir.resolve(readBack?"native-checks-reload.json":"native-checks.json"),result.getBytes(StandardCharsets.UTF_8));
            System.out.println("LOCALFOREST_NATIVE_CHECKS "+result);
        }catch(Throwable error){
            try{Path dir=Paths.get(GlobalData.appDataPath(),"evidence");Files.createDirectories(dir);Files.write(dir.resolve("native-checks-failed.txt"),error.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}
            throw new RuntimeException("Owned island native checks failed",error);
        }
    }
}
