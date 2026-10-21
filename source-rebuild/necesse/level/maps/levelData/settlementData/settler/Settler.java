package necesse.level.maps.levelData.settlementData.settler;

import java.awt.Color;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.registries.IDData;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.RegistryClosedException;
import necesse.engine.registries.SettlerRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.manager.EntityManager;
import necesse.entity.manager.MobSpawnArea;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameColor;
import necesse.gfx.HumanLook;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.armorItem.cosmetics.misc.ShirtArmorItem;
import necesse.inventory.item.armorItem.cosmetics.misc.ShoesArmorItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.LevelSettler;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;
import necesse.level.maps.levelData.settlementData.SettlementBed;

public abstract class Settler {
   public static TreeSet<QualityOfLifeDescription> qualityOfLifeLevels = new TreeSet<>(Comparator.comparingInt(s -> s.minLevel));
   public static TreeSet<FoodQuality> foodQualities = new TreeSet<>(Comparator.comparingInt(s -> s.happinessIncrease));
   public static FoodQuality FOOD_SIMPLE = new FoodQuality("simplefood", new LocalMessage("settlement", "foodsimple"), 10, "B-C-A", "simple");
   public static FoodQuality FOOD_FINE = new FoodQuality("finefood", new LocalMessage("settlement", "foodfine"), 20, "B-C-B", "fine");
   public static FoodQuality FOOD_GOURMET = new FoodQuality("gourmetfood", new LocalMessage("settlement", "foodgourmet"), 35, "B-C-C", "gourmet");
   public static MobSpawnArea SETTLER_SPAWN_AREA = new MobSpawnArea(800, 1280);
   public final IDData idData = new IDData();
   public final String mobStringID;
   public boolean isPartOfCompleteHost = true;
   public GameTexture texture;

   public static QualityOfLifeDescriptionAbstract getQualityOfLife(int level) {
      for (QualityOfLifeDescription description : qualityOfLifeLevels.descendingSet()) {
         if (level >= description.minLevel) {
            return new QualityOfLifeDescriptionWrapped(description, level);
         }
      }

      return qualityOfLifeLevels.first();
   }

   public static GameMessage getMoodDescription(int happiness) {
      if (happiness < HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD / 2) {
         return new LocalMessage("settlement", "moodveryunhappy");
      } else if (happiness < HumanMob.HAPPINESS_QOL_DECREASE_THRESHOLD) {
         return new LocalMessage("settlement", "moodunhappy");
      } else {
         return happiness < HumanMob.HAPPINESS_QOL_INCREASE_THRESHOLD
            ? new LocalMessage("settlement", "moodcontent")
            : new LocalMessage("settlement", "moodveryhappy");
      }
   }

   public static GameMessage getMoodMessage(int happiness) {
      return new LocalMessage("settlement", "moodhappiness").addReplacement("happiness", getMoodDescription(happiness));
   }

   public static GameMessage getQualityOfLifeAndMoodMessage(
      int qualityOfLifeLevel, int happiness, Color highlightColor, boolean addHappinessNumber, boolean addQualityOfLifeNumber
   ) {
      GameMessage moodMessage = getMoodDescription(happiness);
      GameMessage qualityOfLifeMessage = getQualityOfLife(qualityOfLifeLevel).getDescription();
      if (addHappinessNumber) {
         moodMessage = new GameMessageBuilder().append(moodMessage).append(" (" + GameMath.limit(happiness, 0, 100) + "%)");
      }

      if (addQualityOfLifeNumber) {
         qualityOfLifeMessage = new GameMessageBuilder().append(qualityOfLifeMessage).append(" (" + qualityOfLifeLevel + ")");
      }

      if (highlightColor != null) {
         moodMessage = new GameMessageBuilder()
            .append(GameColor.getCustomColorCode(highlightColor))
            .append(moodMessage)
            .append(GameColor.NO_COLOR.getColorCode());
         qualityOfLifeMessage = new GameMessageBuilder()
            .append(GameColor.getCustomColorCode(highlightColor))
            .append(qualityOfLifeMessage)
            .append(GameColor.NO_COLOR.getColorCode());
      }

      return new LocalMessage("settlement", "moodqolmessage").addReplacement("happiness", moodMessage).addReplacement("qualityoflife", qualityOfLifeMessage);
   }

   public final String getStringID() {
      return this.idData.getStringID();
   }

   public final int getID() {
      return this.idData.getID();
   }

   public Settler(String mobStringID) {
      if (SettlerRegistry.instance.isClosed()) {
         throw new RegistryClosedException(
            "Cannot construct Settler objects when settler registry is closed, since they are a static registered objects. Use SettlerRegistry.getSettler(...) to get settlers."
         );
      }

      this.mobStringID = mobStringID;
   }

   public void onSettlerRegistryClosed() {
      if (this.mobStringID != null) {
         Mob m = MobRegistry.getMob(this.mobStringID, null);
         if (!(m instanceof SettlerMob)) {
            throw new IllegalArgumentException(this.mobStringID + " mob does not exist or does not implement SettlerMob interface.");
         }
      }
   }

   public float getArriveAsRecruitAfterDeathChance(ServerSettlementData settlement) {
      return 0.9F;
   }

   public boolean canSpawnInSettlement(ServerSettlementData settlement, PlayerStats stats) {
      return false;
   }

   public boolean canMoveOut(LevelSettler settler, ServerSettlementData settlement) {
      return true;
   }

   public boolean canBanish(LevelSettler settler, ServerSettlementData settlement) {
      return true;
   }

   public void setDefaultArmor(HumanDrawOptions drawOptions, int settlerSeed, HumanLook look, boolean customLook) {
      drawOptions.helmet(null);
      drawOptions.chestplate(ShirtArmorItem.addColorData(new InventoryItem("shirt"), look.getShirtColor()));
      drawOptions.boots(ShoesArmorItem.addColorData(new InventoryItem("shoes"), look.getShoesColor()));
   }

   public double getSpawnChance(Server server, ServerClient client, Level level) {
      return 0.0;
   }

   public GameMessage getAcquireTip() {
      return null;
   }

   public void spawnAtClient(Server server, ServerClient client, Level level) {
   }

   public boolean isValidBed(SettlementBed bed) {
      return this.canUseBed(bed) == null;
   }

   public GameMessage canUseBed(SettlementBed bed) {
      return null;
   }

   private static Point getFaceHairTextureOffset() {
      return new Point(-16, -12);
   }

   public static DrawOptions getHumanFaceDrawOptions(HumanDrawOptions humanDrawOptions, int size, int drawX, int drawY) {
      return getHumanFaceDrawOptions(humanDrawOptions, size, drawX, drawY, null);
   }

   public static DrawOptions getHumanFaceDrawOptions(
      HumanDrawOptions humanDrawOptions, int size, int drawX, int drawY, Consumer<HumanDrawOptions> additionalModifiers
   ) {
      Point offset = getFaceHairTextureOffset();
      float sizeChange = 32.0F / size;
      int offsetX = (int)(offset.x / sizeChange);
      int offsetY = (int)(offset.y / sizeChange);
      humanDrawOptions = humanDrawOptions.sprite(0, 2)
         .dir(2)
         .bodyTexture(null)
         .feetTexture(null)
         .size(size * 2, size * 2)
         .leftArmsTexture(null)
         .rightArmsTexture(null)
         .chestplate(null)
         .boots(null)
         .holdItem(null);
      if (additionalModifiers != null) {
         additionalModifiers.accept(humanDrawOptions);
      }

      return humanDrawOptions.pos(drawX + offsetX, drawY + offsetY);
   }

   public DrawOptions getSettlerFaceDrawOptions(int drawX, int drawY, int size, Mob settlerMob) {
      if (settlerMob != null && settlerMob instanceof HumanMob) {
         HumanMob humanMob = (HumanMob)settlerMob;
         HumanDrawOptions humanOptions = new HumanDrawOptions(null, humanMob.look, !humanMob.customLook);
         humanMob.setDefaultArmor(humanOptions);
         return getHumanFaceDrawOptions(humanOptions, size, drawX, drawY);
      } else {
         return this.getSettlerIcon().initDraw().size(size).pos(drawX, drawY);
      }
   }

   public boolean isMouseOverSettlerFlag(int tileX, int tileY, GameCamera camera) {
      return tileX == camera.getMouseLevelTilePosX() && tileY == camera.getMouseLevelTilePosY();
   }

   public GameTexture getSettlerIcon() {
      return this.texture;
   }

   public void loadTextures() {
      try {
         try {
            this.texture = GameTexture.fromFileRaw("mobs/icons/" + this.getStringID() + "human");
         } catch (FileNotFoundException e) {
            this.texture = GameTexture.fromFileRaw("mobs/icons/" + this.getStringID());
         }
      } catch (FileNotFoundException e) {
         this.texture = GameTexture.fromFile("settlers/" + this.getStringID());
      }
   }

   public String getGenericMobName() {
      return MobRegistry.getDisplayName(MobRegistry.getMobID(this.mobStringID));
   }

   public void onMoveIn(LevelSettler settler) {
   }

   public final SettlerMob getNewSettlerMob(ServerSettlementData settlement) {
      return this.getNewSettlerMob(settlement.getLevel(), settlement);
   }

   public SettlerMob getNewSettlerMob(Level level, ServerSettlementData settlement) {
      SettlerMob settlerMob = (SettlerMob)MobRegistry.getMob(this.mobStringID, level);
      if (settlement != null) {
         Point spawnPos = getNewSettlerSpawnPos(settlerMob.getMob(), settlement);
         if (spawnPos != null) {
            settlerMob.getMob().setPos(spawnPos.x, spawnPos.y, true);
            return settlerMob;
         } else {
            return null;
         }
      } else {
         return settlerMob;
      }
   }

   public static Point getNewSettlerSpawnPos(Mob mob, ServerSettlementData settlement) {
      return settlement.findRandomSpawnLevelPos(mob, true);
   }

   protected Supplier<HumanMob> getNewRecruitMob(ServerSettlementData data) {
      return () -> {
         Mob mob = MobRegistry.getMob(this.mobStringID, data.getLevel());
         return mob instanceof HumanMob ? (HumanMob)mob : null;
      };
   }

   protected boolean doesSettlementHaveThisSettler(ServerSettlementData data) {
      return data.hasSettler(this);
   }

   public void addNewRecruitSettler(ServerSettlementData data, boolean isRandomEvent, TicketSystemList<Supplier<HumanMob>> ticketSystem) {
   }

   public static void tickServerClientSpawn(Server server, ServerClient client) {
      for (Settler settler : SettlerRegistry.getSettlers()) {
         if (GameRandom.globalRandom.getChance(settler.getSpawnChance(server, client, client.getLevel()))) {
            settler.spawnAtClient(server, client, client.getLevel());
         }
      }
   }

   protected Point getSpawnLocation(ServerClient client, Level level, Mob mob, MobSpawnArea spawnArea, Function<Point, Integer> ticketsGetter) {
      Point spawnPos = EntityManager.getMobSpawnTile(level, client.playerMob.getX(), client.playerMob.getY(), spawnArea, ticketsGetter);
      return spawnPos != null && !mob.collidesWith(level, spawnPos.x * 32 + 16, spawnPos.y * 32 + 16)
         ? new Point(spawnPos.x * 32 + 16, spawnPos.y * 32 + 16)
         : null;
   }

   protected Point getSpawnLocation(ServerClient client, Level level, Mob mob, MobSpawnArea spawnArea) {
      return this.getSpawnLocation(client, level, mob, spawnArea, tile -> {
         if (level.isSolidTile(tile.x, tile.y)) {
            return 0;
         } else {
            return level.isLiquidTile(tile.x, tile.y) ? 0 : 100;
         }
      });
   }

   static {
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualitylow"), 0));
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualitymediocre"), 1));
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualitydecent"), 2));
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualitygood"), 3));
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualityimpressive"), 4));
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualitywonderful"), 5));
      qualityOfLifeLevels.add(new QualityOfLifeDescription(new LocalMessage("settlement", "qualityunrivaled"), 6));
      foodQualities.add(FOOD_SIMPLE);
      foodQualities.add(FOOD_FINE);
      foodQualities.add(FOOD_GOURMET);
   }
}
