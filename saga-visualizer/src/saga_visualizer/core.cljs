(ns saga-visualizer.core
  (:require [clojure.browser.repl :as repl]
            [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce pos-offset 10)
(defonce node-initial-pos (reagent/atom {:x 100, :y 150}))
(defonce request-json (reagent/atom {}))
(defonce current-request (reagent/atom (reagent/atom nil)))
(defonce dragging? (reagent/atom false))
(defonce dragging-request (reagent/atom nil))
(defonce lines (reagent/atom {}))
(defonce connector-positions (reagent/atom {}))

(defn json-ui []
  [:div {:class "modal-content"}
   [:h4 {:class "modal-header"} "Request JSON"]
   [:textarea
    {:class     "form-control"
     :value     (.stringify js/JSON (clj->js (map #(dissoc % :uuid) (vals @request-json))) nil 2)
     :read-only true
     :rows      25}]])

(defonce request-count (reagent/atom 0))

(defn request-icon []
  (reagent/with-let [border-color (reagent/atom "gray")]
                    [:rect {
                            :on-click       (fn [_]
                                              (swap! request-count inc)
                                              (reset! node-initial-pos {:x (+ (:x @node-initial-pos) pos-offset)
                                                                        :y (+ (:y @node-initial-pos) pos-offset)}))
                            :on-mouse-over  #(reset! border-color "black")
                            :on-mouse-leave #(reset! border-color "gray")
                            :width          30
                            :height         20
                            :x              10
                            :stroke         @border-color
                            :fill           "gray"}]))

(defn global-coordinate [elem]
  (let [svg (.getElementById js/document "request-graph-svg")
        point (.createSVGPoint svg)]
    (set! (.-x point) (.-baseVal.value (.-cx elem)))
    (set! (.-y point) (.-baseVal.value (.-cy elem)))
    (.matrixTransform point (.getCTM elem))))

(defn position [elem]
  (let [coordinate (global-coordinate elem)]
    {:x (.-x coordinate)
     :y (.-y coordinate)}))

(defn draw-line [start-pos end-pos]
  [:polyline {:points       (str (:x start-pos) "," (:y start-pos) " " (:x end-pos) "," (:y end-pos))
              :fill         "none"
              :stroke-width "2"
              :stroke       "black"
              :marker-end   "url(#Triangle)"}
   ])

(defn line [start end]
  (let [start-pos (@connector-positions start)
        end-pos (@connector-positions end)]
    [draw-line start-pos end-pos]))

(defn request-field [field]
  [:div {:class "col-sm-9"}
   [:input {:type      "text"
            :on-change (fn [evt]
                         (let [content (.-value (.-target evt))]
                           (swap! @current-request assoc field content)
                           (swap! request-json assoc (:uuid (deref @current-request)) (deref @current-request))))
            :value     (field (deref @current-request))}]])

(defn label [description]
  [:span {:class "col-sm-3 col-form-label"}
   description])

(defn request-row [description field]
  [:div {:class "form-group row"}
   [label description]
   [request-field field]])

(defn request-detail []
  [:div {:class "modal-content"}
   [:div {:class "modal-header"}
    [:h4 {:class "modal-title"} "Request Details"]]
   [:div {:class "modal-body"}
    [request-row "Request ID" :id]
    [request-row "Service Name" :serviceName]
    [request-row "Path" :path]]])


(defn elem-by-id [id]
  (let [svg (.getElementById js/document "request-graph-svg")]
    (.getElementById svg id)))

(defn connector [id request pos visible?]
  (reagent/with-let [border-color (reagent/atom "black")]
                    [:circle {:id             id
                              :on-mouse-over  #(reset! border-color "green")
                              :on-mouse-leave #(reset! border-color "black")
                              :on-mouse-down  (fn [evt]
                                                (reset! dragging? true)
                                                (reset! dragging-request {:connector-id id
                                                                          :request      @request})
                                                (swap! connector-positions assoc id (position (elem-by-id id))))
                              :on-mouse-up    (fn [evt]
                                                (when (not (nil? dragging-request))
                                                  (swap! request assoc :parents (set (filter #(not= % (:id @request))
                                                                                             (conj (:parents @request) (:id (:request @dragging-request))))))
                                                  (swap! request-json assoc (:uuid @request) @request)
                                                  (swap! connector-positions assoc id (position (elem-by-id id)))
                                                  (swap! lines assoc (str (:id (:request @dragging-request)) "-" (:id @request))
                                                         {:start-pos (:connector-id @dragging-request)
                                                          :end-pos   id})
                                                  (reset! dragging-request nil)))
                              :cx             (:x pos)
                              :cy             (:y pos)
                              :r              6
                              :stroke         @border-color
                              :visibility     (if visible?
                                                "visible"
                                                "hidden")
                              :fill           "white"}]))

(defn request-node []
  (reagent/with-let [uuid (.toString (random-uuid))
                     connector-ids {:top    (str uuid "-top")
                                    :bottom (str uuid "-bottom")
                                    :left   (str uuid "-left")
                                    :right  (str uuid "-right")}
                     request (reagent/atom {:uuid uuid :serviceName "some service" :parents (set '())})
                     selected? (reagent/atom false)
                     mouse-over? (reagent/atom false)
                     border-color (reagent/atom "black")
                     z-index (reagent/atom '())
                     start-pos (reagent/atom {:x (:x @node-initial-pos) :y (:y @node-initial-pos)})
                     current-pos (reagent/atom {:x (:x @node-initial-pos) :y (:y @node-initial-pos)})]
                    [:g {:id             uuid
                         :transform      (str "translate(" (:x @current-pos) "," (:y @current-pos) ")")
                         :on-mouse-over  #(reset! mouse-over? true)
                         :on-mouse-leave #(reset! mouse-over? false)}
                     [:rect {:id             uuid
                             :on-mouse-over  (fn [_]
                                               (reset! border-color "red"))
                             :on-mouse-up    (fn [_]
                                               (reset! selected? false)
                                               (reset! current-request request)
                                               (swap! z-index #(drop 1 %)))
                             :on-mouse-leave (fn [_]
                                               (reset! selected? false)
                                               (swap! z-index #(drop 1 %))
                                               (reset! border-color "black"))
                             :on-mouse-down  (fn [evt]
                                               (reset! selected? true)
                                               (when (empty? @z-index)
                                                 (swap! z-index conj (.-zIndex (.-target evt))))
                                               (swap! z-index conj 2147483647)
                                               (swap! start-pos assoc :x (.-clientX evt) :y (.-clientY evt)))
                             :on-mouse-move  (fn [evt]
                                               (when @selected?
                                                 (swap! current-pos assoc
                                                        :x (- (+ (:x @current-pos) (.-clientX evt)) (:x @start-pos))
                                                        :y (- (+ (:y @current-pos) (.-clientY evt)) (:y @start-pos)))
                                                 (swap! start-pos assoc :x (.-clientX evt) :y (.-clientY evt))
                                                 (doseq [id (keys @connector-positions)]
                                                   (swap! connector-positions assoc id (position (elem-by-id id))))))
                             :width          100
                             :height         64
                             :stroke         @border-color
                             :fill           "white"}]
                     [:text
                      {:x (/ (- 100 (* 7 (count (:serviceName @request)))) 2)
                       :y 32}
                      (:serviceName @request)]
                     [connector (:top connector-ids) request {:x 50 :y 0} @mouse-over?]
                     [connector (:bottom connector-ids) request {:x 50 :y 64} @mouse-over?]
                     [connector (:left connector-ids) request {:x 0 :y 32} @mouse-over?]
                     [connector (:right connector-ids) request {:x 100 :y 32} @mouse-over?]]
                    ))

(defn draw-lines []
  (when (not (empty? @lines)))
  (for [l (vals @lines)]
    [line (:start-pos l) (:end-pos l)]))

(defn arrow-head []
  [:defs
   [:marker {:id           "Triangle"
             :viewBox      "0 0 10 10"
             :refX         "10"
             :refY         "5"
             :markerWidth  "6"
             :markerHeight "6"
             :orient       "auto"}
    [:path {:d "M 0 0 L 10 5 L 0 10 z"}]]])

(defn absolute-pos [elem]
  (let [rect (.getBoundingClientRect elem)]
    {:x (.-left rect)
     :y (.-top rect)})
  )

(defn draw-dragging-line []
  (reagent/with-let [
                     arrow-end (reagent/atom nil)
                     update-arrow-end (fn [e]
                                        (when @dragging?
                                          (let [svg-pos (absolute-pos (.getElementById js/document "request-graph-svg"))]
                                            (swap! arrow-end assoc
                                                   :x (- (.-pageX e) (:x svg-pos))
                                                   :y (- (.-pageY e) (:y svg-pos))))))
                     clean-arrow-end (fn [_]
                                       (reset! dragging? false)
                                       (reset! arrow-end nil))
                     _ (.addEventListener js/document "mousemove" update-arrow-end)
                     _ (.addEventListener js/document "mouseup" clean-arrow-end)]
                    (when (and (not (nil? @arrow-end)) @dragging?)
                      (let [pos (position (elem-by-id (:connector-id @dragging-request)))]
                        (draw-line pos @arrow-end)))
                    (finally
                      (.removeEventListener js/document "mousemove" update-arrow-end)
                      (.removeEventListener js/document "mouseup" clean-arrow-end)))
  )

(defn graph-ui []
  [:div {:id "request_graph"}
   [:h2 "Graph"]
   (-> [:svg {:id     "request-graph-svg"
              :style  {:background-color "#e6e9ef"}
              :width  "100%"
              :height 480}
        [arrow-head]
        [request-icon]
        [draw-dragging-line]]
       (into (repeat @request-count [request-node]))
       (into (draw-lines)))
   [request-detail]]
  )


(defn interface []
  [:div
   [graph-ui]
   [json-ui]])

(reagent/render-component [interface]
                          (.getElementById js/document "dynamic"))
