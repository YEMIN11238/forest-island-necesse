package necesse.entity.pickup;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.journal.listeners.ItemPickedUpJournalChallengeListener;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.packet.PacketPickupItemEntityPickup;
import necesse.engine.network.packet.PacketSpawnPickupEntity;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.world.WorldSettings;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.EntityDrawable;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventory;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.TickItem;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.ItemPickupText;
import necesse.level.maps.light.GameLight;

public class ItemPickupEntity extends PickupEntity {
   public InventoryItem item;
   public boolean droppedByPlayer;
   public float height;
   public float dh;
   public boolean showsLightBeam;
   protected GameLinkedList<ItemPickupReservedAmount> reservedPickups = new GameLinkedList<>();
   public long playerDeathAuth;
   public int deathInventoryID;
   public int deathInventorySlot;
   public boolean deathIsLocked;
   public long nextSpoilTickWorldTime;

   public ItemPickupEntity() {
      this.bouncy = 0.75F;
   }

   public ItemPickupEntity(Level level, InventoryItem item, float x, float y, float dx, float dy, float height, float dh) {
      super(level, x, y, dx, dy);
      this.height = height;
      this.dh = dh;
      this.item = item;
      this.item.setLocked(false);
      this.bouncy = 0.75F;
   }

   public ItemPickupEntity(Level level, InventoryItem item, float x, float y, float dx, float dy) {
      this(level, item, x, y, dx, dy, GameRandom.globalRandom.getFloatBetween(5.0F, 15.0F), GameRandom.globalRandom.getFloatBetween(20.0F, 30.0F));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.playerDeathAuth != 0L) {
         save.addLong("playerDeathAuth", this.playerDeathAuth);
         save.addInt("deathInventoryID", this.deathInventoryID);
         save.addInt("deathInventorySlot", this.deathInventorySlot);
         save.addBoolean("deathIsLocked", this.deathIsLocked);
      }

      SaveData itemSave = new SaveData("ITEM");
      this.item.addSaveData(itemSave);
      save.addSaveData(itemSave);
      if (this.showsLightBeam) {
         save.addBoolean("showsLightBeam", this.showsLightBeam);
      }

      if (this.droppedByPlayer) {
         save.addBoolean("droppedByPlayer", this.droppedByPlayer);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      if (save.hasLoadDataByName("playerDeathAuth")) {
         this.playerDeathAuth = save.getLong("playerDeathAuth", -1L);
         this.deathInventoryID = save.getInt("deathInventoryID", -1);
         this.deathInventorySlot = save.getInt("deathInventorySlot", -1);
         this.deathIsLocked = save.getBoolean("deathIsLocked", false);
         if (this.playerDeathAuth == -1L || this.deathInventoryID == -1 || this.deathInventorySlot == -1) {
            this.playerDeathAuth = 0L;
            this.deathInventoryID = 0;
            this.deathInventorySlot = 0;
         }
      }

      LoadData itemSave = save.getFirstLoadDataByName("ITEM");
      if (itemSave != null) {
         this.item = InventoryItem.fromLoadData(itemSave);
      }

      if (this.item == null) {
         System.err.println("Loaded pickup entity on was invalid and removed.");
         this.remove();
      }

      this.showsLightBeam = save.getBoolean("showsLightBeam", this.showsLightBeam, false);
      this.droppedByPlayer = save.getBoolean("droppedByPlayer", this.droppedByPlayer, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.height);
      writer.putNextFloat(this.dh);
      if (this.playerDeathAuth != 0L) {
         writer.putNextBoolean(true);
         writer.putNextLong(this.playerDeathAuth);
         writer.putNextShortUnsigned(this.deathInventoryID);
         writer.putNextShortUnsigned(this.deathInventorySlot);
         writer.putNextBoolean(this.deathIsLocked);
      } else {
         writer.putNextBoolean(false);
      }

      Packet itemContent = InventoryItem.getContentPacket(this.item);
      writer.putNextContentPacket(itemContent);
      writer.putNextBoolean(this.showsLightBeam);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.height = reader.getNextFloat();
      this.dh = reader.getNextFloat();
      if (reader.getNextBoolean()) {
         this.playerDeathAuth = reader.getNextLong();
         this.deathInventoryID = reader.getNextShortUnsigned();
         this.deathInventorySlot = reader.getNextShortUnsigned();
         this.deathIsLocked = reader.getNextBoolean();
      } else {
         this.playerDeathAuth = 0L;
         this.deathInventoryID = 0;
         this.deathInventorySlot = 0;
         this.deathIsLocked = false;
      }

      Packet itemContent = reader.getNextContentPacket();
      this.item = InventoryItem.fromContentPacket(itemContent);
      this.showsLightBeam = reader.getNextBoolean();
   }

   @Override
   public Rectangle getSelectBox() {
      Rectangle selectBox = super.getSelectBox();
      selectBox.y = selectBox.y - (10 + (int)Math.max(0.0F, this.height));
      float drawSinking = (float)Math.pow(this.sinking, 0.4F);
      selectBox.y += (int)(drawSinking * 24.0F);
      return selectBox;
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      if (this.height != -1.0F) {
         float heightChange = 50.0F * delta / 250.0F;
         this.dh -= heightChange;
         this.height = this.height + this.dh * delta / 250.0F;
         if (this.height < 0.0F) {
            this.dh = -this.dh * this.bouncy * 0.7F;
            this.height = -this.height;
            if (Math.abs(this.dh) < heightChange * 2.0F) {
               this.height = -1.0F;
               this.dh = 0.0F;
            }
         }
      }
   }

   @Override
   public float getSinkingRate() {
      return this.height > 0.0F ? 0.0F : this.item.item.getSinkingRate(this, this.sinking);
   }

   @Override
   public float getMaxSinking() {
      return this.item.item.getMaxSinking(this);
   }

   @Override
   public long getLifespanMillis() {
      WorldSettings worldSettings = this.getWorldSettings();
      long worldSettingLifespan = (worldSettings == null ? Settings.droppedItemsLifeMinutes : worldSettings.droppedItemsLifeMinutes) * 60 * 1000L;
      long decayTime = this.item.item.getDropDecayTime(this.item);
      if (decayTime > 0L && worldSettingLifespan > 0L) {
         return Math.min(worldSettingLifespan, decayTime);
      } else if (decayTime > 0L) {
         return decayTime;
      } else {
         return worldSettingLifespan > 0L ? worldSettingLifespan : 0L;
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.item.item.tickPickupEntity(this);
      WorldSettings worldSettings = this.getWorldSettings();
      if (this.item.item.isTickItem()) {
         ((TickItem)this.item.item).tick(null, -1, this.item, this, this, this, null, worldSettings, newItem -> {
            if (newItem == null) {
               this.remove();
            } else {
               this.item = newItem;
               this.markDirty();
            }
         });
      }

      if ((worldSettings == null || worldSettings.survivalMode)
         && this.nextSpoilTickWorldTime <= this.getWorldTime()
         && this.item.item.shouldSpoilTick(this.item)) {
         this.nextSpoilTickWorldTime = this.item.item.tickSpoilTime(this.item, this, 1.0F, newItem -> {
            if (newItem == null) {
               this.remove();
            } else {
               this.item = newItem;
               this.markDirty();
            }
         });
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.removed()) {
         if (this.item.getAmount() <= 0) {
            this.remove();
         } else {
            this.item.item.tickPickupEntity(this);
            WorldSettings worldSettings = this.getWorldSettings();
            if (this.item.item.isTickItem()) {
               ((TickItem)this.item.item).tick(null, -1, this.item, this, this, this, null, worldSettings, newItem -> {
                  if (newItem == null) {
                     this.remove();
                  } else {
                     this.item = newItem;
                     this.markDirty();
                  }
               });
            }

            if ((worldSettings == null || worldSettings.survivalMode)
               && this.nextSpoilTickWorldTime <= this.getWorldTime()
               && this.item.item.shouldSpoilTick(this.item)) {
               this.nextSpoilTickWorldTime = this.item.item.tickSpoilTime(this.item, this, 1.0F, newItem -> {
                  if (newItem == null) {
                     this.remove();
                  } else {
                     this.item = newItem;
                     this.markDirty();
                  }
               });
            }
         }
      }
   }

   public boolean canBePickedUpBySettlers() {
      return this.getTarget() == null && this.getReservedAuth() == -1L;
   }

   @Override
   public float getTargetRange(ServerClient client) {
      return super.getTargetRange(client) + client.playerMob.buffManager.getModifier(BuffModifiers.ITEM_PICKUP_RANGE) * 32.0F;
   }

   @Override
   public float getTargetStreamRange() {
      return super.getTargetStreamRange() + BuffModifiers.MAX_PICKUP_RANGE_MODIFIER * 32.0F;
   }

   @Override
   public boolean isValidTarget(ServerClient client) {
      if (this.playerDeathAuth != 0L && this.playerDeathAuth == client.authentication) {
         PlayerInventorySlot slot = new PlayerInventorySlot(this.deathInventoryID, this.deathInventorySlot);
         if (slot.isSlotClear(client.playerMob.getInv())) {
            return true;
         }
      }

      return client.playerMob.getInv().canAddItem(this.item, false, "itempickup") > 0;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         if (this.shouldDraw() && this.sinking < 1.0F) {
            float drawSinking = (float)Math.pow(this.sinking, 0.4F);
            GameLight light = level.getLightLevel(this);
            int drawX = camera.getDrawX(this.x);
            int drawY = camera.getDrawY(this.y) - (int)Math.max(0.0F, this.height);
            drawY += this.getBobbing(this.getX(), this.getY());
            final SharedTextureDrawOptions beamOptions;
            if (this.showsLightBeam) {
               float speed = (float)Math.sqrt(this.dx * this.dx + this.dy * this.dy);
               float speedPercent = 1.0F - GameMath.limit(speed / 70.0F, 0.0F, 1.0F);
               if (speedPercent > 0.0F) {
                  int minHeight = GameMath.lerp(speedPercent, 0, 75);
                  int maxHeight = GameMath.lerp(speedPercent, 0, 200);
                  GameRandom random = new GameRandom(this.getUniqueID());
                  long currentTime = level.getWorldEntity().getLocalTime();
                  beamOptions = new SharedTextureDrawOptions(GameResources.gradient);
                  GameLight beamLight = light.minLevelCopy(100.0F);
                  addLightBeamDraws(
                     beamOptions,
                     currentTime + random.nextInt(1000000),
                     10000,
                     minHeight,
                     maxHeight,
                     290,
                     340,
                     8000,
                     beamLight,
                     camera.getDrawX(this.x),
                     camera.getDrawY(this.y)
                  );
                  addLightBeamDraws(
                     beamOptions,
                     currentTime + random.nextInt(1000000),
                     10000,
                     minHeight,
                     maxHeight,
                     290,
                     340,
                     8000,
                     beamLight,
                     camera.getDrawX(this.x),
                     camera.getDrawY(this.y)
                  );
                  addLightBeamDraws(
                     beamOptions,
                     currentTime + random.nextInt(1000000),
                     10000,
                     minHeight,
                     maxHeight,
                     290,
                     340,
                     8000,
                     beamLight,
                     camera.getDrawX(this.x),
                     camera.getDrawY(this.y)
                  );
               } else {
                  beamOptions = null;
               }
            } else {
               beamOptions = null;
            }

            final DrawOptions options = this.item.getWorldDrawOptions(perspective, drawX, drawY, light, drawSinking);
            list.add(new EntityDrawable(this) {
               @Override
               public void draw(TickManager tickManager) {
                  if (beamOptions != null) {
                     beamOptions.draw();
                  }

                  options.draw();
               }
            });
            int shadowX = camera.getDrawX(this.x);
            int shadowY = camera.getDrawY(this.y);
            shadowY += this.getBobbing(this.getX(), this.getY());
            DrawOptions shadowOptions = this.item.item.getWorldShadowDrawOptions(this.item, perspective, shadowX, shadowY, light, drawSinking);
            if (shadowOptions != null) {
               tileList.add(tm -> shadowOptions.draw());
            }
         }
      }
   }

   public static void addLightBeamDraws(
      SharedTextureDrawOptions options,
      long currentTime,
      int heightAnimTime,
      int minHeight,
      int maxHeight,
      int minHue,
      int maxHue,
      int colorAnimTime,
      GameLight light,
      int drawX,
      int drawY
   ) {
      float heightFloat = GameUtils.getAnimFloatContinuous(currentTime, heightAnimTime);
      float heightSin = (GameMath.sin(heightFloat * 360.0F) + 1.0F) / 2.0F;
      int currentHeight = (int)(heightSin * (maxHeight - minHeight)) + minHeight;
      int midHeight = (int)(currentHeight / 1.5F);
      int topHeight = currentHeight - midHeight;
      float centerFloat = GameUtils.getAnimFloatContinuous(currentTime, heightAnimTime - 1000);
      float centerSin = (GameMath.sin(centerFloat * 360.0F) + 1.0F) / 2.0F;
      int widthPadding = 5;
      int widthSway = 10;
      int leftWidth = (int)(widthSway * centerSin);
      int rightWidth = widthSway - leftWidth;
      float minHueF = minHue / 360.0F;
      float maxHueF = maxHue / 360.0F;
      if (maxHueF < minHueF) {
         maxHueF++;
      }

      float colorFloat = GameUtils.getAnimFloatContinuous(currentTime, colorAnimTime);
      float timeSin = (GameMath.sin(colorFloat * 360.0F) + 1.0F) / 2.0F;
      Color color = new Color(Color.HSBtoRGB(GameMath.lerp(timeSin, minHueF, maxHueF) % 1.0F, 1.0F, 1.0F));
      addLightBeamDraws(
         options, leftWidth + widthPadding, rightWidth + widthPadding, midHeight, topHeight, color, light, drawX + leftWidth / 2 - rightWidth / 2, drawY
      );
   }

   public static void addLightBeamDraws(
      SharedTextureDrawOptions options, int leftWidth, int rightWidth, int midHeight, int topHeight, Color color, GameLight light, int drawX, int drawY
   ) {
      float red = color.getRed() / 255.0F;
      float green = color.getGreen() / 255.0F;
      float blue = color.getBlue() / 255.0F;
      float alpha = color.getAlpha() / 255.0F / 4.0F;
      float brightness = light.getFloatLevel();
      float lightRed = light.getFloatRed();
      float lightGreen = light.getFloatGreen();
      float lightBlue = light.getFloatBlue();
      float finalRed = red * lightRed * brightness;
      float finalGreen = green * lightGreen * brightness;
      float finalBlue = blue * lightBlue * brightness;
      float[] topColorArray = new float[]{
         finalRed,
         finalGreen,
         finalBlue,
         0.0F,
         finalRed,
         finalGreen,
         finalBlue,
         0.0F,
         finalRed,
         finalGreen,
         finalBlue,
         alpha,
         finalRed,
         finalGreen,
         finalBlue,
         alpha
      };
      float[] botColorArray = new float[]{
         finalRed,
         finalGreen,
         finalBlue,
         alpha,
         finalRed,
         finalGreen,
         finalBlue,
         alpha,
         finalRed,
         finalGreen,
         finalBlue,
         0.0F,
         finalRed,
         finalGreen,
         finalBlue,
         0.0F
      };
      options.addFull().size(leftWidth, midHeight).color(finalRed, finalGreen, finalBlue, alpha).mirrorX().pos(drawX - leftWidth, drawY - midHeight);
      options.addFull().size(rightWidth, midHeight).color(finalRed, finalGreen, finalBlue, alpha).pos(drawX, drawY - midHeight);
      options.addFull().size(leftWidth, topHeight).advColor(topColorArray).mirrorX().pos(drawX - leftWidth, drawY - midHeight - topHeight);
      options.addFull().size(rightWidth, topHeight).advColor(topColorArray).pos(drawX, drawY - midHeight - topHeight);
      options.addFull().size(leftWidth, 8).advColor(botColorArray).mirrorX().pos(drawX - leftWidth, drawY);
      options.addFull().size(rightWidth, 8).advColor(botColorArray).pos(drawX, drawY);
   }

   @Override
   public boolean collidesWith(PickupEntity item) {
      if (item.getID() != this.getID()) {
         return false;
      } else {
         return this.playerDeathAuth == 0L && ((ItemPickupEntity)item).playerDeathAuth == 0L ? super.collidesWith(item) : false;
      }
   }

   @Override
   public void collidedWith(PickupEntity pickup) {
      if (pickup != this && pickup.getID() == this.getID() && !pickup.removed()) {
         ItemPickupEntity other = (ItemPickupEntity)pickup;
         if (!this.isOnPickupCooldown()
            && !other.isOnPickupCooldown()
            && other.getReservedAuth() == this.getReservedAuth()
            && this.item.canCombine(this.getLevel(), null, other.item, "pickupcombine")) {
            int startAmount = other.item.getAmount();
            this.item
               .item
               .onCombine(this.getLevel(), null, null, 0, this.item, other.item, Integer.MAX_VALUE, other.item.getAmount(), false, "pickupcombine", null);
            int combinedAmount = startAmount - other.item.getAmount();
            this.spawnTime = Math.max(this.spawnTime, other.spawnTime);
            this.sinking = Math.min(this.sinking, other.sinking);
            this.onItemUpdated();
            this.playerDeathAuth = 0L;
            this.deathInventoryID = 0;
            this.deathInventorySlot = 0;
            this.deathIsLocked = false;
            if (this.isServer()) {
               this.getLevel().getServer().network.sendToClientsWithEntity(new PacketSpawnPickupEntity(this), this);
            }

            other.remove();
            if (combinedAmount > 0) {
               for (ItemPickupReservedAmount reservedPickup : other.reservedPickups) {
                  reservedPickup.submitCombinedEvent(new ItemPickupReservedCombinedEvent(reservedPickup, this, combinedAmount));
               }
            }
         }
      }
   }

   public void onItemUpdated() {
      this.nextSpoilTickWorldTime = 0L;
   }

   @Override
   public void onPickup(ServerClient client) {
      int startAmount = this.item.getAmount();
      AtomicBoolean addedToNonPlayerInventory = new AtomicBoolean();
      boolean result;
      if (this.playerDeathAuth != 0L && this.playerDeathAuth == client.authentication) {
         result = client.playerMob
            .getInv()
            .addItem(
               this.item,
               new PlayerInventorySlot(this.deathInventoryID, this.deathInventorySlot),
               this.deathIsLocked,
               "itempickup",
               (inventory, inventorySlot, amount) -> {
                  if (amount > 0 && !(inventory instanceof PlayerInventory)) {
                     addedToNonPlayerInventory.set(true);
                  }
               }
            );
      } else {
         this.item.setNew(true);
         result = client.playerMob.getInv().addItem(this.item, false, "itempickup", (inventory, inventorySlot, amount) -> {
            if (amount > 0 && !(inventory instanceof PlayerInventory)) {
               addedToNonPlayerInventory.set(true);
            }
         });
      }

      if (result) {
         int pickedUpAmount = startAmount - this.item.getAmount();
         JournalChallengeRegistry.handleListeners(
            client,
            ItemPickedUpJournalChallengeListener.class,
            challenge -> challenge.onItemPickedUp(client, this, pickedUpAmount, addedToNonPlayerInventory.get())
         );
         client.markObtainItem(this.item.item.getStringID());
         if (this.isServer()) {
            PacketPickupItemEntityPickup packet = new PacketPickupItemEntityPickup(this, this.item.copy(pickedUpAmount), addedToNonPlayerInventory.get());
            this.getServer().network.sendToClientsWithAnyRegionExcept(packet, client.playerMob.getRegionPositionsCombined(this), client);
            client.sendPacket(packet);
         }

         this.resetTarget();
         if (this.item.getAmount() == 0) {
            this.remove();
         } else {
            this.sendTargetUpdatePacket();
         }
      }
   }

   @Override
   public void onPickup(ClientClient client, Packet data) {
      GameLog.warn
         .println(
            "Client \""
               + client.getName()
               + "\" received pickup packet for item pickup entity. This should never happen. Instead use "
               + PacketPickupItemEntityPickup.class.getSimpleName()
               + " to send pickup packet"
         );
   }

   public static void onPickup(ItemPickupEntity entity, ClientClient client, PacketPickupItemEntityPickup packet) {
      InventoryItem item = packet.item == null ? null : packet.item.copy();
      if (item != null) {
         int amount = item.getAmount();
         if (packet.playerDeathAuth != 0L && packet.playerDeathAuth == client.authentication) {
            client.playerMob
               .getInv()
               .addItem(item, new PlayerInventorySlot(packet.deathInventoryID, packet.deathInventorySlot), packet.deathIsLocked, "itempickup", null);
         } else {
            item.setNew(true);
            client.playerMob.getInv().addItem(item, false, "itempickup", null);
         }

         if (entity != null) {
            entity.item.setAmount(entity.item.getAmount() - amount);
            if (client.slot == client.getClient().getSlot()) {
               if (Settings.showPickupText) {
                  entity.getLevel()
                     .hudManager
                     .addElement(new ItemPickupText(client.playerMob, new InventoryItem(item.item, amount)).specialOutline(packet.addedToNonPlayerInventory));
               }

               SoundManager.playSound(GameResources.pop, SoundEffect.effect(client.playerMob));
            }
         }
      }
   }

   public ItemPickupEntity setPlayerDeathAuth(NetworkClient client, PlayerInventorySlot slot, boolean isLocked) {
      if (client == null) {
         this.playerDeathAuth = 0L;
         this.deathInventoryID = 0;
         this.deathInventorySlot = 0;
         this.deathIsLocked = false;
      } else {
         this.playerDeathAuth = client.authentication;
         this.deathInventoryID = slot.inventoryID;
         this.deathInventorySlot = slot.slot;
         this.deathIsLocked = isLocked;
      }

      return this;
   }

   public void removeInvalidReservedPickups() {
      this.reservedPickups.removeIf(e -> this.item == null || !e.isReserved(this.getLevel().getWorldEntity()));
   }

   public int getReservedAmount() {
      return this.reservedPickups.stream().filter(e -> e.isReserved(this.getLevel().getWorldEntity())).mapToInt(e -> e.pickupAmount).sum();
   }

   public int getAvailableAmount() {
      return this.item.getAmount() - this.getReservedAmount();
   }

   public ItemPickupReservedAmount reservePickupAmount(int amount) {
      this.removeInvalidReservedPickups();
      int reservedAmount = this.getReservedAmount();
      if (reservedAmount >= this.item.getAmount()) {
         return null;
      }

      int pickedUpAmount = Math.min(amount, this.item.getAmount());
      ItemPickupReservedAmount reserved = new ItemPickupReservedAmount(this, pickedUpAmount, reservedAmount);
      reserved.init(this.reservedPickups.addFirst(reserved), this.getLevel().getWorldEntity());
      return reserved;
   }

   @Override
   public boolean shouldAddToDeletedLevelReturnedPickups() {
      return this.playerDeathAuth != 0L;
   }

   @Override
   public void restore() {
      super.restore();
      this.resetTarget();
      this.dx = 0.0F;
      this.dy = 0.0F;
   }

   @Override
   public boolean onMouseHover(GameCamera camera, PlayerMob perspective, GameBlackboard blackboard, boolean debug) {
      super.onMouseHover(camera, perspective, blackboard, debug);
      StringTooltips tips = new StringTooltips(
         this.item.getItemDisplayName() + (this.item.getAmount() != 1 ? " (" + this.item.getAmount() + ")" : ""), this.item.item.getRarityColor(this.item)
      );
      if (debug) {
         tips.add("Name: " + this.item.getItemDisplayName());
         tips.add("Amount: " + this.item.getAmount());
         tips.add("StringID: " + this.item.item.getStringID());
         tips.add("Height: " + this.height + ", " + this.dh);
         if (this.playerDeathAuth != 0L) {
            tips.add("Player death: " + this.playerDeathAuth + ", " + this.deathInventoryID + ", " + this.deathInventorySlot);
         }
      }

      GameTooltipManager.addTooltip(tips, TooltipLocation.INTERACT_FOCUS);
      return true;
   }

   @Override
   public String toString() {
      return super.toString() + "{" + this.item + "}";
   }
}
