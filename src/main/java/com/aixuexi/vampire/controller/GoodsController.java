package com.aixuexi.vampire.controller;

import com.aixuexi.thor.response.ResultData;
import com.aixuexi.thor.util.Page;
import com.gaosi.api.basicdata.*;
import com.gaosi.api.basicdata.model.bo.*;
import com.gaosi.api.common.to.ApiResponse;
import com.gaosi.api.revolver.constant.GoodsConstant;
import com.gaosi.api.revolver.facade.GoodsServiceFacade;
import com.gaosi.api.revolver.vo.CommonConditionVo;
import com.gaosi.api.revolver.vo.GoodsVo;
import com.gaosi.api.revolver.vo.RelationGoodsVo;
import com.gaosi.api.revolver.vo.RequestGoodsConditionVo;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaowenlei on 17/5/22.
 */
@RestController
@RequestMapping(value = "/goods")
public class GoodsController {

    @Resource
    private GoodsServiceFacade goodsServiceFacade;

    @Resource
    private SubjectProductApi subjectProductApi;

    @Resource
    private ExamAreaApi examAreaApi;

    @Resource
    private BookVersionApi bookVersionApi;

    @Resource
    private DictionaryApi dictionaryApi;

    @Resource
    private SchemeApi schemeApi;

    /**
     * 获取学科列表
     *
     * @return
     */
    @RequestMapping(value = "/getSubject")
    public ResultData getSubject(){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsServiceFacade.querySubject();

        List<CommonConditionVo> conditionVos = new ArrayList<>();
        //调用获取名字接口
        List<SubjectProductBo> productBos = subjectProductApi.findSubjectProductList(response.getBody());
        for (SubjectProductBo productBo: productBos) {
            CommonConditionVo conditionVo = new CommonConditionVo();
            //goods表存储的是subject_product表里面的id
            conditionVo.setId(productBo.getId());
            conditionVo.setName(productBo.getName());
            conditionVos.add(conditionVo);
        }
        conditionVos.add(getCommonConditionVo());
        resultData.setBody(conditionVos);
        return resultData;
    }

    /**
     * 获取学期列表
     *
     * @param subjectId
     * @return
     */
    @RequestMapping(value = "/getPeriod")
    public ResultData getPeriod(@RequestParam(required = false) Integer subjectId){
        ResultData resultData = new ResultData();
        ApiResponse<List<Integer>> response = goodsServiceFacade.queryPeriod(subjectId);
        List<CommonConditionVo> conditionVos = new ArrayList<>();
        //需要调用获取名字接口
        ApiResponse<List<DictionaryBo>> periods = dictionaryApi.findGoodsPeriodByCode(response.getBody());
        for (DictionaryBo dictionaryBo: periods.getBody()) {
            CommonConditionVo conditionVo = new CommonConditionVo();
            conditionVo.setId(Integer.valueOf(dictionaryBo.getCode()));
            conditionVo.setName(dictionaryBo.getName());
            conditionVos.add(conditionVo);
        }
        conditionVos.add(getCommonConditionVo());
        resultData.setBody(conditionVos);
        return resultData;
    }

    /**
     * 获取分类
     *
     * @param subjectId
     * @param period
     * @return
     */
    @RequestMapping(value = "/getCategory")
    public ResultData getCategory(@RequestParam(required = false) Integer subjectId,
                                  @RequestParam(required = false) Integer period){
        ResultData resultData = new ResultData();
        ApiResponse<List<CommonConditionVo>> response = goodsServiceFacade.queryCategory(subjectId, period);
        List<CommonConditionVo> categoryVos = response.getBody();
        resultData.setBody(categoryVos);
        return resultData;
    }

    /**
     * 获取夹菜版本或者考区
     *
     * @param subjectId
     * @param period
     * @param categoryId
     * @return
     */
    @RequestMapping(value = "/getBookVersionArea")
    public ResultData getBookVersionArea(@RequestParam(required = false) Integer subjectId,
                                         @RequestParam(required = false) Integer period,
                                         @RequestParam(required = false) Integer categoryId){
        ResultData resultData = new ResultData();
        List<CommonConditionVo> conditionVos = new ArrayList<>();
        if (categoryId.equals(GoodsConstant.GoodsCategory.BOOKVERSION.getValue())) {
            //查询教材版本
            ApiResponse<List<Integer>> response = goodsServiceFacade.queryBookVersion(subjectId, period);
            ApiResponse<List<BookVersionBo>> bookVersion = bookVersionApi.findByBookVersionIds(response.getBody());
            for (BookVersionBo bookVersionBo: bookVersion.getBody()) {
                CommonConditionVo conditionVo = new CommonConditionVo();
                conditionVo.setId(bookVersionBo.getId());
                conditionVo.setName(bookVersionBo.getName());
                conditionVos.add(conditionVo);
            }
        }else if (categoryId.equals(GoodsConstant.GoodsCategory.AREA.getValue())) {
            //按考区分
            ApiResponse<List<Integer>> response = goodsServiceFacade.queryArea(subjectId, period);
            ApiResponse<List<ExamAreaBo>> examArea = examAreaApi.findByExamAreaIds(response.getBody());

            for (ExamAreaBo examAreaBo: examArea.getBody()) {
                CommonConditionVo conditionVo = new CommonConditionVo();
                conditionVo.setId(examAreaBo.getId());
                conditionVo.setName(examAreaBo.getName());
                conditionVos.add(conditionVo);
            }
        }

        conditionVos.add(getCommonConditionVo());
        resultData.setBody(conditionVos);

        return resultData;
    }

    /**
     * 通过商品名模糊查找商品
     *
     * @param goodName
     * @param insId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/getByGoodsName")
    public ResultData queryByGoodName(@RequestParam String goodName, @RequestParam Integer insId,
                                      @RequestParam Integer pageNum, @RequestParam Integer pageSize){
        ResultData resultData = new ResultData();
        RequestGoodsConditionVo conditionVo = new RequestGoodsConditionVo();
        conditionVo.setInsId(insId);
        conditionVo.setPageNum(pageNum);
        conditionVo.setPageSize(pageSize);
        conditionVo.setGoodsName(goodName);
        ApiResponse<Page<GoodsVo>> response = goodsServiceFacade.queryGoodsList(conditionVo);
        Page<GoodsVo> page = response.getBody();
        loadRelationName(page.getList());
        resultData.setBody(page);

        return resultData;
    }

    /**
     * 商品列表查询
     *
     * @param insId 结构id
     * @param sid 学科id
     * @param pid 学期code
     * @param vtId 教材版本=1/考区=2
     * @param veId 教材版本/考区
     * @param pageNum 当前页
     * @param pageSize 页大小
     * @return
     */
    @RequestMapping(value = "/goodsList", method = RequestMethod.GET)
    public ResultData queryGoodsList(@RequestParam Integer insId, @RequestParam(required = false) Integer sid,
                                     @RequestParam(required = false) Integer pid, @RequestParam(required = false) Integer vtId,
                                     @RequestParam(required = false) Integer veId, @RequestParam Integer pageNum,
                                     @RequestParam Integer pageSize){
        ResultData resultData = new ResultData();
        RequestGoodsConditionVo conditionVo = new RequestGoodsConditionVo();
        conditionVo.setInsId(insId);
        conditionVo.setSid(sid);
        conditionVo.setPid(pid);
        conditionVo.setVtId(vtId);
        conditionVo.setVeId(veId);
        conditionVo.setPageNum(pageNum);
        conditionVo.setPageSize(pageSize);
        ApiResponse<Page<GoodsVo>> response = goodsServiceFacade.queryGoodsList(conditionVo);
        Page<GoodsVo> page = response.getBody();

        loadRelationName(page.getList());
        resultData.setBody(page);

        return resultData;
    }

    /**
     * 商品详情
     *
     * @param goodsId
     * @param insId
     * @return
     */
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public ResultData queryGoodsDetail(@RequestParam Integer goodsId, @RequestParam Integer insId){
        ResultData resultData = new ResultData();
        ApiResponse<GoodsVo> response = goodsServiceFacade.queryGoodsDetail(goodsId, insId);
        GoodsVo goodsVo = response.getBody();
        loadRelationName(Lists.newArrayList(goodsVo));
        goodsVo.setSchemeStr(getScheme(goodsVo.getScheme()));
        resultData.setBody(response.getBody());
        return resultData;
    }

    private String getScheme(Integer scheme){
        ApiResponse<SchemeBo> response = schemeApi.getById(scheme);
        SchemeBo schemeBo = response.getBody();
        return schemeBo.getName();
    }

    private void loadRelationName(List<GoodsVo> list){
        List<Integer> bookVersionIds = new ArrayList<>();
        List<Integer> examAreaIds = new ArrayList<>();
        for (GoodsVo goodsVo: list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation:relationGoods) {
                if (!relation.getBookVersion().equals(0)) {
                    bookVersionIds.add(relation.getBookVersion());
                }
                if (!relation.getExamAreaId().equals(0)) {
                    examAreaIds.add(relation.getExamAreaId());
                }
            }
        }
        List<BookVersionBo> bookVersionBos = new ArrayList<>();
        List<ExamAreaBo> examAreaBos = new ArrayList<>();
        Map<Integer, ExamAreaBo> examAreaMap = new HashMap<>();
        Map<Integer, BookVersionBo> bookVersionMap = new HashMap<>();


        if (CollectionUtils.isNotEmpty(bookVersionIds)) {
            ApiResponse<List<BookVersionBo>> bookVersionResponse = bookVersionApi.findByBookVersionIds(bookVersionIds);
            bookVersionBos = bookVersionResponse.getBody();
        }

        if (CollectionUtils.isNotEmpty(examAreaIds)) {
            ApiResponse<List<ExamAreaBo>> examAreaResponse = examAreaApi.findByExamAreaIds(examAreaIds);
            examAreaBos = examAreaResponse.getBody();
        }

        bookVersionMap = toBookVersionMap(bookVersionBos);
        examAreaMap = toExamAreaMap(examAreaBos);

        for (GoodsVo goodsVo: list) {
            List<RelationGoodsVo> relationGoods = goodsVo.getRelationGoods();
            for (RelationGoodsVo relation:relationGoods) {
                if (!relation.getBookVersion().equals(0)) {
                    BookVersionBo bookVersion = bookVersionMap.get(relation.getBookVersion());
                    relation.setRelationName(bookVersion.getName());
                }
                if (!relation.getExamAreaId().equals(0)) {
                    ExamAreaBo examArea = examAreaMap.get(relation.getExamAreaId());
                    relation.setRelationName(examArea.getName());
                }
            }
        }
    }

    public Map<Integer, BookVersionBo> toBookVersionMap(List<BookVersionBo> bookVersionBos){
        Map<Integer, BookVersionBo> map = new HashMap<>();
        for (BookVersionBo bookVersionBo: bookVersionBos) {
            if (map.containsKey(bookVersionBo.getId())) {
                continue;
            }else {
                map.put(bookVersionBo.getId(), bookVersionBo);
            }
        }

        return map;
    }

    public Map<Integer, ExamAreaBo> toExamAreaMap(List<ExamAreaBo> examAreaBos){
        Map<Integer, ExamAreaBo> map = new HashMap<>();
        for (ExamAreaBo examAreaBo: examAreaBos) {
            if (map.containsKey(examAreaBo.getId())) {
                continue;
            }else {
                map.put(examAreaBo.getId(), examAreaBo);
            }
        }

        return map;
    }

    public CommonConditionVo getCommonConditionVo(){
        CommonConditionVo commonConditionVo = new CommonConditionVo();
        commonConditionVo.setId(0);
        commonConditionVo.setName("全部");
        return commonConditionVo;
    }
}
