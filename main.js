function is_a_match(el, regex) {
    return el.nodeType == 3 && regex.test(el.nodeValue);
}

function is_highlightable(el) {
    var unhighlightable_parents = "textarea";
    var r_unhighlightable_parents = new RegExp(unhighlightable_parents, "i");
    return r_unhighlightable_parents.test(el.tagName) === false
        && $(el).parent(unhighlightable_parents).length === 0;
}

function in_current_url(term) {
    var r = new RegExp(term, "gi");
    if (r.test(document.URL)) {
        return true;
    }
}

// Preserve markup that may have been entered into, say, a textarea.
function escape_markup(text) {
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function highlight(els, str, className) {
    var regex = new RegExp(str, "gi");
    var matched_count = {};
    els.each(function(_, el) {
        $(el).contents().filter(function(__, el_) {
            return is_a_match(el_, regex);
        }).replaceWith(function() {

            var nodeValue = escape_markup(this.nodeValue)
            return nodeValue.replace(regex, function(match) {

                // Return unchanged -- without highlighting or counting.
                if (in_current_url(match)) {
                    return match;
                }

                key = match.toLowerCase();
                matched_count[key] = (matched_count[key] || 0) + 1;

                if (is_highlightable(el)) {
                    // Return with highlighting.
                    return "<span class=\"" + className + "\">" + match + "</span>";
                }

                // Return without highlighting.
                return match;

            });
        });
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

function highlight_watchlist(terms) {
    var start_time = get_time();

    var els = $("body").find("*:not(iframe, noscript, script)");
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
            perform_dramatic_statusbar_reveal();

            var diag = els.length + " elements considered, " + (stop_time - start_time) + " ms elapsed";
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
    chrome.storage.sync.get('watchlist_terms', function(term_data) {
        if (term_data.watchlist_terms) {
            chrome.storage.sync.get('watchlist_blacklist', function(blacklist_data) {
                if (url_allowed(blacklist_data.watchlist_blacklist)) {
                    var fn = function() { highlight_watchlist(term_data.watchlist_terms); };
                    setTimeout(fn, 1);
                }
            });
        }
    });
});
