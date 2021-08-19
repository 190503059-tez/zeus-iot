package com.zmops.iot.web.device.service.work;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zmops.iot.async.callback.IWorker;
import com.zmops.iot.async.wrapper.WorkerWrapper;
import com.zmops.iot.domain.device.Device;
import com.zmops.iot.domain.product.ProductAttribute;
import com.zmops.iot.domain.product.query.QProductAttribute;
import com.zmops.iot.util.ToolUtil;
import com.zmops.iot.web.device.dto.DeviceDto;
import com.zmops.zeus.driver.entity.ZbxItemInfo;
import com.zmops.zeus.driver.service.ZbxItem;
import io.ebean.DB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yefei
 *
 * 更新设备属性中ZBXID
 */
@Slf4j
@Component
public class UpdateAttrZbxIdWorker implements IWorker<DeviceDto, Boolean> {

    @Autowired
    ZbxItem zbxItem;

    @Override
    public Boolean action(DeviceDto deviceDto, Map<String, WorkerWrapper<?, ?>> map) {
        log.debug("处理Attr zbx回填工作……");
        Long deviceId = deviceDto.getDeviceId();
        //创建
        if (null == deviceId) {
            Device device = (Device) map.get("saveDvice").getWorkResult().getResult();
            deviceId = device.getDeviceId();
        } else {
            //修改

            //没有修改关联的产品 不做处理
            if (deviceDto.getProductId().equals(deviceDto.getOldProductId())) {
                return true;
            }
        }

        //取出 ZBX hostid
        Object    result = map.get("saveZbxHostWork").getWorkResult().getResult();
        JSONArray hostid = JSONObject.parseObject(result.toString()).getJSONArray("hostids");

        //根据hostid 取出监控项
        List<ZbxItemInfo> itemInfos = JSONObject.parseArray(zbxItem.getItemInfo(null, hostid.get(0).toString()), ZbxItemInfo.class);
        if (ToolUtil.isEmpty(itemInfos)) {
            return true;
        }

        Map<String, ZbxItemInfo> itemMap = itemInfos.parallelStream().collect(Collectors.toMap(ZbxItemInfo::getName, o -> o));
        //取出继承的属性 并塞入对应的 itemId
        List<ProductAttribute> productAttributeList = new QProductAttribute().productId.eq(deviceId).findList();
        for (ProductAttribute productAttribute : productAttributeList) {
            productAttribute.setZbxId(itemMap.get(productAttribute.getTemplateId() + "").getItemid());
        }

        DB.updateAll(productAttributeList);
        return true;
    }

    @Override
    public Boolean defaultValue() {
        return true;
    }

}