package localforest.scene;

/** Headless regression for translated buildings and camera-local query boundaries. */
public final class SceneGeometryTest {
    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        StructureInstance legacy = new StructureInstance("old", 52, 43);
        StructureInstance relocated = new StructureInstance("new", 108, 86);
        for (int y = 35; y < 48; y++) for (int x = 45; x < 60; x++) {
            boolean expected = x >= 50 && x <= 54 && y >= 39 && y <= 42;
            require(legacy.contains(x, y) == expected, "legacy footprint changed");
            require(legacy.contains(x, y) == relocated.contains(x + 56, y + 43), "translated footprint diverged");
        }
        require(legacy.doorTileX == 52 && legacy.doorTileY == 42, "legacy door moved");
        require(relocated.doorTileX == 108 && relocated.doorTileY == 85, "door did not follow structure");
        require(relocated.windowX(0) - legacy.windowX(0) == 56 * 32, "left light translation");
        require(relocated.windowX(1) - legacy.windowX(1) == 56 * 32, "right light translation");
        require(relocated.windowY() - legacy.windowY() == 43 * 32, "light vertical translation");
        require(!legacy.contains(108, 85), "two houses overlap due to global anchor");

        for (int x : new int[]{-321, -1, 0, 31, 32, 2048, 1000000}) {
            TileBounds b = TileBounds.fromPixels(x, x - 19, 1200, 900, 12);
            require(b.contains(Math.floorDiv(x, 32), Math.floorDiv(x - 19, 32)), "camera origin omitted");
            require(b.contains(Math.floorDiv(x + 1199, 32), Math.floorDiv(x - 19 + 899, 32)), "viewport corner omitted");
            require(b.width() <= 63 && b.height() <= 54, "query grew with world coordinate");
            TileBounds clipped = b.clip(192, 160);
            require(clipped.left >= 0 && clipped.top >= 0 && clipped.right <= 192 && clipped.bottom <= 160, "finite bounds escaped");
            require(clipped.width() >= 0 && clipped.height() >= 0, "outside camera created negative texture bounds");
        }
        TileBounds origin = TileBounds.fromPixels(0, 0, 1200, 900, 12);
        TileBounds distant = TileBounds.fromPixels(32000, 64000, 1200, 900, 12);
        require(origin.width() == distant.width() && origin.height() == distant.height(), "query size depends on map size");
        System.out.println("SceneGeometryTest PASS: legacy house preserved, translated door/lights, bounded camera queries");
    }
}
