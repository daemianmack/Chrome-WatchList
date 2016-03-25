function is_a_text_node(el) {
    return el.nodeType == 3;
}

function highlight(els, terms, className) {
    var matched_count = {};
    var regex = new RegExp(terms, "gi");
    var url = document.URL.toLowerCase();
    els.contents().filter(function(_, el) {
        return is_a_text_node(el);
    }).each(function(_, el) {
        // .exec on global regexes mutates a lastIndex pointer.
        // Walk through match-results, don't use `regex` "out-of-band".
        while ((result = regex.exec(el.nodeValue)) !== null) {
            matched = result[0].toLowerCase();
            if (! url.match(matched)) {
                $(el).parent().addClass("watchlist-highlight");
                matched_count[matched] = (matched_count[matched] || 0) + 1;
            }
        }
    });
    return matched_count;
}

function get_time() {
    var now = new Date();
    return now.getTime();
}

function perform_dramatic_statusbar_reveal() {
    setTimeout(function() {
        $('#watchlist-status-bar').removeClass('loading');
        $('#watchlist-status-bar').addClass('loaded');
    }, 1);
}

function highlight_watchlist(elapsed, terms) {
    var start_time = get_time();
    var els = $("body").find("*:not(iframe, noscript, script, textarea)");
    var results = highlight(els, terms, "watchlist-highlight");

    if (Object.keys(results).length) {
        var printable = "";
        $.each(results, function(k, v) {
            printable += k +": "+ v +" ";
        });

        $.get(chrome.extension.getURL("statusbar.html"), {}, function(data) {
            $('body').append(data);

            $("#watchlist-results").text(printable);
            perform_dramatic_statusbar_reveal();

            var diag = els.length + " elements considered, " + (get_time() - start_time + elapsed) + " ms elapsed";
            $(".watchlist-status-bar-item").attr("title", diag);
            console.log(diag);
        }, 'html');
    };
}

function url_allowed(blacklist) {
    if (typeof(blacklist) === "undefined") {
        return true;
    }
    var regex = new RegExp(blacklist);
    return !regex.test(document.URL);
}

$(document).ready(function() {
    var start_time = get_time();
    chrome.storage.sync.get('watchlist_terms', function(term_data) {
        if (term_data.watchlist_terms) {
            chrome.storage.sync.get('watchlist_blacklist', function(blacklist_data) {
                if (url_allowed(blacklist_data.watchlist_blacklist)) {
                    var elapsed = get_time() - start_time;
                    var fn = function() { highlight_watchlist(elapsed, term_data.watchlist_terms); };
                    setTimeout(fn, 1);
                }
            });
        }
    });
});
