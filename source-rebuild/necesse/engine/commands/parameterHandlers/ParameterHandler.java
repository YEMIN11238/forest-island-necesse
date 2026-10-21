package necesse.engine.commands.parameterHandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public abstract class ParameterHandler<T> {
   public abstract List<AutoComplete> autocomplete(Client var1, Server var2, ServerClient var3, CmdArgument var4);

   public abstract T parse(Client var1, Server var2, ServerClient var3, String var4, CmdParameter var5) throws IllegalArgumentException;

   public abstract boolean tryParse(Client var1, Server var2, ServerClient var3, String var4, CmdParameter var5);

   public abstract T getDefault(Client var1, Server var2, ServerClient var3, CmdParameter var4);

   public int getArgsUsed() {
      return 1;
   }

   public static <T> List<AutoComplete> autocompleteFromList(List<T> list, Function<T, Boolean> filter, Function<T, String> strGetter, CmdArgument argument) {
      return autocompleteFromCollection(list, filter, strGetter, argument);
   }

   public static <T> List<AutoComplete> autocompleteFromSet(Set<T> list, Function<T, Boolean> filter, Function<T, String> strGetter, CmdArgument argument) {
      return autocompleteFromCollection(list, filter, strGetter, argument);
   }

   public static <T> List<AutoComplete> autocompleteFromCollection(
      Collection<T> list, Function<T, Boolean> filter, Function<T, String> strGetter, CmdArgument argument
   ) {
      return autocompleteFromArray((T[])list.toArray(), filter, strGetter, argument);
   }

   public static <T> List<AutoComplete> autocompleteFromArray(T[] array, Function<T, Boolean> filter, Function<T, String> strGetter, CmdArgument argument) {
      if (filter == null) {
         filter = T -> true;
      }

      if (strGetter == null) {
         strGetter = Object::toString;
      }

      if (argument.arg.length() == 0) {
         ArrayList<AutoComplete> possibilities = new ArrayList<>();

         for (T e : array) {
            if (e != null && filter.apply(e)) {
               AutoComplete add = new AutoComplete(argument.argCount, strGetter.apply(e));
               if (!possibilities.contains(add)) {
                  possibilities.add(add);
               }
            }
         }

         return possibilities;
      } else {
         ArrayList<AutoComplete> possibilities = new ArrayList<>();

         for (T e : array) {
            if (e != null && filter.apply(e)) {
               String str = strGetter.apply(e);
               if (str.toLowerCase().startsWith(argument.arg.toLowerCase())) {
                  AutoComplete add = new AutoComplete(argument.argCount, str);
                  if (!possibilities.contains(add)) {
                     possibilities.add(add);
                  }
               }
            }
         }

         for (T e : array) {
            if (e != null && filter.apply(e)) {
               String str = strGetter.apply(e);
               AutoComplete add = new AutoComplete(argument.argCount, str);
               if (!possibilities.contains(add) && str.toLowerCase().contains(argument.arg.toLowerCase())) {
                  possibilities.add(add);
               }
            }
         }

         return possibilities;
      }
   }
}
