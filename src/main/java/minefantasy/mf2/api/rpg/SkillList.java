package minefantasy.mf2.api.rpg;

public class SkillList {

    // CRAFTING
    public static Skill artisanry = new Skill("artisanry").register();
    public static Skill engineering = new Skill("engineering").register();
    public static Skill construction = new Skill("construction").register();
    public static Skill provisioning = new Skill("provisioning").register();
    public static Skill combat = new Skill("combat").register();

    public static void init() {}
}
