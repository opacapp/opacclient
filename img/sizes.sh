for f in ic_launcher.png; do
	echo $f;
	convert $f -thumbnail 36x36 ../opacclient/opacapp/src/main/res/drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../opacclient/opacapp/src/main/res/drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../opacclient/opacapp/src/main/res/drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../opacclient/opacapp/src/main/res/drawable-xhdpi/$f;
	convert $f -thumbnail 144x144 ../opacclient/opacapp/src/main/res/drawable-xxhdpi/$f;
done;
