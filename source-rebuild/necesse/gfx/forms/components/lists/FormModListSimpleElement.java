package necesse.gfx.forms.components.lists;

import java.awt.Color;
import java.awt.Rectangle;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormMouseHover;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.HoverStateTextures;

public class FormModListSimpleElement extends Form {
   public FormModListSimpleElement(String text, int width, final LoadedMod.SaveType modType, final boolean enabled, Color color, final GameTooltips tooltip) {
      super(width, 24);
      this.drawBase = false;
      this.shouldLimitDrawArea = false;
      this.addComponent(new FormCustomDraw(4, 0, 20, 20) {
         @Override
         public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
            HoverStateTextures icon = null;
            GameMessage modMessage = new StaticMessage("Unknown mod type");
            if (modType != null) {
               if (modType.displayName != null) {
                  modMessage = modType.displayName;
               }

               if (modType.iconGetter != null) {
                  icon = modType.iconGetter.apply(this.getInterfaceStyle());
               }
            }

            GameTexture texture = icon != null ? icon.active : Settings.UI.icon_unknown;
            Color colorx = enabled ? this.getInterfaceStyle().activeTextColor : this.getInterfaceStyle().inactiveTextColor;
            if (this.isHovering()) {
               colorx = this.getInterfaceStyle().highlightTextColor;
               texture = icon != null ? icon.highlighted : Settings.UI.icon_unknown;
            }

            texture.initDraw().size(24).color(colorx).draw(this.getX(), this.getY());
            if (this.isHovering()) {
               GameTooltipManager.addTooltip(new StringTooltips(modMessage.translate()), TooltipLocation.FORM_FOCUS);
            }
         }
      });
      int startX = 28;
      this.addComponent(new FormLabel(text, new FontOptions(16).color(color), -1, startX, 4, width - startX));
      if (tooltip != null) {
         this.addComponent(new FormMouseHover(startX, 0, width - startX, this.getHeight()) {
            @Override
            public GameTooltips getTooltips(PlayerMob perspective) {
               return tooltip;
            }
         }, 1000);
      }
   }

   public FormModListSimpleElement(String text, int width, LoadedMod.SaveType modType, boolean enabled, Color color, String tooltip) {
      this(text, width, modType, enabled, color, tooltip == null ? null : new StringTooltips(tooltip));
   }

   public FormModListSimpleElement(String text, int width, LoadedMod.SaveType modType, boolean enabled, Color color) {
      this(text, width, modType, enabled, color, (GameTooltips)null);
   }
}
