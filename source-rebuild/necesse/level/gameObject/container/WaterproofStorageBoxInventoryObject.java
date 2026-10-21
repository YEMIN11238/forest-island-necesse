package necesse.level.gameObject.container;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.interfaces.OEUsers;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.light.GameLight;

public class WaterproofStorageBoxInventoryObject extends StorageBoxInventoryObject {
   public WaterproofStorageBoxInventoryObject(String textureName, int slots, ToolType toolType, Color mapColor) {
      super(textureName, slots, toolType, mapColor);
      this.canPlaceOnLiquid = true;
   }

   public WaterproofStorageBoxInventoryObject(String textureName, int slots, Color mapColor, String... category) {
      super(textureName, slots, mapColor, category);
      this.canPlaceOnLiquid = true;
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
      boolean onWater = level.isLiquidTile(tileX, tileY);
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY) - 32;
      GameTexture texture = this.texture;
      if (this.openTexture != null) {
         ObjectEntity ent = level.entityManager.getObjectEntity(tileX, tileY);
         if (ent != null && ent.implementsOEUsers() && ((OEUsers)ent).isInUse()) {
            texture = this.openTexture;
         }
      }

      LevelTile levelTile = level.getLevelTile(tileX, tileY);
      float bobbingStrength = GameMath.max(1.0F, level.liquidManager.getDepthPercent(tileX, tileY));
      float liquidBobbing = levelTile.getLiquidBobbing() * bobbingStrength;
      drawY += (int)liquidBobbing;
      int rotation = level.getObjectRotation(tileX, tileY) % (texture.getWidth() / 32);
      final TextureDrawOptionsEnd options = texture.initDraw()
         .sprite(rotation, 0, 32, texture.getHeight())
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyTreasure(perspective)
         .pos(drawX, drawY);
      if (onWater) {
         float wavesStrength = liquidBobbing * 1.2F;
         MaskShaderOptions swimMask = new MaskShaderOptions(MobRegistry.Textures.swimmask, 0, 0, 0, 4 + (int)wavesStrength);
         options.addMaskShader(swimMask);
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
}
