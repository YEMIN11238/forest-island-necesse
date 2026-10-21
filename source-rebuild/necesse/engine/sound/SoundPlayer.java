package necesse.engine.sound;

import java.nio.FloatBuffer;
import necesse.engine.Settings;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.sound.gameSound.GameSoundStreamer;
import necesse.engine.window.WindowManager;

public abstract class SoundPlayer {
   public final GameSound gameSound;
   private final GameSoundStreamer streamer;
   public SoundEffect effect;
   protected int source;
   private int maxLoadedSamples;
   private int maxQueuedSamples;
   private int startSamples;
   private long startTime;
   private boolean playing;
   private boolean paused;
   private boolean stopped;
   private boolean disposed;
   private int pauseSamplesPosition;
   private float fadeInVolume;
   private float fadeInSeconds = -1.0F;
   private float fadeOutVolume;
   private float fadeOutSeconds = -1.0F;
   private float loopFadeVolume;
   private float loopFadeSeconds = -1.0F;
   private boolean wasStoppedLastFrame = true;

   public SoundPlayer(GameSound gameSound, SoundEffect effect) {
      this.gameSound = gameSound;
      this.streamer = gameSound.getStreamer();
      this.effect = effect;
   }

   public static SoundEmitter SimpleEmitter(float x, float y) {
      return new SoundPlayer.SimpleSoundEmitter(x, y);
   }

   public void playSound() {
      this.playSound(0.0);
   }

   public void playSound(double seconds) {
      if (!this.disposed) {
         this.alStopAndDeleteSource();
         this.alSetupSource();
         this.startSamples = (int)(seconds * this.streamer.getSampleRate());
         this.maxLoadedSamples = this.startSamples;
         this.maxQueuedSamples = this.startSamples;
         this.startTime = System.currentTimeMillis();
         this.stopped = false;
         this.paused = false;
         this.playing = true;
         this.alPlaySource();
         this.update();
      }
   }

   protected abstract void alStopAndDeleteSource();

   protected abstract void alSetupSource();

   public void update() {
      if (this.source != 0) {
         this.updateStreamer();
         float volumeMod = 1.0F;
         if (Settings.muteOnFocusLoss && !WindowManager.getWindow().isFocused()) {
            volumeMod = 0.0F;
         } else {
            if (this.fadeInSeconds > 0.0F) {
               float volumeIncPerTick = 1.0F / this.fadeInSeconds / 20.0F;
               volumeMod *= this.fadeInVolume;
               this.fadeInVolume = Math.min(this.fadeInVolume + volumeIncPerTick, 1.0F);
            }

            if (this.loopFadeSeconds > 0.0F) {
               float volumeDecPerTick = 1.0F / this.loopFadeSeconds / 20.0F;
               volumeMod *= this.loopFadeVolume;
               this.loopFadeVolume = Math.max(0.0F, this.loopFadeVolume - volumeDecPerTick);
            }

            if (this.fadeOutSeconds > 0.0F) {
               float volumeDecPerTick = 1.0F / this.fadeOutSeconds / 20.0F;
               volumeMod *= this.fadeOutVolume;
               this.fadeOutVolume = Math.max(0.0F, this.fadeOutVolume - volumeDecPerTick);
            }
         }

         this.effect.updateSound(this, this.source, volumeMod * this.gameSound.getVolumeModifier(), SoundManager.getALListener());
         boolean alIsSourceStopped = this.alIsSourceStopped();
         if (alIsSourceStopped && !this.wasStoppedLastFrame) {
            this.stop();
         }

         this.wasStoppedLastFrame = alIsSourceStopped;
      }
   }

   protected abstract void alPlaySource();

   private void updateStreamer() {
      if (!this.streamer.isWorking() && !this.streamer.isDone(this.maxLoadedSamples)) {
         int samplesToLoad = this.streamer.getSampleRate() * 5;
         if (this.getPositionSamples() > this.maxQueuedSamples - samplesToLoad) {
            this.streamer.getNextBuffers(this.maxQueuedSamples, samplesToLoad, (buffer, samplesOffset, samplesLoaded) -> {
               if (!this.isDisposed()) {
                  this.alQueueBuffers(buffer);
                  if (this.isPlaying() && this.alIsSourceStopped()) {
                     this.alPlaySource();
                  }

                  this.maxLoadedSamples = samplesOffset + samplesLoaded;
               }
            });
            this.maxQueuedSamples += samplesToLoad;
         }
      }
   }

   public int getPositionSamples() {
      if (!this.isPlaying()) {
         return this.pauseSamplesPosition;
      }

      long timeSinceStart = System.currentTimeMillis() - this.startTime;
      return this.startSamples + (int)(timeSinceStart / 1000.0 * this.streamer.getSampleRate());
   }

   public boolean isDisposed() {
      return this.disposed;
   }

   protected abstract void alQueueBuffers(int var1);

   public boolean isPlaying() {
      return this.playing;
   }

   protected abstract boolean alIsSourceStopped();

   public void stop() {
      if (!this.stopped) {
         this.stopped = true;
         this.paused = true;
         this.playing = false;
         if (this.source != 0) {
            this.effect.updateSound(this, this.source, 0.0F, SoundManager.getALListener());
         }
      }
   }

   public void fadeOutAndStop(float seconds) {
      this.fadeOutSeconds = seconds;
      float volumeDecPerTick = 1.0F / seconds / 20.0F;
      this.fadeOutVolume = 1.0F + volumeDecPerTick;
   }

   public SoundPlayer refreshLooping() {
      return this.refreshLooping(0.5F);
   }

   public SoundPlayer refreshLooping(float fadeOutTimeSeconds) {
      if (fadeOutTimeSeconds <= 0.0F) {
         throw new IllegalArgumentException("fadeTimeSeconds must be above 0");
      }

      this.loopFadeSeconds = fadeOutTimeSeconds;
      float volumeDecPerTick = 1.0F / this.loopFadeSeconds / 20.0F;
      this.loopFadeVolume = 1.0F + volumeDecPerTick;
      this.alSetSourceLooping(true);
      return this;
   }

   protected abstract void alSetSourceLooping(boolean var1);

   public SoundPlayer fadeIn(float fadeInTimeSeconds) {
      if (fadeInTimeSeconds <= 0.0F) {
         throw new IllegalArgumentException("fadeInTimeSeconds must be above 0");
      }

      this.fadeInSeconds = fadeInTimeSeconds;
      this.fadeInVolume = 0.0F;
      return this;
   }

   public SoundPlayer copyFadeInProgress(SoundPlayer other) {
      this.fadeInSeconds = other.fadeInSeconds;
      this.fadeInVolume = other.fadeInVolume;
      return this;
   }

   public boolean isDone() {
      if (this.stopped) {
         return true;
      }

      if (this.paused) {
         return false;
      }

      if (!this.playing) {
         return false;
      }

      boolean stopped = false;
      if (this.fadeOutSeconds > 0.0F) {
         stopped = this.fadeOutVolume <= 0.0F || this.alIsSourceStopped();
      }

      if (!stopped) {
         if (this.loopFadeSeconds > 0.0F) {
            stopped = this.loopFadeVolume <= 0.0F;
         } else {
            stopped = this.alIsSourceStopped();
         }
      }

      return stopped && !this.streamer.isWorking() && this.streamer.isDone(this.maxLoadedSamples);
   }

   public void setPosition(double seconds) {
      if (!this.disposed) {
         this.playSound(seconds);
      }
   }

   public double getSecondsLeft() {
      return this.getLengthInSeconds() - this.getPositionSeconds();
   }

   public double getLengthInSeconds() {
      return this.streamer.getLengthInSeconds();
   }

   public double getPositionSeconds() {
      return (float)this.getPositionSamples() / this.streamer.getSampleRate();
   }

   public void startOver() {
      this.playSound(0.0);
   }

   public void pause() {
      if (!this.disposed && this.playing) {
         this.alPauseSource();
         this.pauseSamplesPosition = this.getPositionSamples();
         this.paused = true;
         this.playing = false;
      }
   }

   public boolean isPaused() {
      return this.paused;
   }

   protected abstract void alPauseSource();

   public abstract void alSetPitch(float var1);

   public abstract void alSetGain(float var1);

   public abstract void alSetPosition(FloatBuffer var1);

   public void dispose() {
      if (!this.disposed) {
         this.disposed = true;
         this.alStopAndDeleteSource();
         this.streamer.dispose();
      }
   }

   private static class SimpleSoundEmitter implements SoundEmitter {
      private final float x;
      private final float y;

      public SimpleSoundEmitter(float x, float y) {
         this.x = x;
         this.y = y;
      }

      @Override
      public float getSoundPositionX() {
         return this.x;
      }

      @Override
      public float getSoundPositionY() {
         return this.y;
      }
   }
}
