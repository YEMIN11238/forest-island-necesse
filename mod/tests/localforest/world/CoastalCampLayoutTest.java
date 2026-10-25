package localforest.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Local authored POI invariants, not a claim of visual/runtime approval. */
public final class CoastalCampLayoutTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        int worlds=0,minTrees=Integer.MAX_VALUE;
        for(String seed:new String[]{"4212026","mosslume","苔光","12345","coastal-5","coastal-6"}){
            var d=WorldDefinition.fromSeed(WorldDefinition.encodeSeed(seed,"continuous","relaxed"));
            var a=new ContinuousWorldLayout(d);var coast=a.coast.connectToNative((x,y)->false);
            require(coast.nativeExitVerified&&coast.exitY==-40,"Did not join nearby native land");
            Set<Point> reached=flood(coast,d);
            for(var row:d.content.campObjects){
                int x=row.x(d),y=row.y(d);
                require(coast.terrain(x,y)==CoastalCampLayout.Terrain.LAND,"Template object on water: "+row);
                require(reached.contains(new Point(x,y)),"Disconnected template: "+row);
            }
            for(int[] hut:new int[][]{{-11,-13},{4,-22},{-17,-26}}){
                for(int y=-1;y<=6;y++)for(int x=-1;x<=7;x++){
                    int tx=d.campX+hut[0]+x,ty=d.campY+hut[1]+y;
                    require(coast.terrain(tx,ty)==CoastalCampLayout.Terrain.LAND,"Hut on water "+hut[0]+","+hut[1]+" @"+tx+","+ty);
                    require(coast.object(tx,ty)==null,"Population footprint occupied");
                    for(var row:d.content.campObjects)require(row.x(d)!=tx||row.y(d)!=ty,"Camp prop blocks population footprint: "+row);
                }
                require(reached.contains(new Point(d.campX+hut[0]+3,d.campY+hut[1]+5)),"Hut has no walkable connection");
            }
            for(int i=0;i<6;i++){
                int x=coast.animalX(i),y=coast.animalY(i);
                require(coast.terrain(x,y)==CoastalCampLayout.Terrain.LAND&&coast.object(x,y)==null&&reached.contains(new Point(x,y)),"Unsafe animal start");
                for(int[] hut:new int[][]{{-11,-13},{4,-22},{-17,-26}})
                    require(x<d.campX+hut[0]-1||x>d.campX+hut[0]+7||y<d.campY+hut[1]-1||y>d.campY+hut[1]+6,"Starter animal blocks NPC house");
            }
            for(int[] point:new int[][]{{-2,-2},{1,-2},{3,-4},{-7,2},{-5,4}}){
                int x=d.campX+point[0],y=d.campY+point[1];
                require(coast.terrain(x,y)==CoastalCampLayout.Terrain.LAND&&coast.object(x,y)==null&&reached.contains(new Point(x,y)),"Unsafe core start/work point");
            }
            int water=0,land=0,trees=0,flowers=0,bushes=0,paths=0;
            for(int dy=-13;dy<=15;dy++)for(int dx=-18;dx<=18;dx++){
                int x=d.campX+dx,y=d.campY+dy;var t=coast.terrain(x,y);
                if(t==CoastalCampLayout.Terrain.WATER)water++;if(t==CoastalCampLayout.Terrain.LAND)land++;
                String object=coast.object(x,y);
                if(object!=null){if(object.endsWith("tree"))trees++;if(object.endsWith("flowerpatch"))flowers++;if(object.endsWith("bush"))bushes++;}
                if(coast.path(x,y)){paths++;require(reached.contains(new Point(x,y)),"Disconnected path");}
            }
            require(water>260&&land>500,"Sea not visible in the core camera: water="+water+" land="+land);
            require(trees>=25&&flowers>=15&&bushes>=25,"Sparse scene: trees="+trees+" flowers="+flowers+" bushes="+bushes);
            require(paths>70&&paths<220,"Excessive/empty path coverage: "+paths);
            require(coast.terrain(d.campX+400,d.campY)==CoastalCampLayout.Terrain.NATIVE,"Remote generator changed");
            require(coast.terrain(d.campX,d.campY+64)==CoastalCampLayout.Terrain.NATIVE,"Southern native generator changed");
            var again=new ContinuousWorldLayout(d).coast.connectToNative((x,y)->false);
            for(int dx=-32;dx<32;dx++)for(int dy=-44;dy<26;dy++)
                require(java.util.Objects.equals(coast.object(d.campX+dx,d.campY+dy),again.object(d.campX+dx,d.campY+dy)),"Visit-order-dependent POI");
            minTrees=Math.min(minTrees,trees);worlds++;
            if(args.length>0&&seed.equals("4212026"))diagram(coast,d,Path.of(args[0]));
        }
        var d=WorldDefinition.fromSeed(WorldDefinition.encodeSeed("far-exit","continuous","standard"));
        var far=new ContinuousWorldLayout(d).coast.connectToNative((x,y)->y>d.campY-70);
        require(far.nativeExitVerified&&far.exitY<=-72&&flood(far,d).contains(new Point(d.campX+far.exitX,d.campY+far.exitY)),"Bounded native-land fallback disconnected");
        System.out.println("CoastalCampLayoutTest PASS worlds="+worlds+" realTerrainPolicy=true compactSeaVisible=true allHutsDry=true allPathsConnected=true minCoreTrees="+minTrees+" nativeFarField=true");
    }
    private static Set<Point> flood(CoastalCampLayout coast,WorldDefinition d){
        Set<Point> seen=new HashSet<>();ArrayDeque<Point> queue=new ArrayDeque<>();Point start=new Point(d.spawnX,d.spawnY);seen.add(start);queue.add(start);
        while(!queue.isEmpty()){
            Point p=queue.remove();for(int[] offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                Point n=new Point(p.x+offset[0],p.y+offset[1]);if(coast.terrain(n.x,n.y)==CoastalCampLayout.Terrain.LAND&&seen.add(n))queue.add(n);
            }
        }return seen;
    }
    private static void diagram(CoastalCampLayout coast,WorldDefinition d,Path directory)throws Exception{
        Files.createDirectories(directory);int scale=10,left=-32,top=-46,width=66,height=76;
        BufferedImage image=new BufferedImage(width*scale,height*scale,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        Map<String,Color> colors=Map.of("oaktree",new Color(47,95,48),"birchtree",new Color(67,115,58),"blueberrybush",new Color(85,127,68),"raspberrybush",new Color(94,136,72),"yellowflowerpatch",new Color(207,188,89),"redflowerpatch",new Color(173,111,110),"grass",new Color(131,160,91),"surfacerocksmall",Color.GRAY);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int tx=d.campX+left+x,ty=d.campY+top+y;var t=coast.terrain(tx,ty);
            g.setColor(switch(t){case NATIVE->new Color(57,69,70);case LAND->new Color(151,169,105);case WATER->new Color(31,109,142);});g.fillRect(x*scale,y*scale,scale,scale);
            if(coast.path(tx,ty)){g.setColor(new Color(197,171,121));g.fillRect(x*scale,y*scale,scale,scale);}
            String object=coast.object(tx,ty);if(object!=null){g.setColor(colors.get(object));if(object.endsWith("tree"))g.fillOval(x*scale-3,y*scale-6,16,16);else g.fillRect(x*scale+3,y*scale+3,4,4);}
        }
        g.setColor(new Color(139,82,48));g.fillRect((d.houseX-d.campX-2-left)*scale,(d.houseY-d.campY-4-top)*scale,5*scale,4*scale);
        for(int[] hut:new int[][]{{-11,-13},{4,-22},{-17,-26}})g.fillRect((hut[0]-left)*scale,(hut[1]-top)*scale,7*scale,6*scale);
        for(var row:d.content.campObjects)if(!row.role().equals("wall")&&!row.role().equals("bed")){
            g.setColor(row.role().equals("fuel")?Color.ORANGE:row.role().equals("light")?Color.YELLOW:new Color(205,192,155));g.fillRect((row.x(d)-d.campX-left)*scale+2,(row.y(d)-d.campY-top)*scale+2,6,6);
        }
        g.setColor(Color.WHITE);g.drawOval((d.spawnX-d.campX-left)*scale,(d.spawnY-d.campY-top)*scale,9,9);g.dispose();
        ImageIO.write(image,"png",directory.resolve("coastal-camp-plan-4212026.png").toFile());
    }
}
