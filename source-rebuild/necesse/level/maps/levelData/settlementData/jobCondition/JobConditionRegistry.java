package necesse.level.maps.levelData.settlementData.jobCondition;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ClassIDDataContainer;
import necesse.engine.registries.ClassedGameRegistry;

public class JobConditionRegistry extends ClassedGameRegistry<JobCondition, JobConditionRegistry.JobConditionRegistryElement> {
   public static final JobConditionRegistry instance = new JobConditionRegistry();

   public JobConditionRegistry() {
      super("JobCondition", 32767);
   }

   @Override
   public void registerCore() {
      registerJobCondition("doforever", DoForeverJobCondition.class, new LocalMessage("ui", "conditiondoforever"));
      registerJobCondition("doxtimes", DoXTimesJobCondition.class, new LocalMessage("ui", "conditiondocount", "count", "X"));
      registerJobCondition("dowhenthreshold", DoWhenThresholdsJobCondition.class, new LocalMessage("ui", "conditiondowhen", "condition", "X"));
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerJobCondition(String stringID, Class<? extends JobCondition> jobConditionClass, GameMessage listedMessage) {
      try {
         return instance.register(stringID, new JobConditionRegistry.JobConditionRegistryElement(jobConditionClass, listedMessage));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register job condition " + jobConditionClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static JobCondition getNewJobCondition(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static JobCondition getNewJobCondition(String stringID) {
      return getNewJobCondition(getJobConditionID(stringID));
   }

   public static int getJobConditionID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getJobConditionID(Class<? extends JobCondition> clazz) {
      return instance.getElementID(clazz);
   }

   public static GameMessage getJobConditionListedMessage(int id) {
      return id == -1 ? null : instance.getElement(id).listedMessage;
   }

   public static GameMessage getJobConditionListedMessage(String stringID) {
      return getJobConditionListedMessage(getJobConditionID(stringID));
   }

   public static boolean doesJobConditionExist(String stringID) {
      try {
         return instance.getElementIDRaw(stringID) >= 0;
      } catch (NoSuchElementException e) {
         return false;
      }
   }

   public static String getJobConditionStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static Stream<ClassIDDataContainer<JobCondition>> streamMobs() {
      return instance.streamElements().map(e -> (ClassIDDataContainer<JobCondition>)e);
   }

   public static List<ClassIDDataContainer<JobCondition>> getMobs() {
      return streamMobs().collect(Collectors.toList());
   }

   protected static class JobConditionRegistryElement extends ClassIDDataContainer<JobCondition> {
      public GameMessage listedMessage;

      public JobConditionRegistryElement(Class<? extends JobCondition> jobConditionClass, GameMessage listedMessage) throws NoSuchMethodException {
         super(jobConditionClass);
         this.listedMessage = listedMessage;
      }
   }
}
