package necesse.gfx.forms.presets;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModListData;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.modLoader.ModSaveInfo;
import necesse.engine.window.GameWindow;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.lists.FormModListSimpleElement;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.StringTooltips;

public abstract class ModSaveListMismatchForm extends FormSwitcher {
   private ConfirmationForm intro = this.addComponent(new ConfirmationForm("intro"));
   private Form review = this.addComponent(new Form("review", 640, 320));
   private ModSaveConfirmationForm useSavesConfirmation;
   private FormContentBox saveModsContent;
   private FormContentBox myModsContent;
   private FormTextButton useSaves;
   private List<ModListData> modsList;

   public ModSaveListMismatchForm() {
      this.review.addComponent(new FormLocalLabel("ui", "worldmods", new FontOptions(20), 0, this.review.getWidth() / 4, 2));
      this.saveModsContent = this.review.addComponent(new FormContentBox(4, 24, this.review.getWidth() / 2 - 8, this.review.getHeight() - 64));
      this.review.addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, this.review.getWidth() / 2, 4, this.review.getHeight() - 40 - 8, false));
      this.review.addComponent(new FormLocalLabel("ui", "mymods", new FontOptions(20), 0, this.review.getWidth() - this.review.getWidth() / 4, 2));
      this.myModsContent = this.review
         .addComponent(new FormContentBox(this.review.getWidth() / 2 + 4, 24, this.review.getWidth() / 2 - 8, this.review.getHeight() - 64));
      this.useSaves = this.review.addComponent(new FormLocalTextButton("ui", "useworldmods", 4, this.review.getHeight() - 40, this.review.getWidth() / 2 - 6));
      this.useSaves.onClicked(e -> {
         if (this.modsList != null) {
            ModLoader.saveModListSettings(this.modsList);
         } else {
            System.err.println("Error saving mod list");
         }

         this.makeCurrent(this.useSavesConfirmation);
      });
      this.review
         .addComponent(
            new FormLocalTextButton("ui", "backbutton", this.review.getWidth() / 2 + 2, this.review.getHeight() - 40, this.review.getWidth() / 2 - 6)
         )
         .onClicked(e -> this.backPressed());
      this.useSavesConfirmation = this.addComponent(new ModSaveConfirmationForm("useserverconf"));
      this.useSavesConfirmation.setupModSaveConfirmation(this::backPressed);
      this.makeCurrent(this.intro);
   }

   public void setup(List<ModSaveInfo> loadList) {
      this.intro
         .setupConfirmation(
            new LocalMessage("ui", "modmismatchworld"),
            new LocalMessage("ui", "modloadsaveanyway"),
            new LocalMessage("ui", "modreview"),
            this::loadAnywayPressed,
            () -> this.makeCurrent(this.review)
         );
      this.saveModsContent.clearComponents();
      this.myModsContent.clearComponents();
      FormFlow saveModsFlow = new FormFlow();
      FormFlow myModsFlow = new FormFlow();
      List<LoadedMod> myMods = new ArrayList<>(ModLoader.getAllMods());
      Comparator<LoadedMod> comparator = Comparator.comparingInt(m -> m.isEnabled() ? -1000 : m.loadLocation.modProvider.getLoadOrder());
      comparator = comparator.thenComparing(m -> m.isEnabled() ? "" : m.name);
      myMods.sort(comparator);
      boolean hasAllMods = true;
      this.modsList = new ArrayList<>();
      if (loadList.isEmpty()) {
         saveModsFlow.next(20);
         this.saveModsContent
            .addComponent(
               saveModsFlow.nextY(
                  new FormLocalLabel("ui", "nomodsused", new FontOptions(16), 0, this.saveModsContent.getWidth() / 2, 0, this.saveModsContent.getWidth() - 20),
                  10
               )
            );
      }

      int myModsIndex = 0;
      int loadListIndex = 0;

      while (true) {
         LoadedMod myMod = myModsIndex < myMods.size() ? myMods.get(myModsIndex) : null;
         if (myMod != null && !myMod.isEnabled()) {
            this.myModsContent
               .addComponent(
                  myModsFlow.nextY(
                     new FormModListSimpleElement(
                        myMod.name + " " + myMod.version,
                        this.myModsContent.getMinContentWidth(),
                        myMod.getSaveType(),
                        false,
                        this.getInterfaceStyle().inactiveTextColor,
                        Localization.translate("ui", "moddisabled")
                     )
                  )
               );
            ModListData data = new ModListData(myMod);
            data.enabled = false;
            this.modsList.add(data);
            myModsIndex++;
         } else {
            ModSaveInfo loadMod = loadListIndex < loadList.size() ? loadList.get(loadListIndex) : null;
            if (loadMod == null) {
               if (myMod == null) {
                  this.useSaves.setActive(hasAllMods);
                  if (!hasAllMods) {
                     this.useSaves.setTooltip(Localization.translate("ui", "modmissingsome"));
                     this.modsList = null;
                  } else {
                     this.useSaves.setTooltip(null);
                  }

                  this.myModsContent.setContentBox(new Rectangle(this.myModsContent.getWidth(), myModsFlow.next()));
                  this.saveModsContent.setContentBox(new Rectangle(this.saveModsContent.getWidth(), saveModsFlow.next()));
                  this.makeCurrent(this.intro);
                  return;
               }

               this.myModsContent
                  .addComponent(
                     myModsFlow.nextY(
                        new FormModListSimpleElement(
                           myMod.name + " " + myMod.version,
                           this.myModsContent.getMinContentWidth(),
                           myMod.getSaveType(),
                           true,
                           this.getInterfaceStyle().activeTextColor
                        )
                     )
                  );
               ModListData data = new ModListData(myMod);
               data.enabled = false;
               this.modsList.add(data);
               myModsIndex++;
            } else {
               if (myMod != null) {
                  if (this.modsList.stream().noneMatch(m -> m.id.equals(myMod.id))) {
                     ModListData data = new ModListData(myMod);
                     data.enabled = false;
                     this.modsList.add(data);
                  }

                  this.myModsContent
                     .addComponent(
                        myModsFlow.nextY(
                           new FormModListSimpleElement(
                              myMod.name + " " + myMod.version,
                              this.myModsContent.getMinContentWidth(),
                              myMod.getSaveType(),
                              true,
                              this.getInterfaceStyle().activeTextColor
                           )
                        )
                     );
                  myModsIndex++;
               }

               boolean hasMod = myMods.stream().anyMatch(m -> m.id.equals(loadMod.id));
               boolean correctVersion = hasMod && myMods.stream().anyMatch(m -> m.id.equals(loadMod.id) && m.version.equals(loadMod.version));
               Color color = this.getInterfaceStyle().activeTextColor;
               StringTooltips tooltips = null;
               if (!hasMod) {
                  color = this.getInterfaceStyle().errorTextColor;
                  tooltips = new StringTooltips(Localization.translate("ui", "modmissing"));
                  hasAllMods = false;
               } else if (!correctVersion) {
                  color = this.getInterfaceStyle().errorTextColor;
                  tooltips = new StringTooltips(Localization.translate("ui", "modinversion"));
                  hasAllMods = false;
               } else {
                  if (myMod == null || !myMod.id.equals(loadMod.id) || !myMod.version.equals(loadMod.version)) {
                     color = this.getInterfaceStyle().warningTextColor;
                     tooltips = new StringTooltips(Localization.translate("ui", "modinposition"));
                  }

                  LoadedMod foundMod = myMods.stream()
                     .filter(m -> m.id.equals(loadMod.id))
                     .filter(m -> m.version.equals(loadMod.version))
                     .findFirst()
                     .orElse(null);
                  if (foundMod != null) {
                     this.modsList.removeIf(d -> d.id.equals(foundMod.id));
                     this.modsList.add(new ModListData(loadMod, foundMod.loadLocation));
                  } else {
                     hasAllMods = false;
                  }
               }

               this.saveModsContent
                  .addComponent(
                     saveModsFlow.nextY(
                        new FormModListSimpleElement(
                           loadMod.name + " " + loadMod.version, this.saveModsContent.getMinContentWidth(), loadMod.type, true, color, tooltips
                        )
                     )
                  );
               loadListIndex++;
            }
         }
      }
   }

   public abstract void loadAnywayPressed();

   public abstract void backPressed();

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.intro.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
      this.review.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
      this.useSavesConfirmation.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
   }
}
