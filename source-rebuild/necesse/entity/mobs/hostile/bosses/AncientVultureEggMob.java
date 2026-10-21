package necesse.entity.mobs.hostile.bosses;

import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.entity.Entity;
import necesse.entity.mobs.MaxHealthGetter;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AncientVultureEggMob extends BossMob {
   public static LootTable lootTable = new LootTable();
   public static MaxHealthGetter MAX_HEALTH = new MaxHealthGetter(30, 60, 75, 85, 110);
   private long spawnTime;
   private final int hatchTime = 6000;
   private final AncientVultureMob owner;
   protected float lifePercent = 0.0F;
   protected int currentHatchStage = 0;

   public AncientVultureEggMob() {
      this(null);
   }

   public AncientVultureEggMob(AncientVultureMob owner) {
      super(100);
      this.difficultyChanges.setMaxHealth(MAX_HEALTH);
      this.isSummoned = true;
      this.owner = owner;
      this.collision = new Rectangle(-10, -12, 20, 20);
      this.hitBox = new Rectangle(-20, -20, 40, 40);
      this.selectBox = new Rectangle(-18, -45, 36, 58);
      this.setKnockbackModifier(0.0F);
      this.setArmor(20);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.spawnTime = reader.getNextLong();
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextLong(this.spawnTime);
   }

   @Override
   public void init() {
      super.init();
      if (this.spawnTime == 0L) {
         this.spawnTime = this.getWorldEntity().getTime();
      }
   }

   @Override
   public void tickMovement(float delta) {
      this.dx = 0.0F;
      this.dy = 0.0F;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.getWorldEntity().getTime() > this.spawnTime + 6000L) {
         VultureHatchling mob = new VultureHatchling(this.owner);
         this.getLevel().entityManager.addMob(mob, this.getX(), this.getY());
         if (this.owner != null) {
            this.owner.spawnedMobs.removeIf(Entity::removed);
            this.owner.spawnedMobs.add(mob);
         }

         this.setHealth(0);
      }
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.crackdeath).volume(1.4F).fallOffDistance(2000);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.ancientVultureEgg, i, 2, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.lifePercent = (float)(this.getWorldEntity().getTime() - this.spawnTime) / 6000.0F;
      int lastHatchStage = this.currentHatchStage;
      this.currentHatchStage = (int)(this.lifePercent * 4.0F);
      if (lastHatchStage != this.currentHatchStage) {
         this.playEggShake();
      }
   }

   protected void playEggShake() {
      SoundManager.playSound(new SoundSettings(GameResources.crack).volume(1.2F).fallOffDistance(2000), this);
   }

   @Override
   protected void addDrawables(
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 54;
      final TextureDrawOptions shadowOptions = this.getShadowDrawOptions(level, x, y, light, camera);
      float stagePerc = this.lifePercent * 4.0F - this.currentHatchStage;
      float rotate;
      if (stagePerc < 0.3F && this.currentHatchStage >= 1) {
         float rotatePerc = stagePerc / 0.3F;
         double sinfactor = Math.sin(rotatePerc * 4.0F * Math.PI);
         rotate = (float)sinfactor * 15.0F;
      } else {
         rotate = 0.0F;
      }

      int sprite = this.currentHatchStage / 2;
      int spriteSection = 32 * (this.currentHatchStage % 2);
      final DrawOptions options = MobRegistry.Textures.ancientVultureEgg
         .initDraw()
         .spriteSection(sprite, 0, 64, spriteSection, 32 + spriteSection, 0, 64, false)
         .rotate(rotate, 16, 54)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            shadowOptions.draw();
            options.draw();
         }
      });
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.ancientVultureEgg_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2 + 2;
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }
}
