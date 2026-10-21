package necesse.engine.commands.serverCommands;

import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.commands.CmdParameter;
import necesse.engine.commands.CommandLog;
import necesse.engine.commands.ModularChatCommand;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.commands.parameterHandlers.ItemParameterHandler;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ItemRegistry;
import necesse.inventory.item.Item;
import necesse.inventory.recipe.RecipeBrokerValueCompute;
import necesse.inventory.recipe.Recipes;

public class CalculateRecipeValueClientCommand extends ModularChatCommand {
   public CalculateRecipeValueClientCommand() {
      super(
         "calculatevalue",
         "Calculates the broker value of an item based on recipes",
         PermissionLevel.USER,
         false,
         new CmdParameter("item", new ItemParameterHandler())
      );
   }

   @Override
   public void runModular(Client client, Server server, ServerClient serverClient, Object[] args, String[] errors, CommandLog logs) {
      Item item = (Item)args[0];
      AtomicReference<Float> foundValue = new AtomicReference<>(null);
      RecipeBrokerValueCompute compute = Recipes.getDefaultBrokerValueCompute();
      compute.addItem(item.getID(), 1.0F);
      compute.calculate((itemID, brokerValue) -> {
         if (itemID == item.getID()) {
            foundValue.set(brokerValue);
         }
      });
      if (foundValue.get() == null) {
         logs.add("Could not calculate broker value for " + ItemRegistry.getDisplayName(item.getID()));
      } else {
         logs.add("Broker value for item " + ItemRegistry.getDisplayName(item.getID()) + " is: " + foundValue.get());
      }
   }
}
