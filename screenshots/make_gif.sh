if [ "x$1" == "x" -o "x$2" == "x" ]; then
    echo "Usage: ./make_gif input output [start-time]"
fi

input=$1
output=$2
start_time=$3
if [ "x$start_time" == "x" ]; then
    start_time=0
fi

ffmpeg -y -ss $start_time -i $1 -vf fps=60,scale=640:-1:flags=lanczos,palettegen palette.png
ffmpeg -ss $start_time -i $1 -i palette.png -ss $start_time -filter_complex "fps=25,scale=640:-1:flags=lanczos[x];[x][1:v]paletteuse" $2