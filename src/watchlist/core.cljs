(ns watchlist.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [common.dom :as dom]
            [watchlist.nodes :as nodes]
            [re-frame.core :refer [register-handler
                                   register-sub
                                   dispatch
                                   subscribe]]))

(register-sub :matches (fn [db _] (reaction (:matches @db))))
(register-sub :started (fn [db _] (reaction (:started @db))))

(register-handler
 :started
 (fn [db _]
   (assoc db :started (.getTime (js/Date.)))))

(register-handler
 :initialize
 (fn [db [_ option-data]]
   (let [matches (nodes/highlight-matches! (:parsed option-data))]
     (assoc db :matches matches))))


(defn mod-clicks-over-nodes
  [{:keys [term index] :as clicks} term-clicked nodes]
  (if (= term-clicked term)
    (mod (inc index) (count nodes))
    0))

(register-handler
 :clicked
 (fn [{:keys [clicks matches] :as db} [_ term-clicked]]
   (let [term-nodes (filter #(= term-clicked (:term %)) matches)
         index (mod-clicks-over-nodes clicks term-clicked term-nodes)]
     (nodes/scroll-to-node! (:node (nth term-nodes index)))
     (merge db {:clicks {:term term-clicked
                         :index index}}))))

(defn display-match [group-term hits started]
  (let [this (reagent/current-component)]
    [:span {:class (apply str "watchlist-status-bar-item "
                          (-> hits first :groups))
            :on-mouse-over (nodes/evt dom/add-class this "watchlist-item-hover")
            :on-mouse-out  (nodes/evt dom/del-class this "watchlist-item-hover")
            :on-mouse-down (nodes/evt dom/add-class this "watchlist-item-click")
            :on-mouse-up   (nodes/evt dom/del-class this "watchlist-item-click")
            :on-click      #(dispatch [:clicked group-term])
            :title         (str (- (.getTime (js/Date.)) started) " ms elapsed")}
     group-term
     [:span {:class "watchlist-status-bar-count"} (count hits)]]))

(defn statusbar []
  (let [matches (subscribe [:matches])
        started (subscribe [:started])]
    (fn []
      (when (not-empty @matches)
        [:div {:id "watchlist-status-bar" :class "watchlist-emphasized"}
         [:span {:class "watchlist-status-bar-item" :id "watchlist-title"} "Watchlist"]
         (doall
          (for [[group-term hits] (sort-by key (group-by :term @matches))]
            ^{:key group-term} [display-match group-term hits @started]))]))))

(defn url-allowed? [blacklist]
  (not (and (not (empty? blacklist))
            (re-find (re-pattern blacklist)
                     (.-URL js/document)))))

(defn ^:export init! [options]
  (let [{:keys [terms blacklist styles]} options]
    (when (and terms (url-allowed? blacklist))
      (dispatch [:started])
      (dispatch [:initialize options])
      (when (not-empty styles)
        (let [style (dom/mk-el "style" {"type" "text/css"} styles)]
          (.appendChild (.querySelector js/document "body") style)))
      (let [app-root (dom/mk-el "div" {"className" "watchlist-wrapper"})]
        (.appendChild (.querySelector js/document "body") app-root)
        (reagent/render [statusbar] app-root)))))
