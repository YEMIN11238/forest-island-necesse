package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.CampfireObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CookingPotObject extends CampfireAddonObject {
   public GameTexture texture;
   public GameTexture potTexture;

   public CookingPotObject() {
      this.mapColor = new Color(233, 134, 39);
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
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/campfire");
      this.potTexture = GameTexture.fromFile("objects/campfire_pot");
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
      DrawOptions baseOptions = this.texture
         .initDraw()
         .sprite(0, 0, 32, 32)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      final DrawOptions potOptions = this.potTexture
         .initDraw()
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.potTexture.getHeight() - 32));
      boolean isFueled = false;
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof CampfireObjectEntity) {
         CampfireObjectEntity coe = (CampfireObjectEntity)objectEntity;
         isFueled = coe.isFueled();
      }

      TextureDrawOptions flame;
      if (isFueled) {
         int spriteX = (int)(level.getWorldTime() % 6000L / 2000L);
         flame = this.texture.initDraw().sprite(spriteX + 1, 0, 32).light(light).pos(drawX, drawY);
      } else {
         flame = null;
      }

      tileList.add(tm -> {
         baseOptions.draw();
         if (flame != null) {
            flame.draw();
         }
      });
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            potOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, 32).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
      this.potTexture.initDraw().alpha(alpha).draw(drawX, drawY - (this.potTexture.getHeight() - 32));
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.COOKING_POT};
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "cookingpottip"));
      return tooltips;
   }
}
