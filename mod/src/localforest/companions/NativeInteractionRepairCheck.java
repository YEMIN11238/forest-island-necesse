package localforest.companions;

import java.awt.Point;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import necesse.engine.GlobalData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.ai.behaviourTree.leaves.CommandMoveToAINode;
import necesse.level.maps.Level;

/** Asynchronous real-server fixture. No game startup, model calls or assertion of keyboard/UI coverage. */
public final class NativeInteractionRepairCheck {
    private final Map<String,Object> result=new LinkedHashMap<>();
    private CompanionWorldData data;
    private HumanShop actor;
    private ServerClient client;
    private String person="";
    private Point guard,oldGuard;
    private Mob oldFollow,oldAttack;
    private float originalPlayerX,originalPlayerY,walkX,walkY,heldX,heldY;
    private long started,phaseAt,heldAt;
    private int phase,heldSamples,facingSamples;
    private int approachAttempts,landingSamples;
    private long approachAt;
    private Point approachPoint;
    private boolean leaseObserved,landingConfirmed;
    private double walked,maxDrift,resumed;
    private boolean done,commandPreserved;
    public boolean tick(ServerClient next,long nowNanos){
        if(done)return true;
        if(!safeProfile()){result.put("passed",false);result.put("error","Explicit gameplay QA flag and disposable profile are required");done=true;return true;}
        if(started==0)started=nowNanos;
        result.put("phase",phase);result.put("elapsedSeconds",(nowNanos-started)/1e9);
        if(nowNanos-started>30_000_000_000L)return fail(String.valueOf(result.getOrDefault("waitingReason","Native interaction fixture timed out")));
        try{
            if(next==null||next.playerMob==null||next.playerMob.getLevel()==null)return timeout(nowNanos,"Player not loaded");
            client=next;Level level=client.playerMob.getLevel();
            var worldData=client.getServer().world.worldEntity.getWorldData(CompanionSystem.WORLD_KEY);
            if(!(worldData instanceof CompanionWorldData found))return timeout(nowNanos,"Companion WorldData not loaded");
            data=found;
            result.put("population",NpcPopulationService.populationSummary(level));result.put("worldData",data.diagnostics());
            result.put("playerTile",java.util.List.of(client.playerMob.getTileX(),client.playerMob.getTileY()));
            result.put("knownPermanentCharacters",data.permanentPopulationIdentities().keySet());
            if(actor!=null){
                result.put("actor",Map.of("template",NativeNpcIdentity.template(actor),"tile",java.util.List.of(actor.getTileX(),actor.getTileY()),
                    "removed",actor.removed(),"inRange",actor.inInteractRange(client.playerMob),"distance",actor.getDistance(client.playerMob),
                    "attacking",actor.isAttacking,"downed",actor.isDowned(),"samePlace",actor.isSamePlace(client.playerMob)));
                result.put("actorInteractionPosition",java.util.List.of(actor.getInteractPos().x,actor.getInteractPos().y));
                result.put("actorUsingObject",actor.objectUser!=null);
                result.put("fixtureGuardStillPresent",guard!=null&&guard.equals(actor.commandGuardPoint));
            }
            if(phase==0){
                Map<String,Object> counts=NpcPopulationService.populationSummary(level);
                if(((Number)counts.get("seededCore")).longValue()!=4||((Number)counts.get("seededResidents")).longValue()!=6)return timeout(nowNanos,"Starting cast has not finished native seeding");
                Map<String,String> identities=data.permanentPopulationIdentities();boolean valid=identities.size()==10&&new HashSet<>(identities.values()).size()==10
                    &&identities.values().stream().allMatch(id->!NativeNpcIdentity.validUuid(id).isEmpty());
                result.put("uniquePersistentIds",valid);
                if(!valid)return timeout(nowNanos,"Waiting for stable WorldData identities after region return");
                boolean payload=true;int payloadChecked=0;
                for(var mob:level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed()).map(m->(HumanShop)m).toList()){
                    var definition=NpcDefinition.find(NativeNpcIdentity.template(mob));if(definition==null||definition.tier()==NpcDefinition.Tier.VISITOR)continue;
                    SaveData save=new SaveData("NPC");mob.addSaveData(save);
                    payload&=NativeNpcIdentity.get(mob).equals(NativeNpcIdentity.readPayload(save.toLoadData()));
                    payloadChecked++;
                }
                result.put("nativeNpcSavePayloadUuidRoundTrip",payload);
                result.put("nativeNpcSavePayloadLoadedCount",payloadChecked);
                if(!payload)return fail("Native NPC identity payload failed");
                if(payloadChecked==0)return timeout(nowNanos,"Permanent cast exists but no native NPC body has reloaded near the player");
                HumanShop origin=level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed()).map(m->(HumanShop)m)
                    .filter(m->{var d=NpcDefinition.find(NativeNpcIdentity.template(m));return d!=null&&d.tier()==NpcDefinition.Tier.CORE;}).findFirst().orElse(null);
                if(origin==null)return timeout(nowNanos,"Core resident not discovered");
                NpcPopulationService.ensureInitialPopulation(level,NativeNpcIdentity.originX(origin),NativeNpcIdentity.originY(origin));
                var after=NpcPopulationService.populationSummary(level);
                boolean idempotent=identities.equals(data.permanentPopulationIdentities())&&counts.get("seededCore").equals(after.get("seededCore"))
                    &&counts.get("seededResidents").equals(after.get("seededResidents"));result.put("populationReseedKeepsIds",idempotent);
                if(!idempotent)return fail("Population initialization duplicated or changed identities");
                result.put("coreCandidateStates",data.view.companions().stream().filter(p->p.recruited())
                    .map(p->Map.of("role",p.role(),"loaded",p.loaded(),"downed",p.downed(),"threatened",p.threatened(),"level",p.level())).toList());
                var view=data.view.companions().stream().filter(p->p.loaded()&&p.recruited()&&!p.downed()&&!p.threatened())
                    .min(java.util.Comparator.comparingInt(p->p.role().contains("守卫")?0:1)).orElse(null);
                if(view==null)return timeout(nowNanos,"No healthy core resident for conversation");
                actor=(HumanShop)level.entityManager.mobs.get(view.mobId(),false);if(actor==null)return timeout(nowNanos,"Resident not active");
                person=view.id();oldGuard=actor.commandGuardPoint==null?null:new Point(actor.commandGuardPoint);oldFollow=actor.commandFollowMob;oldAttack=actor.commandAttackMob;
                originalPlayerX=client.playerMob.x;originalPlayerY=client.playerMob.y;
                Point target=null;
                for(int[] offset:new int[][]{{6,0},{0,6},{-6,0},{0,-6}}){
                    Point proposed=CommandMoveToAINode.findClosestMoveToTile(actor,(actor.getTileX()+offset[0])*32+16,(actor.getTileY()+offset[1])*32+16);
                    if(proposed!=null&&proposed.distance(actor.getTileX(),actor.getTileY())>=3&&actor.estimateCanMoveTo(proposed.x,proposed.y,false)){target=proposed;break;}
                }
                if(target==null)return timeout(nowNanos,"No native route for resumed-work fixture");
                guard=new Point(target.x*32+16,target.y*32+16);walkX=actor.x;walkY=actor.y;
                actor.commandGuard(null,guard.x,guard.y);phase=1;phaseAt=nowNanos;
            }else if(phase==1){
                walked=Math.max(walked,Math.hypot(actor.x-walkX,actor.y-walkY));
                if(nowNanos-phaseAt<1_000_000_000L)return false;
                if(walked<1&&nowNanos-phaseAt<3_000_000_000L)return false;
                result.put("nativeMovementBeforeChat",walked);
                if(approachAttempts>=3)return fail("No safe interaction-range player position after three checked approach attempts");
                if(!approach(nowNanos))return timeout(nowNanos,"No collision-free player approach inside the real interaction radius");
                heartbeat(nowNanos);phase=2;phaseAt=nowNanos;
            }else if(phase==2){
                heartbeat(nowNanos);
                boolean held=Boolean.TRUE.equals(data.diagnostics().get("conversationHeld"));
                if(held)leaseObserved=true;
                else if(leaseObserved)return fail("Native conversation lease dropped during the continuous hold check");
                boolean landed=landingMatches();landingSamples=landed?landingSamples+1:0;
                if(landingSamples>=2)landingConfirmed=true;
                result.put("nativeClientLandingMatched",landed);result.put("landingSamples",landingSamples);
                result.put("nativeClientLandingConfirmed",landingConfirmed);
                result.put("approachAttempts",approachAttempts);
                if(!held){
                    if(nowNanos-approachAt>=750_000_000L&&!actor.inInteractRange(client.playerMob)){
                        if(approachAttempts>=3)return fail("Moving resident left all three real player approach attempts before lease acquisition");
                        approach(nowNanos);
                    }
                    return timeout(nowNanos,"Waiting for native player landing/range and real server conversation lease");
                }
                if(!actor.inInteractRange(client.playerMob))return fail("Native held conversation is outside its unchanged interaction range");
                if(!guard.equals(actor.commandGuardPoint))return fail("Fixture native guard order changed before conversation hold");
                if(!landingConfirmed)return false;
                if(nowNanos-phaseAt<250_000_000L)return false;
                if(heldAt==0){heldAt=nowNanos;heldX=actor.x;heldY=actor.y;}
                heldSamples++;maxDrift=Math.max(maxDrift,Math.hypot(actor.x-heldX,actor.y-heldY));
                float dx=client.playerMob.x-actor.x,dy=client.playerMob.y-actor.y;
                int expected=Math.abs(dx)>Math.abs(dy)?dx>0?1:3:dy>0?2:0;if(actor.getDir()==expected)facingSamples++;
                if(nowNanos-heldAt<5_000_000_000L)return false;
                commandPreserved=guard.equals(actor.commandGuardPoint);
                result.put("heldSeconds",(nowNanos-heldAt)/1e9);result.put("heldSamples",heldSamples);result.put("maxDriftWorldPixels",maxDrift);
                result.put("facingSamples",facingSamples);result.put("nativeGuardPreserved",commandPreserved);
                data.nativeCheckTalkingTo="";data.nativeCheckPresenceUntil=0;
                data.talkingTo="";data.sceneInteractive=false;data.scenePresenceAt=0;phase=3;phaseAt=nowNanos;
            }else if(phase==3){
                // Stop heartbeats and allow the ordinary WorldData/native AI to release and resume.
                resumed=Math.max(resumed,Math.hypot(actor.x-heldX,actor.y-heldY));
                if(nowNanos-phaseAt<1_500_000_000L)return false;
                boolean released=!Boolean.TRUE.equals(data.diagnostics().get("conversationHeld"))&&!actor.isBeingInteractedWith();
                result.put("releasedAfterExit",released);result.put("nativeMovementAfterExit",resumed);
                result.put("scope","Actual server conversation-presence/lease/native-AI chain; collision-checked native player approach (at most three before acquisition), client teleport/position agreement, unchanged native interaction range, continuous 5-second hold, native movement and NPC save payload. No NPC freezing/position injection, physical keyboard/composer input, paid model, full save reload, or combat-interrupt claim.");
                boolean passed=walked>=1&&maxDrift<=1.5&&heldSamples>=20&&facingSamples==heldSamples&&commandPreserved&&released&&resumed>=1;
                result.put("passed",passed);if(!passed)result.put("error","Native movement/hold/facing/resume acceptance failed");cleanup();done=true;return true;
            }
            return false;
        }catch(RuntimeException error){return fail(error.getClass().getSimpleName()+": "+String.valueOf(error.getMessage()));}
    }
    private boolean approach(long now){
        if(approachAt!=0&&now-approachAt<750_000_000L)return false;
        approachAt=now;approachAttempts++;
        Level level=client.playerMob.getLevel();Point interaction=actor.getInteractPos();
        Point pixels=approachPosition(interaction,new Point(client.playerMob.getX(),client.playerMob.getY()),(x,y)->{
            int tx=Math.floorDiv(x,32),ty=Math.floorDiv(y,32);
            return level.isTileWithinBounds(tx,ty)&&level.regionManager.isTileLoaded(tx,ty)&&!level.isLiquidTile(tx,ty)
                &&!client.playerMob.collidesWith(level,x,y)&&!client.playerMob.collidesWithAnyMob(level,x,y);
        });
        if(pixels==null)return false;
        client.playerMob.dx=0;client.playerMob.dy=0;client.playerMob.moveX=0;client.playerMob.moveY=0;
        // Real same-level teleport increments the native movement buffer and informs the owner.
        client.changeLevel(level.getIdentifier(),ignored->pixels,true);
        approachPoint=pixels;landingSamples=0;landingConfirmed=false;
        result.put("approachPoint",java.util.List.of(pixels.x,pixels.y));
        result.put("approachDistanceToInteraction",pixels.distance(interaction));
        result.put("nativeRangeImmediatelyAfterApproach",actor.inInteractRange(client.playerMob));
        return true;
    }
    private boolean landingMatches(){
        if(approachPoint==null)return false;
        var local=client.getServer().getLocalClient();
        if(local==null||local.getPlayer()==null||local.getLevel()==null||!local.getLevel().getIdentifier().equals(client.getLevelIdentifier()))return false;
        var player=local.getPlayer();
        return player.getMovementTeleportBuffer()==client.playerMob.getMovementTeleportBuffer()
            &&player.getDistance(client.playerMob.x,client.playerMob.y)<=4;
    }
    /** Pixel candidates use the NPC's real interaction/appearance position, including a seated user.
     * A 50px outer radius leaves 25px of margin inside native Mob.inInteractRange's unchanged 75px. */
    static Point approachPosition(Point interaction,Point player,java.util.function.BiPredicate<Integer,Integer> safe){
        Point best=null;double bestScore=Double.POSITIVE_INFINITY;
        for(int dy=-48;dy<=48;dy+=8)for(int dx=-48;dx<=48;dx+=8){
            int distanceSquared=dx*dx+dy*dy;if(distanceSquared<24*24||distanceSquared>50*50)continue;
            int x=interaction.x+dx,y=interaction.y+dy;if(!safe.test(x,y))continue;
            double score=Math.abs(distanceSquared-32*32)*1_000_000d+player.distanceSq(x,y);
            if(score<bestScore){best=new Point(x,y);bestScore=score;}
        }return best;
    }
    private void heartbeat(long now){data.nativeCheckTalkingTo=person;data.nativeCheckPresenceUntil=now+500_000_000L;data.talkingTo=person;data.scenePresenceAt=now;data.sceneInteractive=true;}
    private Map<String,String> identities(Level level){
        Map<String,String> ids=new LinkedHashMap<>();
        for(var mob:level.entityManager.mobs.stream().filter(m->m instanceof HumanShop&&!m.removed()).map(m->(HumanShop)m).toList()){
            var d=NpcDefinition.find(NativeNpcIdentity.template(mob));if(d!=null&&d.tier()!=NpcDefinition.Tier.VISITOR)ids.put(d.id(),NativeNpcIdentity.get(mob));
        }return ids;
    }
    private boolean timeout(long now,String reason){result.put("waitingReason",reason);return now-started>30_000_000_000L?fail(reason):false;}
    private boolean fail(String reason){result.put("passed",false);result.put("error",reason);cleanup();done=true;return true;}
    private void cleanup(){
        if(data!=null){data.nativeCheckTalkingTo="";data.nativeCheckPresenceUntil=0;data.talkingTo="";data.sceneInteractive=false;data.scenePresenceAt=0;}
        if(actor!=null&&!actor.removed()&&guard!=null&&guard.equals(actor.commandGuardPoint)){
            actor.clearCommandsOrders(null);
            if(oldGuard!=null)actor.commandGuard(null,oldGuard.x,oldGuard.y);
            else if(oldFollow!=null&&!oldFollow.removed())actor.commandFollow(null,oldFollow);
            else if(oldAttack!=null&&!oldAttack.removed())actor.commandAttack(null,oldAttack);
        }
        if(client!=null&&client.playerMob!=null&&actor!=null){
            Point original=new Point(Math.round(originalPlayerX),Math.round(originalPlayerY));
            client.changeLevel(client.getLevelIdentifier(),ignored->original,true);
        }
    }
    private static boolean safeProfile(){
        if(!Boolean.getBoolean("mosslume.refactorProbe.gameplay"))return false;
        Path path=Path.of(GlobalData.appDataPath()).toAbsolutePath().normalize();
        return path.getFileName()!=null&&path.getFileName().toString().startsWith("Mosslume-QA-Refactor-");
    }
    public Map<String,Object> evidence(){return Map.copyOf(result);}
}
