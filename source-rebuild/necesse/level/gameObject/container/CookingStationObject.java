package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.FueledInventoryObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Tech;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.SideMultiTile;

public class CookingStationObject extends FueledCraftingStationObject {
   public GameTexture texture;
   protected int counterID;

   public static int[] registerCookingStation() {
      CookingStationObject o1 = new CookingStationObject();
      CookingStationObject.CookingStation2Object o2 = new CookingStationObject.CookingStation2Object();
      int i1 = ObjectRegistry.registerObject("cookingstation", o1, 140.0F, true);
      int i2 = ObjectRegistry.registerObject("cookingstation2", o2, 0.0F, false);
      o1.counterID = i2;
      o2.counterID = i1;
      return new int[]{i1, i2};
   }

   protected CookingStationObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(51, 53, 56);
      this.rarity = Item.Rarity.UNCOMMON;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.lightHue = 50.0F;
      this.lightSat = 0.2F;
      this.hoverHitbox = new Rectangle(0, -10, 32, 42);
   }

   @Override
   public int getCraftingCategoryDepth() {
      return 2;
   }

   @Override
   public int getCraftingFormWidth() {
      return 418;
   }

   @Override
   public boolean allowHighlightOption() {
      return false;
   }

   @Override
   public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
      FueledInventoryObjectEntity fueledObjectEntity = this.getFueledObjectEntity(level, tileX, tileY);
      return fueledObjectEntity != null && fueledObjectEntity.isFueled() ? 100 : 0;
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
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/cookingstation");
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
      boolean isActive = this.getMultiTile(rotation).getMasterLevelObject(level, 0, tileX, tileY).map(lo -> {
         FueledInventoryObjectEntity fueledObjectEntity = ((FueledCraftingStationObject)lo.object).getFueledObjectEntity(lo.level, lo.tileX, lo.tileY);
         return fueledObjectEntity != null && fueledObjectEntity.isFueled();
      }).orElse(false);
      final DrawOptionsList options = new DrawOptionsList();
      if (rotation == 0) {
         options.add(this.texture.initDraw().sprite(0, 2, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
         if (isActive) {
            options.add(this.texture.initDraw().sprite(0, 8, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32 + 14));
         }
      } else if (rotation == 1) {
         options.add(this.texture.initDraw().sprite(0, 5, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(0, 6, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
         if (isActive) {
            options.add(this.texture.initDraw().sprite(0, 7, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 14));
         }
      } else if (rotation == 2) {
         options.add(this.texture.initDraw().sprite(1, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(1, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
         if (isActive) {
            options.add(this.texture.initDraw().sprite(1, 8, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 14));
         }
      } else {
         options.add(this.texture.initDraw().sprite(1, 3, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(1, 4, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
         if (isActive) {
            options.add(this.texture.initDraw().sprite(1, 7, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 14));
         }
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
      if (rotation == 0) {
         this.texture.initDraw().sprite(0, 2, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(0, 0, 32).alpha(alpha).draw(drawX, drawY - 64);
         this.texture.initDraw().sprite(0, 1, 32).alpha(alpha).draw(drawX, drawY - 32);
      } else if (rotation == 1) {
         this.texture.initDraw().sprite(0, 5, 32).alpha(alpha).draw(drawX, drawY - 32);
         this.texture.initDraw().sprite(0, 6, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(1, 5, 32).alpha(alpha).draw(drawX + 32, drawY - 32);
         this.texture.initDraw().sprite(1, 6, 32).alpha(alpha).draw(drawX + 32, drawY);
      } else if (rotation == 2) {
         this.texture.initDraw().sprite(1, 0, 32).alpha(alpha).draw(drawX, drawY - 32);
         this.texture.initDraw().sprite(1, 1, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(1, 2, 32).alpha(alpha).draw(drawX, drawY + 32);
      } else {
         this.texture.initDraw().sprite(1, 3, 32).alpha(alpha).draw(drawX, drawY - 32);
         this.texture.initDraw().sprite(1, 4, 32).alpha(alpha).draw(drawX, drawY);
         this.texture.initDraw().sprite(0, 3, 32).alpha(alpha).draw(drawX - 32, drawY - 32);
         this.texture.initDraw().sprite(0, 4, 32).alpha(alpha).draw(drawX - 32, drawY);
      }
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.COOKING_STATION, RecipeTechRegistry.COOKING_POT, RecipeTechRegistry.ROASTING_STATION};
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.defaultOpen;
   }

   public static class CookingStation2Object extends GameObject {
      public GameTexture texture;
      protected int counterID;

      protected CookingStation2Object() {
         super(new Rectangle(32, 32));
         this.mapColor = new Color(51, 53, 56);
         this.toolType = ToolType.ALL;
         this.isLightTransparent = true;
         this.lightLevel = 100;
         this.lightHue = 50.0F;
         this.lightSat = 0.2F;
         this.hoverHitbox = new Rectangle(0, -10, 32, 42);
      }

      @Override
      public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
         return this.getMultiTile(level.getObjectRotation(tileX, tileY)).getMasterLevelObject(level, layerID, tileX, tileY).map(lo -> {
            FueledInventoryObjectEntity fueledObjectEntity = ((FueledCraftingStationObject)lo.object).getFueledObjectEntity(lo.level, lo.tileX, lo.tileY);
            return fueledObjectEntity != null && fueledObjectEntity.isFueled() ? 100 : 0;
         }).orElseGet(() -> super.getLightLevel(level, layerID, tileX, tileY));
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new SideMultiTile(0, 0, 1, 2, rotation, false, this.getID(), this.counterID);
      }

      @Override
      public void loadTextures() {
         super.loadTextures();
         this.texture = GameTexture.fromFile("objects/cookingstation");
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
         boolean isActive = this.getMultiTile(rotation).getMasterLevelObject(level, 0, tileX, tileY).map(lo -> {
            FueledInventoryObjectEntity fueledObjectEntity = ((FueledCraftingStationObject)lo.object).getFueledObjectEntity(lo.level, lo.tileX, lo.tileY);
            return fueledObjectEntity != null && fueledObjectEntity.isFueled();
         }).orElse(false);
         final DrawOptionsList options = new DrawOptionsList();
         if (rotation == 0) {
            options.add(this.texture.initDraw().sprite(0, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
            options.add(this.texture.initDraw().sprite(0, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
            if (isActive) {
               options.add(this.texture.initDraw().sprite(0, 8, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 14));
            }
         } else if (rotation == 1) {
            options.add(this.texture.initDraw().sprite(1, 5, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
            options.add(this.texture.initDraw().sprite(1, 6, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
            if (isActive) {
               options.add(this.texture.initDraw().sprite(1, 7, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 14));
            }
         } else if (rotation == 2) {
            options.add(this.texture.initDraw().sprite(1, 2, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
            if (isActive) {
               options.add(this.texture.initDraw().sprite(1, 8, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32 + 14));
            }
         } else {
            options.add(this.texture.initDraw().sprite(0, 3, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
            options.add(this.texture.initDraw().sprite(0, 4, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
            if (isActive) {
               options.add(this.texture.initDraw().sprite(0, 7, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 14));
            }
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
      public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
         return this.getMultiTile(level.getObjectRotation(x, y))
            .getMasterLevelObject(level, 0, x, y)
            .map(e -> e.getInteractTip(perspective, debug))
            .orElseGet(() -> super.getInteractTip(level, x, y, perspective, debug));
      }

      @Override
      public boolean canInteract(Level level, int x, int y, PlayerMob player) {
         return this.getMultiTile(level.getObjectRotation(x, y))
            .getMasterLevelObject(level, 0, x, y)
            .map(e -> e.canInteract(player))
            .orElseGet(() -> super.canInteract(level, x, y, player));
      }

      @Override
      public void interact(Level level, int x, int y, PlayerMob player) {
         super.interact(level, x, y, player);
         this.getMultiTile(level.getObjectRotation(x, y)).getMasterLevelObject(level, 0, x, y).ifPresent(e -> e.interact(player));
      }

      @Override
      protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
         return true;
      }

      @Override
      protected SoundSettings getInteractSoundOpen() {
         return SoundSettingsRegistry.defaultOpen;
      }
   }
}
