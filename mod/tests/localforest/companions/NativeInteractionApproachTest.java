package localforest.companions;

import java.awt.Point;
import java.lang.reflect.Field;
import necesse.entity.mobs.Mob;

/** Fixture positioning policy checked against actual native Mob.inInteractRange bytecode. */
public final class NativeInteractionApproachTest {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Field field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);var unsafe=(sun.misc.Unsafe)field.get(null);
        Mob actor=(Mob)unsafe.allocateInstance(Mob.class),player=(Mob)unsafe.allocateInstance(Mob.class);
        actor.x=100;actor.y=-40;
        Point interaction=actor.getInteractPos();
        var point=NativeInteractionRepairCheck.approachPosition(interaction,new Point(-200,-100),(x,y)->true);
        check(point!=null&&point.distance(interaction)>=24&&point.distance(interaction)<=50,"Candidate lacks unchanged native range margin");
        player.x=point.x;player.y=point.y;
        check(actor.inInteractRange(player),"Policy's proposed point fails real native interaction range");
        Point east=new Point(interaction.x+32,interaction.y);
        point=NativeInteractionRepairCheck.approachPosition(interaction,east,(x,y)->x<interaction.x);
        check(point!=null&&point.x<interaction.x&&point.distance(interaction)<=50,"Blocked preferred side escaped range instead of choosing a safe side");
        check(NativeInteractionRepairCheck.approachPosition(interaction,east,(x,y)->false)==null,"All-colliding setup fabricated a destination");
        check(NativeInteractionRepairCheck.approachPosition(interaction,east,(x,y)->x==interaction.x+64)==null,"Distant open tile was accepted like old radius-two tile fallback");
        Point appearance=new Point(-830,422);
        point=NativeInteractionRepairCheck.approachPosition(appearance,new Point(100,-40),(x,y)->true);
        check(point.distance(appearance)<=50&&point.distance(new Point(100,-40))>500,"Selection ignored a seated NPC's actual appearance interaction point");
        player.x=interaction.x+75;player.y=interaction.y;check(actor.inInteractRange(player),"Native boundary unexpectedly changed; reassess fixture");
        player.x=interaction.x+76;check(!actor.inInteractRange(player),"Native radius was widened or test did not exercise the actual method");
        System.out.println("NativeInteractionApproachTest PASS actualNativeRange=75 candidateOuterRadius=50 blockedSide=true allBlockedRejected=true seatedAppearancePoint=true negativeCoordinates=true noNpcMutation=true");
    }
}
