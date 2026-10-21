package necesse.engine.registries;

import java.util.stream.Stream;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.job.JobType;

public class JobTypeRegistry extends StaticObjectGameRegistry<JobType> {
   public static final JobTypeRegistry instance = new JobTypeRegistry();
   public static int needsID;
   public static int recreationID;

   private JobTypeRegistry() {
      super("JobType", 32766);
   }

   @Override
   public void registerCore() {
      recreationID = registerType("recreation", new JobType(false, false, null, null));
      needsID = registerType("needs", new JobType(false, false, null, null));
      registerType("expeditions", new JobType(false, false, null, null));
      registerType("tradingmission", new JobType(false, true, null, null));
      registerType("hauling", new JobType(true, false, new LocalMessage("jobs", "haulingname"), new LocalMessage("jobs", "haulingtip")));
      registerType("crafting", new JobType(true, false, new LocalMessage("jobs", "craftingname"), new LocalMessage("jobs", "craftingtip")));
      registerType("forestry", new JobType(true, false, new LocalMessage("jobs", "forestryname"), new LocalMessage("jobs", "forestrytip")));
      registerType("farming", new JobType(true, false, new LocalMessage("jobs", "farmingname"), new LocalMessage("jobs", "farmingtip")));
      registerType("fertilize", new JobType(true, true, new LocalMessage("jobs", "fertilizename"), new LocalMessage("jobs", "fertilizetip")));
      registerType("husbandry", new JobType(true, true, new LocalMessage("jobs", "husbandryname"), new LocalMessage("jobs", "husbandrytip")));
      registerType("fishing", new JobType(true, true, new LocalMessage("jobs", "fishingname"), null));
      registerType("hunting", new JobType(true, true, new LocalMessage("jobs", "huntingname"), new LocalMessage("jobs", "huntingtip")));
   }

   protected void onRegister(JobType object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
      for (JobType element : this.getElements()) {
         element.onJobTypeRegistryClosed();
      }
   }

   public static int registerType(String stringID, JobType type) {
      return instance.register(stringID, type);
   }

   public static JobType getJobType(int id) {
      return instance.getElement(id);
   }

   public static JobType getJobType(String stringID) {
      return instance.getElement(stringID);
   }

   public static int getJobTypeID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static Iterable<JobType> getTypes() {
      return instance.getElements();
   }

   public static Stream<JobType> streamTypes() {
      return instance.streamElements();
   }
}
