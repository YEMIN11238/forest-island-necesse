package localforest.bosses;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import localforest.visual.SceneLighting;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.level.maps.Level;
import org.lwjgl.opengl.GL11;

/** Direct source side-view bodies, intact source attacks and source effects on native world actors. */
public final class BossSprites {
    private static final Map<String,GameTexture> textures=new LinkedHashMap<>();
    private static final Map<String,Long> draws=new LinkedHashMap<>();
    private static final Map<String,Long> sourceFrameDraws=new LinkedHashMap<>();
    private static long telegraphDraws,deathDraws,impactDraws;
    public static void initResources(){
        textures.clear();draws.clear();sourceFrameDraws.clear();
        for(var def:MosslumeBosses.ALL){
            String path="mosslume/bosses/"+def.art();
            try(var stream=ResourceEncoder.getResourceInputStream(path+".png")){
                var image=ImageIO.read(stream);
                if(image==null||image.getWidth()!=768||image.getHeight()!=384)throw new IllegalStateException("Expected 8x4 cells at 96px");
                for(int row=0;row<4;row++)for(int col=0;col<8;col++){
                    boolean visible=false;for(int y=row*96;y<(row+1)*96&&!visible;y++)for(int x=col*96;x<(col+1)*96;x++)if((image.getRGB(x,y)>>>24)>=128){visible=true;break;}
                    if(!visible)throw new IllegalStateException("Empty Boss pose "+col+","+row);
                }
                GameTexture texture=GameTexture.fromFile(path);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);textures.put(def.id(),texture);
            }catch(Exception error){throw new IllegalStateException("Missing or invalid original Boss art: "+path,error);}
        }
        BossSourceClips.initResources();
        System.out.println("MOSSLUME_BOSS_ART originals="+textures.keySet()+" directSourceClips=true sourceOrientation=side-view-left-right variableCell=true scale=2 death=idle-fade");
    }
    public static void draw(MosslumeBossMob mob,List<MobDrawable> list,OrderableDrawables tiles,Level level,int x,int y,GameCamera camera,PlayerMob viewer){
        GameTexture texture=textures.get(mob.definition.id());if(texture==null)throw new IllegalStateException("Boss resources not initialized");
        int px=camera.getDrawX(x),py=camera.getDrawY(y),col=mob.spriteColumn(),row=Math.floorMod(mob.getDir(),4);
        SourceActionClip clip=col!=6&&mob.attackPhase()!=BossAttackTimeline.Phase.READY?mob.activeSourceClip():null;
        SourceActionClip.Pick pick=clip==null?new SourceActionClip.Pick(false,0):mob.sourceFrame();
        boolean source=clip!=null&&pick.source(),invisible=source&&clip.emptyFrames().contains(pick.column());
        GameTexture selected=source?BossSourceClips.texture(clip):texture;
        int frame=source?pick.column():clip!=null?0:col,cell=source?clip.frameSize():96,footX=source?clip.footX():48,footY=source?clip.footY():80;
        DrawOptions body=selected.initDraw().sprite(frame,row,cell).size(cell*2,cell*2).startGlowOptions(level,mob.getID()).light(SceneLighting.light(level,x,y)).applyEnemyTracker(mob,viewer).pos(px-footX*2,py-footY*2);
        String sourceKey=source?clip.file()+":"+frame:"";
        list.add(new MobDrawable(){@Override public void draw(TickManager manager){body.draw();draws.merge(mob.definition.id()+":"+col,1L,Long::sum);if(!sourceKey.isBlank())sourceFrameDraws.merge(sourceKey,1L,Long::sum);}});
        if(!invisible&&MobRegistry.Textures.small_shadow!=null){DrawOptions shadow=MobRegistry.Textures.small_shadow.initDraw().size(90,30).light(SceneLighting.light(level,x,y)).pos(px-45,py-10);tiles.add(manager->shadow.draw());}
        if(mob.attackPhase()==BossAttackTimeline.Phase.WINDUP)tiles.add(manager->telegraph(mob,camera));
        else if(mob.attackPhase()==BossAttackTimeline.Phase.STRIKE&&mob.isStone()&&mob.activeAttackType()==0)tiles.add(manager->{
            withState(()->Renderer.drawCircle(px,py,mob.secondPhase()?140:118,48,.82f,.89f,.64f,.80f,false));
        });
    }
    private static void telegraph(MosslumeBossMob mob,GameCamera camera){
        int cx=camera.getDrawX(mob.attackOriginX()),cy=camera.getDrawY(mob.attackOriginY());
        float progress=mob.telegraphProgress(),alpha=.35f+.50f*progress;
        double base=Math.atan2(mob.lockedAimY()-mob.attackOriginY(),mob.lockedAimX()-mob.attackOriginX());
        withState(()->{
            if(mob.isStone()&&mob.activeAttackType()==0){
                int radius=mob.secondPhase()?140:118;
                Renderer.drawCircle(cx,cy,radius,48,.91f,.66f,.24f,.10f+progress*.10f,true);
                Renderer.drawCircle(cx,cy,radius,48,1f,.72f,.26f,alpha,false);
                Renderer.drawCircle(cx,cy,(int)(radius*progress),48,1f,.76f,.35f,.28f,false);
            }else if(!mob.isStone()&&mob.activeAttackType()==1){
                int tx=camera.getDrawX(mob.dashTargetX()),ty=camera.getDrawY(mob.dashTargetY());
                float distance=(float)Math.hypot(tx-cx,ty-cy),angle=(float)Math.toDegrees(base);
                Renderer.initQuadDraw((int)distance,44).color(.65f,.89f,.67f,.12f+progress*.12f).rotate(angle,0,22).pos(cx,cy-22).draw();
                Renderer.drawLineRGBA(cx,cy,tx,ty,.78f,.98f,.74f,alpha);Renderer.drawCircle(tx,ty,32,32,.78f,.98f,.74f,alpha,false);
            }else{
                float radius=mob.isStone()?440:(mob.secondPhase()?142:126),half=mob.isStone()?(mob.secondPhase()?66:44):64;
                for(int i=0;i<=12;i++){
                    double a=base+Math.toRadians(-half+2*half*i/12);int tx=cx+(int)(Math.cos(a)*radius),ty=cy+(int)(Math.sin(a)*radius);
                    if(i==0||i==12||mob.isStone()&&i%3==0)Renderer.drawLineRGBA(cx,cy,tx,ty,.83f,.94f,.62f,alpha*.7f);
                    if(i>0){double previous=base+Math.toRadians(-half+2*half*(i-1)/12);Renderer.drawLineRGBA(cx+(int)(Math.cos(previous)*radius),cy+(int)(Math.sin(previous)*radius),tx,ty,.83f,.94f,.62f,alpha);}
                }
            }
        });telegraphDraws++;
    }
    private static void withState(Runnable draw){GL11.glPushAttrib(GL11.GL_TEXTURE_BIT|GL11.GL_CURRENT_BIT);try{draw.run();}finally{GL11.glPopAttrib();}}
    public static void death(MosslumeBossMob mob){
        if(!mob.isClient())return;GameTexture texture=textures.get(mob.definition.id());if(texture==null)return;
        mob.getLevel().entityManager.addParticle(new IdleFade(mob,texture),Particle.GType.CRITICAL);
        Color color=mob.isStone()?new Color(154,170,140):new Color(177,203,155);
        for(int i=0;i<16;i++)mob.getLevel().entityManager.addParticle(mob.x+(i%4-1.5f)*12,mob.y-30,Particle.GType.IMPORTANT_COSMETIC).movesConstant((i%4-1.5f)*15,(i/4-1.5f)*15).color(color).lifeTime(700);
    }
    public static void impact(MosslumeBossMob mob){
        BossSourceClips.Effect effect=BossSourceClips.effect("stone-impact");int count=mob.secondPhase()?6:4;float radius=mob.secondPhase()?94:74;
        for(int i=0;i<count;i++){double angle=Math.PI*2*(i+.5)/count;mob.getLevel().entityManager.addParticle(new SourceImpact(mob,effect,(float)(mob.attackOriginX()+Math.cos(angle)*radius),(float)(mob.attackOriginY()+Math.sin(angle)*radius)),Particle.GType.CRITICAL);}
    }
    public static Map<String,Object> diagnostics(){Map<String,Object> out=new LinkedHashMap<>();out.put("loaded",textures.keySet().toString());out.put("draws",new LinkedHashMap<>(draws));out.put("directSourceFrameDraws",new LinkedHashMap<>(sourceFrameDraws));out.put("sourceAssets",BossSourceClips.diagnostics());out.put("telegraphDraws",telegraphDraws);out.put("deathDraws",deathDraws);out.put("stoneImpactDraws",impactDraws);out.put("sourceGlyphDraws",BossProjectile.glyphDraws);return out;}
    private static final class IdleFade extends Particle {
        private final GameTexture texture;private final int row;
        IdleFade(MosslumeBossMob mob,GameTexture texture){super(mob.getLevel(),mob.x,mob.y,1000);this.texture=texture;row=Math.floorMod(mob.getDir(),4);}
        @Override public void addDrawables(List<LevelSortedDrawable> list,OrderableDrawables tiles,OrderableDrawables top,OrderableDrawables overlay,Level level,TickManager tick,GameCamera camera,PlayerMob viewer){
            if(removed())return;float alpha=1-Math.max(0,getLifeCycleTime()-600)/400f;
            DrawOptions body=texture.initDraw().sprite(0,row,96).size(192,192).light(SceneLighting.light(level,x,y)).alpha(Math.max(0,alpha)).pos(camera.getDrawX(x)-96,camera.getDrawY(y)-160);
            list.add(new EntityDrawable(this){@Override public void draw(TickManager manager){body.draw();deathDraws++;}});
        }
    }
    private static final class SourceImpact extends Particle {
        private final BossSourceClips.Effect effect;
        SourceImpact(MosslumeBossMob owner,BossSourceClips.Effect effect,float x,float y){super(owner.getLevel(),x,y,effect.duration()+120);this.effect=effect;}
        @Override public void addDrawables(List<LevelSortedDrawable> list,OrderableDrawables tiles,OrderableDrawables top,OrderableDrawables overlay,Level level,TickManager tick,GameCamera camera,PlayerMob viewer){
            if(removed())return;long age=getLifeCycleTime();float alpha=Math.min(1,Math.max(0,(effect.duration()+120-age)/120f));
            DrawOptions body=effect.texture().initDraw().sprite(effect.frame(age),0,effect.cell()).size(effect.cell()*2,effect.cell()*2).light(SceneLighting.light(level,x,y)).alpha(alpha).pos(camera.getDrawX(x)-effect.footX()*2,camera.getDrawY(y)-effect.footY()*2);
            list.add(new EntityDrawable(this){@Override public void draw(TickManager manager){body.draw();impactDraws++;}});
        }
    }
    private BossSprites(){}
}
