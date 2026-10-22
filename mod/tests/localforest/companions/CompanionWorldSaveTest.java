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
        save.addSaveData(resident);
        CompanionWorldData first=new CompanionWorldData();first.applyLoadData(save.toLoadData());
        SaveData written=new SaveData(CompanionSystem.WORLD_KEY);first.addSaveData(written);
        CompanionWorldData second=new CompanionWorldData();second.applyLoadData(written.toLoadData());
        var restored=second.view.companions().get(0);
        check(restored.id().equals("stable-linshu")&&restored.mobId()==128,"Companion identity changed");
        check(restored.downed()&&restored.revision()==9,"Downed identity or request invalidation lost");
        check(restored.action().equals("HAUL")&&restored.message().contains("存档已恢复"),"Task resume guard lost");
        check(restored.memories().size()==64&&restored.memories().get(0).id().equals("event-0"),"Bounded event memory or important event lost");
        check(!first.sessionId.equals(second.sessionId),"Reload must create a fresh request session");
        RequestEpochs gate=new RequestEpochs();long a=gate.issue("linshu"),b=gate.issue("xiaohe");
        gate.invalidate("linshu");check(!gate.isCurrent("linshu",a)&&gate.isCurrent("xiaohe",b),"Command invalidation leaked across companions");
        long c=gate.issue("linshu");gate.invalidateAll();check(!gate.isCurrent("linshu",c)&&!gate.isCurrent("xiaohe",b),"Paused/reloaded UI retained stale request");
        long next=gate.issue("linshu");check(next>c&&!gate.isCurrent("linshu",c),"Old ticket reused");
        System.out.println("PASS: native WorldData identity/downed/task/memory round trip; immediate request invalidation");
    }
}
