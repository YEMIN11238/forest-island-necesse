package localforest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import necesse.engine.GameLaunch;
import necesse.engine.GlobalData;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.state.MainGame;
import necesse.engine.window.GameWindow;
import necesse.engine.network.client.Client;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;
import necesse.gfx.Renderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import necesse.engine.window.WindowManager;
import net.bytebuddy.asm.Advice;
import org.lwjgl.glfw.GLFW;
import localforest.world.IslandWorld;

public class IslandDiagnostics {
    private static long entered;
    private static int captures;
    private static boolean titled;
    private static boolean stopping;
    private static boolean styled;
    public static void tick(MainGame game) {
        if(!IslandMod.enabled())return;
        if(!titled){long win=GLFW.glfwGetCurrentContext();if(win!=0){GLFW.glfwSetWindowTitle(win,"林间小岛 · Necesse 内核 0.1");titled=true;}}
        Client client=game.getClient();PlayerMob player=client==null?null:client.getPlayer();
        if(player==null||player.getLevel()==null||!IslandWorld.isIsland(player.getLevel()))return;
        if(!styled){client.storyManager.hideStoryObjectives=true;necesse.gfx.forms.presets.sidebar.TrackedSidebarForm.updateTrackedList();styled=true;}
        if(!GameLaunch.launchOptions.containsKey("islandcapture"))return;
        long now=System.currentTimeMillis();if(entered==0)entered=now;
        if(captures>=3){
            if(!stopping&&now-entered>13000&&GameLaunch.launchOptions.containsKey("islandsmoke")){
                stopping=true;
                client.getLocalServer().stop(server->{System.out.println("LOCALFOREST_SMOKE_SAVED_AND_STOPPED");System.exit(0);});
            }
            return;
        }
        if(now-entered<3500+captures*3000)return;
        Level level=player.getLevel();int objects=0;
        for(int y=0;y<level.tileHeight;y++)for(int x=0;x<level.tileWidth;x++)if(level.getObjectID(x,y)!=0)objects++;
        try {
            Path path=Paths.get(GlobalData.appDataPath(),"evidence");Files.createDirectories(path);
            String json="{\"kernel\":\""+System.getProperty("localforest.kernel","unknown")+"\",\"mod\":\""+IslandMod.VERSION+"\",\"level\":\""+level.getIdentifier()+"\",\"width\":"+level.tileWidth+",\"height\":"+level.tileHeight+",\"objects\":"+objects+",\"playerX\":"+player.x+",\"playerY\":"+player.y+",\"capturedAtMillis\":"+now+"}";
            Files.write(path.resolve("state-"+captures+".json"),json.getBytes(StandardCharsets.UTF_8));
            captureFrame(captures);captures++;
            System.out.println("LOCALFOREST_CAPTURE "+json);
        }catch(Exception e){throw new RuntimeException("Could not capture owned island session",e);}
    }
    private static void captureFrame(int number) throws Exception {
        GameWindow window=WindowManager.getWindow();int w=window.getFrameWidth(),h=window.getFrameHeight();
        ByteBuffer bytes=BufferUtils.createByteBuffer(w*h*4);GL11.glReadBuffer(GL11.GL_FRONT);
        GL11.glReadPixels(0,0,w,h,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
        BufferedImage frame=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int i=(x+y*w)*4;int argb=0xff000000|((bytes.get(i)&255)<<16)|((bytes.get(i+1)&255)<<8)|(bytes.get(i+2)&255);
            frame.setRGB(x,h-1-y,argb);
        }
        Path dir=Paths.get(GlobalData.appDataPath(),"evidence");Files.createDirectories(dir);
        ImageIO.write(frame,"png",dir.resolve("frame-"+number+".png").toFile());
        System.out.println("LOCALFOREST_FRAME_CAPTURED "+w+"x"+h+" trees="+localforest.visual.IslandVisuals.treeDraws+" water="+localforest.visual.IslandVisuals.waterDraws+" flames="+localforest.visual.IslandVisuals.flameFramesSeen);
    }
    @ModMethodPatch(target=MainGame.class,name="frameTick",arguments={TickManager.class,GameWindow.class})
    public static class FrameProbe {
        @Advice.OnMethodExit public static void after(@Advice.This MainGame game) { IslandDiagnostics.tick(game); }
    }
}
