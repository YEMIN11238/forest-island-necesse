package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

class FireChaliceObject3 extends FireChaliceAbstractObject {
   protected int counterIDTopLeft;
   protected int counterIDTopRight;
   protected int counterIDBotRight;

   public FireChaliceObject3(String textureName, Color mapColor) {
      super(textureName, mapColor);
   }

   @Override
   protected void setCounterIDs(int id1, int id2, int id3, int id4) {
      this.counterIDTopLeft = id1;
      this.counterIDTopRight = id2;
      this.counterIDBotRight = id4;
   }

   @Override
   protected Rectangle getCollision(Level level, int x, int y, int rotation) {
      if (rotation == 0) {
         return new Rectangle(x * 32 + 4, y * 32, 28, 24);
      } else if (rotation == 1) {
         return new Rectangle(x * 32 + 4, y * 32 + 5, 28, 27);
      } else {
         return rotation == 2 ? new Rectangle(x * 32, y * 32 + 5, 28, 27) : new Rectangle(x * 32, y * 32, 28, 24);
      }
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new StaticMultiTile(0, 1, 2, 2, rotation, false, this.counterIDTopLeft, this.counterIDTopRight, this.getID(), this.counterIDBotRight);
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
      byte rotation = level.getObjectRotation(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      boolean active = this.isActive(level, tileX, tileY);
      int textureOffset = active ? 64 : 0;
      int flameSpriteOffset;
      synchronized (this.drawRandom) {
         long tileSeed = this.getMultiTile(rotation)
            .getMasterLevelObject(level, 0, tileX, tileY)
            .map(lo -> getTileSeed(lo.tileX, lo.tileY))
            .orElseGet(() -> getTileSeed(tileX, tileY));
         int frame = GameUtils.getAnim(this.drawRandom.seeded(tileSeed).nextInt(800) + level.getWorldEntity().getWorldTime(), 4, 800);
         flameSpriteOffset = 128 + frame * 64;
      }

      final DrawOptions base;
      final DrawOptions flame;
      if (rotation == 1) {
         base = this.texture
            .initDraw()
            .section(textureOffset, 32 + textureOffset, 0, this.texture.getHeight() - 32)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY - (this.texture.getHeight() - 64));
         if (active) {
            flame = this.texture
               .initDraw()
               .section(flameSpriteOffset, flameSpriteOffset + 32, 0, this.texture.getHeight() - 32)
               .light(light)
               .pos(drawX, drawY - (this.texture.getHeight() - 64));
         } else {
            flame = () -> {};
         }
      } else if (rotation == 2) {
         base = this.texture
            .initDraw()
            .section(32 + textureOffset, 64 + textureOffset, 0, this.texture.getHeight() - 32)
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY - (this.texture.getHeight() - 64));
         if (active) {
            flame = this.texture
               .initDraw()
               .section(flameSpriteOffset + 32, flameSpriteOffset + 64, 0, this.texture.getHeight() - 32)
               .light(light)
               .pos(drawX, drawY - (this.texture.getHeight() - 64));
         } else {
            flame = () -> {};
         }
      } else if (rotation == 3) {
         base = this.texture
            .initDraw()
            .section(32 + textureOffset, 64 + textureOffset, this.texture.getHeight() - 32, this.texture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY);
         if (active) {
            flame = this.texture
               .initDraw()
               .section(flameSpriteOffset + 32, flameSpriteOffset + 64, this.texture.getHeight() - 32, this.texture.getHeight())
               .light(light)
               .pos(drawX, drawY);
         } else {
            flame = () -> {};
         }
      } else {
         base = this.texture
            .initDraw()
            .section(textureOffset, 32 + textureOffset, this.texture.getHeight() - 32, this.texture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX, drawY);
         if (active) {
            flame = this.texture
               .initDraw()
               .section(flameSpriteOffset, flameSpriteOffset + 32, this.texture.getHeight() - 32, this.texture.getHeight())
               .light(light)
               .pos(drawX, drawY);
         } else {
            flame = () -> {};
         }
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            base.draw();
            flame.draw();
         }
      });
   }
}
