(ns clomian
  (:require clojure.java.io)
  (:use [incanter core charts])
  (:gen-class))

;(set! *warn-on-reflection* true)

(def types {0 "Air"
            4 "Cobblestone"
            8 "Water"
            12 "Sand"
            16 "Coal ore"
            20 "Glass"
            24 "Lime cloth"
            28 "Blue cloth"
            32 "Magenta cloth"
            36 "White cloth"
            40 "Red mushroom"
            44 "Step"
            48 "Mossy cobblestone"
            52 "Mob spawner"
            56 "Diamond ore"
            60 "Soil"
            64 "Wooden door"
            68 "Wall sign"
            72 "Wooden pressure plate"
            76 "Redstone torch (on)"
            80 "Snow block"
            84 "Jukebox"
            88 "Slow sand"
            3 "Dirt"
            7 "Bedrock"
            11 "Stationary lava"
            15 "Iron ore"
            19 "Sponge"
            23 "Yellow cloth"
            27 "Cyan cloth"
            31 "Violet cloth"
            35 "Gray / white cloth"
            39 "Brown mushroom"
            43 "Double step"
            47 "Bookshelf"
            51 "Fire"
            55 "Redstone wire"
            59 "Crops"
            63 "Sign post"
            67 "Cobblestone stairs"
            71 "Iron door"
            75 "Redstone torch (off)"
            79 "Ice"
            83 "Reed"
            87 "Bloodstone"
            91 "Jack-o-lantern"
            2 "Grass"
            6 "Sapling"
            10 "Lava"
            14 "Gold ore"
            18 "Leaves"
            22 "Orange cloth"
            26 "Aqua green cloth"
            30 "Indigo cloth"
            34 "Black cloth"
            38 "Red rose"
            42 "Iron block"
            46 "TNT"
            50 "Torch"
            54 "Chest"
            58 "Workbench"
            62 "Burning furnace"
            66 "Minecart tracks"
            70 "Stone pressure plate"
            74 "Glowing redstone ore"
            78 "Snow"
            82 "Clay"
            86 "Pumpkin"
            90 "Portal"
            1 "Stone"
            5 "Wood"
            9 "Stationary water"
            13 "Gravel"
            17 "Log"
            21 "Red cloth"
            25 "Green cloth"
            29 "Purple cloth"
            33 "Pink cloth"
            37 "Yellow flower"
            41 "Gold block"
            45 "Brick"
            49 "Obsidian"
            53 "Wooden stairs"
            57 "Diamond block"
            61 "Furnace"
            65 "Ladder"
            69 "Lever"
            73 "Redstone ore"
            77 "Stone button"
            81 "Cactus"
            85 "Fence"
            89 "Lightstone"})

(defn dat-seq [^java.io.File dir]
  (filter #(let [n (.getName ^java.io.File %)]
             (and
               (.endsWith n ".dat")
               (.startsWith n "c.")))
    (tree-seq
     (fn [^java.io.File f] (and
                             (.isDirectory f)
                             (let [n (.getName f)]
                               (or (> 3 (count n))
                                   (= n (.getName dir))))))
     (fn [^java.io.File d] (seq (.listFiles d)))
     dir)))

(defn aconcat [& ars]
  (let [offset (reductions + (map alength ars))
        total-lenght (last offset)
        far (java.util.Arrays/copyOf (first ars) total-lenght)]
    (doseq [[ar off] (map vector (next ars) offset)]
      (System/arraycopy ar 0 far off (alength ar)))
    far))

(defn blocks [^java.io.File file]
  (with-open [nbt (-> file
                    java.io.FileInputStream.
                    java.io.BufferedInputStream.
                    org.jnbt.NBTInputStream.)]
    (-> nbt
      .readTag
      ^java.util.Map (.getValue)
      ^org.jnbt.CompoundTag (.get "Level")
      .getValue
      ^org.jnbt.ByteArrayTag (.get "Blocks")
      .getValue)))

(defn get-layer [layer-num ^bytes blocks]
  (let [size (/ ^Long (alength blocks) 128)
        output (byte-array size)]
    (doseq [output-idx (range size)]
      (let [block-idx (+ (* output-idx 128) layer-num)]
        (aset output output-idx (aget blocks block-idx))))
    output))

(defn afrequencies
  [^bytes a]
  (persistent!
   (areduce a idx counts (transient {})
            (let [x (aget a idx)]
              (assoc! counts x (inc (get counts x 0)))))))

(defn freqs [^bytes blocks]
  (let [layers (map #(get-layer % blocks) (range 128))]
    (pmap afrequencies layers)))

(defn plotfn [freqs btype layer]
  (get (nth freqs layer) (byte btype) 0))

(defn -main [path & options]
  (let [options (set (map #(Integer. ^String %) options))
        fr (time (apply aconcat (map blocks (dat-seq (clojure.java.io/file path)))))
        fr (time (freqs fr))        
        canvas (time (-> (reduce #(add-function %1 (partial plotfn fr (key %2)) 0 128
                                          :series-label (val %2))
                           (xy-plot [] []
                                    :x-label "Layer"
                                    :y-label "Blocks"
                                    :legend true)
                            (select-keys types options))))]
    ;(slider #(set-y-range canvas 0 %) (range 0 500))
    (view canvas)))
    ;(save canvas "graph.png")
    ;(save (set-y-range canvas 0 50) "graph-low.png")))
