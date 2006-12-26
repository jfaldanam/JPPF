<?php
	session_start();
?>
<html>
		<head>
		<title>Java Parallel Processing Framework Links Page</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, Java, Parallel Computing, Distributed Computing, Grid Computing, Cluster, Grid">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
				<table align="center" width="80%" cellspacing="0" cellpadding="5"
			class="table_" style="background: url('images/grid.gif'); background-repeat: repeat; background-attachment: fixed">
			<tr><td height="5"></td></tr>
			<tr>
				<td width="30%" align="left" valign="center">
					<h3>Java Parallel Processing Framework</h3>
				</td>
				<td width="40%" align="center">
					<img src="images/logo.gif" border="0" alt="Java Parallel Processing Framework"/>
				</td>
				<td width="30%" align="right">
					<a href="http://sourceforge.net" target="_top">
						<img src="http://sourceforge.net/sflogo.php?group_id=135654&amp;type=4"
							width="125" height="37" border="0" alt="SourceForge.net Logo" />
					</a>
				</td>
			</tr>
			<tr><td height="5"></td></tr>
		</table>
		<!--<table border="0" style="background-color: #8080FF" cellspacing="0" cellpadding="0" width="80%">-->
		<table style="background: url('images/bkg-menu.gif'); background-repeat: repeat; background-attachment: fixed"
			cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="5">
						<tr>
							<td class="menu_first"><a href="index.html">Home</a></td>
							<!--<td class="menu"><a href="JPPF-Overview.html">Overview</a></td>-->
							<td class="menu"><a href="presentation.php?current=0">Overview</a></td>
							<td class="menu"><a href="http://sourceforge.net/project/showfiles.php?group_id=135654">Files</a></td>
							<td class="menu"><a href="./wiki">Wiki &amp; Doc</a></td>
							<td class="menu"><a href="./forums">Forums</a></td>
							<td class="menu"><a href="screenshots.html">Screenshots</a></td >
							<td class="menu"><a href="api/index.html">API Doc</a></td >
							<td class="menu"><a href="faq.php">Faqs</a></td>
							<td class="menu"><a href="news.php">News</a></td>
							<td class="menu"><a href="http://sourceforge.net/projects/jppf-project">Project</a></td>
							<td class="menu"><a href="links.php">Links</a></td>
							<td class="menu"></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
<?php
			$defined = $_SESSION["defined"];
			if (!$defined)
			{
				$_SESSION["defined"] = "true";
				$_SESSION["first"] = 0;
				$_SESSION["last"] = 20;
			}
			$current = $_REQUEST["current"];
			if (!$current)
			{
				$currrent = 0;
			}
?>
			<table class="table_" cellspacing="0" cellpadding="0" width="80%">
				<tr><td align="center">
<?php
					// Link to first page
					if ($current > $_SESSION["first"])
					{
						echo '<a href="presentation.php?current=', $_SESSION["first"], '">';
						echo '<img src="overview/first1.gif" border=0 alt="Last page"></a>';
					}
					else
					{
						echo '<img src="overview/first0.gif" border=0 alt="Last page">';
					}
					echo '&nbsp;';
					// Link to previous page
					if ($current > $_SESSION["first"])
					{
						echo '<a href="presentation.php?current=', ($current-1), '">';
						echo '<img src="overview/prev1.gif" border=0 alt="Back"></a>';
					}
					else
					{
						echo '<img src="overview/prev0.gif" border=0 alt="Back">';
					}
					echo '&nbsp;';
					// Link to next page
					if ($current < $_SESSION["last"])
					{
						echo '<a href="presentation.php?current=', ($current+1), '">';
						echo '<img src="overview/next1.gif" border=0 alt="Continue"></a>';
					}
					else
					{
						echo '<img src="overview/next0.gif" border=0 alt="Continue">';
					}
					echo '&nbsp;';
					// Link to last page
					if ($current < $_SESSION["last"])
					{
						echo '<a href="presentation.php?current=', $_SESSION["last"], '">';
						echo '<img src="overview/last1.gif" border=0 alt="Last page"></a>';
					}
					else
					{
						echo '<img src="overview/last0.gif" border=0 alt="Last page"></a>';
					}
?>
				</td>
				<td align="left">This presentation is available in <a href="documents/JPPF-Presentation.pdf">PDF Format</a></td>
				</tr>
				<tr><td align="center" colspan="2">
<?php
					echo '<img src="overview/img', $current, '.gif">';
?>
				</td></tr>
			</table>
		</div>
	</body>
</html>
