package necesse.engine;

public abstract class AbstractMusicList {
   public abstract MusicOptions getNextMusic(MusicOptions var1);

   public abstract MusicOptionsOffset getExpectedMusicPlaying();

   public abstract boolean shouldChangeTrack(MusicOptions var1);

   public abstract Iterable<MusicOptions> getMusicInList();
}
