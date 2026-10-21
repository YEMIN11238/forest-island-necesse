package necesse.level.gameObject.furniture;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.DecorDrawOffset;
import necesse.level.gameObject.DecorationHolderInterface;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.TorchHolderInterface;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;

class DinnerTable2Object extends TableObject implements TorchHolderInterface, DecorationHolderInterface {
   protected String textureName;
   public GameTexture texture;
   protected int counterID;

   protected DinnerTable2Object(String textureName, ToolType toolType, Color mapColor, String... category) {
      super(new Rectangle(32, 32), mapColor);
      this.textureName = textureName;
      this.counterID = -1;
      this.toolType = toolType;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -10, 32, 42);
      if (category.length > 0) {
         this.setItemCategory(category);
         this.setCraftingCategory(category);
      } else {
         this.setItemCategory("objects", "furniture");
         this.setCraftingCategory("objects", "furniture");
      }
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new MultiTile(0, 0, 1, 2, rotation, false, this.getID(), this.counterID);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public DecorDrawOffset getTorchDrawOffset(Level level, int tileX, int tileY) {
      return new DecorDrawOffset(0, -18, 20, true);
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      if (rotation == 0) {
         return new Rectangle(x * 32 + 6, y * 32 + 6, 20, 26);
      } else if (rotation == 1) {
         return new Rectangle(x * 32, y * 32 + 6, 26, 20);
      } else {
         return rotation == 2 ? new Rectangle(x * 32 + 6, y * 32, 20, 26) : new Rectangle(x * 32 + 6, y * 32 + 6, 26, 20);
      }
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int rotation = level.getObjectRotation(tileX, tileY);
      final DrawOptionsList options = new DrawOptionsList();
      if (rotation == 0) {
         options.add(this.texture.initDraw().sprite(3, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(3, 2, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else if (rotation == 1) {
         options.add(this.texture.initDraw().sprite(1, 1, 32, 64).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
      } else if (rotation == 2) {
         options.add(this.texture.initDraw().sprite(2, 3, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else {
         options.add(this.texture.initDraw().sprite(0, 0, 32, 64).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable();
   }

   @Override
   public DecorDrawOffset getDecorationDrawOffset(Level level, int tileX, int tileY, GameObject decoration) {
      int objectRotation = level.getObjectRotation(tileX, tileY);
      switch (objectRotation) {
         case 0:
            return new DecorDrawOffset(0, -14, 20, true);
         case 1:
            return new DecorDrawOffset(-2, -18, 20, true);
         case 2:
            return new DecorDrawOffset(0, -20, 20, true);
         case 3:
            return new DecorDrawOffset(2, -18, 20, true);
         default:
            return new DecorDrawOffset(0, -18, 20, true);
      }
   }

   @Override
   public boolean canPlaceDecoration(Level level, int tileX, int tileY) {
      return !this.isTilePlaceOccupied(level, ObjectLayerRegistry.FENCE_AND_TABLE_DECOR, tileX, tileY, true);
   }

   @Override
   public Dimension getMaxDecorationSize(Level level, int tileX, int tileY) {
      return new Dimension(28, 28);
   }
}
