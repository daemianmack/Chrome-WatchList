(ns watchlist.nodes
  (:require [clojure.string :refer [lower-case replace join split]]
            [clojure.set :refer [union]]
            [cljs.pprint :refer [pprint cl-format]]
            [com.xregexp]))

(enable-console-print!)

(defn p [& xs]
  (pprint (into [] xs)))

(defn add-class [node class] (.add    (.-classList node) class))
(defn del-class [node class] (.remove (.-classList node) class))

(defn evt [mod-fn node class]
  (fn [e]
    (mod-fn (.getDOMNode node) class)
    (.preventDefault e)))

(declare node-is-regex-match?)
(defn text-objs-w-i-r [terms]
  ;; avoid "g" flag of regex.
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                NodeFilter.SHOW_TEXT
                                #js {:acceptNode
                                     (fn [node]
                                       (if (node-is-regex-match? (js/RegExp. terms) node)
                                         NodeFilter.FILTER_ACCEPT
                                         NodeFilter.FILTER_SKIP))})]
    (take-while some? (repeatedly #(.nextNode tree)))))
(defn text-objs-w-p-r-c [a-fn]
  ;; avoid "g" flag of regex.
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                NodeFilter.SHOW_TEXT
                                #js {:acceptNode
                                     (fn [node]
                                       (if (a-fn (.-textContent node))
                                         NodeFilter.FILTER_ACCEPT
                                         NodeFilter.FILTER_SKIP))})]
    (take-while some? (repeatedly #(.nextNode tree)))))

(defn text-objs []
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                NodeFilter.SHOW_TEXT)]
    (take-while some? (repeatedly #(.nextNode tree)))))

(defn text [s] {:text s})
(defn html [s] {:html s})

(defn mk-text-node-x [text] (.createTextNode js/document text))

(defn text-gap-x [matches s start end]
  (if (< start end)
    (conj matches {:node (mk-text-node-x (subs s start end))})
    matches))

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

(defn url-contains-term-x?
  [url term]
  (re-find (re-pattern (str "(?i)" term)) url))

(def non-nils-in-url
  "`some?` plays two roles here: one incidental, one crucial.
  1. nil shouldn't be passed to `url-contains-term?.`
  2. nil *should* be passed to `mark-matches` which treats nil as a
     sentinel to trigger a wrap-up phase."
  (let [url-contains-term? (partial url-contains-term? (.-URL js/document))]
    (every-pred some? url-contains-term?)))

(defn mk-node-x
  [categories match]
  (let [node (.createDom goog.dom "mark" nil (mk-text-node-x match))]
    (doseq [class (sort (into ["watchlist-highlight"] (split categories #"\$\$\$")))]
      (add-class node class))
    node))

(declare swap-in-nodes!)

(defn mark-matches-x-reduce
  [regex-data node]
  (let [text-content (.-textContent node)
        regex (.globalize js/XRegExp (:regex regex-data))
        new-node-descs
        (reduce
         (fn [{:keys [prev-idx] :as acc} match-data]
           (if match-data
             (let [[category value] (first (for [category (:category-names regex-data)
                                                 :let [value (goog.object/get match-data category)]
                                                 :when (not-empty value)]
                                             [category value]))]
               (-> acc
                   (update :matches text-gap-x text-content prev-idx (.-index match-data))
                   (update :matches conj {:node (mk-node-x category value)
                                          :term value
                                          :groups (split category #"\$\$\$")
                                          :html true})
                   (assoc :prev-idx (.-lastIndex regex))))
             (reduced (text-gap-x (:matches acc) text-content prev-idx (count text-content)))))
         {:matches [] :prev-idx 0}
         (remove non-nils-in-url (repeatedly #(.exec js/XRegExp text-content regex (.-lastIndex regex)))))]
    (swap-in-nodes! node new-node-descs)
    (filter :html new-node-descs)))

(defn mark-matches-x
  [regex-data node]
  (let [text-content (.-textContent node)
        regex (:regex regex-data)
        new-node-descs (loop [pos 0
                              acc {:matches []}
                              n 5]
                         (if-let [o-res (.exec js/XRegExp text-content regex pos)]
                           (let [res (apply merge-with merge
                                            (for [name (:category-names regex-data)
                                                  :let [val (goog.object/get o-res name)]
                                                  :when (not-empty val)]
                                              {name val}))
                                 acc (-> acc
                                         (update :matches text-gap-x text-content pos (.-index o-res))
                                         (update :matches conj {:node (mk-node-x (first (keys res)) (first (vals res)))
                                                                :term (first (vals res))
                                                                :groups (split (first (keys res)) #"\$\$\$")
                                                                :html true}))]
                             (recur (+ (.-index o-res) (count (first (vals res)))) acc (dec n)))
                           (text-gap-x (:matches acc) text-content pos (count text-content))))]
    (swap-in-nodes! node new-node-descs)
    (filter :html new-node-descs)))

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

(defn mk-mark-node [text group]
  (let [node (.createDom goog.dom "mark" nil (mk-text-node text))]
    (doseq [class (sort ["watchlist-highlight" (name group)])]
      (add-class node class))
    node))

(defn mk-node
  [group {:keys [text html] :as node-desc}]
  (cond-> node-desc
    text (assoc :node (mk-text-node text))
    html (assoc :node (mk-mark-node html group))))

(extend-type js/DOMTokenList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn adopt-classes!
  [old-node donor-nodes]
  (let [parent (.-parentNode old-node)
        new-classes (reduce
                     (fn [acc node] (into acc (.-classList node)))
                     #{}
                     (map :node donor-nodes))
        old-classes (set (.-classList parent))]
    ;; Wipe out existing classes so we can add all classes in predictable order.
    (doseq [class old-classes]
      (del-class parent class))
    (doseq [class (sort (union new-classes old-classes))]
      (add-class parent class))
    (doseq [node donor-nodes]
      (set! (.-display (.-style (:node node ))) "none")
      (.insertBefore parent (:node node) old-node))))

(defn previous-watchlist-mark?
  [node]
  (let [parent (.-parentNode node)]
    (and (= "MARK" (.-tagName parent))
         (not (neg? (.indexOf (.-className parent)
                              "watchlist-highlight"))))))

(defn swap-in-nodes!
  [old-node new-nodes]
  (if (previous-watchlist-mark? old-node)
    (adopt-classes! old-node new-nodes)
    (do (let [parent (.-parentNode old-node)]
          (doseq [new-node new-nodes]
            (.insertBefore parent (:node new-node) old-node))
          (.removeChild parent old-node)))))


(defn parent-is-visible?
  [parent]
  (not= 0
        (.-offsetWidth parent)
        (.-offsetHeight parent)
        (.-length (.getClientRects parent))))

(defn parent-can-contain-markup?
  [parent]
  (not (contains? #{"SCRIPT" "NOSCRIPT" "TEXTAREA"} (.-tagName parent))))

(defn node-is-regex-match? [regex node]
  (re-find regex (.-textContent node)))

(defn node-is-regex-match?-x-reduce [regex node]
  (.test js/XRegExp regex (.-textContent node)))

;; This seems to perform somewhat faster than using an equivalent
;; NodeFilter in the upstream TreeWalker.
(defn qualifying-node-x-reduce [regex node]
  (let [parent (.-parentNode node)]
    (and (node-is-regex-match? regex node)
         (parent-can-contain-markup? parent)
         (parent-is-visible? parent))))

;; This seems to perform somewhat faster than using an equivalent
;; NodeFilter in the upstream TreeWalker.
(defn qualifying-node [regex node]
  (let [parent (.-parentNode node)]
    (and (node-is-regex-match? regex node)
         (parent-can-contain-markup? parent)
         (parent-is-visible? parent))))

(defmulti highlight-matches! (fn [strategy data] strategy))

(defn qualifying-node-w-p-r-c [node]
  (let [parent (.-parentNode node)]
    (and (parent-can-contain-markup? parent)
         (parent-is-visible? parent))))


(defn invert-terms [terms]
  (apply merge-with into
         (for [[k vs] terms
               v (split vs #"\|")]
           {v [k]})))

;; (defn ->regex [terms]
;;   (let [terms (invert-terms terms)
;;         term-map (reduce-kv
;;                   (fn [acc term categories]
;;                     (conj acc (cl-format "(?<~A>~A)"
;;                                          (join "$$$" categories)
;;                                          term)))
;;                   []
;;                   terms)]
;;     {:regex (join "|" term-map)
;;      :capture-names (map #(join "$$$" %) (vals terms))}))

(defn ->regex-data [terms]
  (let [terms (invert-terms terms)
        term-map (reduce-kv
                  (fn [acc term categories]
                    (update acc categories
                            conj term))
                  {}
                  terms)
        regex (map (fn [[categories terms]]
                     (cl-format nil "(?<~A>~A)"
                                (join "$$$" categories)
                                (join "|"   terms)))
                   term-map)]
    {:regex (js/XRegExp. (join "|" regex) "i")
     :category-names (map #(join "$$$" %) (keys term-map))}))

(defmethod highlight-matches! :xregexp-reduce
  [_ term-data]
  (let [regex-data (->regex-data term-data)
        matching-texts (filterv (partial qualifying-node-x-reduce (:regex regex-data)) (text-objs))
        new-nodes (into []
                        (for [old-node matching-texts]
                          (mark-matches-x-reduce regex-data old-node)))]
    new-nodes))

(defmethod highlight-matches! :xregexp
  [_ term-data]
  (let [regex-data (->regex-data term-data)
        matching-texts (filterv (partial qualifying-node (:regex regex-data)) (text-objs))
        new-nodes (into []
                        (for [old-node matching-texts]
                          (mark-matches-x regex-data old-node)))]
    new-nodes))

(defmethod highlight-matches! :legacy
  [_ term-data]
  (reduce-kv
   (fn [acc group terms]
     (let [regex (js/RegExp. terms "gi")
           matching-texts (filterv (partial qualifying-node (js/RegExp. terms "i")) (text-objs))
           new-nodes (doall
                      (for [old-node matching-texts
                            :let [new-node-descs (mark-matches regex (.-textContent old-node))
                                  new-node-seq   (map (partial mk-node group) new-node-descs)
                                  _ (swap-in-nodes! old-node new-node-seq)]
                            mark (filter :html new-node-seq)]
                        {:term (lower-case (:html mark)) :node (:node mark) :group group}))]
       (into acc new-nodes)))
   []
   term-data))

(defn ancestors-of [node]
  (take-while some? (iterate #(.-offsetParent %) (.-parentNode node))))

(defn scroll-to-node!
  [node]
  (let [classes (replace (.-className node) #"\\s+watchlist-emphasized\\s+" "")]
    (js/setTimeout (fn [] (set! (.-className node) classes)) 500)
    (add-class node "watchlist-emphasized"))
  (set! (.-scrollTop (.querySelector js/document "body"))
        (reduce + 0 (map #(.-offsetTop %) (ancestors-of node)))))

