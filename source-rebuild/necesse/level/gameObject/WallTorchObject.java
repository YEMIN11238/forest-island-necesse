package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.projectile.BombProjectile;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WallTorchObject extends GameObject {
   public GameTexture texture;
   public boolean disableParticles = false;
   protected String itemDroppedStringID;

   public WallTorchObject() {
      this.mapColor = new Color(255, 255, 152);
      this.stackSize = 500;
      this.displayMapTooltip = true;
      this.lightLevel = 150;
      this.lightHue = 50.0F;
      this.lightSat = 0.2F;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.setItemCategory("objects", "lighting");
      this.setCraftingCategory("objects", "lighting");
      this.roomProperties.add("lights");
      this.validObjectLayers.add(ObjectLayerRegistry.WALL_DECOR);
   }

   public WallTorchObject setItemDroppedStringID(String itemStringID) {
      if (ObjectRegistry.instance.isClosed()) {
         throw new IllegalStateException("Cannot set wall torch drop once object registry is closed");
      }

      this.itemDroppedStringID = itemStringID;
      return this;
   }

   public String getItemDroppedStringID() {
      return this.itemDroppedStringID;
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return this.itemDroppedStringID != null
         ? new LootTable(new LootItem(this.itemDroppedStringID).preventLootMultiplier())
         : super.getLootTable(level, layerID, tileX, tileY);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/walltorch");
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      if (!this.disableParticles) {
         if (GameRandom.globalRandom.getEveryXthChance(40) && this.isActive(level, layerID, tileX, tileY)) {
            byte rotation = level.getObjectRotation(layerID, tileX, tileY);
            int sprite = this.getSprite(level, tileX, tileY, rotation);
            int startHeight = 10;
            if (sprite == 0) {
               startHeight += 32;
            } else if (sprite == 1 || sprite == 3) {
               startHeight += 14;
            }

            BombProjectile.spawnFuseParticle(level, tileX * 32 + 16, tileY * 32 + 16 + 2, startHeight);
         }
      }
   }

   @Override
   public void addLayerDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      Performance.record(tickManager, "torchSetup", () -> {
         GameLight light = level.getLightLevel(tileX, tileY);
         int drawX = camera.getTileDrawX(tileX);
         int drawY = camera.getTileDrawY(tileY);
         byte rotation = level.getObjectRotation(layerID, tileX, tileY);
         int sprite = this.getSprite(level, tileX, tileY, rotation);
         boolean active = this.isActive(level, layerID, tileX, tileY);
         if (sprite == 0) {
            drawY -= 16;
         } else if (sprite == 2) {
            drawY += 16;
         }

         final TextureDrawOptions options = this.texture.initDraw().sprite(active ? 0 : 1, sprite, 32).light(light).pos(drawX, drawY - 16);
         final int sortY;
         if (sprite == 0) {
            sortY = 0;
         } else if (sprite == 2) {
            sortY = 32;
         } else {
            sortY = 24;
         }

         list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
               return sortY;
            }

            @Override
            public void draw(TickManager tickManager) {
               Performance.record(tickManager, "torchDraw", options::draw);
            }
         });
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int sprite = this.getSprite(level, tileX, tileY, rotation);
      if (sprite == 0) {
         drawY -= 16;
      } else if (sprite == 2) {
         drawY += 16;
      }

      this.texture.initDraw().sprite(0, sprite, 32).alpha(alpha).draw(drawX, drawY - 16);
   }

   protected int getSprite(Level level, int tileX, int tileY, int rotation) {
      boolean attachTop = this.attachesToObject(level, tileX, tileY - 1);
      boolean attachRight = this.attachesToObject(level, tileX + 1, tileY);
      boolean attachBot = this.attachesToObject(level, tileX, tileY + 1);
      boolean attachLeft = this.attachesToObject(level, tileX - 1, tileY);
      if (attachTop) {
         if (rotation == 0 && attachBot) {
            return 2;
         } else if (rotation == 3 && attachRight) {
            return 1;
         } else {
            return rotation == 1 && attachLeft ? 3 : 0;
         }
      } else if (attachBot) {
         if (rotation == 3 && attachRight) {
            return 1;
         } else {
            return rotation == 1 && attachLeft ? 3 : 2;
         }
      } else if (attachRight) {
         return rotation == 1 && attachLeft ? 3 : 1;
      } else {
         return attachLeft ? 3 : 0;
      }
   }

   @Override
   public ArrayList<ObjectPlaceOption> getPlaceOptions(Level level, int levelX, int levelY, PlayerMob playerMob, int playerDir, boolean offsetMultiTile) {
      ArrayList<ObjectPlaceOption> options = new ArrayList<>();

      for (int i = 0; i < 4; i++) {
         int currentRotation = (playerDir + i) % 4;
         options.addAll(super.getPlaceOptions(level, levelX, levelY, playerMob, currentRotation, offsetMultiTile));
      }

      return options;
   }

   @Override
   protected ObjectHoverHitbox getHoverHitbox(Level level, int layerID, int tileX, int tileY) {
      byte rotation = level.getObjectRotation(layerID, tileX, tileY);
      int sprite = this.getSprite(level, tileX, tileY, rotation);
      if (sprite == 0) {
         return new ObjectHoverHitbox(layerID, tileX, tileY, 0, -32, 32, 64, 0);
      } else {
         return sprite != 1 && sprite != 3
            ? new ObjectHoverHitbox(layerID, tileX, tileY, 0, 0, 32, 32, 32)
            : new ObjectHoverHitbox(layerID, tileX, tileY, 0, -16, 32, 48, 32);
      }
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      if (this.isTilePlaceOccupied(level, layerID, x, y, ignoreOtherLayers)) {
         return "occupied";
      }

      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return "tilecovered";
         }
      }

      if (rotation == 0 && this.attachesToObject(level, x, y + 1)) {
         return null;
      } else if (rotation == 1 && this.attachesToObject(level, x - 1, y)) {
         return null;
      } else if (rotation == 2 && this.attachesToObject(level, x, y - 1)) {
         return null;
      } else {
         return rotation == 3 && this.attachesToObject(level, x + 1, y) ? null : "noattachment";
      }
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return false;
         }
      }

      for (Point p : Level.adjacentGettersNotDiagonal) {
         if (this.attachesToObject(level, x + p.x, y + p.y)) {
            return true;
         }
      }

      return false;
   }

   public boolean attachesToObject(Level level, int tileX, int tileY) {
      GameObject object = level.getObject(tileX, tileY);
      return (object.isWall || object.isRock) && !object.isDoor;
   }

   @Override
   public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
      return this.isActive(level, layerID, tileX, tileY) ? 150 : 0;
   }

   public boolean isActive(Level level, int layerID, int tileX, int tileY) {
      return this.getMultiTile(level, layerID, tileX, tileY).streamIDs(tileX, tileY).noneMatch(c -> level.wireManager.isWireActiveAny(c.tileX, c.tileY));
   }

   @Override
   public void onWireUpdate(Level level, int layerID, int tileX, int tileY, int wireID, boolean active) {
      Rectangle rect = this.getMultiTile(level, layerID, tileX, tileY).getTileRectangle(tileX, tileY);
      level.lightManager.updateStaticLight(rect.x, rect.y, rect.x + rect.width - 1, rect.y + rect.height - 1, true);
   }
}
