for f in drawer*.png; do
	echo $f;
	convert $f -thumbnail x150 ../opacclient/opacapp/src/main/res/drawable-ldpi/$f;
	convert $f -thumbnail x200 ../opacclient/opacapp/src/main/res/drawable-mdpi/$f;
	convert $f -thumbnail x300 ../opacclient/opacapp/src/main/res/drawable-hdpi/$f;
	convert $f -thumbnail x400 ../opacclient/opacapp/src/main/res/drawable-xhdpi/$f;
	convert $f -thumbnail x600 ../opacclient/opacapp/src/main/res/drawable-xxhdpi/$f;
done;
