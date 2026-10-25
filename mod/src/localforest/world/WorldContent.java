package localforest.world;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Immutable versioned content; new ordinary biomes use this table, not name branches. */
public final class WorldContent {
    private static final Map<Integer,WorldContent> versions = new HashMap<>();
    private final Properties data;
    public final int version,width,height;
    public final List<String> presets,biomes;
    public final List<TemplateObject> campObjects;
    public final String fingerprint;

    private WorldContent(int version) {
        data=new Properties();
        String resource="/resources/mosslume/content/worlds-v"+version+".properties";
        try(InputStream in=WorldContent.class.getResourceAsStream(resource)) {
            if(in==null)throw new IllegalStateException("Missing immutable world content: "+resource);
            data.load(new InputStreamReader(in,StandardCharsets.UTF_8));
        }catch(Exception e){throw new IllegalStateException("Cannot load world content v"+version,e);}
        this.version=integer("version");
        if(integer("schema")!=1||this.version!=version)throw new IllegalStateException("Unsupported world content schema/version");
        width=integer("width");height=integer("height");
        if(version>=4?(width!=0||height!=0):(width<96||height<96||width>512||height>512))throw new IllegalStateException("World dimensions outside supported budget");
        presets=List.of(csv("presets"));biomes=List.of(csv("biomes"));
        if(presets.isEmpty()||biomes.isEmpty())throw new IllegalStateException("Empty world definitions");
        for(String id:presets){text(id+".name");decimal(id+".landRadius");decimal(id+".treeDensity");}
        for(String id:biomes){text(id+".nativeBiome");csv(id+".trees");decimal(id+".threshold");decimal(id+".treeMultiplier");text(id+".ground");}
        List<TemplateObject> objects=new ArrayList<>();
        try(InputStream in=WorldContent.class.getResourceAsStream("/resources/mosslume/content/camp-v"+version+".csv")){
            if(in==null)throw new IllegalStateException("Missing camp template");
            BufferedReader reader=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));String line;
            while((line=reader.readLine())!=null){
                if(line.isBlank()||line.startsWith("#"))continue;
                String[] p=line.split(",");if(p.length!=6)throw new IllegalStateException("Invalid camp template row "+line);
                if(!List.of("camp","house","cave").contains(p[0]))throw new IllegalStateException("Unknown template anchor "+p[0]);
                objects.add(new TemplateObject(p[0],Integer.parseInt(p[1]),Integer.parseInt(p[2]),p[3],Integer.parseInt(p[4]),p[5]));
            }
        }catch(Exception e){throw new IllegalStateException("Cannot load camp template v"+version,e);}
        campObjects=List.copyOf(objects);
        try{
            StringBuilder canonical=new StringBuilder();
            data.stringPropertyNames().stream().sorted().forEach(key->canonical.append(key).append('=').append(text(key)).append('\n'));
            for(TemplateObject object:campObjects)canonical.append(object.anchor()).append(',').append(object.dx()).append(',').append(object.dy()).append(',').append(object.id()).append(',').append(object.rotation()).append(',').append(object.role()).append('\n');
            fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e){throw new IllegalStateException("Cannot fingerprint world content",e);}
    }
    public static synchronized WorldContent load(int version){return versions.computeIfAbsent(version,WorldContent::new);}
    public String text(String key){String v=data.getProperty(key);if(v==null||v.isBlank())throw new IllegalStateException("Missing content field "+key);return v.trim();}
    public int integer(String key){return Integer.parseInt(text(key));}
    public double decimal(String key){double v=Double.parseDouble(text(key));if(!Double.isFinite(v))throw new IllegalStateException("Non-finite field "+key);return v;}
    public String[] csv(String key){return Arrays.stream(text(key).split(",")).map(String::trim).filter(s->!s.isEmpty()).toArray(String[]::new);}
    public String biome(float noise){String result=biomes.get(0);for(String b:biomes)if(noise>=decimal(b+".threshold"))result=b;return result;}
    public record TemplateObject(String anchor,int dx,int dy,String id,int rotation,String role){
        public int x(WorldDefinition d){return (anchor.equals("house")?d.houseX:anchor.equals("cave")?d.caveX:d.campX)+dx;}
        public int y(WorldDefinition d){return (anchor.equals("house")?d.houseY:anchor.equals("cave")?d.caveY:d.campY)+dy;}
    }
}
