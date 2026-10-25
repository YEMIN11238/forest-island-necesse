import localforest.biomes.AttackCycle;
import localforest.biomes.CreatureAnimation;

/** Pure timing checks: actual hit interval and authored impact frame must agree. */
public class CreatureAnimationCheck {
    public static void main(String[] args){
        int[] row={2,1,0,3};
        for(int direction=0;direction<4;direction++)require(CreatureAnimation.directionRow(direction)==row[direction],"native direction maps to authored row");
        for(int duration:new int[]{420,600}){
            AttackCycle cycle=new AttackCycle();cycle.begin(1000,duration);
            for(int ms=0;ms<=duration;ms++){
                int frame=CreatureAnimation.attackFrame(cycle.progress(1000+ms));
                if(cycle.phase(1000+ms)==AttackCycle.Phase.ACTIVE)require(frame==2,"impact uses strike frame");
                if(cycle.phase(1000+ms)==AttackCycle.Phase.RECOVERY)require(frame==3,"recovery never repeats strike");
            }
        }
        require(CreatureAnimation.hurtFrame(0)==0&&CreatureAnimation.hurtFrame(109)==0&&CreatureAnimation.hurtFrame(110)==1,"two hurt phases");
        int last=-1;for(int time=0;time<=900;time++){
            int frame=CreatureAnimation.deathFrame(time);require(frame>=last&&frame<4,"death progresses once, no looping");last=frame;
        }
        require(CreatureAnimation.deathFrame(0)==0&&CreatureAnimation.deathFrame(440)==3,"death first and corpse frame");
        require(CreatureAnimation.deathAlpha(699)==1&&CreatureAnimation.deathAlpha(800)==.5f&&CreatureAnimation.deathAlpha(900)==0,"corpse fades only after settled pose");
        System.out.println("CreatureAnimationCheck passed: four directions, server impact synchronization, hurt phases and non-looping death");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
