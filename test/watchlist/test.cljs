(ns watchlist.test
   (:require [cljs.test :refer-macros [run-tests run-all-tests]]))

(enable-console-print!)

(defn ^:export run []
  (run-all-tests #"watchlist.*-test")
  #_(run-tests))
