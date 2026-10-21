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

public class AmethystSetBonusBuff extends SetBonusBuff {
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
            .color(new Color(184, 174, 255))
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
      float updatedDamageBuff = this.getAddedDamageFromResilience(owner);
      buff.setModifier(BuffModifiers.ALL_DAMAGE, updatedDamageBuff);
      if (owner.isPlayer && owner.getMaxResilience() > 400) {
         PlayerMob player = (PlayerMob)owner;
         if (player.isServerClient()) {
            ServerClient client = player.getServerClient();
            if (client.achievementsLoaded()) {
               client.achievements().CRYSTALLIZED.markCompleted(client);
            }
         }
      }
   }

   public float getAddedDamageFromResilience(Mob owner) {
      return owner.getMaxResilience() / 10.0F / 100.0F;
   }

   @Override
   public GameTexture getDrawIcon(ActiveBuff buff) {
      Mob owner = buff.owner;
      if (owner.getMaxResilience() < 100) {
         return this.blueTracker;
      } else if (owner.getMaxResilience() < 200) {
         return this.greenTracker;
      } else {
         return owner.getMaxResilience() < 400 ? this.purpleTracker : this.orangeTracker;
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();

      try {
         this.blueTracker = GameTexture.fromFileRaw("buffs/amethystbluedamagetracker");
         this.greenTracker = GameTexture.fromFileRaw("buffs/amethystgreendamagetracker");
         this.purpleTracker = GameTexture.fromFileRaw("buffs/amethystpurpledamagetracker");
         this.orangeTracker = GameTexture.fromFileRaw("buffs/amethystorangedamagetracker");
      } catch (FileNotFoundException e) {
         this.blueTracker = GameTexture.fromFile("buffs/unknown");
         this.greenTracker = GameTexture.fromFile("buffs/unknown");
         this.purpleTracker = GameTexture.fromFile("buffs/unknown");
         this.orangeTracker = GameTexture.fromFile("buffs/unknown");
      }
   }

   @Override
   public ListGameTooltips getTooltip(ActiveBuff ab, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltip(ab, blackboard);
      if (blackboard.get(InventoryItem.class, "setItem") != null) {
         tooltips.add(Localization.translate("itemtooltip", "amethystset"), 400);
      } else {
         Mob owner = ab.owner;
         int damageIncrease = (int)Math.floor(this.getAddedDamageFromResilience(owner) * 100.0F);
         String tooltipText;
         if (owner.getMaxResilience() <= 100) {
            tooltipText = Localization.translate("itemtooltip", "amethystbluetext", "damage", damageIncrease);
         } else if (owner.getMaxResilience() <= 200) {
            tooltipText = Localization.translate("itemtooltip", "amethystgreentext", "damage", damageIncrease);
         } else if (owner.getMaxResilience() <= 400) {
            tooltipText = Localization.translate("itemtooltip", "amethystpurpletext", "damage", damageIncrease);
         } else {
            tooltipText = Localization.translate("itemtooltip", "amethystorangetext", "damage", damageIncrease);
         }

         tooltips.add(tooltipText, 300);
      }

      return tooltips;
   }
}
