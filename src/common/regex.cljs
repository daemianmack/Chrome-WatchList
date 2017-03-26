(ns common.regex
  (:require [clojure.string :refer [join split]]
            cljsjs.xregexp))

(defn category-combinations [terms]
  "Invert a map such that the resulting keys are the unique
  combination of categories assigned to a value."
  ;; TODO Validate classes are viable XRegExp group names.
  (let [term-map (apply merge-with into
                        (for [[k vs] terms
                              v (split vs #"\n|\s*#.*")
                              :when (not-empty v)]
                          {v [k]}))]
    (reduce-kv
     (fn [acc term category-combo]
       (update acc (vec (sort category-combo))
               ;; Sort for stable output.
               #(sort (conj % term))))
     {}
     term-map)))

(defn ->regex-str [terms]
  "Prepare a map of categories to terms for regexification.

   As a workaround to XRegExp's lack of ability to match a term
   against multiple categories [1], produce a regex assigning each
   value to a potentially-unique combination of category names,
   demarcated with magic sequence '$$$' for later recovery.

   Any resulting regex match will thus bear a reference to the full
   group of categories specified for the matched value. [2]"
  ;; [1]
  ;; `(.exec XRegExp "(?<birds>roc)|(?<monsters>roc)" "The roc's egg is monstrous")`
  ;; captures only the groupname "birds" but we'd expect it to match
  ;; both "birds" and "monsters".

  ;; [2]
  ;; `(.exec XRegExp "(?<birds$$$monsters>roc)" "The roc's egg is monstrous")`
  ;; will, in post-processing with special knowledge of the '$$$'
  ;; sequence, let us retrieve both group names for the single match.
  (let [combos (sort (category-combinations terms))
        regex (map (fn [[categories terms]]
                     (str "(?<"
                          (join "$$$" categories)
                          ">"
                          (join "|" terms)
                          ")"))
                   combos)]
    (join "|" regex)))

(defn regexify [regex-str]
  (js/XRegExp. regex-str "i"))

(defn ->regex [terms]
  (regexify (->regex-str terms)))
