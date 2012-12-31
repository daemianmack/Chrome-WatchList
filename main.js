function highlight(els, str, className) {
    var regex = new RegExp(str, "gi");
    var matched_count = {};
    els.each(function(_, el) {
        $(el).contents().filter(function(__, el_) {
            // Noscript tag contents present as text and behave
            // poorly; e.g. Google results for terms that happen to be watchlisted.
            return el_.nodeType == 3 && regex.test(el_.nodeValue) && $(el_).parent("noscript").length == 0;
        }).replaceWith(function() {
            return (this.nodeValue || "").replace(regex, function(match) {
                key = match.toLowerCase();
                matched_count[key] = (matched_count[key] || 0) + 1;
                return "<span class=\"" + className + "\">" + match + "</span>";
            });
        });
    });
    return matched_count;
};

function get_time() {
    var now = new Date();
    return now.getTime();
}

function highlight_watchlist(terms) {
    var start_time = get_time();

    var els = $("body").find("*");
    var results = highlight(els, terms, "watchlist-highlight");

    if (Object.keys(results).length) {
        var printable = "";
        $.each(results, function(k, v) {
            printable += k +": "+ v +" ";
        });

        var stop_time = get_time();

        $.get(chrome.extension.getURL("statusbar.html"), {}, function(data) {
            $('body').append(data);
            $("#watchlist-results").text(printable);

            var diag = "elements considered: "+ els.length +", ms elapsed: "+ (stop_time - start_time) +" ";
            $(".watchlist-status-bar-item").attr("title", diag);

        }, 'html');
    };
}

$(document).ready(function(){
    chrome.storage.local.get('watchlist_terms', function(data) {
        if (data.watchlist_terms) {
            var fn = function() { highlight_watchlist(data.watchlist_terms); };
            setTimeout(fn, 1);
        }
    });
});
