package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.GameClock;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.container.TeleportToTeamContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.TeleportationStoneObjectItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TeleportationStoneObject extends GameObject {
   public GameTexture texture;

   public TeleportationStoneObject() {
      super(new Rectangle(0, 4, 32, 22));
      this.mapColor = new Color(124, 137, 154);
      this.displayMapTooltip = true;
      this.objectHealth = 100;
      this.toolType = ToolType.ALL;
      this.stackSize = 1;
      this.lightLevel = 0;
      this.lightHue = 50.0F;
      this.lightSat = 0.2F;
      this.rarity = Item.Rarity.EPIC;
      this.hoverHitbox = new Rectangle(0, -44, 32, 76);
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/teleportationstone");
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String error = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (error != null) {
         return error;
      } else {
         return !level.getIdentifier().equals(LevelIdentifier.SURFACE_IDENTIFIER) ? "notsurface" : null;
      }
   }

   @Override
   public Item generateNewObjectItem() {
      return new TeleportationStoneObjectItem(this);
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
      int drawX = camera.getTileDrawX(tileX) - 16;
      int drawY = camera.getTileDrawY(tileY) - 96;
      long fadeTime = 1500L;
      float saturation = Math.abs(light.getFloatLevel() - 1.0F) * 0.2F + 0.1F;
      long hash = getTileSeed(tileX, tileY);
      final SharedTextureDrawOptions draws = new SharedTextureDrawOptions(this.texture).addObjectDamageOverlay(this, level, tileX, tileY);
      draws.addSprite(0, 0, 64, this.texture.getHeight()).light(light).pos(drawX, drawY);
      draws.addSprite(1, 0, 64, this.texture.getHeight())
         .startGlowOptions(level, hash)
         .fadeTime(fadeTime)
         .saturation(saturation)
         .minLight(50)
         .apply(true)
         .pos(drawX, drawY);
      draws.addSprite(2, 0, 64, this.texture.getHeight())
         .startGlowOptions(GameClock.offsetClock(level, fadeTime / 3L), hash)
         .fadeTime(fadeTime)
         .saturation(saturation)
         .minLight(50)
         .apply(true)
         .pos(drawX, drawY);
      draws.addSprite(3, 0, 64, this.texture.getHeight())
         .startGlowOptions(GameClock.offsetClock(level, fadeTime / 3L * 2L), hash)
         .fadeTime(fadeTime)
         .saturation(saturation)
         .minLight(50)
         .apply(true)
         .pos(drawX, drawY);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            draws.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX) - 16;
      int drawY = camera.getTileDrawY(tileY) - 96;
      long fadeTime = 1500L;
      float saturation = 0.1F;
      long hash = getTileSeed(tileX, tileY);
      SharedTextureDrawOptions draws = new SharedTextureDrawOptions(this.texture);
      draws.addSprite(0, 0, 64, this.texture.getHeight()).alpha(alpha).pos(drawX, drawY);
      draws.addSprite(1, 0, 64, this.texture.getHeight())
         .startGlowOptions(level, hash)
         .fadeTime(fadeTime)
         .saturation(saturation)
         .minLight(50)
         .apply(true)
         .alpha(alpha)
         .pos(drawX, drawY);
      draws.addSprite(2, 0, 64, this.texture.getHeight())
         .startGlowOptions(GameClock.offsetClock(level, fadeTime / 3L), hash)
         .fadeTime(fadeTime)
         .saturation(saturation)
         .minLight(50)
         .apply(true)
         .alpha(alpha)
         .pos(drawX, drawY);
      draws.addSprite(3, 0, 64, this.texture.getHeight())
         .startGlowOptions(GameClock.offsetClock(level, fadeTime / 3L * 2L), hash)
         .fadeTime(fadeTime)
         .saturation(saturation)
         .minLight(50)
         .apply(true)
         .alpha(alpha)
         .pos(drawX, drawY);
      draws.draw();
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
      if (level.isServer()) {
         ServerClient client = player.getServerClient();
         PacketOpenContainer p = PacketOpenContainer.LevelObject(
            ContainerRegistry.TELEPORTATION_STONE_CONTAINER, x, y, TeleportToTeamContainer.getContainerContentPacket(client, 1000)
         );
         ContainerRegistry.openAndSendContainer(player.getServerClient(), p);
      }
   }
}
