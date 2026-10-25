package localforest.world;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Native island-plan invariants; optional output is a planning diagram, not a game screenshot. */
public final class NativeFiniteTerrainTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        int worlds=0;
        for(String seed:new String[]{"4212026","mosslume","苔光","12345"}){
            WorldDefinition world=WorldDefinition.fromSeed(WorldDefinition.encodeSeed(seed,"biomes","standard"));
            NativeFiniteTerrain first=new NativeFiniteTerrain(world),again=new NativeFiniteTerrain(world);
            int land=0,plants=0,offGridPlants=0;
            for(int y=0;y<world.height;y++)for(int x=0;x<world.width;x++){
                require(first.land(x,y)==again.land(x,y)&&first.plant(x,y)==again.plant(x,y)&&first.secondarySolid(x,y)==again.secondarySolid(x,y),"Seed instability");
                if(first.land(x,y))land++;
                if(first.plant(x,y)){
                    plants++;if(x%2!=0||y%2!=0)offGridPlants++;
                    require(first.clearance(x,y)>=3,"Plant closes a narrow shoreline passage");
                    for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(dx!=0||dy!=0)
                        require(!first.plant(x+dx,y+dy)&&!first.secondarySolid(x+dx,y+dy),"Adjacent solids close forest walking gaps");
                }
                if(Point.distance(x,y,world.spawnX,world.spawnY)<=32)require(first.land(x,y),"Unsafe camp view");
                if(x<3||y<3||x>=world.width-3||y>=world.height-3)require(!first.land(x,y),"Finite boundary escaped");
            }
            require(land==flood(first,world),"Disconnected retained mainland");
            require(plants>200&&offGridPlants>plants*.50,"Vegetation reverted to a regular even-coordinate lattice");
            worlds++;
        }
        if(args.length>0)diagram(Path.of(args[0]));
        System.out.println("NativeFiniteTerrainTest PASS worlds="+worlds+" nativeCellularAutomaton=true connectedMainland=true deterministic=true forestLattice=false spacingSafe=true");
    }
    private static int flood(NativeFiniteTerrain terrain,WorldDefinition world){
        boolean[] seen=new boolean[world.width*world.height];int[] queue=new int[seen.length];int head=0,tail=0,start=world.spawnX+world.spawnY*world.width;
        seen[start]=true;queue[tail++]=start;
        while(head<tail){int at=queue[head++],x=at%world.width,y=at/world.width;
            for(int[] offset:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int nx=x+offset[0],ny=y+offset[1];
                if(terrain.land(nx,ny)){int next=nx+ny*world.width;if(!seen[next]){seen[next]=true;queue[tail++]=next;}}
            }
        }
        return tail;
    }
    private static void diagram(Path directory)throws Exception{
        Files.createDirectories(directory);
        WorldDefinition world=WorldDefinition.fromSeed(WorldDefinition.encodeSeed("4212026","biomes","standard"));
        GeneratedLayout layout=new GeneratedLayout(world);NativeFiniteTerrain terrain=new NativeFiniteTerrain(world);
        Map<String,Integer> colors=Map.of("forest",0x78955d,"wetland",0x597c75,"mushroom",0xa287aa,"ruins",0x969583,"snowfield",0xd7e4ec,"desert",0xcfb479,"volcano",0x895955);
        Map<String,Integer> coverage=new LinkedHashMap<>();int trees=0,paths=0;
        BufferedImage image=new BufferedImage(world.width*4,world.height*4,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        for(int y=0;y<world.height;y++)for(int x=0;x<world.width;x++){
            boolean land=layout.land(x,y);String biome=layout.biome(x,y);int rgb=land?colors.get(biome):0x214e69;
            if(land)coverage.merge(biome,1,Integer::sum);
            g.setColor(new Color(rgb));g.fillRect(x*4,y*4,4,4);
            if(layout.path(x,y)){g.setColor(new Color(0xc7b18d));g.fillRect(x*4,y*4,4,4);paths++;}
            String object=layout.object(x,y);
            if(object!=null&&object.endsWith("tree")){g.setColor(new Color(rgb).darker());g.fillRect(x*4+1,y*4+1,2,2);trees++;}
        }
        for(var structure:layout.biomeLayout().structures()){g.setColor(Color.DARK_GRAY);g.fillRect((structure.x()-1)*4,(structure.y()-1)*4,12,8);}
        for(String biome:BiomeLayout.IDS){Point c=layout.biomeLayout().center(biome);g.setColor(Color.WHITE);g.drawOval(c.x*4-4,c.y*4-4,8,8);}
        g.setColor(Color.WHITE);g.fillRect(world.spawnX*4-6,world.spawnY*4-1,13,3);g.fillRect(world.spawnX*4-1,world.spawnY*4-6,3,13);
        g.setColor(Color.BLACK);g.drawRect(world.caveX*4-3,world.caveY*4-3,6,6);g.dispose();
        Path png=directory.resolve("native-finite-4212026.png");ImageIO.write(image,"png",png.toFile());
        StringBuilder report=new StringBuilder("seed=4212026\nalgorithm=Necesse 1.3.3 finite shaped island and native cellular automaton; Mosslume finite/biome constraints\nsize="+world.width+"x"+world.height+"\nselectedNativeAttempt="+terrain.selectedAttempt+"\ncoverage="+coverage+"\ntrees="+trees+"\nlocalPathTiles="+paths+"\nstructures="+layout.biomeLayout().structures().size()+"\n");
        for(String biome:BiomeLayout.IDS)report.append(biome).append(" center=").append(layout.biomeLayout().center(biome)).append('\n');
        report.append("diagram=land classification; dark dots are native cluster-derived tree placement; rectangles are outposts; white cross is spawn; white circles are habitat centers; black square is cave\nnot_a_game_screenshot=true\n");
        Files.writeString(directory.resolve("native-finite-4212026.txt"),report.toString());System.out.println(report+"diagramPath="+png);
    }
}
