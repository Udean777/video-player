# Keep rules for libVLC (LGPL, dynamically linked via AAR).
# Wired into the release build type in Phase 6; verified on-device then since
# missing keep rules only fail at runtime.
-keep class org.videolan.libvlc.** { *; }
-keep class org.videolan.resources.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
