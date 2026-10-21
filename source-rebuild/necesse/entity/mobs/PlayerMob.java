package necesse.entity.mobs;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import necesse.engine.AreaFinder;
import necesse.engine.GameEvents;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.events.players.ObjectInteractEvent;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.journal.listeners.FoodConsumedJournalChallengeListener;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.client.ClientClient;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketActiveMountAbility;
import necesse.engine.network.packet.PacketActiveMountAbilityStopped;
import necesse.engine.network.packet.PacketActiveSetBuffAbility;
import necesse.engine.network.packet.PacketActiveSetBuffAbilityStopped;
import necesse.engine.network.packet.PacketActiveTrinketBuffAbility;
import necesse.engine.network.packet.PacketActiveTrinketBuffAbilityStopped;
import necesse.engine.network.packet.PacketChatMessage;
import necesse.engine.network.packet.PacketClientRelationshipData;
import necesse.engine.network.packet.PacketContainerAction;
import necesse.engine.network.packet.PacketCreativePlayerSettings;
import necesse.engine.network.packet.PacketLevelEvent;
import necesse.engine.network.packet.PacketObjectInteract;
import necesse.engine.network.packet.PacketPlayerAction;
import necesse.engine.network.packet.PacketPlayerAppearance;
import necesse.engine.network.packet.PacketPlayerAttack;
import necesse.engine.network.packet.PacketPlayerAttackHandler;
import necesse.engine.network.packet.PacketPlayerDropItem;
import necesse.engine.network.packet.PacketPlayerHunger;
import necesse.engine.network.packet.PacketPlayerInventory;
import necesse.engine.network.packet.PacketPlayerItemInteract;
import necesse.engine.network.packet.PacketPlayerItemMobInteract;
import necesse.engine.network.packet.PacketPlayerKissedPacket;
import necesse.engine.network.packet.PacketPlayerMobInteract;
import necesse.engine.network.packet.PacketPlayerMovement;
import necesse.engine.network.packet.PacketPlayerStopAttack;
import necesse.engine.network.packet.PacketPlayerUseMount;
import necesse.engine.network.packet.PacketRequestActiveMountAbility;
import necesse.engine.network.packet.PacketRequestActiveSetBuffAbility;
import necesse.engine.network.packet.PacketRequestActiveTrinketBuffAbility;
import necesse.engine.network.packet.PacketShowAttack;
import necesse.engine.network.packet.PacketShowItemLevelInteract;
import necesse.engine.network.packet.PacketShowItemMobInteract;
import necesse.engine.network.packet.PacketSpawnCreativeItem;
import necesse.engine.network.packet.PacketSpawnProjectile;
import necesse.engine.network.packet.PacketSwapInventorySlots;
import necesse.engine.network.server.AdventureParty;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.JournalChallengeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.screenHudManager.UniqueScreenFloatText;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.ObjectValue;
import necesse.engine.window.WindowManager;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldSettings;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.explosionEvent.TNTExplosionEvent;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.ActiveBuffAbility;
import necesse.entity.mobs.buffs.ActiveBuffAbilityContainer;
import necesse.entity.mobs.buffs.BuffAbility;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.SetBonusBuff;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.itemAttacker.AmmoConsumed;
import necesse.entity.mobs.itemAttacker.AmmoUserMob;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.GameSkin;
import necesse.gfx.HumanLook;
import necesse.gfx.PlayerSprite;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.StringDrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.InputTooltip;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.ControllerInteractTarget;
import necesse.gfx.ui.HUD;
import necesse.gfx.ui.debug.Debug;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventory;
import necesse.inventory.PlayerInventoryManager;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.SlotPriority;
import necesse.inventory.container.Container;
import necesse.inventory.container.ContainerAction;
import necesse.inventory.container.ContainerActionResult;
import necesse.inventory.container.slots.ContainerSlot;
import necesse.inventory.item.FishingPoleHolding;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemControllerInteract;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.ItemUsed;
import necesse.inventory.item.TorchItem;
import necesse.inventory.item.baitItem.BaitItem;
import necesse.inventory.item.mountItem.MountItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.inventory.item.toolItem.ToolDamageItem;
import necesse.level.gameObject.ChairObjectInterface;
import necesse.level.gameObject.GameObject;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;
import necesse.level.maps.hudManager.floatText.DamageText;
import necesse.level.maps.hudManager.floatText.UniqueFloatText;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.RegionPositionGetter;

public class PlayerMob extends ItemAttackerMob implements FishingMob, HungerMob, AmmoUserMob, ObjectUserMob {
   public static int distanceToRunAtFullHunger = 57600;
   public static int secondsToPassAtFullHunger = 10800;
   public static float attacksHungerModifier = 1.0F;
   public static int spawnInvincibilityFrameMilliseconds = 2000;
   public HumanLook look;
   public boolean constantAttack;
   public boolean constantInteract;
   public PlayerInventorySlot attackSlot;
   public final HashMap<Integer, Long> toolHits = new HashMap<>();
   private boolean firstPress;
   private PlayerInventoryManager inv;
   private boolean inventoryExtended;
   private boolean creativeMenuExtended = true;
   private int selectedSlot;
   private boolean inventoryActionUpdate;
   private boolean maxManaReached;
   private long timeOfMaxMana;
   public HealthUpgradeManager healthUpgradeManager = new HealthUpgradeManager();
   public boolean hasInvincibility;
   protected boolean hasGodModeInCreative = true;
   public Point currentAttackLastPlacePosition;
   protected int lastMaxFoodBuffsModifier;
   public EquipmentBuffManager equipmentBuffManager = new EquipmentBuffManager(this) {
      @Override
      public InventoryItem getArmorItem(int slot) {
         return PlayerMob.this.getInv().equipment.getSelectedArmorSlot(slot).getItem();
      }

      @Override
      public InventoryItem getCosmeticItem(int slot) {
         return PlayerMob.this.getInv().equipment.getSelectedCosmeticSlot(slot).getItem();
      }

      @Override
      public ArrayList<InventoryItem> getTrinketItems() {
         PlayerInventoryManager inv = PlayerMob.this.getInv();
         ArrayList<InventoryItem> trinketItems = new ArrayList<>(inv.equipment.getTrinketSlotsSize() + 1);
         trinketItems.add(inv.equipment.getSelectedEquipmentSlot(1).getItem());

         for (int i = 0; i < inv.equipment.getTrinketSlotsSize(); i++) {
            trinketItems.add(inv.equipment.getSelectedTrinketsSlot(i).getItem());
         }

         return trinketItems;
      }
   };
   private ActiveBuffAbilityContainer activeSetBuffAbility;
   private ActiveBuffAbilityContainer activeTrinketBuffAbility;
   private ActiveMountAbilityContainer activeMountAbility;
   private final NetworkClient networkClient;
   private byte movementTeleportBuffer = 122;
   public String playerName;
   public float hungerLevel = 1.3F;
   protected double lastDistanceRan;
   protected double deltaCounter;
   private double waterSoundTimer;
   public boolean autoOpenDoors = true;
   public boolean hotbarLocked = false;
   private boolean smartMiningTogglePressed = false;
   private float lastControllerAimX;
   private float lastControllerAimY;
   private int refreshRemainingSpawnTime;
   private long spawnInvincibilityEndTime;
   protected ObjectUserActive objectUser;
   protected FormExpressionWheel.Expression activeExpression;
   protected long activeExpressionEndTime;
   private final BehaviourTreeAI<?> defaultAI;
   private static final int angleSnap = 5;
   public static Attacker STARVING_ATTACKER = new Attacker() {
      @Override
      public GameMessage getAttackerName() {
         return new LocalMessage("deaths", "starvationname");
      }

      @Override
      public DeathMessageTable getDeathMessages() {
         return this.getDeathMessages("starvation", 2);
      }

      @Override
      public Mob getFirstAttackOwner() {
         return null;
      }
   };

   public PlayerMob(long tempNameIdentifier, NetworkClient networkClient) {
      super(100);
      this.playerName = "player" + tempNameIdentifier;
      this.networkClient = networkClient;
      if (networkClient != null) {
         this.setTeam(networkClient.getTeamID());
      }

      this.look = new HumanLook();
      this.resetInv();
      this.setDir(2);
      this.setSpeed(40.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
      this.hitCooldown = 500;
      this.attackTime = 0L;
      this.hitTime = 0L;
      this.defaultAI = this.ai;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addBoolean("inventoryExtended", this.inventoryExtended);
      save.addBoolean("creativeMenuExtended", this.creativeMenuExtended);
      save.addBoolean("autoOpenDoors", this.autoOpenDoors);
      save.addBoolean("hotbarLocked", this.hotbarLocked);
      save.addFloat("hungerLevel", this.hungerLevel);
      if (this.hasInvincibility) {
         save.addBoolean("hasInvincibility", this.hasInvincibility);
      }

      save.addBoolean("hasGodModeInCreative", this.hasGodModeInCreative);
      this.healthUpgradeManager.addSaveData(save, "HEALTH_UPGRADES");
      SaveData lookData = new SaveData("LOOK");
      this.look.addSaveData(lookData);
      save.addSaveData(lookData);
      SaveData invData = new SaveData("INVENTORY");
      this.getInv().addSaveData(invData);
      save.addSaveData(invData);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.inventoryExtended = save.getBoolean("inventoryExtended", this.inventoryExtended, false);
      this.creativeMenuExtended = save.getBoolean("creativeMenuExtended", this.creativeMenuExtended, false);
      this.autoOpenDoors = save.getBoolean("autoOpenDoors", this.autoOpenDoors, false);
      this.hotbarLocked = save.getBoolean("hotbarLocked", this.hotbarLocked, false);
      this.hungerLevel = save.getFloat("hungerLevel", 1.0F, 0.0F, Float.MAX_VALUE, false);
      this.hasInvincibility = save.getBoolean("hasInvincibility", this.hasInvincibility, false);
      this.hasGodModeInCreative = save.getBoolean("hasGodModeInCreative", this.hasGodModeInCreative, false);
      this.healthUpgradeManager.readLoadData(save, "HEALTH_UPGRADES", this);
      LoadData lookData = save.getFirstLoadDataByName("LOOK");
      if (lookData != null) {
         this.look.applyLoadData(lookData);
      } else {
         GameLog.warn.println("Could not load player look data");
      }

      LoadData inventory = save.getFirstLoadDataByName("INVENTORY");
      if (inventory != null) {
         this.getInv().applyLoadData(inventory);
      } else {
         GameLog.warn.println("Could not load player inventory data");
      }

      this.getInv().clean();
      this.openedDoors.clear();
      this.handleLoadedValues();
   }

   public void addLoadedCharacterSaveData(SaveData save) {
      save.addInt("maxHealth", this.getMaxHealthFlat());
      save.addInt("health", this.getHealth());
      save.addInt("maxResilience", this.getMaxResilienceFlat());
      save.addFloat("resilience", this.getResilience());
      save.addInt("maxMana", this.getMaxManaFlat());
      save.addFloat("mana", this.getMana());
      save.addBoolean("inventoryExtended", this.inventoryExtended);
      save.addBoolean("creativeMenuExtended", this.creativeMenuExtended);
      save.addBoolean("autoOpenDoors", this.autoOpenDoors);
      save.addBoolean("hotbarLocked", this.hotbarLocked);
      save.addFloat("hungerLevel", this.hungerLevel);
      save.addBoolean("hasGodModeInCreative", this.hasGodModeInCreative);
      this.healthUpgradeManager.addSaveData(save, "HEALTH_UPGRADES");
      SaveData buffs = new SaveData("BUFFS");
      this.buffManager.addSaveData(buffs);
      save.addSaveData(buffs);
      SaveData lookData = new SaveData("LOOK");
      this.look.addSaveData(lookData);
      save.addSaveData(lookData);
      SaveData invData = new SaveData("INVENTORY");
      this.getInv().addSaveData(invData);
      save.addSaveData(invData);
   }

   public void applyLoadedCharacterLoadData(LoadData save) {
      this.setMaxHealth(save.getInt("maxHealth", this.getMaxHealthFlat()));
      this.loadedHealth = save.getInt("health", this.getMaxHealthFlat(), false);
      this.setHealthHidden(this.loadedHealth);
      this.setMaxResilience(save.getInt("maxResilience", this.getMaxResilienceFlat()));
      this.loadedResilience = save.getFloat("resilience", 0.0F, false);
      this.setResilienceHidden(this.loadedResilience);
      this.setMaxMana(save.getInt("maxMana", this.getMaxManaFlat()));
      this.loadedMana = save.getFloat("mana", this.getMaxManaFlat(), false);
      this.setManaHidden(this.loadedMana);
      this.inventoryExtended = save.getBoolean("inventoryExtended", this.inventoryExtended, false);
      this.creativeMenuExtended = save.getBoolean("creativeMenuExtended", this.creativeMenuExtended, false);
      this.autoOpenDoors = save.getBoolean("autoOpenDoors", this.autoOpenDoors, false);
      this.hotbarLocked = save.getBoolean("hotbarLocked", this.hotbarLocked, false);
      this.hungerLevel = save.getFloat("hungerLevel", 1.0F, 0.0F, Float.MAX_VALUE, false);
      this.hasGodModeInCreative = save.getBoolean("hasGodModeInCreative", this.hasGodModeInCreative, false);
      this.healthUpgradeManager.readLoadData(save, "HEALTH_UPGRADES", this);
      LoadData buffs = save.getFirstLoadDataByName("BUFFS");
      if (buffs != null) {
         this.buffManager.applyLoadData(buffs);
      }

      LoadData lookData = save.getFirstLoadDataByName("LOOK");
      if (lookData != null) {
         this.look.applyLoadData(lookData);
      } else {
         GameLog.warn.println("Could not load player look data");
      }

      LoadData inventory = save.getFirstLoadDataByName("INVENTORY");
      if (inventory != null) {
         this.getInv().applyLoadData(inventory);
      } else {
         GameLog.warn.println("Could not load player inventory data");
      }

      this.getInv().clean();
      this.equipmentBuffManager.updateAll();
   }

   public void setupLoadedCharacterPacket(PacketWriter writer) {
      writer.putNextInt(this.getMaxHealthFlat());
      writer.putNextInt(Math.max(this.getHealth(), this.loadedHealth));
      writer.putNextInt(this.getMaxResilienceFlat());
      writer.putNextFloat(Math.max(this.getResilience(), this.loadedResilience));
      writer.putNextInt(this.getMaxManaFlat());
      writer.putNextFloat(Math.max(this.getMana(), this.loadedMana));
      this.buffManager.setupContentPacket(writer);
      writer.putNextByteUnsigned(this.selectedSlot);
      writer.putNextBoolean(this.inventoryExtended);
      writer.putNextBoolean(this.autoOpenDoors);
      writer.putNextBoolean(this.hotbarLocked);
      this.look.setupContentPacket(writer, true);
      this.getInv().setupContentPacket(writer);
      writer.putNextFloat(this.hungerLevel);
      writer.putNextBoolean(this.hasGodModeInCreative);
      this.healthUpgradeManager.writePacket(writer);
   }

   public void applyLoadedCharacterPacket(PacketReader reader) {
      this.setMaxHealth(reader.getNextInt());
      this.loadedHealth = reader.getNextInt();
      this.setHealthHidden(this.loadedHealth);
      this.setMaxResilience(reader.getNextInt());
      this.loadedResilience = reader.getNextFloat();
      this.setResilienceHidden(this.loadedResilience);
      this.setMaxMana(reader.getNextInt());
      this.loadedMana = reader.getNextFloat();
      this.setManaHidden(this.loadedMana);
      this.buffManager.applyContentPacket(reader);
      this.selectedSlot = reader.getNextByteUnsigned();
      this.inventoryExtended = reader.getNextBoolean();
      this.autoOpenDoors = reader.getNextBoolean();
      this.hotbarLocked = reader.getNextBoolean();
      this.look.applyContentPacket(reader);
      this.getInv().applyContentPacket(reader);
      this.hungerLevel = reader.getNextFloat();
      this.hasGodModeInCreative = reader.getNextBoolean();
      this.healthUpgradeManager.readPacket(reader);
      this.equipmentBuffManager.updateAll();
      this.buffManager.forceUpdateBuffs();
      this.handleLoadedValues();
   }

   public byte getMovementTeleportBuffer() {
      return this.movementTeleportBuffer;
   }

   public void nextMovementTeleportBuffer() {
      this.movementTeleportBuffer++;
   }

   public double allowServerMovement(Server server, ServerClient client, float x, float y) {
      return this.allowServerMovement(server, client, x, y, this.dx, this.dy);
   }

   public double allowServerMovement(Server server, ServerClient client, float x, float y, float dx, float dy) {
      if (this.getWorldSettings().creativeMode) {
         return -10000.0;
      }

      if (client.getPermissionLevel().getLevel() >= PermissionLevel.ADMIN.getLevel() && server.world.settings.cheatsAllowedOrHidden()) {
         return -100000.0;
      }

      if (!Settings.strictServerAuthority) {
         return -10000.0;
      }

      java.awt.geom.Point2D.Float lastPos = new java.awt.geom.Point2D.Float(this.x, this.y);
      java.awt.geom.Point2D.Float lastDelta = new java.awt.geom.Point2D.Float(this.dx, this.dy);
      double combinedTolerance = lastPos.distance(x, y) + lastDelta.distance(dx, dy);
      return combinedTolerance - Server.clientMoveTolerance;
   }

   public void setupPlayerMovementPacket(PacketWriter writer) {
      this.writeObjectUserPacket(writer);
      writer.putNextMaxValue(this.getDir(), 3);
      writer.putNextByteUnsigned(this.selectedSlot);
      this.getInv().equipment.writeSelectedSet(writer);
      writer.putNextFloat(this.moveX);
      writer.putNextFloat(this.moveY);
      writer.putNextBoolean(this.autoOpenDoors);
      writer.putNextBoolean(this.hotbarLocked);
      writer.putNextBoolean(this.activeSetBuffAbility != null);
      if (this.activeSetBuffAbility != null) {
         writer.putNextInt(this.activeSetBuffAbility.uniqueID);
      }

      writer.putNextBoolean(this.activeTrinketBuffAbility != null);
      if (this.activeTrinketBuffAbility != null) {
         writer.putNextInt(this.activeTrinketBuffAbility.uniqueID);
      }

      writer.putNextBoolean(this.activeMountAbility != null);
      if (this.activeMountAbility != null) {
         writer.putNextInt(this.activeMountAbility.uniqueID);
      }
   }

   public void applyPlayerMovementPacket(PacketPlayerMovement packet, PacketReader reader) {
      boolean usageChanges = this.readObjectUserPacket(reader);
      if (packet != null) {
         this.movementTeleportBuffer = packet.teleportBuffer;
         if (!usageChanges) {
            this.setPos(packet.x, packet.y, packet.isDirect);
            this.dx = packet.dx;
            this.dy = packet.dy;
         }
      }

      this.refreshClientUpdateTime();
      int nextDir = reader.getNextMaxValue(3);
      if (this.isAttacking) {
         this.beforeAttackDir = nextDir;
      } else {
         this.setDir(nextDir);
      }

      this.selectedSlot = reader.getNextByteUnsigned();
      this.getInv().equipment.readSelectedSet(reader);
      this.moveX = reader.getNextFloat();
      this.moveY = reader.getNextFloat();
      if (this.isRiding()) {
         Mob mount = this.getMount();
         if (mount != null) {
            if (!this.isAttacking) {
               mount.setDir(nextDir);
            }

            mount.setPos(this.x, this.y, true);
            mount.moveX = this.moveX;
            mount.moveY = this.moveY;
         }
      }

      this.autoOpenDoors = reader.getNextBoolean();
      this.hotbarLocked = reader.getNextBoolean();
      if (reader.getNextBoolean()) {
         int abilityUniqueID = reader.getNextInt();
         if ((this.activeSetBuffAbility == null || this.activeSetBuffAbility.uniqueID != abilityUniqueID) && this.isClientClient()) {
            ClientClient clientClient = this.getClientClient();
            clientClient.getClient().network.sendPacket(new PacketRequestActiveSetBuffAbility(clientClient.slot));
         }
      } else {
         if (this.activeSetBuffAbility != null) {
            this.activeSetBuffAbility.onStopped(this);
         }

         this.activeSetBuffAbility = null;
      }

      if (reader.getNextBoolean()) {
         int abilityUniqueID = reader.getNextInt();
         if ((this.activeTrinketBuffAbility == null || this.activeTrinketBuffAbility.uniqueID != abilityUniqueID) && this.isClientClient()) {
            ClientClient clientClient = this.getClientClient();
            clientClient.getClient().network.sendPacket(new PacketRequestActiveTrinketBuffAbility(clientClient.slot));
         }
      } else {
         if (this.activeTrinketBuffAbility != null) {
            this.activeTrinketBuffAbility.onStopped(this);
         }

         this.activeTrinketBuffAbility = null;
      }

      if (reader.getNextBoolean()) {
         int abilityUniqueID = reader.getNextInt();
         if ((this.activeMountAbility == null || this.activeMountAbility.uniqueID != abilityUniqueID) && this.isClientClient()) {
            ClientClient clientClient = this.getClientClient();
            clientClient.getClient().network.sendPacket(new PacketRequestActiveMountAbility(clientClient.slot));
         }
      } else {
         if (this.activeMountAbility != null) {
            this.activeMountAbility.onStopped(this);
         }

         this.activeMountAbility = null;
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.movementTeleportBuffer = reader.getNextByte();
      this.inventoryExtended = reader.getNextBoolean();
      this.creativeMenuExtended = reader.getNextBoolean();
      this.selectedSlot = reader.getNextByteUnsigned();
      this.look.applyContentPacket(reader);
      this.getInv().applyContentPacket(reader);
      this.hungerLevel = reader.getNextFloat();
      this.hasGodModeInCreative = reader.getNextBoolean();
      this.healthUpgradeManager.readPacket(reader);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextByte(this.movementTeleportBuffer);
      writer.putNextBoolean(this.inventoryExtended);
      writer.putNextBoolean(this.creativeMenuExtended);
      writer.putNextByteUnsigned(this.selectedSlot);
      this.look.setupContentPacket(writer, true);
      this.getInv().setupContentPacket(writer);
      writer.putNextFloat(this.hungerLevel);
      writer.putNextBoolean(this.hasGodModeInCreative);
      this.healthUpgradeManager.writePacket(writer);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      this.setupPlayerMovementPacket(writer);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.applyPlayerMovementPacket(null, reader);
   }

   @Override
   public void setupHealthPacket(PacketWriter writer, boolean isFull) {
      super.setupHealthPacket(writer, isFull);
      if (this.isServerClient()) {
         writer.putNextBoolean(this.getServerClient().isDead());
      } else if (this.isClientClient()) {
         writer.putNextBoolean(this.getClientClient().isDead());
      } else {
         writer.putNextBoolean(this.removed());
      }
   }

   @Override
   public void applyHealthPacket(PacketReader reader, boolean isFull) {
      super.applyHealthPacket(reader, isFull);
      boolean isDead = reader.getNextBoolean();
      if (this.isClientClient()) {
         ClientClient client = this.getClientClient();
         if (isDead != client.isDead()) {
            if (isDead) {
               client.die(5000);
            } else {
               this.restore();
            }
         }
      } else if (this.isServerClient()) {
         ServerClient client = this.getServerClient();
         if (isDead != client.isDead()) {
            if (isDead) {
               client.die(5000);
            } else {
               this.restore();
            }
         }
      } else if (isDead != this.removed()) {
         if (isDead) {
            this.remove();
         } else {
            this.restore();
         }
      }
   }

   public void applyInventoryPacket(PacketPlayerInventory packet) {
      this.refreshClientUpdateTime();
      if (this.getInv() != null) {
         this.getInv().applyContentPacket(new PacketReader(packet.inventoryContent));
      }
   }

   public void applyAppearancePacket(PacketPlayerAppearance packet) {
      this.refreshClientUpdateTime();
      this.look = new HumanLook(packet.look);
      this.inv.giveLookArmor(false);
      this.playerName = packet.name;
   }

   public void setupCreativeSettingsPacket(PacketWriter writer) {
      writer.putNextBoolean(this.hasGodModeInCreative);
   }

   public void applyCreativeSettingsPacket(PacketReader packetReader) {
      this.refreshClientUpdateTime();
      this.hasGodModeInCreative = packetReader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
   }

   @Override
   public void onLevelChanged() {
      super.onLevelChanged();
      if (this.objectUser != null) {
         this.objectUser.stopUsing(false, 0.0F, 0.0F);
      }
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      CollisionFilter collisionFilter = this.hasGodMode() ? new CollisionFilter() : super.getLevelCollisionFilter();
      return collisionFilter.addEmptyTiles();
   }

   @Override
   public void tickMovement(float delta) {
      if (this.refreshRemainingSpawnTime > 0) {
         this.spawnInvincibilityEndTime = this.getWorldEntity().getLocalTime() + this.refreshRemainingSpawnTime;
         this.refreshRemainingSpawnTime = 0;
      }

      if (this.attackHandler != null && this.isClient() && !(this.attackHandler.isFromInteract() ? Control.MOUSE2 : Control.MOUSE1).isDown()) {
         this.attackHandler.clientEndAttack();
      }

      if (this.isAttacking || this.onCooldown) {
         this.getAttackAnimProgress();
      }

      if (this.isClient() && this.inLiquid() && this.getCurrentSpeed() > 5.0F) {
         this.deltaCounter = this.deltaCounter + delta * Math.max(0.02F, this.getCurrentSpeed() / 40.0F);
         float deltaLimit = GameMath.limit(100.0F - this.getSpeed(), 50.0F, 200.0F);
         if (this.deltaCounter >= deltaLimit) {
            this.deltaCounter -= deltaLimit;
            if (this.getCurrentSpeed() >= 25.0F) {
               addSwimmingParticles(this, 0.0F, 5.0F, 9.0F);
            }
         }

         this.waterSoundTimer = this.waterSoundTimer + delta * Math.max(0.02F, this.getCurrentSpeed() / 40.0F);
         if (this.waterSoundTimer >= 400.0 && this.getCurrentSpeed() >= 5.0F) {
            this.waterSoundTimer = 0.0;
            SoundManager.playSound(new SoundSettings(GameResources.waterblob).volume(0.1F), this);
         }
      }

      super.tickMovement(delta);
   }

   public static void addSwimmingParticles(Mob mob, float xOffset, float yOffset, float dirOffset) {
      Level level = mob.getLevel();
      java.awt.geom.Point2D.Float dir = GameMath.normalize(mob.dx, mob.dy);
      if (dir.y < 0.0F) {
         dirOffset *= 0.5F;
      }

      if (mob.dx != 0.0F && mob.dy == 0.0F) {
         yOffset += 5.0F;
      }

      dirOffset += mob.getCurrentSpeed() * 0.15F;
      java.awt.geom.Point2D.Float particleDir = GameMath.getPerpendicularDir(dir.x, dir.y);
      java.awt.geom.Point2D.Float particleOffset = new java.awt.geom.Point2D.Float(dir.x * dirOffset, dir.y * dirOffset);
      int tileX = mob.getTileX();
      int tileY = mob.getTileY();
      GameTile tile = level.getTile(tileX, tileY);
      Color tileColor = tile.getMapColor(level, tileX, tileY);
      float height = -10 + tile.getLiquidBobbing(level, tileX, tileY);
      float lifeMod = GameMath.limit(mob.getCurrentSpeed() / 40.0F, 0.2F, 1.0F);
      float spreadMod = GameMath.limit(2.0F * (1.0F - mob.getSpeed() / 150.0F), 1.0F, 2.0F);

      for (int i = 0; i < 2; i++) {
         float dirMod = i % 2 == 0 ? spreadMod : -spreadMod;
         java.awt.geom.Point2D.Float moddedParticleDir = new java.awt.geom.Point2D.Float(particleDir.x * dirMod, particleDir.y * dirMod);
         float force = GameRandom.globalRandom.nextFloat() * GameMath.limit(mob.getCurrentSpeed() / 40.0F, 0.2F, 1.0F);
         float randomHeight = 3.0F + force * 3.0F;
         float randomSpeed = (5.0F + force * 10.0F) * lifeMod;
         float spawnY = mob.y
            + yOffset
            + particleOffset.y
            - 10.0F
            + moddedParticleDir.y * 3.0F
            + moddedParticleDir.y * GameRandom.globalRandom.floatGaussian() * 1.5F;
         level.entityManager
            .addParticle(
               mob.x + xOffset + particleOffset.x + moddedParticleDir.x * 3.0F + moddedParticleDir.x * GameRandom.globalRandom.floatGaussian() * 1.5F,
               spawnY,
               Particle.GType.COSMETIC
            )
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               double sin = Math.sin(lifePercent * Math.PI);
               float timeHeight = height + (float)sin * randomHeight;
               pos.x = pos.x + moddedParticleDir.x * randomSpeed * delta * 0.01F;
               float moveY = moddedParticleDir.y * randomSpeed * lifePercent;
               pos.y = spawnY + timeHeight + moveY;
            })
            .color(tileColor.brighter())
            .sizeFadesInAndOut(3, 5, 0.1F)
            .height(-10.0F)
            .lifeTime((int)(GameRandom.globalRandom.getIntBetween(150, 300) * lifeMod));
      }
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      Level level = this.getLevel();
      if (level == null) {
         return null;
      } else {
         return this.autoOpenDoors ? level.regionManager.CAN_OPEN_DOORS_OPTIONS : level.regionManager.CANNOT_OPEN_CAN_CLOSE_DOORS_OPTIONS;
      }
   }

   @Override
   public void serverTick() {
      if (this.hasInvincibility && !this.buffManager.hasBuff(BuffRegistry.DEBUG_INVINCIBILITY)) {
         this.buffManager.addBuff(new ActiveBuff(BuffRegistry.DEBUG_INVINCIBILITY, this, 1.0F, null), false);
      }

      this.refreshCreativeModeBuff();
      if (this.objectUser != null) {
         if (this.isAccelerating() && this.objectUser.accelerationCancelsUse()) {
            java.awt.geom.Point2D.Float dir = GameMath.normalize(this.moveX, this.moveY);
            this.objectUser.stopUsing(true, dir.x, dir.y);
         } else {
            this.objectUser.keepUsing();
            this.objectUser.tick();
         }
      }

      this.getInv().tick();
      this.tickHunger();
      this.buffManager.serverTick();
      this.serverFollowersManager.serverTick();
      this.tickRegen();
      this.tickLevel();
      if (this.activeSetBuffAbility != null && !this.activeSetBuffAbility.tick(this)) {
         this.activeSetBuffAbility.onStopped(this);
         this.activeSetBuffAbility = null;
      }

      if (this.activeTrinketBuffAbility != null && !this.activeTrinketBuffAbility.tick(this)) {
         this.activeTrinketBuffAbility.onStopped(this);
         this.activeTrinketBuffAbility = null;
      }

      if (this.activeMountAbility != null && !this.activeMountAbility.tick(this)) {
         this.activeMountAbility.onStopped(this);
         this.activeMountAbility = null;
      }

      InventoryItem selectedItem = this.getSelectedItem();
      if (selectedItem != null) {
         selectedItem.item.tickHolding(selectedItem, this);
      }

      if (this.healthUpdateTime + this.healthUpdateCooldown < this.getTime()) {
         this.sendHealthPacket(false);
      }

      if (this.resilienceUpdateTime + this.resilienceUpdateCooldown < this.getTime()) {
         this.sendResiliencePacket(false);
      }

      if (this.manaUpdateTime + this.manaUpdateCooldown < this.getTime()) {
         this.sendManaPacket(false);
      }

      if (this.isManaExhausted) {
         this.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.MANA_EXHAUSTION, this, 1000, null), false);
         if (this.isManaExhausted) {
            this.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.MANA_EXHAUSTION, this, 1000, null), false);
         }
      }

      int maxFoodBuffs = this.buffManager.getModifier(BuffModifiers.MAX_FOOD_BUFFS);
      if (maxFoodBuffs < this.lastMaxFoodBuffsModifier) {
         FoodConsumableItem.clearFoodBuffsAboveCount(this, maxFoodBuffs);
      }

      this.lastMaxFoodBuffsModifier = maxFoodBuffs;
   }

   @Override
   public void clientTick() {
      if (this.objectUser != null) {
         this.objectUser.tick();
      }

      this.getInv().tick();
      this.tickHunger();
      this.equipmentBuffManager.clientTickEffects();
      if (GlobalData.isDevMode() && this.ai != this.defaultAI && this.isClientClient()) {
         ClientClient client = this.getClientClient();
         if (client.slot == client.getClient().getSlot()) {
            this.ai.tick();
         }
      }

      super.clientTick();
      this.refreshCreativeModeBuff();
      if (this.activeSetBuffAbility != null && !this.activeSetBuffAbility.tick(this)) {
         if (this.activeSetBuffAbility.isRunningClient) {
            Client client = this.getLevel().getClient();
            client.network.sendPacket(new PacketActiveSetBuffAbilityStopped(client.getSlot(), this.activeSetBuffAbility.uniqueID));
         }

         this.activeSetBuffAbility.onStopped(this);
         this.activeSetBuffAbility = null;
      }

      if (this.activeTrinketBuffAbility != null && !this.activeTrinketBuffAbility.tick(this)) {
         if (this.activeTrinketBuffAbility.isRunningClient) {
            Client client = this.getLevel().getClient();
            client.network.sendPacket(new PacketActiveTrinketBuffAbilityStopped(client.getSlot(), this.activeTrinketBuffAbility.uniqueID));
         }

         this.activeTrinketBuffAbility.onStopped(this);
         this.activeTrinketBuffAbility = null;
      }

      if (this.activeMountAbility != null && !this.activeMountAbility.tick(this)) {
         if (this.activeMountAbility.isRunningClient) {
            Client client = this.getLevel().getClient();
            client.network.sendPacket(new PacketActiveMountAbilityStopped(client.getSlot(), this.activeMountAbility.uniqueID));
         }

         this.activeMountAbility.onStopped(this);
         this.activeMountAbility = null;
      }

      InventoryItem selectedItem = this.getSelectedItem();
      if (selectedItem != null) {
         selectedItem.item.tickHolding(selectedItem, this);
      }
   }

   public void tickSync() {
      this.getInv().tickSync();
   }

   @Override
   protected void doWasHitLogic(MobWasHitEvent event) {
      super.doWasHitLogic(event);
      if (this.isServerClient()) {
         ServerClient client = this.getServerClient();
         if (client.achievementsLoaded() && !client.achievements().SAFETY_LAST.isCompleted() && event.damage > 0 && event.attacker instanceof TNTExplosionEvent
            )
          {
            client.achievements().SAFETY_LAST.markCompleted(client);
         }
      }
   }

   public int getRemainingSpawnInvincibilityTime() {
      if (this.refreshRemainingSpawnTime > 0) {
         return this.refreshRemainingSpawnTime;
      }

      WorldEntity worldEntity = this.getWorldEntity();
      return worldEntity != null ? (int)Math.max(this.spawnInvincibilityEndTime - worldEntity.getLocalTime(), 0L) : 0;
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      int remainingSpawnInvincibilityTime = this.getRemainingSpawnInvincibilityTime();
      return remainingSpawnInvincibilityTime > 0 ? false : super.canBeHit(attacker);
   }

   public void refreshSpawnTime(int remainingInvincibilityTime) {
      this.refreshRemainingSpawnTime = remainingInvincibilityTime;
   }

   public void refreshSpawnTime() {
      this.refreshSpawnTime(spawnInvincibilityFrameMilliseconds);
   }

   public float getInvincibilityFrameAlpha() {
      int remainingTime = this.getRemainingSpawnInvincibilityTime();
      if (remainingTime > 0) {
         if (remainingTime > spawnInvincibilityFrameMilliseconds) {
            float perc = GameUtils.getAnimFloat(remainingTime - spawnInvincibilityFrameMilliseconds, 200);
            return (float)(Math.sin(perc * Math.PI * 2.0) + 1.0) / 2.0F;
         } else {
            float perc = Math.abs((float)remainingTime / spawnInvincibilityFrameMilliseconds - 1.0F);
            return (float)Math.abs((Math.cos((Math.PI * 6) / (perc + 0.5F)) + 1.0) / 2.0);
         }
      } else {
         return 1.0F;
      }
   }

   @Override
   public NetworkClient getPvPOwner() {
      return this.networkClient;
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      if (!super.canBeTargeted(attacker, attackerClient)) {
         return false;
      }

      if (attackerClient != null) {
         NetworkClient networkClient = this.getNetworkClient();
         if (attackerClient == networkClient) {
            return false;
         }

         if (networkClient != null) {
            return networkClient.pvpEnabled() && attackerClient.pvpEnabled();
         }
      }

      return true;
   }

   @Override
   protected boolean switchDoor(LevelObject lo) {
      if (this.isClient() && this.getLevel().getClient().getPlayer() == this) {
         OpenedDoor last = this.openedDoors.get(lo.tileX, lo.tileY);
         if (last != null) {
            last.mobX = this.getX();
            last.mobY = this.getY();
            return true;
         } else {
            PathDoorOption pathDoorOption = this.getPathDoorOption();
            if (pathDoorOption != null && pathDoorOption.canOpen(lo.tileX, lo.tileY)) {
               AtomicBoolean out = new AtomicBoolean();
               GameEvents.triggerEvent(
                  new ObjectInteractEvent(this.getLevel(), lo.tileX, lo.tileY, this),
                  e -> {
                     if (staticSwitchDoor(lo, this)) {
                        this.getLevel()
                           .getClient()
                           .network
                           .sendPacket(new PacketObjectInteract(this.getLevel(), this.getLevel().getClient().getSlot(), lo.tileX, lo.tileY));
                        this.openedDoors.add(lo.tileX, lo.tileY, this.getX(), this.getY(), lo.object.isSwitched);
                        out.set(true);
                     }
                  }
               );
               return out.get();
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   @Override
   protected void switchDoor(OpenedDoor od) {
      GameEvents.triggerEvent(
         new ObjectInteractEvent(this.getLevel(), od.tileX, od.tileY, this),
         e -> {
            LevelObject lo = new LevelObject(this.getLevel(), od.tileX, od.tileY);
            if (staticSwitchDoor(lo, this)) {
               this.getLevel()
                  .getClient()
                  .network
                  .sendPacket(new PacketObjectInteract(this.getLevel(), this.getLevel().getClient().getSlot(), lo.tileX, lo.tileY));
            }
         }
      );
   }

   public void tickControls(MainGame mainGame, boolean isGameTick, GameCamera camera) {
      boolean paralyzed = this.buffManager.getModifier(BuffModifiers.PARALYZED);
      if (Control.INVENTORY.isPressed()) {
         if (mainGame.showMap()) {
            mainGame.setShowMap(false);
         } else if (mainGame.formManager.hasFocusForm() && mainGame.formManager.getFocusComp().inventoryHotkeyClosesContainer()) {
            mainGame.getClient().closeContainer(true);
         } else {
            this.setInventoryExtended(!this.isInventoryExtended());
         }

         mainGame.updateMenuLayerActive();
      }

      if (Control.USE_MOUNT.isPressed() && this.objectUser == null) {
         InventoryItem item = this.inv.equipment.getSelectedEquipmentSlot(0).getItem();
         if (item != null && item.item.isMountItem()) {
            MountItem mountItem = (MountItem)item.item;
            String canUseMountError = mountItem.canUseMount(item, this, this.getLevel());
            if (canUseMountError == null) {
               this.getLevel().getClient().network.sendPacket(new PacketPlayerUseMount(this.getPlayerSlot(), this, mountItem));
            } else if (!canUseMountError.isEmpty()) {
               UniqueFloatText text = new UniqueFloatText(
                  this.getX(), this.getY() - 20, canUseMountError, new FontOptions(16).outline().color(new Color(200, 100, 100)), "mountfail"
               ) {
                  @Override
                  public int getAnchorX() {
                     return PlayerMob.this.getX();
                  }

                  @Override
                  public int getAnchorY() {
                     return PlayerMob.this.getY() - 20;
                  }
               };
               this.getLevel().hudManager.addElement(text);
            }
         } else {
            Mob mount = this.getMount();
            if (mount != null) {
               this.getLevel().getClient().network.sendPacket(new PacketPlayerMobInteract(this.getLevel().getClient().getSlot(), mount.getUniqueID()));
            }
         }
      }

      if (!paralyzed && Control.SET_ABILITY.isPressed()) {
         ObjectValue<ActiveBuff, SetBonusBuff> setBonusBuff = this.equipmentBuffManager.getSetBonusBuff();
         if (setBonusBuff != null) {
            if (setBonusBuff.value instanceof BuffAbility) {
               BuffAbility buffAbility = (BuffAbility)setBonusBuff.value;
               Packet content = buffAbility.getAbilityContent(this, setBonusBuff.object, camera);
               if (buffAbility.canRunAbility(this, setBonusBuff.object, content)) {
                  buffAbility.runAndSendAbility(this.getLevel().getClient(), this, setBonusBuff.object, content);
               }

               return;
            }

            if (setBonusBuff.value instanceof ActiveBuffAbility) {
               ActiveBuffAbility buffAbility = (ActiveBuffAbility)setBonusBuff.value;
               ActiveBuff buff = setBonusBuff.object;
               Packet content = buffAbility.getStartAbilityContent(this, buff, camera);
               if (buffAbility.canRunAbility(this, buff, content)) {
                  if (this.activeSetBuffAbility != null) {
                     this.activeSetBuffAbility.onStopped(this);
                     this.activeSetBuffAbility = null;
                  }

                  buffAbility.onActiveAbilityStarted(this, buff, content);
                  int uniqueID = GameRandom.globalRandom.nextInt();
                  this.activeSetBuffAbility = new ActiveBuffAbilityContainer(uniqueID, true, buffAbility, buff);
                  Client client = this.getLevel().getClient();
                  client.network.sendPacket(new PacketActiveSetBuffAbility(client.getSlot(), buff.buff, uniqueID, content));
               }

               return;
            }
         }
      }

      if (!paralyzed && Control.TRINKET_ABILITY.isPressed()) {
         Mob mount = this.getMount();
         if (mount instanceof MountAbility) {
            MountAbility mountAbility = (MountAbility)mount;
            Packet content = mountAbility.getMountAbilityContent(this, camera);
            if (mountAbility.canRunMountAbility(this, content)) {
               mountAbility.runAndSendMountAbility(this.getLevel().getClient(), this, content);
            }
         } else if (mount instanceof ActiveMountAbility) {
            ActiveMountAbility mountAbility = (ActiveMountAbility)mount;
            Packet content = mountAbility.getStartMountAbilityContent(this, camera);
            if (mountAbility.canRunMountAbility(this, content)) {
               if (this.activeMountAbility != null) {
                  this.activeMountAbility.onStopped(this);
                  this.activeMountAbility = null;
               }

               if (this.activeTrinketBuffAbility != null) {
                  this.activeTrinketBuffAbility.onStopped(this);
                  this.activeTrinketBuffAbility = null;
               }

               mountAbility.onActiveMountAbilityStarted(this, content);
               int uniqueID = GameRandom.globalRandom.nextInt();
               this.activeMountAbility = new ActiveMountAbilityContainer(uniqueID, true, mountAbility, mount);
               Client client = this.getLevel().getClient();
               client.network.sendPacket(new PacketActiveMountAbility(client.getSlot(), mount, uniqueID, content));
            }
         } else {
            for (ActiveTrinketBuff trinketBuffs : this.equipmentBuffManager.getTrinketBuffs()) {
               for (int i = 0; i < trinketBuffs.buffs.length; i++) {
                  if (trinketBuffs.buffs[i] instanceof BuffAbility) {
                     BuffAbility buffAbility = (BuffAbility)trinketBuffs.buffs[i];
                     ActiveBuff buff = trinketBuffs.activeBuffs[i];
                     if (buff != null) {
                        Packet content = buffAbility.getAbilityContent(this, buff, camera);
                        if (buffAbility.canRunAbility(this, buff, content)) {
                           buffAbility.runAndSendAbility(this.getLevel().getClient(), this, buff, content);
                        }

                        return;
                     }
                  } else if (trinketBuffs.buffs[i] instanceof ActiveBuffAbility) {
                     ActiveBuffAbility buffAbility = (ActiveBuffAbility)trinketBuffs.buffs[i];
                     ActiveBuff buff = trinketBuffs.activeBuffs[i];
                     if (buff != null) {
                        Packet content = buffAbility.getStartAbilityContent(this, buff, camera);
                        if (buffAbility.canRunAbility(this, buff, content)) {
                           if (this.activeMountAbility != null) {
                              this.activeMountAbility.onStopped(this);
                              this.activeMountAbility = null;
                           }

                           if (this.activeTrinketBuffAbility != null) {
                              this.activeTrinketBuffAbility.onStopped(this);
                              this.activeTrinketBuffAbility = null;
                           }

                           buffAbility.onActiveAbilityStarted(this, buff, content);
                           int uniqueID = GameRandom.globalRandom.nextInt();
                           this.activeTrinketBuffAbility = new ActiveBuffAbilityContainer(uniqueID, true, buffAbility, buff);
                           Client client = this.getLevel().getClient();
                           client.network.sendPacket(new PacketActiveTrinketBuffAbility(client.getSlot(), buff.buff, uniqueID, content));
                        }

                        return;
                     }
                  }
               }
            }
         }
      } else if (!paralyzed && Control.HEALTH_POT.isPressed()) {
         int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
         this.getLevel().getClient().network.sendPacket(new PacketPlayerAction(PacketPlayerAction.PlayerAction.USE_HEALTH_POTION, seed));
         this.runPlayerAction(PacketPlayerAction.PlayerAction.USE_HEALTH_POTION, seed);
      } else if (!paralyzed && Control.MANA_POT.isPressed()) {
         int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
         this.getLevel().getClient().network.sendPacket(new PacketPlayerAction(PacketPlayerAction.PlayerAction.USE_MANA_POTION, seed));
         this.runPlayerAction(PacketPlayerAction.PlayerAction.USE_MANA_POTION, seed);
      } else if (!paralyzed && Control.EAT_FOOD.isPressed()) {
         int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
         this.getLevel().getClient().network.sendPacket(new PacketPlayerAction(PacketPlayerAction.PlayerAction.EAT_FOOD, seed));
         this.runPlayerAction(PacketPlayerAction.PlayerAction.EAT_FOOD, seed);
      } else if (!paralyzed && Control.BUFF_POTS.isPressed()) {
         int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
         this.getLevel().getClient().network.sendPacket(new PacketPlayerAction(PacketPlayerAction.PlayerAction.USE_BUFF_POTION, seed));
         this.runPlayerAction(PacketPlayerAction.PlayerAction.USE_BUFF_POTION, seed);
      } else {
         if (!paralyzed && Control.PLACE_TORCH.isPressed()) {
            for (int i = 0; i < this.getInv().main.getSize(); i++) {
               final InventoryItem item = this.getInv().main.getItem(i);
               if (item != null && item.item instanceof TorchItem) {
                  Point levelPos;
                  if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
                     java.awt.geom.Point2D.Float aimDir = this.getControllerAimDir();
                     levelPos = item.item.getControllerAttackLevelPos(this.getLevel(), aimDir.x, aimDir.y, this, item);
                  } else {
                     levelPos = new Point(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
                  }

                  final TorchItem torchItem = (TorchItem)item.item;
                  final Level level = this.getLevel();
                  if (torchItem.canPlaceTorch(level, levelPos.x, levelPos.y, item, this)) {
                     this.tryAttack(new PlayerInventorySlot(this.getInv().main.getInventoryID(), i), levelPos.x, levelPos.y);
                     return;
                  }

                  int placeRange = torchItem.getTorchPlaceRange(level, item, this);
                  if (placeRange > 0) {
                     double distance = levelPos.distance(this.x, this.y);
                     java.awt.geom.Point2D.Float placeDir = GameMath.normalize(levelPos.x - this.x, levelPos.y - this.y);
                     final float placeAngle = GameMath.getAngle(placeDir);
                     final java.awt.geom.Point2D.Float angleTestPos = new java.awt.geom.Point2D.Float(this.x - placeDir.x * 64.0F, this.y - placeDir.y * 64.0F);
                     int centerTileX;
                     int centerTileY;
                     if (distance >= placeRange) {
                        centerTileX = GameMath.getTileCoordinate(this.x + placeDir.x * placeRange);
                        centerTileY = GameMath.getTileCoordinate(this.y + placeDir.y * placeRange);
                     } else {
                        centerTileX = GameMath.getTileCoordinate(levelPos.x);
                        centerTileY = GameMath.getTileCoordinate(levelPos.y);
                     }

                     int tileRange = (int)Math.ceil(placeRange / 32.0F) + 1;
                     AreaFinder areaFinder = new AreaFinder(centerTileX, centerTileY, tileRange, true) {
                        @Override
                        public boolean checkPoint(int x, int y) {
                           int levelX = x * 32 + 16;
                           int levelY = y * 32 + 16;
                           java.awt.geom.Point2D.Float dir = GameMath.normalize(levelX - angleTestPos.x, levelY - angleTestPos.y);
                           float angle = GameMath.getAngle(dir);
                           float angleDifference = GameMath.getAngleDifference(placeAngle, angle);
                           return Math.abs(angleDifference) > 45.0F ? false : torchItem.canPlaceTorch(level, levelX, levelY, item, PlayerMob.this);
                        }
                     };
                     areaFinder.runFinder();
                     if (areaFinder.hasFound()) {
                        Point placeTile = areaFinder.getFirstFind();
                        this.tryAttack(new PlayerInventorySlot(this.getInv().main.getInventoryID(), i), placeTile.x * 32 + 16, placeTile.y * 32 + 16);
                        return;
                     }
                  }
               }
            }
         }

         if (Control.ERASER.isPressed() && this.getWorldSettings().creativeMode) {
            boolean toHotbar = !this.inventoryExtended || Input.lastInputIsController;
            Item eraser = ItemRegistry.getItem("eraser");
            if (toHotbar) {
               this.swapToItem(eraser, true);
            } else {
               PacketSpawnCreativeItem.runAndSendAction(this.getClient(), new InventoryItem(eraser), PacketSpawnCreativeItem.Destination.DragSlot, false, true);
            }
         } else if (Control.PIPETTE.isPressed()) {
            this.pipetteAt(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
         } else {
            if (Control.SMART_MINING.isPressed() && !mainGame.formManager.isMouseOver(Control.SMART_MINING.getEvent())) {
               this.smartMiningTogglePressed = true;
            } else if (Control.SMART_MINING.isReleased()) {
               if (this.smartMiningTogglePressed && !mainGame.formManager.isMouseOver(Control.SMART_MINING.getEvent())) {
                  Settings.smartMining = !Settings.smartMining;
                  Settings.saveClientSettings();
                  Color color = Settings.smartMining ? new Color(100, 200, 100) : new Color(200, 100, 100);
                  UniqueFloatText text = new UniqueFloatText(
                     this.getX(),
                     this.getY() - 20,
                     Localization.translate("misc", Settings.smartMining ? "smartminingon" : "smartminingoff"),
                     new FontOptions(16).outline().color(color),
                     "smartmining"
                  ) {
                     @Override
                     public int getAnchorX() {
                        return PlayerMob.this.getX();
                     }

                     @Override
                     public int getAnchorY() {
                        return PlayerMob.this.getY() - 20;
                     }
                  };
                  text.riseTime = 500;
                  text.fadeOutTime = 500;
                  text.expandTime = 50;
                  this.getLevel().hudManager.addElement(text);
                  SoundManager.playSound(GameResources.tick, SoundEffect.ui());
               }

               this.smartMiningTogglePressed = false;
            }

            if (ControllerInput.ATTACK.isJustPressed() && (!ControllerInput.isCursorVisible() || !mainGame.getFormManager().isMouseOver())) {
               if (ControllerInput.isCursorVisible()) {
                  this.tryAttack(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
               } else {
                  this.tryControllerAttack();
               }
            }

            if (ControllerInput.INTERACT.isJustPressed()) {
               if (ControllerInput.isCursorVisible()) {
                  this.runClientInteract(camera.getMouseLevelPosX(), camera.getMouseLevelPosY(), false);
               } else if (this.attackHandler != null && !this.attackHandler.isFromInteract()) {
                  this.attackHandler.onControllerInteracted(ControllerInput.getAimX(), ControllerInput.getAimY());
                  this.getLevel()
                     .getClient()
                     .network
                     .sendPacket(PacketPlayerAttackHandler.clientInteractController(ControllerInput.getAimX(), ControllerInput.getAimY()));
               } else {
                  ControllerInteractTarget target = this.getControllerInteractTarget(false, this.getCurrentAttackHeight(), camera);
                  if (target != null) {
                     target.runInteract();
                  }
               }
            }

            if (Control.NEXT_HOTBAR.isPressed()) {
               this.setSelectedSlot((this.selectedSlot + 1) % 10);
            }

            if (Control.PREV_HOTBAR.isPressed()) {
               this.setSelectedSlot((this.selectedSlot + 9) % 10);
            }

            for (int i = 0; i < Control.HOTBAR_SLOTS.length; i++) {
               if (Control.HOTBAR_SLOTS[i].isPressed()) {
                  this.setSelectedSlot(i);
               }
            }

            for (int i = 0; i < Control.ITEM_SETS.length; i++) {
               if (Control.ITEM_SETS[i].isPressed()) {
                  this.getInv().equipment.setSelectedSet(i);
                  Client client = this.getClient();
                  if (client != null) {
                     client.sendMovementPacket(false);
                  }
               }
            }

            if (isGameTick) {
               if (this.ai != this.defaultAI) {
                  if (!ControllerInput.MOVE.hasChanged()
                     && ControllerInput.MOVE.getX() == 0.0F
                     && ControllerInput.MOVE.getY() == 0.0F
                     && !Control.MOVE_TO_MOUSE.isDown()
                     && !Control.MOVE_LEFT.isDown()
                     && !Control.MOVE_UP.isDown()
                     && !Control.MOVE_RIGHT.isDown()
                     && !Control.MOVE_DOWN.isDown()) {
                     return;
                  }

                  this.ai = this.defaultAI;
                  System.out.println("Reset dev AI for player movement input");
               }

               boolean grounded = this.buffManager.getModifier(BuffModifiers.GROUNDED);
               boolean canMove = !paralyzed && !grounded;
               float lastMoveX = this.moveX;
               float lastMoveY = this.moveY;
               int dir = this.getDir();
               this.moveX = 0.0F;
               this.moveY = 0.0F;
               if (canMove && (ControllerInput.MOVE.hasChanged() || ControllerInput.MOVE.getX() != 0.0F || ControllerInput.MOVE.getY() != 0.0F)) {
                  double distance = new java.awt.geom.Point2D.Float().distance(ControllerInput.MOVE.getX(), ControllerInput.MOVE.getY());
                  if (distance >= 0.7) {
                     this.moveX = ControllerInput.MOVE.getX();
                     this.moveY = ControllerInput.MOVE.getY();
                     this.lastControllerAimX = this.moveX;
                     this.lastControllerAimY = this.moveY;
                     this.snapMovementAngle();
                  } else if (distance >= 0.4F && !this.isAttacking) {
                     this.setFacingDir(ControllerInput.MOVE.getX(), ControllerInput.MOVE.getY());
                     this.lastControllerAimX = ControllerInput.MOVE.getX();
                     this.lastControllerAimY = ControllerInput.MOVE.getY();
                  }
               }

               if (ControllerInput.AIM.hasChanged() || ControllerInput.AIM.getX() != 0.0F || ControllerInput.AIM.getY() != 0.0F) {
                  double distance = new java.awt.geom.Point2D.Float().distance(ControllerInput.AIM.getX(), ControllerInput.AIM.getY());
                  if (distance >= 0.4F) {
                     this.lastControllerAimX = ControllerInput.AIM.getX();
                     this.lastControllerAimY = ControllerInput.AIM.getY();
                  }
               }

               if (canMove && Control.MOVE_TO_MOUSE.isDown()) {
                  int xDelta = camera.getMouseLevelPosX() - this.getX();
                  if (Math.abs(xDelta) > 4) {
                     this.moveX = xDelta;
                  }

                  int yDelta = camera.getMouseLevelPosY() - this.getY();
                  if (Math.abs(yDelta) > 4) {
                     this.moveY = yDelta;
                  }

                  if (this.moveX != 0.0F || this.moveY != 0.0F) {
                     this.snapMovementAngle();
                  }
               }

               boolean reset = this.dx == 0.0F && this.dy == 0.0F;
               if (canMove && Control.MOVE_LEFT.isDown()) {
                  reset = false;
                  if (dir == 3) {
                     this.firstPress = false;
                  }

                  if (!this.firstPress) {
                     this.moveX = -1.0F;
                  } else {
                     if (!this.isAttacking) {
                        this.setDir(3);
                     }

                     this.firstPress = false;
                  }
               }

               if (canMove && Control.MOVE_RIGHT.isDown()) {
                  reset = false;
                  if (dir == 1) {
                     this.firstPress = false;
                  }

                  if (!this.firstPress) {
                     this.moveX = 1.0F;
                  } else {
                     if (!this.isAttacking) {
                        this.setDir(1);
                     }

                     this.firstPress = false;
                  }
               }

               if (canMove && Control.MOVE_UP.isDown()) {
                  reset = false;
                  if (dir == 0) {
                     this.firstPress = false;
                  }

                  if (!this.firstPress) {
                     this.moveY = -1.0F;
                  } else {
                     if (!this.isAttacking) {
                        this.setDir(0);
                     }

                     this.firstPress = false;
                  }
               }

               if (canMove && Control.MOVE_DOWN.isDown()) {
                  reset = false;
                  if (dir == 2) {
                     this.firstPress = false;
                  }

                  if (!this.firstPress) {
                     this.moveY = 1.0F;
                  } else {
                     if (!this.isAttacking) {
                        this.setDir(2);
                     }

                     this.firstPress = false;
                  }
               }

               if (this.buffManager.getModifier(BuffModifiers.CONFUSED)) {
                  this.moveX = -this.moveX;
                  this.moveY = -this.moveY;
               }

               if (reset) {
                  this.firstPress = true;
               }

               if (this.isRiding()) {
                  Mob mount = this.getMount();
                  if (mount != null) {
                     if (!this.isAttacking) {
                        mount.setDir(dir);
                     }

                     mount.setPos(this.x, this.y, true);
                     mount.moveX = this.moveX;
                     mount.moveY = this.moveY;
                  }
               }

               if (lastMoveX != this.moveX || lastMoveY != this.moveY || dir != this.getDir()) {
                  this.getLevel().getClient().sendMovementPacket(false);
               }
            }
         }
      }
   }

   public boolean pipetteAt(int x, int y) {
      boolean isCreative = this.getWorldSettings().creativeMode;
      Item itemToLookFor = null;
      if (isCreative) {
         Optional<Mob> mobToFind = this.getLevel()
            .entityManager
            .streamAreaMobsAndPlayersTileRange(x, y, 1)
            .filter(mob -> !mob.isPlayer && mob.getSelectBox().contains(x, y))
            .findFirst();
         if (mobToFind.isPresent()) {
            itemToLookFor = mobToFind.get().getSpawnItem();
         }
      }

      if (itemToLookFor == null) {
         Point levelPos = new Point(x, y);
         Level level = this.getLevel();
         GameTile tile = level.getTile(GameMath.getTileCoordinate(levelPos.x), GameMath.getTileCoordinate(levelPos.y));
         TileItem tileItem = tile.getTileItem();
         GameObject object = level.getObject(GameMath.getTileCoordinate(levelPos.x), GameMath.getTileCoordinate(levelPos.y));
         ObjectItem objectItem = object.getObjectItem();
         itemToLookFor = object.getID() == 0 ? tileItem : objectItem;
      }

      if (itemToLookFor != null) {
         if (this.getDraggingItem() != null && isCreative && ItemRegistry.isValidCreativeItem(itemToLookFor)) {
            int index = this.getClient().getInventoryContainer().getClientDraggingSlot().getContainerIndex();
            ContainerActionResult result = this.getClient().getContainer().applyContainerAction(index, ContainerAction.QUICK_TRASH);
            this.getClient().network.sendPacket(new PacketContainerAction(index, ContainerAction.QUICK_TRASH, result.value));
         }

         if (this.isCreativeMenuExtended() && this.inventoryExtended) {
            PacketSpawnCreativeItem.runAndSendAction(
               this.getClient(), new InventoryItem(itemToLookFor), PacketSpawnCreativeItem.Destination.DragSlot, false, false
            );
            return true;
         }

         this.swapToItem(itemToLookFor, true);
      }

      return false;
   }

   public boolean swapToItem(Item item, boolean spawnItemInCreative) {
      boolean hasEmptySlot = false;
      boolean shouldSpawnItemInCreative = spawnItemInCreative && this.getWorldSettings().creativeMode && ItemRegistry.isValidCreativeItem(item);
      if (shouldSpawnItemInCreative) {
         for (int i = 0; i < this.getInv().main.getSize(); i++) {
            InventoryItem invItem = this.getInv().main.getItem(i);
            if (invItem == null) {
               hasEmptySlot = true;
            } else if (invItem.item.getID() == item.getID()) {
               shouldSpawnItemInCreative = false;
               break;
            }
         }
      }

      if (shouldSpawnItemInCreative) {
         if (!hasEmptySlot) {
            Container inventoryContainer = this.getClient().getInventoryContainer();
            int index = inventoryContainer.CLIENT_HOTBAR_START + this.getSelectedSlot();
            ContainerActionResult result = this.getClient().getContainer().applyContainerAction(index, ContainerAction.QUICK_TRASH);
            this.getClient().network.sendPacket(new PacketContainerAction(index, ContainerAction.QUICK_TRASH, result.value));
         }

         PacketSpawnCreativeItem.runAndSendAction(this.getClient(), new InventoryItem(item), PacketSpawnCreativeItem.Destination.Hotbar, false, false);
         return true;
      } else {
         for (int i = 0; i < this.getInv().main.getSize(); i++) {
            InventoryItem invItem = this.getInv().main.getItem(i);
            if (invItem != null && invItem.item.getID() == item.getID()) {
               if (i < 10) {
                  this.setSelectedSlot(i);
                  return true;
               }

               int selectedSlot = this.getSelectedSlot();
               if (this.getInv().main.isSlotClear(selectedSlot)) {
                  this.getLevel().getClient().network.sendPacket(new PacketSwapInventorySlots(this, i, selectedSlot));
                  this.getInv().main.swapItems(i, selectedSlot);
                  return true;
               }

               int nonLockedSlot = -1;

               for (int j = 0; j < 10; j++) {
                  if (this.getInv().main.isSlotClear(j)) {
                     this.getLevel().getClient().network.sendPacket(new PacketSwapInventorySlots(this, i, j));
                     this.getInv().main.swapItems(i, j);
                     this.setSelectedSlot(j);
                     return true;
                  }

                  if (nonLockedSlot == -1 && !this.getInv().main.isItemLocked(j)) {
                     nonLockedSlot = j;
                  }
               }

               if (nonLockedSlot != -1) {
                  this.getLevel().getClient().network.sendPacket(new PacketSwapInventorySlots(this, i, nonLockedSlot));
                  this.getInv().main.swapItems(i, nonLockedSlot);
                  this.setSelectedSlot(nonLockedSlot);
                  return true;
               }
               break;
            }
         }

         return false;
      }
   }

   public void runActiveSetBuffAbility(int buffID, int uniqueID, Packet content) {
      ActiveBuff buff = this.buffManager.getBuff(buffID);
      if (buff != null && buff.buff instanceof ActiveBuffAbility) {
         if (this.activeSetBuffAbility != null) {
            this.activeSetBuffAbility.onStopped(this);
            this.activeSetBuffAbility = null;
         }

         ActiveBuffAbility buffAbility = (ActiveBuffAbility)buff.buff;
         buffAbility.onActiveAbilityStarted(this, buff, content);
         this.activeSetBuffAbility = new ActiveBuffAbilityContainer(uniqueID, false, buffAbility, buff);
      }
   }

   public void runActiveTrinketBuffAbility(int buffID, int uniqueID, Packet content) {
      ActiveBuff buff = this.buffManager.getBuff(buffID);
      if (buff != null && buff.buff instanceof ActiveBuffAbility) {
         if (this.activeMountAbility != null) {
            this.activeMountAbility.onStopped(this);
            this.activeMountAbility = null;
         }

         if (this.activeTrinketBuffAbility != null) {
            this.activeTrinketBuffAbility.onStopped(this);
            this.activeTrinketBuffAbility = null;
         }

         ActiveBuffAbility buffAbility = (ActiveBuffAbility)buff.buff;
         buffAbility.onActiveAbilityStarted(this, buff, content);
         this.activeTrinketBuffAbility = new ActiveBuffAbilityContainer(uniqueID, false, buffAbility, buff);
      }
   }

   public void runActiveMountAbility(int mountUniqueID, int uniqueID, Packet content) {
      Mob mount = this.getMount();
      if (mount != null && mount.getUniqueID() == mountUniqueID && mount instanceof ActiveMountAbility) {
         if (this.activeMountAbility != null) {
            this.activeMountAbility.onStopped(this);
            this.activeMountAbility = null;
         }

         if (this.activeTrinketBuffAbility != null) {
            this.activeTrinketBuffAbility.onStopped(this);
            this.activeTrinketBuffAbility = null;
         }

         ActiveMountAbility mountAbility = (ActiveMountAbility)mount;
         mountAbility.onActiveMountAbilityStarted(this, content);
         this.activeMountAbility = new ActiveMountAbilityContainer(uniqueID, false, mountAbility, mount);
      }
   }

   public boolean runActiveSetBuffAbilityUpdate(int uniqueID, Packet content) {
      if (this.activeSetBuffAbility != null && this.activeSetBuffAbility.uniqueID == uniqueID) {
         this.activeSetBuffAbility.update(this, content);
         return true;
      } else {
         return false;
      }
   }

   public boolean runActiveTrinketBuffAbilityUpdate(int uniqueID, Packet content) {
      if (this.activeTrinketBuffAbility != null && this.activeTrinketBuffAbility.uniqueID == uniqueID) {
         this.activeTrinketBuffAbility.update(this, content);
         return true;
      } else {
         return false;
      }
   }

   public boolean runActiveMountAbilityUpdate(int uniqueID, Packet content) {
      if (this.activeMountAbility != null && this.activeMountAbility.uniqueID == uniqueID) {
         this.activeMountAbility.update(this, content);
         return true;
      } else {
         return false;
      }
   }

   public void sendActiveSetBuffAbilityState(Server server, ServerClient target) {
      ServerClient me = this.getServerClient();
      if (this.activeSetBuffAbility != null) {
         Packet content = this.activeSetBuffAbility.buffAbility.getRunningAbilityContent(this, this.activeSetBuffAbility.activeBuff);
         target.sendPacket(new PacketActiveSetBuffAbility(me.slot, this.activeSetBuffAbility.activeBuff.buff, this.activeSetBuffAbility.uniqueID, content));
      } else {
         target.sendPacket(new PacketActiveSetBuffAbilityStopped(me.slot, 0));
      }
   }

   public void sendActiveTrinketBuffAbilityState(Server server, ServerClient target) {
      ServerClient me = this.getServerClient();
      if (this.activeTrinketBuffAbility != null) {
         Packet content = this.activeTrinketBuffAbility.buffAbility.getRunningAbilityContent(this, this.activeTrinketBuffAbility.activeBuff);
         target.sendPacket(
            new PacketActiveTrinketBuffAbility(me.slot, this.activeTrinketBuffAbility.activeBuff.buff, this.activeTrinketBuffAbility.uniqueID, content)
         );
      } else {
         target.sendPacket(new PacketActiveTrinketBuffAbilityStopped(me.slot, 0));
      }
   }

   public void sendActiveMountAbilityState(Server server, ServerClient target) {
      ServerClient me = this.getServerClient();
      if (this.activeMountAbility != null) {
         Packet content = this.activeMountAbility.mountAbility.getRunningMountAbilityContent(this);
         target.sendPacket(new PacketActiveMountAbility(me.slot, this.activeMountAbility.mount, this.activeMountAbility.uniqueID, content));
      } else {
         target.sendPacket(new PacketActiveMountAbilityStopped(me.slot, 0));
      }
   }

   public void onActiveSetBuffAbilityStopped(int uniqueID) {
      if (this.activeSetBuffAbility != null && (uniqueID == 0 || this.activeSetBuffAbility.uniqueID == uniqueID)) {
         this.activeSetBuffAbility.onStopped(this);
         this.activeSetBuffAbility = null;
      }
   }

   public void onActiveTrinketBuffAbilityStopped(int uniqueID) {
      if (this.activeTrinketBuffAbility != null && (uniqueID == 0 || this.activeTrinketBuffAbility.uniqueID == uniqueID)) {
         this.activeTrinketBuffAbility.onStopped(this);
         this.activeTrinketBuffAbility = null;
      }
   }

   public void onActiveMountAbilityStopped(int uniqueID) {
      if (this.activeMountAbility != null && (uniqueID == 0 || this.activeMountAbility.uniqueID == uniqueID)) {
         this.activeMountAbility.onStopped(this);
         this.activeMountAbility = null;
      }
   }

   private void snapMovementAngle() {
      int angle = (int)Math.toDegrees(Math.atan2(this.moveY, this.moveX));
      angle = angle / 5 * 5;
      java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
      this.moveX = GameMath.toDecimals(dir.x, 2);
      this.moveY = GameMath.toDecimals(dir.y, 2);
   }

   @Override
   public void tickCurrentMovement(float delta) {
      if (GlobalData.isDevMode() && this.ai != this.defaultAI && this.isClientClient()) {
         ClientClient client = this.getClientClient();
         if (client.slot == client.getClient().getSlot()) {
            int lastDir = this.getDir();
            float lastMoveX = this.moveX;
            float lastMoveY = this.moveY;
            super.tickCurrentMovement(delta);
            if (lastMoveX != this.moveX || lastMoveY != this.moveY || lastDir != this.getDir()) {
               this.getLevel().getClient().sendMovementPacket(false);
            }
         }
      }
   }

   @Override
   public boolean isVisible() {
      NetworkClient client = this.getNetworkClient();
      return client != null && !client.hasSpawned() ? false : super.isVisible();
   }

   @Override
   public boolean canBePushed(Mob other) {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null && objectUser.preventsUserPushed() ? false : super.canBePushed(other);
   }

   @Override
   public boolean canPushMob(Mob other) {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null && objectUser.preventsUserPushed() ? false : super.canPushMob(other);
   }

   @Override
   public boolean canLevelInteract() {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null && objectUser.preventsUserLevelInteract() ? false : super.canLevelInteract();
   }

   @Override
   public boolean canBeTargetedFromAdjacentTiles() {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null ? objectUser.userCanBeTargetedFromAdjacentTiles() : super.canBeTargetedFromAdjacentTiles();
   }

   @Override
   public Rectangle getCollision() {
      Rectangle collision = super.getCollision();
      ObjectUserActive objectUser = this.objectUser;
      if (objectUser != null) {
         Rectangle newCollision = objectUser.getUserCollisionBox(collision);
         if (newCollision != null) {
            return newCollision;
         }
      }

      return collision;
   }

   @Override
   public Rectangle getHitBox() {
      Rectangle hitbox = super.getHitBox();
      ObjectUserActive objectUser = this.objectUser;
      if (objectUser != null) {
         Rectangle newHitBox = objectUser.getUserHitBox(hitbox);
         if (newHitBox != null) {
            return newHitBox;
         }
      }

      return hitbox;
   }

   @Override
   public Rectangle getSelectBox(int x, int y) {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null ? objectUser.getUserSelectBox() : super.getSelectBox(x, y);
   }

   @Override
   public Point getDrawPos() {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null ? objectUser.getUserAppearancePos() : super.getDrawPos();
   }

   @Override
   public int getDrawX() {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null ? objectUser.getUserAppearancePos().x : super.getDrawX();
   }

   @Override
   public int getDrawY() {
      ObjectUserActive objectUser = this.objectUser;
      return objectUser != null ? objectUser.getUserAppearancePos().y : super.getDrawY();
   }

   @Override
   public DrawOptions getUserDrawOptions(
      Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective, Consumer<HumanDrawOptions> humanDrawOptionsModifier
   ) {
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      return PlayerSprite.getDrawOptions(this, x, y, light, camera, humanDrawOptionsModifier);
   }

   @Override
   public void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      if (this.isVisible()) {
         GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
         DrawOptionsList debug = new DrawOptionsList();
         if (Debug.isActive() && this.getLevel() != null) {
            int debugX = this.getX() - camera.getX() - 20;
            int debugY = this.getY() - camera.getY();
            FontOptions fontOptions = new FontOptions(16);
            debug.add(new StringDrawOptions(fontOptions, x + ", " + this.dx).pos(debugX, debugY));
            debug.add(new StringDrawOptions(fontOptions, y + ", " + this.dy).pos(debugX, debugY + 15));
            debug.add(
               new StringDrawOptions(fontOptions, this.getFriction() + ", " + this.getSpeed() + ", " + this.getCurrentSpeed() + ", " + this.getDistanceRan())
                  .pos(debugX, debugY + 30)
            );
            debug.add(new StringDrawOptions(fontOptions, "Team: " + this.getTeam()).pos(debugX, debugY + 45));
            debug.add(new StringDrawOptions(fontOptions, "Mount: " + this.mount).pos(debugX, debugY + 60));
            debug.add(
               new StringDrawOptions(
                     fontOptions,
                     "Boomerangs: "
                        + this.getBoomerangsUsage()
                        + " - "
                        + GameUtils.join(this.boomerangs.stream().map(p -> p.getStringID() + " (" + p.getUniqueID() + ")").toArray(), ", ")
                  )
                  .pos(debugX, debugY + 75)
            );
            debug.add(new StringDrawOptions(fontOptions, "Dir: " + this.getDir() + ", " + this.beforeAttackDir).pos(debugX, debugY + 90));
         }

         topList.add(tm -> debug.draw());
         if (this.objectUser == null || this.objectUser.drawsUser()) {
            final DrawOptions options = PlayerSprite.getDrawOptions(this, x, y, light, camera, null);
            list.add(new MobDrawable() {
               @Override
               public void draw(TickManager tickManager) {
                  options.draw();
               }
            });
            this.addShadowDrawables(tileList, level, x, y, light, camera);
         }
      }
   }

   public FormExpressionWheel.Expression getActiveExpression() {
      return this.getLocalTime() < this.activeExpressionEndTime ? this.activeExpression : null;
   }

   public void modifyExpressionDrawOptions(HumanDrawOptions options) {
      if (this.activeExpression != null) {
         long time = this.activeExpressionEndTime - this.getLocalTime();
         if (time >= 0L) {
            float progress = Math.min(1.0F, 1.0F - (float)time / this.activeExpression.animationTimeMillis);
            this.activeExpression.drawOptionsModifier.accept(progress, options);
         }
      }
   }

   public void startExpression(FormExpressionWheel.Expression expression) {
      this.startExpression(expression, expression.animationTimeMillis);
   }

   public void startExpression(FormExpressionWheel.Expression expression, int milliseconds) {
      this.activeExpression = expression;
      this.activeExpressionEndTime = this.getLocalTime() + milliseconds;
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-10, -10, 20, 24);
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      if (this.getLevel() != null) {
         PlayerSprite.getIconAnimationDrawOptions(x - 15, y - 26, 32, 32, this).draw();
         String name = this.getDisplayName();
         FontOptions options = new FontOptions(12).color(200, 200, 200).outline();
         int nameWidth = FontManager.bit.getWidthCeil(name, options);
         FontManager.bit.drawString(x - nameWidth / 2, y - 34, name, options);
      }
   }

   public void submitInputEvent(MainGame mainGame, InputEvent event, GameCamera camera) {
      int mouseLevelX = event.pos.sceneX + camera.getX();
      int mouseLevelY = event.pos.sceneY + camera.getY();
      if (!event.isMouseMoveEvent() && event.state && !mainGame.formManager.isMouseOver() && !this.buffManager.getModifier(BuffModifiers.PARALYZED)) {
         if (event.getID() == Control.MOUSE1.getKey()) {
            this.tryAttack(mouseLevelX, mouseLevelY);
         } else if (event.getID() == Control.MOUSE2.getKey() && !this.runClientInteract(mouseLevelX, mouseLevelY, false) && this.getDraggingItem() != null) {
            ContainerAction action;
            if (Control.INV_QUICK_MOVE.isDown()) {
               action = ContainerAction.TAKE_ONE;
            } else {
               action = ContainerAction.RIGHT_CLICK;
            }

            ContainerActionResult result = this.getLevel().getClient().getContainer().applyContainerAction(-1, action);
            if (result.error != null) {
               InputPosition mousePos = WindowManager.getWindow().mousePos();
               Renderer.hudManager
                  .addElement(new UniqueScreenFloatText(mousePos.hudX, mousePos.hudY, result.error, new FontOptions(16).outline(), "dropError"));
            } else {
               this.getLevel().getClient().network.sendPacket(new PacketContainerAction(-1, action, result.value));
            }
         }
      }
   }

   public java.awt.geom.Point2D.Float getControllerAimDir() {
      float dx = ControllerInput.getAimX();
      float dy = ControllerInput.getAimY();
      if (dx == 0.0F && dy == 0.0F) {
         dx = ControllerInput.MOVE.getX();
         dy = ControllerInput.MOVE.getY();
      }

      if (dx == 0.0F && dy == 0.0F) {
         dx = this.lastControllerAimX;
         dy = this.lastControllerAimY;
      }

      if (dx == 0.0F && dy == 0.0F) {
         int dir = this.getDir();
         if (this.isAttacking && this.beforeAttackDir != -1) {
            dir = this.beforeAttackDir;
         }

         if (dir == 0) {
            dy = -1.0F;
         } else if (dir == 1) {
            dx = 1.0F;
         } else if (dir == 2) {
            dy = 1.0F;
         } else if (dir == 3) {
            dx = -1.0F;
         } else {
            dx = 1.0F;
         }
      }

      return new java.awt.geom.Point2D.Float(dx, dy);
   }

   public java.awt.geom.Point2D.Float getControllerAimDirNormalized() {
      java.awt.geom.Point2D.Float dir = this.getControllerAimDir();
      return GameMath.normalize(dir.x, dir.y);
   }

   public void tryAttack(GameCamera camera) {
      if (Input.lastInputIsController && !ControllerInput.isCursorVisible()) {
         this.tryControllerAttack();
      } else {
         this.tryAttack(camera.getMouseLevelPosX(), camera.getMouseLevelPosY());
      }
   }

   public void runClientInteract(boolean onlyItemInteract, GameCamera camera) {
      if (!Input.lastInputIsController || ControllerInput.isCursorVisible()) {
         this.runClientInteract(camera.getMouseLevelPosX(), camera.getMouseLevelPosY(), onlyItemInteract);
      } else if (this.attackHandler != null && !this.attackHandler.isFromInteract()) {
         this.constantInteract = this.attackHandler.getConstantInteract();
         this.attackHandler.onControllerInteracted(ControllerInput.getAimX(), ControllerInput.getAimY());
         this.getLevel()
            .getClient()
            .network
            .sendPacket(PacketPlayerAttackHandler.clientInteractController(ControllerInput.getAimX(), ControllerInput.getAimY()));
      } else {
         ControllerInteractTarget target = this.getControllerInteractTarget(false, this.getCurrentAttackHeight(), camera);
         if (target != null) {
            target.runInteract();
         }
      }
   }

   public void tryAttack(int levelX, int levelY) {
      this.tryAttack(this.getSelectedItemSlot(), levelX, levelY);
   }

   public boolean tryAttack(PlayerInventorySlot slot, int levelX, int levelY) {
      return this.runClientAttack(slot, levelX, levelY);
   }

   public void tryControllerAttack() {
      this.tryControllerAttack(this.getSelectedItemSlot());
   }

   public boolean tryControllerAttack(PlayerInventorySlot slot) {
      return this.runClientControllerAttack(slot);
   }

   public void runPlayerAction(PacketPlayerAction.PlayerAction action, int seed) {
      switch (action) {
         case USE_HEALTH_POTION:
            for (SlotPriority slotPriority : this.getInv().main.getPriorityList(this.getLevel(), this, 0, this.getInv().main.getSize() - 1, "usehealthpotion")) {
               if (!this.getInv().main.isSlotClear(slotPriority.slot)) {
                  ItemUsed itemUsed = this.getInv()
                     .main
                     .getItemSlot(slotPriority.slot)
                     .useHealthPotion(this.getLevel(), this, seed, this.getInv().main.getItem(slotPriority.slot));
                  this.getInv().main.setItem(slotPriority.slot, itemUsed.item);
                  if (itemUsed.used) {
                     break;
                  }
               }
            }
            break;
         case USE_MANA_POTION:
            for (SlotPriority slotPriority : this.getInv().main.getPriorityList(this.getLevel(), this, 0, this.getInv().main.getSize() - 1, "usemanapotion")) {
               if (!this.getInv().main.isSlotClear(slotPriority.slot)) {
                  ItemUsed itemUsed = this.getInv()
                     .main
                     .getItemSlot(slotPriority.slot)
                     .useManaPotion(this.getLevel(), this, seed, this.getInv().main.getItem(slotPriority.slot));
                  this.getInv().main.setItem(slotPriority.slot, itemUsed.item);
                  if (itemUsed.used) {
                     break;
                  }
               }
            }
            break;
         case EAT_FOOD:
            for (SlotPriority slotPriority : this.getInv().main.getPriorityList(this.getLevel(), this, 0, this.getInv().main.getSize() - 1, "eatfood")) {
               if (!this.getInv().main.isSlotClear(slotPriority.slot)) {
                  ItemUsed itemUsed = this.getInv()
                     .main
                     .getItemSlot(slotPriority.slot)
                     .eatFood(this.getLevel(), this, seed, this.getInv().main.getItem(slotPriority.slot));
                  this.getInv().main.setItem(slotPriority.slot, itemUsed.item);
                  if (itemUsed.used) {
                     break;
                  }
               }
            }
            break;
         case USE_BUFF_POTION:
            for (SlotPriority slotPriority : this.getInv().main.getPriorityList(this.getLevel(), this, 0, this.getInv().main.getSize() - 1, "usebuffpotion")) {
               if (!this.getInv().main.isSlotClear(slotPriority.slot)) {
                  this.getInv()
                     .main
                     .setItem(
                        slotPriority.slot,
                        this.getInv()
                           .main
                           .getItemSlot(slotPriority.slot)
                           .useBuffPotion(this.getLevel(), this, seed, this.getInv().main.getItem(slotPriority.slot))
                           .item
                     );
               }
            }

            if (this.isServerClient()) {
               ServerClient serverClient = this.getServerClient();
               if (serverClient.adventureParty.getBuffPotionPolicy() == AdventureParty.BuffPotionPolicy.ON_HOTKEY) {
                  synchronized (serverClient.adventureParty.MOBS_LOCK) {
                     HumanMob[] mobs = serverClient.adventureParty.getMobs().toArray(new HumanMob[0]);

                     for (HumanMob mob : mobs) {
                        for (int i = 0; i < 100; i++) {
                           InventoryItem usedItem = mob.adventureParty.tryConsumeItem("usebuffpotion");
                           if (usedItem == null) {
                              break;
                           }

                           if (i == 99) {
                              GameLog.warn.println(serverClient.getName() + " party consumed 100 items on buff hotkey???");
                           }
                        }
                     }
                  }
               }
            }
      }
   }

   @Override
   public GameMessage getLocalization() {
      return new StaticMessage(this.playerName);
   }

   @Override
   public boolean canInteract(Mob mob) {
      if (mob.isPlayer) {
         PlayerMob player = (PlayerMob)mob;
         NetworkClient me = this.getNetworkClient();
         NetworkClient them = player.getNetworkClient();
         if (me != null && them != null) {
            return them.relationshipData.isInRelationshipWithPlayer(me) || this.isServer();
         }
      }

      return false;
   }

   @Override
   public void interact(PlayerMob player) {
      super.interact(player);
      if (this.isServerClient() && player.isServerClient()) {
         ServerClient me = this.getServerClient();
         ServerClient them = player.getServerClient();
         if (them.relationshipData.isInRelationshipWithPlayer(me)) {
            if (them.relationshipData.checkPlayerRelationshipValid(them)) {
               them.playerMob.applyKissed(me.slot, true);
            } else {
               them.sendPacket(new PacketClientRelationshipData(them));
            }
         } else {
            them.sendPacket(new PacketClientRelationshipData(them));
         }
      }
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return Localization.translate("romance", "kiss");
   }

   @Override
   public boolean inInteractRange(Mob mob) {
      Point interactPos = this.getInteractPos();
      return mob.getDistance(interactPos.x, interactPos.y) <= 32.0F;
   }

   public void applyKissed(int targetSlot, boolean sendPacket) {
      PlayerMob targetPlayer = null;
      if (this.isServerClient()) {
         ServerClient serverClient = this.getServerClient();
         Server server = serverClient.getServer();
         targetPlayer = server.getPlayer(targetSlot);
         if (sendPacket) {
            server.network.sendToClientsWithEntity(new PacketPlayerKissedPacket(serverClient.slot, targetSlot), this);
         }
      } else if (this.isClientClient()) {
         targetPlayer = this.getClient().getPlayer(targetSlot);
      }

      int animTime = GameRandom.globalRandom.getIntBetween(2500, 3500);
      this.buffManager.addBuff(new ActiveBuff(BuffRegistry.LOVED, this, 1800.0F, null), this.isServer());
      SettlerRomancePersonality.spawnHeartParticles(this, animTime);
      if (targetPlayer != null) {
         targetPlayer.buffManager.addBuff(new ActiveBuff(BuffRegistry.LOVED, targetPlayer, 1800.0F, null), this.isServer());
         SettlerRomancePersonality.spawnHeartParticles(targetPlayer, animTime);
         targetPlayer.startExpression(FormExpressionWheel.Expression.BLUSH, 3000);
      }
   }

   public Stream<Mob> streamPossibleInteractableMobs() {
      ArrayList<Mob> mobs = this.getLevel().entityManager.mobs.getInRegionRangeByTile(this.getTileX(), this.getTileY(), 1);
      ArrayList<PlayerMob> players = this.getLevel().entityManager.players.getInRegionRangeByTile(this.getTileX(), this.getTileY(), 1);
      return Stream.concat(mobs.stream(), players.stream().filter(m -> m != this));
   }

   public Iterable<Mob> iteratePossibleInteractableMobs() {
      return () -> this.streamPossibleInteractableMobs().iterator();
   }

   public InventoryItem getSelectedHotbarItem() {
      return !this.getInv().main.isSlotClear(this.getSelectedSlot()) ? this.getInv().main.getItem(this.getSelectedSlot()) : null;
   }

   public InventoryItem getSelectedItem() {
      return this.getDraggingItem() != null ? this.getDraggingItem() : this.getSelectedHotbarItem();
   }

   public PlayerInventorySlot getSelectedItemSlot() {
      return this.getDraggingItem() != null
         ? new PlayerInventorySlot(this.getInv().drag, 0)
         : new PlayerInventorySlot(this.getInv().main, this.getSelectedSlot());
   }

   public InventoryItem getDraggingItem() {
      return this.getInv().drag.getItem(0);
   }

   public void setDraggingItem(InventoryItem item) {
      this.getInv().drag.setItem(0, item);
   }

   @Override
   public ItemAttackSlot getCurrentSelectedAttackSlot() {
      return new PlayerInventoryItemAttackSlot(this, this.getSelectedItemSlot());
   }

   @Override
   public boolean itemAttackerHoldsItem(InventoryItem item) {
      return false;
   }

   public void dropItem(InventoryItem item) {
      if (this.isClient()) {
         System.err.println("Players cannot drop items client side");
      } else {
         float dx = 0.0F;
         float dy = 0.0F;
         int dir = this.getDir();
         if (dir == 0) {
            dx = 0.0F;
            dy = -1.0F;
         } else if (dir == 1) {
            dx = 1.0F;
            dy = 0.0F;
         } else if (dir == 2) {
            dx = 0.0F;
            dy = 1.0F;
         } else if (dir == 3) {
            dx = -1.0F;
            dy = 0.0F;
         }

         ItemPickupEntity pickupEntity = item.getPickupEntity(this.getLevel(), this.x, this.y, dx * 175.0F, dy * 175.0F);
         if (this.isServerClient()) {
            pickupEntity.authPickupCooldown.put(this.getServerClient().authentication, 2000);
         }

         pickupEntity.height = 20.0F;
         pickupEntity.dh = 40.0F;
         pickupEntity.droppedByPlayer = true;
         this.getLevel().entityManager.pickups.add(pickupEntity);
      }
   }

   public void dropDraggingItem(int amount) {
      if (this.isCreativeMenuExtended() && this.inventoryExtended) {
         ServerClient client = this.getServerClient();
         ContainerSlot draggingSlot = client.getContainer().getClientDraggingSlot();
         int draggingSlotIndex = draggingSlot.getContainerIndex();
         client.getContainer().applyContainerAction(draggingSlotIndex, ContainerAction.QUICK_TRASH);
         client.getContainer().getClientTrashSlot().markDirty();
         draggingSlot.markDirty();
      } else {
         this.inv.dropItem(this.inv.drag, 0, amount);
      }
   }

   public void clientDropItem(PlayerInventory inv, int slot) {
      this.clientDropItem(inv, slot, inv.getAmount(slot));
   }

   public void clientDropItem(PlayerInventory inv, int slot, int amount) {
      if (this.isClient() && !inv.isSlotClear(slot)) {
         this.getLevel().getClient().network.sendPacket(new PacketPlayerDropItem(inv.getInventoryID(), slot, amount));
      }
   }

   @Override
   public void spawnDamageText(int damage, int size, boolean isCrit) {
      this.getLevel()
         .hudManager
         .addElement(
            new DamageText(
               this,
               damage,
               new FontOptions(size).outline().color(isCrit ? new Color(255, 85, 0) : Color.RED),
               isCrit ? GameRandom.globalRandom.getIntBetween(60, 70) : GameRandom.globalRandom.getIntBetween(40, 50)
            )
         );
   }

   public int getPlayerSlot() {
      return this.getUniqueID();
   }

   @Override
   public void playHurtSound() {
      float pitch = GameRandom.globalRandom.getOneOf(0.95F, 1.0F, 1.05F);
      SoundManager.playSound(GameResources.hurt, SoundEffect.effect(this).pitch(pitch));
   }

   @Override
   public float getAttackAnimProgress() {
      float progress = (float)(this.getWorldEntity().getTime() - this.attackTime) / this.attackAnimTime;
      if (progress >= 1.0F) {
         this.forceEndAttack();
      }

      return Math.min(1.0F, progress);
   }

   public boolean runClientInteract(int levelX, int levelY, boolean onlyItemInteract) {
      if (GlobalData.getCurrentState().isRunning() && this.getLevel() != null) {
         if (this.attackHandler != null && !this.attackHandler.isFromInteract()) {
            this.constantInteract = this.attackHandler.getConstantInteract();
            this.attackHandler.onMouseInteracted(levelX, levelY);
            this.getLevel().getClient().network.sendPacket(PacketPlayerAttackHandler.clientInteractMouse(levelX, levelY));
            return false;
         }

         if (this.onCooldown) {
            return false;
         }

         PlayerInventorySlot slot = this.getSelectedItemSlot();
         if (this.attackHandler != null
            && this.attackHandler.isFromInteract()
            && !this.attackHandler.canRunAttack(this.getLevel(), levelX, levelY, this, this.attackingItem, new PlayerInventoryItemAttackSlot(this, slot))) {
            return false;
         }

         InventoryItem item = this.getInv().getItem(slot);
         ItemInteractAction interactItem = item != null && item.item instanceof ItemInteractAction ? (ItemInteractAction)item.item : null;

         for (Mob mob : this.iteratePossibleInteractableMobs()) {
            if (mob.isVisible() && mob.getSelectBox().contains(levelX, levelY)) {
               if (interactItem != null && !this.isItemOnCooldown(item.item) && interactItem.canMobInteract(this.getLevel(), mob, this, item)) {
                  this.runClientItemMobInteract(interactItem, item, levelX, levelY, mob, slot);
                  return true;
               }

               if (!onlyItemInteract && mob.inInteractRange(this) && mob.canInteract(this)) {
                  this.getLevel().getClient().network.sendPacket(new PacketPlayerMobInteract(this.getLevel().getClient().getSlot(), mob.getUniqueID()));
                  this.constantInteract = false;
                  return true;
               }
            }
         }

         if (interactItem != null
            && interactItem.overridesObjectInteract(this.getLevel(), this, item)
            && !this.isItemOnCooldown(item.item)
            && interactItem.canLevelInteract(this.getLevel(), levelX, levelY, this, item)) {
            this.runClientItemLevelInteract(interactItem, item, levelX, levelY, slot);
            return true;
         }

         if (!onlyItemInteract) {
            LevelObject interactObject = GameUtils.getInteractObjectHit(
               this.getLevel(), levelX, levelY, 0, lo -> lo.isInInteractRange(this) && lo.canInteract(this), null
            );
            if (interactObject != null) {
               ObjectInteractEvent event = new ObjectInteractEvent(interactObject.level, interactObject.tileX, interactObject.tileY, this);
               GameEvents.triggerEvent(event);
               if (!event.isPrevented()) {
                  interactObject.interact(this);
                  this.getLevel()
                     .getClient()
                     .network
                     .sendPacket(new PacketObjectInteract(this.getLevel(), this.getLevel().getClient().getSlot(), interactObject.tileX, interactObject.tileY));
                  this.constantInteract = false;
                  return true;
               }
            }
         }

         if (interactItem != null && !this.isItemOnCooldown(item.item) && interactItem.canLevelInteract(this.getLevel(), levelX, levelY, this, item)) {
            this.runClientItemLevelInteract(interactItem, item, levelX, levelY, slot);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public ControllerInteractTarget getControllerInteractTarget(boolean onlyItemInteract, int attackHeight, final GameCamera camera) {
      if (!GlobalData.getCurrentState().isRunning()) {
         return null;
      }

      if (this.onCooldown || this.attackHandler != null) {
         return null;
      }

      if (this.getLevel() == null) {
         return null;
      }

      float dx = ControllerInput.getAimX();
      float dy = ControllerInput.getAimY();
      if (dx == 0.0F && dy == 0.0F) {
         int dir = this.getDir();
         if (dir == 0) {
            dy = -1.0F;
         } else if (dir == 1) {
            dx = 1.0F;
         } else if (dir == 2) {
            dy = 1.0F;
         } else if (dir == 3) {
            dx = -1.0F;
         } else {
            dx = 1.0F;
         }
      }

      float angle = GameMath.getAngle(new java.awt.geom.Point2D.Float(dx, dy));
      int dir = (int)(GameMath.fixAngle(angle + 90.0F + 22.5F) * 8.0F / 360.0F);
      LinkedList<Rectangle> mobInteractBoxes = new LinkedList<>();
      LinkedList<Rectangle> tileInteractBoxes = new LinkedList<>();
      int ibw = 6;
      int ibh = 6;
      switch (dir) {
         case 0:
            mobInteractBoxes.add(new Rectangle(this.getX() - ibw * 32 / 2, this.getY() - ibh * 32, ibw * 32, ibh * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() - ibw / 2, this.getTileY() - ibh, ibw, ibh));
            break;
         case 1:
            mobInteractBoxes.add(new Rectangle(this.getX(), this.getY() - ibw * 32, ibw * 32, ibh * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() + 1, this.getTileY() - ibh, ibw, ibh));
            break;
         case 2:
            mobInteractBoxes.add(new Rectangle(this.getX(), this.getY() - ibw * 32 / 2, ibh * 32, ibw * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() + 1, this.getTileY() - ibw / 2, ibh, ibw));
            break;
         case 3:
            mobInteractBoxes.add(new Rectangle(this.getX(), this.getY(), ibw * 32, ibh * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() + 1, this.getTileY() + 1, ibw, ibh));
            break;
         case 4:
            mobInteractBoxes.add(new Rectangle(this.getX() - ibw * 32 / 2, this.getY(), ibw * 32, ibh * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() + 1 - ibw / 2, this.getTileY() + 1, ibw, ibh));
            break;
         case 5:
            mobInteractBoxes.add(new Rectangle(this.getX() - ibw * 32, this.getY(), ibw * 32, ibh * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() - ibw, this.getTileY() + 1, ibw, ibh));
            break;
         case 6:
            mobInteractBoxes.add(new Rectangle(this.getX() - ibw * 32, this.getY() - ibw * 32 / 2, ibh * 32, ibw * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() - ibw, this.getTileY() - ibw / 2, ibh, ibw));
            break;
         case 7:
            mobInteractBoxes.add(new Rectangle(this.getX() - ibw * 32, this.getY() - ibw * 32, ibw * 32, ibh * 32));
            tileInteractBoxes.add(new Rectangle(this.getTileX() - ibw, this.getTileY() - ibh, ibw, ibh));
      }

      final PlayerInventorySlot slot = this.getSelectedItemSlot();
      final InventoryItem item = this.getInv().getItem(slot);
      final ItemInteractAction interactItem = item != null && item.item instanceof ItemInteractAction ? (ItemInteractAction)item.item : null;
      Mob mount = this.getMount();
      ObjectValue<Mob, ? extends ControllerInteractTarget> mobInteract = this.streamPossibleInteractableMobs()
         .filter(m -> m.isVisible() && m != mount)
         .filter(m -> {
            Rectangle selectionBox = m.getSelectBox();
            return mobInteractBoxes.stream().anyMatch(r -> r.intersects(selectionBox));
         })
         .map(
            m -> {
               if (interactItem != null && !this.isItemOnCooldown(item.item) && interactItem.canMobInteract(this.getLevel(), m, this, item)) {
                  return new ObjectValue<>(
                     m,
                     new ControllerInteractTarget() {
                        @Override
                        public void runInteract() {
                           Rectangle selectionBox = m.getSelectBox();
                           int centerX = selectionBox.x + selectionBox.width / 2;
                           int centerY = selectionBox.y + selectionBox.height / 2;
                           PlayerMob.this.runClientItemMobInteract(interactItem, item, centerX, centerY, m, slot);
                        }

                        @Override
                        public DrawOptions getDrawOptions() {
                           Rectangle selectBox = m.getSelectBox();
                           return HUD.levelBoundOptions(camera, Settings.UI.controllerFocusBoundsColor, true, selectBox);
                        }

                        @Override
                        public void onCurrentlyFocused() {
                           Rectangle selectBox = m.getSelectBox();
                           GameTooltipManager.setTooltipsInteractFocus(
                              InputPosition.fromScenePos(WindowManager.getWindow().getInput(), camera.getDrawX(selectBox.x), camera.getDrawY(selectBox.y))
                           );
                           item.item.onMouseHoverMob(item, camera, PlayerMob.this, m, new GameBlackboard(), false);
                        }
                     }
                  );
               } else {
                  return !onlyItemInteract && m.inInteractRange(this) && m.canInteract(this)
                     ? new ObjectValue<>(
                        m,
                        new ControllerInteractTarget() {
                           @Override
                           public void runInteract() {
                              PlayerMob.this.getLevel()
                                 .getClient()
                                 .network
                                 .sendPacket(new PacketPlayerMobInteract(PlayerMob.this.getLevel().getClient().getSlot(), m.getUniqueID()));
                              PlayerMob.this.constantInteract = false;
                           }

                           @Override
                           public DrawOptions getDrawOptions() {
                              Rectangle selectBox = m.getSelectBox();
                              return HUD.levelBoundOptions(camera, Settings.UI.controllerFocusBoundsColor, true, selectBox);
                           }

                           @Override
                           public void onCurrentlyFocused() {
                              Rectangle selectBox = m.getSelectBox();
                              GameTooltipManager.setTooltipsInteractFocus(
                                 InputPosition.fromScenePos(WindowManager.getWindow().getInput(), camera.getDrawX(selectBox.x), camera.getDrawY(selectBox.y))
                              );
                              m.onMouseHover(camera, PlayerMob.this, new GameBlackboard(), false);
                           }
                        }
                     )
                     : null;
               }
            }
         )
         .filter(Objects::nonNull)
         .min(Comparator.comparingDouble(ov -> ov.object.getDistance(this)))
         .orElse(null);
      ObjectValue<Point, ? extends ControllerInteractTarget> itemInteract = null;
      if (interactItem != null && !this.isItemOnCooldown(item.item)) {
         final ItemControllerInteract controllerInteract = interactItem.getControllerInteract(
            this.getLevel(), this, item, true, dir, mobInteractBoxes, tileInteractBoxes
         );
         if (controllerInteract != null) {
            itemInteract = new ObjectValue<>(new Point(controllerInteract.levelX, controllerInteract.levelY), new ControllerInteractTarget() {
               @Override
               public void runInteract() {
                  if (interactItem.canLevelInteract(PlayerMob.this.getLevel(), controllerInteract.levelX, controllerInteract.levelY, PlayerMob.this, item)) {
                     PlayerMob.this.runClientItemLevelInteract(interactItem, item, controllerInteract.levelX, controllerInteract.levelY, slot);
                  }
               }

               @Override
               public DrawOptions getDrawOptions() {
                  return controllerInteract.getDrawOptions(camera);
               }

               @Override
               public void onCurrentlyFocused() {
                  controllerInteract.onCurrentlyFocused(camera);
               }
            });
         }
      }

      ObjectValue<LevelObject, ? extends ControllerInteractTarget> objectInteract = null;
      if (!onlyItemInteract) {
         objectInteract = tileInteractBoxes.stream()
            .flatMap(r -> {
               LinkedList<LevelObject> objects = new LinkedList<>();

               for (int i = 0; i < r.width; i++) {
                  for (int j = 0; j < r.height; j++) {
                     objects.add(this.getLevel().getLevelObject(r.x + i, r.y + j));
                  }
               }

               return objects.stream();
            })
            .filter(lo -> lo.isInInteractRange(this) && lo.canInteract(this))
            .min(Comparator.comparingDouble(lo -> this.getDistance(lo.tileX * 32 + 16, lo.tileY * 32 + 16)))
            .map(
               lo -> new ObjectValue<>(
                  lo,
                  new ControllerInteractTarget() {
                     @Override
                     public void runInteract() {
                        ObjectInteractEvent event = new ObjectInteractEvent(lo.level, lo.tileX, lo.tileY, PlayerMob.this);
                        GameEvents.triggerEvent(event);
                        if (!event.isPrevented()) {
                           lo.interact(PlayerMob.this);
                           PlayerMob.this.getLevel()
                              .getClient()
                              .network
                              .sendPacket(
                                 new PacketObjectInteract(PlayerMob.this.getLevel(), PlayerMob.this.getLevel().getClient().getSlot(), lo.tileX, lo.tileY)
                              );
                           PlayerMob.this.constantInteract = false;
                        }
                     }

                     @Override
                     public DrawOptions getDrawOptions() {
                        Rectangle selectBox = lo.getMultiTile().getTileRectangle(lo.tileX, lo.tileY);
                        return HUD.tileBoundOptions(camera, Settings.UI.controllerFocusBoundsColor, true, selectBox);
                     }

                     @Override
                     public void onCurrentlyFocused() {
                        Rectangle selectBox = lo.getMultiTile().getTileRectangle(lo.tileX, lo.tileY);
                        GameTooltipManager.setTooltipsInteractFocus(
                           InputPosition.fromScenePos(
                              WindowManager.getWindow().getInput(), camera.getDrawX(selectBox.x * 32), camera.getDrawY(selectBox.y * 32)
                           )
                        );
                        String controlMsg = lo.getInteractTip(PlayerMob.this, false);
                        if (controlMsg != null) {
                           GameTooltipManager.addTooltip(
                              new InputTooltip(Control.MOUSE2, controlMsg, lo.isInInteractRange(PlayerMob.this) ? 1.0F : 0.7F), TooltipLocation.INTERACT_FOCUS
                           );
                        }

                        lo.onMouseHover(camera, PlayerMob.this, false);
                     }
                  }
               )
            )
            .orElse(null);
      }

      ControllerInteractTarget best = null;
      double bestDistance = Double.MAX_VALUE;
      if (mobInteract != null) {
         float distance = this.getDistance(mobInteract.object);
         if (distance < bestDistance) {
            best = mobInteract.value;
            bestDistance = distance;
         }
      }

      if (itemInteract != null) {
         float distance = this.getDistance(itemInteract.object.x, itemInteract.object.y);
         if (distance < bestDistance) {
            best = itemInteract.value;
            bestDistance = distance;
         }
      }

      if (objectInteract != null) {
         float distance = this.getDistance(objectInteract.object.tileX * 32 + 16, objectInteract.object.tileY * 32 + 16);
         if (distance < bestDistance) {
            best = objectInteract.value;
            bestDistance = distance;
         }
      }

      if (best != null) {
         return best;
      }

      if (interactItem != null && !this.isItemOnCooldown(item.item)) {
         final ItemControllerInteract controllerInteract = interactItem.getControllerInteract(
            this.getLevel(), this, item, false, dir, mobInteractBoxes, tileInteractBoxes
         );
         if (controllerInteract != null) {
            return new ControllerInteractTarget() {
               @Override
               public void runInteract() {
                  PlayerMob.this.runClientItemLevelInteract(interactItem, item, controllerInteract.levelX, controllerInteract.levelY, slot);
               }

               @Override
               public DrawOptions getDrawOptions() {
                  return controllerInteract.getDrawOptions(camera);
               }

               @Override
               public void onCurrentlyFocused() {
                  controllerInteract.onCurrentlyFocused(camera);
               }
            };
         }
      }

      return null;
   }

   @Override
   public GNDItemMap runItemAttack(InventoryItem item, int targetX, int targetY, int seed, int animAttack, ItemAttackSlot slot, GNDItemMap attackMap) {
      attackMap = super.runItemAttack(item, targetX, targetY, seed, animAttack, slot, attackMap);
      this.constantAttack = item.item.getConstantUse(item);
      return attackMap;
   }

   @Override
   protected GNDItemMap runItemMobInteract(
      ItemInteractAction interactItem, InventoryItem item, int targetX, int targetY, Mob targetMob, int seed, ItemAttackSlot slot, GNDItemMap interactMap
   ) {
      interactMap = super.runItemMobInteract(interactItem, item, targetX, targetY, targetMob, seed, slot, interactMap);
      this.constantInteract = interactItem.getConstantInteract(item);
      return interactMap;
   }

   @Override
   protected GNDItemMap runItemLevelInteract(
      ItemInteractAction interactItem, InventoryItem item, int targetX, int targetY, int seed, ItemAttackSlot slot, GNDItemMap interactMap
   ) {
      interactMap = super.runItemLevelInteract(interactItem, item, targetX, targetY, seed, slot, interactMap);
      this.constantInteract = interactItem.getConstantInteract(item);
      return interactMap;
   }

   protected void runClientItemAttack(InventoryItem item, int targetX, int targetY, int animAttack, PlayerInventorySlot slot, int seed, GNDItemMap attackMap) {
      this.attackSlot = slot;
      this.runItemAttack(item, targetX, targetY, seed, animAttack, new PlayerInventoryItemAttackSlot(this, slot), attackMap);
      Packet p = new PacketPlayerAttack(this, this.attackSlot, item.item, targetX, targetY, animAttack, seed, attackMap);
      this.getClient().network.sendPacket(p);
   }

   protected void runClientItemAttack(InventoryItem item, int targetX, int targetY, int animAttack, PlayerInventorySlot slot, int seed) {
      this.attackSlot = slot;
      GNDItemMap attackMap = this.runItemAttack(item, targetX, targetY, seed, animAttack, new PlayerInventoryItemAttackSlot(this, slot), null);
      Packet p = new PacketPlayerAttack(this, this.attackSlot, item.item, targetX, targetY, animAttack, seed, attackMap);
      this.getClient().network.sendPacket(p);
   }

   protected void runClientItemMobInteract(
      ItemInteractAction interactItem, InventoryItem item, int targetX, int targetY, Mob targetMob, PlayerInventorySlot slot
   ) {
      this.attackSlot = slot;
      int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
      GNDItemMap interactMap = this.runItemMobInteract(
         interactItem, item, targetX, targetY, targetMob, seed, new PlayerInventoryItemAttackSlot(this, slot), null
      );
      Packet p = new PacketPlayerItemMobInteract(this, this.attackSlot, item.item, targetX, targetY, targetMob, 0, seed, interactMap);
      this.getClient().network.sendPacket(p);
   }

   protected void runClientItemLevelInteract(ItemInteractAction interactItem, InventoryItem item, int targetX, int targetY, PlayerInventorySlot slot) {
      this.attackSlot = slot;
      int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
      GNDItemMap interactMap = this.runItemLevelInteract(interactItem, item, targetX, targetY, seed, new PlayerInventoryItemAttackSlot(this, slot), null);
      Packet p = new PacketPlayerItemInteract(this, this.attackSlot, item.item, targetX, targetY, 0, seed, interactMap);
      this.getClient().network.sendPacket(p);
   }

   public void runServerItemMobInteract(PacketPlayerItemMobInteract packet, Mob mob) {
      this.setPos(packet.playerX, packet.playerY, false);
      InventoryItem item = this.getInv().getItem(packet.getSlot());
      if (item != null && item.item.getID() == packet.itemID && item.item instanceof ItemInteractAction) {
         ItemInteractAction interactItem = (ItemInteractAction)item.item;
         if (this.attackHandler != null) {
            return;
         }

         boolean canAttack = interactItem.canMobInteract(this.getLevel(), mob, this, item);
         if (!canAttack && Settings.strictServerAuthority) {
            return;
         }

         this.attackSlot = packet.getSlot();
         this.runItemMobInteract(interactItem, item, mob, packet.seed, new PlayerInventoryItemAttackSlot(this, this.attackSlot), packet.mapContent);
         this.getServer()
            .network
            .sendToClientsWithAnyRegionExcept(
               new PacketShowItemMobInteract(this, item, packet.attackX, packet.attackY, mob, packet.seed, packet.mapContent),
               this.getRegionPositionsCombined(mob),
               this.getServerClient()
            );
      } else {
         this.getInv().markFullDirty();
      }
   }

   public void runServerItemLevelInteract(PacketPlayerItemInteract packet) {
      this.setPos(packet.playerX, packet.playerY, false);
      InventoryItem item = this.getInv().getItem(packet.getSlot());
      if (item != null && item.item.getID() == packet.itemID && item.item instanceof ItemInteractAction) {
         ItemInteractAction interactItem = (ItemInteractAction)item.item;
         if (this.attackHandler != null) {
            return;
         }

         boolean canAttack = interactItem.canLevelInteract(this.getLevel(), packet.attackX, packet.attackY, this, item);
         if (!canAttack && Settings.strictServerAuthority) {
            return;
         }

         this.attackSlot = packet.getSlot();
         this.runItemLevelInteract(
            interactItem, item, packet.attackX, packet.attackY, packet.seed, new PlayerInventoryItemAttackSlot(this, this.attackSlot), packet.mapContent
         );
         this.getLevel()
            .getServer()
            .network
            .sendToClientsWithEntityExcept(
               new PacketShowItemLevelInteract(this, item, packet.attackX, packet.attackY, packet.seed, packet.mapContent), this, this.getServerClient()
            );
      } else {
         this.getInv().markFullDirty();
      }
   }

   public boolean runClientControllerAttack(PlayerInventorySlot slot) {
      if (!GlobalData.getCurrentState().isRunning()) {
         return false;
      }

      if (this.attackHandler != null && this.attackHandler.isFromInteract()) {
         this.constantInteract = this.attackHandler.getConstantInteract();
         this.attackHandler.onControllerInteracted(ControllerInput.getAimX(), ControllerInput.getAimY());
         this.getLevel()
            .getClient()
            .network
            .sendPacket(PacketPlayerAttackHandler.clientInteractController(ControllerInput.getAimX(), ControllerInput.getAimY()));
         return false;
      }

      if (this.buffManager.getModifier(BuffModifiers.PARALYZED) || this.buffManager.getModifier(BuffModifiers.INTIMIDATED)) {
         return false;
      }

      if (this.objectUser != null && !this.objectUser.canInteract(slot, this)) {
         return false;
      }

      InventoryItem attackItem;
      if (this.isAttacking) {
         attackItem = this.attackingItem;
      } else {
         attackItem = this.getInv().getItem(slot);
      }

      if (attackItem != null) {
         java.awt.geom.Point2D.Float aimDir = this.getControllerAimDir();
         this.lastControllerAimX = aimDir.x;
         this.lastControllerAimY = aimDir.y;
         Point levelPos = attackItem.item.getControllerAttackLevelPos(this.getLevel(), aimDir.x, aimDir.y, this, attackItem);
         return this.runClientAttack(slot, levelPos.x, levelPos.y);
      } else {
         return false;
      }
   }

   public boolean runClientAttack(PlayerInventorySlot slot, int levelX, int levelY) {
      if (!GlobalData.getCurrentState().isRunning()) {
         return false;
      }

      if (this.attackHandler != null && this.attackHandler.isFromInteract()) {
         this.constantInteract = this.attackHandler.getConstantInteract();
         this.attackHandler.onMouseInteracted(levelX, levelY);
         this.getLevel().getClient().network.sendPacket(PacketPlayerAttackHandler.clientInteractMouse(levelX, levelY));
         return false;
      }

      if (this.buffManager.getModifier(BuffModifiers.PARALYZED) || this.buffManager.getModifier(BuffModifiers.INTIMIDATED)) {
         return false;
      }

      if (this.objectUser != null && !this.objectUser.canAttack(slot, this)) {
         return false;
      }

      if (this.isAttacking) {
         if (this.canAnimAttackAgain(this.attackSlot.getItem(this.inv)) && this.getNextAnimAttackCooldown() <= 0L) {
            ToolDamageItem.SmartMineTarget hitTile = null;
            if (this.attackingItem != null
               && this.attackingItem.item instanceof ToolDamageItem
               && (Settings.smartMining || Input.lastInputIsController && !ControllerInput.isCursorVisible())) {
               hitTile = ((ToolDamageItem)this.attackingItem.item).getFirstSmartHitTile(this.getLevel(), this, this.attackingItem, levelX, levelY);
               if (hitTile != null) {
                  levelX = hitTile.x * 32 + 16;
                  levelY = hitTile.y * 32 + 16;
               }
            }

            GNDItemMap attackMap = new GNDItemMap();
            int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
            this.attackingItem.item.setupAttackMapContent(attackMap, this.getLevel(), levelX, levelY, this, seed, this.attackingItem);
            if (hitTile != null && hitTile.tileDamageOption != null) {
               ((ToolDamageItem)this.attackingItem.item).setupAttackMapContentHitTile(attackMap, this.getLevel(), hitTile.tileDamageOption);
            }

            this.runClientItemAttack(this.attackingItem, levelX, levelY, this.animAttack, slot, seed, attackMap);
            if (this.getWorldSettings() != null && this.getWorldSettings().playerHunger()) {
               float hungerUsage = this.attackingItem.item.getHungerUsage(this.attackingItem, this) * attacksHungerModifier;
               if (hungerUsage > 0.0F) {
                  this.useHunger(hungerUsage, false);
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         if (this.onCooldown) {
            return false;
         }

         if (this.attackHandler != null
            && !this.attackHandler.isFromInteract()
            && !this.attackHandler.canRunAttack(this.getLevel(), levelX, levelY, this, this.attackingItem, new PlayerInventoryItemAttackSlot(this, slot))) {
            return false;
         }

         this.attackSlot = slot;
         if (!this.getInv().isSlotClear(this.attackSlot)) {
            this.attackingItem = this.getInv().getItem(this.attackSlot);
         } else {
            this.attackingItem = null;
         }

         this.attackDir = GameMath.normalize(levelX - this.x, levelY - this.y);
         if (this.attackingItem != null) {
            this.constantAttack = this.attackingItem.item.getConstantUse(this.attackingItem);
            if (!this.isItemOnCooldown(this.attackingItem.item)) {
               String canAttack = this.attackingItem.item.canAttack(this.getLevel(), levelX, levelY, this, this.attackingItem);
               if (canAttack == null) {
                  ToolDamageItem.SmartMineTarget hitTile = null;
                  if (this.attackingItem != null
                     && this.attackingItem.item instanceof ToolDamageItem
                     && (Settings.smartMining || Input.lastInputIsController && !ControllerInput.isCursorVisible())) {
                     hitTile = ((ToolDamageItem)this.attackingItem.item).getFirstSmartHitTile(this.getLevel(), this, this.attackingItem, levelX, levelY);
                     if (hitTile != null) {
                        levelX = hitTile.x * 32 + 16;
                        levelY = hitTile.y * 32 + 16;
                     }
                  }

                  GNDItemMap attackMap = new GNDItemMap();
                  int seed = Item.getRandomAttackSeed(GameRandom.globalRandom);
                  this.attackingItem.item.setupAttackMapContent(attackMap, this.getLevel(), levelX, levelY, this, seed, this.attackingItem);
                  if (hitTile != null && hitTile.tileDamageOption != null) {
                     ((ToolDamageItem)this.attackingItem.item).setupAttackMapContentHitTile(attackMap, this.getLevel(), hitTile.tileDamageOption);
                  }

                  this.runClientItemAttack(this.attackingItem, levelX, levelY, 0, slot, seed, attackMap);
                  if (this.getWorldSettings() != null && this.getWorldSettings().playerHunger()) {
                     float hungerUsage = this.attackingItem.item.getHungerUsage(this.attackingItem, this) * attacksHungerModifier;
                     if (hungerUsage > 0.0F) {
                        this.useHunger(hungerUsage, false);
                     }
                  }

                  return true;
               }

               if (!canAttack.isEmpty()) {
                  UniqueFloatText text = new UniqueFloatText(
                     this.getX(), this.getY() - 20, canAttack, new FontOptions(16).outline().color(new Color(200, 100, 100)), "mountfail"
                  ) {
                     @Override
                     public int getAnchorX() {
                        return PlayerMob.this.getX();
                     }

                     @Override
                     public int getAnchorY() {
                        return PlayerMob.this.getY() - 20;
                     }
                  };
                  this.getLevel().hudManager.addElement(text);
               }
            }
         }

         return false;
      }
   }

   public void runServerAttack(PacketPlayerAttack packet) {
      InventoryItem item = this.getInv().getItem(packet.getSlot());
      if (item != null && item.item.getID() == packet.itemID) {
         if (packet.animAttack >= 1
            && packet.animAttack == this.animAttack
            && this.canAnimAttackAgain(this.attackSlot.getItem(this.inv))
            && this.getNextAnimAttackCooldown() <= (Settings.strictServerAuthority ? 50 : 1000)) {
            item = this.getInv().getItem(this.attackSlot);
            this.runItemAttack(
               item, packet.attackX, packet.attackY, packet.seed, this.animAttack, new PlayerInventoryItemAttackSlot(this, this.attackSlot), packet.mapContent
            );
            if (this.getWorldSettings() != null && this.getWorldSettings().playerHunger()) {
               float hungerUsage = item.item.getHungerUsage(item, this) * attacksHungerModifier;
               if (hungerUsage > 0.0F) {
                  this.useHunger(hungerUsage, false);
                  this.sendHungerPacket();
               }
            }

            return;
         }

         if (this.isAttacking) {
            if (this.getNextAttackCooldown() > (Settings.strictServerAuthority ? 40 : 1000)) {
               return;
            }

            this.forceEndAttack();
         }

         if (this.attackHandler != null
            && !this.attackHandler
               .canRunAttack(this.getLevel(), packet.attackX, packet.attackY, this, item, new PlayerInventoryItemAttackSlot(this, packet.getSlot()))) {
            return;
         }

         String canAttack = item.item.canAttack(this.getLevel(), packet.attackX, packet.attackY, this, item);
         if (canAttack != null) {
            item.item.onServerCanAttackFailed(this.getLevel(), packet.attackX, packet.attackY, this, item, canAttack, !Settings.strictServerAuthority);
            if (Settings.strictServerAuthority) {
               return;
            }
         }

         this.attackSlot = packet.getSlot();
         this.runItemAttack(item, packet.attackX, packet.attackY, packet.seed, 0, new PlayerInventoryItemAttackSlot(this, this.attackSlot), packet.mapContent);
         if (this.getWorldSettings() != null && this.getWorldSettings().playerHunger()) {
            float hungerUsage = item.item.getHungerUsage(item, this) * attacksHungerModifier;
            if (hungerUsage > 0.0F) {
               this.useHunger(hungerUsage, false);
               this.sendHungerPacket();
            }
         }

         this.getLevel()
            .getServer()
            .network
            .sendToClientsWithEntityExcept(
               new PacketShowAttack(this, item, packet.attackX, packet.attackY, 0, packet.seed, packet.mapContent), this, this.getServerClient()
            );
      } else {
         this.getInv().markFullDirty();
      }
   }

   @Override
   public void showAttackAndSendAttacker(InventoryItem item, int targetX, int targetY, int animAttack, int seed, GNDItemMap attackMap) {
      this.showItemAttack(item, targetX, targetY, animAttack, seed, attackMap);
      if (this.isServer() && this.isServerClient()) {
         this.getServer()
            .network
            .sendToAllClientsExcept(new PacketShowAttack(this, item, targetX, targetY, animAttack, seed, attackMap), this.getServerClient());
      }
   }

   @Override
   public void doAndSendStopAttackAttacker(boolean endAttackHandler) {
      this.stopAttack(false);
      if (this.isServer() && this.isServerClient()) {
         this.getServer().network.sendToAllClientsExcept(new PacketPlayerStopAttack(this.getPlayerSlot()), this.getServerClient());
      }
   }

   @Override
   public void addAndSendAttackerProjectile(Projectile projectile, Runnable runAfterAdded) {
      this.getLevel().entityManager.projectiles.addHidden(projectile);
      if (runAfterAdded != null) {
         runAfterAdded.run();
      }

      if (this.isServer() && this.isServerClient()) {
         this.getServer().network.sendToClientsWithEntityExcept(new PacketSpawnProjectile(projectile), this, this.getServerClient());
      }
   }

   @Override
   public void addAndSendAttackerLevelEvent(LevelEvent event) {
      this.getLevel().entityManager.events.addHidden(event);
      if (this.isServer() && this.isServerClient()) {
         this.getServer().network.sendToClientsWithEntityExcept(new PacketLevelEvent(event), this, this.getServerClient());
      }
   }

   @Override
   public void sendAttackerPacket(RegionPositionGetter entity, Packet packet) {
      if (this.isServer() && this.isServerClient()) {
         this.getServer().network.sendToClientsWithEntityExcept(packet, entity, this.getServerClient());
      }
   }

   @Override
   protected void doHasKilledTarget(MobWasKilledEvent event) {
      super.doHasKilledTarget(event);
      if (this.isServer() && this.isServerClient()) {
         this.getServerClient().adventureParty.doHasKilledTarget(event);
      }
   }

   public void hasRopedAnimal(Mob ropedAnimal) {
      if (this.isServer() && this.isServerClient()) {
         ServerClient client = this.getServerClient();

         for (HumanMob settler : client.adventureParty.getMobs()) {
            PlayerRomanceManager romanceManager = settler.getRomanceManager(client, false);
            if (romanceManager != null) {
               romanceManager.onAnimalCaptured(client, ropedAnimal);
            }
         }
      }
   }

   public void hasReleasedAnimal(Mob releasedAnimal) {
      if (this.isServer() && this.isServerClient()) {
         ServerClient client = this.getServerClient();

         for (HumanMob settler : client.adventureParty.getMobs()) {
            PlayerRomanceManager romanceManager = settler.getRomanceManager(client, false);
            if (romanceManager != null) {
               romanceManager.onAnimalReleased(client, releasedAnimal);
            }
         }
      }
   }

   public void hasNettedCritter(Mob nettedCritter) {
      if (this.isServer() && this.isServerClient()) {
         ServerClient client = this.getServerClient();

         for (HumanMob settler : client.adventureParty.getMobs()) {
            PlayerRomanceManager romanceManager = settler.getRomanceManager(client, false);
            if (romanceManager != null) {
               romanceManager.onCritterNetted(client, nettedCritter);
            }
         }
      }
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      int respawnTime = attackers.stream().filter(a -> !a.removed()).map(Attacker::getRespawnTime).max(Comparator.comparingInt(Integer::intValue)).orElse(5000);
      this.getServerClient().die(respawnTime);
      GameMessage deathMessage = DeathMessageTable.getDeathMessage(attacker, this.getLocalization());
      this.getLevel().getServer().network.sendToAllClients(new PacketChatMessage(new GameMessageBuilder().append("§6").append(deathMessage)));
      System.out.println(deathMessage.translate());
   }

   @Override
   public GameMessage getAttackerName() {
      return new StaticMessage(this.getDisplayName());
   }

   @Override
   public int getUniqueID() {
      return this.getRealUniqueID();
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameSkin gameSkin = this.look.getGameSkin(false);

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), gameSkin, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   public void setSelectedSlot(int slot) {
      if (slot >= 0 && slot <= 9) {
         if (this.selectedSlot != slot) {
            this.inventoryActionUpdate = true;
         }

         this.selectedSlot = slot;
      } else {
         throw new IllegalArgumentException("Slot must be in range [0 - 9]");
      }
   }

   public int getSelectedSlot() {
      return this.selectedSlot;
   }

   public void setInventoryExtended(boolean extended) {
      if (this.inventoryExtended != extended) {
         this.inventoryActionUpdate = true;
      }

      if (this.getLevel() != null && this.isServer() && this.inventoryExtended && !extended) {
         InventoryItem draggingItem = this.getDraggingItem();
         if (draggingItem != null) {
            if (!draggingItem.isLocked()) {
               this.dropDraggingItem(draggingItem.getAmount());
            } else {
               this.getInv().addItem(draggingItem, true, "addback", null);
               if (draggingItem.getAmount() <= 0) {
                  this.setDraggingItem(null);
               } else {
                  this.dropDraggingItem(draggingItem.getAmount());
               }
            }
         }
      }

      this.inventoryExtended = extended;
   }

   public boolean isInventoryExtended() {
      return this.inventoryExtended;
   }

   public void setCreativeMenuExtended(boolean extended) {
      if (this.getWorldSettings().creativeMode) {
         if (this.creativeMenuExtended != extended) {
            this.inventoryActionUpdate = true;
         }

         this.creativeMenuExtended = extended;
      }
   }

   public boolean isCreativeMenuExtended() {
      return !this.getWorldSettings().creativeMode ? false : this.creativeMenuExtended;
   }

   public boolean shouldUpdateInventoryAction() {
      return this.inventoryActionUpdate;
   }

   public void resetUpdateInventoryAction() {
      this.inventoryActionUpdate = false;
   }

   public void resetInv() {
      if (this.inv == null) {
         this.inv = new PlayerInventoryManager(this);
      } else {
         this.inv.clearInventories();
      }
   }

   public NetworkClient getNetworkClient() {
      return this.networkClient;
   }

   public PlayerInventoryManager getInv() {
      return this.inv;
   }

   @Override
   public int getDir() {
      ObjectUserActive objectUser = this.objectUser;
      if (objectUser != null) {
         int forcedDir = objectUser.getForcedUserDir();
         if (forcedDir >= 0 && forcedDir <= 3) {
            return forcedDir;
         }
      }

      return super.getDir();
   }

   @Override
   public void setUsingObject(ObjectUserActive object) {
      if (this.objectUser != null) {
         this.objectUser.stopUsing();
      }

      this.objectUser = object;
      object.init(this);
   }

   @Override
   public ObjectUserActive getUsingObject() {
      return this.objectUser;
   }

   @Override
   public void clearUsingObject() {
      if (this.objectUser != null) {
         this.objectUser.stopUsing();
      }

      this.objectUser = null;
   }

   public boolean isSittingDown() {
      ObjectUserActive usingObject = this.getUsingObject();
      return usingObject != null ? usingObject.object instanceof ChairObjectInterface : false;
   }

   @Override
   public float getHungerLevel() {
      return this.hungerLevel;
   }

   @Override
   public void useHunger(float amount, boolean forceUse) {
      if (this.hasGodMode()) {
         this.hungerLevel = Math.max(0.2F, this.hungerLevel);
      } else if (forceUse || !FoodConsumableItem.doesMobHaveAnyFoodBuff(this)) {
         this.hungerLevel = Math.max(0.0F, this.hungerLevel - amount);
      }
   }

   @Override
   public void addHunger(float amount) {
      this.hungerLevel = Math.min(this.hungerLevel + amount, 1.0F + amount);
   }

   @Override
   public boolean useFoodItem(FoodConsumableItem item, boolean giveBuff) {
      if (!FoodConsumableItem.canEatFood(this, item)) {
         return false;
      }

      HungerMob.super.useFoodItem(item, giveBuff);
      if (this.isServerClient()) {
         ServerClient client = this.getServerClient();
         client.newStats.food_consumed.add(item);
         JournalChallengeRegistry.handleListeners(client, FoodConsumedJournalChallengeListener.class, challenge -> challenge.onFoodConsumed(client, item));
      }

      this.sendHungerPacket();
      return true;
   }

   public void tickHunger() {
      WorldSettings worldSettings = this.getLevel().getWorldSettings();
      if (worldSettings.playerHunger()) {
         float reduce = 0.0F;
         reduce += 50.0F / (1000.0F * secondsToPassAtFullHunger);
         double distanceRan = (int)this.getDistanceRan() - this.lastDistanceRan;
         if (distanceRan > 0.0) {
            float speedMod = Math.min(1.0F, 35.0F / this.getSpeed());
            reduce = (float)(reduce + distanceRan / distanceToRunAtFullHunger * speedMod);
         }

         this.useHunger(reduce, false);
      }

      this.lastDistanceRan = this.getDistanceRan();
      if (worldSettings.playerHunger()) {
         if (this.hungerLevel <= 0.0F) {
            if (this.isServer() && worldSettings.survivalMode && this.canTakeDamage() && !this.isOnGenericCooldown("starvationdamage")) {
               int maxHealth = this.getMaxHealth();
               float damage = Math.max((float)Math.pow(maxHealth, 0.5) + maxHealth / 20.0F, 20.0F);
               damage /= 2.0F;
               this.isServerHit(new GameDamage(DamageTypeRegistry.TRUE, damage), 0.0F, 0.0F, 0.0F, STARVING_ATTACKER);
               this.startGenericCooldown("starvationdamage", 5000L);
            }

            if (!this.buffManager.hasBuff(BuffRegistry.STARVING_BUFF)) {
               this.buffManager.addBuff(new ActiveBuff(BuffRegistry.STARVING_BUFF, this, 0, null), false);
            }

            this.buffManager.removeBuff(BuffRegistry.HUNGRY_BUFF, false);
         } else if (this.hungerLevel <= 0.1F) {
            if (!this.buffManager.hasBuff(BuffRegistry.HUNGRY_BUFF)) {
               this.buffManager.addBuff(new ActiveBuff(BuffRegistry.HUNGRY_BUFF, this, 0, null), false);
            }

            this.buffManager.removeBuff(BuffRegistry.STARVING_BUFF, false);
         } else {
            this.buffManager.removeBuff(BuffRegistry.HUNGRY_BUFF, false);
            this.buffManager.removeBuff(BuffRegistry.STARVING_BUFF, false);
         }
      } else {
         this.buffManager.removeBuff(BuffRegistry.HUNGRY_BUFF, false);
         this.buffManager.removeBuff(BuffRegistry.STARVING_BUFF, false);
      }
   }

   public ServerClient getServerClient() {
      return (ServerClient)this.networkClient;
   }

   public boolean isServerClient() {
      return this.networkClient instanceof ServerClient;
   }

   public ClientClient getClientClient() {
      return (ClientClient)this.networkClient;
   }

   public boolean isClientClient() {
      return this.networkClient instanceof ClientClient;
   }

   public void sendHungerPacket() {
      if (this.isServer()) {
         this.getLevel().getServer().network.sendToClientsWithEntity(new PacketPlayerHunger(this.getUniqueID(), this.hungerLevel), this);
      }
   }

   @Override
   public int getAvailableArrows(String purpose) {
      return this.getInv().main.getAmount(this.getLevel(), this, Item.Type.ARROW, purpose);
   }

   @Override
   public int getAvailableBullets(String purpose) {
      return this.getInv().main.getAmount(this.getLevel(), this, Item.Type.BULLET, purpose);
   }

   @Override
   public Item getFirstAvailableArrow(String purpose) {
      return this.getInv().main.getFirstItem(this.getLevel(), this, Item.Type.ARROW, purpose);
   }

   @Override
   public Item getFirstAvailableBullet(String purpose) {
      return this.getInv().main.getFirstItem(this.getLevel(), this, Item.Type.BULLET, purpose);
   }

   @Override
   public int getAvailableAmmo(Item[] items, String purpose) {
      int count = 0;

      for (Item item : items) {
         count += this.getInv().main.getAmount(this.getLevel(), this, item, purpose);
      }

      return count;
   }

   @Override
   public Item getFirstAvailableAmmo(Item[] items, String purpose) {
      return this.getInv().main.getFirstItem(this.getLevel(), this, items, purpose);
   }

   @Override
   public AmmoConsumed removeAmmo(Item item, int amount, String purpose) {
      return new AmmoConsumed(this.getInv().main.removeItems(this.getLevel(), this, item, amount, purpose), 1.0F);
   }

   @Override
   public boolean hasValidSummonItem(Item item, CheckSlotType slotType) {
      if (slotType != null) {
         switch (slotType) {
            case HELMET:
               InventoryItem helmetItem = this.getInv().equipment.getSelectedArmorSlot(0).getItem();
               return helmetItem != null && helmetItem.item.getID() == item.getID();
            case CHEST:
               InventoryItem chestItem = this.getInv().equipment.getSelectedArmorSlot(1).getItem();
               return chestItem != null && chestItem.item.getID() == item.getID();
            case FEET:
               InventoryItem feetItem = this.getInv().equipment.getSelectedArmorSlot(2).getItem();
               return feetItem != null && feetItem.item.getID() == item.getID();
            case TRINKETS:
               InventoryItem trinketAbilityItem = this.getInv().equipment.getSelectedEquipmentSlot(0).getItem();
               if (trinketAbilityItem != null && trinketAbilityItem.item.getID() == item.getID()) {
                  return true;
               }

               return this.getInv().equipment.getSelectedTrinketsInventory().streamSlots().anyMatch(slot -> {
                  InventoryItem currentItem = slot.getItem();
                  return currentItem != null && currentItem.item.getID() == item.getID();
               });
            case MOUNT:
               InventoryItem mountItem = this.getInv().equipment.getSelectedEquipmentSlot(0).getItem();
               return mountItem != null && mountItem.item.getID() == item.getID();
         }
      }

      return this.getInv().streamInventorySlots(false, false, false, false).anyMatch(slot -> {
         InventoryItem currentItem = slot.getItem();
         return currentItem != null && currentItem.item.getID() == item.getID();
      });
   }

   @Override
   public float getRegenFlat() {
      return this.getMaxHealth() / 200.0F;
   }

   @Override
   public boolean usesMana() {
      return true;
   }

   public boolean isManaBarVisible() {
      if (this.usesMana()) {
         float mana = this.getMana();
         int maxMana = this.getMaxMana();
         if (mana < maxMana) {
            this.maxManaReached = false;
            return true;
         }

         if (this.timeOfMaxMana + 2000L < this.getWorldEntity().getTime() && this.maxManaReached) {
            return false;
         }

         if (!this.maxManaReached) {
            this.timeOfMaxMana = this.getWorldEntity().getTime();
            this.maxManaReached = true;
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean hasGodMode() {
      WorldSettings worldSettings = this.getWorldSettings();
      return worldSettings == null ? false : worldSettings.creativeMode && this.hasGodModeInCreative;
   }

   public void setGodModeInCreative(boolean value) {
      this.hasGodModeInCreative = value;
      this.getClient().network.sendPacket(new PacketCreativePlayerSettings(this));
      this.refreshCreativeModeBuff();
   }

   public void refreshCreativeModeBuff() {
      if (this.hasGodMode()) {
         this.buffManager.addBuff(new ActiveBuff(BuffRegistry.GODMODE_BUFF, this, 1.0F, null), false);
      } else {
         this.buffManager.removeBuff(BuffRegistry.GODMODE_BUFF, false);
      }
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      super.remove(knockbackX, knockbackY, attacker, isDeath);
      if (this.activeSetBuffAbility != null) {
         this.activeSetBuffAbility.onStopped(this);
         this.activeSetBuffAbility = null;
      }

      if (this.activeTrinketBuffAbility != null) {
         this.activeTrinketBuffAbility.onStopped(this);
         this.activeTrinketBuffAbility = null;
      }

      if (this.activeMountAbility != null) {
         this.activeMountAbility.onStopped(this);
         this.activeMountAbility = null;
      }
   }

   @Override
   public int getMaxHealthUpgrade() {
      return this.healthUpgradeManager == null ? 0 : this.healthUpgradeManager.getCurrentMaxHealthIncrease();
   }

   @Override
   public void giveBaitBack(BaitItem bait) {
      ItemPickupEntity pickup = this.inv.addItemsDropRemaining(new InventoryItem(bait), "addback", this, false, false);
      if (pickup != null) {
         pickup.pickupCooldown = 0;
      }
   }

   @Override
   public void stopFishing() {
      this.stopAttack();
   }

   @Override
   public void showFishingWaitAnimation(FishingRodItem fishingRod, int targetX, int targetY) {
      this.showItemAttack(FishingPoleHolding.setGNDData(new InventoryItem("fishinghold"), fishingRod), targetX, targetY, 0, 0, new GNDItemMap());
   }

   @Override
   public boolean isFishingSwingDone() {
      this.getAttackAnimProgress();
      return !this.isAttacking;
   }

   @Override
   public FishingLootTable getFishingLootTable(FishingSpot spot) {
      ServerClient client = this.getServerClient();
      return client == null ? spot.getBiome().getFishingLootTable(spot) : client.getFishingLoot(spot);
   }

   @Override
   public void restore() {
      super.restore();
      this.equipmentBuffManager.updateArmorBuffs();
      this.equipmentBuffManager.updateCosmeticSetBonus();
      this.equipmentBuffManager.updateTrinketBuffs();
   }

   @Override
   public void onUnloading(Region region) {
      super.onUnloading(region);
      this.inv.closeTempInventories();
   }

   @Override
   public void dispose() {
      super.dispose();
      this.getInv().dispose();
      if (this.activeSetBuffAbility != null) {
         this.activeSetBuffAbility.onStopped(this);
         this.activeSetBuffAbility = null;
      }

      if (this.activeTrinketBuffAbility != null) {
         this.activeTrinketBuffAbility.onStopped(this);
         this.activeTrinketBuffAbility = null;
      }

      if (this.activeMountAbility != null) {
         this.activeMountAbility.onStopped(this);
         this.activeMountAbility = null;
      }
   }
}
