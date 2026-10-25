package localforest.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import net.bytebuddy.asm.Advice;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.CompanionView;
import localforest.companions.CompanionWorldData;
import localforest.ai.NpcMind;

/** Recruited-companion work and journal controls. Conversation lives in SceneConversation. */
public final class CompanionPanel extends Form {
    private static WeakReference<MainGame> current=new WeakReference<>(null);
    private static CompanionPanel panel;
    private final Client client;
    private boolean expanded;
    private boolean showMemories;
    private int journalSection,journalPage;
    private long journalStamp=Long.MIN_VALUE;
    private String selected="";
    private long nextRefresh;
    private FormTextButton toggle;
    private FormLabel details,feedback,memories;
    private final List<FormTextButton> workButtons=new ArrayList<>();

    private CompanionPanel(Client client){
        super("mosslumecompanions",300,40);this.client=client;setPosition(12,58);
        addToggle();
    }
    private void addToggle(){toggle=addComponent(new FormTextButton(expanded?"伙伴分工 · 收起":"伙伴分工",8,2,284));toggle.onClicked(e->{expanded=!expanded;rebuild();});}
    public static void tick(MainGame game){
        Client c=game.getClient();
        if(!localforest.world.IslandWorld.enabled()||c==null||c.getLocalServer()==null)return;
        SceneConversation.tick(game);
        if(c.getPlayer()==null||game.formManager==null)return;
        if(current.get()!=game||panel==null||panel.isDisposed()||panel.client!=c){current=new WeakReference<>(game);panel=new CompanionPanel(c);game.formManager.addComponent(panel);}
        boolean interactive=game.isRunning()&&!c.isDead&&!game.showMap()&&!game.formManager.hasFocusForm();
        panel.setHidden(!interactive);
        panel.refresh();
    }
    public static void openForChecks(){if(panel!=null&&!panel.expanded){panel.expanded=true;panel.rebuild();}}
    /** Normal interaction opens the small world-conversation composer. */
    public static void openForMob(Client client,int mobId){
        if(panel!=null&&!panel.isDisposed()&&panel.client==client&&panel.expanded){panel.expanded=false;panel.rebuild();}
        SceneConversation.openForMob(client,mobId);
    }
    /** Available from a recruited companion's conversation "分工" button. */
    public static void openManagementForMob(Client client,int mobId){
        if(panel==null||panel.isDisposed()||panel.client!=client)return;
        CompanionView view=panel.recruitedCompanions().stream().filter(v->v.mobId()==mobId).findFirst().orElse(null);
        if(view==null)return;
        panel.selected=view.id();panel.expanded=true;panel.showMemories=false;panel.rebuild();
    }
    private List<CompanionView> recruitedCompanions(){
        return CompanionSystem.snapshot(client).companions().stream().filter(CompanionView::recruited).toList();
    }
    private CompanionView selection(){
        List<CompanionView> all=recruitedCompanions();
        if(all.isEmpty())return null;
        CompanionView v=all.stream().filter(p->p.id().equals(selected)).findFirst().orElse(all.get(0));selected=v.id();return v;
    }
    private void rebuild(){
        clearComponents();workButtons.clear();details=null;feedback=null;memories=null;addToggle();
        setHeight(expanded?(showMemories?424:408):40);if(!expanded)return;
        FormTextButton choose=addComponent(new FormTextButton("切换伙伴",12,42,176));
        choose.onClicked(e->{List<CompanionView> all=recruitedCompanions();if(!all.isEmpty()){int i=0;for(;i<all.size();i++)if(all.get(i).id().equals(selected))break;selected=all.get((i+1)%all.size()).id();journalPage=0;rebuild();}});
        FormTextButton journal=addComponent(new FormTextButton(showMemories?"任务":"共同经历",196,42,92));
        journal.onClicked(e->{showMemories=!showMemories;rebuild();});
        if(showMemories){
            rebuildJournal(selection());
            return;
        }
        details=addComponent(new FormLabel("伙伴正在载入",new FontOptions(16),-1,14,82,272));
        feedback=addComponent(new FormLabel("安排营地分工；靠近伙伴交互即可交谈。",new FontOptions(14),-1,14,152,270));
        addAction("跟随我","FOLLOW",12,202);addAction("守卫这里","GUARD",106,202);addAction("回住所","HOME",200,202);
        addAction("采收一处","HARVEST",12,240);addAction("搬运一批","HAUL",106,240);addAction("日常工作","WORK",200,240);
        addAction("暂停","PAUSE",12,278);addAction("继续","RESUME",106,278);addAction("取消","CANCEL",200,278);
        FormTextButton business=addComponent(new FormTextButton("交易与营地管理",12,316,176));business.onClicked(e->command("OPEN_NATIVE"));workButtons.add(business);
        FormTextButton rescue=addComponent(new FormTextButton("救治伙伴",196,316,92));rescue.onClicked(e->command("OPEN_RESCUE"));workButtons.add(rescue);
        memories=addComponent(new FormLabel("",new FontOptions(12),-1,14,364,270));
        refreshNow();
    }
    private void addAction(String title,String action,int x,int y){FormTextButton button=addComponent(new FormTextButton(title,x,y,88));button.onClicked(e->command(action));workButtons.add(button);}
    private void command(String action){
        CompanionView v=selection();if(v==null){if(feedback!=null)feedback.setText("还没有已加入营地的伙伴",270);return;}
        if(client.getPlayer()==null)return;
        String message=CompanionSystem.submit(client,v.id(),action,client.getPlayer().getTileX(),client.getPlayer().getTileY(),1);
        if(feedback!=null)feedback.setText(shorten(message,48),270);nextRefresh=0;
    }
    private void refresh(){
        if(!expanded)return;long now=System.currentTimeMillis();if(now<nextRefresh)return;nextRefresh=now+250;refreshNow();
    }
    private void refreshNow(){
        if(!expanded)return;
        if(showMemories){
            if(journalStamp!=journalStamp(selection()))rebuild();
            return;
        }
        if(details==null)return;
        CompanionView v=selection();
        details.setText(v==null?"还没有已加入营地的伙伴":v.name()+" · "+v.state()+"\n"+CompanionWorldData.actionLabel(v.action())+"\n"+shorten(v.message(),34),272);
        workButtons.forEach(button->button.setActive(v!=null));
        memories.setText(v!=null&&!v.memories().isEmpty()?"最近："+shorten(v.memories().get(v.memories().size()-1).text(),22):"",270);
    }
    private void rebuildJournal(CompanionView view){
        journalStamp=journalStamp(view);
        journalText(view==null?"还没有已加入营地的伙伴":view.name()+" · 共同经历",16,84,1);
        if(view==null||view.mind()==null){journalText("相识之后，这里会慢慢留下你们的故事。",14,132,3);return;}
        String[] sections={"相处","记得的事","来往"};
        for(int i=0;i<sections.length;i++){
            final int section=i;
            FormTextButton tab=addComponent(new FormTextButton(sections[i],12+i*94,112,88));
            tab.setActive(i!=journalSection);
            tab.onClicked(e->{journalSection=section;journalPage=0;rebuild();});
        }
        NpcMind.Snapshot mind=view.mind();
        switch(journalSection){
            case 1 -> memoryPage(mind);
            case 2 -> relationshipPage(mind);
            default -> characterPage(mind);
        }
    }
    private void characterPage(NpcMind.Snapshot mind){
        var traits=mind.traits();
        String trust=traits.trust()<.25?"还在保持距离":traits.trust()<.45?"正慢慢熟悉你":traits.trust()<.7?"逐渐信任你的安排":"已经很信任你";
        String caution=traits.caution()<.35?"比较敢于尝试":traits.caution()<.65?"会先确认风险":"遇到危险会更谨慎";
        String social=traits.sociability()<.35?"习惯安静观察":traits.sociability()<.65?"熟悉后愿意交谈":"喜欢主动找人说话";
        journalText("对你的信任 · "+trust,14,158,1);
        journalText("遇到危险时 · "+caution,14,182,1);
        journalText("与别人相处 · "+social,14,206,1);
        journalText("最近放在心上的事",14,246,1);
        Set<String> witnessed=mind.memories().stream().filter(m->m.kind()==NpcMind.Kind.GAME_EVENT).map(NpcMind.Memory::id).collect(Collectors.toSet());
        var thoughts=mind.reflections().stream().filter(r->!r.evidenceIds().isEmpty()&&witnessed.containsAll(r.evidenceIds()))
            .sorted(Comparator.comparingLong(NpcMind.Reflection::time).reversed()).limit(2).toList();
        if(thoughts.isEmpty())journalText("还没有足够的共同经历。今后的相处会一点点留下痕迹。",14,272,4);
        else for(int i=0;i<thoughts.size();i++){
            String text=thoughts.get(i).text().replace("经历整理（已发生的游戏事件）：","");
            journalText("· "+friendlyText(text),14,270+i*56,2);
        }
        journalText("相处会随共同经历慢慢变化。",12,394,1);
    }
    private void memoryPage(NpcMind.Snapshot mind){
        var entries=mind.memories().stream().sorted(Comparator.comparingLong(NpcMind.Memory::time).reversed()
            .thenComparingInt(m->memoryOrder(m.kind()))).toList();
        int pages=Math.max(1,(entries.size()+1)/2);journalPage=Math.min(journalPage,pages-1);
        if(entries.isEmpty())journalText("还没有记下什么。和伙伴说说你的喜好，也可以一起完成一件事。",14,164,5);
        for(int i=0;i<2;i++){
            int index=journalPage*2+i;if(index>=entries.size())break;
            NpcMind.Memory memory=entries.get(index);int y=158+i*104;
            journalText(memoryLabel(memory.kind()),12,y,1);
            journalText(friendlyText(memory.text()),14,y+22,3);
        }
        journalText("你说过的话与亲身经历会分别记住。",12,370,1);
        addJournalPaging(pages);
    }
    private void relationshipPage(NpcMind.Snapshot mind){
        var relations=mind.relationships().stream().filter(r->r.name()!=null&&!r.name().isBlank())
            .sorted(Comparator.comparingLong(NpcMind.Relationship::lastInteraction).reversed()).toList();
        int pages=Math.max(1,(relations.size()+1)/2);journalPage=Math.min(journalPage,pages-1);
        if(relations.isEmpty())journalText("还没有熟悉的邻居。营地闲下来时，伙伴们会有机会靠近、聊聊。",14,164,5);
        for(int i=0;i<2;i++){
            int index=journalPage*2+i;if(index>=relations.size())break;
            NpcMind.Relationship relation=relations.get(index);int y=160+i*100;
            journalText(relation.name(),16,y,1);
            String familiarity=relation.familiarity()<.1?"刚刚开始熟悉":relation.familiarity()<.35?"已经渐渐熟悉":"已经很熟悉彼此";
            String trust=relation.trust()<.3?"相处仍有些保留":relation.trust()<.6?"正在慢慢建立信任":"对彼此比较信任";
            journalText(familiarity+"。",14,y+28,1);
            journalText(trust+"。",14,y+50,1);
        }
        journalText("这里记着实际交谈过的人。",12,370,1);
        addJournalPaging(pages);
    }
    private void addJournalPaging(int pages){
        if(pages<=1)return;
        FormTextButton previous=addComponent(new FormTextButton("上一页",12,390,88));previous.setActive(journalPage>0);
        previous.onClicked(e->{if(journalPage>0){journalPage--;rebuild();}});
        addComponent(new FormLabel((journalPage+1)+" / "+pages,new FontOptions(12),0,150,400));
        FormTextButton next=addComponent(new FormTextButton("下一页",200,390,88));next.setActive(journalPage+1<pages);
        next.onClicked(e->{if(journalPage+1<pages){journalPage++;rebuild();}});
    }
    /** Each Chinese line gets its own baseline: native FormLabel's fontSize line spacing is too tight. */
    private void journalText(String text,int fontSize,int y,int maxLines){
        if(maxLines<=0)return;
        String value=text==null?"":text;FontOptions font=new FontOptions(fontSize);
        List<String> lines=new ArrayList<>();StringBuilder line=new StringBuilder();
        int[] points=value.codePoints().toArray();
        for(int point:points){
            if(point=='\r')continue;
            if(point=='\n'){lines.add(line.toString());line.setLength(0);continue;}
            String glyph=new String(Character.toChars(point));
            if(line.length()>0&&FontManager.bit.getWidthCeil(line.toString()+glyph,font)>272){lines.add(line.toString());line.setLength(0);}
            line.append(glyph);
        }
        if(line.length()>0||lines.isEmpty())lines.add(line.toString());
        if(lines.size()>maxLines){
            String last=lines.get(maxLines-1);
            while(!last.isEmpty()&&FontManager.bit.getWidthCeil(last+"…",font)>272)
                last=last.substring(0,last.offsetByCodePoints(last.length(),-1));
            lines.set(maxLines-1,last+"…");
        }
        int lineStep=Math.max(20,fontSize+8);
        for(int i=0;i<Math.min(maxLines,lines.size());i++)
            addComponent(new FormLabel(lines.get(i),new FontOptions(fontSize),-1,14,y+i*lineStep));
    }
    private static int memoryOrder(NpcMind.Kind kind){return switch(kind){case GAME_EVENT->0;case PLAYER_NAME->1;case PLAYER_PREFERENCE->2;case COMMITMENT->3;case SOCIAL->4;case PLAYER_CLAIM->5;};}
    private static String memoryLabel(NpcMind.Kind kind){return switch(kind){
        case GAME_EVENT -> "一起经历过";
        case PLAYER_CLAIM -> "你曾说起 · 你的讲述";
        case PLAYER_PREFERENCE -> "你说过的喜好";
        case PLAYER_NAME -> "你希望的称呼";
        case COMMITMENT -> "你提出的约定 · 等待兑现";
        case SOCIAL -> "与别人交谈";
    };}
    private static String friendlyText(String text){return text.replace("原生工作","工作").replace("按原生救援结果恢复","按救援情况恢复")
        .replace("原生日程","日程").replace("原生林业区","林业区").replace("原生远征任务","远征任务")
        .replace("原生救援","救援").replace("原生事务","营地事务").replace("玩家自述偏好：","你说过：")
        .replace("玩家希望被称呼为","你希望被称呼为").replace("玩家提出的约定（未确认履行）：","你提出的约定：").replace("玩家说：","你说：").replace("原生","");}
    private static long journalStamp(CompanionView view){
        if(view==null||view.mind()==null)return 0;
        var mind=view.mind();
        return Objects.hash(view.id(),mind.traits(),mind.memories(),mind.reflections(),mind.relationships());
    }
    @Override public void dispose(){
        super.dispose();
        if(panel==this){panel=null;current=new WeakReference<>(null);}
    }
    private static String shorten(String s,int max){return s.codePointCount(0,s.length())<=max?s:s.substring(0,s.offsetByCodePoints(0,max))+"…";}
    @ModMethodPatch(target=MainGame.class,name="frameTick",arguments={TickManager.class,GameWindow.class})
    public static class InstallPatch {
        @Advice.OnMethodExit public static void after(@Advice.This MainGame game){CompanionPanel.tick(game);}
    }
}
