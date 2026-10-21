package necesse.entity.mobs.friendly.human.humanShop;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Stream;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.SettlerPersonalityRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HumanAI;
import necesse.entity.mobs.ai.behaviourTree.util.AIMover;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.HumanGender;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;

public class FriendlyJonasHumanMob extends HumanShop {
   protected ParticleTypeSwitcher explosionTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public FriendlyJonasHumanMob() {
      super(20000, 200, null);
      this.attackCooldown = 600;
      this.attackAnimTime = 500;
      this.setSwimSpeed(1.0F);
      this.setSpeed(40.0F);
      InventoryItem kineticChargerItem = new InventoryItem("kineticcharger");
      kineticChargerItem.item.setUpgradeTier(kineticChargerItem, 1.0F);
      this.equipmentInventory.setItem(6, kineticChargerItem);
   }

   @Override
   public void init() {
      super.init();
      this.updateAI();
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.empty();
   }

   @Override
   public LootTable getLootTable() {
      return new LootTable(new LootItem("jonasglasses").preventLootMultiplier());
   }

   @Override
   protected void setupPersonalities() {
      this.personalities = new ArrayList<>(Collections.singletonList(SettlerPersonalityRegistry.getNewSettlerPersonality("gamedevonnecesse", this)));
   }

   public void updateAI() {
      HumanAI<FriendlyJonasHumanMob> friendlyJonasHumanMobHumanAI = new HumanAI<>(640, false, false, 8000);
      friendlyJonasHumanMobHumanAI.wandererAINode.hideInside = mob -> true;
      friendlyJonasHumanMobHumanAI.wandererAINode.runAwayFromAttacker = false;
      friendlyJonasHumanMobHumanAI.humanTargetFinderAI.humanAngerTargetAINode.angerDecreasePerSecond = 0.0F;
      this.ai = new BehaviourTreeAI<>(this, friendlyJonasHumanMobHumanAI, new AIMover(2000));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 100; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         float var10001 = this.x + 4.0F;
         this.getLevel()
            .entityManager
            .addParticle(var10001, this.y - 10.0F, this.explosionTypeSwitcher.next())
            .rotates()
            .color(new Color(171, 27, 2))
            .movesConstant(dir.x * GameRandom.globalRandom.getIntBetween(12, 36), dir.y * GameRandom.globalRandom.getIntBetween(12, 36))
            .height(10.0F)
            .givesLight(75.0F, 0.5F)
            .heightMoves(0.0F, 20.0F)
            .onProgress(
               0.05F,
               p -> {
                  Float norm = GameMath.normalize(dir.x, dir.y);
                  float var10001x = p.x + norm.x * 15.0F;
                  float var10002 = p.y + norm.y * 15.0F;
                  this.getLevel()
                     .entityManager
                     .addParticle(var10001x, var10002, Particle.GType.IMPORTANT_COSMETIC)
                     .rotates()
                     .height(10.0F)
                     .color(new Color(215, 65, 0))
                     .movesConstant(dir.x * GameRandom.globalRandom.getIntBetween(12, 36), dir.y * GameRandom.globalRandom.getIntBetween(12, 36))
                     .heightMoves(0.0F, 20.0F)
                     .onProgress(
                        0.1F,
                        p2 -> {
                           Float norm2 = GameMath.normalize(dir.x, dir.y);
                           float var10001xx = p2.x + norm2.x * 20.0F;
                           float var10002x = p2.y + norm2.y * 20.0F;
                           this.getLevel()
                              .entityManager
                              .addParticle(var10001xx, var10002x, Particle.GType.IMPORTANT_COSMETIC)
                              .rotates()
                              .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
                              .height(10.0F)
                              .color(new Color(229, 130, 0))
                              .movesConstant(dir.x * GameRandom.globalRandom.getIntBetween(12, 36), dir.y * GameRandom.globalRandom.getIntBetween(12, 36))
                              .heightMoves(0.0F, 20.0F)
                              .lifeTime(2000);
                        }
                     )
                     .lifeTime(2000);
               }
            )
            .lifeTime(2000);
      }
   }

   protected Color getParticleColor(GameRandom random) {
      return new Color(random.getIntBetween(22, 77), random.getIntBetween(22, 77), random.getIntBetween(22, 77));
   }

   @Override
   public boolean shouldOnlyUseHumanLikeLook() {
      return false;
   }

   @Override
   public void setDefaultArmor(HumanDrawOptions drawOptions) {
      drawOptions.helmet(new InventoryItem("jonasglasses"));
      drawOptions.chestplate(new InventoryItem("animalkeepershirt"));
      drawOptions.boots(new InventoryItem("animalkeepershoes"));
   }

   @Override
   public void randomizeLook(HumanLook look, HumanGender gender, GameRandom random) {
      this.gender = HumanGender.MALE;
      super.randomizeLook(look, this.gender, random);
      look.setFacialFeature(6);
      look.setEyeType(1);
      look.setSkin(1);
      look.setHair(4);
      look.setHairColor(0);
      look.setEyeColor(2);
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.isServer()) {
         this.getServer().network.sendToClientsWithEntity(new PacketMobChat(this.getUniqueID(), Localization.translate("mobmsg", "friendlyjonasdies")), this);
      }
   }

   @Override
   protected ArrayList<GameMessage> getAttackMessages() {
      return !this.defendsHimselfAgainstPlayers() ? this.getAngryMessages() : this.getLocalMessages("friendlyjonasattack", 1);
   }

   @Override
   protected ArrayList<GameMessage> getAngryMessages() {
      return this.getLocalMessages("friendlyjonasangry", 4);
   }

   @Override
   public PacketOpenContainer getOpenShopPacket(Server server, ServerClient client) {
      return this.getShopContainerData(client).getPacket(ContainerRegistry.JONAS_CONTAINER, this);
   }

   @Override
   public void updateTeam() {
      if (this.getLevel() != null && !this.isClient()) {
         if (!this.isSettler()) {
            this.team.set(-1);
            this.owner.set(-1L);
         } else {
            super.updateTeam();
         }
      }
   }

   @Override
   public Predicate<Mob> filterHumanTargets() {
      return m -> false;
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }

   @Override
   public boolean isLavaImmune() {
      return true;
   }
}
