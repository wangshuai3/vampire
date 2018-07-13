package com.aixuexi.vampire.manager;

import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.common.util.CollectionUtils;
import com.gaosi.api.davinciNew.service.UserService;
import com.gaosi.api.davincicode.common.service.UserSessionHandler;
import com.gaosi.api.davincicode.model.UserType;
import com.gaosi.api.davincicode.model.bo.UserBo;
import com.gaosi.api.dragonball.model.bo.ApprovalAuthorityBo;
import com.gaosi.api.dragonball.service.WorkFlowApplyService;
import com.gaosi.api.revolver.constant.WorkOrderConstant;
import com.gaosi.api.revolver.util.WorkOrderUtil;
import com.gaosi.api.revolver.vo.WorkOrderDealRecordVo;
import com.gaosi.api.revolver.vo.WorkOrderRefundDetailVo;
import com.gaosi.api.revolver.vo.WorkOrderRefundVo;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.facade.MallItemServiceFacade;
import com.gaosi.api.vulcan.model.GoodsType;
import com.gaosi.api.vulcan.model.MallItemPic;
import com.gaosi.api.vulcan.util.CollectionCommonUtil;
import com.gaosi.api.vulcan.vo.MallItemVo;
import com.gaosi.api.vulcan.vo.MallSkuVo;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author liuxinyun
 * @date 2018/1/2 15:28
 * @description
 */
@Service("workOrderManager")
public class WorkOrderManager {

    @Resource
    private InstitutionService institutionService;
    @Resource
    private UserService newUserService;
    @Resource
    private WorkFlowApplyService workFlowApplyService;
    @Resource
    private MallItemServiceFacade mallItemServiceFacade;

    /**
     * 处理单个退货工单--查看详情使用
     * @param workOrderRefundVo
     */
    public void dealWorkOrderRefundVo(WorkOrderRefundVo workOrderRefundVo){
        List<WorkOrderRefundDetailVo> workOrderRefundDetailVos = workOrderRefundVo.getWorkOrderRefundDetailVos();
        WorkOrderRefundDetailVo workOrderRefundDetailVo = workOrderRefundDetailVos.get(0);
        // 机构信息
        Integer institutionId = workOrderRefundVo.getInstitutionId();
        Institution institution = institutionService.getInsInfoById(institutionId);
        workOrderRefundDetailVo.setInsName(institution.getName());
        // 补充商品相关的信息
        dealWorkOrderRefundDetailVo(workOrderRefundDetailVos);
    }

    /**
     * 补充协商历史信息
     * @param workOrderDealRecordVos
     */
    private void dealWorkOrderDealRecordVo(List<WorkOrderDealRecordVo> workOrderDealRecordVos) {
        // 批量查询用户信息
        Set<Integer> userIds = CollectionCommonUtil.getFieldSetByObjectList(workOrderDealRecordVos, "getUserId", Integer.class);
        Map<Integer, UserBo> userBoMap = queryUserInfo(userIds);
        for (WorkOrderDealRecordVo workOrderDealRecordVo : workOrderDealRecordVos) {
            UserBo user = userBoMap.get(workOrderDealRecordVo.getUserId());
            if (UserType.MANAGE.getValue().equals(user.getUserType())){
                workOrderDealRecordVo.setUserName(WorkOrderConstant.DEAL_RECORD_MANAGER_NAME);
                workOrderDealRecordVo.setUserPic(WorkOrderConstant.DEAL_RECORD_MANAGER_PIC);
            }else {
                workOrderDealRecordVo.setUserName(user.getName());
                workOrderDealRecordVo.setUserPic(user.getPortraitPath());
            }
        }
    }

    /**
     * 处理退货工单详情（补充商品相关的信息）
     * @param workOrderRefundDetailVos
     */
    public void dealWorkOrderRefundDetailVo(List<WorkOrderRefundDetailVo> workOrderRefundDetailVos){
        List<Integer> mallItemIds = CollectionCommonUtil.getFieldListByObjectList(workOrderRefundDetailVos, "getMallItemId", Integer.class);
        ApiResponse<List<MallItemVo>> mallItemVoResponse = mallItemServiceFacade.findMallItemVoByIds(mallItemIds);
        List<MallItemVo> mallItemVos = mallItemVoResponse.getBody();
        Map<Integer, MallItemVo> mallItemVoMap = CollectionCommonUtil.toMapByList(mallItemVos, "getId", Integer.class);
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundDetailVos) {
            Integer mallItemId = workOrderRefundDetailVo.getMallItemId();
            MallItemVo mallItemVo = mallItemVoMap.get(mallItemId);
            // 商品图片
            List<MallItemPic> mallItemPics = mallItemVo.getMallItemPics();
            if(CollectionUtils.isNotEmpty(mallItemPics)) {
                workOrderRefundDetailVo.setPicUrl(mallItemPics.get(0).getPicUrl());
            }
            // 商品规格名称,编码
            List<MallSkuVo> mallSkuVos = mallItemVo.getMallSkuVos();
            Integer mallSkuId = workOrderRefundDetailVo.getMallSkuId();
            for (MallSkuVo mallSkuVo : mallSkuVos) {
                if(mallSkuVo.getId().equals(mallSkuId)){
                    workOrderRefundDetailVo.setSkuName(mallSkuVo.getName());
                    workOrderRefundDetailVo.setSkuCode(mallSkuVo.getCode());
                }
            }
        }
    }

    /**
     * 批量查询用户的审批权限
     * @param workOrderRefundVos
     * @return
     */
    private Map<Integer, ApprovalAuthorityBo> workFlowCheckAuthority(List<WorkOrderRefundVo> workOrderRefundVos){
        List<Integer> approveIds = CollectionCommonUtil.getFieldListByObjectList(workOrderRefundVos,
                "getApproveId", Integer.class);
        ApiResponse<List<ApprovalAuthorityBo>> authorityResponse = workFlowApplyService.checkUserAuthority(
                approveIds, UserSessionHandler.getId());
        List<ApprovalAuthorityBo> authoritys = authorityResponse.getBody();
        return CollectionCommonUtil.toMapByList(authoritys, "getWrId", Integer.class);
    }

    /**
     * 批量查询用户信息
     * @param userIds
     * @return
     */
    private Map<Integer, UserBo> queryUserInfo(Set<Integer> userIds){
        ApiResponse<List<UserBo>> operatorResponse = newUserService.findByIdsWithoutRolename(Lists.newArrayList(userIds));
        List<UserBo> userBoList = operatorResponse.getBody();
        return CollectionCommonUtil.toMapByList(userBoList, "getId", Integer.class);
    }

    /**
     *  填充重量，商品ID
     * @param workOrderRefundVo
     */
    public void dealWorkOrderRefundVo(WorkOrderRefundVo workOrderRefundVo, Map<Integer, GoodsType> goodsTypeMap) {
        for (WorkOrderRefundDetailVo workOrderRefundDetailVo : workOrderRefundVo.getWorkOrderRefundDetailVos()) {
            Integer mallSkuId = workOrderRefundDetailVo.getMallSkuId();
            if (goodsTypeMap != null && goodsTypeMap.containsKey(mallSkuId)) {
                GoodsType goodsType = goodsTypeMap.get(mallSkuId);
                workOrderRefundDetailVo.setWeight(goodsType.getWeight());
            }
        }
    }
}
