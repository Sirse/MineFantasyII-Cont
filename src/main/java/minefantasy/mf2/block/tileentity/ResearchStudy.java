package minefantasy.mf2.block.tileentity;

import java.util.function.IntSupplier;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Study at a research table: how far it has got, on which item and by which player, and the pace it keeps. Progress
 * belongs to one item and one player, and any other starts it again; it is saved with what it belongs to, so a reload
 * carries on where it was left.
 */
public final class ResearchStudy {

    /** Study goes at most this fast: a strike sooner after the last counts for as much less. */
    public static final int STUDY_INTERVAL_TICKS = 8;
    /** How often the shelves are counted again, in ticks. */
    public static final int SHELF_RECOUNT_TICKS = 100;
    private static final long NEVER = Long.MIN_VALUE / 2;

    public float progress;
    private ItemStack studied;
    /** The player's UUID, which a change of name does not change. */
    private String researcher;
    private long lastStrike = NEVER;
    private int shelves;
    private long shelvesCountedAt = NEVER;

    /** How much a strike counts this many ticks after the last: in full once the pace allows, less if sooner. */
    public static float pace(long ticksSinceLast) {
        return Math.max(0F, Math.min(1F, ticksSinceLast / (float) STUDY_INTERVAL_TICKS));
    }

    /**
     * Starts over when the item in the slot or the player is not the one the progress was made on.
     *
     * @return whether it started over
     */
    public boolean startOverIfChanged(ItemStack item, String player) {
        if (ItemStack.areItemStacksEqual(studied, item) && player.equals(researcher)) {
            return false;
        }
        progress = 0;
        studied = item == null ? null : ItemStack.copyItemStack(item);
        researcher = player;
        return true;
    }

    /** The world time of the last strike. */
    public long lastStrike() {
        return lastStrike;
    }

    /** Whether the displayed progress belongs to this item and viewer, without starting study over. */
    public boolean belongsTo(ItemStack item, String player) {
        return player != null && player.equals(researcher) && ItemStack.areItemStacksEqual(studied, item);
    }

    /** A strike at the table at this world time, sped up by the boost; gives what it added. */
    public float strike(long now, float boost) {
        float gained = pace(now - lastStrike) * boost;
        lastStrike = now;
        progress += gained;
        return gained;
    }

    /** The bookshelves around, counted by the counter only now and then and remembered between. */
    public int shelves(long now, IntSupplier counter) {
        if (now - shelvesCountedAt >= SHELF_RECOUNT_TICKS) {
            shelves = counter.getAsInt();
            shelvesCountedAt = now;
        }
        return shelves;
    }

    public void write(NBTTagCompound nbt) {
        nbt.setFloat("progress", progress);
        if (studied != null) {
            nbt.setTag("studied", studied.writeToNBT(new NBTTagCompound()));
        }
        if (researcher != null) {
            nbt.setString("researcher", researcher);
        }
        nbt.setLong("lastStrike", lastStrike);
    }

    public void read(NBTTagCompound nbt) {
        progress = nbt.getFloat("progress");
        studied = nbt.hasKey("studied") ? ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("studied")) : null;
        researcher = nbt.hasKey("researcher") ? nbt.getString("researcher") : null;
        lastStrike = nbt.hasKey("lastStrike") ? nbt.getLong("lastStrike") : NEVER;
    }
}
