for f in cover*.png; do
	echo $f;
	convert $f -thumbnail 36x36 ../opacclient/opacapp/src/main/res/drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../opacclient/opacapp/src/main/res/drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../opacclient/opacapp/src/main/res/drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../opacclient/opacapp/src/main/res/drawable-xhdpi/$f;
	convert $f -thumbnail 144x144 ../opacclient/opacapp/src/main/res/drawable-xxhdpi/$f;
done;
for f in type*.svg; do
	echo $f;
	inkscape -zf $f -w 36 -e ../opacclient/opacapp/src/main/res/drawable-ldpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -w 48 -e ../opacclient/opacapp/src/main/res/drawable-mdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -w 72 -e ../opacclient/opacapp/src/main/res/drawable-hdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -w 96 -e ../opacclient/opacapp/src/main/res/drawable-xhdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -w 144 -e ../opacclient/opacapp/src/main/res/drawable-xxhdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -w 192 -e ../opacclient/opacapp/src/main/res/drawable-xxxhdpi/$(echo $f | sed s/svg/png/);
done;
for f in ic_launcher.png; do
	echo $f;
	convert $f -thumbnail 36x36 ../opacclient/opacapp/src/main/res/drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../opacclient/opacapp/src/main/res/drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../opacclient/opacapp/src/main/res/drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../opacclient/opacapp/src/main/res/drawable-xhdpi/$f;
	convert $f -thumbnail 144x144 ../opacclient/opacapp/src/main/res/drawable-xxhdpi/$f;
done;
