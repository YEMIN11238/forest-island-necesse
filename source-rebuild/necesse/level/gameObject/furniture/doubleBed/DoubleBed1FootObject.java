package necesse.level.gameObject.furniture.doubleBed;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.SidedRotationMultiTile;

class DoubleBed1FootObject extends DoubleBedFootBaseObject {
   protected int head1ID;
   protected int head2ID;
   protected int foot2ID;

   protected DoubleBed1FootObject(String textureName, ToolType toolType, Color mapColor, String... category) {
      super(textureName, toolType, mapColor);
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
      return new SidedRotationMultiTile(1, 0, 2, 2, rotation, false, this.foot2ID, this.getID(), this.head2ID, this.head1ID);
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "usetip");
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      this.getMultiTile(level.getObjectRotation(x, y))
         .streamObjects(x, y)
         .filter(e -> e.value.getID() == this.head1ID)
         .findFirst()
         .map(e -> new LevelObject(level, 0, e.tileX, e.tileY))
         .ifPresent(e -> e.interact(player));
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      if (rotation == 0) {
         return new Rectangle(x * 32, y * 32 + 6, 30, 26);
      } else if (rotation == 1) {
         return new Rectangle(x * 32, y * 32, 30, 30);
      } else {
         return rotation == 2 ? new Rectangle(x * 32 + 2, y * 32, 30, 30) : new Rectangle(x * 32 + 2, y * 32 + 6, 30, 26);
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
         options.add(this.baseTexture.initDraw().sprite(3, 3, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.baseTexture.initDraw().sprite(3, 4, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else if (rotation == 1) {
         options.add(this.baseTexture.initDraw().sprite(1, 5, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else if (rotation == 2) {
         options.add(this.baseTexture.initDraw().sprite(2, 2, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else {
         options.add(this.baseTexture.initDraw().sprite(0, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.baseTexture.initDraw().sprite(0, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 20;
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
      if (rotation == 0) {
         this.baseTexture.initDraw().sprite(3, 3, 32).alpha(alpha).draw(drawX, drawY - 32);
         this.baseTexture.initDraw().sprite(3, 4, 32).alpha(alpha).draw(drawX, drawY);
      } else if (rotation == 1) {
         this.baseTexture.initDraw().sprite(1, 5, 32).alpha(alpha).draw(drawX, drawY);
      } else if (rotation == 2) {
         this.baseTexture.initDraw().sprite(2, 2, 32).alpha(alpha).draw(drawX, drawY);
      } else {
         this.baseTexture.initDraw().sprite(0, 0, 32).alpha(alpha).draw(drawX, drawY - 32);
         this.baseTexture.initDraw().sprite(0, 1, 32).alpha(alpha).draw(drawX, drawY);
      }
   }
}
