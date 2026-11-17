package gio.frag.csvparsely.core.domain.di

import javax.inject.Qualifier

/**
 * Created by Giorgos Fragkis on 16-11-2026
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher


@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
