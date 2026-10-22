package localforest.scene;

import java.util.Map;
import java.util.WeakHashMap;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Rendering queries only loaded regions in a camera-sized neighborhood. Never generates land. */
public final class RegionSceneQuery {
    private static final Map<Level, ObservedView> views = new WeakHashMap<>();

    /** One observation per actual camera state, rather than two bounds
     * allocations for every terrain tile and object in the same render pass.
     * This value never retains its weak Level key or a camera instance.
     */
    static final class ObservedView {
        final int x, y, width, height, levelWidth, levelHeight;
        final TileBounds bounds;
        private ObservedView(int x, int y, int width, int height, int levelWidth, int levelHeight, TileBounds bounds) {
            this.x=x;this.y=y;this.width=width;this.height=height;
            this.levelWidth=levelWidth;this.levelHeight=levelHeight;this.bounds=bounds;
        }
        static ObservedView update(ObservedView old, int x, int y, int width, int height, int levelWidth, int levelHeight) {
            if(old!=null&&old.x==x&&old.y==y&&old.width==width&&old.height==height
                &&old.levelWidth==levelWidth&&old.levelHeight==levelHeight)return old;
            TileBounds bounds=TileBounds.fromPixels(x,y,width,height,12).clip(levelWidth,levelHeight);
            if(old!=null&&bounds.equals(old.bounds))bounds=old.bounds;
            return new ObservedView(x,y,width,height,levelWidth,levelHeight,bounds);
        }
    }

    public static synchronized void observe(Level level, GameCamera camera) {
        ObservedView old=views.get(level);
        ObservedView current=ObservedView.update(old,camera.getX(),camera.getY(),camera.getWidth(),camera.getHeight(),
            level.tileWidth,level.tileHeight);
        if(current!=old)views.put(level,current);
    }

    public static synchronized TileBounds visibleLights(Level level) {
        ObservedView view = views.get(level);
        if (view != null) return view.bounds;
        int width = WindowManager.getWindow() == null ? 1200 : WindowManager.getWindow().getSceneWidth();
        int height = WindowManager.getWindow() == null ? 900 : WindowManager.getWindow().getSceneHeight();
        PlayerMob player = level.getClient() == null ? null : level.getClient().getPlayer();
        float x = player == null ? level.tileWidth * 16f : player.x;
        float y = player == null ? level.tileHeight * 16f : player.y;
        return TileBounds.fromPixels(x - width / 2f, y - height / 2f, width, height, 12)
            .clip(level.tileWidth, level.tileHeight);
    }

    public static boolean inBounds(Level level, int x, int y) {
        return (level.tileWidth <= 0 || x >= 0 && x < level.tileWidth)
            && (level.tileHeight <= 0 || y >= 0 && y < level.tileHeight);
    }

    public static boolean loaded(Level level, int x, int y) {
        return inBounds(level, x, y) && level.regionManager.isTileLoaded(x, y);
    }

    public static boolean liquid(Level level, int x, int y) {
        Region region = level.regionManager.getRegionByTile(x, y, false);
        return region == null || region.tileLayer.isTileLiquidByRegion(x - region.tileXOffset, y - region.tileYOffset);
    }

    private RegionSceneQuery() { }
}
