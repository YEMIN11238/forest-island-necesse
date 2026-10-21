package necesse.engine.sound;

import necesse.gfx.GameResources;

public class SoundSettingsRegistry {
   public static SoundSettings wind = new SoundSettings(GameResources.wind1, GameResources.wind2, GameResources.wind3).volume(0.1F).fallOffDistance(700);
   public static SoundSettings windSnow = new SoundSettings(GameResources.windSnow1, GameResources.windSnow2, GameResources.windSnow3)
      .volume(0.2F)
      .fallOffDistance(700);
   public static SoundSettings windDesert = new SoundSettings(GameResources.windDesert1, GameResources.windDesert2, GameResources.windDesert3)
      .volume(0.2F)
      .fallOffDistance(700);
   public static SoundSettings windPlains = new SoundSettings(GameResources.windPlains1, GameResources.windPlains2, GameResources.windPlains3)
      .volume(0.2F)
      .fallOffDistance(700);
   public static SoundSettings windSwamp = new SoundSettings(GameResources.windSwamp1, GameResources.windSwamp2, GameResources.windSwamp3)
      .volume(0.2F)
      .fallOffDistance(700);
   public static SoundSettings leavesBreakAmbient = new SoundSettings(GameResources.leavesbreak2, GameResources.leavesbreak3, GameResources.leavesbreak4)
      .volume(0.1F)
      .pitchVariance(0.1F)
      .fallOffDistance(500)
      .cooldown(new SameNearSoundCooldown(4000, 200));
   public static SoundSettings leavesBreakAction = new SoundSettings(GameResources.leavesbreak2, GameResources.leavesbreak3, GameResources.leavesbreak4)
      .volume(0.1F)
      .basePitch(1.5F)
      .pitchVariance(0.1F);
   public static SoundSettings fizzAmbient = new SoundSettings(GameResources.fizz)
      .volume(0.1F)
      .basePitch(1.0F)
      .pitchVariance(0.2F)
      .fallOffDistance(500)
      .cooldown(new SameNearSoundCooldown(2000, 200));
   public static SoundSettings defaultOpen = new SoundSettings(GameResources.defaultOpen).volume(0.3F).basePitch(0.9F);
   public static SoundSettings workstationOpen = new SoundSettings(GameResources.workstationOpen).volume(0.4F);
   public static SoundSettings landscapingStationOpen = new SoundSettings(GameResources.landscapingStationOpen).volume(0.5F);
   public static SoundSettings anvilOpen = new SoundSettings(GameResources.anvilOpen).volume(0.4F).pitchVariance(0.01F);
   public static SoundSettings crawlerFootsteps = new SoundSettings(GameResources.blunthit)
      .volume(0.16F)
      .basePitch(4.6F)
      .pitchVariance(0.1F)
      .fallOffDistance(2000);
   public static SoundSettings humanFootsteps = new SoundSettings(GameResources.blunthit)
      .volume(0.14F)
      .basePitch(1.7F)
      .pitchVariance(0.1F)
      .fallOffDistance(1000);
   public static SoundSettings smallFootsteps = new SoundSettings(GameResources.blunthit).volume(0.05F).basePitch(2.4F).pitchVariance(0.1F);
   public static SoundSettings cryoQuakeProjectileMove = new SoundSettings(GameResources.jinglehit)
      .volume(0.4F)
      .basePitch(0.7F)
      .pitchVariance(0.0F)
      .fallOffDistance(350);
   public static SoundSettings crystalGlyph = new SoundSettings(GameResources.crystalGlyph).volume(0.6F);
}
