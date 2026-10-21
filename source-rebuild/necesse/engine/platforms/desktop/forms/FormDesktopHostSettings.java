package necesse.engine.platforms.desktop.forms;

import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.HostSettingsForm;
import necesse.engine.network.server.PortedServerSettings;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerCreationSettings;
import necesse.engine.network.server.ServerHostSettings;
import necesse.engine.state.MainGame;
import necesse.engine.state.MainMenu;
import necesse.engine.state.MainMenu.ConnectFrom;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldSettings;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormPasswordInput;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.gameFont.FontOptions;

public class FormDesktopHostSettings extends HostSettingsForm {
   private final Runnable onHostSarted;
   private final Runnable backButtonPressed;
   private final GameMessage backButtonName;
   private final PortedServerSettings portedServerSettings;
   private final ServerHostSettings serverHostSettings;

   public FormDesktopHostSettings(GameMessage backButtonName, Runnable backButtonPressed, Runnable onHostStarted) {
      super("hostSettingsForm", 320, 400);
      this.onHostSarted = onHostStarted;
      this.backButtonPressed = backButtonPressed;
      this.backButtonName = backButtonName;
      this.portedServerSettings = new PortedServerSettings(null, 10, 14159);
      this.serverHostSettings = new ServerHostSettings();
      this.reset(null);
   }

   private void redraw(FormTextInput lastPortInput) {
      this.clearComponents();
      FormFlow flow = new FormFlow(4);
      int maxTextWidth = this.getWidth() - 6;
      this.addComponent(flow.nextY(new FormLocalLabel("ui", "playerslots", new FontOptions(16), -1, 4, 0)));
      FormSlider slotsSlider = this.addComponent(
         flow.nextY(new FormSlider("", 10, 0, this.portedServerSettings.slots, 1, 25, this.getWidth() - 20, new FontOptions(12)), 5)
      );
      slotsSlider.drawValueInPercent = false;
      slotsSlider.onChanged(e -> this.portedServerSettings.slots = slotsSlider.getValue());
      FormCheckBox allowOutsideCharacters = this.addComponent(
         (FormLocalCheckBox)flow.nextY(new FormLocalCheckBox("ui", "allowoutsidecharactersbox", 5, 0, this.serverHostSettings.allowOutsideCharacters), 5)
      );
      allowOutsideCharacters.handleClicksIfNoEventHandlers = true;
      allowOutsideCharacters.onClicked(e -> this.serverHostSettings.allowOutsideCharacters = allowOutsideCharacters.checked);
      FormCheckBox forcedPvP = this.addComponent(
         (FormLocalCheckBox)flow.nextY(new FormLocalCheckBox("ui", "forcedpvpbox", 5, 0, this.serverHostSettings.forcedPvP), 20)
      );
      forcedPvP.handleClicksIfNoEventHandlers = true;
      forcedPvP.onClicked(e -> this.serverHostSettings.forcedPvP = forcedPvP.checked);
      boolean hostEnabled = true;
      this.addComponent(flow.nextY(new FormLocalLabel("ui", "hostport", new FontOptions(16), -1, 4, 0)));
      FormTextInput portInput = this.addComponent(flow.nextY(new FormTextInput(4, 0, FormInputSize.SIZE_32_TO_40, this.getWidth() - 8, 5), 10));
      portInput.setRegexMatchFull("[0-9]+");
      portInput.setText(this.portedServerSettings.port == -1 ? "" : String.valueOf(this.portedServerSettings.port));
      portInput.onChange(e -> {
         if (!portInput.getText().isEmpty()) {
            this.portedServerSettings.port = Integer.parseInt(portInput.getText());
         } else {
            this.portedServerSettings.port = -1;
         }

         this.redraw(portInput);
      });
      if (lastPortInput != null) {
         portInput.setTyping(true);
         portInput.setCaretIndex(lastPortInput.getCaretIndex());
      }

      if (this.portedServerSettings.port < 0 || this.portedServerSettings.port > 65535) {
         this.addComponent(
            flow.nextY(
               new FormLabel(
                  Localization.translate("ui", "hostporterror", "port", 14159),
                  new FontOptions(12).color(this.getInterfaceStyle().errorTextColor),
                  -1,
                  4,
                  0,
                  maxTextWidth
               ),
               10
            )
         );
         hostEnabled = false;
      }

      flow.next(5);
      this.addComponent(flow.nextY(new FormLocalLabel("ui", "password", new FontOptions(16), -1, 4, 0)));
      FormPasswordInput passwordInput = this.addComponent(flow.nextY(new FormPasswordInput(4, 0, FormInputSize.SIZE_32_TO_40, this.getWidth() - 8, 50), 13));
      passwordInput.setText(this.portedServerSettings.password);
      passwordInput.onChange(e -> this.portedServerSettings.password = passwordInput.getText());
      this.addComponent(flow.nextY(new FormLocalLabel("ui", "passwordtip", new FontOptions(12), -1, 4, 0, maxTextWidth), 10));
      FormLocalTextButton hostButton = this.addComponent(new FormLocalTextButton("ui", "hoststart", 4, flow.next(), this.getWidth() / 2 - 6));
      hostButton.onClicked(e -> this.host());
      hostButton.setActive(hostEnabled);
      if (this.backButtonName != null) {
         this.addComponent(new FormLocalTextButton(this.backButtonName, this.getWidth() / 2 + 2, flow.next(40), this.getWidth() / 2 - 6))
            .onClicked(e -> this.backButtonPressed.run());
      }

      this.setHeight(flow.next());
      this.onWindowResized(WindowManager.getWindow());
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
   }

   @Override
   protected void hostFromLoadedWorld(MainGame mainGame) {
      Server server = mainGame.getClient().getLocalServer();
      if (server != null) {
         server.startHostFromSingleplayer(this.portedServerSettings, this.serverHostSettings);
      } else {
         System.err.println("Could not find local server for hosting");
      }

      if (this.onHostSarted != null) {
         this.onHostSarted.run();
      }
   }

   @Override
   protected void hostFromMainMenu(MainMenu mainMenu) {
      ServerCreationSettings serverCreationSettings = new ServerCreationSettings(this.selectedWorldSave.filePath);
      if (this.selectedWorldSave.creationSettings != null) {
         serverCreationSettings = this.selectedWorldSave.creationSettings;
      }

      PortedServerSettings serverSettings = PortedServerSettings.createHostServerSettings(
         serverCreationSettings, this.portedServerSettings.slots, this.portedServerSettings.port
      );
      serverSettings.allowConnectByIP = this.portedServerSettings.allowConnectByIP;

      try {
         mainMenu.host(this.selectedWorldSave, serverSettings, this.serverHostSettings, ConnectFrom.MultiplayerHostWorld);
      } catch (Exception e) {
         mainMenu.addNotice("Error hosting " + this.selectedWorldSave.filePath.getName());
         e.printStackTrace();
      }

      if (this.onHostSarted != null) {
         this.onHostSarted.run();
      }
   }

   @Override
   public void reset(WorldSettings worldSettings) {
      this.portedServerSettings.slots = Settings.serverSlots;
      this.portedServerSettings.allowConnectByIP = true;
      this.portedServerSettings.port = Settings.serverPort;
      if (worldSettings == null) {
         this.serverHostSettings.forcedPvP = false;
         this.serverHostSettings.allowOutsideCharacters = true;
      } else {
         this.serverHostSettings.forcedPvP = worldSettings.forcedPvP;
         this.serverHostSettings.allowOutsideCharacters = worldSettings.allowOutsideCharacters;
      }

      this.redraw(null);
   }
}
