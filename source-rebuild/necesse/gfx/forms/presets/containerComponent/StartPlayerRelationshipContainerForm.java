package necesse.gfx.forms.presets.containerComponent;

import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.presets.ConfirmationForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.StartPlayerRelationshipContainer;

public class StartPlayerRelationshipContainerForm extends ContainerFormSwitcher<StartPlayerRelationshipContainer> {
   public StartPlayerRelationshipContainerForm(Client client, StartPlayerRelationshipContainer container) {
      super(client, container);
      GameMessageBuilder confirmMessage = new GameMessageBuilder();
      if (container.isReceivingEnd) {
         confirmMessage.append(new LocalMessage("romance", "askedplayer", "name", "**" + container.targetName + "**"));
         if (container.isAlreadyInRelationship) {
            confirmMessage.append(new StaticMessage("\n\n"));
            confirmMessage.append(new LocalMessage("romance", "warnplayer1"));
            confirmMessage.append(new StaticMessage("\n\n"));
            confirmMessage.append(new LocalMessage("romance", "warnplayer2"));
         } else {
            confirmMessage.append(new StaticMessage("\n\n"));
            confirmMessage.append(new LocalMessage("romance", "confirmdating3"));
         }

         confirmMessage.append(new StaticMessage("\n\n"));
         confirmMessage.append(new LocalMessage("romance", "askplayeraccept"));
      } else {
         confirmMessage.append(new LocalMessage("romance", "confirmplayer", "name", "**" + container.targetName + "**"));
         if (container.isAlreadyInRelationship) {
            confirmMessage.append(new StaticMessage("\n\n"));
            confirmMessage.append(new LocalMessage("romance", "warnplayer1"));
            confirmMessage.append(new StaticMessage("\n\n"));
            confirmMessage.append(new LocalMessage("romance", "warnplayer2"));
         } else {
            confirmMessage.append(new StaticMessage("\n\n"));
            confirmMessage.append(new LocalMessage("romance", "confirmdating3"));
         }

         confirmMessage.append(new StaticMessage("\n\n"));
         confirmMessage.append(new LocalMessage("romance", "askplayeraccept"));
      }

      ConfirmationForm form = new ConfirmationForm("confirmrelationshipform", 400, 600);
      form.setupConfirmation(
         content -> {
            FontOptions fontOptions = new FontOptions(16);
            FormFairTypeLabel fairTypeLabel = content.addComponent(
               new FormFairTypeLabel(new StaticMessage(""), fontOptions, FairType.TextAlign.CENTER, content.getWidth() / 2, 10)
            );
            fairTypeLabel.setParsers(
               TypeParsers.GAME_COLOR,
               TypeParsers.highlightParser("**", "**", true, () -> Settings.UI.incursionModifierOrange),
               TypeParsers.ItemIcon(fontOptions.getSize()),
               TypeParsers.MobIcon(fontOptions.getSize()),
               TypeParsers.GameEmote(fontOptions.getSize())
            );
            fairTypeLabel.setLineSpacing(2);
            fairTypeLabel.setMaxWidth(content.getWidth() - 20);
            fairTypeLabel.setText(confirmMessage);
         },
         new LocalMessage("ui", "acceptbutton"),
         new LocalMessage("ui", "declinebutton"),
         container.acceptButton::runAndSend,
         container.declineButton::runAndSend
      );
      this.addComponent(form);
      this.makeCurrent(form);
   }

   @Override
   public boolean shouldShowInventory() {
      return false;
   }

   @Override
   public boolean shouldOpenInventory() {
      return false;
   }

   @Override
   public boolean shouldShowToolbar() {
      return false;
   }
}
