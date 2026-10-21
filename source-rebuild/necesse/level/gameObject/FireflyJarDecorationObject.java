package necesse.level.gameObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FireflyJarDecorationObject extends TableDecorationObject {
   protected GameRandom random = GameRandom.globalRandom;
   protected final FireflyMob.FireflyType fireflyType;

   protected FireflyJarDecorationObject(String textureName, FireflyMob.FireflyType type) {
      super(textureName, new Color(250, 190, 0), 16, 14);
      this.setItemCategory("objects", "lighting");
      this.setCraftingCategory("objects", "lighting");
      this.roomProperties.add("lights");
      this.fireflyType = type;
   }

   public FireflyJarDecorationObject(FireflyMob.FireflyType type) {
      this("fireflyjar", type);
   }

   public static int getJarEmissionLevel(FireflyMob.FireflyType type) {
      return type == FireflyMob.FireflyType.BLUE ? 60 : 50;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      if (level.isClient()) {
         float lightStrength = FireflyMob.getLightStrength(level, tileX, tileY);
         if (lightStrength > 0.0F) {
            float lightFactor = 0.85F + (float)GameMath.limit(Math.abs(Math.sin(this.getSeededTime(level, tileX, tileY) / 1000.0)), 0.0, 1.0) * 0.15F;
            level.lightManager
               .refreshParticleLight(
                  tileX, tileY, this.fireflyType.emissionHue, 1.0F, (int)(getJarEmissionLevel(this.fireflyType) * lightStrength * lightFactor)
               );
         }
      }
   }

   public static Color getSafeColor(int r, int g, int b) {
      return new Color(Math.min(r, 255), Math.min(g, 255), Math.min(b, 255), 255);
   }

   protected long getSeededTime(Level level, int tileX, int tileY) {
      return level.getTime() + (long)tileX * (tileY & 7) + (long)tileY * (tileX & 7);
   }

   protected int getSeededTimeSkew(int tileX, int tileY) {
      return 15 * (tileX % 3 * 2 + tileY % 3);
   }

   @Override
   public void addLayerDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addLayerDrawables(list, tileList, level, layerID, tileX, tileY, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      GameTexture texture = this.texture;
      int sortY = 16;
      if (layerID == ObjectLayerRegistry.FENCE_AND_TABLE_DECOR) {
         DecorationHolderInterface holder = this.getDecorationHolder(level, tileX, tileY);
         if (holder != null) {
            DecorDrawOffset drawOffset = holder.getDecorationDrawOffset(level, tileX, tileY, this);
            drawX += drawOffset.xOffset + this.holderDrawXOffset;
            drawY += drawOffset.yOffset + this.holderDrawYOffset;
            sortY = drawOffset.sortY;
            if (!drawOffset.useShadowTexture) {
               texture = this.texture_decor;
            }
         }
      }

      final TextureDrawOptions dayGlass = texture.initDraw().sprite(0, 0, 32, texture.getHeight()).light(light).pos(drawX, drawY - texture.getHeight() + 32);
      final float lightStrength = FireflyMob.getLightStrength(level, tileX, tileY);
      Color fireflyGlowColor = FireflyMob.getFireflyGlowColor(this.fireflyType, lightStrength);
      int lightifier = (int)(255.0F * (1.0F - lightStrength));
      Color glassColor = getSafeColor(fireflyGlowColor.getRed() + lightifier, fireflyGlowColor.getGreen() + lightifier, fireflyGlowColor.getBlue() + lightifier);
      final TextureDrawOptions dayGlassTransparency = texture.initDraw()
         .sprite(1, 0, 32, texture.getHeight())
         .light(light)
         .alpha(1.0F - lightStrength)
         .pos(drawX, drawY - texture.getHeight() + 32);
      final TextureDrawOptions nightGlass = texture.initDraw()
         .sprite(2, 0, 32, texture.getHeight())
         .color(glassColor)
         .light(new GameLight(120.0F))
         .alpha(lightStrength)
         .pos(drawX, drawY - texture.getHeight() + 32);
      int spriteXframe = GameUtils.getAnim(this.getSeededTime(level, tileX, tileY), 8, 800 + this.getSeededTimeSkew(tileX, tileY));
      final TextureDrawOptions fireflies = texture.initDraw()
         .sprite(spriteXframe + 3, 0, 32, texture.getHeight())
         .color(fireflyGlowColor)
         .mirror((tileX + tileY) % 2 == 0, false)
         .light(new GameLight(150.0F))
         .alpha(lightStrength)
         .pos(drawX, drawY - texture.getHeight() + 32);
      final int finalSortY = sortY;
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return finalSortY;
         }

         @Override
         public void draw(TickManager tickManager) {
            dayGlass.draw();
            if (lightStrength < 1.0F) {
               dayGlassTransparency.draw();
            }

            if (lightStrength > 0.0F) {
               nightGlass.draw();
            }

            if (lightStrength > 0.0F) {
               fireflies.draw();
            }
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      GameTexture texture = this.texture;
      DecorationHolderInterface holder = this.getDecorationHolder(level, tileX, tileY);
      if (holder != null) {
         DecorDrawOffset drawOffset = holder.getDecorationDrawOffset(level, tileX, tileY, this);
         drawX += drawOffset.xOffset + this.holderDrawXOffset;
         drawY += drawOffset.yOffset + this.holderDrawYOffset;
         if (!drawOffset.useShadowTexture) {
            texture = this.texture_decor;
         }
      }

      texture.initDraw().sprite(0, 0, 32, texture.getHeight()).alpha(alpha).draw(drawX, drawY - texture.getHeight() + 32);
      texture.initDraw().sprite(1, 0, 32, texture.getHeight()).alpha(alpha).draw(drawX, drawY - texture.getHeight() + 32);
   }
}
