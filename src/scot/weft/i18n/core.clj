(ns ^{:doc "Internationalisation."
      :author "Simon Brooke"}
  scot.weft.i18n.core
  (:require [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [join]]
            [instaparse.core :as insta]
            [taoensso.timbre :as timbre]))

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


(def accept-language-grammar
  "Grammar for `Accept-Language` headers"
  "HEADER := SPECIFIER | SPECIFIERS;
  SPECIFIERS:= SPECIFIER | SPECIFIER SPEC-SEP SPECIFIERS;
  SPEC-SEP := #',\\s*';
  SPECIFIER := LANGUAGE-TAG | LANGUAGE-TAG Q-SEP Q-VALUE;
  LANGUAGE-TAG := PRIMARY-TAG | PRIMARY-TAG '-' SUB-TAGS;
  PRIMARY-TAG := #'[a-zA-Z]+';
  SUB-TAGS := SUB-TAG | SUB-TAG '-' SUB-TAGS;
  SUB-TAG := #'[a-zA-Z0-9]+';
  Q-SEP := #';\\s*q='
  Q-VALUE := '1' | #'0.[0-9]+';")


(def parse-accept-language-header
  "Parse an `Accept-Language` header"
  (insta/parser accept-language-grammar))


(defn generate-accept-languages
  "From a `parse-tree` generated by the `language-specifier-grammar`, generate
  a list of maps each having a `:language` key, a `:preference` key and a
  `:qualifier` key."
  {:doc/format :markdown}
  [parse-tree]
  (if
    (nil? parse-tree)
    nil
    (case
      (first parse-tree)
      :HEADER (generate-accept-languages (second parse-tree))
      :SPECIFIERS (cons
                    (generate-accept-languages (second parse-tree))
                    (if (>= (count parse-tree) 3)
                      (generate-accept-languages (nth parse-tree 3))))
      :SPEC-SEP nil
      :SPECIFIER (assoc
                   (generate-accept-languages (second parse-tree))
                   :preference
                   (if
                     (>= (count parse-tree) 3)
                     (generate-accept-languages (nth parse-tree 3))
                     1))
      :LANGUAGE-TAG (if
                      (>= (count parse-tree) 3)
                      (assoc
                        (generate-accept-languages (second parse-tree))
                        :qualifier
                        (generate-accept-languages (nth parse-tree 3)))
                      (generate-accept-languages (second parse-tree)))
      :PRIMARY-TAG {:language (second parse-tree) :qualifier "*"}
      :SUB-TAGS (if
                  (>= (count parse-tree) 3)
                  (str
                    (generate-accept-languages (second parse-tree))
                    "-"
                    (generate-accept-languages (nth parse-tree 3)))
                  (generate-accept-languages (second parse-tree)))
      :SUB-TAG (second parse-tree)
      :Q-SEP nil
      :Q-VALUE (read-string (second parse-tree))
      ;; default
      (let [formatted-tree (with-out-str (pprint parse-tree))]
        (throw (Exception. (str "Unexpected parse tree: " formatted-tree)))))))


(defn acceptable-languages
  "Generate an ordered list of acceptable languages, most-preferred first.

  * `accept-language-header` should be the value of an RFC2616 `Accept-Language` header.

  Returns a list of maps as generated by `generate-accept-languages`, in descending order
  of preference."
  {:doc/format :markdown}
  [accept-language-header]
  (reverse
    (sort-by
      :preference
      (generate-accept-languages
        (parse-accept-language-header accept-language-header)))))


(defn slurp-resource
  "Slurp the resource of this name and return its contents as a string; but if it doesn't
   exist log the fact and return nil, rather than throwing an exception."
  [name]
  (try
    (slurp (io/resource name))
    (catch Exception any
      (timbre/error (str "Resource at " name " does not exist."))
      nil)))


(defn find-language-file-name
  "Find the name of a messages file on this resource path which matches this `language-spec`.

  * `language-spec` should be either a map as generated by `generate-accept-languages`, or
  else a string;
  * `resource-path` should be the path name of the directory in which message files are stored,
  within the resources on the classpath.

  Returns the name of an appropriate file if any is found, else nil."
  {:doc/format :markdown}
  [language-spec resource-path]
  (let [file-path (if
                    (string? language-spec)
                    (join
                      java.io.File/separator
                      [resource-path (str language-spec ".edn")]))
        contents (if file-path (slurp-resource file-path))]
    (cond
      contents
      file-path
      (map? language-spec)
      (or
        (find-language-file-name
          (str (:language language-spec) "-" (:qualifier language-spec))
          resource-path)
        (find-language-file-name
          (:language language-spec)
          resource-path)))))


(defn raw-get-messages
  "Return the most acceptable messages collection we have given this `accept-language-header`.
  Do not use this function directly, use the memoized variant `get-messages`, as performance
  will be very much better.

  * `accept-language-header` should be the value of an RFC2616 `Accept-Language` header;
  * `resource-path` should be the fully-qualified path name of the directory in which
  message files are stored;
  * `default-locale` should be a locale specifier to use if no acceptable locale can be
  identified.

  Returns a map of message keys to strings; if no useable file is found, returns nil."
  {:doc/format :markdown}
  [^String accept-language-header ^String resource-path ^String default-locale]
  (let [file-path (first
                    (remove
                      nil?
                      (map
                        #(find-language-file-name % resource-path)
                        (acceptable-languages accept-language-header))))]
    (timbre/debug (str "Found i18n file at '" file-path "'"))
    (try
      (read-string
        (slurp-resource
          (or
            file-path
            (join java.io.File/separator
                  [resource-path
                   (str default-locale ".edn")]))))
      (catch Exception any
        (timbre/error (str "Failed to load internationalisation because " (.getMessage any)))
        nil))))


(def get-messages
  "Return the most acceptable messages collection we have given this `accept-language-header`

  `accept-language-header` should be the value of an RFC2616 `Accept-Language` header;
  `resource-path` should be the fully-qualified path name of the directory in which
  message files are stored;
  `default-locale` should be a locale specifier to use if no acceptable locale can be
  identified.

  Returns a map of message keys to strings.; if no useable file is found, returns nil."
  (memoize raw-get-messages))
