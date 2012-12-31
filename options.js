function save_options() {
    var val = document.getElementById('watchlist_term_input').value;
    if (val) {
        chrome.storage.local.set({'watchlist_terms': val});
    } else {
        chrome.storage.local.clear();
    }

    var status = document.getElementById('status');
    status.innerHTML = 'Options Saved.';
    setTimeout(function() {
        status.innerHTML = '';
    }, 2500);
}

function retrieve_options() {
    chrome.storage.local.get('watchlist_terms', function(data) {
        if (data.watchlist) {
            document.getElementById('watchlist_term_input').value = data.watchlist;
        }
    });
}

window.onload = function() {
    retrieve_options();
//    try {
        document.querySelector('#save').addEventListener('click', save_options);
//    } catch (e) {
//        return null;
//   }
}
