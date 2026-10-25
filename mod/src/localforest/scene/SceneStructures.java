package localforest.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import necesse.level.maps.Level;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.util.LevelIdentifier;
import localforest.world.IslandWorld;
import necesse.engine.network.packet.PacketLevelGNDData;

/** Reads scene instances from the native level GND that is already saved/networked. */
public final class SceneStructures {
    private static final int SCHEMA = "mosslumeStructureSchemaV1".hashCode();
    private static final int COUNT = "mosslumeHouseCount".hashCode();
    private static final int GENERATED = "mosslumeGenerated".hashCode();
    private static final int LEGACY_X = "mosslumeHouseX".hashCode(), LEGACY_Y = "mosslumeHouseY".hashCode();
    private static final int[] X_KEYS = new int[64], Y_KEYS = new int[64];
    private static final int RESIDENCE_COUNT="mosslumeResidenceCountV1".hashCode();
    private static final int[] RES_X=new int[16],RES_Y=new int[16],RES_WIDTH=new int[16],RES_DEPTH=new int[16],RES_ID=new int[16];
    static {
        for (int i=0;i<64;i++) {
            X_KEYS[i]=("mosslumeHouse"+i+"X").hashCode();
            Y_KEYS[i]=("mosslumeHouse"+i+"Y").hashCode();
        }
        for(int i=0;i<16;i++){
            RES_X[i]=("mosslumeResidence"+i+"X").hashCode();RES_Y[i]=("mosslumeResidence"+i+"Y").hashCode();
            RES_WIDTH[i]=("mosslumeResidence"+i+"Width").hashCode();RES_DEPTH[i]=("mosslumeResidence"+i+"Depth").hashCode();
            RES_ID[i]=("mosslumeResidence"+i+"Id").hashCode();
        }
    }
    // Native metadata is authoritative. Draw preparation refreshes it once per
    // frame; callers outside that scope read it directly using prehashed keys.
    static int number(GNDItemMap data,int key,int fallback) {
        GNDItem item=data.getItem(key);
        return item instanceof GNDItem.GNDPrimitive ? ((GNDItem.GNDPrimitive)item).getInt() : fallback;
    }
    static boolean flag(GNDItemMap data,int key) {
        GNDItem item=data.getItem(key);
        return item instanceof GNDItem.GNDPrimitive && ((GNDItem.GNDPrimitive)item).getBoolean();
    }
    private static final Map<Level, Snapshot> snapshots = new WeakHashMap<>();
    private static final Map<Level, Boolean> drawManaged = new WeakHashMap<>();
    private static final class Snapshot {
        final long signature;
        final List<StructureInstance> houses;
        Snapshot(long signature, List<StructureInstance> houses) {
            this.signature = signature;
            this.houses = Collections.unmodifiableList(houses);
        }
    }

    /**
     * New maps set mosslumeHouseX/Y. Multiple houses may use mosslumeHouseCount
     * and mosslumeHouse0X/Y, etc. mosslumeStructureSchemaV1 preserves the
     * meaning of a zero count, which the native GND map otherwise removes.
     * The legacy 52,43 anchor is retained only for old fixed-island metadata.
     */
    public static synchronized List<StructureInstance> houses(Level level) {
        if (level == null || level.isCave || !LevelIdentifier.SURFACE_IDENTIFIER.equals(level.getIdentifier())
            || !IslandWorld.isIsland(level)) return List.of();
        Snapshot prepared=snapshots.get(level);
        if(prepared!=null&&drawManaged.containsKey(level))return prepared.houses;
        return refresh(level);
    }

    /** Called before native draw workers are scheduled, even for a stationary
     * camera/paused world. Edits become visible next frame without rereading and
     * boxing the same native metadata for thousands of tiles in that frame. */
    public static synchronized void refreshForDraw(Level level) {
        if(level==null||level.isCave||!LevelIdentifier.SURFACE_IDENTIFIER.equals(level.getIdentifier())
            ||!IslandWorld.isIsland(level))return;
        refresh(level);drawManaged.put(level,Boolean.TRUE);
    }
    private static List<StructureInstance> refresh(Level level) {
        boolean indexed = indexed(level.gndData);
        int count = houseCount(level.gndData);
        long signature = indexed ? 29 : 17;
        signature = signature * 31 + count;
        for (int i = 0; i < count; i++) {
            signature = signature * 31 + x(level, indexed, i);
            signature = signature * 31 + y(level, indexed, i);
        }
        int residenceCount=Math.max(0,Math.min(16,number(level.gndData,RESIDENCE_COUNT,0)));
        signature=signature*31+residenceCount;
        for(int i=0;i<residenceCount;i++){
            signature=signature*31+number(level.gndData,RES_X[i],0);signature=signature*31+number(level.gndData,RES_Y[i],0);
            signature=signature*31+number(level.gndData,RES_WIDTH[i],0);signature=signature*31+number(level.gndData,RES_DEPTH[i],0);
        }
        Snapshot old = snapshots.get(level);
        if (old != null && old.signature == signature) return old.houses;
        ArrayList<StructureInstance> houses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) houses.add(new StructureInstance("house-" + i,
            x(level, indexed, i), y(level, indexed, i)));
        for(int i=0;i<residenceCount;i++){
            int width=number(level.gndData,RES_WIDTH[i],7),depth=number(level.gndData,RES_DEPTH[i],6);
            if(width<3||width>15||width%2==0||depth<3||depth>12)continue;
            houses.add(new StructureInstance("residence-"+i,number(level.gndData,RES_X[i],0),number(level.gndData,RES_Y[i],0),width,depth));
        }
        Snapshot result = new Snapshot(signature, houses);
        snapshots.put(level, result);
        return result.houses;
    }
    /** Persistent residence list is separate from WorldDefinition's primary-house stamp. */
    public static synchronized void registerResidence(Level level,String id,int left,int top,int width,int depth){
        if(level==null||!level.isServer()||id==null||id.isBlank())return;
        boolean changed=registerResidenceData(level.gndData,id,left,top,width,depth);
        if(changed){snapshots.remove(level);level.getServer().network.sendToClientsAt(new PacketLevelGNDData(level),level);}
    }
    /** Native GND writer shared with the packet/save regression; no alternate persistence format. */
    static boolean registerResidenceData(GNDItemMap data,String id,int left,int top,int width,int depth){
        new StructureInstance(id,left+width/2,top+depth,width,depth); // Validate before mutating metadata.
        int count=Math.max(0,Math.min(16,number(data,RESIDENCE_COUNT,0))),slot=-1;
        for(int i=0;i<count;i++)if(id.equals(data.getString("mosslumeResidence"+i+"Id",""))){slot=i;break;}
        boolean added=slot<0;
        if(slot<0){if(count>=16)throw new IllegalStateException("Residence metadata capacity reached");slot=count++;}
        int x=left+width/2,y=top+depth;
        boolean changed=added||number(data,RES_X[slot],0)!=x||number(data,RES_Y[slot],0)!=y
            ||number(data,RES_WIDTH[slot],0)!=width||number(data,RES_DEPTH[slot],0)!=depth;
        String prefix="mosslumeResidence"+slot;
        data.setString(prefix+"Id",id);data.setInt(prefix+"X",x).setInt(prefix+"Y",y)
            .setInt(prefix+"Width",width).setInt(prefix+"Depth",depth).setInt("mosslumeResidenceCountV1",count);
        return changed;
    }

    private static boolean indexed(GNDItemMap data) {
        return flag(data,SCHEMA) || data.getItem(COUNT)!=null;
    }

    // Package-visible for the native GND zero-value round-trip regression.
    static int houseCount(GNDItemMap data) {
        if (indexed(data)) return Math.max(0, Math.min(64, number(data,COUNT,0)));
        boolean positioned = data.getItem(LEGACY_X)!=null && data.getItem(LEGACY_Y)!=null;
        return positioned || !flag(data,GENERATED) ? 1 : 0;
    }

    private static int x(Level level, boolean indexed, int i) {
        return number(level.gndData,indexed ? X_KEYS[i] : LEGACY_X,indexed ? 0 : 52);
    }
    private static int y(Level level, boolean indexed, int i) {
        return number(level.gndData,indexed ? Y_KEYS[i] : LEGACY_Y,indexed ? 0 : 43);
    }

    public static StructureInstance atFoot(Level level, int x, int y) {
        for (StructureInstance house : houses(level)) if (house.isFoot(x, y)) return house;
        return null;
    }

    public static StructureInstance containing(Level level, int x, int y) {
        for (StructureInstance house : houses(level)) if (house.contains(x, y)) return house;
        return null;
    }

    public static boolean exists(Level level, StructureInstance house) {
        for (int x = house.left; x <= house.right; x++) for (int y = house.top; y <= house.bottom; y++) {
            if (!RegionSceneQuery.loaded(level, x, y)) continue;
            String id = level.getObject(x, y).getStringID();
            if (id.contains("woodwall") || id.equals("localforesthousewall")) return true;
        }
        return false;
    }

    private SceneStructures() { }
}
