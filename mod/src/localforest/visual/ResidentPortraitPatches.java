package localforest.visual;

import java.awt.Rectangle;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import net.bytebuddy.asm.Advice;

/** Native entry-point signatures are checked against the real kernel in ResidentPortraitsTest. */
public final class ResidentPortraitPatches {
    @ModMethodPatch(target=HumanMob.class,name="getCommandFaceDrawOptions",arguments={})
    public static class CommandFace {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static HumanDrawOptions enter(@Advice.This HumanMob mob){return ResidentPortraits.faceOptions(mob,"command");}
        @Advice.OnMethodExit public static void exit(@Advice.Enter HumanDrawOptions replacement,@Advice.Return(readOnly=false)HumanDrawOptions result){if(replacement!=null)result=replacement;}
    }
    @ModMethodPatch(target=HumanMob.class,name="getChatterFaceDrawOptions",arguments={})
    public static class ChatterFace {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static HumanDrawOptions enter(@Advice.This HumanMob mob){return ResidentPortraits.faceOptions(mob,"chatter");}
        @Advice.OnMethodExit public static void exit(@Advice.Enter HumanDrawOptions replacement,@Advice.Return(readOnly=false)HumanDrawOptions result){if(replacement!=null)result=replacement;}
    }
    @ModMethodPatch(target=HumanMob.class,name="drawOnMap",arguments={TickManager.class,Client.class,int.class,int.class,double.class,Rectangle.class,boolean.class})
    public static class MapFace {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static boolean enter(@Advice.This HumanMob mob,@Advice.Argument(0)TickManager tick,@Advice.Argument(1)Client client,@Advice.Argument(2)int x,@Advice.Argument(3)int y){return ResidentPortraits.drawOnMap(mob,tick,client,x,y);}
    }
    @ModMethodPatch(target=Settler.class,name="getSettlerFaceDrawOptions",arguments={int.class,int.class,int.class,Mob.class})
    public static class SettlerFace {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static DrawOptions enter(@Advice.Argument(0)int x,@Advice.Argument(1)int y,@Advice.Argument(2)int size,@Advice.Argument(3)Mob mob){return mob instanceof HumanMob h?ResidentPortraits.icon(h.getLevel(),h.getStringID(),size,x,y,"settler"):null;}
        @Advice.OnMethodExit public static void exit(@Advice.Enter DrawOptions replacement,@Advice.Return(readOnly=false)DrawOptions result){if(replacement!=null)result=replacement;}
    }
    @ModMethodPatch(target=SettlementSettlerData.class,name="getIconDrawOptions",arguments={Level.class,int.class,int.class,int.class})
    public static class SettlementIcon {
        @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
        public static DrawOptions enter(@Advice.This SettlementSettlerData data,@Advice.Argument(0)Level level,@Advice.Argument(1)int size,@Advice.Argument(2)int x,@Advice.Argument(3)int y){
            if(!MosslumeArt.active(level))return null;
            var found=data.getSettlerMob(level);String type=found!=null?found.getMob().getStringID():data.settler.mobStringID;
            return ResidentPortraits.icon(level,type,size,x,y,"settlement");
        }
        @Advice.OnMethodExit public static void exit(@Advice.Enter DrawOptions replacement,@Advice.Return(readOnly=false)DrawOptions result){if(replacement!=null)result=replacement;}
    }
    private ResidentPortraitPatches(){}
}
