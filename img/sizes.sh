cd logo/
for f in ic_launcher.svg; do
	echo $f;
	inkscape -zf $f -h 36 -e ../../opacclient/opacapp/src/main/res/drawable-ldpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -h 48 -e ../../opacclient/opacapp/src/main/res/drawable-mdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -h 72 -e ../../opacclient/opacapp/src/main/res/drawable-hdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -h 96 -e ../../opacclient/opacapp/src/main/res/drawable-xhdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -h 144 -e ../../opacclient/opacapp/src/main/res/drawable-xxhdpi/$(echo $f | sed s/svg/png/);
	inkscape -zf $f -h 192 -e ../../opacclient/opacapp/src/main/res/drawable-xxxhdpi/$(echo $f | sed s/svg/png/);
done;
