package dev.mudrock.gunes.util

import java.util.Date
import kotlin.math.*

object SunCalc {
    private const val PI = Math.PI
    private const val RAD = PI / 180.0
    private const val DAY_MS = 1000.0 * 60 * 60 * 24
    private const val J1970 = 2440588.0
    private const val J2000 = 2451545.0
    private const val J0 = 0.0009

    private fun toJulian(date: Long): Double {
        return date / DAY_MS - 0.5 + J1970
    }

    private fun toDays(date: Long): Double {
        return toJulian(date) - J2000
    }

    private fun rightAscension(l: Double, b: Double): Double {
        return atan2(sin(l) * cos(23.4397 * RAD) - tan(b) * sin(23.4397 * RAD), cos(l))
    }

    private fun declination(l: Double, b: Double): Double {
        return asin(sin(b) * cos(23.4397 * RAD) + cos(b) * sin(23.4397 * RAD) * sin(l))
    }

    private fun azimuth(H: Double, phi: Double, dec: Double): Double {
        return atan2(sin(H), cos(H) * sin(phi) - tan(dec) * cos(phi))
    }

    private fun altitude(H: Double, phi: Double, dec: Double): Double {
        return asin(sin(phi) * sin(dec) + cos(phi) * cos(dec) * cos(H))
    }

    private fun siderealTime(d: Double, lw: Double): Double {
        return (280.16 + 360.9856235 * d) - lw
    }

    private fun astroRefraction(h: Double): Double {
        var hRef = h
        if (hRef < 0) hRef = 0.0
        return 0.0002967 / tan(hRef + 0.00312536 / (hRef + 0.08901179))
    }

    private fun solarMeanAnomaly(d: Double): Double {
        return (357.5291 + 0.98560028 * d) * RAD
    }

    private fun eclipticLongitude(M: Double): Double {
        val C = (1.9148 * sin(M) + 0.02 * sin(2 * M) + 0.0003 * sin(3 * M)) * RAD
        val P = 102.9372 * RAD
        return M + C + P + PI
    }

    private fun solarTransitJ(ds: Double, M: Double, L: Double): Double {
        return J2000 + ds + 0.0053 * sin(M) - 0.0069 * sin(2 * L)
    }

    private fun hourAngle(h: Double, phi: Double, dec: Double): Double {
        return acos((sin(h) - sin(phi) * sin(dec)) / (cos(phi) * cos(dec)))
    }

    private fun observerAngle(height: Double): Double {
        return -2.076 * sqrt(height) / 60.0
    }

    private fun getSetJ(h: Double, lw: Double, phi: Double, dec: Double, n: Double, M: Double, L: Double): Double {
        val w = hourAngle(h, phi, dec)
        val a = approxTransit(w, lw, n)
        return solarTransitJ(a, M, L)
    }

    private fun julianCycle(d: Double, lw: Double): Double {
        return (d - J0 - lw / (2 * PI)).roundToInt().toDouble()
    }

    private fun approxTransit(ht: Double, lw: Double, n: Double): Double {
        return J0 + (ht + lw) / (2 * PI) + n
    }

    fun getTimes(date: Date, lat: Double, lng: Double): SunTimes {
        val lw = -lng * RAD
        val phi = lat * RAD
        val d = toDays(date.time)
        val n = julianCycle(d, lw)
        val ds = approxTransit(0.0, lw, n)
        val M = solarMeanAnomaly(ds)
        val L = eclipticLongitude(M)
        val dec = declination(L, 0.0)
        val Jnoon = solarTransitJ(ds, M, L)

        val h0 = -0.833 * RAD // default
        // val hOffset = observerAngle(height) * RAD // height correction if needed, not used in basic suncalc for standard sunrise/set

        // Sunrise and Sunset
        val Jset = getSetJ(h0, lw, phi, dec, n, M, L)
        val Jrise = Jnoon - (Jset - Jnoon)

        return SunTimes(
            sunrise = getDateFromJulian(Jrise),
            sunset = getDateFromJulian(Jset),
            date = date
        )
    }

    private fun getDateFromJulian(j: Double): Date {
        return Date(((j + 0.5 - J1970) * DAY_MS).toLong())
    }
}
