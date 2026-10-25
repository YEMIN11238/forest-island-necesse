package localforest.visual;

import java.awt.Point;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import javax.imageio.ImageIO;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.ObjectUserActive;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.furniture.SettlerBedObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/** Original resident body across the native lifecycle. Reads animation state only. */
public final class CompanionSprites {
    public enum State { IDLE,WALK,WORK,ATTACK,CARRY,DOWNED,SLEEP,OBJECT_USE }
    private static final String[] DIRS={"up","right","down","left"};
    private static final int FRAME=64,SCALE=2,FOOT_X=32,FOOT_Y=52;
    private static final Map<String,GameTexture> textures=new HashMap<>();
    private static final Map<String,LongAdder> appearanceDraws=new java.util.concurrent.ConcurrentHashMap<>();
    private static final EnumMap<State,LongAdder> draws=new EnumMap<>(State.class);
    static{for(State state:State.values())draws.put(state,new LongAdder());}
    public static volatile long standFallbacks,toolDraws,objectUserDelegations;

    private static final Map<Integer,Map<String,Object>> bodyAudit=new java.util.concurrent.ConcurrentHashMap<>();
    public static Map<Integer,Map<String,Object>> renderAudit(){return Map.copyOf(bodyAudit);}
    public static void initResources(){
        textures.clear();
        ResidentPortraits.initResources();
        for(String role:new String[]{"farmer","carpenter","merchant","ashuo","luan"}){
            for(String dir:DIRS){load(role+"-idle-"+dir,1);load(role+"-walk-"+dir,4);load(role+"-attack-"+dir,6);}
            // Old work/carry/sleep/downed packs depict different people. These
            // states use this exact new family's directional idle until authored.
        }
        for(String role:new String[]{"farmer","carpenter","merchant","ashuo","luan"})for(String dir:DIRS)for(String action:new String[]{"idle","walk","attack"})
            if(!textures.containsKey(role+"-"+action+"-"+dir))throw new IllegalStateException("Missing core character art: "+role+"/"+action+"/"+dir);
        System.out.println("MOSSLUME_COMPANION_SPRITES_READY stateAssets="+textures.size()+" frame=64 foot=32,52 scale=2");
    }
    private static void load(String key,int frames){
        String path="mosslume/residents/"+key;
        try(var input=ResourceEncoder.getResourceInputStream(path+".png")){
            var image=ImageIO.read(input);
            if(image==null||image.getWidth()!=FRAME*frames||image.getHeight()!=FRAME)
                throw new IllegalArgumentException("Expected "+(FRAME*frames)+"x"+FRAME);
            GameTexture texture=GameTexture.fromFile(path);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);textures.put(key,texture);
        }catch(java.io.FileNotFoundException missing){
            // Optional batch assets: retain this resident's original stand sprite.
        }catch(Exception invalid){System.err.println("MOSSLUME_COMPANION_SPRITE_SKIPPED key="+key+" error="+invalid);}
    }
    public static Map<String,Object> counters(){
        Map<String,Object> out=new LinkedHashMap<>();for(State state:State.values())out.put(state.name().toLowerCase(),draws.get(state).sum());
        out.put("standFallbacks",standFallbacks);out.put("nativeToolLayers",toolDraws);out.put("objectUserDelegations",objectUserDelegations);out.put("stateAssets",textures.size());out.put("weaponBinding",CharacterAnimation.weaponCounters());out.put("portraits",ResidentPortraits.counters());
        Map<String,Object> appearances=new LinkedHashMap<>();appearanceDraws.forEach((role,count)->appearances.put(role,count.sum()));out.put("appearances",appearances);return out;
    }
    public static void contactShadow(HumanMob mob,OrderableDrawables tiles,Level level,int x,int y,GameCamera camera){
        if(!mob.isVisible()||mob.isRiding()||mob.getUsingObject()!=null||mob.buffManager.getModifier(BuffModifiers.INVISIBILITY))return;
        GameTexture texture=MobRegistry.Textures.human_shadow;if(texture==null)return;
        int size=texture.getHeight();GameLight light=SceneLighting.light(level,x,y);
        var draw=texture.initDraw().sprite(Math.floorMod(mob.getDir(),4),0,size).light(light)
            .pos(camera.getDrawX(x)-size/2,camera.getDrawY(y)-size/2);
        tiles.add(tick->draw.draw());
    }
    public static boolean draw(HumanMob mob,List<MobDrawable> list,Level level,int x,int y,GameCamera camera,PlayerMob player,
        InventoryItem attacking,InventoryItem held,long heldUntil){
        if(!MosslumeArt.active(level))return false;
        if(!mob.isVisible())return true;
        ObjectUserActive user=mob.getUsingObject();
        // Bed/chair drawables call getUserDrawOptions themselves. Drawing here as well
        // would duplicate the body and put it in the wrong depth layer.
        if(user!=null&&!user.drawsUser()){objectUserDelegations++;return true;}
        DrawOptions options=options(mob,level,x,y,camera,player,attacking,held,heldUntil,false);
        if(options!=null)list.add(new MobDrawable(){@Override public void draw(TickManager tick){options.draw();}});
        return true;
    }
    public static DrawOptions objectUser(HumanMob mob,Level level,int x,int y,GameCamera camera,PlayerMob player,
        InventoryItem attacking,InventoryItem held,long heldUntil){
        if(!MosslumeArt.active(level))return null;
        if(!mob.isVisible())return ()->{};
        return options(mob,level,x,y,camera,player,attacking,held,heldUntil,true);
    }
    private static String role(HumanMob mob){
        return appearanceRole(mob.getStringID());
    }
    public static String appearanceRole(String id){
        // Native profession ID is present in ordinary spawn packets as well as saves.
        // Server-only companion UUID metadata must not determine a client-side body.
        if(id.contains("guard"))return "ashuo";
        if(id.contains("alchemist"))return "luan";
        if(id.contains("farmer")||id.contains("stylist")||id.contains("animalkeeper"))return "farmer";
        if(id.contains("merchant")||id.contains("trader")||id.contains("angler")||id.contains("hunter")||id.contains("explorer")||id.contains("pawnbroker")||id.contains("pirate"))return "merchant";
        return "carpenter";
    }
    private static State state(HumanMob mob,InventoryItem attacking,InventoryItem held,long heldUntil,boolean objectDraw){
        if(mob.isDowned())return State.DOWNED;
        if(mob.getUsingObject()!=null){if(mob.getUsingObject().object instanceof SettlerBedObject)return State.SLEEP;return State.OBJECT_USE;}
        if(mob.isAttacking){
            String id=attacking==null?"":attacking.item.getStringID();
            return id.contains("spriteattack")||id.contains("sickle")||id.endsWith("axe")||id.endsWith("shovel")||id.endsWith("hoe")?State.WORK:State.ATTACK;
        }
        if((held!=null&&mob.getLocalTime()<=heldUntil)||!mob.getWorkInventory().isEmpty())return State.CARRY;
        return moving(mob)?State.WALK:State.IDLE;
    }
    private static boolean moving(HumanMob mob){return Math.abs(mob.dx)+Math.abs(mob.dy)>2;}
    private static DrawOptions options(HumanMob mob,Level level,int x,int y,GameCamera camera,PlayerMob player,
        InventoryItem attacking,InventoryItem held,long heldUntil,boolean objectDraw){
        int dir=mob.isAttacking&&mob.attackDir!=null?CharacterAnimation.direction(mob.attackDir.x,mob.attackDir.y,mob.getDir()):Math.floorMod(mob.getDir(),4);String role=role(mob);State state=state(mob,attacking,held,heldUntil,objectDraw);
        boolean walking=state==State.WALK||(state==State.CARRY&&moving(mob));
        String key=role+switch(state){
            case WALK->"-walk-"+DIRS[dir];case WORK,ATTACK->"-attack-"+DIRS[dir];case CARRY->walking?"-walk-"+DIRS[dir]:"-carry";
            case DOWNED->"-downed";case SLEEP->"-sleep";default->"-idle-"+DIRS[dir];
        };
        String resolvedKey=key;GameTexture chosen=textures.get(key);boolean missing=chosen==null;
        if(chosen==null){resolvedKey=role+"-idle-"+DIRS[dir];chosen=textures.get(resolvedKey);}
        if(chosen==null){resolvedKey=role+"-"+DIRS[dir];chosen=MosslumeArt.resident(role,dir);}
        // A missing new pose must never invoke the engine's different original human body.
        if(chosen==null)return ()->{};
        final GameTexture body=chosen;final boolean fallback=missing;final String actualKey=resolvedKey;
        final float progress=mob.isAttacking?Math.max(0,Math.min(1,mob.getAttackAnimProgress())):0;
        final int frame=mob.isAttacking?CharacterAnimation.attackFrame(attacking,progress,body.getWidth()/FRAME):walking&&body.getWidth()>FRAME?Math.floorMod((int)(mob.getDistanceRan()/12.0),body.getWidth()/FRAME):0;
        int footX=camera.getDrawX(x),footY=camera.getDrawY(y);
        float angle=0;int pivotX=FOOT_X*SCALE,pivotY=FOOT_Y*SCALE;
        if(state==State.SLEEP&&mob.getUsingObject()!=null){
            ObjectUserActive user=mob.getUsingObject();Point appearance=user.getUserAppearancePos();
            // Center the authored body on the bed's native visual anchor; retain the
            // owning bed's sorting/occlusion instead of adding a second world drawable.
            footX=camera.getDrawX(appearance.x);footY=camera.getDrawY(appearance.y)+32;
            int rotation=Math.floorMod(level.getObjectRotation(user.tileX,user.tileY),4);
            angle=rotation==1?-90:rotation==2?180:rotation==3?90:0;pivotY=36*SCALE;
        }else if(state==State.DOWNED&&fallback){angle=90;pivotY=44*SCALE;}
        final int drawX=footX-FOOT_X*SCALE,drawY=footY-FOOT_Y*SCALE;
        final int rotateX=pivotX,rotateY=pivotY;final float rotation=angle;
        final GameLight light=SceneLighting.light(level,x,y);
        DrawOptions bodyDraw=rotation!=0||state==State.SLEEP||state==State.DOWNED?
            ()->body.initDraw().sprite(frame,0,FRAME).size(FRAME*SCALE,FRAME*SCALE).rotate(rotation,rotateX,rotateY).light(light).draw(drawX,drawY):
            CharacterAnimation.body(body,frame,FRAME,SCALE,footX,footY,mob,light,state==State.IDLE,player);
        DrawOptions tool=state==State.DOWNED||state==State.SLEEP?null:tool(mob,level,footX,footY,light,attacking,held,heldUntil,dir,role,frame,player);
        DrawOptions palm=mob.isAttacking&&tool!=null?CharacterAnimation.palm(body,frame,FRAME,SCALE,footX,footY,mob,light,role,dir):null;
        DrawOptions marker=mob.getMarkerDrawOptions(x,y,light,camera,0,state==State.DOWNED?-25:-69,player);
        return ()->{
            if(dir==0&&tool!=null){tool.draw();toolDraws++;}
            bodyDraw.draw();
            if(dir!=0&&tool!=null){tool.draw();toolDraws++;}
            if(palm!=null)palm.draw();
            if(Boolean.getBoolean("mosslume.visualProbe.weaponMatrix")||Boolean.getBoolean("mosslume.visualProbe.npcAudit"))bodyAudit.put(mob.getUniqueID(),Map.of("mobId",mob.getUniqueID(),"nativeType",mob.getStringID(),"role",role,"state",state.name(),"actualTexture","mosslume/residents/"+actualKey+".png","frame",frame,"worldX",x,"worldY",y,"objectUser",objectDraw));
            marker.draw();draws.get(state).increment();appearanceDraws.computeIfAbsent(role,k->new LongAdder()).increment();if(fallback)standFallbacks++;MosslumeArt.residentDraws++;
        };
    }
    private static DrawOptions tool(HumanMob mob,Level level,int footX,int footY,GameLight light,InventoryItem attacking,InventoryItem held,long heldUntil,int dir,String actor,int bodyFrame,PlayerMob perspective){
        if(mob.isAttacking&&attacking!=null&&mob.attackDir!=null){
            float progress=Math.max(0,Math.min(1,mob.getAttackAnimProgress()));
            return CharacterAnimation.weapon(mob,attacking,dir,progress,footX,footY,light,perspective,actor,bodyFrame,SCALE);
        }
        InventoryItem item=held!=null&&mob.getLocalTime()<=heldUntil?held:null;
        if(item==null&&!mob.getWorkInventory().isEmpty())item=mob.getWorkInventory().stream().findFirst().orElse(null);
        if(item==null){var slot=mob.getCurrentSelectedAttackSlot();if(slot!=null&&slot.getItem()!=null&&mob.itemAttackerHoldsItem(slot.getItem()))item=slot.getItem();}
        if(item==null)return null;
        final InventoryItem heldItem=item;
        return ()->heldItem.item.getItemSprite(heldItem,null).initDraw().size(20).light(light).draw(footX+(dir==3?-22:2),footY-35);
    }
    private CompanionSprites(){}
}
