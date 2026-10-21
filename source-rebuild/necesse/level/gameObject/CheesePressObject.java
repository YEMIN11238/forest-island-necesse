package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.CheesePressObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.ProcessingInventoryObjectEntity;
import necesse.entity.objectEntity.ProcessingTechInventoryObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationObject;
import necesse.level.maps.light.GameLight;

public class CheesePressObject extends GameObject implements SettlementWorkstationObject {
   public GameTexture texture;
   public GameTexture onTexture;

   public CheesePressObject() {
      super(new Rectangle(4, 6, 24, 20));
      this.setItemCategory("objects", "craftingstations");
      this.setCraftingCategory("craftingstations");
      this.mapColor = new Color(143, 120, 114);
      this.displayMapTooltip = true;
      this.toolType = ToolType.ALL;
      this.rarity = Item.Rarity.COMMON;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.replaceCategories.add("workstation");
      this.canReplaceCategories.add("workstation");
      this.canReplaceCategories.add("wall");
      this.canReplaceCategories.add("furniture");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/cheesepress");

      try {
         this.onTexture = GameTexture.fromFileRaw("objects/cheesepress_on");
      } catch (FileNotFoundException e) {
         this.onTexture = this.texture;
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int rotation = level.getObjectRotation(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      GameTexture texture = this.texture;
      if (objectEntity instanceof ProcessingInventoryObjectEntity && ((ProcessingInventoryObjectEntity)objectEntity).isProcessing()) {
         texture = this.onTexture;
      }

      final TextureDrawOptions options = texture.initDraw()
         .sprite(rotation % texture.getWidth() / 32, 0, 32, texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - (texture.getHeight() - 32));
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
         .sprite(rotation % this.texture.getWidth() / 32, 0, 32, this.texture.getHeight())
         .alpha(alpha)
         .draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public String getInteractTip(Level level, int x, int y, PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "opentip");
   }

   @Override
   public boolean canInteract(Level level, int x, int y, PlayerMob player) {
      return true;
   }

   @Override
   public void interact(Level level, int x, int y, PlayerMob player) {
      super.interact(level, x, y, player);
      if (level.isServer()) {
         OEInventoryContainer.openAndSendContainer(ContainerRegistry.PROCESSING_INVENTORY_CONTAINER, player.getServerClient(), level, x, y);
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new CheesePressObjectEntity(level, x, y);
   }

   public ProcessingTechInventoryObjectEntity getProcessingObjectEntity(Level level, int tileX, int tileY) {
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      return objectEntity instanceof ProcessingTechInventoryObjectEntity ? (ProcessingTechInventoryObjectEntity)objectEntity : null;
   }

   @Override
   public Stream<Recipe> streamSettlementRecipes(Level level, int tileX, int tileY) {
      ProcessingTechInventoryObjectEntity processingOE = this.getProcessingObjectEntity(level, tileX, tileY);
      return processingOE != null ? Recipes.streamRecipes(processingOE.techs) : Stream.empty();
   }

   @Override
   public boolean isProcessingInventory(Level level, int tileX, int tileY) {
      return true;
   }

   @Override
   public boolean canCurrentlyCraft(Level level, int tileX, int tileY, Recipe recipe) {
      ProcessingTechInventoryObjectEntity processingOE = this.getProcessingObjectEntity(level, tileX, tileY);
      return processingOE != null ? processingOE.getExpectedResults().crafts < 10 : false;
   }

   @Override
   public int getMaxCraftsAtOnce(Level level, int tileX, int tileY, Recipe recipe) {
      return 10;
   }

   @Override
   public InventoryRange getProcessingInputRange(Level level, int tileX, int tileY) {
      ProcessingTechInventoryObjectEntity processingOE = this.getProcessingObjectEntity(level, tileX, tileY);
      return processingOE != null ? processingOE.getInputInventoryRange() : null;
   }

   @Override
   public InventoryRange getProcessingOutputRange(Level level, int tileX, int tileY) {
      ProcessingTechInventoryObjectEntity processingOE = this.getProcessingObjectEntity(level, tileX, tileY);
      return processingOE != null ? processingOE.getOutputInventoryRange() : null;
   }

   @Override
   public ArrayList<InventoryItem> getCurrentAndFutureProcessingOutputs(Level level, int tileX, int tileY) {
      ProcessingTechInventoryObjectEntity processingOE = this.getProcessingObjectEntity(level, tileX, tileY);
      return processingOE != null ? processingOE.getCurrentAndExpectedResults().items : new ArrayList<>();
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "cheesepresstip"));
      return tooltips;
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }
}
