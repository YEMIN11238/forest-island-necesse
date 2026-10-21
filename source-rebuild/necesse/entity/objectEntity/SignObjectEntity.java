package necesse.entity.objectEntity;

import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.forms.presets.containerComponent.object.SignContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.FairTypeTooltip;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.level.maps.Level;

public class SignObjectEntity extends ObjectEntity {
   private GameMessage message;
   private FairTypeDrawOptions textDrawOptions;
   private int textDrawFontSize;

   public SignObjectEntity(Level level, int x, int y) {
      super(level, "sign", x, y);
      this.setMessage("");
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addSaveData(this.message.getSaveData("message"));
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      boolean foundMessage = false;
      LoadData messageSave = save.getFirstLoadDataByName("message");
      if (messageSave != null) {
         GameMessage message = GameMessage.loadSave(messageSave);
         if (message != null) {
            this.setMessage(message);
            foundMessage = true;
         }
      }

      if (!foundMessage) {
         String textSave = save.getSafeString("text", "");
         this.setMessage(textSave);
      }
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      this.message.writePacket(writer);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      this.setMessage(GameMessage.fromPacket(reader));
   }

   public void setMessage(GameMessage message) {
      GameMessage oldMessage = this.message;
      this.message = message;
      if (!message.equals(oldMessage)) {
         this.textDrawOptions = null;
      }
   }

   public void setMessage(String message) {
      this.setMessage(new StaticMessage(message));
   }

   @Deprecated
   public void setText(String str) {
      this.setMessage(str.trim());
   }

   private FairTypeDrawOptions getTextDrawOptions() {
      if (this.textDrawOptions == null || this.textDrawOptions.shouldUpdate() || this.textDrawFontSize != Settings.tooltipTextSize || this.message.hasUpdated()
         )
       {
         FairType type = new FairType();
         FontOptions fontOptions = new FontOptions(Settings.tooltipTextSize).outline();
         type.append(fontOptions, this.message.translate());
         type.applyParsers(SignContainerForm.getParsers(fontOptions));
         this.textDrawOptions = type.getDrawOptions(FairType.TextAlign.LEFT, 400, true, true);
         this.textDrawFontSize = fontOptions.getSize();
      }

      return this.textDrawOptions;
   }

   public GameMessage getSignMessage() {
      return this.message;
   }

   @Override
   public void onMouseHover(PlayerMob perspective, boolean debug) {
      super.onMouseHover(perspective, debug);
      GameTooltipManager.addTooltip(new FairTypeTooltip(this.getTextDrawOptions()), TooltipLocation.INTERACT_FOCUS);
   }

   @Override
   public GameTooltips getMapTooltips() {
      ListGameTooltips tooltips = new ListGameTooltips(new StringTooltips(this.getObject().getDisplayName() + ":"));
      tooltips.add(new FairTypeTooltip(this.getTextDrawOptions()));
      return tooltips;
   }
}
