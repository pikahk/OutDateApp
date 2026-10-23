package ru.pikahk.outdateapp.ui.family

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pikahk.outdateapp.data.remote.ApiException

class FamilyUiTest {

    @Test
    fun `код приводится к шести заглавным символам без пробелов`() {
        assertEquals("K7PQX4", normalizeInviteCode("k7p qx4"))
        assertEquals("K7PQX4", normalizeInviteCode("K7P-QX4-ZZ"))
    }

    @Test
    fun `код показывается двумя группами`() {
        assertEquals("K7P QX4", formatInviteCode("K7PQX4"))
    }

    @Test
    fun `ошибки сервера превращаются в понятные сообщения`() {
        assertEquals(FamilyError.INVALID_CODE, familyError("invalid_code"))
        assertEquals(FamilyError.ALREADY_IN_FAMILY, familyError("already_in_family"))
        assertEquals(FamilyError.NETWORK, familyError(ApiException.NETWORK))
        assertEquals(FamilyError.SERVER, familyError("something_new"))
    }
}
