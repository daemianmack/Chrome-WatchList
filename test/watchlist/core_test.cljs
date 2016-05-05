(ns watchlist.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [watchlist.core :as core]
            [watchlist.nodes :as nodes]))

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
