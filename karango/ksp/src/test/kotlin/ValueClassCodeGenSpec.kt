@file:Suppress("detekt:all")

package io.peekandpoke.karango.ksp

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.meta.testing.expectFileToMatch
import io.peekandpoke.ultra.meta.testing.kspCompileTest
import io.peekandpoke.ultra.vault.Vault
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Regression guard: a `@JvmInline value class` id property must render with the VALUE-CLASS type
 * (`append<RealmId, RealmId>`), NOT reduced to its underlying scalar (the way `Ref` is reduced to
 * `kotlin.String`). Keeping the value-class type is what makes `entity.realm EQ RealmId(...)`
 * type-check and reject a bare `String`. The value class itself gets NO generated file (it is neither
 * data/abstract/sealed, so the processor's blacklist skips it).
 */
@OptIn(ExperimentalCompilerApi::class, ExperimentalKotest::class)
class ValueClassCodeGenSpec : StringSpec() {
    init {
        "Value class id properties render with the value-class type (not the underlying scalar)" {
            kspCompileTest {
                inheritClassPath(true)

                processor(KarangoKspProcessorProvider())

                kotlin(
                    file = "VcRecord.kt",
                    contents = """
                        package karango.compile

                        import ${Vault::class.qualifiedName}

                        @JvmInline
                        value class RealmId(val value: String)

                        @Vault
                        data class VcRecord(val realm: RealmId)

                    """.trimIndent()
                )

                expectFileToMatch(
                    file = "VcRecord${"$$"}karango.kt",
                    contents = """
                        package karango.compile

                        import io.peekandpoke.karango.aql.AqlExpression
                        import io.peekandpoke.karango.aql.AqlIterableExpr
                        import io.peekandpoke.karango.aql.AqlPropertyPath
                        import io.peekandpoke.ultra.vault.lang.L1
                        import io.peekandpoke.ultra.vault.lang.L2
                        import io.peekandpoke.ultra.vault.lang.L3
                        import io.peekandpoke.ultra.vault.lang.L4
                        import io.peekandpoke.ultra.vault.lang.L5

                        //// generic property
                        inline fun <reified T> AqlIterableExpr<VcRecord>.property(name: String) = AqlPropertyPath.start(this).append<T, T>(name)

                        // realm ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        // annotations:
                        // defined as:   Primary Constructor Param
                        // defined by:   Class karango.compile.VcRecord
                        // defined at:   Line 9
                        // defined type: karango.compile.RealmId
                        // cleaned type: karango.compile.RealmId

                        inline val AqlIterableExpr<VcRecord>.realm inline get() = AqlPropertyPath.start(this).append<karango.compile.RealmId, karango.compile.RealmId>("realm")
                        inline val AqlExpression<VcRecord>.realm inline get() = AqlPropertyPath.start(this).append<karango.compile.RealmId, karango.compile.RealmId>("realm")

                        inline val AqlPropertyPath<VcRecord, VcRecord>.realm @JvmName("realm_0") inline get() = append<karango.compile.RealmId, karango.compile.RealmId>("realm")
                        inline val AqlPropertyPath<VcRecord, L1<VcRecord>>.realm @JvmName("realm_1") inline get() = append<karango.compile.RealmId, L1<karango.compile.RealmId>>("realm")
                        inline val AqlPropertyPath<VcRecord, L2<VcRecord>>.realm @JvmName("realm_2") inline get() = append<karango.compile.RealmId, L2<karango.compile.RealmId>>("realm")
                        inline val AqlPropertyPath<VcRecord, L3<VcRecord>>.realm @JvmName("realm_3") inline get() = append<karango.compile.RealmId, L3<karango.compile.RealmId>>("realm")
                        inline val AqlPropertyPath<VcRecord, L4<VcRecord>>.realm @JvmName("realm_4") inline get() = append<karango.compile.RealmId, L4<karango.compile.RealmId>>("realm")
                        inline val AqlPropertyPath<VcRecord, L5<VcRecord>>.realm @JvmName("realm_5") inline get() = append<karango.compile.RealmId, L5<karango.compile.RealmId>>("realm")
                    """.trimIndent()
                )
            }
        }
    }
}
