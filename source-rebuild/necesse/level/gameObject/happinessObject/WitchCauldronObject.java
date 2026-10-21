package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.StaticMultiObject;
import necesse.level.gameObject.furniture.RoomFurniture;
import necesse.level.maps.Level;

public class WitchCauldronObject extends StaticMultiObject implements RoomFurniture, HappinessObject {
   protected WitchCauldronObject(String texturePath, int multiX, int multiY, int multiWidth, int multiHeight, int[] multiIDs, Rectangle fullCollision) {
      super(multiX, multiY, multiWidth, multiHeight, multiIDs, fullCollision, texturePath);
      this.stackSize = 1;
      this.rarity = Item.Rarity.EPIC;
      this.mapColor = new Color(143, 143, 143);
      this.objectHealth = 100;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, 0, 32, 32);
      this.setItemCategory("objects", "furniture");
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      GameRandom random = GameRandom.globalRandom;
      if (random.getChance(0.25F)) {
         float xOffset = this.multiX == 0 ? 8.0F : -8.0F;
         float yOffset = this.multiY == 0 ? 16.0F : 0.0F;
         level.entityManager
            .addTopParticle(
               tileX * 32 + xOffset + random.getFloatBetween(0.0F, 32.0F), tileY * 32 + yOffset + random.getFloatBetween(0.0F, 32.0F), Particle.GType.COSMETIC
            )
            .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
            .movesFriction(0.0F, -35.0F, 0.8F)
            .height(32.0F)
            .sizeFades(12, 24)
            .color(new Color(214, 254, 17))
            .fadesAlphaTimeToCustomAlpha(200, 200, 0.25F)
            .lifeTime(1000);
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
      int anim = (int)(level.getWorldEntity().getTime() / 200L % 4L);
      GameSprite sprite = new GameSprite(this.texture, anim, 0, 64, 96, 64, 96);
      final DrawOptions options = this.getMultiTextureDrawOptions(sprite, level, tileX, tileY, camera);
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
      GameSprite sprite = new GameSprite(this.texture, 0, 0, 64, 96, 64, 96);
      this.drawMultiTexturePreview(sprite, tileX, tileY, alpha, camera);
   }

   public static int[] registerWitchCauldron(String texturePath) {
      int[] ids = new int[4];
      Rectangle collision = new Rectangle(4, 8, 56, 48);
      ids[0] = ObjectRegistry.registerObject(texturePath, new WitchCauldronObject(texturePath, 0, 0, 2, 2, ids, collision), 1000.0F, true, false);
      ids[1] = ObjectRegistry.registerObject(texturePath + "2", new WitchCauldronObject(texturePath, 1, 0, 2, 2, ids, collision), 0.0F, false);
      ids[2] = ObjectRegistry.registerObject(texturePath + "3", new WitchCauldronObject(texturePath, 0, 1, 2, 2, ids, collision), 0.0F, false);
      ids[3] = ObjectRegistry.registerObject(texturePath + "4", new WitchCauldronObject(texturePath, 1, 1, 2, 2, ids, collision), 0.0F, false);
      return ids;
   }

   @Override
   public String getFurnitureType() {
      return null;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
