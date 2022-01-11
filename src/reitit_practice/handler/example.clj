(ns reitit-practice.handler.example
  (:require [reitit.core :as r]))


  (def router
    (r/router
     [["/api/ping" ::ping]
      ["/api/orders/:id" ::order]]))

  (r/match-by-path router "/api/ping")
  #_ #reitit.core.Match{:template "/api/ping", 
                     :data {:name :reitit-practice.handler.example/ping}, 
                     :result nil, 
                     :path-params {}, 
                     :path "/api/ping"}

  (r/match-by-name router ::order {:id 2})
  #_#reitit.core.Match{:template "/api/orders/:id", 
                     :data {:name :reitit-practice.handler.example/order}, 
                     :result nil, 
                     :path-params {:id "2"}, 
                     :path "/api/orders/2"}


