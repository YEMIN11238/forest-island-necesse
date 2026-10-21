package necesse.gfx.forms.presets.creative;

import java.awt.Rectangle;
import necesse.engine.GameDeathPenalty;
import necesse.engine.GameDifficulty;
import necesse.engine.GameRaidFrequency;
import necesse.engine.Settings;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketCreativeWorldSettings;
import necesse.engine.world.WorldSettings;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormComponentListTyped;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormDropdownSelectionButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.localComponents.FormLocalCheckBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

public class CreativeSettingsTab extends CreativeTab {
   protected final FormContentBox contentBox;
   protected final FormLocalCheckBox hostileSpawns;
   protected final FormLocalCheckBox mobAI;
   protected final FormLocalCheckBox survivalMode;
   protected final FormLocalCheckBox playerHunger;
   protected final FormLocalCheckBox canSettlersDie;
   protected final FormDropdownSelectionButton<GameDeathPenalty> deathPenalty;
   protected final FormDropdownSelectionButton<GameRaidFrequency> raidFrequency;
   protected final FormDropdownSelectionButton<GameDifficulty> difficulty;
   protected final GameMessage warningMessage;
   protected final FormComponentListTyped<FormLocalLabel> warningLabelHider;
   protected final FormLocalLabel warningLabel;

   public CreativeSettingsTab(Form form, Client playerClient) {
      super(form, playerClient);
      FontOptions fontOptions = new FontOptions(16);
      form.addComponent(new FormLocalLabel("ui", "creativesettingstab", new FontOptions(20), -1, 5, 5));
      this.warningMessage = new LocalMessage("ui", "creativesettingsnopermission");
      this.warningLabelHider = form.addComponent(new FormComponentListTyped<>());
      this.warningLabel = this.warningLabelHider
         .addComponent(
            new FormLocalLabel(this.warningMessage, new FontOptions(16).color(form.getInterfaceStyle().errorTextColor), -1, 10, 35, form.getWidth() - 10)
         );
      this.contentBox = form.addComponent(new FormContentBox(0, 0, form.getWidth(), form.getHeight()));
      FormFlow flow = new FormFlow(2);
      int selectorWidth = 250;
      int selectorX = this.contentBox.getWidth() - 4 - this.contentBox.getScrollBarWidth() - selectorWidth;
      this.contentBox.addComponent(new FormLocalLabel("ui", "difficulty", fontOptions, -1, 10, flow.next(24)));
      this.difficulty = this.contentBox
         .addComponent(flow.sameY(new FormDropdownSelectionButton<>(selectorX, 0, FormInputSize.SIZE_24, ButtonColor.BASE, selectorWidth), -2));

      for (GameDifficulty value : GameDifficulty.values()) {
         this.difficulty.options.add(value, value.displayName, () -> {
            GameMessageBuilder toolTipBuilder = new GameMessageBuilder();
            value.description.breakMessage(new FontOptions(Settings.tooltipTextSize), 500).forEach(message -> {
               toolTipBuilder.append(message);
               toolTipBuilder.append("\n");
            });
            return toolTipBuilder;
         });
      }

      this.difficulty.onSelected(e -> {
         playerClient.worldSettings.difficulty = e.value;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      flow.next(6);
      this.contentBox.addComponent(new FormLocalLabel("ui", "deathpenalty", fontOptions, -1, 10, flow.next(24)));
      this.deathPenalty = this.contentBox
         .addComponent(flow.sameY(new FormDropdownSelectionButton<>(selectorX, 0, FormInputSize.SIZE_24, ButtonColor.BASE, selectorWidth), -2));

      for (GameDeathPenalty value : GameDeathPenalty.values()) {
         this.deathPenalty.options.add(value, value.displayName, () -> value.description);
      }

      this.deathPenalty.onSelected(e -> {
         playerClient.worldSettings.deathPenalty = e.value;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      flow.next(6);
      this.contentBox.addComponent(new FormLocalLabel("ui", "raidfrequency", fontOptions, -1, 10, flow.next(24)));
      this.raidFrequency = this.contentBox
         .addComponent(flow.sameY(new FormDropdownSelectionButton<>(selectorX, 0, FormInputSize.SIZE_24, ButtonColor.BASE, selectorWidth), -2));

      for (GameRaidFrequency value : GameRaidFrequency.values()) {
         this.raidFrequency.options.add(value, value.displayName, () -> value.description);
      }

      this.raidFrequency.onSelected(e -> {
         playerClient.worldSettings.raidFrequency = e.value;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      flow.next(6);
      this.survivalMode = this.contentBox
         .addComponent(flow.nextY(new FormLocalCheckBox("ui", "survivalmode", 10, 0, this.contentBox.getWidth() - 20).useButtonTexture(), 4));
      this.survivalMode.onClicked(e -> {
         playerClient.worldSettings.survivalMode = e.from.checked;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      this.contentBox
         .addComponent(flow.nextY(new FormLocalLabel("ui", "survivalmodetip", new FontOptions(12), -1, 10, 0, this.contentBox.getWidth() - 30), 12));
      this.playerHunger = this.contentBox
         .addComponent(flow.nextY(new FormLocalCheckBox("ui", "playerhungerbox", 8, 0, this.contentBox.getWidth() - 20).useButtonTexture(), 8));
      this.playerHunger.onClicked(e -> {
         playerClient.worldSettings.playerHunger = e.from.checked;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      this.canSettlersDie = this.contentBox
         .addComponent(flow.nextY(new FormLocalCheckBox("ui", "cansettlersdiebox", 8, 0, this.contentBox.getWidth() - 20).useButtonTexture(), 4));
      this.canSettlersDie.onClicked(e -> {
         playerClient.worldSettings.canSettlersDie = e.from.checked;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      this.canSettlersDie.checked = playerClient.worldSettings.canSettlersDie;
      this.contentBox.addComponent(flow.nextY(new FormLocalLabel("ui", "settlerscandietip1", new FontOptions(12), -1, 10, 0, this.contentBox.getWidth() - 30)));
      this.contentBox
         .addComponent(flow.nextY(new FormLocalLabel("ui", "settlerscandietip2", new FontOptions(12), -1, 10, 0, this.contentBox.getWidth() - 30), 12));
      this.hostileSpawns = this.contentBox.addComponent(flow.nextY(new FormLocalCheckBox("ui", "creativehostilespawnsetting", 8, 0).useButtonTexture(), 8));
      this.hostileSpawns.onClicked(e -> {
         playerClient.worldSettings.disableMobSpawns = !e.from.checked;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      this.hostileSpawns.checked = !playerClient.worldSettings.disableMobSpawns;
      this.mobAI = this.contentBox.addComponent(flow.nextY(new FormLocalCheckBox("ui", "creativedisablemobaisetting", 8, 0).useButtonTexture(), 8));
      this.mobAI.onClicked(e -> {
         playerClient.worldSettings.disableMobAI = !e.from.checked;
         playerClient.network.sendPacket(new PacketCreativeWorldSettings(playerClient));
      });
      this.mobAI.checked = playerClient.worldSettings.disableMobAI;
      this.contentBox.setContentBox(new Rectangle(form.getWidth(), flow.next()));
   }

   @Override
   public void updateBeforeDraw(TickManager tickManager) {
      boolean hasPermission = this.playerClient.getPermissionLevel().getLevel() >= PermissionLevel.CREATIVESETTINGS.getLevel();
      this.warningLabelHider.setHidden(hasPermission);
      this.difficulty.setActive(hasPermission);
      this.deathPenalty.setActive(hasPermission);
      this.raidFrequency.setActive(hasPermission);
      this.survivalMode.setActive(hasPermission);
      this.playerHunger.setActive(hasPermission && !this.survivalMode.checked);
      this.canSettlersDie.setActive(hasPermission);
      this.hostileSpawns.setActive(hasPermission);
      this.mobAI.setActive(hasPermission);
      WorldSettings worldSettings = this.playerClient.worldSettings;
      this.difficulty.setSelected(worldSettings.difficulty, worldSettings.difficulty.displayName);
      this.deathPenalty.setSelected(worldSettings.deathPenalty, worldSettings.deathPenalty.displayName);
      this.raidFrequency.setSelected(worldSettings.raidFrequency, worldSettings.raidFrequency.displayName);
      this.survivalMode.checked = worldSettings.survivalMode;
      this.playerHunger.checked = worldSettings.playerHunger || this.survivalMode.checked;
      this.canSettlersDie.checked = worldSettings.canSettlersDie;
      this.hostileSpawns.checked = !worldSettings.disableMobSpawns;
      this.mobAI.checked = !worldSettings.disableMobAI;
      this.contentBox.setY(35 + (this.warningLabelHider.isHidden() ? 0 : this.warningLabel.getHeight() + 10));
      this.contentBox.setHeight(this.form.getHeight() - this.contentBox.getY());
      super.updateBeforeDraw(tickManager);
   }
}
