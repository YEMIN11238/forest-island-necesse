package necesse.level.gameObject.container;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
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
import necesse.inventory.recipe.Tech;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TransmutationStationObject extends CraftingStationObject {
   public GameTexture texture;

   public TransmutationStationObject() {
      super(new Rectangle(32, 32));
      this.mapColor = new Color(15, 147, 176);
      this.toolType = ToolType.ALL;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.rarity = Item.Rarity.RARE;
   }

   @Override
   public int getCraftingCategoryDepth() {
      return 2;
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/transmutationstation");
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      return new Rectangle(x * 32, y * 32, 32, 32);
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      GameRandom random = GameRandom.globalRandom;
      if (random.getChance(0.5F)) {
         level.entityManager
            .addTopParticle(tileX * 32 + 16 + random.getIntBetween(-10, 10), tileY * 32 - 16 + random.getIntBetween(-10, 20), Particle.GType.COSMETIC)
            .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
            .color(new Color(0, 220, 216))
            .fadesAlpha(0.1F, 0.1F)
            .sizeFades(22, 22)
            .dontRotate()
            .lifeTime(500);
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
      int drawX = camera.getTileDrawX(tileX) - 8;
      int drawY = camera.getTileDrawY(tileY);
      int spriteX = GameUtils.getAnim(level.getWorldEntity().getTime() + Math.abs(getTileSeed(tileX, tileY, 52)), 2, 250);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(spriteX, 0, 50, this.texture.getHeight())
         .light(light)
         .addObjectDamageOverlay(this, level, tileX, tileY)
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
      int drawX = camera.getTileDrawX(tileX) - 8;
      int drawY = camera.getTileDrawY(tileY);
      int spriteX = GameUtils.getAnim(level.getWorldEntity().getTime() + Math.abs(getTileSeed(tileX, tileY, 52)), 2, 250);
      this.texture
         .initDraw()
         .sprite(spriteX, 0, 50, this.texture.getHeight())
         .alpha(alpha)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public Tech[] getCraftingTechs() {
      return new Tech[]{RecipeTechRegistry.TRANSMUTATION_STATION};
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "transmutationstationtip"), 350);
      return tooltips;
   }

   @Override
   protected SoundSettings getInteractSoundOpen() {
      return new SoundSettings(GameResources.alchemyTableOpen).volume(0.15F);
   }
}
