package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.StaticMultiObject;
import necesse.level.maps.Level;

public class BearRugObject extends StaticMultiObject implements HappinessObject {
   protected BearRugObject(int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, "bearrug");
      this.mapColor = new Color(109, 68, 29);
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.rarity = Item.Rarity.RARE;
      this.setItemCategory("objects", "decorations");
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
      DrawOptions options = this.getMultiTextureDrawOptions(sprite, level, tileX, tileY, camera);
      tileList.add(tm -> options.draw());
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 64);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerBearRug() {
      int[] ids = new int[4];
      Rectangle collision = new Rectangle(0, 0, 0, 0);
      ids[0] = ObjectRegistry.registerObject("bearrug", new BearRugObject(0, 0, 2, 2, ids, collision), 500.0F, true, false);
      ids[1] = ObjectRegistry.registerObject("bearrug2", new BearRugObject(1, 0, 2, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject("bearrug3", new BearRugObject(0, 1, 2, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject("bearrug4", new BearRugObject(1, 1, 2, 2, ids, collision), 0.0F, false);
      return ids;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
