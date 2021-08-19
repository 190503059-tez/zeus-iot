package com.zmops.iot.web.device.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zmops.iot.domain.product.Product;
import com.zmops.iot.domain.product.ProductAttribute;
import com.zmops.iot.domain.product.query.QProduct;
import com.zmops.iot.domain.product.query.QProductAttribute;
import com.zmops.iot.model.exception.ServiceException;
import com.zmops.iot.model.page.Pager;
import com.zmops.iot.util.ToolUtil;
import com.zmops.iot.web.exception.enums.BizExceptionEnum;
import com.zmops.iot.web.product.dto.ProductAttr;
import com.zmops.iot.web.product.dto.ProductAttrDto;
import com.zmops.iot.web.product.dto.ProductTag;
import com.zmops.iot.web.product.dto.param.ProductAttrParam;
import com.zmops.zeus.driver.entity.ZbxProcessingStep;
import com.zmops.zeus.driver.service.ZbxItem;
import io.ebean.DB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yefei
 * <p>
 * 设备物模型服务
 **/
@Service
public class DeviceModelService {

    @Autowired
    private ZbxItem zbxItem;

    /**
     * 设备属性分页列表
     *
     * @param productAttr
     * @return
     */
    public Pager<ProductAttrDto> prodModelAttributeList(ProductAttrParam productAttr) {
        QProductAttribute qProductAttribute = new QProductAttribute();

        if (null != productAttr.getProdId()) {
            qProductAttribute.productId.eq(productAttr.getProdId());
        }
        if (ToolUtil.isNotEmpty(productAttr.getAttrName())) {
            qProductAttribute.name.contains(productAttr.getAttrName());
        }
        if (ToolUtil.isNotEmpty(productAttr.getKey())) {
            qProductAttribute.key.contains(productAttr.getKey());
        }

        List<ProductAttrDto> pagedList = qProductAttribute.setFirstRow((productAttr.getPage() - 1) * productAttr.getMaxRow()).setMaxRows(productAttr.getMaxRow()).asDto(ProductAttrDto.class).findList();
        int                  count     = qProductAttribute.findCount();
        return new Pager<>(pagedList, count);
    }

    /**
     * 设备属性列表
     *
     * @param productAttr
     * @return
     */
    public List<ProductAttribute> list(ProductAttrParam productAttr) {
        QProductAttribute qProductAttribute = new QProductAttribute();
        if (null != productAttr.getProdId()) {
            qProductAttribute.productId.eq(productAttr.getProdId());
        }
        if (ToolUtil.isNotEmpty(productAttr.getAttrName())) {
            qProductAttribute.name.contains(productAttr.getAttrName());
        }
        if (ToolUtil.isNotEmpty(productAttr.getKey())) {
            qProductAttribute.key.contains(productAttr.getKey());
        }
        return qProductAttribute.findList();
    }

    /**
     * 设备属性详情
     *
     * @param attrId
     * @return
     */
    public ProductAttrDto detail(Long attrId) {
        ProductAttrDto attr = new QProductAttribute().attrId.eq(attrId).asDto(ProductAttrDto.class).findOne();

        if (null == attr.getZbxId()) {
            return attr;
        }
        JSONArray itemInfo = JSONObject.parseArray(zbxItem.getItemInfo(attr.getZbxId(), null));
        attr.setTags(JSONObject.parseArray(itemInfo.getJSONObject(0).getString("tags"), ProductTag.Tag.class));
        attr.setProcessStepList(formatProcessStep(itemInfo.getJSONObject(0).getString("preprocessing")));
        String valuemap = itemInfo.getJSONObject(0).getString("valuemap");
        if (ToolUtil.isNotEmpty(valuemap) && !"[]".equals(valuemap)) {
            attr.setValuemapid(JSONObject.parseObject(valuemap).getString("valuemapid"));
        }
        return attr;
    }

    private List<ProductAttr.ProcessingStep> formatProcessStep(String preprocessing) {
        if (ToolUtil.isEmpty(preprocessing)) {
            return Collections.emptyList();
        }
        List<ProductAttr.ProcessingStep> processingSteps = new ArrayList<>();
        JSONArray                        jsonArray       = JSONObject.parseArray(preprocessing);
        for (Object object : jsonArray) {
            ProductAttr.ProcessingStep processingStep = new ProductAttr.ProcessingStep();
            processingStep.setType(((JSONObject) object).getString("type"));
            processingStep.setParams(((JSONObject) object).getString("params").split("\\n"));
            processingSteps.add(processingStep);
        }

        return processingSteps;
    }

    /**
     * 创建 设备物模型
     *
     * @param productAttr 产品属性DTO
     */
    public void createProductAttr(ProductAttr productAttr, String zbxId) {
        ProductAttribute productAttribute = new ProductAttribute();
        buildProdAttribute(productAttribute, productAttr);
        productAttribute.setZbxId(zbxId);
        productAttribute.save();
    }

    private ProductAttribute buildProdAttribute(ProductAttribute prodAttribute, ProductAttr productAttr) {
        prodAttribute.setProductId(productAttr.getProductId());
        prodAttribute.setName(productAttr.getAttrName());
        prodAttribute.setKey(productAttr.getKey());
        prodAttribute.setSource(productAttr.getSource());
        prodAttribute.setUnits(productAttr.getUnits());
        prodAttribute.setRemark(productAttr.getRemark());
        prodAttribute.setValueType(productAttr.getValueType());
        prodAttribute.setAttrId(productAttr.getAttrId());
        prodAttribute.setDepAttrId(productAttr.getDepAttrId());
        return prodAttribute;
    }

    /**
     * 创建 Trapper 类型 ITEM
     *
     * @param productAttr 属性
     * @return String
     */
    public String createTrapperItem(ProductAttr productAttr) {

        String itemName = productAttr.getAttrId() + "";

        Product prod = new QProduct().productId.eq(productAttr.getProductId()).findOne();
        if (null == prod) {
            throw new ServiceException(BizExceptionEnum.PRODUCT_NOT_EXISTS);
        }
        String hostId = prod.getZbxId();

        List<ZbxProcessingStep> processingSteps = new ArrayList<>();
        if (ToolUtil.isNotEmpty(productAttr.getProcessStepList())) {
            productAttr.getProcessStepList().forEach(i -> {
                ZbxProcessingStep step = new ZbxProcessingStep();

                step.setType(i.getType());
                step.setParams(i.getParams());

                processingSteps.add(step);
            });
        }
        Map<String, String> tagMap = new HashMap<>();
        if (ToolUtil.isNotEmpty(productAttr.getTags())) {
            tagMap = productAttr.getTags().stream()
                    .collect(Collectors.toMap(ProductTag.Tag::getTag, ProductTag.Tag::getValue, (k1, k2) -> k2));
        }

        return zbxItem.createTrapperItem(itemName, productAttr.getKey(),
                hostId, productAttr.getValueType(), productAttr.getUnits(), processingSteps, productAttr.getValuemapid(), tagMap);
    }

    /**
     * 修改 Trapper 类型 ITEM
     *
     * @param productAttr 属性
     * @return String
     */
    public ProductAttr updateTrapperItem(ProductAttr productAttr) {
        ProductAttribute productAttribute = new QProductAttribute().attrId.eq(productAttr.getAttrId()).findOne();
        buildProdAttribute(productAttribute, productAttr);
        Product prod = new QProduct().productId.eq(productAttr.getProductId()).findOne();
        if (null == prod) {
            throw new ServiceException(BizExceptionEnum.PRODUCT_NOT_EXISTS);
        }
        String                  hostId          = prod.getZbxId();
        List<ZbxProcessingStep> processingSteps = new ArrayList<>();
        if (ToolUtil.isNotEmpty(productAttr.getProcessStepList())) {
            productAttr.getProcessStepList().forEach(i -> {
                ZbxProcessingStep step = new ZbxProcessingStep();

                step.setType(i.getType());
                step.setParams(i.getParams());

                processingSteps.add(step);
            });
        }

        Map<String, String> tagMap = new HashMap<>();
        if (ToolUtil.isNotEmpty(productAttr.getTags())) {
            tagMap = productAttr.getTags().stream()
                    .collect(Collectors.toMap(ProductTag.Tag::getTag, ProductTag.Tag::getValue, (k1, k2) -> k2));
        }


        zbxItem.updateTrapperItem(productAttribute.getZbxId(), productAttr.getAttrId() + "", productAttr.getKey(),
                hostId, productAttr.getValueType(), productAttr.getUnits(), processingSteps, productAttr.getValuemapid(), tagMap);

        DB.update(productAttribute);

        return productAttr;
    }

    /**
     * 删称 Trapper 类型 ITEM
     *
     * @param productAttr 属性
     * @return String
     */
    public void deleteTrapperItem(ProductAttr productAttr) {

        List<String> zbxIds = new QProductAttribute().select(QProductAttribute.alias().zbxId).attrId.in(productAttr.getAttrIds()).findSingleAttributeList();
        //删除zbx item
        zbxItem.deleteTrapperItem(zbxIds);

        //删除 属性
        new QProductAttribute().attrId.in(productAttr.getAttrIds()).delete();

    }
}