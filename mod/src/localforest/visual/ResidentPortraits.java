package localforest.visual;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import javax.imageio.ImageIO;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.QuestGiver;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.level.maps.Level;

/** Heads are exact current idle pixels, never a second original-game HumanLook. */
public final class ResidentPortraits {
    private record Images(GameTexture square,GameTexture nativeHeadAtlas){}
    private static final Map<String,Images> images=new LinkedHashMap<>();
    private static final Map<String,Map<String,Object>> sources=new LinkedHashMap<>();
    private static final Map<String,LongAdder> draws=new ConcurrentHashMap<>();
    static BufferedImage headSquare(BufferedImage idle){
        if(idle.getWidth()!=64||idle.getHeight()!=64)throw new IllegalArgumentException("Portrait requires current 64px idle frame");
        int top=64;
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)if((idle.getRGB(x,y)>>>24)>=128)top=Math.min(top,y);
        if(top==64)throw new IllegalArgumentException("Empty current character idle");
        int bottom=Math.min(64,top+18),left=64,right=0;
        for(int y=top;y<bottom;y++)for(int x=0;x<64;x++)if((idle.getRGB(x,y)>>>24)>=128){left=Math.min(left,x);right=Math.max(right,x+1);}
        if(right-left>32)throw new IllegalArgumentException("Current head too wide for native face slot");
        BufferedImage square=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        int px=(32-(right-left))/2,py=(32-(bottom-top))/2;
        // Exact pixels and alpha, no resampling/repainting or new image generation.
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)square.setRGB(px+x-left,py+y-top,idle.getRGB(x,y));
        return square;
    }
    static BufferedImage nativeAtlas(BufferedImage square){
        BufferedImage atlas=new BufferedImage(64,256,BufferedImage.TYPE_INT_ARGB);
        // Settler.getHumanFaceDrawOptions subtracts (16,12) from a 64px
        // frame and always selects sprite(0,2); keep all four rows valid.
        for(int row=0;row<4;row++)for(int y=0;y<32;y++)for(int x=0;x<32;x++)atlas.setRGB(16+x,row*64+12+y,square.getRGB(x,y));
        return atlas;
    }
    public static void initResources(){
        images.clear();sources.clear();
        for(String role:new String[]{"farmer","carpenter","merchant","ashuo","luan"}){
            String path="mosslume/residents/"+role+"-idle-down.png";
            try(InputStream input=ResourceEncoder.getResourceInputStream(path)){
                byte[] bytes=input.readAllBytes();BufferedImage idle=ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                BufferedImage square=headSquare(idle);GameTexture icon=new GameTexture("Mosslume current head "+role,square);
                GameTexture atlas=new GameTexture("Mosslume native-compatible current head "+role,nativeAtlas(square));
                icon.setBlendQuality(GameTexture.BlendQuality.NEAREST);atlas.setBlendQuality(GameTexture.BlendQuality.NEAREST);
                images.put(role,new Images(icon,atlas));sources.put(role,Map.of("source",path,"sourceSha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),"sourcePixelsPreserved",true));
            }catch(Exception error){throw new IllegalStateException("Current resident portrait "+role,error);}
        }
    }
    public static HumanDrawOptions faceOptions(HumanMob mob,String caller){
        if(!MosslumeArt.active(mob.getLevel()))return null;
        String role=CompanionSprites.appearanceRole(mob.getStringID());Images source=images.get(role);
        if(source==null)throw new IllegalStateException("Missing current resident head "+role);
        draws.computeIfAbsent(caller+":"+role,k->new LongAdder()).increment();
        return new HumanDrawOptions(mob.getLevel()).headTexture(source.nativeHeadAtlas()).drawEyes(false);
    }
    public static DrawOptions icon(Level level,String type,int size,int x,int y,String caller){
        if(!MosslumeArt.active(level)||type==null||(!type.endsWith("human")&&!type.equals("human")))return null;
        String role=CompanionSprites.appearanceRole(type);Images source=images.get(role);
        if(source==null)throw new IllegalStateException("Missing current resident head "+role);
        return ()->{source.square().initDraw().size(size,size).draw(x,y);draws.computeIfAbsent(caller+":"+role,k->new LongAdder()).increment();};
    }
    public static boolean drawOnMap(HumanMob mob,TickManager tick,Client client,int x,int y){
        DrawOptions face=icon(mob.getLevel(),mob.getStringID(),16,x-8,y-8,"map");if(face==null)return false;
        face.draw();var marker=mob.getMarkerOptions(client.getPlayer());
        if(marker!=null&&Settings.showQuestMarkers)QuestGiver.getMarkerDrawOptions(marker.icons,20,marker.color,x,y-5,1f,-1,0).draw();
        return true;
    }
    public static Map<String,Object> counters(){
        Map<String,Object> counts=new LinkedHashMap<>();draws.forEach((key,value)->counts.put(key,value.sum()));
        return Map.of("draws",counts,"sources",Map.copyOf(sources));
    }
    private ResidentPortraits(){}
}
