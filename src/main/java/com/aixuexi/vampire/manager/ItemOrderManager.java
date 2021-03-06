package com.aixuexi.vampire.manager;

import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.UserService;
import com.gaosi.api.davincicode.model.User;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.model.ItemOrder;
import com.gaosi.api.revolver.model.ItemOrderDetail;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.revolver.vo.ItemOrderDetailVo;
import com.gaosi.api.revolver.vo.ItemOrderVo;
import com.gaosi.api.revolver.vo.MallItemSalesNumVo;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.model.MallItem;
import com.gaosi.api.vulcan.model.MallSku;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



/**
 * @Description:商品订单管理，供controller使用
 * @Author: liuxinyun
 * @Date: 2017/8/10 14:20
 */
@Service("itemOrderManager")
public class ItemOrderManager {

    private static final Logger logger = LoggerFactory.getLogger(ItemOrderManager.class);

    @Resource
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Value("${order_update_fail_receive_phone}")
    private String phoneStr;

    @Resource
    private FinancialAccountManager financialAccountManager;

    @Resource
    private UserService userService;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 虚拟商品生成订单对象
     * @param mallItem
     * @param mallSku
     * @param num
     * @return
     */
    public ItemOrderVo generateItemOrderVo(MallItem mallItem, MallSku mallSku, Integer num) {
        ItemOrderVo itemOrderVo = new ItemOrderVo();
        itemOrderVo.setInstitutionId(UserHandleUtil.getInsId());
        itemOrderVo.setUserId(UserHandleUtil.getUserId());
        itemOrderVo.setRemark(StringUtils.EMPTY);
        itemOrderVo.setExtInfo(StringUtils.EMPTY);
        itemOrderVo.setRelationInfo(StringUtils.EMPTY);
        User user = userService.getUserById(UserHandleUtil.getUserId());
        //虚拟商品没有收货人，默认收货人为当前用户,收货人电话为当前用户的电话
        itemOrderVo.setConsigneeName(user.getName());
        itemOrderVo.setConsigneePhone(user.getTelephone());
        //只要提交订单就是待支付，确认支付后再更改状态
        itemOrderVo.setStatus(OrderConstant.OrderStatus.NO_PAY.getValue());
        itemOrderVo.setCategoryId(mallItem.getCategoryId());
        //订单详情
        List<ItemOrderDetailVo> itemOrderDetailVos = new ArrayList<>();
        ItemOrderDetailVo itemOrderDetailVo = new ItemOrderDetailVo();
        itemOrderDetailVo.setItemId(mallItem.getId());
        itemOrderDetailVo.setItemName(mallItem.getName());
        itemOrderDetailVo.setMallSkuId(0);
        if (mallSku.getId() != null) {
            itemOrderDetailVo.setMallSkuId(mallSku.getId());
        }
        itemOrderDetailVo.setItemPrice(mallSku.getPrice());
        itemOrderDetailVo.setItemCount(num);
        itemOrderDetailVo.setDiscount(0D);
        if (mallSku.getOriginalPrice() != null && mallSku.getOriginalPrice() != 0) {
            itemOrderDetailVo.setDiscount(AmountUtil.subtract(mallSku.getOriginalPrice(), mallSku.getPrice()));
        }
        itemOrderDetailVo.setBusinessId(MallItemConstant.Category.getCode(mallItem.getCategoryId()));
        itemOrderDetailVos.add(itemOrderDetailVo);
        itemOrderVo.setItemOrderDetails(itemOrderDetailVos);
        return itemOrderVo;
    }

    /**
     * 虚拟商品提交订单
     * @param itemOrderVo
     * @return
     */
    public String submit(ItemOrderVo itemOrderVo) {
        // 计算订单总金额
        List<ItemOrderDetailVo> itemOrderDetailVos = itemOrderVo.getItemOrderDetails();
        Double consumeCount = 0D;
        for (ItemOrderDetailVo itemOrderDetailVo : itemOrderDetailVos) {
            consumeCount = AmountUtil.multiply(itemOrderDetailVo.getItemPrice(), itemOrderDetailVo.getItemCount());
        }
        itemOrderVo.setConsumeCount(consumeCount);
        ItemOrder itemOrder = baseMapper.map(itemOrderVo, ItemOrder.class);
        List<ItemOrderDetail> itemOrderDetails = baseMapper.mapAsList(itemOrderDetailVos, ItemOrderDetail.class);
        ApiResponse<String> apiResponse = itemOrderServiceFacade.createOrder(itemOrder, itemOrderDetails);
        return apiResponse.getBody();
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderId
     * @return
     */
    public ItemOrderVo getOrderByOrderId(String orderId) {
        ApiResponse<ItemOrderVo> itemOrderResponse = itemOrderServiceFacade.getOrderByOrderId(orderId);
        ItemOrderVo itemOrderVo = itemOrderResponse.getBody();
        return itemOrderVo;
    }

    /**
     * 查询销量
     * @param mallItemIds
     * @return
     */
    public Map<Integer, MallItemSalesNumVo> querySalesNum(List<Integer> mallItemIds){
        ApiResponse<List<MallItemSalesNumVo>> apiResponse = itemOrderServiceFacade.querySalesNumByMallItemIds(mallItemIds);
        List<MallItemSalesNumVo> mallItemSalesNumVos = apiResponse.getBody();
        return mallItemSalesNumVos.stream().collect(Collectors.toMap(MallItemSalesNumVo::getMallItemId, p -> p, (k1, k2) -> k1));
    }

}
