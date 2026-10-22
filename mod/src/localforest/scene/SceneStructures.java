package localforest.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import necesse.level.maps.Level;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.LevelIdentifier;
import localforest.world.IslandWorld;

/** Reads scene instances from the native level GND that is already saved/networked. */
public final class SceneStructures {
    private static final Map<Level, Snapshot> snapshots = new WeakHashMap<>();
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
        boolean indexed = indexed(level.gndData);
        int count = houseCount(level.gndData);
        long signature = indexed ? 29 : 17;
        signature = signature * 31 + count;
        for (int i = 0; i < count; i++) {
            signature = signature * 31 + x(level, indexed, i);
            signature = signature * 31 + y(level, indexed, i);
        }
        Snapshot old = snapshots.get(level);
        if (old != null && old.signature == signature) return old.houses;
        ArrayList<StructureInstance> houses = new ArrayList<>(count);
        for (int i = 0; i < count; i++) houses.add(new StructureInstance("house-" + i,
            x(level, indexed, i), y(level, indexed, i)));
        Snapshot result = new Snapshot(signature, houses);
        snapshots.put(level, result);
        return result.houses;
    }

    private static boolean indexed(GNDItemMap data) {
        return data.getBoolean("mosslumeStructureSchemaV1") || data.hasKey("mosslumeHouseCount");
    }

    // Package-visible for the native GND zero-value round-trip regression.
    static int houseCount(GNDItemMap data) {
        if (indexed(data)) return Math.max(0, Math.min(64, data.getInt("mosslumeHouseCount")));
        boolean positioned = data.hasKey("mosslumeHouseX") && data.hasKey("mosslumeHouseY");
        return positioned || !data.getBoolean("mosslumeGenerated") ? 1 : 0;
    }

    private static int x(Level level, boolean indexed, int i) {
        return level.gndData.getInt(indexed ? "mosslumeHouse" + i + "X" : "mosslumeHouseX", indexed ? 0 : 52);
    }
    private static int y(Level level, boolean indexed, int i) {
        return level.gndData.getInt(indexed ? "mosslumeHouse" + i + "Y" : "mosslumeHouseY", indexed ? 0 : 43);
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
