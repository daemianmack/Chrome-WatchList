(ns watchlist.nodes
  (:require [clojure.string :refer [lower-case replace]]))


(defn classes+ [node class] (str (.-className node) " " class))
(defn classes- [node class] (replace (.-className node) (str " " class) ""))

(defn add-class [node class] (set! (.-className node) (classes+ node class)))
(defn del-class [node class] (set! (.-className node) (classes- node class)))

(defn evt [mod-fn node class]
  (fn [e]
    (mod-fn (.getDOMNode node) class)
    (.preventDefault e)))


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

;; Flaw in this approach: information can be lost when attempting to
;; use the result of a regex match as a regex itself. Example:
;; - Original regex "ce\." is an attempt to match a literal trailing period.
;; - This produces a match of "ce." which enters this function as `term`.
;; - This is then used as the base for a new regex which will naively
;;   treat that dot as a metacharacter and produce false positives
;;   when tested against the URL.
(defn url-contains-term?
  [url [term]]
  (re-find (re-pattern (str "(?i)" term)) url))

(def non-nils-in-url
  "`some?` plays two roles here: one incidental, one crucial.
  1. nil shouldn't be passed to `url-contains-term?.`
  2. nil *should* be passed to `mark-matches` which treats nil as a
     sentinel to trigger a wrap-up phase."
  (let [url-contains-term? (partial url-contains-term? (.-URL js/document))]
    (every-pred some? url-contains-term?)))

(defn mark-matches
  [regex s]
  (reduce 
   (fn [{:keys [matches prev-idx] :as acc} [term _ :as match]]
     (if term
       (-> acc
           (update :matches text-gap s prev-idx (.-index match))
           (update :matches conj (html term))
           (assoc  :prev-idx (+ (.-index match) (count term))))
       (reduced (text-gap matches s prev-idx (count s))))) 
   {:matches [] :prev-idx 0} 
   (remove non-nils-in-url (repeatedly #(.exec regex s)))))

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
  (let [regex (js/RegExp. terms "gi")
        matching-texts (filterv #(re-find regex (.-textContent %)) (text-objs))
        new-nodes (for [old-node matching-texts
                        :let [new-node-descs (mark-matches regex (.-textContent old-node))
                              new-node-seq   (map mk-node new-node-descs)
                              _ (swap-in-nodes! old-node new-node-seq)]
                        mark (filter :html new-node-seq)]
                    {:term (lower-case (:html mark)) :node (:node mark)})]
    new-nodes))


(defn ancestors-of [node]
  (take-while some? (iterate #(.-offsetParent %) (.-parentNode node))))

(defn scroll-to-node!
  [node]
  (let [classes (classes- node "watchlist-emphasized")]
    (js/setTimeout (fn [] (set! (.-className node) classes)) 500)
    (add-class node "watchlist-emphasized"))
  (set! (.-scrollTop (.querySelector js/document "body"))
        (reduce + 0 (map #(.-offsetTop %) (ancestors-of node)))))
