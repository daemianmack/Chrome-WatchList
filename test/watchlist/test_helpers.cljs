(ns watchlist.test-helpers
  (:require [cljs.test       :refer-macros [is]]
            [clojure.string  :refer [replace join split]]
            [cljs.pprint     :refer [pprint cl-format]]
            [common.regex    :as regex]
            [common.dom      :as dom]
            [watchlist.nodes :refer [highlight-matches!]]
            [hipo.core       :as hipo]
            [instaparse.core :as insta]))


(def div-sandbox
  [:div#sandbox
   [:div "one ring to rule them all"]
   [:div "one ring to find them"]
   [:div "one ring to bring them all and in the darkness bind them."]
   [:div "-- "
    [:mark.pre-existing "Gary Busey"]
    ", Keep On Keepin' On"]])

(defn fresh-sandbox! [f]
  (let [sandbox (hipo/create div-sandbox)]
    (.appendChild js/document.body sandbox)
    (f)
    (.removeChild js/document.body sandbox)))


(defn node-seq [root filter]
  (dom/node-seq {:root root :filter filter}))

(defn children-of-id [id & [tag-filter]]
  (node-seq (.getElementById js/document id)
            (fn [node]
               (if (nil? tag-filter)
                 NodeFilter.FILTER_ACCEPT
                 (if (= tag-filter (.toLowerCase (.-tagName node)))
                   NodeFilter.FILTER_ACCEPT
                   NodeFilter.FILTER_SKIP)))))

(defn children-of-node [node & [tag-filter]]
  (node-seq node
            (fn [node]
              (if (= tag-filter (.toLowerCase (.-tagName node)))
                NodeFilter.FILTER_ACCEPT
                NodeFilter.FILTER_SKIP))))

(defn marks [nodes]
  (children-of-node (.getElementById js/document "body")
                    "mark"))

(defn elements-of-tag [tag]
  (.getElementsByTagName js/document tag))

(defn dump-nodes []
  (doseq [node (children-of-id "sandbox" "div")]
    (.log js/console (.-innerHTML node))))

;; Allow description of a node's innerHTML via text containing inline
;; markup references, which is cleaner-reading than mechanically
;; assembling such a string with function calls in the assertion.
;; This was the highlight of my Friday. Don't judge bro.
(def parser
  "Parse strings consisting of text interspersed with markup expandos.
  Expandos are demarcated with ~ and consist of one or more
  css-classes, each with a ! sentinel, followed by a final content string. "
  (insta/parser
   "S = (EXPANDO|LITERAL)*
     LITERAL = #'[^~]*'*
     EXPANDO = <'~'> CONTENT (<'!'> CSS-CLASS)+ <'~'>
     CSS-CLASS = #'[a-zA-Z0-9-]+'*
     CONTENT = #'[ a-zA-Z0-9-]+'* "))

(defn flatten-expando [[content & classes]]
  (let [classes (cons "watchlist-highlight" classes)]
    (cl-format nil "<mark class=\"~A\">~A</mark>"
               (apply str (interpose " " (sort classes)))
               content)))

(defn expand-class-refs [s]
  (let [parsed (parser s)]
    (if (:reason parsed)
      (throw (js/Error. (cl-format nil "Parser error: ~A"
                                   parsed)))
      (apply str
             (reduce
              (fn [acc [tag & data]]
                (into acc
                      (case tag
                        :LITERAL data
                        :EXPANDO (flatten-expando (map second data)))))
              []
              (rest parsed))))))

(defn test-syntax->ui-syntax [m]
  "Expand convenient test syntax to syntax used in UI."
  (reduce-kv #(assoc %1 %2 (join "\n" (split %3 #"\|")))
             {}
             m))

(defn assert-matches [match-spec & strs]
  (highlight-matches! (regex/->regex-str (test-syntax->ui-syntax match-spec)))
  (doall (map
          (fn [regex node]
            (is (re-find regex (.-innerHTML node))))
          (map (comp re-pattern expand-class-refs) strs)
          (children-of-id "sandbox" "div"))))
