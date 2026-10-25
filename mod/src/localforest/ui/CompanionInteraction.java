package localforest.ui;

import localforest.companions.CompanionSystem;
import localforest.companions.CompanionWorldData;
import localforest.world.IslandWorld;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.humanShop.HumanShop;
import net.bytebuddy.asm.Advice;

/** Routes registered, healthy single-player companions to their conversation panel. */
@ModMethodPatch(target=HumanShop.class,name="interact",arguments={PlayerMob.class})
public final class CompanionInteraction {
    private static final ThreadLocal<HumanMob> nativeInteraction=new ThreadLocal<>();

    @Advice.OnMethodEnter(skipOn=Advice.OnNonDefaultValue.class)
    public static boolean enter(@Advice.This HumanShop mob,@Advice.Argument(0) PlayerMob player){
        // Advice is inlined into a native class, so its helper must be public.
        return CompanionInteraction.route(mob,player);
    }

    public static boolean route(HumanShop mob,PlayerMob player){
        if(!IslandWorld.enabled()||mob==null||player==null||nativeInteraction.get()==mob
            ||mob.isDowned()||!mob.isSamePlace(player))return false;
        String level=mob.getLevel().getIdentifier().toString();
        if(mob.isServer()){
            Server server=mob.getServer();
            if(server==null||!server.isSingleplayer()||!player.isServerClient()
                ||server.getLocalServerClient()!=player.getServerClient())return false;
            var data=server.world.worldEntity.getWorldData(CompanionSystem.WORLD_KEY);
            // The native interaction packet still validates range and broadcasts to
            // the client; suppress only its shop container for our registered NPC.
            return data instanceof CompanionWorldData companions
                &&companions.isRegisteredMob(mob.getUniqueID(),level);
        }
        if(!mob.isClient())return false;
        Client client=mob.getClient();
        if(client==null||!client.isSingleplayer()||client.getLocalServer()==null
            ||client.getPlayer()!=player)return false;
        boolean registered=CompanionSystem.snapshot(client).companions().stream()
            .anyMatch(p->p.mobId()==mob.getUniqueID()&&p.level().equals(level));
        if(!registered)return false;
        CompanionPanel.openForMob(client,mob.getUniqueID());
        return true;
    }

    /** Called by the validated server command to retain native trade/settler forms. */
    public static void openNative(HumanMob mob,PlayerMob player){
        HumanMob previous=nativeInteraction.get();
        nativeInteraction.set(mob);
        try{mob.interact(player);}
        finally{
            if(previous==null)nativeInteraction.remove();
            else nativeInteraction.set(previous);
        }
    }

    private CompanionInteraction(){}
}
