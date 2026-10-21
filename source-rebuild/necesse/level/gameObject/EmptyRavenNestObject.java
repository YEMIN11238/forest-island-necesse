package necesse.level.gameObject;

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
import necesse.level.maps.Level;

public class EmptyRavenNestObject extends StaticMultiObject {
   protected final GameRandom drawRandom;

   public EmptyRavenNestObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision, String texturePath) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, texturePath);
      this.displayMapTooltip = true;
      this.drawDamage = false;
      this.drawRandom = new GameRandom();
      this.isLightTransparent = true;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "landscaping", "misc");
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
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 64);
      final DrawOptions options = this.getMultiTextureDrawOptions(sprite, level, tileX, tileY, camera);
      tileList.add(new LevelSortedDrawable(this, tileX, tileY) {
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
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 64);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerEmptyRavenNest(String texturePath) {
      int[] ids = new int[4];
      Rectangle collision = new Rectangle(0, 0, 0, 0);
      ids[0] = ObjectRegistry.registerObject(texturePath, new EmptyRavenNestObject(0, 0, 2, 2, ids, collision, texturePath), -1.0F, true);
      ids[1] = ObjectRegistry.registerObject(texturePath + "2", new EmptyRavenNestObject(1, 0, 2, 2, ids, collision, texturePath), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject(texturePath + "3", new EmptyRavenNestObject(0, 1, 2, 2, ids, collision, texturePath), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject(texturePath + "4", new EmptyRavenNestObject(1, 1, 2, 2, ids, collision, texturePath), 0.0F, false);
      return ids;
   }
}
