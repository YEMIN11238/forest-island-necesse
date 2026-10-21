package necesse.engine.commands.parameterHandlers;

import java.util.List;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.CmdArgument;
import necesse.engine.commands.CmdParameter;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.inventory.item.Item;

public class ArmorSetParameterHandler extends ParameterHandler<Item> {
   @Override
   public List<AutoComplete> autocomplete(Client client, Server server, ServerClient serverClient, CmdArgument argument) {
      return autocompleteFromArray(ItemRegistry.getItems().toArray(new Item[0]), i -> this.canSuggestItem(client, i), Item::getStringID, argument);
   }

   protected boolean canSuggestItem(Client client, Item i) {
      boolean canSuggest = i.isArmorItem();
      return client.worldSettings.creativeMode
         ? canSuggest && ItemRegistry.isObtainableInCreative(i.getID())
         : canSuggest && ItemRegistry.isObtainable(i.getID());
   }

   public Item parse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) throws IllegalArgumentException {
      Item out = ItemRegistry.getItem(arg);
      if (out == null) {
         throw new IllegalArgumentException("Could not find item with stringID \"" + arg + "\" for <" + parameter.name + ">");
      } else {
         return out;
      }
   }

   @Override
   public boolean tryParse(Client client, Server server, ServerClient serverClient, String arg, CmdParameter parameter) {
      return !this.autocomplete(client, server, serverClient, new CmdArgument(parameter, arg, 1)).isEmpty();
   }

   public Item getDefault(Client client, Server server, ServerClient serverClient, CmdParameter parameter) {
      return null;
   }
}
