package necesse.level.gameObject;

import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BrokenPaintingObject extends PaintingObject {
   protected final GameRandom drawRandom = new GameRandom();

   public BrokenPaintingObject(Item.Rarity rarity) {
      super(rarity);
      this.hasBeautyRecreation = false;
      this.roomProperties.remove("painting");
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
      int rndX;
      synchronized (this.drawRandom) {
         rndX = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
      }

      int rotation = level.getObjectRotation(tileX, tileY);
      final DrawOptionsList options = new DrawOptionsList();
      final int sortY;
      if (rotation == 0) {
         sortY = 32;
         options.add(this.texture.initDraw().sprite(rndX, 2, 32, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY + 8));
      } else if (rotation == 1) {
         sortY = 24;
         options.add(this.texture.initDraw().sprite(rndX, 3, 32, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 16));
      } else if (rotation == 2) {
         sortY = 0;
         options.add(this.texture.initDraw().sprite(rndX, 0, 32, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
      } else {
         sortY = 24;
         options.add(this.texture.initDraw().sprite(rndX, 1, 32, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 16));
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
      int rndX;
      synchronized (this.drawRandom) {
         rndX = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
      }

      if (rotation == 0) {
         this.texture.initDraw().sprite(rndX, 2, 32, 32).alpha(alpha).draw(drawX, drawY + 8);
      } else if (rotation == 1) {
         this.texture.initDraw().sprite(rndX, 3, 32, 32).alpha(alpha).draw(drawX, drawY - 16);
      } else if (rotation == 2) {
         this.texture.initDraw().sprite(rndX, 0, 32, 32).alpha(alpha).draw(drawX, drawY - 32);
      } else {
         this.texture.initDraw().sprite(rndX, 1, 32, 32).alpha(alpha).draw(drawX, drawY - 16);
      }
   }
}
