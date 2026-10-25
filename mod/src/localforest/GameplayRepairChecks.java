package localforest;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import localforest.biomes.BiomeMonster;
import localforest.companions.NativeInteractionRepairCheck;
import necesse.engine.GlobalData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.level.maps.Level;

/** Disposable-profile fixtures; advances on real server ticks without blocking that thread. */
public final class GameplayRepairChecks {
    private static GameplayRepairChecks active;
    private final Map<String,Object> evidence=new LinkedHashMap<>();
    private final NativeInteractionRepairCheck conversation=new NativeInteractionRepairCheck();
    private BiomeMonster attacker;
    private Mob target;
    private Level level;
    private long start;
    private int stage,initialHealth;
    private float damageProgress=-1;

    public static Map<String,Object> run(ServerClient client){
        require(Boolean.getBoolean("mosslume.refactorProbe.gameplay")&&Path.of(GlobalData.appDataPath()).getFileName().toString().startsWith("Mosslume-QA-"),"Disposable gameplay profile required");
        if(active==null)active=new GameplayRepairChecks();
        return active.tick(client);
    }
    private Map<String,Object> tick(ServerClient client){
        long now=System.nanoTime();if(start==0)start=now;
        require(now-start<40_000_000_000L,"Gameplay repair fixture timed out at stage "+stage
                + "; combat="+evidence+"; conversation="+conversation.evidence());
        if(stage==0){spawn(client,false);require(attacker.beginAttack(target),"Melee windup rejected");
            require(target.getHealth()==initialHealth,"Damage happened at windup start");stage=1;return null;}
        if(stage==1){
            if(target.getHealth()<initialHealth&&damageProgress<0)damageProgress=attacker.attackProgress();
            if(attacker.attackProgress()<.43f)require(target.getHealth()==initialHealth,"Melee hit during windup");
            if(attacker.isAttacking||attacker.attackPhase()!=localforest.biomes.AttackCycle.Phase.IDLE)return null;
            require(attacker.ownMeleeHits==1&&target.getHealth()<initialHealth,"Melee did not hit exactly once");
            require(damageProgress>=.43f,"Damage preceded active phase");
            evidence.put("melee",Map.of("windupNoDamage",true,"hits",attacker.ownMeleeHits,"firstObservedDamageProgress",damageProgress));
            cleanup();spawn(client,true);require(attacker.beginAttack(target),"Ranged windup rejected");stage=2;return null;
        }
        if(stage==2){
            if(attacker.attackProgress()<.43f)require(attacker.ownShots==0,"Projectile launched during windup");
            if(attacker.isAttacking||attacker.attackPhase()!=localforest.biomes.AttackCycle.Phase.IDLE)return null;
            require(attacker.ownShots==1,"Ranged attack did not launch exactly once");
            evidence.put("ranged",Map.of("windupNoProjectile",true,"shots",attacker.ownShots));
            cleanup();spawn(client,false);require(attacker.beginAttack(target),"Cancellation windup rejected");
            target.remove();stage=3;return null;
        }
        if(stage==3){
            if(attacker.attacksCancelled==0)return null;
            require(!attacker.isAttacking&&attacker.ownMeleeHits==0,"Removed target left a live attack");
            evidence.put("removedTarget",Map.of("cancelled",attacker.attacksCancelled,"hits",attacker.ownMeleeHits,"animationStopped",true));
            cleanup();spawn(client,false);require(attacker.beginAttack(target),"Dodge windup rejected");
            target.setPos(attacker.x+128,attacker.y,true);stage=4;return null;
        }
        if(stage==4){
            if(attacker.isAttacking||attacker.attackPhase()!=localforest.biomes.AttackCycle.Phase.IDLE)return null;
            require(attacker.ownMeleeHits==0&&target.getHealth()==initialHealth,"Melee hit a target that left its range during windup");
            evidence.put("dodge",Map.of("hits",0,"targetHealthUnchanged",true));cleanup();stage=5;
        }
        if(stage==5){if(!conversation.tick(client,now))return null;
            require(Boolean.TRUE.equals(conversation.evidence().get("passed")),"Conversation/population check failed: "+conversation.evidence());
            evidence.put("conversationPopulation",conversation.evidence());stage=6;
            evidence.put("passed",true);evidence.put("scope","Real server ticks, native hit and projectile creation, and server conversation lease. No paid model or physical keyboard/mouse input.");
        }
        return evidence;
    }
    private void spawn(ServerClient client,boolean ranged){
        level=client.getLevel();int tx=client.playerMob.getTileX(),ty=client.playerMob.getTileY();
        int px=tx+5,py=ty+7;boolean found=false;
        for(int y=ty+5;y<ty+15&&!found;y++)for(int x=tx+4;x<tx+15;x++){
            if(level.regionManager.isTileLoaded(x,y)&&!level.isLiquidTile(x,y)&&!level.isSolidTile(x,y)&&!level.isSolidTile(x+1,y)){px=x;py=y;found=true;break;}
        }
        require(found,"No loaded ground for native combat fixture");
        attacker=(BiomeMonster)MobRegistry.getMob(ranged?"mlwetlandpoisoninsect":"mlwetlandmudcreature",level);
        target=MobRegistry.getMob("cow",level);
        level.entityManager.addMob(attacker,px*32+6,py*32+16);
        level.entityManager.addMob(target,px*32+26,py*32+16);
        // Native Mob.serverTick always ticks a valid AI; an empty tree freezes only
        // these disposable fixtures without invalidating that lifecycle.
        attacker.ai=new BehaviourTreeAI<>(attacker,new SelectorAINode<>());
        target.ai=new BehaviourTreeAI<>(target,new SelectorAINode<>());
        attacker.canDespawn=false;target.canDespawn=false;
        // Fixture-only: villagers must not interrupt a synthetic target. Damage/weapon
        // permissions and the production monster state machine remain unchanged.
        attacker.isHostile=false;
        target.setMaxHealth(200);target.setHealthHidden(200);initialHealth=target.getHealth();
    }
    private void cleanup(){
        if(attacker!=null)attacker.remove();if(target!=null)target.remove();attacker=null;target=null;
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    private GameplayRepairChecks(){}
}
