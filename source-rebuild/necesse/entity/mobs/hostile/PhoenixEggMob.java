package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PhoenixEggMob extends HostileMob {
   public static LootTable lootTable = new LootTable(LootItem.between("phoenixfeather", 1, 2));
   public static int TIME_TO_HATCH = 10000;
   public static int TIME_TO_DEATH = 5000;
   protected long spawnTime;
   protected boolean hasHatched = false;
   protected BooleanMobAbility setHatchedAbility;

   public PhoenixEggMob() {
      super(2000);
      this.collision = new Rectangle(-13, -12, 26, 20);
      this.hitBox = new Rectangle(-25, -24, 50, 38);
      this.selectBox = new Rectangle(-30, -45, 60, 60);
      this.setKnockbackModifier(0.0F);
      this.setArmor(60);
      this.setSpeed(0.0F);
      this.setHatchedAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            PhoenixEggMob.this.hasHatched = value;
         }
      });
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addLong("spawnTime", this.spawnTime);
      save.addBoolean("hasHatched", this.hasHatched);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.spawnTime = save.getLong("spawnTime", this.spawnTime, false);
      this.hasHatched = save.getBoolean("hasHatched", this.hasHatched, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.spawnTime);
      writer.putNextBoolean(this.hasHatched);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.spawnTime = reader.getNextLong();
      this.hasHatched = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      if (this.spawnTime == 0L) {
         this.spawnTime = this.getTime();
      }
   }

   public int getTimeSinceSpawned() {
      return (int)(this.getTime() - this.spawnTime);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (!this.hasHatched) {
         int maxParticles = 3;
         float progress = (float)this.getTimeSinceSpawned() / TIME_TO_HATCH;
         int particleCount = (int)(maxParticles * progress);

         for (int i = 0; i < particleCount; i++) {
            this.getLevel()
               .entityManager
               .addTopParticle(
                  this, GameRandom.globalRandom.floatGaussian() * 4.0F, GameRandom.globalRandom.floatGaussian() * 8.0F, Particle.GType.IMPORTANT_COSMETIC
               )
               .color(
                  (options, lifeTime, timeAlive, lifePercent) -> {
                     Color startColor = new Color(255, 246, 79);
                     Color endColor = new Color(255, 0, 0);
                     lifePercent = GameMath.limit(lifePercent, 0.0F, 1.0F);
                     Color color = new Color(
                        GameMath.lerp(lifePercent, startColor.getRed(), endColor.getRed()),
                        GameMath.lerp(lifePercent, startColor.getGreen(), endColor.getGreen()),
                        GameMath.lerp(lifePercent, startColor.getBlue(), endColor.getBlue())
                     );
                     options.color(color);
                  }
               )
               .ignoreLight(true)
               .heightMoves(16.0F, 50.0F)
               .givesLight(75.0F, 0.5F);
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (!this.hasHatched && this.getTimeSinceSpawned() >= TIME_TO_HATCH) {
         PhoenixMob mob = new PhoenixMob();
         if (this.spawnTilePosition != null) {
            mob.setSpawnTilePosition(this.spawnTilePosition.x, this.spawnTilePosition.y);
         }

         this.getLevel().entityManager.addMob(mob, this.getX(), this.getY());
         this.setHatchedAbility.runAndSend(true);
      }

      if (this.getTimeSinceSpawned() >= TIME_TO_HATCH + TIME_TO_DEATH) {
         this.setHealth(0);
      }
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return this.hasHatched ? false : super.canBeHit(attacker);
   }

   @Override
   public boolean canBeTargeted(Mob attacker, NetworkClient attackerClient) {
      return this.hasHatched ? false : super.canBeTargeted(attacker, attackerClient);
   }

   @Override
   public void setHealthHidden(int health, float knockbackX, float knockbackY, Attacker attacker, boolean fromNetworkUpdate) {
      if (health <= 0 && this.hasHatched && this.getTimeSinceSpawned() < TIME_TO_HATCH + TIME_TO_DEATH) {
         health = 1;
      }

      super.setHealthHidden(health, knockbackX, knockbackY, attacker, fromNetworkUpdate);
   }

   @Override
   public boolean isHealthBarVisible() {
      return !this.hasHatched;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public LootTable getLootTable() {
      return this.hasHatched ? new LootTable() : lootTable;
   }

   private void addHatchParticles() {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.phoenixEgg, i, 2, 32, this.x, this.y, 10.0F, 0.0F, 0.0F),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.phoenixEgg, i, 2, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void addDrawables(
      List<MobDrawable> mobList,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(mobList, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int timeSinceSpawned = this.getTimeSinceSpawned();
      float shadowAlpha = !this.hasHatched
         ? Math.min(1.0F, (float)timeSinceSpawned / TIME_TO_HATCH)
         : Math.max(0.0F, 1.0F - (float)timeSinceSpawned / (TIME_TO_HATCH + TIME_TO_DEATH));
      int sprite = this.hasHatched ? 1 : 0;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 43;
      final TextureDrawOptions shadow = MobRegistry.Textures.phoenixEgg_shadow
         .initDraw()
         .sprite(sprite, 0, 64)
         .alpha(shadowAlpha)
         .light(light.minLevelCopy(150.0F))
         .pos(drawX, drawY);
      mobList.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            shadow.draw();
         }
      });
      final TextureDrawOptions drawOptions = MobRegistry.Textures.phoenixEgg
         .initDraw()
         .sprite(sprite, 0, 64)
         .rotate(0.0F, 32, 40)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      mobList.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
   }
}
