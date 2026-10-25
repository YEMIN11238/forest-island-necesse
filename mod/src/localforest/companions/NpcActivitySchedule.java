package localforest.companions;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import necesse.entity.mobs.ai.behaviourTree.leaves.CommandMoveToAINode;
import localforest.tasks.NativeWorkAdapter;

/** Sparse authored daily visits; native jobs, meals, sleep, combat and player orders take priority. */
final class NpcActivitySchedule {
    enum Phase { MORNING, WORK, EVENING, REST }
    private record Visit(HumanShop mob,Point target,long until){}
    private record Ready(long window,long after){}
    private final Map<String,Long> lastWindow=new HashMap<>();
    private final Map<String,Ready> pending=new HashMap<>();
    private final Map<String,Visit> visits=new HashMap<>();
    static Phase phase(float hour){return hour>=21||hour<6?Phase.REST:hour<8?Phase.MORNING:hour<18?Phase.WORK:Phase.EVENING;}
    void tick(String id,HumanShop mob,boolean busy,boolean threatened,String talkingTo,long now){
        var def=NpcDefinition.find(NativeNpcIdentity.template(mob));if(def==null)return;
        Visit old=visits.get(id);
        boolean interrupt=busy||threatened||mob.isDowned()||id.equals(talkingTo)||mob.isBeingInteractedWith()
            ||mob.hasActiveJob()||mob.getCurrentMission()!=null||needsTakePriority(mob.getHungerLevel(),mob.getRecreationLevel(),mob.getWorldEntity().isNight());
        if(old!=null){
            if(old.mob()!=mob||interrupt||now>=old.until()||mob.getDistance(old.target().x,old.target().y)<40){release(id);}
            else if(!old.target().equals(mob.commandGuardPoint))visits.remove(id);
            return;
        }
        if(interrupt||mob.hasActiveJob()||mob.getCurrentMission()!=null||mob.commandFollowMob!=null||mob.commandAttackMob!=null||mob.commandGuardPoint!=null)return;
        float hour=mob.getWorldEntity().getDayTimeHourFloat();Phase phase=phase(hour);
        if(phase==Phase.REST)return;
        long window=window(mob.getWorldEntity().getDay(),hour);if(lastWindow.getOrDefault(id,Long.MIN_VALUE)==window)return;
        if(!mayAttempt(id,window,now,staggerMillis(def.id())))return;
        int cx=NativeNpcIdentity.originX(mob),cy=NativeNpcIdentity.originY(mob);
        var world=localforest.world.WorldDefinition.forLevel(mob.getLevel());boolean continuous=world!=null&&world.isContinuous();
        Point site=site(def,phase,continuous);
        int tx=cx+site.x,ty=cy+site.y;
        Point near=NpcPopulationService.walkableNear(mob.getLevel(),tx,ty,3);if(near==null)return;
        if(Math.hypot(mob.getTileX()-near.x,mob.getTileY()-near.y)>36)return;
        Point target=CommandMoveToAINode.findClosestMoveToTile(mob,near.x*32+16,near.y*32+16);
        if(target==null||target.distance(near)>3||!mob.estimateCanMoveTo(target.x,target.y,false))return;
        Point pixel=new Point(target.x*32+16,target.y*32+16);
        if(visits.values().stream().anyMatch(visit->visit.target().distance(pixel)<40))return;
        lastWindow.put(id,window);pending.remove(id);
        if(mob.getDistance(pixel.x,pixel.y)<40)return;
        // A brief native move, then native jobs/wandering resume. No permanent workplace guard.
        mob.commandGuard(null,pixel.x,pixel.y);visits.put(id,new Visit(mob,pixel,now+12_000));
    }
    /** Arm the retry before doing any native region/path lookup. Every failure path shares it. */
    boolean mayAttempt(String id,long window,long now,long stagger){
        Ready ready=pending.get(id);
        if(ready==null||ready.window()!=window){ready=new Ready(window,now+stagger);pending.put(id,ready);}
        if(now<ready.after())return false;
        pending.put(id,new Ready(window,now+5_000));return true;
    }
    static boolean needsTakePriority(float hunger,float recreation,boolean night){return hunger<=.25f||recreation<=.2f||night;}
    static long staggerMillis(String template){return Math.floorMod(template.hashCode(),12_000);}
    static long window(int day,float hour){Phase phase=phase(hour);return day*96L+phase.ordinal()*24L+(phase==Phase.WORK?(int)hour/2:0);}
    static Point site(NpcDefinition def,Phase phase,boolean continuous){
        if(phase!=Phase.EVENING)return new Point(def.workX(),def.workY());
        int slot=NpcDefinition.INITIAL.stream().filter(d->d.district()==def.district()).toList().indexOf(def);
        int spread=(Math.max(slot,0)-1)*2;
        if(!continuous)return new Point((def.district()==0?0:def.workX())+spread,def.district()==0?3:def.workY()+2);
        return switch(def.district()){
            case 1->new Point(-8+spread,-6);
            case 2->new Point(7+spread,-15);
            case 3->new Point(-14+spread,-19);
            default->new Point(spread,3);
        };
    }
    void interrupt(String id){release(id);}
    void clear(){for(String id:java.util.List.copyOf(visits.keySet()))release(id);lastWindow.clear();pending.clear();}
    private void release(String id){Visit visit=visits.remove(id);if(visit!=null&&!visit.mob().removed()&&visit.target().equals(visit.mob().commandGuardPoint))NativeWorkAdapter.clearCommands(visit.mob(),null);}
}
