package localforest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import necesse.engine.GlobalData;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;
import localforest.visual.CharacterAnimation;
import localforest.visual.CompanionSprites;

/** Explicit disposable-world rendering fixture. Never enabled by normal play or attackPoses. */
public final class WeaponMatrixProbe {
    private static final String[] ITEMS={"woodaxe","woodsword","woodbow"};
    private static final String[] TYPES={"farmerhuman","blacksmithhuman","traderhuman","guardhuman","alchemisthuman"};
    private static final String[] DIRS={"up","right","down","left"};
    private static final int[] DX={0,128,0,-128},DY={-128,0,128,0};
    private static final List<HumanMob> serverActors=new ArrayList<>();
    private static volatile List<Fixture> fixtures=List.of();
    private static volatile boolean ready;
    private static volatile String failure;
    private static volatile int requestedStep=-1;
    private static int serverStep=-1;
    private static final boolean[] accepted=new boolean[12];
    private static final Set<String> playerObserved=new LinkedHashSet<>(),npcObserved=new LinkedHashSet<>(),stills=new LinkedHashSet<>();
    private static int attempts;
    private static long portraitHudFrames;
    private static long portraitHudNanos,portraitHudMaximumNanos,portraitCommandNanos,portraitChatterNanos,portraitMapNanos;
    private static long portraitShaderMismatches;
    private static final Map<String,Object> shotAudits=new LinkedHashMap<>();
    private static final List<Map<String,Object>> serverEvents=java.util.Collections.synchronizedList(new ArrayList<>());
    record Fixture(int id,String type,String role,int x,int y){}
    record Shot(String name,int x,int y,String actor,int nativeBodyFrame,float progress){}
    static boolean enabled(){return Boolean.getBoolean("mosslume.visualProbe.weaponMatrix");}
    private static void requireDisposable(){
        if(!Boolean.getBoolean("mosslume.visualProbe.sceneOnly")||!Path.of(GlobalData.appDataPath()).getFileName().toString().startsWith("Mosslume-QA-"))
            throw new IllegalStateException("weaponMatrix requires sceneOnly and a disposable Mosslume-QA profile");
    }
    static void serverTick(Server server){
        if(!enabled()||failure!=null)return;
        try{
            requireDisposable();
            var owner=server.streamClients().filter(c->c.hasSpawned()&&c.playerMob!=null).findFirst().orElse(null);
            if(owner==null)return;Level level=owner.getLevel();
            if(!ready){
                for(int i=0;i<ITEMS.length;i++)owner.playerMob.getInv().main.setItem(i,new InventoryItem(ITEMS[i]));
                owner.playerMob.getInv().main.setItem(9,new InventoryItem("stonearrow",999));
                List<Fixture> created=new ArrayList<>();int[][] desired={{-96,-64},{-32,-64},{32,-64},{96,-64},{-96,64}};
                for(int i=0;i<TYPES.length;i++){
                    HumanMob mob=(HumanMob)MobRegistry.getMob(TYPES[i],level);int[] pos=findPosition(level,mob,owner.playerMob,desired[i][0],desired[i][1],created);
                    if(pos==null)throw new IllegalStateException("No dry unobstructed QA pose position for "+TYPES[i]);
                    mob.canDespawn=false;mob.shouldSave=false;mob.onSpawned(pos[0],pos[1]);level.entityManager.addMob(mob,pos[0],pos[1]);
                    mob.commandGuard(null,pos[0],pos[1]);
                    serverActors.add(mob);created.add(new Fixture(mob.getUniqueID(),TYPES[i],CompanionSprites.appearanceRole(TYPES[i]),pos[0],pos[1]));
                }
                fixtures=List.copyOf(created);ready=true;
                System.out.println("MOSSLUME_WEAPON_MATRIX_READY fixtures="+fixtures.size()+" onlyDisposableWorld=true");
            }
            int step=requestedStep;
            if(step>=0&&step<12&&step!=serverStep){
                serverStep=step;int direction=step%4;String item=ITEMS[step/4];
                for(HumanMob mob:serverActors){
                    if(mob.removed())continue;
                    // The real native ability sets attackingItem/time/direction and
                    // broadcasts to the ordinary client. This is animation QA,
                    // deliberately not a fabricated AI/combat-damage claim.
                    mob.attackItem(mob.getX()+DX[direction],mob.getY()+DY[direction],new InventoryItem(item),800,800);
                }
                serverEvents.add(Map.of("step",step,"item",item,"direction",DIRS[direction],"nativeNpcAbilityBroadcasts",serverActors.size()));
            }
        }catch(Exception error){failure=error.toString();System.err.println("MOSSLUME_WEAPON_MATRIX_FAILED "+error);error.printStackTrace();}
    }
    private static int[] findPosition(Level level,HumanMob mob,PlayerMob player,int dx,int dy,List<Fixture> occupied){
        int tx=Math.floorDiv(player.getX()+dx,32),ty=Math.floorDiv(player.getY()+dy,32);
        for(int radius=0;radius<=5;radius++)for(int oy=-radius;oy<=radius;oy++)for(int ox=-radius;ox<=radius;ox++){
            if(radius>0&&Math.abs(ox)!=radius&&Math.abs(oy)!=radius)continue;
            int x=(tx+ox)*32+16,y=(ty+oy)*32+16;
            if(!level.regionManager.isTileLoaded(tx+ox,ty+oy)||level.isLiquidTile(tx+ox,ty+oy)||mob.collidesWith(level,x,y)||Math.hypot(x-player.x,y-player.y)<48)continue;
            if(occupied.stream().anyMatch(p->Math.hypot(p.x()-x,p.y()-y)<48))continue;
            return new int[]{x,y};
        }
        return null;
    }
    static void clientTick(Client client,double elapsed){
        if(!enabled())return;requireDisposable();
        if(failure!=null)throw new IllegalStateException(failure);
        if(!ready)return;
        int step=(int)(elapsed/1.25);if(step>=12)return;
        requestedStep=step;PlayerMob player=client.getPlayer();int weapon=step/4,dir=step%4;
        InventoryItem selected=player.getInv().main.getItem(weapon);
        if(!accepted[step]&&elapsed-step*1.25<.65&&selected!=null&&selected.item.getStringID().equals(ITEMS[weapon])){
            player.setSelectedSlot(weapon);attempts++;
            if(player.tryAttack(player.getSelectedItemSlot(),player.getX()+DX[dir],player.getY()+DY[dir]))accepted[step]=true;
        }
        InventoryItem active=CharacterAnimation.activeItem(player);
        if(active!=null&&player.attackDir!=null)playerObserved.add(active.item.getStringID()+":"+CharacterAnimation.direction(player.attackDir.x,player.attackDir.y,player.getDir()));
        for(Fixture fixture:fixtures){
            Mob mob=client.getLevel().entityManager.mobs.stream().filter(m->m.getUniqueID()==fixture.id()).findFirst().orElse(null);
            if(mob instanceof HumanMob human&&human.isAttacking&&human.attackDir!=null){
                InventoryItem item=CharacterAnimation.activeItem(human);
                if(item!=null)npcObserved.add(fixture.role()+":"+item.item.getStringID()+":"+CharacterAnimation.direction(human.attackDir.x,human.attackDir.y,human.getDir()));
            }
        }
    }
    static List<Shot> shots(Client client){
        List<Shot> result=new ArrayList<>();PlayerMob player=client.getPlayer();InventoryItem item=CharacterAnimation.activeItem(player);
        if(item!=null&&player.attackDir!=null){
            float progress=player.getAttackAnimProgress();int phase=CharacterAnimation.frame(progress,6),dir=CharacterAnimation.direction(player.attackDir.x,player.attackDir.y,player.getDir());
            if(phase==2||phase==3){
                String name="weapon-player-"+item.item.getStringID()+"-"+DIRS[dir]+"-phase"+phase+".png";
                if(stills.add(name))result.add(new Shot(name,player.getX(),player.getY(),"player",CharacterAnimation.attackFrame(item,progress,6),progress));
            }
        }
        for(Fixture fixture:fixtures){
            if(result.size()>=2)break;
            String name="weapon-npc-"+fixture.role()+".png";if(stills.contains(name))continue;
            Mob mob=client.getLevel().entityManager.mobs.stream().filter(m->m.getUniqueID()==fixture.id()).findFirst().orElse(null);
            if(mob instanceof HumanMob human&&human.isAttacking){
                float progress=human.getAttackAnimProgress();InventoryItem active=CharacterAnimation.activeItem(human);
                if(active!=null&&progress>=.25f&&progress<.8f){stills.add(name);result.add(new Shot(name,human.getX(),human.getY(),fixture.role(),CharacterAnimation.attackFrame(active,progress,6),progress));}
            }
        }
        for(Shot shot:result)shotAudits.put(shot.name(),CompanionSprites.renderAudit());
        return result;
    }
    /** Actual native endpoints, shown only until the first framebuffer snapshot.
     * Not part of warmup/performance or the continuous attack capture. */
    public static void drawHud(necesse.engine.state.MainGame game,necesse.engine.gameLoop.tickManager.TickManager tick){
        if(!enabled()||!ready||failure!=null||!VisualSceneProbe.wantsPortraitSnapshot())return;requireDisposable();
        Client client=game.getClient();if(client.getPlayer()==null)return;
        long start=System.nanoTime();var previousShader=necesse.gfx.Renderer.getCurrentShader();
        try{
        int left=16,top=necesse.engine.window.WindowManager.getWindow().getHudHeight()-184;
        necesse.gfx.Renderer.initQuadDraw(360,168).color(0.04f,.055f,.065f,.94f).draw(left,top);
        var font=new necesse.gfx.gameFont.FontOptions(10).color(java.awt.Color.WHITE);
        necesse.gfx.gameFont.FontManager.bit.drawString(left+8,top+6,"QA current heads: command / chatter / map",font);
        for(int i=0;i<fixtures.size();i++){
            Fixture fixture=fixtures.get(i);Mob found=client.getLevel().entityManager.mobs.stream().filter(m->m.getUniqueID()==fixture.id()).findFirst().orElse(null);
            if(!(found instanceof HumanMob mob))continue;int x=left+28+i*62;
            necesse.gfx.gameFont.FontManager.bit.drawString(x,top+22,fixture.role(),font);
            long endpoint=System.nanoTime();
            necesse.level.maps.levelData.settlementData.settler.Settler.getHumanFaceDrawOptions(mob.getCommandFaceDrawOptions(),32,x,top+38).draw();
            long afterCommand=System.nanoTime();portraitCommandNanos+=afterCommand-endpoint;
            necesse.level.maps.levelData.settlementData.settler.Settler.getHumanFaceDrawOptions(mob.getChatterFaceDrawOptions(),32,x,top+78).draw();
            long afterChatter=System.nanoTime();portraitChatterNanos+=afterChatter-afterCommand;
            mob.drawOnMap(tick,client,x+16,top+132,1d,new java.awt.Rectangle(left,top,360,168),true);
            portraitMapNanos+=System.nanoTime()-afterChatter;
        }
        portraitHudFrames++;
        }finally{
            long elapsed=System.nanoTime()-start;portraitHudNanos+=elapsed;portraitHudMaximumNanos=Math.max(portraitHudMaximumNanos,elapsed);
            if(necesse.gfx.Renderer.getCurrentShader()!=previousShader)portraitShaderMismatches++;
        }
    }
    static Map<String,Object> report(){
        List<String> missing=new ArrayList<>();
        for(int step=0;step<12;step++)if(!accepted[step])missing.add("client tryAttack "+ITEMS[step/4]+" "+DIRS[step%4]);
        @SuppressWarnings("unchecked") Map<String,Object> bindings=(Map<String,Object>)CharacterAnimation.weaponCounters().get("bindings");
        for(String item:ITEMS)for(int dir=0;dir<4;dir++){
            if(!playerObserved.contains(item+":"+dir)||!bindings.containsKey("player:"+item+":"+dir))missing.add("player rendered "+item+" "+DIRS[dir]);
            for(Fixture fixture:fixtures)if(!npcObserved.contains(fixture.role()+":"+item+":"+dir)||!bindings.containsKey(fixture.role()+":"+item+":"+dir))missing.add("NPC rendered "+fixture.role()+" "+item+" "+DIRS[dir]);
        }
        if(!ready||fixtures.size()!=5)missing.add("five native NPC families");if(failure!=null)missing.add(failure);
        Map<String,Object> out=new LinkedHashMap<>();out.put("passed",missing.isEmpty());out.put("missing",missing);out.put("ready",ready);out.put("clientTryAttackAttempts",attempts);
        out.put("acceptedSteps",java.util.stream.IntStream.range(0,12).filter(i->accepted[i]).boxed().toList());out.put("playerObserved",List.copyOf(playerObserved));out.put("npcObserved",List.copyOf(npcObserved));
        out.put("fixtureActors",fixtures.stream().map(f->Map.of("id",f.id(),"type",f.type(),"role",f.role(),"x",f.x(),"y",f.y(),"shouldSave",false)).toList());
        synchronized(serverEvents){out.put("serverEvents",List.copyOf(serverEvents));}
        out.put("nativeWeaponDrawCounters",CharacterAnimation.weaponCounters());out.put("closeupImages",List.copyOf(stills));out.put("npcRenderAtCloseups",shotAudits);out.put("residentPortraits",localforest.visual.ResidentPortraits.counters());out.put("nativePortraitHudFrames",portraitHudFrames);
        out.put("nativePortraitHudTiming",Map.of("scope","Capture snapshot only; excluded from warmup and no-readback performance; System.nanoTime CPU submission timings, no GPU sync.","totalMs",portraitHudNanos/1e6,"maximumFrameMs",portraitHudMaximumNanos/1e6,"commandMs",portraitCommandNanos/1e6,"chatterMs",portraitChatterNanos/1e6,"mapMs",portraitMapNanos/1e6,"shaderIdentityMismatches",portraitShaderMismatches));
        out.put("scope","Disposable sceneOnly QA only. Player: actual native client tryAttack with server inventory/ammo. NPCs: native networked attackItem animation fixtures, not AI/combat-damage proof. No terrain edits, model calls or physical input. Stills read the previously presented framebuffer, so observed pose metadata may lead the image by one frame.");return out;
    }
    private WeaponMatrixProbe(){}
}
