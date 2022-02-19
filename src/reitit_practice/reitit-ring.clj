(ns reitit-practice.reitit-ring
  (:require 
   [reitit.core :as r]
   [reitit.ring :as ring]))

;;----------------------------------------
;; Ring Router
;;----------------------------------------
;; reitit.ring/ring-router
;ring-routerは高次ルーターであり、：request-method ベースの
;ルーティング、ハンドラー、およびミドルウェアのサポートを追加します。

(defn handler [_]
  {:status 200
   :body "ok"})

(def router
  (ring/router
   ["/ping" {:get handler}]))


(-> (r/match-by-path router "/ping")
    clojure.pprint/pprint )
;=>
#_{:template "/ping",
 :data
 {:get {:handler #function[reitit-practice.reitit-ring/handler]}},
 :result
 {:get
  {:data {:handler #function[reitit-practice.reitit-ring/handler]},
   :handler #function[reitit-practice.reitit-ring/handler],
   :path "/ping",
   :method :get,
   :middleware []},
  :head nil,
  :post nil,
  :put nil,
  :delete nil,
  :connect nil,
  :options
  {:data
   {:no-doc true, :handler #function[reitit.ring/fn--9339/fn--9348]},
   :handler #function[reitit.ring/fn--9339/fn--9348],
   :path "/ping",
   :method :options,
   :middleware []},
  :trace nil,
  :patch nil},
 :path-params {},
 :path "/ping"}


;;reitit.ring/ring-handler
;ring-routerオプションのdefault-handler＆optionsを指定すると、ring-handler関数は、
;同期と非同期の両方の要求処理をサポートする有効なリングハンドラーを返します。
(def app (ring/ring-handler router))
(app {:request-method :get
      :uri "/favicon.ico"}) ;=> nil
(app {:request-method :get
      :uri "/ping"}) ;=> {:status 200, :body "ok"}

;ルーターには、get-routerを介してアクセスできます。
(-> app 
    ring/get-router 
    r/compiled-routes
    clojure.pprint/pprint )


;;----------------------------------------
;; Request-based method routing 
;;----------------------------------------
;ハンドラーは、トップレベル（すべてのメソッド）または特定のメソッドの下に配置できます。
;リクエストメソッドベースのハンドラーが見つからない場合は、トップレベルハンドラーが使用されます。

;デフォルトでは、：optionsルートがすべてのパスに対して生成されます。-CORSのようなものを有効にします.
;CORS オリジン間リソース共有 

(def app
  (ring/ring-handler
   (ring/router
    [["/all" handler]
     ["/ping" {:name ::ping
               :get handler
               :post handler}]])))

;トップレベルハンドラーはすべてのメソッドをキャッチします。
(app {:request-method :delete
      :uri "/all"}) ;=> {:status 200, :body "ok"}

;メソッドレベルのハンドラーは、メソッドのみをキャッチします。
(app {:request-method :get, :uri "/ping"}) ;=> {:status 200, :body "ok"}
(app {:request-method :put, :uri "/ping"}) ;=> nil

;デフォルトでは、：optionsもサポートされています
; OPTIONSメソッドは、対象リソースの通信オプションを記述するために使用します。
; クライアントは OPTIONS メソッドの URL を指定するか、サーバー全体を参照するアスタリスク（*）を指定することができます。
; curl -X OPTIONS http://example.org -i
; レスポンスには、許可されているメソッドを含んだ Allow ヘッダーが含まれます。
; HTTP/1.1 204 No Content
; Allow: OPTIONS, GET, HEAD, POST
; Cache-Control: max-age=604800
; Date: Thu, 13 Oct 2016 11:45:00 GMT
; Expires: Thu, 20 Oct 2016 11:45:00 GMT
; Server: EOS (lax004/2813)
; x-ec-custom-error: 1

(app {:request-method :options, :uri "/ping"}) ;=> {:status 200, :body "", :headers {"Allow" "GET,POST,OPTIONS"}}

;name based reverse routing
(-> app
    ring/get-router
    (r/match-by-name ::ping)
    (r/match->path)
    ;clojure.pprint/pprint
    ) ;=> "/ping"

;;----------------------------------------
;; Middleware
;;----------------------------------------
;ミドルウェアは、：middlewareキーを使用して、トップレベルまたはリクエストメソッドサブマップの下にマウントできます。
;その値は、reitit.middleware/IntoMiddleware値のベクトルである必要があります。

(defn wrap [handler id]
  (fn [request]
    (handler (update request ::acc (fnil conj []) id))))

(defn handler [{::keys [acc]}]
  {:status 200, :body (conj acc :handler)})

(defn handler2 [_]
  {:status 200, :body "ok"})

(handler {:xyz/acc [:abc]}) ;=>{:status 200, :body (:handler)}
(handler {::acc [:abc]}) ;=>{:status 200, :body [:abc :handler]}
(handler {:reitit-practice.reitit-ring/acc [:abc]}) ;=> {:status 200, :body [:abc :handler]}

((wrap handler :abc) {}) ;=>{:status 200, :body [:abc :handler]}

;ネストされたミドルウェアを備えたapp
(def app
  (ring/ring-handler
   (ring/router
    ;; a middlwware function
    ["/api" {:middleware [#(wrap % :api)]}
     ["/ping" handler]
     ;a middlweare vector at top level
     ["/admin" {:middleware [[wrap :admin]]}
      ["/db" {:middleware [[wrap :db]]
              ;a middleware vector at  under a method
              :delete {:middleware [[wrap :delete]]
                       :handler handler}}]]])))

(app {:request-method :delete :uri "/api/ping"})
;=> {:status 200, :body [:api :handler]}
(app {:request-method :delete :uri "/api/admin/db"})
;=> {:status 200, :body [:api :admin :db :delete :handler]}


;ルーティングが行われる前に適用されるトップレベルのミドルウェア
(def app
  (ring/ring-handler
   (ring/router
    ["/api" {:middleware [[wrap :api]]}
     ["/get" {:get handler}]])
   nil
   {:middleware [[wrap :top]]}))

(app {:request-method :get, :uri "/api/get"})
;{:status 200, :body [:top :api :handler]}



(-> app
    ring/get-router
    r/compiled-routes
    clojure.pprint/pprint)
;=>
#_[["/all"
  {:handler #function[reitit-practice.reitit-ring/handler]}
  {:get
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :get,
    :middleware []},
   :head
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :head,
    :middleware []},
   :post
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :post,
    :middleware []},
   :put
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :put,
    :middleware []},
   :delete
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
 
   :path "/all",
    :method :delete,
    :middleware []},
   :connect
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :connect,
    :middleware []},
   :options
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :options,
    :middleware []},
   :trace
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :trace,
    :middleware []},
   :patch
   {:data {:handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/all",
    :method :patch,
    :middleware []}}]
 ["/ping"
  {:name :reitit-practice.reitit-ring/ping,
   :get {:handler #function[reitit-practice.reitit-ring/handler]},
   :post
   {:handler #function[reitit-practice.reitit-ring/handler]}}
  {:get
   {:data
    {:name :reitit-practice.reitit-ring/ping,
     :handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/ping",
    :method :get,
    :middleware []},
   :head nil,
   :post
   {:data
    {:name :reitit-practice.reitit-ring/ping,
     :handler #function[reitit-practice.reitit-ring/handler]},
    :handler #function[reitit-practice.reitit-ring/handler],
    :path "/ping",
    :method :post,
    :middleware []},
   :put nil,
   :delete nil,
   :connect nil,
   :options
   {:data
    {:name :reitit-practice.reitit-ring/ping,
     :no-doc true,
     :handler #function[reitit.ring/fn--9339/fn--9348]},
    :handler #function[reitit.ring/fn--9339/fn--9348],
    :path "/ping",
    :method :options,
    :middleware []},
   :trace nil,
   :patch nil}]]

