(ns watchlist.nodes
  (:require [clojure.string :refer [lower-case replace join split]]
            [clojure.set :refer [union]]
            [cljs.pprint :refer [pprint]]
            [common.regex :as regex]
            [common.dom :as dom]
            cljsjs.xregexp))



(enable-console-print!)


(defn evt [mod-fn node class]
  (fn [e]
    (mod-fn (.getDOMNode node) class)
    (.preventDefault e)))

(defn text-objs []
  (dom/node-seq {:show :text}))

(defn mk-text-node [text] (.createTextNode js/document text))

(defn text-gap [matches s start end]
  (if (< start end)
    (conj! matches {:node (mk-text-node (subs s start end))})
    matches))


;; Flaw in this approach: information can be lost when attempting to
;; use the result of a regex match as a regex itself. Example:
;; - Original regex "ce\." is an attempt to match a literal trailing period.
;; - This produces a match of "ce." which enters this function as `term`.
;; - This is then used as the base for a new regex which will naively
;;   treat that dot as a metacharacter and produce false positives
;;   when tested against the URL.
(defn url-contains-term?
  [url term]
  (re-find (re-pattern (str "(?i)" term)) url))

(defn flt [x]
  (when (and (not-empty x)
             (not (url-contains-term? (.-URL js/document) (first x))))
    x))

(defn mk-node
  [category match]
  (let [categories (split category #"\$\$\$")
        node (dom/mk-el "mark" nil (mk-text-node match))]
    (doseq [class (sort (into ["watchlist-highlight"] categories))]
      (dom/add-class node class))
    {:node node
     :term (lower-case match)
     :groups categories
     :html true}))

(declare swap-in-nodes!)

(defn get-cat-val [category-names match-data]
  (first (for [category category-names
               :let [value (goog.object/get match-data category)]
               :when (not-empty value)]
           [category value])))

(defn mark-matches
  "If the text-bearing `node` matches against `regex-data` then swap
  in to the DOM a new set of replacement nodes with the non-matching
  text preserved and the matching text portion wrapped in a new HTML
  node representing a parent tag we can use to manage it. Return all
  such wrapped nodes."
  [regex-data node]
  (let [text-content (.-textContent node)
        regex (:regex regex-data)
        new-node-descs (persistent!
                        (loop [acc (transient []) pos 0]
                          (if-let [match-data (flt (.exec js/XRegExp text-content regex pos))]
                            (let [[category value] (get-cat-val (:category-names regex-data) match-data)
                                  acc (-> acc
                                          (text-gap text-content pos (.-index match-data))
                                          (conj! (mk-node category value)))]
                              (recur acc (+ (.-index match-data) (count value))))
                            (text-gap acc text-content pos (count text-content)))))]
    (swap-in-nodes! node new-node-descs)
    (filter :html new-node-descs)))


(defn swap-in-nodes!
  [old-node new-nodes]
  (let [parent (.-parentNode old-node)]
    (doseq [new-node new-nodes]
      (.insertBefore parent (:node new-node) old-node))
    (.removeChild parent old-node)))

(defn ^boolean parent-is-visible?
  [parent]
  (not= 0
        (.-offsetWidth parent)
        (.-offsetHeight parent)
        (.-length (.getClientRects parent))))

(defn ^boolean parent-can-contain-markup?
  [parent]
  (not (contains? #{"SCRIPT" "NOSCRIPT" "TEXTAREA"} (.-tagName parent))))

(defn node-is-regex-match? [regex node]
  (.test js/XRegExp (.-textContent node) regex))

;; This seems to perform somewhat faster than using an equivalent
;; NodeFilter in the upstream TreeWalker.
(defn qualifying-node [regex node]
  (let [parent (.-parentNode node)]
    (and (node-is-regex-match? regex node)
         (parent-can-contain-markup? parent)
         (parent-is-visible? parent))))

(defn highlight-matches!
  [regex-data]
  (when regex-data
    (let [regex-map (regex/regexify regex-data)
          matching-texts (filterv (partial qualifying-node (:regex regex-map)) (text-objs))
          new-nodes (reduce #(into %1 (mark-matches regex-map %2)) [] matching-texts)]
      new-nodes)))


(defn ancestors-of [node]
  (take-while some? (iterate #(.-offsetParent %) (.-parentNode node))))

(defn scroll-to-node!
  [node]
  (let [classes (replace (.-className node) #"\\s+watchlist-emphasized\\s+" "")]
    (js/setTimeout (fn [] (set! (.-className node) classes)) 500)
    (dom/add-class node "watchlist-emphasized"))
  (set! (.-scrollTop (.querySelector js/document "body"))
        (reduce + 0 (map #(.-offsetTop %) (ancestors-of node)))))
