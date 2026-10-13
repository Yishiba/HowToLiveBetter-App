package app.lifeguide.core.corpus

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FilterTest {
    private val anyone = Entry("1", "1", "人人适用")
    private val pregnant = Entry("1", "2", "孕期", who = listOf(listOf("怀孕")))
    private val over35 = Entry("1", "3", "35 岁以上", who = listOf(listOf("35岁")))

    @Test
    fun profileHidesTaggedEntriesUntilTheyMatch() {
        assertTrue(Filter.fitsProfile(anyone, emptySet()))
        assertFalse(Filter.fitsProfile(pregnant, emptySet()), "没填情况时，带人群标签的不进清单")
        assertFalse(Filter.fitsProfile(pregnant, setOf("老人")))
        assertTrue(Filter.fitsProfile(pregnant, setOf("怀孕")))
        assertTrue(Filter.fitsProfile(over35, setOf("60岁")), "60 岁蕴含 35 岁，同网页 fitsMe")
    }
}
