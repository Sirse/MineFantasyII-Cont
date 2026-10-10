package minefantasy.mf2.item.tool.crafting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minefantasy.mf2.MineFantasyII;
import minefantasy.mf2.api.heating.Quench;
import minefantasy.mf2.api.heating.TongsHelper;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.Drops;
import minefantasy.mf2.api.helpers.Sounds;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.api.tier.IToolMaterial;
import minefantasy.mf2.api.tool.ISmithTongs;
import minefantasy.mf2.item.list.CreativeTabMF;
import minefantasy.mf2.mechanics.QuenchFlare;

/**
 * @author Anonymous Productions
 */
public class ItemTongs extends ItemTool implements IToolMaterial, ISmithTongs {

    protected int itemRarity;
    private ToolMaterial material;
    private float baseDamage;
    private String name;
    // ===================================================== CUSTOM START
    // =============================================================\\
    private boolean isCustom = false;
    private float efficiencyMod = 1.0F;
    private IIcon detailTex = null;

    public ItemTongs(String name, ToolMaterial material, int rarity) {
        super(0F, material, Sets.newHashSet(new Block[] {}));
        this.material = material;
        itemRarity = rarity;
        this.name = name;
        setCreativeTab(CreativeTabMF.tabOldTools);
        setTextureName("minefantasy2:Tool/Crafting/" + name);
        this.setMaxDamage(getMaxDamage() / 5);
        GameRegistry.registerItem(this, name, MineFantasyII.MODID);
        this.setUnlocalizedName(name);
    }

    @Override
    public Multimap getItemAttributeModifiers() {
        Multimap map = HashMultimap.create();
        map.clear();

        return map;
    }

    @Override
    public ToolMaterial getMaterial() {
        return toolMaterial;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack item, World world, EntityPlayer player) {
        ItemStack held = TongsHelper.getHeldItem(item);
        if (held == null || held.stackSize <= 0 || !TongsHelper.isCoolableItem(held)) {
            return item;
        }
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(world, player, true);

        if (movingobjectposition == null) {
            return item;
        } else {
            if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                int i = movingobjectposition.blockX;
                int j = movingobjectposition.blockY;
                int k = movingobjectposition.blockZ;

                if (!world.canMineBlock(player, i, j, k)) {
                    return item;
                }

                if (!player.canPlayerEdit(i, j, k, movingobjectposition.sideHit, item)) {
                    return item;
                }

                Quench.Source source = TongsHelper.findQuench(world, i, j, k);

                if (source != null) {
                    ItemStack drop = held.copy(), cooled = drop;

                    if (TongsHelper.isCoolableItem(drop)) {
                        if (QuenchFlare.flares(drop, source)) {
                            QuenchFlare.flare(world, i, j, k, player);
                        }
                        cooled = Quench.cool(drop, source, player, world.rand);
                        cooled.stackSize = drop.stackSize;

                        Sounds.quench(player);

                        for (int a = 0; a < 5; a++) {
                            world.spawnParticle("largesmoke", i + 0.5F, j + 1, k + 0.5F, 0, 0.065F, 0);
                        }
                    }
                    if (cooled != null && !world.isRemote) {
                        if (world.isAirBlock(i, j + 1, k)) {
                            Drops.still(world, i + 0.5, j + 1, k + 0.5, cooled, 20);
                        } else {
                            player.entityDropItem(cooled, 0);
                        }
                    }

                    return TongsHelper.clearHeldItem(item, player);
                }
            }

            return item;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return true;
    }

    public ItemTongs setCustom(String s) {
        canRepair = false;
        setTextureName("minefantasy2:custom/tool/" + s + "/" + name);
        isCustom = true;
        return this;
    }

    public ItemTongs setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
        return this;
    }

    public ItemTongs setEfficiencyMod(float efficiencyMod) {
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
            detailTex = reg.registerIcon(this.getIconString() + "_detail");
        }
        super.registerIcons(reg);
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        ItemStack item = TongsHelper.getHeldItem(stack);
        boolean hasHeld = item != null;
        int baseLayer = hasHeld ? 1 : 0;

        if (hasHeld && pass == 0) {
            return item.getItem().getIcon(item, pass);
        }
        if (!isCustom || pass == baseLayer && detailTex != null) {
            return super.getIcon(stack, pass);
        }
        return detailTex != null ? detailTex : super.getIcon(stack, pass);
    }
    /*
     * @Override
     * @SideOnly(Side.CLIENT) public IIcon getIcon(ItemStack stack, int renderPass) { ItemStack item =
     * TongsHelper.getHeldItem(stack); if (renderPass == 0 && item != null) { return item.getItem().getIcon(item,
     * renderPass); } return itemIcon; }
     */

    @Override
    public int getRenderPasses(int metadata) {
        return 3;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack item, int layer) {
        ItemStack held = TongsHelper.getHeldItem(item);
        boolean hasHeld = held != null;
        int baseLayer = hasHeld ? 1 : 0;
        if (hasHeld) // Bottom Layer
        {
            if (layer == 0) {
                return held.getItem().getColorFromItemStack(held, 0);
            }
            return CustomToolHelper.getColourFromItemStack(item, layer - 1, super.getColorFromItemStack(item, layer));
        }
        return CustomToolHelper.getColourFromItemStack(item, layer, super.getColorFromItemStack(item, layer));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return CustomToolHelper.getMaxDamage(stack, super.getMaxDamage(stack));
    }

    public ItemStack construct(String main) {
        return CustomToolHelper.construct(this, main, null);
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
        return CustomToolHelper.getEfficiency(stack, super.getDigSpeed(stack, block, meta), efficiencyMod);
    }

    public float func_150893_a(ItemStack stack, Block block) {
        float base = super.func_150893_a(stack, block);
        return base <= 1.0F ? base
                : CustomToolHelper.getEfficiency(stack, this.efficiencyOnProperMaterial, efficiencyMod);
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return CustomToolHelper.getHarvestLevel(stack, super.getHarvestLevel(stack, toolClass));
    }

    @Override
    public void getSubItems(Item item, CreativeTabs tab, List list) {
        if (isCustom) {
            ArrayList<CustomMaterial> metal = CustomMaterial.getList("metal");
            Iterator iteratorMetal = metal.iterator();
            while (iteratorMetal.hasNext()) {
                CustomMaterial customMat = (CustomMaterial) iteratorMetal.next();
                if (MineFantasyII.isDebug() || customMat.getItem() != null) {
                    list.add(this.construct(customMat.name));
                }
            }
        } else {
            super.getSubItems(item, tab, list);
        }
    }

    @Override
    public void addInformation(ItemStack item, EntityPlayer user, List list, boolean extra) {
        if (isCustom) {
            CustomToolHelper.addInformation(item, list);
        } else {

            ItemStack held = TongsHelper.getHeldItem(item);
            if (held != null) {
                list.add("");
                list.add(held.getItem().getItemStackDisplayName(held));
                held.getItem().addInformation(held, user, list, extra);
            }
        }

        super.addInformation(item, user, list, extra);
    }

    @Override
    public String getItemStackDisplayName(ItemStack item) {
        String unlocalName = this.getUnlocalizedNameInefficiently(item) + ".name";
        return CustomToolHelper.getLocalisedName(item, unlocalName);
    }

    @Override
    public boolean hitEntity(ItemStack item, EntityLivingBase target, EntityLivingBase user) {
        return true;
    }
    // ====================================================== CUSTOM END
    // ==============================================================\\
}
