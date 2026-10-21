package necesse.level.maps.levelData.settlementData.settler.dialogues;

import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
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

public class RomanceSettlerStartDatingDialogue extends RomanceSettlerGiveItemDialogue {
   public boolean isAlreadyInRelationship;

   public RomanceSettlerStartDatingDialogue(HumanMob settlerMob) {
      super(settlerMob);
   }

   public RomanceSettlerStartDatingDialogue(HumanMob settlerMob, ServerClient client) {
      super(settlerMob, "prettyflower", 1, false, false);
      this.isAlreadyInRelationship = client.relationshipData.isInAnyRelationship();
      InventoryItem flower = new InventoryItem("prettyflower");
      String settlerName = settlerMob.getSettlerName();
      this.addInitialButtonText(
         new LocalMessage("romance", "suggestdatingbutton", "settlername", settlerName, "item", TypeParsers.getItemAndNameParseString(flower, "**"))
      );
      this.setAcceptActivityButtonText(
         new LocalMessage("romance", "startdatingbutton", "settlername", settlerName, "item", TypeParsers.getItemAndNameParseString(flower, "**"))
      );
      this.setDeclineActivityButtonText(new LocalMessage("ui", "declinebutton"));
   }

   @Override
   protected void setupContainerPacket(PacketWriter writer) {
      super.setupContainerPacket(writer);
      writer.putNextBoolean(this.isAlreadyInRelationship);
   }

   @Override
   protected void applyContainerPacket(PacketReader reader) {
      super.applyContainerPacket(reader);
      this.isAlreadyInRelationship = reader.getNextBoolean();
   }

   @Override
   public boolean onAcceptClicked(ShopContainer container) {
      if (!super.onAcceptClicked(container)) {
         return false;
      }

      if (container.client.isServer()) {
         ServerClient serverClient = container.client.getServerClient();
         serverClient.relationshipData.clearRelationship();
      }

      return true;
   }

   @Override
   public Form getConfirmClickAcceptForm(ShopContainer container, ShopContainerForm<?> containerForm, DialogueForm subForm, Runnable confirmClicked) {
      GameMessageBuilder confirmMessage = new GameMessageBuilder();
      if (this.isAlreadyInRelationship) {
         confirmMessage.append(new LocalMessage("romance", "warndating1"));
         confirmMessage.append(new StaticMessage("\n\n"));
         confirmMessage.append(
            new LocalMessage("romance", "warndating2", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
         );
      } else {
         confirmMessage.append(
            new LocalMessage("romance", "confirmdating1", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
         );
      }

      confirmMessage.append(new StaticMessage("\n\n"));
      confirmMessage.append(new LocalMessage("romance", "confirmdating2"));
      confirmMessage.append(new StaticMessage("\n\n"));
      confirmMessage.append(new LocalMessage("romance", "confirmdating3"));
      confirmMessage.append(new StaticMessage("\n\n"));
      confirmMessage.append(new LocalMessage("romance", "confirmdating4"));
      ConfirmationForm confirmDatingForm = new ConfirmationForm("confirmrelationshipform", 400, 600);
      confirmDatingForm.setupConfirmation(
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
         confirmClicked,
         () -> {
            this.clickedDeclineAction.runAndSend();
            containerForm.getClient().closeContainer(true);
         }
      );
      return confirmDatingForm;
   }
}
