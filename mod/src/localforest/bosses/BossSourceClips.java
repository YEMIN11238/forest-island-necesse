package localforest.bosses;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.imageio.ImageIO;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;

/** Directly imported user GIF clips and WebP effects; never substitutes generated Boss art. */
public final class BossSourceClips {
    private static final Map<String,SourceActionClip> specs=new LinkedHashMap<>();
    private static final Map<String,GameTexture> textures=new HashMap<>();
    private static final Map<String,Effect> effects=new HashMap<>();
    private static Properties properties;
    public record Effect(GameTexture texture,int cell,int footX,int footY,int columns,int frameMillis) {
        public int frame(long millis){return Math.min(columns-1,Math.max(0,(int)(millis/frameMillis)));}
        public int duration(){return columns*frameMillis;}
    }
    private static synchronized void readSpecs(){
        if(properties!=null)return;
        Properties p=new Properties();
        try(var input=BossSourceClips.class.getClassLoader().getResourceAsStream("resources/mosslume/bosses/source-clips.properties")){
            if(input==null)throw new IllegalStateException("Packaged direct-source Boss metadata is missing from mod classloader");
            p.load(input);
            for(var def:MosslumeBosses.ALL)for(int type=0;type<2;type++)specs.put(def.art()+".attack"+type,SourceActionClip.read(p,def.art()+".attack"+type));
            properties=p;
        }catch(Exception error){throw new IllegalStateException("Direct source Boss clip metadata is missing or invalid",error);}
    }
    public static SourceActionClip spec(String art,int attack){readSpecs();SourceActionClip clip=specs.get(art+".attack"+attack);if(clip==null)throw new IllegalArgumentException("Unknown source Boss attack");return clip;}
    public static GameTexture texture(SourceActionClip clip){GameTexture texture=textures.get(clip.file());if(texture==null)throw new IllegalStateException("Direct source Boss clip texture not initialized: "+clip.file());return texture;}
    public static Effect effect(String id){Effect effect=effects.get(id);if(effect==null)throw new IllegalStateException("Direct source Boss effect not initialized: "+id);return effect;}
    public static void initResources(){
        readSpecs();textures.clear();effects.clear();
        for(SourceActionClip clip:specs.values()){
            String path="mosslume/bosses/"+clip.file();
            try(var input=ResourceEncoder.getResourceInputStream(path+".png")){
                int cell=clip.frameSize();var image=ImageIO.read(input);if(image==null||image.getWidth()!=clip.columns()*cell||image.getHeight()!=4*cell)throw new IllegalStateException("Expected original source clip "+clip.columns()+"x4 at "+cell+"px");
                for(int row=0;row<4;row++)for(int column=0;column<clip.columns();column++){
                    boolean visible=false;for(int y=row*cell;y<(row+1)*cell&&!visible;y++)for(int x=column*cell;x<(column+1)*cell;x++)if((image.getRGB(x,y)>>>24)>=128){visible=true;break;}
                    if(!visible&&!clip.emptyFrames().contains(column))throw new IllegalStateException("Undeclared empty source pose "+column+","+row);
                }
                GameTexture texture=GameTexture.fromFile(path);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);textures.put(clip.file(),texture);
            }catch(Exception error){throw new IllegalStateException("Missing or invalid direct source Boss animation: "+path,error);}
        }
        loadEffect("stone-glyph",32);loadEffect("stone-impact",96);
    }
    private static void loadEffect(String id,int cell){
        int columns=Integer.parseInt(properties.getProperty("fx."+id+".columns","4")),frameMillis=Integer.parseInt(properties.getProperty("fx."+id+".frameMillis","90"));
        cell=Integer.parseInt(properties.getProperty("fx."+id+".frameSize",String.valueOf(cell)));
        int footX=Integer.parseInt(properties.getProperty("fx."+id+".footX",String.valueOf(cell/2))),footY=Integer.parseInt(properties.getProperty("fx."+id+".footY",String.valueOf(id.equals("stone-glyph")?cell/2:cell*5/6)));
        if(columns<1||columns>24||frameMillis<30||frameMillis>250)throw new IllegalStateException("Invalid source effect metadata "+id);
        String path="mosslume/bosses/"+id;
        try(var input=ResourceEncoder.getResourceInputStream(path+".png")){
            var image=ImageIO.read(input);if(image==null||image.getWidth()!=columns*cell||image.getHeight()!=cell)throw new IllegalStateException("Expected effect strip "+(columns*cell)+"x"+cell);
            GameTexture texture=GameTexture.fromFile(path);texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);effects.put(id,new Effect(texture,cell,footX,footY,columns,frameMillis));
        }catch(Exception error){throw new IllegalStateException("Missing or invalid direct source Boss effect "+id,error);}
    }
    public static Map<String,Object> diagnostics(){Map<String,Object> out=new LinkedHashMap<>();out.put("orientation","Original side-view images; left/right mirrored. Up/down reuse the side view and are not authored top-down views.");out.put("sourceClips",specs.entrySet().stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,e->Map.of("file",e.getValue().file(),"columns",e.getValue().columns(),"frameSize",e.getValue().frameSize(),"foot",java.util.List.of(e.getValue().footX(),e.getValue().footY()),"frameMillis",e.getValue().frameMillis(),"declaredInvisibleFrames",e.getValue().emptyFrames().toString()))));out.put("loadedEffects",effects.keySet().toString());out.put("death","Source idle fade, because supplied sheets have no death/corpse animation.");return out;}
    private BossSourceClips(){}
}
