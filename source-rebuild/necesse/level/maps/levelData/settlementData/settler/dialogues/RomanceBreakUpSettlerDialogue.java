package necesse.level.maps.levelData.settlementData.settler.dialogues;

import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.packet.PacketClientRelationshipData;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.presets.ConfirmationForm;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;

public class RomanceBreakUpSettlerDialogue extends RomanceSettlerGiveItemDialogue {
   public RomanceBreakUpSettlerDialogue(HumanMob settlerMob) {
      super(settlerMob);
   }

   public RomanceBreakUpSettlerDialogue(HumanMob settlerMob, ServerClient client) {
      super(settlerMob, "divorcepapers", 1, false, false);
      InventoryItem papers = new InventoryItem("divorcepapers");
      String settlerName = settlerMob.getSettlerName();
      this.addInitialButtonText(new LocalMessage("romance", this.romancePersonality.getStringID() + "_suggestbreakupbutton", "settlername", settlerName));
      this.addMainTextSections("romance", this.romancePersonality.getStringID() + "_suggestbreakup", 2, "name", client.getName(), "settlername", settlerName);
      this.setAcceptActivityButtonText(new LocalMessage("romance", "servepapers", "item", TypeParsers.getItemAndNameParseString(papers, "**")));
      this.setDeclineActivityButtonText(
         new LocalMessage("romance", this.romancePersonality.getStringID() + "_declinebreakupbutton", "settlername", settlerName)
      );
      this.addSettlerChatsAfterAccepting(
         "romance", this.romancePersonality.getStringID() + "_acceptedbreakupchat", 3, "name", client.getName(), "settlername", settlerName
      );
      this.addSettlerChatsAfterDeclining(
         "romance", this.romancePersonality.getStringID() + "_declinedbreakupchat", 2, "name", client.getName(), "settlername", settlerName
      );
   }

   @Override
   public boolean onAcceptClicked(ShopContainer container) {
      if (!super.onAcceptClicked(container)) {
         return false;
      }

      if (container.client.isServer()) {
         ServerClient serverClient = container.client.getServerClient();
         serverClient.relationshipData.clearRelationship();
         serverClient.getServer().network.sendToAllClients(new PacketClientRelationshipData(serverClient));
      }

      return true;
   }

   @Override
   public Form getConfirmClickAcceptForm(ShopContainer container, ShopContainerForm<?> containerForm, DialogueForm subForm, Runnable confirmClicked) {
      GameMessageBuilder builder = new GameMessageBuilder();
      builder.append(
         new LocalMessage("romance", "confirmbreakup1", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
      );
      builder.append(new StaticMessage("\n\n"));
      builder.append(
         new LocalMessage("romance", "confirmbreakup2", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
      );
      builder.append(new StaticMessage("\n\n"));
      builder.append(
         new LocalMessage("romance", "confirmbreakup3", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
      );
      ConfirmationForm divorceConfirmationForm = new ConfirmationForm("breakupconfirmationform", 400, 600);
      divorceConfirmationForm.setupConfirmation(
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
            fairTypeLabel.setText(builder);
         },
         new LocalMessage("ui", "acceptbutton"),
         new LocalMessage("ui", "declinebutton"),
         confirmClicked,
         () -> {
            this.clickedDeclineAction.runAndSend();
            containerForm.getClient().closeContainer(true);
         }
      );
      return divorceConfirmationForm;
   }
}
