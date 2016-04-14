(ns chromex-sample.content-script.core
  (:require-macros [reagent.ratom :refer [reaction]]
                   [content-script.chromex-sample.content-script.macros :refer [inspect]])
  (:require [reagent.core :as reagent]
            [goog.dom]
            [chromex-sample.content-script.nodes :as nodes]
            [re-frame.core :refer [register-handler
                                   register-sub
                                   dispatch
                                   subscribe]]))

(register-sub
 :db
 (fn [db _]
   (reaction @db)))


(register-handler
 :initialize
 (fn [db [_ option-data]]
   (let [matches (nodes/highlight-matches! (:terms option-data))]
     (assoc db :matches matches))))

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
