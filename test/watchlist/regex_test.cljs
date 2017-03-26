(ns watchlist.regex-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clojure.string :refer [join split]]
            [common.regex :as regex]
            [goog.object]))


;; (deftest invert-terms-test
;;   (is (= {["kws" "strs"] [:a :b :c "a" "b" "c"]}
;;          {"kws" [:a :b :c]
;;           "strs" ["a" "b" "c"]
;;           "vowels" ["a" :a]
;;           "consonants" [:b :c "b" "b"]})))

;; (deftest invert-terms-test
;;   (is (= {["vowels"]     ["a" "e"]
;;           ["consonants"] ["b" "c" "r"]
;;           ["liquids"] ["l"]
;;           ["semivowels"] ["j"]
;;           ["consonants" "liquids"] ["r"]
;;           ["consonants" "semivowels"] ["w"]}
;;          (regex/invert-terms {"vowels"     ["a e"]
;;                               "liquids"    ["r l"]
;;                               "consonants" ["b c r w"]
;;                               "semivowels" ["j w"]}))))

(defn ->lines [x]
  (join "\n" (split x #"\s")))

(deftest category-combinations-test
  (is (= {["birds"] ["emu"]
          ["bipeds"] ["dwarf"]
          ["canines"] ["fox"]
          ["tentacled"] ["squid"]
          ["monsters" "tentacled"] ["beholder" "kraken"]
          ["birds" "monsters"] ["roc"]
          ["bipeds" "canines" "monsters"] ["werewolf"]
          ["bipeds" "monsters" "tentacled"] ["gorgon"]}
         (regex/category-combinations
          {"monsters"  (->lines "roc werewolf kraken gorgon beholder")
           "canines"   (->lines "fox werewolf")
           "birds"     (->lines "emu roc")
           "bipeds"    (->lines "werewolf gorgon dwarf")
           "tentacled" (->lines "kraken gorgon squid beholder")}))))

(deftest ->regex-str-test
  (is (= "(?<bipeds>dwarf)|(?<birds>emu)|(?<canines>fox)|(?<tentacled>squid)|(?<birds$$$monsters>roc)|(?<monsters$$$tentacled>beholder|kraken)|(?<bipeds$$$canines$$$monsters>werewolf)|(?<bipeds$$$monsters$$$tentacled>gorgon)"
         (regex/->regex-str
          {"monsters"  (->lines "roc werewolf kraken gorgon beholder")
           "canines"   (->lines "fox werewolf")
           "birds"     (->lines "emu roc")
           "bipeds"    (->lines "werewolf gorgon dwarf")
           "tentacled" (->lines "kraken gorgon squid beholder")}))))

;; xregexp lib maintains a list of all capture-group names that lets
;; us write less code. This property is undocumented, so back it up
;; with a test in case it changes out from underneath us.
(deftest xregexp-undocumented-captureNames-prop-test
  (is (= ["bipeds"
          "birds"
          "canines"
          "tentacled"
          "birds$$$monsters"
          "monsters$$$tentacled"
          "bipeds$$$canines$$$monsters"
          "bipeds$$$monsters$$$tentacled"]
         (let [regex (regex/->regex
                      {"monsters"  (->lines "roc werewolf kraken gorgon beholder")
                       "canines"   (->lines "fox werewolf")
                       "birds"     (->lines "emu roc")
                       "bipeds"    (->lines "werewolf gorgon dwarf")
                       "tentacled" (->lines "kraken gorgon squid beholder")})]
           (js->clj
            (goog.object/getValueByKeys regex
                                        "xregexp"
                                        "captureNames"))))))

