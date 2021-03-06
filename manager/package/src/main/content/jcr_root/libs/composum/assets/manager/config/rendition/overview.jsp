<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="configBean" type="com.composum.assets.manager.config.RenditionConfigBean">
    <div class="rendition config overview">
        <div class="overlay">
            <div class="overlay-background image view">
                <div class="image-wrapper">
                    <div class="image-background" style="background-image: url(${cpn:url(slingRequest,'/libs/composum/nodes/console/browser/images/image-background.png')})">
                        <img src="${configBean.imageUrl}">
                    </div>
                </div>
            </div>
            <div class="overlay-foreground">
                <sling:include resourceType="composum/assets/manager/config/properties"/>
            </div>
        </div>
    </div>
</cpn:component>
