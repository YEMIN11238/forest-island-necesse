package localforest.world;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import necesse.engine.world.WorldEntity;
import necesse.level.maps.Level;

/** The encoded native seed is the durable selection; no mutable process-global pending world. */
public final class WorldDefinition {
    public static final int GENERATOR_VERSION=1;
    public final int generatorVersion,contentVersion,width,height,spawnX,spawnY,houseX,houseY,campX,campY,caveX,caveY;
    public final String preset,rules,userSeed,encodedSeed,contentHash;
    public final long seed;
    public final WorldContent content;

    private WorldDefinition(String encoded,String[] p) {
        generatorVersion=Integer.parseInt(p[1]);contentVersion=Integer.parseInt(p[2]);
        if(generatorVersion!=GENERATOR_VERSION)throw new IllegalArgumentException("Unsupported Mosslume generator "+generatorVersion);
        content=WorldContent.load(contentVersion);preset=p[3];rules=p[4];
        contentHash=p[6];
        if(!content.fingerprint.equals(contentHash))throw new IllegalArgumentException("World content fingerprint differs from this save; restore its exact content version before loading");
        if(!content.presets.contains(preset)||!(rules.equals("standard")||rules.equals("relaxed")))throw new IllegalArgumentException("Unsupported world preset/rules");
        userSeed=new String(Base64.getUrlDecoder().decode(p[5]),StandardCharsets.UTF_8);
        if(userSeed.isBlank()||userSeed.length()>80)throw new IllegalArgumentException("Seed must contain 1–80 characters");
        encodedSeed=encoded;seed=hash(userSeed);width=content.width;height=content.height;
        campX=width/2;campY=height/2;spawnX=campX-2;spawnY=campY+2;
        houseX=campX+6;houseY=campY+6;caveX=campX+width/4;caveY=campY-12;
    }
    public static String encodeSeed(String seed,String preset,String rules) {
        if(seed==null)throw new IllegalArgumentException("Seed is required");
        String result="MOSS:"+GENERATOR_VERSION+":1:"+preset+":"+rules+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(seed.trim().getBytes(StandardCharsets.UTF_8))+":"+WorldContent.load(1).fingerprint;
        fromSeed(result);return result;
    }
    public static WorldDefinition fromSeed(String seed){
        if(seed==null||!seed.startsWith("MOSS:"))return null;
        String[] p=seed.split(":",-1);
        if(p.length!=7)throw new IllegalArgumentException("Invalid Mosslume world definition or missing content fingerprint");
        return new WorldDefinition(seed,p);
    }
    public static WorldDefinition forWorld(WorldEntity world){return world==null?null:fromSeed(world.worldSeed);}
    public static WorldDefinition forLevel(Level level){
        if(level==null)return null;
        String saved=level.gndData.getString("mosslumeWorldDefinition","");
        return saved.isEmpty()?forWorld(level.getWorldEntity()):fromSeed(saved);
    }
    public void stamp(Level level){
        level.gndData.setBoolean("localforestisland",true);
        level.gndData.setBoolean("mosslumeGenerated",true);
        level.gndData.setString("mosslumeWorldDefinition",encodedSeed);
        level.gndData.setInt("mosslumeSpawnX",spawnX).setInt("mosslumeSpawnY",spawnY);
        level.gndData.setInt("mosslumeCampX",campX).setInt("mosslumeCampY",campY);
        // Native GND drops numeric zero values, so keep a non-default schema flag
        // to distinguish an empty structure list from old fixed-island metadata.
        level.gndData.setBoolean("mosslumeStructureSchemaV1",true);
        level.gndData.setInt("mosslumeHouseCount",level.isCave?0:1);
        if(level.isCave){
            level.gndData.clearItem("mosslumeHouseX").clearItem("mosslumeHouseY");
            level.gndData.clearItem("mosslumeHouse0X").clearItem("mosslumeHouse0Y");
        }else{
            level.gndData.setInt("mosslumeHouseX",houseX).setInt("mosslumeHouseY",houseY);
            level.gndData.setInt("mosslumeHouse0X",houseX).setInt("mosslumeHouse0Y",houseY);
        }
        level.gndData.setBoolean("mosslumeStablePathsV1",true);
    }
    public static long hash(String text){long h=0xcbf29ce484222325L;for(byte b:text.getBytes(StandardCharsets.UTF_8)){h^=b&255;h*=0x100000001b3L;}return h;}
    public String displayName(){return content.text(preset+".name");}
}
