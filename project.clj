(defproject org.clojars.simon_brooke/internationalisation "1.0.3"
  :description "Internationalisation library for Clojure"
  :url "https://github.com/simon-brooke/internationalisation"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [instaparse "1.4.7"]]
  :plugins [[lein-codox "0.10.3"]]
  :profiles {:dev {:resource-paths ["resources"]}}
  :lein-release {:deploy-via :clojars}
  :signing {:gpg-key "Simon Brooke (Stultus in monte) <simon@journeyman.cc>"}
  )
