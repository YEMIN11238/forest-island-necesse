package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs;

import java.awt.Color;
import java.io.FileNotFoundException;
import necesse.engine.localization.Localization;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffEventSubscriber;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;

public class RubySetBonusBuff extends SetBonusBuff {
   private GameTexture blueTracker;
   private GameTexture greenTracker;
   private GameTexture purpleTracker;
   private GameTexture orangeTracker;

   @Override
   public void init(ActiveBuff buff, BuffEventSubscriber eventSubscriber) {
      this.canCancel = false;
      this.isVisible = true;
      this.isPassive = true;
      if (buff.owner.isClient() && !buff.getGndData().getBoolean("tooltipInit")) {
         this.spawnSetAssembledParticles(buff);
      }
   }

   private void spawnSetAssembledParticles(ActiveBuff buff) {
      int particleCount = 25;
      Mob owner = buff.owner;
      GameRandom random = GameRandom.globalRandom;
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      float anglePerParticle = 360.0F / particleCount;

      for (int i = 0; i < particleCount; i++) {
         int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * 50.0F;
         float dy = (float)Math.cos(Math.toRadians(angle)) * 50.0F;
         owner.getLevel()
            .entityManager
            .addParticle(owner, typeSwitcher.next())
            .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
            .sizeFades(22, 44)
            .movesFriction(dx * 2.0F, dy * 2.0F, 0.8F)
            .color(new Color(255, 125, 175))
            .givesLight(247.0F, 0.3F)
            .heightMoves(0.0F, 30.0F)
            .lifeTime(1500);
      }
   }

   @Override
   public void clientTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   @Override
   public void serverTick(ActiveBuff buff) {
      this.updateModifiers(buff);
   }

   public void updateModifiers(ActiveBuff buff) {
      Mob owner = buff.owner;
      int additionalSummons = this.getAdditionalSummonsCount(owner);
      buff.setModifier(BuffModifiers.MAX_SUMMONS, additionalSummons);
      if (owner.isPlayer && additionalSummons >= 4) {
         PlayerMob player = (PlayerMob)owner;
         if (player.isServerClient()) {
            ServerClient client = player.getServerClient();
            if (client.achievementsLoaded()) {
               client.achievements().CRYSTALLIZED.markCompleted(client);
            }
         }
      }
   }

   public int getAdditionalSummonsCount(Mob owner) {
      return (int)Math.floor(owner.getMaxResilience() / 150.0);
   }

   @Override
   public int getStacksDisplayCount(ActiveBuff buff) {
      return this.getAdditionalSummonsCount(buff.owner);
   }

   @Override
   public GameTexture getDrawIcon(ActiveBuff buff) {
      int additionalSummons = this.getAdditionalSummonsCount(buff.owner);
      if (additionalSummons <= 1) {
         return this.blueTracker;
      } else if (additionalSummons == 2) {
         return this.greenTracker;
      } else {
         return additionalSummons == 3 ? this.purpleTracker : this.orangeTracker;
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.blueTracker = this.getSummonTrackerTexture("blue");
      this.greenTracker = this.getSummonTrackerTexture("green");
      this.purpleTracker = this.getSummonTrackerTexture("purple");
      this.orangeTracker = this.getSummonTrackerTexture("orange");
   }

   protected GameTexture getSummonTrackerTexture(String color) {
      try {
         return GameTexture.fromFileRaw("buffs/ruby" + color + "summontracker");
      } catch (FileNotFoundException e) {
         return GameTexture.fromFile("buffs/unknown");
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      if (blackboard.get(InventoryItem.class, "setItem") != null) {
         tooltips.add(Localization.translate("itemtooltip", "rubysetnew"), 400);
      } else {
         int additionalSummons = this.getAdditionalSummonsCount(ab.owner);
         String tooltipText;
         if (additionalSummons <= 1) {
            tooltipText = Localization.translate("itemtooltip", "rubybluetext", "summoncount", additionalSummons);
         } else if (additionalSummons == 2) {
            tooltipText = Localization.translate("itemtooltip", "rubygreentext", "summoncount", additionalSummons);
         } else if (additionalSummons == 3) {
            tooltipText = Localization.translate("itemtooltip", "rubypurpletext", "summoncount", additionalSummons);
         } else {
            tooltipText = Localization.translate("itemtooltip", "rubyorangetext", "summoncount", additionalSummons);
         }

         tooltips.add(tooltipText, 300);
      }

      return tooltips;
   }
}
