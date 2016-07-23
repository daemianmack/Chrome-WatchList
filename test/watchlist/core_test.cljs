(ns watchlist.core-test
  (:require [cljs.test :refer-macros [deftest is testing use-fixtures]]
            [watchlist.core :as core]
            [watchlist.nodes :as nodes]
            [watchlist.test-helpers :as th]))


(use-fixtures :each th/fresh-sandbox!)


(enable-console-print!)

(deftest mark-matches-test
  (let [string "You were eaten by a grue"
        mark   #(nodes/mark-matches (js/RegExp. % "gi") string)]
    (testing "No matches"
      (is (= [{:text "You were eaten by a grue"}]
             (mark "xyzzy"))))
    (testing "Leading match"
      (is (= [{:html "You"} {:text " were eaten by a grue"}]
             (mark "you"))))
    (testing "Interim match"
      (is (= [{:text "You were eaten "}
              {:html "by"}
              {:text " a grue"}]
             (mark "by"))))
    (testing "Trailing match"
      (is (= [{:text "You were eaten by a gr"} {:html "ue"}]
             (mark "ue"))))
    (testing "Leading and trailing match"
      (is (= [{:html "Yo"} {:text "u were eaten by a gr"} {:html "ue"}]
             (mark "yo|ue"))))))

(deftest url-allowed-test
  (testing "Match appears in URL and is thus ignored"
    (is (= [{:text "This string contains the word resources"}]
           (nodes/mark-matches (js/RegExp. "resources" "gi")
                               "This string contains the word resources")))))

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
  (th/assert-matches :xregexp
   {"animals" "shark"}
   "one ring to rule them all"
   "one ring to find them"
   "one ring to bring them all and in the darkness bind them."))

(deftest highlight-matches!-single-category
  (th/assert-matches :xregexp
   {"numbers" "one"}
   "~one!numbers~ ring to rule them all"
   "~one!numbers~ ring to find them"
   "~one!numbers~ ring to bring them all and in the darkness bind them."))

(deftest highlight-matches!-single-category-regex
  (th/assert-matches :xregexp
   {"nouns" "ring|darkness"}
   "one ~ring!nouns~ to rule them all"
   "one ~ring!nouns~ to find them"
   "one ~ring!nouns~ to b~ring!nouns~ them all and in the ~darkness!nouns~ bind them."))

(deftest highlight-matches!-multiple-categories-i-r-x
  (th/assert-matches :xregexp
                     {"nouns" "ring|darkness" "errbody" "them\\ all"}
                     "one ~ring!nouns~ to rule ~them all!errbody~"
                     "one ~ring!nouns~ to find them"
                     "one ~ring!nouns~ to b~ring!nouns~ ~them all!errbody~ and in the ~darkness!nouns~ bind them."))

(deftest highlight-matches!-multiple-categories-extra-regex
  (th/assert-matches :xregexp
   {"nouns" "ring|darkness" "stems" "them?|all|[a-z]ind"}
   "one ~ring!nouns~ to rule ~them!stems~ ~all!stems~"
   "one ~ring!nouns~ to ~find!stems~ ~them!stems~"
   "one ~ring!nouns~ to b~ring!nouns~ ~them!stems~ ~all!stems~ and in ~the!stems~ ~darkness!nouns~ ~bind!stems~ ~them!stems~."))

(deftest highlight-matches!-multiple-categories-extra-regex-2
  (th/assert-matches :xregexp
   {"random" "one|to\\ ....\\ ...."}
   "~one!random~ ring ~to rule them!random~ all"
   "~one!random~ ring ~to find them!random~"
   "~one!random~ ring to bring them all and in the darkness bind them"))

(deftest highlight-matches!-overlapping-categories
  (th/assert-matches :xregexp
   {"verbs" "ring|rule" "nouns" "ring"}
   "one ~ring!nouns!verbs~ to ~rule!verbs~ them all"
   "one ~ring!nouns!verbs~ to find them"
   "one ~ring!nouns!verbs~ to b~ring!nouns!verbs~ them all and in the darkness bind them."))


(deftest highlight-matches!-overlapping-categories-preserves-pre-existing-marks
  (th/assert-matches :xregexp
   {"verbs" "ring|rule" "nouns" "ring"}
   "one ~ring!nouns!verbs~ to ~rule!verbs~ them all"
   "one ~ring!nouns!verbs~ to find them"
   "one ~ring!nouns!verbs~ to b~ring!nouns!verbs~ them all and in the darkness bind them."
   "-- <mark class=\"pre-existing\">Gary Busey</mark>, Keep On Keepin' On"))

(deftest highlight-matches!-overlapping-categories-nests-within-pre-existing-marks
  (th/assert-matches :xregexp
   {"verbs" "ring|rule" "nouns" "ring" "people" "busey"}
   "one ~ring!nouns!verbs~ to ~rule!verbs~ them all"
   "one ~ring!nouns!verbs~ to find them"
   "one ~ring!nouns!verbs~ to b~ring!nouns!verbs~ them all and in the darkness bind them."
   "-- <mark class=\"pre-existing\">Gary <mark class=\"people watchlist-highlight\">Busey</mark></mark>, Keep On Keepin' On"))


