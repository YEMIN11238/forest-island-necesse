/*
 * Decompiled with CFR 0.152.
 */
package necesse.gfx.ui;

import java.awt.geom.Point2D.Double;

import java.awt.geom.Line2D.Float;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameRandomNoise;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.client.ClientClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.state.State;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.IntersectionPoint;
import necesse.engine.util.PointSetAbstract;
import necesse.engine.util.pathfinding.PathResult;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldPresets.WorldPresetsRegion;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.path.TilePathfinding;
import necesse.entity.mobs.attackHandler.AttackHandler;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.pickup.PickupEntity;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.PositionedDrawOptionsBox;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.GLDrawOptionsList;
import necesse.gfx.drawables.QuadDrawOptionsList;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ControllerInteractTarget;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.level.gameLogicGate.GameLogicGate;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectHoverHitbox;
import necesse.level.maps.CollisionPoint;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.levelData.CursedCroneArenasLevelData;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.OneWorldPirateVillageData;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.light.SourcedGameLight;
import necesse.level.maps.light.SourcedLightModifier;
import necesse.level.maps.managers.BiomeBlendingManager;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionType;
import necesse.level.maps.regionSystem.SubRegion;
import necesse.level.maps.regionSystem.layers.BiomeBlendingValue;
import org.lwjgl.opengl.GL11;

public class HUD {
    public static boolean debugActive;
    public static DebugShow debugShow;
    public static boolean showRegionBounds;
    public static boolean showWorldPresetRegionBounds;
    private static final LinkedList<SubmittedPath> paths;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void draw(Level var0, GameCamera var1, PlayerMob var2, TickManager var3) {
      State var4 = GlobalData.getCurrentState();
      GameWindow var5 = WindowManager.getWindow();
      int var6 = var1.getMouseLevelPosX();
      int var7 = var1.getMouseLevelPosY();
      int var8 = var1.getMouseLevelTilePosX();
      int var9 = var1.getMouseLevelTilePosY();
      if (var0.isClient()) {
         ClientClient var10 = var0.getClient().getClient();
         if (var10 != null) {
            Mob var11 = var10.playerMob.getSummonFocusMob();
            if (var11 != null) {
               Rectangle var12 = var11.getSelectBox();
               int var13 = 16;
               GameResources.particles
                  .sprite(0, 0, 8)
                  .initDraw()
                  .size(var13, var13)
                  .color(new Color(250, 50, 50))
                  .rotate((float)var0.getWorldEntity().getLocalTime() / 5.0F, var13 / 2, var13 / 2)
                  .draw(var1.getDrawX(var12.x + var12.width / 2) - var13 / 2, var1.getDrawY(var12.y + var12.height / 2) - var13 / 2);
            }
         }
      }

      synchronized (paths) {
         paths.removeIf(var1x -> var1x.time + 5000L < var0.getWorldEntity().getLocalTime());
      }

      switch (debugShow) {
         case NOTHING:
         default:
            break;
         case REGIONS:
            Region var53 = var0.regionManager.getRegionByTile(var8, var9, false);
            StringTooltips var70 = new StringTooltips("Region:");
            if (var53 != null) {
               var70.add("Pos: " + var53.regionX + ", " + var53.regionY + " (" + var53.tileXOffset + ", " + var53.tileYOffset + ")");
               var70.add("Dim: " + var53.tileWidth + ", " + var53.tileHeight);
               GameTooltipManager.addTooltip(var70, TooltipLocation.BOTTOM_LEFT);
               SubRegion var87 = var53.subRegionData.getSubRegionByRegion(var8 - var53.tileXOffset, var9 - var53.tileYOffset);
               if (var87 != null) {
                  drawCells(var87, new Color(255, 255, 0), var0, var1);

                  for (SubRegion var136 : var87.getAdjacentRegions()) {
                     drawCells(var136, new Color(0, 0, 255), var0, var1);
                  }

                  StringTooltips var108 = new StringTooltips("Subregion:");
                  var108.add("Type: " + var87.getType());
                  var108.add("Region ID: " + var87.getRegionID());
                  var108.add("Room ID: " + var87.getRoomID());
                  var108.add("Size: " + var87.size());
                  var108.add("Listeners: " + var87.getListenersSize());
                  GameTooltipManager.addTooltip(var108, TooltipLocation.BOTTOM_LEFT);
               }
            } else {
               var70.add("No region found");
            }
            break;
         case CONNECTED_REGIONS:
            int var52 = var0.regionManager.getRegionCoordByTile(var1.getStartTileX());
            int var69 = var0.regionManager.getRegionCoordByTile(var1.getStartTileY());
            int var86 = var0.regionManager.getRegionCoordByTile(var1.getEndTileX());
            int var106 = var0.regionManager.getRegionCoordByTile(var1.getEndTileY());
            ConnectedSubRegionsResult var135 = var0.regionManager.getTypeConnectedByTile(var8, var9, Integer.MAX_VALUE);
            StringTooltips var163 = new StringTooltips();
            if (var135 == null) {
               var163.add("No region found");
            } else {
               for (SubRegion var212 : var135.connectedRegions) {
                  if (var212.region.regionX >= var52 && var212.region.regionX <= var86 && var212.region.regionY >= var69 && var212.region.regionY <= var106) {
                     drawCells(var212, new Color(255, 255, 0), var0, var1);
                  }
               }

               var163.add("Region ID: " + var135.base.getRegionID());
               var163.add("Region type: " + var135.base.getType());
               var163.add("Connected size: " + var135.size);
            }

            GameTooltipManager.addTooltip(var163, TooltipLocation.BOTTOM_LEFT);
            break;
         case ROOMS:
            int var51 = var0.regionManager.getRegionCoordByTile(var1.getStartTileX());
            int var68 = var0.regionManager.getRegionCoordByTile(var1.getStartTileY());
            int var85 = var0.regionManager.getRegionCoordByTile(var1.getEndTileX());
            int var105 = var0.regionManager.getRegionCoordByTile(var1.getEndTileY());
            ConnectedSubRegionsResult var134 = var0.regionManager.getRoomConnectedByTile(var8, var9, false, Integer.MAX_VALUE);
            StringTooltips var162 = new StringTooltips();
            if (var134 == null) {
               var162.add("No room found");
            } else {
               for (SubRegion var211 : var134.connectedRegions) {
                  if (var211.region.regionX >= var51 && var211.region.regionX <= var85 && var211.region.regionY >= var68 && var211.region.regionY <= var105) {
                     drawCells(var211, new Color(255, 255, 0), var0, var1);
                  }
               }

               var162.add("Room ID: " + var134.base.getRoomID());
               var162.add("Room type: " + var134.base.getType());
               var162.add("Room size: " + var134.size + " (" + var0.regionManager.getRoomSize(var134.base.getRoomID()) + ")");
            }

            GameTooltipManager.addTooltip(var162, TooltipLocation.BOTTOM_LEFT);
            break;
         case HOUSE:
            int var50 = var0.regionManager.getRegionCoordByTile(var1.getStartTileX());
            int var67 = var0.regionManager.getRegionCoordByTile(var1.getStartTileY());
            int var84 = var0.regionManager.getRegionCoordByTile(var1.getEndTileX());
            int var104 = var0.regionManager.getRegionCoordByTile(var1.getEndTileY());
            ConnectedSubRegionsResult var133 = var0.regionManager.getHouseConnectedByTile(var8, var9, Integer.MAX_VALUE);
            StringTooltips var161 = new StringTooltips();
            if (var133 == null) {
               var161.add("No house found");
            } else {
               int var187 = 0;
               int var210 = 0;

               for (SubRegion var250 : var133.connectedRegions) {
                  if (var250.region.regionX >= var50 && var250.region.regionX <= var84 && var250.region.regionY >= var67 && var250.region.regionY <= var104) {
                     drawCells(var250, new Color(255, 255, 0), var0, var1);
                  }

                  if (var250.getType() == RegionType.DOOR) {
                     var187++;
                     if (var250.streamAdjacentRegions().anyMatch(SubRegion::isOutside)) {
                        var210++;
                     }
                  }
               }

               int[] var234 = var133.connectedRegions.stream().mapToInt(SubRegion::getRoomID).distinct().toArray();
               var161.add("House room IDs: " + Arrays.toString(var234));
               var161.add("House doors: " + var187 + ", leading out: " + var210);
               int var251 = Arrays.stream(var234).map(var0.regionManager::getRoomSize).sum();
               var161.add("House size: " + var251 + " (" + var133.size + ")");
            }

            GameTooltipManager.addTooltip(var161, TooltipLocation.BOTTOM_LEFT);
            break;
         case PATHS:
            synchronized (paths) {
               paths.stream().filter(var1x -> var1x.path.finder.level.isSamePlace(var0)).forEach(var1x -> TilePathfinding.drawPathLine(var1x.path.path, var1));
               break;
            }
         case HEIGHT:
            SharedTextureDrawOptions var48 = new SharedTextureDrawOptions(GameResources.empty);
            int var66 = var1.getStartTileX();
            int var83 = var1.getStartTileY();
            int var103 = var1.getEndTileX();
            int var132 = var1.getEndTileY();

            for (int var160 = var66; var160 <= var103; var160++) {
               for (int var186 = var83; var186 <= var132; var186++) {
                  int var209 = var1.getTileDrawX(var160);
                  int var232 = var1.getTileDrawY(var186);
                  int var249 = var0.liquidManager.getHeight(var160, var186);
                  float var261 = var249 >= 0 ? var249 / 10.0F : 1.0F;
                  float var267 = var249 <= 0 ? var249 / -10.0F : 1.0F;
                  var48.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(var261, var267, 1.0F, 0.5F).pos(var209, var232);
               }
            }

            var48.draw();
            break;
         case ADVANCED_HEIGHT:
            ArrayList<Region> var47 = new ArrayList<>(var0.regionManager.getLoadedRegionsSize());
            var0.regionManager.forEachLoadedRegions(var47::add);
            if (!var47.isEmpty()) {
               int var65 = Integer.MAX_VALUE;
               int var82 = Integer.MAX_VALUE;
               int var102 = Integer.MAX_VALUE;
               int var131 = Integer.MAX_VALUE;

               for (Region var184 : var47) {
                  var65 = Math.min(var65, var184.tileXOffset);
                  var82 = Math.min(var82, var184.tileYOffset);
                  var102 = Math.min(var102, var184.regionX);
                  var131 = Math.min(var131, var184.regionY);
               }

               for (Region var185 : var47) {
                  GameTexture var208 = var185.liquidData.getSmoothTexture();
                  if (var208 != null) {
                     int var231 = var185.tileXOffset - var65;
                     int var248 = var185.tileYOffset - var82;
                     var208.initDraw().section(1, var208.getWidth() - 1, 1, var208.getHeight() - 1).draw(var231 + 200, var248 + 200);
                  }
               }
            }

            var0.liquidManager.getAdvancedMobHeightPercentDrawTest(var2, var1, var6 - 100, var7 - 100, 200, 200).draw();
            GameTooltipManager.addTooltip(var0.liquidManager.getAdvancedHeightTooltips(var2, var6, var7), TooltipLocation.INTERACT_FOCUS);
            break;
         case VILLAGE_TILES:
            if (var0.isServer()) {
               OneWorldNPCVillageData var46 = OneWorldNPCVillageData.getVillageData(var0, false);
               OneWorldPirateVillageData var64 = OneWorldPirateVillageData.getPirateVillageData(var0, false);
               SharedTextureDrawOptions var81 = new SharedTextureDrawOptions(GameResources.empty);
               int var101 = var1.getStartTileX();
               int var130 = var1.getStartTileY();
               int var157 = var1.getEndTileX();
               int var183 = var1.getEndTileY();

               for (int var207 = var101; var207 <= var157; var207++) {
                  for (int var230 = var130; var230 <= var183; var230++) {
                     int var247 = var1.getTileDrawX(var207);
                     int var260 = var1.getTileDrawY(var230);
                     if (var46 != null && var46.isVillageTile(var207, var230)) {
                        var81.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(new Color(255, 255, 0)).alpha(0.4F).pos(var247, var260);
                     }

                     if (var64 != null && var64.isPirateTile(var207, var230)) {
                        var81.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(new Color(255, 0, 0)).alpha(0.4F).pos(var247, var260);
                     }
                  }
               }

               var81.draw();
            } else {
               FontManager.bit.drawString(100.0F, 100.0F, "Village tiles only work on server levels", new FontOptions(32).outline());
            }
            break;
         case CURSED_ARENA_TILES:
            if (var0.isServer()) {
               CursedCroneArenasLevelData var45 = CursedCroneArenasLevelData.getCursedCroneArenasData(var0, false);
               if (var45 != null) {
                  SharedTextureDrawOptions var63 = new SharedTextureDrawOptions(GameResources.empty);
                  int var80 = var1.getStartTileX();
                  int var100 = var1.getStartTileY();
                  int var129 = var1.getEndTileX();
                  int var156 = var1.getEndTileY();

                  for (int var182 = var80; var182 <= var129; var182++) {
                     for (int var206 = var100; var206 <= var156; var206++) {
                        int var229 = var1.getTileDrawX(var182);
                        int var246 = var1.getTileDrawY(var206);
                        if (var45.isArenaTile(var182, var206)) {
                           var63.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(new Color(255, 0, 0)).alpha(0.4F).pos(var229, var246);
                        }
                     }
                  }

                  var63.draw();
               } else {
                  FontManager.bit.drawString(100.0F, 100.0F, "No village data found", new FontOptions(32).outline());
               }
            } else {
               FontManager.bit.drawString(100.0F, 100.0F, "Arena tiles only work on server levels", new FontOptions(32).outline());
            }
            break;
         case WATER_TYPE:
            SharedTextureDrawOptions var44 = new SharedTextureDrawOptions(GameResources.empty);
            int var62 = var1.getStartTileX();
            int var79 = var1.getStartTileY();
            int var99 = var1.getEndTileX();
            int var128 = var1.getEndTileY();

            for (int var155 = var62; var155 <= var99; var155++) {
               for (int var181 = var79; var181 <= var128; var181++) {
                  int var205 = var1.getTileDrawX(var155);
                  int var228 = var1.getTileDrawY(var181);
                  if (var0.getTile(var155, var181).isLiquid) {
                     int var245 = var0.liquidManager.getSaltWaterDepth(var155, var181);
                     float var259 = var245 >= 0 ? var245 / 11.0F : 1.0F;
                     var44.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(1.0F, 1.0F - var259, 1.0F, 0.5F).pos(var205, var228);
                     boolean var266 = var0.liquidManager.isSaltWater(var155, var181);
                     Color var272;
                     if (var266) {
                        if (var245 >= 11) {
                           var272 = new Color(255, 0, 0);
                        } else {
                           var272 = new Color(255, 100, 0);
                        }
                     } else {
                        var272 = new Color(100, 255, 100);
                     }

                     var44.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(var272).alpha(0.4F).pos(var205, var228);
                  }
               }
            }

            var44.draw();
            break;
         case BIOME_BLENDING:
            Biome var43 = var0.getBiome(var8, var9);
            FontManager.bit.drawString(100.0F, 100.0F, "Showing biome blending for: " + var43.getDisplayName(), new FontOptions(20).outline());
            SharedTextureDrawOptions var61 = new SharedTextureDrawOptions(GameResources.empty);
            int var78 = var1.getStartTileX();
            int var98 = var1.getStartTileY();
            int var127 = var1.getEndTileX();
            int var154 = var1.getEndTileY();

            for (int var180 = var78; var180 <= var127; var180++) {
               for (int var204 = var98; var204 <= var154; var204++) {
                  int var227 = var1.getTileDrawX(var180);
                  int var244 = var1.getTileDrawY(var204);

                  for (BiomeBlendingValue var265 : var0.biomeBlendingManager.getBlendValues(var180, var204)) {
                     if (var265.biomeID == var43.getID()) {
                        float var271 = (float)var265.value / BiomeBlendingManager.MAX_VALUE;
                        var61.add(new GameTextureSection(GameResources.empty)).size(32, 32).color(var271, var271, var271, 0.5F).pos(var227, var244);
                     }
                  }
               }
            }

            var61.draw();
            break;
         case WIND_OVERLAY:
            QuadDrawOptionsList var41 = new QuadDrawOptionsList();
            int var59 = var1.getStartTileX();
            int var76 = var1.getStartTileY();
            int var96 = var1.getEndTileX();
            int var125 = var1.getEndTileY();

            for (int var152 = var59; var152 <= var96; var152++) {
               for (int var178 = var76; var178 <= var125; var178++) {
                  int var202 = var1.getTileDrawX(var152);
                  int var225 = var1.getTileDrawY(var178);
                  float var242 = var0.weatherLayer.getWindAmount(var152, var178);
                  if (var242 < 0.0F) {
                     var41.add(var202, var225, 32, 32, -var242, 0.0F, 0.0F, 0.4F);
                  } else {
                     var41.add(var202, var225, 32, 32, 0.0F, var242, 0.0F, 0.4F);
                  }
               }
            }

            var41.draw();
         case WIND_STATS:
            int var42 = var0.limitTileXToBounds(var1.getStartTileX() - 100);
            int var60 = var0.limitTileYToBounds(var1.getStartTileY() - 100);
            int var77 = var0.limitTileXToBounds(var1.getEndTileX() + 100);
            int var97 = var0.limitTileYToBounds(var1.getEndTileY() + 100);
            int var126 = var77 - var42;
            int var153 = var97 - var60;
            GameRandomNoise.get2DebugDrawFull(100, 100, var126, var153, var3x -> var0.weatherLayer.getWindAmount(var42 + var3x.x, var60 + var3x.y)).draw();
            int var179 = var1.getStartTileX() - var42;
            int var203 = var1.getStartTileY() - var60;
            int var226 = var1.getEndTileX() - var1.getStartTileX();
            int var243 = var1.getEndTileY() - var1.getStartTileY();
            Renderer.drawRectangleLines(new Rectangle(var179, var203, var226, var243), 100.0F, 100.0F, 1.0F, 1.0F, 1.0F, 0.5F);
            if (var2 != null) {
               int var256 = var2.getTileX() - var42;
               int var21 = var2.getTileY() - var60;
               Renderer.drawCircle(100 + var256, 100 + var21, 5, 10, 1.0F, 1.0F, 1.0F, 1.0F, true);
               Renderer.drawCircle(100 + var256, 100 + var21, 4, 10, 0.0F, 0.0F, 1.0F, 1.0F, true);
               Renderer.drawCircle(100 + var126 + 8, 107, 5, 10, 1.0F, 1.0F, 1.0F, 1.0F, true);
               Renderer.drawCircle(100 + var126 + 8, 107, 4, 10, 0.0F, 0.0F, 1.0F, 1.0F, true);
               FontManager.bit.drawString(100 + var126 + 15, 100.0F, "You", new FontOptions(16).outline());
            }

            int var257 = 100 + var179 + var226 / 2;
            int var264 = 100 + var203 + var243 / 2;
            Renderer.drawCircle(var257, var264, 40, 20, 1.0F, 1.0F, 1.0F, 0.5F, false);
            Renderer.drawCircle(var257, var264, 2, 5, 1.0F, 1.0F, 1.0F, 1.0F, true);
            Double var22 = var0.weatherLayer.getWindDirFull();
            Renderer.drawLineRGBA(var257, var264, var257 + (int)(var22.x * 40.0), var264 + (int)(var22.y * 40.0), 1.0F, 1.0F, 1.0F, 1.0F);
            FontManager.bit.drawString(100.0F, 100 + var153 + 5, "Wind speed: " + var0.weatherLayer.getWindSpeedFull(), new FontOptions(16).outline());
            var0.weatherLayer.getOffsetXDebugDrawOptions(100, 150 + var153, 300, 100, 500000L).draw();
            var0.weatherLayer.getOffsetYDebugDrawOptions(100, 150 + var153 + 150, 300, 100, 500000L).draw();
            break;
         case HOSTILE_SPAWN_OVERLAY:
            GLDrawOptionsList var40 = new GLDrawOptionsList();
            Mob var58 = MobRegistry.getMob("zombie", var0);
            int var75 = var1.getStartTileX();
            int var95 = var1.getStartTileY();
            int var124 = var1.getEndTileX();
            int var151 = var1.getEndTileY();

            for (int var177 = var75; var177 <= var124; var177++) {
               for (int var201 = var95; var201 <= var151; var201++) {
                  int var224 = var1.getTileDrawX(var177);
                  int var19 = var1.getTileDrawY(var201);
                  boolean var20 = new MobSpawnLocation(var58, var177 * 32 + 16, var201 * 32 + 16).checkStaticLightThreshold(null).validAndApply();
                  if (var20) {
                     var40.add(var224, var19, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224 + 32, var19, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224 + 32, var19, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224 + 32, var19 + 32, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224 + 32, var19 + 32, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224, var19 + 32, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224, var19 + 32, 1.0F, 0.0F, 0.0F, 1.0F);
                     var40.add(var224, var19, 1.0F, 0.0F, 0.0F, 1.0F);
                  }
               }
            }

            var40.draw(1);
            break;
         case INSIDE_LIGHT_MODIFIER:
            QuadDrawOptionsList var39 = new QuadDrawOptionsList();
            int var57 = var1.getStartTileX();
            int var74 = var1.getStartTileY();
            int var94 = var1.getEndTileX();
            int var14 = var1.getEndTileY();

            for (int var15 = var57; var15 <= var94; var15++) {
               for (int var16 = var74; var16 <= var14; var16++) {
                  int var17 = var1.getTileDrawX(var15);
                  int var18 = var1.getTileDrawY(var16);
                  var39.add(var17, var18, 32, 32, var0.lightManager.getInsideModifier(var15, var16), 0.0F, 0.0F, 0.4F);
               }
            }

            var39.draw();
      }

      if (showRegionBounds) {
         int var54 = var0.regionManager.getRegionCoordByTile(var1.getStartTileX());
         int var71 = var0.regionManager.getRegionCoordByTile(var1.getStartTileY());
         int var88 = var0.regionManager.getRegionCoordByTile(var1.getEndTileX());
         int var109 = var0.regionManager.getRegionCoordByTile(var1.getEndTileY());
         SettlementsWorldData var137 = SettlementsWorldData.getSettlementsData(var0);

         for (int var164 = var54; var164 <= var88; var164++) {
            int var190 = var0.regionManager.getTileCoordByRegion(var164);
            int var213 = var0.regionManager.getRegionTileWidth(var164, var190);
            if (var213 > 0) {
               for (int var235 = var71; var235 <= var109; var235++) {
                  int var252 = var0.regionManager.getTileCoordByRegion(var235);
                  int var262 = var0.regionManager.getRegionTileHeight(var235, var252);
                  if (var262 > 0) {
                     int var268 = var1.getTileDrawX(var190);
                     int var273 = var1.getTileDrawY(var252);
                     Rectangle var23 = new Rectangle(var268, var273, var213 * 32, var262 * 32);
                     Renderer.drawRectangleLines(var23, 1.0F, 1.0F, 1.0F, 0.5F);
                     FormFlow var24 = new FormFlow(var273 + 5);
                     int var25 = var137.getSettlementUniqueIDAtRegion(var0.getIdentifier(), var164, var235);
                     FontOptions var26 = new FontOptions(16).outline();
                     FontManager.bit.drawString(var268, var24.next(20), "Region: " + var164 + "x" + var235, var26);
                     FontManager.bit.drawString(var268, var24.next(20), "Tile: " + var190 + "x" + var252, var26);
                     FontManager.bit.drawString(var268, var24.next(20), "Size: " + var213 + "x" + var262, var26);
                     FontManager.bit.drawString(var268, var24.next(20), "Settlement: " + var25, var26);
                     if (!var0.regionManager.isRegionLoaded(var164, var235)) {
                        FontManager.bit.drawString(var268, var24.next(20), "Not loaded", var26);
                     } else {
                        Region var27 = var0.regionManager.getRegion(var164, var235, false);
                        if (var27 != null) {
                           if (var0.isServer()) {
                              FontManager.bit.drawString(var268, var24.next(20), "Save buffer: " + var27.getUpdateSaveBuffer(), var26);
                           }

                           if (var27.isPirateVillageRegion) {
                              FontManager.bit.drawString(var268, var24.next(20), "- Pirate village region", var26);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (showWorldPresetRegionBounds) {
         int var55 = GameMath.getRegionCoordByTile(var2 == null ? var1.getStartTileX() : var2.getTileX());
         int var72 = GameMath.getRegionCoordByTile(var2 == null ? var1.getStartTileY() : var2.getTileY());
         int var89 = WorldPresetsRegion.getWorldPresetsRegionFromLevelRegion(var55);
         int var110 = WorldPresetsRegion.getWorldPresetsRegionFromLevelRegion(var72);
         int var138 = 1024;
         int var165 = var1.getTileDrawX(var89 * var138);
         int var191 = var1.getTileDrawY(var110 * var138);
         Rectangle var214 = new Rectangle(var165, var191, var138 * 32, var138 * 32);
         Renderer.drawRectangleLines(var214, 1.0F, 1.0F, 1.0F, 0.5F);
      }

      if (var2 != null && var4.getFormManager() != null && (!var4.getFormManager().isMouseOver() || Input.lastInputIsController)) {
         InventoryItem var56 = var2.getSelectedItem();
         AttackHandler var73 = var2.getAttackHandler();
         if (var73 != null) {
            var73.drawHUDItemSelected(var1, var0, var2, var56);
         } else if (var56 != null) {
            var56.item.drawHUDItemSelected(var1, var0, var2, var56);
         }

         if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
            ControllerInteractTarget var90 = var2.getControllerInteractTarget(false, var2.getCurrentAttackHeight(), var1);
            if (var90 != null) {
               DrawOptions var111 = var90.getDrawOptions();
               if (var111 != null) {
                  var111.draw();
               }

               var90.onCurrentlyFocused();
            }

            if (var73 != null) {
               var73.drawControllerAimPos(var1, var0, var2, var56);
            } else if (var56 != null) {
               var56.item.drawControllerAimPos(var1, var0, var2, var56);
            }
         }

         if (!debugActive) {
            NetworkClient var91 = streamClients(var0)
               .filter(
                  var2x -> var2x != null
                     && var2x.playerMob != null
                     && var2x.hasSpawned()
                     && !var2x.isDead()
                     && var2x.isSamePlace(var0)
                     && var2x.playerMob != var2
               )
               .filter(var2x -> var2x.playerMob.getSelectBox().contains(var6, var7))
               .findFirst()
               .orElse(null);
            if (var91 != null) {
               PlayerMob var112 = var91.playerMob;
               GameBlackboard var139 = new GameBlackboard();
               if ((var56 != null && var56.item.onMouseHoverMob(var56, var1, var2, var112, var139, false)) | var112.onMouseHover(var1, var2, var139, false)) {
                  if (debugShow == HUD.DebugShow.PATHS) {
                     synchronized (paths) {
                        paths.stream()
                           .filter(var1x -> var1x.path.finder.mob.getUniqueID() == var112.getUniqueID())
                           .findFirst()
                           .ifPresent(var1x -> TilePathfinding.drawPathProcess(var1x.path, var1));
                     }
                  }

                  return;
               }
            }

            for (Mob var140 : var0.entityManager.mobs.getInRegionRangeByTile(var8, var9, 1)) {
               if (var140.getSelectBox().contains(var6, var7)) {
                  GameBlackboard var166 = new GameBlackboard();
                  if ((var56 != null && var56.item.onMouseHoverMob(var56, var1, var2, var140, var166, false)) | var140.onMouseHover(var1, var2, var166, false)) {
                     if (debugShow == HUD.DebugShow.PATHS) {
                        synchronized (paths) {
                           paths.stream()
                              .filter(var1x -> var1x.path.finder.mob.getUniqueID() == var140.getUniqueID())
                              .findFirst()
                              .ifPresent(var1x -> TilePathfinding.drawPathProcess(var1x.path, var1));
                        }
                     }

                     return;
                  }
               }
            }

            for (PickupEntity var141 : var0.entityManager.pickups.getInRegionRangeByTile(var8, var9, 1)) {
               if (var141.shouldDraw() && var141.getSelectBox().contains(var6, var7)) {
                  GameBlackboard var167 = new GameBlackboard();
                  if ((var56 != null && var56.item.onMouseHoverPickup(var56, var1, var2, var141, var167, false))
                     | var141.onMouseHover(var1, var2, var167, false)) {
                     return;
                  }
               }
            }

            for (Projectile var142 : var0.entityManager.projectiles.getInRegionRangeByTile(var8, var9, 1)) {
               Shape var168 = var142.getSelectBox();
               if (var168.contains(var6, var7) && var142.onMouseHover(var1, var2, false)) {
                  return;
               }
            }

            LevelObject var116 = GameUtils.getInteractObjectHit(var0, var6, var7, 0, var1x -> var1x.canInteract(var2), null);
            if (var116 != null) {
               Renderer.setCursor(GameWindow.CURSOR.INTERACT);
               var116.onMouseHover(var1, var2, false);
               if (Settings.showControlTips) {
                  String var143 = var116.getInteractTip(var2, false);
                  if (var143 != null) {
                     GameTooltipManager.addTooltip(
                        new InputTooltip(Control.MOUSE2, var143, var116.isInInteractRange(var2) ? 1.0F : 0.7F), TooltipLocation.INTERACT_FOCUS
                     );
                  }
               }
            }

            LevelObject var144 = GameUtils.getInteractObjectHit(var0, var6, var7, 0, null);
            if (var116 == null || var116.tileX != var144.tileX || var116.tileY != var144.tileY) {
               var144.onMouseHover(var1, var2, false);
            }

            if (var0.drawUtils.drawWire(var2) && var0.logicLayer.hasGate(var8, var9)) {
               var0.logicLayer.getLogicGate(var8, var9).onMouseHover(var0, var8, var9, var2, false);
            }

            if (var56 != null) {
               var56.item.onMouseHoverTile(var56, var1, var2, var6, var7, new TilePosition(var0, var8, var9), false);
            }
         } else {
            NetworkClient var92 = streamClients(var0)
               .filter(var1x -> var1x != null && var1x.playerMob != null && var1x.hasSpawned() && !var1x.isDead() && var1x.isSamePlace(var0))
               .filter(var2x -> var2x.playerMob.getSelectBox().contains(var6, var7))
               .findFirst()
               .orElse(null);
            if (var92 != null) {
               PlayerMob var117 = var92.playerMob;
               GameBlackboard var145 = new GameBlackboard();
               if ((var56 != null && var56.item.onMouseHoverMob(var56, var1, var2, var117, var145, true)) | var117.onMouseHover(var1, var2, var145, true)) {
                  Rectangle var176 = var117.getCollision();
                  Rectangle var200 = var117.getHitBox();
                  Renderer.initQuadDraw(var176.width, var176.height).color(1.0F, 0.0F, 0.0F, 0.3F).draw(var1.getDrawX(var176.x), var1.getDrawY(var176.y));
                  Renderer.initQuadDraw(var200.width, var200.height).color(0.0F, 0.0F, 1.0F, 0.3F).draw(var1.getDrawX(var200.x), var1.getDrawY(var200.y));
                  levelBoundOptions(var1, var117.getSelectBox()).draw();
                  if (debugShow == HUD.DebugShow.PATHS) {
                     synchronized (paths) {
                        paths.stream()
                           .filter(var1x -> var1x.path.finder.mob.getUniqueID() == var117.getUniqueID())
                           .findFirst()
                           .ifPresent(var1x -> TilePathfinding.drawPathProcess(var1x.path, var1));
                     }
                  }

                  return;
               }
            }

            for (Mob var146 : var0.entityManager.mobs.getInRegionRangeByTile(var8, var9, 1)) {
               if (var146.getSelectBox().contains(var6, var7)) {
                  GameBlackboard var170 = new GameBlackboard();
                  if ((var56 != null && var56.item.onMouseHoverMob(var56, var1, var2, var146, var170, true)) | var146.onMouseHover(var1, var2, var170, true)) {
                     Rectangle var193 = var146.getCollision();
                     Rectangle var215 = var146.getHitBox();
                     Renderer.initQuadDraw(var193.width, var193.height).color(1.0F, 0.0F, 0.0F, 0.3F).draw(var1.getDrawX(var193.x), var1.getDrawY(var193.y));
                     Renderer.initQuadDraw(var215.width, var215.height).color(0.0F, 0.0F, 1.0F, 0.3F).draw(var1.getDrawX(var215.x), var1.getDrawY(var215.y));
                     levelBoundOptions(var1, var146.getSelectBox()).draw();
                     if (debugShow == HUD.DebugShow.PATHS) {
                        synchronized (paths) {
                           paths.stream()
                              .filter(var1x -> var1x.path.finder.mob.getUniqueID() == var146.getUniqueID())
                              .findFirst()
                              .ifPresent(var1x -> TilePathfinding.drawPathProcess(var1x.path, var1));
                        }
                     }

                     return;
                  }
               }
            }

            for (PickupEntity var147 : var0.entityManager.pickups.getInRegionRangeByTile(var8, var9, 1)) {
               if (var147.shouldDraw() && var147.getSelectBox().contains(var6, var7)) {
                  Rectangle var171 = var147.getCollision();
                  Renderer.initQuadDraw(var171.width, var171.height).color(0.0F, 0.0F, 1.0F, 0.3F).draw(var1.getDrawX(var171.x), var1.getDrawY(var171.y));
                  levelBoundOptions(var1, var147.getSelectBox()).draw();
                  GameBlackboard var194 = new GameBlackboard();
                  if (var56 != null) {
                     var56.item.onMouseHoverPickup(var56, var1, var2, var147, var194, true);
                  }

                  var147.onMouseHover(var1, var2, var194, true);
                  return;
               }
            }

            for (Projectile var148 : var0.entityManager.projectiles.getInRegionRangeByTile(var8, var9, 1)) {
               Shape var172 = var148.getSelectBox();
               if (var172.contains(var6, var7)) {
                  Renderer.drawShape(var172, var1, true, 1.0F, 0.0F, 0.0F, 0.3F);
                  float var195 = Math.max(var148.getHitLength(), 16.0F);
                  Float var216 = new Float(var148.x, var148.y, var148.x + var148.dx * var195, var148.y + var148.dy * var195);
                  Renderer.drawShape(var148.toHitbox(var216), var1, true, 0.0F, 0.0F, 1.0F, 0.3F);
                  var148.onMouseHover(var1, var2, true);
                  return;
               }
            }

            int var121 = var1.getTileDrawX(var8);
            int var149 = var1.getTileDrawY(var9);
            if (!var5.isKeyDown(340)) {
               MultiTile var173 = var0.getObject(var8, var9).getMultiTile(var0, 0, var8, var9);
               tileBoundOptions(var1, var173.getTileRectangle(var8, var9)).draw();

               for (Rectangle var237 : var0.getObject(var8, var9).getCollisions(var0, var8, var9, var0.getObjectRotation(var8, var9))) {
                  Renderer.initQuadDraw(var237.width, var237.height).color(1.0F, 0.0F, 0.0F, 0.5F).draw(var1.getDrawX(var237.x), var1.getDrawY(var237.y));
               }

               for (Rectangle var253 : var0.getObject(var8, var9).getHoverHitboxes(var0, 0, var8, var9)) {
                  Renderer.initQuadDraw(var253.width, var253.height).color(0.0F, 0.0F, 1.0F, 0.5F).draw(var1.getDrawX(var253.x), var1.getDrawY(var253.y));
               }

               StringTooltips var239 = new StringTooltips();
               var239.add("x: " + var8 + ", y: " + var9 + (var0.isProtected(var8, var9) ? " (Protected)" : ""));
               var239.add("Biome: " + var0.getBiome(var8, var9).getStringID() + " (" + var0.biomeLayer.getBiomeID(var8, var9) + ")");
               var239.add(
                  "Tile: "
                     + var0.getTileName(var8, var9).translate()
                     + " ("
                     + var0.getTileID(var8, var9)
                     + "), Placed: "
                     + var0.tileLayer.isPlayerPlaced(var8, var9)
               );
               var0.objectLayer.addObjectsDebugTooltip(var239, var8, var9);
               if (var0.logicLayer.hasGate(var8, var9)) {
                  GameLogicGate var254 = var0.logicLayer.getLogicGate(var8, var9);
                  var239.add("Logic gate: " + var254.getDisplayName() + " (" + var254.getID() + ")");
               }

               StringBuilder var255 = new StringBuilder();
               byte var263 = var0.wireManager.getWireData(var8, var9);

               for (int var269 = 0; var269 < 8; var269++) {
                  var255.append(var263 >> var269 & 1);
               }

               var239.add("Wire: " + var255);
               var239.add("Light: " + var0.lightManager.getLightLevel(var8, var9));
               var239.add("Static light: " + var0.lightManager.getStaticLight(var8, var9));
               var239.add("Particle light: " + var0.lightManager.getParticleLight(var8, var9));
               List<SourcedGameLight> var270 = var0.lightManager.getStaticLightSources(var8, var9);
               if (!var270.isEmpty()) {
                  var239.add("Light sources:");

                  for (SourcedGameLight var277 : var270) {
                     var239.add("  " + var277);
                  }
               }

               if (debugShow == HUD.DebugShow.INSIDE_LIGHT_MODIFIER) {
                  var239.add("Inside light modifier: " + var0.lightManager.getInsideModifier(var8, var9));
                  List<SourcedLightModifier> var275 = var0.lightManager.getInsideLightSources(var8, var9);
                  if (var275 == null) {
                     var239.add("Inside light sources: null");
                  } else {
                     var239.add("Inside light sources: ");

                     for (SourcedLightModifier var280 : var275) {
                        var239.add("  " + var280.sourceX + "x" + var280.sourceY + ": " + var280.getValue());
                     }
                  }
               }

               var239.add("Height: " + var0.liquidManager.getHeight(var8, var9));
               var239.add("Salt water: " + var0.liquidManager.getSaltWaterDepth(var8, var9));
               var239.add("RoomID: " + var0.getRoomID(var8, var9) + ", size: " + var0.getRoomSize(var8, var9));
               var239.add("RegionID: " + var0.getRegionID(var8, var9));
               var239.add("Outside: " + var0.isOutside(var8, var9));
               ObjectEntity var276 = var0.entityManager.getObjectEntity(var8, var9);
               if (var276 != null) {
                  var239.add("Entity: " + var276.type);
               }

               List<TileLevelJob> var279 = var0.jobsLayer.streamJobsInTile(var8, var9).collect(Collectors.toList());
               if (!var279.isEmpty()) {
                  var239.add("Jobs:");

                  for (TileLevelJob var283 : var279) {
                     var239.add("\t" + var283);
                  }
               }

               var0.regionManager.addSplattingDebugTooltips(var8, var9, var239);
               var0.regionManager.addBiomeBlendingDebugTooltips(var8, var9, var239);
               GameTooltipManager.addTooltip(var239, TooltipLocation.BOTTOM_LEFT);
               LevelObject var282 = GameUtils.getInteractObjectHit(var0, var6, var7, 0, var1x -> var1x.canInteract(var2), null);
               if (var282 != null) {
                  Renderer.setCursor(GameWindow.CURSOR.INTERACT);
                  if (Settings.showControlTips) {
                     String var284 = var282.getInteractTip(var2, true);
                     if (var284 != null) {
                        GameTooltipManager.addTooltip(
                           new InputTooltip(Control.MOUSE2, var284, var282.isInInteractRange(var2) ? 1.0F : 0.7F), TooltipLocation.INTERACT_FOCUS
                        );
                     }
                  }
               }

               LevelObject var285 = GameUtils.getInteractObjectHit(var0, var6, var7, 0, null);
               var285.onMouseHover(var1, var2, true);
               if (var0.drawUtils.drawWire(var2) && var0.logicLayer.hasGate(var8, var9)) {
                  var0.logicLayer.getLogicGate(var8, var9).onMouseHover(var0, var8, var9, var2, true);
               }

               if (var56 != null) {
                  var56.item.onMouseHoverTile(var56, var1, var2, var6, var7, new TilePosition(var0, var8, var9), true);
               }
            } else {
               tileBoundOptions(var1, var8, var9, var8, var9).draw();
               FontOptions var174 = new FontOptions(12);
               int var197 = var8 + 1;

               label690:
               while (true) {
                  int var219 = var1.getTileDrawX(var197);
                  if (var219 > var5.getSceneWidth()) {
                     var197 = var8 - 1;

                     while (true) {
                        var219 = var1.getTileDrawX(var197);
                        if (var219 + 32 < 0) {
                           var219 = var9 + 1;

                           while (true) {
                              int var240 = var1.getTileDrawY(var219);
                              if (var240 > var5.getSceneHeight()) {
                                 var219 = var9 - 1;

                                 while (true) {
                                    var240 = var1.getTileDrawY(var219);
                                    if (var240 + 32 < 0) {
                                       break label690;
                                    }

                                    if (var219 % 2 == 0) {
                                       Renderer.initQuadDraw(32, 32).color(0.0F, 0.0F, 1.0F, 0.2F).draw(var121, var240);
                                    } else {
                                       Renderer.initQuadDraw(32, 32).color(0.0F, 1.0F, 0.0F, 0.2F).draw(var121, var240);
                                    }

                                    var219--;
                                    FontManager.bit.drawString(var121, var240, "" + Math.abs(var219 - var9 + 1), var174);
                                 }
                              }

                              if (var219 % 2 == 0) {
                                 Renderer.initQuadDraw(32, 32).color(0.0F, 0.0F, 1.0F, 0.2F).draw(var121, var240);
                              } else {
                                 Renderer.initQuadDraw(32, 32).color(0.0F, 1.0F, 0.0F, 0.2F).draw(var121, var240);
                              }

                              var219++;
                              FontManager.bit.drawString(var121, var240, "" + Math.abs(var219 - var9 - 1), var174);
                           }
                        }

                        if (var197 % 2 == 0) {
                           Renderer.initQuadDraw(32, 32).color(0.0F, 0.0F, 1.0F, 0.2F).draw(var219, var149);
                        } else {
                           Renderer.initQuadDraw(32, 32).color(0.0F, 1.0F, 0.0F, 0.2F).draw(var219, var149);
                        }

                        var197--;
                        FontManager.bit.drawString(var219, var149, "" + Math.abs(var197 - var8 + 1), var174);
                     }
                  }

                  if (var197 % 2 == 0) {
                     Renderer.initQuadDraw(32, 32).color(0.0F, 0.0F, 1.0F, 0.2F).draw(var219, var149);
                  } else {
                     Renderer.initQuadDraw(32, 32).color(0.0F, 1.0F, 0.0F, 0.2F).draw(var219, var149);
                  }

                  var197++;
                  FontManager.bit.drawString(var219, var149, "" + Math.abs(var197 - var8 - 1), var174);
               }
            }
         }

         if (Settings.smartMining || Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
            Point var93;
            if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
               java.awt.geom.Point2D.Float var122 = var2.getControllerAimDir();
               var93 = new Point((int)(var2.x + var122.x * 100.0F), (int)(var2.y + var122.y * 100.0F));
            } else {
               var93 = new Point(var1.getMouseLevelPosX(), var1.getMouseLevelPosY());
            }

            ToolDamageItem.SmartMineTarget var123 = null;
            if (var56 != null && var56.item instanceof ToolDamageItem) {
               var123 = ((ToolDamageItem)var56.item).getFirstSmartHitTile(var0, var2, var56, var93.x, var93.y);
            }

            if (var123 != null) {
               if (var123.isObject) {
                  GameObject var150 = var0.getObject(var123.x, var123.y);
                  MultiTile var175 = var150.getMultiTile(var0, var123.priorityObjectLayerID == -1 ? 0 : var123.priorityObjectLayerID, var123.x, var123.y);
                  Rectangle var199 = var175.getTileRectangle(var123.x, var123.y);
                  tileBoundOptions(var1, new Color(255, 255, 255), true, var199).draw();
               } else {
                  tileBoundOptions(var1, new Color(255, 255, 255), true, var123.x, var123.y, var123.x, var123.y).draw();
               }
            }
         }
      }
   }

    private static Stream<NetworkClient> streamClients(Level level) {
        if (level.isClient()) {
            return level.getClient().streamClients().map(c -> c);
        }
        if (level.isServer()) {
            return level.getServer().streamClients().map(c -> c);
        }
        return Stream.empty();
    }

    public static void reset() {
        debugActive = false;
    }

    private static void drawCells(SubRegion sr, Color color, Level level, GameCamera camera) {
        int startTileX = level.limitTileXToBounds(camera.getStartTileX());
        int startTileY = level.limitTileYToBounds(camera.getStartTileY());
        int endTileX = level.limitTileXToBounds(camera.getEndTileX() + 2);
        int endTileY = level.limitTileYToBounds(camera.getEndTileY() + 2);
        for (Point tile : sr.getLevelTiles()) {
            if (tile.x < startTileX || tile.x >= endTileX || tile.y < startTileY || tile.y >= endTileY) continue;
            Renderer.initQuadDraw(32, 32).color(color).alpha(0.15f).draw(camera.getTileDrawX(tile.x), camera.getTileDrawY(tile.y));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void submitPath(PathResult<Point, TilePathfinding> path) {
        if (!debugActive && debugShow != DebugShow.PATHS) {
            return;
        }
        LinkedList<SubmittedPath> linkedList = paths;
        synchronized (linkedList) {
            long time = ((TilePathfinding)path.finder).level.getWorldEntity().getLocalTime();
            paths.removeIf(p -> ((TilePathfinding)p.path.finder).mob.getUniqueID() == ((TilePathfinding)path.finder).mob.getUniqueID());
            paths.add(new SubmittedPath(path, time));
        }
    }

    public static DrawOptions selectBoundOptions(Color color, boolean outline, int startX, int startY, int endX, int endY) {
        DrawOptionsList drawOptions = new DrawOptionsList();
        GameTexture texture = outline ? Settings.UI.select_outline : Settings.UI.select;
        drawOptions.add(texture.initDraw().sprite(0, 0, 16).color(color).pos(startX, startY));
        drawOptions.add(texture.initDraw().sprite(1, 0, 16).color(color).pos(endX - 16, startY));
        drawOptions.add(texture.initDraw().sprite(0, 1, 16).color(color).pos(startX, endY - 16));
        drawOptions.add(texture.initDraw().sprite(1, 1, 16).color(color).pos(endX - 16, endY - 16));
        return drawOptions;
    }

    public static DrawOptions selectBoundOptions(Color color, boolean outline, Rectangle rectangle) {
        return HUD.selectBoundOptions(color, outline, rectangle.x, rectangle.y, rectangle.x + rectangle.width - 1, rectangle.y + rectangle.height - 1);
    }

    public static DrawOptions levelBoundOptions(GameCamera camera, Color color, boolean outline, int startX, int startY, int endX, int endY) {
        return HUD.selectBoundOptions(color, outline, camera.getDrawX(startX), camera.getDrawY(startY), camera.getDrawX(endX), camera.getDrawY(endY));
    }

    public static DrawOptions levelBoundOptions(GameCamera camera, int startX, int startY, int endX, int endY) {
        return HUD.levelBoundOptions(camera, Color.WHITE, false, startX, startY, endX, endY);
    }

    public static DrawOptions levelBoundOptions(GameCamera camera, Color color, boolean outline, Rectangle rectangle) {
        return HUD.levelBoundOptions(camera, color, outline, rectangle.x, rectangle.y, rectangle.x + rectangle.width - 1, rectangle.y + rectangle.height - 1);
    }

    public static DrawOptions levelBoundOptions(GameCamera camera, Rectangle rectangle) {
        return HUD.levelBoundOptions(camera, Color.WHITE, false, rectangle);
    }

    public static DrawOptions tileBoundOptions(GameCamera camera, Color color, boolean outline, int startX, int startY, int endX, int endY) {
        DrawOptionsList drawOptions = new DrawOptionsList();
        GameTexture texture = outline ? Settings.UI.select_outline : Settings.UI.select;
        drawOptions.add(texture.initDraw().sprite(0, 0, 16).color(color).pos(camera.getTileDrawX(startX), camera.getTileDrawY(startY)));
        drawOptions.add(texture.initDraw().sprite(1, 0, 16).color(color).pos(camera.getTileDrawX(endX) + 16, camera.getTileDrawY(startY)));
        drawOptions.add(texture.initDraw().sprite(0, 1, 16).color(color).pos(camera.getTileDrawX(startX), camera.getTileDrawY(endY) + 16));
        drawOptions.add(texture.initDraw().sprite(1, 1, 16).color(color).pos(camera.getTileDrawX(endX) + 16, camera.getTileDrawY(endY) + 16));
        return drawOptions;
    }

    public static DrawOptions tileBoundOptions(GameCamera camera, int startX, int startY, int endX, int endY) {
        return HUD.tileBoundOptions(camera, Color.WHITE, false, startX, startY, endX, endY);
    }

    public static DrawOptions tileBoundOptions(GameCamera camera, Color color, boolean outline, Rectangle rectangle) {
        return HUD.tileBoundOptions(camera, color, outline, rectangle.x, rectangle.y, rectangle.x + rectangle.width - 1, rectangle.y + rectangle.height - 1);
    }

    public static DrawOptions tileBoundOptions(GameCamera camera, Rectangle rectangle) {
        return HUD.tileBoundOptions(camera, Color.WHITE, false, rectangle);
    }

    public static DrawOptions getDirectionIndicator(float fromX, float fromY, float targetX, float targetY, int maxDistance, float paddingSizeDivisor, FontOptions fontOptions, PositionedDrawOptionsBox labelDrawOptions, int labelDrawX, int labelDrawY, GameCamera camera) {
        float distance;
        int checkDistance;
        IntersectionPoint<Rectangle> screenHitPosition;
        Rectangle labelBounds = labelDrawOptions.getBoundingBox(labelDrawX, labelDrawY);
        boolean addDistanceText = false;
        int xPadding = 25;
        int yPadding = 25;
        if (paddingSizeDivisor > 0.0f) {
            xPadding = (int)((float)camera.getWidth() / paddingSizeDivisor);
            yPadding = (int)((float)camera.getHeight() / paddingSizeDivisor);
        }
        final Rectangle inScreen = new Rectangle(xPadding + labelDrawX - labelBounds.x, yPadding, camera.getWidth() - (labelBounds.width - (labelDrawX - labelBounds.x)), camera.getHeight() - labelBounds.height);
        inScreen.width -= inScreen.x + xPadding;
        inScreen.height -= inScreen.y + yPadding;
        int centerDrawX = camera.getWidth() / 2;
        int centerDrawY = camera.getHeight() / 2;
        if (!inScreen.contains(labelBounds) && (screenHitPosition = GameUtils.castRayFirstHit(new Line2D.Float(centerDrawX, centerDrawY, labelDrawX, labelDrawY), checkDistance = camera.getWidth() + camera.getHeight(), new Function<Line2D, IntersectionPoint<Rectangle>>(){

            @Override
            public IntersectionPoint<Rectangle> apply(Line2D line2D) {
                return CollisionPoint.getClosestCollision(Stream.of(inScreen), line2D, true);
            }
        })) != null) {
            labelDrawX = (int)screenHitPosition.x;
            labelDrawY = (int)screenHitPosition.y;
            addDistanceText = true;
        }
        if (maxDistance > 0 && (distance = GameMath.getExactDistance(centerDrawX, centerDrawY, labelDrawX, labelDrawY)) > (float)maxDistance) {
            Point2D.Float labelDrawDir = GameMath.normalize(labelDrawX - centerDrawX, labelDrawY - centerDrawY);
            labelDrawX = (int)((float)centerDrawX + labelDrawDir.x * (float)maxDistance);
            labelDrawY = (int)((float)centerDrawY + labelDrawDir.y * (float)maxDistance);
            addDistanceText = true;
        }
        DrawOptionsList list = new DrawOptionsList();
        list.add(labelDrawOptions.toDrawOptionsBox(labelDrawX, labelDrawY));
        if (addDistanceText) {
            String distance2 = "(" + (int)GameMath.pixelsToMeters((float)new Point2D.Float(fromX, fromY).distance(targetX, targetY)) + "m)";
            int distanceWidth = FontManager.bit.getWidthCeil(distance2, fontOptions);
            int finalLabelDrawX = labelDrawX;
            int finalLabelDrawY = labelDrawY;
            list.add(() -> FontManager.bit.drawString(finalLabelDrawX - distanceWidth / 2, finalLabelDrawY + labelBounds.height, distance2, fontOptions));
        }
        return list;
    }

    public static DrawOptions getDirectionIndicator(float fromX, float fromY, Mob target, int maxDistance, float paddingSizeDivisor, final ArrayList<String> lines, final FontOptions fontOptions, GameCamera camera) {
        final int maxWidth = lines.stream().mapToInt(text -> FontManager.bit.getWidthCeil((String)text, fontOptions)).max().orElse(0);
        PositionedDrawOptionsBox labelDrawOptions = new PositionedDrawOptionsBox(){

            @Override
            public Rectangle getBoundingBox(int drawX, int drawY) {
                return new Rectangle(drawX - maxWidth / 2, drawY, maxWidth, lines.size() * fontOptions.getSize());
            }

            @Override
            public void draw(int drawX, int drawY) {
                for (int i = 0; i < lines.size(); ++i) {
                    String text = (String)lines.get(i);
                    int width = FontManager.bit.getWidthCeil(text, fontOptions);
                    FontManager.bit.drawString(drawX - width / 2, drawY + i * fontOptions.getSize(), text, fontOptions);
                    new StringDrawOptions(fontOptions, text).pos(drawX - width / 2, drawY + i * 16).draw();
                }
            }
        };
        Rectangle selectBox = target.isHealthBarVisible() ? target.getHealthBarBounds(target.getDrawX(), target.getDrawY()) : target.getSelectBox(target.getDrawX(), target.getDrawY());
        int desiredDrawX = camera.getDrawX(selectBox.x + selectBox.width / 2);
        int desiredDrawY = camera.getDrawY(selectBox.y - labelDrawOptions.getBoundingBox((int)0, (int)0).height - 4);
        return HUD.getDirectionIndicator(fromX, fromY, target.x, target.y, maxDistance, paddingSizeDivisor, fontOptions, labelDrawOptions, desiredDrawX, desiredDrawY, camera);
    }

    public static SharedTextureDrawOptions getOutlinesDrawOptions(GameTexture texture, int edgeResolution, int drawX, int drawY, int width, int height) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addOutlines(drawOptions, edgeResolution, drawX, drawY, width, height);
        return drawOptions;
    }

    public static DrawOptions getTrianglesDrawOptions(float[] xCoords, float[] yCoords, float[][] colors, boolean filled, float xOffset, float yOffset) {
        return HUD.getGLDrawOptions(4, xCoords, yCoords, colors, xOffset, yOffset);
    }

    public static DrawOptions getQuadsDrawOptions(float[] xCoords, float[] yCoords, float[][] colors, boolean filled, float xOffset, float yOffset) {
        return HUD.getGLDrawOptions(7, xCoords, yCoords, colors, xOffset, yOffset);
    }

    public static DrawOptions getPolygonDrawOptions(float[] xCoords, float[] yCoords, float[][] colors, boolean filled, float xOffset, float yOffset) {
        int drawMode = xCoords.length <= 2 ? 1 : (filled ? 9 : 2);
        return HUD.getGLDrawOptions(drawMode, xCoords, yCoords, colors, xOffset, yOffset);
    }

    public static DrawOptions getGLDrawOptions(int drawMode, float[] xCoords, float[] yCoords, float[][] colors, float xOffset, float yOffset) {
        if (xCoords.length != yCoords.length) {
            throw new IllegalArgumentException("x and y coords must be same length");
        }
        if (colors.length != xCoords.length) {
            throw new IllegalArgumentException("colors and coords must be same length");
        }
        return () -> {
            GameTexture.unbindTexture();
            GL11.glLoadIdentity();
            GL11.glBegin(drawMode);
            for (int i = 0; i < xCoords.length; ++i) {
                float[] color = colors[i];
                GL11.glColor4f(color[0], color[1], color[2], color[3]);
                GL11.glVertex2f(xCoords[i] + xOffset, yCoords[i] + yOffset);
            }
            GL11.glEnd();
        };
    }

    public static DrawOptions getPolygonDrawOptions(float[] xCoords, float[] yCoords, Color color, boolean filled, float xOffset, float yOffset) {
        return HUD.getPolygonDrawOptions(xCoords, yCoords, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f, filled, xOffset, yOffset);
    }

    public static DrawOptions getPolygonDrawOptions(float[] xCoords, float[] yCoords, float red, float green, float blue, float alpha, boolean filled, float xOffset, float yOffset) {
        int drawMode = xCoords.length <= 2 ? 1 : (filled ? 9 : 2);
        return HUD.getGLDrawOptions(drawMode, xCoords, yCoords, red, green, blue, alpha, xOffset, yOffset);
    }

    public static DrawOptions getGLDrawOptions(int drawMode, float[] xCoords, float[] yCoords, Color color, float xOffset, float yOffset) {
        return HUD.getGLDrawOptions(drawMode, xCoords, yCoords, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f, xOffset, yOffset);
    }

    public static DrawOptions getGLDrawOptions(int drawMode, float[] xCoords, float[] yCoords, float red, float green, float blue, float alpha, float xOffset, float yOffset) {
        if (xCoords.length != yCoords.length) {
            throw new IllegalArgumentException("x and y coords must be same length");
        }
        return () -> {
            GameTexture.unbindTexture();
            GL11.glLoadIdentity();
            GL11.glColor4f(red, green, blue, alpha);
            GL11.glBegin(drawMode);
            for (int i = 0; i < xCoords.length; ++i) {
                GL11.glVertex2f(xCoords[i] + xOffset, yCoords[i] + yOffset);
            }
            GL11.glEnd();
        };
    }

    public static DrawOptions getArrowHitboxIndicator(float fromX, float fromY, float dirX, float dirY, int range, int width, Color startColor, Color endColor, Color edgeColor) {
        DrawOptionsList out = new DrawOptionsList();
        float pointRange = (float)width / 3.0f;
        float quadRange = (float)range - pointRange;
        float halfWidth = (float)width / 2.0f;
        if (edgeColor != null) {
            float edgeQuadRange = quadRange - pointRange / 7.8539815f;
            Point2D.Float leftPos = GameMath.getPerpendicularPoint(fromX, fromY, halfWidth, dirX, dirY);
            Point2D.Float rightPos = GameMath.getPerpendicularPoint(fromX, fromY, -halfWidth, dirX, dirY);
            float edgeWidth = 5.0f;
            Point2D.Float leftEdgePos = GameMath.getPerpendicularPoint(fromX, fromY, halfWidth - edgeWidth, dirX, dirY);
            Point2D.Float rightEdgePos = GameMath.getPerpendicularPoint(fromX, fromY, -(halfWidth - edgeWidth), dirX, dirY);
            float[] xCoords = new float[]{leftEdgePos.x, leftEdgePos.x + dirX * edgeQuadRange, rightEdgePos.x, rightEdgePos.x, leftEdgePos.x + dirX * edgeQuadRange, rightEdgePos.x + dirX * edgeQuadRange, leftEdgePos.x + dirX * edgeQuadRange, fromX + dirX * ((float)range - edgeWidth), rightEdgePos.x + dirX * edgeQuadRange};
            float[] yCoords = new float[]{leftEdgePos.y, leftEdgePos.y + dirY * edgeQuadRange, rightEdgePos.y, rightEdgePos.y, leftEdgePos.y + dirY * edgeQuadRange, rightEdgePos.y + dirY * edgeQuadRange, leftEdgePos.y + dirY * edgeQuadRange, fromY + dirY * ((float)range - edgeWidth), rightEdgePos.y + dirY * edgeQuadRange};
            float[][] colors = HUD.toFloatColor2D(startColor, endColor, startColor, startColor, endColor, endColor, endColor, endColor, endColor);
            out.add(HUD.getTrianglesDrawOptions(xCoords, yCoords, colors, true, 0.0f, 0.0f));
            float[] edgeXCoords = new float[]{leftPos.x, leftPos.x + dirX * quadRange, leftEdgePos.x + dirX * edgeQuadRange, leftEdgePos.x, leftPos.x + dirX * quadRange, fromX + dirX * (float)range, fromX + dirX * ((float)range - edgeWidth), leftEdgePos.x + dirX * edgeQuadRange, fromX + dirX * (float)range, rightPos.x + dirX * quadRange, rightEdgePos.x + dirX * edgeQuadRange, fromX + dirX * ((float)range - edgeWidth), rightPos.x + dirX * quadRange, rightPos.x, rightEdgePos.x, rightEdgePos.x + dirX * edgeQuadRange};
            float[] edgeYCoords = new float[]{leftPos.y, leftPos.y + dirY * quadRange, leftEdgePos.y + dirY * edgeQuadRange, leftEdgePos.y, leftPos.y + dirY * quadRange, fromY + dirY * (float)range, fromY + dirY * ((float)range - edgeWidth), leftEdgePos.y + dirY * edgeQuadRange, fromY + dirY * (float)range, rightPos.y + dirY * quadRange, rightEdgePos.y + dirY * edgeQuadRange, fromY + dirY * ((float)range - edgeWidth), rightPos.y + dirY * quadRange, rightPos.y, rightEdgePos.y, rightEdgePos.y + dirY * edgeQuadRange};
            float[][] edgeColors = HUD.toFloatColor2D(startColor, edgeColor, edgeColor, startColor, edgeColor, edgeColor, edgeColor, edgeColor, edgeColor, edgeColor, edgeColor, edgeColor, edgeColor, startColor, startColor, edgeColor);
            out.add(HUD.getGLDrawOptions(7, edgeXCoords, edgeYCoords, edgeColors, 0.0f, 0.0f));
        } else {
            Point2D.Float leftPos = GameMath.getPerpendicularPoint(fromX, fromY, halfWidth, dirX, dirY);
            Point2D.Float rightPos = GameMath.getPerpendicularPoint(fromX, fromY, -halfWidth, dirX, dirY);
            float[] xCoords = new float[]{leftPos.x, leftPos.x + dirX * quadRange, rightPos.x, rightPos.x, leftPos.x + dirX * quadRange, rightPos.x + dirX * quadRange, leftPos.x + dirX * quadRange, fromX + dirX * (float)range, rightPos.x + dirX * quadRange};
            float[] yCoords = new float[]{leftPos.y, leftPos.y + dirY * quadRange, rightPos.y, rightPos.y, leftPos.y + dirY * quadRange, rightPos.y + dirY * quadRange, leftPos.y + dirY * quadRange, fromY + dirY * (float)range, rightPos.y + dirY * quadRange};
            float[][] colors = HUD.toFloatColor2D(startColor, endColor, startColor, startColor, endColor, endColor, endColor, endColor, endColor);
            out.add(HUD.getTrianglesDrawOptions(xCoords, yCoords, colors, true, 0.0f, 0.0f));
        }
        return out;
    }

    public static DrawOptions getArrowHitboxIndicator(float fromX, float fromY, float dirX, float dirY, int range, int width, Color startColor, Color endColor, Color edgeColor, GameCamera camera) {
        return HUD.getArrowHitboxIndicator(fromX - (float)camera.getX(), fromY - (float)camera.getY(), dirX, dirY, range, width, startColor, endColor, edgeColor);
    }

    public static float[] toFloatColor(Color ... colors) {
        float[] out = new float[colors.length * 4];
        for (int i = 0; i < colors.length; ++i) {
            Color color = colors[i];
            int outIndex = i * 4;
            out[outIndex] = (float)color.getRed() / 255.0f;
            out[outIndex + 1] = (float)color.getGreen() / 255.0f;
            out[outIndex + 2] = (float)color.getBlue() / 255.0f;
            out[outIndex + 3] = (float)color.getAlpha() / 255.0f;
        }
        return out;
    }

    public static float[][] toFloatColor2D(Color ... colors) {
        float[][] out = new float[colors.length][4];
        for (int i = 0; i < colors.length; ++i) {
            Color color = colors[i];
            float[] colorOut = out[i];
            colorOut[0] = (float)color.getRed() / 255.0f;
            colorOut[1] = (float)color.getGreen() / 255.0f;
            colorOut[2] = (float)color.getBlue() / 255.0f;
            colorOut[3] = (float)color.getAlpha() / 255.0f;
        }
        return out;
    }

    public static float[] getAdvColor(Color topLeft, Color topRight, Color botRight, Color botLeft) {
        return new float[]{(float)topLeft.getRed() / 255.0f, (float)topLeft.getGreen() / 255.0f, (float)topLeft.getBlue() / 255.0f, (float)topLeft.getAlpha() / 255.0f, (float)topRight.getRed() / 255.0f, (float)topRight.getGreen() / 255.0f, (float)topRight.getBlue() / 255.0f, (float)topRight.getAlpha() / 255.0f, (float)botRight.getRed() / 255.0f, (float)botRight.getGreen() / 255.0f, (float)botRight.getBlue() / 255.0f, (float)botRight.getAlpha() / 255.0f, (float)botLeft.getRed() / 255.0f, (float)botLeft.getGreen() / 255.0f, (float)botLeft.getBlue() / 255.0f, (float)botLeft.getAlpha() / 255.0f};
    }

    public static SharedTextureDrawOptions getCenterDrawOptions(GameTexture texture, int edgeResolution, int drawX, int drawY, int width, int height) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addCenter(drawOptions, texture, edgeResolution, drawX, drawY, width, height);
        return drawOptions;
    }

    public static SharedTextureDrawOptions getBackgroundDrawOptions(GameTexture texture, int edgeResolution, int drawX, int drawY, int width, int height) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addOutlines(drawOptions, edgeResolution, drawX, drawY, width, height);
        HUD.addCenter(drawOptions, texture, edgeResolution, drawX, drawY, width, height);
        return drawOptions;
    }

    protected static void addOutlines(SharedTextureDrawOptions drawOptions, int edgeResolution, int drawX, int drawY, int width, int height) {
        int leftWidth = Math.min(edgeResolution, width / 2);
        int rightWidth = Math.min(edgeResolution, width - leftWidth);
        int topHeight = Math.min(edgeResolution, height / 2);
        int botHeight = Math.min(edgeResolution, height - topHeight);
        int midWidth = width - edgeResolution * 2;
        int midHeight = height - edgeResolution * 2;
        drawOptions.addSpriteSection(1, 1, edgeResolution, edgeResolution - leftWidth, edgeResolution, edgeResolution - topHeight, edgeResolution).pos(drawX, drawY);
        drawOptions.addSpriteSection(1, 0, edgeResolution, edgeResolution - leftWidth, edgeResolution, 0, botHeight).pos(drawX, drawY + height - botHeight);
        drawOptions.addSpriteSection(0, 1, edgeResolution, 0, rightWidth, edgeResolution - topHeight, edgeResolution).pos(drawX + width - rightWidth, drawY);
        drawOptions.addSpriteSection(0, 0, edgeResolution, 0, rightWidth, 0, botHeight).pos(drawX + width - rightWidth, drawY + height - botHeight);
        if (midWidth > 0) {
            for (int x = 0; x < midWidth; x += edgeResolution) {
                int currentDrawWidth = Math.min(edgeResolution, midWidth - x);
                drawOptions.addSpriteSection(1, 1, edgeResolution * 2, edgeResolution, 0, currentDrawWidth, edgeResolution - topHeight, edgeResolution).pos(drawX + edgeResolution + x, drawY);
                drawOptions.addSpriteSection(1, 0, edgeResolution * 2, edgeResolution, 0, currentDrawWidth, 0, botHeight).pos(drawX + edgeResolution + x, drawY + height - botHeight);
            }
        }
        if (midHeight > 0) {
            for (int y = 0; y < midHeight; y += edgeResolution) {
                int currentDrawHeight = Math.min(edgeResolution, midHeight - y);
                drawOptions.addSpriteSection(1, 1, edgeResolution, edgeResolution * 2, edgeResolution - leftWidth, edgeResolution, 0, currentDrawHeight).pos(drawX, drawY + edgeResolution + y);
                drawOptions.addSpriteSection(0, 1, edgeResolution, edgeResolution * 2, 0, rightWidth, 0, currentDrawHeight).pos(drawX + width - rightWidth, drawY + edgeResolution + y);
            }
        }
    }

    protected static void addCenter(SharedTextureDrawOptions drawOptions, GameTexture texture, int edgeResolution, int drawX, int drawY, int width, int height) {
        int midWidth = width - edgeResolution * 2;
        int midHeight = height - edgeResolution * 2;
        int textureStartX = 0;
        int textureStartY = edgeResolution * 4;
        int textureWidth = texture.getWidth() - textureStartX;
        int textureHeight = texture.getHeight() - textureStartY;
        if (midWidth > 0 && midHeight > 0 && textureWidth > 0 && textureHeight > 0) {
            for (int x = 0; x < midWidth; x += textureWidth) {
                int currentDrawWidth = Math.min(textureWidth, midWidth - x);
                for (int y = 0; y < midHeight; y += textureHeight) {
                    int currentDrawHeight = Math.min(textureHeight, midHeight - y);
                    drawOptions.addSection(textureStartX, textureStartX + currentDrawWidth, textureStartY, textureStartY + currentDrawHeight).pos(drawX + edgeResolution + x, drawY + edgeResolution + y);
                }
            }
        }
    }

    public static boolean[][] toBooleans(int[][] ints) {
        boolean[][] out = new boolean[ints.length][0];
        for (int i = 0; i < ints.length; ++i) {
            int[] ints2 = ints[i];
            boolean[] out2 = new boolean[ints2.length];
            for (int j = 0; j < ints2.length; ++j) {
                out2[j] = ints2[j] > 0;
            }
            out[i] = out2;
        }
        return out;
    }

    public static SharedTextureDrawOptions getBackgroundEdged(GameTexture texture, int spriteRes, int xOverlap, int yOverlap, int drawX, int drawY, PointSetAbstract<?> set, int boxWidth, int boxHeight) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addBackgroundBoxed(drawOptions, spriteRes, xOverlap, yOverlap, drawX, drawY, set, boxWidth, boxHeight);
        return drawOptions;
    }

    public static void addBackgroundBoxed(SharedTextureDrawOptions drawOptions, int spriteRes, int xOverlap, int yOverlap, int drawX, int drawY, PointSetAbstract<?> set, int boxWidth, int boxHeight) {
        HUD.addBackgroundBoxed(drawOptions, spriteRes, yOverlap, 0, 0, xOverlap, drawX - xOverlap / 2, drawY - yOverlap / 2, set, boxWidth, boxHeight);
    }

    public static SharedTextureDrawOptions getBackgroundEdged(GameTexture texture, int spriteRes, int xOverlap, int yOverlap, int drawX, int drawY, boolean[][] box, int boxWidth, int boxHeight) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addBackgroundBoxed(drawOptions, spriteRes, xOverlap, yOverlap, drawX, drawY, box, boxWidth, boxHeight);
        return drawOptions;
    }

    public static void addBackgroundBoxed(SharedTextureDrawOptions drawOptions, int spriteRes, int xOverlap, int yOverlap, int drawX, int drawY, boolean[][] box, int boxWidth, int boxHeight) {
        HUD.addBackgroundBoxed(drawOptions, spriteRes, yOverlap, 0, 0, xOverlap, drawX - xOverlap / 2, drawY - yOverlap / 2, box, boxWidth, boxHeight);
    }

    protected static SharedTextureDrawOptions getBackgroundEdged(GameTexture texture, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, PointSetAbstract<?> set, int boxWidth, int boxHeight) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addBackgroundBoxed(drawOptions, spriteRes, topOverlap, rightOverlap, botOverlap, leftOverlap, drawX, drawY, set, boxWidth, boxHeight);
        return drawOptions;
    }

    protected static SharedTextureDrawOptions getBackgroundEdged(GameTexture texture, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, boolean[][] box, int boxWidth, int boxHeight) {
        SharedTextureDrawOptions drawOptions = new SharedTextureDrawOptions(texture);
        HUD.addBackgroundBoxed(drawOptions, spriteRes, topOverlap, rightOverlap, botOverlap, leftOverlap, drawX, drawY, box, boxWidth, boxHeight);
        return drawOptions;
    }

    protected static void addBackgroundBoxed(SharedTextureDrawOptions drawOptions, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, PointSetAbstract<?> set, int boxWidth, int boxHeight) {
        HUD.addBackgroundBoxed(drawOptions, spriteRes, topOverlap, rightOverlap, botOverlap, leftOverlap, drawX, drawY, set, (Point p) -> {
            boolean[] edge = new boolean[Level.adjacentGetters.length];
            for (int i = 0; i < Level.adjacentGetters.length; ++i) {
                Point pos = Level.adjacentGetters[i];
                edge[i] = set.contains(p.x + pos.x, p.y + pos.y);
            }
            return edge;
        }, boxWidth, boxHeight);
    }

    protected static void addBackgroundBoxed(SharedTextureDrawOptions drawOptions, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, boolean[][] box, int boxWidth, int boxHeight) {
        Iterable<Point> iterable = () -> {
            Stream.Builder<Point> builder = Stream.builder();
            for (int y = 0; y < box.length; ++y) {
                for (int x = 0; x < box[y].length; ++x) {
                    if (!box[y][x]) continue;
                    builder.accept(new Point(x, y));
                }
            }
            return builder.build().iterator();
        };
        HUD.addBackgroundBoxed(drawOptions, spriteRes, topOverlap, rightOverlap, botOverlap, leftOverlap, drawX, drawY, iterable, (Point p) -> HUD.getAdjacent(box, p.x, p.y), boxWidth, boxHeight);
    }

    protected static void addBackgroundBoxed(SharedTextureDrawOptions drawOptions, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, Iterable<Point> points, Predicate<Point> isActive, int boxWidth, int boxHeight) {
        HUD.addBackgroundBoxed(drawOptions, spriteRes, topOverlap, rightOverlap, botOverlap, leftOverlap, drawX, drawY, points, (Point p) -> {
            boolean[] edge = new boolean[Level.adjacentGetters.length];
            for (int i = 0; i < Level.adjacentGetters.length; ++i) {
                Point pos = Level.adjacentGetters[i];
                edge[i] = isActive.test(new Point(p.x + pos.x, p.y + pos.y));
            }
            return edge;
        }, boxWidth, boxHeight);
    }

    protected static void addBackgroundBoxed(SharedTextureDrawOptions drawOptions, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, Iterable<Point> points, Function<Point, boolean[]> adjacentBuilder, int boxWidth, int boxHeight) {
        for (Point p : points) {
            boolean[] edge = adjacentBuilder.apply(p);
            HUD.addBackgroundBoxTile(drawOptions, spriteRes, topOverlap, rightOverlap, botOverlap, leftOverlap, drawX + p.x * boxWidth, drawY + p.y * boxHeight, edge, boxWidth, boxHeight);
        }
    }

    public static void addBackgroundBoxTile(SharedTextureDrawOptions drawOptions, int spriteRes, int xOverlap, int yOverlap, int drawX, int drawY, boolean[] adjacent, int width, int height) {
        HUD.addBackgroundBoxTile(drawOptions, spriteRes, yOverlap, 0, 0, xOverlap, drawX - xOverlap / 2, drawY - yOverlap / 2, adjacent, width, height);
    }

    protected static void addBackgroundBoxTile(SharedTextureDrawOptions drawOptions, int spriteRes, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap, int drawX, int drawY, boolean[] adjacent, int width, int height) {
        BoxEdge botRight;
        BoxEdge botLeft;
        BoxEdge topRight;
        BoxEdge topLeft;
        if (width < spriteRes) {
            throw new IllegalArgumentException("boxWidth cannot be smaller than spriteRes");
        }
        if (height < spriteRes) {
            throw new IllegalArgumentException("boxHeight cannot be smaller than spriteRes");
        }
        GameTextureSection section = new GameTextureSection(drawOptions.texture);
        int halfWidth1 = width / 2;
        int halfHeight1 = height / 2;
        int halfWidth2 = width - halfWidth1;
        int halfHeight2 = height - halfHeight1;
        int currentDrawX = drawX;
        int currentDrawY = drawY;
        int currentWidth = width;
        int currentHeight = height;
        if (!adjacent[1]) {
            currentHeight -= topOverlap;
            currentDrawY += topOverlap;
        }
        if (!adjacent[3]) {
            currentWidth -= leftOverlap;
            currentDrawX += leftOverlap;
        }
        if (!adjacent[4]) {
            currentWidth -= rightOverlap;
        }
        if (!adjacent[6]) {
            currentHeight -= botOverlap;
        }
        if ((topLeft = HUD.getBoxTopLeft(section, spriteRes, adjacent, width, height, topOverlap, rightOverlap, botOverlap, leftOverlap)) != null) {
            int sectionDrawX = currentDrawX - spriteRes + topLeft.xOffset;
            int sectionDrawY = currentDrawY - spriteRes + topLeft.yOffset;
            if (topLeft.section != null) {
                drawOptions.add(topLeft.section).pos(sectionDrawX, sectionDrawY);
            }
            topLeft.addExtensionDrawsTopLeft(drawOptions, sectionDrawX, sectionDrawY, spriteRes, halfWidth1, halfHeight1);
        }
        if ((topRight = HUD.getBoxTopRight(section, spriteRes, adjacent, width, height, topOverlap, rightOverlap, botOverlap, leftOverlap)) != null) {
            int sectionDrawX = currentDrawX + width + topRight.xOffset;
            int sectionDrawY = currentDrawY - spriteRes + topRight.yOffset;
            if (topRight.section != null) {
                drawOptions.add(topRight.section).pos(sectionDrawX, sectionDrawY);
            }
            topRight.addExtensionDrawsTopRight(drawOptions, sectionDrawX, sectionDrawY, spriteRes, halfWidth2, halfHeight1);
        }
        if ((botLeft = HUD.getBoxBotLeft(section, spriteRes, adjacent, width, height, topOverlap, rightOverlap, botOverlap, leftOverlap)) != null) {
            int sectionDrawX = currentDrawX - spriteRes + botLeft.xOffset;
            int sectionDrawY = currentDrawY + height + botLeft.yOffset;
            if (botLeft.section != null) {
                drawOptions.add(botLeft.section).pos(sectionDrawX, sectionDrawY);
            }
            botLeft.addExtensionDrawsBotLeft(drawOptions, sectionDrawX, sectionDrawY, spriteRes, halfWidth1, halfHeight2);
        }
        if ((botRight = HUD.getBoxBotRight(section, spriteRes, adjacent, width, height, topOverlap, rightOverlap, botOverlap, leftOverlap)) != null) {
            int sectionDrawX = currentDrawX + width + botRight.xOffset;
            int sectionDrawY = currentDrawY + height + botRight.yOffset;
            if (botRight.section != null) {
                drawOptions.add(botRight.section).pos(sectionDrawX, sectionDrawY);
            }
            botRight.addExtensionDrawsBotRight(drawOptions, sectionDrawX, sectionDrawY, spriteRes, halfWidth2, halfHeight2);
        }
        if (!adjacent[0] && adjacent[1] && adjacent[3] && topOverlap > 0 && leftOverlap > 0) {
            HUD.addCenterBoxDraw(drawOptions, section.sprite(2, 0, spriteRes * 2), spriteRes * 2, currentDrawX + leftOverlap, currentDrawY, currentWidth - leftOverlap, currentHeight);
            HUD.addCenterBoxDraw(drawOptions, section.sprite(2, 0, spriteRes * 2), spriteRes * 2, currentDrawX, currentDrawY + topOverlap, width - (currentWidth - leftOverlap), currentHeight - topOverlap);
        } else {
            HUD.addCenterBoxDraw(drawOptions, section.sprite(2, 0, spriteRes * 2), spriteRes * 2, currentDrawX, currentDrawY, currentWidth, currentHeight);
        }
    }

    protected static void addCenterBoxDraw(SharedTextureDrawOptions drawOptions, GameTextureSection texture, int spriteRes, int drawX, int drawY, int width, int height) {
        for (int x = 0; x < width; x += spriteRes) {
            int currentWidth = Math.min(spriteRes, width - x);
            for (int y = 0; y < height; y += spriteRes) {
                int currentHeight = Math.min(spriteRes, height - y);
                drawOptions.add(texture.sprite(2, 2, spriteRes).section(0, currentWidth, 0, currentHeight)).pos(drawX + x, drawY + y);
            }
        }
    }

    protected static BoxEdge getBoxTopLeft(GameTextureSection texture, int spriteRes, boolean[] adj, int width, int height, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap) {
        int topLeft = 0;
        int top = 1;
        int left = 3;
        int bot = 6;
        int right = 4;
        if (adj[left]) {
            if (adj[top]) {
                if (adj[topLeft]) {
                    return null;
                }
                return new BoxEdge(texture.sprite(2, 2, spriteRes), leftOverlap, topOverlap);
            }
            return new BoxEdge(null, 0, 0).extend(texture.sprite(2, 1, spriteRes), true, 0, 0, 0, adj[topLeft], false);
        }
        if (adj[top]) {
            return new BoxEdge(null, 0, 0).extend(texture.sprite(1, 2, spriteRes), false, 0, 0, 0, adj[topLeft], false);
        }
        return new BoxEdge(texture.sprite(1, 1, spriteRes), 0, 0).extend(texture.sprite(2, 1, spriteRes), true, 0, 0, adj[right] ? 0 : -leftOverlap, false, false).extend(texture.sprite(1, 2, spriteRes), false, 0, 0, adj[bot] ? 0 : -topOverlap, false, false);
    }

    protected static BoxEdge getBoxTopRight(GameTextureSection texture, int spriteRes, boolean[] adj, int width, int height, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap) {
        int topRight = 2;
        int top = 1;
        int right = 4;
        int left = 3;
        int bot = 6;
        if (adj[right]) {
            if (adj[top]) {
                if (adj[topRight]) {
                    return null;
                }
                return new BoxEdge(texture.sprite(3, 2, spriteRes), -rightOverlap + (adj[left] ? 0 : -leftOverlap), topOverlap);
            }
            return new BoxEdge(null, 0, 0).extend(texture.sprite(3, 1, spriteRes), true, -width, 0, (adj[left] ? 0 : -leftOverlap) + (adj[topRight] ? leftOverlap : 0), false, adj[topRight]);
        }
        if (adj[top]) {
            return new BoxEdge(null, 0, 0).extend(texture.sprite(0, 2, spriteRes), false, adj[left] ? 0 : -leftOverlap, 0, 0, adj[topRight], false);
        }
        return new BoxEdge(texture.sprite(0, 1, spriteRes), adj[left] ? 0 : -leftOverlap, 0).extend(texture.sprite(3, 1, spriteRes), true, -width, 0, 0, false, false).extend(texture.sprite(0, 2, spriteRes), false, 0, 0, !adj[bot] ? -topOverlap : 0, false, false);
    }

    protected static BoxEdge getBoxBotLeft(GameTextureSection texture, int spriteRes, boolean[] adj, int width, int height, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap) {
        int botLeft = 5;
        int bot = 6;
        int left = 3;
        int top = 1;
        int right = 4;
        if (adj[left]) {
            if (adj[bot]) {
                if (adj[botLeft]) {
                    return null;
                }
                return new BoxEdge(texture.sprite(2, 3, spriteRes), leftOverlap, -botOverlap + (adj[top] ? 0 : -topOverlap));
            }
            return new BoxEdge(null, 0, 0).extend(texture.sprite(2, 0, spriteRes), true, 0, adj[top] ? 0 : -topOverlap, 0, adj[botLeft], false);
        }
        if (adj[bot]) {
            return new BoxEdge(null, 0, 0).extend(texture.sprite(1, 3, spriteRes), false, 0, -height, (adj[top] ? 0 : -topOverlap) + (adj[botLeft] ? topOverlap : 0), false, adj[botLeft]);
        }
        return new BoxEdge(texture.sprite(1, 0, spriteRes), 0, adj[top] ? 0 : -topOverlap).extend(texture.sprite(2, 0, spriteRes), true, 0, 0, adj[right] ? 0 : -leftOverlap, false, false).extend(texture.sprite(1, 3, spriteRes), false, 0, -height, 0, false, false);
    }

    protected static BoxEdge getBoxBotRight(GameTextureSection texture, int spriteRes, boolean[] adj, int width, int height, int topOverlap, int rightOverlap, int botOverlap, int leftOverlap) {
        int botRight = 7;
        int bot = 6;
        int right = 4;
        int left = 3;
        int top = 1;
        if (adj[right]) {
            if (adj[bot]) {
                if (adj[botRight]) {
                    return null;
                }
                return new BoxEdge(texture.sprite(3, 3, spriteRes), -rightOverlap + (adj[left] ? 0 : -leftOverlap), -botOverlap + (adj[top] ? 0 : -topOverlap));
            }
            return new BoxEdge(null, 0, 0).extend(texture.sprite(3, 0, spriteRes), true, -width, adj[top] ? 0 : -topOverlap, (adj[left] || adj[botRight] ? 0 : -leftOverlap) + (adj[left] && adj[botRight] ? leftOverlap : 0), false, adj[botRight]);
        }
        if (adj[bot]) {
            return new BoxEdge(null, 0, 0).extend(texture.sprite(0, 3, spriteRes), false, adj[left] ? 0 : -leftOverlap, -height, (!adj[botRight] && !adj[top] ? -topOverlap : 0) + (adj[botRight] && adj[top] ? topOverlap : 0), false, adj[botRight]);
        }
        return new BoxEdge(texture.sprite(0, 0, spriteRes), adj[left] ? 0 : -leftOverlap, adj[top] ? 0 : -topOverlap).extend(texture.sprite(3, 0, spriteRes), true, -width, 0, 0, false, false).extend(texture.sprite(0, 3, spriteRes), false, 0, -height, 0, false, false);
    }

    protected static boolean[] getAdjacent(boolean[][] box, int x, int y) {
        boolean[] adjacent = new boolean[8];
        if (y > 0) {
            if (x > 0) {
                adjacent[0] = box[y - 1][x - 1];
            }
            adjacent[1] = box[y - 1][x];
            if (x < box[y - 1].length - 1) {
                adjacent[2] = box[y - 1][x + 1];
            }
        }
        if (x > 0) {
            adjacent[3] = box[y][x - 1];
        }
        if (x < box[y].length - 1) {
            adjacent[4] = box[y][x + 1];
        }
        if (y < box.length - 1) {
            if (x > 0) {
                adjacent[5] = box[y + 1][x - 1];
            }
            adjacent[6] = box[y + 1][x];
            if (x < box[y + 1].length - 1) {
                adjacent[7] = box[y + 1][x + 1];
            }
        }
        return adjacent;
    }

    static {
        debugShow = DebugShow.NOTHING;
        showRegionBounds = false;
        showWorldPresetRegionBounds = false;
        paths = new LinkedList();
    }

    public static enum DebugShow {
        NOTHING,
        REGIONS,
        CONNECTED_REGIONS,
        ROOMS,
        HOUSE,
        PATHS,
        HEIGHT,
        ADVANCED_HEIGHT,
        VILLAGE_TILES,
        CURSED_ARENA_TILES,
        WATER_TYPE,
        BIOME_BLENDING,
        WIND_STATS,
        WIND_OVERLAY,
        HOSTILE_SPAWN_OVERLAY,
        INSIDE_LIGHT_MODIFIER;

    }

    private static class SubmittedPath {
        public final PathResult<Point, TilePathfinding> path;
        public final long time;

        public SubmittedPath(PathResult<Point, TilePathfinding> path, long time) {
            this.path = path;
            this.time = time;
        }
    }

    protected static class BoxEdge {
        public final GameTextureSection section;
        public final int xOffset;
        public final int yOffset;
        public LinkedList<BoxExtend> extensions = new LinkedList();

        public BoxEdge(GameTextureSection section, int xOffset, int yOffset) {
            this.section = section;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        public BoxEdge extend(GameTextureSection section, boolean horizontal, int xOffset, int yOffset, int lengthOffset, boolean skipStart, boolean skipEnd) {
            this.extensions.add(new BoxExtend(section, horizontal, xOffset, yOffset, lengthOffset, skipStart, skipEnd));
            return this;
        }

        public void addExtensionDrawsTopLeft(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
            for (BoxExtend extension : this.extensions) {
                extension.addDrawableTopLeft(drawOptions, drawX, drawY, spriteRes, halfWidth, halfHeight);
            }
        }

        public void addExtensionDrawsTopRight(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
            for (BoxExtend extension : this.extensions) {
                extension.addDrawableTopRight(drawOptions, drawX, drawY, spriteRes, halfWidth, halfHeight);
            }
        }

        public void addExtensionDrawsBotLeft(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
            for (BoxExtend extension : this.extensions) {
                extension.addDrawableBotLeft(drawOptions, drawX, drawY, spriteRes, halfWidth, halfHeight);
            }
        }

        public void addExtensionDrawsBotRight(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
            for (BoxExtend extension : this.extensions) {
                extension.addDrawableBotRight(drawOptions, drawX, drawY, spriteRes, halfWidth, halfHeight);
            }
        }

        protected static class BoxExtend {
            public final GameTextureSection extendSection;
            public final boolean horizontal;
            public final int xOffset;
            public final int yOffset;
            public final int lengthOffset;
            public final boolean skipStart;
            public final boolean skipEnd;

            public BoxExtend(GameTextureSection section, boolean horizontal, int xOffset, int yOffset, int lengthOffset, boolean skipStart, boolean skipEnd) {
                this.extendSection = section;
                this.horizontal = horizontal;
                this.xOffset = xOffset;
                this.yOffset = yOffset;
                this.lengthOffset = lengthOffset;
                this.skipStart = skipStart;
                this.skipEnd = skipEnd;
            }

            protected void addDrawable(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int boxHalfWidth, int boxHalfHeight, Color color) {
                int width = this.extendSection.getWidth();
                int height = this.extendSection.getHeight();
                if (this.horizontal) {
                    int i;
                    int end = boxHalfWidth - (this.skipEnd ? width : 0) + this.lengthOffset;
                    int n = i = this.skipStart ? width : 0;
                    while (i < end) {
                        int sectionSize = Math.min(width, end - i);
                        drawOptions.add(this.extendSection.section(0, sectionSize, 0, height)).pos(drawX + i + this.xOffset, drawY + this.yOffset);
                        i += width;
                    }
                } else {
                    int i;
                    int end = boxHalfHeight - (this.skipEnd ? height : 0) + this.lengthOffset;
                    int n = i = this.skipStart ? height : 0;
                    while (i < end) {
                        int sectionSize = Math.min(height, end - i);
                        drawOptions.add(this.extendSection.section(0, width, 0, sectionSize)).pos(drawX + this.xOffset, drawY + i + this.yOffset);
                        i += height;
                    }
                }
            }

            public void addDrawableTopLeft(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
                if (this.horizontal) {
                    drawX += spriteRes;
                } else {
                    drawY += spriteRes;
                }
                this.addDrawable(drawOptions, drawX, drawY, halfWidth, halfHeight, Color.RED);
            }

            public void addDrawableTopRight(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
                if (this.horizontal) {
                    drawX += halfWidth;
                } else {
                    drawY += spriteRes;
                }
                this.addDrawable(drawOptions, drawX, drawY, halfWidth, halfHeight, Color.GREEN);
            }

            public void addDrawableBotLeft(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
                if (this.horizontal) {
                    drawX += spriteRes;
                } else {
                    drawY += halfHeight;
                }
                this.addDrawable(drawOptions, drawX, drawY, halfWidth, halfHeight, Color.BLUE);
            }

            public void addDrawableBotRight(SharedTextureDrawOptions drawOptions, int drawX, int drawY, int spriteRes, int halfWidth, int halfHeight) {
                if (this.horizontal) {
                    drawX += halfWidth;
                } else {
                    drawY += halfHeight;
                }
                this.addDrawable(drawOptions, drawX, drawY, halfWidth, halfHeight, Color.WHITE);
            }
        }
    }
}

