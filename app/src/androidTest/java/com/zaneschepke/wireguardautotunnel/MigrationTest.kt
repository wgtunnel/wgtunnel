package com.zaneschepke.wireguardautotunnel

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zaneschepke.wireguardautotunnel.data.AppDatabase
import com.zaneschepke.wireguardautotunnel.data.Queries
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        helper.createDatabase(dbName, 6).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            execSQL(Queries.createDefaultSettings())
            execSQL(Queries.createTunnelConfig())
            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        helper.runMigrationsAndValidate(dbName, 7, true)
        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }
}
