package de.peekandpoke.ultra.meta

import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Completion
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

@Suppress("RedundantOverride")
abstract class KotlinProcessor(private val logPrefix: String) : AbstractProcessor(), ProcessorUtils {

    override fun getSupportedOptions(): Set<String> = super.getSupportedOptions()
    override fun getSupportedSourceVersion(): SourceVersion = super.getSupportedSourceVersion()
    override fun getSupportedAnnotationTypes(): Set<String> = super.getSupportedAnnotationTypes()
    override fun init(processingEnv: ProcessingEnvironment) = super.init(processingEnv)

    override fun getCompletions(
        element: Element?,
        annotation: AnnotationMirror?,
        member: ExecutableElement?,
        userText: String?
    ): Iterable<Completion> =
        super.getCompletions(element, annotation, member, userText)

    abstract override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean

    /**
     * Get the context
     *
     * NOTICE: this MUST be a getter, otherwise super.processingEnv is not initialized
     */
    override val ctx get() = ProcessorUtils.Context(logPrefix, super.processingEnv)

    /**
     * Returns the directory where generated Kotlin sources should be placed in order to be compiled.
     *
     * If `null`, then this processor is probably not being run through kapt.
     */
    val generatedDir: File? get() = options[KAPT_KOTLIN_GENERATED_OPTION]?.let(::File)
}
