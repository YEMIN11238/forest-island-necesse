package localforest.bosses;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;

/** Two opt-in native encounters. No automatic world population or replacement of stock bosses. */
public final class MosslumeBosses {
    public record Definition(String id,String art,String name,int health,int armor,float speed,String summonId,String summonName,String drop) {}
    public static final Definition STONE=new Definition("mlstonewatcher","stone-watcher","石符术士",1100,6,38,"mlstoneecho","石符召令","copperbar");
    public static final Definition MOON=new Definition("mlmoonreaper","moon-reaper","白发镰刃战士",1000,4,60,"mlmoonseal","月镰印记","ironbar");
    public static final List<Definition> ALL=List.of(STONE,MOON);
    public static void register(){
        MobRegistry.registerMob(STONE.id(),StoneWatcher.class,true,true,new StaticMessage(STONE.name()),new StaticMessage("在空旷野外使用工作台制作的石符召令。"),false);
        MobRegistry.registerMob(MOON.id(),MoonReaper.class,true,true,new StaticMessage(MOON.name()),new StaticMessage("在空旷野外使用工作台制作的月镰印记。"),false);
        ProjectileRegistry.registerProjectile("mlwatchershard",BossProjectile.StoneShard.class,null,null);
        ItemRegistry.registerItem(STONE.summonId(),new BossSummonItem(STONE),0,true);
        ItemRegistry.registerItem(MOON.summonId(),new BossSummonItem(MOON),0,true);
        Recipes.registerModRecipe(new Recipe(STONE.summonId(),1,RecipeTechRegistry.WORKSTATION,new Ingredient[]{new Ingredient("stone",12),new Ingredient("copperore",4)}));
        Recipes.registerModRecipe(new Recipe(MOON.summonId(),1,RecipeTechRegistry.WORKSTATION,new Ingredient[]{new Ingredient("oaklog",12),new Ingredient("swampsludge",4)}));
    }
    public static void initResources(){BossSprites.initResources();}
    public static Map<String,Object> diagnostics(){Map<String,Object> out=new LinkedHashMap<>(BossSprites.diagnostics());out.put("encountersStarted",MosslumeBossMob.encountersStarted);out.put("attacksStarted",MosslumeBossMob.attacksStarted);out.put("areaHits",MosslumeBossMob.areaHits);out.put("projectilesCreated",MosslumeBossMob.projectilesCreated);out.put("phaseTransitions",MosslumeBossMob.phaseTransitions);out.put("projectileHits",BossProjectile.mobHits);return out;}
    public static final class StoneWatcher extends MosslumeBossMob {public StoneWatcher(){super(STONE);}}
    public static final class MoonReaper extends MosslumeBossMob {public MoonReaper(){super(MOON);}}
    private MosslumeBosses(){}
}
