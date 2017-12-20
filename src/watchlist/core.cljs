(ns watchlist.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.db :refer [app-db]]
            [goog.dom]
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

(defn viewport-height-ratio []
  ;; buh
  ;; https://github.com/jquery/jquery/blob/d71f6a53927ad02d728503385d15539b73d21ac8/src/dimensions.js#L23-L29
  (let [data {:doc-client-height (.. js/window -document -documentElement -clientHeight)
              :doc-offset-height (.. js/window -document -documentElement -offsetHeight)
              :doc-scroll-height (.. js/window -document -documentElement -scrollHeight)
              :bod-client-height (.-clientHeight (.querySelector js/document "body"))
              :bod-offset-height (.-offsetHeight (.querySelector js/document "body"))
              :bod-scroll-height (.-scrollHeight (.querySelector js/document "body"))}
        sorted (sort-by (fn [[k v]] v) data)
        [dmin-k dmin-v] (first sorted)
        [dmax-k dmax-v] (last sorted)]
    (/ dmin-v dmax-v)))

(defn draw-blip-bar [matches]
  (when-let [old-blip-bar (.querySelector js/document "#watchlist-blip-bar")]
    (.removeChild (.-parentElement old-blip-bar) old-blip-bar))
  (let [blip-bar (.createDom goog.dom "canvas" #js {"id" "watchlist-blip-bar"})]
    (set! (.-width blip-bar) 16)
    (set! (.-height blip-bar) (.-clientHeight (.querySelector js/document "body")))
    (.appendChild (.querySelector js/document "body") blip-bar)
    (let [ratio (viewport-height-ratio)
          scroll-y (.-scrollY js/window)
          width (.-clientWidth blip-bar)
          ctx (.getContext blip-bar "2d")]
      (doseq [{node :node} matches]
        (let [node-top (.-top (.getBoundingClientRect node))
              node-bgcolor (.getPropertyValue (.getComputedStyle js/window node) "background-color")
              start-y (.round js/Math (* (+ node-top scroll-y) ratio))]
          (set! (.-fillStyle ctx) "#999")
          (.fillRect ctx 0 start-y width 4)
          (set! (.-fillStyle ctx) node-bgcolor)
          (.fillRect ctx 0.5 (+ start-y 0.5) (- width 1) 3))))))

(defn statusbar [styles]
  (let [matches (subscribe [:matches])
        started (subscribe [:started])]
    (fn []
      (when (not-empty @matches)
        (draw-blip-bar @matches)
        [:div {:id "watchlist-status-bar" :class "watchlist-emphasized"}
         (when styles
           [:style {:type "text/css"} styles])
         [:span {:class "watchlist-status-bar-item" :id "watchlist-title"} "Watchlist"]
         (doall
          (for [[group-term hits] (sort-by key (group-by :term @matches))]
            ^{:key group-term} [display-match group-term hits @started]))]))))

(declare observer)

(defn go [{:keys [styles] :as options}]
  ;; The need to redraw the page on subsequent changes adds
  ;; complexity. Observing while redrawing means potentially
  ;; triggering changes due to our own changes. Races within/around
  ;; re-frame require more ad-hoc coordination than pleasant --
  ;; removing previous matches from DB (so we don't do an early draw
  ;; of statusbar with old matches).
  (.disconnect @observer)
  (dispatch [:started])
  (swap! app-db dissoc :matches)
  (dispatch [:initialize options])
  (when-let [old-wrapper (.querySelector js/document "#watchlist-wrapper")]
    (reagent/unmount-component-at-node (.querySelector js/document "#watchlist-status-root"))
    (.removeChild (.-parentElement old-wrapper) old-wrapper))
  ;; Style must be written outside of render call so that it's available to blip bar.
  (let [style    (.createDom goog.dom "style" #js {:type "text/css"} styles)
        app-root (.createDom goog.dom "div" #js {:id "watchlist-status-root"})
        wrapper  (.createDom goog.dom
                             "div"
                             #js {:id "watchlist-wrapper"}
                             style app-root)]
    (.appendChild (.querySelector js/document "body") wrapper)
    (reagent/render [statusbar styles] app-root)
    (.observe @observer
              (.querySelector js/document "body")
              #js {:childList true
                   :attributes true
                   :characterData true
                   :subtree true})))

(def ratio (atom nil))
(def observer (atom nil))

(defn url-allowed? [blacklist]
  (not (and (not (empty? blacklist))
            (re-find (re-pattern blacklist)
                     (.-URL js/document)))))

(defn ^:export init! [{:keys [terms blacklist styles] :as options}]
  (when (and terms (url-allowed? blacklist))
    ;; Observe mutations so we can re-draw if the page changes.
    (reset! ratio (viewport-height-ratio))
    (reset! observer (js/window.MutationObserver.
                      (fn [mutation-list observer]
                        (let [ratio' (viewport-height-ratio)]
                          (if (not= ratio' @ratio)
                            (do (reset! ratio ratio')
                                (go options)))))))
    (go options)))
