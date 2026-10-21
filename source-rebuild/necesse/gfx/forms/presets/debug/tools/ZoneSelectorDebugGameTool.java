package necesse.gfx.forms.presets.debug.tools;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.util.GameUtils;
import necesse.engine.util.PointHashSet;
import necesse.engine.util.Zoning;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.HudDrawElement;

public class ZoneSelectorDebugGameTool extends MouseDebugGameTool {
   private final Object lock = new Object();
   private final LevelSelectTilesGameTool addAreaTool;
   private final LevelSelectTilesGameTool removeAreaTool;
   private final LevelSelectTilesGameTool limitAreaTool;
   private HudDrawElement hudElement;
   private final Zoning zoning = new Zoning(true);
   private PointHashSet edgeTileRange;
   private boolean showRectangles;

   public ZoneSelectorDebugGameTool(DebugForm parent, String name) {
      super(parent, name);
      this.addAreaTool = new LevelSelectTilesGameTool(-100, ControllerInput.MENU_NEXT) {
         @Override
         public Level getLevel() {
            return ZoneSelectorDebugGameTool.this.getLevel();
         }

         @Override
         public void onTileSelection(int tileStartX, int tileStartY, int tileEndX, int tileEndY) {
            ZoneSelectorDebugGameTool.this.edgeTileRange = null;

            for (int x = tileStartX; x <= tileEndX; x++) {
               for (int y = tileStartY; y <= tileEndY; y++) {
                  synchronized (ZoneSelectorDebugGameTool.this.lock) {
                     ZoneSelectorDebugGameTool.this.zoning.addTile(x, y);
                  }
               }
            }
         }

         @Override
         public void drawTileSelection(GameCamera camera, PlayerMob perspective, int tileStartX, int tileStartY, int tileEndX, int tileEndY) {
            Rectangle rectangle = new Rectangle(tileStartX * 32, tileStartY * 32, (tileEndX - tileStartX + 1) * 32, (tileEndY - tileStartY + 1) * 32);
            Zoning.getRectangleDrawOptions(rectangle, new Color(0, 255, 0, 170), new Color(0, 255, 0, 100), camera).draw();
         }

         @Override
         public GameTooltips getTooltips() {
            return ZoneSelectorDebugGameTool.this.getTooltips();
         }
      };
      this.removeAreaTool = new LevelSelectTilesGameTool(-99, ControllerInput.MENU_PREV) {
         @Override
         public Level getLevel() {
            return ZoneSelectorDebugGameTool.this.getLevel();
         }

         @Override
         public void onTileSelection(int tileStartX, int tileStartY, int tileEndX, int tileEndY) {
            ZoneSelectorDebugGameTool.this.edgeTileRange = null;

            for (int x = tileStartX; x <= tileEndX; x++) {
               for (int y = tileStartY; y <= tileEndY; y++) {
                  synchronized (ZoneSelectorDebugGameTool.this.lock) {
                     ZoneSelectorDebugGameTool.this.zoning.removeTile(x, y);
                  }
               }
            }
         }

         @Override
         public void drawTileSelection(GameCamera camera, PlayerMob perspective, int tileStartX, int tileStartY, int tileEndX, int tileEndY) {
            Rectangle rectangle = new Rectangle(tileStartX * 32, tileStartY * 32, (tileEndX - tileStartX + 1) * 32, (tileEndY - tileStartY + 1) * 32);
            Zoning.getRectangleDrawOptions(rectangle, new Color(255, 0, 0, 170), new Color(255, 0, 0, 100), camera).draw();
         }

         @Override
         public GameTooltips getTooltips() {
            return ZoneSelectorDebugGameTool.this.getTooltips();
         }
      };
      this.limitAreaTool = new LevelSelectTilesGameTool(74) {
         @Override
         public Level getLevel() {
            return ZoneSelectorDebugGameTool.this.getLevel();
         }

         @Override
         public int getMouseX() {
            return ZoneSelectorDebugGameTool.this.getMouseX();
         }

         @Override
         public int getMouseY() {
            return ZoneSelectorDebugGameTool.this.getMouseY();
         }

         @Override
         public void onTileSelection(int tileStartX, int tileStartY, int tileEndX, int tileEndY) {
            ZoneSelectorDebugGameTool.this.edgeTileRange = null;
            ZoneSelectorDebugGameTool.this.zoning.limitZoneToTiles(tileStartX, tileEndX, tileStartY, tileEndY);
         }

         @Override
         public void drawTileSelection(GameCamera camera, PlayerMob perspective, int tileStartX, int tileStartY, int tileEndX, int tileEndY) {
            Rectangle rectangle = new Rectangle(tileStartX * 32, tileStartY * 32, (tileEndX - tileStartX + 1) * 32, (tileEndY - tileStartY + 1) * 32);
            Zoning.getRectangleDrawOptions(rectangle, new Color(255, 255, 0, 170), new Color(255, 255, 0, 100), camera).draw();
         }

         @Override
         public GameTooltips getTooltips() {
            return ZoneSelectorDebugGameTool.this.getTooltips();
         }
      };
   }

   @Override
   public void init() {
      this.addAreaTool.init();
      this.removeAreaTool.init();
      this.limitAreaTool.init();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }

      this.getLevel()
         .hudManager
         .addElement(
            this.hudElement = new HudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
                  final SharedTextureDrawOptions options = new SharedTextureDrawOptions(Renderer.getQuadTexture());
                  if (ZoneSelectorDebugGameTool.this.showRectangles) {
                     float hue = 0.0F;
                     synchronized (ZoneSelectorDebugGameTool.this.lock) {
                        for (Rectangle rectangle : ZoneSelectorDebugGameTool.this.zoning.getTileRectangles()) {
                           Color col = Color.getHSBColor(hue, 1.0F, 1.0F);
                           Color edgeColor = new Color(col.getRed(), col.getGreen(), col.getBlue(), 170);
                           Color fillColor = new Color(col.getRed(), col.getGreen(), col.getBlue(), 100);
                           Zoning.addRectangleDrawOptions(
                              options,
                              new Rectangle(rectangle.x * 32, rectangle.y * 32, rectangle.width * 32, rectangle.height * 32),
                              edgeColor,
                              fillColor,
                              camera
                           );
                           hue = (hue + 0.1F) % 1.0F;
                        }
                     }
                  } else {
                     synchronized (ZoneSelectorDebugGameTool.this.lock) {
                        for (Point tile : ZoneSelectorDebugGameTool.this.zoning.getTiles()) {
                           boolean[] adjacent = new boolean[Level.adjacentGetters.length];

                           for (int i = 0; i < adjacent.length; i++) {
                              Point offset = Level.adjacentGetters[i];
                              adjacent[i] = ZoneSelectorDebugGameTool.this.zoning.containsTile(tile.x + offset.x, tile.y + offset.y);
                           }

                           Zoning.addDrawOptions(options, tile, adjacent, new Color(0, 0, 255, 170), new Color(0, 0, 255, 100), camera);
                        }
                     }
                  }

                  PointHashSet edgeTileRange = ZoneSelectorDebugGameTool.this.edgeTileRange;
                  if (edgeTileRange != null) {
                     synchronized (ZoneSelectorDebugGameTool.this.lock) {
                        for (Point tile : edgeTileRange) {
                           boolean[] adjacent = new boolean[Level.adjacentGetters.length];

                           for (int i = 0; i < adjacent.length; i++) {
                              Point offset = Level.adjacentGetters[i];
                              adjacent[i] = edgeTileRange.contains(tile.x + offset.x, tile.y + offset.y);
                           }

                           Zoning.addDrawOptions(options, tile, adjacent, new Color(255, 255, 0, 170), new Color(255, 255, 0, 100), camera);
                        }
                     }
                  }

                  list.add(new SortedDrawable() {
                     @Override
                     public int getPriority() {
                        return -10000;
                     }

                     @Override
                     public void draw(TickManager tickManager) {
                        options.draw();
                     }
                  });
               }
            }
         );
      this.onKeyClick(71, e -> {
         this.showRectangles = !this.showRectangles;
         return true;
      }, "Toggle rectangles");
      this.onKeyClick(
         72,
         e -> {
            if (this.edgeTileRange != null) {
               this.edgeTileRange = null;
            } else if (!this.zoning.isEmpty()) {
               synchronized (this.lock) {
                  long time = System.nanoTime();
                  this.edgeTileRange = Zoning.calculateAtDistanceTiles(this.zoning.getEdgeTiles(), p -> !this.zoning.containsTile(p.x, p.y), 35, 35);
                  System.out
                     .println(
                        "Calculated edge tiles range took "
                           + GameUtils.getTimeStringNano(System.nanoTime() - time)
                           + ", Size: "
                           + this.edgeTileRange.size()
                           + ", "
                           + this.zoning.getEdgeTiles().size()
                     );
               }
            }

            return true;
         },
         "Calculate tile range"
      );
      this.setLeftUsage("Add area");
      this.setRightUsage("Remove area");
      this.setKeyUsage(74, "Limit area");
   }

   @Override
   public boolean inputEvent(InputEvent event) {
      if (this.addAreaTool.inputEvent(event)) {
         return true;
      } else if (this.removeAreaTool.inputEvent(event)) {
         return true;
      } else {
         return this.limitAreaTool.inputEvent(event) ? true : super.inputEvent(event);
      }
   }

   @Override
   public void isCancelled() {
      this.addAreaTool.isCancelled();
      this.removeAreaTool.isCancelled();
      this.limitAreaTool.isCancelled();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }
   }

   @Override
   public void isCleared() {
      this.addAreaTool.isCleared();
      this.removeAreaTool.isCleared();
      this.limitAreaTool.isCleared();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }
   }
}
