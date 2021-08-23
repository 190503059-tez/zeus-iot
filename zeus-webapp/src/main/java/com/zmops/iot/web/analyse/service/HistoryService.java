package com.zmops.iot.web.analyse.service;

import com.alibaba.fastjson.JSONObject;
import com.zmops.iot.domain.device.Device;
import com.zmops.iot.domain.device.query.QDevice;
import com.zmops.iot.domain.product.ProductAttribute;
import com.zmops.iot.domain.product.query.QProductAttribute;
import com.zmops.iot.util.LocalDateTimeUtils;
import com.zmops.iot.util.ToolUtil;
import com.zmops.iot.web.analyse.dto.LatestDto;
import com.zmops.iot.web.analyse.dto.param.HistoryParam;
import com.zmops.zeus.driver.service.ZbxHistoryGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yefei
 * <p>
 * 历史数据服务
 **/
@Service
public class HistoryService {

    @Autowired
    ZbxHistoryGet zbxHistoryGet;

    public List<LatestDto> qeuryHistory(HistoryParam historyParam) {
        return qeuryHistory(historyParam.getDeviceId(), historyParam.getAttrIds(), historyParam.getTimeFrom(), historyParam.getTimeTill());
    }

    public List<LatestDto> qeuryHistory(Long deviceId, List<Long> attrIds, Long timeFrom, Long timeTill) {
        //查询出设备
        Device one = new QDevice().deviceId.eq(deviceId).findOne();
        if (null == one || ToolUtil.isEmpty(one.getZbxId())) {
            return Collections.emptyList();
        }
        //查询设备属性
        QProductAttribute query = new QProductAttribute().productId.eq(deviceId);
        if (ToolUtil.isNotEmpty(attrIds)) {
            query.attrId.in(attrIds);
        }
        List<ProductAttribute> list = query.findList();
        if (ToolUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        //取出属性对应的ItemID
        List<String>                        zbxIds       = list.parallelStream().map(ProductAttribute::getZbxId).collect(Collectors.toList());
        Map<String, List<ProductAttribute>> valueTypeMap = list.parallelStream().collect(Collectors.groupingBy(ProductAttribute::getValueType));
        Map<String, String>                 itemIdMap    = list.parallelStream().collect(Collectors.toMap(ProductAttribute::getZbxId, ProductAttribute::getName));
        List<LatestDto>                     latestDtos   = new ArrayList<>();
        if (null == timeFrom) {
            timeFrom = LocalDateTimeUtils.getSecondsByTime(LocalDateTimeUtils.getDayStart(LocalDateTime.now()));
        }
        //根据属性值类型 分组查询历史数据
        for (Map.Entry<String, List<ProductAttribute>> map : valueTypeMap.entrySet()) {
            String res = zbxHistoryGet.historyGet(one.getZbxId(), zbxIds, 1000, Integer.parseInt(map.getKey()), timeFrom, timeTill);
            latestDtos.addAll(JSONObject.parseArray(res, LatestDto.class));
        }

        latestDtos.forEach(latestDto -> {
            if (null != itemIdMap.get(latestDto.getItemid())) {
                latestDto.setName(itemIdMap.get(latestDto.getItemid()));
            }
        });

        return latestDtos;
    }
}