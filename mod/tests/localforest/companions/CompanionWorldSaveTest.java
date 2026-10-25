package localforest.companions;

import localforest.tasks.CompanionTask;
import necesse.engine.registries.WorldDataRegistry;
import necesse.engine.save.SaveData;

/** Exercises the native WorldData payload, not a separate substitute save format. */
public final class CompanionWorldSaveTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        WorldDataRegistry.registerWorldData("mosslumecompanions",CompanionWorldData.class);
        SaveData save=new SaveData(CompanionSystem.WORLD_KEY);save.addInt("schema",1);
        SaveData resident=new SaveData("companion");resident.addSafeString("id","stable-linshu");
        resident.addSafeString("name","林叔");resident.addInt("mobId",128);resident.addSafeString("level","mosslume_test");
        resident.addBoolean("downed",true);resident.addLong("revision",7);resident.addInt("x",46);resident.addInt("y",22);resident.addInt("homeX",45);resident.addInt("homeY",21);
        CompanionTask task=new CompanionTask("HAUL",46,22,1,1000);task.transition(CompanionTask.State.RUNNING,"运送中",2000);
        resident.addSaveData(task.save());
        for(int i=0;i<80;i++){
            SaveData m=new SaveData("memory");m.addSafeString("id","event-"+i);m.addLong("time",i*100);
            m.addSafeString("type",i==0?"joined":"task_accepted");m.addSafeString("text",i==0?"林叔成为伙伴":"完成营地记录 "+i);m.addSafeString("source","native");resident.addSaveData(m);
        }
        for(int i=0;i<10;i++){
            SaveData d=new SaveData("dialogue");d.addLong("time",i*100);d.addSafeString("player","请叫我阿森 "+i);d.addSafeString("reply","好，阿森 "+i);resident.addSaveData(d);
        }
        save.addSaveData(resident);
        SaveData visitor=new SaveData("companion");visitor.addSafeString("id","merchant-1");visitor.addSafeString("name","阿穆");visitor.addInt("mobId",129);
        visitor.addSafeString("level","mosslume_test");visitor.addInt("x",60);visitor.addInt("y",30);visitor.addInt("homeX",-1);visitor.addInt("homeY",-1);visitor.addBoolean("downed",false);visitor.addLong("revision",0);
        visitor.addBoolean("recruited",false);visitor.addSafeString("role","旅行商人");save.addSaveData(visitor);
        CompanionWorldData first=new CompanionWorldData();first.applyLoadData(save.toLoadData());
        SaveData written=new SaveData(CompanionSystem.WORLD_KEY);first.addSaveData(written);
        CompanionWorldData second=new CompanionWorldData();second.applyLoadData(written.toLoadData());
        var restored=second.view.companions().get(0);
        check(restored.id().equals("stable-linshu")&&restored.mobId()==128,"Companion identity changed");
        check(restored.downed()&&restored.revision()==9,"Downed identity or request invalidation lost");
        check(restored.action().equals("HAUL")&&restored.message().contains("存档已恢复"),"Task resume guard lost");
        check(restored.memories().size()==64&&restored.memories().get(0).id().equals("event-0"),"Bounded event memory or important event lost");
        check(restored.recruited()&&restored.role().equals("营地伙伴"),"Legacy save lost recruited default");
        check(restored.recentConversation().size()==8&&restored.recentConversation().get(0).playerText().endsWith("2")&&restored.recentConversation().get(7).replyText().endsWith("9"),"Bounded dialogue turn persistence lost chronological pairing");
        var merchant=second.view.companions().get(1);check(!merchant.recruited()&&merchant.role().equals("旅行商人")&&merchant.recentConversation().isEmpty(),"Non-recruited role/save identity lost");
        check(CompanionSystem.ConversationReply.status("网络暂不可用").dialogue().isEmpty(),"Technical status became dialogue");
        check(!first.sessionId.equals(second.sessionId),"Reload must create a fresh request session");
        RequestEpochs gate=new RequestEpochs();long a=gate.issue("linshu"),b=gate.issue("xiaohe");
        gate.invalidate("linshu");check(!gate.isCurrent("linshu",a)&&gate.isCurrent("xiaohe",b),"Command invalidation leaked across companions");
        long c=gate.issue("linshu");gate.invalidateAll();check(!gate.isCurrent("linshu",c)&&!gate.isCurrent("xiaohe",b),"Paused/reloaded UI retained stale request");
        long next=gate.issue("linshu");check(next>c&&!gate.isCurrent("linshu",c),"Old ticket reused");
        long cancelled=gate.issue("linshu");gate.invalidate("linshu");
        check(!gate.acceptDialogue("linshu",cancelled)&&!gate.consumeDialogue(cancelled),"Cancelled unseen reply entered dialogue memory");
        long delivered=gate.issue("linshu");check(gate.acceptDialogue("linshu",delivered),"Visible valid reply was not accepted");
        gate.issue("linshu");
        check(gate.consumeDialogue(delivered)&&!gate.consumeDialogue(delivered),"New conversation erased already spoken reply or duplicated its memory");
        long exiting=gate.issue("linshu");gate.acceptDialogue("linshu",exiting);gate.invalidateAll();
        check(!gate.consumeDialogue(exiting),"World exit retained queued dialogue receipts");
        check(CompanionWorldData.validDialoguePlace("surface","surface","surface",true),"Same-place conversation rejected");
        check(!CompanionWorldData.validDialoguePlace("surface","cave","surface",true),"Player map change admitted a late reply");
        check(!CompanionWorldData.validDialoguePlace("surface","surface","cave",true),"NPC map change admitted a late reply");
        check(!CompanionWorldData.validDialoguePlace("surface","surface","surface",false),"Unloaded NPC admitted a late reply");
        necesse.engine.registries.GNDRegistry.registerGNDItem("map",necesse.engine.network.gameNetworkData.GNDItemMap.class);
        necesse.engine.registries.GNDRegistry.registerGNDItem("string",necesse.engine.network.gameNetworkData.GNDItemString.class);
        String uuid="c19d0373-cd85-4f8d-837d-4725e7faf35a";
        SaveData npcSave=new SaveData("HUMAN");NativeNpcIdentity.writePayload(npcSave,uuid);
        String restoredUuid=NativeNpcIdentity.readPayload(npcSave.toLoadData());
        check(uuid.equals(restoredUuid),"NPC-owned UUID did not survive native GND/SaveData round trip");
        check(NativeNpcIdentity.matchesStable(uuid,restoredUuid),"Persistent identity failed across a map or numeric-ID change");
        check(!NativeNpcIdentity.matchesStable(uuid,"4f48f559-4145-44d1-888a-364ff53475ef"),"Different NPC UUID inherited memories");
        var legacy=new NativeNpcIdentity.Legacy("",128,"surface","林叔","elderhuman",42);
        check(NativeNpcIdentity.canMigrate(legacy,128,"surface","林叔","elderhuman",42),"Exact legacy NPC match failed");
        check(!NativeNpcIdentity.canMigrate(legacy,128,"cave","林叔","elderhuman",42),"Same numeric ID in another map stole legacy memory");
        check(!NativeNpcIdentity.canMigrate(legacy,128,"surface","路人","elderhuman",42),"Reused numeric ID with a different name stole memory");
        check(!NativeNpcIdentity.canMigrate(legacy,128,"surface","林叔","farmerhuman",42),"Reused numeric ID with a different native role stole memory");
        check(!NativeNpcIdentity.canMigrate(legacy,128,"surface","林叔","elderhuman",43),"Reused numeric ID with a different native seed stole memory");
        check(!NativeNpcIdentity.canMigrate(new NativeNpcIdentity.Legacy(uuid,128,"surface","林叔","elderhuman",42),128,"surface","林叔","elderhuman",42),"Already-bound UUID was reassigned via numeric ID");
        SaveData boundSave=new SaveData(CompanionSystem.WORLD_KEY);boundSave.addInt("schema",1);
        resident.addSafeString("nativeUuid",uuid);resident.addSafeString("nativeType","elderhuman");resident.addInt("identitySeed",42);boundSave.addSaveData(resident);
        CompanionWorldData bound=new CompanionWorldData();bound.applyLoadData(boundSave.toLoadData());
        SaveData boundWritten=new SaveData(CompanionSystem.WORLD_KEY);bound.addSaveData(boundWritten);
        var identity=boundWritten.toLoadData().getFirstLoadDataByName("companion");
        check(identity.getSafeString("nativeUuid").equals(uuid)&&identity.getSafeString("nativeType").equals("elderhuman")&&identity.getInt("identitySeed")==42,"World companion/native identity binding was not persisted");
        System.out.println("PASS: native WorldData identity/downed/task/memory round trip; immediate request invalidation; recruited/visitor identity, paired 8-turn dialogue history, typed status, cancelled/accepted receipts and map/unload rejection; NPC-owned UUID and strict legacy identity migration");
    }
}
