package necesse.engine.commands.parameterHandlers;

import java.util.Collections;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class StringParameterHandler extends ParameterHandler<String> {
   private String defaultValue;
   private String[] autoCompletes;

   public StringParameterHandler(String defaultValue, String... autoCompletes) {
      this.defaultValue = defaultValue;
      this.autoCompletes = autoCompletes;
   }

   public StringParameterHandler() {
      this(null);
   }

   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      return this.autoCompletes != null && this.autoCompletes.length != 0
         ? autocompleteFromArray(this.autoCompletes, s -> true, s -> s, argument)
         : Collections.emptyList();
   }

   public String parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      return arg;
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return true;
   }

   public String getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return this.defaultValue;
   }
}
