package com.example.processing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.addPreviousResultToClasspath
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

private const val GENERATED_SOURCES_PATH = "kotlin/com/example/generated"

@OptIn(ExperimentalCompilerApi::class)
class ProcessorTest {

    @Test
    fun `multi round test`() {
        val secondaryCompilation = createCompilation(
            """
                @ExampleAnnotation
                data class SomeSecondaryFlag(val someValue: Any)
            """.trimIndent(),
            args = mutableMapOf(
                "suffix" to "Secondary"
            )
        )

        val secondaryResult = secondaryCompilation.compile()

        assertThat(secondaryResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)

        val primaryCompilation = createCompilation(
            """
                @ExampleAnnotation
                data class SomePrimaryFlag(val someValue: Any)
            """.trimIndent(),
            args = mutableMapOf(
                "suffix" to "Primary",
                "aggregate" to "true"
            )
        ).addPreviousResultToClasspath(secondaryResult)

        val primaryPath = "${GENERATED_SOURCES_PATH}/GeneratedValues_Primary.kt"

        assertThat(
            primaryCompilation
                .kspSourcesDir
                .resolve(primaryPath)
                .exists()
        ).isFalse()

        primaryCompilation
            .compile()
            .apply {
                assertThat(exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
            }

        assertThat(
            primaryCompilation
                .kspSourcesDir
                .resolve(primaryPath)
                .exists()
        ).isTrue()

        assertThat(
            primaryCompilation
                .kspSourcesDir
                .resolve("${GENERATED_SOURCES_PATH}/GeneratedValues.kt")
                .exists()
        ).isTrue()
    }

    private fun createCompilation(
        @Language("kotlin") contents: String,
        args: MutableMap<String, String> = mutableMapOf(),
    ): KotlinCompilation = KotlinCompilation().apply {
        symbolProcessorProviders = listOf(ExampleProcessorProvider())
        inheritClassPath = true
        messageOutputStream = System.out
        kspWithCompilation = true
        kspArgs = args

        sources = listOf(
            SourceFile.kotlin(
                name = "Test.kt",
                contents = """
                            package test

                            import com.example.processing.ExampleAnnotation
                    
                            $contents
                        """.trimIndent()
            )
        )
    }

}
