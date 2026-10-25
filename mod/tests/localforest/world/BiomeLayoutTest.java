package localforest.world;

import java.awt.Point;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Save compatibility and traversable scattered habitats, independent of the native renderer. */
public final class BiomeLayoutTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final String V2_FINGERPRINT="f741f987ba778265878af5cb755c2416702b2607ac413c9591311ac411e868ef";
    public static void main(String[] args){
        require(WorldContent.load(1).fingerprint.equals("e4b744d4387ed24e9b4c382513b0ee2fb411b03383333c42c4434579b5baedb0"),"Content v1 changed");
        for(String preset:List.of("forest_island","woodland")){
            WorldDefinition d=WorldDefinition.fromSeed(WorldDefinition.encodeSeed("mosslume",preset,"standard"));
            require(d.generatorVersion==1&&d.contentVersion==1,"Old preset was silently upgraded");
            long expected=preset.equals("forest_island")?0x96b2ba6df86a1762L:0x0ccaa9b69ec761dbL;
            require(golden(new GeneratedLayout(d))==expected,"Old map generation changed: "+preset);
        }
        require(WorldContent.load(2).fingerprint.equals(V2_FINGERPRINT),"Content v2 changed");
        String[] v2Seeds={"mosslume","苔光","12345"};long[] v2Golden={0x3dc644ebc97b152fL,0xb49411badc399e0fL,0x40992e687449a69cL};
        for(int i=0;i<v2Seeds.length;i++){
            String encoded="MOSS:2:2:biomes:standard:"+Base64.getUrlEncoder().withoutPadding().encodeToString(v2Seeds[i].getBytes(StandardCharsets.UTF_8))+":"+V2_FINGERPRINT;
            WorldDefinition d=WorldDefinition.fromSeed(encoded);
            require(d.generatorVersion==2&&d.isMultibiome(),"Version 2 save no longer recognized");
            require(golden(new GeneratedLayout(d))==v2Golden[i],"Existing v2 map changed: "+v2Seeds[i]);
        }
        int checked=0;Set<String> arrangements=new HashSet<>();
        for(String seed:List.of("4212026","mosslume","苔光","12345","north","a","b","seed with spaces","夜雾之城")){
            String encoded=WorldDefinition.encodeSeed(seed,"biomes","standard");
            WorldDefinition d=WorldDefinition.fromSeed(encoded);GeneratedLayout layout=new GeneratedLayout(d);
            require(d.generatorVersion==3&&d.contentVersion==3&&d.isMultibiome(),"Missing v3 save identity");
            require(d.width==256&&d.height==224,"Wrong finite world extent");
            require(WorldDefinition.fromSeed(encoded).encodedSeed.equals(encoded)&&d.userSeed.equals(seed),"Save seed roundtrip changed");
            Map<String,Integer> coverage=new HashMap<>();int forestView=0,paths=0;boolean[] exits=new boolean[64];
            for(int y=0;y<d.height;y++)for(int x=0;x<d.width;x++){
                if(layout.land(x,y))coverage.merge(layout.biome(x,y),1,Integer::sum);
                if(Math.hypot(x-d.spawnX,y-d.spawnY)<=32){
                    require(layout.land(x,y)&&layout.biome(x,y).equals("forest"),"Spawn view is not continuous forest: "+seed+" "+x+","+y);forestView++;
                }
                if(layout.path(x,y)){
                    double distance=Math.hypot(x-d.campX,y-d.campY);require(distance<26,"A global road escaped the local camp: "+seed);paths++;
                    if(distance>=18&&distance<19.5){int bin=(int)Math.floor((Math.atan2(y-d.campY,x-d.campX)+Math.PI)/(Math.PI*2)*64);exits[Math.floorMod(bin,64)]=true;}
                }
            }
            require(forestView>3000&&coverage.getOrDefault("forest",0)>6000,"Missing broad home forest");
            require(paths>50&&paths<160,"Camp paths are absent or excessive");
            int exitGroups=0;for(int i=0;i<64;i++)if(exits[i]&&!exits[(i+63)%64])exitGroups++;
            require(exitGroups==1,"Camp is not a single forest exit: "+seed+" exits="+exitGroups);
            Set<Long> reachable=flood(layout);StringBuilder positions=new StringBuilder();List<Double> angles=new ArrayList<>();
            for(String biome:BiomeLayout.IDS){
                require(coverage.getOrDefault(biome,0)>500,"Biome absent or too small: "+biome+" "+seed);
                Point center=layout.biomeLayout().center(biome);positions.append(center.x).append(',').append(center.y).append(';');
                require(layout.biome(center.x,center.y).equals(biome),"Wrong center biome");
                require(Math.hypot(center.x-d.campX,center.y-d.campY)>=54,"Biome crowding home forest");
                require(reachable.contains(key(center.x,center.y)),"Unreachable center: "+biome+" "+seed);
                require(!layout.path(center.x,center.y),"Biome center has a forced road hub");
                angles.add(Math.atan2(center.y-d.campY,center.x-d.campX));
            }
            Collections.sort(angles);double gapVariation=0;
            for(int i=0;i<angles.size();i++){double next=i==angles.size()-1?angles.get(0)+Math.PI*2:angles.get(i+1);gapVariation+=Math.abs(next-angles.get(i)-Math.PI/3);}
            require(gapVariation>.6,"Biome centers formed a regular six-way arrangement");
            require(arrangements.add(positions.toString()),"Different seeds reused fixed biome positions");
            require(reachable.contains(key(d.caveX,d.caveY)),"Unreachable native cave entrance");
            require(layout.biomeLayout().structures().size()==18,"Missing structures");
            for(BiomeLayout.Structure structure:layout.biomeLayout().structures()){
                require(layout.biome(structure.x(),structure.y()).equals(structure.biome()),"Structure in wrong biome");
                require(reachable.contains(key(structure.x(),structure.y()+1)),"Structure entrance unreachable: "+structure+" "+seed);
                for(int x=structure.x()-2;x<=structure.x()+2;x++)for(int y=structure.y()-2;y<=structure.y()+1;y++){
                    require(layout.land(x,y),"Structure footprint or entrance in water");
                    require(!layout.path(x,y),"Structure blocks road");require(layout.object(x,y)==null,"Generated flora overwrites structure/entrance");
                }
                if(structure.variant()==0){
                    for(int x=structure.x()-4;x<=structure.x()+4;x++)for(int y=structure.y()+1;y<=structure.y()+4;y++){
                        require(layout.land(x,y)&&layout.biome(x,y).equals(structure.biome()),"Utility yard left its dry biome: "+structure+" "+seed);
                        require(layout.object(x,y)==null&&!layout.path(x,y),"Utility or approach overwritten by flora/path");
                    }
                    for(int dx:new int[]{-3,3})require(reachable.contains(key(structure.x()+dx,structure.y()+4)),"Utility interaction approach unreachable: "+structure+" "+seed);
                }
            }
            require(golden(layout)==golden(new GeneratedLayout(WorldDefinition.fromSeed(encoded))),"Generation is not deterministic");
            for(int x=0;x<d.width;x++)require(!layout.land(x,0)&&!layout.land(x,d.height-1),"North/south world escape");
            for(int y=0;y<d.height;y++)require(!layout.land(0,y)&&!layout.land(d.width-1,y),"East/west world escape");
            checked++;
        }
        // Placement stress focuses on seeds that could otherwise exhaust constrained samplers.
        for(int i=0;i<32;i++){
            GeneratedLayout g=new GeneratedLayout(WorldDefinition.fromSeed(WorldDefinition.encodeSeed("placement-stress-"+i,"biomes","standard")));
            require(g.biomeLayout().structures().size()==18,"Structure sampler exhausted: "+i);
        }
        System.out.println("BiomeLayoutTest PASS v3worlds="+checked+" placementStress=32 forestSpawnRadius=32 campExits=1 noGlobalRoads=true regions=6 structures=18 utilityApproaches=12 reachable=true contentV1V2=unchanged mapV1V2=golden");
    }
    private static long golden(GeneratedLayout g){long h=0xcbf29ce484222325L;for(int y=0;y<g.world.height;y++)for(int x=0;x<g.world.width;x++){String o=g.object(x,y);h=(h^(g.land(x,y)?1:0))*0x100000001b3L;h=(h^g.biome(x,y).hashCode())*0x100000001b3L;h=(h^(o==null?0:o.hashCode()))*0x100000001b3L;h=(h^(g.path(x,y)?1:0))*0x100000001b3L;}return h;}
    private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    private static Set<Long> flood(GeneratedLayout g){
        Set<Long> visited=new HashSet<>();ArrayDeque<Point> queue=new ArrayDeque<>();boolean[][] blocked=new boolean[g.world.width][g.world.height];
        for(BiomeLayout.Structure s:g.biomeLayout().structures()){
            for(int x=s.x()-1;x<=s.x()+1;x++)for(int y=s.y()-1;y<=s.y();y++)blocked[x][y]=true;
            if(g.world.generatorVersion>=3&&s.variant()==0){blocked[s.x()-3][s.y()+3]=true;blocked[s.x()+3][s.y()+3]=true;}
        }
        queue.add(new Point(g.world.spawnX,g.world.spawnY));visited.add(key(g.world.spawnX,g.world.spawnY));
        while(!queue.isEmpty()){
            Point p=queue.remove();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                int x=p.x+d[0],y=p.y+d[1];long key=key(x,y);
                if(visited.contains(key)||!g.inside(x,y)||!g.land(x,y)||blocked[x][y])continue;
                String object=g.object(x,y);
                if(object!=null&&(object.endsWith("tree")||object.endsWith("bush")||object.contains("rock")||object.startsWith("mlbiome_ruins_flora")&&Integer.parseInt(object.substring(object.length()-1))<6))continue;
                visited.add(key);queue.add(new Point(x,y));
            }
        }
        return visited;
    }
}
