package necesse.entity.objectEntity;

import java.util.Arrays;
import java.util.Objects;
import necesse.engine.MusicList;
import necesse.engine.MusicOptions;
import necesse.engine.MusicOptionsOffset;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.GameMusic;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.miscItem.VinylItem;

public abstract class MusicPlayerManager {
   protected boolean isPaused;
   protected boolean hasAnyVinyls;
   protected VinylItem[] currentVinyls;
   protected MusicList currentMusicList;
   protected long playingOffset;

   public MusicPlayerManager(int slots) {
      this.currentVinyls = new VinylItem[slots];
   }

   public boolean fixSlots(int slots) {
      if (this.currentVinyls.length != slots) {
         this.currentVinyls = Arrays.copyOf(this.currentVinyls, slots);
         return true;
      } else {
         return false;
      }
   }

   public boolean fixSlots(Inventory inventory, boolean isClient) {
      if (this.fixSlots(inventory.getSize())) {
         this.updatePlayingAll(inventory, isClient);
      }

      return false;
   }

   public abstract void markDirty();

   public void addSaveData(SaveData save) {
      save.addBoolean("isPaused", this.isPaused);
      save.addLong("playingOffset", this.getMusicPlayingOffset());
   }

   public void applyLoadData(LoadData save) {
      this.isPaused = save.getBoolean("isPaused", false, false);
      long playingOffset = save.getLong("playingOffset", 0L, false);
      if (!this.hasAnyVinyls) {
         this.playingOffset = 0L;
      } else if (this.isPaused) {
         this.playingOffset = playingOffset;
      } else {
         this.playingOffset = System.currentTimeMillis() - playingOffset;
      }

      if (this.currentMusicList != null) {
         this.currentMusicList.setTimeOffset(this.getMusicPlayingOffset());
      }
   }

   public void writeContentPacket(PacketWriter writer, boolean full) {
      if (full) {
         writer.putNextShortUnsigned(this.currentVinyls.length);

         for (VinylItem currentVinyl : this.currentVinyls) {
            if (currentVinyl != null) {
               writer.putNextBoolean(true);
               writer.putNextShortUnsigned(currentVinyl.getID());
            } else {
               writer.putNextBoolean(false);
            }
         }

         writer.putNextBoolean(this.hasAnyVinyls);
      }

      writer.putNextBoolean(this.isPaused);
      writer.putNextLong(this.getMusicPlayingOffset());
   }

   public void readContentPacket(PacketReader reader, boolean full) {
      if (full) {
         int length = reader.getNextShortUnsigned();

         for (int i = 0; i < length; i++) {
            VinylItem vinylItem = null;
            if (reader.getNextBoolean()) {
               Item item = ItemRegistry.getItem(reader.getNextShortUnsigned());
               if (item instanceof VinylItem) {
                  vinylItem = (VinylItem)item;
               }
            }

            if (i < this.currentVinyls.length) {
               this.currentVinyls[i] = vinylItem;
            }
         }

         this.hasAnyVinyls = reader.getNextBoolean();
      }

      this.isPaused = reader.getNextBoolean();
      long offset = reader.getNextLong();
      if (!this.hasAnyVinyls) {
         this.playingOffset = 0L;
      } else if (this.isPaused) {
         this.playingOffset = offset;
      } else {
         this.playingOffset = System.currentTimeMillis() - offset;
      }

      if (this.currentMusicList != null) {
         this.currentMusicList.setTimeOffset(this.getMusicPlayingOffset());
      }
   }

   public MusicList getCurrentMusicList() {
      if (this.currentMusicList != null) {
         this.currentMusicList.setTimeOffset(this.getMusicPlayingOffset());
      }

      return this.currentMusicList;
   }

   public boolean hasAnyVinyls() {
      return this.hasAnyVinyls;
   }

   public int getSlots() {
      return this.currentVinyls.length;
   }

   public MusicOptionsOffset getCurrentMusic() {
      if (this.currentMusicList != null) {
         this.currentMusicList.setTimeOffset(this.getMusicPlayingOffset());
         return this.currentMusicList.getExpectedMusicPlaying();
      } else {
         return null;
      }
   }

   public MusicOptions getPreviousMusic() {
      int lastSlotPlaying = -1;
      MusicOptionsOffset lastMusic = this.getCurrentMusic();
      if (lastMusic != null) {
         MusicPlayerManager.ItemMusicOptions lastItemOptions = toItemMusic(lastMusic.options);
         if (lastItemOptions != null) {
            lastSlotPlaying = lastItemOptions.itemSlot;
         }
      }

      if (lastSlotPlaying != -1) {
         for (int i = this.currentVinyls.length; i > 0; i--) {
            int slot = Math.floorMod(lastSlotPlaying + i - 1, this.currentVinyls.length);
            if (this.currentVinyls[slot] != null) {
               return new MusicPlayerManager.ItemMusicOptions(this.currentVinyls[slot], slot);
            }
         }
      }

      return null;
   }

   public boolean setDesiredMusic(GameMusic music, long offset) {
      if (this.currentMusicList == null) {
         return false;
      }

      long currentOffset = 0L;

      for (MusicOptions options : this.currentMusicList.getMusicInList()) {
         if (options.music == music) {
            if (this.isPaused) {
               this.playingOffset = currentOffset + offset;
            } else {
               this.playingOffset = System.currentTimeMillis() + currentOffset + offset;
            }

            return true;
         }

         currentOffset += options.getMusicListMilliseconds();
      }

      return false;
   }

   public void setIsPaused(boolean isPaused) {
      if (this.isPaused != isPaused) {
         this.isPaused = isPaused;
         if (!this.hasAnyVinyls) {
            this.playingOffset = 0L;
         } else {
            this.playingOffset = System.currentTimeMillis() - this.playingOffset;
         }

         this.markDirty();
      }
   }

   public boolean isPaused() {
      return this.isPaused;
   }

   public void forwardMilliseconds(long milliseconds) {
      if (this.hasAnyVinyls) {
         if (milliseconds != 0L) {
            if (this.isPaused) {
               this.playingOffset -= milliseconds;
            } else {
               this.playingOffset += milliseconds;
            }

            this.markDirty();
         }
      }
   }

   public void setOffset(long milliseconds) {
      if (this.hasAnyVinyls) {
         if (this.isPaused) {
            this.playingOffset = milliseconds;
         } else {
            this.playingOffset = System.currentTimeMillis() + milliseconds;
         }

         this.markDirty();
      }
   }

   public long getMusicPlayingOffset() {
      if (!this.hasAnyVinyls) {
         return 0L;
      } else {
         return this.isPaused ? this.playingOffset : System.currentTimeMillis() - this.playingOffset;
      }
   }

   public void updateHasAnyVinyls(boolean isClient) {
      boolean lastHasAnyVinyls = this.hasAnyVinyls;
      this.hasAnyVinyls = Arrays.stream(this.currentVinyls).anyMatch(o -> !Objects.isNull(o));
      if (this.hasAnyVinyls) {
         if (!lastHasAnyVinyls) {
            if (this.isPaused) {
               this.playingOffset = 0L;
            } else {
               this.playingOffset = System.currentTimeMillis();
            }
         }

         if (isClient) {
            this.updateClientPlaying();
         }
      } else {
         this.currentMusicList = null;
         if (this.isPaused) {
            this.playingOffset = 0L;
         } else {
            this.playingOffset = System.currentTimeMillis();
         }
      }
   }

   public void updateClientPlaying() {
      int lastSlotPlaying = -1;
      MusicOptionsOffset lastMusic = this.getCurrentMusic();
      if (lastMusic != null) {
         MusicPlayerManager.ItemMusicOptions lastItemOptions = toItemMusic(lastMusic.options);
         if (lastItemOptions != null) {
            lastSlotPlaying = lastItemOptions.itemSlot;
         }
      }

      this.currentMusicList = new MusicList();
      long offset = 0L;
      boolean found = false;

      for (int slot = 0; slot < this.currentVinyls.length; slot++) {
         VinylItem item = this.currentVinyls[slot];
         if (item != null) {
            MusicOptions next = new MusicPlayerManager.ItemMusicOptions(item, slot).fadeInTime(1000).volume(1.5F);
            if (lastMusic != null) {
               if (lastSlotPlaying == slot) {
                  offset -= lastMusic.offset;
                  found = true;
               } else if (!found) {
                  offset -= next.music.sound.getLengthInMillis() - next.getFadeOutTime();
               }
            }

            this.currentMusicList.addMusic(next);
         }
      }

      if (this.isPaused) {
         this.playingOffset = offset;
      } else {
         this.playingOffset = System.currentTimeMillis() + offset;
      }
   }

   public void updatePlaying(Inventory inventory, int inventorySlot, boolean isClient) {
      InventoryItem item = inventory.getItem(inventorySlot);
      if (item != null && item.item instanceof VinylItem) {
         VinylItem vinylItem = (VinylItem)item.item;
         if (this.currentVinyls[inventorySlot] != vinylItem) {
            this.currentVinyls[inventorySlot] = vinylItem;
            this.updateHasAnyVinyls(isClient);
         }
      } else if (this.currentVinyls[inventorySlot] != null) {
         this.currentVinyls[inventorySlot] = null;
         this.updateHasAnyVinyls(isClient);
      }
   }

   public void updatePlayingAll(Inventory inventory, boolean isClient) {
      boolean updated = false;

      for (int slot = 0; slot < this.currentVinyls.length; slot++) {
         InventoryItem item = inventory.getItem(slot);
         if (item != null && item.item instanceof VinylItem) {
            VinylItem vinylItem = (VinylItem)item.item;
            if (this.currentVinyls[slot] != vinylItem) {
               this.currentVinyls[slot] = vinylItem;
               updated = true;
            }
         } else if (this.currentVinyls[slot] != null) {
            this.currentVinyls[slot] = null;
            updated = true;
         }
      }

      if (updated) {
         this.updateHasAnyVinyls(isClient);
      }
   }

   protected static MusicPlayerManager.ItemMusicOptions toItemMusic(MusicOptions options) {
      return options instanceof MusicPlayerManager.ItemMusicOptions ? (MusicPlayerManager.ItemMusicOptions)options : null;
   }

   public boolean isSame(MusicPlayerManager other) {
      if (this.isPaused != other.isPaused) {
         return false;
      } else if (this.hasAnyVinyls != other.hasAnyVinyls) {
         return false;
      } else {
         return Math.abs(this.playingOffset - other.playingOffset) >= 500L
            ? false
            : this.hasAnyVinyls && Arrays.equals(this.currentVinyls, other.currentVinyls);
      }
   }

   public String getCurrentVinylsDebug() {
      return Arrays.toString(Arrays.stream(this.currentVinyls).map(c -> c == null ? null : c.getStringID()).toArray());
   }

   protected static class ItemMusicOptions extends MusicOptions {
      public final VinylItem vinylItem;
      public final int itemSlot;

      public ItemMusicOptions(VinylItem vinylItem, int itemSlot) {
         super(vinylItem.music);
         this.vinylItem = vinylItem;
         this.itemSlot = itemSlot;
      }
   }
}
