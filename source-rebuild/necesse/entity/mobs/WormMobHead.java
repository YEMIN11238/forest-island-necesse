package necesse.entity.mobs;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.ComputedObjectValue;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ability.EmptyMobAbility;
import necesse.entity.mobs.ability.FloatPosDirMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.leaves.TargetFinderAINode;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.RegionManager;

public abstract class WormMobHead<T extends WormMobBody<B, T>, B extends WormMobHead<T, B>> extends Mob {
   public ArrayList<LevelMob<T>> bodyParts = new ArrayList<>();
   public GameLinkedList<WormMoveLine> moveLines = new GameLinkedList<>();
   public float height;
   public float moveAngle;
   protected float moveAngleAccuracy = 2.0F;
   public float distanceMoved;
   public boolean dive;
   public boolean isUnderground;
   public float soundCounter;
   public final EmptyMobAbility diveAbility;
   public final FloatPosDirMobAbility appearAbility;
   public float waveLength;
   public float distPerMoveSound;
   public final int totalBodyParts;
   public float heightMultiplier;
   public float heightOffset;
   public int removeWhenTilesOutOfLevel = 0;
   protected int checkRemoveTicker;

   public WormMobHead(int health, float waveLength, float distPerMoveSound, int totalBodyParts, float heightMultiplier, float heightOffset) {
      super(health);
      this.waveLength = waveLength;
      this.distPerMoveSound = distPerMoveSound;
      this.totalBodyParts = totalBodyParts;
      this.heightMultiplier = heightMultiplier;
      this.heightOffset = heightOffset;
      this.moveAccuracy = 160;
      this.setSpeed(50.0F);
      this.setSwimSpeed(1.0F);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.accelerationMod = 1.0F;
      this.decelerationMod = 1.0F;
      this.diveAbility = this.registerAbility(new EmptyMobAbility() {
         @Override
         protected void run() {
            WormMobHead.this.dive = true;
         }
      });
      this.appearAbility = this.registerAbility(new FloatPosDirMobAbility() {
         @Override
         protected void run(float x, float y, float dx, float dy) {
            WormMobHead.this.distanceMoved = 0.0F;
            WormMobHead.this.isUnderground = false;
            WormMobHead.this.dive = false;
            float speed = (float)new Float(WormMobHead.this.dx, WormMobHead.this.dy).distance(0.0, 0.0);
            Float dir = GameMath.normalize(dx, dy);
            WormMobHead.this.moveAngle = GameMath.getAngle(dir);
            WormMobHead.this.dx = dir.x * speed;
            WormMobHead.this.dy = dir.y * speed;
            WormMobHead.this.setPos(x, y, true);
            WormMobHead.this.onAppearAbility();
         }
      });
   }

   protected void onAppearAbility() {
   }

   protected abstract float getDistToBodyPart(T var1, int var2, float var3);

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextShortUnsigned(this.bodyParts.size());

      for (LevelMob<T> bodyPart : this.bodyParts) {
         writer.putNextInt(bodyPart.uniqueID);
      }

      ArrayList<Runnable> moveLineWriters = new ArrayList<>(this.bodyParts.size());

      for (LevelMob<T> bodyPart : this.bodyParts) {
         T next = bodyPart.get(this.getLevel());
         if (next != null && next.moveLine != null) {
            WormMoveLine moveLine = (WormMoveLine)next.moveLine.object;
            moveLineWriters.add(() -> moveLine.writeSpawnPacket(writer, next.x, next.y, next.moveLineExtraDist));
         }
      }

      writer.putNextFloat(this.distanceMoved);
      writer.putNextShortUnsigned(moveLineWriters.size());

      for (Runnable moveLineWriter : moveLineWriters) {
         moveLineWriter.run();
      }
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);

      for (LevelMob<T> bodyPart : this.bodyParts) {
         T last = bodyPart.get(this.getLevel());
         if (last != null) {
            last.remove();
         }
      }

      this.bodyParts.clear();
      int bodyPartsCount = reader.getNextShortUnsigned();

      for (int i = 0; i < bodyPartsCount; i++) {
         this.bodyParts.add(new LevelMob<>(reader.getNextInt()));
      }

      this.distanceMoved = reader.getNextFloat();
      this.moveLines.clear();
      int points = reader.getNextShortUnsigned();
      WormMoveLineSpawnData lineSpawnData = new WormMoveLineSpawnData(this.x, this.y);

      for (int i = 0; i < points; i++) {
         this.moveLines.addLast(this.readMoveLine(reader, lineSpawnData));
      }
   }

   @Override
   public void setupMovementPacket(PacketWriter writer) {
      super.setupMovementPacket(writer);
      writer.putNextFloat(this.moveAngle);
      writer.putNextShortUnsigned(this.moveAccuracy);
   }

   @Override
   public void applyMovementPacket(PacketReader reader, boolean isDirect) {
      Float lastPos = new Float(this.getDrawX(), this.getDrawY());
      super.applyMovementPacket(reader, isDirect);
      this.moveAngle = reader.getNextFloat();
      this.moveAccuracy = reader.getNextShortUnsigned();
      Float newPos = new Float(this.getDrawX(), this.getDrawY());
      if (isDirect) {
         newPos = new Float(this.x, this.y);
         lastPos = new Float(this.x, this.y);
      }

      WormMoveLine moveLine = this.newMoveLine(lastPos, newPos, isDirect, this.distanceMoved, this.isUnderground);
      if (moveLine.dist() > 0.0) {
         this.moveLines.addFirst(moveLine);
         this.updateBodyParts();
      }
   }

   protected abstract T createNewBodyPart(int var1);

   protected void modifyBodyPart(int index, T bodyPart) {
      bodyPart.collisionHitCooldowns = this.collisionHitCooldowns;
   }

   @Override
   public void init() {
      super.init();
      if (this.bodyParts.size() < this.totalBodyParts) {
         GameRandom uniqueIDRandom = new GameRandom();
         int start = this.bodyParts.size();
         int lastUniqueID = this.bodyParts.isEmpty() ? this.getUniqueID() : this.bodyParts.get(this.bodyParts.size() - 1).uniqueID;

         for (int i = 0; i < this.totalBodyParts - start; i++) {
            uniqueIDRandom.setSeed(lastUniqueID);
            int newUniqueID = getNewUniqueID(this.getLevel(), uniqueIDRandom);
            this.bodyParts.add(new LevelMob<>(newUniqueID));
            lastUniqueID = newUniqueID;
         }
      }

      T lastBodyPart = null;

      for (int i = 0; i < this.bodyParts.size(); i++) {
         LevelMob<T> levelMob = this.bodyParts.get(i);
         T bodyPart = this.createNewBodyPart(i);
         bodyPart.next = lastBodyPart;
         lastBodyPart = bodyPart;
         bodyPart.setLevel(this.getLevel());
         bodyPart.setUniqueID(levelMob.uniqueID);
         bodyPart.setPos(this.x, this.y, true);
         bodyPart.setMaxHealth(this.getMaxHealth());
         bodyPart.setHealthHidden(this.getHealth());
         bodyPart.master.uniqueID = this.getUniqueID();
         this.modifyBodyPart(i, bodyPart);
         this.getLevel().entityManager.mobs.addHidden(bodyPart);
         this.bodyParts.set(i, new LevelMob<>(bodyPart));
      }

      this.updateBodyParts();
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return this.isVisible() && super.canCollisionHit(target);
   }

   @Override
   public boolean isVisible() {
      return !this.isUnderground;
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return !this.isVisible() ? false : super.canBeHit(attacker);
   }

   @Override
   public void clientTick() {
      super.clientTick();

      for (LevelMob<T> bodyPart : this.bodyParts) {
         bodyPart.computeIfPresent(this.getLevel(), bp -> bp.removeTicker = 0);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.removeWhenTilesOutOfLevel > 0) {
         if (!this.getLevel().isTileWithinBounds(this.getTileX(), this.getTileY(), -this.removeWhenTilesOutOfLevel)) {
            this.remove();
         }

         Point regionPos = this.regionTracker.getSaveToRegionPos();
         if (regionPos == null) {
            this.checkRemoveTicker++;
            if (this.checkRemoveTicker >= 200) {
               this.checkRemoveTicker = 0;
               RegionManager manager = this.getLevel().regionManager;
               int startRegionX = manager.getRegionCoordByTile(this.getTileX() - this.removeWhenTilesOutOfLevel);
               int startRegionY = manager.getRegionCoordByTile(this.getTileY() - this.removeWhenTilesOutOfLevel);
               int endRegionX = manager.getRegionCoordByTile(this.getTileX() + this.removeWhenTilesOutOfLevel);
               int endRegionY = manager.getRegionCoordByTile(this.getTileY() + this.removeWhenTilesOutOfLevel);
               startRegionX = manager.limitRegionXToBounds(startRegionX);
               startRegionY = manager.limitRegionYToBounds(startRegionY);
               endRegionX = manager.limitRegionXToBounds(endRegionX);
               endRegionY = manager.limitRegionYToBounds(endRegionY);
               boolean foundLoaded = false;

               for (int regionX = startRegionX; regionX <= endRegionX; regionX++) {
                  for (int regionY = startRegionY; regionY <= endRegionY; regionY++) {
                     if (manager.isRegionLoaded(regionX, regionY)) {
                        foundLoaded = true;
                        break;
                     }
                  }

                  if (foundLoaded) {
                     break;
                  }
               }

               if (!foundLoaded) {
                  this.remove();
               }
            }
         } else {
            this.checkRemoveTicker = 0;
         }
      }

      for (LevelMob<T> bodyPart : this.bodyParts) {
         bodyPart.computeIfPresent(this.getLevel(), bp -> bp.removeTicker = 0);
      }
   }

   @Override
   public void remove(float knockbackX, float knockbackY, Attacker attacker, boolean isDeath) {
      super.remove(knockbackX, knockbackY, attacker, isDeath);

      for (LevelMob<T> bodyPart : this.bodyParts) {
         bodyPart.computeIfPresent(this.getLevel(), bp -> bp.remove(knockbackX, knockbackY, attacker, isDeath));
      }
   }

   public Stream<T> streamBodyParts() {
      return this.bodyParts.stream().map(m -> m.get(this.getLevel())).filter(Objects::nonNull);
   }

   @Override
   public Point getLootDropsPosition(ServerClient privateClient) {
      ArrayList<T> mostParts = this.getLargestBodyPartSection(
         bp -> bp.isVisible() && !this.getLevel().isSolidTile(bp.getTileX(), bp.getTileY()) && !this.getLevel().isLiquidTile(bp.getTileX(), bp.getTileY()),
         true
      );
      if (!mostParts.isEmpty()) {
         T mid = mostParts.get(mostParts.size() / 2);
         return new Point(mid.getX(), mid.getY());
      } else {
         return super.getLootDropsPosition(privateClient);
      }
   }

   @Override
   public int stoppingDistance(float friction, float currentSpeed) {
      return 0;
   }

   @Override
   public Rectangle getSelectBox(int x, int y) {
      Rectangle selectBox = super.getSelectBox(x, y);
      selectBox.y = (int)(selectBox.y - this.height);
      if (this.height < 0.0F) {
         selectBox.height = (int)(selectBox.height + this.height);
      }

      return selectBox;
   }

   public ArrayList<T> getLargestBodyPartSection(Predicate<T> filter, boolean searchCached) {
      ArrayList<T> mostParts = new ArrayList<>();
      ArrayList<T> currentParts = new ArrayList<>();

      for (LevelMob<T> bodyPart : this.bodyParts) {
         T bp;
         if (searchCached) {
            Mob foundMob = this.getLevel().entityManager.mobs.get(bodyPart.uniqueID, true);

            try {
               bp = (T)foundMob;
            } catch (ClassCastException e) {
               bp = null;
            }
         } else {
            bp = bodyPart.get(this.getLevel());
         }

         if (bp != null && filter.test(bp)) {
            currentParts.add(bp);
         } else if (mostParts.size() < currentParts.size()) {
            mostParts = currentParts;
            currentParts = new ArrayList<>();
         }
      }

      if (mostParts.size() < currentParts.size()) {
         mostParts = currentParts;
      }

      return mostParts;
   }

   @Override
   public boolean isHealthBarVisible() {
      return Settings.showMobHealthBars && this.getHealthUnlimited() < this.getMaxHealth() && (!this.isBoss() || !Settings.showBossHealthBars);
   }

   @Override
   public Rectangle getHealthBarBounds(int x, int y) {
      ArrayList<T> parts = this.getLargestBodyPartSection(WormMobBody::isVisible, false);
      if (parts.isEmpty()) {
         return null;
      }

      GameLinkedList<WormMoveLine>.Element baseLine = parts.get(parts.size() / 2).moveLine;
      double totalLinesDist = 0.0;
      GameLinkedList<WormMoveLine> lines = new GameLinkedList<>();
      lines.add((WormMoveLine)baseLine.object);
      totalLinesDist += ((WormMoveLine)baseLine.object).dist();

      for (GameLinkedList<WormMoveLine>.Element current = baseLine; current.hasPrev(); totalLinesDist += ((WormMoveLine)current.object).dist()) {
         current = current.prev();
         if (current.object == null || ((WormMoveLine)current.object).isUnderground || ((WormMoveLine)current.object).isMoveJump) {
            break;
         }

         lines.addLast((WormMoveLine)current.object);
      }

      for (GameLinkedList<WormMoveLine>.Element var13 = baseLine; var13.hasNext(); totalLinesDist += ((WormMoveLine)var13.object).dist()) {
         var13 = var13.next();
         if (var13.object == null || ((WormMoveLine)var13.object).isUnderground || ((WormMoveLine)var13.object).isMoveJump) {
            break;
         }

         lines.addFirst((WormMoveLine)var13.object);
      }

      ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> cv = moveDistance(lines.getFirstElement(), totalLinesDist / 2.0);
      java.awt.geom.Point2D.Double pos = linePos(cv.object, cv.get());
      int width = 64;
      return new Rectangle((int)pos.x - width / 2, (int)pos.y - 30, width, 7);
   }

   @Override
   public void tickMovement(float delta) {
      Float lastPos = new Float(this.getDrawX(), this.getDrawY());
      super.tickMovement(delta);
      Float newPos = new Float(this.getDrawX(), this.getDrawY());
      WormMoveLine moveLine = this.newMoveLine(lastPos, newPos, false, this.distanceMoved, this.isUnderground);
      if (moveLine.dist() > 0.0) {
         if (this.dive && this.runsPastMinHeight(this.distanceMoved, (float)moveLine.dist())) {
            moveLine.isUnderground = true;
            this.isUnderground = true;
         }

         this.distanceMoved = (float)(this.distanceMoved + moveLine.dist());
         if (this.isClient()) {
            this.soundCounter = (float)(this.soundCounter + moveLine.dist());
            if (this.soundCounter >= this.distPerMoveSound) {
               this.playMoveSound();
               this.soundCounter = this.soundCounter - this.distPerMoveSound;
            }
         }

         this.moveLines.addFirst(moveLine);
         this.updateBodyParts();
      }
   }

   public WormMoveLine newMoveLine(Point2D lastPos, Point2D newPos, boolean isMoveJump, float movedDist, boolean isUnderground) {
      return new WormMoveLine(lastPos, newPos, isMoveJump, movedDist, isUnderground);
   }

   public WormMoveLine readMoveLine(PacketReader reader, WormMoveLineSpawnData data) {
      return new WormMoveLine(reader, data);
   }

   protected abstract void playMoveSound();

   @Override
   public void tickCurrentMovement(float delta) {
      super.tickCurrentMovement(delta);
      if (this.moveX != 0.0F || this.moveY != 0.0F) {
         float targetAngle = GameMath.getAngle(new Float(this.moveX, this.moveY));
         float turnSpeed = this.getTurnSpeed(delta);
         float dif = GameMath.getAngleDifference(targetAngle, this.moveAngle);
         if (Math.abs(dif) > this.moveAngleAccuracy) {
            if (Math.abs(dif) - turnSpeed < 1.0F) {
               this.moveAngle = GameMath.limit(this.moveAngle + turnSpeed * Math.signum(dif), targetAngle - 1.0F, targetAngle + 1.0F);
            } else {
               this.moveAngle = this.moveAngle + turnSpeed * Math.signum(dif);
            }
         }
      }

      Float newMove = GameMath.getAngleDir(this.moveAngle);
      this.moveX = newMove.x;
      this.moveY = newMove.y;
   }

   public float getTurnSpeed(float delta) {
      float speedMod = (float)Math.pow(0.1F, this.getCurrentSpeed() / this.getSpeed());
      return (40.0F + 100.0F * speedMod) * delta / 250.0F;
   }

   public float getWaveHeight(float length) {
      return (float)Math.sin(Math.toRadians(length / this.waveLength * 360.0F - 90.0F)) * this.heightMultiplier + this.heightOffset;
   }

   public float getDistAtHeight(float height) {
      float value = GameMath.limit((height - this.heightOffset) / this.heightMultiplier, -1.0F, 1.0F);
      return (float)((Math.toDegrees(Math.asin(value)) + 90.0) * this.waveLength) / 360.0F;
   }

   public boolean runsPastMinHeight(float length, float lengthAdd) {
      float waveProgress = (length + this.waveLength / 2.0F) % this.waveLength / this.waveLength;
      float addProgress = lengthAdd / this.waveLength;
      return addProgress > 1.0F || waveProgress < 0.5F && waveProgress + addProgress >= 0.5F;
   }

   public void updateBodyParts() {
      float length = -this.distanceMoved;
      this.height = this.getWaveHeight(length);
      if (this.moveLines.isEmpty()) {
         for (LevelMob<T> bodyPart : this.bodyParts) {
            T next = bodyPart.get(this.getLevel());
            if (next != null) {
               next.moveLine = null;
            }
         }
      } else {
         ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> currentLine = new ComputedObjectValue<>(this.moveLines.getFirstElement(), () -> 0.0);
         float lastDistance = 0.0F;

         for (int i = 0; i < this.bodyParts.size(); i++) {
            T next = this.bodyParts.get(i).get(this.getLevel());
            if (currentLine.object != null) {
               float distToBodyPart = this.getDistToBodyPart(next, i, lastDistance);
               lastDistance += distToBodyPart;
               double nextDist = distToBodyPart + currentLine.get();
               currentLine = moveDistance(currentLine.object, nextDist);
               if (currentLine.object != null) {
                  if (next != null) {
                     next.moveLine = currentLine.object;
                     next.moveLineExtraDist = currentLine.get().floatValue();
                     next.height = this.getWaveHeight(((WormMoveLine)next.moveLine.object).movedDist + next.moveLineExtraDist);
                     java.awt.geom.Point2D.Double pos = linePos(currentLine);
                     next.updateBodyPartPosition((B)this, (float)pos.x, (float)pos.y);
                     next.distanceRan = this.distanceRan;
                     this.onUpdatedBodyPartPos(next, i, distToBodyPart);
                  }
                  continue;
               }
            }

            next.moveLine = null;
            next.moveLineExtraDist = 0.0F;
            WormMoveLine lastLine = this.moveLines.getLast();
            next.updateBodyPartPosition((B)this, lastLine.x2, lastLine.y2);
            next.distanceRan = this.distanceRan;
            currentLine = new ComputedObjectValue<>(null, () -> 0.0);
         }

         if (currentLine.object != null) {
            this.removeRemaining(currentLine.object.next());
         }
      }
   }

   protected void onUpdatedBodyPartPos(T bodyPart, int index, float distToBodyPart) {
   }

   public static ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> moveDistance(GameLinkedList<WormMoveLine>.Element line, double distance) {
      if (line != null && !line.isRemoved()) {
         while (!(Math.abs(distance) <= ((WormMoveLine)line.object).dist())) {
            if (distance < 0.0) {
               distance += ((WormMoveLine)line.object).dist();
               line = line.prev();
            } else {
               distance -= ((WormMoveLine)line.object).dist();
               line = line.next();
            }

            if (line == null) {
               double fDistance = distance;
               return new ComputedObjectValue<>(null, () -> fDistance);
            }
         }

         double fDistance = distance;
         return new ComputedObjectValue<>(line, () -> fDistance);
      } else {
         double fDistance = distance;
         return new ComputedObjectValue<>(null, () -> fDistance);
      }
   }

   public static java.awt.geom.Point2D.Double linePos(GameLinkedList<WormMoveLine>.Element line, double distance) {
      WormMoveLine moveLine = (WormMoveLine)line.object;
      Float dir = moveLine.dir();
      return distance < 0.0
         ? new java.awt.geom.Point2D.Double(moveLine.x1 - dir.x * distance, moveLine.y1 - dir.y * distance)
         : new java.awt.geom.Point2D.Double(moveLine.x1 + dir.x * distance, moveLine.y1 + dir.y * distance);
   }

   public static java.awt.geom.Point2D.Double linePos(ComputedObjectValue<GameLinkedList<WormMoveLine>.Element, Double> line) {
      Double dist = line.get();
      return dist < 0.0 ? linePos(line.object, dist) : linePos(line.object, ((WormMoveLine)line.object.object).dist() - dist);
   }

   private void removeRemaining(GameLinkedList<?>.Element current) {
      while (current != null) {
         GameLinkedList<?>.Element last = current;
         current = current.next();
         last.remove();
      }
   }

   @Override
   public boolean canHitThroughCollision() {
      return true;
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return null;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   public static MobDrawable getAngledDrawable(
      Mob mob, GameSprite sprite, GameTexture maskTexture, GameLight light, int height, float angle, int drawX, int drawY, int yOffset, PlayerMob perspective
   ) {
      drawY -= height;
      drawY -= yOffset;
      Float dir = GameMath.getAngleDir(angle);
      int rotates;
      int angleOffset;
      if (Math.abs(dir.y) - Math.abs(dir.x) <= 0.0F) {
         rotates = dir.x < 0.0F ? -1 : 1;
         angleOffset = dir.x < 0.0F ? 180 : 0;
      } else {
         rotates = dir.y < 0.0F ? 0 : 2;
         angleOffset = dir.y < 0.0F ? 90 : 270;
      }

      TextureDrawOptionsEnd textureOptions = sprite.initDraw()
         .rotate(angle + angleOffset, sprite.spriteWidth / 2, sprite.spriteHeight / 2)
         .rotateTexture(rotates)
         .startGlowOptions(mob, mob.getID())
         .light(light)
         .applyEnemyTracker(mob, perspective);
      if (maskTexture != null) {
         textureOptions.addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, 0, -height - yOffset));
      }

      final DrawOptions drawOptions = textureOptions.pos(drawX, drawY);
      return new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      };
   }

   public static void addAngledDrawable(
      List<MobDrawable> list,
      Mob mob,
      GameSprite sprite,
      GameTexture maskTexture,
      GameLight light,
      int height,
      float angle,
      int drawX,
      int drawY,
      int yOffset,
      PlayerMob perspective
   ) {
      list.add(getAngledDrawable(mob, sprite, maskTexture, light, height, angle, drawX, drawY, yOffset, perspective));
   }

   public static int getDirSprite(float angle) {
      Float dir = GameMath.getAngleDir(angle);
      if (Math.abs(dir.y) - Math.abs(dir.x) <= 0.0F) {
         return dir.x < 0.0F ? 3 : 1;
      } else {
         return dir.y < 0.0F ? 0 : 2;
      }
   }

   public static void addDrawable(
      List<MobDrawable> list,
      Mob mob,
      GameSprite sprite,
      GameTexture maskTexture,
      GameLight light,
      int height,
      int drawX,
      int drawY,
      int yOffset,
      PlayerMob perspective
   ) {
      list.add(getDrawable(mob, sprite, maskTexture, light, height, drawX, drawY, yOffset, perspective));
   }

   public static MobDrawable getDrawable(
      Mob mob, GameSprite sprite, GameTexture maskTexture, GameLight light, int height, int drawX, int drawY, int yOffset, PlayerMob perspective
   ) {
      drawY -= height;
      drawY -= yOffset;
      TextureDrawOptionsEnd textureOptions = sprite.initDraw().startGlowOptions(mob, mob.getID()).light(light).applyEnemyTracker(mob, perspective);
      if (maskTexture != null) {
         textureOptions.addMaskShader(new MaskShaderOptions(maskTexture, 0, 0, 0, -height - yOffset));
      }

      final DrawOptions drawOptions = textureOptions.pos(drawX, drawY);
      return new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      };
   }

   protected <C extends WormMobHead<T, B>> WormMobHead<T, B>.BodyPartTarget getRandomTargetFromBodyPart(
      AINode<C> node, TargetFinderAINode<C> targetFinder, BiFunction<Mob, T, Boolean> filter
   ) {
      Point base = new Point(this.getX(), this.getY());
      int bodyPartsCount = this.bodyParts.size() - 5;
      ArrayList<T> bodyParts = new ArrayList<>(bodyPartsCount);

      for (int i = 0; i < bodyPartsCount; i++) {
         T bp = this.bodyParts.get(i).get(this.getLevel());
         if (bp != null && bp.isVisible()) {
            bodyParts.add(bp);
         }
      }

      if (bodyParts.isEmpty()) {
         return null;
      }

      List<Mob> targets = targetFinder.streamPossibleTargets((C)this, base, targetFinder.distance).collect(Collectors.toList());
      Collections.shuffle(targets);
      return targets.stream().filter(m -> targetFinder.validity.isValidTarget(node, (C)this, m, true)).map(m -> {
         Collections.shuffle(bodyParts);
         T bodyPart = bodyParts.stream().filter(bpx -> filter.apply(m, (T)bpx)).findAny().orElse(null);
         return bodyPart != null ? new WormMobHead.BodyPartTarget(bodyPart, m) : null;
      }).filter(Objects::nonNull).findAny().orElse(null);
   }

   protected class BodyPartTarget {
      public final T bodyPart;
      public final Mob target;

      public BodyPartTarget(T bodyPart, Mob target) {
         this.bodyPart = bodyPart;
         this.target = target;
      }
   }
}
