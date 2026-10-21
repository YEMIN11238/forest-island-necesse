package necesse.inventory.item.placeableItem.objectItem;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import necesse.engine.GameEvents;
import necesse.engine.events.players.ItemPlaceEvent;
import necesse.engine.journal.listeners.ObjectPlacedJournalChallengeListener;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketPlaceObject;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.storyObjectives.listeners.ObjectPlacedObjectiveListener;
import necesse.engine.util.GameBlackboard;
import necesse.entity.AbstractDamageResult;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.ObjectItemAttackHandler;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.placeableItem.PlaceableItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.multiTile.MultiTile;

public class ObjectItem extends PlaceableItem {
   public int objectID;
   public GameMessage translatedTypeName;

   public ObjectItem(GameObject object, boolean dropAsMatDeathPenalty) {
      super(object.stackSize, true);
      this.objectID = object.getID();
      this.controllerIsTileBasedPlacing = true;
      this.rarity = object.rarity;
      this.dropsAsMatDeathPenalty = dropAsMatDeathPenalty;
      this.setItemCategory(object.itemCategoryTree);
      this.setItemCategory(ItemCategory.craftingManager, object.craftingCategoryTree);
      this.keyWords.add("object");
      this.keyWords.addAll(object.roomProperties);

      for (String globalIngredient : object.itemGlobalIngredients) {
         this.addGlobalIngredient(globalIngredient);
      }

      this.translatedTypeName = new LocalMessage("object", "object");
   }

   public ObjectItem(GameObject object) {
      this(object, true);
   }

   @Override
   public void loadItemTextures() {
      this.itemTexture = this.getObject().generateItemTexture();
   }

   @Override
   public GameMessage getNewLocalization() {
      return this.getObject().getLocalization();
   }

   @Override
   public String getTranslatedTypeName() {
      return this.translatedTypeName.translate();
   }

   public void setTranslatedTypeName(GameMessage name) {
      this.translatedTypeName = name;
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      tooltips.add(this.getObject().getItemTooltips(item, perspective));
      return tooltips;
   }

   @Override
   public void setupAttackMapContent(GNDItemMap map, Level level, int x, int y, ItemAttackerMob attackerMob, int seed, InventoryItem item) {
      super.setupAttackMapContent(map, level, x, y, attackerMob, seed, item);
      ObjectPlaceOption placeOption = attackerMob.isPlayer ? this.getBestPlaceOption(level, x, y, item, (PlayerMob)attackerMob, null, false) : null;
      if (placeOption != null) {
         this.setupPlaceMapContent(map, placeOption, level);
      }
   }

   public void setupPlaceMapContent(GNDItemMap map, ObjectPlaceOption placeOption, Level level) {
      if (placeOption != null) {
         map.setBoolean("hasPlaceOption", true);
         placeOption.writeGNDMap("placeOption", map);
         level.setupTileAndObjectsHashGNDMap(map, placeOption.tileX, placeOption.tileY, true);
      }
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      int cooldown = this.getAttackCooldownTime(item, attackerMob);
      if (cooldown > 0) {
         return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
      }

      if (!attackerMob.isPlayer) {
         return item;
      }

      ObjectItemAttackHandler handler = new ObjectItemAttackHandler((PlayerMob)attackerMob, slot, x, y, seed, this);
      attackerMob.startAttackHandler(handler);
      handler.runInitialPlace(mapContent);
      return slot.getItem();
   }

   @Override
   public int getPlaceRange(InventoryItem item, ItemAttackerMob attackerMob) {
      MultiTile multiTile = this.getObject().getMultiTile(0);
      if (multiTile.width <= 1 && multiTile.height <= 1) {
         return super.getPlaceRange(item, attackerMob);
      }

      int maxSize = Math.max(multiTile.width, multiTile.height);
      int placeRangeIncrease = (int)((maxSize - 2) / 2.0F * 32.0F);
      return super.getPlaceRange(item, attackerMob) + placeRangeIncrease;
   }

   public ArrayList<ObjectPlaceOption> getPlaceOptions(Level level, int levelX, int levelY, PlayerMob playerMob, int playerDir, boolean offsetMultiTile) {
      return this.getObject().getPlaceOptions(level, levelX, levelY, playerMob, playerDir, offsetMultiTile);
   }

   public ObjectPlaceOption getBestPlaceOption(
      Level level, int x, int y, InventoryItem item, PlayerMob player, Line2D playerPositionLine, boolean onlyBestOption
   ) {
      int dir = player.isAttacking ? player.beforeAttackDir : player.getDir();
      ArrayList<ObjectPlaceOption> placeOptions = this.getPlaceOptions(level, x, y, player, dir, true);
      ObjectPlaceOption bestReplaceOption = null;

      for (ObjectPlaceOption po : placeOptions) {
         for (int layer : po.object.getValidObjectLayers()) {
            String error = po.object.canPlace(level, layer, po.tileX, po.tileY, po.rotation, true, false);
            if (error == null) {
               return po;
            }

            if (bestReplaceOption == null
               && this.canReplace(po.object, level, layer, po.tileX, po.tileY, po.rotation, player, playerPositionLine, true, item, error)) {
               bestReplaceOption = po;
            }
         }
      }

      if (bestReplaceOption != null) {
         return bestReplaceOption;
      } else if (onlyBestOption) {
         return null;
      } else {
         return placeOptions.isEmpty() ? null : placeOptions.get(0);
      }
   }

   public ObjectPlaceOption getPlaceOptionFromMap(GNDItemMap mapContent) {
      return !mapContent.getBoolean("hasPlaceOption") ? null : new ObjectPlaceOption("placeOption", mapContent);
   }

   @Override
   public String canPlace(Level level, int x, int y, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      if (!level.isLevelPosWithinBounds(x, y)) {
         return "outsidelevel";
      }

      if (mapContent == null) {
         mapContent = new GNDItemMap();
         ObjectPlaceOption placeOption = this.getBestPlaceOption(level, x, y, item, player, playerPositionLine, false);
         if (placeOption != null) {
            this.setupPlaceMapContent(mapContent, placeOption, level);
         }
      }

      ObjectPlaceOption po = this.getPlaceOptionFromMap(mapContent);
      if (po == null) {
         return "noplaceoption";
      } else {
         return !this.isValidPlaceOption(level, x, y, player, po)
            ? "invalidplaceoption"
            : this.canPlace(level, po, player, playerPositionLine, item, mapContent);
      }
   }

   public boolean isValidPlaceOption(Level level, int x, int y, PlayerMob player, ObjectPlaceOption placeOption) {
      int dir = player.isAttacking ? player.beforeAttackDir : player.getDir();
      return this.getPlaceOptions(level, x, y, player, dir, true).stream().filter(Objects::nonNull).anyMatch(placeOption::isSame);
   }

   public String canPlace(Level level, ObjectPlaceOption po, PlayerMob player, Line2D playerPositionLine, InventoryItem item, GNDItemMap mapContent) {
      if (!po.object.canPlaceOnProtectedLevels && level.isProtected(po.tileX, po.tileY)) {
         return "protected";
      }

      if (player.isServerClient()) {
         ServerClient client = player.getServerClient();
         level.checkTileAndObjectsHashGNDMap(client, mapContent, po.tileX, po.tileY, true);
      }

      MultiTile multiTile = po.object.getMultiTile(po.rotation);
      if (multiTile.streamOtherObjects(po.tileX, po.tileY).anyMatch(e -> !level.isTileWithinBounds(e.tileX, e.tileY))) {
         return "outsidelevel";
      }

      if (multiTile.streamOtherObjects(po.tileX, po.tileY).anyMatch(e -> !e.value.canPlaceOnProtectedLevels && level.isProtected(e.tileX, e.tileY))) {
         return "protected";
      }

      Point offset = po.object.getPlaceOffset(po.rotation);
      Point placeDistancePoint = new Point(po.tileX * 32 + 16 - offset.x, po.tileY * 32 + 16 - offset.y);
      if (!this.isInPlaceRange(level, placeDistancePoint.x, placeDistancePoint.y, player, playerPositionLine, item)) {
         return "outofrange";
      }

      String firstError = null;

      for (int layer : po.object.getValidObjectLayers()) {
         String error = po.object.canPlace(level, layer, po.tileX, po.tileY, po.rotation, true, false);
         if (error != null) {
            if (!this.canReplace(po.object, level, layer, po.tileX, po.tileY, po.rotation, player, playerPositionLine, true, item, error)) {
               if (firstError == null) {
                  firstError = error;
               }
               continue;
            }

            firstError = null;
            break;
         }

         firstError = null;
         break;
      }

      boolean validPlace = false;
      if (firstError != null) {
         return firstError;
      }

      if (level.isServer()) {
         validPlace = !po.object.checkPlaceCollision(level, po.tileX, po.tileY, po.rotation, true);
      } else if (level.isClient()) {
         validPlace = !po.object.checkPlaceCollision(level, po.tileX, po.tileY, po.rotation, true);
      }

      return !validPlace ? "collision" : null;
   }

   @Override
   public InventoryItem onPlace(Level level, int x, int y, PlayerMob player, int seed, InventoryItem item, GNDItemMap mapContent) {
      if (mapContent == null) {
         mapContent = new GNDItemMap();
         this.setupAttackMapContent(mapContent, level, x, y, player, seed, item);
      }

      ObjectPlaceOption po = this.getPlaceOptionFromMap(mapContent);
      if (po == null) {
         return item;
      }

      boolean isReplace = false;
      int placeLayerID = -1;
      LinkedHashSet<Integer> layers = po.object.getValidObjectLayers();
      HashMap<Integer, String> errors = new HashMap<>();

      for (int layer : layers) {
         String error = po.object.canPlace(level, layer, po.tileX, po.tileY, po.rotation, true, false);
         errors.put(layer, error);
         if (error == null) {
            placeLayerID = layer;
            break;
         }
      }

      if (player != null && placeLayerID == -1) {
         for (int layer : layers) {
            if (this.canReplace(po.object, level, layer, po.tileX, po.tileY, po.rotation, player, null, false, item, errors.get(layer))) {
               if (!this.runReplaceDamageTile(level, x, y, layer, po.tileX, po.tileY, po.rotation, player, item)) {
                  return item;
               }

               if (po.object.canPlace(level, layer, po.tileX, po.tileY, po.rotation, true, false) != null) {
                  return item;
               }

               placeLayerID = layer;
               isReplace = true;
               break;
            }
         }
      }

      ItemPlaceEvent event = new ItemPlaceEvent(level, po.tileX, po.tileY, player, item);
      GameEvents.triggerEvent(event);
      if (!event.isPrevented()) {
         if (!level.isClient()) {
            ServerClient client = player == null ? null : player.getServerClient();
            boolean success = this.onPlaceObject(po.object, level, placeLayerID, po.tileX, po.tileY, po.rotation, client, item);
            if (!success) {
               level.entityManager.pickups.add(item.copy(1).getPickupEntity(level, po.tileX * 32 + 16, po.tileY * 32 + 16));
            } else {
               if (client != null) {
                  int finalPlaceLayerID = placeLayerID;
                  JournalChallengeRegistry.handleListeners(
                     client,
                     ObjectPlacedJournalChallengeListener.class,
                     challenge -> challenge.onObjectPlaced(po.object, level, finalPlaceLayerID, po.tileX, po.tileY, po.rotation, client)
                  );
                  client.newStats.objects_placed.increment(1);
                  if (!client.adventureParty.isEmpty()) {
                     for (HumanMob settler : client.adventureParty.getMobs()) {
                        PlayerRomanceManager romanceManager = settler.getRomanceManager(client, false);
                        if (romanceManager != null) {
                           romanceManager.onObjectPlaced(client, po.object, po.tileX, po.tileY, po.rotation);
                        }
                     }
                  }

                  if (isReplace) {
                     level.getServer()
                        .network
                        .sendToClientsWithTile(
                           new PacketPlaceObject(level, client, placeLayerID, po.tileX, po.tileY, po.object.getID(), po.rotation, true, false),
                           level,
                           po.tileX,
                           po.tileY
                        );
                  } else {
                     level.getServer()
                        .network
                        .sendToClientsWithTileExcept(
                           new PacketPlaceObject(level, client, placeLayerID, po.tileX, po.tileY, po.object.getID(), po.rotation, true, false),
                           level,
                           po.tileX,
                           po.tileY,
                           client
                        );
                     if (mapContent.getBoolean("sendPlacePacketToAll")) {
                        boolean playPlaceSound = false;
                        Point offset = po.object.getPlaceOffset(po.rotation);
                        Point placeDistancePoint = new Point(po.tileX * 32 + 16 - offset.x, po.tileY * 32 + 16 - offset.y);
                        if (!this.isInPlaceRange(level, placeDistancePoint.x, placeDistancePoint.y, player, null, item, true)) {
                           playPlaceSound = true;
                        }

                        client.sendPacket(
                           new PacketPlaceObject(level, client, placeLayerID, po.tileX, po.tileY, po.object.getID(), po.rotation, true, playPlaceSound)
                        );
                     }
                  }
               } else if (level.isServer()) {
                  level.getServer()
                     .network
                     .sendToClientsWithTile(
                        new PacketPlaceObject(level, client, placeLayerID, po.tileX, po.tileY, po.object.getID(), po.rotation, true, true),
                        level,
                        po.tileX,
                        po.tileY
                     );
               }

               level.getTile(po.tileX, po.tileY).checkAround(level, po.tileX, po.tileY);
               level.getObject(po.tileX, po.tileY).checkAround(level, po.tileX, po.tileY);
            }
         } else {
            po.object.placeObject(level, placeLayerID, po.tileX, po.tileY, po.rotation, true);
            po.object.playPlaceSound(po.tileX, po.tileY);
         }

         if (this.isSingleUse(player)) {
            item.setAmount(item.getAmount() - 1);
         }
      }

      return item;
   }

   public boolean onPlaceObject(GameObject object, Level level, int layerID, int tileX, int tileY, int rotation, ServerClient client, InventoryItem item) {
      object.placeObject(level, layerID, tileX, tileY, rotation, true);
      if (level.isServer()) {
         level.onObjectPlaced(object, layerID, tileX, tileY, client);
      }

      if (client != null) {
         client.storyManager
            .streamObjectives(ObjectPlacedObjectiveListener.class)
            .forEach(objective -> objective.onObjectPlace(object, level, layerID, tileX, tileY, rotation));
      }

      return true;
   }

   public boolean canReplace(
      GameObject object,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      int rotation,
      PlayerMob playerMob,
      Line2D playerPositionLine,
      boolean checkRange,
      InventoryItem item,
      String error
   ) {
      MultiTile multiTile = object.getMultiTile(rotation);
      return multiTile.streamObjects(tileX, tileY).allMatch(c -> level.getObjectID(layerID, c.tileX, c.tileY) == 0)
         ? false
         : multiTile.streamObjects(tileX, tileY)
            .allMatch(
               c -> {
                  if (!c.value.canReplace(level, layerID, c.tileX, c.tileY, rotation)) {
                     return false;
                  } else {
                     return level.getObjectID(layerID, c.tileX, c.tileY) == 0
                        ? true
                        : this.getBestToolDamageItem(level, layerID, c.tileX, c.tileY, playerMob, playerPositionLine, checkRange) != null;
                  }
               }
            );
   }

   @Override
   public InventoryItem onAttemptPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent, String error) {
      if (!mapContent.getBoolean("hasPlaceOption")) {
         return item;
      }

      ObjectPlaceOption po = new ObjectPlaceOption("placeOption", mapContent);
      this.getObject().attemptPlace(level, po.tileX, po.tileY, player, error);
      return item;
   }

   @Override
   public float getAttackSpeedModifier(InventoryItem item, ItemAttackerMob attackerMob) {
      float superModifier = super.getAttackSpeedModifier(item, attackerMob);
      return attackerMob != null && attackerMob.isPlayer && ((PlayerMob)attackerMob).hasGodMode()
         ? superModifier
         : superModifier * (attackerMob == null ? 1.0F : attackerMob.buffManager.getModifier(BuffModifiers.BUILDING_SPEED));
   }

   @Override
   public int getAttackAnimTime(InventoryItem item, ItemAttackerMob attackerMob) {
      return Math.max(super.getAttackAnimTime(item, attackerMob), 50);
   }

   public GameObject getObject() {
      return ObjectRegistry.getObject(this.objectID);
   }

   @Override
   public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
      String canPlace = this.canPlace(level, x, y, player, null, item, null);
      if (canPlace == null) {
         ObjectPlaceOption po = this.getBestPlaceOption(level, x, y, item, player, null, false);
         if (po != null) {
            float alpha = 0.5F;
            po.object.drawMultiTilePreview(level, po.tileX, po.tileY, po.rotation, alpha, player, camera);
         }
      } else {
         ObjectPlaceOption po = this.getBestPlaceOption(level, x, y, item, player, null, false);
         if (po != null) {
            po.object.getMultiTile(po.rotation).streamObjects(po.tileX, po.tileY).forEach(e -> {
               String error = e.value.canPlace(level, 0, e.tileX, e.tileY, po.rotation, true, false);
               e.value.drawFailedPreview(level, e.tileX, e.tileY, po.rotation, 0.5F, error, player, camera);
            });
         }
      }
   }

   @Override
   public boolean showWires() {
      return this.getObject().showsWire;
   }

   @Override
   public void refreshLight(Level level, float x, float y, InventoryItem item, boolean isHolding) {
      GameObject object = this.getObject();
      if (object.lightLevel >= 100) {
         level.lightManager.refreshParticleLightFloat(x, y, object.lightHue, object.lightSat);
      }
   }

   protected PlayerInventorySlot getBestToolDamageItem(
      Level level, int layerID, int tileX, int tileY, PlayerMob player, Line2D playerPositionLine, boolean checkRange
   ) {
      ToolDamageItem bestToolItem = null;
      InventoryItem bestInventoryItem = null;
      PlayerInventorySlot bestSlot = null;

      for (int i = 0; i < player.getInv().main.getSize(); i++) {
         InventoryItem slotItem = player.getInv().main.getItem(i);
         if (slotItem != null && slotItem.item instanceof ToolDamageItem) {
            ToolDamageItem slotToolItem = (ToolDamageItem)slotItem.item;
            if ((bestToolItem == null || bestToolItem.getToolDps(bestInventoryItem, player) < slotToolItem.getToolDps(slotItem, player))
               && slotToolItem.canDamageTile(level, layerID == -1 ? 0 : layerID, tileX, tileY, player, slotItem)
               && slotToolItem.getToolType() != ToolType.SHOVEL) {
               GameObject currentObject = level.getObject(layerID == -1 ? 0 : layerID, tileX, tileY);
               if (slotToolItem.getToolType().canDealDamageTo(currentObject.toolType)
                  && (
                     !checkRange || slotToolItem.isTileInRange(level, tileX, tileY, player, playerPositionLine, slotItem) || !currentObject.isMultiTileMaster()
                  )) {
                  bestToolItem = slotToolItem;
                  bestInventoryItem = slotItem;
                  bestSlot = new PlayerInventorySlot(player.getInv().main, i);
               }
            }
         }
      }

      return bestSlot;
   }

   protected boolean runReplaceDamageTile(
      Level level, int levelX, int levelY, int layerID, int tileX, int tileY, int rotation, PlayerMob player, InventoryItem placeableItem
   ) {
      ServerClient client = player.isServerClient() ? player.getServerClient() : null;
      MultiTile multiTile = this.getObject().getMultiTile(rotation);

      for (MultiTile.CoordinateValue<Integer> coord : multiTile.getIDs(tileX, tileY)) {
         if (level.getObjectID(coord.tileX, coord.tileY) != 0) {
            PlayerInventorySlot toolDamageItemSlot = this.getBestToolDamageItem(level, layerID, coord.tileX, coord.tileY, player, null, false);
            if (toolDamageItemSlot == null) {
               return false;
            }

            InventoryItem toolInventoryItem = toolDamageItemSlot.getItem(player.getInv());
            ToolDamageItem toolItem = (ToolDamageItem)toolInventoryItem.item;
            float miningSpeedModifier = toolItem.getMiningSpeedModifier(toolInventoryItem, player);
            int hitDamage = (int)(
               toolItem.getToolDps(toolInventoryItem, player) * (this.getAttackAnimTime(placeableItem, player) / 1000.0F) * miningSpeedModifier
            );
            ToolType toolType = toolItem.getToolType();
            float toolTier = toolItem.getToolTier(toolInventoryItem, player);
            AbstractDamageResult result = level.entityManager
               .doToolDamage(layerID, coord.tileX, coord.tileY, hitDamage, toolType, toolTier, player, client, true, levelX, levelY);
            if (result == null || !result.destroyed) {
               return false;
            }
         }
      }

      return true;
   }

   @Override
   public boolean shouldShowInItemList() {
      return this.getObject().shouldShowInItemList();
   }
}
