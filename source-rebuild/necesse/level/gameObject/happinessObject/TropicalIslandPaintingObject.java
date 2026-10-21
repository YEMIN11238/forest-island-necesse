package necesse.level.gameObject.happinessObject;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.ObjectHoverHitbox;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TropicalIslandPaintingObject extends GameObject implements HappinessObject {
   public GameTexture texture;
   protected String textureName;
   protected String itemDroppedStringID;

   public TropicalIslandPaintingObject(String textureName) {
      this.textureName = textureName;
      this.rarity = Item.Rarity.EPIC;
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.setItemCategory("objects", "decorations", "paintings");
      this.validObjectLayers.add(ObjectLayerRegistry.WALL_DECOR);
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
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      byte rotation = level.getObjectRotation(layerID, tileX, tileY);
      int sprite = this.getSprite(level, tileX, tileY, rotation);
      if (sprite == 0) {
         drawY -= 16;
      } else if (sprite == 2) {
         drawY += 16;
      }

      final TextureDrawOptions options = this.texture.initDraw().sprite(0, sprite, 32).light(light).pos(drawX, drawY - 16);
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
            options.draw();
         }
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
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
