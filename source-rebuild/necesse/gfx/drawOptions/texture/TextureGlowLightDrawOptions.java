package necesse.gfx.drawOptions.texture;

import java.awt.Color;
import necesse.engine.world.GameClock;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.level.maps.light.GameLight;

public class TextureGlowLightDrawOptions<T extends TextureDrawOptionsMods> {
   protected final T options;
   protected final GameClock gameClock;
   protected final long glowHash;
   protected GameLight light;
   protected int minLight = 100;
   protected float colorRed;
   protected float colorGreen;
   protected float colorBlue;
   protected float colorAlpha;
   protected long fadeTime = 2500L;
   protected float saturation = 0.2F;
   protected float brightness = 1.0F;

   public TextureGlowLightDrawOptions(T options, GameClock gameClock, long glowHash) {
      this.options = options;
      this.gameClock = gameClock;
      this.glowHash = glowHash;
      this.colorRed = options.opts.red;
      this.colorGreen = options.opts.green;
      this.colorBlue = options.opts.blue;
      this.colorAlpha = options.opts.alpha;
   }

   public TextureGlowLightDrawOptions<T> light(GameLight light) {
      this.light = light;
      return this;
   }

   public TextureGlowLightDrawOptions<T> minLight(int minLight) {
      this.minLight = minLight;
      return this;
   }

   public TextureGlowLightDrawOptions<T> colorLight(Color color, GameLight light) {
      this.light = light;
      return this.color(color);
   }

   public TextureGlowLightDrawOptions<T> colorMult(Color color) {
      this.colorRed = this.colorRed * (color.getRed() / 255.0F);
      this.colorGreen = this.colorGreen * (color.getGreen() / 255.0F);
      this.colorBlue = this.colorBlue * (color.getBlue() / 255.0F);
      this.colorAlpha = this.colorAlpha * (color.getAlpha() / 255.0F);
      return this;
   }

   public TextureGlowLightDrawOptions<T> color(float red, float green, float blue, float alpha) {
      this.colorRed = red;
      this.colorGreen = green;
      this.colorBlue = blue;
      this.colorAlpha = alpha;
      return this;
   }

   public TextureGlowLightDrawOptions<T> color(float red, float green, float blue) {
      this.colorRed = red;
      this.colorGreen = green;
      this.colorBlue = blue;
      return this;
   }

   public TextureGlowLightDrawOptions<T> color(Color color, boolean overrideAlpha) {
      return this.color(
         color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, overrideAlpha ? color.getAlpha() / 255.0F : this.colorAlpha
      );
   }

   public TextureGlowLightDrawOptions<T> color(Color color) {
      return this.color(color, true);
   }

   public TextureGlowLightDrawOptions<T> fadeTime(long fadeTime) {
      this.fadeTime = fadeTime;
      return this;
   }

   public TextureGlowLightDrawOptions<T> saturation(float saturation) {
      this.saturation = saturation;
      return this;
   }

   public TextureGlowLightDrawOptions<T> brightness(float brightness) {
      this.brightness = brightness;
      return this;
   }

   public T apply(boolean hasEffect) {
      float red = this.colorRed;
      float green = this.colorGreen;
      float blue = this.colorBlue;
      GameLight light = this.light;
      if (hasEffect && this.gameClock != null) {
         long currentTime = this.gameClock.getTime();
         currentTime += this.fadeTime * this.glowHash;
         float hue = (float)Math.floorMod(currentTime, this.fadeTime) / (float)this.fadeTime;
         int spelunkerRGB = Color.HSBtoRGB(hue, this.saturation, this.brightness);
         float spelunkerRed = (spelunkerRGB >> 16 & 0xFF) / 255.0F;
         float spelunkerGreen = (spelunkerRGB >> 8 & 0xFF) / 255.0F;
         float spelunkerBlue = (spelunkerRGB & 0xFF) / 255.0F;
         red *= spelunkerRed;
         green *= spelunkerGreen;
         blue *= spelunkerBlue;
         if (light != null) {
            light = light.minLevelCopy(this.minLight);
         }
      }

      return (T)(light != null ? this.options.colorLight(red, green, blue, this.colorAlpha, light) : this.options.color(red, green, blue, this.colorAlpha));
   }

   public T applySpelunker(PlayerMob perspective) {
      return this.apply(perspective != null && perspective.buffManager.getModifier(BuffModifiers.SPELUNKER));
   }

   public T applyTreasure(PlayerMob perspective) {
      return this.apply(perspective != null && perspective.buffManager.getModifier(BuffModifiers.TREASURE_HUNTER));
   }

   public T applyEnemyTracker(Mob targetMob, PlayerMob perspective) {
      return this.apply(
         targetMob.shouldEnemyTrackerGlow(perspective) && perspective != null && perspective.buffManager.getModifier(BuffModifiers.ENEMY_TRACKER)
      );
   }
}
