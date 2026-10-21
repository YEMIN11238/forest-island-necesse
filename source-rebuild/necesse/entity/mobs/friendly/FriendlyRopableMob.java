package necesse.entity.mobs.friendly;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemInventoryItem;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.ItemCostList;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.leaves.FollowerAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.HumanAngerTargetAINode;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.trails.RopeTrail;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.mob.BuyAnimalContainer;
import necesse.inventory.item.miscItem.RopeItem;
import necesse.level.maps.Level;

public class FriendlyRopableMob extends FriendlyMob implements BuyableAnimalMob, ClientInteractMob {
   protected RopeTrail ropeTrail;
   private final LevelMob<Mob> ropeMob = new LevelMob<Mob>() {
      @Override
      public void onMobChanged(Mob oldMob, Mob newMob) {
         FriendlyRopableMob.this.respawnRopeTrail(newMob);
         if (newMob != null) {
            FriendlyRopableMob.this.lastClearRopeCheckTime = newMob.getTime();
         }
      }

      @Override
      public Level onMobChangedLevel(Mob mob, Level currentLevel) {
         if (currentLevel.isServer()) {
            currentLevel.entityManager.changeMobLevel(FriendlyRopableMob.this, mob.getLevel(), mob.getX(), mob.getY(), true);
            FriendlyRopableMob.this.respawnRopeTrail(mob);
            return mob.getLevel();
         } else {
            return super.onMobChangedLevel(mob, currentLevel);
         }
      }

      @Override
      public void onMobRemoved(Mob mob) {
         super.onMobRemoved(mob);
         if (FriendlyRopableMob.this.isServer() && FriendlyRopableMob.this.removeIfRoperRemoved && !mob.hasDied()) {
            FriendlyRopableMob.this.roperRemoved = true;
            FriendlyRopableMob.this.remove();
         }
      }
   };
   protected InventoryItem ropeItem;
   protected int ropeRange = 100;
   protected float ropeOverDistanceBuffer;
   public boolean removeIfRoperRemoved;
   public boolean shouldEscape;
   protected boolean roperRemoved;
   public ItemCostList buyPrice;
   protected long lastClearRopeCheckTime;
   public final InteractingClients interactingClients = new InteractingClients(this) {
      @Override
      public synchronized void refresh(ServerClient client) {
         super.refresh(client);
         Mob roper = FriendlyRopableMob.this.getRopeMob();
         if (roper instanceof ClientInteractMob) {
            ((ClientInteractMob)roper).refreshInteracting(client);
         }
      }

      @Override
      public synchronized void remove(ServerClient client) {
         super.remove(client);
         Mob roper = FriendlyRopableMob.this.getRopeMob();
         if (roper instanceof ClientInteractMob) {
            ((ClientInteractMob)roper).removeInteracting(client);
         }
      }
   };

   public FriendlyRopableMob(int health) {
      super(health);
   }

   protected int getRopeRange() {
      return this.ropeRange;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("ropeMobUniqueID", this.ropeMob.uniqueID);
      if (this.ropeItem != null) {
         SaveData ropeItem = new SaveData("ropeItem");
         this.ropeItem.addSaveData(ropeItem);
         save.addSaveData(ropeItem);
         save.addInt("ropeRange", this.ropeRange);
         save.addBoolean("removeIfRoperRemoved", this.removeIfRoperRemoved);
      }

      if (this.buyPrice != null) {
         this.buyPrice.addSaveData(save, "buyPrice");
      }

      save.addLong("lastClearRopeCheckTime", this.lastClearRopeCheckTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.ropeMob.uniqueID = save.getInt("ropeMobUniqueID", -1);
      this.respawnRopeTrail(this.getRopeMob());
      LoadData ropeItem = save.getFirstLoadDataByName("ropeItem");
      if (ropeItem != null) {
         this.ropeItem = InventoryItem.fromLoadData(ropeItem);
      }

      this.ropeRange = save.getInt("ropeRange", this.ropeRange, false);
      this.removeIfRoperRemoved = save.getBoolean("removeIfRoperRemoved", this.removeIfRoperRemoved, false);
      this.buyPrice = ItemCostList.fromLoadData(save, "buyPrice", false);
      this.lastClearRopeCheckTime = save.getLong("lastClearRopeCheckTime", this.lastClearRopeCheckTime, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      if (this.buyPrice != null) {
         writer.putNextBoolean(true);
         this.buyPrice.writePacketData(writer);
      } else {
         writer.putNextBoolean(false);
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      if (reader.getNextBoolean()) {
         this.buyPrice = new ItemCostList(reader);
      } else {
         this.buyPrice = null;
      }
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextInt(this.ropeMob.uniqueID);
      InventoryItem.addPacketContent(this.ropeItem, writer);
      if (this.ropeItem != null) {
         writer.putNextShortUnsigned(this.ropeRange);
      }
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.ropeMob.uniqueID = reader.getNextInt();
      this.ropeItem = InventoryItem.fromContentPacket(reader);
      if (this.ropeItem != null) {
         this.ropeRange = reader.getNextShortUnsigned();
      }

      this.ropeMob.get(this.getLevel());
   }

   @Override
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      Mob mob = this.ropeMob.get(this.getLevel());
      if (mob != null) {
         float distance = this.getDistance(mob);
         int ropeDistance = this.getRopeRange();
         if (distance > ropeDistance) {
            if (!this.isClient() && distance > ropeDistance + 50) {
               this.ropeOverDistanceBuffer += delta;
               if (this.ropeOverDistanceBuffer >= 2000.0F) {
                  FollowerAINode.teleportCloseTo(this, mob, 1);
                  this.sendMovementPacket(true);
                  this.ropeOverDistanceBuffer = 0.0F;
               }
            } else {
               this.ropeOverDistanceBuffer = 0.0F;
            }

            float distanceX = mob.getX() - this.getX();
            float distanceY = mob.getY() - this.getY();
            Float normalize = GameMath.normalize(distanceX, distanceY);
            this.dx = normalize.x * distance / 2.0F;
            this.dy = normalize.y * distance / 2.0F;
            this.setFacingDir(distanceX, distanceY);
         }
      } else if (this.ropeMob.uniqueID != -1) {
         boolean dropRope = !this.removeIfRoperRemoved || !this.roperRemoved;
         this.removeRope(dropRope);
      }

      if (this.ropeTrail != null) {
         this.ropeTrail.update(0.0F, 0.0F);
      }
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      return this.getLevel() != null ? this.getLevel().regionManager.CANNOT_PASS_DOORS_OPTIONS : null;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      Mob mob = this.ropeMob.get(this.getLevel());
      if (mob != null && this.ropeItem != null) {
         ActiveBuff ab = new ActiveBuff(BuffRegistry.HOLD_ITEM_BUFF, mob, 1.0F, null);
         ab.getGndData().setItem("holdItem", new GNDItemInventoryItem(this.ropeItem));
         mob.buffManager.addBuff(ab, false);
      }

      if (mob == null && this.ropeTrail != null) {
         this.ropeTrail.remove();
         this.ropeTrail = null;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Mob mob = this.ropeMob.get(this.getLevel());
      if (mob instanceof RopeClearerMob) {
         if (((RopeClearerMob)mob).shouldClearAfterTime(this.lastClearRopeCheckTime)) {
            ((RopeClearerMob)mob).clearRopeFromMob(this);
            mob = this.ropeMob.get(this.getLevel());
         } else {
            this.lastClearRopeCheckTime = this.getTime();
         }
      }

      if (mob != null && this.ropeItem != null) {
         ActiveBuff ab = new ActiveBuff(BuffRegistry.HOLD_ITEM_BUFF, mob, 1.0F, null);
         ab.getGndData().setItem("holdItem", new GNDItemInventoryItem(this.ropeItem));
         mob.buffManager.addBuff(ab, false);
      }

      if (mob == null && this.ropeItem != null) {
         boolean consumeRope = true;
         if (this.ropeItem.item instanceof RopeItem) {
            consumeRope = ((RopeItem)this.ropeItem.item).consumesRope();
         }

         if (consumeRope) {
            this.getLevel().entityManager.pickups.add(this.ropeItem.copy().getPickupEntity(this.getLevel(), this.getX(), this.getY()));
         }

         this.ropeItem = null;
         this.sendMovementPacket(false);
      }
   }

   @Override
   protected void doWasHitLogic(MobWasHitEvent event) {
      super.doWasHitLogic(event);
      if (this.buyPrice != null && !this.shouldEscape) {
         HumanAngerTargetAINode.addNearbyHumansAnger(this, event.attacker, m -> !m.isSettler(), false);
      }
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      Mob roper = this.ropeMob.get(this.getLevel());
      return roper != null && !roper.isPlayer && !roper.canBeTargeted(attacker, attackerClient) ? false : super.canBeTargeted(attacker, attackerClient);
   }

   @Override
   public void interact(PlayerMob player) {
      super.interact(player);
      if (this.ropeMob.uniqueID != -1 && this.ropeMob.uniqueID == player.getUniqueID()) {
         this.removeRope(true);
      } else if (this.buyPrice != null && this.isServer() && player.isServerClient()) {
         ServerClient client = player.getServerClient();
         PacketOpenContainer container = PacketOpenContainer.Mob(
            ContainerRegistry.BUY_ANIMAL_CONTAINER, this, BuyAnimalContainer.getContainerContent(this.buyPrice)
         );
         ContainerRegistry.openAndSendContainer(client, container);
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return this.ropeMob.uniqueID != -1 && this.ropeMob.uniqueID == mob.getUniqueID() ? true : this.buyPrice != null;
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return this.ropeMob.uniqueID != -1 && this.ropeMob.uniqueID == perspective.getUniqueID()
         ? Localization.translate("controls", "removeropetip")
         : Localization.translate("ui", "buyanimal", "animal", this.getDisplayName());
   }

   @Override
   public InteractingClients getInteractingClients() {
      return this.interactingClients;
   }

   public void setDefaultBuyPrice(GameRandom random) {
      this.buyPrice = new ItemCostList();
      this.buyPrice.addItem("coin", random.getIntBetween(300, 400));
   }

   @Override
   public void onBought(PlayerMob from) {
      InventoryItem ropeItem = this.ropeItem;
      this.removeRope(false);
      if (ropeItem == null) {
         ropeItem = new InventoryItem("rope");
      }

      this.onRope(from.getUniqueID(), ropeItem);
      this.buyPrice = null;
      this.shouldEscape = false;
      this.canDespawn = false;
      this.markDirty();
   }

   @Override
   public boolean shouldSave() {
      if (!super.shouldSave()) {
         return false;
      }

      if (this.shouldEscape) {
         return false;
      }

      if (!this.removeIfRoperRemoved) {
         return true;
      }

      Mob roper = this.ropeMob.get(this.getLevel());
      return roper == null || roper.shouldSave();
   }

   public void removeRope(boolean dropRope) {
      Mob roper = this.getRopeMob();
      if (roper != null && roper.isPlayer) {
         ((PlayerMob)roper).hasReleasedAnimal(this);
      }

      this.ropeMob.uniqueID = -1;
      this.respawnRopeTrail(this.getRopeMob());
      if (this.ropeItem != null && this.isServer() && dropRope) {
         boolean consumeRope = true;
         if (this.ropeItem.item instanceof RopeItem) {
            consumeRope = ((RopeItem)this.ropeItem.item).consumesRope();
         }

         if (consumeRope) {
            this.getLevel().entityManager.pickups.add(this.ropeItem.copy().getPickupEntity(this.getLevel(), this.getX(), this.getY()));
         }
      }

      this.ropeItem = null;
      this.sendMovementPacket(false);
   }

   public boolean canRope(int fromUniqueID, InventoryItem item) {
      return item.item instanceof RopeItem && !this.isRoped() && this.buyPrice == null;
   }

   public InventoryItem onRope(int fromUniqueID, InventoryItem item) {
      boolean consumeItem = true;
      if (item.item instanceof RopeItem) {
         consumeItem = ((RopeItem)item.item).consumesRope();
      }

      this.startRope(fromUniqueID, item.copy(1));
      if (consumeItem) {
         item.setAmount(item.getAmount() - 1);
      }

      return item;
   }

   public void startRope(int roperUniqueID, InventoryItem item) {
      this.ropeMob.uniqueID = roperUniqueID;
      this.ropeItem = item;
      if (item.item instanceof RopeItem) {
         this.ropeRange = ((RopeItem)item.item).getRopeRange(item);
      } else {
         this.ropeRange = GameRandom.globalRandom.getIntBetween(75, 150);
      }

      this.removeIfRoperRemoved = false;
      this.sendMovementPacket(false);
      this.respawnRopeTrail(this.getRopeMob());
      Mob roper = this.getRopeMob();
      if (roper != null && roper.isPlayer) {
         ((PlayerMob)roper).hasRopedAnimal(this);
      }
   }

   public void respawnRopeTrail(Mob mob) {
      if (this.ropeTrail != null) {
         this.ropeTrail.remove();
      }

      this.ropeTrail = null;
      if (mob != null) {
         this.getLevel().entityManager.addTrail(this.ropeTrail = this.getNewRopeTrail(mob, this.ropeItem));
      }
   }

   protected RopeTrail getNewRopeTrail(Mob ropeMob, InventoryItem item) {
      return item != null && item.item instanceof RopeItem
         ? new RopeTrail(this.getLevel(), this, ropeMob, 0.0F, 0.0F, ((RopeItem)item.item).getRopeColor(item))
         : new RopeTrail(this.getLevel(), this, ropeMob, 0.0F, 0.0F);
   }

   public Mob getRopeMob() {
      return this.ropeMob.get(this.getLevel());
   }

   protected boolean isBeingDragged() {
      Mob mob = this.getRopeMob();
      return mob != null && this.getDistance(mob) > this.getRopeRange();
   }

   public boolean isRoped() {
      return this.ropeMob.uniqueID != -1;
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      super.remove(knockbackX, knockbackY, attacker, isDeath);
      if (this.ropeTrail != null) {
         this.ropeTrail.remove();
      }

      this.ropeTrail = null;
      if (this.ropeItem != null && this.isServer() && (!this.removeIfRoperRemoved || !this.roperRemoved)) {
         boolean consumeRope = true;
         if (this.ropeItem.item instanceof RopeItem) {
            consumeRope = ((RopeItem)this.ropeItem.item).consumesRope();
         }

         if (consumeRope) {
            this.getLevel().entityManager.pickups.add(this.ropeItem.copy().getPickupEntity(this.getLevel(), this.getX(), this.getY()));
         }
      }
   }
}
