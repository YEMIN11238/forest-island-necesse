package necesse.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RestData {
   public final HashMap<String, String> data = new HashMap<>();
   public final ArrayList<Throwable> getErrors = new ArrayList<>();

   protected String getString(Supplier<Object> supplier) {
      return this.getString(supplier, e -> "ERR: " + e.getClass().getSimpleName());
   }

   protected String getString(Supplier<Object> supplier, String errReturn) {
      return this.getString(supplier, e -> errReturn);
   }

   protected String getString(Supplier<Object> supplier, Function<Throwable, Object> errReturn) {
      try {
         return String.valueOf(supplier.get());
      } catch (Exception | Error var6) {
         Throwable e1 = var6;
         this.getErrors.add(var6);

         try {
            return String.valueOf(errReturn.apply(e1));
         } catch (Exception | Error e2) {
            this.getErrors.add(e2);
            return "ERR_RETURN";
         }
      }
   }

   protected <T> T getObject(Supplier<T> supplier, T errReturn) {
      try {
         return supplier.get();
      } catch (Exception | Error e1) {
         this.getErrors.add(e1);
         return errReturn;
      }
   }

   protected <T> void arrayObjects(Supplier<T[]> arraySupplier, BiFunction<T, Integer, Boolean> forEach, Consumer<Integer> count) {
      this.listObjects(() -> Arrays.asList((T[])((Object[])arraySupplier.get())), forEach, count);
   }

   protected <T> void streamObjects(Supplier<Stream<T>> streamSupplier, BiFunction<T, Integer, Boolean> forEach, Consumer<Integer> count) {
      this.listObjects(() -> streamSupplier.get().collect(Collectors.toList()), forEach, count);
   }

   protected <T> void listObjects(Supplier<Iterable<T>> iterableSupplier, BiFunction<T, Integer, Boolean> forEach, Consumer<Integer> count) {
      int counter = 0;

      try {
         for (T value : iterableSupplier.get()) {
            try {
               if (forEach.apply(value, counter)) {
                  counter++;
               }
            } catch (Exception e) {
               this.getErrors.add(e);
            }
         }
      } catch (Exception | Error e) {
         this.getErrors.add(e);
      }

      count.accept(counter);
   }

   protected <T> void addArray(String totalKey, String keyPrefix, Supplier<T[]> arraySupplier, HashMap<String, String> data, Function<T, String> mapper) {
      this.addList(totalKey, keyPrefix, () -> Arrays.asList((T[])((Object[])arraySupplier.get())), data, mapper);
   }

   protected <T> void addStream(String totalKey, String keyPrefix, Supplier<Stream<T>> streamSupplier, HashMap<String, String> data, Function<T, String> mapper) {
      this.addList(totalKey, keyPrefix, () -> streamSupplier.get().collect(Collectors.toList()), data, mapper);
   }

   protected <T> void addList(
      String totalKey, String keyPrefix, Supplier<Iterable<T>> iterableSupplier, HashMap<String, String> data, Function<T, String> mapper
   ) {
      this.listObjects(iterableSupplier, (v, i) -> {
         String str = mapper.apply(v);
         if (str == null) {
            return false;
         }

         data.put(keyPrefix + i, str);
         return true;
      }, total -> data.put(totalKey, Integer.toString(total)));
   }
}
