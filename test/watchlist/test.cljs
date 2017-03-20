(ns watchlist.test
  (:require [cljs.test :refer-macros [run-tests run-all-tests]]
            [watchlist.core-test]))
            [watchlist.core-test]
            [watchlist.perf-test]))

(defn ^:export run []
  (run-all-tests #"watchlist.*-test"))

;; (defn ^:export run []
;;   (run-all-tests #"watchlist.core-test"))
