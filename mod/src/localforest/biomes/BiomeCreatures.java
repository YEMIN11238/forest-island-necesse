package localforest.biomes;

import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import localforest.world.BiomeLayout;
import localforest.world.GeneratedLayout;
import localforest.world.WorldDefinition;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.Mob;
import necesse.level.maps.Level;
import necesse.level.maps.regionSystem.Region;

/** Original art on native mobs: native pathfinding, health, attacks, loot, packets and saves. */
public final class BiomeCreatures {
    public static final int MAX_ANIMALS=12,MAX_MONSTERS=16;
    private static final Map<Level,BiomeLayout> layouts=new java.util.WeakHashMap<>();
    public static synchronized BiomeLayout layout(Level level,WorldDefinition world){return layouts.computeIfAbsent(level,l->new BiomeLayout(world));}
    public record Species(String biome,String name,String display,boolean animal,int health,float speed,
                          int damage,int armor,String drop,boolean ranged,boolean winged,Class<? extends Mob> type) {
        public String id(){return "ml"+biome+name.replace("-","");}
        public String texture(){return "mosslume/biomes/"+biome+"/"+(animal?"animals/":"monsters/")+name;}
    }
    public static final List<Species> SPECIES=List.of(
        animal("wetland","frog","泽地蛙",14,25,"swamplarva",false,Frog.class),
        animal("wetland","dragonfly","苇蜻蜓",10,40,null,true,Dragonfly.class),
        animal("mushroom","snail","菌林蜗牛",20,12,null,false,Snail.class),
        animal("mushroom","moth","月翅蛾",10,28,null,true,Moth.class),
        animal("snowfield","snow-rabbit","雪兔",18,34,"rabbitleg",false,SnowRabbit.class),
        animal("snowfield","arctic-fox","雪原狐",28,38,"leather",false,ArcticFox.class),
        animal("desert","lizard","沙地蜥蜴",16,32,null,false,Lizard.class),
        animal("desert","fennec","大耳沙狐",24,38,"leather",false,Fennec.class),
        animal("volcano","heat-beetle","耐热甲虫",24,22,null,false,HeatBeetle.class),
        monster("wetland","mud-creature","泥沼怪",70,22,8,0,"clay",false,MudCreature.class),
        monster("wetland","poison-insect","毒沼虫",55,32,7,0,"swamplarva",true,PoisonInsect.class),
        monster("mushroom","spore-creature","孢子怪",65,24,7,0,"swampsludge",true,SporeCreature.class),
        monster("mushroom","fungus-beast","菌甲兽",100,25,12,3,"leather",false,FungusBeast.class),
        monster("snowfield","ice-beast","冰甲兽",100,27,12,4,"snowstone",false,IceBeast.class),
        monster("snowfield","snow-burrower","雪地潜行兽",75,36,10,0,"snowstone",false,SnowBurrower.class),
        monster("desert","sand-scorpion","沙壳蝎",85,30,11,3,"sandstone",false,SandScorpion.class),
        monster("desert","sandworm","沙虫",100,24,12,2,"sandstone",false,Sandworm.class),
        monster("ruins","stone-guard","遗迹石卫",130,21,14,6,"stone",false,StoneGuard.class),
        monster("ruins","bronze-sentinel","青铜哨兵",110,26,12,5,"copperore",false,BronzeSentinel.class),
        monster("volcano","lava-slug","熔壳蛞蝓",110,20,13,3,"stone",false,LavaSlug.class),
        monster("volcano","obsidian-beast","黑曜兽",140,28,15,5,"ironore",false,ObsidianBeast.class)
    );
    private static Species animal(String b,String n,String d,int h,float s,String drop,boolean wings,Class<? extends Mob> t){return new Species(b,n,d,true,h,s,0,0,drop,false,wings,t);}
    private static Species monster(String b,String n,String d,int h,float s,int hit,int armor,String drop,boolean ranged,Class<? extends Mob> t){return new Species(b,n,d,false,h,s,hit,armor,drop,ranged,false,t);}
    public static Species species(String name){return SPECIES.stream().filter(s->s.name.equals(name)).findFirst().orElseThrow();}
    public static Species byID(String id){return SPECIES.stream().filter(s->s.id().equals(id)).findFirst().orElse(null);}
    public static String[] ids(String biome,boolean animals){return SPECIES.stream().filter(s->s.animal==animals&&(biome==null||s.biome.equals(biome))).map(Species::id).toArray(String[]::new);}
    public static void register(){
        for(Species s:SPECIES){int id=MobRegistry.registerMob(s.id(),s.type,true,false,new StaticMessage(s.display),null,false);if(id<0)throw new IllegalStateException("Creature registration failed: "+s.id());}
    }
    public static void initResources(){BiomeCreatureSprites.initResources();}

    /** Called only by native new-region generation. Saved flags survive deaths and reloads. */
    public static void populate(Level level,Region region,WorldDefinition world,GeneratedLayout layout){
        if(!level.isServer()||level.isCave||!world.isMultibiome())return;
        BiomeLayout biomes=layout(level,world);
        for(Species species:SPECIES){
            String marker="mosslumeInitialFaunaV1"+species.id();
            if(level.gndData.getBoolean(marker))continue;
            if(level.entityManager.mobs.stream().anyMatch(m->m.getStringID().equals(species.id()))){level.gndData.setBoolean(marker,true);continue;}
            Point center=biomes.center(species.biome);
            // Each species owns one deterministic candidate area, so different region order
            // and repeated native generation calls cannot duplicate the initial population.
            int ordinal=SPECIES.indexOf(species),dx=species.animal?-5:6,dy=ordinal%2==0?-4:4;
            int wantedX=center.x+dx,wantedY=center.y+dy;
            if(!contains(region,wantedX,wantedY))continue;
            Point chosen=null;
            for(int radius=0;radius<=6&&chosen==null;radius++)for(int y=wantedY-radius;y<=wantedY+radius&&chosen==null;y++)for(int x=wantedX-radius;x<=wantedX+radius;x++){
                if(!contains(region,x,y)||!level.isTileWithinBounds(x,y,3)||!layout.land(x,y))continue;
                if(!layout.biome(x,y).equals(species.biome)||level.getTile(x,y).isLiquid||level.getObject(x,y).isSolid)continue;
                if(Math.hypot(x-world.campX,y-world.campY)<24)continue;
                chosen=new Point(x,y);break;
            }
            if(chosen==null)continue;
            Mob mob=MobRegistry.getMob(species.id(),level);
            mob.canDespawn=false;
            mob.setSpawnTilePosition(chosen.x,chosen.y);
            level.entityManager.addMob(mob,chosen.x*32+16,chosen.y*32+16);
            level.gndData.setBoolean(marker,true);
        }
    }
    private static boolean contains(Region r,int x,int y){return x>=r.tileXOffset&&x<r.tileXOffset+r.tileWidth&&y>=r.tileYOffset&&y<r.tileYOffset+r.tileHeight;}
    public static Map<String,Object> diagnostics(Level level){
        Map<String,Object> out=new LinkedHashMap<>();
        for(Species s:SPECIES)out.put(s.id(),level.entityManager.mobs.stream().filter(m->m.getStringID().equals(s.id())).count());
        out.put("spriteDraws",BiomeCreatureSprites.draws);out.put("nativeMeleeHits",BiomeMonster.meleeHits);out.put("nativeProjectiles",BiomeMonster.shots);return out;
    }
    public static class Frog extends BiomeAnimal {public Frog(){super("frog");}}
    public static class Dragonfly extends BiomeAnimal {public Dragonfly(){super("dragonfly");}}
    public static class Snail extends BiomeAnimal {public Snail(){super("snail");}}
    public static class Moth extends BiomeAnimal {public Moth(){super("moth");}}
    public static class SnowRabbit extends BiomeAnimal {public SnowRabbit(){super("snow-rabbit");}}
    public static class ArcticFox extends BiomeAnimal {public ArcticFox(){super("arctic-fox");}}
    public static class Lizard extends BiomeAnimal {public Lizard(){super("lizard");}}
    public static class Fennec extends BiomeAnimal {public Fennec(){super("fennec");}}
    public static class HeatBeetle extends BiomeAnimal {public HeatBeetle(){super("heat-beetle");}}
    public static class MudCreature extends BiomeMonster {public MudCreature(){super("mud-creature");}}
    public static class PoisonInsect extends BiomeMonster {public PoisonInsect(){super("poison-insect");}}
    public static class SporeCreature extends BiomeMonster {public SporeCreature(){super("spore-creature");}}
    public static class FungusBeast extends BiomeMonster {public FungusBeast(){super("fungus-beast");}}
    public static class IceBeast extends BiomeMonster {public IceBeast(){super("ice-beast");}}
    public static class SnowBurrower extends BiomeMonster {public SnowBurrower(){super("snow-burrower");}}
    public static class SandScorpion extends BiomeMonster {public SandScorpion(){super("sand-scorpion");}}
    public static class Sandworm extends BiomeMonster {public Sandworm(){super("sandworm");}}
    public static class StoneGuard extends BiomeMonster {public StoneGuard(){super("stone-guard");}}
    public static class BronzeSentinel extends BiomeMonster {public BronzeSentinel(){super("bronze-sentinel");}}
    public static class LavaSlug extends BiomeMonster {public LavaSlug(){super("lava-slug");}}
    public static class ObsidianBeast extends BiomeMonster {public ObsidianBeast(){super("obsidian-beast");}}
    private BiomeCreatures(){}
}
