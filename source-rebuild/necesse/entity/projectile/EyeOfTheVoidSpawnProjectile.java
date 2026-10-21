package necesse.entity.projectile;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.postProcessing.PostProcessingEffects;
import necesse.engine.postProcessing.effects.AbstractShockwaveEffect;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.CheckSlotType;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob.WanderbotFollowingMob;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.summonToolItem.SummonToolItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class EyeOfTheVoidSpawnProjectile extends Projectile {
   private SummonToolItem summonToolItem;
   private InventoryItem inventoryItem;
   protected long spawnTime;
   protected long deathTime;
   protected boolean hasSpawnedBot = false;

   public EyeOfTheVoidSpawnProjectile() {
   }

   public EyeOfTheVoidSpawnProjectile(
      Level level,
      Mob owner,
      float x,
      float y,
      int targetX,
      int targetY,
      int distance,
      GameDamage damage,
      int knockback,
      SummonToolItem summonToolItem,
      InventoryItem inventoryItem
   ) {
      this.setLevel(level);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.setDamage(damage);
      this.knockback = knockback;
      this.setDistance(distance);
      this.setOwner(owner);
      this.distance = distance;
      this.summonToolItem = summonToolItem;
      this.inventoryItem = inventoryItem;
   }

   public EyeOfTheVoidSpawnProjectile(
      Level level,
      Mob owner,
      int targetX,
      int targetY,
      int distance,
      GameDamage damage,
      int knockback,
      SummonToolItem summonToolItem,
      InventoryItem inventoryItem
   ) {
      this(level, owner, owner.x, owner.y, targetX, targetY, distance, damage, knockback, summonToolItem, inventoryItem);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.targetX);
      writer.putNextFloat(this.targetY);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.targetX = reader.getNextFloat();
      this.targetY = reader.getNextFloat();
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getTime();
      this.isSolid = false;
      this.canHitMobs = false;
      this.trailOffset = 0.0F;
      if (this.isClient()) {
         PostProcessingEffects.addShockwaveEffect(new AbstractShockwaveEffect() {
            @Override
            public int getDrawX(GameCamera camera) {
               return camera.getDrawX(EyeOfTheVoidSpawnProjectile.this.x);
            }

            @Override
            public int getDrawY(GameCamera camera) {
               return camera.getDrawY(EyeOfTheVoidSpawnProjectile.this.y) - (int)EyeOfTheVoidSpawnProjectile.this.getHeight();
            }

            @Override
            public float getCurrentDistance() {
               return 0.0F;
            }

            @Override
            public float getSize() {
               return EyeOfTheVoidSpawnProjectile.this.getBlackHoleSize();
            }

            @Override
            public float getEasingScale() {
               return 3.4F;
            }

            @Override
            public float getEasingPower() {
               return 0.65F;
            }

            @Override
            public boolean shouldRemove() {
               return EyeOfTheVoidSpawnProjectile.this.removed();
            }
         });
      }
   }

   protected float getBlackHoleSize() {
      float travelPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      float lastFifteenPerc = GameMath.limit(travelPerc - 0.85F, 0.0F, 1.0F) * 10.0F;
      float lastFivePerc = GameMath.limit(GameMath.lerp((travelPerc - 0.98F) * 50.0F, 1.0F, 0.0F), 0.0F, 1.0F);
      return travelPerc > 0.95F ? lastFivePerc * 32.0F * 3.0F : lastFifteenPerc * 32.0F * 3.0F;
   }

   @Override
   public Color getParticleColor() {
      return new Color(159, 31, 177);
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   protected void spawnDeathParticles() {
      ExplosionEvent.spawnExplosionParticles(
         this.getLevel(),
         this.x,
         this.y,
         12,
         15.0F,
         25.0F,
         (level, x, y, dirX, dirY, lifeTime, currentRange) -> level.entityManager
            .addParticle(x, y - 8.0F, Particle.GType.CRITICAL)
            .movesConstant(dirX * 0.1F, dirY * 0.05F)
            .color(new Color(255, 56, 155, 237))
            .heightMoves(5.0F, 55.0F)
            .sizeFades(25, 45)
            .onProgress(
               0.1F,
               p -> {
                  Float norm = GameMath.normalize(dirX, dirY);
                  level.entityManager
                     .addParticle(p.x + norm.x * 20.0F, p.y + norm.y * 10.0F, Particle.GType.IMPORTANT_COSMETIC)
                     .movesConstant(dirX * 0.2F, dirY * 0.1F)
                     .color(new Color(255, 98, 200, 255))
                     .heightMoves(10.0F, 60.0F)
                     .lifeTime(lifeTime);
               }
            )
            .lifeTime(lifeTime * 2)
      );
   }

   @Override
   public float tickMovement(float delta) {
      float travelPerc = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      float travelPercInv = Math.abs(travelPerc - 1.0F);
      this.speed = GameMath.limit(150.0F * travelPercInv, 5.0F, 150.0F);
      this.height = 20.0F * travelPercInv;
      if (travelPerc >= 0.94F && !this.hasSpawnedBot && !this.isClient()) {
         this.spawnWanderbot();
         this.hasSpawnedBot = true;
      }

      return super.tickMovement(delta);
   }

   protected void spawnWanderbot() {
      if (!this.isClient()) {
         Mob owner = this.getOwner();
         if (owner != null && !owner.removed() && owner.isItemAttacker) {
            ItemAttackerMob itemAttackerMob = (ItemAttackerMob)owner;
            WanderbotFollowingMob wanderbotMob = new WanderbotFollowingMob();
            itemAttackerMob.serverFollowersManager
               .addFollower(
                  this.summonToolItem.summonType,
                  wanderbotMob,
                  FollowPosition.CIRCLE_FAR,
                  "summonedmob",
                  1.0F,
                  mob -> this.summonToolItem.getMaxSummons(this.inventoryItem, mob),
                  null,
                  false
               );
            wanderbotMob.updateDamage(this.summonToolItem.getAttackDamage(this.inventoryItem));
            wanderbotMob.setEnchantment(this.summonToolItem.getEnchantment(this.inventoryItem));
            if (!owner.isPlayer) {
               wanderbotMob.setRemoveWhenNotInInventory(ItemRegistry.getItem("eyeofthevoid"), CheckSlotType.WEAPON);
            }

            Float spawnPoint = new Float(this.targetX, this.targetY + 32.0F);
            owner.getLevel().entityManager.addMob(wanderbotMob, spawnPoint.x, spawnPoint.y);
         }
      }
   }

   @Override
   public float getAngle() {
      return (float)(this.getWorldEntity().getTime() - this.spawnTime) * 0.1F;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      if (!this.removed()) {
         GameLight light = level.getLightLevel(this.getTileX(), this.getTileY());
         int drawX = camera.getDrawX(this.x) - 24;
         int drawY = camera.getDrawY(this.y) - 24;
         int spriteX = GameUtils.getAnim(level.getLocalTime(), 6, 600);
         TextureDrawOptions options = this.texture
            .initDraw()
            .sprite(spriteX, 0, 48)
            .light(new GameLight(GameMath.max(light.getLevel(), 100.0F)))
            .rotate(this.getAngle(), 24, 24)
            .pos(drawX, drawY - (int)this.getHeight());
         int shadowX = camera.getDrawX(this.x) - this.shadowTexture.getWidth() / 2;
         int shadowY = camera.getDrawY(this.y) - this.shadowTexture.getHeight() / 2;
         TextureDrawOptions shadowOptions = this.shadowTexture.initDraw().light(light).rotate(this.angle).alpha(0.2F).pos(shadowX, shadowY);
         topList.add(tm -> {
            options.draw();
            shadowOptions.draw();
         });
      }
   }
}
