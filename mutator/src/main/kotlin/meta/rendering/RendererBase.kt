package de.peekandpoke.ultra.mutator.meta.rendering

import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.annotation.processing.ProcessingEnvironment

abstract class RendererBase(
    override val logPrefix: String,
    override val processingEnv: ProcessingEnvironment
) : ProcessorUtils
