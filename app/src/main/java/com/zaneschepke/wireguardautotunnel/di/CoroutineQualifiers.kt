package com.zaneschepke.wireguardautotunnel.di

import javax.inject.Qualifier

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class DefaultDispatcher

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class MainImmediateDispatcher

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class ApplicationScope

@Retention(AnnotationRetention.BINARY) @Qualifier annotation class ServiceScope
