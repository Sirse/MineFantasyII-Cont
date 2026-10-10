package minefantasy.mf2.mechanics;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import minefantasy.mf2.api.heating.Quench;
import minefantasy.mf2.api.heating.QuenchMedium;
import minefantasy.mf2.item.armour.ItemApron;

/**
 * Oil flares when a piece hotter than its unstable heat goes in, as quench oil does past its flash point: a burst of
 * flame scalds the smith, and sets them alight, both less through a blacksmith's apron. No block catches fire.
 */
public final class QuenchFlare {

    /** How long the smith burns. */
    public static final int BURN_SECONDS = 3;
    /** The scald of the burst itself. */
    public static final float SCALD = 3F;
    /** The scald and the burning through a blacksmith's apron. */
    public static final float APRON_SCALD = 1F;
    public static final int APRON_BURN_SECONDS = 1;

    private QuenchFlare() {}

    /** Whether quenching the hot piece in the source flares. */
    public static boolean flares(ItemStack hot, Quench.Source source) {
        return source != null && source.medium == QuenchMedium.OIL
                && hot != null
                && Quench.heatOf(hot) == Quench.Heat.OVERHEATED;
    }

    /** A burst of flame on the client; on the server the smith is scalded and set alight. */
    public static void flare(World world, int x, int y, int z, EntityPlayer smith) {
        if (world.isRemote) {
            for (int i = 0; i < 16; i++) {
                world.spawnParticle(
                        i % 4 == 0 ? "lava" : "flame",
                        x + 0.2F + world.rand.nextFloat() * 0.6F,
                        y + 0.6F,
                        z + 0.2F + world.rand.nextFloat() * 0.6F,
                        (world.rand.nextFloat() - 0.5F) * 0.1F,
                        0.1F + world.rand.nextFloat() * 0.1F,
                        (world.rand.nextFloat() - 0.5F) * 0.1F);
            }
            return;
        }
        world.playSoundEffect(x + 0.5D, y + 1D, z + 0.5D, "mob.ghast.fireball", 0.8F, 0.8F);
        if (smith == null || smith.capabilities.isCreativeMode) {
            return;
        }
        boolean apron = ItemApron.isUserProtected(smith);
        smith.attackEntityFrom(DamageSource.inFire, apron ? APRON_SCALD : SCALD);
        smith.setFire(apron ? APRON_BURN_SECONDS : BURN_SECONDS);
    }
}
