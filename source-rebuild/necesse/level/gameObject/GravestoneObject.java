package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class GravestoneObject extends GameObject {
   protected String textureName;
   protected GameTexture texture;
   protected final GameRandom drawRandom;

   public GravestoneObject(String textureName, ToolType toolType, Color mapColor) {
      super(new Rectangle());
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.toolType = toolType;
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.setItemCategory("objects", "landscaping", "masonry");
      this.setCraftingCategory("objects", "landscaping", "masonry");
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY) ? super.getLootTable(level, layerID, tileX, tileY) : level.getCrateLootTable(tileX, tileY);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", "gravestone");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      return super.getCollision(level, x, y, rotation);
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
      int rotation = level.getObjectRotation(tileX, tileY);
      final int sortY;
      if (rotation == 0) {
         sortY = 22;
      } else if (rotation == 2) {
         sortY = 4;
      } else {
         sortY = 16;
      }

      final SharedTextureDrawOptions draws = new SharedTextureDrawOptions(this.texture);
      draws.addSprite(rotation, 0, 32, this.texture.getHeight() - 32)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyTreasure(perspective)
         .pos(drawX, drawY - this.texture.getHeight() + 64);
      TextureDrawOptions tileOption = this.texture.initDraw().sprite(rotation, this.texture.getHeight() / 32 - 1, 32).light(light).pos(drawX, drawY);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return sortY;
         }

         @Override
         public void draw(TickManager tickManager) {
            draws.draw();
         }
      });
      tileList.add(tm -> tileOption.draw());
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(rotation, this.texture.getHeight() / 32 - 1, 32).alpha(alpha).draw(drawX, drawY);
      this.texture.initDraw().sprite(rotation, 0, 32, this.texture.getHeight() - 32).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 64);
   }

   @Override
   public void onDestroyed(Level level, int layerID, int x, int y, Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped) {
      super.onDestroyed(level, layerID, x, y, attacker, client, itemsDropped);
      if (client != null && client.achievementsLoaded()) {
         client.achievements().GRAVE_DIGGER.markCompleted(client);
      }
   }

   @Override
   protected ObjectHoverHitbox getHoverHitbox(Level level, int layerID, int tileX, int tileY) {
      byte rotation = level.getObjectRotation(layerID, tileX, tileY);
      if (rotation == 2) {
         return new ObjectHoverHitbox(layerID, tileX, tileY, 0, -16, 32, 48, 4);
      } else {
         return rotation != 1 && rotation != 3
            ? new ObjectHoverHitbox(layerID, tileX, tileY, 0, -10, 32, 42, 28)
            : new ObjectHoverHitbox(layerID, tileX, tileY, 0, -10, 32, 42, 16);
      }
   }

   @Override
   public boolean shouldSnapSmartMining(Level level, int x, int y) {
      return true;
   }
}
