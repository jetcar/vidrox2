package com.jetcar.vidrox.remote.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteReleaseTest {
    @Test
    fun `remote asset name uses release tag`() {
        assertEquals("VidroX-Remote-v0.1.1.apk", remoteAssetName("v0.1.1"))
    }

    @Test
    fun `version comparison handles missing components`() {
        assertEquals(0, compareVersions("1.2", "1.2.0"))
        assertTrue(compareVersions("1.2.1", "1.2") > 0)
        assertTrue(compareVersions("1.1.9", "1.2") < 0)
    }
}
