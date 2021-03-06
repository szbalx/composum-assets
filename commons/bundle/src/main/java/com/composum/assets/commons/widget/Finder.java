package com.composum.assets.commons.widget;

import com.composum.assets.commons.handle.ImageAsset;
import com.composum.assets.commons.handle.MetaData;
import com.composum.assets.commons.util.AdaptiveUtil;
import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.core.AbstractServletBean;
import com.composum.sling.core.BeanContext;
import org.apache.sling.api.resource.Resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Finder extends AbstractServletBean {

    public abstract class Thumbnail implements Comparable<Thumbnail> {

        public final Resource resource;

        public Thumbnail(Resource resource) {
            this.resource = resource;
        }

        public Resource getResource() {
            return resource;
        }

        public String getName() {
            return resource.getName();
        }

        public String getPath() {
            return resource.getPath();
        }

        public abstract String getContent();

        public abstract Date getLastModified();

        public abstract boolean isMetaAvailable();

        public abstract MetaData getMetaData();

        @Override
        public int compareTo(Thumbnail other) {
            return getName().compareTo(other.getName());
        }
    }

    public abstract class FileThumbnail extends Thumbnail {

        public final FileHandle file;

        public FileThumbnail(Resource resource) {
            super(resource);
            file = new FileHandle(resource);
        }

        public Date getLastModified() {
            return file.getLastModified().getTime();
        }
    }

    public class ImageThumbnail extends FileThumbnail {

        protected MetaData metaData;

        public ImageThumbnail(Resource resource) {
            super(resource);
            metaData = new MetaData(context, resource);
        }

        public String getContent() {
            return "<img class=\"thumbnail-image\" src=\"" + getPath() + "\"/>";
        }

        public boolean isMetaAvailable() {
            return metaData.isValid();
        }

        public MetaData getMetaData() {
            return metaData;
        }
    }

    public class AssetThumbnail extends Thumbnail {

        public final ImageAsset asset;

        public AssetThumbnail(Resource resource) {
            super(resource);
            asset = new ImageAsset(context, resource);
        }

        public Date getLastModified() {
            return asset.getLastModified().getTime();
        }

        public String getContent() {
            return "<img class=\"thumbnail-image\" src=\"" +
                    AdaptiveUtil.getImageUri(asset, "thumbnail", "large") +
                    "\"/>";
        }

        public boolean isMetaAvailable() {
            return false;
        }

        public MetaData getMetaData() {
            return null;
        }
    }

    public class VideoThumbnail extends FileThumbnail {

        public VideoThumbnail(Resource resource) {
            super(resource);
        }

        public String getContent() {
            return "<video class=\"thumbnail-video\">" +
                    "<source type=\"" + file.getMimeType() + "\" src=\"" + getPath() + "\"/></video>";
        }

        public boolean isMetaAvailable() {
            return false;
        }

        public MetaData getMetaData() {
            return null;
        }
    }

    private transient List<Thumbnail> thumbnails;

    public Finder(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public Finder(BeanContext context) {
        super(context);
    }

    public Finder() {
        super();
    }

    public List<Thumbnail> getThumbnails() {
        if (thumbnails == null) {
            thumbnails = findThumbnails();
        }
        return thumbnails;
    }

    protected List<Thumbnail> findThumbnails() {
        List<Thumbnail> thumbnails = new ArrayList<>();
        return thumbnails;
    }
}
