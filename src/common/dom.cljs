(ns common.dom)

(defn to-s [x]
  (if (.-outerHTML x)
    {:outerHTML (.-outerHTML x)}
    (if (.-textContent x)
      {:textContent (.-textContent x)}
      (if (and (map? x) (:node x))
        (update x :node to-s)
        x))))

(defn node-seq
  [{:keys [root show filter]
    :or {root (.-body js/document)
         show :element
         filter (constantly NodeFilter.FILTER_ACCEPT)}}]
  (let [show-type (case show
                    :element NodeFilter.SHOW_ELEMENT
                    :text    NodeFilter.SHOW_TEXT)
        walker (.createTreeWalker js/document
                                root
                                show-type
                                #js {:acceptNode filter})]
    (take-while some? (repeatedly #(.nextNode walker)))))
