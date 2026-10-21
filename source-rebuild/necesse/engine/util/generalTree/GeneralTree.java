package necesse.engine.util.generalTree;

public class GeneralTree {
   public int xTopAdjustment = 0;
   public int yTopAdjustment = 0;
   public int maxDepth = Integer.MAX_VALUE;
   public int levelSeparation;
   public int siblingSeparation;
   public int subtreeSeparation;

   public GeneralTree(int levelSeparation, int siblingSeparation, int subtreeSeparation) {
      this.levelSeparation = levelSeparation;
      this.siblingSeparation = siblingSeparation;
      this.subtreeSeparation = subtreeSeparation;
   }

   public boolean calculateNodePositions(GeneralTreeNode rootNode) {
      return this.calculateNodePositions(rootNode, 0, 0);
   }

   public boolean calculateNodePositions(GeneralTreeNode rootNode, int startX, int startY) {
      this.xTopAdjustment = startX;
      this.yTopAdjustment = startY;
      if (rootNode != null) {
         rootNode.resetPos();
         this.firstWalk(rootNode);
         this.xTopAdjustment = rootNode.x - rootNode.prelim;
         this.yTopAdjustment = rootNode.y;
         return this.secondWalk(rootNode, 0);
      } else {
         return true;
      }
   }

   protected void firstWalk(GeneralTreeNode node) {
      node.modifier = 0;
      if (!node.isLeaf() && node.level != this.maxDepth) {
         GeneralTreeNode rightMost;
         GeneralTreeNode leftMost = rightMost = node.getFirstChild();
         this.firstWalk(leftMost);

         while (rightMost.hasRightSibling()) {
            rightMost = rightMost.getRightSibling();
            this.firstWalk(rightMost);
         }

         int midPoint = (leftMost.prelim + rightMost.prelim) / 2;
         if (node.hasLeftSibling()) {
            node.prelim = node.getLeftSibling().prelim + this.siblingSeparation + this.meanNodeSize(node.getLeftSibling(), node);
            node.modifier = node.prelim - midPoint;
            this.apportion(node);
         } else {
            node.prelim = midPoint;
         }
      } else if (node.hasLeftSibling()) {
         node.prelim = node.getLeftSibling().prelim + this.siblingSeparation + this.meanNodeSize(node.getLeftSibling(), node);
      } else {
         node.prelim = 0;
      }
   }

   protected boolean secondWalk(GeneralTreeNode node, int modSum) {
      boolean result = true;
      if (node.level <= this.maxDepth) {
         int xTemp = this.xTopAdjustment + node.prelim + modSum;
         int yTemp = this.yTopAdjustment + node.level * this.levelSeparation;
         if (this.canFitInTree(xTemp, yTemp)) {
            node.x = xTemp;
            node.y = yTemp;
            if (!node.getChildren().isEmpty()) {
               result = this.secondWalk(node.getFirstChild(), modSum + node.modifier);
            }

            if (result && node.hasRightSibling()) {
               result = this.secondWalk(node.getRightSibling(), modSum);
            }
         } else {
            result = false;
         }
      }

      return result;
   }

   protected void apportion(GeneralTreeNode node) {
      GeneralTreeNode leftMost = node.getFirstChild();
      GeneralTreeNode neighbour = leftMost.leftNeighbour;
      int compareDepth = 1;
      int depthToStop = this.maxDepth - node.level;

      while (leftMost != null && neighbour != null && compareDepth <= depthToStop) {
         int leftModSum = 0;
         int rightModSum = 0;
         GeneralTreeNode ancestorLeftmost = leftMost;
         GeneralTreeNode ancestorNeighbour = neighbour;

         for (int i = 0; i < compareDepth; i++) {
            ancestorLeftmost = ancestorLeftmost.parent;
            ancestorNeighbour = ancestorNeighbour.parent;
            if (ancestorNeighbour == null) {
               return;
            }

            rightModSum += ancestorLeftmost.modifier;
            leftModSum += ancestorNeighbour.modifier;
         }

         int moveDistance = neighbour.prelim + leftModSum + this.subtreeSeparation + this.meanNodeSize(leftMost, neighbour) - (leftMost.prelim + rightModSum);
         if (moveDistance > 0) {
            int leftSiblings = 0;

            GeneralTreeNode tempPtr;
            for (tempPtr = node; tempPtr != null && tempPtr != ancestorNeighbour; tempPtr = tempPtr.getLeftSibling()) {
               leftSiblings++;
            }

            if (tempPtr == null) {
               return;
            }

            int portion = moveDistance / leftSiblings;

            for (GeneralTreeNode var15 = node; var15 != ancestorNeighbour; var15 = var15.getLeftSibling()) {
               var15.prelim += moveDistance;
               var15.modifier += moveDistance;
               moveDistance -= portion;
            }
         }

         compareDepth++;
         if (leftMost.isLeaf()) {
            leftMost = this.getLeftMost(node, compareDepth);
         } else {
            leftMost = leftMost.getFirstChild();
         }
      }
   }

   private GeneralTreeNode getLeftMost(GeneralTreeNode node, int depth) {
      if (node.level >= depth) {
         return node;
      }

      if (node.isLeaf()) {
         return null;
      }

      GeneralTreeNode rightMost = node.getFirstChild();

      GeneralTreeNode leftMost;
      for (leftMost = this.getLeftMost(rightMost, depth); leftMost != null && rightMost.hasRightSibling(); leftMost = this.getLeftMost(rightMost, depth)) {
         rightMost = rightMost.getRightSibling();
      }

      return leftMost;
   }

   protected int meanNodeSize(GeneralTreeNode left, GeneralTreeNode right) {
      int size = 0;
      if (left != null) {
         size += left.nodeSize / 2;
      }

      if (right != null) {
         size += right.nodeSize / 2;
      }

      return size;
   }

   public boolean canFitInTree(int xCoord, int yCoord) {
      return true;
   }
}
