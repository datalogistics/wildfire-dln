#!/bin/sh

# When creating new icons store in xxxhdpi at highest resolution
# Then run the script like this:
#	./shrink.sh "icon_name" 36 drawable-ldpi
#	./shrink.sh "icon_name" 48 drawable-mhdpi
#	./shrink.sh "icon_name" 72 drawable-hdpi
#	./shrink.sh "icon_name" 128 drawable-xhdpi
#	./shrink.sh "icon_name" 180 drawable-xxhdpi

filter=$1
outSize=$2
outDir=$3
for file in $filter*.png; do
	size=$(identify "$file" | awk -F " " '{ print $3 }')
	width=$(printf $size | awk -F "x" '{ print $1 }')
	height=$(printf $size | awk -F "x" '{ print $2 }')
	resize="$outSize"x"$outSize"
	if (( $width > $height )); then
		if [[ "$height" -le "$outSize" ]]; then
			continue
		fi
		resize="$width"x"$outSize"
	else
		if [[ "$width" -le "$outSize" ]]; then
			continue
		fi
		resize="$outSize"x"$height"
	fi
	echo "$file $size -> $resize"
	convert "$file" -resize $resize "../$outDir/$file"
	svn add "../$outDir/$file"
done