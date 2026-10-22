package localforest.ui;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.gameFont.FontOptions;
import net.bytebuddy.asm.Advice;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.CompanionView;
import localforest.companions.CompanionWorldData;

/** Native form controls; the panel uses the engine's real hit testing and input routing. */
public final class CompanionPanel extends Form {
    private static WeakReference<MainGame> current=new WeakReference<>(null);
    private static CompanionPanel panel;
    private final Client client;
    private boolean expanded;
    private boolean showMemories;
    private boolean wasRunning=true;
    private String selected="";
    private String lastText="";
    private long nextRefresh;
    private FormTextButton toggle;
    private FormLabel details,feedback,memories,modelState;
    private FormTextInput input;
    private FormTextButton send;
    private record Reply(long version,String message) {}
    private final ConcurrentLinkedQueue<Reply> replies=new ConcurrentLinkedQueue<>();
    private final AtomicLong replyVersion=new AtomicLong();

    private CompanionPanel(Client client){
        super("mosslumecompanions",300,40);this.client=client;setPosition(12,58);
        addToggle();
    }
    private void addToggle(){toggle=addComponent(new FormTextButton(expanded?"伙伴与营地  ·  收起":"伙伴与营地",8,2,284));toggle.onClicked(e->{expanded=!expanded;if(!expanded)invalidateReplies();rebuild();});}
    public static void tick(MainGame game){
        Client c=game.getClient();
        if(!localforest.world.IslandWorld.enabled()||c==null||c.getLocalServer()==null||c.getPlayer()==null||game.formManager==null)return;
        if(current.get()!=game||panel==null||panel.isDisposed()){current=new WeakReference<>(game);panel=new CompanionPanel(c);game.formManager.addComponent(panel);}
        boolean interactive=game.isRunning()&&!c.isDead&&!game.showMap()&&!game.formManager.hasFocusForm();
        panel.setHidden(!interactive);
        if(panel.wasRunning&&!interactive)panel.invalidateReplies();
        panel.wasRunning=interactive;panel.refresh();
    }
    public static void openForChecks(){if(panel!=null&&!panel.expanded){panel.expanded=true;panel.rebuild();}}
    private CompanionView selection(){
        List<CompanionView> all=CompanionSystem.snapshot(client).companions();
        if(all.isEmpty())return null;
        CompanionView v=all.stream().filter(p->p.id().equals(selected)).findFirst().orElse(all.get(0));selected=v.id();return v;
    }
    private void rebuild(){
        if(input!=null&&!input.isDisposed())lastText=input.getText();
        clearComponents();addToggle();
        setHeight(expanded?438:40);if(!expanded)return;
        FormTextButton choose=addComponent(new FormTextButton("切换伙伴",12,42,176));
        choose.onClicked(e->{List<CompanionView> all=CompanionSystem.snapshot(client).companions();if(!all.isEmpty()){invalidateReplies();int i=0;for(;i<all.size();i++)if(all.get(i).id().equals(selected))break;selected=all.get((i+1)%all.size()).id();rebuild();}});
        FormTextButton journal=addComponent(new FormTextButton(showMemories?"任务":"共同经历",196,42,92));
        journal.onClicked(e->{showMemories=!showMemories;rebuild();});
        if(showMemories){
            CompanionView v=selection();details=null;feedback=null;memories=null;
            addComponent(new FormLabel(v==null?"伙伴正在载入":v.name()+"记得的共同经历",new FontOptions(16),-1,14,84,272));
            if(v!=null){
                var events=CompanionSystem.recall(client,v.id(),"",6);
                for(int i=0;i<events.size();i++)addComponent(new FormLabel(shorten(events.get(i).text(),42),new FontOptions(14),-1,14,116+i*48,272));
            }
            return;
        }
        details=addComponent(new FormLabel("伙伴正在载入",new FontOptions(16),-1,14,82,272));
        addAction("跟随我","FOLLOW",12,146);addAction("守卫这里","GUARD",106,146);addAction("回住所","HOME",200,146);
        addAction("采收一处","HARVEST",12,184);addAction("搬运一批","HAUL",106,184);addAction("日常工作","WORK",200,184);
        addAction("暂停","PAUSE",12,222);addAction("继续","RESUME",106,222);addAction("取消","CANCEL",200,222);
        FormTextButton rescue=addComponent(new FormTextButton("靠近伙伴后打开救治对话",12,260,276));rescue.onClicked(e->command("OPEN_RESCUE"));
        feedback=addComponent(new FormLabel("",new FontOptions(14),-1,14,300,270));
        modelState=addComponent(new FormLabel("模型稍后配置 · 离线任务可用",new FontOptions(14),-1,14,342,270));
        input=addComponent(new FormTextInput(12,365,FormInputSize.SIZE_32_TO_40,196,200));input.setText(lastText);
        send=addComponent(new FormTextButton("发送",216,365,72));
        send.onClicked(e->{CompanionView v=selection();if(v==null)return;lastText=input.getText();long version=replyVersion.incrementAndGet();CompanionSystem.submitText(client,v.id(),lastText,message->replies.add(new Reply(version,message)));});
        memories=addComponent(new FormLabel("",new FontOptions(12),-1,14,410,270));
        refreshNow();
    }
    private void addAction(String title,String action,int x,int y){addComponent(new FormTextButton(title,x,y,88)).onClicked(e->command(action));}
    private void command(String action){
        long version=replyVersion.incrementAndGet();
        CompanionView v=selection();if(v==null){replies.add(new Reply(version,"伙伴尚未载入"));return;}
        if(client.getPlayer()==null)return;
        replies.add(new Reply(version,CompanionSystem.submit(client,v.id(),action,client.getPlayer().getTileX(),client.getPlayer().getTileY(),1)));nextRefresh=0;
    }
    private void refresh(){
        if(!expanded)return;long now=System.currentTimeMillis();if(now<nextRefresh)return;nextRefresh=now+250;refreshNow();
    }
    private void refreshNow(){
        if(!expanded||details==null)return;
        CompanionView v=selection();
        details.setText(v==null?"还没有已加入营地的伙伴":v.name()+" · "+v.state()+"\n"+CompanionWorldData.actionLabel(v.action())+"\n"+shorten(v.message(),34),272);
        Reply r;while((r=replies.poll())!=null)if(r.version()==replyVersion.get())feedback.setText(shorten(r.message(),35),270);
        if(v!=null&&!v.memories().isEmpty()){
            var recent=v.memories().get(v.memories().size()-1);
            if(recent.type().equals("request_blocked"))feedback.setText(shorten(recent.text(),35),270);
        }
        boolean model=localforest.ai.AiIntentService.isConfigured();input.setActive(model);send.setActive(model);
        modelState.setText(model?"AI 接口已连接，指令仍由游戏校验":"模型稍后配置 · 离线任务可用",270);
        if(v!=null&&!v.memories().isEmpty())memories.setText("最近："+shorten(v.memories().get(v.memories().size()-1).text(),22),270);
    }
    private void invalidateReplies(){replyVersion.incrementAndGet();replies.clear();CompanionSystem.invalidateModelRequests(client);}
    @Override public void dispose(){
        invalidateReplies();super.dispose();
        if(panel==this){panel=null;current=new WeakReference<>(null);}
    }
    private static String shorten(String s,int max){return s.length()<=max?s:s.substring(0,max)+"…";}
    @ModMethodPatch(target=MainGame.class,name="frameTick",arguments={TickManager.class,GameWindow.class})
    public static class InstallPatch {
        @Advice.OnMethodExit public static void after(@Advice.This MainGame game){CompanionPanel.tick(game);}
    }
}
