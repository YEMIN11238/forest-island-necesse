package necesse.entity.levelEvent;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.level.maps.LevelObject;

public class FlameTrapEvent extends LevelEvent implements Attacker {
   public static GameDamage DEFAULT_DAMAGE = new GameDamage(30.0F, 100.0F, 0.0F, 2.0F, 1.0F);
   private int tileX;
   private int tileY;
   private int dir;
   private int ticks;
   private int maxRange;
   private boolean[] lineHasHitSomething;
   private final HashSet<Integer> hitUniqueIDs = new HashSet<>();
   public GameDamage damage = DEFAULT_DAMAGE;

   public FlameTrapEvent() {
   }

   public FlameTrapEvent(int tileX, int tileY, int dir, int range, int perpendicularWidth, GameDamage damage) {
      this.tileX = tileX;
      this.tileY = tileY;
      this.dir = Math.abs(dir) % 4;
      this.lineHasHitSomething = new boolean[perpendicularWidth * 2 + 1];
      this.maxRange = range;
      this.damage = damage == null ? DEFAULT_DAMAGE : damage;
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.dir = reader.getNextByte();
      this.maxRange = reader.getNextShortUnsigned();
      int perpSize = reader.getNextShortUnsigned();
      this.lineHasHitSomething = new boolean[perpSize];
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextByte((byte)this.dir);
      writer.putNextShortUnsigned(this.maxRange);
      writer.putNextShortUnsigned(this.lineHasHitSomething.length);
   }

   @Override
   public void init() {
      super.init();
      this.ticks = 0;
   }

   @Override
   public void clientTick() {
      this.tickRange();
   }

   @Override
   public void serverTick() {
      this.tickRange();
   }

   public void tickRange() {
      int currentRange = this.ticks % this.maxRange;
      if (currentRange == 0) {
         this.lineHasHitSomething = new boolean[this.lineHasHitSomething.length];
         this.hitUniqueIDs.clear();
      }

      Point dir = this.getDir(this.dir);
      Point startPerpPoint = new Point(this.tileX - dir.y * this.lineHasHitSomething.length / 2, this.tileY - dir.x * this.lineHasHitSomething.length / 2);
      Point perpDir = new Point(dir.y, dir.x);

      for (int currentLine = 0; currentLine < this.lineHasHitSomething.length; currentLine++) {
         if (!this.lineHasHitSomething[currentLine]) {
            int currentTileX = startPerpPoint.x + dir.x * currentRange + perpDir.x * currentLine;
            int currentTileY = startPerpPoint.y + dir.y * currentRange + perpDir.y * currentLine;
            if (!this.level.getObject(currentTileX, currentTileY).isWall && !this.level.getObject(currentTileX, currentTileY).isRock) {
               if (!this.isServer()) {
                  for (int i = 0; i < 5; i++) {
                     Particle.GType gType = i <= 2 ? Particle.GType.CRITICAL : Particle.GType.COSMETIC;
                     this.level
                        .entityManager
                        .addParticle(
                           currentTileX * 32 + 16 + (int)(GameRandom.globalRandom.nextGaussian() * 8.0),
                           currentTileY * 32 + 16 + (int)(GameRandom.globalRandom.nextGaussian() * 8.0),
                           gType
                        )
                        .movesConstant(dir.x * GameRandom.globalRandom.nextFloat() * 10.0F, dir.y * GameRandom.globalRandom.nextFloat() * 10.0F)
                        .color(new Color(255, 186, 0))
                        .givesLight(0.0F, 0.5F)
                        .height(12.0F);
                  }
               }

               if (!this.isClient()) {
                  Rectangle hitBox = new Rectangle(currentTileX * 32, currentTileY * 32, 32, 32);
                  LevelObject lo = this.level.getLevelObject(currentTileX, currentTileY);
                  if (lo.object.attackThrough && lo.getAttackThroughCollisions().stream().anyMatch(hitBox::intersects)) {
                     lo.attackThrough(this.damage, this);
                  }

                  for (Mob target : this.getTargets(hitBox)) {
                     if (target.canBeHit(this) && hitBox.intersects(target.getHitBox())) {
                        this.hit(target);
                     }
                  }
               }
            } else {
               this.lineHasHitSomething[currentLine] = true;
            }
         }
      }

      this.ticks++;
      if (this.ticks >= this.maxRange * 2) {
         this.over();
      }
   }

   private void hit(Mob target) {
      if (!this.hitUniqueIDs.contains(target.getHitCooldownUniqueID())) {
         if (target.getLevel().isTrialRoom) {
            GameDamage trialDamage = new GameDamage(DamageTypeRegistry.TRUE, target.getMaxHealth() / 4.0F);
            target.isServerHit(trialDamage, 0.0F, 0.0F, 0.0F, this);
            this.hitUniqueIDs.add(target.getHitCooldownUniqueID());
         } else {
            target.isServerHit(this.damage, 0.0F, 0.0F, 0.0F, this);
            ActiveBuff ab = new ActiveBuff("onfire", target, 10.0F, this);
            target.addBuff(ab, true);
            this.hitUniqueIDs.add(target.getHitCooldownUniqueID());
         }
      }
   }

   private ArrayList<Mob> getTargets(Rectangle hitbox) {
      ArrayList<Mob> out = this.level
         .entityManager
         .mobs
         .getInRegionRangeByTile(GameMath.getTileCoordinate(hitbox.getCenterX()), GameMath.getTileCoordinate(hitbox.getCenterY()), 1);
      if (this.level.isServer()) {
         this.level
            .getServer()
            .streamClients()
            .filter(sc -> sc != null && sc.playerMob != null)
            .filter(sc -> sc.isSamePlace(this.getLevel()))
            .forEach(sc -> out.add(sc.playerMob));
      }

      return out;
   }

   @Override
   public GameMessage getAttackerName() {
      return new LocalMessage("deaths", "flametrapname");
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("flametrap", 2);
   }

   @Override
   public Mob getFirstAttackOwner() {
      return null;
   }

   @Override
   public boolean isTrapAttacker() {
      return true;
   }

   public Point getDir(int dir) {
      if (dir == 0) {
         return new Point(0, -1);
      } else if (dir == 1) {
         return new Point(1, 0);
      } else if (dir == 2) {
         return new Point(0, 1);
      } else {
         return dir == 3 ? new Point(-1, 0) : new Point(0, 0);
      }
   }

   @Override
   public Point getSaveToRegionPos() {
      return new Point(this.level.regionManager.getRegionCoordByTile(this.tileX), this.level.regionManager.getRegionCoordByTile(this.tileY));
   }
}
