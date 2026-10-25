package localforest.companions;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanInteractingAINode;
import necesse.entity.mobs.friendly.human.HumanMob;
import net.bytebuddy.asm.Advice;

/** Short server-owned player conversation lease. Never stores or cancels a player's job order. */
public final class ConversationLease {
    static final long TIMEOUT_NANOS=1_500_000_000L;
    private static final Map<HumanMob,ConversationLease> HELD=Collections.synchronizedMap(new WeakHashMap<>());
    private HumanMob mob;
    private ServerClient player;
    private long renewed;
    private String person="";

    public static boolean eligible(boolean present,boolean alive,boolean samePlace,boolean inRange,
        boolean downed,boolean threatened,boolean fresh){
        return present&&alive&&samePlace&&inRange&&!downed&&!threatened&&fresh;
    }
    boolean renew(String id,HumanMob next,ServerClient client,boolean threatened,long now){
        boolean valid=next!=null&&client!=null&&client.playerMob!=null&&eligible(!next.removed(),!client.isDead(),
            next.isSamePlace(client.playerMob),next.inInteractRange(client.playerMob),next.isDowned(),threatened,true);
        if(!valid){release();return false;}
        if(mob!=next||player!=client){release();mob=next;player=client;person=id;HELD.put(next,this);}
        renewed=now;hold();return true;
    }
    String person(){return person;}
    private void hold(){
        mob.refreshInteracting(player);
        if(mob.objectUser!=null)mob.objectUser.stopUsing();
        if(mob.ai!=null)mob.ai.blackboard.mover.stopMoving(mob);
        mob.stopMoving();mob.dx=0;mob.dy=0;
        int oldDirection=mob.getDir();
        mob.setFacingDir(player.playerMob.x-mob.x,player.playerMob.y-mob.y);
        // Setting the direction before HumanInteractingAINode means that node no longer observes
        // a change itself. Publish it here so the client sees the same facing as the server.
        if(oldDirection!=mob.getDir())mob.sendMovementPacket(false);
    }
    void release(){
        HumanMob previous=mob;ServerClient previousPlayer=player;
        mob=null;player=null;person="";
        if(previous!=null){HELD.remove(previous);if(previousPlayer!=null)previous.removeInteracting(previousPlayer);}
        // HumanInteractingAINode emits resetPathTime after the lease ends. Existing jobs/orders survive.
    }
    /** The recovered kernel's cache expiry is inverted. Refresh immediately before its native AI node,
     * rather than modifying that kernel or allowing a separate movement/teleport controller. */
    public static void refreshForNativeNode(HumanMob mob){
        ConversationLease lease=HELD.get(mob);if(lease==null)return;
        long now=System.nanoTime();
        ServerClient player=lease.player;
        boolean danger=mob.isAttacking||mob.commandAttackMob!=null;
        if(player==null||player.playerMob==null||!eligible(!mob.removed(),!player.isDead(),mob.isSamePlace(player.playerMob),
            mob.inInteractRange(player.playerMob),mob.isDowned(),danger,now-lease.renewed<TIMEOUT_NANOS)){
            lease.release();return;
        }
        lease.hold();
    }
    @ModMethodPatch(target=HumanInteractingAINode.class,name="tick",arguments={HumanMob.class,Blackboard.class})
    public static final class NativeInteractionRefresh {
        @Advice.OnMethodEnter public static void before(@Advice.Argument(0) HumanMob mob){ConversationLease.refreshForNativeNode(mob);}
    }
}
