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

            return Model(
                types.map { type ->

                    MType(
                        ctx,
                        type,
                        genericUsages.get(type.asClassName()),
                        type.variables.map { MVariable(ctx, it) }
                    )
                }
            )
        }
    }

}
