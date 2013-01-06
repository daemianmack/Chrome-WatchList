function save_options() {
    var val = document.getElementById('watchlist_term_input').value;
    if (val) {
        chrome.storage.local.set({'watchlist_terms': to_machine(val)});
    } else {
        chrome.storage.local.remove('watchlist_terms');
    }

    var val = document.getElementById('watchlist_blacklist_input').value;
    if (val) {
        chrome.storage.local.set({'watchlist_blacklist': to_machine(val)});
    } else {
        chrome.storage.local.remove('watchlist_blacklist');
    }

    var status = document.getElementById('status');
    status.innerHTML = 'Options Saved.';
    setTimeout(function() {
        status.innerHTML = '';
    }, 2500);
}

function to_machine(data) {
    // .filter out "" resulting from superfluous newline.
    return data.split("\n").filter(Boolean).join("|");
}

function to_human(data) {
    return data.split("|").join("\n");
}

function retrieve_options() {
    chrome.storage.local.get('watchlist_terms', function(data) {
        if (data.watchlist_terms) {
            document.getElementById('watchlist_term_input').value = to_human(data.watchlist_terms);
        }
    });

    chrome.storage.local.get('watchlist_blacklist', function(data) {
        if (data.watchlist_blacklist) {
            document.getElementById('watchlist_blacklist_input').value = to_human(data.watchlist_blacklist);
        }
    });
}

window.onload = function() {
    retrieve_options();
    document.querySelector('#save').addEventListener('click', save_options);
}
