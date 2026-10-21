package necesse.engine.commands.parameterHandlers;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.localization.Language;
import necesse.engine.localization.Localization;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;

public class LanguageParameterHandler extends ParameterHandler<Language> {
   private Language defaultValue;

   public LanguageParameterHandler() {
      this.defaultValue = null;
   }

   public LanguageParameterHandler(Language defaultValue) {
      this.defaultValue = defaultValue;
   }

   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      List<AutoComplete> out = new ArrayList<>();
      out.addAll(autocompleteFromArray(Localization.getLanguages(), null, l -> l.localDisplayName, argument));
      out.addAll(autocompleteFromArray(Localization.getLanguages(), null, l -> l.stringID, argument));
      return out;
   }

   public Language parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      Language[] values = Localization.getLanguages();

      for (Language language : values) {
         if (arg.equalsIgnoreCase(language.localDisplayName)) {
            return language;
         }

         if (arg.equalsIgnoreCase(language.englishDisplayName)) {
            return language;
         }

         if (arg.equalsIgnoreCase(language.stringID)) {
            return language;
         }
      }

      throw new IllegalArgumentException("Could not find language \"" + arg + "\" for <" + parameter.name + ">");
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public Language getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return this.defaultValue;
   }
}
