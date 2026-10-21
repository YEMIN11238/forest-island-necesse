package necesse.engine;

import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.platforms.Platform;
import necesse.engine.sound.GameMusic;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.ComparableSequence;

public class PlayingMusicManager {
   public static int DEFAULT_FADE_OUT_TIME = 2000;
   public static int DEFAULT_FADE_IN_TIME = 5000;
   public static int MAX_MILLIS_MUSIC_DELTA_OFFSET = 500;
   private PlayingMusicManager.PlayingMusic currentMusic;
   private final ArrayList<PlayingMusicManager.PlayingMusic> fadeOutMusic = new ArrayList<>();
   private AbstractMusicList currentMusicList;
   private ComparableSequence<Integer> nextMusicPriority;
   private boolean forceChangeMusic;

   public void clearNextMusic() {
      this.nextMusicPriority = null;
   }

   public void tick() {
      MusicOptionsOffset expectedMusic = null;
      String changeMusic = null;
      if (this.currentMusic == null
         || !this.currentMusic.player.isDone() && !(this.currentMusic.player.getSecondsLeft() < this.currentMusic.options.fadeOutMillis / 1000.0F)) {
         if (this.currentMusicList != null && this.nextMusicPriority != null) {
            expectedMusic = this.currentMusicList.getExpectedMusicPlaying();
            if (this.currentMusicList.shouldChangeTrack(this.currentMusic == null ? null : this.currentMusic.options)) {
               changeMusic = "changetrack";
            } else if (this.currentMusic != null && expectedMusic != null && this.currentMusic.options.music != expectedMusic.options.music) {
               changeMusic = "expectedwrong";
            }
         }
      } else {
         changeMusic = "ended";
      }

      if (this.forceChangeMusic) {
         changeMusic = "forcechanged";
         this.forceChangeMusic = false;
      }

      if (changeMusic != null) {
         MusicOptionsOffset nextMusic = expectedMusic;
         if (this.currentMusicList != null && nextMusic == null) {
            MusicOptions nextMusicOptions = this.currentMusicList.getNextMusic(this.currentMusic == null ? null : this.currentMusic.options);
            if (nextMusicOptions != null) {
               nextMusic = new MusicOptionsOffset(nextMusicOptions, -1, -1L);
            }
         }

         if (nextMusic != null) {
            GameLog.debug
               .println(
                  "Now playing "
                     + nextMusic.options.music.filePath
                     + " at "
                     + (int)(nextMusic.options.volume * 100.0F)
                     + "% volume fading in over "
                     + nextMusic.options.fadeInMillis
                     + "ms. Reason: "
                     + changeMusic
               );
            double skipSeconds = this.currentMusic == null
               ? 0.0
               : Math.max(this.currentMusic.options.fadeOutMillis / 1000.0F - this.currentMusic.player.getSecondsLeft(), 0.0);
            if (this.currentMusic != null) {
               GameLog.debug.println("Fading " + this.currentMusic.options.music.filePath + " out over " + this.currentMusic.options.fadeOutMillis + "ms");
               this.fadeOutMusic.add(this.currentMusic);
            }

            int previousFadeoutMaxMillis = nextMusic.options.previousFadeoutMaxMillis;
            if (previousFadeoutMaxMillis != -1) {
               this.fadeOutMusic.forEach(p -> p.options.fadeOutMillis = Math.min(p.options.fadeOutMillis, previousFadeoutMaxMillis));
            }

            this.currentMusic = new PlayingMusicManager.PlayingMusic(
               nextMusic.options, Platform.getSoundManager().createSoundPlayer(nextMusic.options.music.sound, SoundEffect.music())
            );
            this.currentMusic.player.effect.volume(0.0F);
            if (nextMusic.offset != -1L) {
               long lengthMillis = (long)((this.currentMusic.player.getLengthInSeconds() - this.currentMusic.options.fadeOutMillis / 1000.0F) * 1000.0);
               if (lengthMillis > 0L) {
                  long millisOffsetFromField = Math.floorMod(nextMusic.offset, lengthMillis);
                  long totalMillisOffset = Math.floorMod((long)(skipSeconds * 1000.0) + millisOffsetFromField, lengthMillis);
                  GameLog.debug.println("Offset music by " + (float)totalMillisOffset / 1000.0F + " seconds");
                  this.currentMusic.player.playSound((float)totalMillisOffset / 1000.0F);
               } else {
                  this.currentMusic.player.playSound();
               }
            } else {
               this.currentMusic.player.playSound();
            }
         } else {
            if (this.currentMusic != null) {
               GameLog.debug.println("Ended music and nothing else to play. Reason: " + changeMusic);
               this.fadeOutMusic.add(this.currentMusic);
            }

            this.currentMusic = null;
         }
      }

      if (this.nextMusicPriority != null && this.currentMusic != null && expectedMusic != null) {
         long expectedOffset = expectedMusic.offset;
         if (expectedOffset != -1L) {
            long lengthMillis = (long)((this.currentMusic.player.getLengthInSeconds() - this.currentMusic.options.fadeOutMillis / 1000.0F) * 1000.0);
            long millisOffsetFromField = Math.floorMod(expectedOffset, lengthMillis);
            long currentMusicOffsetMillis = (long)(this.currentMusic.player.getPositionSeconds() * 1000.0);
            long delta = expectedOffset - currentMusicOffsetMillis;
            if (Math.abs(delta) > MAX_MILLIS_MUSIC_DELTA_OFFSET) {
               long totalMillisOffset = Math.floorMod(millisOffsetFromField, lengthMillis);
               GameLog.debug.println("Adjusted music offset to " + (float)totalMillisOffset / 1000.0F + " seconds");
               this.currentMusic.player.setPosition((float)totalMillisOffset / 1000.0F);
            }
         }
      }

      if (this.currentMusic != null && this.currentMusic.player.effect.getVolume() < this.currentMusic.options.volume) {
         if (this.currentMusic.options.fadeInMillis > 0) {
            double increase = TickManager.getTickDelta(this.currentMusic.options.fadeInMillis);
            if (this.currentMusic.options.volume > 0.0F) {
               increase *= this.currentMusic.options.volume;
            }

            this.currentMusic.player.effect.volume(Math.min(this.currentMusic.player.effect.getVolume() + (float)increase, this.currentMusic.options.volume));
         } else {
            this.currentMusic.player.effect.volume(this.currentMusic.options.volume);
         }
      }

      for (int i = 0; i < this.fadeOutMusic.size(); i++) {
         PlayingMusicManager.PlayingMusic music = this.fadeOutMusic.get(i);
         if (music.options.fadeOutMillis > 0) {
            double decrease = TickManager.getTickDelta(music.options.fadeOutMillis);
            if (music.options.volume > 0.0F) {
               decrease *= music.options.volume;
            }

            music.player.effect.volume(music.player.effect.getVolume() - (float)decrease);
         } else {
            music.player.effect.volume(0.0F);
         }

         if (music.player.isDone() || music.player.effect.getVolume() <= 0.0F) {
            music.player.dispose();
            this.fadeOutMusic.remove(i);
            i--;
         }
      }

      if (this.currentMusic != null) {
         this.currentMusic.player.update();
      }

      this.fadeOutMusic.forEach(pm -> pm.player.update());
   }

   public void updateVolume() {
      if (this.currentMusic != null) {
         this.currentMusic.player.update();
      }

      this.fadeOutMusic.forEach(pm -> pm.player.update());
   }

   public int getPlayingCount() {
      return this.fadeOutMusic.size() + (this.currentMusic == null ? 0 : 1);
   }

   public GameMusic getCurrentMusic() {
      return this.currentMusic == null ? null : this.currentMusic.options.music;
   }

   public void dispose() {
      if (this.currentMusic != null) {
         this.currentMusic.player.dispose();
         this.currentMusic = null;
      }

      for (PlayingMusicManager.PlayingMusic fadingMusic : this.fadeOutMusic) {
         fadingMusic.player.dispose();
      }

      this.fadeOutMusic.clear();
   }

   public void forceChangeMusic() {
      this.forceChangeMusic = true;
   }

   public void setNextMusic(AbstractMusicList list, ComparableSequence<Integer> priority) {
      if (this.nextMusicPriority == null || this.nextMusicPriority.compareTo(priority) < 0) {
         this.currentMusicList = list;
         this.nextMusicPriority = priority;
      }
   }

   private static class PlayingMusic {
      public final MusicOptions options;
      public final SoundPlayer player;

      public PlayingMusic(MusicOptions options, SoundPlayer player) {
         this.options = options;
         this.player = player;
      }
   }
}
