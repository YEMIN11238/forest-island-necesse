package localforest.visual;

import localforest.world.noise.FastNoiseLite;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelTileTerrainDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.Level;
import org.lwjgl.opengl.*;

/** GDQuest water_2D (05a3931a, MIT code) adapted to GLSL120 and world UVs.
 * Noise is generated from our existing MIT FastNoiseLite, with no upstream art.
 */
public final class OceanSurfaceRenderer {
    private static GameShader shader;
    private static GameTexture noise;
    private static boolean unavailable;
    public static long batches;
    public static boolean add(LevelTileTerrainDrawOptions out,Level level,GameTexture art,
            int tx,int ty,int dx,int dy,double ignoredCollectionTime){
        if(unavailable||Boolean.getBoolean("mosslume.ocean.legacy"))return false;
        SharedTextureDrawOptions batch=out.specialDrawables.getOrCreate("mosslume-ocean-flow",2,
            ()->new OceanBatch(art,level,tx*32-dx,ty*32-dy));
        batch.addFull().size(32,32).advColor(OceanGeometryCache.tileColors(level,tx,ty)).pos(dx,dy);return true;
    }
    private static void prepare(){
        if(shader!=null)return;
        shader=new GameShader("mosslumeOcean","mosslumeOcean");
        if(GL20.glGetProgrami(shader.getProgram(),GL20.GL_LINK_STATUS)==GL11.GL_FALSE)throw new IllegalStateException(GL20.glGetProgramInfoLog(shader.getProgram()));
        int side=128;byte[] pixels=new byte[side*side*4];
        FastNoiseLite a=new FastNoiseLite(81237),b=new FastNoiseLite(91751);a.SetFrequency(.055f);b.SetFrequency(.043f);
        for(int y=0;y<side;y++)for(int x=0;x<side;x++){
            // Torus blending makes the generated data seamless at each repeat edge.
            float u=x/(float)(side-1),v=y/(float)(side-1);
            int p=(x+y*side)*4;
            pixels[p]=(byte)Math.round((periodic(a,x,y,u,v,side)*.5f+.5f)*255);
            pixels[p+1]=(byte)Math.round((periodic(b,x,y,u,v,side)*.5f+.5f)*255);
            pixels[p+2]=0;pixels[p+3]=(byte)255;
        }
        noise=new GameTexture("mosslume-ocean-noise",side,side,pixels);noise.setBlendQuality(GameTexture.BlendQuality.LINEAR);
        System.out.println("MOSSLUME_OCEAN_READY upstream=gdquest-05a3931a worldUV=true continuous=true textures=own-fastnoise");
    }
    private static float periodic(FastNoiseLite n,int x,int y,float u,float v,int side){
        float a=n.GetNoise(x,y)*(1-u)+n.GetNoise(x-side+1,y)*u;
        float b=n.GetNoise(x,y-side+1)*(1-u)+n.GetNoise(x-side+1,y-side+1)*u;return a*(1-v)+b*v;
    }
    private static final class OceanBatch extends SharedTextureDrawOptions {
        final Level level;final float cameraX,cameraY,width,height;
        OceanBatch(GameTexture art,Level level,float cameraX,float cameraY){super(art);this.level=level;this.cameraX=cameraX;this.cameraY=cameraY;width=art.getWidth();height=art.getHeight();}
        @Override public void draw(int maxDrawsPerCall){
            TextureStateScope.begin();
            try{
                try{prepare();}catch(RuntimeException e){unavailable=true;System.err.println("MOSSLUME_OCEAN_UNAVAILABLE "+e);super.draw(maxDrawsPerCall);return;}
                shader.use();
                try{
                    shader.pass1f("visualTime",(float)CrownWindRenderer.timeSeconds());shader.pass2f("cameraWorld",cameraX,cameraY);
                    shader.pass2f("artSize",width,height);shader.pass1i("noiseTexture",1);noise.bindTexture(GL13.GL_TEXTURE1);
                    shader.pass1f("nightAmount",SceneLighting.night(level));GL13.glActiveTexture(GL13.GL_TEXTURE0);
                    super.draw(maxDrawsPerCall);batches++;
                }finally{shader.stop();}
            }finally{TextureStateScope.end();}
        }
    }
    private OceanSurfaceRenderer(){}
}
