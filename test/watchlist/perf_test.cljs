(ns watchlist.perf-test
  (:require [cljs.test :refer-macros [deftest]]
            [watchlist.nodes :as nodes]
            [cljs.pprint :refer [print-table]]
            [clojure.string :refer [join]]
            [hipo.core :as hipo]
            [watchlist.perf-data :as pd]))

(def word-pool (take 100 (shuffle pd/words)))

(def rand-word (partial rand-nth word-pool))

(defn fake-content []
  (take 4 (repeatedly rand-word)))

(defn el-multiplier [kids]
  (for [n (range 4)
        kid kids]
    [:div (fake-content) kid]))

(defn gen-dom [x]
  (loop [to-go x
         kids [[:div (fake-content)][:div (fake-content)]]]
    (if (zero? to-go)
      kids
      (recur (dec to-go) (el-multiplier kids)))))

(defn gen-terms [n]
  (apply merge
         (repeatedly n #(hash-map (rand-word)
                                  (join "|" (repeatedly n rand-word))))))

(deftest perf-test
  (print-table
   (sort-by :elapsed-ms
            (reduce
             (fn [acc [label sandbox terms]]
               (.appendChild js/document.body sandbox)
               (let [start (.now js/Date)]
                 (nodes/highlight-matches! terms)
                 (let [res {:label label
                            :elapsed-ms (- (.now js/Date) start)
                            :matchable-count (.-length (.getElementsByTagName js/document "div"))
                            :matched-count (.-length (.getElementsByTagName js/document "mark"))}]
                   (.removeChild js/document.body sandbox)
                   (conj acc res))))

             []

             (let [s-dom (hipo/create [:div#sandbox (gen-dom 2)])
                   m-dom (hipo/create [:div#sandbox (gen-dom 4)])
                   l-dom (hipo/create [:div#sandbox (gen-dom 6)])
                   s-terms (gen-terms 2)
                   m-terms (gen-terms 4)
                   l-terms (gen-terms 6)]
              [["sml DOM, 2x2 terms" s-dom s-terms]
               ["sml DOM, 4x4 terms" s-dom m-terms]
               ["sml DOM, 6x6 terms" s-dom l-terms]

               ["med DOM, 2x2 terms" m-dom s-terms]
               ["med DOM, 4x4 terms" m-dom m-terms]
               ["med DOM, 6x6 terms" m-dom l-terms]

               ["lrg DOM, 2x2 terms" l-dom s-terms]
               ["lrg DOM, 4x4 terms" l-dom m-terms]
               ["lrg DOM, 6x6 terms" l-dom l-terms]])))))
