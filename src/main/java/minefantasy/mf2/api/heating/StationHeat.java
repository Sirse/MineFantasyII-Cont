package minefantasy.mf2.api.heating;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Keeps air and source-produced heat separate, so changing weather cannot manufacture useful heat. Work retains the
 * historical zero-based processing scale; it is not a temperature difference from air. Import/preview methods alone
 * handle writes to legacy public station temperature fields.
 */
public final class StationHeat {

    private boolean initialized;
    private boolean changed;
    private float air, work, displayed;

    /** Compatibility boundary for writes to legacy public temperature fields. */
    public void importLegacyTemperature(float temperature, float ambient, boolean supplied) {
        boolean previousInitialized = initialized;
        float previousAir = air, previousWork = work, previousDisplayed = displayed;
        if (!initialized) {
            work = supplied ? sourceValue(temperature, ambient) : 0F;
            air = work > 0 || temperature == 0 ? ambient : temperature;
            initialized = true;
        } else if (temperature != displayed) {
            // Public station temperature fields are also written by integrations and test fixtures.
            if (supplied) work = sourceValue(temperature, air);
            else {
                work = 0F;
                air = temperature;
            }
        }
        displayed = temperature;
        changed |= previousInitialized != initialized || previousAir != air
                || previousWork != work
                || previousDisplayed != displayed;
    }

    /** Drain after the station has completed an update, including its early-return paths. */
    public boolean consumeChanged() {
        boolean result = changed;
        changed = false;
        return result;
    }

    /** Read-only, including before a new or legacy tile's first tick. */
    public float previewLegacyWork(float temperature, float ambient, boolean supplied) {
        if (!initialized) return supplied ? sourceValue(temperature, ambient) : 0F;
        if (temperature != displayed) return supplied ? sourceValue(temperature, air) : 0F;
        return work;
    }

    private static float sourceValue(float temperature, float air) {
        return Float.isFinite(temperature) && temperature > air ? Math.max(0F, temperature) : 0F;
    }

    public float work() {
        return work;
    }

    /** Follow a permitted source without turning background air into work. */
    public void followSource(float sourceHeat) {
        setWork(sourceHeat);
    }

    public void approachFuelHeat(float target, float step) {
        setWork(AmbientTemperature.approach(work, Math.max(0F, target), step));
    }

    public void cool(float amount) {
        setWork(work - Math.max(0F, amount));
    }

    public void boost(float amount, float limit) {
        if (work < limit) setWork(Math.min(limit, work + Math.max(0F, amount)));
    }

    /** Transfer conserves source-produced heat and cannot reverse the temperature ordering. */
    public void transferTo(StationHeat receiver, float maximum) {
        if (receiver == this || work <= receiver.work) return;
        float amount = Math.min(Math.max(0F, maximum), (work - receiver.work) / 2F);
        setWork(work - amount);
        receiver.setWork(receiver.work + amount);
    }

    private void setWork(float value) {
        float next = Float.isFinite(value) ? Math.max(0F, value) : 0F;
        changed |= next != work;
        work = next;
    }

    public void approachAir(float ambient, float step) {
        float next = AmbientTemperature.approach(air, ambient, step);
        changed |= next != air;
        air = next;
    }

    public float temperature() {
        float next = work > 0 ? Math.max(air, work) : air;
        changed |= next != displayed;
        displayed = next;
        return displayed;
    }

    public void write(NBTTagCompound tag) {
        NBTTagCompound state = new NBTTagCompound();
        state.setBoolean("Initialized", initialized);
        state.setFloat("Air", air);
        state.setFloat("Work", work);
        state.setFloat("Displayed", displayed);
        tag.setTag("SourceHeat", state);
    }

    public void read(NBTTagCompound tag, float oldTemperature) {
        changed = false;
        initialized = tag.hasKey("SourceHeat", 10);
        if (!initialized) {
            // Legacy saves do not prove where stored warmth came from. Keep the temperature, but not work credit.
            initialized = true;
            displayed = Float.isFinite(oldTemperature) ? oldTemperature : 0F;
            air = Math.max(-100F, Math.min(100F, displayed));
            work = 0F;
            return;
        }
        NBTTagCompound state = tag.getCompoundTag("SourceHeat");
        initialized = !state.hasKey("Initialized") || state.getBoolean("Initialized");
        air = state.getFloat("Air");
        displayed = state.getFloat("Displayed");
        setWork(state.getFloat("Work"));
        if (!Float.isFinite(air) || !Float.isFinite(displayed)) initialized = false;
        changed = false;
    }
}
