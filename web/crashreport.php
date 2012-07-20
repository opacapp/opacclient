<?php
mail('crashreport@raphaelmichel.de', 'OpacClient Crash', $_POST['version']." - Android ".$_POST['android']." - SDK ".$_POST['sdk']." - ".$_POST['device']." - ".$_POST['bib']."\n\n".$_POST['traceback'].(isset($_POST['html']) ? "\n\n\n\n".$_POST['html'] : ""), 'From: noreply@raphaelmichel.de');
