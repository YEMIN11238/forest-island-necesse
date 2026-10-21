package necesse.level.maps.levelData.settlementData.settler.dialogues;

import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.presets.ConfirmationForm;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;

public class RomanceSettlerEnterRelationshipDialogue extends RomanceSettlerDialogue {
   public RomanceSettlerEnterRelationshipDialogue(HumanMob settlerMob) {
      super(settlerMob);
      this.romancePersonality = settlerMob.getRomancePersonality();
   }

   public RomanceSettlerEnterRelationshipDialogue(HumanMob settlerMob, boolean hasCompletedActivity) {
      this(settlerMob);
      this.hasCompletedActivity = hasCompletedActivity;
   }

   @Override
   public boolean onAcceptClicked(ShopContainer container) {
      if (!container.client.isServer()) {
         return false;
      }

      ServerClient serverClient = container.client.getServerClient();
      this.romancePersonality.setIsInRelationshipWith(serverClient);
      this.startYapping(serverClient, this.settlerChatsAfterAccepting, 4.0F, true, null);
      return true;
   }

   @Override
   public void onDeclineClicked(ShopContainer container) {
      if (container.client.isServer()) {
         PlayerRomanceManager romanceManager = this.romancePersonality.getPlayerManager(container.client.getServerClient(), true);
         romanceManager.startActivityCooldown(1);
         this.settlerMob.startExpressionAbility.runAndSend(FormExpressionWheel.Expression.SAD, this.settlerChatsAfterDeclining.size() * 4 * 1000);
         this.startYapping(container.client.getServerClient(), this.settlerChatsAfterDeclining, 4.0F, true, () -> this.settlerMob.clearCommandsOrders(null));
      }
   }

   @Override
   public Form getConfirmClickAcceptForm(ShopContainer container, ShopContainerForm<?> containerForm, DialogueForm subForm, Runnable confirmClicked) {
      ConfirmationForm confirmForm = new ConfirmationForm("confirmrelationshipform", 400, 600);
      GameMessageBuilder builder = new GameMessageBuilder();
      builder.append(
         new LocalMessage("romance", "confirmrelationship1", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
      );
      builder.append(new StaticMessage("\n\n"));
      builder.append(new LocalMessage("romance", "confirmrelationship2"));
      builder.append(new StaticMessage("\n\n"));
      builder.append(
         new LocalMessage("romance", "confirmrelationship3", "settlername", SettlerRomancePersonality.highlightedMessage(this.settlerMob.getSettlerName()))
      );
      builder.append(new StaticMessage("\n\n"));
      builder.append(new LocalMessage("romance", "confirmrelationship4"));
      confirmForm.setupConfirmation(
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
         new LocalMessage("romance", "confirmrelationshipbutton", "settlername", this.settlerMob.getSettlerName()),
         new LocalMessage("romance", "declinerelationshipbutton", "settlername", this.settlerMob.getSettlerName()),
         () -> {
            this.clickedAcceptAction.runAndSend();
            containerForm.getClient().closeContainer(true);
         },
         () -> {
            this.clickedDeclineAction.runAndSend();
            containerForm.getClient().closeContainer(true);
         }
      );
      return confirmForm;
   }
}
