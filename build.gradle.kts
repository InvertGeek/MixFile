// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
buildscript {
    repositories {
        // Check that you have the following line (if not, add it):

        google()  // Google's Maven repository
        maven("https://jitpack.io")

    }
    dependencies {


        classpath(libs.google.services)

    }
}