package necesse.gfx.drawOptions.texture;

import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Consumer;
import necesse.engine.world.GameClock;
import necesse.entity.DamagedObjectEntity;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.FormShader;
import necesse.gfx.shader.GameShader;
import necesse.gfx.shader.ShaderState;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;

public class TextureDrawOptionsEnd extends TextureDrawOptions {
   TextureDrawOptionsEnd(TextureDrawOptions other) {
      super(other);
   }

   public TextureDrawOptionsEnd copy() {
      return new TextureDrawOptionsEnd(super.copy());
   }

   public TextureDrawOptionsEnd glRotate(float angle, int centerX, int centerY) {
      this.opts.rotation = angle;
      this.opts.rotTranslateX = centerX;
      this.opts.rotTranslateY = centerY;
      this.opts.useRotation = true;
      return this;
   }

   public TextureDrawOptionsEnd addShaderState(ShaderState state) {
      this.opts.shaderStates.add(state);
      return this;
   }

   public TextureDrawOptionsEnd addShaderBind(ShaderBind bind) {
      this.opts.shaderBinds.add(bind);
      return this;
   }

   public TextureDrawOptionsEnd addShaderBind(int pos, GameTexture texture) {
      return this.addShaderBind(new ShaderBind(pos, texture));
   }

   public TextureDrawOptionsEnd addShaderSprite(ShaderSpriteAbstract sprite) {
      this.opts.shaderSprites.add(sprite);
      return this;
   }

   public TextureDrawOptionsEnd addShaderSprite(int pos, float spriteX1, float spriteX2, float spriteY1, float spriteY2) {
      return this.addShaderSprite(new ShaderSprite(pos, spriteX1, spriteX2, spriteY1, spriteY2));
   }

   public TextureDrawOptionsEnd addShaderTexture(ShaderTexture texture) {
      this.opts.shaderBinds.add(texture.toBind());
      this.opts.shaderSprites.add(texture.toSprite());
      return this;
   }

   public TextureDrawOptionsEnd addShaderTextureFit(GameTexture texture, int pos) {
      if (texture == null) {
         return this;
      }

      float thisTextureWidth = this.opts.spriteWidth * this.texture.getWidth();
      float thisTextureHeight = this.opts.spriteHeight * this.texture.getHeight();
      float widthMod = thisTextureWidth / texture.getWidth();
      float heightMod = thisTextureHeight / texture.getHeight();
      return this.addShaderTexture(new ShaderTexture(pos, texture, 0.0F, widthMod, 0.0F, heightMod));
   }

   public TextureDrawOptionsEnd addShaderTextureFitCenterX(GameTexture texture, int pos) {
      if (texture == null) {
         return this;
      }

      float thisTextureWidth = this.opts.spriteWidth * this.texture.getWidth();
      float thisTextureHeight = this.opts.spriteHeight * this.texture.getHeight();
      float widthMod = thisTextureWidth / texture.getWidth();
      float heightMod = thisTextureHeight / texture.getHeight();
      float xOffset = (1.0F - widthMod) / 2.0F;
      return this.addShaderTexture(new ShaderTexture(pos, texture, xOffset, widthMod + xOffset, 0.0F, heightMod));
   }

   public TextureDrawOptionsEnd addShaderTexture(GameTexture texture, int pos) {
      return texture == null ? this : this.addShaderTexture(new ShaderTexture(pos, texture));
   }

   public TextureDrawOptionsEnd addShaderTextureFit(GameSprite sprite, int pos) {
      if (sprite == null) {
         return this;
      }

      float thisTextureWidth = this.opts.spriteWidth * this.texture.getWidth();
      float thisTextureHeight = this.opts.spriteHeight * this.texture.getHeight();
      float widthMod = thisTextureWidth / sprite.spriteWidth;
      float heightMod = thisTextureHeight / sprite.spriteHeight;
      ShaderTexture shaderTexture = new ShaderTexture(pos, sprite);
      shaderTexture.spriteX2 = shaderTexture.spriteX1 + (shaderTexture.spriteX2 - shaderTexture.spriteX1) * widthMod;
      shaderTexture.spriteY2 = shaderTexture.spriteY1 + (shaderTexture.spriteY2 - shaderTexture.spriteY1) * heightMod;
      return this.addShaderTexture(shaderTexture);
   }

   public TextureDrawOptionsEnd addShaderTexture(GameSprite sprite, int pos) {
      return sprite == null ? this : this.addShaderTexture(new ShaderTexture(pos, sprite));
   }

   public TextureDrawOptionsEnd addMaskShader(MaskShaderOptions maskOptions) {
      return maskOptions == null ? this : maskOptions.apply(this);
   }

   public TextureDrawOptionsEnd recolor(final GameTexture recolorLookupTexture, final int yIndex) {
      if (recolorLookupTexture != null) {
         this.addShaderBind(1, recolorLookupTexture);
         this.addShaderState(new ShaderState() {
            @Override
            public void use() {
               GameShader currentShader = Renderer.getCurrentShader();
               if (currentShader instanceof FormShader) {
                  ((FormShader)currentShader).passRecolor(recolorLookupTexture, yIndex);
               } else {
                  GameResources.sceneShader.use();
                  GameResources.sceneShader.passRecolor(recolorLookupTexture, yIndex);
               }
            }

            @Override
            public void stop() {
               GameShader currentShader = Renderer.getCurrentShader();
               if (currentShader instanceof FormShader) {
                  ((FormShader)currentShader).passRecolor(null, 0);
               } else {
                  GameResources.sceneShader.passRecolor(null, 0);
                  GameResources.sceneShader.stop();
               }
            }
         });
      }

      return this;
   }

   public TextureDrawOptionsEnd rotate(float angle, int centerX, int centerY) {
      super.rotate(angle, centerX, centerY);
      return this;
   }

   public TextureDrawOptionsEnd rotate(float angle) {
      super.rotate(angle);
      return this;
   }

   public TextureDrawOptionsEnd addRotation(float angle, int centerX, int centerY) {
      super.addRotation(angle, centerX, centerY);
      return this;
   }

   public TextureDrawOptionsEnd addPositionMod(Consumer<TextureDrawOptionsPositionMod> change) {
      super.addPositionMod(change);
      return this;
   }

   public TextureDrawOptionsEnd rotateTexture(int rightAngles, int midX, int midY) {
      super.rotateTexture(rightAngles, midX, midY);
      return this;
   }

   public TextureDrawOptionsEnd rotateTexture(int rightAngles) {
      super.rotateTexture(rightAngles);
      return this;
   }

   public TextureDrawOptionsEnd size(int width, int height) {
      super.size(width, height);
      return this;
   }

   public TextureDrawOptionsEnd size(Dimension dimension) {
      super.size(dimension);
      return this;
   }

   public TextureDrawOptionsEnd shrinkWidth(int size, boolean translate) {
      super.shrinkWidth(size, translate);
      return this;
   }

   public TextureDrawOptionsEnd shrinkHeight(int size, boolean translate) {
      super.shrinkHeight(size, translate);
      return this;
   }

   public TextureDrawOptionsEnd size(int size, boolean translate) {
      super.size(size, translate);
      return this;
   }

   public TextureDrawOptionsEnd size(int size) {
      super.size(size);
      return this;
   }

   public TextureDrawOptionsEnd color(float red, float green, float blue, float alpha) {
      super.color(red, green, blue, alpha);
      return this;
   }

   public TextureDrawOptionsEnd color(float grayScale) {
      super.color(grayScale);
      return this;
   }

   public TextureDrawOptionsEnd brightness(float brightness) {
      super.brightness(brightness);
      return this;
   }

   public TextureDrawOptionsEnd color(float red, float green, float blue) {
      super.color(red, green, blue);
      return this;
   }

   public TextureDrawOptionsEnd color(Color color, boolean overrideAlpha) {
      super.color(color, overrideAlpha);
      return this;
   }

   public TextureDrawOptionsEnd color(Color color) {
      super.color(color);
      return this;
   }

   public TextureDrawOptionsEnd alpha(float alpha) {
      super.alpha(alpha);
      return this;
   }

   public TextureDrawOptionsEnd alphaMult(float alpha) {
      super.alphaMult(alpha);
      return this;
   }

   public TextureDrawOptionsEnd light(GameLight light) {
      super.light(light);
      return this;
   }

   public TextureDrawOptionsEnd colorLight(float red, float green, float blue, GameLight light) {
      super.colorLight(red, green, blue, light);
      return this;
   }

   public TextureDrawOptionsEnd colorLight(float red, float green, float blue, float alpha, GameLight light) {
      super.colorLight(red, green, blue, alpha, light);
      return this;
   }

   public TextureDrawOptionsEnd colorLight(Color color, GameLight light) {
      super.colorLight(color, light);
      return this;
   }

   public TextureDrawOptionsEnd colorLight(Color color, boolean overrideAlpha, GameLight light) {
      super.colorLight(color, overrideAlpha, light);
      return this;
   }

   public TextureDrawOptionsEnd colorMult(Color color) {
      super.colorMult(color);
      return this;
   }

   @Override
   public TextureGlowLightDrawOptions<TextureDrawOptionsEnd> startGlowOptions(GameClock clock, long glowHash) {
      return new TextureGlowLightDrawOptions<>(this, clock, glowHash);
   }

   @Deprecated
   public TextureDrawOptionsEnd spelunkerColorLight(
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
   public TextureDrawOptionsEnd spelunkerColorLight(
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
   public TextureDrawOptionsEnd spelunkerColorLight(
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
   public TextureDrawOptionsEnd spelunkerColorLight(
      Color color, GameLight light, boolean hasSpelunker, long colorHash, GameClock gameClock, long fadeTime, float saturation, int minLight
   ) {
      super.spelunkerColorLight(color, light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
      return this;
   }

   @Deprecated
   public TextureDrawOptionsEnd spelunkerLight(
      GameLight light, boolean hasSpelunker, long colorHash, GameClock gameClock, long fadeTime, float saturation, int minLight
   ) {
      super.spelunkerLight(light, hasSpelunker, colorHash, gameClock, fadeTime, saturation, minLight);
      return this;
   }

   @Deprecated
   public TextureDrawOptionsEnd spelunkerLight(GameLight light, boolean hasSpelunker, long colorHash, GameClock gameClock) {
      super.spelunkerLight(light, hasSpelunker, colorHash, gameClock);
      return this;
   }

   public TextureDrawOptionsEnd addObjectDamageOverlay(GameObject object, Level level, int tileX, int tileY) {
      return this.addObjectDamageOverlay(object, level, 0, tileX, tileY);
   }

   public TextureDrawOptionsEnd addObjectDamageOverlay(GameObject object, Level level, int layerID, int tileX, int tileY) {
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

   public TextureDrawOptionsEnd addObjectDamageOverlay(float damagePercent) {
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

   public TextureDrawOptionsEnd addOverlay(final GameTexture texture, int startX, int startY, int endX, int endY) {
      float spriteXMod = (float)this.getWidth() / (endX - startX);
      float spriteYMod = (float)this.getHeight() / (endY - startY);
      final float startSpriteX = TextureDrawOptions.pixel(startX, texture.getWidth());
      final float startSpriteY = TextureDrawOptions.pixel(startY, texture.getHeight());
      final float endSpriteX = TextureDrawOptions.pixel(endX, texture.getWidth());
      final float endSpriteY = TextureDrawOptions.pixel(endY, texture.getHeight());
      float spriteDeltaX = endSpriteX - startSpriteX;
      float spriteDeltaY = endSpriteY - startSpriteY;
      this.addShaderSprite(2, startSpriteX, startSpriteX + spriteDeltaX * spriteXMod, startSpriteY, startSpriteY + spriteDeltaY * spriteYMod);
      this.addShaderState(new ShaderState() {
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

   public TextureDrawOptionsEnd advColor(float[] colors) {
      super.advColor(colors);
      return this;
   }

   public TextureDrawOptionsEnd translatePos(int x, int y) {
      super.translatePos(x, y);
      return this;
   }

   public TextureDrawOptionsEnd addTranslatePos(int x, int y) {
      super.addTranslatePos(x, y);
      return this;
   }

   public TextureDrawOptionsEnd mirrorX() {
      super.mirrorX();
      return this;
   }

   public TextureDrawOptionsEnd mirrorY() {
      super.mirrorY();
      return this;
   }

   public TextureDrawOptionsEnd mirror(boolean horizontal, boolean vertical) {
      super.mirror(horizontal, vertical);
      return this;
   }

   public TextureDrawOptionsEnd depth(float depth) {
      super.depth(depth);
      return this;
   }

   public TextureDrawOptionsEnd blendFunc(int blendSourceRGB, int blendDestinationRGB, int blendSourceAlpha, int blendDestinationAlpha) {
      super.blendFunc(blendSourceRGB, blendDestinationRGB, blendSourceAlpha, blendDestinationAlpha);
      return this;
   }

   public TextureDrawOptionsEnd pos(int drawX, int drawY) {
      super.pos(drawX, drawY);
      return this;
   }

   public TextureDrawOptionsEnd pos(int drawX, int drawY, boolean useTranslate) {
      super.pos(drawX, drawY, useTranslate);
      return this;
   }

   public TextureDrawOptionsEnd posMiddle(int drawX, int drawY, boolean useTranslate) {
      super.posMiddle(drawX, drawY, useTranslate);
      return this;
   }

   public TextureDrawOptionsEnd posMiddle(int drawX, int drawY) {
      super.posMiddle(drawX, drawY);
      return this;
   }

   public void draw(int drawX, int drawY) {
      this.pos(drawX, drawY);
      this.draw();
   }
}
