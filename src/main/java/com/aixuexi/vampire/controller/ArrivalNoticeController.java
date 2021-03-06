package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.response.RpcResultData;
import com.aixuexi.thor.validate.constant.Regex;
import com.aixuexi.thor.validate.util.ValidationUtils;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.turing.model.po.Institution;
import com.gaosi.api.turing.service.InstitutionService;
import com.gaosi.api.vulcan.bean.common.ArrivalNoticeQueryCriteria;
import com.gaosi.api.vulcan.bean.common.Assert;
import com.gaosi.api.vulcan.constant.ArrivalNoticeStatus;
import com.gaosi.api.vulcan.facade.ArrivalNoticeServiceFacade;
import com.gaosi.api.vulcan.model.ArrivalNotice;
import com.gaosi.api.vulcan.vo.ArrivalNoticeVo;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 缺货通知
 */
@RestController
@RequestMapping(value = "/arrivalNotice")
public class ArrivalNoticeController {

    private final Logger logger = LoggerFactory.getLogger(ArrivalNoticeController.class);
    @Resource
    private ArrivalNoticeServiceFacade arrivalNoticeServiceFacade;
    @Resource
    private InstitutionService institutionService;

    /**
     * 插入一条到货通知
     *
     * @param arrivalNotice 到货通知
     * @return 到货通知ID
     */
    @RequestMapping(value = "/noticeAttribute", method = RequestMethod.POST)
    public ResultData insert(@RequestBody ArrivalNotice arrivalNotice) {
        ResultData resultData = new ResultData();
        checkParams4Insert(arrivalNotice);
        Integer userId = UserHandleUtil.getUserId();
        Integer insId = UserHandleUtil.getInsId();
        logger.info("User: [{}] ,insId: [{}], insert arrivalNotice: [{}] ", userId, insId, arrivalNotice);
        handleArrivalNotice4Insert(arrivalNotice, userId, insId);

        //去重
        ArrivalNoticeQueryCriteria arrivalNoticeQueryCriteria = new ArrivalNoticeQueryCriteria();
        arrivalNoticeQueryCriteria.setStatus(ArrivalNoticeStatus.NOT_NOTICE.getValue());
        arrivalNoticeQueryCriteria.setMallSkuId(arrivalNotice.getMallSkuId());
        arrivalNoticeQueryCriteria.setRegisterUserId(userId);
        ApiResponse<List<ArrivalNoticeVo>> listApiResponse = arrivalNoticeServiceFacade.queryByCondition(arrivalNoticeQueryCriteria);
        List<ArrivalNoticeVo> arrivalNoticeVos = listApiResponse.getBody();
        if (CollectionUtils.isNotEmpty(arrivalNoticeVos)) {
            return ResultData.failed("您已经登记过该商品的到货通知");
        }

        ApiResponse<Integer> apiResponse = arrivalNoticeServiceFacade.insert(arrivalNotice);
        resultData.setBody(apiResponse.getBody());
        return resultData;
    }

    private void handleArrivalNotice4Insert(ArrivalNotice arrivalNotice, Integer userId, Integer insId) {
        arrivalNotice.setRegisterUserId(userId);
        arrivalNotice.setInstitutionId(insId);
        RpcResultData<Institution> institution = institutionService.getInstitution(insId);
        Assert.notNull(institution, "查询机构为空");
        Assert.isTrue(RpcResultData.STATUS_NORMAL == institution.getStatus(), "查询机构状态有误");
        Institution institutionData = institution.getData();
        Assert.notNull(institutionData, "查询机构数据为空");
        Assert.notNull(institutionData.getManageId(), "查询机构客户经理为空" );
        arrivalNotice.setManagerId(institutionData.getManageId());
        arrivalNotice.setStatus(ArrivalNoticeStatus.NOT_NOTICE.getValue());
    }

    private void checkParams4Insert(ArrivalNotice arrivalNotice) {
        Assert.notNull(arrivalNotice, "参数不能为空");
        Assert.notNull(arrivalNotice.getMallSkuId(), "skuId不能为空");
        Assert.notNull(arrivalNotice.getNoticePhone(), "缺货电话不能为空");
        Assert.isTrue(ValidationUtils.isMatch(arrivalNotice.getNoticePhone(), Regex.MOBILE_REGEX), "手机号格式不正确");
        Assert.isTrue(null == arrivalNotice.getNum() || arrivalNotice.getNum() >= 0, "期望的数量必须大于0");
    }

}

