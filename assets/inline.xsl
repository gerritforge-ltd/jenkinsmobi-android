<!-- 
	XSL transformation inspired by XML Tree Google Chrome Extension by alan.stroop
	https://chrome.google.com/extensions/detail/gbammbheopgpmaagmckhpjbfgdfkpadb
 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:libxslt="http://xmlsoft.org/XSLT/namespace"
	exclude-result-prefixes="libxslt">
<xsl:template match="/">
<html>
<head>
<title><!-- TODO --></title>
<style>
html,body{1em 1em 0em;padding:0px;background-color:white;}
#tree{color:black;font-size:12px;font-family:verdana;padding-left:1em;word-wrap:break-word;}
.e{margin:2px 0px 5px;margin-left:5px;}
.c,div.inline{display:inline;margin:0;border-left:none}
.nm{color:purple;font-weight:bold}
.an{color:olive;}
.av{color:blue;}
.nx{color:gray;}
.cmt{color:green;font-style:italic;margin-left:5px;}
.t{white-space:pre;color:black;font-weight:bold;}
.cd{white-space:pre;color:teal;font-weight:bold;}
.pi{color:gold;margin-left:5px;}
.closed{background:lime;}
.h{background-color:silver;}
</style>
</head>
<body>
	<div id="tree">
		<xsl:apply-templates select="processing-instruction()" />
		<xsl:apply-templates select="node()[not(self::processing-instruction())]" />
	</div>
</body>
</html>
</xsl:template>
	
	<xsl:variable name="quot">&quot;</xsl:variable>
	<xsl:variable name="colon">:</xsl:variable>
	<xsl:variable name="empty"></xsl:variable>
	<xsl:variable name="apos">&apos;</xsl:variable>
	
	<xsl:variable name="rootNodeNamespaces">
		<xsl:for-each
			select="/*/namespace::*[. != 'http://www.w3.org/XML/1998/namespace']">
			<xsl:text> xmlns</xsl:text>
			<xsl:if test="normalize-space(name(.)) != $empty">
				<xsl:value-of select="concat($colon,name(.))" />
			</xsl:if>
			<xsl:text>=</xsl:text>
			<xsl:value-of select="concat($quot,.,$quot)" />
		</xsl:for-each>
	</xsl:variable>

	<xsl:template match="*">
		<div class="e">
			<xsl:variable name="tag">
				<span class="nm">
					<xsl:value-of select="name(.)" />
				</span>
			</xsl:variable>
			<span>
				<xsl:attribute name="class">
                	<!-- node -->
                    <xsl:text>n n</xsl:text>
                    <xsl:choose>
                        <xsl:when test="node()">
                        	<!-- start -->
                            <xsl:text>s</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                        	<!-- self close -->
                            <xsl:text>sc</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
				<xsl:text>&lt;</xsl:text>
				<xsl:copy-of select="$tag" />
			</span>

			<xsl:apply-templates select="@*" />

			<xsl:choose>
				<xsl:when test=". = /*">
					<!-- render all namespaces declared on the root element -->
					<span class="nx">
						<xsl:value-of select="$rootNodeNamespaces" />
					</span>
				</xsl:when>
				<xsl:otherwise>
					<xsl:variable name="currentNamespace">
						<xsl:call-template name="constructNamespace">
							<xsl:with-param name="node" select="." />
						</xsl:call-template>
					</xsl:variable>
					<xsl:variable name="parentNamespace">
						<xsl:call-template name="constructNamespace">
							<xsl:with-param name="node" select=".." />
						</xsl:call-template>
					</xsl:variable>
					<xsl:if
						test="$currentNamespace != $parentNamespace and not(contains($rootNodeNamespaces,$currentNamespace))">
						<span class="nx">
							<xsl:value-of select="$currentNamespace" />
						</span>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>

			<xsl:if test="not(node())">
				<xsl:text>/</xsl:text>
			</xsl:if>
			<xsl:text>&gt;</xsl:text>

			<xsl:if test="node()">
				<div class="c">
					<xsl:apply-templates />
				</div>
				<!-- node and node-end -->
				<span class="n ne">
					<xsl:text>&lt;</xsl:text>
					<xsl:text>/</xsl:text>
					<xsl:copy-of select="$tag" />
					<xsl:text>&gt;</xsl:text>
				</span>
			</xsl:if>
		</div>
	</xsl:template>
	<xsl:template match="@*">
		<xsl:text> </xsl:text>
		<span class="an">
			<xsl:value-of select="name(.)" />
		</span>
		<xsl:text>=</xsl:text>
		<span class="av">
			<xsl:value-of select="concat($quot, ., $quot)" />
		</span>
		<!-- if we have a namespace AND our namespace isn't the same as parent 
			AND the current element is not root so we avoid duplicate declarations -->
		<xsl:if
			test="namespace-uri(.) and namespace-uri(.) != namespace-uri(..) and parent::* != /*">
			<xsl:variable name="currentNamespace">
				<xsl:call-template name="constructNamespace">
					<xsl:with-param name="node" select="." />
				</xsl:call-template>
			</xsl:variable>
			<xsl:if test="not(contains($rootNodeNamespaces,$currentNamespace))">
				<span class="nx">
					<xsl:call-template name="constructNamespace">
						<xsl:with-param name="node" select="." />
					</xsl:call-template>
				</span>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	<xsl:template match="text()">
		<xsl:if test="normalize-space(.)">
			<span class="t">
				<xsl:value-of select="." />
			</span>
		</xsl:if>
	</xsl:template>
	<xsl:template match="comment()">
		<pre class="cmt">
			<xsl:text>&lt;!--</xsl:text>
			<xsl:value-of select="." />
			<xsl:text>--&gt;</xsl:text>
		</pre>
	</xsl:template>
	<xsl:template match="processing-instruction()">
		<div class="pi">
			<xsl:text>&lt;?</xsl:text>
			<xsl:value-of select="name(.)" />
			<xsl:text> </xsl:text>
			<xsl:value-of select="." />
			<xsl:text>?&gt;</xsl:text>
		</div>
	</xsl:template>
	<xsl:template name="xpath">
		<xsl:for-each select="ancestor-or-self::*">
			<xsl:text>/</xsl:text>
			<xsl:value-of select="name()" />
		</xsl:for-each>
	</xsl:template>
	<xsl:template name="findUniqueNamespaces">
		<xsl:param name="namespaceList" />
		<xsl:param name="uniqueList" />
		<xsl:choose>
			<xsl:when test="contains($namespaceList,'!|!')">
				<xsl:variable name="namespaceDeclaration"
					select="substring-before($namespaceList,'!|!')" />
				<xsl:variable name="list">
					<xsl:if test="contains($uniqueList,$namespaceDeclaration) = false()">
						<xsl:text> </xsl:text>
						<xsl:value-of select="$namespaceDeclaration" />
					</xsl:if>
				</xsl:variable>
				<xsl:call-template name="findUniqueNamespaces">
					<xsl:with-param name="namespaceList"
						select="substring-after($namespaceList,'!|!')" />
					<xsl:with-param name="uniqueList" select="concat($uniqueList,$list)" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$uniqueList" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="constructNamespace">
		<xsl:param name="node" />
		<xsl:variable name="prefix">
			<xsl:if test="contains(name($node), $colon)">
				<xsl:value-of select="substring-before(name($node), $colon)" />
			</xsl:if>
		</xsl:variable>
		<xsl:text> xmlns</xsl:text>
		<xsl:if test="normalize-space($prefix) != $empty">
			<xsl:value-of select="concat($colon,$prefix)" />
		</xsl:if>
		<xsl:text>=</xsl:text>
		<xsl:value-of select="concat($quot,namespace-uri($node),$quot)" />
	</xsl:template>
</xsl:stylesheet>