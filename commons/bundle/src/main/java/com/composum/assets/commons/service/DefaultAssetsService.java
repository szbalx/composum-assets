/*
 * copyright (c) 2015 IST GmbH Dresden, Germany
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.assets.commons.service;

import com.composum.assets.commons.AssetsConstants;
import com.composum.assets.commons.config.AssetConfig;
import com.composum.assets.commons.config.ConfigHandle;
import com.composum.assets.commons.handle.AssetRendition;
import com.composum.assets.commons.handle.AssetVariation;
import com.composum.assets.commons.handle.ImageAsset;
import com.composum.sling.clientlibs.handle.FileHandle;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.MimeTypeUtil;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.tika.mime.MimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.composum.assets.commons.handle.AssetHandle.IMAGE_RESOURCE_TYPE;

@Component
@Service
@SuppressWarnings("deprecation")
public class DefaultAssetsService implements AssetsService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAssetsService.class);

    public static final String PATH_SEP = "/";

    public static final Map<String, Object> FOLDER_PROPERTIES;
    public static final Map<String, Object> CONTENT_PROPERTIES;
    public static final Map<String, Object> ASSET_CONFIG_PROPERTIES;
    public static final Map<String, Object> VARIATION_CONFIG_PROPERTIES;
    public static final Map<String, Object> RENDITION_CONFIG_PROPERTIES;
    public static final Map<String, Map<String, Object>> CONFIG_PROPERTIES;
    public static final Map<String, String> CONFIG_CHILD_TYPE;

    static {
        FOLDER_PROPERTIES = new HashMap<>();
        FOLDER_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_SLING_FOLDER);
    }

    static {
        CONTENT_PROPERTIES = new HashMap<>();
        CONTENT_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED);
    }

    static {
        ASSET_CONFIG_PROPERTIES = new HashMap<>();
        ASSET_CONFIG_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, AssetsConstants.NODE_TYPE_ASSET_CONFIG);
        ASSET_CONFIG_PROPERTIES.put(ResourceUtil.PROP_RESOURCE_TYPE, AssetsConstants.RESOURCE_TYPE_CONFIG);
        VARIATION_CONFIG_PROPERTIES = new HashMap<>();
        VARIATION_CONFIG_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, AssetsConstants.NODE_TYPE_VARIATION_CONFIG);
        VARIATION_CONFIG_PROPERTIES.put(ResourceUtil.PROP_RESOURCE_TYPE, AssetsConstants.RESOURCE_TYPE_VARIATION_CONFIG);
        RENDITION_CONFIG_PROPERTIES = new HashMap<>();
        RENDITION_CONFIG_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, AssetsConstants.NODE_TYPE_RENDITION_CONFIG);
        RENDITION_CONFIG_PROPERTIES.put(ResourceUtil.PROP_RESOURCE_TYPE, AssetsConstants.RESOURCE_TYPE_RENDITION_CONFIG);
        CONFIG_PROPERTIES = new HashMap<>();
        CONFIG_PROPERTIES.put(AssetsConstants.NODE_TYPE_ASSET_CONFIG, ASSET_CONFIG_PROPERTIES);
        CONFIG_PROPERTIES.put(AssetsConstants.NODE_TYPE_VARIATION_CONFIG, VARIATION_CONFIG_PROPERTIES);
        CONFIG_PROPERTIES.put(AssetsConstants.NODE_TYPE_RENDITION_CONFIG, RENDITION_CONFIG_PROPERTIES);
        CONFIG_CHILD_TYPE = new HashMap<>();
        CONFIG_CHILD_TYPE.put(AssetsConstants.NODE_TYPE_ASSET_CONFIG, AssetsConstants.NODE_TYPE_VARIATION_CONFIG);
        CONFIG_CHILD_TYPE.put(AssetsConstants.NODE_TYPE_VARIATION_CONFIG, AssetsConstants.NODE_TYPE_RENDITION_CONFIG);
    }

    public static final List<String> COPY_KEYS = Arrays.asList(

            ConfigHandle.CATEGORIES,
            ConfigHandle.FILE_QUALITY,

            ConfigHandle.WIDTH,
            ConfigHandle.HEIGHT,
            ConfigHandle.ASPECT_RATIO,

            ConfigHandle.CROP_VERTICAL,
            ConfigHandle.CROP_HORIZONTAL,
            ConfigHandle.CROP_SCALE,

            ConfigHandle.WATERMARK_TEXT,
            ConfigHandle.WATERMARK_FONT_FAMILY,
            ConfigHandle.WATERMARK_FONT_BOLD,
            ConfigHandle.WATERMARK_FONT_ITALIC,
            ConfigHandle.WATERMARK_FONT_SIZE,
            ConfigHandle.WATERMARK_POS_VERTICAL,
            ConfigHandle.WATERMARK_POS_HORIZONTAL,
            ConfigHandle.WATERMARK_COLOR,
            ConfigHandle.WATERMARK_ALPHA,

            ConfigHandle.EXAMPLE_IMAGE_PATH,

            ConfigHandle.TRANSFORMATION_BLUR_FACTOR
    );

    public static final Map<String, Object> IMAGE_PROPERTIES;
    public static final Map<String, Object> IMAGE_CONTENT_PROPERTIES;
    public static final Map<String, Object> IMAGE_META_PROPERTIES;

    static {
        IMAGE_PROPERTIES = new HashMap<>();
        IMAGE_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, AssetsConstants.NODE_TYPE_ASSET);
        IMAGE_PROPERTIES.put(ResourceUtil.PROP_RESOURCE_TYPE, IMAGE_RESOURCE_TYPE);
        IMAGE_CONTENT_PROPERTIES = new HashMap<>();
        IMAGE_CONTENT_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, AssetsConstants.NODE_TYPE_ASSET_CONTENT);
        IMAGE_META_PROPERTIES = new HashMap<>();
        IMAGE_META_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, AssetsConstants.NODE_TYPE_META_DATA);
    }

    public static final Map<String, Object> IMAGE_FILE_PROPERTIES;
    public static final Map<String, Object> IMAGE_FILE_CONTENT_PROPERTIES;

    static {
        IMAGE_FILE_PROPERTIES = new HashMap<>();
        IMAGE_FILE_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_FILE);
        IMAGE_FILE_CONTENT_PROPERTIES = new HashMap<>();
        IMAGE_FILE_CONTENT_PROPERTIES.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_RESOURCE);
    }

    @Reference
    protected AdaptiveImageService adaptiveImageService;

    @Override
    public void uploadImageAsset(BeanContext context, String parentPath, String name,
                                 String variation, InputStream imageData)
            throws Exception {
        ResourceResolver resolver = context.getResolver();
        String pathAndName = parentPath + "/" + name;
        Resource assetResource = resolver.getResource(pathAndName);
        if (assetResource == null || !assetResource.isResourceType(AssetsConstants.NODE_TYPE_ASSET)) {
            createImageAsset(context, parentPath, name, variation, imageData);
        } else {
            changeImageAsset(context, assetResource, variation, imageData);
        }
    }

    @Override
    public Resource createImageAsset(BeanContext context, String parentPath, String name,
                                     String variation, InputStream imageData)
            throws Exception {
        ResourceResolver resolver = context.getResolver();
        String pathAndName = parentPath + "/" + name;
        Resource assetResource = resolver.getResource(pathAndName);
        deleteAsset(assetResource);
        Resource parent = getOrCreateFolder(resolver, parentPath);
        LOG.info("image.create: " + pathAndName);
        assetResource = resolver.create(parent, name, IMAGE_PROPERTIES);
        Resource content = resolver.create(assetResource, ResourceUtil.CONTENT_NODE, IMAGE_CONTENT_PROPERTIES);
        ImageAsset imageAsset = new ImageAsset(context, assetResource);
        storeImageOriginal(imageAsset, variation, imageData);
        return assetResource;
    }

    @Override
    public void transformToImageAsset(BeanContext context, Resource imageResource)
            throws PersistenceException, RepositoryException {
        if (!ResourceUtil.isResourceType(imageResource, AssetsConstants.NODE_TYPE_ASSET)) {
            ResourceResolver resolver = context.getResolver();
            Resource parent = imageResource.getParent();
            String assetPath = imageResource.getPath();
            String name = imageResource.getName();
            String tmpPath = getAssetTmpPath(resolver, name);
            // CRUD not possible because the resolvers 'move' performs 'copy' internally
            // and the file is referenceable... - exception on copying the 'uuid'!
            Session session = resolver.adaptTo(Session.class);
            session.move(assetPath, tmpPath);
            resolver.refresh();
            Resource assetResource = resolver.create(parent, name, IMAGE_PROPERTIES);
            Resource content = resolver.create(assetResource, ResourceUtil.CONTENT_NODE, IMAGE_CONTENT_PROPERTIES);
            ImageAsset imageAsset = new ImageAsset(context, assetResource);
            AssetVariation variation = imageAsset.getOrCreateVariation(null);
            AssetRendition rendition = variation.getOrCreateOriginal();
            session.move(tmpPath, rendition.getPath() + "/" + name);
            resolver.refresh();
        }
    }

    @Override
    public void transformToSimpleImage(BeanContext context, Resource assetResource)
            throws PersistenceException, RepositoryException {
        if (!ResourceUtil.isResourceType(assetResource, JcrConstants.NT_FILE)) {
            ResourceResolver resolver = context.getResolver();
            String assetPath = assetResource.getPath();
            ImageAsset imageAsset = new ImageAsset(context, assetResource);
            AssetRendition original = imageAsset.getOriginal();
            FileHandle file = original.getFile();
            String filePath = file.getPath();
            String tmpPath = getAssetTmpPath(resolver, assetResource.getName());
            // CRUD not possible because - see above...
            Session session = resolver.adaptTo(Session.class);
            session.move(filePath, tmpPath);
            session.removeItem(assetPath);
            session.move(tmpPath, assetPath);
            resolver.refresh();
        }
    }

    @Override
    public void changeImageAsset(BeanContext context, Resource assetResource,
                                 String variation, InputStream imageData)
            throws Exception {
        ImageAsset imageAsset = new ImageAsset(context, assetResource);
        storeImageOriginal(imageAsset, variation, imageData);
    }

    protected void storeImageOriginal(ImageAsset imageAsset, String variationKey, InputStream imageData)
            throws Exception {
        AssetVariation variation = imageAsset.getOrCreateVariation(variationKey);
        AssetRendition rendition = variation.getOrCreateOriginal();
        FileHandle file = rendition.getFile();
        if (file == null) {
            Resource renditionResource = rendition.getResource();
            ResourceResolver resolver = renditionResource.getResourceResolver();
            Resource fileResource = resolver.create(renditionResource, imageAsset.getName(), IMAGE_FILE_PROPERTIES);
            resolver.create(fileResource, ResourceUtil.CONTENT_NODE, IMAGE_FILE_CONTENT_PROPERTIES);
            file = new FileHandle(fileResource);
        }
        file.storeContent(imageData);
        ResourceHandle fileContent = file.getContent();
        MimeType mimeType = MimeTypeUtil.getMimeType(fileContent);
        if (mimeType != null) {
            fileContent.setProperty(ResourceUtil.PROP_MIME_TYPE, mimeType.toString());
        }
        adaptiveImageService.dropRenditions(imageAsset.getPath(), variationKey, null);
    }

    public void deleteAsset(Resource assetResource)
            throws PersistenceException {
        if (assetResource != null && !ResourceUtil.isNonExistingResource(assetResource)) {
            LOG.info("asset.delete: " + assetResource.getPath());
            assetResource.getResourceResolver().delete(assetResource);
        }
    }

    @Override
    public void setDefaultConfiguration(BeanContext context, Resource configResource, boolean commit)
            throws PersistenceException {
        String configPath = configResource.getPath();
        Resource folder = configResource.getParent();
        List<Resource> configList = ResourceUtil.getChildrenByType(folder, AssetsConstants.ASSET_CONFIG_TYPE_SET);
        for (Resource sibling : configList) {
            if (!sibling.getPath().equals(configPath)) {
                ValueMap values = sibling.adaptTo(ModifiableValueMap.class);
                List<String> categories = new ArrayList<>();
                boolean changed = false;
                for (String category : values.get(ConfigHandle.CATEGORIES, new String[0])) {
                    if (!ConfigHandle.DEFAULT.equals(category)) {
                        categories.add(category);
                    } else {
                        changed = true;
                    }
                }
                if (changed) {
                    values.put(ConfigHandle.CATEGORIES, categories.toArray());
                }
            }
        }
        ValueMap values = configResource.adaptTo(ModifiableValueMap.class);
        List<String> categories = new ArrayList<>(Arrays.asList(values.get(ConfigHandle.CATEGORIES, new String[0])));
        if (!categories.contains(ConfigHandle.DEFAULT)) {
            categories.add(ConfigHandle.DEFAULT);
            values.put(ConfigHandle.CATEGORIES, categories.toArray());
        }
        if (commit) {
            ResourceResolver resolver = context.getResolver();
            resolver.commit();
        }
    }

    @Override
    public Resource getOrCreateConfiguration(BeanContext context, String path, boolean commit)
            throws PersistenceException {
        Resource config = null;
        ResourceResolver resolver = context.getResolver();
        Resource folder = resolver.getResource(path);
        if (folder != null) {
            Resource content = folder.getChild(JcrConstants.JCR_CONTENT);
            if (content == null) {
                LOG.info("folder.createContent: " + path);
                content = resolver.create(folder, JcrConstants.JCR_CONTENT, CONTENT_PROPERTIES);
            } else {
                List<Resource> configList = ResourceUtil.getChildrenByType(content, AssetsConstants.NODE_TYPE_ASSET_CONFIG);
                if (configList.size() > 0) {
                    config = configList.get(0);
                }
            }
            if (config == null) {
                LOG.info("folder.createConfig: " + path);
                config = resolver.create(content, AssetConfig.CHILD_NAME, ASSET_CONFIG_PROPERTIES);
                if (commit) {
                    resolver.commit();
                }
            }
        }
        return config;
    }

    @Override
    public Resource copyConfigNode(BeanContext context, Resource parent, Resource template, boolean commit)
            throws PersistenceException {
        Resource configNode = null;
        if (parent != null && template != null) {
            String name = template.getName();
            configNode = createConfigNode(context, parent, name, false);
            if (configNode != null) {
                ResourceResolver resolver = context.getResolver();
                ModifiableValueMap values = configNode.adaptTo(ModifiableValueMap.class);
                ValueMap templateValues = template.getValueMap();
                for (Map.Entry<String, Object> entry : templateValues.entrySet()) {
                    String key = entry.getKey();
                    if (COPY_KEYS.contains(key)) {
                        values.put(key, entry.getValue());
                    }
                }
                if (commit) {
                    resolver.commit();
                }
            }
        }
        return configNode;
    }

    @Override
    public Resource createConfigNode(BeanContext context, Resource parent, String name, boolean commit)
            throws PersistenceException {
        Resource configNode = null;
        if (parent != null) {
            String parentType = ResourceUtil.getPrimaryType(parent);
            String childType = CONFIG_CHILD_TYPE.get(parentType);
            if (childType == null) {
                if (AssetsConstants.NODE_TYPE_RENDITION_CONFIG.equals(parentType)) {
                    // if the parent is a rendition config create a sibling
                    parent = parent.getParent();
                    configNode = parent.getChild(name);
                    parentType = ResourceUtil.getPrimaryType(parent);
                    childType = CONFIG_CHILD_TYPE.get(parentType);
                } else {
                    // otherwise we assume that the parent is a folder
                    // which should be transformed into a configuration node
                    return getOrCreateConfiguration(context, parent.getPath(), commit);
                }
            }
            if (StringUtils.isNotBlank(name)) {
                configNode = parent.getChild(name);
                if (configNode == null && StringUtils.isNotBlank(childType)) {
                    ResourceResolver resolver = context.getResolver();
                    LOG.info("folder.createConfigNode: " + parent.getPath() + ":" + name);
                    configNode = resolver.create(parent, name, CONFIG_PROPERTIES.get(childType));
                    if (commit) {
                        resolver.commit();
                    }
                }
            }
        }
        return configNode;
    }

    @Override
    public void deleteConfigNode(BeanContext context, Resource configNode, boolean commit)
            throws PersistenceException {
        if (configNode != null) {
            ResourceResolver resolver = context.getResolver();
            String configPath = configNode.getPath();
            String configType = ResourceUtil.getPrimaryType(configNode);
            if (AssetsConstants.NODE_TYPE_ASSET_CONFIG.equals(configType)) {
                LOG.info("folder.deleteConfig: " + configPath);
            } else {
                LOG.info("assetConfig.delete: " + configPath);
            }
            resolver.delete(configNode);
            if (commit) {
                resolver.commit();
            }
        }
    }

    protected Resource getOrCreateFolder(ResourceResolver resolver, String path)
            throws PersistenceException {
        Resource resource = resolver.getResource(path);
        if (resource == null) {
            String parentPath = StringUtils.substringBeforeLast(path, PATH_SEP);
            String name = StringUtils.substringAfterLast(path, PATH_SEP);
            Resource parent = getOrCreateFolder(resolver, parentPath);
            LOG.info("folder.create: " + path);
            resource = resolver.create(parent, name, FOLDER_PROPERTIES);
        }
        return resource;
    }

    protected String getAssetTmpPath(ResourceResolver resolver, String name)
            throws PersistenceException {
        return getAssetTmpFolder(resolver).getPath() + "/" + name;
    }

    protected Resource getAssetTmpFolder(ResourceResolver resolver)
            throws PersistenceException {
        return getOrCreateFolder(resolver, "/var/tmp/assets");
    }
}
