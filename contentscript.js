/*
 * Copyright (c) 2010 The Chromium Authors. All rights reserved.  Use of this
 * source code is governed by a BSD-style license that can be found in the
 * LICENSE file.
 */

chrome.extension.sendRequest({method: "getSearchable"}, function(response) {
    var searchable = response.searchable;
    if (typeof searchable !== "undefined") {
      var regex = new RegExp(searchable, "gi");
      matches = document.body.innerText.match(regex);
      if (matches) {
        var payload = {
          count: matches.length    // Pass the number of matches back.
        };
        chrome.extension.sendRequest(payload, function(response) {});
      }
    }
});
