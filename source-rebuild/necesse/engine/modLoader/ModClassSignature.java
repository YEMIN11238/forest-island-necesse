package necesse.engine.modLoader;

import java.util.Arrays;
import java.util.Objects;

public class ModClassSignature {
   public final Class<?> targetClass;
   public final String targetMethod;
   public final Class<?>[] arguments;
   public final boolean checkCLInit;

   public static ModClassSignature constructor(Class<?> clazz, Class<?>... arguments) {
      return new ModClassSignature(clazz, "<init>", arguments);
   }

   public static ModClassSignature method(Class<?> clazz, String methodName, Class<?>... arguments) {
      return new ModClassSignature(clazz, methodName, arguments);
   }

   public static ModClassSignature allMethodsWithName(Class<?> clazz, String methodName) {
      return new ModClassSignature(clazz, methodName, null);
   }

   public static ModClassSignature allMethodsWithName(String methodName) {
      return new ModClassSignature(null, methodName, null);
   }

   public static ModClassSignature allInClass(Class<?> clazz) {
      return new ModClassSignature(clazz, null, null);
   }

   public ModClassSignature(Class<?> targetClass, String targetMethod, Class<?>[] arguments) {
      this.targetClass = targetClass;
      this.targetMethod = targetMethod;
      this.arguments = arguments;
      this.checkCLInit = targetMethod != null && targetMethod.equals("<init>");
   }

   public boolean matches(ModClassSignature other) {
      if (this.targetClass == null || other.targetClass == null || this.targetClass == other.targetClass) {
         if (this.targetMethod == null || other.targetMethod == null) {
            return true;
         }

         if (this.targetMethod.equals(other.targetMethod)) {
            if (this.arguments != null && other.arguments != null) {
               return Arrays.equals(this.arguments, other.arguments);
            }

            return true;
         }
      }

      return false;
   }

   public boolean matches(StackTraceElement e) {
      if (this.targetClass.getName().equals(e.getClassName())) {
         if (this.targetMethod == null) {
            return true;
         } else {
            return this.checkCLInit && "<clinit>".equals(e.getMethodName()) ? true : this.targetMethod.equals(e.getMethodName());
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ModClassSignature that = (ModClassSignature)o;
         return Objects.equals(this.targetClass, that.targetClass)
            && Objects.equals(this.targetMethod, that.targetMethod)
            && Arrays.equals(this.arguments, that.arguments);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.targetClass, this.targetMethod, Arrays.hashCode(this.arguments));
   }
}
