package necesse.level.gameObject.container;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.BannerStandObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.miscItem.BannerItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BannerStandObject extends GameObject {
   public GameTexture texture;

   public BannerStandObject() {
      super(new Rectangle());
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.rarity = Item.Rarity.RARE;
      this.canPlaceOnProtectedLevels = true;
      this.shouldReturnOnDeletedLevels = true;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/bannerstand");
   }

   protected BannerItem getBannerInInventory(Level level, int tileX, int tileY) {
      ObjectEntity ent = level.entityManager.getObjectEntity(tileX, tileY);
      return ent != null && ent.implementsOEInventory() && ent instanceof BannerStandObjectEntity
         ? ((BannerStandObjectEntity)ent).getBannerInInventory()
         : null;
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
      TextureDrawOptions base = this.texture
         .initDraw()
         .sprite(0, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      tileList.add(tm -> base.draw());
      BannerItem bannerItem = this.getBannerInInventory(level, tileX, tileY);
      final DrawOptions item;
      if (bannerItem != null) {
         item = bannerItem.getStandDrawOptions(level, tileX, tileY, drawX, drawY, light);
      } else {
         item = () -> {};
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 12;
         }

         @Override
         public void draw(TickManager tickManager) {
            item.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "opentip");
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (level.isServer()) {
         OEInventoryContainer.openAndSendContainer(ContainerRegistry.OE_INVENTORY_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new BannerStandObjectEntity(level, x, y);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "bannerstandtip"));
      return tooltips;
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }
}
