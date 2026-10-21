package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.objectEntity.FueledRefrigeratorObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameObject.container.CoolingBoxInventoryObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class VendingMachineObject extends CoolingBoxInventoryObject {
   public VendingMachineObject(String textureName, int slots, Color mapColor) {
      super(textureName, slots, mapColor);
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      return new Rectangle(x * 32 + 2, y * 32 + 2, 28, 28);
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
      FueledRefrigeratorObjectEntity coolingBoxObjectEntity = this.getCoolingBoxObjectEntity(level, tileX, tileY);
      int spriteY = coolingBoxObjectEntity != null && coolingBoxObjectEntity.hasFuel() ? 1 : 0;
      GameTexture texture = this.texture;
      if (this.openTexture != null) {
         ObjectEntity ent = level.entityManager.getObjectEntity(tileX, tileY);
         if (ent != null && ent.implementsOEUsers() && ((OEUsers)ent).isInUse()) {
            texture = this.openTexture;
         }
      }

      final SharedTextureDrawOptions draws = new SharedTextureDrawOptions(texture).addObjectDamageOverlay(this, level, tileX, tileY);
      int rotation = level.getObjectRotation(tileX, tileY) % (texture.getWidth() / 32);
      boolean treasureHunter = perspective != null && perspective.buffManager.getModifier(BuffModifiers.TREASURE_HUNTER);
      draws.addSprite(rotation, spriteY, 32, texture.getHeight() / 3).spelunkerLight(light, treasureHunter, this.getID(), level).pos(drawX, drawY - 32);
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
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      rotation %= this.texture.getWidth() / 32;
      this.texture
         .initDraw()
         .sprite(rotation, 1, 32, this.texture.getHeight() / 3)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .alpha(alpha)
         .draw(drawX, drawY - 32);
   }
}
