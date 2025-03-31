window.$ = window.$ || function () {
}

$(() => {
    $(".insights-bar-placeholder").each((idx, placeholder) => {

        const $placeholder = $(placeholder);

        const filename = encodeURIComponent($placeholder.data("insights-filename"));

        let triesLeft = 5

        function tryToLoad() {

            $.ajax(`/_/insights/bar/${filename}`)
                .then(result => {
                    $placeholder.replaceWith($(result));

                    $('#close-insights-bar').on("click", () => $('#insights-bar').remove())
                })
                .catch(error => {
                    if (--triesLeft > 0) {
                        setTimeout(tryToLoad, 500)
                    } else {
                        console.error("Could not load insights data", error)
                    }
                })
        }

        tryToLoad()
    });
});
