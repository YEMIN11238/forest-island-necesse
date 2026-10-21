package necesse.engine.platforms.sharedOnPC.sound;

import java.nio.FloatBuffer;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.gameSound.GameSound;
import org.lwjgl.openal.AL10;

public class LWJGLSoundPlayer extends SoundPlayer {
   public LWJGLSoundPlayer(GameSound gameSound, SoundEffect effect) {
      super(gameSound, effect);
   }

   public static String getError() {
      int error = AL10.alGetError();
      switch (error) {
         case 40961:
            return "AL_INVALID_NAME";
         case 40962:
            return "AL_INVALID_ENUM";
         case 40963:
            return "AL_INVALID_VALUE";
         case 40964:
            return "AL_INVALID_OPERATION";
         case 40965:
            return "AL_OUT_OF_MEMORY";
         default:
            return null;
      }
   }

   @Override
   protected void alStopAndDeleteSource() {
      if (this.source != 0) {
         AL10.alSourceStop(this.source);
         AL10.alDeleteSources(this.source);
         this.source = 0;
      }
   }

   @Override
   protected void alSetupSource() {
      this.source = AL10.alGenSources();
      AL10.alSourcei(this.source, 514, 1);
      AL10.alSourcef(this.source, 4131, 0.0F);
   }

   @Override
   protected void alPlaySource() {
      AL10.alSourcePlay(this.source);
   }

   @Override
   protected void alQueueBuffers(int buffer) {
      AL10.alSourceQueueBuffers(this.source, buffer);
   }

   @Override
   protected boolean alIsSourceStopped() {
      return AL10.alGetSourcei(this.source, 4112) == 4116;
   }

   @Override
   protected void alSetSourceLooping(boolean loops) {
      AL10.alSourcei(this.source, 4103, loops ? 1 : 0);
   }

   @Override
   protected void alPauseSource() {
      AL10.alSourcePause(this.source);
   }

   @Override
   public void alSetPitch(float pitch) {
      AL10.alSourcef(this.source, 4099, pitch);
   }

   @Override
   public void alSetGain(float volume) {
      AL10.alSourcef(this.source, 4106, volume);
   }

   @Override
   public void alSetPosition(FloatBuffer position) {
      AL10.alSourcefv(this.source, 4100, position);
   }

   private String state() {
      int state = AL10.alGetSourcei(this.source, 4112);
      switch (state) {
         case 4113:
            return "AL_INITIAL";
         case 4114:
            return "AL_PLAYING";
         case 4115:
            return "AL_PAUSED";
         case 4116:
            return "AL_STOPPED";
         default:
            return "N/A";
      }
   }
}
