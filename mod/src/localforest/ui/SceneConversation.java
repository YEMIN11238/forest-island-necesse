package localforest.ui;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import localforest.ai.AiIntentService;
import localforest.companions.CompanionSystem;
import localforest.companions.CompanionSystem.CompanionView;
import localforest.companions.CompanionSystem.ConversationReply;
import necesse.engine.input.InputEvent;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.gameFont.FontOptions;

/** Small in-world conversation composer; the actual dialogue belongs above the speakers. */
public final class SceneConversation extends Form {
    private static WeakReference<MainGame> current=new WeakReference<>(null);
    private static SceneConversation composer;
    private final Client client;
    private final ConcurrentLinkedQueue<Reply> replies=new ConcurrentLinkedQueue<>();
    private record Reply(long version,int mobId,ConversationReply result) {}
    private String selected="",level="",draft="",statusText="回车发送 · Esc 结束交谈";
    private int selectedMob,widthUsed;
    private boolean opened,pending;
    private long version,deadline;
    private FormTextInput input;
    private FormTextButton send;
    private FormLabel status;

    private SceneConversation(Client client){super("mosslumeconversation",480,106);this.client=client;setHidden(true);}
    public static void tick(MainGame game){
        Client client=game.getClient();
        if(client==null||client.getPlayer()==null||game.formManager==null)return;
        WorldDialogueBubbles.tick(client);
        if(current.get()!=game||composer==null||composer.isDisposed()){
            if(composer!=null&&!composer.isDisposed())composer.closeConversation();
            current=new WeakReference<>(game);composer=new SceneConversation(client);game.formManager.addComponent(composer);
        }
        CompanionSystem.updateScenePresence(client,game.isRunning()&&!client.isDead&&!game.showMap()&&!game.formManager.hasFocusForm(),composer.opened?composer.selected:"");
        for(var line:CompanionSystem.pollWorldSpeech(client))WorldDialogueBubbles.npc(client,line.mobId(),line.text());
        if(!composer.opened)return;
        if(!game.isRunning()||client.isDead||game.showMap()||game.formManager.hasFocusForm()
            ||client.getLevel()==null||!composer.level.equals(client.getLevel().getIdentifier().toString())){
            composer.closeConversation();return;
        }
        composer.update();
    }
    public static void openForMob(Client client,int mobId){
        if(composer==null||composer.client!=client||composer.isDisposed())return;
        CompanionView person=CompanionSystem.snapshot(client).companions().stream().filter(v->v.mobId()==mobId).findFirst().orElse(null);
        if(person==null||client.getLevel()==null)return;
        if(!composer.selected.equals(person.id())){
            composer.cancelPending();composer.draft="";
            if(composer.input!=null&&!composer.input.isDisposed())composer.input.setText("");
        }
        composer.selected=person.id();composer.selectedMob=mobId;composer.level=person.level();
        composer.opened=true;composer.statusText="回车发送 · Esc 结束交谈";composer.setHidden(false);composer.rebuild();
        CompanionSystem.updateScenePresence(client,true,person.id());
        if(composer.input.isActive())composer.input.setTyping(true);
    }
    private CompanionView person(){return CompanionSystem.snapshot(client).companions().stream().filter(v->v.id().equals(selected)).findFirst().orElse(null);}
    private void rebuild(){
        boolean typing=input!=null&&!input.isDisposed()&&input.isTyping();
        if(input!=null&&!input.isDisposed())draft=input.getText();
        clearComponents();GameWindow window=WindowManager.getWindow();widthUsed=Math.max(300,Math.min(480,window.getHudWidth()-24));setWidth(widthUsed);
        CompanionView person=person();String name=person==null?"角色":person.name();
        addComponent(new FormLabel("与 "+name+" 交谈",new FontOptions(16),-1,12,12,widthUsed-226));
        if(person!=null&&person.recruited())addComponent(new FormTextButton("分工",widthUsed-210,4,72)).onClicked(e->{
            closeConversation();CompanionPanel.openManagementForMob(client,selectedMob);
        });
        addComponent(new FormTextButton("事务",widthUsed-132,4,72)).onClicked(e->nativeBusiness());
        addComponent(new FormTextButton("×",widthUsed-52,4,40)).onClicked(e->closeConversation());
        input=addComponent(new FormTextInput(12,38,FormInputSize.SIZE_32_TO_40,widthUsed-108,200));input.setText(draft);
        input.placeHolder=new StaticMessage("对 "+name+" 说点什么……");
        // Native text fields also submit on blur and Escape. Neither should send a paid request.
        input.onSubmit(e->{if(e.event!=null&&e.event.state&&(e.event.getID()==257||e.event.getID()==335)){
            sendText();e.preventDefault();e.event.use();
        }});
        send=addComponent(new FormTextButton("发送",widthUsed-84,38,72));send.onClicked(e->sendText());
        status=addComponent(new FormLabel(statusText,new FontOptions(12),-1,12,83,widthUsed-24));
        input.setActive(AiIntentService.isConfigured());send.setActive(AiIntentService.isConfigured()&&!pending);
        if(typing&&input.isActive())input.setTyping(true);
        anchor(window);
    }
    private void anchor(GameWindow window){setPosition(Math.max(12,(window.getHudWidth()-getWidth())/2),Math.max(12,window.getHudHeight()-getHeight()-82));}
    @Override public void onWindowResized(GameWindow window){super.onWindowResized(window);if(opened)rebuild();else anchor(window);}
    private void sendText(){
        if(!opened||pending||!AiIntentService.isConfigured())return;
        CompanionView person=person();if(person==null){showStatus("对方暂时不在这里。");return;}
        Mob mob=GameUtils.getLevelMob(person.mobId(),client.getLevel());PlayerMob player=client.getPlayer();
        if(mob==null||player==null||!mob.isSamePlace(player)||!mob.inInteractRange(player)){
            showStatus("靠近对方，再和他交谈。");return;
        }
        String message=input.getText().trim();if(message.isEmpty())return;
        pending=true;long request=++version;deadline=System.nanoTime()+22_000_000_000L;
        WorldDialogueBubbles.player(client,message);draft="";input.setText("");input.setTyping(true);send.setActive(false);
        showStatus(person.name()+"正在想……");
        CompanionSystem.submitConversation(client,selected,message,result->replies.add(new Reply(request,person.mobId(),result)));
    }
    private void update(){
        CompanionView person=person();PlayerMob player=client.getPlayer();
        Mob target=client.getLevel()==null?null:GameUtils.getLevelMob(selectedMob,client.getLevel());
        if(person==null||target==null||player==null||!target.isSamePlace(player)||!target.inInteractRange(player)||person.downed()||person.threatened()){
            closeConversation();return;
        }
        if(widthUsed!=Math.max(300,Math.min(480,WindowManager.getWindow().getHudWidth()-24)))rebuild();
        if(pending&&System.nanoTime()>deadline){cancelPending();showStatus("暂时没有收到回复，请稍后再说。");}
        Reply next;
        while((next=replies.poll())!=null){
            if(next.version()!=version||next.mobId()!=selectedMob)continue;
            pending=false;
            if(!next.result().dialogue().isBlank()){
                WorldDialogueBubbles.npc(client,next.mobId(),next.result().dialogue());
                CompanionSystem.confirmConversationReply(client,selected,next.result());
            }
            showStatus(next.result().status().isBlank()?"回车发送 · Esc 结束交谈":next.result().status());
        }
        boolean connected=AiIntentService.isConfigured();input.setActive(connected);send.setActive(connected&&!pending);
        if(!connected)showStatus("暂时无法回应；可以点击“事务”继续交互。");
    }
    private void nativeBusiness(){
        CompanionView person=person();if(person==null)return;
        PlayerMob player=client.getPlayer();if(player==null)return;
        CompanionSystem.submit(client,person.id(),"OPEN_NATIVE",player.getTileX(),player.getTileY(),1);closeConversation();
    }
    private void showStatus(String text){statusText=text;if(status!=null&&!status.isDisposed())status.setText(text.length()>64?text.substring(0,64)+"…":text,widthUsed-24);}
    private void cancelPending(){version++;pending=false;replies.clear();if(!selected.isEmpty())CompanionSystem.cancelConversation(client,selected);}
    private void closeConversation(){
        cancelPending();opened=false;
        CompanionSystem.updateScenePresence(client,false,"");
        if(input!=null&&!input.isDisposed()){draft=input.getText();input.setTyping(false);}
        setHidden(true);
    }
    @Override public void handleInputEvent(InputEvent event,TickManager tick,PlayerMob perspective){
        if(opened&&event.state&&event.getID()==256){event.use();closeConversation();return;}
        super.handleInputEvent(event,tick,perspective);
    }
    @Override public void dispose(){closeConversation();super.dispose();if(composer==this){composer=null;current=new WeakReference<>(null);}}
}
