package dev.mudrock.gunes.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = if (languageCode == "zh") {
            Locale.SIMPLIFIED_CHINESE
        } else {
            Locale.forLanguageTag(languageCode)
        }
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)
        return context.createConfigurationContext(configuration)
    }

    fun applyLocaleToContext(context: Context, languageCode: String): Context {
        val locale = if (languageCode == "zh") {
            Locale.SIMPLIFIED_CHINESE
        } else {
            Locale.forLanguageTag(languageCode)
        }
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    fun getCurrentLocale(context: Context): Locale {
        return context.resources.configuration.locales.get(0)
    }
}