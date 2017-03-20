(ns common.dom)

(defn to-s [x]
  (if (.-outerHTML x)
    {:outerHTML (.-outerHTML x)}
    (if (.-textContent x)
      {:textContent (.-textContent x)}
      (if (and (map? x) (:node x))
        (update x :node to-s)
        x))))
