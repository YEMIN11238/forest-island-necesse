package necesse.entity.levelEvent;

import java.awt.Color;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;

public class ChickenGlyphTrapEvent extends GlyphObjectTrapEvent {
   public static Color particleColor = new Color(255, 206, 82);
   public static float particleHue = 43.0F;

   public ChickenGlyphTrapEvent() {
   }

   public ChickenGlyphTrapEvent(int x, int y, GameRandom uniqueIDRandom) {
      super(x, y, uniqueIDRandom);
   }

   @Override
   public GameTexture getParticleTexture() {
      return GameResources.chickenGlyphParticle;
   }

   @Override
   public Color getParticleColor() {
      return particleColor;
   }

   @Override
   public float getParticleColorHue() {
      return particleHue;
   }

   @Override
   public void playGlyphSound() {
      SoundManager.playSound(
         GameResources.humanChicken, SoundEffect.effect(this.x, this.y).volume(0.8F).pitch(GameRandom.globalRandom.getFloatBetween(1.0F, 1.1F))
      );
      SoundManager.playSound(GameResources.fizz, SoundEffect.effect(this.x, this.y).volume(0.8F).pitch(GameRandom.globalRandom.getFloatBetween(1.0F, 1.1F)));
   }

   @Override
   public void applyGlyphServer(Mob target) {
      target.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.POLYMORPH_CHICKEN, target, 15000, null), true);
   }
}
