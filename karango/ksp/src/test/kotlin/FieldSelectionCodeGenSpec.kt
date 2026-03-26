@file:Suppress("detekt:all")

package de.kotlincook.karango.ksp

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.karango.ksp.KarangoKspProcessorProvider
import io.peekandpoke.ultra.meta.testing.expectFileToMatch
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.slumber.Slumber
import io.peekandpoke.ultra.vault.LazyRef
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class FieldSelectionCodeGenSpec : StringSpec() {
    init {
        "Ctor parameters are picked up properly" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "CtorParams.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}

                        @Vault
                        data class CtorParams(val name: kotlin.String, val age: kotlin.Int, val active: kotlin.Boolean)

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "CtorParams${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<CtorParams>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // name ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.CtorParams
                        // defined at:   Line 6
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<CtorParams>.name inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("name")
                        inline val AqlExpression<CtorParams>.name inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("name")

                        inline val AqlPropertyPath<CtorParams, CtorParams>.name @JvmName("name_0") inline get() = append<kotlin.String, kotlin.String>("name")
                        inline val AqlPropertyPath<CtorParams, L1<CtorParams>>.name @JvmName("name_1") inline get() = append<kotlin.String, L1<kotlin.String>>("name")
                        inline val AqlPropertyPath<CtorParams, L2<CtorParams>>.name @JvmName("name_2") inline get() = append<kotlin.String, L2<kotlin.String>>("name")
                        inline val AqlPropertyPath<CtorParams, L3<CtorParams>>.name @JvmName("name_3") inline get() = append<kotlin.String, L3<kotlin.String>>("name")
                        inline val AqlPropertyPath<CtorParams, L4<CtorParams>>.name @JvmName("name_4") inline get() = append<kotlin.String, L4<kotlin.String>>("name")
                        inline val AqlPropertyPath<CtorParams, L5<CtorParams>>.name @JvmName("name_5") inline get() = append<kotlin.String, L5<kotlin.String>>("name")

                        // age /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.CtorParams
                        // defined at:   Line 6
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int

                        inline val AqlIterableExpr<CtorParams>.age inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("age")
                        inline val AqlExpression<CtorParams>.age inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("age")

                        inline val AqlPropertyPath<CtorParams, CtorParams>.age @JvmName("age_0") inline get() = append<kotlin.Int, kotlin.Int>("age")
                        inline val AqlPropertyPath<CtorParams, L1<CtorParams>>.age @JvmName("age_1") inline get() = append<kotlin.Int, L1<kotlin.Int>>("age")
                        inline val AqlPropertyPath<CtorParams, L2<CtorParams>>.age @JvmName("age_2") inline get() = append<kotlin.Int, L2<kotlin.Int>>("age")
                        inline val AqlPropertyPath<CtorParams, L3<CtorParams>>.age @JvmName("age_3") inline get() = append<kotlin.Int, L3<kotlin.Int>>("age")
                        inline val AqlPropertyPath<CtorParams, L4<CtorParams>>.age @JvmName("age_4") inline get() = append<kotlin.Int, L4<kotlin.Int>>("age")
                        inline val AqlPropertyPath<CtorParams, L5<CtorParams>>.age @JvmName("age_5") inline get() = append<kotlin.Int, L5<kotlin.Int>>("age")

                        // active //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.CtorParams
                        // defined at:   Line 6
                        // defined type: kotlin.Boolean
                        // cleaned type: kotlin.Boolean

                        inline val AqlIterableExpr<CtorParams>.active inline get() = AqlPropertyPath.start(this).append<kotlin.Boolean, kotlin.Boolean>("active")
                        inline val AqlExpression<CtorParams>.active inline get() = AqlPropertyPath.start(this).append<kotlin.Boolean, kotlin.Boolean>("active")

                        inline val AqlPropertyPath<CtorParams, CtorParams>.active @JvmName("active_0") inline get() = append<kotlin.Boolean, kotlin.Boolean>("active")
                        inline val AqlPropertyPath<CtorParams, L1<CtorParams>>.active @JvmName("active_1") inline get() = append<kotlin.Boolean, L1<kotlin.Boolean>>("active")
                        inline val AqlPropertyPath<CtorParams, L2<CtorParams>>.active @JvmName("active_2") inline get() = append<kotlin.Boolean, L2<kotlin.Boolean>>("active")
                        inline val AqlPropertyPath<CtorParams, L3<CtorParams>>.active @JvmName("active_3") inline get() = append<kotlin.Boolean, L3<kotlin.Boolean>>("active")
                        inline val AqlPropertyPath<CtorParams, L4<CtorParams>>.active @JvmName("active_4") inline get() = append<kotlin.Boolean, L4<kotlin.Boolean>>("active")
                        inline val AqlPropertyPath<CtorParams, L5<CtorParams>>.active @JvmName("active_5") inline get() = append<kotlin.Boolean, L5<kotlin.Boolean>>("active")
                    """.trimIndent()
                )
            }
        }

        "Vault.Field annotated non-ctor fields get code generated" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "VaultFieldAnnotated.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}

                        @Vault
                        data class VaultFieldAnnotated(val id: kotlin.String) {
                            @Vault.Field
                            val extra: kotlin.Int = 0
                        }

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "VaultFieldAnnotated${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<VaultFieldAnnotated>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // id //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.VaultFieldAnnotated
                        // defined at:   Line 6
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<VaultFieldAnnotated>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")
                        inline val AqlExpression<VaultFieldAnnotated>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")

                        inline val AqlPropertyPath<VaultFieldAnnotated, VaultFieldAnnotated>.id @JvmName("id_0") inline get() = append<kotlin.String, kotlin.String>("id")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L1<VaultFieldAnnotated>>.id @JvmName("id_1") inline get() = append<kotlin.String, L1<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L2<VaultFieldAnnotated>>.id @JvmName("id_2") inline get() = append<kotlin.String, L2<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L3<VaultFieldAnnotated>>.id @JvmName("id_3") inline get() = append<kotlin.String, L3<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L4<VaultFieldAnnotated>>.id @JvmName("id_4") inline get() = append<kotlin.String, L4<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L5<VaultFieldAnnotated>>.id @JvmName("id_5") inline get() = append<kotlin.String, L5<kotlin.String>>("id")

                        // extra ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: io.peekandpoke.ultra.vault.Vault.Field
                        // defined as:   Property
                        // defined by:   Class karango.compile.VaultFieldAnnotated
                        // defined at:   Line 8
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int

                        inline val AqlIterableExpr<VaultFieldAnnotated>.extra inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("extra")
                        inline val AqlExpression<VaultFieldAnnotated>.extra inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("extra")

                        inline val AqlPropertyPath<VaultFieldAnnotated, VaultFieldAnnotated>.extra @JvmName("extra_0") inline get() = append<kotlin.Int, kotlin.Int>("extra")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L1<VaultFieldAnnotated>>.extra @JvmName("extra_1") inline get() = append<kotlin.Int, L1<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L2<VaultFieldAnnotated>>.extra @JvmName("extra_2") inline get() = append<kotlin.Int, L2<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L3<VaultFieldAnnotated>>.extra @JvmName("extra_3") inline get() = append<kotlin.Int, L3<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L4<VaultFieldAnnotated>>.extra @JvmName("extra_4") inline get() = append<kotlin.Int, L4<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<VaultFieldAnnotated, L5<VaultFieldAnnotated>>.extra @JvmName("extra_5") inline get() = append<kotlin.Int, L5<kotlin.Int>>("extra")
                    """.trimIndent()
                )
            }
        }

        "Slumber.Field annotated non-ctor fields get code generated" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "SlumberFieldAnnotated.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}
                        import ${Slumber::class.qualifiedName}

                        @Vault
                        data class SlumberFieldAnnotated(val id: kotlin.String) {
                            @Slumber.Field
                            val extra: kotlin.Int = 0
                        }

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "SlumberFieldAnnotated${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<SlumberFieldAnnotated>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // id //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.SlumberFieldAnnotated
                        // defined at:   Line 7
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<SlumberFieldAnnotated>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")
                        inline val AqlExpression<SlumberFieldAnnotated>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")

                        inline val AqlPropertyPath<SlumberFieldAnnotated, SlumberFieldAnnotated>.id @JvmName("id_0") inline get() = append<kotlin.String, kotlin.String>("id")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L1<SlumberFieldAnnotated>>.id @JvmName("id_1") inline get() = append<kotlin.String, L1<kotlin.String>>("id")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L2<SlumberFieldAnnotated>>.id @JvmName("id_2") inline get() = append<kotlin.String, L2<kotlin.String>>("id")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L3<SlumberFieldAnnotated>>.id @JvmName("id_3") inline get() = append<kotlin.String, L3<kotlin.String>>("id")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L4<SlumberFieldAnnotated>>.id @JvmName("id_4") inline get() = append<kotlin.String, L4<kotlin.String>>("id")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L5<SlumberFieldAnnotated>>.id @JvmName("id_5") inline get() = append<kotlin.String, L5<kotlin.String>>("id")

                        // extra ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations: io.peekandpoke.ultra.slumber.Slumber.Field
                        // defined as:   Property
                        // defined by:   Class karango.compile.SlumberFieldAnnotated
                        // defined at:   Line 9
                        // defined type: kotlin.Int
                        // cleaned type: kotlin.Int

                        inline val AqlIterableExpr<SlumberFieldAnnotated>.extra inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("extra")
                        inline val AqlExpression<SlumberFieldAnnotated>.extra inline get() = AqlPropertyPath.start(this).append<kotlin.Int, kotlin.Int>("extra")

                        inline val AqlPropertyPath<SlumberFieldAnnotated, SlumberFieldAnnotated>.extra @JvmName("extra_0") inline get() = append<kotlin.Int, kotlin.Int>("extra")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L1<SlumberFieldAnnotated>>.extra @JvmName("extra_1") inline get() = append<kotlin.Int, L1<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L2<SlumberFieldAnnotated>>.extra @JvmName("extra_2") inline get() = append<kotlin.Int, L2<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L3<SlumberFieldAnnotated>>.extra @JvmName("extra_3") inline get() = append<kotlin.Int, L3<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L4<SlumberFieldAnnotated>>.extra @JvmName("extra_4") inline get() = append<kotlin.Int, L4<kotlin.Int>>("extra")
                        inline val AqlPropertyPath<SlumberFieldAnnotated, L5<SlumberFieldAnnotated>>.extra @JvmName("extra_5") inline get() = append<kotlin.Int, L5<kotlin.Int>>("extra")
                    """.trimIndent()
                )
            }
        }

        "Vault.Ignore fields do NOT get code generated" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "VaultIgnoreField.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}

                        @Vault
                        data class VaultIgnoreField(val id: kotlin.String) {
                            @Vault.Ignore
                            @Vault.Field
                            val ignored: kotlin.Int = 0
                        }

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "VaultIgnoreField${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<VaultIgnoreField>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // id //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.VaultIgnoreField
                        // defined at:   Line 6
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<VaultIgnoreField>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")
                        inline val AqlExpression<VaultIgnoreField>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")

                        inline val AqlPropertyPath<VaultIgnoreField, VaultIgnoreField>.id @JvmName("id_0") inline get() = append<kotlin.String, kotlin.String>("id")
                        inline val AqlPropertyPath<VaultIgnoreField, L1<VaultIgnoreField>>.id @JvmName("id_1") inline get() = append<kotlin.String, L1<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultIgnoreField, L2<VaultIgnoreField>>.id @JvmName("id_2") inline get() = append<kotlin.String, L2<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultIgnoreField, L3<VaultIgnoreField>>.id @JvmName("id_3") inline get() = append<kotlin.String, L3<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultIgnoreField, L4<VaultIgnoreField>>.id @JvmName("id_4") inline get() = append<kotlin.String, L4<kotlin.String>>("id")
                        inline val AqlPropertyPath<VaultIgnoreField, L5<VaultIgnoreField>>.id @JvmName("id_5") inline get() = append<kotlin.String, L5<kotlin.String>>("id")
                    """.trimIndent()
                )
            }
        }

        "Unannotated non-ctor fields do NOT get code generated" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "UnannotatedField.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}

                        @Vault
                        data class UnannotatedField(val id: kotlin.String) {
                            val notIncluded: kotlin.Int = 0
                        }

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "UnannotatedField${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<UnannotatedField>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // id //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.UnannotatedField
                        // defined at:   Line 6
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<UnannotatedField>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")
                        inline val AqlExpression<UnannotatedField>.id inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("id")

                        inline val AqlPropertyPath<UnannotatedField, UnannotatedField>.id @JvmName("id_0") inline get() = append<kotlin.String, kotlin.String>("id")
                        inline val AqlPropertyPath<UnannotatedField, L1<UnannotatedField>>.id @JvmName("id_1") inline get() = append<kotlin.String, L1<kotlin.String>>("id")
                        inline val AqlPropertyPath<UnannotatedField, L2<UnannotatedField>>.id @JvmName("id_2") inline get() = append<kotlin.String, L2<kotlin.String>>("id")
                        inline val AqlPropertyPath<UnannotatedField, L3<UnannotatedField>>.id @JvmName("id_3") inline get() = append<kotlin.String, L3<kotlin.String>>("id")
                        inline val AqlPropertyPath<UnannotatedField, L4<UnannotatedField>>.id @JvmName("id_4") inline get() = append<kotlin.String, L4<kotlin.String>>("id")
                        inline val AqlPropertyPath<UnannotatedField, L5<UnannotatedField>>.id @JvmName("id_5") inline get() = append<kotlin.String, L5<kotlin.String>>("id")
                    """.trimIndent()
                )
            }
        }

        "Ref properties are treated as kotlin.String" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "RefProperty.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}
                        import ${Ref::class.qualifiedName}

                        data class Other(val num: kotlin.Int)

                        @Vault
                        data class RefProperty(val ref: Ref<Other>)

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "RefProperty${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<RefProperty>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // ref /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.RefProperty
                        // defined at:   Line 9
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<RefProperty>.ref inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("ref")
                        inline val AqlExpression<RefProperty>.ref inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("ref")

                        inline val AqlPropertyPath<RefProperty, RefProperty>.ref @JvmName("ref_0") inline get() = append<kotlin.String, kotlin.String>("ref")
                        inline val AqlPropertyPath<RefProperty, L1<RefProperty>>.ref @JvmName("ref_1") inline get() = append<kotlin.String, L1<kotlin.String>>("ref")
                        inline val AqlPropertyPath<RefProperty, L2<RefProperty>>.ref @JvmName("ref_2") inline get() = append<kotlin.String, L2<kotlin.String>>("ref")
                        inline val AqlPropertyPath<RefProperty, L3<RefProperty>>.ref @JvmName("ref_3") inline get() = append<kotlin.String, L3<kotlin.String>>("ref")
                        inline val AqlPropertyPath<RefProperty, L4<RefProperty>>.ref @JvmName("ref_4") inline get() = append<kotlin.String, L4<kotlin.String>>("ref")
                        inline val AqlPropertyPath<RefProperty, L5<RefProperty>>.ref @JvmName("ref_5") inline get() = append<kotlin.String, L5<kotlin.String>>("ref")
                    """.trimIndent()
                )
            }
        }

        "LazyRef properties are treated as kotlin.String" {

            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "LazyRefProperty.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}
                        import ${LazyRef::class.qualifiedName}

                        data class Another(val num: kotlin.Int)

                        @Vault
                        data class LazyRefProperty(val ref: LazyRef<Another>)

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "LazyRefProperty${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.*
                        import io.peekandpoke.karango.aql.*
                        import io.peekandpoke.ultra.vault.lang.*

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<LazyRefProperty>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // ref /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.LazyRefProperty
                        // defined at:   Line 9
                        // defined type: kotlin.String
                        // cleaned type: kotlin.String

                        inline val AqlIterableExpr<LazyRefProperty>.ref inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("ref")
                        inline val AqlExpression<LazyRefProperty>.ref inline get() = AqlPropertyPath.start(this).append<kotlin.String, kotlin.String>("ref")

                        inline val AqlPropertyPath<LazyRefProperty, LazyRefProperty>.ref @JvmName("ref_0") inline get() = append<kotlin.String, kotlin.String>("ref")
                        inline val AqlPropertyPath<LazyRefProperty, L1<LazyRefProperty>>.ref @JvmName("ref_1") inline get() = append<kotlin.String, L1<kotlin.String>>("ref")
                        inline val AqlPropertyPath<LazyRefProperty, L2<LazyRefProperty>>.ref @JvmName("ref_2") inline get() = append<kotlin.String, L2<kotlin.String>>("ref")
                        inline val AqlPropertyPath<LazyRefProperty, L3<LazyRefProperty>>.ref @JvmName("ref_3") inline get() = append<kotlin.String, L3<kotlin.String>>("ref")
                        inline val AqlPropertyPath<LazyRefProperty, L4<LazyRefProperty>>.ref @JvmName("ref_4") inline get() = append<kotlin.String, L4<kotlin.String>>("ref")
                        inline val AqlPropertyPath<LazyRefProperty, L5<LazyRefProperty>>.ref @JvmName("ref_5") inline get() = append<kotlin.String, L5<kotlin.String>>("ref")
                    """.trimIndent()
                )
            }
        }
    }
}
