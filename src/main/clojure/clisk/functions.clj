(ns clisk.functions
  (:import clisk.Util))

(set! *unchecked-math* true)

;; standard position vector
(def pos ['x 'y 'z 't])

(defn error [& vals]
  (throw (Error. (str (reduce str vals)))))

(defn vectorize [x]
  "Converts a value into a vector function form. If x is already a factor, does nothing. If x is a function, apply it to the current position."
  (cond
    (vector? x)
      x
    (number? x)
      (vec (repeat 4 (double x)))
    (fn? x)
      (x pos)
    :else
      (vec (repeat 4 x))))

(defn component [i a]
  (let [a (vectorize a)]
    (if (< i (count a))
      (a i)
      0.0)))

(defn check-dims [& vectors]
  "Ensure that parameters are equal sized vectors. Returns the size of the vector(s) if successful."
  (if (every? vector? vectors)
    (let [[v & vs] vectors
          dims (count v)]
      (if (every? #(= dims (count %)) vs)
        dims
        (error "Unequal vector sizes: " (map count vectors))))
    (error "Not all vectors: " vectors)))

(defn vectorize-op1 [f]
  (fn [v1]
    (let [v1 (vectorize v1)
         dims (check-dims v1)]
      (vec (for [i (range dims)]
           (list f (v1 i)))))))

(defn vectorize-op2 [f]
  (fn [v1 v2]
    (let [v1 (vectorize v1)
          v2 (vectorize v2)
          dims1 (check-dims v1)
          dims2 (check-dims v2)
          dims (max dims1 dims2)]
      (vec 
        (for [i (range dims)]
          (cond 
            (>= i dims1) (list f 0.0 (v2 i))
            (>= i dims2) (list f (v1 i) 0.0)
            :else (list f (v1 i) (v2 i))))))))



(defn vif [c a b]
  (let [c (vectorize c)
        a (vectorize a)
        b (vectorize b)
        adims (check-dims a)
        bdims (check-dims b)
        cdims (check-dims c)
        dims (max adims bdims)]
    (vec (for [i (range dims)]
           (let [av (if (< i adims) (a i) 0.0)
                 bv (if (< i bdims) (b i) 0.0)
                 cv (if (< i cdims) (c i) 0.0)]
             `(if (> 0.0 ~cv) ~av ~bv))))))

(defn ^:static frac ^double [^double x]
  (- x (Math/floor x)))

(defn ^:static phash 
  (^double [^double x]
    (Util/dhash x))
  (^double [^double x ^double y]
    (Util/dhash x y))
  (^double [^double x ^double y ^double z]
    (Util/dhash x y z))
  (^double [^double x ^double y ^double z ^double t]
    (Util/dhash x y z t)))

(def vsin
  (vectorize-op1 'Math/sin))

(def vabs
  (vectorize-op1 'Math/abs))

(def vround
  (vectorize-op1 'Math/round))

(def vfloor
  (vectorize-op1 'Math/floor))

(def vfrac
  (vectorize-op1 'clisk.functions/frac))

(def v+ 
  (vectorize-op2 'clojure.core/+))

(def v* 
  (vectorize-op2 'clojure.core/*))

(def v- 
  (vectorize-op2 'clojure.core/-))

(def vdivide 
  (vectorize-op2 'clojure.core//))

(defn vdot [a b]
  (let [a (vectorize a)
        b (vectorize b)
        adims (check-dims a)
        bdims (check-dims b)
        dims (min adims bdims)]
    (cons 'clojure.core/+
      (for [i (range dims)]
        `(clojure.core/* ~(a i) ~(b i))))))

(defn vlength [a]
  `(Math/sqrt ~(vdot a a)))

(defn vwarp 
  [warp f]
  (let [warp (vectorize warp)
        f (vectorize f)
        wdims (check-dims warp)
        fdims (check-dims f)
        vars (take wdims ['x 'y 'z 't])
        temps (take wdims ['x-temp 'y-temp 'z-temp 't-temp])
        bindings 
          (concat
            (interleave temps warp)
            (interleave vars temps))]
    (vec
      (for [i (range fdims)]
        `(let [~@bindings] ~(f i))))))

(defn vscale [factor f] 
  (let [factor (vectorize factor)
        f (vectorize f)]
    (vwarp (vdivide pos factor) f)))

(defn voffset 
  [warp f]
    (vwarp (v+ 
             pos
             warp)
           f))

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

(defn vgradient [f]
  "Computes the gradient of a scalar function f with respect to [x y z t]"
  (let [epsilon 0.000001]
    (vec 
      (map 
        (fn [sym] 
          `(clojure.core// 
             (clojure.core/-
               (let [~sym (clojure.core/+ ~epsilon ~sym)]
                 ~f)
               ~f)
             ~epsilon))
        pos))))

(defn lerp [v a b]
  `(let [a# ~a
         b# ~b
         v# ~v]
     (if (<= v# 0) a#
       (if (>= v# 1) b#
         (+ 
           (* v# b#)
           (* (- 1.0 v#) a#))))))

(defn vlerp 
  ([a b]
    (fn [v] (vlerp a b v)))
  ([a b v]
	  (let [a (vectorize a)
	        b (vectorize b)
	        v (component 0 v)
	        dims (max (count a) (count b))
	        vsym (gensym "val")]
	    (vec (for [i (range dims)]
			  `(let [~vsym ~v]
			     (if (<= ~vsym 0) ~(component i a)
			       (if (>= ~vsym 1) ~(component i b)
			         (+ (* ~vsym ~(component i b)) (* (- 1.0 ~vsym) ~(component i a)))))))))))

(defn colour-map 
  ([mapping v]
    ((colour-map mapping) v))
  ([mapping]
	  (fn [x]
		  (let [vals (vec mapping)
		        v (component 0 x)
		        c (count vals)]
		    (cond 
		      (<= c 0) (error "No colour map available!")
		      (== c 1) (vectorize (second (vals 0)))
		      (== c 2) 
		        (let [lo (first (vals 0))
		              hi (first (vals 1))]
		            (vlerp  
		              (vectorize (second (vals 0))) 
		              (vectorize (second (vals 1)))
                  `(/ (- ~v ~lo) (- ~hi ~lo))))
		      :else
		        (let [mid (quot c 2)
		              mv (first (vals mid))
		              vsym (gensym "val")
		              upper (colour-map (subvec vals mid c))
		              lower (colour-map (subvec vals 0 (inc mid)))]
		          (vec (for [i (range 4)]
		          `(let [~vsym ~v] 
		             (if (< ~vsym ~mv)
		               ~(component i (lower vsym))
		               ~(component i (upper vsym))))))))))))

(def scalar-hash-function
  "Hash function producing a scalar value in the range [0..1) for every unique point in space"
  `(phash ~'x ~'y ~'z ~'t))

(def vhash
  "Hash function producing a vector value in the range [0..1)^4 for every unique point in space"
  (vector-offsets scalar-hash-function))

(def vmin
  (vectorize-op2 'Math/min))

(def vmax
  (vectorize-op2 'Math/max))

(defn vclamp [v low high]
  (let [v (vectorize v)
        low (vectorize low)
        high (vectorize high)]
    (vmax low (vmin high v))))


