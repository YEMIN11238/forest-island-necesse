package localforest.visual;

import java.lang.reflect.Field;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.itemAttack.HumanAttackDrawOptions;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/** Native attack time is authoritative; only the authored body and visual layers change.
 * Frame selection follows libGDX Animation's clamped NORMAL playback (Apache-2.0).
 * See resources/third-party/libgdx and third-party/libgdx-render/ANIMATION-PORT.md.
 */
public final class CharacterAnimation {
    private static final Field ACTIVE_ITEM;
    static {
        try { ACTIVE_ITEM=ItemAttackerMob.class.getDeclaredField("attackingItem");ACTIVE_ITEM.setAccessible(true); }
        catch(ReflectiveOperationException error){throw new ExceptionInInitializerError(error);}
    }
    public static int frame(float progress,int count){
        if(count<=1)return 0;
        if(!Float.isFinite(progress))return 0;
        return Math.min(count-1,Math.max(0,(int)(progress*count)));
    }
    /** A held bow aims from an extended hand, rather than borrowing axe windup. */
    public static int attackFrame(InventoryItem item,float progress,int count){
        return item!=null&&item.item instanceof BowProjectileToolItem?Math.min(Math.max(0,count-1),3):frame(progress,count);
    }
    public static int direction(float dx,float dy,int fallback){
        if(!Float.isFinite(dx)||!Float.isFinite(dy)||Math.abs(dx)+Math.abs(dy)<.001f)return Math.floorMod(fallback,4);
        return Math.abs(dx)>Math.abs(dy)?dx>=0?1:3:dy>=0?2:0;
    }
    public static InventoryItem activeItem(ItemAttackerMob mob){
        if(!mob.isAttacking)return null;
        try{return (InventoryItem)ACTIVE_ITEM.get(mob);}
        catch(IllegalAccessException error){throw new IllegalStateException("Native attack item access",error);}
    }
    public static double phase(int identity){return (Integer.toUnsignedLong(identity*0x45d9f3b)%65536)/65536.0*Math.PI*2;}
    public static float breath(double time,int identity){return (float)Math.sin(time*1.6+phase(identity))*1.25f;}

    /** Bends only above the hips: shoes and world foot point never translate. */
    public static DrawOptions body(GameTexture texture,int frame,int sourceSize,int scale,int footX,int footY,
            Mob mob,GameLight light,boolean idle,PlayerMob viewer){
        final int left=footX-32*scale,top=footY-52*scale;
        final float breath=idle?breath(VisualClock.seconds(),mob.getUniqueID()):0;
        final var mask=mob.getSwimMaskShaderOptions(mob.inLiquidFloat(mob.getX(),mob.getY()));
        final float alpha=mob instanceof PlayerMob player?player.getInvincibilityFrameAlpha():1f;
        if(!idle&&!mob.isAttacking&&texture.getWidth()==sourceSize&&Math.abs(mob.dx)+Math.abs(mob.dy)>2){
            float stride=(float)Math.sin(mob.getDistanceRan()*.23)*1.2f;
            var torso=texture.initDraw().section(0,sourceSize,0,44).size(sourceSize*scale,44*scale)
                .light(light).alpha(alpha).addMaskShader(mask).pos(left,top);
            var leftLeg=texture.initDraw().section(0,32,44,sourceSize).size(32*scale,(sourceSize-44)*scale)
                .light(light).alpha(alpha).addMaskShader(mask).addPositionMod(p->{p.addY3(stride);p.addY4(stride);}).pos(left,top+44*scale);
            var rightLeg=texture.initDraw().section(32,sourceSize,44,sourceSize).size((sourceSize-32)*scale,(sourceSize-44)*scale)
                .light(light).alpha(alpha).addMaskShader(mask).addPositionMod(p->{p.addY3(-stride);p.addY4(-stride);}).pos(left+32*scale,top+44*scale);
            return ()->{torso.draw();leftLeg.draw();rightLeg.draw();};
        }
        if(idle){
            var upper=texture.initDraw().section(frame*sourceSize,(frame+1)*sourceSize,0,44)
                .size(sourceSize*scale,44*scale).startGlowOptions(mob.getLevel(),mob.getID()).light(light).applyEnemyTracker(mob,viewer)
                .alpha(alpha).addMaskShader(mask).addPositionMod(p->{p.addY1(breath);p.addY2(breath);}).pos(left,top);
            var lower=texture.initDraw().section(frame*sourceSize,(frame+1)*sourceSize,44,sourceSize)
                .size(sourceSize*scale,(sourceSize-44)*scale).startGlowOptions(mob.getLevel(),mob.getID()).light(light).applyEnemyTracker(mob,viewer)
                .alpha(alpha).addMaskShader(mask).pos(left,top+44*scale);
            return ()->{upper.draw();lower.draw();};
        }
        return texture.initDraw().sprite(frame,0,sourceSize).size(sourceSize*scale,sourceSize*scale)
            .startGlowOptions(mob.getLevel(),mob.getID()).light(light).applyEnemyTracker(mob,viewer).alpha(alpha).addMaskShader(mask).pos(left,top);
    }
    private static final Map<String,LongAdder> weaponBindings=new java.util.concurrent.ConcurrentHashMap<>();
    private static final LongAdder unsupportedWeapons=new LongAdder();

    /** Native items retain their texture, authored grip, angle, charge/stretch,
     * colour and extra sprite layers. Only their old body-sized arm orbit is
     * replaced by the hand in the exact body frame being drawn.
     */
    public static DrawOptions weapon(ItemAttackerMob mob,InventoryItem item,int direction,float progress,
            int footX,int footY,GameLight light,PlayerMob perspective,String actor,int bodyFrame,int scale){
        if(item==null||mob.attackDir==null||!mob.isAttacking)return null;
        HumanAttackDrawOptions nativeDraw=item.item.getAttackDrawOptions(item,mob.getLevel(),perspective,
            null,null,null,direction,mob.attackDir.x,mob.attackDir.y,progress,null);
        AttackHandSockets.Grip hand=AttackHandSockets.at(actor,direction,bodyFrame);
        int handX=hand.worldX(footX,scale),handY=hand.worldY(footY,scale);
        if(nativeDraw instanceof ItemAttackDrawOptions items){
            bindNativeGrip(items,direction);
            int[] offset=nativeRawPivotOffset(direction);
            DrawOptions draw=nativeDraw.light(mob.getLevel(),false,mob.getUniqueID(),light).pos(handX-offset[0],handY-offset[1]);
            String key=actor+":"+item.item.getStringID()+":"+Math.floorMod(direction,4);
            return ()->{draw.draw();weaponBindings.computeIfAbsent(key,k->new LongAdder()).increment();};
        }
        // A few late-game native weapons replace the whole draw interface with
        // bespoke no-op/beam wrappers. Preserve that behaviour rather than draw
        // another icon over it; counters make this explicit during native QA.
        unsupportedWeapons.increment();
        return nativeDraw.light(mob.getLevel(),false,mob.getUniqueID(),light).pos(footX-32,footY-57);
    }
    static void bindNativeGrip(ItemAttackDrawOptions items,int direction){
        // itemRawCoords rotates each layer about its own real itemRotatePoint.
        // Do not replace that point with the centre of an inventory icon.
        items.forEachItemSprite(ItemAttackDrawOptions.AttackItemSprite::itemRawCoords);
        items.setOffsets(0,0,0,0,0f,0,0,0,0,0);
        items.drawOffset(0,0);
    }
    /** These are the native renderer's four raw-coordinate pivot bases, read
     * from ItemAttackDrawOptions.pos, not an art-specific global correction.
     */
    private static final int[][] RAW_PIVOT_OFFSETS={{6,22},{-4,5},{-10,18},{4,5}};
    static int[] nativeRawPivotOffset(int direction){return RAW_PIVOT_OFFSETS[Math.floorMod(direction,4)];}
    /** Reuse the existing palm pixels above the handle. The source body stays
     * untouched; a single native weapon is drawn between body and this palm.
     */
    public static DrawOptions palm(GameTexture body,int bodyFrame,int sourceSize,int scale,int footX,int footY,
            ItemAttackerMob mob,GameLight light,String actor,int direction){
        if(!mob.isAttacking||direction==0)return null;
        AttackHandSockets.Grip hand=AttackHandSockets.at(actor,direction,bodyFrame);
        int radius=2,left=Math.max(0,hand.x()-radius),top=Math.max(0,hand.y()-radius);
        int right=Math.min(sourceSize,left+radius*2+1),bottom=Math.min(sourceSize,top+radius*2+1);
        float alpha=mob instanceof PlayerMob p?p.getInvincibilityFrameAlpha():1f;
        return body.initDraw().section(bodyFrame*sourceSize+left,bodyFrame*sourceSize+right,top,bottom)
            .size((right-left)*scale,(bottom-top)*scale).light(light).alpha(alpha)
            .addMaskShader(mob.getSwimMaskShaderOptions(mob.inLiquidFloat(mob.getX(),mob.getY())))
            .pos(footX+(left-32)*scale,footY+(top-52)*scale);
    }
    public static Map<String,Object> weaponCounters(){
        Map<String,Object> counts=new LinkedHashMap<>();weaponBindings.forEach((key,value)->counts.put(key,value.sum()));
        return Map.of("bindings",counts,"bespokeNativeWrappers",unsupportedWeapons.sum());
    }
    private CharacterAnimation(){}
}
