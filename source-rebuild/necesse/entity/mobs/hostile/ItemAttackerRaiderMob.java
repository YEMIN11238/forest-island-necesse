package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldSettings;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.settlementRaidEvent.SettlementRaidLevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.EquipmentBuffManager;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.RaiderMobPhase;
import necesse.entity.mobs.StaticItemAttackSlot;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.composites.SelectorAINode;
import necesse.entity.mobs.ai.behaviourTree.decorators.ConditionAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.event.AIWasHitEvent;
import necesse.entity.mobs.ai.behaviourTree.event.SetMoveToTileAIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.EscapeAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.MoveToAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.RaiderItemFinderAINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.WandererAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.ItemAttackerPlayerChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.ai.behaviourTree.util.TargetFinderDistance;
import necesse.entity.mobs.ai.behaviourTree.util.WandererBaseOptions;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.friendly.human.MoveToTile;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.SwingSpriteAttackItem;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public class ItemAttackerRaiderMob extends HostileItemAttackerMob {
   public static LootTable lootTable = new LootTable(new LootItemInterface() {
      @Override
      public void addPossibleLoot(LootList list, Object... extra) {
         ItemAttackerRaiderMob self = LootTable.expectExtra(ItemAttackerRaiderMob.class, extra, 0);
         if (self != null && self.carryingLoot != null) {
            list.add(self.carryingLoot.item);
         }
      }

      @Override
      public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
         ItemAttackerRaiderMob self = LootTable.expectExtra(ItemAttackerRaiderMob.class, extra, 0);
         if (self != null && self.carryingLoot != null) {
            list.add(self.carryingLoot);
         }
      }
   }, new LootItemInterface() {
      @Override
      public void addPossibleLoot(LootList list, Object... extra) {
         ItemAttackerRaiderMob self = LootTable.expectExtra(ItemAttackerRaiderMob.class, extra, 0);
         if (self != null) {
            if (self.droppedWeapon != null) {
               if (ItemRegistry.isObtainable(self.droppedWeapon.item.getID()) && self.weaponDropTickets > 0) {
                  list.add(self.droppedWeapon.item);
               }
            } else if (self.weapon != null && ItemRegistry.isObtainable(self.weapon.item.getID()) && self.weaponDropTickets > 0) {
               list.add(self.weapon.item);
            }

            if (self.canEquipArmor) {
               if (self.helmet != null && ItemRegistry.isObtainable(self.helmet.item.getID()) && self.helmetDropTickets > 0) {
                  list.add(self.helmet.item);
               }

               if (self.chest != null && ItemRegistry.isObtainable(self.chest.item.getID()) && self.chestDropTickets > 0) {
                  list.add(self.chest.item);
               }

               if (self.boots != null && ItemRegistry.isObtainable(self.boots.item.getID()) && self.bootsDropTickets > 0) {
                  list.add(self.boots.item);
               }
            }
         }
      }

      @Override
      public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
         ItemAttackerRaiderMob self = LootTable.expectExtra(ItemAttackerRaiderMob.class, extra, 0);
         if (random.getChance(0.25F)) {
            TicketSystemList<InventoryItem> items = new TicketSystemList<>();
            if (self != null) {
               if (self.droppedWeapon != null) {
                  if (ItemRegistry.isObtainable(self.droppedWeapon.item.getID()) && self.weaponDropTickets > 0) {
                     items.addObject(self.weaponDropTickets, self.droppedWeapon.copy());
                  }
               } else if (self.weapon != null && ItemRegistry.isObtainable(self.weapon.item.getID()) && self.weaponDropTickets > 0) {
                  items.addObject(self.weaponDropTickets, self.weapon.copy());
               }

               if (self.canEquipArmor) {
                  if (self.helmet != null && ItemRegistry.isObtainable(self.helmet.item.getID()) && self.helmetDropTickets > 0) {
                     items.addObject(self.helmetDropTickets, self.helmet.copy());
                  }

                  if (self.chest != null && ItemRegistry.isObtainable(self.chest.item.getID()) && self.chestDropTickets > 0) {
                     items.addObject(self.chestDropTickets, self.chest.copy());
                  }

                  if (self.boots != null && ItemRegistry.isObtainable(self.boots.item.getID()) && self.bootsDropTickets > 0) {
                     items.addObject(self.bootsDropTickets, self.boots.copy());
                  }
               }
            }

            InventoryItem item = items.getRandomObject(random);
            if (item != null) {
               list.add(item);
            }
         }
      }
   }, new ChanceLootItem(0.01F, "stormingthehamletpart1vinyl"), new ChanceLootItem(0.01F, "stormingthehamletpart2vinyl"));
   protected final float defaultSpeed = 15.0F;
   protected int healthPerArmorValue = 75;
   protected float attackSpeedMod = -0.5F;
   public final boolean canEquipArmor;
   public int lookSeed = GameRandom.globalRandom.nextInt();
   public Point preparingTile;
   public Point attackTile;
   public int raidingStartTimer;
   public int raidingGroup;
   public int raidEventUniqueID = 0;
   private SettlementRaidLevelEvent raidEvent;
   public RaiderMobPhase phase = RaiderMobPhase.PREPARING;
   public float difficultyModifier = 1.0F;
   public InventoryItem weapon;
   public InventoryItem droppedWeapon;
   public float skillPercent = 0.8F;
   public int weaponDropTickets = 150;
   public InventoryItem helmet;
   public InventoryItem chest;
   public InventoryItem boots;
   public boolean isArmorCosmetic;
   public int helmetDropTickets = 100;
   public int chestDropTickets = 100;
   public int bootsDropTickets = 100;
   public double weaponValue;
   public double armorValue;
   public InventoryItem carryingLoot;
   public int carryingLootTimer;
   public boolean combatTriggered = false;
   protected int noTargetFoundCounter;
   protected final ItemAttackerRaiderMob.CarryingLootMobAbility setCarryingLoot;
   public EquipmentBuffManager equipmentBuffManager = new EquipmentBuffManager(this) {
      @Override
      public InventoryItem getArmorItem(int slot) {
         if (ItemAttackerRaiderMob.this.isArmorCosmetic) {
            return null;
         }

         switch (slot) {
            case 0:
               return ItemAttackerRaiderMob.this.helmet;
            case 1:
               return ItemAttackerRaiderMob.this.chest;
            case 2:
               return ItemAttackerRaiderMob.this.boots;
            default:
               return null;
         }
      }

      @Override
      public InventoryItem getCosmeticItem(int slot) {
         switch (slot) {
            case 0:
               return ItemAttackerRaiderMob.this.helmet;
            case 1:
               return ItemAttackerRaiderMob.this.chest;
            case 2:
               return ItemAttackerRaiderMob.this.boots;
            default:
               return null;
         }
      }

      @Override
      public ArrayList<InventoryItem> getTrinketItems() {
         return new ArrayList<>();
      }
   };

   public ItemAttackerRaiderMob(boolean canEquipArmor) {
      super(100);
      this.canEquipArmor = canEquipArmor;
      this.shouldSave = false;
      this.isUsingItemsForArmorAndHealth = true;
      this.setCarryingLoot = this.registerAbility(new ItemAttackerRaiderMob.CarryingLootMobAbility());
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("lookSeed", this.lookSeed);
      if (this.weapon != null) {
         SaveData weaponSave = new SaveData("weapon");
         this.weapon.addSaveData(weaponSave);
         save.addSaveData(weaponSave);
      }

      if (this.droppedWeapon != null) {
         SaveData weaponSave = new SaveData("droppedWeapon");
         this.droppedWeapon.addSaveData(weaponSave);
         save.addSaveData(weaponSave);
      }

      if (this.canEquipArmor) {
         if (this.helmet != null) {
            SaveData helmetSave = new SaveData("helmet");
            this.helmet.addSaveData(helmetSave);
            save.addSaveData(helmetSave);
         }

         if (this.chest != null) {
            SaveData chestSave = new SaveData("chest");
            this.chest.addSaveData(chestSave);
            save.addSaveData(chestSave);
         }

         if (this.boots != null) {
            SaveData bootsSave = new SaveData("boots");
            this.boots.addSaveData(bootsSave);
            save.addSaveData(bootsSave);
         }

         save.addBoolean("isArmorCosmetic", this.isArmorCosmetic);
      }

      int armor = this.getArmorFlat();
      if (armor != 0) {
         save.addInt("armor", armor);
      }

      save.addDouble("weaponValue", this.weaponValue);
      save.addDouble("armorValue", this.armorValue);
      if (this.carryingLoot != null) {
         SaveData lootSave = new SaveData("carryingLoot");
         this.carryingLoot.addSaveData(lootSave);
         save.addSaveData(lootSave);
         save.addInt("carryingLootTimer", this.carryingLootTimer);
      }

      save.addEnum("phase", this.phase);
      if (this.preparingTile != null) {
         save.addPoint("preparingTile", this.preparingTile);
      }

      if (this.attackTile != null) {
         save.addPoint("attackTile", this.attackTile);
      }

      save.addInt("raidingStartTimer", this.raidingStartTimer);
      save.addInt("raidingGroup", this.raidingGroup);
      save.addFloat("difficultyModifier", this.difficultyModifier);
      save.addInt("raidEventUniqueID", this.raidEventUniqueID);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.lookSeed = save.getInt("lookSeed", this.lookSeed);
      LoadData weaponSave = save.getFirstLoadDataByName("weapon");
      if (weaponSave != null && !weaponSave.isData()) {
         this.weapon = InventoryItem.fromLoadData(weaponSave);
      }

      LoadData droppedWeaponSave = save.getFirstLoadDataByName("droppedWeapon");
      if (droppedWeaponSave != null && !droppedWeaponSave.isData()) {
         this.droppedWeapon = InventoryItem.fromLoadData(droppedWeaponSave);
      }

      if (this.canEquipArmor) {
         LoadData helmetSave = save.getFirstLoadDataByName("helmet");
         if (helmetSave != null) {
            this.helmet = InventoryItem.fromLoadData(helmetSave);
         }

         LoadData chestSave = save.getFirstLoadDataByName("chest");
         if (chestSave != null) {
            this.chest = InventoryItem.fromLoadData(chestSave);
         }

         LoadData bootsSave = save.getFirstLoadDataByName("boots");
         if (bootsSave != null) {
            this.boots = InventoryItem.fromLoadData(bootsSave);
         }

         this.isArmorCosmetic = save.getBoolean("isArmorCosmetic", this.isArmorCosmetic, false);
      }

      int armor = save.getInt("armor", 0, false);
      if (armor != 0) {
         this.setArmor(armor);
      }

      this.weaponValue = save.getDouble("weaponValue", this.weaponValue, false);
      this.armorValue = save.getDouble("armorValue", this.armorValue, false);
      LoadData lootSave = save.getFirstLoadDataByName("carryingLoot");
      if (lootSave != null && !lootSave.isData()) {
         this.carryingLoot = InventoryItem.fromLoadData(lootSave);
         this.carryingLootTimer = save.getInt("carryingLootTimer", 0, false);
      }

      this.phase = save.getEnum(RaiderMobPhase.class, "phase", RaiderMobPhase.ESCAPING, false);
      this.preparingTile = save.getPoint("preparingTile", this.preparingTile, false);
      this.attackTile = save.getPoint("attackTile", this.attackTile, false);
      this.raidingStartTimer = save.getInt("raidingStartTimer", this.raidingStartTimer, false);
      if (this.raidingStartTimer == 0) {
         this.raidingStartTimer = save.getInt("raidingStartTicks", 0, false) * 50;
      }

      this.raidingGroup = save.getInt("raidingGroup", this.raidingGroup);
      this.difficultyModifier = save.getFloat("difficultyModifier", this.difficultyModifier);
      this.raidEventUniqueID = save.getInt("raidEventUniqueID", this.raidEventUniqueID);
      this.updateAIAndLook();
      this.updateArmor();
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.lookSeed = reader.getNextInt();
      this.weapon = InventoryItem.fromContentPacket(reader);
      this.droppedWeapon = InventoryItem.fromContentPacket(reader);
      if (this.canEquipArmor) {
         this.helmet = InventoryItem.fromContentPacket(reader);
         this.chest = InventoryItem.fromContentPacket(reader);
         this.boots = InventoryItem.fromContentPacket(reader);
         this.isArmorCosmetic = reader.getNextBoolean();
      }

      this.setArmor(reader.getNextInt());
      this.carryingLoot = InventoryItem.fromContentPacket(reader);
      if (this.carryingLoot != null) {
         this.carryingLootTimer = reader.getNextInt();
      }

      this.updateAIAndLook();
      this.updateArmor();
      this.phase = reader.getNextEnum(RaiderMobPhase.class);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.lookSeed);
      InventoryItem.addPacketContent(this.weapon, writer);
      InventoryItem.addPacketContent(this.droppedWeapon, writer);
      if (this.canEquipArmor) {
         InventoryItem.addPacketContent(this.helmet, writer);
         InventoryItem.addPacketContent(this.chest, writer);
         InventoryItem.addPacketContent(this.boots, writer);
         writer.putNextBoolean(this.isArmorCosmetic);
      }

      writer.putNextInt(this.getArmorFlat());
      InventoryItem.addPacketContent(this.carryingLoot, writer);
      if (this.carryingLoot != null) {
         writer.putNextInt(this.carryingLootTimer);
      }

      writer.putNextEnum(this.phase);
   }

   @Override
   public void init() {
      super.init();
      if (this.attackTile == null && this.isServer()) {
         NetworkSettlementData settlement = this.getNetworkSettlementData();
         if (settlement != null) {
            this.setAttackTile(new Point(settlement.getTileX(), settlement.getTileY()));
         }
      }

      this.updateAIAndLook();
      this.updateArmor();
   }

   public void updateAIAndLook() {
      this.ai = new BehaviourTreeAI<>(this, new ItemAttackerRaiderMob.ItemAttackerRaiderAI<>(), new AIMover(HumanMob.humanPathIterations));
   }

   public void updateArmor() {
      if (this.canEquipArmor) {
         this.equipmentBuffManager.updateArmorBuffs();
         this.equipmentBuffManager.updateCosmeticSetBonus();
         this.equipmentBuffManager.updateTrinketBuffs();
      }

      int lastHealth = this.getHealth();
      float healthPercent = (float)this.getHealth() / this.getMaxHealth();
      int armor;
      if (this.canEquipArmor && !this.isArmorCosmetic) {
         ActiveBuff equipmentBuff = this.buffManager.getBuff(BuffRegistry.EQUIPMENT_BUFF);
         armor = equipmentBuff == null ? 0 : equipmentBuff.getModifier(BuffModifiers.ARMOR_FLAT);
      } else {
         armor = this.getArmorFlat();
      }

      int maxHealth = Math.max(50, armor * this.healthPerArmorValue);
      this.setMaxHealth(maxHealth);
      this.setHealthHidden(Math.max(lastHealth, (int)(this.getMaxHealth() * healthPercent)));
   }

   @Override
   public ItemAttackSlot getCurrentSelectedAttackSlot() {
      return this.weapon == null ? null : new StaticItemAttackSlot(this.weapon);
   }

   @Override
   public boolean hasValidSummonItem(Item item, CheckSlotType slotType) {
      if (slotType != null) {
         switch (slotType) {
            case HELMET:
               if (this.canEquipArmor && !this.isArmorCosmetic) {
                  return this.helmet != null && this.helmet.item.getID() == item.getID();
               }

               return true;
            case CHEST:
               if (this.canEquipArmor && !this.isArmorCosmetic) {
                  return this.chest != null && this.chest.item.getID() == item.getID();
               }

               return true;
            case FEET:
               if (this.canEquipArmor && !this.isArmorCosmetic) {
                  return this.boots != null && this.boots.item.getID() == item.getID();
               }

               return true;
         }
      }

      return this.weapon != null && this.weapon.item.getID() == item.getID();
   }

   @Override
   public float getWeaponSkillPercent(InventoryItem item) {
      return this.skillPercent;
   }

   public double getMaxWeaponOrArmorValue() {
      return Math.max(this.weaponValue, this.armorValue);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.carryingLoot != null) {
         this.carryingLootTimer += 50;
      }

      if (this.raidEventUniqueID != 0 && !this.combatTriggered && this.isInCombat()) {
         SettlementRaidLevelEvent raidEvent = this.getRaidEvent();
         if (raidEvent != null) {
            this.combatTriggered = true;
            raidEvent.triggerCombatEvent();
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.carryingLoot != null) {
         this.carryingLootTimer += 50;
      }

      if (this.canEquipArmor) {
         this.equipmentBuffManager.clientTickEffects();
      }
   }

   @Override
   public boolean canBePushed(Mob other) {
      return super.canBePushed(other) && this.carryingLootTimer <= 45000;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      return this.getLevel() != null ? this.getLevel().regionManager.CAN_BREAK_OBJECTS_OPTIONS : null;
   }

   @Override
   public boolean shouldSave() {
      return this.phase != RaiderMobPhase.ESCAPING || this.carryingLoot != null;
   }

   @Override
   public boolean isValidSpawnLocation(Server server, ServerClient client, int targetX, int targetY) {
      return new MobSpawnLocation(this, targetX, targetY).checkNotLevelCollides().validAndApply();
   }

   @Override
   public boolean canDespawn() {
      return this.shouldSave() ? false : super.canDespawn();
   }

   public void makeRaider(
      SettlementRaidLevelEvent event, Point preparingTile, Point attackTile, int raidingStartTimer, int raidingGroup, float difficultyModifier
   ) {
      this.phase = RaiderMobPhase.PREPARING;
      this.raidingStartTimer = raidingStartTimer;
      this.raidingGroup = raidingGroup;
      this.preparingTile = preparingTile;
      this.setAttackTile(attackTile);
      this.difficultyModifier = difficultyModifier;
      this.raidEventUniqueID = event == null ? 0 : event.getUniqueID();
      this.raidEvent = event;
   }

   public void setAttackTile(Point tile) {
      this.attackTile = tile;
      this.ai.blackboard.submitEvent("setMoveToTile", new SetMoveToTileAIEvent(this.getMoveToTile(), true));
   }

   protected MoveToTile getMoveToTile() {
      return this.attackTile == null ? null : new MoveToTile(this.attackTile.x, this.attackTile.y, true) {
         @Override
         public boolean moveIfPathFailed(float tileDistance) {
            return true;
         }

         @Override
         public boolean isAtLocation(float tileDistance, boolean foundPath) {
            return tileDistance <= 5.0F;
         }

         @Override
         public void onArrivedAtLocation() {
            ItemAttackerRaiderMob.this.setAttackTile(null);
         }
      };
   }

   public SettlementRaidLevelEvent getRaidEvent() {
      if (this.raidEvent != null) {
         return this.raidEvent;
      }

      if (this.raidEventUniqueID != 0) {
         LevelEvent event = this.getLevel().entityManager.events.get(this.raidEventUniqueID, false);
         if (event instanceof SettlementRaidLevelEvent) {
            this.raidEvent = (SettlementRaidLevelEvent)event;
         }
      }

      return this.raidEvent;
   }

   public ServerSettlementData getServerSettlementData() {
      SettlementRaidLevelEvent raidEvent = this.getRaidEvent();
      return raidEvent != null ? raidEvent.getServerSettlementData() : null;
   }

   public NetworkSettlementData getNetworkSettlementData() {
      SettlementRaidLevelEvent raidEvent = this.getRaidEvent();
      return raidEvent != null ? raidEvent.getNetworkSettlementData() : null;
   }

   public int getRaidingStartTimer() {
      return this.raidingStartTimer;
   }

   public void updateRaidingStartTimer(int raidingStartTimer) {
      this.raidingStartTimer = raidingStartTimer;
   }

   public void setRaiderPhase(RaiderMobPhase phase) {
      this.phase = phase;
   }

   public void setWeapon(InventoryItem weapon) {
      this.weapon = weapon;
   }

   public void setDroppedWeapon(InventoryItem droppedWeapon) {
      this.droppedWeapon = droppedWeapon;
   }

   public void setArmorItems(InventoryItem helmet, InventoryItem chest, InventoryItem boots) {
      if (this.canEquipArmor) {
         this.helmet = helmet;
         this.chest = chest;
         this.boots = boots;
      } else {
         int equipmentArmor = 0;
         if (helmet != null && helmet.item.isArmorItem()) {
            equipmentArmor += ((ArmorItem)helmet.item).getTotalArmorValue(helmet, this);
         }

         if (chest != null && chest.item.isArmorItem()) {
            equipmentArmor += ((ArmorItem)chest.item).getTotalArmorValue(chest, this);
         }

         if (boots != null && boots.item.isArmorItem()) {
            equipmentArmor += ((ArmorItem)boots.item).getTotalArmorValue(boots, this);
         }

         this.setArmor(equipmentArmor);
      }
   }

   public void setArmorBasedOnEquipment(InventoryItem helmet, InventoryItem chest, InventoryItem boots) {
      int equipmentArmor = 0;
      if (helmet != null && helmet.item.isArmorItem()) {
         equipmentArmor += ((ArmorItem)helmet.item).getTotalArmorValue(helmet, this);
      }

      if (chest != null && chest.item.isArmorItem()) {
         equipmentArmor += ((ArmorItem)chest.item).getTotalArmorValue(chest, this);
      }

      if (boots != null && boots.item.isArmorItem()) {
         equipmentArmor += ((ArmorItem)boots.item).getTotalArmorValue(boots, this);
      }

      this.setArmor(equipmentArmor);
   }

   public boolean wouldLikeToStartLooting() {
      return this.noTargetFoundCounter >= 100;
   }

   @Override
   public float getOutgoingDamageModifier() {
      WorldSettings worldSettings = this.getWorldSettings();
      float modifier = worldSettings == null ? 1.0F : worldSettings.difficulty.raiderDamageModifier;
      if (this.weapon != null && this.weapon.item instanceof ToolItem) {
         modifier *= ((ToolItem)this.weapon.item).getRaiderDamageModifier(this.weapon, this);
      }

      return super.getOutgoingDamageModifier() * modifier;
   }

   protected int getMinCoinDrops() {
      return this.getMaxHealth() / 40;
   }

   protected int getMaxCoinDrops() {
      return this.getMaxHealth() / 20;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      ModifierValue<Float> slow = new ModifierValue<>(BuffModifiers.SLOW, 0.0F);
      ModifierValue<Float> knockback = new ModifierValue<>(BuffModifiers.KNOCKBACK_INCOMING_MOD, 0.0F);
      if (this.carryingLoot != null) {
         slow = slow.min(0.25F, Integer.MAX_VALUE);
         if (this.carryingLootTimer > 30000) {
            knockback = knockback.min(1.0F, Integer.MAX_VALUE);
         }
      }

      return Stream.concat(super.getDefaultModifiers(), Stream.of(slow, knockback, new ModifierValue<>(BuffModifiers.ATTACK_SPEED, this.attackSpeedMod)));
   }

   @Override
   public GameTooltips getMapTooltips() {
      StringTooltips tooltips = new StringTooltips(this.getDisplayName());
      if (this.carryingLoot != null) {
         tooltips.add(Localization.translate("misc", "raiderescaping", "item", this.carryingLoot.getAmount() + "x " + this.carryingLoot.getItemDisplayName()));
      }

      return tooltips;
   }

   @Override
   protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
      super.addHoverTooltips(tooltips, debug);
      if (this.carryingLoot != null) {
         tooltips.add(Localization.translate("misc", "raiderescaping", "item", this.carryingLoot.getAmount() + "x " + this.carryingLoot.getItemDisplayName()));
      }
   }

   @Override
   protected void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      if (this.weapon != null) {
         tooltips.add("Weapon: " + this.weapon);
      }

      if (this.helmet != null) {
         tooltips.add("Helmet: " + this.helmet);
      }

      if (this.chest != null) {
         tooltips.add("Chest: " + this.chest);
      }

      if (this.boots != null) {
         tooltips.add("Boots: " + this.boots);
      }

      if (this.weaponValue != 0.0) {
         tooltips.add("Weapon value: " + this.weaponValue);
      }

      if (this.armorValue != 0.0) {
         tooltips.add("Armor value: " + this.armorValue);
      }
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (attacker != null && attacker.isTrapAttacker()) {
         GameUtils.streamServerClients(this.getLevel()).filter(ServerClient::achievementsLoaded).forEach(c -> c.achievements().HOME_ALONE.markCompleted(c));
      }
   }

   public class CarryingLootMobAbility extends MobAbility {
      public void runAndSend(InventoryItem invItem) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         InventoryItem.addPacketContent(invItem, writer);
         if (invItem == null) {
            ItemAttackerRaiderMob.this.carryingLootTimer = 0;
         }

         writer.putNextInt(ItemAttackerRaiderMob.this.carryingLootTimer);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         ItemAttackerRaiderMob.this.carryingLoot = InventoryItem.fromContentPacket(reader);
         ItemAttackerRaiderMob.this.carryingLootTimer = reader.getNextInt();
      }
   }

   public static class ItemAttackerRaiderAI<T extends ItemAttackerRaiderMob> extends SelectorAINode<T> {
      public final EscapeAINode<T> escapeAINode;
      public final ItemAttackerPlayerChaserAI<T> playerChaserAI;
      public final WandererAINode<T> wandererAINode;

      public ItemAttackerRaiderAI() {
         this.addChild(new ConditionAINode<>(new RaiderItemFinderAINode<T>() {
            public float getMaxPickupValue(T mob) {
               return mob.getMaxHealth() / 5.0F;
            }

            public void onPickedUpItem(T mob, int tileX, int tileY, InventoryItem inventoryItem) {
               InventoryItem showItem = SwingSpriteAttackItem.setup(new InventoryItem("swingspriteattack"), inventoryItem.item, true);
               mob.showAttackAndSendAttacker(showItem, tileX * 32 + 16, tileY * 32 + 16, 0, Item.getRandomAttackSeed(GameRandom.globalRandom));
               mob.setCarryingLoot.runAndSend(inventoryItem);
               mob.phase = RaiderMobPhase.ESCAPING;
            }

            public void onFoundItem(T mob, int tileX, int tileY, int slot, InventoryItem inventoryItem) {
               super.onFoundItem(mob, tileX, tileY, slot, inventoryItem);
               SettlementRaidLevelEvent raidEvent = mob.getRaidEvent();
               if (raidEvent != null) {
                  raidEvent.reservedLoot.add(new Point(tileX, tileY), slot);
               }
            }

            public boolean isValidItem(T mob, int tileX, int tileY, int slot, InventoryItem inventoryItem) {
               if (!super.isValidItem(mob, tileX, tileY, slot, inventoryItem)) {
                  return false;
               }

               SettlementRaidLevelEvent raidEvent = mob.getRaidEvent();
               return raidEvent != null ? !raidEvent.reservedLoot.contains(new Point(tileX, tileY), slot) : true;
            }
         }, mob -> mob.phase == RaiderMobPhase.LOOTING, AINodeResult.FAILURE));
         this.addChild(
            this.escapeAINode = new EscapeAINode<T>() {
               public void onEscaped(T mob) {
                  super.onEscaped(mob);
                  if (mob.carryingLoot != null) {
                     NetworkSettlementData settlement = mob.getNetworkSettlementData();
                     if (settlement != null) {
                        GameMessage settlementName = settlement.getSettlementName();
                        GameMessage itemString = new GameMessageBuilder()
                           .append(mob.carryingLoot.getAmount() + "x " + TypeParsers.getItemParseString(mob.carryingLoot) + " ")
                           .append(mob.carryingLoot.getItemLocalization());
                        LocalMessage message = new LocalMessage("misc", "raiderescapedwith")
                           .addReplacement("raider", mob.getLocalization())
                           .addReplacement("settlement", settlementName)
                           .addReplacement("item", itemString);
                        settlement.streamTeamMembers().forEach(c -> c.sendChatMessage(message));
                     }
                  }
               }

               public boolean shouldEscape(T mob, Blackboard<T> blackboard) {
                  if (mob.isHostile && !mob.isSummoned && mob.shouldLevelRetreat()) {
                     return true;
                  } else {
                     return mob.carryingLoot != null ? true : mob.phase == RaiderMobPhase.LOOTING || mob.phase == RaiderMobPhase.ESCAPING || mob.phase == null;
                  }
               }
            }
         );
         this.addChild(
            new AINode<T>() {
               protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
               }

               public void init(T mob, Blackboard<T> blackboard) {
               }

               public AINodeResult tick(T mob, Blackboard<T> blackboard) {
                  if (mob.phase == RaiderMobPhase.PREPARING) {
                     if (mob.raidingStartTimer > 0) {
                        mob.raidingStartTimer -= 50;
                     }

                     if (mob.raidingStartTimer <= 0) {
                        mob.phase = RaiderMobPhase.RAIDING;
                        SettlementRaidLevelEvent raidEvent = mob.getRaidEvent();
                        if (raidEvent != null) {
                           raidEvent.startRaid(true);
                        }
                     }

                     for (AIWasHitEvent lastHit : blackboard.getLastHits()) {
                        mob.getLevel()
                           .entityManager
                           .mobs
                           .getInRegionByTileRange(mob.getTileX(), mob.getTileY(), 25)
                           .stream()
                           .filter(m -> m instanceof ItemAttackerRaiderMob)
                           .forEach(m -> m.ai.blackboard.submitEvent("startRaid" + mob.raidingGroup, new AIEvent()));
                        mob.phase = RaiderMobPhase.RAIDING;
                        SettlementRaidLevelEvent raidEvent = mob.getRaidEvent();
                        if (raidEvent != null) {
                           raidEvent.startRaid(true);
                        }
                     }

                     for (AIEvent event : blackboard.getLastCustomEvents("startRaid" + mob.raidingGroup)) {
                        mob.phase = RaiderMobPhase.RAIDING;
                     }
                  }

                  return AINodeResult.FAILURE;
               }
            }
         );
         this.addChild(this.playerChaserAI = new ItemAttackerPlayerChaserAI<T>(0, new InventoryItem("woodsword")) {
            public void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
               super.onRootSet(root, mob, blackboard);
               blackboard.onEvent("itemAttackerUpdated", e -> {
                  ChaserAINode<T> chaser = this.itemAttackerChaserAINode.getChaserAIIfExists();
                  if (chaser != null) {
                     chaser.moveIfFailedPath = (target, path) -> mob.getDistance(target) > 320.0F;
                  }
               });
            }
         });
         this.playerChaserAI.targetFinderAINode.distance = new TargetFinderDistance<T>(0) {
            protected int getSearchDistanceFlat(T mob, Mob target) {
               if (mob.phase == RaiderMobPhase.LOOTING) {
                  return 384;
               } else {
                  return mob.phase == RaiderMobPhase.RAIDING ? 768 : 512;
               }
            }
         };
         this.playerChaserAI.targetFinderAINode.loseTargetMinCooldown = 1000;
         this.playerChaserAI.targetFinderAINode.loseTargetMaxCooldown = 2000;
         this.addChild(new AINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
            }

            public void init(T mob, Blackboard<T> blackboard) {
            }

            public AINodeResult tick(T mob, Blackboard<T> blackboard) {
               if (mob.phase == RaiderMobPhase.RAIDING && mob.attackTile == null) {
                  Mob currentTarget = blackboard.getObject(Mob.class, "currentTarget");
                  if (currentTarget == null) {
                     mob.noTargetFoundCounter++;
                  }
               }

               return AINodeResult.FAILURE;
            }
         });
         this.addChild(new ConditionAINode<>(new MoveToAINode<T>() {
            protected void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
               super.onRootSet(root, mob, blackboard);
               this.moveToTile = mob.getMoveToTile();
            }

            public AINodeResult tickNode(T mob, Blackboard<T> blackboard) {
               return super.tickNode(mob, blackboard);
            }
         }, mob -> mob.phase == RaiderMobPhase.RAIDING, AINodeResult.FAILURE));
         this.addChild(this.wandererAINode = new WandererAINode<T>(40000) {
            @Override
            public WandererBaseOptions<T> getBaseOptions() {
               return new WandererBaseOptions<T>() {
                  public Point getBaseTile(T mob) {
                     return mob.phase == RaiderMobPhase.PREPARING ? mob.preparingTile : null;
                  }
               };
            }
         });
         this.wandererAINode.searchRadius = 5;
      }
   }
}
