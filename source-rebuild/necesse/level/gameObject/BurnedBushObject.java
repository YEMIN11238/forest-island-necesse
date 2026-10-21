package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.packet.PacketHitObject;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.window.WindowManager;
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
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BurnedBushObject extends GameObject {
   public int weaveTime = 250;
   public float weaveAmount = 0.08F;
   protected String textureName;
   protected GameTexture texture;
   protected final GameRandom drawRandom;

   public BurnedBushObject(String textureName, Color mapColor) {
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.displayMapTooltip = true;
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.drawRandom = new GameRandom();
      this.attackThrough = true;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.replaceCategories.add("bush");
      this.replaceRotations = false;
      this.setItemCategory("objects", "landscaping", "plants");
   }

   public BurnedBushObject(String textureName) {
      this(textureName, new Color(33, 33, 33));
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      super.tick(mob, level, x, y);
      if (Settings.wavyGrass && mob.getFlyingHeight() < 10 && (mob.dx != 0.0F || mob.dy != 0.0F)) {
         level.makeGrassWeave(x, y, this.weaveTime, false);
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return new LootTable(new LootItem("charredbark"));
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
      float alpha = 1.0F;
      if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
         Rectangle alphaRec = new Rectangle(tileX * 32 - 12, tileY * 32 - 24, 56, 32);
         if (perspective.getCollision().intersects(alphaRec)) {
            alpha = 0.5F;
         } else if (alphaRec.contains(camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY)
            )
          {
            alpha = 0.5F;
         }
      }

      double yGaussian;
      boolean mirror;
      int spriteX;
      synchronized (this.drawRandom) {
         spriteX = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(2);
         yGaussian = this.drawRandom.seeded(getTileSeed(tileX, tileY, 0)).nextFloat() * 2.0F - 1.0F;
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         mirror = this.drawRandom.nextBoolean();
      }

      Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, 400L, this.weaveAmount, 2, this.drawRandom, getTileSeed(tileX, tileY, 0), mirror, 2.0F);
      int offset = 28 + (int)(yGaussian * 4.0);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(spriteX, 0, 64)
         .alpha(alpha)
         .light(light)
         .mirror(mirror, false)
         .addPositionMod(waveChange)
         .pos(drawX - 32 + 16, drawY - this.texture.getHeight() + offset);
      final int sortY = offset - 16;
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int spriteX;
      double yGaussian;
      boolean mirror;
      synchronized (this.drawRandom) {
         spriteX = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(2);
         yGaussian = this.drawRandom.seeded(getTileSeed(tileX, tileY, 0)).nextFloat() * 2.0F - 1.0F;
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         mirror = this.drawRandom.nextBoolean();
      }

      int offset = 28 + (int)(yGaussian * 4.0);
      this.texture
         .initDraw()
         .sprite(spriteX, 0, 64)
         .light(light)
         .mirror(mirror, false)
         .alpha(alpha)
         .draw(drawX - 32 + 16, drawY - this.texture.getHeight() + offset);
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      level.getServer().network.sendToClientsWithTile(new PacketHitObject(level, x, y, this, damage), level, x, y);
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage) {
      super.attackThrough(level, x, y, damage);
      level.makeGrassWeave(x, y, 800, false);
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(SoundSettingsRegistry.leavesBreakAction, SoundEffect.effect(x * 32 + 16, y * 32 + 16));
   }
}
