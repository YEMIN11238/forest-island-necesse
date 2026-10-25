package localforest.companions;

import java.lang.reflect.Method;
import java.util.HashSet;
import localforest.ai.NpcMind;
import necesse.engine.registries.WorldDataRegistry;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanInteractingAINode;
import necesse.entity.mobs.friendly.human.HumanMob;

/** Content contract plus actual WorldData persistence and native patch signature validation. */
public final class NpcPopulationPolicyTest {
    static void check(boolean good,String why){if(!good)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        check(NpcDefinition.INITIAL.size()==10,"Expected ten permanent starting-region characters");
        check(NpcDefinition.INITIAL.stream().filter(d->d.tier()==NpcDefinition.Tier.CORE).count()==4,"Core companions no longer limited to four");
        check(NpcDefinition.INITIAL.stream().filter(d->d.tier()==NpcDefinition.Tier.RESIDENT).count()==6,"Regional cast missing");
        HashSet<String> ids=new HashSet<>(),names=new HashSet<>();
        for(var d:java.util.stream.Stream.concat(NpcDefinition.INITIAL.stream(),NpcDefinition.VISITORS.stream()).toList()){
            check(ids.add(d.id())&&names.add(d.name()),"Two character templates collide");check(NpcDefinition.find(d.id())==d,"Template identity cannot be resolved");
        }
        Method nativeNode=HumanInteractingAINode.class.getDeclaredMethod("tick",HumanMob.class,Blackboard.class);
        check(nativeNode.getReturnType()==necesse.entity.mobs.ai.behaviourTree.AINodeResult.class,"Native interaction adapter targets the wrong erased signature");
        check(NpcSimulationTier.of(true,true,100)==NpcSimulationTier.NEAR&&NpcSimulationTier.of(true,false,100)==NpcSimulationTier.LOADED_DISTANT
            &&NpcSimulationTier.of(false,false,0)==NpcSimulationTier.UNLOADED,"Remote population is not tiered");
        check(NpcActivitySchedule.phase(22)==NpcActivitySchedule.Phase.REST&&NpcActivitySchedule.phase(7)==NpcActivitySchedule.Phase.MORNING
            &&NpcActivitySchedule.phase(12)==NpcActivitySchedule.Phase.WORK&&NpcActivitySchedule.phase(19)==NpcActivitySchedule.Phase.EVENING,"Sleep/work/social windows invalid");
        check(ConversationLease.eligible(true,true,true,true,false,false,true),"Healthy near conversation cannot hold");
        check(!ConversationLease.eligible(true,true,true,true,false,true,true)&&!ConversationLease.eligible(true,true,false,true,false,false,true)
            &&!ConversationLease.eligible(true,true,true,true,false,false,false),"Danger, map change or expired composer can pin a resident");
        WorldDataRegistry.registerWorldData("mosslumecompanions",CompanionWorldData.class);
        SaveData world=new SaveData(CompanionSystem.WORLD_KEY);world.addInt("schema",1);
        for(int i=0;i<200;i++){
            SaveData npc=new SaveData("companion");npc.addSafeString("id","npc-"+i);npc.addSafeString("name","人物"+i);npc.addInt("mobId",i+1);npc.addSafeString("level","surface");
            npc.addInt("x",0);npc.addInt("y",0);npc.addInt("homeX",-1);npc.addInt("homeY",-1);npc.addBoolean("downed",false);npc.addLong("revision",0);
            npc.addBoolean("recruited",i<4);npc.addSafeString("template",i<4?NpcDefinition.INITIAL.get(i).id():"aqiao");npc.addSafeString("role","居民");npc.addLong("lastSeen",i*1000L);
            var mind=new NpcMind("npc-"+i,"人物"+i,"居民");if(i==5)mind.observeDialogue(1000,"我喜欢蓝莓。","我记住了。");mind.save(npc);world.addSaveData(npc);
        }
        CompanionWorldData data=new CompanionWorldData();data.applyLoadData(world.toLoadData());
        check(data.view.companions().size()==4,"Historical remote residents are all republished as active full snapshots");
        check(((Number)data.diagnostics().get("activeMinds")).longValue()==4,"Unloaded residents eagerly rebuilt their active memory indexes");
        SaveData stored=new SaveData(CompanionSystem.WORLD_KEY);data.addSaveData(stored);
        check(stored.toLoadData().getLoadDataByName("companion").size()==200,"Archived residents lost persistent identity or memories");
        var fifth=stored.toLoadData().getLoadDataByName("companion").get(5);
        check(fifth.getSafeString("template").equals("aqiao")&&fifth.getLong("lastSeen")==5000,"Population tier/template metadata lost during round trip");
        check(NpcMind.context(NpcMind.load(fifth.getFirstLoadDataByName("npcMind"),"npc-5","人物5","居民").snapshot(),"蓝莓",2000).contains("蓝莓"),"Unloaded resident memory was discarded");
        System.out.println("NpcPopulationPolicyTest PASS: 4 core + 6 residents + visitor definitions, native node patch signature, simulation tiers/daily phases, lease eligibility, 200-resident actual WorldData round trip with bounded snapshots and preserved remote memories; no game or model");
    }
}
