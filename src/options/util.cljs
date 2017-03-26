(ns options.util
  (:require [clojure.string :refer [split join replace upper-case]]))

(defn invert-terms [terms]
  ;; TODO Validate classes are viable XRegExp group names.
  (apply merge-with into
         (for [[k vs] terms ;; TODO Use domain words like "category-names" here.
               v (clojure.string/split vs #"\n|\s*#.*")
               :when (not-empty v)]
           {v [k]})))

(defn ->regex-str [terms] ;; TODO Explain purpose of fn -- allows one call to highlight-matches by creating single regex with unique category-name per combo of cats.
  (let [terms (invert-terms terms)
        term-map (reduce-kv
                  (fn [acc term categories]
                    (update acc categories
                            conj term))
                  {}
                  terms)
        regex (map (fn [[categories terms]]
                     (str "(?<"
                          (join "$$$" categories)
                          ">"
                          (join "|" terms)
                          ")"))
                   term-map)]
    {:regex (join "|" regex)
     :category-names (map #(join "$$$" %) (keys term-map))}))

