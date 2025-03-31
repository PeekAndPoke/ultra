// noinspection JSJQueryEfficiency,ES6ConvertVarToLetConst

// Use this snippet to extract icons from https://fomantic-ui.com/elements/flag.html

// Extract as getters
console.log(
    $("table i[class$='flag']").map((idx, it) => {
        var classes = $(it).attr("class").split(" ").filter(it => it !== "flag");
        var prop = classes.map(it => it.replaceAll("-", "")).join("_")
        return `@SemanticFlagMarker inline val ${prop}: SemanticFlag\n    get() = this + "${classes.join(" ")}"`;
    }).get()
        // Distinct
        .filter((value, index, self) => self.indexOf(value) === index)
        .sort()
        .join("\n")
)

// Extract as class strings
console.log(
    $("table i[class$='flag']").map((idx, it) => {
        var classes = $(it).attr("class").split(" ").filter(it => it !== "flag");
        return `"${classes.join(" ")}"`;
    }).get()
        // Distinct
        .filter((value, index, self) => self.indexOf(value) === index)
        .sort()
        .join(", ")
)
