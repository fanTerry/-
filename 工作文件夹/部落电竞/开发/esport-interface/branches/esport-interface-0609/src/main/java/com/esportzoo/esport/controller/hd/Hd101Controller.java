package com.esportzoo.esport.controller.hd;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.esportzoo.common.appmodel.domain.result.ModelResult;
import com.esportzoo.common.appmodel.domain.result.PageResult;
import com.esportzoo.common.appmodel.page.DataPage;
import com.esportzoo.common.redisclient.RedisClient;
import com.esportzoo.esport.client.service.charge.UserThirdOrderServiceClient;
import com.esportzoo.esport.client.service.common.SysConfigPropertyServiceClient;
import com.esportzoo.esport.connect.request.hd.*;
import com.esportzoo.esport.connect.response.CommonResponse;
import com.esportzoo.esport.connect.response.hd.AnnounceResponse;
import com.esportzoo.esport.connect.response.hd.UserContinueWayResponse;
import com.esportzoo.esport.connect.response.hd.UserHdWalletInfoResponse;
import com.esportzoo.esport.connect.response.payment.PayOrderResponse;
import com.esportzoo.esport.constant.ResponseConstant;
import com.esportzoo.esport.constants.ClientType;
import com.esportzoo.esport.constants.FeatureKey;
import com.esportzoo.esport.controller.BaseController;
import com.esportzoo.esport.domain.UserConsumer;
import com.esportzoo.esport.hd.constants.*;
import com.esportzoo.esport.hd.entity.HdInfo;
import com.esportzoo.esport.hd.entity.HdSubjectLog;
import com.esportzoo.esport.hd.entity.HdUserWalletLog;
import com.esportzoo.esport.hd.entity.HdUserWithdraw;
import com.esportzoo.esport.hd.info.HdInfoServiceClient;
import com.esportzoo.esport.hd.share.HdUserShareServiceClient;
import com.esportzoo.esport.hd.subject.HdSubjectLogServiceClient;
import com.esportzoo.esport.hd.subject.HdSubjectServiceClient;
import com.esportzoo.esport.hd.vo.HdUserGetShareUrlParam;
import com.esportzoo.esport.hd.vo.HdUserGetShareUrlResult;
import com.esportzoo.esport.hd.vo.HdUserWalletLogQueryVo;
import com.esportzoo.esport.hd.vo.HdUserWithdrawQueryVo;
import com.esportzoo.esport.hd.vo.subject.HdSubjectContinueVo;
import com.esportzoo.esport.hd.wallet.HdUserWalletLogServiceClient;
import com.esportzoo.esport.hd.wallet.HdUserWithdrawServiceClient;
import com.esportzoo.esport.manager.hd.Hd101Manager;
import com.esportzoo.esport.manager.hd.HdCommonManager;
import com.esportzoo.esport.manager.hd.SubjectManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 答题活动接口
 * 
 * @author jing.wu
 * @version 创建时间：2019年9月17日 下午2:26:05
 */
@Controller
@RequestMapping("hd101")
@Api(value = "答题活动相关接口", tags = { "答题活动相关接口" })
public class Hd101Controller extends BaseController {

	private transient final Logger logger = LoggerFactory.getLogger(getClass());
	public static final String logPrefix = "答题活动[101]相关接口-";

	@Autowired
	private HdCommonManager hdCommonManager;
	@Autowired
	private Hd101Manager hd101Manager;
	@Autowired
	private SubjectManager subjectManager;
	@Autowired
	private HdInfoServiceClient hdInfoServiceClient;
	@Autowired
	private HdSubjectLogServiceClient hdSubjectLogClient;
	@Autowired
	private HdUserShareServiceClient hdUserShareClient;
	@Autowired
	private UserThirdOrderServiceClient userThirdOrderServiceClient;
	@Autowired
	private HdSubjectServiceClient hdSubjectClient;


	@Autowired
	HdUserWalletLogServiceClient hdUserWalletLogServiceClient;

	@Autowired
	HdUserWithdrawServiceClient hdUserWithdrawServiceClient;


	@Autowired
	SysConfigPropertyServiceClient sysConfigPropertyServiceClient;

	@Autowired
	private RedisClient redisClient;

	/** 生成邀请码,并返回分享地址 */
	@RequestMapping(value = "/share/getShareUrl", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "生成分享地址", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "生成分享地址接口", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<JSONObject> getShareUrl(HttpServletRequest request, UserShareUrlRequest getShareUrlRequest) {
		JSONObject jsonObject = new JSONObject();
		CommonResponse<JSONObject> commonResponse = new CommonResponse<>();
		try {
			UserConsumer userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			logger.info(logPrefix + "用户id={},生成分享地址参数:{}", userConsumer.getId(), JSONObject.toJSONString(getShareUrlRequest));
			Long hdSubjectLogId = getShareUrlRequest.getHdSubjectLogId();
			if (null == hdSubjectLogId) {
				return CommonResponse.withErrorResp("必要参数为空");
			}
			HdUserGetShareUrlParam param = new HdUserGetShareUrlParam();
			param.setHdCode(HdCode.HD_101.getIndex());
			param.setHdSubjectLogId(hdSubjectLogId);
			param.setUserId(userConsumer.getId());
			param.setShareType(HdUserShareType.UNKNOW.getIndex());
			ModelResult<HdUserGetShareUrlResult> modelResult = hdUserShareClient.getUserShareUrl(param);
			logger.info(logPrefix + "用户id={},生成分享地址,调用接口返回数据:{}", userConsumer.getId(), JSONObject.toJSONString(modelResult));
			if (null == modelResult || !modelResult.isSuccess() || null == modelResult.getModel()) {
				logger.info(logPrefix + "用户id={},生成分享地址,调用接口异常,hdSubjectLogId:{}", userConsumer.getId(), hdSubjectLogId);
				return CommonResponse.withErrorResp("暂无答题记录,无法分享");
			}
			jsonObject.put("userShareUrl", modelResult.getModel());
			commonResponse.setData(jsonObject);
		} catch (Exception e) {
			logger.error(logPrefix + "获取分享地址异常, 参数【{}】 异常【{}】", JSONObject.toJSONString(getShareUrlRequest), e.getMessage(), e);
			return CommonResponse.withErrorResp("分享异常");
		}
		return commonResponse;
	}

	/** 判断是否续命成功可以继续答题(1分钟内前端刷新) */
	@RequestMapping(value = "/share/checkContinueStatus", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "是否续命成功可以继续答题", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "是否续命成功可以继续答题", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<JSONObject> checkContinueStatus(HttpServletRequest request, UserShareUrlRequest getShareUrlRequest) {
		JSONObject jsonObject = new JSONObject();
		CommonResponse<JSONObject> commonResponse = new CommonResponse<>();
		try {
			UserConsumer userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			logger.info(logPrefix + "用户id={},是否续命成功可以继续答题参数:{}", userConsumer.getId(), JSONObject.toJSONString(getShareUrlRequest));

			Long hdSubjectLogId = getShareUrlRequest.getHdSubjectLogId();
			if (null == hdSubjectLogId) {
				return CommonResponse.withErrorResp("必要参数为空");
			}
			// 活动状态
			CommonResponse<HdInfo> hdResponse = hdCommonManager.getHdBaseInfo(HdCode.HD_101.getIndex());
			if (null != hdResponse && StringUtils.isNotBlank(hdResponse.getCode()) && hdResponse.getCode().equals(ResponseConstant.RESP_SUCC_CODE)) {
				//logger.info(logPrefix + "用户id={},活动:{},正常开启", userConsumer.getId(), hdResponse.getData().getName());
			} else {
				logger.info(logPrefix + "用户id={},活动状态异常:{}", userConsumer.getId(), JSONObject.toJSONString(hdResponse));
				return CommonResponse.withErrorResp("活动状态异常");
			}
			ModelResult<HdSubjectContinueVo> modelResult = hdSubjectLogClient.queryHdSubjectLogByIdAndUserId(hdSubjectLogId, userConsumer.getId());
			if (null == modelResult || !modelResult.isSuccess() || null == modelResult.getModel()) {
				logger.info(logPrefix + "用户id={},是否续命成功可以继续答题,调用接口异常,hdSubjectLogId:{}", userConsumer.getId(), hdSubjectLogId);
				return CommonResponse.withErrorResp("暂无答题记录,无法分享");
			}
			logger.info(logPrefix + "用户id={},是否续命成功可以继续答题,调用接口返回是否续命成功:{}", userConsumer.getId(), modelResult.getModel().getIsDoSubject());
			HdSubjectContinueVo continueVo = modelResult.getModel();
			// 续命成功
			if (null != continueVo.getIsDoSubject() && continueVo.getIsDoSubject()) {
				subjectManager.reflushErrorSubjectCache(userConsumer.getId(), getShareUrlRequest.getHdUserLogId(), continueVo.getErrorSubjectId().longValue());
			}
			jsonObject.put("continueVo", continueVo);
			commonResponse.setData(jsonObject);
		} catch (Exception e) {
			logger.error(logPrefix + "获取分享地址异常, 参数【{}】 异常【{}】", JSONObject.toJSONString(getShareUrlRequest), e.getMessage(), e);
			return CommonResponse.withErrorResp("查询分享状态异常");
		}
		return commonResponse;
	}

	/** 获取用户续命方式 */
	@RequestMapping(value = "/getContinueWay", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "获取用户续命方式", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "获取用户续命方式接口", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<List<UserContinueWayResponse>> getContinueWay(HttpServletRequest request, UserContinueWayRequest userContinueWayRequest) {
		try {
			UserConsumer userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			logger.info(logPrefix + "用户id={},获取用户续命方式参数:{}", userConsumer.getId(), JSONObject.toJSONString(userContinueWayRequest));
			//设置60秒续命时间
//			String cach60Key  = Hd101Constants.SUBJECT_PAY_60S_LIMIT_KEY+"_"+userConsumer.getId()+"_"+userContinueWayRequest.getHdSubjectLogId();
//			redisClient.set(cach60Key, "1", 60);
			return hd101Manager.getContinueWay(userContinueWayRequest, userConsumer.getId());
		} catch (Exception e) {
			logger.error(logPrefix + "获取用户续命方式异常, 参数【{}】 异常【{}】", JSONObject.toJSONString(userContinueWayRequest), e.getMessage(), e);
			return CommonResponse.withErrorResp("接口异常");
		}
	}

	@RequestMapping(value = "/subjectPay", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "答题续命付费", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "答题续命付费", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<PayOrderResponse> subjectPay(SubjectPayRequest subjectPayRequest, HttpServletRequest request) {
		String logPrefix = "答题续命付费请求_";
		try {
			UserConsumer userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			String cach60Key  = Hd101Constants.SUBJECT_PAY_60S_LIMIT_KEY+userConsumer.getId()+"_"+subjectPayRequest.getHdSubjectLogId();
			DateTime time = redisClient.getObj(cach60Key);
			if (time==null){
				logger.info(logPrefix+"用户：【{}】，HdSubjectLogId={}, 60秒续命时间为空 【{}】",userConsumer.getId(),subjectPayRequest.getHdSubjectLogId());
				return CommonResponse.withErrorResp("续命时间为空60秒续命时间已经过期");
			}
//			Date expierTime = DateUtil.getDate2(time);
//			Date currTime = new Date();
//			if (DateUtil.calcCompareDate3(expierTime,currTime)==-1){
//				logger.info(logPrefix+"用户：【{}】，HdSubjectLogId={}, 60秒续命时间过期 【{}】",userConsumer.getId(),subjectPayRequest.getHdSubjectLogId());
//				return CommonResponse.withErrorResp("60秒续命时间已经过期");
//			}


			return hd101Manager.subjectPay(subjectPayRequest, request, userConsumer);
		} catch (Exception e) {
			logger.info(logPrefix + "发生异常,exception={}", e.getMessage(), e);
			return CommonResponse.withErrorResp(e.getMessage());
		}
	}


	@RequestMapping(value = "/watchOrShareToContinue", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "观看广告或者分享续命方式", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "观看广告或者分享续命方式", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse watchAdToContinue(HttpServletRequest request, Long subjectLogId, Integer type) {
		try {
			if (null==subjectLogId || null==type || null==SubjectContinueWayEnum.getSubjectContinueWayEnumByType(type)){
			    return CommonResponse.withErrorResp("重要参数有误！");
			}
			UserConsumer loginUsr = getLoginUsr(request);
			if (null == loginUsr) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			ModelResult<HdSubjectLog> hdSubjectLogModelResult = hdSubjectLogClient.queryById(subjectLogId);
			HdSubjectLog model = hdSubjectLogModelResult.getModel();
			if (!hdSubjectLogModelResult.isSuccess() || null == model) {
				logger.info(logPrefix + "观看广告续命 没有找到对应的答题流水 {} 发生异常,exception={}", subjectLogId, hdSubjectLogModelResult.getErrorMsg());
				return CommonResponse.withErrorResp("服务器异常");
			}
			if (model.getStatus().intValue() != HdSubjectLogStatus.CONTINUE.getIndex()) {
				logger.info(logPrefix + "观看广告续命 答题流水 {} 没有处于续命中状态", subjectLogId);
				return CommonResponse.withErrorResp("答题状态异常！");
			}
			if (ClientType.WXXCY.getIndex()!=model.getClientType().intValue() || loginUsr.getId().intValue()!=model.getUserId().intValue()){
			    return CommonResponse.withErrorResp("数据有误！");
			}
			if (type.equals(SubjectContinueWayEnum.WATCHAD.getWayType())) {
				if (StrUtil.isNotBlank(model.getFeature(FeatureKey.HD_WATCH_AD))){
				    return CommonResponse.withErrorResp("当前方式续命次数已用完!");
				}
				model.setupFeature(FeatureKey.HD_WATCH_AD, Boolean.TRUE);
			}
			if (type.equals(SubjectContinueWayEnum.SHARE.getWayType())) {
				if (StrUtil.isNotBlank(model.getFeature(FeatureKey.HD_SHARE))){
					return CommonResponse.withErrorResp("当前方式续命次数已用完!");
				}
				model.setupFeature(FeatureKey.HD_SHARE, Boolean.TRUE);
			}
			model.setStatus(HdSubjectLogStatus.INTO.getIndex());
			ModelResult<Integer> integerModelResult = hdSubjectLogClient.updateById(model);
			if (!integerModelResult.isSuccess()) {
				return CommonResponse.withErrorResp("服务器异常");
			}
		} catch (Exception e) {
			logger.info(logPrefix + "观看广告续命 发生异常,exception={}", e.getMessage(), e);
			return CommonResponse.withErrorResp(e.getMessage());
		}
		return CommonResponse.withSuccessResp(Boolean.TRUE);
	}

	@RequestMapping(value = "/subjectJoinPay", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "答题首页参与付费", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "答题首页参与付费", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<PayOrderResponse> subjectJoinPay(SubjectPayRequest subjectPayRequest, HttpServletRequest request) {
		String logPrefix = "答题首页参与付费请求_";
		try {
			UserConsumer userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			return hd101Manager.subjectJoinPay(subjectPayRequest, request, userConsumer);
		} catch (Exception e) {
			logger.info(logPrefix + "发生异常,exception={}", e.getMessage(), e);
			return CommonResponse.withErrorResp(e.getMessage());
		}
	}

	@RequestMapping(value = "/listUserGift", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "答题首页奖励榜单", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "答题首页奖励榜单", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<List<AnnounceResponse>> listUserGift(ShowUserGift userGift, HttpServletRequest request) {
		String logPrefix = "答题首页奖励榜单_";
		try {
			ModelResult<HdInfo> hdInfoModelResult = hdInfoServiceClient.queryByCode(HdCode.HD_101.getIndex());
			HdInfo model = hdInfoModelResult.getModel();
			if (!hdInfoModelResult.isSuccess() || null==model){
				logger.info(logPrefix+"未找到 hdcode为101的活动  param[{}]",JSONObject.toJSONString(userGift));
			}
			UserConsumer userConsumer = getLoginUsr(request);
			List<AnnounceResponse> announceResponses = null;
			Integer clientType = userGift.getClientType();
			if (null!=clientType&&ClientType.WXXCY.getIndex()==clientType.intValue()){
				if (userGift.getShowUser()){
					if (userConsumer == null) {
						return CommonResponse.withErrorResp("未获取到登录用户信息");
					}
					announceResponses=hd101Manager.showUserGiftInfo(userConsumer.getId(),Long.valueOf(model.getId()),clientType);
				}else {
					announceResponses=hd101Manager.showUserGiftInfo(null,Long.valueOf(model.getId()),clientType);
				}
			}else {
				if (userGift.getShowUser()) {
					if (userConsumer == null) {
						return CommonResponse.withErrorResp("未获取到登录用户信息");
					}
					announceResponses = hd101Manager.listUserGift(userConsumer.getId(), new Long(model.getId()));
				} else {
					announceResponses = hd101Manager.listUserGift(null, new Long(model.getId()));
				}
			}
//			logger.info(logPrefix + "此次获得的数据【{}】", JSONObject.toJSONString(showUserGiftResponses));
			return CommonResponse.withSuccessResp(announceResponses);
		} catch (Exception e) {
			logger.info(logPrefix + "发生异常,exception={}", e.getMessage(), e);
			return CommonResponse.withErrorResp(e.getMessage());
		}
	}

	@RequestMapping(value = "/incomeRecord", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "营收记录", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "营收记录", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<JSONObject> incomeRecord(HttpServletRequest request, Integer recordType, Integer pageSize, Integer pageNo) {
		String logPrefix = "用户营收记录请求_";
		JSONObject jsonObject = new JSONObject();
		try {
			UserConsumer userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			if (recordType == null) {
				logger.info("查询用户营收和体现记录,recordType.is.null  userId{}", userConsumer.getId());
				return CommonResponse.withErrorResp("recordType.is.null");
			}
			/** 0.营收 1.体现 */
			if (recordType == 0) {// 营收记录
				DataPage<HdUserWalletLog> page = new DataPage<>();
				page.setPageNo(pageNo);
				page.setPageSize(pageSize);
				if (pageSize == null) {
					page.setPageSize(10);
				}
				if (pageNo == null) {
					page.setPageNo(1);
				}
				HdUserWalletLogQueryVo queryVo = new HdUserWalletLogQueryVo();
				queryVo.setUserId(userConsumer.getId());
				queryVo.setTransType(HdTransType.SUBJECT_HD_SHARE_INCOME.getIndex());
				queryVo.setOrderBy("create_time desc");
				PageResult<HdUserWalletLog> pageResult = hdUserWalletLogServiceClient.queryPage(page, queryVo);
				if (!pageResult.isSuccess()) {
					logger.error("用户：【{}】，参数param 【{}】查询用户分页营收记录失败，失败原因：{},", userConsumer.getId(), JSON.toJSONString(queryVo),
							pageResult.getErrorMsg());
					return CommonResponse.withErrorResp("查询用户分页提现记录失败，失败原因");
				}
				jsonObject.put("recordList", pageResult.getPage().getDataList());
			} else {
				// 提现
				DataPage<HdUserWithdraw> page = new DataPage<>();
				page.setPageNo(pageNo);
				page.setPageSize(pageSize);
				if (pageSize == null) {
					page.setPageSize(10);
				}
				if (pageNo == null) {
					page.setPageNo(1);
				}
				HdUserWithdrawQueryVo withdrawQueryVo = new HdUserWithdrawQueryVo();
				withdrawQueryVo.setUserId(userConsumer.getId());
				withdrawQueryVo.setOrderBy("create_time desc");
				PageResult<HdUserWithdraw> pageResult = hdUserWithdrawServiceClient.queryPage(page, withdrawQueryVo);
				if (!pageResult.isSuccess()) {
					logger.error("用户：【{}】，参数param 【{}】查询用户分页提现记录失败，失败原因：{},", userConsumer.getId(), JSON.toJSONString(withdrawQueryVo),
							pageResult.getErrorMsg());
					return CommonResponse.withErrorResp("查询用户分页提现记录失败，失败原因");
				}
				List<HdUserWithdraw> hdUserWithdrawList = pageResult.getPage().getDataList();
				jsonObject.put("recordList", hdUserWithdrawList);
			}

		} catch (Exception e) {
			logger.info("发生异常,exception={}", e.getMessage(), e);
			return CommonResponse.withErrorResp(e.getMessage());
		}
		return CommonResponse.withSuccessResp(jsonObject);
	}

	@RequestMapping(value = "/showUserWallet", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "个人中心钱包信息", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "个人中心钱包信息", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<UserHdWalletInfoResponse> showUserWalletInfo(HttpServletRequest request) {
		String logPrefix = "个人中心钱包信息_";
		UserConsumer userConsumer = null;
		ModelResult<HdInfo> hdInfoModelResult = hdInfoServiceClient.queryByCode(HdCode.HD_101.getIndex());
		HdInfo model = hdInfoModelResult.getModel();
		if (!hdInfoModelResult.isSuccess() || null==model){
			logger.info(logPrefix+"查询用户钱包信息 未找到 hdcode为101的活动");
		}
		try {
			userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}
			UserHdWalletInfoResponse userHdWalletInfoResponse = hd101Manager.showUserWalletInfo(userConsumer, new Long(model.getId()));
			return CommonResponse.withSuccessResp(userHdWalletInfoResponse);
		} catch (Exception e) {
			logger.info("查询用户【{}】钱包信息发生异常【{}】", userConsumer.getId(), e.getMessage(), e);
			return CommonResponse.withErrorResp("服务器异常！");
		}
	}

	@RequestMapping(value = "/userWithdraw", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "活动提现", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "活动提现", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<UserHdWalletInfoResponse> userWithdraw(HttpServletRequest request, SubjectRequest subjectRequest) {
		String logPrefix = "活动提现";
		UserConsumer userConsumer = null;
		try {
			userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}

			return subjectManager.userWithdraw(userConsumer,subjectRequest);

		} catch (Exception e) {
			logger.info("查询用户【{}】活动提现发生异常【{}】", userConsumer.getId(), e.getMessage(), e);
			return CommonResponse.withErrorResp("服务器异常！");
		}
	}

	@RequestMapping(value = "/checkWithdraw", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
	@ApiOperation(value = "查询当天提现金额", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json")
	@ApiResponse(code = 200, message = "查询当天提现", response = CommonResponse.class)
	@ResponseBody
	public CommonResponse<UserHdWalletInfoResponse> checkWithdraw(HttpServletRequest request, SubjectRequest subjectRequest) {
		String logPrefix = "活动提现";
		UserConsumer userConsumer = null;
		try {
			userConsumer = getLoginUsr(request);
			if (userConsumer == null) {
				return CommonResponse.withErrorResp("未获取到登录用户信息");
			}

			return subjectManager.checkWithdraw(userConsumer,subjectRequest);

		} catch (Exception e) {
			logger.info("查询用户【{}】活动提现发生异常【{}】", userConsumer.getId(), e.getMessage(), e);
			return CommonResponse.withErrorResp("服务器异常！");
		}
	}

}
