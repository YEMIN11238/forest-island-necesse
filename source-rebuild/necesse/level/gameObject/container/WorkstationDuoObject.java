package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.SideMultiTile;

public class WorkstationDuoObject extends CraftingStationObject {
   public GameTexture texture;
   protected int counterID;

   public static int[] registerWorkstation() {
      WorkstationDuoObject o1 = new WorkstationDuoObject();
      WorkstationDuoObject.WorkstationDuo2Object o2 = new WorkstationDuoObject.WorkstationDuo2Object();
      int i1 = ObjectRegistry.registerObject("workstationduo", o1, 10.0F, true);
      int i2 = ObjectRegistry.registerObject("workstationduo2", o2, 0.0F, false);
      o1.counterID = i2;
      o2.counterID = i1;
      return new int[]{i1, i2};
   }

   protected WorkstationDuoObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(132, 91, 25);
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", "workstation");
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/workstationduo");
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
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("demonicworkstationduo"), new Ingredient("demonicbar", 5));
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
      final DrawOptionsList options = new DrawOptionsList();
      if (rotation == 0) {
         options.add(this.texture.initDraw().sprite(0, 2, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else if (rotation == 1) {
         options.add(this.texture.initDraw().sprite(0, 5, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(0, 6, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else if (rotation == 2) {
         options.add(this.texture.initDraw().sprite(1, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(1, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
      } else {
         options.add(this.texture.initDraw().sprite(1, 3, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
         options.add(this.texture.initDraw().sprite(1, 4, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
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
      return new Tech[]{RecipeTechRegistry.WORKSTATION};
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.workstationOpen;
   }

   public static class WorkstationDuo2Object extends CraftingStationObject {
      public GameTexture texture;
      protected int counterID;

      protected WorkstationDuo2Object() {
         super(new Rectangle(32, 32));
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
         this.texture = GameTexture.fromFile("objects/workstationduo");
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
         final DrawOptionsList options = new DrawOptionsList();
         if (rotation == 0) {
            options.add(this.texture.initDraw().sprite(0, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
            options.add(this.texture.initDraw().sprite(0, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
         } else if (rotation == 1) {
            options.add(this.texture.initDraw().sprite(1, 5, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
            options.add(this.texture.initDraw().sprite(1, 6, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
            int flameSprite = (int)(level.getWorldEntity().getWorldTime() % 1200L / 300L);
            options.add(
               this.texture
                  .initDraw()
                  .sprite(flameSprite % 2, 7 + flameSprite / 2, 32)
                  .addObjectDamageOverlay(this, level, tileX, tileY)
                  .light(light)
                  .pos(drawX, drawY)
            );
         } else if (rotation == 2) {
            options.add(this.texture.initDraw().sprite(1, 2, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
         } else {
            options.add(this.texture.initDraw().sprite(0, 3, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY - 32));
            options.add(this.texture.initDraw().sprite(0, 4, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY));
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
      protected SoundSettings getInteractSoundOpen() {
         return SoundSettingsRegistry.workstationOpen;
      }
   }
}
