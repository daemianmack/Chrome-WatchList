function is_a_match(el, regex) {
    // Noscript tag contents present as text and behave poorly; e.g.
    // Google results for terms that happen to be
    // watchlisted.
    var bad_parents = "noscript, textarea";
    return el.nodeType == 3
        && regex.test(el.nodeValue)
        && $(el).parent(bad_parents).length == 0;
}

function highlight(els, str, className) {
    var regex = new RegExp(str, "gi");
    var matched_count = {};
    els.each(function(_, el) {
        $(el).contents().filter(function(__, el_) {
            return is_a_match(el_, regex);
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

function url_allowed(blacklist) {
    if (typeof(blacklist) === "undefined") {
        return true;
    }
    var regex = new RegExp(blacklist);
    if (regex.test(document.URL)) {
        return false;
    }
    return true;
}

$(document).ready(function() {
    chrome.storage.local.get('watchlist_terms', function(term_data) {
        if (term_data.watchlist_terms) {
            chrome.storage.local.get('watchlist_blacklist', function(blacklist_data) {
                if (url_allowed(blacklist_data.watchlist_blacklist)) {
                    var fn = function() { highlight_watchlist(term_data.watchlist_terms); };
                    setTimeout(fn, 1);
                }
            });
        }
    });
});
