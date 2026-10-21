package necesse.engine.platforms.sharedOnPC.sound;

import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.sound.gameSound.SampleGameSoundStreamer;
import org.lwjgl.openal.AL10;

public class LWJGLSampleGameSoundStreamer extends SampleGameSoundStreamer {
   public LWJGLSampleGameSoundStreamer(GameSound.VorbisSamples data) {
      super(data);
   }

   @Override
   protected void generateBuffers(int[] buffers) {
      AL10.alGenBuffers(buffers);
   }

   @Override
   protected void deleteBuffers(int bufferCount) {
      AL10.alDeleteBuffers(bufferCount);
   }

   @Override
   protected void bufferData(int buffer, boolean isStereo, short[] audioData, int sampleRate) {
      AL10.alBufferData(buffer, isStereo ? 4355 : 4353, audioData, sampleRate);
   }
}
