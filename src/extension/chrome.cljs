(ns extension.chrome
  "This being a separate namespace abstracts Chrome storage
  implementation details out of the init! fn, and hides them from the
  tests, which don't have a js/chrome object available by default"
  (:require [watchlist.core :as core]))

(.get js/chrome.storage.sync "watchlist"
      #(when-let [options-data (:watchlist (js->clj % :keywordize-keys true))]
         (core/init! options-data)))
