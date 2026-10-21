package necesse.level.gameObject.happinessObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.ObjectHoverHitbox;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.EnjoyArtTileLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.SidedRotationMultiTile;

public class SwampGuardianLargePaintingObject extends GameObject implements HappinessObject {
   public GameTexture texture;
   protected int counterID;
   private final String texturePath;
   public Supplier<GameTooltips> tooltipSupplier;

   protected SwampGuardianLargePaintingObject(String texturePath, Item.Rarity rarity) {
      this.texturePath = texturePath;
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.rarity = rarity;
      this.validObjectLayers.add(ObjectLayerRegistry.WALL_DECOR);
      this.setItemCategory("objects", "decorations", "paintings");
      this.setCraftingCategory("objects", "decorations", "paintings");
      this.roomProperties.add("painting");
      this.replaceCategories.add("painting");
      this.canReplaceCategories.add("painting");
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/" + this.texturePath);
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new SidedRotationMultiTile(0, 0, 2, 1, rotation, true, this.getID(), this.counterID);
   }

   @Override
   public void addLayerDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int rotation = level.getObjectRotation(layerID, tileX, tileY);
      final DrawOptionsList options = new DrawOptionsList();
      final int sortY;
      if (rotation == 0) {
         sortY = 32;
         options.add(
            this.texture.initDraw().sprite(0, 2, 32, 64).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 16)
         );
      } else if (rotation == 1) {
         sortY = 24;
         options.add(
            this.texture.initDraw().sprite(0, 6, 32, 32).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 16)
         );
      } else if (rotation == 2) {
         sortY = 0;
         options.add(
            this.texture.initDraw().sprite(1, 0, 32, 64).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 64)
         );
      } else {
         sortY = 24;
         options.add(
            this.texture.initDraw().sprite(1, 3, 32, 32).addObjectDamageOverlay(this, level, layerID, tileX, tileY).light(light).pos(drawX, drawY - 16)
         );
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return sortY;
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
      if (rotation == 0) {
         this.texture.initDraw().sprite(0, 2, 32, 64).alpha(alpha).draw(drawX, drawY - 16);
      } else if (rotation == 1) {
         this.texture.initDraw().sprite(0, 6, 32, 32).alpha(alpha).draw(drawX, drawY - 16);
      } else if (rotation == 2) {
         this.texture.initDraw().sprite(1, 0, 32, 64).alpha(alpha).draw(drawX, drawY - 64);
      } else {
         this.texture.initDraw().sprite(1, 3, 32, 32).alpha(alpha).draw(drawX, drawY - 16);
      }
   }

   @Override
   public ArrayList<ObjectPlaceOption> getPlaceOptions(Level level, int levelX, int levelY, PlayerMob playerMob, int playerDir, boolean offsetMultiTile) {
      ArrayList<ObjectPlaceOption> options = new ArrayList<>();

      for (int i = 0; i < 4; i++) {
         int currentRotation = (playerDir + i) % 4;
         options.addAll(super.getPlaceOptions(level, levelX, levelY, playerMob, currentRotation, offsetMultiTile));
      }

      return options;
   }

   @Override
   protected ObjectHoverHitbox getHoverHitbox(Level level, int layerID, int tileX, int tileY) {
      byte rotation = level.getObjectRotation(layerID, tileX, tileY);
      if (rotation == 0) {
         return new ObjectHoverHitbox(layerID, tileX, tileY, 0, 0, 32, 32, 32);
      } else {
         return rotation != 1 && rotation != 3
            ? new ObjectHoverHitbox(layerID, tileX, tileY, 0, -32, 32, 64, 0)
            : new ObjectHoverHitbox(layerID, tileX, tileY, 0, -16, 32, 48, 32);
      }
   }

   @Override
   public boolean canReplace(Level level, int layerID, int tileX, int tileY, int rotation) {
      return super.canReplace(level, layerID, tileX, tileY, rotation) && this.attachesToObject(level, tileX, tileY, rotation);
   }

   public boolean attachesToObject(Level level, int tileX, int tileY) {
      return level.getObject(tileX, tileY).isWall && !level.getObject(tileX, tileY).isDoor;
   }

   public boolean attachesToObject(Level level, int tileX, int tileY, int rotation) {
      if (rotation == 0 && this.attachesToObject(level, tileX, tileY + 1)) {
         return true;
      } else if (rotation == 1 && this.attachesToObject(level, tileX - 1, tileY)) {
         return true;
      } else {
         return rotation == 2 && this.attachesToObject(level, tileX, tileY - 1) ? true : rotation == 3 && this.attachesToObject(level, tileX + 1, tileY);
      }
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      MultiTile multiTile = this.getMultiTile(rotation);
      if (multiTile.isMaster) {
         String childError = multiTile.streamOtherObjects(x, y)
            .map(e -> e.value.canPlace(level, layerID, e.tileX, e.tileY, rotation, byPlayer, ignoreOtherLayers))
            .reduce(null, (prev, e) -> prev != null ? prev : e);
         if (childError != null) {
            return childError;
         }
      }

      if (this.isTilePlaceOccupied(level, layerID, x, y, ignoreOtherLayers)) {
         return "occupied";
      }

      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return "tilecovered";
         }
      }

      return this.attachesToObject(level, x, y, rotation) ? null : "nowall";
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      int rotation = level.getObjectRotation(layerID, x, y);
      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return false;
         }
      }

      return !this.attachesToObject(level, x, y, rotation)
         ? false
         : this.getMultiTile(rotation).streamOtherIDs(x, y).allMatch(e -> level.getObjectID(layerID, e.tileX, e.tileY) == e.value);
   }

   @Override
   public List<TileLevelJob> getLevelJobs(Level level, int tileX, int tileY) {
      return Collections.singletonList(new EnjoyArtTileLevelJob(tileX, tileY, this, level.getObjectRotation(tileX, tileY), 2));
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      if (this.tooltipSupplier != null) {
         GameTooltips tooltip = this.tooltipSupplier.get();
         if (tooltip != null) {
            tooltips.add(tooltip);
         }
      }

      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }

   public static int[] registerLargePainting(String stringID, Item.Rarity rarity, float brokerValue, boolean isObtainable, boolean countInStats) {
      SwampGuardianLargePaintingObject obj1 = new SwampGuardianLargePaintingObject(stringID, rarity);
      SwampGuardianLargePaintingObject2 obj2 = new SwampGuardianLargePaintingObject2(stringID, rarity);
      int i1 = ObjectRegistry.registerObject(stringID, obj1, brokerValue, isObtainable, countInStats);
      int i2 = ObjectRegistry.registerObject(stringID + "2", obj2, 0.0F, false);
      obj1.counterID = i2;
      obj2.counterID = i1;
      return new int[]{i1, i2};
   }
}
