package localforest.companions;

import java.awt.Point;
import java.util.HashSet;

public final class NpcActivityScheduleTest {
    private static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args){
        check(NpcActivitySchedule.needsTakePriority(.2f,.9f,false),"Hungry native eater can be given a schedule guard");
        check(NpcActivitySchedule.needsTakePriority(.9f,.1f,false)&&NpcActivitySchedule.needsTakePriority(.9f,.9f,true),"Recreation or sleep loses priority");
        check(!NpcActivitySchedule.needsTakePriority(.8f,.8f,false),"Healthy idle resident cannot visit work site");
        HashSet<Point> work=new HashSet<>(),evening=new HashSet<>();HashSet<Long> startTimes=new HashSet<>();
        for(var resident:NpcDefinition.INITIAL){
            work.add(NpcActivitySchedule.site(resident,NpcActivitySchedule.Phase.WORK,true));
            evening.add(NpcActivitySchedule.site(resident,NpcActivitySchedule.Phase.EVENING,true));
            startTimes.add(NpcActivitySchedule.staggerMillis(resident.id()));
            check(NpcActivitySchedule.staggerMillis(resident.id())>=0&&NpcActivitySchedule.staggerMillis(resident.id())<12_000,"Unbounded visit delay");
            check(NpcActivitySchedule.site(resident,NpcActivitySchedule.Phase.MORNING,true).equals(new Point(resident.workX(),resident.workY())),"Occupation work site changed unpredictably");
        }
        check(work.size()==10&&evening.size()==10&&startTimes.size()==10,"Residents converge on identical targets or synchronize visits");
        check(evening.stream().anyMatch(p->p.y<=-19)&&evening.stream().anyMatch(p->p.y>=3),"All districts share the same central evening gathering");
        check(NpcActivitySchedule.window(0,8)==NpcActivitySchedule.window(0,9.9f)
            &&NpcActivitySchedule.window(0,8)!=NpcActivitySchedule.window(0,10)
            &&NpcActivitySchedule.window(1,8)!=NpcActivitySchedule.window(0,8),"Work visits repeat every tick or never refresh next period/day");
        check(NpcActivitySchedule.phase(22)==NpcActivitySchedule.Phase.REST&&NpcActivitySchedule.phase(5)==NpcActivitySchedule.Phase.REST,"Night does not release authored activity");
        var schedule=new NpcActivitySchedule();
        check(!schedule.mayAttempt("blocked-npc",1,1000,2000)&&!schedule.mayAttempt("blocked-npc",1,2999,2000),"Initial authored stagger was lost");
        check(schedule.mayAttempt("blocked-npc",1,3000,2000),"First due attempt rejected");
        for(long now=3250;now<8000;now+=250)check(!schedule.mayAttempt("blocked-npc",1,now,2000),"Blocked route repeats native path search at observation frequency");
        check(schedule.mayAttempt("blocked-npc",1,8000,2000),"Five-second retry never unlocked");
        check(schedule.mayAttempt("other-npc",1,3250,0),"One blocked NPC throttles another character");
        check(!schedule.mayAttempt("blocked-npc",2,9000,2000)&&schedule.mayAttempt("blocked-npc",2,11000,2000),"Next daily window inherited the previous window's retry");
        System.out.println("NpcActivityScheduleTest PASS nativeNeedsFirst=true tenDistinctSites=true districtEveningSites=true staggeredVisits=true boundedWindows=true nightRest=true failedRouteRetry=5000ms perNpc=true");
    }
}
