(ns watchlist.perf-test
  (:require [cljs.test :refer-macros [deftest]]
            [watchlist.nodes :as nodes]
            [common.regex :as regex]
            [common.dom :as dom]
            [cljs.pprint :refer [print-table pprint]]
            [clojure.string :refer [join]]
            [watchlist.test-helpers :as th]
            [hipo.core :as hipo]
            [watchlist.perf-data :as pd]))

(def word-pool (take 100 (shuffle pd/words)))

(defn el-multiplier [kids x-by]
  (for [n (range x-by)
        kid kids]
    [:div (take 2 word-pool) kid]))

(defn gen-dom [depth x-by]
  (loop [to-go depth
         kids [[:div (take 4 word-pool)][:div (take 4 (drop 4 word-pool))]]]
    (if (zero? to-go)
      kids
      (recur (dec to-go) (el-multiplier kids x-by)))))

;; This should join on "\n", not "|" but leaving it in place now for
;; perf comparison. It means that the :xregexp perftests don't apply
;; the correct class set to multiple-match nodes, but this likely
;; doesn't impact perf much -- it's just a lookup to a different key.
(defn gen-terms [n]
  (into {}
        (for [sq (take (* 20 n) (partition-all n 1 word-pool))]
          [(str (gensym)) (join "|" sq)])))


(def perf-scenarios
  (let [s-dom #(hipo/create [:div#sandbox (gen-dom 2 2)])
        m-dom #(hipo/create [:div#sandbox (gen-dom 4 4)])
        l-dom #(hipo/create [:div#sandbox (gen-dom 6 6)])
        s-terms (gen-terms 2)
        m-terms (gen-terms 4)
        l-terms (gen-terms 8)]
    [["sml DOM, 2x2 terms" s-dom s-terms]
     ["sml DOM, 4x4 terms" s-dom m-terms]
     ["sml DOM, 8x8 terms" s-dom l-terms]

     ["med DOM, 2x2 terms" m-dom s-terms]
     ["med DOM, 4x4 terms" m-dom m-terms]
     ["med DOM, 8x8 terms" m-dom l-terms]

     ["lrg DOM, 2x2 terms" l-dom s-terms]
     ["lrg DOM, 4x4 terms" l-dom m-terms]
     ["lrg DOM, 8x8 terms" l-dom l-terms]]))


(deftest perf-test
  (prn :legacy-perf-test)
  (print-table
   (sort-by :elapsed-ms
            (reduce
             (fn [acc [label sandbox terms]]
               (let [sandbox (sandbox)]
                 (.appendChild js/document.body sandbox)
                 (let [start (.now js/Date)]
                   (nodes/highlight-matches! :legacy terms)
                   (let [res {:label label
                              :elapsed-ms (- (.now js/Date) start)
                              :matchable-count (.-length (.getElementsByTagName js/document "div"))
                              :matched-count (.-length (.getElementsByTagName js/document "mark"))}]
                     (.removeChild js/document.body sandbox)
                     (conj acc res)))))
             []
             perf-scenarios))))


(deftest perf-test-xregexp
  (prn :perf-test-xregexp)
  (print-table
   (sort-by :elapsed-ms
            (reduce
             (fn [acc [label sandbox terms]]
               (let [sandbox (sandbox)]
                 (.appendChild js/document.body sandbox)
                 (let [terms (regex/->regex-data terms)
                       start (.now js/Date)]
                   (nodes/highlight-matches! :xregexp terms)
                   (let [res {:label label
                              :elapsed-ms (- (.now js/Date) start)
                              :matchable-count (.-length (.getElementsByTagName js/document "div"))
                              :matched-count (.-length (.getElementsByTagName js/document "mark"))}]
                     (.removeChild js/document.body sandbox)
                     (conj acc res)))))
             []
             perf-scenarios))))
