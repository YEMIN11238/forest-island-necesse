package necesse.engine.network.client;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.Server;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.PointHashMap;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldPresets.LevelPresetsRegion;
import necesse.engine.world.worldPresets.WorldPresetsRegion;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.sidebar.ShowPresetRegionSidebarForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.HUD;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.MapHudDrawElement;
import necesse.level.maps.mapData.ClientDiscoveredMap;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.managers.RegionFilesManager;

public class ClientDebugMapHudDrawElement extends MapHudDrawElement {
   public final Client client;
   public final Level level;

   public ClientDebugMapHudDrawElement(Client client, Level level) {
      this.client = client;
      this.level = level;
   }

   @Override
   public void addDrawables(List<SortedDrawable> list, GameCamera camera, PlayerMob perspective) {
   }

   @Override
   public boolean shouldDrawOnMap(Client client, ClientDiscoveredMap map) {
      return HUD.showRegionBounds || HUD.showWorldPresetRegionBounds;
   }

   @Override
   public Rectangle getMapLevelDrawBounds() {
      return null;
   }

   @Override
   public Point getMapLevelPos() {
      PlayerMob player = this.client.getPlayer();
      return player != null ? new Point(player.getX(), player.getY()) : new Point(0, 0);
   }

   @Override
   public void drawOnMap(
      TickManager tickManager, Client client, int drawX, int drawY, double tileScale, Rectangle drawBounds, boolean isMinimap, int mouseHudX, int mouseHudY
   ) {
      PlayerMob player = client.getPlayer();
      if (player != null) {
         Level serverLevel = null;
         Server server = client.getLocalServer();
         if (server != null) {
            serverLevel = server.world.getLevel(this.getLevel().getIdentifier());
         }

         if (HUD.showRegionBounds) {
            int regionX = GameMath.getRegionCoordByTile(player.getTileX());
            int regionY = GameMath.getRegionCoordByTile(player.getTileY());
            int worldRegionX = RegionFilesManager.getWorldRegionPos(regionX);
            int worldRegionY = RegionFilesManager.getWorldRegionPos(regionY);
            int worldRegionTileSize = 1024;
            Rectangle tileRectangle = new Rectangle(
               worldRegionX * worldRegionTileSize + 512, worldRegionY * worldRegionTileSize + 512, worldRegionTileSize, worldRegionTileSize
            );
            this.drawRectangle(tileRectangle, player.getX(), player.getY(), drawX, drawY, drawBounds, tileScale, new Color(255, 255, 255), 0, null);
            if (serverLevel != null && Settings.serverPerspective) {
               SettlementsWorldData settlements = SettlementsWorldData.getSettlementsData(serverLevel);
               Collection<Region> loadedRegions = serverLevel.regionManager.collectLoadedRegions();
               Iterable<Point> regions = GameUtils.mapIterable(loadedRegions.iterator(), r -> new Point(r.regionX, r.regionY));
               this.drawRegions(
                  serverLevel,
                  regions,
                  settlements,
                  player.getX(),
                  player.getY(),
                  drawX,
                  drawY,
                  drawBounds,
                  tileScale,
                  new Color(0, 255, 0, 100),
                  new Color(255, 0, 0, 100),
                  0
               );
               if (mouseHudX != Integer.MIN_VALUE && mouseHudY != Integer.MIN_VALUE) {
                  int mouseTileX = (int)((mouseHudX - drawX) / tileScale) + player.getTileX();
                  int mouseTileY = (int)((mouseHudY - drawY) / tileScale) + player.getTileY();
                  int mouseRegionX = GameMath.getRegionCoordByTile(mouseTileX);
                  int mouseRegionY = GameMath.getRegionCoordByTile(mouseTileY);
                  if (!serverLevel.regionManager.isRegionLoaded(mouseRegionX, mouseRegionY)) {
                     Rectangle rectangle = new Rectangle(
                        this.level.regionManager.getTileCoordByRegion(mouseRegionX), this.level.regionManager.getTileCoordByRegion(mouseRegionY), 16, 16
                     );
                     boolean generated = serverLevel.regionManager.isRegionGenerated(mouseRegionX, mouseRegionY);
                     String string = "Region: " + mouseRegionX + "x" + mouseRegionY + "\nNot loaded\nGenerated: " + generated;
                     this.drawRectangle(rectangle, player.getX(), player.getY(), drawX, drawY, drawBounds, tileScale, new Color(255, 0, 255, 100), 0, string);
                     FontManager.bit.drawString(drawX, drawY, "MOUSE: " + mouseTileX + ", " + mouseTileY, new FontOptions(16).outline());
                  }
               }
            } else {
               ClientLevelLoading loading = client.levelManager.loading();
               SettlementsWorldData settlements = SettlementsWorldData.getSettlementsData(client);
               this.drawRegions(
                  this.level,
                  loading.getLoadedRegions(),
                  settlements,
                  player.getX(),
                  player.getY(),
                  drawX,
                  drawY,
                  drawBounds,
                  tileScale,
                  new Color(0, 255, 0, 100),
                  new Color(255, 0, 0, 100),
                  0
               );
               this.drawRegions(
                  this.level,
                  loading.getRequestedRegions(),
                  settlements,
                  player.getX(),
                  player.getY(),
                  drawX,
                  drawY,
                  drawBounds,
                  tileScale,
                  new Color(0, 255, 255, 100),
                  new Color(0, 0, 255, 100),
                  2
               );
               this.drawRegions(
                  this.level,
                  loading.getQueuedRegions(),
                  settlements,
                  player.getX(),
                  player.getY(),
                  drawX,
                  drawY,
                  drawBounds,
                  tileScale,
                  new Color(255, 255, 0, 100),
                  new Color(255, 0, 255, 100),
                  4
               );
            }
         }

         if (HUD.showWorldPresetRegionBounds) {
            int regionX = GameMath.getRegionCoordByTile(player.getTileX());
            int regionY = GameMath.getRegionCoordByTile(player.getTileY());
            int presetRegionX = WorldPresetsRegion.getWorldPresetsRegionFromLevelRegion(regionX);
            int presetRegionY = WorldPresetsRegion.getWorldPresetsRegionFromLevelRegion(regionY);
            int presetRegionTileSize = 1024;
            Rectangle tileRectangle = new Rectangle(
               presetRegionX * presetRegionTileSize, presetRegionY * presetRegionTileSize, presetRegionTileSize, presetRegionTileSize
            );
            this.drawRectangle(tileRectangle, player.getX(), player.getY(), drawX, drawY, drawBounds, tileScale, new Color(255, 255, 255), 0, null);
            if (serverLevel != null) {
               WorldPresetsRegion worldPresetsRegion = serverLevel.getWorldEntity().getWorldPresets(regionX, regionY);
               LevelPresetsRegion levelPresetsRegion = worldPresetsRegion.getLevelRegions(serverLevel.getIdentifier(), 0);
               PointHashMap<Integer> regionDrawCounts = new PointHashMap<>();
               HashMap<Integer, Integer> generationUniqueIDsFound = new HashMap<>();
               String searchFilter = ShowPresetRegionSidebarForm.searchFilter.toLowerCase();

               for (LevelPresetsRegion.PresetDebugData data : levelPresetsRegion.getDebugData()) {
                  String name = data.getDebugName();
                  if (searchFilter.isEmpty() || name.toLowerCase().contains(searchFilter)) {
                     int generationUniqueID = data.getGenerationRegionUniqueID();
                     int generationUniqueIDIndex = generationUniqueIDsFound.compute(
                        data.getGenerationRegionUniqueID(), (id, last) -> last == null ? generationUniqueIDsFound.size() : last
                     );
                     Color color = Color.getHSBColor(generationUniqueIDIndex * 50.0F % 360.0F / 360.0F, 1.0F, 1.0F);
                     this.drawRegions(
                        this.level,
                        data.getOccupiedRegions(),
                        player.getX(),
                        player.getY(),
                        drawX,
                        drawY,
                        drawBounds,
                        tileScale,
                        color,
                        color,
                        0,
                        (regionPos, region) -> {
                           int drawCount = regionDrawCounts.compute(regionPos.x, regionPos.y, (x, y, last) -> last == null ? 0 : last + 1);
                           StringBuilder stringBuilder = new StringBuilder("Region: " + regionPos.x + "x" + regionPos.y + "\n");
                           stringBuilder.append("Generation: ").append(generationUniqueID).append("\n");

                           for (int i = 0; i < drawCount; i++) {
                              stringBuilder.append("\n");
                           }

                           stringBuilder.append(name);
                           return stringBuilder.toString();
                        }
                     );

                     for (Rectangle rectangle : data.getOccupiedTileRectangles()) {
                        this.drawRectangle(rectangle, player.getX(), player.getY(), drawX, drawY, drawBounds, tileScale, new Color(255, 255, 255), 2, name);
                     }
                  }
               }
            }
         }
      }
   }

   protected void drawRegions(
      Level level,
      Iterable<Point> regionPositions,
      SettlementsWorldData settlementsData,
      int playerX,
      int playerY,
      int drawX,
      int drawY,
      Rectangle drawBounds,
      double tileScale,
      Color foundColor,
      Color notFoundColor,
      int padding
   ) {
      this.drawRegions(
         level,
         regionPositions,
         playerX,
         playerY,
         drawX,
         drawY,
         drawBounds,
         tileScale,
         foundColor,
         notFoundColor,
         padding,
         (regionPos, region) -> {
            if (region == null) {
               String string = regionPos.x + "x" + regionPos.y + "\nNot loaded";
               if (level.isServer()) {
                  string = string + "\nGenerated: " + level.regionManager.isRegionGenerated(regionPos.x, regionPos.y);
               }

               return string;
            } else {
               int settlementUniqueID = settlementsData.getSettlementUniqueIDAtRegion(level.getIdentifier(), regionPos.x, regionPos.y);
               return "Region: "
                  + regionPos.x
                  + "x"
                  + regionPos.y
                  + "\nTile: "
                  + region.tileXOffset
                  + "x"
                  + region.tileYOffset
                  + "\nSize: "
                  + region.tileWidth
                  + "x"
                  + region.tileHeight
                  + "\nUnload: "
                  + region.unloadRegionBuffer.getBuffer()
                  + "\nSettlement: \n"
                  + settlementUniqueID;
            }
         }
      );
   }

   protected void drawRegions(
      Level level,
      Iterable<Point> regionPositions,
      int playerX,
      int playerY,
      int drawX,
      int drawY,
      Rectangle drawBounds,
      double tileScale,
      Color foundColor,
      Color notFoundColor,
      int padding,
      BiFunction<Point, Region, String> getText
   ) {
      for (Point regionPos : regionPositions) {
         Region region = level.regionManager.getRegion(regionPos.x, regionPos.y, false);
         String text = getText == null ? null : getText.apply(regionPos, region);
         if (region != null) {
            Rectangle rectangle = new Rectangle(region.tileXOffset, region.tileYOffset, region.tileWidth, region.tileHeight);
            this.drawRectangle(rectangle, playerX, playerY, drawX, drawY, drawBounds, tileScale, foundColor, padding, text);
         } else {
            Rectangle rectangle = new Rectangle(
               level.regionManager.getTileCoordByRegion(regionPos.x), level.regionManager.getTileCoordByRegion(regionPos.y), 16, 16
            );
            this.drawRectangle(rectangle, playerX, playerY, drawX, drawY, drawBounds, tileScale, notFoundColor, padding, text);
         }
      }
   }

   protected void drawRectangle(
      Rectangle rectangle, int playerX, int playerY, int drawX, int drawY, Rectangle drawBounds, double tileScale, Color color, int padding, String text
   ) {
      float playerTileX = GameMath.getTileFloatCoordinate(playerX);
      float playerTileY = GameMath.getTileFloatCoordinate(playerY);
      Rectangle drawRectangle = new Rectangle(
         (int)(rectangle.x * tileScale - playerTileX * tileScale) + padding + drawX,
         (int)(rectangle.y * tileScale - playerTileY * tileScale) + padding + drawY,
         (int)(rectangle.width * tileScale) - padding * 2,
         (int)(rectangle.height * tileScale) - padding * 2
      );
      if (drawBounds.intersects(drawRectangle)) {
         Renderer.drawRectangleLines(drawRectangle, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
         if (text != null) {
            int fontSize = Math.min(12, (int)(1.5 * tileScale));
            if (fontSize > 0) {
               String[] split = text.split("\n");

               for (int i = 0; i < split.length; i++) {
                  FontManager.bit.drawString(drawRectangle.x + 4, drawRectangle.y + 4 + i * fontSize, split[i], new FontOptions(fontSize).outline());
               }
            }
         }
      }
   }
}
