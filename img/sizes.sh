for f in type*.png cover*.png; do
	convert $f -thumbnail 36x36 ../res/drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../res/drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../res/drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../res/drawable-xhdpi/$f;
done;
for f in ic_launcher.png; do
	convert $f -thumbnail 36x36 ../res/drawable-ldpi/$f;
	convert $f -thumbnail 48x48 ../res/drawable-mdpi/$f;
	convert $f -thumbnail 72x72 ../res/drawable-hdpi/$f;
	convert $f -thumbnail 96x96 ../res/drawable-xhdpi/$f;
done;
