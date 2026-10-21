package necesse.engine.platforms.sharedOnPC.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundCooldown;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.sound.gameSound.ResourceGameSoundStreamer;
import necesse.engine.sound.gameSound.SampleGameSoundStreamer;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.ALUtil;

public class LWJGLSoundManager extends SoundManager {
   private String alDeviceName;
   private long alContext;
   private long alDevice;
   private float retryFailedAudioDeviceTimer = 5000.0F;

   @Override
   public boolean initialize() {
      try {
         Thread.sleep(100L);
         this.initALDevice();
      } catch (InterruptedException e) {
         e.printStackTrace();
         return false;
      }

      return super.initialize();
   }

   @Override
   protected SoundManager getCurrentSoundManager() {
      return this;
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.alContext != 0L) {
         ALC10.alcDestroyContext(this.alContext);
      }

      if (this.alDevice != 0L) {
         ALC10.alcCloseDevice(this.alDevice);
      }
   }

   @Override
   public List<String> getOutputDevices() {
      return ALUtil.getStringList(0L, 4115);
   }

   @Override
   public boolean audioDeviceReady() {
      return this.alDevice != 0L && this.alContext != 0L;
   }

   @Override
   public void checkForNewDefaultAudioDevice() {
      if (Settings.outputDevice == null) {
         ALC10.alcGetString(0L, 4115);
         String defaultDeviceName = ALC10.alcGetString(0L, 4114);
         if (!Objects.equals(this.alDeviceName, defaultDeviceName)) {
            this.initALDevice();
         }
      }
   }

   @Override
   public String getCurrentAudioDeviceName() {
      return this.alDeviceName;
   }

   @Override
   public void setAudioDeviceFromSettings() {
      this.initALDevice();
   }

   @Override
   public GameSound createGameSound(String path, SoundCooldown cooldown, boolean isMusic) throws IOException {
      return new LWJGLGameSound(path, cooldown, isMusic);
   }

   @Override
   public SampleGameSoundStreamer createSampleGameSoundStreamer(GameSound.VorbisSamples data) {
      return new LWJGLSampleGameSoundStreamer(data);
   }

   @Override
   public ResourceGameSoundStreamer createResourceGameSoundStreamer(GameSound sound, ByteBuffer inputBytes) {
      return new LWJGLResourceGameSoundStreamer(sound, inputBytes);
   }

   @Override
   public SoundPlayer createSoundPlayer(GameSound gameSound, SoundEffect effect) {
      return new LWJGLSoundPlayer(gameSound, effect);
   }

   private void initALDevice() {
      if (Settings.outputDevice == null || !Settings.outputDevice.equals(this.alDeviceName) || this.alContext == 0L) {
         if (weather != null) {
            weather.dispose();
            weather = null;
         }

         if (fadingWeather != null) {
            fadingWeather.dispose();
            fadingWeather = null;
         }

         musicManager.dispose();
         synchronized (soundSynchronized) {
            for (SoundPlayer playingSound : playingSounds) {
               playingSound.dispose();
            }

            playingSounds.clear();
         }

         ALC10.alcMakeContextCurrent(0L);
         if (this.alDevice != 0L) {
            ALC10.alcCloseDevice(this.alDevice);
         }

         if (this.alContext != 0L) {
            ALC10.alcDestroyContext(this.alContext);
         }

         this.alDevice = 0L;
         this.alContext = 0L;
         this.alDeviceName = "";
         String deviceName = "";
         long device = 0L;
         if (Settings.outputDevice != null) {
            device = ALC10.alcOpenDevice(Settings.outputDevice);
            deviceName = Settings.outputDevice;
         }

         if (device == 0L) {
            String defaultDeviceName = ALC10.alcGetString(0L, 4114);
            device = ALC10.alcOpenDevice(defaultDeviceName);
            deviceName = defaultDeviceName;
            if (device == 0L) {
               System.err.println("Could initialize OpenAL: No device was found");
               return;
            }
         }

         this.alDeviceName = deviceName;
         this.alDevice = device;
         int[] attributes = new int[]{0};
         this.alContext = ALC10.alcCreateContext(this.alDevice, attributes);
         if (this.alContext != 0L && ALC10.alcMakeContextCurrent(this.alContext)) {
            ALCapabilities alCapabilities;
            try {
               ALCCapabilities alcCapabilities = ALC.createCapabilities(this.alDevice);
               alCapabilities = AL.createCapabilities(alcCapabilities);
            } catch (IllegalStateException e) {
               this.alContext = 0L;
               this.retryFailedAudioDeviceTimer = 5000.0F;
               System.err.println("Could initialize OpenAL: Could not create alcCapabilities for " + deviceName);
               return;
            }

            if (!alCapabilities.OpenAL11) {
               GameLog.warn.println("OpenAL11 is not supported on this device");
            } else if (!alCapabilities.OpenAL10) {
               GameLog.warn.println("OpenAL10 is not supported on this device");
            }

            AL10.alListenerfv(4100, new float[]{0.0F, 0.0F, 0.0F});
            AL10.alListenerfv(4102, new float[]{0.0F, 0.0F, 0.0F});
            AL10.alListenerfv(4111, new float[]{0.0F, 0.0F, -1.0F, 0.0F, 1.0F, 0.0F});
         } else {
            this.alContext = 0L;
            this.retryFailedAudioDeviceTimer = 5000.0F;
            System.err.println("Could initialize OpenAL: Could not create device context for " + deviceName);
         }
      }
   }

   @Override
   public void postGameTick(TickManager tickManager) {
      super.postGameTick(tickManager);
      if (this.retryFailedAudioDeviceTimer > 0.0F && this.alContext == 0L) {
         this.retryFailedAudioDeviceTimer = this.retryFailedAudioDeviceTimer - tickManager.getDelta();
         if (this.retryFailedAudioDeviceTimer <= 0.0F) {
            this.retryFailedAudioDeviceTimer = 5000.0F;
            GameLog.out.println("Retrying to set audio device that failed to setup last time.");
            this.initALDevice();
         }
      }
   }
}
