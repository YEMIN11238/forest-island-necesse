package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.MouseWheelBuffer;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketRemoveDeathLocations;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldGenerator;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameColor;
import necesse.gfx.PlayerSprite;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.forms.FormClickHandler;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerFocusHandler;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.floatMenu.SelectionFloatMenu;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.forms.presets.containerComponent.TravelContainerComponent;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.FairTypeTooltip;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.HUD;
import necesse.gfx.ui.HoverStateTextures;
import necesse.inventory.container.travel.IslandData;
import necesse.inventory.container.travel.IslandsResponseEvent;
import necesse.inventory.container.travel.TravelContainer;
import necesse.inventory.container.travel.TravelDir;
import necesse.level.maps.biomes.Biome;

public class FormTravelContainerGrid extends FormComponent implements FormPositionContainer {
   private FormPosition position;
   private final int width;
   private final int height;
   private final MouseWheelBuffer wheelBuffer = new MouseWheelBuffer(true);
   public final Client client;
   public final TravelContainer travelContainer;
   public final TravelContainerComponent travelContainerForm;
   private int gridStartX;
   private int gridStartY;
   public final int gridWidth;
   public final int gridHeight;
   private final FormTravelContainerGrid.FormDestination[][] destinations;
   private final FormClickHandler topArrowHandler;
   private final FormClickHandler botArrowHandler;
   private final FormClickHandler leftArrowHandler;
   private final FormClickHandler rightArrowHandler;
   private final FormClickHandler topRightArrowHandler;
   private final FormClickHandler topLeftArrowHandler;
   private final FormClickHandler botRightArrowHandler;
   private final FormClickHandler botLeftArrowHandler;
   private boolean isHoveringTop;
   private boolean isHoveringBot;
   private boolean isHoveringLeft;
   private boolean isHoveringRight;
   private final List<Rectangle> expectedRequests;
   public int minIslandX;
   public int minIslandY;
   public int maxIslandX;
   public int maxIslandY;
   private Point controllerSelected = null;

   public FormTravelContainerGrid(
      FormTravelContainerGrid last,
      int x,
      int y,
      int width,
      int height,
      Client client,
      TravelContainer travelContainer,
      TravelContainerComponent travelContainerForm
   ) {
      if (width >= 80 && height >= 80) {
         this.position = new FormFixedPosition(x, y);
         this.width = width;
         this.height = height;
         this.client = client;
         this.travelContainer = travelContainer;
         this.travelContainerForm = travelContainerForm;
         this.minIslandX = Integer.MIN_VALUE;
         this.minIslandY = Integer.MIN_VALUE;
         this.maxIslandX = Integer.MAX_VALUE;
         this.maxIslandY = Integer.MAX_VALUE;
         this.expectedRequests = new ArrayList<>();
         this.gridWidth = width / 40 - 1;
         this.gridHeight = height / 40 - 1;
         this.destinations = new FormTravelContainerGrid.FormDestination[this.gridWidth][this.gridHeight];
         this.topArrowHandler = new FormClickHandler(e -> this.isMouseOverTopArrow(e) && this.canScrollUp(), -100, e -> {
            this.playTickSound();
            this.scrollUp();
         });
         this.botArrowHandler = new FormClickHandler(e -> this.isMouseOverBotArrow(e) && this.canScrollDown(), -100, e -> {
            this.playTickSound();
            this.scrollDown();
         });
         this.leftArrowHandler = new FormClickHandler(e -> this.isMouseOverLeftArrow(e) && this.canScrollLeft(), -100, e -> {
            this.playTickSound();
            this.scrollLeft();
         });
         this.rightArrowHandler = new FormClickHandler(e -> this.isMouseOverRightArrow(e) && this.canScrollRight(), -100, e -> {
            this.playTickSound();
            this.scrollRight();
         });
         this.topRightArrowHandler = new FormClickHandler(
            e -> this.isMouseOverTopArrow(e) && this.canScrollUp() && this.isMouseOverRightArrow(e) && this.canScrollRight(), -100, e -> {
               this.playTickSound();
               this.scrollUp();
               this.scrollRight();
            }
         );
         this.topLeftArrowHandler = new FormClickHandler(
            e -> this.isMouseOverTopArrow(e) && this.canScrollUp() && this.isMouseOverLeftArrow(e) && this.canScrollLeft(), -100, e -> {
               this.playTickSound();
               this.scrollUp();
               this.scrollLeft();
            }
         );
         this.botRightArrowHandler = new FormClickHandler(
            e -> this.isMouseOverBotArrow(e) && this.canScrollDown() && this.isMouseOverRightArrow(e) && this.canScrollRight(), -100, e -> {
               this.playTickSound();
               this.scrollDown();
               this.scrollRight();
            }
         );
         this.botLeftArrowHandler = new FormClickHandler(
            e -> this.isMouseOverBotArrow(e) && this.canScrollDown() && this.isMouseOverLeftArrow(e) && this.canScrollLeft(), -100, e -> {
               this.playTickSound();
               this.scrollDown();
               this.scrollLeft();
            }
         );
         TravelDir travelDir = travelContainer.travelDir;
         if (last != null) {
            int deltaWidth = this.gridWidth - last.gridWidth;
            int deltaHeight = this.gridHeight - last.gridHeight;
            if (travelDir == TravelDir.NorthWest || travelDir == TravelDir.West || travelDir == TravelDir.SouthWest) {
               this.gridStartX = last.gridStartX - deltaWidth;
            } else if (travelDir != TravelDir.NorthEast && travelDir != TravelDir.East && travelDir != TravelDir.SouthEast) {
               this.gridStartX = last.gridStartX - deltaWidth / 2;
            } else {
               this.gridStartX = last.gridStartX;
            }

            if (travelDir == TravelDir.NorthWest || travelDir == TravelDir.North || travelDir == TravelDir.NorthEast) {
               this.gridStartY = last.gridStartY - deltaHeight;
            } else if (travelDir != TravelDir.SouthWest && travelDir != TravelDir.South && travelDir != TravelDir.SouthEast) {
               this.gridStartY = last.gridStartY - deltaHeight / 2;
            } else {
               this.gridStartY = last.gridStartY;
            }
         } else {
            if (travelDir == TravelDir.NorthWest || travelDir == TravelDir.West || travelDir == TravelDir.SouthWest) {
               this.gridStartX = travelContainer.travelLevel.getIslandX() - (this.gridWidth - 1);
            } else if (travelDir != TravelDir.NorthEast && travelDir != TravelDir.East && travelDir != TravelDir.SouthEast) {
               this.gridStartX = travelContainer.travelLevel.getIslandX() - this.gridWidth / 2;
            } else {
               this.gridStartX = travelContainer.travelLevel.getIslandX();
            }

            if (travelDir == TravelDir.NorthWest || travelDir == TravelDir.North || travelDir == TravelDir.NorthEast) {
               this.gridStartY = travelContainer.travelLevel.getIslandY() - (this.gridHeight - 1);
            } else if (travelDir != TravelDir.SouthWest && travelDir != TravelDir.South && travelDir != TravelDir.SouthEast) {
               this.gridStartY = travelContainer.travelLevel.getIslandY() - this.gridHeight / 2;
            } else {
               this.gridStartY = travelContainer.travelLevel.getIslandY();
            }

            for (int gridX = 0; gridX < this.gridWidth; gridX++) {
               for (int gridY = 0; gridY < this.gridHeight; gridY++) {
                  int islandX = this.gridStartX + gridX;
                  int islandY = this.gridStartY + gridY;
                  this.destinations[gridX][gridY] = new FormTravelContainerGrid.LoadingDestination(islandX, islandY);
               }
            }
         }

         travelContainer.onEvent(
            IslandsResponseEvent.class,
            event -> this.applyRequestResponse(event.startX, event.startY, event.width, event.height, event.islands),
            () -> !this.isDisposed()
         );
         this.request(this.gridStartX, this.gridStartY, this.gridWidth, this.gridHeight);
      } else {
         throw new IllegalArgumentException("Width and height must be at least 80");
      }
   }

   public FormTravelContainerGrid(
      int x, int y, int width, int height, Client client, TravelContainer travelContainer, TravelContainerComponent travelContainerForm
   ) {
      this(null, x, y, width, height, client, travelContainer, travelContainerForm);
   }

   private void request(int startX, int startY, int width, int height) {
      this.expectedRequests.add(new Rectangle(startX, startY, width, height));
      this.travelContainer.requestIslandsAction.runAndSend(startX, startY, width, height);
   }

   public void applyRequestResponse(int startX, int startY, int width, int height, IslandData[][] islands) {
      boolean found = false;

      for (int i = 0; i < this.expectedRequests.size(); i++) {
         Rectangle request = this.expectedRequests.get(i);
         if (request.x == startX && request.y == startY && request.width == width && request.height == height) {
            found = true;
            this.expectedRequests.remove(i);
            i--;
         }
      }

      if (!found) {
         GameLog.warn.println("Received unknown travel grid response");
      }

      for (int y = Math.max(0, startY - this.gridStartY); y < this.gridHeight; y++) {
         int dataY = y + this.gridStartY - startY;
         if (dataY >= height) {
            break;
         }

         for (int x = Math.max(0, startX - this.gridStartX); x < this.gridWidth; x++) {
            int dataX = x + this.gridStartX - startX;
            if (dataX < width) {
               IslandData data = islands[dataX][dataY];
               this.destinations[x][y] = new FormTravelContainerGrid.LoadedDestination(data, x, y);
               continue;
            }
         }
      }
   }

   public void reloadNotes(int islandX, int islandY) {
      int gridX = islandX - this.gridStartX;
      int gridY = islandY - this.gridStartY;
      if (gridX >= 0 && gridY >= 0 && gridX < this.gridWidth && gridY < this.gridHeight) {
         if (this.destinations[gridX][gridY] != null) {
            this.destinations[gridX][gridY].loadNotes();
         }
      }
   }

   public void reloadClients(int islandX, int islandY) {
      int gridX = islandX - this.gridStartX;
      int gridY = islandY - this.gridStartY;
      if (gridX >= 0 && gridY >= 0 && gridX < this.gridWidth && gridY < this.gridHeight) {
         if (this.destinations[gridX][gridY] != null) {
            this.destinations[gridX][gridY].loadClients();
         }
      }
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.state && this.isMouseOverGridSpace(event) && event.isMouseWheelEvent()) {
         this.wheelBuffer.add(event);
         int wheelY = this.wheelBuffer.useAllScrollY();
         if (wheelY < 0) {
            if (this.canScrollDown()) {
               this.scrollDown();
               this.playTickSound();
            }
         } else if (wheelY > 0 && this.canScrollUp()) {
            this.scrollUp();
            this.playTickSound();
         }

         int wheelX = this.wheelBuffer.useAllScrollX();
         if (wheelX < 0) {
            if (this.canScrollRight()) {
               this.scrollRight();
               this.playTickSound();
            }
         } else if (wheelX > 0 && this.canScrollLeft()) {
            this.scrollLeft();
            this.playTickSound();
         }
      }

      this.topRightArrowHandler.handleEvent(event);
      this.topLeftArrowHandler.handleEvent(event);
      this.botRightArrowHandler.handleEvent(event);
      this.botLeftArrowHandler.handleEvent(event);
      this.topArrowHandler.handleEvent(event);
      this.botArrowHandler.handleEvent(event);
      this.leftArrowHandler.handleEvent(event);
      this.rightArrowHandler.handleEvent(event);
      if (event.isMouseMoveEvent()) {
         this.isHoveringTop = this.isMouseOverTopArrow(event);
         this.isHoveringBot = this.isMouseOverBotArrow(event);
         this.isHoveringLeft = this.isMouseOverLeftArrow(event);
         this.isHoveringRight = this.isMouseOverRightArrow(event);
         if (this.isHoveringTop || this.isHoveringBot || this.isHoveringLeft || this.isHoveringRight) {
            event.useMove();
         }
      }

      for (int gridX = 0; gridX < this.gridWidth; gridX++) {
         for (int gridY = 0; gridY < this.gridHeight; gridY++) {
            if (this.destinations[gridX][gridY] != null) {
               if (event.isMouseMoveEvent()) {
                  this.destinations[gridX][gridY].isHovering = this.isMouseOverGrid(event, gridX, gridY);
                  if (this.destinations[gridX][gridY].isHovering) {
                     event.useMove();
                  }
               }

               this.destinations[gridX][gridY].clickHandler.handleEvent(event);
            }
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.getState() == ControllerInput.MENU_SELECT) {
         if (this.isControllerFocus() && event.buttonState) {
            if (this.controllerSelected == null) {
               this.controllerSelected = new Point(this.gridWidth / 2, this.gridHeight / 2);
            } else {
               InputEvent inputEvent = InputEvent.ControllerButtonEvent(event, tickManager);
               this.destinations[this.controllerSelected.x][this.controllerSelected.y].clickHandler.forceHandleEvent(inputEvent);
            }

            event.use();
         } else if (!event.buttonState && this.controllerSelected != null) {
            InputEvent inputEvent = InputEvent.ControllerButtonEvent(event, tickManager);
            this.destinations[this.controllerSelected.x][this.controllerSelected.y].clickHandler.forceHandleEvent(inputEvent);
         }
      } else if ((event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU)
         && this.controllerSelected != null
         && event.buttonState) {
         this.controllerSelected = null;
         event.use();
      }
   }

   @Override
   public void onControllerUnfocused(ControllerFocus current) {
      super.onControllerUnfocused(current);
      this.controllerSelected = null;
   }

   @Override
   public boolean handleControllerNavigate(int dir, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.controllerSelected == null) {
         return super.handleControllerNavigate(dir, event, tickManager, perspective);
      }

      if (this.expectedRequests.size() < 4) {
         switch (dir) {
            case 0:
               if (this.controllerSelected.y > 0) {
                  if (this.controllerSelected.y <= 1 && this.canScrollUp()) {
                     this.scrollUp();
                  } else {
                     this.controllerSelected.y--;
                  }
               }

               event.use();
               break;
            case 1:
               if (this.controllerSelected.x < this.gridWidth) {
                  if (this.controllerSelected.x >= this.gridWidth - 2 && this.canScrollRight()) {
                     this.scrollRight();
                  } else {
                     this.controllerSelected.x++;
                  }
               }

               event.use();
               break;
            case 2:
               if (this.controllerSelected.y < this.gridHeight) {
                  if (this.controllerSelected.y >= this.gridHeight - 2 && this.canScrollDown()) {
                     this.scrollDown();
                  } else {
                     this.controllerSelected.y++;
                  }
               }

               event.use();
               break;
            case 3:
               if (this.controllerSelected.x > 0) {
                  if (this.controllerSelected.x <= 1 && this.canScrollLeft()) {
                     this.scrollLeft();
                  } else {
                     this.controllerSelected.x--;
                  }
               }

               event.use();
         }
      }

      return true;
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
      ControllerFocus.add(list, area, this, this.getBoundingBox(), currentXOffset, currentYOffset, this.controllerInitialFocusPriority, customNavigationHandler);
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      boolean canScrollUp = this.canScrollUp();
      boolean canScrollDown = this.canScrollDown();
      boolean canScrollLeft = this.canScrollLeft();
      boolean canScrollRight = this.canScrollRight();
      boolean mouseOverTop = canScrollUp && this.isHoveringTop;
      boolean mouseOverBot = canScrollDown && this.isHoveringBot;
      boolean mouseOverLeft = canScrollLeft && this.isHoveringLeft;
      boolean mouseOverRight = canScrollRight && this.isHoveringRight;
      int x = this.getX();
      int y = this.getY();
      if (canScrollUp) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_vertical, mouseOverTop && !mouseOverLeft && !mouseOverRight)
            .draw(x + this.width / 2 - 8, y + 10);
      }

      if (canScrollDown) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_vertical, mouseOverBot && !mouseOverLeft && !mouseOverRight)
            .mirrorY()
            .draw(x + this.width / 2 - 8, y + this.height - 20);
      }

      if (canScrollLeft) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_horizontal, mouseOverLeft && !mouseOverTop && !mouseOverBot)
            .draw(x + 10, y + this.height / 2 - 8);
      }

      if (canScrollRight) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_horizontal, mouseOverRight && !mouseOverTop && !mouseOverBot)
            .mirrorX()
            .draw(x + this.width - 20, y + this.height / 2 - 8);
      }

      if (canScrollUp && canScrollLeft) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_diagonal, mouseOverTop && mouseOverLeft).draw(x + 10, y + 10);
      }

      if (canScrollUp && canScrollRight) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_diagonal, mouseOverTop && mouseOverRight).mirrorX().draw(x + this.width - 24, y + 10);
      }

      if (canScrollDown && canScrollLeft) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_diagonal, mouseOverBot && mouseOverLeft).mirrorY().draw(x + 10, y + this.height - 24);
      }

      if (canScrollDown && canScrollRight) {
         this.initArrowDraw(this.getInterfaceStyle().button_navigate_diagonal, mouseOverBot && mouseOverRight)
            .mirrorX()
            .mirrorY()
            .draw(x + this.width - 24, y + this.height - 24);
      }

      for (int gridX = 0; gridX < this.gridWidth; gridX++) {
         for (int gridY = 0; gridY < this.gridHeight; gridY++) {
            if (this.destinations[gridX][gridY] != null) {
               this.destinations[gridX][gridY]
                  .draw(
                     x + 20 + gridX * 40,
                     y + 20 + gridY * 40,
                     this.controllerSelected != null && this.controllerSelected.x == gridX && this.controllerSelected.y == gridY
                  );
            }
         }
      }
   }

   @Override
   public void drawControllerFocus(ControllerFocus current) {
      if (this.controllerSelected != null) {
         Rectangle box = current.boundingBox;
         box = new Rectangle(box.x + 20 + this.controllerSelected.x * 40, box.y + 20 + this.controllerSelected.y * 40, 40, 40);
         int padding = 5;
         box = new Rectangle(box.x - padding, box.y - padding, box.width + padding * 2, box.height + padding * 2);
         HUD.selectBoundOptions(this.getInterfaceStyle().controllerFocusBoundsHighlightColor, true, box).draw();
      } else {
         super.drawControllerFocus(current);
         GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
      }
   }

   @Override
   public Point getControllerTooltipAndFloatMenuPoint(ControllerFocus current) {
      return this.controllerSelected != null
         ? new Point(current.boundingBox.x + 20 + this.controllerSelected.x * 40, current.boundingBox.y + 20 + this.controllerSelected.y * 40)
         : super.getControllerTooltipAndFloatMenuPoint(current);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return singleBox(new Rectangle(this.getX(), this.getY(), this.width, this.height));
   }

   private TextureDrawOptionsEnd initArrowDraw(HoverStateTextures textures, boolean isMouseOver) {
      GameTexture texture = isMouseOver ? textures.highlighted : textures.active;
      return texture.initDraw().color(isMouseOver ? this.getInterfaceStyle().highlightElementColor : this.getInterfaceStyle().activeElementColor);
   }

   public boolean canScrollUp() {
      return this.gridStartY > this.minIslandY;
   }

   public void scrollUp() {
      for (int y = this.gridHeight - 2; y >= 0; y--) {
         int newY = y + 1;

         for (int x = 0; x < this.gridWidth; x++) {
            this.destinations[x][newY] = this.destinations[x][y];
            if (this.destinations[x][newY] != null) {
               this.destinations[x][newY].changeGridPos(x, newY);
            }

            this.destinations[x][y] = null;
         }
      }

      this.gridStartY--;

      for (int x = 0; x < this.gridWidth; x++) {
         this.destinations[x][0] = new FormTravelContainerGrid.LoadingDestination(this.gridStartX + x, this.gridStartY);
      }

      this.request(this.gridStartX, this.gridStartY, this.gridWidth, 1);
      WindowManager.getWindow().submitNextMoveEvent();
   }

   public boolean canScrollDown() {
      return this.gridStartY + this.gridHeight - 1 < this.maxIslandY;
   }

   public void scrollDown() {
      for (int y = 1; y < this.gridHeight; y++) {
         int newY = y - 1;

         for (int x = 0; x < this.gridWidth; x++) {
            this.destinations[x][newY] = this.destinations[x][y];
            if (this.destinations[x][newY] != null) {
               this.destinations[x][newY].changeGridPos(x, newY);
            }

            this.destinations[x][y] = null;
         }
      }

      this.gridStartY++;

      for (int x = 0; x < this.gridWidth; x++) {
         this.destinations[x][this.gridHeight - 1] = new FormTravelContainerGrid.LoadingDestination(this.gridStartX + x, this.gridStartY + this.gridHeight - 1);
      }

      this.request(this.gridStartX, this.gridStartY + this.gridHeight - 1, this.gridWidth, 1);
      WindowManager.getWindow().submitNextMoveEvent();
   }

   public boolean canScrollLeft() {
      return this.gridStartX > this.minIslandX;
   }

   public void scrollLeft() {
      for (int x = this.gridWidth - 2; x >= 0; x--) {
         int newX = x + 1;

         for (int y = 0; y < this.gridHeight; y++) {
            this.destinations[newX][y] = this.destinations[x][y];
            if (this.destinations[newX][y] != null) {
               this.destinations[newX][y].changeGridPos(newX, y);
            }

            this.destinations[x][y] = null;
         }
      }

      this.gridStartX--;

      for (int y = 0; y < this.gridHeight; y++) {
         this.destinations[0][y] = new FormTravelContainerGrid.LoadingDestination(this.gridStartX, this.gridStartY + y);
      }

      this.request(this.gridStartX, this.gridStartY, 1, this.gridHeight);
      WindowManager.getWindow().submitNextMoveEvent();
   }

   public boolean canScrollRight() {
      return this.gridStartX + this.gridWidth - 1 < this.maxIslandX;
   }

   public void scrollRight() {
      for (int x = 1; x < this.gridWidth; x++) {
         int newX = x - 1;

         for (int y = 0; y < this.gridHeight; y++) {
            this.destinations[newX][y] = this.destinations[x][y];
            if (this.destinations[newX][y] != null) {
               this.destinations[newX][y].changeGridPos(newX, y);
            }

            this.destinations[x][y] = null;
         }
      }

      this.gridStartX++;

      for (int y = 0; y < this.gridHeight; y++) {
         this.destinations[this.gridWidth - 1][y] = new FormTravelContainerGrid.LoadingDestination(this.gridStartX + this.gridWidth - 1, this.gridStartY + y);
      }

      this.request(this.gridStartX + this.gridWidth - 1, this.gridStartY, 1, this.gridHeight);
      WindowManager.getWindow().submitNextMoveEvent();
   }

   private boolean isMouseOverTopArrow(InputEvent event) {
      return event.isMoveUsed() ? false : new Rectangle(this.getX(), this.getY(), this.width, 20).contains(event.pos.hudX, event.pos.hudY);
   }

   private boolean isMouseOverBotArrow(InputEvent event) {
      return event.isMoveUsed() ? false : new Rectangle(this.getX(), this.getY() + this.height - 20, this.width, 20).contains(event.pos.hudX, event.pos.hudY);
   }

   private boolean isMouseOverLeftArrow(InputEvent event) {
      return event.isMoveUsed() ? false : new Rectangle(this.getX(), this.getY(), 20, this.height).contains(event.pos.hudX, event.pos.hudY);
   }

   private boolean isMouseOverRightArrow(InputEvent event) {
      return event.isMoveUsed() ? false : new Rectangle(this.getX() + this.width - 20, this.getY(), 20, this.height).contains(event.pos.hudX, event.pos.hudY);
   }

   private boolean isMouseOverGrid(InputEvent event, int gridX, int gridY) {
      return event.isMoveUsed()
         ? false
         : new Rectangle(this.getX() + 20 + gridX * 40 + 2, this.getY() + 20 + gridY * 40 + 2, 36, 36).contains(event.pos.hudX, event.pos.hudY);
   }

   private boolean isMouseOverGridSpace(InputEvent event) {
      return new Rectangle(this.getX() + 20, this.getY() + 20, this.width - 40, this.height - 40).contains(event.pos.hudX, event.pos.hudY);
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   public enum CoordinateSetting {
      RELATIVE_SELF(
         new LocalMessage("ui", "travelcoordself"),
         (p, g) -> g.travelContainer.travelLevel.isIslandPosition()
            ? p.x - g.travelContainer.travelLevel.getIslandX() + ", " + (p.y - g.travelContainer.travelLevel.getIslandY())
            : null
      ),
      RELATIVE_SELF_SPAWN(
         new LocalMessage("ui", "travelcoordselfspawn"),
         (p, g) -> g.travelContainer.playerSpawnLevel.isIslandPosition()
            ? p.x - g.travelContainer.playerSpawnLevel.getIslandX() + ", " + (p.y - g.travelContainer.playerSpawnLevel.getIslandY())
            : null
      ),
      RELATIVE_WORLD_SPAWN(
         new LocalMessage("ui", "travelcoordworldspawn"),
         (p, g) -> g.travelContainer.worldSpawnLevel.isIslandPosition()
            ? p.x - g.travelContainer.worldSpawnLevel.getIslandX() + ", " + (p.y - g.travelContainer.worldSpawnLevel.getIslandY())
            : null
      ),
      GLOBAL(new LocalMessage("ui", "travelcoordglobal"), (p, g) -> p.x + "," + p.y);

      public GameMessage displayName;
      public BiFunction<Point, FormTravelContainerGrid, String> displayCoordinates;

      CoordinateSetting(GameMessage displayName, BiFunction<Point, FormTravelContainerGrid, String> displayCoordinates) {
         this.displayName = displayName;
         this.displayCoordinates = displayCoordinates;
      }
   }

   private abstract class FormDestination implements ControllerFocusHandler {
      protected FormClickHandler clickHandler;
      protected boolean isHovering;

      private FormDestination() {
      }

      public abstract void draw(int var1, int var2, boolean var3);

      public void loadNotes() {
      }

      public void loadClients() {
      }

      public void changeGridPos(int gridX, int gridY) {
      }

      @Override
      public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      }

      @Override
      public boolean handleControllerNavigate(int dir, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         return false;
      }
   }

   private class LoadedDestination extends FormTravelContainerGrid.FormDestination {
      public final IslandData destination;
      public final Biome biome;
      public boolean hasDeath;
      public FairTypeDrawOptions noteDrawOptions;
      public final boolean isWorldSpawn;
      public final boolean isPlayerSpawn;
      public final List<ClientClient> clientsThere;

      public LoadedDestination(IslandData data, int gridX, int gridY) {
         this.destination = data;
         this.biome = BiomeRegistry.getBiome(data.biome);
         this.hasDeath = data.hasDeath;
         this.isWorldSpawn = FormTravelContainerGrid.this.travelContainer.worldSpawnLevel.isIslandPosition()
            && this.destination.islandX == FormTravelContainerGrid.this.travelContainer.worldSpawnLevel.getIslandX()
            && this.destination.islandY == FormTravelContainerGrid.this.travelContainer.worldSpawnLevel.getIslandY();
         this.isPlayerSpawn = FormTravelContainerGrid.this.travelContainer.playerSpawnLevel.isIslandPosition()
            && this.destination.islandX == FormTravelContainerGrid.this.travelContainer.playerSpawnLevel.getIslandX()
            && this.destination.islandY == FormTravelContainerGrid.this.travelContainer.playerSpawnLevel.getIslandY();
         this.clientsThere = new ArrayList<>();
         this.loadNotes();
         this.loadClients();
         this.changeGridPos(gridX, gridY);
      }

      @Override
      public void loadNotes() {
         this.noteDrawOptions = null;
         String notesString = FormTravelContainerGrid.this.client.islandNotes.get(this.destination.islandX, this.destination.islandY);
         if (notesString != null && !notesString.isEmpty()) {
            FairType type = new FairType();
            FontOptions options = new FontOptions(Settings.tooltipTextSize).outline();
            type.append(options, notesString);
            type.applyParsers(TravelContainerComponent.getNoteParsers(options));
            this.noteDrawOptions = type.getDrawOptions(FairType.TextAlign.LEFT, 280, true, true);
         }
      }

      @Override
      public void loadClients() {
         this.clientsThere.clear();
         ArrayList<Integer> slots = FormTravelContainerGrid.this.travelContainer
            .friendlyClientSlots
            .get(new Point(this.destination.islandX, this.destination.islandY));

         for (int slot : slots) {
            ClientClient otherClient = FormTravelContainerGrid.this.client.getClient(slot);
            if (otherClient != null && otherClient.loadedPlayer) {
               this.clientsThere.add(otherClient);
            }
         }

         ClientClient me = FormTravelContainerGrid.this.client.getClient();

         for (int i = 0; i < FormTravelContainerGrid.this.client.getSlots(); i++) {
            if (!slots.contains(i)) {
               ClientClient cl = FormTravelContainerGrid.this.client.getClient(i);
               if (cl != null
                  && cl.loadedPlayer
                  && (cl.isSameTeam(me) || GlobalData.debugCheatActive())
                  && cl.getLevelIdentifier().isSameIsland(this.destination.islandX, this.destination.islandY)) {
                  this.clientsThere.add(cl);
               }
            }
         }
      }

      @Override
      public void changeGridPos(int gridX, int gridY) {
         this.clickHandler = new FormClickHandler(
            e -> FormTravelContainerGrid.this.isMouseOverGrid(e, gridX, gridY),
            e -> e.getID() == -100 || e.getID() == -99,
            e -> {
               if (!this.destination.isOutsideWorldBorder) {
                  FormTravelContainerGrid.this.playTickSound();
                  if (e.getID() == -99
                     || FormTravelContainerGrid.this.travelContainer.travelDir == TravelDir.None && e.getID() == -100
                     || e.isControllerEvent()) {
                     SelectionFloatMenu menu = new SelectionFloatMenu(FormTravelContainerGrid.this.travelContainerForm);
                     if (this.destination.canTravel && FormTravelContainerGrid.this.travelContainer.travelDir != TravelDir.None) {
                        menu.add(Localization.translate("ui", "travelconfirm"), () -> {
                           FormTravelContainerGrid.this.travelContainerForm.travelTo(this.destination);
                           menu.remove();
                        });
                     }

                     menu.add(Localization.translate("ui", "travelsetnotes"), () -> {
                        FormTravelContainerGrid.this.travelContainerForm.focusTravel(this.destination, this.biome);
                        menu.remove();
                     });
                     menu.add(
                        Localization.translate("ui", "clearrecentdeaths"),
                        () -> this.hasDeath,
                        null,
                        null,
                        () -> {
                           FormTravelContainerGrid.this.client
                              .network
                              .sendPacket(new PacketRemoveDeathLocations(this.destination.islandX, this.destination.islandY));
                           this.hasDeath = false;
                           if (FormTravelContainerGrid.this.client.getLevel().getIdentifier().equals(this.destination.islandX, this.destination.islandY, 0)) {
                              FormTravelContainerGrid.this.client.levelManager.clearDeathLocations();
                           }

                           menu.remove();
                        }
                     );
                     FormTravelContainerGrid.this.getManager().openFloatMenu(menu);
                  } else if (e.getID() == -100) {
                     if (this.destination.canTravel && FormTravelContainerGrid.this.travelContainer.travelDir != TravelDir.None) {
                        FormTravelContainerGrid.this.travelContainerForm.travelTo(this.destination);
                     } else {
                        FormTravelContainerGrid.this.travelContainerForm.focusTravel(this.destination, this.biome);
                     }
                  }
               }
            }
         );
      }

      @Override
      public void draw(int drawX, int drawY, boolean isControllerSelected) {
         boolean isHovering = this.isHovering || isControllerSelected;
         GameTexture borderTexture = isHovering
            ? FormTravelContainerGrid.this.getInterfaceStyle().biome_border.highlighted
            : FormTravelContainerGrid.this.getInterfaceStyle().biome_border.active;
         Color drawCol;
         if (!this.destination.isOutsideWorldBorder && (this.destination.canTravel || FormTravelContainerGrid.this.travelContainer.travelDir == TravelDir.None)
            )
          {
            drawCol = isHovering
               ? FormTravelContainerGrid.this.getInterfaceStyle().highlightElementColor
               : FormTravelContainerGrid.this.getInterfaceStyle().activeElementColor;
         } else {
            drawCol = FormTravelContainerGrid.this.getInterfaceStyle().inactiveElementColor;
         }

         this.biome.getIconTexture(isHovering).initDraw().sprite(0, 0, 32).color(drawCol).draw(drawX + 4, drawY + 4);
         borderTexture.initDraw().color(drawCol).draw(drawX, drawY);
         if (isHovering) {
            String displayCoordinates = Settings.mapCoordinates
               .displayCoordinates
               .apply(new Point(this.destination.islandX, this.destination.islandY), FormTravelContainerGrid.this);
            String islandPos = displayCoordinates == null ? "" : " (" + displayCoordinates + ")";
            String debugText = GlobalData.debugCheatActive()
               ? " (" + WorldGenerator.getIslandSize(this.destination.islandX, this.destination.islandY) + ")"
               : "";
            ListGameTooltips tooltips = new ListGameTooltips(this.biome.getDisplayName() + islandPos + debugText);
            if (this.destination.isOutsideWorldBorder) {
               tooltips.add(Localization.translate("ui", "travelserverborder"));
            } else if (!this.destination.canTravel && FormTravelContainerGrid.this.travelContainer.travelDir != TravelDir.None) {
               tooltips.add(Localization.translate("ui", "traveloutrange"));
            }

            if (this.isWorldSpawn) {
               tooltips.add(Localization.translate("ui", "travelworldspawn"));
            }

            if (!this.isWorldSpawn && this.isPlayerSpawn) {
               tooltips.add(Localization.translate("ui", "travelselfspawn"));
            }

            if (this.destination.settlementName != null) {
               tooltips.add("§#6c53ff" + this.destination.settlementName.translate());
            }

            if (this.hasDeath) {
               tooltips.add(GameColor.RED.getColorCode() + Localization.translate("misc", "recentdeath"));
            }

            if (!this.destination.isOutsideWorldBorder) {
               tooltips.add(
                  this.destination.discovered ? Localization.translate("ui", "traveldiscovered") : Localization.translate("ui", "travelnotdiscovered")
               );
               tooltips.add(this.destination.visited ? Localization.translate("ui", "travelvisited") : Localization.translate("ui", "travelnotvisited"));
            }

            if (!this.clientsThere.isEmpty()) {
               tooltips.add(Localization.translate("ui", "travelteam"));

               for (ClientClient client : this.clientsThere) {
                  tooltips.add(client.getName());
               }
            }

            if (this.noteDrawOptions != null) {
               tooltips.add(new StringTooltips(Localization.translate("ui", "travelnotes"), GameColor.PURPLE));
               tooltips.add(new FairTypeTooltip(this.noteDrawOptions));
            }

            GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
         }

         int icon = 0;
         if (this.noteDrawOptions != null) {
            FormTravelContainerGrid.this.getInterfaceStyle()
               .note_island
               .initDraw()
               .color(drawCol)
               .draw(drawX + 5 + this.getIconDrawX(icon), drawY + 5 + this.getIconDrawY(icon));
            icon++;
         }

         if (this.isWorldSpawn || this.isPlayerSpawn) {
            FormTravelContainerGrid.this.getInterfaceStyle()
               .spawn_island
               .initDraw()
               .color(drawCol)
               .draw(drawX + 5 + this.getIconDrawX(icon), drawY + 5 + this.getIconDrawY(icon));
            icon++;
         }

         if (this.destination.settlementName != null) {
            FormTravelContainerGrid.this.getInterfaceStyle()
               .settlement_island
               .initDraw()
               .color(drawCol)
               .draw(drawX + 5 + this.getIconDrawX(icon), drawY + 5 + this.getIconDrawY(icon));
            icon++;
         }

         if (this.hasDeath) {
            FormTravelContainerGrid.this.getInterfaceStyle()
               .deathmarker_note
               .initDraw()
               .color(drawCol)
               .draw(drawX + 5 + this.getIconDrawX(icon), drawY + 5 + this.getIconDrawY(icon));
            icon++;
         }

         if (this.destination.visited) {
            FormTravelContainerGrid.this.getInterfaceStyle()
               .visited_note
               .initDraw()
               .color(drawCol)
               .draw(drawX + 5 + this.getIconDrawX(icon), drawY + 5 + this.getIconDrawY(icon));
            icon++;
         }

         if (FormTravelContainerGrid.this.travelContainer.travelLevel.isSameIsland(this.destination.islandX, this.destination.islandY)) {
            PlayerSprite.drawInForms(
               (inDrawX, inDrawY) -> PlayerSprite.getIconDrawOptions(inDrawX, inDrawY, FormTravelContainerGrid.this.client.getPlayer()).draw(),
               drawX + 4,
               drawY + 4
            );
         } else if (!this.clientsThere.isEmpty()) {
            int index = (int)(System.currentTimeMillis() / 1000L % this.clientsThere.size());
            PlayerMob player = this.clientsThere.get(index).playerMob;
            PlayerSprite.drawInForms((inDrawX, inDrawY) -> PlayerSprite.getIconDrawOptions(inDrawX, inDrawY, player).draw(), drawX + 4, drawY + 4);
         }
      }

      private int getIconDrawX(int icon) {
         if (icon > 3) {
            icon++;
         }

         return icon / 3 * 10;
      }

      private int getIconDrawY(int icon) {
         if (icon > 3) {
            icon++;
         }

         return icon % 3 * 10;
      }
   }

   private class LoadingDestination extends FormTravelContainerGrid.FormDestination {
      public final int islandX;
      public final int islandY;
      public final Biome biome;

      public LoadingDestination(int islandX, int islandY) {
         this.islandX = islandX;
         this.islandY = islandY;
         this.biome = BiomeRegistry.UNKNOWN;
         this.clickHandler = new FormClickHandler(e -> false, e -> false, e -> {});
      }

      @Override
      public void draw(int drawX, int drawY, boolean isControllerSelected) {
         boolean isHovering = this.isHovering || isControllerSelected;
         Color drawCol = isHovering
            ? FormTravelContainerGrid.this.getInterfaceStyle().highlightElementColor
            : FormTravelContainerGrid.this.getInterfaceStyle().activeElementColor;
         GameTexture borderTexture = isHovering
            ? FormTravelContainerGrid.this.getInterfaceStyle().biome_border.highlighted
            : FormTravelContainerGrid.this.getInterfaceStyle().biome_border.active;
         drawCol = new Color(drawCol.getRed(), drawCol.getGreen(), drawCol.getBlue(), 100);
         this.biome.getIconTexture(isHovering).initDraw().sprite(0, 0, 32).color(drawCol).draw(drawX + 4, drawY + 4);
         borderTexture.initDraw().color(drawCol).draw(drawX, drawY);
         if (isHovering) {
            String displayCoordinates = Settings.mapCoordinates.displayCoordinates.apply(new Point(this.islandX, this.islandY), FormTravelContainerGrid.this);
            String islandPos = displayCoordinates == null ? "" : " (" + displayCoordinates + ")";
            String debugText = GlobalData.debugCheatActive() ? " (" + WorldGenerator.getIslandSize(this.islandX, this.islandY) + ")" : "";
            StringTooltips tt = new StringTooltips(this.biome.getDisplayName() + islandPos + debugText);
            tt.add(Localization.translate("ui", "travelloadingisland"));
            GameTooltipManager.addTooltip(tt, TooltipLocation.FORM_FOCUS);
         }
      }
   }
}
