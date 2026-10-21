package necesse.engine.registries;

import java.lang.reflect.InvocationTargetException;
import necesse.entity.projectile.modifiers.ProjectileModifier;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;

public class ProjectileModifierRegistry extends ClassedGameRegistry<ProjectileModifier, ProjectileModifierRegistry.ProjectileModifierRegistryElement> {
   public static final ProjectileModifierRegistry instance = new ProjectileModifierRegistry();

   private ProjectileModifierRegistry() {
      super("ProjectileModifier", 32762);
   }

   @Override
   public void registerCore() {
      registerModifier("resilienceonhit", ResilienceOnHitProjectileModifier.class);
   }

   @Override
   protected void onRegistryClose() {
   }

   public static int registerModifier(String stringID, Class<? extends ProjectileModifier> modifierClass) {
      try {
         return instance.register(stringID, new ProjectileModifierRegistry.ProjectileModifierRegistryElement(modifierClass));
      } catch (NoSuchMethodException e) {
         System.err.println("Could not register ProjectileModifier " + modifierClass.getSimpleName() + ": Missing constructor with no parameters");
         return -1;
      }
   }

   public static ProjectileModifier getModifier(int id) {
      try {
         return instance.getElement(id).newInstance();
      } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static ProjectileModifier getModifier(String stringID) {
      return getModifier(getModifierID(stringID));
   }

   public static int getModifierID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static int getModifierID(Class<? extends ProjectileModifier> clazz) {
      return instance.getElementID(clazz);
   }

   public static String getModifierStringID(int id) {
      return instance.getElementStringID(id);
   }

   protected static class ProjectileModifierRegistryElement extends ClassIDDataContainer<ProjectileModifier> {
      public ProjectileModifierRegistryElement(Class<? extends ProjectileModifier> modifierClass) throws NoSuchMethodException {
         super(modifierClass);
      }
   }
}
