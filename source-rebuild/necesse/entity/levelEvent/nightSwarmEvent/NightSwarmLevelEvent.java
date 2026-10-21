package necesse.entity.levelEvent.nightSwarmEvent;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;
import necesse.engine.GameDifficulty;
import necesse.engine.achievements.AchievementManager;
import necesse.engine.eventStatusBars.EventStatusBarData;
import necesse.engine.eventStatusBars.EventStatusBarManager;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.actions.BooleanLevelEventAction;
import necesse.entity.levelEvent.actions.DoubleIntLevelEventAction;
import necesse.entity.levelEvent.actions.IntLevelEventAction;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.ChargeNightSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.CircleChargeNightSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.FlyByNightSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.JailNightSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.MoveAroundNightSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.NightSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.WaitMajorityCounterSwarmEventStage;
import necesse.entity.levelEvent.nightSwarmEvent.eventStages.WaitNightSwarmEventStage;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobHealthScaling;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.staticBuffs.BossNearbyBuff;
import necesse.entity.mobs.hostile.bosses.NightSwarmBatMob;
import necesse.entity.mobs.hostile.bosses.NightSwarmStartMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.inventory.lootTable.lootItem.RotationLootItem;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class NightSwarmLevelEvent extends LevelEvent {
   public static int START_BAT_COUNT = 75;
   public static MaxHealthGetter BAT_MAX_HEALTH = new MaxHealthGetter(1300, 2000, 2300, 2600, 3300);
   public static RotationLootItem vinylRotation = RotationLootItem.globalLootRotation(
      new LootItemList(),
      new LootItemList(),
      new LootItemList(),
      RotationLootItem.globalLootRotation(4, new LootItem("invasionofthecryptvinyl"), new LootItem("theswarmofthenightvinyl"))
   );
   public static LootTable lootTable = new LootTable(new LootItemMultiplierIgnored(vinylRotation));
   public static RotationLootItem uniqueDrops = RotationLootItem.privateLootRotation();
   public static LootTable privateLootTable = new LootTable(new LootItemMultiplierIgnored(uniqueDrops));
   public int startBatCount = START_BAT_COUNT;
   public ArrayList<LevelMob<NightSwarmBatMob>> bats = new ArrayList<>();
   public float levelX;
   public float levelY;
   public int playerCount;
   public int partyMemberCount;
   public int batsMaxHealth;
   public GameDifficulty difficulty;
   public final BooleanLevelEventAction eventOverAction;
   public final IntLevelEventAction removeBatAction;
   public final DoubleIntLevelEventAction updatePlayerCountAction;
   public LevelMob<NightSwarmStartMob> masterMob;
   public float nextLevelX;
   public float nextLevelY;
   public long nextUpdateTargetTime;
   public Mob currentTarget;
   public int despawnTimer;
   public int batsDoneWithStages;
   public float lastHealthProgress;
   public ArrayList<NightSwarmEventStage> stages = new ArrayList<>();
   public int currentStage = -1;
   public HashSet<Attacker> attackers = new HashSet<>();
   public boolean isDamagedByPlayers;

   public NightSwarmLevelEvent() {
      this.eventOverAction = this.registerAction(
         new BooleanLevelEventAction() {
            @Override
            protected void run(boolean value) {
               if (value) {
                  SoundManager.playSound(
                     GameResources.nightswarmdeathfinal,
                     SoundEffect.effect(NightSwarmLevelEvent.this.levelX, NightSwarmLevelEvent.this.levelY).volume(0.8F).falloffDistance(3000)
                  );
               }

               NightSwarmLevelEvent.this.over();
            }
         }
      );
      this.removeBatAction = this.registerAction(new IntLevelEventAction() {
         @Override
         protected void run(int uniqueID) {
            NightSwarmLevelEvent.this.bats.removeIf(b -> b.uniqueID == uniqueID);
         }
      });
      this.updatePlayerCountAction = this.registerAction(new DoubleIntLevelEventAction() {
         @Override
         protected void run(int nextPlayerCount, int nextPartyMemberCount) {
            NightSwarmLevelEvent.this.updateBatMaxHealthValue(nextPlayerCount, nextPartyMemberCount);

            for (LevelMob<NightSwarmBatMob> bat : NightSwarmLevelEvent.this.bats) {
               NightSwarmBatMob mob = bat.get(NightSwarmLevelEvent.this.level);
               if (mob != null && !mob.removed()) {
                  float percHealth = (float)mob.getHealth() / mob.getMaxHealth();
                  mob.setMaxHealth(NightSwarmLevelEvent.this.batsMaxHealth);
                  int health = (int)(mob.getMaxHealth() * percHealth);
                  mob.setHealthHidden(health);
                  mob.sendHealthPacket(false);
               }
            }
         }
      });
   }

   public NightSwarmLevelEvent(NightSwarmStartMob masterMob, float levelX, float levelY) {
      this();
      this.masterMob = new LevelMob<>(masterMob);
      this.levelX = levelX;
      this.levelY = levelY;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.masterMob.uniqueID);
      writer.putNextInt(this.startBatCount);
      writer.putNextInt(this.batsMaxHealth);
      writer.putNextFloat(this.levelX);
      writer.putNextFloat(this.levelY);
      writer.putNextShortUnsigned(this.bats.size());

      for (LevelMob<NightSwarmBatMob> bat : this.bats) {
         writer.putNextInt(bat.uniqueID);
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.masterMob = new LevelMob<>(reader.getNextInt());
      this.startBatCount = reader.getNextInt();
      this.batsMaxHealth = reader.getNextInt();
      this.levelX = reader.getNextFloat();
      this.levelY = reader.getNextFloat();
      this.bats.clear();
      int batCount = reader.getNextShortUnsigned();

      for (int i = 0; i < batCount; i++) {
         this.bats.add(new LevelMob<>(reader.getNextInt()));
      }
   }

   @Override
   public void init() {
      super.init();
      if (!this.isClient()) {
         MobHealthScaling.PlayerCount playerCount = MobHealthScaling.getPlayerCount(this.level, this.levelX, this.levelY);
         this.updateBatMaxHealthValue(playerCount.players, playerCount.partyMembers);
         int shareHitCooldownWithEvery = this.startBatCount / 5;
         if (this.bats.size() < this.startBatCount) {
            for (int i = this.bats.size(); i < this.startBatCount; i++) {
               int shareIndex = i / shareHitCooldownWithEvery * shareHitCooldownWithEvery;
               NightSwarmBatMob mob = (NightSwarmBatMob)MobRegistry.getMob("nightswarmbat", this.level);
               mob.setMaxHealth(this.batsMaxHealth);
               mob.setHealthHidden(mob.getMaxHealth());
               mob.setPos(this.levelX + GameRandom.globalRandom.getIntBetween(-100, 100), this.levelY + GameRandom.globalRandom.getIntBetween(-100, 100), true);
               mob.nightSwarmEventUniqueID = this.getUniqueID();
               if (shareIndex != i) {
                  mob.shareHitCooldownUniqueID = this.bats.get(shareIndex).uniqueID;
               }

               mob.batIndex = i;
               mob.idleXPos = this.levelX;
               mob.idleYPos = this.levelY;
               this.level.entityManager.mobs.add(mob);
               this.bats.add(new LevelMob<>(mob.getUniqueID()));
            }
         }
      } else {
         SoundManager.playSound(GameResources.nightswarmbegin, SoundEffect.effect(this.levelX, this.levelY).falloffDistance(4000));
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.updateBats(false);
      if (this.bats.isEmpty()) {
         this.over();
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.updateBats(true);
      long currentTime = this.level.getWorldEntity().getTime();
      if (this.nextUpdateTargetTime <= currentTime) {
         this.currentTarget = this.level
            .entityManager
            .players
            .streamAreaTileRange((int)this.levelX, (int)this.levelY, 100)
            .filter(m -> m != null && !m.removed() && m.isVisible())
            .findBestDistance(0, Comparator.comparingDouble(m -> m.getDistance(this.levelX, this.levelY)))
            .orElse(null);
         this.nextUpdateTargetTime = currentTime + GameRandom.globalRandom.getIntBetween(4000, 6000);
      } else if (this.currentTarget != null && (this.currentTarget.removed() || !this.currentTarget.isVisible())) {
         this.nextUpdateTargetTime = Math.min(this.nextUpdateTargetTime, currentTime + GameRandom.globalRandom.getIntBetween(1000, 2000));
      }

      if (this.currentTarget != null) {
         if (this.currentStage == -1) {
            this.stages.clear();
            this.stages.add(new MoveAroundNightSwarmEventStage(1, 500, false));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new FlyByNightSwarmEventStage(250, 150, 200, 150, 500, 400));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new MoveAroundNightSwarmEventStage(1, 450, false, 160));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new FlyByNightSwarmEventStage(200, 150, 200, 150, 450, 400));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new MoveAroundNightSwarmEventStage(1, 400, false, 160));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new FlyByNightSwarmEventStage(150, 150, 150, 200, 400, 350));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new WaitNightSwarmEventStage(100, 2000));
            this.stages.add(new MoveAroundNightSwarmEventStage(3, 600, true));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new ChargeNightSwarmEventStage(400));
            this.stages.add(new WaitNightSwarmEventStage(100, 2000));
            this.stages.add(new MoveAroundNightSwarmEventStage(4, 600, true));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new ChargeNightSwarmEventStage(400));
            this.stages.add(new WaitNightSwarmEventStage(100, 2000));
            this.stages.add(new MoveAroundNightSwarmEventStage(4, 600, true));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new ChargeNightSwarmEventStage(400));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new CircleChargeNightSwarmEventStage());
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new JailNightSwarmEventStage());
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.stages.add(new WaitNightSwarmEventStage(100, 2000));
            this.stages.add(new MoveAroundNightSwarmEventStage(2, 600, true));
            this.stages.add(new WaitMajorityCounterSwarmEventStage());
            this.currentStage++;
            this.stages.get(this.currentStage).onStarted(this);
         }

         NightSwarmEventStage stage = this.stages.get(this.currentStage);
         stage.serverTick(this);
         if (stage.hasCompleted(this)) {
            stage.onCompleted(this);
            this.currentStage++;
            if (this.currentStage >= this.stages.size()) {
               this.currentStage = -1;
            } else {
               this.stages.get(this.currentStage).onStarted(this);
            }
         }
      }

      if (this.currentTarget == null) {
         this.despawnTimer += 50;
         if (this.despawnTimer >= 5000) {
            for (LevelMob<NightSwarmBatMob> levelMob : this.bats) {
               NightSwarmBatMob mob = levelMob.get(this.level);
               if (mob != null) {
                  mob.remove();
               }
            }

            this.eventOverAction.runAndSend(false);
         }
      } else {
         this.despawnTimer = 0;
      }

      if (!this.isOver() && this.bats.isEmpty()) {
         NightSwarmStartMob master = this.masterMob.get(this.level);
         if (master != null) {
            master.setPos(this.levelX, this.levelY, true);
            master.addAttackers(this.attackers);
            master.remove(0.0F, 0.0F, null, true);
         }

         if (this.isServer()) {
            this.attackers
               .stream()
               .map(Attacker::getFirstPlayerOwner)
               .filter(Objects::nonNull)
               .filter(PlayerMob::isServerClient)
               .map(PlayerMob::getServerClient)
               .distinct()
               .forEach(c -> c.sendChatMessage(new LocalMessage("misc", "bossdefeat", "name", MobRegistry.getLocalization("nightswarm"))));
            if (!this.isDamagedByPlayers) {
               AchievementManager.checkMeAndThisArmyKill(this.getLevel(), this.attackers);
            }
         }

         this.eventOverAction.runAndSend(true);
      }
   }

   public void updateBats(boolean authoritative) {
      int currentHealth = 0;
      ListIterator<LevelMob<NightSwarmBatMob>> li = this.bats.listIterator();
      float nextLevelX = 0.0F;
      float nextLevelY = 0.0F;
      int batsRemaining = 0;
      HashSet<Integer> removes = new HashSet<>();
      if (authoritative) {
         this.updatePlayerAndPartyMemberCount();
      }

      LinkedList<NightSwarmBatMob> aliveBats = new LinkedList<>();
      this.batsDoneWithStages = 0;

      while (li.hasNext()) {
         LevelMob<NightSwarmBatMob> next = li.next();
         NightSwarmBatMob mob = next.get(this.level);
         if (mob != null && !mob.removed()) {
            batsRemaining++;
            currentHealth += mob.getHealth();
            nextLevelX += mob.x;
            nextLevelY += mob.y;
            if (mob.currentStage == null && mob.stages.isEmpty()) {
               this.batsDoneWithStages++;
            }

            aliveBats.add(mob);
         } else if (authoritative) {
            li.remove();
            removes.add(next.uniqueID);
         }
      }

      for (int uniqueID : removes) {
         this.removeBatAction.runAndSend(uniqueID);
      }

      if (batsRemaining > 0) {
         this.levelX = nextLevelX / batsRemaining;
         this.levelY = nextLevelY / batsRemaining;
      }

      BossNearbyBuff.applyAround(this.level, this.levelX, this.levelY, 1600);
      NightSwarmStartMob master = this.masterMob.get(this.level);
      if (master != null) {
         master.setPos(this.levelX, this.levelY, true);
      }

      int maxHealth = (int)(this.startBatCount * this.batsMaxHealth * this.level.buffManager.getModifier(LevelModifiers.ENEMY_MAX_HEALTH));
      this.lastHealthProgress = GameMath.limit(1.0F - (float)currentHealth / maxHealth, 0.0F, 1.0F);
      aliveBats.forEach(m -> m.setSpeed(100.0F + this.lastHealthProgress * 80.0F));
      if (master != null) {
         master.updateHealth(currentHealth, this.startBatCount * this.batsMaxHealth);
         if (this.isClient() && master.isClientPlayerNearby()) {
            EventStatusBarManager.registerEventStatusBar(
               this.getUniqueID(), currentHealth, maxHealth, EventStatusBarData.BarCategory.boss, new LocalMessage("mob", "nightswarm")
            );
         }
      }
   }

   public void updatePlayerAndPartyMemberCount() {
      MobHealthScaling.PlayerCount nextCount = MobHealthScaling.getPlayerCount(this.level, this.levelX, this.levelY);
      GameDifficulty nextDifficulty = this.level.getWorldSettings().difficulty;
      if (this.playerCount < nextCount.players || this.partyMemberCount != nextCount.partyMembers || this.difficulty != nextDifficulty) {
         this.updatePlayerCountAction.runAndSend(nextCount.players, nextCount.partyMembers);
      }
   }

   public void updateBatMaxHealthValue(int nextPlayerCount, int nextPartyMemberCount) {
      this.playerCount = nextPlayerCount;
      this.partyMemberCount = nextPartyMemberCount;
      this.difficulty = this.level.getWorldSettings().difficulty;
      this.batsMaxHealth = Math.round(
         BAT_MAX_HEALTH.get(this.level).intValue()
            * GameUtils.getMultiplayerScaling(this.playerCount)
            * GameUtils.getMultiplayerScaling(this.partyMemberCount + 1, Integer.MAX_VALUE, 0.2F, 0.02F)
      );
   }

   public Iterable<NightSwarmBatMob> getBats(boolean nullable) {
      return () -> this.bats.stream().map(lm -> lm.get(this.level)).filter(m -> nullable || m != null && !m.removed()).iterator();
   }

   @Override
   public Point getSaveToRegionPos() {
      return new Point(
         this.level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(this.levelX)),
         this.level.regionManager.getRegionCoordByTile(GameMath.getTileCoordinate(this.levelY))
      );
   }

   @Override
   public void onDispose() {
      super.onDispose();
   }

   public interface DebugHudDraw {
      DrawOptions get(GameCamera var1, PlayerMob var2);
   }
}
