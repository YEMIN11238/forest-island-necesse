package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.ShippingChestObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.ShippingChestLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.SideMultiTile;

public class ShippingChestObject extends GameObject {
   public GameTexture texture;
   public GameTexture openTexture;
   protected int counterID;

   public static int[] registerShippingChest() {
      ShippingChestObject o1 = new ShippingChestObject();
      ShippingChestObject.ShippingChest2Object o2 = new ShippingChestObject.ShippingChest2Object();
      int i1 = ObjectRegistry.registerObject("shippingchest", o1, 200.0F, true);
      int i2 = ObjectRegistry.registerObject("shippingchest2", o2, 0.0F, false);
      o1.counterID = i2;
      o2.counterID = i1;
      return new int[]{i1, i2};
   }

   protected ShippingChestObject() {
      super(new Rectangle(32, 32));
      this.displayMapTooltip = true;
      this.mapColor = new Color(132, 91, 25);
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.rarity = Item.Rarity.UNCOMMON;
      this.setItemCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/shippingchest");
      this.openTexture = GameTexture.fromFile("objects/shippingchest_open");
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new SideMultiTile(0, 1, 1, 2, rotation, true, this.counterID, this.getID());
   }

   @Override
   public ArrayList<ObjectPlaceOption> getPlaceOptions(Level level, int levelX, int levelY, PlayerMob playerMob, int playerDir, boolean offsetMultiTile) {
      return super.getPlaceOptions(level, levelX, levelY, playerMob, Math.floorMod(playerDir - 1, 4), offsetMultiTile);
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      if (rotation == 0) {
         return new Rectangle(x * 32 + 4, y * 32, 24, 26);
      } else if (rotation == 1) {
         return new Rectangle(x * 32 + 6, y * 32 + 6, 26, 20);
      } else {
         return rotation == 2 ? new Rectangle(x * 32 + 4, y * 32 + 4, 24, 28) : new Rectangle(x * 32, y * 32 + 6, 26, 20);
      }
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
      GameTexture texture = this.texture;
      if (this.openTexture != null) {
         ShippingChestObjectEntity objectEntity = this.getCurrentObjectEntity(level, tileX, tileY, ShippingChestObjectEntity.class);
         if (objectEntity != null && objectEntity.isInUse()) {
            texture = this.openTexture;
         }
      }

      final DrawOptions options;
      if (rotation == 0) {
         options = texture.initDraw()
            .sprite(2, texture.getHeight() / 32 - 1, 32)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY);
      } else if (rotation == 1) {
         options = texture.initDraw()
            .sprite(3, 0, 32, texture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY - texture.getHeight() + 32);
      } else if (rotation == 2) {
         options = texture.initDraw()
            .sprite(5, 0, 32, texture.getHeight() - 32)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY - 32);
      } else {
         options = texture.initDraw()
            .sprite(1, 0, 32, texture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY - texture.getHeight() + 32);
      }

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
      DrawOptions options;
      if (rotation == 0) {
         options = this.texture.initDraw().sprite(2, this.texture.getHeight() / 32 - 1, 32).alpha(alpha).pos(drawX, drawY);
      } else if (rotation == 1) {
         options = this.texture.initDraw().sprite(3, 0, 32, this.texture.getHeight()).alpha(alpha).pos(drawX, drawY - this.texture.getHeight() + 32);
      } else if (rotation == 2) {
         options = this.texture.initDraw().sprite(5, 0, 32, this.texture.getHeight() - 32).alpha(alpha).pos(drawX, drawY - 32);
      } else {
         options = this.texture.initDraw().sprite(1, 0, 32, this.texture.getHeight()).alpha(alpha).pos(drawX, drawY - this.texture.getHeight() + 32);
      }

      options.draw();
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(new LocalMessage("ui", "shippingchesttip"), 400);
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
      if (level.isServer()) {
         OEInventoryContainer.openAndSendContainer(ContainerRegistry.SHIPPING_CHEST_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new ShippingChestObjectEntity(level, x, y);
   }

   @Override
   public List<TileLevelJob> getLevelJobs(Level level, int tileX, int tileY) {
      return Collections.singletonList(new ShippingChestLevelJob(tileX, tileY));
   }

   public static class ShippingChest2Object extends GameObject {
      public GameTexture texture;
      public GameTexture openTexture;
      protected int counterID;

      protected ShippingChest2Object() {
         super(new Rectangle(32, 32));
         this.displayMapTooltip = true;
         this.mapColor = new Color(132, 91, 25);
         this.toolType = ToolType.ALL;
         this.isLightTransparent = true;
         this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new SideMultiTile(0, 0, 1, 2, rotation, false, this.getID(), this.counterID);
      }

      @Override
      public void loadTextures() {
         super.loadTextures();
         this.texture = GameTexture.fromFile("objects/shippingchest");
         this.openTexture = GameTexture.fromFile("objects/shippingchest_open");
      }

      @Override
      public Rectangle getCollision(Level level, int x, int y, int rotation) {
         if (rotation == 0) {
            return new Rectangle(x * 32 + 4, y * 32 + 4, 24, 28);
         } else if (rotation == 1) {
            return new Rectangle(x * 32, y * 32 + 6, 26, 20);
         } else {
            return rotation == 2 ? new Rectangle(x * 32 + 4, y * 32, 24, 26) : new Rectangle(x * 32 + 6, y * 32 + 6, 26, 20);
         }
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
         GameTexture texture = this.texture;
         if (this.openTexture != null) {
            ShippingChestObjectEntity objectEntity = this.getMultiTile(level.getObjectRotation(tileX, tileY))
               .getMasterLevelObject(level, 0, tileX, tileY)
               .map(e -> this.getCurrentObjectEntity(level, e.tileX, e.tileY, ShippingChestObjectEntity.class))
               .orElse(null);
            if (objectEntity != null && objectEntity.isInUse()) {
               texture = this.openTexture;
            }
         }

         final DrawOptions options;
         if (rotation == 0) {
            options = texture.initDraw()
               .sprite(2, 0, 32, texture.getHeight() - 32)
               .addObjectDamageOverlay(this, level, tileX, tileY)
               .light(light)
               .pos(drawX, drawY - 32);
         } else if (rotation == 1) {
            options = texture.initDraw()
               .sprite(4, 0, 32, texture.getHeight())
               .addObjectDamageOverlay(this, level, tileX, tileY)
               .light(light)
               .pos(drawX, drawY - texture.getHeight() + 32);
         } else if (rotation == 2) {
            options = texture.initDraw()
               .sprite(5, texture.getHeight() / 32 - 1, 32)
               .addObjectDamageOverlay(this, level, tileX, tileY)
               .light(light)
               .pos(drawX, drawY);
         } else {
            options = texture.initDraw()
               .sprite(0, 0, 32, texture.getHeight())
               .addObjectDamageOverlay(this, level, tileX, tileY)
               .light(light)
               .pos(drawX, drawY - texture.getHeight() + 32);
         }

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
         DrawOptions options;
         if (rotation == 0) {
            options = this.texture.initDraw().sprite(2, 0, 32, this.texture.getHeight() - 32).alpha(alpha).pos(drawX, drawY - 32);
         } else if (rotation == 1) {
            options = this.texture.initDraw().sprite(4, 0, 32, this.texture.getHeight()).alpha(alpha).pos(drawX, drawY - this.texture.getHeight() + 32);
         } else if (rotation == 2) {
            options = this.texture.initDraw().sprite(5, this.texture.getHeight() / 32 - 1, 32).alpha(alpha).pos(drawX, drawY);
         } else {
            options = this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).pos(drawX, drawY - this.texture.getHeight() + 32);
         }

         options.draw();
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
         this.getMultiTile(level.getObjectRotation(x, y)).getMasterLevelObject(level, 0, x, y).ifPresent(e -> e.interact(player));
      }
   }
}
