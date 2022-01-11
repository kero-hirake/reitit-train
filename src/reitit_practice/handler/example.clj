(ns reitit-practice.handler.example
  (:require [reitit.core :as r]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]))

;; Quick start
(def router
  (r/router
   [["/api/ping" ::ping]
    ["/api/orders/:id" ::order]]))

(r/match-by-path router "/api/ping")
#_#reitit.core.Match{:template "/api/ping"
                     :data {:name :reitit-practice.handler.example/ping}
                     :result nil
                     :path-params {}
                     :path "/api/ping"}

(r/match-by-name router ::order {:id 2})
#_#reitit.core.Match{:template "/api/orders/:id"
                     :data {:name :reitit-practice.handler.example/order}
                     :result nil
                     :path-params {:id "2"}
                     :path "/api/orders/2"}

;; Ring Example
(def app
  (ring/ring-handler
   (ring/router
    ["/api"
     ["/math" {:get {:parameters {:query {:x int?, :y int?}}
                     :responses {200 {:body {:total int?}}}
                     :handler   (fn [{{{:keys [x y]} :query} :parameters}]
                                  {:status 200
                                   :body {:total (+ x y)}})}}]]
    {:data {:coercion   reitit.coercion.spec/coercion
            :muuntaja   m/instance
            :middleware [parameters/parameters-middleware
                         rrc/coerce-request-middleware
                         muuntaja/format-response-middleware
                         rrc/coerce-response-middleware]}})))



(app {:request-method :get
      :uri "/api/math"
      :query-params {:x "1", :y "2"}})
#_{:status 200, 
 :body #object[java.io.ByteArrayInputStream 0x11a186fe "java.io.ByteArrayInputStream@11a186fe"], 
 :headers {"Content-Type" "application/json; charset=utf-8"}}
(-> (app {:request-method :get
          :uri "/api/math"
          :query-params {:x "1", :y "2"}})
    :body
    slurp)
; "{\"total\":3}"

(app {:request-method :get
      :uri "/api/math"
      :query-params {:x "1", :y "a"}})
; Execution error (ExceptionInfo) at reitit.coercion/request-coercion-failed! (coercion.cljc:46).
; Request coercion failed: 
#_ #reitit.coercion.CoercionError{:spec #Spec{:form (clojure.spec.alpha/keys 
                                                  :req-un [:spec$13985/x 
                                                           :spec$13985/y]), 
                                           :type :map, 
                                           :leaf? false}, 
                               :problems #:clojure.spec.alpha{
                                                              :problems ({:path [:y], 
                                                                          :pred clojure.core/int?, 
                                                                          :val "a", 
                                                                          :via [:spec$13985/y], 
                                                                          :in [:y]}), 
                                                              :spec #Spec{:form (clojure.spec.alpha/keys 
                                                                                 :req-un [:spec$13985/x :spec$13985/y]), 
                                                                          :type :map, 
                                                                          :leaf? false}, 
                                                              :value {:y "a", :x 1}}}
;サンプルでは400が返っているが...

;; Document
;; Introduction
;ring router
(defn handler [_]
  {:status 200
   :body "ok"})

(defn wrap [handler id]
  (fn [request]
    (update (handler request) :wrap (fnil conj '()) id))) ;fnil 関数fを受け取り、fを呼び出す関数で、fの第1引数のnilを与えられた値xで置き換える。

(def app
  (ring/ring-handler
   (ring/router
    ["/api" {:middleware [[wrap :api]]}
     ["/ping" {:get handler
               :name ::ping}]
     ["/admin" {:middleware [[wrap :admin]]}
      ["/users" {:get handler
                 :post handler}]]])))
;routing
(app {:request-method :get, :uri "/api/admin/users"})
#_{:status 200, 
 :body "ok", 
 :wrap (:api :admin)}

(app {:request-method :put, :uri "/api/admin/users"})
;nil

;reverse routing
(-> app
    ring/get-router
    (r/match-by-name ::ping))
#_ #reitit.core.Match{:template "/api/ping", 
                   :data {:middleware [[#function[reitit-practice.handler.example/wrap] :api]], 
                          :get {:handler #function[reitit-practice.handler.example/handler]}, 
                          :name :reitit-practice.handler.example/ping}, 
                   :result #reitit.ring.Methods{:get #reitit.ring.Endpoint{:data {:middleware [[#function[reitit-practice.handler.example/wrap] :api]], 
                                                                                  :name :reitit-practice.handler.example/ping, 
                                                                                  :handler #function[reitit-practice.handler.example/handler]}, 
                                                                           :handler #function[reitit-practice.handler.example/wrap/fn--14035], 
                                                                           :path "/api/ping", 
                                                                           :method :get, 
                                                                           :middleware [#reitit.middleware.Middleware{:name nil, 
                                                                                                                      :wrap #function[reitit.middleware/eval10630/fn--10632/fn--10637], 
                                                                                                                      :spec nil}]}, 
                                                :head nil, 
                                                :post nil, 
                                                :put nil, 
                                                :delete nil, 
                                                :connect nil, 
                                                :options #reitit.ring.Endpoint{:data {:middleware [[#function[reitit-practice.handler.example/wrap] :api]], 
                                                                                      :name :reitit-practice.handler.example/ping, 
                                                                                      :no-doc true, 
                                                                                      :handler #function[reitit.ring/fn--10823/fn--10832]},
                                                                               :handler #function[reitit-practice.handler.example/wrap/fn--14035], 
                                                                               :path "/api/ping", 
                                                                               :method :options, 
                                                                               :middleware [#reitit.middleware.Middleware{:name nil, 
                                                                                                                          :wrap #function[reitit.middleware/eval10630/fn--10632/fn--10637], 
                                                                                                                          :spec nil}]}, 
                                                :trace nil, 
                                                :patch nil}, 
                                                :path-params nil, 
                                                :path "/api/ping"}
