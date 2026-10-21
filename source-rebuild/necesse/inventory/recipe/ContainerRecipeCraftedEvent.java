package necesse.inventory.recipe;

import java.util.ArrayList;
import necesse.inventory.InventoryItemsRemoved;
import necesse.inventory.container.Container;

public class ContainerRecipeCraftedEvent extends RecipeCraftedEvent {
   public final Container container;

   public ContainerRecipeCraftedEvent(Recipe recipe, ArrayList<InventoryItemsRemoved> itemsUsed, Container container) {
      super(recipe, itemsUsed);
      this.container = container;
   }
}
