package necesse.gfx.forms.presets;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameBackground;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormTextBox;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.reports.CrashReportData;

public class ModCrashReportContinueForm extends ConfirmationContinueForm {
   public ModCrashReportContinueForm(
      CrashReportData data,
      List<LoadedMod> mods,
      int maxTextBoxWidth,
      int maxTextBoxHeight,
      Consumer<ModCrashReportContinueForm> openModSettingsPressed,
      Consumer<ModCrashReportContinueForm> closePressed
   ) {
      super("modcrash", 450, 10000);
      this.setWidth(GameMath.limit(WindowManager.getWindow().getHudWidth() - 100, Math.min(450, maxTextBoxWidth), maxTextBoxWidth));
      this.setupConfirmation(
         content -> {
            String modNames = mods.isEmpty() ? "N/A" : GameUtils.join(mods.toArray(new LoadedMod[0]), LoadedMod::getModNameString, ", ", " & ");
            LocalMessage title = new LocalMessage("ui", "modrunerror", "mod", modNames);
            FormLocalLabel label = content.addComponent(
               new FormLocalLabel(title, new FontOptions(16).color(this.getInterfaceStyle().activeTextColor), 0, this.getWidth() / 2, 10, this.getWidth() - 20)
            );
            FormContentBox textContent = content.addComponent(
               new FormContentBox(
                  4,
                  label.getHeight() + 20,
                  this.getWidth() - 8,
                  GameMath.limit(WindowManager.getWindow().getHudHeight() - 150, Math.min(100, maxTextBoxHeight), maxTextBoxHeight),
                  GameBackground.textBox
               )
            );
            FormTextBox textBox = textContent.addComponent(
               new FormTextBox(new FontOptions(16), FairType.TextAlign.LEFT, this.getInterfaceStyle().textBoxTextColor, 0, 0, -1, -1, -1)
            );

            try {
               StringWriter writer = new StringWriter();

               try {
                  writer.write("Mods: " + modNames + "\n\n");

                  for (Throwable modException : data.errors) {
                     modException.printStackTrace(new PrintWriter(writer));
                  }

                  writer.flush();
                  textBox.setText(writer.toString());
               } catch (Throwable var14) {
                  try {
                     writer.close();
                  } catch (Throwable var13) {
                     var14.addSuppressed(var13);
                  }

                  throw var14;
               }

               writer.close();
            } catch (IOException e) {
               textBox.setText("Error writing error log");
            }

            textBox.allowTyping = false;
            textBox.setEmptyTextSpace(new Rectangle(textContent.getX(), textContent.getY(), textContent.getWidth(), textContent.getHeight()));
            textBox.onChange(e -> {
               Rectangle boxx = textContent.getContentBoxToFitComponents();
               textContent.setContentBox(boxx);
               textContent.scrollToFit(textBox.getCaretBoundingBox());
            });
            textBox.onCaretMove(e -> {
               if (!e.causedByMouse) {
                  textContent.scrollToFit(textBox.getCaretBoundingBox());
               }
            });
            Rectangle box = textContent.getContentBoxToFitComponents();
            textContent.setContentBox(box);
         },
         new LocalMessage("ui", "settings"),
         new LocalMessage("ui", "closebutton"),
         () -> openModSettingsPressed.accept(this),
         () -> closePressed.accept(this)
      );
   }
}
