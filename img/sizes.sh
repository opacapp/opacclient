cp ic_launcher.png ic_launcher_big.png                                   
convert ic_launcher.png -resize 512x512 ../web/graphics/launcher.png

for f in type*.png ic_launcher.png; do
	convert $f -thumbnail 36x36 ../res/drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../res/drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../res/drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../res/drawable-xhdpi/$f;
done;

for f in navigate*.png; do
	convert $f -thumbnail x80 ../res/drawable-xhdpi/$f;
	convert $f -thumbnail x60 ../res/drawable-hdpi/$f;
	convert $f -thumbnail x40 ../res/drawable-mdpi/$f;
	convert $f -thumbnail x30 ../res/drawable-ldpi/$f;
done;

for f in action*.png; do
	convert $f -thumbnail 260x ../res/drawable-xhdpi/$f;
	convert $f -thumbnail 195x ../res/drawable-hdpi/$f;
	convert $f -thumbnail 130x ../res/drawable-mdpi/$f;
	convert $f -thumbnail 98x ../res/drawable-ldpi/$f;
done;
