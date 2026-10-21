package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public abstract class RandomBreakObject extends GameObject {
   protected final GameRandom drawRandom;
   public String texturePath;
   public GameTexture objectTexture;
   public GameTexture debrisTexture;
   public boolean spawnsDebris;
   public boolean countAsCratesBroken = true;
   public boolean lightUpAsTreasure = true;

   public RandomBreakObject(Rectangle collision, String texturePath, Color mapColor, boolean spawnsDebris) {
      super(collision);
      this.texturePath = texturePath;
      this.mapColor = mapColor;
      this.spawnsDebris = spawnsDebris;
      this.displayMapTooltip = true;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.attackThrough = true;
      this.drawRandom = new GameRandom();
      this.canPlaceOnProtectedLevels = true;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "landscaping", "misc");
   }

   public RandomBreakObject(Rectangle collision, String texturePath, Color debrisColor) {
      this(collision, texturePath, debrisColor, true);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      if (this.objectTexture == null) {
         this.objectTexture = GameTexture.fromFile("objects/" + this.texturePath);
      }

      if (this.spawnsDebris && this.debrisTexture == null) {
         this.debrisTexture = GameTexture.fromFile("objects/" + this.texturePath + "debris");
      }
   }

   @Override
   public LootTable getLootTable(final Level level, int layerID, final int tileX, final int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY) ? super.getLootTable(level, layerID, tileX, tileY) : new LootTable(new LootItemInterface() {
         @Override
         public void addPossibleLoot(LootList list, Object... extra) {
            RandomBreakObject.this.getBreakLootTable(level, tileX, tileY).addPossibleLoot(list, extra);
         }

         @Override
         public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
            GameRandom seedRandom = new GameRandom(GameObject.getTileSeed(tileX, tileY, (int)level.getSeed()));
            RandomBreakObject.this.getBreakLootTable(level, tileX, tileY).addItems(list, seedRandom, lootMultiplier, extra);
         }
      });
   }

   public abstract LootTable getBreakLootTable(Level var1, int var2, int var3);

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      if (!level.objectLayer.isPlayerPlaced(x, y)) {
         super.attackThrough(level, x, y, damage, attacker);
         if (this.countAsCratesBroken) {
            Mob attackOwner = attacker != null ? attacker.getAttackOwner() : null;
            if (attackOwner != null && attackOwner.isPlayer) {
               ((PlayerMob)attackOwner).getServerClient().newStats.crates_broken.increment(1);
            }
         }
      }
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage) {
      super.attackThrough(level, x, y, damage);
      if (damage.damage > 0.0F) {
         this.playDamageSound(level, x, y, true);
      }
   }

   @Override
   public void spawnDestroyedParticles(Level level, int tileX, int tileY) {
      super.spawnDestroyedParticles(level, tileX, tileY);
      GameRandom r = new GameRandom(GameRandom.globalRandom.nextInt(32767));
      GameTextureSection[] debrisSprites = this.getDebrisSprites(new GameRandom(), tileX, tileY);
      if (debrisSprites != null) {
         for (int i = 0; i < 4; i++) {
            level.entityManager
               .addParticle(
                  new FleshParticle(level, debrisSprites[r.nextInt(debrisSprites.length)], tileX * 32 + 16, tileY * 32 + 16, 10.0F, 0.0F, 0.0F),
                  Particle.GType.IMPORTANT_COSMETIC
               );
         }
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
      final TextureDrawOptionsEnd options = sprite.initDraw()
         .mirror(mirror, false)
         .startGlowOptions(level, this.getID())
         .light(light)
         .apply(treasureHunter)
         .pos(drawX, drawY);
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      GameTextureSection sprite;
      boolean mirror;
      synchronized (this.drawRandom) {
         sprite = this.getSprite(this.drawRandom, tileX, tileY);
         mirror = this.drawRandom.seeded(getTileSeed(tileX, tileY, 6199)).nextBoolean();
      }

      boolean treasureHunter = player != null && this.lightUpAsTreasure && player.buffManager.getModifier(BuffModifiers.TREASURE_HUNTER);
      drawY -= sprite.getHeight() - 32;
      sprite.initDraw().mirror(mirror, false).startGlowOptions(level, this.getID()).light(light).apply(treasureHunter).alpha(alpha).draw(drawX, drawY);
   }

   protected int getSprite(GameRandom random, int tileX, int tileY, int sprites) {
      return random.seeded(getTileSeed(tileX, tileY)).nextInt(sprites);
   }

   public GameTextureSection getSprite(GameRandom random, int tileX, int tileY) {
      int sprites = this.objectTexture.getWidth() / 32;
      int sprite = this.getSprite(random, tileX, tileY, sprites);
      return new GameTextureSection(this.objectTexture).sprite(sprite, 0, 32, this.objectTexture.getHeight());
   }

   public GameTextureSection[] getDebrisSprites(GameRandom random, int tileX, int tileY) {
      GameTextureSection sprite = this.getSprite(random, tileX, tileY);
      if (!this.spawnsDebris) {
         return null;
      }

      int height = this.debrisTexture.getHeight() / 32;
      GameTextureSection[] sprites = new GameTextureSection[height];

      for (int i = 0; i < height; i++) {
         int startY = i * 32;
         sprites[i] = new GameTextureSection(this.debrisTexture, sprite.getStartX(), sprite.getEndX(), startY, startY + 32);
      }

      return sprites;
   }

   public long getTreasureHash() {
      return this.getID();
   }
}
