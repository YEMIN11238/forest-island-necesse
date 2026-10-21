package necesse.engine.platforms.sharedOnPC.sound;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.sound.gameSound.GameSoundStreamer;
import necesse.engine.sound.gameSound.ResourceGameSoundStreamer;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryUtil;

public class LWJGLResourceGameSoundStreamer extends ResourceGameSoundStreamer {
   public LWJGLResourceGameSoundStreamer(GameSound sound, ByteBuffer inputBytes) {
      super(sound, inputBytes);
   }

   @Override
   protected void loadVorbisData() {
      if (this.errorBuffer != null) {
         MemoryUtil.memFree(this.errorBuffer);
      }

      this.errorBuffer = MemoryUtil.memAllocInt(1);
      STBVorbisInfo info = STBVorbisInfo.malloc();

      label54: {
         try {
            synchronized (this.inputBytes) {
               this.decoder = STBVorbis.stb_vorbis_open_memory(this.inputBytes, this.errorBuffer, null);
               if (this.decoder == 0L) {
                  System.err.println("Error creating decoder for " + this.sound.path + ": " + this.errorBuffer.get(0));
                  break label54;
               }

               STBVorbis.stb_vorbis_get_info(this.decoder, info);
               this.channels = info.channels();
               this.sampleRate = info.sample_rate();
               this.sampleLength = STBVorbis.stb_vorbis_stream_length_in_samples(this.decoder);
               this.lengthSeconds = (float)this.sampleLength / this.sampleRate;
            }
         } catch (Throwable var6) {
            if (info != null) {
               try {
                  info.close();
               } catch (Throwable var4) {
                  var6.addSuppressed(var4);
               }
            }

            throw var6;
         }

         if (info != null) {
            info.close();
         }

         return;
      }

      if (info != null) {
         info.close();
      }
   }

   @Override
   protected void decodeInput(int sampleOffset, ShortBuffer audioBuffer, GameSoundStreamer.BufferHandler handler) {
      if (STBVorbis.stb_vorbis_seek(this.decoder, sampleOffset)) {
         int samplesLoaded = STBVorbis.stb_vorbis_get_samples_short_interleaved(this.decoder, this.channels, audioBuffer);
         if (this.errorBuffer.get(0) != 0L) {
            System.err.println("Error getting audio samples for " + this.sound.path + " - " + this.errorBuffer.get(0));
            return;
         }

         audioBuffer.position(0);
         if (this.isDisposed.get()) {
            return;
         }

         int buffer = AL10.alGenBuffers();
         AL10.alBufferData(buffer, this.channels > 1 ? 4355 : 4353, audioBuffer, this.sampleRate);
         this.buffers.add(buffer);
         handler.handle(buffer, sampleOffset, samplesLoaded);
      }
   }

   @Override
   protected void disposeVorbisData() {
      this.buffers.forEach(AL10::alDeleteBuffers);
      if (this.decoder != 0L) {
         STBVorbis.stb_vorbis_close(this.decoder);
         this.decoder = 0L;
      }
   }
}
