package necesse.entity.projectile.bulletProjectile;

import java.awt.Color;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.SeedObjectItem;
import necesse.level.maps.LevelObjectHit;

public class SeedBulletProjectile extends BulletProjectile {
   private SeedObjectItem seedItem;
   private Buff buff;
   private float baseSpeed;
   private int baseDistance;
   private int baseKnockback;
   private int lifeGain;

   public SeedBulletProjectile() {
   }

   public SeedBulletProjectile(float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, Mob owner) {
      super(x, y, targetX, targetY, speed, distance, damage, knockback, owner);
      this.baseSpeed = speed;
      this.baseDistance = distance;
      this.baseKnockback = knockback;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.baseSpeed);
      writer.putNextInt(this.baseDistance);
      writer.putNextInt(this.baseKnockback);
      writer.putNextShortUnsigned(this.seedItem == null ? 0 : this.seedItem.getID());
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.baseSpeed = reader.getNextFloat();
      this.baseDistance = reader.getNextInt();
      this.baseKnockback = reader.getNextInt();
      int seedItemID = reader.getNextShortUnsigned();
      if (seedItemID != 0) {
         Item item = ItemRegistry.getItem(seedItemID);
         if (item instanceof SeedObjectItem) {
            this.seedItem = (SeedObjectItem)item;
         }
      }
   }

   @Override
   public void init() {
      super.init();
      this.particleSpeedMod = 0.03F;
      if (this.seedItem != null) {
         String seedStringID = this.seedItem.getStringID();
         switch (seedStringID) {
            case "pumpkinseed":
               this.knockback = this.baseKnockback * 2;
               break;
            case "iceblossomseed":
               this.buff = BuffRegistry.Debuffs.FROSTSLOW;
               break;
            case "firemoneseed":
            case "chilipepperseed":
               this.buff = BuffRegistry.Debuffs.ON_FIRE;
               break;
            case "cabbageseed":
            case "tomatoseed":
            case "potatoseed":
            case "sunflowerseed":
               this.lifeGain = 1;
               break;
            case "eggplantseed":
            case "onionseed":
            case "sugarbeetseed":
            case "beetseed":
               this.speed = this.baseSpeed * 2.0F;
               this.distance = this.baseDistance * 2;
         }
      }
   }

   @Override
   public void onMaxMoveTick() {
      if (this.isClient()) {
         this.spawnSpinningParticle();
      }
   }

   @Override
   public Color getParticleColor() {
      return this.seedItem == null ? null : this.seedItem.getSeedObject().mapColor;
   }

   @Override
   protected Color getWallHitColor() {
      return this.getParticleColor();
   }

   @Override
   public void applyDamage(Mob mob, float x, float y, float knockbackDirX, float knockbackDirY) {
      GameDamage variantDamage = this.getDamage();
      if (this.seedItem != null && this.seedItem.getStringID().equals("carrotseed")) {
         variantDamage = variantDamage.modDamage(1.2F);
      }

      mob.isServerHit(variantDamage, knockbackDirX, knockbackDirY, this.knockback, this);
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (mob != null) {
         Mob owner = this.getOwner();
         if (this.buff != null) {
            mob.buffManager.addBuff(new ActiveBuff(this.buff, mob, 10000, owner), true);
         }

         if (owner != null && this.lifeGain > 0 && mob.canBeHit(owner)) {
            owner.setHealthHidden(owner.getHealth() + this.lifeGain);
         }
      }
   }

   public void setSeedBulletVariant(SeedObjectItem seedObjectItem) {
      this.seedItem = seedObjectItem;
   }

   @Override
   protected void dropItem() {
      if (this.seedItem != null) {
         this.getLevel().entityManager.pickups.add(new InventoryItem(this.seedItem.getStringID()).getPickupEntity(this.getLevel(), this.x, this.y));
      }
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), this.getParticleColor(), 22.0F, 100, this.getHeight());
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      return trail;
   }

   @Override
   public void refreshParticleLight() {
   }

   @Override
   public void playHitSound(float x, float y) {
      SoundManager.playSound(GameResources.flick, SoundEffect.effect(x, y).volume(1.5F));
   }
}
