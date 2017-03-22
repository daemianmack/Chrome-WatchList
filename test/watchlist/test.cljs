(ns watchlist.test
  (:require [cljs.test :refer-macros [run-all-tests]]))


(defn ^:export run-tests []
  (run-all-tests #"watchlist.*-test"))

(defn ^:export perf-bench []
  (run-all-tests #"watchlist.perf-bench"))
