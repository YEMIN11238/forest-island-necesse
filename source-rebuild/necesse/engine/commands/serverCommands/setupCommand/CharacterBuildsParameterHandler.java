package necesse.engine.commands.serverCommands.setupCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.parameterHandlers.ParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameUtils;

public class CharacterBuildsParameterHandler extends ParameterHandler<CharacterBuilds> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      String[] args = argument.arg.split(" ");
      if (argument.arg.endsWith(" ")) {
         args = GameUtils.concat(args, new String[]{""});
      }

      LinkedList<String> remainingBuilds = new LinkedList<>(DemoServerCommand.builds.keySet());

      for (int i = 0; i < args.length; i++) {
         String currentArg = args[i];
         ListIterator<String> iterator = remainingBuilds.listIterator();
         boolean success = false;

         while (iterator.hasNext()) {
            String next = iterator.next();
            if (next.equals(currentArg)) {
               if (i == args.length - 1) {
                  return Collections.singletonList(new AutoComplete(1, next, true));
               }

               iterator.remove();
               success = true;
               break;
            }
         }

         if (i < args.length - 1 && !success) {
            return Collections.emptyList();
         }

         if (!success) {
            List<AutoComplete> nextCompletes = autocompleteFromArray(
               (String[])remainingBuilds.toArray(new String[0]), null, null, new CmdArgument(argument.param, currentArg, argument.argCount)
            );
            ArrayList<AutoComplete> finalCompletes = new ArrayList<>(nextCompletes.size());

            for (AutoComplete next : nextCompletes) {
               finalCompletes.add(new AutoComplete(1, next.newArgs, true));
            }

            return finalCompletes;
         }
      }

      return Collections.emptyList();
   }

   public CharacterBuilds parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      ArrayList<CharacterBuildEntry> out = new ArrayList<>();
      String[] args = arg.split(" ");

      for (String currentArg : args) {
         boolean success = false;

         for (Entry<String, CharacterBuild> entry : DemoServerCommand.builds.entrySet()) {
            if (entry.getKey().equals(currentArg)) {
               out.add(new CharacterBuildEntry(entry.getKey(), entry.getValue()));
               success = true;
               break;
            }
         }

         if (!success) {
            throw new IllegalArgumentException("Could not find build with name \"" + currentArg + "\" for <" + parameter.name + ">");
         }
      }

      return new CharacterBuilds(out.toArray(new CharacterBuildEntry[0]));
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public CharacterBuilds getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return new CharacterBuilds();
   }

   @Override
   public int getArgsUsed() {
      return 10000;
   }
}
