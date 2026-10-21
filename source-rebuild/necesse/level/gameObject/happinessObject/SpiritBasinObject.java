package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.furniture.FurnitureObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SpiritBasinObject extends FurnitureObject implements HappinessObject {
   protected String textureName;
   public GameTexture texture;
   public GameTexture lightTexture;

   public SpiritBasinObject(String textureName, String... category) {
      super(new Rectangle(32, 32));
      this.textureName = textureName;
      this.toolType = ToolType.ALL;
      this.mapColor = new Color(31, 122, 101);
      this.rarity = Item.Rarity.LEGENDARY;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.furnitureType = null;
      this.lightLevel = 50;
      this.lightHue = 167.0F;
      this.lightSat = 0.8F;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.setItemCategory(category);
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      GameRandom random = GameRandom.globalRandom;
      AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
      float distance = 32.0F;
      int objectPosX = tileX * 32 + 16;
      int objectPosY = tileY * 32 + 16;
      level.entityManager
         .addParticle(
            objectPosX + GameMath.sin(currentAngle.get()) * distance + random.getIntBetween(-5, 5),
            objectPosY + 16 + GameMath.cos(currentAngle.get()) * distance + random.getIntBetween(-5, 5) * 0.85F,
            Particle.GType.COSMETIC
         )
         .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
         .heightMoves(32.0F, 48.0F)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
            float distY = distance * lifePercent * 0.85F;
            pos.x = objectPosX + GameMath.sin(angle) * (distance * lifePercent);
            pos.y = objectPosY + 16 + GameMath.cos(angle) * distY * 0.85F;
         })
         .color(new Color(69, 229, 193))
         .ignoreLight(true)
         .sizeFades(12, 24)
         .fadesAlphaTimeToCustomAlpha(200, 200, 0.25F)
         .lifeTime(1000);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
      this.lightTexture = GameTexture.fromFile("objects/" + this.textureName + "_light");
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
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(0, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() + 32);
      final TextureDrawOptions lightOptions = this.lightTexture
         .initDraw()
         .sprite(0, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light.minLevelCopy(150.0F))
         .pos(drawX, drawY - this.texture.getHeight() + 32);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
            lightOptions.draw();
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
