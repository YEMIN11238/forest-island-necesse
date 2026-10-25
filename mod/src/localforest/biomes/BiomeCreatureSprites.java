package localforest.biomes;

import java.awt.Color;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import localforest.visual.SceneLighting;
import localforest.visual.CharacterAnimation;
import localforest.visual.VisualClock;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

/** Direction rows are authored DOWN, RIGHT, UP, LEFT; engine directions are UP, RIGHT, DOWN, LEFT. */
public final class BiomeCreatureSprites {
    private static final Map<String,GameTexture> textures=new HashMap<>();
    private static final Map<String,GameTexture> idleTextures=new HashMap<>();
    private static final int[] ROW={2,1,0,3},WALK={1,2,3,2};
    private static final Map<String,EnumMap<CreatureAnimation.Action,GameTexture>> actions=new HashMap<>();
    private static final EnumMap<CreatureAnimation.Action,Long> actionDraws=new EnumMap<>(CreatureAnimation.Action.class);
    public static long missingActionFrames,deathAnimations;
    public static long draws;
    public static void initResources(){
        textures.clear();idleTextures.clear();actions.clear();actionDraws.clear();
        for(BiomeCreatures.Species species:BiomeCreatures.SPECIES){
            try(var in=ResourceEncoder.getResourceInputStream(species.texture()+".png")){
                var image=ImageIO.read(in);int height=species.animal()?256:128;
                if(image==null||image.getWidth()!=256||image.getHeight()!=height)throw new IllegalStateException("Expected 256x"+height);
                var texture=GameTexture.fromFile(species.texture());texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);textures.put(species.id(),texture);
            }catch(Exception ex){throw new IllegalStateException("Invalid biome creature art: "+species.texture(),ex);}
            if(!species.animal()){loadIdle(species);loadActions(species);}
        }
        System.out.println("MOSSLUME_BIOME_CREATURE_TEXTURES count="+textures.size()+" frame=64 baseline=32,52");
    }
    private static void loadIdle(BiomeCreatures.Species species){
        String path=species.texture()+"-idle";
        try(var input=ResourceEncoder.getResourceInputStream(path+".png")){
            var image=ImageIO.read(input);
            if(image==null||image.getWidth()!=256||image.getHeight()!=64)throw new IllegalStateException("Expected four 64px direction columns");
            for(int col=0;col<4;col++){
                boolean visible=false;
                for(int y=0;y<64&&!visible;y++)for(int x=col*64;x<(col+1)*64;x++)if((image.getRGB(x,y)>>>24)>=128){visible=true;break;}
                if(!visible)throw new IllegalStateException("Empty direction column "+col);
            }
            var texture=GameTexture.fromFile(path);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);idleTextures.put(species.id(),texture);
        }catch(FileNotFoundException missing){
            // Unupgraded species retain their original authored idle directions.
        }catch(Exception invalid){throw new IllegalStateException("Invalid directional creature idle: "+path,invalid);}
    }
    private static void loadActions(BiomeCreatures.Species species){
        var loaded=new EnumMap<CreatureAnimation.Action,GameTexture>(CreatureAnimation.Action.class);
        for(var action:CreatureAnimation.Action.values()){
            String path=species.texture()+"-"+action.suffix;
            try(var input=ResourceEncoder.getResourceInputStream(path+".png")){
                var image=ImageIO.read(input);
                if(image==null||image.getWidth()!=action.columns*64||image.getHeight()!=256)
                    throw new IllegalStateException("Expected "+(action.columns*64)+"x256, four authored direction rows");
                // Empty cells are almost always a cut/export error. Reject the whole action
                // rather than silently rendering invisible or neighbouring sprites.
                for(int row=0;row<4;row++)for(int col=0;col<action.columns;col++){
                    boolean visible=false;
                    for(int y=row*64;y<(row+1)*64&&!visible;y++)for(int x=col*64;x<(col+1)*64;x++)if((image.getRGB(x,y)>>>24)>=128){visible=true;break;}
                    if(!visible)throw new IllegalStateException("Empty action frame "+col+","+row);
                }
                var texture=GameTexture.fromFile(path);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);loaded.put(action,texture);
            }catch(FileNotFoundException missing){
                // An older content pack may lack action sheets. Its real-direction idle
                // stays visible; no side pose is substituted for another view.
            }catch(Exception invalid){throw new IllegalStateException("Invalid directional creature action: "+path,invalid);}
        }
        if(!loaded.isEmpty())actions.put(species.id(),loaded);
    }
    private static GameTexture actionTexture(BiomeCreatures.Species species,CreatureAnimation.Action action){
        var clips=actions.get(species.id());return clips==null?null:clips.get(action);
    }
    public static Map<String,Object> animationDiagnostics(){
        Map<String,Object> out=new LinkedHashMap<>();
        for(var entry:actions.entrySet())out.put(entry.getKey(),entry.getValue().keySet().toString());
        out.put("actionDraws",new EnumMap<>(actionDraws));out.put("deathAnimations",deathAnimations);out.put("missingActionFrames",missingActionFrames);
        out.put("matchingScaleIdleSpecies",idleTextures.size());
        return out;
    }
    public static void draw(Mob mob,BiomeCreatures.Species species,List<MobDrawable> list,OrderableDrawables tiles,Level level,int x,int y,GameCamera camera,PlayerMob viewer){
        if(!mob.isVisible())return;
        GameTexture texture=textures.get(species.id());if(texture==null)return;
        int direction=Math.floorMod(mob.getDir(),4),col,row;boolean mirror=false;
        CreatureAnimation.Action action=null;
        boolean attacking=mob instanceof BiomeMonster monster&&monster.isAttacking;
        long hurtElapsed=mob.getTimeSinceLastHit();
        boolean hurt=mob.hitTime>0&&hurtElapsed>=0&&hurtElapsed<CreatureAnimation.HURT_MILLIS;
        if(species.animal()){
            row=ROW[direction];
            boolean moving=Math.abs(mob.dx)+Math.abs(mob.dy)>2;
            col=species.winged()?Math.floorMod((int)(mob.getTime()/120),4):moving?WALK[Math.floorMod((int)(mob.getDistanceRan()/9),4)]:0;
        }else{
            col=ROW[direction];row=0;
            // A directional resting pose exported at the same scale as combat prevents a
            // body-size jump. It is still a standing pose, not an authored walk cycle.
            texture=idleTextures.getOrDefault(species.id(),texture);
            action=hurt?CreatureAnimation.Action.HURT:attacking?CreatureAnimation.Action.ATTACK:null;
            if(action!=null){
                GameTexture authored=actionTexture(species,action);
                if(authored!=null){
                    texture=authored;row=CreatureAnimation.directionRow(direction);
                    col=action==CreatureAnimation.Action.HURT?CreatureAnimation.hurtFrame(hurtElapsed):CreatureAnimation.attackFrame(((BiomeMonster)mob).getAttackAnimProgress());
                }else{missingActionFrames++;action=null;}
            }
        }
        GameLight light=SceneLighting.light(level,x,y);
        int footX=camera.getDrawX(x),footY=camera.getDrawY(y);
        int drawX=footX-32,drawY=footY-52-(species.winged()?6:0);
        boolean moving=Math.abs(mob.dx)+Math.abs(mob.dy)>2;
        final GameTexture sprite=texture;
        final CreatureAnimation.Action drawnAction=action;
        final DrawOptions body;
        if(action!=null||(!species.animal()&&(attacking||hurt))){
            // Authored combat poses retain their complete silhouette and common foot point.
            body=sprite.initDraw().sprite(col,row,64).startGlowOptions(level,mob.getID()).light(light).applyEnemyTracker(mob,viewer).pos(drawX,drawY);
        }else{
            float breathe=moving?(float)Math.sin(mob.getDistanceRan()*.23)*.6f:CharacterAnimation.breath(VisualClock.seconds(),mob.getUniqueID())*.55f;
            final var upper=sprite.initDraw().section(col*64,(col+1)*64,row*64,row*64+44)
                .startGlowOptions(level,mob.getID()).light(light).applyEnemyTracker(mob,viewer)
                .addPositionMod(p->{p.addY1(breathe);p.addY2(breathe);}).pos(drawX,drawY);
            final var lower=sprite.initDraw().section(col*64,(col+1)*64,row*64+44,(row+1)*64)
                .startGlowOptions(level,mob.getID()).light(light).applyEnemyTracker(mob,viewer).pos(drawX,drawY+44);
            body=()->{upper.draw();lower.draw();};
        }
        list.add(new MobDrawable(){@Override public void draw(TickManager tick){body.draw();draws++;if(drawnAction!=null)actionDraws.merge(drawnAction,1L,Long::sum);}});
        if(MobRegistry.Textures.small_shadow!=null){
            var shadow=MobRegistry.Textures.small_shadow;final DrawOptions shadowDraw=shadow.initDraw().light(light).pos(footX-shadow.getWidth()/2,footY-shadow.getHeight()/2);
            tiles.add(tick->shadowDraw.draw());
        }
    }
    public static void death(Mob mob,BiomeCreatures.Species species){
        if(!mob.isClient())return;
        GameTexture death=actionTexture(species,CreatureAnimation.Action.DEATH);
        if(death!=null){
            mob.getLevel().entityManager.addParticle(new DeathAnimation(mob,death),Particle.GType.CRITICAL);deathAnimations++;
        }
        Color color=switch(species.biome()){case "snowfield"->new Color(157,185,190);case "desert"->new Color(174,139,82);case "volcano"->new Color(112,89,82);case "ruins"->new Color(129,132,112);case "mushroom"->new Color(135,114,145);default->new Color(104,137,82);};
        for(int i=0;i<6;i++)mob.getLevel().entityManager.addParticle(mob.x,mob.y-8,Particle.GType.IMPORTANT_COSMETIC).movesConstant((i%3-1)*14,(i/3*2-1)*12).color(color);
    }
    /** Native removal still occurs immediately; this cosmetic has no health, collision or loot. */
    private static final class DeathAnimation extends Particle {
        private final GameTexture texture;private final int row;
        DeathAnimation(Mob mob,GameTexture texture){
            super(mob.getLevel(),mob.x,mob.y,CreatureAnimation.DEATH_MILLIS);this.texture=texture;row=CreatureAnimation.directionRow(mob.getDir());
        }
        @Override public void addDrawables(List<LevelSortedDrawable> list,OrderableDrawables tiles,OrderableDrawables top,OrderableDrawables overlay,
            Level level,TickManager tick,GameCamera camera,PlayerMob viewer){
            if(removed())return;
            long age=getLifeCycleTime();
            final DrawOptions body=texture.initDraw().sprite(CreatureAnimation.deathFrame(age),row,64)
                .light(SceneLighting.light(level,getX(),getY())).alpha(CreatureAnimation.deathAlpha(age)).pos(camera.getDrawX(x)-32,camera.getDrawY(y)-52);
            list.add(new EntityDrawable(this){@Override public void draw(TickManager tick){body.draw();actionDraws.merge(CreatureAnimation.Action.DEATH,1L,Long::sum);}});
        }
    }
    private BiomeCreatureSprites(){}
}
