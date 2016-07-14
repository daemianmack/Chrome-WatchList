(ns options.ui
  (:require [clojure.string :refer [split join replace upper-case]]
            [cljs.pprint :refer [pprint]]))

(enable-console-print!)

(defn to-regex [s]
  (->> (split s #"\n")
       (filter not-empty)
       (join "|")))

(defn to-text [s]
  (->> (split s #"\|")
       (join "\n")))

(defn id->el [id] (.getElementById js/document id))

(defn set-val!  [id val] (set! (.-value     (id->el id)) val))
(defn set-html! [id val] (set! (.-innerHTML (id->el id)) val))

(defn upcase [s]
  (str (upper-case (first s)) (subs s 1)))

(defn update-status [tab-handle]
  (.setTimeout js/window
               #(set-html! (str tab-handle "-status") "")
               3000)
  (set-html! (str tab-handle "-status")
             (str (upcase tab-handle) " saved.")))

(defn save-options* [tab-handle val-fn]
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [data (js->clj data)
                el   (id->el (str tab-handle "-input"))]
            (.set js/chrome.storage.sync
                  (clj->js (assoc-in data ["watchlist" tab-handle]
                                     (val-fn (.-value el))))
                  #(update-status tab-handle))))))

(defmulti save-options! (fn [tab-handle] tab-handle))

(defmethod save-options! :default [tab-handle]
  (save-options* tab-handle to-regex))

(defn populated-inputs [root]
  (let [filter-fn (fn [node]
                    (if (and (contains? #{"text" "textarea"}
                                        (.-type node))
                             (not (empty? (.-value node))))
                      NodeFilter.FILTER_ACCEPT
                      NodeFilter.FILTER_SKIP))
        walker (.createTreeWalker js/document
                                  root
                                  NodeFilter.SHOW_ELEMENT
                                  #js {:acceptNode filter-fn})]
    (->> (take-while some? (repeatedly #(.nextNode walker)))
         (map #(.-value %))
         (partition-all 2))))

(defmethod save-options! "terms" [tab-handle]
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (let [data (js->clj data)
                final (reduce
                       (fn [acc [category terms]]
                         (merge-with #(str %1 "|" %2)
                                     acc
                                     {category (to-regex terms)}))
                       {}
                       (populated-inputs (id->el "terms-form")))]
            (.set js/chrome.storage.sync
                  (clj->js (assoc-in data ["watchlist" "terms"] final))
                  #(update-status tab-handle))))))

(defmethod save-options! "styles" [tab-handle]
  (save-options* tab-handle identity))

(defmulti fill-in-options! (fn [k v] k))

(defmethod fill-in-options! :default [k v]
  (throw (js/Exception (str "Unknown option key " k))))

(defn clone-template!
  ([] (clone-template! (gensym) ""))
  ([category terms]
   (let [terms (to-text terms)
         template (id->el "terms-template")
         new-set (doto (.cloneNode template true)
                   (#(set! (.-id %) (str "terms-" category))))
         walker   (.createTreeWalker js/document
                                     new-set
                                     NodeFilter.SHOW_ELEMENT)]
     (doseq [el (take-while some? (repeatedly #(.nextNode walker)))]
       (set! (.-id el) (str (.-id el) "-" category)))
     (.insertBefore (.-parentNode template) new-set (id->el "terms-controls"))
     (when (not-empty terms)
       (set-val!  (str "terms-input-category-" category) category)
       (set-html! (str "terms-input-terms-" category)    (to-text terms))))))

(defmethod fill-in-options! "terms" [_ term-data]
  (doseq [[category terms] (sort term-data)]
    (clone-template! category terms)))

(defmethod fill-in-options! "blacklist" [_ blacklist]
  (when blacklist (set-val! "blacklist-input" (to-text blacklist))))

(defmethod fill-in-options! "styles" [_ styles]
  (when styles (set-val! "styles-input" styles)))

(defn apply-defaults [data]
  (let [options (get data "watchlist")]
    (if (get options "terms")
      options
      (assoc-in options ["terms" "default"] ""))))

(defn retrieve-options! []
  (.get js/chrome.storage.sync "watchlist"
        (fn [data]
          (doseq [[k v] (apply-defaults (js->clj data))]
            (fill-in-options! k v)))))

(def tab-handles  #{"terms" "blacklist" "styles"})
(def tab-suffixes #{"-tab" "-form"})

(defn classes+ [node class] (str     (.-className node) " " class))
(defn classes- [node class] (replace (.-className node) class ""))

(defn add-class! [node class] (set! (.-className node) (classes+ node class)))
(defn del-class! [node class] (set! (.-className node) (classes- node class)))

(defn assign-click-handlers! []
  (doseq [handle tab-handles]
    (set! (.-onclick (id->el (str handle "-tab")))
          (fn [e]
            (doseq [handle tab-handles
                    suffix tab-suffixes]
              (del-class! (id->el (str handle suffix)) "active"))
            (doseq [suffix tab-suffixes]
             (add-class! (id->el (str handle suffix))  "active"))))))

(defn save-handlers! []
  (doseq [tab-handle #{"terms" "blacklist" "styles"}]
    (set! (.-onclick (id->el (str tab-handle "-save")))
          (partial save-options! tab-handle))))

(defn assign-add-term-set-handler! []
  (set! (.-onclick (id->el "terms-add-set"))
        (fn [e] (clone-template!))))

(defn main []
  (retrieve-options!)
  (assign-click-handlers!)
  (save-handlers!)
  (assign-add-term-set-handler!))

(main)
