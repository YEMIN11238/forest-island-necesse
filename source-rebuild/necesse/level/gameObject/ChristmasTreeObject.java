package necesse.level.gameObject;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameRandom;
import necesse.engine.window.WindowManager;
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
import necesse.level.gameObject.furniture.RoomFurniture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ChristmasTreeObject extends GameObject implements RoomFurniture {
   protected GameTexture texture;
   protected final GameRandom drawRandom;

   public ChristmasTreeObject() {
      super(new Rectangle(32, 32));
      this.hoverHitbox = new Rectangle(-16, -48, 64, 80);
      this.displayMapTooltip = true;
      this.toolType = ToolType.ALL;
      this.drawRandom = new GameRandom();
      this.rarity = Item.Rarity.RARE;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public String getFurnitureType() {
      return "christmastree";
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/christmastree");
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
      GameLight ornamentLights;
      if (level.wireManager.isWireActiveAny(tileX, tileY)) {
         ornamentLights = light;
      } else {
         ornamentLights = light.minLevelCopy(100.0F);
      }

      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      float alpha = 1.0F;
      if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
         Rectangle alphaRec = new Rectangle(tileX * 32 - 40 + 16, tileY * 32 - 80 + 16, 80, 80);
         if (perspective.getCollision().intersects(alphaRec)) {
            alpha = 0.5F;
         } else if (alphaRec.contains(camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY)
            )
          {
            alpha = 0.5F;
         }
      }

      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         mirror = this.drawRandom.nextBoolean();
      }

      final TextureDrawOptions base = this.texture.initDraw().sprite(0, 0, 128).alpha(alpha).light(light).mirror(mirror, false).pos(drawX - 48, drawY - 96);
      final TextureDrawOptions ornaments = this.texture
         .initDraw()
         .sprite(1, 0, 128)
         .alpha(alpha)
         .light(ornamentLights)
         .mirror(mirror, false)
         .pos(drawX - 48, drawY - 96);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            base.draw();
            ornaments.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      GameLight ornamentLights;
      if (level.wireManager.isWireActiveAny(tileX, tileY)) {
         ornamentLights = light;
      } else {
         ornamentLights = light.minLevelCopy(100.0F);
      }

      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         mirror = this.drawRandom.nextBoolean();
      }

      this.texture.initDraw().sprite(0, 0, 128).alpha(alpha).light(light).mirror(mirror, false).draw(drawX - 48, drawY - 96);
      this.texture.initDraw().sprite(1, 0, 128).alpha(alpha).light(ornamentLights).mirror(mirror, false).draw(drawX - 48, drawY - 96);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "christmastreetip"));
      tooltips.add(Localization.translate("itemtooltip", "biggerthanitlookstip"));
      return tooltips;
   }
}
