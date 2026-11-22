package com.rabindra.focustime

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rabindra.focustime.data.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorialPersistenceTest {

    @get:Rule
    val folder = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun tutorialIsUnfinishedOnFirstLaunchAndRemembersCompletion() = runBlocking {
        val file = File(folder.root, "settings-${System.nanoTime()}.preferences_pb")
        val repository = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )

        assertFalse(repository.current().tutorialCompleted)

        repository.update { it.copy(tutorialCompleted = true) }
        assertTrue(repository.settings.first().tutorialCompleted)

        repository.update { it.copy(tutorialCompleted = false) }
        assertFalse(repository.current().tutorialCompleted)

        repository.update { it.copy(tutorialCompleted = true) }
        assertTrue(repository.current().tutorialCompleted)
    }
}
