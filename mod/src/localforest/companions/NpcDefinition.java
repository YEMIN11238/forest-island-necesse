package localforest.companions;

import java.util.List;

/** Authored population, separate from a model prompt and from mutable native entities. */
public record NpcDefinition(String id,String name,String nativeType,String occupation,Tier tier,
    int district,int workX,int workY,String introduction) {
    public enum Tier { CORE, RESIDENT, VISITOR }
    public NpcDefinition {
        if(id==null||!id.matches("[a-z][a-z0-9_]{1,40}")||name==null||name.isBlank())
            throw new IllegalArgumentException("Invalid NPC definition");
    }
    public static final List<NpcDefinition> INITIAL=List.of(
        new NpcDefinition("linshu","林叔","elderhuman","营地顾问",Tier.CORE,0,-2,-2,"经验丰富，务实，关心同伴安全。"),
        new NpcDefinition("xiaohe","小禾","farmerhuman","种植与食物",Tier.CORE,0,3,-4,"细心而好奇，重视食物和植物。"),
        new NpcDefinition("ashuo","阿朔","guardhuman","营地守卫",Tier.CORE,1,-7,2,"寡言但可靠，喜欢先确认同伴是否安全。"),
        new NpcDefinition("luan","鹿安","alchemisthuman","草药与照护",Tier.CORE,1,-5,4,"耐心，喜欢记录草药与共同经历。"),
        new NpcDefinition("aqiao","阿巧","blacksmithhuman","工具修造",Tier.RESIDENT,2,13,-8,"做事认真，愿意讲解工具的用途。"),
        new NpcDefinition("haitang","海棠","anglerhuman","渔人",Tier.RESIDENT,2,16,-4,"随和，留意天气和海边的变化。"),
        new NpcDefinition("mumo","木木","animalkeeperhuman","牧养",Tier.RESIDENT,2,11,-2,"温和，关心动物和营地食物。"),
        new NpcDefinition("ayue","阿越","hunterhuman","猎人与向导",Tier.RESIDENT,3,-15,-5,"谨慎，喜欢结伴探索。"),
        new NpcDefinition("shiyan","石岩","minerhuman","矿工",Tier.RESIDENT,3,-17,-9,"话不多，习惯先准备工具再出发。"),
        new NpcDefinition("sisi","思思","stylisthuman","缝补与衣饰",Tier.RESIDENT,3,-12,-2,"健谈，善于记住别人亲口说的喜好。")
    );
    public static final List<NpcDefinition> VISITORS=List.of(
        new NpcDefinition("amu","阿穆","traderhuman","旅行商人",Tier.VISITOR,0,1,4,"喜欢与人交流旅途见闻，不编造自己没有去过的地点。"),
        new NpcDefinition("zhilu","知路","explorerhuman","旅人",Tier.VISITOR,0,-3,3,"好奇而谨慎，愿意听取附近居民的真实消息。")
    );
    public static NpcDefinition find(String id){
        for(var d:INITIAL)if(d.id().equals(id))return d;
        for(var d:VISITORS)if(d.id().equals(id))return d;
        return null;
    }
}
