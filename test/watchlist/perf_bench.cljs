(ns watchlist.perf-bench
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
(def word-cycler (cycle word-pool))
(defn next-n-strs
  ([tn x] (next-n-strs tn tn x))
  ([tn dn x] (join " " (take tn (drop dn x)))))

(defn gen-terms [group-count group-size]
  (into {}
        (for [sq (take group-count (partition-all group-size 1 word-pool))]
          [(str (gensym)) (join "\n" sq)])))

(defn mk-parent [height children terms-per cycler offset]
  [:div {"height" height "offset" offset}
   (next-n-strs terms-per offset cycler)
   children])

(defn mk-tree
  ([height cycler terms-per]
   (mk-tree (dec height) cycler terms-per ()))
  ([height cycler terms-per nodes]
   (if (zero? height)
     (mk-parent height nodes terms-per cycler 0)
     (mk-tree (dec height)
                (drop (* terms-per 2) cycler)
                terms-per
                (map-indexed (partial mk-parent height nodes terms-per cycler)
                             (range 2))))))

(def perf-scenarios
  (let [T-dom #(hipo/create [:section#sandbox (mk-tree  4 word-cycler 2)])
        S-dom #(hipo/create [:section#sandbox (mk-tree  8 word-cycler 2)])
        M-dom #(hipo/create [:section#sandbox (mk-tree 10 word-cycler 4)])
        L-dom #(hipo/create [:section#sandbox (mk-tree 12 word-cycler 4)])

        T-terms (gen-terms  20 1)
        S-terms (gen-terms  40 2)
        M-terms (gen-terms  80 4)
        L-terms (gen-terms 160 8)]

    [#_["T DOM, T terms" 16 2 T-dom T-terms]

     ["S DOM S terms"  256 2 S-dom S-terms]
     ["S DOM M terms"  256 2 S-dom M-terms]
     ["S DOM L terms"  256 2 S-dom L-terms]

     ["M DOM S terms" 1024 4 M-dom S-terms]
     ["M DOM M terms" 1024 4 M-dom M-terms]
     ["M DOM L terms" 1024 4 M-dom L-terms]

     ["L DOM S terms" 4096 4 L-dom S-terms]
     ["L DOM M terms" 4096 4 L-dom M-terms]
     ["L DOM L terms" 4096 4 L-dom L-terms]]))

(deftest perf-test-dom-scenarios
  (print-table
   (sort-by :elapsed-ms
            (reduce
             (fn [acc [label n-nodes n-words-per-node sandbox terms]]
               (let [sandbox (sandbox)]
                 (.appendChild js/document.body sandbox)
                 (let [terms (regex/->regex-data terms)
                       start (.now js/Date)]
                   (nodes/highlight-matches! terms)
                   (let [res {:label label
                              :nodes n-nodes
                              :matchable-words (* n-nodes n-words-per-node)
                              :matched-count (.-length (.getElementsByTagName js/document "mark"))
                              :elapsed-ms (- (.now js/Date) start)}]
                     (.removeChild js/document.body sandbox)
                     (conj acc res)))))
             []
             perf-scenarios))))
