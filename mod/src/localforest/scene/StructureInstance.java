package localforest.scene;

/** Stable scene anchor shared by art, doors, roof occlusion and local window lights. */
public final class StructureInstance {
    public final String id;
    public final int footTileX, footTileY;
    public final int left, top, right, bottom, doorTileX, doorTileY;

    public StructureInstance(String id, int footTileX, int footTileY) {
        this.id = id;
        this.footTileX = footTileX;
        this.footTileY = footTileY;
        // The existing authored house has a five-by-four tile native shell.
        // These offsets belong to its definition, not to a particular map.
        left = footTileX - 2;
        right = footTileX + 2;
        top = footTileY - 4;
        bottom = footTileY - 1;
        doorTileX = footTileX;
        doorTileY = footTileY - 1;
    }

    public boolean contains(int x, int y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    public boolean isFoot(int x, int y) {
        return x == footTileX && y == footTileY;
    }

    public float windowX(int index) {
        return footTileX * 32f + (index == 0 ? -43 : 65);
    }

    public float windowY() {
        return footTileY * 32f - 25;
    }
}
