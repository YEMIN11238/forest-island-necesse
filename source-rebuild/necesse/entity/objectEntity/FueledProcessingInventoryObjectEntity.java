package necesse.entity.objectEntity;

import java.util.ArrayList;
import java.util.function.Function;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketOEProgressUpdate;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.save.levelData.InventorySave;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.gfx.forms.presets.containerComponent.object.ProcessingHelp;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.level.maps.Level;

public abstract class FueledProcessingInventoryObjectEntity extends ObjectEntity implements OEInventory, IProgressObjectEntity {
   public final Inventory inventory;
   public final int fuelSlots;
   public final int inputSlots;
   public final int outputSlots;
   public final boolean fuelAlwaysOn;
   public final boolean fuelRunsOutWhenNotProcessing;
   public final boolean runningOutOfFuelResetsProcessingTime;
   public boolean useWorldTime = true;
   protected boolean forceUpdate = true;
   protected boolean keepFuelRunning;
   protected long lastTickedTime;
   protected int remainingFuelTime;
   protected int remainingProcessingTime;
   protected int usedFuelTime;
   protected int usedProcessingTime;
   protected boolean processingPaused;
   protected boolean fuelPaused;
   protected int lastProcessingHash;
   protected boolean progressDirty = false;

   public FueledProcessingInventoryObjectEntity(
      Level level,
      String type,
      int x,
      int y,
      int fuelSlots,
      int inputSlots,
      int outputSlots,
      boolean fuelAlwaysOn,
      boolean fuelRunsOutWhenNotProcessing,
      boolean runningOutOfFuelResetsProcessingTime
   ) {
      super(level, type, x, y);
      this.fuelSlots = fuelSlots;
      this.inputSlots = inputSlots;
      this.outputSlots = outputSlots;
      this.fuelAlwaysOn = fuelAlwaysOn;
      this.fuelRunsOutWhenNotProcessing = fuelRunsOutWhenNotProcessing;
      this.runningOutOfFuelResetsProcessingTime = runningOutOfFuelResetsProcessingTime;
      this.inventory = new Inventory(fuelSlots + inputSlots + outputSlots) {
         @Override
         public void updateSlot(int slot) {
            super.updateSlot(slot);
            FueledProcessingInventoryObjectEntity.this.onSlotUpdate(slot);
         }
      };
      this.inventory.filter = (slot, item) -> {
         if (item == null) {
            return true;
         } else if (slot < fuelSlots) {
            return this.isValidFuelItem(item);
         } else {
            return slot < fuelSlots + inputSlots ? this.isValidInputItem(item) : false;
         }
      };
   }

   protected void onSlotUpdate(int slot) {
      this.forceNextUpdate();
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addSaveData(InventorySave.getSave(this.inventory));
      save.addLong("lastTickedTime", this.lastTickedTime);
      save.addInt("remainingFuelTime", this.remainingFuelTime);
      save.addInt("remainingProcessingTime", this.remainingProcessingTime);
      save.addInt("usedFuelTime", this.usedFuelTime);
      save.addInt("usedProcessingTime", this.usedProcessingTime);
      save.addInt("lastProcessingHash", this.lastProcessingHash);
      if (!this.fuelAlwaysOn) {
         save.addBoolean("keepFuelRunning", this.keepFuelRunning);
      }

      save.addBoolean("fuelPaused", this.fuelPaused);
      save.addBoolean("processingPaused", this.processingPaused);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.inventory.override(InventorySave.loadSave(save.getFirstLoadDataByName("INVENTORY")));
      this.lastTickedTime = save.getLong("lastTickedTime", 0L);
      this.remainingFuelTime = save.getInt("remainingFuelTime", 0);
      this.remainingProcessingTime = save.getInt("remainingProcessingTime", 0);
      this.usedFuelTime = save.getInt("usedFuelTime", 0);
      this.usedProcessingTime = save.getInt("usedProcessingTime", 0);
      this.lastProcessingHash = save.getInt("lastProcessingHash", 0);
      if (!this.fuelAlwaysOn) {
         this.keepFuelRunning = save.getBoolean("keepFuelRunning", this.keepFuelRunning);
      }

      this.fuelPaused = save.getBoolean("fuelPaused", false);
      this.processingPaused = save.getBoolean("processingPaused", false);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      this.inventory.writeContent(writer);
      writer.putNextInt(this.remainingFuelTime);
      writer.putNextInt(this.remainingProcessingTime);
      writer.putNextInt(this.usedFuelTime);
      writer.putNextInt(this.usedProcessingTime);
      if (!this.fuelAlwaysOn) {
         writer.putNextBoolean(this.keepFuelRunning);
      }

      writer.putNextBoolean(this.processingPaused);
      writer.putNextBoolean(this.fuelPaused);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      this.inventory.override(Inventory.getInventory(reader));
      boolean oldIsFuelRunning = this.isFuelRunning();
      this.remainingFuelTime = reader.getNextInt();
      this.remainingProcessingTime = reader.getNextInt();
      this.usedFuelTime = reader.getNextInt();
      this.usedProcessingTime = reader.getNextInt();
      if (!this.fuelAlwaysOn) {
         this.keepFuelRunning = reader.getNextBoolean();
      }

      this.processingPaused = reader.getNextBoolean();
      this.fuelPaused = reader.getNextBoolean();
      if (oldIsFuelRunning != this.isFuelRunning()) {
         this.getLevel().lightManager.updateStaticLight(this.tileX, this.tileY);
      }
   }

   @Override
   public void setupProgressPacket(PacketWriter writer) {
      writer.putNextInt(this.remainingFuelTime);
      writer.putNextInt(this.remainingProcessingTime);
      writer.putNextInt(this.usedFuelTime);
      writer.putNextInt(this.usedProcessingTime);
      if (!this.fuelAlwaysOn) {
         writer.putNextBoolean(this.keepFuelRunning);
      }

      writer.putNextBoolean(this.processingPaused);
      writer.putNextBoolean(this.fuelPaused);
   }

   @Override
   public void applyProgressPacket(PacketReader reader) {
      boolean oldIsFuelRunning = this.isFuelRunning();
      this.remainingFuelTime = reader.getNextInt();
      this.remainingProcessingTime = reader.getNextInt();
      this.usedFuelTime = reader.getNextInt();
      this.usedProcessingTime = reader.getNextInt();
      if (!this.fuelAlwaysOn) {
         this.keepFuelRunning = reader.getNextBoolean();
      }

      this.processingPaused = reader.getNextBoolean();
      this.fuelPaused = reader.getNextBoolean();
      if (oldIsFuelRunning != this.isFuelRunning()) {
         this.getLevel().lightManager.updateStaticLight(this.tileX, this.tileY);
      }
   }

   @Override
   public ArrayList<InventoryItem> getDroppedItems() {
      ArrayList<InventoryItem> list = new ArrayList<>();

      for (int i = 0; i < this.inventory.getSize(); i++) {
         if (!this.inventory.isSlotClear(i)) {
            list.add(this.inventory.getItem(i));
         }
      }

      return list;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      Performance.record(this.getLevel().tickManager(), "tickItems", () -> this.inventory.tickItems(this));
      long currentTime = this.useWorldTime ? this.getWorldEntity().getWorldTime() : this.getWorldEntity().getTime();
      long lastTime = this.lastTickedTime == 0L ? currentTime : this.lastTickedTime;
      long addedTime = Math.max(0L, currentTime - lastTime);
      if (!this.fuelPaused) {
         this.remainingFuelTime = (int)Math.max(this.remainingFuelTime - addedTime, 0L);
         if (this.remainingFuelTime > 0) {
            this.usedFuelTime = (int)(this.usedFuelTime + addedTime);
         }
      }

      if (!this.processingPaused) {
         this.remainingProcessingTime = (int)Math.max(this.remainingProcessingTime - addedTime, 0L);
         if (this.remainingProcessingTime > 0) {
            this.usedProcessingTime = (int)(this.usedProcessingTime + addedTime);
         }
      }

      this.lastTickedTime = currentTime;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Performance.record(this.getLevel().tickManager(), "tickItems", () -> this.inventory.tickItems(this));
      long currentTime = this.useWorldTime ? this.getWorldEntity().getWorldTime() : this.getWorldEntity().getTime();
      if (this.forceUpdate || this.remainingFuelTime > 0 || this.remainingProcessingTime > 0) {
         long lastTime = this.lastTickedTime == 0L ? currentTime : this.lastTickedTime;
         long addedTime = currentTime - lastTime;

         while (addedTime > 0L || this.forceUpdate) {
            if (this.forceUpdate) {
               this.forceUpdate = false;
               if (this.keepFuelRunning || this.fuelAlwaysOn) {
                  if (this.remainingFuelTime <= 0) {
                     this.useFuel(true);
                  }

                  if (this.fuelPaused) {
                     this.fuelPaused = false;
                     this.markProgressDirty();
                  }
               }

               FueledProcessingInventoryObjectEntity.NextProcessTask nextProcessTask = this.getNextProcessTask();
               if (nextProcessTask != null) {
                  if (this.lastProcessingHash != nextProcessTask.recipeHash) {
                     this.markProgressDirty();
                     this.lastProcessingHash = nextProcessTask.recipeHash;
                     this.remainingProcessingTime = nextProcessTask.processTime;
                     this.usedProcessingTime = 0;
                  }
               } else {
                  if (this.lastProcessingHash != 0 || this.remainingProcessingTime != 0 || this.usedProcessingTime != 0) {
                     this.markProgressDirty();
                  }

                  this.lastProcessingHash = 0;
                  this.remainingProcessingTime = 0;
                  this.usedProcessingTime = 0;
               }
            }

            boolean done = true;
            if (this.lastProcessingHash != 0) {
               done = false;
               boolean couldNotUseFuel = false;
               if (this.remainingFuelTime > 0) {
                  if (this.processingPaused) {
                     this.markProgressDirty();
                  }

                  this.processingPaused = false;
               } else if (!this.useFuel(true)) {
                  couldNotUseFuel = true;
                  if (!this.runningOutOfFuelResetsProcessingTime) {
                     if (!this.processingPaused) {
                        this.markProgressDirty();
                     }

                     this.processingPaused = true;
                  } else {
                     if (this.usedProcessingTime > 0 || !this.processingPaused) {
                        this.markProgressDirty();
                     }

                     this.remainingProcessingTime = this.remainingProcessingTime + this.usedProcessingTime;
                     this.usedProcessingTime = 0;
                     this.processingPaused = true;
                  }
               } else {
                  if (this.processingPaused) {
                     this.markProgressDirty();
                  }

                  this.processingPaused = false;
               }

               long usedTime = Math.max(0L, GameMath.min(this.remainingFuelTime, this.remainingProcessingTime, addedTime));
               this.remainingFuelTime = (int)(this.remainingFuelTime - usedTime);
               this.usedFuelTime = (int)(this.usedFuelTime + usedTime);
               this.remainingProcessingTime = (int)(this.remainingProcessingTime - usedTime);
               this.usedProcessingTime = (int)(this.usedProcessingTime + usedTime);
               addedTime -= usedTime;
               if (this.remainingProcessingTime <= 0) {
                  if (this.processInput()) {
                     this.markProgressDirty();
                     this.lastProcessingHash = 0;
                     this.remainingProcessingTime = 0;
                     this.usedProcessingTime = 0;
                     this.forceNextUpdate();
                  } else {
                     if (!this.fuelPaused) {
                        this.markProgressDirty();
                     }

                     this.fuelPaused = true;
                     done = true;
                  }
               } else if (this.fuelPaused) {
                  this.fuelPaused = false;
                  this.markProgressDirty();
               }

               if (this.remainingFuelTime <= 0) {
                  if (!couldNotUseFuel) {
                     this.forceNextUpdate();
                  } else {
                     done = true;
                  }

                  if (this.usedFuelTime != 0 || !this.fuelPaused) {
                     this.markProgressDirty();
                  }

                  this.usedFuelTime = 0;
                  this.fuelPaused = false;
               }
            } else {
               if (this.remainingFuelTime > 0) {
                  if (!this.fuelRunsOutWhenNotProcessing && !this.fuelAlwaysOn && !this.keepFuelRunning) {
                     if (!this.fuelPaused) {
                        this.markProgressDirty();
                     }

                     this.fuelPaused = true;
                  } else {
                     done = false;
                     long usedTime = Math.max(0L, GameMath.min(this.remainingFuelTime, addedTime));
                     this.remainingFuelTime = (int)(this.remainingFuelTime - usedTime);
                     this.usedFuelTime = (int)(this.usedFuelTime + usedTime);
                     addedTime -= usedTime;
                  }
               } else {
                  if (this.usedFuelTime != 0 || !this.fuelPaused) {
                     this.markProgressDirty();
                  }

                  this.usedFuelTime = 0;
                  if (!this.fuelAlwaysOn && !this.keepFuelRunning) {
                     this.fuelPaused = true;
                  } else {
                     done = false;
                     boolean couldNotUseFuel = !this.useFuel(true);
                     if (this.remainingFuelTime <= 0) {
                        if (!couldNotUseFuel) {
                           this.forceNextUpdate();
                        } else {
                           done = true;
                        }
                     }

                     this.fuelPaused = false;
                  }
               }

               if (!this.processingPaused) {
                  this.markProgressDirty();
               }

               this.processingPaused = true;
            }

            if (done) {
               break;
            }
         }
      }

      this.lastTickedTime = currentTime;
      if (this.progressDirty) {
         if (this.isServer()) {
            this.getLevel().getServer().network.sendToClientsWithEntity(new PacketOEProgressUpdate(this), this);
         }

         this.progressDirty = false;
         this.getLevel().lightManager.updateStaticLight(this.tileX, this.tileY);
      }

      this.serverTickInventorySync(this.getLevel().getServer(), this);
   }

   public boolean useFuel(boolean startPaused) {
      boolean oldIsFuelRunning = this.isFuelRunning();
      int nextFuelBurnTime = Math.max(0, this.getNextFuelBurnTime(true));
      this.remainingFuelTime += nextFuelBurnTime;
      if (this.remainingFuelTime > 0 && this.fuelPaused && startPaused) {
         this.fuelPaused = false;
         this.markProgressDirty();
      }

      if (nextFuelBurnTime != 0) {
         this.markProgressDirty();
      }

      if (oldIsFuelRunning != this.isFuelRunning()) {
         this.getLevel().lightManager.updateStaticLight(this.tileX, this.tileY);
      }

      return nextFuelBurnTime > 0;
   }

   public boolean canUseFuel() {
      return this.getNextFuelBurnTime(false) > 0;
   }

   @Override
   public void markClean() {
      super.markClean();
      this.inventory.clean();
      this.progressDirty = false;
   }

   public abstract boolean isValidFuelItem(InventoryItem var1);

   public abstract int getNextFuelBurnTime(boolean var1);

   protected int itemToBurnTime(boolean useItem, Function<InventoryItem, Integer> itemToBurnTime) {
      for (int i = this.fuelSlots - 1; i >= 0; i--) {
         InventoryItem item = this.inventory.getItem(i);
         if (item != null) {
            int fuelBurnTime = itemToBurnTime.apply(item);
            if (fuelBurnTime > 0) {
               if (useItem) {
                  this.inventory.addAmount(i, -1);
                  if (this.inventory.getAmount(i) <= 0) {
                     this.inventory.setItem(i, null);
                  }

                  this.inventory.markDirty(i);
               }

               return fuelBurnTime;
            }
         }
      }

      return 0;
   }

   public abstract boolean isValidInputItem(InventoryItem var1);

   public abstract FueledProcessingInventoryObjectEntity.NextProcessTask getNextProcessTask();

   public abstract boolean processInput();

   public abstract ProcessingHelp getProcessingHelp();

   public boolean canAddOutput(InventoryItem... outputItems) {
      Inventory copy = this.inventory.copy();

      for (InventoryItem outputItem : outputItems) {
         InventoryItem outputItemCopy = outputItem.copy();
         if (!copy.addItem(this.getLevel(), null, outputItemCopy, this.fuelSlots + this.inputSlots, copy.getSize() - 1, false, "add", true, false, null)) {
            return false;
         }

         if (outputItemCopy.getAmount() > 0) {
            return false;
         }
      }

      return true;
   }

   public void addOutput(InventoryItem... outputItems) {
      for (InventoryItem outputItem : outputItems) {
         this.inventory
            .addItem(this.getLevel(), null, outputItem, this.fuelSlots + this.inputSlots, this.inventory.getSize() - 1, false, "add", true, false, null);
      }
   }

   public void forceNextUpdate() {
      this.forceUpdate = true;
   }

   public void markProgressDirty() {
      this.progressDirty = true;
   }

   public boolean isFuelRunning() {
      return !this.fuelPaused && (this.remainingFuelTime > 0 || this.usedFuelTime > 0);
   }

   public boolean hasFuel() {
      return this.remainingFuelTime > 0 || this.getNextFuelBurnTime(false) > 0;
   }

   public boolean isProcessingRunning() {
      return !this.processingPaused;
   }

   public boolean shouldKeepFuelRunning() {
      return this.keepFuelRunning;
   }

   public void setKeepFuelRunning(boolean keepFuelRunning) {
      if (this.keepFuelRunning != keepFuelRunning) {
         this.keepFuelRunning = keepFuelRunning;
         this.forceNextUpdate();
         this.markProgressDirty();
      }
   }

   public boolean shouldBeAbleToChangeKeepFuelRunning() {
      return true;
   }

   public float getFuelProgress() {
      if (this.remainingFuelTime > 0) {
         int totalFuelTime = this.usedFuelTime + this.remainingFuelTime;
         return Math.abs(GameMath.limit((float)this.usedFuelTime / totalFuelTime, 0.0F, 1.0F) - 1.0F);
      } else {
         return 0.0F;
      }
   }

   public float getProcessingProgress() {
      if (this.remainingProcessingTime > 0) {
         int totalProcessingTime = this.usedProcessingTime + this.remainingProcessingTime;
         return GameMath.limit((float)this.usedProcessingTime / totalProcessingTime, 0.0F, 1.0F);
      } else {
         return this.processingPaused ? 0.0F : 1.0F;
      }
   }

   @Override
   public Inventory getInventory() {
      return this.inventory;
   }

   @Override
   public GameMessage getInventoryName() {
      return this.getObject().getLocalization();
   }

   @Override
   public boolean canQuickStackInventory() {
      return false;
   }

   @Override
   public boolean canRestockInventory() {
      return false;
   }

   @Override
   public boolean canSortInventory() {
      return false;
   }

   @Override
   public boolean canUseForNearbyCrafting() {
      return false;
   }

   @Override
   public InventoryRange getSettlementStorage() {
      return null;
   }

   public InventoryRange getFuelInventoryRange() {
      return new InventoryRange(this.inventory, 0, this.fuelSlots - 1);
   }

   public InventoryRange getInputInventoryRange() {
      return new InventoryRange(this.inventory, this.fuelSlots, this.fuelSlots + this.inputSlots - 1);
   }

   public InventoryRange getOutputInventoryRange() {
      return new InventoryRange(this.inventory, this.fuelSlots + this.inputSlots, this.inventory.getSize() - 1);
   }

   @Override
   public void onMouseHover(PlayerMob perspective, boolean debug) {
      super.onMouseHover(perspective, debug);
   }

   public static class NextProcessTask {
      public final int recipeHash;
      public final int processTime;

      public NextProcessTask(int recipeHash, int processTime) {
         this.recipeHash = recipeHash == 0 ? 1415926 : recipeHash;
         this.processTime = processTime;
      }
   }
}
