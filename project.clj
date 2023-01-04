(defproject org.clojars.simon_brooke/internationalisation "1.0.4"
  :cloverage {:output "docs/cloverage"
              :codecov? true
              :emma-xml? true}
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs/codox"
          :source-uri "https://github.com/simon-brooke/internationalisation/blob/master/{filepath}#L{line}"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.taoensso/timbre "6.0.4"]
                 [instaparse "1.4.12"]
                 [trptr/java-wrapper "0.2.3"]]
  :description "Internationalisation library for Clojure"
  :lein-release {:deploy-via :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-cloverage "1.2.2"]
            [lein-codox "0.10.7"]]
  :profiles {:dev {:resource-paths ["resources"]}}
  :signing {:gpg-key "Simon Brooke (Stultus in monte) <simon@journeyman.cc>"}
  :url "https://github.com/simon-brooke/internationalisation")
