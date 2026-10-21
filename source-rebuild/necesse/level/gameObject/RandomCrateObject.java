package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.seasons.GameSeasons;
import necesse.engine.seasons.SeasonCrate;
import necesse.engine.sound.SimpleSoundCooldown;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.light.GameLight;

public class RandomCrateObject extends RandomBreakObject {
   public RandomCrateObject(String texturePath) {
      super(new Rectangle(5, 12, 22, 12), texturePath, new Color(112, 89, 52));
      this.setItemCategory("objects", "misc");
      this.canPlaceOnLiquid = true;
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String superCanPlace = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (superCanPlace != null) {
         return superCanPlace;
      } else {
         return level.getTileID(x, y) == TileRegistry.lavaID ? "onlava" : null;
      }
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return !super.isValid(level, layerID, x, y) ? false : level.getTileID(x, y) != TileRegistry.lavaID;
   }

   @Override
   public LootTable getBreakLootTable(Level level, int tileX, int tileY) {
      LootTable out = level.getCrateLootTable(tileX, tileY);
      if (this.useEventTexture(new GameRandom(), tileX, tileY)) {
         out = new LootTable(out, out);
      }

      return out;
   }

   protected SeasonCrate getSeasonCrate(GameRandom random, int tileX, int tileY) {
      return GameSeasons.getCrate(random.seeded(getTileSeed(tileX, tileY, 38813)));
   }

   public boolean useEventTexture(GameRandom random, int tileX, int tileY) {
      return this.getSeasonCrate(random, tileX, tileY) != null;
   }

   @Override
   public GameTextureSection getSprite(GameRandom random, int tileX, int tileY) {
      SeasonCrate seasonCrate = this.getSeasonCrate(random, tileX, tileY);
      GameTexture texture = seasonCrate != null ? seasonCrate.getTexture() : this.objectTexture;
      int sprites = texture.getWidth() / 32;
      int sprite = this.getSprite(random, tileX, tileY, sprites);
      return new GameTextureSection(texture).sprite(sprite, 0, 32, texture.getHeight());
   }

   @Override
   public GameTextureSection[] getDebrisSprites(GameRandom random, int tileX, int tileY) {
      SeasonCrate seasonCrate = this.getSeasonCrate(random, tileX, tileY);
      GameTexture texture = seasonCrate != null ? seasonCrate.getDebrisTexture() : this.debrisTexture;
      int count = texture.getWidth() / 32;
      GameTextureSection[] sprites = new GameTextureSection[count];

      for (int i = 0; i < count; i++) {
         int startX = i * 32;
         sprites[i] = new GameTextureSection(texture, startX, startX + 32, 0, 32);
      }

      return sprites;
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
      boolean onWater = level.isLiquidTile(tileX, tileY);
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      GameTextureSection sprite;
      boolean mirror;
      synchronized (this.drawRandom) {
         sprite = this.getSprite(this.drawRandom, tileX, tileY);
         mirror = this.drawRandom.seeded(getTileSeed(tileX, tileY, 6199)).nextBoolean();
      }

      boolean treasureHunter = perspective != null && this.lightUpAsTreasure && perspective.buffManager.getModifier(BuffModifiers.TREASURE_HUNTER);
      drawY -= sprite.getHeight() - 32;
      LevelTile levelTile = level.getLevelTile(tileX, tileY);
      float bobbingStrength = GameMath.max(0.8F, level.liquidManager.getDepthPercent(tileX, tileY));
      float liquidBobbing = levelTile.getLiquidBobbing() * bobbingStrength;
      drawY += (int)liquidBobbing;
      final TextureDrawOptionsEnd options = sprite.initDraw()
         .mirror(mirror, false)
         .startGlowOptions(level, this.getID())
         .light(light)
         .apply(treasureHunter)
         .pos(drawX, drawY);
      if (onWater) {
         float wavesStrength = liquidBobbing * 1.5F;
         MaskShaderOptions swimMask = new MaskShaderOptions(MobRegistry.Textures.swimmask, 0, 0, 0, (int)wavesStrength);
         options.addMaskShader(swimMask);
      }

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
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      boolean onWater = level.isLiquidTile(x, y);
      if (onWater) {
         SoundManager.playSound(
            new SoundSettings(GameResources.splash).volume(0.3F).cooldown(new SimpleSoundCooldown(200)), SoundEffect.effect(x * 32 + 16, y * 32 + 16)
         );
      }

      GameSound crateBreakSound = GameRandom.globalRandom.getOneOf(GameResources.cratebreak1, GameResources.cratebreak2, GameResources.cratebreak3);
      SoundManager.playSound(
         new SoundSettings(crateBreakSound).volume(0.8F).cooldown(new SimpleSoundCooldown(200)), SoundEffect.effect(x * 32 + 16, y * 32 + 16)
      );
   }
}
