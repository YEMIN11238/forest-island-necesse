package necesse.level.gameObject;

import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.BannerObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BannerObject extends GameObject {
   public GameTexture texture;
   protected int xOffset = 0;
   protected final GameRandom drawRandom;
   final int animTime = 1600;

   public BannerObject() {
      super(new Rectangle(5, 4, 22, 20));
      this.displayMapTooltip = true;
      this.stackSize = 10;
      this.objectHealth = 100;
      this.isLightTransparent = true;
      this.rarity = Item.Rarity.NORMAL;
      this.drawRandom = new GameRandom();
      this.setItemCategory("objects", "decorations", "banners");
      this.setCraftingCategory("objects", "decorations", "banners");
   }

   public BannerObject(int xOffset) {
      this();
      this.xOffset = xOffset;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.getStringID());
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
      int textureWidth = this.texture.getWidth() / 4;
      int textureHeight = this.texture.getHeight();
      int sprite = GameUtils.getAnim(Math.abs(level.getTime() + getTileSeed(tileX, tileY, 52)), 4, 1600);
      Consumer<TextureDrawOptionsPositionMod> waveChange;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         waveChange = GameResources.waveShader
            .setupGrassWaveMod(level, tileX, tileY, 1000L, 0.02F, 2, this.drawRandom, getTileSeed(tileX, tileY, 0), false, 3.0F);
      }

      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(sprite, 0, textureWidth, textureHeight)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .addPositionMod(waveChange)
         .pos(drawX - textureWidth / 4 + this.xOffset, drawY - textureHeight + 32);
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
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      if (Settings.windEffects) {
         float windSpeed = level.weatherLayer.getWindSpeed();
         if (windSpeed > 0.2F) {
            float windAmount = level.weatherLayer.getWindAmount(tileX, tileY) * 3.0F;
            if (windAmount > 0.5F) {
               float buffer = 0.016666668F * windAmount * windSpeed;
               if (buffer >= 1.0F || GameRandom.globalRandom.getChance(buffer)) {
                  level.makeGrassWeave(tileX, tileY, 1600, false);
               }
            }
         }
      }
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new BannerObjectEntity(level, x, y);
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int textureWidth = this.texture.getWidth() / 4;
      int textureHeight = this.texture.getHeight();
      this.texture.initDraw().sprite(0, 0, textureWidth, textureHeight).alpha(alpha).draw(drawX - textureWidth / 4 + this.xOffset, drawY - textureHeight + 32);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "cosmeticbannertip"), 400);
      return tooltips;
   }

   @Override
   public List<ObjectHoverHitbox> getHoverHitboxes(Level level, int layerID, int tileX, int tileY) {
      List<ObjectHoverHitbox> list = super.getHoverHitboxes(level, layerID, tileX, tileY);
      list.add(new ObjectHoverHitbox(layerID, tileX, tileY, 0, -32, 32, 32));
      return list;
   }
}
