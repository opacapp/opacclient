cp ic_launcher.png ic_launcher_big.png                                   
convert ic_launcher.png -resize 512x512 ../../web/graphics/launcher.png

for f in type*.png ic_launcher.png; do
	convert $f -thumbnail 36x36 ../drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../drawable-xhdpi/$f;
done;

for f in navigate*.png; do
	convert $f -thumbnail x80 ../drawable-xhdpi/$f;
	convert $f -thumbnail x60 ../drawable-hdpi/$f;
	convert $f -thumbnail x40 ../drawable-mdpi/$f;
	convert $f -thumbnail x30 ../drawable-ldpi/$f;
done;

for f in action*.png; do
	convert $f -thumbnail 260x ../drawable-xhdpi/$f;
	convert $f -thumbnail 195x ../drawable-hdpi/$f;
	convert $f -thumbnail 130x ../drawable-mdpi/$f;
	convert $f -thumbnail 98x ../drawable-ldpi/$f;
done;
