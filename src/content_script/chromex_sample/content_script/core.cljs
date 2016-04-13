(ns chromex-sample.content-script.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [content-script.chromex-sample.content-script.macros :refer [inspect]])
  (:require [reagent.core :as reagent]
            [goog.dom]
            [clojure.string :as s]
            [re-frame.core :refer [register-handler
                                   register-sub
                                   dispatch
                                   subscribe]]))

(register-sub
 :db
 (fn [db _]
   (reaction @db)))

(defn node-seq []
  (let [tree (.createTreeWalker js/document
                                (.-body js/document)
                                NodeFilter.SHOW_TEXT)]
    (take-while some? (repeatedly #(.nextNode tree)))))

(defn text-node [text] (.createTextNode js/document text))

(defn mark-text-node [text]
  (.createDom goog.dom "mark"
              (clj->js {:class "watchlist-highlight"})
              (.createTextNode goog.dom text)))

(defn text [s] {:type :text :body s})
(defn html [s] {:type :html :body s})

(defn text-gap [matches s start end]
  (if (< start end)
    (conj matches (text (subs s start end)))
    matches))

(defn replace-matches
  [re s]
  ;; Needs to make its own g regexp?
  (reduce 
   (fn [{:keys [matches prev-idx] :as acc} [term _ :as match]] 
     (if term
       (-> acc
           (update :matches text-gap s prev-idx (.-index match))
           (update :matches conj (html term))
           (assoc  :prev-idx (+ (.-index match) (count term))))
       (reduced (text-gap matches s prev-idx (count s))))) 
   {:matches [] :prev-idx 0} 
   (repeatedly #(.exec re s))))

(defn new-nodes
  [{:keys [type body] :as instructions}]
  (case type
    :text {:node (text-node body)}
    :html {:node (mark-text-node body) :term body}))

(register-handler
 :initialize
 (fn [db [_ option-data]]
   (let [terms   (js/RegExp. (:terms option-data))
         terms-g (js/RegExp. (:terms option-data) "g")]
     (let [nodes   (filterv #(re-find terms (.-textContent %)) (node-seq))
           _ (.log js/console (str "nodes found" (count nodes)))
           new-kids (doall (for [node nodes
                                 :let [content (.-textContent node)]]
                             [node (map new-nodes (replace-matches terms-g content))]))
           _ (do (doseq [[node new-kid-seq] new-kids
                         kid new-kid-seq
                         :let [parent (.-parentNode node)]]
                   (.insertBefore parent (:node kid) node))
                 (doseq [[node _] new-kids]
                   (.removeChild (.-parentNode node) node)))
           matches (doall (for [mark (filter :term (mapcat second new-kids))]
                            {:term (str (:term mark)) :node (.-parentNode (:node mark))}))]
       (merge db {:started (.getTime (js/Date.))
                  :matches matches})))))

(defn display-match [group-term hits started]
  [:span {:class "watchlist-status-bar-item"
          :title (str (- (.getTime (js/Date.))
                         started)
                      " ms elapsed")}
   group-term ":" (count hits)])

(defn statusbar []
  (let [db (subscribe [:db])]
    (fn []
      [:div {:id "watchlist-status-bar" :class :loading}
       [:span {:class "watchlist-status-bar-item"} "Watchlist"]
       [:span {:class "watchlist-status-bar-separator"}]
       (doall
        (for [[group-term hits] (group-by :term (:matches @db))]
          ^{:key group-term} [display-match group-term hits (:started @db)]))])))

(defn url-allowed? [blacklist]
  (not (and (some? blacklist)
            (re-find (re-pattern blacklist)
                     (.-URL js/document)))))

(defn ^:export init! []
  (.get js/chrome.storage.sync "watchlist"
        #(when-let [options-data (:watchlist (js->clj % :keywordize-keys true))]
           (dispatch [:initialize options-data])
           (let [{:keys [terms blacklist]} options-data]
             (when (and terms (url-allowed? blacklist))
               (let [attrs (clj->js {"className" "watchlist-wrapper"})
                     app-root (.createDom goog.dom "div" attrs)]
                 (.appendChild (.querySelector js/document "body") app-root)
                 (reagent/render [statusbar] app-root)))))))
