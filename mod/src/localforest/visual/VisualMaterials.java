package localforest.visual;

import com.eclipsesource.json.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;

/** One registry drives visible geometry, wind classification, normals and projected silhouettes. */
public final class VisualMaterials {
    private static final Map<String,VisualMaterialDefinition> definitions=new HashMap<>();
    private static final Map<String,float[]> anchors=new HashMap<>();
    private static final IdentityHashMap<GameTexture,VisualMaterialDefinition> textures=new IdentityHashMap<>();
    private static final IdentityHashMap<GameTexture,GameTexture> normals=new IdentityHashMap<>();
    public static void catalog(String biome){
        // ResourceEncoder deliberately indexes only png/ogg/glsl/ttf. LoadedMod appends
        // its JAR to the system loader; packaged data keeps the full resources/ prefix.
        String path="resources/mosslume/biomes/"+biome+"/catalog.json";
        try(InputStream in=VisualMaterials.class.getClassLoader().getResourceAsStream(path)){
            if(in==null)throw new FileNotFoundException("Missing packaged visual metadata "+path);
            JsonObject root=Json.parse(new InputStreamReader(in,StandardCharsets.UTF_8)).asObject();
            for(JsonValue value:root.get("entries").asArray()){
                JsonObject entry=value.asObject();JsonValue anchor=entry.get("footAnchor");
                if(anchor!=null)anchors.put("mosslume/biomes/"+biome+"/"+entry.getString("id",""),new float[]{anchor.asArray().get(0).asFloat(),anchor.asArray().get(1).asFloat()});
            }
        }catch(Exception e){throw new IllegalStateException("Cannot read visual anchors for "+biome,e);}
    }
    public static VisualMaterialDefinition register(String id,GameTexture texture,BufferedImage image){
        float[] anchor=anchors.get(id);
        float ax=anchor==null?image.getWidth()*.5f:anchor[0],ay=anchor==null?image.getHeight():anchor[1];
        if(anchor==null&&!id.equals("glow")&&!id.equals("mote")&&!id.equals("smoke")&&!id.startsWith("flame-")){
            int bottom=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if((image.getRGB(x,y)>>>24)>=64)bottom=Math.max(bottom,y+1);
            if(bottom>0)ay=bottom;
        }
        boolean crown=id.endsWith("-top")||id.startsWith("crown-")||id.endsWith("/tree-crown");
        boolean tree=id.endsWith("/tree");
        float target=0,shadow=crown||tree?108:Math.min(100,image.getHeight()),rigidity=1;
        if(crown){rigidity=.12f;if(!id.contains("/biomes/")){
            target=id.equals("tree-a-top")?124:id.equals("tree-b-top")?116:id.equals("crown-young")?96:128;
            shadow=target+10;ay+=6;
        }}
        if(tree){target=id.contains("snowfield")?140:id.contains("mushroom")?128:134;rigidity=.12f;shadow=target*.90f;}
        if(id.contains("/biomes/")&&(id.endsWith("/tree-crown")||id.endsWith("/tree-trunk")))shadow=(id.contains("snowfield")?140:id.contains("mushroom")?128:134)*.90f;
        if(id.contains("/biomes/")&&id.contains("/flora-")){
            int i=Integer.parseInt(id.substring(id.lastIndexOf('-')+1));
            String b=id.split("/")[2];
            boolean flexible=switch(b){
                case "wetland" -> i==0||i==2||i==3||i==4||i==5||i==8;
                case "mushroom" -> i==0||i==2||i==3||i==6||i==7;
                case "snowfield" -> i==0||i==3||i==4;
                case "desert" -> i==2||i==3||i==5;
                default -> false;
            };
            rigidity=flexible?.26f:1;
            target=switch(b){
                case "wetland" -> i==2?46:i==0?34:i==1||i==6||i==7?27:23;
                case "mushroom" -> i==0?32:i==7?34:i==4||i==8?28:24;
                case "snowfield" -> i==0?50:i==1||i==2?44:i==3?31:27;
                case "desert" -> i==0||i==4?42:i==3?38:26;
                case "ruins" -> i==0?62:i==1?48:i==3||i==5?38:26;
                default -> i==0||i==1?38:28;
            };
            shadow=Math.max(12,target*.65f);
        }else if(!id.contains("/biomes/")&&(id.equals("bush")||id.equals("berry-bush")||id.equals("fern")||id.equals("grass")||id.equals("flowers")||id.equals("wildflowers")||id.equals("reeds")||id.contains("/berry-")||id.contains("/crops/"))){
            rigidity=.20f;target=id.equals("reeds")?44:id.equals("bush")||id.contains("berry")?30:22;shadow=target*.7f;
        }else if(id.endsWith("/sapling")||id.endsWith("/young-tree")||id.endsWith("/seedling")){
            rigidity=.35f;target=id.endsWith("/young-tree")?64:32;
        }
        if(id.contains("/biomes/")&&id.contains("/structure-")){
            String biome=id.split("/")[2];int variant=Integer.parseInt(id.substring(id.lastIndexOf('-')+1));
            target=switch(biome){
                case "ruins" -> variant==0?90:variant==1?124:92;
                case "desert" -> variant==0?190:variant==1?154:126;
                case "wetland" -> variant==2?158:192;
                case "mushroom" -> 194;
                default -> 200;
            };
            shadow=Math.min(146,target*.64f);
        }
        if(id.contains("house-layers/")||id.equals("mosslume-house-shadow")){ax=48;ay=108;shadow=100;}
        VisualMaterialDefinition material=new VisualMaterialDefinition(id,image,ax,ay,target,shadow,rigidity,crown||tree);
        definitions.put(id,material);textures.put(texture,material);
        GameTexture normal=new GameTexture("mosslume-normal-"+id,image.getWidth(),image.getHeight(),material.bakeNormals(image));
        normal.setBlendQuality(GameTexture.BlendQuality.NEAREST);GameTexture old=normals.put(texture,normal);if(old!=null)old.delete();
        return material;
    }
    public static VisualMaterialDefinition get(String id){return definitions.get(id);}
    public static VisualMaterialDefinition get(GameTexture texture){return textures.get(texture);}
    public static GameTexture normal(GameTexture texture){return normals.get(texture);}
    public static float scale(String id,float fallback){VisualMaterialDefinition d=get(id);return d==null?fallback:d.scale(fallback);}
    public static boolean flexible(String id){VisualMaterialDefinition d=get(id);return d!=null&&d.rigidity<1;}
    public static int size(){return definitions.size();}
    private VisualMaterials(){}
}
