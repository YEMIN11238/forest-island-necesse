package localforest.bosses;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.BossMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

/** Server-authoritative, saveable native bosses with visible windup and locked aim. */
public class MosslumeBossMob extends BossMob {
    public final MosslumeBosses.Definition definition;
    public static long encountersStarted,attacksStarted,areaHits,projectilesCreated,phaseTransitions;
    private final BossAttackTimeline timeline=new BossAttackTimeline();
    private final Set<Integer> dashHits=new HashSet<>();
    private int attackType=-1,attackOrdinal;
    private float aimX,aimY,attackX,attackY,homeX=Float.NaN,homeY=Float.NaN;
    private long nextAttackAt;
    private boolean phaseTwo;
    private boolean clientImpactSpawned;
    private boolean dashStarted;
    private long lastAttackClock,attackMaximumClockGap;
    private final Map<String,Object> lastImpact=new LinkedHashMap<>();
    public long ownAttacks,ownAreaHits,ownProjectiles,ownTransitions;
    private final AttackSender attackAbility=registerAbility(new AttackSender());
    private final class AttackSender extends MobAbility {
        void send(Packet packet){runAndSendAbility(packet);}
        @Override public void executePacket(PacketReader r){
            int type=r.getNextInt(),windup=r.getNextInt(),active=r.getNextInt(),recovery=r.getNextInt();
            attackType=type;aimX=r.getNextFloat();aimY=r.getNextFloat();attackX=r.getNextFloat();attackY=r.getNextFloat();
            phaseTwo=r.getNextBoolean();setFacingDir(aimX-attackX,aimY-attackY);timeline.begin(getTime(),windup,active,recovery);dashHits.clear();clientImpactSpawned=false;dashStarted=false;lastAttackClock=getTime();attackMaximumClockGap=0;lastImpact.clear();
        }
    }
    protected MosslumeBossMob(MosslumeBosses.Definition definition){
        super(definition.health());this.definition=definition;
        shouldSave=true;canDespawn=false;setSpeed(definition.speed());setArmor(definition.armor());setFriction(3);setKnockbackModifier(0);
        collision=new Rectangle(-24,-16,48,32);hitBox=new Rectangle(-44,-113,88,126);selectBox=new Rectangle(-64,-142,128,160);
    }
    public boolean isStone(){return definition==MosslumeBosses.STONE;}
    public boolean secondPhase(){return phaseTwo;}
    public int activeAttackType(){return attackType;}
    public BossAttackTimeline.Phase attackPhase(){return timeline.phase(getTime());}
    public float lockedAimX(){return aimX;}public float lockedAimY(){return aimY;}
    public float attackOriginX(){return attackX;}public float attackOriginY(){return attackY;}
    public float telegraphProgress(){return timeline.telegraphProgress(getTime());}
    public SourceActionClip activeSourceClip(){return attackType>=0?BossSourceClips.spec(definition.art(),attackType):null;}
    public SourceActionClip.Pick sourceFrame(){SourceActionClip clip=activeSourceClip();return clip==null?new SourceActionClip.Pick(false,0):clip.pick(timeline.elapsed(getTime()),timeline.windup(),timeline.active());}
    public int spriteColumn(){
        if(timeline.running())return timeline.spriteColumn(getTime());
        if(hitTime>0&&getTimeSinceLastHit()>=0&&getTimeSinceLastHit()<160)return 6;
        return Math.abs(dx)+Math.abs(dy)>3?1+Math.floorMod((int)(getDistanceRan()/14),2):0;
    }
    @Override public void init(){
        super.init();if(Float.isNaN(homeX)){homeX=x;homeY=y;}nextAttackAt=getTime()+1400;
        ai=new BehaviourTreeAI<>(this,new BossBrain());
    }
    /** Read by native QA, but also the same method called by the ordinary behaviour tree. */
    public boolean beginAttack(int type,Mob target){
        if(!isServer()||removed()||hasDied()||timeline.running()||target==null||target.removed()||target.hasDied()||!isSamePlace(target)||!target.canBeTargeted(this,getPvPOwner()))return false;
        if(type<0||type>1)return false;
        int windup=isStone()?(type==0?750:900):(type==0?650:850);
        if(phaseTwo)windup=Math.max(350,(int)(windup*.78f));
        SourceActionClip source=BossSourceClips.spec(definition.art(),type);
        windup=Math.max(windup,source.minimumWindupMillis());
        int active=Math.max(!isStone()&&type==1?420:120,source.minimumActiveMillis()),recovery=Math.max(isStone()?600:500,source.minimumRecoveryMillis());
        Packet content=new Packet();PacketWriter w=new PacketWriter(content);
        w.putNextInt(type);w.putNextInt(windup);w.putNextInt(active);w.putNextInt(recovery);
        w.putNextFloat(target.x);w.putNextFloat(target.y);w.putNextFloat(x);w.putNextFloat(y);w.putNextBoolean(phaseTwo);
        // This protected native helper is exposed by a narrowly scoped ability subclass.
        attackAbility.send(content);ownAttacks++;attacksStarted++;stopForAttack();return true;
    }
    private void stopForAttack(){if(ai!=null)ai.blackboard.mover.stopMoving(this);else stopMoving();}
    @Override public void serverTick(){
        super.serverTick();BossNearbyBuff.applyAround(this);
        if(!phaseTwo&&getHealth()*2<=getMaxHealth()){phaseTwo=true;ownTransitions++;phaseTransitions++;}
        setSpeed(definition.speed()*(phaseTwo?1.18f:1));
        if(!timeline.running())return;
        attackMaximumClockGap=Math.max(attackMaximumClockGap,getTime()-lastAttackClock);lastAttackClock=getTime();
        if(!isStone()&&attackType==1&&!dashStarted&&timeline.phase(getTime())==BossAttackTimeline.Phase.STRIKE){
            dashStarted=true;setSpeed(phaseTwo?250:215);if(ai!=null)ai.blackboard.mover.directMoveTo(ai.tree,Math.round(dashEndX()),Math.round(dashEndY()));
        }
        if(timeline.impact(getTime(),activeSourceClip().damageDelayMillis())){
            lastImpact.put("resolved",true);lastImpact.put("elapsed",timeline.elapsed(getTime()));lastImpact.put("windupMillis",timeline.windup());lastImpact.put("activeMillis",timeline.active());lastImpact.put("sourceDamageDelay",activeSourceClip().damageDelayMillis());lastImpact.put("maximumWorldClockGapMillis",attackMaximumClockGap);
            if(isStone()&&attackType==0)areaAttack(phaseTwo?140:118,false);
            else if(isStone())fireFan(phaseTwo?7:5,22,phaseTwo?185:155,phaseTwo?14:12);
            else if(attackType==0)areaAttack(phaseTwo?142:126,true);
        }else if(lastImpact.isEmpty()&&timeline.elapsed(getTime())>=timeline.windup()+activeSourceClip().damageDelayMillis()){
            lastImpact.put("resolved",false);lastImpact.put("reason","Native world clock skipped the active damage window");lastImpact.put("elapsed",timeline.elapsed(getTime()));lastImpact.put("windupMillis",timeline.windup());lastImpact.put("activeMillis",timeline.active());lastImpact.put("sourceDamageDelay",activeSourceClip().damageDelayMillis());lastImpact.put("maximumWorldClockGapMillis",attackMaximumClockGap);
        }
        if(!isStone()&&attackType==1&&timeline.phase(getTime())==BossAttackTimeline.Phase.STRIKE){
            setSpeed(phaseTwo?250:215);
            if(timeline.elapsed(getTime())>=timeline.windup()+activeSourceClip().damageDelayMillis())areaAttack(54,true);
        }else stopForAttack();
        if(timeline.complete(getTime())){timeline.clear();attackType=-1;stopForAttack();nextAttackAt=getTime()+(phaseTwo?600:1000);}
    }
    private float dashEndX(){float d=(float)Math.hypot(aimX-attackX,aimY-attackY);return attackX+(aimX-attackX)/Math.max(1,d)*Math.min(240,d+48);}
    private float dashEndY(){float d=(float)Math.hypot(aimX-attackX,aimY-attackY);return attackY+(aimY-attackY)/Math.max(1,d)*Math.min(240,d+48);}
    public float dashTargetX(){return dashEndX();}public float dashTargetY(){return dashEndY();}
    private void areaAttack(float radius,boolean arc){
        List<Map<String,Object>> candidates=new ArrayList<>();lastImpact.put("areaRadius",radius);lastImpact.put("candidates",candidates);
        getLevel().entityManager.streamMobsAndPlayers(x,y,(int)Math.ceil(radius)).filter(p->p!=null&&!p.removed()&&!p.hasDied()).forEach(target->{
            Map<String,Object> candidate=new LinkedHashMap<>();candidate.put("id",target.getUniqueID());candidate.put("type",target.getStringID());candidate.put("position",List.of(target.x,target.y));candidate.put("distance",getDistance(target));candidate.put("healthBefore",target.getHealth());candidate.put("lastHitAge",target.getTimeSinceLastHit());if(candidates.size()<16)candidates.add(candidate);
            if(dashHits.contains(target.getUniqueID())){candidate.put("rejected","alreadyHitThisAttack");return;}
            if(getDistance(target)>radius){candidate.put("rejected","outsideRadius");return;}
            if(!target.canBeTargeted(this,getPvPOwner())){candidate.put("rejected","nativeTargetability");return;}
            if(!target.canBeHit(this)){candidate.put("rejected","nativeHitCooldown");return;}
            float vx=target.x-x,vy=target.y-y,ax=aimX-attackX,ay=aimY-attackY;
            if(arc&&(vx*ax+vy*ay)<Math.hypot(vx,vy)*Math.hypot(ax,ay)*Math.cos(Math.toRadians(64))){candidate.put("rejected","outsideLockedArc");return;}
            if(getLevel().collides(new Line2D.Float(x,y,target.x,target.y),getLevelCollisionFilter())){candidate.put("rejected","nativeLineOfSight");return;}
            target.isServerHit(new GameDamage(isStone()?phaseTwo?17:14:phaseTwo?16:13),vx,vy,90,this);
            candidate.put("hit",true);candidate.put("healthAfter",target.getHealth());
            dashHits.add(target.getUniqueID());ownAreaHits++;areaHits++;
        });
    }
    private void fireFan(int count,float spread,float speed,int damage){
        double base=Math.atan2(aimY-y,aimX-x);
        for(int i=0;i<count;i++){
            double angle=base+Math.toRadians((i-(count-1)/2f)*spread);
            BossProjectile projectile=new BossProjectile.StoneShard();
            projectile.configure(this,x,y,(float)(x+Math.cos(angle)*520),(float)(y+Math.sin(angle)*520),speed,520,damage);
            getLevel().entityManager.projectiles.add(projectile);ownProjectiles++;projectilesCreated++;
        }
    }
    @Override public void clientTick(){
        super.clientTick();BossNearbyBuff.applyAround(this);if(isClientPlayerNearby())EventStatusBarManager.registerMobHealthStatusBar(this);
        if(!phaseTwo&&getHealth()*2<=getMaxHealth())phaseTwo=true;
        if(timeline.running()&&!clientImpactSpawned&&timeline.elapsed(getTime())>=timeline.windup()+activeSourceClip().damageDelayMillis()){
            clientImpactSpawned=true;
            if(isStone()&&attackType==0&&timeline.elapsed(getTime())<timeline.windup()+500)BossSprites.impact(this);
        }
        if(timeline.complete(getTime())){timeline.clear();attackType=-1;}
    }
    @Override public GameDamage getCollisionDamage(Mob target,boolean fromPacket,necesse.engine.network.server.ServerClient client){return null;}
    @Override public boolean canBePushed(Mob other){return false;}
    @Override public LootTable getLootTable(){return new LootTable(LootItem.between(definition.drop(),4,7),LootItem.between("coin",25,40));}
    @Override public void addSaveData(SaveData save){super.addSaveData(save);save.addFloat("mlBossHomeX",homeX);save.addFloat("mlBossHomeY",homeY);save.addInt("mlBossAttackOrdinal",attackOrdinal);save.addBoolean("mlBossPhaseTwo",phaseTwo);}
    @Override public void applyLoadData(LoadData save){super.applyLoadData(save);homeX=save.getFloat("mlBossHomeX",x);homeY=save.getFloat("mlBossHomeY",y);attackOrdinal=save.getInt("mlBossAttackOrdinal",0);phaseTwo=save.getBoolean("mlBossPhaseTwo",getHealth()*2<=getMaxHealth());timeline.clear();}
    @Override public void setupSpawnPacket(PacketWriter writer){super.setupSpawnPacket(writer);writer.putNextBoolean(phaseTwo);writer.putNextInt(attackType);writer.putNextFloat(aimX);writer.putNextFloat(aimY);writer.putNextFloat(attackX);writer.putNextFloat(attackY);writer.putNextInt(timeline.running()?(int)timeline.elapsed(getTime()):-1);writer.putNextInt(timeline.windup());writer.putNextInt(timeline.active());writer.putNextInt(Math.max(100,timeline.total()-timeline.windup()-timeline.active()));}
    @Override public void applySpawnPacket(PacketReader reader){super.applySpawnPacket(reader);phaseTwo=reader.getNextBoolean();attackType=reader.getNextInt();aimX=reader.getNextFloat();aimY=reader.getNextFloat();attackX=reader.getNextFloat();attackY=reader.getNextFloat();int elapsed=reader.getNextInt(),windup=reader.getNextInt(),active=reader.getNextInt(),recovery=reader.getNextInt();if(elapsed>=0)timeline.begin(getTime()-elapsed,windup,active,recovery);else timeline.clear();}
    @Override public void spawnDeathParticles(float knockbackX,float knockbackY){BossSprites.death(this);}
    @Override public void addDrawables(List<MobDrawable> list,OrderableDrawables tiles,OrderableDrawables top,Level level,int x,int y,TickManager tick,GameCamera camera,PlayerMob viewer){super.addDrawables(list,tiles,top,level,x,y,tick,camera,viewer);BossSprites.draw(this,list,tiles,level,x,y,camera,viewer);}
    public Map<String,Object> combatState(){Map<String,Object> state=new LinkedHashMap<>();state.put("id",definition.id());state.put("health",getHealth());state.put("phaseTwo",phaseTwo);state.put("attackType",attackType);state.put("phase",attackPhase().name());state.put("ownAttacks",ownAttacks);state.put("ownHits",ownAreaHits);state.put("ownProjectiles",ownProjectiles);state.put("ownTransitions",ownTransitions);state.put("maximumAttackWorldClockGapMillis",attackMaximumClockGap);state.put("lastImpact",new LinkedHashMap<>(lastImpact));return state;}
    private static final class BossBrain extends AINode<MosslumeBossMob> {
        @Override protected void onRootSet(AINode<MosslumeBossMob> root,MosslumeBossMob mob,Blackboard<MosslumeBossMob> board){}
        @Override public void init(MosslumeBossMob mob,Blackboard<MosslumeBossMob> board){}
        @Override public AINodeResult tick(MosslumeBossMob mob,Blackboard<MosslumeBossMob> board){
            if(!mob.isServer())return AINodeResult.SUCCESS;
            if(mob.timeline.running())return AINodeResult.RUNNING;
            Mob target=GameUtils.streamServerClients(mob.getLevel()).map(c->c.playerMob).filter(p->p!=null&&!p.removed()&&!p.hasDied()&&p.canBeTargeted(mob,mob.getPvPOwner())&&mob.getDistance(p)<1100).min(Comparator.comparingDouble(mob::getDistance)).orElse(null);
            if(target==null||Math.hypot(mob.x-mob.homeX,mob.y-mob.homeY)>1100){
                if(Math.hypot(mob.x-mob.homeX,mob.y-mob.homeY)>36)board.mover.directMoveTo(this,Math.round(mob.homeX),Math.round(mob.homeY));else board.mover.stopMoving(mob);
                return AINodeResult.SUCCESS;
            }
            float distance=mob.getDistance(target);int type=mob.attackOrdinal%2;
            float attackRange=mob.isStone()?(type==0?112:460):(type==0?124:300);
            if(distance>attackRange){board.mover.setMobTarget(this,target,false);return AINodeResult.RUNNING;}
            board.mover.stopMoving(mob);mob.setFacingDir(target.x-mob.x,target.y-mob.y);
            if(mob.getTime()>=mob.nextAttackAt&&mob.beginAttack(type,target))mob.attackOrdinal++;
            return AINodeResult.SUCCESS;
        }
    }
}
