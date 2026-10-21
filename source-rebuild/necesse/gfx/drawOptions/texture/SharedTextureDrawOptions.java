package necesse.gfx.drawOptions.texture;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import necesse.engine.world.GameClock;
import necesse.entity.DamagedObjectEntity;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.shader.ShaderState;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;

public class SharedTextureDrawOptions {
   public static int MAX_VERTEX_CALLS_PER_DRAW_CALL = Integer.MAX_VALUE;
   public final GameTexture texture;
   private final ArrayList<TextureDrawOptionsObj> options = new ArrayList<>();
   private final ArrayList<ShaderBind> shaderBinds = new ArrayList<>();
   private final ArrayList<ShaderState> shaderStates = new ArrayList<>();
   private final ArrayList<Consumer<SharedTextureDrawOptions.Wrapper>> optionsModifiers = new ArrayList<>();

   public SharedTextureDrawOptions(GameTexture texture) {
      Objects.requireNonNull(texture);
      this.texture = texture;
   }

   public SharedTextureDrawOptions addShaderBind(int pos, GameTexture texture) {
      this.shaderBinds.add(new ShaderBind(pos, texture));
      return this;
   }

   public SharedTextureDrawOptions addObjectDamageOverlay(GameObject object, Level level, int tileX, int tileY) {
      return this.addObjectDamageOverlay(object, level, 0, tileX, tileY);
   }

   public SharedTextureDrawOptions addObjectDamageOverlay(GameObject object, Level level, int layerID, int tileX, int tileY) {
      MultiTile multiTile = object.getMultiTile(level, layerID, tileX, tileY);
      LevelObject master = multiTile.getMasterLevelObject(level, layerID, tileX, tileY).orElse(null);
      DamagedObjectEntity damagedObjectEntity;
      if (master != null) {
         damagedObjectEntity = level.entityManager.getDamagedObjectEntity(master.tileX, master.tileY);
      } else {
         damagedObjectEntity = level.entityManager.getDamagedObjectEntity(tileX, tileY);
      }

      int damage = level.getAppearedObjectDamage(object, layerID, tileX, tileY);
      if (damagedObjectEntity != null) {
         damage = Math.max(damagedObjectEntity.getObjectDamage(layerID), damage);
      }

      return damage > 0 ? this.addObjectDamageOverlay((float)damage / object.objectHealth) : this;
   }

   public SharedTextureDrawOptions addObjectDamageOverlay(float damagePercent) {
      if (damagePercent > 0.0F) {
         int spriteSize = 32;
         int spriteLength = GameResources.objectDamageOverlay.getWidth() / spriteSize + 1 - 1;
         int sprite = Math.min((int)(damagePercent * spriteLength), spriteLength);
         if (sprite == 0) {
            return this;
         }

         this.addOverlay(GameResources.objectDamageOverlay, --sprite * spriteSize, 0, (sprite + 1) * spriteSize, spriteSize);
      }

      return this;
   }

   public SharedTextureDrawOptions addOverlay(final GameTexture texture, int startX, int startY, int endX, int endY) {
      final float startSpriteX = TextureDrawOptions.pixel(startX, texture.getWidth());
      final float startSpriteY = TextureDrawOptions.pixel(startY, texture.getHeight());
      final float endSpriteX = TextureDrawOptions.pixel(endX, texture.getWidth());
      final float endSpriteY = TextureDrawOptions.pixel(endY, texture.getHeight());
      float spriteDeltaX = endSpriteX - startSpriteX;
      float spriteDeltaY = endSpriteY - startSpriteY;
      this.optionsModifiers.add(wrapper -> {
         float spriteXMod = (float)wrapper.getWidth() / (endX - startX);
         float spriteYMod = (float)wrapper.getHeight() / (endY - startY);
         wrapper.addShaderSprite(2, startSpriteX, startSpriteX + spriteDeltaX * spriteXMod, startSpriteY, startSpriteY + spriteDeltaY * spriteYMod);
      });
      this.addShaderBind(2, texture);
      this.shaderStates.add(new ShaderState() {
         @Override
         public void use() {
            GameResources.sceneShader.use();
            GameResources.sceneShader.passOverlay(texture, startSpriteX, startSpriteY, endSpriteX, endSpriteY);
         }

         @Override
         public void stop() {
            GameResources.sceneShader.stopOverlay();
            GameResources.sceneShader.stop();
         }
      });
      return this;
   }

   public SharedTextureDrawOptions.Wrapper add(GameTextureSection section) {
      if (section.getTexture() != this.texture) {
         throw new IllegalStateException("Invalid texture");
      }

      TextureDrawOptionsObj opts = section.initDraw().opts;
      SharedTextureDrawOptions.Wrapper wrapper = new SharedTextureDrawOptions.Wrapper(opts);
      synchronized (this) {
         this.optionsModifiers.forEach(c -> c.accept(wrapper));
         this.options.add(opts);
         return wrapper;
      }
   }

   public SharedTextureDrawOptions.Wrapper addFull() {
      return this.add(new GameTextureSection(this.texture));
   }

   public SharedTextureDrawOptions.Wrapper addSprite(int spriteX, int spriteY, int spriteWidth, int spriteHeight) {
      return this.add(new GameTextureSection(this.texture).sprite(spriteX, spriteY, spriteWidth, spriteHeight));
   }

   public SharedTextureDrawOptions.Wrapper addSprite(int spriteX, int spriteY, int spriteRes) {
      return this.add(new GameTextureSection(this.texture).sprite(spriteX, spriteY, spriteRes));
   }

   public SharedTextureDrawOptions.Wrapper addSection(int startX, int endX, int startY, int endY) {
      return this.add(new GameTextureSection(this.texture, startX, endX, startY, endY));
   }

   public SharedTextureDrawOptions.Wrapper addSpriteSection(
      int spriteX, int spriteY, int spriteWidth, int spriteHeight, int startX, int endX, int startY, int endY
   ) {
      return this.add(new GameTextureSection(this.texture).sprite(spriteX, spriteY, spriteWidth, spriteHeight).section(startX, endX, startY, endY));
   }

   public SharedTextureDrawOptions.Wrapper addSpriteSection(int spriteX, int spriteY, int spriteRes, int startX, int endX, int startY, int endY) {
      return this.add(new GameTextureSection(this.texture).sprite(spriteX, spriteY, spriteRes).section(startX, endX, startY, endY));
   }

   public SharedTextureDrawOptions forEachDraw(Consumer<SharedTextureDrawOptions.Wrapper> consumer) {
      synchronized (this) {
         for (TextureDrawOptionsObj option : this.options) {
            consumer.accept(new SharedTextureDrawOptions.Wrapper(option));
         }

         return this;
      }
   }

   public void draw() {
      this.draw(MAX_VERTEX_CALLS_PER_DRAW_CALL);
   }

   public void draw(int maxDrawsPerCall) {
      synchronized (this) {
         try {
            Iterator<TextureDrawOptionsObj> iterator = this.options.iterator();
            this.shaderStates.forEach(ShaderState::use);
            TextureDrawOptionsObj last = null;
            int drawCounter = 0;
            boolean first = true;

            while (iterator.hasNext()) {
               TextureDrawOptionsObj next = iterator.next();
               if (drawCounter >= maxDrawsPerCall && last != null) {
                  last.glEnd();
                  first = true;
                  drawCounter = 0;
               }

               if (next != null) {
                  if (first) {
                     this.shaderBinds.forEach(ShaderBind::bind);
                  }

                  next.draw(first, !iterator.hasNext());
                  last = next;
                  first = false;
               } else {
                  if (last != null) {
                     last.glEnd();
                  }

                  first = true;
               }

               drawCounter++;
            }
         } finally {
            this.shaderStates.forEach(ShaderState::stop);
         }
      }
   }

   public static class Wrapper extends TextureDrawOptionsMods {
      public Wrapper(TextureDrawOptionsObj opts) {
         super(opts);
      }

      public SharedTextureDrawOptions.Wrapper addShaderSprite(ShaderSpriteAbstract sprite) {
         this.opts.shaderSprites.add(sprite);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper addShaderSprite(int pos, float spriteX1, float spriteX2, float spriteY1, float spriteY2) {
         return this.addShaderSprite(new ShaderSprite(pos, spriteX1, spriteX2, spriteY1, spriteY2));
      }

      public SharedTextureDrawOptions.Wrapper rotate(float angle, int centerX, int centerY) {
         super.rotate(angle, centerX, centerY);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper rotate(float angle) {
         super.rotate(angle);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper addRotation(float angle, int centerX, int centerY) {
         super.addRotation(angle, centerX, centerY);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper addPositionMod(Consumer<TextureDrawOptionsPositionMod> change) {
         super.addPositionMod(change);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper rotateTexture(int rightAngles, int midX, int midY) {
         super.rotateTexture(rightAngles, midX, midY);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper rotateTexture(int rightAngles) {
         super.rotateTexture(rightAngles);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper size(int width, int height) {
         super.size(width, height);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper size(Dimension dimension) {
         super.size(dimension);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper shrinkWidth(int size, boolean translate) {
         super.shrinkWidth(size, translate);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper shrinkHeight(int size, boolean translate) {
         super.shrinkHeight(size, translate);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper size(int size, boolean translate) {
         super.size(size, translate);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper size(int size) {
         super.size(size);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper color(float red, float green, float blue, float alpha) {
         super.color(red, green, blue, alpha);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper color(float grayScale) {
         super.color(grayScale);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper brightness(float brightness) {
         super.brightness(brightness);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper color(float red, float green, float blue) {
         super.color(red, green, blue);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper color(Color color, boolean overrideAlpha) {
         super.color(color, overrideAlpha);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper color(Color color) {
         super.color(color);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper alpha(float alpha) {
         super.alpha(alpha);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper alphaMult(float alpha) {
         super.alphaMult(alpha);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper light(GameLight light) {
         super.light(light);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper colorLight(float red, float green, float blue, GameLight light) {
         super.colorLight(red, green, blue, light);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper colorLight(float red, float green, float blue, float alpha, GameLight light) {
         super.colorLight(red, green, blue, alpha, light);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper colorLight(Color color, GameLight light) {
         super.colorLight(color, light);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper colorLight(Color color, boolean overrideAlpha, GameLight light) {
         super.colorLight(color, overrideAlpha, light);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper colorMult(Color color) {
         super.colorMult(color);
         return this;
      }

      @Override
      public TextureGlowLightDrawOptions<SharedTextureDrawOptions.Wrapper> startGlowOptions(GameClock clock, long glowHash) {
         return new TextureGlowLightDrawOptions<>(this, clock, glowHash);
      }

      @Deprecated
      public SharedTextureDrawOptions.Wrapper spelunkerColorLight(
         float red,
         float green,
         float blue,
         float alpha,
         GameLight light,
         boolean hasSpelunker,
         long colorHash,
         GameClock gameClock,
         long fadeTime,
         float saturation,
         int minLight
      ) {
         super.spelunkerColorLight(red, green, blue, alpha, light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
         return this;
      }

      @Deprecated
      public SharedTextureDrawOptions.Wrapper spelunkerColorLight(
         float red,
         float green,
         float blue,
         GameLight light,
         boolean hasSpelunker,
         long colorHash,
         GameClock gameClock,
         long fadeTime,
         float saturation,
         int minLight
      ) {
         super.spelunkerColorLight(red, green, blue, light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
         return this;
      }

      @Deprecated
      public SharedTextureDrawOptions.Wrapper spelunkerColorLight(
         Color color,
         boolean overrideAlpha,
         GameLight light,
         boolean hasSpelunker,
         long colorHash,
         GameClock gameClock,
         long fadeTime,
         float saturation,
         int minLight
      ) {
         super.spelunkerColorLight(color, overrideAlpha, light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
         return this;
      }

      @Deprecated
      public SharedTextureDrawOptions.Wrapper spelunkerColorLight(
         Color color, GameLight light, boolean hasSpelunker, long colorHash, GameClock gameClock, long fadeTime, float saturation, int minLight
      ) {
         super.spelunkerColorLight(color, light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
         return this;
      }

      @Deprecated
      public SharedTextureDrawOptions.Wrapper spelunkerLight(
         GameLight light, boolean hasSpelunker, long colorHash, GameClock gameClock, long fadeTime, float saturation, int minLight
      ) {
         super.spelunkerLight(light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
         return this;
      }

      @Deprecated
      public SharedTextureDrawOptions.Wrapper spelunkerLight(GameLight light, boolean hasSpelunker, long colorHash, GameClock gameClock) {
         super.spelunkerLight(light, hasSpelunker, colorHash, gameClock);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper advColor(float[] colors) {
         super.advColor(colors);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper translatePos(int x, int y) {
         super.translatePos(x, y);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper addTranslatePos(int x, int y) {
         super.addTranslatePos(x, y);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper mirrorX() {
         super.mirrorX();
         return this;
      }

      public SharedTextureDrawOptions.Wrapper mirrorY() {
         super.mirrorY();
         return this;
      }

      public SharedTextureDrawOptions.Wrapper mirror(boolean horizontal, boolean vertical) {
         super.mirror(horizontal, vertical);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper depth(float depth) {
         super.depth(depth);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper blendFunc(int blendSourceRGB, int blendDestinationRGB, int blendSourceAlpha, int blendDestinationAlpha) {
         super.blendFunc(blendSourceRGB, blendDestinationRGB, blendSourceAlpha, blendDestinationAlpha);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper pos(int drawX, int drawY) {
         super.pos(drawX, drawY);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper pos(int drawX, int drawY, boolean useTranslate) {
         super.pos(drawX, drawY, useTranslate);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper posMiddle(int drawX, int drawY, boolean useTranslate) {
         super.posMiddle(drawX, drawY, useTranslate);
         return this;
      }

      public SharedTextureDrawOptions.Wrapper posMiddle(int drawX, int drawY) {
         super.posMiddle(drawX, drawY);
         return this;
      }

      @Override
      public int getWidth() {
         return super.getWidth();
      }

      @Override
      public int getHeight() {
         return super.getHeight();
      }
   }
}
