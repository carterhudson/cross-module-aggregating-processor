plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ksp)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.junit)
    testImplementation(libs.assertk)
    testImplementation(libs.kotlin.compile.testing.ksp)
}
