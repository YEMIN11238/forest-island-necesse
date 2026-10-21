package necesse.entity.mobs.friendly;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameObjectReservable;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.misc.MobProcessMobHandler;
import necesse.entity.mobs.misc.StartMobProcessMobMobAbility;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.consumableItem.food.GrainItem;
import necesse.level.maps.levelData.jobs.MilkHusbandryMobLevelJob;
import necesse.level.maps.levelData.jobs.ShearHusbandryMobLevelJob;
import necesse.level.maps.levelData.jobs.SlaughterHusbandryMobLevelJob;
import necesse.level.maps.regionSystem.ConnectedSubRegionsResult;
import necesse.level.maps.regionSystem.RegionType;
import necesse.level.maps.regionSystem.SubRegion;

public abstract class HusbandryMob extends FriendlyRopableMob implements FeedingTroughMob {
   public static int maxCloseMobsToBirthTileRange = 8;
   public static int maxCloseMobsToBirth = 10;
   public float hungerLossPerHour = 2.0F;
   public float defaultHungerGainPerFeed = 0.75F;
   public float tamenessLossPerHour = 0.5F;
   public float tamenessGainPerHour = 1.0F;
   public int birthingCooldown = 1440000;
   public int timeToGrowUp = 1920000;
   protected float hunger = 0.0F;
   protected float tameness = 0.0F;
   protected long growUpTime;
   protected long nextBirthTime;
   public MilkHusbandryMobLevelJob milkJob;
   public ShearHusbandryMobLevelJob shearJob;
   public SlaughterHusbandryMobLevelJob slaughterJob;
   protected MobProcessMobHandler<HusbandryMob, HusbandryMob> impregnateHandler = new MobProcessMobHandler<HusbandryMob, HusbandryMob>(this) {
      @Override
      public void tickInProgress() {
         super.tickInProgress();
         HusbandryMob target = this.targetMob.get(HusbandryMob.this.getLevel());
         if (!HusbandryMob.this.isClient() && target != null) {
            target.impregnateReservable.reserve(HusbandryMob.this);
         }

         if (!HusbandryMob.this.isServer()) {
            if (GameRandom.globalRandom.nextInt(20) == 0) {
               HusbandryMob.this.spawnLoveParticles();
            }

            if (target != null && GameRandom.globalRandom.nextInt(20) == 0) {
               target.spawnLoveParticles();
            }
         }
      }

      @Override
      public void onCompleted() {
         super.onCompleted();
         HusbandryMob target = this.targetMob.get(HusbandryMob.this.getLevel());
         if (target != null && !HusbandryMob.this.isClient()) {
            target.refreshBirthingCooldown();
            HusbandryMob.this.refreshBirthingCooldown();
            HusbandryMob.this.sendMovementPacket(false);
            target.sendMovementPacket(false);
            target.onImpregnated(HusbandryMob.this);
            HusbandryMob.this.ai.blackboard.submitEvent("wanderNow", new AIEvent());
            target.ai.blackboard.submitEvent("wanderNow", new AIEvent());
         }

         if (!HusbandryMob.this.isServer()) {
            for (int i = 0; i < 5; i++) {
               HusbandryMob.this.spawnLoveParticles();
               if (target != null) {
                  target.spawnLoveParticles();
               }
            }
         }
      }
   };
   public StartMobProcessMobMobAbility<HusbandryMob, HusbandryMob> impregnateMobAbility;
   public GameObjectReservable impregnateReservable = new GameObjectReservable();

   public HusbandryMob(int health) {
      super(health);
      this.impregnateMobAbility = this.registerAbility(new StartMobProcessMobMobAbility<>(this.impregnateHandler));
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addFloat("hunger", this.hunger);
      save.addFloat("tameness", this.tameness);
      save.addLong("growUpTime", this.growUpTime);
      save.addLong("nextBirthTime", this.nextBirthTime);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hunger = save.getFloat("hunger", this.hunger, false);
      this.tameness = save.getFloat("tameness", this.tameness, false);
      this.growUpTime = save.getLong("growUpTime", this.growUpTime, false);
      this.nextBirthTime = save.getLong("nextBirthTime", this.nextBirthTime, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.tameness);
      writer.putNextLong(this.growUpTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.tameness = reader.getNextFloat();
      this.growUpTime = reader.getNextLong();
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextFloat(this.hunger);
      if (!this.canBirth() && !this.canImpregnate()) {
         writer.putNextBoolean(false);
      } else {
         writer.putNextBoolean(true);
         writer.putNextLong(this.nextBirthTime);
      }
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.hunger = reader.getNextFloat();
      if (reader.getNextBoolean()) {
         this.nextBirthTime = reader.getNextLong();
      } else {
         this.nextBirthTime = 0L;
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.tickHungerAndTameness();
      this.impregnateHandler.tick();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.tickHungerAndTameness();
      this.impregnateHandler.tick();
   }

   public void tickHungerAndTameness() {
      if (this.hunger > 0.0F) {
         int secondsPerHour = 3600;
         float hungerLossPerSecond = this.hungerLossPerHour / secondsPerHour;
         float hungerLossPerTick = hungerLossPerSecond / 20.0F;
         this.hunger = Math.max(this.hunger - hungerLossPerTick, 0.0F);
         if (this.tameness < 1.0F) {
            float tameGainPerSecond = this.tamenessGainPerHour / secondsPerHour;
            float tameGainPerTick = tameGainPerSecond / 20.0F;
            this.tameness = Math.min(this.tameness + tameGainPerTick, 1.0F);
         }
      } else if (this.tameness > 0.0F) {
         int secondsPerHour = 3600;
         float tameLossPerSecond = this.tamenessLossPerHour / secondsPerHour;
         float tameLossPerTick = tameLossPerSecond / 20.0F;
         this.tameness = Math.max(this.tameness - tameLossPerTick, 0.0F);
      }
   }

   public void startBaby() {
      this.growUpTime = this.getWorldTime() + this.timeToGrowUp;
      this.tameness = 1.0F;
      this.hunger = 1.0F;
   }

   public void setTameness(float percent) {
      this.tameness = percent;
      this.hunger = 1.0F;
   }

   public void setImported() {
      this.tameness = 1.0F;
      this.hunger = 1.0F;
   }

   public boolean canBeUsedAsSettlerThought() {
      return this.tameness > 0.0F || !this.isGrown();
   }

   public List<HusbandryMob> getNearbyHusbandryMobs() {
      SubRegion subregion = this.getLevel().regionManager.getSubRegionByTile(this.getTileX(), this.getTileY());
      Predicate<Mob> test;
      if (subregion != null) {
         ConnectedSubRegionsResult connected = subregion.getAllConnected(sr -> sr.getType() == RegionType.OPEN || sr.getType() == RegionType.SOLID, 500);
         if (connected.size >= 500) {
            test = m -> true;
         } else {
            Set<Integer> regionIDs = connected.connectedRegions.stream().map(SubRegion::getRegionID).collect(Collectors.toSet());
            test = m -> regionIDs.contains(this.getLevel().getRegionID(m.getTileX(), m.getTileY()));
         }
      } else {
         test = m -> true;
      }

      return this.getLevel()
         .entityManager
         .mobs
         .getInRegionByTileRange(this.getTileX(), this.getTileY(), maxCloseMobsToBirthTileRange)
         .stream()
         .filter(m -> m != this)
         .filter(test)
         .filter(m -> m instanceof HusbandryMob)
         .map(m -> (HusbandryMob)m)
         .filter(m -> GameMath.diagonalMoveDistance(this.getX(), this.getY(), m.getX(), m.getY()) <= maxCloseMobsToBirthTileRange * 32)
         .collect(Collectors.toList());
   }

   @Override
   public InventoryItem onFed(InventoryItem item) {
      this.hunger = this.hunger + this.defaultHungerGainPerFeed;
      this.sendMovementPacket(false);
      item.setAmount(item.getAmount() - 1);
      return item;
   }

   @Override
   public boolean canFeed(InventoryItem item) {
      return !this.isOnFeedCooldown() && item.item instanceof GrainItem;
   }

   @Override
   public boolean isOnFeedCooldown() {
      return this.hunger > 1.0F;
   }

   public boolean isGrown() {
      return this.growUpTime <= this.getWorldTime();
   }

   public boolean canBirth() {
      return this.getGender() == HumanGender.FEMALE && this.isGrown() && this.tameness >= 1.0F;
   }

   public boolean canImpregnate() {
      return this.isGrown() && this.getGender() == HumanGender.MALE && this.tameness >= 1.0F;
   }

   public boolean isReservedForImpregnating() {
      return !this.impregnateReservable.isAvailable(this.impregnateReservable, this.getWorldEntity());
   }

   public boolean isImpregnating() {
      return this.impregnateHandler.isInProgress();
   }

   public boolean canImpregnateMob(HusbandryMob other) {
      return false;
   }

   public void onImpregnated(HusbandryMob father) {
      Mob child = MobRegistry.getMob(this.getRandomChildMobStringID(father), this.getLevel());
      if (child instanceof HusbandryMob) {
         ((HusbandryMob)child).startBaby();
      }

      ArrayList<Point> possibleSpawnPoints = new ArrayList<>();

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            if (x != 0 || y != 0) {
               Point point = new Point(this.getX() + x * 4, this.getY() + y * 4);
               if (!child.collidesWith(this.getLevel(), point.x, point.y)) {
                  possibleSpawnPoints.add(point);
               }
            }
         }
      }

      Point spawnPoint = GameRandom.globalRandom.getOneOf(possibleSpawnPoints);
      if (spawnPoint == null) {
         spawnPoint = new Point(this.getX(), this.getY());
      }

      float var10002 = spawnPoint.x;
      this.getLevel().entityManager.addMob(child, var10002, spawnPoint.y);
   }

   public String getRandomChildMobStringID(HusbandryMob father) {
      return this.getStringID();
   }

   public void spawnLoveParticles() {
      float posX = this.x + GameRandom.globalRandom.getIntBetween(-10, 10);
      float posY = this.y + GameRandom.globalRandom.getIntBetween(-5, 5);
      int startHeight = 8 + GameRandom.globalRandom.nextInt(24);
      int lifeTime = GameRandom.globalRandom.getIntBetween(2500, 3500);
      final int swing = GameRandom.globalRandom.getIntBetween(5, 10);
      final float swingOffset = GameRandom.globalRandom.nextFloat();
      float moveX = GameRandom.globalRandom.floatGaussian() * 2.0F;
      float moveY = GameRandom.globalRandom.floatGaussian() * 1.5F;
      final ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
      this.getLevel()
         .entityManager
         .addParticle(posX, posY, Particle.GType.COSMETIC)
         .sprite(GameResources.heartParticle)
         .dontRotate()
         .color(new Color(255, 255, 255))
         .heightMoves(startHeight, startHeight + 40)
         .moves(new ParticleOption.Mover() {
            @Override
            public void tick(Float pos, float delta, int lifeTimex, int timeAlive, float lifePercent) {
               frictionMover.tick(pos, delta, lifeTimex, timeAlive, lifePercent);
               float angle = (lifePercent + swingOffset) * 500.0F;
               pos.x = pos.x + GameMath.sin(angle) * swing * delta / 250.0F;
            }
         })
         .lifeTime(lifeTime);
   }

   public void refreshBirthingCooldown() {
      if (this.birthingCooldown < 0) {
         this.nextBirthTime = -1L;
      } else {
         this.nextBirthTime = this.getWorldTime() + this.birthingCooldown;
      }
   }

   public boolean isOnBirthingCooldown() {
      return this.nextBirthTime < 0L || this.getWorldTime() <= this.nextBirthTime;
   }

   public abstract HumanGender getGender();

   public boolean canMilk(InventoryItem item) {
      return false;
   }

   public InventoryItem onMilk(InventoryItem item, List<InventoryItem> products) {
      return item;
   }

   public boolean canShear(InventoryItem item) {
      return false;
   }

   public InventoryItem onShear(InventoryItem item, List<InventoryItem> products) {
      return item;
   }

   @Override
   protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
      super.addHoverTooltips(tooltips, debug);
      if (this.tameness > 0.0F && this.hunger <= 0.0F) {
         tooltips.add(Localization.translate("misc", "animalhungry"));
      }

      if (this.tameness >= 1.0F) {
         tooltips.add(Localization.translate("misc", "animaltame"));
      } else if (this.tameness > 0.0F) {
         int tamenessPercent = (int)(this.tameness * 100.0F);
         tooltips.add(Localization.translate("misc", "animaltameness", "percent", tamenessPercent));
      }
   }

   @Override
   protected void addDebugTooltips(ListGameTooltips tooltips) {
      super.addDebugTooltips(tooltips);
      if (this.isOnBirthingCooldown()) {
         tooltips.add("Birth cooldown: " + GameUtils.getTimeStringMillis(this.nextBirthTime - this.getWorldTime()));
      }

      if (!this.isGrown()) {
         tooltips.add("Grown in: " + GameUtils.getTimeStringMillis(this.growUpTime - this.getWorldTime()) + " to " + MobRegistry.getDisplayName(this.getID()));
      }

      tooltips.add("Hunger: " + this.hunger);
      tooltips.add("Tameness: " + this.tameness);
      if (this.isServer()) {
         tooltips.add("Slaughter job: " + this.slaughterJob);
         tooltips.add("Milk job: " + this.milkJob);
         tooltips.add("Shear job: " + this.shearJob);
      }
   }
}
