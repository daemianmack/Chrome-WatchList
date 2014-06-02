UNHIGHLIGHTABLE_PARENTS = "textarea"


function is_a_text_match(el, regex) {
    return el.nodeType == 3 && regex.test(el.nodeValue);
}

function is_highlightable(el) {
    // Node's parent is not one of UNHIGHLIGHTABLE_PARENTS.
    return $(el).parent(UNHIGHLIGHTABLE_PARENTS).length === 0;
}

function in_current_url(regex) {
     return regex.test(document.URL);
}

function is_good_match(el, regex) {
    // regex must match against a text node
    // and el must be a highlightable tag
    // and regex must not match URL.
    return (is_a_text_match(el, regex) &&
            is_highlightable(el) &&
            (! in_current_url(regex)));
}

function highlight(els, terms, className) {
    var regex = new RegExp(terms, "gi");
    var matched_count = {};
    els.contents().filter(function(_, el) {
        return is_good_match(el, regex);
    }).replaceWith(function() {

        return this.nodeValue.replace(regex, function(match) {

            key = match.toLowerCase();
            matched_count[key] = (matched_count[key] || 0) + 1;

            // Return match value with highlight class wrapper.
            return "<span class=\"" + className + "\">" + match + "</span>";
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
