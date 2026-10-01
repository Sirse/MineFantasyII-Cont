package minefantasy.mf2.block.tileentity;

import static minefantasy.mf2.gametest.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;

/**
 * The research table's pace: bookshelves speed study up less with each one and never past a cap, striking faster than
 * the pace allows gains nothing in the same time, and progress belongs to one item and one player and outlasts a
 * reload.
 */
@GameTestHolder("minefantasy2")
public class ResearchPaceTest {

    private ResearchPaceTest() {}

    private static final String ALICE = "00000000-0000-0000-0000-00000000000a";
    private static final String BOB = "00000000-0000-0000-0000-00000000000b";

    private static ResearchStudy begun(ItemStack item) {
        ResearchStudy study = new ResearchStudy();
        study.startOverIfChanged(item, ALICE);
        study.strike(0, 1.0F);
        return study;
    }

    @GameTest
    public static void noShelvesNoBoost(GameTestHelper helper) {
        assertEquals(1.0, TileEntityResearch.shelfBoost(0), 1e-6);
        helper.succeed();
    }

    @GameTest
    public static void eachShelfHelpsLessThanTheOneBefore(GameTestHelper helper) {
        float first = TileEntityResearch.shelfBoost(1) - TileEntityResearch.shelfBoost(0);
        float tenth = TileEntityResearch.shelfBoost(10) - TileEntityResearch.shelfBoost(9);
        assertTrue("shelves help", first > 0);
        assertTrue("the tenth helps less than the first", tenth < first);
        helper.succeed();
    }

    @GameTest
    public static void aWholeLibraryStaysUnderTheCap(GameTestHelper helper) {
        float most = 1.0F + TileEntityResearch.SHELF_MOST;
        assertTrue(TileEntityResearch.shelfBoost(1000) <= most);
        assertTrue("near the cap", TileEntityResearch.shelfBoost(1000) > most - 0.01F);
        helper.succeed();
    }

    @GameTest
    public static void strikingEveryTickGainsNoMoreThanOnceInPace(GameTestHelper helper) {
        int interval = ResearchStudy.STUDY_INTERVAL_TICKS;
        ResearchStudy hasty = begun(new ItemStack(Items.book));
        for (int tick = 1; tick <= interval; tick++) {
            hasty.strike(tick, 1.0F);
        }
        ResearchStudy paced = begun(new ItemStack(Items.book));
        paced.strike(interval, 1.0F);
        assertEquals("the same time, the same progress", paced.progress, hasty.progress, 1e-5);
        helper.succeed();
    }

    @GameTest
    public static void aSecondStrikeInTheSameTickAddsNothing(GameTestHelper helper) {
        ResearchStudy study = begun(new ItemStack(Items.book));
        study.strike(100, 1.0F);
        assertEquals(0.0, study.strike(100, 1.0F), 1e-6);
        assertEquals(0.0, study.strike(100, 1.0F), 1e-6);
        helper.succeed();
    }

    @GameTest
    public static void theFirstStrikeCountsInFull(GameTestHelper helper) {
        ResearchStudy study = new ResearchStudy();
        study.startOverIfChanged(new ItemStack(Items.book), ALICE);
        assertEquals(2.0, study.strike(5, 2.0F), 1e-6);
        helper.succeed();
    }

    @GameTest
    public static void anotherPlayerStartsOver(GameTestHelper helper) {
        ItemStack book = new ItemStack(Items.book);
        ResearchStudy study = begun(book);
        assertFalse("the same player carries on", study.startOverIfChanged(book, ALICE));
        assertTrue(study.startOverIfChanged(book, BOB));
        assertEquals(0.0, study.progress, 1e-6);
        helper.succeed();
    }

    @GameTest
    public static void anotherItemStartsOver(GameTestHelper helper) {
        ResearchStudy study = begun(new ItemStack(Items.book));
        assertTrue(study.startOverIfChanged(new ItemStack(Items.paper), ALICE));
        assertEquals(0.0, study.progress, 1e-6);
        study.strike(20, 1.0F);
        assertTrue("taken out of the slot", study.startOverIfChanged(null, ALICE));
        helper.succeed();
    }

    @GameTest
    public static void progressOutlastsAReload(GameTestHelper helper) {
        ItemStack book = new ItemStack(Items.book);
        ResearchStudy before = begun(book);
        before.strike(20, 1.0F);
        NBTTagCompound saved = new NBTTagCompound();
        before.write(saved);

        ResearchStudy after = new ResearchStudy();
        after.read(saved);
        assertFalse("the same player on the same item carries on", after.startOverIfChanged(book.copy(), ALICE));
        assertEquals(before.progress, after.progress, 1e-6);
        assertEquals("the pace carries on too", 0.0, after.strike(20, 1.0F), 1e-6);
        helper.succeed();
    }

    @GameTest
    public static void shelvesAreCountedOnlyNowAndThen(GameTestHelper helper) {
        ResearchStudy study = new ResearchStudy();
        AtomicInteger counts = new AtomicInteger();
        study.shelves(0, counts::incrementAndGet);
        study.shelves(ResearchStudy.SHELF_RECOUNT_TICKS - 1, counts::incrementAndGet);
        assertEquals(1, counts.get());
        assertEquals("counted again", 2, study.shelves(ResearchStudy.SHELF_RECOUNT_TICKS, counts::incrementAndGet));
        helper.succeed();
    }
}
