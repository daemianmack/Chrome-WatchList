(ns watchlist.test)

(comment
  (map-indexed #(println %1 (.-textContent %2)) (take 31 (node-seq)))

  (def nodes (node-seq))

  (def node (nth nodes 1))
  (def cnodes (.call js/Array.prototype.slice (.-childNodes node)))
  (some #(= 3 (.-nodeType %)) cnodes)

  (def mark (.createElement js/document "mark"))
  (.appendChild mark (.createTextNode js/document "some stuff"))
  
  )

(comment
  :regexes-whee

  (def s  "at lived battlecat vedics")
  (def re #"at|ved")

  (defn go [re s] 
    (loop [acc [] from 0 runs 0] 
      (when (< runs 10) 
        (if-let [ret (.exec re s)] 
          (let [acc (if (not= (.-index ret) from)
                      (conj acc (subs s from (.-index ret)))
                      acc)
                acc (conj acc (str "<mark>" (first ret) "</mark>"))] 
            (recur acc (+ (.-index ret) (count (first ret))) (inc runs)))
          (if (< from (count s))
            (conj acc (subs s from))
            acc)))))


  )

(comment
  
 (and (= [{:text "socket "}
          {:html "<mark>repl</mark>"}
          {:text " and n"}
          {:html "<mark>REPL</mark>"}]
         (replace-matches (js/RegExp. "repl" "gi") "socket repl and nREPL"))

      (= [{:html "<mark>h</mark>"}
          {:text "ow to "}
          {:html "<mark>st</mark>"}
          {:text "a"}
          {:html "<mark>rt</mark>"}
          {:text " a cu"}
          {:html "<mark>lt</mark>"}]
         (replace-matches (js/RegExp. "h|[lrs]t" "gi") "how to start a cult"))

      (= [{:text "how to "}
          {:html "<mark>re</mark>"}
          {:text "lease endo"}
          {:html "<mark>rp</mark>"}
          {:text "hins"}]
         (replace-matches (js/RegExp. "r." "gi") "how to release endorphins"))))
