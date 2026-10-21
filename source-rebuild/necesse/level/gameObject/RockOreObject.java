package necesse.level.gameObject;

import java.awt.Color;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.MergeFunction;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;

public class RockOreObject extends RockObject {
   protected String oreMaskTextureName;
   protected String oreTextureName;
   public RockObject parentRock;
   public GameTexture[] oreTextures;
   public long oreHash;
   public String droppedOre;
   public int droppedOreMin;
   public int droppedOreMax;
   public int placedDroppedOre;
   protected final GameRandom oreTextureRandom;

   public RockOreObject(
      RockObject parentRock,
      String oreMaskTextureName,
      String oreTextureName,
      Color oreColor,
      String droppedOre,
      int droppedOreMin,
      int droppedOreMax,
      int placedDroppedOre,
      boolean isIncursionExtractionObject,
      String... category
   ) {
      super("rock", parentRock.mapColor, parentRock.droppedStone);
      this.parentRock = parentRock;
      this.toolTier = parentRock.toolTier;
      this.isOre = true;
      this.oreMaskTextureName = oreMaskTextureName;
      this.oreTextureName = oreTextureName;
      this.mapColor = oreColor;
      this.oreHash = oreColor.hashCode();
      this.droppedOre = droppedOre;
      this.droppedOreMin = droppedOreMin;
      this.droppedOreMax = droppedOreMax;
      this.placedDroppedOre = placedDroppedOre;
      this.oreTextureRandom = new GameRandom();
      this.isIncursionExtractionObject = isIncursionExtractionObject;
      this.displayMapTooltip = true;
      if (category.length > 0) {
         this.setItemCategory(category);
         this.setCraftingCategory(category);
      } else {
         this.setItemCategory("objects", "landscaping", "rocksandores");
         this.setCraftingCategory("objects", "landscaping", "rocksandores");
      }
   }

   public RockOreObject(
      RockObject parentRock,
      String oreMaskTextureName,
      String oreTextureName,
      Color oreColor,
      String droppedOre,
      int droppedOreMin,
      int droppedOreMax,
      int placedDroppedOre,
      String... category
   ) {
      this(parentRock, oreMaskTextureName, oreTextureName, oreColor, droppedOre, droppedOreMin, droppedOreMax, placedDroppedOre, true, category);
   }

   public RockOreObject(RockObject parentRock, String oreMaskTextureName, String oreTextureName, Color oreColor, String droppedOre, String... category) {
      this(parentRock, oreMaskTextureName, oreTextureName, oreColor, droppedOre, 1, 3, 2, category);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", "rockoreformat", "rock", this.parentRock.getNewLocalization(), "ore", new LocalMessage("object", this.oreTextureName));
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         return super.getLootTable(level, layerID, tileX, tileY);
      }

      LootTable out = this.parentRock.getLootTable(level, layerID, tileX, tileY);
      if (this.droppedOre != null) {
         out.items.add(LootItem.between(this.droppedOre, this.droppedOreMin, this.droppedOreMax).splitItems(5));
      }

      return out;
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
      this.addRockDrawables(
         list, level, tileX, tileY, this.parentRock.rockTextures, this.getOreTexture(tileX, tileY), this.oreHash, tickManager, camera, perspective
      );
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      LinkedList<LevelSortedDrawable> list = new LinkedList<>();
      this.addRockDrawables(
         list, level, tileX, tileY, this.parentRock.rockTextures, this.getOreTexture(tileX, tileY), this.oreHash, alpha, null, camera, player
      );
      list.forEach(e -> e.draw(null));
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      GameTexture oreTemp = GameTexture.fromFile("objects/" + this.oreTextureName);
      this.oreTextures = new GameTexture[oreTemp.getWidth() / 32];

      GameTexture oreMask;
      try {
         oreMask = GameTexture.fromFileRaw("objects/" + this.oreMaskTextureName);
      } catch (FileNotFoundException e) {
         oreMask = GameTexture.fromFile("objects/oremask");
      }

      for (int i = 0; i < this.oreTextures.length; i++) {
         GameTexture oreTexture = new GameTexture(oreMask);

         for (int x = 0; x < oreTexture.getWidth(); x += 32) {
            for (int y = 0; y < oreTexture.getHeight(); y += 32) {
               oreTexture.merge(oreTemp, x, y, i * 32, 0, 32, 32, (cCol, mCol) -> {
                  float cRed = cCol.getRed() / 255.0F;
                  float cGreen = cCol.getGreen() / 255.0F;
                  float cBlue = cCol.getBlue() / 255.0F;
                  float cAlpha = cCol.getAlpha() / 255.0F;
                  float mRed = mCol.getRed() / 255.0F;
                  float mGreen = mCol.getGreen() / 255.0F;
                  float mBlue = mCol.getBlue() / 255.0F;
                  float mAlpha = mCol.getAlpha() / 255.0F;
                  return new Color(cRed * mRed, cGreen * mGreen, cBlue * mBlue, cAlpha * mAlpha);
               });
            }
         }

         this.oreTextures[i] = oreTexture;
      }
   }

   protected GameTexture getOreTexture(int tileX, int tileY) {
      int oreTexture;
      synchronized (this.oreTextureRandom) {
         oreTexture = this.oreTextureRandom.seeded(getTileSeed(tileX, tileY) * 8629L).nextInt(this.oreTextures.length);
      }

      return this.oreTextures[oreTexture];
   }

   @Override
   public GameTexture generateItemTexture() {
      GameTexture oreTemp = GameTexture.fromFile("objects/" + this.oreTextureName);

      GameTexture oreMask;
      try {
         oreMask = GameTexture.fromFileRaw("items/" + this.oreMaskTextureName);
      } catch (FileNotFoundException e) {
         oreMask = GameTexture.fromFile("items/oremask");
      }

      GameTexture oreTexture = new GameTexture(oreMask);
      oreTexture.merge(oreTemp, 0, 0, 0, 0, 32, 32, (cCol, mCol) -> {
         float cRed = cCol.getRed() / 255.0F;
         float cGreen = cCol.getGreen() / 255.0F;
         float cBlue = cCol.getBlue() / 255.0F;
         float cAlpha = cCol.getAlpha() / 255.0F;
         float mRed = mCol.getRed() / 255.0F;
         float mGreen = mCol.getGreen() / 255.0F;
         float mBlue = mCol.getBlue() / 255.0F;
         float mAlpha = mCol.getAlpha() / 255.0F;
         return new Color(cRed * mRed, cGreen * mGreen, cBlue * mBlue, cAlpha * mAlpha);
      });
      GameTexture itemTexture = new GameTexture(this.parentRock.generateItemTexture());
      itemTexture.merge(oreTexture, 0, 0, MergeFunction.NORMAL);
      itemTexture.makeFinal();
      return itemTexture;
   }
}
