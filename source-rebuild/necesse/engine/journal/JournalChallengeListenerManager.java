package necesse.engine.journal;

import java.util.ArrayList;
import java.util.stream.Stream;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapArrayList;

public class JournalChallengeListenerManager {
   protected Class<?>[] listenerClasses;
   protected HashMapArrayList<Class<?>, JournalChallenge> listenerToChallenges;

   public JournalChallengeListenerManager(Class<?>... listenerClasses) {
      this.listenerClasses = listenerClasses;
      this.listenerToChallenges = new HashMapArrayList<>();
   }

   public void addChallenge(JournalChallenge challenge) {
      for (Class<?> listenerClass : this.listenerClasses) {
         if (listenerClass.isAssignableFrom(challenge.getClass())) {
            this.listenerToChallenges.add(listenerClass, challenge);
         }
      }
   }

   public <T> Iterable<T> getChallenges(Class<T> listenerClass) {
      ArrayList<JournalChallenge> journalChallenges = this.listenerToChallenges.get(listenerClass);
      return GameUtils.mapIterable(journalChallenges.iterator(), o -> (T)o);
   }

   public <T> Stream<T> streamChallenges(Class<T> listenerClass) {
      ArrayList<JournalChallenge> journalChallenges = this.listenerToChallenges.get(listenerClass);
      return journalChallenges.stream().map(o -> (T)o);
   }
}
