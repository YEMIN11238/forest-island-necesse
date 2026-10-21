package necesse.gfx.forms.presets;

import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.state.MainMenu;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.FormTextureButton;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.reports.ReportUtils;

public abstract class SubscribeToNewsletterForm extends FormSwitcher {
   private static long nextSubscribeTime;
   public final MainMenu mainMenu;
   public Form mainForm;
   public FormTextInput emailInput;
   public FormLocalTextButton subscribeButton;
   public static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w\\-\\.]+@([\\w-]+\\.)+[\\w-]{2,}$");

   public SubscribeToNewsletterForm(MainMenu mainMenu) {
      this.mainMenu = mainMenu;
      this.mainForm = this.addComponent(new Form("main", 400, 500));
      int buttonsWidth = this.mainForm.getWidth() - 8;
      FormFlow mainFlow = new FormFlow(10);
      this.mainForm.addComponent(new FormLocalLabel("ui", "subscribenewsletter", new FontOptions(20), 0, this.mainForm.getWidth() / 2, mainFlow.next(30)));
      FormTextureButton fairTexture = new FormTextureButton(this.mainForm.getWidth() / 2, 0, () -> GameResources.fair, this.mainForm.getWidth() - 20, -1);
      fairTexture.xAlign = FairType.TextAlign.CENTER;
      this.mainForm.addComponent(mainFlow.nextY(fairTexture, 10));
      GameMessageBuilder mainTextBuilder = new GameMessageBuilder();
      mainTextBuilder.append(new LocalMessage("misc", "newsletter1"));
      mainTextBuilder.append(new StaticMessage("\n\n"));
      mainTextBuilder.append(new LocalMessage("misc", "newsletter2"));
      mainTextBuilder.append(new StaticMessage("\n\n"));
      mainTextBuilder.append(new LocalMessage("misc", "newsletter3"));
      FontOptions mainTextFontOptions = new FontOptions(16);
      FormFairTypeLabel mainTextLabel = new FormFairTypeLabel(
         new StaticMessage(""), mainTextFontOptions, FairType.TextAlign.CENTER, this.mainForm.getWidth() / 2, 0
      );
      mainTextLabel.setParsers(TypeParsers.GameEmote(mainTextFontOptions.getSize()));
      mainTextLabel.setLineSpacing(2);
      mainTextLabel.setMaxWidth(this.mainForm.getWidth() - 15);
      mainTextLabel.setText(mainTextBuilder);
      this.mainForm.addComponent(mainFlow.nextY(mainTextLabel, 10));
      this.emailInput = new FormTextInput(4, mainFlow.next(40), FormInputSize.SIZE_32_TO_40, buttonsWidth, 200);
      this.emailInput.placeHolder = new LocalMessage("misc", "yourmailhere");
      this.mainForm.addComponent(this.emailInput);
      this.subscribeButton = new FormLocalTextButton("misc", "subscribebutton", 4, mainFlow.next(40), buttonsWidth);
      this.subscribeButton.onClicked(e -> this.sendEmail(this.emailInput.getText().trim()));
      this.mainForm.addComponent(this.subscribeButton);
      GameMessageBuilder privacyTextBuilder = new GameMessageBuilder();
      privacyTextBuilder.append(new LocalMessage("misc", "newsletterprivacypolicy1"));
      privacyTextBuilder.append(new StaticMessage("\n"));
      privacyTextBuilder.append(new LocalMessage("misc", "newsletterprivacypolicy2"));
      FontOptions privacyFontOptions = new FontOptions(12);
      FormFairTypeLabel privacyLabel = new FormFairTypeLabel(
         new StaticMessage(""), privacyFontOptions, FairType.TextAlign.CENTER, this.mainForm.getWidth() / 2, 0
      );
      privacyLabel.setLineSpacing(2);
      privacyLabel.setMaxWidth(this.mainForm.getWidth() - 15);
      privacyLabel.setText(privacyTextBuilder);
      privacyLabel.setColor(() -> this.getInterfaceStyle().inactiveTextColor);
      this.mainForm.addComponent(mainFlow.nextY(privacyLabel, 10));
      String privacyURL = "https://necessegame.com/privacy-policy";
      LocalMessage openPrivacyMessage = new LocalMessage("misc", "newsletterprivacypolicylink");
      LocalMessage openPrivacyURLMessage = new LocalMessage("misc", "openurl", "url", privacyURL);
      int privacyButtonWidth = Math.min(buttonsWidth, 300);
      this.mainForm
         .addComponent(
            mainFlow.nextY(
               new FormLocalTextButton(
                  openPrivacyMessage,
                  openPrivacyURLMessage,
                  this.mainForm.getWidth() / 2 - privacyButtonWidth / 2,
                  0,
                  privacyButtonWidth,
                  FormInputSize.SIZE_20,
                  ButtonColor.BASE
               ),
               10
            )
         )
         .onClicked(
            e -> {
               if (PlatformManager.getPlatform().canOpenURLs()) {
                  GameUtils.openURL(privacyURL);
               } else {
                  ExternalLinkForm externalLink = new ExternalLinkForm(
                     this.getInterfaceStyle().qr_privacy_policy, privacyURL, openPrivacyMessage, () -> this.makeCurrent(this.mainForm)
                  );
                  this.addAndMakeCurrentTemporary(externalLink);
               }
            }
         );
      this.mainForm.addComponent(new FormLocalTextButton("ui", "backbutton", 4, mainFlow.next(40), buttonsWidth)).onClicked(e -> this.onBackPressed());
      this.mainForm.setHeight(mainFlow.next());
      this.makeCurrent(this.mainForm);
   }

   public abstract void onBackPressed();

   public boolean isEmailValid(String email) {
      return email.isEmpty() ? false : EMAIL_PATTERN.matcher(email.trim()).matches();
   }

   private void sendEmail(String email) {
      AtomicBoolean interrupted = new AtomicBoolean(false);
      Thread thread = new Thread(
         () -> {
            String error = ReportUtils.submitNewsletterSignup(email);
            if (!interrupted.get()) {
               if (error == null) {
                  nextSubscribeTime = System.currentTimeMillis() + 120000L;
                  NoticeForm thanksForm = this.addComponent(new NoticeForm("newsletterthanks", 300, 400), (form, current) -> {
                     if (!current) {
                        this.removeComponent(form);
                     }
                  });
                  thanksForm.setupNotice(new LocalMessage("misc", "thankyouforsigningup"), new LocalMessage("ui", "continuebutton"));
                  thanksForm.onContinue(() -> {
                     this.makeCurrent(this.mainForm);
                     this.emailInput.setTyping(false);
                     this.onBackPressed();
                  });
                  this.emailInput.setText("");
                  this.makeCurrent(thanksForm);
               } else {
                  ConfirmationForm errorForm = this.addComponent(new ConfirmationForm("newslettererror", 300, 400), (form, current) -> {
                     if (!current) {
                        this.removeComponent(form);
                     }
                  });
                  errorForm.setupConfirmation(
                     new StaticMessage(error),
                     new LocalMessage("ui", "sendreportretry"),
                     new LocalMessage("ui", "backbutton"),
                     () -> this.sendEmail(email),
                     () -> this.makeCurrent(this.mainForm)
                  );
                  this.makeCurrent(errorForm);
               }
            }
         }
      );
      NoticeForm waitForm = this.addComponent(new NoticeForm("newsletterwaitwait", 300, 400), (form, current) -> {
         if (!current) {
            this.removeComponent(form);
         }
      });
      waitForm.setupNotice(new LocalMessage("ui", "loadingdotdot"), new LocalMessage("ui", "cancelbutton"));
      waitForm.setButtonCooldown(200);
      waitForm.onContinue(() -> {
         interrupted.set(true);
         thread.interrupt();
         this.makeCurrent(this.mainForm);
      });
      this.makeCurrent(waitForm);
      thread.start();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.isCurrent(this.mainForm)) {
         long timeLeft = nextSubscribeTime - System.currentTimeMillis();
         if (timeLeft < 0L) {
            String email = this.emailInput.getText();
            if (this.isEmailValid(email)) {
               this.subscribeButton.setActive(true);
               this.subscribeButton.setLocalTooltip(null);
            } else {
               this.subscribeButton.setActive(false);
               this.subscribeButton.setLocalTooltip(new LocalMessage("misc", "invalidemail"));
            }
         } else {
            this.subscribeButton.setActive(false);
            this.subscribeButton
               .setLocalTooltip(new LocalMessage("ui", "sendfeedbackwaittime", "time", GameUtils.formatSeconds((int)Math.ceil(timeLeft / 1000.0))));
         }
      }

      super.draw(tickManager, perspective, renderBox);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.mainForm.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
   }
}
