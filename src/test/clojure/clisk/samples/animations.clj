(ns clisk.samples.animations
  "Examples of using Clisk with the Telegenic library to create procedurally generated animations"
  (:use [clisk live animation])
  (:import [clisk Util])
  (:require [telegenic.core :as telegenic]))


;; =======================================================================================================
;; SAMPLE ANIMATIONS
;;
;; Designed to be run at the REPL
(comment

  ;; Simple example - scrolling in x axis
  (render-animation [i 50]
    (offset [(* i 0.04) 0]     ;; use an offset in x-axis to scroll the image over time  
            (vnoise vsnoise))  ;; use any Clisk function to create the image
    {:filename "out.mp4" ;; filename to output 
     :width 256          ;; width of generated video
     :height 256})       ;; height of generated video
  
  
  ;; Clojure procedural animation #1 - https://www.youtube.com/watch?v=XKV209VfkTk
  ;; black and white swirly patterns
  (render-animation [i 300]
    (offset [0 0 (* i 0.003)] ;; use an offset in z-axis to change the image over time   
            (scale 0.5 (clisk.live/vabs (clisk.live/length (clisk.live/vsin (clisk.live/v* (clisk.live/lightness-from-rgb (clisk.live/dot [-0.7247 -0.5677] (clisk.live/v+ [-2.4375 -1.7593 -1.0106] [0.7885 -1.7012 1.7523 0.2914]))) (clisk.live/vdivide (clisk.live/saturation-from-rgb (clisk.live/lerp (clisk.live/adjust-hsl (clisk.live/adjust-hsl clisk.live/vsnoise [0.795 2.244 -0.63]) [-1.9613 -1.3866 -0.0114 1.3613]) (clisk.live/y (clisk.live/v* clisk.live/pos 1.2235)) [-1.4063 3.9377999999999997 -1.4972])) (clisk.live/lightness-from-rgb (clisk.live/y (clisk.live/alpha [-2.4185 -1.3271])))))))))  )
    {})  ;; just use default options
  
  
  ;; colourful swirly patterns - variant of #1
  ;; several hours to render!
  ;; 151104_023134_N.clj
  (render-animation [i 600]
    (offset [0 0 (* i 0.003)] ;; use an offset in z-axis to change the image over time     
      (clisk.live/vabs 
        (clisk.live/lerp 
          (clisk.live/adjust-hsl (clisk.live/adjust-hsl clisk.live/vsnoise [0.795 2.244 -0.63]) [-1.9613 -1.3866 -0.0114 1.3613]) 
          (clisk.live/y (clisk.live/v* clisk.live/pos 1.2235)) [-1.4063 3.9377999999999997 -1.4972])))
    {})

   
  ;; Clojure procedural animation #2 - https://www.youtube.com/watch?v=94CtmzAUIBI
  ;; cloudy skyscape
  ;; several hours to render!
  ;; 150829_223119_C.clj
  (render-animation [i 400]
    (offset [0 0 (* i 0.003)] ;; use an offset in z-axis to change the image over time  
      (clisk.live/vpow 
        (clisk.live/length (clisk.live/green-from-hsl (clisk.live/rgb-from-hsl (clisk.live/v- clisk.live/pos (clisk.live/blue-from-hsl (clisk.live/rgb-from-hsl (clisk.live/v- (clisk.live/v+ [0.3541 -1.1358] clisk.live/pos) (clisk.live/vsin (clisk.live/vsin clisk.live/vsnoise))))))))) 
        (clisk.live/vpow (clisk.live/length clisk.live/splasma) (clisk.live/sigmoid (clisk.live/gradient (clisk.live/green-from-hsl (clisk.live/rgb-from-hsl (clisk.live/v- clisk.live/vsnoise (clisk.live/vsin (clisk.live/y clisk.live/vsnoise))))))))))
    {})
  
  
  ;; Clojure procedural animation #3 - https://www.youtube.com/watch?v=rPEZzUWlDEQ
  ;; colourful glow effects
  (render-animation [i 400]
    (offset [(* i 0.005) (* i 0.005) (* i 0.01)] ;; include [x y] drift to add some scrolling  
      (clisk.live/vabs (clisk.live/gradient (clisk.live/green-from-hsl (clisk.live/hue-from-rgb clisk.live/vsnoise)))))
    {})
  
  
  ;; Clojure procedural animation #4
  ;; Rainbows with checkered discontinuities
  (render-animation [i 400]
    (offset [(* i 0.005) (* i 0.005) (* i 0.01)]   
                                   (clisk.live/vfrac (clisk.live/vcos (clisk.live/gradient (clisk.live/green-from-hsl (clisk.live/hue-from-rgb clisk.live/vsnoise) )))))
    {})
  
  
  ;; Clojure procedural animation #5
  ;; Dedicated to Cindy
  (render-animation [i 400]
    (offset [0 0 (* i 0.003)]   
      (scale 0.3 (vplasma vsnoise)))
    {})
  
  
  ;; Clojure procedural animation #6
  ;; TweeGeeMee evolved: 151112_193202_D.clj
  ;; https://gist.github.com/rogerallen/90bb215917b20fc3b38b#file-1_archive-edn-L3689-L3691
  (render-animation [i 20]
    (offset [0 0 (* i 0.003)]   
      (clisk.live/vsqrt (clisk.live/vdivide (clisk.live/hsl-from-rgb (clisk.live/gradient (clisk.live/vsin (clisk.live/v* (clisk.live/lightness-from-rgb (clisk.live/dot [-0.7247 -0.5677] (clisk.live/v+ [-2.4375 -1.7593 -1.0106] [0.7885 -1.7012 1.7523 0.2914]))) (clisk.live/vdivide (clisk.live/saturation-from-rgb (clisk.live/lerp (clisk.live/adjust-hsl (clisk.live/adjust-hsl clisk.live/vsnoise [0.795 2.244 -0.63]) [-1.9613 -1.3866 -0.0114 1.3613]) (clisk.live/y (clisk.live/v* (clisk.live/square (clisk.live/z (clisk.live/x clisk.live/spots))) 1.2235)) [-2.6455 1.3693 -1.6896])) (clisk.live/lightness-from-rgb (clisk.live/vround (clisk.live/square (clisk.live/z (clisk.live/x 2.6875)))))))))) (clisk.live/rgb-from-hsl (clisk.live/blue-from-hsl 0.9575)))))
    {:width 256 :height 256})
  
  
  ;; Clojure procedural animation #7
  ;; TweeGeeMee evolved: 151214_143140_N.clj
  (render-animation [i 20]
    (offset [0 0 (* i 0.003)]   
      (clisk.live/vabs (clisk.live/vcos (clisk.live/v* (clisk.live/vabs (clisk.live/scale (clisk.live/vround [2.1838 -1.5047 -1.3361 0.6155]) (clisk.live/x (clisk.live/hue-from-rgb (clisk.live/adjust-hsl clisk.live/noise (clisk.live/vfrac clisk.live/vsnoise)))))) [2.0727 1.3225 -0.9395]))))
    {:width 256 :height 256})  

  ;; Clojure procedural animation #8
  ;; TweeGeeMee evolved: 151215_223145_C.clj
  (render-animation [i 20]
    (offset [0 0 (* i 0.003)]   
      (clisk.live/vabs (clisk.live/hue-from-rgb (clisk.live/adjust-hsl clisk.live/noise (clisk.live/gradient (clisk.live/adjust-hsl (clisk.live/vround [2.1838 -1.5047 -1.3361 0.6155]) clisk.live/vsnoise))))))
    {:width 256 :height 256})  

  
  
  
  )