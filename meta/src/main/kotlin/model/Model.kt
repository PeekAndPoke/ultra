package de.peekandpoke.ultra.meta.model

import com.squareup.kotlinpoet.asClassName
import de.peekandpoke.ultra.meta.GenericUsages
import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.lang.model.element.TypeElement

class Model internal constructor(
    val types: List<MType>
) {

    class Builder(override val ctx: ProcessorUtils.Context) :
        ProcessorUtils {

        fun build(types: List<TypeElement>): Model {

            val genericUsages = types.fold(GenericUsages(ctx)) { acc, type -> acc.add(type) }

            fun TypeElement.toMType(): MType = MType(
                ctx,
                this,
                this.asTypeName(),
                { genericUsages.get(asClassName()) },
                variables.map { MVariable(ctx, it, it.asTypeName()) }
            )

            return Model(
                types.map { it.toMType() }
            )
        }
    }

}
