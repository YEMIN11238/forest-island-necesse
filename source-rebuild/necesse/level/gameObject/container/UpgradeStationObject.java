package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.UpgradeStationObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.UpgradeStationContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class UpgradeStationObject extends GameObject {
   public GameTexture texture;

   public UpgradeStationObject() {
      super(new Rectangle(32, 32));
      this.stackSize = 1;
      this.rarity = Item.Rarity.RARE;
      this.displayMapTooltip = true;
      this.mapColor = new Color(0, 107, 109);
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -10, 32, 42);
      this.setItemCategory("objects", "craftingstations");
      this.setCraftingCategory("craftingstations");
      this.replaceCategories.add("workstation");
      this.canReplaceCategories.add("workstation");
      this.canReplaceCategories.add("wall");
      this.canReplaceCategories.add("furniture");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/upgradestation");
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
      final TextureDrawOptions drawOptions = this.texture
         .initDraw()
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX + 16 - this.texture.getWidth() / 2, drawY - this.texture.getHeight() + 32 + 4);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().alpha(alpha).draw(drawX + 16 - this.texture.getWidth() / 2, drawY - this.texture.getHeight() + 32 + 4);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "upgradestationtip"), 400);
      return tooltips;
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "usetip");
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (level.isServer()) {
         UpgradeStationContainer.openAndSendContainer(ContainerRegistry.UPGRADE_STATION_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new UpgradeStationObjectEntity(level, x, y);
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.anvilOpen;
   }
}
