package moe.caa.multilogin.core.auth.validate.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.util.UUID

class SecureIdentityTest {
    private val onlineUUID = UUID.fromString("00515173-a5ec-5458-28c2-b09f7f08497b")

    @Test
    fun acceptsOnlyTheYggdrasilIdentity() {
        assertNull(secureIdentityMismatch(onlineUUID, onlineUUID, "Alice", "Alice"))
        assertEquals(
            SecureIdentityMismatch.UUID,
            secureIdentityMismatch(onlineUUID, UUID.randomUUID(), "Alice", "Alice")
        )
        assertEquals(
            SecureIdentityMismatch.NAME,
            secureIdentityMismatch(onlineUUID, onlineUUID, "Alice", "Alice1")
        )
    }
}
