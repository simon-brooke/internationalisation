(ns ^{:doc "Tests for Internationalisation."
      :author "Simon Brooke"} scot.weft.i18n.test.core
  (:require [clojure.test :refer [deftest is testing]]
            [scot.weft.i18n.core :refer [*default-language*
                                         acceptable-languages
                                         generate-accept-languages
                                         get-message
                                         get-messages
                                         parse-accept-language-header]]))

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
     "Space after comma should be tolerated.")
    (is (vector? (parse-accept-language-header "en, fr"))
        "If the header is valid, we should get a (parse tree) vector")
    (is (not (vector? (parse-accept-language-header "")))
        "If the header is invalid, we should get a failure object not a vector")))


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
      (:pipe (get-messages "en-GB, fr-FR;q=0.9" "i18n" "en-GB"))))
    (is
     (=
      "Ceci n'est pas une pipe."
      (:pipe (get-messages "en-GB;q=0.9, fr-FR" "i18n" "en-GB"))))
    (is
     (= nil (get-messages "xx-XX;q=0.5, yy-YY" "i18n" "zz-ZZ"))
     "If no usable file is found, an exception should not be thrown.")
    (binding [*default-language* "en-GB"]
      (is (= "This is not a pipe" (get-message :pipe)))
      (is
       (=
        "Ceci n'est pas une pipe." (get-message :pipe "en-GB;q=0.9, fr-FR")))
      (is (= "это не труба." (get-message :pipe "de-DE" "i18n" "ru")))
      (is (= "froboz" (get-message :froboz))))))
