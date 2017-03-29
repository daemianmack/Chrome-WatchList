(ns watchlist.core-test
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [common.regex :as regex]
            [common.dom :as dom]
            [watchlist.core :as core]
            [watchlist.nodes :as nodes]
            [watchlist.test-helpers :as th]))


(use-fixtures :each th/fresh-sandbox!)


(enable-console-print!)



(defn mk-tree
  ([txt] (mk-tree {:id "foo"} txt))
  ([root-attrs txt]
   (.createDom goog.dom "div" (clj->js root-attrs)
               (.createTextNode js/document txt))))

(defn mark-matches [regex tree]
  (nodes/mark-matches regex
                      (first (dom/node-seq {:root tree :show :text}))))

(defn terms-marked [tree-fn terms]
   (map :term (mark-matches (regex/->regex terms) (tree-fn))))

(deftest mark-matches-basic-regexp-functioning-test
  (let [tree #(mk-tree "You were eaten by a grue")]
    (testing "No matches"
      (is (= []
             (terms-marked tree {"dummygroup" "xyzzy"}))))
    (testing "Leading match"
      (is (= ["you"]
             (terms-marked tree {"dummygroup" "you"}))))
    (testing "Interim match"
      (is (= ["by"]
             (terms-marked tree {"dummygroup" "by"}))))
    (testing "Trailing match"
      (is (= ["ue"]
             (terms-marked tree {"dummygroup" "ue"}))))
    (testing "Leading and trailing match"
      (is (= ["yo" "ue"]
             (terms-marked tree {"dummygroup" "yo|ue"}))))))

(deftest url-allowed-test
  "This test makes use of the incidental fact that the file URI passed
  to the Phantom test runner contains the word 'resources' and does
  not contain the word 'Qresources'."
  (let [tree #(mk-tree "resources")]
    (testing "Match appears in URL and is thus ignored"
      (is (= []
             (terms-marked tree {"dummygroup" "resources"}))))
    (testing "Match doesn't appear in URL and is thus acknowledged"
      (is (= []
             (terms-marked tree {"dummygroup" "Qresources"}))))))

(deftest mod-clicks-over-nodes-test
  (let [three-fake-nodes [{} {} {}]
        click (fn [nth clicked-term old-term]
                (core/mod-clicks-over-nodes {:index nth
                                             :term old-term}
                                            clicked-term
                                            three-fake-nodes))]
   (is (= 0 (click nil "grue" nil)))
   (is (= 1 (click 0 "grue" "grue")))
   (is (= 2 (click 1 "grue" "grue")))
   (is (= 0 (click 2 "grue" "grue")))
   (is (= 0 (click 1 "grue" "cave")))))

(deftest highlight-matches!-no-matches
  (th/assert-matches
   {"animals" "shark"}
   "one ring to rule them all"
   "one ring to find them"
   "one ring to bring them all and in the darkness bind them."))

(deftest highlight-matches!-single-category
  (th/assert-matches
   {"numbers" "one"}
   "~one!numbers~ ring to rule them all"
   "~one!numbers~ ring to find them"
   "~one!numbers~ ring to bring them all and in the darkness bind them."))

(deftest highlight-matches!-single-category-more
  (th/assert-matches
   {"nouns" "ring|darkness"}
   "one ~ring!nouns~ to rule them all"
   "one ~ring!nouns~ to find them"
   "one ~ring!nouns~ to b~ring!nouns~ them all and in the ~darkness!nouns~ bind them."))

(deftest highlight-matches!-multiple-categories
  (th/assert-matches
   {"nouns" "ring|darkness" "errbody" "them\\ all"}
   "one ~ring!nouns~ to rule ~them all!errbody~"
   "one ~ring!nouns~ to find them"
   "one ~ring!nouns~ to b~ring!nouns~ ~them all!errbody~ and in the ~darkness!nouns~ bind them."))

(deftest highlight-matches!-multiple-categories-extra-regex
  (th/assert-matches
   {"nouns" "ring|darkness" "stems" "them?|all|[a-z]ind"}
   "one ~ring!nouns~ to rule ~them!stems~ ~all!stems~"
   "one ~ring!nouns~ to ~find!stems~ ~them!stems~"
   "one ~ring!nouns~ to b~ring!nouns~ ~them!stems~ ~all!stems~ and in ~the!stems~ ~darkness!nouns~ ~bind!stems~ ~them!stems~."))

(deftest highlight-matches!-multiple-categories-extra-regex-2
  (th/assert-matches
   {"random" "one|to\\ ....\\ ...."}
   "~one!random~ ring ~to rule them!random~ all"
   "~one!random~ ring ~to find them!random~"
   "~one!random~ ring to bring them all and in the darkness bind them"))

(deftest highlight-matches!-overlapping-categories
  (th/assert-matches
   {"verbs" "ring|rule" "nouns" "ring"}
   "one ~ring!nouns!verbs~ to ~rule!verbs~ them all"
   "one ~ring!nouns!verbs~ to find them"
   "one ~ring!nouns!verbs~ to b~ring!nouns!verbs~ them all and in the darkness bind them."))


(deftest highlight-matches!-overlapping-categories-preserves-pre-existing-marks
  (th/assert-matches
   {"verbs" "ring|rule" "nouns" "ring"}
   "one ~ring!nouns!verbs~ to ~rule!verbs~ them all"
   "one ~ring!nouns!verbs~ to find them"
   "one ~ring!nouns!verbs~ to b~ring!nouns!verbs~ them all and in the darkness bind them."
   "-- <mark class=\"pre-existing\">Gary Busey</mark>, Keep On Keepin' On"))

(deftest highlight-matches!-overlapping-categories-nests-within-pre-existing-marks
  (th/assert-matches
   {"verbs" "ring|rule" "nouns" "ring" "people" "busey"}
   "one ~ring!nouns!verbs~ to ~rule!verbs~ them all"
   "one ~ring!nouns!verbs~ to find them"
   "one ~ring!nouns!verbs~ to b~ring!nouns!verbs~ them all and in the darkness bind them."
   "-- <mark class=\"pre-existing\">Gary <mark class=\"people watchlist-highlight\">Busey</mark></mark>, Keep On Keepin' On"))
