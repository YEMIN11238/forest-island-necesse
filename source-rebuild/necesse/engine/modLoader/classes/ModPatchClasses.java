package necesse.engine.modLoader.classes;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModClassSignature;
import necesse.engine.modLoader.ModLoadException;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.annotations.ModConstructorPatch;
import necesse.engine.modLoader.annotations.ModCustomPatch;
import necesse.engine.modLoader.annotations.ModCustomPatchMethod;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameUtils;
import net.bytebuddy.agent.builder.AgentBuilder.Default;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.Listener;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.matcher.ElementMatchers;

public class ModPatchClasses extends ModClass {
   private final List<Class<?>> modClasses = new LinkedList<>();
   private LoadedMod mod;

   @Override
   public boolean shouldRegisterModClass(Class<?> var1) {
      return var1.isAnnotationPresent(ModMethodPatch.class)
         || var1.isAnnotationPresent(ModConstructorPatch.class)
         || var1.isAnnotationPresent(ModCustomPatch.class);
   }

   @Override
   public void registerModClass(LoadedMod var1, Class<?> var2) throws ModLoadException {
      this.modClasses.add(var2);
   }

   @Override
   public void finalizeLoading(LoadedMod var1) throws ModLoadException {
      this.mod = var1;
   }

   public void applyPatches(ComputedValue<Instrumentation> var1, HashSet<ModClassSignature> var2) throws ModLoadException {
      LinkedList<ModPatchClasses.PrioritizedPatchClass> var3 = new LinkedList<>();

      for (Class<?> var5 : this.modClasses) {
         try {
            if (var5.isAnnotationPresent(ModMethodPatch.class)) {
               ModMethodPatch var20 = var5.getAnnotation(ModMethodPatch.class);
               String var24 = Arrays.stream(var20.arguments()).map(Class::getSimpleName).collect(Collectors.joining(", "));
               String var27 = "." + var20.name() + "(" + var24 + ")";
               GameUtils.insertSortedList(
                  var3,
                  new ModPatchClasses.PrioritizedPatchClass(
                     var5,
                     var20.target(),
                     var20.name(),
                     var20.arguments(),
                     var20.priority(),
                     var27,
                     var2x -> var2x.visit(
                        Advice.to(var5)
                           .on(ElementMatchers.isMethod().and(ElementMatchers.named(var20.name())).and(ElementMatchers.takesArguments(var20.arguments())))
                     )
                  ),
                  Comparator.comparingInt(var0 -> -var0.priority)
               );
            } else if (var5.isAnnotationPresent(ModConstructorPatch.class)) {
               ModConstructorPatch var19 = var5.getAnnotation(ModConstructorPatch.class);
               String var23 = Arrays.stream(var19.arguments()).map(Class::getSimpleName).collect(Collectors.joining(", "));
               String var26 = ".<constructor>(" + var23 + ")";
               GameUtils.insertSortedList(
                  var3,
                  new ModPatchClasses.PrioritizedPatchClass(
                     var5,
                     var19.target(),
                     "<init>",
                     var19.arguments(),
                     var19.priority(),
                     var26,
                     var2x -> var2x.visit(Advice.to(var5).on(ElementMatchers.isConstructor().and(ElementMatchers.takesArguments(var19.arguments()))))
                  ),
                  Comparator.comparingInt(var0 -> -var0.priority)
               );
            } else if (var5.isAnnotationPresent(ModCustomPatch.class)) {
               ModCustomPatch var6 = var5.getAnnotation(ModCustomPatch.class);
               boolean var22 = false;
               Method[] var8 = var5.getDeclaredMethods();
               int var9 = var8.length;
               int var10 = 0;

               while (true) {
                  if (var10 < var9) {
                     Method var11 = var8[var10];
                     if (!var11.isAnnotationPresent(ModCustomPatchMethod.class)
                        || var11.getParameterCount() != 1
                        || var11.getParameterTypes()[0] != Advice.class
                        || var11.getReturnType() != AsmVisitorWrapper.class) {
                        var10++;
                        continue;
                     }

                     GameUtils.insertSortedList(
                        var3, new ModPatchClasses.PrioritizedPatchClass(var5, var6.target(), null, null, var6.priority(), " with custom patch", var2x -> {
                           try {
                              return var2x.visit((AsmVisitorWrapper)var11.invoke(null, Advice.to(var5)));
                           } catch (IllegalAccessException | InvocationTargetException var4) {
                              throw new ModPatchClasses.PatchBuilderException(var4);
                           }
                        }), Comparator.comparingInt(var0 -> -var0.priority)
                     );
                     var22 = true;
                  }

                  if (!var22) {
                     throw new ModLoadException(
                        this.mod,
                        "@ModCustomPatch must have a static method annotated with @ModCustomPatchMethod taking an Advice.class parameter and returning an AsmVisitorWrapper object"
                     );
                  }
                  break;
               }
            }
         } catch (ModLoadException var14) {
            throw var14;
         } catch (TypeNotPresentException var15) {
            System.err.println("Error loading mod patch class \"" + var5.getName() + "\" from likely outdated mod: " + this.mod.getModNameString());
            String var7 = var15.getMessage();
            if (var7 != null && !var7.isEmpty()) {
               System.err.println("\t" + var7);
            } else {
               var15.printStackTrace();
            }
         } catch (Exception var16) {
            throw new ModLoadException(this.mod, "Error loading mod patch class: " + var5.getName(), var16);
         }
      }

      for (ModPatchClasses.PrioritizedPatchClass var18 : var3) {
         ModClassSignature var21 = new ModClassSignature(var18.targetClass, var18.targetMethod, var18.methodArguments);
         if (ModLoader.isIllegalModPatch(var21)) {
            System.err.println(this.mod.id + " tried to perform patch on illegal signature: " + var18.targetClass.getName() + var18.callName);
         } else {
            try {
               AtomicReference<Exception> var25 = new AtomicReference<>();
               final AtomicInteger var28 = new AtomicInteger();
               new Default().disableClassFormatChanges().with(RedefinitionStrategy.RETRANSFORMATION).with(new Listener() {
                  public void onBatch(int var1, List<Class<?>> var2x, List<Class<?>> var3) {
                  }

                  public Iterable<? extends List<Class<?>>> onError(int var1, List<Class<?>> var2x, Throwable var3, List<Class<?>> var4) {
                     return null;
                  }

                  public void onComplete(int var1, List<Class<?>> var2x, Map<List<Class<?>>, Throwable> var3) {
                     var28.addAndGet(var1);
                  }
               }).type(ElementMatchers.is(var18.targetClass)).transform((var2x, var3x, var4, var5x) -> {
                  try {
                     return var18.builderFunc.apply(var2x);
                  } catch (Exception var7) {
                     var25.set(var7);
                     return var2x;
                  }
               }).installOn(var1.get());
               var1.get().retransformClasses(this.modClasses.toArray(new Class[1]));
               if (var25.get() != null) {
                  Throwable var29 = var25.get();
                  if (var29 instanceof ModPatchClasses.PatchBuilderException) {
                     var29 = var29.getCause();
                     if (var29 instanceof InvocationTargetException) {
                        var29 = var29.getCause();
                     }
                  }

                  throw new ModLoadException(this.mod, "Error performing mod patch transformation", var29);
               }

               if (var28.get() > 0) {
                  System.out.println(this.mod.id + " transformed " + var18.targetClass.getName() + var18.callName);
                  var2.add(var21);
               } else {
                  GameLog.warn
                     .println(
                        this.mod.id
                           + " advice class \""
                           + var18.adviceClass.getName()
                           + "\" did not complete any patches. Are you sure you're targeting the right class and method?"
                     );
               }
            } catch (ModLoadException var12) {
               throw var12;
            } catch (Exception var13) {
               throw new ModLoadException(this.mod, "Error loading mod patch builder: " + var18.adviceClass.getName(), var13);
            }
         }
      }
   }

   private static class PatchBuilderException extends RuntimeException {
      public PatchBuilderException(Throwable var1) {
         super(var1);
      }
   }

   private static class PrioritizedPatchClass {
      public final Class<?> adviceClass;
      public final Class<?> targetClass;
      public final String targetMethod;
      public final Class<?>[] methodArguments;
      public final int priority;
      public final String callName;
      public final Function<Builder<?>, Builder<?>> builderFunc;

      public PrioritizedPatchClass(Class<?> var1, Class<?> var2, String var3, Class<?>[] var4, int var5, String var6, Function<Builder<?>, Builder<?>> var7) {
         this.adviceClass = var1;
         this.targetClass = var2;
         this.targetMethod = var3;
         this.methodArguments = var4;
         this.priority = var5;
         this.callName = var6;
         this.builderFunc = var7;
      }
   }
}
