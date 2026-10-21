package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.FeedingTroughObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;

public class FeedingTroughObject extends GameObject {
   public GameTexture texture;
   protected int counterID;

   public static int[] registerFeedingTrough() {
      FeedingTroughObject o1;
      int o1ID = ObjectRegistry.registerObject("feedingtrough", o1 = new FeedingTroughObject(), 20.0F, true);
      FeedingTroughObject.FeedingTrough2Object o2;
      int o2ID = ObjectRegistry.registerObject("feedingtrough2", o2 = new FeedingTroughObject.FeedingTrough2Object(), 0.0F, false);
      o1.counterID = o2ID;
      o2.counterID = o1ID;
      return new int[]{o1ID, o2ID};
   }

   protected FeedingTroughObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(150, 119, 70);
      this.displayMapTooltip = true;
      this.toolType = ToolType.ALL;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new MultiTile(0, 1, 1, 2, rotation, true, this.counterID, this.getID());
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/feedingtrough");
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      if (rotation == 0) {
         return new Rectangle(x * 32 + 5, y * 32, 22, 26);
      } else if (rotation == 1) {
         return new Rectangle(x * 32 + 6, y * 32 + 6, 26, 22);
      } else {
         return rotation == 2 ? new Rectangle(x * 32 + 5, y * 32 + 6, 22, 26) : new Rectangle(x * 32, y * 32 + 6, 26, 22);
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
      boolean hasFeed = false;
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof FeedingTroughObjectEntity) {
         hasFeed = ((FeedingTroughObjectEntity)objectEntity).hasFeed();
      }

      final SharedTextureDrawOptions options = new SharedTextureDrawOptions(this.texture).addObjectDamageOverlay(this, level, tileX, tileY);
      if (rotation == 0) {
         options.addSprite(2, 2, 32).light(light).pos(drawX, drawY);
         if (hasFeed) {
            options.addSprite(3, 2, 32).light(light).pos(drawX, drawY);
         }
      } else if (rotation == 1) {
         options.addSprite(0, 2, 32).light(light).pos(drawX, drawY);
         if (hasFeed) {
            options.addSprite(0, 0, 32).light(light).pos(drawX, drawY - 32);
            options.addSprite(0, 1, 32).light(light).pos(drawX, drawY);
         }
      } else if (rotation == 2) {
         options.addSprite(2, 1, 32).light(light).pos(drawX, drawY);
         if (hasFeed) {
            options.addSprite(3, 0, 32).light(light).pos(drawX, drawY - 32);
            options.addSprite(3, 1, 32).light(light).pos(drawX, drawY);
         }
      } else {
         options.addSprite(1, 2, 32).light(light).pos(drawX, drawY);
         if (hasFeed) {
            options.addSprite(1, 0, 32).light(light).pos(drawX, drawY - 32);
            options.addSprite(1, 1, 32).light(light).pos(drawX, drawY);
         }
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 20;
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
      if (rotation == 0) {
         this.texture.initDraw().sprite(2, 2, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(2, 1, 32).alpha(alpha).draw(drawX, drawY - 32);
      } else if (rotation == 1) {
         this.texture.initDraw().sprite(0, 2, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(1, 2, 32).alpha(alpha).draw(drawX + 32, drawY);
      } else if (rotation == 2) {
         this.texture.initDraw().sprite(2, 1, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(2, 2, 32).alpha(alpha).draw(drawX, drawY + 32);
      } else {
         this.texture.initDraw().sprite(1, 2, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(0, 2, 32).alpha(alpha).draw(drawX - 32, drawY);
      }
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
      return new FeedingTroughObjectEntity(level, x, y);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "feedingtroughtip"));
      return tooltips;
   }

   public static class FeedingTrough2Object extends GameObject {
      public GameTexture texture;
      protected int counterID;

      protected FeedingTrough2Object() {
         super(new Rectangle(32, 32));
         this.mapColor = new Color(109, 68, 29);
         this.toolType = ToolType.ALL;
         this.objectHealth = 50;
         this.isLightTransparent = true;
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new MultiTile(0, 0, 1, 2, rotation, false, this.getID(), this.counterID);
      }

      @Override
      public void loadTextures() {
         super.loadTextures();
         this.texture = GameTexture.fromFile("objects/feedingtrough");
      }

      @Override
      public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
         return new LootTable();
      }

      @Override
      public Rectangle getCollision(Level level, int x, int y, int rotation) {
         if (rotation == 0) {
            return new Rectangle(x * 32 + 5, y * 32 + 6, 22, 26);
         } else if (rotation == 1) {
            return new Rectangle(x * 32, y * 32 + 6, 26, 22);
         } else {
            return rotation == 2 ? new Rectangle(x * 32 + 5, y * 32, 22, 26) : new Rectangle(x * 32 + 6, y * 32 + 6, 26, 22);
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
         boolean hasFeed = false;
         LevelObject master = this.getMultiTile(rotation).getMasterLevelObject(level, 0, tileX, tileY).orElse(null);
         if (master != null) {
            ObjectEntity objectEntity = level.entityManager.getObjectEntity(master.tileX, master.tileY);
            if (objectEntity instanceof FeedingTroughObjectEntity) {
               hasFeed = ((FeedingTroughObjectEntity)objectEntity).hasFeed();
            }
         }

         final SharedTextureDrawOptions options = new SharedTextureDrawOptions(this.texture).addObjectDamageOverlay(this, level, tileX, tileY);
         if (rotation == 0) {
            options.addSprite(2, 1, 32).light(light).pos(drawX, drawY);
            if (hasFeed) {
               options.addSprite(3, 1, 32).light(light).pos(drawX, drawY);
               options.addSprite(3, 0, 32).light(light).pos(drawX, drawY - 32);
            }
         } else if (rotation == 1) {
            options.addSprite(1, 2, 32).light(light).pos(drawX, drawY);
            if (hasFeed) {
               options.addSprite(1, 0, 32).light(light).pos(drawX, drawY - 32);
               options.addSprite(1, 1, 32).light(light).pos(drawX, drawY);
            }
         } else if (rotation == 2) {
            options.addSprite(2, 2, 32).light(light).pos(drawX, drawY);
            if (hasFeed) {
               options.addSprite(3, 2, 32).light(light).pos(drawX, drawY);
            }
         } else {
            options.addSprite(0, 2, 32).light(light).pos(drawX, drawY);
            if (hasFeed) {
               options.addSprite(0, 0, 32).light(light).pos(drawX, drawY - 32);
               options.addSprite(0, 1, 32).light(light).pos(drawX, drawY);
            }
         }

         list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
               return 20;
            }

            @Override
            public void draw(TickManager tickManager) {
               options.draw();
            }
         });
      }

      @Override
      public boolean canInteract(Level level, int x, int y, PlayerMob player) {
         return true;
      }

      @Override
      public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
         return Localization.translate("controls", "opentip");
      }

      @Override
      public void interact(Level level, int x, int y, PlayerMob player) {
         this.getMultiTile(level.getObjectRotation(x, y)).getMasterLevelObject(level, 0, x, y).ifPresent(e -> e.interact(player));
      }
   }
}
