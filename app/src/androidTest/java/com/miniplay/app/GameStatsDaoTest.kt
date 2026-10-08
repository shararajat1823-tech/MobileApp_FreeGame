package com.miniplay.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.miniplay.app.data.local.MiniPlayDatabase
import com.miniplay.app.data.local.entity.GameStatsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for score persistence (requirement: "Repository: Save/load
 * scores"). Uses an in-memory Room database so it is fast and isolated.
 */
@RunWith(AndroidJUnit4::class)
class GameStatsDaoTest {

    private lateinit var db: MiniPlayDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MiniPlayDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertThenReadBack() = runTest {
        val dao = db.gameStatsDao()
        assertNull(dao.get("game2048"))

        val entity = GameStatsEntity(
            gameId = "game2048",
            bestScore = 4096,
            playCount = 3,
            wins = 1,
            totalPlayTimeMillis = 120_000,
            lastPlayedAt = 42L,
        )
        dao.upsert(entity)

        val loaded = dao.get("game2048")
        assertEquals(entity, loaded)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun upsertReplacesExisting() = runTest {
        val dao = db.gameStatsDao()
        dao.upsert(GameStatsEntity("reaction", 300, 1, 0, 1000, 1L))
        dao.upsert(GameStatsEntity("reaction", 250, 2, 0, 2000, 2L))

        val loaded = dao.get("reaction")
        assertEquals(250L, loaded?.bestScore)
        assertEquals(2, loaded?.playCount)
    }

    @Test
    fun clearRemovesEverything() = runTest {
        val dao = db.gameStatsDao()
        dao.upsert(GameStatsEntity("tictactoe", null, 5, 2, 0, 0L))
        dao.clear()
        assertEquals(0, dao.observeAll().first().size)
    }
}
