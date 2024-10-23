package com.example.processing

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

@OptIn(KspExperimental::class)
class ExampleProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {

    private var round = 0

    override fun process(resolver: Resolver): List<KSAnnotated> {
        round++
        val aggregate = env.options["aggregate"].toBoolean()

        return when {
            round == 1 -> round1(resolver)
            round == 2 && aggregate -> aggregate(resolver)
            else -> emptyList()
        }
    }

    private fun aggregate(resolver: Resolver): List<KSAnnotated> {
        val (valid, invalid) = resolver.getDeclarationsFromPackage("com.example.generated")
            .filterIsInstance<KSPropertyDeclaration>()
            .filter { it.type.resolve().toClassName() == Set::class.asClassName() }
            .partition { it.validate() }

        if (valid.isEmpty()) {
            return emptyList()
        }

        FileSpec
            .builder(
                packageName = "com.example.generated",
                fileName = "GeneratedValues"
            )
            .addProperty(
                PropertySpec
                    .builder(
                        name = "allExampleSets",
                        type = setOfKClassOutAnyTypeName()
                    )
                    .initializer(
                        CodeBlock.of(
                            format = "%L",
                            valid
                                .map { it.simpleName.asString() }
                                .joinToCode(separator = " + ") { set ->
                                    CodeBlock.of("%L", set)
                                }
                        )
                    )
                    .build()
            )
            .build()
            .writeTo(
                codeGenerator = env.codeGenerator,
                aggregating = true
            )

        return invalid
    }

    private fun round1(resolver: Resolver): List<KSAnnotated> {
        val annotationName = ExampleAnnotation::class.qualifiedName ?: return emptyList()

        val (valid, invalid) = resolver
            .getSymbolsWithAnnotation(annotationName)
            .filterIsInstance<KSClassDeclaration>()
            .partition { it.validate() }

        if (valid.isEmpty()) {
            return emptyList()
        }

        val suffix = env.options["suffix"] ?: return emptyList()

        FileSpec
            .builder(
                packageName = "com.example.generated",
                fileName = "GeneratedValues_$suffix"
            )
            .addProperty(
                PropertySpec
                    .builder(
                        name = "exampleSet_$suffix",
                        type = setOfKClassOutAnyTypeName()
                    )
                    .initializer(
                        CodeBlock.of(
                            format = "%M(%L)",
                            MemberName(
                                packageName = "kotlin.collections",
                                simpleName = "setOf"
                            ),
                            valid
                                .map { it.toClassName() }
                                .joinToCode { token ->
                                    CodeBlock.of("%T::class", token)
                                }
                        )
                    )
                    .build()
            )
            .build()
            .writeTo(
                codeGenerator = env.codeGenerator,
                aggregating = true
            )

        return invalid
    }

    private fun setOfKClassOutAnyTypeName() = Set::class
        .asClassName()
        .parameterizedBy(
            KClass::class
                .asClassName()
                .parameterizedBy(
                    WildcardTypeName.producerOf(Any::class)
                )
        )
}
