(ns watchlist.test
  (:require [cljs.test :refer-macros [run-tests run-all-tests]]
            [watchlist.core-test]))

(defn ^:export run []
  (run-all-tests #"watchlist.*-test"))
