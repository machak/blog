<!DOCTYPE HTML>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/blogjsp/include/include.jsp" %>
<%--
  * Copyright 2010 Jasha Joachimsthal
  *
  * Licensed under the Apache License, Version 2.0 (the  "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
--%>
<!--[if lt IE 7 ]> <html lang="en" class="no-js ie6"> <![endif]-->
<!--[if IE 7 ]>    <html lang="en" class="no-js ie7"> <![endif]-->
<!--[if IE 8 ]>    <html lang="en" class="no-js ie8"> <![endif]-->
<!--[if IE 9 ]>    <html lang="en" class="no-js ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en" class="no-js"> <!--<![endif]-->
<head>
  <meta charset="UTF-8"/>
  <hst:headContributions categoryExcludes="css,jsExternal,jsInline"/>
  <hst:headContributions categoryIncludes="css"/>
  <%-- Script for IE7+ to enable HTML5 elements --%>
  <script src="<hst:link path="/js/libs/modernizr-1.6.min.js"/>"></script>
</head>
<body>
<div id="container">
<hst:include ref="header"/>
<div id="main">
<hst:include ref="article"/>
<hst:include ref="aside"/>
</div>
<hst:include ref="footer"/>
</div>
<script src="<hst:link path="/js/libs/jquery-1.4.2.min.js"/>"></script>
<hst:headContributions categoryIncludes="jsExternal"/>
<hst:headContributions categoryIncludes="jsInline"/>
<%-- Google Analytics goes here --%>
<c:if test="${not empty labels['googleanalytics.account']}">
  <script>if (window.location.hostname!="localhost") {
    var _gaq = _gaq || [];
    _gaq.push(['_setAccount', '${labels['googleanalytics.account']}}']);
    <c:if test="${not empty labels['googleanalytics.domain']}">
    _gaq.push(['_setDomainName', '${labels['googleanalytics.domain']}']);
    </c:if>
    _gaq.push(['_trackPageview']);

    (function() {
      var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
      ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
      var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
    })();
  }</script>
</c:if>

</body>
</html>
