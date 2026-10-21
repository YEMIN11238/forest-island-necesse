package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.FireworkDispenserObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FireworkDispenserObject extends GameObject {
   public GameTexture texture;

   public FireworkDispenserObject() {
      super(new Rectangle(5, 5, 20, 20));
      this.toolType = ToolType.ALL;
      this.mapColor = new Color(88, 113, 44);
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.showsWire = true;
      this.setItemCategory("wiring");
      this.setCraftingCategory("wiring");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/fireworkdispenser");
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
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(0, 0, 32, this.texture.getHeight())
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
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
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
   public void onWireUpdate(Level level, int layerID, int tileX, int tileY, int wireID, boolean active) {
      if (active) {
         ObjectEntity ent = level.entityManager.getObjectEntity(tileX, tileY);
         if (ent != null) {
            ((FireworkDispenserObjectEntity)ent).fire();
         }
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new FireworkDispenserObjectEntity(level, x, y);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "fireworkdispensertip"));
      return tooltips;
   }
}
