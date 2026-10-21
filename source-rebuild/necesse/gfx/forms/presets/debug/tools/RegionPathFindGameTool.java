package necesse.gfx.forms.presets.debug.tools;

import java.awt.Color;
import java.awt.Point;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.pathfinding.PathResult;
import necesse.engine.util.pathfinding.Pathfinding;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.path.RegionPathfinding;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.HUD;
import necesse.level.maps.hudManager.HudDrawElement;
import necesse.level.maps.regionSystem.SubRegion;

public class RegionPathFindGameTool extends MouseDebugGameTool {
   public PathResult<SubRegion, RegionPathfinding> result;
   public boolean cacheResult = false;
   public double pathTileLength;
   public Point from;
   public Point to;
   public TilePathFindGameTool.DoorMode doorMode = TilePathFindGameTool.DoorMode.CAN_OPEN;
   public HudDrawElement hudElement;

   public RegionPathFindGameTool(DebugForm parent) {
      super(parent, "Region path finding");
   }

   @Override
   public void init() {
      this.onLeftClick(e -> {
         this.from = new Point(this.getMouseTileX(), this.getMouseTileY());
         this.updatePath();
         return true;
      }, "Select start tile");
      this.onRightClick(e -> {
         this.to = new Point(this.getMouseTileX(), this.getMouseTileY());
         this.updatePath();
         return true;
      }, "Select target tile");
      this.onScroll(e -> {
         TilePathFindGameTool.DoorMode[] values = TilePathFindGameTool.DoorMode.values();
         int nextIndex = Math.floorMod(this.doorMode.ordinal() + (e.getMouseWheelY() < 0.0 ? -1 : 1), values.length);
         this.doorMode = values[nextIndex];
         this.updateScrollUsage();
         this.updatePath();
         return true;
      }, "");
      this.updateScrollUsage();
      this.onKeyClick(67, e -> {
         this.doorMode.doorOptionGetter.apply(this.getLevel()).invalidateCache();
         this.updatePath();
         return true;
      }, "Reset cache");
      this.getLevel()
         .hudManager
         .addElement(
            this.hudElement = new HudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, final GameCamera camera, PlayerMob perspective) {
                  list.add(
                     new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return -10000;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           if (RegionPathFindGameTool.this.from != null) {
                              HUD.tileBoundOptions(
                                    camera,
                                    new Color(50, 50, 200),
                                    false,
                                    RegionPathFindGameTool.this.from.x,
                                    RegionPathFindGameTool.this.from.y,
                                    RegionPathFindGameTool.this.from.x,
                                    RegionPathFindGameTool.this.from.y
                                 )
                                 .draw();
                           }

                           if (RegionPathFindGameTool.this.to != null) {
                              HUD.tileBoundOptions(
                                    camera,
                                    new Color(50, 200, 50),
                                    false,
                                    RegionPathFindGameTool.this.to.x,
                                    RegionPathFindGameTool.this.to.y,
                                    RegionPathFindGameTool.this.to.x,
                                    RegionPathFindGameTool.this.to.y
                                 )
                                 .draw();
                           }

                           if (RegionPathFindGameTool.this.result != null) {
                              for (Pathfinding<SubRegion>.Node node : RegionPathFindGameTool.this.result.closedNodes) {
                                 Point p = ((SubRegion)node.item).getAverageLevelTile();
                                 Renderer.drawCircle(camera.getTileDrawX(p.x) + 16, camera.getTileDrawY(p.y) + 16, 8, 10, 1.0F, 0.0F, 0.0F, 1.0F, true);
                              }

                              for (Pathfinding<SubRegion>.Node node : RegionPathFindGameTool.this.result.openNodes) {
                                 Point p = ((SubRegion)node.item).getAverageLevelTile();
                                 Renderer.drawCircle(camera.getTileDrawX(p.x) + 16, camera.getTileDrawY(p.y) + 16, 8, 10, 0.0F, 1.0F, 0.0F, 1.0F, true);
                              }

                              SubRegion last = null;
                              int i = 0;

                              for (Pathfinding<SubRegion>.Node node : RegionPathFindGameTool.this.result.path) {
                                 SubRegion current = (SubRegion)node.item;
                                 if (last != null) {
                                    Color col = Color.getHSBColor((float)i / RegionPathFindGameTool.this.result.path.size(), 1.0F, 1.0F);
                                    Point p1 = last.getAverageLevelTile();
                                    Point p2 = current.getAverageLevelTile();
                                    int x1 = camera.getTileDrawX(p1.x) + 16;
                                    int y1 = camera.getTileDrawY(p1.y) + 16;
                                    int x2 = camera.getTileDrawX(p2.x) + 16;
                                    int y2 = camera.getTileDrawY(p2.y) + 16;
                                    Renderer.drawLineRGBA(x1, y1, x2, y2, col.getRed() / 255.0F, col.getGreen() / 255.0F, col.getBlue() / 255.0F, 1.0F);
                                 }

                                 i++;
                                 last = current;
                              }

                              StringTooltips tooltips = new StringTooltips()
                                 .add("Found: " + RegionPathFindGameTool.this.result.foundTarget)
                                 .add("Iterations: " + RegionPathFindGameTool.this.result.iterations)
                                 .add("Time: " + GameUtils.getTimeStringNano(RegionPathFindGameTool.this.result.nsTaken))
                                 .add("Tile length: " + GameMath.toDecimals(RegionPathFindGameTool.this.pathTileLength, 2))
                                 .add("Cache result: " + RegionPathFindGameTool.this.cacheResult);
                              GameTooltipManager.addTooltip(tooltips, TooltipLocation.PLAYER);
                           }
                        }
                     }
                  );
               }
            }
         );
   }

   public void updateScrollUsage() {
      this.scrollUsage = this.doorMode.displayName;
   }

   public void updatePath() {
      if (this.from != null && this.to != null) {
         PathDoorOption doorOption = this.doorMode.doorOptionGetter.apply(this.getLevel());
         this.result = RegionPathfinding.findMoveToTile(
            this.getLevel(),
            this.from.x,
            this.from.y,
            this.to.x,
            this.to.y,
            doorOption,
            (current, target) -> current.getRegionID() == target.getRegionID(),
            1000
         );
         this.cacheResult = doorOption.canMoveToTile(this.from.x, this.from.y, this.to.x, this.to.y, false);
         this.pathTileLength = RegionPathfinding.estimatePathTileLength(this.result.path);
      } else {
         this.result = null;
      }
   }

   @Override
   public void isCancelled() {
      super.isCancelled();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }
   }

   @Override
   public void isCleared() {
      super.isCleared();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }
   }
}
