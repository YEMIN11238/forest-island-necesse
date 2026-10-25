package localforest.visual;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.eclipsesource.json.Json;
import javax.imageio.ImageIO;
import necesse.gfx.drawOptions.ArrayDrawOptions;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsMods;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.axeToolItem.AxeToolItem;
import necesse.inventory.item.toolItem.swordToolItem.WoodSwordToolItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.WoodBowProjectileToolItem;

/** Uses real native item setup, rotation and pos-to-quad geometry; no GL draw or hit logic mocked. */
public final class AttackHandSocketsTest {
    private static final String[] DIRS={"up","right","down","left"};
    private static Object get(Object object,String name)throws Exception{
        Class<?> type=object.getClass();
        while(type!=null){try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(object);}catch(NoSuchFieldException missing){type=type.getSuperclass();}}
        throw new NoSuchFieldException(name);
    }
    private static float number(Object object,String name)throws Exception{return ((Number)get(object,name)).floatValue();}
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of("");Map<String,AttackHandSockets.Grip[][]> sockets;
        try(InputStream input=Files.newInputStream(root.resolve("mod/resources/mosslume/characters/attack-hand-sockets.properties"))){sockets=AttackHandSockets.read(input);}
        var metadata=Json.parse(Files.readString(root.resolve("mosslume/art/hand-grips-r8/hand-sockets.json"))).asObject().get("actors").asObject();
        for(String actor:metadata.names())for(var entryValue:metadata.get(actor).asArray()){
            var entry=entryValue.asObject();Path strip=root.resolve(entry.getString("strip",""));
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(strip)));
            check(digest.equals(entry.getString("sha256","")),"Body art changed without re-reviewing its hands: "+strip);
            int direction=List.of(DIRS).indexOf(entry.getString("direction",""));
            for(int frame=0;frame<6;frame++){
                var point=entry.get("hands").asArray().get(frame).asArray();var grip=AttackHandSockets.at(sockets,actor,direction,frame);
                check(grip.x()==point.get(0).asInt()&&grip.y()==point.get(1).asInt(),"Runtime hand metadata differs from reviewed source "+actor);
            }
        }
        check(sockets.keySet().containsAll(Set.of("player","ashuo","luan","farmer","carpenter","merchant")),"All six visible families need reviewed hands");
        int reviewed=0;
        for(String actor:sockets.keySet())for(int dir=0;dir<4;dir++){
            Path file=root.resolve("mod/resources/mosslume/"+(actor.equals("player")?"players/":"residents/")+actor+"-attack-"+DIRS[dir]+".png");
            BufferedImage image=ImageIO.read(file.toFile());check(image.getWidth()==384&&image.getHeight()==64,"Wrong body strip "+file);
            for(int frame=0;frame<6;frame++){
                var hand=AttackHandSockets.at(sockets,actor,dir,frame);
                check((image.getRGB(frame*64+hand.x(),hand.y())>>>24)>=128,"Hand outside visible body: "+actor+" "+dir+" "+frame);reviewed++;
            }
        }
        check(sockets.containsKey("player")&&sockets.containsKey("ashuo")&&sockets.containsKey("luan"),"Core hand metadata missing");
        necesse.engine.registries.GNDRegistry.instance.registerCore();
        GameTexture texture=new GameTexture("offline native attack geometry",32,32);
        Item sword=new WoodSwordToolItem(){@Override public GameSprite getAttackSprite(InventoryItem item,necesse.entity.mobs.PlayerMob player){return new GameSprite(texture);}};
        Item axe=new AxeToolItem(100,null){@Override public GameSprite getAttackSprite(InventoryItem item,necesse.entity.mobs.PlayerMob player){return new GameSprite(texture);}};
        Item bow=new WoodBowProjectileToolItem(){@Override public GameSprite getAttackSprite(InventoryItem item,necesse.entity.mobs.PlayerMob player){return new GameSprite(texture);}};
        int checks=0;
        float[][] aims={{0,-1},{1,0},{0,1},{-1,0}};
        for(String actor:sockets.keySet())for(Item item:List.of(sword,axe,bow))for(int charge=0;charge<(item==bow?3:1);charge++)for(int dir=0;dir<4;dir++)for(float progress:new float[]{0,.08f,.24f,.42f,.50f,.68f,.86f,.999f}){
            InventoryItem stack=new InventoryItem(item);
            if(charge>0){stack.getGndData().setBoolean("charging",true);stack.getGndData().setFloat("chargePercent",charge*.5f);stack.getGndData().setFloat("chargedArm",charge*.5f);}
            check(CharacterAnimation.attackFrame(stack,progress,6)==(item==bow?3:CharacterAnimation.frame(progress,6)),"Bow aims rather than using melee windup");
            ItemAttackDrawOptions options=(ItemAttackDrawOptions)item.getAttackDrawOptions(stack,null,null,null,null,null,dir,aims[dir][0],aims[dir][1],progress,null);
            @SuppressWarnings("unchecked") List<Object> sprites=(List<Object>)get(options,"itemSprites");
            check(sprites.size()==1,"Unexpected duplicate weapon layer");
            Object sprite=sprites.get(0);float rx=number(sprite,"rotateX"),ry=number(sprite,"rotateY");
            float width=number(sprite,"width"),height=number(sprite,"height");
            float originalRotation=number(options,"rotation");
            CharacterAnimation.bindNativeGrip(options,dir);
            check(number(options,"rotation")==originalRotation,"Native sword/axe/bow angle changed");
            var hand=AttackHandSockets.at(sockets,actor,dir,CharacterAnimation.attackFrame(stack,progress,6));
            // Nonzero translated/camera-relative feet exercise walking attacks as well as standing.
            int hx=hand.worldX(533+(int)(progress*197),2),hy=hand.worldY(-731+(int)(progress*71),2);
            int[] offset=CharacterAnimation.nativeRawPivotOffset(dir);
            ArrayDrawOptions draw=(ArrayDrawOptions)options.pos(hx-offset[0],hy-offset[1]);
            DrawOptions[] children=(DrawOptions[])get(draw,"options");check(children.length==1,"Body/arm or weapon was duplicated");
            Object quad=get(children[0],"opts");
            float u=(dir==3?width-rx:rx)/width,v=(height-ry+(dir==2?1:0))/height;
            float x1=number(quad,"x1"),y1=number(quad,"y1");
            float x=x1+(number(quad,"x2")-x1)*u+(number(quad,"x4")-x1)*v;
            float y=y1+(number(quad,"y2")-y1)*u+(number(quad,"y4")-y1)*v;
            check(Math.abs(x-hx)<.02&&Math.abs(y-hy)<.02,"Native grip left authored hand: "+actor+" dir"+dir+" p"+progress+" error="+(x-hx)+","+(y-hy));checks++;
        }
        System.out.println("AttackHandSocketsTest PASS: "+reviewed+" visible reviewed sockets; "+checks+" actual native axe/sword/bow quads incl. charge, four-way aim and translated feet; angles preserved; one weapon, zero native arm/body duplicates");
    }
}
