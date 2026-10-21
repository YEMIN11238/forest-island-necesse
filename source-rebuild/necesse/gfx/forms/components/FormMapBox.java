package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.client.ClientDebugMapHudDrawElement;
import necesse.engine.network.client.ClientLevelManager;
import necesse.engine.registries.MapIconRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.DrawOnMapEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.floatMenu.ContinueComponentFloatMenu;
import necesse.gfx.forms.presets.EditMapMarkerForm;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.HudDrawOnMap;
import necesse.level.maps.mapData.ClientDiscoveredMap;
import necesse.level.maps.mapData.ClientDiscoveredMapRegion;
import necesse.level.maps.mapData.DiscoveredMapBoundsExecutor;
import necesse.level.maps.mapData.MapDrawElement;

public class FormMapBox extends FormTextureMapBox {
   public final Client client;
   public final boolean isMinimap;
   private DiscoveredMapBoundsExecutor mapTextureGetter;
   private int mouseDownX;
   private int mouseDownY;
   private long mouseDownTime;

   public FormMapBox(Client client, int x, int y, int width, int height, int[] zoomLevels, int zoomLevel, boolean allowControllerFocus, boolean isMinimap) {
      super(x, y, width, height, 256, zoomLevels, zoomLevel, allowControllerFocus);
      this.client = client;
      this.isMinimap = isMinimap;
   }

   @Override
   public int getTileScale() {
      return 32;
   }

   @Override
   public Rectangle getTileBounds() {
      Level level = this.client.getLevel();
      return level != null ? new Rectangle(0, 0, level.tileWidth, level.tileHeight) : null;
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (this.isMouseOver(event) && !event.isUsed() && event.getID() == -100) {
         if (event.state
            && (this.client.getPermissionLevel().getLevel() >= PermissionLevel.ADMIN.getLevel() || this.client.worldSettings.creativeMode)
            && WindowManager.getWindow().isKeyDown(340)) {
            if (!this.client.worldSettings.cheatsAllowedOrHidden() && !this.client.worldSettings.creativeMode) {
               this.client.chat.addMessage(Localization.translate("misc", "allowcheats"));
            } else {
               int posX = this.getMouseMapPosX(event.pos.hudX);
               int posY = this.getMouseMapPosY(event.pos.hudY);
               PlayerMob player = this.client.getPlayer();
               Level level = this.client.getLevel();
               if (level != null) {
                  Rectangle edge = new Rectangle(level.tileWidth * 32, level.tileHeight * 32);
                  if (edge.width <= 0) {
                     edge.x = posX - 16;
                     edge.width = 32;
                  }

                  if (edge.height <= 0) {
                     edge.y = posY - 16;
                     edge.height = 32;
                  }

                  if (edge.contains(player.getCollision(posX, posY))) {
                     player.setPos(posX, posY, true);
                     this.client.sendMovementPacket(true);
                  }
               }
            }

            event.use();
            this.mouseDown = false;
            return;
         }

         if (event.state) {
            this.mouseDownX = event.pos.hudX;
            this.mouseDownY = event.pos.hudY;
            this.mouseDownTime = System.currentTimeMillis();
         } else {
            long timeSinceMouseDown = System.currentTimeMillis() - this.mouseDownTime;
            if (this.mouseDownX == event.pos.hudX
               && this.mouseDownY == event.pos.hudY
               && timeSinceMouseDown <= 500L
               && this.screenshotDragStartPos == null
               && this.screenshotResizeStartPos == null
               && this.screenshotChangeStartBounds == null) {
               this.mouseDown = false;
               FormManager formManager = GlobalData.getCurrentState().getFormManager();
               Level level = this.client.getLevel();
               if (!formManager.hasFloatMenu() && level != null && level.isOneWorldLevel()) {
                  InputEvent offsetEvent = InputEvent.OffsetHudEvent(WindowManager.getWindow().getInput(), event, -this.getX(), -this.getY());
                  int mouseHudX = offsetEvent.pos.hudX;
                  int mouseHudY = offsetEvent.pos.hudY;
                  int scale = this.zoomLevels[this.zoomLevel];
                  double resHalfX = this.getWidth() / 2.0;
                  double resHalfY = this.getHeight() / 2.0;
                  Rectangle drawBounds = new Rectangle(this.getWidth(), this.getHeight());

                  for (ClientLevelManager.MapMarker mapMarker : this.client.levelManager.getMapMarkers()) {
                     int drawX = this.getHudPosX(resHalfX, scale, mapMarker.tileX * 32 + 16);
                     int drawY = this.getHudPosY(resHalfY, scale, mapMarker.tileY * 32 + 16);
                     Rectangle drawBox = new Rectangle(mapMarker.icon.getDrawBoundingBox());
                     drawBox.x += drawX;
                     drawBox.y += drawY;
                     if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                        formManager.openFloatMenu(new ContinueComponentFloatMenu(this, new EditMapMarkerForm(this.client, 400, mapMarker, false)));
                        offsetEvent.use();
                        return;
                     }
                  }

                  if (!formManager.hasFloatMenu()) {
                     int posX = this.getMouseMapPosX(event.pos.hudX);
                     int posY = this.getMouseMapPosY(event.pos.hudY);
                     int tileX = GameMath.getTileCoordinate(posX);
                     int tileY = GameMath.getTileCoordinate(posY);
                     ClientLevelManager.MapMarker mapMarker = this.client
                        .levelManager
                        .addMapMarker(MapIconRegistry.defaultIcon, new StaticMessage(""), this.client.getLevel().getIdentifier(), tileX, tileY);
                     formManager.openFloatMenu(new ContinueComponentFloatMenu(this, new EditMapMarkerForm(this.client, 400, mapMarker, true)));
                  }

                  event.use();
                  return;
               }
            }
         }
      }

      super.handleInputEvent(event, tickManager, perspective);
      if (this.isMouseOver(event) && !event.isUsed() && event.state && event.getID() == -99) {
         int scale = this.zoomLevels[this.zoomLevel];
         double tileScale = 32.0 / scale;
         double resHalfX = this.getWidth() / 2.0;
         double resHalfY = this.getHeight() / 2.0;
         InputEvent offsetEvent = InputEvent.OffsetHudEvent(WindowManager.getWindow().getInput(), event, -this.getX(), -this.getY());
         Rectangle drawBounds = new Rectangle(this.getWidth(), this.getHeight());
         int mouseHudX = offsetEvent.pos.hudX;
         int mouseHudY = offsetEvent.pos.hudY;
         Level level = this.client.getLevel();
         if (level != null) {
            for (DrawOnMapEntity e : level.entityManager.getEntityDrawOnMap()) {
               if (offsetEvent.isUsed()) {
                  return;
               }

               if (e.isVisibleOnMap(this.client, this.client.levelManager.getMap())) {
                  Point mapPos = e.getMapPos();
                  int drawX = this.getHudPosX(resHalfX, scale, mapPos.x);
                  int drawY = this.getHudPosY(resHalfY, scale, mapPos.y);
                  Rectangle drawBox = new Rectangle(e.drawOnMapBox(tileScale, this.isMinimap));
                  drawBox.x += drawX;
                  drawBox.y += drawY;
                  if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                     e.onMapInteract(offsetEvent, perspective);
                  }
               }
            }
         }

         if (offsetEvent.isUsed()) {
            return;
         }

         ClientClient me = this.client.getClient();

         for (int i = 0; i < this.client.getSlots(); i++) {
            if (offsetEvent.isUsed()) {
               return;
            }

            ClientClient cClient = this.client.getClient(i);
            if (cClient != null
               && cClient.loadedPlayer
               && cClient.hasSpawned()
               && !cClient.isDead()
               && (cClient == me || cClient.isSameTeam(me))
               && cClient.isSamePlace(level)) {
               PlayerMob player = cClient.playerMob;
               int drawX = this.getHudPosX(resHalfX, scale, player.getX());
               int drawY = this.getHudPosY(resHalfY, scale, player.getY());
               Rectangle drawBox = new Rectangle(player.drawOnMapBox(tileScale, this.isMinimap));
               drawBox.x += drawX;
               drawBox.y += drawY;
               if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                  player.onMapInteract(offsetEvent, perspective);
               }
            }
         }

         if (offsetEvent.isUsed()) {
            return;
         }

         for (MapDrawElement element : this.client.levelManager.getMap().getDrawElements()) {
            if (offsetEvent.isUsed()) {
               return;
            }

            int drawX = this.getHudPosX(resHalfX, scale, element.getX());
            int drawY = this.getHudPosY(resHalfY, scale, element.getY());
            Rectangle drawBox = new Rectangle(element.getBoundingBox());
            drawBox.x += drawX;
            drawBox.y += drawY;
            if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
               element.onMapInteract(offsetEvent, perspective);
            }
         }

         FormManager formManager = GlobalData.getCurrentState().getFormManager();
         if (!formManager.hasFloatMenu() && level != null && level.isOneWorldLevel()) {
            for (ClientLevelManager.MapMarker mapMarker : this.client.levelManager.getMapMarkers()) {
               if (offsetEvent.isUsed()) {
                  return;
               }

               int drawX = this.getHudPosX(resHalfX, scale, mapMarker.tileX * 32 + 16);
               int drawY = this.getHudPosY(resHalfY, scale, mapMarker.tileY * 32 + 16);
               Rectangle drawBox = new Rectangle(mapMarker.icon.getDrawBoundingBox());
               drawBox.x += drawX;
               drawBox.y += drawY;
               if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                  this.client.levelManager.deleteMapIcon(mapMarker);
                  offsetEvent.use();
                  break;
               }
            }
         }
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      super.handleControllerEvent(event, tickManager, perspective);
      if (!event.isUsed() && event.getState() == ControllerInput.MENU_ITEM_ACTIONS_MENU && this.isControllerSelected) {
         FormManager formManager = GlobalData.getCurrentState().getFormManager();
         Level level = this.client.getLevel();
         if (!formManager.hasFloatMenu() && level != null && level.isOneWorldLevel()) {
            InputEvent offsetEvent = InputEvent.OffsetHudEvent(
               WindowManager.getWindow().getInput(), InputEvent.ControllerButtonEvent(event, tickManager), -this.getX(), -this.getY()
            );
            int mouseHudX = this.getWidth() / 2;
            int mouseHudY = this.getHeight() / 2;
            int scale = this.zoomLevels[this.zoomLevel];
            double resHalfX = this.getWidth() / 2.0;
            double resHalfY = this.getHeight() / 2.0;
            Rectangle drawBounds = new Rectangle(this.getWidth(), this.getHeight());

            for (ClientLevelManager.MapMarker mapMarker : this.client.levelManager.getMapMarkers()) {
               int drawX = this.getHudPosX(resHalfX, scale, mapMarker.tileX * 32 + 16);
               int drawY = this.getHudPosY(resHalfY, scale, mapMarker.tileY * 32 + 16);
               Rectangle drawBox = new Rectangle(mapMarker.icon.getDrawBoundingBox());
               drawBox.x += drawX;
               drawBox.y += drawY;
               if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                  EditMapMarkerForm editForm = new EditMapMarkerForm(this.client, 400, mapMarker, false);
                  editForm.onContinue(() -> this.isControllerSelected = true);
                  formManager.openFloatMenu(new ContinueComponentFloatMenu(this, editForm));
                  offsetEvent.use();
                  return;
               }
            }

            int posX = this.getMouseMapPosX(this.getWidth() / 2 - this.getX());
            int posY = this.getMouseMapPosY(this.getHeight() / 2 - this.getY());
            int tileX = GameMath.getTileCoordinate(posX);
            int tileY = GameMath.getTileCoordinate(posY);
            ClientLevelManager.MapMarker mapMarker = this.client
               .levelManager
               .addMapMarker(MapIconRegistry.defaultIcon, new StaticMessage(""), this.client.getLevel().getIdentifier(), tileX, tileY);
            EditMapMarkerForm editForm = new EditMapMarkerForm(this.client, 400, mapMarker, true);
            editForm.onContinue(() -> this.isControllerSelected = true);
            formManager.openFloatMenu(new ContinueComponentFloatMenu(this, editForm));
            event.use();
            this.isControllerSelected = false;
            return;
         }
      }

      if (!event.isUsed() && event.getState() == ControllerInput.MENU_SELECT && this.isControllerFocus() && event.buttonState) {
         int scale = this.zoomLevels[this.zoomLevel];
         double tileScale = 32.0 / scale;
         double resHalfX = this.getWidth() / 2.0;
         double resHalfY = this.getHeight() / 2.0;
         InputEvent offsetEvent = InputEvent.OffsetHudEvent(
            WindowManager.getWindow().getInput(), InputEvent.ControllerButtonEvent(event, tickManager), -this.getX(), -this.getY()
         );
         Rectangle drawBounds = new Rectangle(this.getWidth(), this.getHeight());
         int mouseHudX = this.getWidth() / 2;
         int mouseHudY = this.getHeight() / 2;
         Level level = this.client.getLevel();
         if (level != null) {
            for (DrawOnMapEntity e : level.entityManager.getEntityDrawOnMap()) {
               if (offsetEvent.isUsed()) {
                  return;
               }

               if (e.isVisibleOnMap(this.client, this.client.levelManager.getMap())) {
                  Point mapPos = e.getMapPos();
                  int drawX = this.getHudPosX(resHalfX, scale, mapPos.x);
                  int drawY = this.getHudPosY(resHalfY, scale, mapPos.y);
                  Rectangle drawBox = new Rectangle(e.drawOnMapBox(tileScale, this.isMinimap));
                  drawBox.x += drawX;
                  drawBox.y += drawY;
                  if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                     e.onMapInteract(offsetEvent, perspective);
                  }
               }
            }
         }

         if (offsetEvent.isUsed()) {
            return;
         }

         ClientClient me = this.client.getClient();

         for (int i = 0; i < this.client.getSlots(); i++) {
            if (offsetEvent.isUsed()) {
               return;
            }

            ClientClient cClient = this.client.getClient(i);
            if (cClient != null
               && cClient.loadedPlayer
               && cClient.hasSpawned()
               && !cClient.isDead()
               && (cClient == me || cClient.isSameTeam(me))
               && cClient.isSamePlace(level)) {
               PlayerMob player = cClient.playerMob;
               int drawX = this.getHudPosX(resHalfX, scale, player.getX());
               int drawY = this.getHudPosY(resHalfY, scale, player.getY());
               Rectangle drawBox = new Rectangle(player.drawOnMapBox(tileScale, this.isMinimap));
               drawBox.x += drawX;
               drawBox.y += drawY;
               if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                  player.onMapInteract(offsetEvent, perspective);
               }
            }
         }

         if (offsetEvent.isUsed()) {
            return;
         }

         for (MapDrawElement element : this.client.levelManager.getMap().getDrawElements()) {
            if (offsetEvent.isUsed()) {
               return;
            }

            int drawX = this.getHudPosX(resHalfX, scale, element.getX());
            int drawY = this.getHudPosY(resHalfY, scale, element.getY());
            Rectangle drawBox = new Rectangle(element.getBoundingBox());
            drawBox.x += drawX;
            drawBox.y += drawY;
            if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
               element.onMapInteract(offsetEvent, perspective);
            }
         }

         FormManager formManager = GlobalData.getCurrentState().getFormManager();
         if (this.isControllerSelected && !formManager.hasFloatMenu() && level != null && level.isOneWorldLevel()) {
            for (ClientLevelManager.MapMarker mapMarker : this.client.levelManager.getMapMarkers()) {
               if (offsetEvent.isUsed()) {
                  return;
               }

               int drawX = this.getHudPosX(resHalfX, scale, mapMarker.tileX * 32 + 16);
               int drawY = this.getHudPosY(resHalfY, scale, mapMarker.tileY * 32 + 16);
               Rectangle drawBox = new Rectangle(mapMarker.icon.getDrawBoundingBox());
               drawBox.x += drawX;
               drawBox.y += drawY;
               if (drawBounds.intersects(drawBox) && drawBox.contains(mouseHudX, mouseHudY)) {
                  this.client.levelManager.deleteMapIcon(mapMarker);
                  offsetEvent.use();
                  break;
               }
            }
         }
      }
   }

   @Override
   public void drawControllerFocus(ControllerFocus current) {
      super.drawControllerFocus(current);
      if (this.isControllerSelected) {
         FormManager formManager = GlobalData.getCurrentState().getFormManager();
         if (!formManager.hasFloatMenu()) {
            GameTooltipManager.addControllerGlyph(Localization.translate("ui", "placemapmarker"), ControllerInput.MENU_ITEM_ACTIONS_MENU);
         }
      }
   }

   @Override
   public void setupMapDraw(int startTileX, int startTileY, int endTileX, int endTileY) {
      super.setupMapDraw(startTileX, startTileY, endTileX, endTileY);
      ClientDiscoveredMap map = this.client.levelManager.getMap();
      if (map != null) {
         this.mapTextureGetter = new DiscoveredMapBoundsExecutor(map, startTileX, startTileY, endTileX, endTileY, true, true);
      } else {
         this.mapTextureGetter = null;
      }
   }

   @Override
   public void drawMapTexture(int textureX, int textureY, double tileScale, int drawX, int drawY) {
      if (this.mapTextureGetter != null) {
         ClientDiscoveredMapRegion region = this.mapTextureGetter.getRegion(textureX, textureY);
         if (region != null) {
            GameTexture texture = region.getTexture();
            if (texture != null) {
               int textureWidth = (int)(tileScale * texture.getWidth());
               int textureHeight = (int)(tileScale * texture.getHeight());
               texture.initDraw().size(textureWidth, textureHeight).draw(drawX, drawY);
               Renderer.drawShape(new Rectangle(textureWidth, textureHeight), drawX, drawY, false, 1.0F, 0.0F, 0.0F, 0.0F);
            }
         }
      }
   }

   @Override
   public void drawMapOverlays(
      TickManager tickManager, PlayerMob perspective, int scale, double tileScale, double resHalfX, double resHalfY, int mouseHudX, int mouseHudY
   ) {
      boolean isHovering = this.isHovering || this.isControllerSelected();
      Level level;
      if (Settings.serverPerspective && this.client.getLocalServer() != null) {
         level = this.client.getLocalServer().world.getLevel(this.client.getLevel().getIdentifier());
      } else {
         level = this.client.getLevel();
      }

      if (level != null) {
         ClientDiscoveredMap map = this.client.levelManager.getMap();
         if (map != null) {
            Rectangle drawBounds = new Rectangle(this.getWidth(), this.getHeight());
            level.entityManager.getEntityDrawOnMap().stream().filter(e -> e.isVisibleOnMap(this.client, map)).forEach(e -> {
               Point mapPos = e.getMapPos();
               int drawXx = this.getHudPosX(resHalfX, scale, mapPos.x);
               int drawYx = this.getHudPosY(resHalfY, scale, mapPos.y);
               Rectangle drawBoxx = new Rectangle(e.drawOnMapBox(tileScale, this.isMinimap));
               drawBoxx.x += drawXx;
               drawBoxx.y += drawYx;
               if (drawBounds.intersects(drawBoxx)) {
                  e.drawOnMap(tickManager, this.client, drawXx, drawYx, tileScale, drawBounds, this.isMinimap);
                  if (isHovering && drawBoxx.contains(mouseHudX, mouseHudY)) {
                     GameTooltips tooltipsx = e.getMapTooltips();
                     if (tooltipsx != null) {
                        GameTooltipManager.addTooltip(tooltipsx, TooltipLocation.FORM_FOCUS);
                     }

                     String interactTip = e.getMapInteractTooltip();
                     if (interactTip != null) {
                        GameTooltipManager.addTooltip(new InputTooltip(-99, interactTip), TooltipLocation.FORM_FOCUS);
                        Renderer.setCursor(GameWindow.CURSOR.INTERACT);
                     }
                  }
               }
            });
            ClientClient me = this.client.getClient();

            for (int i = 0; i < this.client.getSlots(); i++) {
               ClientClient cClient = this.client.getClient(i);
               if (cClient != null
                  && cClient.loadedPlayer
                  && cClient.hasSpawned()
                  && !cClient.isDead()
                  && (cClient == me || cClient.isSameTeam(me))
                  && cClient.isSamePlace(level)) {
                  PlayerMob player = cClient.playerMob;
                  int drawX = this.getHudPosX(resHalfX, scale, player.getX());
                  int drawY = this.getHudPosY(resHalfY, scale, player.getY());
                  Rectangle drawBox = new Rectangle(player.drawOnMapBox(tileScale, this.isMinimap));
                  drawBox.x += drawX;
                  drawBox.y += drawY;
                  if (drawBounds.intersects(drawBox)) {
                     player.drawOnMap(tickManager, this.client, drawX, drawY, tileScale, drawBounds, this.isMinimap);
                     if (isHovering && drawBox.contains(mouseHudX, mouseHudY)) {
                        GameTooltips tooltips = player.getMapTooltips();
                        if (tooltips != null) {
                           GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
                        }

                        String interactTip = player.getMapInteractTooltip();
                        if (interactTip != null) {
                           GameTooltipManager.addTooltip(new InputTooltip(-99, interactTip), TooltipLocation.FORM_FOCUS);
                           Renderer.setCursor(GameWindow.CURSOR.INTERACT);
                        }
                     }
                  }
               }
            }

            for (HudDrawOnMap hudMapDraw : level.hudManager.getMapDraws()) {
               if (hudMapDraw.shouldDrawOnMap(this.client, map)) {
                  Point levelPos = hudMapDraw.getMapLevelPos();
                  int drawX = this.getHudPosX(resHalfX, scale, levelPos.x);
                  int drawY = this.getHudPosY(resHalfY, scale, levelPos.y);
                  Rectangle elementDrawBounds = hudMapDraw.getMapLevelDrawBounds();
                  if (elementDrawBounds != null) {
                     elementDrawBounds = new Rectangle(elementDrawBounds);
                     elementDrawBounds.x += drawX;
                     elementDrawBounds.y += drawY;
                  }

                  if (elementDrawBounds == null || drawBounds.intersects(elementDrawBounds)) {
                     hudMapDraw.drawOnMap(tickManager, this.client, drawX, drawY, tileScale, drawBounds, this.isMinimap, mouseHudX, mouseHudY);
                  }
               }
            }

            ClientDebugMapHudDrawElement debugMapDrawElement = this.client.levelManager.debugMapDrawElement;
            if (debugMapDrawElement != null && debugMapDrawElement.shouldDrawOnMap(this.client, map)) {
               Point levelPos = debugMapDrawElement.getMapLevelPos();
               int drawX = this.getHudPosX(resHalfX, scale, levelPos.x);
               int drawY = this.getHudPosY(resHalfY, scale, levelPos.y);
               debugMapDrawElement.drawOnMap(tickManager, this.client, drawX, drawY, tileScale, drawBounds, this.isMinimap, mouseHudX, mouseHudY);
            }

            ControllerFocus currentFocus = this.getManager().getCurrentFocus();

            for (MapDrawElement element : map.getDrawElements()) {
               int drawX = this.getHudPosX(resHalfX, scale, element.getX());
               int drawY = this.getHudPosY(resHalfY, scale, element.getY());
               Rectangle drawBox = new Rectangle(element.getBoundingBox());
               drawBox.x += drawX;
               drawBox.y += drawY;
               if (drawBounds.intersects(drawBox)) {
                  if (isHovering && drawBox.contains(mouseHudX, mouseHudY)) {
                     GameTooltips tooltips = element.getTooltips(drawX, drawY, perspective);
                     if (tooltips != null) {
                        GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
                     }

                     String interactTip = element.getMapInteractTooltip();
                     if (interactTip != null) {
                        if (Input.lastInputIsController && this.isControllerSelected() && currentFocus != null) {
                           GameTooltipManager.addTooltip(new InputTooltip(ControllerInput.MENU_SELECT, interactTip), TooltipLocation.FORM_FOCUS);
                        } else {
                           GameTooltipManager.addTooltip(new InputTooltip(-99, interactTip), TooltipLocation.FORM_FOCUS);
                           Renderer.setCursor(GameWindow.CURSOR.INTERACT);
                        }
                     }
                  }

                  element.draw(drawX, drawY, perspective);
               }
            }

            if (perspective != null) {
               PlayerInventorySlot slot = perspective.getSelectedItemSlot();
               InventoryItem selectedItem = slot.getInv(perspective.getInv()).getItem(slot.slot);
               if (selectedItem != null && selectedItem.item.isPlaceable()) {
                  GameCamera camera = GlobalData.getCurrentState().getCamera();
                  if (camera != null) {
                     Point levelPos;
                     if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
                        Float aimDir = perspective.getControllerAimDir();
                        levelPos = selectedItem.item.getControllerAttackLevelPos(level, aimDir.x, aimDir.y, perspective, selectedItem);
                     } else {
                        levelPos = new Point(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
                     }

                     int drawX = this.getHudPosX(resHalfX, scale, levelPos.x);
                     int drawY = this.getHudPosY(resHalfY, scale, levelPos.y);
                     selectedItem.item
                        .getPlaceable()
                        .drawMapPlacePreview(
                           level,
                           levelPos.x,
                           levelPos.y,
                           tickManager,
                           this.client,
                           drawX,
                           drawY,
                           tileScale,
                           drawBounds,
                           this.isMinimap,
                           perspective,
                           selectedItem,
                           slot
                        );
                  }
               }
            }

            if (level.isOneWorldLevel()) {
               boolean hoveringMarker = false;

               for (ClientLevelManager.MapMarker mapMarker : this.client.levelManager.getMapMarkers()) {
                  boolean sameLevel = level.getIdentifier().equals(mapMarker.levelIdentifier);
                  int drawX = this.getHudPosX(resHalfX, scale, mapMarker.tileX * 32 + 16);
                  int drawY = this.getHudPosY(resHalfY, scale, mapMarker.tileY * 32 + 16);
                  Rectangle drawBox = new Rectangle(mapMarker.icon.getDrawBoundingBox());
                  drawBox.x += drawX;
                  drawBox.y += drawY;
                  if (drawBounds.intersects(drawBox)) {
                     if (isHovering && drawBox.contains(mouseHudX, mouseHudY)) {
                        ListGameTooltips tooltips = new ListGameTooltips();
                        if (!mapMarker.name.isEmpty()) {
                           tooltips.add(mapMarker.name);
                        }

                        if (!sameLevel) {
                           tooltips.add("(" + mapMarker.levelIdentifier.getDisplayName().translate() + ")");
                        }

                        hoveringMarker = true;
                        GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
                     }

                     mapMarker.icon.drawIcon(drawX, drawY, sameLevel ? Color.WHITE : new Color(0.5F, 0.5F, 0.5F, 0.5F));
                  }
               }

               if (hoveringMarker) {
                  FormManager formManager = GlobalData.getCurrentState().getFormManager();
                  ListGameTooltips tooltips = new ListGameTooltips();
                  if (Input.lastInputIsController && this.isControllerSelected() && currentFocus != null) {
                     if (!formManager.hasFloatMenu()) {
                        tooltips.add(new InputTooltip(ControllerInput.MENU_ITEM_ACTIONS_MENU, Localization.translate("controls", "edittip")));
                        tooltips.add(new InputTooltip(ControllerInput.MENU_SELECT, Localization.translate("controls", "deletetip")));
                     }
                  } else {
                     if (!formManager.hasFloatMenu()) {
                        tooltips.add(new InputTooltip(-100, Localization.translate("controls", "edittip")));
                        tooltips.add(new InputTooltip(-99, Localization.translate("controls", "deletetip")));
                     }

                     Renderer.setCursor(GameWindow.CURSOR.INTERACT);
                  }

                  GameTooltipManager.addTooltip(tooltips, TooltipLocation.FORM_FOCUS);
               }
            }

            if (isHovering) {
               int mouseTileX = GameMath.getTileCoordinate(this.getMouseMapPosX(mouseHudX + this.getX()));
               int mouseTileY = GameMath.getTileCoordinate(this.getMouseMapPosY(mouseHudY + this.getY()));
               if (map.isTileKnown(mouseTileX, mouseTileY) && level.regionManager.isTileLoaded(mouseTileX, mouseTileY)) {
                  GameTooltips tileTooltips = level.getLevelTile(mouseTileX, mouseTileY).getMapTooltips();
                  if (tileTooltips != null) {
                     GameTooltipManager.addTooltip(tileTooltips, TooltipLocation.FORM_FOCUS);
                  }

                  GameTooltips objectTooltips = level.getLevelObject(mouseTileX, mouseTileY).getMapTooltips();
                  if (objectTooltips != null) {
                     GameTooltipManager.addTooltip(objectTooltips, TooltipLocation.FORM_FOCUS);
                  }
               }
            }
         }
      }
   }
}
