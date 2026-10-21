package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WorkstationLegacyObject extends CraftingStationObject {
   public GameTexture texture;

   public WorkstationLegacyObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(132, 91, 25);
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -10, 32, 42);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable(new LootItem(this.getStringID()).preventLootMultiplier());
   }

   @Override
   public CraftingStationUpgrade getStationUpgrade() {
      return new CraftingStationUpgrade(ObjectRegistry.getObject("demonicworkstationduo"), new Ingredient("demonicbar", 5));
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/workstation");
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
      final TextureDrawOptions drawOptions = this.texture
         .initDraw()
         .sprite(rotation % 4, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() + 32 + yOffset);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int yOffset = rotation % 2 == 0 ? -2 : 0;
      this.texture.initDraw().sprite(rotation % 4, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32 + yOffset);
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.WORKSTATION};
   }
}
