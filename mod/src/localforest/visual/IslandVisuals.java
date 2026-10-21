package localforest.visual;

import java.awt.Color;
import java.util.*;
import localforest.world.IslandWorld;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.*;
import necesse.gfx.gameTexture.GameTexture;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import necesse.gfx.res.ResourceEncoder;
import org.lwjgl.BufferUtils;
import necesse.engine.window.WindowManager;
import org.lwjgl.opengl.GL11;

import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/** Art adapter for the original Necesse object/tile systems. It never mutates inventory or simulation. */
public final class IslandVisuals {
    public static final int OX=22, OY=12;
    private static final Map<String,GameTexture> art=new HashMap<>();
    private static volatile boolean ready;
    private static final Map<String,AlphaMask> masks=new HashMap<>();
    private static final class AlphaMask { final int w,h;final int[] a;AlphaMask(BufferedImage im){w=im.getWidth();h=im.getHeight();a=im.getRGB(0,0,w,h,null,0,w);for(int i=0;i<a.length;i++)a[i]=(a[i]>>>24)&255;}}
    public static long treeDraws, objectDraws, waterDraws, terrainDraws;
    public static int flameFramesSeen;
    public static void init() { System.out.println("LOCALFOREST_VISUAL_HOOKS_REGISTERED"); }
    public static void postInit() { }
    public static void initResources() {
        String[] names={"tree-a-top","tree-b-top","tree-a-wood","tree-b-wood","crown-leafy","crown-young",
            "bush","fern","berry-bush","wildflowers","reeds","mushroom-log","flower-planter","grass","flowers",
            "rock","chest","bench","house","logs","brazier","smoke","lamp-post","timber-rack","purple-cauldron",
            "trail-sign","stone-furnace","stone-basin","smith-workbench","shore","earth","ground","dirt","shallow","deep","ocean-art","glow","mote"};
        for(String name:names) art.put(name, GameTexture.fromFile("localforest/"+name));
        for(String name:new String[]{"tree-a-top","tree-b-top","tree-a-wood","tree-b-wood","crown-leafy","crown-young","bush","fern","berry-bush","rock","chest","bench","house","logs","brazier","lamp-post","wildflowers","reeds","mushroom-log","flower-planter","grass","flowers","timber-rack","purple-cauldron","trail-sign","stone-furnace","stone-basin","smith-workbench"})
            art.put(name+"-mask",GameTexture.fromFile("localforest/"+name+"-mask"));
        for(int i=0;i<8;i++)art.put("flame-"+i,GameTexture.fromFile("localforest/flame-"+i));
        for(String key:new ArrayList<>(art.keySet()))if(key.endsWith("-mask")) {
            try(java.io.InputStream stream=ResourceEncoder.getResourceInputStream("localforest/"+key+".png")) {
                masks.put(key.substring(0,key.length()-5),new AlphaMask(ImageIO.read(stream)));
            }catch(Exception e){System.err.println("LOCALFOREST_SHADOW_MASK_UNAVAILABLE "+key+" "+e);}
        }
        ready=true; System.out.println("LOCALFOREST_ART_READY "+art.size()+" textures, native world simulation retained");
    }
    public static boolean active(Level level) {return ready&&level!=null&&IslandWorld.isIsland(level);}
    public static int variation(int x,int y) { int n=x*374761393+y*668265263; n=(n^(n>>>13))*1274126177;return n^(n>>>16); }
    public static int mod(int n,int d) { return Math.floorMod(n,d); }
    public static double seconds(Level l) {return l.getWorldEntity().getWorldTime()/1000.0;}
    public static GameLight light(Level l,int x,int y) {return l.getLightLevel(x,y).minLevelCopy(54);}
    public static boolean land(Level l,int x,int y) {return x>=0&&y>=0&&x<l.tileWidth&&y<l.tileHeight&&!l.getTile(x,y).isLiquid;}
    private static SharedTextureDrawOptions terrainBatch(LevelTileTerrainDrawOptions out,String name,int priority) {
        return out.specialDrawables.getOrCreate("localforest-"+name,priority,()->new SharedTextureDrawOptions(art.get(name)));
    }
    private static float shallowAt(Level level,int vx,int vy) {
        double nearest=2.5;
        for(int y=vy-3;y<=vy+2;y++)for(int x=vx-3;x<=vx+2;x++)if(land(level,x,y)) {
            double dx=Math.max(x-vx,Math.max(0,vx-x-1)),dy=Math.max(y-vy,Math.max(0,vy-y-1));
            nearest=Math.min(nearest,Math.hypot(dx,dy));
        }
        float value=(float)Math.max(0,1-nearest/2.5);
        return value*value*(3-2*value);
    }
    private static float[] seaColors(Level level,int worldX,int worldY,int width,int height,double time,int layer) {
        float[] colors=new float[16];
        int[] xs={worldX,worldX+width,worldX+width,worldX},ys={worldY,worldY,worldY+height,worldY+height};
        for(int i=0;i<4;i++) {
            double tx=xs[i]/32.0,ty=ys[i]/32.0;
            double front=Math.sin(tx*.115+ty*.065-time*.42),cross=Math.sin(tx*.045-ty*.09+time*.29);
            float shallow=shallowAt(level,Math.round(xs[i]/32f),Math.round(ys[i]/32f));
            colors[i*4]=(float)((layer==2?.62:.88)+(layer==2?.07:.06)*front);
            colors[i*4+1]=(float)((layer==2?.85:.9)+(layer==2?.08:.06)*cross);
            colors[i*4+2]=1;
            colors[i*4+3]=layer==1?shallow:layer==2?.58f-.22f*shallow:1;
        }
        return colors;
    }
    /** World-space UVs and common vertex colors keep neighboring tile edges identical. */
    private static void seaPatch(LevelTileTerrainDrawOptions out,String name,int priority,Level level,int tx,int ty,int dx,int dy,int offsetX,int offsetY,double time,int layer) {
        GameTexture tex=art.get(name);SharedTextureDrawOptions batch=terrainBatch(out,name,priority);
        int worldX=tx*32,worldY=ty*32;
        for(int y=0;y<32;) {
            int sy=mod(worldY+offsetY+y,tex.getHeight()),height=Math.min(32-y,tex.getHeight()-sy);
            for(int x=0;x<32;) {
                int sx=mod(worldX+offsetX+x,tex.getWidth()),width=Math.min(32-x,tex.getWidth()-sx);
                batch.addSection(sx,sx+width,sy,sy+height).advColor(seaColors(level,worldX+x,worldY+y,width,height,time,layer)).pos(dx+x,dy+y);
                x+=width;
            }
            y+=height;
        }
    }
    public static boolean terrain(GameTile tile,LevelTileTerrainDrawOptions under, LevelTileLiquidDrawOptions liquid,
            LevelTileTerrainDrawOptions over,OrderableDrawables objects,List<LevelSortedDrawable> sorted,
            Level level,int tx,int ty,GameCamera camera,TickManager tick) {
        if(!active(level))return false;
        int dx=camera.getTileDrawX(tx),dy=camera.getTileDrawY(ty),v=variation(tx,ty);
        if(tile.isLiquid) {
            double t=seconds(level);
            boolean shore=!land(level,tx,ty)&& (land(level,tx,ty-1)||land(level,tx-1,ty)||land(level,tx+1,ty)||land(level,tx,ty+1));
            seaPatch(under,"deep",0,level,tx,ty,dx,dy,0,0,t,0);
            seaPatch(under,"shallow",1,level,tx,ty,dx,dy,0,0,t,1);
            int ox=(int)(Math.sin(t*2.94)*2),oy=(int)(Math.cos(t*1.38)*2);
            seaPatch(under,"ocean-art",2,level,tx,ty,dx,dy,ox,oy,t,2);
            // The raised shoreline casts a subtle, coherent receiver-plane shadow onto the sea.
            if(land(level,tx,ty-1))terrainBatch(under,"glow",3).addFull().size(42,25).color(0.01f,.03f,.08f,.46f).pos(dx-5,dy-4);
            if(land(level,tx-1,ty))terrainBatch(under,"glow",3).addFull().size(22,40).color(.01f,.03f,.08f,.3f).pos(dx-7,dy-4);
            if(shore) {
                List<Source> shoreLights=sources(level,tx,ty);
                if(!shoreLights.isEmpty()) {
                    Source source=shoreLights.get(0);
                    float q=13f/Math.max(22,source.z);
                    int sxShadow=(int)((tx*32+16-source.x)*q*.32f),syShadow=(int)((ty*32+16-source.y)*q*.32f);
                    terrainBatch(under,"glow",4).addFull().size(42+Math.abs(sxShadow),18+Math.abs(syShadow))
                        .color(.01f,.025f,.06f,Math.min(.22f,source.strength*.22f)).pos(dx-5+sxShadow/2,dy-6+syShadow/2);
                }
            }
            waterDraws++; return true;
        }
        String id=tile.getStringID();
        boolean dirt=id.contains("dirt")||id.contains("path")||id.contains("gravel")||id.contains("farmland");
        String base=dirt?"dirt":"ground";
        int sx=mod(tx*32,64),sy=mod(ty*32,64);
        // Native LevelDrawUtils applies the smooth, four-corner tile light pass after terrain.
        // Lighting this texture again would square the brightness and reveal every 32px tile.
        terrainBatch(under,base,0).addSection(sx,sx+32,sy,sy+32).pos(dx,dy);
        if(!land(level,tx,ty+1)) {
            terrainBatch(over,"earth",1).addSection(mod(tx,2)*32,mod(tx,2)*32+32,0,7).size(32,11).pos(dx,dy+30);
            terrainBatch(over,"shore",2).addSection(mod(tx,2)*32,mod(tx,2)*32+32,0,8).size(32,4).pos(dx,dy+29);
        }
        if(!land(level,tx-1,ty))terrainBatch(over,"earth",1).addSection(0,5,0,7).size(3,33).pos(dx-2,dy+2);
        if(!land(level,tx+1,ty))terrainBatch(over,"earth",1).addSection(0,5,0,7).size(3,33).pos(dx+31,dy+2);
        // Low native-free undergrowth follows shore and trail edges; native objects retain all interaction.
        if(!dirt&&level.getObjectID(tx,ty)==0&&!houseFootprint(tx,ty)&&mod(v,11)<3) {
            String[] kinds={"fern","berry-bush","wildflowers","grass","mushroom-log","flowers","reeds"};
            String kind=kinds[mod(v>>>5,kinds.length)];
            if(kind.equals("reeds")&&land(level,tx-1,ty)&&land(level,tx+1,ty)&&land(level,tx,ty-1)&&land(level,tx,ty+1))kind="fern";
            final String k=kind; final float scale=k.equals("mushroom-log")?1.15f:k.equals("reeds")?1.25f:1.45f;
            final int x=dx+16+(mod(v>>>9,9)-4),y=dy+29;
            sorted.add(sorted(level.getObject(tx,ty),tx,ty,26,()->windSprite(k,x,y,scale,level,tx,ty,false,1)));
        }
        if(tx==52&&ty==43&&houseExists(level))addHouse(sorted,objects,level,camera,tx,ty);
        // Emitters are tied to tile positions; the sorted foot remains fixed while particles ascend.
        if(tx>=32&&tx<=59&&ty>=25&&ty<=48&&mod(v,31)==0) {
            final int tilex=tx,tiley=ty;
            sorted.add(sorted(level.getObject(tx,ty),tx,ty,27,()->motes(level,camera,tilex,tiley)));
        }
        terrainDraws++;return true;
    }
    public static boolean houseFootprint(int x,int y) {return x>=50&&x<=54&&y>=39&&y<=42;}
    public static boolean houseExists(Level level) {
        for(int x=50;x<=54;x++)for(int y=39;y<=42;y++) {
            String id=level.getObject(x,y).getStringID();if(id.contains("woodwall")||id.equals("localforesthousewall"))return true;
        }return false;
    }
    private static void addHouse(List<LevelSortedDrawable> sorted,OrderableDrawables tiles,Level level,GameCamera camera,int tx,int ty) {
        int x=camera.getTileDrawX(tx)+16,y=camera.getTileDrawY(ty)+4;
        addShadow(tiles,"house",x,y,2.75f,level,tx,ty,65);
        sorted.add(sorted(level.getObject(tx,ty),tx,ty,23,()->{
            PlayerMob local=level.getClient()==null?null:level.getClient().getPlayer();
            float opacity=local!=null&&houseFootprint(local.getTileX(),local.getTileY())?.28f:1f;
            bottom("house",x,y,2.75f,light(level,tx,ty),opacity);
            for(int i=0;i<2;i++) {
                float age=(float)((seconds(level)*.22+i*.5)%1);
                bottom("smoke",x-13+(int)(Math.sin(age*4+i)*5),y-178-(int)(age*40),1.25f+age*.3f,new GameLight(150),(float)(Math.sin(age*Math.PI)*.12));
            }
            double pulse=.94+.06*Math.sin(seconds(level)*1.2);
            art.get("glow").initDraw().size(38,46).color(1f,.57f,.15f,(float)(.12*pulse)).draw(x-18,y-55);
        }));
    }
    public static LevelSortedDrawable sorted(GameObject obj,int tx,int ty,final int sort,Runnable run) {
        return new LevelSortedDrawable(obj,tx,ty){public int getSortY(){return sort;}public void draw(TickManager tm){run.run();}};
    }
    public static void bottom(String id,int footX,int footY,float scale,GameLight lighting,float alpha) {
        GameTexture tex=art.get(id);if(tex==null)return;
        tex.initDraw().size(Math.round(tex.getWidth()*scale),Math.round(tex.getHeight()*scale)).light(lighting).alpha(alpha).draw(footX-Math.round(tex.getWidth()*scale/2),footY-Math.round(tex.getHeight()*scale));
    }
    public static void windSprite(String id,int footX,int footY,float scale,Level level,int tx,int ty,boolean crown,float alpha) {
        GameTexture tex=art.get(id);if(tex==null)return;
        SharedTextureDrawOptions rows=new SharedTextureDrawOptions(tex);
        double phase=seconds(level)*Math.PI*2/2.14+.21+Math.sin(tx*.12+ty*.08)*.02;
        float wind=(float)Math.sin(phase);
        for(int row=0;row<tex.getHeight();row++) {
            float upper=1-row/(float)Math.max(1,tex.getHeight()-1);
            float bend=tex.getWidth()*scale*wind*(crown?.008f+.034f*upper:.035f*upper*upper);
            int top=Math.round((tex.getHeight()-row)*scale), bottom=Math.round((tex.getHeight()-row-1)*scale);
            rows.addSection(0,tex.getWidth(),row,row+1).size(Math.round(tex.getWidth()*scale),Math.max(1,top-bottom)).light(light(level,tx,ty)).alpha(alpha)
                .pos(footX-Math.round(tex.getWidth()*scale/2)+Math.round(bend),footY-top);
        } rows.draw();
    }
    public static boolean object(GameObject object,List<LevelSortedDrawable> list,OrderableDrawables tileList,
            Level level,int tx,int ty,TickManager tick,GameCamera camera,PlayerMob perspective,String type) {
        if(!active(level))return false;
        if(type.equals("wall"))return houseFootprint(tx,ty);
        final int v=variation(tx,ty),x=camera.getTileDrawX(tx)+16,y=camera.getTileDrawY(ty)+29;
        final GameLight l=light(level,tx,ty);
        if(type.equals("tree")) {
            final String crown=mod(v,8)<4?(mod(v>>>5,2)==0?"tree-a-top":"tree-b-top"):mod(v,8)<6?"crown-leafy":"crown-young";
            final String wood=mod(v>>>5,2)==0?"tree-a-wood":"tree-b-wood";
            final float scale=2.3f+mod(v,5)*.06f;
            final int rootX=x+mod(v>>>5,9)-4,rootY=y+mod(v>>>11,5)-2;
            addShadow(tileList,crown,rootX,rootY,scale,level,tx,ty,62);
            list.add(sorted(object,tx,ty,23,()->{
                bottom(wood,rootX,rootY,scale,l,1);
                windSprite(crown,rootX,rootY-Math.round(art.get(wood).getHeight()*scale*.55f),scale,level,tx,ty,true,1);
            })); treeDraws++;return true;
        }
        String id=type;float scale=2f;
        if(type.equals("bush")) {id=mod(v,10)<5?"bush":mod(v,10)<8?"berry-bush":"fern";scale=id.equals("bush")?1.95f:id.equals("berry-bush")?1.82f:1.65f;}
        if(type.equals("grass")){id=mod(v,4)==0?"reeds":mod(v,4)==1?"fern":"grass";scale=1.4f;}
        if(type.equals("flowers")){id=mod(v,2)==0?"flowers":"wildflowers";scale=1.5f;}
        if(type.equals("inventory")) {
            String stringID=object.getStringID();
            if(!stringID.contains("chest"))return false;id="chest";scale=2f;
        }
        if(type.equals("campfire")) {
            final boolean fueled=isFueled(level,tx,ty);
            addShadow(tileList,"logs",x,y,1.45f,level,tx,ty,10);
            list.add(sorted(object,tx,ty,22,()->{
                bottom("logs",x,y,1.45f,l,1);
                if(fueled) {
                    int f=mod((int)(level.getWorldEntity().getWorldTime()/100)+mod(v,8),8);flameFramesSeen|=1<<f;
                    bottom("flame-"+f,x,y-5,1.35f,new GameLight(150),1);
                    double life=(seconds(level)*.32+mod(v,100)*.01)%1;
                    bottom("smoke",x+(int)(Math.sin(life*4+v)*4),y-20-(int)(life*31),1.15f,new GameLight(150),(float)(Math.sin(life*Math.PI)*.24));
                }
            })); objectDraws++;return true;
        }
        if(type.equals("torch")){id="lamp-post";scale=1.8f;}
        if(!art.containsKey(id))return false;
        final String artID=id;final float drawScale=scale;
        addShadow(tileList,id,x,y,scale,level,tx,ty,Math.min(48,art.get(id).getHeight()*scale));
        list.add(sorted(object,tx,ty,23,()->{
            if(type.equals("bush")||type.equals("grass")||type.equals("flowers"))windSprite(artID,x,y,drawScale,level,tx,ty,false,1);
            else bottom(artID,x,y,drawScale,l,1);
        })); objectDraws++;return true;
    }
    public static boolean isFueled(Level level,int tx,int ty) {
        ObjectEntity e=level.entityManager.getObjectEntity(tx,ty);
        return e instanceof FueledInventoryObjectEntity&&((FueledInventoryObjectEntity)e).isFueled();
    }
    private static void motes(Level level,GameCamera camera,int tx,int ty) {
        float hour=level.getWorldEntity().getDayTimeHourFloat();
        if(hour>=6&&hour<18)return;
        float night=Math.min(1,Math.max(0,hour>=18?(hour-18):6-hour));
        double t=seconds(level);int h=variation(tx,ty);
        for(int i=0;i<3;i++) {
            double lifetime=2.2+mod(h+i*7,72)/100.0,cycle=lifetime+.9;
            double age=(t+mod(h+i*997,997)/997.0*cycle)%cycle;
            if(age>=lifetime)continue;
            double p=age/lifetime;
            float a=night*(float)Math.min(1,p/.2)*(float)Math.min(1,(1-p)/.45);
            int x=camera.getTileDrawX(tx)+mod(h+i*13,31)+(int)(Math.sin(age+i)*1.5),y=camera.getTileDrawY(ty)+25-(int)(age*(5+mod(h>>>8,4)));
            int size=3+mod(h+i,2);
            art.get("mote").initDraw().size(size*4,size*4).color(.63f,1f,.28f,a*.17f).draw(x-size*2,y-size*2);
            art.get("mote").initDraw().size(size,size).color(.86f,1f,.38f,a*.85f).draw(x-size/2,y-size/2);
        }
    }
    private static final class Source {float x,y,z,strength;Source(float x,float y,float z,float strength){this.x=x;this.y=y;this.z=z;this.strength=strength;}}
    private static final Map<Level,LightSnapshot> sourceCache=new WeakHashMap<>();
    private static final class LightSnapshot { long time; final List<Source> lights=new ArrayList<>(); }
    private static synchronized List<Source> allSources(Level level) {
        long now=level.getWorldEntity().getWorldTime();
        LightSnapshot snapshot=sourceCache.get(level);
        if(snapshot!=null&&now>=snapshot.time&&now-snapshot.time<120)return snapshot.lights;
        snapshot=new LightSnapshot(); snapshot.time=now;
        for(int yy=0;yy<level.tileHeight;yy++)for(int xx=0;xx<level.tileWidth;xx++) {
            GameObject o=level.getObject(xx,yy);int brightness=o.getLightLevel(level,0,xx,yy);
            if(brightness>0)snapshot.lights.add(new Source(xx*32+16,yy*32+12,o.getStringID().contains("torch")?48:28,brightness/100f));
        }
        sourceCache.put(level,snapshot); return snapshot.lights;
    }
    public static List<Source> sources(Level level,int tx,int ty) {
        ArrayList<Source> found=new ArrayList<>();
        for(Source source:allSources(level)){
            double d=Math.hypot(source.x/32-(tx+.5),source.y/32-(ty+.5));
            if(d<.6||d>7)continue;
            found.add(new Source(source.x,source.y,source.z,(float)(source.strength*Math.pow(1-d/8,1.5))));
        }
        found.sort((a,b)->Float.compare(b.strength,a.strength));
        if(found.size()>2)found.subList(2,found.size()).clear();
        if(found.isEmpty()) {
            double angle=(level.getWorldEntity().getDayTimeHourFloat()-6)/12*Math.PI;
            found.add(new Source(tx*32+16-(float)Math.cos(angle)*300,ty*32+29-150-(float)Math.sin(angle)*80,200,.34f));
        }
        return found;
    }
    private static final Map<OrderableDrawables,ShadowGroup> shadowGroups=new WeakHashMap<>();
    private static GameTexture shadowTexture;
    private static ByteBuffer shadowUpload;
    private static final int SHADOW_RES=2;
    private static final class ShadowStrip {
        final AlphaMask mask;final int row,x,y,w,h,alpha;
        ShadowStrip(AlphaMask mask,int row,int x,int y,int w,int h,int alpha){this.mask=mask;this.row=row;this.x=x;this.y=y;this.w=w;this.h=h;this.alpha=alpha;}
    }
    private static final class ShadowGroup {
        final List<ShadowStrip> strips=Collections.synchronizedList(new ArrayList<>());
        void draw() {
            int sceneW=WindowManager.getWindow().getSceneWidth(),sceneH=WindowManager.getWindow().getSceneHeight();
            int width=(sceneW+SHADOW_RES-1)/SHADOW_RES,height=(sceneH+SHADOW_RES-1)/SHADOW_RES;
            if(width<1||height<1)return;
            int[] coverage=new int[width*height];
            synchronized(strips) {
                for(ShadowStrip strip:strips) {
                    int minY=Math.max(0,Math.floorDiv(strip.y,SHADOW_RES)),maxY=Math.min(height,(strip.y+strip.h+SHADOW_RES-1)/SHADOW_RES);
                    if(minY>=maxY)continue;
                    for(int sourceX=0;sourceX<strip.mask.w;sourceX++) {
                        int alpha=Math.max(strip.mask.a[strip.row*strip.mask.w+sourceX],strip.mask.a[Math.min(strip.mask.h-1,strip.row+1)*strip.mask.w+sourceX])*strip.alpha/255;
                        if(alpha==0)continue;
                        int start=strip.x+sourceX*strip.w/strip.mask.w,end=strip.x+(sourceX+1)*strip.w/strip.mask.w;
                        int minX=Math.max(0,Math.floorDiv(start,SHADOW_RES)),maxX=Math.min(width,(end+SHADOW_RES-1)/SHADOW_RES);
                        for(int yy=minY;yy<maxY;yy++)for(int xx=minX;xx<maxX;xx++) {
                            int at=yy*width+xx;if(alpha>coverage[at])coverage[at]=alpha;
                        }
                    }
                }
            }
            if(shadowTexture==null||shadowTexture.getWidth()!=width||shadowTexture.getHeight()!=height) {
                if(shadowTexture!=null)shadowTexture.delete();
                shadowTexture=new GameTexture("localforest-shadow-union",width,height);
                shadowTexture.setBlendQuality(GameTexture.BlendQuality.LINEAR);
                shadowUpload=BufferUtils.createByteBuffer(width*height*4);
            }
            shadowUpload.clear();
            for(int alpha:coverage)shadowUpload.put((byte)0).put((byte)0).put((byte)0).put((byte)alpha);
            shadowUpload.flip();shadowTexture.bindTexture();
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,shadowUpload);
            shadowTexture.initDraw().size(sceneW,sceneH).draw(0,0);
        }
    }
    private static synchronized ShadowGroup shadowGroup(OrderableDrawables tiles) {
        ShadowGroup group=shadowGroups.get(tiles);
        if(group==null) {
            group=new ShadowGroup();shadowGroups.put(tiles,group);
            final ShadowGroup draw=group;tiles.add(-100,tm->draw.draw());
        }
        return group;
    }
    /** Max-union alpha silhouettes on a receiver plane, composed once without changing framebuffer state. */
    private static void addShadow(OrderableDrawables tiles,String id,int fx,int fy,float scale,Level level,int tx,int ty,float height) {
        AlphaMask mask=masks.get(id);if(mask==null)return;
        List<Source> lights=sources(level,tx,ty);
        final int rootWX=tx*32+16,rootWY=ty*32+29;
        ShadowGroup group=shadowGroup(tiles);
        for(Source source:lights)for(int row=0;row<mask.h;row+=2) {
            float upper=1-row/(float)mask.h,nextUpper=1-Math.min(mask.h,row+2)/(float)mask.h;
            float q=Math.min(2.2f,height*upper/Math.max(26,source.z));
            float nextQ=Math.min(2.2f,height*nextUpper/Math.max(26,source.z));
            int px=(int)((rootWX-source.x)*q*.52f),py=(int)((rootWY-source.y)*q*.52f);
            int nextPY=(int)((rootWY-source.y)*nextQ*.52f);
            int width=Math.round(mask.w*scale*(1+q*.12f));
            int stripHeight=Math.max(2,Math.abs(py-nextPY)+2);
            int alpha=Math.round(Math.min(.19f,source.strength*.19f)*(1-upper*.3f)*255);
            group.strips.add(new ShadowStrip(mask,row,fx+px-width/2,fy+Math.min(py,nextPY)-2,width,stripHeight,alpha));
        }
    }
}
