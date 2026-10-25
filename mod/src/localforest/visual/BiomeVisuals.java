package localforest.visual;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import javax.imageio.ImageIO;
import localforest.world.BiomeLayout;
import localforest.world.WorldDefinition;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.InventoryObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.DirtTile;
import necesse.level.maps.Level;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;
import necesse.level.maps.regionSystem.SimulatePriorityList;

/** Generated art bound to native, saved, damageable objects, using the existing wind/light pipeline. */
public final class BiomeVisuals {
    public static final int GROUND_PERIOD=BiomeTerrainMaterials.PERIOD;
    private static boolean ready;
    private static final Map<Level,Optional<BiomeLayout>> layouts=new WeakHashMap<>();
    private static final String[] NAMES={"湿地","蘑菇林","古代遗迹","雪地","沙漠","火山"};
    private static final Color[] COLORS={new Color(88,110,83),new Color(97,83,108),new Color(120,117,96),new Color(172,190,200),new Color(175,147,94),new Color(83,72,74)};
    public static String name(String biome){for(int i=0;i<BiomeLayout.IDS.length;i++)if(BiomeLayout.IDS[i].equals(biome))return NAMES[i];return "林间营地";}
    public static String key(String biome,String asset){return "mosslume/biomes/"+biome+"/"+asset;}
    public static boolean hasTree(String biome){return !biome.equals("ruins");}
    public static void init(){
        for(int b=0;b<BiomeLayout.IDS.length;b++){
            String biome=BiomeLayout.IDS[b];
            TileRegistry.registerTile("mlbiome_"+biome+"_ground",new BiomeGround(biome,COLORS[b]),0f,false,false,false);
            for(int i=0;i<9;i++)ObjectRegistry.registerObject("mlbiome_"+biome+"_flora"+i,new Flora(biome,i,false,COLORS[b]),-1f,false);
            if(hasTree(biome))ObjectRegistry.registerObject("mlbiome_"+biome+"_tree",new Flora(biome,0,true,COLORS[b]),-1f,false);
            for(int variant=0;variant<3;variant++){
                int[] ids=new int[6];
                for(int y=0;y<2;y++)for(int x=0;x<3;x++){
                    String id="mlbiome_"+biome+"_structure"+variant+(x==1&&y==1?"":"_"+x+"_"+y);
                    ids[x+y*3]=ObjectRegistry.registerObject(id,new Structure(biome,variant,x,y,ids,COLORS[b]),-1f,false);
                }
            }
        }
    }
    public static void initResources(){
        for(String biome:BiomeLayout.IDS){
            VisualMaterials.catalog(biome);
            loadGround(biome);
            for(int i=0;i<9;i++)load(key(biome,"flora-"+i));
            for(int i=0;i<3;i++)load(key(biome,"structure-"+i));
            if(hasTree(biome))for(String asset:new String[]{"tree","tree-trunk","tree-crown"})load(key(biome,asset));
        }
        CrownSpriteBatch.prepareAtlas(IslandVisuals.artwork());
        ready=true;
        System.out.println("MOSSLUME_BIOME_ART_READY regions=6 terrain=6 flora=54 trees=5 structures=18 lighting=existing");
    }
    private static void loadGround(String biome){
        try(InputStream in=ResourceEncoder.getResourceInputStream(key(biome,"ground")+".png")){
            var material=BiomeTerrainMaterials.prepare(biome,ImageIO.read(in));
            GameTexture texture=new GameTexture(material.key,GROUND_PERIOD,GROUND_PERIOD);
            byte[] pixels=new byte[GROUND_PERIOD*GROUND_PERIOD*4];
            for(int y=0;y<GROUND_PERIOD;y++)for(int x=0;x<GROUND_PERIOD;x++){
                int rgb=material.ground(x,y),offset=(x+y*GROUND_PERIOD)*4;
                pixels[offset]=(byte)(rgb>>>16);pixels[offset+1]=(byte)(rgb>>>8);pixels[offset+2]=(byte)rgb;pixels[offset+3]=(byte)255;
            }
            texture.putData(0,pixels);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
            IslandVisuals.registerRuntimeArt(material.key,texture,material.ground);
        }catch(Exception e){throw new IllegalStateException("Cannot prepare biome terrain "+biome,e);}
    }
    private static void load(String key){
        try(InputStream in=ResourceEncoder.getResourceInputStream(key+".png")){
            BufferedImage image=ImageIO.read(in);
            if(image==null)throw new IllegalStateException("Unreadable biome image "+key);
            GameTexture texture=GameTexture.fromFile(key);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
            IslandVisuals.registerRuntimeArt(key,texture,image);
        }catch(Exception e){throw new IllegalStateException("Missing required biome resource "+key,e);}
    }
    public static String groundTexture(Level level,int x,int y){
        if(!ready||level==null||level.isCave)return null;
        String tile=level.getTile(x,y).getStringID();
        if(!tile.startsWith("mlbiome_")||!tile.endsWith("_ground"))return null;
        return BiomeTerrainMaterials.key(tile.substring(8,tile.length()-7));
    }
    public static boolean biomeGround(Level level,int x,int y){return ready&&level.getTile(x,y).getStringID().startsWith("mlbiome_");}
    /** Roads retain native dirt IDs; choose their verge from the versioned world layout. */
    public static String pathGround(Level level,int x,int y){
        if(!ready||level==null||level.isCave)return null;
        Optional<BiomeLayout> layout=layouts.computeIfAbsent(level,l->{
            WorldDefinition d=WorldDefinition.forLevel(l);
            return d!=null&&d.isMultibiome()?Optional.of(new BiomeLayout(d)):Optional.empty();
        });
        if(layout.isEmpty())return null;
        String biome=layout.get().biome(x,y);
        return biome.equals("forest")?null:BiomeTerrainMaterials.key(biome);
    }
    private static final class BiomeGround extends DirtTile {
        private final String biome;
        BiomeGround(String biome,Color color){this.biome=biome;mapColor=color;isOrganic=!biome.equals("ruins")&&!biome.equals("volcano");}
        @Override public GameMessage getNewLocalization(){return new StaticMessage(name(biome)+"地表");}
        @Override public void tick(Level level,int x,int y){}
        @Override public void addSimulateLogic(Level level,int x,int y,long ticks,SimulatePriorityList list,boolean send){}
    }
    private static final class Flora extends GameObject {
        private final String biome,art;private final int variant;private final boolean tree;
        Flora(String biome,int variant,boolean tree,Color color){
            super(tree?new Rectangle(8,19,16,10):biome.equals("ruins")&&variant<6?new Rectangle(5,14,22,16):new Rectangle());
            this.biome=biome;this.variant=variant;this.tree=tree;art=key(biome,tree?"tree":"flora-"+variant);
            mapColor=color;toolType=biome.equals("ruins")||biome.equals("volcano")&&!tree?ToolType.PICKAXE:tree?ToolType.AXE:ToolType.ALL;
            objectHealth=tree?65:25;isLightTransparent=true;
            hoverHitbox=tree?new Rectangle(-16,-48,64,80):new Rectangle(-8,-32,48,64);
        }
        @Override public GameMessage getNewLocalization(){return new StaticMessage(name(biome)+(tree?"林木":"资源"));}
        @Override public GameTexture generateItemTexture(){return GameTexture.fromFile(art);}
        @Override public LootTable getLootTable(Level level,int layer,int x,int y){
            String item=tree?"oaklog":switch(biome){
                case "mushroom" -> variant==6?"oaklog":"mushroom";
                case "wetland" -> variant<6?"clay":"oaklog";
                case "snowfield" -> variant==5||variant==6?"snowstone":"oaklog";
                case "desert" -> variant>5?"sandstone":"oaklog";
                default -> "stone";
            };
            return new LootTable(LootItem.between(item,tree?3:1,tree?5:2));
        }
        @Override public void addDrawables(List<LevelSortedDrawable> list,OrderableDrawables ground,Level level,int x,int y,TickManager tick,GameCamera camera,PlayerMob player){
            if(!ready)return;
            int fx=camera.getTileDrawX(x)+16,fy=camera.getTileDrawY(y)+29;
            final float visualScale=VisualMaterials.scale(art,1);
            IslandVisuals.addShadow(ground,art,fx,fy,visualScale,level,x,y,tree?108:20);
            list.add(IslandVisuals.sorted(this,x,y,26,()->{
                if(tree){IslandVisuals.litBottom(key(biome,"tree-trunk"),fx,fy,visualScale,level,x,y,1);IslandVisuals.windSprite(key(biome,"tree-crown"),fx,fy,visualScale,level,x,y,true,1);}
                else if(VisualMaterials.flexible(art))IslandVisuals.windSprite(art,fx,fy,1,level,x,y,false,1);
                else IslandVisuals.litBottom(art,fx,fy,1,level,x,y,1);
            }));
        }
        @Override public void drawPreview(Level level,int x,int y,int rotation,float alpha,PlayerMob player,GameCamera camera){if(ready)IslandVisuals.litBottom(art,camera.getTileDrawX(x)+16,camera.getTileDrawY(y)+29,1,level,x,y,alpha);}
    }
    /** A three-by-two native multi-object with saved shared storage at its front center. */
    private static final class Structure extends GameObject {
        private final String biome,art;private final int variant,mx,my;private final int[] ids;
        Structure(String biome,int variant,int mx,int my,int[] ids,Color color){
            super(new Rectangle(0,my==0?4:0,32,my==0?28:20));
            this.biome=biome;this.variant=variant;this.mx=mx;this.my=my;this.ids=ids;this.art=key(biome,"structure-"+variant);
            mapColor=color;toolType=ToolType.PICKAXE;objectHealth=180;isLightTransparent=true;hoverHitbox=new Rectangle(0,-48,32,80);
        }
        private boolean master(){return mx==1&&my==1;}
        @Override public MultiTile getMultiTile(int rotation){return new StaticMultiTile(mx,my,3,2,master(),ids);}
        @Override public GameMessage getNewLocalization(){return new StaticMessage(name(biome)+"驿站 "+(variant+1));}
        @Override public GameTexture generateItemTexture(){return GameTexture.fromFile(art);}
        @Override public ObjectEntity getNewObjectEntity(Level level,int x,int y){return master()?new InventoryObjectEntity(level,x,y,20):null;}
        @Override public boolean canInteract(Level level,int x,int y,PlayerMob player){return true;}
        @Override public String getInteractTip(Level level,int x,int y,PlayerMob player,boolean debug){return "打开驿站储物";}
        @Override public void interact(Level level,int x,int y,PlayerMob player){
            if(level.isServer())OEInventoryContainer.openAndSendContainer(ContainerRegistry.OE_INVENTORY_CONTAINER,player.getServerClient(),level,x+1-mx,y+1-my);
        }
        @Override public LootTable getLootTable(Level level,int layer,int x,int y){return master()?new LootTable(LootItem.between("stone",4,6),LootItem.between("oaklog",3,5)):new LootTable();}
        @Override public void addDrawables(List<LevelSortedDrawable> list,OrderableDrawables ground,Level level,int x,int y,TickManager tick,GameCamera camera,PlayerMob player){
            if(!ready||!master())return;
            int fx=camera.getTileDrawX(x)+16,fy=camera.getTileDrawY(y)+29;
            IslandVisuals.addShadow(ground,art,fx,fy,1,level,x,y,80);
            list.add(IslandVisuals.sorted(this,x,y,26,()->IslandVisuals.litBottom(art,fx,fy,1,level,x,y,1)));
        }
        @Override public void drawPreview(Level level,int x,int y,int rotation,float alpha,PlayerMob player,GameCamera camera){if(ready&&master())IslandVisuals.litBottom(art,camera.getTileDrawX(x)+16,camera.getTileDrawY(y)+29,1,level,x,y,alpha);}
    }
    private BiomeVisuals(){}
}
