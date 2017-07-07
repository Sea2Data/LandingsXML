<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : fangst.xsl
    Created on : July 7, 2017, 9:52 AM
    Author     : a5362
    Description:
       Extracts fangst and redskap along with seddeldata. Discards rest.
       Use to filter before parsing.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:land="http://www.imr.no/formats/landinger/v1" xmlns:wl="whitelist">
    <xsl:output method="xml" indent="yes"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:template match="/land:landingsdata">
        <xsl:copy>
            <xsl:apply-templates select="land:seddellinje"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="land:seddellinje">
        <xsl:copy>
            <xsl:copy-of select="@*"/> <!-- copy attributes -->
            <xsl:copy-of select="*[not(*) and text() != '']"/> <!-- copy childless elements, with non-empty textnodes -->
            <xsl:apply-templates select="land:fangstdata"/>
            <xsl:apply-templates select="land:redskap"/>                   
        </xsl:copy>
    </xsl:template>

    <xsl:template match="land:fangstdata">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="land:redskap">
        <xsl:copy-of select="."/>
    </xsl:template>
               
</xsl:stylesheet>
