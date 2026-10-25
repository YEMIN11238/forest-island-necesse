package localforest.companions;

import java.awt.Point;
import java.lang.reflect.Field;
import java.util.Map;
import localforest.ai.NpcMind;
import necesse.engine.registries.WorldDataRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

/** Tests actual CompanionWorldData migration and save wiring without a client or model. */
public final class NpcCognitionSaveTest {
    private static final String ALICE="stable-alice",BOB="stable-bob";
    public static void main(String[] args) throws Exception {
        WorldDataRegistry.registerWorldData("mosslumecompanions",CompanionWorldData.class);
        SaveData legacy=new SaveData(CompanionSystem.WORLD_KEY);legacy.addInt("schema",1);
        SaveData alice=resident(ALICE,"林叔",128,"木匠");
        event(alice,"legacy-join",1000,"joined","林叔成为营地伙伴","native.settler");
        event(alice,"legacy-volcano",180_000,"downed","林叔曾在火山熔岩旁重伤","native.health");
        event(alice,"legacy-recovered",360_000,"rescued","林叔已恢复活动，救援者未确认","native.health");
        event(alice,"legacy-task",540_000,"task_completed","林叔已把营地木材搬入仓库","native.task");
        for(int i=0;i<8;i++)dialogue(alice,550_000+i*1000,i==0?"请叫我阿森。我最喜欢蓝莓。我不喜欢蘑菇。我们约好明晚一起去遗迹。":"我今天看到了第"+i+"片树叶。",i==0?"好，阿森，我们到时候再看。":"树叶很好看。");
        SaveData bob=resident(BOB,"小月",129,"旅人");
        dialogue(bob,600_000,"我最喜欢橙子。","我记住了。");
        legacy.addSaveData(alice);legacy.addSaveData(bob);
        SaveData social=new SaveData("npcSocial");
        for(String id:new String[]{"already-reported-rescue","already-reported-task","night:9"}){SaveData item=new SaveData("reported");item.addSafeString("id",id);social.addSaveData(item);}
        last(social,ALICE,900_000);last(social,BOB,800_000);
        pendingMove(social,ALICE,new Point(3024,3600));pendingMove(social,BOB,new Point(3088,3536));
        legacy.addSaveData(social);

        CompanionWorldData first=new CompanionWorldData();first.applyLoadData(new LoadData(legacy.getScript()));
        var migrated=find(first,ALICE);
        String context=NpcMind.context(migrated.mind(),"记得蓝莓和火山那次经历吗？",1_000_000);
        check(context.contains("蓝莓")&&context.contains("火山"),"Schema 1 migration omitted actual events or eight-turn player preferences");
        check(migrated.mind().memories().stream().anyMatch(m->m.kind()==NpcMind.Kind.PLAYER_NAME&&m.text().contains("阿森")),"Legacy preferred address did not become a typed long-term record");
        check(migrated.mind().memories().stream().anyMatch(m->m.kind()==NpcMind.Kind.COMMITMENT&&m.text().contains("未确认履行")),"Legacy player promise was lost or treated as completed");
        check(migrated.mind().memories().stream().filter(m->m.kind()==NpcMind.Kind.GAME_EVENT).count()==4,"Legacy verified receipts were mixed with conversation claims");
        var baseline=new NpcMind(ALICE,"林叔","木匠").snapshot().traits();
        check(migrated.mind().traits().trust()>baseline.trust()&&migrated.mind().traits().caution()>baseline.caution(),"Historical task/injury receipts were not used for one-time growth");
        check(migrated.recentConversation().size()==8,"WorldData migration changed native recent-history bound");
        check(find(first,BOB).mind().memories().stream().noneMatch(m->m.text().contains("蓝莓")||m.text().contains("火山")||m.text().contains("阿森")),"One companion inherited another's mind during migration");

        // Inject observations into the actual world-owned domain object; the production
        // WorldData writer, parser and reader below are used without a substitute format.
        NpcMind aliceMind=ownedMind(first,ALICE);
        for(int i=0;i<90;i++) {
            long now=1_000_000+i*180_000;
            aliceMind.observeDialogue(now,"沿路看到了第"+i+"块石头。","继续往前走吧。");
            String type=i%7==0?"task_completed":"task_accepted";
            aliceMind.observeEvent("later-task-"+i,now,type,(i%7==0?"搬运了":"收到搬运指令：")+"第"+i+"批建材","native.task");
        }
        long socialAt=18_000_000;
        aliceMind.observeSocial(socialAt,BOB,"小月","小月问起了那次火山救援，我说还记得当时的危险。");
        var grown=aliceMind.snapshot().traits();
        check(grown.trust()>baseline.trust()&&grown.trust()<.9&&grown.caution()>.1,"Fixture saturated traits and could hide an incorrect replay on reload");
        SaveData written=save(first);
        check(written.toLoadData().getLoadDataByName("companion").stream().allMatch(c->c.getFirstLoadDataByName("npcMind")!=null),"WorldData writer failed to include each NPC mind");
        checkSocial(written.toLoadData());
        CompanionWorldData second=reload(written);
        var restored=find(second,ALICE);
        String longTerm=NpcMind.context(restored.mind(),"你怎么称呼我，还记得我喜欢吃什么和火山经历吗？",19_000_000);
        check(longTerm.contains("阿森")&&longTerm.contains("蓝莓")&&longTerm.contains("火山"),"Actual world save lost important facts after more than eight recent turns");
        check(restored.mind().traits().equals(grown),"World reload replayed old receipts and inflated personality traits");
        check(restored.mind().relationships().stream().anyMatch(r->r.peerId().equals(BOB)&&r.lastInteraction()==socialAt),"Native world save omitted NPC-to-NPC relationship history");
        check(restored.recentConversation().size()==8,"Long-term domain state unexpectedly expanded recent chat transcript");
        var foreign=find(second,BOB).mind();
        check(foreign.memories().stream().noneMatch(m->m.text().contains("蓝莓")||m.text().contains("火山")),"Roundtrip crossed NPC long-term memory boundaries");
        check(foreign.memories().stream().anyMatch(m->m.text().contains("橙子")),"Independent NPC memory was dropped");
        var relationship=restored.mind().relationships().stream().filter(r->r.peerId().equals(BOB)).findFirst().orElseThrow();
        ownedMind(second,ALICE).observeSocial(socialAt+1000,BOB,"小月","duplicate replay after reload");
        check(ownedMind(second,ALICE).snapshot().relationships().stream().filter(r->r.peerId().equals(BOB)).findFirst().orElseThrow().equals(relationship),"Mind social cooldown was not restored by world serialization");
        for(int i=0;i<3;i++) {
            SaveData again=save(second);checkSocial(again.toLoadData());second=reload(again);
            check(find(second,ALICE).mind().traits().equals(grown),"Repeated world roundtrip recalculated/increased personality");
        }
        System.out.println("NpcCognitionSaveTest PASS: actual WorldData schema-1 migration, typed evidence and 8-turn claims, 90 additional observations through native save/parse/reload, long-term preference/event recall, traits not replayed, NPC isolation, social relationship cooldown, reported/last timestamps and pending native social-guard ownership markers persisted across repeated reloads; marker consumption requires native runtime QA; no game or paid model");
    }
    private static SaveData resident(String id,String name,int mobId,String role) {
        SaveData c=new SaveData("companion");c.addSafeString("id",id);c.addSafeString("name",name);c.addInt("mobId",mobId);c.addSafeString("level","mosslume_fixture");
        c.addInt("x",94);c.addInt("y",112);c.addInt("homeX",94);c.addInt("homeY",113);c.addLong("revision",0);c.addBoolean("downed",false);c.addBoolean("recruited",true);c.addSafeString("role",role);return c;
    }
    private static void event(SaveData c,String id,long time,String type,String text,String source) {SaveData e=new SaveData("memory");e.addSafeString("id",id);e.addLong("time",time);e.addSafeString("type",type);e.addSafeString("text",text);e.addSafeString("source",source);c.addSaveData(e);}
    private static void dialogue(SaveData c,long time,String player,String reply) {SaveData d=new SaveData("dialogue");d.addLong("time",time);d.addSafeString("player",player);d.addSafeString("reply",reply);c.addSaveData(d);}
    private static void last(SaveData c,String id,long time) {SaveData d=new SaveData("last");d.addSafeString("id",id);d.addLong("time",time);c.addSaveData(d);}
    private static void pendingMove(SaveData c,String id,Point target) {SaveData d=new SaveData("pendingMove");d.addSafeString("id",id);d.addPoint("target",target);c.addSaveData(d);}
    private static CompanionSystem.CompanionView find(CompanionWorldData world,String id) {return world.view.companions().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();}
    private static SaveData save(CompanionWorldData world) {SaveData result=new SaveData(CompanionSystem.WORLD_KEY);world.addSaveData(result);return result;}
    private static CompanionWorldData reload(SaveData data) {CompanionWorldData world=new CompanionWorldData();world.applyLoadData(new LoadData(data.getScript()));return world;}
    private static NpcMind ownedMind(CompanionWorldData world,String id) throws Exception {
        Field companions=CompanionWorldData.class.getDeclaredField("companions");companions.setAccessible(true);
        Object companion=((Map<?,?>)companions.get(world)).get(id);Field mind=companion.getClass().getDeclaredField("mind");mind.setAccessible(true);return (NpcMind)mind.get(companion);
    }
    private static void checkSocial(LoadData world) {
        LoadData social=world.getFirstLoadDataByName("npcSocial");check(social!=null,"Social runtime payload missing from WorldData");
        var reported=social.getLoadDataByName("reported").stream().map(c->c.getSafeString("id")).toList();
        check(reported.size()==3&&reported.contains("already-reported-rescue")&&reported.contains("already-reported-task")&&reported.contains("night:9"),"Previously spoken event IDs were dropped/duplicated on world reload");
        check(social.getLoadDataByName("last").stream().anyMatch(c->c.getSafeString("id").equals(ALICE)&&c.getLong("time")==900_000)
            &&social.getLoadDataByName("last").stream().anyMatch(c->c.getSafeString("id").equals(BOB)&&c.getLong("time")==800_000),"Social per-person last interaction timestamps were lost");
        var pending=social.getLoadDataByName("pendingMove");
        check(pending.size()==2&&pending.stream().anyMatch(c->c.getSafeString("id").equals(ALICE)&&new Point(3024,3600).equals(c.getPoint("target")))
            &&pending.stream().anyMatch(c->c.getSafeString("id").equals(BOB)&&new Point(3088,3536).equals(c.getPoint("target"))),"Pending native social guard ownership marker was lost, altered, or duplicated before NPC load/consumption");
    }
    private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
}
