package necesse.gfx.forms.presets.containerComponent.object;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.client.Client;
import necesse.engine.util.MultiTickRunnable;
import necesse.engine.util.ObjectValue;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.ContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.object.CartographersTableContainer;

public class CartographersTableForm<T extends CartographersTableContainer> extends ContainerForm<T> {
   private final HashSet<Integer> currentShowingSlots = new HashSet<>();
   private final HashMap<NetworkClient, FormTextButton> clientButtons = new HashMap<>();
   private final FormContentBox content;
   private int contentHeight;

   public CartographersTableForm(Client client, T container) {
      super(client, 400, 135, container);
      FormFlow flow = new FormFlow(4);
      this.addComponent(
         (FormLocalLabel)flow.nextY(new FormLocalLabel("ui", "cartographerselect", new FontOptions(20), 0, this.getWidth() / 2, 0, this.getWidth() - 8), 10)
      );
      this.content = this.addComponent(flow.nextY(new FormContentBox(0, 0, this.getWidth(), 200)));
      this.updateContent();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (!this.container.tableObjectEntity.hasSameUsingSlots(this.currentShowingSlots)) {
         this.updateContent();
      }

      this.clientButtons.forEach((client, button) -> {
         MultiTickRunnable sendTask = this.container.sendTasks.get(client.slot);
         if (sendTask != null) {
            button.setText(String.format("%.0f%%", sendTask.getProgressPercent() * 100.0F));
            button.startCooldown(5000);
         } else if (button.isOnCooldown()) {
            button.setText("100%");
         } else {
            button.setText(client.getName());
         }
      });
      super.draw(tickManager, perspective, renderBox);
   }

   public void updateContent() {
      this.currentShowingSlots.clear();
      this.clientButtons.clear();
      this.content.clearComponents();
      FormFlow flow = new FormFlow(4);
      boolean addedAny = false;
      int buttonWidth = Math.min(300, this.content.getWidth() - 20);
      int buttonXOffset = (this.content.getWidth() - buttonWidth) / 2;

      for (ObjectValue<Integer, NetworkClient> client : this.container.tableObjectEntity.getOptionalUsingClients()) {
         this.currentShowingSlots.add(client.object);
         if (client.value != null && this.client.getSlot() != client.object) {
            FormTextButton button = this.content
               .addComponent(flow.nextY(new FormTextButton(client.value.getName(), buttonXOffset, 0, buttonWidth, FormInputSize.SIZE_24, ButtonColor.BASE), 4));
            button.onClicked(
               e -> this.container.shareWithPlayer.sendAllMapDataOverMultiplePackets(client.object, this.client.levelManager.loadDimensionsMapData())
            );
            this.clientButtons.put(client.value, button);
            addedAny = true;
         }
      }

      if (!addedAny) {
         flow.next(10);
         this.content
            .addComponent(
               flow.nextY(
                  new FormLocalLabel("ui", "cartographerselectnone", new FontOptions(16), 0, this.content.getWidth() / 2, 0, this.content.getWidth() - 20)
               )
            );
         flow.next(20);
      }

      this.contentHeight = Math.max(flow.next(), 50);
      this.content.setContentBox(new Rectangle(this.content.getWidth(), flow.next()));
      this.updateSize();
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this);
   }

   public void updateSize() {
      this.content.setHeight(Math.min(this.contentHeight, 200));
      this.setHeight(this.content.getY() + this.content.getHeight());
      ContainerComponent.setPosFocus(this);
   }
}
