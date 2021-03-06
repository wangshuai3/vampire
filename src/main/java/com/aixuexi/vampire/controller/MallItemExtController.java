package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.aixuexi.vampire.manager.DictionaryManager;
import com.aixuexi.vampire.manager.FinancialAccountManager;
import com.aixuexi.vampire.manager.GoodsManager;
import com.aixuexi.vampire.manager.ItemOrderManager;
import com.aixuexi.vampire.util.BaseMapper;
import com.aixuexi.vampire.util.UserHandleUtil;
import com.gaosi.api.axxBank.model.RemainResult;
import com.gaosi.api.basicdata.model.bo.DictionaryBo;
import com.gaosi.api.basicdata.model.bo.SubjectProductBo;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.vo.MallItemSalesNumVo;
import com.gaosi.api.vulcan.bean.common.Assert;
import com.gaosi.api.vulcan.bean.common.QueryCriteria;
import com.gaosi.api.vulcan.constant.GoodsTypePriceConstant;
import com.gaosi.api.vulcan.constant.MallItemConstant;
import com.gaosi.api.vulcan.facade.MallItemExtServiceFacade;
import com.gaosi.api.vulcan.model.TalentFilterCondition;
import com.gaosi.api.vulcan.util.MallCategoryUtil;
import com.gaosi.api.vulcan.vo.*;
import com.gaosi.api.workorder.facade.TemplateServiceFacade;
import com.gaosi.api.workorder.vo.TemplateFieldVo;
import com.gaosi.api.xmen.constant.DictConstants;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.aixuexi.vampire.util.Constants.RCZX_TEMPLATE_CODE;

/**
 * Created by ruanyanjie on 2017/7/28.
 */
@RestController
@RequestMapping(value = "/item")
public class MallItemExtController {

    private final Logger logger = LoggerFactory.getLogger(MallItemExtController.class);

    @Resource
    private MallItemExtServiceFacade mallItemExtServiceFacade;

    @Resource
    private FinancialAccountManager financialAccountManager;

    @Resource
    private DictionaryManager dictionaryManager;

    @Resource
    private TemplateServiceFacade templateServiceFacade;

    @Resource
    private GoodsManager goodsManager;

    @Resource
    private ItemOrderManager itemOrderManager;

    @Resource
    private BaseMapper baseMapper;

    /**
     * 校长培训列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/nail", method = RequestMethod.GET)
    public ResultData queryMallItemNailList(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        QueryCriteria queryCriteria = new QueryCriteria();
        queryCriteria.setPageNum(pageNum);
        queryCriteria.setPageSize(pageSize);
        queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
        ApiResponse<Page<MallItemNailVo>> mallItemNailVoResponse = mallItemExtServiceFacade.queryMallItemNailList(queryCriteria);
        Page<MallItemNailVo> mallItemNailVoPage = mallItemNailVoResponse.getBody();
        dealMallItemNailVo(mallItemNailVoPage.getList());
        return ResultData.successed(mallItemNailVoPage);
    }

    /**
     * 校长培训详情
     * @param mallItemId
     * @return
     */
    @RequestMapping(value = "/nail/detail", method = RequestMethod.GET)
    public ResultData queryMallItemNailDetail(@RequestParam Integer mallItemId) {
        ApiResponse<MallItemNailVo> mallItemNailVoResponse = mallItemExtServiceFacade.queryMallItemNailDetail(mallItemId, MallItemConstant.ShelvesStatus.ON);
        MallItemNailVo mallItemNailVo = mallItemNailVoResponse.getBody();
        dealMallItemNailVo(Lists.newArrayList(mallItemNailVo));
        return ResultData.successed(mallItemNailVo);
    }

    /**
     * 校长培训确认订单
     * @param mallItemId
     * @param goodsPieces
     * @return
     */
    @RequestMapping(value = "/nail/confirm", method = RequestMethod.GET)
    public ResultData confirmMallItemNail(@RequestParam Integer mallItemId, @RequestParam Integer goodsPieces) {
        logger.info("confirmMallItemNail userId :{} mallItemId :{} num :{}",
                UserHandleUtil.getUserId(), mallItemId,  goodsPieces);
        Assert.isTrue(goodsPieces >= 1,"商品数量错误");
        ResultData resultData = new ResultData();
        ApiResponse<ConfirmMallItemNailVo> apiResponse = mallItemExtServiceFacade.confirmMallItemNail(mallItemId, goodsPieces);
        ConfirmMallItemNailVo confirmMallItemNailVo = apiResponse.getBody();
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        confirmMallItemNailVo.setAidouUsableRemain(Double.valueOf(rr.getAidouUsableRemain()) / 10000);
        confirmMallItemNailVo.setRmbUsableRemain(Double.valueOf(rr.getRmbUsableRemain()) / 10000);
        resultData.setBody(confirmMallItemNailVo);
        return resultData;
    }

    /**
     * 定制服务列表
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/customService", method = RequestMethod.GET)
    public ResultData queryCustomServiceList(@RequestParam Integer pageNum, @RequestParam Integer pageSize) {
        QueryCriteria queryCriteria = new QueryCriteria();
        queryCriteria.setPageNum(pageNum);
        queryCriteria.setPageSize(pageSize);
        queryCriteria.setGoodsStatus(MallItemConstant.ShelvesStatus.ON);
        List<Integer> categoryIds = MallCategoryUtil.queryAllIdsBySameTopLevel(MallItemConstant.Category.DZFW.getId());
        queryCriteria.setCategoryIds(categoryIds);
        ApiResponse<Page<MallItemCustomServiceVo>> apiResponse = mallItemExtServiceFacade.queryMallItemList4DZFW(queryCriteria, UserHandleUtil.getInsId());
        return ResultData.successed(apiResponse.getBody());
    }

    /**
     * 定制服务确认订单
     * @param mallItemId
     * @param goodsPieces
     * @return
     */
    @RequestMapping(value = "/customService/confirm", method = RequestMethod.GET)
    public ResultData confirmCustomService(@RequestParam Integer mallItemId,@RequestParam Integer goodsPieces ){
        logger.info("confirmCustomService userId :{} mallItemId :{} num :{}",
                UserHandleUtil.getUserId(), mallItemId,  goodsPieces);
        Assert.isTrue(goodsPieces >= 1,"商品数量错误");
        ResultData resultData = new ResultData();
        ApiResponse<ConfirmCustomServiceVo> apiResponse = mallItemExtServiceFacade.confirmMallItem4DZFW(mallItemId, goodsPieces);
        ConfirmCustomServiceVo confirmCustomServiceVo = apiResponse.getBody();
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        confirmCustomServiceVo.setAidouUsableRemain(Double.valueOf(rr.getAidouUsableRemain()) / 10000);
        confirmCustomServiceVo.setRmbUsableRemain(Double.valueOf(rr.getRmbUsableRemain()) / 10000);
        resultData.setBody(confirmCustomServiceVo);
        return resultData;
    }

    /**
     * 人才中心筛选条件
     * @return
     */
    @RequestMapping(value = "/talentCenter/queryCondition", method = RequestMethod.GET)
    public ResultData queryCondition4TalentCenter(){
        // 全部筛选条件
        List<CommonConditionVo> allCondition = new ArrayList<>();
        // 获取人才中心的筛选条件
        ApiResponse<TalentFilterCondition> talentResponse = mallItemExtServiceFacade.queryTalentFilterCondition();
        TalentFilterCondition talentFilterCondition = talentResponse.getBody();
        List<String> typeCodes = talentFilterCondition.getTypeCode();
        // 查询字典表中的人才类型
        List<DictionaryBo> dictionaryBos = dictionaryManager.selectDictByType(DictConstants.TALENT_TYPE);
        List<CommonConditionVo> typeCodeCondition = new ArrayList<>();
        for (DictionaryBo dictionaryBo : dictionaryBos) {
            if(typeCodes.contains(dictionaryBo.getCode())){
                typeCodeCondition.add(new CommonConditionVo(dictionaryBo.getId(),dictionaryBo.getCode(),dictionaryBo.getName()));
            }
        }
        typeCodeCondition.add(0, goodsManager.addAllCondition());
        allCondition.add(new CommonConditionVo(0,"人才类型",typeCodeCondition));
        // 查询基础数据的学科
        List<Integer> subjectProductIds = talentFilterCondition.getSubjectProductId();
        // 是否包含其他学科
        Boolean containOther = false;
        // 特殊处理其他学科
        Iterator<Integer> iterator = subjectProductIds.iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == 0) {
                iterator.remove();
                containOther = true;
            }
        }
        List<CommonConditionVo> subjectProductCondition = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(subjectProductIds)) {
            List<SubjectProductBo> subjectProductList = goodsManager.querySubjectProduct(subjectProductIds);
            subjectProductCondition = baseMapper.mapAsList(subjectProductList, CommonConditionVo.class);
        }
        if (containOther) {
            subjectProductCondition.add(new CommonConditionVo(0, StringUtils.EMPTY, "其他"));
        }
        subjectProductCondition.add(0, new CommonConditionVo(null, StringUtils.EMPTY, "全部"));
        allCondition.add(new CommonConditionVo(1,"所属学科",subjectProductCondition));
        return ResultData.successed(allCondition);
    }

    /**
     * 人才中心列表
     * @return
     */
    @RequestMapping(value = "/talentCenter/list", method = RequestMethod.GET)
    public ResultData queryTalentCenterList(ReqTalentCenterConditionVo reqTalentCenterConditionVo){
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        reqTalentCenterConditionVo.setManager(false);
        reqTalentCenterConditionVo.setSkuShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        ApiResponse<Page<MallItemTalentVo>> apiResponse = mallItemExtServiceFacade.queryMallItemList4Talent(reqTalentCenterConditionVo);
        Page<MallItemTalentVo> mallItemTalentVoPage = apiResponse.getBody();
        List<MallItemTalentVo> mallItemTalentVos = mallItemTalentVoPage.getList();
        dealMallItemTalentVo(mallItemTalentVos);
        return ResultData.successed(mallItemTalentVoPage);
    }

    /**
     * 人才中心详情
     * @param mallItemId
     * @return
     */
    @RequestMapping(value = "/talentCenter/detail", method = RequestMethod.GET)
    public ResultData queryTalentCenterDetail(@RequestParam Integer mallItemId) {
        ReqTalentCenterConditionVo reqTalentCenterConditionVo = new ReqTalentCenterConditionVo();
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setMallItemId(mallItemId);
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        reqTalentCenterConditionVo.setSkuShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        ApiResponse<MallItemTalentVo> apiResponse = mallItemExtServiceFacade.queryMallItem4Talent(reqTalentCenterConditionVo);
        MallItemTalentVo mallItemTalentVo = apiResponse.getBody();
        dealMallItemTalentVo(Lists.newArrayList(mallItemTalentVo));
        return ResultData.successed(mallItemTalentVo);
    }

    /**
     * 人才中心订单确认
     * @param mallItemId
     * @param mallSkuId
     * @param num
     * @return
     */
    @RequestMapping(value = "/talentCenter/confirm", method = RequestMethod.GET)
    public ResultData confirmTalentCenter(@RequestParam Integer mallItemId,@RequestParam Integer mallSkuId,
                                          @RequestParam Integer num) {
        logger.info("confirmTalentCenter userId :{} mallItemId :{} mallSkuId :{} num :{}",
                UserHandleUtil.getUserId(), mallItemId, mallSkuId, num);
        Assert.isTrue(num >= 1,"商品数量错误");
        ReqTalentCenterConditionVo reqTalentCenterConditionVo = new ReqTalentCenterConditionVo();
        reqTalentCenterConditionVo.setInstitutionId(UserHandleUtil.getInsId());
        reqTalentCenterConditionVo.setShelvesStatus(MallItemConstant.ShelvesStatus.ON);
        reqTalentCenterConditionVo.setMallItemId(mallItemId);
        reqTalentCenterConditionVo.setPriceChannel(GoodsTypePriceConstant.PriceChannel.WEB.getValue());
        ApiResponse<ConfirmTalentVo> apiResponse = mallItemExtServiceFacade.confirmTalentCenter(reqTalentCenterConditionVo, mallSkuId, num);
        ConfirmTalentVo confirmTalentVo = apiResponse.getBody();
        // 查询账户余额
        RemainResult rr = financialAccountManager.getAccountInfoByInsId(UserHandleUtil.getInsId());
        confirmTalentVo.setAidouUsableRemain(Double.valueOf(rr.getAidouUsableRemain()) / 10000);
        confirmTalentVo.setRmbUsableRemain(Double.valueOf(rr.getRmbUsableRemain()) / 10000);
        // 查询工单模板
        com.aixuexi.thor.response.ApiResponse<List<TemplateFieldVo>> templateResponse = templateServiceFacade.queryTemplateFields(RCZX_TEMPLATE_CODE);
        List<TemplateFieldVo> templateFieldVos = templateResponse.getBody();
        confirmTalentVo.setTalentTemplate(templateFieldVos);
        return ResultData.successed(confirmTalentVo);
    }

    /**
     * 处理人才中心VO（补充销售量）
     *
     * @param mallItemTalentVos
     */
    private void dealMallItemTalentVo(List<MallItemTalentVo> mallItemTalentVos) {
        if(CollectionUtils.isNotEmpty(mallItemTalentVos)) {
            List<Integer> mallItemIds = mallItemTalentVos.stream().map(MallItemTalentVo::getMallItemId).collect(Collectors.toList());
            Map<Integer, MallItemSalesNumVo> salesNumVoMap = itemOrderManager.querySalesNum(mallItemIds);
            for (MallItemTalentVo mallItemTalentVo : mallItemTalentVos) {
                mallItemTalentVo.setSalesNum(salesNumVoMap.get(mallItemTalentVo.getMallItemId()).getNum());
            }
        }
    }

    /**
     * 处理校长培训VO（补充销售量,报名状态）
     *
     * @param mallItemNailVos
     */
    private void dealMallItemNailVo(List<MallItemNailVo> mallItemNailVos) {
        if (CollectionUtils.isNotEmpty(mallItemNailVos)) {
            List<Integer> mallItemIds = mallItemNailVos.stream().map(MallItemNailVo::getMallItemId).collect(Collectors.toList());
            Map<Integer, MallItemSalesNumVo> salesNumVoMap = itemOrderManager.querySalesNum(mallItemIds);
            for (MallItemNailVo mno : mallItemNailVos) {
                Integer mallItemId = mno.getMallItemId();
                Integer salesNum = salesNumVoMap.get(mallItemId).getNum();
                mno.setSalesNum(salesNum);
                //添加名额已满的状态
                if (mno.getSignUpNum() > 0 && salesNum >= mno.getSignUpNum()) {
                    mno.setSignUpStatus(MallItemConstant.SignUpStatus.NUM_FULL);
                }
            }
        }
    }
}
