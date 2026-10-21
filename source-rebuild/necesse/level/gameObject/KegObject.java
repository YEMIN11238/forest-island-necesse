package necesse.level.gameObject;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class KegObject extends GameObject {
   public GameTexture texture;
   public final String texturePath;
   public int statueXOffset;
   public int spriteCount;

   public KegObject(String texturePath) {
      super(new Rectangle(2, 2, 28, 28));
      this.displayMapTooltip = true;
      this.stackSize = 10;
      this.objectHealth = 100;
      this.isLightTransparent = true;
      this.rarity = Item.Rarity.NORMAL;
      this.toolType = ToolType.PICKAXE;
      this.texturePath = texturePath;
      this.spriteCount = 4;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.texturePath);
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
      int spriteWidth = this.texture.getWidth() / this.spriteCount;
      int drawX = camera.getTileDrawX(tileX) - this.statueXOffset;
      int drawY = camera.getTileDrawY(tileY);
      int rotation = level.getObjectRotation(tileX, tileY) % (this.texture.getWidth() / spriteWidth);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(rotation, 0, spriteWidth, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() + 32);
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
      int spriteWidth = this.texture.getWidth() / this.spriteCount;
      int drawX = camera.getTileDrawX(tileX) - this.statueXOffset;
      int drawY = camera.getTileDrawY(tileY);
      int var11 = (byte)(rotation % (this.texture.getWidth() / spriteWidth));
      this.texture.initDraw().sprite(var11, 0, spriteWidth, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public List<ObjectHoverHitbox> getHoverHitboxes(Level level, int layerID, int tileX, int tileY) {
      List<ObjectHoverHitbox> list = super.getHoverHitboxes(level, layerID, tileX, tileY);
      list.add(new ObjectHoverHitbox(layerID, tileX, tileY, 0, -16, 32, 32));
      return list;
   }
}
