package de.peekandpoke.ultra.common.markup.images

/**
 * Helper function for building [ImageSizes]
 */
fun imagesSizes(builder: ImageSizes.Builder.() -> Unit) = ImageSizes.Builder().apply(builder).build()

/**
 * Represent multiple sizes for an image
 *
 * With [defaultSize] being the default image size.
 * With [sizes] being device/resolution-dependent image sizes
 */
data class ImageSizes(val defaultSize: CssSize, val sizes: List<Entry>) {

    companion object {
        /** Below is mobile, above is desktop */
        const val mobileBreakpoint = 900

        /** Device sizes */
        val breakpoints = listOf(
            320,
            512,
            640,
            1024,
            1440,
            2048
        )

        /** Default sizes */
        val default = imagesSizes {
            defaultSize = 100.vw

            breakpoints.forEach { max(it, it.px) }
        }

        val mobile100desktop50 = imagesSizes { mobile(100.vw).desktop(50.vw) }
    }

    /**
     * Represents a css size with a [value] and a [unit]
     *
     * Examples:
     *  - 50px
     *  - 100vh
     */
    data class CssSize(val value: Float, val unit: CssUnit) {

        override fun toString(): String {

            val asInt = value.toInt()

            return when (value - asInt < 0.0001) {
                true -> "$asInt$unit"
                else -> "$value$unit"
            }
        }
    }

    /**
     * Enumerates possible css units
     */
    enum class CssUnit {
        EM,
        PX,
        PT,
        CM,
        IN,
        VW,
        VH;

        /** Renders the [CssUnit] as lower case string */
        override fun toString() = super.toString().toLowerCase()
    }


    /**
     * Builder for [ImageSizes]
     */
    class Builder {
        /** default image size */
        var defaultSize = 100.vw

        /** builder entries */
        private val entries = mutableListOf<Entry>()

        /**
         * Creates the [ImageSizes]
         */
        fun build() = ImageSizes(defaultSize, entries)

        /**
         * Converts a number to a [CssSize] with unit em
         */
        val Number.em get() = CssSize(toFloat(), CssUnit.EM)

        /**
         * Converts a number to a [CssSize] with unit px
         */
        val Number.px get() = CssSize(toFloat(), CssUnit.PX)

        /**
         * Converts a number to a [CssSize] with unit pt
         */
        val Number.pt get() = CssSize(toFloat(), CssUnit.PT)

        /**
         * Converts a number to a [CssSize] with unit cm
         */
        val Number.cm get() = CssSize(toFloat(), CssUnit.CM)

        /**
         * Converts a number to a [CssSize] with unit inch
         */
        val Number.inch get() = CssSize(toFloat(), CssUnit.IN)

        /**
         * Converts a number to a [CssSize] with unit vw
         */
        val Number.vw get() = CssSize(toFloat(), CssUnit.VW)

        /**
         * Converts a number to a [CssSize] with unit vh
         */
        val Number.vh get() = CssSize(toFloat(), CssUnit.VH)

        /**
         * Adds a min width entry -> "(min-width: [px]px) [size]"
         */
        fun min(px: Int, size: CssSize) = apply {
            entries.add(
                Entry.MinWidth(px, size)
            )
        }

        /**
         * Adds a max width entry -> "(min-width: [px]px) [size]"
         */
        fun max(px: Int, size: CssSize) = apply {
            entries.add(
                Entry.MaxWidth(px, size)
            )
        }

        /**
         * Adds a max width entry for all [breakpoints] below the [mobileBreakpoint]
         */
        fun mobile(size: CssSize) = apply {
            breakpoints.forEach {
                if (it < mobileBreakpoint) {
                    max(it, size)
                }
            }
        }

        /**
         * Adds a max width entry for all [breakpoints] above the [mobileBreakpoint]
         */
        fun desktop(size: CssSize) = apply {
            breakpoints.forEach {
                if (it > mobileBreakpoint) {
                    max(it, size)
                }
            }
        }

        /**
         * Adds [mobile] and [desktop] entries
         */
        fun responsive(mobile: CssSize, desktop: CssSize) {
            mobile(mobile)
            desktop(desktop)
        }
    }

    /**
     * Entry
     */
    sealed class Entry {
        /** Width in px */
        abstract val px: Int

        /** Renders the entry as string */
        abstract fun render(): String

        /** Min width entry */
        data class MinWidth(override val px: Int, val size: CssSize) : Entry() {
            override fun render() = "(min-width: ${px}px) $size"
        }

        /** Max width entry */
        data class MaxWidth(override val px: Int, val size: CssSize) : Entry() {
            override fun render() = "(max-width: ${px}px) $size"
        }
    }

    /**
     * Renders the whole [ImageSizes] as a string that is understood by the browser in the img::sizes attributes
     */
    fun render() = when (sizes.isEmpty()) {
        true -> default.toString()
        false -> sizes.joinToString(", ") { it.render() }
    }
}
