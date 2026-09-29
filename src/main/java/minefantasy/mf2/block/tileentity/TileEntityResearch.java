package minefantasy.mf2.block.tileentity;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;

import minefantasy.mf2.api.crafting.IBasicMetre;
import minefantasy.mf2.api.helpers.Sounds;
import minefantasy.mf2.api.knowledge.IArtefact;
import minefantasy.mf2.api.knowledge.InformationBase;
import minefantasy.mf2.api.knowledge.ResearchArtefacts;
import minefantasy.mf2.api.knowledge.ResearchLogic;
import minefantasy.mf2.item.list.ComponentListMF;

public class TileEntityResearch extends TileEntityStation implements IBasicMetre {

    public float progress;
    public float maxProgress;
    public int researchID = -1;
    private ItemStack[] items = new ItemStack[1];
    private Random rand = new Random();
    private int ticksExisted;

    public static ArrayList<String> getInfo(ItemStack item) {
        if (item == null) {
            return null;
        }

        return ResearchArtefacts.getResearchNames(item);
    }

    public static boolean canAccept(ItemStack item) {
        ArrayList<String> info = getInfo(item);
        return info != null && info.size() > 0;
    }

    public boolean interact(EntityPlayer user) {
        if (worldObj.isRemote) {
            return true;
        }
        ArrayList<String> research = this.getInfo(items[0]);
        int result = canResearch(user, research);

        if (research != null && research.size() > 0 && result != 0) {
            if (result == -1) {
                if (!user.worldObj.isRemote)
                    user.addChatComponentMessage(new ChatComponentTranslation("research.noskill", new Object[0]));
                return true;
            }
            maxProgress = getMaxTime();
            if (maxProgress > 0) {
                addProgress(user);

                if (progress >= maxProgress) {
                    addResearch(research, user);
                    progress = 0;
                }

                return true;
            }
        } else {
            if (result == 0) {
                if (!user.worldObj.isRemote)
                    user.addChatComponentMessage(new ChatComponentTranslation("research.null", new Object[0]));
            }
            progress = 0;
        }

        return items[0] != null;
    }

    private void addResearch(ArrayList<String> research, EntityPlayer user) {
        for (int id = 0; id < research.size(); id++) {
            InformationBase base = ResearchLogic.getResearch(research.get(id));
            if (base != null && !ResearchLogic.alreadyUsedArtefact(user, base, items[0])
                    && ResearchLogic.canPurchase(user, base)
                    && base.hasSkillsUnlocked(user)) {
                int artefacts = ResearchArtefacts.useArtefact(items[0], base, user);
                Sounds.at(this, "minefantasy2:updateResearch", 1.0F, 1.0F);
                if (!user.worldObj.isRemote) {
                    Object name = new ChatComponentTranslation("knowledge." + base.getUnlocalisedName());
                    if (artefacts == -1) {
                        user.addChatComponentMessage(new ChatComponentTranslation("research.finishResearch", name));
                    } else {
                        user.addChatComponentMessage(
                                new ChatComponentTranslation(
                                        "research.addArtefact",
                                        name,
                                        artefacts,
                                        base.getArtefactCount()));
                    }
                }
                return;
            }

        }
    }

    // 0 nothing, -1 for no skill, 1 for yes
    private int canResearch(EntityPlayer user, ArrayList<String> research) {
        if (research == null) {
            return 0;
        }
        int result = 0;

        for (int id = 0; id < research.size(); id++) {
            InformationBase base = ResearchLogic.getResearch(research.get(id));
            if (base != null && !ResearchLogic.alreadyUsedArtefact(user, base, items[0])) {
                if (ResearchLogic.canPurchase(user, base) && base.hasSkillsUnlocked(user)) {
                    return 1;
                } else if (!ResearchLogic.hasInfoUnlocked(user, base)) {
                    result = -1;
                }
            }
        }
        return result;
    }

    private float getMaxTime() {
        int t = 10;
        if (items[0] != null && items[0].getItem() instanceof IArtefact) {
            return ((IArtefact) items[0].getItem()).getStudyTime(items[0]);
        }
        return this.getInfo(items[0]) != null ? t : 0;
    }

    private void addProgress(EntityPlayer user) {
        ItemStack held = user.getHeldItem();
        if (held != null && (held.getItem() == ComponentListMF.talisman_lesser
                || held.getItem() == ComponentListMF.talisman_greater)) {
            progress = maxProgress;
            if (!user.capabilities.isCreativeMode && held.getItem() == ComponentListMF.talisman_lesser) {
                --held.stackSize;
                if (held.stackSize <= 0) {
                    user.setCurrentItemOrArmor(0, null);
                }
            }
            return;
        }
        float efficiency = 1.0F;
        if (user.swingProgress > 0) {
            efficiency *= Math.max(0F, 1.0F - user.swingProgress);
        }
        Sounds.at(this, "minefantasy2:block.flipPage", 1.0F, rand.nextFloat() * 0.4F + 0.8F);
        efficiency *= getEnvironmentBoost();
        progress += efficiency;
    }

    private float getEnvironmentBoost() {
        int books = 0;
        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {
                    if (worldObj.getBlock(xCoord + x, yCoord + y, zCoord + z) == Blocks.bookshelf) {
                        ++books;
                    }
                }
            }
        }
        return 1.0F + (0.1F * books);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        ++ticksExisted;
        syncData();
    }

    public void syncData() {
        sendState(false);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setInteger("researchID", researchID);
        nbt.setInteger("ticksExisted", ticksExisted);
        nbt.setFloat("progress", progress);
        nbt.setFloat("maxProgress", maxProgress);
        InventorySlots.write(nbt, "Items", items);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        ticksExisted = nbt.getInteger("ticksExisted");
        researchID = nbt.getInteger("researchID");
        progress = nbt.getFloat("progress");
        maxProgress = nbt.getFloat("maxProgress");
        items = InventorySlots.read(nbt, "Items", items.length);
    }

    @Override
    public String getInventoryName() {
        return "gui.research.name";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack item) {
        return canAccept(item);
    }

    @Override
    public int getMetreScale(int size) {
        if (maxProgress <= 0) {
            return 0;
        }
        return (int) Math.min(size, Math.ceil(size / maxProgress * progress));
    }

    @Override
    public boolean shouldShowMetre() {
        return maxProgress > 0;
    }

    @Override
    public String getLocalisedName() {
        return "";
    }

    @Override
    protected ItemStack[] slots() {
        return items;
    }

    @Override
    protected NBTTagCompound describe() {
        NBTTagCompound state = new NBTTagCompound();
        state.setInteger("ResearchID", researchID);
        state.setFloat("Progress", progress);
        state.setFloat("MaxProgress", Math.max(0, maxProgress));
        InventorySlots.write(state, "Items", items);
        return state;
    }

    @Override
    public void show(NBTTagCompound state) {
        researchID = state.getInteger("ResearchID");
        progress = state.getFloat("Progress");
        maxProgress = state.getFloat("MaxProgress");
        items = InventorySlots.read(state, "Items", items.length);
    }

    @Override
    public Role role(int slot) {
        return Role.WORK;
    }
}
