set title network
set xlabel "Time (sec)"
set ylabel "#mydist messages"
set term png
set output network.".png"
plot "measurements.log" using ($1*0.1):9 title '#mydist' with lines
