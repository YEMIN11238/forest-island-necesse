package localforest.biomes;

import java.nio.file.Path;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import necesse.engine.GlobalData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.level.maps.Level;

/** Opt-in server actions plus observation of the ordinary client renderer. Never draws test sprites itself. */
public final class NativeCreatureAnimationCheck {
    private static final String[] SPECIES={"mlwetlandmudcreature","mlwetlandpoisoninsect"};
    private enum Stage { READY,ATTACK,HURT,DEATH }
    private record Counts(long attack,long hurt,long death,long deathAnimations,long missing,int idleSpecies) {
        long action(Stage stage){return switch(stage){case ATTACK->attack;case HURT->hurt;case DEATH->death;default->0;};}
    }
    private record Plan(int epoch,int mobId,String species,String levelId,int x,int y,Stage stage,Counts before) {}
    private record Observation(int epoch,boolean present,boolean onScreen,boolean attackSeen,boolean hurtSeen,
        int visibleFrames,Counts counts,Map<String,Object> resources) {}
    private record Viewport(String levelId,int left,int top,int right,int bottom) {
        boolean contains(int x,int y){return x>=left+40&&x<right-40&&y>=top+64&&y<bottom-24;}
    }
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final List<Map<String,Object>> cases=new ArrayList<>();
    private volatile Plan plan;
    private volatile Observation observed;
    private volatile Viewport viewport;
    private volatile String clientError;
    private int epoch,speciesIndex,clientEpoch=-1,visibleFrames;
    private boolean clientAttackSeen,clientHurtSeen,done;
    private long started,stageStarted;
    private Level level;
    private BiomeMonster actor;
    private Mob target;
    private int healthBeforeHurt,healthAfterHurt;
    private Map<String,Object> current;
    private final List<Map<String,Object>> attackSamples=new ArrayList<>();

    public static boolean enabled(){return Boolean.getBoolean("mosslume.refactorProbe.creatureAnimations");}
    private static boolean safeProfile(){
        Path name=Path.of(GlobalData.appDataPath()).getFileName();
        return enabled()&&name!=null&&name.toString().startsWith("Mosslume-QA-");
    }
    /** Call on the client/frame thread alongside the existing RefactorNativeChecks.frame hook. */
    public void frame(MainGame game){
        if(!safeProfile())return;
        try{
            var client=game.getClient();if(client==null||client.isDisconnected()||client.getPlayer()==null)return;
            Level visible=client.getPlayer().getLevel();
            if(visible==null)return;
            var camera=game.getCamera();
            viewport=new Viewport(visible.getIdentifier().toString(),camera.getX(),camera.getY(),camera.getX()+camera.getWidth(),camera.getY()+camera.getHeight());
            Plan expected=plan;if(expected==null||!visible.getIdentifier().toString().equals(expected.levelId()))return;
            var resources=BiomeCreatureSprites.animationDiagnostics();
            Counts counts=readCounts(resources);
            Mob mob=GameUtils.getLevelMob(expected.mobId(),visible,false);
            boolean present=mob instanceof BiomeMonster&&!mob.removed();
            int px=present?mob.getX():expected.x(),py=present?mob.getY():expected.y();
            int dx=camera.getDrawX(px),dy=camera.getDrawY(py);
            boolean onScreen=dx>=40&&dx<camera.getWidth()-40&&dy>=64&&dy<camera.getHeight()-24
                &&visible.regionManager.isTileLoaded(Math.floorDiv(px,32),Math.floorDiv(py,32));
            if(clientEpoch!=expected.epoch()){
                clientEpoch=expected.epoch();visibleFrames=0;clientAttackSeen=false;clientHurtSeen=false;
            }
            if(onScreen&&present){
                visibleFrames++;
                clientAttackSeen|=((BiomeMonster)mob).isAttacking;
                clientHurtSeen|=mob.hitTime>0&&mob.getTimeSinceLastHit()>=0&&mob.getTimeSinceLastHit()<CreatureAnimation.HURT_MILLIS;
            }
            observed=new Observation(expected.epoch(),present,onScreen,clientAttackSeen,clientHurtSeen,visibleFrames,counts,resources);
        }catch(Throwable error){clientError=error.toString();}
    }
    /** Call on real server ticks. Returns true only after both fixtures were rendered and cleaned up. */
    public boolean tick(ServerClient client,long now){
        require(safeProfile(),"Creature animation check requires the explicit flag and disposable Mosslume-QA profile");
        if(done)return true;
        try{
            if(started==0){if(viewport==null)return false;started=now;spawn(client,now);return false;}
            require(clientError==null,"Client animation observation failed: "+clientError);
            require(now-started<25_000_000_000L,"Creature animation check timed out: "+evidence());
            require(now-stageStarted<6_000_000_000L,"Creature animation stage timed out: "+evidence());
            Observation seen=observed;Plan expected=plan;
            if(seen==null||seen.epoch()!=expected.epoch())return false;
            if(expected.stage()!=Stage.READY)require(seen.counts().missing()==expected.before().missing(),"An action used missing-art fallback");
            if(expected.stage()==Stage.READY){
                if(!seen.present()||!seen.onScreen()||seen.visibleFrames()<3)return false;
                require(seen.counts().idleSpecies()==12,"Not all matching-scale creature idle resources loaded");
                Object loaded=seen.resources().get(expected.species());
                require(loaded!=null&&loaded.toString().contains("ATTACK")&&loaded.toString().contains("HURT")&&loaded.toString().contains("DEATH"),"Fixture action resources missing: "+loaded);
                current.put("clientLoadedAndVisibleBeforeAttack",true);
                current.put("beforeAttack",combatState());
                require(!actor.collidesWith(level)&&!target.collidesWith(level)&&rayClear()&&actor.getDistance(target)<=56,"Fixture moved into collision before attack: "+combatState());
                advance(Stage.ATTACK,seen.counts(),now);
                require(actor.beginAttack(target),"Native fixture attack rejected");return false;
            }
            long draws=seen.counts().action(expected.stage())-expected.before().action(expected.stage());
            if(expected.stage()==Stage.ATTACK){
                if(attackSamples.size()<30)attackSamples.add(combatState());
                if(!seen.attackSeen()||draws<1||actor.isAttacking||actor.attackPhase()!=AttackCycle.Phase.IDLE)return false;
                require(actor.species.ranged()?actor.ownShots==1:actor.ownMeleeHits==1,"Native combat effect missing: "+combatState());
                current.put("attack",Map.of("clientObservedAttackState",true,"nativeDrawDelta",draws,"meleeHits",actor.ownMeleeHits,"shots",actor.ownShots));
                current.put("attackServerSamples",new ArrayList<>(attackSamples));
                healthBeforeHurt=actor.getHealth();advance(Stage.HURT,seen.counts(),now);
                actor.isServerHit(new GameDamage(12),0,0,0,target);healthAfterHurt=actor.getHealth();
                require(healthAfterHurt>0&&healthAfterHurt<healthBeforeHurt,"Nonlethal native hit did not reduce fixture health");return false;
            }
            if(expected.stage()==Stage.HURT){
                if(!seen.hurtSeen()||draws<1||now-stageStarted<550_000_000L)return false;
                current.put("hurt",Map.of("clientObservedHitState",true,"nativeDrawDelta",draws,"healthBefore",healthBeforeHurt,"healthAfter",healthAfterHurt));
                advance(Stage.DEATH,seen.counts(),now);
                actor.isServerHit(new GameDamage(100000),0,0,0,target);
                require(actor.hasDied()||actor.removed(),"Lethal native hit did not remove fixture");return false;
            }
            if(expected.stage()==Stage.DEATH){
                long deaths=seen.counts().deathAnimations()-expected.before().deathAnimations();
                if(seen.present()||!seen.onScreen()||draws<1||deaths<1||now-stageStarted<1_100_000_000L)return false;
                current.put("death",Map.of("nativeMobRemoved",true,"clientMobRemoved",true,"nativeDrawDelta",draws,"deathAnimationDelta",deaths));
                cases.add(current);cleanup();speciesIndex++;
                if(speciesIndex<SPECIES.length){spawn(client,now);return false;}
                done=true;evidence.put("passed",true);evidence.put("fixtures",cases);
                evidence.put("elapsedSeconds",(now-started)/1e9);evidence.put("ownFixturesRemoved",true);
                evidence.put("scope","Two native monster fixtures: real server attack/nonlethal/lethal hits, client replica states and actual ATTACK/HURT/DEATH draw counter increases. Counters are renderer-wide, paired with each on-screen fixture stage. Not an all-species/four-direction visual review, framebuffer image assertion or physical player input test.");
                return true;
            }
            return false;
        }catch(RuntimeException error){
            evidence.put("failureState",evidence());cleanup();evidence.put("passed",false);evidence.put("error",error.toString());throw error;
        }
    }
    public Map<String,Object> evidence(){
        Map<String,Object> out=new LinkedHashMap<>(evidence);out.put("fixtures",new ArrayList<>(cases));
        Plan p=plan;Observation o=observed;
        if(p!=null){out.put("currentSpecies",p.species());out.put("stage",p.stage().toString());out.put("fixtureMobId",p.mobId());}
        if(current!=null)out.put("currentFixture",new LinkedHashMap<>(current));
        if(actor!=null&&target!=null){out.put("serverCombat",combatState());out.put("attackServerSamples",new ArrayList<>(attackSamples));}
        if(o!=null)out.put("clientObservation",Map.of("epoch",o.epoch(),"present",o.present(),"onScreen",o.onScreen(),"visibleFrames",o.visibleFrames(),"attackSeen",o.attackSeen(),"hurtSeen",o.hurtSeen(),"counts",o.counts().toString()));
        return out;
    }
    /** Also safe for parent failure cleanup; only this instance's disposable mobs are removed. */
    public void cleanup(){
        plan=null;if(actor!=null&&!actor.removed())actor.remove();if(target!=null&&!target.removed())target.remove();actor=null;target=null;
    }
    private void spawn(ServerClient client,long now){
        level=client.getLevel();require(level!=null&&level.isServer(),"Native server level required");
        Viewport view=viewport;require(view!=null&&view.levelId().equals(level.getIdentifier().toString()),"Client viewport and fixture level differ");
        actor=(BiomeMonster)MobRegistry.getMob(SPECIES[speciesIndex],level);target=MobRegistry.getMob("cow",level);
        int tx=client.playerMob.getTileX(),ty=client.playerMob.getTileY(),x=0,y=0;boolean found=false;
        for(int radius=2;radius<=6&&!found;radius++)for(int oy=-radius;oy<=radius&&!found;oy++)for(int ox=-radius;ox<=radius;ox++){
            if(Math.max(Math.abs(ox),Math.abs(oy))!=radius)continue;
            int candidateX=tx+ox,candidateY=ty+oy;
            if(!clear(candidateX,candidateY)||!clear(candidateX+1,candidateY)||!clear(candidateX,candidateY-1))continue;
            int px=candidateX*32+16,py=candidateY*32+16;
            if(!view.contains(px,py)||!view.contains(px+32,py))continue;
            // Tile flags alone do not cover overhanging object colliders. Test the
            // exact native bodies and strike segment before adding either fixture.
            if(actor.collidesWith(level,px,py)||target.collidesWith(level,px+32,py)
                ||level.collides(new Line2D.Float(px,py,px+32,py),actor.getLevelCollisionFilter()))continue;
            if(level.entityManager.streamMobsAndPlayers(px+16,py,112).anyMatch(m->!m.removed()&&(m.getDistance(px,py)<72||m.getDistance(px+32,py)<72)))continue;
            x=px;y=py;found=true;break;
        }
        require(found,"No loaded open ground near player for animation fixture");
        level.entityManager.addMob(actor,x,y);level.entityManager.addMob(target,x+32,y);
        actor.ai=new BehaviourTreeAI<>(actor,new SelectorAINode<>());target.ai=new BehaviourTreeAI<>(target,new SelectorAINode<>());
        actor.stopMoving();target.stopMoving();attackSamples.clear();
        actor.canDespawn=false;target.canDespawn=false;actor.isHostile=false;
        target.setMaxHealth(200);target.setHealthHidden(200);
        current=new LinkedHashMap<>();current.put("species",SPECIES[speciesIndex]);current.put("mobId",actor.getUniqueID());current.put("position",List.of(x,y));
        // READY snapshots establish the actual renderer baseline; the initial zero
        // values are ignored until the first loaded client observation arrives.
        Counts before=observed==null?new Counts(0,0,0,0,0,0):observed.counts();advance(Stage.READY,before,now);
    }
    private boolean clear(int x,int y){return level.regionManager.isTileLoaded(x,y)&&!level.isLiquidTile(x,y)&&!level.isSolidTile(x,y);}
    private boolean rayClear(){return !level.collides(new Line2D.Float(actor.x,actor.y,target.x,target.y),actor.getLevelCollisionFilter());}
    private Map<String,Object> combatState(){
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("species",actor.species.id());out.put("worldTime",actor.getTime());out.put("phase",actor.attackPhase().toString());out.put("progress",actor.attackProgress());
        out.put("actorPosition",List.of(actor.x,actor.y));out.put("targetPosition",List.of(target.x,target.y));out.put("distance",actor.getDistance(target));
        out.put("actorHealth",actor.getHealth());out.put("targetHealth",target.getHealth());out.put("actorRemoved",actor.removed());out.put("targetRemoved",target.removed());
        out.put("actorCollision",actor.collidesWith(level));out.put("targetCollision",target.collidesWith(level));out.put("rayClear",rayClear());out.put("targetCanBeHit",target.canBeHit(actor));
        out.put("actorHitAge",actor.getTimeSinceLastHit());out.put("targetHitAge",target.getTimeSinceLastHit());
        out.put("attacksStarted",actor.attacksStarted);out.put("cancelled",actor.attacksCancelled);out.put("hits",actor.ownMeleeHits);out.put("shots",actor.ownShots);return out;
    }
    private void advance(Stage stage,Counts before,long now){
        stageStarted=now;plan=new Plan(++epoch,actor.getUniqueID(),actor.species.id(),level.getIdentifier().toString(),actor.getX(),actor.getY(),stage,before);
    }
    private static Counts readCounts(Map<String,Object> values){
        Map<?,?> draws=values.get("actionDraws") instanceof Map<?,?> map?map:Map.of();
        return new Counts(number(draws.get(CreatureAnimation.Action.ATTACK)),number(draws.get(CreatureAnimation.Action.HURT)),number(draws.get(CreatureAnimation.Action.DEATH)),
            number(values.get("deathAnimations")),number(values.get("missingActionFrames")),(int)number(values.get("matchingScaleIdleSpecies")));
    }
    private static long number(Object value){return value instanceof Number n?n.longValue():0;}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
