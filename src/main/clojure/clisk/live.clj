(ns 
  ^{:author "mikera"
    :doc "Namespace with all includes ready for live coding."}    
  clisk.live
;;  (:refer-clojure :exclude [* + - /])
  (:use [clisk core node functions patterns colours textures util effects]))

;; do we really want to override these???
;;(def * v*)
;;(def + v+)
;;(def - v-)
;;(def / clisk.functions/vdivide)