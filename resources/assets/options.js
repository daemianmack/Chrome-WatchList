function save_options() {
    var terms     = document.getElementById('watchlist_term_input').value;
    var blacklist = document.getElementById('watchlist_blacklist_input').value;

    chrome.storage.sync.set({'watchlist': {'terms':     to_machine(terms),
                                           'blacklist': to_machine(blacklist)}});
    
    
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
    chrome.storage.sync.get('watchlist', function(data) {
        if (data.watchlist.terms) {
            document.getElementById('watchlist_term_input').value = to_human(data.watchlist.terms);
        }
        if (data.watchlist.blacklist) {
            document.getElementById('watchlist_blacklist_input').value = to_human(data.watchlist.blacklist);
        }
    });
 
}

window.onload = function() {
    retrieve_options();
    document.getElementById('save').onclick = save_options;
}
