package localforest.companions;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import localforest.ai.AiIntentService;
import localforest.ai.ModelProvider;
import localforest.ai.NpcBehaviorPolicy;
import localforest.ai.NpcMind;
import localforest.tasks.NativeWorkAdapter;
import localforest.ui.CompanionPanel;
import localforest.ui.WorldDialogueBubbles;
import localforest.world.WorldDefinition;
import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.level.maps.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A single, disposable native cognition probe. Fixtures are explicitly labelled and never
 * create inventory, new NPCs, terrain, model claims or a paid request. Native movement and
 * the production social adapter must actually run before a presented frame is captured.
 */
public final class NativeCognitionCheck {
    private static final long LIMIT=40_000_000_000L;
    private final Map<String,Object> evidence=new ConcurrentHashMap<>();
    private final AtomicBoolean offline=new AtomicBoolean();
    private volatile int capture;
    private volatile String frameFailure;
    private volatile boolean picturesDone;
    private volatile int actorMob;
    private volatile String actorId="";
    private int phase,renderFrames;
    private long began,captureAt,initialApproaches,initialUtterances;
    private float startX,startY;
    private double moved;
    private NpcSocialRuntime social;
    private CompanionWorldData data;
    private HumanMob actor;
    private CompletableFuture<Void> imageWrite;

    public Map<String,Object> evidence(){return evidence;}
    private static boolean allowed(){
        if(!Boolean.getBoolean("mosslume.refactorProbe.cognition"))return false;
        Path profile=Path.of(GlobalData.appDataPath()).toAbsolutePath().normalize();
        return profile.getFileName()!=null&&profile.getFileName().toString().startsWith("Mosslume-QA-Refactor");
    }
    private void disableModels(){
        if(offline.compareAndSet(false,true)){
            AiIntentService.setProvider(ModelProvider.offline());
            evidence.put("paidModelRequests",0);evidence.put("modelProvider","offline throughout this probe");
            evidence.put("physicalMouseOrKeyboardTested",false);
            evidence.put("fixtureScope","QA-only injected dialogue/history and marked task receipt; existing native NPCs and unmodified camp terrain");
        }
    }
    /** Render/client-thread UI and GL work only. No world-owned memory is mutated here. */
    public void frame(MainGame game){
        if(!allowed())return;
        disableModels();
        try{
            Client client=game.getClient();
            if(client==null||client.isDisconnected()||client.getPlayer()==null||client.getLevel()==null)return;
            if(capture==1){
                // NPC's bubble has to come through the actual social speech queue. Only this
                // player-side fixture response is injected directly into the normal HUD API.
                WorldDialogueBubbles.player(client,"辛苦了，回来歇一会儿吧。");
                WorldDialogueBubbles.tick(client);captureAt=System.nanoTime();renderFrames=0;capture=2;
            }else if(capture==2){
                WorldDialogueBubbles.tick(client);renderFrames++;
                if(renderFrames>=3&&System.nanoTime()-captureAt>300_000_000L){
                    int[] visible=bubbleCounts(actorMob);
                    if(visible[0]<1||visible[1]<1)return;
                    evidence.put("presentedBubbleSpeakers",Map.of("player",visible[0],"socialNpc",visible[1]));
                    savePresentedFrame("cognition-bubbles.png");
                    evidence.put("bubbleFrameReadBuffer","GL_FRONT previous presented native frame");
                    capture=3;renderFrames=0;captureAt=System.nanoTime();
                }
            }else if(capture==3){
                if(imageWrite!=null&&!imageWrite.isDone())return;
                checkImageWrite();
                CompanionPanel.openManagementForMob(client,actorMob);
                Object panel=field(CompanionPanel.class,"panel").get(null);
                if(panel==null)throw new IllegalStateException("Native companion management panel is not installed");
                field(panel.getClass(),"showMemories").setBoolean(panel,true);
                field(panel.getClass(),"journalSection").setInt(panel,2);
                Method rebuild=panel.getClass().getDeclaredMethod("rebuild");rebuild.setAccessible(true);rebuild.invoke(panel);
                capture=4;renderFrames=0;captureAt=System.nanoTime();
            }else if(capture==4){
                renderFrames++;
                if(renderFrames>=3&&System.nanoTime()-captureAt>300_000_000L){
                    savePresentedFrame("cognition-relationships.png");capture=5;
                }
            }else if(capture==5){
                if(imageWrite!=null&&!imageWrite.isDone())return;
                checkImageWrite();evidence.put("nativeRelationshipJournalOpened",true);
                picturesDone=true;capture=6;
            }
        }catch(Throwable failed){frameFailure=failed.getClass().getSimpleName()+": "+failed.getMessage();}
    }
    /** Server thread: assertions and native fixture setup are bounded to this one QA world. */
    public boolean tick(ServerClient server,Client client,long nanos){
        require(allowed(),"Cognition probe refuses a non-QA profile or missing explicit cognition flag");
        disableModels();
        if(began==0)began=nanos;
        require(nanos-began<LIMIT,"Cognition native probe timed out in phase "+phase+"; evidence="+evidence);
        require(frameFailure==null,"Cognition render/UI probe failed: "+frameFailure);
        require(!AiIntentService.isConfigured(),"Cognition probe requires offline provider; paid requests are forbidden");
        if(server==null||client==null||client.isDisconnected()||!server.hasSpawned()||server.playerMob==null||server.getLevel()==null)return false;
        if(phase==0){
            WorldDefinition definition=WorldDefinition.forLevel(server.getLevel());
            require(definition!=null&&!server.getLevel().isCave,"Cognition probe requires a generated surface camp");
            data=(CompanionWorldData)server.getServer().world.worldEntity.getWorldData(CompanionSystem.WORLD_KEY);
            if(data==null||CompanionSystem.snapshot(client).companions().stream().filter(v->v.loaded()&&v.recruited()).count()<2)return false;
            try{prepare(server,definition);}catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot inspect native companion cognition wiring",error);}
            phase=1;return false;
        }
        require(actor!=null&&!actor.removed()&&actor.isSamePlace(server.playerMob),"Native social fixture actor was removed or left the test map");
        moved=Math.max(moved,Math.hypot(actor.x-startX,actor.y-startY));
        Map<String,Object> counters=social.counters();evidence.put("socialCounters",counters);evidence.put("nativeActorDisplacementPixels",moved);
        evidence.put("nativeActorTile",List.of(actor.getTileX(),actor.getTileY()));
        if(phase==1&&number(counters,"nativeApproaches")>initialApproaches&&number(counters,"worldUtterances")>initialUtterances){
            require(moved>24,"Social counters advanced without actual native NPC movement");
            require(actor.getDistance(server.playerMob)<120,"Social speaker never reached the player");
            var view=CompanionSystem.snapshot(client).companions().stream().filter(v->v.id().equals(actorId)).findFirst().orElseThrow();
            if(view.mind().relationships().stream().noneMatch(r->r.peerId().equals("player")))return false;
            evidence.put("nativeApproachAndSpeechObserved",true);evidence.put("nativeActorEnd",List.of(actor.x,actor.y));
            capture=1;phase=2;
        }
        if(phase==2&&picturesDone){
            evidence.put("elapsedSeconds",(nanos-began)/1e9);evidence.put("passed",true);return true;
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    private void prepare(ServerClient server,WorldDefinition definition) throws ReflectiveOperationException {
        Level level=server.getLevel();long now=level.getWorldEntity().getWorldTime();
        require(Math.hypot(server.playerMob.getTileX()-definition.campX,server.playerMob.getTileY()-definition.campY)<16,
            "Player is not inside the original camp; cognition probe will not move them across the map");
        Map<String,Object> records=(Map<String,Object>)field(CompanionWorldData.class,"companions").get(data);
        List<Object> present=records.values().stream().filter(p->{
            try{Object mob=field(p.getClass(),"mob").get(p);return mob instanceof HumanMob human&&!human.removed()&&human.isSamePlace(server.playerMob)
                &&field(p.getClass(),"recruited").getBoolean(p)&&!human.isDowned();}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }).sorted(Comparator.comparing(p->{try{return (String)field(p.getClass(),"id").get(p);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}})).limit(2).toList();
        require(present.size()==2,"Two healthy, recruited native camp NPCs are not ready");
        social=(NpcSocialRuntime)field(CompanionWorldData.class,"social").get(data);social.clear();
        initialApproaches=number(social.counters(),"nativeApproaches");initialUtterances=number(social.counters(),"worldUtterances");
        evidence.put("socialCounterBaseline",social.counters());
        Object chosen=present.get(0);actor=(HumanMob)field(chosen.getClass(),"mob").get(chosen);
        actorId=(String)field(chosen.getClass(),"id").get(chosen);actorMob=actor.getUniqueID();
        List<Point> walkable=campWalkable(level,new Point(server.playerMob.getTileX(),server.playerMob.getTileY()));
        Point far=walkable.stream().filter(p->{double d=p.distance(server.playerMob.getTileX(),server.playerMob.getTileY());return d>=4&&d<=6;})
            .max(Comparator.comparingDouble(p->p.distance(server.playerMob.getTileX(),server.playerMob.getTileY()))).orElseThrow(()->new IllegalStateException("No connected walkable camp position 4–6 tiles from the player"));
        Point second=walkable.stream().filter(p->p.distance(far)>=3&&p.distance(server.playerMob.getTileX(),server.playerMob.getTileY())>=2)
            .min(Comparator.comparingDouble(p->p.distance(server.playerMob.getTileX(),server.playerMob.getTileY()))).orElseThrow();
        for(int i=0;i<present.size();i++){
            Object record=present.get(i);HumanMob human=(HumanMob)field(record.getClass(),"mob").get(record);
            require(human.canBeCommanded(server,true),"QA native NPC cannot accept the existing command/path adapter");
            field(record.getClass(),"task").set(record,null);
            NativeWorkAdapter.clearCommands(human,null);NativeWorkAdapter.cancel(human);
            // Suppress automatic job re-acquisition only in the disposable QA profile.
            human.getJobTypeHandler().globalCooldown=human.getTime()+45_000;
            Point at=i==0?far:second;human.setPos(at.x*32+16,at.y*32+16,true);human.sendMovementPacket(true);
        }
        startX=actor.x;startY=actor.y;
        evidence.put("nativeActorStart",List.of(startX,startY));evidence.put("connectedCampWalkableTiles",walkable.size());
        evidence.put("playerNotTeleported",true);evidence.put("nativeNpcFixturePlacement",List.of(List.of(far.x,far.y),List.of(second.x,second.y)));
        NpcMind mind=(NpcMind)field(chosen.getClass(),"mind").get(chosen);require(mind!=null,"Native companion mind was not initialized");
        var before=mind.snapshot().traits();
        List<CompanionSystem.DialogueTurn> turns=(List<CompanionSystem.DialogueTurn>)field(chosen.getClass(),"recentConversation").get(chosen);
        String first="请叫我蓝莓。我最喜欢蓝莓。我们约好下次一起去遗迹。";
        mind.observeDialogue(now,first,"好的，蓝莓。下次出发前先确认准备。QA 测试对话。");
        turns.add(new CompanionSystem.DialogueTurn(now,first,"好的，蓝莓。下次出发前先确认准备。QA 测试对话。"));
        for(int i=0;i<12;i++){
            String question="QA 杂项对话第"+i+"轮，路边有一块石头。";String reply="我们继续看看周围。QA 测试对话。";
            mind.observeDialogue(now+i+1,question,reply);turns.add(new CompanionSystem.DialogueTurn(now+i+1,question,reply));
            while(turns.size()>8)turns.remove(0);
        }
        String receipt="qa-cognition-native-social-receipt";
        String receiptText="QA fixture：原生社交接线测试的任务完成回执；这是隔离测试数据，不代表玩家实际采集了物品。";
        List<CompanionSystem.MemoryEvent> memories=(List<CompanionSystem.MemoryEvent>)field(chosen.getClass(),"memories").get(chosen);
        memories.add(new CompanionSystem.MemoryEvent(receipt,now+20,"task_completed",receiptText,"qa.fixture.native_receipt"));
        mind.observeEvent(receipt,now+20,"task_completed",receiptText,"qa.fixture.native_receipt");
        require(mind.snapshot().traits().trust()>before.trust(),"Native fixture receipt did not affect persisted personality traits");
        SaveData saved=new SaveData("cognitionWorldData");data.addSaveData(saved);
        CompanionWorldData restored=new CompanionWorldData();restored.applyLoadData(new LoadData(saved.getScript()));
        var back=restored.view.companions().stream().filter(p->p.id().equals(actorId)).findFirst().orElseThrow();
        String recalled=NpcMind.context(back.mind(),"你记得我叫什么、喜欢什么吗？",now+1000);
        require(recalled.contains("蓝莓"),"Native WorldData save/parse/load lost the old name/preference");
        require(back.recentConversation().size()==8&&back.recentConversation().stream().noneMatch(t->t.playerText().contains("蓝莓")),
            "The old preference was not actually beyond the latest-eight-turn transcript");
        require(back.mind().traits().equals(mind.snapshot().traits()),"Native WorldData round-trip changed learned traits");
        var cautious=NpcBehaviorPolicy.evaluate("FOLLOW",new NpcBehaviorPolicy.Traits(.5,.95,.5,.05),new NpcBehaviorPolicy.Situation(.38,false,false,true,true,true,true));
        var rescue=NpcBehaviorPolicy.evaluate("HARVEST",CompanionSystem.behaviorTraits(back.mind()),new NpcBehaviorPolicy.Situation(.1,true,false,false,false,false,true));
        var home=NpcBehaviorPolicy.evaluate("HOME",CompanionSystem.behaviorTraits(back.mind()),new NpcBehaviorPolicy.Situation(.1,true,false,true,true,true,true));
        require(!cautious.allowed()&&rescue.choice()==NpcBehaviorPolicy.Choice.REQUEST_RESCUE&&home.allowed(),"Risk policy lost refusal/rescue/safe-return behavior");
        evidence.put("longMemoryBeyondRecentEightTurns",true);evidence.put("nativeWorldDataSaveParseLoad",true);
        evidence.put("savedRecentDialoguePairs",back.recentConversation().size());evidence.put("savedLongMemoryCount",back.mind().memories().size());
        evidence.put("traitsBefore",traits(before));evidence.put("traitsAfter",traits(back.mind().traits()));
        evidence.put("riskPolicy",Map.of("cautiousDeclinesHazard",true,"severelyHurtRequestsRescue",true,"homeRemainsAllowed",true));
        evidence.put("socialFixtureReceipt",Map.of("id",receipt,"source","qa.fixture.native_receipt","text",receiptText));
        // Publish actual fixture identity/state before waiting for production social ticks.
        Method publish=CompanionWorldData.class.getDeclaredMethod("publish");publish.setAccessible(true);publish.invoke(data);
    }
    private static List<Point> campWalkable(Level level,Point center){
        require(walkable(level,center.x,center.y),"Player's original camp tile is not walkable");
        List<Point> result=new ArrayList<>();ArrayDeque<Point> queue=new ArrayDeque<>();Set<Point> seen=new HashSet<>();
        queue.add(center);seen.add(center);
        while(!queue.isEmpty()){
            Point p=queue.removeFirst();result.add(p);
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){Point next=new Point(p.x+d[0],p.y+d[1]);
                if(Math.abs(next.x-center.x)>8||Math.abs(next.y-center.y)>8||!seen.add(next)||!walkable(level,next.x,next.y))continue;queue.addLast(next);}
        }return result;
    }
    private static boolean walkable(Level level,int x,int y){return x>0&&y>0&&x<level.tileWidth-1&&y<level.tileHeight-1&&!level.isLiquidTile(x,y)&&!level.isSolidTile(x,y);}
    private static Map<String,Double> traits(NpcMind.Traits t){return Map.of("trust",t.trust(),"caution",t.caution(),"sociability",t.sociability());}
    private static long number(Map<String,Object> values,String key){Object value=values.get(key);return value instanceof Number n?n.longValue():0;}
    private static Field field(Class<?> owner,String name) throws ReflectiveOperationException {Field f=owner.getDeclaredField(name);f.setAccessible(true);return f;}
    private static int[] bubbleCounts(int mobId) throws ReflectiveOperationException {
        Object session=field(WorldDialogueBubbles.class,"current").get(null);int players=0,npcs=0;
        if(session!=null)for(Object bubble:(List<?>)field(session.getClass(),"speakers").get(session)){
            boolean player=field(bubble.getClass(),"player").getBoolean(bubble);Mob mob=(Mob)field(bubble.getClass(),"mob").get(bubble);
            if(player)players++;else if(mob.getUniqueID()==mobId)npcs++;
        }return new int[]{players,npcs};
    }
    private void savePresentedFrame(String filename) throws Exception {
        String output=System.getProperty("mosslume.refactorProbe","");require(!output.isBlank(),"No cognition evidence output directory");
        Path directory=Path.of(output).toAbsolutePath().normalize();Files.createDirectories(directory);Path file=directory.resolve(filename);
        long window=GLFW.glfwGetCurrentContext();require(window!=0,"No native GL context for cognition screenshot");
        int[] width={0},height={0};GLFW.glfwGetFramebufferSize(window,width,height);
        int w=width[0],h=height[0];require(w>0&&h>0&&w<=6000&&h<=4000,"Invalid native cognition framebuffer dimensions "+w+"x"+h);
        ByteBuffer pixels=BufferUtils.createByteBuffer(w*h*4);int framebuffer=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,0);int previous=GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try{GL11.glReadBuffer(GL11.GL_FRONT);GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);}
        finally{GL11.glReadBuffer(previous);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer);}
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);int[] rgb=((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=(x+y*w)*4;rgb[x+(h-1-y)*w]=(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|(pixels.get(i+2)&255);}
        evidence.put(filename,Map.of("file",file.toString(),"width",w,"height",h,"readBuffer","GL_FRONT"));
        imageWrite=CompletableFuture.runAsync(()->{try{if(!ImageIO.write(image,"png",file.toFile()))throw new IllegalStateException("No PNG writer");}catch(Exception error){throw new java.util.concurrent.CompletionException(error);}});
    }
    private void checkImageWrite(){if(imageWrite!=null)imageWrite.join();}
    private static void require(boolean condition,String error){if(!condition)throw new IllegalStateException(error);}
}
