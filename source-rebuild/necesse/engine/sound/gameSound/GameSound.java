package necesse.engine.sound.gameSound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.platforms.Platform;
import necesse.engine.sound.SameNearRandomSoundCooldown;
import necesse.engine.sound.SameNearSoundCooldown;
import necesse.engine.sound.SoundCooldown;
import necesse.engine.util.GameUtils;
import necesse.gfx.res.ResourceEncoder;
import org.lwjgl.system.MemoryUtil;

public abstract class GameSound {
   private static final int LOADING_THREADS = 10;
   protected static ThreadPoolExecutor loadingExecutor;
   private static final HashMap<String, GameSound> loadedSounds = new HashMap<>();
   public final String path;
   public final SoundCooldown cooldown;
   protected Future<GameSound.VorbisSamples> getSamplesLoading;
   protected ByteBuffer inputBytes;
   protected GameSound.VorbisInfo info;
   protected GameSound.VorbisSamples samples;
   private final boolean isMusic;
   private boolean foundInFile;
   private float volumeModifier = 1.0F;

   protected GameSound(String path, SoundCooldown cooldown, boolean isMusic) throws IOException {
      this.path = path;
      this.cooldown = cooldown;
      this.isMusic = isMusic;
   }

   public static void startLoaderThreads() {
      if (loadingExecutor == null) {
         loadingExecutor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(), defaultThreadFactory());
      }
   }

   private static ThreadFactory defaultThreadFactory() {
      AtomicInteger threadNum = new AtomicInteger(0);
      return r -> {
         Thread thread = new Thread(null, r, "sound-loader-" + threadNum.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      };
   }

   public static void endLoaderThreads() {
      for (GameSound sound : loadedSounds.values()) {
         sound.waitForDoneLoading();
      }

      loadingExecutor.shutdown();
      loadingExecutor = null;
   }

   public void waitForDoneLoading() {
      if (this.getSamplesLoading != null) {
         GameLoadingScreen.drawLoadingSub("sound/" + this.path);

         try {
            this.samples = this.getSamplesLoading.get();
         } catch (InterruptedException e) {
            this.samples = new GameSound.VorbisSamples(MemoryUtil.memAllocShort(0), new GameSound.VorbisInfo(0, 1, 0));
         } catch (ExecutionException e) {
            throw new RuntimeException(e);
         }

         if (this.samples.info.channels > 1 && !this.isMusic) {
            GameLog.warn.println(this.path + " audio file is not mono, so positional audio will not work.");
         }

         this.info = this.samples.info;
         if (!this.foundInFile && !GlobalData.isDevMode()) {
            GameLog.warn.println("sound/" + this.path + " was not found in resource file.");
         }
      }

      this.getSamplesLoading = null;
   }

   public static void listUnloadedSounds(List<String> excludes, List<String> excludeDirs) {
      if (excludes == null) {
         excludes = new ArrayList<>();
      }

      if (excludeDirs == null) {
         excludeDirs = new ArrayList<>();
      }

      listUnloadedSounds("", excludes, excludeDirs);
   }

   private static void listUnloadedSounds(String dir, List<String> excludes, List<String> excludeDirs) {
      excludes.replaceAll(s -> GameUtils.formatFileExtension(s, "ogg"));
      File fDir = new File(GlobalData.rootPath() + "res/sound/" + dir);
      String subDir = dir.isEmpty() ? "" : dir + "/";
      if (fDir.exists() && fDir.isDirectory()) {
         File[] files = fDir.listFiles();
         if (files != null) {
            for (File f : files) {
               if (f.isDirectory()) {
                  if (!excludeDirs.contains(subDir + f.getName())) {
                     listUnloadedSounds(subDir + f.getName(), excludes, excludeDirs);
                  }
               } else {
                  String name = f.getName();
                  String key = subDir + name;
                  if (name.endsWith(".ogg")) {
                     GameSound loadedTexture = loadedSounds.get(key);
                     if (loadedTexture == null && !excludes.contains(key)) {
                        GameLog.warn.println("Sound " + key + " is never loaded");
                     }
                  }
               }
            }
         }
      }
   }

   public static GameSound fromFile(String filePath) {
      return fromFile(filePath, null);
   }

   public static GameSound fromFile(String filePath, int cooldown) {
      return fromFile(filePath, cooldown > 0 ? new SameNearSoundCooldown(cooldown, 50) : null, false);
   }

   public static GameSound fromFile(String filePath, int minRandomCooldown, int maxRandomCooldown) {
      return fromFile(filePath, new SameNearRandomSoundCooldown(minRandomCooldown, maxRandomCooldown, 50), false);
   }

   public static GameSound fromFile(String filePath, SoundCooldown cooldown) {
      return fromFile(filePath, cooldown, false);
   }

   public static GameSound fromFile(String filePath, SoundCooldown cooldown, boolean isMusic) {
      Objects.requireNonNull(filePath);

      try {
         return fromFileRaw(filePath, cooldown, isMusic);
      } catch (IOException e) {
         System.err.println("Could not find sound file " + filePath);
         e.printStackTrace();
         return null;
      }
   }

   public static GameSound fromFileRaw(String filePath, SoundCooldown cooldown, boolean isMusic) throws IOException {
      filePath = GameUtils.formatFileExtension(filePath, "ogg");
      GameSound gameSound = loadedSounds.get(filePath);
      if (gameSound == null) {
         gameSound = Platform.getSoundManager().createGameSound(filePath, cooldown, isMusic);
         loadedSounds.put(filePath, gameSound);
      }

      return gameSound;
   }

   public static GameSound fromFileMusic(String filePath) {
      return fromFile(filePath, null, true);
   }

   public static boolean tryReloadSound(String path) {
      String soundPath = path.substring(path.lastIndexOf("\\res\\sound\\") + 11);
      GameSound existingSound = loadedSounds.get(soundPath);
      if (existingSound == null) {
         return false;
      } else {
         loadedSounds.remove(soundPath);
         GameSound newSound = fromFile(soundPath, existingSound.cooldown, existingSound.isMusic);
         if (newSound == null) {
            loadedSounds.put(soundPath, existingSound);
            return false;
         } else {
            existingSound.delete();
            existingSound.samples = newSound.samples;
            existingSound.inputBytes = newSound.inputBytes;
            existingSound.info = newSound.info;
            loadedSounds.put(soundPath, existingSound);
            return true;
         }
      }
   }

   public static void deleteSounds() {
      if (loadingExecutor != null) {
         loadingExecutor.shutdownNow();
      }

      for (GameSound sound : loadedSounds.values()) {
         sound.delete();
      }

      loadedSounds.clear();
   }

   public void delete() {
      if (this.samples != null) {
         MemoryUtil.memFree(this.samples.samples);
      }

      if (this.inputBytes != null) {
         MemoryUtil.memFree(this.inputBytes);
      }
   }

   protected byte[] loadBytes() throws IOException {
      File outsideFile = new File(GlobalData.rootPath() + "res/sound/" + this.path);
      if (outsideFile.exists()) {
         this.foundInFile = true;
         return GameUtils.loadByteFile(outsideFile);
      }

      try {
         this.foundInFile = true;
         return ResourceEncoder.getResourceBytes("sound/" + this.path);
      } catch (FileNotFoundException e) {
         return GameUtils.loadByteFile(outsideFile);
      }
   }

   public float getVolumeModifier() {
      return this.volumeModifier;
   }

   public GameSound setVolumeModifier(float modifier) {
      this.volumeModifier = modifier;
      return this;
   }

   public long getLengthInMillis() {
      return this.info != null ? this.info.lengthMillis : -1L;
   }

   public float getLengthInSeconds() {
      return this.info != null ? this.info.lengthSeconds : -1.0F;
   }

   public boolean isMusic() {
      return this.isMusic;
   }

   public GameSoundStreamer getStreamer() {
      return this.samples != null
         ? Platform.getSoundManager().createSampleGameSoundStreamer(this.samples)
         : Platform.getSoundManager().createResourceGameSoundStreamer(this, this.inputBytes);
   }

   public static class VorbisInfo {
      public final int totalSamples;
      public final int channels;
      public final int sampleRate;
      public final long lengthMillis;
      public final float lengthSeconds;

      public VorbisInfo(int totalSamples, int channels, int sampleRate) {
         this.totalSamples = totalSamples;
         this.channels = channels;
         this.sampleRate = sampleRate;
         if (sampleRate != 0) {
            this.lengthMillis = (long)((double)totalSamples / sampleRate * 1000.0);
            this.lengthSeconds = (float)((double)totalSamples / sampleRate);
         } else {
            this.lengthMillis = 0L;
            this.lengthSeconds = 0.0F;
         }
      }
   }

   public static class VorbisSamples {
      public final ShortBuffer samples;
      public final GameSound.VorbisInfo info;

      public VorbisSamples(ShortBuffer samples, GameSound.VorbisInfo info) {
         this.samples = samples;
         this.info = info;
      }
   }
}
