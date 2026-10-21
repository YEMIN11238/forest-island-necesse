package necesse.engine.commands.parameterHandlers;

import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.registries.IDDataContainer;
import necesse.inventory.enchants.ItemEnchantment;

public class EnchantmentParameterHandler extends ParameterHandler<ItemEnchantment> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      return autocompleteFromList(EnchantmentRegistry.getEnchantments(), e -> e.getID() != 0, IDDataContainer::getStringID, argument);
   }

   public ItemEnchantment parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      ItemEnchantment out = EnchantmentRegistry.getEnchantment(arg);
      if (out == null) {
         throw new IllegalArgumentException("Could not find enchantment with stringID \"" + arg + "\" for <" + parameter.name + ">");
      } else {
         return out;
      }
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public ItemEnchantment getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
