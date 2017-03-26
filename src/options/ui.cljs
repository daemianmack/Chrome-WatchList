(ns options.ui
  (:require [common.regex :as regex]
            [common.dom :as dom]
            [clojure.string :refer [split join replace upper-case]]
            [cljs.pprint :refer [pprint]]))

(enable-console-print!)

(defn to-simple-regex [s]
  (->> (split s #"\n")
       (filter not-empty)
       (join "|")))

(defn from-simple-regex [s]
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

(defn save-options* [tab-handle data & [fn]]
  (.set js/chrome.storage.sync
        (clj->js {tab-handle data})
        (when fn
          fn)))

(defn save-options-and-flash [tab-handle data]
  (save-options* tab-handle data #(update-status tab-handle)))

(defmulti save-options! (fn [tab-handle] tab-handle))

(defmethod save-options! :default [tab-handle]
  (throw (js/Exception (str "Unknown option key " tab-handle))))

(defn populated-inputs [root]
  (let [filter-fn (fn [node]
                    (if (and (contains? #{"text" "textarea"}
                                        (.-type node))
                             (not (empty? (.-value node))))
                      NodeFilter.FILTER_ACCEPT
                      NodeFilter.FILTER_SKIP))]
    (->> (dom/node-seq {:root root :filter filter-fn})
         (map #(.-value %))
         (partition-all 2))))

(defmethod save-options! "terms" [tab-handle]
  (let [terms  (populated-inputs (id->el "terms-form"))
        parsed (regex/->regex-str terms)]
    (save-options-and-flash tab-handle terms)
    (save-options* "parsed" parsed)))

(defmethod save-options! "blacklist" [tab-handle]
  (save-options-and-flash tab-handle (-> (str tab-handle "-input")
                                         id->el
                                         .-value
                                         to-simple-regex)))

(defmethod save-options! "styles" [tab-handle]
  (save-options-and-flash tab-handle
                          (-> (str tab-handle "-input")
                              id->el
                              .-value)))

(defmulti fill-in-options! (fn [k v] k))

(defmethod fill-in-options! :default [k v]
  (throw (js/Exception (str "Unknown option key " k))))

(defn clone-template-for! [template category]
  (let [new-set (doto (.cloneNode template true)
                  (#(set! (.-id %) (str "terms-" category))))]
    (doseq [el (dom/node-seq {:root new-set})]
      (set! (.-id el) (str (.-id el) "-" category)))
    new-set))

(defn clone-terms-template!
  ([] (clone-terms-template! (gensym) ""))
  ([category terms]
   (let [terms    (from-simple-regex terms)
         template (id->el "terms-template")
         new-set  (clone-template-for! template category)]
     (.insertBefore (.-parentNode template) new-set (id->el "terms-controls"))
     (when (not-empty terms)
       (set-val!  (str "terms-input-category-" category) category)
       (set-html! (str "terms-input-terms-" category)    (from-simple-regex terms))))))

(defmethod fill-in-options! "terms" [_ term-data]
  (doseq [[category terms] (sort term-data)]
    (clone-terms-template! category terms)))

(defmethod fill-in-options! "blacklist" [_ blacklist]
  (when blacklist (set-val! "blacklist-input" (from-simple-regex blacklist))))

(defmethod fill-in-options! "styles" [_ styles]
  (when styles (set-val! "styles-input" styles)))

(defmethod fill-in-options! "parsed" [_ _])

(defn apply-backward-compatibility [data]
  "0.6.0 simplified the data structure used to save options by
  removing a redundant top-level 'watchlist' key."
  (if-let [older-version-data (get data "watchlist")]
    (do (.remove js/chrome.storage.sync "watchlist"
                 (.set js/chrome.storage.sync
                       (clj->js older-version-data)
                       #(.log js/console "Ported Watchlist extension storage to 0.6.0 format.")))
        older-version-data)
    data))

(defn apply-defaults [data]
  (let [options (apply-backward-compatibility data)]
    (if (get options "terms")
      options
      (assoc-in options ["terms" "default"] ""))))

(defn retrieve-options! []
  (.get js/chrome.storage.sync nil
        (fn [data]
          (doseq [[k v] (apply-defaults (js->clj data))]
            (fill-in-options! k v)))))

(def tab-handles  #{"terms" "blacklist" "styles"})
(def tab-suffixes #{"-tab" "-form"})

(defn assign-click-handlers! []
  (doseq [handle tab-handles]
    (set! (.-onclick (id->el (str handle "-tab")))
          (fn [e]
            (doseq [handle tab-handles
                    suffix tab-suffixes]
              (dom/del-class (id->el (str handle suffix)) "active"))
            (doseq [suffix tab-suffixes]
             (dom/add-class (id->el (str handle suffix))  "active"))))))

(defn save-handlers! []
  (doseq [tab-handle #{"terms" "blacklist" "styles"}]
    (set! (.-onclick (id->el (str tab-handle "-save")))
          (partial save-options! tab-handle))))

(defn assign-add-term-set-handler! []
  (set! (.-onclick (id->el "terms-add-set"))
        (fn [e] (clone-terms-template!))))

(defn main []
  (retrieve-options!)
  (assign-click-handlers!)
  (save-handlers!)
  (assign-add-term-set-handler!))

(main)
