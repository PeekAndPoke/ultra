// Use this snippet to extract icons from https://fomantic-ui.com/elements/icon.html#/icon


// Extract as getters
console.log(
    $("i[class$='icon']").map((idx, it) => {
        var classes = $(it).attr("class").split(" ").filter(it => it !== "icon");
        var prop = classes.map(it => it.replaceAll("-", "")).join("_")
        return `@SemanticIconMarker val ${prop}: SemanticIcon\n    get() = this + "${classes.join(" ")}"`;
    }).get()
        // Distinct
        .filter((value, index, self) => self.indexOf(value) === index)
        .sort()
        .map((it, idx) => {
            var letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            var lettersSize = letters.length

            function toLetters(v) {
                var letter = letters[v % lettersSize]
                var rest = parseInt(v / lettersSize)

                if (rest === 0) {
                    return letter
                } else {
                    return letter + toLetters(rest)
                }
            }

            var jsName = "Fn.f" + ("" + (idx + 1)).padStart(4, '0')

            return it.replace(" get() ", ` @JsName("_${toLetters(idx)}") get() `)
        })
        .join("\n")
)

// Extract as class strings
console.log(
    $("i[class$='icon']").map((idx, it) => {
        var classes = $(it).attr("class").split(" ").filter(it => it !== "icon");
        return `"${classes.join(" ")}"`;
    }).get()
        // Distinct
        .filter((value, index, self) => self.indexOf(value) === index)
        .sort()
        .join(", ")
)
