package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;

public class AncientPillarObject extends StaticMultiObject {
   protected final GameRandom drawRandom;

   protected AncientPillarObject(String texturePath, int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "statues/" + texturePath);
      this.stackSize = 1;
      this.rarity = Item.Rarity.NORMAL;
      this.mapColor = new Color(143, 143, 143);
      this.objectHealth = 100;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, 0, 32, 32);
      this.setItemCategory("objects", "landscaping", "misc");
      this.drawRandom = new GameRandom();
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
      int SpriteWidth = 96;
      int spriteCount = this.texture.getWidth() / SpriteWidth;
      int spriteX = 0;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX - this.multiX, tileY - this.multiY));
         if (spriteCount > 1) {
            spriteX = this.drawRandom.nextInt(spriteCount);
         }
      }

      GameSprite sprite = new GameSprite(this.texture, spriteX, 0, 96, 224, 96, 224);
      final DrawOptions options = this.getMultiTextureDrawOptions(sprite, level, tileX, tileY, camera);
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
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int SpriteWidth = 96;
      int spriteCount = this.texture.getWidth() / SpriteWidth;
      int spriteX = 0;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX - this.multiX, tileY - this.multiY));
         if (spriteCount > 1) {
            spriteX = this.drawRandom.nextInt(spriteCount);
         }
      }

      GameSprite sprite = new GameSprite(this.texture, spriteX, 0, 96, 224, 96, 224);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerAncientPillar(String texturePath, boolean isObtainable) {
      int[] ids = new int[6];
      Rectangle collision = new Rectangle(0, 0, 96, 64);
      ids[0] = ObjectRegistry.registerObject(texturePath, new AncientPillarObject(texturePath, 0, 0, 3, 2, ids, collision), 125.0F, isObtainable);
      ids[1] = ObjectRegistry.registerObject(texturePath + "1", new AncientPillarObject(texturePath, 1, 0, 3, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject(texturePath + "2", new AncientPillarObject(texturePath, 2, 0, 3, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject(texturePath + "3", new AncientPillarObject(texturePath, 0, 1, 3, 2, ids, collision), 0.0F, false);
      ids[4] = ObjectRegistry.registerObject(texturePath + "4", new AncientPillarObject(texturePath, 1, 1, 3, 2, ids, collision), 0.0F, false);
      ids[5] = ObjectRegistry.registerObject(texturePath + "5", new AncientPillarObject(texturePath, 2, 1, 3, 2, ids, collision), 0.0F, false);
      return ids;
   }
}
