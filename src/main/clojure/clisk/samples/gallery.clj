(ns clisk.samples.gallery
    ^{:author "mikera"
    :doc "Selection of nice sample images."}  
    (:use [clisk core node functions patterns colours textures util]))

;; we define sample gallery as a quoted map
;; so that we can pull out code later
;; TODO: build a simple GUI to show samples with corresponding code

(def samples `{
  :coloured-panels
  ;; vnoise provides a nice colourful plasma texture
  ;; the offset function randomly offsets each 1*1 square
  (show (scale 0.2 
               (offset 
                 (v* 10 (warp vfloor grain)) 
                 vnoise)))
 
  :fractal-landscape
  ;; a factal ladscape
  (show 
	  (scale 0.4 
      (let [z (v+ (v* 2.5 plasma) -0.75)
            colour (landscape-map z)
            height (v* 3.0 (vmax 0.5 z))]
	       (render-lit colour height ))))
  
  :plasma-clouds
  ;; a nice plasma cloud background texture
  (show (seamless 0.5 (vplasma vplasma)) 1024 1024)
  
  :stained-glass
  (show (let [voronoi1 (voronoi :points 512)] 
    (v*
      (v* 20.0 (voronoi-blocks :voronoi voronoi1))
      (warp (voronoi-points :voronoi voronoi1) grain)))
    1024 1024)
  
  :oily-colours
  (show (offset (seamless (v* 2 vplasma)) 
              (compose vnoise 
                       [(v* 10 (seamless 0.4 noise)) y z t]))1024 1024)  
  :polished-blue-stone
  (show (offset (seamless (v* 2 vplasma)) 
              (compose (v* [0.3 0.6 1.0] vnoise) 
                       (v* 150 (seamless 0.4 (v* splasma noise))))) 1024 1024)
  
  :landscape-contours
  (show (offset (seamless (v* 2 vplasma)) 
              (compose (components [0 1] vnoise) 
                       (v* 150 (seamless 0.4 noise)))) 1024 1024)
  
  :colourful-spots
  (show (seamless 0.5 (compose vnoise [spots y z t])) 1024 1024)
  
  :mandelbrot
  (show (viewport [-2 -1.5] [1 1.5]
          (fractal 
            :while (v- 2 (length [x y])) 
            :update (v+ c [(v- (v* x x) (v* y y))  (v* 2 x y)]) 
            :result (vplasma (v* 0.1 'i))
            :bailout-result black
            :max-iterations 1000)) 1024 1024)
  
  :voronoi-net-fractal
    (show (let [voronoi1 (voronoi :points 32)] 
	          (fractal 
	            :while (v- 0.97 (voronoi-function 
	                              (v- 1 (v* (vdivide (v- y x) y) (vdivide (v- z x) z)))
	                              :voronoi voronoi1)) 
	            :update (v+ (v* pos 2) pos) 
	            :result (vdivide 3 (v+ 3 'i)) 
	            :max-iterations 4)) 512 512)
    
})


(def sample-list 
  "List of sample keywords"
  (keys samples))