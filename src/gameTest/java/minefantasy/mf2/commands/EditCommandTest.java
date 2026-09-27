package minefantasy.mf2.commands;

import static minefantasy.mf2.commands.CommandPlayer.*;
import static minefantasy.mf2.gametest.Assert.*;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

import minefantasy.mf2.api.helpers.CustomToolHelper;
import minefantasy.mf2.api.helpers.ItemQuality;
import minefantasy.mf2.api.material.CustomMaterial;
import minefantasy.mf2.item.list.ComponentListMF;
import minefantasy.mf2.item.list.CustomToolListMF;
import minefantasy.mf2.item.list.ToolListMF;

/** {@code /mf edit} as the server runs it: who may, what it refuses, and what it leaves on the item. */
@GameTestHolder("minefantasy2")
public class EditCommandTest {

    private EditCommandTest() {}

    /** Two materials of the type, the first and the second the mod has. */
    private static String[] two(String type) {
        String[] found = new String[2];
        int n = 0;
        for (CustomMaterial material : CustomMaterial.materialList.values()) {
            if (type.equalsIgnoreCase(material.type) && n < 2) {
                found[n++] = material.getName();
            }
        }
        assertEquals("the mod has two materials of type " + type, 2, n);
        return found;
    }

    private static String main(ItemStack item) {
        return CustomMaterial.getMaterialFor(item, CustomToolHelper.slot_main).getName();
    }

    private static String haft(ItemStack item) {
        return CustomMaterial.getMaterialFor(item, CustomToolHelper.slot_haft).getName();
    }

    private static ItemStack sword() {
        String[] metals = two("metal");
        String[] woods = two("wood");
        return CustomToolHelper.construct(CustomToolListMF.standard_sword, metals[0], woods[0]);
    }

    // region rights and wrong use

    @GameTest
    public static void aPlayerWithoutRightsIsRefused(GameTestHelper helper) throws Exception {
        ItemStack hammer = new ItemStack(ToolListMF.hammerStone);
        CommandPlayer player = CommandPlayer.player(helper, hammer);
        assertEquals("commands.generic.permission", key(player.answer("mf edit quality 10")));
        assertFalse("the item was changed", hammer.hasTagCompound());
        helper.succeed();
    }

    @GameTest
    public static void wrongArgumentsAreAnsweredWithTheReason(GameTestHelper helper) throws Exception {
        CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(ToolListMF.hammerStone));
        assertEquals("commands.generic.num.invalid", key(player.answer("mf edit quality lots")));
        assertEquals("commands.generic.num.tooBig", key(player.answer("mf edit quality 201")));
        // 1.7.10 shows a usage error as the usage itself, in red
        assertEquals(EditCommand.USAGE, key(player.answer("mf edit quality 100 more")));
        assertEquals(EditCommand.USAGE, key(player.answer("mf edit colour red")));
        assertEquals(CommandMF.USAGE, key(player.answer("mf")));
        assertEquals("command.mf.edit.unknown_slot", key(player.answer("mf edit material Iron blade")));

        CommandPlayer emptyHanded = CommandPlayer.operator(helper, null);
        assertEquals("command.mf.edit.no_item", key(emptyHanded.answer("mf edit quality 100")));
        helper.succeed();
    }

    @GameTest
    public static void completionFollowsWhatIsTyped(GameTestHelper helper) throws Exception {
        CommandMF command = new CommandMF();
        CommandPlayer player = CommandPlayer.operator(helper, null);
        assertEquals(java.util.Arrays.asList("edit"), command.addTabCompletionOptions(player, new String[] { "e" }));
        assertEquals(
                java.util.Arrays.asList("quality"),
                command.addTabCompletionOptions(player, new String[] { "EDIT", "q" }));
        String metal = two("metal")[0];
        assertTrue(
                command.addTabCompletionOptions(player, new String[] { "edit", "material", metal.substring(0, 2) })
                        .contains(metal));
        helper.succeed();
    }

    // endregion

    // region material

    @GameTest
    public static void aWoodenPartChangesItsOwnWood(GameTestHelper helper) throws Exception {
        String[] woods = two("wood");
        ItemStack plank = ComponentListMF.plank.construct(woods[0]);
        assertEquals(woods[0], main(plank));
        CommandPlayer player = CommandPlayer.operator(helper, plank);
        IChatComponent answer = player.answer("mf edit material " + woods[1]);
        assertEquals("command.mf.edit.material", key(answer));
        assertEquals("main", args(answer)[0]);
        assertEquals("the wood of the part stayed", woods[1], main(player.getHeldItem()));
        helper.succeed();
    }

    @GameTest
    public static void aToolChangesThePartOfTheMaterialsType(GameTestHelper helper) throws Exception {
        String[] metals = two("metal");
        String[] woods = two("wood");
        CommandPlayer player = CommandPlayer.operator(helper, sword());
        assertEquals("command.mf.edit.material", key(player.answer("mf edit material " + woods[1])));
        assertEquals(metals[0], main(player.getHeldItem()));
        assertEquals(woods[1], haft(player.getHeldItem()));
        assertEquals("command.mf.edit.material", key(player.answer("mf edit material " + metals[1])));
        assertEquals(metals[1], main(player.getHeldItem()));
        assertEquals(woods[1], haft(player.getHeldItem()));
        helper.succeed();
    }

    @GameTest
    public static void aNamedPartTakesTheMaterialAsked(GameTestHelper helper) throws Exception {
        String[] woods = two("wood");
        CommandPlayer player = CommandPlayer.operator(helper, sword());
        assertEquals("command.mf.edit.material", key(player.answer("mf edit material " + woods[1] + " main")));
        assertEquals(woods[1], main(player.getHeldItem()));

        CommandPlayer plain = CommandPlayer.operator(helper, ComponentListMF.plank.construct(woods[0]));
        IChatComponent refused = plain.answer("mf edit material " + woods[1] + " haft");
        assertEquals("command.mf.edit.no_slot", key(refused));
        assertEquals(
                "the part had no haft to change",
                null,
                CustomMaterial.getMaterialFor(plain.getHeldItem(), CustomToolHelper.slot_haft));
        helper.succeed();
    }

    @GameTest
    public static void materialsTheItemCannotTakeAreRefused(GameTestHelper helper) throws Exception {
        CommandPlayer plain = CommandPlayer.operator(helper, new ItemStack(ToolListMF.hammerStone));
        assertEquals("command.mf.edit.no_materials", key(plain.answer("mf edit material " + two("metal")[0])));
        CommandPlayer smith = CommandPlayer.operator(helper, sword());
        IChatComponent unknown = smith.answer("mf edit material NoSuchMetal");
        assertEquals("command.mf.edit.unknown_material", key(unknown));
        assertEquals("NoSuchMetal", args(unknown)[0]);
        helper.succeed();
    }

    // endregion

    // region quality

    /** Whether the item is marked inferior (true), superior (false) or neither (null). */
    private static Boolean inferior(ItemStack item) {
        return item.getTagCompound().hasKey(ItemQuality.INFERIOR_KEY)
                ? item.getTagCompound().getBoolean(ItemQuality.INFERIOR_KEY)
                : null;
    }

    @GameTest
    public static void anOrdinaryQualityClearsWhatCameBefore(GameTestHelper helper) throws Exception {
        CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(ToolListMF.hammerStone));
        player.answer("mf edit quality 50");
        assertEquals(Boolean.TRUE, inferior(player.getHeldItem()));
        assertEquals("command.mf.edit.quality", key(player.answer("mf edit quality 100")));
        assertNull("the item stayed inferior", inferior(player.getHeldItem()));

        player.answer("mf edit quality 150");
        assertEquals(Boolean.FALSE, inferior(player.getHeldItem()));
        player.answer("mf edit quality 100");
        assertNull("the item stayed superior", inferior(player.getHeldItem()));
        assertEquals(100F, ItemQuality.get(player.getHeldItem()), 0F);
        helper.succeed();
    }

    @GameTest
    public static void stackingItemsHaveNoQuality(GameTestHelper helper) throws Exception {
        CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(ComponentListMF.plank, 4));
        assertEquals("command.mf.edit.stackable", key(player.answer("mf edit quality 100")));
        assertFalse(player.getHeldItem().hasTagCompound());
        helper.succeed();
    }

    // endregion

    @GameTest
    public static void unbreakableTakesOnlyTrueOrFalse(GameTestHelper helper) throws Exception {
        CommandPlayer player = CommandPlayer.operator(helper, new ItemStack(ToolListMF.hammerStone));
        assertEquals("command.mf.edit.unbreakable", key(player.answer("mf edit unbreakable true")));
        assertTrue(player.getHeldItem().getTagCompound().getBoolean("Unbreakable"));
        assertEquals("commands.generic.boolean.invalid", key(player.answer("mf edit unbreakable maybe")));
        assertEquals("command.mf.edit.breakable", key(player.answer("mf edit unbreakable false")));
        assertFalse(player.getHeldItem().getTagCompound().getBoolean("Unbreakable"));
        helper.succeed();
    }
}
