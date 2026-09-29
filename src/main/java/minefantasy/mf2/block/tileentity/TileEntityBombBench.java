package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.api.crafting.engineer.IBombComponent;
import minefantasy.mf2.api.helpers.ToolHelper;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.api.recipe.CraftInventory;
import minefantasy.mf2.api.recipe.CraftPlan;
import minefantasy.mf2.api.recipe.RecipeId;
import minefantasy.mf2.api.rpg.SkillList;
import minefantasy.mf2.item.gadget.ItemBomb;
import minefantasy.mf2.item.gadget.ItemExplodingArrow;
import minefantasy.mf2.item.list.ToolListMF;
import minefantasy.mf2.knowledge.KnowledgeListMF;

public class TileEntityBombBench extends TileEntityStation implements IBasicMetre {

    public float progress;
    public float maxProgress = 25F;
    public boolean hasRecipe;
    private ItemStack[] inv = new ItemStack[6];
    private Random rand = new Random();
    private int ticksExisted;

    public static boolean isMatch(ItemStack item, String type) {
        String component = getComponentType(item);
        return component != null && component.equalsIgnoreCase(type);
    }

    public static String getComponentType(ItemStack item) {
        if (item != null && item.getItem() instanceof IBombComponent) {
            return ((IBombComponent) item.getItem()).getComponentType();
        }
        return null;
    }

    public static byte getComponentTier(ItemStack item) {
        if (item != null && item.getItem() instanceof IBombComponent) {
            return ((IBombComponent) item.getItem()).getTier();
        }
        return (byte) 0;
    }

    @Override
    public void updateEntity() {
        ++ticksExisted;
        if (worldObj.isRemote) {
            return;
        }
        sendState(false);
    }

    public boolean tryCraft(EntityPlayer user, boolean pressUsed) {
        boolean sticky = !pressUsed && ResearchLogic.hasInfoUnlocked(user, KnowledgeListMF.stickybomb)
                && user.getHeldItem() != null
                && user.getHeldItem().getItem() == Items.slime_ball;
        if (!worldObj.isRemote && sticky && applySlime()) {
            user.inventory.consumeInventoryItem(Items.slime_ball);
            return true;
        }
        ItemStack result = findResult();
        hasRecipe = result != null;

        if (result == null) {
            progress = 0F;
        } else if ((pressUsed || ToolHelper.getCrafterTool(user.getHeldItem()).equalsIgnoreCase("spanner"))) {
            if (!pressUsed && user.getHeldItem() != null) {
                worldObj.playSoundEffect(
                        xCoord + 0.5,
                        yCoord,
                        zCoord + 0.5,
                        "minefantasy2:block.twistbolt",
                        0.25F,
                        1.0F);
                user.getHeldItem().damageItem(1, user);
                if (user.getHeldItem().getItemDamage() >= user.getHeldItem().getMaxDamage()) {
                    user.destroyCurrentEquippedItem();
                }
            }
            float efficiency = pressUsed ? maxProgress : ToolHelper.getCrafterEfficiency(user.getHeldItem());

            if (!pressUsed && user.swingProgress > 0 && user.swingProgress <= 1.0) {
                efficiency *= (0.5F - user.swingProgress);
            }

            if (!worldObj.isRemote) {
                progress += efficiency;
            }
            if ((progress >= maxProgress && craftItem(result))) {
                worldObj.playSoundEffect(xCoord + 0.5, yCoord, zCoord + 0.5, "random.door_open", 0.35F, 0.5F);
                progress = 0;
                if (user != null) {
                    SkillList.engineering.addXP(user, 2);
                }
            }
            return true;
        }
        return false;
    }

    private boolean applySlime() {
        ItemStack res = getStackInSlot(4);

        if (res != null && res.getItem() instanceof ItemBomb) {
            if (res.hasTagCompound() && !res.getTagCompound().hasKey("stickyBomb")) {
                res.getTagCompound().setBoolean("stickyBomb", true);
                return true;
            }
        }
        return false;
    }

    private static final RecipeId ASSEMBLY = RecipeId.of("minefantasy2", "bomb_bench/assembly");
    private static final int OUTPUT_SLOT = 4, SPARE_SLOT = 5;

    /**
     * Takes one of each component and puts the result into the output, the components' containers into the spare slot;
     * all of it or nothing, if the output is taken by something else or a component is no longer there.
     */
    private boolean craftItem(ItemStack result) {
        boolean isArrow = isMatch(0, "arrow") || isMatch(0, "bolt");
        CraftPlan.Builder plan = CraftPlan.builder(ASSEMBLY, 0, OUTPUT_SLOT).returns(SPARE_SLOT);
        for (int slot = 0; slot < 4; slot++) {
            ItemStack part = inv[slot];
            if (part != null && !(isArrow && slot == 3)) {
                plan.consume(slot, part, 1, part, null);
            }
        }
        List<ItemStack> spill = new ArrayList<>();
        if (!plan.output(result).build().apply(CraftInventory.of(this), spill)) {
            return false;
        }
        for (ItemStack stack : spill) {
            InventorySlots.drop(worldObj, xCoord, yCoord, zCoord, stack);
        }
        return true;
    }

    public void syncData() {
        sendState(false);
    }

    private ItemStack findResult() {
        boolean isArrow = isMatch(0, "arrow") || isMatch(0, "bolt");
        if (!isMatch(1, "powder") || (!isArrow && !isMatch(3, "fuse"))) {
            return null;
        }
        byte caseTier = -1;
        byte filling = 0;
        byte fuse = -1;
        byte powder = -1;
        Item design = null;
        String com0 = getComponentType(inv[0]);
        String com1 = getComponentType(inv[1]);
        String com2 = getComponentType(inv[2]);
        String com3 = getComponentType(inv[3]);
        if (com0 != null) {
            caseTier = getComponentTier(inv[0]);
            String type = com0;
            design = getDesignCrafted(type);
        }
        if (com1 != null) {
            if (com1.equalsIgnoreCase("powder")) {
                powder = getComponentTier(inv[1]);
            } else {
                return null;
            }
        } else return null;
        if (com2 != null) {
            if (com2.equalsIgnoreCase("filling")) {
                filling = getComponentTier(inv[2]);
            } else {
                return null;
            }
        }
        if (isArrow || com3 != null) {
            if (!isArrow) {
                if (com3.equalsIgnoreCase("fuse")) {
                    fuse = getComponentTier(inv[3]);
                } else {
                    return null;
                }
            }
        } else return null;

        if (design != null && isArrow && powder > -1) {
            return ItemExplodingArrow.createBombArrow(design, powder, filling);
        } else if (design != null && fuse > -1 && powder > -1) {
            return ItemBomb.createExplosive(design, caseTier, filling, fuse, powder, 1, false);
        }
        return null;
    }

    private Item getDesignCrafted(String type) {
        if (type.equalsIgnoreCase("bombcase")) {
            return ToolListMF.bomb_custom;
        }
        if (type.equalsIgnoreCase("minecase")) {
            return ToolListMF.mine_custom;
        }
        if (type.equalsIgnoreCase("arrow")) {
            return ToolListMF.exploding_arrow;
        }
        if (type.equalsIgnoreCase("bolt")) {
            return ToolListMF.exploding_bolt;
        }
        return null;
    }

    private boolean isMatch(int slot, String type) {
        return isMatch(inv[slot], type);
    }

    // INVENORY

    @Override
    public String getInventoryName() {
        return "gui.bombcraftmf.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        if (this.isMatch(item, "bolt") || this.isMatch(item, "arrow")
                || this.isMatch(item, "bombcase")
                || this.isMatch(item, "minecase")) {
            return slot == 0;
        }
        if (this.isMatch(item, "powder")) {
            return slot == 1;
        }
        if (this.isMatch(item, "filling")) {
            return slot == 2;
        }
        if (this.isMatch(item, "fuse")) {
            return slot == 3;
        }
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        inv = InventorySlots.read(nbt, "Items", inv.length);
        progress = nbt.getFloat("progress");
        maxProgress = nbt.getFloat("maxProgress");
        hasRecipe = nbt.getBoolean("hasRecipe");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        InventorySlots.write(nbt, "Items", inv);
        nbt.setFloat("progress", progress);
        nbt.setFloat("maxProgress", maxProgress);
        nbt.setBoolean("hasRecipe", hasRecipe);
    }

    @Override
    public int getMetreScale(int size) {
        return (int) Math.min(size, size / maxProgress * progress);
    }

    @Override
    public boolean shouldShowMetre() {
        return true;
    }

    @Override
    public String getLocalisedName() {
        return StatCollector.translateToLocal("tile.bombBench.name");
    }

    @Override
    protected ItemStack[] slots() {
        return inv;
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        state.setFloat("Progress", progress);
        state.setFloat("MaxProgress", maxProgress);
        state.setBoolean("HasRecipe", hasRecipe);
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        progress = state.getFloat("Progress");
        maxProgress = state.getFloat("MaxProgress");
        hasRecipe = state.getBoolean("HasRecipe");
    }

    @Override
    public Role role(int slot) {
        return slot == OUTPUT_SLOT || slot == SPARE_SLOT ? Role.OUTPUT : Role.INPUT;
    }
}
