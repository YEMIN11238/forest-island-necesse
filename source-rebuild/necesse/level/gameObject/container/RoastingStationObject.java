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

public class RoastingStationObject extends CampfireAddonObject {
   public GameTexture texture;
   public GameTexture roasterTexture;

   public RoastingStationObject() {
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
      this.roasterTexture = GameTexture.fromFile("objects/campfire_roaster");
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
      TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(0, 0, 32, 32)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      final DrawOptions roasterOptions = this.roasterTexture
         .initDraw()
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.roasterTexture.getHeight() - 32));
      boolean isFueled = false;
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof CampfireObjectEntity) {
         CampfireObjectEntity coe = (CampfireObjectEntity)objectEntity;
         isFueled = coe.isFueled();
      }

      TextureDrawOptions flame;
      if (isFueled) {
         int spriteX = (int)(level.getWorldEntity().getWorldTime() % 6000L / 2000L);
         flame = this.texture.initDraw().sprite(spriteX + 1, 0, 32).light(light).pos(drawX, drawY);
      } else {
         flame = null;
      }

      tileList.add(tm -> {
         options.draw();
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
            roasterOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, 32).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
      this.roasterTexture.initDraw().alpha(alpha).draw(drawX, drawY - (this.roasterTexture.getHeight() - 32));
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.ROASTING_STATION};
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "roastingtip"));
      return tooltips;
   }
}
