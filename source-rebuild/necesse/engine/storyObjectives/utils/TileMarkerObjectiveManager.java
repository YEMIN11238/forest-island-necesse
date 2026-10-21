package necesse.engine.storyObjectives.utils;

import java.awt.Point;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.AreaFinder;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.registries.TileRegistry;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.level.maps.Level;

public abstract class TileMarkerObjectiveManager {
   public int maxTilesToKeepTrackOf;
   public int maxTileRange;
   protected long nextSearchTime;
   protected ArrayList<Point> foundTiles = new ArrayList<>();

   public TileMarkerObjectiveManager(int maxTilesToKeepTrackOf, int maxTileRange) {
      this.maxTilesToKeepTrackOf = maxTilesToKeepTrackOf;
      this.maxTileRange = maxTileRange;
   }

   public void clientTick(PlayerMob player) {
      if (this.nextSearchTime <= player.getLocalTime()) {
         final Level level = player.getLevel();
         if (level != null) {
            this.foundTiles.clear();
            final AtomicBoolean foundEmpty = new AtomicBoolean();
            (new AreaFinder(player.getTileX(), player.getTileY(), this.maxTileRange) {
               @Override
               public boolean checkPoint(int x, int y) {
                  if (level.getTileID(x, y) == TileRegistry.emptyID) {
                     foundEmpty.set(true);
                     return true;
                  } else if (TileMarkerObjectiveManager.this.isValidTile(level, x, y)) {
                     TileMarkerObjectiveManager.this.foundTiles.add(new Point(x, y));
                     return TileMarkerObjectiveManager.this.foundTiles.size() >= TileMarkerObjectiveManager.this.maxTilesToKeepTrackOf;
                  } else {
                     return false;
                  }
               }
            }).runFinder();
            this.nextSearchTime = player.getLocalTime() + (foundEmpty.get() ? 1000 : 2000);
         }
      }
   }

   public abstract boolean isValidTile(Level var1, int var2, int var3);

   public Point getFirstFoundTile() {
      return this.foundTiles.isEmpty() ? null : this.foundTiles.get(0);
   }

   public void clearFoundTiles() {
      this.foundTiles.clear();
   }

   public ArrayList<Point> getFoundTiles() {
      return this.foundTiles;
   }

   public DrawOptions getArrowHudDrawOptions(GameMessage foundMessage, GameMessage nonFoundMessage, GameCamera camera, PlayerMob perspective) {
      DrawOptionsList drawOptions = new DrawOptionsList();

      for (Point foundTile : this.getFoundTiles()) {
         drawOptions.add(StoryObjective.getLevelTextDrawOptions(foundMessage, foundTile.x * 32 + 16, foundTile.y * 32, camera, perspective, true));
      }

      if (drawOptions.isEmpty() && nonFoundMessage != null) {
         drawOptions.add(
            StoryObjective.getLevelTextDrawOptions(nonFoundMessage, perspective.getDrawX(), perspective.getDrawY() - 48, camera, perspective, false)
         );
      }

      return drawOptions;
   }
}
