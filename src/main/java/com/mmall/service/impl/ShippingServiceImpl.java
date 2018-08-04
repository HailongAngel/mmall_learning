package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.mmall.common.ServiceResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @program: mmall
 * @description:
 * @author: Hailong
 * @create: 2018-08-02 15:03
 **/
@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService{
    @Autowired
    private ShippingMapper shippingMapper;
   public ServiceResponse add(Integer userId, Shipping shipping){
       shipping.setUserId(userId);
       int rowCount = shippingMapper.insert(shipping);
       if (rowCount > 0){
           Map result = Maps.newHashMap();
           result.put("shippingId",shipping.getId());
           return ServiceResponse.createBySuccess("新建地址成功");
       }
       return ServiceResponse.createBySuccess("新建地址失败");

   }

   public  ServiceResponse del(Integer userId,Integer shippingId){
       //会出现横向越权
       int resultCount = shippingMapper.deleteByShippingIdUserId(userId,shippingId);
       if (resultCount > 0){
           return ServiceResponse.createBySuccess("删除地址成功");
       }
       return ServiceResponse.createByErrorMessage("删除地址失败");
   }

    public  ServiceResponse update(Integer userId,Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShipping(shipping);
        if (rowCount > 0) {
            return ServiceResponse.createBySuccess("更新地址成功");

        }
        return ServiceResponse.createByErrorMessage("更新地址失败");
    }

    public  ServiceResponse<Shipping> select(Integer userId,Integer shippingId){
       Shipping shipping = shippingMapper.selectByShippingIdUserId(userId,shippingId);
       if (shipping == null){
           return ServiceResponse.createByErrorMessage("无法查询到改地址");
       }
       return ServiceResponse.createBySuccess("更新地址成功",shipping);

    }

    public ServiceResponse<PageInfo> list(Integer userId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippingList = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServiceResponse.createBySuccess(pageInfo);


    }


}
