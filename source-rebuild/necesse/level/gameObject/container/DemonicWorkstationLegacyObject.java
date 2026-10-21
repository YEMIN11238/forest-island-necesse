package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class DemonicWorkstationLegacyObject extends CraftingStationObject {
   public GameTexture texture;

   public DemonicWorkstationLegacyObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(156, 51, 39);
      this.rarity = Item.Rarity.COMMON;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.lightLevel = 100;
      this.lightHue = 270.0F;
      this.lightSat = 0.3F;
      this.hoverHitbox = new Rectangle(0, -10, 32, 42);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable(new LootItem(this.getStringID()).preventLootMultiplier());
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("tungstenworkstation"), new Ingredient("tungstenbar", 8), new Ingredient("quartz", 4));
   }

   @Override
   public HashSet<ItemCategory> getForcedSoloCraftingCategories() {
      HashSet<ItemCategory> depths = super.getForcedSoloCraftingCategories();
      depths.add(ItemCategory.craftingManager.getCategory("equipment", "trinkets"));
      return depths;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/demonicworkstation");
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      return rotation % 2 == 0 ? new Rectangle(x * 32 + 2, y * 32 + 6, 28, 20) : new Rectangle(x * 32 + 6, y * 32 + 2, 20, 28);
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
      int rotation = level.getObjectRotation(tileX, tileY) % 4;
      int yOffset = rotation % 2 == 0 ? -2 : 0;
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(rotation % 4, 0, 64)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX - 16, drawY - 32 + yOffset);
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
      int yOffset = rotation % 2 == 0 ? -2 : 0;
      this.texture.initDraw().sprite(rotation % 4, 0, 64).alpha(alpha).draw(drawX - 16, drawY - 32 + yOffset);
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.DEMONIC_WORKSTATION, RecipeTechRegistry.WORKSTATION};
   }

   public LocalMessage getCraftingHeader() {
      return new LocalMessage("ui", "demoniccrafting");
   }
}
