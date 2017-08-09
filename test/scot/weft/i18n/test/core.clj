(ns ^{:doc "Tests for Internationalisation."
      :author "Simon Brooke"} scot.weft.i18n.test.core
  (:require [clojure.test :refer :all]
            [scot.weft.i18n.core :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;
;;;; scot.weft.i18n: a simple internationalisation library for Clojure.
;;;;
;;;; This library is distributed under the Eclipse Licence in the hope
;;;; that it may be useful, but without guarantee.
;;;;
;;;; Copyright (C) 2017 Simon Brooke
;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(deftest test-generator
  (testing "Generating normalised maps from parse trees"
    (is
      (=
        0.6
        (generate-accept-languages
          [:Q-VALUE "0.6"])))
    (is
      (=
        {:language "en", :qualifier "*"}
        (generate-accept-languages
          [:PRIMARY-TAG "en"])))
    (is
      (=
        {:language "en", :qualifier "*"}
        (generate-accept-languages
          [:LANGUAGE-TAG
           [:PRIMARY-TAG "en"]])))
    (is
      (=
        {:language "en", :qualifier "*", :preference 1}
        (generate-accept-languages
          [:SPECIFIER
           [:LANGUAGE-TAG
            [:PRIMARY-TAG "en"]]])))
    (is
      (=
        {:language "en", :qualifier "GB", :preference 1}
        (generate-accept-languages
          [:SPECIFIER
           [:LANGUAGE-TAG
            [:PRIMARY-TAG "en"]
            "-"
            [:SUB-TAGS
             [:SUB-TAG "GB"]]]])))
    (is
      (=
        '({:language "en", :qualifier "GB", :preference 1})
        (generate-accept-languages
          [:SPECIFIERS
           [:SPECIFIER
            [:LANGUAGE-TAG
             [:PRIMARY-TAG "en"]
             "-"
             [:SUB-TAGS
              [:SUB-TAG "GB"]]]]])))
    (is
      (=
        '({:language "en", :qualifier "US", :preference 0.8}
                     {:language "en", :qualifier "*", :preference 0.6})
        (generate-accept-languages
          [:SPECIFIERS
           [:SPECIFIER
            [:LANGUAGE-TAG
             [:PRIMARY-TAG "en"]
             "-"
             [:SUB-TAGS
              [:SUB-TAG "US"]]]
            ";q="
            [:Q-VALUE "0.8"]]
           [:SPEC-SEP ","]
           [:SPECIFIERS
            [:SPECIFIER
             [:LANGUAGE-TAG
              [:PRIMARY-TAG "en"]]
             ";q="
             [:Q-VALUE "0.6"]]]])))
    (is
      (=
        '({:language "en", :qualifier "GB", :preference 1}
                     {:language "en", :qualifier "US", :preference 0.8}
                     {:language "en", :qualifier "*", :preference 0.6})
        (generate-accept-languages
          [:HEADER
           [:SPECIFIERS
            [:SPECIFIER
             [:LANGUAGE-TAG
              [:PRIMARY-TAG "en"]
              "-"
              [:SUB-TAGS
               [:SUB-TAG "GB"]]]]
            [:SPEC-SEP ","]
            [:SPECIFIERS
             [:SPECIFIER
              [:LANGUAGE-TAG
               [:PRIMARY-TAG "en"]
               "-"
               [:SUB-TAGS
                [:SUB-TAG "US"]]]
              ";q="
              [:Q-VALUE "0.8"]]
             [:SPEC-SEP ","]
             [:SPECIFIERS
              [:SPECIFIER
               [:LANGUAGE-TAG
                [:PRIMARY-TAG "en"]]
               ";q="
               [:Q-VALUE "0.6"]]]]]])))))

(deftest test-parser
  (testing "Generating parse trees from `Accept-Language` headers"
    (is
      (=
        [:HEADER
         [:SPECIFIERS
          [:SPECIFIER
           [:LANGUAGE-TAG
            [:PRIMARY-TAG "dk"]]]]]
        (parse-accept-language-header "dk"))
      "Should accept a basic language specifier.")
    (is
      (=
        [:HEADER
         [:SPECIFIERS
          [:SPECIFIER
           [:LANGUAGE-TAG
            [:PRIMARY-TAG "en"]
            "-"
            [:SUB-TAGS [:SUB-TAG "GB"]]]]]]
        (parse-accept-language-header "en-GB"))
      "Should accept a basic locale.")
    (is
      (=
        [:HEADER
         [:SPECIFIERS
          [:SPECIFIER
           [:LANGUAGE-TAG
            [:PRIMARY-TAG "en"]
            "-"
            [:SUB-TAGS [:SUB-TAG "GB"]]]
           [:Q-SEP ";q="]
           [:Q-VALUE "0.6"]]]]
        (parse-accept-language-header "en-GB;q=0.6"))
      "Should accept a q value.")
    (is
      (=
        [:HEADER
         [:SPECIFIERS
          [:SPECIFIER
           [:LANGUAGE-TAG
            [:PRIMARY-TAG "en"]
            "-"
            [:SUB-TAGS [:SUB-TAG "GB"]]]
           [:Q-SEP "; q="]
           [:Q-VALUE "0.6"]]]]
        (parse-accept-language-header "en-GB; q=0.6"))
      "Space after semi-colon should be tolerated.")
    (is
      (=
        [:HEADER
         [:SPECIFIERS
          [:SPECIFIER [:LANGUAGE-TAG [:PRIMARY-TAG "en"]]]
          [:SPEC-SEP ","]
          [:SPECIFIERS
           [:SPECIFIER [:LANGUAGE-TAG [:PRIMARY-TAG "fr"]]]]]]
        (parse-accept-language-header "en,fr"))
      "Should accept multiple specifications.")
    (is
      (=
        [:HEADER
         [:SPECIFIERS
          [:SPECIFIER [:LANGUAGE-TAG [:PRIMARY-TAG "en"]]]
          [:SPEC-SEP ", "]
          [:SPECIFIERS
           [:SPECIFIER [:LANGUAGE-TAG [:PRIMARY-TAG "fr"]]]]]]
        (parse-accept-language-header "en, fr"))
      "Space after comma should be tolerated.")))


(deftest test-ordering
  (testing "Languages specified are ordered correctly"
    (is
      (=
        '({:language "en", :qualifier "GB", :preference 1}
                    {:language "en", :qualifier "AU", :preference 0.8}
                    {:language "en", :qualifier "US", :preference 0.6})
        (acceptable-languages "en-AU;q=0.8, en-GB, en-US;q=0.6")))))


(deftest test-pipe
  (testing "Top level functionality"
    (is
      (=
        "This is not a pipe"
        (:pipe (get-messages "en-GB, fr-FR;q=0.9" "resources/i18n" "en-GB"))))
    (is
      (=
        "Ceci n'est pas une pipe."
        (:pipe (get-messages "en-GB;q=0.9, fr-FR" "resources/i18n" "en-GB"))))))
