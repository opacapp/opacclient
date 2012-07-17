<?php
/*
 * bibs.json (bzw ein symlink) sollte im selben Ordner liegen!
 */
$bibs = json_decode(file_get_contents("bibs.json"));
$baseurl = $bibs->bibs->{$_GET['bib']}[0];
?><!DOCTYPE html>
<html>

<head>
	<title>Einen Moment…</title>
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<script type="text/javascript">
		var img = new Image();
		
		window.onload = function(){
			window.setTimeout(function(){
				location.href="<?php echo $baseurl; ?>/index.asp?MedienNr=<?php echo htmlspecialchars($_GET['id']); ?>";
			}, 3000);
		}
		
		img.src="<?php echo $baseurl; ?>/woload.asp?lkz=1&nextpage=";
	</script>
</head>

<body>
	<h1>Einen Moment bitte…</h1>
	<h2>Sie werden in wenigen Sekunden zur Stadtbibliothek <?php echo htmlspecialchars($_GET['bib']); ?> weitergeleitet…</h2>
	<p>Dieser Zwischenschritt ist leider aus technischen Gründen notwendig. Wir finden das auch doof.</p>
	<!--
	<p>Wenn die Weiterleitung nach fünf Sekunden nicht erfolgt, klicken Sie bitte folgenden Link:</p>
	<p><a href="<?php echo $baseurl; ?>/index.asp?MedienNr=<?php echo htmlspecialchars($_GET['id']); ?>"><?php echo $baseurl; ?>/index.asp?MedienNr=<?php echo htmlspecialchars($_GET['id']); ?></a></p>
	-->
</body>

</html>
