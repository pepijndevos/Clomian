(ns clomian
  (:require clojure.java.io)
  (:use [incanter core charts])
  (:gen-class))

(set! *warn-on-reflection* true)

(def types {0  "air"
            7  "Bedrock"
            39 "Brown Mushroom"
            40 "Red Mushroom"
            10 "Lava"
            11 "Stationary Lava"
            13 "Gravel"
            49 "Obsidian"
            51 "Fire"
            87 "Bloodstone"
            88 "Slow Sand"
            89 "Lightstone"
            90 "Portal"})

(defn blocks [file]
  (-> ^java.io.File file
    java.io.FileInputStream.
    org.jnbt.NBTInputStream.
    .readTag
    ^java.util.Map (.getValue)
    ^org.jnbt.CompoundTag (.get "Level")
    .getValue
    ^org.jnbt.ByteArrayTag (.get "Blocks")
    .getValue))

(defn freqs [blocks]
  (->> blocks
    (partition 128)
    (reduce (fn [counts col]
              (doall (map #(assoc! %1 %2 (inc (get %1 %2 0))) counts col)))
            (repeatedly 128 #(transient {})))))

(defn plotfn [freqs btype layer]
  (get (nth freqs layer) (byte btype) 0))

(defn -main [& options]
  (let [fr (file-seq (clojure.java.io/file "/Users/pepijndevos/Library/Application Support/minecraft/saves/World2/DIM-1/"))
        fr (mapcat blocks (filter #(and (.isFile ^java.io.File %) (not (.isHidden ^java.io.File %))) fr))
        fr (freqs fr)
        canvas (-> (reduce #(add-function %1 (partial plotfn fr (key %2)) 0 128
                                          :series-label (val %2))
                           (xy-plot [] []
                                    :x-label "Layer"
                                    :y-label "Blocks"
                                    :legend true)
                            types))]
    (slider #(set-y-range canvas 0 %) (range 0 500))
    (view canvas)))
    ;(save canvas "graph.png")
    ;(save (set-y-range canvas 0 50) "graph-low.png")))
