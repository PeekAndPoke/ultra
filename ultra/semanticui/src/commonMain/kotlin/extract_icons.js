// noinspection JSJQueryEfficiency,ES6ConvertVarToLetConst

// Use this snippet to extract icons from https://fomantic-ui.com/elements/icon.html#/icon

// Extract as getters
console.log(
    $("i[class$='icon']").map((idx, it) => {
        var classes = $(it).attr("class").split(" ").filter(it => it !== "icon");
        var prop = classes.map(it => it.replaceAll("-", "")).join("_")
        return `@SemanticIconMarker inline val ${prop}: SemanticIcon\n    get() = this + "${classes.join(" ")}"`;
    }).get()
        // Distinct
        .filter((value, index, self) => self.indexOf(value) === index)
        .sort()
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
