<%@ page import="org.eclipse.help.servlet.*" errorPage="err.jsp"%>

<% 
	// calls the utility class to initialize the application
	application.getRequestDispatcher("/servlet/org.eclipse.help.servlet.InitServlet").include(request,response);
%>

<html>
<head>
<title>Tabs</title>

<link rel="stylesheet" TYPE="text/css" HREF="help.css" TITLE="sea">
    
<style type="text/css">

BODY {
	/*background-color:ActiveBorder;*/
	background-color:#D4D0C8;
	background-image:url(images/eclipse_tabbackground.gif);
	background-repeat:repeat-x;
	margin-width:0;
	margin-left:0px;
	border-bottom: 2px solid #848284;
}


.tab {
	top:0;
	height:20;
	margin-top:2;
	padding:0px 2px 0px 2px;
	border:0;
	cursor:default;
}

.pressed {
	top:0;
	margin-top:1;
	padding:0px 2px 0px 2px;
	height:20;
	border-bottom:1px solid #ffffff;
	border-top:1px solid #ffffff;
	cursor:default;
}


IMG {
	margin:0;
	border:0;
	padding-left:0;
}
</style>
     
<script language="JavaScript">
 
// input parameters
var args = parent.parseQueryString();
var switchTabTimerSet;
var switchTabArg;

/* 
 * Switch tabs.
 */ 
function switchTab(nav)
{
	// wait for parent.NavFrame.turnToFrame
	if(!parent.NavFrame.turnToFrame)
	{
		if(!this.switchTabTimerSet)
		{
			this.switchTabArg=nav;
			this.switchTabTimerSet=setInterval("switchTab(switchTabArg)", 10);
		}
		return;
	}
	if(this.switchTabTimerSet)
		clearInterval(this.switchTabTimerSet);
		
	// show appropriate IFrame
	parent.NavFrame.turnToFrame(nav);
 
 	// show the appropriate pressed tab
  	var buttons = document.body.getElementsByTagName("A");
  	for (var i=0; i<buttons.length; i++)
  	{
  		if (buttons[i].id == nav) // Note: assumes the same id shared by tabs and iframes
			buttons[i].className = "pressed";
		else
			buttons[i].className = "tab";
 	 }
  
  	// set the images for borders
  	var tocI = document.getElementById("tocI");
  	var searchI = document.getElementById("searchI");
  	var linksI = document.getElementById("linksI");
  	if (nav == "toc")
  	{
  		tocI.src = "images/rightBorder.gif";
		searchI.src = "images/middleBorder.gif";
		linksI.src = "images/noBorder.gif";
  	}
  	else if (nav == "search")
  	{
  		tocI.src = "images/leftBorder.gif";
		searchI.src = "images/rightBorder.gif";
		linksI.src = "images/noBorder.gif";
  	}
  	else if (nav == "links")
  	{
  		tocI.src = "images/middleBorder.gif";
		searchI.src = "images/leftBorder.gif";
		linksI.src = "images/rightBorder.gif";
  	}	
}
 
function onloadHandler()
{	
	var tab = "toc";
	if (args && args["tab"])
	    tab = args["tab"];
	switchTab(tab);
}
 
</script>


</head>
   
<body onload="onloadHandler()">
   <table cellspacing="0" cellpadding="0" border="0">
   <tr>
   <td><a class="tab" id="toc" href="javascript:switchTab('toc')">&nbsp;<%=WebappResources.getString("Content", null)%></a></td>
   <td><img id="tocI" src="images/rightBorder.gif" width="2" height="20"></td>
   <td><a class="tab" id="search" href="javascript:switchTab('search')"><%=WebappResources.getString("Search", null)%></a></td>
   <td><img id="searchI" src="images/middleBorder.gif" width="2" height="20"></td>
   <td><a class="tab" id="links" href="javascript:switchTab('links')"><%=WebappResources.getString("Links", null)%></a></td>
   <td><img id="linksI" src="images/noBorder.gif" width="2" height="20"></td>
   </tr>
   </table>
</body>
</html>

