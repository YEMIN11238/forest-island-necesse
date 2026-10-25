package localforest.bosses;

import java.util.Set;
import java.util.Properties;

public final class BossAttackTimelineTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        BossAttackTimeline attack=new BossAttackTimeline();
        check(attack.phase(2000)==BossAttackTimeline.Phase.READY&&!attack.impact(2000),"Idle timeline inflicted damage");
        attack.begin(1000,700,120,500);
        check(attack.phase(1699)==BossAttackTimeline.Phase.WINDUP&&!attack.impact(1699),"Damage preceded telegraph");
        check(attack.spriteColumn(1699)==3&&attack.spriteColumn(1700)==4,"Sprite windows differ from native damage timing");
        check(attack.phase(1700)==BossAttackTimeline.Phase.STRIKE&&attack.impact(1700),"Strike boundary missed");
        check(!attack.impact(1701)&&!attack.impact(2200),"One attack applied impact more than once");
        check(attack.phase(1819)==BossAttackTimeline.Phase.STRIKE&&attack.phase(1820)==BossAttackTimeline.Phase.RECOVERY,"Active interval boundary wrong");
        check(!attack.complete(2319)&&attack.complete(2320)&&attack.spriteColumn(2320)==0,"Recovery boundary wrong");
        attack.clear();check(!attack.running()&&!attack.impact(9999),"Cleared attack retained hit");
        attack.begin(5000,500,420,500);
        check(attack.telegraphProgress(4990)==0&&attack.telegraphProgress(5500)==1,"Clock delta or clamping wrong");
        check(attack.impact(5550)&&!attack.impact(5550),"Delayed server tick duplicated dash impact");
        attack.clear();attack.begin(8000,350,100,400);check(attack.impact(8350),"Phase two attack failed to reset impact guard");
        try{attack.begin(0,100,100,100);throw new AssertionError("Invisible windup allowed");}catch(IllegalArgumentException expected){}
        SourceActionClip dash=new SourceActionClip("moon-reaper-attack1",14,192,96,168,90,10,new int[]{0,1,2,3,4,5,6,7},new int[]{8,9,10,11},new int[]{12,13},Set.of(5,6,7));
        check(dash.minimumWindupMillis()==720&&dash.minimumActiveMillis()==360&&dash.minimumRecoveryMillis()==180,"Source 90ms frame durations lost");
        check(dash.damageDelayMillis()==180,"Reappearance became early invisible damage");
        attack.clear();attack.begin(10_000,850,420,500);
        check(!attack.impact(10_850,dash.damageDelayMillis())&&!attack.impact(11_029,dash.damageDelayMillis()),"Generic native impact preceded original source damage pose");
        check(attack.impact(11_030,dash.damageDelayMillis())&&!attack.impact(11_031,dash.damageDelayMillis()),"Delayed source damage was lost or duplicated");
        attack.clear();attack.begin(20_000,750,120,600);check(!attack.impact(21_300),"Missed strike window caused invisible recovery damage after a server stall");
        check(dash.pick(0,850,420).column()==0&&dash.pick(219,850,420).column()==0&&dash.pick(220,850,420).column()==1,"Native extra windup stretched original authored frames");
        check(dash.pick(849,850,420).column()==7&&dash.pick(850,850,420).column()==8&&dash.pick(1030,850,420).column()==10,"Authored vanish/reappear/slash order was changed");
        check(dash.pick(1270,850,420).column()==12&&dash.pick(1449,850,420).column()==13&&!dash.pick(1450,850,420).source(),"Source recovery failed to return to native idle");
        Properties props=new Properties();props.setProperty("boss.file","moon-reaper-attack1");props.setProperty("boss.columns","14");props.setProperty("boss.frameSize","192");props.setProperty("boss.footX","96");props.setProperty("boss.footY","168");props.setProperty("boss.windup","0,1,2,3,4,5,6,7");props.setProperty("boss.strike","8,9,10,11");props.setProperty("boss.recovery","12,13");props.setProperty("boss.emptyFrames","5,6,7");props.setProperty("boss.damageFrame","10");
        SourceActionClip read=SourceActionClip.read(props,"boss");check(read.frameSize()==192&&read.footY()==168&&read.damageDelayMillis()==180&&read.emptyFrames().equals(Set.of(5,6,7)),"Variable source canvas, grip or invisible-frame metadata lost");
        try{new SourceActionClip("bad",14,192,96,168,90,5,new int[]{0,1},new int[]{8,9},new int[]{10,11},Set.of());throw new AssertionError("Non-strike source damage frame allowed");}catch(IllegalArgumentException expected){}
        System.out.println("PASS: boss windows and exactly-once damage; original 90ms source animation, variable 192px canvas, explicit invisible frames, delayed reappearance/slash damage and idle recovery");
    }
}
