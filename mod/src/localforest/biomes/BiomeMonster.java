package localforest.biomes;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.CollisionChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserWandererAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionShooterPlayerChaserWandererAI;
import necesse.entity.mobs.hostile.GiantCaveSpiderMob;
import necesse.entity.mobs.hostile.HostileMob;
import necesse.entity.projectile.CaveSpiderSpitProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

/** Native melee and spit-projectile behaviour trees, with generated character art. */
public class BiomeMonster extends HostileMob {
    public final BiomeCreatures.Species species;
    public static long meleeHits,shots;
    public long ownMeleeHits,ownShots,attacksStarted,attacksCancelled;
    private final AttackCycle cycle=new AttackCycle();
    private final EmptyMobAbility stopAttackAnimation=registerAbility(new EmptyMobAbility(){
        @Override protected void run(){isAttacking=false;cycle.cancel();attackTarget=null;}
    });
    private Mob attackTarget;
    private float aimX,aimY;
    protected BiomeMonster(String name){
        super(BiomeCreatures.species(name).health());species=BiomeCreatures.species(name);
        setSpeed(species.speed());setFriction(3);setArmor(species.armor());attackAnimTime=420;
        collision=new Rectangle(-12,-9,24,18);hitBox=new Rectangle(-20,-37,40,44);selectBox=new Rectangle(-25,-44,50,52);
    }
    @Override public void init(){
        super.init();
        GameDamage damage=new GameDamage(species.damage());
        if(species.ranged()){
            // The native shooter tree also supports implicit contact damage. Disable that
            // extra path: this creature's only damage comes from its visible timed spit.
            ai=new BehaviourTreeAI<>(this,new CollisionShooterPlayerChaserWandererAI<BiomeMonster>(null,256,null,70,CooldownAttackTargetAINode.CooldownTimer.HAS_TARGET,3000,240,14000){
                @Override public boolean shootAtTarget(BiomeMonster mob,Mob target){
                    return mob.beginAttack(target);
                }
            });
        }else{
            ai=new BehaviourTreeAI<>(this,new CollisionPlayerChaserWandererAI<BiomeMonster>(null,240,damage,80,14000){
                @Override public boolean attackTarget(BiomeMonster mob,Mob target){
                    return mob.beginAttack(target);
                }
            });
        }
    }
    public boolean beginAttack(Mob target){
        if(!isServer()||removed()||hasDied()||target==null||target.removed()||target.hasDied()||!isSamePlace(target)||!canAttack()
            ||!target.canBeTargeted(this,getPvPOwner())||!target.canBeHit(this))return false;
        if(hitTime>0&&getTimeSinceLastHit()<150)return false;
        if(!cycle.begin(getTime(),attackAnimTime))return false;
        attackTarget=target;aimX=target.x;aimY=target.y;
        attack(target.getX(),target.getY(),true);attacksStarted++;
        if(ai!=null)ai.blackboard.mover.stopMoving(this);
        return true;
    }
    @Override public boolean canAttack(){return (cycle==null||!cycle.running())&&super.canAttack();}
    public AttackCycle.Phase attackPhase(){return cycle.phase(getTime());}
    public float attackProgress(){return cycle.progress(getTime());}
    private void cancelAttack(){stopAttackAnimation.runAndSend();attacksCancelled++;}
    @Override public void serverTick(){
        super.serverTick();
        if(!cycle.running())return;
        Mob target=attackTarget;
        if(hasDied()||removed()||target==null||target.removed()||target.hasDied()||!isSamePlace(target)||!target.canBeTargeted(this,getPvPOwner())
            ||(hitTime>0&&getTimeSinceLastHit()<100&&cycle.phase(getTime())==AttackCycle.Phase.WINDUP)){
            cancelAttack();return;
        }
        if(ai!=null)ai.blackboard.mover.stopMoving(this);
        if(cycle.impact(getTime())){
            boolean clear=target.canBeHit(this)&&!getLevel().collides(new Line2D.Float(x,y,target.x,target.y),getLevelCollisionFilter());
            if(species.ranged()){
                if(clear&&getDistance(target)<360){
                    int distance=Math.max(1,(int)Math.ceil(Math.hypot(aimX-x,aimY-y)));
                    getLevel().entityManager.projectiles.add(new CaveSpiderSpitProjectile(GiantCaveSpiderMob.Variant.SWAMP,x,y,aimX,aimY,
                        new GameDamage(Math.max(4,species.damage()-2)),this,distance));
                    ownShots++;shots++;
                }
            }else if(clear&&getDistance(target)<=56&&((target.x-x)*(aimX-x)+(target.y-y)*(aimY-y)>=0)){
                if(CollisionChaserAINode.simpleAttack(this,target,new GameDamage(species.damage()),80)){ownMeleeHits++;meleeHits++;}
            }
        }
        if(cycle.complete(getTime())){cycle.cancel();attackTarget=null;}
    }
    @Override public LootTable getLootTable(){return new LootTable(LootItem.between(species.drop(),1,2));}
    @Override public void spawnDeathParticles(float x,float y){BiomeCreatureSprites.death(this,species);}
    @Override public void addDrawables(List<MobDrawable> list,OrderableDrawables tiles,OrderableDrawables top,Level level,int x,int y,TickManager tick,GameCamera camera,PlayerMob viewer){
        super.addDrawables(list,tiles,top,level,x,y,tick,camera,viewer);
        BiomeCreatureSprites.draw(this,species,list,tiles,level,x,y,camera,viewer);
    }
}
