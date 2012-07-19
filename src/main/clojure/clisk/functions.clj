(ns clisk.functions
  (:import clisk.Util)
  (:use clisk.node)
  (:use clisk.util))

(set! *unchecked-math* true)

(def ^:const TAO (* 2.0 Math/PI))

(def ^:const COMPONENT_TO_DOUBLE (/ 1.0 255.0))

(defn ensure-scalar [x]
  "Ensure x is a scalar value. If x is a vector, resturns the first component (index 0)."
  (let [x (node x)]
	  (cond 
	    (vector-node? x)
	      (component 0 x)
	    (scalar-node? x)
	      x
	    :else x)))

(defn vectorize 
	"Converts a value into a vector function form. If a is already a vector node, does nothing. If a is a function, apply it to the current position."
  ([a]
	  (let [a (node a)] 
	    (cond
		    (vector-node? a)
		      a
		    (scalar-node? a)
		      (vector-node a)
		    :else
		      (error "Should not be possible!")))))

(defn components [mask a]
  "Gets a subset of components from a, where the mask vector is > 0. Other components are zeroed"
  (let [a (vectorize a)]
    (apply vector-node 
         (map 
           (fn [m v]
             (if (> m 0.0)
               v
               0.0)) 
           mask
           (:nodes a)))))

(defn ^:static red-from-argb 
  "Gets the red component value from an ARGB integer"
  (^double [^long argb]
    (* COMPONENT_TO_DOUBLE (bit-and (int 0xFF) (bit-shift-right argb 16)))))

(defn ^:static green-from-argb 
  "Gets the green component value from an ARGB integer"
  (^double [^long argb]
    (* COMPONENT_TO_DOUBLE (bit-and (int 0xFF) (bit-shift-right argb 8)))))

(defn ^:static blue-from-argb 
  "Gets the blue component value from an ARGB integer"
  (^double [^long argb]
    (* COMPONENT_TO_DOUBLE (bit-and (int 0xFF) argb))))

(defn ^:static alpha-from-argb 
  "Gets the alpha component value from an ARGB integer"
  (^double [^long argb]
    (* COMPONENT_TO_DOUBLE (bit-and (int 0xFF) (bit-shift-right argb 24)))))

(defn x 
  "Extracts the x component of a vector"
  ([v]
	  (component 0 v)))

(defn y 
  "Extracts the y component of a vector"
  ([v]
	  (component 1 v)))

(defn z 
  "Extracts the z component of a vector"
  ([v]
    (component 2 v)))

(defn t 
  "Extracts the t component of a vector"
  ([v]
    (component 3 v)))

(defn rgb
  "Creates an RGB colour vector"
  ([^java.awt.Color java-colour]
    (rgb (/ (.getRed java-colour) 255.0)
         (/ (.getBlue java-colour) 255.0)
         (/ (.getGreen java-colour) 255.0)))
  ([r g b]
    [r g b 1.0])
  ([r g b a]
    [r g b a]))

(defn rgba
  "Creates an RGBA colour vector"
  ([^java.awt.Color java-colour]
    (rgba (/(.getRed java-colour) 255.0)
          (/(.getBlue java-colour) 255.0)
          (/(.getGreen java-colour) 255.0)
          (/(.getAlpha java-colour) 255.0)))
  ([r g b a]
    [r g b a]))


(defn check-dims [& vectors]
  "Ensure that parameters are equal sized vectors. Returns the size of the vector(s) if successful."
  (let [vectors (map node vectors)]
    (let [[v & vs] vectors
          dims (dimensions v)]
      (if (every? #(= dims (dimensions %)) vs)
        dims
        (error "Unequal vector sizes: " (map count vectors))))))

;; todo handle  zeros and ones efficiently
(defn vectorize-op 
  "Make an arbitrary function work on clisk vectors in a component-wise manner"
  ([f]
	  (fn [& vs]
	    (let [vs (map node vs)
	          dims (apply max (map dimensions vs))]
        (if 
          (some vector-node? vs)
		      (apply vector-node 
	          (for [i (range dims)]
		          (apply function-node f (map #(component i %) vs))))
          (apply function-node f vs))))))

(defn vlet 
  "let one or more scalar values within a vector function" 
  ([bindings form]
    (let [form (node form)
          binding-nodes (map (comp node second) (partition 2 bindings))
          symbols (map first (partition 2 bindings))]
      (if-not (every? scalar-node? binding-nodes) (error "All binding values must be scalar"))
		  (if (seq bindings)
		    (apply transform-components
          (fn [form & binds]
            `(let [~@(interleave symbols (map :code binds))]
               ~(:code form)))
          (cons form binding-nodes))
      form))))

(defn vif [c a b]
  "Conditional vector function. First scalar argument is used as conditional value, > 0.0  is true. aLways returns a vector node."
  (let [a (vectorize a)
        b (vectorize b)
        c (component 0 c)
        adims (dimensions a)
        bdims (dimensions b)
        dims (max adims bdims)]
    (vec-node
      (for [i (range dims)]
           (transform-node
             (fn [c a b]
               (if (:constant c)
                 ;; constant case - use appropriate branch directly
                 (if (> 0.0 (evaluate c)) a b) 
                 ;; variable case
                 `(if (> 0.0 ~(:code c)) ~(:code a) ~(:code b)) ))
             (component 0 c)
             (component i a)
             (component i b))))))

(defn apply-to-components
  "Applies a function f to all components of a vector"
  ([f v]
    (let [v (vectorize v)]
      (apply function-node f (:nodes v)))))

(defn ^:static frac
  "Retuns the fractional part of a number. Equivalent to Math/floor."
  (^double [^double x]
    (- x (Math/floor x))))

(defn ^:static phash 
  "Returns a hashed double value in the range [0..1)"
  (^double [^double x]
    (Util/dhash x))
  (^double [^double x ^double y]
    (Util/dhash x y))
  (^double [^double x ^double y ^double z]
    (Util/dhash x y z))
  (^double [^double x ^double y ^double z ^double t]
    (Util/dhash x y z t)))

(def vsin
  (vectorize-op 'Math/sin))

(def vabs
  (vectorize-op 'Math/abs))

(def vround
  (vectorize-op 'Math/round))

(def vfloor
  (vectorize-op 'Math/floor))

(def vfrac
  (vectorize-op 'clisk.functions/frac))

(def v+ 
  "Adds two or more vectors"
  (vectorize-op 'clojure.core/+))

(def v* 
  "Multiplies two or more vectors"
  (vectorize-op 'clojure.core/*))

(def v- 
  "Subtracts two or more vectors"
  (vectorize-op 'clojure.core/-))

(def vdivide 
  "Divides two or more vectors"
  (vectorize-op 'clojure.core//))

(defn dot 
	"Returns the dot product of two vectors"
  ([a b]
	  (let [a (vectorize a)
	        b (vectorize b)
	        adims (check-dims a)
	        bdims (check-dims b)
	        dims (min adims bdims)]
     (transform-node
		   #(cons 'clojure.core/+
		      (for [i (range dims)]
		        `(clojure.core/* ~(:code (component i %1)) ~(:code (component i %2)))))
       a b))))

(defn vcross3
  "Returns the cross product of 2 3D vectors"
  ([a b]
    (transform-node
	    (fn [a b]
	       (let [[x1 y1 z1] (:codes (vectorize a))
		          [x2 y2 z2] (:codes (vectorize b))]
		        [`(- (* ~y1 ~z2) (* ~z1 ~y2))
		         `(- (* ~z1 ~x2) (* ~x1 ~z1))
		         `(- (* ~x1 ~y2) (* ~y1 ~x1))]))
     (node a)
     (node b))))

(defn max-component 
  "Returns the max component of a vector"
  ([v]
    (let [v (vectorize v)]
      (transform-node 
        (fn [v]
          `(max ~@(:codes v)))
        v))))

(defn min-component 
  "Returns the min component of a vector"
  ([v]
    (let [v (vectorize v)]
      (transform-node 
        (fn [v]
          `(min ~@(:codes v)))
        v))))

(defn length 
  "Calculates the length of a vector"
  ([a]
	  (let [comps (:nodes (vectorize a))
	        syms (for [c comps] (gensym "length-comp"))] 
	    (vlet (vec (interleave syms comps))
           `(Math/sqrt (+ ~@(map #(do `(* ~% ~%)) syms)))))))

(defn vnormalize 
  "Normalizes a vector"
  ([a]
	  (let [a (vectorize a)
	        syms (vec (map (fn [_] (gensym "temp")) a))]
	    (vlet (vec (interleave syms a))
	          (vdivide syms `(Math/sqrt ~(dot syms syms)))))))

(defn vwarp 
  "Warps the position vector before calculating a vector function"
  ([warp f]
	  (let [warp (vectorize warp)
	        f (vectorize f)
	        wdims (dimensions warp)
	        fdims (dimensions f)
	        vars (take wdims ['x 'y 'z 't])
	        temps (take wdims ['x-temp 'y-temp 'z-temp 't-temp])
	        bindings 
	          (vec (concat
                  (interleave temps (take wdims (map :code (:nodes warp)))) ;; needed so that symbols x,y,z,t aren't overwritten too early
                  (interleave vars temps)))]
     (vlet bindings f))))

(defn vscale 
  "Scales a function by a given factor."
  ([factor f] 
	  (let [factor (node factor)
	        f (node f)]
	    (vwarp (vdivide pos factor) f))))

(defn voffset 
  "Offsets a function by a specified amount"
  ([offset f]
    (vwarp (v+ 
             pos
             offset)
           f)))

(def offsets-for-vectors [[-120.34 +340.21 -13.67 +56.78]
                          [+12.301 +70.261 -167.678 +34.568]
                          [+78.676 -178.678 -79.612 -80.111]
                          [-78.678 7.6789 200.567 124.099]])

(defn vector-offsets [func]
  "Creates a vector version of a scalar function, where the components are offset versions of the original scalar function"
  (vec 
    (map
      (fn [offs]
        `(let [~@(interleave pos (map #(do `(clojure.core/+ ~%1 ~%2)) offs pos))] 
           ~func))
      offsets-for-vectors)))

(defn vgradient 
  "Computes the gradient of a scalar function f with respect to [x y z t]"
	([f]
	  (let [epsilon 0.000001
	        f (component 0 f)]
	    (vec 
	      (map 
	        (fn [sym] 
	          `(clojure.core// 
	             (clojure.core/-
	               (let [~sym (clojure.core/+ ~epsilon ~sym)]
	                 ~f)
	               ~f)
	             ~epsilon))
	        pos)))))

(defn lerp 
  "Performs clamped linear interpolation between two values, according to the proportion given in the 3rd parameter."
  ([a b v]
	  (let [a# a
	        b# b
	        v# v]
	     (if (<= v# 0) a#
	       (if (>= v# 1) b#
	         (+ 
	           (* v# b#)
	           (* (- 1.0 v#) a#)))))))

(def vlerp 
  "Performs clamped linear interpolation between two vectors, according to the proportion given in the 3rd parameter."
  (vectorize-op lerp))

(defn colour-map 
  ([mapping]
    (fn [x]
      (colour-map mapping x)))
  ([mapping x]
		  (let [vals (vec mapping)
		        v (component 0 x)
		        c (count vals)]
		    (cond 
		      (<= c 0) (error "No colour map available!")
		      (== c 1) (vectorize (second (vals 0)))
		      (== c 2) 
		        (let [lo (first (vals 0))
		              hi (first (vals 1))]
                (if (<= hi lo)
                  (vectorize (second (vals 0)))
			            (vlerp  
			              (vectorize (second (vals 0))) 
			              (vectorize (second (vals 1)))
	                  `(/ (- ~v ~lo) ~(- hi lo)))))
		      :else
		        (let [mid (quot c 2)
		              mv (first (vals mid))
		              vsym (gensym "val")
		              upper (colour-map (subvec vals mid c))
		              lower (colour-map (subvec vals 0 (inc mid)))]
		          (vec (for [i (range 4)]
		          `(let [~vsym ~v] 
		             (if (<= ~vsym ~mv)
		               ~(component i (lower vsym))
		               ~(component i (upper vsym)))))))))))

(def scalar-hash-function
  "Hash function producing a scalar value in the range [0..1) for every 
   unique point in space"
  `(phash ~'x ~'y ~'z ~'t))

(def vhash
  "Hash function producing a vector value 
   in the range [0..1)^4 for every 
   unique point in space"
  (vector-offsets scalar-hash-function))

(def vmin
  "Computes the maximum of two vectors"
  (vectorize-op 'Math/min))

(def vmax
  "Computes the maximum of two vectors"
  (vectorize-op 'Math/max))

(defn vclamp [v low high]
  "Clamps a vector between a low and high vector. Typically used to limit 
   a vector to a range e.g. (vclamp something [0 0 0] [1 1 1])."
  (let [v (vectorize v)
        low (vectorize low)
        high (vectorize high)]
    (vmax low (vmin high v))))


(defn viewport 
  "Rescales the texture as if viwed from [ax, ay] to [bx ,by]"
  ([a b function]
    (let [[x1 y1] a
          [x2 y2] b
          w (- x2 x1)
          h (- y2 y1)]
      (vscale [(/ 1.0 w) (/ 1.0 h) 1 1] (voffset [x1 y1] function)))))



(defn vseamless 
  "Creates a seamless 2D tileable version of a 4D texture in the [0 0] to [1 1] region. The scale argument detrmines the amount of the source texture to be used per repeat."
  ([scale v4]
    (let [v4 (vectorize v4)
          scale (/ 1.0 (component 0 scale) TAO)
          dims (check-dims v4)]
      (if (< dims 4) (error "vseamless requires 4D input texture, found " dims))
      (vwarp
        [`(* (Math/cos (* ~'x TAO)) ~scale) 
         `(* (Math/sin (* ~'x TAO)) ~scale) 
         `(* (Math/cos (* ~'y TAO)) ~scale)
         `(* (Math/sin (* ~'y TAO)) ~scale)]
        v4))))

(defn height 
  "Calculates the height value (z) of a source function"
  ([f] 
    (z f)))

(defn height-normal 
  "Calculates a vector normal to the surface defined by the z-value of a source vector or a scalar height value. The result is *not* normalised."
  ([heightmap]
    (v- [0 0 1] (components [1 1 0] (vgradient (z heightmap)))))
  ([scale heightmap]
    (v- [0 0 1] (components [1 1 0] (vgradient `(* ~scale ~(z heightmap)))))))


(defn light-value 
  "Calculates diffuse light intensity given a light direction and a surface normal vector. 
   This function performs its own normalisation, so neither the light vector nor the normal vector need to be normalised."
  ([light-direction normal-direction]
	  `(max 0.0 
	        ~(dot (vnormalize light-direction) (vnormalize normal-direction)))))

(defn diffuse-light 
  "Calculate the diffuse light on a surface normal vector.
   This function performs its own normalisation, so neither the light vector nor the normal vector need to be normalised."
  ([light-colour light-direction normal-direction]
    (v* light-colour (light-value light-direction normal-direction))))
