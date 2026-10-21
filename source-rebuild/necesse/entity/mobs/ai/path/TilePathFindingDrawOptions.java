package necesse.entity.mobs.ai.path;

import java.awt.Point;
import java.util.LinkedList;
import java.util.stream.Stream;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.pathfinding.PathResult;
import necesse.engine.util.pathfinding.Pathfinding;
import necesse.entity.Entity;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class TilePathFindingDrawOptions implements DrawOptions {
   private Point mouseTile;
   private GameCamera camera;
   private Pathfinding<Point>.Node mouseNode;
   private TilePathfinding finder;
   private DrawOptionsList drawOptions = new DrawOptionsList();
   private SharedTextureDrawOptions quadDrawOptions = new SharedTextureDrawOptions(Renderer.getQuadTexture());

   public TilePathFindingDrawOptions(PathResult<Point, TilePathfinding> result, GameCamera camera) {
      this.finder = result.finder;
      this.camera = camera;
      this.drawOptions.add(this.quadDrawOptions::draw);
      double maxCost = Stream.concat(result.openNodes.stream(), result.closedNodes.stream()).map(t -> t.pathCost).max(Double::compareTo).orElse(1.0);
      int index = 0;

      for (Pathfinding<Point>.Node t : result.closedNodes) {
         float floatCost = (float)(t.pathCost / maxCost);
         this.addNodeDrawOptions(t, result, index, floatCost, 0.0F, 0.0F, 0.5F, camera, this.finder.moveOffsetX, this.finder.moveOffsetY);
         index++;
      }

      index = 0;

      for (Pathfinding<Point>.Node t : result.openNodes) {
         float floatCost = (float)(t.pathCost / maxCost);
         this.addNodeDrawOptions(t, result, index, floatCost, 1.0F, 0.0F, 0.5F, camera, this.finder.moveOffsetX, this.finder.moveOffsetY);
         index++;
      }

      for (Point t : result.invalidChecked) {
         int tileDrawX = camera.getTileDrawX(t.x);
         int tileDrawY = camera.getTileDrawY(t.y);
         this.quadDrawOptions.addFull().size(32, 32).color(0.0F, 1.0F, 1.0F, 0.25F).pos(tileDrawX, tileDrawY);
      }

      this.drawOptions.add(TilePathfinding.getPathLineDrawOptions(result.path, camera));
      int tileDrawX = camera.getTileDrawX(result.target.x);
      int tileDrawY = camera.getTileDrawY(result.target.y);
      this.quadDrawOptions.addFull().size(32, 32).color(0.0F, 0.0F, 0.5F).pos(tileDrawX, tileDrawY);
      Pathfinding<Point>.Node last = result.getLastPathNode();
      if (last != null) {
         this.addNodeDrawOptions(last, result, 0, 0.0F, 0.0F, 1.0F, 0.5F, camera, this.finder.moveOffsetX, this.finder.moveOffsetY);
      }

      this.drawOptions
         .add(
            () -> GameTooltipManager.addTooltip(
               new StringTooltips("Path iterations: " + result.iterations)
                  .add("Node priority: " + result.finder.options.nodePriority)
                  .add("Time taken: " + GameUtils.getTimeStringNano(result.nsTaken))
                  .add("Found target: " + result.foundTarget),
               TooltipLocation.PLAYER
            )
         );
   }

   public TilePathFindingDrawOptions(Pathfinding<Point>.Process<TilePathfinding> process, GameCamera camera) {
      this.finder = process.finder;
      this.camera = camera;
      synchronized (process.nodeLock) {
         double maxCost = Stream.concat(process.streamOpenNodes(), process.streamClosedNodes()).map(t -> t.pathCost).max(Double::compareTo).orElse(1.0);
         int index = 0;

         for (Pathfinding<Point>.Node t : process.getClosedNodes()) {
            float floatCost = (float)(t.pathCost / maxCost);
            this.addNodeDrawOptions(t, process, index, floatCost, 0.0F, 0.0F, 0.5F, camera, this.finder.moveOffsetX, this.finder.moveOffsetY);
            index++;
         }

         index = 0;

         for (Pathfinding<Point>.Node t : process.getOpenNodes()) {
            float floatCost = (float)(t.pathCost / maxCost);
            this.addNodeDrawOptions(t, process, index, floatCost, 1.0F, 0.0F, 0.5F, camera, this.finder.moveOffsetX, this.finder.moveOffsetY);
            index++;
         }

         for (Point t : process.getInvalidChecked()) {
            int tileDrawX = camera.getTileDrawX(t.x);
            int tileDrawY = camera.getTileDrawY(t.y);
            this.quadDrawOptions.addFull().size(32, 32).color(0.0F, 1.0F, 1.0F, 0.25F).pos(tileDrawX, tileDrawY);
         }

         int tileDrawX = camera.getTileDrawX(((Point)process.target).x);
         int tileDrawY = camera.getTileDrawY(((Point)process.target).y);
         this.quadDrawOptions.addFull().size(32, 32).color(0.0F, 0.0F, 0.5F).pos(tileDrawX, tileDrawY);
         this.addTileDrawOptions((Point)process.target, 0, 0.0F, 0.0F, 1.0F, 0.5F, camera);
         this.drawOptions
            .add(
               () -> GameTooltipManager.addTooltip(
                  new StringTooltips("Path iterations: " + process.getIterations())
                     .add("Node priority: " + this.finder.options.nodePriority)
                     .add("Time taken: " + GameUtils.getTimeStringNano(process.getTime())),
                  TooltipLocation.PLAYER
               )
            );
      }
   }

   @Override
   public void draw() {
      this.mouseTile = new Point(this.camera.getMouseLevelTilePosX(), this.camera.getMouseLevelTilePosY());
      this.quadDrawOptions.draw();
      this.drawOptions.draw();
      if (this.mouseNode != null) {
         LinkedList<Pathfinding<Point>.Node> path = this.finder.constructPath(this.mouseNode);
         FinalPath finalPath = new FinalPath(TilePathfinding.reducePathPoints(this.finder, path));
         finalPath.drawPath(null, this.camera);
         GameTooltipManager.addTooltip(
            new StringTooltips("Path length: " + finalPath.getFullLength())
               .add("Time for speed 35: " + Entity.getTravelTimeMillis(35.0F, finalPath.getCurrentLength())),
            TooltipLocation.PLAYER
         );
      }
   }

   public boolean isMouseOverTile(int tileX, int tileY) {
      return this.mouseTile != null && this.mouseTile.x == tileX && this.mouseTile.y == tileY;
   }

   private void addNodeDrawOptions(
      Pathfinding<Point>.Node node,
      Pathfinding<Point>.Process<TilePathfinding> process,
      int index,
      float r,
      float g,
      float b,
      float alpha,
      GameCamera camera,
      int moveOffsetX,
      int moveOffsetY
   ) {
      this.addNodeDrawOptions(node, process.finder, index, r, g, b, alpha, camera, moveOffsetX, moveOffsetY);
   }

   private void addNodeDrawOptions(
      Pathfinding<Point>.Node node,
      PathResult<Point, TilePathfinding> result,
      int index,
      float r,
      float g,
      float b,
      float alpha,
      GameCamera camera,
      int moveOffsetX,
      int moveOffsetY
   ) {
      this.addNodeDrawOptions(node, result.finder, index, r, g, b, alpha, camera, moveOffsetX, moveOffsetY);
   }

   private void addNodeDrawOptions(
      Pathfinding<Point>.Node node,
      TilePathfinding finder,
      int index,
      float r,
      float g,
      float b,
      float alpha,
      GameCamera camera,
      int moveOffsetX,
      int moveOffsetY
   ) {
      this.addNodeDrawOptions(node, finder.options, index, r, g, b, alpha, camera, moveOffsetX, moveOffsetY);
   }

   private void addNodeDrawOptions(
      Pathfinding<Point>.Node node,
      PathOptions pathOptions,
      int index,
      float r,
      float g,
      float b,
      float alpha,
      GameCamera camera,
      int moveOffsetX,
      int moveOffsetY
   ) {
      this.addNodeDrawOptions(node, pathOptions.nodePriority, index, r, g, b, alpha, camera, moveOffsetX, moveOffsetY);
   }

   private void addNodeDrawOptions(
      Pathfinding<Point>.Node node,
      TilePathfinding.NodePriority nodePriority,
      int index,
      float r,
      float g,
      float b,
      float alpha,
      GameCamera camera,
      int moveOffsetX,
      int moveOffsetY
   ) {
      int tileDrawX = camera.getTileDrawX(((Point)node.item).x);
      int tileDrawY = camera.getTileDrawY(((Point)node.item).y);
      if (tileDrawX > -32 && tileDrawY > -32 && tileDrawX < camera.getWidth() && tileDrawY < camera.getHeight()) {
         this.quadDrawOptions.addFull().size(32, 32).color(r, g, b, alpha).pos(tileDrawX, tileDrawY);
         if (node.cameFrom != null) {
            int fromTileDrawX = camera.getTileDrawX(((Point)node.cameFrom.item).x);
            int fromTileDrawY = camera.getTileDrawY(((Point)node.cameFrom.item).y);
            this.drawOptions
               .add(
                  () -> Renderer.drawLineRGBA(
                     tileDrawX + moveOffsetX, tileDrawY + moveOffsetY, fromTileDrawX + moveOffsetX, fromTileDrawY + moveOffsetY, 1.0F, 1.0F, 1.0F, 0.5F
                  )
               );
         }

         FontOptions fontOptions = new FontOptions(12).alphaf(0.8F);
         this.drawOptions
            .add(
               () -> {
                  FontManager.bit.drawString(tileDrawX, tileDrawY, "" + index, fontOptions);
                  if (this.isMouseOverTile(((Point)node.item).x, ((Point)node.item).y)) {
                     this.mouseNode = node;
                     GameTooltipManager.addTooltip(
                        new StringTooltips("Path cost: " + GameMath.toDecimals(node.pathCost, 1))
                           .add("Node cost: " + GameMath.toDecimals(node.nodeCost, 1))
                           .add("Heuristic cost: " + GameMath.toDecimals(node.heuristicCost, 1))
                           .add("Total cost: " + nodePriority.cost.getCost(node))
                           .add("Path count: " + node.pathCount),
                        TooltipLocation.PLAYER
                     );
                  }
               }
            );
      }
   }

   private void addTileDrawOptions(Point tile, int index, float r, float g, float b, float alpha, GameCamera camera) {
      int tileDrawX = camera.getTileDrawX(tile.x);
      int tileDrawY = camera.getTileDrawY(tile.y);
      if (tileDrawX > -32 && tileDrawY > -32 && tileDrawX < camera.getWidth() && tileDrawY < camera.getHeight()) {
         this.quadDrawOptions.addFull().size(32, 32).color(r, g, b, alpha).pos(tileDrawX, tileDrawY);
         FontOptions fontOptions = new FontOptions(12).alphaf(0.8F);
         this.drawOptions.add(() -> FontManager.bit.drawString(tileDrawX, tileDrawY, "" + index, fontOptions));
      }
   }
}
