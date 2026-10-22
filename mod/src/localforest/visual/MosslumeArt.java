package localforest.visual;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;
import necesse.engine.GameLaunch;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.DamagedObjectEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.*;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.*;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.gameObject.*;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/** First playable art adapter. Native entities, inventory, damage and saves stay authoritative. */
public final class MosslumeArt {
    private static final Map<String,GameTexture> textures=new HashMap<>();
    private static final Map<String,String> aliases=new HashMap<>();
    private static final Map<InventoryObjectEntity,ChestAnimation> chests=new WeakHashMap<>();
    public static long playerDraws,iconDraws,worldDraws,residentDraws;
    private static boolean ready;
    private static final String[] DIRS={"up","right","down","left"};
    // 32 source body pixels x 2 = 64 world pixels, i.e. two 32 px tiles.
    // Scaling the whole frame and anchor equally preserves the original art.
    private static final int CHARACTER_SCALE=2, CHARACTER_FRAME=64;
    private static final int CHARACTER_FOOT_X=32, CHARACTER_FOOT_Y=52;
    public static boolean enabled(){return Boolean.getBoolean("mosslume")||(GameLaunch.launchOptions!=null&&GameLaunch.launchOptions.containsKey("mosslume"));}
    public static boolean active(Level level){return ready&&enabled()&&IslandVisuals.active(level);}
    private static void load(String key){
        GameTexture texture=GameTexture.fromFile("mosslume/"+key);
        if(!Boolean.getBoolean("mosslume.visual.legacy"))texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
        textures.put(key,texture);
    }
    private static void alias(String art,String... ids){for(String id:ids)aliases.put(id,art);}
    public static void initResources(){
        if(!enabled())return;
        String[] icons={"axe","pickaxe","shovel","hoe","watering-can","fishing-rod","hammer","log","stone","plant-fiber","plank","clay","copper-ore","iron-ore","copper-bar","iron-bar","coal","wheat-seed","carrot-seed","pumpkin-seed","wheat","carrot","pumpkin","apple","berry","mushroom","bread","vegetable-stew","grilled-fish","tea","water-flask","chest","workbench","furnace","cooking-pot","campfire","lantern","wood-wall","letter","trade-coin","flower-bouquet","parcel"};
        for(String s:icons){load("items/"+s);aliases.put(s.replace("-",""),s);}
        alias("log","oaklog","sprucelog","birchlog","palmlog","willowlog","pinelog");
        alias("plant-fiber","plantfiber","grassseed");alias("plank","woodplank","oakplank","woodfloor");
        alias("axe","woodaxe","copperaxe","ironaxe");alias("pickaxe","woodpickaxe","copperpickaxe","ironpickaxe");
        alias("shovel","woodshovel","coppershovel","ironshovel");alias("fishing-rod","woodfishingrod","ironfishingrod");
        alias("chest","oakchest","sprucechest","storagebox");alias("workbench","workstation","workstationduo","carpentersbench");
        alias("furnace","forge");alias("lantern","torch");alias("wood-wall","woodwall","wooddoor","wooddooropen");
        alias("berry","blueberry","raspberry","blackberry");alias("trade-coin","coin");
        alias("bread","freshbread");alias("vegetable-stew","vegetablestew","potatosoup");alias("grilled-fish","cookedfish","roastedfish");
        for(String d:DIRS){load("players/player-stand-"+d);load("players/player-walk-"+d);for(String role:new String[]{"farmer","carpenter","merchant"})load("residents/"+role+"-"+d);}
        String[] objects={"anvil","bed","berry-empty","berry-flowering","berry-unripe","berry-ripe","bookshelf","chair","chest-closed","chest-opening","chest-open","cupboard","fallen-branch","floor-lamp","interior-wall","noticeboard","occlusion-trim","plank-floor","potted-plant","rock-cracked","rock-damaged","rock-depleted","rock-intact","sapling","seedling","stump","stump-regrowth","table","wall-lamp","woven-rug","young-tree"};
        for(String s:objects){load("world/"+s);registerShadow("world/"+s);}
        for(String s:new String[]{"roof","walls","door-closed","door-open","windows-dark","windows-lit"})load("world/house-layers/"+s);
        registerHouseShadow();
        for(String crop:new String[]{"wheat","carrot","pumpkin"})for(String stage:new String[]{"sown","sprout","young","growing","ripe","harvested"}){load("crops/"+crop+"-"+stage);registerShadow("crops/"+crop+"-"+stage);}
        ready=true;System.out.println("MOSSLUME_ART_READY textures="+textures.size()+" aliases="+aliases.size()+" sourceFoot=32,52 characterScale=2 bodyWorldPixels=64");
    }
    private static void registerShadow(String key){
        try(java.io.InputStream stream=ResourceEncoder.getResourceInputStream("mosslume/"+key+".png")){
            BufferedImage im=ImageIO.read(stream);IslandVisuals.registerRuntimeArt("mosslume/"+key,textures.get(key),im);
        }catch(Exception e){throw new IllegalStateException("Cannot load Mosslume world art "+key,e);}
    }
    private static void registerHouseShadow(){
        BufferedImage silhouette=new BufferedImage(96,112,BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g=silhouette.createGraphics();
        try{
            for(String layer:new String[]{"walls","door-closed","roof"}){
                try(java.io.InputStream stream=ResourceEncoder.getResourceInputStream("mosslume/world/house-layers/"+layer+".png")){g.drawImage(ImageIO.read(stream),0,0,null);}
            }
        }catch(Exception e){throw new IllegalStateException("House shadow layers",e);}finally{g.dispose();}
        IslandVisuals.registerRuntimeArt("mosslume-house-shadow",textures.get("world/house-layers/roof"),silhouette);
    }
    public static String iconKey(Item item){return item==null?null:aliases.get(item.getStringID());}
    public static boolean icon(Item item,InventoryItem stack,PlayerMob player,int x,int y,int size,Color color){
        if(!ready||!enabled())return false;
        String key=iconKey(item);GameTexture texture=key==null?null:textures.get("items/"+key);if(texture==null)return false;
        texture.initDraw().size(size,size).color(color==null?Color.WHITE:color).draw(x,y);iconDraws++;return true;
    }
    public static DrawOptions player(PlayerMob player,int x,int y,GameLight light,GameCamera camera){
        if(player==null||!active(player.getLevel())||player.isRiding()||player.getUsingObject()!=null)return null;
        int dir=Math.floorMod(player.getDir(),4);String d=DIRS[dir];
        boolean moving=!player.isAttacking&&(Math.abs(player.dx)+Math.abs(player.dy)>2);
        String key="players/player-"+(moving?"walk-":"stand-")+d;
        final GameTexture body=textures.get(key);if(body==null)return null;
        // Four authored compact poses, selected by actual movement. The cycle
        // covers 48 world pixels, matching the previous six-frame stride.
        final int f=moving?Math.floorMod((int)(player.getDistanceRan()/12.0),walkFrameCount(body)):0;
        final int footX=camera.getDrawX(x),footY=camera.getDrawY(y);
        final int px=footX-CHARACTER_FOOT_X*CHARACTER_SCALE,py=footY-CHARACTER_FOOT_Y*CHARACTER_SCALE;
        final GameLight sceneLight=SceneLighting.light(player.getLevel(),x,y);
        InventoryItem selected=player.getSelectedItem();String tool=selected==null?null:iconKey(selected.item);
        final GameTexture handTool=player.isAttacking&&tool!=null?textures.get("items/"+tool):null;
        final float progress=Math.max(0,Math.min(1,player.isAttacking?player.getAttackAnimProgress():0));
        final float alpha=player.getInvincibilityFrameAlpha();
        // Keep the compact body through every native attack. The icon rotates
        // around its grip, with the hand and draw order selected by direction.
        // Damage, target selection and attack duration remain native.
        final float baseAngle=dir==0?-34:dir==1?56:dir==2?146:236;
        final float angle=baseAngle-80+105*progress;
        final int handX=footX+(dir==3?-10:10),handY=footY+(dir==0?-30:dir==2?-18:-22);
        final DrawOptions toolDraw=()->{if(handTool!=null)handTool.initDraw().size(32,32).rotate(angle,8,25).light(sceneLight).alpha(alpha).draw(handX-8,handY-25);};
        return ()->{
            if(dir==0)toolDraw.draw();
            body.initDraw().sprite(f,0,CHARACTER_FRAME).size(CHARACTER_FRAME*CHARACTER_SCALE,CHARACTER_FRAME*CHARACTER_SCALE).light(sceneLight).alpha(alpha).draw(px,py);
            if(dir!=0)toolDraw.draw();
            playerDraws++;
        };
    }
    private static int walkFrameCount(GameTexture strip){return Math.max(1,strip.getWidth()/CHARACTER_FRAME);}

    public static GameTexture resident(String role,int dir){return ready?textures.get("residents/"+role+"-"+DIRS[Math.floorMod(dir,4)]):null;}
    public static boolean drawResident(HumanMob mob,List<MobDrawable> list,Level level,int x,int y,GameCamera camera,PlayerMob player){
        if(!active(level)||mob.getUsingObject()!=null||mob.isDowned()||mob.isAttacking)return false;
        if(!mob.isVisible())return true;
        String id=mob.getStringID(),role=id.contains("farmer")?"farmer":id.contains("merchant")?"merchant":"carpenter";
        final GameTexture t=resident(role,mob.getDir());if(t==null)return false;
        final GameLight light=IslandVisuals.light(level,mob.getTileX(),mob.getTileY());
        final int dx=camera.getDrawX(x)-CHARACTER_FOOT_X*CHARACTER_SCALE,dy=camera.getDrawY(y)-CHARACTER_FOOT_Y*CHARACTER_SCALE;
        final DrawOptions marker=mob.getMarkerDrawOptions(x,y,light,camera,0,-69,player);
        list.add(new MobDrawable(){public void draw(TickManager tick){t.initDraw().size(CHARACTER_FRAME*CHARACTER_SCALE,CHARACTER_FRAME*CHARACTER_SCALE).light(light).draw(dx,dy);marker.draw();residentDraws++;}});
        return true;
    }
    public static String groundTexture(Level level,int tx,int ty){
        if(!active(level))return null;
        String tile=level.getTile(tx,ty).getStringID();
        return tile.contains("woodfloor")?"mosslume/world/plank-floor":null;
    }
    private static final class ChestAnimation{boolean open;long changed;ChestAnimation(boolean open){this.open=open;}}
    public static boolean object(GameObject object,List<LevelSortedDrawable> list,OrderableDrawables tileList,Level level,int tx,int ty,TickManager tick,GameCamera camera,PlayerMob player,String type){
        if(!active(level))return false;String key=null;float scale=1f;
        if(type.equals("bush")&&object instanceof FruitBushObject){key=((FruitBushObject)object).getFruitStage(level,tx,ty)>0?"berry-ripe":"berry-empty";scale=1.1f;}
        else if(type.equals("rock")){
            DamagedObjectEntity damage=level.entityManager.getDamagedObjectEntity(tx,ty);float ratio=damage==null?0:damage.getObjectDamage(0)/(float)Math.max(1,object.objectHealth);
            key=ratio>.55f?"rock-damaged":ratio>0?"rock-cracked":"rock-intact";scale=1f;
        }else if(type.equals("inventory")&&object.getStringID().contains("chest")){
            ObjectEntity entity=level.entityManager.getObjectEntity(tx,ty);boolean open=entity instanceof InventoryObjectEntity&&((InventoryObjectEntity)entity).isInUse();
            key=open?"chest-open":"chest-closed";
            if(entity instanceof InventoryObjectEntity){InventoryObjectEntity chest=(InventoryObjectEntity)entity;ChestAnimation a=chests.get(chest);if(a==null){a=new ChestAnimation(open);chests.put(chest,a);}if(a.open!=open){a.open=open;a.changed=System.currentTimeMillis();}if(System.currentTimeMillis()-a.changed<110)key="chest-opening";}
        }else if(type.equals("anvil"))key="anvil";
        else if(type.equals("noticeboard"))key="noticeboard";
        else if(type.equals("sapling"))key="sapling";
        else if(type.equals("bed"))key="bed";
        else if(type.equals("crop")&&object instanceof SeedObject){
            SeedObject seed=(SeedObject)object;String crop=seed.productStringID;if(!Arrays.asList("wheat","carrot","pumpkin").contains(crop))return false;
            int stage=seed.thisStage;String state=seed.isLastStage()?"ripe":stage==0?"sown":stage==1?"sprout":stage==2?"young":"growing";
            key="crops/"+crop+"-"+state;
        }else return false;
        final String textureKey=key.startsWith("crops/")?key:"world/"+key;
        if(!textures.containsKey(textureKey))return false;
        final int x=camera.getTileDrawX(tx)+16,y=camera.getTileDrawY(ty)+29;
        final float s=scale;final String drawKey="mosslume/"+textureKey;
        IslandVisuals.addShadow(tileList,drawKey,x,y,s,level,tx,ty,Math.min(48,textures.get(textureKey).getHeight()*s));
        list.add(IslandVisuals.sorted(object,tx,ty,23,()->{if(type.equals("bush")||type.equals("crop")||type.equals("sapling"))IslandVisuals.windSprite(drawKey,x,y,s,level,tx,ty,false,1);else IslandVisuals.litBottom(drawKey,x,y,s,level,tx,ty,1);}));
        worldDraws++;return true;
    }
    public static boolean house(Level level,GameCamera camera,int footX,int footY){
        if(!active(level))return false;
        PlayerMob player=level.getClient()==null?null:level.getClient().getPlayer();boolean inside=player!=null&&IslandVisuals.houseFootprint(player.getTileX(),player.getTileY());
        boolean open=level.getObject(52,42).getStringID().contains("open");
        float hour=level.getWorldEntity().getDayTimeHourFloat();boolean lit=hour>=18||hour<7;
        final float scale=Boolean.getBoolean("mosslume.visual.legacy")?2.4f:2f;
        int x=footX-Math.round(48*scale),y=footY-Math.round(108*scale);
        GameLight light=IslandVisuals.light(level,52,43);
        for(String layer:new String[]{"walls",open?"door-open":"door-closed",lit?"windows-lit":"windows-dark","roof"}){
            float alpha=inside?(layer.equals("roof")?.12f:.36f):1f;
            GameTexture t=textures.get("world/house-layers/"+layer);
            if(!layer.equals("windows-lit")&&SceneLighting.enabled()&&CrownWindRenderer.draw(t,footX,footY+Math.round(4*scale),scale,light,alpha,52,43,false,level,false,100))continue;
            t.initDraw().size(Math.round(96*scale),Math.round(112*scale)).light(layer.equals("windows-lit")?new GameLight(150):light).alpha(alpha).draw(x,y);
        }
        return true;
    }
    private MosslumeArt(){}
}
