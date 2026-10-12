package dev.farjad.letterline

import dev.farjad.letterline.domain.ButtonTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ButtonTargetTest {
    @Test
    fun roundTrips() {
        listOf(
            ButtonTarget.App("pkg/Main#0"),
            ButtonTarget.Folder("f1"),
            ButtonTarget.Shortcut(10, "com.chat", "compose", "New chat | quick"),
            ButtonTarget.Action(ButtonTarget.Kind.Search),
            ButtonTarget.Action(ButtonTarget.Kind.Lock),
            ButtonTarget.Action(ButtonTarget.Kind.Shade),
        ).forEach { assertEquals(it, ButtonTarget.parse(it.encode())) }
    }

    @Test
    fun rejectsBrokenValues() {
        assertNull(ButtonTarget.parse(null))
        assertNull(ButtonTarget.parse("widget:3"))
        assertNull(ButtonTarget.parse("action:selfdestruct"))
        assertNull(ButtonTarget.parse("shortcut:x|pkg|id|label"))
        assertNull(ButtonTarget.parse("shortcut:0||id|label"))
    }
}
