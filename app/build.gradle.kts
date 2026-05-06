// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}
buildscript {
    repositories {
        // Check that you have the following line (if not, add it):

        google()  // Google's Maven repository
        maven("https://jitpack.io")

    }
    dependencies {

        classpath("com.google.gms:google-services:4.4.3")
    }
}