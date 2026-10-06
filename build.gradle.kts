// Top-level build file for GitPush
// NOTE: kotlin.android is intentionally NOT applied in app/ (AGP 9 built-in Kotlin).
// The version here only puts KGP on the classpath so AGP compiles with it.
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
