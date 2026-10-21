package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketRequestLevelEvent;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.FurnaceGolemOverheatEvent;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FurnaceGolemMob extends HostileMob {
   public static LootTable lootTable = new LootTable(LootItem.between("ironore", 1, 3), LootItem.between("copperore", 1, 3));
   public float overheatProgress = 0.0F;
   public float overheatGainRate = 0.005F;
   public float overheatDecayRate = 0.01F;
   private final int baseArmor = 1000;
   public GameDamage maxOverheatDamage = new GameDamage(100.0F);
   public int eventUniqueID;
   public FurnaceGolemOverheatEvent overheatEvent;

   public FurnaceGolemMob() {
      super(2500);
      this.setSpeed(25.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.1F);
      this.setArmor(1000);
      this.collision = new Rectangle(-20, -15, 40, 30);
      this.hitBox = new Rectangle(-25, -40, 50, 60);
      this.selectBox = new Rectangle(-35, -60, 70, 80);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -48;
      this.swimSinkOffset = -8;
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextInt(this.eventUniqueID);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      super.applyMovementPacket(reader, isDirect);
      this.eventUniqueID = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(null, 512, new GameDamage(100.0F), 100, 40000));
      if (this.isServer()) {
         this.overheatEvent = new FurnaceGolemOverheatEvent(this, GameRandom.globalRandom);
         this.getLevel().entityManager.events.add(this.overheatEvent);
         this.eventUniqueID = this.overheatEvent.getUniqueID();
      }
   }

   public GameDamage getOverheatDamage(Mob target) {
      return new GameDamage(
         this.maxOverheatDamage.damage * this.overheatProgress * target.buffManager.getModifier(BuffModifiers.FIRE_DAMAGE), 300.0F * this.overheatProgress
      );
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.overheatTick();
      if (this.overheatProgress != 0.0F) {
         GameRandom random = GameRandom.globalRandom;
         this.getLevel()
            .entityManager
            .addTopParticle(
               this.x + random.getFloatBetween(-10.0F, 10.0F), this.y + random.getFloatBetween(-10.0F, 10.0F) - 20.0F, Particle.GType.IMPORTANT_COSMETIC
            )
            .color(new Color(255, 150, 50))
            .ignoreLight(true)
            .givesLight(20.0F, 1.0F);

         for (int i = 0; i < Math.max(this.overheatProgress * 5.0F, 1.0F); i++) {
            this.getLevel()
               .entityManager
               .addParticle(this.x + random.getFloatBetween(-30.0F, 30.0F), this.y + random.getFloatBetween(-20.0F, 20.0F) - 40.0F, Particle.GType.COSMETIC)
               .sprite(GameResources.smokePuff.sprite(random.getIntBetween(0, 4), 0, 32))
               .color(new Color(205, 210, 218))
               .sizeFades((int)(16.0F * this.overheatProgress), (int)(32.0F * this.overheatProgress))
               .movesFriction(0.0F, -10.0F, 0.7F)
               .moves(
                  (pos, delta, lifeTime, timeAlive, lifePercent) -> {
                     pos.x = pos.x
                        + (random.nextBoolean() ? (float)(Math.sin(timeAlive / 50.0F) * delta) * 0.1F : (float)(Math.cos(timeAlive / 50.0F) * delta) * 0.1F);
                     pos.y -= (2.0F - lifePercent) * delta * 0.05F;
                  }
               )
               .fadesAlphaTime(100, 200)
               .lifeTime(600 + random.getIntBetween(0, 200));
         }
      }

      if (this.eventUniqueID != 0) {
         LevelEvent event = this.getLevel().entityManager.events.get(this.eventUniqueID, false);
         if (event == null) {
            this.getClient().network.sendPacket(new PacketRequestLevelEvent(this.eventUniqueID));
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.overheatTick();
      if (this.overheatEvent.isOver()) {
         this.eventUniqueID = 0;
         this.overheatEvent = null;
         this.sendMovementPacket(false);
      }
   }

   public void overheatTick() {
      if (this.inLiquid()) {
         this.updateOverheat(-this.overheatDecayRate * 2.0F);
      } else {
         this.updateOverheat(this.isInCombat() ? this.overheatGainRate : -this.overheatDecayRate);
      }

      this.setArmor((int)(1000.0F - 1000.0F * this.overheatProgress));
   }

   public void updateOverheat(float change) {
      this.overheatProgress = GameMath.limit(this.overheatProgress + change, 0.0F, 1.0F);
   }

   @Override
   public MobWasHitEvent isHit(MobWasHitEvent event, Attacker attacker) {
      this.updateOverheat(this.overheatGainRate * 5.0F);
      return super.isHit(event, attacker);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void playHitSound() {
      SoundManager.playSound(GameResources.cling, SoundEffect.effect(this).volume(1.6F).pitch(GameRandom.globalRandom.getFloatBetween(0.3F, 0.5F)));
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.crackdeath).volume(0.8F);
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      if (this.overheatEvent != null) {
         this.overheatEvent.over();
      }

      super.onDeath(attacker, attackers);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.furnaceGolem, i, 12, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("scrapyard", 5);
   }

   @Override
   public void addDrawables(
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
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 78;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions drawOptions = MobRegistry.Textures.furnaceGolem
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .addMaskShader(swimMask)
         .light(light)
         .pos(drawX, drawY);
      final DrawOptions lightDrawOptions = MobRegistry.Textures.furnaceGolem_light
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .addMaskShader(swimMask)
         .light(light.minLevelCopy(150.0F))
         .pos(drawX, drawY);
      final DrawOptions overheatDrawOptions = MobRegistry.Textures.furnaceGolem_overheat
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .addMaskShader(swimMask)
         .light(light.minLevelCopy(75.0F))
         .alpha(this.overheatProgress)
         .pos(drawX, drawY);
      final DrawOptions overheatLightDrawOptions = MobRegistry.Textures.furnaceGolem_overheat_light
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .addMaskShader(swimMask)
         .light(light.minLevelCopy(150.0F))
         .alpha(this.overheatProgress)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
            overheatDrawOptions.draw();
            lightDrawOptions.draw();
            overheatLightDrawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.furnaceGolem_shadow;
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 96 + 22;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(0, 0, 96).light(light).pos(drawX, drawY);
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      int frameCount = 4;
      int spriteIndex = GameUtils.getAnim(this.getTime(), frameCount, frameCount * 100);
      Point p = new Point(spriteIndex, dir);
      if (this.inLiquid(x, y)) {
         p.x = 8 + spriteIndex;
      } else if (Math.abs(this.dx) <= 0.01F && Math.abs(this.dy) <= 0.01F) {
         p.x = spriteIndex;
      } else {
         p.x = (int)(this.getDistanceRan() / this.getRockSpeed()) % 4 + 4;
      }

      return p;
   }

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }
}
