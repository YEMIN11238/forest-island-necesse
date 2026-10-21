package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTexture.SharedGameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.RegionType;

public class WallObject extends GameObject {
   public static SharedGameTexture wallTextures;
   public static HashMap<String, GameTextureSection> outlineTextures = new HashMap<>();
   public static GameTexture generatedWallTexture;
   public String textureName;
   public String outlineTextureName;
   public GameTextureSection wallTexture;
   public GameTextureSection outlineTexture;
   public HashSet<Integer> connectedWalls = new HashSet<>();
   public int windowID = -1;

   public static void setupWallTextures() {
      wallTextures = new SharedGameTexture("wallsShared");
   }

   public static void generateWallTextures() {
      generatedWallTexture = wallTextures.generate();
      wallTextures.close();
   }

   public WallObject(String textureName, String outlineTextureName, Color mapColor, float toolTier, ToolType toolType) {
      super(new Rectangle(32, 32));
      this.textureName = textureName;
      this.outlineTextureName = outlineTextureName;
      this.mapColor = mapColor;
      this.toolTier = toolTier;
      this.toolType = toolType;
      this.regionType = RegionType.WALL;
      this.setItemCategory("objects", "wallsanddoors");
      this.setCraftingCategory("objects", "wallsanddoors");
      this.isWall = true;
      this.stackSize = 500;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.replaceCategories.add("wall");
      this.canReplaceCategories.add("wall");
      this.canReplaceCategories.add("door");
      this.canReplaceCategories.add("fence");
      this.canReplaceCategories.add("fencegate");
      this.replaceRotations = false;
   }

   @Override
   public void onObjectRegistryClosed() {
      super.onObjectRegistryClosed();
      this.connectedWalls.add(this.getID());
   }

   public GameMessage getNewTrapLocalization(GameMessage trap) {
      return new LocalMessage("object", this.textureName + "trap", "trap", trap);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();

      try {
         this.wallTexture = wallTextures.addTexture(GameTexture.fromFileRaw("objects/" + this.textureName + "_short"));
      } catch (FileNotFoundException e) {
         this.wallTexture = wallTextures.addTexture(GameTexture.fromFile("objects/" + this.textureName));
      }

      if (this.outlineTextureName != null) {
         if (outlineTextures.containsKey(this.outlineTextureName)) {
            this.outlineTexture = outlineTextures.get(this.outlineTextureName);
         } else {
            GameTexture outlineTexture;
            try {
               outlineTexture = GameTexture.fromFileRaw("objects/" + this.outlineTextureName + "_short");
            } catch (FileNotFoundException e1) {
               try {
                  outlineTexture = GameTexture.fromFileRaw("objects/" + this.outlineTextureName);
               } catch (FileNotFoundException e2) {
                  outlineTexture = null;
               }
            }

            this.outlineTexture = outlineTexture == null ? null : wallTextures.addTexture(outlineTexture);
            outlineTextures.put(this.outlineTextureName, this.outlineTexture);
         }
      }
   }

   public static float[] getAdvancedLight(GameLight[] lights, float alpha, int spriteCoordX, int spriteCoordY) {
      GameLight centerLight = lights[4];
      GameLight l0;
      GameLight l1;
      GameLight l2;
      GameLight l3;
      if (spriteCoordX == 0) {
         if (spriteCoordY == -1) {
            l0 = centerLight.average(lights[0], lights[1], lights[3]);
            l1 = centerLight.mix(lights[1]);
            l2 = centerLight;
            l3 = centerLight.mix(lights[3]);
         } else if (spriteCoordY == 0) {
            l0 = centerLight.mix(lights[3]);
            l1 = centerLight;
            l2 = centerLight.mix(lights[7]);
            l3 = centerLight.average(lights[6], lights[7], lights[3]);
         } else {
            centerLight = lights[7];
            l0 = centerLight.average(lights[3], lights[4], lights[6]);
            l1 = centerLight.mix(lights[4]);
            l2 = centerLight;
            l3 = centerLight.mix(lights[6]);
         }
      } else if (spriteCoordY == -1) {
         l0 = centerLight.mix(lights[1]);
         l1 = centerLight.average(lights[2], lights[1], lights[5]);
         l2 = centerLight.mix(lights[5]);
         l3 = centerLight;
      } else if (spriteCoordY == 0) {
         l0 = centerLight;
         l1 = centerLight.mix(lights[5]);
         l2 = centerLight.average(lights[8], lights[7], lights[5]);
         l3 = centerLight.mix(lights[7]);
      } else {
         centerLight = lights[7];
         l0 = centerLight.mix(lights[4]);
         l1 = centerLight.average(lights[5], lights[4], lights[8]);
         l2 = centerLight.mix(lights[8]);
         l3 = centerLight;
      }

      return l0.getAdvColor(l1, l2, l3, alpha);
   }

   public SharedTextureDrawOptions.Wrapper applyLights(
      SharedTextureDrawOptions.Wrapper wrapper, GameLight[] lights, float alpha, int spriteCoordX, int spriteCoordY
   ) {
      return Settings.smoothLighting
         ? wrapper.advColor(getAdvancedLight(lights, alpha, spriteCoordX, spriteCoordY))
         : wrapper.light(spriteCoordY == 1 ? lights[7] : lights[4]).alpha(alpha);
   }

   protected void addWallDrawOptions(
      SharedTextureDrawOptions options,
      GameTextureSection texture,
      float alpha,
      int drawX,
      int drawY,
      GameLight[] lights,
      boolean[] adj,
      boolean[] sameWall,
      boolean[] isWall,
      boolean forceDrawTop,
      boolean forceRemoveBot
   ) {
      boolean top = adj[1];
      boolean left = adj[3];
      boolean right = adj[4];
      boolean botLeft = adj[5];
      boolean bot = adj[6];
      boolean botRight = adj[7];
      boolean topWall = isWall[1];
      if (!top) {
         if (topWall) {
            alpha = 1.0F;
         }

         if (left) {
            this.applyLights(options.add(texture.sprite(2, 0, 16)), lights, alpha, 0, -1).pos(drawX, drawY - 16);
         } else {
            this.applyLights(options.add(texture.sprite(0, 0, 16)), lights, alpha, 0, -1).pos(drawX, drawY - 16);
         }

         if (right) {
            this.applyLights(options.add(texture.sprite(1, 0, 16)), lights, alpha, 1, -1).pos(drawX + 16, drawY - 16);
         } else {
            this.applyLights(options.add(texture.sprite(3, 0, 16)), lights, alpha, 1, -1).pos(drawX + 16, drawY - 16);
         }
      } else {
         boolean topLeft = adj[0];
         boolean topRight = adj[2];
         if (isWall[1] && (forceDrawTop || !sameWall[1])) {
            if (!left) {
               this.applyLights(options.add(texture.sprite(0, 1, 16)), lights, forceDrawTop ? 1.0F : alpha, 0, -1).pos(drawX, drawY - 16);
            } else if (!topLeft) {
               this.applyLights(options.add(texture.sprite(0, 7, 16)), lights, forceDrawTop ? 1.0F : alpha, 0, -1).pos(drawX, drawY - 16);
            }

            if (!right) {
               this.applyLights(options.add(texture.sprite(3, 1, 16)), lights, forceDrawTop ? 1.0F : alpha, 1, 1).pos(drawX + 16, drawY - 16);
            } else if (!topRight) {
               this.applyLights(options.add(texture.sprite(1, 7, 16)), lights, forceDrawTop ? 1.0F : alpha, 1, 1).pos(drawX + 16, drawY - 16);
            }
         }

         if (left && (!bot || !botLeft) && topLeft) {
            if (right && topRight) {
               this.applyLights(options.add(texture.sprite(2, 1, 16)), lights, 1.0F, 1, -1).pos(drawX + 16, drawY - 16);
            }

            this.applyLights(options.add(texture.sprite(1, 1, 16)), lights, 1.0F, 0, -1).pos(drawX, drawY - 16);
         }

         if (right && (!bot || !botRight) && topRight) {
            this.applyLights(options.add(texture.sprite(2, 1, 16)), lights, 1.0F, 1, -1).pos(drawX + 16, drawY - 16);
            if (left && topLeft) {
               this.applyLights(options.add(texture.sprite(1, 1, 16)), lights, 1.0F, 0, -1).pos(drawX, drawY - 16);
            }
         }
      }

      if (bot) {
         if (left) {
            if (botLeft) {
               this.applyLights(options.add(texture.sprite(1, 2, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
               if (!forceRemoveBot) {
                  this.applyLights(options.add(texture.sprite(1, 1, 16)), lights, 1.0F, 0, 1).pos(drawX, drawY + 16);
               }
            } else {
               boolean botLeftWall = isWall[5];
               if (botLeftWall) {
                  this.applyLights(options.add(texture.sprite(3, 7, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
               } else {
                  this.applyLights(options.add(texture.sprite(0, 5, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
               }

               if (!forceRemoveBot) {
                  this.applyLights(options.add(texture.sprite(0, 6, 16)), lights, 1.0F, 0, 1).pos(drawX, drawY + 16);
               }
            }
         } else if (botLeft) {
            this.applyLights(options.add(texture.sprite(0, 2, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
            if (!forceRemoveBot) {
               this.applyLights(options.add(texture.sprite(0, 7, 16)), lights, 1.0F, 0, 1).pos(drawX, drawY + 16);
            }
         } else {
            this.applyLights(options.add(texture.sprite(0, 2, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
            if (!forceRemoveBot) {
               this.applyLights(options.add(texture.sprite(0, 1, 16)), lights, 1.0F, 0, 1).pos(drawX, drawY + 16);
            }
         }

         if (right) {
            if (botRight) {
               this.applyLights(options.add(texture.sprite(2, 2, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
               if (!forceRemoveBot) {
                  this.applyLights(options.add(texture.sprite(2, 1, 16)), lights, 1.0F, 1, 1).pos(drawX + 16, drawY + 16);
               }
            } else {
               boolean botRightWall = isWall[7];
               if (botRightWall) {
                  this.applyLights(options.add(texture.sprite(2, 7, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
               } else {
                  this.applyLights(options.add(texture.sprite(1, 5, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
               }

               if (!forceRemoveBot) {
                  this.applyLights(options.add(texture.sprite(1, 6, 16)), lights, 1.0F, 1, 1).pos(drawX + 16, drawY + 16);
               }
            }
         } else if (botRight) {
            this.applyLights(options.add(texture.sprite(3, 2, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
            if (!forceRemoveBot) {
               this.applyLights(options.add(texture.sprite(1, 7, 16)), lights, 1.0F, 1, 1).pos(drawX + 16, drawY + 16);
            }
         } else {
            this.applyLights(options.add(texture.sprite(3, 2, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
            if (!forceRemoveBot) {
               this.applyLights(options.add(texture.sprite(3, 1, 16)), lights, 1.0F, 1, 1).pos(drawX + 16, drawY + 16);
            }
         }
      } else {
         boolean botWall = isWall[6];
         if (left) {
            if (botWall) {
               this.applyLights(options.add(texture.sprite(3, 5, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
            } else {
               this.applyLights(options.add(texture.sprite(2, 3, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
               this.applyLights(options.add(texture.sprite(2, 4, 16)), lights, 1.0F, 0, 1).pos(drawX, drawY + 16);
            }
         } else if (botWall) {
            this.applyLights(options.add(texture.sprite(2, 5, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
         } else {
            this.applyLights(options.add(texture.sprite(0, 3, 16)), lights, 1.0F, 0, 0).pos(drawX, drawY);
            this.applyLights(options.add(texture.sprite(0, 4, 16)), lights, 1.0F, 0, 1).pos(drawX, drawY + 16);
         }

         if (right) {
            if (botWall) {
               this.applyLights(options.add(texture.sprite(2, 6, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
            } else {
               this.applyLights(options.add(texture.sprite(1, 3, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
               this.applyLights(options.add(texture.sprite(1, 4, 16)), lights, 1.0F, 1, 1).pos(drawX + 16, drawY + 16);
            }
         } else if (botWall) {
            this.applyLights(options.add(texture.sprite(3, 6, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
         } else {
            this.applyLights(options.add(texture.sprite(3, 3, 16)), lights, 1.0F, 1, 0).pos(drawX + 16, drawY);
            this.applyLights(options.add(texture.sprite(3, 4, 16)), lights, 1.0F, 1, 1).pos(drawX + 16, drawY + 16);
         }
      }
   }

   public void addWallDrawOptions(
      SharedTextureDrawOptions options,
      GameTextureSection wallTexture,
      int drawX,
      int drawY,
      GameLight[] lights,
      float alpha,
      boolean[] sameWall,
      boolean allIsSameWall,
      boolean forceDrawTop,
      boolean forceRemoveBot
   ) {
      if (allIsSameWall) {
         options.add(wallTexture.section(16, 32, 16, 32)).advColor(getAdvancedLight(lights, 1.0F, 0, -1)).pos(drawX, drawY - 16);
         options.add(wallTexture.section(32, 48, 16, 32)).advColor(getAdvancedLight(lights, 1.0F, 1, -1)).pos(drawX + 16, drawY - 16);
         options.add(wallTexture.section(16, 32, 32, 48)).advColor(getAdvancedLight(lights, 1.0F, 0, 0)).pos(drawX, drawY);
         options.add(wallTexture.section(32, 48, 32, 48)).advColor(getAdvancedLight(lights, 1.0F, 1, 0)).pos(drawX + 16, drawY);
      } else {
         this.addWallDrawOptions(options, wallTexture, alpha, drawX, drawY, lights, sameWall, sameWall, sameWall, forceDrawTop, forceRemoveBot);
         if (this.outlineTexture != null) {
            this.addWallDrawOptions(options, this.outlineTexture, alpha, drawX, drawY, lights, sameWall, sameWall, sameWall, forceDrawTop, forceRemoveBot);
         }
      }
   }

   public void addWallDrawOptions(
      SharedTextureDrawOptions options,
      Level level,
      int tileX,
      int tileY,
      GameLight lightOverride,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      Performance.record(
         tickManager,
         "wallSetup",
         () -> {
            int drawX = camera.getTileDrawX(tileX);
            int drawY = camera.getTileDrawY(tileY);
            GameObject[] adj = level.getAdjacentObjects(tileX, tileY);
            boolean allIsSameWall = true;
            boolean[] sameWall = new boolean[adj.length];
            boolean forceDrawTop = false;
            boolean forceRemoveBot = false;

            for (int i = 0; i < adj.length; i++) {
               GameObject adjObject = adj[i];
               boolean connectedWall = this.isConnectedWall(adjObject);
               sameWall[i] = connectedWall;
               allIsSameWall = allIsSameWall && connectedWall;
               if (connectedWall) {
                  if (i == 1) {
                     if (adjObject instanceof WallObject && ((WallObject)adjObject).isWallDrawingTop()) {
                        forceDrawTop = true;
                     }
                  } else if (i == 6 && adjObject instanceof WallObject && ((WallObject)adjObject).isWallDrawingTop()) {
                     forceRemoveBot = true;
                  }
               }
            }

            float alpha = 1.0F;
            if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
               Rectangle alphaRec = new Rectangle(tileX * 32 - 16, tileY * 32 - 32, 64, 48);
               if (perspective.getCollision().intersects(alphaRec)) {
                  alpha = 0.5F;
               } else if (alphaRec.contains(
                  camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY
               )) {
                  alpha = 0.5F;
               }
            }

            GameLight[] lights;
            if (lightOverride == null) {
               lights = level.getRelative(tileX, tileY, Level.adjacentGettersWithCenter, level::getLightLevelWall, GameLight[]::new);
            } else {
               lights = new GameLight[9];
               Arrays.fill(lights, lightOverride);
            }

            this.addWallDrawOptions(options, this.wallTexture, drawX, drawY, lights, alpha, sameWall, allIsSameWall, forceDrawTop, forceRemoveBot);
         }
      );
   }

   public boolean isConnectedWall(GameObject object) {
      return object == this || this.connectedWalls.contains(object.getID());
   }

   public boolean isWallDrawingTop() {
      return false;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      final SharedTextureDrawOptions options = new SharedTextureDrawOptions(generatedWallTexture).addObjectDamageOverlay(this, level, tileX, tileY);
      this.addWallDrawOptions(options, level, tileX, tileY, null, tickManager, camera, perspective);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 20;
         }

         @Override
         public void draw(TickManager tickManager) {
            Performance.record(tickManager, "wallDraw", () -> options.draw());
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      SharedTextureDrawOptions options = new SharedTextureDrawOptions(generatedWallTexture);
      this.addWallDrawOptions(options, level, tileX, tileY, level.lightManager.newLight(150.0F), null, camera, player);
      options.forEachDraw(w -> w.alpha(0.5F)).draw();
   }

   @Override
   public boolean stopsTerrainSplatting() {
      return true;
   }

   @Override
   public boolean drawsFullTile() {
      return true;
   }

   public static int[] registerWallObjects(
      String stringIDPrefix,
      String textureName,
      String outlineTextureName,
      float toolTier,
      Color mapColor,
      ToolType toolType,
      float wallBrokerValue,
      float doorBrokerValue,
      boolean itemObtainable,
      boolean itemCountInStats
   ) {
      WallObject wallObject = new WallObject(textureName, outlineTextureName, mapColor, toolTier, toolType);
      int wall = ObjectRegistry.registerObject(stringIDPrefix + "wall", wallObject, wallBrokerValue, itemObtainable, itemCountInStats);
      int[] doors = WallDoorObject.registerDoorPair(stringIDPrefix + "door", wallObject, doorBrokerValue, itemObtainable, itemCountInStats);
      int window = ObjectRegistry.registerObject(stringIDPrefix + "window", new WallWindowObject(wallObject), wallBrokerValue, false, false);
      return new int[]{wall, doors[0], doors[1], window};
   }

   public static int[] registerWallObjects(
      String stringIDPrefix,
      String textureName,
      String outlineTextureName,
      float toolTier,
      Color mapColor,
      ToolType toolType,
      float wallBrokerValue,
      float doorBrokerValue,
      boolean itemObtainable
   ) {
      return registerWallObjects(
         stringIDPrefix, textureName, outlineTextureName, toolTier, mapColor, toolType, wallBrokerValue, doorBrokerValue, itemObtainable, itemObtainable
      );
   }

   public static int[] registerWallObjects(
      String stringIDPrefix,
      String textureName,
      String outlineTextureName,
      float toolTier,
      Color mapColor,
      ToolType toolType,
      float wallBrokerValue,
      float doorBrokerValue
   ) {
      return registerWallObjects(stringIDPrefix, textureName, outlineTextureName, toolTier, mapColor, toolType, wallBrokerValue, doorBrokerValue, true);
   }

   public static int[] registerWallObjects(
      String stringIDPrefix,
      String textureName,
      float toolTier,
      Color mapColor,
      ToolType toolType,
      float wallBrokerValue,
      float doorBrokerValue,
      boolean itemObtainable,
      boolean itemCountInStats
   ) {
      return registerWallObjects(
         stringIDPrefix, textureName, "walloutlines", toolTier, mapColor, toolType, wallBrokerValue, doorBrokerValue, itemObtainable, itemCountInStats
      );
   }

   public static int[] registerWallObjects(
      String stringIDPrefix,
      String textureName,
      float toolTier,
      Color mapColor,
      ToolType toolType,
      float wallBrokerValue,
      float doorBrokerValue,
      boolean itemObtainable
   ) {
      return registerWallObjects(stringIDPrefix, textureName, toolTier, mapColor, toolType, wallBrokerValue, doorBrokerValue, itemObtainable, itemObtainable);
   }

   public static int[] registerWallObjects(
      String stringIDPrefix, String textureName, float toolTier, Color mapColor, ToolType toolType, float wallBrokerValue, float doorBrokerValue
   ) {
      return registerWallObjects(stringIDPrefix, textureName, toolTier, mapColor, toolType, wallBrokerValue, doorBrokerValue, true);
   }

   public static int[] registerWallObjects(
      String stringIDPrefix, String textureName, String outlineTextureName, float toolTier, Color mapColor, float wallBrokerValue, float doorBrokerValue
   ) {
      return registerWallObjects(stringIDPrefix, textureName, outlineTextureName, toolTier, mapColor, ToolType.PICKAXE, wallBrokerValue, doorBrokerValue);
   }

   public static int[] registerWallObjects(
      String stringIDPrefix, String textureName, float toolTier, Color mapColor, float wallBrokerValue, float doorBrokerValue
   ) {
      return registerWallObjects(stringIDPrefix, textureName, toolTier, mapColor, ToolType.PICKAXE, wallBrokerValue, doorBrokerValue);
   }
}
