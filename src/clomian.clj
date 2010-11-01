(ns clomian
  (:require clojure.java.io)
  (:use [incanter core charts]))

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
  (-> file
    java.io.FileInputStream.
    org.jnbt.NBTInputStream.
    .readTag
    .getValue
    (get "Level")
    .getValue
    (get "Blocks")
    .getValue))

(defn freqs [blocks]
  (->> blocks
    (partition 128)
    (reduce (fn [counts col]
              (pmap #(update-in %1 [%2] (fnil inc 0)) counts col))
            (take 128 (repeat {})))))

(defn plotfn [freqs btype layer]
  (get (nth freqs layer) (byte btype) 0))

(let [fr (file-seq (clojure.java.io/file "/Users/pepijndevos/Library/Application Support/minecraft/saves/World2/DIM-1/"))
      fr (mapcat blocks (filter #(and (.isFile %) (not (.isHidden %))) fr))
      fr (freqs fr)
      canvas (-> (reduce #(add-function %1 (partial plotfn fr (key %2)) 0 128
                                        :series-label (val %2))
                         (xy-plot [] []
                                  :x-label "Layer"
                                  :y-label "Blocks"
                                  :legend true)
                          types))]
  (slider #(set-y-range canvas 0 %) (range 0 500))
  (view canvas))
  ;(save canvas "graph.png")
  ;(save (set-y-range canvas 0 50) "graph-low.png"))
