package com.aixuexi.vampire.manager;

import com.aixuexi.thor.except.ExceptionCode;
import com.aixuexi.thor.sms_mail.SMSConstant;
import com.aixuexi.thor.util.Functions;
import com.aixuexi.transformers.mq.ONSMQProducer;
import com.aixuexi.transformers.msg.SmsSend;
import com.gaosi.api.vulcan.bean.common.BusinessException;
import com.aixuexi.vampire.util.ApiResponseCheck;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.BusinessResult;
import com.gaosi.api.axxBank.model.CostProxyParams;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.axxBank.service.ChangeCostProxyHandler;
import com.gaosi.api.axxBank.service.FinancialAccountService;
import com.gaosi.api.common.constants.ApiRetCode;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.revolver.constant.OrderConstant;
import com.gaosi.api.revolver.constant.PayTypeConstant;
import com.gaosi.api.revolver.facade.ItemOrderServiceFacade;
import com.gaosi.api.revolver.model.ItemOrder;
import com.gaosi.api.revolver.model.ItemOrderDetail;
import com.gaosi.api.revolver.util.AmountUtil;
import com.gaosi.api.revolver.vo.ItemOrderVo;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.model.MallItem;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @Description:商品订单管理，供controller使用
 * @Author: liuxinyun
 * @Date: 2017/8/10 14:20
 */
@Service("itemOrderManager")
public class ItemOrderManager {

    private static final Logger logger = LoggerFactory.getLogger(ItemOrderManager.class);

    @Resource
    private FinancialAccountService financialAccountService;

    @Autowired
    private ItemOrderServiceFacade itemOrderServiceFacade;

    @Autowired(required = false)
    private ONSMQProducer mqProducer;

    @Value("${order_update_fail_receive_phone}")
    private String phoneStr;

    @Resource
    private FinancialAccountManager financialAccountManager;

    /**
     * 订单提交
     *
     * @param mallItem 商品
     * @param itemCount   商品数量
     * @return
     */
    public String submit(MallItem mallItem, Integer itemCount, Integer insId ,Double price,Double originalPrice) {
        //查询当前机构账号余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(insId);
        Double consumeCount = price * itemCount;
        Double totalCount = consumeCount * 10000;//根据商品现价和数量计算花费
        financialAccountManager.checkRemainMoney(rr,totalCount.longValue());

        ItemOrder itemOrder = new ItemOrder();
        itemOrder.setInstitutionId(insId);
        itemOrder.setUserId(UserHandleUtil.getUserId());
        itemOrder.setConsigneeName(UserSessionHandler.getUsername());//虚拟商品没有收货人，默认收货人为当前用户
        itemOrder.setStatus(OrderConstant.OrderStatus.NO_PAY.getValue());//只要提交订单就是待支付，确认支付后再更改状态
        itemOrder.setCategoryId(mallItem.getCategoryId());
        itemOrder.setConsumeCount(consumeCount);

        //订单详情拼接
        List<ItemOrderDetail> itemOrderDetails = Lists.newArrayList();
        ItemOrderDetail itemOrderDetail = new ItemOrderDetail();
        itemOrderDetail.setItemId(mallItem.getId());
        itemOrderDetail.setItemName(mallItem.getName());
        itemOrderDetail.setItemPrice(price);
        itemOrderDetail.setItemCount(itemCount);
        itemOrderDetail.setDiscount(originalPrice - price);
        itemOrderDetail.setBusinessId(MallItemConstant.Category.getCode(mallItem.getCategoryId()));
        itemOrderDetails.add(itemOrderDetail);

        ApiResponse<String> apiResponse = itemOrderServiceFacade.createOrder(itemOrder, itemOrderDetails);
        if (apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "创建订单失败");
        }
        return apiResponse.getBody();
    }

    /**
     * 订单支付
     *
     * @param orderId
     * @return
     */
    public void pay(String orderId, String token) {
        //查询当前机构账号余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        ItemOrder itemOrder = getOrderByOrderId(orderId);
        if (itemOrder.getStatus() == OrderConstant.OrderStatus.CANCELLED.getValue()) {// 防止用户在确认支付页面停留时间超过规定支付时间，订单已取消仍可支付的情况出现
            throw new BusinessException(ExceptionCode.UNKNOWN, "支付超时，该订单已自动取消");
        }
        Double amount = AmountUtil.multiply(itemOrder.getConsumeCount(), 10000);//扩大10000倍
        if (amount.longValue() > rr.getUsableRemain()) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "余额不足");
        }
        String optionDesc = "订单号[order]" + orderId + "[/order]";
        ChangeCostProxyHandler proxyHandler = new ChangeCostProxyHandler(financialAccountService);
        CostProxyParams proxyParams = new CostProxyParams();
        proxyParams.setInsId(UserHandleUtil.getInsId());
        proxyParams.setAmount(amount.longValue());
        proxyParams.setDiscount(100);
        proxyParams.setOperatorId(UserHandleUtil.getUserId());
        proxyParams.setOperatorType(1);
        proxyParams.setOptionItemEnum(PayTypeConstant.PayType.getOptionItemEnum(itemOrder.getCategoryId()));
        proxyParams.setToken(token);
        proxyParams.setOptionDesc(optionDesc);
        BusinessResult businessResult = proxyHandler.costAidou(proxyParams, this.financialOperation(orderId));
        if (businessResult.getCgFinancialResult().getStatus() == 1) {
            updateOrderStatus(orderId);
            logger.info("订单扣费成功，optionDesc：{},amount:{},token:{}", optionDesc, amount, token);
        } else {
            throw new BusinessException(ExceptionCode.UNKNOWN, "订单扣费失败，错误信息：" +
                    businessResult.getCgFinancialResult().getMessage());
        }

    }

    /**
     * 财务扣款操作
     *
     * @param orderId
     * @return
     */
    private Functions.Function0<BusinessResult> financialOperation(final String orderId) {
        return new Functions.Function0<BusinessResult>() {
            @Override
            public BusinessResult apply() {
                return new BusinessResult(orderId, null);
            }
        };
    }

    /**
     * 付款后更新订单状态
     *
     * @param orderId
     */
    private void updateOrderStatus(String orderId) {
        boolean flag = true;
        try {
            int retryNum = 0;
            while (retryNum < 3) {//重试三次
                ApiResponse<?> apiResponse = itemOrderServiceFacade.updateOrderStatus(orderId, OrderConstant.OrderStatus.COMPLETED.getValue());
                if (apiResponse == null || apiResponse.getRetCode() != ApiRetCode.SUCCESS_CODE) {
                    //更新状态失败，重试次数累加。
                    retryNum++;
                    //等待100毫秒后重试
                    Thread.sleep(100);
                } else {
                    flag = false;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("orderId=[{}] pay success, but updateStatus failed for {}.", orderId, e);
        } finally {
            //重试三次更新状态均失败，打印错误日志。(发短信通知)
            if (flag) {
                logger.error("orderId=[{}] pay success, but updateStatus failed.", orderId);
                String[] phones = phoneStr.split(",");
                SmsSend.SmsSendObject.Builder builder = SmsSend.SmsSendObject.newBuilder();
                builder.setSignName(SMSConstant.SIGN_AIXUEXI)
                        .putParam("orderId", orderId)
                        .setTemplateCode(SMSConstant.TEMPLATE_CODE_ORDER_UPDATE_FAIL_NOTIFY)
                        .addAllPhones(Arrays.asList(phones))
                        .setBusinessType(SMSConstant.BUSINESS_TYPE_ORDER_UPDATE_FAIL_NOTIFY);
                mqProducer.send(builder);
            }
        }
    }

    /**
     * 根据订单号查询订单
     *
     * @param orderId
     * @return
     */
    public ItemOrder getOrderByOrderId(String orderId) {
        ApiResponse<ItemOrderVo> itemOrderResponse = itemOrderServiceFacade.getOrderByOrderId(orderId);
        ApiResponseCheck.check(itemOrderResponse);
        ItemOrderVo itemOrderVo = itemOrderResponse.getBody();
        if (itemOrderVo == null) {
            throw new BusinessException(ExceptionCode.UNKNOWN, "订单:" + orderId + "不存在");
        }
        return itemOrderVo;
    }

}
