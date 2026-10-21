package necesse.engine.modLoader.classes;

import java.lang.reflect.InvocationTargetException;
import necesse.engine.GameLoadingScreen;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModLoadException;
import necesse.engine.modLoader.ModMethod;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.modLoader.annotations.ModEntry;

public class EntryClass extends SingleModClass {
   private LoadedMod mod;
   private ModMethod preInit;
   private ModMethod init;
   private ModMethod initResources;
   private ModMethod postInit;
   private ModMethod initSettings;
   private ModMethod dispose;

   public EntryClass() {
      super(true);
   }

   @Override
   public boolean shouldRegisterModClass(Class<?> modClass) {
      return modClass.isAnnotationPresent(ModEntry.class);
   }

   @Override
   public String getErrorName() {
      return "@ModEntry annotation";
   }

   @Override
   public void finalizeSingleLoading(LoadedMod mod) throws ModLoadException {
      this.mod = mod;

      try {
         Class<?> modClass = this.getModClass();
         Object instance = modClass.getDeclaredConstructor().newInstance();
         this.preInit = new ModMethod(mod, ModEntry.class, instance, modClass, "preInit");
         this.init = new ModMethod(mod, ModEntry.class, instance, modClass, "init");
         this.initResources = new ModMethod(mod, ModEntry.class, instance, modClass, "initResources");
         this.postInit = new ModMethod(mod, ModEntry.class, instance, modClass, "postInit");
         this.initSettings = new ModMethod(mod, ModEntry.class, instance, modClass, "initSettings");
         if (this.initSettings.foundMethod() && !ModSettings.class.isAssignableFrom(this.initSettings.getReturnType())) {
            throw new ModLoadException(mod, "initSettings must return a ModSettings class");
         }

         this.dispose = new ModMethod(mod, ModEntry.class, instance, modClass, "dispose");
      } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
         throw new ModLoadException(mod, "Error loading ModEntry class", e);
      }
   }

   public void preInit() {
      GameLoadingScreen.drawLoadingSub(this.mod.name);
      this.preInit.invoke();
   }

   public void init() {
      GameLoadingScreen.drawLoadingSub(this.mod.name);
      this.init.invoke();
   }

   public void initResources() {
      GameLoadingScreen.drawLoadingSub(this.mod.name);
      this.initResources.invoke();
   }

   public void postInit() {
      GameLoadingScreen.drawLoadingSub(this.mod.name);
      this.postInit.invoke();
   }

   public ModSettings initSettings() {
      GameLoadingScreen.drawLoadingSub(this.mod.name);
      return (ModSettings)this.initSettings.invoke();
   }

   public void dispose() {
      this.dispose.invoke();
   }
}
