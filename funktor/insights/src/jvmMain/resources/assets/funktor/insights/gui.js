$(() => {

    $("#content > *").hide().first().show();

    $("#menu > .item").on("click", function () {

        const $target = $(this);
        const key = $target.data("key");

        // switch active menu
        $("#menu .item").removeClass("active");
        $(`#menu .item[data-key=${key}]`).addClass("active");

        // switch active content
        $("#content > *").hide();
        $(`#content > [data-key=${key}]`).show();
    });

    // sortable tables
    $("table.sortable").tablesort();
});
