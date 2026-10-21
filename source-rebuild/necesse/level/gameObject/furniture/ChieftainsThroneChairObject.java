package necesse.level.gameObject.furniture;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.ObjectUserMob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ChieftainsThroneChairObject extends ChairObject {
   public ChieftainsThroneChairObject(String textureName, Color mapColor) {
      super(textureName, mapColor);
      this.hoverHitbox = new Rectangle(0, 0, 32, 32);
      this.collision = new Rectangle(0, 0, 32, 32);
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
      List<ObjectUserMob> users = this.getObjectUsers(level, tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      GameLight light = level.getLightLevel(tileX, tileY);
      TextureDrawOptions drawOptions = this.texture
         .initDraw()
         .sprite(0, 0, this.texture.getWidth(), this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX - this.texture.getWidth() / 3, drawY - this.texture.getHeight() + 32);
      final DrawOptionsList options = new DrawOptionsList();
      options.add(drawOptions);

      for (ObjectUserMob user : users) {
         Point offset = this.getMobPosSitOffset(level, tileX, tileY);
         options.add(user.getUserDrawOptions(level, tileX * 32 + offset.x, tileY * 32 + offset.y, tickManager, camera, perspective, humanOptions -> {
            if (humanOptions != null) {
               this.modifyHumanDrawOptions(level, tileX, tileY, humanOptions);
            }
         }));
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
      this.texture
         .initDraw()
         .sprite(0, 0, this.texture.getWidth(), this.texture.getHeight())
         .alpha(alpha)
         .draw(drawX - this.texture.getWidth() / 3, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      return new Rectangle(x * 32 + this.collision.x, y * 32 + this.collision.x, this.collision.width, this.collision.height);
   }

   @Override
   public void modifyHumanDrawOptions(Level level, int tileX, int tileY, HumanDrawOptions options) {
      options.dir(2).sprite(6, 2);
   }

   @Override
   public Point getMobPosSitOffset(Level level, int tileX, int tileY) {
      return new Point(16, 12);
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return false;
   }

   @Override
   public GameMessage preventsLadderPlacement(Level level, int tileX, int tileY) {
      return new LocalMessage("misc", "blockingexit");
   }
}
