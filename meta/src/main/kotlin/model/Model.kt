package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.ClassName
import de.peekandpoke.ultra.meta.GenericUsages
import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.lang.model.element.TypeElement

fun ProcessorUtils.model(types: List<TypeElement>) = Model(ctx, types)

class Model(
    override val ctx: ProcessorUtils.Context,
    types: List<TypeElement>
) : ProcessorUtils, Iterable<MType> {

    val types = types.map { it.toMType() }

    private val genericUsages = GenericUsages(this, types)

    fun getGenericUsages(className: ClassName) = genericUsages.get(className)

    private fun TypeElement.toMType(): MType = MType(
        this@Model,
        this,
        this.asTypeName()
    )

    fun getDirectChildTypes(parent: MType): List<MType> {
        return types.filter {
            it.directSuperTypes.any { superType -> superType == parent }
        }
    }

    override fun iterator(): Iterator<MType> = types.iterator()
}

