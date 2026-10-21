package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SmallRuneStoneObject extends GameObject {
   protected GameTexture texture;
   protected GameTexture lightTexture;
   protected GameTexture shadowTexture;
   protected final GameRandom drawRandom;

   public SmallRuneStoneObject() {
      super(new Rectangle(4, 8, 24, 16));
      this.mapColor = new Color(136, 214, 255);
      this.toolType = ToolType.PICKAXE;
      this.toolTier = 1.0F;
      this.rarity = Item.Rarity.UNCOMMON;
      this.setItemCategory("objects", "landscaping", "plainsrocksandores");
      this.setCraftingCategory("objects", "landscaping", "plainsrocksandores");
      this.objectHealth = 80;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.hoverHitbox = new Rectangle(4, -4, 24, 24);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY)
         ? super.getLootTable(level, layerID, tileX, tileY)
         : new LootTable(new LootItem("runestone", GameRandom.globalRandom.getIntBetween(1, 2)));
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/smallrunestone");
      this.lightTexture = GameTexture.fromFile("objects/smallrunestone_light");
      this.shadowTexture = GameTexture.fromFile("objects/smallrunestone_shadow");
   }

   public float getDesiredHeight(Level level, int tileX, int tileY) {
      int seededOffset = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(3000);
      float perc = GameUtils.getAnimFloat(level.getWorldEntity().getTime() + seededOffset, 3000);
      return GameMath.sin(perc * 360.0F) * 5.0F;
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
      int sprite;
      boolean mirrored;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
         mirrored = this.drawRandom.seeded(getTileSeed(tileX, tileY, 5)).nextBoolean();
      }

      final TextureDrawOptionsEnd drawOptions = this.texture
         .initDraw()
         .sprite(sprite, 0, 32, 64)
         .mirror(mirrored, false)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applySpelunker(perspective)
         .pos(drawX, drawY - (int)this.getDesiredHeight(level, tileX, tileY) - (this.texture.getHeight() - 32));
      final TextureDrawOptionsEnd lightDrawOptions = this.lightTexture
         .initDraw()
         .sprite(sprite, 0, 32, 64)
         .mirror(mirrored, false)
         .light(light.minLevelCopy(150.0F))
         .pos(drawX, drawY - (int)this.getDesiredHeight(level, tileX, tileY) - (this.texture.getHeight() - 32));
      final TextureDrawOptionsEnd shadowDrawOptions = this.shadowTexture
         .initDraw()
         .sprite(sprite, 0, 32, 64)
         .mirror(mirrored, false)
         .light(light)
         .pos(drawX, drawY - (this.texture.getHeight() - 32));
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            shadowDrawOptions.draw();
            drawOptions.draw();
            lightDrawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      boolean mirrored;
      int sprite;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
         mirrored = this.drawRandom.seeded(getTileSeed(tileX, tileY, 5)).nextBoolean();
      }

      this.texture.initDraw().sprite(sprite, 0, 32, 64).mirror(mirrored, false).alpha(alpha).draw(drawX, drawY - (this.texture.getHeight() - 32));
   }

   @Override
   public boolean shouldSnapSmartMining(Level level, int x, int y) {
      return true;
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return !level.isShore(x, y);
   }
}
