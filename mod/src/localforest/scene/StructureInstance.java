package localforest.scene;

/** Stable scene anchor shared by art, doors, roof occlusion and local window lights. */
public final class StructureInstance {
    public final String id;
    public final int footTileX, footTileY;
    public final int left, top, right, bottom, doorTileX, doorTileY;
    public final int nativeWidth,nativeDepth;
    public final float visualScale;

    public StructureInstance(String id, int footTileX, int footTileY) {
        this(id,footTileX,footTileY,5,4);
    }
    public StructureInstance(String id,int footTileX,int footTileY,int width,int depth){
        if(width<3||width>15||width%2==0||depth<3||depth>12)throw new IllegalArgumentException("Invalid residence shell");
        this.id = id;
        this.footTileX = footTileX;
        this.footTileY = footTileY;
        nativeWidth=width;nativeDepth=depth;visualScale=2f*width/5f;
        left = footTileX - width/2;
        right = footTileX + width/2;
        top = footTileY - depth;
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
        return footTileX*32f+16+(index==0?-59:49)*(visualScale/2f);
    }

    public float windowY() {
        return footTileY*32f+4-29*(visualScale/2f);
    }
}
