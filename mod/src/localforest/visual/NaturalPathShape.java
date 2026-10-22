package localforest.visual;

/** Pixel-space centre lines for dirt trails. This is geometry, independent of the game/GL. */
public final class NaturalPathShape {
    public static final int NORTH=1, EAST=2, SOUTH=4, WEST=8;
    public static final int NORTH_WEST=16, NORTH_EAST=32, SOUTH_EAST=64, SOUTH_WEST=128;
    private static final double[][] PORTS={{16,0},{32,16},{16,32},{0,16},{0,0},{32,0},{32,32},{0,32}};

    /** Shared edge ports join adjacent native tiles; the interior curve rounds each bend. */
    public static int coverage(int x,int y,int worldX,int worldY,int neighbours) {
        double px=x+.5,py=y+.5;
        int connected=neighbours&15;
        // A truly diagonal neighbour gets a corner port only when neither cardinal route
        // already connects it. Grass-side corner wedges below keep the connection full-width.
        if((neighbours&NORTH_WEST)!=0&&(neighbours&(NORTH|WEST))==0)connected|=NORTH_WEST;
        if((neighbours&NORTH_EAST)!=0&&(neighbours&(NORTH|EAST))==0)connected|=NORTH_EAST;
        if((neighbours&SOUTH_EAST)!=0&&(neighbours&(SOUTH|EAST))==0)connected|=SOUTH_EAST;
        if((neighbours&SOUTH_WEST)!=0&&(neighbours&(SOUTH|WEST))==0)connected|=SOUTH_WEST;
        int count=Integer.bitCount(connected);
        double distance;
        if(count==2) {
            int first=Integer.numberOfTrailingZeros(connected);
            int second=Integer.numberOfTrailingZeros(connected&~(1<<first));
            double[] a=PORTS[first],b=PORTS[second];
            // A quadratic through the middle replaces the staircase-shaped right-angle turn.
            // Opposite ports receive a small bend, while all four boundary centres stay fixed.
            double drift=first<4&&second<4?Math.sin(worldX*.031+worldY*.057)*2.4:0;
            double cx=16+(first%2==0&&second%2==0?drift:0);
            double cy=16+(first%2==1&&second%2==1?drift:0);
            distance=Double.POSITIVE_INFINITY;
            double lastX=a[0],lastY=a[1];
            for(int step=1;step<=12;step++) {
                double t=step/12.0,u=1-t;
                double nextX=u*u*a[0]+2*u*t*cx+t*t*b[0];
                double nextY=u*u*a[1]+2*u*t*cy+t*t*b[1];
                distance=Math.min(distance,segmentDistance(px,py,lastX,lastY,nextX,nextY));
                lastX=nextX;lastY=nextY;
            }
        } else {
            distance=Math.hypot(px-16,py-16);
            for(int edge=0;edge<8;edge++)if((connected&(1<<edge))!=0)
                distance=Math.min(distance,segmentDistance(px,py,16,16,PORTS[edge][0],PORTS[edge][1]));
        }
        // A complete 2x2 patch is a worn clearing, not four tracks enclosing a square grass hole.
        if((neighbours&(NORTH|WEST|NORTH_WEST))==(NORTH|WEST|NORTH_WEST)&&px<=16&&py<=16
            ||(neighbours&(NORTH|EAST|NORTH_EAST))==(NORTH|EAST|NORTH_EAST)&&px>=16&&py<=16
            ||(neighbours&(SOUTH|EAST|SOUTH_EAST))==(SOUTH|EAST|SOUTH_EAST)&&px>=16&&py>=16
            ||(neighbours&(SOUTH|WEST|SOUTH_WEST))==(SOUTH|WEST|SOUTH_WEST)&&px<=16&&py>=16)
            distance=0;
        return coverageForDistance(x,y,worldX,worldY,distance);
    }

    /** Narrow verge triangles on unmodified grass complete a full-width diagonal connection. */
    public static int cornerCoverage(int x,int y,int worldX,int worldY,int neighbours) {
        double px=x+.5,py=y+.5,distance=Double.POSITIVE_INFINITY;
        if((neighbours&(NORTH|WEST|NORTH_WEST))==(NORTH|WEST))
            distance=Math.min(distance,segmentDistance(px,py,-16,16,16,-16));
        if((neighbours&(NORTH|EAST|NORTH_EAST))==(NORTH|EAST))
            distance=Math.min(distance,segmentDistance(px,py,16,-16,48,16));
        if((neighbours&(SOUTH|EAST|SOUTH_EAST))==(SOUTH|EAST))
            distance=Math.min(distance,segmentDistance(px,py,48,16,16,48));
        if((neighbours&(SOUTH|WEST|SOUTH_WEST))==(SOUTH|WEST))
            distance=Math.min(distance,segmentDistance(px,py,16,48,-16,16));
        return coverageForDistance(x,y,worldX,worldY,distance);
    }
    public static boolean hasCorner(int neighbours) {
        return (neighbours&(NORTH|WEST|NORTH_WEST))==(NORTH|WEST)
            ||(neighbours&(NORTH|EAST|NORTH_EAST))==(NORTH|EAST)
            ||(neighbours&(SOUTH|EAST|SOUTH_EAST))==(SOUTH|EAST)
            ||(neighbours&(SOUTH|WEST|SOUTH_WEST))==(SOUTH|WEST);
    }
    private static int coverageForDistance(int x,int y,int worldX,int worldY,double distance) {
        // World-space variation is continuous across tile boundaries. It is deliberately small:
        // the dirt remains a readable track instead of a chain of lumpy discs.
        int wx=worldX+x,wy=worldY+y;
        double radius=11.8+1.05*Math.sin(wx*.073+wy*.11)+.65*Math.sin(wx*.22-wy*.135);
        double edge=radius-distance;
        // Short pixel clusters let grass nibble into the verge, without a blurred halo.
        int hash=hash(wx/2,wy/2);
        if(edge<1.4)edge+=(hash&7)*.13-.4;
        return edge>=.7?255:edge>=0?176:0;
    }

    private static double segmentDistance(double x,double y,double ax,double ay,double bx,double by) {
        double dx=bx-ax,dy=by-ay;
        double t=Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy)));
        return Math.hypot(x-ax-t*dx,y-ay-t*dy);
    }
    private static int hash(int x,int y){int n=x*374761393+y*668265263;return (n^(n>>>13))*1274126177;}
    private NaturalPathShape(){}
}
