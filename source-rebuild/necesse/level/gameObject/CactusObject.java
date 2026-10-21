package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CactusObject extends GameObject implements ForestryJobObject {
   public int weaveTime = 250;
   public float weaveAmount = 0.02F;
   protected GameTexture texture;
   protected final GameRandom drawRandom;
   protected String saplingStringID;

   public CactusObject(String saplingStringID) {
      super(new Rectangle(8, 16, 16, 8));
      this.saplingStringID = saplingStringID;
      this.mapColor = new Color(47, 79, 8);
      this.displayMapTooltip = true;
      this.toolType = ToolType.AXE;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.hoverHitbox = new Rectangle(0, -24, 32, 56);
      this.setItemCategory("objects", "landscaping", "plants");
      this.roomProperties.add("plant");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/cactus");
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable(LootItem.between("cactussapling", 3, 5));
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
      int spriteRes = 64;
      int spriteX = 0;
      if (this.texture.getWidth() > spriteRes && level.getTileID(tileX, tileY) == TileRegistry.snowID) {
         spriteX = 1;
      }

      int spritesHeight = this.texture.getHeight() / spriteRes;
      int spriteY = 0;
      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         if (spritesHeight > 1) {
            spriteY = this.drawRandom.nextInt(spritesHeight);
         }

         mirror = this.drawRandom.nextBoolean();
      }

      Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, this.weaveTime, this.weaveAmount, 2, this.drawRandom, getTileSeed(tileX, tileY, 0), mirror, 3.0F);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(spriteX, spriteY, spriteRes)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .mirror(mirror, false)
         .addPositionMod(waveChange)
         .pos(drawX - 16, drawY - 32);
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
      int spriteRes = 64;
      int spriteX = 0;
      if (this.texture.getWidth() > spriteRes && level.getTileID(tileX, tileY) == TileRegistry.snowID) {
         spriteX = 1;
      }

      int spritesHeight = this.texture.getHeight() / spriteRes;
      int spriteY = 0;
      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         if (spritesHeight > 1) {
            spriteY = this.drawRandom.nextInt(spritesHeight);
         }

         mirror = this.drawRandom.nextBoolean();
      }

      this.texture.initDraw().sprite(spriteX, spriteY, spriteRes).light(light).alpha(alpha).mirror(mirror, false).draw(drawX - 16, drawY - 32);
   }

   @Override
   public boolean onDamaged(
      Level level, int layerID, int x, int y, int damage, Attacker attacker, ServerClient client, boolean showEffect, int mouseX, int mouseY
   ) {
      boolean out = super.onDamaged(level, layerID, x, y, damage, attacker, client, showEffect, mouseX, mouseY);
      if (showEffect) {
         level.makeGrassWeave(x, y, this.weaveTime, true);
      }

      return out;
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return super.isValid(level, layerID, x, y) && level.getTileID(x, y) == TileRegistry.getTileID("sandtile");
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String error = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (error != null) {
         return error;
      } else {
         return !level.getTile(x, y).getStringID().equals("sandtile") ? "notsand" : null;
      }
   }

   @Override
   public String getSaplingStringID() {
      return this.saplingStringID;
   }
}
