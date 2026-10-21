package necesse.level.gameObject.happinessObject;

import java.util.Collections;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectHoverHitbox;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.EnjoyArtTileLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.SidedRotationMultiTile;

class SwampGuardianLargePaintingObject2 extends GameObject {
   public GameTexture texture;
   protected int counterID;
   final String texturePath;

   protected SwampGuardianLargePaintingObject2(String texturePath, Item.Rarity rarity) {
      this.texturePath = texturePath;
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.rarity = rarity;
      this.validObjectLayers.add(ObjectLayerRegistry.WALL_DECOR);
      this.roomProperties.add("painting");
      this.replaceCategories.add("painting");
      this.canReplaceCategories.add("painting");
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/" + this.texturePath);
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new SidedRotationMultiTile(1, 0, 2, 1, rotation, false, this.counterID, this.getID());
   }

   @Override
   public void addLayerDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int rotation = level.getObjectRotation(layerID, tileX, tileY);
      final DrawOptionsList options = new DrawOptionsList();
      final int sortY;
      if (rotation == 0) {
         sortY = 32;
         options.add(
            this.texture.initDraw().sprite(1, 2, 32, 64).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 16)
         );
      } else if (rotation == 1) {
         sortY = 24;
         options.add(
            this.texture.initDraw().sprite(0, 7, 32, 32).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 16)
         );
      } else if (rotation == 2) {
         sortY = 0;
         options.add(
            this.texture.initDraw().sprite(0, 0, 32, 64).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 64)
         );
      } else {
         sortY = 24;
         options.add(
            this.texture.initDraw().sprite(1, 2, 32, 32).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 16)
         );
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return sortY;
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
         this.texture.initDraw().sprite(1, 2, 32, 64).alpha(alpha).draw(drawX, drawY - 16);
      } else if (rotation == 1) {
         this.texture.initDraw().sprite(0, 7, 32, 32).alpha(alpha).draw(drawX, drawY - 16);
      } else if (rotation == 2) {
         this.texture.initDraw().sprite(0, 0, 32, 64).alpha(alpha).draw(drawX, drawY - 64);
      } else {
         this.texture.initDraw().sprite(1, 2, 32, 32).alpha(alpha).draw(drawX, drawY - 16);
      }
   }

   @Override
   protected ObjectHoverHitbox getHoverHitbox(Level level, int layerID, int tileX, int tileY) {
      byte rotation = level.getObjectRotation(layerID, tileX, tileY);
      if (rotation == 0) {
         return new ObjectHoverHitbox(layerID, tileX, tileY, 0, 0, 32, 32, 32);
      } else {
         return rotation != 1 && rotation != 3
            ? new ObjectHoverHitbox(layerID, tileX, tileY, 0, -32, 32, 64, 0)
            : new ObjectHoverHitbox(layerID, tileX, tileY, 0, -16, 32, 48, 32);
      }
   }

   @Override
   public boolean canReplace(Level level, int layerID, int tileX, int tileY, int rotation) {
      return super.canReplace(level, layerID, tileX, tileY, rotation) && this.attachesToObject(level, tileX, tileY, rotation);
   }

   public boolean attachesToObject(Level level, int tileX, int tileY) {
      return level.getObject(tileX, tileY).isWall && !level.getObject(tileX, tileY).isDoor;
   }

   public boolean attachesToObject(Level level, int tileX, int tileY, int rotation) {
      if (rotation == 0 && this.attachesToObject(level, tileX, tileY + 1)) {
         return true;
      } else if (rotation == 1 && this.attachesToObject(level, tileX - 1, tileY)) {
         return true;
      } else {
         return rotation == 2 && this.attachesToObject(level, tileX, tileY - 1) ? true : rotation == 3 && this.attachesToObject(level, tileX + 1, tileY);
      }
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      if (this.isTilePlaceOccupied(level, layerID, x, y, ignoreOtherLayers)) {
         return "occupied";
      }

      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return "tilecovered";
         }
      }

      return this.attachesToObject(level, x, y, rotation) ? null : "nowall";
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      int rotation = level.getObjectRotation(layerID, x, y);
      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return false;
         }
      }

      return !this.attachesToObject(level, x, y, rotation)
         ? false
         : this.getMultiTile(rotation).streamOtherIDs(x, y).allMatch(e -> level.getObjectID(layerID, e.tileX, e.tileY) == e.value);
   }

   @Override
   public List<TileLevelJob> getLevelJobs(Level level, int tileX, int tileY) {
      return Collections.singletonList(new EnjoyArtTileLevelJob(tileX, tileY, this, level.getObjectRotation(tileX, tileY), 2));
   }
}
