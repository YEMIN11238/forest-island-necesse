package necesse.gfx.forms.presets;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import necesse.engine.GameAuth;
import necesse.engine.GameLaunch;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.save.CharacterSave;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.HumanLook;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormCharacterSaveComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

public abstract class CharacterSelectForm extends FormSwitcher {
   protected boolean worldHasCheatsEnabled;
   protected boolean worldIsCreative;
   protected boolean isOwner;
   protected Form selectForm;
   protected FormContentBox charactersBox;
   protected Thread loadThread;
   protected NewCharacterForm createNewForm;
   protected ArrayList<CharacterSave> extraCharacters = new ArrayList<>();
   protected ArrayList<FormCharacterSaveComponent> addedCharacters = new ArrayList<>();

   public CharacterSelectForm(GameMessage backButton, boolean worldHasCheatsEnabled, boolean worldIsCreative, boolean isOwner) {
      this.worldHasCheatsEnabled = worldHasCheatsEnabled;
      this.worldIsCreative = worldIsCreative;
      this.isOwner = isOwner;
      FormFlow selectFlow = new FormFlow(10);
      this.selectForm = this.addComponent(new Form("selectCharacter", 400, 10));
      this.selectForm.addComponent(selectFlow.nextY(new FormLocalLabel("ui", "selectcharacter", new FontOptions(20), 0, this.selectForm.getWidth() / 2, 0), 5));
      LocalMessage warningMessage = null;
      if (worldIsCreative && worldHasCheatsEnabled) {
         warningMessage = new LocalMessage("ui", "characterworldcheatsandcreativeenabled");
      } else if (worldIsCreative) {
         warningMessage = new LocalMessage("ui", "characterworldcreativeenabled");
      } else if (worldHasCheatsEnabled) {
         warningMessage = new LocalMessage("ui", "characterworldcheatsenabled");
      }

      if (warningMessage != null) {
         this.selectForm
            .addComponent(
               selectFlow.nextY(
                  new FormLocalLabel(
                     warningMessage,
                     new FontOptions(12).color(this.getInterfaceStyle().errorTextColor),
                     0,
                     this.selectForm.getWidth() / 2,
                     0,
                     this.selectForm.getWidth() - 20
                  ),
                  5
               )
            );
      }

      selectFlow.next(5);
      this.charactersBox = this.selectForm.addComponent(selectFlow.nextY(new FormContentBox(0, 0, this.selectForm.getWidth(), 350), 4));
      this.selectForm
         .addComponent(
            selectFlow.nextY(
               new FormLocalTextButton("ui", "createnewcharacter", 4, 0, this.selectForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE), 4
            )
         )
         .onClicked(e -> this.makeCurrent(this.createNewForm));
      this.selectForm
         .addComponent(selectFlow.nextY(new FormLocalTextButton(backButton, 4, 0, this.selectForm.getWidth() - 8, FormInputSize.SIZE_32, ButtonColor.BASE), 4))
         .onClicked(e -> this.onBackPressed());
      this.selectForm.setHeight(selectFlow.next());
      this.createNewForm = this.addComponent(new NewCharacterForm("createCharacter") {
         @Override
         public void onCreatePressed(PlayerMob player) {
            CharacterSave character = CharacterSave.newCharacter(player, 0L);
            CharacterSave.saveCharacter(character, null, false);
            CharacterSelectForm.this.loadCharacters();
            CharacterSelectForm.this.makeCurrent(CharacterSelectForm.this.selectForm);
            this.reset();
         }

         @Override
         public void onCancelPressed() {
            CharacterSelectForm.this.makeCurrent(CharacterSelectForm.this.selectForm);
         }
      });
      this.onWindowResized(WindowManager.getWindow());
      this.makeCurrent(this.selectForm);
   }

   public abstract void onSelected(File var1, CharacterSave var2);

   public abstract void onBackPressed();

   public abstract void onDownloadPressed(int var1);

   @Override
   protected void init() {
      synchronized (this) {
         super.init();
      }
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      synchronized (this) {
         super.handleInputEvent(event, tickManager, perspective);
         if (!event.isUsed() && event.state && event.getID() == 256 && !this.isCurrent(this.selectForm)) {
            this.makeCurrent(this.selectForm);
            event.use();
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      synchronized (this) {
         super.handleControllerEvent(event, tickManager, perspective);
         if (!event.isUsed()
            && event.buttonState
            && (event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU)
            && !this.isCurrent(this.selectForm)) {
            this.makeCurrent(this.selectForm);
            event.use();
         }
      }
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      synchronized (this) {
         super.addNextControllerFocus(list, currentXOffset, currentYOffset, customNavigationHandler, area, draw);
      }
   }

   public void addExtraCharacter(int serverCharacterUniqueID, PlayerMob player, long timePlayed) {
      this.extraCharacters.add(CharacterSave.newCharacter(serverCharacterUniqueID, player, timePlayed));
   }

   public FormFlow refreshFlow() {
      FormFlow flow = new FormFlow(0);
      synchronized (this) {
         for (FormCharacterSaveComponent comp : this.addedCharacters) {
            flow.nextY(comp);
         }

         this.charactersBox.setContentBox(new Rectangle(this.charactersBox.getWidth(), flow.next()));
         return flow;
      }
   }

   public void onCharacterLoaded(File file, CharacterSave character, boolean forceInFront) {
      synchronized (this) {
         FormCharacterSaveComponent last = this.addedCharacters
            .stream()
            .filter(c -> c.character.characterUniqueID == character.characterUniqueID)
            .filter(c -> c.file != null || c.character.timePlayed < character.timePlayed + 60L)
            .findFirst()
            .orElse(null);
         if (last != null && character.getTimeModified() < last.character.getTimeModified()) {
            GameLog.warn
               .println(
                  "Found older character file ("
                     + file.getName()
                     + ") with same uniqueID as newer character ("
                     + (last.file == null ? null : last.file.getName())
                     + ")."
               );
         } else {
            FormCharacterSaveComponent comp = new FormCharacterSaveComponent(
               this.charactersBox.getMinContentWidth(), file, character, this.worldHasCheatsEnabled, this.worldIsCreative, this.isOwner
            ) {
               @Override
               public void onSelectPressed() {
                  CharacterSelectForm.this.selectCharacter(this.file, this.character);
               }

               @Override
               public void onRenamePressed() {
                  CharacterSelectForm.this.startRename(this.file, this.character);
               }

               @Override
               public void onDeletePressed() {
                  CharacterSelectForm.this.startDeleteConfirm(this.file, this.character);
               }

               @Override
               public void onDownloadPressed() {
               }
            };
            if (forceInFront) {
               if (last != null) {
                  this.charactersBox.removeComponent(last);
                  this.charactersBox.addComponent(comp);
                  this.addedCharacters.remove(last);
                  this.addedCharacters.add(0, comp);
               } else {
                  this.charactersBox.addComponent(comp);
                  this.addedCharacters.add(0, comp);
               }
            } else if (last != null) {
               this.charactersBox.removeComponent(last);
               this.charactersBox.addComponent(comp);
               int lastIndex = this.addedCharacters.indexOf(last);
               this.addedCharacters.remove(lastIndex);
               this.addedCharacters.add(lastIndex, comp);
            } else {
               this.charactersBox.addComponent(comp);
               this.addedCharacters.add(comp);
            }

            this.refreshFlow();
         }
      }
   }

   public void loadCharacters() {
      synchronized (this) {
         if (this.loadThread != null) {
            this.loadThread.interrupt();
         }

         this.charactersBox.clearComponents();
         this.addedCharacters.clear();

         for (final CharacterSave extraCharacter : this.extraCharacters) {
            FormCharacterSaveComponent comp = this.charactersBox
               .addComponent(
                  new FormCharacterSaveComponent(
                     this.charactersBox.getMinContentWidth(), null, extraCharacter, this.worldHasCheatsEnabled, this.worldIsCreative, this.isOwner
                  ) {
                     @Override
                     public void onSelectPressed() {
                        CharacterSelectForm.this.onSelected(this.file, this.character);
                     }

                     @Override
                     public void onRenamePressed() {
                     }

                     @Override
                     public void onDeletePressed() {
                     }

                     @Override
                     public void onDownloadPressed() {
                        CharacterSelectForm.this.onDownloadPressed(extraCharacter.characterUniqueID);
                     }
                  }
               );
            this.addedCharacters.add(comp);
         }

         this.refreshFlow();
         this.loadThread = new Thread("CharacterLoader") {
            @Override
            public void run() {
               CharacterSave.loadCharacters(
                  (file, character) -> {
                     boolean shouldBeInFront = CharacterSelectForm.this.extraCharacters
                        .stream()
                        .anyMatch(c -> c.characterUniqueID == character.characterUniqueID);
                     CharacterSelectForm.this.onCharacterLoaded(file, character, shouldBeInFront);
                  },
                  this::isInterrupted,
                  interrupted -> {
                     if (!interrupted) {
                        if (CharacterSelectForm.this.addedCharacters.isEmpty()) {
                           CharacterSelectForm.this.charactersBox
                              .addComponent(
                                 new FormLocalLabel(
                                    "ui",
                                    "nocharacterstip",
                                    new FontOptions(16),
                                    0,
                                    CharacterSelectForm.this.charactersBox.getWidth() / 2,
                                    10,
                                    CharacterSelectForm.this.charactersBox.getMinContentWidth() - 10
                                 )
                              );
                        } else if (GameLaunch.useCharacter != null) {
                           FormCharacterSaveComponent found;
                           if (GameLaunch.useCharacter.isEmpty()) {
                              found = CharacterSelectForm.this.addedCharacters
                                 .stream()
                                 .filter(c -> c.character.lastUsedAuth == GameAuth.getAuthentication())
                                 .findFirst()
                                 .orElse(CharacterSelectForm.this.addedCharacters.get(0));
                           } else {
                              found = CharacterSelectForm.this.addedCharacters
                                 .stream()
                                 .filter(c -> c.character.player.playerName.equals(GameLaunch.useCharacter))
                                 .findFirst()
                                 .orElse(null);
                           }

                           if (found != null) {
                              CharacterSelectForm.this.selectCharacter(found.file, found.character);
                           } else if (!GameLaunch.useCharacter.isEmpty()) {
                              PlayerMob player = new PlayerMob(0L, null);
                              player.look = new HumanLook(GameRandom.globalRandom, false);
                              player.playerName = GameLaunch.useCharacter;
                              player.getInv().giveStarterItems();
                              CharacterSave character = CharacterSave.newCharacter(player, 0L);
                              File file = CharacterSave.saveCharacter(character, null, false);
                              System.out.println("Created new character with launch option name: " + GameLaunch.useCharacter + " to file: " + file.getName());
                              CharacterSelectForm.this.selectCharacter(file, character);
                           }
                        }
                     }
                  },
                  -1
               );
            }
         };
         this.loadThread.start();
      }
   }

   protected void selectCharacter(File file, CharacterSave character) {
      boolean hasUnsavedCharacter = !this.extraCharacters.isEmpty()
         && this.extraCharacters
            .stream()
            .anyMatch(c -> this.addedCharacters.stream().anyMatch(c2 -> c2.character.characterUniqueID == c.characterUniqueID && c2.file == null));
      Runnable onSelected;
      if (hasUnsavedCharacter) {
         onSelected = () -> {
            ConfirmationForm confirmFormx = new ConfirmationForm("confirmSelect");
            confirmFormx.setupConfirmation(new LocalMessage("ui", "loadnewchararacterwarning"), () -> {
               this.onSelected(file, character);
               this.makeCurrent(this.selectForm);
            }, () -> this.makeCurrent(this.selectForm));
            this.addComponent(confirmFormx, (component, active) -> {
               if (!active) {
                  this.removeComponent(component);
               }
            });
            this.makeCurrent(confirmFormx);
         };
      } else {
         onSelected = () -> this.onSelected(file, character);
      }

      ArrayList<LocalMessage> warnings = new ArrayList<>();
      if (this.worldIsCreative && !character.creativeEnabled) {
         warnings.add(new LocalMessage("ui", "loadcharacterwithoutcreativewarning"));
      } else if (character.creativeEnabled && !this.worldIsCreative) {
         warnings.add(new LocalMessage("ui", "loadcharacterwithcreativewarning"));
      }

      if (this.worldHasCheatsEnabled && !character.cheatsEnabled) {
         warnings.add(new LocalMessage("ui", "loadcharacterwithoutcheatswarning"));
      } else if (character.cheatsEnabled && !this.worldHasCheatsEnabled) {
         warnings.add(new LocalMessage("ui", "loadcharacterwithcheatswarning"));
      }

      if (warnings.isEmpty()) {
         onSelected.run();
      } else {
         boolean isLast = true;
         ConfirmationForm lastConfirm = null;

         for (int i = warnings.size() - 1; i >= 0; i--) {
            LocalMessage warning = warnings.get(i);
            ConfirmationForm confirmForm = new ConfirmationForm("confirmSelect");
            ConfirmationForm finalLastConfirm = lastConfirm;
            confirmForm.setupConfirmation(warning, isLast ? onSelected : () -> this.makeCurrent(finalLastConfirm), () -> this.makeCurrent(this.selectForm));
            this.addComponent(confirmForm, (component, active) -> {
               if (!active) {
                  this.removeComponent(component);
               }
            });
            if (i == 0) {
               this.makeCurrent(confirmForm);
            }

            lastConfirm = confirmForm;
            isLast = false;
         }
      }
   }

   protected void startRename(final File filePath, final CharacterSave character) {
      LabeledTextInputForm renameForm = new LabeledTextInputForm(
         "renameInput",
         new LocalMessage("ui", "renamecharacter"),
         false,
         GameUtils.playerNameSymbolsPattern,
         new LocalMessage("ui", "renamebutton"),
         new LocalMessage("ui", "backbutton")
      ) {
         @Override
         public GameMessage getInputError(String text) {
            return GameUtils.isValidPlayerName(text);
         }

         @Override
         public void onConfirmed(String text) {
            if (!text.equals(character.player.playerName)) {
               character.player.playerName = text;
               File newFile = CharacterSave.saveCharacter(character, null, false);
               if (!newFile.getName().equals(filePath.getName())) {
                  CharacterSave.deleteCharacter(filePath);
               }
            }

            CharacterSelectForm.this.makeCurrent(CharacterSelectForm.this.selectForm);
            CharacterSelectForm.this.loadCharacters();
         }

         @Override
         public void onCancelled() {
            CharacterSelectForm.this.makeCurrent(CharacterSelectForm.this.selectForm);
         }
      };
      renameForm.setInput(character.player.playerName);
      this.addComponent(renameForm, (component, active) -> {
         if (!active) {
            this.removeComponent(component);
         }
      });
      this.makeCurrent(renameForm);
      renameForm.selectAllAndSetTyping();
   }

   protected void startDeleteConfirm(File filePath, CharacterSave character) {
      ConfirmationForm confirmForm = new ConfirmationForm("deleteCharacter");
      confirmForm.setupConfirmation(new LocalMessage("ui", "confirmdeletecharacter", "character", character.player.playerName), () -> {
         System.out.println("Deleting character: " + filePath.getName());
         CharacterSave.deleteCharacter(filePath);
         this.extraCharacters.removeIf(c -> c.characterUniqueID == character.characterUniqueID);
         this.makeCurrent(this.selectForm);
         this.loadCharacters();
      }, () -> this.makeCurrent(this.selectForm));
      this.addComponent(confirmForm, (component, active) -> {
         if (!active) {
            this.removeComponent(component);
         }
      });
      this.makeCurrent(confirmForm);
   }

   public boolean isCharacterUniqueIDOccupied(int uniqueID) {
      synchronized (this) {
         return this.addedCharacters.stream().anyMatch(c -> c.character.characterUniqueID == uniqueID)
            || this.extraCharacters.stream().anyMatch(c -> c.characterUniqueID == uniqueID);
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      synchronized (this) {
         super.draw(tickManager, perspective, renderBox);
      }
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.selectForm.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
      this.createNewForm.onWindowResized(window);
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.loadThread != null) {
         this.loadThread.interrupt();
      }

      this.loadThread = null;
   }

   protected static class CharacterCompData {
   }
}
