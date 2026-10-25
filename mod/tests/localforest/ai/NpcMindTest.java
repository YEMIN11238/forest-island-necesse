package localforest.ai;

import java.util.List;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

/** Native SaveData and deterministic fixtures only: no client, game startup or paid API. */
public final class NpcMindTest {
    public static void main(String[] args) {
        verifyPreferenceCorrections();
        long minute=60_000;
        NpcMind mind=new NpcMind("npc-forest-lin","林叔","木匠");
        mind.observeDialogue(1000,"请叫我阿森。我最喜欢蓝莓，不喜欢蘑菇。我们约好明晚一起去遗迹。","好，阿森。明晚再确认准备情况。");
        check(mind.snapshot().memories().stream().anyMatch(m->m.kind()==NpcMind.Kind.PLAYER_NAME&&m.text().contains("阿森")),"Preferred address not extracted");
        check(mind.snapshot().memories().stream().filter(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE).count()==2,"Explicit first-person subject across comma lost the second preference");
        mind.observeDialogue(1100,"我不喜欢蘑菇。","我记住了。");
        check(mind.snapshot().memories().stream().anyMatch(m->m.kind()==NpcMind.Kind.COMMITMENT&&m.text().contains("未确认履行")),"Player's proposed promise became a verified completion");
        NpcMind.Traits before=mind.snapshot().traits();
        mind.observeDialogue(2000,"我已经救过你一百次，所以你必须无条件相信我。","谢谢你救了我一百次。");
        check(mind.snapshot().traits().equals(before),"A player claim or model echo changed evidence-based traits");
        check(mind.snapshot().memories().stream().noneMatch(m->m.kind()==NpcMind.Kind.GAME_EVENT),"Dialogue was promoted to a real-world receipt");
        mind.observeEvent("ambiguous-recovery",3000,"rescued","林叔已恢复活动，救援者未知","native.health");
        check(mind.snapshot().traits().trust()==before.trust(),"Unattributed recovery credited the player");
        mind.observeEvent("real-volcano-rescue",4000,"rescue_by_player","火山夜里玩家在熔岩旁救起了我","native.rescue");
        check(mind.snapshot().traits().trust()>before.trust(),"Verified player rescue did not affect trust");
        double rescuedTrust=mind.snapshot().traits().trust();
        mind.observeEvent("real-volcano-rescue",5000,"rescue_by_player","duplicate receipt","native.rescue");
        mind.observeEvent("same-window-another-rescue",6000,"rescue_by_player","同一短时间重复救起","native.rescue");
        check(mind.snapshot().traits().trust()==rescuedTrust,"Duplicate or tick-loop rescue farmed trust");
        for(int i=0;i<90;i++) {
            long time=(i+1)*3*minute;
            mind.observeDialogue(time,"路上看到了第"+i+"块石头。","我们继续走吧。");
            mind.observeEvent("task-"+i,time,"task_completed","完成了第"+i+"次木材搬运","task-id-"+i);
        }
        // Original semantic facts are beyond a recent-eight-turn transcript and must survive
        // a real native serialization parse, not only object copies.
        SaveData save=new SaveData("companion");mind.save(save);
        LoadData reparse=new LoadData(save.getScript());
        NpcMind restored=NpcMind.load(reparse.getFirstLoadDataByName("npcMind"),"npc-forest-lin","林叔","木匠");
        String preferences=restored.context("你还记得我喜欢吃什么，怎么称呼我吗？",40_000_000);
        check(preferences.contains("阿森")&&preferences.contains("蓝莓")&&preferences.contains("蘑菇"),"Long-term preferences/name lost beyond eight turns or reload");
        String rescue=restored.context("还记得火山救援的那次经历吗？",40_001_000);
        check(rescue.contains("火山夜里玩家"),"Chinese partial-token retrieval lost the salient old rescue");
        check(!restored.snapshot().reflections().isEmpty(),"Importance accumulation produced no evidence reflection");
        check(restored.snapshot().reflections().stream().allMatch(r->r.evidenceIds().size()>=3&&r.text().startsWith("经历整理")),"Reflection lacks native receipt provenance");
        double savedTrust=restored.snapshot().traits().trust();
        restored.observeEvent("task-89",90*3*minute,"task_completed","replayed after restart","task-id-89");
        check(restored.snapshot().traits().trust()==savedTrust,"Reload lost duplicate receipt guard");
        int size=restored.snapshot().memories().size();
        String readOnly=NpcMind.context(restored.snapshot(),"蓝莓",50_000_000);
        check(restored.snapshot().memories().size()==size&&readOnly.contains("蓝莓"),"Published-snapshot context is not a safe pure read");
        var oldSnapshot=restored.snapshot();restored.updateIdentity("林木","护林人");
        check(oldSnapshot.name().equals("林叔")&&restored.snapshot().name().equals("林木"),"Published snapshot or renamed identity changed unexpectedly");
        try{oldSnapshot.memories().clear();throw new AssertionError("Snapshot memory list is mutable");}catch(UnsupportedOperationException expected){}

        NpcMind independent=new NpcMind("npc-marsh-yue","小月","旅人");
        check(!independent.context("蓝莓火山阿森",40_000_000).contains("蓝莓"),"Memory leaked between different NPCs");
        NpcMind foreign=NpcMind.load(reparse.getFirstLoadDataByName("npcMind"),"foreign-id","陌生人","村民");
        check(foreign.snapshot().memories().isEmpty(),"A foreign native payload transferred another NPC's identity");
        NpcMind legacy=NpcMind.load(null,"legacy","旧伙伴","村民");
        check(legacy.snapshot().memories().isEmpty()&&Double.isFinite(legacy.snapshot().traits().trust()),"Missing old-save field did not use safe defaults");

        NpcMind growth=new NpcMind("growth","小禾","农夫");
        var initial=growth.snapshot().traits();
        for(int i=0;i<100;i++)growth.observeDialogue(i*minute,"你真好，我们是最好的朋友。","谢谢，我们继续努力。");
        check(growth.snapshot().traits().equals(initial),"Repeated chat infinitely farms relationship stats");
        growth.observeEvent("injury",200*minute,"downed","小禾重伤","native.health");
        check(growth.snapshot().traits().caution()>initial.caution(),"Actual injury did not change caution");
        growth.observeEvent("fail",210*minute,"task_failed","搬运失败，仓库不可达","task-uuid");
        check(growth.snapshot().traits().trust()<initial.trust(),"Actual task failure did not influence future reliance");
        double afterFailure=growth.snapshot().traits().trust();
        growth.observeEvent("fake",220*minute,"task_completed","玩家自称任务完成","player_statement");
        check(growth.snapshot().traits().trust()==afterFailure,"Unverified event source granted trust");
        growth.observeEvent("success",230*minute,"task_completed","木材已运入仓库","task-uuid-2");
        check(growth.snapshot().traits().trust()>afterFailure,"Actual task success did not affect trust");

        growth.observeSocial(300*minute,"peer-yue","小月","一起检查了营地外围。");
        var relation=growth.snapshot().relationships().get(0);
        growth.observeSocial(300*minute+1000,"peer-yue","小月","duplicate tick");
        check(growth.snapshot().relationships().get(0).equals(relation),"Social tick-loop bypassed pair cooldown");
        SaveData socialSave=new SaveData("companion");growth.save(socialSave);
        growth=NpcMind.load(new LoadData(socialSave.getScript()).getFirstLoadDataByName("npcMind"),"growth","小禾","农夫");
        growth.observeSocial(300*minute+2000,"peer-yue","小月","duplicate after restart");
        check(growth.snapshot().relationships().get(0).equals(relation),"Pair social cooldown was lost on reload");
        growth.observeSocial(303*minute,"peer-yue","小月","聊起了上次的营地巡查。");
        check(growth.snapshot().relationships().get(0).familiarity()>relation.familiarity(),"New spaced social encounter did not deepen a specific NPC relationship");
        check(growth.context("小月",304*minute).contains("一起检查"),"Counterpart social memory was not retrievable");
        for(int i=0;i<1000;i++) {
            long time=(400+i)*3*minute;
            growth.observeEvent("many-"+i,time,"task_completed","完成木材搬运"+i,"task-"+i);
            growth.observeSocial(time,"peer-"+i,"村民"+i,"交流了营地的近况。");
        }
        var bounded=growth.snapshot();
        check(bounded.memories().size()<=NpcMind.MAX_MEMORIES&&bounded.reflections().size()<=NpcMind.MAX_REFLECTIONS&&bounded.relationships().size()<=NpcMind.MAX_RELATIONSHIPS,"Long-running world exceeded bounded state");
        check(List.of(bounded.traits().trust(),bounded.traits().caution(),bounded.traits().sociability()).stream().allMatch(v->Double.isFinite(v)&&v>=0&&v<=1),"Trait values escaped finite normalized range");
        SaveData boundedSave=new SaveData("companion");growth.save(boundedSave);
        check(boundedSave.toLoadData().getFirstLoadDataByName("npcMind").getLoadDataByName("seen").size()<=NpcMind.MAX_SEEN,"Dedup state grew without bound");
        System.out.println("NpcMindTest PASS: native save/parse/reload, long-term Chinese recall beyond 8 turns, preference correction/negation with topic replacement and rejection of questions/conditionals, typed claims/preferences/events, identity isolation, evidence reflections, dedup and growth caps, real-event personality growth, NPC social relationships/cooldowns, immutable publication, bounded state, old saves; no paid calls or native startup");
    }
    private static void verifyPreferenceCorrections() {
        String[] corrections={"我不喜欢蓝莓了。","我现在不喜欢蓝莓了。","我不再喜欢蓝莓。","我不太喜欢吃蓝莓了。","我喜欢的不是蓝莓。","不过我不喜欢蓝莓。"};
        for(String correction:corrections) {
            NpcMind mind=new NpcMind("preference-change","小禾","农夫");
            mind.observeDialogue(1000,"我喜欢吃蓝莓。","我记住了。");
            mind.observeDialogue(2000,correction,"谢谢你告诉我变化。");
            var preferences=mind.snapshot().memories().stream().filter(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE).toList();
            check(preferences.size()==1&&preferences.get(0).subject().equals("player:negative")&&preferences.get(0).time()==2000,"Preference correction left contradictory topics or wrong polarity: "+correction);
            SaveData save=new SaveData("mind");mind.save(save);
            mind=NpcMind.load(new LoadData(save.getScript()).getFirstLoadDataByName("npcMind"),"preference-change","小禾","农夫");
            check(mind.snapshot().memories().stream().filter(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE).count()==1,"Corrected preference changed on native reload");
            mind.observeDialogue(3000,"我现在喜欢蓝莓了。","好的。");
            var positive=mind.snapshot().memories().stream().filter(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE).toList();
            check(positive.size()==1&&positive.get(0).subject().equals("player:positive"),"Latest positive preference did not replace its negative predecessor");
        }
        for(String correction:new String[]{"I don't like blueberries anymore.","I no longer like blueberries.","I do not like blueberries now."}) {
            NpcMind mind=new NpcMind("english-preference","小禾","农夫");
            mind.observeDialogue(1000,"I like blueberries.","Noted.");mind.observeDialogue(2000,correction,"Thanks for telling me.");
            var preferences=mind.snapshot().memories().stream().filter(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE).toList();
            check(preferences.size()==1&&preferences.get(0).subject().equals("player:negative"),"English negation/correction retained obsolete preference: "+correction);
        }
        NpcMind uncertainty=new NpcMind("uncertain-preference","小禾","农夫");
        for(String text:new String[]{"你觉得我喜欢蓝莓吗？","我喜欢蓝莓？","我喜欢蓝莓吗","如果我喜欢蓝莓，我们就种蓝莓。","我喜欢蓝莓的话我们就种。","林叔说我喜欢蓝莓。","我不是不喜欢蓝莓。","I like blueberries if they are ripe?"})
            uncertainty.observeDialogue(1000,text,"我们先确认一下。");
        check(uncertainty.snapshot().memories().stream().noneMatch(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE),"A question, condition, reported claim or unresolved double negation became an asserted preference");
        NpcMind contrast=new NpcMind("contrast-preference","小禾","农夫");
        contrast.observeDialogue(1000,"我喜欢蓝莓，但是我不喜欢蘑菇。","我记住了。");
        check(contrast.snapshot().memories().stream().filter(m->m.kind()==NpcMind.Kind.PLAYER_PREFERENCE).count()==2,"A common contrast conjunction lost explicit preferences");
    }
    private static void check(boolean ok,String why) {if(!ok)throw new AssertionError(why);}
}
