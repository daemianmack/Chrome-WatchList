(ns chromex-sample.content-script.nodes
  (:require [clojure.string :refer [replace]]))

(defn text-objs []
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                NodeFilter.SHOW_TEXT)]
    (take-while some? (repeatedly #(.nextNode tree)))))

(defn text [s] {:text s})
(defn html [s] {:html s})

(defn text-gap [matches s start end]
  (if (< start end)
    (conj matches (text (subs s start end)))
    matches))

(defn mark-matches
  [re s]
  (reduce 
   (fn [{:keys [matches prev-idx] :as acc} [term _ :as match]] 
     (if term
       (-> acc
           (update :matches text-gap s prev-idx (.-index match))
           (update :matches conj (html term))
           (assoc  :prev-idx (+ (.-index match) (count term))))
       (reduced (text-gap matches s prev-idx (count s))))) 
   {:matches [] :prev-idx 0} 
   (repeatedly #(.exec re s))))

(defn mk-text-node [text] (.createTextNode js/document text))

(defn mk-mark-node [text]
  (.createDom goog.dom "mark"
              (clj->js {:class "watchlist-highlight"})
              (mk-text-node text)))

(defn mk-node
  [{:keys [text html] :as node-desc}]
  (cond-> node-desc
    text (assoc :node (mk-text-node text))
    html (assoc :node (mk-mark-node html))))

(defn swap-in-nodes!
  [old-node new-nodes]
  (doseq [new-node new-nodes
          :let [parent (.-parentNode old-node)]]
    (.insertBefore parent (:node new-node) old-node))
  (.removeChild (.-parentNode old-node) old-node))

(defn highlight-matches!
  [terms]
  (let [regex (js/RegExp. terms "g")
        matching-texts (filterv #(re-find regex (.-textContent %)) (text-objs))
        new-nodes (for [old-node matching-texts
                        :let [new-node-descs (mark-matches regex (.-textContent old-node))
                              new-node-seq   (map mk-node new-node-descs)
                              _ (swap-in-nodes! old-node new-node-seq)]
                        mark (filter :html new-node-seq)]
                    {:term (:html mark) :node (:node mark)})]
    new-nodes))

(defn ancestors-of [node]
  (take-while some? (iterate #(.-offsetParent %) (.-parentNode node))))

(defn scroll-to-node!
  [node]
  (let [classes (replace (.-className node) #" watchlist-scrolled" "")]
    (js/setTimeout (fn [] (set! (.-className node) classes)) 500)
    (set! (.-className node) (str classes " watchlist-scrolled")))
  (set! (.-scrollTop (.querySelector js/document "body"))
        (reduce + 0 (map #(.-offsetTop %) (ancestors-of node)))))
