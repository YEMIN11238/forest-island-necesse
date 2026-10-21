package necesse.gfx.forms;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.PointHashSet;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormContentIconToggleButton;
import necesse.gfx.forms.components.FormFairTypeDraw;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTypingComponent;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.ui.ButtonColor;

public abstract class ControllerKeyboardForm extends FormComponentList {
   public static ArrayList<String[]> keyboards = new ArrayList<>(
      Arrays.asList(
         new String[]{"1234567890", "1234567890", "qwertyuiop", "QWERTYUIOP", "asdfghjkl'", "ASDFGHJKL\"", "zxcvbnm,.?", "ZXCVBNM;:!"},
         new String[]{"!#%&/\\`´~^", "!#%&/\\`´~^", "@_-+=;:<>|", "@_-+=;:<>|", "()[]{}*€$£", "()[]{}*âêã", "áéíúü¡¿æøå", "ãêїñöšçÆØÅ"}
      )
   );
   private final FormTypingComponent typingComponent;
   private FormLocalLabel header;
   private final FormFairTypeDraw label;
   private final FormSwitcher keyboardSwitcher;
   private final ArrayList<Form> normalForms = new ArrayList<>(keyboards.size());
   private final ArrayList<Form> capitalizedForms = new ArrayList<>(keyboards.size());
   private final Object repeatSpacebar = new Object();
   private final Object repeatBackspace = new Object();
   private int currentForm;
   private boolean capitalized;

   public ControllerKeyboardForm(FormTypingComponent typingComponent) {
      this.typingComponent = typingComponent;
      this.keyboardSwitcher = this.addComponent(new FormSwitcher());
      GameMessage typingHeader = typingComponent.getControllerTypingHeader();
      if (typingHeader != null) {
         this.header = this.addComponent(new FormLocalLabel(typingHeader, new FontOptions(20).color(Color.WHITE).outline(), 0, 0, 0, 400));
      }

      this.label = this.addComponent(new FormFairTypeDraw(0, 0));
      this.label.drawOptions = typingComponent.getTextBoxDrawOptions();

      for (int i = 0; i < keyboards.size(); i++) {
         String[] keyboard = keyboards.get(i);
         PointHashSet tiles = new PointHashSet();
         int maxLength = 8;

         for (int j = 0; j < keyboard.length; j += 2) {
            String normal = keyboard[j];
            String cap = keyboard[j + 1];
            int length = Math.max(normal.length(), cap.length());
            maxLength = Math.max(length, maxLength);

            for (int k = 0; k < length; k++) {
               tiles.add(k, j / 2);
            }
         }

         int height = keyboard.length / 2;

         for (int j = 0; j < maxLength; j++) {
            tiles.add(j, height);
         }

         int padding = 1;
         FormInputSize inputSize = FormInputSize.SIZE_32;
         int componentSize = inputSize.height + padding * 2;
         int buttonSize = inputSize.height;
         Form normalForm = this.keyboardSwitcher.addComponent(new Form("keyboardNormal" + i, tiles, buttonSize + padding * 2, 4));
         this.normalForms.add(normalForm);
         Form capitalizedForm = this.keyboardSwitcher.addComponent(new Form("keyboardCap" + i, tiles, buttonSize + padding * 2, 4));
         this.capitalizedForms.add(capitalizedForm);

         for (int j = 0; j < keyboard.length; j += 2) {
            String normal = keyboard[j];
            String cap = keyboard[j + 1];
            int length = Math.max(normal.length(), cap.length());

            for (int k = 0; k < length; k++) {
               char normalChar = k < normal.length() ? normal.charAt(k) : ' ';
               char capChar = k < cap.length() ? cap.charAt(k) : ' ';
               FormTextButton normalCharButton = normalForm.addComponent(
                  new FormTextButton(
                     Character.toString(normalChar), k * componentSize + padding, j / 2 * componentSize + padding, buttonSize, inputSize, ButtonColor.BASE
                  )
               );
               normalCharButton.onClicked(e -> this.submitText(String.valueOf(normalChar)));
               normalCharButton.controllerFocusHashcode = "keyboardKey" + j + "x" + k;
               normalCharButton.acceptMouseRepeatEvents = true;
               normalCharButton.submitControllerPressEvent = true;
               normalCharButton.setActive(typingComponent.isValidAppendText(String.valueOf(normalChar)));
               FormTextButton capCharButton = capitalizedForm.addComponent(
                  new FormTextButton(
                     Character.toString(capChar), k * componentSize + padding, j / 2 * componentSize + padding, buttonSize, inputSize, ButtonColor.BASE
                  )
               );
               capCharButton.onClicked(e -> this.submitText(String.valueOf(capChar)));
               capCharButton.controllerFocusHashcode = "keyboardKey" + j + "x" + k;
               capCharButton.acceptMouseRepeatEvents = true;
               normalCharButton.submitControllerPressEvent = true;
               capCharButton.setActive(typingComponent.isValidAppendText(String.valueOf(capChar)));
            }
         }

         int spacebarWidth = maxLength - 5;
         (normalForm.addComponent(
               new FormContentIconToggleButton(
                  padding, height * componentSize + padding, buttonSize, inputSize, ButtonColor.YELLOW, this.getInterfaceStyle().keyboard_shift
               ) {
                  @Override
                  public boolean isToggled() {
                     return ControllerKeyboardForm.this.capitalized;
                  }
               }
            ))
            .onClicked(e -> {
               this.capitalized = !this.capitalized;
               this.refreshCurrentKeyboard();
            })
            .controllerFocusHashcode = "keyboardCap";
         normalForm.addComponent(
               new FormContentIconButton(
                  componentSize + padding, height * componentSize + padding, buttonSize, inputSize, ButtonColor.YELLOW, this.getInterfaceStyle().keyboard_next
               )
            )
            .onClicked(e -> {
               this.currentForm = (this.currentForm + 1) % this.normalForms.size();
               this.refreshCurrentKeyboard();
            })
            .controllerFocusHashcode = "keyboardChange";
         FormContentIconButton normalSpaceButton = normalForm.addComponent(
            new FormContentIconButton(
               componentSize * 2 + padding,
               height * componentSize + padding,
               spacebarWidth * buttonSize + padding * (spacebarWidth - 1) * 2,
               inputSize,
               ButtonColor.YELLOW,
               this.getInterfaceStyle().keyboard_spacebar
            )
         );
         normalSpaceButton.onClicked(e -> this.submitText(" "));
         normalSpaceButton.controllerFocusHashcode = "keyboardSpace";
         normalSpaceButton.acceptMouseRepeatEvents = true;
         normalSpaceButton.setActive(typingComponent.isValidAppendText(" "));
         FormContentIconButton normalBackspaceButton = normalForm.addComponent(
            new FormContentIconButton(
               componentSize * 2 + spacebarWidth * componentSize + padding,
               height * componentSize + padding,
               buttonSize * 2 + padding * 2,
               inputSize,
               ButtonColor.YELLOW,
               this.getInterfaceStyle().keyboard_backspace
            )
         );
         normalBackspaceButton.onClicked(e -> this.submitBackspace());
         normalBackspaceButton.controllerFocusHashcode = "keyboardBack";
         normalBackspaceButton.acceptMouseRepeatEvents = true;
         normalForm.addComponent(
               new FormContentIconButton(
                  componentSize * 4 + spacebarWidth * componentSize + padding,
                  height * componentSize + padding,
                  buttonSize,
                  inputSize,
                  ButtonColor.GREEN,
                  this.getInterfaceStyle().keyboard_return
               )
            )
            .onClicked(e -> this.submitEnter(e.event))
            .controllerFocusHashcode = "keyboardEnter";
         (capitalizedForm.addComponent(
               new FormContentIconToggleButton(
                  padding, height * componentSize + padding, buttonSize, inputSize, ButtonColor.YELLOW, this.getInterfaceStyle().keyboard_shift
               ) {
                  @Override
                  public boolean isToggled() {
                     return ControllerKeyboardForm.this.capitalized;
                  }
               }
            ))
            .onClicked(e -> {
               this.capitalized = !this.capitalized;
               this.refreshCurrentKeyboard();
            })
            .controllerFocusHashcode = "keyboardCap";
         capitalizedForm.addComponent(
               new FormContentIconButton(
                  componentSize + padding, height * componentSize + padding, buttonSize, inputSize, ButtonColor.YELLOW, this.getInterfaceStyle().keyboard_next
               )
            )
            .onClicked(e -> {
               this.currentForm = (this.currentForm + 1) % this.normalForms.size();
               this.refreshCurrentKeyboard();
            })
            .controllerFocusHashcode = "keyboardChange";
         FormContentIconButton capSpacebarButton = capitalizedForm.addComponent(
            new FormContentIconButton(
               componentSize * 2 + padding,
               height * componentSize + padding,
               spacebarWidth * buttonSize + padding * (spacebarWidth - 1) * 2,
               inputSize,
               ButtonColor.YELLOW,
               this.getInterfaceStyle().keyboard_spacebar
            )
         );
         capSpacebarButton.onClicked(e -> this.submitText(" "));
         capSpacebarButton.controllerFocusHashcode = "keyboardSpace";
         capSpacebarButton.acceptMouseRepeatEvents = true;
         capSpacebarButton.setActive(typingComponent.isValidAppendText(" "));
         FormContentIconButton capBackspaceButton = capitalizedForm.addComponent(
            new FormContentIconButton(
               componentSize * 2 + spacebarWidth * componentSize + padding,
               height * componentSize + padding,
               buttonSize * 2 + padding * 2,
               inputSize,
               ButtonColor.YELLOW,
               this.getInterfaceStyle().keyboard_backspace
            )
         );
         capBackspaceButton.onClicked(e -> this.submitBackspace());
         capBackspaceButton.controllerFocusHashcode = "keyboardBack";
         capBackspaceButton.acceptMouseRepeatEvents = true;
         capitalizedForm.addComponent(
               new FormContentIconButton(
                  componentSize * 4 + spacebarWidth * componentSize + padding,
                  height * componentSize + padding,
                  buttonSize,
                  inputSize,
                  ButtonColor.GREEN,
                  this.getInterfaceStyle().keyboard_return
               )
            )
            .onClicked(e -> this.submitEnter(e.event))
            .controllerFocusHashcode = "keyboardEnter";
      }

      this.refreshCurrentKeyboard();
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleInputEvent(event, tickManager, perspective);
      if (!event.isUsed()) {
         if (this.typingComponent.submitTypingEvent(event, false)) {
            this.label.drawOptions = this.typingComponent.getTextBoxDrawOptions();
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleControllerEvent(event, tickManager, perspective);
      if (!event.isUsed()) {
         if ((!event.buttonState || event.getState() != ControllerInput.MENU_ITEM_ACTIONS_MENU) && !event.isRepeatEvent(this.repeatSpacebar)) {
            if ((!event.buttonState || event.getState() != ControllerInput.MENU_NEXT) && !event.isRepeatEvent(this.repeatBackspace)) {
               if (event.buttonState && event.getState() == ControllerInput.MENU_PREV) {
                  this.capitalized = !this.capitalized;
                  this.refreshCurrentKeyboard();
                  if (event.shouldSubmitSound()) {
                     this.playTickSound();
                  }

                  event.use();
               }
            } else {
               event.startRepeatEvents(this.repeatBackspace);
               this.submitBackspace();
               if (event.shouldSubmitSound()) {
                  this.playTickSound();
               }

               event.use();
            }
         } else {
            if (this.typingComponent.isValidAppendText(" ")) {
               event.startRepeatEvents(this.repeatSpacebar);
               this.submitText(" ");
               if (event.shouldSubmitSound()) {
                  this.playTickSound();
               }
            }

            event.use();
         }
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      Form form = this.capitalized ? this.capitalizedForms.get(this.currentForm) : this.normalForms.get(this.currentForm);
      Rectangle boundingBox = this.label.getBoundingBox();
      GameWindow window = WindowManager.getWindow();
      if (this.label.drawOptions != null) {
         switch (this.label.drawOptions.align) {
            case LEFT:
               this.label.setPosition(window.getHudWidth() / 2 - boundingBox.width / 2, form.getY() - boundingBox.height - 30);
               break;
            case CENTER:
               this.label.setPosition(window.getHudWidth() / 2, form.getY() - boundingBox.height - 30);
               break;
            case RIGHT:
               this.label.setPosition(window.getHudWidth() / 2 + boundingBox.width / 2, form.getY() - boundingBox.height - 30);
         }
      } else {
         this.label.setPosition(window.getHudWidth() / 2, form.getY() - boundingBox.height - 30);
      }

      if (this.header != null) {
         this.header.setPosition(window.getHudWidth() / 2, this.label.getY() - this.header.getHeight() - 15);
      }

      GameBackground.textBox.getDrawOptions(boundingBox.x - 8, boundingBox.y - 8, boundingBox.width + 16, boundingBox.height + 16).draw();
      super.draw(tickManager, perspective, renderBox);
      GameBackground.textBox.getEdgeDrawOptions(boundingBox.x - 8, boundingBox.y - 8, boundingBox.width + 16, boundingBox.height + 16).draw();
      GameTooltipManager.addControllerGlyph(Localization.translate("controls", "spacebartip"), ControllerInput.MENU_ITEM_ACTIONS_MENU);
      GameTooltipManager.addControllerGlyph(Localization.translate("controls", "backspacetip"), ControllerInput.MENU_NEXT);
      GameTooltipManager.addControllerGlyph(Localization.translate("controls", "capitalizetip"), ControllerInput.MENU_PREV);
   }

   public void refreshCurrentKeyboard() {
      Form form = this.capitalized ? this.capitalizedForms.get(this.currentForm) : this.normalForms.get(this.currentForm);
      GameWindow window = WindowManager.getWindow();
      form.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
      this.keyboardSwitcher.makeCurrent(form);
   }

   public void submitText(String text) {
      this.typingComponent.appendText(text);
      this.label.drawOptions = this.typingComponent.getTextBoxDrawOptions();
   }

   public void submitBackspace() {
      this.typingComponent.submitBackspace();
      this.label.drawOptions = this.typingComponent.getTextBoxDrawOptions();
   }

   public abstract void submitEnter(InputEvent var1);
}
