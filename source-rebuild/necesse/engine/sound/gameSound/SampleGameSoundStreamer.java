package necesse.engine.sound.gameSound;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.sound.SoundManager;

public abstract class SampleGameSoundStreamer extends GameSoundStreamer {
   public final GameSound.VorbisSamples data;
   private final AtomicBoolean isWorking;
   private final LinkedList<Integer> buffers = new LinkedList<>();

   public SampleGameSoundStreamer(GameSound.VorbisSamples data) {
      this.data = data;
      this.isWorking = new AtomicBoolean(false);
   }

   @Override
   public int getLengthInSamples() {
      return this.data.info.totalSamples;
   }

   @Override
   public int getSampleRate() {
      return this.data.info.sampleRate;
   }

   @Override
   public float getLengthInSeconds() {
      return this.data.info.lengthSeconds;
   }

   @Override
   public boolean isDone(int loadedSamples) {
      return loadedSamples >= this.data.info.totalSamples * this.data.info.channels;
   }

   @Override
   public boolean isWorking() {
      return this.isWorking.get();
   }

   @Override
   public void getNextBuffers(int sampleOffset, int minSamples, GameSoundStreamer.BufferHandler handler) {
      if (!this.isWorking()) {
         this.isWorking.set(true);
         SoundManager.musicStreamer.submit(() -> {
            try {
               synchronized (this.data) {
                  int samplesToLoad = Math.min(this.data.info.totalSamples - sampleOffset, minSamples);
                  int lengthToLoad = samplesToLoad * this.data.info.channels;
                  int maxBufferSize = 1048576;
                  int loadedSamples = sampleOffset;
                  int buffersLength = lengthToLoad / maxBufferSize + 1;
                  int[] buffers = new int[buffersLength];
                  this.generateBuffers(buffers);
                  int pos = 0;

                  for (int buffer : buffers) {
                     int size = Math.min(maxBufferSize, lengthToLoad - pos);
                     short[] partBuffer = new short[size];
                     int offset = loadedSamples;
                     this.data.samples.position(offset * this.data.info.channels);
                     this.data.samples.get(partBuffer);
                     loadedSamples += size / this.data.info.channels;
                     pos += size;
                     this.bufferData(buffer, this.data.info.channels > 1, partBuffer, this.data.info.sampleRate);
                     this.buffers.add(buffer);
                     handler.handle(buffer, offset, loadedSamples);
                  }
               }
            } finally {
               this.isWorking.set(false);
            }
         });
      }
   }

   @Override
   public void disposeBuffer(int buffer) {
      synchronized (this.data) {
         if (this.buffers.contains(buffer)) {
            this.deleteBuffers(buffer);
            this.buffers.remove(Integer.valueOf(buffer));
         }
      }
   }

   @Override
   public void dispose() {
      synchronized (this.data) {
         this.buffers.forEach(this::deleteBuffers);
      }
   }

   protected abstract void deleteBuffers(int var1);

   protected abstract void generateBuffers(int[] var1);

   protected abstract void bufferData(int var1, boolean var2, short[] var3, int var4);
}
