package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
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
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.furniture.CandelabraObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PestWardenLampObject extends CandelabraObject implements HappinessObject {
   public PestWardenLampObject(String textureName, ToolType toolType, String... category) {
      super(textureName, toolType, new Color(47, 105, 12), 90.0F, 0.8F, category);
      this.rarity = Item.Rarity.LEGENDARY;
   }

   public PestWardenLampObject(String textureName, String... category) {
      this(textureName, ToolType.ALL, category);
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
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
      GameTexture texture = this.isActive(level, tileX, tileY) ? this.texture : this.texture_off;
      final TextureDrawOptions options = texture.initDraw()
         .sprite(0, 0, 64, texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX - 16, drawY - texture.getHeight() + 32);
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
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 64, this.texture.getHeight()).alpha(alpha).draw(drawX - 16, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
