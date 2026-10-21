package necesse.engine.platforms.sharedOnPC.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import necesse.engine.GameLoadingScreen;
import necesse.engine.sound.SoundCooldown;
import necesse.engine.sound.gameSound.GameSound;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class LWJGLGameSound extends GameSound {
   protected LWJGLGameSound(String path, SoundCooldown cooldown, boolean isMusic) throws IOException {
      super(path, cooldown, isMusic);
      GameLoadingScreen.drawLoadingSub("sound/" + path);
      byte[] data = this.loadBytes();
      if (isMusic) {
         this.inputBytes = MemoryUtil.memAlloc(data.length);
         this.inputBytes.put(data);
         this.inputBytes.position(0);
         MemoryStack stack = MemoryStack.stackPush();

         try {
            IntBuffer errorBuffer = stack.mallocInt(1);
            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            long decoder = STBVorbis.stb_vorbis_open_memory(this.inputBytes, errorBuffer, null);
            if (decoder != 0L) {
               int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
               STBVorbis.stb_vorbis_get_info(decoder, info);
               this.info = new GameSound.VorbisInfo(totalSamples, info.channels(), info.sample_rate());
               STBVorbis.stb_vorbis_close(decoder);
            } else {
               System.err.println("Error creating decoder for " + path + ": " + errorBuffer.get(0));
            }
         } catch (Throwable var12) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (stack != null) {
            stack.close();
         }
      } else {
         byte[] finalInputBytes = data;
         Callable<GameSound.VorbisSamples> completeSamplesLoading = () -> decodeVorbisSamples(this, finalInputBytes);
         if (loadingExecutor != null) {
            this.getSamplesLoading = loadingExecutor.submit(completeSamplesLoading);
         } else {
            FutureTask<GameSound.VorbisSamples> task = new FutureTask<>(completeSamplesLoading);
            this.getSamplesLoading = task;
            task.run();
            this.waitForDoneLoading();
         }
      }
   }

   private static GameSound.VorbisSamples decodeVorbisSamples(LWJGLGameSound sound, byte[] inputBytes) {
      long decoder = 0L;
      STBVorbisInfo info = null;
      IntBuffer errorBuffer = null;
      ByteBuffer inputData = null;

      try {
         errorBuffer = MemoryUtil.memAllocInt(1);
         info = STBVorbisInfo.malloc();
         inputData = MemoryUtil.memAlloc(inputBytes.length);
         inputData.put(inputBytes);
         inputData.position(0);
         decoder = STBVorbis.stb_vorbis_open_memory(inputData, errorBuffer, null);
         if (decoder == 0L) {
            System.err.println("Error creating decoder for " + sound.path + ": " + errorBuffer.get(0));
            return new GameSound.VorbisSamples(MemoryUtil.memAllocShort(0), new GameSound.VorbisInfo(0, 1, 0));
         } else {
            STBVorbis.stb_vorbis_get_info(decoder, info);
            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int sampleLength = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            ShortBuffer audioBuffer = MemoryUtil.memAllocShort(sampleLength * channels);
            int samplesLoaded = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, audioBuffer);
            audioBuffer.position(0);
            return new GameSound.VorbisSamples(audioBuffer, new GameSound.VorbisInfo(samplesLoaded, channels, sampleRate));
         }
      } finally {
         MemoryUtil.memFree(errorBuffer);
         if (info != null) {
            info.free();
         }

         MemoryUtil.memFree(inputData);
         if (decoder != 0L) {
            STBVorbis.stb_vorbis_close(decoder);
         }
      }
   }

   public static float getBufferSeconds(int buffer) {
      int sampleRate = AL10.alGetBufferi(buffer, 8193);
      int channels = AL10.alGetBufferi(buffer, 8195);
      int bytes = AL10.alGetBufferi(buffer, 8196);
      int bits = AL10.alGetBufferi(buffer, 8194);
      int samples = bytes / (bits / 8) / channels;
      return (float)samples / sampleRate;
   }

   private static void queryAndPrintALError(String prefix) {
      String error = queryALError();
      if (error != null) {
         System.err.println(prefix + "AL error: " + error);
      }
   }

   private static String queryALError() {
      int error = AL10.alGetError();
      return error == 0 ? null : AL10.alGetString(error);
   }
}
