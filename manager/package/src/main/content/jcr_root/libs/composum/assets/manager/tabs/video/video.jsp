<%@page session="false" pageEncoding="utf-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<sling:defineObjects/>
<cpn:component id="video" type="com.composum.assets.commons.handle.VideoAsset">
    <div class="detail-panel video" data-path="${video.path}">
        <div class="detail-tabs action-bar btn-toolbar" role="toolbar">
            <div class="btn-group btn-group-sm" role="group">
                <button type="button" class="go-up fa fa-folder-o text-muted btn btn-default"
                        title="${cpn:i18n(slingRequest,'Go to Folder')}"><i
                        class="fa fa-chevron-up fa-stack-1x"></i><cpn:text
                        value="Folder" tagName="span" tagClass="label" i18n="true"/></button>
            </div>
            <div class="btn-group btn-group-sm" role="group">
                <div class="btn-group btn-group-sm" role="group">
                    <a class="general fa fa-file-video-o btn btn-default" href="#general" data-group="general"
                       title="Video"><span class="label">Video</span></a>
                </div>
            </div>
        </div>
        <div class="detail-content">
        </div>
    </div>
</cpn:component>
