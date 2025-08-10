package minefantasy.mf2.item;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.IFuelHandler;
import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.item.list.ComponentListMF;

public class FuelHandlerMF implements IFuelHandler {

    @Override
    public int getBurnTime(ItemStack fuel) {
        if (fuel.getItem() == null) {
            return 0;
        }
        if (fuel.getItem() == ComponentListMF.plank) {
            return (int) (200 * CustomToolHelper.getBurnModifier(fuel));
        }
        if (fuel.getItem() == ComponentListMF.coalDust) {
            return 400;
        }
        if (fuel.getItem() == ComponentListMF.coke) {
            return 2400;
        }
        return 0;
    }

}
