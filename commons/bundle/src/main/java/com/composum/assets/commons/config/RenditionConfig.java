/*
 * copyright (c) 2015 IST GmbH Dresden, Germany
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.assets.commons.config;

import com.composum.assets.commons.AssetsConstants;
import com.composum.assets.commons.config.aspect.Crop;
import com.composum.assets.commons.config.aspect.Example;
import com.composum.assets.commons.config.aspect.File;
import com.composum.assets.commons.config.aspect.Size;
import com.composum.assets.commons.config.aspect.Watermark;
import com.composum.assets.commons.config.transform.Blur;
import com.composum.sling.core.ResourceHandle;
import org.apache.sling.api.resource.Resource;

import java.util.Collections;
import java.util.List;

public class RenditionConfig extends ConfigHandle {

    public static final String NODE_TYPE = AssetsConstants.NODE_TYPE_RENDITION_CONFIG;
    public static final String RESOURCE_TYPE = AssetsConstants.RESOURCE_TYPE_RENDITION_CONFIG;

    protected final VariationConfig variation;

    private transient File file;
    private transient Size size;
    private transient Crop crop;
    private transient Blur blur;
    private transient Watermark watermark;
    private transient Example exampleImage;

    public RenditionConfig(VariationConfig variation, Resource resource) {
        this(variation, Collections.singletonList(ResourceHandle.use(resource)));
    }

    public RenditionConfig(VariationConfig variation, List<ResourceHandle> cascade) {
        super(cascade);
        this.variation = variation;
    }

    public AssetConfig getAssetConfig() {
        return getVariation().getAssetConfig();
    }

    public VariationConfig getVariation() {
        return variation;
    }

    public RenditionConfig getOriginal() {
        return getVariation().getOriginal();
    }

    public File getFile() {
        if (file == null) {
            file = new File(this);
        }
        return file;
    }

    public Size getSize() {
        if (size == null) {
            size = new Size(this);
        }
        return size;
    }

    public Crop getCrop() {
        if (crop == null) {
            crop = new Crop(this);
        }
        return crop;
    }

    public Blur getBlur() {
        if (blur == null) {
            blur = new Blur(this);
        }
        return blur;
    }

    public Watermark getWatermark() {
        if (watermark == null) {
            watermark = new Watermark(this);
        }
        return watermark;
    }

    public Example getExampleImage() {
        if (exampleImage == null) {
            exampleImage = new Example(this);
        }
        return exampleImage;
    }
}
