package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.packet.PacketHitObject;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CustomWildFlowerObject extends GameObject {
   protected String textureName;
   public GameTexture texture;
   public int textureSpriteX;
   public String seedStringID;
   public String productStringID;
   public int maxSeedAmount;
   public int maxProductAmount;
   public LootItemInterface customSeedLootItem;
   public LootItemInterface customProductLootItem;
   public String[] growTiles;
   protected final GameRandom drawRandom;

   public CustomWildFlowerObject(
      String textureName,
      int textureSpriteX,
      String seedStringID,
      int maxSeedAmount,
      String productStringID,
      int maxProductAmount,
      Color mapColor,
      String... growTiles
   ) {
      super(new Rectangle(0, 0));
      this.textureName = textureName;
      this.textureSpriteX = textureSpriteX;
      this.seedStringID = seedStringID;
      this.productStringID = productStringID;
      this.maxSeedAmount = maxSeedAmount;
      this.maxProductAmount = maxProductAmount;
      this.growTiles = growTiles;
      this.mapColor = mapColor;
      this.displayMapTooltip = true;
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.attackThrough = true;
      this.drawRandom = new GameRandom();
      this.roomProperties.add("flower");
      this.roomProperties.add("plant");
   }

   public CustomWildFlowerObject(
      String textureName, int textureSpriteX, String seedStringID, int maxSeedAmount, String productStringID, int maxProductAmount, Color debrisColor
   ) {
      this(textureName, textureSpriteX, seedStringID, maxSeedAmount, productStringID, maxProductAmount, debrisColor, "grasstile", "overgrowngrasstile");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      GameTexture spriteTexture = GameTexture.fromFile("objects/" + this.textureName);
      this.texture = new GameTexture("objects/" + this.textureName + " weave", 64, spriteTexture.getHeight());
      this.texture.copy(spriteTexture, 16, 0, this.textureSpriteX * 32, 0, 32, spriteTexture.getHeight());
      this.texture.resetTexture();
      spriteTexture.makeFinal();
      this.texture.makeFinal();
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      if (level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         return new LootTable(new LootItemMultiplierIgnored(new LootItem(this.productStringID)));
      }

      LootTable lootTable = new LootTable();
      if (this.customProductLootItem != null) {
         lootTable.items.add(this.customProductLootItem);
      } else if (this.maxProductAmount > 0) {
         lootTable.items.add(LootItem.between(this.productStringID, 1, this.maxProductAmount));
      }

      if (this.customSeedLootItem != null) {
         lootTable.items.add(this.customSeedLootItem);
      } else if (this.maxSeedAmount > 0) {
         lootTable.items.add(LootItem.between(this.seedStringID, 1, this.maxSeedAmount));
      }

      return lootTable;
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      if (level.objectLayer.isPlayerPlaced(x, y)) {
         level.getServer().network.sendToClientsWithTile(new PacketHitObject(level, x, y, this, damage), level, x, y);
      } else {
         super.attackThrough(level, x, y, damage, attacker);
      }
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage) {
      super.attackThrough(level, x, y, damage);
      if (!(damage.damage <= 0.0F) && !level.objectLayer.isPlayerPlaced(x, y)) {
         this.playDamageSound(level, x, y, true);
      } else {
         level.makeGrassWeave(x, y, 1000, false);
      }
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(GameResources.grass, SoundEffect.effect(x * 32 + 16, y * 32 + 16));
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      super.tick(mob, level, x, y);
      if (Settings.wavyGrass && mob.getFlyingHeight() < 10 && (mob.dx != 0.0F || mob.dy != 0.0F)) {
         level.makeGrassWeave(x, y, 1000, false);
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
      double nextGaus;
      double nextGaus2;
      boolean mirrored;
      synchronized (this.drawRandom) {
         nextGaus = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextGaussian();
         nextGaus2 = this.drawRandom.nextGaussian();
         mirrored = this.drawRandom.nextBoolean();
      }

      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, 1000L, 0.07F, 1, this.drawRandom, getTileSeed(tileX, tileY), mirrored, 2.0F);
      int offsetY = 16 + (int)(nextGaus * 4.0);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .light(light)
         .mirror(mirrored, false)
         .addPositionMod(waveChange)
         .pos(drawX - 16 + (int)(nextGaus2 * 4.0), drawY - this.texture.getHeight() + offsetY);
      final int sortY = offsetY - 6;
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
      double nextGaus;
      double nextGaus2;
      boolean mirrored;
      synchronized (this.drawRandom) {
         nextGaus = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextGaussian();
         nextGaus2 = this.drawRandom.nextGaussian();
         mirrored = this.drawRandom.nextBoolean();
      }

      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int offsetY = 16 + (int)(nextGaus * 4.0);
      this.texture
         .initDraw()
         .light(light)
         .mirror(mirrored, false)
         .alpha(alpha)
         .draw(drawX - 16 + (int)(nextGaus2 * 4.0), drawY - this.texture.getHeight() + offsetY);
   }

   @Override
   public boolean canPlaceOn(Level level, int layerID, int x, int y, GameObject other) {
      return other.getID() == 0 || !other.getValidObjectLayers().contains(ObjectLayerRegistry.TILE_LAYER);
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String superCanPlace = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (superCanPlace != null) {
         return superCanPlace;
      } else if (level.getObjectID(ObjectLayerRegistry.TILE_LAYER, x, y) != 0) {
         return "occupied";
      } else {
         GameTile tile = level.getTile(x, y);
         if (Arrays.stream(this.growTiles).anyMatch(sID -> tile.getID() == TileRegistry.getTileID(sID))) {
            return null;
         } else {
            return byPlayer && tile.isOrganic ? null : "invalidtile";
         }
      }
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      if (level.getObjectID(ObjectLayerRegistry.TILE_LAYER, x, y) != 0) {
         return false;
      }

      GameTile tile = level.getTile(x, y);
      boolean validGrowTile = Arrays.stream(this.growTiles).anyMatch(sID -> tile.getID() == TileRegistry.getTileID(sID));
      return validGrowTile || level.objectLayer.isPlayerPlaced(layerID, x, y) && tile.isOrganic;
   }
}
