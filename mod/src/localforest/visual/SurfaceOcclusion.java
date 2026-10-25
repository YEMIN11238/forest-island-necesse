package localforest.visual;

import java.nio.ByteBuffer;
import java.util.*;
import localforest.scene.RegionSceneQuery;
import localforest.scene.TileBounds;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Bounded, single-target GL2.1 receiver/occluder map. SurfaceMap/floodlight concepts from
 * SDV-Radiance MIT (65c153a9), adapted to native loaded tiles, not Stardew map labels.
 * This supplies height-aware cross-sprite lamp occlusion, not the upstream full MRT relief replay.
 */
public final class SurfaceOcclusion {
    private static final Map<Level,Long> versions=new WeakHashMap<>();
    private static Level current;
    private static TileBounds bounds;
    private static GameTexture texture;
    private static ByteBuffer upload;
    private static long sampledAt=Long.MIN_VALUE,version=Long.MIN_VALUE;
    private static boolean capabilitiesReported;
    public static long uploads;
    public static synchronized void invalidate(Level level){versions.put(level,versions.getOrDefault(level,0L)+1);}
    public static void bind(GameShader shader,Level level){
        if(level==null){shader.pass1i("occlusionEnabled",0);return;}
        TileBounds next=RegionSceneQuery.visibleLights(level);
        long time=System.nanoTime()/1_000_000,requestedVersion=versions.getOrDefault(level,0L);
        if(current!=level||bounds==null||!bounds.equals(next)||version!=requestedVersion||time-sampledAt>=120){
            prepare(level,next);current=level;bounds=next;sampledAt=time;version=requestedVersion;
        }
        shader.pass1i("occlusionEnabled",texture==null?0:1);
        if(texture!=null){
            shader.pass1i("occlusionMap",2);
            shader.pass4f("occlusionBounds",bounds.left*32,bounds.top*32,bounds.width()*32,bounds.height()*32);
            texture.bindTexture(GL13.GL_TEXTURE2);
        }
    }
    private static void prepare(Level level,TileBounds area){
        int width=area.width(),height=area.height();if(width<1||height<1)return;
        if(!capabilitiesReported){
            int drawBuffers=GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
            System.out.println("MOSSLUME_SURFACE_PIPELINE gl="+GL11.glGetString(GL11.GL_VERSION)+" drawBuffers="+drawBuffers+" normalBake=cpu receiverMap=single-rgba8 mrtRequired=false fullReliefReplay=false");
            capabilitiesReported=true;
        }
        if(texture==null||texture.getWidth()!=width||texture.getHeight()!=height){
            if(texture!=null)texture.delete();texture=new GameTexture("mosslume-surfaces",width,height);
            texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);upload=BufferUtils.createByteBuffer(width*height*4);
        }
        upload.clear();
        for(int y=area.top;y<area.bottom;y++)for(int x=area.left;x<area.right;x++){
            int h=0,kind=0,opacity=0;
            if(RegionSceneQuery.loaded(level,x,y)){
                String id=level.getObject(x,y).getStringID();
                if(id.contains("wall")&&!id.contains("lamp")||id.contains("door")&&!id.contains("open")){h=76;kind=2;opacity=255;}
                else if(id.endsWith("_tree")||id.contains("tree")&&!id.contains("sapling")){h=98;kind=4;opacity=165;}
                else if(id.contains("structure")){h=82;kind=3;opacity=235;}
                else if(RegionSceneQuery.liquid(level,x,y)){kind=1;}
            }
            upload.put((byte)Math.min(255,h)).put((byte)(kind*32)).put((byte)opacity).put((byte)255);
        }
        upload.flip();texture.bindTexture(GL13.GL_TEXTURE2);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,0,width,height,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,upload);uploads++;
    }
    private SurfaceOcclusion(){}
}
