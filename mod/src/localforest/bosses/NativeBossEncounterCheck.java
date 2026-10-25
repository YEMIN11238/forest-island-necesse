package localforest.bosses;

import java.nio.file.Path;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.geom.Line2D;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import localforest.world.WorldDefinition;
import necesse.engine.GlobalData;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.level.maps.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Explicit disposable-profile test of real native attacks, state sync, draws, save and drops. */
public final class NativeBossEncounterCheck {
    private enum Stage { READY, FIRST, SECOND, HALF, ENRAGED, DEATH }
    private record View(String level,int left,int top,int right,int bottom,int playerX,int playerY,boolean arenaLoaded){}
    private record Plan(int epoch,int mobId,String id,Stage stage,long started){}
    private record Seen(int epoch,boolean present,int frames,Map<String,Object> draws){}
    private volatile View view;
    private volatile Plan plan;
    private volatile Seen seen;
    private volatile String clientError;
    private int clientEpoch=-1,clientFrames,index,epoch;
    private long started,stageStarted;
    private boolean done;
    private Level level;
    private ServerClient serverClient;
    private Point home;
    private volatile Point arenaBoss,arenaPlayer;
    private boolean arenaWaiting;
    private MosslumeBossMob actor;
    private Mob target;
    private int targetHealthAtStage;
    private long hitsAtStage,shotsAtStage;
    private Map<String,Object> current;
    private final Map<String,Object> result=new LinkedHashMap<>();
    private final List<Map<String,Object>> encounters=new ArrayList<>();
    private final Set<String> captured=new HashSet<>();
    private final Map<String,Object> captures=new ConcurrentHashMap<>();
    private final Map<String,BufferedImage> bufferedImages=new ConcurrentHashMap<>();
    private CompletableFuture<Void> imageWrite;
    private long writeStarted;
    public static boolean enabled(){return Boolean.getBoolean("mosslume.refactorProbe.bossEncounters");}
    private static boolean safe(){Path name=Path.of(GlobalData.appDataPath()).getFileName();return enabled()&&name!=null&&name.toString().startsWith("Mosslume-QA-");}
    public void frame(MainGame game){
        if(!safe())return;
        try{
            var client=game.getClient();if(client==null||client.isDisconnected()||client.getPlayer()==null)return;
            Level visible=client.getPlayer().getLevel();if(visible==null)return;var camera=game.getCamera();
            boolean loaded=true;Point arena=arenaBoss;
            if(arena!=null)for(int y=-1;y<=1&&loaded;y++)for(int x=-1;x<=4;x++)if(!visible.regionManager.isTileLoaded(Math.floorDiv(arena.x,32)+x,Math.floorDiv(arena.y,32)+y)){loaded=false;break;}
            view=new View(visible.getIdentifier().toString(),camera.getX(),camera.getY(),camera.getX()+camera.getWidth(),camera.getY()+camera.getHeight(),client.getPlayer().getX(),client.getPlayer().getY(),loaded);
            Plan p=plan;if(p==null)return;
            if(clientEpoch!=p.epoch()){clientEpoch=p.epoch();clientFrames=0;}
            Mob mob=GameUtils.getLevelMob(p.mobId(),visible,false);boolean present=mob instanceof MosslumeBossMob&&!mob.removed();
            if(present)clientFrames++;
            if(present&&clientFrames>=3){
                int pose=((MosslumeBossMob)mob).spriteColumn();
                SourceActionClip source=((MosslumeBossMob)mob).activeSourceClip();boolean actualStrike=pose==4&&source!=null&&((MosslumeBossMob)mob).sourceFrame().column()==source.damageFrame();
                if(p.stage()==Stage.READY&&pose==0||p.stage()==Stage.FIRST&&(pose==3||actualStrike)||p.stage()==Stage.SECOND&&(pose==3||actualStrike)||p.stage()==Stage.ENRAGED&&actualStrike)
                    captureFrame(p.id()+"-"+p.stage().name().toLowerCase()+"-"+pose+".png");
            }else if(!present&&p.stage()==Stage.DEATH&&System.nanoTime()-p.started()>250_000_000L)captureFrame(p.id()+"-death.png");
            seen=new Seen(p.epoch(),present,clientFrames,BossSprites.diagnostics());
        }catch(Throwable failure){clientError=failure.toString();}
    }
    public boolean tick(ServerClient client,long now){
        require(safe(),"Boss fixtures require explicit flag and disposable QA profile");if(done)return true;
        try{
            if(imageWrite!=null){
                require(now-writeStarted<35_000_000_000L,"Deferred Boss evidence encoding timed out");
                if(!imageWrite.isDone())return false;imageWrite.join();done=true;result.put("passed",true);result.put("elapsedSeconds",(now-started)/1e9);return true;
            }
            if(started==0){if(view==null)return false;started=now;stageStarted=now;prepareArena(client);return false;}
            require(clientError==null,"Client Boss check failed: "+clientError);
            require(now-started<75_000_000_000L,"Boss fixture total timeout: "+evidence());
            if(arenaWaiting){
                require(now-stageStarted<20_000_000_000L,"Ordinary native client arena loading timed out");
                View v=view;if(v==null||!v.level().equals(level.getIdentifier().toString())||Math.hypot(v.playerX()-arenaPlayer.x,v.playerY()-arenaPlayer.y)>4||!v.arenaLoaded())return false;
                arenaWaiting=false;result.put("nativeArenaTeleportObserved",true);result.put("nativeArenaRegionLoaded",true);spawn(client,now);return false;
            }
            require(now-stageStarted<9_000_000_000L,"Boss fixture stage timeout: "+evidence());
            Plan p=plan;Seen s=seen;if(p==null||s==null||s.epoch()!=p.epoch())return false;
            switch(p.stage()){
                case READY->{
                    if(!s.present()||s.frames()<3)return false;
                    require(actor.isBoss()&&actor.shouldSave(),"Actor lost native boss registry/save identity");
                    current.put("nativeBossAndSave",true);advance(Stage.FIRST,now);require(actor.beginAttack(0,target),"First attack rejected");
                }
                case FIRST->{
                    if(actor.attackPhase()!=BossAttackTimeline.Phase.READY||actor.ownAttacks<1)return false;
                    require(actor.ownAreaHits>hitsAtStage,"First area attack missed native target: "+actor.combatState());
                    current.put("firstAttack",Map.of("hits",actor.ownAreaHits-hitsAtStage,"targetHealthBefore",targetHealthAtStage,"targetHealthAfter",target.getHealth()));
                    resetPositions();advance(Stage.SECOND,now);require(actor.beginAttack(1,target),"Second attack rejected");
                }
                case SECOND->{
                    if(actor.attackPhase()!=BossAttackTimeline.Phase.READY||actor.ownAttacks<2||now-stageStarted<2_000_000_000L)return false;
                    if(actor.isStone()){
                        require(actor.ownProjectiles-shotsAtStage==5,"Stone fan did not create five native projectiles");
                        require(target.getHealth()<targetHealthAtStage,"Native source talisman projectile did not damage its real target");
                    }
                    else require(actor.ownAreaHits>hitsAtStage,"Locked dash failed native contact hit");
                    current.put("secondAttack",Map.of("hits",actor.ownAreaHits-hitsAtStage,"projectiles",actor.ownProjectiles-shotsAtStage,"targetHealthBefore",targetHealthAtStage,"targetHealthAfter",target.getHealth()));
                    resetPositions();advance(Stage.HALF,now);actor.isServerHit(new GameDamage(actor.getMaxHealth()*.65f),0,0,0,target);
                }
                case HALF->{
                    if(!actor.secondPhase())return false;
                    require(actor.getHealth()>0&&actor.getHealth()*2<=actor.getMaxHealth()&&actor.ownTransitions==1,"Half-health transition wrong");
                    SaveData snapshot=new SaveData("BOSS");actor.addSaveData(snapshot);MosslumeBossMob restored=(MosslumeBossMob)MobRegistry.getMob(actor.definition.id(),level);restored.applyLoadData(snapshot.toLoadData());
                    require(restored.getHealth()==actor.getHealth()&&restored.secondPhase()&&restored.shouldSave(),"Native Boss save payload lost health or phase");
                    current.put("nativeSaveRoundTrip",Map.of("health",restored.getHealth(),"phaseTwo",restored.secondPhase(),"sameId",restored.getUniqueID()==actor.getUniqueID()));
                    advance(Stage.ENRAGED,now);require(actor.beginAttack(0,target),"Phase two attack rejected");
                }
                case ENRAGED->{
                    if(actor.attackPhase()!=BossAttackTimeline.Phase.READY||actor.ownAttacks<3)return false;
                    require(actor.ownAreaHits>hitsAtStage&&actor.ownTransitions==1,"Phase-two hit or exactly-once transition failed");
                    current.put("enragedAttack",Map.of("hits",actor.ownAreaHits-hitsAtStage,"projectiles",actor.ownProjectiles-shotsAtStage));
                    current.put("clientDraws",s.draws());advance(Stage.DEATH,now);actor.isServerHit(new GameDamage(100000),0,0,0,target);
                }
                case DEATH->{
                    if(s.present()||now-stageStarted<1_200_000_000L)return false;
                    require(actor.removed()||actor.hasDied(),"Lethal native damage did not remove boss");
                    require(actor.itemsDropped.size()>=2,"Native boss did not produce material and coin loot");
                    current.put("nativeDrops",actor.itemsDropped.stream().map(drop->drop.item.getItemDisplayName()).toList());
                    current.put("clientAfterDeath",s.draws());encounters.add(current);clearFixtures();index++;
                    if(index<MosslumeBosses.ALL.size())spawn(client,now);else{restoreHome();writeStarted=now;imageWrite=CompletableFuture.runAsync(this::writeCapturedFrames);result.put("pngEncoding","Deferred until all combat fixtures finish; one serial worker, no concurrent PNG encoders during attacks.");result.put("scope","Two disposable native Boss fixtures on existing generated wilderness ground: ordinary native teleport/region loading, both attacks, real native hits/projectiles, one half-health transition, SaveData round trip, client draw counters, GL_FRONT presented-frame PNGs and native drops. Fixtures use frozen native cow targets. This is not an autonomous full fight, game difficulty balance assessment, physical-input test or world save/reload across separate processes.");}
                }
            }
            return done;
        }catch(RuntimeException failure){result.put("passed",false);result.put("failure",failure.toString());result.put("failureState",actor==null?Map.of():actor.combatState());if(target!=null)result.put("targetAtFailure",Map.of("position",List.of(target.x,target.y),"health",target.getHealth(),"lastHitAge",target.getTimeSinceLastHit(),"knockbackModifier",target.getKnockbackModifier(),"nativeCanBeHit",actor!=null&&target.canBeHit(actor),"nativeCanBeTargeted",actor!=null&&target.canBeTargeted(actor,actor.getPvPOwner())));cleanup();throw failure;}
    }
    private void prepareArena(ServerClient client){
        serverClient=client;level=client.getLevel();home=new Point(client.playerMob.getX(),client.playerMob.getY());
        WorldDefinition definition=WorldDefinition.forLevel(level);require(definition!=null&&definition.isContinuous(),"Boss arena selection requires native continuous world");
        int[][] regions={{0,-64},{64,-64},{-64,-64},{64,64},{-64,64},{128,-128}};
        for(int[] offset:regions){
            int centerX=definition.campX+offset[0],centerY=definition.campY+offset[1];
            level.regionManager.ensureTilesAreLoaded(centerX-12,centerY-12,centerX+12,centerY+12);
            for(int y=centerY-8;y<=centerY+8;y++)for(int x=centerX-7;x<=centerX+7;x++){
                boolean clear=true;
                for(int oy=-1;oy<=1&&clear;oy++)for(int ox=-1;ox<=4;ox++)if(!clear(x+ox,y+oy)){clear=false;break;}
                if(!clear||!clear(x-3,y-4))continue;
                int px=x*32+16,py=y*32+16,playerX=(x-3)*32+16,playerY=(y-4)*32+16;
                if(client.playerMob.collidesWith(level,playerX,playerY))continue;
                if(level.entityManager.streamMobsAndPlayers(px,py,180).anyMatch(m->!m.removed()))continue;
                arenaBoss=new Point(px,py);arenaPlayer=new Point(playerX,playerY);
                client.playerMob.stopMoving();client.changeLevel(level.getIdentifier(),ignored->new Point(arenaPlayer),true);arenaWaiting=true;
                result.put("arena",Map.of("bossTile",List.of(x,y),"playerTile",List.of(x-3,y-4),"existingNativeGround",true,"terrainOrObjectInjection",false));return;
            }
        }
        throw new IllegalStateException("No existing generated open ground for Boss QA after six bounded wilderness searches");
    }
    private boolean clear(int x,int y){return level.regionManager.isTileLoaded(x,y)&&!level.isLiquidTile(x,y)&&!level.isSolidTile(x,y);}
    private void spawn(ServerClient client,long now){
        level=client.getLevel();View v=view;require(level!=null&&v!=null&&v.level().equals(level.getIdentifier().toString()),"Boss viewport/level mismatch");
        actor=(MosslumeBossMob)MobRegistry.getMob(MosslumeBosses.ALL.get(index).id(),level);target=MobRegistry.getMob("cow",level);
        require(arenaBoss!=null,"Native wilderness arena missing");int px=arenaBoss.x,py=arenaBoss.y;
        require(px>=v.left()+96&&px<v.right()-112&&py>=v.top()+160&&py<v.bottom()-36,"Native Boss arena is outside actual camera: "+v);
        require(!actor.collidesWith(level,px,py)&&!target.collidesWith(level,px+96,py)&&!level.collides(new Line2D.Float(px,py,px+96,py),actor.getLevelCollisionFilter()),"Existing arena ground collides with actual bodies");
        level.entityManager.addMob(actor,px,py);level.entityManager.addMob(target,px+96,py);actor.setSpawnTilePosition(Math.floorDiv(px,32),Math.floorDiv(py,32));
        actor.ai=new BehaviourTreeAI<>(actor,new SelectorAINode<>());target.ai=new BehaviourTreeAI<>(target,new SelectorAINode<>());actor.isHostile=false;actor.stopMoving();target.stopMoving();target.setMaxHealth(1000);target.setHealthHidden(1000);target.setKnockbackModifier(0);target.canDespawn=false;
        current=new LinkedHashMap<>();current.put("id",actor.definition.id());current.put("position",List.of(px,py));current.put("mobId",actor.getUniqueID());advance(Stage.READY,now);
    }
    private void resetPositions(){actor.stopMoving();target.stopMoving();target.setPos(actor.x+96,actor.y,true);}
    private void advance(Stage stage,long now){stageStarted=now;hitsAtStage=actor.ownAreaHits;shotsAtStage=actor.ownProjectiles;targetHealthAtStage=target.getHealth();plan=new Plan(++epoch,actor.getUniqueID(),actor.definition.id(),stage,now);}
    public Map<String,Object> evidence(){Map<String,Object> out=new LinkedHashMap<>(result);out.put("captures",new LinkedHashMap<>(captures));out.put("encounters",new ArrayList<>(encounters));if(plan!=null)out.put("stage",plan.stage().name());if(actor!=null)out.put("current",actor.combatState());if(current!=null)out.put("fixture",new LinkedHashMap<>(current));return out;}
    private void clearFixtures(){plan=null;if(actor!=null){for(var drop:actor.itemsDropped)if(!drop.removed())drop.remove();if(!actor.removed())actor.remove();}if(target!=null&&!target.removed())target.remove();actor=null;target=null;}
    private void restoreHome(){if(serverClient==null||home==null)return;try{serverClient.changeLevel(serverClient.getLevelIdentifier(),ignored->new Point(home),true);result.put("returnedToCamp",true);}catch(RuntimeException failure){result.put("returnToCampError",failure.toString());}}
    public void cleanup(){clearFixtures();restoreHome();if(imageWrite!=null)imageWrite.join();else writeCapturedFrames();}
    private void captureFrame(String name)throws Exception{
        if(!captured.add(name))return;
        Path directory=Path.of(System.getProperty("mosslume.refactorProbe"));Files.createDirectories(directory);Path file=directory.resolve(name);
        long window=GLFW.glfwGetCurrentContext();require(window!=0,"Native Boss capture requires its GL context");
        int[] width={0},height={0};GLFW.glfwGetFramebufferSize(window,width,height);int w=width[0],h=height[0];require(w>0&&h>0&&w<=6000&&h<=4000,"Invalid Boss capture framebuffer");
        ByteBuffer pixels=BufferUtils.createByteBuffer(w*h*4);int framebuffer=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,0);int previous=GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try{GL11.glReadBuffer(GL11.GL_FRONT);GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,pixels);}finally{GL11.glReadBuffer(previous);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer);}
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);int[] rgb=((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=(x+y*w)*4;rgb[x+(h-1-y)*w]=(pixels.get(i)&255)<<16|(pixels.get(i+1)&255)<<8|(pixels.get(i+2)&255);}
        captures.put(name,Map.of("file",file.toString(),"width",w,"height",h,"readBuffer","GL_FRONT","scope","Previously presented native framebuffer at observed Boss phase; not an editor mockup."));
        bufferedImages.put(name,image);
    }
    private void writeCapturedFrames(){
        Path directory=Path.of(System.getProperty("mosslume.refactorProbe"));
        for(var entry:new java.util.TreeMap<>(bufferedImages).entrySet()){
            try{
                Path file=directory.resolve(entry.getKey());require(ImageIO.write(entry.getValue(),"png",file.toFile()),"No Boss PNG writer");
                Map<String,Object> metadata=new LinkedHashMap<>((Map<String,Object>)captures.get(entry.getKey()));metadata.put("fileComplete",true);captures.put(entry.getKey(),metadata);bufferedImages.remove(entry.getKey());
            }catch(Exception error){throw new java.util.concurrent.CompletionException(error);}
        }
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
