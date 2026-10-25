package localforest.bosses;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.Trail;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

/** Native hit detection and networking, rendered with the user's original blue talisman frames. */
public class BossProjectile extends Projectile {
    public static long mobHits,glyphDraws;
    protected BossProjectile(){}
    public void configure(Mob owner,float x,float y,float tx,float ty,float speed,int distance,int damage){setLevel(owner.getLevel());this.x=x;this.y=y;setTarget(tx,ty);this.speed=speed;setDistance(distance);setDamage(new GameDamage(damage));setOwner(owner);knockback=75;}
    @Override public void init(){super.init();setWidth(14);height=72;isSolid=true;canBreakObjects=false;piercing=0;}
    @Override public float getHeight(){return Math.max(24,height-(height-24)*Math.min(1,traveledDistance/160));}
    @Override public Color getParticleColor(){return new Color(120,208,235);}
    @Override public Trail getTrail(){return new Trail(this,getLevel(),getParticleColor(),7,160,72);}
    @Override public void doHitLogic(Mob mob,float x,float y){super.doHitLogic(mob,x,y);if(isServer()&&mob!=null)mobHits++;}
    @Override public void addDrawables(List<LevelSortedDrawable> list,OrderableDrawables tiles,OrderableDrawables top,OrderableDrawables overlay,Level level,TickManager tick,GameCamera camera,PlayerMob viewer){
        if(removed())return;int x=camera.getDrawX(this.x),y=camera.getDrawY(this.y-getHeight());
        BossSourceClips.Effect effect=BossSourceClips.effect("stone-glyph");int frame=Math.floorMod((int)(getTimeSinceSpawned()/effect.frameMillis()),effect.columns());
        DrawOptions glyph=effect.texture().initDraw().sprite(frame,0,effect.cell()).size(effect.cell()*2,effect.cell()*2).mirror(dx>0,false).pos(x-effect.footX()*2,y-effect.footY()*2);
        list.add(new EntityDrawable(this){@Override public void draw(TickManager manager){
            glyph.draw();glyphDraws++;
        }});
    }
    public static final class StoneShard extends BossProjectile {public StoneShard(){super();}}
}
