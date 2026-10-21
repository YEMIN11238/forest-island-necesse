package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class IronAnvilObject extends CraftingStationObject {
   public GameTexture texture;

   public IronAnvilObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(130, 139, 152);
      this.isLightTransparent = true;
      this.roomProperties.add("metalwork");
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("demonicanvil"), new Ingredient("demonicbar", 5));
   }

   @Override
   public int getCraftingCategoryDepth() {
      return 3;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/ironanvil");
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      return rotation % 2 == 0 ? new Rectangle(x * 32 + 8, y * 32 + 8, 16, 20) : new Rectangle(x * 32 + 5, y * 32 + 14, 22, 16);
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
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(rotation % 4, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 20;
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
      this.texture.initDraw().sprite(rotation % 4, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.IRON_ANVIL};
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "anviltip"));
      return tooltips;
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return SoundSettingsRegistry.anvilOpen;
   }
}
