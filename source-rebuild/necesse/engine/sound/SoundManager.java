package necesse.engine.sound;

import java.awt.geom.Point2D.Float;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import necesse.engine.AbstractMusicList;
import necesse.engine.GlobalData;
import necesse.engine.MusicList;
import necesse.engine.MusicOptions;
import necesse.engine.PlayingMusicManager;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.sound.gameSound.ResourceGameSoundStreamer;
import necesse.engine.sound.gameSound.SampleGameSoundStreamer;
import necesse.engine.state.State;
import necesse.engine.util.ComparableSequence;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.window.WindowManager;

public abstract class SoundManager {
   protected static final Object soundSynchronized = new Object();
   public static ExecutorService musicStreamer;
   public static SoundEmitter listener;
   protected static SoundPlayer weather;
   protected static SoundPlayer fadingWeather;
   protected static ArrayList<SoundPlayer> playingSounds = new ArrayList<>();
   protected static LinkedList<SoundTime> recentSounds = new LinkedList<>();
   protected static PlayingMusicManager musicManager = new PlayingMusicManager();
   private static SoundManager currentSoundManager;
   private static boolean wasWindowFocused = false;

   public static void forceChangeMusic() {
      musicManager.forceChangeMusic();
   }

   public static void setMusic(AbstractMusicList list, SoundManager.MusicPriority priority) {
      setMusic(list, priority.priority);
   }

   public static void setMusic(AbstractMusicList list, int priority) {
      setMusic(list, new ComparableSequence<>(priority));
   }

   public static void setMusic(AbstractMusicList list, ComparableSequence<Integer> priority) {
      musicManager.setNextMusic(list, priority);
   }

   public static void setMusic(MusicOptions music, SoundManager.MusicPriority priority) {
      setMusic(music, priority.priority);
   }

   public static void setMusic(MusicOptions music, int priority) {
      setMusic(music, new ComparableSequence<>(priority));
   }

   public static void setMusic(MusicOptions music, ComparableSequence<Integer> priority) {
      setMusic(new MusicList().addMusic(music), priority);
   }

   public static void setMusic(GameMusic music, SoundManager.MusicPriority priority, float volume) {
      setMusic(music, priority.priority, volume);
   }

   public static void setMusic(GameMusic music, int priority, float volume) {
      setMusic(music, new ComparableSequence<>(priority), volume);
   }

   public static void setMusic(GameMusic music, ComparableSequence<Integer> priority, float volume) {
      setMusic(new MusicList().addMusic(music, volume), priority);
   }

   public static void setMusic(GameMusic music, SoundManager.MusicPriority priority) {
      setMusic(music, priority.priority);
   }

   public static void setMusic(GameMusic music, int priority) {
      setMusic(music, new ComparableSequence<>(priority));
   }

   public static void setMusic(GameMusic music, ComparableSequence<Integer> priority) {
      setMusic(new MusicList().addMusic(music), priority);
   }

   public static GameMusic getCurrentMusic() {
      return musicManager.getCurrentMusic();
   }

   public static SoundPlayer playSound(GameSound gameSound, SoundEffect effect) {
      return gameSound == null ? null : playSound(gameSound, effect, gameSound.cooldown);
   }

   public static SoundPlayer playSound(GameSound gameSound, SoundEffect effect, Consumer<SoundPlayer> onCreated) {
      return gameSound == null ? null : playSound(gameSound, effect, gameSound.cooldown, onCreated);
   }

   public static SoundPlayer playSoundAtPosition(GameSound gameSound, float position, SoundEffect effect, Consumer<SoundPlayer> onCreated) {
      return gameSound == null ? null : playSoundAtPosition(gameSound, position, effect, gameSound.cooldown, onCreated);
   }

   public static SoundPlayer playSound(GameSound gameSound, SoundEffect effect, SoundCooldown cooldown) {
      return playSound(gameSound, effect, cooldown, null);
   }

   public static SoundPlayer playSound(GameSound gameSound, SoundEffect effect, SoundCooldown cooldown, Consumer<SoundPlayer> onCreated) {
      return playSoundAtPosition(gameSound, 0.0, effect, cooldown, onCreated);
   }

   public static SoundPlayer playSoundAtPosition(
      GameSound gameSound, double position, SoundEffect effect, SoundCooldown cooldown, Consumer<SoundPlayer> onCreated
   ) {
      if (gameSound == null || effect == null) {
         return null;
      }

      if (!currentSoundManager.audioDeviceReady()) {
         return null;
      }

      SoundPlayer player = currentSoundManager.createSoundPlayer(gameSound, effect);
      if (onCreated != null) {
         onCreated.accept(player);
      }

      addQueuedSound(player, position, cooldown);
      return player;
   }

   public static SoundPlayer playSound(SoundSettings soundSettings, float x, float y) {
      return playSound(soundSettings, SoundEffect.effect(x, y));
   }

   public static SoundPlayer playSound(SoundSettings soundSettings, PrimitiveSoundEmitter emitter) {
      return playSound(soundSettings, SoundEffect.effect(emitter));
   }

   public static SoundPlayer playSound(SoundSettings soundSettings, PositionSoundEffect effect) {
      return playSound(soundSettings, effect, null);
   }

   public static SoundPlayer playSound(SoundSettings soundSettings, PositionSoundEffect effect, Consumer<SoundPlayer> onCreated) {
      if (soundSettings != null && soundSettings.sounds != null) {
         GameSound sound = soundSettings.sounds[GameRandom.globalRandom.getIntBetween(0, soundSettings.sounds.length - 1)];
         if (sound == null) {
            return null;
         }

         SoundCooldown cooldown = soundSettings.getCooldown();
         return playSound(sound, soundSettings.applySettings(effect), cooldown != null ? cooldown : sound.cooldown, onCreated);
      } else {
         return null;
      }
   }

   public abstract boolean audioDeviceReady();

   public abstract SoundPlayer createSoundPlayer(GameSound var1, SoundEffect var2);

   private static void addQueuedSound(SoundPlayer sound, double position, SoundCooldown cooldown) {
      synchronized (soundSynchronized) {
         if (cooldown != null) {
            cooldown = cooldown.initializeForSound(sound);
         }

         if (cooldown != null) {
            for (SoundTime recentSound : recentSounds) {
               if (cooldown.isBlockedBy(sound, recentSound)) {
                  sound.dispose();
                  return;
               }
            }
         }

         sound.playSound(position);
         playingSounds.add(sound);
         if (cooldown != null && cooldown.getTimeToLive() > 0) {
            synchronized (soundSynchronized) {
               recentSounds.add(new SoundTime(sound, cooldown));
            }
         }
      }
   }

   public static int getPlayingSoundsCount() {
      int count = 0;
      if (playingSounds != null) {
         count += playingSounds.size();
      }

      if (weather != null) {
         count++;
      }

      if (fadingWeather != null) {
         count++;
      }

      return count + musicManager.getPlayingCount();
   }

   public static void setWeatherSound(GameSound sound, float dx, float dy, float distance, float intensity) {
      if (currentSoundManager.audioDeviceReady()) {
         if (weather != null && weather.getSecondsLeft() < 0.7F) {
            fadingWeather = weather;
            updateWeatherVol(fadingWeather);
            weather = null;
         }

         Float dir = GameMath.normalize(dx, dy);
         SoundEffect effect = new RainSoundEffect(() -> Settings.masterVolume * Settings.weatherVolume, dir.x, dir.y, distance, intensity);
         if (weather == null) {
            weather = currentSoundManager.createSoundPlayer(sound, effect);
            weather.playSound();
         } else {
            weather.effect = effect;
         }

         updateWeatherVol(weather);
      }
   }

   private static void updateWeatherVol(SoundPlayer sound) {
      if (sound.getSecondsLeft() < 0.5) {
         float vol = (float)(sound.getSecondsLeft() / 0.5);
         sound.effect.volume(vol);
      } else if (sound.getPositionSeconds() < 0.5) {
         float vol = (float)(sound.getPositionSeconds() / 0.5);
         sound.effect.volume(vol);
      } else {
         sound.effect.volume(1.0F);
      }

      sound.update();
   }

   public boolean initialize() {
      musicStreamer = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), r -> new Thread(null, r, "music-streamer"));
      listener = getALListener();
      currentSoundManager = this.getCurrentSoundManager();
      return true;
   }

   public static SoundEmitter getALListener() {
      State state = GlobalData.getCurrentState();
      return state == null ? new SoundEmitter() {
         @Override
         public float getSoundPositionX() {
            return 0.0F;
         }

         @Override
         public float getSoundPositionY() {
            return 0.0F;
         }
      } : state.getALListener();
   }

   protected abstract SoundManager getCurrentSoundManager();

   public void dispose() {
      GameSound.deleteSounds();
      synchronized (soundSynchronized) {
         playingSounds.forEach(SoundPlayer::dispose);
      }

      musicManager.dispose();

      try {
         if (musicStreamer != null) {
            musicStreamer.shutdownNow();
            musicStreamer.awaitTermination(2L, TimeUnit.SECONDS);
         }
      } catch (InterruptedException var3) {
      }

      if (weather != null) {
         weather.dispose();
      }

      if (fadingWeather != null) {
         fadingWeather.dispose();
      }
   }

   public void preGameTick(TickManager tickManager) {
      if (tickManager.isGameTick()) {
         musicManager.clearNextMusic();
      }
   }

   public void postGameTick(TickManager tickManager) {
      if (tickManager.isGameTick() && this.audioDeviceReady()) {
         synchronized (soundSynchronized) {
            musicManager.tick();
            if (fadingWeather != null) {
               if (fadingWeather.isDone()) {
                  fadingWeather.dispose();
                  fadingWeather = null;
               } else {
                  updateWeatherVol(fadingWeather);
               }
            }

            if (weather != null) {
               if (weather.isDone()) {
                  weather.dispose();
                  weather = null;
               } else {
                  updateWeatherVol(weather);
               }
            }

            listener = getALListener();
            Performance.record(tickManager, "recentSounds", () -> {
               synchronized (soundSynchronized) {
                  recentSounds.removeIf(t -> t.getAge() > t.cooldown.getTimeToLive());
               }
            });
            Performance.record(tickManager, "playingSounds", () -> {
               synchronized (soundSynchronized) {
                  for (int i = 0; i < playingSounds.size(); i++) {
                     SoundPlayer s = playingSounds.get(i);
                     s.update();
                     if (s.isDone()) {
                        s.dispose();
                        playingSounds.remove(i);
                        i--;
                     }
                  }
               }
            });
         }
      }

      boolean focus = WindowManager.getWindow().isFocused();
      if (focus != wasWindowFocused) {
         wasWindowFocused = focus;
         updateVolume();
         if (focus) {
            this.checkForNewDefaultAudioDevice();
         }
      }
   }

   public static void updateVolume() {
      synchronized (soundSynchronized) {
         if (playingSounds != null) {
            playingSounds.forEach(SoundPlayer::update);
         }

         if (weather != null) {
            weather.update();
         }

         if (fadingWeather != null) {
            fadingWeather.update();
         }

         musicManager.updateVolume();
      }
   }

   public abstract void checkForNewDefaultAudioDevice();

   public abstract List<String> getOutputDevices();

   public abstract String getCurrentAudioDeviceName();

   public abstract void setAudioDeviceFromSettings();

   public abstract GameSound createGameSound(String var1, SoundCooldown var2, boolean var3) throws IOException;

   public abstract SampleGameSoundStreamer createSampleGameSoundStreamer(GameSound.VorbisSamples var1);

   public abstract ResourceGameSoundStreamer createResourceGameSoundStreamer(GameSound var1, ByteBuffer var2);

   public enum MusicPriority {
      BIOME(0),
      EVENT(100),
      MUSIC_PLAYER(1000),
      PORTABLE_MUSIC_PLAYER(2000);

      public final int priority;

      MusicPriority(int priority) {
         this.priority = priority;
      }

      public ComparableSequence<Integer> thenBy(int priority) {
         return new ComparableSequence<>(this.priority).thenBy(priority);
      }
   }
}
