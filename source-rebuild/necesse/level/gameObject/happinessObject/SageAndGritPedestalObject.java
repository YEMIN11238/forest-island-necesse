package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SageAndGritPedestalObject extends GameObject implements HappinessObject {
   private final boolean isSage;
   protected String textureName;
   public GameTexture texture;

   public SageAndGritPedestalObject(String textureName, boolean isSage, Color mapColor, String... category) {
      super(new Rectangle(32, 32));
      this.textureName = textureName;
      this.toolType = ToolType.ALL;
      this.mapColor = mapColor;
      this.isSage = isSage;
      this.rarity = Item.Rarity.LEGENDARY;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      if (!isSage) {
         this.lightLevel = 100;
         this.lightHue = 37.0F;
         this.lightSat = 0.8F;
      }

      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.setItemCategory(category);
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      GameRandom random = GameRandom.globalRandom;
      if (this.isSage) {
         level.entityManager
            .addTopParticle(tileX * 32 + 16 + random.getIntBetween(-5, 5), tileY * 32 + random.getIntBetween(-5, 5), Particle.GType.COSMETIC)
            .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
            .color(new Color(155, 210, 255))
            .movesFriction(random.getIntBetween(-5, 5), -random.getIntBetween(20, 50), 0.8F)
            .heightMoves(16.0F, -16.0F)
            .fadesAlphaTime(100, 300)
            .lifeTime(500);
      } else {
         int xOffset = random.getIntBetween(-5, 5);
         level.entityManager
            .addParticle(tileX * 32 + 16 + xOffset, tileY * 32 + random.getIntBetween(-5, 5), Particle.GType.COSMETIC)
            .movesConstant(xOffset * -1, 0.0F)
            .heightMoves(16.0F, 48.0F)
            .color((options, lifeTime, timeAlive, lifePercent) -> {
               Color startColor = new Color(255, 219, 5);
               Color endColor = new Color(233, 96, 39);
               int r = GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed());
               int g = GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen());
               int b = GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue());
               options.color(new Color(r, g, b));
            })
            .lifeTime(500);
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
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
      int anim = (int)(level.getWorldEntity().getTime() / 200L % 4L);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(anim, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() + 32);
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
      this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
