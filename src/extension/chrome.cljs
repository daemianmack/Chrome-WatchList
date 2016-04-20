(ns extension.chrome
  (:require [watchlist.core :as core]))

(.get js/chrome.storage.sync "watchlist"
      #(when-let [options-data (:watchlist (js->clj % :keywordize-keys true))]
         (core/init! options-data)))
