package com.diaryomi.di

import android.content.Context
import com.diaryomi.data.local.DiaryomiDatabase
import com.diaryomi.data.local.SearchResultDao
import com.diaryomi.data.local.TrackedSearchDao
import com.diaryomi.data.repository.SearchRepositoryImpl
import com.diaryomi.domain.repository.SearchRepository
import com.diaryomi.extension.GazetteExtension
import com.diaryomi.extension.govfederal.GovFederalExtension
import com.diaryomi.extension.tcern.TceRnExtension
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

class InMemoryCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val current = cookieStore.getOrPut(host) { mutableListOf() }
        synchronized(current) {
            cookies.forEach { newCookie ->
                current.removeAll { it.name == newCookie.name }
                current.add(newCookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val current = cookieStore[url.host] ?: return emptyList()
        synchronized(current) {
            val now = System.currentTimeMillis()
            current.removeAll { it.expiresAt < now }
            return current.toList()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DiaryomiDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            DiaryomiDatabase::class.java,
            "diaryomi_db"
        ).fallbackToDestructiveMigration().build()
    }
    
    @Provides
    fun provideTrackedSearchDao(db: DiaryomiDatabase): TrackedSearchDao = db.trackedSearchDao()
    
    @Provides
    fun provideSearchResultDao(db: DiaryomiDatabase): SearchResultDao = db.searchResultDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(InMemoryCookieJar())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtensionModule {
    @Binds
    @IntoSet
    abstract fun bindGovFederalExtension(impl: GovFederalExtension): GazetteExtension
    
    @Binds
    @IntoSet
    abstract fun bindTceRnExtension(impl: TceRnExtension): GazetteExtension
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
