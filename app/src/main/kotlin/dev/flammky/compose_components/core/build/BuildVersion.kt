package dev.flammky.compose_components.core.build

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object BuildVersion {

    // Nougat
    // 7.1
    // 25
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
    fun hasNMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    // Oreo
    // 8.0
    // 26
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun hasOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    // Oreo
    // 8.1
    // 27
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
    fun hasOMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

    // Pie
    // 9.0
    // 28
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun hasPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    // Q
    // 10.0
    // 29
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    // Red_Velvet
    // 11.0
    // 30
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    // Snow_cone
    // 12.0
    // 31
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun hasSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Snow_cone_V2
    // 12L
    // 32
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    fun hasSnowConeV2() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2

    // Nougat
    // 7.0
    // 24
    @JvmStatic
    fun isNougat() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N

    // Nougat
    // 7.1
    // 25
    @JvmStatic
    fun isNMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1

    // Oreo
    // 8.0
    // 26
    @JvmStatic
    fun isOreo() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O

    // Oreo
    // 8.1
    // 27
    @JvmStatic
    fun isOMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1

    // Pie
    // 9.0
    // 28
    @JvmStatic
    fun isPie() = Build.VERSION.SDK_INT == Build.VERSION_CODES.P

    // Q
    // 10.0
    // 29
    @JvmStatic
    fun isQ() = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q

    // Red_Velvet
    // 11.0
    // 30
    @JvmStatic
    fun isR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R

    // Snow_cone
    // 12.0
    // 31
    @JvmStatic
    fun isSnowCone() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S

    // Snow_cone_V2
    // 12L
    // 32
    @JvmStatic
    fun isSnowConeV2() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2
}