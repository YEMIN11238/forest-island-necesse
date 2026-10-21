package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.GrainMillObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.ProcessingTechInventoryObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.object.OEInventoryContainer;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementWorkstationObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

public abstract class GrainMillBaseObject extends GameObject {
   public static int[] registerGrainMill() {
      GrainMillBaseObject.GrainMillObject o1;
      int id1 = ObjectRegistry.registerObject("grainmill", o1 = new GrainMillBaseObject.GrainMillObject(), 10.0F, true);
      GrainMillBaseObject.GrainMillObject2 o2;
      int id2 = ObjectRegistry.registerObject("grainmill2", o2 = new GrainMillBaseObject.GrainMillObject2(), 0.0F, false);
      GrainMillBaseObject.GrainMillObject3 o3;
      int id3 = ObjectRegistry.registerObject("grainmill3", o3 = new GrainMillBaseObject.GrainMillObject3(), 0.0F, false);
      GrainMillBaseObject.GrainMillObject4 o4;
      int id4 = ObjectRegistry.registerObject("grainmill4", o4 = new GrainMillBaseObject.GrainMillObject4(), 0.0F, false);
      o1.setCounterIDs(id1, id2, id3, id4);
      o2.setCounterIDs(id1, id2, id3, id4);
      o3.setCounterIDs(id1, id2, id3, id4);
      o4.setCounterIDs(id1, id2, id3, id4);
      return new int[]{id1, id2, id3, id4};
   }

   public GrainMillBaseObject() {
      super(new Rectangle(32, 32));
      this.setItemCategory("objects", "craftingstations");
      this.setCraftingCategory("craftingstations");
      this.mapColor = new Color(93, 67, 24);
      this.displayMapTooltip = true;
      this.toolType = ToolType.ALL;
      this.objectHealth = 100;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
      this.replaceCategories.add("workstation");
      this.canReplaceCategories.add("workstation");
      this.canReplaceCategories.add("wall");
      this.canReplaceCategories.add("furniture");
   }

   protected abstract void setCounterIDs(int var1, int var2, int var3, int var4);

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
      if (this.isMultiTileMaster()) {
         if (level.isServer()) {
            OEInventoryContainer.openAndSendContainer(ContainerRegistry.PROCESSING_INVENTORY_CONTAINER, player.getServerClient(), level, x, y);
         }
      } else {
         this.getMultiTile(level.getObjectRotation(x, y)).getMasterLevelObject(level, 0, x, y).ifPresent(e -> e.interact(player));
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return this.isMultiTileMaster() ? new GrainMillObjectEntity(level, x, y) : null;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "grainmilltip"));
      return tooltips;
   }

   @Override
   protected boolean shouldPlayInteractSound(Level level, int tileX, int tileY) {
      return true;
   }

   public static class GrainMillObject extends GrainMillBaseObject implements SettlementWorkstationObject {
      public GameTexture texture;
      public GameTexture bladeTexture;
      protected int counterIDTopRight;
      protected int counterIDBotLeft;
      protected int counterIDBotRight;

      protected GrainMillObject() {
      }

      @Override
      protected Rectangle getCollision(Level level, int x, int y, int rotation) {
         if (rotation == 0) {
            return new Rectangle(x * 32 + 5, y * 32 + 12, 27, 20);
         } else if (rotation == 1) {
            return new Rectangle(x * 32, y * 32 + 12, 27, 20);
         } else {
            return rotation == 2 ? new Rectangle(x * 32, y * 32, 27, 22) : new Rectangle(x * 32 + 5, y * 32, 27, 22);
         }
      }

      @Override
      protected void setCounterIDs(int id1, int id2, int id3, int id4) {
         this.counterIDTopRight = id2;
         this.counterIDBotLeft = id3;
         this.counterIDBotRight = id4;
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new StaticMultiTile(0, 0, 2, 2, rotation, true, this.getID(), this.counterIDTopRight, this.counterIDBotLeft, this.counterIDBotRight);
      }

      @Override
      public void loadTextures() {
         super.loadTextures();
         this.texture = GameTexture.fromFile("objects/grainmill");
         this.bladeTexture = GameTexture.fromFile("objects/grainmillblade");
      }

      public GrainMillObjectEntity getGrainMillObjectEntity(Level level, int tileX, int tileY) {
         ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
         return objectEntity instanceof GrainMillObjectEntity ? (GrainMillObjectEntity)objectEntity : null;
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
         float angle = 0.0F;
         GrainMillObjectEntity grainMill = this.getGrainMillObjectEntity(level, tileX, tileY);
         if (grainMill != null) {
            angle = grainMill.bladeRotation;
         }

         int originalTileX = tileX;
         int originalTileY = tileY;
         byte rotation = level.getObjectRotation(tileX, tileY);
         if (rotation == 1) {
            tileX--;
         } else if (rotation == 2) {
            tileX--;
            tileY--;
         } else if (rotation == 3) {
            tileY--;
         }

         int drawX = camera.getTileDrawX(tileX);
         int drawY = camera.getTileDrawY(tileY);
         final DrawOptions options = this.texture
            .initDraw()
            .addObjectDamageOverlay(this, level, originalTileX, originalTileY)
            .light(light)
            .pos(drawX, drawY - (this.texture.getHeight() - 64));
         final DrawOptions bladeOptions = this.bladeTexture
            .initDraw()
            .light(light)
            .rotate(angle, this.bladeTexture.getWidth() / 2, this.bladeTexture.getHeight() / 2)
            .posMiddle(drawX + this.texture.getWidth() / 2, drawY - 16);
         list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
               return 40;
            }

            @Override
            public void draw(TickManager tickManager) {
               options.draw();
               bladeOptions.draw();
            }
         });
      }

      @Override
      public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
         if (rotation == 1) {
            tileX--;
         } else if (rotation == 2) {
            tileX--;
            tileY--;
         } else if (rotation == 3) {
            tileY--;
         }

         int drawX = camera.getTileDrawX(tileX);
         int drawY = camera.getTileDrawY(tileY);
         this.texture.initDraw().alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 64));
         this.bladeTexture.initDraw().alpha(alpha).posMiddle(drawX + this.texture.getWidth() / 2, drawY - 16).draw();
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
         return processingOE != null ? processingOE.getExpectedResults().crafts < 40 : false;
      }

      @Override
      public int getMaxCraftsAtOnce(Level level, int tileX, int tileY, Recipe recipe) {
         return 5;
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
   }

   public static class GrainMillObject2 extends GrainMillBaseObject {
      protected int counterIDTopLeft;
      protected int counterIDBotLeft;
      protected int counterIDBotRight;

      protected GrainMillObject2() {
      }

      @Override
      protected Rectangle getCollision(Level level, int x, int y, int rotation) {
         if (rotation == 0) {
            return new Rectangle(x * 32, y * 32 + 12, 27, 20);
         } else if (rotation == 1) {
            return new Rectangle(x * 32, y * 32, 27, 22);
         } else {
            return rotation == 2 ? new Rectangle(x * 32 + 5, y * 32, 27, 22) : new Rectangle(x * 32 + 5, y * 32 + 12, 27, 20);
         }
      }

      @Override
      protected void setCounterIDs(int id1, int id2, int id3, int id4) {
         this.counterIDTopLeft = id1;
         this.counterIDBotLeft = id3;
         this.counterIDBotRight = id4;
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new StaticMultiTile(1, 0, 2, 2, rotation, false, this.counterIDTopLeft, this.getID(), this.counterIDBotLeft, this.counterIDBotRight);
      }
   }

   public static class GrainMillObject3 extends GrainMillBaseObject {
      protected int counterIDTopLeft;
      protected int counterIDTopRight;
      protected int counterIDBotRight;

      protected GrainMillObject3() {
      }

      @Override
      protected void setCounterIDs(int id1, int id2, int id3, int id4) {
         this.counterIDTopLeft = id1;
         this.counterIDTopRight = id2;
         this.counterIDBotRight = id4;
      }

      @Override
      protected Rectangle getCollision(Level level, int x, int y, int rotation) {
         if (rotation == 0) {
            return new Rectangle(x * 32 + 5, y * 32, 27, 22);
         } else if (rotation == 1) {
            return new Rectangle(x * 32 + 5, y * 32 + 12, 27, 20);
         } else {
            return rotation == 2 ? new Rectangle(x * 32, y * 32 + 12, 27, 20) : new Rectangle(x * 32, y * 32, 27, 22);
         }
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new StaticMultiTile(0, 1, 2, 2, rotation, false, this.counterIDTopLeft, this.counterIDTopRight, this.getID(), this.counterIDBotRight);
      }
   }

   public static class GrainMillObject4 extends GrainMillBaseObject {
      protected int counterIDTopLeft;
      protected int counterIDTopRight;
      protected int counterIDBotLeft;

      protected GrainMillObject4() {
      }

      @Override
      protected void setCounterIDs(int id1, int id2, int id3, int id4) {
         this.counterIDTopLeft = id1;
         this.counterIDTopRight = id2;
         this.counterIDBotLeft = id3;
      }

      @Override
      protected Rectangle getCollision(Level level, int x, int y, int rotation) {
         if (rotation == 0) {
            return new Rectangle(x * 32, y * 32, 27, 22);
         } else if (rotation == 1) {
            return new Rectangle(x * 32 + 5, y * 32, 27, 22);
         } else {
            return rotation == 2 ? new Rectangle(x * 32 + 5, y * 32 + 12, 27, 20) : new Rectangle(x * 32, y * 32 + 12, 27, 20);
         }
      }

      @Override
      public MultiTile getMultiTile(int rotation) {
         return new StaticMultiTile(1, 1, 2, 2, rotation, false, this.counterIDTopLeft, this.counterIDTopRight, this.counterIDBotLeft, this.getID());
      }
   }
}
