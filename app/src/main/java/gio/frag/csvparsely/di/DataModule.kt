package gio.frag.csvparsely.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gio.frag.csvparsely.core.data.download.CsvDownloader
import gio.frag.csvparsely.core.data.parse.CsvParser
import gio.frag.csvparsely.core.data.repository.CsvRepositoryImpl
import gio.frag.csvparsely.core.domain.di.IoDispatcher
import gio.frag.csvparsely.core.domain.repository.CsvRepository
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

/**
 * Created by Giorgos Fragkis on 18-11-2026
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val CSV_URL = "https://raw.githubusercontent.com/RabobankDev/AssignmentCSV/main/issues.csv"

    @Provides
    fun provideCsvDownloader(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CsvDownloader = CsvDownloader(
        okHttpClient = okHttpClient,
        directory = File(context.cacheDir, "csv"),
        ioDispatcher = ioDispatcher,
    )

    // One repository for the whole app, so only one load writes download.csv at a time
    @Provides
    @Singleton
    fun provideCsvRepository(
        csvDownloader: CsvDownloader,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CsvRepository = CsvRepositoryImpl(
        url = CSV_URL,
        csvDownloader = csvDownloader,
        csvParser = CsvParser(),
        ioDispatcher = ioDispatcher,
    )
}
