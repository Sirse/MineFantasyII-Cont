package minefantasy.mf2.item.tool.advanced;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.world.BlockEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.Drops;
import minefantasy.mf2.api.mining.RandomDigs;
import minefantasy.mf2.api.tier.IToolMaterial;
import minefantasy.mf2.api.tool.IMiningDrops;
import minefantasy.mf2.item.list.CreativeTabMF;
import minefantasy.mf2.mechanics.HarvestTool;

/**
 * @author Anonymous Productions
 */
public class ItemTrowMF extends ItemSpade implements IToolMaterial, IMiningDrops {

    protected int itemRarity;
    private String name;
    private float baseDamage = 1F;
    // ===================================================== CUSTOM START
    // =============================================================\\
    private boolean isCustom = false;
    private float efficiencyMod = 1.0F;
    private IIcon detailTex = null;
    private IIcon haftTex = null;

    public ItemTrowMF(String name, ToolMaterial material, int rarity) {
        super(material);
        itemRarity = rarity;
        setCreativeTab(CreativeTabMF.tabOldTools);
        this.name = name;
        setTextureName("minefantasy2:Tool/Advanced/" + name);
        GameRegistry.registerItem(this, name, MineFantasyII.MODID);
        this.setUnlocalizedName(name);
        setMaxDamage(material.getMaxUses());
    }

    @Override
    public void giveMiningDrops(World world, int x, int y, int z, Block block, int meta, EntityPlayer player,
            ItemStack held) {
        int harvestlvl = this.getMaterial().getHarvestLevel();
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, held);
        boolean silk = EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, held) > 0;
        ArrayList<ItemStack> specialdrops = RandomDigs.getDroppedItems(block, meta, harvestlvl, fortune, silk, y);
        if (specialdrops != null) {
            // The registered loot is shared: each drop is a copy
            for (ItemStack loot : specialdrops) {
                if (loot != null) Drops.fromBlock(world, x, y, z, loot.copy());
            }
        }
    }

    /** Gravel dug with a trowel always gives flint in place of its usual drops; silk touch keeps the gravel. */
    public static void digGravel(BlockEvent.HarvestDropsEvent event) {
        if (event.block != Blocks.gravel || event.harvester == null || event.isSilkTouching) return;
        // A trowel worn out by this very dig is already gone from the hand: read the tool the harvest began with
        ItemStack held = event.harvester instanceof EntityPlayerMP
                ? ((HarvestTool) ((EntityPlayerMP) event.harvester).theItemInWorldManager).mf2$harvestTool()
                : null;
        if (held == null) held = event.harvester.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemTrowMF)) return;
        event.drops.clear();
        event.drops.add(
                new ItemStack(
                        Items.flint,
                        1 + (event.fortuneLevel > 0 ? event.world.rand.nextInt(event.fortuneLevel) : 0)));
        event.dropChance = 1F;
    }

    @Override
    public ToolMaterial getMaterial() {
        return toolMaterial;
    }

    public ItemTrowMF setCustom(String s) {
        canRepair = false;
        setTextureName("minefantasy2:custom/tool/" + s + "/" + name);
        isCustom = true;
        return this;
    }

    public ItemTrowMF setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
        return this;
    }

    public ItemTrowMF setEfficiencyMod(float efficiencyMod) {
        this.efficiencyMod = efficiencyMod;
        return this;
    }

    @Override
    public Multimap getAttributeModifiers(ItemStack item) {
        Multimap map = HashMultimap.create();
        map.put(
                SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
                new AttributeModifier(field_111210_e, "Weapon modifier", getMeleeDamage(item), 0));

        return map;
    }

    /**
     * Gets a stack-sensitive value for the melee dmg
     */
    protected float getMeleeDamage(ItemStack item) {
        return baseDamage + CustomToolHelper.getMeleeDamage(item, toolMaterial.getDamageVsEntity());
    }

    protected float getWeightModifier(ItemStack stack) {
        return CustomToolHelper.getWeightModifier(stack, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg) {
        if (isCustom) {
            haftTex = reg.registerIcon(this.getIconString() + "_haft");
            detailTex = reg.registerIcon(this.getIconString() + "_detail");

        }
        super.registerIcons(reg);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return isCustom;
    }

    // Returns the number of render passes this item has.
    @Override
    public int getRenderPasses(int metadata) {
        return 3;
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        if (isCustom && pass == 1 && haftTex != null) {
            return haftTex;
        }
        if (isCustom && pass == 2 && detailTex != null) {
            return detailTex;
        }
        return super.getIcon(stack, pass);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack item, int layer) {
        return CustomToolHelper.getColourFromItemStack(item, layer, super.getColorFromItemStack(item, layer));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return CustomToolHelper.getMaxDamage(stack, super.getMaxDamage(stack)) / 2;
    }

    public ItemStack construct(String main, String haft) {
        return CustomToolHelper.construct(this, main, haft);
    }

    @Override
    public EnumRarity getRarity(ItemStack item) {
        return CustomToolHelper.getRarity(item, itemRarity);
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        if (!ForgeHooks.isToolEffective(stack, block, meta)) {
            return this.func_150893_a(stack, block);
        }
        return CustomToolHelper.getEfficiency(stack, super.getDigSpeed(stack, block, meta), efficiencyMod / 2);
    }

    public float func_150893_a(ItemStack stack, Block block) {
        return block.getMaterial() != Material.iron && block.getMaterial() != Material.anvil
                && block.getMaterial() != Material.rock ? super.func_150893_a(stack, block)
                        : CustomToolHelper.getEfficiency(stack, this.efficiencyOnProperMaterial, efficiencyMod / 2);
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return CustomToolHelper.getHarvestLevel(stack, super.getHarvestLevel(stack, toolClass));
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        if (isCustom) {
            CustomToolHelper.addCreativeMetalVariants(list, metal -> this.construct(metal, "OakWood"));
        } else {
            super.getSubItems(item, tab, list);
        }
    }

    @Override
    public void addInformation(ItemStack item, EntityPlayer user, List list, boolean extra) {
        if (isCustom) {
            CustomToolHelper.addInformation(item, list);
        }
        super.addInformation(item, user, list, extra);
    }

    @Override
    public String getItemStackDisplayName(ItemStack item) {
        String unlocalName = this.getUnlocalizedNameInefficiently(item) + ".name";
        return CustomToolHelper.getLocalisedName(item, unlocalName);
    }
    // ====================================================== CUSTOM END
    // ==============================================================\\
}
