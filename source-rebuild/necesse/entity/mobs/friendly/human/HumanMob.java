package necesse.entity.mobs.friendly.human;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.GameAuth;
import necesse.engine.GameLog;
import necesse.engine.Settings;
import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketHumanWorkUpdate;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.LevelJobRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.registries.SettlerRomancePersonalityRegistry;
import necesse.engine.registries.SettlerThoughtRegistry;
import necesse.engine.registries.VersionMigration;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.save.levelData.InventorySave;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.InvalidLevelIdentifierException;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.OneWorldMigration;
import necesse.engine.world.WorldEntity;
import necesse.engine.world.WorldSettings;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.engine.world.worldData.SettlersWorldData;
import necesse.entity.levelEvent.fishingEvent.FishingEvent;
import necesse.entity.mobs.ActivityDescription;
import necesse.entity.mobs.ActivityDescriptionMob;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.EquipmentBuffManager;
import necesse.entity.mobs.FishingMob;
import necesse.entity.mobs.HungerMob;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobBeforeHitCalculatedEvent;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobInventory;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.entity.mobs.ObjectUserActive;
import necesse.entity.mobs.ObjectUserMob;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.QuestGiver;
import necesse.entity.mobs.QuestMarkerOptions;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ability.CustomMobAbility;
import necesse.entity.mobs.ability.EnumListMobAbility;
import necesse.entity.mobs.ability.IntMobAbility;
import necesse.entity.mobs.ability.MobAbility;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.friendly.ClientInteractMob;
import necesse.entity.mobs.friendly.InteractingClients;
import necesse.entity.mobs.friendly.RopeClearerMob;
import necesse.entity.mobs.friendly.human.humanShop.explorerMission.SettlerMission;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanEquipmentFilterSetting;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanWorkSetting;
import necesse.entity.mobs.friendly.human.humanWorkSetting.HumanWorkSettingRegistry;
import necesse.entity.mobs.itemAttacker.AmmoConsumed;
import necesse.entity.mobs.itemAttacker.AmmoUserMob;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.job.EntityJobWorker;
import necesse.entity.mobs.job.FoundJobGenerator;
import necesse.entity.mobs.job.JobFinder;
import necesse.entity.mobs.job.JobSequence;
import necesse.entity.mobs.job.JobType;
import necesse.entity.mobs.job.JobTypeHandler;
import necesse.entity.mobs.job.JobWorkerChatter;
import necesse.entity.mobs.job.JobWorkerChatterHandler;
import necesse.entity.mobs.job.WorkInventory;
import necesse.entity.mobs.job.activeJob.ActiveJob;
import necesse.entity.mobs.networkField.BooleanNetworkField;
import necesse.entity.mobs.networkField.IntNetworkField;
import necesse.entity.mobs.networkField.LongNetworkField;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.GameColor;
import necesse.gfx.GameResources;
import necesse.gfx.GameSkin;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormProgressBar;
import necesse.gfx.forms.components.FormSettlerHappinessBar;
import necesse.gfx.forms.components.FormSettlerHungerBar;
import necesse.gfx.forms.components.FormSettlerRecreationBar;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.settlement.events.SettlementSettlerActivityDataEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerEquipmentSelfManageChangedEvent;
import necesse.inventory.item.FishingPoleHolding;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemHolding;
import necesse.inventory.item.SwingSpriteAttackItem;
import necesse.inventory.item.WorkSpriteAttackItem;
import necesse.inventory.item.baitItem.BaitItem;
import necesse.inventory.item.placeableItem.consumableItem.food.FoodConsumableItem;
import necesse.inventory.item.placeableItem.fishingRodItem.FishingRodItem;
import necesse.inventory.itemFilter.ItemCategoriesFilter;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootInventoryItem;
import necesse.level.gameObject.ChairObjectInterface;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectUsersObject;
import necesse.level.gameObject.furniture.SettlerBedObject;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.MoodFloatText;
import necesse.level.maps.hudManager.floatText.thoughtBubble.AgreeThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.AngerThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.DisagreeThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.ExcitedThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.ItemThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.LoveThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.MobThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.OtherSettlerThoughtBubble;
import necesse.level.maps.hudManager.floatText.thoughtBubble.ThoughtBubble;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.jobs.AbstractLevelJob;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.NetworkSettlementData;
import necesse.level.maps.levelData.settlementData.SavedSettlerSettings;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;
import necesse.level.maps.levelData.settlementData.SettlementRoom;
import necesse.level.maps.levelData.settlementData.ZoneTester;
import necesse.level.maps.levelData.settlementData.notifications.SettlementNotificationSeverity;
import necesse.level.maps.levelData.settlementData.settler.ActiveSettlerThoughtsManager;
import necesse.level.maps.levelData.settlementData.settler.CommandMob;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.SettlerMob;
import necesse.level.maps.levelData.settlementData.settler.SettlerRecreationExhaustionManager;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtStacks;
import necesse.level.maps.levelData.settlementData.settler.SettlerThoughtsList;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.thoughts.PopulationSettlerThought;
import necesse.level.maps.levelData.settlementData.settler.thoughts.SettlerThought;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.mapData.ClientDiscoveredMap;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;
import necesse.level.maps.regionSystem.Region;
import necesse.level.maps.regionSystem.SubRegion;

public abstract class HumanMob
   extends ItemAttackerMob
   implements HumanSettlerMob,
   CommandMob,
   EntityJobWorker,
   FishingMob,
   HungerMob,
   ObjectUserMob,
   ActivityDescriptionMob,
   MobInventory,
   ClientInteractMob,
   AmmoUserMob,
   RopeClearerMob,
   JobWorkerChatter {
   public static int visitorStayTimeSeconds = 480;
   public static int timeToTravelOut = 240;
   public static int humanPathIterations = 25000;
   public static int defaultJobPathIterations = humanPathIterations;
   public static int secondsToPassAtFullHunger = 1200;
   public static float adventurePartyHungerUsageMod = 2.0F;
   public static int SECONDS_TO_MAX_RECREATION = 120;
   public static int SECONDS_TO_EMPTY_RECREATION = 1800;
   public static int HAPPINESS_QOL_DECREASE_THRESHOLD = 50;
   public static int HAPPINESS_QOL_INCREASE_THRESHOLD = 80;
   public static int SECONDS_TO_DECREASE_QOL = 900;
   public static int SECONDS_TO_INCREASE_QOL = 300;
   public static ArrayList<ModifierValue<?>> QOL_MODIFIERS = new ArrayList<>(
      Arrays.asList(
         new ModifierValue<>(BuffModifiers.WORK_SPEED, 0.05F),
         new ModifierValue<>(BuffModifiers.SPEED, 0.05F),
         new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 0.05F)
      )
   );
   public static boolean debugReloadPersonalities = false;
   public HumanGender gender;
   public boolean customLook;
   public HumanLook look;
   public Point home;
   public int maxSecondsFullHealthRegen = 300;
   public OneWorldNPCVillageData.NPCVillagerData villagerData;
   public int settlerSeed;
   protected String settlerName;
   protected ArrayList<SettlerPersonality> personalities = new ArrayList<>();
   protected boolean morePersonalitiesFromIncursionPerk;
   protected SettlerRomancePersonality romancePersonality;
   public final HumanMob.UpdateRomancePersonalityAbility updateRomancePersonalityAbility;
   protected Point moveInPoint;
   protected Point moveOutPoint;
   private int settlerCheck;
   protected BooleanNetworkField clientHasCommandOrders = this.registerNetworkField(
      new BooleanNetworkField(false) {
         public void onChanged(Boolean value) {
            super.onChanged(value);
            if (HumanMob.this.isServer() && HumanMob.this.isSettler()) {
               ServerSettlementData serverData = HumanMob.this.getSettlerSettlementServerData();
               if (serverData != null) {
                  new SettlementSettlerActivityDataEvent(serverData, Collections.singletonList(HumanMob.this.getUniqueID()))
                     .applyAndSendToAllClients(HumanMob.this.getServer());
               }
            }
         }
      }
   );
   public int resetCommandsBuffer;
   public Mob commandFollowMob;
   public Point commandGuardPoint;
   public boolean commandMoveToGuardPoint;
   public boolean commandMoveToFollowPoint;
   public Mob commandAttackMob;
   public boolean canJoinAdventureParties = true;
   public final AdventurePartyHumanHandler adventureParty = new AdventurePartyHumanHandler(this);
   protected long nextShowMoodTime;
   public final InteractingClients interactingClients = new InteractingClients(this);
   public final HumanMob.ShowSimpleThoughtMobAbility showAgreeThoughtAbility;
   public final HumanMob.ShowSimpleThoughtMobAbility showDisagreeThoughtAbility;
   public final HumanMob.ShowSimpleThoughtMobAbility showExcitedThoughtAbility;
   public final HumanMob.ShowSimpleThoughtMobAbility showAngerThoughtAbility;
   public final HumanMob.ShowSimpleThoughtMobAbility showLoveThoughtAbility;
   public final HumanMob.ShowOtherChatterThoughtMobAbility showOtherChatterThoughtAbility;
   public final HumanMob.ShowItemThoughtMobAbility showItemThoughtAbility;
   public final HumanMob.ShowMobThoughtMobAbility showMobThoughtAbility;
   public final HumanMob.SpawnHeartParticlesMobAbility spawnHeartParticlesAbility;
   protected FormExpressionWheel.Expression activeExpression;
   protected long activeExpressionEndTime;
   public final HumanMob.StartExpressionMobAbility startExpressionAbility;
   public final String settlerStringID;
   protected int settlementUniqueID;
   protected Point earlyAccessSettlementIsland;
   protected ServerSettlementData cachedServerSettlementData;
   protected NetworkSettlementData cachedNetworkSettlementData;
   public LevelSettler levelSettler;
   protected int settlerQualityOfLife = 0;
   protected float settlerQualityOfLifeChangeBuffer;
   protected int settlerHappiness = 0;
   protected float settlerRecreationLevel = 0.75F;
   protected float lastSettlerRecreationSyncLevel = this.settlerRecreationLevel;
   protected SettlerRecreationExhaustionManager recreationExhaustionManager = new SettlerRecreationExhaustionManager(this);
   protected ActiveSettlerThoughtsManager activeThoughts = new ActiveSettlerThoughtsManager(this);
   protected ItemCategoriesFilter loadedDietFilter = null;
   public FoodConsumableItem lastFoodEaten;
   public LinkedList<Integer> recentFoodItemIDsEaten = new LinkedList<>();
   public float hungerLevel = 0.2F;
   protected long clearRopedTime;
   protected HumanMob.DownedState downedState = null;
   protected Point earlyAccessRecruitReservedIsland;
   protected int recruitReservedSettlementUniqueID;
   protected long recruitReservedTimeout;
   protected InventoryItem customHoldItem;
   protected long customHoldItemTimeout;
   private long lastWorkAnimationRequest = -1L;
   public final CustomMobAbility itemAttackAbility;
   public final CustomMobAbility itemSwingSpriteAbility;
   public final CustomMobAbility itemWorkSpriteAbility;
   public final CustomMobAbility itemHoldSpriteAbility;
   public final BooleanMobAbility playConsumeSound;
   public final EnumListMobAbility<MoodFloatText.Moods> showMoods;
   public final IntMobAbility setDanceAnimationTimeoutAbility;
   protected long danceAnimationTimeout;
   public final HumanWorkSettingRegistry workSettings;
   public final CustomMobAbility workSettingAction;
   public long searchEquipmentCooldown;
   public BooleanNetworkField selfManageEquipment = this.registerNetworkField(
      new BooleanNetworkField(true) {
         public void onChanged(Boolean value) {
            super.onChanged(value);
            if (HumanMob.this.isSettler() && HumanMob.this.levelSettler != null) {
               new SettlementSettlerEquipmentSelfManageChangedEvent(HumanMob.this.levelSettler)
                  .applyAndSendToAllClients(HumanMob.this.levelSettler.data.getServer());
            }
         }
      }
   );
   public SavedSettlerSettings savedSettlerSettings;
   public Inventory equipmentInventory = new Inventory(7) {
      @Override
      public void updateSlot(int slot) {
         super.updateSlot(slot);
         if (slot < 3) {
            HumanMob.this.equipmentBuffManager.updateArmorBuffs();
            HumanMob.this.updateStats(true);
         } else if (slot < 6) {
            HumanMob.this.equipmentBuffManager.updateCosmeticSetBonus();
         }

         Level level = HumanMob.this.getLevel();
         if (level != null && level.isLoadingComplete() && HumanMob.this.isServer() && HumanMob.this.isSettler()) {
            ServerSettlementData data = HumanMob.this.getSettlerSettlementServerData();
            if (data != null) {
               InventoryRange range;
               if (slot >= 6) {
                  range = null;
               } else if (slot < 3) {
                  range = new InventoryRange(this, 0, 2);
               } else {
                  range = new InventoryRange(this, 3, 5);
               }

               data.onSettlerEquipmentChanged(HumanMob.this.getUniqueID(), range, slot, range != null && slot >= 3);

               for (SettlerPersonality personality : HumanMob.this.personalities) {
                  personality.onEquipmentChanged(range, slot, range != null && slot >= 3);
               }
            }
         }
      }

      @Override
      public boolean canBeUsedForCrafting() {
         return false;
      }
   };
   public EquipmentBuffManager equipmentBuffManager = new EquipmentBuffManager(this) {
      @Override
      public InventoryItem getArmorItem(int slot) {
         return HumanMob.this.equipmentInventory.getItem(slot);
      }

      @Override
      public InventoryItem getCosmeticItem(int slot) {
         return HumanMob.this.equipmentInventory.getItem(3 + slot);
      }

      @Override
      public ArrayList<InventoryItem> getTrinketItems() {
         return new ArrayList<>();
      }
   };
   public ArrayList<InventoryItem> workInventory = new ArrayList<>();
   public ActivityDescription currentActivity;
   public int recreationStrikeCooldownBuffer;
   public int hungerStrikeCooldownBuffer;
   public float leaveBuffer;
   public float leaveBufferWarnings;
   public BooleanNetworkField isOnStrike = this.registerNetworkField(new BooleanNetworkField(false) {
      public void onChanged(Boolean value) {
         super.onChanged(value);
         if (!value) {
            HumanMob.this.endStrikeBuffer = 0;
         }
      }
   });
   public int endStrikeBuffer;
   public JobTypeHandler jobTypeHandler = new JobTypeHandler();
   public long nextMissionWorldTime;
   protected boolean cancelJob;
   protected boolean allowFullInventoryMood;
   private boolean sendWorkUpdatePacket;
   protected int nonSettlerHealth;
   protected int settlerHealth;
   protected Point earlyAccessVisitorIsland;
   protected int visitorSettlementUniqueID;
   protected ServerSettlementData cachedVisitorServerSettlementData;
   protected NetworkSettlementData cachedVisitorNetworkSettlementData;
   protected long travelOutTime;
   protected SettlerMission currentMission;
   public boolean completedMission;
   public boolean missionFailed;
   public GameMessage missionFailedMessage;
   public ObjectUserActive objectUser;
   protected Point loadedObjectUserTile;
   public BooleanNetworkField hideOnLowHealth = this.registerNetworkField(new BooleanNetworkField(true));
   public IntNetworkField team = this.registerNetworkField(new IntNetworkField(-10) {
      public void onChanged(Integer value) {
         super.onChanged(value);
         HumanMob.this.setTeam(value);
      }
   });
   public LongNetworkField owner = this.registerNetworkField(new LongNetworkField(-1L) {
      public void onChanged(Long value) {
         super.onChanged(value);
      }
   });
   public boolean isHiding;
   public static String[] elderNames = new String[]{
      "Diligo", "Haydex", "Kamikaze", "Subject", "Vireyar", "Rainfury", "Tsukuss", "Fair", "Stormes", "Jonas", "Rune", "Bo", "Jonathan", "Oliver", "Christian"
   };
   public static String[] maleNames = new String[]{
      "Aaron",
      "Adam",
      "Alastair",
      "Albert",
      "Alexander",
      "Alfred",
      "Alton",
      "Andrew",
      "Archie",
      "Arthur",
      "Basil",
      "Ben",
      "Benny",
      "Billy",
      "Blake",
      "Brandon",
      "Brayden",
      "Brent",
      "Bruce",
      "Cain",
      "Calvin",
      "Carl",
      "Chad",
      "Charles",
      "Chris",
      "Cody",
      "Dale",
      "Darren",
      "Dave",
      "David",
      "Dean",
      "Dennis",
      "Devon",
      "Dexter",
      "Dustin",
      "Elvis",
      "Eric",
      "Francis",
      "Garrett",
      "Gavin",
      "Glen",
      "Harrison",
      "Henry",
      "Issac",
      "Jake",
      "James",
      "Jared",
      "Jason",
      "Jayden",
      "Jeff",
      "Jordan",
      "Josh",
      "Justin",
      "Kaine",
      "Karl",
      "Ken",
      "Kent",
      "King",
      "Kyle",
      "Larry",
      "Lloyd",
      "Marc",
      "Marco",
      "Mason",
      "Matthew",
      "Michael",
      "Mick",
      "Nick",
      "Oliver",
      "Olly",
      "Patrick",
      "Paul",
      "Peter",
      "Phil",
      "Philip",
      "Ricardo",
      "Ricky",
      "Rob",
      "Roger",
      "Samuel",
      "Scott",
      "Sean",
      "Seth",
      "Shane",
      "Shawn",
      "Sheldon",
      "Stan",
      "Steve",
      "Tim",
      "Tobias",
      "Tom",
      "Tony",
      "Troy",
      "Warren",
      "Will",
      "Zach"
   };
   public static String[] femaleNames = new String[]{
      "Abigail",
      "Abitha",
      "Alexis",
      "Alicia",
      "Amity",
      "Angel",
      "Anne",
      "Aphra",
      "Aurinda",
      "Azuba",
      "Betsy",
      "Bonny",
      "Camille",
      "Candace",
      "Carmen",
      "Carolyn",
      "Christine",
      "Cess",
      "Charity",
      "Charlotte",
      "Christine",
      "Claire",
      "Clara",
      "Connie",
      "Cornelia",
      "Cristi",
      "Denise",
      "Diana",
      "Diane",
      "Edith",
      "Eleanor",
      "Electra",
      "Elizabeth",
      "Emeline",
      "Emma",
      "Esther",
      "Faith",
      "Fidelity",
      "Frances",
      "Francine",
      "Georgine",
      "Gloria",
      "Grace",
      "Harriet",
      "Hazel",
      "Helen",
      "Henrietta",
      "Hepzibah",
      "Hester",
      "Isabella",
      "Jane",
      "Jess",
      "Joy",
      "Joyce",
      "Judith",
      "Kathrine",
      "Kaz",
      "Kristine",
      "Laura",
      "Leona",
      "Lisha",
      "Lucy",
      "Lydia",
      "Maggie",
      "Martha",
      "Martina",
      "Mary",
      "Megan",
      "Melissa",
      "Mellie",
      "Mercy",
      "Natalie",
      "Ness",
      "Nina",
      "Penny",
      "Petra",
      "Phila",
      "Phoebe",
      "Rebekah",
      "Rosanna",
      "Sara",
      "Shirley",
      "Sophia",
      "Sue",
      "Susanna",
      "Tabitha",
      "Tamara",
      "Tess",
      "Tiara",
      "Violet",
      "Virginia",
      "Winnie"
   };
   public static String[] neutralNames = new String[]{
      "Alex", "Ash", "Bobby", "Casey", "Darcy", "Dylan", "Frankie", "George", "Glenn", "Jesse", "Leigh", "Lindsay", "Robin", "Ryan", "Sam"
   };
   public static String[] lastNames = new String[]{
      "Smith",
      "Johnson",
      "Williams",
      "Brown",
      "Jones",
      "Miller",
      "Davis",
      "Garcia",
      "Rodriguez",
      "Wilson",
      "Martinez",
      "Anderson",
      "Taylor",
      "Thomas",
      "Hernandez",
      "Moore",
      "Martin",
      "Jackson",
      "Thompson",
      "White",
      "Lopez",
      "Lee",
      "Gonzalez",
      "Harris",
      "Clark",
      "Lewis",
      "Robinson",
      "Walker",
      "Perez",
      "Hall",
      "Young",
      "Allen",
      "Sanchez",
      "Wright",
      "King",
      "Scott",
      "Green",
      "Baker",
      "Adams",
      "Nelson",
      "Hill",
      "Ramirez",
      "Campbell",
      "Mitchell",
      "Roberts",
      "Carter",
      "Phillips",
      "Evans",
      "Turner",
      "Torres",
      "Parker",
      "Collins",
      "Edwards",
      "Stewart",
      "Flores",
      "Morris",
      "Nguyen",
      "Murphy",
      "Rivera",
      "Cook",
      "Rogers",
      "Morgan",
      "Peterson",
      "Cooper",
      "Reed",
      "Bailey",
      "Bell",
      "Gomez",
      "Kelly",
      "Howard",
      "Ward",
      "Cox",
      "Diaz",
      "Richardson",
      "Wood",
      "Watson",
      "Brooks",
      "Bennett",
      "Gray",
      "James",
      "Reyes",
      "Cruz",
      "Hughes",
      "Price",
      "Myers",
      "Long",
      "Foster",
      "Sanders",
      "Ross",
      "Morales",
      "Powell",
      "Sullivan",
      "Russell",
      "Ortiz",
      "Jenkins",
      "Gutierrez",
      "Perry",
      "Butler",
      "Barnes",
      "Fisher"
   };

   public HumanMob(int nonSettlerHealth, int settlerHealth, String settlerStringID) {
      super(nonSettlerHealth);
      this.nonSettlerHealth = nonSettlerHealth;
      this.settlerHealth = settlerHealth;
      this.settlerStringID = settlerStringID;
      this.isHostile = false;
      this.look = new HumanLook();
      this.attackAnimTime = this.attackCooldown;
      this.setSpeed(25.0F);
      this.setFriction(3.0F);
      this.setArmor(15);
      this.isUsingItemsForArmorAndHealth = true;
      this.setTeam(-10);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-14, -41, 28, 48);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -2;
      this.swimSinkOffset = -4;
      this.updateRomancePersonalityAbility = this.registerAbility(new HumanMob.UpdateRomancePersonalityAbility());
      this.showAgreeThoughtAbility = this.registerAbility(new HumanMob.ShowSimpleThoughtMobAbility(animTime -> new AgreeThoughtBubble(this, animTime)));
      this.showDisagreeThoughtAbility = this.registerAbility(new HumanMob.ShowSimpleThoughtMobAbility(animTime -> new DisagreeThoughtBubble(this, animTime)));
      this.showExcitedThoughtAbility = this.registerAbility(new HumanMob.ShowSimpleThoughtMobAbility(animTime -> new ExcitedThoughtBubble(this, animTime)));
      this.showAngerThoughtAbility = this.registerAbility(new HumanMob.ShowSimpleThoughtMobAbility(animTime -> new AngerThoughtBubble(this, animTime)));
      this.showLoveThoughtAbility = this.registerAbility(new HumanMob.ShowSimpleThoughtMobAbility(animTime -> new LoveThoughtBubble(this, animTime)));
      this.showOtherChatterThoughtAbility = this.registerAbility(new HumanMob.ShowOtherChatterThoughtMobAbility());
      this.showItemThoughtAbility = this.registerAbility(new HumanMob.ShowItemThoughtMobAbility());
      this.showMobThoughtAbility = this.registerAbility(new HumanMob.ShowMobThoughtMobAbility());
      this.spawnHeartParticlesAbility = this.registerAbility(new HumanMob.SpawnHeartParticlesMobAbility());
      this.startExpressionAbility = this.registerAbility(new HumanMob.StartExpressionMobAbility());
      this.workSettings = new HumanWorkSettingRegistry();
      this.workSettingAction = this.registerAbility(new CustomMobAbility() {
         @Override
         protected void run(Packet content) {
            PacketReader reader = new PacketReader(content);
            int id = reader.getNextShortUnsigned();
            HumanWorkSetting<?> setting = HumanMob.this.workSettings.getSetting(id);
            if (setting != null) {
               setting.runAction(reader.getNextContentPacket());
            }
         }
      });
      this.itemAttackAbility = this.registerAbility(new CustomMobAbility() {
         @Override
         protected void run(Packet content) {
            PacketReader reader = new PacketReader(content);
            int x = reader.getNextInt();
            int y = reader.getNextInt();
            InventoryItem invItem;
            if (reader.getNextBoolean()) {
               invItem = InventoryItem.fromContentPacket(reader);
               if (invItem == null) {
                  return;
               }
            } else {
               Item item = ItemRegistry.getItem(reader.getNextShortUnsigned());
               if (item == null) {
                  return;
               }

               invItem = new InventoryItem(item);
            }

            int animTime = reader.getNextInt();
            int cooldown = reader.getNextInt();
            HumanMob.this.attackingItem = invItem;
            HumanMob.this.showItemAttack(invItem, x, y, 0, HumanMob.this.attackSeed, null);
            HumanMob.this.attackAnimTime = animTime;
            HumanMob.this.attackCooldown = Math.max(animTime, cooldown);
            HumanMob.this.lastWorkAnimationRequest = -1L;
         }
      });
      this.itemSwingSpriteAbility = this.registerAbility(new CustomMobAbility() {
         @Override
         protected void run(Packet content) {
            PacketReader reader = new PacketReader(content);
            int x = reader.getNextInt();
            int y = reader.getNextInt();
            int itemID;
            if (reader.getNextBoolean()) {
               itemID = reader.getNextShortUnsigned();
            } else {
               itemID = -1;
            }

            int animTime = reader.getNextInt();
            boolean inverted = reader.getNextBoolean();
            InventoryItem invItem = SwingSpriteAttackItem.setup(new InventoryItem("swingspriteattack"), itemID, inverted);
            HumanMob.this.showItemAttack(invItem, x, y, 0, HumanMob.this.attackSeed, null);
            HumanMob.this.attackAnimTime = animTime;
            HumanMob.this.lastWorkAnimationRequest = -1L;
         }
      });
      this.itemWorkSpriteAbility = this.registerAbility(new CustomMobAbility() {
         @Override
         protected void run(Packet content) {
            PacketReader reader = new PacketReader(content);
            int x = reader.getNextInt();
            int y = reader.getNextInt();
            int itemID;
            if (reader.getNextBoolean()) {
               itemID = reader.getNextShortUnsigned();
            } else {
               itemID = -1;
            }

            int animTime = reader.getNextInt();
            long lastAttackTime = HumanMob.this.attackTime;
            InventoryItem invItem = WorkSpriteAttackItem.setup(new InventoryItem("workspriteattack"), itemID, false);
            HumanMob.this.showItemAttack(invItem, x, y, 0, HumanMob.this.attackSeed, null);
            HumanMob.this.attackAnimTime = animTime;
            if (!HumanMob.this.isAttacking) {
               HumanMob.this.attackTime = lastAttackTime;
            }

            HumanMob.this.lastWorkAnimationRequest = HumanMob.this.getWorldEntity().getLocalTime();
         }
      });
      this.itemHoldSpriteAbility = this.registerAbility(new CustomMobAbility() {
         @Override
         protected void run(Packet content) {
            PacketReader reader = new PacketReader(content);
            if (reader.getNextBoolean()) {
               InventoryItem holdItem = new InventoryItem("itemhold");
               int itemID = reader.getNextShortUnsigned();
               ItemHolding.setGNDData(holdItem, new InventoryItem(ItemRegistry.getItem(itemID)));
               HumanMob.this.customHoldItem = holdItem;
               HumanMob.this.customHoldItemTimeout = HumanMob.this.getLocalTime() + reader.getNextInt();
            } else {
               HumanMob.this.customHoldItem = null;
            }
         }
      });
      this.playConsumeSound = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            if (HumanMob.this.isClient()) {
               SoundManager.playSound(value ? GameResources.drink : GameResources.eat, SoundEffect.effect(HumanMob.this));
            }
         }
      });
      this.showMoods = this.registerAbility(new EnumListMobAbility<MoodFloatText.Moods>(MoodFloatText.Moods.class) {
         @Override
         protected void run(List<MoodFloatText.Moods> list) {
            if (!HumanMob.this.isServer()) {
               HumanMob.this.getLevel().hudManager.addElement(new MoodFloatText(HumanMob.this, 5000, list));
            }
         }
      });
      this.setDanceAnimationTimeoutAbility = this.registerAbility(new IntMobAbility() {
         @Override
         protected void run(int value) {
            HumanMob.this.danceAnimationTimeout = HumanMob.this.getLocalTime() + value;
         }
      });
      this.currentActivity = new ActivityDescription(this);
      LevelJobRegistry.addHandlers(this, this.jobTypeHandler);
      this.workSettings.registerSetting(new HumanEquipmentFilterSetting(this));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.home != null) {
         save.addPoint("home", this.home);
      }

      save.addBoolean("hideInsideOnLowHealth", this.hideOnLowHealth.get());
      if (this.commandGuardPoint != null) {
         save.addBoolean("commandMoveToGuardPoint", this.commandMoveToGuardPoint);
         save.addPoint("commandGuardPoint", this.commandGuardPoint);
      }

      if (this.commandFollowMob != null) {
         save.addInt("commandFollowMob", this.commandFollowMob.getUniqueID());
      }

      if (this.commandAttackMob != null) {
         save.addInt("commandAttackMob", this.commandAttackMob.getUniqueID());
      }

      if (this.resetCommandsBuffer > 0) {
         save.addInt("resetCommandsBuffer", this.resetCommandsBuffer);
      }

      this.adventureParty.addSaveData("adventurePartyAuth", save);
      save.addInt("settlerSeed", this.settlerSeed);
      if (this.customLook) {
         SaveData customLookData = new SaveData("customLook");
         this.look.addSaveData(customLookData);
         save.addSaveData(customLookData);
      }

      SaveData activeThoughtsSave = new SaveData("activeThoughts");
      this.activeThoughts.addSaveData(activeThoughtsSave);
      if (!activeThoughtsSave.isEmpty()) {
         save.addSaveData(activeThoughtsSave);
      }

      if (this.settlementUniqueID != 0) {
         save.addInt("settlementUniqueID", this.settlementUniqueID);
      }

      save.addInt("visitorSettlementUniqueID", this.visitorSettlementUniqueID);
      if (this.visitorSettlementUniqueID != 0 || this.earlyAccessVisitorIsland != null) {
         save.addLong("travelOutTime", this.travelOutTime);
      }

      if (this.savedSettlerSettings != null) {
         SaveData savedSettlerSave = new SaveData("savedSettlerSettings");
         this.savedSettlerSettings.addSaveData(savedSettlerSave);
         save.addSaveData(savedSettlerSave);
      }

      save.addFloat("settlerRecreationLevel", this.settlerRecreationLevel);
      this.recreationExhaustionManager.addSaveData(save);
      save.addFloat("hungerLevel", this.hungerLevel);
      if (this.lastFoodEaten != null) {
         save.addUnsafeString("lastFoodEaten", this.lastFoodEaten.getStringID());
      }

      if (!this.recentFoodItemIDsEaten.isEmpty()) {
         String[] recentFoodItemStringIDs = this.recentFoodItemIDsEaten
            .stream()
            .map(ItemRegistry::getItemStringID)
            .filter(Objects::nonNull)
            .toArray(String[]::new);
         save.addStringArray("recentFoodItemsEaten", recentFoodItemStringIDs);
      }

      if (this.downedState != null) {
         save.addEnum("downedState", this.downedState);
      }

      if (this.recruitReservedSettlementUniqueID != 0 || this.earlyAccessRecruitReservedIsland != null) {
         save.addInt("recruitReservedSettlementUniqueID", this.recruitReservedSettlementUniqueID);
         save.addLong("recruitReservedTimeout", this.recruitReservedTimeout);
      }

      if (this.moveInPoint != null) {
         save.addPoint("moveInPoint", this.moveInPoint);
      }

      if (this.moveOutPoint != null) {
         save.addPoint("moveOutPoint", this.moveOutPoint);
      }

      if (this.objectUser != null) {
         save.addPoint("objectUser", new Point(this.objectUser.tileX, this.objectUser.tileY));
      }

      save.addSafeString("settlerName", this.settlerName);
      if (!this.personalities.isEmpty()) {
         SaveData personalitiesSave = new SaveData("personalities");

         for (SettlerPersonality personality : this.personalities) {
            SaveData personalitySave = new SaveData(personality.getStringID());
            personality.addSaveData(personalitySave);
            personalitiesSave.addSaveData(personalitySave);
         }

         if (!personalitiesSave.isEmpty()) {
            save.addSaveData(personalitiesSave);
         }
      }

      if (this.morePersonalitiesFromIncursionPerk) {
         save.addBoolean("morePersonalitiesFromIncursionPerk", this.morePersonalitiesFromIncursionPerk);
      }

      if (this.romancePersonality != null) {
         SaveData romancePersonalitySave = new SaveData("romancePersonality");
         romancePersonalitySave.addSafeString("romanceStringID", this.romancePersonality.getStringID());
         this.romancePersonality.addSaveData(romancePersonalitySave);
         save.addSaveData(romancePersonalitySave);
      }

      if (this.isSettler()) {
         save.addInt("settlerQualityOfLife", this.settlerQualityOfLife);
         save.addFloat("settlerQualityOfLifeChangeBuffer", this.settlerQualityOfLifeChangeBuffer);
      }

      save.addInt("recreationStrikeCooldownBuffer", this.recreationStrikeCooldownBuffer);
      save.addInt("hungerStrikeCooldownBuffer", this.hungerStrikeCooldownBuffer);
      save.addFloat("leaveBuffer", this.leaveBuffer);
      save.addBoolean("isOnStrike", this.isOnStrike.get());
      if (this.endStrikeBuffer > 0) {
         save.addInt("endStrikeBuffer", this.endStrikeBuffer);
      }

      if (this.nextMissionWorldTime != 0L) {
         save.addLong("nextMissionWorldTime", this.nextMissionWorldTime);
      }

      if (this.jobTypeHandler.lastPerformedJobID != -1) {
         String jobStringID = LevelJobRegistry.getJobStringID(this.jobTypeHandler.lastPerformedJobID);
         if (jobStringID != null) {
            save.addSafeString("lastPerformedJobID", jobStringID);
         }
      }

      if (this.jobTypeHandler.prioritizeNextJobID != -1) {
         String jobStringID = LevelJobRegistry.getJobStringID(this.jobTypeHandler.prioritizeNextJobID);
         if (jobStringID != null) {
            save.addSafeString("prioritizeNextJobID", jobStringID);
         }
      }

      SaveData workPriorities = new SaveData("workPriorities");

      for (JobTypeHandler.TypePriority typePriority : this.jobTypeHandler.getTypePriorities()) {
         if (typePriority.priority != 0 || typePriority.disabledByPlayer) {
            SaveData workPriority = new SaveData(typePriority.type.getStringID());
            typePriority.addSaveData(workPriority);
            workPriorities.addSaveData(workPriority);
         }
      }

      if (!workPriorities.isEmpty()) {
         save.addSaveData(workPriorities);
      }

      if (!this.workInventory.isEmpty()) {
         SaveData workInventoryData = new SaveData("workInventory");

         for (InventoryItem item : this.workInventory) {
            SaveData itemSave = new SaveData("item");
            item.addSaveData(itemSave);
            workInventoryData.addSaveData(itemSave);
         }

         save.addSaveData(workInventoryData);
      }

      if (this.currentMission != null) {
         SaveData missionData = new SaveData("MISSION");
         missionData.addUnsafeString("stringID", this.currentMission.getStringID());
         this.currentMission.addSaveData(this, missionData);
         save.addSaveData(missionData);
      }

      save.addBoolean("completedMission", this.completedMission);
      save.addBoolean("missionFailed", this.missionFailed);
      if (this.missionFailed && this.missionFailedMessage != null) {
         save.addSaveData(this.missionFailedMessage.getSaveData("missionFailedMessage"));
      }

      save.addBoolean("selfManageEquipment", this.selfManageEquipment.get());
      save.addSaveData(InventorySave.getSave(this.equipmentInventory, "equipment"));
      if (this.villagerData != null) {
         save.addSaveData(this.villagerData.getSaveData("VILLAGER_DATA"));
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.home = save.getPoint("home", this.home, false);
      this.hideOnLowHealth.set(save.getBoolean("hideInsideOnLowHealth", this.hideOnLowHealth.get(), false));
      this.commandMoveToGuardPoint = save.getBoolean("commandMoveToGuardPoint", this.commandMoveToGuardPoint, false);
      this.commandGuardPoint = save.getPoint("commandGuardPoint", this.commandGuardPoint, false);
      int commandFollowMob = save.getInt("commandFollowMob", -1, false);
      if (commandFollowMob != -1) {
         this.runOnNextServerTick.add(() -> this.commandFollowMob = GameUtils.getLevelMob(commandFollowMob, this.getLevel()));
      }

      int commandAttackMob = save.getInt("commandAttackMob", -1, false);
      if (commandAttackMob != -1) {
         this.runOnNextServerTick.add(() -> this.commandAttackMob = GameUtils.getLevelMob(commandAttackMob, this.getLevel()));
      }

      this.resetCommandsBuffer = save.getInt("resetCommandsBuffer", this.resetCommandsBuffer, false);
      this.adventureParty.applyLoadData("adventurePartyAuth", save);
      this.setSettlerSeed(save.getInt("settlerSeed", GameRandom.globalRandom.nextInt(), false), false);
      LoadData customLookData = save.getFirstLoadDataByName("customLook");
      if (customLookData != null) {
         this.customLook = true;
         this.look = new HumanLook();
         this.look.applyLoadData(customLookData);
      }

      LoadData activeThoughtsSave = save.getFirstLoadDataByName("activeThoughts");
      if (activeThoughtsSave != null) {
         this.activeThoughts.applyLoadData(activeThoughtsSave);
      }

      this.settlementUniqueID = save.getInt("settlementUniqueID", this.settlementUniqueID, false);
      if (save.getBoolean("isSettler", false, false)) {
         int settlerIslandX = save.getInt("settlerIslandX", Integer.MIN_VALUE, false);
         int settlerIslandY = save.getInt("settlerIslandY", Integer.MIN_VALUE, false);
         if (settlerIslandX != Integer.MIN_VALUE && settlerIslandY != Integer.MIN_VALUE && this.isServer()) {
            OneWorldMigration migration = this.getServer().world.oneWorldMigration;
            if (migration != null) {
               this.earlyAccessSettlementIsland = new Point(settlerIslandX, settlerIslandY);
            }
         }
      }

      this.visitorSettlementUniqueID = save.getInt("visitorSettlementUniqueID", this.visitorSettlementUniqueID, false);
      boolean isOldVisitor = save.getBoolean("isVisitor", false, false);
      if (save.hasLoadDataByName("isTravelingHuman")) {
         isOldVisitor = save.getBoolean("isTravelingHuman", isOldVisitor, false);
      }

      if (isOldVisitor) {
         LevelIdentifier oldIdentifier = this.getLevel().getIdentifier();
         if (oldIdentifier.isIslandPosition()) {
            this.earlyAccessVisitorIsland = new Point(oldIdentifier.getIslandX(), oldIdentifier.getIslandY());
         } else {
            this.remove();
         }
      }

      if (this.visitorSettlementUniqueID != 0 || this.earlyAccessVisitorIsland != null) {
         this.travelOutTime = save.getLong("travelOutTime", 0L);
         if (this.travelOutTime <= 0L) {
            this.remove();
         }
      }

      LoadData dietFilterSave = save.getFirstLoadDataByName("dietFilter");
      if (dietFilterSave != null) {
         this.loadedDietFilter = new ItemCategoriesFilter(ItemCategory.foodQualityMasterCategory, true);
         this.loadedDietFilter.applyLoadData(dietFilterSave);
      }

      LoadData savedSettlerSave = save.getFirstLoadDataByName("savedSettlerSettings");
      if (savedSettlerSave != null) {
         try {
            this.savedSettlerSettings = new SavedSettlerSettings(savedSettlerSave);
         } catch (Exception e) {
            System.err.println("Could not load saved settler settings for " + this);
            e.printStackTrace();
         }
      }

      this.settlerRecreationLevel = save.getFloat("settlerRecreationLevel", this.settlerRecreationLevel, false);
      this.recreationExhaustionManager.applyLoadData(save);
      this.lastSettlerRecreationSyncLevel = this.settlerRecreationLevel;
      this.hungerLevel = save.getFloat("hungerLevel", this.hungerLevel, false);
      String lastFoodEatenStringID = save.getUnsafeString("lastFoodEaten", null, false);
      if (lastFoodEatenStringID != null) {
         String newStringID = VersionMigration.tryFixStringID(lastFoodEatenStringID, VersionMigration.oldItemStringIDs);
         Item item = ItemRegistry.getItem(newStringID);
         if (item != null && item.isFoodItem()) {
            this.lastFoodEaten = (FoodConsumableItem)item;
         }
      }

      this.recentFoodItemIDsEaten.clear();
      String[] recentFoodItemStringIDs = save.getStringArray("recentFoodItemsEaten", null, false);
      if (recentFoodItemStringIDs != null) {
         for (String stringID : recentFoodItemStringIDs) {
            String newStringID = VersionMigration.tryFixStringID(stringID, VersionMigration.oldItemStringIDs);
            Item item = ItemRegistry.getItem(newStringID);
            if (item != null && item.isFoodItem()) {
               this.recentFoodItemIDsEaten.add(item.getID());
            }
         }
      }

      if (save.getBoolean("isTrapped", false, false)) {
         this.downedState = HumanMob.DownedState.TRAPPED;
      } else {
         this.downedState = save.getEnum(HumanMob.DownedState.class, "downedState", null, false);
      }

      this.recruitReservedSettlementUniqueID = save.getInt("recruitReservedSettlementUniqueID", 0, false);
      this.recruitReservedTimeout = save.getLong("recruitReservedTimeout", this.recruitReservedTimeout, false);
      if (save.hasLoadDataByName("recruitReserved")) {
         String recruitReservedString = save.getUnsafeString("recruitReserved", null, false);
         if (recruitReservedString != null) {
            try {
               LevelIdentifier recruitReserved = new LevelIdentifier(recruitReservedString);
               if (recruitReserved.isIslandPosition()) {
                  this.earlyAccessRecruitReservedIsland = new Point(recruitReserved.getIslandX(), recruitReserved.getIslandY());
               }
            } catch (InvalidLevelIdentifierException var28) {
            }
         }
      }

      this.moveInPoint = save.getPoint("moveInPoint", this.moveInPoint, false);
      this.moveOutPoint = save.getPoint("moveOutPoint", this.moveOutPoint, false);
      this.loadedObjectUserTile = save.getPoint("objectUser", null, false);
      this.settlerName = save.getSafeString("settlerName", this.settlerName, false);
      LoadData personalitiesSave = save.getFirstLoadDataByName("personalities");
      if (personalitiesSave != null) {
         if (debugReloadPersonalities) {
            this.setupPersonalities();

            for (SettlerPersonality personality : this.personalities) {
               LoadData personalitySave = personalitiesSave.getFirstLoadDataByName(personality.getStringID());
               if (personalitySave != null) {
                  try {
                     personality.applyLoadData(personalitySave);
                  } catch (Exception e) {
                     System.err.println("Error loading personality data for " + personalitySave.getName() + " on " + this);
                     e.printStackTrace();
                  }
               }
            }
         } else {
            this.personalities = new ArrayList<>();

            for (LoadData personalitySave : personalitiesSave.getLoadData()) {
               int personalityID = SettlerPersonalityRegistry.getSettlerPersonalityID(personalitySave.getName());
               if (personalityID != -1) {
                  try {
                     SettlerPersonality personality = SettlerPersonalityRegistry.getNewSettlerPersonality(personalityID, this);
                     personality.applyLoadData(personalitySave);
                     this.personalities.add(personality);
                  } catch (Exception e) {
                     System.err.println("Error loading personality data for " + personalitySave.getName() + " on " + this);
                     e.printStackTrace();
                  }
               } else {
                  GameLog.warn.println("Could not find settler personality with stringID: " + personalitySave.getName() + " when loading " + this);
               }
            }
         }
      }

      this.morePersonalitiesFromIncursionPerk = save.getBoolean("morePersonalitiesFromIncursionPerk", this.morePersonalitiesFromIncursionPerk, false);
      LoadData romancePersonalitySave = save.getFirstLoadDataByName("romancePersonality");
      if (romancePersonalitySave != null) {
         String romanceStringID = romancePersonalitySave.getSafeString("romanceStringID", null, false);
         if (romanceStringID != null) {
            int settlerRomancePersonalityID = SettlerRomancePersonalityRegistry.getSettlerRomancePersonalityID(romanceStringID);
            if (settlerRomancePersonalityID != -1) {
               try {
                  this.romancePersonality = SettlerRomancePersonalityRegistry.getNewSettlerRomancePersonality(settlerRomancePersonalityID, this);
                  this.romancePersonality.applyLoadData(romancePersonalitySave);
               } catch (Exception e) {
                  System.err.println("Error loading romance personality data for " + romanceStringID + " on " + this);
                  e.printStackTrace();
               }
            } else {
               GameLog.warn.println("Could not find romance personality with stringID: " + romanceStringID + " when loading " + this);
            }
         } else {
            GameLog.warn.println("Could not find romance personality stringID when loading " + this);
         }
      }

      this.settlerQualityOfLife = save.getInt("settlerQualityOfLife", this.settlerQualityOfLife, false);
      if (this.settlerQualityOfLife < 0) {
         this.settlerQualityOfLife = 0;
      }

      this.settlerQualityOfLifeChangeBuffer = save.getFloat("settlerQualityOfLifeChangeBuffer", this.settlerQualityOfLifeChangeBuffer, false);
      this.recreationStrikeCooldownBuffer = save.getInt("recreationStrikeCooldownBuffer", this.recreationStrikeCooldownBuffer, false);
      this.hungerStrikeCooldownBuffer = save.getInt("hungerStrikeCooldownBuffer", this.hungerStrikeCooldownBuffer, false);
      this.leaveBuffer = save.getFloat("leaveBuffer", this.leaveBuffer, false);
      this.isOnStrike.set(save.getBoolean("isOnStrike", this.isOnStrike.get(), false));
      this.endStrikeBuffer = save.getInt("endStrikeBuffer", this.endStrikeBuffer, false);
      this.nextMissionWorldTime = save.getLong("nextMissionWorldTime", this.nextMissionWorldTime, false);
      String lastPerformedJobStringID = save.getSafeString("lastPerformedJobID", null, false);
      if (lastPerformedJobStringID != null) {
         this.jobTypeHandler.lastPerformedJobID = LevelJobRegistry.getJobID(lastPerformedJobStringID);
      }

      String prioritizeNextJobStringID = save.getSafeString("prioritizeNextJobID", null, false);
      if (prioritizeNextJobStringID != null) {
         this.jobTypeHandler.prioritizeNextJobID = LevelJobRegistry.getJobID(prioritizeNextJobStringID);
      }

      LoadData workPriorities = save.getFirstLoadDataByName("workPriorities");
      if (workPriorities != null) {
         for (LoadData workPriority : workPriorities.getLoadData()) {
            if (workPriority.isArray()) {
               JobTypeHandler.TypePriority priority = this.jobTypeHandler.getPriority(workPriority.getName());
               if (priority != null) {
                  priority.loadSaveData(workPriority);
               }
            }
         }
      }

      this.workInventory.clear();
      LoadData workInventorySave = save.getFirstLoadDataByName("workInventory");
      if (workInventorySave != null) {
         for (LoadData itemSave : workInventorySave.getLoadData()) {
            InventoryItem item = InventoryItem.fromLoadData(itemSave);
            if (item != null) {
               this.workInventory.add(item);
            }
         }
      }

      LoadData missionData = save.getFirstLoadDataByName("MISSION");
      if (missionData != null) {
         String missionStringID = missionData.getUnsafeString("stringID", null, false);
         if (missionStringID != null) {
            try {
               this.currentMission = SettlerMission.registry.getNewInstance(missionStringID);
               this.currentMission.applySaveData(this, missionData);
            } catch (Exception e) {
               System.err.println("Failed to load explorer mission");
               e.printStackTrace();
               this.currentMission = null;
            }
         } else {
            GameLog.warn.println("Could not find explorer mission stringID");
         }
      }

      if (this.isDowned() && this.currentMission != null) {
         this.currentMission = null;
      }

      this.completedMission = save.getBoolean("completedMission", this.completedMission, false);
      this.missionFailed = save.getBoolean("missionFailed", this.missionFailed, false);
      LoadData missionFailedMessageSave = save.getFirstLoadDataByName("missionFailedMessage");
      if (missionFailedMessageSave != null) {
         this.missionFailedMessage = GameMessage.loadSave(missionFailedMessageSave);
      }

      this.selfManageEquipment.set(save.getBoolean("selfManageEquipment", this.selfManageEquipment.get(), false));
      LoadData equipmentSave = save.getFirstLoadDataByName("equipment");
      if (equipmentSave != null) {
         this.equipmentInventory.override(InventorySave.loadSave(equipmentSave));
      }

      LoadData villagerDataSave = save.getFirstLoadDataByName("VILLAGER_DATA");
      if (villagerDataSave != null) {
         try {
            this.villagerData = new OneWorldNPCVillageData.NPCVillagerData(villagerDataSave);
         } catch (LoadDataException e) {
            System.err.println("Error loading villager data for " + this + ": " + e.getMessage());
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      this.updateStats(false);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      if (reader.getNextBoolean()) {
         this.settlementUniqueID = reader.getNextInt();
      }

      this.visitorSettlementUniqueID = reader.getNextInt();
      this.setSettlerSeed(reader.getNextInt(), false);
      int personalitiesCount = reader.getNextShortUnsigned();
      this.personalities = new ArrayList<>(personalitiesCount);

      for (int i = 0; i < personalitiesCount; i++) {
         SettlerPersonality personality = SettlerPersonalityRegistry.getNewSettlerPersonality(reader.getNextShortUnsigned(), this);
         personality.applySpawnPacket(reader);
         this.personalities.add(personality);
      }

      if (reader.getNextBoolean()) {
         this.romancePersonality = SettlerRomancePersonalityRegistry.getNewSettlerRomancePersonality(reader.getNextInt(), this);
         this.romancePersonality.applySpawnPacket(reader);
      }

      this.customLook = reader.getNextBoolean();
      if (this.customLook) {
         this.look.applyContentPacket(reader);
      }

      if (reader.getNextBoolean()) {
         this.settlerName = reader.getNextString();
      } else {
         this.settlerName = null;
      }

      this.applyWorkPacket(reader);
      this.currentActivity.readSpawnPacket(reader);
      this.equipmentInventory.override(Inventory.getInventory(reader));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      if (this.settlementUniqueID != 0) {
         writer.putNextBoolean(true);
         writer.putNextInt(this.settlementUniqueID);
      } else {
         writer.putNextBoolean(false);
      }

      writer.putNextInt(this.visitorSettlementUniqueID);
      writer.putNextInt(this.settlerSeed);
      writer.putNextShortUnsigned(this.personalities.size());

      for (SettlerPersonality personality : this.personalities) {
         writer.putNextShortUnsigned(personality.getID());
         personality.setupSpawnPacket(writer);
      }

      if (this.romancePersonality != null) {
         writer.putNextBoolean(true);
         writer.putNextInt(this.romancePersonality.getID());
         this.romancePersonality.setupSpawnPacket(writer);
      } else {
         writer.putNextBoolean(false);
      }

      writer.putNextBoolean(this.customLook);
      if (this.customLook) {
         this.look.setupContentPacket(writer, false);
      }

      writer.putNextBoolean(this.settlerName != null);
      if (this.settlerName != null) {
         writer.putNextString(this.settlerName);
      }

      this.setupWorkPacket(writer);
      this.currentActivity.writeSpawnPacket(writer);
      this.equipmentInventory.writeContent(writer);
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      this.writeObjectUserPacket(writer);
      writer.putNextBoolean(this.currentMission != null);
      if (this.currentMission != null) {
         writer.putNextShortUnsigned(this.currentMission.getID());
         this.currentMission.setupMovementPacket(this, writer);
      }

      writer.putNextInt(this.settlerQualityOfLife);
      writer.putNextInt(this.settlerHappiness);
      writer.putNextFloat(this.settlerRecreationLevel);
      writer.putNextFloat(this.hungerLevel);
      this.isOnStrike.writeUpdatePacket(writer);
      writer.putNextBoolean(this.completedMission);
      writer.putNextBoolean(this.missionFailed);
      writer.putNextBoolean(this.downedState != null);
      if (this.downedState != null) {
         writer.putNextEnum(this.downedState);
      }
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.readObjectUserPacket(reader);
      if (reader.getNextBoolean()) {
         this.currentMission = SettlerMission.registry.getNewInstance(reader.getNextShortUnsigned());
         this.currentMission.applyMovementPacket(this, reader);
      } else {
         this.currentMission = null;
      }

      this.settlerQualityOfLife = reader.getNextInt();
      this.settlerHappiness = reader.getNextInt();
      this.settlerRecreationLevel = reader.getNextFloat();
      this.lastSettlerRecreationSyncLevel = this.settlerRecreationLevel;
      this.hungerLevel = reader.getNextFloat();
      this.isOnStrike.readUpdatePacket(reader);
      this.completedMission = reader.getNextBoolean();
      this.missionFailed = reader.getNextBoolean();
      HumanMob.DownedState prevDownedState = this.downedState;
      if (reader.getNextBoolean()) {
         this.downedState = reader.getNextEnum(HumanMob.DownedState.class);
      } else {
         this.downedState = null;
      }

      if (prevDownedState != this.downedState) {
         this.updateStats(false);
         this.updateTeam();
      }
   }

   public void setupWorkPacket(PacketWriter writer) {
      if (this.home != null) {
         writer.putNextBoolean(true);
         writer.putNextInt(this.home.x);
         writer.putNextInt(this.home.y);
      } else {
         writer.putNextBoolean(false);
      }

      writer.putNextFloat(this.hungerLevel);
      writer.putNextBoolean(this.lastFoodEaten != null);
      if (this.lastFoodEaten != null) {
         writer.putNextShortUnsigned(this.lastFoodEaten.getID());
      }

      writer.putNextShortUnsigned(this.workInventory.size());

      for (InventoryItem item : this.workInventory) {
         InventoryItem.addPacketContent(item, writer);
      }
   }

   public void applyWorkPacket(PacketReader reader) {
      if (reader.getNextBoolean()) {
         int x = reader.getNextInt();
         int y = reader.getNextInt();
         this.home = new Point(x, y);
      } else {
         this.home = null;
      }

      this.hungerLevel = reader.getNextFloat();
      if (reader.getNextBoolean()) {
         int foodID = reader.getNextShortUnsigned();
         Item item = ItemRegistry.getItem(foodID);
         if (item != null && item.isFoodItem()) {
            this.lastFoodEaten = (FoodConsumableItem)item;
         } else {
            this.lastFoodEaten = null;
         }
      } else {
         this.lastFoodEaten = null;
      }

      int itemsCount = reader.getNextShortUnsigned();
      this.workInventory = new ArrayList<>(itemsCount);

      for (int i = 0; i < itemsCount; i++) {
         this.workInventory.add(InventoryItem.fromContentPacket(reader));
      }
   }

   public void sendWorkUpdatePacket() {
      this.sendWorkUpdatePacket = true;
   }

   @Override
   public void init() {
      super.init();
      this.workSettings.closeRegistry();
      this.interactingClients.clear();
      if (this.settlerSeed == 0) {
         this.setSettlerSeed(GameRandom.globalRandom.nextInt(), false);
      }

      this.settlerCheck = GameRandom.globalRandom.nextInt(600);

      for (SettlerPersonality personality : this.personalities) {
         personality.init();
      }

      if (this.romancePersonality == null) {
         this.setupRomancePersonality();
      }

      this.romancePersonality.init();
      this.activeThoughts.init();
      this.equipmentBuffManager.updateAll();
      this.updateStats(true);
      this.updateTeam();
      this.ai = new BehaviourTreeAI<>(this, new HumanAI<>(320, true, false, 25000), new AIMover(humanPathIterations));
      this.jobTypeHandler.startGlobalCooldown(this.getTime(), GameRandom.globalRandom.getIntBetween(5, 10) * 1000);
      this.searchEquipmentCooldown = this.getTime() + GameRandom.globalRandom.getIntBetween(60, 300) * 1000L;
      if (this.isServer() && this.isSettler() && !this.isSettlerOnCurrentLevel()) {
         SettlersWorldData settlersData = SettlersWorldData.getSettlersData(this.getServer());
         if (!settlersData.exists(this.getUniqueID())) {
            this.remove();
            System.out.println("REMOVED SETTLER OUTSIDE SETTLEMENT BECAUSE IT IS NOT IN SETTLERS WORLD DATA? " + this);
         } else {
            this.commandGuard(null, this.getX(), this.getY());
         }
      }
   }

   @Override
   public void onUnloading(Region region) {
      super.onUnloading(region);
      if (this.objectUser != null) {
         this.objectUser.stopUsing();
      }

      if (this.isServer() && this.isSettler() && !this.isSettlerWithinSettlementLoadedRegions()) {
         SettlersWorldData settlersData = SettlersWorldData.getSettlersData(this.getServer());
         settlersData.refreshWorldSettler(this, false);
      }
   }

   @Override
   protected void handleLoadedValues() {
      super.handleLoadedValues();
      if (this.loadedObjectUserTile != null) {
         GameObject object = this.getLevel().getObject(this.loadedObjectUserTile.x, this.loadedObjectUserTile.y);
         if (object instanceof ObjectUsersObject) {
            ((ObjectUsersObject)object).updateUserToExitPos(this.getLevel(), this.loadedObjectUserTile.x, this.loadedObjectUserTile.y, this, 0.0F, 0.0F);
         }

         this.loadedObjectUserTile = null;
      }
   }

   @Override
   public void onLevelChanged() {
      super.onLevelChanged();
      if (this.objectUser != null) {
         this.objectUser.stopUsing(false, 0.0F, 0.0F);
      }

      if (this.isServer() && this.levelSettler != null && !this.isSettlerWithinSettlement(this.levelSettler.data.networkData)) {
         this.setHome(null);
      }
   }

   @Override
   public InteractingClients getInteractingClients() {
      return this.interactingClients;
   }

   @Override
   public GameMessage getLocalization() {
      return this.settlerName == null ? super.getLocalization() : new LocalMessage("mob", this.getStringID() + "name", "name", this.settlerName);
   }

   @Override
   public LootTable getLootTable() {
      LootTable lootTable = new LootTable();

      for (InventoryItem item : this.workInventory) {
         lootTable.items.add(new LootInventoryItem(item));
      }

      for (int slot = 0; slot < this.equipmentInventory.getSize(); slot++) {
         InventoryItem item = this.equipmentInventory.getItem(slot);
         if (item != null) {
            lootTable.items.add(new LootInventoryItem(item));
         }
      }

      if (this.currentMission != null) {
         LootTable missionLootTable = this.currentMission.getLootTable(this);
         if (missionLootTable != null) {
            lootTable.items.add(missionLootTable);
         }
      }

      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameSkin gameSkin = this.look.getGameSkin(!this.customLook);

      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), gameSkin, GameRandom.globalRandom.nextInt(5), 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void doBeforeHitCalculatedLogic(MobBeforeHitCalculatedEvent event) {
      super.doBeforeHitCalculatedLogic(event);

      for (SettlerPersonality personality : this.personalities) {
         personality.doBeforeHitCalculatedLogic(event);
      }

      if (!event.isPrevented() && event.getExpectedHealth() <= 0 && this.adventureParty.isInAdventureParty()) {
         this.adventureParty.onSecondWindAttempt(event);
      }
   }

   @Override
   public void setHealthHidden(int health, float knockbackX, float knockbackY, Attacker attacker, boolean fromNetworkUpdate) {
      if (this.isDowned()) {
         int maxHealth = Math.max(1, (int)(this.getMaxHealth() * 0.1F));
         if (health > maxHealth) {
            health = maxHealth;
         }
      }

      if (health <= 0 && (this.isSettler() || this.isDowned())) {
         WorldSettings worldSettings = this.getWorldSettings();
         if (worldSettings == null || !worldSettings.canSettlersDie) {
            health = 1;
            if (this.isServer() && !this.isDowned()) {
               ServerSettlementData serverSettlementData = this.getSettlerSettlementServerData();
               if (serverSettlementData != null) {
                  if (this.levelSettler != null) {
                     this.savedSettlerSettings = new SavedSettlerSettings(this.levelSettler);
                  }

                  serverSettlementData.removeSettler(this.getUniqueID(), null);
                  this.moveOutPoint = null;
                  this.settlementUniqueID = 0;
                  this.levelSettler = null;
                  this.cancelJob();
                  this.updateStats(false);
                  GameMessage displayName = new LocalMessage(
                     "deaths", "settlerfrom", "victim", this.getLocalization(), "settlement", serverSettlementData.networkData.getSettlementName()
                  );
                  LocalMessage downedMessage = new LocalMessage("deaths", "settlerdowned", "settler", displayName);
                  GameMessage coloredMessage = new GameMessageBuilder().append("§9").append(downedMessage);
                  serverSettlementData.networkData.streamTeamMembers().forEach(c -> c.sendChatMessage(coloredMessage));
               }

               int timeout = 600000;
               this.setDowned(this.getSettlementUniqueID(), timeout);

               for (InventoryItem inventoryItem : this.workInventory) {
                  this.getLevel().entityManager.pickups.add(inventoryItem.getPickupEntity(this.getLevel(), this.x, this.y));
               }

               this.workInventory.clear();
               this.sendWorkUpdatePacket();
            }
         }
      }

      super.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.isSettler() && this.isServer()) {
         if (this.romancePersonality.isInAnyRelationship()) {
            SettlersWorldData.getSettlersData(this.getServer()).onSettlerRelationshipDeath(this.getUniqueID());
         }

         ServerSettlementData data = this.getSettlerSettlementServerData();
         if (data != null) {
            data.onSettlerDeath(this.getUniqueID());
            if (this.levelSettler != null) {
               this.levelSettler.onSettlerDeath();
            }

            LocalMessage displayName = new LocalMessage(
               "deaths", "settlerfrom", "victim", this.getLocalization(), "settlement", data.networkData.getSettlementName()
            );
            GameMessage deathMessage = new GameMessageBuilder().append("§9").append(DeathMessageTable.getDeathMessage(attacker, displayName));
            data.networkData.streamTeamMembers().forEach(c -> c.sendChatMessage(deathMessage));
         }
      }
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      if (!this.removed()) {
         if (this.getLevel() != null && this.isServer()) {
            ServerClient serverClient = this.adventureParty.getServerClient();
            if (serverClient != null) {
               serverClient.adventureParty.serverRemove(this, true, isDeath);
            }
         }

         if (isDeath && this.villagerData != null) {
            OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(this.getLevel(), true);
            villageData.addNPCVillager(this.villagerData);
            this.villagerData = null;
         }
      }

      super.remove(knockbackX, knockbackY, attacker, isDeath);
   }

   @Override
   public void addAttackersToSet(HashSet<Attacker> attackers) {
      super.addAttackersToSet(attackers);
      if (this.adventureParty != null) {
         PlayerMob player = this.adventureParty.getPlayerMob();
         if (player != null) {
            attackers.add(player);
         }
      }
   }

   @Override
   public PathDoorOption getPathDoorOption() {
      return this.getLevel() != null ? this.getLevel().regionManager.CAN_OPEN_DOORS_OPTIONS : null;
   }

   @Override
   public void clientTick() {
      this.equipmentBuffManager.clientTickEffects();
      super.clientTick();
      if (this.objectUser != null) {
         this.objectUser.tick();
      }

      this.tickHunger();
      this.tickRecreationUsage();

      for (SettlerPersonality personality : this.personalities) {
         personality.gameTick();
      }

      this.activeThoughts.tick();
      InventoryItem.tickList(this, this, this, null, this.getWorldSettings(), 1.0F, this.workInventory);
      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }

      Performance.record(this.getLevel().tickManager(), "tickItems", () -> this.equipmentInventory.tickItems(this));
      if (this.currentMission != null) {
         this.currentMission.clientTick(this);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.serverFollowersManager.setSummonFocus(this.commandAttackMob);
      if (this.leaveBuffer > 0.0F && (this.hungerLevel > 0.0F && !this.isOnStrike() || this.hungerLevel >= 0.25F)) {
         float bufferDecrease = 1.3888889E-5F;
         this.leaveBuffer -= bufferDecrease;
      }

      if (this.isOnStrike.get()) {
         HumanMob.StrikeChance strikeChance = this.wantsToGoOnStrikeChance();
         if (strikeChance == null || strikeChance.chance <= 0.0F) {
            this.isOnStrike.set(false);
         }
      }

      if (this.endStrikeBuffer > 0) {
         NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
         if ((settlement == null || !settlement.isRaidActiveOrApproaching()) && this.hasCommandOrders()) {
            this.clearCommandsOrders(null);
         }

         this.endStrikeBuffer -= 50;
      }

      if (this.isOnStrike.get() && this.endStrikeBuffer <= 0) {
         if (this.settlerRecreationLevel <= 0.5F) {
            this.settlerRecreationLevel = 0.5F;
         }

         this.isOnStrike.set(false);
      }

      if (this.objectUser != null) {
         this.objectUser.tick();
      }

      this.tickHunger();
      this.tickRecreationUsage();
      float recreationSyncDelta = Math.abs(this.settlerRecreationLevel - this.lastSettlerRecreationSyncLevel);
      if (recreationSyncDelta > 0.02F) {
         this.sendMovementPacket(false);
         this.lastSettlerRecreationSyncLevel = this.settlerRecreationLevel;
      }

      for (SettlerPersonality personality : this.personalities) {
         personality.gameTick();
      }

      if (this.romancePersonality != null && this.adventureParty.isInAdventureParty()) {
         ServerClient serverClient = this.adventureParty.getServerClient();
         if (serverClient != null) {
            PlayerRomanceManager playerRomanceManager = this.romancePersonality.getPlayerManager(serverClient, false);
            if (playerRomanceManager != null) {
               playerRomanceManager.serverTickInAdventureParty(serverClient);
            }
         }
      }

      this.activeThoughts.tick();
      this.serverTickSettlerQualityOfLife();
      InventoryItem.tickList(this, this, this, null, this.getWorldSettings(), 1.0F, this.workInventory);
      if (this.resetCommandsBuffer > 0) {
         this.resetCommandsBuffer -= 50;
         if (this.resetCommandsBuffer <= 0) {
            this.clearCommandsOrders(null);
         }
      }

      this.adventureParty.serverTick();
      if (this.isSettler()) {
         if (this.nextShowMoodTime <= this.getTime()) {
            this.nextShowMoodTime = this.getTime() + 5000L;
            if (this.getCurrentMission() == null) {
               ArrayList<MoodFloatText.Moods> moodList = this.getMoods();
               if (!moodList.isEmpty()) {
                  this.showMoods.runAndSend(moodList);
               }
            }

            NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
            if (settlement != null) {
               if (this.levelSettler == null || this.levelSettler.getBed() != null) {
                  settlement.notifications.removeNotification("nobed", this);
               } else if (this.canSubmitNoBedNotification()) {
                  settlement.notifications.submitNotification("nobed", this, SettlementNotificationSeverity.NOTE);
               }

               if (this.getSettlerQualityOfLife() <= 0 && !this.shouldIgnoreQualityOfLifeAndHappiness()) {
                  SettlementNotificationSeverity severity = SettlementNotificationSeverity.NOTE;
                  if (this.getSettlerHappiness() < HAPPINESS_QOL_DECREASE_THRESHOLD) {
                     severity = SettlementNotificationSeverity.WARNING;
                  }

                  settlement.notifications.submitNotification("lowqol", this, severity);
               } else {
                  settlement.notifications.removeNotification("lowqol", this);
               }
            }
         }

         if (this.commandFollowMob != null && !this.commandFollowMob.isSamePlace(this)) {
            this.commandFollowMob = null;
         }

         this.settlerCheck++;
         if (this.settlerCheck > 600) {
            this.runSettlerCheck();
            this.settlerCheck = 0;
         }
      }

      if (this.isAttacking) {
         this.getAttackAnimProgress();
      }

      this.interactingClients.serverTick();
      this.clientHasCommandOrders.set(this.hasCommandOrders());
      Performance.record(this.getLevel().tickManager(), "tickItems", () -> this.equipmentInventory.tickItems(this));
      this.serverTickInventorySync(this.getServer(), this);
      if (this.sendWorkUpdatePacket && this.isServer()) {
         this.sendWorkUpdatePacket = false;
         this.getServer().network.sendToClientsWithEntity(new PacketHumanWorkUpdate(this), this);
      }

      if (this.isVisitor()) {
         this.travelOutTime -= 50L;
         if (this.moveOutPoint == null) {
            if (this.travelOutTime <= 0L) {
               this.endVisitor();
            }
         } else if (this.travelOutTime <= -timeToTravelOut * 1000L) {
            this.remove();
         }
      }

      if (this.currentMission != null) {
         if (!this.currentMission.isOver()) {
            this.currentMission.serverTick(this);
         }

         if (this.currentMission.isOver()) {
            this.currentMission = null;
         }
      }

      if (this.isSettlerOnCurrentLevel()) {
         NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
         SettlerMission currentMission = this.getCurrentMission();
         if (currentMission != null) {
            this.setActivity("idle", 1000, currentMission.getActivityMessage(this));
         } else if (!this.isOnStrike() || settlement != null && settlement.isRaidActiveOrApproaching()) {
            if (this.isHiding) {
               this.setActivity("idle", 1000, new LocalMessage("activities", "hiding"));
            } else if (!this.getWorldEntity().isNight() || settlement != null && settlement.isRaidActive()) {
               if (this.getWorkInventory().isFull()) {
                  this.setActivity("idle", 1000, new LocalMessage("activities", "inventoryfull"));
               } else {
                  for (SettlerPersonality personality : this.personalities) {
                     GameMessage message = personality.getIdleActivityMessage();
                     if (message != null) {
                        this.setActivity("idle", 1000, message);
                        break;
                     }
                  }
               }
            } else {
               this.setActivity("idle", 1000, new LocalMessage("activities", "resting"));
            }
         } else {
            this.setActivity("idle", 1000, new LocalMessage("activities", "onstrike"));
         }
      }

      this.currentActivity.serverTick();
   }

   public void serverTickSettlerQualityOfLife() {
      if (!this.isSettler() && !this.isDowned()) {
         this.settlerQualityOfLife = 0;
         this.settlerQualityOfLifeChangeBuffer = 0.0F;
      } else {
         int happiness = this.getSettlerHappiness();
         if (happiness < HAPPINESS_QOL_DECREASE_THRESHOLD) {
            if (this.settlerQualityOfLife > 0 || this.settlerQualityOfLifeChangeBuffer > 0.0F) {
               float amountChangePerTick = 1.0F / SECONDS_TO_DECREASE_QOL / 20.0F;
               this.settlerQualityOfLifeChangeBuffer -= amountChangePerTick;
               if (this.settlerQualityOfLife <= 0) {
                  if (this.settlerQualityOfLifeChangeBuffer < 0.0F) {
                     this.settlerQualityOfLifeChangeBuffer = 0.0F;
                  }
               } else if (this.settlerQualityOfLifeChangeBuffer <= -1.0F) {
                  this.settlerQualityOfLife--;
                  this.settlerQualityOfLifeChangeBuffer = 0.0F;
                  this.sendMovementPacket(false);
                  this.updateHappiness();
               }
            }
         } else if (happiness >= HAPPINESS_QOL_INCREASE_THRESHOLD) {
            float amountChangePerTick = 1.0F / SECONDS_TO_INCREASE_QOL / 20.0F;
            this.settlerQualityOfLifeChangeBuffer += amountChangePerTick;
            if (this.settlerQualityOfLifeChangeBuffer >= 1.0F) {
               this.settlerQualityOfLife++;
               this.settlerQualityOfLifeChangeBuffer = 0.0F;
               this.sendMovementPacket(false);
               this.updateHappiness();
            }
         }
      }
   }

   public final void setHome(int x, int y) {
      this.setHome(new Point(x, y));
   }

   public int getHomeX() {
      return this.home == null ? -1 : this.home.x;
   }

   public int getHomeY() {
      return this.home == null ? -1 : this.home.y;
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

      return this.isDowned() ? 1 : super.getDir();
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
      if (objectUser != null) {
         return objectUser.getUserSelectBox();
      } else {
         return this.isDowned() ? new Rectangle(x - 20, y - 16, 48, 28) : super.getSelectBox(x, y);
      }
   }

   @Override
   public float getHungerLevel() {
      return this.hungerLevel;
   }

   public void tickHunger() {
      if (this.isSettler()) {
         if (this.getCurrentMission() == null) {
            boolean inAdventureParty = this.adventureParty.isInAdventureParty() && !this.isSettlerWithinSettlement();
            if (!this.getWorldEntity().isNight() || inAdventureParty) {
               float usageModifier = 1.0F;
               if (inAdventureParty) {
                  usageModifier *= adventurePartyHungerUsageMod;
               }

               if (this.isBeingInteractedWith()) {
                  usageModifier *= 0.25F;
               }

               float usedHunger = (float)(50.0 / (1000.0 * secondsToPassAtFullHunger)) * usageModifier;
               this.useHunger(usedHunger, false);
               if (this.hungerLevel <= 0.0F && this.isServer()) {
                  this.hungerStrikeCooldownBuffer -= 50;
                  if (this.hungerStrikeCooldownBuffer <= 0) {
                     if (this.isOnStrike()) {
                        this.addLeaveBuffer(0.2F);
                     } else {
                        this.attemptStartStrike(false);
                     }

                     this.resetHungerStrikeCooldownBuffer();
                  }
               }

               if (this.hungerLevel < 0.1F) {
                  this.lastFoodEaten = null;
               } else {
                  this.resetHungerStrikeCooldownBuffer();
               }
            }
         }
      }
   }

   public void resetHungerStrikeCooldownBuffer() {
      this.hungerStrikeCooldownBuffer = 420000;
   }

   @Override
   public void useHunger(float amount, boolean forceUse) {
      this.hungerLevel = Math.max(0.0F, this.hungerLevel - amount);
   }

   @Override
   public void addHunger(float amount) {
      this.hungerLevel = Math.min(this.hungerLevel + amount, 1.0F + amount);
   }

   @Override
   public boolean useFoodItem(FoodConsumableItem item, boolean giveBuff) {
      boolean out = HungerMob.super.useFoodItem(item, giveBuff);
      this.playConsumeSound.runAndSend(item.drinkSound);
      this.lastFoodEaten = item;
      this.recentFoodItemIDsEaten.addFirst(item.getID());

      while (this.recentFoodItemIDsEaten.size() > SettlerThoughtRegistry.dietVarieties.last().variety) {
         this.recentFoodItemIDsEaten.removeLast();
      }

      this.updateHappiness();
      this.sendWorkUpdatePacket();
      return out;
   }

   public void addLeaveBuffer(float percent) {
      if (this.isServer()) {
         WorldSettings worldSettings = this.getWorldSettings();
         if (worldSettings == null || worldSettings.survivalMode) {
            this.leaveBuffer += percent;
            if (this.isSettler()) {
               ServerSettlementData serverData = this.getSettlerSettlementServerData();
               if (serverData != null) {
                  GameMessage teamMessage = null;
                  if (this.leaveBuffer >= 0.8F && this.leaveBufferWarnings < this.leaveBuffer) {
                     this.leaveBufferWarnings = this.leaveBuffer;
                     teamMessage = new LocalMessage(
                        "misc", "settlerhungrywarning2", "settler", this.getLocalization(), "settlement", serverData.networkData.getSettlementName()
                     );
                  } else if (this.leaveBuffer >= 0.6F && this.leaveBufferWarnings < this.leaveBuffer) {
                     this.leaveBufferWarnings = this.leaveBuffer;
                     teamMessage = new LocalMessage(
                        "misc", "settlerhungrywarning2", "settler", this.getLocalization(), "settlement", serverData.networkData.getSettlementName()
                     );
                  } else if (this.leaveBuffer >= 0.4F && this.leaveBufferWarnings < this.leaveBuffer) {
                     this.leaveBufferWarnings = this.leaveBuffer;
                     teamMessage = new LocalMessage(
                        "misc", "settlerhungrywarning1", "settler", this.getLocalization(), "settlement", serverData.networkData.getSettlementName()
                     );
                  } else {
                     this.leaveBufferWarnings = 0.0F;
                  }

                  if (this.leaveBuffer >= 1.0F && serverData.removeSettler(this.getUniqueID(), null)) {
                     teamMessage = new LocalMessage(
                        "misc", "settlerhungryleft", "settler", this.getLocalization(), "settlement", serverData.networkData.getSettlementName()
                     );
                  }

                  if (teamMessage != null) {
                     GameMessage finalWarning = teamMessage;
                     serverData.networkData.streamTeamMembers().forEach(c -> c.sendChatMessage(finalWarning));
                  }
               }
            }
         }
      }
   }

   @Override
   public void setSettlerSeed(int seed, boolean overrideName) {
      this.settlerSeed = seed;
      GameRandom random = new GameRandom(seed + 6);
      this.gender = random.getOneOfWeighted(HumanGender.class, 40, HumanGender.MALE, 40, HumanGender.FEMALE, 20, HumanGender.NEUTRAL);
      if (!this.customLook) {
         this.look = new HumanLook();
         this.randomizeLook(this.look, this.gender, random);
      }

      if (this.settlerName == null || overrideName) {
         this.settlerName = this.getRandomName(new GameRandom(seed));
      }

      if (this.personalities.isEmpty() || overrideName) {
         this.setupPersonalities();
      }

      if (this.romancePersonality == null || overrideName) {
         this.setupRomancePersonality();
      }
   }

   public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
      look.randomizeLook(random, this.shouldOnlyUseHumanLikeLook(), gender, true, true, true, true);
   }

   protected void setupPersonalities() {
      GameRandom random = new GameRandom(this.settlerSeed).nextSeeded(63);
      int regularCount = 2;
      int bonusCount = 1;
      float regularChance = this.morePersonalitiesFromIncursionPerk ? 0.6F : 0.3F;
      float bonusChance = this.morePersonalitiesFromIncursionPerk ? 0.3F : 0.15F;

      while (random.getChance(regularChance) && regularCount < 5) {
         regularCount++;
      }

      while (random.getChance(bonusChance) && bonusCount < 2) {
         bonusCount++;
      }

      if (this.morePersonalitiesFromIncursionPerk) {
         if (random.getChance(0.25F)) {
            bonusCount++;
         } else {
            regularCount++;
         }
      }

      this.personalities = SettlerPersonalityRegistry.getNewRandomSettlerPersonalities(this, random, regularCount, bonusCount);
   }

   protected void setupRomancePersonality() {
      GameRandom random = new GameRandom(this.settlerSeed).nextSeeded(235);
      this.romancePersonality = SettlerRomancePersonalityRegistry.getNewRandomSettlerRomancePersonality(this, random);
   }

   public ArrayList<SettlerPersonality> getPersonalities() {
      return this.personalities;
   }

   public SettlerRomancePersonality getRomancePersonality() {
      return this.romancePersonality;
   }

   public PlayerRomanceManager getRomanceManager(ServerClient client, boolean createIfDoesNotExist) {
      return this.romancePersonality.getPlayerManager(client, createIfDoesNotExist);
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
   public void interact(PlayerMob player) {
      super.interact(player);
      if (!this.isAttacking) {
         this.turnTo(player);
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return true;
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "talktip");
   }

   protected void turnTo(Mob mob) {
      int deltaX = mob.getX() - this.getX();
      int deltaY = mob.getY() - this.getY();
      this.setFacingDir(deltaX, deltaY);
   }

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public boolean canBePushed(Mob other) {
      ObjectUserActive objectUser = this.objectUser;
      if (objectUser != null && objectUser.preventsUserPushed()) {
         return false;
      } else {
         return this.currentMission != null && !this.currentMission.isMobVisible(this) ? false : super.canBePushed(other);
      }
   }

   @Override
   public boolean canPushMob(Mob other) {
      ObjectUserActive objectUser = this.objectUser;
      if (objectUser != null && objectUser.preventsUserPushed()) {
         return false;
      } else {
         return this.currentMission != null && !this.currentMission.isMobVisible(this) ? false : super.canPushMob(other);
      }
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
   public boolean isVisible() {
      return this.currentMission != null ? this.currentMission.isMobVisible(this) : super.isVisible();
   }

   public MoveToTile getMoveToPoint() {
      if (this.currentMission != null) {
         return this.currentMission.getMoveOutPoint(this);
      }

      if (this.moveOutPoint != null) {
         if (!this.isDowned() && !this.isTrapped()) {
            if (this.isAtEdgeOfSettlement()) {
               this.remove();
            }

            return new MoveToTile(this.moveOutPoint, true) {
               @Override
               public boolean moveIfPathFailed(float tileDistance) {
                  return tileDistance >= 20.0F;
               }

               @Override
               public boolean isAtLocation(float tileDistance, boolean foundPath) {
                  return foundPath ? tileDistance < 2.0F : tileDistance < 20.0F;
               }

               @Override
               public void onArrivedAtLocation() {
                  HumanMob.this.remove();
               }
            };
         } else {
            this.moveOutPoint = null;
            return null;
         }
      } else {
         return this.moveInPoint != null ? new MoveToTile(this.moveInPoint, true) {
            @Override
            public boolean moveIfPathFailed(float tileDistance) {
               return tileDistance >= 30.0F;
            }

            @Override
            public boolean isAtLocation(float tileDistance, boolean foundPath) {
               return foundPath ? tileDistance < 5.0F : tileDistance < 30.0F;
            }

            @Override
            public void onArrivedAtLocation() {
               HumanMob.this.stopMovingIn();
            }
         } : null;
      }
   }

   public void moveIn(int tileX, int tileY, boolean walkNow) {
      this.setHome(tileX, tileY);
      if (walkNow) {
         this.moveInPoint = this.getNewMoveInPoint(tileX, tileY);
      }
   }

   public void stopMovingIn() {
      this.moveInPoint = null;
   }

   protected Point getNewMoveInPoint(int homeX, int homeY) {
      if (this.getLevel().isOutside(homeX, homeY)) {
         return new Point(homeX, homeY);
      }

      ArrayList<Point> destinations = new ArrayList<>();
      ConnectedSubRegionsResult room = this.getLevel().regionManager.getRoomConnectedByTile(homeX, homeY, true, 2000);
      if (room == null) {
         return new Point(homeX, homeY);
      }

      for (SubRegion sr : room.connectedRegions) {
         for (Point tile : sr.getLevelTiles()) {
            if (!this.getLevel().getObject(tile.x, tile.y).isSolid(this.getLevel(), tile.x, tile.y)) {
               destinations.add(tile);
            }
         }
      }

      return destinations.isEmpty() ? new Point(homeX, homeY) : GameRandom.globalRandom.getOneOf(destinations);
   }

   public boolean isAtEdgeOfSettlement() {
      NetworkSettlementData settlement = this.getSettlementNetworkData();
      if (settlement == null) {
         return true;
      }

      Level level = this.getLevel();
      if (!settlement.level.isSamePlace(level)) {
         return true;
      }

      int tileX = this.getTileX();
      int tileY = this.getTileY();
      int padding = 5;
      if (level.tileWidth <= 0 || tileX >= padding && tileX < level.tileWidth - padding) {
         if (level.tileHeight <= 0 || tileY >= padding && tileY < level.tileHeight - padding) {
            Rectangle tileRectangle = settlement.getLoadedTileRectangle();
            tileRectangle = new Rectangle(
               tileRectangle.x + padding, tileRectangle.y + padding, tileRectangle.width - padding * 2, tileRectangle.height - padding * 2
            );
            return !tileRectangle.contains(tileX, tileY);
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public Point getNewEdgeOfSettlementTile() {
      NetworkSettlementData settlement = this.getSettlementNetworkData();
      if (settlement == null) {
         return new Point(this.getTileX(), this.getTileY());
      }

      int padding = 3;
      Rectangle[] spawnTileRectangles = ServerSettlementData.getOutsideRectangles(settlement.getLoadedTileRectangle(), -padding, padding);
      Point tile = null;

      for (int i = 0; i < 50; i++) {
         Rectangle spawnRectangle = GameRandom.globalRandom.getOneOf(spawnTileRectangles);
         tile = new Point(
            this.getLevel().limitTileXToBounds(spawnRectangle.x + GameRandom.globalRandom.nextInt(spawnRectangle.width), 0, 2),
            this.getLevel().limitTileYToBounds(spawnRectangle.y + GameRandom.globalRandom.nextInt(spawnRectangle.height), 0, 2)
         );
         if (this.estimateCanMoveTo(tile.x, tile.y, true)) {
            return tile;
         }
      }

      return tile;
   }

   public boolean startMission(SettlerMission mission) {
      if (mission.canStart(this)) {
         this.cancelJob();
         if (this.adventureParty.isInAdventureParty()) {
            this.adventureParty.clear(true);
         }

         this.clearCommandsOrders(null);
         this.currentMission = mission;
         this.currentMission.start(this);
         this.sendMovementPacket(false);
         this.nextMissionWorldTime = this.getWorldTime() + 600000L;
         return true;
      } else {
         return false;
      }
   }

   public SettlerMission getCurrentMission() {
      return this.currentMission;
   }

   public boolean canDoExpedition(SettlerExpedition expedition) {
      return false;
   }

   public List<ExpeditionList> getPossibleExpeditions() {
      return Collections.emptyList();
   }

   public boolean hasCompletedMission() {
      return this.completedMission;
   }

   public void clearMissionResult() {
      this.completedMission = false;
      this.missionFailed = false;
      this.missionFailedMessage = null;
      this.sendMovementPacket(false);
   }

   public void updateTeam() {
      if (this.getLevel() != null && !this.isClient()) {
         if (this.downedState != null) {
            this.team.set(-1);
            this.owner.set(-1L);
         } else {
            int newTeam = -10;
            long newOwnerAuth = -1L;
            if (this.isSettler()) {
               ServerSettlementData settlementData = this.getSettlerSettlementServerData();
               int teamID;
               if (settlementData != null) {
                  teamID = settlementData.networkData.getTeamID();
                  newOwnerAuth = settlementData.networkData.getOwnerAuth();
               } else {
                  teamID = -1;
               }

               if (teamID != -1) {
                  newTeam = teamID;
               }
            } else if (this.isVisitor()) {
               newTeam = -1;
            }

            this.team.set(newTeam);
            this.owner.set(newOwnerAuth);
         }
      }
   }

   @Override
   public void onPerformedJob(AbstractLevelJob job, JobTypeHandler.TypePriority priority) {
      JobTypeHandler.SubHandler<?> handler = this.jobTypeHandler.getJobHandler(job.getID());
      if (handler != null && handler.startCooldownAfterPerformed) {
         handler.startCooldown(this.getTime());
      }

      for (SettlerPersonality personality : this.personalities) {
         personality.onPerformedJob(job, priority);
      }
   }

   public void startVisitor(ServerSettlementData data) {
      this.levelSettler = null;
      this.settlementUniqueID = 0;
      this.visitorSettlementUniqueID = data.uniqueID;
      this.cachedServerSettlementData = null;
      this.cachedNetworkSettlementData = null;
      this.cachedVisitorServerSettlementData = data;
      this.cachedVisitorNetworkSettlementData = data.networkData;
      this.updateStats(true);
      Point travelPos = data.getFlagTile();
      if (travelPos != null) {
         this.moveIn(travelPos.x, travelPos.y, true);
      } else {
         GameLog.warn.println("Could not find move-in position for visitor " + this);
      }

      this.home = new Point(this.moveInPoint);
      this.travelOutTime = visitorStayTimeSeconds * 1000L;
      this.updateTeam();
   }

   @Override
   public ServerSettlementData getSettlerSettlementServerData() {
      int settlementUniqueID = this.getSettlementUniqueID();
      if (settlementUniqueID == 0) {
         this.cachedServerSettlementData = null;
         return null;
      }

      if (this.cachedServerSettlementData != null
         && this.cachedServerSettlementData.uniqueID == settlementUniqueID
         && !this.cachedServerSettlementData.networkData.isUnloadedOrDisbanded()) {
         return this.cachedServerSettlementData;
      }

      this.cachedServerSettlementData = HumanSettlerMob.super.getSettlerSettlementServerData();
      return this.cachedServerSettlementData;
   }

   @Override
   public NetworkSettlementData getSettlerSettlementNetworkData() {
      int settlementUniqueID = this.getSettlementUniqueID();
      if (settlementUniqueID == 0) {
         this.cachedNetworkSettlementData = null;
         return null;
      }

      if (this.cachedNetworkSettlementData != null
         && this.cachedNetworkSettlementData.uniqueID == settlementUniqueID
         && !this.cachedNetworkSettlementData.isUnloadedOrDisbanded()) {
         return this.cachedNetworkSettlementData;
      }

      if (this.cachedServerSettlementData != null) {
         if (this.cachedServerSettlementData.uniqueID == settlementUniqueID && !this.cachedServerSettlementData.networkData.isUnloadedOrDisbanded()) {
            this.cachedNetworkSettlementData = this.cachedServerSettlementData.networkData;
            return this.cachedServerSettlementData.networkData;
         }

         this.cachedServerSettlementData = null;
      }

      this.cachedNetworkSettlementData = HumanSettlerMob.super.getSettlerSettlementNetworkData();
      return this.cachedNetworkSettlementData;
   }

   public void endVisitor() {
      this.moveOut();
      ServerSettlementData serverData = this.getVisitorSettlementServerData();
      if (serverData != null) {
         serverData.onVisitorLeave(this);
      }
   }

   public ServerSettlementData getVisitorSettlementServerData() {
      if (this.visitorSettlementUniqueID == 0) {
         this.cachedVisitorServerSettlementData = null;
         return null;
      }

      if (this.cachedVisitorServerSettlementData != null
         && this.cachedVisitorServerSettlementData.uniqueID == this.visitorSettlementUniqueID
         && !this.cachedVisitorServerSettlementData.networkData.isUnloadedOrDisbanded()) {
         return this.cachedVisitorServerSettlementData;
      }

      if (!this.isServer()) {
         return null;
      }

      this.cachedVisitorServerSettlementData = SettlementsWorldData.getSettlementsData(this.getMob()).getOrLoadServerData(this.visitorSettlementUniqueID);
      return this.cachedVisitorServerSettlementData;
   }

   public NetworkSettlementData getVisitorSettlementNetworkData() {
      if (this.visitorSettlementUniqueID == 0) {
         this.cachedVisitorNetworkSettlementData = null;
         return null;
      }

      if (this.cachedVisitorNetworkSettlementData != null
         && this.cachedVisitorNetworkSettlementData.uniqueID == this.visitorSettlementUniqueID
         && !this.cachedVisitorNetworkSettlementData.isUnloadedOrDisbanded()) {
         return this.cachedVisitorNetworkSettlementData;
      }

      if (this.cachedVisitorServerSettlementData != null) {
         if (this.cachedVisitorServerSettlementData.uniqueID == this.visitorSettlementUniqueID
            && !this.cachedVisitorServerSettlementData.networkData.isUnloadedOrDisbanded()) {
            this.cachedVisitorNetworkSettlementData = this.cachedVisitorServerSettlementData.networkData;
            return this.cachedVisitorNetworkSettlementData;
         }

         this.cachedVisitorServerSettlementData = null;
      }

      this.cachedVisitorNetworkSettlementData = SettlementsWorldData.getSettlementsData(this.getMob()).getNetworkData(this.visitorSettlementUniqueID);
      return this.cachedVisitorNetworkSettlementData;
   }

   public ServerSettlementData getSettlementServerData() {
      if (this.isVisitor()) {
         return this.getVisitorSettlementServerData();
      } else {
         return this.isSettler()
            ? this.getSettlerSettlementServerData()
            : SettlementsWorldData.getSettlementsData(this).getOrLoadServerDataAtTile(this.getLevel().getIdentifier(), this.getTileX(), this.getTileY());
      }
   }

   public NetworkSettlementData getSettlementNetworkData() {
      if (this.isVisitor()) {
         return this.getVisitorSettlementNetworkData();
      } else {
         return this.isSettler()
            ? this.getSettlerSettlementNetworkData()
            : SettlementsWorldData.getSettlementsData(this).getNetworkDataAtTile(this.getLevel().getIdentifier(), this.getTileX(), this.getTileY());
      }
   }

   public boolean doesClientHaveAccessToSettlement(ServerClient client) {
      NetworkSettlementData settlementNetworkData = this.getSettlementNetworkData();
      return settlementNetworkData != null && settlementNetworkData.doesClientHaveAccess(client);
   }

   public boolean doIHaveAccessToSettlement(Client client) {
      NetworkSettlementData settlementNetworkData = this.getSettlementNetworkData();
      return settlementNetworkData != null && settlementNetworkData.doIHaveAccess(client);
   }

   public ActiveSettlerThoughtsManager getActiveThoughts() {
      return this.activeThoughts;
   }

   public SettlerThoughtsList getStaticThoughts() {
      SettlerThoughtsList thoughts = new SettlerThoughtsList();
      boolean lastFoodEatenOverridden = false;
      boolean dietVarietyOverridden = false;
      if (this.levelSettler != null) {
         SettlementBed bed = this.levelSettler.getBed();
         if (bed != null) {
            thoughts.addAll(bed.getSettlerThoughts());
         } else {
            thoughts.addThought(SettlerThoughtRegistry.noBedThought);
         }

         for (SettlerPersonality personality : this.personalities) {
            SettlerThoughtsList personalityThoughts = personality.getThoughts(bed);
            if (personalityThoughts != null) {
               thoughts.addAll(personalityThoughts);
            }

            if (personality.preventsSharingRoomThought()) {
               thoughts.removeThought(SettlerThoughtRegistry.sharingRoomThought);
            }

            if (personality.preventsMissingFloorThought()) {
               thoughts.removeThought(SettlerThoughtRegistry.missingFloorThought);
            }

            lastFoodEatenOverridden = lastFoodEatenOverridden || personality.preventsLastFoodEatenThought();
            dietVarietyOverridden = dietVarietyOverridden || personality.preventsDietVarietyThought();
         }

         SettlerThoughtsList romanceThoughts = this.romancePersonality.getThoughts(bed);
         if (romanceThoughts != null) {
            thoughts.addAll(romanceThoughts);
         }

         thoughts.addThought(SettlerThoughtRegistry.qualityOfLifeThought);
         PopulationSettlerThought populationThought = SettlerThoughtRegistry.getPopulationThought(this.levelSettler.data.countTotalSettlers());
         if (populationThought != null) {
            thoughts.addThought(populationThought);
         }

         float recreationLevel = this.getRecreationLevel();
         if (recreationLevel >= 0.75F) {
            thoughts.addThought("recreationfulfilled");
         } else if (recreationLevel >= 0.25F) {
            thoughts.addThought("recreationsatisfied");
         } else {
            thoughts.addThought("recreationdeprived");
         }
      }

      if (!lastFoodEatenOverridden) {
         if (this.lastFoodEaten != null && this.lastFoodEaten.quality != null) {
            thoughts.addThought(this.lastFoodEaten.quality.getThought());
         } else {
            thoughts.addThought(SettlerThoughtRegistry.noFoodThought);
         }
      }

      if (!dietVarietyOverridden) {
         int differentFoodsEaten = (int)this.recentFoodItemIDsEaten.stream().distinct().count();
         SettlerThought dietThought = SettlerThoughtRegistry.getDietVarietyThought(differentFoodsEaten);
         if (dietThought != null) {
            thoughts.addThought(dietThought);
         }
      }

      return thoughts;
   }

   public void updateHappiness() {
      int oldHappiness = this.settlerHappiness;
      this.settlerHappiness = 0;

      for (SettlerThoughtStacks thought : this.getStaticThoughts()) {
         this.settlerHappiness = this.settlerHappiness + thought.getHappinessModifier(this);
      }

      if (this.isServer()) {
         this.romancePersonality.updateRelationship(this.getServer(), false);
      }

      this.settlerHappiness = this.settlerHappiness + this.getActiveThoughts().getTotalHappinessModifier();
      if (oldHappiness != this.settlerHappiness) {
         this.sendMovementPacket(false);
         if (this.settlerQualityOfLife >= 6) {
            NetworkSettlementData networkData = this.getSettlerSettlementNetworkData();
            if (networkData != null) {
               networkData.streamTeamMembers().forEach(c -> {
                  if (c.achievementsLoaded()) {
                     c.achievements().CLOUD_NINE.markCompleted(c);
                  }
               });
            }
         }
      }
   }

   public ArrayList<MoodFloatText.Moods> getMoods() {
      ArrayList<MoodFloatText.Moods> moodList = new ArrayList<>();
      if (!this.hasActiveJob()) {
         if (this.hungerLevel <= 0.15F) {
            moodList.add(MoodFloatText.Moods.HUNGRY);
         }

         if (!this.isOnStrike() && !this.getWorldEntity().isNight() && this.allowFullInventoryMood && this.getWorkInventory().isFull()) {
            moodList.add(MoodFloatText.Moods.INVENTORY_FULL);
         }
      }

      if (this.levelSettler != null && this.canSubmitNoBedNotification()) {
         SettlementBed bed = this.levelSettler.getBed();
         if (bed != null) {
            SettlementRoom room = this.levelSettler.getBed().getRoom();
            if (room == null) {
               moodList.add(MoodFloatText.Moods.BED_OUTSIDE);
            }
         } else {
            moodList.add(MoodFloatText.Moods.NO_BED);
         }
      }

      return moodList;
   }

   public static int getBonusHealth(int armor) {
      float healthPerArmorValue = 25.0F;
      return (int)(armor * healthPerArmorValue);
   }

   public int getBonusHealth() {
      ActiveBuff equipmentBuff = this.buffManager.getBuff(BuffRegistry.EQUIPMENT_BUFF);
      int armor = equipmentBuff == null ? 0 : equipmentBuff.getModifier(BuffModifiers.ARMOR_FLAT);
      return getBonusHealth(armor);
   }

   public void updateStats(boolean updateHealth) {
      int lastHealth = this.getHealth();
      float healthPercent = (float)this.getHealth() / this.getMaxHealth();
      if (!this.isSettler() && !this.isVisitor()) {
         this.setMaxHealth(this.nonSettlerHealth);
         this.setArmor(50);
         this.hitCooldown = 0;
      } else {
         this.setMaxHealth(this.settlerHealth + this.getBonusHealth());
         this.setArmor(0);
         this.hitCooldown = 250;
      }

      this.buffManager.forceUpdateBuffs();
      if (updateHealth) {
         this.setHealth(Math.max(lastHealth, (int)(this.getMaxHealth() * healthPercent)));
      }
   }

   public boolean isFriendlyClient(NetworkClient client) {
      if (client != null && client.playerMob != null) {
         if (this.isVisitor()) {
            NetworkSettlementData settlement = this.getVisitorSettlementNetworkData();
            if (settlement != null && settlement.isClientPartOf(client)) {
               return true;
            }
         }

         long ownerAuth = this.owner.get();
         if (ownerAuth != -1L) {
            if (ownerAuth == client.authentication) {
               return true;
            }

            if (client.pvpEnabled()) {
               NetworkClient ownerClient = null;
               if (this.isClient()) {
                  ownerClient = this.getClient().getClientByAuth(ownerAuth);
               } else if (this.isServer()) {
                  ownerClient = this.getServer().getClientByAuth(ownerAuth);
               }

               if (ownerClient != null && !ownerClient.pvpEnabled()) {
                  return true;
               }

               if (ownerClient == null && !this.getWorldSettings().forcedPvP) {
                  return true;
               }
            } else if (!this.getWorldSettings().forcedPvP) {
               return true;
            }
         }

         return this.isSameTeam(client.playerMob);
      } else {
         return false;
      }
   }

   public boolean isFriendlyHuman(HumanMob other) {
      if (this.isVisitor() || other.isVisitor()) {
         return true;
      }

      if (this.isVisitor(other.settlementUniqueID)) {
         return true;
      }

      if (other.isVisitor(this.settlementUniqueID)) {
         return true;
      }

      int myTeam = this.getTeam();
      int otherTeam = other.getTeam();
      if (myTeam == -1 && otherTeam == -1) {
         return true;
      }

      if (myTeam >= 0 && myTeam == otherTeam) {
         return true;
      }

      long ownerAuth = this.owner.get();
      long targetOwnerAuth = other.owner.get();
      if (ownerAuth == targetOwnerAuth) {
         return true;
      }

      if (!this.getWorldSettings().forcedPvP && ownerAuth != -1L && targetOwnerAuth != -1L) {
         NetworkClient ownerClient = null;
         NetworkClient targetOwnerClient = null;
         if (this.isClient()) {
            ownerClient = this.getClient().getClientByAuth(ownerAuth);
            targetOwnerClient = this.getClient().getClientByAuth(targetOwnerAuth);
         } else if (this.isServer()) {
            ownerClient = this.getServer().getClientByAuth(ownerAuth);
            targetOwnerClient = this.getServer().getClientByAuth(targetOwnerAuth);
         }

         return ownerClient != null && targetOwnerClient != null ? !ownerClient.pvpEnabled() || !targetOwnerClient.pvpEnabled() : true;
      } else {
         return false;
      }
   }

   public GameMessage willJoinAdventureParty(ServerClient client) {
      return this.isOnStrike() ? new LocalMessage("ui", "settlerjoinpartynothappy") : null;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      Stream<ModifierValue<?>> out = super.getDefaultModifiers();
      Level level = this.getLevel();
      if (level != null) {
         for (SettlerPersonality personality : this.personalities) {
            Stream<ModifierValue<?>> personalityModifiers = personality.getDefaultModifiers();
            if (personalityModifiers != null) {
               out = Stream.concat(out, personalityModifiers);
            }
         }
      }

      if (this.downedState == null) {
         boolean isSettler = this.isSettler();
         if (!isSettler) {
            out = Stream.concat(out, Stream.of(new ModifierValue<>(BuffModifiers.ALL_DAMAGE, 2.0F), new ModifierValue<>(BuffModifiers.ARMOR_PEN_FLAT, 1000)));
         }

         if (level != null) {
            if (isSettler) {
               int qualityOfLifeLevel = this.getSettlerQualityOfLife();
               if (qualityOfLifeLevel > 0) {
                  Stream<ModifierValue<?>> qolStream = QOL_MODIFIERS.stream();
                  if (qualityOfLifeLevel > 1) {
                     qolStream = qolStream.map(modifier -> modifier.stack(qualityOfLifeLevel));
                  }

                  out = Stream.concat(out, qolStream);
               }

               if (this.adventureParty.isInAdventureParty()) {
                  out = Stream.concat(
                     out, Stream.of(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE).max(0.0F), new ModifierValue<>(BuffModifiers.POISON_DAMAGE).max(0.0F))
                  );
               }
            }

            NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
            if (settlement != null && settlement.isRaidActive() && settlement.isTileWithinBounds(this.getTileX(), this.getTileY())) {
               out = Stream.concat(out, Stream.of(new ModifierValue<>(BuffModifiers.SPEED_FLAT, 10.0F)));
            }

            if (this.getHealthPercent() <= 0.4F) {
               out = Stream.concat(out, Stream.of(new ModifierValue<>(BuffModifiers.KNOCKBACK_INCOMING_MOD, 0.5F)));
            }
         }

         return out;
      } else {
         return Stream.concat(
            out,
            Stream.of(
               new ModifierValue<>(BuffModifiers.PARALYZED, true),
               new ModifierValue<>(BuffModifiers.INCOMING_DAMAGE_MOD, level != null && level.isIncursionLevel() ? 0.01F : 0.1F),
               new ModifierValue<>(BuffModifiers.FRICTION, -0.5F),
               new ModifierValue<>(BuffModifiers.POISON_DAMAGE, 1.0F).max(0.0F),
               new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 1.0F).max(0.0F),
               new ModifierValue<>(BuffModifiers.FROST_DAMAGE, 1.0F).max(0.0F),
               new ModifierValue<>(BuffModifiers.BLEED_DAMAGE, 1.0F).max(0.0F)
            )
         );
      }
   }

   @Override
   public float getRegenFlat() {
      if (this.adventureParty.isInAdventureParty() && !this.isSettlerWithinSettlement()) {
         return super.getRegenFlat();
      }

      int maxHealth = this.getMaxHealth();
      return Math.max((float)maxHealth / this.maxSecondsFullHealthRegen, super.getRegenFlat());
   }

   @Override
   public boolean canTarget(Mob target) {
      if (target.isPlayer && this.isFriendlyClient(((PlayerMob)target).getNetworkClient())) {
         return false;
      } else {
         return target instanceof HumanMob ? !this.isFriendlyHuman((HumanMob)target) : super.canTarget(target);
      }
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      if (this.isDowned()) {
         return false;
      } else {
         return attackerClient != null && this.isFriendlyClient(attackerClient) ? false : super.canBeTargeted(attacker, attackerClient);
      }
   }

   @Override
   public boolean isSameTeam(Mob other) {
      return other instanceof HumanMob ? this.isFriendlyHuman((HumanMob)other) : super.isSameTeam(other);
   }

   public boolean isVisitor() {
      return this.visitorSettlementUniqueID != 0;
   }

   public boolean isVisitor(int settlementUniqueID) {
      return this.visitorSettlementUniqueID != 0 && this.visitorSettlementUniqueID == settlementUniqueID;
   }

   public int getVisitorToSettlementUniqueID() {
      return this.visitorSettlementUniqueID;
   }

   @Override
   public Inventory getInventory() {
      return this.equipmentInventory;
   }

   public InventoryItem getArmorItem(int slot) {
      return this.equipmentInventory.getItem(slot);
   }

   public InventoryItem getCosmeticArmorItem(int slot) {
      return this.equipmentInventory.getItem(3 + slot);
   }

   public InventoryItem getDisplayArmor(int slot, InventoryItem defaultItem) {
      InventoryItem cosmeticItem = this.getCosmeticArmorItem(slot);
      if (cosmeticItem != null && cosmeticItem.item.isArmorItem()) {
         return cosmeticItem;
      }

      InventoryItem armorItem = this.getArmorItem(slot);
      return armorItem != null && armorItem.item.isArmorItem() ? armorItem : defaultItem;
   }

   public InventoryItem getInventoryWeapon() {
      return this.equipmentInventory.getItem(6);
   }

   @Override
   public ItemAttackSlot getCurrentSelectedAttackSlot() {
      return new HumanMobItemAttackSlot(this);
   }

   @Override
   public boolean hasValidSummonItem(Item item, CheckSlotType slotType) {
      if (this.getTimeSinceLastCombat() > 10000L) {
         return false;
      }

      if (slotType != null) {
         switch (slotType) {
            case HELMET:
               InventoryItem helmetItem = this.getArmorItem(0);
               return helmetItem != null && helmetItem.item.getID() == item.getID();
            case CHEST:
               InventoryItem chestItem = this.getArmorItem(1);
               return chestItem != null && chestItem.item.getID() == item.getID();
            case FEET:
               InventoryItem feetItem = this.getArmorItem(2);
               return feetItem != null && feetItem.item.getID() == item.getID();
         }
      }

      InventoryItem weapon = this.getInventoryWeapon();
      return weapon != null && weapon.item.getID() == item.getID();
   }

   @Override
   protected void doWasHitLogic(MobWasHitEvent event) {
      super.doWasHitLogic(event);
      if (!this.isClient()) {
         for (SettlerPersonality personality : this.personalities) {
            personality.onMobWasHit(event);
         }
      }
   }

   @Override
   protected void doHasAttackedLogic(MobWasHitEvent event) {
      super.doHasAttackedLogic(event);
      if (!this.isClient()) {
         for (SettlerPersonality personality : this.personalities) {
            personality.onMobHasAttacked(event);
         }
      }
   }

   @Override
   protected void doHasKilledTarget(MobWasKilledEvent event) {
      super.doHasKilledTarget(event);
      ServerClient serverClient = this.adventureParty.getServerClient();
      if (serverClient != null) {
         serverClient.adventureParty.doHasKilledTarget(event);
      }
   }

   public void doAdventurePartyHasKilledTarget(ServerClient partyOwner, MobWasKilledEvent event) {
      for (SettlerPersonality personality : this.getPersonalities()) {
         personality.onAdventurePartyHasKilledTarget(partyOwner, event);
      }

      if (this.romancePersonality != null) {
         PlayerRomanceManager playerRomanceManager = this.romancePersonality.getPlayerManager(partyOwner, false);
         if (playerRomanceManager != null) {
            playerRomanceManager.onMobKilled(partyOwner, event);
         }
      }
   }

   @Override
   public int getAvailableArrows(String purpose) {
      return 1000;
   }

   @Override
   public int getAvailableBullets(String purpose) {
      return 1000;
   }

   @Override
   public int getAvailableAmmo(Item[] items, String purpose) {
      return 1000;
   }

   @Override
   public Item getFirstAvailableArrow(String purpose) {
      return ItemRegistry.getItem("stonearrow");
   }

   @Override
   public Item getFirstAvailableBullet(String purpose) {
      return ItemRegistry.getItem("simplebullet");
   }

   @Override
   public Item getFirstAvailableAmmo(Item[] items, String purpose) {
      return items.length == 0 ? null : items[0];
   }

   @Override
   public AmmoConsumed removeAmmo(Item item, int amount, String purpose) {
      return new AmmoConsumed(amount, 0.0F);
   }

   @Override
   public Point getJobSearchTile() {
      return this.home != null ? new Point(this.home.x, this.home.y) : new Point(this.getTileX(), this.getTileY());
   }

   @Override
   public Rectangle getJobSearchBounds() {
      if (this.isSettler()) {
         NetworkSettlementData settlementNetworkData = this.getSettlementNetworkData();
         if (settlementNetworkData != null) {
            return settlementNetworkData.getLevelRectangle();
         }
      }

      return EntityJobWorker.super.getJobSearchBounds();
   }

   @Override
   public JobSequence findJob(boolean ignoreRecreationJobs, Consumer<JobFinder> finderMod) {
      if (!this.adventureParty.isInAdventureParty() && !this.hasCommandOrders() && this.getWorldEntity().isNight()) {
         return null;
      }

      if (this.objectUser != null) {
         return null;
      }

      NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
      if (settlement != null) {
         if (settlement.isRaidActive()) {
            return null;
         }

         if (settlement.isDisbanding()) {
            return null;
         }

         if (!settlement.hasOwner()) {
            return null;
         }
      }

      if (this.isHiding) {
         return null;
      }

      if (this.isVisitor()) {
         return null;
      }

      if (!this.adventureParty.isInAdventureParty() && !this.hasCommandOrders() && this.attemptStartStrike(true)) {
         return null;
      }

      JobSequence foundJob = EntityJobWorker.super.findJob(ignoreRecreationJobs, finderMod);
      if (foundJob == null && this.allowFullInventoryMood && this.getWorkInventory().isFull()) {
         this.submitFullInventoryNotification();
      } else {
         this.removeFullInventoryNotification();
      }

      return foundJob;
   }

   @Override
   public boolean allowsFindingJobType(JobType jobType) {
      for (SettlerPersonality personality : this.personalities) {
         if (!personality.allowsFindingJobType(jobType)) {
            return false;
         }
      }

      return EntityJobWorker.super.allowsFindingJobType(jobType);
   }

   @Override
   public ZoneTester getJobRestrictZone() {
      if (this.levelSettler != null) {
         return this.levelSettler.isTileInSettlementBoundsAndRestrictZoneTester();
      }

      Level level = this.getLevel();
      SettlementsWorldData settlementsData = SettlementsWorldData.getSettlementsData(this);
      ZoneTester noSettlementTester = (tileX, tileY) -> settlementsData.getNetworkDataAtTile(level.getIdentifier(), tileX, tileY) == null;
      ZoneTester tester = EntityJobWorker.super.getJobRestrictZone();
      return tester.and(noSettlementTester);
   }

   @Override
   public JobTypeHandler getJobTypeHandler() {
      return this.jobTypeHandler;
   }

   @Override
   public Mob getJobFollowingMob() {
      return this.commandFollowMob;
   }

   @Override
   public Stream<FoundJobGenerator<?>> streamExtraJobs() {
      Stream<FoundJobGenerator<?>> out = null;

      for (SettlerPersonality personality : this.personalities) {
         Stream<FoundJobGenerator<?>> personalityJobs = personality.streamExtraJobs();
         if (personalityJobs != null) {
            if (out == null) {
               out = personalityJobs;
            } else {
               out = Stream.concat(out, personalityJobs);
            }
         }
      }

      return out;
   }

   @Override
   public boolean isFullInventoryNotificationStillValid() {
      return this.allowFullInventoryMood && this.getWorkInventory().isFull();
   }

   public boolean isInventoryFull(boolean changeAllowMood) {
      boolean out = this.getWorkInventory().isFull();
      if (changeAllowMood) {
         this.allowFullInventoryMood = out;
      }

      return out;
   }

   @Override
   public WorkInventory getWorkInventory() {
      return new WorkInventory() {
         @Override
         public ListIterator<InventoryItem> listIterator() {
            return HumanMob.this.workInventory.listIterator();
         }

         @Override
         public Iterable<InventoryItem> items() {
            return HumanMob.this.workInventory;
         }

         @Override
         public Stream<InventoryItem> stream() {
            return HumanMob.this.workInventory.stream();
         }

         @Override
         public void markDirty() {
            HumanMob.this.sendWorkUpdatePacket();
         }

         @Override
         public void add(InventoryItem item) {
            if (HumanMob.this.isSettler()) {
               item.combineOrAddToList(HumanMob.this.getLevel(), null, HumanMob.this.workInventory, "add");
               this.markDirty();
            }
         }

         @Override
         public int getCanAddAmount(InventoryItem item) {
            int totalStacks = this.getTotalItemStacks();
            if (totalStacks > 4) {
               return 0;
            }

            float currentBrokerValue = HumanMob.this.workInventory.stream().reduce(0.0F, (value, i) -> value + i.getBrokerValue(), Float::sum);
            float remainingValue = 300.0F - currentBrokerValue;
            if (remainingValue < 0.0F) {
               return 0;
            }

            float singleItemBrokerValue = item.item.getBrokerValue(item);
            int totalItemsToPickUp = Math.min((int)(remainingValue / singleItemBrokerValue), item.getAmount());
            return totalStacks == 0 ? Math.max(totalItemsToPickUp, 1) : totalItemsToPickUp;
         }

         @Override
         public boolean isFull() {
            if (this.getTotalItemStacks() > 4) {
               return true;
            }

            float brokerValue = HumanMob.this.workInventory.stream().reduce(0.0F, (value, i) -> value + i.getBrokerValue(), Float::sum);
            return brokerValue > 300.0F;
         }

         @Override
         public int getTotalItemStacks() {
            return HumanMob.this.workInventory.size();
         }

         @Override
         public boolean isEmpty() {
            return HumanMob.this.workInventory.isEmpty();
         }
      };
   }

   public long getShopSeed() {
      return this.getWorldEntity().getDay() * 191L * this.settlerSeed;
   }

   @Override
   public boolean hasActiveJob() {
      return this.ai.blackboard.getObject(ActiveJob.class, "currentJob") != null;
   }

   @Override
   public boolean isInWorkAnimation() {
      return this.isAttacking;
   }

   @Override
   public boolean isJobCancelled() {
      return this.cancelJob;
   }

   @Override
   public void resetJobCancelled() {
      this.cancelJob = false;
   }

   public void cancelJob() {
      this.cancelJob = true;
   }

   public float getExpeditionTimeModifier() {
      float modifier = 1.0F;

      for (SettlerPersonality personality : this.personalities) {
         modifier *= personality.getExpeditionTimeModifier();
      }

      return modifier;
   }

   public void onExpeditionStarted() {
      for (SettlerPersonality personality : this.personalities) {
         personality.onExpeditionStarted();
      }
   }

   @Override
   public void sendMovementPacket(boolean isDirect) {
      super.sendMovementPacket(isDirect);
      this.lastSettlerRecreationSyncLevel = this.settlerRecreationLevel;
   }

   public void tickRecreationUsage() {
      if (!this.isOnStrike()) {
         if (this.getCurrentMission() == null) {
            if (!this.adventureParty.isInAdventureParty()) {
               float usageModifier = 1.0F;
               if (this.isBeingInteractedWith()) {
                  usageModifier *= 0.25F;
               }

               if (this.getWorldEntity().isNight()) {
                  usageModifier *= 0.25F;
                  if (this.settlerRecreationLevel <= 0.05F) {
                     usageModifier = 0.0F;
                  }
               }

               if (this.settlerRecreationLevel < 0.3F) {
                  usageModifier *= 0.5F;
               }

               if (this.settlerRecreationLevel < 0.15F) {
                  usageModifier *= 0.5F;
               }

               this.useRecreation(50.0F, usageModifier);
               if (this.isServer() && !this.isOnStrike() && !this.getWorldEntity().isNight()) {
                  this.recreationStrikeCooldownBuffer -= 50;
                  if (this.recreationStrikeCooldownBuffer <= 0) {
                     this.attemptStartStrike(false);
                     this.resetRecreationStrikeCooldownBuffer();
                  }
               }

               if (this.settlerRecreationLevel >= 0.3F) {
                  this.resetRecreationStrikeCooldownBuffer();
               }
            }
         }
      }
   }

   public void resetRecreationStrikeCooldownBuffer() {
      this.recreationStrikeCooldownBuffer = 420000;
   }

   @Override
   public void useRecreation(float milliseconds, float modifier) {
      float delta = milliseconds / 1000.0F / SECONDS_TO_EMPTY_RECREATION * modifier;
      this.settlerRecreationLevel = Math.max(this.settlerRecreationLevel - delta, 0.0F);
      this.recreationExhaustionManager.useRecreation(delta);
   }

   @Override
   public void regenRecreation(float milliseconds, float modifier, String exhaustionIdentifier) {
      float delta = milliseconds / 1000.0F / SECONDS_TO_MAX_RECREATION * modifier;
      float exhaustionModifier;
      if (exhaustionIdentifier == null) {
         exhaustionModifier = 1.0F;
      } else {
         exhaustionModifier = Math.max(this.recreationExhaustionManager.getRecreationRegenModifier(exhaustionIdentifier), 0.05F);
      }

      this.settlerRecreationLevel = Math.min(this.settlerRecreationLevel + delta * exhaustionModifier, 1.0F);
      if (exhaustionIdentifier != null) {
         this.recreationExhaustionManager.regenRecreation(delta * exhaustionModifier, exhaustionIdentifier);
      }
   }

   @Override
   public float getRecreationRegenModifier(String identifier) {
      return this.recreationExhaustionManager.getRecreationRegenModifier(identifier);
   }

   public float getRecreationLevel() {
      return this.settlerRecreationLevel;
   }

   @Override
   public boolean wantsToDoRecreation() {
      return this.settlerRecreationLevel <= 0.4F || this.getWorldEntity().getTimeOfDay() == WorldEntity.TimeOfDay.EVENING;
   }

   public HumanMob.StrikeChance wantsToGoOnStrikeChance() {
      if (!(this.getHungerLevel() <= 0.0F) || !this.isOnStrike() && this.hungerStrikeCooldownBuffer > 0) {
         if (!(this.getRecreationLevel() <= 0.0F) || !this.isOnStrike() && this.recreationStrikeCooldownBuffer > 0) {
            if (this.getSettlerQualityOfLife() <= 0) {
               float happinessClamp = GameMath.clamp(this.getSettlerHappiness(), 0.0F, HAPPINESS_QOL_DECREASE_THRESHOLD);
               happinessClamp = GameMath.limit(happinessClamp, 0.0F, 1.0F);
               return new HumanMob.StrikeChance(1.0F - happinessClamp, new LocalMessage("settlement", "onstrike"));
            } else {
               return null;
            }
         } else {
            return new HumanMob.StrikeChance(1.0F, new LocalMessage("settlement", "onstrikerecreation"));
         }
      } else {
         HumanMob.StrikeChance strikeChance = new HumanMob.StrikeChance(1.0F, new LocalMessage("settlement", "onstrikehungry"));
         strikeChance.onStrikeStarted = () -> this.addLeaveBuffer(0.2F);
         return strikeChance;
      }
   }

   @Override
   public boolean isOnStrike() {
      return this.isOnStrike.get();
   }

   @Override
   public boolean attemptStartStrike(boolean activateOthers) {
      if (this.isSettlerWithinSettlement() && !this.isOnStrike()) {
         HumanMob.StrikeChance strikeChance = this.wantsToGoOnStrikeChance();
         if (strikeChance != null && strikeChance.chance > 0.0F && GameRandom.globalRandom.getChance(strikeChance.chance)) {
            this.isOnStrike.set(true);
            this.endStrikeBuffer = 300000;
            this.cancelJob();
            if (strikeChance.onStrikeStarted != null) {
               strikeChance.onStrikeStarted.run();
            }

            if (activateOthers) {
               ServerSettlementData data = this.getSettlerSettlementServerData();
               if (data != null) {
                  for (LevelSettler settler : data.getSettlers()) {
                     SettlerMob mob = settler.getMob();
                     if (mob instanceof EntityJobWorker) {
                        ((EntityJobWorker)mob).attemptStartStrike(false);
                     }
                  }
               }
            }

            return true;
         }
      }

      return false;
   }

   @Override
   public void setActivity(String type, int priority, GameMessage description) {
      this.currentActivity.setActivity(type, priority, description);
   }

   @Override
   public void clearActivity(String type) {
      this.currentActivity.clearActivity(type);
   }

   @Override
   public void showPickupAnimation(int x, int y, Item item, int attackAnimTime, boolean reduceByWorkSpeed) {
      this.attackSprite(x, y, item, reduceByWorkSpeed ? this.getWorkAnimTime(attackAnimTime) : attackAnimTime, true);
   }

   @Override
   public void showPlaceAnimation(int x, int y, Item item, int attackAnimTime, boolean reduceByWorkSpeed) {
      this.attackSprite(x, y, item, reduceByWorkSpeed ? this.getWorkAnimTime(attackAnimTime) : attackAnimTime, false);
   }

   @Override
   public void showWorkAnimation(int x, int y, Item item, int attackAnimTime, boolean reduceByWorkSpeed) {
      int finalAnimTime = reduceByWorkSpeed ? this.getWorkAnimTime(attackAnimTime) : attackAnimTime;
      long timeSinceLastWorkRequest = this.getLocalTime() - this.lastWorkAnimationRequest;
      if (timeSinceLastWorkRequest >= finalAnimTime / 2) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(x);
         writer.putNextInt(y);
         writer.putNextBoolean(item != null);
         if (item != null) {
            writer.putNextShortUnsigned(item.getID());
         }

         writer.putNextInt(finalAnimTime);
         this.itemWorkSpriteAbility.runAndSend(content);
         this.lastWorkAnimationRequest = this.getLocalTime();
      }
   }

   @Override
   public void showAttackAnimation(int x, int y, Item item, int attackAnimTime, boolean reduceByWorkSpeed) {
      int finalAnimTime = reduceByWorkSpeed ? this.getWorkAnimTime(attackAnimTime) : attackAnimTime;
      this.attackItem(x, y, item, finalAnimTime, finalAnimTime);
   }

   @Override
   public float getWorkSpeedModifier() {
      return this.buffManager.getModifier(BuffModifiers.WORK_SPEED);
   }

   public int getWorkAnimTime(int animTime) {
      return (int)(animTime * this.getWorkAnimSpeedMultiplier());
   }

   @Override
   public void showHoldAnimation(Item item, int timeout) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextBoolean(item != null);
      if (item != null) {
         writer.putNextShortUnsigned(item.getID());
         writer.putNextInt(timeout);
      }

      this.itemHoldSpriteAbility.runAndSend(content);
   }

   @Override
   public void clearHoldAnimation() {
      this.showHoldAnimation(null, 0);
   }

   public void showDanceAnimation(int timeout) {
      this.setDanceAnimationTimeoutAbility.runAndSend(timeout);
   }

   public void stopDanceAnimationNow() {
      this.setDanceAnimationTimeoutAbility.runAndSend(0);
   }

   @Override
   public void giveBaitBack(BaitItem bait) {
   }

   @Override
   public void stopFishing() {
      this.isAttacking = false;
   }

   @Override
   public void showFishingWaitAnimation(FishingRodItem fishingRod, int targetX, int targetY) {
      this.showAttack(targetX, targetY, false);
      InventoryItem invItem = FishingPoleHolding.setGNDData(new InventoryItem("fishinghold"), fishingRod);
      this.showItemAttack(invItem, targetX, targetY, 0, 0, null);
      this.attackAnimTime = 5000;
   }

   @Override
   public boolean isFishingSwingDone() {
      this.getAttackAnimProgress();
      return !this.isAttacking;
   }

   @Override
   public void giveCaughtItem(FishingEvent event, InventoryItem item) {
      this.getWorkInventory().add(item);
   }

   @Override
   public boolean shouldClearAfterTime(long time) {
      return time < this.clearRopedTime;
   }

   public void clearRopedMobs() {
      this.clearRopedTime = this.getTime() + 1000L;
   }

   @Override
   public HumanDrawOptions getChatterFaceDrawOptions() {
      HumanDrawOptions humanOptions = new HumanDrawOptions(null, this.look, !this.customLook);
      this.setDefaultArmor(humanOptions);
      return humanOptions;
   }

   @Override
   public JobWorkerChatterHandler getCurrentChatterHandler() {
      HumanAI<?> humanAI = (HumanAI<?>)this.ai.tree;
      JobWorkerChatterHandler jobHandler = humanAI.humanJobsAINode.getChatterHandler();
      return jobHandler != null ? jobHandler : humanAI.humanInteractAINode.getChatterHandler();
   }

   @Override
   public GameMessage getChatterSimpleName() {
      return new StaticMessage(this.getSettlerName());
   }

   public int getRecruitedToSettlementUniqueID(ServerClient client) {
      if (this.recruitReservedSettlementUniqueID != 0 && this.getTime() < this.recruitReservedTimeout) {
         CachedSettlementData cachedData = SettlementsWorldData.getSettlementsData(this).getCachedData(this.recruitReservedSettlementUniqueID);
         if (cachedData != null) {
            return this.recruitReservedSettlementUniqueID;
         }

         this.recruitReservedSettlementUniqueID = 0;
      }

      if (this.downedState == HumanMob.DownedState.TRAPPED) {
         return 0;
      } else {
         return this.isVisitor() ? this.visitorSettlementUniqueID : 0;
      }
   }

   public boolean isValidRecruitment(CachedSettlementData settlement, ServerClient client) {
      return true;
   }

   public boolean shouldTeleportToLevelOnRecruited(ServerClient client, ServerSettlementData data, LevelSettler settler) {
      return !this.isDowned();
   }

   public void onRecruited(ServerClient client, ServerSettlementData data, LevelSettler settler) {
      if (client != null && client.achievementsLoaded()) {
         client.achievements().HEADHUNTER.markCompleted(client);
      }

      if (this.villagerData != null) {
         OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(this.getLevel(), true);
         villageData.addNPCVillager(this.villagerData);
         this.villagerData = null;
      }

      if (this.savedSettlerSettings != null && this.savedSettlerSettings.settlementUniqueID == data.uniqueID) {
         this.savedSettlerSettings.applyToSettler(settler);
      } else {
         this.selfManageEquipment.set(data.newSettlerSelfManageEquipment);
      }

      this.savedSettlerSettings = null;
      this.clearRopedMobs();
      this.buffManager.addBuff(new ActiveBuff(BuffRegistry.SPAWN_INVINCIBILITY, this, 5.0F, null), true);
      if (this.isServer() && this.getHealth() != this.getMaxHealth()) {
         this.setHealthHidden(Math.max(this.getMaxHealth() / 4, this.getHealth()));
      }

      this.updateStats(true);
      this.activeThoughts.addThoughtSeconds(SettlerThoughtRegistry.recentlyRecruitedThought, 1800.0F);
      this.resetHungerStrikeCooldownBuffer();
      this.resetRecreationStrikeCooldownBuffer();
   }

   @Override
   public boolean hasCommandOrders() {
      return this.getLevel() != null && this.isClient()
         ? this.clientHasCommandOrders.get()
         : this.commandGuardPoint != null || this.commandFollowMob != null || this.commandAttackMob != null;
   }

   @Override
   public boolean canBeCommandPartySelected(Client client) {
      if (client.adventureParty.contains(this)) {
         return true;
      }

      int myTeam = client.getTeam();
      return myTeam == -1 ? this.owner.get() == GameAuth.getAuthentication() : this.team.get() == myTeam;
   }

   @Override
   public boolean canBeCommanded(Client client) {
      if (this.getCurrentMission() != null) {
         return false;
      }

      if (client.adventureParty.contains(this)) {
         return true;
      }

      if (this.isOnStrike()) {
         NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
         if (settlement != null && !settlement.isRaidActiveOrApproaching() && settlement.isTileWithinBounds(this.getTileX(), this.getTileY())) {
            return false;
         }
      }

      int myTeam = client.getTeam();
      return myTeam == -1 ? this.owner.get() == GameAuth.getAuthentication() : this.team.get() == myTeam;
   }

   @Override
   public boolean canBeCommanded(ServerClient client, boolean onlyForSettings) {
      if (this.getCurrentMission() != null && !onlyForSettings) {
         return false;
      }

      if (client.adventureParty.contains(this)) {
         return true;
      }

      if (this.isOnStrike() && !onlyForSettings) {
         NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
         if (settlement != null && !settlement.isRaidActiveOrApproaching() && settlement.isTileWithinBounds(this.getTileX(), this.getTileY())) {
            return false;
         }
      }

      NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
      return settlement == null ? false : settlement.doesClientHaveAccess(client);
   }

   @Override
   public void clearCommandsOrders(ServerClient commander) {
      if (this.adventureParty.isInAdventureParty()) {
         this.adventureParty.clear(true);
         if (!this.isSettlerWithinSettlement()) {
            this.cancelJob();
            this.commandGuard(null, this.getX(), this.getY());
            if (this.getHealthPercent() <= 0.5F) {
               SettlersWorldData settlersData = SettlersWorldData.getSettlersData(this.getLevel().getServer());
               settlersData.returnToSettlement(this, false);
            }

            return;
         }
      }

      boolean hadCommands = this.hasCommandOrders();
      this.commandFollowMob = null;
      this.commandGuardPoint = null;
      this.commandMoveToGuardPoint = false;
      this.commandMoveToFollowPoint = false;
      this.commandAttackMob = null;
      this.sendNextMovementPacket = true;
      this.stopMovingIn();
      this.adventureParty.clear(true);
      this.ai.blackboard.submitEvent("resetTarget", new AIEvent());
      if (hadCommands) {
         this.ai.blackboard.submitEvent("wanderNow", new AIEvent());
         this.cancelJob();
      }
   }

   @Override
   public void commandFollow(ServerClient commander, Mob mob) {
      this.cancelJob();
      this.commandFollowMob = mob;
      this.commandGuardPoint = null;
      this.commandMoveToGuardPoint = false;
      this.commandMoveToFollowPoint = true;
      this.commandAttackMob = null;
      this.sendNextMovementPacket = true;
      this.stopMovingIn();
      this.ai.blackboard.submitEvent("resetTarget", new AIEvent());
      this.ai.blackboard.submitEvent("newCommandSet", new AIEvent());
      if (this.canJoinAdventureParties
         && commander != null
         && mob == commander.playerMob
         && this.willJoinAdventureParty(commander) == null
         && this.adventureParty.getServerClient() != commander) {
         this.adventureParty.set(commander);
      }

      this.resetCommandsBuffer = 0;
   }

   @Override
   public void commandGuard(ServerClient commander, int x, int y) {
      this.cancelJob();
      this.commandFollowMob = null;
      this.commandGuardPoint = new Point(x, y);
      this.commandMoveToGuardPoint = true;
      this.commandMoveToFollowPoint = false;
      this.commandAttackMob = null;
      this.sendNextMovementPacket = true;
      this.stopMovingIn();
      if (commander != null && !this.isSamePlace(commander.playerMob)) {
         this.getLevel().entityManager.changeMobLevel(this, commander.playerMob.getLevel(), commander.playerMob.getX(), commander.playerMob.getY(), true);
         this.ai.blackboard.mover.stopMoving(this);
      }

      if (this.isSettlerWithinSettlement()) {
         this.adventureParty.clear(true);
      } else if (this.canJoinAdventureParties
         && commander != null
         && this.willJoinAdventureParty(commander) == null
         && this.adventureParty.getServerClient() != commander) {
         this.adventureParty.set(commander);
      }

      this.ai.blackboard.submitEvent("resetTarget", new AIEvent());
      this.ai.blackboard.submitEvent("newCommandSet", new AIEvent());
      this.resetCommandsBuffer = 600000;
   }

   @Override
   public void commandAttack(ServerClient commander, Mob mob) {
      if (mob == null || mob.canBeTargeted(this, null)) {
         this.commandMoveToGuardPoint = false;
         this.commandMoveToFollowPoint = false;
         this.stopMovingIn();
         this.commandAttackMob = mob;
         this.ai.blackboard.submitEvent("resetTarget", new AIEvent());
         this.ai.blackboard.submitEvent("newCommandSet", new AIEvent());
         this.resetCommandsBuffer = 600000;
      }
   }

   @Override
   public void setHideOnLowHealth(boolean shouldHideInside) {
      this.hideOnLowHealth.set(shouldHideInside);
   }

   @Override
   public boolean getHideOnLowHealth() {
      return this.hideOnLowHealth.get();
   }

   @Override
   public Mob getMob() {
      return this;
   }

   @Override
   public GameMessage getCommandMobSingleName() {
      return this.getLocalization();
   }

   @Override
   public GameMessage getCommandMobTypeName() {
      return new LocalMessage("ui", "commandtypesettlers");
   }

   @Override
   public HumanDrawOptions getCommandFaceDrawOptions() {
      HumanDrawOptions humanOptions = new HumanDrawOptions(null, this.look, !this.customLook);
      this.setDefaultArmor(humanOptions);
      return humanOptions;
   }

   @Override
   public void addCommandFormHeader(ComponentListContainer<? super FormComponent> container, FormFlow flow, int maxWidth) {
      DialogueForm.addSettlerHeader(this, true, -1, maxWidth, container, flow);
      if (this.isSettler()) {
         container.addComponent(flow.nextY(new FormLocalLabel("ui", "settlerhappinessbar", new FontOptions(16), -1, 10, 0), 5));
         container.addComponent(flow.nextY(new FormSettlerHappinessBar(10, 0, maxWidth - 20, true) {
            @Override
            public int getHappinessLevel() {
               return HumanMob.this.getSettlerHappiness();
            }

            @Override
            public String getSettlerName() {
               return HumanMob.this.getSettlerName();
            }
         }, 8));
         int needsBarSplitWidth = 20;
         int needsBarWidth = (maxWidth - 20 - needsBarSplitWidth) / 2;
         FormLocalLabel hungerLabel = container.addComponent(new FormLocalLabel("ui", "settlerhungerbar", new FontOptions(16), -1, 10, 0, needsBarWidth));
         FormLocalLabel recreationLabel = container.addComponent(
            new FormLocalLabel("ui", "settlerrecreationbar", new FontOptions(16), -1, 10 + needsBarWidth + needsBarSplitWidth, 0, needsBarWidth)
         );
         flow.next(Math.max(hungerLabel.getBoundingBox().height, recreationLabel.getBoundingBox().height) + 5);
         int needsBarStartY = flow.next();
         hungerLabel.setY(needsBarStartY - hungerLabel.getBoundingBox().height - 5);
         recreationLabel.setY(needsBarStartY - recreationLabel.getBoundingBox().height - 5);
         FormProgressBar hungerBar = container.addComponent(new FormSettlerHungerBar(10, needsBarStartY, needsBarWidth, true) {
            @Override
            public float getHungerLevel() {
               return HumanMob.this.getHungerLevel();
            }
         });
         container.addComponent(new FormSettlerRecreationBar(10 + needsBarWidth + needsBarSplitWidth, needsBarStartY, needsBarWidth, true) {
            @Override
            public float getRecreationLevel() {
               return HumanMob.this.getRecreationLevel();
            }
         });
         flow.next(hungerBar.getFullHeight() + 8);
      }
   }

   public Predicate<Mob> filterHumanTargets() {
      return this.downedState != null ? m -> false : m -> m.canBeTargetedByHumans(this);
   }

   @Override
   public boolean canAttack() {
      return this.downedState == null && super.canAttack();
   }

   public void attackSprite(int x, int y, Item item, int animTime, boolean inverted) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(x);
      writer.putNextInt(y);
      writer.putNextBoolean(item != null);
      if (item != null) {
         writer.putNextShortUnsigned(item.getID());
      }

      writer.putNextInt(animTime);
      writer.putNextBoolean(inverted);
      this.itemSwingSpriteAbility.runAndSend(content);
   }

   public void attackSprite(int x, int y, String itemStringID, int animTime, boolean inverted) {
      this.attackSprite(x, y, itemStringID == null ? null : ItemRegistry.getItem(itemStringID), animTime, inverted);
   }

   public void attackItem(int x, int y, Item item, int attackTime, int cooldown) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(x);
      writer.putNextInt(y);
      writer.putNextBoolean(false);
      writer.putNextShortUnsigned(item.getID());
      writer.putNextInt(attackTime);
      writer.putNextInt(cooldown);
      this.itemAttackAbility.runAndSend(content);
   }

   public void attackItem(int x, int y, InventoryItem item, int attackTime, int cooldown) {
      Packet content = new Packet();
      PacketWriter writer = new PacketWriter(content);
      writer.putNextInt(x);
      writer.putNextInt(y);
      writer.putNextBoolean(true);
      item.addPacketContent(writer);
      writer.putNextInt(attackTime);
      writer.putNextInt(cooldown);
      this.itemAttackAbility.runAndSend(content);
   }

   public void attackItem(int x, int y, InventoryItem item) {
      this.attackItem(
         x, y, item, item.item.getAttackAnimTime(item, this), Math.max(item.item.getAttackCooldownTime(item, this), item.item.getItemCooldownTime(item, this))
      );
   }

   public void attackItem(int x, int y, String itemStringID, int animTime, int cooldown) {
      this.attackItem(x, y, ItemRegistry.getItem(itemStringID), animTime, cooldown);
   }

   @Override
   public float getAttackAnimProgress() {
      float out = super.getAttackAnimProgress();
      if (!this.isAttacking) {
         long timeSinceLastWorkRequest = this.getLocalTime() - this.lastWorkAnimationRequest;
         if (this.lastWorkAnimationRequest > 0L && timeSinceLastWorkRequest < this.attackAnimTime) {
            this.attackTime = this.getTime();
            this.isAttacking = true;
         }
      }

      return out;
   }

   public boolean shouldOnlyUseHumanLikeLook() {
      return true;
   }

   @Override
   public DrawOptions getUserDrawOptions(
      Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective, Consumer<HumanDrawOptions> humanDrawOptionsModifier
   ) {
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      boolean humanLikeLook = this.shouldOnlyUseHumanLikeLook();
      if (this.customLook) {
         humanLikeLook = false;
      }

      HumanDrawOptions humanOptions = new HumanDrawOptions(level, this.look, humanLikeLook);
      this.setDefaultArmor(humanOptions);
      this.setDisplayArmor(humanOptions);
      this.modifyExpressionDrawOptions(humanOptions);
      humanOptions.invis(this.buffManager.getModifier(BuffModifiers.INVISIBILITY)).blinking(this.isBlinking()).sprite(sprite).dir(dir).light(light);
      humanDrawOptionsModifier.accept(humanOptions);
      humanOptions = this.setCustomItemAttackOptions(humanOptions);
      DrawOptions drawOptions = humanOptions.pos(drawX, drawY);
      DrawOptions markerOptions = this.getMarkerDrawOptions(x, y, light, camera, 0, -45, perspective);
      return () -> {
         drawOptions.draw();
         markerOptions.draw();
      };
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
      if (this.objectUser == null || this.objectUser.drawsUser()) {
         if (this.isVisible()) {
            GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
            int drawX = camera.getDrawX(x) - 22 - 10;
            int drawY = camera.getDrawY(y) - 44 - 7;
            int dir = this.getDir();
            Point sprite = this.getAnimSprite(x, y, dir);
            if (this.getLocalTime() <= this.danceAnimationTimeout) {
               sprite.x = (int)(this.getTime() / 300L % 4L) + 1;
            }

            drawY += this.getBobbing(x, y);
            drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
            MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
            boolean humanLikeLook = this.shouldOnlyUseHumanLikeLook();
            if (this.customLook) {
               humanLikeLook = false;
            }

            HumanDrawOptions humanOptions = new HumanDrawOptions(level, this.look, humanLikeLook)
               .invis(this.buffManager.getModifier(BuffModifiers.INVISIBILITY))
               .blinking(this.isBlinking())
               .sprite(sprite)
               .mask(swimMask)
               .dir(dir)
               .light(light)
               .applyEnemyTracker(this, perspective);
            this.setDefaultArmor(humanOptions);
            this.setDisplayArmor(humanOptions);
            this.modifyExpressionDrawOptions(humanOptions);
            boolean inLiquid = this.inLiquid(x, y);
            if (this.isDowned()) {
               humanOptions.blinking(true);
               humanOptions.dir(1);
               humanOptions.rotate(90.0F, 32, 32).drawOffset(6, inLiquid ? 7 : 11);
               humanOptions.mask(new MaskShaderOptions(0, 0));
            } else if (inLiquid) {
               humanOptions.armSprite(2);
               humanOptions.mask(MobRegistry.Textures.boat_mask[sprite.y % 4], 0, -7);
            }

            humanOptions = this.setCustomItemAttackOptions(humanOptions);
            this.buffManager.addHumanDraws(humanOptions);
            final DrawOptions drawOptions = humanOptions.pos(drawX, drawY);
            final DrawOptions boat = inLiquid ? MobRegistry.Textures.woodBoat.initDraw().sprite(0, dir % 4, 64).light(light).pos(drawX, drawY + 7) : null;
            final DrawOptions markerOptions = this.getMarkerDrawOptions(x, y, light, camera, 0, -45, perspective);
            list.add(new MobDrawable() {
               @Override
               public void draw(TickManager tickManager) {
                  if (boat != null) {
                     boat.draw();
                  }

                  drawOptions.draw();
                  markerOptions.draw();
               }
            });
            this.addShadowDrawables(tileList, level, x, y, light, camera);
         }
      }
   }

   protected HumanDrawOptions setCustomItemAttackOptions(HumanDrawOptions options) {
      options = this.setupAttackDraw(options);
      if (!this.isAttacking) {
         NetworkSettlementData settlement = this.getSettlerSettlementNetworkData();
         if (!this.isOnStrike() || settlement != null && settlement.isRaidActiveOrApproaching()) {
            if (this.customHoldItem != null && this.getLocalTime() <= this.customHoldItemTimeout) {
               options.holdItem(this.customHoldItem);
            }
         } else {
            options.holdItem(new InventoryItem("strikebanner"));
         }
      }

      return options;
   }

   @Override
   public boolean itemAttackerHoldsItem(InventoryItem item) {
      return this.hasCommandOrders() || this.adventureParty.isInAdventureParty();
   }

   public DrawOptions getMarkerDrawOptions(int x, int y, GameLight light, GameCamera camera, int xOffset, int yOffset, PlayerMob perspective) {
      QuestMarkerOptions markerOptions = this.getMarkerOptions(perspective);
      return markerOptions != null ? markerOptions.getDrawOptions(x, y, light, camera, xOffset, yOffset) : () -> {};
   }

   public QuestMarkerOptions getMarkerOptions(PlayerMob perspective) {
      if (this.isVisitor()) {
         return new QuestMarkerOptions('?', QuestMarkerOptions.orangeColor);
      } else {
         return this.completedMission ? new QuestMarkerOptions('?', this.missionFailed ? QuestMarkerOptions.grayColor : QuestMarkerOptions.orangeColor) : null;
      }
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      Point sprite = super.getAnimSprite(x, y, dir);
      if (this.downedState != null) {
         sprite.x = 6;
      }

      return sprite;
   }

   @Override
   public void setFacingDir(float deltaX, float deltaY) {
      if (this.downedState == null) {
         super.setFacingDir(deltaX, deltaY);
      }
   }

   protected void setDownedState(HumanMob.DownedState downedState) {
      if (this.downedState != downedState) {
         this.downedState = downedState;
         this.updateTeam();
         this.sendMovementPacket(false);
      }
   }

   public boolean isDowned() {
      return this.downedState == HumanMob.DownedState.DOWNED;
   }

   public boolean isTrapped() {
      return this.downedState == HumanMob.DownedState.TRAPPED;
   }

   public void setTrapped() {
      this.setDownedState(HumanMob.DownedState.TRAPPED);
   }

   public void setDowned(int settlementUniqueID, int reservedTime) {
      this.setDownedState(HumanMob.DownedState.DOWNED);
      this.recruitReservedSettlementUniqueID = settlementUniqueID;
      this.recruitReservedTimeout = this.getTime() + reservedTime;
      if (this.currentMission != null) {
         LootTable lootTable = this.currentMission.getLootTable(this);
         if (lootTable != null) {
            ArrayList<InventoryItem> drops = lootTable.getNewList(GameRandom.globalRandom, this.getLevel().buffManager.getModifier(LevelModifiers.LOOT), this);
            Point dropPos = this.getLootDropsPosition(null);
            dropPos.x = this.getLevel().limitLevelXToBounds(dropPos.x);
            dropPos.y = this.getLevel().limitLevelYToBounds(dropPos.y);

            for (InventoryItem item : drops) {
               ItemPickupEntity entity = item.getPickupEntity(this.getLevel(), dropPos.x, dropPos.y);
               this.getLevel().entityManager.pickups.add(entity);
            }
         }

         this.currentMission = null;
      }

      this.setMovement(null);
   }

   public void clearDownedState() {
      this.setDownedState(null);
   }

   public void markMorePersonalitiesFromIncursionPerk() {
      this.morePersonalitiesFromIncursionPerk = true;
   }

   public void setDefaultArmor(HumanDrawOptions drawOptions) {
      Settler settler = this.getSettler();
      if (settler != null) {
         settler.setDefaultArmor(drawOptions, this.settlerSeed, this.look, this.customLook);
      }
   }

   public void setDisplayArmor(HumanDrawOptions drawOptions) {
      InventoryItem helmet = this.getDisplayArmor(0, null);
      if (helmet != null && Settings.showSettlerHeadArmor) {
         drawOptions.helmet(helmet);
      }

      InventoryItem chestplate = this.getDisplayArmor(1, null);
      if (chestplate != null) {
         drawOptions.chestplate(chestplate);
      }

      InventoryItem boots = this.getDisplayArmor(2, null);
      if (boots != null) {
         drawOptions.boots(boots);
      }

      if (this.downedState == HumanMob.DownedState.TRAPPED) {
         drawOptions.addTopDraw(
            (level, player, dir, spriteX, spriteY, spriteRes, drawX, drawY, width, height, mirrorX, mirrorY, light, hasGlowEffect, glowHash, alpha, mask) -> MobRegistry.Textures.human_enchained_iron
               .initDraw()
               .sprite(spriteX, spriteY, spriteRes)
               .startGlowOptions(level, glowHash)
               .light(light)
               .apply(hasGlowEffect)
               .alpha(alpha)
               .size(width, height)
               .mirror(mirrorX, mirrorY)
               .addMaskShader(mask)
               .pos(drawX, drawY)
         );
      }
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

   @Override
   public boolean shouldDrawOnMap() {
      return !this.isVisible() ? false : this.isSettler() || this.isVisitor() || this.isDowned();
   }

   @Override
   public boolean isVisibleOnMap(Client client, ClientDiscoveredMap map) {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      HumanDrawOptions humanOptions = new HumanDrawOptions(this.getLevel(), this.look, !this.customLook);
      this.setDefaultArmor(humanOptions);
      this.setDisplayArmor(humanOptions);
      this.modifyExpressionDrawOptions(humanOptions);
      if (this.isDowned()) {
         humanOptions.blinking(true);
         humanOptions.rotate(90.0F, 16, 14);
      }

      Settler.getHumanFaceDrawOptions(humanOptions, 16, x - 8, y - 8).draw();
      QuestMarkerOptions marker = this.getMarkerOptions(client.getPlayer());
      if (marker != null && Settings.showQuestMarkers) {
         QuestGiver.getMarkerDrawOptions(marker.icons, 20, marker.color, x, y - 5, 1.0F, -1, 0).draw();
      }
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-8, -8, 16, 16);
   }

   @Override
   public GameTooltips getMapTooltips() {
      StringTooltips tooltips = new StringTooltips(this.getDisplayName());
      if (this.isSettler()) {
         GameMessage currentActivity = this.getCurrentActivity();
         if (currentActivity != null) {
            if (this.hasCommandOrders()) {
               tooltips.add(currentActivity.translate(), GameColor.RED, 300);
            } else {
               tooltips.add(currentActivity.translate(), GameColor.LIGHT_GRAY, 300);
            }
         }
      }

      if (this.isDowned()) {
         tooltips.add(Localization.translate("activities", "downed"), GameColor.YELLOW);
      }

      return tooltips;
   }

   @Override
   public boolean shouldSave() {
      return this.isDowned() || !this.isMovingOut() && super.shouldSave();
   }

   @Override
   public boolean shouldAddToDeletedLevelReturnedMobs() {
      return this.isDowned();
   }

   @Override
   protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
      super.addHoverTooltips(tooltips, debug);
      if (this.isSettler()) {
         GameMessage currentActivity = this.getCurrentActivity();
         if (currentActivity != null) {
            if (this.hasCommandOrders()) {
               tooltips.add(new StringTooltips(currentActivity.translate(), GameColor.RED, 300));
            } else {
               tooltips.add(new StringTooltips(currentActivity.translate(), GameColor.LIGHT_GRAY, 300));
            }
         }
      }

      if (this.isDowned()) {
         tooltips.add(new StringTooltips(Localization.translate("activities", "downed"), GameColor.YELLOW));
      }
   }

   @Override
   protected void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      HumanMob serverHumanMob = null;
      tooltips.add("Home: " + (this.home == null ? null : this.home.x + ", " + this.home.y));
      if (this.isVisitor()) {
         tooltips.add("Leave: " + GameUtils.formatSeconds(this.travelOutTime / 1000L));
      }

      if (this.currentMission != null) {
         tooltips.add("currentMission: " + this.currentMission.getStringID());
         this.currentMission.addDebugTooltips(tooltips);
      }

      tooltips.add("completedMission: " + this.completedMission);
      tooltips.add("missionFailed: " + this.missionFailed);
      if (this.isSettler()) {
         tooltips.add("qol level: " + this.getSettlerQualityOfLife());
         tooltips.add("happiness: " + this.getSettlerHappiness() + "%");
         tooltips.add("hunger: " + (int)(this.getHungerLevel() * 100.0F) + "%");
         tooltips.add("recreation: " + (int)(this.getRecreationLevel() * 100.0F) + "%");
         if (this.lastFoodEaten != null) {
            tooltips.add("lastFoodEaten: " + this.lastFoodEaten.getStringID() + " (" + this.lastFoodEaten.quality.displayName.translate() + ")");
         }

         tooltips.add("isOnStrike: " + this.isOnStrike.get());
      }

      tooltips.add("romancePersonality: " + this.romancePersonality.getStringID());
      if (this.isClient()) {
         Server server = this.getClient().getLocalServer();
         if (server != null) {
            Level serverLevel = server.world.getLevel(server.getLocalServerClient());
            if (serverLevel != null) {
               Mob serverMob = serverLevel.entityManager.mobs.get(this.getUniqueID(), false);
               if (serverMob instanceof HumanMob) {
                  serverHumanMob = (HumanMob)serverMob;
               }
            }
         }
      } else {
         serverHumanMob = this;
      }

      if (serverHumanMob != null) {
         tooltips.add("qol change: " + (int)(serverHumanMob.settlerQualityOfLifeChangeBuffer * 100.0F) + "%");
         int differentFoodsEaten = (int)serverHumanMob.recentFoodItemIDsEaten.stream().distinct().count();
         tooltips.add(
            "recentFood: ("
               + differentFoodsEaten
               + ") "
               + Arrays.toString(serverHumanMob.recentFoodItemIDsEaten.stream().map(ItemRegistry::getItemStringID).toArray())
         );
         tooltips.add("endStrikeBuffer: " + serverHumanMob.endStrikeBuffer);
         tooltips.add("hungerStrikeCooldownBuffer: " + serverHumanMob.hungerStrikeCooldownBuffer / 1000);
         tooltips.add("leaveBuffer: " + serverHumanMob.leaveBuffer);
         tooltips.add("recreationExhaustion: " + serverHumanMob.recreationExhaustionManager.getDebugTooltip());
         tooltips.add("recreationStrikeCooldownBuffer: " + serverHumanMob.recreationStrikeCooldownBuffer / 1000);
         tooltips.add(serverHumanMob.romancePersonality.getDebugServerTooltips());
      }
   }

   public GameMessage getRandomAngryMessage() {
      ArrayList<GameMessage> messages = this.getAngryMessages();
      return GameRandom.globalRandom.getOneOf(messages);
   }

   protected ArrayList<GameMessage> getAngryMessages() {
      ArrayList<GameMessage> messages = this.getLocalMessages("humanangry", 6);
      if (this.defendsHimselfAgainstPlayers()) {
         messages.addAll(this.getLocalMessages("humanangryactive", 3));
      }

      return messages;
   }

   protected boolean defendsHimselfAgainstPlayers() {
      return this.downedState == null;
   }

   public GameMessage getRandomAttackMessage() {
      ArrayList<GameMessage> messages = this.getAttackMessages();
      return GameRandom.globalRandom.getOneOf(messages);
   }

   protected ArrayList<GameMessage> getAttackMessages() {
      return !this.defendsHimselfAgainstPlayers() ? this.getAngryMessages() : this.getLocalMessages("humanattack", 6);
   }

   public GameMessage getRandomMessage(GameRandom random, ServerClient client) {
      ArrayList<GameMessage> messages = this.getMessages(client);
      if (this.romancePersonality != null) {
         PlayerRomanceManager romanceManager = this.romancePersonality.getPlayerManager(client, false);
         if (romanceManager == null) {
            return random.getOneOf(messages);
         }

         messages.addAll(this.romancePersonality.getRomancePersonalityMessages(client));
         if (romanceManager.romanceLevel > 0 && !this.romancePersonality.isInRelationshipWith(client)) {
            GameMessage currentActivityMessage = this.romancePersonality.getCurrentActivityMessage(client, romanceManager);
            if (currentActivityMessage != null) {
               return currentActivityMessage;
            }
         }

         if (this.romancePersonality.isInRelationshipWith(client) && this.getWorldEntity().isNight() && !client.adventureParty.contains(this)) {
            ObjectUserActive user = this.getUsingObject();
            if (user != null && user.object instanceof SettlerBedObject) {
               ArrayList<GameMessage> nightMessages = this.romancePersonality.getInRelationShipNightChatMessages(client);
               if (nightMessages != null && !nightMessages.isEmpty()) {
                  return random.getOneOf(nightMessages);
               }
            }
         }
      }

      return random.getOneOf(messages);
   }

   protected GameMessage getTrappedMessage(ServerClient client) {
      return new LocalMessage("ui", "enchainedhelpme");
   }

   protected ArrayList<GameMessage> getMessages(ServerClient client) {
      return this.getLocalMessages("humantalk", 5);
   }

   protected final ArrayList<GameMessage> getLocalMessages(String keyPrefix, int totalKeys) {
      return getLocalMessages("mobmsg", keyPrefix, totalKeys);
   }

   public static ArrayList<GameMessage> getLocalMessages(String category, String keyPrefix, int totalKeys) {
      ArrayList<GameMessage> out = new ArrayList<>();

      for (int i = 0; i < totalKeys; i++) {
         out.add(new LocalMessage(category, keyPrefix + (i + 1)));
      }

      return out;
   }

   public static ArrayList<GameMessage> getLocalMessages(String category, String keyPrefix, int totalKeys, String... replacements) {
      ArrayList<GameMessage> out = new ArrayList<>();

      for (int i = 0; i < totalKeys; i++) {
         out.add(new LocalMessage(category, keyPrefix + (i + 1), replacements));
      }

      return out;
   }

   protected HumanMob getRandomHuman(String type) {
      return this.getRandomHuman(type, 1600);
   }

   protected HumanMob getRandomHuman(String type, int range) {
      Predicate<HumanMob> filter;
      if (this.isSettler()) {
         if (!this.isSettlerWithinSettlement()) {
            return null;
         }

         filter = m -> m.isSettler();
      } else {
         filter = m -> !m.isSettler();
      }

      HumanMob[] mobs = this.getLevel()
         .entityManager
         .mobs
         .streamInRegionsShape(GameUtils.rangeBounds(this.getX(), this.getY(), range), 0)
         .filter(m -> m.getStringID().contains(type) && m instanceof HumanMob)
         .map(m -> (HumanMob)m)
         .filter(filter)
         .toArray(HumanMob[]::new);
      return GameRandom.globalRandom.getOneOf(mobs);
   }

   protected String getRandomName(GameRandom random) {
      switch (this.gender) {
         case MALE:
            return getRandomName(random, maleNames);
         case FEMALE:
            return getRandomName(random, femaleNames);
         case NEUTRAL:
            return getRandomName(random, neutralNames);
         default:
            return "NULL";
      }
   }

   public static String getRandomName(GameRandom random, HumanGender gender) {
      switch (gender) {
         case MALE:
            return maleNames[random.nextInt(maleNames.length)];
         case FEMALE:
            return femaleNames[random.nextInt(femaleNames.length)];
         case NEUTRAL:
         default:
            return neutralNames[random.nextInt(neutralNames.length)];
      }
   }

   public static String getRandomName(GameRandom random, String[] names) {
      return names[random.nextInt(names.length)];
   }

   @Override
   public void migrateToOneWorld(OneWorldMigration migrationData, LevelIdentifier oldLevelIdentifier, Point tileOffset, Point positionOffset, Mob oldMob) {
      super.migrateToOneWorld(migrationData, oldLevelIdentifier, tileOffset, positionOffset, oldMob);
      if (oldMob instanceof HumanMob) {
         HumanMob oldHuman = (HumanMob)oldMob;
         if (oldHuman.earlyAccessSettlementIsland != null) {
            this.settlementUniqueID = migrationData.getOldSettlementAtLevelUniqueID(
               new LevelIdentifier(oldHuman.earlyAccessSettlementIsland.x, oldHuman.earlyAccessSettlementIsland.y, 0)
            );
         }

         if (oldHuman.earlyAccessVisitorIsland != null) {
            this.visitorSettlementUniqueID = migrationData.getOldSettlementAtLevelUniqueID(
               new LevelIdentifier(oldHuman.earlyAccessVisitorIsland.x, oldHuman.earlyAccessVisitorIsland.y, 0)
            );
            this.travelOutTime = oldHuman.travelOutTime;
         }

         if (oldHuman.earlyAccessRecruitReservedIsland != null) {
            this.recruitReservedSettlementUniqueID = migrationData.getOldSettlementAtLevelUniqueID(
               new LevelIdentifier(oldHuman.earlyAccessRecruitReservedIsland.x, oldHuman.earlyAccessRecruitReservedIsland.y, 0)
            );
            this.recruitReservedTimeout = oldHuman.recruitReservedTimeout;
         }
      }

      if (this.home != null) {
         this.home.x = this.home.x + tileOffset.x;
         this.home.y = this.home.y + tileOffset.y;
      }

      if (this.isSettler() && !this.isSettlerWithinSettlementLoadedRegions()) {
         SettlersWorldData settlersData = SettlersWorldData.getSettlersData(this.getServer());
         settlersData.refreshWorldSettler(this, false);
      }
   }

   public enum DownedState {
      DOWNED,
      TRAPPED;
   }

   public class ShowItemThoughtMobAbility extends MobAbility {
      public void runAndSend(Item item, int animTime) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextInt(item.getID());
         writer.putNextInt(animTime);
         this.runAndSendAbility(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int itemID = reader.getNextInt();
         int animTime = reader.getNextInt();
         HumanMob.this.getLevel().hudManager.addElement(new ItemThoughtBubble(HumanMob.this, animTime, ItemRegistry.getItem(itemID)));
      }
   }

   public class ShowMobThoughtMobAbility extends MobAbility {
      public void runAndSend(String mobStringID, int animTime) {
         this.runAndSend(MobRegistry.getMobID(mobStringID), animTime);
      }

      public void runAndSend(int mobID, int animTime) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextInt(mobID);
         writer.putNextInt(animTime);
         this.runAndSendAbility(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int mobID = reader.getNextInt();
         int animTime = reader.getNextInt();
         HumanMob.this.getLevel().hudManager.addElement(new MobThoughtBubble(HumanMob.this, animTime, mobID));
      }
   }

   public class ShowOtherChatterThoughtMobAbility extends MobAbility {
      public void runAndSend(JobWorkerChatter target, int animTime) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(target.getChatterMob().getUniqueID());
         writer.putNextInt(animTime);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int uniqueID = reader.getNextInt();
         Mob target = GameUtils.getLevelMob(uniqueID, this.getMob().getLevel());
         int animTime = reader.getNextInt();
         if (target instanceof JobWorkerChatter) {
            HumanMob.this.getLevel().hudManager.addElement(new OtherSettlerThoughtBubble(HumanMob.this, animTime, (JobWorkerChatter)target));
         }
      }
   }

   public class ShowSimpleThoughtMobAbility extends MobAbility {
      public Function<Integer, ThoughtBubble> thoughtBubbleConstructor;

      public ShowSimpleThoughtMobAbility(Function<Integer, ThoughtBubble> thoughtBubbleConstructor) {
         this.thoughtBubbleConstructor = thoughtBubbleConstructor;
      }

      public void runAndSend(int animTime) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(animTime);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int animTime = reader.getNextInt();
         ThoughtBubble thoughtBubble = this.thoughtBubbleConstructor.apply(animTime);
         HumanMob.this.getLevel().hudManager.addElement(thoughtBubble);
      }
   }

   public class SpawnHeartParticlesMobAbility extends MobAbility {
      public void runAndSend(Mob target, int animTime) {
         Packet content = new Packet();
         PacketWriter writer = new PacketWriter(content);
         writer.putNextInt(target.getUniqueID());
         writer.putNextInt(animTime);
         this.runAndSendAbility(content);
      }

      @Override
      public void executePacket(PacketReader reader) {
         int uniqueID = reader.getNextInt();
         Mob target = GameUtils.getLevelMob(uniqueID, this.getMob().getLevel());
         int animTime = reader.getNextInt();
         if (target != null) {
            SettlerRomancePersonality.spawnHeartParticles(target, animTime);
         }
      }
   }

   public class StartExpressionMobAbility extends MobAbility {
      public void runAndSend(FormExpressionWheel.Expression expression, int animTime) {
         Packet packet = new Packet();
         PacketWriter writer = new PacketWriter(packet);
         writer.putNextEnum(expression);
         writer.putNextInt(animTime);
         this.runAndSendAbility(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         HumanMob.this.activeExpression = reader.getNextEnum(FormExpressionWheel.Expression.class);
         HumanMob.this.activeExpressionEndTime = HumanMob.this.getLocalTime() + reader.getNextInt();
      }
   }

   public static class StrikeChance {
      public final float chance;
      public final GameMessage reason;
      public Runnable onStrikeStarted;

      public StrikeChance(float chance, GameMessage reason) {
         this.chance = chance;
         this.reason = reason;
      }
   }

   public class UpdateRomancePersonalityAbility extends MobAbility {
      public void runAndSend() {
         Packet packet = new Packet();
         HumanMob.this.romancePersonality.setupSpawnPacket(new PacketWriter(packet));
         this.runAndSendAbility(packet);
      }

      @Override
      public void executePacket(PacketReader reader) {
         HumanMob.this.romancePersonality.applySpawnPacket(reader);
      }
   }
}
